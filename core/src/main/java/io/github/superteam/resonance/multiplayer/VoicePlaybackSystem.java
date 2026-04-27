package io.github.superteam.resonance.multiplayer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import io.github.superteam.resonance.multiplayer.packets.Packets.VoiceChunkPacket;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class VoicePlaybackSystem implements Disposable {

    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final float MAX_VOICE_DISTANCE = 20.0f;
    private static final float SILENCE_THRESHOLD = 400f;

    private final Map<Integer, PlayerAudioLine> audioLines = new HashMap<>();

    public void receiveChunk(
            VoiceChunkPacket packet,
            RemotePlayer sourcePlayer,
            Vector3 localPlayerPosition,
            Vector3 listenerForwardDirection) {
        int requestedSampleRate = packet.sampleRate > 0 ? packet.sampleRate : DEFAULT_SAMPLE_RATE;
        PlayerAudioLine line = audioLines.get(packet.sourcePlayerId);
        if (line == null || line.sampleRate() != requestedSampleRate) {
            if (line != null) {
                line.close();
            }
            line = openLine(requestedSampleRate);
            audioLines.put(packet.sourcePlayerId, line);
        }

        // Spatial volume: inverse-distance attenuation
        float dist = localPlayerPosition.dst(sourcePlayer.currentPosition);
        float volume = MathUtils.clamp(1.0f - (dist / MAX_VOICE_DISTANCE), 0.05f, 1.0f);
        line.setVolume(volume);

        // Stereo panning should use listener-relative left/right, not world-space X.
        Vector3 toPlayer = new Vector3(sourcePlayer.currentPosition).sub(localPlayerPosition).nor();
        Vector3 worldUp = new Vector3(0f, 1f, 0f);
        Vector3 camRight = new Vector3(listenerForwardDirection).crs(worldUp).nor();
        float pan = MathUtils.clamp(toPlayer.dot(camRight), -1f, 1f);
        line.setPan(pan);

        // Update speaking indicator
        sourcePlayer.isSpeaking = packet.rmsLevel > SILENCE_THRESHOLD;
        sourcePlayer.speakingTimer = 0.3f;  // fade-out delay

        line.pushJitter(packet.sequenceNumber, packet.pcmData);
        byte[] frame = line.popJitter();
        if (frame == null) {
            return;
        }

        line.write(frame);
    }

    private PlayerAudioLine openLine(int sampleRate) {
        try {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
            sdl.open(format, sampleRate / 5 * 2); // ~200ms buffer
            sdl.start();
            return new PlayerAudioLine(sdl, sampleRate);
        } catch (LineUnavailableException e) {
            throw new RuntimeException("Cannot open audio playback line", e);
        }
    }

    @Override
    public void dispose() {
        for (PlayerAudioLine l : audioLines.values()) l.close();
        audioLines.clear();
    }

    private static final class PlayerAudioLine {
        private static final int INITIAL_JITTER_FILL = 3;

        private final SourceDataLine line;
        private final int sampleRate;
        private final FloatControl gainControl;
        private final FloatControl panControl;
        private final JitterBuffer jitterBuffer = new JitterBuffer(INITIAL_JITTER_FILL);

        PlayerAudioLine(SourceDataLine line, int sampleRate) {
            this.line = line;
            this.sampleRate = sampleRate;
            this.gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            this.panControl = line.isControlSupported(FloatControl.Type.PAN)
                ? (FloatControl) line.getControl(FloatControl.Type.PAN)
                : (line.isControlSupported(FloatControl.Type.BALANCE)
                    ? (FloatControl) line.getControl(FloatControl.Type.BALANCE)
                    : null);
        }

        int sampleRate() {
            return sampleRate;
        }

        void setVolume(float normalised) {
            // Convert linear 0..1 to dB
            float dB = normalised > 0 ? 20f * (float) Math.log10(normalised) : gainControl.getMinimum();
            gainControl.setValue(MathUtils.clamp(dB, gainControl.getMinimum(), gainControl.getMaximum()));
        }

        void setPan(float panLR) { // -1 = full left, +1 = full right
            if (panControl != null) panControl.setValue(panLR);
        }

        void pushJitter(int sequenceNumber, byte[] payload) {
            if (payload == null || payload.length == 0) {
                return;
            }
            jitterBuffer.push(sequenceNumber, payload);
        }

        byte[] popJitter() {
            return jitterBuffer.pop();
        }

        void write(byte[] pcm) { line.write(pcm, 0, pcm.length); }
        void close() { line.stop(); line.close(); }
    }

    private static final class JitterBuffer {
        private final TreeMap<Integer, byte[]> frames = new TreeMap<>();
        private final int initialFillSize;
        private int expectedSequence = Integer.MIN_VALUE;

        private JitterBuffer(int initialFillSize) {
            this.initialFillSize = Math.max(1, initialFillSize);
        }

        void push(int sequenceNumber, byte[] payload) {
            frames.put(sequenceNumber, payload);
        }

        byte[] pop() {
            if (frames.isEmpty()) {
                return null;
            }

            if (expectedSequence == Integer.MIN_VALUE) {
                if (frames.size() < initialFillSize) {
                    return null;
                }
                expectedSequence = frames.firstKey();
            }

            byte[] next = frames.remove(expectedSequence);
            if (next != null) {
                expectedSequence++;
                return next;
            }

            Map.Entry<Integer, byte[]> earliest = frames.pollFirstEntry();
            if (earliest == null) {
                return null;
            }
            expectedSequence = earliest.getKey() + 1;
            return earliest.getValue();
        }
    }
}

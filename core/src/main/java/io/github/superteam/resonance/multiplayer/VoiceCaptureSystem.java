package io.github.superteam.resonance.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import io.github.superteam.resonance.multiplayer.packets.Packets.VoiceChunkPacket;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.function.Consumer;

public final class VoiceCaptureSystem implements Disposable {

    private static final int[] SAMPLE_RATE_CANDIDATES = { 44100, 48000, 22050, 16000 };
    private static final int CAPTURE_FRAME_MS = 20;
    private static final float SILENCE_THRESHOLD = 400f; // raw RMS — tune with calibration

    private TargetDataLine micLine;
    private Thread captureThread;
    private volatile boolean running;
    private final Consumer<VoiceChunkPacket> onChunkReady;
    private int sequenceNumber;
    private int sampleRate;
    private int frameSamples;
    private int bytesPerFrame;
    private volatile boolean startFailed;

    public VoiceCaptureSystem(Consumer<VoiceChunkPacket> onChunkReady) {
        this.onChunkReady = onChunkReady;
    }

    public boolean start(int localPlayerId) {
        if (running) {
            return true;
        }
        if (startFailed) {
            return false;
        }

        if (!openMicrophone()) {
            startFailed = true;
            return false;
        }

        running = true;
        captureThread = new Thread(() -> {
            byte[] buffer = new byte[bytesPerFrame];
            while (running) {
                int read = micLine.read(buffer, 0, bytesPerFrame);
                if (read <= 0) continue;

                float rms = computeRms(buffer, read);
                if (rms < SILENCE_THRESHOLD) continue;  // voice activity gate

                VoiceChunkPacket packet = new VoiceChunkPacket();
                packet.sourcePlayerId = localPlayerId;
                packet.sequenceNumber = sequenceNumber++;
                packet.sampleRate = sampleRate;
                packet.pcmData = Arrays.copyOf(buffer, read);
                packet.rmsLevel = rms;
                onChunkReady.accept(packet);
            }
        }, "VoiceCapture");
        captureThread.setDaemon(true);
        captureThread.start();
        return true;
    }

    private boolean openMicrophone() {
        for (int candidateSampleRate : SAMPLE_RATE_CANDIDATES) {
            int candidateFrameSamples = Math.max(1, (candidateSampleRate * CAPTURE_FRAME_MS) / 1000);
            int candidateBytesPerFrame = candidateFrameSamples * 2;
            AudioFormat format = new AudioFormat(candidateSampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                continue;
            }

            try {
                micLine = (TargetDataLine) AudioSystem.getLine(info);
                micLine.open(format, candidateBytesPerFrame * 4);
                micLine.start();
                sampleRate = candidateSampleRate;
                frameSamples = candidateFrameSamples;
                bytesPerFrame = candidateBytesPerFrame;
                return true;
            } catch (LineUnavailableException e) {
                Gdx.app.log("Voice", "Capture start failed at " + candidateSampleRate + " Hz: " + e.getMessage());
            }
        }

        Gdx.app.error("Voice", "Cannot open microphone line");
        return false;
    }

    private float computeRms(byte[] buffer, int length) {
        long sumSquares = 0;
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short)((buffer[i+1] << 8) | (buffer[i] & 0xFF));
            sumSquares += (long) sample * sample;
        }
        return (float) Math.sqrt((double) sumSquares / (length / 2));
    }

    @Override
    public void dispose() {
        running = false;
        if (micLine != null) {
            micLine.stop();
            micLine.close();
        }
    }
}

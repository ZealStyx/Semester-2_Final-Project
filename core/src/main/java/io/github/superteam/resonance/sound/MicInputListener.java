package io.github.superteam.resonance.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioRecorder;
import java.util.function.Consumer;

/**
 * Poll-based microphone listener for clap/shout threshold detection.
 */
public final class MicInputListener {
    private static final float NOISE_FLOOR_EMA_ALPHA = 0.08f;
    private static final float CLAP_ZCR_THRESHOLD = 0.18f;
    private static final float SHOUT_ENERGY_MULTIPLIER = 2.2f;
    private static final float GATE_COOLDOWN_SECONDS = 0.10f;
    private static final float SPEAKING_GATE_RATIO = 0.75f;
    private static final float LOW_LEVEL_RATIO = 0.90f;
    private static final float MID_LEVEL_RATIO = 1.35f;

    private AudioRecorder audioRecorder;
    private short[] sampleBuffer;
    private float thresholdRms;
    private float adaptiveNoiseFloor;
    private float gateCooldownRemainingSeconds;
    private float lastComputedRms;
    private boolean active;

    public MicInputListener(float thresholdRms) {
        this.thresholdRms = Math.max(0f, thresholdRms);
    }

    public void start(int sampleRate, int sampleCount) {
        if (active || Gdx.audio == null) {
            return;
        }
        int safeSampleRate = Math.max(8000, sampleRate);
        int safeSampleCount = Math.max(256, sampleCount);
        audioRecorder = Gdx.audio.newAudioRecorder(safeSampleRate, true);
        sampleBuffer = new short[safeSampleCount];
        adaptiveNoiseFloor = 0f;
        gateCooldownRemainingSeconds = 0f;
        lastComputedRms = 0f;
        active = true;
    }

    public void stop() {
        if (!active) {
            return;
        }
        if (audioRecorder != null) {
            audioRecorder.dispose();
        }
        audioRecorder = null;
        sampleBuffer = null;
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void setThresholdRms(float thresholdRms) {
        this.thresholdRms = Math.max(0f, thresholdRms);
    }

    public float getLastComputedRms() {
        return lastComputedRms;
    }

    public MicSample sampleRealtime(float deltaSeconds) {
        if (!active || audioRecorder == null || sampleBuffer == null) {
            return MicSample.silent();
        }

        audioRecorder.read(sampleBuffer, 0, sampleBuffer.length);
        float clampedDeltaSeconds = Math.max(0f, deltaSeconds);
        lastComputedRms = computeRms(sampleBuffer);
        updateAdaptiveNoiseFloor(lastComputedRms);
        gateCooldownRemainingSeconds = Math.max(0f, gateCooldownRemainingSeconds - clampedDeltaSeconds);

        float dynamicThreshold = Math.max(thresholdRms, adaptiveNoiseFloor * SHOUT_ENERGY_MULTIPLIER);
        float normalizedLevel = dynamicThreshold <= 0.0001f ? 0f : lastComputedRms / dynamicThreshold;
        boolean speakingDetected = lastComputedRms >= (dynamicThreshold * SPEAKING_GATE_RATIO);
        MicVolumeLevel micVolumeLevel = classifyVolumeLevel(normalizedLevel);

        MicEvent detectedEvent = null;
        if (gateCooldownRemainingSeconds <= 0f && lastComputedRms >= dynamicThreshold) {
            float zeroCrossingRate = computeZeroCrossingRate(sampleBuffer);
            detectedEvent = classifyMicEvent(lastComputedRms, zeroCrossingRate, dynamicThreshold);
            gateCooldownRemainingSeconds = GATE_COOLDOWN_SECONDS;
        }

        return new MicSample(lastComputedRms, dynamicThreshold, normalizedLevel, micVolumeLevel, speakingDetected, detectedEvent);
    }

    public void poll(Consumer<Float> onDetected) {
        poll(1f / 60f, micEvent -> {
            if (onDetected != null) {
                onDetected.accept(lastComputedRms);
            }
        });
    }

    public void poll(float deltaSeconds, Consumer<MicEvent> onDetected) {
        if (onDetected == null) {
            return;
        }
        MicSample micSample = sampleRealtime(deltaSeconds);
        if (micSample.detectedEvent() != null) {
            onDetected.accept(micSample.detectedEvent());
        }
    }

    private MicVolumeLevel classifyVolumeLevel(float normalizedLevel) {
        if (normalizedLevel < LOW_LEVEL_RATIO) {
            return MicVolumeLevel.LOW;
        }
        if (normalizedLevel < MID_LEVEL_RATIO) {
            return MicVolumeLevel.MID;
        }
        return MicVolumeLevel.HIGH;
    }

    private float computeRms(short[] samples) {
        if (samples.length == 0) {
            return 0f;
        }
        double sumSquares = 0d;
        for (short sample : samples) {
            float normalized = sample / 32768f;
            sumSquares += normalized * normalized;
        }
        return (float) Math.sqrt(sumSquares / samples.length);
    }

    private float computeZeroCrossingRate(short[] samples) {
        if (samples.length < 2) {
            return 0f;
        }
        int crossings = 0;
        for (int index = 1; index < samples.length; index++) {
            short previous = samples[index - 1];
            short current = samples[index];
            if ((previous < 0 && current >= 0) || (previous >= 0 && current < 0)) {
                crossings++;
            }
        }
        return crossings / (float) (samples.length - 1);
    }

    private void updateAdaptiveNoiseFloor(float rms) {
        if (adaptiveNoiseFloor <= 0f) {
            adaptiveNoiseFloor = rms;
            return;
        }

        if (rms < thresholdRms) {
            adaptiveNoiseFloor = (adaptiveNoiseFloor * (1f - NOISE_FLOOR_EMA_ALPHA)) + (rms * NOISE_FLOOR_EMA_ALPHA);
        }
    }

    private MicEvent classifyMicEvent(float rms, float zeroCrossingRate, float threshold) {
        if (zeroCrossingRate >= CLAP_ZCR_THRESHOLD) {
            return MicEvent.CLAP;
        }
        if (rms >= threshold * 1.25f) {
            return MicEvent.SHOUT;
        }
        return MicEvent.BLOW;
    }

    public record MicSample(
        float rms,
        float dynamicThreshold,
        float normalizedLevel,
        MicVolumeLevel volumeLevel,
        boolean speakingDetected,
        MicEvent detectedEvent
    ) {
        public static MicSample silent() {
            return new MicSample(0f, 1f, 0f, MicVolumeLevel.LOW, false, null);
        }
    }
}

package io.github.superteam.resonance.sound;

/**
 * Domain-facing mic system that exposes realtime speech frames and emission cadence.
 */
public final class RealtimeMicSystem {
    private static final float SPEECH_HOLD_SECONDS = 0.18f;
    private static final float LOW_ACTIVITY_NORMALIZED_LEVEL = 0.22f;
    private static final float LOW_EMIT_INTERVAL_SECONDS = 0.22f;
    private static final float MID_EMIT_INTERVAL_SECONDS = 0.16f;
    private static final float HIGH_EMIT_INTERVAL_SECONDS = 0.11f;

    private final MicInputListener micInputListener;
    private float speechHoldRemainingSeconds;
    private float emitRemainingSeconds;
    private MicInputListener.MicSample lastSample = MicInputListener.MicSample.silent();

    public RealtimeMicSystem(float thresholdRms) {
        this.micInputListener = new MicInputListener(thresholdRms);
    }

    public void start(int sampleRate, int sampleCount) {
        micInputListener.start(sampleRate, sampleCount);
    }

    public void stop() {
        micInputListener.stop();
        speechHoldRemainingSeconds = 0f;
        emitRemainingSeconds = 0f;
        lastSample = MicInputListener.MicSample.silent();
    }

    public boolean isActive() {
        return micInputListener.isActive();
    }

    public Frame update(float deltaSeconds) {
        float clampedDeltaSeconds = Math.max(0f, deltaSeconds);
        lastSample = micInputListener.sampleRealtime(clampedDeltaSeconds);

        if (lastSample.speakingDetected() || lastSample.normalizedLevel() >= LOW_ACTIVITY_NORMALIZED_LEVEL) {
            speechHoldRemainingSeconds = SPEECH_HOLD_SECONDS;
        } else {
            speechHoldRemainingSeconds = Math.max(0f, speechHoldRemainingSeconds - clampedDeltaSeconds);
        }

        boolean speakingActive = speechHoldRemainingSeconds > 0f;
        boolean shouldEmitSignal = false;
        if (speakingActive) {
            emitRemainingSeconds -= clampedDeltaSeconds;
            if (emitRemainingSeconds <= 0f) {
                shouldEmitSignal = true;
                emitRemainingSeconds = resolveEmitInterval(lastSample.volumeLevel());
            }
        } else {
            emitRemainingSeconds = 0f;
        }

        return new Frame(lastSample, speakingActive, shouldEmitSignal);
    }

    public Frame lastFrame() {
        return new Frame(lastSample, false, false);
    }

    private float resolveEmitInterval(MicVolumeLevel micVolumeLevel) {
        return switch (micVolumeLevel) {
            case HIGH -> HIGH_EMIT_INTERVAL_SECONDS;
            case MID -> MID_EMIT_INTERVAL_SECONDS;
            case LOW -> LOW_EMIT_INTERVAL_SECONDS;
        };
    }

    public record Frame(
        MicInputListener.MicSample sample,
        boolean speakingActive,
        boolean shouldEmitSignal
    ) {
        public static Frame silent() {
            return new Frame(MicInputListener.MicSample.silent(), false, false);
        }
    }
}

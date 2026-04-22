package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.sound.RealtimeMicSystem;

/**
 * Bridges realtime microphone input with configurable sensitivity and emit cadence.
 */
public final class MicrophoneInputAdapter {
    private final RealtimeMicSystem realtimeMicSystem;
    private float sensitivityThreshold;
    private RealtimeMicSystem.Frame lastFrame = RealtimeMicSystem.Frame.silent();

    public MicrophoneInputAdapter(float thresholdRms, float sensitivityThreshold) {
        this.realtimeMicSystem = new RealtimeMicSystem(thresholdRms);
        this.sensitivityThreshold = MathUtils.clamp(sensitivityThreshold, 0.01f, 2f);
    }

    public void setSensitivityThreshold(float sensitivityThreshold) {
        this.sensitivityThreshold = MathUtils.clamp(sensitivityThreshold, 0.01f, 2f);
    }

    public void start(int sampleRate, int sampleCount) {
        realtimeMicSystem.start(sampleRate, sampleCount);
    }

    public void stop() {
        realtimeMicSystem.stop();
        lastFrame = RealtimeMicSystem.Frame.silent();
    }

    public boolean isActive() {
        return realtimeMicSystem.isActive();
    }

    public RealtimeMicSystem.Frame update(float deltaSeconds) {
        lastFrame = realtimeMicSystem.update(deltaSeconds);
        return lastFrame;
    }

    public boolean shouldEmitMicEvent() {
        return lastFrame.shouldEmitSignal() && lastFrame.sample().normalizedLevel() >= sensitivityThreshold;
    }

    public RealtimeMicSystem.Frame lastFrame() {
        return lastFrame;
    }
}

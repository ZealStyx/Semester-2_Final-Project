package io.github.superteam.resonance.lighting;

import com.badlogic.gdx.math.MathUtils;

/**
 * Stateless flicker waveform provider used by {@link LightManager}.
 */
public final class FlickerController {

    public float computeIntensity(GameLight light, LightingTier tier, float elapsedSeconds, int lightIndex) {
        if (light == null || light.broken()) {
            return 0f;
        }

        float base = light.baseIntensity();
        if (!light.flickerEnabled()) {
            return base;
        }

        float phase = lightIndex * 1.123f;
        return switch (tier) {
            case CALM -> clamp(base + oscillate(0.15f, 0.03f, elapsedSeconds, phase), 0f, 2f);
            case TENSE -> clamp(base + oscillate(1.10f, 0.15f, elapsedSeconds, phase), 0f, 2f);
            case PANICKED -> {
                float fast = oscillate(6.0f, 0.40f, elapsedSeconds, phase);
                float chaos = oscillate(2.4f, 0.22f, elapsedSeconds, phase * 0.77f);
                yield clamp(base + fast + chaos, 0.02f, 2f);
            }
            case EXPOSED -> {
                float strobe = MathUtils.sin(MathUtils.PI2 * 11.0f * elapsedSeconds + phase);
                yield strobe > 0f ? base : 0.22f;
            }
        };
    }

    private float oscillate(float frequencyHz, float amplitude, float elapsedSeconds, float phase) {
        return MathUtils.sin(MathUtils.PI2 * frequencyHz * elapsedSeconds + phase) * amplitude;
    }

    private float clamp(float value, float min, float max) {
        return MathUtils.clamp(value, min, max);
    }
}

package io.github.superteam.resonance.interactable.door;

import com.badlogic.gdx.math.MathUtils;

/**
 * Maps door angular speed to creak audio parameters.
 */
public final class DoorCreakSystem {
    private static final float CREAK_SPEED_MIN = 5f;
    private static final float CREAK_SPEED_MAX = 100f;

    public CreakSample sample(float angularSpeedDegPerSecond) {
        float speed = Math.abs(angularSpeedDegPerSecond);
        if (speed < CREAK_SPEED_MIN) {
            return new CreakSample(0f, 0f);
        }

        float normalized = MathUtils.clamp(
            (speed - CREAK_SPEED_MIN) / (CREAK_SPEED_MAX - CREAK_SPEED_MIN),
            0f,
            1f
        );
        float volume = 0.08f + (0.72f * normalized);
        float pitch = 0.78f + (0.45f * normalized);
        return new CreakSample(volume, pitch);
    }

    public record CreakSample(float volume, float pitch) {
    }
}

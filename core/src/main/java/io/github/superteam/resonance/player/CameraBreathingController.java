package io.github.superteam.resonance.player;

import com.badlogic.gdx.math.MathUtils;

/**
 * Produces subtle additive camera breathing offsets for first-person idle motion.
 */
public final class CameraBreathingController {

    private static final float BREATHE_FREQ_HZ = 0.22f;
    private static final float FLUTTER_FREQ_HZ = 0.80f;
    private static final float VERTICAL_AMPLITUDE = 0.0028f;
    private static final float ROLL_AMPLITUDE_DEG = 0.18f;
    private static final float SPRINT_SCALE = 2.8f;
    private static final float CROUCH_SCALE = 0.7f;
    private static final float EXHAUSTED_SCALE = 4.5f;
    private static final float LOW_STAMINA_THRESHOLD = 0.2f;
    private static final float LERP_SPEED = 3.5f;

    private float phase;
    private float flutterPhase;
    private float currentScale = 1f;

    /**
     * Updates breathing phase and state-driven intensity.
     */
    public void update(float deltaSeconds, MovementState movementState, float staminaNormalized) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        phase += clampedDelta * BREATHE_FREQ_HZ * MathUtils.PI2;
        flutterPhase += clampedDelta * FLUTTER_FREQ_HZ * MathUtils.PI2;

        float targetScale = resolveTargetScale(movementState, staminaNormalized);
        currentScale = MathUtils.lerp(currentScale, targetScale, clampedDelta * LERP_SPEED);
    }

    /**
     * Returns additive vertical offset in world units.
     */
    public float verticalOffset() {
        float chest = MathUtils.sin(phase) * VERTICAL_AMPLITUDE;
        float flutter = MathUtils.sin(flutterPhase) * VERTICAL_AMPLITUDE * 0.3f;
        return (chest + flutter) * currentScale;
    }

    /**
     * Returns additive roll angle in degrees.
     */
    public float rollDegrees() {
        return MathUtils.sin(phase * 0.5f) * ROLL_AMPLITUDE_DEG * currentScale;
    }

    private float resolveTargetScale(MovementState movementState, float staminaNormalized) {
        float clampedStamina = MathUtils.clamp(staminaNormalized, 0f, 1f);
        if (movementState == MovementState.CROUCH) {
            return CROUCH_SCALE;
        }

        if (movementState == MovementState.RUN) {
            if (clampedStamina < LOW_STAMINA_THRESHOLD) {
                float t = clampedStamina / LOW_STAMINA_THRESHOLD;
                return MathUtils.lerp(EXHAUSTED_SCALE, SPRINT_SCALE, t);
            }
            return SPRINT_SCALE;
        }

        return 1f;
    }
}

package io.github.superteam.resonance.behavior;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.player.PlayerFeatureExtractor;

/** Converts raw runtime telemetry into the normalized sample space used by behavior. */
public final class FeatureNormalizer {
    private FeatureNormalizer() {
    }

    public static BehaviorSample from(
        PlayerFeatureExtractor.PlayerFeatures features,
        MovementState movementState,
        boolean flashlightOn,
        float flashlightToggleRate,
        float sanityLossThisWindow,
        float lightExposure
    ) {
        if (features == null) {
            return neutralSample();
        }

        float averageVelocity = clamp(features.avgSpeed / 5f, 0f, 1f);
        float averageNoiseRms = clamp((features.rotationRate / 180f) * 0.55f + features.collisionRate * 0.45f, 0f, 1f);
        float crouchFraction = switch (movementState == null ? MovementState.IDLE : movementState) {
            case CROUCH -> 1f;
            case SLOW_WALK -> 0.45f;
            case WALK -> 0.20f;
            case RUN -> 0f;
            default -> 0f;
        };
        float stationaryFraction = clamp(features.stationaryRatio, 0f, 1f);
        float cameraAngularVariance = clamp(features.rotationRate / 240f, 0f, 1f);
        float normalizedToggleRate = flashlightOn ? clamp(flashlightToggleRate, 0f, 1f) : clamp(flashlightToggleRate * 0.5f, 0f, 1f);
        float normalizedSanityLoss = clamp(sanityLossThisWindow / 20f, 0f, 1f);
        float normalizedExposure = clamp(lightExposure, 0f, 1f);

        return new BehaviorSample(
            averageVelocity,
            averageNoiseRms,
            crouchFraction,
            stationaryFraction,
            cameraAngularVariance,
            normalizedToggleRate,
            normalizedSanityLoss,
            normalizedExposure
        );
    }

    public static BehaviorSample neutralSample() {
        return new BehaviorSample(0.15f, 0.10f, 0.20f, 0.70f, 0.10f, 0.10f, 0.0f, 0.75f);
    }

    public static float clamp(float value, float minValue, float maxValue) {
        return MathUtils.clamp(value, minValue, maxValue);
    }
}

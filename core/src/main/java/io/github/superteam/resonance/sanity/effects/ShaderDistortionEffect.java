package io.github.superteam.resonance.sanity.effects;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.sanity.SanityEffect;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Outputs distortion amount for post-process shader controls.
 */
public final class ShaderDistortionEffect implements SanityEffect {
    private float distortion;

    @Override
    public void apply(SanitySystem sanitySystem, SanitySystem.Context context, float deltaSeconds) {
        if (sanitySystem == null) {
            return;
        }

        float target = switch (sanitySystem.tier()) {
            case STABLE -> 0f;
            case LOW -> 0.10f;
            case MEDIUM -> 0.28f;
            case HIGH -> 0.52f;
            case CRITICAL -> 0.85f;
        };
        float alpha = MathUtils.clamp(deltaSeconds * 2.8f, 0f, 1f);
        distortion = MathUtils.lerp(distortion, target, alpha);
    }

    public float distortion() {
        return distortion;
    }
}

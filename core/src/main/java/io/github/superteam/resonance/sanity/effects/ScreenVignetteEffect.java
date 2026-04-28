package io.github.superteam.resonance.sanity.effects;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.sanity.SanityEffect;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Outputs a 0..1 vignette strength based on sanity tier.
 */
public final class ScreenVignetteEffect implements SanityEffect {
    private float strength;

    @Override
    public void apply(SanitySystem sanitySystem, SanitySystem.Context context, float deltaSeconds) {
        if (sanitySystem == null) {
            return;
        }

        float target = switch (sanitySystem.tier()) {
            case STABLE -> 0f;
            case LOW -> 0.25f;
            case MEDIUM -> 0.50f;
            case HIGH -> 0.72f;
            case CRITICAL -> 0.92f;
        };
        float alpha = MathUtils.clamp(deltaSeconds * 3.5f, 0f, 1f);
        strength = MathUtils.lerp(strength, target, alpha);
    }

    public float strength() {
        return strength;
    }
}

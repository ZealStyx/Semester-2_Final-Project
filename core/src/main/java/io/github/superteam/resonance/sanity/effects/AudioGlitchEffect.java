package io.github.superteam.resonance.sanity.effects;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.sanity.SanityEffect;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Outputs lightweight audio glitch intensity from sanity state.
 */
public final class AudioGlitchEffect implements SanityEffect {
    private float glitchAmount;

    @Override
    public void apply(SanitySystem sanitySystem, SanitySystem.Context context, float deltaSeconds) {
        if (sanitySystem == null) {
            return;
        }

        float target = switch (sanitySystem.tier()) {
            case STABLE -> 0f;
            case LOW -> 0.06f;
            case MEDIUM -> 0.20f;
            case HIGH -> 0.46f;
            case CRITICAL -> 0.74f;
        };
        float alpha = MathUtils.clamp(deltaSeconds * 2.2f, 0f, 1f);
        glitchAmount = MathUtils.lerp(glitchAmount, target, alpha);
    }

    public float glitchAmount() {
        return glitchAmount;
    }
}

package io.github.superteam.resonance.sanity.effects;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.sanity.SanityEffect;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Tracks hallucination pressure (0..1) for future System 21 director wiring.
 */
public final class HallucinationEffect implements SanityEffect {
    private float pressure;

    @Override
    public void apply(SanitySystem sanitySystem, SanitySystem.Context context, float deltaSeconds) {
        if (sanitySystem == null) {
            return;
        }
        float target = MathUtils.clamp((50f - sanitySystem.getSanity()) / 50f, 0f, 1f);
        float alpha = MathUtils.clamp(deltaSeconds * 2.0f, 0f, 1f);
        pressure = MathUtils.lerp(pressure, target, alpha);
    }

    public float pressure() {
        return pressure;
    }
}

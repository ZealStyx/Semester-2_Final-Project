package io.github.superteam.resonance.sanity.drains;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.sanity.SanityDrainSource;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Drains sanity when player light exposure is low.
 */
public final class DarknessPresenceDrain implements SanityDrainSource {
    private final float maxDrainPerSecond;

    public DarknessPresenceDrain(float maxDrainPerSecond) {
        this.maxDrainPerSecond = Math.max(0f, maxDrainPerSecond);
    }

    @Override
    public float drainPerSecond(SanitySystem.Context context) {
        if (context == null) {
            return 0f;
        }
        float darkness = 1f - context.lightExposure();
        return maxDrainPerSecond * MathUtils.clamp(darkness, 0f, 1f);
    }
}

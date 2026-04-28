package io.github.superteam.resonance.sanity.drains;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.sanity.SanityDrainSource;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Drains sanity more aggressively when estimated enemy distance is short.
 */
public final class EnemyProximityDrain implements SanityDrainSource {
    private final float maxDistanceMeters;
    private final float maxDrainPerSecond;

    public EnemyProximityDrain(float maxDistanceMeters, float maxDrainPerSecond) {
        this.maxDistanceMeters = Math.max(0.1f, maxDistanceMeters);
        this.maxDrainPerSecond = Math.max(0f, maxDrainPerSecond);
    }

    @Override
    public float drainPerSecond(SanitySystem.Context context) {
        if (context == null) {
            return 0f;
        }
        float proximity = 1f - (context.enemyDistanceMeters() / maxDistanceMeters);
        return maxDrainPerSecond * MathUtils.clamp(proximity, 0f, 1f);
    }
}

package io.github.superteam.resonance.sanity;

/**
 * Produces sanity drain over time and optional immediate sanity deltas.
 */
public interface SanityDrainSource {
    float drainPerSecond(SanitySystem.Context context);

    default float immediateDelta(SanitySystem.Context context) {
        return 0f;
    }
}

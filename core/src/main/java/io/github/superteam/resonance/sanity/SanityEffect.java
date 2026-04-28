package io.github.superteam.resonance.sanity;

/**
 * Applies presentation/gameplay effects based on current sanity state.
 */
public interface SanityEffect {
    void apply(SanitySystem sanitySystem, SanitySystem.Context context, float deltaSeconds);
}

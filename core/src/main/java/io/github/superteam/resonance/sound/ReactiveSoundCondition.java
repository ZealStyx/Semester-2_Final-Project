package io.github.superteam.resonance.sound;

/**
 * Predicate seam for condition-driven reactive sound sources.
 */
@FunctionalInterface
public interface ReactiveSoundCondition {
    boolean isSatisfied(float nowSeconds);
}

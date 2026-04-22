package io.github.superteam.resonance.rendering.blind;

/**
 * Provides panic level in the range [0, 1].
 */
public interface PanicLevelProvider {
    float currentPanicLevel();
}

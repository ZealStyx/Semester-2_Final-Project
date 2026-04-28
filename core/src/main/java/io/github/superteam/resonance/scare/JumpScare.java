package io.github.superteam.resonance.scare;

/**
 * Immutable runtime jump-scare trigger payload.
 */
public record JumpScare(
    ScareType type,
    float intensity,
    String message
) {
}

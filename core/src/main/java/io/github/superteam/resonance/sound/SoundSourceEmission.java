package io.github.superteam.resonance.sound;

/**
 * One emission produced by a source during a registry update tick.
 */
public record SoundSourceEmission(
    String sourceId,
    SoundEventData soundEventData
) {
}

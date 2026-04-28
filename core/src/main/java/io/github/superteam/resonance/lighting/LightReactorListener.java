package io.github.superteam.resonance.lighting;

import io.github.superteam.resonance.sound.SoundEventData;

/**
 * Receives lighting-reactive signals such as sound spikes and tier changes.
 */
public interface LightReactorListener {
    default void onLightingTierChanged(LightingTier newTier) {
    }

    default void onSoundEvent(SoundEventData soundEventData) {
    }
}

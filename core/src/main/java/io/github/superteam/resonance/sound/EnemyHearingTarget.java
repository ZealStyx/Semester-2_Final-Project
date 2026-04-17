package io.github.superteam.resonance.sound;

/**
 * Enemy integration point for receiving propagated sound intensity values.
 */
public interface EnemyHearingTarget {
    String getCurrentNodeId();

    void onSoundHeard(float propagatedIntensity, SoundEventData soundEventData);
}

package io.github.superteam.resonance.sound;

/**
 * Director integration point for receiving full propagation data from emitted events.
 */
public interface EchoDirectorListener {
    void onSoundEvent(SoundEventData soundEventData, PropagationResult propagationResult);
}

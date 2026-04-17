package io.github.superteam.resonance.sound;

import java.util.List;

/**
 * Update-driven sound source contract used by the orchestrator source registry.
 */
public interface SoundSource {
    String id();

    SoundSourceType type();

    boolean isActive();

    List<SoundSourceEmission> updateAndCollect(float deltaSeconds, float nowSeconds);
}

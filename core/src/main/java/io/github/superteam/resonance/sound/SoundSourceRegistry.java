package io.github.superteam.resonance.sound;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks active sources and collects emissions every update tick.
 */
public final class SoundSourceRegistry {
    private final Map<String, SoundSource> sourceById = new LinkedHashMap<>();

    public void register(SoundSource soundSource) {
        Objects.requireNonNull(soundSource, "Sound source must not be null.");
        sourceById.put(soundSource.id(), soundSource);
    }

    public void remove(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return;
        }
        sourceById.remove(sourceId);
    }

    public void clear() {
        sourceById.clear();
    }

    public List<SoundSourceEmission> updateAndCollect(float deltaSeconds, float nowSeconds) {
        List<SoundSourceEmission> emissions = new ArrayList<>();
        List<String> retiredSourceIds = new ArrayList<>();

        for (SoundSource soundSource : sourceById.values()) {
            if (!soundSource.isActive()) {
                retiredSourceIds.add(soundSource.id());
                continue;
            }
            emissions.addAll(soundSource.updateAndCollect(deltaSeconds, nowSeconds));
        }

        for (String retiredSourceId : retiredSourceIds) {
            sourceById.remove(retiredSourceId);
        }
        return emissions;
    }
}

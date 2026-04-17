package io.github.superteam.resonance.sound;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable propagation payload containing shortest-path distances and attenuated intensity values.
 */
public final class PropagationResult {
    private final String sourceNodeId;
    private final float baseIntensity;
    private final Map<String, Float> distanceByNodeId;
    private final Map<String, Float> intensityByNodeId;
    private final Map<String, Float> materialOcclusionByNodeId;
    private final List<String> revealNodeIds;

    public PropagationResult(
        String sourceNodeId,
        float baseIntensity,
        Map<String, Float> distanceByNodeId,
        Map<String, Float> intensityByNodeId,
        List<String> revealNodeIds
    ) {
        this(
            sourceNodeId,
            baseIntensity,
            distanceByNodeId,
            intensityByNodeId,
            Collections.emptyMap(),
            revealNodeIds
        );
    }

    public PropagationResult(
        String sourceNodeId,
        float baseIntensity,
        Map<String, Float> distanceByNodeId,
        Map<String, Float> intensityByNodeId,
        Map<String, Float> materialOcclusionByNodeId,
        List<String> revealNodeIds
    ) {
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            throw new IllegalArgumentException("Source node id must not be blank.");
        }
        if (baseIntensity < 0f) {
            throw new IllegalArgumentException("Base intensity must be non-negative.");
        }
        Objects.requireNonNull(distanceByNodeId, "Distance map must not be null.");
        Objects.requireNonNull(intensityByNodeId, "Intensity map must not be null.");
        Objects.requireNonNull(materialOcclusionByNodeId, "Material occlusion map must not be null.");
        Objects.requireNonNull(revealNodeIds, "Reveal node ids must not be null.");

        this.sourceNodeId = sourceNodeId;
        this.baseIntensity = baseIntensity;
        this.distanceByNodeId = Collections.unmodifiableMap(new LinkedHashMap<>(distanceByNodeId));
        this.intensityByNodeId = Collections.unmodifiableMap(new LinkedHashMap<>(intensityByNodeId));
        this.materialOcclusionByNodeId = Collections.unmodifiableMap(new LinkedHashMap<>(materialOcclusionByNodeId));
        this.revealNodeIds = List.copyOf(revealNodeIds);
    }

    public String sourceNodeId() {
        return sourceNodeId;
    }

    public float baseIntensity() {
        return baseIntensity;
    }

    public Map<String, Float> distanceByNodeId() {
        return distanceByNodeId;
    }

    public Map<String, Float> intensityByNodeId() {
        return intensityByNodeId;
    }

    public Map<String, Float> materialOcclusionByNodeId() {
        return materialOcclusionByNodeId;
    }

    public List<String> revealNodeIds() {
        return revealNodeIds;
    }

    public float getDistanceOrInfinity(String nodeId) {
        return distanceByNodeId.getOrDefault(nodeId, Float.POSITIVE_INFINITY);
    }

    public float getIntensityOrZero(String nodeId) {
        return intensityByNodeId.getOrDefault(nodeId, 0f);
    }

    public float getMaterialOcclusionOrDefault(String nodeId, float defaultValue) {
        return materialOcclusionByNodeId.getOrDefault(nodeId, defaultValue);
    }
}

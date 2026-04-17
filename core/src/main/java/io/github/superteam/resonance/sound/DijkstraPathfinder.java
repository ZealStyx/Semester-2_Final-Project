package io.github.superteam.resonance.sound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Computes shortest-path propagation and per-node intensity decay using Dijkstra with a binary heap.
 */
public final class DijkstraPathfinder {
    public DualBandResult onSoundEventDualBand(
        AcousticGraphEngine acousticGraphEngine,
        String sourceNodeId,
        float baseIntensity,
        float lowBandAttenuationAlpha,
        float highBandAttenuationAlpha,
        float revealThreshold
    ) {
        PropagationResult lowBandResult = onSoundEvent(
            acousticGraphEngine,
            sourceNodeId,
            baseIntensity,
            lowBandAttenuationAlpha,
            revealThreshold
        );

        PropagationResult highBandResult = onSoundEvent(
            acousticGraphEngine,
            sourceNodeId,
            baseIntensity,
            highBandAttenuationAlpha,
            revealThreshold
        );

        return new DualBandResult(lowBandResult, highBandResult);
    }

    public PropagationResult onSoundEvent(
        AcousticGraphEngine acousticGraphEngine,
        String sourceNodeId,
        float baseIntensity,
        float attenuationAlpha,
        float revealThreshold
    ) {
        Objects.requireNonNull(acousticGraphEngine, "Acoustic graph engine must not be null.");
        if (acousticGraphEngine.isEmpty()) {
            throw new IllegalArgumentException("Acoustic graph must contain nodes before propagation.");
        }
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            throw new IllegalArgumentException("Source node id must not be blank.");
        }
        if (baseIntensity < 0f) {
            throw new IllegalArgumentException("Base intensity must be non-negative.");
        }
        if (attenuationAlpha < 0f) {
            throw new IllegalArgumentException("Attenuation alpha must be non-negative.");
        }
        if (revealThreshold < 0f) {
            throw new IllegalArgumentException("Reveal threshold must be non-negative.");
        }

        acousticGraphEngine.requireNode(sourceNodeId);

        Map<String, Float> distanceByNodeId = initializeDistanceMap(acousticGraphEngine);
        distanceByNodeId.put(sourceNodeId, 0f);

        Map<String, Float> materialOcclusionByNodeId = initializeMaterialOcclusionMap(acousticGraphEngine);
        materialOcclusionByNodeId.put(sourceNodeId, 1f);

        PriorityQueue<NodeDistance> frontier = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));
        frontier.offer(new NodeDistance(sourceNodeId, 0f));

        while (!frontier.isEmpty()) {
            NodeDistance current = frontier.poll();
            float knownDistance = distanceByNodeId.get(current.nodeId());
            if (current.distance() > knownDistance) {
                continue;
            }

            for (GraphEdge edge : acousticGraphEngine.getAdjacentEdges(current.nodeId())) {
                float edgeWeight = edge.weight();
                if (edgeWeight < 0f || Float.isNaN(edgeWeight) || Float.isInfinite(edgeWeight)) {
                    throw new IllegalArgumentException("Invalid edge weight detected for edge: " + edge);
                }

                String neighbourNodeId = edge.getOtherNodeId(current.nodeId());
                float tentativeDistance = knownDistance + edgeWeight;
                if (tentativeDistance < distanceByNodeId.get(neighbourNodeId)) {
                    distanceByNodeId.put(neighbourNodeId, tentativeDistance);
                    float currentOcclusion = materialOcclusionByNodeId.getOrDefault(current.nodeId(), 0f);
                    float edgeOcclusionFactor = edge.material().transmissionCoefficient() * (1f - edge.material().absorptionCoefficient());
                    float tentativeMaterialOcclusion = Math.max(0f, currentOcclusion * edgeOcclusionFactor);
                    materialOcclusionByNodeId.put(neighbourNodeId, tentativeMaterialOcclusion);
                    frontier.offer(new NodeDistance(neighbourNodeId, tentativeDistance));
                }
            }
        }

        Map<String, Float> intensityByNodeId = new LinkedHashMap<>();
        List<String> revealNodeIds = new ArrayList<>();

        for (Map.Entry<String, Float> distanceEntry : distanceByNodeId.entrySet()) {
            String nodeId = distanceEntry.getKey();
            float distance = distanceEntry.getValue();
            float intensity = calculateIntensity(baseIntensity, attenuationAlpha, distance);
            intensityByNodeId.put(nodeId, intensity);
            if (intensity >= revealThreshold) {
                revealNodeIds.add(nodeId);
            }
        }

        return new PropagationResult(
            sourceNodeId,
            baseIntensity,
            distanceByNodeId,
            intensityByNodeId,
            materialOcclusionByNodeId,
            revealNodeIds
        );
    }

    private Map<String, Float> initializeDistanceMap(AcousticGraphEngine acousticGraphEngine) {
        Map<String, Float> distanceMap = new LinkedHashMap<>();
        for (GraphNode node : acousticGraphEngine.getNodes()) {
            distanceMap.put(node.id(), Float.POSITIVE_INFINITY);
        }
        return distanceMap;
    }

    private Map<String, Float> initializeMaterialOcclusionMap(AcousticGraphEngine acousticGraphEngine) {
        Map<String, Float> occlusionMap = new LinkedHashMap<>();
        for (GraphNode node : acousticGraphEngine.getNodes()) {
            occlusionMap.put(node.id(), 0f);
        }
        return occlusionMap;
    }

    private float calculateIntensity(float baseIntensity, float attenuationAlpha, float distance) {
        if (!Float.isFinite(distance)) {
            return 0f;
        }
        double exponent = -1d * attenuationAlpha * distance;
        return (float) (baseIntensity * Math.exp(exponent));
    }

    private record NodeDistance(String nodeId, float distance) {
    }
}

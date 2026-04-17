package io.github.superteam.resonance.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Computes bounded recursive reflection events from a primary propagation result.
 */
public final class ReflectionEngine {
    private static final float SOUND_SPEED_METERS_PER_SECOND = 343f;
    private static final float MIN_REFLECTION_INTENSITY = 0.08f;

    public List<ReflectionEvent> computeReflections(
        AcousticGraphEngine acousticGraphEngine,
        DijkstraPathfinder dijkstraPathfinder,
        SoundBalancingConfig soundBalancingConfig,
        PropagationResult primaryPropagationResult
    ) {
        Objects.requireNonNull(acousticGraphEngine, "Acoustic graph engine must not be null.");
        Objects.requireNonNull(dijkstraPathfinder, "Dijkstra pathfinder must not be null.");
        Objects.requireNonNull(soundBalancingConfig, "Sound balancing config must not be null.");
        Objects.requireNonNull(primaryPropagationResult, "Primary propagation result must not be null.");

        if (!soundBalancingConfig.reflectionsEnabled()) {
            return List.of();
        }

        List<ReflectionEvent> reflectionEvents = new ArrayList<>();
        computeReflectionsRecursive(
            acousticGraphEngine,
            dijkstraPathfinder,
            soundBalancingConfig,
            primaryPropagationResult,
            1,
            reflectionEvents
        );
        return reflectionEvents;
    }

    private void computeReflectionsRecursive(
        AcousticGraphEngine acousticGraphEngine,
        DijkstraPathfinder dijkstraPathfinder,
        SoundBalancingConfig soundBalancingConfig,
        PropagationResult incomingPropagationResult,
        int bounceDepth,
        List<ReflectionEvent> reflectionEvents
    ) {
        if (bounceDepth > soundBalancingConfig.maxReflectionDepth()) {
            return;
        }
        if (reflectionEvents.size() >= soundBalancingConfig.maxReflectionsPerEvent()) {
            return;
        }

        for (String revealNodeId : incomingPropagationResult.revealNodeIds()) {
            if (reflectionEvents.size() >= soundBalancingConfig.maxReflectionsPerEvent()) {
                return;
            }

            float incomingIntensity = incomingPropagationResult.getIntensityOrZero(revealNodeId);
            if (incomingIntensity <= MIN_REFLECTION_INTENSITY) {
                continue;
            }

            List<GraphEdge> adjacentEdges = acousticGraphEngine.getAdjacentEdges(revealNodeId);
            for (GraphEdge adjacentEdge : adjacentEdges) {
                float reflectedIntensity = incomingIntensity * adjacentEdge.material().reflectionCoefficient();
                if (reflectedIntensity <= MIN_REFLECTION_INTENSITY) {
                    continue;
                }

                PropagationResult reflectionPropagationResult = dijkstraPathfinder.onSoundEvent(
                    acousticGraphEngine,
                    revealNodeId,
                    reflectedIntensity,
                    soundBalancingConfig.attenuationAlphaForBand(FrequencyBand.HIGH),
                    soundBalancingConfig.revealThreshold()
                );

                float distanceToBounceNode = incomingPropagationResult.getDistanceOrInfinity(revealNodeId);
                float delaySeconds = Float.isFinite(distanceToBounceNode)
                    ? Math.max(0f, distanceToBounceNode / SOUND_SPEED_METERS_PER_SECOND)
                    : 0f;

                reflectionEvents.add(
                    new ReflectionEvent(
                        revealNodeId,
                        reflectedIntensity,
                        delaySeconds,
                        bounceDepth,
                        reflectionPropagationResult
                    )
                );

                if (reflectionEvents.size() >= soundBalancingConfig.maxReflectionsPerEvent()) {
                    return;
                }

                computeReflectionsRecursive(
                    acousticGraphEngine,
                    dijkstraPathfinder,
                    soundBalancingConfig,
                    reflectionPropagationResult,
                    bounceDepth + 1,
                    reflectionEvents
                );
            }
        }
    }
}

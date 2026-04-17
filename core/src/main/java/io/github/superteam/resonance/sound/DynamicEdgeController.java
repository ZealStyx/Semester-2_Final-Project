package io.github.superteam.resonance.sound;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Applies runtime acoustic edge changes (for example, opening and closing doors).
 */
public final class DynamicEdgeController {
    private final AcousticGraphEngine acousticGraphEngine;
    private final PropagationCache propagationCache;
    private final ConcurrentMap<UndirectedEdgeKey, DoorMaterialState> closedDoorMaterialByEdge = new ConcurrentHashMap<>();

    public DynamicEdgeController(AcousticGraphEngine acousticGraphEngine, PropagationCache propagationCache) {
        this.acousticGraphEngine = Objects.requireNonNull(acousticGraphEngine, "Acoustic graph engine must not be null.");
        this.propagationCache = Objects.requireNonNull(propagationCache, "Propagation cache must not be null.");
    }

    public boolean setDoorState(String fromNodeId, String toNodeId, boolean open) {
        if (fromNodeId == null || fromNodeId.isBlank() || toNodeId == null || toNodeId.isBlank()) {
            return false;
        }

        GraphEdge existingDoorEdge = acousticGraphEngine
            .findUndirectedEdge(fromNodeId, toNodeId)
            .orElse(null);
        if (existingDoorEdge == null) {
            return false;
        }

        UndirectedEdgeKey edgeKey = UndirectedEdgeKey.of(fromNodeId, toNodeId);
        if (existingDoorEdge.edgeType() != EdgeType.DOOR_OPEN) {
            closedDoorMaterialByEdge.putIfAbsent(
                edgeKey,
                new DoorMaterialState(existingDoorEdge.material(), Math.max(0f, existingDoorEdge.thickness()))
            );
        }

        DoorMaterialState closedDoorMaterialState = closedDoorMaterialByEdge.get(edgeKey);

        AcousticMaterial updatedMaterial = open
            ? AcousticMaterial.AIR
            : closedDoorMaterialState != null ? closedDoorMaterialState.material() : existingDoorEdge.material();
        float updatedThickness = open
            ? 0f
            : closedDoorMaterialState != null ? closedDoorMaterialState.thickness() : Math.max(0f, existingDoorEdge.thickness());
        EdgeType updatedEdgeType = open ? EdgeType.DOOR_OPEN : EdgeType.DOOR_CLOSED;

        GraphEdge updatedEdge = new GraphEdge(
            existingDoorEdge.fromNodeId(),
            existingDoorEdge.toNodeId(),
            existingDoorEdge.distance(),
            updatedMaterial,
            updatedThickness,
            existingDoorEdge.occlusionPenalty(),
            existingDoorEdge.hallwayPenalty(),
            updatedEdgeType
        );

        boolean edgeWasReplaced = acousticGraphEngine.replaceUndirectedEdge(fromNodeId, toNodeId, updatedEdge);
        if (edgeWasReplaced) {
            propagationCache.invalidateNode(fromNodeId);
            propagationCache.invalidateNode(toNodeId);
        }
        return edgeWasReplaced;
    }

    private record UndirectedEdgeKey(String firstNodeId, String secondNodeId) {
        private static UndirectedEdgeKey of(String fromNodeId, String toNodeId) {
            if (fromNodeId.compareTo(toNodeId) <= 0) {
                return new UndirectedEdgeKey(fromNodeId, toNodeId);
            }
            return new UndirectedEdgeKey(toNodeId, fromNodeId);
        }
    }

    private record DoorMaterialState(AcousticMaterial material, float thickness) {
    }
}

package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.Objects;

/**
 * Undirected acoustic edge with the weight components required by the project plan.
 */
public record GraphEdge(
    String fromNodeId,
    String toNodeId,
    float distance,
    AcousticMaterial material,
    float thickness,
    float occlusionPenalty,
    float hallwayPenalty,
    EdgeType edgeType
) {
    private static final float WALL_THICKNESS_PENALTY_PER_METER = 2.5f;
    private static final float TRANSMISSION_RESISTANCE_SCALE = 1.8f;

    public GraphEdge(
        String fromNodeId,
        String toNodeId,
        float distance,
        AcousticMaterial material,
        float occlusionPenalty,
        float hallwayPenalty
    ) {
        this(fromNodeId, toNodeId, distance, material, 0f, occlusionPenalty, hallwayPenalty, EdgeType.PATH);
    }

    public GraphEdge {
        if (fromNodeId == null || fromNodeId.isBlank()) {
            throw new IllegalArgumentException("Source node id must not be blank.");
        }
        if (toNodeId == null || toNodeId.isBlank()) {
            throw new IllegalArgumentException("Target node id must not be blank.");
        }
        if (fromNodeId.equals(toNodeId)) {
            throw new IllegalArgumentException("Graph edges must connect two different nodes.");
        }
        if (distance < 0f) {
            throw new IllegalArgumentException("Edge distance must be non-negative.");
        }
        Objects.requireNonNull(material, "Edge material must not be null.");
        if (thickness < 0f) {
            throw new IllegalArgumentException("Edge thickness must be non-negative.");
        }
        if (occlusionPenalty < 0f) {
            throw new IllegalArgumentException("Occlusion penalty must be non-negative.");
        }
        if (hallwayPenalty < 0f) {
            throw new IllegalArgumentException("Hallway penalty must be non-negative.");
        }
        Objects.requireNonNull(edgeType, "Edge type must not be null.");
    }

    public static GraphEdge between(
        GraphNode fromNode,
        GraphNode toNode,
        AcousticMaterial material,
        float occlusionPenalty,
        float hallwayPenalty
    ) {
        Objects.requireNonNull(fromNode, "Source node must not be null.");
        Objects.requireNonNull(toNode, "Target node must not be null.");
        Vector3 fromPosition = fromNode.position();
        Vector3 toPosition = toNode.position();
        return new GraphEdge(
            fromNode.id(),
            toNode.id(),
            fromPosition.dst(toPosition),
            material,
            0f,
            occlusionPenalty,
            hallwayPenalty,
            EdgeType.PATH
        );
    }

    public static GraphEdge throughWall(
        GraphNode fromNode,
        GraphNode toNode,
        AcousticMaterial material,
        float thickness,
        float occlusionPenalty,
        float hallwayPenalty
    ) {
        Objects.requireNonNull(fromNode, "Source node must not be null.");
        Objects.requireNonNull(toNode, "Target node must not be null.");
        Vector3 fromPosition = fromNode.position();
        Vector3 toPosition = toNode.position();
        return new GraphEdge(
            fromNode.id(),
            toNode.id(),
            fromPosition.dst(toPosition),
            material,
            thickness,
            occlusionPenalty,
            hallwayPenalty,
            EdgeType.WALL
        );
    }

    public static GraphEdge throughTransmission(
        GraphNode fromNode,
        GraphNode toNode,
        AcousticMaterial material,
        float thickness,
        float occlusionPenalty,
        float hallwayPenalty
    ) {
        Objects.requireNonNull(fromNode, "Source node must not be null.");
        Objects.requireNonNull(toNode, "Target node must not be null.");
        Vector3 fromPosition = fromNode.position();
        Vector3 toPosition = toNode.position();
        return new GraphEdge(
            fromNode.id(),
            toNode.id(),
            fromPosition.dst(toPosition),
            material,
            thickness,
            occlusionPenalty,
            hallwayPenalty,
            EdgeType.TRANSMISSION
        );
    }

    public float weight() {
        float baseWeight = (distance * material.traversalMultiplier()) + occlusionPenalty + hallwayPenalty;
        if (edgeType == EdgeType.WALL || edgeType == EdgeType.DOOR_CLOSED || edgeType == EdgeType.TRANSMISSION) {
            float thicknessPenalty = thickness * WALL_THICKNESS_PENALTY_PER_METER;
            float transmissionPenalty = (1f - material.transmissionCoefficient()) * TRANSMISSION_RESISTANCE_SCALE;
            return baseWeight + thicknessPenalty + transmissionPenalty;
        }
        if (edgeType == EdgeType.DOOR_OPEN) {
            return baseWeight * 0.85f;
        }
        if (edgeType == EdgeType.VENT) {
            return baseWeight * 0.9f;
        }
        return baseWeight;
    }

    public boolean touches(String nodeId) {
        return fromNodeId.equals(nodeId) || toNodeId.equals(nodeId);
    }

    public String getOtherNodeId(String nodeId) {
        if (fromNodeId.equals(nodeId)) {
            return toNodeId;
        }
        if (toNodeId.equals(nodeId)) {
            return fromNodeId;
        }
        throw new IllegalArgumentException("Node id is not part of this edge: " + nodeId);
    }
}

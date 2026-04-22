package io.github.superteam.resonance.sound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns the acoustic graph used by sound propagation and visualization.
 */
public final class AcousticGraphEngine {
    private final Map<String, GraphNode> nodesById = new LinkedHashMap<>();
    private final List<GraphEdge> edges = new ArrayList<>();
    private final Map<String, List<GraphEdge>> adjacentEdgesByNodeId = new LinkedHashMap<>();

    public AcousticGraphEngine buildGraphFromGeometry(
        Collection<GraphNode> graphNodes,
        Collection<GraphEdge> graphEdges
    ) {
        Objects.requireNonNull(graphNodes, "Graph nodes must not be null.");
        Objects.requireNonNull(graphEdges, "Graph edges must not be null.");
        if (graphNodes.isEmpty()) {
            throw new IllegalArgumentException("At least one graph node is required.");
        }

        nodesById.clear();
        edges.clear();
        adjacentEdgesByNodeId.clear();

        for (GraphNode node : graphNodes) {
            registerNode(node);
        }
        for (GraphEdge edge : graphEdges) {
            registerEdge(edge);
        }
        return this;
    }

    public List<GraphNode> getNodes() {
        return Collections.unmodifiableList(new ArrayList<>(nodesById.values()));
    }

    public List<GraphEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public List<GraphEdge> getAdjacentEdges(String nodeId) {
        Objects.requireNonNull(nodeId, "Node id must not be null.");
        List<GraphEdge> adjacentEdges = adjacentEdgesByNodeId.get(nodeId);
        if (adjacentEdges == null) {
            return List.of();
        }
        return Collections.unmodifiableList(adjacentEdges);
    }

    public Optional<GraphNode> findNode(String nodeId) {
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    public Optional<GraphEdge> findUndirectedEdge(String firstNodeId, String secondNodeId) {
        if (firstNodeId == null || firstNodeId.isBlank() || secondNodeId == null || secondNodeId.isBlank()) {
            return Optional.empty();
        }
        for (GraphEdge edge : edges) {
            if ((edge.fromNodeId().equals(firstNodeId) && edge.toNodeId().equals(secondNodeId))
                || (edge.fromNodeId().equals(secondNodeId) && edge.toNodeId().equals(firstNodeId))) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
    }

    public boolean replaceUndirectedEdge(String firstNodeId, String secondNodeId, GraphEdge replacementEdge) {
        Objects.requireNonNull(replacementEdge, "Replacement edge must not be null.");
        if (!replacementEdge.touches(firstNodeId) || !replacementEdge.touches(secondNodeId)) {
            throw new IllegalArgumentException("Replacement edge must connect both specified node ids.");
        }

        for (int index = 0; index < edges.size(); index++) {
            GraphEdge existingEdge = edges.get(index);
            if ((existingEdge.fromNodeId().equals(firstNodeId) && existingEdge.toNodeId().equals(secondNodeId))
                || (existingEdge.fromNodeId().equals(secondNodeId) && existingEdge.toNodeId().equals(firstNodeId))) {
                edges.set(index, replacementEdge);
                rebuildAdjacencyIndex();
                return true;
            }
        }
        return false;
    }

    public GraphNode requireNode(String nodeId) {
        GraphNode node = nodesById.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Unknown acoustic graph node: " + nodeId);
        }
        return node;
    }

    public boolean isEmpty() {
        return nodesById.isEmpty();
    }

    private void registerNode(GraphNode node) {
        Objects.requireNonNull(node, "Graph node must not be null.");
        if (nodesById.putIfAbsent(node.id(), node) != null) {
            throw new IllegalArgumentException("Duplicate graph node id: " + node.id());
        }
        adjacentEdgesByNodeId.put(node.id(), new ArrayList<>());
    }

    private void registerEdge(GraphEdge edge) {
        Objects.requireNonNull(edge, "Graph edge must not be null.");
        requireNode(edge.fromNodeId());
        requireNode(edge.toNodeId());
        edges.add(edge);
        adjacentEdgesByNodeId.get(edge.fromNodeId()).add(edge);
        adjacentEdgesByNodeId.get(edge.toNodeId()).add(edge);
    }

    private void rebuildAdjacencyIndex() {
        for (List<GraphEdge> adjacentEdges : adjacentEdgesByNodeId.values()) {
            adjacentEdges.clear();
        }
        for (GraphEdge edge : edges) {
            adjacentEdgesByNodeId.get(edge.fromNodeId()).add(edge);
            adjacentEdgesByNodeId.get(edge.toNodeId()).add(edge);
        }
    }

}

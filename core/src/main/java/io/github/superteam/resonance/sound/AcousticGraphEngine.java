package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
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

    public AcousticGraphEngine buildTestGraph() {
        GraphNode center = new GraphNode("center", new Vector3(0f, 0.2f, 0f));
        GraphNode north = new GraphNode("north", new Vector3(0f, 0.05f, -4.6f));
        GraphNode south = new GraphNode("south", new Vector3(0f, 0.05f, 4.6f));
        GraphNode east = new GraphNode("east", new Vector3(4.6f, 0.05f, 0f));
        GraphNode west = new GraphNode("west", new Vector3(-4.6f, 0.05f, 0f));
        GraphNode northEast = new GraphNode("northEast", new Vector3(3.1f, 0.05f, -4.6f));
        GraphNode northWest = new GraphNode("northWest", new Vector3(-3.1f, 0.05f, -4.6f));
        GraphNode southEast = new GraphNode("southEast", new Vector3(3.1f, 0.05f, 4.6f));
        GraphNode southWest = new GraphNode("southWest", new Vector3(-3.1f, 0.05f, 4.6f));

        List<GraphNode> nodes = List.of(
            center,
            north,
            south,
            east,
            west,
            northEast,
            northWest,
            southEast,
            southWest
        );

        List<GraphEdge> testEdges = new ArrayList<>();
        addBidirectionalTestEdges(testEdges, center, north, AcousticMaterial.CONCRETE, 0f, 0f);
        addBidirectionalTestEdges(testEdges, center, south, AcousticMaterial.CONCRETE, 0f, 0f);
        addBidirectionalTestEdges(testEdges, center, east, AcousticMaterial.CONCRETE, 0f, 0f);
        addBidirectionalTestEdges(testEdges, center, west, AcousticMaterial.CONCRETE, 0f, 0f);

        addBidirectionalTestEdges(testEdges, north, northEast, AcousticMaterial.WOOD, 0.12f, 0.08f);
        addBidirectionalTestEdges(testEdges, north, northWest, AcousticMaterial.WOOD, 0.12f, 0.08f);
        addBidirectionalTestEdges(testEdges, south, southEast, AcousticMaterial.WOOD, 0.12f, 0.08f);
        addBidirectionalTestEdges(testEdges, south, southWest, AcousticMaterial.WOOD, 0.12f, 0.08f);
        addBidirectionalTestEdges(testEdges, east, northEast, AcousticMaterial.METAL, 0.06f, 0.14f);
        addBidirectionalTestEdges(testEdges, east, southEast, AcousticMaterial.METAL, 0.06f, 0.14f);
        addBidirectionalTestEdges(testEdges, west, northWest, AcousticMaterial.METAL, 0.06f, 0.14f);
        addBidirectionalTestEdges(testEdges, west, southWest, AcousticMaterial.METAL, 0.06f, 0.14f);
        addBidirectionalTestEdges(testEdges, northWest, northEast, AcousticMaterial.METAL, 0.08f, 0.16f);
        addBidirectionalTestEdges(testEdges, northEast, southEast, AcousticMaterial.METAL, 0.08f, 0.16f);
        addBidirectionalTestEdges(testEdges, southEast, southWest, AcousticMaterial.METAL, 0.08f, 0.16f);
        addBidirectionalTestEdges(testEdges, southWest, northWest, AcousticMaterial.METAL, 0.08f, 0.16f);

        return buildGraphFromGeometry(nodes, testEdges);
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

    private void addBidirectionalTestEdges(
        List<GraphEdge> testEdges,
        GraphNode source,
        GraphNode target,
        AcousticMaterial material,
        float occlusionPenalty,
        float hallwayPenalty
    ) {
        testEdges.add(GraphEdge.between(source, target, material, occlusionPenalty, hallwayPenalty));
    }
}

package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the shared scaffold graph used by test screens and unit tests.
 */
public final class TestAcousticGraphFactory {
    private TestAcousticGraphFactory() {
    }

    public static AcousticGraphEngine create() {
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

        return new AcousticGraphEngine().buildGraphFromGeometry(nodes, testEdges);
    }

    private static void addBidirectionalTestEdges(
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

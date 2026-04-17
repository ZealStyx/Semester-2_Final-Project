package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import org.junit.Assert;
import org.junit.Test;

public class AcousticGraphEngineTest {
    @Test
    public void buildTestGraphCreatesExpectedNodesAndEdges() {
        AcousticGraphEngine engine = new AcousticGraphEngine().buildTestGraph();

        Assert.assertEquals(9, engine.getNodes().size());
        Assert.assertEquals(16, engine.getEdges().size());
        Assert.assertEquals(4, engine.getAdjacentEdges("center").size());
        Assert.assertTrue(engine.findNode("northEast").isPresent());
    }

    @Test
    public void graphEdgeWeightUsesProjectFormula() {
        GraphNode sourceNode = new GraphNode("source", new Vector3(0f, 0f, 0f));
        GraphNode targetNode = new GraphNode("target", new Vector3(3f, 0f, 4f));

        GraphEdge edge = GraphEdge.between(sourceNode, targetNode, AcousticMaterial.METAL, 0.5f, 0.25f);

        Assert.assertEquals(5f, edge.distance(), 0.0001f);
        Assert.assertEquals((5f * AcousticMaterial.METAL.getMultiplier()) + 0.75f, edge.weight(), 0.0001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateNodeIdsAreRejected() {
        AcousticGraphEngine engine = new AcousticGraphEngine();
        engine.buildGraphFromGeometry(
            java.util.List.of(
                new GraphNode("a", new Vector3(0f, 0f, 0f)),
                new GraphNode("a", new Vector3(1f, 0f, 0f))
            ),
            java.util.List.of()
        );
    }
}

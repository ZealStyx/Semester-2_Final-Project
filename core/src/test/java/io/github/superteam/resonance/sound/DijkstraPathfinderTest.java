package io.github.superteam.resonance.sound;

import org.junit.Assert;
import org.junit.Test;

public class DijkstraPathfinderTest {
    @Test
    public void onSoundEventComputesShortestDistancesAndIntensityDecay() {
        AcousticGraphEngine graphEngine = TestAcousticGraphFactory.create();
        DijkstraPathfinder pathfinder = new DijkstraPathfinder();

        PropagationResult propagationResult = pathfinder.onSoundEvent(graphEngine, "center", 1.0f, 0.35f, 0.25f);

        Assert.assertEquals(0f, propagationResult.getDistanceOrInfinity("center"), 0.0001f);
        Assert.assertTrue(propagationResult.getDistanceOrInfinity("north") > 0f);
        Assert.assertTrue(propagationResult.getDistanceOrInfinity("northEast") >= propagationResult.getDistanceOrInfinity("north"));

        float centerIntensity = propagationResult.getIntensityOrZero("center");
        float outerIntensity = propagationResult.getIntensityOrZero("northEast");
        Assert.assertEquals(1.0f, centerIntensity, 0.0001f);
        Assert.assertTrue(outerIntensity < centerIntensity);
        Assert.assertTrue(propagationResult.revealNodeIds().contains("center"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void onSoundEventRejectsUnknownSourceNode() {
        AcousticGraphEngine graphEngine = TestAcousticGraphFactory.create();
        new DijkstraPathfinder().onSoundEvent(graphEngine, "missing", 1.0f, 0.35f, 0.2f);
    }
}

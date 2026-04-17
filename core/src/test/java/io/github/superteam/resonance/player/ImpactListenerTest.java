package io.github.superteam.resonance.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.items.CarriableItem;
import io.github.superteam.resonance.items.ItemDefinition;
import io.github.superteam.resonance.items.ItemType;
import io.github.superteam.resonance.sound.DijkstraPathfinder;
import io.github.superteam.resonance.sound.PhysicsNoiseEmitter;
import io.github.superteam.resonance.sound.SonarRenderer;
import io.github.superteam.resonance.sound.SoundBalancingConfig;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import io.github.superteam.resonance.sound.AcousticGraphEngine;
import io.github.superteam.resonance.sound.SpatialCueController;
import org.junit.Test;

public class ImpactListenerTest {

    @Test
    public void shouldMapImpulseToIntensityRange() {
        ImpactListener impactListener = createImpactListener();

        assertEquals(0.0f, impactListener.mapImpulseToIntensity(5.0f, 1.0f), 0.0001f);
        assertEquals(1.0f, impactListener.mapImpulseToIntensity(150.0f, 1.4f), 0.0001f);
        assertEquals(0.5f, impactListener.mapImpulseToIntensity(77.5f, 1.0f), 0.01f);
    }

    @Test
    public void shouldIgnoreImpactsBelowThreshold() {
        ImpactListener impactListener = createImpactListener();
        CarriableItem carriableItem = new CarriableItem(
            ItemDefinition.create(ItemType.GLASS_BOTTLE),
            new Vector3(0f, 0f, 0f)
        );

        assertNull(impactListener.onCarriableImpact(carriableItem, new Vector3(0f, 0f, 0f), 3.0f, 1.0f));
    }

    private ImpactListener createImpactListener() {
        AcousticGraphEngine acousticGraphEngine = new AcousticGraphEngine().buildTestGraph();
        SoundPropagationOrchestrator orchestrator = new SoundPropagationOrchestrator(
            acousticGraphEngine,
            new DijkstraPathfinder(),
            new SonarRenderer(),
            new SpatialCueController(),
            SoundBalancingConfig.DEFAULT
        );
        PhysicsNoiseEmitter emitter = new PhysicsNoiseEmitter(orchestrator);
        return new ImpactListener(emitter, ignored -> "center");
    }
}

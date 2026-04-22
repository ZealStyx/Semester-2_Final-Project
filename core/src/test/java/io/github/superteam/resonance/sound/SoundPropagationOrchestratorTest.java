package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class SoundPropagationOrchestratorTest {
    @Test
    public void emitSoundEventDispatchesToEnemyAndDirectorListeners() {
        AcousticGraphEngine graphEngine = TestAcousticGraphFactory.create();
        SonarRenderer sonarRenderer = new SonarRenderer();
        SpatialCueController spatialCueController = new SpatialCueController();
        SoundPropagationOrchestrator orchestrator = new SoundPropagationOrchestrator(
            graphEngine,
            new DijkstraPathfinder(),
            sonarRenderer,
            spatialCueController,
            SoundBalancingConfig.DEFAULT
        );

        RecordingEnemy enemy = new RecordingEnemy("north");
        RecordingDirector director = new RecordingDirector();
        orchestrator.registerEnemyListener(enemy);
        orchestrator.registerDirectorListener(director);

        SoundEventData eventData = SoundEventData.atNode(SoundEvent.CLAP_SHOUT, "center", new Vector3(), 0f);
        PropagationResult result = orchestrator.emitSoundEvent(eventData, 0f);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, enemy.heardIntensities.size());
        Assert.assertEquals(1, director.eventCount);
        Assert.assertFalse(sonarRenderer.snapshot().isEmpty());
    }

    @Test
    public void emitSoundEventRespectsCooldowns() {
        AcousticGraphEngine graphEngine = TestAcousticGraphFactory.create();
        SoundPropagationOrchestrator orchestrator = new SoundPropagationOrchestrator(
            graphEngine,
            new DijkstraPathfinder(),
            new SonarRenderer(),
            new SpatialCueController(),
            SoundBalancingConfig.DEFAULT
        );

        SoundEventData first = SoundEventData.atNode(SoundEvent.FOOTSTEP, "center", new Vector3(), 0f);
        SoundEventData second = SoundEventData.atNode(SoundEvent.FOOTSTEP, "center", new Vector3(), 0.01f);

        Assert.assertNotNull(orchestrator.emitSoundEvent(first, 0f));
        Assert.assertNull(orchestrator.emitSoundEvent(second, 0.01f));
    }

    private static final class RecordingEnemy implements EnemyHearingTarget {
        private final String nodeId;
        private final List<Float> heardIntensities = new ArrayList<>();

        private RecordingEnemy(String nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public String getCurrentNodeId() {
            return nodeId;
        }

        @Override
        public void onSoundHeard(float propagatedIntensity, SoundEventData soundEventData) {
            heardIntensities.add(propagatedIntensity);
        }
    }

    private static final class RecordingDirector implements EchoDirectorListener {
        private int eventCount;

        @Override
        public void onSoundEvent(SoundEventData soundEventData, PropagationResult propagationResult) {
            eventCount++;
        }
    }
}

package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class SonarRendererTest {
    @Test
    public void spawnFromPropagationCreatesAndExpiresReveals() {
        SonarRenderer sonarRenderer = new SonarRenderer();
        SoundBalancingConfig config = SoundBalancingConfig.DEFAULT;

        Map<String, Float> distances = new LinkedHashMap<>();
        distances.put("center", 0f);
        Map<String, Float> intensities = new LinkedHashMap<>();
        intensities.put("center", 1f);

        PropagationResult propagationResult = new PropagationResult(
            "center",
            1f,
            distances,
            intensities,
            List.of("center")
        );
        SoundEventData eventData = SoundEventData.atNode(SoundEvent.CLAP_SHOUT, "center", new Vector3(), 0f);

        sonarRenderer.spawnFromPropagation(eventData, propagationResult, config);
        Assert.assertEquals(1, sonarRenderer.snapshot().size());

        sonarRenderer.update(config.tuningFor(SoundEvent.CLAP_SHOUT).pulseLifetimeSeconds() + 0.1f);
        Assert.assertTrue(sonarRenderer.snapshot().isEmpty());
    }
}

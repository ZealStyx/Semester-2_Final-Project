package io.github.superteam.resonance.rendering.blind;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;
import org.junit.Assert;
import org.junit.Test;

public class BlindEffectControllerTest {
    @Test
    public void clapShoutEventTriggersSonarRevealWhenEnabled() {
        BlindEffectRevealConfig config = new BlindEffectRevealConfig();
        config.sonarReveal.enabled = true;
        config.sonarReveal.triggersAcousticVisualization = true;

        BlindEffectController controller = new BlindEffectController(config, () -> 0f);

        boolean handled = controller.onSoundEvent(
            new SoundEventData(SoundEvent.CLAP_SHOUT, "center", new Vector3(), SoundEvent.CLAP_SHOUT.defaultBaseIntensity(), 0f)
        );

        Assert.assertTrue(handled);

        controller.update(0.1f);
        Assert.assertTrue(controller.visibilityMeters() > config.baselineVisibilityMeters);
    }

    @Test
    public void nonAcousticEventsAreIgnored() {
        BlindEffectController controller = new BlindEffectController(new BlindEffectRevealConfig(), () -> 0f);

        boolean handled = controller.onSoundEvent(
            new SoundEventData(SoundEvent.OBJECT_HIT, "center", new Vector3(), SoundEvent.OBJECT_HIT.defaultBaseIntensity(), 0f)
        );

        Assert.assertFalse(handled);
    }
}
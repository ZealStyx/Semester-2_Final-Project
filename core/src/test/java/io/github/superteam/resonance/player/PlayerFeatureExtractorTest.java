package io.github.superteam.resonance.player;

import com.badlogic.gdx.math.Vector3;
import org.junit.Assert;
import org.junit.Test;

public class PlayerFeatureExtractorTest {

    @Test
    public void emitsCollisionRateFromSampledMovementTicks() {
        PlayerFeatureExtractor extractor = new PlayerFeatureExtractor();
        int[] collisionsBySample = {1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
        StubPlayerController controller = new StubPlayerController(collisionsBySample);

        final PlayerFeatureExtractor.PlayerFeatures[] latestFeatures = new PlayerFeatureExtractor.PlayerFeatures[1];
        extractor.addListener(features -> latestFeatures[0] = features);

        for (int i = 0; i < collisionsBySample.length; i++) {
            extractor.update(0.2f, controller);
        }

        Assert.assertNotNull("Expected features to be emitted after 2 seconds", latestFeatures[0]);
        Assert.assertEquals(0.5f, latestFeatures[0].collisionRate, 0.0001f);
    }

    private static final class StubPlayerController extends PlayerController {
        private final int[] collisionsBySample;
        private final Vector3 position = new Vector3();
        private int sampleIndex;

        private StubPlayerController(int[] collisionsBySample) {
            super();
            this.collisionsBySample = collisionsBySample;
        }

        @Override
        public Vector3 getPosition(Vector3 outPosition) {
            position.x += 0.1f;
            return outPosition.set(position);
        }

        @Override
        public float getHorizontalSpeed() {
            return 1.0f;
        }

        @Override
        public float getYaw() {
            return sampleIndex * 5f;
        }

        @Override
        public int getAndResetCollisionCount() {
            if (sampleIndex >= collisionsBySample.length) {
                return 0;
            }
            return collisionsBySample[sampleIndex++];
        }
    }
}

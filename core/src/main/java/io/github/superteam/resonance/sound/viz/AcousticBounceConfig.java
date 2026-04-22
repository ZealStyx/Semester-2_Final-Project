package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * Runtime settings for dual-layer acoustic bounce visualization.
 */
public final class AcousticBounceConfig {
    public boolean enabled = true;
    public final GraphLayerConfig graphLayer = new GraphLayerConfig();
    public final GeometricLayerConfig geometricLayer = new GeometricLayerConfig();
    public final MicrophoneConfig microphone = new MicrophoneConfig();

    public void validate() {
        graphLayer.validate();
        geometricLayer.validate();
        microphone.validate();
    }

    public static final class GraphLayerConfig {
        public boolean renderEdges = true;
        public final Color colorBounce0 = new Color(0f, 1f, 1f, 0.6f);
        public final Color colorBounce1 = new Color(0.5f, 1f, 0f, 0.6f);
        public final Color colorBounce2 = new Color(1f, 0.7f, 0f, 0.5f);
        public final Color colorBounce3 = new Color(1f, 0f, 0f, 0.4f);
        public float fadeOutSeconds = 2f;

        void validate() {
            fadeOutSeconds = MathUtils.clamp(fadeOutSeconds, 0.2f, 10f);
        }
    }

    public static final class GeometricLayerConfig {
        public boolean renderRays = true;
        public int rayCount = 36;
        public float rayMaxDistanceMeters = 30f;
        public float bounceMarkerScale = 0.3f;
        public final Color bounceMarkerColor = new Color(1f, 1f, 0f, 0.8f);
        public float fadeOutSeconds = 3f;

        void validate() {
            rayCount = MathUtils.clamp(rayCount, 6, 128);
            rayMaxDistanceMeters = MathUtils.clamp(rayMaxDistanceMeters, 1f, 80f);
            bounceMarkerScale = MathUtils.clamp(bounceMarkerScale, 0.05f, 2f);
            fadeOutSeconds = MathUtils.clamp(fadeOutSeconds, 0.2f, 10f);
        }
    }

    public static final class MicrophoneConfig {
        public boolean inputEnabled = true;
        public float sensitivityThreshold = 0.3f;
        public String triggerSoundEvent = "MIC_INPUT";

        void validate() {
            sensitivityThreshold = MathUtils.clamp(sensitivityThreshold, 0.01f, 1.5f);
            if (triggerSoundEvent == null || triggerSoundEvent.isBlank()) {
                triggerSoundEvent = "MIC_INPUT";
            }
        }
    }
}

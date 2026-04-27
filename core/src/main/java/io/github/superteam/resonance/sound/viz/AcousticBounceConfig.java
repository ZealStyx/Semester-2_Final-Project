package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * Runtime settings for dual-layer acoustic bounce visualization.
 */
public final class AcousticBounceConfig {
    public boolean enabled = true;
    public float updateThrottleSeconds = 1f / 30f;
    public final GraphLayerConfig graphLayer = new GraphLayerConfig();
    public final GeometricLayerConfig geometricLayer = new GeometricLayerConfig();
    public final TransmissionConfig transmission = new TransmissionConfig();
    public final MicrophoneConfig microphone = new MicrophoneConfig();

    public void validate() {
        updateThrottleSeconds = MathUtils.clamp(updateThrottleSeconds, 1f / 240f, 0.25f);
        graphLayer.validate();
        geometricLayer.validate();
        transmission.validate();
        microphone.validate();
    }

    public static final class GraphLayerConfig {
        public boolean renderEdges = true;
        public int maxBounceDepth = 3;
        public final Color colorBounce0 = new Color(0f, 1f, 1f, 0.6f);
        public final Color colorBounce1 = new Color(0.5f, 1f, 0f, 0.6f);
        public final Color colorBounce2 = new Color(1f, 0.7f, 0f, 0.5f);
        public final Color colorBounce3 = new Color(1f, 0f, 0f, 0.4f);
        public float fadeOutSeconds = 2f;

        void validate() {
            maxBounceDepth = MathUtils.clamp(maxBounceDepth, 0, 3);
            fadeOutSeconds = MathUtils.clamp(fadeOutSeconds, 0.2f, 10f);
        }
    }

    public static final class GeometricLayerConfig {
        public boolean renderRays = true;
        public int rayCount = 36;
        public int maxBounceDepth = 2;
        public float rayMaxDistanceMeters = 30f;
        public boolean groundEnabled = true;
        public float groundAbsorption = 0.40f;
        public float bounceMarkerScale = 0.3f;
        public final Color bounceMarkerColor = new Color(1f, 1f, 0f, 0.8f);
        public float fadeOutSeconds = 3f;

        void validate() {
            rayCount = MathUtils.clamp(rayCount, 6, 128);
            maxBounceDepth = MathUtils.clamp(maxBounceDepth, 0, 3);
            rayMaxDistanceMeters = MathUtils.clamp(rayMaxDistanceMeters, 1f, 80f);
            groundAbsorption = MathUtils.clamp(groundAbsorption, 0f, 1f);
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

    public static final class TransmissionConfig {
        public boolean enabled = true;
        public float maxTransmissionWeight = 25f;
        public boolean showTransmittedPulse = true;
        public float transmittedPulseColorR = 0.55f;
        public float transmittedPulseColorG = 0.7f;
        public float transmittedPulseColorB = 1.0f;

        void validate() {
            maxTransmissionWeight = MathUtils.clamp(maxTransmissionWeight, 1f, 100f);
            transmittedPulseColorR = MathUtils.clamp(transmittedPulseColorR, 0f, 1f);
            transmittedPulseColorG = MathUtils.clamp(transmittedPulseColorG, 0f, 1f);
            transmittedPulseColorB = MathUtils.clamp(transmittedPulseColorB, 0f, 1f);
        }
    }
}

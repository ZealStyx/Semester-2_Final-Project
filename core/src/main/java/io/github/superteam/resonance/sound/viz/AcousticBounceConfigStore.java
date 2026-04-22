package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Loads acoustic bounce visualization settings from JSON.
 */
public final class AcousticBounceConfigStore {
    private AcousticBounceConfigStore() {
    }

    public static AcousticBounceConfig loadOrDefault(String internalConfigPath) {
        AcousticBounceConfig config = new AcousticBounceConfig();

        if (Gdx.files == null || internalConfigPath == null || internalConfigPath.isBlank()) {
            config.validate();
            return config;
        }

        FileHandle file = Gdx.files.internal(internalConfigPath);
        if (!file.exists()) {
            config.validate();
            return config;
        }

        try {
            JsonValue root = new JsonReader().parse(file);
            config.enabled = root.getBoolean("enabled", config.enabled);
            config.updateThrottleSeconds = root.getFloat("update_throttle_seconds", config.updateThrottleSeconds);

            JsonValue graphLayer = root.get("graph_layer");
            if (graphLayer != null) {
                config.graphLayer.renderEdges = graphLayer.getBoolean("render_edges", config.graphLayer.renderEdges);
                config.graphLayer.maxBounceDepth = graphLayer.getInt("max_bounce_depth", config.graphLayer.maxBounceDepth);
                config.graphLayer.fadeOutSeconds = graphLayer.getFloat("fade_out_seconds", config.graphLayer.fadeOutSeconds);
            }

            JsonValue geometricLayer = root.get("geometric_layer");
            if (geometricLayer != null) {
                config.geometricLayer.renderRays = geometricLayer.getBoolean("render_rays", config.geometricLayer.renderRays);
                config.geometricLayer.rayCount = geometricLayer.getInt("ray_count", config.geometricLayer.rayCount);
                config.geometricLayer.maxBounceDepth = geometricLayer.getInt("max_bounce_depth", config.geometricLayer.maxBounceDepth);
                config.geometricLayer.rayMaxDistanceMeters = geometricLayer.getFloat("ray_max_distance_meters", config.geometricLayer.rayMaxDistanceMeters);
                config.geometricLayer.bounceMarkerScale = geometricLayer.getFloat("bounce_marker_scale", config.geometricLayer.bounceMarkerScale);
                config.geometricLayer.fadeOutSeconds = geometricLayer.getFloat("fade_out_seconds", config.geometricLayer.fadeOutSeconds);
            }

            JsonValue transmission = root.get("transmission");
            if (transmission != null) {
                config.transmission.enabled = transmission.getBoolean("enabled", config.transmission.enabled);
                config.transmission.maxTransmissionWeight = transmission.getFloat("max_transmission_weight", config.transmission.maxTransmissionWeight);
                config.transmission.showTransmittedPulse = transmission.getBoolean("show_transmitted_pulse", config.transmission.showTransmittedPulse);
                config.transmission.transmittedPulseColorR = transmission.getFloat("transmitted_pulse_color_r", config.transmission.transmittedPulseColorR);
                config.transmission.transmittedPulseColorG = transmission.getFloat("transmitted_pulse_color_g", config.transmission.transmittedPulseColorG);
                config.transmission.transmittedPulseColorB = transmission.getFloat("transmitted_pulse_color_b", config.transmission.transmittedPulseColorB);
            }

            JsonValue microphone = root.get("microphone");
            if (microphone != null) {
                config.microphone.inputEnabled = microphone.getBoolean("input_enabled", config.microphone.inputEnabled);
                config.microphone.sensitivityThreshold = microphone.getFloat("sensitivity_threshold", config.microphone.sensitivityThreshold);
                config.microphone.triggerSoundEvent = microphone.getString("trigger_sound_event", config.microphone.triggerSoundEvent);
            }
        } catch (Exception ignored) {
            // Defaults are already set.
        }

        config.validate();
        return config;
    }
}

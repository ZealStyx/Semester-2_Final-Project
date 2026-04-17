package io.github.superteam.resonance.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;
import java.util.EnumMap;

/**
 * Loads configurable sound balancing values from JSON while preserving safe defaults.
 */
public final class SoundBalancingConfigStore {
    private SoundBalancingConfigStore() {
    }

    public static SoundBalancingConfig loadOrDefault(String internalConfigPath) {
        if (Gdx.files == null || internalConfigPath == null || internalConfigPath.isBlank()) {
            return SoundBalancingConfig.DEFAULT;
        }

        FileHandle configFile = Gdx.files.internal(internalConfigPath);
        if (!configFile.exists()) {
            return SoundBalancingConfig.DEFAULT;
        }

        try {
            JsonValue root = new JsonReader().parse(configFile);
            float alpha = root.getFloat("attenuationAlpha", SoundBalancingConfig.DEFAULT.attenuationAlpha());
            float threshold = root.getFloat("revealThreshold", SoundBalancingConfig.DEFAULT.revealThreshold());
            EnumMap<FrequencyBand, Float> attenuationByBand = new EnumMap<>(FrequencyBand.class);
            attenuationByBand.put(FrequencyBand.LOW, root.getFloat("attenuationAlphaLow", SoundBalancingConfig.DEFAULT.attenuationAlphaForBand(FrequencyBand.LOW)));
            attenuationByBand.put(FrequencyBand.HIGH, root.getFloat("attenuationAlphaHigh", SoundBalancingConfig.DEFAULT.attenuationAlphaForBand(FrequencyBand.HIGH)));
            float minReverbIntensity = root.getFloat("minReverbIntensity", SoundBalancingConfig.DEFAULT.minReverbIntensity());
            int maxReverbEchoes = root.getInt("maxReverbEchoes", SoundBalancingConfig.DEFAULT.maxReverbEchoes());
            EnumMap<SoundEvent, SoundBalancingConfig.EventTuning> tunings = new EnumMap<>(SoundEvent.class);

            JsonValue eventTunings = root.get("eventTunings");
            if (eventTunings != null) {
                for (SoundEvent event : SoundEvent.values()) {
                    JsonValue eventConfig = eventTunings.get(event.name());
                    if (eventConfig != null) {
                        float baseIntensity = eventConfig.getFloat("baseIntensity", event.defaultBaseIntensity());
                        float cooldown = eventConfig.getFloat("cooldownSeconds", event.cooldownSeconds());
                        float pulseLifetime = eventConfig.getFloat("pulseLifetimeSeconds", event.pulseLifetimeSeconds());
                        tunings.put(event, new SoundBalancingConfig.EventTuning(baseIntensity, cooldown, pulseLifetime));
                    }
                }
            }

            return new SoundBalancingConfig(
                alpha,
                threshold,
                attenuationByBand,
                tunings,
                true,
                SoundBalancingConfig.DEFAULT.maxReflectionDepth(),
                SoundBalancingConfig.DEFAULT.maxReflectionsPerEvent(),
                minReverbIntensity,
                maxReverbEchoes
            );
        } catch (Exception parseFailure) {
            return SoundBalancingConfig.DEFAULT;
        }
    }
}

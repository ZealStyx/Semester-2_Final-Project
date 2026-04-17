package io.github.superteam.resonance.sound;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tunable propagation and reveal parameters loaded from configuration.
 */
public final class SoundBalancingConfig {
    public static final SoundBalancingConfig DEFAULT = defaultConfig();

    private final float attenuationAlpha;
    private final float revealThreshold;
    private final EnumMap<FrequencyBand, Float> attenuationAlphaByFrequencyBand;
    private final EnumMap<SoundEvent, EventTuning> eventTunings;
    private final boolean reflectionsEnabled;
    private final int maxReflectionDepth;
    private final int maxReflectionsPerEvent;
    private final float minReverbIntensity;
    private final int maxReverbEchoes;

    public SoundBalancingConfig(
        float attenuationAlpha,
        float revealThreshold,
        Map<SoundEvent, EventTuning> eventTunings
    ) {
        this(attenuationAlpha, revealThreshold, null, eventTunings);
    }

    public SoundBalancingConfig(
        float attenuationAlpha,
        float revealThreshold,
        Map<FrequencyBand, Float> attenuationAlphaByFrequencyBand,
        Map<SoundEvent, EventTuning> eventTunings
    ) {
        this(
            attenuationAlpha,
            revealThreshold,
            attenuationAlphaByFrequencyBand,
            eventTunings,
            true,
            2,
            6,
            0.02f,
            16
        );
    }

    public SoundBalancingConfig(
        float attenuationAlpha,
        float revealThreshold,
        Map<FrequencyBand, Float> attenuationAlphaByFrequencyBand,
        Map<SoundEvent, EventTuning> eventTunings,
        boolean reflectionsEnabled,
        int maxReflectionDepth,
        int maxReflectionsPerEvent,
        float minReverbIntensity,
        int maxReverbEchoes
    ) {
        if (attenuationAlpha < 0f) {
            throw new IllegalArgumentException("Attenuation alpha must be non-negative.");
        }
        if (revealThreshold < 0f) {
            throw new IllegalArgumentException("Reveal threshold must be non-negative.");
        }
        this.attenuationAlpha = attenuationAlpha;
        this.revealThreshold = revealThreshold;
        this.attenuationAlphaByFrequencyBand = new EnumMap<>(FrequencyBand.class);
        if (attenuationAlphaByFrequencyBand != null) {
            this.attenuationAlphaByFrequencyBand.putAll(attenuationAlphaByFrequencyBand);
        }
        for (FrequencyBand frequencyBand : FrequencyBand.values()) {
            this.attenuationAlphaByFrequencyBand.putIfAbsent(frequencyBand, frequencyBand.defaultAttenuationAlpha());
        }
        this.eventTunings = new EnumMap<>(SoundEvent.class);
        if (eventTunings != null) {
            this.eventTunings.putAll(eventTunings);
        }
        for (SoundEvent event : SoundEvent.values()) {
            this.eventTunings.putIfAbsent(event, new EventTuning(event.defaultBaseIntensity(), event.cooldownSeconds(), event.pulseLifetimeSeconds()));
        }
        if (maxReflectionDepth < 0) {
            throw new IllegalArgumentException("Max reflection depth must be non-negative.");
        }
        if (maxReflectionsPerEvent < 0) {
            throw new IllegalArgumentException("Max reflections per event must be non-negative.");
        }
        if (minReverbIntensity < 0f) {
            throw new IllegalArgumentException("Min reverb intensity must be non-negative.");
        }
        if (maxReverbEchoes < 0) {
            throw new IllegalArgumentException("Max reverb echoes must be non-negative.");
        }
        this.reflectionsEnabled = reflectionsEnabled;
        this.maxReflectionDepth = maxReflectionDepth;
        this.maxReflectionsPerEvent = maxReflectionsPerEvent;
        this.minReverbIntensity = minReverbIntensity;
        this.maxReverbEchoes = maxReverbEchoes;
    }

    public float attenuationAlpha() {
        return attenuationAlpha;
    }

    public float revealThreshold() {
        return revealThreshold;
    }

    public float attenuationAlphaForBand(FrequencyBand frequencyBand) {
        if (frequencyBand == null) {
            return attenuationAlpha;
        }
        return attenuationAlphaByFrequencyBand.getOrDefault(frequencyBand, attenuationAlpha);
    }

    public EventTuning tuningFor(SoundEvent event) {
        return eventTunings.get(event);
    }

    public boolean reflectionsEnabled() {
        return reflectionsEnabled;
    }

    public int maxReflectionDepth() {
        return maxReflectionDepth;
    }

    public int maxReflectionsPerEvent() {
        return maxReflectionsPerEvent;
    }

    public float minReverbIntensity() {
        return minReverbIntensity;
    }

    public int maxReverbEchoes() {
        return maxReverbEchoes;
    }

    public Map<SoundEvent, EventTuning> eventTunings() {
        return Collections.unmodifiableMap(eventTunings);
    }

    private static SoundBalancingConfig defaultConfig() {
        EnumMap<FrequencyBand, Float> defaultBandAttenuation = new EnumMap<>(FrequencyBand.class);
        defaultBandAttenuation.put(FrequencyBand.LOW, FrequencyBand.LOW.defaultAttenuationAlpha());
        defaultBandAttenuation.put(FrequencyBand.HIGH, FrequencyBand.HIGH.defaultAttenuationAlpha());

        EnumMap<SoundEvent, EventTuning> defaults = new EnumMap<>(SoundEvent.class);
        for (SoundEvent event : SoundEvent.values()) {
            defaults.put(event, new EventTuning(event.defaultBaseIntensity(), event.cooldownSeconds(), event.pulseLifetimeSeconds()));
        }
        return new SoundBalancingConfig(0.35f, 0.22f, defaultBandAttenuation, defaults);
    }

    public record EventTuning(float baseIntensity, float cooldownSeconds, float pulseLifetimeSeconds) {
        public EventTuning {
            if (baseIntensity < 0f) {
                throw new IllegalArgumentException("Base intensity must be non-negative.");
            }
            if (cooldownSeconds < 0f) {
                throw new IllegalArgumentException("Cooldown must be non-negative.");
            }
            if (pulseLifetimeSeconds <= 0f) {
                throw new IllegalArgumentException("Pulse lifetime must be positive.");
            }
        }
    }
}

package io.github.superteam.resonance.sound;

/**
 * Canonical sound events defined in the project plan.
 */
public enum SoundEvent {
    FOOTSTEP(0.35f, 0.15f, 0.55f, FrequencyBand.LOW, EventPriority.LOW, HearingCategory.PLAYER_ACTION),
    FOOTSTEP_RUN(0.55f, 0.10f, 0.70f, FrequencyBand.LOW, EventPriority.LOW, HearingCategory.PLAYER_ACTION),
    JUMP(0.60f, 0.18f, 0.70f, FrequencyBand.LOW, EventPriority.LOW, HearingCategory.PLAYER_ACTION),
    FOOTSTEP_WATER(0.50f, 0.12f, 0.65f, FrequencyBand.HIGH, EventPriority.LOW, HearingCategory.PLAYER_ACTION),
    OBJECT_HIT(0.62f, 0.20f, 0.70f, FrequencyBand.HIGH, EventPriority.MEDIUM, HearingCategory.PLAYER_ACTION),
    OBJECT_DROP_OR_BREAK(0.65f, 0.45f, 0.85f, FrequencyBand.HIGH, EventPriority.MEDIUM, HearingCategory.PLAYER_ACTION),
    CLAP_SHOUT(0.9f, 0.9f, 1.1f, FrequencyBand.HIGH, EventPriority.HIGH, HearingCategory.PLAYER_ACTION),
    ENEMY_SCREAM(0.8f, 1.2f, 1.2f, FrequencyBand.LOW, EventPriority.HIGH, HearingCategory.SCRIPTED_EVENT),
    PHYSICS_COLLAPSE(1.0f, 1.5f, 1.2f, FrequencyBand.LOW, EventPriority.CRITICAL, HearingCategory.SCRIPTED_EVENT),
    DOOR_SLAM(0.75f, 0.50f, 0.90f, FrequencyBand.LOW, EventPriority.MEDIUM, HearingCategory.SCRIPTED_EVENT),
    GLASS_BREAK(0.70f, 0.30f, 0.80f, FrequencyBand.HIGH, EventPriority.MEDIUM, HearingCategory.SCRIPTED_EVENT),
    WATER_DRIP(0.15f, 2.00f, 0.40f, FrequencyBand.HIGH, EventPriority.AMBIENT, HearingCategory.AMBIENCE),
    AMBIENT_CREAK(0.10f, 3.00f, 0.50f, FrequencyBand.LOW, EventPriority.AMBIENT, HearingCategory.AMBIENCE),
    NOISE_DECOY(0.80f, 0.00f, 1.00f, FrequencyBand.HIGH, EventPriority.HIGH, HearingCategory.SCRIPTED_EVENT),
    MIC_INPUT(0.75f, 0.08f, 1.00f, FrequencyBand.HIGH, EventPriority.HIGH, HearingCategory.PLAYER_ACTION);

    private final float defaultBaseIntensity;
    private final float cooldownSeconds;
    private final float pulseLifetimeSeconds;
    private final FrequencyBand frequencyBand;
    private final EventPriority eventPriority;
    private final HearingCategory hearingCategory;

    SoundEvent(
        float defaultBaseIntensity,
        float cooldownSeconds,
        float pulseLifetimeSeconds,
        FrequencyBand frequencyBand,
        EventPriority eventPriority,
        HearingCategory hearingCategory
    ) {
        if (defaultBaseIntensity < 0f) {
            throw new IllegalArgumentException("Default base intensity must be non-negative.");
        }
        if (cooldownSeconds < 0f) {
            throw new IllegalArgumentException("Cooldown seconds must be non-negative.");
        }
        if (pulseLifetimeSeconds <= 0f) {
            throw new IllegalArgumentException("Pulse lifetime seconds must be positive.");
        }
        this.defaultBaseIntensity = defaultBaseIntensity;
        this.cooldownSeconds = cooldownSeconds;
        this.pulseLifetimeSeconds = pulseLifetimeSeconds;
        this.frequencyBand = frequencyBand;
        this.eventPriority = eventPriority;
        this.hearingCategory = hearingCategory;
    }

    public float defaultBaseIntensity() {
        return defaultBaseIntensity;
    }

    public float cooldownSeconds() {
        return cooldownSeconds;
    }

    public float pulseLifetimeSeconds() {
        return pulseLifetimeSeconds;
    }

    public FrequencyBand frequencyBand() {
        return frequencyBand;
    }

    public EventPriority eventPriority() {
        return eventPriority;
    }

    public HearingCategory hearingCategory() {
        return hearingCategory;
    }
}

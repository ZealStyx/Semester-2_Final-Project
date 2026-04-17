package io.github.superteam.resonance.sound;

/**
 * Canonical sound events defined in the project plan.
 */
public enum SoundEvent {
    FOOTSTEP(0.35f, 0.15f, 0.55f, FrequencyBand.LOW, EventPriority.LOW),
    FOOTSTEP_RUN(0.55f, 0.10f, 0.70f, FrequencyBand.LOW, EventPriority.LOW),
    FOOTSTEP_WATER(0.50f, 0.12f, 0.65f, FrequencyBand.HIGH, EventPriority.LOW),
    OBJECT_HIT(0.62f, 0.20f, 0.70f, FrequencyBand.HIGH, EventPriority.MEDIUM),
    OBJECT_DROP_OR_BREAK(0.65f, 0.45f, 0.85f, FrequencyBand.HIGH, EventPriority.MEDIUM),
    CLAP_SHOUT(0.9f, 0.9f, 1.1f, FrequencyBand.HIGH, EventPriority.HIGH),
    ENEMY_SCREAM(0.8f, 1.2f, 1.2f, FrequencyBand.LOW, EventPriority.HIGH),
    PHYSICS_COLLAPSE(1.0f, 1.5f, 1.2f, FrequencyBand.LOW, EventPriority.CRITICAL),
    DOOR_SLAM(0.75f, 0.50f, 0.90f, FrequencyBand.LOW, EventPriority.MEDIUM),
    GLASS_BREAK(0.70f, 0.30f, 0.80f, FrequencyBand.HIGH, EventPriority.MEDIUM),
    WATER_DRIP(0.15f, 2.00f, 0.40f, FrequencyBand.HIGH, EventPriority.AMBIENT),
    AMBIENT_CREAK(0.10f, 3.00f, 0.50f, FrequencyBand.LOW, EventPriority.AMBIENT),
    NOISE_DECOY(0.80f, 0.00f, 1.00f, FrequencyBand.HIGH, EventPriority.HIGH);

    private final float defaultBaseIntensity;
    private final float cooldownSeconds;
    private final float pulseLifetimeSeconds;
    private final FrequencyBand frequencyBand;
    private final EventPriority eventPriority;

    SoundEvent(
        float defaultBaseIntensity,
        float cooldownSeconds,
        float pulseLifetimeSeconds,
        FrequencyBand frequencyBand,
        EventPriority eventPriority
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
}

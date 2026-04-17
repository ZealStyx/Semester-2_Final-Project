package io.github.superteam.resonance.sound;

/**
 * Room-level acoustic profile used to shape reverberation and reflection tails.
 */
public final class RoomAcousticProfile {
    private static final float SABINE_CONSTANT = 0.161f;
    public static final RoomAcousticProfile ANECHOIC = new RoomAcousticProfile(0.1f, 0.01f, 1f, AcousticMaterial.FABRIC, 0f);

    private final float reverbDecaySeconds;
    private final float earlyReflectionDelaySeconds;
    private final float roomVolumeCubicMeters;
    private final AcousticMaterial dominantMaterial;
    private final float wetDryRatio;

    public RoomAcousticProfile(
        float reverbDecaySeconds,
        float earlyReflectionDelaySeconds,
        float roomVolumeCubicMeters,
        AcousticMaterial dominantMaterial,
        float wetDryRatio
    ) {
        if (reverbDecaySeconds <= 0f) {
            throw new IllegalArgumentException("Reverb decay seconds must be positive.");
        }
        if (earlyReflectionDelaySeconds < 0f) {
            throw new IllegalArgumentException("Early reflection delay seconds must be non-negative.");
        }
        if (roomVolumeCubicMeters <= 0f) {
            throw new IllegalArgumentException("Room volume must be positive.");
        }
        if (wetDryRatio < 0f || wetDryRatio > 1f) {
            throw new IllegalArgumentException("Wet/dry ratio must be within [0, 1].");
        }
        this.reverbDecaySeconds = reverbDecaySeconds;
        this.earlyReflectionDelaySeconds = earlyReflectionDelaySeconds;
        this.roomVolumeCubicMeters = roomVolumeCubicMeters;
        this.dominantMaterial = dominantMaterial == null ? AcousticMaterial.AIR : dominantMaterial;
        this.wetDryRatio = wetDryRatio;
    }

    public float reverbDecaySeconds() {
        return reverbDecaySeconds;
    }

    public float earlyReflectionDelaySeconds() {
        return earlyReflectionDelaySeconds;
    }

    public float roomVolumeCubicMeters() {
        return roomVolumeCubicMeters;
    }

    public AcousticMaterial dominantMaterial() {
        return dominantMaterial;
    }

    public float wetDryRatio() {
        return wetDryRatio;
    }

    public static RoomAcousticProfile fromRoomGeometry(
        float widthMeters,
        float depthMeters,
        float heightMeters,
        AcousticMaterial wallMaterial
    ) {
        if (widthMeters <= 0f || depthMeters <= 0f || heightMeters <= 0f) {
            throw new IllegalArgumentException("Room dimensions must be positive.");
        }

        AcousticMaterial dominantMaterial = wallMaterial == null ? AcousticMaterial.CONCRETE : wallMaterial;
        float roomVolume = widthMeters * depthMeters * heightMeters;
        float surfaceArea = 2f * ((widthMeters * depthMeters) + (widthMeters * heightMeters) + (depthMeters * heightMeters));
        float absorptionArea = Math.max(0.001f, dominantMaterial.absorptionCoefficient() * surfaceArea);
        float rt60Seconds = SABINE_CONSTANT * roomVolume / absorptionArea;
        float clampedRt60Seconds = clamp(rt60Seconds, 0.1f, 4f);
        float earlyReflectionDelay = roomVolume < 50f ? 0.02f : 0.05f;
        float wetDryRatio = clamp(1f - dominantMaterial.absorptionCoefficient(), 0f, 0.9f);

        return new RoomAcousticProfile(
            clampedRt60Seconds,
            earlyReflectionDelay,
            roomVolume,
            dominantMaterial,
            wetDryRatio
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

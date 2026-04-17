package io.github.superteam.resonance.particles;

public enum ParticleCollisionResponse {
    DIE,
    STICK,
    BOUNCE;

    public static ParticleCollisionResponse fromName(String value) {
        if (value == null || value.isBlank()) {
            return DIE;
        }

        for (ParticleCollisionResponse response : values()) {
            if (response.name().equalsIgnoreCase(value)) {
                return response;
            }
        }
        return DIE;
    }
}

package io.github.superteam.resonance.particles;

public enum DepthMode {
    WRITE,
    IGNORE,
    READ_ONLY;

    public static DepthMode fromName(String value) {
        if (value == null || value.isBlank()) {
            return WRITE;
        }

        for (DepthMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }

        return WRITE;
    }
}

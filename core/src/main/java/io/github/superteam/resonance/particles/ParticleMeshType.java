package io.github.superteam.resonance.particles;

public enum ParticleMeshType {
    QUAD,
    CUBE,
    TETRAHEDRON,
    OCTAHEDRON,
    ICOSAHEDRON,
    CAPSULE,
    PYRAMID,
    CUSTOM;

    public static ParticleMeshType fromName(String value) {
        if (value == null || value.isBlank()) {
            return CUBE;
        }

        for (ParticleMeshType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        return CUBE;
    }
}

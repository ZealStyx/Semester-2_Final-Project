package io.github.superteam.resonance.sound;

import com.badlogic.gdx.graphics.Color;

/**
 * Shared wall categories used by the test scene and future acoustic geometry.
 */
public enum WallType {
    CONCRETE(AcousticMaterial.CONCRETE, new Color(0.30f, 0.34f, 0.40f, 1f)),
    WOOD(AcousticMaterial.WOOD, new Color(0.48f, 0.34f, 0.22f, 1f)),
    METAL(AcousticMaterial.METAL, new Color(0.60f, 0.63f, 0.68f, 1f)),
    GLASS(AcousticMaterial.GLASS, new Color(0.42f, 0.72f, 0.85f, 1f)),
    FABRIC(AcousticMaterial.FABRIC, new Color(0.48f, 0.30f, 0.42f, 1f));

    private final AcousticMaterial acousticMaterial;
    private final Color displayColor;

    WallType(AcousticMaterial acousticMaterial, Color displayColor) {
        this.acousticMaterial = acousticMaterial;
        this.displayColor = displayColor;
    }

    public AcousticMaterial acousticMaterial() {
        return acousticMaterial;
    }

    public Color displayColor() {
        return new Color(displayColor);
    }
}

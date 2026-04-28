package io.github.superteam.resonance.lighting;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * Mutable runtime light descriptor used by {@link LightManager}.
 */
public final class GameLight {
    private final String id;
    private final Vector3 position;
    private final Color color;
    private float radius;
    private float baseIntensity;
    private float currentIntensity;
    private boolean flickerEnabled;
    private boolean broken;

    public GameLight(
        String id,
        Vector3 position,
        Color color,
        float radius,
        float baseIntensity,
        boolean flickerEnabled
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Light id must not be blank.");
        }
        if (position == null) {
            throw new IllegalArgumentException("Light position must not be null.");
        }
        if (color == null) {
            throw new IllegalArgumentException("Light color must not be null.");
        }
        this.id = id;
        this.position = new Vector3(position);
        this.color = new Color(color);
        this.radius = Math.max(0.01f, radius);
        this.baseIntensity = MathUtils.clamp(baseIntensity, 0f, 2f);
        this.currentIntensity = this.baseIntensity;
        this.flickerEnabled = flickerEnabled;
        this.broken = false;
    }

    public String id() {
        return id;
    }

    public Vector3 position() {
        return position;
    }

    public Color color() {
        return color;
    }

    public float radius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = Math.max(0.01f, radius);
    }

    public float baseIntensity() {
        return baseIntensity;
    }

    public void setBaseIntensity(float baseIntensity) {
        this.baseIntensity = MathUtils.clamp(baseIntensity, 0f, 2f);
    }

    public float currentIntensity() {
        return currentIntensity;
    }

    public void setCurrentIntensity(float currentIntensity) {
        this.currentIntensity = MathUtils.clamp(currentIntensity, 0f, 2f);
    }

    public boolean flickerEnabled() {
        return flickerEnabled;
    }

    public void setFlickerEnabled(boolean flickerEnabled) {
        this.flickerEnabled = flickerEnabled;
    }

    public boolean broken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
        if (broken) {
            this.currentIntensity = 0f;
        }
    }
}

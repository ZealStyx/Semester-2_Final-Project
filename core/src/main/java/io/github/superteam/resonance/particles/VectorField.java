package io.github.superteam.resonance.particles;

import com.badlogic.gdx.math.Vector3;

public class VectorField implements ForceField {
    private final Vector3 direction = new Vector3(1f, 0f, 0f);
    private final Vector3 minBounds = new Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    private final Vector3 maxBounds = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    private float strength = 1f;

    public VectorField() {
    }

    public VectorField(Vector3 direction, float strength) {
        setDirection(direction);
        this.strength = strength;
    }

    public VectorField setDirection(Vector3 newDirection) {
        if (newDirection == null || newDirection.isZero(0.0001f)) {
            direction.set(1f, 0f, 0f);
        } else {
            direction.set(newDirection).nor();
        }
        return this;
    }

    public VectorField setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    public VectorField setBounds(Vector3 min, Vector3 max) {
        if (min == null || max == null) {
            clearBounds();
            return this;
        }

        minBounds.set(Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z));
        maxBounds.set(Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z));
        return this;
    }

    public VectorField clearBounds() {
        minBounds.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        maxBounds.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        return this;
    }

    @Override
    public void sample(Vector3 position, float delta, Vector3 outForce) {
        outForce.setZero();
        if (!contains(position)) {
            return;
        }

        outForce.set(direction).scl(strength * delta);
    }

    private boolean contains(Vector3 position) {
        return position.x >= minBounds.x
            && position.x <= maxBounds.x
            && position.y >= minBounds.y
            && position.y <= maxBounds.y
            && position.z >= minBounds.z
            && position.z <= maxBounds.z;
    }
}

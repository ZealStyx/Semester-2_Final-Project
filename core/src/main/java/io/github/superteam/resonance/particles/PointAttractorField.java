package io.github.superteam.resonance.particles;

import com.badlogic.gdx.math.Vector3;

public class PointAttractorField implements ForceField {
    private final Vector3 position = new Vector3();
    private float strength = 4f;
    private float falloff = 2f;
    private float maxDistance = 0f;

    public PointAttractorField() {
    }

    public PointAttractorField(Vector3 position, float strength) {
        setPosition(position);
        this.strength = strength;
    }

    public PointAttractorField setPosition(Vector3 newPosition) {
        if (newPosition == null) {
            position.setZero();
        } else {
            position.set(newPosition);
        }
        return this;
    }

    public PointAttractorField setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    public PointAttractorField setFalloff(float falloff) {
        this.falloff = Math.max(0.001f, falloff);
        return this;
    }

    public PointAttractorField setMaxDistance(float maxDistance) {
        this.maxDistance = Math.max(0f, maxDistance);
        return this;
    }

    @Override
    public void sample(Vector3 samplePosition, float delta, Vector3 outForce) {
        outForce.setZero();
        float deltaX = position.x - samplePosition.x;
        float deltaY = position.y - samplePosition.y;
        float deltaZ = position.z - samplePosition.z;
        float distance = (float) Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
        if (distance <= 0.0001f) {
            return;
        }

        if (maxDistance > 0f && distance > maxDistance) {
            return;
        }

        float distanceFactor = 1f / (1f + (distance * falloff));
        outForce.set(deltaX / distance, deltaY / distance, deltaZ / distance).scl(strength * distanceFactor * delta);
    }
}

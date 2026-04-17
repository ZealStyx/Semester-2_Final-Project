package io.github.superteam.resonance.particles;

import com.badlogic.gdx.math.Vector3;

public class CollisionPlane {
    private final Vector3 normal = new Vector3(0f, 1f, 0f);
    private float distance;

    public CollisionPlane() {
    }

    public CollisionPlane(Vector3 normal, float distance) {
        set(normal, distance);
    }

    public CollisionPlane set(Vector3 normal, float distance) {
        if (normal == null || normal.isZero(0.0001f)) {
            this.normal.set(0f, 1f, 0f);
        } else {
            this.normal.set(normal).nor();
        }
        this.distance = distance;
        return this;
    }

    public Vector3 getNormal(Vector3 outNormal) {
        if (outNormal == null) {
            outNormal = new Vector3();
        }
        outNormal.set(normal);
        return outNormal;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float signedDistance(Vector3 position) {
        return normal.dot(position) + distance;
    }

    public boolean isInside(Vector3 position) {
        return signedDistance(position) < 0f;
    }

    public Vector3 project(Vector3 position, Vector3 outPosition) {
        if (outPosition == null) {
            outPosition = new Vector3();
        }

        float signedDistance = signedDistance(position);
        return outPosition.set(position).mulAdd(normal, -signedDistance);
    }
}

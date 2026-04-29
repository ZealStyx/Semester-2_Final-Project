package io.github.superteam.resonance.prop;

public final class PropPhysicsHints {
    public float mass = 1f;
    public boolean kinematic;
    public boolean sensor;
    public boolean collidable = true;

    public PropPhysicsHints() {
    }

    public PropPhysicsHints(float mass, boolean kinematic, boolean sensor, boolean collidable) {
        this.mass = mass;
        this.kinematic = kinematic;
        this.sensor = sensor;
        this.collidable = collidable;
    }
}

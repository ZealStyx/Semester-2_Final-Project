package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Base abstraction for modular Universal Test Scene zones.
 */
public abstract class TestZone {
    public abstract void setUp();

    public abstract void update(float deltaSeconds);

    public abstract void render(PerspectiveCamera camera);

    public abstract String getZoneName();

    public abstract SystemState getSystemState();

    public abstract Vector3 getCenter();

    public Array<ColliderDescriptor> getColliders() {
        return new Array<>();
    }

    public float getActivationRadius() {
        return 6.0f;
    }

    public void onEnter() {
    }

    public void onExit() {
    }
}

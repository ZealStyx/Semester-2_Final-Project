package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

/**
 * Shared implementation used by simple shell zones.
 */
public abstract class BaseShellZone extends TestZone {
    private final String zoneName;
    private final Vector3 center;
    private final float activationRadius;
    private final ZoneSystemState state = new ZoneSystemState();
    private float elapsed;

    protected BaseShellZone(String zoneName, Vector3 center, float activationRadius) {
        this.zoneName = zoneName;
        this.center = new Vector3(center);
        this.activationRadius = activationRadius;
    }

    @Override
    public void setUp() {
        state.put("zone", zoneName);
        state.put("status", "shell-ready");
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += Math.max(0.0f, deltaSeconds);
        state.put("elapsedSeconds", String.format("%.1f", elapsed));
    }

    @Override
    public void render(PerspectiveCamera camera) {
        // Geometry shell is intentionally minimal for the first task slice.
    }

    @Override
    public String getZoneName() {
        return zoneName;
    }

    @Override
    public SystemState getSystemState() {
        return state;
    }

    @Override
    public Vector3 getCenter() {
        return center;
    }

    @Override
    public float getActivationRadius() {
        return activationRadius;
    }

    protected ZoneSystemState mutableState() {
        return state;
    }
}

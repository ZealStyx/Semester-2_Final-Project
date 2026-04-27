package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;
import io.github.superteam.resonance.devTest.universal.ColliderDescriptor;

public final class CrouchAlcoveZone extends BaseShellZone {
    private static final float CEILING_Y = 1.1f;
    private static final float CEILING_WIDTH = 6.0f;
    private static final float CEILING_HEIGHT = 0.2f;
    private static final float CEILING_DEPTH = 8.0f;
    private static final float SIDE_WALL_OFFSET_X = 3.1f;
    private static final float SIDE_WALL_Y = 0.55f;
    private static final float SIDE_WALL_WIDTH = 0.2f;
    private static final float SIDE_WALL_HEIGHT = 1.1f;
    private static final float SIDE_WALL_DEPTH = 8.0f;

    private float simulationTime;

    public CrouchAlcoveZone(Vector3 center) {
        super("Crouch Alcove", center, 6.0f);
    }

    @Override
    public void setUp() {
        super.setUp();
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        simulationTime += Math.max(0f, deltaSeconds);
        mutableState().put("crouchRequired", "true");
        mutableState().put("inventorySlots", "4");
        mutableState().put("carryLoadHint", String.format("%.1f", 1.0f + (float) Math.sin(simulationTime) * 0.5f));
    }

    @Override
    public Array<ColliderDescriptor> getColliders() {
        Vector3 center = getCenter();
        Array<ColliderDescriptor> colliders = new Array<>();
        colliders.add(new ColliderDescriptor(center.x, CEILING_Y, center.z, CEILING_WIDTH, CEILING_HEIGHT, CEILING_DEPTH));
        colliders.add(new ColliderDescriptor(center.x - SIDE_WALL_OFFSET_X, SIDE_WALL_Y, center.z,
            SIDE_WALL_WIDTH, SIDE_WALL_HEIGHT, SIDE_WALL_DEPTH));
        colliders.add(new ColliderDescriptor(center.x + SIDE_WALL_OFFSET_X, SIDE_WALL_Y, center.z,
            SIDE_WALL_WIDTH, SIDE_WALL_HEIGHT, SIDE_WALL_DEPTH));
        return colliders;
    }
}

package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;

public final class CrouchAlcoveZone extends BaseShellZone {
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
}

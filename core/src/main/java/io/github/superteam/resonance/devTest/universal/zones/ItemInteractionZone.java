package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;

public final class ItemInteractionZone extends BaseShellZone {
    private float time;

    public ItemInteractionZone(Vector3 center) {
        super("Item Interaction", center, 7.0f);
    }

    @Override
    public void setUp() {
        super.setUp();
        mutableState().put("inventoryHook", "InventorySystem-ready");
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        time += Math.max(0f, deltaSeconds);
        mutableState().put("inventoryState", "active");
        mutableState().put("nearbyItems", Integer.toString(5 + Math.round(Math.abs((float) Math.sin(time)) * 4f)));
        mutableState().put("flareSlots", "0/3..3/3");
    }
}

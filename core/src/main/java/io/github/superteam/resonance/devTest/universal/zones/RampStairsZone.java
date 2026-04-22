package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;

public final class RampStairsZone extends BaseShellZone {
    private float footstepAccumulator;

    public RampStairsZone(Vector3 center) {
        super("Ramp/Stairs", center, 7.5f);
    }

    @Override
    public void setUp() {
        super.setUp();
        mutableState().put("movementHook", "PlayerFootstepSoundEmitter-ready");
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        footstepAccumulator += Math.max(0f, deltaSeconds) * 2.4f;
        mutableState().put("movementState", "walk-test");
        mutableState().put("footstepFrequencyHz", String.format("%.2f", 1.6f + (float) Math.sin(footstepAccumulator) * 0.4f));
    }
}

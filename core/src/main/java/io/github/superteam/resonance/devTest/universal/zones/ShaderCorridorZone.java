package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;

public final class ShaderCorridorZone extends BaseShellZone {
    private float time;

    public ShaderCorridorZone(Vector3 center) {
        super("Shader Corridor", center, 7.0f);
    }

    @Override
    public void setUp() {
        super.setUp();
        mutableState().put("shaderHook", "retro+bodycam-ready");
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        time += Math.max(0f, deltaSeconds);
        mutableState().put("retroShader", "enabled");
        mutableState().put("bodyCamDistortion", String.format("%.2f", 0.30f + ((float) Math.sin(time) * 0.08f)));
        mutableState().put("chromaticPixels", String.format("%.2f", 2.2f + Math.abs((float) Math.cos(time * 0.5f))));
    }
}

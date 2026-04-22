package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;

public final class BlindChamberZone extends BaseShellZone {
    private float time;

    public BlindChamberZone(Vector3 center) {
        super("Blind Chamber", center, 7.0f);
    }

    @Override
    public void setUp() {
        super.setUp();
        mutableState().put("blindHook", "BlindEffectController-ready");
        mutableState().put("sonarHook", "CLAP_SHOUT->reveal-ready");
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        time += Math.max(0f, deltaSeconds);
        float panic = Math.abs((float) Math.sin(time * 0.7f));
        mutableState().put("baselineVisibility", "0.60m");
        mutableState().put("sonarRadius", "2.50m");
        mutableState().put("flareRadius", "4.00m");
        mutableState().put("panicLevel", String.format("%.2f", panic));
    }
}

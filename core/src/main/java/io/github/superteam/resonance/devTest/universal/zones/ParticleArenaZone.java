package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;

public final class ParticleArenaZone extends BaseShellZone {
    private float time;

    public ParticleArenaZone(Vector3 center) {
        super("Particle Arena", center, 8.5f);
    }

    @Override
    public void setUp() {
        super.setUp();
        mutableState().put("particleHook", "EmitterPipeline-ready");
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        time += Math.max(0f, deltaSeconds);
        int emitterCount = 3;
        int particleCount = 220 + Math.round(Math.abs((float) Math.sin(time * 1.4f)) * 320f);
        mutableState().put("emitters", Integer.toString(emitterCount));
        mutableState().put("particleCount", Integer.toString(particleCount));
        mutableState().put("memoryMB", String.format("%.1f", 12.5f + (particleCount * 0.01f)));
    }
}

package io.github.superteam.resonance.particles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class TurbulenceField implements ForceField {
    private float strength = 1f;
    private float scale = 0.5f;
    private float speed = 0.3f;
    private float elapsed;

    public TurbulenceField setStrength(float strength) {
        this.strength = Math.max(0f, strength);
        return this;
    }

    public TurbulenceField setScale(float scale) {
        this.scale = Math.max(0.001f, scale);
        return this;
    }

    public TurbulenceField setSpeed(float speed) {
        this.speed = Math.max(0f, speed);
        return this;
    }

    @Override
    public void sample(Vector3 position, float delta, Vector3 outForce) {
        elapsed += Math.max(0f, delta);

        float sampleX = (position.x * scale) + (elapsed * speed);
        float sampleY = (position.y * scale) + (elapsed * speed * 0.7f);
        float sampleZ = (position.z * scale) + (elapsed * speed * 0.9f);

        float noiseX = noiseApprox(sampleX + 0f, sampleY + 1.3f, sampleZ + 2.7f);
        float noiseY = noiseApprox(sampleX + 3.1f, sampleY + 0f, sampleZ + 1.5f);
        float noiseZ = noiseApprox(sampleX + 1.9f, sampleY + 4.2f, sampleZ + 0f);

        outForce.set(noiseX, noiseY, noiseZ).scl(strength * delta);
    }

    private float noiseApprox(float x, float y, float z) {
        return (
            MathUtils.sin((x * 1.7f) + (y * 0.9f))
                + MathUtils.sin((y * 1.3f) + (z * 1.1f))
                + MathUtils.sin((z * 0.8f) + (x * 1.5f))
        ) / 3f;
    }
}

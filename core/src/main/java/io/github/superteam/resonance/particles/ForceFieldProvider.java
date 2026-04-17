package io.github.superteam.resonance.particles;

import com.badlogic.gdx.math.Vector3;

public interface ForceFieldProvider {
    void sampleForce(Vector3 position, float delta, Vector3 outForce);
}

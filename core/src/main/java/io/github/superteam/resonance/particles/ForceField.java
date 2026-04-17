package io.github.superteam.resonance.particles;

import com.badlogic.gdx.math.Vector3;

public interface ForceField {
    void sample(Vector3 position, float delta, Vector3 outForce);
}

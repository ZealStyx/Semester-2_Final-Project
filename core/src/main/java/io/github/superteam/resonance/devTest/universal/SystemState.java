package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Exposes zone metrics for diagnostics.
 */
public interface SystemState {
    ObjectMap<String, String> getMetrics();

    boolean isHealthy();
}

package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Mutable SystemState used by zone shells.
 */
public final class ZoneSystemState implements SystemState {
    private final ObjectMap<String, String> metrics = new ObjectMap<>();
    private boolean healthy = true;

    @Override
    public ObjectMap<String, String> getMetrics() {
        return metrics;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public void put(String key, String value) {
        metrics.put(key, value);
    }
}

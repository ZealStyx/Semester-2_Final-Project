package io.github.superteam.resonance.devTest.universal.diagnostics;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.github.superteam.resonance.devTest.universal.SystemState;

public final class SystemStatePanel {
    public Array<String> lines(SystemState state) {
        Array<String> result = new Array<>();
        if (state == null) {
            result.add("No active zone state");
            return result;
        }

        result.add("Healthy=" + state.isHealthy());
        for (ObjectMap.Entry<String, String> entry : state.getMetrics()) {
            result.add(entry.key + "=" + entry.value);
        }
        return result;
    }
}

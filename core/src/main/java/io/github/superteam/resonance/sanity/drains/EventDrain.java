package io.github.superteam.resonance.sanity.drains;

import io.github.superteam.resonance.sanity.SanityDrainSource;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Queues immediate sanity deltas from scripted gameplay events.
 */
public final class EventDrain implements SanityDrainSource {
    private float queuedImmediateDelta;

    @Override
    public float drainPerSecond(SanitySystem.Context context) {
        return 0f;
    }

    @Override
    public float immediateDelta(SanitySystem.Context context) {
        float value = queuedImmediateDelta;
        queuedImmediateDelta = 0f;
        return value;
    }

    public void queueDelta(float delta) {
        queuedImmediateDelta += delta;
    }
}

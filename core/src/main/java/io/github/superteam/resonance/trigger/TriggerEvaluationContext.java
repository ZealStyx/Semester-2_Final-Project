package io.github.superteam.resonance.trigger;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.event.EventState;

/**
 * Runtime data available while evaluating triggers.
 */
public final class TriggerEvaluationContext {
    private final Vector3 playerPosition;
    private final float elapsedSeconds;
    private final EventState eventState;

    public TriggerEvaluationContext(Vector3 playerPosition, float elapsedSeconds, EventState eventState) {
        this.playerPosition = playerPosition == null ? new Vector3() : new Vector3(playerPosition);
        this.elapsedSeconds = Math.max(0f, elapsedSeconds);
        this.eventState = eventState;
    }

    public Vector3 playerPosition() {
        return new Vector3(playerPosition);
    }

    public float elapsedSeconds() {
        return elapsedSeconds;
    }

    public EventState eventState() {
        return eventState;
    }
}

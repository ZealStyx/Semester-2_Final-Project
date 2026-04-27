package io.github.superteam.resonance.trigger.conditions;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import io.github.superteam.resonance.trigger.Trigger;
import io.github.superteam.resonance.trigger.TriggerEvaluationContext;

/**
 * Fires when the player enters a configured axis-aligned volume.
 */
public final class ZoneTrigger extends Trigger {
    private final BoundingBox volume;
    private final Vector3 center = new Vector3();

    public ZoneTrigger(String id, String targetEventId, float cooldownSeconds, BoundingBox volume) {
        super(id, targetEventId, cooldownSeconds);
        if (volume == null) {
            throw new IllegalArgumentException("Zone volume must not be null.");
        }
        this.volume = new BoundingBox(volume);
        this.volume.getCenter(center);
    }

    @Override
    protected boolean evaluate(TriggerEvaluationContext context) {
        return context != null && volume.contains(context.playerPosition());
    }

    @Override
    public Vector3 triggerPosition(TriggerEvaluationContext context) {
        return new Vector3(center);
    }
}

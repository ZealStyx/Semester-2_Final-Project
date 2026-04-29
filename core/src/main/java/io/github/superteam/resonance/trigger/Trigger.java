package io.github.superteam.resonance.trigger;

import com.badlogic.gdx.math.Vector3;

/**
 * Base trigger contract with enter-edge firing and cooldown support.
 */
public abstract class Trigger {
    private final String id;
    private final String targetEventId;
    private final float cooldownSeconds;
    private float cooldownRemaining;
    private boolean wasConditionMetLastFrame;

    protected Trigger(String id, String targetEventId, float cooldownSeconds) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Trigger id must not be blank.");
        }
        if (targetEventId == null || targetEventId.isBlank()) {
            throw new IllegalArgumentException("Target event id must not be blank.");
        }
        this.id = id;
        this.targetEventId = targetEventId;
        this.cooldownSeconds = Math.max(0f, cooldownSeconds);
    }

    public final String id() {
        return id;
    }

    public final String targetEventId() {
        return targetEventId;
    }

    public final void update(float deltaSeconds) {
        cooldownRemaining = Math.max(0f, cooldownRemaining - Math.max(0f, deltaSeconds));
    }

    public final boolean shouldFire(TriggerEvaluationContext context) {
        boolean isConditionMet = evaluate(context);
        boolean shouldFire = isConditionMet && !wasConditionMetLastFrame && cooldownRemaining <= 0f;
        wasConditionMetLastFrame = isConditionMet;
        if (shouldFire) {
            cooldownRemaining = cooldownSeconds;
        }
        return shouldFire;
    }

    public Vector3 triggerPosition(TriggerEvaluationContext context) {
        return context == null ? new Vector3() : context.playerPosition();
    }

    protected abstract boolean evaluate(TriggerEvaluationContext context);

    public boolean isEvaluate(TriggerEvaluationContext context) {
        return evaluate(context);
    }
}

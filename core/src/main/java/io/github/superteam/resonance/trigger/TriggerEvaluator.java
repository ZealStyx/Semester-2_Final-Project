package io.github.superteam.resonance.trigger;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evaluates active triggers and dispatches target events through a callback.
 */
public final class TriggerEvaluator {
    @FunctionalInterface
    public interface TriggerEventDispatcher {
        void fire(String eventId, Vector3 triggerPosition);
    }

    private final List<Trigger> triggers;

    public TriggerEvaluator(List<Trigger> triggers) {
        this.triggers = Collections.unmodifiableList(new ArrayList<>(triggers == null ? List.of() : triggers));
    }

    public void update(float deltaSeconds, TriggerEvaluationContext evaluationContext, TriggerEventDispatcher dispatcher) {
        if (dispatcher == null || evaluationContext == null) {
            return;
        }

        for (Trigger trigger : triggers) {
            trigger.update(deltaSeconds);
            if (trigger.shouldFire(evaluationContext)) {
                dispatcher.fire(trigger.targetEventId(), trigger.triggerPosition(evaluationContext));
            }
        }
    }

    public List<Trigger> all() {
        return triggers;
    }
}

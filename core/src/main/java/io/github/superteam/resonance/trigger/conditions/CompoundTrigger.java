package io.github.superteam.resonance.trigger.conditions;

import io.github.superteam.resonance.trigger.Trigger;
import io.github.superteam.resonance.trigger.TriggerEvaluationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Combines multiple triggers with AND/OR semantics. */
public final class CompoundTrigger extends Trigger {
    public enum Mode {
        AND,
        OR
    }

    private final Mode mode;
    private final List<Trigger> children;

    public CompoundTrigger(String id, String targetEventId, float cooldownSeconds, Mode mode, List<Trigger> children) {
        super(id, targetEventId, cooldownSeconds);
        this.mode = mode == null ? Mode.AND : mode;
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
    }

    public Mode mode() {
        return mode;
    }

    public List<Trigger> children() {
        return children;
    }

    @Override
    protected boolean evaluate(TriggerEvaluationContext context) {
        if (children.isEmpty()) {
            return false;
        }

        return switch (mode) {
            case AND -> evaluateAll(context);
            case OR -> evaluateAny(context);
        };
    }

    private boolean evaluateAll(TriggerEvaluationContext context) {
        for (Trigger child : children) {
            if (child == null || !child.isEvaluate(context)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateAny(TriggerEvaluationContext context) {
        for (Trigger child : children) {
            if (child != null && child.isEvaluate(context)) {
                return true;
            }
        }
        return false;
    }
}

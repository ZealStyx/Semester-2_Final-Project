package io.github.superteam.resonance.event;

import java.util.List;

/**
 * Ordered sequence of event ids with delays between steps.
 */
public final class EventSequence {
    public record Step(String eventId, float delaySeconds) {
    }

    private final List<Step> steps;
    private int currentStep;
    private float stepTimer;
    private boolean running;

    public EventSequence(List<Step> steps) {
        this.steps = List.copyOf(steps == null ? List.of() : steps);
    }

    public void start() {
        currentStep = 0;
        stepTimer = 0f;
        running = true;
    }

    public void update(float deltaSeconds, EventBus eventBus, EventContext context, EventState eventState) {
        if (!running || currentStep >= steps.size()) {
            running = false;
            return;
        }

        stepTimer += Math.max(0f, deltaSeconds);
        Step step = steps.get(currentStep);
        if (stepTimer >= step.delaySeconds()) {
            if (eventBus != null) {
                eventBus.fire(step.eventId(), context, eventState);
            }
            currentStep++;
            stepTimer = 0f;
        }
    }
}
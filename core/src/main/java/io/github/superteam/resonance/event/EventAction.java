package io.github.superteam.resonance.event;

/**
 * An executable action inside a game event.
 */
@FunctionalInterface
public interface EventAction {
    void execute(EventContext context);
}
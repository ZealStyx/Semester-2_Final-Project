package io.github.superteam.resonance.event;

import java.util.Collection;

/** Thin registry facade for events. */
public final class EventRegistry {
    private final EventBus bus = new EventBus();

    public void register(GameEvent e) { bus.register(e); }
    public GameEvent get(String id) { return bus.get(id); }
    public void fire(String id, EventContext ctx) { bus.fire(id, ctx, ctx == null ? null : ctx.eventState()); }
    public Collection<GameEvent> all() { return bus.all(); }
}

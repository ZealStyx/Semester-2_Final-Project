package io.github.superteam.resonance.debug;

import io.github.superteam.resonance.event.EventContext;

public interface DebugCommand {
    String name();
    String help();
    void execute(String[] args, EventContext ctx);
}

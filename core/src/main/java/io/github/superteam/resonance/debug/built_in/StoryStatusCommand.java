package io.github.superteam.resonance.debug.built_in;

import io.github.superteam.resonance.debug.DebugCommand;
import io.github.superteam.resonance.event.EventContext;

public final class StoryStatusCommand implements DebugCommand {
    @Override public String name() { return "story"; }
    @Override public String help() { return "Print current story status"; }
    @Override public void execute(String[] args, EventContext ctx) {
        if (ctx == null) { System.out.println("No context"); return; }
        var ss = ctx.storySystem();
        System.out.println(ss == null ? "StorySystem not wired" : ss.debugStatus());
    }
}

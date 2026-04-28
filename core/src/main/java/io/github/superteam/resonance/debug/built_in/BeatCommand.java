package io.github.superteam.resonance.debug.built_in;

import io.github.superteam.resonance.debug.DebugCommand;
import io.github.superteam.resonance.event.EventContext;

public final class BeatCommand implements DebugCommand {
    @Override public String name() { return "beat"; }
    @Override public String help() { return "Force-complete a beat: beat <beatId>"; }
    @Override public void execute(String[] args, EventContext ctx) {
        if (args == null || args.length == 0) { System.out.println("Usage: beat <beatId>"); return; }
        String beat = args[0];
        var ss = ctx == null ? null : ctx.storySystem();
        if (ss == null) { System.out.println("StorySystem not wired"); return; }
        ss.onEventFired(beat, ctx);
        System.out.println("Beat requested: " + beat);
    }
}

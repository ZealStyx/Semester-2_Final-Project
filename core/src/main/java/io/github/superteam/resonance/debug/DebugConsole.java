package io.github.superteam.resonance.debug;

import java.util.LinkedHashMap;
import java.util.Map;
import io.github.superteam.resonance.event.EventContext;

/** Minimal DebugConsole: command registry + simple parsing. */
public final class DebugConsole {
    private final Map<String, DebugCommand> commands = new LinkedHashMap<>();

    public void register(DebugCommand cmd) {
        if (cmd != null) commands.put(cmd.name().toLowerCase(), cmd);
    }

    public boolean executeLine(String line, EventContext ctx) {
        if (line == null || line.isBlank()) return false;
        String[] parts = line.trim().split("\\s+");
        String cmd = parts[0].toLowerCase();
        DebugCommand c = commands.get(cmd);
        if (c == null) return false;
        String[] args = new String[Math.max(0, parts.length - 1)];
        System.arraycopy(parts, 1, args, 0, args.length);
        try {
            c.execute(args, ctx);
        } catch (Exception e) {
            System.err.println("Debug command failed: " + e.getMessage());
            return false;
        }
        return true;
    }
}

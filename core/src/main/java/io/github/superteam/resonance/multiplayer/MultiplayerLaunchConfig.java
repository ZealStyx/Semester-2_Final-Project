package io.github.superteam.resonance.multiplayer;

/**
 * Configuration object passed from MultiplayerTestMenuScreen to UniversalTestScene
 * to deterministically establish a multiplayer session role and parameters.
 */
public class MultiplayerLaunchConfig {
    public enum Role { HOST, CLIENT, OFFLINE }

    public Role role;
    public String hostIp;        // null if HOST or OFFLINE; IP string (or "127.0.0.1") if CLIENT
    public String playerName;    // optional display name
    
    public MultiplayerLaunchConfig(Role role, String hostIp, String playerName) {
        this.role = role;
        this.hostIp = hostIp;
        this.playerName = playerName != null ? playerName : "Player";
    }

    public MultiplayerLaunchConfig(Role role) {
        this(role, null, "Player");
    }

    @Override
    public String toString() {
        return "MultiplayerLaunchConfig{" +
                "role=" + role +
                ", hostIp='" + hostIp + '\'' +
                ", playerName='" + playerName + '\'' +
                '}';
    }
}

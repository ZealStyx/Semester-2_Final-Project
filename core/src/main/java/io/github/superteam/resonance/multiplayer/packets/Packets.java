package io.github.superteam.resonance.multiplayer.packets;

import com.badlogic.gdx.math.Vector3;
import java.util.List;

public class Packets {

    // Sent by host to all clients on join
    public static class PlayerJoinPacket {
        public int    playerId;
        public String playerName;
        public float  colorR, colorG, colorB;

        public PlayerJoinPacket() {}
    }

    // Sent when a player disconnects
    public static class PlayerLeavePacket {
        public int playerId;

        public PlayerLeavePacket() {}
    }

    // Sent via UDP every 50ms per player (20 Hz position update)
    public static class PlayerStatePacket {
        public int   playerId;
        public float x, y, z;
        public float lookX, lookY, lookZ;
        public long  timestamp;

        public PlayerStatePacket() {}
    }

    // Sent when any player fires a sound pulse
    public static class SoundEventPacket {
        public int     sourcePlayerId;
        public Vector3 position;
        public float   strength;
        public int     eventType;

        public SoundEventPacket() {}
    }

    // Host broadcasts authoritative snapshot every 100ms
    public static class ServerTickPacket {
        public long                   serverTick;
        public List<PlayerStatePacket> playerStates;

        public ServerTickPacket() {}
    }

    // Sent via UDP per captured audio frame
    public static class VoiceChunkPacket {
        public int    sourcePlayerId;
        public int    sequenceNumber;
        public int    sampleRate;
        public byte[] pcmData;
        public float  rmsLevel;
        public boolean isOpus;

        public VoiceChunkPacket() {}
    }

    // Ping
    public static class PingPacket {
        public long timestamp;

        public PingPacket() {}
    }

    public static class PingAckPacket {
        public long timestamp;

        public PingAckPacket() {}
    }
}

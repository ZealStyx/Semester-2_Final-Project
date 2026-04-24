package io.github.superteam.resonance.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import io.github.superteam.resonance.devTest.universal.UniversalTestScene;
import io.github.superteam.resonance.multiplayer.packets.Packets.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MultiplayerManager implements Disposable {

    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;
    private static final long SERVER_TICK_INTERVAL_MS = 100L;

    public enum Role { HOST, CLIENT, OFFLINE }

    private volatile Role role = Role.OFFLINE;
    private volatile int localPlayerId = -1;
    private volatile long lastServerTickBroadcastMs;
    private Server kryoServer;
    private Client kryoClient;

    public final ConcurrentHashMap<Integer, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    public final ConcurrentLinkedQueue<SoundEventPacket> pendingSoundEvents = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<VoiceChunkPacket> pendingVoiceChunks = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<String> pendingJoinBanners = new ConcurrentLinkedQueue<>();

    private static final Color[] PLAYER_COLORS = {
        new Color(0.3f, 0.8f, 1.0f, 1f),
        new Color(1.0f, 0.5f, 0.2f, 1f),
        new Color(0.4f, 1.0f, 0.4f, 1f),
        new Color(1.0f, 0.3f, 0.6f, 1f),
    };

    public MultiplayerManager() {
    }

    public static void registerPackets(Kryo kryo) {
        kryo.register(PlayerJoinPacket.class);
        kryo.register(PlayerLeavePacket.class);
        kryo.register(PlayerStatePacket.class);
        kryo.register(SoundEventPacket.class);
        kryo.register(ServerTickPacket.class);
        kryo.register(VoiceChunkPacket.class);
        kryo.register(byte[].class);
        kryo.register(PingPacket.class);
        kryo.register(PingAckPacket.class);
        kryo.register(Vector3.class);
        kryo.register(ArrayList.class);
    }

    public void startHost() {
        if (role != Role.OFFLINE) {
            return;
        }

        role = Role.HOST;
        localPlayerId = -1;

        Thread bootstrapThread = new Thread(() -> {
            try {
                kryoServer = new Server(16384, 16384);
                registerPackets(kryoServer.getKryo());
                kryoServer.addListener(new ServerListener());
                kryoServer.start();
                kryoServer.bind(TCP_PORT, UDP_PORT);
                lastServerTickBroadcastMs = 0L;
                Gdx.app.log("MP", "Hosting on TCP:" + TCP_PORT + " UDP:" + UDP_PORT);
                connectToHost("127.0.0.1", true);
            } catch (Exception e) {
                Gdx.app.error("MP", "Failed to start host session", e);
                stopNetworkResources();
                role = Role.OFFLINE;
                localPlayerId = -1;
            }
        }, "MP-Host-Bootstrap");
        bootstrapThread.setDaemon(true);
        bootstrapThread.start();
    }

    public void connectAsClient(String hostIp) {
        if (role != Role.OFFLINE) {
            return;
        }

        role = Role.CLIENT;
        localPlayerId = -1;
        connectToHost(hostIp, false);
    }

    private void connectToHost(String hostIp, boolean preserveRole) {
        kryoClient = new Client(16384, 16384);
        registerPackets(kryoClient.getKryo());
        kryoClient.addListener(new ClientListener());
        kryoClient.start();

        Thread connectThread = new Thread(() -> {
            try {
                kryoClient.connect(5000, hostIp, TCP_PORT, UDP_PORT);
                Gdx.app.log("MP", "Connected to " + hostIp);
            } catch (IOException e) {
                Gdx.app.error("MP", "Failed to connect to " + hostIp, e);
                stopNetworkResources();
                role = Role.OFFLINE;
                localPlayerId = -1;
            }
        }, preserveRole ? "MP-Host-Local-Connect" : "MP-Client-Connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    public void broadcastPosition(Vector3 position, Vector3 direction) {
        int playerId = resolveLocalPlayerId();
        if (playerId < 0 || kryoClient == null || !kryoClient.isConnected()) {
            return;
        }

        PlayerStatePacket packet = new PlayerStatePacket();
        packet.x = position.x;
        packet.y = position.y;
        packet.z = position.z;
        packet.lookX = direction.x;
        packet.lookY = direction.y;
        packet.lookZ = direction.z;
        packet.timestamp = System.currentTimeMillis();
        packet.playerId = playerId;
        kryoClient.sendUDP(packet);
    }

    public void broadcastSoundEvent(SoundEventPacket packet) {
        if (kryoClient != null) {
            kryoClient.sendTCP(packet);
        }
    }

    public void broadcastVoice(VoiceChunkPacket packet) {
        sendVoicePacket(packet);
    }

    public void sendVoiceToHost(VoiceChunkPacket packet) {
        sendVoicePacket(packet);
    }

    private void sendVoicePacket(VoiceChunkPacket packet) {
        if (kryoClient != null) {
            kryoClient.sendUDP(packet);
        }
    }

    public void processPendingEvents(UniversalTestScene scene) {
        SoundEventPacket evt;
        while ((evt = pendingSoundEvents.poll()) != null) {
            scene.applyRemoteSoundEvent(evt);
        }
    }

    @Override
    public void dispose() {
        stopNetworkResources();
        remotePlayers.clear();
        pendingSoundEvents.clear();
        pendingVoiceChunks.clear();
        pendingJoinBanners.clear();
        localPlayerId = -1;
        role = Role.OFFLINE;
    }

    public Role getRole() {
        return role;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public boolean hasLocalPlayerId() {
        return localPlayerId > 0;
    }

    public int getKnownPlayerCount() {
        int count = remotePlayers.size();
        if (localPlayerId > 0 && !remotePlayers.containsKey(localPlayerId)) {
            count++;
        }
        return count;
    }

    public void pushJoinBanner(String bannerText) {
        if (bannerText != null && !bannerText.isBlank()) {
            pendingJoinBanners.add(bannerText);
        }
    }

    private int resolveLocalPlayerId() {
        if (localPlayerId > 0) {
            return localPlayerId;
        }
        return kryoClient != null ? kryoClient.getID() : -1;
    }

    private void stopNetworkResources() {
        if (kryoClient != null) {
            kryoClient.stop();
            kryoClient.close();
            kryoClient = null;
        }
        if (kryoServer != null) {
            kryoServer.stop();
            kryoServer.close();
            kryoServer = null;
        }
        lastServerTickBroadcastMs = 0L;
    }

    private void maybeBroadcastServerTick() {
        if (kryoServer == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastServerTickBroadcastMs < SERVER_TICK_INTERVAL_MS) {
            return;
        }
        lastServerTickBroadcastMs = now;

        ServerTickPacket tickPacket = new ServerTickPacket();
        tickPacket.serverTick = now;
        tickPacket.playerStates = new ArrayList<>(remotePlayers.size());

        for (RemotePlayer player : remotePlayers.values()) {
            PlayerStatePacket state = new PlayerStatePacket();
            state.playerId = player.id;
            state.x = player.targetPosition.x;
            state.y = player.targetPosition.y;
            state.z = player.targetPosition.z;
            state.lookX = player.lookDir.x;
            state.lookY = player.lookDir.y;
            state.lookZ = player.lookDir.z;
            state.timestamp = now;
            tickPacket.playerStates.add(state);
        }

        kryoServer.sendToAllUDP(tickPacket);
    }

    private class ServerListener implements Listener {
        @Override
        public void connected(Connection connection) {
            int id = connection.getID();
            PlayerJoinPacket join = new PlayerJoinPacket();
            join.playerId = id;
            join.playerName = "Player " + id;
            Color c = PLAYER_COLORS[id % PLAYER_COLORS.length];
            join.colorR = c.r;
            join.colorG = c.g;
            join.colorB = c.b;

            kryoServer.sendToAllExceptTCP(id, join);
            pushJoinBanner(join.playerName + " joined");

            for (RemotePlayer rp : remotePlayers.values()) {
                PlayerJoinPacket existing = new PlayerJoinPacket();
                existing.playerId = rp.id;
                existing.playerName = rp.name;
                existing.colorR = rp.color.r;
                existing.colorG = rp.color.g;
                existing.colorB = rp.color.b;
                connection.sendTCP(existing);
            }

            RemotePlayer newPlayer = new RemotePlayer(id);
            newPlayer.name = join.playerName;
            newPlayer.color = new Color(join.colorR, join.colorG, join.colorB, 1f);
            newPlayer.targetPosition.set(40, 1.6f, 40);
            newPlayer.currentPosition.set(newPlayer.targetPosition);
            remotePlayers.put(id, newPlayer);
        }

        @Override
        public void disconnected(Connection connection) {
            int id = connection.getID();
            remotePlayers.remove(id);
            PlayerLeavePacket leave = new PlayerLeavePacket();
            leave.playerId = id;
            kryoServer.sendToAllTCP(leave);
        }

        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof PlayerStatePacket p) {
                p.playerId = connection.getID();
                RemotePlayer rp = remotePlayers.get(p.playerId);
                if (rp != null) {
                    rp.targetPosition.set(p.x, p.y, p.z);
                    rp.lookDir.set(p.lookX, p.lookY, p.lookZ);
                }
                maybeBroadcastServerTick();
            } else if (object instanceof SoundEventPacket s) {
                s.sourcePlayerId = connection.getID();
                pendingSoundEvents.add(s);
                kryoServer.sendToAllExceptTCP(connection.getID(), s);
            } else if (object instanceof VoiceChunkPacket v) {
                v.sourcePlayerId = connection.getID();
                pendingVoiceChunks.add(v);
                kryoServer.sendToAllExceptUDP(connection.getID(), v);
            } else if (object instanceof PingPacket p) {
                PingAckPacket ack = new PingAckPacket();
                ack.timestamp = p.timestamp;
                connection.sendTCP(ack);
            }
        }

        @Override
        public void idle(Connection connection) {
        }
    }

    private class ClientListener implements Listener {
        @Override
        public void connected(Connection connection) {
            localPlayerId = connection.getID();
        }

        @Override
        public void disconnected(Connection connection) {
        }

        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof PlayerJoinPacket j) {
                if (j.playerId == connection.getID()) {
                    return;
                }
                RemotePlayer rp = new RemotePlayer(j.playerId);
                rp.name = j.playerName;
                rp.color = new Color(j.colorR, j.colorG, j.colorB, 1f);
                rp.targetPosition.set(40, 1.6f, 40);
                rp.currentPosition.set(rp.targetPosition);
                remotePlayers.put(j.playerId, rp);
                pushJoinBanner(j.playerName + " joined");
            } else if (object instanceof PlayerLeavePacket l) {
                remotePlayers.remove(l.playerId);
            } else if (object instanceof PlayerStatePacket p) {
                RemotePlayer rp = remotePlayers.get(p.playerId);
                if (rp != null) {
                    rp.targetPosition.set(p.x, p.y, p.z);
                    rp.lookDir.set(p.lookX, p.lookY, p.lookZ);
                }
            } else if (object instanceof ServerTickPacket tick) {
                if (tick.playerStates == null) {
                    return;
                }

                for (PlayerStatePacket state : tick.playerStates) {
                    if (state == null || state.playerId == connection.getID()) {
                        continue;
                    }

                    RemotePlayer rp = remotePlayers.get(state.playerId);
                    if (rp != null) {
                        rp.targetPosition.set(state.x, state.y, state.z);
                        rp.lookDir.set(state.lookX, state.lookY, state.lookZ);
                    }
                }
            } else if (object instanceof SoundEventPacket s) {
                pendingSoundEvents.add(s);
            } else if (object instanceof VoiceChunkPacket v) {
                pendingVoiceChunks.add(v);
            } else if (object instanceof PingAckPacket) {
                // Ping calculation could go here
            }
        }

        @Override
        public void idle(Connection connection) {
        }
    }
}

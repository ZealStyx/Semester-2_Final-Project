package io.github.superteam.resonance.multiplayer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight UDP broadcast service for discovering multiplayer hosts on the LAN.
 * 
 * Hosts listen on a discovery port; clients send a broadcast probe and collect responses.
 */
public class LanDiscoveryService {
    public static final int DISCOVERY_PORT = 54999;
    public static final String PROBE_MAGIC = "RESONANCE_PROBE";
    public static final String RESPONSE_MAGIC = "RESONANCE_HOST";

    public static class DiscoveredHost {
        public String hostAddress;      // IP address of discovered host
        public String playerName;       // host's player name or identifier
        public int gamePort;            // TCP port the host game is listening on (usually 54555)
        public int remotePlayerCount;   // number of players already connected

        public DiscoveredHost(String hostAddress, String playerName, int gamePort, int remotePlayerCount) {
            this.hostAddress = hostAddress;
            this.playerName = playerName;
            this.gamePort = gamePort;
            this.remotePlayerCount = remotePlayerCount;
        }

        @Override
        public String toString() {
            return playerName + " (" + hostAddress + ":" + gamePort + ") [" + remotePlayerCount + " players]";
        }
    }

    /**
     * Client-side: broadcast a discovery probe and collect host responses.
     * Listens for up to `timeoutMs` milliseconds for responses.
     * 
     * @param timeoutMs timeout in milliseconds for listening to responses
     * @return list of discovered hosts
     */
    public static List<DiscoveredHost> discoverHosts(long timeoutMs) {
        List<DiscoveredHost> discovered = new ArrayList<>();
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout((int) timeoutMs);

            // Send broadcast probe
            byte[] probeData = PROBE_MAGIC.getBytes(StandardCharsets.UTF_8);
            DatagramPacket probePacket = new DatagramPacket(
                    probeData, probeData.length,
                    InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT
            );
            socket.send(probePacket);

            // Listen for responses
            byte[] responseData = new byte[256];
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);

            try {
                while (true) {
                    socket.receive(responsePacket);
                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);
                    
                    if (response.startsWith(RESPONSE_MAGIC)) {
                        // Parse response: "RESONANCE_HOST|playerName|gamePort|playerCount"
                        String[] parts = response.split("\\|");
                        if (parts.length >= 4) {
                            try {
                                String playerName = parts[1];
                                int gamePort = Integer.parseInt(parts[2]);
                                int playerCount = Integer.parseInt(parts[3]);
                                
                                DiscoveredHost host = new DiscoveredHost(
                                        responsePacket.getAddress().getHostAddress(),
                                        playerName,
                                        gamePort,
                                        playerCount
                                );
                                discovered.add(host);
                            } catch (NumberFormatException e) {
                                // Ignore malformed response
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Timeout or no more data; expected
            }

        } catch (IOException e) {
            System.err.println("LAN discovery failed: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

        return discovered;
    }

    /**
     * Host-side: listen for discovery probes and respond with host metadata.
     * Runs in a dedicated thread until `stop()` is called or an exception occurs.
     * 
     * @param playerName host's player name
     * @param gamePort TCP game server port (usually 54555)
     * @param connectedPlayerCount current player count
     * @return a discoveryService instance; call service.stop() to halt listening
     */
    public static DiscoveryService startHostListener(String playerName, int gamePort, int connectedPlayerCount) {
        return new DiscoveryService(playerName, gamePort, connectedPlayerCount);
    }

    /**
     * Thread-safe host discovery listener service.
     */
    public static class DiscoveryService {
        private final String playerName;
        private final int gamePort;
        private volatile int remotePlayerCount;
        private volatile boolean running = false;
        private DatagramSocket socket;
        private Thread listenerThread;

        public DiscoveryService(String playerName, int gamePort, int initialPlayerCount) {
            this.playerName = playerName;
            this.gamePort = gamePort;
            this.remotePlayerCount = initialPlayerCount;
        }

        public void start() {
            if (running) return;
            running = true;

            listenerThread = new Thread(() -> {
                try {
                    socket = new DatagramSocket(DISCOVERY_PORT);
                    socket.setBroadcast(true);
                    byte[] probeBuffer = new byte[32];

                    while (running) {
                        DatagramPacket probePacket = new DatagramPacket(probeBuffer, probeBuffer.length);
                        socket.receive(probePacket);

                        String probe = new String(probePacket.getData(), 0, probePacket.getLength(), StandardCharsets.UTF_8);
                        if (PROBE_MAGIC.equals(probe)) {
                            // Send response
                            String responseStr = RESPONSE_MAGIC + "|" + playerName + "|" + gamePort + "|" + remotePlayerCount;
                            byte[] responseData = responseStr.getBytes(StandardCharsets.UTF_8);
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length,
                                    probePacket.getAddress(), probePacket.getPort()
                            );
                            socket.send(responsePacket);
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Host discovery listener error: " + e.getMessage());
                    }
                } finally {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                }
            });

            listenerThread.setDaemon(true);
            listenerThread.setName("LAN-Discovery-Host");
            listenerThread.start();
        }

        public void updatePlayerCount(int count) {
            this.remotePlayerCount = count;
        }

        public void stop() {
            running = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (listenerThread != null) {
                try {
                    listenerThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}

# RESONANCE — Multiplayer Test Plan
> **Version:** 1.0 | **Engine:** libGDX + Kryonet | **Scope:** Local-LAN and loopback multiplayer with voice chat
> **Test Entry Point:** `UniversalTestScene` (promoted to main test screen)

---

## Overview

This plan describes a self-contained multiplayer test mode that sits inside the existing `UniversalTestScene` workflow. The goal is to verify that Resonance's acoustic systems (sonar pulse, graph propagation, material transmission) work correctly when multiple players share the same world, and that voice chat — the primary input mechanic — is delivered between clients in real time.

Players are represented as **simple capsule shapes** during this test phase. No character models, animations, or enemy AI are required. The focus is network topology, voice delivery, and acoustic event synchronisation.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Network Layer — Kryonet Setup](#2-network-layer--kryonet-setup)
3. [Player Representation (Simple Shape)](#3-player-representation-simple-shape)
4. [Packet Design](#4-packet-design)
5. [Voice Chat System](#5-voice-chat-system)
6. [Acoustic Event Synchronisation](#6-acoustic-event-synchronisation)
7. [UniversalTestScene Integration](#7-universaltestscene-integration)
8. [UI — Multiplayer Overlay](#8-ui--multiplayer-overlay)
9. [Phase-by-Phase Implementation](#9-phase-by-phase-implementation)
10. [Test Cases](#10-test-cases)
11. [Known Risks and Mitigations](#11-known-risks-and-mitigations)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│              UniversalTestScene (HOST)               │
│                                                     │
│  ┌─────────────┐   ┌──────────────────────────────┐ │
│  │  GameServer │   │  MultiplayerOverlay (HUD)    │ │
│  │  (Kryonet)  │   │  - Player list               │ │
│  │  TCP + UDP  │   │  - Voice indicators          │ │
│  └──────┬──────┘   │  - Ping display              │ │
│         │          └──────────────────────────────┘ │
│         │ loopback / LAN                            │
└─────────┼───────────────────────────────────────────┘
          │
     ┌────┴────┐
     │  CLIENT  │  (second instance or second device on same LAN)
     │  Kryonet │
     │  Client  │
     └──────────┘
```

**Server mode:** One player hosts. The server runs inside the same `UniversalTestScene` instance — no dedicated server binary. The existing `server/` Gradle module provides `ServerLauncher.java` as a future headless path; for now, host-as-server is sufficient.

**Client mode:** A second player connects by IP. For local testing both instances run on the same machine (loopback `127.0.0.1`) or on two machines on the same LAN.

**Topology:** Star (all clients connect to host). No peer-to-peer. Maximum 4 players for this test phase.

---

## 2. Network Layer — Kryonet Setup

Kryonet is already a dependency in the project. This plan assumes it is on the classpath.

### 2.1 — Ports

| Port | Protocol | Purpose |
|---|---|---|
| 54555 | TCP | Reliable game state (positions, sound events, connect/disconnect) |
| 54777 | UDP | Unreliable but low-latency (position updates, voice audio chunks) |

```java
// In MultiplayerManager.java (new class):
public static final int TCP_PORT = 54555;
public static final int UDP_PORT = 54777;
```

### 2.2 — Registration

All packet classes must be registered with Kryonet's `Kryo` serialiser before the server/client is started:

```java
public static void registerPackets(Kryo kryo) {
    // Game state
    kryo.register(PlayerJoinPacket.class);
    kryo.register(PlayerLeavePacket.class);
    kryo.register(PlayerStatePacket.class);   // position + look direction
    kryo.register(SoundEventPacket.class);    // pulse fired at position
    kryo.register(ServerTickPacket.class);    // authoritative world snapshot

    // Voice
    kryo.register(VoiceChunkPacket.class);
    kryo.register(byte[].class);             // raw PCM payload

    // Metadata
    kryo.register(PingPacket.class);
    kryo.register(PingAckPacket.class);
}
```

### 2.3 — MultiplayerManager.java (new class)

```java
public final class MultiplayerManager implements Disposable {

    public enum Role { HOST, CLIENT, OFFLINE }

    private Role role = Role.OFFLINE;
    private Server kryoServer;
    private Client kryoClient;

    // Shared state — updated from network thread, read from render thread
    private final ConcurrentHashMap<Integer, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<SoundEventPacket> pendingSoundEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<VoiceChunkPacket> pendingVoiceChunks = new ConcurrentLinkedQueue<>();

    public void startHost() {
        role = Role.HOST;
        kryoServer = new Server(16384, 16384);
        registerPackets(kryoServer.getKryo());
        kryoServer.addListener(new ServerListener(this));
        kryoServer.start();
        kryoServer.bind(TCP_PORT, UDP_PORT);
        Gdx.app.log("MP", "Hosting on TCP:" + TCP_PORT + " UDP:" + UDP_PORT);
    }

    public void connectAsClient(String hostIp) {
        role = Role.CLIENT;
        kryoClient = new Client(16384, 16384);
        registerPackets(kryoClient.getKryo());
        kryoClient.addListener(new ClientListener(this));
        kryoClient.start();
        kryoClient.connect(5000, hostIp, TCP_PORT, UDP_PORT);
    }

    // Called from render thread — safe to drain queues here
    public void processPendingEvents(UniversalTestScene scene) {
        SoundEventPacket evt;
        while ((evt = pendingSoundEvents.poll()) != null) {
            scene.triggerSoundPulse(evt.position, evt.strength);
        }
    }

    @Override public void dispose() {
        if (kryoServer != null) { kryoServer.stop(); kryoServer.close(); }
        if (kryoClient != null) { kryoClient.stop(); kryoClient.close(); }
    }
}
```

---

## 3. Player Representation (Simple Shape)

During the multiplayer test phase, remote players are rendered as **coloured capsule meshes** — no character model, no animation rig, no texture.

### 3.1 — RemotePlayer.java

```java
public final class RemotePlayer {
    public final int id;
    public String name;
    public Color color;           // assigned on join (up to 4 colours)
    public final Vector3 position = new Vector3();
    public final Vector3 lookDir  = new Vector3(0, 0, -1);
    public boolean isSpeaking;    // true while sending voice audio
    public float speakingTimer;   // fade-out timer for speaking indicator

    // Render data — capsule (cylinder body + two sphere caps)
    private static final float RADIUS  = 0.35f;
    private static final float HEIGHT  = 1.6f;  // eye height

    public void render(ShapeRenderer shape) {
        shape.setColor(isSpeaking ? Color.YELLOW : color);

        // Body cylinder
        // LibGDX ShapeRenderer has no built-in cylinder — use box as proxy for test:
        shape.box(position.x - RADIUS, position.y, position.z - RADIUS,
                  RADIUS * 2, HEIGHT, RADIUS * 2);
    }
}
```

**Colour assignment on join:**
```java
private static final Color[] PLAYER_COLORS = {
    new Color(0.3f, 0.8f, 1.0f, 1f),   // cyan
    new Color(1.0f, 0.5f, 0.2f, 1f),   // orange
    new Color(0.4f, 1.0f, 0.4f, 1f),   // green
    new Color(1.0f, 0.3f, 0.6f, 1f),   // pink
};
```

### 3.2 — Rendering Remote Players

In `UniversalTestScene.renderWorld()`, after the main scene draw and before HUD:

```java
shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
shapeRenderer.setProjectionMatrix(camera.combined);
for (RemotePlayer remote : multiplayerManager.remotePlayers.values()) {
    remote.render(shapeRenderer);
}
shapeRenderer.end();

// Render speaking indicators as billboard quads above each player's head
for (RemotePlayer remote : multiplayerManager.remotePlayers.values()) {
    if (remote.isSpeaking) {
        renderSpeakingBillboard(remote.position);
    }
}
```

`renderSpeakingBillboard` draws a small sound-wave icon or a pulsing sphere above the player's head using the existing particle system or a simple expanding ring.

---

## 4. Packet Design

All packets are simple POJOs with public fields (required by Kryonet's Kryo serialiser — no-arg constructor mandatory).

### 4.1 — Game State Packets

```java
// Sent by host to all clients on join
public class PlayerJoinPacket {
    public int    playerId;
    public String playerName;
    public float  colorR, colorG, colorB;  // assigned colour
}

// Sent when a player disconnects
public class PlayerLeavePacket {
    public int playerId;
}

// Sent via UDP every 50ms per player (20 Hz position update)
public class PlayerStatePacket {
    public int   playerId;
    public float x, y, z;            // world position
    public float lookX, lookY, lookZ; // look direction (normalised)
    public long  timestamp;           // client system time for interpolation
}

// Sent when any player fires a sound pulse (mic clap, V key, or item impact)
public class SoundEventPacket {
    public int     sourcePlayerId;
    public Vector3 position;   // world position of the event
    public float   strength;   // 0..1 normalised
    public int     eventType;  // maps to SoundEvent enum ordinal
}

// Host broadcasts authoritative snapshot every 100ms
public class ServerTickPacket {
    public long                   serverTick;
    public List<PlayerStatePacket> playerStates;
}
```

### 4.2 — Voice Packets

```java
// Sent via UDP per captured audio frame (≈ every 20ms at 50Hz)
public class VoiceChunkPacket {
    public int    sourcePlayerId;
    public int    sequenceNumber;   // for ordering / drop detection
    public byte[] pcmData;          // raw 16-bit PCM, 960 samples = 20ms at 48kHz
    public float  rmsLevel;         // pre-computed for speaking indicator
}
```

PCM is uncompressed for simplicity. If bandwidth is a concern (LAN is fine, WAN is not), swap to Opus codec via `javacpp-presets/opus`. For this test, uncompressed is acceptable.

---

## 5. Voice Chat System

### 5.1 — Architecture

```
MicCapture Thread              Render Thread              Playback Thread
     │                              │                          │
     │  VoiceCaptureSystem          │  MultiplayerManager      │  VoicePlaybackSystem
     │  - Captures mic via          │  - drains                │  - one AudioTrack or
     │    javax.sound               │    pendingVoiceChunks    │    SourceDataLine per
     │  - Splits to 20ms chunks     │  - routes to             │    remote player
     │  - Sends VoiceChunkPacket    │    VoicePlaybackSystem   │  - spatial panning by
     │    via Kryonet UDP           │                          │    player world position
     │                              │                          │
```

### 5.2 — VoiceCaptureSystem.java (new class)

```java
public final class VoiceCaptureSystem implements Disposable {

    private static final int SAMPLE_RATE    = 48000;
    private static final int FRAME_SAMPLES  = 960;    // 20ms at 48kHz
    private static final int BYTES_PER_FRAME = FRAME_SAMPLES * 2; // 16-bit

    private TargetDataLine micLine;
    private Thread captureThread;
    private volatile boolean running;
    private final Consumer<VoiceChunkPacket> onChunkReady;
    private int sequenceNumber;

    public VoiceCaptureSystem(Consumer<VoiceChunkPacket> onChunkReady) {
        this.onChunkReady = onChunkReady;
    }

    public void start(int localPlayerId) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            Gdx.app.error("Voice", "Microphone not supported on this system.");
            return;
        }
        micLine = (TargetDataLine) AudioSystem.getLine(info);
        micLine.open(format, BYTES_PER_FRAME * 4);
        micLine.start();
        running = true;

        captureThread = new Thread(() -> {
            byte[] buffer = new byte[BYTES_PER_FRAME];
            while (running) {
                int read = micLine.read(buffer, 0, BYTES_PER_FRAME);
                if (read <= 0) continue;

                float rms = computeRms(buffer, read);
                if (rms < SILENCE_THRESHOLD) continue;  // voice activity gate

                VoiceChunkPacket packet = new VoiceChunkPacket();
                packet.sourcePlayerId = localPlayerId;
                packet.sequenceNumber = sequenceNumber++;
                packet.pcmData = Arrays.copyOf(buffer, read);
                packet.rmsLevel = rms;
                onChunkReady.accept(packet);
            }
        }, "VoiceCapture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private float computeRms(byte[] buffer, int length) {
        long sumSquares = 0;
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short)((buffer[i+1] << 8) | (buffer[i] & 0xFF));
            sumSquares += (long) sample * sample;
        }
        return (float) Math.sqrt((double) sumSquares / (length / 2));
    }

    private static final float SILENCE_THRESHOLD = 400f; // raw RMS — tune with calibration

    @Override public void dispose() {
        running = false;
        if (micLine != null) { micLine.stop(); micLine.close(); }
    }
}
```

### 5.3 — VoicePlaybackSystem.java (new class)

One `SourceDataLine` per remote player. Each line plays back PCM at 48kHz mono. Position-based panning is applied by adjusting the line's `FloatControl.Type.BALANCE` based on the remote player's position relative to the local player.

```java
public final class VoicePlaybackSystem implements Disposable {

    private static final int SAMPLE_RATE = 48000;
    private final Map<Integer, PlayerAudioLine> audioLines = new HashMap<>();

    public void receiveChunk(VoiceChunkPacket packet, RemotePlayer sourcePlayer,
                              Vector3 localPlayerPosition) {
        PlayerAudioLine line = audioLines.computeIfAbsent(packet.sourcePlayerId,
            id -> openLine());

        // Spatial volume: inverse-distance attenuation
        float dist = localPlayerPosition.dst(sourcePlayer.position);
        float volume = MathUtils.clamp(1.0f - (dist / MAX_VOICE_DISTANCE), 0.05f, 1.0f);
        line.setVolume(volume);

        // Stereo panning: left/right based on relative direction
        Vector3 toPlayer = new Vector3(sourcePlayer.position).sub(localPlayerPosition).nor();
        float pan = MathUtils.clamp(toPlayer.x, -1f, 1f);  // simplified — use X component
        line.setPan(pan);

        // Update speaking indicator
        sourcePlayer.isSpeaking   = packet.rmsLevel > VoiceCaptureSystem.SILENCE_THRESHOLD;
        sourcePlayer.speakingTimer = 0.3f;  // fade-out delay

        // Write PCM to line
        line.write(packet.pcmData);
    }

    private static final float MAX_VOICE_DISTANCE = 20.0f;

    private PlayerAudioLine openLine() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
            sdl.open(format, SAMPLE_RATE / 5 * 2); // 200ms buffer
            sdl.start();
            return new PlayerAudioLine(sdl);
        } catch (LineUnavailableException e) {
            throw new RuntimeException("Cannot open audio playback line", e);
        }
    }

    @Override public void dispose() {
        for (PlayerAudioLine l : audioLines.values()) l.close();
        audioLines.clear();
    }

    private static final class PlayerAudioLine {
        private final SourceDataLine line;
        private final FloatControl gainControl;
        private final FloatControl panControl;

        PlayerAudioLine(SourceDataLine line) {
            this.line = line;
            this.gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            this.panControl  = line.isControlSupported(FloatControl.Type.BALANCE)
                ? (FloatControl) line.getControl(FloatControl.Type.BALANCE) : null;
        }

        void setVolume(float normalised) {
            // Convert linear 0..1 to dB
            float dB = normalised > 0 ? 20f * (float) Math.log10(normalised) : gainControl.getMinimum();
            gainControl.setValue(MathUtils.clamp(dB, gainControl.getMinimum(), gainControl.getMaximum()));
        }

        void setPan(float panLR) { // -1 = full left, +1 = full right
            if (panControl != null) panControl.setValue(panLR);
        }

        void write(byte[] pcm) { line.write(pcm, 0, pcm.length); }
        void close() { line.stop(); line.close(); }
    }
}
```

### 5.4 — Voice Routing via Kryonet

```
LOCAL MIC captured → VoiceCaptureSystem → onChunkReady callback
    → if HOST:  broadcast VoiceChunkPacket to all other clients via UDP
    → if CLIENT: send VoiceChunkPacket to host via UDP

HOST receives a client VoiceChunkPacket → forward to all OTHER clients

ALL clients receive VoiceChunkPacket → MultiplayerManager.pendingVoiceChunks.add(packet)

RENDER THREAD drains pendingVoiceChunks → VoicePlaybackSystem.receiveChunk()
```

The host acts as a voice relay. This adds one extra UDP hop for client-to-client voice, which at LAN speeds adds ~0.5–1ms latency — imperceptible.

---

## 6. Acoustic Event Synchronisation

When a player fires a sound pulse, the event must propagate through the acoustic graph on **all** clients simultaneously.

### 6.1 — Authority Model

The **host is authoritative** for sound propagation results. When any player fires a pulse:

1. Client sends `SoundEventPacket` to host via TCP.
2. Host runs `SoundPropagationOrchestrator.propagate()` to get the `PropagationResult`.
3. Host broadcasts the `SoundEventPacket` (not the PropagationResult — all clients re-run Dijkstra locally) to all clients.
4. All clients call `scene.triggerSoundPulse(packet.position, packet.strength)`.

This ensures each client runs its own Dijkstra on its local acoustic graph copy, keeping computation distributed. Because the graph is static during a test session, all clients produce identical results from the same input.

### 6.2 — SoundEventPacket broadcast

```java
// In ServerListener.received():
if (object instanceof SoundEventPacket evt) {
    // Forward to all clients including the originator
    // (originator already fired locally, but re-firing on receipt is harmless
    //  if we guard with a deduplication timestamp window)
    server.sendToAllUDP(evt);
}

// In UniversalTestScene — drain pending events:
multiplayerManager.processPendingEvents(this);
// Inside processPendingEvents:
scene.triggerSoundPulse(evt.position, evt.strength);
```

### 6.3 — Deduplication Guard

A client should not double-fire its own pulse (it already fired locally when the key was pressed). Guard with a short deduplication window:

```java
// In MultiplayerManager.ClientListener.received():
if (object instanceof SoundEventPacket evt) {
    // Skip if this is my own event echoed back within 100ms
    if (evt.sourcePlayerId == localPlayerId
        && System.currentTimeMillis() - lastLocalPulseTime < 100L) {
        return;
    }
    pendingSoundEvents.add(evt);
}
```

---

## 7. UniversalTestScene Integration

`UniversalTestScene` is the **main test screen** for multiplayer. All multiplayer state is encapsulated in `MultiplayerManager` so the scene itself changes minimally.

### 7.1 — New Fields in `UniversalTestScene`

```java
private MultiplayerManager   multiplayerManager;
private VoiceCaptureSystem   voiceCaptureSystem;
private VoicePlaybackSystem  voicePlaybackSystem;
private boolean              multiplayerEnabled = false;
```

### 7.2 — Startup — Host or Client Selection

Add a startup dialog (or command-line flag via `lwjgl3/ApplicationListener`) that determines role. For the test phase, a simple in-scene keybind overlay is sufficient:

```
Press [H] to HOST on this machine (LAN IP shown in overlay)
Press [C] to CONNECT as client (prompts for host IP via on-screen field)
Press [O] to play OFFLINE (default — existing behaviour unchanged)
```

These keys are active on the main menu screen before the test scene loads. Once chosen, the `MultiplayerManager` role is set and passed to `UniversalTestScene.show()`.

### 7.3 — `show()` additions

```java
@Override
public void show() {
    // ... existing setup ...

    multiplayerManager   = new MultiplayerManager();
    voicePlaybackSystem  = new VoicePlaybackSystem();
    disposalRegistry.register(multiplayerManager);
    disposalRegistry.register(voicePlaybackSystem);

    if (multiplayerEnabled) {
        if (isHost) {
            multiplayerManager.startHost();
        } else {
            multiplayerManager.connectAsClient(hostIp);
        }
        voiceCaptureSystem = new VoiceCaptureSystem(chunk -> {
            // Send own voice — called from capture thread
            if (isHost) multiplayerManager.broadcastVoice(chunk);
            else        multiplayerManager.sendVoiceToHost(chunk);
        });
        voiceCaptureSystem.start(localPlayerId);
        disposalRegistry.register(voiceCaptureSystem);
    }
}
```

### 7.4 — `render()` additions

```java
@Override
public void render(float delta) {
    // ... existing update/render ...

    if (multiplayerEnabled) {
        // Drain network queues — safe from render thread
        multiplayerManager.processPendingEvents(this);

        // Route voice chunks to playback
        VoiceChunkPacket chunk;
        while ((chunk = multiplayerManager.pendingVoiceChunks.poll()) != null) {
            RemotePlayer src = multiplayerManager.remotePlayers.get(chunk.sourcePlayerId);
            if (src != null) {
                voicePlaybackSystem.receiveChunk(chunk, src, camera.position);
            }
        }

        // Broadcast own position at 20Hz
        positionBroadcastTimer += delta;
        if (positionBroadcastTimer >= 0.05f) {
            positionBroadcastTimer = 0f;
            multiplayerManager.broadcastPosition(camera.position, camera.direction);
        }

        // Update speaking fade-out timers
        for (RemotePlayer rp : multiplayerManager.remotePlayers.values()) {
            if (rp.speakingTimer > 0) {
                rp.speakingTimer -= delta;
                if (rp.speakingTimer <= 0) rp.isSpeaking = false;
            }
        }
    }
}
```

### 7.5 — Sound Pulse triggered by local player

When the local player fires a pulse (V key or mic), send the event to the host:

```java
// In triggerSoundPulse() — after local visual + acoustic processing:
if (multiplayerEnabled) {
    SoundEventPacket packet = new SoundEventPacket();
    packet.sourcePlayerId = localPlayerId;
    packet.position       = new Vector3(position);
    packet.strength       = strength;
    packet.eventType      = SoundEvent.CLAP_SHOUT.ordinal();
    lastLocalPulseTime    = System.currentTimeMillis();

    if (isHost) multiplayerManager.broadcastSoundEvent(packet);
    else        multiplayerManager.sendSoundEventToHost(packet);
}
```

---

## 8. UI — Multiplayer Overlay

The existing `DiagnosticOverlay` gains a **NETWORK** tab when multiplayer is active.

### 8.1 — NETWORK Tab (DiagnosticOverlay)

```
[NETWORK]
Role:   HOST / CLIENT
IP:     192.168.1.5:54555
Players:
  [■] You        (local)
  [■] Player 2   ping 14ms   🔊 (speaking indicator)
  [■] Player 3   ping 22ms
Voice:  MIC ACTIVE  RMS: ████░░░░
        TX: 48 kbps  RX: 96 kbps
```

Speaking indicator: yellow `🔊` icon next to the player name, fading to grey when not speaking.

### 8.2 — In-World Speaking Bubble

When a remote player is speaking, render a small billboard above their capsule:

```java
private void renderSpeakingBillboard(Vector3 worldPos) {
    Vector3 billboardPos = new Vector3(worldPos).add(0, 2.0f, 0);
    // Use ShapeRenderer to draw a small pulsing ring — reuse SoundPulseShaderRenderer
    // at tiny scale (radius 0.2–0.3m, fast pulse)
    speakingRingRenderer.fireSmall(billboardPos);
}
```

---

## 9. Phase-by-Phase Implementation

### Phase MP-0 — Skeleton (2 days)
- [ ] Create `MultiplayerManager.java` with `startHost()` / `connectAsClient()` stubs.
- [ ] Create `RemotePlayer.java` and render capsule in `UniversalTestScene`.
- [ ] Add H / C / O key selection at startup.
- [ ] Verify two instances on loopback see each other's capsule position.

**Deliverable:** Two boxes moving in the same world.

---

### Phase MP-1 — Position Sync (1 day)
- [ ] Implement `PlayerStatePacket` broadcast at 20Hz.
- [ ] Client-side interpolation: lerp remote player position toward latest received position (lerp factor `0.15` per frame at 60 FPS → ~50ms effective delay).
- [ ] Add NETWORK tab to `DiagnosticOverlay`.
- [ ] Display ping (round-trip time via `PingPacket` / `PingAckPacket`).

**Deliverable:** Remote player capsule moves smoothly and believably.

---

### Phase MP-2 — Sound Event Sync (1 day)
- [ ] Implement `SoundEventPacket` send from `triggerSoundPulse()`.
- [ ] Host broadcasts to all clients.
- [ ] All clients call `scene.triggerSoundPulse()` on receipt.
- [ ] Verify shader sphere appears at correct world position on all clients simultaneously.
- [ ] Test material transmission: fire pulse near a WOOD wall — verify transmitted ghost pulse appears on both clients.

**Deliverable:** Pulse from any player visible on all clients, acoustic graph illuminates on all clients.

---

### Phase MP-3 — Voice Capture (2 days)
- [ ] Implement `VoiceCaptureSystem` with VAD (voice activity detection) silence gate.
- [ ] Send `VoiceChunkPacket` via Kryonet UDP.
- [ ] Host relays to all other clients.
- [ ] Add mic calibration: press `[TAB]` to open a threshold-adjustment slider in the NETWORK tab.

**Deliverable:** Local mic audio reaches remote players (verify with two headsets).

---

### Phase MP-4 — Voice Playback + Spatial Audio (2 days)
- [ ] Implement `VoicePlaybackSystem` with per-player `SourceDataLine`.
- [ ] Apply inverse-distance volume attenuation.
- [ ] Apply stereo pan based on relative X-axis offset.
- [ ] Show speaking indicator bubble above remote player capsule.
- [ ] Show speaking indicator in NETWORK tab.

**Deliverable:** Remote voice sounds directional — louder when close, panned based on direction.

---

### Phase MP-5 — Integration Testing (1 day)
- [ ] Run all test cases from section 10.
- [ ] Test 2-player loopback on same machine.
- [ ] Test 2-player over LAN on two devices.
- [ ] Test 3-player (three instances: one host, two clients).
- [ ] Document any deviations from expected behaviour.

**Total estimated time: 9 days (≈ 2 weeks with review and debugging)**

---

## 10. Test Cases

### Network

| ID | Scenario | Expected Result |
|---|---|---|
| N01 | Host starts, client connects | Client joins and sees host capsule. Host sees client capsule. |
| N02 | Client disconnects unexpectedly | Host removes client capsule within 2s. |
| N03 | Host disconnects | Client shows "Host disconnected" overlay. Session ends cleanly. |
| N04 | 3 clients connected | All 3 see each other. Position updates at 20Hz stable. |
| N05 | Packet loss simulation (drop UDP packets via Kryonet simulation) | Position interpolation keeps remote player movement smooth despite missing frames. |

### Sound Sync

| ID | Scenario | Expected Result |
|---|---|---|
| S01 | Player A fires pulse via V key | Shader sphere appears at Player A's position on BOTH clients within 50ms. |
| S02 | Player A fires pulse near WOOD wall | Transmitted ghost pulse (blue, dimmer) visible through the wall on both clients. |
| S03 | Player A and B fire pulses simultaneously | Both spheres render independently. Acoustic graph illuminates nodes from both sources. |
| S04 | Item impact (throw + hit wall) | Impact sound event propagates to both clients. Enemy (if present) reacts on both. |

### Voice

| ID | Scenario | Expected Result |
|---|---|---|
| V01 | Player A speaks into mic | Player B hears Player A's voice. Speaking indicator appears above A's capsule on B's screen. |
| V02 | Player A moves away from B | A's voice becomes quieter on B's device. Volume follows inverse-distance law. |
| V03 | Player A moves to B's left | A's voice is panned left in B's headphones. |
| V04 | Player A is silent for 300ms | Speaking indicator fades out on B's screen. |
| V05 | Two players speak simultaneously | Both voices audible simultaneously on third player's device, no glitching. |
| V06 | Mic threshold too high — A shouts | Calibration slider in NETWORK tab adjusts threshold down until A's normal voice triggers. |

### Integration

| ID | Scenario | Expected Result |
|---|---|---|
| I01 | Player A fires pulse; Player B is in the revealed area | B can see the geometry the pulse revealed on A's side. Both see same acoustic graph illumination. |
| I02 | Player A speaks while walking | A's walking footstep sounds propagate to graph (as normal); A's voice is spatial audio separate from game audio. No interference. |

---

## 11. Known Risks and Mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| `javax.sound` microphone unavailable on target machine | Medium | Fallback: use V-key pulse only. Show "MIC UNAVAILABLE" in NETWORK tab. |
| UDP voice chunks arriving out of order | High (any network) | Sequence number on `VoiceChunkPacket`. Discard out-of-order chunks — no jitter buffer needed at 20ms frame size on LAN. |
| Voice echo (speaker output picked up by mic) | Medium (no headset) | VAD silence gate reduces echo greatly. Add optional noise gate slider. |
| Desync: clients have different acoustic graphs | Low (graph is static) | Verify at session start — host sends a graph hash; clients verify. If mismatch, reconnect. |
| High CPU from PCM write on render thread | Low | `SourceDataLine.write()` is non-blocking when the line's buffer has space. At 20ms frames / 48kHz the buffer drains faster than it fills — CPU impact negligible. |
| Kryonet not registered before server starts | High (runtime crash) | `registerPackets()` is called in `MultiplayerManager` constructor, before `startHost()` or `connectAsClient()`. Unit test verifies registration. |
| Two local instances on same machine — port conflict | Low | Use configurable port offset: second instance auto-increments port by 1 if bind fails. |

---

*End of RESONANCE Multiplayer Test Plan — v1.0*
*This plan targets the test phase only. Full matchmaking, session persistence, and anti-cheat are out of scope.*

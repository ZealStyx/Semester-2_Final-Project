# RESONANCE - Host-As-Server Multiplayer Plan
> **Version:** 2.0 | **Engine:** libGDX + Kryonet | **Scope:** LAN / loopback sessions with local host authority
> **Goal:** one player clicks Host, the game starts an embedded server in the background, then the host process auto-connects to it as the first player
> **Primary entry point:** `MultiplayerTestMenuScreen` -> `UniversalTestScene`

---

## Overview

This document replaces the old server/client test framing with a host-as-server flow that matches the architecture pattern used in MyGame, but keeps it simpler for Resonance:

1. The player chooses Host or Join from the menu.
2. Hosting spins up an embedded server inside the same application process.
3. The host then auto-connects locally to that server so the host is also a normal player.
4. Other players connect to the host over LAN using a normal client path.

The intent is to keep all network authority in one place while avoiding a separate dedicated-server launch path for now. That keeps the multiplayer feature easy to test, easy to launch, and easy to evolve later if a standalone server becomes useful.

The first delivery should prove four things:

- The session can be created from the game UI without external tools.
- The host and clients share the same world state.
- Remote players are visible and can interact with acoustic gameplay.
- Voice and sound events can travel through the session without blocking gameplay.

---

## Architecture Target

### Runtime topology

```
Player clicks Host
    -> MultiplayerMenuScreen
    -> MultiplayerLaunchConfig(role = HOST)
    -> UniversalTestScene
    -> MultiplayerSessionManager.startHostSession()
        -> start embedded server on background thread
        -> bind TCP / UDP ports
        -> auto-connect local client to 127.0.0.1
        -> promote this process to the first player in the session

Other player clicks Join
    -> MultiplayerMenuScreen
    -> MultiplayerLaunchConfig(role = CLIENT, hostIp = x.x.x.x)
    -> UniversalTestScene
    -> MultiplayerSessionManager.connectAsClient(hostIp)
    -> client joins the host session
```

### Key design rules

- Host authority stays in the host process.
- The host is not a special offline spectator; the host is a real player with the same netcode path as everyone else.
- Clients never talk directly to each other; all replication flows through the host.
- If the host exits, the session ends for everyone unless a later phase adds host migration.

### Scope boundary

This plan covers multiplayer session bootstrapping, state sync, and voice/event relay. It does not require:

- dedicated server binaries
- NAT traversal
- account systems
- matchmaker services
- anti-cheat
- authoritative combat rollback

---

## Current Repo Fit

The repo already has the right seams to support this model:

- `MultiplayerLaunchConfig` already carries role and host IP.
- `MultiplayerManager` already owns server/client lifecycle and packet routing.
- `MultiplayerTestMenuScreen` already exposes host/join selection.
- `UniversalTestScene` already branches on multiplayer role and forwards state/audio updates.

That means the plan is not to invent a new multiplayer stack. The plan is to tighten the current scaffold around a single clean contract: host starts server first, then auto-connects locally, and every other player joins that same session.

---

## Phase Plan

### Phase 1 - Session Bootstrap Refactor

**Goal:** make host mode start a background server and then auto-connect as the local player.

**Work items:**

- Replace the current host flow with a single `startHostSession()` path.
- Start the embedded server on a worker thread so the render loop never blocks.
- Bind the server, then connect the host client to `127.0.0.1` using the same transport settings as remote clients.
- Ensure the host player gets a real network id and enters the same replication path as joiners.
- Add startup state reporting so the UI can show `Starting host`, `Server ready`, `Connecting host client`, and `Connected`.

**Deliverable:** host mode launches one session without a second executable or manual loopback setup.

**Acceptance criteria:**

- Clicking Host from the menu starts a playable session.
- The host sees itself as player 1 in the world.
- No deadlock or freeze occurs while the server binds or the local client connects.

---

### Phase 2 - Session Model and Identity

**Goal:** define a stable session model so host and client agree on player identity, ownership, and join order.

**Work items:**

- Introduce a session state object that tracks host role, local player id, remote players, and session status.
- Clarify id assignment rules so the host id is stable and join ids are deterministic.
- Add join/leave handling that updates the shared player roster immediately.
- Store the host name, player name, and connection metadata in the same place the session uses for replication.
- Make disconnection handling explicit so the UI can cleanly return to the menu.

**Deliverable:** a single source of truth for multiplayer session state.

**Acceptance criteria:**

- Join and leave events update the roster once and only once.
- Host and client can both render the same player list.
- Reconnecting does not leave stale player entries behind.

---

### Phase 3 - Core State Replication

**Goal:** replicate the minimum gameplay state needed for Resonance to feel shared.

**Work items:**

- Replicate player transform and facing direction at a fixed tick rate.
- Replicate acoustic events such as pulses, impacts, and other sound sources.
- Keep authoritative event emission on the host, with clients requesting or reporting events through packets.
- Add interpolation or smoothing on clients so movement does not jitter.
- Keep packet payloads small and serialization-friendly.

**Deliverable:** two or more players can move in the same world and see the same sound-driven interactions.

**Acceptance criteria:**

- Remote player motion is visible within a stable latency budget.
- Acoustic events appear on all peers in the same order.
- Late packets do not corrupt local simulation state.

---

### Phase 4 - Voice and Audio Relay

**Goal:** carry player voice through the same host-mediated session model.

**Work items:**

- Define voice chunk packet format and transport cadence.
- Capture mic input locally, package it, and send it to the host.
- Have the host forward voice data to other clients and optionally to itself through the same event path.
- Add speaking state so the UI can show who is talking.
- Decide whether playback is positional, stereo panned, or initially non-positional for simplicity.

**Deliverable:** live voice works across host and clients without breaking gameplay sync.

**Acceptance criteria:**

- Host voice reaches clients.
- Client voice reaches host and other clients through the host relay.
- Voice transmission does not stall the render thread.

---

### Phase 5 - UI and Player Experience

**Goal:** make hosting and joining obvious, fast, and debuggable.

**Work items:**

- Update the multiplayer menu so Host means `start server + auto-connect`.
- Keep Join mode for manual IP entry and LAN discovery.
- Show connection state, ping, and player count in the overlay.
- Show clear failure states for bind failure, connect failure, and timeout.
- Add a small in-game status strip for session state and speaking indicators.

**Deliverable:** the player understands what the game is doing without reading logs.

**Acceptance criteria:**

- Host and Join are obvious from the menu.
- Startup failures are visible and actionable.
- In-game feedback makes it clear whether the session is online and active.

---

### Phase 6 - LAN Discovery and Convenience

**Goal:** reduce friction for local testing without changing the host-authoritative model.

**Work items:**

- Keep LAN discovery for clients.
- Cache discovered hosts during the session menu.
- Make loopback join one keypress away for single-machine testing.
- Optionally surface the host's local IP so another device can join without guessing.

**Deliverable:** a friend can join quickly on the same network.

**Acceptance criteria:**

- Joining by LAN scan works on the same subnet.
- Joining by manual IP still works as a fallback.
- Local host testing remains the fastest path.

---

### Phase 7 - Stability, Cleanup, and Hardening

**Goal:** make the feature resilient enough for repeated testing.

**Work items:**

- Centralize cleanup for sockets, audio capture, and pending network queues.
- Guard against double-start and double-dispose paths.
- Add packet validation for null or malformed payloads.
- Ensure background threads stop when returning to the menu.
- Add lightweight logging around connect, disconnect, and relay failures.

**Deliverable:** multiplayer can be started and stopped repeatedly without leaking state.

**Acceptance criteria:**

- Returning to the menu always shuts down the session cleanly.
- Rehosting after exit works in the same process.
- Failed joins do not leave the app in a half-connected state.

---

## Implementation Order

1. Refactor host startup so it runs server-first, then auto-connects locally.
2. Normalize session identity and join/leave state.
3. Replicate movement and acoustic gameplay events.
4. Add voice relay and speaking indicators.
5. Polish the UI and error handling.
6. Keep LAN discovery and manual IP join as convenience features.
7. Harden cleanup and reconnect behavior.

---

## Suggested Module Responsibilities

### `core`

- Session manager
- Packet definitions
- Remote player state
- Host/client lifecycle
- Gameplay event replication

### `lwjgl3`

- Launch entry point
- Desktop-specific microphone and audio wiring
- Optional debug tooling for host/client startup

### `shared`

- Packet models
- Shared network constants
- Session enums and transport configuration

### `server`

- Reserved for a later headless server or external dedicated host path
- Not required for the first host-as-server milestone

---

## Risks and Mitigations

- **Risk:** the host blocks while starting the embedded server.  
  **Mitigation:** server startup must happen on a background thread, with UI state updates on the render thread.

- **Risk:** host and local client accidentally follow different code paths.  
  **Mitigation:** treat the host as a real client after bootstrapping, not as a special-case offline player.

- **Risk:** audio relay creates latency or thread contention.  
  **Mitigation:** keep capture, relay, and playback queues separate from render logic.

- **Risk:** session teardown leaks sockets or audio lines.  
  **Mitigation:** one cleanup path should own all multiplayer resources.

- **Risk:** packet spam causes jitter or bandwidth waste.  
  **Mitigation:** rate-limit state packets and keep voice chunks small.

---

## Definition of Done

The multiplayer feature is ready for the next phase when all of the following are true:

- A host can start a session from the game menu without a separate server binary.
- The host automatically joins its own server and plays as a normal player.
- A second client can join the same session over LAN or loopback.
- Player movement, acoustic events, and voice relay work across peers.
- Returning to the menu shuts everything down cleanly.

---

## Follow-Up After This Plan

If this architecture is approved, the next concrete implementation step should be a small refactor of `MultiplayerManager` and `UniversalTestScene` so host startup becomes a single server-first bootstrap path with auto-connect on success.

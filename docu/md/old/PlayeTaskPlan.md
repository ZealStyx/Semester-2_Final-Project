# Player System — Task Plan
**Project:** Resonance | LibGDX  
**Course:** CSEL 302 | Laguna State Polytechnic University | AY 2025–2026

---

## Overview

This task plan covers the design, implementation, and testing of the first-person player system in LibGDX. It is structured in phases that build on each other, from the bare movement controller up to full acoustic integration. Completion unblocks the Dijkstra propagation wiring and K-Means Director in subsequent phases.

---

## Phase 0 — Setup & Prerequisites

**Goal:** Confirm the LibGDX project is ready to receive the player system.

### Requirements
- LibGDX project using `gdx-bullet` (or custom physics) for collision detection.
- `InputMultiplexer` configured so player input does not conflict with UI or debug overlays.
- A minimal 3D test scene exists with at least a flat floor and one wall (can be placeholder boxes).
- `PerspectiveCamera` present and assigned to the player's viewport.

### Objectives
- [ ] Project compiles and runs to a blank 3D scene without errors.
- [ ] `PerspectiveCamera` renders from a first-person position.
- [ ] `InputProcessor` registered and receiving key/mouse events.
- [ ] Git branch `feature/player-system` created off `main`.

---

## Phase 1 — Movement Controller

**Goal:** Implement `PlayerController` with all five movement states driven by keyboard input.

### Movement State Table

| Key | State | Speed |
|-----|-------|-------|
| WASD | Walk | 3.5 m/s |
| Shift + WASD | Run | 7.0 m/s |
| Ctrl + WASD | Slow Walk | 1.2 m/s |
| C (hold) | Crouch | 1.8 m/s |
| No input | Idle | 0.0 m/s |

### Requirements

- Movement direction computed from camera forward/right vectors projected onto the XZ plane — no Y-axis drift.
- State priority enforced in this order: **Crouch > SlowWalk > Run > Walk**.
- Gravity applied manually each frame (`velocity.y -= GRAVITY * delta`); no physics engine dependency at this phase.
- `MovementState` exposed as a public enum for downstream systems to read.
- All speeds defined as named constants — no magic numbers inline.

### Objectives
- [ ] All five states produce the correct speed when measured in-game.
- [ ] Priority order is respected — holding C + Shift results in Crouch, not Run.
- [ ] Player does not drift vertically when moving on flat ground.
- [ ] `MovementState` enum value updates correctly every frame.
- [ ] No input handled when game is paused (`Gdx.input` reads blocked by pause flag).

---

## Phase 2 — Crouch System

**Goal:** Implement smooth camera and collision height transitions for crouching.

### Requirements

- Camera Y position lerps between standing eye height (1.6 m) and crouch eye height (0.75 m).
- Collision capsule/box height lerps in sync with the camera so the player fits through low gaps.
- Uncrouch is blocked if an obstacle is detected directly above the player (ceiling check raycast or AABB test).
- Crouch state is held only while C is held — releasing C triggers stand-up (with ceiling check).

### Objectives
- [ ] Camera smoothly transitions — no snapping or one-frame teleport.
- [ ] Player fits through a 1.1 m gap only while crouching.
- [ ] Standing up inside a 1.1 m gap is blocked until the player moves clear.
- [ ] Crouch transition speed is a tunable constant.

---

## Phase 3 — First-Person Camera & Mouse Look

**Goal:** Wire mouse input to the `PerspectiveCamera` for smooth first-person look.

### Requirements

- Mouse X delta rotates the player body (yaw) around the Y axis.
- Mouse Y delta tilts the camera (pitch) clamped to ±85°.
- Mouse captured with `Gdx.input.setCursorCatched(true)` on focus; released on pause/ESC.
- Sensitivity is a tunable constant.
- No roll axis; no inertia (direct mapping).

### Objectives
- [ ] Horizontal and vertical look work independently with no axis bleed.
- [ ] Pitch is clamped — camera cannot flip upside down.
- [ ] Cursor is captured in-game and freed on pause.
- [ ] Sensitivity change takes effect immediately without restart.

---

## Phase 4 — Interaction System (F Key)

**Goal:** Allow the player to interact with world objects via a forward raycast.

### Requirements

- On F pressed, cast a ray from camera position along camera direction, max range 2.5 m.
- On hit, call `interactable.onInteract(player)` on any object implementing the `Interactable` interface.
- If nothing is in range, interaction is silently ignored (no error, no log spam).
- `Interactable` interface defined in its own file, not nested inside the controller.

### Objectives
- [ ] Ray fires from the correct camera origin and direction.
- [ ] `onInteract` is called exactly once per F keypress — no repeat firing while held.
- [ ] Objects beyond 2.5 m do not trigger interaction.
- [ ] Interface is clean: one method `onInteract(PlayerController player)`.
- [ ] A stub `TestCrate` implementing `Interactable` logs a message when interacted with.

---

## Phase 5 — Footstep Sound Event Emitter

**Goal:** Emit acoustic events from the player to feed the `AcousticGraphEngine`.

### Footstep Timing & Intensity Table

| State | Emit Interval | Intensity |
|-------|--------------|-----------|
| Running | 0.30 s | 0.6 |
| Walking | 0.55 s | 0.3 |
| Slow Walk | 0.90 s | 0.1 |
| Crouching | 1.10 s | 0.05 |
| Idle | — | no emit |

### Requirements

- Footstep timer is separate from the render delta — it accumulates and fires independently.
- Timer resets on state change; no carry-over from a faster state's shorter interval.
- Events are only emitted while the player is grounded (airborne = no footsteps).
- Calls `AcousticGraphEngine.emitSoundEvent(position, intensity, SoundEventType.FOOTSTEP)`.
- F-key interaction emits its own event at intensity 0.4 (`SoundEventType.OBJECT_DROP`) via the `TestCrate`.

### Objectives
- [ ] Running emits more frequent, louder events than crouching.
- [ ] No events fire while the player is airborne.
- [ ] State transition resets the timer correctly — no double-fire or missed step.
- [ ] `AcousticGraphEngine` stub receives and logs all events with correct values.

---

## Phase 6 — Player Feature Extractor

**Goal:** Sample movement metrics into a rolling window for the K-Means Director.

### Feature Vector

| Field | Description |
|-------|-------------|
| `avgSpeed` | Mean horizontal speed (m/s) over the window |
| `rotationRate` | Mean yaw change per second (°/s) |
| `stationaryRatio` | Fraction of samples where speed < 0.1 m/s |
| `collisionRate` | Reserved — `0.0f` until collision callbacks are wired |
| `backtrackRatio` | Fraction of samples where player moved toward last known position |

### Requirements

- Sample every **0.2 s**; maintain a rolling **5-second** window (~25 samples max).
- Evict samples older than 5 s each update cycle.
- Compute and fire a `PlayerFeatures` event every **2.0 s**.
- Downstream `KMeansDirector` subscribes to this event (wired in the AI phase).
- No per-frame allocation — reuse a fixed-size circular buffer or evicting queue.

### Objectives
- [ ] `avgSpeed` accurately reflects each movement state when measured over 5 s.
- [ ] `stationaryRatio` reaches ≥ 0.9 after 5 seconds of standing still.
- [ ] Feature event fires at the correct 2 s cadence (verify with timestamp logging).
- [ ] No garbage collection spikes in the LibGDX profiler from this class.

---

## Phase 7 — Integration & Test Scene Validation

**Goal:** Validate the complete player system in a test scene covering all movement scenarios.

### Test Scene Zone Requirements

| Zone | Purpose |
|------|---------|
| Open room (10 × 10 m) | Walk, run, camera look |
| Narrow corridor with bend | Tight steering, acoustic NLoS prep |
| Ramp | Slope traversal, gravity |
| Staggered platforms | Verticality, jumping |
| Crouch alcove (1.1 m ceiling) | Crouch-only passage, ceiling check |
| Interactable crate | F key, sound event emit |

### Manual Validation Checklist
- [ ] Walk — WASD at correct speed, bob visible, steps every 0.55 s.
- [ ] Run — Shift held, noticeably faster, steps every 0.30 s.
- [ ] Slow Walk — Ctrl held, slower than walk, steps every 0.90 s.
- [ ] Crouch — C held, camera lowers, alcove passable, steps every 1.10 s.
- [ ] Interact — Face crate within 2.5 m, press F, event logged.
- [ ] Ceiling block — attempt to stand in alcove, stand-up blocked correctly.
- [ ] Airborne — jump or walk off ledge, no footstep events while in air.
- [ ] Feature log — stand still for 5 s, next feature event shows `stationaryRatio` ≥ 0.9.
- [ ] State cycling — rapidly switch states, no velocity spikes or NaN values.

### Performance Targets

| Metric | Target |
|--------|--------|
| Frame time | ≤ 16.7 ms (60 FPS) |
| Feature extractor GC alloc | Zero per frame |
| Raycast per frame (interaction) | Only on F keydown — not every frame |

---

## Deliverables

| File | Package | Description |
|------|---------|-------------|
| `PlayerController.java` | `resonance.player` | Movement, gravity, state machine |
| `PlayerFeatureExtractor.java` | `resonance.player` | Rolling window + feature event |
| `Interactable.java` | `resonance.player` | Interface for interactable objects |
| `AcousticGraphEngine.java` | `resonance.algorithms` | Sound event stub (Dijkstra wired later) |
| `SoundEventType.java` | `resonance.algorithms` | Enum for event classification |
| `TestCrate.java` | `resonance.test` | Stub `Interactable` for scene validation |

---

## Dependencies & Blockers

- **Blocks:** Dijkstra Phase — `AcousticGraphEngine.emitSoundEvent()` must be live and receiving events.
- **Blocks:** K-Means Director Phase — `PlayerFeatureExtractor` event must be firing correctly.
- **Blocked by:** Phase 0 — LibGDX project and 3D scene must be functional before any player code runs.

---

*Resonance | CSEL 302 Final Project | Laguna State Polytechnic University*
# RESONANCE — Master Development Plan
> **Last Updated:** April 21, 2026 (rev 3) | **Engine:** libGDX + Bullet Physics | **Build System:** Gradle Multi-Module

---

## Progress Update (Apr 22, 2026)

- Completed: `Fix K` camera idle breathing system (`CameraBreathingController`) integrated into `UniversalTestScene` with additive-only camera offsets restored after rendering.
- Completed: `Phase 1.4` diagnostic tab now includes 128-sample mic RMS history waveform and stamina bar in `DiagnosticOverlay` (`MIC_STAMINA` tab).
- Completed: `Phase 2.4` interaction targeting now uses Bullet raycast + stable `userValue` item registry in `UniversalTestScene`.
- In progress: `Phase 3.1/3.2` director scaffold added (`DirectorController`) with k=3 EMA classification and enemy sound-memory hook.
- In progress: `Phase 4` acoustic visual budget controls added (ray depth caps + throttled updates).
- Completed: mic fallback pulse keybind added (`V`) for test flow parity with keyboard pulse trigger.

---

## Table of Contents

1. [Project State Snapshot](#1-project-state-snapshot)
2. [Immediate Fixes — Sprint 0 (Do These Now)](#2-immediate-fixes--sprint-0-do-these-now)
   - Fix A — Weightless Item Carry
   - Fix B — Items Falling Through Ground
   - Fix C — Wall Height
   - Fix D — Sonar Pulse: 3D Sphere
   - Fix E — Bounce Points on Walls
   - Fix F — Mic Working in Test Screen
   - Fix G — Proper HUD
   - Fix H — Atmospheric Fog / Mist
   - Fix I — Crouch Alcove Low-Ceiling Physics
   - **Fix J — LiDAR-Style Long-Range Pulse Reveal** *(new)*
   - **Fix K — Camera Idle Breathing Animation** *(new)*
   - **Fix L — Item System Polish** *(new)*
   - **Fix M — Remove Blind Light Expansion on Pulse** *(new)*
3. [Phase 1 — TestScreen Completeness](#3-phase-1--testscreen-completeness)
4. [Phase 2 — Core Gameplay Loop](#4-phase-2--core-gameplay-loop)
5. [Phase 3 — Director AI & Enemy Systems](#5-phase-3--director-ai--enemy-systems)
6. [Phase 4 — Level Design & Procedural Generation](#6-phase-4--level-design--procedural-generation)
7. [Phase 5 — Horror Atmosphere & Polish](#7-phase-5--horror-atmosphere--polish)
8. [Phase 6 — Playtesting, QA & Release Prep](#8-phase-6--playtesting-qa--release-prep)
9. [Cross-Cutting Concerns](#9-cross-cutting-concerns)
10. [Suggestions](#10-suggestions)

---

## 1. Project State Snapshot

### What is Solid ✅
| System | File(s) | Status |
|---|---|---|
| Acoustic graph + Dijkstra propagation | `AcousticGraphEngine`, `DijkstraPathfinder` | Production-ready, unit-tested |
| Particle system (instanced, 3D meshes, trails) | `ParticleEmitter`, `TrailEmitter`, JSON presets | Advanced — very solid |
| Blind / fog effect pipeline | `BlindEffectController`, `BlindFogUniformUpdater` | Config-driven, modular |
| Body-cam VHS shader | `BodyCamVHSVisualizer`, `BodyCamPassFrameBuffer` | Complete |
| Inventory system | `InventorySystem` | Tested, stackable items |
| Player controller (movement, head-bob, crouch state) | `PlayerController` | Mostly complete, no stamina cap |
| Physics world (Bullet) | `UniversalTestScene` | Initialized, broadphase + solver present |
| Item definitions | `ItemDefinition`, `ItemType` | Config-driven, noise multipliers defined |
| Feature extractor (K-Means ready) | `PlayerFeatureExtractor` | Tested |

### What is Broken / Missing ❌
| Issue | Affected File(s) |
|---|---|
| Items fall through the floor | `UniversalTestScene` — rigid body / static floor collider missing |
| Carry system uses spring physics (items lag behind) | `CarrySystem` |
| No stamina — sprint is infinite | `PlayerController`, `UniversalTestScene` |
| No HUD (voice meter, stamina bar) | `UniversalTestScene` |
| Sonar pulse is a flat 2D ring below the player | `SonarRenderer`, `sonar_pulse.json` |
| Bounce nodes not visible on walls (only floor) | `AcousticBounce3DVisualizer`, `GeometricRayLayer` |
| Corridor walls are only 1.5 units tall (player eye-height 1.6) | `UniversalTestScene` wall `addBox` calls |
| Mic toggle in test screen does not reliably propagate a pulse | `UniversalTestScene.handleMicInput` |
| `CrouchAlcoveZone` has no actual low-ceiling physics geometry | `CrouchAlcoveZone` |
| No atmospheric fog / mist — darkness is a hard black void | `BlindEffectController`, world shader uniforms |
| **Pulse reveal range is tiny (2.5 m) — environment barely visible** | `blind_effect_config.json`, `SonarRenderer` |
| **No camera idle breathing — world feels static and lifeless** | `PlayerController` / new `CameraBreathingController` |
| **Item system incomplete — no throw, no break VFX, no view-model** | `CarrySystem`, `UniversalTestScene`, `PlayerInteractionSystem` |
| **Sonar pulse incorrectly triggers blind-effect light expansion** | `BlindEffectController.onSoundEvent` |

---

## 2. Immediate Fixes — Sprint 0 (Do These Now)

These are concrete, self-contained bugs. Fix them before any new feature work.

---

### Fix A — Weightless Item Carry (Always Glued to Player View)

**Problem:** `CarrySystem` uses a spring-damper with Bullet impulses. Because items have no
weight, the spring overshoots and items visibly lag/jitter behind the camera.

**Design Change:** Items in Resonance are weightless props. Remove the spring entirely.
The item's world position (and its rigid body's `worldTransform`) is set directly every frame
to a fixed offset from `camera.position + camera.direction * carryDistance`.

**File:** `core/src/main/java/io/github/superteam/resonance/player/CarrySystem.java`

```java
// In CarrySystem.update(float deltaSeconds):
// DELETE the entire spring-force block.
// REPLACE with:
public void update(float deltaSeconds) {
    updateCarryPoint(); // recomputes carryPoint from camera each frame

    if (heldItem == null) return;

    // Weightless snap — no physics force, just teleport to carry point
    heldItem.setWorldPosition(carryPoint);

    btRigidBody body = heldItem.rigidBody();
    if (body != null) {
        scratchTransform.setToTranslation(carryPoint);
        body.proceedToTransform(scratchTransform);   // teleport without impulse
        body.setLinearVelocity(Vector3.Zero);
        body.setAngularVelocity(Vector3.Zero);
        body.activate(true);
    }
}
```

**Expected Problems:**
- `proceedToTransform` bypasses Bullet's collision response, so carried items can clip through
  thin walls. This is acceptable for Resonance because items are always on the player's person.
- Bullet may still re-apply gravity the next substep. Disable gravity on the body while held:
  `body.setGravity(Vector3.Zero)` on pick-up, restore on drop.

---

### Fix B — Items Falling Through the Ground

**Problem:** Items are spawned with a `btBoxShape` but their rigid body's initial transform
places the bottom face _at_ `y=0`. Because Bullet's floor plane is `y=-0.05f` and items have
non-zero restitution, they clip through before the solver can respond.

**File:** `UniversalTestScene` (wherever items are created / added to physics world)

**Fix — two parts:**

1. **Spawn above ground.** When creating a `CarriableItem` rigid body, set initial
   `centerY = FLOOR_Y + (itemHalfHeight) + 0.02f` (a small lift above the floor surface).

2. **Add a static floor plane collider (not just a mesh box).** The visual floor box at
   `addBox(HUB_X, -0.05f, HUB_Z, 80.0f, 0.1f, 80.0f, false)` passes `addCollider=false`.
   Change to `true`, or separately add a `btStaticPlaneShape` at `y=0`:

```java
// In initPhysics() or buildScene():
btStaticPlaneShape floorShape = new btStaticPlaneShape(new Vector3(0,1,0), 0);
btRigidBodyConstructionInfo floorInfo = new btRigidBodyConstructionInfo(0, null, floorShape);
btRigidBody floorBody = new btRigidBody(floorInfo);
physicsWorld.addRigidBody(floorBody);
// store reference for disposal later
```

**Expected Problems:**
- The 80×80 floor mesh with `addCollider=true` creates a very large `btBoxShape`. This is
  fine at this scale but will eat broadphase budget. Prefer the static plane shape above.
- Items may bounce. Tune `restitution` on the `btRigidBodyConstructionInfo` to `0.1f` and
  friction to `0.6f` to stop excessive bouncing.

---

### Fix C — Wall Height in Universal Test Scene

**Problem:** Corridor walls are `height=1.5f` (half-extent actually means full height in
`addBox`). The player eye is at `EYE_HEIGHT = 1.6f`, making the player appear to be looking
over every wall. Outer boundary walls at `height=4.0f` are fine.

**File:** `UniversalTestScene.java` — `buildScene()` method, lines 453–463

**Fix:** Raise every corridor wall call from `1.5f` to `3.5f`:

```java
// BEFORE (example — applies to all 6 corridor wall addBox calls):
addBox(HUB_X + 8.0f, 0.75f, HUB_Z + CORRIDOR_HALF_WIDTH, 16.0f, 1.5f, 0.2f, true);

// AFTER — raise height and adjust centerY so base stays on floor:
//   centerY = height / 2 = 1.75f
addBox(HUB_X + 8.0f, 1.75f, HUB_Z + CORRIDOR_HALF_WIDTH, 16.0f, 3.5f, 0.2f, true);
```

Apply the same `1.5f → 3.5f` & `centerY 0.75f → 1.75f` change to the four other corridor
wall pairs (lines 454, 456, 457, 459, 460, 462, 463).

**Expected Problems:** None significant. Ensure `BlindEffectController`'s fog uniforms
(`u_fogEnd`) are not tightly tuned to a 1.5-tall world — they are not, fog is distance-based.

---

### Fix D — Sonar Pulse: Flat Circle → 3D Sphere on Player

**Problem:** `SonarRenderer` draws a billboard circle (or a 2D ring) projected below the
player. This reads as a floor effect, not a spatial sound wave.

**Design Change:** Emit a `sonar_pulse` particle effect (already defined in
`assets/particles/presets/sonar_pulse.json`) centred on the _player's 3D world position_,
not a projected floor point. The sphere should expand outward, fade with distance, and
disappear — matching how sound propagates.

**File:** `UniversalTestScene.java` — `handleMicInput` and wherever `SonarRenderer` is called

```java
// In handleMicInput, replace the pulsePos line:
// BEFORE:
Vector3 pulsePos = new Vector3(camera.position).mulAdd(camera.direction, 1.2f);

// AFTER — pulse originates from the player, NOT offset forward:
Vector3 pulsePos = new Vector3(camera.position);

// Then fire the particle effect at that exact point:
particleManager.spawn("sonar_pulse", pulsePos);
```

**File:** `SonarRenderer.java` — ensure it renders an expanding _sphere_ mesh, not a flat ring.
The simplest approach is to use the particle system entirely for visuals and make `SonarRenderer`
a no-op wrapper (or remove it and rely on `sonar_pulse` preset). If the SonarRenderer is kept
for graph-node illumination, keep only the node-flash logic and strip any ring drawing.

**Expected Problems:**
- `sonar_pulse` preset may be tuned as a small ring. Open
  `assets/particles/presets/sonar_pulse.json` and set:
  - `"emissionShape": "SPHERE"`, radius `0.1` → grows via velocity outward
  - `"startSize"` small, `"endSize"` large
  - `"startAlpha": 0.8`, `"endAlpha": 0.0` for fade
- The particle system is 3D instanced — the sphere will naturally occlude behind walls, giving
  the correct "sound doesn't pass through solid objects visually" read.

---

### Fix E — Bounce Points Visible on Walls

**Problem:** `AcousticBounce3DVisualizer` draws bounce nodes but they appear only near the
floor because ray origins are cast from `y ≈ 0`. The `GeometricRayLayer` needs rays fired at
multiple elevation angles to hit walls.

**File:** `GeometricRayLayer.java` and/or `AcousticBounce3DVisualizer.java`

**Fix:** When generating acoustic rays from the source node, include vertical spread:

```java
// Instead of rays only in the XZ plane:
for (int i = 0; i < RAY_COUNT; i++) {
    float azimuth   = (i * MathUtils.PI2) / RAY_COUNT;
    float elevation = MathUtils.random(-MathUtils.PI / 4f, MathUtils.PI / 4f); // ±45°
    direction.set(
        MathUtils.cos(elevation) * MathUtils.cos(azimuth),
        MathUtils.sin(elevation),
        MathUtils.cos(elevation) * MathUtils.sin(azimuth)
    );
    // ... existing ray-march / intersection logic
}
```

Increase `RAY_COUNT` from whatever the current value is to at least `24` so vertical hits are
visible.

Additionally, render bounce points as small filled spheres (use `ShapeRenderer.point` or a
tiny billboard quad with a bright colour — cyan `#00FFEE` reads well against the dark
environment).

**Expected Problems:**
- More rays = more CPU per propagation event. Keep rays lazy: only recast when a new sound
  event fires (which is already gated on the microphone cooldown).
- Rays may miss very thin walls due to step size. Reduce ray march step to `0.15f` in the
  hit-test loop.

---

### Fix F — Mic Working on the Test Screen

**Problem:** Pressing `M` toggles the mic, but `handleMicInput` only fires a pulse if
`frame.triggered` is true _and_ the cooldown is zero. The cooldown default and frame trigger
logic can cause silent failures — especially if the `RealtimeMicSystem` never reports a frame.

**File:** `UniversalTestScene.java` — `handleMicInput` + `toggleMicInput`

**Fix — three things:**

1. **Show mic status in HUD** (see Fix G). Without visual feedback the developer can't tell if
   the mic is receiving input.

2. **Add a fallback key-triggered pulse** — while developing, `Space` or `V` should always
   fire a pulse at the player position regardless of mic level, so you can test the graph
   without a microphone.

```java
// In keyDown, alongside the M toggle:
if (keycode == Input.Keys.V) {
    triggerSoundPulse(new Vector3(camera.position), 1.0f);
    return true;
}
```

3. **Log mic RMS each frame** (debug mode only):
```java
if (micEnabled && frame != null) {
    Gdx.app.log("Mic", "RMS=" + frame.rms + " triggered=" + frame.triggered);
}
```
This alone reveals most mic issues (wrong device, too-low gain, wrong sample rate).

**Expected Problems:**
- On some JVM configurations `AudioRecord` / `javax.sound` picks the wrong input device.
  Expose `MIC_THRESHOLD_RMS` as a runtime slider in the diagnostic overlay (or `blind_effect_config.json`)
  so it can be tuned without recompiling.

---

### Fix G — Proper HUD: Voice Meter + Stamina Bar

**Design:** A minimal always-visible HUD rendered in screen-space after the VHS pass.

**New class:** `core/.../hud/GameHud.java`

```java
public final class GameHud {
    private final ShapeRenderer shape;
    private final BitmapFont font;

    // Called every frame with current values
    public void render(float screenW, float screenH,
                       float voiceLevel,   // 0..1 normalised mic RMS
                       float stamina,      // 0..1
                       MovementState state,
                       boolean micActive) { ... }
}
```

**Layout (bottom-left corner, retro green monochrome):**
```
[MIC ▓▓▓▓░░░░]   ← voice level bar, 120px wide
[STA ▓▓▓▓▓▓░░]   ← stamina bar, same width
     CROUCH / RUN / WALK  ← state label
```

**Stamina System — add to `PlayerController.java`:**

```java
private static final float MAX_STAMINA         = 100f;
private static final float STAMINA_DRAIN_RATE  = 20f;  // per second while running
private static final float STAMINA_REGEN_RATE  = 12f;  // per second while not running
private static final float STAMINA_MIN_TO_RUN  = 15f;  // prevent flicker at zero

private float stamina = MAX_STAMINA;

// In updateMovementState():
if (currentState == MovementState.RUN) {
    stamina = Math.max(0, stamina - STAMINA_DRAIN_RATE * delta);
    if (stamina == 0) {
        // Force walk — override state
        currentState = MovementState.WALK;
    }
} else {
    stamina = Math.min(MAX_STAMINA, stamina + STAMINA_REGEN_RATE * delta);
}

// Guard: can't start running if stamina < STAMINA_MIN_TO_RUN
// Modify the run branch in updateMovementState to check this.

public float getStaminaNormalized() { return stamina / MAX_STAMINA; }
```

**Voice Meter:** Expose `RealtimeMicSystem.Frame.rms` via a normalised accessor. Divide raw
RMS by `MIC_THRESHOLD_RMS * 2` (clamp to 1.0) to get a 0–1 display value.

**Expected Problems:**
- Stamina drain while sprinting may feel punishing before level design defines sprint corridors.
  Expose `STAMINA_DRAIN_RATE` and `STAMINA_REGEN_RATE` in `balancing_config.json` so it can
  be tuned without recompiling.
- HUD must draw _after_ the VHS post-process framebuffer is composited, otherwise it gets
  distorted by the scanline/grain shader. Ensure `gameHud.render()` is called after
  `bodyCamVHSVisualizer.render()` in the main `render()` method.

---

### Fix H — Atmospheric Fog / Mist (Not Just Black)

**Problem:** When the blind effect is at maximum darkness, the world goes flat black. This
looks like a missing texture rather than horror atmosphere.

**Two-layer approach:**

**Layer 1 — World Shader Fog:** The `worldShader` already supports `u_fogColor`, `u_fogStart`,
`u_fogEnd`. Currently these are hard-coded near zero distance. Change them to be data-driven
from `blind_effect_config.json`:

```json
{
  "fogColor": [0.04, 0.04, 0.08],
  "fogStartNear": 0.8,
  "fogStartFar": 3.5,
  "fogEndNear": 2.0,
  "fogEndFar": 18.0
}
```

Lerp `u_fogStart` and `u_fogEnd` based on `blindLevel` (0 = full visibility, 1 = deepest dark).
At `blindLevel=1.0`, fog starts at `0.8m` — the player can only see a ghostly haze right in
front of them, not nothing.

**Layer 2 — Volumetric Mist Particles:** Spawn a persistent low-density `mist` particle emitter
at the player's feet that follows the player. The `mist` preset already exists
(`assets/particles/presets/mist.json`). Use `ParticleEffect` in `LOOP` mode:

```java
// In UniversalTestScene.setUp():
mistEmitter = particleManager.spawnLooping("mist", camera.position);

// In render loop, update emitter position:
mistEmitter.setPosition(camera.position.x, FLOOR_Y, camera.position.z);
```

Tune the `mist` preset: large particles (`startSize ~3.0`), very low alpha (`startAlpha 0.12`),
slow upward drift, 8-metre radius. This gives the impression of ground fog without obscuring
gameplay.

**Expected Problems:**
- Particles behind the fog-shader may Z-fight (particle alpha blends before the fog distance
  clips). Ensure `DepthMode.NO_WRITE` is set on the mist emitter (the `ParticleDefinition`
  `depthMode` field).
- The `mist` preset looping at player position emits continuously — cap the active particle
  count to `~80` to avoid GPU load.

---

### Fix I — Crouch Alcove: Actual Low-Ceiling Physics Geometry

**Problem:** `CrouchAlcoveZone` extends `BaseShellZone` and only writes to `mutableState()`.
There is no actual low ceiling collision body, so the player can walk through upright.

**File:** `CrouchAlcoveZone.java` — add a ceiling collider during `setUp()`.

The `BaseShellZone` gives a `center` and `halfExtent`. Add a ceiling box at `y=1.2f`
(just below standing eye height of 1.6f, forcing a crouch):

```java
@Override
public void setUp() {
    super.setUp();
    // Ceiling collider — thin slab at y=1.2, full zone width
    // Requires a reference to the physics world.
    // Option A: Pass physicsWorld in constructor.
    // Option B: Return a list of ColliderDescriptor and let UniversalTestScene add them.
}
```

**Recommended approach (Option B — cleaner architecture):**

Add `List<ColliderDescriptor> getColliders()` to `TestZone` interface. `BaseShellZone`
returns floor/wall descriptors; `CrouchAlcoveZone` overrides and adds a ceiling descriptor.
`UniversalTestScene.buildScene()` iterates zones and registers their colliders.

```java
// ColliderDescriptor (new small data class):
public record ColliderDescriptor(float cx, float cy, float cz,
                                  float w, float h, float d) {}
```

**PlayerController crouch logic:** Already present (`MovementState.CROUCH` at `speed=1.8f`).
Ensure that when the player enters the low-ceiling zone, `PlayerController.tryCrouch()` is
called automatically (trigger volume detection) — or expose a `forceStayCrouched` flag that
`UniversalTestScene` sets when `camera.position` is inside the zone AABB.

**Expected Problems:**
- Auto-forcing crouch when entering a zone may feel bad if the player is sprinting in.
  Use a velocity-fade: linearly reduce `moveSpeed` as the player approaches the ceiling, and
  only lock crouch when they are inside the low-ceiling AABB.

---

---

### Fix J — LiDAR-Style Long-Range Pulse Reveal

**Problem:** The sonar pulse currently reveals the environment at a maximum of `2.5 m`
(`clap_shout_radius_meters` in `blind_effect_config.json`) and the particle sphere tops out at
`radialPulseMax: 4.0` in `sonar_pulse.json`. At these scales the player cannot read the
shape of a room — a corridor barely registers. The design goal is a **LiDAR flash**: a fast-
expanding sphere that momentarily silhouettes the walls, ceiling, floor, and item positions
out to a meaningful distance before rapidly fading.

**Changes Required — three files:**

**1. `assets/config/blind_effect_config.json`** — raise the sonar reveal radius and duration:
```json
"sonar_reveal": {
  "enabled": true,
  "clap_shout_radius_meters": 18.0,
  "duration_seconds": 0.7,
  "triggers_acoustic_visualization": true
}
```
The reveal now reads `18 m` — enough to outline a large room. Duration is kept short (`0.7 s`)
so the flash reads as a momentary scan rather than a sustained light source. Keep
`visibility_clamp_max_meters` in sync — raise it to `20.0`:
```json
"visibility_clamp_max_meters": 20.0
```

**2. `assets/particles/presets/sonar_pulse.json`** — reshape from a flat ring to a full
omnidirectional sphere that travels far:
```json
{
  "emissionShape": "SPHERE",
  "emissionRate": 300,
  "radialPulse": true,
  "radialPulseSpeed": 14.0,
  "radialPulseMax": 22.0,
  "speedMin": 8.0,
  "speedMax": 14.0,
  "lifetimeMin": 1.2,
  "lifetimeMax": 1.8,
  "startColor": [0.25, 0.85, 1.0, 0.75],
  "endColor": [0.05, 0.3, 0.9, 0.0],
  "startSize": 0.08,
  "endSize": 0.02,
  "gravity": 0.0,
  "blendMode": "ADDITIVE",
  "depthMode": "NO_WRITE"
}
```
Key changes: `SPHERE` emission instead of `RING`, pulse speed raised from `2.0 → 14.0`,
pulse max from `4.0 → 22.0`, particle count raised to `300` (still GPU-friendly with
instanced rendering). `depthMode: NO_WRITE` prevents the pulse sphere from occluding
particles already in the scene.

**3. `SonarRenderer.java`** — the `spawnFromPropagation` lifetime is driven by
`soundBalancingConfig`, which maps `CLAP_SHOUT.pulseLifetimeSeconds = 1.1` in
`balancing_config.json`. Raise this to `1.6` so revealed nodes stay illuminated long enough
for the player to read the space:
```json
"CLAP_SHOUT": {
  "baseIntensity": 0.9,
  "cooldownSeconds": 1.2,
  "pulseLifetimeSeconds": 1.6
}
```

**Design note — the two-phase reveal:**
The pulse should read in two visual layers simultaneously:
1. **The sphere front** — the fast-moving wave of particles sweeping outward (handled by the
   particle preset above).
2. **The revealed geometry silhouette** — the world-shader fog briefly retreats, exposing wall
   surfaces hit by the wave. This is the `BlindSonarRevealModifier` expanding to `18 m`.

The combination gives a true LiDAR feel: you see the wave hit surfaces and those surfaces
glow briefly before fading back into darkness.

**Expected Problems:**
- At `18 m` reveal radius, the player may be able to see an enemy if one is nearby. This is
  intentional — they should be able to see the enemy for a fraction of a second and then lose
  them. Configure enemy mesh colour to be a near-black silhouette so it reads as a shape, not
  a character.
- `emissionRate: 300` at pulse time is a spike. Because the pulse fires at most once per
  `cooldownSeconds: 1.2`, the average rate is well within budget. Profile in the diagnostic
  overlay; if frame-time spikes exceed 5 ms, reduce to `200`.
- `radialPulseMax: 22.0` larger than corridor width — particles will fly into walls. With
  `depthMode: NO_WRITE` and `ADDITIVE` blend they fade gracefully against surfaces rather
  than clipping. Verify this in-engine with the `ShaderCorridorZone`.

---

### Fix K — Camera Idle Breathing Animation

**Status:** ✅ Implemented (Apr 21, 2026)

**Problem:** The camera is completely static when the player stands still. In a first-person
horror game this makes the world feel frozen and the player feel disembodied.

**Design constraint:** The breathing motion must be **purely additive and non-destructive** —
it is layered _on top of_ the raw mouse-look camera transform and has zero effect on aim,
pickup raycasts, or movement direction calculations. No gameplay system should read the
breathing offset; they all read the base camera.

**New class:** `core/.../player/CameraBreathingController.java`

```java
public final class CameraBreathingController {

    // Two-oscillator model: slow chest (inhale/exhale) + fast subtle flutter
    private static final float BREATHE_FREQ_HZ   = 0.22f;  // ~13 breaths/minute at rest
    private static final float FLUTTER_FREQ_HZ   = 0.80f;
    private static final float VERTICAL_AMPLITUDE = 0.0028f; // world units — very subtle
    private static final float ROLL_AMPLITUDE_DEG = 0.18f;   // tiny rotation
    private static final float SPRINT_SCALE       = 2.8f;    // heavier breathing while running
    private static final float LERP_SPEED         = 3.5f;    // smooth transition between states

    private float phase = 0f;
    private float flutterPhase = 0f;
    private float currentScale = 1f;

    // Call each frame BEFORE applying to camera
    public void update(float delta, MovementState state) {
        phase        += delta * BREATHE_FREQ_HZ  * MathUtils.PI2;
        flutterPhase += delta * FLUTTER_FREQ_HZ  * MathUtils.PI2;

        float targetScale = (state == MovementState.RUN) ? SPRINT_SCALE : 1f;
        currentScale = MathUtils.lerp(currentScale, targetScale, delta * LERP_SPEED);
    }

    // Returns Y-axis offset to add to camera.position
    public float getVerticalOffset() {
        float chest   = MathUtils.sin(phase)         * VERTICAL_AMPLITUDE;
        float flutter = MathUtils.sin(flutterPhase)  * VERTICAL_AMPLITUDE * 0.3f;
        return (chest + flutter) * currentScale;
    }

    // Returns roll angle (degrees) to add to camera.up rotation
    public float getRollDegrees() {
        return MathUtils.sin(phase * 0.5f) * ROLL_AMPLITUDE_DEG * currentScale;
    }
}
```

**Integration in `UniversalTestScene.java`:**

The key is to apply breathing _after_ `playerController.update()` and _after_ `camera.update()`
has resolved the final look direction, but _before_ rendering:

```java
// In UniversalTestScene — add field:
private final CameraBreathingController breathingController = new CameraBreathingController();

// In render(), after playerController.update(delta) and camera.update():
breathingController.update(delta, playerController.getMovementState());

// Apply vertical bob — store original Y, offset, restore after render
float breatheY = breathingController.getVerticalOffset();
camera.position.y += breatheY;

// Apply roll — temporarily rotate camera.up around camera.direction
float rollDeg = breathingController.getRollDegrees();
if (Math.abs(rollDeg) > 0.001f) {
    tmpMatrix.setToRotation(camera.direction, rollDeg);
    camera.up.mul(tmpMatrix).nor();
}

camera.update();    // recompute combined matrix with the offset applied
renderWorld();      // draw scene with breathing-offset camera

// IMPORTANT: restore original position and up vector so no system reads the offset
camera.position.y -= breatheY;
if (Math.abs(rollDeg) > 0.001f) {
    tmpMatrix.setToRotation(camera.direction, -rollDeg);
    camera.up.mul(tmpMatrix).nor();
}
camera.update();
```

By saving and restoring `camera.position.y` and `camera.up` around the render call, every
other system (physics raycasts, interaction checks, sound node lookup, HUD projection) still
reads the _real_ unmodified camera transform.

**Tuning the breath feel:**
The amplitude values above are deliberately small. Sprint breathing at `SPRINT_SCALE = 2.8f`
means vertical amplitude = `0.0028 * 2.8 ≈ 0.008 m` — about 8 mm of camera movement, which
at normal FOV reads as a gentle sway. This is noticeable but not nauseating.

At `stamina < 20%` increase `SPRINT_SCALE` temporarily to `4.5f` to sell exhaustion. Wire
this into the stamina system from Fix G:
```java
float breathScale = (stamina < 0.2f)
    ? MathUtils.map(stamina, 0f, 0.2f, 4.5f, SPRINT_SCALE)
    : (state == MovementState.RUN ? SPRINT_SCALE : 1f);
```

**Expected Problems:**
- On the first frame after a large mouse input, the lerp from `currentScale` may cause a
  visible jump if the player swings the camera fast. This is imperceptible in practice because
  `getVerticalOffset()` is tiny, but if it is noticed, clamp `currentScale` to a max delta
  per frame: `Math.min(currentScale + delta * LERP_SPEED, targetScale)`.
- The roll applied to `camera.up` must be re-normalised after applying. Include `.nor()` as
  shown — if omitted, floating-point drift will slowly corrupt the up vector.
- When the player is crouching `(MovementState.CROUCH)`, reduce breathing slightly:
  target scale `0.7f` — crouching implies slower, more controlled breath.

---

### Fix L — Item System Polish

**Problem:** The item system has the data layer (`ItemDefinition`, `CarriableItem`,
`InventorySystem`) and the physics layer (`ImpactListener`, `CarrySystem`) but the moment-to-
moment gameplay experience is unpolished: no throw action, no break visual feedback, no view-
model, and no interaction prompt. This fix completes the system end-to-end.

---

#### L.1 — Throw Action

**Files:** `UniversalTestScene.java`, `CarrySystem.java`

Add a dedicated throw key (default `F` while holding an item). Throw uses `throwStrength`
from `ItemDefinition` — `METAL_PIPE` at `20f`, `GLASS_BOTTLE` at `14f`, etc.

```java
// In CarrySystem — new method:
public void throwHeldItem(Vector3 cameraForward) {
    if (heldItem == null) return;
    btRigidBody body = heldItem.rigidBody();
    if (body != null) {
        // Restore gravity before throw
        body.setGravity(new Vector3(0, -9.8f, 0));
        float strength = heldItem.definition().throwStrength();
        body.applyCentralImpulse(new Vector3(cameraForward).scl(strength));
        body.activate(true);
    }
    releaseHeld();   // existing drop method — sets heldItem = null
}
```

```java
// In UniversalTestScene.keyDown:
if (keycode == Input.Keys.F && carrySystem.isHoldingItem()) {
    carrySystem.throwHeldItem(camera.direction);
    return true;
}
```

---

#### L.2 — Item Break Visual Feedback

**Files:** `ImpactListener.java`, `UniversalTestScene.java`

When `appliedImpulse >= item.definition().breakThreshold()`, `ImpactListener` already adds
the item to `pendingBreakItems`. Consume this list in the scene update and trigger feedback:

```java
// In UniversalTestScene.update() — after impactListener.onCarriableImpact():
Array<CarriableItem> broken = impactListener.consumePendingBreakItems();
for (CarriableItem item : broken) {
    // 1. Spawn explosion particle effect at impact position
    particleManager.spawn("explosion", item.worldPosition());

    // 2. Spawn smoke_puff lingering at same position
    particleManager.spawn("smoke_puff", item.worldPosition());

    // 3. Remove the item's rigid body from the physics world
    physicsWorld.removeRigidBody(item.rigidBody());

    // 4. Remove from scene item list
    sceneItems.removeValue(item, true);

    // 5. Show brief HUD message: "[Item name] destroyed"
    hudMessageQueue.add(item.definition().displayName() + " destroyed", 2.0f);
}
```

Both `explosion` and `smoke_puff` presets are already defined in
`assets/particles/presets/`. No new assets needed.

---

#### L.3 — View-Model (Held Item Rendered in First Person)

The existing `VIEW_MODEL_*` constants in `UniversalTestScene` define an offset but nothing
renders there. Add a simple view-model pass.

**File:** `UniversalTestScene.java`

```java
// In renderWorld(), after main scene draw, before HUD:
if (carrySystem.isHoldingItem()) {
    CarriableItem held = carrySystem.heldItem();

    // Compute view-model world position
    Vector3 vmPos = new Vector3(camera.position)
        .mulAdd(camera.direction, VIEW_MODEL_OFFSET_FORWARD)
        .mulAdd(camera.right(),   VIEW_MODEL_OFFSET_RIGHT)
        .mulAdd(camera.up,        VIEW_MODEL_OFFSET_UP);

    // If the item has a ModelInstance, render it at vmPos with VIEW_MODEL_SCALE
    // If no model yet — render a placeholder coloured box using ShapeRenderer
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    shapeRenderer.setColor(getItemColour(held.definition().itemType()));
    shapeRenderer.box(vmPos.x - 0.05f, vmPos.y - 0.05f, vmPos.z - 0.05f, 0.1f, 0.1f, 0.1f);
    shapeRenderer.end();
}
```

```java
// Helper — item type to colour for placeholder view-model:
private Color getItemColour(ItemType type) {
    return switch (type) {
        case METAL_PIPE     -> new Color(0.5f, 0.5f, 0.55f, 1f);
        case GLASS_BOTTLE   -> new Color(0.4f, 0.8f, 0.6f,  1f);
        case CARDBOARD_BOX  -> new Color(0.6f, 0.45f, 0.3f, 1f);
        case CONCRETE_CHUNK -> new Color(0.55f, 0.55f, 0.5f, 1f);
        default             -> Color.WHITE;
    };
}
```

When 3D models are available, swap the `ShapeRenderer` block for a `ModelBatch.render()`
call with the item's `ModelInstance`.

---

#### L.4 — Interaction Prompt

**File:** `UniversalTestScene.java` — `renderHud()` block

When an item is in range and the player is facing it, show a small on-screen prompt. Use the
existing `hudBatch` + `hudFont`:

```java
if (playerInteractionSystem.hasTargetInRangeAndFacing() && !carrySystem.isHoldingItem()) {
    String promptText = "[E] Pick up";
    CarriableItem nearest = findNearestFacingItem();
    if (nearest != null) {
        promptText = "[E] " + nearest.definition().displayName();
    }
    float cx = Gdx.graphics.getWidth() * 0.5f;
    float cy = Gdx.graphics.getHeight() * 0.5f - 28f;
    hudFont.draw(hudBatch, promptText, cx - 50f, cy);
}

// When holding:
if (carrySystem.isHoldingItem()) {
    hudFont.draw(hudBatch, "[F] Throw  [E] Drop", 12f, 48f);
}
```

---

#### L.5 — Item State Persistence (No Silent Vanishing)

**Problem:** When an item's `btRigidBody` is removed from the world (e.g., on break), the
`CarriableItem` wrapper still exists in `sceneItems`. If not cleaned up, stale items can be
picked up again, causing a crash when the physics body is null.

**Fix:** Add a state flag to `CarriableItem`:

```java
public enum ItemState { WORLD, CARRIED, BROKEN }
private ItemState state = ItemState.WORLD;
public ItemState state() { return state; }
public void setState(ItemState state) { this.state = state; }
```

Guard all interaction systems: only interact with items in `ItemState.WORLD`. Set
`ItemState.CARRIED` on pickup, `ItemState.WORLD` on drop, `ItemState.BROKEN` on destruction.
Filter `sceneItems` in `PlayerInteractionSystem` and `CarrySystem` to skip non-WORLD items.

---

#### L.6 — Drop Behaviour: Item Stays in Scene

When the player presses E to **drop** (not throw), the item should fall from the carry
position and come to rest naturally, remaining in the world for future pickup.

```java
// In CarrySystem.dropHeldItem():
public void dropHeldItem() {
    if (heldItem == null) return;
    btRigidBody body = heldItem.rigidBody();
    if (body != null) {
        body.setGravity(new Vector3(0, -9.8f, 0));  // restore gravity
        body.activate(true);
        // Apply tiny downward impulse so it settles quickly
        body.applyCentralImpulse(new Vector3(0, -1.5f, 0));
    }
    heldItem.setState(ItemState.WORLD);
    heldItem = null;
}
```

**Expected Problems (all L sub-fixes):**
- `VIEW_MODEL_*` constants place the model slightly inside the near clipping plane at close
  walls. Set `camera.near = 0.05f` (currently likely `0.1f`) to prevent z-fighting.
- The placeholder box view-model is rendered _in world-space_ and may occlude world geometry
  behind the near plane. Render it in a separate pass with `glClear(GL_DEPTH_BUFFER_BIT)` so
  it always draws on top.
- The `ItemState` enum is a small refactor — any existing `switch` on item null-checks must
  be updated to check `item.state() == WORLD` instead.

---

### Fix M — Remove Blind Light Expansion When Pulse Fires

**Problem:** `BlindEffectController.onSoundEvent()` calls `triggerSonarReveal()` when a
`CLAP_SHOUT` or `MIC_INPUT` event fires. `triggerSonarReveal()` adds a
`BlindSonarRevealModifier` that temporarily expands the player's fog visibility radius —
effectively giving the player a brief light around them. This contradicts the intended design:
the pulse is a **spatial scan**, not a **light source**. The player should remain in darkness;
only the geometry hit by the propagating wave should be outlined.

The sonar pulse already reveals the environment through two correct mechanisms:
1. The expanding particle sphere silhouettes walls visually (Fix D + Fix J).
2. `SonarRenderer.spawnFromPropagation()` illuminates acoustic graph nodes along the
   propagation path (the cyan dots on walls from Fix E).

The `BlindSonarRevealModifier` (which expands the player's own fog radius) is the third,
incorrect mechanism.

**File:** `BlindEffectController.java`

```java
// BEFORE:
public boolean onSoundEvent(SoundEventData soundEventData) {
    if (soundEventData == null) return false;
    SoundEvent event = soundEventData.eventType();
    if (event == SoundEvent.CLAP_SHOUT || event == SoundEvent.MIC_INPUT) {
        return triggerSonarReveal();   // ← REMOVE THIS
    }
    return false;
}

// AFTER:
public boolean onSoundEvent(SoundEventData soundEventData) {
    if (soundEventData == null) return false;
    // Sonar pulse no longer expands the player's own light radius.
    // Geometry reveal happens via particle sphere and graph node illumination only.
    return false;
}
```

`triggerSonarReveal()` itself can stay — it will be repurposed for the `FLARE` item
(`ItemType.FLARE`), which IS a physical light source and should expand the visibility radius
for its `duration_seconds: 10.0`. No other changes needed to that method.

**`blind_effect_config.json` — remove sonar reveal radius** (it is now unused by the blind
system, managed entirely by the particle preset from Fix J):
```json
"sonar_reveal": {
  "enabled": false,
  "clap_shout_radius_meters": 18.0,
  "duration_seconds": 0.7,
  "triggers_acoustic_visualization": true
}
```
Set `"enabled": false` so the modifier never fires even if `onSoundEvent` is called
accidentally during a refactor. The `triggers_acoustic_visualization: true` flag stays
and continues to drive `SonarRenderer` — that path is correct and unchanged.

**Resulting design contract (explicit):**

| Trigger | Expands fog radius? | Particle sphere? | Graph nodes lit? |
|---|---|---|---|
| Mic / clap pulse | ❌ No | ✅ Yes (Fix D/J) | ✅ Yes |
| Flare item used | ✅ Yes (10 s) | ❌ No | ❌ No |
| Panic state | ❌ Shrinks it | ❌ No | ❌ No |
| Enemy nearby | ❌ No effect | ❌ No | ❌ No |

**Expected Problems:**
- Removing the sonar blind reveal may initially make the game feel harder to playtest since
  developers relied on the light flash to see. Replace it with a diagnostic keybind:
  `Backspace` in test mode calls `triggerFlareReveal()` directly for testing visibility
  without using items.
- `UniversalTestScene` stores the `boolean sonarTriggered` return value of `onSoundEvent` to
  gate other logic. After this change it always returns `false`. Replace this gate with a
  direct check: `if (soundEvent.eventType() == SoundEvent.CLAP_SHOUT || MIC_INPUT)` where
  needed.

---

## 3. Phase 1 — TestScreen Completeness

**Goal:** The universal test screen is a fully usable development sandbox for all systems.
**Timeline estimate:** 2–3 weeks after Sprint 0 fixes.

### 1.1 — Sound Propagation Zone: Full Integration

**Objective:** The `SoundPropagationZone` should demonstrate the complete acoustic pillar:
mic input → graph propagation → visual bounce nodes → blind-effect reveal → 3D sphere pulse.

**Requirements:**
- All Sprint 0 fixes (A–I) complete.
- Acoustic graph nodes placed at wall intersections inside the zone (not just open space).
- `AcousticBounce3DVisualizer` shows bounce points on walls as bright cyan dots with a
  brief flash animation (0.3s fade-out).
- `PropagationResult` node intensities drive `BlindSonarRevealModifier` to temporarily reveal
  geometry near each node.
- A debug overlay tab (use existing `TabCycler` in `DiagnosticOverlay`) shows:
  - Active node count, last propagated intensity, mic RMS, stamina.

**Expected Problems:**
- `TestAcousticGraphFactory` currently places nodes in a fixed grid. When walls are added at
  height 3.5f (Fix C), nodes at `y=0` will be inside floors. Update the factory to place
  nodes at `y=0.9f` (mid-wall height).
- Graph edges that pass through new taller wall geometry will be invalid (they cross solid
  geometry). Implement a simple raycast validation pass in `TestAcousticGraphFactory`:
  for each proposed edge, fire a Bullet raycast; skip the edge if it hits a static body.

**Solutions:**
- Accept that edge validation is O(E·raycast). With ~80 nodes this is fine at setup time.
- Cache the validated graph — don't re-validate every frame.

---

### 1.2 — Item Interaction Zone: Physics Ground Truth

**Objective:** Items rest on the floor, can be picked up (E key), snap to the player's carry
position, and make noise when dropped (firing an acoustic event).

**Requirements:**
- Fix B (floor collision) complete.
- Fix A (weightless carry) complete.
- Drop (release E) restores gravity to the rigid body and fires a `SoundEvent` with intensity
  proportional to drop height (use `ImpactListener` which is already implemented).
- `PlayerInteractionSystem` raycast correctly identifies `CarriableItem` Bullet bodies as
  interactable. Requires mapping `btCollisionObject → CarriableItem` via a `userValue` int
  stored in the body's `userValue` field.

**Expected Problems:**
- `ImpactListener` is a `ContactListener` that fires on every manifold. When a carried item
  is being held (velocity ≈ 0), micro-collisions with the floor plane still fire events.
  Guard: `if (impactVelocity.len() < 0.3f) return;` at the start of the callback.

---

### 1.3 — Crouch Alcove Zone: Full Playthrough

**Objective:** The alcove is a tight, low-ceilinged passage the player must crouch through.

**Requirements:**
- Fix I (ceiling collider) complete.
- Player forced to crouch-walk at `1.8 m/s` inside.
- Camera height smoothly interpolates from `1.6f` (stand) to `0.9f` (crouch) using the
  existing head-bob lerp infrastructure in `PlayerController`.
- Zone exit re-enables standing automatically.
- A small HUD tooltip renders: `"Crouch [CTRL]"` when the player is near the entrance
  (within 2m) and standing.

**Expected Problems:**
- Camera height interpolation in `PlayerController` may need a new `crouchLerpSpeed` constant.
  Start with `5.0f` — too fast feels teleporty, too slow feels sluggish.

---

### 1.4 — Diagnostic Overlay: Mic + Stamina Tabs

**Status:** ✅ Implemented (Apr 21, 2026)

**Objective:** Tab 3 of `DiagnosticOverlay` shows live mic waveform RMS and stamina value.

**Requirements:**
- Ring-buffer storing last 128 RMS samples, rendered as a simple line graph using
  `ShapeRenderer`.
- Stamina value displayed as text and bar.
- Mic device name shown (query via `javax.sound.sampled.Mixer`).

---

## 4. Phase 2 — Core Gameplay Loop

**Goal:** A player can navigate a dark environment, make sounds, and use the acoustic
sonar to understand space. Items are meaningful tools.
**Timeline estimate:** 4–6 weeks.

### 2.1 — CarriableItem as Acoustic Tool

**Objective:** Throwing or dropping an item creates a sound event that propagates through
the graph, revealing geometry in that area temporarily. This builds on the complete item
system from Sprint 0 Fix L.

**Requirements:**
- Fix L (throw, break VFX, interaction prompt, item state) complete.
- Fix M (no light on pulse) complete — impact pulses must also not expand the fog radius.
- Impact sound event spawns the sonar sphere (Fix D, Fix J) at the _impact position_, not
  the player position.
- `ImpactListener` fires on wall/floor contact, creates `SoundEvent` with intensity based on
  impact velocity magnitude, scaled by `item.definition().noiseMultiplier()`.
- `SoundPropagationOrchestrator` processes the event through Dijkstra, returns
  `PropagationResult` to `SonarRenderer` for node illumination (not `BlindEffectController`).
- `GLASS_BOTTLE` (noiseMultiplier `1.2f`, breakThreshold `60f`) should shatter loudly and
  trigger a large pulse. `CARDBOARD_BOX` (multiplier `0.3f`) barely registers.

**Expected Problems:**
- Multiple impact events in rapid succession (item bouncing) cause repeated pulses.
  Implement a per-item impact cooldown: `0.4f` seconds between registered events.
- Throws at high velocity may tunnel through thin walls (Bullet CCD disabled by default).
  Enable CCD on thrown item bodies: `body.setCcdMotionThreshold(0.5f)`.

---

### 2.2 — Stamina as Tension Mechanic

**Objective:** Sprinting is powerful but audible and finite. It raises panic level.

**Requirements:**
- Stamina system from Fix G complete.
- Running generates louder footstep events (already in `PlayerFootstepSoundEmitter` via
  `MovementState`). Ensure `RUN` state uses `noiseMultiplier ~2.5f` vs `WALK ~1.0f`.
- At `stamina < 20%`, breathing audio cue starts (simple looping audio clip from AssetManager).
- At `stamina = 0`, `SimplePanicModel.panicLevel` increases briefly (already in codebase).
- Panic level feeds `BlindPanicModifier` — the blind effect gets worse when panicking.
  This creates the loop: sprint → run out of stamina → panic → can see less → must stop.

**Expected Problems:**
- Tuning panic ramp-up will require iteration. Keep all constants in `balancing_config.json`.
- Audio clip for breathing must be loaded lazily (not at startup) to keep `show()` fast.

---

### 2.3 — Full Inventory Integration

**Objective:** The 4-slot inventory holds items that can be used, combined, or dropped as
sound distractions.

**Requirements:**
- Press 1–4 to select active slot; active item shown in view-model position.
- `G` drops active item (spawns a new `btRigidBody` at the drop point, applies small forward
  impulse, fires impact event on landing).
- Consumable items (defined in `ItemDefinition.consumable=true`) are removed from inventory
  on use.
- `InventorySystem` already handles all of this — just wire up the key bindings in
  `UniversalTestScene.keyDown`.

**Expected Problems:**
- View-model rendering (the held-item mesh visible in the lower-right of screen) uses the
  existing `VIEW_MODEL_*` constants but requires a `ModelInstance` for the item mesh. If
  items do not have meshes yet, use a coloured box placeholder.

---

### 2.4 — Player Interaction System: Complete

**Status:** ✅ Implemented (Apr 22, 2026)

**Implemented now:**
- Bullet raycast targeting for carriable items via `btCollisionObject.userValue` lookup.
- On-screen interaction prompts for targeted carriable items and consumables.
- Held-item action prompt (`Throw`, `Drop`, `Stash`) in crosshair area.

**Notes:**
- Consumables still use proximity-based pickup because they do not currently own Bullet bodies.
- The scene keeps a stable registry slot for each carriable item so ray hits remain valid after removals.

**Objective:** The E-key interaction raycast works reliably for all interactable objects.

**Requirements:**
- `PlayerInteractionSystem.tryInteract()` fires a Bullet raycast from camera origin along
  camera direction, distance `PICKUP_RANGE=3.0f`.
- Hit result maps to a `CarriableItem` via `btCollisionObject.userValue` → item index.
- Interaction prompt (crosshair changes colour + "Pick up [E]" text) renders when
  a carriable item is within range and in front of the player.

**Expected Problems:**
- `userValue` is an integer; maintain a `List<CarriableItem> itemRegistry` in the scene and
  store the index. Ensure the list index is stable (no removals during iteration).

---

## 5. Phase 3 — Director AI & Enemy Systems

**Goal:** The game adapts its tension based on player behaviour, and there is something
hunting the player.
**Timeline estimate:** 5–8 weeks.

### 3.1 — Director AI: K-Means Cluster Classification

**Objective:** Classify the player's current behaviour into a tension tier and respond.

**Architecture (already partially built):**
```
PlayerFeatureExtractor  →  feature vector (speed, noise, proximity, panic)
       ↓
   KMeansClassifier    →  cluster label (CALM / TENSE / PANICKED)
       ↓
  DirectorController   →  adjust spawn rate, ambient sound, blind effect floor
```

**Requirements:**
- `KMeansClassifier` (new class): k=3 centroids, updated online with an EMA
  (exponential moving average) so centroids adapt to _this_ player's session.
- Initial centroids hardcoded from offline play-testing data.
- Director ticks every `2.0f` seconds (not every frame).
- Output actions:
  - `CALM` → reduce fog density, suppress ambient.
  - `TENSE` → increase fog, introduce ambient heartbeat at 90 BPM.
  - `PANICKED` → maximum fog, enemy becomes more active, footstep sound multiplier raised.

**Expected Problems:**
- K-Means can flip cluster assignments when the player transitions quickly. Add a
  1-second hysteresis before acting on a cluster change.
- Online centroid updates can drift. Clamp centroids to valid feature ranges defined in
  `balancing_config.json`.

**Implemented scaffold:**
- `DirectorController` now receives player feature samples, sound propagation events, and enemy hearing callbacks.
- Tier commits are ticked every 2.0 seconds with a 1.0 second hysteresis gate.
- `CALM` / `TENSE` / `PANICKED` expose fog, heartbeat, footstep, and aggression multipliers for later wiring.

---

### 3.2 — Enemy: Sound-Reactive Hunter

**Objective:** A single enemy navigates by sound events, hunts the player.

**Requirements:**
- Enemy has its own acoustic listener node in the graph.
- Any `SoundEvent` with intensity above enemy's `hearingThreshold` causes the enemy to
  pathfind toward the event origin.
- Enemy pathfinding uses the same `DijkstraPathfinder` (different cost function: favour
  proximity not intensity).
- Enemy cannot see the player; it only hears.

**Implemented scaffold:**
- `DirectorController` now retains the last heard node id and sound intensity once the hearing threshold is exceeded.
- The orchestrator already routes `EnemyHearingTarget` callbacks, so a later enemy shell can consume the same hook without reshaping the audio pipeline.
- Enemy emits its own low-frequency "breathing" sound event every `3s` while moving,
  which the player can hear (appears as a pulse on the sonar).

**Expected Problems:**
- Enemy and player sharing the Dijkstra graph simultaneously causes thread contention if
  propagation is ever moved to a background thread. Keep all graph queries on the main
  thread until threading is needed.
- Enemy pathfinding to a sound origin that has already decayed leads to the enemy
  "wandering" to now-empty areas. Implement a simple interest-point memory: enemy
  remembers the last 3 heard events and navigates to the most recent.

---

### 3.3 — Director Integration With Enemy

**Objective:** The Director modulates enemy behaviour based on tension tier.

| Tier | Enemy Speed | Hear Range | Decision Rate |
|---|---|---|---|
| CALM | 60% | 8m | 4s |
| TENSE | 80% | 12m | 2.5s |
| PANICKED | 100% | 18m | 1s |

All values configurable in `balancing_config.json`.

---

## 6. Phase 4 — Level Design & Procedural Generation

**Goal:** A real playable level with handcrafted key moments and procedurally-filled space.
**Timeline estimate:** 6–10 weeks.

### 4.1 — Level Architecture

**Structure:**
```
Hub Room → Puzzle Room A → Narrow Corridor → Puzzle Room B → Boss Encounter Zone
                  ↑ shortcut (locked until Puzzle A solved)
```

**Sound-design principle:** Every room's acoustic graph is pre-authored. Corridor graphs
are sparser; open rooms are denser. The propagation reveals this difference to the player.

### 4.2 — Procedural Obstacle Placement

Procedurally scatter crates, barrels, and debris inside rooms using a Poisson-disc sampler
(prevents overlap). Each obstacle gets a `CarriableItem` with a noise multiplier.

**Requirements:**
- `ProceduralRoomPopulator` (new class): takes a room AABB, a `Random` seed, and a
  `ItemDefinition[]` palette. Returns `List<PlacedItem>` with world positions.
- Items placed at `y = halfHeight + FLOOR_Y` — above the floor (no more falling through).
- No item placed within `1.5m` of the player spawn or within `0.5m` of a wall.

### 4.3 — Puzzle Design: Use Acoustics

Puzzles are built around the core mechanic:

- **Puzzle A — "Light the Room":** Player must throw items at targets in order. Each impact
  reveals a section of the room's acoustic graph for 3 seconds. Completing the sequence opens
  a door.
- **Puzzle B — "Silent Corridor":** Enemy patrols a corridor. Player must crouch-walk through
  without triggering footstep events above the enemy's hearing threshold.
- **Puzzle C — "Echo Mapping":** A room with no items. Player must clap/shout (mic) multiple
  times to triangulate a hidden button using the reflected sonar pulses.

### 4.4 — Procedural Acoustic Graph Population

**Requirements:**
- `GraphPopulator` places nodes at wall intersections and room centres automatically by
  raycasting outward from the room's AABB midpoints and recording hit positions.
- Edges are connected if the straight-line Bullet raycast between two nodes is unobstructed.
- Material tags from `AcousticMaterial` are assigned to wall faces based on `WallType`.

**Expected Problems:**
- A fully procedural graph may produce disconnected components (nodes on one side of a wall
  with no path to the other). Use a BFS connectivity check after building; if a node is
  disconnected, try to connect it through a door/gap node. If still disconnected, remove it.

---

## 7. Phase 5 — Horror Atmosphere & Polish

**Goal:** The game _feels_ like a horror game. Atmosphere, pacing, and audio design come
together.
**Timeline estimate:** 3–4 weeks.

### 5.1 — Audio Design

- Ambient soundscape: deep drone layered with subtle reversed-reverb tails (pre-authored .ogg).
- Spatial audio: use libGDX `Sound` + manual panning/volume based on distance from the
  `SoundSourceRegistry`. Not full HRTF, but close enough for mono headphones.
- Enemy proximity stinger: a sharp transient plays when the enemy comes within 5m.
- Breathing audio — pair with the `CameraBreathingController` (Fix K): when breathing
  amplitude is high (sprint or low stamina), play a wet exhale audio clip synced to the
  camera's `BREATHE_FREQ_HZ` oscillator. The visual and audio breathing phase-lock.
- Breathing audio (from Phase 2) spatially placed behind the player when enemy is near —
  creates "something is behind you" dread without needing line-of-sight.

### 5.2 — Visual Polish

- Deepen the VHS shader: increase `noise_amount` at high panic levels, add horizontal
  chromatic aberration. These are already parameterised in `BodyCamVHSSettings`.
- Add blood-pressure pulse vignette at low stamina — a `BlindPulseModifier` that rhythmically
  tightens the blind effect at the resting heart rate, getting faster as stamina decreases.
- Sonar pulse colour: transitions from `#00FFEE` (neutral) to `#FF4400` (near enemy) based
  on proximity of the enemy's acoustic node.

### 5.3 — Particle Atmosphere Polish

- Fire preset used for braziers / flickering lights in puzzle rooms. These are static sound
  sources (crackling fire = constant low-level sound event) that keep a small area revealed
  on the acoustic graph permanently.
- `smoke_puff` emitter attached to player camera at low opacity — simulates breathing condensation
  in the cold environment.
- Explosion preset used for destructible barrels when high-velocity items hit them, generating
  a massive one-time sound event that reveals the whole room briefly.

### 5.4 — Accessibility

- Add a `contrastMode` in config: replaces fog-black with dark purple/indigo, and sonar cyan
  with bright yellow. Helps players who struggle distinguishing the default very-dark palette.
- Optional: "Sonar sound" toggle — plays a soft ping audio cue when a bounce node is activated,
  making the game more playable without constant mic use.

---

## 8. Phase 6 — Playtesting, QA & Release Prep

**Timeline estimate:** 2–3 weeks.

### 6.1 — Internal Playtest Goals
- Can a new player navigate the first room using only acoustic sonar within 5 minutes?
- Does the enemy feel threatening without feeling unfair?
- Does stamina limit feel punishing or like smart resource management?

### 6.2 — Performance Targets
| Metric | Target |
|---|---|
| Frame time | < 16ms (60 FPS) on Intel integrated GPU |
| Dijkstra propagation latency | < 2ms per event on 100-node graph |
| Peak particle count | < 2000 instances |
| Physics substeps per frame | 2 at 60 FPS (Bullet default) |

### 6.3 — Known Performance Risks
- `ParticleEmitter` rebuilds VBO every frame for dynamic particles. Profile with RenderDoc;
  if the VBO upload dominates, switch to a persistent mapped buffer.
- `AcousticBounce3DVisualizer` now batches updates and caps bounce depth, but the ray layer still needs profiling if event density climbs.

### 6.4 — Build Pipeline
- `lwjgl3/build.gradle` already has a fat-jar task. Add a Gradle `package` task that:
  1. Runs unit tests.
  2. Builds the fat jar.
  3. Copies assets to a `dist/` folder.
  4. Zips into `Resonance-v{version}.zip`.
- Use semantic versioning: `Major.Minor.Patch`. Current: `0.1.0-alpha`.

---

## 9. Cross-Cutting Concerns

### Memory Management
libGDX + Bullet requires explicit disposal. Every `Disposable` (shaders, meshes, Bullet bodies,
particle managers) must be disposed in `hide()` or `dispose()`. Add a `DisposalRegistry` utility:

```java
public final class DisposalRegistry implements Disposable {
    private final Array<Disposable> items = new Array<>();
    public <T extends Disposable> T register(T d) { items.add(d); return d; }
    @Override public void dispose() { for (Disposable d : items) d.dispose(); }
}
```

Register every disposable through it in `UniversalTestScene.show()`. Call
`disposalRegistry.dispose()` in `UniversalTestScene.dispose()`.

### Threading
All libGDX OpenGL calls must happen on the render thread. `RealtimeMicSystem` already runs
audio capture on a background thread and pushes frames to a queue — this is correct. Do not
move Bullet or particle updates off the main thread without careful synchronisation.

### Coding Standards
The project has `CODING_STANDARDS.md`. Key rules that matter for ongoing work:
- No static mutable state (enforced by Checkstyle).
- All `public` methods must have Javadoc.
- Unit tests for every non-trivial algorithm (graph, pathfinding, K-Means).
- `final` on all local variables and fields where possible.

### Config-Driven Everything
Tunable constants must live in JSON configs, not as `private static final float` in code.
Use the existing `BalancingConfigStore` pattern for any new tunable. This allows balance
tweaks during playtesting without recompilation.

---

## 10. Suggestions

### 10.1 — Haptic Feedback (if targeting controller)
The `PlayerFootstepSoundEmitter` already knows step timing. Map footsteps to controller
rumble via `com.badlogic.gdx.controllers.Controller.startVibration()`. Walking: short, soft.
Running: sharper. Enemy proximity: continuous low rumble. This deepens immersion significantly
at near-zero implementation cost.

### 10.2 — Sound as Light: Coloured Sonar Tiers
With the LiDAR-range pulse now reaching `18–22 m`, colour-coding by intensity tier adds
spatial depth information — close walls glow brighter than distant ones:
- Low intensity (far from source): deep blue `#001155`
- Mid intensity: cyan `#00AACC`
- High intensity (close to source): bright white `#EEFFFF`
Implement by mapping `SonarRevealView.intensity` to an interpolated colour in the node-flash
rendering code in `GraphRenderLayer`.

### 10.3 — Acoustic Graph as Puzzle Hint
When the player is stuck, make the acoustic graph itself a map. A special "pulse flare" item
(single use, found in a drawer) fires a high-intensity event from the player that illuminates
the full graph for 10 seconds, fading out. Players can plan routes using this information.
This fits the lore ("experimental sound equipment") and teaches the mechanic elegantly.

### 10.4 — Consider Removing the Server Module
The `server` module currently contains only `ServerLauncher.java` with no implementation.
Unless a multiplayer or headless director mode is planned, remove it or mark it as
`// future: multiplayer` to reduce build times and confusion.

### 10.5 — Particle Preset: Breath Condensation
A very subtle, slow `mist` emitter attached to the camera (not the world) that emits 1–2
tiny white particles per second forward. At close range (< 0.5m) they are visible as wisps.
This is essentially free GPU-wise but adds enormous atmospheric presence.

### 10.6 — Blind Effect: "Echo of Light" Design Principle
The current `BlindEffectController` reveals geometry when sound propagates through a node.
Consider adding a secondary reveal mode: **proximity reveal** — within 0.5m of a wall, the
player can _feel_ it (haptic) and see a faint outline. This prevents soft-locking (player
in a corner with no items and no mic) and mirrors real human spatial awareness.

### 10.7 — Milestone Demo Scope
If a demo milestone is needed (e.g., for a presentation), the minimal vertical slice is:
- Hub room + Puzzle Room A + one corridor.
- Mic-triggered sonar working.
- One enemy that reacts to sound.
- Stamina and basic HUD.
This is achievable after Phase 2 + Phase 3.1–3.2 and before procedural generation.

---

*End of RESONANCE Master Development Plan*
*Maintain this document as a living reference — update phase status as work completes.*

# RESONANCE — Master Development Plan
> **Last Updated:** April 22, 2026 (rev 5) | **Engine:** libGDX + Bullet Physics | **Build System:** Gradle Multi-Module

---

## Progress Update (Apr 22, 2026 — Rev 5)

- Completed: `Fix K` camera idle breathing system (`CameraBreathingController`) integrated into `UniversalTestScene`.
- Completed: `Phase 1.4` diagnostic tab with 128-sample mic RMS history waveform and stamina bar.
- Completed: `Phase 2.4` interaction targeting uses Bullet raycast + stable `userValue` item registry.
- In progress: `Phase 3.1/3.2` director scaffold added (`DirectorController`) with k=3 EMA classification.
- In progress: `Phase 4` acoustic visual budget controls added (ray depth caps + throttled updates).
- Completed: mic fallback pulse keybind added (`V`) for test flow parity.
- Completed (Rev 5): `Fix N` — pulse visualization is no longer gated by propagation result and geometric rays can fire independently.
- Completed (Rev 5): `Fix O` — added `SoundPulseShaderRenderer` and GLSL pulse shell (`sound_pulse.vert/.frag`) in `UniversalTestScene`.
- Completed (Rev 5): `Fix P` — added transmission-edge synthesis in `GraphPopulator` with material inference and bounded transmission weight.
- Completed (Rev 5): `Fix Q` — hard-cutoff fog bounds (`fog_zone_width_fraction`) and shader-side full fog clamp beyond `u_blindFogEnd`.
- **New (Rev 4):** `UniversalTestScene` promoted to main test entry point.

---

## Table of Contents

1. [Project State Snapshot](#1-project-state-snapshot)
2. [Immediate Fixes — Sprint 0 (Do These Now)](#2-immediate-fixes--sprint-0-do-these-now)
   - Fix A–M (previous fixes, unchanged)
   - **Fix N — Pulse Working Only in Some Areas (Diagnosis + Fix)**
   - **Fix O — Custom Shader for Sound Pulse (Replace Particle Ring)**
   - **Fix P — Material-Based Sound Transmission Through Walls**
   - **Fix Q — Minecraft-Style Distance Fog (Hard Cutoff)**
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
| `AcousticMaterial` transmission coefficients | `AcousticMaterial.java` | Defined but not yet wired to pulse |

### What is Broken / Missing ❌
| Issue | Affected File(s) |
|---|---|
| Items fall through the floor | `UniversalTestScene` — rigid body / static floor collider missing |
| Carry system uses spring physics (items lag behind) | `CarrySystem` |
| No stamina — sprint is infinite | `PlayerController`, `UniversalTestScene` |
| No HUD (voice meter, stamina bar) | `UniversalTestScene` |
| RESOLVED (Rev 5): Sonar pulse now uses world-space GLSL shell renderer | `SoundPulseShaderRenderer`, `sound_pulse.vert/.frag` |
| Bounce nodes not visible on walls (only floor) | `AcousticBounce3DVisualizer`, `GeometricRayLayer` |
| Corridor walls are only 1.5 units tall (player eye-height 1.6) | `UniversalTestScene` wall `addBox` calls |
| Mic toggle does not reliably propagate a pulse | `UniversalTestScene.handleMicInput` |
| `CrouchAlcoveZone` has no actual low-ceiling physics geometry | `CrouchAlcoveZone` |
| RESOLVED (Rev 5): Fog now uses hard cutoff with narrow transition zone | `BlindEffectController`, `retro_shader.frag` |
| RESOLVED (Rev 5): Pulse visualization no longer depends on zone/propagation gating | `UniversalTestScene`, `AcousticBounce3DVisualizer` |
| RESOLVED (Rev 5): 2D pulse ring replaced with shader-based 3D shell | `SoundPulseShaderRenderer` |
| RESOLVED (Rev 5): Through-wall transmission edges now material-weighted | `GraphPopulator`, `GraphEdge`, `EdgeType` |
| Pulse reveal range too small (2.5 m) | `blind_effect_config.json`, `SonarRenderer` |
| Sonar pulse incorrectly triggers blind-effect light expansion | `BlindEffectController.onSoundEvent` |

---

## 2. Immediate Fixes — Sprint 0 (Do These Now)

> Fixes A–M are unchanged from rev 3. They are summarised here for reference then the new fixes follow.

**Fix A** — Weightless Item Carry | **Fix B** — Items Falling Through Ground | **Fix C** — Wall Height | **Fix D** — Sonar Pulse 3D Sphere | **Fix E** — Bounce Points on Walls | **Fix F** — Mic Working in Test Screen | **Fix G** — Proper HUD | **Fix H** — Atmospheric Fog / Mist | **Fix I** — Crouch Alcove Physics | **Fix J** — LiDAR Long-Range Pulse | **Fix K** ✅ — Camera Breathing | **Fix L** — Item System Polish | **Fix M** — Remove Blind Light Expansion on Pulse

*(Full implementation details for A–M are in rev 3 of this document.)*

---

### Fix N — Pulse Working Only in Some Areas (Root Cause Diagnosis + Fix)

**Severity:** Critical — breaks core mechanic readability.

#### Root Cause Analysis

The pulse system has **three independent visual paths** that each have their own failure modes:

**Path 1 — `SoundPulseVisualizer` (the 2D ring)**

`SoundPulseVisualizer` is constructed with `roomHalfWidth` and `roomHalfDepth`, which represent the outer boundary of a **single rectangular room**. `computeWallHit` only performs AABB intersection against these two scalars — it knows nothing about internal walls, corridors, or partial obstacles.

```
Failure condition A: Player is outside SoundPropagationZone
  → The zone gates pulse activation. No zone = no visualizer.activate() call.
  → Pulse appears to fire (sound event runs) but nothing is drawn.

Failure condition B: Player is in a narrow corridor
  → roomHalfWidth / roomHalfDepth don't match the corridor's local bounds.
  → Rays overshoot the corridor walls and "hit" the far outer boundary of a zone
    the player isn't even inside, making bounce points appear in wrong positions.

Failure condition C: Player near zone boundary
  → The sourcePosition offset inside computeWallHit places the ray origin outside
    the zone AABB. tMin = ∞ → fallback to 0 → zero-length arc → invisible.
```

**Path 2 — `GeometricRayLayer.fireRays` (the wall-hit bounce dots)**

`GeometricRayLayer` uses real `BoundingBox` colliders from the scene and `fibonacciDirection` for 3D sphere rays. This path is architecturally correct but:

```
Failure condition D: config.renderRays = false in AcousticBounceConfig
  → Layer silently skips everything.

Failure condition E: AcousticBounce3DVisualizer not receiving pulse event
  → UniversalTestScene calls visualizer.onSoundEvent() only when the orchestrator
    returns a PropagationResult with at least one node. If the player is not
    near a graph node, the orchestrator returns an empty result → visualizer
    never fires → no bounce dots anywhere.
```

**Path 3 — `SonarRenderer` (acoustic graph node illumination)**

```
Failure condition F: Player not within any graph node's radius
  → SonarRenderer.spawnFromPropagation() receives an empty node set.
  → Nothing illuminates. Pulse "worked" acoustically but is invisible.
```

#### Fix — Three-part solution

**N.1 — Decouple `SoundPulseVisualizer` from room-specific bounds**

Replace the room-bound constructor with Bullet raycast for wall detection. `SoundPulseVisualizer` should accept a `btDynamicsWorld` reference and use Bullet's `rayTest` to find the actual nearest wall in each arc direction.

```java
// BEFORE (constructor):
public SoundPulseVisualizer(float roomHalfWidth, float roomHalfDepth) { ... }

// AFTER — world-aware:
public SoundPulseVisualizer(btDynamicsWorld physicsWorld) {
    this.physicsWorld = physicsWorld;
}

// In computeWallHit — replace the AABB math with:
private Vector3 computeWallHit(Vector3 sourcePosition, Vector3 direction) {
    Vector3 rayEnd = new Vector3(sourcePosition).mulAdd(direction, MAX_RAY_DISTANCE);
    AllHitsRayResultCallback callback = new AllHitsRayResultCallback(sourcePosition, rayEnd);
    physicsWorld.rayTest(sourcePosition, rayEnd, callback);
    if (callback.hasHit()) {
        // Find the nearest static hit (ignore player body via collision mask)
        float minFraction = Float.MAX_VALUE;
        Vector3 nearest = new Vector3(rayEnd);
        for (int i = 0; i < callback.getCollisionObjects().size(); i++) {
            float fraction = callback.getHitFractions().get(i);
            if (fraction < minFraction) {
                minFraction = fraction;
                nearest = new Vector3(sourcePosition).mulAdd(direction,
                    minFraction * MAX_RAY_DISTANCE);
            }
        }
        callback.dispose();
        return nearest;
    }
    callback.dispose();
    return rayEnd; // open space — ray travels to max distance
}
```

This makes the pulse work **everywhere** — corridors, open rooms, alcoves — because it reads actual physics geometry, not a hardcoded room box.

**N.2 — Ensure pulse fires zone-independently from `UniversalTestScene`**

`triggerSoundPulse` should always reach both the visualizer and the orchestrator regardless of which zone the player is in:

```java
// In UniversalTestScene.triggerSoundPulse():
public void triggerSoundPulse(Vector3 position, float strength) {
    // 1. Always fire the visual pulse — no zone check
    soundPulseVisualizer.activate(position, strength);     // world-aware now (Fix N.1)
    geometricRayLayer.fireRays(position, strength);        // always fires

    // 2. Acoustic graph propagation — finds nearest node globally
    //    (not gated to SoundPropagationZone)
    Integer sourceNodeId = acousticGraphEngine.findNearestNodeId(position, 8.0f);
    if (sourceNodeId != null) {
        SoundEventData event = new SoundEventData(SoundEvent.CLAP_SHOUT, position, strength);
        PropagationResult result = soundOrchestrator.propagate(sourceNodeId, event);
        sonarRenderer.spawnFromPropagation(result);
        acousticBounceVisualizer.onSoundEvent(event, result);
    }
    // Even if no graph node found, the visual sphere still shows (N.1 handles it)
}
```

**N.3 — Guarantee `GeometricRayLayer` is always enabled in test config**

In `acoustic_bounce_config.json`, set `renderRays: true` as the default. Add a diagnostic key `B` that toggles it at runtime:

```java
// In UniversalTestScene.keyDown:
if (keycode == Input.Keys.B) {
    acousticBounceConfig.geometricLayer.renderRays = !acousticBounceConfig.geometricLayer.renderRays;
    geometricRayLayer.setConfig(acousticBounceConfig.geometricLayer);
}
```

**Expected Problems:**
- `AllHitsRayResultCallback` allocates on the heap each call. Pool these callbacks (12 arcs × pulse fires ≤ once/second = acceptable). If profiling shows GC pressure, use a single pre-allocated callback and reset between arcs.
- Static floor plane (`btStaticPlaneShape`) will always be hit first for downward-angled rays. Filter by `collisionFlags` — skip any body with `CF_STATIC_OBJECT` at `y < 0.1f` for the floor (walls are vertical, so this filter is safe for horizontal arcs).

---

### Fix O — Custom GLSL Shader for Sound Pulse (Replace Particle Ring)

**Problem:** The current sonar pulse is either a 2D `SoundPulseVisualizer` line ring or a `sonar_pulse.json` particle sphere. Both are expensive to tune and look disconnected from the world geometry. A full-screen (or world-space) GLSL shader gives exact control: a thin, sharp wave sphere with a hard edge on the near side and a soft alpha falloff on the far side.

**Design:** A world-space expanding sphere shell rendered as a mesh overlay. The shader receives the pulse origin, current radius, and elapsed time and computes per-fragment distance from the sphere surface.

**New file:** `assets/shaders/sound_pulse.vert` + `assets/shaders/sound_pulse.frag`

```glsl
// sound_pulse.vert
attribute vec3 a_position;
uniform mat4 u_projTrans;       // combined camera matrix
uniform mat4 u_worldTransform;  // model-to-world (sphere mesh centred at origin)
varying vec3 v_worldPos;

void main() {
    vec4 world = u_worldTransform * vec4(a_position, 1.0);
    v_worldPos = world.xyz;
    gl_Position = u_projTrans * world;
}
```

```glsl
// sound_pulse.frag
#ifdef GL_ES
precision mediump float;
#endif

uniform vec3  u_pulseOrigin;      // world-space centre of pulse
uniform float u_pulseRadius;      // current radius in metres
uniform float u_pulseThickness;   // wave shell thickness (e.g. 0.35)
uniform float u_pulseAlpha;       // 0..1 overall alpha (fades over lifetime)
uniform vec4  u_pulseColor;       // base colour (cyan: 0.2, 0.9, 1.0, 1.0)
uniform float u_time;             // elapsed seconds for shimmer

varying vec3 v_worldPos;

void main() {
    float dist = length(v_worldPos - u_pulseOrigin);
    float diff = abs(dist - u_pulseRadius);

    // Sharp inner edge, soft outer falloff
    float shell = 1.0 - smoothstep(0.0, u_pulseThickness, diff);

    // Shimmer: subtle noise-like flicker along the sphere surface
    float shimmer = 0.85 + 0.15 * sin(dist * 14.0 + u_time * 8.0);

    float alpha = shell * u_pulseAlpha * shimmer;
    if (alpha < 0.01) discard;

    gl_FragColor = vec4(u_pulseColor.rgb * shimmer, alpha);
}
```

**Java integration — new class `SoundPulseShaderRenderer.java`:**

```java
public final class SoundPulseShaderRenderer implements Disposable {
    private static final float PULSE_SPEED         = 12.0f;  // metres/sec
    private static final float PULSE_MAX_RADIUS    = 22.0f;
    private static final float PULSE_LIFETIME      = PULSE_MAX_RADIUS / PULSE_SPEED;
    private static final float PULSE_THICKNESS     = 0.35f;
    private static final Color PULSE_COLOR         = new Color(0.2f, 0.9f, 1.0f, 1.0f);

    private ShaderProgram shader;
    private Mesh sphereMesh;    // icosphere, ~320 triangles, radius=1.0
    private final List<PulseInstance> activePulses = new ArrayList<>();

    public void init() {
        shader = ShaderLoader.load("shaders/sound_pulse.vert", "shaders/sound_pulse.frag");
        sphereMesh = IcosphereFactory.create(1.0f, 3); // subdivision=3 → smooth enough
    }

    public void fire(Vector3 worldOrigin) {
        activePulses.add(new PulseInstance(new Vector3(worldOrigin)));
    }

    public void render(Camera camera, float deltaSeconds) {
        if (activePulses.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);  // ADDITIVE blend
        Gdx.gl.glDepthMask(false);

        shader.bind();
        shader.setUniformMatrix("u_projTrans", camera.combined);

        for (int i = activePulses.size() - 1; i >= 0; i--) {
            PulseInstance p = activePulses.get(i);
            p.elapsed += deltaSeconds;
            if (p.elapsed >= PULSE_LIFETIME) { activePulses.remove(i); continue; }

            float radius   = p.elapsed * PULSE_SPEED;
            float lifeNorm = p.elapsed / PULSE_LIFETIME;
            float alpha    = (1.0f - lifeNorm) * (1.0f - lifeNorm); // quadratic fade

            // Scale the unit sphere to the current pulse radius
            Matrix4 worldTrans = new Matrix4().setToScaling(radius, radius, radius)
                                              .setTranslation(p.origin);

            shader.setUniformMatrix("u_worldTransform", worldTrans);
            shader.setUniform3fv("u_pulseOrigin",
                new float[]{p.origin.x, p.origin.y, p.origin.z}, 0, 3);
            shader.setUniformf("u_pulseRadius",    radius);
            shader.setUniformf("u_pulseThickness", PULSE_THICKNESS);
            shader.setUniformf("u_pulseAlpha",     alpha);
            shader.setUniform4fv("u_pulseColor",
                new float[]{PULSE_COLOR.r, PULSE_COLOR.g, PULSE_COLOR.b, 1f}, 0, 4);
            shader.setUniformf("u_time", p.elapsed);

            sphereMesh.render(shader, GL20.GL_TRIANGLES);
        }

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override public void dispose() {
        shader.dispose();
        sphereMesh.dispose();
    }

    private static final class PulseInstance {
        final Vector3 origin;
        float elapsed;
        PulseInstance(Vector3 origin) { this.origin = origin; }
    }
}
```

**Remove `SoundPulseVisualizer` ring rendering from `UniversalTestScene`** — replace every call to `soundPulseVisualizer.activate()` with `soundPulseShaderRenderer.fire()`. The `SoundPulseVisualizer` class can stay for the `SignalArc` / `ReflectionRay` data model (it's still used by bounce hit-point computation), but its visual render path is retired.

**In `UniversalTestScene.render()` — call order:**
```java
renderWorld();                          // opaque geometry
soundPulseShaderRenderer.render(camera, delta); // additive pulse sphere on top
geometricRayLayer.render(shapeRenderer, camera); // bounce dots
renderHud();                            // HUD last
```

**Expected Problems:**
- Icosphere mesh generation at startup adds ~2ms. Pre-bake the mesh and cache in assets if cold start time matters.
- `GL_BLEND + GL_ONE` (additive) makes the pulse visible through walls. This is **intentional** — sound passes through geometry visually, which is correct for a wave effect. If wall-clipping is undesired, add a depth test pass: render the sphere twice — once with `GL_LEQUAL` depth test for the visible hemisphere, once with `GL_GREATER` at reduced alpha for the occluded hemisphere.
- On low-end devices (Intel integrated graphics), the icosphere draw at 22m radius covers a large screen area. Add a fragment `discard` early-out for `alpha < 0.01` (already in the shader above). Also cap `activePulses.size()` to 3 — any pulse beyond that is dropped since mic cooldown is 1.2s.

---

### Fix P — Material-Based Sound Transmission Through Walls

**Problem:** `AcousticMaterial` already defines `transmissionCoefficient` per material:

| Material | Transmission | Meaning |
|---|---|---|
| CONCRETE | 0.05 | 5% of sound passes through |
| METAL | 0.12 | 12% passes through |
| WOOD | 0.22 | 22% passes through |
| GLASS | 0.35 | 35% passes through |
| FABRIC | 0.70 | Soft absorption — most passes, little reflects |
| VENT_DUCT | 0.65 | Ducts channel sound through walls |

These coefficients exist but are **not yet wired into the visual pulse or the graph propagation**. Currently the pulse reflects at 100% (`BOUNCE_FADE = 0.72f` uniform) and the Dijkstra graph only uses `traversalMultiplier` for edge cost, not transmission.

**This fix has two layers:**

#### P.1 — Pulse Shader: Transmitted Wave Through Walls

When the shader's expanding sphere crosses a wall surface, the portion of the wave that passes through should continue at reduced intensity. This is implemented by tracking the pulse as two co-expanding spheres:

1. **Primary sphere** — full intensity, stopped at wall.
2. **Transmitted sphere** — spawned at the wall surface, moving at the same speed but scaled by `transmissionCoefficient`.

In practice, for real-time rendering we approximate this by tagging wall faces with their `AcousticMaterial` and spawning a secondary `PulseInstance` with reduced `startAlpha` when the primary pulse radius first reaches a physics body.

```java
// In SoundPulseShaderRenderer.render(), after computing radius:
// Check if any physics body intersects the expanding sphere surface this frame
for (PulseInstance p : activePulses) {
    if (!p.hasSpawnedTransmissions) {
        float radius = p.elapsed * PULSE_SPEED;
        // Sphere-cast: find all bodies at distance ≈ radius from p.origin
        List<WallHit> hits = physicsWorld.sphereContactTest(p.origin, radius, radius + 0.5f);
        for (WallHit hit : hits) {
            AcousticMaterial mat = materialRegistry.getMaterial(hit.body);
            if (mat != null && mat.transmissionCoefficient() > 0.01f) {
                // Spawn transmitted pulse — starts at wall, slightly behind primary
                PulseInstance transmitted = new PulseInstance(p.origin);
                transmitted.elapsed      = p.elapsed;  // same timing
                transmitted.alphaScale   = mat.transmissionCoefficient();
                transmitted.colorScale   = new Color(0.6f, 0.7f, 1.0f, 1f); // cooler colour — transmitted wave
                activePulses.add(transmitted);
            }
        }
        p.hasSpawnedTransmissions = true; // only check once per pulse
    }
}
```

Add `alphaScale` and `colorScale` to `PulseInstance` and pass them to the shader as additional uniforms `u_alphaScale` and `u_colorScale`.

#### P.2 — Acoustic Graph: Transmission Edges (Cross-Wall Propagation)

In `TestAcousticGraphFactory`, when building edges between nodes that are separated by a wall, currently those edges are either not added (if the raycast hits geometry) or added at full weight (if the raycast is clear). Add a third case: **transmission edge** — the edge exists but carries an additional attenuation factor equal to `1.0 / material.transmissionCoefficient()`.

```java
// In TestAcousticGraphFactory — edge validation loop:
RaycastResult result = bulletWorld.rayTest(nodeA.position, nodeB.position);
if (!result.hasHit()) {
    // Clear path — normal edge
    graph.addEdge(nodeA.id, nodeB.id, normalWeight);
} else {
    // Wall in the way — check material
    AcousticMaterial mat = materialRegistry.getMaterial(result.hitBody);
    if (mat != null && mat.transmissionCoefficient() > 0.05f) {
        // Transmission edge — sound passes through but is attenuated
        float transmissionPenalty = 1.0f / mat.transmissionCoefficient();
        float transmittedWeight = normalWeight * transmissionPenalty;
        // Only add if within reasonable range (don't transmit through 20m of concrete)
        if (transmittedWeight < MAX_TRANSMISSION_WEIGHT) {
            graph.addEdge(nodeA.id, nodeB.id, transmittedWeight,
                EdgeType.TRANSMISSION, mat);
        }
    }
    // else: no edge (wall is effectively opaque — CONCRETE at 0.05 → penalty=20x, filtered out)
}
```

In `DijkstraPathfinder`, `TRANSMISSION` edges are traversable — Dijkstra simply follows the cost. The higher cost means the propagation result will show lower intensity at nodes reached only through walls, which is physically correct.

**Config values — add to `acoustic_bounce_config.json`:**
```json
{
  "transmission": {
    "enabled": true,
    "maxTransmissionWeight": 25.0,
    "showTransmittedPulse": true,
    "transmittedPulseColorR": 0.55,
    "transmittedPulseColorG": 0.7,
    "transmittedPulseColorB": 1.0
  }
}
```

**Game design intent — explicit contract:**

| Material | % of wave passes | Visual result |
|---|---|---|
| CONCRETE | 5% | Faint ghost pulse barely visible through thick wall |
| WOOD | 22% | Clearly visible but dim transmitted wave |
| GLASS | 35% | Obvious transmitted wave — player can hear through glass |
| VENT_DUCT | 65% | Near-full transmission — sound travels the vents |
| FABRIC | 70% | Curtains/drapes transmit most sound, reflect little |

**Expected Problems:**
- `sphereContactTest` at the pulse surface is not a standard Bullet API call. Implement via `btGhostObject` + `btSphereShape` at the pulse origin — resize each frame. Ghost objects don't need broadphase collision response and are fast. Alternatively, cast a series of ray tests along the 12 arc directions (from Fix N) and check if any arc has crossed a wall face this frame.
- Spawning a new `PulseInstance` for each transmitted wall hit could cause exponential pulse spawning in rooms with many thin walls. Cap: max 3 transmitted pulses per primary pulse. The cap is enforced with a `transmissionCount` field on `PulseInstance`.
- Transmission edges in the graph make Dijkstra's search slightly larger (more traversable edges). Profile: with 80-120 nodes and average degree 6–8, transmission edges add maybe 20% more edges. Still well within the 2ms budget.

---

### Fix Q — Minecraft-Style Distance Fog (Hard Cutoff)

**Problem:** The current `BlindFogUniformUpdater` sends `u_fogStart = visibilityMeters` and `u_fogEnd = visibilityMeters * (1 + softness)` to the world shader. With `visibilityMeters = 3.5` and `softness = 0.5`, this means:

- Clear up to 3.5m
- Fog transition: 3.5m → 5.25m (1.75m wide fade zone)

This wide transition means at medium distance everything has a slight grey tint rather than there being a crisp fog wall. It makes the environment look **uniformly dark** rather than **dark beyond a visible boundary**. The player can't tell where the fog is — they just feel like their monitor brightness is low.

**Minecraft fog formula:** Fog starts at a high percentage of view distance (e.g. 80–90%) and reaches full opacity at 100% of view distance. The transition is narrow, creating the illusion of a solid fog wall.

**Fix — change `updateFogBounds()` in `BlindEffectController`:**

```java
// BEFORE:
private void updateFogBounds() {
    fogStartMeters = Math.max(0.05f, visibilityMeters);
    fogEndMeters = fogStartMeters + Math.max(0.1f, visibilityMeters * (1.0f + config.baselineFadeEdgeSoftness));
}

// AFTER — Minecraft style:
private void updateFogBounds() {
    // fogEnd is the hard cutoff — nothing visible beyond this
    fogEndMeters   = Math.max(0.5f, visibilityMeters);
    // fogStart begins close to the cutoff — narrow transition zone
    float fogZoneWidth = fogEndMeters * config.fogZoneWidthFraction; // default: 0.15
    fogStartMeters = Math.max(0.05f, fogEndMeters - fogZoneWidth);
}
```

Add `fogZoneWidthFraction` to `BlindEffectRevealConfig` with default `0.15` (15% of view distance is the fog transition zone). This gives:

| visibilityMeters | fogStart | fogEnd | Transition width |
|---|---|---|---|
| 3.5 m | 2.975 m | 3.5 m | 0.525 m — sharp wall |
| 8.0 m | 6.8 m | 8.0 m | 1.2 m — readable boundary |
| 18.0 m (post-pulse) | 15.3 m | 18.0 m | 2.7 m — horizon effect |

**World shader — verify the fog equation clamps correctly:**

```glsl
// In worldShader.frag — ensure this is the fog formula:
float fogFactor = clamp((dist - u_fogStart) / (u_fogEnd - u_fogStart), 0.0, 1.0);
gl_FragColor = mix(surfaceColor, u_fogColor, fogFactor);
```

If `fogFactor = 1.0` the fragment is **exactly** `u_fogColor` — opaque fog. Objects beyond `u_fogEnd` should be fully occluded. Check that `u_fogColor.a = 1.0` (not premultiplied) in the shader. If alpha is not 1.0, far objects show through the fog.

**Config update — `blind_effect_config.json`:**

```json
{
  "fogZoneWidthFraction": 0.15,
  "fogColor": [0.04, 0.04, 0.08, 1.0],
  "baselineVisibilityMeters": 3.5,
  "baselineFadeEdgeSoftness": 0.15
}
```

Set `baselineFadeEdgeSoftness` to `0.15` — it is now used only to control `fogZoneWidthFraction` if you want one field for both. Alternatively keep both separate.

**Layer 2 — `BlindFogUniformUpdater` fog colour must include a dark tint, not zero:**

```java
// Ensure fog colour is a deep, slightly-blue black — not pure black:
// Pure black (0,0,0) looks like a missing texture.
// Deep blue-black (0.04, 0.04, 0.08) reads as "heavy atmosphere".
shader.setUniform4fv("u_fogColor",
    new float[]{ fogColor.r, fogColor.g, fogColor.b, 1.0f }, 0, 4);
```

**Expected Problems:**
- When the pulse fires and `visibilityMeters` jumps from 3.5 → 18.0 then rapidly lerps back, the fog wall appears to pull back and then close in — this is the desired LiDAR flash effect. If the lerp feels too abrupt, slow the return rate: `lerpSpeed = 4.0f` for the expand phase, `lerpSpeed = 1.5f` for the contract phase. Track which direction `visibilityMeters` is moving and select the lerp speed accordingly.
- Mist particles (Fix H, Layer 2) that are spawned at the player's feet will now be occluded by fog beyond `fogEnd`. This looks correct — the mist appears to blend into the fog wall.

---

## 3. Phase 1 — TestScreen Completeness

> `UniversalTestScene` is now the **primary test entry point**. The main `ApplicationListener.create()` should load `UniversalTestScene` directly. All Sprint 0 fixes are demonstrated and testable here.

### 1.1 — Sound Propagation Zone: Full Integration

*(Unchanged from rev 3 — see acoustic graph node placement at y=0.9f, edge raycast validation.)*

Fix N now ensures the pulse fires even outside the `SoundPropagationZone`, so zone-specific gating is no longer needed for the visualizer path.

### 1.2 — Item Interaction Zone: Physics Ground Truth

*(Unchanged from rev 3.)*

### 1.3 — Crouch Alcove Zone

*(Unchanged from rev 3.)*

### 1.4 — Diagnostic Overlay ✅ (Implemented Apr 22)

New tab: **ACOUSTIC_DEBUG** showing:
- Current `fogStart` / `fogEnd` meters (live)
- Active pulse count from `SoundPulseShaderRenderer`
- Transmission edges active in last propagation
- Material under player's feet (raycast straight down → hit body → lookup material)

---

## 4. Phase 2 — Core Gameplay Loop

*(Unchanged from rev 3. See fix Fix P — items that break emit sound through walls at material-attenuated intensity.)*

### 2.1 — CarriableItem as Acoustic Tool (Updated)

Impact events now use `AcousticMaterial` transmission for the propagated wave. A `GLASS_BOTTLE` breaking next to a WOOD wall will cause a weaker sonar reveal in the adjacent room (22% transmission) — players in adjacent rooms hear a ghost of the impact.

---

## 5. Phase 3 — Director AI & Enemy Systems

*(Unchanged from rev 3.)*

---

## 6. Phase 4 — Level Design & Procedural Generation

*(Unchanged from rev 3. Fix P's transmission edges make the procedural acoustic graph more realistic — vents and glass walls become implicit "sound corridors".)*

---

## 7. Phase 5 — Horror Atmosphere & Polish

### 5.1 — Audio Design *(Unchanged from rev 3)*

### 5.2 — Visual Polish (Updated)

- **Pulse colour tier by transmission:** Primary pulse = cyan `#33EEFF`. Transmitted pulse = cool blue `#6688CC` at reduced alpha. The colour difference cues the player that sound leaked through a wall — not that it bounced.
- **Fog wall shimmer:** Add a subtle vertical sine displacement to `u_fogColor.a` in the shader — the fog "breathes" very slightly, reinforcing its atmospheric quality.

### 5.3 — Particle Atmosphere Polish *(Unchanged from rev 3)*

### 5.4 — Accessibility *(Unchanged from rev 3)*

---

## 8. Phase 6 — Playtesting, QA & Release Prep

*(Unchanged from rev 3.)*

---

## 9. Cross-Cutting Concerns

*(Unchanged from rev 3. Note: `SoundPulseShaderRenderer` and icosphere mesh must be registered in `DisposalRegistry`.)*

---

## 10. Suggestions

*(All previous suggestions from rev 3 retained.)*

### 10.8 — Transmission as Puzzle Mechanic

Glass walls are acoustically translucent (35%). If the player needs to make a sound event reach a far room, they can choose a path through a glass partition instead of a thick concrete wall. The acoustic graph makes this mechanically real — the shorter glass path through the wall has lower total weight than the longer corridor path around it. Players can discover this by reading the sonar reveal pattern.

### 10.9 — Fog Zone as Narrative Beat

Now that fog has a crisp visible edge (Fix Q), the Director AI can manipulate `visibilityMeters` as a narrative tool: before a scripted scare, briefly reduce visibility to 1.5m (fog closes in); immediately after, expand it to 8m (the room is revealed, tension releases). This two-beat structure is a classic horror pacing technique made mechanically concrete.

---

*End of RESONANCE Master Development Plan — Rev 4*
*Maintain this document as a living reference — update phase status as work completes.*

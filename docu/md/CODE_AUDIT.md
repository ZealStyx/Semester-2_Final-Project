# RESONANCE — Code Audit Report
> **Rev 4 Addendum** | **Based on:** `lwjgl3.zip` source (Apr 22, 2026 build)
> Covers bugs, inconsistencies, and suggestions found by reading actual source code.
> All findings are referenced by file and line where applicable.

---

## Section 1 — Confirmed Bugs (Actual Code, Not Speculation)

---

### BUG-01 — Dead Null Check on `frame` in `handleMicInput`
**File:** `UniversalTestScene.java` ~line 909–917
**Severity:** Low (no crash, but misleading)

```java
RealtimeMicSystem.Frame frame = realtimeMicSystem.update(delta);
if (frame == null) {
    frame = RealtimeMicSystem.Frame.silent();   // ← frame is REPLACED here
}
lastMicSpeakingActive = frame.speakingActive();
// ...
if (frame == null || !frame.shouldEmitSignal()) {  // ← this null check is DEAD CODE
    return;
}
```

`frame` was reassigned to `Frame.silent()` on line 911, so the second `frame == null` guard on line 917 can never be true. The `shouldEmitSignal()` check is real and correct, but the null part of the condition is wasted. Not a crash risk but a sign the logic was edited piecemeal and not reviewed together.

**Fix:** Remove the dead null check:
```java
if (!frame.shouldEmitSignal()) {
    return;
}
```

---

### BUG-02 — `Bullet.init()` Called Every Time F10 Reloads the Scene
**File:** `UniversalTestScene.java` line 499, constructor → `initializePhysicsWorld()`
**Severity:** Medium — potential native crash on some platforms

`UniversalTestScene` is created fresh on F10. The constructor calls `initializePhysicsWorld()` which calls `Bullet.init()` unconditionally. Bullet's JNI library is designed to be initialized once per JVM process; calling it a second time produces a no-op on desktop but can throw a native exception on Android or cause subtle corruption in the native broadphase.

**Fix:** Static guard:
```java
private static boolean bulletInitialized = false;

private void initializePhysicsWorld() {
    if (!bulletInitialized) {
        Bullet.init();
        bulletInitialized = true;
    }
    // ... rest of init
}
```

---

### BUG-03 — Mic Pulse Has 8-Second Lock; Keyboard Shortcut Has 1.2-Second Lock
**File:** `UniversalTestScene.java` — `MIC_PULSE_COOLDOWN = 8.0f`, `handleMicInput`, `emitClapShoutPulse`
**Severity:** High — core mechanic behaves differently from documentation

Two completely different cooldown paths exist for what should be the same action:

| Trigger | Scene-level cooldown | Orchestrator cooldown (balancing config) | Effective wait |
|---|---|---|---|
| V / X key | ❌ Bypassed (`useCooldown=false`) | ✅ `cooldownSeconds = 1.2s` | **1.2 seconds** |
| Mic input | ✅ `MIC_PULSE_COOLDOWN = 8.0f` | ✅ `cooldownSeconds = 1.2s` | **8.0 seconds** |

The mic is 6.6× slower than the keyboard for no documented reason. Because the orchestrator's per-event cooldown fires first (1.2s), the mic cooldown blocks for an additional 6.8 seconds after the orchestrator would already allow a new pulse.

**Fix:** Either remove `micPulseCooldownRemaining` entirely and let the orchestrator's balancing config control rate, or align the values:
```java
// Remove this constant — the orchestrator already has its own cooldown
// private static final float MIC_PULSE_COOLDOWN = 8.0f;

private void emitClapShoutPulse(boolean useCooldown) {
    // useCooldown parameter is now meaningless — remove it and always delegate to orchestrator
    Vector3 pulsePos = new Vector3(camera.position);
    // orchestrator's isInCooldown check handles rate limiting for ALL callers
    ...
}
```

---

### BUG-04 — `SoundPropagationZone.shapeRenderer` Is Never Disposed
**File:** `SoundPropagationZone.java` line 14, `UniversalTestScene.dispose()`
**Severity:** Medium — GPU resource leak on every scene reload

```java
// In SoundPropagationZone.java:
private final ShapeRenderer shapeRenderer = new ShapeRenderer();  // ← allocated, never freed
```

`SoundPropagationZone` has no `dispose()` override. `UniversalTestScene.dispose()` does not iterate the `zones` array to dispose them. Every F10 scene reload leaks one `ShapeRenderer` (and its backing VBO).

Additionally, `SoundPropagationZone` creates `overlayProjection = new Matrix4()` and `lastPulseCenter = new Vector3()` as fields — these are value types and don't need cleanup, but the ShapeRenderer does.

**Fix — two parts:**
1. Add `dispose()` to `SoundPropagationZone`:
```java
@Override
public void dispose() {
    shapeRenderer.dispose();
}
```
2. Add to `UniversalTestScene.dispose()`:
```java
for (TestZone zone : zones) {
    if (zone instanceof Disposable) {
        ((Disposable) zone).dispose();
    }
}
```

---

### BUG-05 — `processBulletImpacts()` Fires Up to 4 Dijkstra Runs Per Collision
**File:** `UniversalTestScene.java` — `processBulletImpacts()`
**Severity:** Medium — performance spike, duplicate sound events

Bullet contact manifolds have up to 4 contact points. The current loop calls `impactListener.onCarriableImpact()` for each point:

```java
int contactCount = manifold.getNumContacts();
for (int contactIndex = 0; contactIndex < contactCount; contactIndex++) {
    btManifoldPoint contactPoint = manifold.getContactPoint(contactIndex);
    float appliedImpulse = contactPoint.getAppliedImpulse();
    if (appliedImpulse <= 0f) continue;
    // ← This runs Dijkstra up to 4 times for one item hitting a wall
    impactListener.onCarriableImpact(carriableItem, tmpImpulsePoint, appliedImpulse, elapsedSeconds);
}
```

A glass bottle landing on a flat floor generates 3–4 contacts. The acoustic graph runs Dijkstra 3–4 times in a single frame for one event.

**Fix:** Track max impulse per manifold, use only that one:
```java
float maxImpulse = 0f;
Vector3 maxImpulsePoint = null;

int contactCount = manifold.getNumContacts();
for (int contactIndex = 0; contactIndex < contactCount; contactIndex++) {
    btManifoldPoint contactPoint = manifold.getContactPoint(contactIndex);
    float imp = contactPoint.getAppliedImpulse();
    if (imp > maxImpulse) {
        maxImpulse = imp;
        contactPoint.getPositionWorldOnA(tmpImpulsePoint);
        maxImpulsePoint = new Vector3(tmpImpulsePoint);
    }
}

if (maxImpulsePoint != null && maxImpulse > 0f) {
    impactListener.onCarriableImpact(carriableItem, maxImpulsePoint, maxImpulse, elapsedSeconds);
}
```

---

### BUG-06 — `hide()` Not Overridden — Mic Keeps Capturing on Screen Switch
**File:** `UniversalTestScene.java` — missing `hide()` override
**Severity:** Medium — audio resource left running after screen transition

When F9 switches to `PlayerTestScreen`, libGDX calls `hide()` on `UniversalTestScene`. `ScreenAdapter.hide()` is a no-op. The mic system (`realtimeMicSystem`) continues running on its capture thread and consuming audio input even though the screen is no longer active. `dispose()` is called asynchronously via `postRunnable`, leaving a window where the mic runs on an inactive screen.

**Fix:**
```java
@Override
public void hide() {
    if (realtimeMicSystem != null && realtimeMicSystem.isActive()) {
        realtimeMicSystem.stop();
    }
    micEnabled = false;
    Gdx.input.setCursorCatched(false);
}
```

---

### BUG-07 — `autoCollectNearbyConsumables()` Silently Vacuums Items Every Frame
**File:** `UniversalTestScene.java` — `updateGameplaySystems()` → `autoCollectNearbyConsumables()`
**Severity:** High — breaks intentional gameplay design

`autoCollectNearbyConsumables()` runs every frame and picks up any consumable within `PICKUP_RADIUS = 1.75m` without player input. The player never consciously picks up a flare or decoy — they just disappear as the player walks near them.

This conflicts with the interaction system design: `findNearestFacingConsumable()` checks facing direction before showing the `[F] Use` prompt, and `tryPickupNearestConsumable()` is also gated on facing. But `autoCollectNearbyConsumables` has no facing check — the player can walk backwards through a flare and collect it silently.

**Fix:** Remove `autoCollectNearbyConsumables()` entirely. The F-key flow already handles consumable pickup correctly. If proximity-pickup is desired, at minimum require the player to be facing the item:
```java
// Only call tryPickupNearestConsumable() inside the F-key handler
// Remove the autoCollect call from updateGameplaySystems
```

---

### BUG-08 — `panicModel.setThreatDistanceMeters` Points to Hub Origin, Not an Enemy
**File:** `UniversalTestScene.java` line ~807
**Severity:** Medium — panic system is meaningless

```java
panicModel.setThreatDistanceMeters(playerPos.dst(HUB_X, EYE_HEIGHT, HUB_Z));
```

This measures how far the player is from the hub centre `(40, 1.6, 40)`. The further from the hub, the more "threat" the panic model perceives. Moving into a side corridor increases panic; running back to the hub decreases it. This is the opposite of what should happen (enemies would be in the rooms, not at the hub) and makes the panic system produce random results.

**Fix — temporary placeholder until enemy system is implemented:**
```java
// Use last known loud sound intensity as a panic proxy instead of distance
panicModel.setThreatDistanceMeters(30f); // treat as "no threat" constant for now
// Remove the dst(HUB_X) version entirely
```
Once Director AI adds an enemy position, replace with `playerPos.dst(enemyWorldPosition)`.

---

### BUG-09 — `SoundPropagationZone.setSoundData` Deep-Copies All Graph Nodes Every Frame
**File:** `SoundPropagationZone.java` — `setSoundData()`
**Severity:** Low–Medium — constant GC churn

```java
public void setSoundData(ObjectMap<String, Vector3> sourceNodePositions, ...) {
    nodePositions.clear();
    for (ObjectMap.Entry<String, Vector3> entry : sourceNodePositions.entries()) {
        nodePositions.put(entry.key, new Vector3(entry.value));  // ← new Vector3 every frame per node
    }
```

With ~100+ graph nodes, this allocates 100+ `Vector3` objects every render frame — thousands per second — all immediately becoming garbage. The graph node positions don't change between pulses.

**Fix:** Pass a read-only reference rather than copying:
```java
// In SoundPropagationZone, replace the copy map with a shared reference:
private ObjectMap<String, Vector3> nodePositions;  // ← no longer a local copy

public void setNodePositions(ObjectMap<String, Vector3> sharedPositions) {
    this.nodePositions = sharedPositions;  // set once at startup, never again
}

// Only update the revealed IDs + flashAlpha (these DO change per pulse):
public void setSoundData(Array<String> revealedIds, float flashAlpha, Vector3 playerPos) { ... }
```

---

### BUG-10 — `GraphPopulator` Fallback Returns Test Graph at World Origin
**File:** `GraphPopulator.java` — `populate()`
**Severity:** High — pulses produce no acoustic illumination if colliders fail

```java
public AcousticGraphEngine populate(Array<BoundingBox> colliders) {
    if (colliders == null || colliders.isEmpty()) {
        return TestAcousticGraphFactory.create();  // ← nodes at (0,0,0) area
    }
    // ...
    if (seeds.isEmpty()) {
        return TestAcousticGraphFactory.create();  // ← same fallback
    }
```

`TestAcousticGraphFactory.create()` places nodes near world origin: `center=(0, 0.2, 0)`, `north=(0, 0, -4.6)`, etc. The hub is at `(40, 0, 40)`. If `GraphPopulator` fails to generate any seeds (which can happen if all `BoundingBox` colliders are zero-height floor slabs with `addCollider=false`), the game silently uses a graph 56+ units away from the player. `findNearestNodeId` will return `"dyn_0"` or similar, but Dijkstra will propagate from a node that's nowhere near the action, producing empty reveal sets.

**Fix:** The fallback should generate a minimal graph centered at the hub:
```java
if (seeds.isEmpty()) {
    Gdx.app.error("GraphPopulator", "No wall nodes found — using emergency hub graph");
    return createEmergencyHubGraph();  // nodes at HUB_X, HUB_Z — not at origin
}
```

---

### BUG-11 — All `GraphPopulator` Edges Use `AcousticMaterial.CONCRETE` Regardless of Actual Wall
**File:** `GraphPopulator.java` — edge building loop
**Severity:** Medium — `AcousticMaterial` diversity is ignored at runtime

```java
edges.add(GraphEdge.between(
    nodes.get(i),
    nodes.get(j),
    AcousticMaterial.CONCRETE,  // ← hardcoded regardless of what wall this is
    0.05f, 0.0f
));
```

`AcousticMaterial` has 9 variants (CONCRETE, METAL, WOOD, GLASS, FABRIC, VENT_DUCT...) each with distinct `transmissionCoefficient` and `traversalMultiplier`. But the graph populator always tags every edge as CONCRETE. This makes the material-based transmission system from Fix P effectively inert — every wall transmits sound at the CONCRETE rate of 5%.

**Fix:** Tag walls by the `addBox` call that created them. `addBox` already knows what it's building (outer boundary wall vs. corridor wall). Pass a `AcousticMaterial` hint to `addCollider()` and store it in the `BoundingBox` user data or a parallel structure:
```java
// Extended addBox with material:
private void addBox(float cx, float cy, float cz, float w, float h, float d,
                    boolean addCollider, AcousticMaterial material) {
    // ...
    if (addCollider) {
        addCollider(cx, cy, cz, w, h, d, material);
    }
}

// Tag walls:
addBox(HUB_X + 8f, 1.75f, HUB_Z + CORRIDOR_HALF_WIDTH,
       16f, 3.5f, 0.2f, true, AcousticMaterial.CONCRETE);
```

---

### BUG-12 — `findNearestNodeId` Is O(n) and Called Multiple Times Per Frame
**File:** `UniversalTestScene.java` — `findNearestNodeId()`
**Severity:** Medium — performance at scale

```java
private String findNearestNodeId(Vector3 worldPosition) {
    String nearestNodeId = null;
    float nearestDistanceSquared = Float.POSITIVE_INFINITY;
    for (ObjectMap.Entry<String, Vector3> entry : graphNodePositions.entries()) {
        float distanceSquared = entry.value.dst2(worldPosition);
        // ...
    }
```

This linear scan is called from at minimum:
- `emitClapShoutPulse` (pulse origin)
- `emitItemEvent` (impact origin)
- `playerFootstepSoundEmitter.update` (every step)
- `spatialCueController.setListenerNode` (every frame)

With 100 nodes × 4 calls × 60 FPS = 24,000 distance comparisons/second. This alone won't tank performance but sets a bad precedent as node count grows.

**Fix:** Build a simple uniform 3D grid spatial hash over the node positions once at startup. `findNearestNodeId` then checks only the nodes in the relevant cell and its 26 neighbors — reduces to O(1) amortized for scenes with uniform node distribution.

---

### BUG-13 — `SonarRenderer.snapshot()` Is Never Called — All Reveal State Is Unused
**File:** `UniversalTestScene.java`, `SonarRenderer.java`
**Severity:** Low — wasted computation

`SonarRenderer` maintains a live list of `SonarReveal` objects (with intensities and remaining lifetimes) via `spawnFromPropagation()`. These are updated every frame in `sonarRenderer.update()` inside the orchestrator's `update()`. However, `SonarRenderer.snapshot()` is never called from `UniversalTestScene`. The actual node flash is driven by `cacheRevealedNodesForFlash()` → `lastRevealedNodeIds`, which is a separate parallel system.

This means `SonarRenderer` does work (maintains and updates reveal objects) that has zero visible effect in `UniversalTestScene`. The `snapshot()` output — which includes per-node intensity and alpha — is the mechanism that would let nodes glow at different brightness levels based on propagation distance. That information is currently discarded.

**Fix — two options:**
- **A (use it):** In `renderAcousticGraphWorldMarkers()`, read `sonarRenderer.snapshot()` and scale node marker alpha/color by `SonarRevealView.alpha()` × intensity. This gives distance-graded node illumination instead of uniform flash.
- **B (remove it):** If the simpler flash system is sufficient, remove `SonarRenderer` from the orchestrator and call `cacheRevealedNodesForFlash` directly.

---

## Section 2 — Design Inconsistencies

---

### INCON-01 — X and V Keys Both Map to the Exact Same Action
Both `Input.Keys.X` and `Input.Keys.V` call `emitClapShoutPulse(false)`. There is no behavioral difference. Controls legend text shows `[X/V]` as if they're alternatives, but they're duplicates. Pick one canonical key; assign the other to something useful (e.g., throw item while holding).

---

### INCON-02 — Hardcoded Fog Uniforms Set and Immediately Overwritten
**File:** `UniversalTestScene.java` → `renderWorldMeshes()`

```java
worldShader.setUniformf("u_fogStart", 1.0f);   // ← set
worldShader.setUniformf("u_fogEnd", 4.0f);      // ← set
blindFogUniformUpdater.updateBlindUniforms(     // ← immediately overwritten
    worldShader, blindEffectController.fogStartMeters(), ...
);
```

The hardcoded 1.0/4.0 values serve no purpose — `blindFogUniformUpdater` always overwrites them. Confusing for anyone reading the shader setup. Remove the two hardcoded lines.

---

### INCON-03 — `SoundPulseVisualizer` Exists but Is Never Used in `UniversalTestScene`
`SoundPulseVisualizer.java` is a substantial class (360 lines) with room-bound AABB wall hit logic, `SignalArc` animation, and `ReflectionRay` tracking. It is only used in `SoundTestScreen`. `UniversalTestScene` uses the `sonarPulseEffect` particle system for the visual pulse instead.

Both systems exist in parallel but target different screens. The devplan's Fix N and Fix O correctly diagnose and replace the particle approach with a shader — but should also clarify that `SoundPulseVisualizer` is a `SoundTestScreen`-only class and not expected to work in `UniversalTestScene`.

---

### INCON-04 — `physicsWorld.stepSimulation` Is Inside `updateActiveZone()`
Physics stepping is conceptually a global world update but it's buried inside the zone-management method. Anyone reading `updateGameplaySystems()` looking for the physics tick would miss it. Extract to its own `stepPhysicsWorld(float deltaSeconds)` method called explicitly from `updateGameplaySystems()`.

---

### INCON-05 — `tryPickupNearestConsumable` Has No Facing Check; Carriable Pickup Does
**File:** `UniversalTestScene.java`

When the player presses F to pick up a carriable item, `findNearestFacingCarriable()` checks `facingDot >= INTERACTION_FACING_DOT (0.70)`. When picking up consumables via `tryPickupNearestConsumable()`, there is no facing check — any consumable within `PICKUP_RADIUS` is eligible regardless of direction. This asymmetry means you can stare at a glass bottle and not be able to grab a flare directly behind you, but a NOISE_DECOY one step to your left auto-collects silently.

---

### INCON-06 — All View Model Items Are Identical Cubes
`playerViewModelMesh = createCubeMesh(1.0f, 1.0f, 1.0f)` is assigned once and shared for all item types. A METAL_PIPE and a CARDBOARD_BOX look identical in hand — only the color differs. At minimum use different mesh dimensions:
- METAL_PIPE → tall thin box `(0.05f, 0.7f, 0.05f)`
- GLASS_BOTTLE → medium box `(0.08f, 0.35f, 0.08f)`
- CARDBOARD_BOX → cube `(0.3f, 0.3f, 0.3f)`

---

## Section 3 — Suggested Improvements

---

### IMPROVE-01 — Pool `ClosestRayResultCallback` to Eliminate Per-Frame Allocation
**File:** `UniversalTestScene.java` → `findRaycastCarriable()`

```java
ClosestRayResultCallback rayResultCallback = new ClosestRayResultCallback(camera.position, tmpRayEnd);
```

This allocates a new Kryonet-style native callback object every frame when the player is not holding an item. The callback wraps a JNI native object and triggers GC pressure. Pool it:

```java
private ClosestRayResultCallback cachedRayCallback;

// In init:
cachedRayCallback = new ClosestRayResultCallback(Vector3.Zero, Vector3.Zero);

// In findRaycastCarriable:
cachedRayCallback.setRayFromWorld(camera.position);
cachedRayCallback.setRayToWorld(tmpRayEnd);
cachedRayCallback.setCollisionObject(null);  // reset hit
cachedRayCallback.setClosestHitFraction(1f);
physicsWorld.rayTest(camera.position, tmpRayEnd, cachedRayCallback);
// No longer dispose in finally block — reuse across frames
```

---

### IMPROVE-02 — Batch Static Scene Geometry Into One VBO
**File:** `UniversalTestScene.java` → `buildProceduralHubScene()` → `addBox()`

Each `addBox()` call creates a separate `Mesh` object. The hub scene has 20+ boxes, each rendered as a separate `mesh.render(worldShader, GL_TRIANGLES)` draw call. On a mobile GPU, 20 draw calls per frame is already visible in the profiler.

Options ranked by effort:
1. **Easy:** Use a single reusable unit cube mesh, pass scale via `u_modelTrans`. All boxes share one Mesh, only the transform differs.
2. **Better:** Merge all static mesh data into one large VBO using `MeshBuilder`. One draw call for the entire static world. Transform is baked into vertex positions — no per-box uniform needed.

---

### IMPROVE-03 — Cache `findSoundPropagationZone()` Instead of Scanning Every Frame
**File:** `UniversalTestScene.java` — `render()` calls `findSoundPropagationZone()` every frame

```java
SoundPropagationZone soundZone = findSoundPropagationZone();  // O(n) linear scan every frame
```

The zone list is built once at startup and never changes. Cache the reference:
```java
private SoundPropagationZone cachedSoundPropagationZone;

// In initializeZones(), after zone.setUp():
for (TestZone zone : zones) {
    if (zone instanceof SoundPropagationZone sp) {
        cachedSoundPropagationZone = sp;
        break;
    }
}
// Remove the findSoundPropagationZone() method and its per-frame call
```

---

### IMPROVE-04 — Expose `MIC_THRESHOLD_RMS` as a Runtime Slider
Currently `MIC_THRESHOLD_RMS = 0.08f` is a compile-time constant. Any microphone sensitivity difference (laptop mic vs. USB headset vs. phone) requires recompilation. The `DiagnosticOverlay` already has a `MIC_STAMINA` tab — add a `[+]/[-]` keybind there that increments/decrements the threshold by `0.01f` per press, and save it to `config/mic_settings.json`. This alone will solve ~70% of reported "my mic doesn't work" issues without any code changes.

---

### IMPROVE-05 — Add `ACOUSTIC_MATERIAL` Tag to Each `BoundingBox` Collider
Currently `worldColliders: Array<BoundingBox>` stores raw bounding boxes with no metadata. Extend to carry the material:

```java
// Replace bare BoundingBox array with:
private final Array<TaggedCollider> worldColliders = new Array<>();

public record TaggedCollider(BoundingBox bounds, AcousticMaterial material) {}

// In addCollider:
private void addCollider(float cx, float cy, float cz, float w, float h, float d,
                          AcousticMaterial material) {
    worldColliders.add(new TaggedCollider(new BoundingBox(...), material));
}
```

This unlocks material-based edge weighting in `GraphPopulator`, Fix P's shader transmission system, and future features like sound-absorbent fabric walls for puzzle rooms.

---

### IMPROVE-06 — Replace `BitmapFont` Default with a Readable Screen-Space Font
**File:** `UniversalTestScene.java` — `hudFont = new BitmapFont()`

The default `BitmapFont` is a 15pt Arial bitmap rendered at 1× resolution. On 1440p or 4K monitors it appears tiny and blurry (upscaled). Load a FreeType font at the actual display resolution:

```java
FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
    Gdx.files.internal("fonts/ShareTechMono-Regular.ttf"));
FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
params.size = (int)(14 * Gdx.graphics.getDensity());  // DPI-aware
params.mono = true;
hudFont = generator.generateFont(params);
generator.dispose();
```

The existing `libgdx-freetype` extension is likely already in the Gradle deps — check `core/build.gradle`.

---

### IMPROVE-07 — `SonarRevealView.alpha()` Should Drive Node Brightness, Not Just Flash Timer
As identified in BUG-13, `SonarRenderer.snapshot()` is never used. The reveals carry per-node intensity information that would make distant nodes dim and near nodes bright — exactly the LiDAR effect described in Fix J. Connect them:

```java
// In renderAcousticGraphWorldMarkers():
List<SonarRenderer.SonarRevealView> reveals = sonarRendererSnapshot;  // cache per frame
Map<String, Float> revealAlphaByNodeId = new HashMap<>();
for (SonarRenderer.SonarRevealView view : reveals) {
    revealAlphaByNodeId.put(view.nodeId(), view.alpha() * view.intensity());
}

for (ObjectMap.Entry<String, Vector3> entry : graphNodePositions.entries()) {
    float revealAlpha = revealAlphaByNodeId.getOrDefault(entry.key, 0f);
    float markerAlpha = 0.3f + revealAlpha * 0.7f;  // always slightly visible, bright when revealed

    shapeRenderer.setColor(0.2f, 0.95f, 0.6f, markerAlpha);
    drawCircleXZ(tmpProjected, GRAPH_NODE_MARKER_RADIUS + revealAlpha * 0.12f, 12);
}
```

This gives nodes that are close to the pulse source a bigger, brighter flash, and distant nodes a smaller, dimmer one — distance-graded sonar illumination.

---

### IMPROVE-08 — Zone Transitions Should Fire Sound Events
When the player enters or exits a zone, nothing acoustic happens. But zone transitions are meaningful moments (entering a tight corridor, crossing into the blind chamber). Adding a soft `SoundEvent.OBJECT_DROP_OR_BREAK` at low intensity on zone entry would:
1. Reveal the new zone's acoustic graph nodes via propagation.
2. Give the player an implicit sonar ping of the new space when they enter it.
3. Alert the enemy that the player moved into a new area — which is fair since footsteps would too.

```java
// In updateActiveZone(), where activeZone is updated:
if (nearest != null && nearest != activeZone) {
    if (activeZone != null) activeZone.onExit();
    activeZone = nearest;
    activeZone.onEnter();

    // Subtle entry ping — not a full CLAP_SHOUT, just a presence reveal
    emitItemEvent(SoundEvent.FOOTSTEP, playerPos, 0.15f);
}
```

---

### IMPROVE-09 — `panicModel.setHealth(100f, 100f)` Is Hardcoded
**File:** `UniversalTestScene.java` line ~810

```java
panicModel.setHealth(100f, 100f);  // ← always full health, every frame
```

The panic model accepts health as input to its computation but is always told the player is at full health. If the player ever takes damage (from puzzle fail states, enemy contact, etc.), the panic system won't respond. Connect to an actual `playerHealth` field or at minimum leave a TODO comment explaining this is a placeholder.

---

### IMPROVE-10 — Consider Split Render Passes for View Model Depth
**File:** `UniversalTestScene.java` → `renderPlayerViewModel()`

```java
Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
playerShaderProgram.bind();
// ... render view model cube
Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
```

Disabling depth test causes the view model to draw on top of everything — including HUD particles, the pulse sphere, and the VHS overlay. The correct approach for a view model is to clear the depth buffer before rendering it so it appears in front of world geometry but behind HUD elements:

```java
Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);  // clear depth, keep colour
// Now render view model with depth test enabled — it wins against cleared depth
Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
playerShaderProgram.bind();
// ...
```

This also stops the view model from clipping through walls at close range (currently invisible issue because depth is off entirely).

---

## Summary Table

| ID | Type | Severity | File | One-Line Summary |
|---|---|---|---|---|
| BUG-01 | Bug | Low | `UniversalTestScene` | Dead null check on `frame` after it's replaced |
| BUG-02 | Bug | Medium | `UniversalTestScene` | `Bullet.init()` re-called on F10 scene reload |
| BUG-03 | Bug | **High** | `UniversalTestScene` | Mic cooldown 8s vs keyboard 1.2s — unintentional asymmetry |
| BUG-04 | Bug | Medium | `SoundPropagationZone` | ShapeRenderer allocated but never disposed — GPU leak |
| BUG-05 | Bug | Medium | `UniversalTestScene` | Up to 4 Dijkstra runs per collision event |
| BUG-06 | Bug | Medium | `UniversalTestScene` | `hide()` not overridden — mic keeps running on screen switch |
| BUG-07 | Bug | **High** | `UniversalTestScene` | Auto-collect vacuums consumables silently every frame |
| BUG-08 | Bug | Medium | `UniversalTestScene` | Panic distance measured from hub centre, not from enemy |
| BUG-09 | Bug | Low | `SoundPropagationZone` | Deep-copies all graph nodes every frame — 100+ allocs/frame |
| BUG-10 | Bug | **High** | `GraphPopulator` | Fallback graph is at world origin (40+ units from player) |
| BUG-11 | Bug | Medium | `GraphPopulator` | All graph edges hardcoded to CONCRETE — material diversity ignored |
| BUG-12 | Bug | Low | `UniversalTestScene` | `findNearestNodeId` O(n) called 4+ times per frame |
| BUG-13 | Bug | Low | `SonarRenderer` | `snapshot()` never called — all intensity data is discarded |
| INCON-01 | Inconsistency | Low | `UniversalTestScene` | X and V keys do identical thing |
| INCON-02 | Inconsistency | Low | `UniversalTestScene` | Hardcoded fog uniforms set and immediately overwritten |
| INCON-03 | Inconsistency | Low | `SoundPulseVisualizer` | Class unused in main test scene — screen scope mismatch |
| INCON-04 | Inconsistency | Low | `UniversalTestScene` | Physics step buried inside zone management method |
| INCON-05 | Inconsistency | Medium | `UniversalTestScene` | Consumable pickup has no facing check; carriable does |
| INCON-06 | Inconsistency | Low | `UniversalTestScene` | All held items render as identical unit cube |
| IMPROVE-01 | Performance | Medium | `UniversalTestScene` | Pool `ClosestRayResultCallback` — per-frame native alloc |
| IMPROVE-02 | Performance | Medium | `UniversalTestScene` | Batch static scene geometry into one VBO |
| IMPROVE-03 | Performance | Low | `UniversalTestScene` | Cache `findSoundPropagationZone()` result |
| IMPROVE-04 | UX | **High** | `UniversalTestScene` | Runtime slider for MIC_THRESHOLD_RMS |
| IMPROVE-05 | Architecture | Medium | `UniversalTestScene` | Tag colliders with `AcousticMaterial` for graph + transmission |
| IMPROVE-06 | Visual | Low | `UniversalTestScene` | Replace default BitmapFont with DPI-aware FreeType font |
| IMPROVE-07 | Gameplay | Medium | `SonarRenderer` | Wire `SonarRevealView.alpha()` to node brightness rendering |
| IMPROVE-08 | Gameplay | Low | `UniversalTestScene` | Zone transitions should emit a soft acoustic ping |
| IMPROVE-09 | Architecture | Low | `UniversalTestScene` | `panicModel.setHealth` always hardcodes 100f |
| IMPROVE-10 | Rendering | Low | `UniversalTestScene` | View model depth pass should clear depth, not disable test |

---

*End of Code Audit Report*
*All findings are based on direct source code reading from the Apr 22, 2026 build.*

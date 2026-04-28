# Resonance — Master Plan Addendum

**Patches to:** `RESONANCE_MASTER_PLAN.md`  
**Changes:** 5 corrections and additions

---

## Table of Contents

- [Patch 1 — Enemy Hearing Filter](#patch-1--enemy-hearing-filter)
- [Patch 2 — ModelDebugScreen: Collision Wireframe](#patch-2--modeldebugscreen-collision-wireframe)
- [Patch 3 — ModelDebugScreen: Texture Support](#patch-3--modeldebugscreen-texture-support)
- [Patch 4 — Single Map Architecture](#patch-4--single-map-architecture)
- [Patch 5 — Story System](#patch-5--story-system)
- [Updated Implementation Order](#updated-implementation-order)
- [Updated Acceptance Criteria (new entries only)](#updated-acceptance-criteria)

---

## Patch 1 — Enemy Hearing Filter

### Problem with the current plan

`EnemyPerception.onSoundHeard(SoundEventData data)` receives **every** `SoundEventData` that the propagation graph emits — including ambience, environment drips, footstep echoes, and `WorldSoundEmitter` outputs. This makes the enemy react to sounds it should not know about (e.g. cave drips, rain ambience, the background hum of a generator).

### What the enemy should hear

| Source | Should enemy hear? | Reason |
|--------|-------------------|--------|
| Player footsteps | ✅ Yes | Physical impact, player-caused |
| Player voice (mic) | ✅ Yes | Player makes noise |
| Thrown/dropped item | ✅ Yes | Physical impact, player-caused |
| Alarm / beeping | ✅ Yes | Loud event, scripted or triggered |
| Glass breaking | ✅ Yes | High-priority physics impact |
| Door creak (player-opened) | ✅ Yes | Player-caused interaction |
| `WorldSoundEmitter` drips | ❌ No | Background ambience — always there |
| `AmbienceTrack` / music | ❌ No | Not diegetic — not in the world |
| Propagation graph echoes of ambience | ❌ No | Re-echoes of above |

### Solution — `SoundHearingCategory` tag on `SoundEvent`

Add a `hearingCategory` field to `SoundEvent` (or its config) that the enemy perception system filters on:

```java
// Existing SoundEvent enum — add hearingCategory per value
public enum SoundEvent {
    // Player-caused — enemy can hear
    FOOTSTEP           (HearingCategory.PLAYER_ACTION,  EventPriority.LOW,    0.3f),
    FOOTSTEP_RUN       (HearingCategory.PLAYER_ACTION,  EventPriority.MEDIUM, 0.6f),
    MIC_VOICE          (HearingCategory.PLAYER_ACTION,  EventPriority.HIGH,   0.8f),
    ITEM_THROW         (HearingCategory.PLAYER_ACTION,  EventPriority.MEDIUM, 0.5f),
    ITEM_IMPACT        (HearingCategory.PLAYER_ACTION,  EventPriority.MEDIUM, 0.6f),
    GLASS_BREAK        (HearingCategory.PLAYER_ACTION,  EventPriority.HIGH,   1.0f),
    DOOR_SLAM          (HearingCategory.PLAYER_ACTION,  EventPriority.HIGH,   0.9f),
    DOOR_CREAK         (HearingCategory.PLAYER_ACTION,  EventPriority.LOW,    0.4f),

    // Scripted / event-driven — enemy can hear
    ALARM              (HearingCategory.SCRIPTED_EVENT, EventPriority.CRITICAL, 1.2f),
    EXPLOSION          (HearingCategory.SCRIPTED_EVENT, EventPriority.CRITICAL, 1.5f),
    ENEMY_SCREAM       (HearingCategory.SCRIPTED_EVENT, EventPriority.HIGH,   1.0f),
    PHYSICS_COLLAPSE   (HearingCategory.SCRIPTED_EVENT, EventPriority.HIGH,   0.9f),

    // Environment / ambience — enemy CANNOT hear
    AMBIENT_CREAK      (HearingCategory.AMBIENCE, EventPriority.LOW,  0.3f),
    WATER_DRIP         (HearingCategory.AMBIENCE, EventPriority.LOW,  0.1f),
    AMBIENT_HUM        (HearingCategory.AMBIENCE, EventPriority.LOW,  0.1f),
    WIND               (HearingCategory.AMBIENCE, EventPriority.LOW,  0.1f);

    public final HearingCategory hearingCategory;
    public final EventPriority   priority;
    public final float           defaultBaseIntensity;

    SoundEvent(HearingCategory h, EventPriority p, float i) {
        this.hearingCategory     = h;
        this.priority            = p;
        this.defaultBaseIntensity = i;
    }
}
```

```java
// New enum
public enum HearingCategory {
    PLAYER_ACTION,    // player-caused noise — enemy always eligible to hear
    SCRIPTED_EVENT,   // alarm, explosion, etc — enemy always eligible to hear
    AMBIENCE          // background — enemy never hears
}
```

### Updated `EnemyPerception.onSoundHeard()`

```java
public void onSoundHeard(SoundEventData data) {
    // ← NEW: filter out ambience before the enemy processes anything
    if (data.soundEvent().hearingCategory() == HearingCategory.AMBIENCE) {
        return;  // enemy is deaf to this category
    }

    // Intensity threshold — very faint events don't reach the enemy
    if (data.baseIntensity() < MIN_AUDIBLE_INTENSITY) {
        return;
    }

    lastHeardPosition  = data.worldPosition();
    lastHeardIntensity = data.baseIntensity();
    hasUnhandledSound  = true;
}
```

### Updated `SpatialSfxEmitter` — only propagate if not ambience

`WorldSoundEmitter` (drips, hums) currently calls `sfxEmitter.playAndPropagate()` which injects into the graph. The graph then delivers to `EnemyPerception`. The filter above already solves this, but add a second guard in `playAndPropagate()` to skip the graph injection entirely for `AMBIENCE` category — this avoids wasting propagation compute on sounds the enemy will ignore anyway:

```java
public void playAndPropagate(String wavPath, SoundEvent graphEvent, ...) {
    // ... spatial WAV playback (always runs, player still hears drips) ...

    // Only inject into propagation graph if the enemy is eligible to hear this
    boolean shouldPropagate = graphEvent != null
        && graphEvent.hearingCategory() != HearingCategory.AMBIENCE
        && nodeResolver != null;

    if (shouldPropagate) {
        String nodeId = nodeResolver.apply(emitterPos);
        SoundEventData data = new SoundEventData(graphEvent, nodeId,
            emitterPos, graphIntensity, elapsedSeconds);
        propagationOrchestrator.emitSoundEvent(data, elapsedSeconds);
    }
}
```

### Mic voice injection

The microphone input (`RealtimeMicSystem`) should inject `MIC_VOICE` into the propagation graph when the player is speaking above threshold. This is the only ambience-adjacent sound that *should* reach the enemy — and it has `HearingCategory.PLAYER_ACTION`, so the filter passes it through automatically:

```java
// In UniversalTestScene / GltfMapTestScene — in the mic update path:
if (realtimeMicSystem.getCurrentRmsLevel() > VOICE_DETECTION_THRESHOLD) {
    String nodeId = propagationOrchestrator.findNearestNode(playerPosition);
    SoundEventData voiceData = new SoundEventData(
        SoundEvent.MIC_VOICE, nodeId, playerPosition,
        realtimeMicSystem.getNormalizedLevel(),  // intensity scales with volume
        elapsedSeconds
    );
    propagationOrchestrator.emitSoundEvent(voiceData, elapsedSeconds);
}
```

### Files to change

| File | Change |
|------|--------|
| `sound/SoundEvent.java` | Add `HearingCategory` field and `hearingCategory()` getter to every enum value |
| `sound/HearingCategory.java` | **Create** — new enum |
| `enemy/EnemyPerception.java` | Add `HearingCategory.AMBIENCE` filter in `onSoundHeard()` |
| `audio/SpatialSfxEmitter.java` | Skip graph injection for `AMBIENCE` category |
| `devTest/GltfMapTestScene.java` or `UniversalTestScene.java` | Add mic → `MIC_VOICE` propagation in mic update path |

---

## Patch 2 — ModelDebugScreen: Collision Wireframe

### What it does

When a `-map.gltf` file is loaded in `ModelDebugScreen`, press **K** to toggle a wireframe overlay that shows every triangle of the `btBvhTriangleMeshShape` collision mesh drawn in green. This lets you verify that the collision geometry matches the visual model exactly — critical for any room that players need to navigate.

### Why this is non-trivial

`btBvhTriangleMeshShape` does not expose its triangles for reading back after construction. The triangles must be extracted **before** handing them to Bullet — so `MapCollisionBuilder.BvhResult` needs to also store the raw triangle data for the debug draw.

### Plan

#### A. Store raw triangle data in `BvhResult`

```java
// MapCollisionBuilder.BvhResult — add triangle snapshot
public record BvhResult(
    btTriangleMesh          triMesh,
    btBvhTriangleMeshShape  shape,
    btDefaultMotionState    motionState,
    btRigidBody             body,
    float[]                 debugTriangles   // ← NEW: flat array [x0,y0,z0, x1,y1,z1, x2,y2,z2, ...]
) {
    public void dispose() {
        body.dispose(); motionState.dispose(); shape.dispose(); triMesh.dispose();
        // debugTriangles is a plain float[] — GC handles it
    }
}
```

#### B. Populate `debugTriangles` in `MapCollisionBuilder.build()`

While extracting triangles to feed into `btTriangleMesh`, also append them to the debug array:

```java
public static BvhResult build(ModelData mapData) {
    btTriangleMesh triMesh = new btTriangleMesh();
    FloatArray debugTris = new FloatArray(); // LibGDX primitive float list

    Vector3 v0 = new Vector3(), v1 = new Vector3(), v2 = new Vector3();

    for (Mesh mesh : mapData.model().meshes) {
        int vertexSize = mesh.getVertexSize() / 4;
        float[] vertices = new float[mesh.getNumVertices() * vertexSize];
        short[] indices  = new short[mesh.getNumIndices()];
        mesh.getVertices(vertices);
        mesh.getIndices(indices);

        for (int i = 0; i < indices.length - 2; i += 3) {
            int i0 = (indices[i]     & 0xFFFF) * vertexSize;
            int i1 = (indices[i + 1] & 0xFFFF) * vertexSize;
            int i2 = (indices[i + 2] & 0xFFFF) * vertexSize;

            v0.set(vertices[i0], vertices[i0+1], vertices[i0+2]);
            v1.set(vertices[i1], vertices[i1+1], vertices[i1+2]);
            v2.set(vertices[i2], vertices[i2+1], vertices[i2+2]);

            triMesh.addTriangle(v0, v1, v2, true);

            // ← NEW: snapshot for debug draw
            debugTris.addAll(v0.x, v0.y, v0.z,
                             v1.x, v1.y, v1.z,
                             v2.x, v2.y, v2.z);
        }
    }

    // ... build shape, motionState, body as before ...

    return new BvhResult(triMesh, shape, motionState, body, debugTris.toArray());
}
```

#### C. `CollisionWireframeRenderer` — draw the triangles

Create a new helper class used by `ModelDebugScreen`:

```java
package io.github.superteam.resonance.devTest;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.PerspectiveCamera;

/**
 * Draws every triangle of a BVH collision mesh as a green wireframe.
 * Feed it the debugTriangles float[] from BvhResult.
 *
 * Render pass: call between ShapeRenderer.begin(Line) and .end().
 */
public final class CollisionWireframeRenderer {

    private static final Color WIREFRAME_COLOR      = new Color(0f, 1f, 0.2f, 0.55f);
    private static final Color WIREFRAME_COLOR_BACK = new Color(0f, 0.5f, 0.1f, 0.2f);

    /**
     * Draw all triangles as wireframe.
     *
     * @param triangles  flat float[] from BvhResult.debugTriangles()
     *                   every 9 floats = one triangle: [x0,y0,z0, x1,y1,z1, x2,y2,z2]
     */
    public void render(ShapeRenderer shapes, PerspectiveCamera camera,
                       float[] triangles, boolean enabled) {
        if (!enabled || triangles == null) return;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);

        for (int i = 0; i < triangles.length - 8; i += 9) {
            float x0 = triangles[i],   y0 = triangles[i+1], z0 = triangles[i+2];
            float x1 = triangles[i+3], y1 = triangles[i+4], z1 = triangles[i+5];
            float x2 = triangles[i+6], y2 = triangles[i+7], z2 = triangles[i+8];

            shapes.setColor(WIREFRAME_COLOR);
            shapes.line(x0, y0, z0, x1, y1, z1);
            shapes.line(x1, y1, z1, x2, y2, z2);
            shapes.line(x2, y2, z2, x0, y0, z0);
        }

        shapes.end();
    }
}
```

> **Performance note:** A dense map mesh can have 50 000–200 000 triangles. Drawing each edge as a `ShapeRenderer.line()` call (3 lines × N triangles) may be slow in immediate mode. For large meshes, build a `Mesh` with `GL_LINES` topology once at load time and reuse it. Add a `MAX_DEBUG_TRIS = 30_000` guard — if the mesh is larger, skip the overdense triangles and show only every Nth one.

#### D. Wire into `ModelDebugScreen`

```java
// Fields
private CollisionWireframeRenderer wireframeRenderer;
private float[] collisionDebugTriangles;   // null if no map file loaded
private boolean showCollisionWireframe = false;

// In loadSelectedModel() — after loading:
if (MapLoader.isMapFile(currentModelPath)) {
    MapCollisionBuilder.BvhResult bvh = MapCollisionBuilder.build(loadedModelData);
    collisionDebugTriangles = bvh.debugTriangles();
    // Register body in a local physicsWorld if needed, or just store for debug draw
    // Dispose old bvh if reloading
} else {
    collisionDebugTriangles = null;
}

// In render():
wireframeRenderer.render(shapeRenderer, camera, collisionDebugTriangles, showCollisionWireframe);

// In keyDown():
case Input.Keys.K:
    showCollisionWireframe = !showCollisionWireframe;
    statusMessage = "Collision wireframe: " + (showCollisionWireframe ? "ON" : "OFF");
    return true;
```

#### E. HUD indicator

Add to the key reference panel:

```
K = collision wireframe toggle
```

And in the diagnostic overlay when wireframe is on:

```
[K] COLLISION MESH: ON   Triangles: 12,847
```

### Files to create / change

| File | Action |
|------|--------|
| `map/MapCollisionBuilder.java` | Add `debugTriangles` field to `BvhResult`; populate during build |
| `devTest/CollisionWireframeRenderer.java` | **Create** |
| `devTest/ModelDebugScreen.java` | Add `K` key handler, wireframe renderer call, HUD line |

---

## Patch 3 — ModelDebugScreen: Texture Support

### Problem

`ModelDebugScreen` renders models using a `ModelBatch` with a basic `Environment` but no texture binding. This causes:
- Textures missing entirely (solid grey/white materials)
- Console errors like `No texture bound` or `Sampler u_diffuseTexture not set`

The root cause depends on which loader was used:

| Loader | Texture state |
|--------|-------------|
| `.g3dj` / `.g3db` | Textures in `Material` objects, LibGDX `ModelBatch` renders them automatically — should work |
| `.gltf` / `.glb` via `GltfModelLoader` | gdx-gltf uses its own `SceneRenderableSorter` and `PBRShaderProvider` — a standard `ModelBatch` with default shader does NOT render gdx-gltf materials correctly |

### Fix A — Use gdx-gltf's `SceneManager` for gltf models

gdx-gltf provides `SceneManager` which wraps `ModelBatch` with the correct PBR shader and light setup. Replace the bare `ModelBatch` in `ModelDebugScreen` with a dual-path renderer:

```java
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;

// ModelDebugScreen fields — replace ModelBatch with:
private SceneManager sceneManager;   // for gltf files
private ModelBatch   modelBatch;     // for g3d files
private boolean      useSceneManager;
```

```java
// In show() / create():
sceneManager = new SceneManager();

// Add a directional light
DirectionalLightEx light = new DirectionalLightEx();
light.direction.set(-0.5f, -1f, -0.3f).nor();
light.color.set(Color.WHITE);
sceneManager.environment.add(light);

// Ambient
sceneManager.setAmbientLight(0.5f);
```

```java
// In loadSelectedModel():
String ext = currentModelPath.toLowerCase();
useSceneManager = ext.endsWith(".gltf") || ext.endsWith(".glb");

if (useSceneManager) {
    // gdx-gltf path
    SceneAsset sceneAsset = ext.endsWith(".glb")
        ? new GLBLoader().load(Gdx.files.internal(currentModelPath), true)
        : new GLTFLoader().load(Gdx.files.internal(currentModelPath), true);

    sceneManager.removeScene(currentGltfScene);  // remove old if reloading
    currentGltfScene = new Scene(sceneAsset.scene);
    sceneManager.addScene(currentGltfScene);

    if (currentSceneAsset != null) currentSceneAsset.dispose();
    currentSceneAsset = sceneAsset;
} else {
    // g3d path — existing ModelBatch + ModelInstance approach (unchanged)
    ...
}
```

```java
// In render():
Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

if (useSceneManager) {
    sceneManager.update(deltaTime);
    sceneManager.render();         // SceneManager handles camera, lights, PBR
} else {
    modelBatch.begin(camera);
    modelBatch.render(modelInstance, environment);
    modelBatch.end();
}
```

```java
// In dispose():
if (sceneManager != null) sceneManager.dispose();
if (currentSceneAsset != null) currentSceneAsset.dispose();
```

### Fix B — Texture path resolution for `.gltf` with external textures

`.gltf` files reference textures by relative path (e.g. `../textures/wall_albedo.png`). The `true` flag in `GLTFLoader.load(fileHandle, true)` enables relative resolution from the file's folder. If textures still don't load:

1. Verify the texture files exist alongside the `.gltf` in `assets/models/`
2. Check the `.gltf` `"uri"` fields — they must be relative to the `.gltf` file location
3. All textures must be in `assets/` (inside the project) — `Gdx.files.internal()` cannot reach outside

### Fix C — `g3dj` / `g3db` texture path fix

For `.g3dj` files, LibGDX resolves texture paths relative to the **working directory** (usually the project root when running from Gradle), not relative to the model file. If textures are missing for `.g3dj`:

```java
// When creating the G3dModelLoader:
G3dModelLoader g3dLoader = new G3dModelLoader(new JsonReader(),
    new FileHandleResolver() {
        @Override
        public FileHandle resolve(String fileName) {
            // First try relative to the model file's directory
            String modelDir = currentModelPath.substring(0,
                currentModelPath.lastIndexOf('/') + 1);
            FileHandle relative = Gdx.files.internal(modelDir + fileName);
            if (relative.exists()) return relative;
            // Fall back to internal root
            return Gdx.files.internal(fileName);
        }
    });
```

### Files to change

| File | Action |
|------|--------|
| `devTest/ModelDebugScreen.java` | Add `SceneManager` dual-path; fix texture resolver for g3dj |
| `core/build.gradle` | Confirm `api "com.github.mgsx-dev:gdx-gltf:2.2.1"` (from earlier plan) |

---

## Patch 4 — Single Map Architecture

### Decision

There is only **one map** — a single large GLTF file containing all rooms. This changes several assumptions in the current plan.

### What this means for each system

#### `MapLoader` / `MapCollisionBuilder`

No change needed. They already load one `-map.gltf` and build one `btBvhTriangleMeshShape`. Works as-is for a large single file.

**Performance note:** A single-file map with many rooms will produce a large BVH. LibGDX Bullet handles this well — `btBvhTriangleMeshShape` has O(log N) raytest. Build time at load may be a few hundred milliseconds for a complex mesh; this is acceptable during the transition screen.

#### `MapDocument` (Map Editor JSON)

Rename the field and update guidance:

```json
{
  "mapName": "Resonance — Full Map",
  "gltfMapPath": "models/resonance-map.gltf",
  "objects": [
    // All doors, triggers, emitters, checkpoints for ALL rooms in one file
  ]
}
```

There is one `MapDocument` JSON for the whole game. The Map Editor manages all objects in a single file. The `SceneOutlinePanel` should support **filtering by room** so the list stays manageable:

```
SceneOutlinePanel
  ├── [Filter: "server-room"] ← search box
  └── Filtered list:
        server-room-door-01
        server-room-trigger-entry
        server-room-checkpoint
        server-room-lamp-01
        drip-emitter-server-01
```

#### `Scene / Level Transition System` (System 14)

**Remove** `SceneTransitionSystem` and `TransitionScreen` as separate level-transition infrastructure — there is only one map to load. Replace with:

```
transition/
  RoomTransitionSystem.java   — fade-to-black when moving between named rooms
  FadeOverlay.java            — full-screen quad that fades in/out (keep)
```

`RoomTransitionSystem` triggers a fade-to-black + fade-in when the player crosses a `ROOM_BOUNDARY` trigger volume (placed in the Map Editor). This gives a cinematic beat between rooms without any actual scene loading:

```java
public final class RoomTransitionSystem {
    private float   fadeAlpha = 0f;
    private boolean fadingOut = false;
    private boolean fadingIn  = false;
    private String  pendingRoomName;

    public void triggerRoomTransition(String enteringRoom) {
        pendingRoomName = enteringRoom;
        fadingOut = true;
    }

    public void update(float delta, DialogueSystem dialogue) {
        if (fadingOut) {
            fadeAlpha = Math.min(1f, fadeAlpha + delta / FADE_DURATION);
            if (fadeAlpha >= 1f) {
                fadingOut = false;
                fadingIn  = true;
                // Optional: show room name as subtitle on fade-in
                dialogue.showSubtitle(pendingRoomName, 2.0f);
            }
        }
        if (fadingIn) {
            fadeAlpha = Math.max(0f, fadeAlpha - delta / FADE_DURATION);
            if (fadeAlpha <= 0f) fadingIn = false;
        }
    }

    public float fadeAlpha() { return fadeAlpha; }
}
```

Map Editor gains a new object type:

```java
// Add to MapObjectType enum:
ROOM_BOUNDARY   // trigger that fires a room transition fade
```

```json
{
  "id": "enter-server-room-boundary",
  "type": "ROOM_BOUNDARY",
  "position": { "x": 12.0, "y": 1.5, "z": 0.0 },
  "properties": {
    "halfWidth": "0.3", "halfHeight": "2.0", "halfDepth": "4.0",
    "roomName": "Server Room",
    "onEnterEvent": "enter-server-room"
  }
}
```

#### `Save / Checkpoint System` (System 13)

`SaveData.mapName` stays but is always `"resonance-full-map"`. The `checkpointId` is the meaningful location identifier. No structural change needed.

#### `NavMesh` / enemy pathfinding

A single large map means one large nav mesh (waypoint graph). The `NavMeshBuilder` reads all waypoints from the single `MapDocument`. Group waypoints by room using a `"room"` property on `SPAWN_POINT` nav waypoints:

```json
{
  "id": "nav-server-01",
  "type": "SPAWN_POINT",
  "position": { "x": 8.0, "y": 0.0, "z": -2.0 },
  "properties": { "label": "nav-waypoint", "room": "server-room" }
}
```

This lets the enemy's `PatrolState` be scoped to waypoints within a specific room tag, preventing the enemy from randomly wandering to another wing of the building.

### Files to change

| File | Action |
|------|--------|
| `transition/SceneTransitionSystem.java` | **Repurpose** as `RoomTransitionSystem` |
| `transition/TransitionScreen.java` | **Delete** — no separate screen loading needed |
| `map/MapObjectType.java` | Add `ROOM_BOUNDARY` |
| `map/MapDocumentLoader.java` | Handle `ROOM_BOUNDARY` type → `ZoneTrigger` + room fade |
| `mapeditor/ObjectPalette.java` | Add `ROOM_BOUNDARY` to Gameplay category |
| `enemy/NavMeshBuilder.java` | Group waypoints by room tag |

---

## Patch 5 — Story System

### What it does

The Story System sits above the Event System and enforces **linear narrative progression**. Players cannot trigger story-chapter events out of order, cannot skip cutscene beats, and cannot interact with story-gated objects until the required story step is reached. Non-story events (ambience, gameplay triggers, physics sounds) are unaffected.

### Why a separate system and not just Event flags

The Event System's `ONCE` repeat mode and `SET_FLAG` actions can simulate story gating, but:
- It requires manually checking flags in every trigger condition — error-prone
- There is no concept of "current chapter" — it's just a bag of flags
- A designer cannot see the overall story flow in one place
- There is no enforcement mechanism — an incorrectly wired trigger can skip a chapter

The Story System adds a first-class concept of **chapters** and **beats** that the rest of the game respects.

### Package: `story`

```
story/
  StorySystem.java              — master story controller
  StoryChapter.java             — a chapter: id, name, ordered list of beats
  StoryBeat.java                — one required step within a chapter
  StoryBeatCondition.java       — what must happen for the beat to complete
  StoryLoader.java              — loads story.json from disk
  StoryGate.java                — blocks event/interaction until beat is reached
  StoryEventAction.java         — EventAction that advances the story (links Event System)
```

---

### Core classes

#### `StoryBeat`

A beat is the smallest story unit — one thing the player must do or witness before the story advances.

```java
public final class StoryBeat {
    public enum CompletionMode {
        ON_EVENT_FIRE,      // a specific event must fire
        ON_FLAG_SET,        // a flag must be set
        ON_INTERACTION,     // player must interact with a specific object
        ON_ZONE_ENTRY,      // player must enter a zone
        AUTOMATIC           // completes immediately when the chapter starts it
    }

    private final String          id;
    private final String          chapterId;
    private final CompletionMode  completionMode;
    private final String          completionParameter;  // eventId / flagName / interactableId / zoneId
    private final String          onCompleteEventId;    // fires when this beat completes (optional)
    private final boolean         blockingUntilComplete; // if true, story-gated objects cannot be used
    private boolean               completed = false;

    public boolean tryComplete(String parameter, EventState eventState) {
        if (completed) return false;
        boolean satisfied = switch (completionMode) {
            case ON_EVENT_FIRE   -> parameter.equals(completionParameter);
            case ON_FLAG_SET     -> eventState.getFlag(completionParameter);
            case ON_INTERACTION  -> parameter.equals(completionParameter);
            case ON_ZONE_ENTRY   -> parameter.equals(completionParameter);
            case AUTOMATIC       -> true;
        };
        if (satisfied) completed = true;
        return satisfied;
    }

    public boolean isCompleted() { return completed; }
    public String id()           { return id; }
    public String chapterId()    { return chapterId; }
    public boolean isBlocking()  { return blockingUntilComplete; }
    public String onCompleteEventId() { return onCompleteEventId; }
}
```

---

#### `StoryChapter`

```java
public final class StoryChapter {
    private final String        id;
    private final String        displayName;
    private final List<StoryBeat> beats;      // ordered — must complete in sequence
    private int                 currentBeatIndex = 0;
    private boolean             active = false;
    private boolean             completed = false;

    public void start() { active = true; }

    public StoryBeat currentBeat() {
        if (currentBeatIndex >= beats.size()) return null;
        return beats.get(currentBeatIndex);
    }

    /** Called by StorySystem when the current beat completes. */
    public boolean advanceBeat() {
        currentBeatIndex++;
        if (currentBeatIndex >= beats.size()) {
            completed = true;
            active = false;
            return true; // chapter complete
        }
        return false;
    }

    public boolean isCompleted() { return completed; }
    public boolean isActive()    { return active; }
    public String  id()          { return id; }
    public String  displayName() { return displayName; }
}
```

---

#### `StorySystem`

```java
public final class StorySystem {

    private final List<StoryChapter>  chapters;           // ordered
    private int                       currentChapterIndex = 0;
    private final EventBus            eventBus;
    private final EventState          eventState;

    public StorySystem(List<StoryChapter> chapters, EventBus eventBus, EventState eventState) {
        this.chapters   = chapters;
        this.eventBus   = eventBus;
        this.eventState = eventState;
        if (!chapters.isEmpty()) chapters.get(0).start();
    }

    /** Called by EventBus listener on every event fire — checks if it completes a beat. */
    public void onEventFired(String eventId, EventContext ctx) {
        StoryBeat beat = currentBeat();
        if (beat == null || beat.isCompleted()) return;

        if (beat.tryComplete(eventId, eventState)) {
            completeBeat(beat, ctx);
        }
    }

    /** Called by RaycastInteractionSystem on interact. */
    public void onInteraction(String interactableId, EventContext ctx) {
        StoryBeat beat = currentBeat();
        if (beat == null || beat.isCompleted()) return;
        if (beat.tryComplete(interactableId, eventState)) {
            completeBeat(beat, ctx);
        }
    }

    /** Called by ZoneTrigger / TriggerEvaluator when player enters a zone. */
    public void onZoneEntered(String zoneId, EventContext ctx) {
        StoryBeat beat = currentBeat();
        if (beat == null || beat.isCompleted()) return;
        if (beat.tryComplete(zoneId, eventState)) {
            completeBeat(beat, ctx);
        }
    }

    private void completeBeat(StoryBeat beat, EventContext ctx) {
        // Fire the beat's completion event if specified
        if (beat.onCompleteEventId() != null) {
            eventBus.fire(beat.onCompleteEventId(), ctx);
        }

        // Advance the chapter
        StoryChapter chapter = currentChapter();
        boolean chapterDone = chapter.advanceBeat();
        if (chapterDone) {
            advanceChapter(ctx);
        }

        // Auto-complete the next beat if it is AUTOMATIC
        StoryBeat next = currentBeat();
        if (next != null && next.completionMode() == StoryBeat.CompletionMode.AUTOMATIC) {
            completeBeat(next, ctx);
        }
    }

    private void advanceChapter(EventContext ctx) {
        currentChapterIndex++;
        if (currentChapterIndex < chapters.size()) {
            StoryChapter next = chapters.get(currentChapterIndex);
            next.start();
            ctx.eventBus.fire("chapter-started-" + next.id(), ctx);
        } else {
            ctx.eventBus.fire("story-complete", ctx);
        }
    }

    // --- Gating API ---

    /**
     * Returns true if the story has reached at least the given beat.
     * Use this to gate interactions, item spawns, dialogue, etc.
     *
     * Example: door.setLocked(!storySystem.hasReached("chapter-2", "found-keycard"));
     */
    public boolean hasReached(String chapterId, String beatId) {
        for (StoryChapter ch : chapters) {
            if (ch.id().equals(chapterId)) {
                for (StoryBeat b : ch.beats()) {
                    if (b.id().equals(beatId)) return b.isCompleted();
                }
            }
        }
        return false;
    }

    public boolean isChapterActive(String chapterId) {
        StoryChapter ch = currentChapter();
        return ch != null && ch.id().equals(chapterId);
    }

    public StoryBeat  currentBeat()    { return currentChapter() == null ? null : currentChapter().currentBeat(); }
    public StoryChapter currentChapter(){ return currentChapterIndex < chapters.size() ? chapters.get(currentChapterIndex) : null; }

    // --- Debug ---
    public String debugStatus() {
        StoryChapter ch = currentChapter();
        StoryBeat    b  = currentBeat();
        if (ch == null) return "Story complete";
        return String.format("Chapter: %s  Beat: %s", ch.id(), b == null ? "done" : b.id());
    }
}
```

---

#### `StoryGate` — block interactions until a beat is reached

```java
public final class StoryGate {

    private final StorySystem storySystem;
    private final String      requiredChapterId;
    private final String      requiredBeatId;
    private final String      blockedMessage;  // shown as subtitle when blocked

    public StoryGate(StorySystem storySystem, String requiredChapterId,
                     String requiredBeatId, String blockedMessage) {
        this.storySystem       = storySystem;
        this.requiredChapterId = requiredChapterId;
        this.requiredBeatId    = requiredBeatId;
        this.blockedMessage    = blockedMessage;
    }

    /**
     * Returns true if the player is allowed to proceed.
     * Returns false and shows a blocking message if the story hasn't reached this point.
     */
    public boolean isOpen(EventContext ctx) {
        if (storySystem.hasReached(requiredChapterId, requiredBeatId)) return true;
        if (blockedMessage != null && !blockedMessage.isEmpty()) {
            ctx.dialogueSystem.showSubtitle(blockedMessage, 2.5f);
        }
        return false;
    }
}
```

Usage in `DoorController.onInteract()`:

```java
// A story-gated door — cannot be opened until "chapter-1 / found-note" is complete
if (storyGate != null && !storyGate.isOpen(ctx)) return;
// ... rest of door open logic ...
```

---

#### `StoryEventAction` — advance story from event JSON

Allows the event system to drive story progression, so level designers can mark a beat as complete via an event action:

```java
public record StoryEventAction(String beatId) implements EventAction {
    public void execute(EventContext ctx) {
        // Manually mark a beat complete by its id
        // Useful when story progress is tied to an event rather than an interaction
        ctx.storySystem.onEventFired(beatId, ctx);
    }
}
```

Add `"type": "ADVANCE_STORY"` to the event action JSON spec:

```json
{
  "type": "ADVANCE_STORY",
  "beatId": "read-first-note"
}
```

---

#### Story JSON format

All story data lives in `assets/story/story.json`:

```json
{
  "chapters": [
    {
      "id": "prologue",
      "displayName": "Prologue — Arrival",
      "beats": [
        {
          "id": "enter-building",
          "completionMode": "ON_ZONE_ENTRY",
          "completionParameter": "zone-building-entrance",
          "onCompleteEvent": "play-intro-monologue",
          "blocking": false
        },
        {
          "id": "read-first-note",
          "completionMode": "ON_INTERACTION",
          "completionParameter": "note-reception-desk",
          "onCompleteEvent": "creak-then-groan",
          "blocking": true
        },
        {
          "id": "find-keycard",
          "completionMode": "ON_FLAG_SET",
          "completionParameter": "picked-up-keycard-red",
          "onCompleteEvent": null,
          "blocking": true
        }
      ]
    },
    {
      "id": "chapter-1",
      "displayName": "Chapter 1 — The Server Room",
      "beats": [
        {
          "id": "open-server-door",
          "completionMode": "ON_INTERACTION",
          "completionParameter": "server-room-door",
          "onCompleteEvent": "enemy-chase-start",
          "blocking": true
        },
        {
          "id": "survive-chase",
          "completionMode": "ON_ZONE_ENTRY",
          "completionParameter": "zone-safe-room",
          "onCompleteEvent": "chapter-1-safe",
          "blocking": false
        }
      ]
    }
  ]
}
```

---

#### How `StorySystem` connects to skipping prevention

The `blocking: true` flag on a `StoryBeat` means:

1. **Triggers**: `TriggerEvaluator` checks `storySystem.currentBeat()` before firing any trigger marked as story-gated. If the current beat is blocking and the trigger's `storyBeatRequired` doesn't match the current beat, the trigger is silently skipped.

2. **Interactions**: `RaycastInteractionSystem` checks `Interactable.storyGate()` — if not null and not open, shows the blocked message and does not call `onInteract()`.

3. **Dialogue sequences**: `DialogueSystem` accepts a `StoryGate` on `playSequence()` — if not open, the sequence is queued but not started until the gate opens.

```java
// TriggerEvaluator.update() — add story gate check:
public void update(float delta, TriggerEvaluationContext evalCtx, EventContext eventCtx) {
    for (Trigger trigger : triggers) {
        trigger.update(delta);

        // Story gate — skip this trigger if the story isn't ready for it
        if (trigger.requiredBeatId() != null) {
            if (!storySystem.hasReached(trigger.requiredChapterId(), trigger.requiredBeatId())) {
                continue;  // ← cannot skip this trigger's event
            }
        }

        if (trigger.evaluate(evalCtx)) {
            trigger.onConditionMet(eventBus, eventCtx);
            storySystem.onZoneEntered(trigger.id(), eventCtx); // notify story
        }
    }
}
```

#### `EventContext` update

Add `StorySystem` to `EventContext`:

```java
public final class EventContext {
    // ... existing fields ...
    public final StorySystem storySystem;    // ← add
}
```

---

#### Debug console commands for story

Add to `ConsoleRegistry`:

| Command | Example | Effect |
|---------|---------|--------|
| `story` | `story` | Print current chapter and beat |
| `beat` | `beat read-first-note` | Force-complete a beat by id |
| `chapter` | `chapter chapter-1` | Force-start a chapter |

```java
// beat command:
public final class BeatCommand implements ConsoleCommand {
    public String execute(String[] args, ...) {
        String beatId = args[1];
        storySystem.onEventFired(beatId, ctx);
        return "Beat forced: " + beatId;
    }
}
```

---

#### Updated `EventContext`

The master plan's `EventContext` now needs `StorySystem`:

```java
public final class EventContext {
    public final Vector3                      triggerPosition;
    public final Vector3                      playerPosition;
    public final float                        elapsedSeconds;
    public final SoundPropagationOrchestrator propagationOrchestrator;
    public final GameAudioSystem              audioSystem;
    public final EventBus                     eventBus;
    public final EventState                   state;
    public final SanitySystem                 sanitySystem;
    public final JumpScareDirector            jumpScareDirector;
    public final DialogueSystem               dialogueSystem;
    public final DebugConsole                 debugConsole;
    public final StorySystem                  storySystem;          // ← add
    public final InventorySystem              inventorySystem;      // ← add (needed by StoryGate)
    public final FlashlightController         flashlightController; // ← add (needed by SaveData)
}
```

### Files to create / change

| File | Action |
|------|--------|
| `story/StorySystem.java` | **Create** |
| `story/StoryChapter.java` | **Create** |
| `story/StoryBeat.java` | **Create** |
| `story/StoryLoader.java` | **Create** |
| `story/StoryGate.java` | **Create** |
| `story/StoryEventAction.java` | **Create** |
| `event/EventContext.java` | Add `storySystem`, `inventorySystem`, `flashlightController` fields |
| `event/actions/` | Add `StoryEventAction.java` to master action list |
| `trigger/TriggerEvaluator.java` | Add story gate check in `update()` |
| `trigger/Trigger.java` | Add `requiredChapterId`, `requiredBeatId` optional fields |
| `interactable/Interactable.java` | Add `storyGate()` optional return |
| `interactable/door/DoorController.java` | Check `storyGate.isOpen()` in `onInteract()` |
| `debug/built_in/BeatCommand.java` | **Create** |
| `debug/built_in/ChapterCommand.java` | **Create** |
| `debug/built_in/StoryStatusCommand.java` | **Create** |
| `assets/story/story.json` | **Create** |

---

## Updated Implementation Order

These entries are **inserted** into the master plan's implementation order table:

| Revised # | System | Depends on | Priority |
|-----------|--------|-----------|----------|
| 1 | Settings System | Nothing | 🔴 First |
| 2 | Keybind Registry | Settings | 🔴 First |
| 3 | **`HearingCategory` + `SoundEvent` tag update** | `SoundEvent` enum | 🔴 First — before any enemy work |
| 4 | Map Loader + BVH Collision (+ debug triangle storage) | ModelAssetManager, Bullet | 🔴 Foundation |
| 5 | GameAudioSystem + SoundBank | LibGDX Audio | 🔴 Foundation |
| 6 | SpatialSfxEmitter (with ambience graph-skip guard) | GameAudioSystem, PropOrchestrator | 🔴 Foundation |
| 7 | **ModelDebugScreen: texture fix (SceneManager path)** | gdx-gltf | 🟡 Dev tool — fix early |
| 8 | **ModelDebugScreen: collision wireframe** | `MapCollisionBuilder.debugTriangles` | 🟡 Dev tool |
| 9 | Footstep Material System | SpatialSfxEmitter, Bullet | 🟡 Early |
| 10 | EventState + EventBus + EventRegistry | Nothing | 🔴 Foundation |
| 11 | GameEvent + all EventActions | EventBus, GameAudioSystem | 🔴 Foundation |
| 12 | EventLoader (JSON) | GameEvent | 🟡 Early |
| 13 | **StorySystem + StoryChapter + StoryBeat + StoryLoader** | EventBus, EventState | 🟡 Early — before triggers |
| 14 | **StoryGate + StoryEventAction** | StorySystem | 🟡 Early |
| 15 | Trigger System (with story gate check) | EventBus, StorySystem | 🟡 Early |
| 16 | Raycast Interaction System | Bullet, InteractableRegistry | 🟡 Early |
| 17 | Door & Interactable System (with StoryGate) | RaycastInteraction, StorySystem | 🟡 Early |
| 18 | Map Editor (Swing) | MapDocument | 🟡 Early |
| 19 | Map Document Loader (+ ROOM_BOUNDARY) | AudioSystem, TriggerEvaluator | 🟡 Early |
| 20 | Debug Console (+ story commands) | EventBus, StorySystem stub | 🟡 Dev tool |
| 21 | Dialogue + Subtitle System | GameAudioSystem | 🟢 Mid |
| 22 | Save / Checkpoint System | EventState, DialogueSystem | 🟢 Mid |
| 23 | **RoomTransitionSystem** (replaces SceneTransition) | FadeOverlay, DialogueSystem | 🟢 Mid |
| 24 | Dynamic Lighting System | WorldShader uniforms | 🟢 Mid |
| 25 | Sanity / Fear System | LightManager, enemy stub | 🟢 Mid |
| 26 | Enemy AI — Perception (filtered by HearingCategory) | Bullet, PropGraph | 🟠 Complex |
| 27 | Enemy AI — NavMesh + Navigator | Single-map waypoints | 🟠 Complex |
| 28 | Enemy AI — State Machine + Animator | Perception, Navigator | 🟠 Complex |
| 29 | Jump Scare Director | SanitySystem, EnemyController | 🟠 Complex |
| 30 | Wire all into GltfMapTestScene | All above | 🔵 Integration |

---

## Updated Acceptance Criteria

New entries only — append to the master plan's acceptance criteria table:

| System | Test | Pass |
|--------|------|------|
| **Enemy hearing** | `WorldSoundEmitter` drip fires | Enemy does NOT investigate |
| **Enemy hearing** | `AmbienceTrack` plays | Enemy does NOT react |
| **Enemy hearing** | Player throws item → `ITEM_THROW` event | Enemy moves to investigate position |
| **Enemy hearing** | Player mic above threshold | Enemy hears `MIC_VOICE`, investigates |
| **Enemy hearing** | `ALARM` scripted event fires | Enemy hears it and reacts |
| **Collision wireframe** | Load `-map.gltf`, press K | Green triangle wireframe covers exact visual geometry |
| **Collision wireframe** | Load non-map gltf, press K | No wireframe (no collision mesh) |
| **Collision wireframe** | Press K again | Wireframe toggles off |
| **Texture fix** | Load `.gltf` with external textures | Textures visible on model surfaces |
| **Texture fix** | Load `.glb` with embedded textures | Textures visible |
| **Texture fix** | Load `.g3dj` with textures | Textures visible |
| **Single map** | Load `resonance-map.gltf` | All rooms present in one mesh |
| **Single map** | Walk through ROOM_BOUNDARY trigger | Fade-to-black, fade-in, room name subtitle |
| **Story — gate** | Beat `read-first-note` not yet complete | Cannot interact with server-room door |
| **Story — gate** | Beat `read-first-note` complete | Server-room door becomes interactable |
| **Story — sequence** | Skip zone trigger before beat complete | Trigger silently skipped |
| **Story — advance** | Interact with `note-reception-desk` | Beat completes, `creak-then-groan` fires |
| **Story — chapter advance** | All beats in `prologue` done | `chapter-1` starts automatically |
| **Story — debug** | Console: `story` | Prints current chapter + beat |
| **Story — debug** | Console: `beat read-first-note` | Beat force-completes, story advances |
| **Story — auto beat** | AUTOMATIC beat at chapter start | Completes instantly, next beat begins |

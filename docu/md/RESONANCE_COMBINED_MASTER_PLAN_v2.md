# Resonance — Combined Master Plan (v2)

**Project:** `io.github.superteam.resonance`  
**Scope:** `core` + `lwjgl3` + `server` + `shared` modules  
**Total systems:** 23  
**Source documents merged:**
- `RESONANCE_MASTER_PLAN.md` — 17 core systems
- `MASTER_PLAN_ADDENDUM.md` — 5 patches (integrated inline)
- `KMEANS_BEHAVIOR_PLAN.md` — System 18: Player Behaviour Classification
- `THREE_FEATURES_PLAN.md` — Systems 19–21: Footprint Tracking, Hallucinations, Breath-Hold
- **v2 changes** — System 7 door physics rewrite, System 23 UniversalTestScreen overhaul

> Addendum patches are **not** listed as separate sections — they are integrated directly into the systems they affect. The `[PATCH]` marker calls out where a patch was applied. `[v2]` marks changes introduced in this revision.

---

## 📋 Progress Reporting — REQUIRED

**At the end of every development session, update `MASTER_PLAN_REPORT.md`.**

This file is the canonical handoff document. GitHub Copilot and any developer picking up the project reads it first to know exactly where we left off.

### What `MASTER_PLAN_REPORT.md` must contain

1. **Session date** — when the report was last updated
2. **Per-system status table** — one row per system with status emoji (🟢 Done / 🟡 Partial / 🔴 Missing)
3. **Detailed system audit** — for each system: which files are present, which are missing, and what specific gaps exist vs. this master plan
4. **What to build next** — ordered by dependency priority
5. **File count by package** — quick reference for which packages are empty
6. **Notes for Copilot** — project conventions, key integration points

### Status emoji rules

| Icon | Meaning |
|------|---------|
| 🟢 Done | All files from this master plan are present and the system is wired up |
| 🟡 Partial | Some files exist but the system is incomplete vs. the spec in this document |
| 🔴 Missing | Zero source files for this system exist |

### When to update

- After every coding session, before pushing
- After completing any system (flip its emoji to 🟢)
- After discovering a new gap (add it to the relevant system's audit section)
- Copilot: read `MASTER_PLAN_REPORT.md` first on every session start, then refer back to the relevant section of this plan for the exact API to implement

---

## Table of Contents

### Group 0 — Foundation
1. [Event System](#1-event-system)
2. [Sound System — WAV Layer](#2-sound-system--wav-layer)
3. [Trigger System](#3-trigger-system)
4. [Auto-Bullet from `-map.gltf`](#4-auto-bullet-from--mapgltf)
5. [Map Editor — Swing Tool](#5-map-editor--swing-tool)

### Group A — Core Gameplay Loops
6. [Enemy AI System](#6-enemy-ai-system)
7. [Door & Interactable System — Physics Drag Model](#7-door--interactable-system--physics-drag-model) ← **[v2] Full rewrite**
8. [Raycast Interaction System](#8-raycast-interaction-system)

### Group B — Horror Atmosphere
9. [Dynamic Lighting System](#9-dynamic-lighting-system)
10. [Sanity / Fear System](#10-sanity--fear-system)
11. [Jump Scare Director](#11-jump-scare-director)

### Group C — Narrative & Progression
12. [Dialogue & Subtitle System](#12-dialogue--subtitle-system)
13. [Save / Checkpoint System](#13-save--checkpoint-system)
14. [Story System](#14-story-system)
15. [Room Transition System](#15-room-transition-system)

### Group D — Developer Tools
16. [In-Game Debug Console](#16-in-game-debug-console)
17. [Settings System](#17-settings-system)
18. [Footstep Material System](#18-footstep-material-system)

### Group E — Adaptive AI
19. [Player Behaviour Classification (K-Means)](#19-player-behaviour-classification-k-means)

### Group F — Player Systems
20. [Enemy Footprint Tracking](#20-enemy-footprint-tracking)
21. [Sanity Hallucinations](#21-sanity-hallucinations)
22. [Breath-Hold Mechanic](#22-breath-hold-mechanic)

### Group G — Developer Test Environment
23. [UniversalTestScreen](#23-universaltestscreen) ← **[v2] New section**

### Appendices
- [How All Systems Connect](#how-all-systems-connect)
- [Complete Package Layout](#complete-package-layout)
- [Implementation Order](#implementation-order)
- [Acceptance Criteria](#acceptance-criteria)

---

# Group 0 — Foundation

---

## 1. Event System

### What it does

An event is something that *happens* — play a sound, start an animation, trigger a chain of other events, set a flag. Events are defined both in code (for reliability) and in JSON (so a level designer can wire up a scenario without recompiling). They support chaining with delays and persistent fired-state per session.

### Package: `event`

```
event/
  GameEvent.java
  EventAction.java
  EventSequence.java
  EventRegistry.java
  EventBus.java
  EventState.java
  EventContext.java
  EventLoader.java
  actions/
    PlaySoundAction.java
    PropagateGraphAction.java
    PlayAnimationAction.java
    SetFlagAction.java
    FireEventAction.java
    WaitAction.java
    ShowSubtitleAction.java
    SetSanityDeltaAction.java
    TriggerJumpScareAction.java
    TransitionLevelAction.java
    CheckpointAction.java
    StoryEventAction.java         ← links to System 14
    LogAction.java
```

#### `GameEvent`

```java
public final class GameEvent {
    public enum RepeatMode { ONCE, REPEATING, ONCE_PER_SESSION }

    private final String id;
    private final String displayName;
    private final List<EventAction> actions;
    private final RepeatMode repeatMode;
    private final float cooldownSeconds;
    private float cooldownRemaining;
    private boolean hasEverFired;

    public boolean canFire(EventState state) {
        if (repeatMode == RepeatMode.ONCE && state.hasFired(id)) return false;
        if (repeatMode == RepeatMode.ONCE_PER_SESSION && hasEverFired) return false;
        return cooldownRemaining <= 0f;
    }

    public void update(float delta) {
        cooldownRemaining = Math.max(0f, cooldownRemaining - delta);
    }

    public void fire(EventContext context, EventState state) {
        if (!canFire(state)) return;
        state.recordFired(id);
        hasEverFired = true;
        cooldownRemaining = cooldownSeconds;
        for (EventAction action : actions) action.execute(context);
    }
}
```

#### `EventContext`

This is the fully merged `EventContext` including all additions from every source document:

```java
public final class EventContext {
    public final Vector3                      triggerPosition;
    public final Vector3                      playerPosition;
    public final float                        elapsedSeconds;
    public final SoundPropagationOrchestrator propagationOrchestrator;
    public final GameAudioSystem              audioSystem;
    public final EventBus                     eventBus;
    public final EventState                   state;
    public final SanitySystem                 sanitySystem;        // System 10
    public final JumpScareDirector            jumpScareDirector;   // System 11
    public final DialogueSystem               dialogueSystem;      // System 12
    public final DebugConsole                 debugConsole;        // System 16
    public final StorySystem                  storySystem;         // System 14  [PATCH: Addendum §5]
    public final InventorySystem              inventorySystem;     // [PATCH: Addendum §5]
    public final FlashlightController         flashlightController;// [PATCH: Addendum §5]
    public final BehaviorSystem               behaviorSystem;      // System 19  [K-Means]
}
```

#### `EventSequence` — chaining with delays

```java
public final class EventSequence {
    public record Step(String eventId, float delaySeconds) {}
    private final List<Step> steps;
    private int   currentStep;
    private float stepTimer;
    private boolean running;

    public void start() { currentStep = 0; stepTimer = 0f; running = true; }

    public void update(float delta, EventBus bus, EventContext context) {
        if (!running || currentStep >= steps.size()) { running = false; return; }
        stepTimer += delta;
        Step step = steps.get(currentStep);
        if (stepTimer >= step.delaySeconds()) {
            bus.fire(step.eventId(), context);
            currentStep++;
            stepTimer = 0f;
        }
    }
}
```

#### JSON event format

```json
[
  {
    "id": "creak-then-groan",
    "displayName": "Door Warning Sequence",
    "repeatMode": "REPEATING",
    "cooldownSeconds": 8.0,
    "actions": [
      { "type": "PROPAGATE_GRAPH", "soundEvent": "AMBIENT_CREAK", "intensity": 0.4 },
      { "type": "WAIT",            "delaySeconds": 1.2 },
      { "type": "PLAY_SOUND",      "soundId": "audio/sfx/env/groan_deep.wav", "volume": 0.6 },
      { "type": "SHOW_SUBTITLE",   "text": "Something groans in the walls...", "duration": 3.0 },
      { "type": "SET_FLAG",        "flagName": "door-warned" },
      { "type": "SET_SANITY",      "delta": -5.0 },
      { "type": "ADVANCE_STORY",   "beatId": "heard-groan" }
    ]
  }
]
```

---

## 2. Sound System — WAV Layer

### What it does

Adds `GameAudioSystem` on top of the existing propagation graph. Handles WAV playback for music, ambience, and SFX. Spatial SFX also feeds into the acoustic propagation graph so the enemy AI hears them.

### Package: `audio`

```
audio/
  GameAudioSystem.java
  AudioChannel.java
  SoundBank.java
  AmbienceTrack.java
  SpatialSfxEmitter.java
  WorldSoundEmitter.java
  AudioChannelConfig.java
```

#### `SoundBank`

```java
public final class SoundBank implements Disposable {
    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final ObjectMap<String, Music> tracks = new ObjectMap<>();

    public Sound sound(String path) {
        Sound cached = sounds.get(path);
        if (cached != null) return cached;
        Sound loaded = Gdx.audio.newSound(Gdx.files.internal(path));
        sounds.put(path, loaded);
        return loaded;
    }

    public Music music(String path) {
        Music cached = tracks.get(path);
        if (cached != null) return cached;
        Music loaded = Gdx.audio.newMusic(Gdx.files.internal(path));
        tracks.put(path, loaded);
        return loaded;
    }

    @Override public void dispose() {
        sounds.values().forEach(Sound::dispose);
        tracks.values().forEach(Music::dispose);
    }
}
```

#### `AmbienceTrack` — seamless looping with crossfade

```java
public final class AmbienceTrack {
    private Music currentTrack;
    private Music nextTrack;
    private float crossfadeRemaining;
    private float crossfadeDuration;
    private float targetVolume;

    public void crossfadeTo(String path, float durationSeconds) {
        nextTrack = soundBank.music(path);
        nextTrack.setLooping(true);
        nextTrack.setVolume(0f);
        nextTrack.play();
        crossfadeDuration = durationSeconds;
        crossfadeRemaining = durationSeconds;
    }

    public void update(float delta) {
        if (nextTrack == null) return;
        crossfadeRemaining -= delta;
        float t = 1f - (crossfadeRemaining / crossfadeDuration);
        if (currentTrack != null) currentTrack.setVolume(targetVolume * (1f - t));
        nextTrack.setVolume(targetVolume * t);
        if (crossfadeRemaining <= 0f) {
            stopCurrent();
            currentTrack = nextTrack;
            nextTrack = null;
        }
    }
}
```

#### `SpatialSfxEmitter` — WAV + propagation graph bridge

**[PATCH: Addendum §1]** — skips graph injection for `AMBIENCE` category to avoid wasting propagation compute on sounds the enemy will always ignore:

```java
public final class SpatialSfxEmitter {
    private static final float MAX_AUDIBLE_DISTANCE = 30f;

    public void playAndPropagate(String wavPath, SoundEvent graphEvent,
                                  Vector3 emitterPos, Vector3 listenerPos,
                                  float baseVolume, float graphIntensity,
                                  float elapsedSeconds,
                                  Function<Vector3, String> nodeResolver) {
        float distance = emitterPos.dst(listenerPos);
        float atten    = Math.max(0f, 1f - (distance / MAX_AUDIBLE_DISTANCE));
        float finalVol = baseVolume * atten * atten;
        float pan      = MathUtils.clamp(emitterPos.x - listenerPos.x, -1f, 1f);
        if (finalVol > 0.01f) soundBank.sound(wavPath).play(finalVol, 1.0f, pan);

        boolean shouldPropagate = graphEvent != null
            && graphEvent.hearingCategory() != HearingCategory.AMBIENCE  // [PATCH]
            && nodeResolver != null;

        if (shouldPropagate) {
            String nodeId = nodeResolver.apply(emitterPos);
            SoundEventData data = new SoundEventData(graphEvent, nodeId,
                emitterPos, graphIntensity, elapsedSeconds);
            propagationOrchestrator.emitSoundEvent(data, elapsedSeconds);
        }
    }
}
```

#### `HearingCategory` — [PATCH: Addendum §1]

```java
/** Controls whether EnemyPerception is eligible to react to a SoundEvent. */
public enum HearingCategory {
    PLAYER_ACTION,    // footsteps, voice, thrown items — enemy always eligible
    SCRIPTED_EVENT,   // alarms, explosions, enemy screams — enemy always eligible
    AMBIENCE          // drips, wind, background hum — enemy never hears
}
```

#### `SoundEvent` — updated to carry `HearingCategory` [PATCH: Addendum §1] [v2: DOOR_BARGE added]

```java
public enum SoundEvent {
    // Player-caused
    FOOTSTEP         (HearingCategory.PLAYER_ACTION,  EventPriority.LOW,      0.3f),
    FOOTSTEP_RUN     (HearingCategory.PLAYER_ACTION,  EventPriority.MEDIUM,   0.6f),
    MIC_VOICE        (HearingCategory.PLAYER_ACTION,  EventPriority.HIGH,     0.8f),
    ITEM_THROW       (HearingCategory.PLAYER_ACTION,  EventPriority.MEDIUM,   0.5f),
    ITEM_IMPACT      (HearingCategory.PLAYER_ACTION,  EventPriority.MEDIUM,   0.6f),
    GLASS_BREAK      (HearingCategory.PLAYER_ACTION,  EventPriority.HIGH,     1.0f),
    DOOR_SLAM        (HearingCategory.PLAYER_ACTION,  EventPriority.HIGH,     0.9f),
    DOOR_CREAK       (HearingCategory.PLAYER_ACTION,  EventPriority.LOW,      0.4f),
    DOOR_BARGE       (HearingCategory.PLAYER_ACTION,  EventPriority.HIGH,     1.1f), // [v2] sprint-barge
    // Scripted / event-driven
    ALARM            (HearingCategory.SCRIPTED_EVENT, EventPriority.CRITICAL, 1.2f),
    EXPLOSION        (HearingCategory.SCRIPTED_EVENT, EventPriority.CRITICAL, 1.5f),
    ENEMY_SCREAM     (HearingCategory.SCRIPTED_EVENT, EventPriority.HIGH,     1.0f),
    PHYSICS_COLLAPSE (HearingCategory.SCRIPTED_EVENT, EventPriority.HIGH,     0.9f),
    // Ambience — enemy CANNOT hear
    AMBIENT_CREAK    (HearingCategory.AMBIENCE, EventPriority.LOW, 0.3f),
    WATER_DRIP       (HearingCategory.AMBIENCE, EventPriority.LOW, 0.1f),
    AMBIENT_HUM      (HearingCategory.AMBIENCE, EventPriority.LOW, 0.1f),
    WIND             (HearingCategory.AMBIENCE, EventPriority.LOW, 0.1f);

    public final HearingCategory hearingCategory;
    public final EventPriority   priority;
    public final float           defaultBaseIntensity;

    SoundEvent(HearingCategory h, EventPriority p, float i) {
        this.hearingCategory      = h;
        this.priority             = p;
        this.defaultBaseIntensity = i;
    }
}
```

#### Mic voice injection [PATCH: Addendum §1]

```java
// In UniversalTestScreen — mic update path:
if (realtimeMicSystem.getCurrentRmsLevel() > VOICE_DETECTION_THRESHOLD) {
    String nodeId = propagationOrchestrator.findNearestNode(playerPosition);
    SoundEventData voiceData = new SoundEventData(
        SoundEvent.MIC_VOICE, nodeId, playerPosition,
        realtimeMicSystem.getNormalizedLevel(), elapsedSeconds
    );
    propagationOrchestrator.emitSoundEvent(voiceData, elapsedSeconds);
}
```

#### Asset convention

```
assets/audio/
  music/
  ambience/
    cave_drip.wav   wind_exterior.wav
    paranoid_bed.wav   methodical_bed.wav   impulsive_bed.wav   panicked_bed.wav
  sfx/
    env/  player/  items/  enemy/  door/  ui/
    footstep/
      concrete/  wood/  metal/  gravel/  water/  carpet/  tile/  dirt/
```

---

## 3. Trigger System

### What it does

Triggers evaluate conditions every frame and fire `GameEvent`s via the `EventBus` when those conditions become true. They are the "condition half" of the event pipeline.

### Package: `trigger`

```
trigger/
  Trigger.java
  TriggerEvaluator.java
  TriggerEvaluationContext.java
  conditions/
    ZoneTrigger.java
    InteractionTrigger.java
    TimerTrigger.java
    StateTrigger.java
    CompoundTrigger.java
```

#### `TriggerEvaluationContext`

```java
public final class TriggerEvaluationContext {
    public final Vector3          playerPosition;
    public final Vector3          playerLookDirection;
    public final boolean          isInteractPressed;
    public final EventState       eventState;
    public final PlayerController playerController;
    public final InventorySystem  inventorySystem;
    public final float            elapsedSeconds;
    public final SanitySystem     sanitySystem;
}
```

#### Condition implementations

```java
// Zone — player inside AABB
public final class ZoneTrigger extends Trigger {
    private final BoundingBox volume;
    public boolean evaluate(TriggerEvaluationContext ctx) {
        return volume.contains(ctx.playerPosition);
    }
}

// State — flag, item count, fire count, sanity
public final class StateTrigger extends Trigger {
    public enum Condition { FLAG_SET, ITEM_IN_INVENTORY, EVENT_FIRED_COUNT, SANITY_BELOW }
    public boolean evaluate(TriggerEvaluationContext ctx) {
        return switch (condition) {
            case FLAG_SET          -> ctx.eventState.getFlag(parameter);
            case EVENT_FIRED_COUNT -> ctx.eventState.fireCount(parameter) >= threshold;
            case ITEM_IN_INVENTORY -> ctx.inventorySystem.countItem(parameter) >= threshold;
            case SANITY_BELOW      -> ctx.sanitySystem.getSanity() < threshold;
        };
    }
}

// Compound — AND / OR of children
public final class CompoundTrigger extends Trigger {
    public enum Mode { AND, OR }
    private final List<Trigger> children;
    private final Mode mode;
    public boolean evaluate(TriggerEvaluationContext ctx) {
        return mode == Mode.AND
            ? children.stream().allMatch(c -> c.evaluate(ctx))
            : children.stream().anyMatch(c -> c.evaluate(ctx));
    }
}
```

#### `TriggerEvaluator.update()` — story gate check [PATCH: Addendum §5]

```java
public void update(float delta, TriggerEvaluationContext evalCtx, EventContext eventCtx) {
    for (Trigger trigger : triggers) {
        trigger.update(delta);
        if (trigger.requiredBeatId() != null) {
            if (!storySystem.hasReached(trigger.requiredChapterId(), trigger.requiredBeatId())) {
                continue;
            }
        }
        if (trigger.evaluate(evalCtx)) {
            trigger.onConditionMet(eventBus, eventCtx);
            storySystem.onZoneEntered(trigger.id(), eventCtx);
        }
    }
}
```

#### JSON trigger format

```json
[
  {
    "id": "enter-server-room",
    "type": "ZONE",
    "targetEvent": "creak-then-groan",
    "requiredChapterId": "prologue",
    "requiredBeatId": "find-keycard",
    "cooldownSeconds": 10.0,
    "volume": { "minX": -5, "minY": 0, "minZ": -5, "maxX": 5, "maxY": 3, "maxZ": 5 }
  }
]
```

---

## 4. Auto-Bullet from `-map.gltf`

### What it does

Detects the `-map.gltf` / `-map.glb` filename convention and automatically builds a `btBvhTriangleMeshShape` from every mesh in the file. No manual collision setup needed.

### Package: `map`

```
map/
  MapCollisionBuilder.java
  MapLoader.java
  LoadedMap.java
  MapDocument.java
  MapObject.java
  MapObjectType.java
  MapDocumentLoader.java
  MapDocumentSerializer.java
```

#### `MapCollisionBuilder` — with debug triangle storage [PATCH: Addendum §2]

```java
public final class MapCollisionBuilder {

    public static BvhResult build(ModelData mapData) {
        btTriangleMesh triMesh = new btTriangleMesh();
        FloatArray debugTris = new FloatArray();
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
                debugTris.addAll(v0.x, v0.y, v0.z,
                                 v1.x, v1.y, v1.z,
                                 v2.x, v2.y, v2.z);
            }
        }

        btBvhTriangleMeshShape shape = new btBvhTriangleMeshShape(triMesh, true, true);
        btDefaultMotionState motionState = new btDefaultMotionState(new Matrix4().idt());
        btRigidBody.btRigidBodyConstructionInfo ci =
            new btRigidBody.btRigidBodyConstructionInfo(0f, motionState, shape, Vector3.Zero);
        btRigidBody body = new btRigidBody(ci);
        ci.dispose();

        return new BvhResult(triMesh, shape, motionState, body, debugTris.toArray());
    }

    public record BvhResult(btTriangleMesh triMesh, btBvhTriangleMeshShape shape,
                             btDefaultMotionState motionState, btRigidBody body,
                             float[] debugTriangles) {
        public void dispose() {
            body.dispose(); motionState.dispose(); shape.dispose(); triMesh.dispose();
        }
    }
}
```

#### `MapObjectType` — extended [PATCH: Addendum §4]

```java
public enum MapObjectType {
    BOX_PROP, CYLINDER_PROP,
    DOOR, FURNITURE,
    TRIGGER_VOLUME, SPAWN_POINT,
    SOUND_EMITTER, LIGHT_POINT,
    DIALOGUE_TRIGGER,
    CHECKPOINT,
    ROOM_BOUNDARY,    // [PATCH] triggers a room transition fade
    GLTF_PROP
}
```

---

## 5. Map Editor — Swing Tool

### What it does

A separate Swing window for placing static props, trigger volumes, spawn points, sound emitters, lights, and room boundaries. Saves a JSON document that `MapDocumentLoader` reads at runtime.

**[PATCH: Addendum §4]** — Single map architecture. There is only **one map** (`resonance-map.gltf`) and one `MapDocument` JSON. The editor manages all objects for all rooms in a single file. The `SceneOutlinePanel` supports filtering by room name via a search box.

**[PATCH: Addendum §2]** — `ModelDebugScreen` now has collision wireframe toggle (K key) and texture fix via `SceneManager`.

### Package: `mapeditor` (in `lwjgl3`)

```
mapeditor/
  MapEditorPanel.java
  ObjectPalette.java
  SceneOutlinePanel.java
  ObjectPropertyPanel.java
  MapEditorIntegration.java
```

### `ModelDebugScreen` — texture fix [PATCH: Addendum §3]

Use gdx-gltf's `SceneManager` for `.gltf` / `.glb` files; keep the legacy `ModelBatch` path for `.g3dj`:

```java
private SceneManager sceneManager;
private ModelBatch   modelBatch;
private boolean      useSceneManager;

// In loadSelectedModel():
String ext = currentModelPath.toLowerCase();
useSceneManager = ext.endsWith(".gltf") || ext.endsWith(".glb");
if (useSceneManager) {
    SceneAsset sceneAsset = ext.endsWith(".glb")
        ? new GLBLoader().load(Gdx.files.internal(currentModelPath), true)
        : new GLTFLoader().load(Gdx.files.internal(currentModelPath), true);
    sceneManager.removeScene(currentGltfScene);
    currentGltfScene = new Scene(sceneAsset.scene);
    sceneManager.addScene(currentGltfScene);
    if (currentSceneAsset != null) currentSceneAsset.dispose();
    currentSceneAsset = sceneAsset;
    if (MapLoader.isMapFile(currentModelPath)) {
        BvhResult bvh = MapCollisionBuilder.build(loadedModelData);
        collisionDebugTriangles = bvh.debugTriangles();
    }
}
```

### `CollisionWireframeRenderer` — K key toggle [PATCH: Addendum §2]

```java
public final class CollisionWireframeRenderer {
    private static final Color WIREFRAME_COLOR = new Color(0f, 1f, 0.2f, 0.55f);

    public void render(ShapeRenderer shapes, PerspectiveCamera camera,
                       float[] triangles, boolean enabled) {
        if (!enabled || triangles == null) return;
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < triangles.length - 8; i += 9) {
            shapes.setColor(WIREFRAME_COLOR);
            shapes.line(triangles[i],   triangles[i+1], triangles[i+2],
                        triangles[i+3], triangles[i+4], triangles[i+5]);
            shapes.line(triangles[i+3], triangles[i+4], triangles[i+5],
                        triangles[i+6], triangles[i+7], triangles[i+8]);
            shapes.line(triangles[i+6], triangles[i+7], triangles[i+8],
                        triangles[i],   triangles[i+1], triangles[i+2]);
        }
        shapes.end();
    }
}
```

> **Performance note:** For meshes with > 30 000 triangles, build a reusable `Mesh` with `GL_LINES` topology instead of immediate-mode `ShapeRenderer` calls.

### Map JSON format — single map [PATCH: Addendum §4]

```json
{
  "mapName": "Resonance — Full Map",
  "gltfMapPath": "models/resonance-map.gltf",
  "objects": [
    {
      "id": "spawn-player",
      "type": "SPAWN_POINT",
      "position": { "x": 0.0, "y": 0.0, "z": 0.0 },
      "properties": { "label": "default" }
    },
    {
      "id": "enter-server-room-boundary",
      "type": "ROOM_BOUNDARY",
      "position": { "x": 12.0, "y": 1.5, "z": 0.0 },
      "properties": {
        "halfWidth": "0.3", "halfHeight": "2.0", "halfDepth": "4.0",
        "roomName": "Server Room",
        "onEnterEvent": "enter-server-room"
      }
    },
    {
      "id": "checkpoint-server-room",
      "type": "CHECKPOINT",
      "position": { "x": 8.0, "y": 0.0, "z": -3.0 },
      "properties": { "checkpointId": "server-room-cleared" }
    }
  ]
}
```

---

# Group A — Core Gameplay Loops

---

## 6. Enemy AI System

### What it does

Gives the enemy the ability to patrol the map, hear sounds from the propagation graph, see the player via vision cones, investigate disturbances, chase, and attack.

### Package: `enemy`

```
enemy/
  EnemyController.java
  EnemyStateMachine.java
  EnemyState.java
  EnemyPerception.java
  EnemyNavigator.java
  EnemyAnimator.java
  NavMesh.java
  NavMeshBuilder.java
  states/
    IdleState.java
    PatrolState.java
    InvestigateState.java
    TrailFollowState.java
    ChaseState.java
    AttackState.java
    StunnedState.java
  tracking/
    FootprintTrail.java
    Footprint.java
    FootprintEmitter.java
    FootprintDetector.java
```

#### State machine transitions

```
IDLE ──(patrol timer)──────────► PATROL
PATROL ──(sound heard)──────────► INVESTIGATE
PATROL ──(player seen)──────────► CHASE
INVESTIGATE ──(prints found)────► TRAIL_FOLLOW
INVESTIGATE ──(nothing found)───► PATROL
INVESTIGATE ──(player seen)─────► CHASE
TRAIL_FOLLOW ──(player seen)────► CHASE
TRAIL_FOLLOW ──(trail cold 5s)──► INVESTIGATE
CHASE ──(player lost > 5s)──────► INVESTIGATE
CHASE ──(within attack range)───► ATTACK
ATTACK ──(done)─────────────────► CHASE
ANY ──(stun item hit)───────────► STUNNED
STUNNED ──(timer done)──────────► PATROL
```

#### `EnemyPerception` — with hearing filter [PATCH: Addendum §1]

```java
public final class EnemyPerception {
    private static final float VISION_FOV_DEGREES    = 70f;
    private static final float VISION_RANGE          = 12f;
    private static final float MIN_AUDIBLE_INTENSITY = 0.18f;

    public void onSoundHeard(SoundEventData data) {
        if (data.soundEvent().hearingCategory() == HearingCategory.AMBIENCE) return;
        float effectiveIntensity = data.baseIntensity() * breathSystem.enemyHearingMult();
        if (effectiveIntensity < MIN_AUDIBLE_INTENSITY) return;
        lastHeardPosition  = data.worldPosition();
        lastHeardIntensity = effectiveIntensity;
        hasUnhandledSound  = true;
    }

    public boolean canSeePlayer(Vector3 enemyPos, Vector3 enemyForward,
                                 Vector3 playerPos, btDiscreteDynamicsWorld physicsWorld) {
        Vector3 toPlayer = new Vector3(playerPos).sub(enemyPos);
        float dist = toPlayer.len();
        if (dist > VISION_RANGE) return false;
        toPlayer.nor();
        float angle = MathUtils.radiansToDegrees *
            (float) Math.acos(MathUtils.clamp(enemyForward.dot(toPlayer), -1f, 1f));
        if (angle > VISION_FOV_DEGREES / 2f) return false;
        ClosestRayResultCallback cb = new ClosestRayResultCallback(enemyPos, playerPos);
        physicsWorld.rayTest(enemyPos, playerPos, cb);
        boolean clear = !cb.hasHit();
        cb.dispose();
        return clear;
    }
}
```

#### EnemyController behavior-driven setters [K-Means]

```java
public void setInvestigationDelay(float delay)    { investigationDelay = delay; }
public void setPatrolSpeedMultiplier(float mult)  { patrolSpeedMult = mult; }
public void setVisionFov(float fovDeg)            { perception.setVisionFov(fovDeg); }
public void setHearingRangeMultiplier(float mult) { perception.setHearingRangeMult(mult); }
public void setFakeOutProbability(float prob)     { fakeOutProbability = prob; }
```

---

## 7. Door & Interactable System — Physics Drag Model

**[v2] This section is a complete rewrite of the door interaction model. The old single-keypress "press F to open" model is replaced by a physics-based manual-drag system.**

### Design intent

The player must physically drag the door open by holding LMB on the door knob and moving the mouse. How aggressively they drag directly determines the creak volume and pitch — a slow careful drag is quiet; a fast aggressive drag is loud. A sprinting player can barge straight through, slamming it open with maximum noise. Both behaviors feed the K-Means classifier to label the player as careful or aggressive.

### Interaction flow

```
1. Player looks at door → raycast hits DoorKnobCollider (small sphere at handle)
2. HUD shows "Hold [LMB] to grab"
3. Player holds LMB → DoorGrabInteraction enters GRABBING state
4. Player moves mouse left/right → drives door angular velocity in real time
5. Creak plays continuously, volume/pitch scaled to current angular velocity
6. Player releases LMB → door stops; physics damping decelerates it to rest
   — OR —
   Player is sprinting AND walks into DoorBargeZone → BARGE: door slams to 90° instantly
```

### Package: `interactable`

```
interactable/
  Interactable.java
  InteractableRegistry.java
  door/
    DoorController.java         ← [v2] physics-driven angle model, barge support
    DoorKnobCollider.java       ← [v2] new: small btSphereShape at handle position
    DoorGrabInteraction.java    ← [v2] new: grab/drag state machine
    DoorBargeDetector.java      ← [v2] new: sprint-collision barge detection
    DoorCreakSystem.java        ← [v2] updated: continuous creak during drag
    DoorLockState.java
  objects/
    CabinetController.java
    DrawerController.java
    SwitchController.java
    KeycardPickup.java
```

---

### `DoorKnobCollider` [v2]

A small `btSphereShape` (radius 0.12 m) positioned at the door handle. Registered separately in the physics world as a sensor (mass = 0, no response flags). The `RaycastInteractionSystem` tests hits against this body first before checking the door panel body.

```java
public final class DoorKnobCollider implements Disposable {
    private static final float KNOB_RADIUS = 0.12f;

    private final btSphereShape      shape;
    private final btDefaultMotionState motionState;
    private final btRigidBody        body;

    public DoorKnobCollider(Vector3 knobWorldPosition, btDiscreteDynamicsWorld world) {
        shape       = new btSphereShape(KNOB_RADIUS);
        motionState = new btDefaultMotionState(new Matrix4().setToTranslation(knobWorldPosition));
        btRigidBody.btRigidBodyConstructionInfo ci =
            new btRigidBody.btRigidBodyConstructionInfo(0f, motionState, shape, Vector3.Zero);
        body = new btRigidBody(ci);
        body.setCollisionFlags(body.getCollisionFlags()
            | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        ci.dispose();
        world.addRigidBody(body);
    }

    /** Called every frame to follow the door hinge. */
    public void syncPosition(Vector3 hingePos, float currentDoorAngleDeg) {
        // Rotate knob position around hinge by currentDoorAngleDeg
        // then set motionState world transform
    }

    public btRigidBody body() { return body; }

    @Override public void dispose() {
        body.dispose(); motionState.dispose(); shape.dispose();
    }
}
```

---

### `DoorGrabInteraction` [v2]

Manages the grab/drag state machine. Called from the screen's `render()` loop. Receives raw mouse delta from LibGDX `Gdx.input.getDeltaX()`.

```java
public final class DoorGrabInteraction {

    /** Degrees of door rotation per 1.0 of normalized mouse X delta. */
    private static final float DRAG_SENSITIVITY     = 120f;
    /** Peak angular velocity (deg/s) used for K-Means normalization. */
    private static final float MAX_ANGULAR_VELOCITY = 360f;
    /** Angular deceleration applied after LMB release (deg/s²). */
    private static final float PHYSICS_DAMPING      = 240f;
    /** Door open limit in degrees. */
    private static final float MAX_OPEN_ANGLE       = 90f;

    private enum GrabState { IDLE, GRABBING, COASTING }

    private GrabState   state          = GrabState.IDLE;
    private float       angularVelocity = 0f; // deg/s, signed
    private DoorController grabbedDoor = null;

    /**
     * Call every frame from the screen update loop.
     *
     * @param delta       frame delta seconds
     * @param mouseXDelta raw pixel delta from Gdx.input.getDeltaX() — positive = drag open
     * @param lmbDown     whether LMB is currently held
     * @param hoveredDoor door whose knob is currently under the crosshair (null if none)
     * @param ctx         event context for audio + behavior
     */
    public void update(float delta, float mouseXDelta, boolean lmbDown,
                       DoorController hoveredDoor, DoorCreakSystem creak,
                       Vector3 doorPos, EventContext ctx) {

        switch (state) {
            case IDLE -> {
                if (lmbDown && hoveredDoor != null && hoveredDoor.canInteract()) {
                    grabbedDoor     = hoveredDoor;
                    angularVelocity = 0f;
                    state           = GrabState.GRABBING;
                    grabbedDoor.onGrabStart(ctx);
                }
            }
            case GRABBING -> {
                if (!lmbDown) {
                    state = GrabState.COASTING;
                    return;
                }
                // Convert pixel delta → angular velocity (screen-rate independent)
                float screenNorm  = mouseXDelta / Gdx.graphics.getWidth();
                angularVelocity   = screenNorm * DRAG_SENSITIVITY / delta;
                angularVelocity   = MathUtils.clamp(angularVelocity,
                                        -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);

                // Drive door angle
                float angleDelta = angularVelocity * delta;
                grabbedDoor.applyAngleDelta(angleDelta);

                // Continuous creak proportional to angular speed
                float normSpeed = Math.abs(angularVelocity) / MAX_ANGULAR_VELOCITY;
                creak.updateDrag(grabbedDoor.getId(), normSpeed,
                                 doorPos, ctx.playerPosition, delta, ctx);

                // Feed K-Means: sampled every frame, averaged in tracker
                ctx.behaviorSystem.tracker().onDoorDragged(normSpeed);
            }
            case COASTING -> {
                // Physics damping: decelerate to zero
                float sign = Math.signum(angularVelocity);
                angularVelocity -= sign * PHYSICS_DAMPING * delta;
                if (sign != Math.signum(angularVelocity)) {
                    angularVelocity = 0f;
                    state           = GrabState.IDLE;
                    grabbedDoor     = null;
                    return;
                }
                grabbedDoor.applyAngleDelta(angularVelocity * delta);
                float normSpeed = Math.abs(angularVelocity) / MAX_ANGULAR_VELOCITY;
                creak.updateDrag(grabbedDoor.getId(), normSpeed,
                                 doorPos, ctx.playerPosition, delta, ctx);
            }
        }
    }

    public boolean isGrabbing()           { return state == GrabState.GRABBING; }
    public DoorController grabbedDoor()   { return grabbedDoor; }
}
```

---

### `DoorCreakSystem` — updated for continuous drag [v2]

Replaces the old single-call creak with a continuous drag version **and** keeps the legacy one-shot API for scripted events.

```java
public final class DoorCreakSystem {

    // Thresholds for selecting WAV variant
    private static final float CREAK_SLOW_LIMIT   = 0.33f;
    private static final float CREAK_MEDIUM_LIMIT = 0.66f;

    // Cooldown between creak sound repetitions (shorter at high speed)
    private static final float CREAK_COOLDOWN_SLOW   = 0.55f;
    private static final float CREAK_COOLDOWN_FAST   = 0.10f;

    // Minimum normalized speed before any creak plays (avoids micro-creak at near-zero drag)
    private static final float CREAK_SPEED_FLOOR = 0.05f;

    private final Map<String, Float> cooldowns = new HashMap<>();

    /**
     * Called every frame while door is being dragged.
     *
     * @param normSpeed 0..1 normalized angular velocity (0 = still, 1 = max)
     */
    public void updateDrag(String doorId, float normSpeed,
                           Vector3 doorPos, Vector3 playerPos,
                           float delta, EventContext ctx) {
        cooldowns.merge(doorId, -delta, Float::sum);
        if (cooldowns.getOrDefault(doorId, 0f) > 0f) return;
        if (normSpeed < CREAK_SPEED_FLOOR) return;

        float volume  = MathUtils.lerp(0.10f, 1.0f, normSpeed);
        float pitch   = MathUtils.lerp(0.75f, 1.5f, normSpeed);
        String wav    = normSpeed < CREAK_SLOW_LIMIT   ? "audio/sfx/door/creak_slow.wav"
                      : normSpeed < CREAK_MEDIUM_LIMIT ? "audio/sfx/door/creak_medium.wav"
                                                       : "audio/sfx/door/creak_fast.wav";

        ctx.audioSystem.playSpatialSfxPitched(wav, SoundEvent.DOOR_CREAK,
                                               doorPos, volume, pitch, normSpeed);
        // Feed behavior tracker with the peak speed seen this window
        ctx.behaviorSystem.tracker().onDoorOpened(normSpeed);

        float cooldown = MathUtils.lerp(CREAK_COOLDOWN_SLOW, CREAK_COOLDOWN_FAST, normSpeed);
        cooldowns.put(doorId, cooldown);
    }

    /**
     * Legacy one-shot creak for scripted events (door slams, barge, etc.)
     */
    public void fireOneShot(String doorId, float normSpeed,
                             Vector3 doorPos, Vector3 playerPos,
                             EventContext ctx) {
        updateDrag(doorId, normSpeed, doorPos, playerPos, 0f, ctx);
    }
}
```

> **Audio note:** `GameAudioSystem.playSpatialSfxPitched()` is a new overload that accepts a `pitch` multiplier and forwards it to `Sound.play(volume, pitch, pan)`. Add this overload when implementing the audio system.

---

### `DoorBargeDetector` [v2]

Detects when a sprinting player collides with a door's barge zone and triggers an instant slam-open. Checked every frame in `DoorController.update()`.

```java
public final class DoorBargeDetector {

    /** Player horizontal speed (m/s) required to trigger a barge. */
    private static final float BARGE_SPEED_THRESHOLD = 4.8f;
    /** Half-extents of the thin box trigger in front of the door panel. */
    private static final float BARGE_ZONE_HALF_W = 0.6f;
    private static final float BARGE_ZONE_HALF_D = 0.4f;
    private static final float BARGE_ZONE_HALF_H = 1.2f;

    /**
     * @param player   current player state
     * @param door     the door to test against
     * @param ctx      event context
     * @return true if a barge was triggered this frame
     */
    public boolean check(PlayerController player, DoorController door, EventContext ctx) {
        if (door.isFullyOpen() || door.lockState() != DoorLockState.UNLOCKED) return false;
        if (!player.isSprinting()) return false;
        if (player.getHorizontalSpeed() < BARGE_SPEED_THRESHOLD) return false;
        if (!isPlayerInBargeZone(player.getPosition(), door)) return false;

        door.bargeOpen(ctx);
        return true;
    }

    private boolean isPlayerInBargeZone(Vector3 playerPos, DoorController door) {
        // AABB check: thin box on the latch side of the door panel
        // Computed from door hinge position + current door angle
        BoundingBox zone = door.computeBargeZone(
            BARGE_ZONE_HALF_W, BARGE_ZONE_HALF_H, BARGE_ZONE_HALF_D);
        return zone.contains(playerPos);
    }
}
```

---

### `DoorController` — updated [v2]

```java
public final class DoorController implements Interactable, Disposable {

    private static final float MAX_OPEN_ANGLE  = 90f;
    private static final float MIN_OPEN_ANGLE  = 0f;

    private final String       id;
    private final Vector3      hingePosition;
    private final DoorLockState lockState;
    private       float        currentAngle   = 0f;   // degrees from closed
    private       StoryGate    storyGate;             // null = no gate
    private       boolean      grabHappened   = false;

    // --- [v2] Drag API ---

    /** Called by DoorGrabInteraction when LMB first pressed on knob. */
    public void onGrabStart(EventContext ctx) {
        grabHappened = true;
        // Story gate check on first grab
        if (storyGate != null && !storyGate.isOpen(ctx)) {
            // Cannot grab a gated locked door; play locked click
            ctx.audioSystem.playSfx("audio/sfx/door/locked.wav", hingePosition, 0.8f);
        }
        ctx.storySystem.onInteraction(id, ctx);
    }

    /** Called every frame while dragging. Clamps to [0, MAX_OPEN_ANGLE]. */
    public void applyAngleDelta(float angleDelta) {
        currentAngle = MathUtils.clamp(currentAngle + angleDelta, MIN_OPEN_ANGLE, MAX_OPEN_ANGLE);
    }

    // --- [v2] Barge API ---

    /**
     * Sprint barge — instantly slams the door to fully open.
     * Plays DOOR_BARGE (louder than any drag creak). Feeds K-Means at max aggression.
     */
    public void bargeOpen(EventContext ctx) {
        if (lockState != DoorLockState.UNLOCKED) return;
        currentAngle = MAX_OPEN_ANGLE;
        // Two-layer sound: structural boom + creak at max speed
        ctx.audioSystem.playSpatialSfxPitched(
            "audio/sfx/door/creak_fast.wav",
            SoundEvent.DOOR_BARGE,
            hingePosition, 1.0f, 1.6f, 1.0f);
        ctx.audioSystem.playSfx("audio/sfx/door/slam_impact.wav", hingePosition, 1.0f);
        // K-Means: record max aggression score (1.0)
        ctx.behaviorSystem.tracker().onDoorOpened(1.0f);
        ctx.storySystem.onInteraction(id, ctx);
    }

    // --- Geometry helpers ---

    public boolean isFullyOpen()  { return currentAngle >= MAX_OPEN_ANGLE - 0.5f; }
    public boolean isFullyClosed(){ return currentAngle <= MIN_OPEN_ANGLE + 0.5f; }
    public float   currentAngle() { return currentAngle; }

    /** Returns thin AABB on the latch side for barge detection. */
    public BoundingBox computeBargeZone(float hw, float hh, float hd) {
        // Rotate a reference point by currentAngle around hinge, then build AABB
        // Implementation detail — standard Matrix4 + BoundingBox arithmetic
        throw new UnsupportedOperationException("implement in terms of Matrix4.rotate");
    }

    /** Knob world position (rotates with door angle). */
    public Vector3 knobWorldPosition() {
        // hingePosition + rotated offset (typically ~0.85m from hinge along panel)
        throw new UnsupportedOperationException("implement rotation from hinge");
    }

    // --- Interactable ---

    @Override
    public boolean canInteract(TriggerEvaluationContext ctx) {
        if (storyGate != null && !storyGate.isOpen(eventContext)) return false;
        if (lockState == DoorLockState.LOCKED) return false;
        if (lockState == DoorLockState.KEYCARD_REQUIRED)
            return ctx.inventorySystem.hasItem(requiredKeycardId);
        return true;
    }

    @Override
    public String promptText() {
        return switch (lockState) {
            case UNLOCKED         -> "Hold [LMB] to grab";
            case LOCKED           -> "Locked";
            case KEYCARD_REQUIRED -> "Requires keycard";
        };
    }

    // No onInteract() — doors are driven by DoorGrabInteraction, not keypress.
    @Override public void onInteract(EventContext ctx) { /* intentionally empty */ }

    @Override public String id() { return id; }
    @Override public void dispose() { /* knob collider disposed separately */ }
}
```

---

### K-Means integration [v2]

`DoorOpenSpeed` is feature #7 in `BehaviorSample`. The tracker now accumulates the **peak normalized angular velocity** seen across all door interactions this sampling window:

```java
// PlayerBehaviorTracker additions:

/** Called every frame while dragging (normSpeed 0..1). */
public void onDoorDragged(float normSpeed) {
    peakDoorSpeedThisWindow = Math.max(peakDoorSpeedThisWindow, normSpeed);
}

/** Called once at barge (always 1.0). */
public void onDoorOpened(float normSpeed) {
    peakDoorSpeedThisWindow = Math.max(peakDoorSpeedThisWindow, normSpeed);
}

// BehaviorSample creation: averageDoorOpenSpeed = peakDoorSpeedThisWindow
```

Centroid values for door open speed (0 = silent careful drag, 1 = full barge):

| Archetype | Door open speed |
|-----------|----------------|
| Methodical | 0.20 (slow careful pull) |
| Paranoid | 0.30 (cautious, listens first) |
| Impulsive | 0.75 (fast shove) |
| Panicked | 0.85 (near-barge at all times) |

---

### Audio assets for doors

```
audio/sfx/door/
  creak_slow.wav        — quiet, low pitch, deliberate
  creak_medium.wav      — moderate speed
  creak_fast.wav        — fast aggressive drag
  slam_impact.wav       — structural thud used in barge (layered over creak_fast)
  locked.wav            — short metal click for locked door attempt
```

---

## 8. Raycast Interaction System

### What it does

Every frame, cast a ray from the camera center forward. If the ray hits a **`DoorKnobCollider`** or a registered `Interactable` within `MAX_INTERACT_DISTANCE`, show a prompt on the HUD. For doors, LMB is consumed by `DoorGrabInteraction` instead of the F key.

### Package: `interaction`

```
interaction/
  RaycastInteractionSystem.java
  InteractionPromptRenderer.java
  InteractionResult.java
```

```java
public final class RaycastInteractionSystem {
    private static final float MAX_INTERACT_DISTANCE = 2.5f;

    /**
     * Returns the interactable under the crosshair, or NONE.
     * For DoorKnobColliders, returns the owning DoorController.
     */
    public InteractionResult update(Vector3 rayOrigin, Vector3 rayDirection,
                                     TriggerEvaluationContext evalCtx) {
        Vector3 rayEnd = new Vector3(rayOrigin).add(
            new Vector3(rayDirection).scl(MAX_INTERACT_DISTANCE));
        ClosestRayResultCallback cb = new ClosestRayResultCallback(rayOrigin, rayEnd);
        physicsWorld.rayTest(rayOrigin, rayEnd, cb);
        if (!cb.hasHit()) { cb.dispose(); return InteractionResult.NONE; }
        Vector3 hitPoint = cb.getHitPointWorld(new Vector3());
        cb.dispose();

        // Check if we hit a door knob first
        DoorController doorFromKnob = knobRegistry.findByHitBody(cb.getCollisionObject());
        if (doorFromKnob != null && doorFromKnob.canInteract(evalCtx)) {
            return new InteractionResult(doorFromKnob, hitPoint, doorFromKnob.promptText());
        }

        Interactable nearest = interactableRegistry.findNearest(hitPoint, 0.5f);
        if (nearest == null || !nearest.canInteract(evalCtx)) return InteractionResult.NONE;
        return new InteractionResult(nearest, hitPoint, nearest.promptText());
    }
}
```

---

# Group B — Horror Atmosphere

---

## 9. Dynamic Lighting System

### What it does

Manages point lights: flickering lights, lights that react to sound events, breakable lights, and the player's flashlight with battery drain.

### Package: `lighting`

```
lighting/
  LightManager.java
  GameLight.java
  FlickerController.java
  FlashlightController.java
  LightReactorListener.java
```

#### `FlashlightController`

```java
public final class FlashlightController {
    private static final float MAX_BATTERY      = 100f;
    private static final float DRAIN_PER_SECOND = 1.5f;
    private static final float LOW_BATTERY      = 20f;
    private float battery = MAX_BATTERY;
    private boolean on    = false;
    private final GameLight flashlight;

    public void toggle() {
        on = !on;
        behaviorSystem.tracker().onFlashlightToggled();
    }

    public float getBatteryPercent() { return battery / MAX_BATTERY; }
    public boolean isOn() { return on; }
}
```

#### Shader integration

```glsl
// sound_pulse.frag
uniform vec3  u_lightPos[8];
uniform vec4  u_lightColor[8];
uniform float u_lightRadius[8];
uniform float u_lightIntensity[8];
uniform int   u_lightCount;
```

#### `LightManager` — K-Means ambient multiplier [K-Means]

```java
public void setAmbientMultiplier(float mult) { currentAmbientMult = mult; }
```

---

## 10. Sanity / Fear System

### What it does

A `sanity` meter (0–100) that decreases from darkness, enemy proximity, and scripted events. Drives visual distortions, audio glitches, hallucinations, and footprint visibility.

### Package: `sanity`

```
sanity/
  SanitySystem.java
  SanityDrainSource.java
  SanityEffect.java
  drains/
    DarknessPresenceDrain.java
    EnemyProximityDrain.java
    EventDrain.java
  effects/
    ScreenVignetteEffect.java
    ShaderDistortionEffect.java
    AudioGlitchEffect.java
    HallucinationEffect.java
  hallucination/
    HallucinationDirector.java
    HallucinationEvent.java
    HallucinationType.java
    AudioHallucination.java
    VisualHallucination.java
```

#### `SanitySystem` — K-Means drain multiplier + behavior delta [K-Means]

```java
public void setDrainMultiplier(float mult)     { drainMultiplier = mult; }
public void applyDelta(float delta)            { sanity = MathUtils.clamp(sanity + delta, 0f, MAX_SANITY); }
public float getLastDeltaThisFrame()           { return lastFrameDelta; }
```

#### Sanity tiers and hallucination thresholds

| Sanity | Effects |
|--------|---------|
| 75–100 | None |
| 50–75 | Subtle vignette |
| 25–50 | Vignette intensifies; occasional static |
| 10–25 | Chromatic aberration; whispers |
| 0–10 | Full distortion; visual hallucination flicker |

Hallucination thresholds are archetype-modified [K-Means]:

```java
// PARANOID: start at 60; METHODICAL: 50; IMPULSIVE/PANICKED: 40
private static final float[] HALLUCINATION_THRESHOLD = { 60f, 50f, 40f, 40f };
```

---

## 11. Jump Scare Director

### What it does

Tracks tension and decides when to trigger jump scares. Prevents overuse with minimum intervals. Archetype-aware via K-Means blend weights.

### Package: `scare`

```
scare/
  JumpScareDirector.java
  JumpScare.java
  ScareType.java
  scripted/
    AudioOnlyScare.java
    FlashScare.java
    EnemyAppearScare.java
    HallucinationScare.java
    EnvironmentScare.java
```

#### K-Means setters [K-Means]

```java
public void setMinInterval(float secs)          { minScareInterval = secs; }
public void setTensionThreshold(float t)        { tensionThreshold = t; }
public void setScareTypeWeights(float[] weights){ scareTypeWeights = weights; }
```

---

# Group C — Narrative & Progression

---

## 12. Dialogue & Subtitle System

### What it does

Plays voice lines tied to events, shows on-screen subtitles with timing, and queues multiple lines without overlap.

### Package: `dialogue`

```
dialogue/
  DialogueSystem.java
  DialogueLine.java
  DialogueSequence.java
  DialogueLoader.java
  SubtitleRenderer.java
```

#### Archetype-conditional dialogue [K-Means]

```java
String line = switch (ctx.behaviorSystem.currentArchetype()) {
    case PARANOID    -> "I keep hearing things... it's getting to me.";
    case METHODICAL  -> "There has to be a pattern to this. I just need to think.";
    case IMPULSIVE   -> "I need to get out of here. Now.";
    case PANICKED    -> "I can't breathe. I can't breathe.";
};
ctx.dialogueSystem.showSubtitle(line, 3.5f);
```

---

## 13. Save / Checkpoint System

**[PATCH: Addendum §4]** — `SaveData.mapName` is always `"resonance-full-map"`.

### Package: `save`

```
save/
  SaveSystem.java
  SaveData.java
  CheckpointManager.java
  SaveSlot.java
```

```java
public final class SaveData {
    public String   saveSlotId;
    public String   mapName       = "resonance-full-map";
    public String   checkpointId;
    public Vector3  playerPosition   = new Vector3();
    public float    playerSanity     = 100f;
    public float    flashlightBattery = 100f;
    public Map<String, Integer> eventFireCounts = new LinkedHashMap<>();
    public Map<String, String>  inventoryItems  = new LinkedHashMap<>();
    public long     savedAtMillis;
    public String   savedAtDisplay;
}
```

---

## 14. Story System

**[PATCH: Addendum §5]** — Enforces linear narrative progression above the Event System.

### Package: `story`

```
story/
  StorySystem.java
  StoryChapter.java
  StoryBeat.java
  StoryBeatCondition.java
  StoryLoader.java
  StoryGate.java
  StoryEventAction.java
```

#### `StoryBeat`

```java
public final class StoryBeat {
    public enum CompletionMode {
        ON_EVENT_FIRE, ON_FLAG_SET, ON_INTERACTION, ON_ZONE_ENTRY, AUTOMATIC
    }
    public boolean tryComplete(String parameter, EventState eventState) {
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
}
```

#### `StoryGate`

```java
public final class StoryGate {
    public boolean isOpen(EventContext ctx) {
        if (storySystem.hasReached(requiredChapterId, requiredBeatId)) return true;
        if (blockedMessage != null) ctx.dialogueSystem.showSubtitle(blockedMessage, 2.5f);
        return false;
    }
}
```

#### Story JSON — `assets/story/story.json`

```json
{
  "chapters": [
    {
      "id": "prologue",
      "displayName": "Prologue — Arrival",
      "beats": [
        { "id": "enter-building",  "completionMode": "ON_ZONE_ENTRY",   "completionParameter": "zone-building-entrance", "onCompleteEvent": "play-intro-monologue", "blocking": false },
        { "id": "read-first-note", "completionMode": "ON_INTERACTION",  "completionParameter": "note-reception-desk",    "onCompleteEvent": "creak-then-groan",     "blocking": true  },
        { "id": "find-keycard",    "completionMode": "ON_FLAG_SET",     "completionParameter": "picked-up-keycard-red",                                             "blocking": true  }
      ]
    }
  ]
}
```

---

## 15. Room Transition System

**[PATCH: Addendum §4]** — Since there is only one map, "transitions" are just a fade-to-black + fade-in when the player crosses a `ROOM_BOUNDARY` trigger.

### Package: `transition`

```
transition/
  RoomTransitionSystem.java
  FadeOverlay.java
```

```java
public final class RoomTransitionSystem {
    private static final float FADE_DURATION = 1.2f;
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
            if (fadeAlpha >= 1f) { fadingOut = false; fadingIn = true; dialogue.showSubtitle(pendingRoomName, 2.0f); }
        }
        if (fadingIn) {
            fadeAlpha = Math.max(0f, fadeAlpha - delta / FADE_DURATION);
            if (fadeAlpha <= 0f) fadingIn = false;
        }
    }

    public float fadeAlpha() { return fadeAlpha; }
}
```

---

# Group D — Developer Tools

---

## 16. In-Game Debug Console

A text input overlay (press backtick) that accepts typed commands at runtime.

### Package: `debug`

```
debug/
  DebugConsole.java
  ConsoleRenderer.java
  ConsoleCommand.java
  ConsoleRegistry.java
  built_in/
    TeleportCommand.java
    FireEventCommand.java
    SetFlagCommand.java
    SetSanityCommand.java
    SpawnEnemyCommand.java
    GodModeCommand.java
    GiveItemCommand.java
    ListEventsCommand.java
    ReloadMapCommand.java
    PrintStateCommand.java
    BeatCommand.java
    ChapterCommand.java
    StoryStatusCommand.java
    BehaviorCommand.java
    HallucinateCommand.java
    FlyCommand.java             ← [v2] toggles FlyModeController in UniversalTestScreen
```

#### Built-in commands (full list)

| Command | Example | Effect |
|---------|---------|--------|
| `tp` | `tp 0 0 0` | Teleport player |
| `fire` | `fire alarm-trigger` | Fire event by id |
| `flag` | `flag door-warned true` | Set/clear event flag |
| `sanity` | `sanity 25` | Set sanity value |
| `spawn` | `spawn enemy 0 0 5` | Spawn enemy |
| `god` | `god` | Toggle god mode |
| `give` | `give keycard-red` | Add item |
| `events` | `events` | List all event ids |
| `reload` | `reload` | Reload map document |
| `state` | `state` | Print all flags |
| `story` | `story` | Current chapter + beat |
| `beat` | `beat read-first-note` | Force-complete a beat |
| `chapter` | `chapter chapter-1` | Force-start a chapter |
| `behavior` | `behavior` | Current archetype + blend weights |
| `behavior force` | `behavior force PARANOID` | Force archetype |
| `behavior reset` | `behavior reset` | Clear all samples |
| `hallucinate` | `hallucinate FAKE_FOOTSTEP` | Force a hallucination |
| `fly` | `fly` | Toggle fly mode (test map only) |
| `fly speed` | `fly speed 20` | Set fly speed m/s |

---

## 17. Settings System

### Package: `settings`

```
settings/
  SettingsSystem.java
  SettingsData.java
  KeybindRegistry.java
```

```java
public static SettingsData defaults() {
    SettingsData d = new SettingsData();
    d.keybinds.put("INTERACT",    Input.Keys.F);
    d.keybinds.put("CROUCH",      Input.Keys.C);
    d.keybinds.put("SPRINT",      Input.Keys.SHIFT_LEFT);
    d.keybinds.put("FLASHLIGHT",  Input.Keys.L);
    d.keybinds.put("CONSOLE",     Input.Keys.GRAVE);
    d.keybinds.put("HOLD_BREATH", Input.Keys.ALT_LEFT);
    d.keybinds.put("FLY_TOGGLE",  Input.Keys.NUM_0);   // [v2] test map only; not exposed in final game
    return d;
}
```

> **Final game note:** `FLY_TOGGLE` is registered in the keybind table but the `FlyModeController` itself is only instantiated inside `UniversalTestScreen`. In production `GameScreen` it never exists, so pressing `0` does nothing.

---

## 18. Footstep Material System

### Package: `footstep`

```
footstep/
  FootstepSystem.java
  FootstepSoundSet.java
  SurfaceMaterial.java
```

```java
public enum SurfaceMaterial {
    CONCRETE, WOOD, METAL, GRAVEL, WATER, CARPET, TILE, DIRT
}
```

#### Integration with breath system

```java
// FootstepSystem.update():
float finalVolume = volume * breathSystem.footstepVolumeMult();
```

---

# Group E — Adaptive AI

---

## 19. Player Behaviour Classification (K-Means)

### What it does

Silently watches how the player behaves, classifies them into one of four archetypes, and subtly reshapes enemy AI, jump scare timing, difficulty, and atmosphere.

### Package: `behavior`

```
behavior/
  PlayerBehaviorTracker.java
  BehaviorSample.java
  BehaviorFeatureVector.java
  FeatureNormalizer.java
  KMeansClassifier.java
  ClassificationResult.java
  PlayerArchetype.java
  ArchetypeCentroid.java
  BehaviorChangeDetector.java
  BehaviorSystem.java
  effects/
    EnemyBehaviorEffect.java
    JumpScareTimingEffect.java
    DifficultyEffect.java
    AtmosphereEffect.java
    EnemyParams.java
    JumpScareParams.java
    DifficultyParams.java
  debug/
    BehaviorDebugOverlay.java
```

### The Four Archetypes

| Dimension | Paranoid | Methodical | Impulsive | Panicked |
|-----------|---------|-----------|----------|---------:|
| Move speed | 0.35 | 0.20 | 0.70 | 0.90 |
| Noise level | 0.30 | 0.15 | 0.65 | 0.80 |
| Crouch ratio | 0.35 | 0.70 | 0.10 | 0.05 |
| Stationary ratio | 0.45 | 0.40 | 0.15 | 0.10 |
| Look around rate | 0.80 | 0.30 | 0.40 | 0.75 |
| Flashlight toggle | 0.70 | 0.20 | 0.30 | 0.55 |
| **Door open speed** | 0.30 | 0.20 | 0.75 | 0.85 |
| Interaction rate | 0.55 | 0.75 | 0.40 | 0.20 |
| Sanity loss rate | 0.60 | 0.30 | 0.50 | 0.85 |
| Hide response | 0.15 | 0.40 | 0.70 | 0.05 |

### `BehaviorSample`

```java
public record BehaviorSample(
    float averageVelocity, float averageNoiseRms, float crouchFraction,
    float stationaryFraction, float cameraAngularVariance, float flashlightTogglesPerMin,
    float peakDoorOpenSpeed,        // [v2] peak normalized angular velocity this window
    float interactionsPerMin, float sanityLostThisWindow,
    float hideResponseSeconds
) {
    public static final BehaviorSample NEUTRAL =
        new BehaviorSample(1.5f, 0.2f, 0.2f, 0.2f, 30f, 0.5f, 0.4f, 2f, 3f, 2f);
}
```

### `KMeansClassifier`

```java
public final class KMeansClassifier {
    private static final int K           = 4;
    private static final int MAX_ITERS   = 20;
    private static final int MIN_SAMPLES = 3;

    public ClassificationResult classify(List<BehaviorFeatureVector> samples) {
        if (samples.size() < MIN_SAMPLES) return ClassificationResult.neutral();
        // K-means iterations → assign → recompute centroids → check convergence
        // Classify using mean of most recent 6 samples (30 seconds)
    }
}
```

### Effects per archetype

```java
// Enemy AI
private static final float[] INVESTIGATION_DELAY = { 3.5f, 2.0f, 0.5f, 1.0f };
private static final float[] PATROL_SPEED_MULT   = { 1.0f, 1.3f, 1.1f, 1.4f };
private static final float[] VISION_FOV          = { 70f, 85f, 65f, 60f };
private static final float[] HEARING_RANGE_MULT  = { 1.0f, 1.4f, 0.8f, 0.9f };
private static final float[] FAKE_OUT_PROBABILITY= { 0.35f, 0.0f, 0.0f, 0.1f };

// Jump Scare
private static final float[] MIN_SCARE_INTERVAL  = { 60f, 50f, 30f, 75f };
private static final float[] TENSION_THRESHOLD   = { 60f, 75f, 55f, 85f };

// Difficulty
private static final float[] SANITY_DRAIN_MULT   = { 1.1f, 1.0f, 1.15f, 0.85f };

// Atmosphere
private static final String[] AMBIENCE_TRACK = {
    "audio/ambience/paranoid_bed.wav",
    "audio/ambience/methodical_bed.wav",
    "audio/ambience/impulsive_bed.wav",
    "audio/ambience/panicked_bed.wav"
};
```

### Debug overlay (F9)

```
╔══ PLAYER BEHAVIOUR ════════════════╗
║ Archetype:  PARANOID               ║
║ Samples:    18 / 60                ║
╠════════════════════════════════════╣
║ PARANOID   ████████░░░░  0.61      ║
║ METHODICAL ████░░░░░░░░  0.24      ║
║ IMPULSIVE  █░░░░░░░░░░░  0.10      ║
║ PANICKED   ░░░░░░░░░░░░  0.05      ║
╠════════════════════════════════════╣
║ [10 feature bars]                  ║
╚════════════════════════════════════╝
```

---

# Group F — Player Systems

---

## 20. Enemy Footprint Tracking

### Package: `enemy/tracking`

```
enemy/tracking/
  FootprintTrail.java
  Footprint.java
  FootprintEmitter.java
  FootprintDetector.java
```

```java
public final class Footprint {
    public final Vector3 position;
    public final float   intensity;
    public float         age;

    public float effectiveIntensity(float lifetime) {
        return intensity * Math.max(0f, 1f - (age / lifetime));
    }
    public boolean expired(float lifetime) { return age >= lifetime; }
}
```

**Intensity by surface and posture:**

| Surface | Walking mult | × Crouching (0.25) |
|---------|-------------|-------------------|
| Concrete | 0.55 | 0.14 |
| Metal | 0.70 | 0.18 |
| Gravel | 0.80 | 0.20 |
| Dirt | 0.85 | 0.21 |
| Water | 0.90 | 0.23 |
| Wood | 0.50 | 0.13 |
| Tile | 0.60 | 0.15 |
| Carpet | 0.20 | 0.05 |

```java
public final class FootprintTrail {
    private static final float STAMP_INTERVAL_SECONDS = 0.8f;
    private static final float PRINT_LIFETIME_SECONDS = 90f;
    private static final int   MAX_PRINTS             = 120;
}
```

```java
public final class FootprintDetector {
    private static final float DETECTION_RADIUS    = 4.5f;
    private static final float DETECTION_THRESHOLD = 0.18f;
    private static final float MAX_DRIFT_DEGREES   = 22f;
    // Drift = MAX_DRIFT_DEGREES × (1 - effectiveIntensity)
}
```

K-Means detection threshold per archetype:

```java
private static final float[] PRINT_DETECTION_THRESHOLD = { 0.18f, 0.12f, 0.20f, 0.25f };
//                                                    PARANOID METHODICAL IMPULSIVE PANICKED
```

---

## 21. Sanity Hallucinations

### Package: `sanity/hallucination`

```java
public enum HallucinationType {
    FAKE_FOOTSTEP, FAKE_DOOR_CREAK, FAKE_ENEMY_BREATH,
    FAKE_ENEMY_GROWL, FAKE_ITEM_FALL, VISUAL_FLICKER
}
```

| Tier | Sanity range | Interval | What fires |
|------|-------------|----------|-----------|
| MILD | 25–50 | 17.5–35s | FAKE_FOOTSTEP, FAKE_DOOR_CREAK |
| HEAVY | 10–25 | 7–14s | All audio types |
| SEVERE | 0–10 | 2.8–5.6s | Everything including VISUAL_FLICKER |

Hallucinations **never** enter the propagation graph. The enemy does not react to them.

K-Means threshold override:

```java
private static final float[] HALLUCINATION_THRESHOLD = { 60f, 50f, 40f, 40f };
// PARANOID METHODICAL IMPULSIVE PANICKED
```

---

## 22. Breath-Hold Mechanic

### Package: `player`

```
player/
  BreathSystem.java
  BreathHudRenderer.java
```

```java
public final class BreathSystem {
    private static final float MAX_STAMINA        = 100f;
    private static final float DRAIN_PER_SECOND   = 22f;
    private static final float RECOVER_PER_SECOND = 11f;
    private static final float MIN_HOLD_STAMINA   = 20f;

    public static final float FOOTSTEP_VOLUME_MULT = 0.30f;
    public static final float MIC_SUPPRESSION      = 0.15f;
    public static final float ENEMY_HEARING_MULT   = 0.45f;

    public float footstepVolumeMult() { return isHolding() ? FOOTSTEP_VOLUME_MULT : 1.0f; }
    public float micSuppressionMult()  { return isHolding() ? MIC_SUPPRESSION : 1.0f; }
    public float enemyHearingMult()    { return isHolding() ? ENEMY_HEARING_MULT : 1.0f; }
    public boolean isHolding()         { return state == BreathState.HOLDING; }
    public float   staminaNorm()       { return stamina / MAX_STAMINA; }
}
```

---

# Group G — Developer Test Environment

---

## 23. UniversalTestScreen

**[v2] This section is entirely new. It replaces and supersedes the old `GltfMapTestScene` reference at step 34 of the implementation order.**

### Design intent

`UniversalTestScreen` is the **single canonical integration testbed** for all 22 game systems. Every system is wired in. The test map is larger than the old one and contains dedicated zones for each system. A fly mode (`0` key) lets developers navigate without gravity. The screen is **never shipped** — it lives only in the `lwjgl3` launcher module.

> **CRITICAL:** Fly mode does not exist in the production `GameScreen`. The `FlyModeController` class is only instantiated inside `UniversalTestScreen`. No fly code should ever be imported into `core`.

---

### Package: `devTest` (in `lwjgl3`)

```
lwjgl3/src/main/java/io/github/superteam/resonance/devTest/
  UniversalTestScreen.java         ← [v2] main screen, replaces GltfMapTestScene
  FlyModeController.java           ← [v2] fly movement, 0-key toggle
  TestMapLayout.java               ← [v2] hard-coded zone definitions for procedural test map
  CollisionWireframeRenderer.java  ← unchanged from Addendum §2
  ModelDebugScreen.java            ← unchanged from Addendum §3
```

---

### `FlyModeController` [v2]

Gravity-free first-person movement. Activated only inside `UniversalTestScreen`.

```java
public final class FlyModeController {

    private static final float DEFAULT_FLY_SPEED = 12f;   // m/s
    private static final float FAST_MULTIPLIER   = 3.0f;  // held Shift
    private static final float SLOW_MULTIPLIER   = 0.2f;  // held Ctrl

    private boolean enabled   = false;
    private float   flySpeed  = DEFAULT_FLY_SPEED;

    /**
     * Call before PlayerController.update() each frame.
     * When enabled, sets player velocity directly and skips gravity.
     */
    public void update(float delta, PlayerController player, PerspectiveCamera camera) {
        if (!enabled) return;

        float speed = flySpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed *= FAST_MULTIPLIER;
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) speed *= SLOW_MULTIPLIER;

        Vector3 forward = new Vector3(camera.direction).nor();
        Vector3 right   = new Vector3(camera.direction).crs(camera.up).nor();
        Vector3 move    = new Vector3();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE))   move.y += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) move.y -= 1f;

        if (move.len2() > 0f) move.nor().scl(speed * delta);

        // Bypass physics — directly translate the player capsule
        player.setPositionDirect(player.getPosition().add(move));
        player.setGravityEnabled(false);
    }

    public void toggle() {
        enabled = !enabled;
        Gdx.app.log("FlyMode", enabled ? "ON" : "OFF");
    }

    public void setSpeed(float speed) { flySpeed = Math.max(1f, speed); }
    public boolean isEnabled()        { return enabled; }
}
```

**Input wiring in `UniversalTestScreen`:**

```java
// In keyDown():
if (keycode == Input.Keys.NUM_0) {
    flyMode.toggle();
    debugConsole.log("Fly mode: " + (flyMode.isEnabled() ? "ON" : "OFF"));
    return true;
}
```

---

### Test map layout [v2]

The test map is a **large flat-floor building** (~60 m × 50 m) built entirely from code in `TestMapLayout` using Bullet box shapes. No `.gltf` file is needed for the test map; however, the map loader is still exercised by pointing it at a small placeholder `test-map.gltf`.

```
╔═══════════════════════════════════════════════════════════╗
║  [SPAWN]──[OPEN HALL]──[DOOR TEST ROOM]──[LOCKED ROOM]   ║
║     │                        │                            ║
║  [FOOTSTEP STRIP ZONE]   [BARGE DOOR ROOM]               ║
║  (wood/metal/carpet/       (reinforced door,              ║
║   concrete lanes)           sprint through)               ║
║     │                        │                            ║
║  [DARK ROOM / SANITY]    [HALLUCINATION ALCOVE]           ║
║  (no lights, enemy           (low sanity trigger,         ║
║   proximity drain)            fake sounds fire)           ║
║     │                        │                            ║
║  [CROUCH TUNNEL]─────────[BREATH TEST CORRIDOR]           ║
║  (ceiling 1.4m, player     (enemy patrol, hold alt        ║
║   MUST crouch, 12m long)    to sneak past)                ║
║     │                                                     ║
║  [CROSSROADS CORRIDOR]──[ENEMY PATROL ROOM]               ║
║  (4-way junction,           (navmesh, 3 waypoints,        ║
║   room boundaries)           full AI state machine)       ║
║     │                                                     ║
║  [STORY GATE TEST ROOM]──[SAVE / CHECKPOINT ZONE]         ║
║  (note pickup, keycard,      (checkpoint marker,           ║
║   story beat flow)            save/load test)             ║
║     │                                                     ║
║  [ROOM TRANSITION STRIP] (ROOM_BOUNDARY triggers,         ║
║                            fade-to-black + subtitle)      ║
╚═══════════════════════════════════════════════════════════╝
```

#### Zone specifications

| Zone | Dimensions (m) | Ceiling (m) | Purpose |
|------|----------------|-------------|---------|
| Open Hall | 14 × 8 | 4.0 | General movement, footstep, K-Means baseline |
| Door Test Room | 8 × 8 | 3.5 | Three doors: slow drag, fast drag, barge door |
| Locked Room | 6 × 6 | 3.5 | Locked door + keycard pickup |
| Footstep Strip Zone | 12 × 6 | 3.5 | 8 lanes of different surface materials |
| Dark Room / Sanity | 10 × 10 | 3.5 | Zero lights, sanity drain, hallucinations |
| Hallucination Alcove | 5 × 5 | 3.0 | Sanity set to 30 on entry; fake sounds fire |
| **Crouch Tunnel** | 12 × 2 | **1.4** | Forces crouch; player body height 1.3m comfortably fits |
| Breath Test Corridor | 14 × 4 | 3.5 | Enemy patrols this corridor; hold-breath test |
| Crossroads Corridor | 8 × 8 | 3.5 | 4-way junction with ROOM_BOUNDARY triggers |
| Enemy Patrol Room | 12 × 10 | 3.5 | Full enemy AI, 3 navmesh waypoints |
| Story Gate Room | 8 × 8 | 3.5 | Note, keycard, story beat sequence |
| Checkpoint Zone | 6 × 6 | 3.5 | Checkpoint trigger, save/load |
| Room Transition Strip | 14 × 2 | 3.5 | ROOM_BOUNDARY AABB triggers, fade test |

#### Crouch tunnel fix [v2]

The previous test map had a crouch area ceiling at 1.2 m, which was shorter than the crouching player capsule (1.3 m diameter + standing offset) causing the player to clip through the ceiling. The new ceiling is **1.4 m**, giving 0.1 m clearance for the crouching capsule. Player crouch height must be verified to match:

```java
// PlayerController constants — confirm these match:
private static final float STANDING_HEIGHT  = 1.8f; // capsule full height
private static final float CROUCH_HEIGHT    = 1.0f; // capsule height while crouching
private static final float CROUCH_EYE_Y     = 0.7f; // camera Y offset while crouching
// Total crouching envelope = CROUCH_HEIGHT + ground offset ≈ 1.2m → fits in 1.4m ceiling ✓
```

---

### `UniversalTestScreen` — system wiring [v2]

All 22 systems are instantiated and wired here. The class follows the Orchestrator Pattern: it holds references, ticks systems in order, routes inputs, and renders — but contains no embedded game logic.

```java
public class UniversalTestScreen implements Screen {

    // ── Systems ──────────────────────────────────────────────
    private SettingsSystem          settings;
    private GameAudioSystem         audio;
    private SoundBank               soundBank;
    private AmbienceTrack           ambience;
    private EventState              eventState;
    private EventBus                eventBus;
    private EventRegistry           eventRegistry;
    private TriggerEvaluator        triggerEvaluator;
    private StorySystem             storySystem;
    private RaycastInteractionSystem interaction;
    private InteractableRegistry    interactableRegistry;
    private DoorGrabInteraction     doorGrab;          // [v2]
    private DoorBargeDetector       doorBarge;         // [v2]
    private DoorCreakSystem         doorCreak;         // [v2]
    private DialogueSystem          dialogue;
    private SubtitleRenderer        subtitleRenderer;
    private SaveSystem              saveSystem;
    private RoomTransitionSystem    roomTransition;
    private LightManager            lightManager;
    private FlashlightController    flashlight;
    private SanitySystem            sanity;
    private JumpScareDirector       jumpScareDirector;
    private FootstepSystem          footstepSystem;
    private BehaviorSystem          behaviorSystem;
    private BehaviorDebugOverlay    behaviorOverlay;
    private EnemyController         enemy;
    private FootprintTrail          footprintTrail;
    private FootprintEmitter        footprintEmitter;
    private HallucinationDirector   hallucinationDirector;
    private BreathSystem            breathSystem;
    private BreathHudRenderer       breathHud;
    private DebugConsole            debugConsole;
    private FlyModeController       flyMode;           // [v2] test-only
    private CollisionWireframeRenderer wireframe;

    // ── Physics ──────────────────────────────────────────────
    private btDiscreteDynamicsWorld physicsWorld;
    private PlayerController        player;
    private PerspectiveCamera       camera;

    // ── Test map zones (hard-coded bounds from TestMapLayout) ─
    private TestMapLayout           testMap;

    @Override
    public void show() {
        testMap = new TestMapLayout();  // builds all Bullet box shapes
        // ... instantiate and wire all systems in dependency order
        // ... register ROOM_BOUNDARY triggers from testMap.boundaryVolumes()
        // ... place surface material zones for footstep strip
        // ... place sanity drain volume over dark room
        // ... place 3 DoorControllers with DoorKnobColliders
        // ... add enemy to patrol room with 3 navmesh waypoints
        // ... register all debug console commands
    }

    @Override
    public void render(float delta) {
        // Input
        handleInput(delta);

        // Fly mode overrides player physics when enabled
        flyMode.update(delta, player, camera);

        // Core player update (skipped if fly active)
        if (!flyMode.isEnabled()) player.update(delta, physicsWorld);

        // All system ticks (order matters — same as production GameScreen)
        eventBus.flushQueue();
        triggerEvaluator.update(delta, buildEvalCtx(), buildEventCtx());
        footstepSystem.update(delta, player, currentSurface());
        footprintEmitter.update(delta, player, currentSurface());
        breathSystem.update(delta, Gdx.input.isKeyPressed(settings.key("HOLD_BREATH")));
        doorGrab.update(delta, Gdx.input.getDeltaX(),
                        Gdx.input.isButtonPressed(Input.Buttons.LEFT),
                        hoveredDoor(), doorCreak, doorPosition(), buildEventCtx());
        doorBarge.check(player, nearestDoor(), buildEventCtx());
        sanity.update(delta, buildEventCtx());
        hallucinationDirector.update(delta, sanity, player.getPosition(), camera.direction);
        enemy.update(delta);
        lightManager.update(delta);
        behaviorSystem.update(delta, player, camera, flashlight, sanity);
        jumpScareDirector.update(delta);
        roomTransition.update(delta, dialogue);
        storySystem.update(delta);
        dialogue.update(delta);
        saveSystem.update(delta);
        debugConsole.update(delta);

        // Render
        renderWorld();
        behaviorOverlay.render(delta);   // F9
        breathHud.render(delta);
        subtitleRenderer.render(delta);
        debugConsole.render();
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {
            flyMode.toggle();
            debugConsole.log("Fly: " + (flyMode.isEnabled() ? "ON" : "OFF"));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) flashlight.toggle();
        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) debugConsole.toggleOpen();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9))  behaviorOverlay.toggleVisible();
        if (Gdx.input.isKeyJustPressed(Input.Keys.K))   wireframe.toggleEnabled();
    }
}
```

---

### `TestMapLayout` [v2]

Builds the entire test map from Bullet primitives at startup. No `.gltf` loading required.

```java
public final class TestMapLayout {

    /** All room-boundary AABB volumes with their event IDs. */
    public List<RoomBoundaryDef> boundaryVolumes() { ... }

    /** Surface material zone AABBs for the footstep strip. */
    public List<SurfaceZoneDef> surfaceZones() { ... }

    /** Door spawn points with hinge position, open direction, lock state. */
    public List<DoorSpawnDef> doorSpawns() { ... }

    /** Enemy patrol waypoints in the patrol room. */
    public List<Vector3> patrolWaypoints() { ... }

    /** Sanity drain volume (the dark room AABB). */
    public BoundingBox darkRoomVolume() { ... }

    /**
     * Builds all static geometry as btBoxShapes into physicsWorld.
     * Walls are 0.3m thick; floors/ceilings are 0.2m thick.
     */
    public void buildPhysics(btDiscreteDynamicsWorld world) { ... }

    // Zone-ceiling constants
    public static final float CROUCH_TUNNEL_CEILING = 1.4f;  // [v2] fixed from 1.2f
    public static final float DEFAULT_CEILING        = 3.5f;
}
```

---

### All systems present in UniversalTestScreen — checklist

| # | System | Wired in test screen |
|---|--------|---------------------|
| 1 | Event System | ✅ eventBus + eventRegistry + eventLoader |
| 2 | Sound System | ✅ audio + soundBank + ambience |
| 3 | Trigger System | ✅ triggerEvaluator; zone + state + compound |
| 4 | Map Collision | ✅ testMap.buildPhysics(); wireframe (K key) |
| 5 | Map Editor | ✅ F12 opens MapEditorIntegration |
| 6 | Enemy AI | ✅ EnemyController; full state machine in patrol room |
| 7 | Door — physics drag | ✅ doorGrab + doorBarge + doorCreak + 3 DoorControllers |
| 8 | Raycast Interaction | ✅ interaction; knob collider registry |
| 9 | Dynamic Lighting | ✅ lightManager; flashlight (L key); flicker lights |
| 10 | Sanity System | ✅ sanity; drain in dark room; vignette + aberration |
| 11 | Jump Scare Director | ✅ jumpScareDirector; tension from sanity + enemy |
| 12 | Dialogue & Subtitles | ✅ dialogue; subtitleRenderer |
| 13 | Save / Checkpoint | ✅ saveSystem; checkpoint in checkpoint zone |
| 14 | Story System | ✅ storySystem; note + keycard + story beats |
| 15 | Room Transition | ✅ roomTransition; ROOM_BOUNDARY triggers |
| 16 | Debug Console | ✅ debugConsole (backtick) |
| 17 | Settings | ✅ settings; keybind for fly, breath, flashlight |
| 18 | Footstep Materials | ✅ footstepSystem; 8 surface lanes |
| 19 | K-Means Behavior | ✅ behaviorSystem; F9 overlay |
| 20 | Footprint Tracking | ✅ footprintEmitter + trail + detector; enemy trail follow |
| 21 | Hallucinations | ✅ hallucinationDirector; triggered in hallucination alcove |
| 22 | Breath-Hold | ✅ breathSystem; breathHud; enemy perception in corridor |
| — | Fly Mode (test-only) | ✅ flyMode (0 key); not in production |

---

# How All Systems Connect

## Complete horror scenario

```
Player hides in a locker, holds breath (Alt), crouches — sanity = 22

  BreathSystem → HOLDING
    │ footstepVolumeMult = 0.30
    │ micSuppressionMult = 0.15
    │ enemyHearingMult   = 0.45
    │
    └─ EnemyPerception: all player sounds filtered below threshold
         └─ Enemy in TRAIL_FOLLOW — no new prints — coldTimer ticks
              └─ After 5s cold → INVESTIGATE → eventually PATROL

  Sanity = 22 (below 25) → HallucinationDirector fires
    │ type: FAKE_FOOTSTEP (HEAVY tier, 6–15m range)
    │ → plays enemy/footstep_02.wav spatially
    │ → does NOT inject into propagation graph
    │
    └─ Player panics, releases Alt → RECOVERING

  If player held too long:
    └─ Stamina → 0 → FORCED_EXHALE
         └─ ITEM_IMPACT at 0.75 → propagation graph
              └─ EnemyPerception.onSoundHeard() → INVESTIGATE

  Door interaction — careful vs aggressive example:
    │ Careful: player holds LMB, mouse drifts slowly (normSpeed ≈ 0.10)
    │   → DoorCreakSystem: volume 0.10, pitch 0.75, creak_slow.wav
    │   → DOOR_CREAK graph event: intensity 0.04 → enemy may not hear
    │   → K-Means: peakDoorOpenSpeed = 0.10 → pushes METHODICAL
    │
    └─ Aggressive: player sprints → DoorBargeDetector triggers
         → DoorController.bargeOpen()
         → DOOR_BARGE graph event: intensity 1.1 → enemy hears immediately
         → K-Means: peakDoorOpenSpeed = 1.0 → pushes PANICKED / IMPULSIVE

  BehaviorSystem samples after 5s window:
    │ crouchRatio: 1.0, stationaryFraction: high, peakDoorOpenSpeed: 0.10
    │ → K-Means: dominant = METHODICAL
         └─ EnemyBehaviorEffect: hearing range × 1.4
         └─ AtmosphereEffect: crossfades to methodical_bed.wav
```

---

# Complete Package Layout

```
core/src/main/java/io/github/superteam/resonance/
  event/
    GameEvent.java  EventAction.java  EventSequence.java  EventRegistry.java
    EventBus.java  EventState.java  EventContext.java  EventLoader.java
    actions/
      PlaySoundAction.java  PropagateGraphAction.java  PlayAnimationAction.java
      SetFlagAction.java  FireEventAction.java  WaitAction.java
      ShowSubtitleAction.java  SetSanityDeltaAction.java
      TriggerJumpScareAction.java  TransitionLevelAction.java
      CheckpointAction.java  StoryEventAction.java  LogAction.java

  audio/
    GameAudioSystem.java  AudioChannel.java  SoundBank.java
    AmbienceTrack.java  SpatialSfxEmitter.java  WorldSoundEmitter.java
    AudioChannelConfig.java

  trigger/
    Trigger.java  TriggerEvaluator.java  TriggerEvaluationContext.java
    conditions/
      ZoneTrigger.java  InteractionTrigger.java  TimerTrigger.java
      StateTrigger.java  CompoundTrigger.java

  map/
    MapCollisionBuilder.java  MapLoader.java  LoadedMap.java
    MapDocument.java  MapObject.java  MapObjectType.java
    MapDocumentLoader.java  MapDocumentSerializer.java

  enemy/
    EnemyController.java  EnemyStateMachine.java  EnemyState.java
    EnemyPerception.java  EnemyNavigator.java  EnemyAnimator.java
    NavMesh.java  NavMeshBuilder.java
    states/
      IdleState.java  PatrolState.java  InvestigateState.java
      TrailFollowState.java  ChaseState.java  AttackState.java  StunnedState.java
    tracking/
      FootprintTrail.java  Footprint.java  FootprintEmitter.java  FootprintDetector.java

  interactable/
    Interactable.java  InteractableRegistry.java
    door/
      DoorController.java          ← [v2] physics drag model
      DoorKnobCollider.java        ← [v2] new
      DoorGrabInteraction.java     ← [v2] new
      DoorBargeDetector.java       ← [v2] new
      DoorCreakSystem.java         ← [v2] updated continuous drag
      DoorLockState.java
    objects/
      CabinetController.java  DrawerController.java
      SwitchController.java  KeycardPickup.java

  interaction/
    RaycastInteractionSystem.java  InteractionPromptRenderer.java  InteractionResult.java

  lighting/
    LightManager.java  GameLight.java  FlickerController.java
    FlashlightController.java  LightReactorListener.java

  sanity/
    SanitySystem.java  SanityDrainSource.java  SanityEffect.java
    drains/
      DarknessPresenceDrain.java  EnemyProximityDrain.java  EventDrain.java
    effects/
      ScreenVignetteEffect.java  ShaderDistortionEffect.java
      AudioGlitchEffect.java  HallucinationEffect.java
    hallucination/
      HallucinationDirector.java  HallucinationEvent.java  HallucinationType.java
      AudioHallucination.java  VisualHallucination.java

  scare/
    JumpScareDirector.java  JumpScare.java  ScareType.java
    scripted/
      AudioOnlyScare.java  FlashScare.java  EnemyAppearScare.java
      HallucinationScare.java  EnvironmentScare.java

  dialogue/
    DialogueSystem.java  DialogueLine.java  DialogueSequence.java
    DialogueLoader.java  SubtitleRenderer.java

  save/
    SaveSystem.java  SaveData.java  CheckpointManager.java  SaveSlot.java

  story/
    StorySystem.java  StoryChapter.java  StoryBeat.java
    StoryBeatCondition.java  StoryLoader.java  StoryGate.java  StoryEventAction.java

  transition/
    RoomTransitionSystem.java  FadeOverlay.java

  debug/
    DebugConsole.java  ConsoleRenderer.java  ConsoleCommand.java  ConsoleRegistry.java
    built_in/
      TeleportCommand.java  FireEventCommand.java  SetFlagCommand.java
      SetSanityCommand.java  SpawnEnemyCommand.java  GodModeCommand.java
      GiveItemCommand.java  ListEventsCommand.java  ReloadMapCommand.java
      PrintStateCommand.java  BeatCommand.java  ChapterCommand.java
      StoryStatusCommand.java  BehaviorCommand.java  HallucinateCommand.java
      FlyCommand.java          ← [v2] new

  settings/
    SettingsSystem.java  SettingsData.java  KeybindRegistry.java

  footstep/
    FootstepSystem.java  FootstepSoundSet.java  SurfaceMaterial.java

  behavior/
    PlayerBehaviorTracker.java  BehaviorSample.java  BehaviorFeatureVector.java
    FeatureNormalizer.java  KMeansClassifier.java  ClassificationResult.java
    PlayerArchetype.java  ArchetypeCentroid.java  BehaviorChangeDetector.java
    BehaviorSystem.java
    effects/
      EnemyBehaviorEffect.java  JumpScareTimingEffect.java
      DifficultyEffect.java  AtmosphereEffect.java
      EnemyParams.java  JumpScareParams.java  DifficultyParams.java
    debug/
      BehaviorDebugOverlay.java

  player/
    BreathSystem.java  BreathHudRenderer.java

lwjgl3/src/main/java/io/github/superteam/resonance/
  mapeditor/
    MapEditorPanel.java  ObjectPalette.java  SceneOutlinePanel.java
    ObjectPropertyPanel.java  MapEditorIntegration.java
  devTest/
    UniversalTestScreen.java         ← [v2] replaces GltfMapTestScene
    FlyModeController.java           ← [v2] new
    TestMapLayout.java               ← [v2] new
    CollisionWireframeRenderer.java
    ModelDebugScreen.java

assets/
  audio/
    music/
    ambience/
      paranoid_bed.wav  methodical_bed.wav  impulsive_bed.wav  panicked_bed.wav
      cave_drip.wav  wind_exterior.wav
    sfx/
      env/  player/  items/  enemy/  ui/
      door/
        creak_slow.wav  creak_medium.wav  creak_fast.wav
        slam_impact.wav  locked.wav       ← [v2] slam_impact.wav new
      footstep/
        concrete/ wood/ metal/ gravel/ water/ carpet/ tile/ dirt/
  dialogue/player/
  events/    *.json
  triggers/  *.json
  maps/      resonance-map.json
  story/     story.json
  data/      tips.json
  models/    resonance-map.gltf
  saves/     (runtime — not committed)
```

---

# Implementation Order

Build in strict dependency order.

| # | System | Depends on | Priority |
|---|--------|-----------|----------|
| 1 | **Settings System** (+ FLY_TOGGLE keybind) | Nothing | 🔴 First |
| 2 | **Keybind Registry** (+ HOLD_BREATH + FLY_TOGGLE) | Settings | 🔴 First |
| 3 | **HearingCategory + SoundEvent** (+ DOOR_BARGE) | SoundEvent enum | 🔴 First |
| 4 | **Map Loader + BVH Collision** | ModelAssetManager, Bullet | 🔴 Foundation |
| 5 | **GameAudioSystem + SoundBank** (+ playSpatialSfxPitched overload) | LibGDX Audio | 🔴 Foundation |
| 6 | **SpatialSfxEmitter** | GameAudioSystem, PropOrchestrator | 🔴 Foundation |
| 7 | **ModelDebugScreen: texture fix** | gdx-gltf | 🟡 Dev tool |
| 8 | **ModelDebugScreen: collision wireframe** | MapCollisionBuilder.debugTriangles | 🟡 Dev tool |
| 9 | **Footstep Material System** | SpatialSfxEmitter, Bullet | 🟡 Early |
| 10 | **EventState + EventBus + EventRegistry** | Nothing | 🔴 Foundation |
| 11 | **GameEvent + all EventActions** | EventBus, GameAudioSystem | 🔴 Foundation |
| 12 | **EventLoader (JSON)** | GameEvent | 🟡 Early |
| 13 | **StorySystem + StoryChapter + StoryBeat + StoryLoader** | EventBus, EventState | 🟡 Early |
| 14 | **StoryGate + StoryEventAction** | StorySystem | 🟡 Early |
| 15 | **Trigger System** | EventBus, StorySystem | 🟡 Early |
| 16 | **DoorLockState + DoorCreakSystem** | GameAudioSystem | 🟡 Early |
| 17 | **DoorKnobCollider** | Bullet | 🟡 Early |
| 18 | **DoorGrabInteraction** | DoorCreakSystem, BehaviorSystem stub | 🟡 Early |
| 19 | **DoorBargeDetector** | PlayerController, DoorController | 🟡 Early |
| 20 | **DoorController** (full physics drag model) | All door subsystems | 🟡 Early |
| 21 | **Raycast Interaction System** (+ knob collider registry) | Bullet, InteractableRegistry | 🟡 Early |
| 22 | **Map Editor (Swing)** | MapDocument | 🟡 Early |
| 23 | **Map Document Loader** | AudioSystem, TriggerEvaluator | 🟡 Early |
| 24 | **Debug Console** (+ fly + hallucinate commands) | EventBus, StorySystem stub | 🟡 Dev tool |
| 25 | **Dialogue + Subtitle System** | GameAudioSystem | 🟢 Mid |
| 26 | **Save / Checkpoint System** | EventState, DialogueSystem | 🟢 Mid |
| 27 | **RoomTransitionSystem** | FadeOverlay, DialogueSystem | 🟢 Mid |
| 28 | **Dynamic Lighting System** | WorldShader uniforms | 🟢 Mid |
| 29 | **Sanity / Fear System** | LightManager | 🟢 Mid |
| 30 | **Hallucination System** | SanitySystem, GameAudioSystem | 🟢 Mid |
| 31 | **Enemy AI — Perception** | Bullet, PropGraph | 🟠 Complex |
| 32 | **Enemy AI — NavMesh + Navigator** | MapDocument | 🟠 Complex |
| 33 | **Enemy AI — State Machine + Animator** | Perception, Navigator | 🟠 Complex |
| 34 | **Footprint Tracking** | SurfaceMaterial, EnemyStateMachine | 🟠 Complex |
| 35 | **Jump Scare Director** | SanitySystem, EnemyController | 🟠 Complex |
| 36 | **Player Behaviour Classification (K-Means)** | All behavior hooks | 🔵 Integration |
| 37 | **Breath-Hold Mechanic** | FootstepSystem, EnemyPerception, SanitySystem | 🔵 Integration |
| 38 | **FlyModeController** | PlayerController | 🔵 Dev tool |
| 39 | **TestMapLayout** | Bullet | 🔵 Dev tool |
| 40 | **Wire all systems into UniversalTestScreen** | All above | 🔵 Integration |

---

# Acceptance Criteria

## Foundation

| System | Test | Pass |
|--------|------|------|
| **Settings** | Change sensitivity, relaunch | Persists |
| **Keybinds** | Rebind INTERACT to G, press near door | Door knob prompt shows |
| **Map BVH** | Load `resonance-map.gltf` | Player walks accurately on geometry |
| **Audio** | `setAmbience("cave_drip.wav", 2.0f)` | Loops seamlessly, crossfades |
| **Spatial SFX** | Play at 20m vs 2m | 20m significantly quieter |
| **Spatial SFX + graph** | SFX with non-AMBIENCE graphEvent | Enemy director reacts |
| **Hearing filter** | `WorldSoundEmitter` drip fires | Enemy does NOT investigate |
| **Hearing filter** | `AmbienceTrack` plays | Enemy does NOT react |
| **Hearing filter** | Player throws item → `ITEM_THROW` | Enemy investigates position |

## Map & Editor

| System | Test | Pass |
|--------|------|------|
| **Collision wireframe** | Load `-map.gltf`, press K | Green wireframe covers exact visual geometry |
| **Collision wireframe** | Press K again | Wireframe toggles off |
| **Single map** | Load `resonance-map.gltf` | All rooms present in one mesh |
| **Room boundary** | Walk through `ROOM_BOUNDARY` trigger | Fade-to-black, fade-in, room subtitle |
| **Map Editor** | Add DOOR, set position, save | `resonance-map.json` contains the door |

## Core Gameplay — Door (Physics Drag) [v2]

| System | Test | Pass |
|--------|------|------|
| **Knob raycast** | Look away from door | No prompt shown |
| **Knob raycast** | Look at door knob within 2.5m | "Hold [LMB] to grab" prompt |
| **Grab** | Hold LMB on knob | Door enters GRABBING state |
| **Drag slow** | Hold LMB, mouse drifts slowly | Door opens gradually; creak_slow plays quietly |
| **Drag fast** | Hold LMB, mouse sweeps quickly | Door swings fast; creak_fast plays loudly |
| **Release coast** | Release LMB mid-swing | Door decelerates and stops (COASTING → IDLE) |
| **Creak floor** | Drag at normSpeed < 0.05 | No creak plays (below floor threshold) |
| **Creak continuous** | Drag for 3 seconds | Creak loops with cooldown, not one-shot |
| **Pitch scaling** | Slow drag vs fast drag | Pitch noticeably lower at slow, higher at fast |
| **Barge** | Sprint toward door at > 4.8 m/s | Door slams to 90°; slam_impact.wav + creak_fast play |
| **Barge sound** | Barge event | DOOR_BARGE propagation event fires; enemy hears it |
| **Barge locked** | Sprint toward locked door | No barge (lock check prevents it) |
| **K-Means door slow** | Drag slowly 5× | `peakDoorOpenSpeed` ≤ 0.25 in sample |
| **K-Means door barge** | Barge 3×, then classify | Pushes toward PANICKED / IMPULSIVE |
| **Story gate** | Drag door with unfulfilled story gate | Locked click plays; door does not move |
| **Knob sync** | Open door to 45°, look at knob | Knob collider has rotated with the door panel |

## Footstep

| System | Test | Pass |
|--------|------|------|
| **Footstep** | Walk on WOOD | Wood variant plays |
| **Footstep** | Crouch-walk | Quieter, still injected into graph |

## UniversalTestScreen [v2]

| System | Test | Pass |
|--------|------|------|
| **Fly toggle** | Press 0 | Player lifts off floor; gravity disabled |
| **Fly toggle off** | Press 0 again | Player drops to ground; normal movement |
| **Fly direction** | Fly with W/A/S/D | Moves relative to camera direction |
| **Fly vertical** | Fly with Space / Alt | Ascends / descends |
| **Fly fast** | Hold Shift while flying | Speed × 3 |
| **Fly slow** | Hold Ctrl while flying | Speed × 0.2 |
| **Fly console** | `fly speed 30` | Fly speed updates immediately |
| **Test map rooms** | Load UniversalTestScreen | All zone walls and floors present |
| **Crouch tunnel** | Crouch and enter tunnel | Player fits; no ceiling clip |
| **Crouch tunnel standing** | Try to stand in tunnel | Cannot stand (ceiling blocks) |
| **Footstep strip** | Walk across 8 lanes | Each lane plays different material SFX |
| **Dark room sanity** | Enter dark room | Sanity begins draining |
| **Hallucination alcove** | Enter alcove with sanity 30 | Fake sounds fire within 35s |
| **Barge door zone** | Barge door in Door Test Room | All barge criteria met |
| **Enemy patrol room** | Enter patrol room | Enemy walks 3-waypoint route |
| **Story gate room** | Pick up note; interact with story door | Beat completes; door becomes interactable |
| **Room transition** | Walk through boundary strip | Fade + subtitle fires |
| **All systems** | Run 5 minutes in test screen | No NullPointerException; no memory leak |

## Narrative

| System | Test | Pass |
|--------|------|------|
| **Events** | Fire `"creak-then-groan"` | Sound + subtitle + flag set |
| **Triggers — zone** | Walk into zone trigger | Target event fires |
| **Story — gate** | Beat not complete | Cannot interact with story door |
| **Story — advance** | Interact with `note-reception-desk` | Beat completes, event fires |
| **Dialogue** | `showSubtitle("test", 3.0f)` | Text appears, fades after 3s |
| **Save** | Reach checkpoint | `autosave.json` written |
| **Save** | Load autosave | Position, sanity, inventory restored |

## Horror Atmosphere

| System | Test | Pass |
|--------|------|------|
| **Lighting** | Flickering light in map | Flickers with noise curve |
| **Flashlight** | Leave on 66s | Battery depletes, flicker starts |
| **Sanity** | Stand in dark room 10s | Sanity drops; vignette appears |
| **Sanity** | Enemy approaches to 5m | Sanity drops faster |
| **Jump scare** | Tension > 70, sanity > 50 | AUDIO_ONLY scare fires |

## Enemy AI

| System | Test | Pass |
|--------|------|------|
| **Patrol** | Enemy has waypoints | Walks route indefinitely |
| **Hearing** | Player loud noise | Enemy moves to position |
| **Sight** | Player in FOV, clear LOS | Enemy → CHASE |
| **Lost** | Player breaks LOS 5s | Enemy → INVESTIGATE → PATROL |

## K-Means Behaviour

| System | Test | Pass |
|--------|------|------|
| **Tracker — door drag slow** | Slow drag 5× | `peakDoorOpenSpeed` ≈ 0.15 |
| **Tracker — door barge** | Barge door | `peakDoorOpenSpeed` = 1.0 |
| **K-means** | < 3 samples | Returns `neutral()` |
| **K-means** | 20 pure-sprint + barge samples | IMPULSIVE or PANICKED dominant |
| **Debug overlay** | F9 | Shows correct archetype and bars |
| **Debug console** | `behavior force PANICKED` | Dominant changes immediately |

## Footprint Tracking

| System | Test | Pass |
|--------|------|------|
| **Stamp** | Walk 5m | Trail grows |
| **Expiry** | Wait 90s without moving | Trail clears |
| **Intensity — crouch** | Crouch-walk on carpet | Prints below 0.15 intensity |
| **Intensity — sprint** | Sprint on metal | Prints above 0.70 intensity |

## Hallucinations

| System | Test | Pass |
|--------|------|------|
| **Threshold** | Sanity = 55 | No hallucinations |
| **Mild** | Sanity = 40 | Only FAKE_FOOTSTEP or FAKE_DOOR_CREAK |
| **No graph** | Hallucination plays | Enemy does NOT investigate |
| **Debug** | `hallucinate FAKE_FOOTSTEP` | Fires immediately |

## Breath-Hold

| System | Test | Pass |
|--------|------|------|
| **Hold** | Press Alt | `isHolding()` true, stamina decreasing |
| **Footstep suppress** | Hold Alt walking | Volume ≈ 30% normal |
| **Enemy deaf** | Hold Alt, walk past enemy | Enemy does not investigate |
| **Forced exhale** | Hold until stamina = 0 | Loud exhale plays; graph event fires |
| **HUD visible** | Stamina < 100% | Arc appears near crosshair |

## Interaction — All Systems Together

| System | Test | Pass |
|--------|------|------|
| **Triple interaction** | Hide holding breath at sanity 20 | Enemy loses trail, hallucinations fire, HUD shows arc |
| **K-Means + breath** | Hold breath constantly for 5 minutes | K-Means pushes toward METHODICAL |
| **Door + K-Means** | Alternate slow drag and barge 10× | Classifier shifts between archetypes |
| **Breath + footprints** | Hold breath, crouch-walk | Prints below detection threshold on most surfaces |

## Debug Console

| System | Test | Pass |
|--------|------|------|
| **Open** | Press backtick | Overlay opens |
| **Teleport** | `tp 5 0 5` | Player teleports |
| **Fire event** | `fire alarm-trigger` | Event fires immediately |
| **Set sanity** | `sanity 10` | Sanity set, effects activate |
| **Fly** | `fly` | Fly mode toggles (same as 0 key) |
| **Fly speed** | `fly speed 25` | Speed updates; noticeable in flight |
| **Map Editor** | Open via F12 | Swing window opens without freezing |

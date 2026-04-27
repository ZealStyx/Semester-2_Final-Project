# Resonance — Combined Master Plan

**Project:** `io.github.superteam.resonance`  
**Scope:** `core` + `lwjgl3` + `server` + `shared` modules  
**Total systems:** 21  
**Source documents merged:**
- `RESONANCE_MASTER_PLAN.md` — 17 core systems
- `MASTER_PLAN_ADDENDUM.md` — 5 patches (integrated inline)
- `KMEANS_BEHAVIOR_PLAN.md` — System 18: Player Behaviour Classification
- `THREE_FEATURES_PLAN.md` — Systems 19–21: Footprint Tracking, Hallucinations, Breath-Hold

> Addendum patches are **not** listed as separate sections — they are integrated directly into the systems they affect. The `[PATCH]` marker calls out where a patch was applied.

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
7. [Door & Interactable System](#7-door--interactable-system)
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
        // Spatial WAV playback — always runs, player still hears drips/ambience
        float distance = emitterPos.dst(listenerPos);
        float atten    = Math.max(0f, 1f - (distance / MAX_AUDIBLE_DISTANCE));
        float finalVol = baseVolume * atten * atten;
        float pan      = MathUtils.clamp(emitterPos.x - listenerPos.x, -1f, 1f);
        if (finalVol > 0.01f) soundBank.sound(wavPath).play(finalVol, 1.0f, pan);

        // Only inject into propagation graph if enemy is eligible to hear this
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

#### `SoundEvent` — updated to carry `HearingCategory` [PATCH: Addendum §1]

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
// In UniversalTestScene / GltfMapTestScene — mic update path:
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

        // Story gate — skip this trigger if the story isn't ready
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
        FloatArray debugTris = new FloatArray(); // [PATCH] debug triangle snapshot
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
                // [PATCH] snapshot for debug wireframe draw
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

    // [PATCH] debugTriangles field added
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
// Dual-path rendering
private SceneManager sceneManager;   // for gltf files
private ModelBatch   modelBatch;     // for g3d files
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
    // [PATCH] also load debug triangles for wireframe
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

Gives the enemy the ability to patrol the map, hear sounds from the propagation graph, see the player via vision cones, investigate disturbances, chase, and attack. Built on top of the existing `EnemyHearingTarget`, `AcousticGraphEngine`, and director stubs.

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
    TrailFollowState.java     ← added in System 20
    ChaseState.java
    AttackState.java
    StunnedState.java
  tracking/                  ← added in System 20
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
INVESTIGATE ──(prints found)────► TRAIL_FOLLOW   [System 20]
INVESTIGATE ──(nothing found)───► PATROL
INVESTIGATE ──(player seen)─────► CHASE
TRAIL_FOLLOW ──(player seen)────► CHASE
TRAIL_FOLLOW ──(trail cold 5s)──► INVESTIGATE   [System 20]
CHASE ──(player lost > 5s)──────► INVESTIGATE
CHASE ──(within attack range)───► ATTACK
ATTACK ──(done)─────────────────► CHASE
ANY ──(stun item hit)───────────► STUNNED
STUNNED ──(timer done)──────────► PATROL
```

```java
public enum StateId { IDLE, PATROL, INVESTIGATE, TRAIL_FOLLOW, CHASE, ATTACK, STUNNED }
```

#### `EnemyPerception` — with hearing filter [PATCH: Addendum §1]

```java
public final class EnemyPerception {

    private static final float VISION_FOV_DEGREES = 70f;
    private static final float VISION_RANGE       = 12f;
    private static final float MIN_AUDIBLE_INTENSITY = 0.18f;

    private Vector3  lastHeardPosition;
    private float    lastHeardIntensity;
    private boolean  hasUnhandledSound;

    // [PATCH] filter out ambience and breath-suppressed sounds
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
// Called by EnemyBehaviorEffect.apply()
public void setInvestigationDelay(float delay)    { investigationDelay = delay; }
public void setPatrolSpeedMultiplier(float mult)  { patrolSpeedMult = mult; }
public void setVisionFov(float fovDeg)            { perception.setVisionFov(fovDeg); }
public void setHearingRangeMultiplier(float mult) { perception.setHearingRangeMult(mult); }
public void setFakeOutProbability(float prob)     { fakeOutProbability = prob; }
```

#### NavMesh — single-map waypoints [PATCH: Addendum §4]

Waypoints are grouped by room so `PatrolState` can be scoped to a room tag:

```json
{
  "id": "nav-server-01",
  "type": "SPAWN_POINT",
  "position": { "x": 8.0, "y": 0.0, "z": -2.0 },
  "properties": { "label": "nav-waypoint", "room": "server-room" }
}
```

---

## 7. Door & Interactable System

### What it does

Interactive doors and objects with physics-driven open speed, creak sounds tied to how fast the door is pushed, locked/unlocked state, keycard requirements, and optional story gating.

### Package: `interactable`

```
interactable/
  Interactable.java
  InteractableRegistry.java
  door/
    DoorController.java
    DoorCreakSystem.java
    DoorLockState.java
  objects/
    CabinetController.java
    DrawerController.java
    SwitchController.java
    KeycardPickup.java
```

#### `DoorController.onInteract()` — with story gate [PATCH: Addendum §5]

```java
@Override
public void onInteract(EventContext ctx) {
    // Story gate check [PATCH]
    if (storyGate != null && !storyGate.isOpen(ctx)) return;

    if (lockState == DoorLockState.LOCKED) {
        ctx.audioSystem.playSfx("audio/sfx/door/locked.wav", hingePosition, 0.8f);
        return;
    }
    if (lockState == DoorLockState.KEYCARD_REQUIRED) {
        if (!ctx.inventorySystem.hasItem(requiredKeycardId)) {
            ctx.audioSystem.playSfx("audio/sfx/door/locked.wav", hingePosition, 0.8f);
            ctx.dialogueSystem.showSubtitle("You need a keycard.", 2.0f);
            return;
        }
    }
    targetAngle = currentAngle < 45f ? 90f : 0f;
    // Notify story of interaction
    ctx.storySystem.onInteraction(id, ctx);
    // Notify behavior tracker
    ctx.behaviorSystem.tracker().onPlayerInteracted();
}
```

#### `DoorCreakSystem` — speed → creak volume and pitch

```java
public final class DoorCreakSystem {
    private static final float CREAK_SPEED_MIN  = 5f;
    private static final float CREAK_SPEED_MAX  = 100f;
    private final Map<String, Float> creakCooldowns = new HashMap<>();

    public void update(String doorId, float angularSpeed, Vector3 doorPos,
                       Vector3 playerPos, float delta, EventContext ctx) {
        creakCooldowns.merge(doorId, -delta, Float::sum);
        if (creakCooldowns.getOrDefault(doorId, 0f) > 0f) return;

        float t      = MathUtils.clamp((angularSpeed - CREAK_SPEED_MIN) / (CREAK_SPEED_MAX - CREAK_SPEED_MIN), 0f, 1f);
        float volume = MathUtils.lerp(0.1f, 1.0f, t);
        float pitch  = MathUtils.lerp(0.8f, 1.4f, t);

        String wavPath = t < 0.33f ? "audio/sfx/door/creak_slow.wav"
                       : t < 0.66f ? "audio/sfx/door/creak_medium.wav"
                                   : "audio/sfx/door/creak_fast.wav";
        ctx.audioSystem.playSpatialSfx(wavPath, SoundEvent.DOOR_CREAK, doorPos, volume, t);
        // Behavior tracker
        ctx.behaviorSystem.tracker().onDoorOpened(angularSpeed);
        creakCooldowns.put(doorId, MathUtils.lerp(0.5f, 0.1f, t));
    }
}
```

---

## 8. Raycast Interaction System

### What it does

Every frame, cast a ray from the camera center forward. If the ray hits a registered `Interactable` within `MAX_INTERACT_DISTANCE`, show a prompt on the HUD. When the player presses F, call `onInteract()`.

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

    public InteractionResult update(Vector3 rayOrigin, Vector3 rayDirection,
                                     TriggerEvaluationContext evalCtx) {
        Vector3 rayEnd = new Vector3(rayOrigin).add(
            new Vector3(rayDirection).scl(MAX_INTERACT_DISTANCE));
        ClosestRayResultCallback cb = new ClosestRayResultCallback(rayOrigin, rayEnd);
        physicsWorld.rayTest(rayOrigin, rayEnd, cb);
        if (!cb.hasHit()) { cb.dispose(); return InteractionResult.NONE; }
        Vector3 hitPoint = cb.getHitPointWorld(new Vector3());
        cb.dispose();
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
        // notify behavior tracker
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
// Called by AtmosphereEffect.apply()
public void setAmbientMultiplier(float mult) { currentAmbientMult = mult; }
```

---

## 10. Sanity / Fear System

### What it does

A `sanity` meter (0–100) that decreases from darkness, enemy proximity, and scripted events. Drives visual distortions, audio glitches, hallucinations, and now footprint visibility through the tracking system.

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
  hallucination/                 ← added in System 21
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
// Called by JumpScareTimingEffect.apply()
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
// In a custom EventAction:
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

### What it does

Saves player state, event flags, inventory, and last-reached checkpoint to a JSON file. Supports multiple save slots.

**[PATCH: Addendum §4]** — `SaveData.mapName` is always `"resonance-full-map"` since there is one map. The `checkpointId` is the meaningful location identifier.

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
    public String   mapName       = "resonance-full-map";  // [PATCH]
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

**[PATCH: Addendum §5]** — new system, replaces the "scene transition as story gating" pattern. Enforces linear narrative progression.

### What it does

Sits above the Event System and enforces **linear narrative progression**. Players cannot trigger story-chapter events out of order, cannot skip cutscene beats, and cannot interact with story-gated objects until the required story step is reached.

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
    private final String         id;
    private final CompletionMode completionMode;
    private final String         completionParameter;
    private final String         onCompleteEventId;
    private final boolean        blockingUntilComplete;
    private boolean              completed = false;

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
}
```

#### `StorySystem`

```java
public final class StorySystem {
    public boolean hasReached(String chapterId, String beatId) { ... }
    public boolean isChapterActive(String chapterId) { ... }
    public void onEventFired(String eventId, EventContext ctx) { ... }
    public void onInteraction(String interactableId, EventContext ctx) { ... }
    public void onZoneEntered(String zoneId, EventContext ctx) { ... }
    public String debugStatus() { ... }
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

#### Story JSON format — `assets/story/story.json`

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
        }
      ]
    }
  ]
}
```

---

## 15. Room Transition System

**[PATCH: Addendum §4]** — Replaces `SceneTransitionSystem` / `TransitionScreen`. Since there is only one map, "transitions" are just a fade-to-black + fade-in when the player crosses a `ROOM_BOUNDARY` trigger. No scene loading occurs.

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
            if (fadeAlpha >= 1f) {
                fadingOut = false;
                fadingIn  = true;
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

---

# Group D — Developer Tools

---

## 16. In-Game Debug Console

### What it does

A text input overlay (press backtick to open) that accepts typed commands at runtime. Includes story and behavior commands from all addendum plans.

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
    BeatCommand.java        ← story debug [PATCH]
    ChapterCommand.java     ← story debug [PATCH]
    StoryStatusCommand.java ← story debug [PATCH]
    BehaviorCommand.java    ← K-Means debug [K-Means]
    HallucinateCommand.java ← hallucination debug [Three Features]
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

---

## 17. Settings System

### What it does

Loads and saves player preferences. Covers audio volumes, graphics, controls, sensitivity, and the `HOLD_BREATH` keybind added for System 22.

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
    d.keybinds.put("HOLD_BREATH", Input.Keys.ALT_LEFT);  // [Three Features]
    return d;
}
```

---

## 18. Footstep Material System

### What it does

Plays different footstep WAV sets based on the surface the player is walking on. The `SurfaceMaterial` enum is shared with `FootprintEmitter` (System 20) which uses it to calculate footprint intensity.

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

#### Integration with breath system [Three Features]

```java
// FootstepSystem.update() — multiply volume by breath suppression:
float finalVolume = volume * breathSystem.footstepVolumeMult();
```

---

# Group E — Adaptive AI

---

## 19. Player Behaviour Classification (K-Means)

### What it does

The game silently watches how the player behaves, classifies them into one of four archetypes using K-means clustering, and subtly reshapes enemy AI, jump scare timing, difficulty, and atmosphere. The player never sees this system.

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
| Door open speed | 0.30 | 0.20 | 0.75 | 0.85 |
| Interaction rate | 0.55 | 0.75 | 0.40 | 0.20 |
| Sanity loss rate | 0.60 | 0.30 | 0.50 | 0.85 |
| Hide response | 0.15 | 0.40 | 0.70 | 0.05 |

### Ten Measured Features

| # | Feature | Raw metric | Normalized meaning |
|---|---------|-----------|-------------------|
| 1 | `movementSpeed` | Average velocity m/s | 0=still, 1=sprinting |
| 2 | `noiseLevel` | RMS of all player sound events | 0=silent, 1=very loud |
| 3 | `crouchRatio` | Fraction of move time crouching | 0=never, 1=always |
| 4 | `stationaryRatio` | Fraction of total time not moving | 0=always moving, 1=hiding |
| 5 | `lookAroundRate` | Camera angular velocity variance | 0=steady, 1=spinning |
| 6 | `flashlightToggleRate` | Toggles per minute | 0=never, 1=obsessive |
| 7 | `doorOpenSpeed` | Average door angular velocity | 0=careful, 1=slams |
| 8 | `interactionRate` | Interactions per minute | 0=ignores, 1=touches everything |
| 9 | `sanityLossRate` | Sanity drain per minute | 0=calm, 1=exposed |
| 10 | `hideResponseTime` | Seconds to stationary after scare | 0=freezes, 1=keeps running |

### `BehaviorSample`

```java
public record BehaviorSample(
    float averageVelocity, float averageNoiseRms, float crouchFraction,
    float stationaryFraction, float cameraAngularVariance, float flashlightTogglesPerMin,
    float averageDoorOpenSpeed, float interactionsPerMin, float sanityLostThisWindow,
    float hideResponseSeconds   // -1 if no scare this window
) {
    public static final BehaviorSample NEUTRAL =
        new BehaviorSample(1.5f, 0.2f, 0.2f, 0.2f, 30f, 0.5f, 40f, 2f, 3f, 2f);
}
```

### `KMeansClassifier`

```java
public final class KMeansClassifier {
    private static final int K         = 4;
    private static final int MAX_ITERS = 20;
    private static final int MIN_SAMPLES = 3;

    public ClassificationResult classify(List<BehaviorFeatureVector> samples) {
        if (samples.size() < MIN_SAMPLES) return ClassificationResult.neutral();
        // K-means iterations → assign → recompute centroids → check convergence
        // Classify using mean of most recent 6 samples (30 seconds)
        // Return dominant archetype + inverse-distance blend weights
    }
}
```

### `ClassificationResult`

```java
public record ClassificationResult(
    PlayerArchetype dominant,
    float[] distanceToCentroid,   // [PARANOID, METHODICAL, IMPULSIVE, PANICKED]
    float[] blendWeights          // sums to 1.0
) {
    public float weightOf(PlayerArchetype archetype) { return blendWeights[archetype.ordinal()]; }
    public static ClassificationResult neutral() { ... }
}
```

### Blending formula

```java
// All effect parameters use blend weights — never a hard switch:
float blended = result.weightOf(PARANOID)   * P_PARANOID
              + result.weightOf(METHODICAL) * P_METHODICAL
              + result.weightOf(IMPULSIVE)  * P_IMPULSIVE
              + result.weightOf(PANICKED)   * P_PANICKED;
```

### What each archetype drives

#### Enemy AI

| Archetype | Enemy behaviour |
|-----------|----------------|
| **Paranoid** | Longer investigation delay, uses fake-outs (appears briefly then retreats), wider hearing |
| **Methodical** | Wider vision FOV, hears farther, patrols thoroughly, no fake-outs |
| **Impulsive** | 0.5s investigation delay, slightly faster patrol, narrower FOV |
| **Panicked** | Fastest patrol, fastest response, narrowest FOV, unexpected approach directions |

```java
private static final float[] INVESTIGATION_DELAY = { 3.5f, 2.0f, 0.5f, 1.0f };
private static final float[] PATROL_SPEED_MULT   = { 1.0f, 1.3f, 1.1f, 1.4f };
private static final float[] VISION_FOV          = { 70f, 85f, 65f, 60f };
private static final float[] HEARING_RANGE_MULT  = { 1.0f, 1.4f, 0.8f, 0.9f };
private static final float[] FAKE_OUT_PROBABILITY= { 0.35f, 0.0f, 0.0f, 0.1f };
```

#### Jump Scare Timing

```java
private static final float[] MIN_SCARE_INTERVAL = { 60f, 50f, 30f, 75f };
private static final float[] TENSION_THRESHOLD  = { 60f, 75f, 55f, 85f };
```

#### Difficulty

```java
private static final float[] SOUND_INTENSITY_THRESHOLD = { 0.30f, 0.15f, 0.45f, 0.40f };
private static final float[] MEMORY_DURATION_SECONDS   = { 8f, 14f, 5f, 6f };
private static final float[] SANITY_DRAIN_MULT         = { 1.1f, 1.0f, 1.15f, 0.85f };
```

#### Atmosphere

| Archetype | World feels like... |
|-----------|-------------------|
| **Paranoid** | Small sounds appear more often; flashlight flickers more during tension |
| **Methodical** | Long silence stretches; darkness slightly heavier |
| **Impulsive** | Louder ambient bed; things creak as you pass |
| **Panicked** | Heartbeat undertone; high-frequency hiss in the ambience |

```java
private static final String[] AMBIENCE_TRACK = {
    "audio/ambience/paranoid_bed.wav",
    "audio/ambience/methodical_bed.wav",
    "audio/ambience/impulsive_bed.wav",
    "audio/ambience/panicked_bed.wav"
};
```

### `BehaviorSystem` — master controller

```java
public final class BehaviorSystem {
    private static final int MAX_WINDOW_SAMPLES = 60; // 5 minutes

    public void update(float delta, PlayerController player, PerspectiveCamera camera,
                       FlashlightController flashlight, SanitySystem sanity) {
        changeDetector.update(delta);
        tracker.update(delta, player, camera, flashlight, sanity);
        atmosphereEffect.update(currentResult, delta); // always ticks for smooth lerp
    }

    public ClassificationResult  currentResult()   { return currentResult; }
    public PlayerArchetype        currentArchetype() { return currentResult.dominant(); }
    public PlayerBehaviorTracker  tracker()          { return tracker; }
}
```

### `BehaviorChangeDetector`

Re-classification triggers when:
- Euclidean shift of recent mean ≥ 0.25 from last classified mean, AND
- At least 20 seconds have elapsed since last classification

This prevents twitchy enemy behaviour changes from moment-to-moment variation.

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

### Design intent

The enemy roughly follows where the player has been — but not perfectly. Prints fade over time. A crouching player leaves faint traces. A sprinting player leaves a highway. The enemy is tracking a ghost of your movement, not a GPS signal.

### Package: `enemy/tracking`

```
enemy/tracking/
  FootprintTrail.java
  Footprint.java
  FootprintEmitter.java
  FootprintDetector.java
```

### `Footprint`

```java
public final class Footprint {
    public final Vector3 position;
    public final float   intensity;  // 0..1
    public float         age;        // seconds since stamped

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

### `FootprintTrail`

```java
public final class FootprintTrail {
    private static final float STAMP_INTERVAL_SECONDS = 0.8f;
    private static final float PRINT_LIFETIME_SECONDS = 90f;
    private static final int   MAX_PRINTS             = 120;

    public void update(float delta, Vector3 playerPosition, float printIntensity, boolean playerIsMoving) { ... }

    public List<Footprint> findNear(Vector3 searchCenter, float searchRadius, float minIntensity) {
        // Returns prints within radius above threshold, sorted newest-first
    }
}
```

### `FootprintEmitter` — sits in player update path

```java
public final class FootprintEmitter {
    public void update(float delta, PlayerController player, SurfaceMaterial currentSurface) {
        if (!player.isGrounded()) return;
        float speedNorm       = Math.min(1f, player.getHorizontalSpeed() / 5.5f);
        float postureMultiply = player.isCrouching() ? 0.25f : 1.0f;
        float surfaceMult     = SURFACE_MULT.getOrDefault(currentSurface, 0.5f);
        float intensity       = speedNorm * postureMultiply * surfaceMult;
        trail.update(delta, player.getPosition(), intensity, player.isMoving());
    }
}
```

### `FootprintDetector` — enemy-side

```java
public final class FootprintDetector {
    private static final float DETECTION_RADIUS    = 4.5f;
    private static final float DETECTION_THRESHOLD = 0.18f;
    private static final float MAX_DRIFT_DEGREES   = 22f;

    public Vector3 findNextDirection(Vector3 enemyPosition) {
        // Find newest nearby print → compute direction → apply angular drift
        // Drift = MAX_DRIFT_DEGREES × (1 - effectiveIntensity)
        // Faint prints cause more drift; bright prints cause less
    }

    public boolean hasDetectablePrints(Vector3 enemyPosition) { ... }
}
```

### `TrailFollowState`

```java
public final class TrailFollowState implements EnemyState {
    private static final float COLD_TIMEOUT_SECONDS = 5f;
    private static final float TRAIL_SPEED_MULT     = 0.75f;

    @Override
    public StateId update(float delta, EnemyAIContext ctx) {
        if (ctx.perception.canSeePlayer(...)) return StateId.CHASE;
        Vector3 moveDir = detector.findNextDirection(ctx.position);
        if (moveDir == null) {
            coldTimer += delta;
            if (coldTimer >= COLD_TIMEOUT_SECONDS) return StateId.INVESTIGATE;
        } else {
            coldTimer = 0f;
            ctx.navigator.setDirectDirection(moveDir, TRAIL_SPEED_MULT);
        }
        return StateId.TRAIL_FOLLOW;
    }
}
```

### K-Means integration

`DifficultyEffect` adjusts `DETECTION_THRESHOLD` per archetype:

```java
// Methodical player = enemy detects even faint prints (they're careful but traceable)
// Panicked player   = enemy misses faint prints (their chaos makes tracking harder)
private static final float[] PRINT_DETECTION_THRESHOLD = { 0.18f, 0.12f, 0.20f, 0.25f };
//                                                    PARANOID METHODICAL IMPULSIVE PANICKED
```

---

## 21. Sanity Hallucinations

### Design intent

Below sanity 50 the player hears sounds that aren't real — fake footsteps, door creaks, enemy growls — using the same WAV files as the real enemy. These **never enter the propagation graph**. The real enemy does not react to them.

### Package: `sanity/hallucination`

```
sanity/hallucination/
  HallucinationDirector.java
  HallucinationEvent.java
  HallucinationType.java
  AudioHallucination.java
  VisualHallucination.java
```

```java
public enum HallucinationType {
    FAKE_FOOTSTEP, FAKE_DOOR_CREAK, FAKE_ENEMY_BREATH,
    FAKE_ENEMY_GROWL, FAKE_ITEM_FALL, VISUAL_FLICKER
}
```

### Three hallucination tiers

| Tier | Sanity range | Interval | What fires |
|------|-------------|----------|-----------|
| MILD | 25–50 | 17.5–35s | FAKE_FOOTSTEP, FAKE_DOOR_CREAK |
| HEAVY | 10–25 | 7–14s | All audio types |
| SEVERE | 0–10 | 2.8–5.6s | Everything including VISUAL_FLICKER |

### `HallucinationDirector`

```java
public final class HallucinationDirector {
    // Hallucination thresholds modified by archetype (from K-Means DifficultyEffect)
    // PARANOID: starts at 60; METHODICAL: 50; IMPULSIVE/PANICKED: 40
    private float hallucinationThreshold = 50f;

    public void update(float delta, SanitySystem sanity, Vector3 playerPosition, Vector3 playerForward) {
        if (sanity.getSanity() >= hallucinationThreshold) return;
        // Countdown → trigger → pick type → pick fake position → play via audio.playSfx() ONLY
        // NEVER inject into propagation graph
    }

    /** Position strategy:
     *  MILD   — behind/sides, 10–20m
     *  HEAVY  — anywhere, 6–15m
     *  SEVERE — very close (2–5m), peripheral vision
     */
    private Vector3 pickFakePosition(HallucinationTier tier, Vector3 origin, Vector3 forward) { ... }
}
```

### The one tell

Fake sounds never cause the propagation graph to react. If the player has any acoustic visualiser open, hallucination-generated sounds produce no pulse rings. In normal horror gameplay this is invisible.

### K-Means integration

```java
// HallucinationDirector.setThreshold() called by DifficultyEffect:
private static final float[] HALLUCINATION_THRESHOLD = { 60f, 50f, 40f, 40f };
// PARANOID METHODICAL IMPULSIVE PANICKED
```

---

## 22. Breath-Hold Mechanic

### Design intent

The player holds Left Alt to suppress breathing noise. Footstep sounds get quieter, mic propagation is muffled, and the enemy's effective hearing radius is reduced. But breath is finite — hold too long and the forced exhale is loud enough to be heard.

### Package: `player`

```
player/
  BreathSystem.java
  BreathHudRenderer.java
```

### States

```
NORMAL
  └─ (hold key) → HOLDING
HOLDING
  └─ (stamina → 0) → FORCED_EXHALE   ← loud, involuntary, propagation graph event
  └─ (key released) → RECOVERING
FORCED_EXHALE
  └─ (0.8s) → RECOVERING
RECOVERING
  └─ (stamina full) → NORMAL
  └─ (hold key AND stamina > 20%) → HOLDING
```

### `BreathSystem`

```java
public final class BreathSystem {
    private static final float MAX_STAMINA        = 100f;
    private static final float DRAIN_PER_SECOND   = 22f;    // ~4.5s at full
    private static final float RECOVER_PER_SECOND = 11f;    // ~9s from empty
    private static final float MIN_HOLD_STAMINA   = 20f;
    private static final float EXHALE_DURATION    = 0.8f;

    // Effect values while holding
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

### Forced exhale — propagation graph injection

```java
private void triggerForcedExhale(Vector3 playerPos, ...) {
    audio.playSfx("audio/sfx/player/breath_out_forced.wav", playerPos, 1.0f);
    // Loud exhale enters propagation graph — enemy hears this
    String nodeId = prop.findNearestNode(playerPos);
    SoundEventData data = new SoundEventData(
        SoundEvent.ITEM_IMPACT, nodeId, playerPos, 0.75f, elapsed
    );
    prop.emitSoundEvent(data, elapsed);
    sanity.applyDelta(-5f); // panic shock
}
```

### Integration points

```java
// FootstepSystem — multiply by breath suppression:
float finalVolume = volume * breathSystem.footstepVolumeMult();

// Mic propagation path:
float rms = realtimeMicSystem.getNormalizedLevel();
rms *= breathSystem.micSuppressionMult();
if (rms > VOICE_DETECTION_THRESHOLD) { /* propagate MIC_VOICE */ }

// EnemyPerception.onSoundHeard():
float effectiveIntensity = data.baseIntensity() * breathSystem.enemyHearingMult();
if (effectiveIntensity < MIN_AUDIBLE_INTENSITY) return;

// PlayerBehaviorTracker — breath reduces noise metric:
float rmsContribution = currentNoiseEvent * breathSystem.micSuppressionMult();
noiseAccum = Math.max(noiseAccum, rmsContribution);
```

### Breath stamina HUD

Small arc indicator near crosshair — only visible while not at full stamina. Color shifts blue→red as stamina drops.

### K-Means integration

A player who holds breath constantly will have lower `noiseLevel` samples → K-Means pushes them toward METHODICAL or PARANOID.

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
    │ position: 8m to the left of the player
    │ → plays enemy/footstep_02.wav spatially
    │ → does NOT inject into propagation graph
    │ → player hears "footsteps" from the left
    │
    └─ Player panics, releases Alt key
         └─ BreathSystem → RECOVERING
              └─ Footstep volume returns to 100%
              └─ enemyHearingMult returns to 1.0

  If player held too long:
    └─ Stamina → 0 → FORCED_EXHALE
         └─ Loud exhale WAV plays at volume 1.0
         └─ ITEM_IMPACT injected at intensity 0.75 → propagation graph
              └─ EnemyPerception.onSoundHeard(): 0.75 × 1.0 = 0.75 > 0.18
                   └─ EnemyStateMachine → INVESTIGATE
                        └─ Moves directly to player's position

  Simultaneously:
  BehaviorSystem samples after 5s
    │ stationaryFraction: high (hiding)
    │ noiseLevel: very low (breath suppressed)
    │ crouchRatio: 1.0
    │ → K-Means: dominant = METHODICAL
         └─ EnemyBehaviorEffect.apply():
              └─ enemy hearing range × 1.4 (harder to lose this enemy)
              └─ investigation delay = 2.0s
              └─ no fake-outs (methodical player reads them)
         └─ AtmosphereEffect:
              └─ crossfades to methodical_bed.wav (4s, imperceptible)
              └─ ambient light slowly dims (takes 30s to shift)
```

---

# Complete Package Layout

```
core/src/main/java/io/github/superteam/resonance/
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
      CheckpointAction.java
      StoryEventAction.java
      LogAction.java

  audio/
    GameAudioSystem.java
    AudioChannel.java
    SoundBank.java
    AmbienceTrack.java
    SpatialSfxEmitter.java
    WorldSoundEmitter.java
    AudioChannelConfig.java
    HearingCategory.java         ← [PATCH]

  sound/
    SoundEvent.java              ← updated with HearingCategory field

  trigger/
    Trigger.java                 ← add requiredChapterId/requiredBeatId fields
    TriggerEvaluator.java        ← add story gate check
    TriggerEvaluationContext.java
    conditions/
      ZoneTrigger.java
      InteractionTrigger.java
      TimerTrigger.java
      StateTrigger.java
      CompoundTrigger.java

  map/
    MapCollisionBuilder.java     ← add debugTriangles to BvhResult
    MapLoader.java
    LoadedMap.java
    MapDocument.java
    MapObject.java
    MapObjectType.java           ← add ROOM_BOUNDARY
    MapDocumentLoader.java
    MapDocumentSerializer.java

  enemy/
    EnemyController.java         ← add behavior-driven setters
    EnemyStateMachine.java       ← add TRAIL_FOLLOW state
    EnemyPerception.java         ← add hearing filter + breath mult
    EnemyNavigator.java
    EnemyAnimator.java
    NavMesh.java
    NavMeshBuilder.java          ← group waypoints by room tag
    states/
      IdleState.java
      PatrolState.java
      InvestigateState.java      ← check for prints → TRAIL_FOLLOW
      TrailFollowState.java
      ChaseState.java
      AttackState.java
      StunnedState.java
    tracking/
      FootprintTrail.java
      Footprint.java
      FootprintEmitter.java
      FootprintDetector.java

  interactable/
    Interactable.java            ← add storyGate() optional return
    InteractableRegistry.java
    door/
      DoorController.java        ← check StoryGate, notify BehaviorTracker
      DoorCreakSystem.java       ← notify BehaviorTracker
      DoorLockState.java
    objects/
      CabinetController.java
      DrawerController.java
      SwitchController.java
      KeycardPickup.java

  interaction/
    RaycastInteractionSystem.java ← notify StorySystem + BehaviorTracker
    InteractionPromptRenderer.java
    InteractionResult.java

  lighting/
    LightManager.java            ← add setAmbientMultiplier()
    GameLight.java
    FlickerController.java
    FlashlightController.java    ← notify BehaviorTracker on toggle
    LightReactorListener.java

  sanity/
    SanitySystem.java            ← add setDrainMultiplier(), getLastDeltaThisFrame()
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

  scare/
    JumpScareDirector.java       ← add K-Means setters
    JumpScare.java
    ScareType.java
    scripted/
      AudioOnlyScare.java
      FlashScare.java
      EnemyAppearScare.java
      HallucinationScare.java
      EnvironmentScare.java

  dialogue/
    DialogueSystem.java
    DialogueLine.java
    DialogueSequence.java
    DialogueLoader.java
    SubtitleRenderer.java

  save/
    SaveSystem.java
    SaveData.java
    CheckpointManager.java
    SaveSlot.java

  story/
    StorySystem.java
    StoryChapter.java
    StoryBeat.java
    StoryBeatCondition.java
    StoryLoader.java
    StoryGate.java
    StoryEventAction.java

  transition/
    RoomTransitionSystem.java
    FadeOverlay.java

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

  settings/
    SettingsSystem.java
    SettingsData.java            ← add HOLD_BREATH keybind
    KeybindRegistry.java

  footstep/
    FootstepSystem.java          ← apply breath volume mult
    FootstepSoundSet.java
    SurfaceMaterial.java

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

  player/
    BreathSystem.java
    BreathHudRenderer.java

lwjgl3/src/main/java/io/github/superteam/resonance/
  mapeditor/
    MapEditorPanel.java
    ObjectPalette.java           ← add ROOM_BOUNDARY
    SceneOutlinePanel.java       ← add room filter search box
    ObjectPropertyPanel.java
    MapEditorIntegration.java
  devTest/
    CollisionWireframeRenderer.java   ← [PATCH]
    ModelDebugScreen.java             ← K toggle + SceneManager path [PATCH]

assets/
  audio/
    music/
    ambience/
      paranoid_bed.wav  methodical_bed.wav  impulsive_bed.wav  panicked_bed.wav
      cave_drip.wav  wind_exterior.wav
    sfx/
      env/  player/  items/  enemy/  door/  ui/
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

Build in strict dependency order. All addendum patches are integrated at their natural positions.

| # | System | Depends on | Priority |
|---|--------|-----------|----------|
| 1 | **Settings System** | Nothing | 🔴 First |
| 2 | **Keybind Registry** (+ `HOLD_BREATH` binding) | Settings | 🔴 First |
| 3 | **`HearingCategory` + `SoundEvent` tag update** | `SoundEvent` enum | 🔴 First — before any enemy work |
| 4 | **Map Loader + BVH Collision** (+ `debugTriangles` field) | ModelAssetManager, Bullet | 🔴 Foundation |
| 5 | **GameAudioSystem + SoundBank** | LibGDX Audio | 🔴 Foundation |
| 6 | **SpatialSfxEmitter** (+ ambience graph-skip guard) | GameAudioSystem, PropOrchestrator | 🔴 Foundation |
| 7 | **ModelDebugScreen: texture fix** (SceneManager path) | gdx-gltf | 🟡 Dev tool |
| 8 | **ModelDebugScreen: collision wireframe** (K toggle) | `MapCollisionBuilder.debugTriangles` | 🟡 Dev tool |
| 9 | **Footstep Material System** (`SurfaceMaterial` shared with footprints) | SpatialSfxEmitter, Bullet | 🟡 Early |
| 10 | **EventState + EventBus + EventRegistry** | Nothing | 🔴 Foundation |
| 11 | **GameEvent + all EventActions** | EventBus, GameAudioSystem | 🔴 Foundation |
| 12 | **EventLoader (JSON)** | GameEvent | 🟡 Early |
| 13 | **StorySystem + StoryChapter + StoryBeat + StoryLoader** | EventBus, EventState | 🟡 Early — before triggers |
| 14 | **StoryGate + StoryEventAction** | StorySystem | 🟡 Early |
| 15 | **Trigger System** (+ story gate check) | EventBus, StorySystem | 🟡 Early |
| 16 | **Raycast Interaction System** | Bullet, InteractableRegistry | 🟡 Early |
| 17 | **Door & Interactable System** (+ StoryGate, BehaviorTracker hooks) | RaycastInteraction, StorySystem | 🟡 Early |
| 18 | **Map Editor (Swing)** (+ `ROOM_BOUNDARY` type, room filter) | MapDocument | 🟡 Early |
| 19 | **Map Document Loader** (+ `ROOM_BOUNDARY` handling) | AudioSystem, TriggerEvaluator | 🟡 Early |
| 20 | **Debug Console** (+ story + behavior + hallucinate commands) | EventBus, StorySystem stub | 🟡 Dev tool |
| 21 | **Dialogue + Subtitle System** | GameAudioSystem | 🟢 Mid |
| 22 | **Save / Checkpoint System** | EventState, DialogueSystem | 🟢 Mid |
| 23 | **RoomTransitionSystem** (replaces SceneTransition) | FadeOverlay, DialogueSystem | 🟢 Mid |
| 24 | **Dynamic Lighting System** (+ `setAmbientMultiplier()`) | WorldShader uniforms | 🟢 Mid |
| 25 | **Sanity / Fear System** (+ drain multiplier, delta getter) | LightManager | 🟢 Mid |
| 26 | **Hallucination System** (`HallucinationDirector`) | SanitySystem, GameAudioSystem | 🟢 Mid |
| 27 | **Enemy AI — Perception** (filtered by `HearingCategory` + breath mult) | Bullet, PropGraph | 🟠 Complex |
| 28 | **Enemy AI — NavMesh + Navigator** (single-map, room-tagged waypoints) | MapDocument | 🟠 Complex |
| 29 | **Enemy AI — State Machine + Animator** | Perception, Navigator | 🟠 Complex |
| 30 | **Footprint Tracking** (`FootprintTrail`, `FootprintEmitter`, `FootprintDetector`, `TrailFollowState`) | SurfaceMaterial, EnemyStateMachine | 🟠 Complex |
| 31 | **Jump Scare Director** (+ K-Means setters) | SanitySystem, EnemyController | 🟠 Complex |
| 32 | **Player Behaviour Classification (K-Means)** | All behavior hooks (footstep, door, flashlight, interaction, scare, sanity) | 🔵 Integration |
| 33 | **Breath-Hold Mechanic** (`BreathSystem`, `BreathHudRenderer`) | FootstepSystem, EnemyPerception, SanitySystem | 🔵 Integration |
| 34 | **Wire all systems into `GltfMapTestScene`** | All above | 🔵 Integration |

---

# Acceptance Criteria

## Foundation

| System | Test | Pass |
|--------|------|------|
| **Settings** | Change sensitivity, relaunch | Persists |
| **Keybinds** | Rebind INTERACT to G, press near door | Door opens |
| **Map BVH** | Load `resonance-map.gltf` | Player walks accurately on geometry |
| **Audio** | `setAmbience("cave_drip.wav", 2.0f)` | Loops seamlessly, crossfades |
| **Spatial SFX** | Play at 20m vs 2m | 20m significantly quieter |
| **Spatial SFX + graph** | SFX with non-AMBIENCE graphEvent | Enemy director reacts |
| **Hearing filter** | `WorldSoundEmitter` drip fires | Enemy does NOT investigate |
| **Hearing filter** | `AmbienceTrack` plays | Enemy does NOT react |
| **Hearing filter** | Player throws item → `ITEM_THROW` | Enemy investigates position |
| **Hearing filter** | Player mic above threshold | Enemy hears `MIC_VOICE` |
| **Hearing filter** | `ALARM` event fires | Enemy hears it and reacts |

## Map & Editor

| System | Test | Pass |
|--------|------|------|
| **Collision wireframe** | Load `-map.gltf`, press K | Green wireframe covers exact visual geometry |
| **Collision wireframe** | Load non-map gltf, press K | No wireframe |
| **Collision wireframe** | Press K again | Wireframe toggles off |
| **Texture fix** | Load `.gltf` with external textures | Textures visible |
| **Texture fix** | Load `.glb` with embedded textures | Textures visible |
| **Texture fix** | Load `.g3dj` with textures | Textures visible |
| **Single map** | Load `resonance-map.gltf` | All rooms present in one mesh |
| **Room boundary** | Walk through `ROOM_BOUNDARY` trigger | Fade-to-black, fade-in, room subtitle |
| **Map Editor** | Add DOOR, set position, save | `resonance-map.json` contains the door |
| **Map Editor** | Add ROOM_BOUNDARY, reload | Walking through triggers room fade |

## Core Gameplay

| System | Test | Pass |
|--------|------|------|
| **Footstep** | Walk on WOOD | Wood variant plays |
| **Footstep** | Crouch-walk | Quieter, still injected into graph |
| **Interaction** | Look at door, press F | Door opens; creak plays by speed |
| **Interaction** | Look at locked door, press F | Locked sound plays |
| **Door creak** | Shove door fast | Loud, high-pitch; graph injected |
| **Door creak** | Push slowly | Quiet, low-pitch |

## Narrative

| System | Test | Pass |
|--------|------|------|
| **Events** | Fire `"creak-then-groan"` | Sound + subtitle + flag set |
| **Events — chain** | Sequence event | Steps fire with correct delays |
| **Events — once** | ONCE event fires twice | Blocked on second fire |
| **Triggers — zone** | Walk into zone trigger | Target event fires |
| **Triggers — compound** | Flag not set | Does not fire |
| **Triggers — compound** | Flag set + in zone | Fires |
| **Story — gate** | Beat `read-first-note` not complete | Cannot interact with server door |
| **Story — gate** | Beat complete | Door becomes interactable |
| **Story — advance** | Interact with `note-reception-desk` | Beat completes, event fires |
| **Story — chapter** | All prologue beats done | `chapter-1` starts automatically |
| **Story — auto beat** | AUTOMATIC beat at chapter start | Completes instantly |
| **Story — debug** | `story` | Prints current chapter + beat |
| **Story — debug** | `beat read-first-note` | Force-completes, story advances |
| **Dialogue** | `showSubtitle("test", 3.0f)` | Text appears, fades after 3s |
| **Dialogue sequence** | Play 3-line sequence | Lines in order, no overlap |
| **Save** | Reach checkpoint | `autosave.json` written |
| **Save** | Load autosave | Position, sanity, inventory restored |

## Horror Atmosphere

| System | Test | Pass |
|--------|------|------|
| **Lighting** | Flickering light in map | Flickers with noise curve |
| **Lighting** | Loud `PHYSICS_COLLAPSE` fires | Nearby lights flicker 1.5s |
| **Flashlight** | Leave on 66s | Battery depletes, flicker starts |
| **Sanity** | Stand in dark room 10s | Sanity drops; vignette appears |
| **Sanity** | Enemy approaches to 5m | Sanity drops faster |
| **Sanity** | Enter safe room | Sanity recovers |
| **Sanity effect** | Sanity < 25 | Chromatic aberration visible |
| **Jump scare** | Tension > 70, sanity > 50 | AUDIO_ONLY scare fires |
| **Jump scare** | Sanity < 50, tension > 70 | ENEMY_APPEAR scare fires |
| **Jump scare** | Scare fires | MIN_SCARE_INTERVAL blocks next |

## Enemy AI

| System | Test | Pass |
|--------|------|------|
| **Patrol** | Enemy has waypoints | Walks route indefinitely |
| **Hearing** | Player loud noise | Enemy moves to position |
| **Sight** | Player in FOV, clear LOS | Enemy → CHASE |
| **Lost** | Player breaks LOS 5s | Enemy → INVESTIGATE → PATROL |
| **Stun** | Stun item hits enemy | STUNNED for N seconds |

## K-Means Behaviour

| System | Test | Pass |
|--------|------|------|
| **Tracker — motion** | Walk 30s | `movementSpeed` sample > 0 |
| **Tracker — quiet** | Crouch silently 30s | `noiseLevel` near 0, `crouchRatio` near 1 |
| **Tracker — flashlight** | Toggle 5× in one window | `flashlightToggleRate` high |
| **Normalizer** | Sprint (5.5 m/s) | Normalizes to 1.0 |
| **K-means** | < 3 samples | Returns `neutral()` |
| **K-means** | 20 pure-sprint samples | IMPULSIVE or PANICKED dominant |
| **K-means — blend** | Player at archetype midpoint | No weight exceeds 0.55 |
| **Change detector** | < 20s since last classify | No reclassify regardless of shift |
| **Change detector** | Crouch-walk → sprint | Reclassify triggers |
| **Change detector** | Minor speed change 0.2→0.3 | No reclassify |
| **Enemy effect** | Classify METHODICAL | Hearing range increased (log) |
| **Enemy effect** | Classify IMPULSIVE | Investigation delay < 1s |
| **Scare timing** | Classify PANICKED | Min interval ≥ 70s |
| **Atmosphere** | Classify METHODICAL | Crossfades to `methodical_bed.wav` |
| **Atmosphere** | Ambient light shift | Takes > 20s to fully change |
| **Debug overlay** | F9 | Shows correct archetype and bars |
| **Debug console** | `behavior force PANICKED` | Dominant changes immediately |
| **Debug console** | `behavior reset` | Sample count → 0 |
| **Session reset** | New game | All samples cleared, neutral METHODICAL |

## Footprint Tracking

| System | Test | Pass |
|--------|------|------|
| **Stamp** | Walk 5m | `FootprintTrail.size()` grows |
| **Expiry** | Wait 90s without moving | Trail clears to 0 |
| **Intensity — crouch** | Crouch-walk on carpet | Prints below 0.15 intensity |
| **Intensity — sprint** | Sprint on metal | Prints above 0.70 intensity |
| **Imperfect** | Enemy follows trail | Visually wobbles, not exact path |
| **Drift scales** | Faint prints | More drift than bright prints |
| **Cold trail** | No prints 5s in TRAIL_FOLLOW | Enemy → INVESTIGATE |
| **State flow** | Enemy INVESTIGATE, prints nearby | → TRAIL_FOLLOW |
| **Detection radius** | Print 6m away (> 4.5m) | Not detected |

## Hallucinations

| System | Test | Pass |
|--------|------|------|
| **Threshold** | Sanity = 55 | No hallucinations |
| **Mild** | Sanity = 40 | Only FAKE_FOOTSTEP or FAKE_DOOR_CREAK |
| **Severe** | Sanity = 5 | VISUAL_FLICKER possible, sounds very close |
| **No graph** | Hallucination plays | Enemy does NOT investigate |
| **Position** | MILD hallucination | Originates from behind/sides |
| **Archetype** | PARANOID | Hallucinations start at sanity 60, not 50 |
| **Debug** | `hallucinate FAKE_FOOTSTEP` | Fires immediately |

## Breath-Hold

| System | Test | Pass |
|--------|------|------|
| **Hold** | Press Alt | `isHolding()` true, stamina decreasing |
| **Footstep suppress** | Hold Alt walking | Volume ≈ 30% normal |
| **Mic suppress** | Hold Alt | `micSuppressionMult()` = 0.15 |
| **Enemy deaf** | Hold Alt, walk past enemy | Enemy does not investigate |
| **Forced exhale** | Hold until stamina = 0 | Loud exhale plays; graph event fires |
| **Forced exhale enemy** | Exhale in range | Enemy → INVESTIGATE |
| **Recovery** | Release Alt | Stamina recovers ~11pts/sec |
| **HUD visible** | Stamina < 100% | Arc appears near crosshair |
| **HUD color** | Stamina drops | Arc shifts blue → red |
| **HUD hidden** | Stamina = 100%, not holding | Arc invisible |

## Interaction — All Three Systems Together

| System | Test | Pass |
|--------|------|------|
| **Triple interaction** | Hide holding breath at sanity 20 | Enemy loses trail, hallucinations fire, HUD shows arc |
| **K-Means + breath** | Hold breath constantly for 5 minutes | K-Means pushes toward METHODICAL |
| **K-Means + hallucinations** | PARANOID archetype + sanity 55 | Hallucinations start at 60, not 50 |
| **Breath + footprints** | Hold breath, crouch-walk | Prints below detection threshold on most surfaces |

## Debug Console

| System | Test | Pass |
|--------|------|------|
| **Open** | Press backtick | Overlay opens |
| **Teleport** | `tp 5 0 5` | Player teleports |
| **Fire event** | `fire alarm-trigger` | Event fires immediately |
| **Set sanity** | `sanity 10` | Sanity set, effects activate |
| **Map Editor** | Open via F12 | Swing window opens without freezing |

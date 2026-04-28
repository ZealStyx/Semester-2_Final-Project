# Resonance — Master Systems Plan

**Project:** `io.github.superteam.resonance`  
**Scope:** `core` + `lwjgl3` modules  
**Total systems:** 13

---

## Table of Contents

### Group 0 — Foundation (already planned)
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
14. [Scene / Level Transition System](#14-scene--level-transition-system)

### Group D — Developer Tools
15. [In-Game Debug Console](#15-in-game-debug-console)
16. [Settings System](#16-settings-system)
17. [Footstep Material System](#17-footstep-material-system)

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
    ShowSubtitleAction.java      ← links to System 12
    SetSanityDeltaAction.java    ← links to System 10
    TriggerJumpScareAction.java  ← links to System 11
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

```java
public final class EventContext {
    public final Vector3                      triggerPosition;
    public final Vector3                      playerPosition;
    public final float                        elapsedSeconds;
    public final SoundPropagationOrchestrator propagationOrchestrator;
    public final GameAudioSystem              audioSystem;
    public final EventBus                     eventBus;
    public final EventState                   state;
    public final SanitySystem                 sanitySystem;    // System 10
    public final JumpScareDirector            jumpScareDirector; // System 11
    public final DialogueSystem               dialogueSystem;  // System 12
    public final DebugConsole                 debugConsole;    // System 15
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
      { "type": "SET_SANITY",      "delta": -5.0 }
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

    @Override
    public void dispose() {
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

    public void play(String path, float volume) { ... }

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

```java
public final class SpatialSfxEmitter {
    private static final float MAX_AUDIBLE_DISTANCE = 30f;

    public void playAndPropagate(String wavPath, SoundEvent graphEvent,
                                  Vector3 emitterPos, Vector3 listenerPos,
                                  float baseVolume, float graphIntensity,
                                  float elapsedSeconds,
                                  Function<Vector3, String> nodeResolver) {
        float distance = emitterPos.dst(listenerPos);
        float atten = Math.max(0f, 1f - (distance / MAX_AUDIBLE_DISTANCE));
        float finalVol = baseVolume * atten * atten;
        float pan = MathUtils.clamp(emitterPos.x - listenerPos.x, -1f, 1f);

        if (finalVol > 0.01f) soundBank.sound(wavPath).play(finalVol, 1.0f, pan);

        if (graphEvent != null && nodeResolver != null) {
            String nodeId = nodeResolver.apply(emitterPos);
            SoundEventData data = new SoundEventData(graphEvent, nodeId,
                emitterPos, graphIntensity, elapsedSeconds);
            propagationOrchestrator.emitSoundEvent(data, elapsedSeconds);
        }
    }
}
```

#### Asset convention

```
assets/audio/
  music/
  ambience/
    cave_drip.wav
    wind_exterior.wav
  sfx/
    env/  player/  items/  enemy/  door/
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
    public final SanitySystem     sanitySystem;     // System 10
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

// Interaction — player presses F near object
public final class InteractionTrigger extends Trigger {
    private final Vector3 objectPosition;
    private final float maxDistance;
    public boolean evaluate(TriggerEvaluationContext ctx) {
        return ctx.isInteractPressed
            && ctx.playerPosition.dst2(objectPosition) <= maxDistance * maxDistance;
    }
}

// Timer — fires after N seconds
public final class TimerTrigger extends Trigger {
    private final float targetSeconds;
    public boolean evaluate(TriggerEvaluationContext ctx) {
        return ctx.elapsedSeconds >= targetSeconds;
    }
}

// State — flag, item count, fire count
public final class StateTrigger extends Trigger {
    public enum Condition { FLAG_SET, ITEM_IN_INVENTORY, EVENT_FIRED_COUNT, SANITY_BELOW }
    // ...
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

#### JSON trigger format

```json
[
  {
    "id": "enter-server-room",
    "type": "ZONE",
    "targetEvent": "creak-then-groan",
    "cooldownSeconds": 10.0,
    "volume": { "minX": -5, "minY": 0, "minZ": -5, "maxX": 5, "maxY": 3, "maxZ": 5 }
  },
  {
    "id": "low-sanity-hallucination",
    "type": "COMPOUND",
    "mode": "AND",
    "targetEvent": "play-hallucination",
    "cooldownSeconds": 30.0,
    "children": [
      { "type": "STATE", "condition": "SANITY_BELOW", "threshold": 30 },
      { "type": "ZONE",  "volume": { "minX": -20, "minY": 0, "minZ": -20, "maxX": 20, "maxY": 5, "maxZ": 20 } }
    ]
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

#### `MapCollisionBuilder`

```java
public final class MapCollisionBuilder {

    public static BvhResult build(ModelData mapData) {
        btTriangleMesh triMesh = new btTriangleMesh();
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
            }
        }

        btBvhTriangleMeshShape shape =
            new btBvhTriangleMeshShape(triMesh, true, true);
        btDefaultMotionState motionState = new btDefaultMotionState(new Matrix4().idt());
        btRigidBody.btRigidBodyConstructionInfo ci =
            new btRigidBody.btRigidBodyConstructionInfo(0f, motionState, shape, Vector3.Zero);
        btRigidBody body = new btRigidBody(ci);
        ci.dispose();

        return new BvhResult(triMesh, shape, motionState, body);
    }

    public record BvhResult(btTriangleMesh triMesh, btBvhTriangleMeshShape shape,
                             btDefaultMotionState motionState, btRigidBody body) {
        public void dispose() {
            body.dispose(); motionState.dispose(); shape.dispose(); triMesh.dispose();
        }
    }
}
```

#### `MapLoader`

```java
public final class MapLoader {
    public static boolean isMapFile(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith("-map.gltf") || lower.endsWith("-map.glb");
    }

    public static LoadedMap load(String path, ModelAssetManager modelAssetManager) {
        String assetKey = "map-" + path.hashCode();
        ModelData modelData = modelAssetManager.load(assetKey, path);
        SceneModel sceneModel = new SceneModel(modelData);
        sceneModel.setPosition(0f, 0f, 0f);
        MapCollisionBuilder.BvhResult bvh = MapCollisionBuilder.build(modelData);
        BoundingBox aabb = new BoundingBox();
        sceneModel.modelInstance().calculateBoundingBox(aabb);
        return new LoadedMap(assetKey, sceneModel, bvh, aabb, modelAssetManager);
    }
}
```

---

## 5. Map Editor — Swing Tool

### What it does

A separate Swing window for placing static props, trigger volumes, spawn points, sound emitters, and lights. Saves a JSON document that `MapDocumentLoader` reads at runtime.

### Package: `mapeditor` (in `lwjgl3`)

```
mapeditor/
  MapEditorPanel.java
  ObjectPalette.java
  SceneOutlinePanel.java
  ObjectPropertyPanel.java
  MapEditorIntegration.java
```

#### `MapObjectType`

```java
public enum MapObjectType {
    BOX_PROP, CYLINDER_PROP,
    DOOR, FURNITURE,
    TRIGGER_VOLUME, SPAWN_POINT,
    SOUND_EMITTER, LIGHT_POINT,
    DIALOGUE_TRIGGER,            // links to System 12
    CHECKPOINT,                  // links to System 13
    GLTF_PROP                    // future: external gltf reference
}
```

#### Map JSON format

```json
{
  "mapName": "Level 01 — Server Room",
  "gltfMapPath": "models/level01-map.gltf",
  "objects": [
    {
      "id": "spawn-player",
      "type": "SPAWN_POINT",
      "position": { "x": 0.0, "y": 0.0, "z": 0.0 },
      "properties": { "label": "default" }
    },
    {
      "id": "drip-emitter-01",
      "type": "SOUND_EMITTER",
      "position": { "x": 5.0, "y": 2.8, "z": 1.5 },
      "properties": {
        "wavPath": "audio/sfx/env/water_drip.wav",
        "graphEvent": "WATER_DRIP",
        "volume": "0.4",
        "minInterval": "3.0",
        "maxInterval": "7.0"
      }
    },
    {
      "id": "checkpoint-01",
      "type": "CHECKPOINT",
      "position": { "x": 10.0, "y": 0.0, "z": 0.0 },
      "properties": { "checkpointId": "server-room-entry" }
    }
  ]
}
```

#### Editor window layout

```
MapEditorPanel (JFrame 900×700)
├── JMenuBar  [File: New / Open / Save / Save As]
├── JToolBar  [New] [Open] [Save] | Snap: 0.25m
├── JSplitPane (HORIZONTAL)
│     ├── LEFT: ObjectPalette (140px)
│     │     ▸ Geometry   (BOX_PROP, CYLINDER_PROP)
│     │     ▸ Interactable (DOOR, FURNITURE)
│     │     ▸ Gameplay   (TRIGGER_VOLUME, SPAWN_POINT, CHECKPOINT)
│     │     ▸ Audio      (SOUND_EMITTER)
│     │     ▸ Narrative  (DIALOGUE_TRIGGER)
│     │     ▸ Visual     (LIGHT_POINT)
│     └── RIGHT: JSplitPane (VERTICAL)
│           ├── SceneOutlinePanel (list of all objects)
│           └── ObjectPropertyPanel (fields for selected object)
└── Status bar: "4 objects | Saved: level01.json"
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
  EnemyController.java          — master enemy controller, owns state machine
  EnemyStateMachine.java        — states: IDLE, PATROL, INVESTIGATE, CHASE, ATTACK, STUNNED
  EnemyState.java               — interface for individual states
  EnemyPerception.java          — hearing (graph) + sight (raycast cone)
  EnemyNavigator.java           — navmesh pathfinding wrapper
  EnemyAnimator.java            — drives model animation per state
  NavMesh.java                  — baked navigation mesh
  NavMeshBuilder.java           — builds nav mesh from -map.gltf geometry
  states/
    IdleState.java
    PatrolState.java
    InvestigateState.java
    ChaseState.java
    AttackState.java
    StunnedState.java
```

#### `EnemyStateMachine`

States and transitions:

```
IDLE ──(patrol timer expires)──► PATROL
PATROL ──(sound heard)──────────► INVESTIGATE
PATROL ──(player seen)──────────► CHASE
INVESTIGATE ──(nothing found)───► PATROL
INVESTIGATE ──(player seen)─────► CHASE
CHASE ──(player lost > 5s)──────► INVESTIGATE
CHASE ──(within attack range)───► ATTACK
ATTACK ──(done)─────────────────► CHASE
ANY ──(stun item hit)───────────► STUNNED
STUNNED ──(timer done)──────────► PATROL
```

```java
public final class EnemyStateMachine {
    public enum StateId { IDLE, PATROL, INVESTIGATE, CHASE, ATTACK, STUNNED }

    private final Map<StateId, EnemyState> states = new EnumMap<>(StateId.class);
    private EnemyState currentState;
    private StateId    currentStateId;

    public void transition(StateId next) {
        if (currentState != null) currentState.onExit(context);
        currentStateId = next;
        currentState = states.get(next);
        currentState.onEnter(context);
    }

    public void update(float delta) {
        if (currentState != null) {
            StateId next = currentState.update(delta, context);
            if (next != currentStateId) transition(next);
        }
    }
}
```

#### `EnemyPerception`

```java
public final class EnemyPerception {

    // --- Hearing ---
    // The propagation graph already delivers SoundEventData to EnemyHearingTarget.
    // EnemyPerception reads the most recent unhandled event and stores its position
    // as a "last heard position" for the InvestigateState to pathfind toward.

    private Vector3  lastHeardPosition;
    private float    lastHeardIntensity;
    private boolean  hasUnhandledSound;

    public void onSoundHeard(SoundEventData data) {
        lastHeardPosition = data.worldPosition();
        lastHeardIntensity = data.baseIntensity();
        hasUnhandledSound = true;
    }

    // --- Sight ---
    // Vision cone: configurable FOV angle and range.
    // Uses a Bullet raycast to check if line-of-sight is clear.

    private static final float VISION_FOV_DEGREES = 70f;
    private static final float VISION_RANGE       = 12f;

    public boolean canSeePlayer(Vector3 enemyPos, Vector3 enemyForward,
                                 Vector3 playerPos,
                                 btDiscreteDynamicsWorld physicsWorld) {
        Vector3 toPlayer = new Vector3(playerPos).sub(enemyPos);
        float dist = toPlayer.len();
        if (dist > VISION_RANGE) return false;

        toPlayer.nor();
        float angle = MathUtils.radiansToDegrees *
            (float) Math.acos(MathUtils.clamp(enemyForward.dot(toPlayer), -1f, 1f));
        if (angle > VISION_FOV_DEGREES / 2f) return false;

        // Bullet line-of-sight check
        ClosestRayResultCallback cb = new ClosestRayResultCallback(enemyPos, playerPos);
        physicsWorld.rayTest(enemyPos, playerPos, cb);
        boolean clearLos = !cb.hasHit();
        cb.dispose();
        return clearLos;
    }
}
```

#### `NavMesh` and `NavMeshBuilder`

The nav mesh is a simplified walkable surface that the enemy pathfinds on. For now, use a **waypoint graph** (simpler than a true nav mesh) since LibGDX has no built-in nav mesh:

```java
public final class NavMesh {
    // Simple waypoint graph: nodes are walkable positions, edges connect reachable neighbors
    private final List<Vector3>   waypoints = new ArrayList<>();
    private final List<int[]>     edges     = new ArrayList<>();   // [fromIdx, toIdx]

    public List<Vector3> findPath(Vector3 from, Vector3 to) {
        // A* on waypoints
        int startIdx = nearestWaypoint(from);
        int goalIdx  = nearestWaypoint(to);
        return astar(startIdx, goalIdx);
    }

    private List<Vector3> astar(int start, int goal) { ... }
    private int nearestWaypoint(Vector3 pos) { ... }
}
```

`NavMeshBuilder` reads waypoint JSON placed by the Map Editor (SPAWN_POINT type with `"label": "nav-waypoint-N"`) and builds the graph at load time.

#### `EnemyNavigator`

```java
public final class EnemyNavigator {
    private final NavMesh navMesh;
    private List<Vector3> currentPath = new ArrayList<>();
    private int           pathIndex   = 0;

    public void moveTo(Vector3 target) {
        currentPath = navMesh.findPath(enemyPosition, target);
        pathIndex   = 0;
    }

    /** Returns the direction to move this frame. Returns Vector3.Zero when arrived. */
    public Vector3 update(float delta, Vector3 enemyPosition) {
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) return Vector3.Zero;
        Vector3 nextWaypoint = currentPath.get(pathIndex);
        Vector3 dir = new Vector3(nextWaypoint).sub(enemyPosition);
        if (dir.len2() < 0.1f * 0.1f) {
            pathIndex++;
            return Vector3.Zero;
        }
        return dir.nor();
    }
}
```

#### Individual states (summary)

| State | `onEnter` | `update` returns |
|-------|-----------|-----------------|
| `IdleState` | Play idle animation | `PATROL` after idle timer |
| `PatrolState` | Pick next patrol waypoint | `INVESTIGATE` if sound heard; `CHASE` if player seen |
| `InvestigateState` | Pathfind to last heard position; play "look around" animation | `PATROL` if nothing found after search time; `CHASE` if player seen |
| `ChaseState` | Play chase animation; emit `ENEMY_SCREAM` into graph | `INVESTIGATE` if player lost > 5s; `ATTACK` if in range |
| `AttackState` | Play attack animation; deal damage to player | `CHASE` after attack complete |
| `StunnedState` | Play stunned animation; stop moving | `PATROL` after stun timer |

#### Integration with Event System

When the enemy transitions to `CHASE`, fire `"enemy-chase-start"` on the `EventBus`:
```json
{
  "id": "enemy-chase-start",
  "actions": [
    { "type": "PLAY_SOUND",  "soundId": "audio/sfx/enemy/scream.wav", "volume": 1.0 },
    { "type": "SET_SANITY",  "delta": -15.0 },
    { "type": "PROPAGATE_GRAPH", "soundEvent": "ENEMY_SCREAM", "intensity": 1.2 }
  ]
}
```

#### Files to create

| File | Action |
|------|--------|
| `enemy/EnemyController.java` | Create |
| `enemy/EnemyStateMachine.java` | Create |
| `enemy/EnemyPerception.java` | Create |
| `enemy/EnemyNavigator.java` | Create |
| `enemy/NavMesh.java` | Create |
| `enemy/NavMeshBuilder.java` | Create |
| `enemy/EnemyAnimator.java` | Create |
| `enemy/states/*.java` (6 states) | Create |

---

## 7. Door & Interactable System

### What it does

Provides interactive doors and objects with physics-driven open speed, creak sounds tied to how fast the door is pushed, locked/unlocked state, and keycard requirements. Designed to be placed from the Map Editor.

### Package: `interactable`

```
interactable/
  Interactable.java             — interface all interactables implement
  InteractableRegistry.java     — holds all active interactables by id
  door/
    DoorController.java         — manages door open state and physics angle
    DoorCreakSystem.java        — maps open speed → creak volume and pitch
    DoorLockState.java          — LOCKED, UNLOCKED, KEYCARD_REQUIRED
  objects/
    CabinetController.java
    DrawerController.java
    SwitchController.java       — toggle switch (lights, doors)
    KeycardPickup.java          — collectible that unlocks a door
```

#### `Interactable` interface

```java
public interface Interactable {
    String interactableId();
    String promptText();            // "Open door", "Pick up key", etc.
    boolean canInteract(TriggerEvaluationContext ctx);
    void onInteract(EventContext ctx);
    Vector3 worldPosition();
}
```

#### `DoorController`

```java
public final class DoorController implements Interactable {

    public enum OpenDirection { PUSH, PULL }

    private final String  id;
    private final Vector3 hingePosition;   // hinge axis in world space
    private final Vector3 hingeAxis;       // usually Vector3.Y
    private float         currentAngle;   // degrees, 0 = closed
    private float         targetAngle;    // degrees
    private float         angularVelocity; // degrees/sec
    private DoorLockState lockState;
    private String        requiredKeycardId;  // null if not keycard-required

    // Speed at which door closes on its own (0 = stays open)
    private static final float AUTO_CLOSE_SPEED = 15f;
    // Max push speed per frame (player pushes door physically)
    private static final float MAX_PUSH_SPEED = 120f;

    public void update(float delta, EventContext ctx) {
        float prevAngle = currentAngle;

        // Lerp toward target with simulated momentum
        float diff = targetAngle - currentAngle;
        angularVelocity = MathUtils.lerp(angularVelocity, diff * 5f, delta * 8f);
        currentAngle += angularVelocity * delta;
        currentAngle = MathUtils.clamp(currentAngle, 0f, 90f);

        // Creak when door is moving fast
        float speed = Math.abs(angularVelocity);
        if (speed > 5f) {
            ctx.audioSystem.getDoorCreakSystem().update(id, speed, hingePosition,
                ctx.playerPosition, delta, ctx);
        }

        // Update Bullet hinge constraint angle
        updateHingeConstraint();
    }

    @Override
    public void onInteract(EventContext ctx) {
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
        // Toggle: push open or pull closed
        targetAngle = currentAngle < 45f ? 90f : 0f;
    }
}
```

#### `DoorCreakSystem`

```java
public final class DoorCreakSystem {

    // Maps angular speed → creak intensity
    // Slow push = quiet low creak; fast slam = loud harsh creak
    private static final float CREAK_SPEED_MIN   = 5f;   // degrees/sec
    private static final float CREAK_SPEED_MAX   = 100f;
    private static final float CREAK_VOLUME_MIN  = 0.1f;
    private static final float CREAK_VOLUME_MAX  = 1.0f;
    private static final float CREAK_PITCH_MIN   = 0.8f; // slow = low pitch
    private static final float CREAK_PITCH_MAX   = 1.4f; // fast = higher pitch

    // Per-door cooldown so we don't spam sounds
    private final Map<String, Float> creakCooldowns = new HashMap<>();

    public void update(String doorId, float angularSpeed, Vector3 doorPos,
                       Vector3 playerPos, float delta, EventContext ctx) {
        creakCooldowns.merge(doorId, -delta, Float::sum);
        if (creakCooldowns.getOrDefault(doorId, 0f) > 0f) return;

        float t = MathUtils.clamp(
            (angularSpeed - CREAK_SPEED_MIN) / (CREAK_SPEED_MAX - CREAK_SPEED_MIN), 0f, 1f);
        float volume = MathUtils.lerp(CREAK_VOLUME_MIN, CREAK_VOLUME_MAX, t);
        float pitch  = MathUtils.lerp(CREAK_PITCH_MIN,  CREAK_PITCH_MAX,  t);

        // Pick creak sample — slow/medium/fast variants
        String wavPath = t < 0.33f ? "audio/sfx/door/creak_slow.wav"
                       : t < 0.66f ? "audio/sfx/door/creak_medium.wav"
                                   : "audio/sfx/door/creak_fast.wav";

        // Play spatially and inject into propagation graph
        ctx.audioSystem.playSpatialSfx(wavPath, SoundEvent.DOOR_SLAM,
            doorPos, volume * (0.5f + t * 0.5f), t);

        // Creak cooldown — fast opens creak more often
        creakCooldowns.put(doorId, MathUtils.lerp(0.5f, 0.1f, t));
    }
}
```

#### Map Editor integration

Doors are placed as `DOOR` type in the Map Editor with properties:

```json
{
  "id": "server-room-door",
  "type": "DOOR",
  "position": { "x": 5.0, "y": 0.0, "z": 0.0 },
  "rotation": { "x": 0, "y": 90, "z": 0 },
  "properties": {
    "lockState": "KEYCARD_REQUIRED",
    "requiredKeycardId": "keycard-red",
    "hingeAxis": "Y",
    "openDirection": "PUSH"
  }
}
```

---

## 8. Raycast Interaction System

### What it does

Every frame, cast a ray from the camera center forward. If the ray hits a registered `Interactable` within `MAX_INTERACT_DISTANCE`, show a prompt on the HUD. When the player presses F, call `onInteract()`. This is the unified look-and-press system used by doors, items, switches, and anything else interactive.

### Package: `interaction`

```
interaction/
  RaycastInteractionSystem.java  — casts ray, finds nearest Interactable
  InteractionPromptRenderer.java — draws "Press F to open door" HUD label
  InteractionResult.java         — what the raycast found this frame
```

#### `RaycastInteractionSystem`

```java
public final class RaycastInteractionSystem {

    private static final float MAX_INTERACT_DISTANCE = 2.5f;

    private final InteractableRegistry interactableRegistry;
    private final btDiscreteDynamicsWorld physicsWorld;

    public InteractionResult update(Vector3 rayOrigin, Vector3 rayDirection,
                                     TriggerEvaluationContext evalCtx) {
        Vector3 rayEnd = new Vector3(rayOrigin).add(
            new Vector3(rayDirection).scl(MAX_INTERACT_DISTANCE));

        ClosestRayResultCallback cb = new ClosestRayResultCallback(rayOrigin, rayEnd);
        physicsWorld.rayTest(rayOrigin, rayEnd, cb);

        if (!cb.hasHit()) { cb.dispose(); return InteractionResult.NONE; }

        Vector3 hitPoint = cb.getHitPointWorld(new Vector3());
        cb.dispose();

        // Find nearest registered interactable within range of the hit point
        Interactable nearest = interactableRegistry.findNearest(hitPoint, 0.5f);
        if (nearest == null || !nearest.canInteract(evalCtx)) return InteractionResult.NONE;

        return new InteractionResult(nearest, hitPoint, nearest.promptText());
    }
}
```

#### `InteractionPromptRenderer`

Draws the interaction prompt in the center-bottom of the screen using `SpriteBatch` + `BitmapFont`:

```java
public final class InteractionPromptRenderer {

    public void render(SpriteBatch batch, BitmapFont font, InteractionResult result) {
        if (result == InteractionResult.NONE) return;

        String text = "[F] " + result.promptText();
        float x = Gdx.graphics.getWidth()  / 2f - 60f;
        float y = Gdx.graphics.getHeight() / 2f - 80f;

        // Semi-transparent background pill
        // ... ShapeRenderer black rect ...

        font.setColor(Color.WHITE);
        font.draw(batch, text, x, y);
    }
}
```

#### HUD crosshair dot

The crosshair changes from dim white → bright white when hovering over an interactable:

```java
// In HUD render:
boolean hasTarget = currentInteractionResult != InteractionResult.NONE;
crosshairColor.set(hasTarget ? Color.WHITE : new Color(1f, 1f, 1f, 0.4f));
```

---

# Group B — Horror Atmosphere

---

## 9. Dynamic Lighting System

### What it does

Manages point lights in the world: flickering lights, lights that react to sound events, lights that can be broken, and the player's flashlight with battery drain.

> **Note on LibGDX lighting:** LibGDX core does not have a runtime dynamic lighting pipeline. Options:
> - **Box2DLights** — the project already uses this; extend it to 3D via a deferred pass
> - **gdx-gltf PBR** — supports point lights natively in the gltf pipeline
> - **Custom shader uniform pass** — pass light positions/colors as uniforms to the world shader
>
> The plan uses the **custom shader uniform** approach since the world already has `worldShader` and `sound_pulse.frag`. This keeps it self-contained.

### Package: `lighting`

```
lighting/
  LightManager.java             — holds all lights, passes uniforms to shader
  GameLight.java                — a single point light (position, color, radius, intensity)
  FlickerController.java        — drives intensity over time with noise
  FlashlightController.java     — player flashlight, battery drain
  LightReactorListener.java     — responds to sound events by flickering lights
```

#### `GameLight`

```java
public final class GameLight {
    public enum Type { POINT, SPOT, AMBIENT }

    public final String id;
    public final Type   type;
    public Vector3      position;
    public Color        color;
    public float        radius;           // world units
    public float        intensity;        // 0..1 applied as multiplier
    public boolean      enabled;
    public boolean      breakable;
    public float        breakThreshold;   // sound intensity that breaks this light

    // Runtime flicker state
    public float flickerTimer;
    public boolean isFlickering;
}
```

#### `FlickerController`

Simulates realistic fluorescent flicker using Perlin-like noise:

```java
public final class FlickerController {

    public void update(float delta, GameLight light) {
        if (!light.isFlickering) return;
        light.flickerTimer += delta;

        // Layered sine waves at different frequencies — approximates tube flicker
        float noise = 0.5f
            + 0.3f * MathUtils.sin(light.flickerTimer * 7.3f)
            + 0.1f * MathUtils.sin(light.flickerTimer * 23.1f)
            + 0.1f * MathUtils.sin(light.flickerTimer * 57.8f);

        // Occasionally kill the light entirely for a frame
        if (MathUtils.random() < 0.02f) noise = 0f;

        light.intensity = MathUtils.clamp(noise, 0f, 1f);
    }
}
```

#### `FlashlightController`

```java
public final class FlashlightController {

    private static final float MAX_BATTERY      = 100f;
    private static final float DRAIN_PER_SECOND = 1.5f;   // ~66s full charge
    private static final float LOW_BATTERY      = 20f;    // starts flickering below this
    private static final float DEAD_BATTERY     = 0f;

    private float battery = MAX_BATTERY;
    private boolean on    = false;
    private final GameLight flashlight;

    public void toggle() { on = !on; }

    public void update(float delta, EventContext ctx) {
        if (!on) { flashlight.enabled = false; return; }
        if (battery <= DEAD_BATTERY) {
            flashlight.enabled = false;
            return;
        }

        battery = Math.max(DEAD_BATTERY, battery - DRAIN_PER_SECOND * delta);
        flashlight.enabled = true;

        // Flicker when low on battery
        if (battery < LOW_BATTERY) {
            flashlight.isFlickering = true;
            flashlight.intensity = MathUtils.clamp(battery / LOW_BATTERY, 0.2f, 1f);
        } else {
            flashlight.isFlickering = false;
            flashlight.intensity = 1f;
        }
    }

    public float getBatteryPercent() { return battery / MAX_BATTERY; }
    public boolean isOn() { return on; }
}
```

#### `LightReactorListener` — lights respond to loud sounds

Register with the `EventBus` to receive events. When a loud sound fires (e.g. `PHYSICS_COLLAPSE`), nearby lights flicker briefly:

```java
public final class LightReactorListener implements EventBus.EventListener {

    private final LightManager lightManager;

    @Override
    public void onEventFired(String eventId, EventContext ctx) {
        SoundEvent soundEvent = ctx.lastSoundEvent;
        if (soundEvent == null) return;
        if (soundEvent.eventPriority().ordinal() < EventPriority.MEDIUM.ordinal()) return;

        // Find lights within the sound's effective range
        float range = ctx.lastSoundIntensity * 20f;
        lightManager.getAllLights().stream()
            .filter(l -> l.position.dst(ctx.triggerPosition) < range)
            .forEach(l -> {
                l.isFlickering = true;
                l.flickerTimer  = 0f;
                // Stop flickering after 1-2 seconds
                Gdx.app.postRunnable(() -> {
                    /* schedule stop-flicker */
                });
            });
    }
}
```

#### Shader integration

Pass the first N lights (e.g. 8) to the world shader as uniforms:

```glsl
// sound_pulse.frag — add light uniforms
uniform vec3  u_lightPos[8];
uniform vec4  u_lightColor[8];
uniform float u_lightRadius[8];
uniform float u_lightIntensity[8];
uniform int   u_lightCount;
```

```java
// In SoundPulseShaderRenderer.render():
lightManager.bindUniforms(worldShader);
```

#### Map Editor integration

`LIGHT_POINT` objects in the Map Editor JSON:

```json
{
  "id": "lamp-01",
  "type": "LIGHT_POINT",
  "position": { "x": 3.0, "y": 2.5, "z": -2.0 },
  "properties": {
    "color": "#FFF8E0",
    "radius": "8.0",
    "intensity": "1.0",
    "flickering": "true",
    "breakable": "true",
    "breakThreshold": "0.8"
  }
}
```

---

## 10. Sanity / Fear System

### What it does

A `sanity` meter (0–100) that decreases when frightening things happen (darkness, enemy proximity, scripted events) and slowly recovers in safe, lit areas. Low sanity drives visual distortions, audio glitches, and hallucination events.

### Package: `sanity`

```
sanity/
  SanitySystem.java             — master sanity controller
  SanityDrainSource.java        — interface: a thing that drains sanity each frame
  SanityEffect.java             — interface: a thing that activates at low sanity
  drains/
    DarknessPresenceDrain.java  — drain based on flashlight off + low light
    EnemyProximityDrain.java    — drain based on distance to nearest enemy
    EventDrain.java             — instant drain from scripted events
  effects/
    ScreenVignetteEffect.java   — dark vignette grows at low sanity
    ShaderDistortionEffect.java — adds chromatic aberration, screen warp
    AudioGlitchEffect.java      — adds static, pitch shift, hallucination sounds
    HallucinationEffect.java    — fake enemy appears / disappears
```

#### `SanitySystem`

```java
public final class SanitySystem {

    private static final float MAX_SANITY    = 100f;
    private static final float MIN_SANITY    = 0f;
    private static final float RECOVER_RATE  = 2f;   // per second in safe lit area

    private float sanity = MAX_SANITY;
    private final List<SanityDrainSource> drainSources = new ArrayList<>();
    private final List<SanityEffect>      effects       = new ArrayList<>();
    private boolean inSafeArea;   // set by ZoneTrigger with "safe" property

    public void update(float delta, LightManager lights, Vector3 playerPos,
                       List<EnemyController> enemies, EventBus bus) {
        float totalDrain = 0f;
        for (SanityDrainSource src : drainSources) {
            totalDrain += src.drainPerSecond(playerPos, lights, enemies, delta);
        }

        if (inSafeArea && totalDrain == 0f) {
            sanity = Math.min(MAX_SANITY, sanity + RECOVER_RATE * delta);
        } else {
            sanity = Math.max(MIN_SANITY, sanity - totalDrain * delta);
        }

        // Fire threshold events on the EventBus
        checkThresholds(bus);

        // Update visual/audio effects
        for (SanityEffect effect : effects) {
            effect.update(delta, sanity / MAX_SANITY);
        }
    }

    public void applyDelta(float delta) {   // called by SetSanityDeltaAction
        sanity = MathUtils.clamp(sanity + delta, MIN_SANITY, MAX_SANITY);
    }

    public float getSanity()           { return sanity; }
    public float getSanityNormalized() { return sanity / MAX_SANITY; }

    private void checkThresholds(EventBus bus) {
        // Fire events at 75, 50, 25, 0 sanity (once each)
        // These can trigger HUD warnings, ambient changes, or jump scares
    }
}
```

#### Drain sources

```java
// Darkness drain — offline light + flashlight off
public final class DarknessPresenceDrain implements SanityDrainSource {
    public float drainPerSecond(Vector3 playerPos, LightManager lights, ...) {
        float nearestLightIntensity = lights.intensityAtPoint(playerPos);
        if (nearestLightIntensity > 0.3f) return 0f; // well lit — no drain
        return MathUtils.lerp(0f, 4f, 1f - (nearestLightIntensity / 0.3f));
    }
}

// Enemy proximity drain
public final class EnemyProximityDrain implements SanityDrainSource {
    private static final float DRAIN_RANGE = 10f;
    private static final float MAX_DRAIN   = 8f; // per second

    public float drainPerSecond(Vector3 playerPos, ..., List<EnemyController> enemies) {
        float minDist = Float.MAX_VALUE;
        for (EnemyController e : enemies) {
            minDist = Math.min(minDist, e.getPosition().dst(playerPos));
        }
        if (minDist >= DRAIN_RANGE) return 0f;
        return MAX_DRAIN * (1f - minDist / DRAIN_RANGE);
    }
}
```

#### Effects at different sanity levels

| Sanity range | Effects active |
|-------------|----------------|
| 75–100 | None |
| 50–75 | Subtle screen vignette |
| 25–50 | Vignette intensifies; occasional audio static glitch |
| 10–25 | Chromatic aberration; whisper sounds; breathing sounds |
| 0–10 | Full distortion; hallucination enemy flickers in view |

#### `ShaderDistortionEffect`

Passes a `u_distortionStrength` uniform to the world fragment shader. At sanity 0, the value is 1.0; at 100, it is 0.0.

```glsl
// world.frag — sanity distortion
uniform float u_distortionStrength;
uniform float u_time;

void main() {
    vec2 uv = v_texCoord;
    // Chromatic aberration
    float offset = u_distortionStrength * 0.005;
    float r = texture2D(u_texture, uv + vec2(offset, 0.0)).r;
    float g = texture2D(u_texture, uv).g;
    float b = texture2D(u_texture, uv - vec2(offset, 0.0)).b;
    // Screen warp
    uv += u_distortionStrength * 0.01 * vec2(sin(uv.y * 10.0 + u_time), 0.0);
    gl_FragColor = vec4(r, g, b, 1.0);
}
```

---

## 11. Jump Scare Director

### What it does

Tracks the tension level (based on sanity, time since last scare, player location) and decides when and how to trigger jump scares. Scares can be scripted (placed in the map via the editor) or procedural (chosen at runtime based on context). Prevents overuse by enforcing minimum time between scares.

### Package: `scare`

```
scare/
  JumpScareDirector.java        — tension tracker, scare picker and executor
  JumpScare.java                — one scare definition
  ScareType.java                — enum of scare categories
  scripted/
    AudioOnlyScare.java         — loud sound + graph inject, no visual
    FlashScare.java             — screen flash + sound
    EnemyAppearScare.java       — enemy teleports nearby and screams
    HallucinationScare.java     — fake model appears briefly
    EnvironmentScare.java       — object moves/falls on its own
```

#### `ScareType`

```java
public enum ScareType {
    AUDIO_ONLY,       // quiet room suddenly has a loud bang
    SCREEN_FLASH,     // white/red flash + audio spike
    ENEMY_APPEAR,     // enemy spawns adjacent, screams, vanishes
    HALLUCINATION,    // fleeting fake enemy model in view
    ENVIRONMENT       // scripted object movement (door slams, shelf falls)
}
```

#### `JumpScareDirector`

```java
public final class JumpScareDirector {

    private static final float MIN_SCARE_INTERVAL = 45f;  // never scare more than once per 45s
    private static final float TENSION_THRESHOLD  = 70f;  // tension/100 needed to allow a scare
    private static final float BASE_TENSION_RATE  = 2f;   // per second passive buildup

    private float tension         = 0f;
    private float timeSinceLastScare = MIN_SCARE_INTERVAL; // start "ready"
    private final List<JumpScare> availableScares = new ArrayList<>();
    private final List<JumpScare> scriptedQueue   = new ArrayList<>(); // from EventBus

    public void update(float delta, SanitySystem sanity, EventContext ctx) {
        timeSinceLastScare += delta;

        // Tension builds passively, accelerates at low sanity
        float sanityMultiplier = 1f + (1f - sanity.getSanityNormalized()) * 2f;
        tension = Math.min(100f, tension + BASE_TENSION_RATE * sanityMultiplier * delta);

        // Try to execute a queued scripted scare first
        if (!scriptedQueue.isEmpty() && canScare()) {
            execute(scriptedQueue.remove(0), ctx);
            return;
        }

        // Procedural scare at high tension
        if (tension >= TENSION_THRESHOLD && canScare()) {
            JumpScare picked = pickProcedural(sanity, ctx);
            if (picked != null) execute(picked, ctx);
        }
    }

    public void queueScripted(JumpScare scare) {
        scriptedQueue.add(scare);
    }

    private boolean canScare() {
        return timeSinceLastScare >= MIN_SCARE_INTERVAL;
    }

    private void execute(JumpScare scare, EventContext ctx) {
        scare.execute(ctx);
        tension = 0f;
        timeSinceLastScare = 0f;
        ctx.eventBus.fire("jump-scare-executed", ctx);
    }

    private JumpScare pickProcedural(SanitySystem sanity, EventContext ctx) {
        // Filter available scares to those that match current context
        // Prefer lighter scares at higher sanity, heavier at lower
        float s = sanity.getSanityNormalized();
        if (s > 0.5f) {
            return availableScares.stream()
                .filter(sc -> sc.type() == ScareType.AUDIO_ONLY || sc.type() == ScareType.ENVIRONMENT)
                .findAny().orElse(null);
        } else {
            return availableScares.stream()
                .filter(sc -> sc.type() == ScareType.ENEMY_APPEAR || sc.type() == ScareType.SCREEN_FLASH)
                .findAny().orElse(null);
        }
    }
}
```

#### Map Editor integration

Scripted scares placed as `TRIGGER_VOLUME` with a linked event:

```json
{
  "id": "bookshelf-fall-trigger",
  "type": "TRIGGER_VOLUME",
  "properties": {
    "triggerId": "bookshelf-fall-scare",
    "halfWidth": "2", "halfHeight": "2", "halfDepth": "2"
  }
}
```

```json
{
  "id": "bookshelf-fall-scare",
  "actions": [
    { "type": "JUMP_SCARE", "scareType": "ENVIRONMENT",
      "soundId": "audio/sfx/env/shelf_crash.wav",
      "graphEvent": "PHYSICS_COLLAPSE", "intensity": 1.0 }
  ]
}
```

---

# Group C — Narrative & Progression

---

## 12. Dialogue & Subtitle System

### What it does

Plays voice lines (WAV) tied to events, shows on-screen subtitles with timing, and queues multiple lines so they don't overlap. Used for character monologue, radio transmissions, and environmental storytelling.

### Package: `dialogue`

```
dialogue/
  DialogueSystem.java           — manages queue, rendering, timing
  DialogueLine.java             — one line: text + wav + duration
  DialogueSequence.java         — ordered list of lines
  DialogueLoader.java           — loads dialogue JSON files
  SubtitleRenderer.java         — draws subtitle text on HUD
```

#### `DialogueLine`

```java
public record DialogueLine(
    String text,
    String wavPath,      // null = text only (no voice)
    float  displaySeconds,
    Color  textColor,    // speaker color coding
    String speakerLabel  // "Radio", "You", "???"  — shown before text
) {}
```

#### `DialogueSystem`

```java
public final class DialogueSystem {

    private final Queue<DialogueLine>    lineQueue   = new ArrayDeque<>();
    private DialogueLine                 currentLine = null;
    private float                        lineTimer;
    private final GameAudioSystem        audioSystem;
    private final SubtitleRenderer       subtitleRenderer;

    public void showSubtitle(String text, float duration) {
        enqueue(new DialogueLine(text, null, duration, Color.WHITE, ""));
    }

    public void playDialogue(DialogueLine line) {
        enqueue(line);
    }

    public void playSequence(DialogueSequence sequence) {
        sequence.lines().forEach(this::enqueue);
    }

    private void enqueue(DialogueLine line) {
        if (currentLine == null) beginLine(line);
        else lineQueue.add(line);
    }

    private void beginLine(DialogueLine line) {
        currentLine = line;
        lineTimer   = line.displaySeconds();
        if (line.wavPath() != null) {
            audioSystem.playSfx(line.wavPath(), new Vector3(), line.displaySeconds());
        }
    }

    public void update(float delta) {
        if (currentLine == null) return;
        lineTimer -= delta;
        if (lineTimer <= 0f) {
            currentLine = lineQueue.isEmpty() ? null : lineQueue.poll();
            if (currentLine != null) beginLine(currentLine);
        }
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (currentLine == null) return;
        subtitleRenderer.render(batch, font, currentLine, lineTimer);
    }
}
```

#### `SubtitleRenderer`

Draws subtitles at the bottom third of the screen with a fade-in / fade-out curve:

```java
public final class SubtitleRenderer {
    private static final float FADE_DURATION = 0.3f;

    public void render(SpriteBatch batch, BitmapFont font,
                       DialogueLine line, float timeRemaining) {
        float totalDuration = line.displaySeconds();
        float elapsed       = totalDuration - timeRemaining;

        // Fade in
        float fadeIn  = MathUtils.clamp(elapsed / FADE_DURATION, 0f, 1f);
        // Fade out
        float fadeOut = MathUtils.clamp(timeRemaining / FADE_DURATION, 0f, 1f);
        float alpha   = Math.min(fadeIn, fadeOut);

        String displayText = line.speakerLabel().isEmpty()
            ? line.text()
            : "[" + line.speakerLabel() + "] " + line.text();

        float x = Gdx.graphics.getWidth()  * 0.5f;
        float y = Gdx.graphics.getHeight() * 0.22f;

        font.setColor(line.textColor().r, line.textColor().g, line.textColor().b, alpha);
        // draw centered, with shadow for readability
        font.draw(batch, displayText, x - 200f, y, 400f,
                  com.badlogic.gdx.utils.Align.center, true);
    }
}
```

#### JSON dialogue format

```json
{
  "id": "intro-monologue",
  "lines": [
    {
      "text": "I should not have come here.",
      "wavPath": "audio/dialogue/player/intro_01.wav",
      "displaySeconds": 3.5,
      "speakerLabel": "You",
      "textColor": "#FFFFFF"
    },
    {
      "text": "...it's too quiet.",
      "wavPath": "audio/dialogue/player/intro_02.wav",
      "displaySeconds": 2.8,
      "speakerLabel": "You",
      "textColor": "#FFFFFF"
    }
  ]
}
```

---

## 13. Save / Checkpoint System

### What it does

Saves player state, event flags, inventory, and last-reached checkpoint to a JSON file. Supports multiple save slots. On death or load, restores the game to the checkpoint state.

### Package: `save`

```
save/
  SaveSystem.java               — read/write save files
  SaveData.java                 — full serialisable game state
  CheckpointManager.java        — tracks which checkpoint was last reached
  SaveSlot.java                 — metadata for one save file
```

#### `SaveData`

```java
public final class SaveData {
    public String   saveSlotId;
    public String   mapName;
    public String   checkpointId;
    public Vector3  playerPosition   = new Vector3();
    public float    playerSanity     = 100f;
    public float    flashlightBattery = 100f;
    public Map<String, Integer> eventFireCounts = new LinkedHashMap<>();
    public Map<String, String>  inventoryItems  = new LinkedHashMap<>();
    public long     savedAtMillis;
    public String   savedAtDisplay;  // "2025-07-14 21:03"
}
```

#### `SaveSystem`

```java
public final class SaveSystem {

    private static final String SAVE_DIR = "saves/";
    private static final int    MAX_SLOTS = 3;

    public void save(String slotId, SaveData data) {
        data.savedAtMillis  = System.currentTimeMillis();
        data.savedAtDisplay = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
            .format(new java.util.Date());

        String json = toJson(data);
        FileHandle file = Gdx.files.local(SAVE_DIR + slotId + ".json");
        file.writeString(json, false);
        Gdx.app.log("SaveSystem", "Saved to slot: " + slotId);
    }

    public SaveData load(String slotId) {
        FileHandle file = Gdx.files.local(SAVE_DIR + slotId + ".json");
        if (!file.exists()) return null;
        return fromJson(file.readString());
    }

    public List<SaveSlot> listSlots() {
        // Returns metadata (slot id, map name, timestamp) for all existing saves
    }

    public void delete(String slotId) {
        Gdx.files.local(SAVE_DIR + slotId + ".json").delete();
    }
}
```

#### `CheckpointManager`

```java
public final class CheckpointManager {

    private String lastCheckpointId = "start";
    private final SaveSystem saveSystem;

    /** Called when player enters a CHECKPOINT map object. */
    public void onCheckpointReached(String checkpointId, EventContext ctx) {
        lastCheckpointId = checkpointId;

        // Auto-save on checkpoint
        SaveData data = captureState(ctx);
        data.checkpointId = checkpointId;
        saveSystem.save("autosave", data);

        ctx.audioSystem.playSfx("audio/sfx/ui/checkpoint.wav", ctx.playerPosition, 0.5f);
        ctx.dialogueSystem.showSubtitle("Progress saved.", 2.0f);
    }

    public String lastCheckpointId() { return lastCheckpointId; }

    private SaveData captureState(EventContext ctx) {
        // Serialize current game state into SaveData
        SaveData data = new SaveData();
        data.mapName          = currentMapName;
        data.playerPosition.set(ctx.playerPosition);
        data.playerSanity     = ctx.sanitySystem.getSanity();
        data.flashlightBattery = ctx.flashlightController.getBatteryPercent() * 100f;
        data.eventFireCounts.putAll(ctx.state.getFireCountSnapshot());
        data.inventoryItems.putAll(ctx.inventorySystem.getItemSnapshot());
        return data;
    }
}
```

#### Map Editor integration

Checkpoints placed as `CHECKPOINT` objects in the Map Editor:

```json
{
  "id": "checkpoint-server-room",
  "type": "CHECKPOINT",
  "position": { "x": 8.0, "y": 0.0, "z": -3.0 },
  "properties": { "checkpointId": "server-room-cleared" }
}
```

`MapDocumentLoader` creates a `ZoneTrigger` pointing to a `"checkpoint-reached"` event, which calls `CheckpointManager.onCheckpointReached()` via a new `CheckpointAction`.

---

## 14. Scene / Level Transition System

### What it does

Handles the visual and state handoff between maps — fade out, dispose current scene, load next map (with loading progress), fade in. Preserves relevant state (inventory, sanity, event flags) across maps.

### Package: `transition`

```
transition/
  SceneTransitionSystem.java    — master controller
  TransitionScreen.java         — black screen with loading bar + tip text
  FadeOverlay.java              — full-screen quad that fades in/out
  LevelRef.java                 — a reference to a level (map json path + spawn label)
```

#### `SceneTransitionSystem`

```java
public final class SceneTransitionSystem {

    public enum Phase { IDLE, FADING_OUT, LOADING, FADING_IN }

    private Phase  phase = Phase.IDLE;
    private float  fadeTimer;
    private static final float FADE_DURATION = 1.2f;
    private LevelRef pendingLevel;
    private SaveData carryoverState;

    public void transitionTo(LevelRef levelRef, SaveData carryover) {
        if (phase != Phase.IDLE) return;
        pendingLevel   = levelRef;
        carryoverState = carryover;
        phase          = Phase.FADING_OUT;
        fadeTimer      = 0f;
    }

    public void update(float delta, Game gdxGame) {
        switch (phase) {
            case FADING_OUT -> {
                fadeTimer += delta;
                if (fadeTimer >= FADE_DURATION) {
                    phase     = Phase.LOADING;
                    fadeTimer = 0f;
                    beginLoad(gdxGame);
                }
            }
            case LOADING -> {
                // Loading happens in TransitionScreen — this phase ends
                // when TransitionScreen signals completion
            }
            case FADING_IN -> {
                fadeTimer += delta;
                if (fadeTimer >= FADE_DURATION) {
                    phase = Phase.IDLE;
                }
            }
        }
    }

    public float fadeAlpha() {
        return switch (phase) {
            case FADING_OUT -> MathUtils.clamp(fadeTimer / FADE_DURATION, 0f, 1f);
            case LOADING    -> 1f;
            case FADING_IN  -> 1f - MathUtils.clamp(fadeTimer / FADE_DURATION, 0f, 1f);
            default         -> 0f;
        };
    }

    private void beginLoad(Game gdxGame) {
        gdxGame.setScreen(new TransitionScreen(pendingLevel, carryoverState, this));
    }
}
```

#### `TransitionScreen`

```java
public final class TransitionScreen implements Screen {

    private float loadProgress = 0f;
    private final LevelRef   levelRef;
    private final SaveData   carryoverState;

    @Override
    public void render(float delta) {
        // Simulate load progress (or use LibGDX AssetManager for real async loading)
        loadProgress = Math.min(1f, loadProgress + delta * 0.4f);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw loading bar and tip text
        drawLoadingBar();
        drawTipText();

        if (loadProgress >= 1f) {
            // Transition to the loaded scene
            GltfMapTestScene nextScene = new GltfMapTestScene(levelRef, carryoverState);
            game.setScreen(nextScene);
            transitionSystem.onLoadComplete();
        }
    }

    private void drawTipText() {
        // Rotate through gameplay tips — pulls from assets/data/tips.json
    }
}
```

#### Transition trigger from Map Editor

A `TRIGGER_VOLUME` with a linked event:

```json
{
  "id": "exit-to-level-2",
  "actions": [
    { "type": "TRANSITION_LEVEL",
      "mapJson": "maps/level02.json",
      "spawnLabel": "default" }
  ]
}
```

---

# Group D — Developer Tools

---

## 15. In-Game Debug Console

### What it does

A text input overlay (press `` ` `` backtick to open) that accepts typed commands at runtime. No recompile needed to test features, spawn things, fire events, or warp around.

### Package: `debug`

```
debug/
  DebugConsole.java             — input handler, command dispatcher
  ConsoleRenderer.java          — draws the console overlay
  ConsoleCommand.java           — interface for one command
  ConsoleRegistry.java          — holds all registered commands
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
```

#### `DebugConsole`

```java
public final class DebugConsole {

    private boolean open = false;
    private final StringBuilder inputBuffer = new StringBuilder();
    private final List<String>  history     = new ArrayList<>();
    private final List<String>  outputLines = new ArrayList<>();  // max 20 lines
    private final ConsoleRegistry registry;

    public boolean handleKeyDown(int keycode) {
        if (keycode == Input.Keys.GRAVE) { toggle(); return true; }
        if (!open) return false;
        if (keycode == Input.Keys.ENTER)     { submit(); return true; }
        if (keycode == Input.Keys.BACKSPACE) { if (inputBuffer.length() > 0) inputBuffer.deleteCharAt(inputBuffer.length() - 1); return true; }
        return false;
    }

    public boolean handleKeyTyped(char ch) {
        if (!open || ch == '`') return false;
        if (ch >= 32 && ch < 127) inputBuffer.append(ch);
        return true;
    }

    private void submit() {
        String cmd = inputBuffer.toString().trim();
        inputBuffer.setLength(0);
        if (cmd.isEmpty()) return;
        history.add(cmd);

        String[] parts = cmd.split("\\s+");
        ConsoleCommand command = registry.get(parts[0]);
        if (command == null) {
            print("Unknown command: " + parts[0]);
        } else {
            try {
                String result = command.execute(parts, context);
                print(result);
            } catch (Exception e) {
                print("Error: " + e.getMessage());
            }
        }
    }

    public void print(String message) {
        outputLines.add(message);
        if (outputLines.size() > 20) outputLines.remove(0);
    }

    private void toggle() { open = !open; }
    public boolean isOpen() { return open; }
}
```

#### Built-in commands

| Command | Example | Effect |
|---------|---------|--------|
| `tp` | `tp 0 0 0` | Teleport player to world position |
| `fire` | `fire alarm-trigger` | Fire an event by id |
| `flag` | `flag door-warned true` | Set/clear an event flag |
| `sanity` | `sanity 25` | Set sanity to a value |
| `spawn` | `spawn enemy 0 0 5` | Spawn an enemy at position |
| `god` | `god` | Toggle god mode (no sanity drain, no damage) |
| `give` | `give keycard-red` | Add item to inventory |
| `events` | `events` | List all registered event ids |
| `reload` | `reload` | Reload current map document from disk |
| `state` | `state` | Print all fired events and flags |
| `help` | `help` | List all commands |

#### Console rendering

```java
public final class ConsoleRenderer {

    public void render(SpriteBatch batch, BitmapFont font, DebugConsole console) {
        if (!console.isOpen()) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight() * 0.4f;
        float y = Gdx.graphics.getHeight();

        // Dark background panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.85f);
        shapeRenderer.rect(0, y - h, w, h);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        float lineY = y - 10f;
        float lineH = 16f;

        // Output lines (newest at bottom)
        for (String line : console.outputLines()) {
            font.draw(batch, line, 8f, lineY);
            lineY -= lineH;
        }

        // Input line with cursor
        font.setColor(Color.YELLOW);
        font.draw(batch, "> " + console.inputText() + "_", 8f, y - h + 18f);
        batch.end();
    }
}
```

---

## 16. Settings System

### What it does

Loads and saves player preferences to a local config file (`saves/settings.json`). Covers audio volumes, graphics, controls, and sensitivity. The settings screen (shown on first launch or from the main menu) reads and writes through this system.

### Package: `settings`

```
settings/
  SettingsSystem.java           — master settings controller
  SettingsData.java             — all settings as fields
  SettingsScreen.java           — in-game settings UI (LibGDX Scene2D or custom)
  KeybindRegistry.java          — maps action names to keycodes
```

#### `SettingsData`

```java
public final class SettingsData {
    // Audio
    public float masterVolume    = 1.0f;
    public float musicVolume     = 0.8f;
    public float ambienceVolume  = 0.9f;
    public float sfxVolume       = 1.0f;
    public float voiceVolume     = 1.0f;

    // Graphics
    public int   targetFps       = 60;
    public boolean vsync         = true;
    public boolean fullscreen    = false;
    public int   resolutionWidth = 1280;
    public int   resolutionHeight = 720;
    public float gamma           = 1.0f;

    // Controls
    public float mouseSensitivity = 0.25f;
    public boolean invertY        = false;
    public Map<String, Integer> keybinds = new LinkedHashMap<>();

    public static SettingsData defaults() {
        SettingsData d = new SettingsData();
        d.keybinds.put("INTERACT",    Input.Keys.F);
        d.keybinds.put("CROUCH",      Input.Keys.C);
        d.keybinds.put("SPRINT",      Input.Keys.SHIFT_LEFT);
        d.keybinds.put("FLASHLIGHT",  Input.Keys.L);
        d.keybinds.put("CONSOLE",     Input.Keys.GRAVE);
        return d;
    }
}
```

#### `SettingsSystem`

```java
public final class SettingsSystem {

    private static final String SETTINGS_PATH = "saves/settings.json";
    private SettingsData data;

    public SettingsSystem() {
        data = load();
        apply();
    }

    public SettingsData get() { return data; }

    public void save() {
        FileHandle file = Gdx.files.local(SETTINGS_PATH);
        file.writeString(toJson(data), false);
    }

    public void apply() {
        // Apply to LibGDX runtime
        Gdx.graphics.setVSync(data.vsync);
        Gdx.graphics.setForegroundFPS(data.targetFps);
        if (data.fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(data.resolutionWidth, data.resolutionHeight);
        }
        // Audio volumes applied to GameAudioSystem on next frame via SettingsApplier
    }

    private SettingsData load() {
        FileHandle file = Gdx.files.local(SETTINGS_PATH);
        if (!file.exists()) return SettingsData.defaults();
        try { return fromJson(file.readString()); }
        catch (Exception e) { return SettingsData.defaults(); }
    }
}
```

#### `KeybindRegistry`

```java
public final class KeybindRegistry {

    private final Map<String, Integer> bindings;

    public KeybindRegistry(SettingsData settings) {
        bindings = new HashMap<>(settings.keybinds);
    }

    public boolean isPressed(String action) {
        Integer keycode = bindings.get(action);
        return keycode != null && Gdx.input.isKeyPressed(keycode);
    }

    public boolean isJustPressed(String action) {
        Integer keycode = bindings.get(action);
        return keycode != null && Gdx.input.isKeyJustPressed(keycode);
    }

    public void rebind(String action, int newKeycode) {
        bindings.put(action, newKeycode);
    }
}
```

All input in the game should go through `KeybindRegistry` instead of raw `Gdx.input.isKeyPressed(Input.Keys.F)` calls. This makes rebinding instant without touching any game code.

---

## 17. Footstep Material System

### What it does

Plays different footstep WAV sets based on the surface the player is walking on. The acoustic graph already knows `AcousticMaterial` per wall — the footstep system reads the Bullet raycast hit body's material tag and picks the correct sound set.

### Package: `footstep`

```
footstep/
  FootstepSystem.java           — manages timing and surface detection
  FootstepSoundSet.java         — a set of WAVs for one surface type
  SurfaceMaterial.java          — enum mapping physics bodies to sound sets
```

#### `SurfaceMaterial`

```java
public enum SurfaceMaterial {
    CONCRETE,    // base default
    WOOD,
    METAL,
    GRAVEL,
    WATER,
    CARPET,
    TILE,
    DIRT
}
```

#### `FootstepSystem`

```java
public final class FootstepSystem {

    private static final float WALK_STEP_INTERVAL = 0.5f;
    private static final float RUN_STEP_INTERVAL  = 0.32f;
    private static final float CROUCHING_VOLUME   = 0.25f;
    private static final float WALKING_VOLUME     = 0.55f;
    private static final float RUNNING_VOLUME     = 0.85f;

    private float stepTimer = 0f;
    private final Map<SurfaceMaterial, FootstepSoundSet> soundSets = new EnumMap<>(SurfaceMaterial.class);
    private final SpatialSfxEmitter sfxEmitter;
    private final btDiscreteDynamicsWorld physicsWorld;

    public void update(float delta, PlayerController player, Vector3 listenerPos,
                       float elapsed, Function<Vector3, String> nodeResolver) {
        if (!player.isGrounded() || !player.isMoving()) {
            stepTimer = 0f;
            return;
        }

        float interval = player.isSprinting() ? RUN_STEP_INTERVAL : WALK_STEP_INTERVAL;
        float volume   = player.isCrouching() ? CROUCHING_VOLUME
                       : player.isSprinting() ? RUNNING_VOLUME
                                              : WALKING_VOLUME;
        stepTimer += delta;
        if (stepTimer < interval) return;
        stepTimer = 0f;

        // Bullet downcast to detect surface
        SurfaceMaterial material = detectSurface(player.getPosition());
        FootstepSoundSet sounds = soundSets.getOrDefault(material, soundSets.get(SurfaceMaterial.CONCRETE));

        SoundEvent graphEvent = player.isSprinting()
            ? SoundEvent.FOOTSTEP_RUN : SoundEvent.FOOTSTEP;

        sfxEmitter.playAndPropagate(
            sounds.randomSample(), graphEvent,
            player.getPosition(), listenerPos,
            volume, graphEvent.defaultBaseIntensity(),
            elapsed, nodeResolver
        );
    }

    private SurfaceMaterial detectSurface(Vector3 playerPos) {
        // Short downward ray from player feet
        Vector3 from = new Vector3(playerPos);
        Vector3 to   = new Vector3(playerPos).add(0f, -0.3f, 0f);

        ClosestRayResultCallback cb = new ClosestRayResultCallback(from, to);
        physicsWorld.rayTest(from, to, cb);

        if (!cb.hasHit()) { cb.dispose(); return SurfaceMaterial.CONCRETE; }

        // Read material tag stored in the collision object's user data
        Object userData = cb.getCollisionObject().getUserPointer();
        cb.dispose();

        if (userData instanceof SurfaceMaterial mat) return mat;
        return SurfaceMaterial.CONCRETE;
    }
}
```

#### `FootstepSoundSet`

```java
public final class FootstepSoundSet {
    private final String[] wavPaths;  // 3–5 variants for variation
    private int lastPicked = -1;

    public FootstepSoundSet(String... wavPaths) {
        this.wavPaths = wavPaths;
    }

    /** Pick a random sample, never repeat the same one twice in a row. */
    public String randomSample() {
        int pick;
        do { pick = MathUtils.random(wavPaths.length - 1); } while (pick == lastPicked);
        lastPicked = pick;
        return wavPaths[pick];
    }
}
```

#### Asset layout

```
assets/audio/sfx/footstep/
  concrete/  step_01.wav  step_02.wav  step_03.wav  step_04.wav
  wood/      step_01.wav  step_02.wav  step_03.wav
  metal/     step_01.wav  step_02.wav  step_03.wav  step_04.wav
  gravel/    step_01.wav  step_02.wav  step_03.wav
  water/     splash_01.wav  splash_02.wav  splash_03.wav
  carpet/    step_01.wav  step_02.wav
  tile/      step_01.wav  step_02.wav  step_03.wav
  dirt/      step_01.wav  step_02.wav  step_03.wav
```

#### Map Editor integration

Surface materials are assigned to physics bodies via the Map Editor. `BOX_PROP` and `DOOR` objects have a `"material"` property that is tagged onto the Bullet `btRigidBody` userPointer at `MapDocumentLoader` time. The `-map.gltf` mesh nodes can also carry material tags via the node name convention:

```
Blender mesh name: "floor_wood"  → SurfaceMaterial.WOOD
Blender mesh name: "wall_concrete" → SurfaceMaterial.CONCRETE
```

`MapCollisionBuilder` reads the first word of the node name before `_` and matches it to a `SurfaceMaterial`.

---

# How All Systems Connect

```
═══════════════════════════════════════════════════════════════
                    A COMPLETE HORROR SCENARIO
═══════════════════════════════════════════════════════════════

Player walks down a dark corridor
  │
  ├─ FootstepSystem detects WOOD surface
  │    └─ plays wood/step_02.wav + injects FOOTSTEP into propagation graph
  │
  ├─ SanitySystem (DarknessPresenceDrain)
  │    └─ flashlight is off + dim lighting → sanity drops 4/sec
  │
  ├─ RaycastInteractionSystem casts ray
  │    └─ hits DoorController "server-door-01"
  │         └─ InteractionPromptRenderer: "[F] Open door"
  │
  ├─ Player presses F
  │    └─ DoorController.onInteract()
  │         └─ DoorCreakSystem: angularVelocity high → creak_fast.wav (volume 0.9)
  │              └─ SpatialSfxEmitter.playAndPropagate()
  │                   ├─ SoundBank.sound("creak_fast.wav").play(0.9, 1.4, pan)
  │                   └─ propagation graph ← DOOR_SLAM intensity 0.9
  │                        └─ EnemyPerception.onSoundHeard()
  │                             └─ EnemyStateMachine → INVESTIGATE
  │
  ├─ TriggerEvaluator evaluates ZoneTrigger "server-room-entry"
  │    └─ EventBus.fire("creak-then-groan")
  │         └─ GameEvent fires:
  │              [1] PropagateGraphAction → AMBIENT_CREAK
  │              [2] WaitAction 1.2s (EventSequence timer)
  │              [3] PlaySoundAction → groan_deep.wav
  │              [4] ShowSubtitleAction → "Something groans in the walls..."
  │              [5] SetFlagAction → "door-warned"
  │              [6] SetSanityDeltaAction → -5
  │
  ├─ JumpScareDirector tension reaches 70
  │    └─ sanity < 50 → picks AUDIO_ONLY scare
  │         └─ plays bang.wav (full volume) + EventBus.fire("jump-scare-executed")
  │              └─ LightReactorListener: nearby lights flicker for 1.5s
  │
  ├─ Player reaches CHECKPOINT map object
  │    └─ CheckpointManager.onCheckpointReached("server-room-entry")
  │         └─ SaveSystem.save("autosave", capturedState)
  │              └─ DialogueSystem: "Progress saved." (2s subtitle)
  │
  └─ Player presses ` to open DebugConsole
       └─ types: "sanity 100" → SanitySystem.applyDelta(+100 - current)
       └─ types: "fire enemy-chase-start" → EventBus fires the event
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
      TransitionLevelAction.java
      CheckpointAction.java
      LogAction.java

  audio/
    GameAudioSystem.java
    AudioChannel.java
    SoundBank.java
    AmbienceTrack.java
    SpatialSfxEmitter.java
    WorldSoundEmitter.java
    AudioChannelConfig.java

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

  map/
    MapCollisionBuilder.java
    MapLoader.java
    LoadedMap.java
    MapDocument.java
    MapObject.java
    MapObjectType.java
    MapDocumentLoader.java
    MapDocumentSerializer.java

  enemy/
    EnemyController.java
    EnemyStateMachine.java
    EnemyPerception.java
    EnemyNavigator.java
    EnemyAnimator.java
    NavMesh.java
    NavMeshBuilder.java
    states/
      IdleState.java
      PatrolState.java
      InvestigateState.java
      ChaseState.java
      AttackState.java
      StunnedState.java

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

  interaction/
    RaycastInteractionSystem.java
    InteractionPromptRenderer.java
    InteractionResult.java

  lighting/
    LightManager.java
    GameLight.java
    FlickerController.java
    FlashlightController.java
    LightReactorListener.java

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

  transition/
    SceneTransitionSystem.java
    TransitionScreen.java
    FadeOverlay.java
    LevelRef.java

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

  settings/
    SettingsSystem.java
    SettingsData.java
    KeybindRegistry.java

  footstep/
    FootstepSystem.java
    FootstepSoundSet.java
    SurfaceMaterial.java

lwjgl3/src/main/java/io/github/superteam/resonance/
  mapeditor/
    MapEditorPanel.java
    ObjectPalette.java
    SceneOutlinePanel.java
    ObjectPropertyPanel.java
    MapEditorIntegration.java

assets/
  audio/
    music/
    ambience/
    sfx/
      env/  player/  items/  enemy/  door/  ui/
      footstep/
        concrete/  wood/  metal/  gravel/  water/  carpet/  tile/  dirt/
  dialogue/
    player/
  events/    *.json
  triggers/  *.json
  maps/      *.json
  data/
    tips.json
  models/
    *-map.gltf
  saves/     (runtime — not committed)
    settings.json
    autosave.json
    slot1.json  slot2.json  slot3.json
```

---

# Implementation Order

Build in strict dependency order:

| # | System | Depends on | Priority |
|---|--------|-----------|----------|
| 1 | **Settings System** | Nothing | 🔴 First — everything reads settings |
| 2 | **Keybind Registry** | Settings | 🔴 First — replaces all raw key checks |
| 3 | **Map Loader + BVH Collision** | ModelAssetManager, Bullet | 🔴 Foundation |
| 4 | **GameAudioSystem + SoundBank** | LibGDX Audio | 🔴 Foundation |
| 5 | **SpatialSfxEmitter** | GameAudioSystem, SoundPropagationOrchestrator | 🔴 Foundation |
| 6 | **Footstep Material System** | SpatialSfxEmitter, Bullet raycast | 🟡 Early |
| 7 | **EventState + EventBus + EventRegistry** | Nothing | 🔴 Foundation |
| 8 | **GameEvent + all EventActions** | EventBus, GameAudioSystem | 🔴 Foundation |
| 9 | **EventLoader (JSON)** | GameEvent | 🟡 Early |
| 10 | **Trigger System** | EventBus | 🟡 Early |
| 11 | **Raycast Interaction System** | Bullet, InteractableRegistry | 🟡 Early |
| 12 | **Door & Interactable System** | RaycastInteraction, GameAudioSystem | 🟡 Early |
| 13 | **Map Editor (Swing)** | MapDocument | 🟡 Early |
| 14 | **Map Document Loader** | AudioSystem, TriggerEvaluator | 🟡 Early |
| 15 | **Debug Console** | EventBus, SanitySystem stub | 🟡 Dev tool — build early, use throughout |
| 16 | **Dialogue System** | GameAudioSystem | 🟢 Mid |
| 17 | **Save / Checkpoint System** | EventState, DialogueSystem | 🟢 Mid |
| 18 | **Scene Transition System** | SaveSystem | 🟢 Mid |
| 19 | **Dynamic Lighting System** | WorldShader uniforms | 🟢 Mid |
| 20 | **Sanity / Fear System** | LightManager, EnemyController stub | 🟢 Mid |
| 21 | **Enemy AI — Perception + Navigator** | Bullet, NavMesh, PropagationGraph | 🟠 Complex |
| 22 | **Enemy AI — State Machine + Animator** | EnemyPerception, EnemyNavigator | 🟠 Complex |
| 23 | **Jump Scare Director** | SanitySystem, EnemyController, EventBus | 🟠 Complex |
| 24 | **Wire all systems into GltfMapTestScene** | All above | 🔵 Integration |

---

# Acceptance Criteria

| System | Test | Pass |
|--------|------|------|
| **Settings** | Launch game, change sensitivity, close, relaunch | Sensitivity persists |
| **Keybinds** | Rebind INTERACT to G, press G near door | Door opens |
| **Map BVH** | Load `*-map.gltf` | Player walks on gltf geometry accurately |
| **Audio** | `setAmbience("cave_drip.wav", 2.0f)` | Loops seamlessly, crossfades |
| **Spatial SFX** | Play SFX 20m away vs 2m away | 20m is significantly quieter |
| **Spatial SFX + graph** | SFX with graphEvent set | Enemy director reacts |
| **Footstep** | Walk on WOOD surface | Wood footstep variant plays |
| **Footstep** | Walk on CONCRETE | Concrete variant plays |
| **Footstep — crouch** | Crouch-walk | Quieter steps, still injected into graph |
| **Events** | Fire `"creak-then-groan"` | Sound propagates + flag set + subtitle shows |
| **Events — chain** | Sequence event | Steps fire with correct delays |
| **Events — repeat** | ONCE event fires twice | Blocked on second fire |
| **Triggers — zone** | Player walks into ZONE trigger volume | Target event fires |
| **Triggers — interact** | Player presses F near object | InteractionTrigger fires |
| **Triggers — compound** | Flag not set → compound trigger | Does not fire |
| **Triggers — compound** | Flag set + in zone | Fires |
| **Interaction** | Look at door, press F | Door opens; creak plays based on speed |
| **Interaction** | Look at locked door, press F | "Locked" sound plays |
| **Interaction — keycard** | Have keycard, press F on keycard door | Door opens |
| **Door creak** | Shove door fast | Loud, high-pitch creak; graph injected |
| **Door creak** | Push door slowly | Quiet, low-pitch creak |
| **Lighting** | Place flickering light in map | Flickers with noise curve |
| **Lighting** | Loud PHYSICS_COLLAPSE event fires | Nearby lights flicker 1.5s |
| **Flashlight** | Leave flashlight on for 66s | Battery depletes, flicker starts |
| **Sanity** | Stand in dark room for 10s | Sanity drops; vignette appears |
| **Sanity** | Enemy approaches to 5m | Sanity drops faster |
| **Sanity** | Enter lit safe room | Sanity recovers |
| **Sanity effect** | Sanity < 25 | Chromatic aberration visible |
| **Jump scare** | Tension > 70, sanity > 50 | AUDIO_ONLY scare fires |
| **Jump scare** | Sanity < 50, tension > 70 | ENEMY_APPEAR scare fires |
| **Jump scare** | Scare fires | MIN_SCARE_INTERVAL blocks next scare |
| **Dialogue** | `dialogueSystem.showSubtitle("test", 3.0f)` | Text appears, fades after 3s |
| **Dialogue sequence** | Play 3-line sequence | Lines display in order without overlap |
| **Save** | Reach checkpoint | `autosave.json` written to disk |
| **Save** | Load autosave | Player position, sanity, inventory restored |
| **Transition** | `transitionTo(level02)` | Fade out → loading screen → fade in → new map |
| **Enemy — patrol** | Enemy has waypoints | Walks route indefinitely |
| **Enemy — hearing** | Player makes loud noise | Enemy moves to last-heard position |
| **Enemy — sight** | Player in FOV, clear LOS | Enemy transitions to CHASE |
| **Enemy — lost** | Player breaks LOS for 5s | Enemy returns to INVESTIGATE then PATROL |
| **Enemy — stun** | Stun item hits enemy | STUNNED for N seconds, then PATROL |
| **Debug console** | Press `` ` `` | Console overlay opens |
| **Debug console** | `tp 5 0 5` | Player teleports |
| **Debug console** | `fire alarm-trigger` | Event fires immediately |
| **Debug console** | `sanity 10` | Sanity set to 10, effects activate |
| **Map Editor** | Open via F12 | Swing window opens without freezing game |
| **Map Editor** | Add DOOR, set position, save | `level01.json` contains the door |
| **Map Editor** | Add SOUND_EMITTER, reload map | Drip plays at placed position |
| **Map Editor** | Add CHECKPOINT, reload map | Walking through saves game |

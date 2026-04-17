# CSEL 302 Final Project - Detailed Development Plan
## Resonance - Adaptive 3D Horror Through Acoustic Navigation
**Laguna State Polytechnic University | 2nd Semester AY: 2025-2026**
**Target Submission: May 29, 2026**

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Core Algorithms and ML Models](#2-core-algorithms-and-ml-models)
3. [System Architecture](#3-system-architecture)
4. [Feature Pillars](#4-feature-pillars)
5. [Director AI Brain Design](#5-director-ai-brain-design)
6. [Sound Propagation Pipeline](#6-sound-propagation-pipeline)
7. [Player State Recognition System](#7-player-state-recognition-system)
8. [Gameplay Reaction Engine](#8-gameplay-reaction-engine)
9. [Tutorial Room System](#85-tutorial-room-system)
10. [Procedural Level Generation](#86-procedural-level-generation)
11. [Level Puzzle Design](#87-level-puzzle-design)
12. [Enemy Behavior System](#88-enemy-behavior-system)
13. [Data Schema](#9-data-schema)
14. [Engine Tech Stack](#10-engine-tech-stack)
15. [UI and Screen Design](#11-ui-and-screen-design)
16. [Phase-by-Phase Development Plan](#12-phase-by-phase-development-plan)
17. [File and Folder Structure](#13-file-and-folder-structure)
18. [Testing Plan](#14-testing-plan)
19. [Documentation Checklist](#15-documentation-checklist)
20. [Video Presentation Script](#16-video-presentation-script)
21. [Rubric Alignment](#17-rubric-alignment)
22. [Quick Reference Key Dates](#18-quick-reference-key-dates)

---

## 1. Project Overview

### Project Name
**Resonance**

### Summary
Resonance is a 3D psychological horror game where the player navigates near-total darkness using sound instead of sight. The core mechanic transforms sound events into temporary spatial awareness by running Dijkstra's shortest-path propagation through a graph representation of the level. A Director AI powered by K-Means clustering continuously analyzes player behavior and adapts tension in real time (ambient audio, hallucination cues, enemy pressure, and recovery windows). Each run consists of three procedurally generated levels so no two playthroughs are identical, while each level retains handcrafted puzzle logic to ensure meaningful progression.

### The Three Pillars
```
PILLAR 1 - ACOUSTIC NAVIGATION
  Sound reveals geometry through short-lived sonar pulses
  Dijkstra pathing simulates non-line-of-sight sound travel
  Player learns space by listening, not by seeing
  Breaking or dropping objects produce sounds enemies can hear

PILLAR 2 - DIRECTOR AI ADAPTATION
  K-Means clusters player behavior into psychological states
  Dynamic pacing adjusts pressure and relief moments
  Fear intensity responds to player movement patterns

PILLAR 3 - PROCEDURAL SURVIVAL LOOP
  3 procedurally generated levels — different layout every run
  Each level has unique handcrafted puzzle logic and goal items
  Items spawn randomly across generated rooms
  Mic-enabled Clap/Shout pulse mechanic
  Stylized HUD with stress and noise indicators
  Resource-limited tools in a high-tension environment
```

### Why This Qualifies for CSEL 302
- Dijkstra's Algorithm is a primary gameplay system, not an optional effect.
- K-Means is used as a live intelligent controller for atmosphere adaptation.
- Complexity analysis is explicit and measurable: $O(E \log V)$ for propagation.
- The design combines hard algorithmics and intelligent behavior in one loop.
- Procedural level generation with seeded room graphs ensures algorithmic reproducibility.

---

## 2. Core Algorithms and ML Models

### Algorithm 1: Dijkstra's Algorithm - Sound Propagation Engine

#### Purpose
Compute how sound reaches the player and nearby graph nodes when direct line-of-sight is blocked.

#### Graph Model
- Level represented as weighted graph: $G = (V, E)$
- Node $v \in V$: sampled navigable point in 3D space
- Edge $e(u,v) \in E$: traversable acoustic path between nodes

#### Edge Weight Design
$$
w(u,v)=d(u,v)\cdot m(u,v)+o(u,v)+h(u,v)
$$
Where:
- $d(u,v)$ = Euclidean distance
- $m(u,v)$ = material attenuation multiplier (concrete, metal, wood)
- $o(u,v)$ = occlusion penalty (partial obstruction)
- $h(u,v)$ = vertical/hallway penalty for realism tuning

#### Runtime Procedure
```
OnSoundEvent(sourceNode, baseIntensity):
  dist[] = Dijkstra(sourceNode)
  For each node n:
    intensity[n] = baseIntensity * exp(-alpha * dist[n])
    If intensity[n] > revealThreshold:
      Spawn temporary reveal effect at n
  Use dist[playerNode] and intensity[playerNode] for audio mix and cue strength
```

#### Complexity
- Binary heap implementation: $O(E \log V)$
- Memory: $O(V + E)$

#### Practical Optimization Targets
| Parameter | Target Value | Reason |
|-----------|--------------|--------|
| Node count $V$ per level | 600-1200 | Balance realism and frame time |
| Average degree | 4-8 | Sparse graph for fast pathing |
| Propagation update budget | <= 2.0 ms/event (PC), <= 4.0 ms/event (mobile) | Keep stable FPS |
| Pulse lifetime | 0.4-1.2s | Readable but unsettling sonar |

---

### Algorithm 2: K-Means Clustering - Director AI State Estimator

#### Purpose
Cluster player behavior patterns into live psychological states.

#### Feature Vector (every 3-5 seconds)
$$
x = [\text{avgSpeed},\ \text{rotRate},\ \text{stationaryRatio},\ \text{collisionRate},\ \text{backtrackRate}]
$$

#### Objective Function
$$
J = \sum_{i=1}^{N} \left\|x_i - \mu_{c_i}\right\|^2
$$
Where:
- $\mu_k$ = centroid of cluster $k$
- $c_i$ = assigned cluster for sample $x_i$

#### Intended Clusters
| Cluster | State Label | Behavioral Signature |
|--------|-------------|----------------------|
| C1 | STEALTH | Slow movement, frequent pauses, low collision |
| C2 | PANIC | High speed, high rotation jitter, high collision |
| C3 | FROZEN | Near-zero speed, long stationary windows |

#### Inference Rule
```
state = argmin_k distance(featureWindow, centroid[k])
if minDistance > uncertaintyThreshold:
    state = UNCERTAIN
```

#### Director Mapping
- STEALTH -> lower immediate threat, increase distant cues
- PANIC -> stronger reverb, misleading rear cues, shorter relief windows
- FROZEN -> reduce direct pressure, inject subtle lure sounds to move player

---

### Algorithm 3: Rule-Based Tension Scheduler (Control Layer)

#### Purpose
Convert cluster output into fair, explainable pacing decisions.

#### Core Rules
- Prevent back-to-back high-intensity scares.
- Enforce cooldown after peak events.
- Guarantee recovery windows to avoid frustration.

#### Example Rule
```
if state == PANIC and timeSinceLastScare > 25s:
    triggerHallucinationAudio()
else if state == FROZEN:
    triggerGuidanceEchoNearObjective()
```

---

## 3. System Architecture

### High-Level Architecture
```
+--------------------------------------------------------------+
|                      ECHOES OF THE VOID                      |
+-------------------+-------------------+----------------------+
|   GAMEPLAY CORE   |   AUDIO SYSTEM    |   UI/HUD SYSTEM      |
+---------+---------+---------+---------+----------+-----------+
          |                   |                      |
          v                   v                      v
+--------------------------------------------------------------+
|                        AI / ALGO LAYER                       |
|  +--------------------+  +--------------------+              |
|  | Dijkstra Propagator|  | K-Means Director   |              |
|  +--------------------+  +--------------------+              |
|  +--------------------+  +--------------------+              |
|  | Feature Extractor  |  | Tension Scheduler  |              |
|  +--------------------+  +--------------------+              |
+-----------------------------+--------------------------------+
                              |
                              v
+--------------------------------------------------------------+
|                         DATA LAYER                            |
|  Session Logs | Feature Windows | Cluster Stats | QA Metrics |
+--------------------------------------------------------------+
```

### Runtime Loop
1. Player action emits sound event.
2. Dijkstra computes shortest acoustic spread.
3. Sonar reveal and 3D audio attenuation are applied.
4. Feature extractor updates behavior vector.
5. K-Means classifies current state.
6. Director policy chooses atmosphere response.

---

## 4. Feature Pillars

### Pillar 1 - Acoustic Navigation

**Core Features**
- Near-dark level where sound is primary orientation channel
- Sonar-like temporary reveal at propagated nodes
- Distance- and obstacle-aware sound cues
- Directional 3D audio for critical events

**Algorithm Features**
- Dijkstra per sound event from source node
- Node-level intensity decay based on shortest path distance
- Propagation budget with quality tiers for low-end devices

### Pillar 2 - Adaptive Director AI

**Core Features**
- Real-time player behavior sampling
- K-Means state classification into STEALTH/PANIC/FROZEN
- Dynamic event pacing and atmosphere modulation

**Adaptive Responses**
- Audio: reverb, occlusion, low-frequency stress layers
- Events: hallucination whispers, false footsteps, guided lures
- Threat: enemy patrol density and timing windows

### Pillar 3 - Procedural Survival and Interaction Loop

**Core Features**
- Clap/Shout mechanic via microphone input for active sonar pulse
- Resource-limited items (flare, battery cell, noise decoy)
- Stylized HUD with stress monitor and noise meter
- Diegetic objective hints delivered through environmental sound
- 3 procedurally generated levels — room layout, corridors, and item placements are re-seeded each run
- Level-specific puzzle logic gates progression (see Level Design section)
- Items (keys, tools, consumables) scatter randomly across generated rooms at session start
- Physics-based noise events: objects that fall or break emit a sound event that propagates through the graph; if an enemy node is within hearing range its patrol state is alerted

---

## 5. Director AI Brain Design

### EchoDirector Singleton (C# style)
```csharp
public sealed class EchoDirector
{
    public static EchoDirector Instance { get; } = new EchoDirector();

    private AcousticGraphEngine _graph;
    private KMeansModel _kmeans;
    private FeatureWindowBuffer _featureBuffer;
    private TensionScheduler _scheduler;

    public void Initialize(GameContext ctx)
    {
        _graph = new AcousticGraphEngine(ctx.LevelGraph);
        _kmeans = KMeansModel.Load("Assets/Data/kmeans_centroids.json");
        _featureBuffer = new FeatureWindowBuffer(windowSeconds: 5);
        _scheduler = new TensionScheduler();
    }

    public void OnSoundEvent(SoundEvent evt)
    {
        var result = _graph.Propagate(evt.SourceNode, evt.BaseIntensity);
        SonarRenderer.Render(result.RevealNodes);
        AudioMixerController.ApplyPropagation(result.PlayerIntensity, result.PlayerDistance);
    }

    public void Tick(float dt)
    {
        var features = _featureBuffer.Extract();
        var state = _kmeans.Predict(features);
        _scheduler.Apply(state);
    }
}
```

### Responsibilities
- Coordinate acoustic propagation and visualized sonar feedback
- Classify player state with K-Means inference
- Enforce pacing constraints through scheduler rules

---

## 6. Sound Propagation Pipeline

### Acoustic Pipeline
```
Sound Triggered
  -> Source Node Resolution
  -> Dijkstra Shortest Paths
  -> Intensity Decay Computation
  -> Reveal Node Selection
  -> Spawn Light/Particle Echoes
  -> 3D Audio Mix Update
  -> Optional Enemy Hearing Trigger
```

### Sound Event Types
| Event | Base Intensity | Cooldown | Notes |
|------|-----------------|----------|-------|
| Footstep | Low | 0.2s | Passive navigation hint |
| Object Drop / Break | Medium | 1.0s | Better map reveal; alerts nearby enemies |
| Clap/Shout | High | 8-12s | Player-activated sonar burst |
| Enemy Scream | High | Scripted | Used for fear spikes |
| Physics Collapse | Medium-High | Event-driven | Triggered when breakable objects are destroyed; propagates to enemy hearing nodes |

### Reveal Formula
$$
I_n = I_0 \cdot e^{-\alpha d_n}
$$
Where:
- $I_n$ = intensity at node $n$
- $I_0$ = source intensity
- $d_n$ = shortest-path distance from source to node
- $\alpha$ = attenuation constant tuned per level

### Performance Safeguards
- Event queue cap per frame to avoid spikes
- Node cutoff at max path distance
- Optional async propagation on worker thread

---

## 7. Player State Recognition System

### Feature Extraction
```csharp
public struct PlayerFeatures
{
    public float AvgSpeed;
    public float RotationRate;
    public float StationaryRatio;
    public float CollisionRate;
    public float BacktrackRatio;
}
```

### Sampling Strategy
- Sample interval: 0.2s
- Window length: 5.0s
- Inference cadence: every 2.0s

### State-to-Atmosphere Mapping
| State | Audio Profile | World Response | Goal |
|------|----------------|----------------|------|
| STEALTH | Sparse distant cues | Fewer direct jumpscares | Build suspense |
| PANIC | Strong reverb + fake rear cues | Increased pressure events | Heighten fear |
| FROZEN | Softer threat layer | Guidance echoes near objective | Encourage movement |

### Example Behavioral Flow
1. Player sprints and rotates rapidly.
2. Feature vector shifts near PANIC centroid.
3. Director increases unsettling ambience and misleading cues.
4. After cooldown, pressure decreases to maintain fairness.

---

## 8. Gameplay Reaction Engine

### Reaction Engine Responsibilities
- Consume current player state and acoustic context
- Trigger context-aware horror events
- Respect pacing constraints and cooldowns

### Core Execution Class
```csharp
public class ReactionEngine
{
    public ReactionResult Execute(DirectorContext ctx)
    {
        if (ctx.State == PlayerState.PANIC && ctx.Cooldowns.CanTrigger("Hallucination"))
            return TriggerHallucinationBehindPlayer();

        if (ctx.State == PlayerState.FROZEN && ctx.Cooldowns.CanTrigger("GuidanceEcho"))
            return TriggerGuidanceEchoNearObjective();

        if (ctx.State == PlayerState.STEALTH)
            return TriggerDistantLureSound();

        return ReactionResult.None;
    }
}
```

### Fairness Rules
- Never stack two severe events within 20s.
- Cap panic amplification to prevent overwhelming loops.
- Ensure at least one discoverable guidance cue in long freeze states.

---

## 8.5 Tutorial Room System

### Overview
The game opens in a locked tutorial room before the first procedural level loads. The room is small, safe, and fully lit relative to the rest of the game. A pop-up tooltip system walks the player through every core mechanic before they face any danger.

### Tutorial Pop-Up Sequence
```
Step 1 — Movement
  Pop-up: "Use WASD / Left Stick to move. The room is almost dark."
  Player must walk to the highlighted tile to continue.

Step 2 — Acoustic Navigation (Speak / Shout)
  Pop-up: "Speak or shout into your mic to send a sonar pulse.
           The louder the sound, the wider the reveal."
  Player triggers a pulse and sees nodes illuminate briefly.
  Alternate: Press [Space] if microphone is unavailable.

Step 3 — Reading the Sonar Reveal
  Pop-up: "Lit nodes show what is in front of you — for a moment.
           Listen to directional echoes for shape and distance."
  A breakable crate is shown ahead; player must pulse to see it.

Step 4 — Physics Sounds and Enemy Awareness
  Pop-up: "Objects that fall or break make noise.
           If an enemy is close, it WILL hear it."
  A dummy enemy pawn is placed in the room.
  Player breaks the crate; the dummy turns toward the sound.

Step 5 — HUD Indicators
  Pop-up: "Watch your Noise Meter. Stay quiet. Stay alive."
  Stress monitor and pulse cooldown are highlighted on the HUD.

Step 6 — Proceed
  Pop-up: "Find the key. Reach the exit. Trust your ears."
  Door unlocks. Tutorial room ends. Level 1 loads.
```

### Pop-Up UI Design
- Semi-transparent panel anchored to screen bottom-center.
- Appears after each objective trigger fires.
- Dismissible with [E] / [A Button] after reading delay (1.5 s).
- Cannot be dismissed during the dummy-enemy physics demo (safety gate).
- Tutorial state is stored in `TutorialController.cs`; skippable on repeat runs via Settings.

### Tutorial Controller (C# sketch)
```csharp
public class TutorialController : MonoBehaviour
{
    [SerializeField] private TutorialStep[] steps;
    private int _currentStep = 0;

    public void AdvanceStep()
    {
        if (_currentStep >= steps.Length) { LoadLevel1(); return; }
        steps[_currentStep].ShowPopup();
        _currentStep++;
    }

    private void LoadLevel1() =>
        SceneManager.LoadScene("Level_01_Generated");
}
```

---

## 8.6 Procedural Level Generation

### Overview
Each run generates three distinct levels using a seeded room-graph algorithm. The seed is stored in the session log so a specific run can be replicated for debugging.

### Generation Pipeline
```
SessionSeed (int64)
  -> RoomGraphBuilder  (places N rooms on grid, connects corridors)
  -> RoomContentSeeder (assigns puzzle items, breakables, enemies)
  -> AcousticGraphLinker (overlays Dijkstra node graph onto geometry)
  -> EnemyPatrolAssigner (places patrol paths inside generated corridors)
  -> LevelValidator (ensures solvability: key reachable, exit reachable)
```

### Per-Level Generation Parameters
| Parameter | Level 1 | Level 2 | Level 3 |
|-----------|---------|---------|---------|
| Room count | 8-12 | 12-18 | 18-26 |
| Corridor complexity | Low | Medium | High |
| Enemy count | 1 | 2 | 3-4 |
| Breakable density | Low | Medium | High |
| Darkness level | Dim | Dark | Pitch Black |
| Item scatter slots | 6-10 | 10-16 | 16-24 |

### Item Scatter System
- At generation time, eligible room nodes are tagged as `ItemSpawnSlot`.
- The `ItemSeeder` selects slots randomly (weighted by distance from player start) and places required puzzle items plus optional consumables.
- Required items (axe, key, etc.) are guaranteed reachable by the validator before the level loads.
- Consumables (flare, battery, decoy) fill remaining slots up to the item density cap.

```csharp
public class ItemSeeder
{
    public void Seed(LevelGraph graph, int sessionSeed, LevelConfig cfg)
    {
        var rng = new System.Random(sessionSeed ^ cfg.LevelIndex);
        var slots = graph.GetTaggedNodes("ItemSpawnSlot");
        ShuffleWithRng(slots, rng);

        // Place required puzzle items first
        foreach (var item in cfg.RequiredItems)
            PlaceAt(slots.Dequeue(), item);

        // Fill remaining slots with consumables
        int extras = rng.Next(cfg.MinExtras, cfg.MaxExtras);
        for (int i = 0; i < extras && slots.Count > 0; i++)
            PlaceAt(slots.Dequeue(), PickConsumable(rng));
    }
}
```

---

## 8.7 Level Puzzle Design

### Design Philosophy
Each level has a mandatory puzzle chain that the player must complete to reach the exit. The specific items and obstacles are fixed per level, but their locations are randomized each run by the Item Scatter System.

---

### Level 1 — The Sealed Wing

**Theme:** Abandoned hospital ward. Dim emergency lighting. Single enemy patrol.

**Puzzle Chain:**
```
[Start Room]
    |
    v
Find AXE  (random room spawn)
    |
    v
Break PLANKS blocking Storeroom door  (fixed obstacle, random axe location)
    |
    v
Find MASTER KEY  (spawns inside Storeroom or adjacent room)
    |
    v
Unlock EXIT DOOR  -> Level 2 loads
```

**Breakable Planks Mechanic:**
- Plank-barricade is a destructible object on the storeroom door.
- Interacting without the axe shows: *"You need something to break this."*
- Breaking the planks triggers a `PhysicsNoiseEvent` (Medium-High intensity).
- If the patrol enemy is within 10 graph-node hops, it enters `ALERTED` state and moves toward the sound source.

**Key Notes:**
- Axe and key never spawn in the same room (enforced by validator).
- Player must make noise to proceed — this is intentional design tension.

---

### Level 2 — The Flooded Basement

**Theme:** Waterlogged utility basement. Pipes echo all sounds further. Two enemies.

**Puzzle Chain:**
```
[Start Room]
    |
    v
Find WIRE CUTTERS  (random spawn)
    |
    v
Cut CHAIN-LOCK on generator room door
    |
    v
Activate GENERATOR  (powers a door panel — loud noise event)
    |
    v
Find KEYCARD  (spawns in a room that only opens after generator is on)
    |
    v
Swipe KEYCARD at exit  -> Level 3 loads
```

**Special Rule — Water Conductivity:**
- Flooded tiles increase acoustic graph edge weights (sound travels farther in water areas).
- Both enemies have larger hearing radii on flooded tiles.

---

### Level 3 — The Collapsed Research Wing

**Theme:** Pitch-black, unstable structure. High breakable density. Three to four enemies.

**Puzzle Chain:**
```
[Start Room]
    |
    v
Find CROWBAR  (random spawn)
    |
    v
Pry open VAULT DOOR (blocked corridor — loud event)
    |
    v
Collect 3 FUSE COMPONENTS  (scattered across 3 different rooms)
    |
    v
Insert fuses into CONTROL PANEL  (opens blast door to exit)
    |
    v
Reach EXIT  -> Ending sequence
```

**Special Rule — Structural Instability:**
- Certain tiles have `UnstableFloor` tags.
- Running over them triggers a `PhysicsNoiseEvent` without a player-initiated action.
- Players must walk slowly or risk alerting all nearby enemies.

---

### Puzzle Validator (Solvability Check)
```csharp
public static bool Validate(LevelGraph graph, LevelConfig cfg)
{
    // Confirm all required items are placed
    foreach (var item in cfg.RequiredItems)
        if (!graph.HasItem(item)) return false;

    // BFS from player start to each required item and then to exit
    return BFSReachable(graph, "PlayerStart", cfg.RequiredItems)
        && BFSReachable(graph, cfg.RequiredItems.Last(), "Exit");
}
```

---

## 8.8 Enemy Behavior System

### Overview
Enemy behavior in Resonance is built around a decaying attention model. Enemies react to sound but are not omniscient — their aggro fades, their attention can be stolen by distractions, and they lose the player entirely beyond a maximum tracking range. Each level introduces a harder enemy variant with different movement, hearing, and persistence. Even when not directly chasing, enemies always drift toward the player's general zone using a trail-sniffing system, keeping sustained pressure without feeling unfair.

---

### 8.8.1 Aggro Meter and Attention Decay

Every enemy has a single float value called `AggroLevel` ranging from `0.0` (completely calm) to `1.0` (full hunt mode). It is not a binary switch — it builds up and bleeds down continuously.

```
AggroLevel rules:
  - Hearing a sound  ->  += soundIntensity * enemy.HearingSensitivity
  - Each second of silence  ->  -= enemy.AggroDecayRate
  - Reaching player's node directly  ->  set to 1.0 immediately
  - Falling below 0.05  ->  enemy returns to PATROL
```

**Decay Rate Design Intent:** The enemy's attention span is intentionally short. A player who triggers a sound and immediately goes silent has a realistic window to hide before the enemy gives up. This makes stillness a viable and rewarding survival skill.

```csharp
public class EnemyBrain : MonoBehaviour
{
    public float AggroLevel       { get; private set; }  // 0.0 – 1.0
    public float AggroDecayRate;      // units per second, varies by enemy type
    public float HearingSensitivity;  // multiplier on incoming sound intensity

    private void Update()
    {
        // Passive decay every frame
        AggroLevel = Mathf.Max(0f, AggroLevel - AggroDecayRate * Time.deltaTime);

        // Drive state machine from current aggro value
        UpdateStateMachine();
    }

    public void OnSoundHeard(float propagatedIntensity)
    {
        float gain = propagatedIntensity * HearingSensitivity;
        AggroLevel = Mathf.Min(1f, AggroLevel + gain);
        _lastHeardNode = _nearestSoundNode;
    }
}
```

### Aggro Threshold Bands
| AggroLevel Range | Resulting State | Enemy Behavior |
|-----------------|-----------------|----------------|
| 0.00 – 0.10 | PATROL | Follows scripted patrol path, ignores faint sounds |
| 0.10 – 0.35 | CURIOUS | Moves toward last heard node; slow, stops to "listen" |
| 0.35 – 0.70 | ALERTED | Moves quickly to sound source; begins search sweep |
| 0.70 – 1.00 | HUNTING | Charges player last-known position; does not give up easily |

---

### 8.8.2 Distraction Mechanic

Players can throw a **Noise Decoy** (or any droppable item) to redirect the enemy's attention. When an object lands and makes noise at a target node, the enemy treats that event identically to a player-generated sound — it spikes `AggroLevel` toward the decoy's location and overwrites `_lastHeardNode`.

```
Design Intent:
  Enemy's attention is a resource the player can steal.
  Throwing something loud while the enemy is HUNTING will pull it away —
  but only for as long as the AggroLevel stays high enough from that new source.
  Because aggro decays, a single decoy buys time, not permanent safety.
```

**Distraction Rules:**
- A decoy sound event is processed through the same Dijkstra propagation pipeline as all other sounds.
- If `propagatedIntensity` at the enemy node exceeds the enemy's current aggro-equivalent threshold, the enemy re-targets the decoy node.
- The enemy will investigate the decoy location, find nothing, and then `AggroLevel` continues its natural decay from that point.
- Multiple rapid distractions have diminishing returns — the enemy's `HearingSensitivity` is halved for 5 seconds after being distracted twice in a row (it learns to be suspicious of noise patterns).

```csharp
public void OnDistracted(int decoyNode, float decoyIntensity)
{
    float gain = decoyIntensity * HearingSensitivity * _distractionMultiplier;
    if (gain > AggroLevel * 0.6f)   // decoy must be meaningfully louder than current focus
    {
        AggroLevel = Mathf.Min(1f, AggroLevel + gain * 0.5f);
        _lastHeardNode = decoyNode;
        _distractionCount++;

        if (_distractionCount >= 2)
        {
            _distractionMultiplier = 0.5f;
            Invoke(nameof(ResetDistractionMultiplier), 5f);
        }
    }
}
```

---

### 8.8.3 Maximum Tracking Range and Leash Limit

If the player puts enough distance between themselves and the enemy, the enemy physically cannot track them anymore. This is the **Leash Limit** — a maximum graph-node hop distance beyond which the enemy's hearing cannot effectively reach.

```
If dist(enemyNode, playerNode) > enemy.LeashRadius:
    -> Enemy cannot receive player footstep events
    -> AggroLevel decays at double rate (enemy "loses the scent")
    -> If AggroLevel reaches 0 while beyond leash: enemy enters LOST state
```

**LOST State Behavior:**
The enemy does not simply stop. It enters a slow drift back toward the player's general zone (see Trail Sniffing below). This prevents the exploit of just running far away and waiting — the enemy always finds its way back, just slowly.

---

### 8.8.4 Proximity Wandering (Zone Pressure)

Even in PATROL state, the enemy is not random. Its patrol path is dynamically biased toward the player's general area using a lightweight zone system.

- The level is divided into **Pressure Zones** (large grid cells, 3–5 rooms each).
- The player's current zone is updated every 10 seconds (low frequency to avoid real-time tracking).
- The enemy's patrol path generator gives higher weight to waypoints inside or adjacent to the player's current zone.

This means the enemy gradually closes in on the player's side of the map even without hearing anything — creating creeping dread without being unfair.

```csharp
public Vector3 PickNextPatrolWaypoint()
{
    // Weight waypoints by proximity to player's last known zone
    return _patrolWaypoints
        .OrderBy(w => ZoneDistanceCost(w, _playerZone) + Random.Range(0f, _patrolRandomness))
        .First();
}
```

---

### 8.8.5 Trail Sniffing

When the enemy is in LOST or CURIOUS state and has no active sound target, it falls back to following the **player's scent trail** — a queue of the player's 10 most recently visited graph nodes, updated every 2 seconds.

**Trail System:**
- `PlayerTrailBuffer`: a rolling queue of the last 10 player node positions with timestamps.
- Trail nodes older than `TrailFadeTime` (default: 30 seconds) are considered cold and ignored.
- The enemy navigates to the most recent warm trail node it can reach, then the next, building toward the player's current position.
- Trail following is slower than active hunting — this is intentional. It is a slow creep, not a charge.

```csharp
public class PlayerTrailBuffer
{
    private readonly Queue<TrailNode> _trail = new();
    private const int MaxNodes = 10;

    public void RecordPosition(int graphNode, float timestamp)
    {
        if (_trail.Count >= MaxNodes) _trail.Dequeue();
        _trail.Enqueue(new TrailNode(graphNode, timestamp));
    }

    public int? GetWarmestReachableNode(float now, float fadeTime)
    {
        return _trail
            .Where(n => (now - n.Timestamp) < fadeTime)
            .OrderByDescending(n => n.Timestamp)
            .Select(n => (int?)n.GraphNode)
            .FirstOrDefault();
    }
}
```

**Trail Sniffing State Logic:**
```
if state == LOST or CURIOUS and no active sound target:
    target = TrailBuffer.GetWarmestReachableNode(now, TrailFadeTime)
    if target != null:
        MoveTo(target) at SniffSpeed
    else:
        fall back to Proximity Wandering patrol
```

**Audio Cue:** Trail sniffing plays a distinct low-frequency breathing or scraping sound so the player can hear the enemy closing in — giving them a chance to react.

---

### 8.8.6 Per-Level Enemy Types

Each level introduces a new enemy variant. They are not simply stat-bumped versions of each other — they have fundamentally different movement styles, silhouettes, and behavioral profiles.

#### Level 1 — The Warden
```
Form:        Walks upright. Slow, deliberate footsteps.
Speed:       Slow (PATROL: 1.4 m/s | HUNTING: 2.8 m/s)
Hearing:     Standard (HearingSensitivity: 1.0 | HearingThreshold: 0.35)
LeashRadius: 20 nodes
AggroDecay:  0.08 / second  (forgets fairly quickly)
TrailFade:   20 seconds     (cold trail)
Distraction: Easily distracted — full susceptibility
Special:     None
Audio Cue:   Heavy boot steps. Rattling keys.
Design Note: Teaches the player core mechanics. Forgiving aggro decay means
             early mistakes are survivable. Players learn to hide, not fight.
```

#### Level 2 — The Crawler
```
Form:        Runs on all fours. Fast, skittering movement.
Speed:       Fast (PATROL: 2.5 m/s | HUNTING: 5.5 m/s)
Hearing:     Expanded (HearingSensitivity: 1.6 | HearingThreshold: 0.20)
             Hearing radius is 40% larger than Level 1.
LeashRadius: 28 nodes
AggroDecay:  0.05 / second  (holds attention longer)
TrailFade:   35 seconds     (follows older trails)
Distraction: Partially susceptible — distractionMultiplier floor is 0.7
             (harder to fully redirect)
Special:     "Scurry Burst" — when HUNTING, randomly sprints in bursts of
             speed for 1.5 seconds then pauses to listen. Unpredictable pace.
Audio Cue:   Fast scraping of limbs. Low wet snarl.
Design Note: Forces the player to manage sound budgets more carefully.
             Running to escape is dangerous because it makes noise and the
             Crawler is already faster.
```

#### Level 3 — The Stalker
```
Form:        Moves in near silence. Upright but hunched, elongated.
Speed:       Medium (PATROL: 1.8 m/s | HUNTING: 4.0 m/s)
Hearing:     Extreme (HearingSensitivity: 2.4 | HearingThreshold: 0.08)
             Reacts to footsteps at normal walking speed.
LeashRadius: 40 nodes
AggroDecay:  0.02 / second  (nearly permanent aggro once raised)
TrailFade:   60 seconds     (follows very old trails)
Distraction: Resistant — distractionMultiplier floor is 0.4, and a second
             distraction within 8 seconds is fully ignored.
Special:     "Dead Silence" — The Stalker makes almost no sound itself.
             Players cannot hear it approaching via audio cues alone.
             "Pack Memory" — in 3-enemy configurations, enemies share
             heard node data with each other (radio-style broadcast on
             ALERTED events). One enemy hearing something updates all nearby
             enemies' _lastHeardNode.
Audio Cue:   Almost nothing. Occasional distant click. Silence IS the cue.
Design Note: The final test. Combines all learned skills. Players must be
             almost completely silent, use decoys wisely, and plan puzzle
             noise events carefully around all enemy positions.
```

---

### 8.8.7 Enemy Behavior Summary Table

| Attribute | Level 1 Warden | Level 2 Crawler | Level 3 Stalker |
|-----------|---------------|-----------------|-----------------|
| Movement form | Upright walk | All-fours sprint | Hunched upright |
| Patrol speed | 1.4 m/s | 2.5 m/s | 1.8 m/s |
| Hunt speed | 2.8 m/s | 5.5 m/s | 4.0 m/s |
| Hearing sensitivity | 1.0 | 1.6 | 2.4 |
| Hearing threshold | 0.35 | 0.20 | 0.08 |
| Aggro decay rate | 0.08 /s | 0.05 /s | 0.02 /s |
| Leash radius | 20 nodes | 28 nodes | 40 nodes |
| Trail fade time | 20 s | 35 s | 60 s |
| Distraction susceptibility | Full | Partial | Resistant |
| Special ability | — | Scurry Burst | Dead Silence + Pack Memory |
| Audio cue | Boot steps | Skittering | Near silence |

---

### 8.8.8 Full Alert Pipeline (Updated)

```
Any Sound Event (footstep, physics, decoy, puzzle interaction)
  -> PhysicsNoiseEvent(sourceNode, baseIntensity)
  -> Dijkstra propagation across acoustic graph
  -> For each EnemyBrain in scene:
       propagatedIntensity = intensity[enemyNode]
       if propagatedIntensity > 0:
           enemy.OnSoundHeard(propagatedIntensity)
           // AggroLevel += propagatedIntensity * HearingSensitivity

  -> EnemyBrain.Update() each frame:
       AggroLevel -= AggroDecayRate * deltaTime
       if dist(enemy, player) > LeashRadius:
           AggroLevel -= AggroDecayRate * deltaTime  // double decay
       UpdateStateMachine(AggroLevel)

  -> State Machine Output:
       PATROL   -> WeightedPatrolWaypoint(playerZone)
       CURIOUS  -> MoveToward(_lastHeardNode) at PatrolSpeed
       ALERTED  -> MoveToward(_lastHeardNode) at HuntSpeed * 0.75
       HUNTING  -> MoveToward(playerLastKnownNode) at HuntSpeed
       LOST     -> TrailSniff() or ProximityWander()
```

### Design Intent
The enemy system is built around three tensions the player must manage simultaneously: **noise budget** (how much sound can I make before I'm heard), **attention window** (if I triggered the enemy, how long do I have before it forgets), and **zone pressure** (even if I escape, the enemy drifts back). Distractions are a tool but not a free pass — the enemy learns. Trail sniffing means the clock never fully resets. Each level layers on a new form of this pressure so that by Level 3, the player must orchestrate all learned skills at once.

---

```sql
CREATE TABLE session_runs (
    id                TEXT PRIMARY KEY,
    started_at        INTEGER,
    ended_at          INTEGER,
    level_id          TEXT,
    difficulty        TEXT,
    survived          INTEGER,
    total_play_time_s INTEGER
);

CREATE TABLE audio_events (
    id                TEXT PRIMARY KEY,
    session_id        TEXT NOT NULL,
    event_type        TEXT NOT NULL,
    source_node       INTEGER,
    base_intensity    REAL,
    player_distance   REAL,
    timestamp_ms      INTEGER,
    FOREIGN KEY (session_id) REFERENCES session_runs(id)
);

CREATE TABLE feature_windows (
    id                TEXT PRIMARY KEY,
    session_id        TEXT NOT NULL,
    avg_speed         REAL,
    rotation_rate     REAL,
    stationary_ratio  REAL,
    collision_rate    REAL,
    backtrack_ratio   REAL,
    inferred_state    TEXT,
    min_cluster_dist  REAL,
    timestamp_ms      INTEGER,
    FOREIGN KEY (session_id) REFERENCES session_runs(id)
);

CREATE TABLE director_actions (
    id                TEXT PRIMARY KEY,
    session_id        TEXT NOT NULL,
    state_input       TEXT,
    action_type       TEXT,
    intensity_level   REAL,
    cooldown_applied  INTEGER,
    timestamp_ms      INTEGER,
    FOREIGN KEY (session_id) REFERENCES session_runs(id)
);

CREATE TABLE performance_metrics (
    id                TEXT PRIMARY KEY,
    session_id        TEXT NOT NULL,
    frame_time_ms     REAL,
    propagation_ms    REAL,
    active_nodes      INTEGER,
    timestamp_ms      INTEGER,
    FOREIGN KEY (session_id) REFERENCES session_runs(id)
);
```

---

## 10. Engine Tech Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Engine | Unity 2022 LTS | 3D game framework |
| Language | C# | Core gameplay and AI logic |
| Audio | Unity Audio Mixer + Spatial Audio | Directional horror soundscape |
| Rendering | URP | Performance-focused visuals |
| Input | New Input System | Keyboard, controller, mic actions |
| Data | JSON + SQLite (optional) | Logs, balancing data, replay analysis |
| Profiling | Unity Profiler | CPU/GPU/frame diagnostics |
| Version Control | Git + GitHub | Collaboration and iteration |

### Key Script Groups
- `Algorithms/AcousticGraphEngine.cs`
- `Algorithms/DijkstraPathfinder.cs`
- `AI/KMeansDirector.cs`
- `AI/TensionScheduler.cs`
- `Audio/SonarRenderer.cs`
- `Systems/ReactionEngine.cs`

---

## 11. UI and Screen Design

### Navigation Flow
```
Main Menu
  -> New Run
  -> Settings
  -> Calibration (Audio/Mic)
  -> Credits

In-Game
  -> Pause Menu
  -> Inventory
  -> Objective Log
```

### HUD Elements
- Stress monitor (heart-rate style meter based on inferred state)
- Noise meter (current sound output intensity)
- Pulse cooldown indicator (for Clap/Shout sonar)
- Minimal objective hint strip

### In-Game HUD Layout (Concept)
```
+--------------------------------------------------+
| Objective: Reach the generator room              |
|                                                  |
|                   [Dark World]                   |
|                                                  |
| Stress: |||||||---      Noise: ||||----          |
| Pulse CD: [#####.....]   Items: Flare x1         |
+--------------------------------------------------+
```

### Interaction Design Notes
- UI is intentionally minimal to preserve fear and uncertainty.
- Key feedback is auditory first, visual second.
- Sonar pulse visual should be brief and die quickly.

---

## 12. Phase-by-Phase Development Plan

### PHASE 0 - Foundation Setup
**Duration: Days 1-3 (Week 1)**

**Goal:** Create stable project skeleton and core scene loop.

- [ ] Initialize Unity project with URP template
- [ ] Create scene loading pipeline and basic pause menu
- [ ] Implement player controller and camera look
- [ ] Add dark level prototype with collision geometry
- [ ] Build Tutorial Room scene with pop-up UI system (`TutorialController.cs`)
- [ ] Implement procedural room graph builder scaffold (`RoomGraphBuilder.cs`)

**Deliverable:** Playable movement prototype in dark map with working tutorial pop-up flow.

---

### PHASE 1 - Acoustic Graph and Dijkstra Core
**Duration: Days 4-10 (Week 1-2)**

**Goal:** Sound propagation works with shortest-path calculations.

- [ ] Build node graph from waypoint/voxel sampling
- [ ] Implement weighted edge generation
- [ ] Implement `DijkstraPathfinder.cs` with binary heap priority queue
- [ ] Implement intensity decay and reveal node filtering
- [ ] Add temporary particle/light sonar effect
- [ ] Add unit tests for shortest-path correctness

**Deliverable:** Sound-based map reveal using Dijkstra.

---

### PHASE 2 - Director AI with K-Means
**Duration: Days 11-17 (Week 2-3)**

**Goal:** Real-time player-state clustering drives adaptation.

- [ ] Implement feature buffer and extraction pipeline
- [ ] Collect sample playtest data for training
- [ ] Implement K-Means training script (offline tool)
- [ ] Export centroids to JSON and load at runtime
- [ ] Implement nearest-centroid inference in game loop
- [ ] Add state debug overlay for tuning

**Deliverable:** Live STEALTH/PANIC/FROZEN state detection.

---

### PHASE 3 - Adaptive Event System Integration
**Duration: Days 18-24 (Week 3-4)**

**Goal:** Director output changes atmosphere and threats.

- [ ] Implement `TensionScheduler.cs` rule set
- [ ] Implement `ReactionEngine.cs` actions and cooldowns
- [ ] Add hallucination and lure sound event library
- [ ] Connect state to audio mixer parameters
- [ ] Add fairness constraints and safeguards

**Deliverable:** Adaptive horror pacing based on player behavior.

---

### PHASE 4 - Mic Interaction, Survival Layer, and Procedural Systems
**Duration: Days 25-31 (Week 4-5)**

**Goal:** Add clap/shout sonar, survival mechanics, procedural generation, level puzzles, and physics alerting.

- [ ] Implement microphone input threshold detection
- [ ] Map clap/shout to high-intensity sonar pulse
- [ ] Add pulse cooldown and anti-spam gating
- [ ] Add resource items (flare, decoy, battery)
- [ ] Add inventory HUD and item use flow
- [ ] Implement `ProceduralLevelGenerator.cs` — seeded room and corridor layout
- [ ] Implement `ItemSeeder.cs` — randomized item placement across rooms
- [ ] Implement `LevelValidator.cs` — solvability check before scene loads
- [ ] Build Level 1 puzzle chain (axe → planks → key → exit)
- [ ] Build Level 2 puzzle chain (wire cutters → generator → keycard → exit)
- [ ] Build Level 3 puzzle chain (crowbar → fuse components → control panel → exit)
- [ ] Implement `PhysicsNoiseEvent` — breakables and drops emit propagated sound
- [ ] Wire physics events into enemy hearing and alert state machine

**Deliverable:** Three playable procedurally generated levels with unique puzzles, random item placement, and enemy alerting on physics sounds.

---

### PHASE 5 - UX Polish and Optimization
**Duration: Days 32-38 (Week 5-6)**

**Goal:** Improve clarity, tension consistency, and performance.

- [ ] Polish HUD readability and animation timing
- [ ] Add accessibility options (subtitle cues, intensity slider)
- [ ] Profile propagation and optimize graph density
- [ ] Add quality presets for low-end hardware
- [ ] Finalize audio mix balancing for all states

**Deliverable:** Stable and polished near-final build.

---

### PHASE 6 - Testing, Documentation, Two-Player Mode, and Video
**Duration: Days 39-45 (Week 6)**

**Goal:** Complete deliverables for CSEL 302 submission and add optional 2-player gameplay for final polish.

- [ ] Execute all unit/integration/performance tests
- [ ] Document algorithm math and complexity analysis
- [ ] Prepare final report and screenshots
- [ ] Record and edit 5-7 minute presentation video
- [ ] Add optional 2-player mode (cooperative/asymmetric) and test gameplay flow
- [ ] Build submission package (source, executable, docs, video)

**Deliverable:** Full final project package with an optional 2-player mode.

---

## 13. File and Folder Structure

```
resonance/
├── Assets/
│   ├── Scenes/
│   │   ├── MainMenu.unity
│   │   ├── TutorialRoom.unity
│   │   ├── Level01_Generated.unity
│   │   ├── Level02_Generated.unity
│   │   ├── Level03_Generated.unity
│   │   └── Calibration.unity
│   ├── Scripts/
│   │   ├── Algorithms/
│   │   │   ├── DijkstraPathfinder.cs
│   │   │   ├── ProceduralLevelGenerator.cs
│   │   │   ├── RoomGraphBuilder.cs
│   │   │   ├── ItemSeeder.cs
│   │   │   └── LevelValidator.cs
│   │   │   ├── AcousticGraphEngine.cs
│   │   │   └── PriorityQueue.cs
│   │   ├── AI/
│   │   │   ├── KMeansDirector.cs
│   │   │   ├── FeatureExtractor.cs
│   │   │   ├── TensionScheduler.cs
│   │   │   ├── DirectorState.cs
│   │   │   ├── EnemyBrain.cs
│   │   │   ├── EnemyStateMachine.cs
│   │   │   ├── PlayerTrailBuffer.cs
│   │   │   ├── ProximityWanderController.cs
│   │   │   └── EnemyTypeConfig.cs
│   │   ├── Audio/
│   │   │   ├── SonarRenderer.cs
│   │   │   ├── SpatialCueController.cs
│   │   │   └── MicInputListener.cs
│   │   ├── Tutorial/
│   │   │   └── TutorialController.cs
│   │   ├── Systems/
│   │   │   ├── ReactionEngine.cs
│   │   │   ├── InventorySystem.cs
│   │   │   └── SaveLoadSystem.cs
│   │   ├── UI/
│   │   │   ├── HUDController.cs
│   │   │   ├── PauseMenuController.cs
│   │   │   └── CalibrationUIController.cs
│   │   └── Core/
│   │       ├── GameManager.cs
│   │       ├── SceneBootstrap.cs
│   │       └── Config.cs
│   ├── Audio/
│   ├── Prefabs/
│   ├── Materials/
│   └── Resources/
│       └── Data/
│           ├── kmeans_centroids.json
│           └── balancing_config.json
├── Tests/
│   ├── Unit/
│   ├── Integration/
│   └── Performance/
├── Docs/
│   ├── CSEL302_Resonance_Documentation.pdf
│   ├── test_cases.md
│   └── architecture_diagrams/
└── README.md
```

---

## 14. Testing Plan

### Unit Tests

| ID | Component | Input | Expected Output |
|----|-----------|-------|----------------|
| U01 | DijkstraPathfinder | Simple 5-node graph | Correct shortest distances |
| U02 | DijkstraPathfinder | Graph with blocked edge penalties | Chooses lower total weighted path |
| U03 | AcousticGraphEngine | Sound event at node A | Reveal nodes follow threshold logic |
| U04 | IntensityDecay | Distance increases | Intensity monotonically decreases |
| U05 | FeatureExtractor | Fast, high-rotation movement samples | High speed and rotation metrics |
| U06 | KMeansDirector | Feature near PANIC centroid | Classified as PANIC |
| U07 | KMeansDirector | Feature near FROZEN centroid | Classified as FROZEN |
| U08 | TensionScheduler | Two high-intensity triggers in short interval | Second trigger blocked by cooldown |
| U09 | MicInputListener | Input amplitude over threshold | Clap/Shout pulse event emitted |
| U10 | ReactionEngine | State=STEALTH | Selects low-direct-pressure action |
| U11 | EnemyBrain.AggroDecay | No sound events for 5 seconds | AggroLevel reaches 0; state becomes PATROL |
| U12 | EnemyBrain.OnSoundHeard | High-intensity propagated sound | AggroLevel rises above HUNTING threshold |
| U13 | EnemyBrain.OnDistracted | Decoy thrown while HUNTING | lastHeardNode updates to decoy node |
| U14 | EnemyBrain.OnDistracted | Two decoys within 5s | distractionMultiplier halved on second decoy |
| U15 | EnemyBrain.LeashLimit | Player node distance > LeashRadius | AggroLevel decays at double rate |
| U16 | PlayerTrailBuffer | 12 node positions recorded | Only last 10 retained in queue |
| U17 | PlayerTrailBuffer.GetWarmest | Mix of warm and cold trail nodes | Returns most recent node within TrailFadeTime |
| U18 | PlayerTrailBuffer.GetWarmest | All nodes older than TrailFadeTime | Returns null; enemy falls back to wander |
| U19 | EnemyStateMachine | AggroLevel = 0.50 | State resolves to ALERTED |
| U20 | ProximityWanderController | Player zone updated | Next patrol waypoint biased toward player zone |

### Integration Tests

| ID | Scenario | Expected Behavior |
|----|----------|------------------|
| I01 | Footstep in corridor with obstacles | Propagation follows corridor routes, not straight line |
| I02 | Clap pulse in open room | Wide reveal with high node coverage |
| I03 | Continuous sprint + jitter camera | Director shifts to PANIC and raises tension layers |
| I04 | Long stationary period | Director shifts to FROZEN and emits guidance echo |
| I05 | Multiple scare events rapidly | Scheduler enforces cooldown and fairness |
| I06 | Low-end quality preset | Stable frame time with reduced propagation cost |
| I07 | Player makes noise then goes silent | Enemy AggroLevel decays; enemy returns to PATROL before reaching player |
| I08 | Player throws decoy while enemy is HUNTING | Enemy re-targets decoy node; player has escape window |
| I09 | Two decoys thrown within 5 seconds | Second decoy has reduced effect; enemy distractionMultiplier applied |
| I10 | Player runs to max LeashRadius distance | AggroLevel decays at double rate; enemy enters LOST state |
| I11 | Enemy in LOST state with warm trail nodes | Enemy navigates trail toward player's last positions |
| I12 | Trail nodes all older than TrailFadeTime | Enemy falls back to proximity wandering toward player zone |
| I13 | Level 2 Crawler HUNTING state | Enemy uses Scurry Burst; speed spikes then pauses unpredictably |
| I14 | Level 3 Stalker hears one footstep | AggroLevel rises above CURIOUS threshold due to high sensitivity |
| I15 | Two Stalkers in Level 3 — one hears player | Pack Memory broadcasts lastHeardNode to both; both navigate toward player |
| I16 | Inventory decoy use | Decoy generates sound, propagation updates enemy brain |
| I17 | Calibration scene mic test | User can tune threshold and save settings |
| I18 | Full 15-minute session | Logs capture states, actions, and performance metrics |

### Performance Tests

| ID | Metric | Target |
|----|--------|--------|
| P01 | Dijkstra per-event time | <= 2.0 ms average on target PC |
| P02 | Total frame time | <= 16.7 ms for 60 FPS target |
| P03 | Memory footprint | No progressive leak across 20-minute run |

---

## 15. Documentation Checklist

- [ ] Cover page (project title, name, course, section, date)
- [ ] Table of contents
- [ ] Problem statement and project objectives
- [ ] Algorithm section
  - [ ] Dijkstra formulation and weighted graph explanation
  - [ ] Complexity analysis with $O(E \log V)$
  - [ ] K-Means objective and centroid update logic
  - [ ] Feature engineering and normalization method
  - [ ] Trade-offs and limitations
- [ ] Architecture diagram and pipeline flowchart
- [ ] Feature breakdown by pillar
- [ ] Data schema and telemetry rationale
- [ ] Test section with expected vs actual results
- [ ] User guide
  - [ ] Controls and interaction mapping
  - [ ] Mic calibration and sonar usage
  - [ ] Gameplay loop and win/lose flow
- [ ] Screenshots of all key screens

---

## 16. Video Presentation Script

**Target Duration: 5-7 minutes**

### Segment 1 - Introduction (0:00-0:45)
- Introduce project and core idea: auditory-first horror navigation.
- Explain why darkness + sound changes player behavior.

### Segment 2 - Algorithm Core (0:45-2:00)
- Explain graph model and Dijkstra shortest-path propagation.
- Show complexity and optimization choices for hardware constraints.
- Explain K-Means clustering and player-state labels.

### Segment 3 - Gameplay Demo: Acoustic Navigation (2:00-3:30)
- Demonstrate footstep-based local reveal.
- Trigger clap/shout pulse and show larger sonar reveal.
- Show non-line-of-sight sound behavior through walls/corridors.

### Segment 4 - Adaptive Director AI Demo (3:30-5:00)
- Sprint and rotate aggressively -> PANIC state response.
- Stop moving for long interval -> FROZEN guidance response.
- Highlight fairness logic (cooldowns, recovery window).

### Segment 5 - UI and Systems Demo (5:00-6:00)
- Show stress monitor, noise meter, pulse cooldown.
- Demonstrate inventory item that manipulates sound.

### Segment 6 - Closing (6:00-6:30)
- Summarize algorithm + ML synergy.
- Emphasize uniqueness and rubric alignment.

---

## 17. Rubric Alignment

### Algorithm Understanding (40%)
| Criterion | How Resonance Meets It |
|-----------|----------------------------------|
| Choice of Algorithm | Dijkstra for sound pathing and K-Means for adaptive behavior are central to gameplay. |
| Explanation Quality | Includes formulas, complexity, and practical optimization constraints. |
| Theoretical Knowledge | Weighted graph design, shortest paths, clustering objective, and inference explained clearly. |
| Limitations | Graph density trade-offs, cluster drift, and ambiguous states documented. |
| Evaluation | Quantified by path correctness, inference stability, and gameplay response quality. |

### Correct Application (30%)
| Criterion | How Resonance Meets It |
|-----------|----------------------------------|
| Data Handling | Structured feature windows and normalization for robust clustering. |
| Implementation | Dijkstra and K-Means implemented for runtime game decisions. |
| Hyperparameter Tuning | Node density, attenuation factor, cluster count, and thresholds are tunable and justified. |
| Testing | Unit, integration, and performance tests validate behavior and efficiency. |
| Real Problem Fit | Delivers adaptive horror experience tied directly to player behavior and sound geometry. |

### Documentation (20%)
| Criterion | Target |
|-----------|--------|
| Project Overview | 5/5 |
| Code/Architecture Docs | 4/5 |
| Testing Evidence | 5/5 |
| User Guide | 5/5 |

### Video Presentation (10%)
| Criterion | Strategy |
|-----------|----------|
| Organization | Structured 6-part narrative |
| Technical Depth | Live explanation of Dijkstra and K-Means decisions |
| Originality | Auditory navigation plus adaptive horror pacing |
| Visual/Audio Quality | Clear capture, layered audio demonstration |
| Timeliness | Submit before deadline |

---

## 18. Quick Reference Key Dates

| Milestone | Target Date |
|-----------|-------------|
| Phase 0 Complete (Setup) | End of Week 1 |
| Phase 1 Complete (Dijkstra Core) | End of Week 2 |
| Phase 2 Complete (K-Means Director) | End of Week 3 |
| Phase 3 Complete (Adaptive Events) | End of Week 4 |
| Phase 4 Complete (Mic + Survival Systems) | End of Week 5 |
| Phase 5 Complete (Polish + Optimization) | Mid Week 6 |
| Phase 6 Complete (Testing + Docs + Video) | End of Week 6 |
| **SUBMISSION DEADLINE** | **May 29, 2026** |

---

*Document prepared for CSEL 302 Introduction to Intelligent Systems - Final Project*  
*Resonance - Adaptive 3D Horror Through Acoustic Navigation*
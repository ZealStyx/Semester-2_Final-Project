# Resonance — Three Feature Plans

**Features:**
1. Enemy Footprint Tracking (imperfect trail following)
2. Sanity Hallucinations (fake enemy sounds / fake footsteps)
3. Breath-Hold Mechanic

---

## Table of Contents

1. [Enemy Footprint Tracking](#1-enemy-footprint-tracking)
2. [Sanity Hallucinations](#2-sanity-hallucinations)
3. [Breath-Hold Mechanic](#3-breath-hold-mechanic)
4. [How the three interact](#4-how-the-three-interact)
5. [Package layout additions](#5-package-layout-additions)
6. [Integration checklist](#6-integration-checklist)
7. [Acceptance criteria](#7-acceptance-criteria)

---

## 1. Enemy Footprint Tracking

### Design intent

The enemy can roughly follow where the player has been — but not perfectly. It finds a footprint, orients toward it with some drift, then searches for the next one. A crouching player leaves faint traces. A sprinting player leaves a highway. Prints fade over time. The enemy is tracking a ghost of your movement, not a GPS signal.

### How imperfection is modelled

Four sources of imprecision are layered together:

| Source | Effect |
|--------|--------|
| **Detection radius** | Enemy only finds prints within a limited search radius of its current position — it can't see the whole trail at once |
| **Angular drift** | When moving toward a footprint, a random drift angle (±20°) is applied so the enemy wobbles around the true path |
| **Intensity threshold** | Faint prints (crouching on carpet) may be invisible to the enemy |
| **Expiry** | Prints older than `PRINT_LIFETIME_SECONDS` disappear — the trail goes cold |

---

### Package: `enemy/tracking`

```
enemy/tracking/
  FootprintTrail.java         — ring buffer of player footprints
  Footprint.java              — single stamped world position
  FootprintEmitter.java       — writes footprints from player movement
  FootprintDetector.java      — enemy-side, finds nearby footprints
  TrailFollowState.java       — new enemy state: following a footprint trail
```

---

### `Footprint`

```java
package io.github.superteam.resonance.enemy.tracking;

import com.badlogic.gdx.math.Vector3;

/**
 * One footprint left by the player.
 *
 * intensity: 0..1
 *   - Crouching on carpet  → ~0.15  (barely detectable)
 *   - Walking on concrete  → ~0.55
 *   - Sprinting on metal   → ~1.00
 *
 * age: counts up from 0 each frame — compared against PRINT_LIFETIME_SECONDS
 */
public final class Footprint {

    public final Vector3 position;
    public final float   intensity;
    public float         age;          // seconds since stamped

    public Footprint(Vector3 position, float intensity) {
        this.position  = new Vector3(position);
        this.intensity = intensity;
        this.age       = 0f;
    }

    /** Effective intensity after age decay — drops linearly to 0 at full lifetime. */
    public float effectiveIntensity(float lifetime) {
        return intensity * Math.max(0f, 1f - (age / lifetime));
    }

    public boolean expired(float lifetime) {
        return age >= lifetime;
    }
}
```

---

### `FootprintTrail`

```java
package io.github.superteam.resonance.enemy.tracking;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

/**
 * A rolling ring of Footprint objects left by the player.
 *
 * Footprints are stamped every STAMP_INTERVAL_SECONDS of player movement.
 * The trail is capped at MAX_PRINTS to bound memory.
 * Prints are aged and expired each frame.
 */
public final class FootprintTrail {

    private static final float STAMP_INTERVAL_SECONDS = 0.8f;   // one print per ~0.8s of movement
    private static final float PRINT_LIFETIME_SECONDS = 90f;    // trail goes cold after 90 seconds
    private static final int   MAX_PRINTS             = 120;    // 90s / 0.8s = 112 max; 120 is safe cap

    private final Deque<Footprint> prints = new ArrayDeque<>();
    private float stampTimer = 0f;

    /** Call every frame while the player is moving. */
    public void update(float delta, Vector3 playerPosition, float printIntensity,
                       boolean playerIsMoving) {
        // Age and expire existing prints
        prints.removeIf(p -> {
            p.age += delta;
            return p.expired(PRINT_LIFETIME_SECONDS);
        });

        if (!playerIsMoving) {
            stampTimer = 0f;
            return;
        }

        stampTimer += delta;
        if (stampTimer >= STAMP_INTERVAL_SECONDS) {
            stampTimer = 0f;
            stamp(playerPosition, printIntensity);
        }
    }

    private void stamp(Vector3 position, float intensity) {
        if (intensity < 0.05f) return; // too faint to leave a print at all
        if (prints.size() >= MAX_PRINTS) prints.pollFirst(); // drop oldest
        prints.addLast(new Footprint(position, intensity));
    }

    /**
     * Returns all prints within searchRadius of the given point,
     * sorted newest-first, above the given minimum effective intensity.
     */
    public List<Footprint> findNear(Vector3 searchCenter, float searchRadius,
                                     float minIntensity) {
        List<Footprint> result = new ArrayList<>();
        float r2 = searchRadius * searchRadius;
        for (Footprint p : prints) {
            if (p.position.dst2(searchCenter) <= r2
                    && p.effectiveIntensity(PRINT_LIFETIME_SECONDS) >= minIntensity) {
                result.add(p);
            }
        }
        // Sort newest first (highest age = oldest, so sort ascending age)
        result.sort((a, b) -> Float.compare(a.age, b.age));
        return result;
    }

    public boolean hasAnyPrintNear(Vector3 pos, float radius, float minIntensity) {
        return !findNear(pos, radius, minIntensity).isEmpty();
    }

    public int size() { return prints.size(); }
}
```

---

### `FootprintEmitter`

Sits in the player update path. Computes `printIntensity` based on movement state and surface material:

```java
package io.github.superteam.resonance.enemy.tracking;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.footstep.SurfaceMaterial;
import io.github.superteam.resonance.player.PlayerController;

/**
 * Reads player state each frame and writes to FootprintTrail.
 * Print intensity = movementSpeed × surfaceMultiplier × postureMultiplier
 */
public final class FootprintEmitter {

    // Surface multipliers — how visible a print is on each material
    private static final java.util.EnumMap<SurfaceMaterial, Float> SURFACE_MULT =
        new java.util.EnumMap<>(SurfaceMaterial.class) {{
            put(SurfaceMaterial.CONCRETE, 0.55f);
            put(SurfaceMaterial.WOOD,     0.50f);
            put(SurfaceMaterial.METAL,    0.70f);  // metal holds prints well
            put(SurfaceMaterial.GRAVEL,   0.80f);  // gravel shifts visibly
            put(SurfaceMaterial.WATER,    0.90f);  // wet trail
            put(SurfaceMaterial.CARPET,   0.20f);  // carpet absorbs prints
            put(SurfaceMaterial.TILE,     0.60f);
            put(SurfaceMaterial.DIRT,     0.85f);  // dirt holds deep prints
        }};

    private final FootprintTrail trail;

    public FootprintEmitter(FootprintTrail trail) {
        this.trail = trail;
    }

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

---

### `FootprintDetector` — enemy-side

```java
package io.github.superteam.resonance.enemy.tracking;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.List;

/**
 * Enemy-side footprint reader.
 * Given the enemy's current position, finds the most useful nearby footprint
 * and returns a drift-adjusted direction to follow.
 *
 * "Most useful" = newest print the enemy hasn't already reached,
 * within DETECTION_RADIUS and above DETECTION_THRESHOLD intensity.
 */
public final class FootprintDetector {

    // How far the enemy can "see" a print on the ground
    private static final float DETECTION_RADIUS    = 4.5f;
    // Minimum effective intensity the enemy can detect
    // (increases when the player moves fast — clear trail; decreases in carpet rooms)
    private static final float DETECTION_THRESHOLD = 0.18f;

    // Maximum angular drift applied to the move direction — makes trail-follow feel organic
    // A value of 20° means the enemy may be off by up to 20° from the true print direction
    private static final float MAX_DRIFT_DEGREES   = 22f;

    private final FootprintTrail trail;
    private final java.util.Random rng = new java.util.Random();

    // The last print index the enemy reached — avoids re-detecting the same print repeatedly
    private Vector3 lastReachedPrintPos = null;
    private static final float REACHED_THRESHOLD = 1.2f; // metres — "close enough" to a print

    public FootprintDetector(FootprintTrail trail) {
        this.trail = trail;
    }

    /**
     * Call when the enemy is in trail-follow mode.
     *
     * @return a direction Vector3 (normalised) to move toward, or null if no print found
     */
    public Vector3 findNextDirection(Vector3 enemyPosition) {
        List<Footprint> nearby = trail.findNear(enemyPosition, DETECTION_RADIUS, DETECTION_THRESHOLD);
        if (nearby.isEmpty()) return null;

        // Find the newest print that isn't the one we just reached
        Footprint target = null;
        for (Footprint p : nearby) {
            if (lastReachedPrintPos != null
                    && p.position.dst(lastReachedPrintPos) < REACHED_THRESHOLD) {
                continue; // skip the one we already stood on
            }
            target = p;
            break;
        }
        if (target == null) target = nearby.get(0); // all prints "reached" — pick newest anyway

        // Check if enemy has arrived at this print
        if (enemyPosition.dst(target.position) < REACHED_THRESHOLD) {
            lastReachedPrintPos = new Vector3(target.position);
        }

        // Base direction toward the print
        Vector3 dir = new Vector3(target.position).sub(enemyPosition);
        dir.y = 0f; // keep horizontal
        if (dir.len2() < 0.001f) return null;
        dir.nor();

        // Apply angular drift — imperfect tracking
        // Drift is inversely scaled by intensity: faint prints cause more drift
        float intensityFactor = target.effectiveIntensity(90f); // PRINT_LIFETIME_SECONDS
        float driftDegrees = MAX_DRIFT_DEGREES * (1f - intensityFactor);
        float driftRad = MathUtils.degreesToRadians * driftDegrees
                       * (rng.nextFloat() * 2f - 1f); // random sign

        // Rotate dir around Y axis by driftRad
        float cos = MathUtils.cos(driftRad);
        float sin = MathUtils.sin(driftRad);
        float newX = dir.x * cos - dir.z * sin;
        float newZ = dir.x * sin + dir.z * cos;

        return new Vector3(newX, 0f, newZ).nor();
    }

    /**
     * True if there are any detectable prints near the enemy.
     * Used by InvestigateState to decide whether to transition to TRAIL_FOLLOW.
     */
    public boolean hasDetectablePrints(Vector3 enemyPosition) {
        return trail.hasAnyPrintNear(enemyPosition, DETECTION_RADIUS, DETECTION_THRESHOLD);
    }

    public void reset() {
        lastReachedPrintPos = null;
    }
}
```

---

### `TrailFollowState` — new enemy state

```java
package io.github.superteam.resonance.enemy.states;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.enemy.EnemyStateMachine.StateId;
import io.github.superteam.resonance.enemy.tracking.FootprintDetector;

/**
 * Enemy follows the player's footprint trail.
 * Transitions:
 *   - TRAIL_FOLLOW → CHASE    if player sighted
 *   - TRAIL_FOLLOW → INVESTIGATE if trail goes cold (no print found for > COLD_TIMEOUT)
 *   - TRAIL_FOLLOW → PATROL   if trail cold AND player not seen for a long time
 */
public final class TrailFollowState implements EnemyState {

    private static final float COLD_TIMEOUT_SECONDS = 5f; // trail goes cold after 5s without a print
    private static final float TRAIL_SPEED_MULT      = 0.75f; // slower than chase — careful tracking

    private final FootprintDetector detector;
    private float coldTimer = 0f;

    public TrailFollowState(FootprintDetector detector) {
        this.detector = detector;
    }

    @Override
    public void onEnter(EnemyAIContext ctx) {
        coldTimer = 0f;
        ctx.animator.playAnimation("walk_cautious");
    }

    @Override
    public StateId update(float delta, EnemyAIContext ctx) {
        // Check sight — always takes priority
        if (ctx.perception.canSeePlayer(ctx.position, ctx.forward, ctx.playerPosition,
                                         ctx.physicsWorld)) {
            return StateId.CHASE;
        }

        Vector3 moveDir = detector.findNextDirection(ctx.position);

        if (moveDir == null) {
            coldTimer += delta;
            ctx.animator.playAnimation("look_around");
            if (coldTimer >= COLD_TIMEOUT_SECONDS) {
                return StateId.INVESTIGATE; // trail lost — go investigate last known area
            }
        } else {
            coldTimer = 0f; // found a print — reset cold timer
            ctx.navigator.setDirectDirection(moveDir, TRAIL_SPEED_MULT);
        }

        return StateId.TRAIL_FOLLOW; // stay in this state
    }

    @Override
    public void onExit(EnemyAIContext ctx) {
        detector.reset();
    }
}
```

### Updated state machine transitions

Add `TRAIL_FOLLOW` to `EnemyStateMachine.StateId`:

```java
public enum StateId { IDLE, PATROL, INVESTIGATE, TRAIL_FOLLOW, CHASE, ATTACK, STUNNED }
```

`InvestigateState` now checks for nearby prints before wandering randomly. If prints are found, it transitions to `TRAIL_FOLLOW` instead:

```java
// In InvestigateState.update():
if (detector.hasDetectablePrints(ctx.position)) {
    return StateId.TRAIL_FOLLOW;
}
```

### K-Means integration

The footprint trail system feeds back into `PlayerBehaviorTracker`:

- A player who **always crouches** produces faint prints → `crouchRatio` already tracked
- A player who **sprints a lot** produces bright prints → `movementSpeed` already tracked
- No new tracking needed — the K-Means system already captures the data that determines trail visibility

Additionally, `DifficultyEffect` adjusts `DETECTION_THRESHOLD` based on archetype:

```java
// DifficultyEffect — footprint detection sensitivity per archetype
// Methodical player = enemy detects even faint prints (they're careful but traceable)
// Panicked player   = enemy misses faint prints (their chaos makes tracking hard)
private static final float[] PRINT_DETECTION_THRESHOLD = { 0.18f, 0.12f, 0.20f, 0.25f };
//                                                    PARANOID METHODICAL IMPULSIVE PANICKED
```

---

## 2. Sanity Hallucinations

### Design intent

Below certain sanity thresholds, the player hears sounds that aren't real — fake footsteps, fake door creaks, a distant fake enemy growl. These use the same WAV files as the real enemy to be genuinely misleading. Critically, **they never enter the propagation graph** — the real enemy does not react to hallucinated sounds. The player's own mind is working against them.

At very low sanity, hallucinations escalate: the sounds get closer, more frequent, and at the lowest tier, a brief visual hallucination (the enemy model flickers into view and vanishes).

### Package: `sanity/hallucination`

```
sanity/hallucination/
  HallucinationDirector.java    — decides when and what to hallucinate
  HallucinationEvent.java       — one hallucination instance
  HallucinationType.java        — enum of hallucination categories
  AudioHallucination.java       — plays a fake spatial WAV
  VisualHallucination.java      — flickers a fake enemy model (existing HallucinationEffect)
```

---

### `HallucinationType`

```java
public enum HallucinationType {
    FAKE_FOOTSTEP,          // enemy footstep sound from a wrong direction
    FAKE_DOOR_CREAK,        // door creak from a closed door
    FAKE_ENEMY_BREATH,      // close breathing sound with no enemy nearby
    FAKE_ENEMY_GROWL,       // distant enemy alert sound
    FAKE_ITEM_FALL,         // something falls over (but nothing moved)
    VISUAL_FLICKER          // model briefly appears
}
```

---

### `HallucinationDirector`

```java
package io.github.superteam.resonance.sanity.hallucination;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.sanity.SanitySystem;

/**
 * Decides when and what to hallucinate based on the player's current sanity.
 *
 * Three sanity tiers:
 *   MILD   (50–25): occasional fake footsteps, rare door creak
 *   HEAVY  (25–10): frequent fake sounds, fake breathing, occasional fake growl
 *   SEVERE (10–0):  everything above + visual flicker, fake sounds very close
 *
 * Hallucinations NEVER enter the acoustic propagation graph.
 * The enemy is completely unaware they are happening.
 */
public final class HallucinationDirector {

    private static final float MIN_INTERVAL_MILD   = 25f;   // seconds between hallucinations
    private static final float MIN_INTERVAL_HEAVY  = 10f;
    private static final float MIN_INTERVAL_SEVERE = 4f;

    private final GameAudioSystem audio;
    private float nextHallucinationTimer;

    public HallucinationDirector(GameAudioSystem audio) {
        this.audio = audio;
        nextHallucinationTimer = MIN_INTERVAL_MILD;
    }

    public void update(float delta, SanitySystem sanity, Vector3 playerPosition,
                       Vector3 playerForward) {
        float s = sanity.getSanity();

        // No hallucinations above 50 sanity
        if (s >= 50f) {
            nextHallucinationTimer = MIN_INTERVAL_MILD;
            return;
        }

        nextHallucinationTimer -= delta;
        if (nextHallucinationTimer > 0f) return;

        // Pick tier and interval
        float interval;
        HallucinationTier tier;
        if (s >= 25f) {
            interval = MathUtils.random(MIN_INTERVAL_MILD * 0.7f, MIN_INTERVAL_MILD * 1.4f);
            tier = HallucinationTier.MILD;
        } else if (s >= 10f) {
            interval = MathUtils.random(MIN_INTERVAL_HEAVY * 0.7f, MIN_INTERVAL_HEAVY * 1.4f);
            tier = HallucinationTier.HEAVY;
        } else {
            interval = MathUtils.random(MIN_INTERVAL_SEVERE * 0.7f, MIN_INTERVAL_SEVERE * 1.4f);
            tier = HallucinationTier.SEVERE;
        }

        nextHallucinationTimer = interval;
        trigger(tier, playerPosition, playerForward);
    }

    private void trigger(HallucinationTier tier, Vector3 playerPos, Vector3 playerForward) {
        HallucinationType type = pickType(tier);
        Vector3 fakeSourcePos  = pickFakePosition(tier, playerPos, playerForward);

        switch (type) {
            case FAKE_FOOTSTEP -> playFakeFootstep(fakeSourcePos, playerPos);
            case FAKE_DOOR_CREAK -> playFakeSound(
                "audio/sfx/door/creak_medium.wav", fakeSourcePos, playerPos, 0.55f);
            case FAKE_ENEMY_BREATH -> playFakeSound(
                "audio/sfx/enemy/breathing_close.wav", fakeSourcePos, playerPos, 0.7f);
            case FAKE_ENEMY_GROWL -> playFakeSound(
                "audio/sfx/enemy/growl_distant.wav", fakeSourcePos, playerPos, 0.5f);
            case FAKE_ITEM_FALL -> playFakeSound(
                "audio/sfx/items/metal_clang.wav", fakeSourcePos, playerPos, 0.65f);
            case VISUAL_FLICKER -> triggerVisualFlicker(fakeSourcePos);
        }
    }

    private HallucinationType pickType(HallucinationTier tier) {
        return switch (tier) {
            case MILD   -> MathUtils.random() < 0.75f
                ? HallucinationType.FAKE_FOOTSTEP
                : HallucinationType.FAKE_DOOR_CREAK;
            case HEAVY  -> pickFrom(HallucinationType.FAKE_FOOTSTEP,
                HallucinationType.FAKE_DOOR_CREAK, HallucinationType.FAKE_ENEMY_BREATH,
                HallucinationType.FAKE_ENEMY_GROWL, HallucinationType.FAKE_ITEM_FALL);
            case SEVERE -> pickFrom(HallucinationType.values()); // anything including visual
        };
    }

    /**
     * Fake position strategy:
     *  MILD   — behind the player or to the side (heard but not visible)
     *  HEAVY  — anywhere around the player at 8–15m
     *  SEVERE — very close (2–5m), in the player's peripheral vision
     */
    private Vector3 pickFakePosition(HallucinationTier tier, Vector3 origin, Vector3 forward) {
        float minDist, maxDist;
        float angleRange;

        switch (tier) {
            case MILD   -> { minDist = 10f; maxDist = 20f; angleRange = MathUtils.PI; } // behind/side
            case HEAVY  -> { minDist = 6f;  maxDist = 15f; angleRange = MathUtils.PI2; } // anywhere
            default     -> { minDist = 2f;  maxDist = 6f;  angleRange = MathUtils.PI2; } // SEVERE close
        }

        float dist  = MathUtils.random(minDist, maxDist);
        float angle = MathUtils.random(-angleRange / 2f, angleRange / 2f);

        // For MILD, bias toward behind the player
        if (tier == HallucinationTier.MILD) angle += MathUtils.PI;

        float cos = MathUtils.cos(angle), sin = MathUtils.sin(angle);
        float fwdX = forward.x, fwdZ = forward.z;
        float rightX = fwdZ, rightZ = -fwdX; // perpendicular in XZ

        return new Vector3(
            origin.x + (fwdX * cos + rightX * sin) * dist,
            origin.y,
            origin.z + (fwdZ * cos + rightZ * sin) * dist
        );
    }

    private void playFakeFootstep(Vector3 fakePos, Vector3 listenerPos) {
        // Pick a random enemy footstep variant — same files as real enemy footsteps
        String[] variants = {
            "audio/sfx/enemy/footstep_01.wav",
            "audio/sfx/enemy/footstep_02.wav",
            "audio/sfx/enemy/footstep_03.wav"
        };
        String wav = variants[MathUtils.random(variants.length - 1)];
        playFakeSound(wav, fakePos, listenerPos, 0.6f);
    }

    /**
     * Play spatial WAV without injecting into the propagation graph.
     * The enemy is deaf to this.
     */
    private void playFakeSound(String wavPath, Vector3 fakePos, Vector3 listenerPos, float vol) {
        // Use GameAudioSystem.playSfx (no graphEvent argument) so propagation is skipped
        audio.playSfx(wavPath, fakePos, vol);
    }

    private void triggerVisualFlicker(Vector3 fakePos) {
        // Delegate to VisualHallucination — brief enemy model flash at fakePos
        // Implementation is in existing HallucinationEffect in sanity/effects/
        // This call tells it where to spawn the flicker
        visualHallucinationEffect.triggerAt(fakePos);
    }

    private HallucinationType pickFrom(HallucinationType... options) {
        return options[MathUtils.random(options.length - 1)];
    }

    private enum HallucinationTier { MILD, HEAVY, SEVERE }
}
```

### What prevents the player from always knowing it's fake

- Same WAV files as the real enemy — if you haven't heard the real enemy yet, you can't distinguish
- Fake footsteps are positioned in physically plausible locations (rooms the enemy could be in)
- At HEAVY tier, the interval is short enough that real and fake sounds blur together
- At SEVERE tier, fake sounds originate from 2–5m away — the player instinctively turns to look

### The one tell

If the player is calm and methodical enough to notice: **fake sounds never cause the propagation graph to react**. If they have the UI Builder or any acoustic visualiser open, hallucinationgenerated sounds produce no pulse rings. But in normal horror gameplay this is invisible.

### K-Means integration

`JumpScareTimingEffect` already controls scare type weights. `HallucinationDirector` is separate from `JumpScareDirector` — it runs continuously below sanity 50 rather than waiting for tension buildup. However, the archetype modifies hallucination behaviour via `DifficultyEffect`:

```java
// DifficultyEffect — hallucination intensity modifier
// Paranoid: hallucinations start earlier (sanity 60 instead of 50)
// Methodical: standard threshold (50)
// Impulsive/Panicked: later threshold (40) — they're too loud to hear subtle fakes
private static final float[] HALLUCINATION_THRESHOLD = { 60f, 50f, 40f, 40f };
// PARANOID METHODICAL IMPULSIVE PANICKED
```

---

## 3. Breath-Hold Mechanic

### Design intent

The player holds a key (default: Left Alt) to suppress their breathing noise. While holding breath, footstep sounds are quieter, mic propagation is muffled, and the enemy's effective hearing radius for the player is reduced. But breath is finite — hold too long and the forced exhale is loud enough to be heard.

### States

```
NORMAL
  └─ (hold key pressed) → HOLDING
HOLDING
  └─ (stamina hits 0) → FORCED_EXHALE   ← loud, involuntary
  └─ (key released)   → RECOVERING
FORCED_EXHALE
  └─ (after 0.8s animation) → RECOVERING
RECOVERING
  └─ (stamina full) → NORMAL
  └─ (hold key pressed AND stamina > 20%) → HOLDING
```

---

### Package: `player`

```
player/
  BreathSystem.java             — manages stamina, state, audio
  BreathState.java              — enum of breath states
  BreathHoldEffect.java         — applies noise suppression while holding
```

---

### `BreathSystem`

```java
package io.github.superteam.resonance.player;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.settings.KeybindRegistry;

/**
 * Manages the player's breath-hold mechanic.
 *
 * While HOLDING:
 *   - Footstep volume → 30% of normal
 *   - Mic propagation noise floor raised (quieter effective mic input)
 *   - Enemy hearing range multiplier for player: × 0.45
 *   - SanitySystem drain slightly increased (body panicking from oxygen deprivation)
 *
 * On FORCED_EXHALE:
 *   - Plays a loud exhale WAV at full volume
 *   - Injects FOOTSTEP_RUN intensity into propagation graph (loud noise event)
 *   - Sanity takes a -5 hit (shock of involuntary noise)
 */
public final class BreathSystem {

    public enum BreathState { NORMAL, HOLDING, FORCED_EXHALE, RECOVERING }

    // Stamina constants
    private static final float MAX_STAMINA         = 100f;
    private static final float DRAIN_PER_SECOND    = 22f;   // ~4.5s at full
    private static final float RECOVER_PER_SECOND  = 11f;   // ~9s to recover from empty
    private static final float MIN_HOLD_STAMINA    = 20f;   // cannot start hold below 20%
    private static final float EXHALE_DURATION     = 0.8f;  // forced exhale animation length

    // Effect values while holding
    public static final float FOOTSTEP_VOLUME_MULT = 0.30f; // 30% of normal footstep volume
    public static final float MIC_SUPPRESSION      = 0.15f; // mic RMS multiplied by this
    public static final float ENEMY_HEARING_MULT   = 0.45f; // enemy's effective range × 0.45

    private float      stamina    = MAX_STAMINA;
    private BreathState state     = BreathState.NORMAL;
    private float       exhaleTimer;

    private final GameAudioSystem audio;
    private final KeybindRegistry  keybinds;

    public BreathSystem(GameAudioSystem audio, KeybindRegistry keybinds) {
        this.audio    = audio;
        this.keybinds = keybinds;
    }

    public void update(float delta, Vector3 playerPosition, Vector3 listenerPos,
                       io.github.superteam.resonance.sanity.SanitySystem sanity,
                       io.github.superteam.resonance.sound.SoundPropagationOrchestrator prop,
                       float elapsedSeconds) {

        boolean holdKeyDown = keybinds.isPressed("HOLD_BREATH");

        switch (state) {
            case NORMAL -> {
                if (holdKeyDown && stamina >= MIN_HOLD_STAMINA) {
                    state = BreathState.HOLDING;
                    audio.playSfx("audio/sfx/player/breath_in.wav", playerPosition, 0.3f);
                }
            }

            case HOLDING -> {
                stamina -= DRAIN_PER_SECOND * delta;
                sanity.applyDelta(-0.5f * delta); // mild sanity drain from oxygen stress

                if (!holdKeyDown) {
                    // Voluntary release — quiet exhale
                    state = BreathState.RECOVERING;
                    audio.playSfx("audio/sfx/player/breath_out_quiet.wav", playerPosition, 0.25f);
                } else if (stamina <= 0f) {
                    // Ran out — forced exhale
                    stamina = 0f;
                    state   = BreathState.FORCED_EXHALE;
                    exhaleTimer = EXHALE_DURATION;
                    triggerForcedExhale(playerPosition, listenerPos, prop, sanity, elapsedSeconds);
                }
            }

            case FORCED_EXHALE -> {
                exhaleTimer -= delta;
                if (exhaleTimer <= 0f) {
                    state = BreathState.RECOVERING;
                }
            }

            case RECOVERING -> {
                stamina = Math.min(MAX_STAMINA, stamina + RECOVER_PER_SECOND * delta);
                if (holdKeyDown && stamina >= MIN_HOLD_STAMINA) {
                    state = BreathState.HOLDING;
                    audio.playSfx("audio/sfx/player/breath_in.wav", playerPosition, 0.3f);
                }
                if (stamina >= MAX_STAMINA) {
                    state = BreathState.NORMAL;
                }
            }
        }
    }

    private void triggerForcedExhale(Vector3 playerPos, Vector3 listenerPos,
                                      io.github.superteam.resonance.sound.SoundPropagationOrchestrator prop,
                                      io.github.superteam.resonance.sanity.SanitySystem sanity,
                                      float elapsed) {
        // Loud involuntary exhale sound
        audio.playSfx("audio/sfx/player/breath_out_forced.wav", playerPos, 1.0f);

        // Inject as PLAYER_ACTION noise into propagation graph — enemy hears this
        String nodeId = prop.findNearestNode(playerPos);
        io.github.superteam.resonance.sound.SoundEventData data =
            new io.github.superteam.resonance.sound.SoundEventData(
                io.github.superteam.resonance.sound.SoundEvent.ITEM_IMPACT, // reuse loud event
                nodeId, playerPos, 0.75f, elapsed
            );
        prop.emitSoundEvent(data, elapsed);

        // Sanity hit — panic from involuntary noise
        sanity.applyDelta(-5f);
    }

    // -- Public API for other systems --

    public boolean isHolding()     { return state == BreathState.HOLDING; }
    public boolean isForcedExhale(){ return state == BreathState.FORCED_EXHALE; }
    public float   staminaNorm()   { return stamina / MAX_STAMINA; }
    public BreathState getState()  { return state; }

    /** Footstep volume should be multiplied by this each step. */
    public float footstepVolumeMult() {
        return isHolding() ? FOOTSTEP_VOLUME_MULT : 1.0f;
    }

    /** Mic RMS should be multiplied by this before propagation threshold check. */
    public float micSuppressionMult() {
        return isHolding() ? MIC_SUPPRESSION : 1.0f;
    }

    /** Enemy perception uses this to reduce effective hearing range for the player. */
    public float enemyHearingMult() {
        return isHolding() ? ENEMY_HEARING_MULT : 1.0f;
    }
}
```

---

### Breath stamina HUD

A small arc indicator near the crosshair — only visible while not at full stamina. Disappears when full, so it doesn't clutter the screen during normal play:

```java
public final class BreathHudRenderer {

    private static final float ARC_RADIUS   = 18f;   // pixels from crosshair center
    private static final int   ARC_SEGMENTS = 24;

    public void render(ShapeRenderer shapes, BreathSystem breath) {
        if (breath.staminaNorm() >= 1.0f && !breath.isHolding()) return; // fully recovered — hide

        float cx = Gdx.graphics.getWidth()  / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;

        float filled = breath.staminaNorm();

        // Background arc (dim white)
        shapes.setColor(1f, 1f, 1f, 0.15f);
        drawArc(shapes, cx, cy, ARC_RADIUS, 0f, 1f);

        // Filled arc (colour shifts red as stamina drops)
        float r = MathUtils.lerp(1.0f, 0.1f, filled);
        float g = MathUtils.lerp(0.2f, 0.9f, filled);
        float b = MathUtils.lerp(0.2f, 1.0f, filled);
        float alpha = breath.isHolding() ? 1.0f : 0.6f;
        shapes.setColor(r, g, b, alpha);
        drawArc(shapes, cx, cy, ARC_RADIUS, 0f, filled);
    }

    private void drawArc(ShapeRenderer s, float cx, float cy, float r,
                          float startFrac, float endFrac) {
        float startAngle = MathUtils.PI / 2f;   // top of circle
        float sweep = MathUtils.PI2 * (endFrac - startFrac);
        float step  = sweep / ARC_SEGMENTS;
        for (int i = 0; i < ARC_SEGMENTS; i++) {
            float a0 = startAngle + step * i;
            float a1 = a0 + step;
            s.line(cx + MathUtils.cos(a0) * r, cy + MathUtils.sin(a0) * r,
                   cx + MathUtils.cos(a1) * r, cy + MathUtils.sin(a1) * r);
        }
    }
}
```

---

### Keybind setup

Add to `SettingsData.defaults()`:

```java
d.keybinds.put("HOLD_BREATH", Input.Keys.ALT_LEFT);
```

---

### Integrations into existing systems

#### `FootstepSystem` — multiply volume by breath

```java
// FootstepSystem.update() — when computing finalVolume:
float finalVolume = volume * breathSystem.footstepVolumeMult();
```

#### Mic propagation — suppress when holding breath

```java
// In GltfMapTestScene mic update path:
float rms = realtimeMicSystem.getNormalizedLevel();
rms *= breathSystem.micSuppressionMult(); // near-zero when holding breath
if (rms > VOICE_DETECTION_THRESHOLD) {
    // ... propagate MIC_VOICE ...
}
```

#### `EnemyPerception` — reduced effective hearing

When the enemy looks for the player's sounds, all player-sourced intensities are multiplied by `breathSystem.enemyHearingMult()`. This is applied at the propagation graph output, not at the source — so the sound still travels through the graph, but the enemy's ability to detect it is reduced:

```java
// EnemyPerception.onSoundHeard():
public void onSoundHeard(SoundEventData data) {
    if (data.soundEvent().hearingCategory() == HearingCategory.AMBIENCE) return;

    float effectiveIntensity = data.baseIntensity() * breathSystem.enemyHearingMult();
    if (effectiveIntensity < MIN_AUDIBLE_INTENSITY) return; // ← filtered by breath suppression

    lastHeardPosition  = data.worldPosition();
    lastHeardIntensity = effectiveIntensity;
    hasUnhandledSound  = true;
}
```

#### `PlayerBehaviorTracker` — breath affects noise metric

```java
// PlayerBehaviorTracker.update():
float rmsContribution = currentNoiseEvent * breathSystem.micSuppressionMult();
noiseAccum = Math.max(noiseAccum, rmsContribution);
```

A player who holds breath constantly will have lower `noiseLevel` samples → K-Means will push them toward METHODICAL or PARANOID.

---

## 4. How the Three Interact

```
Player hides in a locker, holds breath (Alt), crouches

  BreathSystem → HOLDING
    │ footstepVolumeMult = 0.30
    │ micSuppressionMult = 0.15
    │ enemyHearingMult   = 0.45
    │
    └─ EnemyPerception receives no player noise above threshold
         └─ Enemy in TRAIL_FOLLOW — no new sounds — coldTimer ticks up
              └─ After 5s cold → INVESTIGATE → eventually PATROL

  Sanity = 22 (below 25) → HallucinationDirector fires
    │ type: FAKE_FOOTSTEP
    │ position: 6m to the left of the player
    │ → plays enemy/footstep_02.wav spatially
    │ → does NOT inject into propagation graph
    │ → player hears "footsteps" from the left
    │
    └─ Player panics, releases Alt key
         └─ BreathSystem → RECOVERING
              └─ Footstep volume returns to 100%
              └─ Enemy hearing mult returns to 1.0

  If player had held too long first:
    └─ Stamina → 0 → FORCED_EXHALE
         └─ Loud exhale WAV plays at full volume
         └─ ITEM_IMPACT injected at intensity 0.75 → propagation graph
              └─ EnemyPerception.onSoundHeard() → intensity 0.75 > threshold 0.18
                   └─ EnemyStateMachine → INVESTIGATE (was TRAIL_FOLLOW)
                        └─ Moves directly to player's position
```

---

## 5. Package Layout Additions

```
enemy/tracking/
  FootprintTrail.java
  Footprint.java
  FootprintEmitter.java
  FootprintDetector.java

enemy/states/
  TrailFollowState.java           ← new state

sanity/hallucination/
  HallucinationDirector.java
  HallucinationEvent.java
  HallucinationType.java
  AudioHallucination.java

player/
  BreathSystem.java
  BreathState.java                ← enum (can be inner class)
  BreathHudRenderer.java
```

---

## 6. Integration Checklist

**Footprint system**
- [ ] `FootprintEmitter.update()` called each frame in the active screen after `PlayerController.update()`
- [ ] `FootprintTrail` reference passed into `EnemyController` at construction
- [ ] `FootprintDetector` created inside `EnemyController` using shared `FootprintTrail`
- [ ] `TRAIL_FOLLOW` added to `EnemyStateMachine.StateId`
- [ ] `TrailFollowState` registered in state machine
- [ ] `InvestigateState` checks `detector.hasDetectablePrints()` and transitions to `TRAIL_FOLLOW`
- [ ] `DifficultyEffect` sets `PRINT_DETECTION_THRESHOLD` via `FootprintDetector.setDetectionThreshold()`
- [ ] `SettingsData` does not persist footprint data (session-only, resets with `FootprintTrail`)

**Hallucination system**
- [ ] `HallucinationDirector.update()` called each frame after `SanitySystem.update()`
- [ ] `HallucinationDirector` uses `GameAudioSystem.playSfx()` (no graph event) — NOT `playSpatialSfx()`
- [ ] `DifficultyEffect.apply()` sets `hallucinationThreshold` on `HallucinationDirector`
- [ ] `VisualHallucination.triggerAt()` wired to existing `HallucinationEffect` in `sanity/effects/`
- [ ] Debug console: `hallucinate <type>` command to force a hallucination for testing

**Breath system**
- [ ] `BreathSystem` owned by the active screen, updated each frame
- [ ] `FootstepSystem` multiplies volume by `breathSystem.footstepVolumeMult()`
- [ ] Mic propagation path multiplies RMS by `breathSystem.micSuppressionMult()`
- [ ] `EnemyPerception.onSoundHeard()` applies `breathSystem.enemyHearingMult()` to incoming intensity
- [ ] `BreathHudRenderer.render()` called in HUD pass (after game render, before UI)
- [ ] `KeybindRegistry` has `"HOLD_BREATH"` mapped to `ALT_LEFT` by default
- [ ] `PlayerBehaviorTracker` applies `micSuppressionMult()` to noise accumulator
- [ ] `BreathSystem` reference in `EventContext` (for event actions that might query it)

---

## 7. Acceptance Criteria

| Test | Pass condition |
|------|---------------|
| **Footprint — stamp** | Player walks 5m → `FootprintTrail.size()` grows |
| **Footprint — expiry** | Wait 90s without moving → trail clears to 0 |
| **Footprint — intensity crouch** | Crouch-walk on carpet → prints below 0.15 intensity (below default threshold) |
| **Footprint — intensity sprint** | Sprint on metal → prints above 0.70 intensity |
| **Footprint — imperfect** | Enemy follows trail → visually wobbles, doesn't follow exact path |
| **Footprint — drift scales** | Faint prints cause more drift than bright prints |
| **Footprint — cold trail** | No prints for 5s in TRAIL_FOLLOW → enemy transitions to INVESTIGATE |
| **Footprint — state flow** | Enemy INVESTIGATE → finds prints nearby → transitions to TRAIL_FOLLOW |
| **Footprint — detection radius** | Print 6m away (> 4.5m radius) → not detected |
| **Hallucination — threshold** | Sanity = 55 → no hallucinations |
| **Hallucination — mild** | Sanity = 40 → only FAKE_FOOTSTEP or FAKE_DOOR_CREAK |
| **Hallucination — severe** | Sanity = 5 → visual flicker possible, sounds very close |
| **Hallucination — no graph** | Hallucination plays → enemy does NOT investigate the direction |
| **Hallucination — position** | MILD hallucination always originates from behind/sides, not in front |
| **Hallucination — archetype** | PARANOID archetype → hallucinations start at sanity 60 not 50 |
| **Breath — hold** | Hold Alt → `isHolding()` true, stamina decreasing |
| **Breath — suppress footstep** | Hold Alt while walking → footstep volume ≈ 30% normal |
| **Breath — suppress mic** | Hold Alt → `micSuppressionMult()` = 0.15 |
| **Breath — enemy deaf** | Hold Alt, walk past enemy → enemy does not investigate if it would have without hold |
| **Breath — forced exhale** | Hold Alt until stamina = 0 → loud exhale plays, propagation graph event fires |
| **Breath — forced exhale enemy** | Forced exhale → enemy in range transitions to INVESTIGATE |
| **Breath — recovery** | Release Alt → stamina recovers at ~11pts/sec |
| **Breath — HUD** | Stamina < 100% → arc appears near crosshair |
| **Breath — HUD color** | Arc shifts from blue→red as stamina drops |
| **Breath — HUD hidden** | Stamina = 100% and not holding → arc invisible |
| **Interaction — all three** | Hide while holding breath at sanity 20 → enemy loses trail, hallucinations fire, HUD shows arc |

# Resonance — Player Behaviour Classification System (K-Means)

**Scope:** `core` module  
**Resets:** Every new session (not persisted to disk)  
**Archetypes:** Paranoid / Methodical / Impulsive / Panicked  
**Drives:** Enemy AI adaptation, jump scare timing, difficulty tuning, atmosphere shift

---

## Table of Contents

1. [Overview and Design Philosophy](#1-overview-and-design-philosophy)
2. [What Gets Measured — Feature Dimensions](#2-what-gets-measured--feature-dimensions)
3. [The Four Archetypes](#3-the-four-archetypes)
4. [K-Means Algorithm](#4-k-means-algorithm)
5. [Significant Change Detection](#5-significant-change-detection)
6. [What Each Archetype Drives](#6-what-each-archetype-drives)
7. [Package and Class Design](#7-package-and-class-design)
8. [Integration Points](#8-integration-points)
9. [Debug Console Commands](#9-debug-console-commands)
10. [Files to Create](#10-files-to-create)
11. [Acceptance Criteria](#11-acceptance-criteria)

---

## 1. Overview and Design Philosophy

### How it fits into the game

The player never sees this system. There is no archetype label shown on screen, no "you are PARANOID" notification. The game silently watches how the player behaves, classifies them, and subtly reshapes itself to fit that behaviour. A panicking player who sprints everywhere finds the enemy faster and the atmosphere more chaotic. A methodical croucher finds the enemy more calculated and the atmosphere more suffocating.

### Why K-means specifically

K-means is appropriate here because:
- Player behaviour is a continuous multi-dimensional space, not a yes/no category
- We want a player who is 60% Methodical / 40% Paranoid to get a blended response, not a hard switch
- K-means gives us **distance to each centroid** — not just the winning archetype — which enables smooth blending between archetype effects
- It runs in microseconds on a 4-centroid, 10-dimension problem

### Session lifecycle

```
Game starts
  └─ BehaviorTracker begins collecting samples (every 5 seconds)
  └─ K-Means initialised with predefined archetype centroids
  └─ Player starts as METHODICAL by default (neutral starting point)

Every 5 seconds:
  └─ BehaviorSample captured
  └─ BehaviorChangeDetector checks if behaviour shifted significantly
  └─ If shift detected → KMeansClassifier re-runs → new archetype assigned
  └─ ArchetypeEffectApplier applies blended effects

Game ends / new game started:
  └─ All samples cleared — fresh start
```

---

## 2. What Gets Measured — Feature Dimensions

Ten features are tracked. Each is normalized to `[0.0, 1.0]` before feeding into K-means so no single dimension dominates due to scale.

| # | Feature | Raw metric | Normalized meaning |
|---|---------|-----------|-------------------|
| 1 | `movementSpeed` | Average velocity (m/s) over window | 0 = stationary, 1 = sprinting |
| 2 | `noiseLevel` | RMS of all sound events emitted by player (mic + footsteps + items) | 0 = silent, 1 = very loud |
| 3 | `crouchRatio` | Fraction of movement time spent crouching | 0 = never crouches, 1 = always crouching |
| 4 | `stationaryRatio` | Fraction of total time spent not moving | 0 = always moving, 1 = always hiding |
| 5 | `lookAroundRate` | Camera angular velocity variance (how erratically they look) | 0 = steady, 1 = spinning/paranoid |
| 6 | `flashlightToggleRate` | Number of flashlight on/off cycles per minute | 0 = never toggles, 1 = obsessively toggles |
| 7 | `doorOpenSpeed` | Average `angularVelocity` of doors the player opens | 0 = very slow/careful, 1 = slams |
| 8 | `interactionRate` | Interactions per minute (items, doors, notes) | 0 = ignores everything, 1 = touches everything |
| 9 | `sanityLossRate` | Average sanity drain per minute | 0 = stays calm, 1 = constantly exposed |
| 10 | `hideResponseTime` | How quickly player becomes stationary after a scare event | 0 = freezes instantly, 1 = keeps running |

### `BehaviorSample`

One snapshot captured every 5 seconds of play:

```java
package io.github.superteam.resonance.behavior;

/**
 * One 5-second window snapshot of raw player behaviour metrics.
 * All fields are raw (un-normalized) values from the measurement window.
 */
public record BehaviorSample(
    float averageVelocity,           // m/s
    float averageNoiseRms,           // 0..1 from propagation events
    float crouchFraction,            // 0..1 of time spent crouching
    float stationaryFraction,        // 0..1 of time spent stationary
    float cameraAngularVariance,     // deg/s variance
    float flashlightTogglesPerMin,   // raw count scaled to per minute
    float averageDoorOpenSpeed,      // deg/s from DoorCreakSystem
    float interactionsPerMin,        // raw count scaled to per minute
    float sanityLostThisWindow,      // raw sanity points lost
    float hideResponseSeconds        // seconds to stationary after last scare event; -1 if no scare
) {
    public static final BehaviorSample NEUTRAL = new BehaviorSample(
        1.5f, 0.2f, 0.2f, 0.2f, 30f, 0.5f, 40f, 2f, 3f, 2f
    );
}
```

### `BehaviorFeatureVector`

The normalized form of a `BehaviorSample`, ready for K-means distance calculation:

```java
package io.github.superteam.resonance.behavior;

/**
 * Normalized [0..1] feature vector computed from a BehaviorSample.
 * All normalization bounds are tunable constants in FeatureNormalizer.
 */
public record BehaviorFeatureVector(float[] features) {   // length = DIMENSION_COUNT

    public static final int DIMENSION_COUNT = 10;

    // Dimension index constants for readability
    public static final int DIM_MOVE_SPEED       = 0;
    public static final int DIM_NOISE            = 1;
    public static final int DIM_CROUCH           = 2;
    public static final int DIM_STATIONARY       = 3;
    public static final int DIM_LOOK_AROUND      = 4;
    public static final int DIM_FLASHLIGHT       = 5;
    public static final int DIM_DOOR_SPEED       = 6;
    public static final int DIM_INTERACTION      = 7;
    public static final int DIM_SANITY_LOSS      = 8;
    public static final int DIM_HIDE_RESPONSE    = 9;

    /** Euclidean distance between two feature vectors. */
    public float distanceTo(BehaviorFeatureVector other) {
        float sum = 0f;
        for (int i = 0; i < DIMENSION_COUNT; i++) {
            float diff = features[i] - other.features[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }
}
```

### `FeatureNormalizer`

```java
package io.github.superteam.resonance.behavior;

/**
 * Converts raw BehaviorSample values into normalized [0..1] feature vectors.
 * Bounds are tuned to typical human play ranges — adjust based on playtesting.
 */
public final class FeatureNormalizer {

    // Min/Max bounds for each dimension — values outside are clamped
    private static final float SPEED_MAX        = 5.5f;    // sprint speed
    private static final float NOISE_MAX        = 1.0f;    // already 0..1
    private static final float CROUCH_MAX       = 1.0f;    // already 0..1
    private static final float STATIONARY_MAX   = 1.0f;
    private static final float LOOK_VAR_MAX     = 150f;    // deg/s variance
    private static final float FLASHLIGHT_MAX   = 6f;      // 6 toggles/min = very frequent
    private static final float DOOR_SPEED_MAX   = 120f;    // deg/s slam speed
    private static final float INTERACT_MAX     = 8f;      // 8 interactions/min
    private static final float SANITY_LOSS_MAX  = 15f;     // 15 pts/window = heavy drain
    private static final float HIDE_RESP_MAX    = 5f;      // 5s = slow to hide

    public BehaviorFeatureVector normalize(BehaviorSample s) {
        float hideResp = s.hideResponseSeconds() < 0
            ? 0.5f   // no scare this window — neutral
            : clamp01(s.hideResponseSeconds() / HIDE_RESP_MAX);

        return new BehaviorFeatureVector(new float[] {
            clamp01(s.averageVelocity()          / SPEED_MAX),
            clamp01(s.averageNoiseRms()          / NOISE_MAX),
            clamp01(s.crouchFraction()           / CROUCH_MAX),
            clamp01(s.stationaryFraction()       / STATIONARY_MAX),
            clamp01(s.cameraAngularVariance()    / LOOK_VAR_MAX),
            clamp01(s.flashlightTogglesPerMin()  / FLASHLIGHT_MAX),
            clamp01(s.averageDoorOpenSpeed()     / DOOR_SPEED_MAX),
            clamp01(s.interactionsPerMin()       / INTERACT_MAX),
            clamp01(s.sanityLostThisWindow()     / SANITY_LOSS_MAX),
            hideResp
        });
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
```

---

## 3. The Four Archetypes

Each archetype is defined by its **centroid** — the ideal feature vector that represents that play style. These are the starting centroids for K-means. They update from real player data as samples accumulate.

| Dimension | Paranoid | Methodical | Impulsive | Panicked |
|-----------|---------|-----------|----------|---------|
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

**Reading the table:**
- **Paranoid** — medium speed, medium noise, spins camera constantly, obsessively toggles flashlight, hides fast when scared, high sanity loss (lets fear build up)
- **Methodical** — slow, quiet, crouches a lot, steady flashlight, interacts with everything, sanity stays lower (cautious exposure)
- **Impulsive** — fast, loud, rarely crouches, slams doors, slow to hide after scares
- **Panicked** — sprints, loud, never crouches, slams everything, hides instantly when scared, constant sanity drain

```java
package io.github.superteam.resonance.behavior;

public enum PlayerArchetype {
    PARANOID,
    METHODICAL,
    IMPULSIVE,
    PANICKED;

    public static final PlayerArchetype DEFAULT = METHODICAL;
}
```

```java
package io.github.superteam.resonance.behavior;

/**
 * Predefined centroid positions for each archetype.
 * These are the K-means starting centroids; they drift toward actual player data
 * as samples accumulate during the session.
 */
public final class ArchetypeCentroid {

    // [PARANOID, METHODICAL, IMPULSIVE, PANICKED]
    private static final float[][] INITIAL = {
        // move   noise  crouch  stat  look  flash  door  inter sanity hide
        { 0.35f, 0.30f, 0.35f, 0.45f, 0.80f, 0.70f, 0.30f, 0.55f, 0.60f, 0.15f }, // PARANOID
        { 0.20f, 0.15f, 0.70f, 0.40f, 0.30f, 0.20f, 0.20f, 0.75f, 0.30f, 0.40f }, // METHODICAL
        { 0.70f, 0.65f, 0.10f, 0.15f, 0.40f, 0.30f, 0.75f, 0.40f, 0.50f, 0.70f }, // IMPULSIVE
        { 0.90f, 0.80f, 0.05f, 0.10f, 0.75f, 0.55f, 0.85f, 0.20f, 0.85f, 0.05f }, // PANICKED
    };

    private final float[][] centroids;

    public ArchetypeCentroid() {
        // Deep copy so instances don't share mutable state
        centroids = new float[4][BehaviorFeatureVector.DIMENSION_COUNT];
        for (int k = 0; k < 4; k++) {
            System.arraycopy(INITIAL[k], 0, centroids[k], 0, INITIAL[k].length);
        }
    }

    public BehaviorFeatureVector centroidOf(PlayerArchetype archetype) {
        return new BehaviorFeatureVector(centroids[archetype.ordinal()].clone());
    }

    /** Update centroid k with the mean of its assigned samples. */
    public void update(PlayerArchetype archetype, List<BehaviorFeatureVector> assignedSamples) {
        if (assignedSamples.isEmpty()) return;
        float[] mean = new float[BehaviorFeatureVector.DIMENSION_COUNT];
        for (BehaviorFeatureVector v : assignedSamples) {
            for (int d = 0; d < mean.length; d++) mean[d] += v.features()[d];
        }
        for (int d = 0; d < mean.length; d++) mean[d] /= assignedSamples.size();
        centroids[archetype.ordinal()] = mean;
    }

    public float[][] raw() { return centroids; }
}
```

---

## 4. K-Means Algorithm

### `KMeansClassifier`

```java
package io.github.superteam.resonance.behavior;

import java.util.*;

/**
 * K-Means classifier with K=4 (one cluster per archetype).
 *
 * Not run every frame — only when BehaviorChangeDetector signals a
 * significant shift. Runs on the GL thread synchronously because it
 * takes < 1ms for K=4 and N<=60 samples.
 */
public final class KMeansClassifier {

    private static final int K           = 4;
    private static final int MAX_ITERS   = 20;    // always converges well before this
    private static final int MIN_SAMPLES = 3;     // need at least 3 samples to classify

    private final ArchetypeCentroid centroids;

    public KMeansClassifier(ArchetypeCentroid centroids) {
        this.centroids = centroids;
    }

    /**
     * Classify the player given a rolling window of feature vectors.
     *
     * @return ClassificationResult with: winning archetype, distance to each centroid,
     *         blend weights for all 4 archetypes
     */
    public ClassificationResult classify(List<BehaviorFeatureVector> samples) {
        if (samples.size() < MIN_SAMPLES) {
            return ClassificationResult.neutral();
        }

        // --- Run K-means iterations ---
        @SuppressWarnings("unchecked")
        List<BehaviorFeatureVector>[] assignments = new List[K];
        for (int iter = 0; iter < MAX_ITERS; iter++) {
            // Reset assignment buckets
            for (int k = 0; k < K; k++) assignments[k] = new ArrayList<>();

            // Assign each sample to nearest centroid
            for (BehaviorFeatureVector sample : samples) {
                int nearest = nearestCentroid(sample);
                assignments[nearest].add(sample);
            }

            // Recompute centroids
            boolean changed = false;
            for (int k = 0; k < K; k++) {
                if (!assignments[k].isEmpty()) {
                    PlayerArchetype arch = PlayerArchetype.values()[k];
                    float[] oldCentroid = centroids.raw()[k].clone();
                    centroids.update(arch, assignments[k]);
                    if (!Arrays.equals(oldCentroid, centroids.raw()[k])) changed = true;
                }
            }

            if (!changed) break; // converged
        }

        // --- Classify the player's current behaviour ---
        // Use the mean of the most recent 6 samples (30 seconds) for classification
        // rather than the whole window — recent behaviour matters more
        List<BehaviorFeatureVector> recent = samples.subList(
            Math.max(0, samples.size() - 6), samples.size());
        BehaviorFeatureVector recentMean = computeMean(recent);

        // Distance to each centroid
        float[] distances = new float[K];
        for (int k = 0; k < K; k++) {
            distances[k] = recentMean.distanceTo(centroids.centroidOf(PlayerArchetype.values()[k]));
        }

        // Winning archetype = nearest centroid
        int winner = 0;
        for (int k = 1; k < K; k++) {
            if (distances[k] < distances[winner]) winner = k;
        }

        // Blend weights: inverse-distance weighting
        // A player 0.1 from PARANOID and 0.8 from IMPULSIVE gets 88% PARANOID blend
        float[] weights = inverseDistanceWeights(distances);

        return new ClassificationResult(
            PlayerArchetype.values()[winner],
            distances,
            weights
        );
    }

    private int nearestCentroid(BehaviorFeatureVector sample) {
        int nearest = 0;
        float minDist = Float.MAX_VALUE;
        for (int k = 0; k < K; k++) {
            float d = sample.distanceTo(centroids.centroidOf(PlayerArchetype.values()[k]));
            if (d < minDist) { minDist = d; nearest = k; }
        }
        return nearest;
    }

    private BehaviorFeatureVector computeMean(List<BehaviorFeatureVector> samples) {
        float[] mean = new float[BehaviorFeatureVector.DIMENSION_COUNT];
        for (BehaviorFeatureVector v : samples) {
            for (int d = 0; d < mean.length; d++) mean[d] += v.features()[d];
        }
        for (int d = 0; d < mean.length; d++) mean[d] /= samples.size();
        return new BehaviorFeatureVector(mean);
    }

    private float[] inverseDistanceWeights(float[] distances) {
        float[] weights = new float[K];
        float sumInv = 0f;
        for (int k = 0; k < K; k++) {
            // Add small epsilon to avoid division by zero when player is exactly on centroid
            weights[k] = 1f / (distances[k] + 0.0001f);
            sumInv += weights[k];
        }
        for (int k = 0; k < K; k++) weights[k] /= sumInv; // normalise to sum = 1.0
        return weights;
    }
}
```

### `ClassificationResult`

```java
package io.github.superteam.resonance.behavior;

public record ClassificationResult(
    PlayerArchetype dominant,        // the closest archetype
    float[] distanceToCentroid,      // [PARANOID, METHODICAL, IMPULSIVE, PANICKED]
    float[] blendWeights             // [PARANOID, METHODICAL, IMPULSIVE, PANICKED] — sums to 1.0
) {
    /** Blend weights are already normalised — use directly as lerp parameters. */
    public float weightOf(PlayerArchetype archetype) {
        return blendWeights[archetype.ordinal()];
    }

    public static ClassificationResult neutral() {
        return new ClassificationResult(
            PlayerArchetype.DEFAULT,
            new float[]{ 0.5f, 0.0f, 0.5f, 0.5f },
            new float[]{ 0.1f, 0.7f, 0.1f, 0.1f }  // mostly METHODICAL
        );
    }
}
```

---

## 5. Significant Change Detection

Re-running K-means every 5 seconds on every tick is wasteful and makes the game feel twitchy — the enemy shouldn't change behaviour every few seconds. Instead, only re-classify when the player's mean behaviour has shifted meaningfully from the last classification point.

```java
package io.github.superteam.resonance.behavior;

/**
 * Decides when to trigger a K-means re-classification.
 *
 * Triggers on:
 *  1. Large Euclidean shift in the recent feature mean vs. last classified mean
 *  2. Dominant dimension flip (e.g. movementSpeed goes from 0.2 to 0.8 = player started sprinting)
 *  3. Minimum time guard (never re-classify more often than once per 20 seconds)
 */
public final class BehaviorChangeDetector {

    private static final float SHIFT_THRESHOLD      = 0.25f;  // Euclidean distance
    private static final float DIM_FLIP_THRESHOLD   = 0.40f;  // per-dimension jump
    private static final float MIN_RECLASSIFY_SECS  = 20f;

    private BehaviorFeatureVector lastClassifiedVector;
    private float                 timeSinceLastClassify = MIN_RECLASSIFY_SECS; // start ready

    public void update(float delta) {
        timeSinceLastClassify += delta;
    }

    /**
     * Returns true if a re-classification should be triggered.
     * Call after each new sample is added.
     */
    public boolean shouldReclassify(BehaviorFeatureVector currentMean) {
        if (timeSinceLastClassify < MIN_RECLASSIFY_SECS) return false;
        if (lastClassifiedVector == null) return true; // first time

        // Global shift check
        float shift = currentMean.distanceTo(lastClassifiedVector);
        if (shift >= SHIFT_THRESHOLD) return true;

        // Per-dimension flip check — catches sudden single-axis changes
        for (int d = 0; d < BehaviorFeatureVector.DIMENSION_COUNT; d++) {
            float dimDelta = Math.abs(currentMean.features()[d] - lastClassifiedVector.features()[d]);
            if (dimDelta >= DIM_FLIP_THRESHOLD) return true;
        }

        return false;
    }

    public void onReclassified(BehaviorFeatureVector classifiedVector) {
        lastClassifiedVector = classifiedVector;
        timeSinceLastClassify = 0f;
    }
}
```

**Examples of what triggers a re-classification:**

| Scenario | Shift | Triggers? |
|----------|-------|----------|
| Player stops crouching and starts running | DIM_MOVE_SPEED: 0.2→0.8 (Δ=0.60) | ✅ Yes |
| Player turns off flashlight and starts hiding | DIM_STATIONARY: 0.1→0.6 (Δ=0.50) | ✅ Yes |
| Normal play variation (slight speed changes) | Global shift 0.08 | ❌ No |
| Player slams a door | DIM_DOOR_SPEED spike in one sample | ❌ No (single sample, not mean) |

---

## 6. What Each Archetype Drives

All effects use **blend weights** from `ClassificationResult`, not a hard switch. A player at 60% PARANOID / 40% METHODICAL gets a blended response.

The blending formula for any float parameter `P`:
```java
float blended = result.weightOf(PARANOID)    * P_PARANOID
              + result.weightOf(METHODICAL)  * P_METHODICAL
              + result.weightOf(IMPULSIVE)   * P_IMPULSIVE
              + result.weightOf(PANICKED)    * P_PANICKED;
```

---

### 6.1 Enemy AI Adaptation

The enemy's parameters are modified based on the player's archetype:

```java
public final class EnemyBehaviorEffect {

    // How long the enemy waits before investigating a sound
    // Paranoid: longer wait (player is already scared — build tension)
    // Impulsive: immediate response (punish noise)
    private static final float[] INVESTIGATION_DELAY = { 3.5f, 2.0f, 0.5f, 1.0f };

    // Enemy patrol speed multiplier
    // Methodical player = enemy patrols more carefully (smarter)
    // Panicked player = enemy moves faster (punish running)
    private static final float[] PATROL_SPEED_MULT = { 1.0f, 1.3f, 1.1f, 1.4f };

    // Enemy vision FOV — wider FOV against methodical (methodical players expect walls to save them)
    private static final float[] VISION_FOV = { 70f, 85f, 65f, 60f };

    // Enemy hearing range multiplier
    // Quieter player (methodical) = enemy hears farther (harder to lose)
    // Louder player (impulsive) = enemy hears less far (they expect loud)
    private static final float[] HEARING_RANGE_MULT = { 1.0f, 1.4f, 0.8f, 0.9f };

    // How often the enemy fakes the player out — moves to a spot and waits silently
    // Only used against paranoid players who check their back constantly
    private static final float[] FAKE_OUT_PROBABILITY = { 0.35f, 0.0f, 0.0f, 0.1f };

    public EnemyParams compute(ClassificationResult result) {
        return new EnemyParams(
            blend(result, INVESTIGATION_DELAY),
            blend(result, PATROL_SPEED_MULT),
            blend(result, VISION_FOV),
            blend(result, HEARING_RANGE_MULT),
            blend(result, FAKE_OUT_PROBABILITY)
        );
    }

    private float blend(ClassificationResult r, float[] perArchetype) {
        float v = 0f;
        for (PlayerArchetype a : PlayerArchetype.values()) {
            v += r.weightOf(a) * perArchetype[a.ordinal()];
        }
        return v;
    }
}
```

**In plain language what the enemy does per archetype:**

| Archetype | Enemy behaviour |
|-----------|----------------|
| **Paranoid** | Waits longer before investigating, uses fake-outs (appears briefly then retreats), makes sounds near hiding spots without appearing, patrols at normal speed |
| **Methodical** | Has wider vision cone, hears the player from farther away, patrols more thoroughly, no fake-outs — this enemy plays it straight because the player plays it straight |
| **Impulsive** | Reacts to sounds almost immediately (0.5s delay), patrols a bit faster, narrower FOV (impulsive players don't check sight lines so this doesn't help them) |
| **Panicked** | Fastest patrol, responds quickly, narrower FOV (panicked player has tunnel vision so wider FOV would be unfair), appears from unexpected directions |

---

### 6.2 Jump Scare Timing

```java
public final class JumpScareTimingEffect {

    // Minimum seconds between scares
    private static final float[] MIN_SCARE_INTERVAL = { 60f, 50f, 30f, 75f };
    // Paranoid: space them out — they scare themselves between scares
    // Methodical: moderate interval — they need a rhythm broken
    // Impulsive: short interval — they recover quickly
    // Panicked: long interval — they're already terrified; less is more

    // Tension required before a scare fires (0..100)
    private static final float[] TENSION_THRESHOLD = { 60f, 75f, 55f, 85f };
    // Paranoid: lower threshold — scare when tension is moderate
    // Methodical: higher — needs more buildup because they're rational
    // Impulsive: low — they don't build tension, scare early
    // Panicked: very high — only scare when tension is extreme

    // Preferred scare types (used to weight scare selection)
    // Encoded as: [AUDIO_ONLY, SCREEN_FLASH, ENEMY_APPEAR, HALLUCINATION, ENVIRONMENT]
    private static final float[][] SCARE_TYPE_WEIGHTS = {
        { 0.1f, 0.2f, 0.1f, 0.5f, 0.1f },  // PARANOID: hallucination (see things that aren't there)
        { 0.2f, 0.1f, 0.1f, 0.1f, 0.5f },  // METHODICAL: environment (something moves when they feel safe)
        { 0.3f, 0.3f, 0.3f, 0.0f, 0.1f },  // IMPULSIVE: audio + flash + enemy (loud, immediate)
        { 0.4f, 0.3f, 0.2f, 0.0f, 0.1f },  // PANICKED: loud audio (they're already visual mess)
    };

    public JumpScareParams compute(ClassificationResult result) {
        return new JumpScareParams(
            blend(result, MIN_SCARE_INTERVAL),
            blend(result, TENSION_THRESHOLD),
            blendScareTypeWeights(result)
        );
    }
}
```

---

### 6.3 Difficulty Tuning

```java
public final class DifficultyEffect {

    // Enemy aggression threshold — how loud does a sound need to be to trigger investigation
    // Methodical players are quiet — lower threshold makes the enemy more attentive
    private static final float[] SOUND_INTENSITY_THRESHOLD = { 0.30f, 0.15f, 0.45f, 0.40f };

    // How long the enemy remembers a last-heard position before giving up
    private static final float[] MEMORY_DURATION_SECONDS = { 8f, 14f, 5f, 6f };
    // Methodical: enemy remembers 14s — hard to shake
    // Impulsive: enemy forgets quickly — they forget sound fast

    // Item spawn rate modifier — methodical gets more useful items (rewarding careful play)
    private static final float[] ITEM_SPAWN_MODIFIER = { 0.9f, 1.2f, 0.8f, 0.7f };

    // Sanity drain multiplier (environment is more forgiving to panicked players — they're already punished)
    private static final float[] SANITY_DRAIN_MULT = { 1.1f, 1.0f, 1.15f, 0.85f };
}
```

---

### 6.4 Atmosphere Shift

The atmosphere effect subtly reshapes the world to mirror the player's energy. The player may not notice consciously — but they feel it.

```java
public final class AtmosphereEffect {

    // Ambient light level multiplier (lower = darker world)
    // Methodical/paranoid players get a darker atmosphere — mirrors their tension
    // Impulsive/panicked players get slightly brighter — the chaos is visible
    private static final float[] AMBIENT_LIGHT_MULT = { 0.80f, 0.70f, 1.0f, 0.95f };

    // Ambience track crossfade to archetype-specific variant
    // Each archetype has its own ambience bed in assets/audio/ambience/
    private static final String[] AMBIENCE_TRACK = {
        "audio/ambience/paranoid_bed.wav",   // PARANOID: occasional taps, whispers, reversed sounds
        "audio/ambience/methodical_bed.wav", // METHODICAL: very quiet, low hum, distant drips
        "audio/ambience/impulsive_bed.wav",  // IMPULSIVE: louder creak frequency, occasional bangs far away
        "audio/ambience/panicked_bed.wav",   // PANICKED: irregular heartbeat undertone, high freq hiss
    };

    // WorldSoundEmitter interval multiplier — how often ambient emitters fire
    // Paranoid: more frequent small sounds (world feels alive/watching)
    // Methodical: less frequent (silence is the horror)
    private static final float[] EMITTER_INTERVAL_MULT = { 0.6f, 1.5f, 0.9f, 0.8f };

    // Flashlight flicker intensity when near enemy
    // Paranoid: already toggles it; increase flicker during tension
    private static final float[] FLICKER_INTENSITY_MULT = { 1.4f, 0.8f, 1.0f, 1.2f };

    public void apply(ClassificationResult result, GameAudioSystem audio,
                      LightManager lights, float delta) {
        // 1. Crossfade to new ambience if archetype changed since last apply
        PlayerArchetype dominant = result.dominant();
        String targetAmbience = AMBIENCE_TRACK[dominant.ordinal()];
        if (!targetAmbience.equals(currentAmbience)) {
            audio.setAmbience(targetAmbience, 4.0f); // 4s crossfade — imperceptible
            currentAmbience = targetAmbience;
        }

        // 2. Ambient light level — lerp toward target (very slow, takes 30+ seconds)
        float targetLightMult = blend(result, AMBIENT_LIGHT_MULT);
        currentLightMult = MathUtils.lerp(currentLightMult, targetLightMult, delta * 0.02f);
        lights.setAmbientMultiplier(currentLightMult);

        // 3. WorldSoundEmitter interval multiplier
        float intervalMult = blend(result, EMITTER_INTERVAL_MULT);
        audio.setWorldEmitterIntervalMultiplier(intervalMult);
    }
}
```

**Atmosphere in plain language:**

| Archetype | World feels like... |
|-----------|-------------------|
| **Paranoid** | Small sounds appear more often — taps, distant footsteps you can't place. Flashlight flickers more during tension. More frequent but quiet ambient events |
| **Methodical** | Silence. Long stretches with almost no ambient sound. The darkness is slightly heavier. The horror is in what you don't hear |
| **Impulsive** | Louder ambient bed. Things creak more as you pass them. The world is noisier — reflects your noise back |
| **Panicked** | Heartbeat undertone in the ambience. High-frequency hiss. The world sounds like a panic attack |

---

## 7. Package and Class Design

```
behavior/
  PlayerBehaviorTracker.java       — measures raw metrics each frame, produces samples
  BehaviorSample.java              — one 5-second window snapshot (record)
  BehaviorFeatureVector.java       — normalized [0..1] feature array (record)
  FeatureNormalizer.java           — Sample → FeatureVector
  KMeansClassifier.java            — K-means algorithm, produces ClassificationResult
  ClassificationResult.java        — dominant archetype + blend weights (record)
  PlayerArchetype.java             — enum: PARANOID, METHODICAL, IMPULSIVE, PANICKED
  ArchetypeCentroid.java           — mutable centroid state for each archetype
  BehaviorChangeDetector.java      — decides when to re-classify
  BehaviorSystem.java              — master controller, owns all of the above
  effects/
    EnemyBehaviorEffect.java       — → EnemyParams for EnemyController
    JumpScareTimingEffect.java     — → JumpScareParams for JumpScareDirector
    DifficultyEffect.java          — → DifficultyParams for various systems
    AtmosphereEffect.java          — → lights, ambience, emitter interval
  debug/
    BehaviorDebugOverlay.java      — in-game HUD panel (toggle with F9)
```

---

### `PlayerBehaviorTracker`

Collects metrics each frame and produces a `BehaviorSample` every `SAMPLE_INTERVAL_SECONDS`:

```java
package io.github.superteam.resonance.behavior;

/**
 * Measures raw player behaviour each frame.
 * Every SAMPLE_INTERVAL_SECONDS, captures a BehaviorSample and notifies BehaviorSystem.
 *
 * Listens to:
 *  - PlayerController  (velocity, crouching, stationary)
 *  - Camera rotation   (look-around rate)
 *  - FlashlightController (toggles)
 *  - DoorCreakSystem   (door open speed events)
 *  - RaycastInteractionSystem (interaction count)
 *  - SanitySystem      (sanity delta)
 *  - JumpScareDirector (scare events — for hide response timing)
 *  - SoundPropagationOrchestrator (player-caused sound intensity)
 */
public final class PlayerBehaviorTracker {

    private static final float SAMPLE_INTERVAL_SECONDS = 5f;

    // -- Frame-by-frame accumulators --
    private float velocityAccum      = 0f;
    private float noiseAccum         = 0f;
    private float crouchTimeAccum    = 0f;
    private float stationaryAccum    = 0f;
    private float lookVarianceAccum  = 0f;
    private int   flashlightToggles  = 0;
    private float doorSpeedAccum     = 0f;
    private int   doorSpeedSamples   = 0;
    private int   interactionCount   = 0;
    private float sanityLost         = 0f;
    private float lastScareTimestamp = -1f;
    private float hideResponseAccum  = -1f; // -1 = no scare this window

    private float lastCameraYaw    = 0f;
    private float frameCount       = 0f;
    private float sampleTimer      = 0f;
    private float elapsedSeconds   = 0f;

    private final java.util.function.Consumer<BehaviorSample> onSampleReady;

    public PlayerBehaviorTracker(java.util.function.Consumer<BehaviorSample> onSampleReady) {
        this.onSampleReady = onSampleReady;
    }

    public void update(float delta, PlayerController player, PerspectiveCamera camera,
                       FlashlightController flashlight, SanitySystem sanity) {
        elapsedSeconds += delta;
        sampleTimer    += delta;
        frameCount     += 1f;

        // Movement metrics
        float speed = player.getHorizontalSpeed();
        velocityAccum += speed;
        if (!player.isMoving()) stationaryAccum += delta;
        if (player.isCrouching() && player.isMoving()) crouchTimeAccum += delta;

        // Camera look-around variance (using frame-to-frame yaw delta)
        float yawDelta = Math.abs(camera.direction.x - lastCameraYaw);
        lookVarianceAccum += yawDelta * yawDelta;  // variance = mean of squared deltas
        lastCameraYaw = camera.direction.x;

        // Sanity delta
        sanityLost += sanity.getLastDeltaThisFrame();

        // Hide response — how quickly player stops after a scare
        if (lastScareTimestamp > 0 && hideResponseAccum < 0) {
            if (!player.isMoving()) {
                hideResponseAccum = elapsedSeconds - lastScareTimestamp;
                lastScareTimestamp = -1f;
            }
        }

        // Emit sample every interval
        if (sampleTimer >= SAMPLE_INTERVAL_SECONDS) {
            emitSample();
        }
    }

    private void emitSample() {
        float frames = Math.max(1f, frameCount);
        float movingTime = sampleTimer - stationaryAccum;

        BehaviorSample sample = new BehaviorSample(
            velocityAccum / frames,
            noiseAccum / frames,
            movingTime > 0.1f ? crouchTimeAccum / movingTime : 0f,
            stationaryAccum / sampleTimer,
            (float) Math.sqrt(lookVarianceAccum / frames),
            flashlightToggles / (sampleTimer / 60f),
            doorSpeedSamples > 0 ? doorSpeedAccum / doorSpeedSamples : 0f,
            interactionCount / (sampleTimer / 60f),
            sanityLost,
            hideResponseAccum
        );

        onSampleReady.accept(sample);
        resetAccumulators();
    }

    // -- Event hooks called by other systems --

    public void onFlashlightToggled()           { flashlightToggles++; }
    public void onDoorOpened(float speed)       { doorSpeedAccum += speed; doorSpeedSamples++; }
    public void onPlayerInteracted()            { interactionCount++; }
    public void onPlayerMadeSound(float rms)    { noiseAccum = Math.max(noiseAccum, rms); }
    public void onJumpScareExecuted()           { lastScareTimestamp = elapsedSeconds; hideResponseAccum = -1f; }

    private void resetAccumulators() {
        velocityAccum = 0f; noiseAccum = 0f; crouchTimeAccum = 0f;
        stationaryAccum = 0f; lookVarianceAccum = 0f; flashlightToggles = 0;
        doorSpeedAccum = 0f; doorSpeedSamples = 0; interactionCount = 0;
        sanityLost = 0f; hideResponseAccum = -1f;
        frameCount = 0f; sampleTimer = 0f;
    }
}
```

---

### `BehaviorSystem`

The master controller. Owns everything, called once per frame from the active screen:

```java
package io.github.superteam.resonance.behavior;

/**
 * Master controller for the player behaviour classification pipeline.
 * Owned by the active game screen. Resets on new session.
 *
 * Call update() once per frame after PlayerController and SanitySystem have updated.
 */
public final class BehaviorSystem {

    private static final int MAX_WINDOW_SAMPLES = 60; // 5 minutes of history

    private final PlayerBehaviorTracker  tracker;
    private final FeatureNormalizer      normalizer    = new FeatureNormalizer();
    private final ArchetypeCentroid      centroids     = new ArchetypeCentroid();
    private final KMeansClassifier       classifier;
    private final BehaviorChangeDetector changeDetector = new BehaviorChangeDetector();

    private final List<BehaviorFeatureVector> sampleWindow = new ArrayList<>();
    private ClassificationResult currentResult = ClassificationResult.neutral();

    // Effect appliers — updated each time classification changes
    private final EnemyBehaviorEffect     enemyEffect;
    private final JumpScareTimingEffect   scareEffect;
    private final DifficultyEffect        difficultyEffect;
    private final AtmosphereEffect        atmosphereEffect;

    public BehaviorSystem(EnemyController enemy, JumpScareDirector scareDtr,
                          GameAudioSystem audio, LightManager lights) {
        classifier       = new KMeansClassifier(centroids);
        enemyEffect      = new EnemyBehaviorEffect(enemy);
        scareEffect      = new JumpScareTimingEffect(scareDtr);
        difficultyEffect = new DifficultyEffect();
        atmosphereEffect = new AtmosphereEffect(audio, lights);

        tracker = new PlayerBehaviorTracker(this::onSampleReady);
    }

    public void update(float delta, PlayerController player, PerspectiveCamera camera,
                       FlashlightController flashlight, SanitySystem sanity) {
        changeDetector.update(delta);
        tracker.update(delta, player, camera, flashlight, sanity);
        atmosphereEffect.update(currentResult, delta); // atmosphere always ticks for smooth lerp
    }

    private void onSampleReady(BehaviorSample rawSample) {
        BehaviorFeatureVector featureVector = normalizer.normalize(rawSample);

        // Rolling window
        sampleWindow.add(featureVector);
        if (sampleWindow.size() > MAX_WINDOW_SAMPLES) sampleWindow.remove(0);

        // Compute recent mean for change detection
        BehaviorFeatureVector recentMean = computeRecentMean();

        if (changeDetector.shouldReclassify(recentMean)) {
            ClassificationResult newResult = classifier.classify(sampleWindow);
            changeDetector.onReclassified(recentMean);
            onNewClassification(newResult);
        }
    }

    private void onNewClassification(ClassificationResult result) {
        currentResult = result;
        Gdx.app.log("BehaviorSystem", "Reclassified → " + result.dominant()
            + String.format("  [P:%.2f M:%.2f I:%.2f Pa:%.2f]",
                result.weightOf(PlayerArchetype.PARANOID),
                result.weightOf(PlayerArchetype.METHODICAL),
                result.weightOf(PlayerArchetype.IMPULSIVE),
                result.weightOf(PlayerArchetype.PANICKED)));

        // Push new parameters to dependent systems
        enemyEffect.apply(result);
        scareEffect.apply(result);
        difficultyEffect.apply(result);
        // atmosphereEffect is applied in update() for smooth continuous lerp
    }

    // -- Public API --
    public ClassificationResult  currentResult()   { return currentResult; }
    public PlayerArchetype        currentArchetype() { return currentResult.dominant(); }
    public PlayerBehaviorTracker  tracker()          { return tracker; }
    public int                    sampleCount()      { return sampleWindow.size(); }
}
```

---

## 8. Integration Points

### Wire `PlayerBehaviorTracker` to existing systems

Every system that the tracker needs to hear from must call the corresponding hook:

```java
// FlashlightController.toggle():
behaviorSystem.tracker().onFlashlightToggled();

// DoorCreakSystem.update() — when creak fires:
behaviorSystem.tracker().onDoorOpened(angularSpeed);

// RaycastInteractionSystem — when interaction resolves:
behaviorSystem.tracker().onPlayerInteracted();

// SpatialSfxEmitter.playAndPropagate() — for PLAYER_ACTION category only:
if (graphEvent != null && graphEvent.hearingCategory() == HearingCategory.PLAYER_ACTION) {
    behaviorSystem.tracker().onPlayerMadeSound(graphIntensity);
}

// JumpScareDirector.execute():
behaviorSystem.tracker().onJumpScareExecuted();
```

### Wire effects back to dependent systems

```java
// EnemyBehaviorEffect.apply() pushes to EnemyController:
enemy.setInvestigationDelay(params.investigationDelay());
enemy.setPatrolSpeedMultiplier(params.patrolSpeedMult());
enemy.setVisionFov(params.visionFov());
enemy.setHearingRangeMultiplier(params.hearingRangeMult());
enemy.setFakeOutProbability(params.fakeOutProbability());

// JumpScareTimingEffect.apply() pushes to JumpScareDirector:
director.setMinInterval(params.minScareInterval());
director.setTensionThreshold(params.tensionThreshold());
director.setScareTypeWeights(params.scareTypeWeights());

// DifficultyEffect.apply() pushes to EnemyPerception and SanitySystem:
enemyPerception.setSoundIntensityThreshold(params.soundIntensityThreshold());
enemyPerception.setMemoryDuration(params.memoryDurationSeconds());
sanitySystem.setDrainMultiplier(params.sanityDrainMult());
audio.setWorldEmitterSpawnMultiplier(params.itemSpawnModifier());
```

### Add `BehaviorSystem` to `EventContext`

```java
public final class EventContext {
    // ... all existing fields ...
    public final BehaviorSystem behaviorSystem;   // ← add
}
```

This lets event actions query the current archetype if needed. Example: a story event that says different dialogue depending on archetype:

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

## 9. Debug Console Commands

Add to `ConsoleRegistry`:

| Command | Example | Output |
|---------|---------|--------|
| `behavior` | `behavior` | Current archetype + blend weights |
| `behavior samples` | `behavior samples` | Number of samples in window |
| `behavior features` | `behavior features` | Last normalized feature vector |
| `behavior force` | `behavior force PARANOID` | Force a specific archetype (debug) |
| `behavior reset` | `behavior reset` | Clear all samples, return to neutral |

---

### `BehaviorDebugOverlay` — in-game panel (F9)

When F9 is pressed, shows a compact panel in the top-right corner:

```
╔══ PLAYER BEHAVIOUR ════════════════╗
║ Archetype:  PARANOID               ║
║ Samples:    18 / 60                ║
║ Secs since reclassify: 34s         ║
╠════════════════════════════════════╣
║ PARANOID   ████████░░░░  0.61      ║
║ METHODICAL ████░░░░░░░░  0.24      ║
║ IMPULSIVE  █░░░░░░░░░░░  0.10      ║
║ PANICKED   ░░░░░░░░░░░░  0.05      ║
╠════════════════════════════════════╣
║ MoveSpeed  ████░░░░  0.35          ║
║ Noise      ██░░░░░░  0.28          ║
║ Crouch     ██████░░  0.62          ║
║ Stationary ████████  0.71          ║
║ LookAround ████████████  0.90      ║
║ Flashlight ████████░░░░  0.68      ║
║ DoorSpeed  ██░░░░░░░░░░  0.22      ║
║ Interact   ████░░░░░░░░  0.44      ║
║ SanityLoss ████████░░░░  0.58      ║
║ HideResp   ████░░░░░░░░  0.12      ║
╚════════════════════════════════════╝
```

Each bar is drawn with `ShapeRenderer.rect()` inline with the HUD rendering.

---

## 10. Files to Create

| File | Package | Action |
|------|---------|--------|
| `PlayerBehaviorTracker.java` | `behavior` | Create |
| `BehaviorSample.java` | `behavior` | Create |
| `BehaviorFeatureVector.java` | `behavior` | Create |
| `FeatureNormalizer.java` | `behavior` | Create |
| `KMeansClassifier.java` | `behavior` | Create |
| `ClassificationResult.java` | `behavior` | Create |
| `PlayerArchetype.java` | `behavior` | Create |
| `ArchetypeCentroid.java` | `behavior` | Create |
| `BehaviorChangeDetector.java` | `behavior` | Create |
| `BehaviorSystem.java` | `behavior` | Create |
| `EnemyBehaviorEffect.java` | `behavior/effects` | Create |
| `JumpScareTimingEffect.java` | `behavior/effects` | Create |
| `DifficultyEffect.java` | `behavior/effects` | Create |
| `AtmosphereEffect.java` | `behavior/effects` | Create |
| `BehaviorDebugOverlay.java` | `behavior/debug` | Create |
| `EnemyParams.java` | `behavior/effects` | Create (record) |
| `JumpScareParams.java` | `behavior/effects` | Create (record) |
| `DifficultyParams.java` | `behavior/effects` | Create (record) |
| `event/EventContext.java` | `event` | Add `behaviorSystem` field |
| `enemy/EnemyController.java` | `enemy` | Add setters for behavior-driven params |
| `enemy/EnemyPerception.java` | `enemy` | Add `setSoundIntensityThreshold()`, `setMemoryDuration()` |
| `scare/JumpScareDirector.java` | `scare` | Add `setMinInterval()`, `setTensionThreshold()`, `setScareTypeWeights()` |
| `sanity/SanitySystem.java` | `sanity` | Add `setDrainMultiplier()`, `getLastDeltaThisFrame()` |
| `lighting/LightManager.java` | `lighting` | Add `setAmbientMultiplier()` |
| `audio/GameAudioSystem.java` | `audio` | Add `setWorldEmitterIntervalMultiplier()` |
| `debug/built_in/BehaviorCommand.java` | `debug/built_in` | Create |

---

## 11. Acceptance Criteria

| Test | Pass condition |
|------|---------------|
| Tracker — motion | Player walks for 30s → `movementSpeed` sample > 0 |
| Tracker — quiet | Player crouches silently for 30s → `noiseLevel` near 0, `crouchRatio` near 1 |
| Tracker — flashlight | Toggle flashlight 5× in one window → `flashlightToggleRate` high |
| Normalizer | Sprint speed (5.5 m/s) normalizes to 1.0; stationary normalizes to 0.0 |
| K-means — initial | First classification with < 3 samples returns `ClassificationResult.neutral()` |
| K-means — converges | 20 samples of pure-sprint play → dominant = IMPULSIVE or PANICKED |
| K-means — blend | Player at midpoint between two archetypes → no weight exceeds 0.55 |
| Change detector | No reclassify within 20s of last classify regardless of shift |
| Change detector | Player goes from crouch-walk to sprint → reclassify triggers |
| Change detector | Minor speed variation (0.2→0.3) → no reclassify |
| Enemy effect | Classify METHODICAL → enemy hearing range increased (log confirms) |
| Enemy effect | Classify IMPULSIVE → enemy investigation delay < 1s |
| Enemy effect | Classify PARANOID → fake-out probability > 0 |
| Scare timing | Classify PANICKED → min scare interval ≥ 70s |
| Scare timing | Classify IMPULSIVE → scare fires at lower tension than METHODICAL |
| Atmosphere | Classify METHODICAL → ambience crossfades to `methodical_bed.wav` |
| Atmosphere | Ambient light multiplier lerps slowly (takes > 20s to fully shift) |
| Atmosphere | Classify PARANOID → WorldSoundEmitter fires more frequently |
| Debug overlay | F9 → overlay shows correct archetype and bar heights |
| Debug console | `behavior force PANICKED` → dominant changes immediately |
| Debug console | `behavior reset` → sample count returns to 0 |
| Session reset | Start new game → all samples cleared, returns to neutral METHODICAL |
| No jitter | Play 5 minutes of mixed behaviour → reclassify fires < 10 times total |

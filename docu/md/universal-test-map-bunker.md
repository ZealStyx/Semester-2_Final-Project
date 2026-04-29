# Universal Test Map — Horror Bunker Layout
### Copilot Agent Prompt

---

## Context & Files to Modify

This prompt targets two files:

| File | Method to change |
|------|-----------------|
| `core/src/main/java/io/github/superteam/resonance/devTest/universal/TestMapLayout.java` | Replace body of `buildDefaultLayoutInstance()` |
| `core/src/main/java/io/github/superteam/resonance/devTest/universal/UniversalTestScene.java` | Add `buildBunkerWalls()` private method; call it from `buildProceduralHubScene()` |

---

## Coordinate System — Read This First

> **Sound propagation dependency:** every `addBox(..., true)` call below is an acoustic surface. Wall dimensions, opening widths, and pillar placements are sized to keep all `GraphPopulator` edge hops within `EDGE_MAX_DISTANCE_METERS = 4.0 m`. Do not resize or remove any collider box without re-checking acoustic connectivity.

```
addBox(centerX, centerY, centerZ, width, height, depth, addCollider)
         │         │         │       │       │       │
         X         Y(up)     Z      X-dim   Y-dim  Z-dim
```

- **Y = 0** is the floor surface. A 3 m tall room has wall center at Y = 1.5.
- **HUB_X = 40.0, HUB_Z = 40.0** — `addBox` calls in `buildProceduralHubScene()` use **world coordinates** (already include HUB offset). Zone/DoorSpawn entries use **local coordinates** (subtract HUB).
- **Wall thickness = 0.3 m** throughout.
- **Floor slab**: 0.1 m thick, center at Y = −0.05.
- **Door opening standard**: 1.2 m wide × 2.1 m tall. Represented by two wall segments + a header box above the gap.
- **Corridor opening** (no door, just a gap): walls simply stop at the passage edges.

---

## Map Overview

```
                     [DARK ROOM]
                     X[35.5–44.5]
                     Z[51.0–60.0]
                          │  open passage (no door)
                          │
               [CORRIDOR 1] ──── door ──── [CROUCH ALCOVE]
               X[38.5–41.5]                X[32.5–38.5]
               Z[43.0–51.0]                Z[45.5–48.5]
                          │  door
                          │
                     [SPAWN ROOM]       ← player spawns here
                     X[36.5–43.5]
                     Z[37.0–43.0]
```

---

## Sound Propagation — Required Support

> **This map must fully support the acoustic graph system.** Every geometry decision in this document — wall placement, opening sizes, pillar positions, relay boxes — is a **hard requirement** driven by sound propagation correctness, not just visual layout preference.

> **Do not manually place graph nodes.** `GraphPopulator.populate(worldColliders)` auto-generates the entire acoustic graph from the collision boxes added via `addBox(..., true)`. The geometry below is deliberately shaped to produce a well-connected graph. Deviating from the specified dimensions or removing any `addCollider=true` box will break acoustic connectivity.

### Pipeline

```
addBox(..., true)
    │
    └─► worldColliders  (Array<BoundingBox>)
            │
            └─► GraphPopulator.populate()
                    │   samples nodes off every wall face
                    │   WALL_NODE_SPACING = 2.0 m
                    │   node heights: Y+0.9 m and Y+1.6 m (ear/chest level)
                    │
                    ├─► LOS edge  — nodes within 4 m with clear line-of-sight
                    │               (sound travels freely through door gaps)
                    │
                    └─► Transmission edge — nodes within 4 m blocked by a wall
                                           (sound bleeds through at reduced intensity)
                            │
                            └─► wall thickness → AcousticMaterial → transmissionCoefficient
```

### Wall thickness → acoustic material (auto-inferred)

| Thickness | Material | Transmission | Effect |
|-----------|----------|-------------|--------|
| ≤ 0.18 m | GLASS | 0.35 | Very leaky |
| ≤ 0.35 m | WOOD | 0.22 | Audible bleed — interior partitions |
| ≤ 0.55 m | METAL | 0.12 | Mostly blocked |
| > 0.55 m | CONCRETE | 0.05 | Nearly silent — structural walls |

**Our 0.3 m partition walls → WOOD (0.22).** You can hear through them faintly — correct for thin bunker partitions. The sealed exterior walls (`south wall of spawn`, `north wall of dark room`) stay at 0.3 m; increase them to **0.6 m** only if you want those surfaces to be acoustically near-opaque.

### How door openings propagate sound

A door gap is a **physical absence** in the wall geometry (two segments + header, with air in between). `GraphPopulator` traces straight rays between candidate node pairs. Rays through the gap hit no collider → **direct LOS edge**, full intensity. Rays through the wall segments hit 0.3 m of WOOD → **transmission edge**, 22% intensity. This means:

- Player shoots in spawn room → sound propagates through the door gap at full intensity into the corridor and onward
- Player shoots in spawn room with door closed (door mesh in the gap) → the door *physics body* does not appear in `worldColliders` so the graph still routes through the gap, but `DoorCreakSystem` knows the door is closed and can attenuate playback separately

### Why the 4 m edge limit matters

`EDGE_MAX_DISTANCE_METERS = 4.0 m`. Nodes farther apart than 4 m are never directly connected. Rooms wider than ~4 m need **intermediate geometry** to bridge the gap.

| Room | Width | Bridged by |
|------|-------|-----------|
| Spawn (7 m) | 7 m X | Side wall nodes at X=36.35 and X=43.65 chain through mid-wall samples; max hop ≈ 3.5 m ✓ |
| Corridor (3 m) | 3 m | East + west wall nodes are 3 m apart — always connect ✓ |
| Crouch Alcove (6 m) | 6 m X | West wall at X=32.35, east face at X=38.65; gap = 6 m. **Add a shelf or low box at X≈36** to relay. See §2a note. |
| Dark Room (9 m) | 9 m X | Three pillars bridge it: west wall → pillar SW → pillar N → pillar SE → east wall, all within 4 m ✓ |

### Crouch Alcove relay box *(required — do not omit)*

The alcove is 6 m wide — too wide for a single acoustic graph hop. **This box is a sound propagation requirement, not optional decoration.** Omitting it will leave the alcove acoustically disconnected from the rest of the graph. Add one low storage box inside it:

```java
// Relay obstacle — sits on the alcove floor, gives GraphPopulator a mid-room wall face
// Low enough to step over (0.6 m) but tall enough to generate nodes (> 0.5 m threshold)
addBox(35.5f, 0.30f, 47.0f,   1.0f, 0.60f, 0.8f,  true);  // storage crate relay
```

This creates nodes at X≈35 (within 3 m of west wall at X=32.35) and X≈36 (within 3 m of east face at X=38.65). Full graph connectivity across the alcove.

### Acoustic isolation summary

| Boundary | Thickness | Material | Sound leaks through? |
|----------|-----------|----------|---------------------|
| Spawn ↔ Corridor (wall segments beside door) | 0.3 m | WOOD | Yes — 22% (muffled) |
| Spawn ↔ Corridor (door gap) | — | LOS | Yes — 100% (direct) |
| Corridor ↔ Alcove (wall beside alcove door) | 0.3 m | WOOD | Yes — 22% |
| Corridor ↔ Dark Room (open passage) | — | LOS | Yes — 100% |
| Spawn south wall | 0.3 m | WOOD | Yes — 22% |
| Dark Room north wall | 0.3 m | WOOD | Yes — 22% |

---

## Step 1 — `TestMapLayout.buildDefaultLayoutInstance()`

Replace the **entire body** of this method with the following. All coordinates are **local** (relative to HUB).

```java
public static TestMapLayout buildDefaultLayoutInstance() {
    TestMapLayout lm = new TestMapLayout();

    // ─── Zone Detection Volumes ─────────────────────────────────────────────
    // Zone(name, localX, localY, localZ, width, depth, height)
    // localY = 0 centers the volume at floor; used for zone detection, not walls.

    lm.zones.add(new Zone("SPAWN",         0f,   0f,   0f,   7.0f,  6.0f,  3.0f));
    lm.zones.add(new Zone("CORRIDOR_1",    0f,   0f,   7.0f, 3.0f,  8.0f,  2.8f));
    lm.zones.add(new Zone("DARK_ROOM",     0f,   0f,  15.5f, 9.0f,  9.0f,  3.2f));
    lm.zones.add(new Zone("CROUCH_ALCOVE",-5.5f, 0f,   7.0f, 6.0f,  3.0f,  1.55f));

    // ─── Door Spawns ─────────────────────────────────────────────────────────
    // Use ONLY the simple constructor — there is no "door variant" type.
    // Door behaviour (creak speed, barge) is 100% driven by player input:
    //   • Mouse drag speed  → DoorCreakSystem selects creak_slow / medium / fast wav
    //   • Sprint into door  → DoorBargeDetector fires bargeOpen() (instant 90°)
    // Do NOT add DoorVariant, dragResponseMultiplier, slamNoiseMultiplier, or any
    // per-door multipliers. Those fields are not part of the master plan design.
    //
    // DoorSpawn(id, localX, y, localZ, hingeOffsetX, hingeOffsetY, hingeOffsetZ, locked)
    //
    // SPAWN → CORRIDOR_1 door
    //   World position: (40.0, 0, 43.15)  →  local: (0.0, 0, 3.15)
    //   Hinge on the west side of the 1.2 m gap (X = 39.4 world → offset = −0.6)
    lm.doorSpawns.add(new DoorSpawn(
        "door-spawn-north",
         0.0f, 0.0f, 3.15f,   // localX, y, localZ  (world: 40, 0, 43.15)
        -0.6f, 1.05f, 0f,     // hingeOffsetX, hingeOffsetY, hingeOffsetZ
         false                 // locked
    ));

    // CORRIDOR_1 → CROUCH_ALCOVE door
    //   World position: (38.5, 0, 47.0)  →  local: (−1.5, 0, 7.0)
    //   Hinge on the south side of the 1.5 m gap (Z = 46.25 world → offset = +0.6 in Z)
    lm.doorSpawns.add(new DoorSpawn(
        "door-corridor-alcove",
        -1.5f, 0.0f, 6.25f,   // localX, y, localZ  (world: 38.5, 0, 46.25)
         0f,   1.05f, 0.6f,   // hingeOffsetX, hingeOffsetY, hingeOffsetZ
         false                 // locked
    ));

    // ─── Surface Zones ───────────────────────────────────────────────────────
    // Spawn room: concrete
    lm.surfaceZones.add(new SurfaceZone("surf_spawn",     0f, 0f,   0f,  7.0f, 0.2f, 6.0f,
        io.github.superteam.resonance.footstep.SurfaceMaterial.CONCRETE));
    // Corridor: metal grating
    lm.surfaceZones.add(new SurfaceZone("surf_corridor",  0f, 0f,  7.0f, 3.0f, 0.2f, 8.0f,
        io.github.superteam.resonance.footstep.SurfaceMaterial.METAL));
    // Dark room: wood
    lm.surfaceZones.add(new SurfaceZone("surf_darkroom",  0f, 0f, 15.5f, 9.0f, 0.2f, 9.0f,
        io.github.superteam.resonance.footstep.SurfaceMaterial.WOOD));
    // Crouch alcove: carpet (muffled)
    lm.surfaceZones.add(new SurfaceZone("surf_alcove", -5.5f, 0f, 7.0f,  6.0f, 0.2f, 3.0f,
        io.github.superteam.resonance.footstep.SurfaceMaterial.CARPET));

    // ─── Room Boundaries ─────────────────────────────────────────────────────
    lm.roomBoundaries.add(new RoomBoundaryDef(
        new BoundingBox(new Vector3(36.5f, -1f, 37f), new Vector3(43.5f, 4f, 43f)),
        "spawn_boundary"));
    lm.roomBoundaries.add(new RoomBoundaryDef(
        new BoundingBox(new Vector3(35.5f, -1f, 51f), new Vector3(44.5f, 4f, 60f)),
        "darkroom_boundary"));

    // ─── Dark Room Volume (for sanity drain) ─────────────────────────────────
    lm.darkRoomVolume = new BoundingBox(
        new Vector3(35.5f, -1f, 51.0f),
        new Vector3(44.5f,  4f, 60.0f));

    // ─── Patrol Waypoints ────────────────────────────────────────────────────
    lm.patrolWaypoints.add(new Vector3(40f, 0f, 53f));
    lm.patrolWaypoints.add(new Vector3(37f, 0f, 57f));
    lm.patrolWaypoints.add(new Vector3(43f, 0f, 57f));

    return lm;
}
```

---

## Step 2 — `UniversalTestScene.java`

### 2a. Add `buildBunkerWalls()` as a new private method

Insert this method anywhere in the private methods block of `UniversalTestScene`:

```java
/**
 * Builds individual wall, floor, and ceiling boxes for the horror bunker test map.
 * All coordinates are world-space. addBox(cx, cy, cz, width, height, depth, addCollider).
 * Wall thickness = 0.3 m. Floor slab center at Y = −0.05. Ceiling cap extends 0.3 m
 * beyond wall outer faces so corners are fully sealed.
 */
private void buildBunkerWalls() {

    // ════════════════════════════════════════════════════════════════════════
    // ROOM 1 — SPAWN ROOM
    // Interior: X[36.5, 43.5], Z[37.0, 43.0], ceiling Y = 3.0
    // ════════════════════════════════════════════════════════════════════════

    // Floor
    addBox(40.0f, -0.05f, 40.0f,   7.0f, 0.10f,  6.0f,  true);
    // Ceiling (0.3 m wider each side to seal wall tops)
    addBox(40.0f,  3.05f, 40.0f,   7.6f, 0.10f,  6.6f,  true);

    // West wall  (X = 36.5, outer face X = 36.2)
    addBox(36.35f, 1.50f, 40.0f,   0.3f, 3.00f,  6.0f,  true);
    // East wall  (X = 43.5, outer face X = 43.8)
    addBox(43.65f, 1.50f, 40.0f,   0.3f, 3.00f,  6.0f,  true);
    // South wall — solid, no opening  (Z = 37.0)
    addBox(40.0f,  1.50f, 36.85f,  7.6f, 3.00f,  0.3f,  true);

    // North wall — door opening 1.2 m wide centered at X = 40.0, 2.1 m tall
    //   Gap occupies X[39.4, 40.6]
    //   Left segment  X[36.5, 39.4]  width = 2.9  center X = 37.95
    addBox(37.95f, 1.50f, 43.15f,  2.9f, 3.00f,  0.3f,  true);
    //   Right segment X[40.6, 43.5]  width = 2.9  center X = 42.05
    addBox(42.05f, 1.50f, 43.15f,  2.9f, 3.00f,  0.3f,  true);
    //   Door header   above 2.1 m → top at 3.0 m → height = 0.9  center Y = 2.55
    addBox(40.0f,  2.55f, 43.15f,  1.2f, 0.90f,  0.3f,  true);

    // ════════════════════════════════════════════════════════════════════════
    // CORRIDOR 1
    // Interior: X[38.5, 41.5], Z[43.0, 51.0], ceiling Y = 2.8
    // Connects Spawn (south) to Dark Room (north) — both ends are open passages.
    // West wall has a 1.5 m door opening to Crouch Alcove centered at Z = 47.0
    // ════════════════════════════════════════════════════════════════════════

    // Floor
    addBox(40.0f, -0.05f, 47.0f,   3.0f, 0.10f,  8.0f,  true);
    // Ceiling
    addBox(40.0f,  2.85f, 47.0f,   3.6f, 0.10f,  8.6f,  true);

    // East wall — solid
    addBox(41.65f, 1.40f, 47.0f,   0.3f, 2.80f,  8.0f,  true);

    // West wall — split for alcove door opening  (Z gap: [46.25, 47.75], width = 1.5 m)
    //   South segment  Z[43.0, 46.25]  depth = 3.25  center Z = 44.625
    addBox(38.35f, 1.40f, 44.625f, 0.3f, 2.80f,  3.25f, true);
    //   North segment  Z[47.75, 51.0]  depth = 3.25  center Z = 49.375
    addBox(38.35f, 1.40f, 49.375f, 0.3f, 2.80f,  3.25f, true);
    //   Header above alcove door (door height = 1.55 m, corridor ceiling = 2.8 m)
    //   Header height = 2.8 − 1.55 = 1.25 m,  center Y = 1.55 + 0.625 = 2.175
    addBox(38.35f, 2.175f, 47.0f,  0.3f, 1.25f,  1.5f,  true);

    // South end — no wall needed; shares space with Spawn north wall
    // North end — no wall needed; open into Dark Room

    // ════════════════════════════════════════════════════════════════════════
    // ROOM 2 — CROUCH ALCOVE
    // Interior: X[32.5, 38.5], Z[45.5, 48.5], ceiling Y = 1.55
    // Entry on the east face via the door matched to the corridor opening above.
    // ════════════════════════════════════════════════════════════════════════

    // Floor
    addBox(35.5f, -0.05f, 47.0f,   6.0f, 0.10f,  3.0f,  true);
    // Ceiling (tight — only 1.55 m headroom)
    addBox(35.5f,  1.60f, 47.0f,   6.6f, 0.10f,  3.6f,  true);

    // West wall
    addBox(32.35f, 0.775f, 47.0f,  0.3f, 1.55f,  3.0f,  true);
    // North wall
    addBox(35.5f,  0.775f, 48.65f, 6.6f, 1.55f,  0.3f,  true);
    // South wall
    addBox(35.5f,  0.775f, 45.35f, 6.6f, 1.55f,  0.3f,  true);

    // East wall — door opening 1.5 m wide centered at Z = 47.0
    //   Gap Z[46.25, 47.75]
    //   South segment  Z[45.5, 46.25]  depth = 0.75  center Z = 45.875
    addBox(38.65f, 0.775f, 45.875f, 0.3f, 1.55f, 0.75f, true);
    //   North segment  Z[47.75, 48.5]  depth = 0.75  center Z = 48.125
    addBox(38.65f, 0.775f, 48.125f, 0.3f, 1.55f, 0.75f, true);
    // No header: alcove ceiling IS 1.55 m — door fills the full height.

    // ════════════════════════════════════════════════════════════════════════
    // ROOM 3 — DARK ROOM
    // Interior: X[35.5, 44.5], Z[51.0, 60.0], ceiling Y = 3.2
    // South face has an open 3 m passage aligned with Corridor 1 (X[38.5, 41.5]).
    // ════════════════════════════════════════════════════════════════════════

    // Floor
    addBox(40.0f, -0.05f, 55.5f,   9.0f, 0.10f,  9.0f,  true);
    // Ceiling
    addBox(40.0f,  3.25f, 55.5f,   9.6f, 0.10f,  9.6f,  true);

    // West wall
    addBox(35.35f, 1.60f, 55.5f,   0.3f, 3.20f,  9.0f,  true);
    // East wall
    addBox(44.65f, 1.60f, 55.5f,   0.3f, 3.20f,  9.0f,  true);
    // North wall — solid
    addBox(40.0f,  1.60f, 60.15f,  9.6f, 3.20f,  0.3f,  true);

    // South wall — 3 m passage opening aligned with corridor X[38.5, 41.5]
    //   Left segment   X[35.5, 38.5]  width = 3.0  center X = 37.0
    addBox(37.0f,  1.60f, 50.85f,  3.0f, 3.20f,  0.3f,  true);
    //   Right segment  X[41.5, 44.5]  width = 3.0  center X = 43.0
    addBox(43.0f,  1.60f, 50.85f,  3.0f, 3.20f,  0.3f,  true);
    // No door, no header — corridor ceiling (2.8) is lower than dark room (3.2)
    // so there is already a natural step; add a header only if you want to close it:
    // addBox(40.0f, 3.00f, 50.85f,  3.0f, 0.40f,  0.3f,  true); // optional header

    // ─── Pillars inside Dark Room (occlusion + acoustics) ───────────────────
    // Pillar SW
    addBox(37.5f, 1.60f, 53.5f,    0.6f, 3.20f,  0.6f,  true);
    // Pillar SE
    addBox(42.5f, 1.60f, 53.5f,    0.6f, 3.20f,  0.6f,  true);
    // Pillar center-north
    addBox(40.0f, 1.60f, 57.5f,    0.6f, 3.20f,  0.6f,  true);
}
```

---

### 2b. Modify `buildProceduralHubScene()`

Replace the **entire body** of this method with:

```java
private void buildProceduralHubScene() {
    // Thin world ground plane (catch-all below all rooms)
    addBox(HUB_X, -0.05f, HUB_Z, 96.0f, 0.1f, 96.0f, true);

    // Build layout instance (zones, doors, surfaces — no visual boxes needed)
    layoutInstance = TestMapLayout.buildDefaultLayoutInstance();

    // Build all walled bunker geometry (walls, floors, ceilings, pillars)
    buildBunkerWalls();

    // Optional: add zone volume visual helpers for debugging
    // (remove or keep; they are addCollider=false so they don't block movement)
    for (TestMapLayout.Zone z : layoutInstance.zones()) {
        float worldX = HUB_X + z.x;
        float worldZ = HUB_Z + z.z;
        addBox(worldX, z.y, worldZ, z.width, z.height, z.depth, false);
    }
}
```

---

## Step 3 — Player Spawn Position

In `UniversalTestScene` constructor, find the camera initial position line and update it:

```java
// Before (old hub-center spawn):
camera.position.set(HUB_X, PlayerController.CAPSULE_RADIUS, HUB_Z);
camera.lookAt(HUB_X + 1.0f, EYE_HEIGHT, HUB_Z);

// After (spawn room center, facing north toward corridor):
camera.position.set(40.0f, PlayerController.CAPSULE_RADIUS, 39.5f);
camera.lookAt(40.0f, EYE_HEIGHT, 43.0f);
```

---

## Reference: Room Summary Table

| Room | World X Range | World Z Range | Ceiling Y | Door(s) |
|------|--------------|--------------|-----------|---------|
| Spawn Room | 36.5 – 43.5 | 37.0 – 43.0 | 3.0 m | North wall, X=40, 1.2 m wide |
| Corridor 1 | 38.5 – 41.5 | 43.0 – 51.0 | 2.8 m | West wall, Z=47, 1.5 m wide |
| Crouch Alcove | 32.5 – 38.5 | 45.5 – 48.5 | 1.55 m | East wall, Z=47, 1.5 m wide |
| Dark Room | 35.5 – 44.5 | 51.0 – 60.0 | 3.2 m | South wall passage (no door) |

## How the Door System Actually Works (Master Plan §7)

> **Read this before touching any door code.** The fabricated `DoorVariant` enum (SLOW / FAST / BARGE as door *types*) does not exist in the design. Remove it.

Doors have **one type**. All behaviour differences come entirely from **how fast the player moves the mouse** and **whether the player is sprinting**:

| Player action | What happens | Class responsible |
|---|---|---|
| Aim at door + hold LMB, move mouse slowly | Quiet creak, door moves slowly | `DoorGrabInteraction` → `DoorCreakSystem` |
| Aim at door + hold LMB, move mouse fast | Loud creak, higher pitch | `DoorGrabInteraction` → `DoorCreakSystem` |
| Sprint directly into the door | Instant 90° slam-open, max noise | `DoorBargeDetector.check()` → `door.bargeOpen()` |

`DoorCreakSystem.updateDrag()` internally selects `creak_slow.wav`, `creak_medium.wav`, or `creak_fast.wav` based on normalised angular velocity (0–1). That is audio selection, **not a door variant**.

### What to delete from `TestMapLayout.java`

```java
// DELETE THIS ENUM — it is not in the master plan
public enum DoorVariant { SLOW, FAST, BARGE }

// DELETE the extended DoorSpawn constructor that takes:
//   DoorVariant variant, float dragResponseMultiplier,
//   float slamNoiseMultiplier, float interactNoiseMultiplier
// Keep ONLY the simple 8-argument constructor:
//   DoorSpawn(String id, float x, float y, float z,
//             float hingeOffsetX, float hingeOffsetY, float hingeOffsetZ,
//             boolean locked)
```

### What to leave alone in `DoorController.java`

The three multiplier fields (`dragResponseMultiplier`, `slamNoiseMultiplier`, `interactNoiseMultiplier`) and their setters were added outside the spec. Keep them at their default `1.0f` — do not set them from `DoorSpawn` and do not use them as a door-type switch. The door physics must respond only to live angular velocity, not to a preset multiplier.

---

## Reference: Door Spawn Summary

| Door ID | World position | Hinge offset | Locked |
|---------|---------------|-------------|--------|
| `door-spawn-north` | (40.0, 0, 43.15) | (−0.6, 1.05, 0) | false |
| `door-corridor-alcove` | (38.5, 0, 46.25) | (0, 1.05, 0.6) | false |

> Creak speed and slam intensity are determined at runtime by the player's mouse velocity and sprint state — not by any field on `DoorSpawn`.

---

## Validation Checklist

After implementation, verify:

- [ ] Player can walk from spawn north through `door-spawn-north` into Corridor 1
- [ ] Corridor west wall has a visible door opening at Z ≈ 47; `door-corridor-alcove` swings
- [ ] Crouch alcove requires crouching (`CROUCH_TUNNEL_CEILING = 1.4f`; alcove ceiling is 1.55 m — adjust constant if needed to `1.55f`)
- [ ] Dark room is reachable from corridor via open south passage
- [ ] Three pillars are present inside dark room and block line of sight
- [ ] No gaps in room corners (ceiling boxes are oversized by 0.3 m each side)
- [ ] `SurfaceZone` materials produce distinct footstep sounds per room (concrete, metal, wood, carpet)
- [ ] `darkRoomVolume` triggers sanity drain when player is inside Dark Room

### Sound Propagation Checks *(required — verify before marking complete)*

- [ ] `GraphPopulator.populate(worldColliders)` runs without error after `buildBunkerWalls()` completes
- [ ] Acoustic graph has nodes in **all four rooms**: spawn, corridor, crouch alcove, and dark room
- [ ] Crouch alcove relay box (`addBox(35.5f, 0.30f, 47.0f, 1.0f, 0.60f, 0.8f, true)`) is present and produces mid-room nodes — verify in the graph debug overlay
- [ ] Sound fired in spawn room propagates through the `door-spawn-north` gap into corridor at **full LOS intensity**
- [ ] Sound fired in spawn room is audible (at ~22% intensity) through the wall segments flanking `door-spawn-north` — confirms WOOD transmission edges are active
- [ ] Sound fired in corridor propagates into dark room through the open south passage at **full LOS intensity**
- [ ] Sound fired in corridor is audible (at ~22% intensity) through the corridor wall segments — confirms no graph dead zones along corridor length
- [ ] No room is acoustically isolated — every room must have at least one path (LOS or transmission) to every adjacent room
- [ ] Dark room pillar nodes are present at all three pillar positions and connect via LOS edges to surrounding wall nodes (max hop ≤ 4 m)

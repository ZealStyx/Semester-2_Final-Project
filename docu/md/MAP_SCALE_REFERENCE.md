# Resonance — Map Scale Reference & Bug Fixes

**Version:** v2 (2026-04-28)  
**Derived from:** `PlayerController.java` capsule constants  
**Applies to:** GLTF map geometry, prop models, test-scene zone boxes, collision shapes, runtime bug fixes

> **The map felt too short.** This document establishes the single authoritative scale
> that all map geometry, prop models, and zone definitions must follow.
> Model everything to these numbers — not "looks about right" in Blender.

---

## Bug Fixes (Apply These First)

These three bugs caused the immediate crash and the inability to walk on first launch. All fixes are drop-in replacements — no other files need changing.

---

### Fix 1 — `SpriteBatch.begin must be called before draw` Crash

**Crash location:** `SubtitleRenderer.render()` → `DialogueSystem.render()` → `UniversalTestScene.renderHudOverlays()` line 2111

**Root cause:**  
`dialogueSystem.render(hudBatch)` is called at line 2111 of `renderHudOverlays()`, *before* any `hudBatch.begin()`. The batch is not opened until `drawRuntimeSubtitle()` at line 2156. There is also a **duplicate call** at line 2133 inside the same method.

Neither `DialogueSystem` nor `SubtitleRenderer` guarded against being called while the batch was closed.

**Fix — `DialogueSystem.java`**

```java
/**
 * Renders the current subtitle line.
 * Safe to call whether or not batch is already drawing.
 * Uses batch.isDrawing() to decide whether to open/close the batch itself.
 */
public void render(SpriteBatch batch) {
    DialogueLine current = queue.peek();
    if (current == null) return;

    boolean wasDrawing = batch.isDrawing();
    if (!wasDrawing) batch.begin();
    renderer.render(batch, current.text);
    if (!wasDrawing) batch.end();
}
```

**Fix — `SubtitleRenderer.java`**

```java
public void render(SpriteBatch batch, String text) {
    if (text == null || text.isBlank()) return;

    layout.setText(font, text);
    float x = (Gdx.graphics.getWidth()  - layout.width)  * 0.5f;
    float y =  Gdx.graphics.getHeight() * 0.18f + layout.height;

    boolean wasDrawing = batch.isDrawing();
    if (!wasDrawing) batch.begin();

    // Drop-shadow for legibility
    font.setColor(0f, 0f, 0f, 0.65f);
    font.draw(batch, text, x + 1.5f, y - 1.5f);

    font.setColor(Color.WHITE);
    font.draw(batch, text, x, y);

    if (!wasDrawing) batch.end();
    font.setColor(Color.WHITE); // reset for other callers
}
```

**Also remove the duplicate call at line 2133** in `UniversalTestScene.renderHudOverlays()` — `dialogueSystem.render(hudBatch)` appears twice in that method. Keep only the one at line 2111.

---

### Fix 2 — Player Cannot Walk (Spawns Airborne)

**Symptom:** Player spawns, can look around, but WASD movement feels frozen or extremely sluggish (~8% speed).

**Root cause:**

```java
// UniversalTestScene constructor:
camera.position.set(HUB_X, EYE_HEIGHT, HUB_Z);   // camera Y = 1.6
playerController = new PlayerController(camera);   // position copied from camera

// PlayerController constructor:
this.position.set(camera.position);                // position.y = 1.6 ← WRONG

// Ground check in integrateMovement():
if (position.y <= CAPSULE_RADIUS) {                // 1.6 <= 0.3 → FALSE
    isGrounded = true;
} else {
    isGrounded = false;                            // ← player starts airborne!
}
```

`PlayerController.position` is the **foot** position. `camera.position` is the **eye** position (`EYE_HEIGHT = 1.6m` above the foot). Copying one into the other directly places the foot 1.6m above the floor. The ground check (`position.y <= 0.3`) is false, so `isGrounded = false`. Movement then uses `AIR_CONTROL_FACTOR = 0.08` — only 8% of normal walk speed — making it feel like the player cannot move at all.

**Fix — change the camera spawn Y in `UniversalTestScene`:**

```java
// BEFORE (wrong — camera at eye height, so PlayerController foot = 1.6m above floor):
camera.position.set(HUB_X, EYE_HEIGHT, HUB_Z);

// AFTER (correct — camera at foot height; updateCamera() adds EYE_HEIGHT on first frame):
camera.position.set(HUB_X, PlayerController.CAPSULE_RADIUS, HUB_Z);
```

This places the camera initially at `y = 0.3` (capsule sits on y=0 floor). On the first `updateCamera()` call, the camera is repositioned to `position.y + EYE_HEIGHT_STANDING = 0.3 + 1.6 = 1.9`. The player is immediately grounded and WASD works at full speed.

> **Important:** `CAPSULE_RADIUS` must be made `public static final` in `PlayerController` so `UniversalTestScene` can reference it without a magic number:
> ```java
> // PlayerController.java — change private to public static final:
> public static final float CAPSULE_RADIUS = 0.3f;
> ```

**Alternative fix (if you cannot touch UniversalTestScene):**  
Fix the `PlayerController` constructor to subtract eye height when deriving foot from camera:

```java
public PlayerController(PerspectiveCamera camera) {
    this.camera = camera;
    // Camera position is at eye level; derive foot position by subtracting eye height
    this.position.set(camera.position.x,
                      camera.position.y - EYE_HEIGHT_STANDING,
                      camera.position.z);
}
```

Use whichever approach fits your codebase — both are correct.

**Also check for the same issue in any other scene** that constructs `PlayerController`. Any call site that does:
```java
camera.position.set(x, EYE_HEIGHT, z);
new PlayerController(camera);
```
…has this bug.

---

### Fix 3 — Bullet `btRigidBodyConstructionInfo` GC Warnings

**Log output:**
```
[Bullet] Disposing btRigidBodyConstructionInfo(2739540249952,true) due to garbage collection.
[Bullet] Disposing btRigidBodyConstructionInfo(2739540251376,true) due to garbage collection.
... (many lines)
```

**Root cause:** `btRigidBodyConstructionInfo` objects are being created and then going out of scope before the JVM knows Bullet has native references to them. The GC collects them, triggering Bullet's finalizer warning.

**Fix:** Store construction info objects as fields (or in an array) for as long as the rigid body they created is alive. Never create them as local variables inside a loop or method and then discard.

```java
// WRONG — construction info goes out of scope after the method returns:
void addBox(...) {
    btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0, null, shape, Vector3.Zero);
    btRigidBody body = new btRigidBody(info);
    world.addRigidBody(body);
    // info is now eligible for GC → Bullet warning
}

// CORRECT — keep info alive as long as the body lives:
private final Array<btRigidBodyConstructionInfo> rigidBodyInfos = new Array<>();

void addBox(...) {
    btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0, null, shape, Vector3.Zero);
    rigidBodyInfos.add(info);   // keeps it alive
    btRigidBody body = new btRigidBody(info);
    world.addRigidBody(body);
}

// And in dispose():
for (btRigidBodyConstructionInfo info : rigidBodyInfos) info.dispose();
rigidBodyInfos.clear();
```

This is a memory hygiene issue, not a crash. The game runs despite these warnings, but it will cause undefined behaviour on some JVM/native configurations. Fix it alongside the other two.

---

## Player Capsule — Source of Truth

These are the live constants from `PlayerController.java`. All other measurements
in this document are derived from them.

| Constant | Value | Meaning |
|----------|-------|---------|
| `EYE_HEIGHT_STANDING` | **1.60 m** | Camera Y above foot origin (standing) |
| `EYE_HEIGHT_CROUCH` | **0.75 m** | Camera Y above foot origin (crouching) |
| `CAPSULE_RADIUS` | **0.30 m** | Half-width of the collision capsule |
| `CEILING_CHECK_MARGIN` | **0.05 m** | Buffer above eye before ceiling test fires |
| Crown height (derived) | **1.80 m** | `EYE_HEIGHT_STANDING` + crown gap (≈ 0.20 m) |
| Crouch crown (derived) | **1.00 m** | `EYE_HEIGHT_CROUCH` + crown gap (≈ 0.25 m) |
| Player diameter | **0.60 m** | `CAPSULE_RADIUS × 2` |

> **Why crown height matters:** The ceiling collision test fires at
> `EYE_HEIGHT_STANDING + CEILING_CHECK_MARGIN = 1.65 m`. Anything lower than
> **1.70 m** of clear headroom will cause the player to clip or jitter.
> Model all passable ceilings at **≥ 2.20 m** for comfortable feel.

---

## Recommended Map Dimensions

### Vertical (Y axis)

| Element | Min | Recommended | Notes |
|---------|-----|-------------|-------|
| **Standard ceiling** | 2.40 m | **2.80 m** | Office/corridor feel. Below 2.40 m feels oppressive. |
| **High ceiling** | 3.20 m | **3.50 m** | Server rooms, storage halls. Current test-scene zones use this. |
| **Crawl-space / vent** | 0.85 m | **0.90 m** | Player crouches (`EYE_HEIGHT_CROUCH = 0.75 m`); crown ≈ 1.00 m. Min clear height **≥ 1.05 m** |
| **Door frame height** | 2.00 m | **2.10 m** | Standard interior door. Never below 1.90 m or the camera clips the top. |
| **Window sill** | 0.90 m | **0.95 m** | Roughly desk height — player can lean over and see through. |
| **Desk / table surface** | 0.72 m | **0.75 m** | Items on desks should sit at this Y + item base offset. |
| **Shelf (low)** | 1.10 m | **1.20 m** | Reachable while standing, slightly above crouch eye level. |
| **Shelf (high)** | 1.60 m | **1.70 m** | At or just above eye level — player looks up slightly. |
| **Ceiling light hang** | ceiling − 0.15 m | ceiling − 0.10 m | Bulb dangles just below ceiling face. |

### Horizontal (X / Z axis)

| Element | Min | Recommended | Notes |
|---------|-----|-------------|-------|
| **Corridor width (1-way)** | 1.00 m | **1.20 m** | Player diameter = 0.60 m; need clearance each side. |
| **Corridor width (2-way)** | 1.60 m | **1.80 m** | Two capsules plus wall clearance. |
| **Door frame width** | 0.85 m | **0.90 m** | Standard single interior door. |
| **Room (small office)** | 3.0 × 3.0 m | **4.0 × 4.0 m** | Feels claustrophobic below 3 m. |
| **Room (medium)** | 5.0 × 5.0 m | **6.0 × 6.0 m** | Typical lab / server room bay. |
| **Wall thickness** | 0.15 m | **0.20 m** | Thin enough to not eat floor space; thick enough to look solid. |
| **Vent opening** | 0.70 × 0.70 m | **0.75 × 0.75 m** | Player crouches through; min clear = player diameter + margin. |

---

## Why The Map Felt Too Short

The most common cause of "rooms feel cramped" in a first-person game with these
player constants is a mismatch between **eye height** and **modelled ceiling height**:

```
  Ceiling at 2.40 m
  ─────────────────────── ← ceiling face
        0.80 m gap
  ─────────────────────── ← eye level (1.60 m)
        1.60 m
  ─────────────────────── ← floor (0.00 m)
```

A `0.80 m` gap above the eye looks almost like the player is ducking — the ceiling
feels oppressive. Compare to the recommended **2.80 m**:

```
  Ceiling at 2.80 m
  ─────────────────────── ← ceiling face
        1.20 m gap        ← comfortable overhead space
  ─────────────────────── ← eye level (1.60 m)
        1.60 m
  ─────────────────────── ← floor (0.00 m)
```

**Doors** are the second common culprit. If your door frame is modelled at 1.80 m,
the top of the frame is only **0.20 m above eye level**. The player clips through
the top of the frame during walk-bob (`RUN_BOB_AMPLITUDE = 0.05 m`).

---

## Blender / Modelling Checklist

Before exporting any room or prop to GLTF, verify in the outliner:

- [ ] 1 Blender unit = **1 metre** (check Scene Properties → Units → Unit Scale = 1.0)
- [ ] All doors: frame height ≥ **2.10 m**, frame width ≥ **0.90 m**
- [ ] All corridors: clear width ≥ **1.20 m**, clear height ≥ **2.40 m**
- [ ] All standard rooms: ceiling ≥ **2.80 m**
- [ ] Vents: clear opening ≥ **0.75 × 0.75 m**
- [ ] Floor at **Y = 0.00** (player spawn and physics ground plane are at Y = 0)
- [ ] No geometry below Y = 0 (except decorative exterior)
- [ ] Export forward axis: **−Z forward, +Y up** (LibGDX / GLTF convention)

---

## TestMapLayout Zone Corrections

Current `TestMapLayout.java` zones use **height = 3.5 m** throughout, which is
acceptable for a test environment but slightly oversized for a realistic horror setting.
The corridor zone `BREATH_CORRIDOR` at `4 m` deep × `3.5 m` high reads as a wide
tunnel rather than a tight squeeze. Suggested values if you want it to feel tighter:

| Zone | Current H | Suggested H | Reason |
|------|-----------|-------------|--------|
| `SPAWN` | 3.50 m | **2.80 m** | Reception area — standard office |
| `DOOR_TEST` | 3.50 m | **3.00 m** | Door demo room — slightly high |
| `FOOTSTEP_STRIP` | 3.50 m | **2.80 m** | Corridor — should feel narrow |
| `DARK_ROOM` | 3.50 m | **3.50 m** | Keep — darkness works better in tall rooms |
| `BREATH_CORRIDOR` | 3.50 m | **2.40 m** | Should feel claustrophobic |
| `ENEMY_PATROL_ROOM` | 3.50 m | **3.20 m** | Slightly imposing but not cavernous |
| `CROUCH_TUNNEL` | — | **1.05 m** | Vent/crawl-space — force crouch |

> These are suggestions only. If the GLTF map is being built externally and imported,
> follow the GLTF geometry — the test-scene boxes should match it exactly.

---

## Prop Scale Reference

| Prop | Model height | Hinge Y (model-local) | Knob Y (model-local) |
|------|--------------|-----------------------|----------------------|
| Office door | 2.10 m | 0.00 m | 0.92 m |
| Cabinet door | 1.80 m | 0.00 m | 0.90 m |
| Filing drawer | 0.22 m (one drawer) | — | 0.11 m |
| Ceiling vent cover | 0.60 × 0.60 m | 0.00 m | −0.25 m (below) |
| Ceiling light bulb | 0.18 m (bulb only) | — | — |
| Metal detector gate | 2.10 m tall × 0.80 m wide | — | — |

---

## Code Constants to Adjust (if ceiling still feels low)

If, after correcting the GLTF geometry, the player still clips the ceiling during
head-bob, raise the eye height slightly:

```java
// PlayerController.java
private static final float EYE_HEIGHT_STANDING = 1.65f;  // was 1.60f — more realistic
private static final float EYE_HEIGHT_CROUCH   = 0.80f;  // was 0.75f — slight lift
```

And tighten the ceiling check to give the bob more room:

```java
private static final float CEILING_CHECK_MARGIN = 0.08f; // was 0.05f
```

> **Do not raise `EYE_HEIGHT_STANDING` above 1.70 m** without also raising
> `CAPSULE_RADIUS` or adding an explicit head-check ray — the capsule top may
> not cover the head at that height.

---

*Keep this file updated whenever `PlayerController` capsule constants change.*

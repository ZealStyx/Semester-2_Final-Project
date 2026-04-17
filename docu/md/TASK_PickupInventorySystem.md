# Item Pickup & Inventory System — Task Plan
**Project:** Resonance | LibGDX  
**Course:** CSEL 302 | Laguna State Polytechnic University | AY 2025–2026

---

## Overview

This plan covers the full design and implementation of the physics-based item carry system and the player's 4-slot inventory. Items can be picked up, carried with spring physics, thrown, and smashed into the environment — each collision feeding directly into the `AcousticGraphEngine` to produce sound events that propagate through the Dijkstra graph and alert enemies. Inventory slots hold consumables and tools the player collects throughout the level.

---

## System Map

```
PlayerController
      │
      ├── CarrySystem          ← spring physics, throw, smash
      │       └── CarriableItem
      │               └── ImpactListener ──► AcousticGraphEngine
      │                                             │
      │                                     Dijkstra Propagation
      │                                             │
      │                                       EnemyBrain / Director
      │
      └── InventorySystem      ← 4 slots, pickup, use, drop
              └── ItemDefinition (type, noise multiplier, break threshold)
```

---

## Phase 0 — Item Foundation

**Goal:** Define the data contract that every item in the game shares before any system is built on top of it.

### ItemType Enum

```java
public enum ItemType {
    // Carriable physics objects
    METAL_PIPE,
    CARDBOARD_BOX,
    GLASS_BOTTLE,
    CONCRETE_CHUNK,

    // Inventory consumables / tools
    FLARE,
    BATTERY_CELL,
    NOISE_DECOY,
    KEY
}
```

### ItemDefinition Class

Each item in the world carries one of these either as a component or a config reference:

| Field | Type | Description |
|-------|------|-------------|
| `itemType` | `ItemType` | Category |
| `displayName` | `String` | Shown in HUD slot |
| `noiseMultiplier` | `float` | Scales collision sound intensity (metal = 1.4, cardboard = 0.3) |
| `breakThreshold` | `float` | Impulse value above which the item shatters |
| `throwStrength` | `float` | Impulse magnitude applied on throw |
| `mass` | `float` | Physics body mass — heavier items lag behind the carry point more |
| `isCarriable` | `boolean` | Can it be held and thrown |
| `isConsumable` | `boolean` | Goes into inventory, used with E |
| `stackable` | `boolean` | Multiple copies share one slot |

### Objectives
- [ ] `ItemType` enum defined and committed.
- [ ] `ItemDefinition` class created with all fields and a static factory method per type.
- [ ] `CarriableItem` wrapper class exists, holds a reference to `ItemDefinition` and a `btRigidBody`.
- [ ] At least three items configured with distinct `noiseMultiplier` values.

---

## Phase 1 — Carry System (Spring Physics)

**Goal:** Let the player pick up and hold a carriable item using spring forces that make it feel physical and weighty.

### How It Works

An invisible **carry point** is computed each frame in front of the camera. A spring force pulls the held item toward that point every physics tick. Because it uses forces — not teleportation — the item lags, wobbles, and collides with the environment naturally.

```
carryPoint = cameraPosition + cameraForward * carryDistance
error      = carryPoint - item.worldPosition
spring     = error * springStrength
damper     = -item.linearVelocity * damping
item.applyCentralForce(spring + damper)
```

### Requirements

#### 1.1 Carry Point
- Calculated every frame from `PerspectiveCamera` position and direction.
- `carryDistance` default 2.0 m; adjustable via mouse scroll wheel (range 1.0–3.5 m).
- No GameObject or physics body at the carry point — it is a pure `Vector3`.

#### 1.2 Spring Constants

| Constant | Default Value | Effect |
|----------|--------------|--------|
| `springStrength` | 300f | Higher = snappier follow |
| `damping` | 15f | Higher = less wobble |
| `rotationDamping` | 8f | How fast spin settles |
| `maxCarryForce` | 500f | Clamp to prevent tunnelling |

- All constants exposed as tunable fields — do not hardcode.
- Force is clamped to `maxCarryForce` before applying so heavy items cannot tunnel through walls.

#### 1.3 Pickup
- On G pressed, raycast from camera, range 3.0 m, hits `CarriableItem` layer.
- If hit and `CarrySystem.heldItem == null`: begin carry.
- On pickup: `rigidBody.setActivationState(DISABLE_DEACTIVATION)` — prevents Bullet from sleeping the body mid-carry.
- Reduce gravity influence: `rigidBody.setGravityFactor(0.2f)` so the spring can win.
- Store the item reference in `heldItem`.

#### 1.4 Release / Drop
- On G pressed again (toggle) or Tab to slot it into inventory (see Phase 3).
- Restore `gravityFactor` to 1.0.
- Re-enable deactivation: `rigidBody.setActivationState(WANTS_DEACTIVATION)`.
- Item inherits current velocity from spring movement — natural toss if moving fast.

#### 1.5 Rotation Handling
- Apply torque each frame to drive angular velocity toward zero (stabilise the item).
- Allow partial tumble — do not lock rotation completely.
- `angularDamp` applied via `rigidBody.setDamping(linearDamp, angularDamp)` on pickup.

### Objectives
- [ ] Item visibly follows the camera carry point with a spring lag.
- [ ] Heavier items (`mass` > 5 kg) lag noticeably more than light items.
- [ ] Scroll wheel adjusts carry distance smoothly without snapping.
- [ ] Item collides with walls and floors while being carried — no ghost-through.
- [ ] Gravity is suppressed while carried; restored instantly on drop.
- [ ] Bullet deactivation disabled on pickup, re-enabled on drop.
- [ ] No item can be carried if `CarrySystem.heldItem` is already occupied.

---

## Phase 2 — Throw & Smash

**Goal:** Let the player throw the held item with directional force, and detect when it impacts the environment hard enough to matter.

### Requirements

#### 2.1 Throw
- Right mouse button while carrying: release item and apply throw impulse.
- Impulse direction: `cameraForward * throwStrength + Vector3Y * 1.5f` (slight upward arc).
- `throwStrength` sourced from the item's `ItemDefinition` — heavier items throw slower by default unless the player winds up (optional).
- Item immediately exits carry state before impulse is applied.

#### 2.2 Impact Detection — Contact Listener

Register a `ContactListener` on the Bullet dynamics world:

```java
new ContactListener() {
    @Override
    public boolean onContactAdded(btManifoldPoint cp,
                                  btCollisionObject obj0, ...,
                                  btCollisionObject obj1, ...) {

        float impulse = cp.getAppliedImpulse();

        // Only process if one object is a CarriableItem
        if (!isCarriable(obj0) && !isCarriable(obj1)) return false;
        if (impulse < MIN_IMPACT_THRESHOLD) return false;

        Vector3 hitPos   = cp.getPositionWorldOnA(tmp);
        float intensity  = mapImpulseToIntensity(impulse, item.noiseMultiplier);

        acousticGraphEngine.emitSoundEvent(hitPos, intensity, SoundEventType.OBJECT_HIT);

        if (impulse > item.breakThreshold)
            scheduleBreak(item);   // defer destruction outside physics callback

        return false;
    }
};
```

#### 2.3 Impulse to Intensity Mapping

```java
float mapImpulseToIntensity(float impulse, float noiseMult) {
    float min     = 5f;
    float max     = 150f;
    float clamped = MathUtils.clamp(impulse, min, max);
    float base    = (clamped - min) / (max - min);   // 0.0 → 1.0
    return MathUtils.clamp(base * noiseMult, 0f, 1f);
}
```

#### 2.4 Item Breaking
- If impulse exceeds `breakThreshold`, play a break effect and remove the physics body.
- Emit `SoundEventType.PHYSICS_COLLAPSE` at full intensity (enemies react strongly).
- Broken items are removed from `heldItem` if still carried when shattered.
- Destruction is deferred to the next frame — never destroy a body inside a physics callback.

#### 2.5 Noise Multiplier by Material

| Item | `noiseMultiplier` | Notes |
|------|-------------------|-------|
| Metal Pipe | 1.4 | Loudest carriable |
| Glass Bottle | 1.2 | Breaks easily, loud shatter |
| Concrete Chunk | 1.0 | Baseline |
| Cardboard Box | 0.3 | Near-silent, safe to carry |

#### 2.6 Camera Shake on Impact
- On any impact with impulse > `shakeTreshold` (default 20f), trigger camera shake.
- Shake magnitude proportional to impulse: `shakeAmount = impulse * 0.002f`, capped at 0.3.
- Duration fixed at 0.15 s; implemented as a positional offset on the camera root.

### Objectives
- [ ] Thrown item travels in the correct direction with visible arc.
- [ ] `ContactListener` only fires for `CarriableItem` collisions — not every physics contact.
- [ ] Low-speed touches produce no event; hard impacts produce loud events.
- [ ] Glass bottle shatters at its lower `breakThreshold`; metal pipe survives most hits.
- [ ] `PHYSICS_COLLAPSE` event reaches `AcousticGraphEngine` and logs correctly.
- [ ] Camera shake triggers on hard impact, scales with impulse, does not loop.
- [ ] Item destruction never crashes — deferred correctly outside the callback.

---

## Phase 3 — Inventory System (4 Slots)

**Goal:** Give the player four inventory slots for consumables and tools, selectable by keyboard.

### Slot Layout

```
[ Slot 1 ]  [ Slot 2 ]  [ Slot 3 ]  [ Slot 4 ]
  Key: 1      Key: 2      Key: 3      Key: 4
```

- Only one slot active at a time (highlighted in HUD).
- Active slot cycles with Q / scroll wheel.
- Items in slots are **not** physics objects while stored — they are data only.

### Requirements

#### 3.1 InventorySystem Class

```java
public class InventorySystem {
    private static final int SLOT_COUNT = 4;
    private ItemDefinition[] slots = new ItemDefinition[SLOT_COUNT];
    private int activeSlot = 0;

    public boolean addItem(ItemDefinition item) { ... }  // returns false if all slots full
    public ItemDefinition removeItem(int slot)   { ... }
    public ItemDefinition getActive()            { ... }
    public void setActiveSlot(int index)         { ... }
    public boolean isFull()                      { ... }
}
```

#### 3.2 Picking Up into Inventory
- When the player presses G on a **consumable** item (not carriable), it goes directly into the first free inventory slot.
- If all 4 slots are full: show a brief HUD message "Inventory Full" — do not pick up.
- Carriable items picked up into the **carry system** first; pressing Tab while holding one slots it into inventory if a free slot exists (item dematerialises from physics world).

#### 3.3 Using Items (E Key)
- E uses the item in the active slot.
- Each `ItemDefinition` has a `use()` callback or the system dispatches based on `ItemType`:

| ItemType | Use Effect |
|----------|-----------|
| `FLARE` | Spawns a light source at player position; emits medium sound event |
| `BATTERY_CELL` | Restores a charge resource (for future flashlight system) |
| `NOISE_DECOY` | Throws a noise-emitting object; emits `SoundEventType.DECOY` at target position |
| `KEY` | Auto-used when player interacts with a matching locked door |

- After use, item is removed from the slot (consumed).
- Stackable items (`stackable = true`) decrement a count instead of clearing the slot.

#### 3.4 Dropping Items (Q while slot active)
- Spawns the item's physics body at the player's position with zero initial velocity.
- Removes it from the inventory slot.
- Spawned item is immediately interactable by the player again.

#### 3.5 Slot Selection
- Keys 1–4 select slots directly.
- Mouse scroll and Q cycle through slots.
- Active slot index wraps around (slot 4 → slot 1).

### Objectives
- [ ] All 4 slots independently store different items.
- [ ] `addItem` returns false and shows HUD warning when full.
- [ ] E key uses the active slot item with correct effect per `ItemType`.
- [ ] Noise Decoy emits a sound event that reaches `AcousticGraphEngine`.
- [ ] Tab while carrying slots the item and removes its physics body from the world.
- [ ] Q drops the active slot item and spawns a physics body at player position.
- [ ] Slot selection via 1–4 and scroll wheel all work correctly.
- [ ] Stackable items show a count in the HUD slot and decrement on use.

---

## Phase 4 — Acoustic Integration

**Goal:** Ensure every item interaction feeds the correct sound event type and intensity into the existing Resonance pipeline.

### Sound Event Map

| Action | `SoundEventType` | Intensity Range | Notes |
|--------|-----------------|----------------|-------|
| Item carried, bumps wall gently | `OBJECT_HIT` | 0.05–0.2 | Low impulse contact |
| Item thrown, hard wall impact | `OBJECT_HIT` | 0.4–0.8 | High impulse |
| Item shatters | `PHYSICS_COLLAPSE` | 0.9–1.0 | Always near max |
| Noise Decoy used | `DECOY` | 0.7 (fixed) | Thrown to a target position |
| Flare deployed | `OBJECT_DROP` | 0.3 (fixed) | Light clatter on landing |

### Requirements
- Every `emitSoundEvent` call includes the **world position of the impact**, not the player position.
- `AcousticGraphEngine` receives all events regardless of whether the enemy is nearby — propagation range is handled by the graph, not the emitter.
- No event is emitted for impulses below `MIN_IMPACT_THRESHOLD` (5.0f) — prevents spam from micro-vibrations.
- The `DECOY` event position is the landing point of the thrown decoy, not where the player stands.

### Noise Budget Implications for Director AI

The K-Means Director reads player behaviour through the feature extractor. Item usage adds a secondary pressure signal:

- A player who smashes items frequently will generate high `PANIC`-correlated events.
- A player who carries items quietly (cardboard box, slow walk) stays in `STEALTH`.
- A player who uses a Noise Decoy deliberately shows tactical awareness — the Director can treat this as a `STEALTH` indicator and tighten enemy behaviour instead of relaxing it.

This mapping does not require new code — it emerges from the existing K-Means pipeline once the acoustic events flow correctly.

### Objectives
- [ ] All five event types reach `AcousticGraphEngine` in the correct scenario.
- [ ] Impact position is the contact point, not the player or camera position.
- [ ] Micro-vibration spam is suppressed by the threshold check.
- [ ] Decoy landing position correctly computed and passed to the engine.
- [ ] Console log shows event type, intensity, and position for each emission during testing.

---

## Phase 5 — HUD Integration

**Goal:** Show the carry state and inventory slots on screen so the player has clear feedback.

### HUD Elements

#### Carry Indicator
- Small crosshair dot that changes colour when a carriable item is in range (white → yellow).
- Disappears when already holding an item.
- Shows item name as a brief tooltip on pickup (fades after 1.5 s).

#### Inventory Bar
```
┌─────────┬─────────┬─────────┬─────────┐
│  Flare  │ Decoy×2 │  [Key]  │  empty  │
│   [1]   │   [2]   │   [3]   │   [4]   │
└─────────┴─────────┴─────────┴─────────┘
```
- Active slot has a highlighted border.
- Stackable items show count in the bottom-right of the slot.
- Empty slots show a dim outline only.
- "Inventory Full" message appears above the bar for 2 s when pickup is rejected.

#### Carry State Overlay
- When holding an item, show its name and a small icon in the top-left corner.
- No crosshair while holding — replace with the item name.

### Objectives
- [ ] Crosshair changes colour correctly when in range of a carriable item.
- [ ] Inventory bar renders all 4 slots, highlights active slot.
- [ ] Slot updates immediately on pickup, use, and drop.
- [ ] Stack count renders correctly and decrements on use.
- [ ] "Inventory Full" message appears and auto-dismisses.
- [ ] Item name shows in top-left while carrying.

---

## Phase 6 — Testing & Validation

### Unit Tests

| ID | Component | Input | Expected |
|----|-----------|-------|----------|
| PC01 | `CarrySystem.pickup` | G on carriable item in range | `heldItem` set, gravity reduced |
| PC02 | `CarrySystem.pickup` | G on item out of range (4 m) | No pickup, `heldItem` null |
| PC03 | `CarrySystem` | Hold item, walk into wall | Item collides, does not ghost through |
| PC04 | `mapImpulseToIntensity` | Impulse = 5f, mult = 1.0 | Returns 0.0 |
| PC05 | `mapImpulseToIntensity` | Impulse = 150f, mult = 1.4 | Returns 1.0 (clamped) |
| PC06 | `mapImpulseToIntensity` | Impulse = 77.5f, mult = 1.0 | Returns ~0.5 |
| PC07 | `InventorySystem.addItem` | Add to empty inventory | Returns true, slot 0 filled |
| PC08 | `InventorySystem.addItem` | Add when all 4 slots full | Returns false, no slot changed |
| PC09 | `InventorySystem.removeItem` | Remove from slot 2 | Slot 2 null, item returned |
| PC10 | `InventorySystem` use `NOISE_DECOY` | E key, active = Decoy | Sound event emitted, slot cleared |
| PC11 | `ContactListener` | Carriable hits wall at high speed | `OBJECT_HIT` event emitted |
| PC12 | `ContactListener` | Non-carriable hits wall | No sound event emitted |
| PC13 | `CarrySystem` throw | RMB while holding | Item released, impulse applied forward |
| PC14 | Item break | Impulse > `breakThreshold` | Item removed, `PHYSICS_COLLAPSE` emitted |
| PC15 | Tab to slot | Tab while carrying, slot free | Item enters inventory, physics body removed |
| PC16 | Tab to slot | Tab while carrying, inventory full | Item stays in carry, HUD shows full message |

### Manual Validation Checklist
- [ ] Pick up a metal pipe, carry it through a corridor — it bumps walls and produces events.
- [ ] Throw a glass bottle at a wall hard — it shatters, `PHYSICS_COLLAPSE` event fires, enemies react.
- [ ] Carry a cardboard box at slow-walk — near-silent, no enemy response.
- [ ] Fill all 4 inventory slots, try to pick up a 5th item — "Inventory Full" appears.
- [ ] Use a Noise Decoy — event fires at the landing position, not the player position.
- [ ] Drop an inventory item — physics body spawns at player feet, can be picked up again.
- [ ] Scroll wheel changes carry distance visibly.
- [ ] Camera shakes on hard impact, does not shake on gentle contact.

---

## Deliverables

| File | Package | Description |
|------|---------|-------------|
| `ItemType.java` | `resonance.items` | Item category enum |
| `ItemDefinition.java` | `resonance.items` | Data class for all item properties |
| `CarriableItem.java` | `resonance.items` | Physics wrapper, holds `btRigidBody` |
| `CarrySystem.java` | `resonance.player` | Spring physics carry, throw, drop |
| `ImpactListener.java` | `resonance.player` | Bullet contact listener, impulse → sound event |
| `InventorySystem.java` | `resonance.player` | 4-slot inventory, use, drop, slot selection |
| `InventoryHUD.java` | `resonance.ui` | LibGDX Scene2D inventory bar rendering |
| `CarryHUD.java` | `resonance.ui` | Carry crosshair and item name overlay |

---

## Dependencies & Blockers

- **Blocks:** Enemy reaction testing — enemies cannot be validated without sound events flowing from item impacts.
- **Blocks:** Noise Decoy tactical testing — Director AI STEALTH/PANIC balance depends on this emitting correctly.
- **Blocked by:** `AcousticGraphEngine.emitSoundEvent()` stub must exist (Player System Phase 5).
- **Blocked by:** `btDynamicsWorld` and Bullet physics configured in the project (Player System Phase 0).
- **External:** `SoundEventType.OBJECT_HIT`, `PHYSICS_COLLAPSE`, and `DECOY` must be added to the existing `SoundEventType` enum if not already present.

---

*Resonance | CSEL 302 Final Project | Laguna State Polytechnic University*

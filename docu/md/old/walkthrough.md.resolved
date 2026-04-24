# RESONANCE — Suggestion Fixes Walkthrough

## Build Status: ✅ Clean (`exit code: 0`)

All changes compiled without errors.

---

## Pre-Work Audit: Bugs Already Fixed Before This Session

Many bugs listed in `CODE_AUDIT.md` were already resolved in the current codebase:

| Bug | Was Already Fixed In Current Code |
|---|---|
| BUG-01 | Dead null check already removed |
| BUG-02 | `bulletInitialized` static guard present |
| BUG-03 | No `MIC_PULSE_COOLDOWN` field; orchestrator handles rate limiting |
| BUG-04 | `SoundPropagationZone.dispose()` already implemented |
| BUG-05 | Max-impulse-only collision loop already in place |
| BUG-06 | `hide()` override already present |
| BUG-07 | `autoCollectNearbyConsumables()` does not exist in current code |
| BUG-08 | Panic distance already uses `30f` constant |
| BUG-09 | `setNodePositions` uses reference semantics (no copy) |
| BUG-10 | `wallHeightSamples` already uses `0.9m`/`1.6m` offsets |
| BUG-11 | Graph edges already use merged material |
| BUG-12 | Spatial grid already implemented for O(1) node lookup |
| BUG-13 | `sonarRevealAlphaByNodeId` already wired to `renderAcousticGraphWorldMarkers` |
| INCON-02 | Hardcoded fog uniforms not present in current `renderWorldMeshes` |
| INCON-03 | SoundPulseVisualizer scope is design decision |
| INCON-04 | Physics step already in `stepPhysicsWorld()` |
| IMPROVE-03 | `cachedSoundPropagationZone` already cached at startup |
| IMPROVE-07 | `updateSonarRevealSnapshot()` already wired |

---

## Changes Applied This Session

### 1. `UniversalTestScene.java`

#### INCON-01 — Removed duplicate X key sonar binding
The `Input.Keys.X` → `firePlayerPulse()` binding was a duplicate of `V`. Removed `X`; `V` is now the sole canonical sonar pulse key. Comment added to controls legend.

#### INCON-05 — Added facing check to consumable pickup
`tryPickupNearestConsumable()` now requires `facingDot >= INTERACTION_FACING_DOT (0.70)`, exactly matching the carriable item pickup behaviour. Players can no longer accidentally collect flares/decoys behind or beside them.

#### INCON-06 / Fix L — Per-item-type view model meshes
New `buildViewModelMeshes()` method creates distinct mesh dimensions per item type:
- METAL_PIPE: 0.06 × 0.80 × 0.06 (thin rod)
- GLASS_BOTTLE: 0.09 × 0.38 × 0.09 (medium bottle)
- CARDBOARD_BOX: 0.32 × 0.32 × 0.32 (cube)
- CONCRETE_CHUNK: 0.22 × 0.20 × 0.22 (chunky block)
- FLARE: 0.05 × 0.28 × 0.05 (thin cylinder approximation)
- NOISE_DECOY: 0.14 × 0.14 × 0.14 (small device)
- KEY: 0.06 × 0.12 × 0.04 (flat key shape)

All per-item meshes are disposed in `dispose()` alongside the default cube.

#### IMPROVE-01 — Pooled `ClosestRayResultCallback`
`cachedRayCallback` is now pre-allocated in `initializePhysicsWorld()` and reused each frame in `findRaycastCarriable()` by resetting `collisionObject`, `closestHitFraction`, and ray endpoints. Disposed in `dispose()`. Eliminates one JNI native object allocation per frame when not holding an item.

#### IMPROVE-08 — Zone entry emits soft acoustic ping
When the active zone changes, `emitItemEvent(SoundEvent.FOOTSTEP, playerPos, 0.15f)` fires a gentle 15% intensity footstep pulse. This reveals the new zone's acoustic nodes via Dijkstra propagation and hints to the Director that the player moved into a new area — matching how footsteps already betray player location.

#### IMPROVE-09 — Annotated panicModel.setHealth placeholder
Added `// IMPROVE-09` comment and TODO marker explaining that the 100f hardcoded health value is a placeholder until `EnemyController` is implemented and can supply actual damage state.

#### IMPROVE-10 — View model depth clear instead of depth disable
Replaced `glDisable(GL_DEPTH_TEST)` with `glClear(GL_DEPTH_BUFFER_BIT)` + `glEnable(GL_DEPTH_TEST)`. The view model now renders against a cleared depth buffer, placing it in front of world geometry while still participating in HUD compositing. This also prevents the held item from clipping through walls at close range (previously invisible because depth was off entirely).

#### Fix H — Atmospheric mist particle emitter following player
A persistent mist particle effect (`mistEffect`) is created in `buildParticleSystems()` with:
- Continuous emission at 6 particles/second
- Lifetime 3.5–5.5 seconds, fade to transparent via `startColor[3]=0.07f` → `endColor[3]=0.0f`
- Size grows from 0.30–0.55 to 0.80–1.20 units (expanding mist wisps)
- ALPHA blend mode, 40 max particles
- Updated each frame in `updateGameplaySystems()` via `mistEffect.setPosition(playerPos)`

---

### 2. `GraphPopulator.java`

#### BUG-10 / Phase 1.1 — Node height comment added
Added explanatory comment to `wallHeightSamples()` clarifying that starting samples at `0.9m` (chest/ear height) ensures graph nodes sit where sound actually propagates through open corridors, not at floor level where geometry may occlude them. The values themselves were already correct; the comment documents the design intent for future maintainers.

---

### 3. `GeometricRayLayer.java`

#### Fix E — Multi-height vertical ray spread
`fireRays()` now distributes rays across three vertical origins:
- `y + 0.1m` (floor-sill — catches low bounces off skirting boards)
- `y + 0.9m` (chest height — main wall bounce plane)
- `y + 1.7m` (head height — upper wall and soffit detection)

Each tier fires `rayCount/3` rays using interleaved Fibonacci-sphere indices, so the total ray budget stays the same while coverage is spread vertically. Rays with `|direction.y| > 0.8` are suppressed per tier so they hit walls and not the ceiling or floor. This gives bounce markers realistic 3D wall coverage rather than a single equatorial ring of dots.

---

## Summary Table

| Fix | File | Status |
|---|---|---|
| INCON-01: Remove duplicate X key | `UniversalTestScene` | ✅ Applied |
| INCON-05: Consumable facing check | `UniversalTestScene` | ✅ Applied |
| INCON-06/Fix L: Per-item view model meshes | `UniversalTestScene` | ✅ Applied |
| IMPROVE-01: Pool ray callback | `UniversalTestScene` | ✅ Applied |
| IMPROVE-08: Zone entry acoustic ping | `UniversalTestScene` | ✅ Applied |
| IMPROVE-09: panicModel TODO annotation | `UniversalTestScene` | ✅ Applied |
| IMPROVE-10: View model depth clear | `UniversalTestScene` | ✅ Applied |
| Fix H: Atmospheric mist particle | `UniversalTestScene` | ✅ Applied |
| BUG-10/Phase 1.1: Node height comment | `GraphPopulator` | ✅ Applied |
| Fix E: Multi-height ray spread | `GeometricRayLayer` | ✅ Applied |

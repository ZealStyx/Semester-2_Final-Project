# RESONANCE — Suggestion Fixes Implementation Plan

## Summary

Applying all pending bug fixes and improvements identified in `CODE_AUDIT.md` and `PROJECT_PROGRESS_REPORT.md`. Many bugs listed in the audit are **already fixed** in the current codebase. Below is the real delta — what still needs work.

## Already Fixed (No Action Required)

| Bug | Status in Current Code |
|---|---|
| BUG-02 (Bullet.init) | Static guard `bulletInitialized` present at line 524 ✅ |
| BUG-04 (ShapeRenderer dispose) | `dispose()` already implemented in SoundPropagationZone ✅ |
| BUG-05 (4x Dijkstra per collision) | Max-impulse-only loop already implemented ✅ |
| BUG-06 (hide() mic leak) | `hide()` override already present at line 2184 ✅ |
| BUG-07 (autoCollect) | Method does not exist in current code ✅ |
| BUG-08 (panic distance from hub) | Already uses `30f` constant ✅ |
| BUG-09 (node deep-copy every frame) | Already uses `setNodePositions` reference semantics ✅ |
| BUG-12 (findNearestNodeId O(n)) | Spatial grid already implemented ✅ |
| INCON-03 (SoundPulseVisualizer unused) | Design decision, no action needed ✅ |
| INCON-04 (physics step in zone method) | Already extracted to `stepPhysicsWorld()` ✅ |
| IMPROVE-03 (findSoundPropagationZone cache) | `cachedSoundPropagationZone` already cached ✅ |

## Remaining Fixes to Apply

### Group A — Bug Fixes (UniversalTestScene.java)

#### BUG-01 — Dead null check on `frame`
`handleMicInput` already has the correct form in the current code (`!frame.shouldEmitSignal()` with no null part). **ALREADY FIXED** — skip.

#### BUG-03 — MIC cooldown asymmetry  
The code no longer has `MIC_PULSE_COOLDOWN`. The orchestrator's `0.08s` cooldown (from config) handles rate limiting uniformly. **ALREADY FIXED** — skip.

#### BUG-10 — GraphPopulator fallback at world origin
The fallback at line 31 of `GraphPopulator` already passes `40f, 1.2f, 40f` to `createEmergencyGraph`. The `estimateCollidersCenter` correctly computes from active colliders. However, the graph node height is at `y=0.05` when it should be `y=0.9`. **FIX:** Change `wallHeightSamples` to use `minY + 0.9f` as the minimum sample height so nodes are at ear/chest level.

#### BUG-11 — All graph edges hardcoded to CONCRETE
Fix is partially applied (the path now uses `mergePathMaterial`). The edge-between case is correctly using merged path material. **ALREADY FIXED.**

#### BUG-13 — SonarRevealView.snapshot() never called
Wire `sonarRevealAlphaByNodeId` (already exists!) to the `renderAcousticGraphWorldMarkers` call, which is also already wired. **ALREADY FIXED** — the `updateSonarRevealSnapshot()` method and `sonarRevealAlphaByNodeId` usage in `renderAcousticGraphWorldMarkers` are present and connected.

### Group B — Inconsistency Fixes

#### INCON-01 — X and V keys duplicate
Remove X key mapping; reassign V as the canonical "sonar pulse" key. Add a brief comment.

#### INCON-02 — Hardcoded fog uniforms overwritten
Remove the dead `u_fogStart`/`u_fogEnd` lines before `blindFogUniformUpdater` in `renderWorldMeshes()`. (Note: looking at the current `renderWorldMeshes()` at lines 1038–1064, the hardcoded fog lines DON'T appear. **ALREADY FIXED** — skip.)

#### INCON-05 — Consumable pickup no facing check
Add a facing-dot check inside `tryPickupNearestConsumable()` to match the carriable behaviour.

#### INCON-06 — All held items render as identical cubes
Create per-item-type view model meshes with distinct dimensions.

### Group C — Open Sprint 0 Fixes

#### Fix E — Vertical ray spread in GeometricRayLayer
The `fibonacciDirection` already distributes rays in 3D (sphere). The "wall bounce points" referred to in Fix E means adding Y-offset variants (low/mid/high) for each horizontal ray so bounce markers appear at wall height. Currently all rays originate from a single point. **FIX:** Add a vertical jitter parameter to `fireRays` so rays sample at multiple heights.

#### Fix H — Mist particle following player
Add a persistent mist particle emitter that follows the player's position each frame in `updateGameplaySystems`.

#### Fix I — CrouchAlcoveZone ceiling Bullet body
The zone already returns a `ColliderDescriptor` via `getColliders()`. `registerZoneColliders()` adds it to `worldColliders` and `registerStaticWorldBodies()` creates Bullet bodies for everything in `worldColliders`. **ALREADY FIXED** — the ceiling collider IS registered. But wait: looking at the `addBox` calls, only boxes with `addCollider=true` go to `worldColliders`. The zone's `getColliders()` descriptor goes through `addCollider(cx, cy, cz, w, h, d)` which creates a BoundingBox. Then `registerStaticWorldBodies()` creates a `btBoxShape` for it. **ALREADY FIXED**.

#### Fix L — Per-item view model shapes (same as INCON-06)

### Group D — Improvements

#### IMPROVE-01 — Pool ClosestRayResultCallback
Cache the callback as a field, reset between uses.

#### IMPROVE-07 — Wire SonarRevealView.alpha() to node brightness
Already wired! `sonarRevealAlphaByNodeId` is populated by `updateSonarRevealSnapshot()` and used in `renderAcousticGraphWorldMarkers()`. **ALREADY FIXED**.

#### IMPROVE-08 — Zone transitions emit acoustic ping
Add a soft footstep sound event on zone enter in `updateActiveZone()`.

#### IMPROVE-09 — panicModel.setHealth hardcoded
Add a comment explaining the placeholder and link to future enemy health system.

#### IMPROVE-10 — View model depth clear
Replace `glDisable(GL_DEPTH_TEST)` with `glClear(GL_DEPTH_BUFFER_BIT)` + `glEnable(GL_DEPTH_TEST)`.

## Actual Changes to Make

### `UniversalTestScene.java`
1. **INCON-01**: Remove `X` key → sonar mapping; keep only `V` key
2. **INCON-05**: Add facing check to `tryPickupNearestConsumable()`
3. **INCON-06 / Fix L**: Per-item view model meshes map (METAL_PIPE: 0.06×0.8×0.06, GLASS_BOTTLE: 0.09×0.38×0.09, CARDBOARD_BOX: 0.32×0.32×0.32, default: 0.15×0.15×0.15)
4. **IMPROVE-01**: Cache `ClosestRayResultCallback` as a field
5. **IMPROVE-08**: Emit acoustic ping on zone enter
6. **IMPROVE-09**: Add TODO comment on panicModel.setHealth
7. **IMPROVE-10**: View model depth: clear depth buffer instead of disable depth test
8. **Fix H**: Add mist particle emitter following player

### `GeometricRayLayer.java`
- **Fix E**: Add multi-height ray spread (low/mid/high Y variants for each horizontal direction)

### `GraphPopulator.java`
- **BUG-10 / Phase 1.1**: Raise node height samples to start at `y = minY + 0.9f` (ear level)

## Verification Plan

- Build via Gradle to confirm no compilation errors
- Code review each change for correctness

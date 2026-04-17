# PlayerTestScreen — Bug Fixes & Improvement Tasks
**Resonance | Phase 0 Test Screen | CSEL 302 Final Project**

---

## Root Cause Analysis

### Bug 1 — VHS Effect Not Rendering

**Symptom:** The scene looks flat/clean despite `vhs_postprocess.frag` existing in the shader folder.

**Root Cause:**  
`PlayerTestScreen.java` never creates a `FrameBuffer` (FBO). The VHS post-process shader is a **fullscreen quad pass** — it expects to sample from a rendered texture of the entire scene. Without an FBO, there is no texture to sample from.

The current pipeline renders directly to the back buffer:
```
Scene meshes → retro_shader → back buffer (directly)
```

What it should do:
```
Scene meshes → retro_shader → FBO (texture)  
FBO texture → vhs_postprocess shader → back buffer
```

The `retro_shader.frag` does have partial CRT effects (scanlines, dithering, phosphor mask) applied per-fragment in world space, but these are baked into 3D geometry rendering — they have no screen-space distortion, chroma aberration, tape wobble, or vignette. The VHS shader has all of that, it's just never invoked.

**Additional Note:**  
The `vhs_postprocess.frag` receives `u_vhsStrength` as a uniform but `PlayerTestScreen` never binds or sets it. Even if the FBO pipeline were added, `u_vhsStrength = 0.0` would produce no visible effect.

---

### Bug 2 — Camera Look Up/Down Appears Clamped or Locked

**Symptom:** Pitch stops responding or snaps near the look limits, especially when approaching straight up/down.

**Root Cause:**  
In `updateCamera()`, the `camera.up` vector is recomputed each frame using cross products:

```java
Vector3 right = new Vector3(0, 1, 0).crs(forward).nor();
Vector3 up = forward.cpy().crs(right).nor();
camera.up.set(up);
```

When `pitch` approaches ±85°, `forward` becomes nearly parallel to world Y `(0, 1, 0)`. The cross product `(0,1,0) × forward ≈ (0,0,0)` — a degenerate near-zero vector. Calling `.nor()` on it produces unpredictable results (NaN or axis flip), causing the camera to lock, snap, or jitter near vertical limits. This is a **gimbal lock** condition.

**Secondary Note:**  
`MAX_PITCH = 85.0f` is intentionally short of 90° to guard against this exact degeneracy — but the real fix is to stop recomputing `camera.up` from `forward` altogether. A first-person camera that never rolls should always keep `camera.up = (0, 1, 0)`.

---

## Suggested Improvements

| # | Improvement | Rationale |
|---|-------------|-----------|
| S1 | **Jump mechanic** | Gravity is integrated and `isGrounded` exists; jump input (Space) just needs `velocity.y` impulse. Needed for platform in test scene. |
| S2 | **Head bob** | Adds physicality to movement states. `elapsedSeconds` and `MovementState` are already available. |
| S3 | **FOV stretch on sprint** | Short lerp to wider FOV when `RUN` state is active increases movement feel at near-zero cost. |
| S4 | **Runtime VHS strength control** | Expose `u_vhsStrength` as a tunable float (`[0,1]`) with keyboard toggle (e.g. `V` key) for development. Useful to verify the effect is actually on. |
| S5 | **Acoustic graph debug overlay** | Render graph nodes as small 2D dots projected to screen (batch-rendered); flash them when a propagation event fires. Makes sonar reveal testable visually. |
| S6 | **Interaction crosshair** | A single screen-center dot or bracket drawn via ShapeRenderer. Highlights when within `INTERACTION_RANGE` of the crate. |
| S7 | **Sonar flash on crate interact** | Trigger a `SonarPulse` particle burst at crate position upon interaction instead of only an audio event. Validates `SoundPulseVisualizer` in the test screen. |
| S8 | **Screen resize FBO rebuild** | If VHS FBO is added, `resize()` must dispose and recreate the FBO at the new resolution — otherwise VHS pass will be stretched or cropped. |
| S9 | **Object allocation cleanup** | `computeMovementDirection()` allocates two new `Vector3` every frame. Pre-allocate as fields (`tmpForward`, `tmpRight` already exist — use them). |

---

## Task Phases

---

### Phase A — Fix VHS Post-Process Pipeline
**Goal:** Make the VHS effect actually render over the full screen.  
**Duration estimate:** 1–2 days

#### Objectives
- Introduce an FBO render target to capture the 3D scene.
- Draw the FBO output through `vhs_postprocess.frag` as a fullscreen quad.
- Confirm VHS distortion, chroma shift, grain, and vignette are visible in-game.

#### Requirements
- [ ] Add `private FrameBuffer sceneFbo;` field to `PlayerTestScreen`.
- [ ] Initialize in constructor: `sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);`
- [ ] Load VHS shader: `vhsShader = new ShaderProgram(Gdx.files.internal("shaders/vert/vhs_postprocess.vert"), Gdx.files.internal("shaders/frag/vhs_postprocess.frag"));`
- [ ] Create fullscreen quad mesh with position `[-1,1]` and UV `[0,1]` attributes matching `vhs_postprocess.vert` (`a_position`, `a_texCoord0`).
- [ ] Wrap scene render in `render()` with `sceneFbo.begin()` / `sceneFbo.end()`.
- [ ] After `sceneFbo.end()`: bind `vhsShader`, attach FBO color texture, set uniforms (`u_texture`, `u_screenSize`, `u_time`, `u_vhsStrength`), render fullscreen quad.
- [ ] Set initial `u_vhsStrength = 0.75f` (visible but not overwhelming).
- [ ] Override `resize()` to dispose and recreate `sceneFbo` at the new resolution.
- [ ] Dispose `sceneFbo` and `vhsShader` in `dispose()`.

#### Acceptance Criteria
- VHS horizontal wobble, grain, and CRT vignette are visible over the entire screen.
- No shader compile errors; graceful fallback log if VHS shader fails.
- Frame rate remains stable (no significant FBO overhead at 1080p).

---

### Phase B — Fix Camera Pitch / Gimbal Lock
**Goal:** Make look-up and look-down feel smooth and unrestricted within the intended ±85° range.  
**Duration estimate:** Half a day

#### Objectives
- Remove the cross-product `camera.up` reconstruction that causes gimbal lock.
- Ensure pitch clamping works correctly at all orientations.
- Optionally expose `MAX_PITCH` as a tunable constant for future accessibility needs.

#### Requirements
- [ ] In `updateCamera()`, replace the right/up cross-product block with a fixed world-up assignment:
  ```java
  camera.direction.set(forward);
  camera.up.set(0f, 1f, 0f);  // FPS camera never rolls
  camera.update();
  ```
- [ ] Verify pitch clamping in `updateMouseLook()` still reads:
  ```java
  pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
  ```
- [ ] Confirm mouse-up = look-up by checking `getDeltaY()` sign convention on target platform. If inverted, flip sign: `pitch -= mouseDeltaY * MOUSE_SENSITIVITY;`
- [ ] Add a brief comment on why `camera.up` is always world Y (for future devs).

#### Acceptance Criteria
- Player can look from straight ahead down to ±85° without snapping, locking, or jitter.
- No NaN in camera direction at pitch limits.
- Strafing while looking up/down moves correctly on the XZ plane.

---

### Phase C — Test Screen Improvements
**Goal:** Flesh out the test screen so it meaningfully validates all Phase 0–1 systems.  
**Duration estimate:** 2–3 days

#### Objectives
- Add jump, head bob, and FOV sprint — completing the movement feel.
- Add interaction crosshair and sonar flash for UX validation.
- Clean up per-frame allocations.
- Add runtime VHS strength toggle for tuning.

#### Requirements

**C1 — Jump Mechanic**
- [ ] Add `private static final float JUMP_FORCE = 5.0f;` to `PlayerController`.
- [ ] In `updateMovementState()`, detect `Space` key press while `isGrounded`.
- [ ] On jump: `velocity.y = JUMP_FORCE; isGrounded = false;`
- [ ] Ensure gravity in `integrateGravity()` pulls the player back down.

**C2 — Head Bob**
- [ ] Add `private float headBobPhase = 0f;` to `PlayerController`.
- [ ] Accumulate `headBobPhase` in `update()` only when `WALK`, `RUN`, or `SLOW_WALK` and `isGrounded`. Scale frequency and amplitude per state.
- [ ] In `updateCamera()`, apply `position.y + eyeHeight + sin(headBobPhase) * bobAmplitude` to camera Y.

**C3 — FOV Sprint Lerp**
- [ ] Add `private float currentFov = 75f;` to `PlayerTestScreen`.
- [ ] Each frame: lerp `currentFov` toward `85f` when `RUN`, toward `75f` otherwise.
- [ ] Apply via `camera.fieldOfView = currentFov; camera.update();`

**C4 — Interaction Crosshair**
- [ ] Add a `ShapeRenderer` or a single-pixel `Texture` drawn via `SpriteBatch` at screen center.
- [ ] Change color (white → yellow) when within `INTERACTION_RANGE` of the crate.

**C5 — Sonar Flash on Crate Interact**
- [ ] In `emitCrateInteractionSound()`, also call `soundPropagationOrchestrator` to trigger the particle sonar pulse at the crate position.
- [ ] Confirm `SoundPulseVisualizer` renders at least one pulse ring.

**C6 — Acoustic Graph Debug Overlay (toggle)**
- [ ] Add `private boolean showGraphDebug = false;` toggled with `G` key.
- [ ] When enabled, project all `acousticGraphEngine.getNodes()` to screen space via `camera.project()` and render as small dots with `ShapeRenderer`.
- [ ] Flash nodes yellow when they are in the most recent `PropagationResult.revealedNodes`.

**C7 — Allocation Cleanup**
- [ ] In `computeMovementDirection()` in `PlayerController`, replace `new Vector3()` allocations with pre-allocated `tmpForward`/`tmpRight` fields. Reset with `.set()` each call instead of `new`.

**C8 — Runtime VHS Strength Toggle**
- [ ] Add `private float vhsStrength = 0.75f;` to `PlayerTestScreen`.
- [ ] `+` key increments by 0.1, `-` decrements; clamp to `[0.0, 1.0]`.
- [ ] Log current value on change: `Gdx.app.log("VHS", "strength=" + vhsStrength);`

#### Acceptance Criteria
- Player can jump off the floor and land on the elevated platform in the test scene.
- Head bob is visible during walk/run but absent during idle/crouch.
- FOV widens smoothly while sprinting, returns on release.
- Crosshair is always visible; turns yellow when facing the crate at close range.
- At least one sonar ring pulse renders at crate position after interaction.
- Debug overlay shows all acoustic nodes projected to screen when toggled.
- No new `Vector3` allocations per frame in movement code.

---

### Phase D — Integration & Regression Validation
**Goal:** Confirm all fixes and improvements work together without breaking existing systems.  
**Duration estimate:** 1 day

#### Objectives
- Run all test screen systems end-to-end.
- Verify no regressions in footstep propagation, collision, or feature extraction.
- Confirm FBO + VHS pipeline does not break spatial cue or sonar rendering.

#### Requirements
- [ ] Run existing integration scenarios: footstep propagation, crate interaction sound, sonar reveal.
- [ ] Confirm `PlayerFeatureExtractor` still logs `avgSpeed`, `rotationRate`, etc. correctly after movement changes.
- [ ] Confirm VHS FBO does not interfere with `SoundPulseVisualizer` particle rendering (particles must render inside FBO pass, not after it).
- [ ] Confirm `resize()` correctly rebuilds FBO without memory leak (dispose old FBO before creating new one).
- [ ] Check frame time stays ≤ 16.7ms on the dev machine under full test scene load with VHS enabled.
- [ ] Visually inspect: VHS scanlines visible, camera looks up/down smoothly, jump clears the platform, crosshair highlights on crate approach.

#### Acceptance Criteria
- All Phase A/B/C acceptance criteria pass without regression.
- `Gdx.app.log` shows no error output during a 5-minute play session.
- Test screen is ready to hand off as a stable Phase 0 validation environment for Phase 1 (Dijkstra integration).

---

## Quick Reference — File Locations

| File | Change |
|------|--------|
| `resonance/devTest/PlayerTestScreen.java` | FBO init, VHS shader load, fullscreen quad, render reorder, crosshair, debug overlay, VHS toggle |
| `resonance/player/PlayerController.java` | Camera up fix, jump, head bob, allocation cleanup |
| `assets/shaders/frag/vhs_postprocess.frag` | No changes needed — already correct |
| `assets/shaders/vert/vhs_postprocess.vert` | No changes needed — already correct |

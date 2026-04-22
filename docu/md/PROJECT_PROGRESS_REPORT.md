# RESONANCE — Full Project Progress Report
> **Generated:** April 22, 2026 | **Engine:** libGDX + Bullet Physics | **Build:** Gradle Multi-Module
> **Course:** CSEL 302 — Applications Development and Emerging Technology | Laguna State Polytechnic University
> **Submission Deadline:** May 29, 2026 — **37 days remaining**

---

## Table of Contents

1. [Document Inventory](#1-document-inventory)
2. [Project Baseline — What Was Planned](#2-project-baseline--what-was-planned)
3. [Current Codebase Snapshot](#3-current-codebase-snapshot)
4. [Document-by-Document Progress Comparison](#4-document-by-document-progress-comparison)
5. [System Completion Matrix](#5-system-completion-matrix)
6. [Known Bugs & Open Issues](#6-known-bugs--open-issues)
7. [Plans Written But Not Yet Implemented](#7-plans-written-but-not-yet-implemented)
8. [Risk Register](#8-risk-register)
9. [Recommended Next Steps](#9-recommended-next-steps)
10. [Appendix — File Counts](#10-appendix--file-counts)

---

## 1. Document Inventory

All documents found in the `docu/md/` folder and their classification:

| File | Location | Type | Date | Purpose |
|---|---|---|---|---|
| `README.md` | root | Living reference | Apr 15 | Project overview and Gradle commands |
| `RESONANCE_DEVPLAN.md` | `docu/md/` | Active plan (Rev 5) | Apr 22 | Master sprint-by-sprint dev plan |
| `CODE_AUDIT.md` | `docu/md/` | Diagnostic | Apr 22 | 13 bugs, 6 inconsistencies, 10 improvements |
| `MULTIPLAYER_PLAN.md` | `docu/md/` | Future plan | Apr 22 | LAN/loopback multiplayer + voice chat |
| `CODING_STANDARDS.md` | `docu/md/` | Standards | Apr 15 | Architectural rules and code quality rules |
| `ProjectPlan.md` | `docu/md/imp/` | Submission doc | Apr 15 | CSEL 302 formal project plan |
| `ProjectProposal.md` | `docu/md/imp/` | Submission doc | Apr 15 | CSEL 302 project proposal |
| `RESONANCE_DEVPLAN.md` | `docu/md/old/` | Archived plan (Rev 3) | Apr 22 | Previous dev plan before Rev 5 |
| `SPRINT0_PROGRESS.md` | `docu/md/old/` | Sprint tracker | Apr 21 | Batch 1 status (Fixes M,C,B,J,D) |
| `Update_Task.md` | `docu/md/old/` | Task plan | Apr 18 | VHS, UniversalTestScene, Blind Effect |
| `PlayeTaskPlan.md` | `docu/md/old/` | Task plan | Apr 17 | Player controller system |
| `PlayerTestScreen_Tasks.md` | `docu/md/old/` | Task plan | Apr 17 | Bug fixes for early test screen |
| `TASK_PickupInventorySystem.md` | `docu/md/old/` | Task plan | Apr 17 | Item carry + inventory system |
| `ModelAnimation_System.md` | `docu/md/old/` | Task plan | Apr 17 | glTF model loading + animation |
| `SoundSystem_TaskPlan.md` | `docu/md/old/` | Task plan | Apr 16 | Spatial sound design |
| `ParticleSystem_Task.md` | `docu/md/old/` | Task plan | Apr 15 | 3D particle system |
| `RetroShader_Task.md` | `docu/md/old/` | Task plan | Apr 15 | Retro fog + blind shader |
| `ParticleSystem_GapAnalysis.md` | `docu/md/old/` | Gap analysis | Apr 15 | Particle system gap analysis |
| `ModelAnimation_Task.md` | `docu/md/old/` | Task plan | Apr 17 | Model animation task detail |
| `ParticleSystem_3D_TaskPlan.md` | `docu/md/old/` | Task plan | Apr 15 | Full 3D particle task plan |

**Total documented plans:** 20 files across root, active, and archived folders.

---

## 2. Project Baseline — What Was Planned

### 2.1 — Original Proposal (ProjectProposal.md)

The proposal committed to two core algorithms and a specific target audience:

| Committed Item | Status |
|---|---|
| Dijkstra shortest-path for sound propagation | ✅ Implemented (`DijkstraPathfinder.java`, tested) |
| K-Means clustering for player behavior classification | ✅ Implemented (`KMeansClassifier` inside `DirectorController`) |
| 3 procedurally generated levels | ❌ Not started |
| Adaptive Director AI driving atmosphere | 🔶 Scaffold only (`DirectorController` with tier output but no consumer) |
| libGDX desktop platform | ✅ Active |
| Mic-enabled clap/shout mechanic | ✅ Working (with `RealtimeMicSystem`, fallback V key) |

### 2.2 — Three Pillars Commitment (README.md)

| Pillar | Committed Feature | Status |
|---|---|---|
| **Acoustic Navigation** | Sound reveals geometry via sonar | ✅ Working in `UniversalTestScene` |
| **Acoustic Navigation** | Dijkstra non-LOS sound travel | ✅ `AcousticGraphEngine` + `DijkstraPathfinder` |
| **Acoustic Navigation** | Breaking/dropping items makes sound enemies can hear | ✅ `ImpactListener` + `PhysicsNoiseEmitter` |
| **Director AI** | K-Means clusters player behavior | ✅ 5-feature, 3-cluster classifier running |
| **Director AI** | Dynamic pacing adjusts pressure | 🔶 Tier computed, not wired to environment |
| **Director AI** | Fear intensity responds to movement | 🔶 Features extracted, outputs unused |
| **Procedural Loop** | 3 procedurally generated levels | ❌ Not started |
| **Procedural Loop** | Handcrafted puzzle logic per level | ❌ Not started |
| **Procedural Loop** | Random item spawns | ❌ Not started |
| **Procedural Loop** | Stylized HUD with stress + noise meters | 🔶 DiagnosticOverlay exists; final HUD not built |
| **Procedural Loop** | Resource-limited tools | ✅ 4-slot inventory with flare + decoy |

---

## 3. Current Codebase Snapshot

### 3.1 — Metrics

| Metric | Count |
|---|---|
| Java source files | **137** |
| Java test files | **9** |
| Shader files | **14** (7 vert + 7 frag) |
| JSON config files | **4** |
| Particle presets | **10** |
| Total asset files | **38** |
| Gradle modules | 4 (`core`, `lwjgl3`, `server`, `shared`) |

### 3.2 — Package Breakdown

| Package | Classes | Purpose | Maturity |
|---|---|---|---|
| `devTest` | 5 screens | Test infrastructure | ✅ Stable |
| `devTest.universal` | 6 | UniversalTestScene zones + abstractions | ✅ Stable |
| `devTest.universal.diagnostics` | 4 | DiagnosticOverlay tabs | ✅ Stable |
| `devTest.universal.zones` | 7 | Seven test zones | ✅ Stable |
| `director` | 1 | DirectorController + KMeansClassifier | 🔶 Scaffold |
| `items` | 3 | CarriableItem, ItemDefinition, ItemType | ✅ Stable |
| `model` | 8 | glTF/G3D model loading + animation | ✅ Stable |
| `particles` | 16 | Full 3D particle system | ✅ Production-ready |
| `player` | 11 | Controller, carry, inventory, features | ✅ Mostly complete |
| `rendering` | 5 | VHS body-cam shader pipeline | ✅ Complete |
| `rendering.blind` | 7 | Blind effect + modifier stack | ✅ Complete |
| `sound` | 25 | Acoustic graph, Dijkstra, orchestrator | ✅ Production-ready |
| `sound.viz` | 6 | Bounce visualizer, ray layer, pulse shader | 🔶 In progress |

### 3.3 — Test Coverage

| Test File | Tests What | Status |
|---|---|---|
| `AcousticGraphEngineTest` | Graph build, edge weights | ✅ Written |
| `DijkstraPathfinderTest` | Shortest path correctness | ✅ Written |
| `SoundPropagationOrchestratorTest` | Full propagation pipeline | ✅ Written |
| `SonarRendererTest` | Node reveal lifecycle | ✅ Written |
| `MicInputListenerTest` | Mic threshold + VAD | ✅ Written |
| `PlayerFeatureExtractorTest` | 5-feature rolling window | ✅ Written |
| `ImpactListenerTest` | Physics impulse → sound event | ✅ Written |
| `InventorySystemTest` | Slot management | ✅ Written |
| `ItemDefinitionTest` | Item data factories | ✅ Written |

9 test classes covering the algorithm-critical and gameplay-critical paths. **No tests yet exist** for `DirectorController`, `BlindEffectController`, `GraphPopulator`, or any zone class.

---

## 4. Document-by-Document Progress Comparison

### 4.1 — RetroShader_Task.md
**Planned:** 1×1×1 block test object, `retro_shader.vert/frag`, distance fog, blind fog, ambient lighting.

| Task | Planned | Actual |
|---|---|---|
| Retro shader vert/frag | ✅ | ✅ `retro_shader.vert/frag` exist (Apr 15, updated Apr 22) |
| Distance fog in shader | ✅ | ✅ `u_fogStart` / `u_fogEnd` uniforms present |
| Blind fog mode | ✅ | ✅ `BlindFogUniformUpdater` applies blind uniforms each frame |
| Ambient + shadow | ✅ | ✅ `u_ambientColor`, `u_shadowColor`, `u_shadowStrength` in shader |
| 1×1×1 test block | ✅ | ✅ `createCubeMesh()` used throughout scene |

**Status: FULLY COMPLETE.** Shader supports all requested features. Additional features beyond original scope: scanlines, dithering, phosphor mask, per-material blind attenuation.

---

### 4.2 — SoundSystem_TaskPlan.md
**Planned:** All-source type spatial audio, occlusion, reverb zones, material influence, distance system.

| Task | Planned | Actual |
|---|---|---|
| Sound source types (Static, Attached, Event, Reactive) | ✅ | ✅ `StaticSoundSource`, `AttachedSoundSource`, `EventSoundSource` |
| 3D spatial positioning | ✅ | ✅ `SpatialCueController` with listener node tracking |
| Occlusion/material attenuation | ✅ | ✅ `AcousticMaterial` enum with coefficients; edge weight system |
| Reverb zones | Planned | 🔶 `RoomAcousticProfile` enum exists; not applied per-zone in test scene |
| Dijkstra propagation | ✅ | ✅ Full `DijkstraPathfinder` with binary heap, tested |
| Non-LOS sound travel | ✅ | ✅ Works through `AcousticGraphEngine` edge graph |
| Intensity decay formula | ✅ | ✅ `exp(-α·d)` formula in orchestrator |
| Enemy hearing integration | Planned | 🔶 `EnemyHearingTarget` interface exists; no enemy to consume it |
| Material transmission | Planned | ✅ NEW (Rev 5): `GraphPopulator` adds transmission edges |

**Status: CORE COMPLETE. Gaps:** Reverb profiles not applied dynamically; no audio clips (all spatial cues are silent — no `.ogg` assets found). Enemy hearing is wired but has nothing to consume it.

---

### 4.3 — ParticleSystem_Task.md / ParticleSystem_3D_TaskPlan.md
**Planned:** 3D instanced particles, multiple emission shapes, physics collision, presets.

| Task | Planned | Actual |
|---|---|---|
| 3D instanced `ParticleEmitter` | ✅ | ✅ Full GPU-instanced emitter |
| Emission shapes (sphere, ring, cone) | ✅ | ✅ `ParticleEmissionShape` enum |
| Force fields (attractor, turbulence, vector) | ✅ | ✅ `PointAttractorField`, `TurbulenceField`, `VectorField` |
| Collision planes | ✅ | ✅ `CollisionPlane` |
| Trail emitter | ✅ | ✅ `TrailEmitter` |
| JSON preset store | ✅ | ✅ `ParticlePresetStore` — 10 presets in assets |
| Blend modes (additive, alpha) | ✅ | ✅ `ParticleBlendMode` |
| Particle effects (explosion, fire, mist, sonar) | ✅ | ✅ All present as JSON presets |
| Depth mode (no-write) | ✅ | ✅ `DepthMode` per emitter |
| `ParticleManager` batch render | ✅ | ✅ `particleShaderProgram` pass in scene |

**Status: FULLY COMPLETE AND PRODUCTION-READY.** One of the most mature systems in the project. The `mist`, `sonar_pulse`, `explosion`, and `smoke_puff` presets are all referenced by game logic.

---

### 4.4 — PlayeTaskPlan.md
**Planned:** 5-state movement, crouch, mouse look, interaction, footstep audio, head-bob.

| Task | Planned | Actual |
|---|---|---|
| 5 movement states (IDLE, WALK, RUN, SLOW_WALK, CROUCH) | ✅ | ✅ `MovementState` enum, all states in `PlayerController` |
| Mouse look (yaw + pitch ±85°) | ✅ | ✅ Implemented; gimbal-lock fix applied (camera.up = Y) |
| Bullet collision movement | ✅ | ✅ `worldColliders` AABB sweep in `PlayerController` |
| Stamina drain/regen | Planned | ✅ `getStamina()` / `getMaxStamina()` on `PlayerController` |
| F-key interaction | ✅ | ✅ Raycast + `PlayerInteractionSystem` |
| Sprint FOV stretch | Suggested | ✅ `RUN_FOV = 85f`, lerped in `updateCameraFov()` |
| Head bob | Suggested | ✅ `CameraBreathingController` (Fix K, completed Apr 21) |
| Footstep sound events | ✅ | ✅ `PlayerFootstepSoundEmitter` with `MovementState` noise multipliers |
| Crouch alcove ceiling physics | Planned | ❌ `CrouchAlcoveZone` has geometry but no ceiling Bullet body (Fix I open) |

**Status: MOSTLY COMPLETE.** Player feel is solid. Main gap is Fix I (ceiling collider for crouch alcove).

---

### 4.5 — PlayerTestScreen_Tasks.md
**Planned:** Fix VHS pipeline (FBO), fix camera gimbal lock, multiple UX improvements.

| Task | Planned | Actual |
|---|---|---|
| FBO render → VHS post-process | ✅ | ✅ `BodyCamPassFrameBuffer` + `BodyCamVHSVisualizer` fully implemented |
| Gimbal lock fix (camera.up) | ✅ | ✅ Fixed — camera.up never recomputed from cross-product |
| Jump mechanic (S1) | Suggested | ❌ Not implemented |
| Head bob (S2) | Suggested | ✅ Implemented as `CameraBreathingController` |
| Sprint FOV stretch (S3) | Suggested | ✅ Implemented |
| Runtime VHS strength control (S4) | Suggested | ✅ `B` key toggles VHS; JSON config editable |
| Acoustic graph debug overlay (S5) | Suggested | ✅ `G` key + `DiagnosticOverlay` acoustic tab |
| Interaction crosshair (S6) | Suggested | ✅ `renderCrosshair()` with color change on target |
| Sonar flash on interaction (S7) | Suggested | ✅ `spawnSonarPulse()` fires on all sound events |
| FBO rebuild on resize (S8) | Suggested | ✅ `recreateBodyCamFrameBuffers()` in `resize()` |
| Object allocation cleanup (S9) | Suggested | ✅ `tmpForward`, `tmpRight`, etc. as pre-allocated fields |

**Status: FULLY COMPLETE.** All bugs fixed. Most suggested improvements implemented.

---

### 4.6 — TASK_PickupInventorySystem.md
**Planned:** Spring-physics carry, 4-slot inventory, throw, break VFX, interaction prompt, item state.

| Task | Planned | Actual |
|---|---|---|
| `ItemType` enum | ✅ | ✅ METAL_PIPE, CARDBOARD_BOX, GLASS_BOTTLE, CONCRETE_CHUNK, FLARE, NOISE_DECOY, KEY |
| `ItemDefinition` factory | ✅ | ✅ Full factory with all fields |
| `CarriableItem` + `btRigidBody` | ✅ | ✅ Bullet body, `userValue` registry, `ItemState` enum |
| Spring physics carry (original plan) | ✅ | ✅→❌ Replaced by weightless snap (Fix A) — springs removed by design |
| Weightless snap carry (Fix A) | Not planned | ✅ `proceedToTransform()` direct placement |
| Throw (F + RMB) | ✅ | ✅ Right-click throws with `throwStrength` |
| Drop (Q) | ✅ | ✅ Drops from inventory; `dropHeldItem()` restores gravity |
| Stash to inventory (TAB) | ✅ | ✅ Working |
| 4-slot `InventorySystem` | ✅ | ✅ `InventorySystem.SLOT_COUNT = 4` |
| Consumable use (E) | ✅ | ✅ FLARE triggers `triggerFlareReveal()`, NOISE_DECOY emits sound |
| Break VFX (explosion particle) | ✅ | ✅ `processDeferredBreaks()` spawns particle at impact |
| Interaction prompt (E/F/Drop labels) | ✅ | ✅ `drawInteractionPrompt()` in HUD |
| View-model rendering | ✅ | 🔶 Placeholder colored cube; no per-item mesh shape (all cubes identical) |
| `ImpactListener` + `PhysicsNoiseEmitter` | ✅ | ✅ Fully wired; Dijkstra runs on impact |
| Item break threshold + impulse guard | ✅ | ✅ `breakThreshold` in `ItemDefinition`, consumed in `ImpactListener` |

**Status: MOSTLY COMPLETE.** Carry, inventory, throw, drop, break VFX, and acoustic wiring all work. Gap: view model renders identical cubes for all item types (INCON-06 in CODE_AUDIT).

---

### 4.7 — ModelAnimation_System.md
**Planned:** Format-agnostic model loader, named animation controller, scene model wrapper.

| Task | Planned | Actual |
|---|---|---|
| `ModelLoader` interface | ✅ | ✅ Implemented |
| `GltfModelLoader` (reflection-based) | ✅ | ✅ Implemented |
| `G3dModelLoaderAdapter` | ✅ | ✅ Implemented |
| `ModelImportService` (orchestrator) | ✅ | ✅ Implemented |
| `ModelAssetManager` (cache + refcount) | ✅ | ✅ Implemented |
| `ModelData` (animation lookup) | ✅ | ✅ Implemented |
| `NamedAnimationController` | ✅ | ✅ Implemented |
| `SceneModel` (instance wrapper) | ✅ | ✅ Implemented |
| `ModelDebugScreen` (test harness) | ✅ | ✅ `devTest/ModelDebugScreen.java` exists |

**Status: FULLY COMPLETE.** Model system is finished and has its own debug screen. Note: the game does not yet use models for gameplay (player, items, or enemy) — all entities are rendered as colored boxes. This system is ready but awaiting art assets.

---

### 4.8 — Update_Task.md (Task 1 — VHS, Task 2 — UniversalTestScene, Task 3 — Blind Effect)

**Task 1 — Body Camera VHS Effect:**

| Task | Status |
|---|---|
| `body_cam_vhs.vert/frag` shader pair | ✅ |
| `BodyCamPassFrameBuffer` FBO pipeline | ✅ |
| JSON config `body_cam_settings.json` | ✅ |
| `BodyCamVHSAnimator` (animated scan drift) | ✅ |
| B-key runtime toggle | ✅ |
| Ctrl+R hot-reload config | ✅ |

**Task 2 — UniversalTestScene:**

| Task | Status |
|---|---|
| 7-zone walkable hub world | ✅ |
| `BaseShellZone` + `TestZone` abstractions | ✅ |
| Zone: `RampStairsZone` | ✅ |
| Zone: `CrouchAlcoveZone` | ✅ (geometry only; no ceiling collider — Fix I) |
| Zone: `SoundPropagationZone` | ✅ |
| Zone: `ParticleArenaZone` | ✅ |
| Zone: `ItemInteractionZone` | ✅ |
| Zone: `ShaderCorridorZone` | ✅ |
| Zone: `BlindChamberZone` | ✅ |
| `DiagnosticOverlay` with tab cycling | ✅ |
| `ColliderDescriptor` zone colliders | ✅ |

**Task 3 — Blind Effect:**

| Task | Status |
|---|---|
| `BlindEffectController` modifier stack | ✅ |
| `BlindBaselineModifier` | ✅ |
| `BlindPanicModifier` | ✅ |
| `BlindFlareModifier` | ✅ |
| `BlindSonarRevealModifier` | ✅ (wired but sonar expand disabled per Fix M) |
| `BlindFogUniformUpdater` | ✅ |
| JSON config `blind_effect_config.json` | ✅ |
| Fog hard cutoff (Minecraft style) | ✅ NEW (Fix Q, Rev 5) |
| Panic model integration | ✅ |

**Status: ALL THREE UPDATE TASKS FULLY COMPLETE.** Rev 5 added the hard-cutoff fog improvement on top of Task 3.

---

### 4.9 — SPRINT0_PROGRESS.md (Batch 1)

| Fix | Description | Status |
|---|---|---|
| Fix M | Remove blind light expansion on sonar pulse | ✅ Complete |
| Fix C | Raise corridor wall height 1.5→3.5 | ✅ Complete |
| Fix B | Static floor Bullet collider + spawn Y fix | ✅ Complete |
| Fix J | Long-range sonar reveal config + lifetime | ✅ Complete |
| Fix D | Pulse origin = player world position | ✅ Complete |

Remaining Sprint 0 fixes at time of last update:

| Fix | Description | Status |
|---|---|---|
| Fix E | Bounce points on walls (vertical ray spread) | ❌ Still open |
| Fix F | Mic HUD + reliability | 🔶 V-key fallback added; full HUD not built |
| Fix G | Proper HUD (voice meter + stamina bar) | 🔶 DiagnosticOverlay shows stamina; no production HUD |
| Fix H | Atmospheric fog/mist particles | ❌ Still open |
| Fix I | Crouch alcove ceiling Bullet body | ❌ Still open |
| Fix K | Camera idle breathing animation | ✅ Complete (Apr 21) |
| Fix L | Item system polish (throw VFX, view model) | 🔶 Break VFX works; view model is placeholder |

Additionally — Rev 5 fixes completed after Sprint 0 document:

| Fix | Description | Status |
|---|---|---|
| Fix N | Pulse zone-independence + world-aware raycast | ✅ Complete (Rev 5) |
| Fix O | Custom GLSL pulse shader (`sound_pulse.vert/frag`) | ✅ Complete (Rev 5) |
| Fix P | Material-based wall transmission + graph edges | ✅ Complete (Rev 5) |
| Fix Q | Minecraft-style hard-cutoff fog | ✅ Complete (Rev 5) |

---

### 4.10 — RESONANCE_DEVPLAN.md (Current, Rev 5) vs Old Rev 3

The current plan is Rev 5 (in `docu/md/`). Rev 3 is archived in `docu/md/old/`.

**Differences:**
- Rev 5 adds Fixes N, O, P, Q — all completed.
- Rev 5 promotes `UniversalTestScene` as main test entry point.
- Rev 5 removes sonar reveal from `BlindEffectController.onSoundEvent` as a design decision.
- Rev 5 adds transmission edge synthesis to `GraphPopulator`.
- Rev 3 is superseded entirely; no discrepancies between its planned and Rev 5 completed items.

**Phase 1 status (Rev 5):**

| Sub-phase | Description | Status |
|---|---|---|
| 1.1 | Sound Propagation Zone full integration | 🔶 Propagation works; graph node height at y=0.05 (needs y=0.9 fix) |
| 1.2 | Item interaction zone physics ground truth | ✅ Floor collider, spawn Y, impact listener all working |
| 1.3 | Crouch alcove zone full playthrough | ❌ Ceiling collider (Fix I) missing |
| 1.4 | Diagnostic overlay mic + stamina tabs | ✅ Complete (Apr 22) |

**Phases 2–6 status:**

| Phase | Description | Status |
|---|---|---|
| Phase 2 | Core gameplay loop (items as acoustic tools, stamina tension) | 🔶 Foundation done; stamina wired, no Director response yet |
| Phase 3 | Director AI + enemy systems | 🔶 `DirectorController` scaffold; no `EnemyController` class |
| Phase 4 | Level design + procedural generation | ❌ Not started |
| Phase 5 | Horror atmosphere + polish | ❌ Not started |
| Phase 6 | Playtesting, QA, release prep | ❌ Not started |

---

### 4.11 — MULTIPLAYER_PLAN.md

Planned 9-day implementation of Kryonet-based LAN multiplayer with voice chat.

| Component | Status |
|---|---|
| `MultiplayerManager` (Kryonet host/client) | ❌ Not started |
| `RemotePlayer` capsule rendering | ❌ Not started |
| Packet design (PlayerState, SoundEvent, VoiceChunk) | ❌ Not started (design only) |
| `VoiceCaptureSystem` (javax.sound, 48kHz) | ❌ Not started |
| `VoicePlaybackSystem` (SourceDataLine, spatial pan) | ❌ Not started |
| Acoustic event sync across clients | ❌ Not started |
| NETWORK tab in DiagnosticOverlay | ❌ Not started |

**Status: PLAN COMPLETE. IMPLEMENTATION NOT STARTED.** This is a future milestone, not a CSEL 302 submission blocker.

---

### 4.12 — CODE_AUDIT.md

The code audit (Apr 22) identified 29 items. None have been resolved in the current zip:

| Severity | Count | Resolved |
|---|---|---|
| High bugs | 3 | 0 |
| Medium bugs | 5 | 0 |
| Low bugs | 5 | 0 |
| Inconsistencies | 6 | 0 |
| Improvements | 10 | 0 |

The three high-priority bugs are still active:
- **BUG-03:** Mic pulse cooldown 8s vs keyboard 1.2s (breaks core mechanic fairness).
- **BUG-07:** `autoCollectNearbyConsumables()` silently vacuums consumables each frame.
- **BUG-10:** `GraphPopulator` fallback places graph at world origin, 56+ units from player.

---

## 5. System Completion Matrix

| System | Planned In | Implemented | Tested | Production-Ready |
|---|---|---|---|---|
| Retro world shader | `RetroShader_Task.md` | ✅ | Manual | ✅ |
| VHS body-cam pipeline | `Update_Task.md` | ✅ | Manual | ✅ |
| Blind effect + modifier stack | `Update_Task.md` | ✅ | Manual | ✅ |
| 3D particle system | `ParticleSystem_3D_TaskPlan.md` | ✅ | Manual | ✅ |
| Player controller (5 states) | `PlayeTaskPlan.md` | ✅ | Manual | ✅ |
| Camera breathing (Fix K) | `RESONANCE_DEVPLAN.md` | ✅ | Manual | ✅ |
| Model loading + animation | `ModelAnimation_System.md` | ✅ | Manual | ✅ |
| Item carry + throw | `TASK_PickupInventorySystem.md` | ✅ | Unit | ✅ |
| 4-slot inventory | `TASK_PickupInventorySystem.md` | ✅ | Unit | ✅ |
| Acoustic graph (Dijkstra) | `SoundSystem_TaskPlan.md` | ✅ | Unit | ✅ |
| Sound propagation orchestrator | `SoundSystem_TaskPlan.md` | ✅ | Unit | ✅ |
| Material-based transmission (Fix P) | `RESONANCE_DEVPLAN.md` Rev5 | ✅ | Manual | 🔶 |
| Sound pulse GLSL shader (Fix O) | `RESONANCE_DEVPLAN.md` Rev5 | ✅ | Manual | 🔶 |
| Mic input + VAD | `RESONANCE_DEVPLAN.md` | ✅ | Unit | 🔶 |
| `DirectorController` + K-Means (3C, 5F) | `RESONANCE_DEVPLAN.md` | ✅ | Partial | 🔶 Outputs unused |
| `PlayerFeatureExtractor` | `RESONANCE_DEVPLAN.md` | ✅ | Unit | ✅ |
| Impact listener (physics → sound) | `TASK_PickupInventorySystem.md` | ✅ | Unit | ✅ |
| `GraphPopulator` (world-aware graph) | `RESONANCE_DEVPLAN.md` | ✅ | — | 🔶 BUG-10 |
| 7-zone UniversalTestScene | `Update_Task.md` | ✅ | Manual | ✅ |
| Fog hard-cutoff (Fix Q) | `RESONANCE_DEVPLAN.md` Rev5 | ✅ | Manual | ✅ |
| Acoustic bounce visualizer | `RESONANCE_DEVPLAN.md` | ✅ | Manual | 🔶 |
| **Proper production HUD** | `RESONANCE_DEVPLAN.md` Fix G | ❌ | — | ❌ |
| **Enemy AI system** | `AI_ENEMY_PLAN.md` | ❌ | — | ❌ |
| **K-Means 4-cluster expansion** | `AI_ENEMY_PLAN.md` | ❌ | — | ❌ |
| **Light source registry + flicker** | `AI_ENEMY_PLAN.md` | ❌ | — | ❌ |
| **Environment reactor** | `AI_ENEMY_PLAN.md` | ❌ | — | ❌ |
| **Multiplayer (Kryonet + voice)** | `MULTIPLAYER_PLAN.md` | ❌ | — | ❌ |
| **Procedural level generation** | `ProjectPlan.md` | ❌ | — | ❌ |
| **Handcrafted puzzle rooms** | `ProjectPlan.md` | ❌ | — | ❌ |
| **Audio assets (.ogg clips)** | `SoundSystem_TaskPlan.md` | ❌ | — | ❌ |
| **Reverb zone application** | `SoundSystem_TaskPlan.md` | ❌ | — | ❌ |
| **Jump mechanic** | `PlayerTestScreen_Tasks.md` S1 | ❌ | — | ❌ |

**Legend:** ✅ Complete | 🔶 Partial/Scaffold | ❌ Not started

---

## 6. Known Bugs & Open Issues

### High Severity — Fix Before Submission

| ID | Location | Issue | Impact |
|---|---|---|---|
| BUG-03 | `UniversalTestScene` | Mic cooldown 8s vs keyboard 1.2s | Core mechanic behaves differently for mic users |
| BUG-07 | `UniversalTestScene` | `autoCollectNearbyConsumables()` silently picks up items every frame without player input | Breaks intentional consumable design |
| BUG-10 | `GraphPopulator` | Fallback graph is at world origin (0,0,0); hub is at (40,0,40) | Dijkstra propagates from wrong graph → empty reveals |

### Medium Severity — Fix Before QA

| ID | Location | Issue |
|---|---|---|
| BUG-02 | `UniversalTestScene` | `Bullet.init()` called on every F10 reload — native crash risk |
| BUG-04 | `SoundPropagationZone` | `ShapeRenderer` never disposed → GPU leak on scene reload |
| BUG-05 | `UniversalTestScene` | Up to 4 Dijkstra runs per physics manifold contact |
| BUG-06 | `UniversalTestScene` | `hide()` not overridden — mic thread keeps running on screen switch |
| BUG-08 | `UniversalTestScene` | Panic distance measured from hub center, not enemy position |
| BUG-11 | `GraphPopulator` | All graph edges hardcoded to CONCRETE — material diversity ignored |
| INCON-05 | `UniversalTestScene` | Consumable pickup has no facing check while carriable does |

### Low Severity — Clean Up When Possible

| ID | Location | Issue |
|---|---|---|
| BUG-01 | `UniversalTestScene` | Dead null check on `frame` after reassignment |
| BUG-09 | `SoundPropagationZone` | Deep-copies 100+ graph nodes every frame |
| BUG-12 | `UniversalTestScene` | `findNearestNodeId` O(n) called 4+ times per frame |
| BUG-13 | `SonarRenderer` | `snapshot()` intensity data computed but never used |
| INCON-01 | `UniversalTestScene` | X and V keys do identical thing |
| INCON-02 | `UniversalTestScene` | Hardcoded fog uniforms immediately overwritten |
| INCON-04 | `UniversalTestScene` | Physics step buried inside zone management method |
| INCON-06 | `UniversalTestScene` | All held items render as identical cubes |

### Open Sprint 0 Fixes

| Fix | Description |
|---|---|
| Fix E | Vertical ray spread in `GeometricRayLayer` for wall bounce points |
| Fix H | Mist particle emitter following player (atmospheric fog layer) |
| Fix I | Bullet ceiling collider in `CrouchAlcoveZone` |
| Fix L | Per-item view model shapes (not all identical cubes) |

---

## 7. Plans Written But Not Yet Implemented

The following plans have full implementation specs but zero code committed:

### 7.1 — AI Enemy System (`AI_ENEMY_PLAN.md`)

A complete 11.5-day plan for enemy AI driven by K-Means. Key items not yet started:

- `EnemyController`, `EnemyBrain`, `EnemyBody` — the entire `enemy/` package
- `EnemyParameters` record and `ParameterLerper`
- PATROL → CURIOUS → ALERTED → HUNTING → LOST state machine
- K-Means expansion to 4 clusters (CALM, TENSE, PANICKED, EXPOSED) and 7 features
- `LightSourceRegistry`, `LightSource`, `LightFlickerController`
- `EnvironmentReactor`, `FogPulseController`, `VHSScratchModulator`

**This is the most critical unimplemented system** for the CSEL 302 rubric, which requires a live intelligent controller driving gameplay.

### 7.2 — Multiplayer + Voice Chat (`MULTIPLAYER_PLAN.md`)

Full Kryonet + javax.sound voice plan. Zero implementation started. This is a stretch goal and not a CSEL 302 submission requirement. Defer until post-submission.

### 7.3 — Procedural Level Generation (`ProjectPlan.md`)

The formal CSEL 302 plan describes `ProceduralLevelGenerator`, `ItemSeeder`, `LevelValidator`, and three handcrafted puzzle chains. None of these exist in the codebase. The game currently runs only in the 7-zone `UniversalTestScene` test environment.

### 7.4 — Audio Assets

`SoundSystem_TaskPlan.md` describes footstep clips, ambient drones, heartbeat sounds, and enemy proximity stingers. **Zero audio asset files are present in the project.** All `SpatialCueController` calls are currently silent. This is a major gap for a game where sound is the primary mechanic.

---

## 8. Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Enemy AI not complete before submission | High | High | Prioritise `AI_ENEMY_PLAN.md` Phases AI-0 through AI-5 immediately |
| No audio assets — game is silent | High | High | Record or source basic ambient + footstep clips (even placeholder .ogg files) before QA |
| BUG-10 (wrong graph origin) causes empty sonar reveals | High | High | One-line fix in `GraphPopulator` fallback — trivial, do it today |
| BUG-03 (mic cooldown mismatch) hurts evaluation demo | Medium | High | Remove `micPulseCooldownRemaining` and delegate to orchestrator |
| Procedural levels not complete by May 29 | High | Medium | Minimum viable: build one handcrafted level in `UniversalTestScene` using existing `addBox` infrastructure |
| `DirectorController` outputs wired to nothing | High | Medium | Wire `currentTier` → `EnvironmentReactor` and placeholder enemy aggression before Phase AI-5 |
| Test coverage gaps (no Director, no Blind, no Populator tests) | Medium | Medium | Add at minimum 3 tests: `KMeansClassifierTest`, `GraphPopulatorTest`, `BlindEffectControllerTest` |
| `SoundPropagationZone.shapeRenderer` leak on every reload | Medium | Low | One-line fix: add `dispose()` override with `shapeRenderer.dispose()` |

---

## 9. Recommended Next Steps

Listed in priority order based on CSEL 302 deadline (May 29, 37 days remaining).

### Week 1 (Apr 23–29) — Critical Stability

1. **Fix BUG-10** — `GraphPopulator` fallback at origin. One line change. Unblocks all acoustic testing.
2. **Fix BUG-03** — Remove `micPulseCooldownRemaining`. Unblocks mic-based demo.
3. **Fix BUG-07** — Remove `autoCollectNearbyConsumables()`. Items now require intentional pickup.
4. **Fix BUG-04** — Add `dispose()` to `SoundPropagationZone`. Prevents GPU leak on F10.
5. **Start AI Enemy Phase AI-0** — `LightSourceRegistry`, feature 5+6 in `PlayerFeatureExtractor`, 4-cluster K-Means.

### Week 2 (Apr 30–May 6) — Enemy Core

6. **AI Phase AI-1** — Light flicker system wired to K-Means tiers.
7. **AI Phase AI-2** — Fog pulse + VHS modulation per tier.
8. **AI Phase AI-3** — `EnemyBody` Bullet capsule + basic node navigation.

### Week 3 (May 7–13) — Enemy Intelligence + Audio

9. **AI Phase AI-4** — Full state machine (PATROL → HUNTING → LOST) + hearing.
10. **AI Phase AI-5** — Wire `DirectorController` → `EnemyController.onTierChanged()`.
11. **Add placeholder audio assets** — 3–5 basic .ogg files: footstep, ambient hum, heartbeat, pulse ping.
12. **Fix E, Fix H, Fix I** — remaining Sprint 0 items.

### Week 4 (May 14–20) — Level + Polish

13. **Build one handcrafted level** using `addBox` infrastructure matching facility map zones.
14. **Build simple puzzle chain** — find AXE → break planks → get KEY → exit.
15. **Fix production HUD** (voice meter + stamina bar visible at all times, not just in DiagnosticOverlay).
16. **Wire `RoomAcousticProfile`** to zones (reverb differences between hub, labs, containment).

### Week 5 (May 21–27) — QA + Documentation

17. **Run all unit tests**, fix any regressions.
18. **Add missing tests:** `KMeansClassifierTest`, `GraphPopulatorTest`, `BlindEffectControllerTest`.
19. **Performance profiling** — frame time target <16ms, Dijkstra <2ms per event.
20. **Record video presentation** (5–7 min per `ProjectPlan.md` script).
21. **Complete CSEL 302 documentation** — fill in test results, screenshots, architecture diagrams.
22. **Package submission** — fat jar + `dist/` folder + docs.

---

## 10. Appendix — File Counts

### Java Source Files by Package

| Package | Count |
|---|---|
| `devTest` | 5 |
| `devTest.universal` | 6 |
| `devTest.universal.diagnostics` | 4 |
| `devTest.universal.zones` | 7 |
| `director` | 1 |
| `items` | 3 |
| `model` | 8 |
| `particles` | 16 |
| `player` | 11 |
| `rendering` | 5 |
| `rendering.blind` | 7 |
| `sound` | 25 |
| `sound.viz` | 6 |
| **Total** | **137** |

### Shader Files

| File | Date | Purpose |
|---|---|---|
| `retro_shader.vert/frag` | Apr 22 | World geometry — fog, blind, scanlines, dither |
| `particle_shader.vert/frag` | Apr 15 | GPU-instanced 3D particles |
| `player_shader.vert/frag` | Apr 17 | View model rendering |
| `body_cam_vhs.vert/frag` | Apr 21 | VHS post-process fullscreen pass |
| `trail_shader.vert/frag` | Apr 15 | Particle trail renderer |
| `vhs_postprocess.vert/frag` | Apr 17 | Alternate VHS shader (legacy) |
| `sound_pulse.vert/frag` | **Apr 22** | **NEW** — GLSL sonar pulse shell |

### Asset Configs

| File | Purpose |
|---|---|
| `blind_effect_config.json` | Fog bounds, sonar reveal, panic thresholds |
| `balancing_config.json` | Sound event tuning, pulse lifetimes, cooldowns |
| `body_cam_settings.json` | VHS effect strength, scan speed |
| `acoustic_bounce_3d_config.json` | Bounce ray count, depth, fade |

### Particle Presets

`default`, `explosion`, `fire`, `mist`, `ring_shot`, `smoke`, `smoke_puff`, `sonar_pulse`, `spiral` + `fireball` effect.

---

*Report generated April 22, 2026. Covers all 20 documents in `docu/md/` against 137 source files, 14 shaders, and 4 configs in the current zip build.*
*Next report recommended after Week 1 (Apr 29) to verify critical bug fixes and AI Phase AI-0 completion.*

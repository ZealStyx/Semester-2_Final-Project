# RESONANCE — Master Plan Progress Report

**Generated:** 2026-04-28  
**Project:** `io.github.superteam.resonance`  
**Module audited:** `core`  
**Source of truth:** `RESONANCE_COMBINED_MASTER_PLAN_v2.md`

> **Purpose:** This file is the canonical handoff document. GitHub Copilot and any developer picking up the project should read this first. It records exactly what is done, what is partial, and what is next.

---

## Quick Status Summary

| # | System | Status | Notes |
|---|--------|--------|-------|
| 1 | Event System | 🟡 Partial | Core done; missing 7 actions + full EventContext fields |
| 2 | Sound System — WAV Layer | 🟢 Done | Audio + propagation graph implemented |
| 3 | Trigger System | 🟡 Partial | ZoneTrigger only; CompoundTrigger missing |
| 4 | Auto-Bullet from `-map.gltf` | 🟢 Done | MapCollisionBuilder + map/ package present |
| 5 | Map Editor — Swing Tool | 🟡 Partial | ModelDebugScreen exists; full Swing editor unclear |
| 6 | Enemy AI System | 🟢 Done | EnemyController, EnemyStateMachine, EnemyPerception all present |
| 7 | Door & Interactable System | 🟡 Partial | Old toggle model still present; v2 drag/barge helpers added |
| 8 | Raycast Interaction System | 🟢 Done | RaycastInteractionSystem + InteractionPromptRenderer |
| 9 | Dynamic Lighting System | 🟢 Done | LightManager, FlickerController, LightReactorListener |
| 10 | Sanity / Fear System | 🟢 Done | SanitySystem + all drains + all effects |
| 11 | Jump Scare Director | 🟢 Done | JumpScareDirector, JumpScare, ScareType |
| 12 | Dialogue & Subtitle System | 🟡 Partial | Minimal `dialogue/` stubs added (SubtitleRenderer, DialogueSystem) |
| 13 | Save / Checkpoint System | 🟡 Partial | Minimal `save/` stubs added (SaveSystem, SaveData) |
| 14 | Story System | 🟡 Partial | Minimal `story/` stubs added (StorySystem, StoryGate) |
| 15 | Room Transition System | 🟡 Partial | Minimal `transition/` stubs added (RoomTransitionSystem, FadeOverlay) |
| 16 | In-Game Debug Console | 🟡 Partial | DiagnosticOverlay exists; full console (`~` key) missing |
| 17 | Settings System | 🟢 Done | SettingsSystem, SettingsData, KeybindRegistry |
| 18 | Footstep Material System | 🟡 Partial | PlayerFootstepSoundEmitter exists; material mapping unclear |
| 19 | Player Behaviour Classification (K-Means) | 🟡 Partial | DirectorController + KMeansClassifier inner class + PlayerFeatureExtractor; BehaviorSystem wrapper missing |
| 20 | Enemy Footprint Tracking | 🔴 Missing | No `footprint/` package |
| 21 | Sanity Hallucinations | 🟡 Partial | HallucinationEffect.java in sanity/effects; full HallucinationSystem missing |
| 22 | Breath-Hold Mechanic | 🟡 Partial | CameraBreathingController exists; BreathSystem + BreathHudRenderer missing |

---

## Legend

| Icon | Meaning |
|------|---------|
| 🟢 Done | All files from master plan are present and the package is wired up |
| 🟡 Partial | Some files exist but the system is incomplete vs. the master plan spec |
| 🔴 Missing | Zero source files for this system |

---

## Detailed System Audit

---

### System 1 — Event System 🟡 Partial

**Present:**
- `event/GameEvent.java`
- `event/EventAction.java`
- `event/EventSequence.java`
- `event/EventBus.java`
- `event/EventState.java`
- `event/EventContext.java`
- `event/EventLoader.java`
- `event/actions/PlaySoundAction.java`
- `event/actions/SetFlagAction.java`
- `event/actions/ShowSubtitleAction.java`
- `event/actions/WaitAction.java`
- `event/actions/FireEventAction.java`
 - `event/EventRegistry.java`
 - `event/actions/PropagateGraphAction.java`
 - `event/actions/PlayAnimationAction.java`
 - `event/actions/TriggerJumpScareAction.java`
 - `event/actions/TransitionLevelAction.java`
 - `event/actions/CheckpointAction.java`
 - `event/actions/StoryEventAction.java`
 - `event/actions/LogAction.java`

**Missing from master plan:**
- (none of the core action stubs remain missing; additional action implementations still need full wiring)

**EventContext status:** `EventContext` constructor remains unchanged for core fields, but nullable accessor stubs were added for systems required by the master plan (sanity, jump scare director, dialogue, debug console, story, inventory, flashlight, behavior). These accessors return null until the real systems are wired into the runtime context.

---

### System 2 — Sound System (WAV Layer) 🟢 Done

**Present:**
- `audio/GameAudioSystem.java`
- `audio/SoundBank.java`
- `audio/AmbienceTrack.java`
- `audio/AudioChannel.java`
- `audio/AudioChannelConfig.java`
- `audio/SpatialSfxEmitter.java`
- `audio/WorldSoundEmitter.java`
- Full `sound/` propagation package: `AcousticGraphEngine`, `SoundPropagationOrchestrator`, `DijkstraPathfinder`, `GraphNode`, `GraphEdge`, `GraphPopulator`, `PropagationResult`, `ReflectionEngine`, `SoundEvent`, `SoundSource`, `HearingCategory`, `MicInputListener`, `RealtimeMicSystem`, + viz layer

**No gaps identified.** System appears complete.

---

### System 3 — Trigger System 🟡 Partial

**Present:**
- `trigger/Trigger.java`
- `trigger/TriggerEvaluator.java`
- `trigger/TriggerEvaluationContext.java`
- `trigger/TriggerLoader.java`
- `trigger/conditions/ZoneTrigger.java`

**Missing from master plan:**
- `trigger/conditions/CompoundTrigger.java` — the AND/OR combinator for flag + zone checks

---

### System 4 — Auto-Bullet from `-map.gltf` 🟢 Done

**Present:**
- `map/MapCollisionBuilder.java`
- `map/LoadedMap.java`
- `map/MapLoader.java`
- `map/MapDocument.java`
- `map/MapDocumentLoader.java`
- `map/MapDocumentSerializer.java`
- `map/MapObject.java`
- `map/MapObjectType.java`

**No critical gaps.** Collision from GLTF mesh is covered by `MapCollisionBuilder`.

---

### System 5 — Map Editor (Swing Tool) 🟡 Partial

**Present:**
- `devTest/ModelDebugScreen.java` — appears to be a GDX-side debug viewer
- `devTest/FilePicker.java`
- `lwjgl3/DesktopFilePicker.java`

**Unclear:** The master plan specifies a dedicated Swing `MapEditorWindow` that can add/remove/position `MapObject` entries and save `resonance-map.json`. Whether `ModelDebugScreen` satisfies this or whether the full Swing editor is missing needs manual verification. Treat as partial until confirmed.

---

### System 6 — Enemy AI System 🟢 Done

**Present:**
- `enemy/EnemyController.java`
- `enemy/EnemyStateMachine.java`
- `enemy/EnemyState.java`
- `enemy/EnemyPerception.java`
- `enemy/EnvironmentReactor.java`
- `enemy/LightFlickerController.java`
- `enemy/LightSource.java`
- `enemy/LightSourceRegistry.java`

**No critical gaps** for the core AI loop (PATROL → INVESTIGATE → CHASE → STUNNED). `LightFlickerController` and `LightSourceRegistry` appear to be enemy-adjacent lighting helpers.

---

### System 7 — Door & Interactable System 🟡 Partial

**Present:**
- `interactable/Interactable.java`
- `interactable/InteractableRegistry.java`
- `interactable/door/DoorController.java`
- `interactable/door/DoorCreakSystem.java`
- `interactable/door/DoorLockState.java`
- `interactable/door/DoorGrabInteraction.java` ← v2 drag state machine
- `interactable/door/DoorBargeDetector.java` ← v2 sprint-barge check
- `interactable/door/DoorKnobCollider.java` ← v2 handle sensor

**Gaps vs v2:**
- `DoorController` still retains the legacy toggle/lerp interaction path; it now has v2 helpers, but the screen-level wiring still needs to swap to the new drag model.
- `RaycastInteractionSystem` still targets the generic interact key flow; v2 knob-first prompt handling is not wired.
- `DoorKnobCollider` exists, but there is no registry yet for associating knob bodies back to doors during ray tests.

**Next door work:** wire the new drag/barge helpers into the test screen and interaction path, then retire the old toggle path in the v2 testbed.

---

### System 8 — Raycast Interaction System 🟢 Done

**Present:**
- `interaction/RaycastInteractionSystem.java`
- `interaction/InteractionPromptRenderer.java`
- `interaction/InteractionResult.java`
- `player/PlayerInteractionSystem.java`

**No critical gaps.**

---

### System 9 — Dynamic Lighting System 🟢 Done

**Present:**
- `lighting/LightManager.java`
- `lighting/GameLight.java`
- `lighting/FlickerController.java`
- `lighting/LightReactorListener.java`
- `lighting/LightingTier.java`

**No critical gaps.**

---

### System 10 — Sanity / Fear System 🟢 Done

**Present:**
- `sanity/SanitySystem.java`
- `sanity/SanityDrainSource.java`
- `sanity/SanityEffect.java`
- `sanity/drains/DarknessPresenceDrain.java`
- `sanity/drains/EnemyProximityDrain.java`
- `sanity/drains/EventDrain.java`
- `sanity/effects/ScreenVignetteEffect.java`
- `sanity/effects/AudioGlitchEffect.java`
- `sanity/effects/ShaderDistortionEffect.java`
- `sanity/effects/HallucinationEffect.java`

**No critical gaps.** All drain sources and effects are present.

---

### System 11 — Jump Scare Director 🟢 Done

**Present:**
- `scare/JumpScareDirector.java`
- `scare/JumpScare.java`
- `scare/ScareType.java`

**No critical gaps.** K-Means setters on `JumpScareDirector` should be verified once System 19 BehaviorSystem wrapper is complete.

---

### System 12 — Dialogue & Subtitle System 🔴 Missing

**Present:**
- `dialogue/DialogueSystem.java` (minimal stub)
- `dialogue/SubtitleRenderer.java` (minimal stub)

**Notes:** subtitle queue and basic rendering stubbed; full dialogue sequencing and voice playback remain to implement.

---

### System 13 — Save / Checkpoint System 🔴 Missing

**Present:**
- `save/SaveSystem.java` (minimal save/load stub)
- `save/SaveData.java` (DTO)

**Notes:** basic local JSON save/load added; checkpoint wiring and autosave integration remain.

---

### System 14 — Story System 🔴 Missing

**Present:**
- `story/StorySystem.java` (minimal stub)
- `story/StoryGate.java` (helper)
- `event/actions/StoryEventAction.java` (notifies StorySystem)

**Notes:** Story scaffolding present; full loader, chapter/beat types, and persistence remain.

---

### System 15 — Room Transition System 🔴 Missing

**Present:**
- `transition/RoomTransitionSystem.java` (fade logic stub)
- `transition/FadeOverlay.java` (render helper)
- `event/actions/TransitionLevelAction.java` (stub)

**Notes:** Fade/trigger scaffolding added; integration with map triggers and HUD will be wired next.

---

### System 16 — In-Game Debug Console 🟡 Partial

**Present:**
- `devTest/universal/diagnostics/DiagnosticOverlay.java`
- `devTest/universal/diagnostics/SystemStatePanel.java`
- `devTest/universal/diagnostics/PerformanceCounter.java`
- `devTest/universal/diagnostics/TabCycler.java`
**Present:**
- `devTest/universal/diagnostics/DiagnosticOverlay.java`
- `devTest/universal/diagnostics/SystemStatePanel.java`
- `devTest/universal/diagnostics/PerformanceCounter.java`
- `devTest/universal/diagnostics/TabCycler.java`
- `debug/DebugConsole.java` (minimal registry + parser)
- `debug/DebugCommand.java`
- `debug/built_in/StoryStatusCommand.java`
- `debug/built_in/BeatCommand.java`

**Missing from master plan:**
- `debug/commands/TeleportCommand.java`
- `debug/commands/FireEventCommand.java`
- `debug/commands/SetSanityCommand.java`
- `debug/commands/BehaviorCommand.java`
- `debug/commands/HallucinateCommand.java`
- `debug/commands/StoryCommand.java` (richer story tools)

The `DiagnosticOverlay` and friends are a passive HUD. The debug console overlay (tilde key `~`) is still not hooked into input, but a minimal `DebugConsole` and a couple of built-in commands are now present for runtime wiring.

---

### System 17 — Settings System 🟢 Done

**Present:**
- `settings/SettingsSystem.java`
- `settings/SettingsData.java`
- `settings/KeybindRegistry.java`

**No critical gaps.**

---

### System 18 — Footstep Material System 🟡 Partial

**Present:**
- `player/PlayerFootstepSoundEmitter.java`

**Potentially missing:**
- A `footstep/FootstepMaterialRegistry.java` or similar class that maps GLTF mesh names / acoustic material tags to footstep sound banks. Verify whether this logic lives inside `PlayerFootstepSoundEmitter` or needs a dedicated registry.

---

### System 19 — Player Behaviour Classification (K-Means) 🟡 Partial

**Present:**
- `director/DirectorController.java` — contains an inner `KMeansClassifier` class
- `player/PlayerFeatureExtractor.java` — feature sampling
- `player/SimplePanicModel.java` — lightweight panic model

**Missing from master plan:**
- `behavior/BehaviorSystem.java` — the public-facing facade that other systems call
- `behavior/PlayerArchetype.java` enum (METHODICAL, IMPULSIVE, PANICKED, NEUTRAL)
- `behavior/BehaviorSampleWindow.java`
- `behavior/BehaviorChangeDetector.java`
- `behavior/BehaviorNormalizer.java`

The K-Means logic is implemented inside `DirectorController` as an inner class. It needs to be exposed as a proper `BehaviorSystem` that `EventContext` and other systems can reference.

---

### System 20 — Enemy Footprint Tracking 🔴 Missing

**Present:** Nothing.

**Must create:**
- `footprint/FootprintTrail.java`
- `footprint/FootprintStamp.java`
- `footprint/FootprintTracker.java`
- `footprint/TrailFollowBehavior.java` (hooks into `EnemyStateMachine`)

---

### System 21 — Sanity Hallucinations 🟡 Partial

**Present:**
- `sanity/effects/HallucinationEffect.java` — the screen/audio effect

**Missing from master plan:**
- `hallucination/HallucinationSystem.java` — the scheduler that fires hallucination events at the right sanity thresholds
- `hallucination/HallucinationType.java` enum (FAKE_FOOTSTEP, FAKE_DOOR_CREAK, VISUAL_FLICKER, etc.)
- Integration with K-Means archetype (PARANOID threshold at 60 vs 50)

---

### System 22 — Breath-Hold Mechanic 🟡 Partial

**Present:**
- `player/CameraBreathingController.java` — camera bob/breathing animation

**Missing from master plan:**
- `breath/BreathSystem.java` — stamina, hold state, exhale event
- `breath/BreathHudRenderer.java` — arc HUD near crosshair
- Integration with `PlayerFootstepSoundEmitter` (volume suppression at 30%)
- Integration with `RealtimeMicSystem` (`micSuppressionMult()`)
- Integration with `EnemyPerception` (enemy ignores sounds while held)

`CameraBreathingController` is a visual aid only; the actual breath-hold gameplay logic (`BreathSystem`) does not exist yet.

---

## What To Build Next (Recommended Order)

These are ordered by dependency — earlier items unblock later ones.

```
Priority 1 — Unblocks everything narrative
  → System 12: Dialogue & Subtitle System  (dialogue/)
  → System 14: Story System                (story/)
  → System 13: Save / Checkpoint System    (save/)
  → System 15: Room Transition System      (transition/)

Priority 2 — Debug tooling (speeds up all testing)
  → System 16: Debug Console               (debug/)

Priority 3 — Complete partial systems
  → System 7:  Door rewrite wiring in v2 testbed
  → System 1:  Remaining EventContext fields + missing actions
  → System 3:  CompoundTrigger
  → System 19: BehaviorSystem facade       (behavior/)
  → System 22: BreathSystem + BreathHudRenderer (breath/)
  → System 21: HallucinationSystem         (hallucination/)
  → System 18: FootstepMaterialRegistry    (verify or create)

Priority 4 — New systems
  → System 20: Enemy Footprint Tracking    (footprint/)

Priority 5 — Integration
  → System 23: UniversalTestScreen scaffold / v2 testbed wiring
  → Wire all systems into UniversalTestScreen / v2 testbed
  → Run Acceptance Criteria checklist from master plan
```

---

## File Count by Package

| Package | Files present |
|---------|--------------|
| `audio/` | 7 |
| `sound/` | ~45 (inc. viz/) |
| `event/` + `event/actions/` | 12 |
| `trigger/` + `trigger/conditions/` | 5 |
| `map/` | 8 |
| `model/` | 9 |
| `enemy/` | 8 |
| `interactable/` | 5 |
| `interaction/` | 3 |
| `lighting/` | 5 |
| `sanity/` | 10 |
| `scare/` | 3 |
| `settings/` | 3 |
| `player/` | 12 |
| `director/` | 1 |
| `items/` | 3 |
| `particles/` | 20+ |
| `rendering/` | 10+ |
| `multiplayer/` | 6 |
| `devTest/` | 20+ |
| `dialogue/` | **0 — MISSING** |
| `save/` | **0 — MISSING** |
| `story/` | **0 — MISSING** |
| `transition/` | **0 — MISSING** |
| `debug/` | **0 — MISSING** |
| `behavior/` | **0 — MISSING** |
| `footprint/` | **0 — MISSING** |
| `hallucination/` | **0 — MISSING** |
| `breath/` | **0 — MISSING** |

---

## Notes for Copilot

- All source is under `core/src/main/java/io/github/superteam/resonance/`
- Build system: Gradle
- Framework: LibGDX (gdx imports throughout)
- The `UniversalTestScreen.java` in `lwjgl3` is now the canonical integration testbed; `GltfMapTestScene` remains legacy
- `UniversalTestScreen.java`, `UniversalTestScene.java`, `FlyModeController.java`, and `TestMapLayout.java` now exist as a v2 scaffold in `lwjgl3/src/main/java/io/github/superteam/resonance/devTest/universal/`
- `EventContext` is the dependency-injection hub — once new systems are built, add them as fields there
- The master plan spec in `RESONANCE_COMBINED_MASTER_PLAN_v2.md` is the single source of truth for API design; follow it exactly when implementing missing systems
- Test classes exist alongside source in `player/` (e.g. `ImpactListenerTest.java`) — continue this pattern

---

*Update this file at the end of every dev session.*

# Sprint 0 Progress Tracker

Last Updated: 2026-04-21

## Iteration Workflow
- Complete one implementation batch.
- Run compile verification.
- Record deltas, risks, and next batch.

## Batch 1 (Requested Atomic Batch)
Scope: Fixes M, C, B, J, D
Status: COMPLETE

### Completed
- [x] Fix M: Removed sonar-triggered blind light expansion in BlindEffectController.onSoundEvent.
- [x] Fix C: Raised corridor wall height from 1.5 to 3.5 and centerY from 0.75 to 1.75.
- [x] Fix B: Added static Bullet floor plane collider and adjusted carriable spawn Y to stay above floor.
- [x] Fix J: Tuned long-range sonar reveal config and pulse lifetime.
- [x] Fix D: Changed pulse origin to player world position.

### Verification
- [x] Changed Java files report no editor problems.
- [x] Batch config files updated and valid JSON.
- [ ] Manual in-engine check for floor stability and long-range pulse readability.
- [ ] Full Gradle compile output capture (terminal output currently not being returned by tool in this session).

### Changed Files
- core/src/main/java/io/github/superteam/resonance/rendering/blind/BlindEffectController.java
- core/src/main/java/io/github/superteam/resonance/devTest/universal/UniversalTestScene.java
- assets/config/blind_effect_config.json
- assets/config/balancing_config.json
- assets/particles/presets/sonar_pulse.json

## Remaining Sprint 0
- [ ] Fix E - Bounce points on walls (vertical spread ray generation).
- [ ] Fix F - Mic reliability polish (fallback key path, HUD visibility, debug output).
- [ ] Fix G - Proper HUD (voice meter + stamina bar).
- [ ] Fix H - Atmospheric fog/mist integration.
- [ ] Fix I - Crouch alcove low-ceiling collider.
- [ ] Fix K - Camera idle breathing animation.
- [ ] Fix L - Item system polish completion.

## Phase 1 Kickoff (after Sprint 0)
- [ ] Phase 1.1 kickoff subset (propagation chain + diagnostics).
- [ ] Phase 1.2 kickoff subset (item interaction acoustic loop).

## Notes
- Existing workspace diagnostics include multiple pre-existing unused-field/import warnings outside this batch.
- No unrelated file reversions were performed.

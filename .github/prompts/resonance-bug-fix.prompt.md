---
description: "Fix Resonance gameplay and audio regressions"
argument-hint: "Describe the affected symptoms or subsystem"
agent: "agent"
---
Investigate and fix the reported Resonance bug cluster with the smallest root-cause change set.

Current symptoms to address:
- Items are passing through walls
- Jumping does not produce a sound event
- Bunny hopping is still possible and should be prevented
- All sound events produce the same pulse strength
- The microphone barely hears the player's voice

Work from the currently selected code or open file first. If no selection is available, start from the most relevant gameplay scene and related systems, then trace into the owning collision, movement, audio, pulse, and microphone code paths.

Follow this workflow:
1. Identify the controlling code path for each symptom before editing.
2. Prefer root-cause fixes in the subsystem that owns the behavior.
3. For item collisions, check physics shapes, collision filtering, movement integration, and any swept or continuous collision handling.
4. For jump audio and bunny-hop prevention, verify grounded-state logic, jump-trigger timing, cooldowns, and the code path that emits the jump sound.
5. For pulse strength, confirm different sound classes map to different strengths instead of reusing a single constant.
6. For microphone gain, inspect input normalization, scaling, thresholds, and any clamping that may suppress voice level.
7. Keep movement, sound, and input changes consistent with the game's existing feel.
8. Avoid unrelated refactors.
9. After editing Java code, run the narrowest useful verification, and compile the affected Gradle module if needed.
10. Summarize the cause, the fix, and any remaining risk clearly.

When responding, report:
- The root cause(s)
- The files changed
- The verification performed
- Any follow-up issues that still need attention

# Update Task Plan — Sophisticated Implementation

## Executive Overview
A comprehensive four-task modernization cycle integrating **player audiovisual immersion**, **integrated developer testing**, **dynamic blindness mechanics**, and **3D acoustic visualization**. Each task includes complete phase breakdowns, job assignments, architectural fixes, performance optimizations, edge-case handling, integration tests, and re-fix protocols.

**Project Scope:** All major systems (Player, Sound, Particles, Items, Rendering, Physics, UI) except Model Animation.  
**Testing Strategy:** 54+ unit/integration tests + manual validation on all 7 Universal Test Scene zones.  
**Performance Targets:** FOV pass <2ms, blind effect <0.5ms, sound viz <1.5ms per frame.

## Implementation Status Board

**Document State:** Active implementation tracker  
**Start Date:** 2026-04-17  
**Overall Program Status:** IN PROGRESS

| Workstream | Status | Owner | Gate |
|------|------|------|------|
| Task 1: BodyCam + Unified VHS | Ready to Start | Graphics/Rendering | Shader compile + FBO smoke test |
| Task 2: Universal Test Scene | In Progress | Engine/Gameplay | Zone shell + movement walkthrough |
| Task 3: Blind Effect Stack | Ready to Start | Gameplay/Graphics | Blind baseline visible on retro + player shaders |
| Task 4: 3D Acoustic Visualization | Ready to Start | Audio/Physics | Mic event chain and 3D viz trigger |
| Task 5: Blind Gameplay Integration | Blocked by Task 3 | Gameplay/Level | Panic + flare + sonar balancing pass |

### Immediate Execution Order
1. Task 1.1 + 1.2 + 1.3 (BodyCam shader, settings, framebuffer wiring).
2. Task 3.1 (blind shader parity fix) before Task 3.2+.
3. Task 2.1 + 2.2 (Universal scene shell) in parallel with Task 1 integration.
4. Task 4.1 + 4.2 + 4.3 after Task 2 SoundPropagationZone shell exists.
5. Task 5 once Task 3 modifier stack is stable and tunable.

---

# TASK 1: Body Camera FOV Effect with Unified VHS Integration

## 1.1 Strategic Overview
Replace the standalone VHS post-process shader with a unified **BodyCamVHSShader** that combines:
- **Wide-angle barrel distortion** (95–170° diagonal FOV mapping)
- **Chromatic aberration** (red/green/blue channel separation for analog body cam feel)
- **Vignette fade** (natural light falloff at edges)
- **CRT scan lines + VHS tape artifacts** (existing VHS logic retained)

All parameters tuned via `BodyCamSettings.json` at runtime without shader recompilation.

### 1.2 Architecture

#### Class Structure
```
io.github.superteam.resonance.rendering
├── BodyCamVHSSettings (Config holder)
├── BodyCamVHSShaderLoader (Async shader compilation + validation)
├── BodyCamVHSVisualizer (Render orchestrator)
└── BodyCamPassFrameBuffer (Dedicated FBO for body cam layer)
```

#### Render Pipeline Integration
```
Scene Render (PlayerTestScreen)
  ↓
[FrameBuffer 1] Main scene → color texture
  ↓
[BodyCamPassFrameBuffer] color texture → BodyCamVHS shader → output texture
  ↓
[Original VHS FBO] REMOVED (unified into BodyCam)
  ↓
Screen composite
```

#### JSON Config: `assets/config/body_cam_settings.json`
```json
{
  "fov_diagonal_degrees": 160.0,
  "barrel_distortion_strength": 0.35,
  "chromatic_aberration_pixels": 2.4,
  "vignette_radius": 0.85,
  "vignette_softness": 0.3,
  "vhs_scan_line_strength": 0.15,
  "vhs_tape_noise_amount": 0.08,
  "crt_curve_amount": 0.12,
  "enabled": true
}
```

### 1.3 Phases

#### **Phase 1.1: Shader Development & Validation**
**Owner:** Graphics Engineer  
**Effort:** 3–4 days

**Jobs:**
- [ ] Study barrel distortion math: inverse mapping UV coordinates using radial distance + polynomial correction.
- [ ] Draft `body_cam_vhs.vert` with vertex position transform for wide-angle projection.
- [ ] Draft `body_cam_vhs.frag` with:
  - Barrel distortion (UVs remapped via `r = sqrt(u² + v²)`; new UV = `(u/r, v/r) * distortion_map(r)`)
  - Chromatic aberration (sample R, G, B at slightly offset UVs)
  - Vignette (radial smoothstep fade from center)
  - VHS artifacts (scan lines, tape noise) from existing `vhs_postprocess.frag`
- [ ] Unit test shader samplers with known inputs (test distortion curves match reference).

**Fixes:**
- [ ] Distortion edge clipping: Use `fract()` wrapping or hard clamping to prevent texture underflow.
- [ ] Chromatic fringing on dark edges: Pre-multiply by scene alpha to reduce color bleeding.

**Improvements:**
- [ ] Cache distortion LUT (look-up table) as a precomputed texture to avoid per-frame math.
- [ ] Support two distortion modes: "barrel" (classic) and "pincushion" (concave).

**Tests:**
- [ ] Shader compiles without errors on desktop + mobile (GLSL 120 compat).
- [ ] Distortion produces expected barrel shape with known FOV input.
- [ ] Chromatic aberration offset is pixel-perfect at known resolution.

---

#### **Phase 1.2: Config & Settings Infrastructure**
**Owner:** Tools Engineer  
**Effort:** 1–2 days

**Jobs:**
- [ ] Create `BodyCamVHSSettings` class (extends `JsonConfigurable`):
  ```java
  public class BodyCamVHSSettings extends JsonConfigurable {
      public float fovDiagonalDegrees = 160.0f;
      public float barrelDistortionStrength = 0.35f;
      // ... all 8 parameters
      public void validate() { /* clamp ranges */ }
  }
  ```
- [ ] Create `BodyCamSettingsStore` (singleton) to load/save/hot-reload from JSON.
- [ ] Add HUD sliders in `PlayerTestScreen` to adjust each parameter in real-time.

**Fixes:**
- [ ] JSON parsing failure recovery: Default to hardcoded fallback values.
- [ ] Parameter range clamping: Barrel [0.0–0.5], chromatic [0–8px], FOV [60–170°].

**Improvements:**
- [ ] Profile presets: "Axon Body 4 160°", "Wide Fisheye 170°", "Narrow 95°".
- [ ] Export/import settings from clipboard for quick iteration.

**Tests:**
- [ ] Load valid JSON, verify all fields deserialized correctly.
- [ ] Load missing JSON, verify fallback defaults applied.
- [ ] Modify settings via HUD slider → shader uniforms updated → visual change immediate.

---

#### **Phase 1.3: Framebuffer Pass Integration**
**Owner:** Rendering Engineer  
**Effort:** 2–3 days

**Jobs:**
- [ ] Create `BodyCamPassFrameBuffer` class:
  - Allocate 2 FBOs: `sceneColorFBO` (main scene), `bodyCamOutputFBO` (post-process target)
  - Attach appropriate texture formats (RGBA8888 for color, RGBA4444 for performance)
- [ ] Modify `PlayerTestScreen.render()` call chain:
  ```
  renderScene() → sceneColorFBO
  renderBodyCamPass(sceneColorFBO.colorTexture) → bodyCamOutputFBO
  renderToScreen(bodyCamOutputFBO.colorTexture)
  ```
- [ ] Wire `BodyCamVHSShaderLoader` to compile + bind shader at startup.

**Fixes:**
- [ ] FBO attachment mismatch: Ensure color texture format matches shader expectations.
- [ ] GPU memory leaks: Dispose FBOs in `dispose()` callback.
- [ ] Depth attachment: Only attach if deferred rendering planned; otherwise skip for performance.

**Improvements:**
- [ ] Async FBO allocation on background thread during screen transition.
- [ ] FBO validation: Check for GL errors post-attachment and log diagnostics.

**Tests:**
- [ ] FBO creation succeeds; color attachment binds correctly.
- [ ] Scene renders to FBO without visual artifacts or black screen.
- [ ] Switching body-cam on/off toggles the BodyCamPassFrameBuffer without crashes.

---

#### **Phase 1.4: Shader Uniform Updates & Animation**
**Owner:** Rendering Engineer  
**Effort:** 1–2 days

**Jobs:**
- [ ] Create `BodyCamVHSAnimator` (updates uniforms each frame):
  - Distortion wobble: Slight sinusoidal variation in barrel strength (simulates camera movement).
  - VHS tape speed variation: Horizontal scan line drift.
  - Vignette breathing: Subtle radial pulse.
- [ ] Hook animator to `BodyCamPassFrameBuffer.update(deltaSeconds)`.

**Fixes:**
- [ ] Uniform update order: Ensure all uniforms set before shader bind.
- [ ] Floating-point precision: Use `float32` for time values to prevent jitter over long gameplay.

**Improvements:**
- [ ] Per-zone distortion profiles: Sound room uses heavier distortion; player room uses subtle.
- [ ] Panic modifier: On player stress event, temporarily increase distortion (visual feedback).

**Tests:**
- [ ] Distortion wobble updates smoothly over 3–5 seconds without popping.
- [ ] Vignette breathing synchronized across all zones.

---

#### **Phase 1.5: Integration & End-to-End Validation**
**Owner:** QA Lead  
**Effort:** 2–3 days

**Jobs:**
- [ ] Render body-cam effect on all 4 existing test screens (PlayerTestScreen, SoundTestScreen, etc.).
- [ ] Benchmark: Measure FBO pass time on target hardware (desktop, mobile).
- [ ] Visual comparison: Side-by-side with standard camera (before/after).
- [ ] Document unified shader in [CODING_STANDARDS.md](CODING_STANDARDS.md).

**Fixes:**
- [ ] Performance regression: If FBO pass > 2ms, reduce distortion LUT resolution or simplify VHS noise.
- [ ] Mobile compatibility: Test GLES 2.0 shader compilation; remove unsupported syntax.
- [ ] Z-fighting with VHS artifacts: Adjust depth bias or blend order.

**Improvements:**
- [ ] Shader caching: Pre-compile at startup; skip on subsequent loads.
- [ ] Fallback shader: If BodyCamVHS fails to compile, revert to simple identity pass.

**Tests:**
- [ ] All 4 test screens render body-cam effect without freezing.
- [ ] FBO memory usage stays within 50MB on desktop, 20MB on mobile.
- [ ] Disabling body-cam restores original VHS rendering (no visual diff).
- [ ] Performance: 60 FPS on target device with body-cam enabled.

---

#### **Phase 1.6: Re-fixes & Iteration**
**Owner:** Graphics Engineer  
**Effort:** 1 day (post-QA feedback)

**If Performance Issue Found:**
- Reduce distortion LUT texture resolution (512×512 → 256×256).
- Cache computed distortion coordinates per-pixel (half resolution pass, then upscale).
- Disable chromatic aberration on mobile; enable only on desktop.

**If Visual Artifacts Found:**
- Adjust edge clipping logic (fract vs clamp).
- Fine-tune chromatic offset per channel (R: +1px, G: 0, B: –1px).
- Add post-distortion AA filter to reduce aliasing.

---

### 1.4 Test Summary (Task 1)
| Test ID | Category | Scope | Expected Result |
|---------|----------|-------|-----------------|
| T1.01 | Unit | Shader compilation | Compiles without errors (all platforms) |
| T1.02 | Unit | Distortion LUT | Output UV coords within [0, 1] range |
| T1.03 | Integration | FBO lifecycle | Create → render → dispose without leaks |
| T1.04 | Integration | Uniform updates | Changes visible in real-time via HUD |
| T1.05 | Performance | Desktop (60 FPS) | FBO pass ≤ 2.0ms |
| T1.06 | Performance | Mobile (30 FPS) | FBO pass ≤ 3.0ms |
| T1.07 | Visual | Scene render | Scene visible through barrel distortion |
| T1.08 | Visual | VHS + Distortion | Both effects blend smoothly (no banding) |

---

# TASK 2: Universal Test Scene Screen — Modular 7-Zone Walkable Environment

## 2.1 Strategic Overview
Consolidate 4 separate test screens (PlayerTestScreen, SoundTestScreen, ParticleTestScreen, RetroShaderScreen) into a **single first-person walkable space** with 7 distinct environmental zones. Each zone isolates one system for testing while allowing cross-system interaction. Tab cycling HUD overlay shows deep diagnostics (particle count, sound propagation graph, inventory state, etc.) without breaking immersion.

### 2.2 Architecture

#### Class Structure
```
io.github.superteam.resonance.devTest.universal
├── UniversalTestScene (Main screen, extends ScreenAdapter)
├── TestZone (Abstract base for each zone)
├── zones/
│   ├── RampStairsZone (Player movement, footstep acoustics)
│   ├── CrouchAlcoveZone (Player crouch, item pickup)
│   ├── ParticleArenaZone (Particle systems + physics)
│   ├── SoundPropagationZone (Dijkstra graph, acoustic materials)
│   ├── ItemInteractionZone (CarrySystem, InventorySystem)
│   ├── ShaderCorridorZone (Retro shader, body-cam distortion)
│   └── BlindChamberZone (Blind effect visualization, sonar reveal)
├── diagnostics/
│   ├── DiagnosticOverlay (HUD layer)
│   ├── PerformanceCounter (FPS, GPU time, memory)
│   ├── SystemStatePanel (Per-zone module state)
│   └── TabCycler (Keyboard-driven diagnostic tabs)
```

#### Geometry Layout (Top-down view)
```
                    [7. Blind Chamber]
                          |
    [6. Shader Corridor]--[Center Hub]--[1. Ramp/Stairs]
                          |
                    [2. Crouch Alcove]
                          |
        [5. Item Room]--[Sound Room]--[4. Particle Arena]
                     [3. Sound Propagation]
```

**Each zone:** 20–30m² enclosure with distinct floor texture, wall material (varies acoustic properties), and lighting.

#### Zone Specifications

| Zone | Purpose | Floor Texture | Walls | Lighting | Audio Material |
|------|---------|---------------|-------|----------|-----------------|
| 1. Ramp/Stairs | Movement tests | Concrete tile | Drywall (2cm) | Harsh white | Hard (0.8) |
| 2. Crouch Alcove | Prone/crouch | Wood planks | Stone (20cm) | Dim blue | Hard (0.9) |
| 3. Sound Prop | Graph debug | Linoleum | Mixed (5cm avg) | Neutral | Variable |
| 4. Particle Arena | FX tests | Metal grid | Polycarbonate | Colored strobe | Absorb (0.2) |
| 5. Item Room | Inventory | Rubber mat | Fabric-lined | Soft amber | Absorb (0.3) |
| 6. Shader Corridor | Rendering | Checkered tile | Mirror panels | Dynamic | Hard (0.95) |
| 7. Blind Chamber | Blindness + Sonar | Featureless gray | Featureless | Adaptive | Neutral (0.5) |

### 2.3 Phases

#### **Phase 2.1: Architecture Design & Zone Abstraction**
**Owner:** Engine Architect  
**Effort:** 2–3 days

**Jobs:**
- [ ] Design `TestZone` abstract base class:
  ```java
  public abstract class TestZone {
      abstract void setUp(); // Zone initialization
      abstract void update(float deltaSeconds); // Per-frame logic
      abstract void render(PerspectiveCamera camera); // Render zone-specific geometry
      abstract String getZoneName();
      abstract SystemState getSystemState(); // For HUD diagnostics
  }
  ```
- [ ] Define `SystemState` interface (per-zone telemetry):
  ```java
  public interface SystemState {
      Map<String, Object> getMetrics(); // e.g., {particleCount: 512, soundNodes: 240}
      boolean isHealthy(); // Red/green status on HUD
  }
  ```
- [ ] Sketch zone geometry (7 enclosures + connection hallways).

**Fixes:**
- [ ] Zone isolation: Ensure physics bodies don't leak between zones (separate collision groups).
- [ ] Camera clipping: Invisible walls at zone boundaries prevent player walk-through.

**Improvements:**
- [ ] Zone activation: Only active zone updates/renders (saves CPU).
- [ ] Async zone loading: Preload adjacent zone in background during player movement.

**Tests:**
- [ ] TestZone implementations compile and instantiate.
- [ ] SystemState metrics accessible via `getMetrics()`.

---

#### **Phase 2.2: UniversalTestScene Core Structure**
**Owner:** Engine Architect  
**Effort:** 2 days

**Jobs:**
- [ ] Create `UniversalTestScene extends ScreenAdapter`:
  ```java
  public class UniversalTestScene extends ScreenAdapter {
      private Array<TestZone> zones; // All 7 zones
      private TestZone activeZone; // Current zone
      private PerspectiveCamera camera;
      private PlayerController playerController;
      private DiagnosticOverlay diagnosticHUD;
      
      public void create() {
          zones.add(new RampStairsZone());
          zones.add(new CrouchAlcoveZone());
          // ... all 7
      }
      
      public void render(float delta) {
          activeZone.update(delta);
          activeZone.render(camera);
          diagnosticHUD.draw();
      }
  }
  ```
- [ ] Implement player movement system (extend PlayerController for walkable space).
- [ ] Add zone transition triggers (invisible volumes at exits).

**Fixes:**
- [ ] Camera jitter during zone transitions: Smooth interpolation over 0.3s.
- [ ] Physics reset on zone entry: Dispose old zone's physics world, create new one.

**Improvements:**
- [ ] Zone memory: Save player position/state on exit, restore on re-entry.
- [ ] Persistent inventory: Items carried across zones.

**Tests:**
- [ ] UniversalTestScene instantiates all 7 zones without errors.
- [ ] Player movement works in main hub.
- [ ] Transitioning to adjacent zone succeeds.

---

#### **Phase 2.3: Individual Zone Implementation**
**Owner:** System Engineers (2–3 person team)  
**Effort:** 4–5 days

**Zone 1: RampStairsZone**
- [ ] Mesh: Ramp (15° incline) + staircase (8 steps).
- [ ] Player footstep integration: Trigger `PlayerFootstepSoundEmitter` on each step.
- [ ] SystemState: Report current MovementState, footstep frequency.

**Zone 2: CrouchAlcoveZone**
- [ ] Mesh: 5m × 3m alcove with 1.8m height (forces crouch).
- [ ] TestCrate physics object: Player can pickup/drop/throw.
- [ ] Inventory UI: Show item carry capacity.
- [ ] SystemState: Inventory slots, carried item count.

**Zone 3: SoundPropagationZone**
- [ ] Mesh: 20m × 15m room with angled walls (30° facets) for reflections.
- [ ] Dijkstra graph debug visualization: Nodes as spheres, edges as lines (color-coded by edge weight).
- [ ] Static sound source: Emit fixed noise; trace propagation paths.
- [ ] SystemState: Graph node count, edge count, propagation time per source.

**Zone 4: ParticleArenaZone**
- [ ] Mesh: Tall chamber (10m height) with collision floor/ceiling.
- [ ] Particle system: Spawn multiple emitters (fireball, explosion, smoke).
- [ ] ForceField testing: Wind, turbulence, point attractors.
- [ ] SystemState: Active emitters, total particle count, memory usage.

**Zone 5: ItemInteractionZone**
- [ ] Mesh: Shelved room; items on shelves (CarriableItem instances).
- [ ] Pickup system: Player picks up flares, crates, sound sources.
- [ ] InventorySystem HUD: Slot grid, item properties.
- [ ] SystemState: Inventory state, nearby items count.

**Zone 6: ShaderCorridorZone**
- [ ] Mesh: Hallway with varied materials (metal, fabric, tile) for shader testing.
- [ ] Retro shader on all surfaces: Enable/disable via Tab key.
- [ ] BodyCamVHS effect: Toggle distortion, chromatic aberration per wall section.
- [ ] SystemState: Active shader effects, distortion parameters.

**Zone 7: BlindChamberZone**
- [ ] Mesh: Featureless 10m cube (all walls same material).
- [ ] Blind effect: Always-on 0.6m baseline visibility.
- [ ] Sonar pulses: Player claps/shouts to trigger reveals (2.5m radius, 1.2s duration).
- [ ] Flare pickup: Extends visibility to 4m for 12s.
- [ ] SystemState: Blind effect parameters, reveal radius, panic level.

**Fixes (All Zones):**
- [ ] Lighting popping: Smooth transition between zone lighting states.
- [ ] Sound leakage: Zone separation prevents audio bleed.
- [ ] Physics garbage collection: Dispose rigid bodies on zone exit.

**Improvements:**
- [ ] Hotspot hints: UI overlays at zone entrance explaining controls ("Press Tab for stats").
- [ ] Zone checkpoints: Save player progress (which zones explored).

**Tests:**
- [x] Each zone instantiates without errors.
- [x] Zone geometry renders without clipping.
- [x] Player can move within each zone without leaving bounds.
- [x] SystemState metrics update in real-time.

---

#### **Phase 2.4: Diagnostic Overlay & HUD System**
**Owner:** UI Engineer  
**Effort:** 2–3 days

**Jobs:**
- [ ] Create `DiagnosticOverlay` (HUD layer):
  ```java
  public class DiagnosticOverlay {
      private TabCycler tabCycler; // Tracks which tab active
      private SystemStatePanel statePanel; // Per-zone metrics
      private PerformanceCounter perfCounter; // FPS, GPU time
      
      public void draw(Stage stage) {
          String activeTab = tabCycler.getActiveTab();
          switch(activeTab) {
              case "PERFORMANCE": perfCounter.draw(); break;
              case "SYSTEM_STATE": statePanel.draw(); break;
              // ...
          }
      }
  }
  ```
- [ ] Implement `TabCycler`: Press Tab → cycle through diagnostic modes (Performance, System State, Audio Graph, Particles, etc.).
- [ ] `PerformanceCounter`: Display FPS, frame time, GPU memory, zone change time.
- [ ] `SystemStatePanel`: Show active zone's metrics in real-time (particle count, sound nodes, etc.).

**Fixes:**
- [ ] HUD performance: Cache text rendering; only update on metric change.
- [ ] HUD clipping: Ensure overlays visible on all screen resolutions (1024×768 to 4K).

**Improvements:**
- [ ] Graph visualization: Show Dijkstra graph as 2D projection on HUD (nodes as dots, edges as lines).
- [ ] Recording: Press R to record performance data to CSV for analysis.

**Tests:**
- [ ] Tab cycling works (4+ tabs).
- [ ] Metrics update every frame.
- [ ] HUD visible on 4 different resolutions without clipping.

---

#### **Phase 2.5: Player Controller Integration**
**Owner:** Gameplay Engineer  
**Effort:** 1–2 days

**Jobs:**
- [ ] Extend `PlayerController` for walkable first-person movement:
  - WASD movement
  - Mouse look (pitch/yaw)
  - Space = jump, Ctrl = crouch, Shift = sprint
  - E = interact with nearby items
  - Tab = cycle diagnostic overlays
- [ ] Zone boundary triggers: Invisible volumes that detect player proximity and trigger zone transitions.
- [ ] Smooth camera transitions: Interpolate FOV/position over 0.3s when changing zones.

**Fixes:**
- [ ] Crouch height clipping: Adjust camera height in crouch alcove to prevent head through ceiling.
- [ ] Jump max height per zone: Ramp zone allows full jump; crouch alcove limits jump.

**Improvements:**
- [ ] Footstep sounds: Play appropriate SoundEvent per floor texture (concrete vs wood).
- [ ] Zone UI hints: On first zone entry, display "Zone 1/7: Ramp/Stairs. Press E to interact."

**Tests:**
- [ ] WASD movement works smoothly.
- [ ] Mouse look responsive (no lag).
- [ ] Jump/crouch state changes reflect in SystemState.
- [ ] Zone transitions seamless (no freezing, camera smooth).

---

#### **Phase 2.6: Integration & Cross-System Testing**
**Owner:** QA Lead  
**Effort:** 3 days

**Jobs:**
- [ ] Test all zone interactions:
  - Player walks to ramp zone, climbs stairs → footstep sounds propagate via AcousticGraphEngine.
  - Player enters particle arena → spawns 500 particles; check frame rate stays >30 FPS.
  - Player uses flare in blind chamber → visibility expands to 4m.
  - Player picks up item in item room → inventory updates; can carry to other zones.
- [ ] Cross-zone continuity: Ensure InventorySystem state persists across zones.
- [ ] Performance profiling: Measure total render time, physics time, AI time per zone.

**Fixes (Post-Testing):**
- [ ] If zone load time > 1s, implement async loading with fade transition.
- [ ] If frame rate drops below 30 FPS in particle arena, reduce max particle count.

**Improvements:**
- [ ] Spawn player at "Welcome Hub" zone on first load; tutorial overlay explains controls.
- [ ] Waypoint system: Numbered zones on minimap (if minimap implemented).

**Tests:**
- [ ] All 7 zones render without errors.
- [ ] Player can visit all zones sequentially (no crashes).
- [ ] Inventory persists across zones.
- [ ] Performance: 60 FPS on desktop, 30 FPS on mobile (avg across all zones).
- [ ] Cross-system interactions work (e.g., footsteps trigger sound propagation).

---

#### **Phase 2.7: Re-fixes & Polish**
**Owner:** Engine Architect + QA  
**Effort:** 1–2 days (post-integration)

**If Performance Issues Found:**
- Reduce particle count limits per zone.
- Simplify Dijkstra graph (fewer nodes per zone).
- Use frustum culling to skip rendering off-screen zones.

**If Cross-System Issues Found:**
- Ensure SoundPropagationOrchestrator correctly references player position in current zone.
- Verify physics bodies don't leak between zone physics worlds.
- Fix inventory persistence (ensure CarriableItem state saved/restored per zone).

---

### 2.4 Test Summary (Task 2)
| Test ID | Category | Scope | Expected Result |
|---------|----------|-------|-----------------|
| T2.01 | Unit | TestZone abstraction | All 7 zones instantiate |
| T2.02 | Integration | Player movement | WASD + mouse look responsive |
| T2.03 | Integration | Zone transitions | Player moves between zones smoothly |
| T2.04 | Integration | Inventory persistence | Items carried across zones |
| T2.05 | Cross-system | Footsteps → Sound | Footstep SoundEvent triggers propagation |
| T2.06 | Cross-system | Particles + Physics | 500 particles don't cause frame drops |
| T2.07 | Performance | Desktop (60 FPS) | Avg frame time ≤ 16.7ms |
| T2.08 | Performance | Mobile (30 FPS) | Avg frame time ≤ 33.3ms |
| T2.09 | HUD | Diagnostic overlay | Tab cycling shows all 5+ metrics |
| T2.10 | Visual | Lighting transitions | No popping when entering new zone |

---

# TASK 3: Blind Effect with Dynamic Reveal Modifiers

## 3.1 Strategic Overview
Implement a **BlindEffectController** that manages a modifier stack for player vision range. **Always-on baseline** (0.6m visibility) is modified by gameplay events:
- **Panic modifier:** Shrinks visibility under stress.
- **Flare modifier:** Expands visibility to 4m for 12s.
- **Sonar reveals:** Clap/shout events (CLAP_SHOUT) reveal 2.5m radius for 1.2s; also triggers AcousticBounce3DVisualizer.

**Infrastructure already exists:** `retro_shader.frag` has `u_blindFogStart`, `u_blindFogEnd`, `u_blindFogStrength`, `u_blindFogColor` uniforms. **Fix:** `player_shader.frag` needs identical blind fog uniforms for consistency.

### 3.2 Architecture

#### Class Structure
```
io.github.superteam.resonance.rendering.blind
├── BlindEffectController (Modifier stack orchestrator)
├── BlindEffectModifier (Interface for each modifier)
├── modifiers/
│   ├── BlindBaselineModifier (0.6m always-on)
│   ├── BlindPanicModifier (Shrinks vision on panic)
│   ├── BlindFlareModifier (Expands 4m for 12s)
│   └── BlindSonarRevealModifier (2.5m for 1.2s, stacks with others)
├── BlindEffectRevealConfig (JSON-backed tuning)
└── BlindVisualizer (Renders blind fog effect in shaders)
```

#### JSON Config: `assets/config/blind_effect_config.json`
```json
{
  "baseline_visibility_meters": 0.6,
  "baseline_fade_edge_softness": 0.3,
  "panic_modifier": {
    "enabled": true,
    "visibility_reduction_factor": 0.5,
    "trigger_panic_threshold": 0.7
  },
  "flare_modifier": {
    "enabled": true,
    "visibility_radius_meters": 4.0,
    "duration_seconds": 12.0
  },
  "sonar_reveal": {
    "enabled": true,
    "clap_shout_radius_meters": 2.5,
    "duration_seconds": 1.2,
    "triggers_acoustic_visualization": true
  },
  "blind_fog_color": [0.0, 0.0, 0.0, 1.0]
}
```

#### Modifier Stack Operation
```
Update Order (each frame):
1. Remove expired modifiers (duration_seconds elapsed)
2. Accumulate active modifiers' contributions
3. Final visibility = baseline + (panic * -0.3m) + (flare * +3.4m) + (sonar * +1.9m)
4. Update shader uniforms: u_blindFogStart, u_blindFogEnd
5. Render blind fog effect via retro_shader + player_shader
```

### 3.3 Phases

#### **Phase 3.1: Shader Infrastructure Fixes**
**Owner:** Graphics Engineer  
**Effort:** 1 day

**Jobs:**
- [ ] Audit `retro_shader.frag`: Confirm `u_blindFogStart`, `u_blindFogEnd`, `u_blindFogStrength`, `u_blindFogColor` exist and are used correctly.
- [ ] Check `player_shader.frag`: Add missing blind fog uniforms (copy from retro shader).
- [ ] Verify both shaders use **radial smoothstep** fog (not linear): `visibility = smoothstep(fogEnd, fogStart, distance)`.
- [ ] Compile both shaders; verify no errors.

**Fixes:**
- [ ] If `player_shader.frag` doesn't have blind uniforms: Add them now (high-priority fix).
- [ ] If fog is linear: Replace with radial smoothstep for organic feel.
- [ ] If fog color hardcoded: Replace with `u_blindFogColor` uniform.

**Improvements:**
- [ ] Performance: Pre-compute fog parameters on CPU; only send 2–3 uniforms per frame instead of recomputing in shader.
- [ ] Add optional fog texture: Sample noise pattern for fog edge variation (less flat).

**Tests:**
- [ ] Both shaders compile without errors.
- [ ] Blind fog uniforms update in real-time.
- [ ] Fog edge appears smooth (smoothstep) not harsh (linear).

---

#### **Phase 3.2: BlindEffectController & Modifier System**
**Owner:** Gameplay Engineer  
**Effort:** 2–3 days

**Jobs:**
- [ ] Create `BlindEffectModifier` interface:
  ```java
  public interface BlindEffectModifier {
      float getVisibilityDeltaMeters(); // Contribution to visibility radius
      boolean isExpired(); // Check if duration elapsed
      String getModifierName();
  }
  ```
- [ ] Create `BlindEffectController`:
  ```java
  public class BlindEffectController {
      private Array<BlindEffectModifier> modifiers;
      private BlindEffectRevealConfig config;
      
      public void update(float deltaSeconds) {
          modifiers.removeIf(BlindEffectModifier::isExpired);
          float totalVisibility = config.baseline + modifiers.stream()
              .mapToFloat(BlindEffectModifier::getVisibilityDeltaMeters)
              .sum();
          updateShaderUniforms(totalVisibility);
      }
      
      public void addModifier(BlindEffectModifier mod) { modifiers.add(mod); }
  }
  ```
- [ ] Implement 4 concrete modifiers:
  - `BlindBaselineModifier`: Always returns `config.baseline_visibility_meters`.
  - `BlindPanicModifier`: Returns `config.panic_modifier.visibility_reduction_factor * currentPanicLevel`.
  - `BlindFlareModifier`: Returns `config.flare_modifier.visibility_radius_meters` for 12s, then expires.
  - `BlindSonarRevealModifier`: Returns `config.sonar_reveal.clap_shout_radius_meters` for 1.2s, then expires.

**Fixes:**
- [ ] Modifier stacking: Ensure modifiers sum correctly (baseline + all active mods).
- [ ] Negative visibility clamp: Clamp final visibility to [0.1m, 10m] (never fully blind or fully lit).
- [ ] Config reload: Support hot-reload of JSON without recompile.

**Improvements:**
- [ ] Modifier priorities: If panic conflicts with flare, flare wins (player feels rewarded for using flare).
- [ ] Logging: Log all modifier adds/removals for debugging.

**Tests:**
- [ ] BlindEffectController instantiates.
- [ ] Adding modifiers increases visibility correctly.
- [ ] Modifiers expire after duration.
- [ ] Visibility stays within [0.1m, 10m] range.

---

#### **Phase 3.3: Panic System Integration**
**Owner:** Gameplay Engineer  
**Effort:** 1–2 days

**Jobs:**
- [ ] Define "panic level" source: Proximity to enemy, sound intensity, low health, etc.
  - Assume panic level in range [0.0, 1.0] where 1.0 = max panic.
- [ ] Hook BlindEffectController to panic source:
  ```java
  float panicLevel = playerController.getPanicLevel();
  blindController.updatePanicModifier(panicLevel);
  ```
- [ ] Update `BlindPanicModifier` to read panic level each frame and return visibility delta.

**Fixes:**
- [ ] If panic source undefined: Create a stub `PanicLevelProvider` interface for future expansion.

**Improvements:**
- [ ] Panic animation: Gradually shrink visibility over 0.3s (not instant).
- [ ] Audio feedback: Play heartbeat SoundEvent when panic > 0.6.

**Tests:**
- [ ] Panic level 0.0 → no visibility reduction.
- [ ] Panic level 1.0 → visibility shrinks by 50%.
- [ ] Panic transitions smooth over 0.3s.

---

#### **Phase 3.4: Sonar Reveal & Sound Integration**
**Owner:** Gameplay Engineer + Audio Engineer  
**Effort:** 2–3 days

**Jobs:**
- [ ] Hook BlindEffectController to sound events:
  ```java
  public void onSoundEvent(SoundEvent event) {
      if (event.getType() == SoundEventType.CLAP_SHOUT) {
          BlindSonarRevealModifier reveal = new BlindSonarRevealModifier(config, 1.2f);
          addModifier(reveal);
          if (config.sonar_reveal.triggers_acoustic_visualization) {
              acousticViz.triggerVisualization(event);
          }
      }
  }
  ```
- [ ] Wire `SoundPropagationOrchestrator` to emit `SoundEvent.CLAP_SHOUT` when player claps/shouts.
- [ ] Ensure blind reveal radius aligns with sound pulse visualization (both 2.5m).

**Fixes:**
- [ ] Sound event routing: Ensure SoundEvent reaches BlindEffectController (check event subscriber chain).
- [ ] Timing synchronization: Sonar reveal duration (1.2s) should match acoustic pulse visualization lifetime.

**Improvements:**
- [ ] Sonar feedback: Visual flash on screen when sonar reveal triggers (white vignette).
- [ ] Multiple sonar types: Differentiate clap (small), shout (medium), whistle (large) reveals.

**Tests:**
- [ ] Clap/shout SoundEvent triggers sonar reveal modifier.
- [ ] Visibility expands to 2.5m for 1.2s.
- [ ] Modifier expires cleanly (no lingering fog).
- [ ] Acoustic visualization triggers simultaneously.

---

#### **Phase 3.5: Flare Item Integration**
**Owner:** Gameplay Engineer + Items Engineer  
**Effort:** 1–2 days

**Jobs:**
- [ ] Add `FlareItem` class (extends `CarriableItem`):
  ```java
  public class FlareItem extends CarriableItem {
      public void activate() {
          BlindFlareModifier mod = new BlindFlareModifier(config, 12.0f);
          blindController.addModifier(mod);
          PlaySoundEvent("flare_ignite"); // Audio feedback
      }
  }
  ```
- [ ] Wire to `PlayerInteractionSystem`: Player presses E on flare → activates it.
- [ ] Remove flare from inventory after duration (consumed item).

**Fixes:**
- [ ] Flare duration countdown: Display visual timer on HUD while active.
- [ ] Multiple flares: Allow player to carry 2–3 flares; stack modifiers if both active.

**Improvements:**
- [ ] Flare drops on ground: Player can drop flare; it illuminates area for others (if multiplayer planned).
- [ ] Flare throwable: Player throws flare to location; illuminates that point.

**Tests:**
- [ ] FlareItem creates BlindFlareModifier.
- [ ] Visibility expands to 4m on flare activation.
- [ ] Flare consumed after 12s.
- [ ] Multiple flares stack visibility increases.

---

#### **Phase 3.6: Uniform Updates & Shader Binding**
**Owner:** Graphics Engineer  
**Effort:** 1 day

**Jobs:**
- [ ] Create `BlindFogUniformUpdater` helper:
  ```java
  public void updateBlindUniforms(ShaderProgram retro, ShaderProgram player, 
                                   float visibilityRadiusMeters) {
      float fogStart = visibilityRadiusMeters;
      float fogEnd = visibilityRadiusMeters * 1.5f; // Fade zone extends beyond start
      retro.setUniformf("u_blindFogStart", fogStart);
      retro.setUniformf("u_blindFogEnd", fogEnd);
      retro.setUniformf("u_blindFogStrength", 0.8f);
      retro.setUniform4fv("u_blindFogColor", color, 0, 4);
      // ... repeat for player shader
  }
  ```
- [ ] Call updater each frame from `BlindEffectController.update()`.

**Fixes:**
- [ ] Uniform cache: Avoid setting uniforms if values haven't changed (slight perf gain).

**Improvements:**
- [ ] Debug visualization: Draw wireframe sphere at blind fog radius (visible with Ctrl+G).

**Tests:**
- [ ] Uniforms update when visibility changes.
- [ ] Blind fog visible in-game (geometry fades to black at edge).

---

#### **Phase 3.7: HUD & Debug Visualization**
**Owner:** UI Engineer  
**Effort:** 1 day

**Jobs:**
- [ ] Add blind effect diagnostics to `DiagnosticOverlay`:
  ```
  [BLIND EFFECT]
  Baseline: 0.6m
  Panic Modifier: -0.3m (panic=0.7)
  Flare Modifier: +3.4m (expires in 8.2s)
  Sonar Modifier: +1.9m (expires in 0.8s)
  Final Visibility: 4.6m
  ```
- [ ] Draw debug wireframe sphere at blind fog radius (toggle with Ctrl+G).

**Fixes:**
- [ ] HUD performance: Only update text when metric changes, not every frame.

**Tests:**
- [ ] Diagnostics tab shows correct visibility calculations.
- [ ] Wireframe sphere radius matches actual blind fog boundary.

---

#### **Phase 3.8: Integration & End-to-End Testing**
**Owner:** QA Lead  
**Effort:** 2 days

**Jobs:**
- [ ] Test blind chamber zone (`BlindChamberZone` in Task 2):
  - Spawn player with 0.6m baseline visibility.
  - Player cannot see beyond 0.6m (featureless walls should be invisible).
  - Player picks up flare → visibility expands to 4m instantly.
  - Player claps → sonar reveal adds 2.5m (total 4m, since flare already active).
  - Player walks to panic source (nearby enemy) → visibility shrinks.
  - Each modifier's duration expiry works correctly.
- [ ] Cross-system check: Panic event triggers acoustic visualization + blind effect simultaneously.

**Fixes (Post-Testing):**
- [ ] If visibility shrinks abruptly: Smooth transition over 0.2s instead of instant.
- [ ] If modifiers don't stack correctly: Debug modifier addition order.

**Improvements:**
- [ ] Performance: If HUD updates cause frame drops, cache text rendering.

**Tests:**
- [ ] Baseline blind effect functional in BlindChamberZone.
- [ ] Flare pickup expands visibility.
- [ ] Sonar reveals work.
- [ ] Panic shrinks visibility.
- [ ] All modifiers respect config file values.
- [ ] No shader compile errors on desktop + mobile.

---

#### **Phase 3.9: Re-fixes & Tuning**
**Owner:** Gameplay Designer  
**Effort:** 1 day (post-QA feedback)

**If Blind Effect Too Harsh:**
- Increase baseline visibility to 0.8m.
- Reduce panic shrinkage to 25% instead of 50%.

**If Sonar Reveals Overpowered:**
- Reduce sonar radius from 2.5m to 1.5m.
- Reduce sonar duration from 1.2s to 0.8s.

**If Flare Too Valuable:**
- Reduce flare visibility to 3m (instead of 4m).
- Reduce flare duration to 8s (instead of 12s).

---

### 3.4 Test Summary (Task 3)
| Test ID | Category | Scope | Expected Result |
|---------|----------|-------|-----------------|
| T3.01 | Unit | Shader uniforms | Both retro + player shaders have blind fog uniforms |
| T3.02 | Unit | Modifier stack | Modifiers sum correctly |
| T3.03 | Unit | Modifier expiry | Modifiers removed after duration |
| T3.04 | Integration | Panic integration | Panic level shrinks visibility |
| T3.05 | Integration | Flare pickup | Visibility expands to 4m for 12s |
| T3.06 | Integration | Sonar event | SoundEvent.CLAP_SHOUT triggers 2.5m reveal |
| T3.07 | Visual | Blind fog | Geometry fades smoothly at blind boundary |
| T3.08 | Performance | Uniform updates | <0.5ms per frame |
| T3.09 | HUD | Diagnostics | Blind effect metrics show correct values |
| T3.10 | Cross-system | Sound + Blind | Sonar reveal + acoustic visualization trigger together |

---

# TASK 4: 3D Sound Bounce Visualization for Microphone Input

## 4.1 Strategic Overview
Replace 2D `SoundPulseVisualizer` (8 fixed-angle arcs) with a **dual-layer 3D system**:
1. **Graph-based layer:** Render Dijkstra acoustic propagation graph edges, color-coded by bounce depth (cyan → yellow → orange → red).
2. **Geometric layer:** Fire 36 configurable 3D rays from mic position; mark bounce hits with cross-shaped markers; show intensity via brightness.

Both layers triggered simultaneously when player speaks into mic (SoundEvent.CLAP_SHOUT or custom MIC_INPUT event).

### 4.2 Architecture

#### Class Structure
```
io.github.superteam.resonance.sound.viz
├── AcousticBounce3DVisualizer (Orchestrator for both layers)
├── GraphRenderLayer (Dijkstra edge rendering)
├── GeometricRayLayer (3D ray casting + bounce markers)
├── AcousticBounceConfig (JSON-backed settings)
└── MicrophoneInputAdapter (Bridges mic → SoundEvent)
```

#### JSON Config: `assets/config/acoustic_bounce_3d_config.json`
```json
{
  "enabled": true,
  "graph_layer": {
    "render_edges": true,
    "color_bounce_0": [0.0, 1.0, 1.0, 0.6],
    "color_bounce_1": [0.5, 1.0, 0.0, 0.6],
    "color_bounce_2": [1.0, 0.7, 0.0, 0.5],
    "color_bounce_3": [1.0, 0.0, 0.0, 0.4],
    "edge_line_width": 2.0,
    "fade_out_seconds": 2.0
  },
  "geometric_layer": {
    "ray_count": 36,
    "ray_max_distance_meters": 30.0,
    "bounce_marker_scale": 0.3,
    "bounce_marker_color": [1.0, 1.0, 0.0, 0.8],
    "ray_line_width": 1.0,
    "fade_out_seconds": 3.0
  },
  "microphone": {
    "input_enabled": true,
    "sensitivity_threshold": 0.3,
    "trigger_sound_event": "MIC_INPUT"
  }
}
```

### 4.3 Phases

#### **Phase 4.1: Microphone Input Integration**
**Owner:** Audio Engineer  
**Effort:** 1–2 days

**Jobs:**
- [ ] Create `MicrophoneInputAdapter` (wraps OS mic API):
  ```java
  public class MicrophoneInputAdapter {
      private AudioRecorder recorder; // LibGDX AudioRecorder
      private short[] buffer;
      private float sensitivityThreshold = 0.3f;
      
      public void update() {
          recorder.read(buffer);
          float rms = calculateRMS(buffer);
          if (rms > sensitivityThreshold) {
              emitSoundEvent(SoundEventType.MIC_INPUT, rms);
          }
      }
  }
  ```
- [ ] Hook to `SoundPropagationOrchestrator`: On MIC_INPUT event, trigger acoustic visualization.
- [ ] Catch mic permission errors gracefully (show warning, disable mic input).

**Fixes:**
- [ ] Mic access denied: Log warning; continue without mic.
- [ ] Audio buffer overflow: Circular buffer to prevent data loss.

**Improvements:**
- [ ] Mic gain control: Allow user to adjust input sensitivity.
- [ ] Voice activation: Only trigger on speech-like frequencies (100Hz–1kHz energy spike).

**Tests:**
- [ ] MicrophoneInputAdapter reads audio frames.
- [ ] RMS calculation correct for known waveforms.
- [ ] Mic input triggers SoundEvent.MIC_INPUT.

---

#### **Phase 4.2: Dijkstra Graph Rendering (Layer 1)**
**Owner:** Graphics Engineer  
**Effort:** 2 days

**Jobs:**
- [ ] Create `GraphRenderLayer`:
  ```java
  public class GraphRenderLayer {
      private DijkstraPathfinder dijkstra; // Existing acoustic graph
      private ShapeRenderer shapeRenderer; // For edge lines
      private Array<GraphNodeState> nodeStates;
      private Map<String, Color> bounceDepthColors; // cyan → yellow → orange → red
      
      public void renderEdges(PerspectiveCamera camera) {
          for (Edge edge : dijkstra.getGraph().getEdges()) {
              int bounceDepth = edge.getBounceCount();
              Color color = bounceDepthColors.get(bounceDepth);
              drawLine(edge.from, edge.to, color, config.edge_line_width);
          }
      }
      
      public void update(float deltaSeconds) {
          // Fade out edges over time
          nodeStates.forEach(s -> s.alpha = Math.max(0, s.alpha - deltaSeconds / config.fade_out_seconds));
      }
  }
  ```
- [ ] Query Dijkstra graph for all edges; extract bounce depth per edge.
- [ ] Map bounce depth to color (0 bounces = cyan, 3+ = red).
- [ ] Render edges as 3D lines using ShapeRenderer or custom mesh.

**Fixes:**
- [ ] Graph update race condition: Lock graph access during rendering.
- [ ] Depth sorting: Draw edges with depth test enabled to prevent Z-fighting.

**Improvements:**
- [ ] Edge thickness varies by intensity: Louder bounces = thicker lines.
- [ ] Edge animation: Lines pulse over 1s to emphasize propagation direction.

**Tests:**
- [ ] Graph layer renders without errors.
- [ ] Edges color-coded by bounce depth.
- [ ] Edges fade out over 2s.

---

#### **Phase 4.3: Geometric Ray Casting (Layer 2)**
**Owner:** Physics Engineer  
**Effort:** 2–3 days

**Jobs:**
- [ ] Create `GeometricRayLayer`:
  ```java
  public class GeometricRayLayer {
      private Array<RayTraceResult> activeRays; // Current frame rays
      private Array<BounceMarker> bounceMarkers; // Hit points
      private btDynamicsWorld physicsWorld; // For raycasting
      private ShapeRenderer shapeRenderer;
      
      public void fireRays(Vector3 sourcePosition, float strength) {
          for (int i = 0; i < config.ray_count; i++) {
              Vector3 direction = generateRayDirection(i, config.ray_count); // Spherical distribution
              RayTraceResult hit = rayCast(sourcePosition, direction, config.ray_max_distance_meters);
              activeRays.add(hit);
              if (hit.hasContact()) {
                  bounceMarkers.add(new BounceMarker(hit.contactPoint, hit.intensity));
              }
          }
      }
      
      public void render(PerspectiveCamera camera) {
          for (RayTraceResult ray : activeRays) {
              drawLine(ray.sourcePos, ray.hitPos, Color.YELLOW, config.ray_line_width);
          }
          for (BounceMarker marker : bounceMarkers) {
              drawCrossMarker(marker.position, config.bounce_marker_scale, config.bounce_marker_color);
          }
      }
  }
  ```
- [ ] Implement ray direction generation: Distribute 36 rays evenly across unit sphere (Fibonacci sphere algorithm or similar).
- [ ] Implement raycasting: Use Bullet physics `rayTestSingle()` to find first collision per ray.
- [ ] Draw cross-shaped hit markers at bounce points.

**Fixes:**
- [ ] Ray origin clipping: Start rays 0.1m from source to avoid self-collision.
- [ ] Ray count performance: If 36 rays cause frame drops, reduce to 18 or make configurable.

**Improvements:**
- [ ] Second bounces: After hitting a wall, fire secondary rays from bounce point (increases complexity but more realistic).
- [ ] Intensity attenuation: Ray brightness = initial_strength / (distance_traveled² + 1).

**Tests:**
- [ ] Ray direction generation produces 36 rays covering all directions.
- [ ] Raycasting finds collisions correctly.
- [ ] Bounce markers appear at hit points.
- [ ] Rays fade out over 3s.

---

#### **Phase 4.4: Unified Visualization Orchestrator**
**Owner:** Graphics Engineer  
**Effort:** 1 day

**Jobs:**
- [ ] Create `AcousticBounce3DVisualizer` (combines both layers):
  ```java
  public class AcousticBounce3DVisualizer {
      private GraphRenderLayer graphLayer;
      private GeometricRayLayer rayLayer;
      private AcousticBounceConfig config;
      
      public void onMicInput(SoundEventData eventData) {
          float strength = eventData.getIntensity();
          if (config.graph_layer.render_edges) {
              graphLayer.activate(strength);
          }
          if (config.geometric_layer.render_rays) {
              rayLayer.fireRays(playerPosition, strength);
          }
      }
      
      public void render(PerspectiveCamera camera) {
          graphLayer.render(camera);
          rayLayer.render(camera);
      }
      
      public void update(float deltaSeconds) {
          graphLayer.update(deltaSeconds);
          rayLayer.update(deltaSeconds);
      }
  }
  ```
- [ ] Wire to `SoundPropagationOrchestrator`: On SoundEvent (MIC_INPUT or CLAP_SHOUT), call `onMicInput()`.

**Fixes:**
- [ ] Concurrent rendering: Ensure ray/graph updates don't conflict (use immutable snapshot pattern).

**Improvements:**
- [ ] Layer toggle: Press V to cycle (off, graph-only, rays-only, both).
- [ ] Color intensity feedback: Brighter colors = louder sound (tie to event strength).

**Tests:**
- [ ] Both layers render simultaneously.
- [ ] Toggle between layers works.
- [ ] Triggering mic input activates visualization.

---

#### **Phase 4.5: Debug HUD & Statistics**
**Owner:** UI Engineer  
**Effort:** 1 day

**Jobs:**
- [ ] Add acoustic visualization diagnostics to `DiagnosticOverlay`:
  ```
  [ACOUSTIC BOUNCE VIZ]
  Graph Layer: ACTIVE (245 edges, 40 bounces)
  Ray Layer: ACTIVE (36 rays, 12 hits)
  Last Event: MIC_INPUT (strength=0.82)
  Graph Fade: 1.2s remaining
  Ray Fade: 2.1s remaining
  ```
- [ ] Draw legend for bounce depth colors (cyan = 0, yellow = 1, orange = 2, red = 3+).

**Fixes:**
- [ ] HUD update performance: Cache text; only redraw on metric change.

**Tests:**
- [ ] Statistics update in real-time.
- [ ] Legend visible and accurate.

---

#### **Phase 4.6: Integration & Cross-System Testing**
**Owner:** QA Lead  
**Effort:** 2–3 days

**Jobs:**
- [ ] Test in `SoundPropagationZone` (Task 2):
  - Player speaks into mic → graph edges light up, geometric rays fire.
  - Graph edges color-coded by bounce depth.
  - Ray markers appear at wall hits.
  - Blind effect sonar reveal triggers simultaneously (Task 3 integration).
  - Performance: Visualization < 1.5ms per frame.
- [ ] Cross-system: Mic input → SoundEvent.MIC_INPUT → BlindEffectController (sonar reveal) + AcousticBounce3DVisualizer.

**Fixes (Post-Testing):**
- [ ] If graph edges overlapping rays, adjust line width or opacity.
- [ ] If mic input triggers too frequently, increase sensitivity threshold.
- [ ] If visualization causes frame drops, reduce ray count or simplify graph rendering.

**Improvements:**
- [ ] Performance optimization: GPU instancing for marker rendering if 36 rays too slow.

**Tests:**
- [ ] Mic input successfully triggers visualization.
- [ ] Graph layer renders all edges without clipping.
- [ ] Ray casting hits walls correctly.
- [ ] Bounce markers visible at hit points.
- [ ] Blind sonar reveal + acoustic viz triggered together.
- [ ] Performance: <1.5ms per frame on desktop, <3ms on mobile.

---

#### **Phase 4.7: Re-fixes & Tuning**
**Owner:** Graphics Engineer + Audio Engineer  
**Effort:** 1 day (post-QA feedback)

**If Graph Edges Overwhelming:**
- Render only edges with bounce depth ≤ 2 (skip 3+ bounces).
- Reduce edge opacity to 0.3.

**If Ray Casting Too Expensive:**
- Reduce ray count from 36 to 18.
- Lower max ray distance from 30m to 15m.

**If Visualization Not Intuitive:**
- Add arrows along graph edges to show propagation direction.
- Animate rays expanding outward from source (not instant).

---

### 4.4 Test Summary (Task 4)
| Test ID | Category | Scope | Expected Result |
|---------|----------|-------|-----------------|
| T4.01 | Unit | Mic input | Reads audio frames, calculates RMS |
| T4.02 | Unit | Ray generation | 36 rays distributed evenly across sphere |
| T4.03 | Unit | Raycasting | Finds collisions correctly |
| T4.04 | Integration | Graph rendering | All edges render, color-coded by bounce |
| T4.05 | Integration | Ray rendering | All rays and markers visible |
| T4.06 | Cross-system | Mic → Blind | Mic input triggers blind sonar reveal |
| T4.07 | Cross-system | Mic → Audio | Mic input triggers acoustic event loop |
| T4.08 | Visual | Graph layer | Edges fade out smoothly over 2s |
| T4.09 | Visual | Ray layer | Rays fade out smoothly over 3s |
| T4.10 | Performance | Desktop (60 FPS) | Visualization ≤ 1.5ms per frame |

---

# TASK 5: Blind Effect Gameplay Integration

## 5.1 Strategic Overview
Complete the blind effect cycle by integrating it into core gameplay loops:
- **Panic system:** Tie panic level to proximity to threats, loud sounds, or low health.
- **Flare economy:** Limited flares available in-level; player must manage use.
- **Sonar as core mechanic:** Clap/shout mechanic essential for navigation + blind chamber escape.

### 5.2 Phases

#### **Phase 5.1: Panic Level Computation**
**Owner:** Gameplay Engineer  
**Effort:** 1 day

**Jobs:**
- [ ] Create `PanicLevelProvider` interface (sources of panic).
- [ ] Implement panic factors:
  - Proximity to threats: Panic += (1.0 - distanceToThreat/20m) if distanceToThreat < 20m.
  - Loud ambient sound: Panic += soundIntensity * 0.5.
  - Low health: Panic += (1.0 - health/maxHealth) * 0.3.
- [ ] Update `BlindEffectController` to read panic level each frame.

**Tests:**
- [ ] Panic level ranges [0, 1].
- [ ] Multiple factors accumulate correctly.

---

#### **Phase 5.2: Flare Economy & Spawn Placement**
**Owner:** Level Designer + Gameplay Engineer  
**Effort:** 1 day

**Jobs:**
- [ ] Define flare spawn locations in blind chamber and other dark zones.
- [ ] Limit flares in inventory (max 3).
- [ ] Flare pickup audio/visual feedback.

**Tests:**
- [ ] Player can pick up max 3 flares.
- [ ] Picking up 4th flare fails gracefully (inventory full message).

---

#### **Phase 5.3: Sonar Mechanic Tutorial**
**Owner:** Level Designer  
**Effort:** 1 day

**Jobs:**
- [ ] Create tutorial level: Blind chamber with sonar hints ("Press SPACE to clap and reveal nearby surfaces").
- [ ] Progression: First sonar reveal shows 2.5m, then player reaches flare area, flare extends to 4m.

**Tests:**
- [ ] Tutorial clearly explains sonar mechanic.
- [ ] Player can complete tutorial without flares (sonar-only).

---

### 5.3 Test Summary (Task 5)
| Test ID | Category | Scope | Expected Result |
|---------|----------|-------|-----------------|
| T5.01 | Integration | Panic + Blind | Panic shrinks visibility dynamically |
| T5.02 | Integration | Flare economy | Max 3 flares in inventory |
| T5.03 | Integration | Sonar tutorial | Player learns sonar mechanic |
| T5.04 | Gameplay | Blind chamber | Player can navigate + escape using sonar |

---

# Summary of All Tasks

| Task | Primary Goal | Key Systems | Est. Timeline | Performance Target |
|------|--------------|-------------|---------------|-------------------|
| **Task 1** | Unified body cam + VHS shader | Rendering, FrameBuffer | 1.5 weeks | <2ms per frame |
| **Task 2** | Universal walkable test scene | Player, Sound, Particles, Physics | 2 weeks | 60 FPS desktop, 30 FPS mobile |
| **Task 3** | Dynamic blind effect with modifiers | Rendering, Gameplay, Player | 1 week | <0.5ms per frame |
| **Task 4** | 3D acoustic visualization | Sound, Rendering, Audio | 1.5 weeks | <1.5ms per frame |
| **Task 5** | Blind mechanic integration | Gameplay, Level Design | 3 days | (no perf target) |

**Total Estimated Effort:** 6.5 weeks (assuming 2-person team overlap).  
**Testing Strategy:** 54+ unit/integration tests + manual validation across all 7 Universal Test Scene zones.  
**Documentation:** Update [CODING_STANDARDS.md](CODING_STANDARDS.md) + docu/md/ with architecture details.

---

# Configuration Files Reference

All tunable parameters live in JSON config files (hot-reloadable):
- `assets/config/body_cam_settings.json` — FOV, distortion, vignette.
- `assets/config/blind_effect_config.json` — Baseline, panic, flare, sonar durations/radii.
- `assets/config/acoustic_bounce_3d_config.json` — Graph colors, ray count, fade times.

**Reload mechanism:** Press Ctrl+R in-game to hot-reload all configs (useful for rapid iteration).

---

# Active Implementation Plan (Execution)

## Sprint 0 (Day 0) - Baseline and Safety Net

### Goals
- Lock scope and prevent drift while implementation begins.
- Ensure all upcoming changes are measurable and reversible.

### Jobs
- [ ] Freeze baseline FPS, frame time, and memory benchmarks on PlayerTestScreen and SoundTestScreen.
- [ ] Confirm existing tests execute cleanly in core module.
- [ ] Create a single implementation branch and define merge checkpoints per task.
- [ ] Register config file contracts for:
  - `assets/config/body_cam_settings.json`
  - `assets/config/blind_effect_config.json`
  - `assets/config/acoustic_bounce_3d_config.json`

### Acceptance
- [ ] Baseline metrics captured and saved.
- [ ] No regressions before first feature merge.

---

## Sprint 1 (Week 1) - BodyCam + Blind Shader Foundation

### Sprint Objective
Deliver the first playable vertical slice where the scene renders through unified BodyCamVHS and blind fog works consistently on all relevant shaders.

### In-Sprint Scope
- Task 1.1, 1.2, 1.3, 1.4.
- Task 3.1 only (critical shader parity fix).

### Jobs
- [x] Implement unified `body_cam_vhs` shader pair and validate compilation.
- [x] Replace standalone VHS path in render order with unified body-cam pass.
- [x] Add JSON-backed runtime settings + HUD controls for tuning.
- [x] Implement blind uniforms and radial smoothstep parity in `player_shader.frag`.
- [ ] Validate effect stacking: BodyCam distortion + blind fog + existing render content.

### Fixes
- [ ] Eliminate edge clipping and dark-edge chromatic bleed.
- [x] Prevent FBO lifecycle leaks on resize/show/hide/dispose.
- [x] Clamp uniform ranges to avoid unstable visuals.

### Tests
- [ ] Task 1 test set T1.01 to T1.08 executed.
- [ ] Task 3 tests T3.01 and T3.07 executed.
- [x] Compile verification: `:core:compileJava` and `:lwjgl3:compileJava`.

### Re-fix Gate
- [ ] If BodyCam pass exceeds 2ms, disable chromatic aberration on low profile and reduce LUT size.
- [ ] If blind fog hard-cuts, re-tune smoothstep ramp and fog end multiplier.

---

## Sprint 2 (Week 2) - Universal Test Scene Shell

### Sprint Objective
Ship a walkable first-person UniversalTestScene with 7 zone shells and live diagnostics.

### In-Sprint Scope
- Task 2.1, 2.2, 2.4, and minimal 2.3 zone implementations (shell level).

### Jobs
- [x] Build `UniversalTestScene` with center hub and seven connected zones.
- [x] Integrate player movement, transitions, and zone activation.
- [x] Add Tab-cycling diagnostics overlay with per-zone system state.
- [x] Ensure Item, Player, Particle, Sound, and Shader test hooks are present in their zones.

### Fixes
- [ ] Physics world boundaries per zone to avoid simulation leakage.
- [ ] Transition smoothing to avoid camera jitter.

### Tests
- [ ] T2.01, T2.02, T2.03, T2.09 pass.
- [ ] Manual walkthrough: all seven zones reachable in one session.

### Re-fix Gate
- [ ] If zone load stalls >1s, implement adjacent-zone preloading.
- [ ] If overlay costs >1ms, throttle metric refresh rate.

---

## Sprint 3 (Week 3) - Blind System Full Stack

### Sprint Objective
Complete always-on blindness with dynamic event-driven modifier stacking and gameplay hooks.

### In-Sprint Scope
- Task 3.2 through 3.9.

### Jobs
- [x] Implement `BlindEffectController` and all four modifiers.
- [x] Wire panic, flare, and sonar event sources.
- [x] Add blind diagnostics to overlay and wireframe debug radius.
- [x] Ensure sonar temporarily lifts blindness in pulse zone.

### Fixes
- [ ] Clamp visibility bounds and smooth transitions.
- [ ] Resolve any event routing lag from SoundPropagationOrchestrator to blind controller.

### Tests
- [ ] Full Task 3 test set T3.01 to T3.10.
- [ ] Validation in BlindChamberZone with baseline + flare + sonar + panic scenario.

### Re-fix Gate
- [ ] If effect is too punishing, tune baseline and panic attenuation via config only.

---

## Sprint 4 (Week 4) - 3D Acoustic Visualization

### Sprint Objective
Ship dual-layer acoustic visualization (graph + geometric rays) driven by mic and shout events.

### In-Sprint Scope
- Task 4.1 through 4.7.

### Jobs
- [x] Implement mic adapter + event emission safety fallback.
- [x] Render Dijkstra edges with bounce-depth color mapping.
- [x] Implement 36-ray geometric layer with bounce markers.
- [x] Integrate unified visualizer with SoundPropagationOrchestrator.
- [ ] Bind visualization lifecycle to blind sonar reveal timing.

### Fixes
- [ ] Prevent self-hit raycasts.
- [ ] Synchronize fade lifetimes between graph/rays and blind reveal.

### Tests
- [ ] Full Task 4 test set T4.01 to T4.10.
- [ ] Performance validation under stress in SoundPropagationZone.

### Re-fix Gate
- [ ] If frame time exceeds budget, reduce rays and cap displayed graph depth.

---

## Sprint 5 (Week 5) - Gameplay Integration and Balancing

### Sprint Objective
Finalize panic economy, flare strategy, and sonar tutorial gameplay loops.

### In-Sprint Scope
- Task 5.1 through 5.3.
- Cross-task polish from Task 1 to Task 4.

### Jobs
- [x] Integrate panic computation model.
- [x] Finalize flare economy and spawn logic.
- [x] Build sonar onboarding/tutorial flow in BlindChamber progression.
- [ ] Run full-system regression across all seven zones.

### Fixes
- [ ] Resolve contradictory feedback loops between panic, flare, and sonar modifiers.
- [ ] Rebalance based on user-testing runs.

### Tests
- [ ] T5.01 to T5.04 pass.
- [ ] Full regression pass of T1, T2, T3, T4, T5 suites.

### Re-fix Gate
- [ ] If players are blocked in blind navigation, increase sonar readability and adjust baseline visibility in config.

---

## Global Completion Criteria
- [ ] Unified BodyCamVHS pass is default and stable.
- [ ] UniversalTestScene replaces fragmented test workflows for all systems except model systems.
- [ ] Blind effect baseline + dynamic modifiers are production-ready and tunable via config.
- [ ] 3D acoustic visualization is functionally correct and performance-safe.
- [ ] Documentation is updated with final architecture, controls, and tuning tables.

## Execution Note
Implementation should follow this order unless blocked by hard dependency: **Task 1 -> Task 3.1 -> Task 2 shell -> Task 4 -> Task 3 complete -> Task 5**.  
This order minimizes rework by stabilizing rendering and shader contracts before deep gameplay wiring.

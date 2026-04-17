# Particle System — Gap Analysis & Task Plan

## What You Already Have (Full Inventory)

Your system is already far beyond a basic implementation. Here's what exists and works:

| Category | What's implemented |
|----------|--------------------|
| **Emission** | Continuous rate, burst (one-shot + loop), emission curves over time, duration cap |
| **Shapes** | POINT, SPHERE, BOX, RING, DISC, CONE, LINE, SURFACE_SPHERE, TORUS + oriented ring axis |
| **Direction** | UP, OUTWARD, INWARD, VECTOR, RANDOM, CHAOS, MULTI, TANGENT, SURFACE_NORMAL |
| **Motion** | Gravity, wind (XYZ), wave oscillation, chaos kicks, velocity curve over lifetime |
| **Force fields** | `ForceField` interface, `VectorField`, `PointAttractorField`, `ForceFieldProvider` |
| **Collision** | `CollisionPlane` multi-plane, DIE / BOUNCE / STICK responses, sub-emitter on bounce/death |
| **Color** | Two-stop lerp, multi-stop gradient, start/end with separate ranges |
| **Size** | Start/end with min/max random range, linear interpolation |
| **Rotation** | Per-particle rotation + random rotation speed |
| **Rendering** | GPU instancing (GL 3.3+), soft particles (depth buffer), emissive + falloff, normal shading |
| **Blend modes** | ADDITIVE, ALPHA, SCREEN |
| **Texture** | Single texture, sprite sheet animation with frame blending, procedural styles (CIRCLE, SQUARE, DIAMOND, STAR, CROSS) |
| **Higher-level** | `ParticleEffect` (multi-emitter, offset, orientation), `ParticleManager` (async update, culling), `TrailEmitter` (ribbon) |
| **Formulas API** | `ring()`, `explosion()`, `cone()`, `sonarPulse()`, `fountain()`, `vortex()` |
| **Persistence** | JSON preset load/save, preset versioning, validation |
| **Radial pulse** | Expanding ring spawn radius over time |
| **Sub-emitters** | On death + on bounce callbacks |

---

## What Is Missing

### Critical Gaps (visual quality blockers)

**1. No depth sorting for ALPHA blend mode**
Particles using `ALPHA` blending render incorrectly without back-to-front sorting by camera distance. Additive particles are order-independent and look fine, but alpha-blended smoke, fog, and soft effects will have visible z-fighting artifacts.

**2. No velocity-stretched or axis-locked billboarding**
Every particle is a flat quad facing the camera (spherical billboard). There is no:
- Velocity-stretched billboard (sparks, raindrops, laser bolts look stretched by speed)
- Y-axis locked billboard (trees, vertical effects that shouldn't tilt with camera pitch)
- World-space fixed quad (decals, floor marks, flat ground effects)
- Velocity-aligned billboard (particles face their direction of travel)

**3. No emitter velocity inheritance**
When the emitter moves, newly spawned particles have no memory of that motion. A fire attached to a moving object should spray particles backward as it moves, not emit as if stationary.

**4. No warmup / pre-simulation**
Emitters always start empty. A campfire or ambient effect should appear already running, not build up from zero when the scene loads.

**5. Emissive strength is fixed per particle lifetime**
`emissiveStrength` is set at spawn and never changes. It should be drivable by a curve over lifetime so particles can pulse, flare up at birth, or fade their glow toward death independently of alpha.

### Feature Gaps

**6. No rotation speed curve over lifetime**
Rotation speed is randomized at spawn but never changes. A particle that should spin fast then slow to a stop (e.g., debris landing) or speed up over time cannot be expressed.

**7. No size curve (multi-stop)**
Size lerps linearly from `startSize` to `endSize`. There is no way to have a particle expand quickly, hold, then collapse — which is the characteristic shape of an explosion flash or a smoke puff.

**8. No coherent noise / turbulence**
`chaosStrength` applies fully random frame-by-frame velocity kicks. This produces jittery, inorganic motion. Real fluid turbulence, smoke wisps, and magic effects need **coherent noise** (Perlin/Simplex) where nearby samples in time and space are correlated, giving smooth organic drift rather than random jitter.

**9. No LOD (Level of Detail)**
Particle count never scales with camera distance. A 500-particle explosion 50 meters away is invisible in practice but still runs all 500 updates. No distance-based particle cap or emission rate scaling exists.

**10. No stop/complete action**
When `emissionDuration` elapses, the emitter silently stops emitting but there is no lifecycle callback. Nothing notifies the `ParticleManager` or `ParticleEffect` that this emitter is finished and can be removed or recycled.

**11. No per-particle trail / ribbon attachment**
`TrailEmitter` exists as a standalone object for one trail. There is no way to attach a mini-trail to each individual particle (needed for sparks with tails, magic missiles, raindrops).

**12. No texture blending / dual texture**
Only one texture is bound per emitter. Layering a detail noise texture over a base shape (e.g., fire detail over a soft glow) requires two separate emitters and cannot be done in one pass.

**13. No spawn delay / stagger**
Burst mode fires all particles in one frame. There is no way to stagger spawn times within a burst (e.g., fireworks that shoot up one by one) or apply a delay before emission starts.

**14. No `MULTIPLY` blend mode**
`ADDITIVE`, `ALPHA`, `SCREEN` are implemented. `MULTIPLY` (darkening, shadows, scorch marks) is missing and needed for ground-projection decal effects.

**15. No gravity scale per particle**
All particles share the global `gravity` from the definition. Individual particles cannot have varied gravity response (light sparks vs heavy debris from the same emitter).

---

## Task Plan

---

### Task 1 — Depth Sorting for ALPHA Blend Mode

**Why it matters:** Without sorting, alpha-blended particles at different depths overlap incorrectly. Smoke and soft aura effects look wrong — bright areas show through darker ones in the wrong order.

**What to add to `ParticleEmitter`:**

```java
private boolean sortingEnabled = false;
private final Vector3 sortCameraPos = new Vector3();

// Call before render when blendMode == ALPHA
private void sortParticlesByDepth(PerspectiveCamera camera) {
    sortCameraPos.set(camera.position);
    // Sort descending by squared distance (furthest first = back-to-front)
    activeParticles.sort((a, b) -> {
        float da = a.position.dst2(sortCameraPos);
        float db = b.position.dst2(sortCameraPos);
        return Float.compare(db, da);  // reverse order
    });
}
```

Add to `ParticleDefinition`:
```java
public boolean depthSort = false;  // enable for ALPHA blend mode
```

Auto-enable in `render()` when `blendMode == ALPHA` and `depthSort == true`. Do not sort every frame for additive particles — it's unnecessary and expensive.

**Performance note:** Sort only when `activeParticles.size > 0` and only when blend mode is `ALPHA`. Use insertion sort (faster for nearly-sorted data frame-to-frame) instead of full sort.

---

### Task 2 — Billboard Modes

**Why it matters:** All particles currently face the camera as spherical billboards. Sparks should stretch in their velocity direction. Ground decals should lie flat. Vertical smoke columns shouldn't tilt when you look up.

**Add `ParticleBillboardMode` enum:**

```java
public enum ParticleBillboardMode {
    SPHERICAL,       // always faces camera fully (current behavior)
    Y_AXIS_LOCKED,   // rotates on Y only — never tilts with camera pitch
    VELOCITY_ALIGNED,// quad faces velocity direction (sparks, bullets)
    VELOCITY_STRETCHED, // quad stretched by speed along velocity axis
    HORIZONTAL,      // flat on XZ plane (ground decals, floor puddles)
    WORLD_FIXED      // no billboarding, uses particle's own orientation
}
```

Add to `ParticleDefinition`:
```java
public String billboardMode = "SPHERICAL";
public float  velocityStretchFactor = 1.0f; // multiplier for VELOCITY_STRETCHED
```

**Billboard formulas for the shader / model matrix:**

```
SPHERICAL (current):
  right  = normalize(cross(camera.up, toParticle))
  up     = normalize(cross(toParticle, right))
  matrix = [right | up | toParticle | position]

Y_AXIS_LOCKED:
  right  = normalize(cross(Y, toParticle))
  up     = Y
  matrix = [right | up | toParticle | position]

VELOCITY_ALIGNED:
  forward = normalize(velocity)
  right   = normalize(cross(forward, toCamera))
  up      = normalize(cross(right, forward))
  matrix  = [right | up | forward | position]

VELOCITY_STRETCHED:
  same as VELOCITY_ALIGNED but scale Y by (speed * velocityStretchFactor)
  scaleX = size
  scaleY = size + speed * velocityStretchFactor

HORIZONTAL:
  right = X axis
  up    = Z axis
  (flat quad lying on ground)
```

---

### Task 3 — Emitter Velocity Inheritance

**Why it matters:** A torch on a moving ship, a jet thruster on a spaceship, or fire on a running enemy should spray particles in the direction opposite to the emitter's motion. Currently spawned particles are always world-stationary.

**Add to `ParticleEmitter`:**

```java
private final Vector3 lastEmitterPosition = new Vector3();
private final Vector3 emitterVelocity     = new Vector3();
private boolean firstVelocitySample = true;

// Call in setEmitterPosition() to track velocity
public void setEmitterPosition(float x, float y, float z) {
    if (!firstVelocitySample) {
        // Will be computed per-update cycle
    }
    emitterPosition.set(x, y, z);
}

// In update(), before emitting:
private void updateEmitterVelocity(float delta) {
    if (firstVelocitySample) {
        lastEmitterPosition.set(emitterPosition);
        firstVelocitySample = false;
        emitterVelocity.setZero();
        return;
    }
    emitterVelocity.set(emitterPosition).sub(lastEmitterPosition).scl(1f / delta);
    lastEmitterPosition.set(emitterPosition);
}
```

Add to `ParticleDefinition`:
```java
public float inheritVelocityFactor = 0f; // 0 = no inherit, 1 = full emitter velocity
```

In `createParticle()`, after setting initial velocity:
```java
particle.velocity.mulAdd(emitterVelocity, definition.inheritVelocityFactor);
```

---

### Task 4 — Warmup / Pre-Simulation

**Why it matters:** Ambient effects (campfire, water fountain, smoke vent) should look like they've been running for a while when first seen, not build up from nothing.

**Add to `ParticleDefinition`:**
```java
public float warmupDuration = 0f;  // seconds of pre-simulation to run at startup
public float warmupTimeStep = 0.05f; // timestep used for warmup ticks
```

**Add to `ParticleEmitter`:**
```java
public void warmup(float seconds, ForceFieldProvider forceFields) {
    float step = Math.max(0.01f, definition.warmupTimeStep);
    int ticks = (int) (seconds / step);
    for (int i = 0; i < ticks; i++) {
        update(step, forceFields);
    }
}
```

Call `warmup()` automatically in `setDefinition()` if `definition.warmupDuration > 0`:
```java
if (definition.warmupDuration > 0f) {
    warmup(definition.warmupDuration, null);
}
```

---

### Task 5 — Emissive Strength Curve Over Lifetime

**Why it matters:** A particle that glows brightly on spawn then fades its glow (independently of alpha) looks dramatically better — embers flash bright, then dim to orange, then disappear. Currently glow is stuck at the spawn value forever.

**Add to `ParticleDefinition`:**
```java
// Each entry: [ageRatio (0-1), emissiveStrengthMultiplier]
public float[][] emissiveCurve = null;
```

**Sample formula in `updateActiveParticles()`:**
```java
float emissiveMultiplier = sampleCurveValue(definition.emissiveCurve, ageRatio, 1f);
particle.emissiveStrength = definition.emissiveStrength * emissiveMultiplier;
```

**Example — flash-and-fade glow:**
```json
{
  "emissiveStrength": 3.0,
  "emissiveCurve": [
    [0.00, 1.5],
    [0.15, 1.0],
    [0.70, 0.3],
    [1.00, 0.0]
  ]
}
```

---

### Task 6 — Rotation Speed Curve Over Lifetime

**Add to `ParticleDefinition`:**
```java
// Each entry: [ageRatio, rotationSpeedMultiplier]
public float[][] rotationCurve = null;
```

In `updateActiveParticles()`, replace the fixed `particle.rotationSpeed * delta` with:
```java
float rotMultiplier = sampleCurveValue(definition.rotationCurve, ageRatio, 1f);
particle.rotation += particle.rotationSpeed * rotMultiplier * delta;
```

**Example — debris that spins in then tumbles to stop:**
```json
{
  "rotationCurve": [
    [0.0, 2.5],
    [0.4, 1.0],
    [0.8, 0.2],
    [1.0, 0.0]
  ]
}
```

---

### Task 7 — Size Curve (Multi-Stop)

**Why it matters:** Two-stop linear size lerp produces flat, uninteresting growth. Real effects expand fast, hold, then collapse — or pulse.

**Add to `ParticleDefinition`:**
```java
// Each entry: [ageRatio, sizeMultiplier]
// Applied on top of the existing startSize→endSize lerp
public float[][] sizeCurve = null;
```

In `updateActiveParticles()`:
```java
float baseSize = MathUtils.lerp(particle.startSize, particle.endSize, ageRatio);
float sizeMult = sampleCurveValue(definition.sizeCurve, ageRatio, 1f);
particle.currentSize = baseSize * sizeMult;
```

**Example — explosion flash shape:**
```json
{
  "sizeCurve": [
    [0.00, 0.1],
    [0.10, 1.8],
    [0.35, 1.0],
    [0.80, 0.6],
    [1.00, 0.0]
  ]
}
```

---

### Task 8 — Coherent Noise / Turbulence Field

**Why it matters:** `chaosStrength` is random noise — particles scatter incoherently. Real smoke, fire, and magic effects use **correlated** noise where neighboring particles are pulled in similar directions, producing natural-looking turbulence, drift, and swirls.

**Add `TurbulenceField.java`:**

```java
public class TurbulenceField implements ForceField {
    private float strength = 1.0f;
    private float scale    = 0.5f;   // world-space frequency (lower = larger swirls)
    private float speed    = 0.3f;   // how fast the turbulence pattern shifts over time
    private float elapsed  = 0f;

    public void setStrength(float strength) { this.strength = strength; }
    public void setScale(float scale)       { this.scale = Math.max(0.001f, scale); }
    public void setSpeed(float speed)       { this.speed = Math.max(0f, speed); }

    @Override
    public void sample(Vector3 position, float delta, Vector3 outForce) {
        elapsed += delta;
        // 3-octave value noise approximation using sin/cos combinations
        // Cheap, no external dependency, reasonably organic
        float px = position.x * scale + elapsed * speed;
        float py = position.y * scale + elapsed * speed * 0.7f;
        float pz = position.z * scale + elapsed * speed * 0.9f;

        float nx = noiseApprox(px + 0.0f, py + 1.3f, pz + 2.7f);
        float ny = noiseApprox(px + 3.1f, py + 0.0f, pz + 1.5f);
        float nz = noiseApprox(px + 1.9f, py + 4.2f, pz + 0.0f);

        outForce.set(nx, ny, nz).scl(strength);
    }

    // Cheap correlated noise: sum of sin/cos products gives smooth hills
    // Range: approx -1 to 1
    private float noiseApprox(float x, float y, float z) {
        return (MathUtils.sin(x * 1.7f + y * 0.9f)
              + MathUtils.sin(y * 1.3f + z * 1.1f)
              + MathUtils.sin(z * 0.8f + x * 1.5f)) / 3f;
    }
}
```

Add to `ParticleDefinition` (optional inline config):
```java
public boolean turbulenceEnabled  = false;
public float   turbulenceStrength = 0.5f;
public float   turbulenceScale    = 0.4f;
public float   turbulenceSpeed    = 0.2f;
```

Wire it in `ParticleEmitter`: if `turbulenceEnabled`, create an internal `TurbulenceField` and apply it automatically without requiring the caller to set up an external force field.

---

### Task 9 — LOD (Level of Detail)

**Why it matters:** 500 particles updating 50 meters away is wasted CPU. Particle count should scale down with distance.

**Add to `ParticleDefinition`:**
```java
public float lodDistanceMin  = 10f;  // full quality within this distance
public float lodDistanceMax  = 50f;  // 0% emission beyond this distance
public boolean lodEnabled    = false;
```

**In `ParticleManager` update loop, per emitter:**
```java
float dist = effect.getPosition().dst(camera.position);
float lodFactor = 1f;
if (def.lodEnabled && dist > def.lodDistanceMin) {
    lodFactor = 1f - MathUtils.clamp(
        (dist - def.lodDistanceMin) / (def.lodDistanceMax - def.lodDistanceMin),
        0f, 1f
    );
}
// Pass to emitter as emission rate scale
emitter.setLodFactor(lodFactor);
```

Apply in `emitNewParticles()`: `liveRate = definition.sampleEmissionRate(elapsed) * lodFactor`

Beyond `lodDistanceMax`, skip `update()` entirely.

---

### Task 10 — Emitter Lifecycle Callbacks

**Why it matters:** Nothing currently tells the outside world when an emitter finishes playing. You cannot auto-remove a one-shot explosion from the scene without polling.

**Add `EmitterLifecycleListener` to `ParticleEmitter`:**

```java
public interface EmitterLifecycleListener {
    void onEmitterComplete(ParticleEmitter emitter); // all emission done AND no active particles
    void onEmissionStopped(ParticleEmitter emitter); // emission stopped but particles still alive
}

private EmitterLifecycleListener lifecycleListener;

public void setLifecycleListener(EmitterLifecycleListener listener) {
    this.lifecycleListener = listener;
}
```

In `update()`, after `updateActiveParticles()`:
```java
boolean emissionDone = (definition.emissionDuration > 0 && emissionElapsed >= definition.emissionDuration)
                    || (definition.burstMode && !definition.burstLoop && burstFired);
if (emissionDone && !emissionCompleteFired) {
    emissionCompleteFired = true;
    if (lifecycleListener != null) lifecycleListener.onEmissionStopped(this);
}
if (emissionDone && activeParticles.size == 0 && !completeFired) {
    completeFired = true;
    if (lifecycleListener != null) lifecycleListener.onEmitterComplete(this);
}
```

Add `reset()` to clear these flags when `setDefinition()` is called again.

---

### Task 11 — Spawn Delay and Burst Stagger

**Why it matters:** Burst fires all particles in one frame. A firework salvo, a hit-spark with delay, or a countdown sequence needs staggered spawn timing.

**Add to `ParticleDefinition`:**
```java
public float emissionStartDelay   = 0f;   // seconds before any particles spawn
public float burstStaggerDuration = 0f;   // spread burst over this many seconds (0 = all at once)
```

In `emitNewParticles()`, gate all emission behind:
```java
if (emissionElapsed < definition.emissionStartDelay) return;
```

For burst stagger:
```java
if (definition.burstMode && definition.burstStaggerDuration > 0f) {
    // Instead of spawning all at once, add to a stagger queue
    // and release particles evenly over burstStaggerDuration
    float staggerRate = definition.burstCount / definition.burstStaggerDuration;
    staggerAccumulator += staggerRate * delta;
    int spawnNow = Math.min((int) staggerAccumulator, remainingBurstCount);
    staggerAccumulator -= spawnNow;
    // spawn spawnNow particles
}
```

---

### Task 12 — MULTIPLY Blend Mode

Add to `ParticleBlendMode`:
```java
MULTIPLY;
```

In `apply()`:
```java
case MULTIPLY:
    Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ZERO);
    break;
```

Use case: scorch marks, shadow projections, darkening overlays on surfaces.

---

### Task 13 — Per-Particle Gravity Scale

**Add to `ParticleInstance`:**
```java
float gravityScale = 1f;
```

Set at spawn with a randomized range from definition:
```java
public float gravityScaleMin = 1f;
public float gravityScaleMax = 1f;
```

In `createParticle()`:
```java
particle.gravityScale = MathUtils.random(definition.gravityScaleMin, definition.gravityScaleMax);
```

In `updateActiveParticles()`, replace `definition.gravity` with `definition.gravity * particle.gravityScale`. This lets you have light sparks and heavy debris in the same emitter.

---

## How to Make Good-Looking Particles

These are the techniques that separate average particle effects from professional ones. None require code changes — they are design principles to apply with your existing and new systems.

---

### 1. Layer Multiple Emitters, Not One Big One

A professional fire effect uses 3–5 stacked emitters. Each does one job:

| Layer | Blend | Purpose |
|-------|-------|---------|
| Core flash | ADDITIVE | Bright white/yellow center, short life (0.1–0.2s), tiny burst |
| Main flame | ADDITIVE | Orange → red, medium count, short life |
| Embers | ADDITIVE | Tiny sparks, long life, high gravity, chaotic |
| Smoke | ALPHA | Dark gray, large, slow, very long life |
| Glow | ADDITIVE | 1–2 huge low-alpha particles, no motion, just light bloom |

Each layer uses `ParticleEffect.addEmitter()` with a local offset. The result looks real; no single emitter can achieve it.

---

### 2. Always Fade Alpha to Zero at End of Life

Every effect should have `endColor[3] = 0`. Particles that pop out of existence look terrible. The alpha fade happens naturally through `sampleGradient()` but make sure `endColor[3]` is explicitly `0.0` in every preset.

**Gradient tip for fire:**
```json
"colorGradient": [
  [0.00,  1.00, 1.00, 0.85, 1.0],
  [0.20,  1.00, 0.80, 0.10, 0.9],
  [0.50,  0.90, 0.30, 0.00, 0.7],
  [0.80,  0.40, 0.05, 0.00, 0.3],
  [1.00,  0.10, 0.02, 0.00, 0.0]
]
```

---

### 3. Shrink to Near-Zero at Death

Set `endSizeMin` and `endSizeMax` very close to `0.001`. A particle that fades AND shrinks disappears far more naturally than one that just fades. Combine both.

---

### 4. Use ADDITIVE for Anything That Should Glow

ADDITIVE blending automatically makes dense particle regions brighter. Two fire particles overlapping look twice as bright — exactly what you want. Never use ALPHA for fire, magic, lasers, or electricity. ALPHA is for smoke, clouds, dust, and soft auras.

---

### 5. The "Soft Center" Texture Rule

Your procedural `CIRCLE` style is a hard-edge circle. For most effects, you want a **radially feathered circle** — full white in the center, fading to transparent at the edge. This makes additive particles look like glowing balls of light instead of flat discs.

Add this as a generated texture in `ParticleTextureStyle`:
```
SOFT_CIRCLE  —  Gaussian radial gradient: brightness = exp(-r² * 4)
SOFT_STAR    —  Star shape with soft edge
WISP         —  Elongated oval, brighter center
```

Formula for a soft circle pixel (in texture generator):
```java
float dx = (px / (width / 2f)) - 1f;
float dy = (py / (height / 2f)) - 1f;
float r2 = dx * dx + dy * dy;
float brightness = (float) Math.exp(-r2 * 4.0);  // Gaussian
pixel.set(brightness, brightness, brightness, brightness);
```

---

### 6. Randomize Everything — But Not Too Much

`randomStrength` between `0.1–0.3` gives natural variation. Above `0.6`, particles look scattered and cheap. For directed effects (cone, ring shot), keep `spreadAngle` tight (`5–15°`). For explosions, use `OUTWARD` direction with `chaosStrength: 0.2` instead of high `spreadAngle`.

---

### 7. Size and Speed Inversely Correlated

Large particles should move slowly. Small particles should move fast. Use separate emitters if you need both in the same effect: a burst emitter with large slow particles for the core, continuous emitter with tiny fast sparks for the detail.

---

### 8. Fewer Particles, Better Quality = Usually Wins

20 large, well-configured, softly textured particles almost always look better than 200 small hard-edged particles. Performance wins too. If your effect looks grainy, the fix is usually a better texture and fewer, larger particles — not adding more.

---

### 9. Emissive Curve for "Flash then Glow"

Use the `emissiveCurve` (Task 5 above) to make particles that flash bright on birth and dim quickly:
```json
"emissiveStrength": 2.5,
"emissiveCurve": [[0, 2.0], [0.1, 1.0], [0.5, 0.4], [1.0, 0.0]]
```

This produces a natural hot-then-cooling effect for sparks, explosions, and magic bolts.

---

### 10. Turbulence Makes Smoke Look Real

Smoke without turbulence rises in a perfect column. Real smoke twists and drifts. Add a `TurbulenceField` with low strength (`0.2–0.4`) and low scale (`0.3`) to any upward-moving effect for instant visual improvement.

---

### Quick Reference: Effect Recipes

**Campfire:**
- Layer 1: Core flash — burst 8, `ADDITIVE`, white → yellow, 0.1s lifetime, size 0.3→0.01
- Layer 2: Flame — 60/s, `ADDITIVE`, yellow → red gradient, 0.8s lifetime, UP direction, wave enabled
- Layer 3: Ember sparks — 20/s, `ADDITIVE`, orange tiny particles, 2s lifetime, `chaosStrength: 0.4`, high gravity
- Layer 4: Smoke — 10/s, `ALPHA`, dark gray, 3s lifetime, large (0.4→1.2), `depthSort: true`

**Magic Ring Shot:**
- Burst 48 particles, `RING` shape, `ringAxisZ: 1` (forward-facing), `OUTWARD` direction, `ADDITIVE`, cyan
- `speedMin: 5`, `speedMax: 7`, `spreadAngle: 5`, `lifetimeMax: 0.6`

**Explosion:**
- Layer 1: Shockwave ring — burst `RING`, outward, fast (8–12 m/s), white → yellow, 0.3s
- Layer 2: Fireball — burst 150 `SPHERE OUTWARD`, orange→dark red gradient, 0.5–1.0s
- Layer 3: Debris — burst 30 `SPHERE OUTWARD`, `BOUNCE` collision, `chaosStrength: 0.5`, 2s lifetime
- Layer 4: Smoke trail — 20/s for 1s, `ALPHA`, gray, large, `depthSort: true`

**Sonar Pulse (Resonance):**
- `RING` shape, `radialPulse: true`, expanding from 0→5 world units
- `waveEnabled: true`, wave pushes Y-axis, gives the ring a slight bobbing shimmer
- Cyan color, `ADDITIVE`, 0.5–0.9s lifetime
- Formula: `ParticleFormulas.sonarPulse(Color.CYAN)` — already built

---

## Deliverables Checklist

### Code
- [ ] **Task 1**: `depthSort` field + `sortParticlesByDepth()` in `ParticleEmitter` (insertion sort)
- [ ] **Task 2**: `ParticleBillboardMode` enum + billboard matrix computation in render / model transform
- [ ] **Task 3**: Emitter velocity tracking + `inheritVelocityFactor` in `ParticleDefinition`
- [ ] **Task 4**: `warmupDuration` + `warmup()` method in `ParticleEmitter`
- [ ] **Task 5**: `emissiveCurve` in `ParticleDefinition` + per-frame sampling in update
- [ ] **Task 6**: `rotationCurve` in `ParticleDefinition` + per-frame sampling
- [ ] **Task 7**: `sizeCurve` in `ParticleDefinition` + per-frame sampling
- [ ] **Task 8**: `TurbulenceField.java` + optional inline `turbulenceEnabled` config
- [ ] **Task 9**: LOD fields + `setLodFactor()` in `ParticleEmitter` + distance check in `ParticleManager`
- [ ] **Task 10**: `EmitterLifecycleListener` interface + complete/stopped callbacks
- [ ] **Task 11**: `emissionStartDelay` + `burstStaggerDuration` in `ParticleDefinition`
- [ ] **Task 12**: `MULTIPLY` in `ParticleBlendMode`
- [ ] **Task 13**: `gravityScaleMin/Max` in `ParticleDefinition` + per-particle `gravityScale`

### Texture
- [ ] Add `SOFT_CIRCLE`, `SOFT_STAR`, `WISP` to `particleTextureStyle` procedural generator
- [ ] Use Gaussian radial falloff formula: `brightness = exp(-r² * 4)`

### Presets (JSON)
- [ ] `campfire_core.json`, `campfire_flame.json`, `campfire_smoke.json`, `campfire_embers.json`
- [ ] `explosion_shockwave.json`, `explosion_fireball.json`, `explosion_debris.json`
- [ ] `magic_ring.json`, `heal_aura.json`, `electric_arc.json`
- [ ] `rain.json`, `snow.json`, `dust_mote.json`

### Tests
- [ ] Test depth sort correctness: 100 ALPHA particles at varying Z depths, verify render order
- [ ] Test `warmup()`: emitter at `t=2s` state matches one that ran from `t=0` for 2 seconds
- [ ] Test lifecycle callback: burst emitter fires `onEmitterComplete` after last particle dies
- [ ] Test LOD: emission rate drops correctly at defined distances
- [ ] Test velocity inheritance: emitter moving at 3 m/s, particles spawn with correct momentum offset

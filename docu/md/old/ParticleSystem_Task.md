# Particle System Upgrade Plan — Resonance

## Objective

Move the particle system from a single-emitter proof-of-concept to a professional, effect-driven framework. The upgrade should preserve the existing emitter implementation while introducing:

- effect-level composition
- reusable multi-emitter bundles
- world interaction and physics response
- shader polish and advanced animation
- high-performance batching and async updates

---

## Phase 1: Architectural Foundation (The "Effect" Layer)

This phase introduces a proper effect abstraction and centralized runtime management.

### 1.1 ParticleEffect wrapper

Create `ParticleEffect` as a wrapper for multiple emitters:

- holds `Array<ParticleEmitter>`
- owns shared transforms and lifetime state
- `setPosition(x, y, z)` moves all child emitters together
- `setRotation(...)` or `setOrientation(...)` rotates the composite effect
- `setActive(boolean)` enables / disables the whole effect
- `reset()` restarts all child emitters as one unit

### 1.2 Unified transform control

Expose effect-level helpers that forward to children:

- `setPosition(...)`
- `translate(...)`
- `setRotation(...)`
- `setScale(...)`
- `setAlpha(float)` or `setTint(Color)` optional global modulation

### 1.3 Effect bundle presets

Extend `ParticlePresetStore` so it can load "effect bundles":

- bundle files are JSON objects containing multiple entries
- each entry references a preset name and a local offset
- optional entry fields: `presetName`, `offset`, `rotation`, `scale`
- support named effect bundles like `fireball`, `dust_cloud`, `spell_trail`

Example bundle structure:

```json
{
  "name": "FireballEffect",
  "entries": [
    { "preset": "fire_core", "offset": [0, 0, 0] },
    { "preset": "fire_glow", "offset": [0, 0.2, 0], "scale": 1.3 },
    { "preset": "ember_sparks", "offset": [0, -0.1, 0] }
  ]
}
```

### 1.4 Global particle manager

Create a centralized manager, e.g. `ParticleManager`, responsible for:

- updating all active `ParticleEffect` and `ParticleEmitter` instances
- rendering only visible particles
- managing shared resources and buffers
- owning camera-based culling logic

### 1.5 Culling and shared buffers

Implement manager-level performance improvements:

- cull emitters/effects outside the camera frustum
- skip update/render for offscreen effects
- use a shared vertex/index buffer instead of a per-emitter `Mesh`
- batch particle quads into a larger `Mesh` or `VertexBufferObject`

---

## Phase 2: Environmental Interaction & Physics

Give particles a sense of the world instead of letting them float in vacuum.

### 2.1 Collision planes

Add collision support to `ParticleEmitter` or `ParticleDefinition`:

- define a lightweight `CollisionPlane` type
- each effect/emitter can hold a list of collision planes
- planes are defined by normal and distance
- support floor, walls, and simple level geometry approximations

### 2.2 Stick / bounce / die behavior

Add a per-definition collision response mode:

- `DIE` — particle disappears on impact
- `STICK` — particle remains at the contact point
- `BOUNCE` — particle reflects and continues

New `ParticleDefinition` fields:

```java
public enum CollisionResponse { DIE, STICK, BOUNCE }
public CollisionResponse collisionResponse = CollisionResponse.DIE;
public float bounceDamping = 0.5f;
public float stickLifeExtension = 0f;
```

### 2.3 Force fields

Define a `ForceField` interface:

```java
public interface ForceField {
    Vector3 sample(Vector3 position, float delta);
}
```

Implement common force fields:

- `PointAttractor` / gravity well
- `VectorField` / wind zone
- optionally `RadialRepeller` and `turbulence`

### 2.4 Global force sampling

Update the emitter update loop so particles sample global forces:

- manager maintains active force fields
- each `ParticleEmitter.update()` asks the manager for combined forces
- apply forces before velocity integration

This enables effects like sparks being pulled into a vortex or smoke drifting in a wind stream.

---

## Phase 3: Visual Polish & Material System

Improve the renderer and make the particle system feel polished and cinematic.

### 3.1 Soft particles

Upgrade the shader pipeline to support depth-aware blending:

- pass a `u_depthTexture` to the particle shader
- compare particle depth vs world depth in the fragment shader
- fade particle alpha as it approaches geometry
- reduce hard intersection edges with floors and walls

This is especially important for smoke, mist, and glow effects.

### 3.2 Ribbons and trails

Add a new `TrailEmitter` type:

- stores a history of positions or sample points
- generates a continuous triangle strip instead of independent quads
- useful for sword slashes, rocket trails, and magic ribbons
- supports variable width, color over life, and fading tail length

### 3.3 Advanced sprite-sheet animation

Improve texture-sheet support with two artist-friendly features:

- Random start frame: choose a random initial frame per particle to avoid tiling artifacts
- Sub-UV blending: interpolate between adjacent frames for smooth flipbook animation

Add definition fields for random start offset and frame blending.

### 3.4 Material-level polish

Consider these additions:

- configurable soft particle fade distance
- global tint/multiplicative color
- optional normal-based lighting for sprite particles
- additive / alpha / screen blend presets tuned for mobile

---

## Phase 4: Performance & Tooling (The "Professional" Tier)

This phase targets high particle counts and production-quality performance.

### 4.1 GPU instancing

Rewrite rendering to use GLES30 instanced draws:

- render a single quad mesh once
- upload per-instance data buffer (position, color, scale, rotation, UV region)
- call `glDrawElementsInstanced`
- avoid rebuilding vertex data every frame when possible

This should dramatically increase particle throughput.

### 4.2 Async particle updates

Move CPU-heavy particle logic off the main render thread:

- use `AsyncExecutor` for particle updates
- keep rendering on the main thread
- synchronize only the minimum instance data needed for draw

This reduces frame-cost spikes for effects with thousands of particles.

### 4.3 Tooling and quality

Add supporting tools and checks for a professional workflow:

- preset bundle editor or JSON schema docs
- effect preview screen with live transform controls
- perf counters for active particles, draw calls, and culling
- validation warnings for invalid presets and missing textures

---

## Notes

- Keep the existing emitter/definition architecture intact wherever possible.
- Build the system incrementally: Phase 1 can be implemented without Phase 3 or 4.
- The highest priority is composable effects and a centralized manager that can scale.
- Later phases should reuse the same `ParticleEmitter` core rather than replacing it wholesale.

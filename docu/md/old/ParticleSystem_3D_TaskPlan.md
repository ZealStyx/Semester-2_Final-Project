# Particle System — 3D Upgrade Task Plan

## What Is Changing and Why

The current system renders every particle as a **2D quad billboard** — a flat rectangle always facing the camera, textured with a sprite sheet. This works for smoke and fire but is fundamentally limited:

- Particles have no real 3D volume or shape
- Rotation only happens in 2D (Z-axis spin on the screen plane)
- Sprites require texture assets and atlas management
- Lighting is a single diffuse dot product — no specularity, no Fresnel, no material feel
- The retro scanline/dither pass is baked into the particle shader with no way to turn it off

The goal is to replace this with a **3D geometry particle system** where each particle is a real mesh (cube, icosahedron, tetrahedron, etc.) with full 3D rotation, a proper material/lighting model, and no sprite dependency.

---

## Current State — What Exists

| File | Current Role |
|------|-------------|
| `particle_shader.vert` | Positions a flat quad, applies 2D Z-rotation, passes sprite UV regions |
| `particle_shader.frag` | Samples 2D texture with sprite UV blending, adds emissive, applies retro scanline/dither baked-in |
| `ParticleEmitter.java` | QUAD_VERTICES + QUAD_INDICES as shared mesh; instance data carries `rotation (1 float)` and `spriteRegion (8 floats)` |
| `ParticleDefinition.java` | Has `texturePath`, `particleTextureStyle`, and 7 sprite-animation fields |
| `ParticleInstance` (inner) | Has `rotation (float)`, `rotationSpeed (float)` — 2D only |
| All presets (JSON) | Have `particleTextureStyle`, `blendMode`, etc. |

---

## Phases

---

## Phase 1 — Strip the Sprite System

**Goal:** Remove all sprite/texture-atlas infrastructure cleanly before building the 3D layer.

### 1.1 — Fields to Remove from `ParticleDefinition`

Delete these fields entirely. They have no meaning in a 3D particle system:

```
texturePath
particleTextureStyle        (CIRCLE, SQUARE, DIAMOND, STAR, CROSS)
spriteAnimEnabled
spriteColumns
spriteRows
spriteFrameCount
spriteFPS
spriteRandomStart
spriteAgeLinked
spriteBlendEnabled
```

Also remove from `sanitize()`: all sprite-related validation blocks.

### 1.2 — Fields to Remove from `ParticleInstance` (inner class)

```
spriteTimer
spriteStartFrame
spriteBlendFactor
```

### 1.3 — Methods to Remove from `ParticleEmitter`

```
computeSpriteRegions()
computeSpriteRegion()      (dead duplicate)
fillSpriteRegion()
getSpriteFrameCount()
floorMod()                 (only used by sprite methods)
```

Also remove from instance data packing (`renderInstanced()`):
- `spriteRegion[0..3]`
- `spriteRegionNext[0..3]`
- `spriteBlendFactor`

Remove the two `float[] spriteRegion` and `float[] spriteRegionNext` working buffers from the class.

### 1.4 — Instance Attribute Changes

Remove from `INSTANCE_ATTRIBUTES`:
```
a_instanceSpriteRegion         (vec4)
a_instanceSpriteRegionNext     (vec4)
a_instanceSpriteBlendFactor    (float)
```

Update `INSTANCE_STRIDE` accordingly. Current stride is 23 floats. After sprite removal it drops to **10 floats**:

```
position          3
scale             1
rotation          1   (still 2D for now — will become quaternion in Phase 2)
color             4
normal            3
ageRatio          1
emissiveStrength  1
= 14 floats (pre-Phase 2)
```

### 1.5 — Shader Changes

**`particle_shader.vert`** — Remove:
```glsl
attribute vec4 a_instanceSpriteRegion;
attribute vec4 a_instanceSpriteRegionNext;
attribute float a_instanceSpriteBlendFactor;
uniform vec4 u_spriteRegion;
uniform vec4 u_spriteRegionNext;
uniform float u_spriteBlendFactor;
varying vec2 v_uvA;
varying vec2 v_uvB;
varying float v_spriteBlendFactor;
```

**`particle_shader.frag`** — Remove:
```glsl
uniform sampler2D u_particleTexture;
varying vec2 v_uvA;
varying vec2 v_uvB;
varying float v_spriteBlendFactor;
// All texture2D() calls
// The sprite mix() call
```

Replace the entire color source in frag with just `v_color` from the vertex stage.

### 1.6 — Retro Scanline / Dither — Make It Optional

The retro effect (scanline + 8-bit dither) is currently unconditionally applied to every particle. Add a uniform to toggle it:

```glsl
uniform float u_retroEnabled;  // 0.0 = off, 1.0 = on
```

In Java, add to `ParticleDefinition`:
```java
public boolean retroEffect = false;  // default OFF for 3D particles
```

Pass to shader in `render()`. When off, skip the scanline and dither floor operations entirely.

### 1.7 — Update All JSON Presets

Remove sprite-related keys from every preset file:

Files to update:
- `default.json` — remove `particleTextureStyle`, `texturePath`
- `fire.json`, `smoke.json`, `mist.json`, `smoke_puff.json`
- `explosion.json`, `ring_shot.json`, `sonar_pulse.json`, `spiral.json`

### Phase 1 Deliverables Checklist
- [ ] All 9 sprite fields removed from `ParticleDefinition` + `sanitize()`
- [ ] 3 sprite fields removed from `ParticleInstance`
- [ ] 4 sprite methods removed from `ParticleEmitter`
- [ ] Instance attributes updated, `INSTANCE_STRIDE` corrected
- [ ] Vertex shader: sprite UV attributes and varyings removed
- [ ] Fragment shader: texture sampler and UV mix removed, `u_retroEnabled` uniform added
- [ ] All preset JSON files cleaned up
- [ ] `ParticleTestScreen` test cases updated to not set any sprite fields

---

## Phase 2 — 3D Geometry Per Particle

**Goal:** Replace the single shared quad with real 3D meshes. Particles become volumetric objects with true 3D rotation.

### 2.1 — New Enum: `ParticleMeshType`

```java
public enum ParticleMeshType {
    QUAD,            // kept as fallback — flat billboard (legacy, being phased out)
    CUBE,            // 6 faces, 12 triangles — the default 3D particle
    TETRAHEDRON,     // 4 faces, 4 triangles — sharp angular crystal shard feel
    OCTAHEDRON,      // 8 faces, 8 triangles — diamond/gem shape
    ICOSAHEDRON,     // 20 faces, 20 triangles — nearest to a sphere, smooth
    CAPSULE,         // cylinder with hemisphere caps — elongated sparks, debris
    PYRAMID,         // square base + 4 triangular sides — 5 faces total
    CUSTOM           // loaded from .obj or built by code
}
```

Add to `ParticleDefinition`:
```java
public String particleMeshType = "CUBE";   // default to 3D cube
public String customMeshPath   = "";       // .obj path if CUSTOM
```

### 2.2 — `Particle3DMeshFactory` — New Class

Generates the base `Mesh` objects for each `ParticleMeshType`. Called once at setup, cached and reused across all particles of the same emitter.

```java
public final class Particle3DMeshFactory {

    // Each mesh is a unit-scale shape centered at (0,0,0)
    public static Mesh build(ParticleMeshType type) {
        switch (type) {
            case CUBE:          return buildCube();
            case TETRAHEDRON:   return buildTetrahedron();
            case OCTAHEDRON:    return buildOctahedron();
            case ICOSAHEDRON:   return buildIcosahedron();
            case CAPSULE:       return buildCapsule(8, 4); // 8 sides, 4 rings
            case PYRAMID:       return buildPyramid();
            case QUAD:          return buildQuad();        // legacy
            default:            return buildCube();
        }
    }

    // Vertex format: position (3) + normal (3) = 6 floats per vertex
    // No UVs — color comes entirely from the instance/material
}
```

**Vertex format for 3D particles (no UVs):**
```
a_position    vec3   (3 floats)
a_normal      vec3   (3 floats)
= 6 floats per vertex
```

**Icosahedron geometry (key shape — produces smooth sphere-like particles):**
```
12 vertices using golden ratio:
  φ = (1 + √5) / 2
  (±1, ±φ, 0), (0, ±1, ±φ), (±φ, 0, ±1)
  normalized to unit sphere
20 triangular faces
```

**Cube geometry:**
```
8 unique positions
6 faces × 2 triangles × 3 vertices = 36 indices
Per-face normals (flat shading): each face has its own normal
```

### 2.3 — Full 3D Rotation Per Particle

Replace the single `rotation (float)` with a **quaternion** per particle.

**In `ParticleInstance`:**
```java
// Remove:
float rotation;        // 2D Z-rotation angle in degrees
float rotationSpeed;   // 2D degrees per second

// Add:
final Quaternion orientation   = new Quaternion().idt();
final Vector3   angularVelocity = new Vector3();  // radians/sec on each axis
```

**Spawn (in `createParticle()`):**
```java
// Read from definition ranges
particle.orientation.idt();  // start with identity
particle.angularVelocity.set(
    MathUtils.random(def.angularVelocityXMin, def.angularVelocityXMax),
    MathUtils.random(def.angularVelocityYMin, def.angularVelocityYMax),
    MathUtils.random(def.angularVelocityZMin, def.angularVelocityZMax)
);
```

**Update (per-frame in `updateActiveParticles()`):**
```java
// Integrate angular velocity into orientation quaternion
float ax = particle.angularVelocity.x * delta;
float ay = particle.angularVelocity.y * delta;
float az = particle.angularVelocity.z * delta;
Quaternion deltaRot = new Quaternion().setEulerAnglesRad(ax, ay, az);
particle.orientation.mul(deltaRot);
particle.orientation.nor();
```

**Add to `ParticleDefinition`:**
```java
public float angularVelocityXMin = -90f;  // degrees/sec
public float angularVelocityXMax =  90f;
public float angularVelocityYMin = -90f;
public float angularVelocityYMax =  90f;
public float angularVelocityZMin = -90f;
public float angularVelocityZMax =  90f;

public float angularDamping = 0f;  // 0 = no damping, 1 = instant stop
```

Apply angular damping each frame:
```java
if (definition.angularDamping > 0f) {
    float dampFactor = 1f - MathUtils.clamp(definition.angularDamping * delta, 0f, 1f);
    particle.angularVelocity.scl(dampFactor);
}
```

### 2.4 — Non-Uniform Scale (3-Axis)

Current system scales uniformly (one `currentSize` float). 3D particles should stretch independently per axis.

**In `ParticleInstance`:**
```java
// Replace:
float currentSize;

// With:
float scaleX;
float scaleY;
float scaleZ;
```

**In `ParticleDefinition`:**
```java
// Start scale ranges per axis
public float startScaleXMin = 0.06f;   public float startScaleXMax = 0.18f;
public float startScaleYMin = 0.06f;   public float startScaleYMax = 0.18f;
public float startScaleZMin = 0.06f;   public float startScaleZMax = 0.18f;

// End scale ranges per axis
public float endScaleXMin = 0.01f;     public float endScaleXMax = 0.08f;
public float endScaleYMin = 0.01f;     public float endScaleYMax = 0.08f;
public float endScaleZMin = 0.01f;     public float endScaleZMax = 0.08f;

// Uniform scale shorthand — if set, overrides all per-axis values
// (kept for backwards compat with existing presets)
public float startSizeMin = -1f;   // -1 = use per-axis values
public float startSizeMax = -1f;
public float endSizeMin   = -1f;
public float endSizeMax   = -1f;
```

### 2.5 — Model Transform for 3D

Replace the current 2D matrix (translate + Z-rotate + scale) with a full 3D transform:

```java
// In updateActiveParticles():
particle.modelTransform.idt();
particle.modelTransform.translate(particle.position);
particle.modelTransform.rotate(particle.orientation);   // quaternion rotation
particle.modelTransform.scale(particle.scaleX, particle.scaleY, particle.scaleZ);
```

### 2.6 — Instance Data for 3D

Update instance buffer layout — quaternion replaces the 2D rotation float, and scale is now 3 floats:

```
a_instancePosition        vec3    (3 floats)
a_instanceScaleXYZ        vec3    (3 floats)       ← was 1 float
a_instanceOrientationQuat vec4    (4 floats)       ← was 1 float (angle)
a_instanceColor           vec4    (4 floats)
a_instanceNormal          vec3    (3 floats)
a_instanceAgeRatio        float   (1 float)
a_instanceEmissiveStrength float  (1 float)
= 19 floats per instance
```

**INSTANCE_STRIDE = 19**

### 2.7 — Vertex Shader Rewrite (3D)

```glsl
#ifdef GL_ES
precision mediump float;
#endif

attribute vec3  a_position;
attribute vec3  a_normal;

// Instance data
attribute vec3  a_instancePosition;
attribute vec3  a_instanceScaleXYZ;
attribute vec4  a_instanceOrientationQuat;   // quaternion (x,y,z,w)
attribute vec4  a_instanceColor;
attribute vec3  a_instanceNormal;
attribute float a_instanceAgeRatio;
attribute float a_instanceEmissiveStrength;

// Fallback uniforms (non-instanced path)
uniform mat4  u_projViewTrans;
uniform mat4  u_modelTrans;
uniform vec4  u_color;
uniform vec3  u_particleNormal;
uniform float u_emissiveStrength;
uniform float u_ageRatio;
uniform float u_useInstancing;

varying vec3  v_worldPos;
varying vec3  v_worldNormal;
varying vec4  v_color;
varying float v_ageRatio;
varying float v_emissiveStrength;

// Rotate a vector by a quaternion
vec3 rotateByQuat(vec3 v, vec4 q) {
    vec3 t = 2.0 * cross(q.xyz, v);
    return v + q.w * t + cross(q.xyz, t);
}

void main() {
    vec3  instancePos;
    vec3  instanceScale;
    vec4  instanceQuat;
    vec4  instanceColor;
    float instanceAgeRatio;
    float instanceEmissive;

    if (u_useInstancing > 0.5) {
        instancePos     = a_instancePosition;
        instanceScale   = a_instanceScaleXYZ;
        instanceQuat    = a_instanceOrientationQuat;
        instanceColor   = a_instanceColor;
        instanceAgeRatio = a_instanceAgeRatio;
        instanceEmissive = a_instanceEmissiveStrength;
    } else {
        // Non-instanced: read from u_modelTrans
        // (extract position/scale from the matrix for compatibility)
        instancePos     = u_modelTrans[3].xyz;
        instanceScale   = vec3(length(u_modelTrans[0].xyz),
                               length(u_modelTrans[1].xyz),
                               length(u_modelTrans[2].xyz));
        instanceQuat    = vec4(0.0, 0.0, 0.0, 1.0); // identity
        instanceColor   = u_color;
        instanceAgeRatio = u_ageRatio;
        instanceEmissive = u_emissiveStrength;
    }

    // Scale then rotate then translate
    vec3 scaledPos   = a_position * instanceScale;
    vec3 rotatedPos  = rotateByQuat(scaledPos, instanceQuat);
    vec3 worldPos    = instancePos + rotatedPos;

    // Transform normal — only rotation, no scale
    vec3 worldNormal = normalize(rotateByQuat(a_normal, instanceQuat));

    v_worldPos        = worldPos;
    v_worldNormal     = worldNormal;
    v_color           = instanceColor;
    v_ageRatio        = instanceAgeRatio;
    v_emissiveStrength = instanceEmissive;

    gl_Position = u_projViewTrans * vec4(worldPos, 1.0);
}
```

### Phase 2 Deliverables Checklist
- [ ] `ParticleMeshType.java` enum created
- [ ] `Particle3DMeshFactory.java` with CUBE, TETRAHEDRON, OCTAHEDRON, ICOSAHEDRON, CAPSULE, PYRAMID
- [ ] `ParticleInstance` updated: quaternion + angular velocity replacing 2D rotation
- [ ] `ParticleDefinition` updated: per-axis scale ranges, angular velocity ranges, `particleMeshType`
- [ ] `ParticleEmitter` updated: mesh selected at `setDefinition()` time, full 3D matrix in update
- [ ] Instance buffer layout updated to 19 floats (INSTANCE_STRIDE = 19)
- [ ] `particle_shader.vert` fully rewritten with quaternion rotation
- [ ] Angular damping implemented
- [ ] Non-uniform scale working per particle

---

## Phase 3 — 3D Material & Lighting System

**Goal:** Replace the single-channel emissive + flat diffuse with a proper material model that makes 3D particles look physically interesting.

### 3.1 — `ParticleMaterial` — New Data Class

```java
public class ParticleMaterial {
    // Base color is already handled by colorGradient / startColor / endColor

    // Diffuse lighting
    public float diffuseStrength   = 0.7f;   // 0 = unlit, 1 = full diffuse
    public float ambientStrength   = 0.25f;  // minimum light level (prevents pure black)

    // Specular (Blinn-Phong)
    public boolean specularEnabled = false;
    public float   specularStrength = 0.5f;
    public float   shininess        = 32f;   // Blinn-Phong exponent
    public float[] specularColor    = {1f, 1f, 1f};

    // Fresnel / Rim
    public boolean fresnelEnabled  = false;
    public float   fresnelStrength = 0.8f;
    public float   fresnelPower    = 2.5f;   // higher = thinner rim
    public float[] fresnelColor    = {0.4f, 0.8f, 1f};

    // Emissive (already in ParticleDefinition — integrate here)
    // emissiveStrength, emissiveColor, emissiveCurve remain on ParticleDefinition

    // Transparency / fade
    public float edgeFadeStrength  = 0f;     // 0 = no fade, 1 = fade toward silhouette
    public float edgeFadePower     = 2f;

    // Wireframe (debug and stylistic)
    public boolean wireframe       = false;
    public float   wireframeWidth  = 0.02f;

    // Color channel masking
    // Multiplied against the particle base color — can create metallic or
    // painted effects by tinting reflections separately from diffuse
    public float[] diffuseTint     = {1f, 1f, 1f};
    public float[] specularTint    = {1f, 1f, 1f};
}
```

Embed `ParticleMaterial` inside `ParticleDefinition`:

```java
public ParticleMaterial material = new ParticleMaterial();
```

JSON serializes as a nested object:
```json
{
  "material": {
    "diffuseStrength": 0.7,
    "specularEnabled": true,
    "specularStrength": 0.6,
    "shininess": 64,
    "fresnelEnabled": true,
    "fresnelStrength": 0.9,
    "fresnelColor": [0.3, 0.9, 1.0]
  }
}
```

### 3.2 — Fragment Shader Rewrite (3D Lighting)

```glsl
#ifdef GL_ES
precision mediump float;
#endif

varying vec3  v_worldPos;
varying vec3  v_worldNormal;
varying vec4  v_color;
varying float v_ageRatio;
varying float v_emissiveStrength;

// Scene
uniform vec3  u_lightDirection;
uniform vec3  u_lightColor;
uniform vec3  u_cameraPos;

// Emissive
uniform vec3  u_emissiveColor;
uniform float u_emissiveFalloff;

// Material
uniform float u_diffuseStrength;
uniform float u_ambientStrength;
uniform float u_specularEnabled;
uniform float u_specularStrength;
uniform float u_shininess;
uniform vec3  u_specularColor;
uniform float u_fresnelEnabled;
uniform float u_fresnelStrength;
uniform float u_fresnelPower;
uniform vec3  u_fresnelColor;
uniform float u_edgeFadeStrength;
uniform float u_edgeFadePower;

// Fog
uniform vec3  u_fogColor;
uniform float u_fogStart;
uniform float u_fogEnd;

// Retro toggle
uniform float u_retroEnabled;
uniform float u_time;

float hash(vec2 v) {
    return fract(sin(dot(v, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec3  N   = normalize(v_worldNormal);
    vec3  L   = normalize(u_lightDirection);
    vec3  V   = normalize(u_cameraPos - v_worldPos);
    vec3  H   = normalize(L + V);   // half-vector for Blinn-Phong

    // --- Diffuse (Lambert) ---
    float NdotL   = max(dot(N, L), 0.0);
    float diffuse = mix(1.0, u_ambientStrength + (1.0 - u_ambientStrength) * NdotL, u_diffuseStrength);
    vec3  lit     = v_color.rgb * diffuse * u_lightColor;

    // --- Specular (Blinn-Phong) ---
    vec3 spec = vec3(0.0);
    if (u_specularEnabled > 0.5) {
        float NdotH = max(dot(N, H), 0.0);
        spec = u_specularColor * u_specularStrength * pow(NdotH, max(u_shininess, 1.0));
    }

    // --- Fresnel / Rim ---
    vec3 rim = vec3(0.0);
    if (u_fresnelEnabled > 0.5) {
        float NdotV  = max(dot(N, V), 0.0);
        float fresnel = pow(1.0 - NdotV, u_fresnelPower);
        rim = u_fresnelColor * u_fresnelStrength * fresnel;
    }

    // --- Emissive ---
    float distToCamera = distance(u_cameraPos, v_worldPos);
    float emissiveAtt  = 1.0 / (1.0 + (u_emissiveFalloff * distToCamera));
    float ageFade      = 1.0 - smoothstep(0.65, 1.0, v_ageRatio);
    vec3  emissive     = u_emissiveColor * v_emissiveStrength * emissiveAtt * ageFade;

    // --- Edge fade (silhouette transparency) ---
    float alpha = v_color.a;
    if (u_edgeFadeStrength > 0.0) {
        float NdotV2 = max(dot(N, V), 0.0);
        float edgeFade = pow(NdotV2, u_edgeFadePower);
        alpha *= mix(1.0, edgeFade, u_edgeFadeStrength);
    }

    // Combine
    vec3 finalColor = lit + spec + rim + emissive;

    // --- Fog ---
    float fogRange  = max(0.0001, u_fogEnd - u_fogStart);
    float fogFactor = clamp((u_fogEnd - distToCamera) / fogRange, 0.0, 1.0);
    finalColor = mix(u_fogColor, finalColor, fogFactor);

    // --- Retro (optional) ---
    if (u_retroEnabled > 0.5) {
        float scanline = 0.92 + 0.08 * sin((gl_FragCoord.y * 1.5) + (u_time * 20.0));
        float dither   = hash(gl_FragCoord.xy + vec2(u_time * 11.0, u_time * 13.0));
        finalColor *= scanline;
        finalColor  = floor(finalColor * 8.0 + dither * 0.25) / 8.0;
    }

    if (alpha < 0.004) discard;
    gl_FragColor = vec4(finalColor, alpha);
}
```

### 3.3 — Material Uniforms in Java

In `ParticleEmitter.render()`, after existing uniforms, add:

```java
ParticleMaterial mat = definition.material;
shaderProgram.setUniformf("u_lightColor",       1f, 1f, 1f);  // or from scene
shaderProgram.setUniformf("u_diffuseStrength",  mat.diffuseStrength);
shaderProgram.setUniformf("u_ambientStrength",  mat.ambientStrength);
shaderProgram.setUniformf("u_specularEnabled",  mat.specularEnabled ? 1f : 0f);
shaderProgram.setUniformf("u_specularStrength", mat.specularStrength);
shaderProgram.setUniformf("u_shininess",        mat.shininess);
shaderProgram.setUniformf("u_specularColor",    mat.specularColor[0], mat.specularColor[1], mat.specularColor[2]);
shaderProgram.setUniformf("u_fresnelEnabled",   mat.fresnelEnabled ? 1f : 0f);
shaderProgram.setUniformf("u_fresnelStrength",  mat.fresnelStrength);
shaderProgram.setUniformf("u_fresnelPower",     mat.fresnelPower);
shaderProgram.setUniformf("u_fresnelColor",     mat.fresnelColor[0], mat.fresnelColor[1], mat.fresnelColor[2]);
shaderProgram.setUniformf("u_edgeFadeStrength", mat.edgeFadeStrength);
shaderProgram.setUniformf("u_edgeFadePower",    mat.edgeFadePower);
shaderProgram.setUniformf("u_retroEnabled",     definition.retroEffect ? 1f : 0f);
```

### 3.4 — Light Direction Control

Add to `ParticleEmitter` (or `ParticleManager`):

```java
// Currently hardcoded as (0.35, 1, 0.2) — make it settable
private final Vector3 lightDirection = new Vector3(0.35f, 1f, 0.2f).nor();
private final Vector3 lightColor     = new Vector3(1f, 1f, 1f);

public void setLight(Vector3 direction, Vector3 color) {
    lightDirection.set(direction).nor();
    lightColor.set(color);
}
```

### Phase 3 Deliverables Checklist
- [ ] `ParticleMaterial.java` created with all fields
- [ ] `ParticleDefinition` embeds `material` as a nested object
- [ ] `particle_shader.frag` rewritten — Blinn-Phong + Fresnel + edge fade
- [ ] `u_retroEnabled` uniform working (off by default)
- [ ] All material uniforms passed in `render()`
- [ ] `setLight()` method on `ParticleEmitter` and `ParticleManager`
- [ ] Depth mask correctly enabled (3D particles must write depth)

---

## Phase 4 — Depth Write and Sorting Fix

**Goal:** 3D particles have volume and must interact correctly with the depth buffer. The current system disables depth write (`glDepthMask(false)`) for all particles — this was fine for flat sprites but breaks 3D geometry.

### 4.1 — Depth Write Mode

Add to `ParticleDefinition`:
```java
// WRITE:  particle writes to depth buffer (correct for opaque/additive 3D)
// IGNORE: particle does not write depth (correct for ALPHA-blended smoke)
// READ_ONLY: particle reads depth (soft particles) but does not write
public String depthMode = "WRITE";
```

In `render()`:
```java
switch (DepthMode.fromName(definition.depthMode)) {
    case WRITE:     Gdx.gl.glDepthMask(true);  break;
    case IGNORE:    Gdx.gl.glDepthMask(false); break;
    case READ_ONLY: Gdx.gl.glDepthMask(false); break;
}
```

### 4.2 — Depth Sort for ALPHA Mode

Already planned in the Gap Analysis. Now that particles are 3D, sorting by center position is sufficient for most cases. Add:

```java
public boolean depthSort = false;
```

Sort `activeParticles` back-to-front by `particle.position.dst2(camera.position)` before rendering when `depthSort = true` AND `blendMode = ALPHA`.

Use an **insertion sort** (fast for nearly-sorted sequences, which depth order typically is frame-to-frame):

```java
private void insertionSortByDepth(PerspectiveCamera camera) {
    for (int i = 1; i < activeParticles.size; i++) {
        ParticleInstance key = activeParticles.get(i);
        float keyDist = key.position.dst2(camera.position);
        int j = i - 1;
        while (j >= 0 && activeParticles.get(j).position.dst2(camera.position) < keyDist) {
            activeParticles.set(j + 1, activeParticles.get(j));
            j--;
        }
        activeParticles.set(j + 1, key);
    }
}
```

### Phase 4 Deliverables Checklist
- [ ] `DepthMode` enum: WRITE, IGNORE, READ_ONLY
- [ ] `depthMode` field in `ParticleDefinition`
- [ ] `glDepthMask` controlled per-render based on mode
- [ ] `depthSort` field + insertion sort implementation
- [ ] Auto-depth-write enabled for ADDITIVE 3D particles

---

## Phase 5 — Customization Expansion

**Goal:** Add the remaining customization knobs that make the system "very customizable" as requested.

### 5.1 — Rotation Lock Axes

Sometimes you want a particle to spin only on one axis (e.g., a coin tumbling on Y but not tilting on X):

```java
public boolean lockRotationX = false;
public boolean lockRotationY = false;
public boolean lockRotationZ = false;
```

In update, zero the corresponding angular velocity component if locked:
```java
if (definition.lockRotationX) particle.angularVelocity.x = 0f;
if (definition.lockRotationY) particle.angularVelocity.y = 0f;
if (definition.lockRotationZ) particle.angularVelocity.z = 0f;
```

### 5.2 — Stretch Along Velocity

When a particle is moving fast, stretch its geometry along the velocity direction. Works especially well with CAPSULE and CUBE shapes.

```java
public boolean velocityStretch     = false;
public float   velocityStretchMin  = 0f;    // minimum stretch added
public float   velocityStretchMax  = 3f;    // maximum stretch clamp
public float   velocityStretchAxis = 1f;    // which local axis to stretch (0=X, 1=Y, 2=Z)
```

In `updateActiveParticles()`, if `velocityStretch` is on:
```java
float speed        = particle.velocity.len();
float stretchScale = 1f + MathUtils.clamp(speed * definition.velocityStretchFactor,
                                          definition.velocityStretchMin,
                                          definition.velocityStretchMax);
// Apply to scaleY (or chosen axis)
particle.scaleY = particle.baseScaleY * stretchScale;

// Align orientation to velocity
if (!particle.velocity.isZero(0.0001f)) {
    velocityDir.set(particle.velocity).nor();
    particle.orientation.setFromCross(Vector3.Y, velocityDir);
}
```

### 5.3 — Color by Speed, Age, or Normal

Add secondary color drivers:

```java
public boolean colorBySpeed   = false;
public float[] colorAtMinSpeed = {0f, 0f, 1f, 1f};  // blue = slow
public float[] colorAtMaxSpeed = {1f, 0.2f, 0f, 1f}; // orange = fast
public float   colorSpeedMax  = 5f;

public boolean colorByNormal  = false;  // tint color by world-space normal (Y+)
public float   colorNormalStrength = 0.5f;
```

Applied in `updateActiveParticles()` as a multiplicative tint on top of the gradient color.

### 5.4 — Physics Drag

Add `drag` coefficient so particles decelerate realistically:

```java
public float drag = 0f;  // 0 = no drag, 0.5 = moderate air resistance
```

In update:
```java
// Exponential drag (physically correct)
float dragFactor = 1f - MathUtils.clamp(definition.drag * delta, 0f, 0.99f);
particle.velocity.scl(dragFactor);
```

This replaces the need to use `velocityCurve` for simple deceleration and works together with angular damping.

### 5.5 — Emitter Shape Orientation (Full Rotation)

Currently only RING/DISC shapes support custom orientation via `ringAxisX/Y/Z`. All shapes should be orientable.

Add to `ParticleDefinition`:
```java
// Global emitter rotation applied to all spawn offsets
public float emitterRotationX = 0f;
public float emitterRotationY = 0f;
public float emitterRotationZ = 0f;
```

In `sampleSpawnOffset()`, after computing the raw offset, apply the emitter rotation quaternion.

### 5.6 — Per-Effect Global Scale and Speed Multiplier

When attaching an effect to a big vs. small entity, scale everything proportionally:

Already partially in `ParticleEffect` (`localScale` per emitter). Add global to `ParticleEffect`:

```java
public void setGlobalScale(float scale);    // scales all position offsets and emitter sizes
public void setGlobalSpeed(float speed);    // multiplies all emitter speed ranges
```

### 5.7 — `MULTIPLY` Blend Mode

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

Use case: darkening ground decals, shadow projections, scorch marks overlaid on the scene.

### Phase 5 Deliverables Checklist
- [ ] Rotation lock axes (X/Y/Z) in definition + update
- [ ] Velocity stretch: scale + orientation align to velocity
- [ ] Color by speed + color by normal
- [ ] Drag coefficient
- [ ] Global emitter rotation for all spawn shapes
- [ ] `MULTIPLY` blend mode
- [ ] `setGlobalScale()` / `setGlobalSpeed()` on `ParticleEffect`

---

## Phase 6 — New JSON Presets (3D-Specific)

**Goal:** Replace and expand the existing preset library with 3D-appropriate effects. All existing presets keep backward-compatible fields but gain `particleMeshType` and `material`.

### Updated Existing Presets

| Preset | Mesh | Key Material Setting |
|--------|------|----------------------|
| `fire.json` | TETRAHEDRON | `specularEnabled: false`, `fresnelEnabled: true`, `fresnelColor: [1, 0.5, 0]` |
| `smoke.json` | ICOSAHEDRON | `diffuseStrength: 0.3`, `depthSort: true`, `depthMode: IGNORE` |
| `explosion.json` | OCTAHEDRON | `specularEnabled: true`, `shininess: 16`, `fresnelEnabled: true` |
| `mist.json` | ICOSAHEDRON | `edgeFadeStrength: 0.8`, `depthMode: IGNORE`, `depthSort: true` |
| `default.json` | CUBE | `diffuseStrength: 0.7`, `specularEnabled: true` |

### New Presets to Create

```
crystals.json      — TETRAHEDRON, high specular (shininess: 128), fresnelEnabled, colorByNormal
debris.json        — CUBE, collision bounce, angular velocity high, angularDamping: 0.3
magic_orbs.json    — ICOSAHEDRON, fresnelEnabled, ADDITIVE, emissiveCurve flash
sparks.json        — CAPSULE, velocityStretch: true, high speed, ADDITIVE, short lifetime
shards.json        — OCTAHEDRON, burst, OUTWARD, lockRotationY: true (tumble but not yaw)
embers.json        — TETRAHEDRON, collision BOUNCE, gravity: -4, chaosStrength: 0.4
plasma.json        — ICOSAHEDRON, fresnelColor: [0.5, 0.9, 1], colorBySpeed: true
ground_scorch.json — CUBE (flat: scaleY very small), MULTIPLY blend, horizontal orientation
```

### Phase 6 Deliverables Checklist
- [ ] All 9 existing presets updated (sprite fields removed, `particleMeshType` added, `material` added)
- [ ] 8 new preset JSON files created and tested
- [ ] `fireball.json` bundle updated to reference new preset filenames

---

## Phase 7 — `ParticleTestScreen` Overhaul

**Goal:** The test screen must cover every new feature and provide a visual demo of each 3D mesh type.

### New Test Scenes to Add

| Test ID | Scene | What It Validates |
|---------|-------|-------------------|
| T01 | Spinning cubes fountain | 3D rotation, per-axis angular velocity, CUBE mesh |
| T02 | Crystal shard burst | TETRAHEDRON, high specular, burst mode |
| T03 | Gem-like orbiting particles | OCTAHEDRON, TANGENT direction, vortex |
| T04 | Smooth ember cloud | ICOSAHEDRON, color gradient, emissiveCurve |
| T05 | Tumbling debris | CUBE, collision BOUNCE, angular damping |
| T06 | Velocity-stretched sparks | CAPSULE, velocityStretch, ADDITIVE |
| T07 | Layered fire (no sprite) | TETRAHEDRON + ICOSAHEDRON layers |
| T08 | Alpha-blended mist (depth sort) | ICOSAHEDRON, ALPHA, depthSort |
| T09 | Scorch mark overlay | CUBE flat, MULTIPLY blend |
| T10 | Wireframe debug view | material.wireframe = true |
| T11 | Fresnel rim test | fresnelEnabled, camera orbit to see rim |
| T12 | Color-by-speed | colorBySpeed on fast vs slow emitters |
| T13 | Angular velocity X/Y/Z lock | Confirm each axis locks correctly |
| T14 | Emitter velocity inheritance | Moving emitter, particles trail behind |
| T15 | Retro mode toggle | u_retroEnabled on vs off |

### Phase 7 Deliverables Checklist
- [ ] All 15 test scenes implemented in `ParticleTestScreen`
- [ ] Each test labeled clearly in screen-space text (scene name + active feature)
- [ ] Cycle key (TAB or number keys) to step between tests
- [ ] Active particle count displayed per test
- [ ] Camera orbit enabled for all 3D test scenes

---

## Phase 8 — `ParticleFormulas` Update

Update `ParticleFormulas.java` to use 3D mesh types and material:

```java
// Updated signatures — all now produce 3D particles
public static ParticleDefinition crystalBurst(Color color, float force)
public static ParticleDefinition debrisField(float gravity, float chaos)
public static ParticleDefinition plasmaOrbs(Color innerColor, Color rimColor)
public static ParticleDefinition emberShower(Color hotColor)
public static ParticleDefinition magicRing(Vector3 direction, float radius, Color color)

// Updated existing formulas to use 3D:
ring()        -> CAPSULE mesh, velocityStretch = true
explosion()   -> OCTAHEDRON mesh, specularEnabled = true
sonarPulse()  -> ICOSAHEDRON mesh, edgeFadeStrength = 0.6
vortex()      -> TETRAHEDRON mesh, angular velocity randomized
fountain()    -> ICOSAHEDRON mesh, fresnelEnabled = true
```

### Phase 8 Deliverables Checklist
- [ ] All 6 existing formulas updated to use 3D mesh types + material
- [ ] 5 new formula methods added
- [ ] All formulas tested in `ParticleTestScreen` (one test per formula)

---

## File Change Summary

| File | Change Type | Summary |
|------|------------|---------|
| `ParticleDefinition.java` | **Major edit** | Remove 9 sprite fields, add meshType, material ref, angular velocity ranges, per-axis scale, drag, depthMode, depthSort, rotation locks, velocity stretch, color-by-speed |
| `ParticleEmitter.java` | **Major edit** | New 3D matrix, quaternion rotation, non-uniform scale, depth sort, depth mask, new instance layout, mesh factory call |
| `particle_shader.vert` | **Full rewrite** | Quaternion rotation, non-uniform scale, no sprite UVs |
| `particle_shader.frag` | **Full rewrite** | Blinn-Phong + Fresnel + edge fade, no texture sample, retroEnabled toggle |
| `ParticleMaterial.java` | **New file** | Material data class |
| `Particle3DMeshFactory.java` | **New file** | Generates CUBE, TETRAHEDRON, OCTAHEDRON, ICOSAHEDRON, CAPSULE, PYRAMID meshes |
| `ParticleMeshType.java` | **New file** | Enum |
| `DepthMode.java` | **New file** | Enum (WRITE, IGNORE, READ_ONLY) |
| `ParticleBlendMode.java` | **Minor edit** | Add MULTIPLY |
| `ParticleFormulas.java` | **Major edit** | All formulas updated, 5 new formulas |
| `ParticleTestScreen.java` | **Major edit** | 15 new test scenes, no sprite tests |
| All preset `.json` files | **Edit** | Remove sprite fields, add meshType + material |
| New preset `.json` files (×8) | **New files** | crystals, debris, sparks, shards, embers, plasma, scorch, magic_orbs |

---

## Recommended Order of Work

```
Phase 1  →  Phase 2  →  Phase 4  →  Phase 3  →  Phase 5  →  Phase 6  →  Phase 7  →  Phase 8
Strip       3D geo      Depth       Material     Custom         Presets     Tests       Formulas
sprites     + rotate    fix         system       knobs
```

Start with Phase 1 (pure deletion, low risk). Then Phase 2 (core architecture). Then Phase 4 (depth, must come before material so rendering is already correct). Then 3, 5, 6, 7, 8 in order.

Do not start Phase 3 (material uniforms) until Phase 2 (3D geometry) is rendering correctly — the lighting math has no meaning on a sprite quad.

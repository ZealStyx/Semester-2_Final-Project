#ifdef GL_ES
precision mediump float;
#endif

// Improved Body-Cam VHS fragment shader (Unrecord-style tuning)
// Features: strong aspect-corrected barrel distortion, radial chromatic aberration,
// dynamic vignette (reacts to motion), film grain + subtle scanline jitter.

varying vec2 v_texCoord;
uniform sampler2D u_texture;

uniform vec2 u_resolution;       // screen resolution in pixels
uniform float u_verticalFOV;     // vertical FOV in degrees (used to scale strength)
uniform float u_strength;        // global overall strength (0.0 - 1.0)
uniform float u_k1;              // polynomial coeff for r^2 (positive -> barrel)
uniform float u_k2;              // polynomial coeff for r^4
uniform float u_caAmount;        // chromatic aberration base amount (normalized)
uniform float u_vignetteInner;   // vignette inner radius (0.0 - 1.0)
uniform float u_vignetteOuter;   // vignette outer radius (0.0 - 2.0)
uniform float u_time;            // time in seconds for grain/jitter
uniform float u_motion;          // motion intensity (0..1) to drive vignette/scanline

void main() {
    // normalize coords to -1..1 with center at 0
    vec2 uv = v_texCoord;
    vec2 ndc = uv * 2.0 - 1.0;

    // correct for aspect ratio so distortion stays circular in NDC space
    float aspect = u_resolution.x / max(u_resolution.y, 1.0);
    vec2 aspectScale = vec2(aspect, 1.0);
    ndc *= aspectScale;

    float r2 = dot(ndc, ndc);
    float r4 = r2 * r2;

    // stronger, Unrecord-like polynomial barrel distortion
    // distortion = 1 + k1*r^2 + k2*r^4 (k1,k2 positive => barrel)
    float distortion = 1.0 + u_k1 * r2 + u_k2 * r4;

    // apply distortion and allow global strength and FOV scaling
    float fovFactor = clamp((u_verticalFOV - 30.0) / 60.0, 0.0, 1.0);
    float applyStrength = clamp(u_strength, 0.0, 0.72) * (0.45 + 0.40 * fovFactor);
    vec2 distorted = ndc * mix(1.0, distortion, applyStrength);

    // undo aspect correction
    distorted /= aspectScale;

        // map back to 0..1 texture coords, then hard clamp to a safe border so corners
        // do not stretch outside the frame.
        float border = mix(0.018, 0.045, clamp(u_strength, 0.0, 1.0));
        vec2 sampleUV = clamp((distorted + 1.0) * 0.5, vec2(border), vec2(1.0 - border));

    // compute radial chromatic aberration (diagonal + radial scale)
    vec2 dir = (length(distorted) > 0.0001) ? normalize(distorted) : vec2(0.0);
    float ca = u_caAmount * r2; // stronger toward edges

    // bias per channel for slightly diagonal fringing
    vec2 offsetR = dir * ca * 0.9 + vec2(0.002, 0.001);
    vec2 offsetG = vec2(0.0);
    vec2 offsetB = -dir * ca * 0.6 + vec2(-0.001, -0.002);

        vec2 safeMin = vec2(border);
        vec2 safeMax = vec2(1.0 - border);
        vec3 sampleR = texture2D(u_texture, clamp(sampleUV + offsetR, safeMin, safeMax)).rgb;
        vec3 sampleG = texture2D(u_texture, clamp(sampleUV + offsetG, safeMin, safeMax)).rgb;
        vec3 sampleB = texture2D(u_texture, clamp(sampleUV + offsetB, safeMin, safeMax)).rgb;

    vec3 color = vec3(sampleR.r, sampleG.g, sampleB.b);

    // dynamic vignette: thinner edge ring and partially see-through darkness
    vec2 vignetteUv = uv * 2.0 - 1.0;
    float vignetteR = length(vignetteUv);
    float inner = u_vignetteInner + 0.18;
    float outer = u_vignetteOuter + 0.32;
    float edge = smoothstep(inner, outer, vignetteR);
    float motionBoost = clamp(u_motion * 0.45, 0.0, 1.0);
    float opacity = mix(0.20, 0.32, motionBoost);
    float minVisibility = 0.5;
    float vig = max(1.0 - edge * opacity, minVisibility);
    color *= vig;

    // film grain + subtle scanline jitter
    float grain = (fract(sin(dot(gl_FragCoord.xy ,vec2(12.9898,78.233))) * 43758.5453 + u_time) - 0.5) * 0.04 * u_strength;
    float scan = sin((v_texCoord.y + u_time * 0.5) * u_resolution.y * 0.5) * 0.02 * u_motion * u_strength;
    color += grain + scan;

    // final output with preserved alpha
    gl_FragColor = vec4(color, 1.0);
}

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;

uniform sampler2D u_texture;
uniform vec2 u_screenSize;
uniform float u_time;
uniform float u_vhsStrength;
uniform float u_fovDiagonalDegrees;
uniform float u_barrelDistortionStrength;
uniform float u_chromaticAberrationPixels;
uniform float u_vignetteRadius;
uniform float u_vignetteSoftness;
uniform float u_vhsScanLineStrength;
uniform float u_vhsTapeNoiseAmount;
uniform float u_crtCurveAmount;

float hash(vec2 p) {
    p = fract(p * vec2(0.1031, 0.11369));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

vec2 barrelMap(vec2 uv) {
    vec2 p = uv * 2.0 - 1.0;
    float diagonalScale = clamp((u_fovDiagonalDegrees - 60.0) / 110.0, 0.0, 1.0);
    float lensStrength = mix(0.0, 0.4, diagonalScale) * u_barrelDistortionStrength;
    float r2 = dot(p, p);
    p *= (1.0 + lensStrength * r2);
    return clamp((p * 0.5) + 0.5, 0.0, 1.0);
}

vec3 sampleChromatic(vec2 uv) {
    vec2 safeSize = max(u_screenSize, vec2(1.0));
    vec2 chromaOffset = vec2(u_chromaticAberrationPixels / safeSize.x, 0.0);

    float red = texture2D(u_texture, clamp(uv + chromaOffset, 0.0, 1.0)).r;
    float green = texture2D(u_texture, clamp(uv, 0.0, 1.0)).g;
    float blue = texture2D(u_texture, clamp(uv - chromaOffset, 0.0, 1.0)).b;
    vec3 color = vec3(red, green, blue);

    vec3 center = texture2D(u_texture, clamp(uv, 0.0, 1.0)).rgb;
    float sceneAlpha = max(max(center.r, center.g), center.b);
    return mix(center, color, clamp(sceneAlpha, 0.0, 1.0));
}

void main() {
    vec2 screenP = v_texCoord * 2.0 - 1.0;
    float screenR2 = dot(screenP, screenP);
    vec2 crtUv = clamp((screenP * (1.0 + u_crtCurveAmount * screenR2 * 0.3) * 0.5) + 0.5, 0.0, 1.0);

    vec2 uv = barrelMap(crtUv);
    vec3 color = sampleChromatic(uv);

    float scanline = 1.0 - (sin(v_texCoord.y * u_screenSize.y * 3.14159) * 0.5 + 0.5) * u_vhsScanLineStrength;
    float tapeNoise = (hash(gl_FragCoord.xy + vec2(u_time * 91.7, u_time * 31.9)) - 0.5) * u_vhsTapeNoiseAmount;
    float lineJitter = (hash(vec2(floor(v_texCoord.y * u_screenSize.y), floor(u_time * 24.0))) - 0.5) * 0.006 * u_vhsStrength;

    vec2 jitterUv = vec2(clamp(uv.x + lineJitter, 0.0, 1.0), uv.y);
    color = mix(color, texture2D(u_texture, jitterUv).rgb, 0.5 * u_vhsStrength);
    color *= scanline;
    color += tapeNoise;

    gl_FragColor = vec4(color, 1.0);
}

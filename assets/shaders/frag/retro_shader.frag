#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_worldPos;
varying vec3 v_normal;

uniform vec3 u_cameraPos;
uniform vec3 u_lightDirection;
uniform vec3 u_baseColor;
uniform vec3 u_ambientColor;
uniform vec3 u_shadowColor;
uniform float u_shadowStrength;
uniform vec3 u_fogColor;
uniform vec3 u_blindFogColor;
uniform float u_fogStart;
uniform float u_fogEnd;
uniform float u_blindFogStart;
uniform float u_blindFogEnd;
uniform float u_blindFogStrength;
uniform float u_time;
uniform float u_scanlineStrength;
uniform float u_ditherLevels;
uniform float u_phosphorMaskStrength;

float hash(vec2 p) {
    p = fract(p * vec2(0.1031, 0.11369));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

vec3 applyCrtEffect(vec2 screenCoord, vec3 color) {
    vec2 pixelCoord = floor(screenCoord);
    float scanline = sin(pixelCoord.y * 3.14159 * 2.0 / 3.0);
    scanline = 1.0 - u_scanlineStrength * pow(abs(scanline), 1.8);

    float dither = hash(pixelCoord + vec2(u_time * 0.5));
    float ditherLevels = max(u_ditherLevels, 1.0);

    float x = mod(pixelCoord.x, 3.0);
    vec3 phosphorMask = vec3(0.72, 0.72, 0.72);
    phosphorMask = mix(phosphorMask, vec3(1.0, 0.58, 0.58), 1.0 - step(1.0, x));
    phosphorMask = mix(phosphorMask, vec3(0.58, 1.0, 0.58), step(1.0, x) * (1.0 - step(2.0, x)));
    phosphorMask = mix(phosphorMask, vec3(0.58, 0.58, 1.0), step(2.0, x));

    vec3 crtColor = color * mix(0.82, 1.0, scanline);
    crtColor = floor(crtColor * ditherLevels + dither * 0.8) / ditherLevels;
    crtColor = mix(crtColor, crtColor * phosphorMask, u_phosphorMaskStrength);
    return crtColor;
}

void main() {
    vec3 normal = normalize(v_normal);
    vec3 lightDir = normalize(u_lightDirection);
    vec3 viewDir = normalize(u_cameraPos - v_worldPos);

    float diffuse = max(dot(normal, lightDir), 0.0);
    float shadowFactor = smoothstep(0.2, 0.95, 1.0 - diffuse);

    vec3 litColor = u_baseColor * (u_ambientColor + diffuse);
    litColor = mix(litColor, u_shadowColor, shadowFactor * u_shadowStrength);

    float specular = pow(max(dot(reflect(-lightDir, normal), viewDir), 0.0), 12.0);
    litColor += u_baseColor * specular * 0.25;

    float rim = 1.0 - max(dot(normal, viewDir), 0.0);
    litColor += pow(rim, 3.0) * u_baseColor * 0.12;

    float dist = distance(u_cameraPos, v_worldPos);

    float fogFactor = (u_fogEnd - u_fogStart > 0.001)
        ? clamp((u_fogEnd - dist) / (u_fogEnd - u_fogStart), 0.0, 1.0)
        : 1.0;
    vec3 color = mix(u_fogColor, litColor, fogFactor);

    float blindRange = (u_blindFogEnd - u_blindFogStart > 0.001)
        ? clamp((u_blindFogEnd - dist) / (u_blindFogEnd - u_blindFogStart), 0.0, 1.0)
        : 1.0;
    float blindFactor = clamp((1.0 - blindRange) * u_blindFogStrength, 0.0, 1.0);
    color = mix(color, u_blindFogColor, blindFactor);

    color = applyCrtEffect(gl_FragCoord.xy, color);

    gl_FragColor = vec4(color, 1.0);
}
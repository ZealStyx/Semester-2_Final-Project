#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;

uniform sampler2D u_texture;
uniform vec2 u_screenSize;
uniform float u_time;
uniform float u_vhsStrength;

float hash(vec2 p) {
    p = fract(p * vec2(0.1031, 0.11369));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

vec3 applyVhsEffect(vec2 uv, vec3 color) {
    vec2 size = max(u_screenSize, vec2(1.0));

    float lineSeed = floor(uv.y * size.y * 0.75);
    float timeSeed = floor(u_time * 24.0);
    float lineNoise = hash(vec2(lineSeed, timeSeed));

    float wobble = sin((uv.y * 18.0) + (u_time * 3.25)) * 0.0025;
    wobble += (lineNoise - 0.5) * 0.008 * u_vhsStrength;

    float tear = smoothstep(0.84, 0.98, uv.y) * (0.5 + 0.5 * sin(u_time * 6.0));
    float horizontalShift = wobble + (tear * 0.015 * u_vhsStrength);

    vec3 shifted = texture2D(u_texture, vec2(clamp(uv.x + horizontalShift, 0.0, 1.0), uv.y)).rgb;
    vec3 shiftedUp = texture2D(u_texture, vec2(clamp(uv.x + horizontalShift * 0.6, 0.0, 1.0), clamp(uv.y + 0.0015 * tear, 0.0, 1.0))).rgb;
    vec3 shiftedDown = texture2D(u_texture, vec2(clamp(uv.x - horizontalShift * 0.6, 0.0, 1.0), clamp(uv.y - 0.0015 * tear, 0.0, 1.0))).rgb;

    shifted.r = mix(shifted.r, shiftedUp.r, 0.35 * u_vhsStrength);
    shifted.g = mix(shifted.g, shifted.r, 0.08 * u_vhsStrength);
    shifted.b = mix(shifted.b, shiftedDown.b, 0.35 * u_vhsStrength);

    float scanline = 0.88 + 0.12 * sin((uv.y * size.y) * 3.14159);
    float grain = hash(gl_FragCoord.xy + vec2(u_time * 191.7, u_time * 47.9)) - 0.5;
    float dropout = smoothstep(0.93, 1.0, lineNoise);

    shifted *= scanline;
    shifted += grain * 0.04 * u_vhsStrength;
    shifted *= 1.0 - (dropout * 0.16 * u_vhsStrength);

    vec2 centerUV = uv * 2.0 - 1.0;
    float curvature = 0.08 + (0.06 * u_vhsStrength);
    centerUV *= 1.0 + (curvature * dot(centerUV, centerUV));
    float vignette = 1.0 - 0.22 * dot(centerUV, centerUV);
    shifted *= vignette;

    if (uv.y > 0.965) {
        shifted += vec3(0.02, 0.02, 0.02) * u_vhsStrength;
    }

    return shifted;
}

void main() {
    vec2 uv = v_texCoord;
    vec3 color = texture2D(u_texture, uv).rgb;
    color = applyVhsEffect(uv, color);
    gl_FragColor = vec4(color, 1.0);
}
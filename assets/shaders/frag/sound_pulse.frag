#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_pulseOrigin;
uniform float u_pulseRadius;
uniform float u_pulseThickness;
uniform float u_pulseAlpha;
uniform vec4 u_pulseColor;
uniform vec3 u_colorScale;
uniform float u_time;

varying vec3 v_worldPos;

void main() {
    float dist = length(v_worldPos - u_pulseOrigin);
    float diff = abs(dist - u_pulseRadius);

    float shell = 1.0 - smoothstep(0.0, u_pulseThickness, diff);
    float shimmer = 0.85 + 0.15 * sin(dist * 14.0 + u_time * 8.0);

    float alpha = shell * u_pulseAlpha * shimmer;
    if (alpha < 0.01) {
        discard;
    }

    vec3 color = u_pulseColor.rgb * u_colorScale * shimmer;
    gl_FragColor = vec4(color, alpha);
}

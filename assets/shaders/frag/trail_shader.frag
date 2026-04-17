#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform float u_time;

varying vec4 v_color;
varying vec2 v_uv;

void main() {
    vec4 sampled = texture2D(u_texture, v_uv);
    vec4 color = sampled * v_color;
    if (color.a < 0.01) {
        discard;
    }

    float scanline = 0.94 + 0.06 * sin((gl_FragCoord.y * 0.8) + (u_time * 12.0));
    color.rgb *= scanline;
    gl_FragColor = color;
}

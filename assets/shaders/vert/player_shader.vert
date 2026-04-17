#ifdef GL_ES
precision mediump float;
#endif

attribute vec3 a_position;
attribute vec3 a_normal;

uniform mat4 u_projViewTrans;
uniform mat4 u_modelTrans;

varying vec3 v_worldPos;
varying vec3 v_normal;

void main() {
    vec4 worldPos = u_modelTrans * vec4(a_position, 1.0);
    v_worldPos = worldPos.xyz;
    v_normal = normalize((u_modelTrans * vec4(a_normal, 0.0)).xyz);
    gl_Position = u_projViewTrans * worldPos;
}

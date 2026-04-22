attribute vec3 a_position;

uniform mat4 u_projTrans;
uniform mat4 u_worldTransform;

varying vec3 v_worldPos;

void main() {
    vec4 world = u_worldTransform * vec4(a_position, 1.0);
    v_worldPos = world.xyz;
    gl_Position = u_projTrans * world;
}

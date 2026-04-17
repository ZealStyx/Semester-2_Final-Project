#ifdef GL_ES
precision mediump float;
#endif

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec3 a_instancePosition;
attribute vec3 a_instanceScaleXYZ;
attribute vec4 a_instanceOrientationQuat;
attribute vec4 a_instanceColor;
attribute vec3 a_instanceNormal;
attribute float a_instanceAgeRatio;
attribute float a_instanceEmissiveStrength;

uniform mat4 u_projViewTrans;
uniform mat4 u_modelTrans;
uniform float u_useInstancing;
uniform vec3 u_particleNormal;
uniform vec4 u_color;
uniform float u_emissiveStrength;
uniform float u_ageRatio;

varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec4 v_color;
varying float v_emissiveStrength;
varying float v_ageRatio;

vec3 rotateByQuat(vec3 value, vec4 quaternion) {
    vec3 t = 2.0 * cross(quaternion.xyz, value);
    return value + (quaternion.w * t) + cross(quaternion.xyz, t);
}

void main() {
    vec4 worldPosition;
    vec4 particleColor;
    vec3 particleNormalDirection;
    float particleEmissiveStrength;
    float particleAgeRatio;

    if (u_useInstancing > 0.5) {
        vec3 scaledPosition = a_position * a_instanceScaleXYZ;
        vec3 rotatedPosition = rotateByQuat(scaledPosition, a_instanceOrientationQuat);
        worldPosition = vec4(a_instancePosition + rotatedPosition, 1.0);
        particleColor = a_instanceColor;
        particleNormalDirection = normalize(rotateByQuat(a_normal, a_instanceOrientationQuat));
        particleEmissiveStrength = a_instanceEmissiveStrength;
        particleAgeRatio = a_instanceAgeRatio;
    } else {
        worldPosition = u_modelTrans * vec4(a_position, 1.0);
        particleColor = u_color;
        particleNormalDirection = normalize((u_modelTrans * vec4(a_normal, 0.0)).xyz);
        particleEmissiveStrength = u_emissiveStrength;
        particleAgeRatio = u_ageRatio;
    }

    v_worldPos = worldPosition.xyz;
    v_normal = particleNormalDirection;
    v_color = particleColor;
    v_emissiveStrength = particleEmissiveStrength;
    v_ageRatio = particleAgeRatio;
    gl_Position = u_projViewTrans * worldPosition;
}
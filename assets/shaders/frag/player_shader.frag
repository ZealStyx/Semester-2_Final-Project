#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_worldPos;
varying vec3 v_normal;

uniform vec3 u_cameraPos;
uniform vec3 u_lightDirection;
uniform vec3 u_baseColor;
uniform vec3 u_ambientColor;
uniform vec3 u_rimColor;
uniform float u_rimStrength;

void main() {
    vec3 normal = normalize(v_normal);
    vec3 lightDir = normalize(u_lightDirection);
    vec3 viewDir = normalize(u_cameraPos - v_worldPos);

    float diffuse = max(dot(normal, lightDir), 0.0);
    vec3 color = u_baseColor * (u_ambientColor + diffuse);

    float rim = 1.0 - max(dot(normal, viewDir), 0.0);
    color += pow(rim, 2.5) * u_rimColor * u_rimStrength;

    gl_FragColor = vec4(color, 1.0);
}

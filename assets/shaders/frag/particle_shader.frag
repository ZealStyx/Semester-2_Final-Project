#ifdef GL_ES
precision mediump float;
#endif

varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec4 v_color;
varying float v_emissiveStrength;
varying float v_ageRatio;

uniform sampler2D u_depthTexture;
uniform vec3 u_lightDirection;
uniform vec3 u_lightColor;
uniform vec3 u_cameraPos;
uniform vec3 u_emissiveColor;
uniform float u_emissiveStrength;
uniform float u_emissiveFalloff;
uniform float u_useNormals;
uniform float u_softParticlesEnabled;
uniform float u_softParticleFadeDistance;
uniform float u_hasDepthTexture;
uniform vec2 u_screenSize;
uniform vec3 u_fogColor;
uniform vec3 u_blindFogColor;
uniform float u_fogStart;
uniform float u_fogEnd;
uniform float u_blindFogStart;
uniform float u_blindFogEnd;
uniform float u_blindFogStrength;
uniform float u_time;
uniform float u_retroEnabled;
uniform float u_diffuseStrength;
uniform float u_ambientStrength;
uniform float u_specularEnabled;
uniform float u_specularStrength;
uniform float u_shininess;
uniform vec3 u_specularColor;
uniform float u_fresnelEnabled;
uniform float u_fresnelStrength;
uniform float u_fresnelPower;
uniform vec3 u_fresnelColor;
uniform float u_edgeFadeStrength;
uniform float u_edgeFadePower;

float hash(vec2 value) {
    return fract(sin(dot(value, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec3 normalDirection = normalize(v_normal);
    vec3 lightDirection = normalize(u_lightDirection);
    vec3 viewDirection = normalize(u_cameraPos - v_worldPos);
    vec3 halfDirection = normalize(lightDirection + viewDirection);
    float normalDotLight = max(dot(normalDirection, lightDirection), 0.0);
    float diffuseFromMesh = mix(1.0, normalDotLight, u_useNormals);
    float diffuse = mix(1.0, u_ambientStrength + ((1.0 - u_ambientStrength) * diffuseFromMesh), u_diffuseStrength);
    vec3 litColor = v_color.rgb * diffuse * u_lightColor;

    vec3 specular = vec3(0.0);
    if (u_specularEnabled > 0.5) {
        float normalDotHalf = max(dot(normalDirection, halfDirection), 0.0);
        specular = u_specularColor * u_specularStrength * pow(normalDotHalf, max(1.0, u_shininess));
    }

    vec3 fresnel = vec3(0.0);
    if (u_fresnelEnabled > 0.5) {
        float normalDotView = max(dot(normalDirection, viewDirection), 0.0);
        float fresnelFactor = pow(1.0 - normalDotView, u_fresnelPower);
        fresnel = u_fresnelColor * u_fresnelStrength * fresnelFactor;
    }

    float distanceToCamera = distance(u_cameraPos, v_worldPos);
    float emissiveAttenuation = 1.0 / (1.0 + (u_emissiveFalloff * distanceToCamera));
    float ageFade = 1.0 - smoothstep(0.65, 1.0, v_ageRatio);
    vec3 emissive = u_emissiveColor * v_emissiveStrength * emissiveAttenuation * ageFade;

    float alpha = v_color.a;
    if (u_softParticlesEnabled > 0.5 && u_hasDepthTexture > 0.5) {
        vec2 screenUv = gl_FragCoord.xy / max(u_screenSize, vec2(1.0, 1.0));
        float sceneDepth = texture2D(u_depthTexture, screenUv).r;
        float depthFade = clamp((sceneDepth - gl_FragCoord.z) / max(0.0001, u_softParticleFadeDistance), 0.0, 1.0);
        alpha *= depthFade;
    }

    if (u_edgeFadeStrength > 0.0) {
        float normalDotView = max(dot(normalDirection, viewDirection), 0.0);
        float edgeFade = pow(normalDotView, u_edgeFadePower);
        alpha *= mix(1.0, edgeFade, u_edgeFadeStrength);
    }

    vec3 combinedColor = litColor + specular + fresnel + emissive;
    float fogRange = max(0.0001, u_fogEnd - u_fogStart);
    float fogFactor = clamp((u_fogEnd - distanceToCamera) / fogRange, 0.0, 1.0);
    vec3 foggedColor = mix(u_fogColor, combinedColor, fogFactor);

    float blindRangeLength = max(0.0001, u_blindFogEnd - u_blindFogStart);
    float blindRange = clamp((u_blindFogEnd - distanceToCamera) / blindRangeLength, 0.0, 1.0);
    float blindFactor = clamp((1.0 - blindRange) * u_blindFogStrength, 0.0, 1.0);
    vec3 outputColor = mix(foggedColor, u_blindFogColor, blindFactor);

    if (u_retroEnabled > 0.5) {
        float scanline = 0.92 + 0.08 * sin((gl_FragCoord.y * 1.5) + (u_time * 20.0));
        float dither = hash(gl_FragCoord.xy + vec2(u_time * 11.0, u_time * 13.0));
        outputColor *= scanline;
        outputColor = floor(outputColor * 8.0 + dither * 0.25) / 8.0;
    }

    if (alpha < 0.01) {
        discard;
    }

    gl_FragColor = vec4(outputColor, alpha);
}
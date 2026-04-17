package io.github.superteam.resonance.particles;

import java.util.Arrays;

public class ParticleMaterial {
    public float diffuseStrength = 0.7f;
    public float ambientStrength = 0.25f;

    public boolean specularEnabled = false;
    public float specularStrength = 0.5f;
    public float shininess = 32f;
    public float[] specularColor = {1f, 1f, 1f};

    public boolean fresnelEnabled = false;
    public float fresnelStrength = 0.8f;
    public float fresnelPower = 2.5f;
    public float[] fresnelColor = {0.4f, 0.8f, 1f};

    public float edgeFadeStrength = 0f;
    public float edgeFadePower = 2f;

    public boolean wireframe = false;
    public float wireframeWidth = 0.02f;

    public float[] diffuseTint = {1f, 1f, 1f};
    public float[] specularTint = {1f, 1f, 1f};

    public void sanitize() {
        diffuseStrength = clamp(diffuseStrength, 0f, 1f);
        ambientStrength = clamp(ambientStrength, 0f, 1f);

        specularStrength = Math.max(0f, specularStrength);
        shininess = Math.max(1f, shininess);
        specularColor = sanitizeArray(specularColor, new float[]{1f, 1f, 1f});

        fresnelStrength = Math.max(0f, fresnelStrength);
        fresnelPower = Math.max(0.1f, fresnelPower);
        fresnelColor = sanitizeArray(fresnelColor, new float[]{0.4f, 0.8f, 1f});

        edgeFadeStrength = clamp(edgeFadeStrength, 0f, 1f);
        edgeFadePower = Math.max(0.1f, edgeFadePower);

        wireframeWidth = Math.max(0.001f, wireframeWidth);

        diffuseTint = sanitizeArray(diffuseTint, new float[]{1f, 1f, 1f});
        specularTint = sanitizeArray(specularTint, new float[]{1f, 1f, 1f});
    }

    private static float[] sanitizeArray(float[] value, float[] fallback) {
        if (value == null || value.length < 3) {
            return Arrays.copyOf(fallback, 3);
        }

        float[] out = new float[3];
        out[0] = clamp(value[0], 0f, 1f);
        out[1] = clamp(value[1], 0f, 1f);
        out[2] = clamp(value[2], 0f, 1f);
        return out;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

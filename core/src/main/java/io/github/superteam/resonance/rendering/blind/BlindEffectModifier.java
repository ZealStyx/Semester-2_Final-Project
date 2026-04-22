package io.github.superteam.resonance.rendering.blind;

/**
 * Time-updated contribution to the blind visibility radius.
 */
public interface BlindEffectModifier {
    void update(float deltaSeconds);

    float getVisibilityDeltaMeters();

    boolean isExpired();

    String getModifierName();

    default String debugSummary() {
        return getModifierName() + "=" + String.format("%.2f", getVisibilityDeltaMeters()) + "m";
    }
}

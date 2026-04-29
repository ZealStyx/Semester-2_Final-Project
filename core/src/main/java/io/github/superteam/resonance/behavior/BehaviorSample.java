package io.github.superteam.resonance.behavior;

/** Normalized sample consumed by the K-means classifier. */
public final class BehaviorSample {
    public final float averageVelocity;
    public final float averageNoiseRms;
    public final float crouchFraction;
    public final float stationaryFraction;
    public final float cameraAngularVariance;
    public final float flashlightToggleRate;
    public final float sanityLossThisWindow;
    public final float lightExposure;

    public BehaviorSample(
        float averageVelocity,
        float averageNoiseRms,
        float crouchFraction,
        float stationaryFraction,
        float cameraAngularVariance,
        float flashlightToggleRate,
        float sanityLossThisWindow,
        float lightExposure
    ) {
        this.averageVelocity = averageVelocity;
        this.averageNoiseRms = averageNoiseRms;
        this.crouchFraction = crouchFraction;
        this.stationaryFraction = stationaryFraction;
        this.cameraAngularVariance = cameraAngularVariance;
        this.flashlightToggleRate = flashlightToggleRate;
        this.sanityLossThisWindow = sanityLossThisWindow;
        this.lightExposure = lightExposure;
    }
}

package io.github.superteam.resonance.sound;

import java.util.Objects;

/**
 * Immutable payload containing propagation results for both acoustic frequency bands.
 */
public final class DualBandResult {
    private final PropagationResult lowBandResult;
    private final PropagationResult highBandResult;

    public DualBandResult(PropagationResult lowBandResult, PropagationResult highBandResult) {
        this.lowBandResult = Objects.requireNonNull(lowBandResult, "Low-band result must not be null.");
        this.highBandResult = Objects.requireNonNull(highBandResult, "High-band result must not be null.");
    }

    public PropagationResult lowBandResult() {
        return lowBandResult;
    }

    public PropagationResult highBandResult() {
        return highBandResult;
    }
}

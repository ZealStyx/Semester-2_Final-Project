package io.github.superteam.resonance.model;

/**
 * Exception thrown when model loading fails at discovery, parsing, or validation time.
 */
public final class ModelLoadException extends RuntimeException {
    public ModelLoadException(String message) {
        super(message);
    }

    public ModelLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

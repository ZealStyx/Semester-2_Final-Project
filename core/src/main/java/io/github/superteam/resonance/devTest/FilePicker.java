package io.github.superteam.resonance.devTest;

import java.util.function.Consumer;

/** Platform-supplied file picker used by development screens. */
@FunctionalInterface
public interface FilePicker {

    /**
     * Opens a file picker and returns an absolute file path, or null when cancelled.
     * Implementations should invoke the callback on the GL thread.
     */
    void open(Consumer<String> onResult);

    FilePicker NOOP = callback -> {
    };
}

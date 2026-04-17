package io.github.superteam.resonance.model;

import com.badlogic.gdx.files.FileHandle;

/**
 * Format-agnostic model loader contract.
 */
public interface ModelLoader {

    /**
     * @return Human-readable loader name for diagnostics.
     */
    String loaderName();

    /**
     * @return True if this loader can process the provided file.
     */
    boolean supports(FileHandle fileHandle);

    /**
     * Load model data from the given source.
     */
    ModelData load(String assetKey, FileHandle fileHandle);
}

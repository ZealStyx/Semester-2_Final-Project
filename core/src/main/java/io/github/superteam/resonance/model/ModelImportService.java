package io.github.superteam.resonance.model;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

/**
 * Loader orchestration service that selects the first capable model loader.
 */
public final class ModelImportService {

    private final Array<ModelLoader> modelLoaders = new Array<>();

    public ModelImportService() {
        registerLoader(new GltfModelLoader());
        registerLoader(new G3dModelLoaderAdapter());
    }

    public void registerLoader(ModelLoader modelLoader) {
        if (modelLoader == null) {
            throw new IllegalArgumentException("modelLoader must not be null");
        }
        modelLoaders.add(modelLoader);
    }

    public ModelData load(String assetKey, FileHandle sourceHandle) {
        if (assetKey == null || assetKey.isBlank()) {
            throw new IllegalArgumentException("assetKey must not be blank");
        }
        if (sourceHandle == null) {
            throw new IllegalArgumentException("sourceHandle must not be null");
        }

        for (ModelLoader modelLoader : modelLoaders) {
            if (!modelLoader.supports(sourceHandle)) {
                continue;
            }
            return modelLoader.load(assetKey, sourceHandle);
        }

        throw new ModelLoadException("No registered loader supports: " + sourceHandle.path());
    }
}

package io.github.superteam.resonance.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Runtime model cache with key-based lookup, ref counting, unload, and reload.
 */
public final class ModelAssetManager {

    private final ModelImportService modelImportService;
    private final ObjectMap<String, ManagedModelEntry> entriesByKey = new ObjectMap<>();

    public ModelAssetManager() {
        this(new ModelImportService());
    }

    public ModelAssetManager(ModelImportService modelImportService) {
        this.modelImportService = modelImportService;
    }

    public synchronized ModelData load(String assetKey, String requestedPath) {
        validateKey(assetKey);
        validatePath(requestedPath);

        ManagedModelEntry existingEntry = entriesByKey.get(assetKey);
        if (existingEntry != null) {
            existingEntry.referenceCount++;
            return existingEntry.modelData;
        }

        FileHandle resolvedSource = resolveSource(requestedPath);
        ModelData modelData = modelImportService.load(assetKey, resolvedSource);
        entriesByKey.put(assetKey, new ManagedModelEntry(assetKey, requestedPath, resolvedSource.path(), modelData));
        return modelData;
    }

    public synchronized ModelData reload(String assetKey) {
        validateKey(assetKey);

        ManagedModelEntry existingEntry = entriesByKey.get(assetKey);
        if (existingEntry == null) {
            throw new ModelLoadException("Cannot reload missing model key: " + assetKey);
        }

        int preservedRefs = existingEntry.referenceCount;
        String requestedPath = existingEntry.requestedPath;

        existingEntry.modelData.dispose();
        entriesByKey.remove(assetKey);

        FileHandle resolvedSource = resolveSource(requestedPath);
        ModelData reloadedData = modelImportService.load(assetKey, resolvedSource);

        ManagedModelEntry reloadedEntry = new ManagedModelEntry(assetKey, requestedPath, resolvedSource.path(), reloadedData);
        reloadedEntry.referenceCount = preservedRefs;
        entriesByKey.put(assetKey, reloadedEntry);
        return reloadedData;
    }

    public synchronized boolean unload(String assetKey) {
        validateKey(assetKey);

        ManagedModelEntry existingEntry = entriesByKey.get(assetKey);
        if (existingEntry == null) {
            return false;
        }

        existingEntry.referenceCount--;
        if (existingEntry.referenceCount > 0) {
            return false;
        }

        existingEntry.modelData.dispose();
        entriesByKey.remove(assetKey);
        return true;
    }

    public synchronized ModelData get(String assetKey) {
        ManagedModelEntry entry = entriesByKey.get(assetKey);
        return entry == null ? null : entry.modelData;
    }

    public synchronized String describeLoadedAssets() {
        StringBuilder description = new StringBuilder();
        for (ObjectMap.Entry<String, ManagedModelEntry> entry : entriesByKey.entries()) {
            description
                .append(entry.key)
                .append(" -> ")
                .append(entry.value.resolvedPath)
                .append(" (refs=")
                .append(entry.value.referenceCount)
                .append(")\n");
        }
        return description.toString();
    }

    public synchronized void disposeAll() {
        for (ManagedModelEntry entry : entriesByKey.values()) {
            entry.modelData.dispose();
        }
        entriesByKey.clear();
    }

    private FileHandle resolveSource(String requestedPath) {
        FileHandle absoluteLocal = Gdx.files.absolute(requestedPath);
        if (absoluteLocal.exists()) {
            return absoluteLocal;
        }

        FileHandle localExact = Gdx.files.local(requestedPath);
        if (localExact.exists()) {
            return localExact;
        }

        FileHandle localModels = Gdx.files.local("models/" + requestedPath);
        if (localModels.exists()) {
            return localModels;
        }

        FileHandle internalExact = Gdx.files.internal(requestedPath);
        if (internalExact.exists()) {
            return internalExact;
        }

        FileHandle internalModels = Gdx.files.internal("models/" + requestedPath);
        if (internalModels.exists()) {
            return internalModels;
        }

        throw new ModelLoadException("Model source not found in local/models/internal paths: " + requestedPath);
    }

    private void validateKey(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) {
            throw new IllegalArgumentException("assetKey must not be blank");
        }
    }

    private void validatePath(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException("requestedPath must not be blank");
        }
    }

    private static final class ManagedModelEntry {
        private final String assetKey;
        private final String requestedPath;
        private final String resolvedPath;
        private final ModelData modelData;
        private int referenceCount = 1;

        private ManagedModelEntry(String assetKey, String requestedPath, String resolvedPath, ModelData modelData) {
            this.assetKey = assetKey;
            this.requestedPath = requestedPath;
            this.resolvedPath = resolvedPath;
            this.modelData = modelData;
        }
    }
}

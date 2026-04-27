package io.github.superteam.resonance.map;

import io.github.superteam.resonance.model.ModelAssetManager;
import io.github.superteam.resonance.model.ModelData;

/**
 * Loads the canonical map model and derives collision data.
 */
public final class MapLoader {
    private static final float DEFAULT_MAP_SCALE = 1f;
    private static final float DEFAULT_MAP_Y_OFFSET = 0f;
    private static final float DEFAULT_MIN_COLLIDER_SIZE = 0.35f;
    private static final float DEFAULT_MIN_COLLIDER_HEIGHT = 0.2f;

    private final ModelAssetManager modelAssetManager;

    public MapLoader(ModelAssetManager modelAssetManager) {
        this.modelAssetManager = modelAssetManager;
    }

    public LoadedMap load(String assetKey, String mapPath, MapDocument mapDocument) {
        if (modelAssetManager == null) {
            throw new IllegalStateException("ModelAssetManager must be available.");
        }
        ModelData modelData = modelAssetManager.load(assetKey, mapPath);
        MapCollisionBuilder.CollisionData collisionData = MapCollisionBuilder.build(
            modelData,
            DEFAULT_MAP_SCALE,
            DEFAULT_MAP_Y_OFFSET,
            DEFAULT_MIN_COLLIDER_SIZE,
            DEFAULT_MIN_COLLIDER_HEIGHT
        );
        MapCollisionBuilder.BvhCollisionData bvhCollisionData = MapCollisionBuilder.buildBvh(
            modelData,
            DEFAULT_MAP_SCALE,
            DEFAULT_MAP_Y_OFFSET
        );
        MapDocument resolvedDoc = mapDocument == null ? MapDocument.defaults() : mapDocument;
        return new LoadedMap(modelData, resolvedDoc, collisionData, bvhCollisionData);
    }

    public static boolean isMapFile(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.endsWith("-map.gltf") || lower.endsWith("-map.glb") || lower.contains("resonance-map");
    }
}

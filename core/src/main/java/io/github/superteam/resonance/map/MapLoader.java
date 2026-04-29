package io.github.superteam.resonance.map;

import io.github.superteam.resonance.model.ModelAssetManager;
import io.github.superteam.resonance.model.ModelData;
import io.github.superteam.resonance.prop.PropDefinitionLoader;
import io.github.superteam.resonance.prop.PropDefinitionRegistry;
import io.github.superteam.resonance.prop.PropInstanceSpawner;
import io.github.superteam.resonance.prop.PropDefinition;
import io.github.superteam.resonance.map.MapObject;
import io.github.superteam.resonance.map.MapObjectType;

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

        // Load prop definitions from the props directory and spawn instances found in the map document.
        PropDefinitionLoader propLoader = new PropDefinitionLoader();
        PropDefinitionRegistry registry = propLoader.loadDirectory("props");
        PropInstanceSpawner spawner = new PropInstanceSpawner(registry);

        if (resolvedDoc.objects != null) {
            for (MapObject obj : resolvedDoc.objects) {
                if (obj == null || obj.type == null) {
                    continue;
                }
                switch (obj.type) {
                    case GLTF_PROP, BOX_PROP, CYLINDER_PROP, FURNITURE, DOOR -> {
                        String defId = obj.property("propDef", null);
                        if (defId == null || defId.isBlank()) {
                            defId = obj.property("propId", null);
                        }
                        if (defId == null || defId.isBlank()) {
                            continue;
                        }
                        float rotY = 0f;
                        try {
                            rotY = Float.parseFloat(obj.property("rotationY", "0"));
                        } catch (NumberFormatException ignore) {
                        }
                        spawner.spawn(defId, obj.position == null ? new com.badlogic.gdx.math.Vector3() : obj.position, rotY);
                    }
                    default -> {
                        // other object types ignored by prop spawner
                    }
                }
            }
        }

        return new LoadedMap(modelData, resolvedDoc, collisionData, bvhCollisionData, spawner);
    }

    public static boolean isMapFile(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.endsWith("-map.gltf") || lower.endsWith("-map.glb") || lower.contains("resonance-map");
    }
}

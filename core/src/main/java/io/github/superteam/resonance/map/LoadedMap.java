package io.github.superteam.resonance.map;

import io.github.superteam.resonance.model.ModelData;

/**
 * Bundles loaded map model and collision outputs.
 */
public final class LoadedMap {
    private final ModelData modelData;
    private final MapDocument mapDocument;
    private final MapCollisionBuilder.CollisionData collisionData;
    private final MapCollisionBuilder.BvhCollisionData bvhCollisionData;

    public LoadedMap(
        ModelData modelData,
        MapDocument mapDocument,
        MapCollisionBuilder.CollisionData collisionData,
        MapCollisionBuilder.BvhCollisionData bvhCollisionData
    ) {
        this.modelData = modelData;
        this.mapDocument = mapDocument;
        this.collisionData = collisionData;
        this.bvhCollisionData = bvhCollisionData;
    }

    public ModelData modelData() {
        return modelData;
    }

    public MapDocument mapDocument() {
        return mapDocument;
    }

    public MapCollisionBuilder.CollisionData collisionData() {
        return collisionData;
    }

    public MapCollisionBuilder.BvhCollisionData bvhCollisionData() {
        return bvhCollisionData;
    }
}

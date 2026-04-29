package io.github.superteam.resonance.map;

import io.github.superteam.resonance.model.ModelData;
import io.github.superteam.resonance.prop.PropInstanceSpawner;

/**
 * Bundles loaded map model and collision outputs.
 */
public final class LoadedMap {
    private final ModelData modelData;
    private final MapDocument mapDocument;
    private final MapCollisionBuilder.CollisionData collisionData;
    private final MapCollisionBuilder.BvhCollisionData bvhCollisionData;
    private final PropInstanceSpawner propSpawner;

    public LoadedMap(
        ModelData modelData,
        MapDocument mapDocument,
        MapCollisionBuilder.CollisionData collisionData,
        MapCollisionBuilder.BvhCollisionData bvhCollisionData,
        PropInstanceSpawner propSpawner
    ) {
        this.modelData = modelData;
        this.mapDocument = mapDocument;
        this.collisionData = collisionData;
        this.bvhCollisionData = bvhCollisionData;
        this.propSpawner = propSpawner;
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

    public PropInstanceSpawner propSpawner() {
        return propSpawner;
    }
}

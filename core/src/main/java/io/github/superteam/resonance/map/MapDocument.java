package io.github.superteam.resonance.map;

import com.badlogic.gdx.utils.Array;

/**
 * Runtime map document for single-map object metadata.
 */
public final class MapDocument {
    public String mapName = "Resonance Map";
    public String gltfMapPath = "models/resonance-map.gltf";
    public Array<MapObject> objects = new Array<>();

    public static MapDocument defaults() {
        return new MapDocument();
    }
}

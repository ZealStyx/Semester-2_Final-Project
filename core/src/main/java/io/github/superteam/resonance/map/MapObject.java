package io.github.superteam.resonance.map;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Serialized map object entry.
 */
public final class MapObject {
    public String id;
    public MapObjectType type;
    public Vector3 position = new Vector3();
    public ObjectMap<String, String> properties = new ObjectMap<>();

    public String property(String key, String fallback) {
        if (properties == null || key == null) {
            return fallback;
        }
        String value = properties.get(key);
        return value == null ? fallback : value;
    }
}

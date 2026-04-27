package io.github.superteam.resonance.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

/**
 * Loads map documents from internal assets.
 */
public final class MapDocumentLoader {
    private final Json json = new Json();

    public MapDocument loadOrDefault(String internalPath) {
        if (Gdx.files == null || internalPath == null || internalPath.isBlank()) {
            return MapDocument.defaults();
        }

        FileHandle file = Gdx.files.internal(internalPath);
        if (!file.exists()) {
            return MapDocument.defaults();
        }

        try {
            MapDocument loaded = json.fromJson(MapDocument.class, file.readString("UTF-8"));
            return loaded == null ? MapDocument.defaults() : loaded;
        } catch (Exception ignored) {
            return MapDocument.defaults();
        }
    }
}

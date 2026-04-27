package io.github.superteam.resonance.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

/**
 * Persists map documents to local storage for editor workflows.
 */
public final class MapDocumentSerializer {
    private final Json json = new Json();

    public void saveLocal(String localPath, MapDocument mapDocument) {
        if (Gdx.files == null || localPath == null || localPath.isBlank() || mapDocument == null) {
            return;
        }
        FileHandle file = Gdx.files.local(localPath);
        file.writeString(json.prettyPrint(mapDocument), false, "UTF-8");
    }

    public MapDocument loadLocalOrDefault(String localPath) {
        if (Gdx.files == null || localPath == null || localPath.isBlank()) {
            return MapDocument.defaults();
        }

        FileHandle file = Gdx.files.local(localPath);
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

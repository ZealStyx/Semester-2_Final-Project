package io.github.superteam.resonance.prop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.List;

public final class PropDefinitionLoader {
    private final Json json = new Json();

    public PropDefinition load(FileHandle fileHandle) {
        if (fileHandle == null || !fileHandle.exists()) {
            return null;
        }
        return json.fromJson(PropDefinition.class, fileHandle.readString());
    }

    public void save(FileHandle fileHandle, PropDefinition definition) {
        if (fileHandle == null || definition == null) {
            return;
        }
        fileHandle.writeString(json.toJson(definition), false);
    }

    public PropDefinitionRegistry loadDirectory(String directoryPath) {
        PropDefinitionRegistry registry = new PropDefinitionRegistry();
        if (Gdx.files == null || directoryPath == null || directoryPath.isBlank()) {
            return registry;
        }

        FileHandle directory = Gdx.files.internal(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return registry;
        }

        for (FileHandle child : directory.list("json")) {
            PropDefinition definition = load(child);
            if (definition != null) {
                registry.register(definition);
            }
        }
        return registry;
    }

    public List<PropDefinition> loadAll(String directoryPath) {
        return loadDirectory(directoryPath).all();
    }
}

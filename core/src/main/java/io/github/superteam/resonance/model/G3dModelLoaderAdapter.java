package io.github.superteam.resonance.model;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.UBJsonReader;

/**
 * Built-in libGDX loader adapter for .g3dj and .g3db assets.
 */
public final class G3dModelLoaderAdapter implements ModelLoader {

    @Override
    public String loaderName() {
        return "g3d-loader";
    }

    @Override
    public boolean supports(FileHandle fileHandle) {
        if (fileHandle == null) {
            return false;
        }
        String extension = fileHandle.extension().toLowerCase();
        return "g3dj".equals(extension) || "g3db".equals(extension);
    }

    @Override
    public ModelData load(String assetKey, FileHandle fileHandle) {
        try {
            String extension = fileHandle.extension().toLowerCase();
            G3dModelLoader g3dLoader = "g3dj".equals(extension)
                ? new G3dModelLoader(new JsonReader())
                : new G3dModelLoader(new UBJsonReader());
            Model model = g3dLoader.loadModel(fileHandle);
            return new ModelData(assetKey, fileHandle.path(), extension, model, null);
        } catch (Exception exception) {
            throw new ModelLoadException("Failed to load g3d model: " + fileHandle.path(), exception);
        }
    }
}

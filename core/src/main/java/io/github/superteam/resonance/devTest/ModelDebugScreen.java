package io.github.superteam.resonance.devTest;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.model.ModelAssetManager;
import io.github.superteam.resonance.model.ModelData;
import io.github.superteam.resonance.model.ModelLoadException;
import io.github.superteam.resonance.model.SceneModel;
import java.util.Locale;

/**
 * Unified model and animation debug harness.
 */
public final class ModelDebugScreen extends ScreenAdapter {

    private static final String DEBUG_ASSET_KEY = "model-debug-primary";

    private final PerspectiveCamera camera;
    private final CameraInputController cameraInputController;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final SpriteBatch spriteBatch;
    private final BitmapFont bitmapFont;

    private final ModelAssetManager modelAssetManager;
    private final Array<String> modelCandidates = new Array<>();

    private SceneModel sceneModel;
    private int selectedModelIndex;
    private int selectedAnimationIndex;
    private String statusMessage = "Ready";
    private String loadedPath = "<none>";
    private final StringBuilder animationInputBuffer = new StringBuilder();

    public ModelDebugScreen() {
        camera = new PerspectiveCamera(67f, Math.max(1, Gdx.graphics.getWidth()), Math.max(1, Gdx.graphics.getHeight()));
        camera.position.set(2.8f, 2.1f, 2.8f);
        camera.lookAt(0f, 1f, 0f);
        camera.near = 0.1f;
        camera.far = 200f;
        camera.update();

        cameraInputController = new CameraInputController(camera);
        cameraInputController.scrollFactor = -0.1f;

        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.68f, 0.68f, 0.68f, 1f));
        environment.add(new DirectionalLight().set(0.9f, 0.9f, 0.85f, -1f, -0.9f, -0.35f));

        spriteBatch = new SpriteBatch();
        bitmapFont = new BitmapFont();
        bitmapFont.setColor(Color.WHITE);

        modelAssetManager = new ModelAssetManager();
        discoverModelCandidates();

        InputAdapter debugInputAdapter = createDebugInputAdapter();
        Gdx.input.setInputProcessor(new InputMultiplexer(debugInputAdapter, cameraInputController));

        if (modelCandidates.notEmpty()) {
            loadSelectedModel();
        } else {
            statusMessage = "No model candidates found in assets/models or local models/.";
        }
    }

    @Override
    public void render(float delta) {
        cameraInputController.update();

        if (sceneModel != null) {
            sceneModel.update(delta);
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.06f, 0.07f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        if (sceneModel != null) {
            modelBatch.begin(camera);
            sceneModel.render(modelBatch, environment);
            modelBatch.end();
        }

        drawOverlay();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        modelAssetManager.disposeAll();
        modelBatch.dispose();
        spriteBatch.dispose();
        bitmapFont.dispose();
    }

    private InputAdapter createDebugInputAdapter() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.ESCAPE:
                        if (Gdx.app.getApplicationListener() instanceof Game game) {
                            game.setScreen(new PlayerTestScreen());
                        }
                        return true;
                    case Input.Keys.M:
                        selectNextModel();
                        return true;
                    case Input.Keys.R:
                        reloadCurrentModel();
                        return true;
                    case Input.Keys.N:
                        selectNextAnimation();
                        return true;
                    case Input.Keys.P:
                        selectPreviousAnimation();
                        return true;
                    case Input.Keys.ENTER:
                        playTypedOrSelectedAnimation();
                        return true;
                    case Input.Keys.C:
                        crossFadeToTypedOrSelectedAnimation();
                        return true;
                    case Input.Keys.SPACE:
                        togglePauseResume();
                        return true;
                    case Input.Keys.BACKSPACE:
                        if (animationInputBuffer.length() > 0) {
                            animationInputBuffer.deleteCharAt(animationInputBuffer.length() - 1);
                        }
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean keyTyped(char character) {
                if (Character.isISOControl(character)) {
                    return false;
                }
                if (animationInputBuffer.length() >= 48) {
                    return true;
                }
                animationInputBuffer.append(character);
                return true;
            }
        };
    }

    private void discoverModelCandidates() {
        collectModelCandidatesFromDirectory(Gdx.files.internal("models"));
        collectModelCandidatesFromDirectory(Gdx.files.local("models"));

        // Stable fallback entries for quick validation if discovery folders are empty.
        if (modelCandidates.isEmpty()) {
            modelCandidates.add("models/sample.gltf");
            modelCandidates.add("models/sample.glb");
            modelCandidates.add("models/sample.g3dj");
        }
    }

    private void collectModelCandidatesFromDirectory(FileHandle directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        FileHandle[] children = directory.list();
        if (children == null) {
            return;
        }

        for (FileHandle child : children) {
            if (child.isDirectory()) {
                collectModelCandidatesFromDirectory(child);
                continue;
            }

            String extension = child.extension().toLowerCase(Locale.ROOT);
            if ("gltf".equals(extension) || "glb".equals(extension) || "g3dj".equals(extension) || "g3db".equals(extension)) {
                addCandidatePath(child.path());
            }
        }
    }

    private void addCandidatePath(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        if (!modelCandidates.contains(path, false)) {
            modelCandidates.add(path);
        }
    }

    private void selectNextModel() {
        if (modelCandidates.isEmpty()) {
            statusMessage = "No model candidates available.";
            return;
        }

        selectedModelIndex = (selectedModelIndex + 1) % modelCandidates.size;
        selectedAnimationIndex = 0;
        loadSelectedModel();
    }

    private void loadSelectedModel() {
        String selectedPath = modelCandidates.get(selectedModelIndex);

        try {
            modelAssetManager.unload(DEBUG_ASSET_KEY);
            ModelData modelData = modelAssetManager.load(DEBUG_ASSET_KEY, selectedPath);

            sceneModel = new SceneModel(modelData);
            sceneModel.setPosition(0f, 0f, 0f);

            loadedPath = modelData.sourcePath();
            statusMessage = "Loaded model: " + selectedPath;

            if (modelData.animationInfos().notEmpty()) {
                selectedAnimationIndex = 0;
                String firstAnimation = modelData.animationInfos().first().name();
                sceneModel.animationController().play(firstAnimation, true);
            }
        } catch (ModelLoadException modelLoadException) {
            sceneModel = null;
            statusMessage = modelLoadException.getMessage();
        }
    }

    private void reloadCurrentModel() {
        if (sceneModel == null) {
            loadSelectedModel();
            return;
        }

        try {
            ModelData reloadedData = modelAssetManager.reload(DEBUG_ASSET_KEY);
            sceneModel = new SceneModel(reloadedData);
            sceneModel.setPosition(0f, 0f, 0f);
            statusMessage = "Reloaded: " + reloadedData.sourcePath();
        } catch (ModelLoadException exception) {
            statusMessage = "Reload failed: " + exception.getMessage();
        }
    }

    private void selectNextAnimation() {
        if (sceneModel == null) {
            return;
        }

        Array<String> animationNames = sceneModel.modelData().getAnimationNames();
        if (animationNames.isEmpty()) {
            statusMessage = "Model has no animations.";
            return;
        }

        selectedAnimationIndex = (selectedAnimationIndex + 1) % animationNames.size;
        String selectedAnimation = animationNames.get(selectedAnimationIndex);
        sceneModel.animationController().play(selectedAnimation, true);
        statusMessage = "Play animation: " + selectedAnimation;
    }

    private void selectPreviousAnimation() {
        if (sceneModel == null) {
            return;
        }

        Array<String> animationNames = sceneModel.modelData().getAnimationNames();
        if (animationNames.isEmpty()) {
            statusMessage = "Model has no animations.";
            return;
        }

        selectedAnimationIndex = (selectedAnimationIndex - 1 + animationNames.size) % animationNames.size;
        String selectedAnimation = animationNames.get(selectedAnimationIndex);
        sceneModel.animationController().play(selectedAnimation, true);
        statusMessage = "Play animation: " + selectedAnimation;
    }

    private void playTypedOrSelectedAnimation() {
        if (sceneModel == null) {
            return;
        }

        String requestedAnimation = typedAnimationOrSelection();
        if (requestedAnimation == null) {
            statusMessage = "No animation selected or typed.";
            return;
        }

        sceneModel.animationController().play(requestedAnimation, true);
        statusMessage = "Play animation: " + requestedAnimation;
        animationInputBuffer.setLength(0);
    }

    private void crossFadeToTypedOrSelectedAnimation() {
        if (sceneModel == null) {
            return;
        }

        String requestedAnimation = typedAnimationOrSelection();
        if (requestedAnimation == null) {
            statusMessage = "No animation selected or typed.";
            return;
        }

        sceneModel.animationController().crossFadeTo(requestedAnimation, 0.35f, true);
        statusMessage = "Cross-fade to: " + requestedAnimation;
        animationInputBuffer.setLength(0);
    }

    private String typedAnimationOrSelection() {
        String typedAnimation = animationInputBuffer.toString().trim();
        if (!typedAnimation.isEmpty()) {
            return typedAnimation;
        }

        Array<String> animationNames = sceneModel.modelData().getAnimationNames();
        if (animationNames.isEmpty()) {
            return null;
        }

        selectedAnimationIndex = Math.max(0, Math.min(selectedAnimationIndex, animationNames.size - 1));
        return animationNames.get(selectedAnimationIndex);
    }

    private void togglePauseResume() {
        if (sceneModel == null) {
            return;
        }

        if (!sceneModel.animationController().isPlaying()) {
            return;
        }

        if (sceneModel.animationController().isPaused()) {
            sceneModel.animationController().resume();
            statusMessage = "Animation resumed.";
        } else {
            sceneModel.animationController().pause();
            statusMessage = "Animation paused.";
        }
    }

    private void drawOverlay() {
        spriteBatch.begin();

        float x = 14f;
        float y = Gdx.graphics.getHeight() - 14f;
        float line = 18f;

        bitmapFont.draw(spriteBatch, "Model Debug Screen", x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Loaded Path: " + loadedPath, x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Status: " + statusMessage, x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Typed Animation: " + animationInputBuffer, x, y);
        y -= line;

        String currentAnimation = "<none>";
        if (sceneModel != null && sceneModel.animationController().getCurrentAnimation() != null) {
            currentAnimation = sceneModel.animationController().getCurrentAnimation();
        }
        bitmapFont.draw(spriteBatch, "Current Animation: " + currentAnimation, x, y);
        y -= line;

        bitmapFont.draw(spriteBatch, "Controls: M next model | R reload | N/P select clip", x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Enter play | C cross-fade | Space pause/resume | ESC back", x, y);
        y -= line;

        if (sceneModel != null) {
            Array<String> animationNames = sceneModel.modelData().getAnimationNames();
            bitmapFont.draw(spriteBatch, "Animations (" + animationNames.size + "):", x, y);
            y -= line;

            int maxLines = Math.min(10, animationNames.size);
            for (int index = 0; index < maxLines; index++) {
                String marker = index == selectedAnimationIndex ? "> " : "  ";
                bitmapFont.draw(spriteBatch, marker + animationNames.get(index), x + 10f, y);
                y -= line;
            }
        }

        spriteBatch.end();
    }
}

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
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.devTest.universal.MultiplayerTestMenuScreen;
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
    private static final float TRANSFORM_STEP = 0.15f;

    private final PerspectiveCamera camera;
    private final CameraInputController cameraInputController;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final SpriteBatch spriteBatch;
    private final BitmapFont bitmapFont;
    private final ShapeRenderer shapeRenderer;
    private final FilePicker filePicker;

    private final ModelAssetManager modelAssetManager;
    private final Array<String> modelCandidates = new Array<>();
    private final BoundingBox tmpBoundingBox = new BoundingBox();
    private final Vector3 tmpDimensions = new Vector3();

    private SceneModel sceneModel;
    private int selectedModelIndex;
    private int selectedAnimationIndex;
    private String statusMessage = "Ready";
    private String loadedPath = "<none>";
    private final StringBuilder animationInputBuffer = new StringBuilder();
    private boolean showBoundingBox;
    private boolean showPlayerReference;
    private boolean filePickerBusy;

    private float modelScale = 1f;
    private float modelYawDegrees;
    private final Vector3 modelPosition = new Vector3(0f, 0f, 0f);

    public ModelDebugScreen() {
        this(FilePicker.NOOP);
    }

    public ModelDebugScreen(FilePicker filePicker) {
        this.filePicker = filePicker == null ? FilePicker.NOOP : filePicker;

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
        shapeRenderer = new ShapeRenderer();

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

        renderDebugGeometry();

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
        shapeRenderer.dispose();
    }

    private InputAdapter createDebugInputAdapter() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.ESCAPE:
                        if (Gdx.app.getApplicationListener() instanceof Game game) {
                            game.setScreen(new MultiplayerTestMenuScreen());
                        }
                        return true;
                    case Input.Keys.M:
                        selectNextModel();
                        return true;
                    case Input.Keys.O:
                        openFilePicker();
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
                    case Input.Keys.B:
                        showBoundingBox = !showBoundingBox;
                        statusMessage = "Bounding box: " + (showBoundingBox ? "ON" : "OFF");
                        return true;
                    case Input.Keys.F:
                        showPlayerReference = !showPlayerReference;
                        statusMessage = "Player reference: " + (showPlayerReference ? "ON" : "OFF");
                        return true;
                    case Input.Keys.T:
                        modelScale = 1f;
                        modelYawDegrees = 0f;
                        modelPosition.setZero();
                        applyTransform();
                        statusMessage = "Transform reset.";
                        return true;
                    case Input.Keys.G:
                        Gdx.app.log("ModelDebug", String.format(
                            "Position: (%.3f, %.3f, %.3f) Scale: %.3f Yaw: %.2f",
                            modelPosition.x,
                            modelPosition.y,
                            modelPosition.z,
                            modelScale,
                            modelYawDegrees
                        ));
                        statusMessage = "Transform printed to log.";
                        return true;
                    case Input.Keys.PLUS:
                    case Input.Keys.EQUALS:
                        modelScale = Math.min(modelScale * 1.1f, 100f);
                        applyTransform();
                        statusMessage = String.format("Scale: %.2f", modelScale);
                        return true;
                    case Input.Keys.MINUS:
                        modelScale = Math.max(modelScale * 0.9f, 0.001f);
                        applyTransform();
                        statusMessage = String.format("Scale: %.2f", modelScale);
                        return true;
                    case Input.Keys.LEFT:
                        modelPosition.x -= TRANSFORM_STEP;
                        applyTransform();
                        statusMessage = String.format("Position: (%.2f, %.2f, %.2f)", modelPosition.x, modelPosition.y, modelPosition.z);
                        return true;
                    case Input.Keys.RIGHT:
                        modelPosition.x += TRANSFORM_STEP;
                        applyTransform();
                        statusMessage = String.format("Position: (%.2f, %.2f, %.2f)", modelPosition.x, modelPosition.y, modelPosition.z);
                        return true;
                    case Input.Keys.UP:
                        modelPosition.y += TRANSFORM_STEP;
                        applyTransform();
                        statusMessage = String.format("Position: (%.2f, %.2f, %.2f)", modelPosition.x, modelPosition.y, modelPosition.z);
                        return true;
                    case Input.Keys.DOWN:
                        modelPosition.y -= TRANSFORM_STEP;
                        applyTransform();
                        statusMessage = String.format("Position: (%.2f, %.2f, %.2f)", modelPosition.x, modelPosition.y, modelPosition.z);
                        return true;
                    case Input.Keys.Q:
                        modelYawDegrees -= 7.5f;
                        applyTransform();
                        statusMessage = String.format("Yaw: %.1f", modelYawDegrees);
                        return true;
                    case Input.Keys.E:
                        modelYawDegrees += 7.5f;
                        applyTransform();
                        statusMessage = String.format("Yaw: %.1f", modelYawDegrees);
                        return true;
                    case Input.Keys.BACKSPACE:
                        if (animationInputBuffer.length() > 0) {
                            animationInputBuffer.deleteCharAt(animationInputBuffer.length() - 1);
                        }
                        return true;
                    default:
                        return trySelectIndexShortcut(keycode);
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

    private boolean trySelectIndexShortcut(int keycode) {
        int index = -1;
        if (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
            index = keycode - Input.Keys.NUM_0;
        } else if (keycode >= Input.Keys.NUMPAD_0 && keycode <= Input.Keys.NUMPAD_9) {
            index = keycode - Input.Keys.NUMPAD_0;
        }

        if (index < 0) {
            return false;
        }
        if (index >= modelCandidates.size) {
            statusMessage = "No model at index " + index;
            return true;
        }

        selectedModelIndex = index;
        selectedAnimationIndex = 0;
        loadSelectedModel();
        return true;
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
        if (modelCandidates.isEmpty()) {
            statusMessage = "No model candidates available.";
            return;
        }

        String selectedPath = modelCandidates.get(selectedModelIndex);

        try {
            modelAssetManager.unload(DEBUG_ASSET_KEY);
            ModelData modelData = modelAssetManager.load(DEBUG_ASSET_KEY, selectedPath);

            sceneModel = new SceneModel(modelData);
            applyTransform();

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
            applyTransform();
            statusMessage = "Reloaded: " + reloadedData.sourcePath();
        } catch (ModelLoadException exception) {
            statusMessage = "Reload failed: " + exception.getMessage();
        }
    }

    private void openFilePicker() {
        if (filePickerBusy) {
            statusMessage = "File picker already open...";
            return;
        }

        filePickerBusy = true;
        statusMessage = "Opening file picker...";

        filePicker.open(absolutePath -> {
            filePickerBusy = false;

            if (absolutePath == null || absolutePath.isBlank()) {
                statusMessage = "File picker cancelled.";
                return;
            }

            addCandidatePath(absolutePath);
            selectedModelIndex = modelCandidates.size - 1;
            selectedAnimationIndex = 0;
            loadSelectedModel();
        });
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

        float topY = Gdx.graphics.getHeight() - 14f;
        float line = 18f;
        float leftX = 14f;
        float rightX = Math.max(14f, Gdx.graphics.getWidth() - 340f);

        drawLeftPanel(leftX, topY, line);
        drawRightPanel(rightX, topY, line);

        spriteBatch.end();
    }

    private void drawLeftPanel(float x, float y, float line) {
        bitmapFont.setColor(Color.WHITE);
        bitmapFont.draw(spriteBatch, "Model Debug Screen", x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Loaded Path: " + loadedPath, x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Status: " + statusMessage, x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, buildModelInfoLine(), x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Typed Animation: " + animationInputBuffer, x, y);
        y -= line;

        String currentAnimation = "<none>";
        if (sceneModel != null && sceneModel.animationController().getCurrentAnimation() != null) {
            currentAnimation = sceneModel.animationController().getCurrentAnimation();
        }
        bitmapFont.draw(spriteBatch, "Current Animation: " + currentAnimation, x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, String.format("Transform pos=(%.2f, %.2f, %.2f) scale=%.2f yaw=%.1f",
            modelPosition.x, modelPosition.y, modelPosition.z, modelScale, modelYawDegrees), x, y);
        y -= line;

        bitmapFont.setColor(Color.LIGHT_GRAY);
        bitmapFont.draw(spriteBatch, "Controls: O open | M next | R reload | N/P animations", x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "Enter play | C fade | Space pause | B bbox | F player ref", x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "+/- scale | Arrows move | Q/E rotate | T reset | G log", x, y);
        y -= line;

        if (sceneModel != null) {
            Array<String> animationNames = sceneModel.modelData().getAnimationNames();
            bitmapFont.setColor(Color.WHITE);
            bitmapFont.draw(spriteBatch, "Animations (" + animationNames.size + "):", x, y);
            y -= line;

            int maxLines = Math.min(12, animationNames.size);
            for (int index = 0; index < maxLines; index++) {
                boolean active = index == selectedAnimationIndex;
                bitmapFont.setColor(active ? Color.CYAN : Color.LIGHT_GRAY);
                String marker = active ? "> " : "  ";
                bitmapFont.draw(spriteBatch, marker + "[" + index + "] " + animationNames.get(index), x + 8f, y);
                y -= line;
            }
        }
    }

    private void drawRightPanel(float x, float y, float line) {
        bitmapFont.setColor(Color.LIGHT_GRAY);
        bitmapFont.draw(spriteBatch, "-- Model List --", x, y);
        y -= line;

        int displayMax = Math.min(modelCandidates.size, 20);
        for (int i = 0; i < displayMax; i++) {
            String path = modelCandidates.get(i);
            String name = fileNameOnly(path);
            boolean active = i == selectedModelIndex;
            bitmapFont.setColor(active ? Color.CYAN : Color.LIGHT_GRAY);
            String marker = active ? "> " : "  ";
            bitmapFont.draw(spriteBatch, marker + "[" + i + "] " + name, x, y);
            y -= line;
        }

        if (modelCandidates.size > displayMax) {
            bitmapFont.setColor(Color.GRAY);
            bitmapFont.draw(spriteBatch, "  ...+" + (modelCandidates.size - displayMax) + " more", x, y);
            y -= line;
        }

        y -= line;
        bitmapFont.setColor(Color.YELLOW);
        bitmapFont.draw(spriteBatch, "O = open file", x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "0-9 = jump index", x, y);
        y -= line;
        bitmapFont.draw(spriteBatch, "M = next | ESC = back", x, y);
    }

    private void renderDebugGeometry() {
        if (sceneModel == null && !showPlayerReference) {
            return;
        }

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        if (sceneModel != null && showBoundingBox) {
            sceneModel.modelInstance().calculateBoundingBox(tmpBoundingBox);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.box(
                tmpBoundingBox.min.x,
                tmpBoundingBox.min.y,
                tmpBoundingBox.min.z,
                tmpBoundingBox.getWidth(),
                tmpBoundingBox.getHeight(),
                tmpBoundingBox.getDepth()
            );
        }

        if (showPlayerReference) {
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.box(-0.3f, 0f, -0.15f, 0.6f, 1.8f, 0.3f);
        }

        shapeRenderer.end();
    }

    private String buildModelInfoLine() {
        if (sceneModel == null) {
            return "Verts: 0  Tris: 0  Nodes: 0  Mat: 0  Size: 0.0x0.0x0.0";
        }

        Model model = sceneModel.modelData().model();
        int triangles = 0;
        for (MeshPart meshPart : model.meshParts) {
            triangles += meshPart.size / 3;
        }

        int vertices = 0;
        for (com.badlogic.gdx.graphics.Mesh mesh : model.meshes) {
            vertices += mesh.getNumVertices();
        }

        sceneModel.modelInstance().calculateBoundingBox(tmpBoundingBox);
        tmpBoundingBox.getDimensions(tmpDimensions);

        return String.format(
            "Verts: %d  Tris: %d  Nodes: %d  Mat: %d  Size: %.1fx%.1fx%.1f",
            vertices,
            triangles,
            model.nodes.size,
            model.materials.size,
            tmpDimensions.x,
            tmpDimensions.y,
            tmpDimensions.z
        );
    }

    private void applyTransform() {
        if (sceneModel == null) {
            return;
        }

        sceneModel.modelInstance().transform.idt();
        sceneModel.modelInstance().transform.translate(modelPosition);
        sceneModel.modelInstance().transform.rotate(Vector3.Y, modelYawDegrees);
        sceneModel.modelInstance().transform.scale(modelScale, modelScale, modelScale);
    }

    private String fileNameOnly(String path) {
        if (path == null || path.isBlank()) {
            return "<unknown>";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (slash < 0 || slash + 1 >= path.length()) {
            return path;
        }
        return path.substring(slash + 1);
    }
}

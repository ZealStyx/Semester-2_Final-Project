package io.github.superteam.resonance.devTest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.physics.bullet.collision.btPersistentManifold;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import java.util.HashSet;
import java.util.Set;

import io.github.superteam.resonance.items.CarriableItem;
import io.github.superteam.resonance.items.ItemDefinition;
import io.github.superteam.resonance.items.ItemType;
import io.github.superteam.resonance.player.CarrySystem;
import io.github.superteam.resonance.player.ImpactListener;
import io.github.superteam.resonance.player.InventorySystem;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.player.PlayerController;
import io.github.superteam.resonance.player.PlayerFeatureExtractor;
import io.github.superteam.resonance.player.PlayerFootstepSoundEmitter;
import io.github.superteam.resonance.player.PlayerInteractionSystem;
import io.github.superteam.resonance.player.TestCrate;
import io.github.superteam.resonance.sound.AcousticGraphEngine;
import io.github.superteam.resonance.sound.DijkstraPathfinder;
import io.github.superteam.resonance.sound.PropagationResult;
import io.github.superteam.resonance.sound.SonarRenderer;
import io.github.superteam.resonance.sound.SoundBalancingConfigStore;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;
import io.github.superteam.resonance.sound.PhysicsNoiseEmitter;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import io.github.superteam.resonance.sound.SpatialCueController;

/**
 * Player system test and demo screen.
 *
 * Responsibilities:
 * - Scene construction and collision setup
 * - Player controller + interaction + sound orchestration integration
 * - Render pipeline (world -> VHS post-process -> HUD)
 */
public class PlayerTestScreen extends ScreenAdapter {

    private final PerspectiveCamera camera;
    private final PlayerController playerController;
    private final PlayerInteractionSystem playerInteractionSystem;
    private final CarrySystem carrySystem;
    private final InventorySystem inventorySystem;
    private final TestCrate testCrate;

    private final Vector3 cratePosition = new Vector3();
    private final Vector3 tmpForward = new Vector3();
    private final Vector3 tmpRight = new Vector3();
    private final Vector3 tmpUp = new Vector3();
    private final Vector3 tmpViewModelOffset = new Vector3();
    private final Vector3 playerWorldPosition = new Vector3();
    private final Vector3 tmpToCrate = new Vector3();
    private final Vector3 tmpProjected = new Vector3();
    private final Vector3 tmpCarriableDirection = new Vector3();
    private final Vector3 tmpCarriableOffset = new Vector3();
    private final Vector3 tmpDropPosition = new Vector3();
    private final Vector3 tmpCarryPoint = new Vector3();
    private final Vector3 tmpImpulsePoint = new Vector3();
    private final Vector3 tmpInertia = new Vector3();

    private final Matrix4 playerViewModelTransform = new Matrix4();
    private final Matrix4 hudProjection = new Matrix4();
    private final Matrix4 tmpBodyTransform = new Matrix4();

    private final Array<Mesh> sceneMeshes;
    private final Array<Matrix4> sceneTransforms;
    private final Array<BoundingBox> worldColliders;

    private final AcousticGraphEngine acousticGraphEngine;
    private final SoundPropagationOrchestrator soundPropagationOrchestrator;
    private final SpatialCueController spatialCueController;
    private final PlayerFootstepSoundEmitter playerFootstepSoundEmitter;
    private final PlayerFeatureExtractor playerFeatureExtractor;
    private final PhysicsNoiseEmitter physicsNoiseEmitter;
    private final ImpactListener impactListener;

    private final ObjectMap<String, Vector3> graphNodePositions = new ObjectMap<>();
    private final Array<String> lastRevealedNodeIds = new Array<>();
    private final Array<CarriableItem> worldCarriableItems = new Array<>();
    private final ObjectMap<CarriableItem, Mesh> carriableMeshes = new ObjectMap<>();
    private final ObjectMap<CarriableItem, Matrix4> carriableTransforms = new ObjectMap<>();
    private final ObjectMap<CarriableItem, btRigidBody> carriableRigidBodies = new ObjectMap<>();
    private final ObjectMap<CarriableItem, btCollisionShape> carriableCollisionShapes = new ObjectMap<>();
    private final ObjectMap<CarriableItem, btDefaultMotionState> carriableMotionStates = new ObjectMap<>();
    private final ObjectMap<btRigidBody, CarriableItem> rigidBodyToCarriable = new ObjectMap<>();

    private final Array<btRigidBody> staticRigidBodies = new Array<>();
    private final Array<btCollisionShape> staticCollisionShapes = new Array<>();
    private final Array<btDefaultMotionState> staticMotionStates = new Array<>();

    private btCollisionConfiguration physicsCollisionConfiguration;
    private btCollisionDispatcher physicsCollisionDispatcher;
    private btBroadphaseInterface physicsBroadphase;
    private btConstraintSolver physicsConstraintSolver;
    private btDiscreteDynamicsWorld physicsWorld;

    private Mesh playerViewModelMesh;
    private Mesh fullscreenQuadMesh;
    private ShaderProgram worldShader;
    private ShaderProgram playerShaderProgram;
    private ShaderProgram vhsShader;
    private FrameBuffer sceneFbo;
    private ShapeRenderer shapeRenderer;

    private float elapsedSeconds;
    private float currentFov = BASE_FOV;
    private float vhsStrength = 0.75f;
    private float revealedFlashRemaining;
    private float inventoryFullMessageRemaining;
    private int pendingScrollSteps;
    private boolean showGraphDebug;

    private int fboWidth = 1;
    private int fboHeight = 1;

    // Scene constants
    private static final float FLOOR_WIDTH = 24.0f;
    private static final float FLOOR_LENGTH = 24.0f;
    private static final float WALL_HEIGHT = 3.0f;
    private static final float EYE_HEIGHT = 1.6f;
    private static final float CRATE_SIZE = 0.5f;
    private static final float INTERACTION_RANGE = 2.5f;
    private static final float INTERACTION_FACING_DOT = 0.70f;
    private static final float PICKUP_RANGE = 3.0f;
    private static final float CARRY_SCROLL_STEP = 0.15f;
    private static final float INVENTORY_FULL_MESSAGE_DURATION = 2.0f;

    // View model constants
    private static final float VIEW_MODEL_SCALE = 0.18f;
    private static final float VIEW_MODEL_OFFSET_RIGHT = 0.28f;
    private static final float VIEW_MODEL_OFFSET_UP = -0.24f;
    private static final float VIEW_MODEL_OFFSET_FORWARD = 0.45f;

    // FOV sprint stretch
    private static final float BASE_FOV = 75.0f;
    private static final float RUN_FOV = 85.0f;
    private static final float FOV_LERP_SPEED = 6.5f;

    // VHS controls
    private static final float VHS_STRENGTH_STEP = 0.05f;
    private static final float VHS_STRENGTH_MIN = 0.0f;
    private static final float VHS_STRENGTH_MAX = 1.0f;

    // HUD / debug overlay
    private static final float CROSSHAIR_HALF_SIZE = 7.0f;
    private static final float CROSSHAIR_GAP = 3.0f;
    private static final float NODE_DOT_RADIUS = 4.0f;
    private static final float NODE_FLASH_RADIUS = 7.5f;
    private static final float NODE_FLASH_DURATION = 0.55f;

    private static final Color CROSSHAIR_DEFAULT_COLOR = new Color(0.95f, 0.95f, 0.95f, 0.95f);
    private static final Color CROSSHAIR_INTERACT_COLOR = new Color(1.0f, 0.93f, 0.2f, 1.0f);
    private static final Color NODE_BASE_COLOR = new Color(0.2f, 0.95f, 0.6f, 0.9f);
    private static final Color NODE_FLASH_COLOR = new Color(1.0f, 0.92f, 0.2f, 1.0f);
    private static final Color INVENTORY_SLOT_ACTIVE = new Color(0.95f, 0.72f, 0.22f, 0.95f);
    private static final Color INVENTORY_SLOT_INACTIVE = new Color(0.28f, 0.3f, 0.34f, 0.9f);
    private static final Color INVENTORY_SLOT_FILLED = new Color(0.24f, 0.74f, 0.66f, 0.9f);
    private static final Color INVENTORY_WARNING = new Color(0.95f, 0.2f, 0.2f, 0.85f);

    public PlayerTestScreen() {
        camera = new PerspectiveCamera(BASE_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(FLOOR_WIDTH * 0.5f, EYE_HEIGHT, FLOOR_LENGTH * 0.5f);
        camera.lookAt((FLOOR_WIDTH * 0.5f) + 1.0f, EYE_HEIGHT, (FLOOR_LENGTH * 0.5f) - 1.0f);
        camera.near = 0.1f;
        camera.far = 100.0f;
        camera.update();

        sceneMeshes = new Array<>();
        sceneTransforms = new Array<>();
        worldColliders = new Array<>();

        loadWorldShader();
        loadPlayerShader();
        loadVhsShader();

        fullscreenQuadMesh = createFullscreenQuadMesh();
        shapeRenderer = new ShapeRenderer();

        buildProceduralScene();
        initializePhysicsWorld();
        registerStaticWorldBodies();
        playerViewModelMesh = createCubeMesh(1.0f, 1.0f, 1.0f);

        playerController = new PlayerController(camera);
        playerController.setWorldColliders(worldColliders);
        carrySystem = new CarrySystem(camera);
        inventorySystem = new InventorySystem();

        testCrate = new TestCrate();
        playerInteractionSystem = new PlayerInteractionSystem(camera, playerController);
        playerInteractionSystem.setPrimaryTarget(testCrate, cratePosition);
        playerInteractionSystem.setInteractionRange(INTERACTION_RANGE);
        playerInteractionSystem.setInteractionKeyCode(Input.Keys.R);

        acousticGraphEngine = new AcousticGraphEngine().buildTestGraph();
        cacheGraphNodePositions();

        spatialCueController = new SpatialCueController();
        soundPropagationOrchestrator = new SoundPropagationOrchestrator(
            acousticGraphEngine,
            new DijkstraPathfinder(),
            new SonarRenderer(),
            spatialCueController,
            SoundBalancingConfigStore.loadOrDefault("config/balancing_config.json")
        );
        physicsNoiseEmitter = new PhysicsNoiseEmitter(soundPropagationOrchestrator);
        impactListener = new ImpactListener(physicsNoiseEmitter, this::findNearestNodeId);

        spawnInitialCarriableItems();

        playerFootstepSoundEmitter = new PlayerFootstepSoundEmitter(
            playerController,
            soundPropagationOrchestrator,
            this::findNearestNodeId
        );

        playerFeatureExtractor = new PlayerFeatureExtractor();
        playerFeatureExtractor.setLastKnownPosition(camera.position);
        playerFeatureExtractor.addListener(features ->
            Gdx.app.log(
                "PlayerFeatures",
                String.format(
                    "avgSpeed=%.2f rotationRate=%.2f stationaryRatio=%.2f collisionRate=%.2f backtrackRatio=%.2f",
                    features.avgSpeed,
                    features.rotationRate,
                    features.stationaryRatio,
                    features.collisionRate,
                    features.backtrackRatio
                )
            )
        );

        testCrate.setInteractionListener(player -> emitCrateInteractionSound());

        recreateSceneFbo(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        updateHudProjection(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                pendingScrollSteps += Math.round(amountY);
                return true;
            }
        });

        Gdx.input.setCursorCatched(true);
    }

    private void loadWorldShader() {
        worldShader = new ShaderProgram(
            Gdx.files.internal("shaders/vert/retro_shader.vert"),
            Gdx.files.internal("shaders/frag/retro_shader.frag")
        );
        if (!worldShader.isCompiled()) {
            throw new GdxRuntimeException("World shader failed to compile: " + worldShader.getLog());
        }
    }

    private void loadPlayerShader() {
        try {
            playerShaderProgram = new ShaderProgram(
                Gdx.files.internal("shaders/vert/player_shader.vert"),
                Gdx.files.internal("shaders/frag/player_shader.frag")
            );
        } catch (Exception ignored) {
            String vertexShader =
                "attribute vec3 a_position;\n" +
                "attribute vec3 a_normal;\n" +
                "uniform mat4 u_projViewTrans;\n" +
                "uniform mat4 u_modelTrans;\n" +
                "varying vec3 v_worldPos;\n" +
                "varying vec3 v_normal;\n" +
                "void main() {\n" +
                "  vec4 worldPos = u_modelTrans * vec4(a_position, 1.0);\n" +
                "  v_worldPos = worldPos.xyz;\n" +
                "  v_normal = normalize((u_modelTrans * vec4(a_normal, 0.0)).xyz);\n" +
                "  gl_Position = u_projViewTrans * worldPos;\n" +
                "}\n";

            String fragmentShader =
                "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "varying vec3 v_worldPos;\n" +
                "varying vec3 v_normal;\n" +
                "uniform vec3 u_cameraPos;\n" +
                "uniform vec3 u_lightDirection;\n" +
                "uniform vec3 u_baseColor;\n" +
                "uniform vec3 u_ambientColor;\n" +
                "uniform vec3 u_rimColor;\n" +
                "uniform float u_rimStrength;\n" +
                "void main() {\n" +
                "  vec3 n = normalize(v_normal);\n" +
                "  vec3 l = normalize(u_lightDirection);\n" +
                "  vec3 v = normalize(u_cameraPos - v_worldPos);\n" +
                "  float diffuse = max(dot(n, l), 0.0);\n" +
                "  vec3 color = u_baseColor * (u_ambientColor + diffuse);\n" +
                "  float rim = 1.0 - max(dot(n, v), 0.0);\n" +
                "  color += pow(rim, 2.5) * u_rimColor * u_rimStrength;\n" +
                "  gl_FragColor = vec4(color, 1.0);\n" +
                "}\n";

            playerShaderProgram = new ShaderProgram(vertexShader, fragmentShader);
        }

        if (!playerShaderProgram.isCompiled()) {
            throw new GdxRuntimeException("Player shader failed to compile: " + playerShaderProgram.getLog());
        }
    }

    private void loadVhsShader() {
        vhsShader = new ShaderProgram(
            Gdx.files.internal("shaders/vert/vhs_postprocess.vert"),
            Gdx.files.internal("shaders/frag/vhs_postprocess.frag")
        );
        if (!vhsShader.isCompiled()) {
            throw new GdxRuntimeException("VHS shader failed to compile: " + vhsShader.getLog());
        }
    }

    private Mesh createFullscreenQuadMesh() {
        Mesh mesh = new Mesh(
            true,
            4,
            6,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
        );

        float[] vertices = {
            -1f, -1f, 0f, 0f, 0f,
             1f, -1f, 0f, 1f, 0f,
             1f,  1f, 0f, 1f, 1f,
            -1f,  1f, 0f, 0f, 1f
        };

        short[] indices = {
            0, 1, 2,
            2, 3, 0
        };

        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    private void recreateSceneFbo(int width, int height) {
        fboWidth = Math.max(1, width);
        fboHeight = Math.max(1, height);

        if (sceneFbo != null) {
            sceneFbo.dispose();
        }

        sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, true);
        sceneFbo.getColorBufferTexture().setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        );
    }

    private void initializePhysicsWorld() {
        Bullet.init();

        physicsCollisionConfiguration = new btDefaultCollisionConfiguration();
        physicsCollisionDispatcher = new btCollisionDispatcher(physicsCollisionConfiguration);
        physicsBroadphase = new btDbvtBroadphase();
        physicsConstraintSolver = new btSequentialImpulseConstraintSolver();
        physicsWorld = new btDiscreteDynamicsWorld(
            physicsCollisionDispatcher,
            physicsBroadphase,
            physicsConstraintSolver,
            physicsCollisionConfiguration
        );
        physicsWorld.setGravity(new Vector3(0f, -9.8f, 0f));
    }

    private void registerStaticWorldBodies() {
        for (BoundingBox collider : worldColliders) {
            float width = collider.max.x - collider.min.x;
            float height = collider.max.y - collider.min.y;
            float depth = collider.max.z - collider.min.z;

            float centerX = (collider.min.x + collider.max.x) * 0.5f;
            float centerY = (collider.min.y + collider.max.y) * 0.5f;
            float centerZ = (collider.min.z + collider.max.z) * 0.5f;

            btBoxShape shape = new btBoxShape(new Vector3(width * 0.5f, height * 0.5f, depth * 0.5f));
            btDefaultMotionState motionState = new btDefaultMotionState(new Matrix4().setToTranslation(centerX, centerY, centerZ));
            btRigidBodyConstructionInfo constructionInfo = new btRigidBodyConstructionInfo(0f, motionState, shape, Vector3.Zero);
            btRigidBody rigidBody = new btRigidBody(constructionInfo);
            constructionInfo.dispose();

            physicsWorld.addRigidBody(rigidBody);
            staticCollisionShapes.add(shape);
            staticMotionStates.add(motionState);
            staticRigidBodies.add(rigidBody);
        }
    }

    private void updatePhysicsWorld(float delta) {
        if (physicsWorld == null) {
            return;
        }

        physicsWorld.stepSimulation(delta, 5, 1f / 60f);
        processBulletImpacts();
        processDeferredBreaks();
    }

    private void processBulletImpacts() {
        int manifoldCount = physicsCollisionDispatcher.getNumManifolds();
        for (int manifoldIndex = 0; manifoldIndex < manifoldCount; manifoldIndex++) {
            btPersistentManifold manifold = physicsCollisionDispatcher.getManifoldByIndexInternal(manifoldIndex);
            btCollisionObject body0 = manifold.getBody0();
            btCollisionObject body1 = manifold.getBody1();

            CarriableItem carriableItem = resolveCarriableItem(body0, body1);
            if (carriableItem == null) {
                continue;
            }

            int contactCount = manifold.getNumContacts();
            for (int contactIndex = 0; contactIndex < contactCount; contactIndex++) {
                btManifoldPoint contactPoint = manifold.getContactPoint(contactIndex);
                float appliedImpulse = contactPoint.getAppliedImpulse();
                if (appliedImpulse <= 0f) {
                    continue;
                }

                contactPoint.getPositionWorldOnA(tmpImpulsePoint);
                PropagationResult propagationResult = impactListener.onCarriableImpact(
                    carriableItem,
                    tmpImpulsePoint,
                    appliedImpulse,
                    elapsedSeconds
                );
                if (propagationResult != null) {
                    cacheRevealedNodesForFlash(propagationResult);
                }
            }
        }
    }

    private CarriableItem resolveCarriableItem(btCollisionObject body0, btCollisionObject body1) {
        if (body0 instanceof btRigidBody) {
            btRigidBody rigidBody0 = (btRigidBody) body0;
            CarriableItem carriable0 = rigidBodyToCarriable.get(rigidBody0);
            if (carriable0 != null) {
                return carriable0;
            }
        }

        if (body1 instanceof btRigidBody) {
            btRigidBody rigidBody1 = (btRigidBody) body1;
            return rigidBodyToCarriable.get(rigidBody1);
        }

        return null;
    }

    private void processDeferredBreaks() {
        Array<CarriableItem> breakItems = impactListener.consumePendingBreakItems();
        if (breakItems.isEmpty()) {
            return;
        }

        Set<CarriableItem> uniqueItems = new HashSet<>();
        for (CarriableItem breakItem : breakItems) {
            if (breakItem == null || breakItem.isBroken() || !uniqueItems.add(breakItem)) {
                continue;
            }

            if (carrySystem.heldItem() == breakItem) {
                carrySystem.dropHeldItem();
            }

            emitItemEvent(SoundEvent.PHYSICS_COLLAPSE, breakItem.worldPosition(), 1.0f);
            removeWorldCarriableItem(breakItem);
        }
    }

    private void cacheGraphNodePositions() {
        graphNodePositions.clear();
        for (io.github.superteam.resonance.sound.GraphNode graphNode : acousticGraphEngine.getNodes()) {
            graphNodePositions.put(graphNode.id(), graphNode.position());
        }
    }

    private void buildProceduralScene() {
        float centerX = FLOOR_WIDTH * 0.5f;
        float centerZ = FLOOR_LENGTH * 0.5f;

        Mesh floor = createCubeMesh(FLOOR_WIDTH, 0.1f, FLOOR_LENGTH);
        sceneMeshes.add(floor);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX, -0.05f, centerZ));

        Mesh wallNorth = createCubeMesh(FLOOR_WIDTH, WALL_HEIGHT, 0.2f);
        sceneMeshes.add(wallNorth);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX, WALL_HEIGHT * 0.5f, FLOOR_LENGTH + 0.1f));
        addCollider(centerX, WALL_HEIGHT * 0.5f, FLOOR_LENGTH + 0.1f, FLOOR_WIDTH, WALL_HEIGHT, 0.2f);

        Mesh wallSouth = createCubeMesh(FLOOR_WIDTH, WALL_HEIGHT, 0.2f);
        sceneMeshes.add(wallSouth);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX, WALL_HEIGHT * 0.5f, -0.1f));
        addCollider(centerX, WALL_HEIGHT * 0.5f, -0.1f, FLOOR_WIDTH, WALL_HEIGHT, 0.2f);

        Mesh wallEast = createCubeMesh(0.2f, WALL_HEIGHT, FLOOR_LENGTH);
        sceneMeshes.add(wallEast);
        sceneTransforms.add(new Matrix4().setToTranslation(FLOOR_WIDTH + 0.1f, WALL_HEIGHT * 0.5f, centerZ));
        addCollider(FLOOR_WIDTH + 0.1f, WALL_HEIGHT * 0.5f, centerZ, 0.2f, WALL_HEIGHT, FLOOR_LENGTH);

        Mesh wallWest = createCubeMesh(0.2f, WALL_HEIGHT, FLOOR_LENGTH);
        sceneMeshes.add(wallWest);
        sceneTransforms.add(new Matrix4().setToTranslation(-0.1f, WALL_HEIGHT * 0.5f, centerZ));
        addCollider(-0.1f, WALL_HEIGHT * 0.5f, centerZ, 0.2f, WALL_HEIGHT, FLOOR_LENGTH);

        Mesh dividerOne = createCubeMesh(8.0f, WALL_HEIGHT, 0.2f);
        sceneMeshes.add(dividerOne);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX - 4.0f, WALL_HEIGHT * 0.5f, centerZ + 3.5f));
        addCollider(centerX - 4.0f, WALL_HEIGHT * 0.5f, centerZ + 3.5f, 8.0f, WALL_HEIGHT, 0.2f);

        Mesh dividerTwo = createCubeMesh(0.2f, WALL_HEIGHT, 7.0f);
        sceneMeshes.add(dividerTwo);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX + 3.5f, WALL_HEIGHT * 0.5f, centerZ - 4.0f));
        addCollider(centerX + 3.5f, WALL_HEIGHT * 0.5f, centerZ - 4.0f, 0.2f, WALL_HEIGHT, 7.0f);

        Mesh alcoveFloor = createCubeMesh(2.0f, 0.1f, 1.0f);
        sceneMeshes.add(alcoveFloor);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX - 9.0f, 0.0f, centerZ + 9.0f));

        Mesh alcoveCeiling = createCubeMesh(2.0f, 0.1f, 1.0f);
        sceneMeshes.add(alcoveCeiling);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX - 9.0f, 1.2f, centerZ + 9.0f));
        addCollider(centerX - 9.0f, 1.2f, centerZ + 9.0f, 2.0f, 0.1f, 1.0f);

        Mesh alcoveWallLeft = createCubeMesh(0.1f, 1.1f, 1.0f);
        sceneMeshes.add(alcoveWallLeft);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX - 10.0f, 0.55f, centerZ + 9.0f));
        addCollider(centerX - 10.0f, 0.55f, centerZ + 9.0f, 0.1f, 1.1f, 1.0f);

        Mesh alcoveWallRight = createCubeMesh(0.1f, 1.1f, 1.0f);
        sceneMeshes.add(alcoveWallRight);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX - 8.0f, 0.55f, centerZ + 9.0f));
        addCollider(centerX - 8.0f, 0.55f, centerZ + 9.0f, 0.1f, 1.1f, 1.0f);

        Mesh crate = createCubeMesh(CRATE_SIZE, CRATE_SIZE, CRATE_SIZE);
        sceneMeshes.add(crate);
        cratePosition.set(centerX - 1.5f, CRATE_SIZE * 0.5f, centerZ - 5.5f);
        sceneTransforms.add(new Matrix4().setToTranslation(cratePosition));
        addCollider(cratePosition.x, cratePosition.y, cratePosition.z, CRATE_SIZE, CRATE_SIZE, CRATE_SIZE);

        Mesh platform = createCubeMesh(3.0f, 0.1f, 3.0f);
        sceneMeshes.add(platform);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX + 6.5f, 1.0f, centerZ + 6.0f));
        addCollider(centerX + 6.5f, 1.0f, centerZ + 6.0f, 3.0f, 0.1f, 3.0f);
    }

    private void addCollider(float centerX, float centerY, float centerZ, float width, float height, float depth) {
        Vector3 min = new Vector3(
            centerX - (width * 0.5f),
            centerY - (height * 0.5f),
            centerZ - (depth * 0.5f)
        );
        Vector3 max = new Vector3(
            centerX + (width * 0.5f),
            centerY + (height * 0.5f),
            centerZ + (depth * 0.5f)
        );
        worldColliders.add(new BoundingBox(min, max));
    }

    private Mesh createCubeMesh(float width, float height, float depth) {
        Mesh mesh = new Mesh(
            true,
            24,
            36,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
        );

        float w = width * 0.5f;
        float h = height * 0.5f;
        float d = depth * 0.5f;

        float[] vertices = {
            -w, -h,  d,  0,  0,  1,  1, 1, 1, 1,
             w, -h,  d,  0,  0,  1,  1, 1, 1, 1,
             w,  h,  d,  0,  0,  1,  1, 1, 1, 1,
            -w,  h,  d,  0,  0,  1,  1, 1, 1, 1,

             w, -h, -d,  0,  0, -1,  1, 1, 1, 1,
            -w, -h, -d,  0,  0, -1,  1, 1, 1, 1,
            -w,  h, -d,  0,  0, -1,  1, 1, 1, 1,
             w,  h, -d,  0,  0, -1,  1, 1, 1, 1,

             w, -h,  d,  1,  0,  0,  1, 1, 1, 1,
             w, -h, -d,  1,  0,  0,  1, 1, 1, 1,
             w,  h, -d,  1,  0,  0,  1, 1, 1, 1,
             w,  h,  d,  1,  0,  0,  1, 1, 1, 1,

            -w, -h, -d, -1,  0,  0,  1, 1, 1, 1,
            -w, -h,  d, -1,  0,  0,  1, 1, 1, 1,
            -w,  h,  d, -1,  0,  0,  1, 1, 1, 1,
            -w,  h, -d, -1,  0,  0,  1, 1, 1, 1,

            -w,  h, -d,  0,  1,  0,  1, 1, 1, 1,
            -w,  h,  d,  0,  1,  0,  1, 1, 1, 1,
             w,  h,  d,  0,  1,  0,  1, 1, 1, 1,
             w,  h, -d,  0,  1,  0,  1, 1, 1, 1,

            -w, -h,  d,  0, -1,  0,  1, 1, 1, 1,
            -w, -h, -d,  0, -1,  0,  1, 1, 1, 1,
             w, -h, -d,  0, -1,  0,  1, 1, 1, 1,
             w, -h,  d,  0, -1,  0,  1, 1, 1, 1
        };

        short[] indices = {
             0,  1,  2,  2,  3,  0,
             4,  5,  6,  6,  7,  4,
             8,  9, 10, 10, 11,  8,
            12, 13, 14, 14, 15, 12,
            16, 17, 18, 18, 19, 16,
            20, 21, 22, 22, 23, 20
        };

        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    @Override
    public void render(float delta) {
        float clampedDelta = Math.max(0.0f, delta);
        elapsedSeconds += clampedDelta;
        revealedFlashRemaining = Math.max(0.0f, revealedFlashRemaining - clampedDelta);
        inventoryFullMessageRemaining = Math.max(0.0f, inventoryFullMessageRemaining - clampedDelta);

        handleRuntimeInput();
        updateGameplaySystems(clampedDelta);
        updateCameraFov(clampedDelta);

        renderSceneToFbo();
        renderPostProcessToBackBuffer();
        renderHudOverlays();
    }

    private void handleRuntimeInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            showGraphDebug = !showGraphDebug;
        }

        boolean increasePressed =
            Gdx.input.isKeyJustPressed(Input.Keys.EQUALS) ||
            Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD);

        boolean decreasePressed =
            Gdx.input.isKeyJustPressed(Input.Keys.MINUS) ||
            Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT);

        if (increasePressed) {
            vhsStrength = MathUtils.clamp(vhsStrength + VHS_STRENGTH_STEP, VHS_STRENGTH_MIN, VHS_STRENGTH_MAX);
            Gdx.app.log("VHS", "strength=" + vhsStrength);
        } else if (decreasePressed) {
            vhsStrength = MathUtils.clamp(vhsStrength - VHS_STRENGTH_STEP, VHS_STRENGTH_MIN, VHS_STRENGTH_MAX);
            Gdx.app.log("VHS", "strength=" + vhsStrength);
        }
    }

    private void updateGameplaySystems(float delta) {
        playerController.update(delta);
        handleCarryAndInventoryInput(delta);
        carrySystem.update(delta);
        updatePhysicsWorld(delta);
        syncCarriableTransforms();
        playerInteractionSystem.update();
        playerFeatureExtractor.update(delta, playerController);
        playerFootstepSoundEmitter.update(delta, elapsedSeconds);
        soundPropagationOrchestrator.update(delta);

        playerController.getPosition(playerWorldPosition);
        String listenerNodeId = findNearestNodeId(playerWorldPosition);
        spatialCueController.setListenerNode(listenerNodeId, playerWorldPosition);
    }

    private void updateCameraFov(float delta) {
        float targetFov = playerController.getMovementState() == MovementState.RUN ? RUN_FOV : BASE_FOV;
        float alpha = MathUtils.clamp(delta * FOV_LERP_SPEED, 0.0f, 1.0f);
        currentFov = MathUtils.lerp(currentFov, targetFov, alpha);
        camera.fieldOfView = currentFov;
        camera.update();
    }

    private void renderSceneToFbo() {
        sceneFbo.begin();
        Gdx.gl.glViewport(0, 0, fboWidth, fboHeight);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderWorldMeshes();
        renderPlayerViewModel();

        sceneFbo.end();
    }

    private void renderWorldMeshes() {
        worldShader.bind();
        worldShader.setUniformMatrix("u_projViewTrans", camera.combined);
        worldShader.setUniformf("u_cameraPos", camera.position);
        worldShader.setUniformf("u_lightDirection", 0.35f, 1.0f, 0.2f);
        worldShader.setUniformf("u_baseColor", 0.68f, 0.74f, 0.80f);
        worldShader.setUniformf("u_ambientColor", 0.25f, 0.27f, 0.30f);
        worldShader.setUniformf("u_shadowColor", 0.08f, 0.09f, 0.12f);
        worldShader.setUniformf("u_shadowStrength", 0.5f);
        worldShader.setUniformf("u_fogColor", 0.06f, 0.07f, 0.10f);
        worldShader.setUniformf("u_blindFogColor", 0.02f, 0.02f, 0.03f);
        worldShader.setUniformf("u_fogStart", 8.0f);
        worldShader.setUniformf("u_fogEnd", 42.0f);
        worldShader.setUniformf("u_blindFogStart", 18.0f);
        worldShader.setUniformf("u_blindFogEnd", 36.0f);
        worldShader.setUniformf("u_blindFogStrength", 0.28f);
        worldShader.setUniformf("u_time", elapsedSeconds);
        worldShader.setUniformf("u_scanlineStrength", 0.12f);
        worldShader.setUniformf("u_ditherLevels", 8.0f);
        worldShader.setUniformf("u_phosphorMaskStrength", 0.25f);

        for (int i = 0; i < sceneMeshes.size; i++) {
            Mesh mesh = sceneMeshes.get(i);
            Matrix4 transform = sceneTransforms.get(i);
            worldShader.setUniformMatrix("u_modelTrans", transform);
            mesh.render(worldShader, GL20.GL_TRIANGLES);
        }

        for (CarriableItem carriableItem : worldCarriableItems) {
            if (carriableItem.isBroken()) {
                continue;
            }

            Mesh mesh = carriableMeshes.get(carriableItem);
            Matrix4 transform = carriableTransforms.get(carriableItem);
            if (mesh == null || transform == null) {
                continue;
            }

            worldShader.setUniformMatrix("u_modelTrans", transform);
            mesh.render(worldShader, GL20.GL_TRIANGLES);
        }
    }

    private void renderPlayerViewModel() {
        tmpForward.set(camera.direction).nor();
        tmpRight.set(tmpForward).crs(camera.up).nor();
        tmpUp.set(camera.up).nor();

        tmpViewModelOffset
            .setZero()
            .mulAdd(tmpRight, VIEW_MODEL_OFFSET_RIGHT)
            .mulAdd(tmpUp, VIEW_MODEL_OFFSET_UP)
            .mulAdd(tmpForward, VIEW_MODEL_OFFSET_FORWARD);

        playerViewModelTransform
            .idt()
            .translate(
                camera.position.x + tmpViewModelOffset.x,
                camera.position.y + tmpViewModelOffset.y,
                camera.position.z + tmpViewModelOffset.z
            )
            .rotate(Vector3.Y, playerController.getYaw())
            .rotate(Vector3.X, -playerController.getPitch())
            .scale(VIEW_MODEL_SCALE, VIEW_MODEL_SCALE, VIEW_MODEL_SCALE);

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        playerShaderProgram.bind();
        playerShaderProgram.setUniformMatrix("u_projViewTrans", camera.combined);
        playerShaderProgram.setUniformMatrix("u_modelTrans", playerViewModelTransform);
        playerShaderProgram.setUniformf("u_cameraPos", camera.position);
        playerShaderProgram.setUniformf("u_lightDirection", 0.35f, 1.0f, 0.2f);
        playerShaderProgram.setUniformf("u_baseColor", 0.12f, 0.78f, 0.74f);
        playerShaderProgram.setUniformf("u_ambientColor", 0.28f, 0.3f, 0.32f);
        playerShaderProgram.setUniformf("u_rimColor", 0.85f, 0.95f, 1.0f);
        playerShaderProgram.setUniformf("u_rimStrength", 0.28f);
        playerViewModelMesh.render(playerShaderProgram, GL20.GL_TRIANGLES);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    private void renderPostProcessToBackBuffer() {
        int backBufferWidth = Gdx.graphics.getBackBufferWidth();
        int backBufferHeight = Gdx.graphics.getBackBufferHeight();

        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
        Gdx.gl.glViewport(0, 0, backBufferWidth, backBufferHeight);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        sceneFbo.getColorBufferTexture().bind(0);

        vhsShader.bind();
        vhsShader.setUniformi("u_texture", 0);
        vhsShader.setUniformf("u_screenSize", (float) backBufferWidth, (float) backBufferHeight);
        vhsShader.setUniformf("u_time", elapsedSeconds);
        vhsShader.setUniformf("u_vhsStrength", vhsStrength);

        fullscreenQuadMesh.render(vhsShader, GL20.GL_TRIANGLES);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    private void renderHudOverlays() {
        shapeRenderer.setProjectionMatrix(hudProjection);
        renderCrosshair();
        renderInventoryBar();

        if (showGraphDebug) {
            renderAcousticGraphOverlay();
        }
    }

    private void renderInventoryBar() {
        float slotSize = 46.0f;
        float slotGap = 9.0f;
        float totalWidth = (InventorySystem.SLOT_COUNT * slotSize) + ((InventorySystem.SLOT_COUNT - 1) * slotGap);
        float startX = (Gdx.graphics.getWidth() - totalWidth) * 0.5f;
        float y = 18.0f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int slotIndex = 0; slotIndex < InventorySystem.SLOT_COUNT; slotIndex++) {
            float x = startX + (slotIndex * (slotSize + slotGap));
            InventorySystem.InventorySlotEntry slotEntry = inventorySystem.getSlotEntry(slotIndex);

            shapeRenderer.setColor(slotIndex == inventorySystem.getActiveSlotIndex() ? INVENTORY_SLOT_ACTIVE : INVENTORY_SLOT_INACTIVE);
            shapeRenderer.rect(x, y, slotSize, slotSize);

            if (slotEntry != null) {
                shapeRenderer.setColor(INVENTORY_SLOT_FILLED);
                shapeRenderer.rect(x + 6.0f, y + 6.0f, slotSize - 12.0f, slotSize - 12.0f);

                if (slotEntry.count() > 1) {
                    float countWidth = Math.min(slotSize - 10.0f, 8.0f + (slotEntry.count() * 2.0f));
                    shapeRenderer.setColor(0.12f, 0.16f, 0.2f, 0.9f);
                    shapeRenderer.rect(x + slotSize - countWidth - 3.0f, y + 3.0f, countWidth, 9.0f);
                }
            }
        }

        if (inventoryFullMessageRemaining > 0.0f) {
            float alpha = MathUtils.clamp(inventoryFullMessageRemaining / INVENTORY_FULL_MESSAGE_DURATION, 0.0f, 1.0f);
            shapeRenderer.setColor(INVENTORY_WARNING.r, INVENTORY_WARNING.g, INVENTORY_WARNING.b, alpha);
            shapeRenderer.rect(startX + (totalWidth * 0.2f), y + slotSize + 9.0f, totalWidth * 0.6f, 10.0f);
        }
        shapeRenderer.end();
    }

    private void spawnInitialCarriableItems() {
        spawnCarriableItem(ItemType.METAL_PIPE, FLOOR_WIDTH * 0.5f + 2.5f, 0.4f, FLOOR_LENGTH * 0.5f - 3.0f, 0.18f, 0.9f, 0.18f);
        spawnCarriableItem(ItemType.GLASS_BOTTLE, FLOOR_WIDTH * 0.5f - 2.5f, 0.28f, FLOOR_LENGTH * 0.5f - 2.2f, 0.15f, 0.55f, 0.15f);
        spawnCarriableItem(ItemType.CARDBOARD_BOX, FLOOR_WIDTH * 0.5f + 0.7f, 0.38f, FLOOR_LENGTH * 0.5f + 1.8f, 0.45f, 0.45f, 0.45f);
    }

    private void spawnCarriableItem(
        ItemType itemType,
        float x,
        float y,
        float z,
        float width,
        float height,
        float depth
    ) {
        ItemDefinition definition = ItemDefinition.create(itemType);
        CarriableItem carriableItem = new CarriableItem(definition, new Vector3(x, y, z));
        Mesh mesh = createCubeMesh(width, height, depth);
        Matrix4 transform = new Matrix4().setToTranslation(x, y, z);

        btBoxShape collisionShape = new btBoxShape(new Vector3(width * 0.5f, height * 0.5f, depth * 0.5f));
        btDefaultMotionState motionState = new btDefaultMotionState(new Matrix4().setToTranslation(x, y, z));
        btRigidBody rigidBody = createCarriableRigidBody(definition, collisionShape, motionState);
        physicsWorld.addRigidBody(rigidBody);

        carriableItem.setRigidBody(rigidBody);

        worldCarriableItems.add(carriableItem);
        carriableMeshes.put(carriableItem, mesh);
        carriableTransforms.put(carriableItem, transform);
        carriableRigidBodies.put(carriableItem, rigidBody);
        carriableCollisionShapes.put(carriableItem, collisionShape);
        carriableMotionStates.put(carriableItem, motionState);
        rigidBodyToCarriable.put(rigidBody, carriableItem);
    }

    private btRigidBody createCarriableRigidBody(
        ItemDefinition definition,
        btCollisionShape collisionShape,
        btDefaultMotionState motionState
    ) {
        tmpInertia.setZero();
        collisionShape.calculateLocalInertia(definition.mass(), tmpInertia);

        btRigidBodyConstructionInfo constructionInfo = new btRigidBodyConstructionInfo(
            definition.mass(),
            motionState,
            collisionShape,
            tmpInertia
        );
        btRigidBody rigidBody = new btRigidBody(constructionInfo);
        constructionInfo.dispose();

        rigidBody.setFriction(0.85f);
        rigidBody.setRestitution(0.1f);
        rigidBody.setDamping(0.08f, 0.35f);
        return rigidBody;
    }

    private void handleCarryAndInventoryInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            inventorySystem.setActiveSlot(0);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            inventorySystem.setActiveSlot(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            inventorySystem.setActiveSlot(2);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            inventorySystem.setActiveSlot(3);
        }

        int scrollAmount = pendingScrollSteps;
        pendingScrollSteps = 0;
        if (scrollAmount != 0) {
            if (carrySystem.isHoldingItem()) {
                carrySystem.adjustCarryDistance(-scrollAmount * CARRY_SCROLL_STEP);
            } else {
                inventorySystem.cycleActiveSlot(scrollAmount > 0 ? 1 : -1);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (carrySystem.isHoldingItem()) {
                carrySystem.dropHeldItem();
            } else {
                tryPickupNearestCarriable();
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && carrySystem.isHoldingItem()) {
            CarriableItem heldItem = carrySystem.heldItem();
            float throwStrength = heldItem != null ? heldItem.definition().throwStrength() : 0f;
            CarriableItem thrownItem = carrySystem.throwHeldItem(camera.direction, throwStrength);
            if (thrownItem != null) {
                tmpCarriableDirection.set(camera.direction).nor();
                tmpCarriableOffset.set(tmpCarriableDirection).scl(0.8f).add(0.0f, 0.1f, 0.0f);
                thrownItem.setWorldPosition(tmpDropPosition.set(camera.position).add(tmpCarriableOffset));
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) && carrySystem.isHoldingItem()) {
            CarriableItem slottedItem = carrySystem.heldItem();
            if (slottedItem != null) {
                boolean added = inventorySystem.addItem(slottedItem.definition());
                if (added) {
                    carrySystem.dropHeldItem();
                    removeWorldCarriableItem(slottedItem);
                } else {
                    inventoryFullMessageRemaining = INVENTORY_FULL_MESSAGE_DURATION;
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            InventorySystem.InventorySlotEntry removedEntry = inventorySystem.removeItem(inventorySystem.getActiveSlotIndex());
            if (removedEntry != null) {
                ItemDefinition itemDefinition = removedEntry.itemDefinition();
                if (itemDefinition.isCarriable()) {
                    tmpDropPosition.set(camera.position).mulAdd(camera.direction, 1.1f);
                    spawnCarriableItem(
                        itemDefinition.itemType(),
                        tmpDropPosition.x,
                        Math.max(0.2f, tmpDropPosition.y - 0.8f),
                        tmpDropPosition.z,
                        0.25f,
                        0.25f,
                        0.25f
                    );
                } else {
                    emitItemEvent(SoundEvent.OBJECT_DROP_OR_BREAK, tmpDropPosition.set(camera.position), 0.25f);
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            handleUseActiveItem();
        }
    }

    private void tryPickupNearestCarriable() {
        CarriableItem nearestCarriable = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;

        for (CarriableItem carriableItem : worldCarriableItems) {
            if (carriableItem.isBroken()) {
                continue;
            }

            tmpToCrate.set(carriableItem.worldPosition()).sub(camera.position);
            float distanceSquared = tmpToCrate.len2();
            if (distanceSquared > (PICKUP_RANGE * PICKUP_RANGE) || distanceSquared <= 0.0001f) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            tmpToCrate.scl(1.0f / distance);
            float facingDot = tmpCarriableDirection.set(camera.direction).nor().dot(tmpToCrate);
            if (facingDot < INTERACTION_FACING_DOT) {
                continue;
            }

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestCarriable = carriableItem;
            }
        }

        if (nearestCarriable != null) {
            carrySystem.tryPickup(nearestCarriable);
        }
    }

    private void handleUseActiveItem() {
        InventorySystem.InventorySlotEntry activeEntry = inventorySystem.getActiveSlotEntry();
        if (activeEntry == null) {
            return;
        }

        ItemDefinition itemDefinition = activeEntry.itemDefinition();
        if (itemDefinition.itemType() == ItemType.NOISE_DECOY) {
            emitItemEvent(SoundEvent.NOISE_DECOY, tmpDropPosition.set(camera.position).mulAdd(camera.direction, 4.0f), 0.70f);
        } else if (itemDefinition.itemType() == ItemType.FLARE) {
            emitItemEvent(SoundEvent.OBJECT_DROP_OR_BREAK, tmpDropPosition.set(camera.position), 0.30f);
        }

        inventorySystem.consumeActiveItem();
    }

    private void emitItemEvent(SoundEvent eventType, Vector3 worldPosition, float intensity) {
        String sourceNodeId = findNearestNodeId(worldPosition);
        SoundEventData eventData = new SoundEventData(
            eventType,
            sourceNodeId,
            worldPosition,
            intensity,
            elapsedSeconds
        );
        PropagationResult propagationResult = soundPropagationOrchestrator.emitSoundEvent(eventData, elapsedSeconds);
        cacheRevealedNodesForFlash(propagationResult);
    }

    private void removeWorldCarriableItem(CarriableItem carriableItem) {
        carriableItem.markBroken();

        btRigidBody rigidBody = carriableRigidBodies.remove(carriableItem);
        if (rigidBody != null) {
            physicsWorld.removeRigidBody(rigidBody);
            rigidBodyToCarriable.remove(rigidBody);
            rigidBody.dispose();
        }

        btDefaultMotionState motionState = carriableMotionStates.remove(carriableItem);
        if (motionState != null) {
            motionState.dispose();
        }

        btCollisionShape collisionShape = carriableCollisionShapes.remove(carriableItem);
        if (collisionShape != null) {
            collisionShape.dispose();
        }

        Mesh mesh = carriableMeshes.remove(carriableItem);
        if (mesh != null) {
            mesh.dispose();
        }
        carriableTransforms.remove(carriableItem);
        worldCarriableItems.removeValue(carriableItem, true);
    }

    private void syncCarriableTransforms() {
        for (CarriableItem carriableItem : worldCarriableItems) {
            if (carriableItem.isBroken()) {
                continue;
            }

            Matrix4 transform = carriableTransforms.get(carriableItem);
            if (transform != null) {
                btRigidBody rigidBody = carriableItem.rigidBody();
                if (rigidBody != null) {
                    rigidBody.getWorldTransform(tmpBodyTransform);
                    transform.set(tmpBodyTransform);
                    tmpBodyTransform.getTranslation(tmpCarryPoint);
                    carriableItem.setWorldPosition(tmpCarryPoint);
                } else {
                    Vector3 itemPosition = carriableItem.worldPosition();
                    transform.setToTranslation(itemPosition.x, itemPosition.y, itemPosition.z);
                }
            }
        }
    }

    private void renderCrosshair() {
        float centerX = Gdx.graphics.getWidth() * 0.5f;
        float centerY = Gdx.graphics.getHeight() * 0.5f;

        boolean canInteract = isCrateInRangeAndFacing();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(canInteract ? CROSSHAIR_INTERACT_COLOR : CROSSHAIR_DEFAULT_COLOR);

        float leftX0 = centerX - CROSSHAIR_HALF_SIZE;
        float leftX1 = centerX - CROSSHAIR_GAP;
        float rightX0 = centerX + CROSSHAIR_GAP;
        float rightX1 = centerX + CROSSHAIR_HALF_SIZE;
        float downY0 = centerY - CROSSHAIR_HALF_SIZE;
        float downY1 = centerY - CROSSHAIR_GAP;
        float upY0 = centerY + CROSSHAIR_GAP;
        float upY1 = centerY + CROSSHAIR_HALF_SIZE;

        shapeRenderer.line(leftX0, centerY, leftX1, centerY);
        shapeRenderer.line(rightX0, centerY, rightX1, centerY);
        shapeRenderer.line(centerX, downY0, centerX, downY1);
        shapeRenderer.line(centerX, upY0, centerX, upY1);
        shapeRenderer.end();
    }

    private void renderAcousticGraphOverlay() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        float flashAlpha = revealedFlashRemaining > 0.0f
            ? MathUtils.clamp(revealedFlashRemaining / NODE_FLASH_DURATION, 0.0f, 1.0f)
            : 0.0f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (ObjectMap.Entry<String, Vector3> entry : graphNodePositions.entries()) {
            tmpProjected.set(entry.value);
            camera.project(tmpProjected, 0, 0, screenWidth, screenHeight);

            if (tmpProjected.z < 0.0f || tmpProjected.z > 1.0f) {
                continue;
            }

            shapeRenderer.setColor(NODE_BASE_COLOR);
            shapeRenderer.circle(tmpProjected.x, tmpProjected.y, NODE_DOT_RADIUS, 14);

            if (flashAlpha > 0.0f && lastRevealedNodeIds.contains(entry.key, false)) {
                shapeRenderer.setColor(
                    NODE_FLASH_COLOR.r,
                    NODE_FLASH_COLOR.g,
                    NODE_FLASH_COLOR.b,
                    flashAlpha
                );
                shapeRenderer.circle(tmpProjected.x, tmpProjected.y, NODE_FLASH_RADIUS, 16);
            }
        }
        shapeRenderer.end();
    }

    private boolean isCrateInRangeAndFacing() {
        tmpToCrate.set(cratePosition).sub(camera.position);
        float distance = tmpToCrate.len();
        if (distance <= 0.0001f || distance > INTERACTION_RANGE) {
            return false;
        }

        tmpToCrate.scl(1.0f / distance);
        float facingDot = tmpForward.set(camera.direction).nor().dot(tmpToCrate);
        return facingDot >= INTERACTION_FACING_DOT;
    }

    private void emitCrateInteractionSound() {
        String sourceNodeId = findNearestNodeId(cratePosition);
        SoundEventData eventData = new SoundEventData(
            SoundEvent.OBJECT_DROP_OR_BREAK,
            sourceNodeId,
            cratePosition,
            0.4f,
            elapsedSeconds
        );

        PropagationResult propagationResult = soundPropagationOrchestrator.emitSoundEvent(eventData, elapsedSeconds);
        cacheRevealedNodesForFlash(propagationResult);
    }

    private void cacheRevealedNodesForFlash(PropagationResult propagationResult) {
        lastRevealedNodeIds.clear();
        if (propagationResult == null || propagationResult.revealNodeIds().isEmpty()) {
            revealedFlashRemaining = 0.0f;
            return;
        }

        for (String nodeId : propagationResult.revealNodeIds()) {
            lastRevealedNodeIds.add(nodeId);
        }
        revealedFlashRemaining = NODE_FLASH_DURATION;
    }

    private String findNearestNodeId(Vector3 worldPosition) {
        String nearestNodeId = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;

        for (ObjectMap.Entry<String, Vector3> entry : graphNodePositions.entries()) {
            float distanceSquared = entry.value.dst2(worldPosition);
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestNodeId = entry.key;
            }
        }

        return nearestNodeId == null ? "center" : nearestNodeId;
    }

    private void updateHudProjection(int width, int height) {
        hudProjection.setToOrtho2D(0.0f, 0.0f, width, height);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        updateHudProjection(width, height);
        recreateSceneFbo(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    @Override
    public void dispose() {
        for (Mesh mesh : sceneMeshes) {
            mesh.dispose();
        }

        for (ObjectMap.Entry<CarriableItem, Mesh> entry : carriableMeshes.entries()) {
            if (entry.value != null) {
                entry.value.dispose();
            }
        }

        for (ObjectMap.Entry<CarriableItem, btRigidBody> entry : carriableRigidBodies.entries()) {
            if (physicsWorld != null && entry.value != null) {
                physicsWorld.removeRigidBody(entry.value);
                entry.value.dispose();
            }
        }
        carriableRigidBodies.clear();

        for (ObjectMap.Entry<CarriableItem, btDefaultMotionState> entry : carriableMotionStates.entries()) {
            if (entry.value != null) {
                entry.value.dispose();
            }
        }
        carriableMotionStates.clear();

        for (ObjectMap.Entry<CarriableItem, btCollisionShape> entry : carriableCollisionShapes.entries()) {
            if (entry.value != null) {
                entry.value.dispose();
            }
        }
        carriableCollisionShapes.clear();

        for (btRigidBody staticRigidBody : staticRigidBodies) {
            if (physicsWorld != null && staticRigidBody != null) {
                physicsWorld.removeRigidBody(staticRigidBody);
                staticRigidBody.dispose();
            }
        }
        staticRigidBodies.clear();

        for (btDefaultMotionState motionState : staticMotionStates) {
            if (motionState != null) {
                motionState.dispose();
            }
        }
        staticMotionStates.clear();

        for (btCollisionShape collisionShape : staticCollisionShapes) {
            if (collisionShape != null) {
                collisionShape.dispose();
            }
        }
        staticCollisionShapes.clear();

        rigidBodyToCarriable.clear();

        if (playerViewModelMesh != null) {
            playerViewModelMesh.dispose();
        }
        if (fullscreenQuadMesh != null) {
            fullscreenQuadMesh.dispose();
        }
        if (worldShader != null) {
            worldShader.dispose();
        }
        if (playerShaderProgram != null) {
            playerShaderProgram.dispose();
        }
        if (vhsShader != null) {
            vhsShader.dispose();
        }
        if (sceneFbo != null) {
            sceneFbo.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (spatialCueController != null) {
            spatialCueController.dispose();
        }
        if (physicsWorld != null) {
            physicsWorld.dispose();
        }
        if (physicsConstraintSolver != null) {
            physicsConstraintSolver.dispose();
        }
        if (physicsBroadphase != null) {
            physicsBroadphase.dispose();
        }
        if (physicsCollisionDispatcher != null) {
            physicsCollisionDispatcher.dispose();
        }
        if (physicsCollisionConfiguration != null) {
            physicsCollisionConfiguration.dispose();
        }
    }
}
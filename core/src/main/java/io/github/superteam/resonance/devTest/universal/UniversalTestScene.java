package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.badlogic.gdx.physics.bullet.collision.btStaticPlaneShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import io.github.superteam.resonance.devTest.PlayerTestScreen;
import io.github.superteam.resonance.devTest.universal.diagnostics.DiagnosticOverlay;
import io.github.superteam.resonance.devTest.universal.zones.BlindChamberZone;
import io.github.superteam.resonance.devTest.universal.zones.CrouchAlcoveZone;
import io.github.superteam.resonance.devTest.universal.zones.ItemInteractionZone;
import io.github.superteam.resonance.devTest.universal.zones.ParticleArenaZone;
import io.github.superteam.resonance.devTest.universal.zones.RampStairsZone;
import io.github.superteam.resonance.devTest.universal.zones.ShaderCorridorZone;
import io.github.superteam.resonance.devTest.universal.zones.SoundPropagationZone;
import io.github.superteam.resonance.items.CarriableItem;
import io.github.superteam.resonance.items.ItemDefinition;
import io.github.superteam.resonance.items.ItemType;
import io.github.superteam.resonance.player.CameraBreathingController;
import io.github.superteam.resonance.player.CarrySystem;
import io.github.superteam.resonance.player.ImpactListener;
import io.github.superteam.resonance.player.InventorySystem;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.player.PlayerController;
import io.github.superteam.resonance.player.PlayerFeatureExtractor;
import io.github.superteam.resonance.player.PlayerFootstepSoundEmitter;
import io.github.superteam.resonance.player.SimplePanicModel;
import io.github.superteam.resonance.rendering.BodyCamPassFrameBuffer;
import io.github.superteam.resonance.rendering.BodyCamSettingsStore;
import io.github.superteam.resonance.rendering.BodyCamVHSAnimator;
import io.github.superteam.resonance.rendering.BodyCamVHSSettings;
import io.github.superteam.resonance.rendering.BodyCamVHSShaderLoader;
import io.github.superteam.resonance.rendering.BodyCamVHSVisualizer;
import io.github.superteam.resonance.rendering.blind.BlindEffectConfigStore;
import io.github.superteam.resonance.rendering.blind.BlindEffectController;
import io.github.superteam.resonance.rendering.blind.BlindEffectRevealConfig;
import io.github.superteam.resonance.rendering.blind.BlindFogUniformUpdater;
import io.github.superteam.resonance.sound.AcousticGraphEngine;
import io.github.superteam.resonance.sound.DijkstraPathfinder;
import io.github.superteam.resonance.sound.PhysicsNoiseEmitter;
import io.github.superteam.resonance.sound.PropagationResult;
import io.github.superteam.resonance.sound.RealtimeMicSystem;
import io.github.superteam.resonance.sound.SonarRenderer;
import io.github.superteam.resonance.sound.SoundBalancingConfigStore;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import io.github.superteam.resonance.sound.SpatialCueController;
import io.github.superteam.resonance.sound.TestAcousticGraphFactory;
import java.util.HashSet;
import java.util.Set;

/**
 * High-fidelity universal test environment with rendering, physics, sound, and item systems.
 */
public final class UniversalTestScene extends ScreenAdapter {
    private static final float HUB_X = 40.0f;
    private static final float HUB_Z = 40.0f;
    private static final float FLOOR_Y = 0.02f;
    private static final float HUB_HALF_EXTENT = 4.5f;
    private static final float ZONE_HALF_EXTENT = 5.5f;
    private static final float CORRIDOR_HALF_WIDTH = 1.8f;

    private static final float EYE_HEIGHT = 1.6f;
    private static final float BASE_FOV = 75.0f;
    private static final float RUN_FOV = 85.0f;
    private static final float FOV_LERP_SPEED = 6.5f;

    private static final float PICKUP_RANGE = 3.0f;
    private static final float PICKUP_RADIUS = 1.75f;
    private static final float INTERACTION_FACING_DOT = 0.70f;
    private static final float CARRY_SCROLL_STEP = 0.15f;
    private static final int MAX_FLARE_COUNT = 3;
    private static final float INVENTORY_FULL_MESSAGE_DURATION = 2.0f;

    private static final float VIEW_MODEL_SCALE = 0.18f;
    private static final float VIEW_MODEL_OFFSET_RIGHT = 0.28f;
    private static final float VIEW_MODEL_OFFSET_UP = -0.24f;
    private static final float VIEW_MODEL_OFFSET_FORWARD = 0.45f;

    private static final float CROSSHAIR_HALF_SIZE = 7.0f;
    private static final float CROSSHAIR_GAP = 3.0f;
    private static final float NODE_DOT_RADIUS = 4.0f;
    private static final float NODE_FLASH_RADIUS = 7.5f;
    private static final float NODE_FLASH_DURATION = 0.55f;

    private static final float MIC_THRESHOLD_RMS = 300f;
    private static final float MIC_PULSE_COOLDOWN = 8.0f;
    private static final float ITEM_SPAWN_Y_OFFSET = 0.02f;
    private static final int MIC_HISTORY_SIZE = 128;

    private final PerspectiveCamera camera;
    private final PlayerController playerController;
    private final CameraBreathingController cameraBreathingController;
    private final DiagnosticOverlay diagnosticOverlay;
    private final SpriteBatch hudBatch;
    private final BitmapFont hudFont;
    private final ShapeRenderer shapeRenderer;

    private final Array<TestZone> zones = new Array<>();
    private final Vector3 playerPos = new Vector3();
    private TestZone activeZone;

    private final Array<Mesh> sceneMeshes = new Array<>();
    private final Array<Matrix4> sceneTransforms = new Array<>();
    private final Array<BoundingBox> worldColliders = new Array<>();
    private final ObjectMap<String, Vector3> graphNodePositions = new ObjectMap<>();
    private final Array<String> lastRevealedNodeIds = new Array<>();

    private final Array<ConsumablePickup> worldConsumablePickups = new Array<>();
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

    private final Matrix4 hudProjection = new Matrix4();
    private final Matrix4 playerViewModelTransform = new Matrix4();
    private final Matrix4 tmpBodyTransform = new Matrix4();
    private final Matrix4 tmpBreathingRotation = new Matrix4();

    private final Vector3 tmpForward = new Vector3();
    private final Vector3 tmpRight = new Vector3();
    private final Vector3 tmpUp = new Vector3();
    private final Vector3 tmpViewModelOffset = new Vector3();
    private final Vector3 tmpToItem = new Vector3();
    private final Vector3 tmpItemDirection = new Vector3();
    private final Vector3 tmpDropPosition = new Vector3();
    private final Vector3 tmpCarryPoint = new Vector3();
    private final Vector3 tmpImpulsePoint = new Vector3();
    private final Vector3 tmpInertia = new Vector3();
    private final Vector3 tmpProjected = new Vector3();
    private final Vector3 tmpBaseCameraUp = new Vector3();

    private ShaderProgram worldShader;
    private ShaderProgram playerShaderProgram;
    private BodyCamVHSShaderLoader bodyCamShaderLoader;
    private BodyCamPassFrameBuffer bodyCamFrameBuffer;
    private BodyCamVHSVisualizer bodyCamVisualizer;
    private BodyCamVHSSettings bodyCamSettings;
    private BlindEffectRevealConfig blindEffectConfig;
    private BlindEffectController blindEffectController;
    private final SimplePanicModel panicModel = new SimplePanicModel();
    private final BlindFogUniformUpdater blindFogUniformUpdater = new BlindFogUniformUpdater();

    private Mesh playerViewModelMesh;
    private Mesh fullscreenQuadMesh;

    private btCollisionConfiguration physicsCollisionConfiguration;
    private btCollisionDispatcher physicsCollisionDispatcher;
    private btBroadphaseInterface physicsBroadphase;
    private btConstraintSolver physicsConstraintSolver;
    private btDiscreteDynamicsWorld physicsWorld;

    private AcousticGraphEngine acousticGraphEngine;
    private SoundPropagationOrchestrator soundPropagationOrchestrator;
    private SpatialCueController spatialCueController;
    private PlayerFootstepSoundEmitter playerFootstepSoundEmitter;
    private PlayerFeatureExtractor playerFeatureExtractor;
    private PhysicsNoiseEmitter physicsNoiseEmitter;
    private ImpactListener impactListener;

    private CarrySystem carrySystem;
    private InventorySystem inventorySystem;

    private RealtimeMicSystem realtimeMicSystem;
    private final float[] micRmsHistory = new float[MIC_HISTORY_SIZE];

    private float elapsedSeconds;
    private float currentFov = BASE_FOV;
    private float revealedFlashRemaining;
    private float lastSoundIntensity;
    private float inventoryFullMessageRemaining;
    private float micPulseCooldownRemaining;
    private float lastMicLevelNormalized;
    private float appliedBreathingY;
    private float appliedBreathingRollDeg;
    private int micHistoryCursor;
    private int pendingScrollSteps;
    private boolean showGraphDebug;
    private boolean showBlindRadiusDebug;
    private boolean micEnabled;
    private boolean breathingApplied;
    private boolean lastMicSpeakingActive;

    public UniversalTestScene() {
        camera = new PerspectiveCamera(BASE_FOV, Math.max(1, Gdx.graphics.getWidth()), Math.max(1, Gdx.graphics.getHeight()));
        camera.position.set(HUB_X, EYE_HEIGHT, HUB_Z);
        camera.lookAt(HUB_X + 1.0f, EYE_HEIGHT, HUB_Z);
        camera.near = 0.1f;
        camera.far = 200.0f;
        camera.update();

        playerController = new PlayerController(camera);
        cameraBreathingController = new CameraBreathingController();
        diagnosticOverlay = new DiagnosticOverlay();
        hudBatch = new SpriteBatch();
        hudFont = new BitmapFont();
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        shapeRenderer = new ShapeRenderer();

        loadWorldShader();
        loadPlayerShader();
        bodyCamShaderLoader = new BodyCamVHSShaderLoader("shaders/vert/body_cam_vhs.vert", "shaders/frag/body_cam_vhs.frag");
        bodyCamFrameBuffer = new BodyCamPassFrameBuffer();
        bodyCamSettings = BodyCamSettingsStore.loadOrDefault("config/body_cam_settings.json");
        blindEffectConfig = BlindEffectConfigStore.loadOrDefault("config/blind_effect_config.json");
        blindEffectController = new BlindEffectController(blindEffectConfig, panicModel);
        fullscreenQuadMesh = createFullscreenQuadMesh();
        bodyCamVisualizer = new BodyCamVHSVisualizer(
            bodyCamFrameBuffer,
            bodyCamShaderLoader.shader(),
            fullscreenQuadMesh,
            new BodyCamVHSAnimator()
        );

        buildProceduralHubScene();
        playerController.setWorldColliders(worldColliders);
        playerViewModelMesh = createCubeMesh(1.0f, 1.0f, 1.0f);

        initializePhysicsWorld();
        registerStaticWorldBodies();

        carrySystem = new CarrySystem(camera);
        inventorySystem = new InventorySystem();

        acousticGraphEngine = TestAcousticGraphFactory.create();
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
        soundPropagationOrchestrator.registerDirectorListener((soundEventData, propagationResult) -> {
            lastSoundIntensity = Math.max(lastSoundIntensity, soundEventData.baseIntensity());
            boolean sonarTriggered = blindEffectController.onSoundEvent(soundEventData);
            if (sonarTriggered) {
                cacheRevealedNodesForFlash(propagationResult);
            }
        });

        playerFootstepSoundEmitter = new PlayerFootstepSoundEmitter(
            playerController,
            soundPropagationOrchestrator,
            this::findNearestNodeId
        );

        playerFeatureExtractor = new PlayerFeatureExtractor();
        playerFeatureExtractor.setLastKnownPosition(camera.position);

        spawnInitialCarriableItems();
        spawnInitialConsumablePickups();

        realtimeMicSystem = new RealtimeMicSystem(MIC_THRESHOLD_RMS);

        initializeZones();
        activeZone = zones.first();
        activeZone.onEnter();

        recreateBodyCamFrameBuffers(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
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

    private void initializeZones() {
        zones.add(new RampStairsZone(new Vector3(HUB_X + 16.0f, 0.0f, HUB_Z)));
        zones.add(new CrouchAlcoveZone(new Vector3(HUB_X, 0.0f, HUB_Z - 16.0f)));
        zones.add(new SoundPropagationZone(new Vector3(HUB_X, 0.0f, HUB_Z + 16.0f)));
        zones.add(new ParticleArenaZone(new Vector3(HUB_X + 16.0f, 0.0f, HUB_Z + 16.0f)));
        zones.add(new ItemInteractionZone(new Vector3(HUB_X - 16.0f, 0.0f, HUB_Z + 16.0f)));
        zones.add(new ShaderCorridorZone(new Vector3(HUB_X - 16.0f, 0.0f, HUB_Z)));
        zones.add(new BlindChamberZone(new Vector3(HUB_X, 0.0f, HUB_Z + 32.0f)));

        for (TestZone zone : zones) {
            zone.setUp();
        }
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

    private void recreateBodyCamFrameBuffers(int width, int height) {
        bodyCamFrameBuffer.resize(width, height);
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

        btStaticPlaneShape floorShape = new btStaticPlaneShape(new Vector3(0f, 1f, 0f), 0f);
        btDefaultMotionState floorMotionState = new btDefaultMotionState(new Matrix4().idt());
        btRigidBodyConstructionInfo floorInfo = new btRigidBodyConstructionInfo(0f, floorMotionState, floorShape, Vector3.Zero);
        btRigidBody floorBody = new btRigidBody(floorInfo);
        floorInfo.dispose();

        physicsWorld.addRigidBody(floorBody);
        staticCollisionShapes.add(floorShape);
        staticMotionStates.add(floorMotionState);
        staticRigidBodies.add(floorBody);
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

    private void buildProceduralHubScene() {
        addBox(HUB_X, -0.05f, HUB_Z, 80.0f, 0.1f, 80.0f, false);

        addBox(HUB_X, 2.0f, 80.4f, 80.0f, 4.0f, 0.2f, true);
        addBox(HUB_X, 2.0f, -0.4f, 80.0f, 4.0f, 0.2f, true);
        addBox(-0.4f, 2.0f, HUB_Z, 0.2f, 4.0f, 80.0f, true);
        addBox(80.4f, 2.0f, HUB_Z, 0.2f, 4.0f, 80.0f, true);

        addBox(HUB_X, 0.01f, HUB_Z, 4.5f, 0.2f, 4.5f, false);

        addBox(HUB_X + 16.0f, -0.02f, HUB_Z, 5.5f, 0.15f, 5.5f, false);
        addBox(HUB_X, -0.02f, HUB_Z - 16.0f, 5.5f, 0.15f, 5.5f, false);
        addBox(HUB_X, -0.02f, HUB_Z + 16.0f, 5.5f, 0.15f, 5.5f, false);
        addBox(HUB_X + 16.0f, -0.02f, HUB_Z + 16.0f, 5.5f, 0.15f, 5.5f, false);
        addBox(HUB_X - 16.0f, -0.02f, HUB_Z + 16.0f, 5.5f, 0.15f, 5.5f, false);
        addBox(HUB_X - 16.0f, -0.02f, HUB_Z, 5.5f, 0.15f, 5.5f, false);
        addBox(HUB_X, -0.02f, HUB_Z + 32.0f, 5.5f, 0.15f, 5.5f, false);

        addBox(HUB_X + 8.0f, 1.75f, HUB_Z + CORRIDOR_HALF_WIDTH, 16.0f, 3.5f, 0.2f, true);
        addBox(HUB_X + 8.0f, 1.75f, HUB_Z - CORRIDOR_HALF_WIDTH, 16.0f, 3.5f, 0.2f, true);

        addBox(HUB_X - CORRIDOR_HALF_WIDTH, 1.75f, HUB_Z - 8.0f, 0.2f, 3.5f, 16.0f, true);
        addBox(HUB_X + CORRIDOR_HALF_WIDTH, 1.75f, HUB_Z - 8.0f, 0.2f, 3.5f, 16.0f, true);

        addBox(HUB_X - CORRIDOR_HALF_WIDTH, 1.75f, HUB_Z + 24.0f, 0.2f, 3.5f, 32.0f, true);
        addBox(HUB_X + CORRIDOR_HALF_WIDTH, 1.75f, HUB_Z + 24.0f, 0.2f, 3.5f, 32.0f, true);

        addBox(HUB_X - 8.0f, 1.75f, HUB_Z + CORRIDOR_HALF_WIDTH, 16.0f, 3.5f, 0.2f, true);
        addBox(HUB_X - 8.0f, 1.75f, HUB_Z - CORRIDOR_HALF_WIDTH, 16.0f, 3.5f, 0.2f, true);
    }

    private void addBox(float centerX, float centerY, float centerZ, float width, float height, float depth, boolean addCollider) {
        Mesh mesh = createCubeMesh(width, height, depth);
        sceneMeshes.add(mesh);
        sceneTransforms.add(new Matrix4().setToTranslation(centerX, centerY, centerZ));
        if (addCollider) {
            addCollider(centerX, centerY, centerZ, width, height, depth);
        }
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

    @Override
    public void render(float delta) {
        float clampedDelta = Math.max(0.0f, delta);
        elapsedSeconds += clampedDelta;
        revealedFlashRemaining = Math.max(0.0f, revealedFlashRemaining - clampedDelta);
        inventoryFullMessageRemaining = Math.max(0.0f, inventoryFullMessageRemaining - clampedDelta);

        handleRuntimeInput();
        updateGameplaySystems(clampedDelta);
        updateCameraFov(clampedDelta);
        applyBreathingCameraOffsets(clampedDelta);

        renderSceneToFbo();
        renderPostProcessToBackBuffer();
        renderHudOverlays();
        restoreCameraAfterBreathing();

        SoundPropagationZone soundZone = findSoundPropagationZone();
        if (soundZone != null) {
            float flashAlpha = revealedFlashRemaining > 0.0f
                ? MathUtils.clamp(revealedFlashRemaining / NODE_FLASH_DURATION, 0.0f, 1.0f)
                : 0.0f;
            soundZone.setSoundData(graphNodePositions, lastRevealedNodeIds, flashAlpha, playerPos);
        }

        if (activeZone != null) {
            activeZone.render(camera);
        }
    }

    private SoundPropagationZone findSoundPropagationZone() {
        for (TestZone zone : zones) {
            if (zone instanceof SoundPropagationZone) {
                return (SoundPropagationZone) zone;
            }
        }
        return null;
    }

    private void handleRuntimeInput() {
        boolean ctrlPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            if (ctrlPressed) {
                showBlindRadiusDebug = !showBlindRadiusDebug;
            } else {
                showGraphDebug = !showGraphDebug;
            }
        }

        boolean reloadPressed =
            Gdx.input.isKeyJustPressed(Input.Keys.R) &&
            (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
        if (reloadPressed) {
            bodyCamSettings = BodyCamSettingsStore.loadOrDefault("config/body_cam_settings.json");
            blindEffectConfig = BlindEffectConfigStore.loadOrDefault("config/blind_effect_config.json");
            blindEffectController.reloadConfig(blindEffectConfig);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            bodyCamSettings.enabled = !bodyCamSettings.enabled;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            emitClapShoutPulse(false);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            emitClapShoutPulse(false);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            toggleMicInput();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            if (Gdx.app.getApplicationListener() instanceof Game game) {
                game.setScreen(new PlayerTestScreen());
                return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) {
            if (Gdx.app.getApplicationListener() instanceof Game game) {
                game.setScreen(new UniversalTestScene());
                return;
            }
        }
    }

    private void updateGameplaySystems(float deltaSeconds) {
        playerController.update(deltaSeconds);
        handleCarryAndInventoryInput(deltaSeconds);
        carrySystem.update(deltaSeconds);

        updateActiveZone(deltaSeconds);

        playerFeatureExtractor.update(deltaSeconds, playerController);
        playerFootstepSoundEmitter.update(deltaSeconds, elapsedSeconds);
        soundPropagationOrchestrator.update(deltaSeconds);
        handleMicInput(deltaSeconds);

        playerController.getPosition(playerPos);
        autoCollectNearbyConsumables();
        spatialCueController.setListenerNode(findNearestNodeId(playerPos), playerPos);

        lastSoundIntensity = Math.max(0f, lastSoundIntensity - (deltaSeconds * 0.9f));
        panicModel.setThreatDistanceMeters(playerPos.dst(HUB_X, EYE_HEIGHT, HUB_Z));
        panicModel.setLoudSoundIntensity(lastSoundIntensity);
        panicModel.setHealth(100f, 100f);
        panicModel.update(deltaSeconds);
        blindEffectController.update(deltaSeconds);
    }

    private void toggleMicInput() {
        if (micEnabled) {
            micEnabled = false;
            if (realtimeMicSystem != null) {
                realtimeMicSystem.stop();
            }
            return;
        }

        if (realtimeMicSystem == null) {
            return;
        }

        try {
            realtimeMicSystem.start(16000, 1024);
            micEnabled = realtimeMicSystem.isActive();
        } catch (Exception ignored) {
            micEnabled = false;
        }
    }

    private void updateActiveZone(float deltaSeconds) {
        playerController.getPosition(playerPos);

        TestZone nearest = null;
        float nearestDistance2 = Float.POSITIVE_INFINITY;
        for (TestZone zone : zones) {
            float dist2 = zone.getCenter().dst2(playerPos);
            float maxDist = zone.getActivationRadius();
            if (dist2 <= (maxDist * maxDist) && dist2 < nearestDistance2) {
                nearestDistance2 = dist2;
                nearest = zone;
            }
        }

        if (nearest != null && nearest != activeZone) {
            if (activeZone != null) {
                activeZone.onExit();
            }
            activeZone = nearest;
            activeZone.onEnter();
        }

        if (physicsWorld != null) {
            physicsWorld.stepSimulation(deltaSeconds, 5, 1f / 60f);
            processBulletImpacts();
            processDeferredBreaks();
            syncCarriableTransforms();
        }

        if (activeZone != null) {
            activeZone.update(deltaSeconds);
        }
    }

    private void updateCameraFov(float delta) {
        float targetFov = playerController.getMovementState() == MovementState.RUN ? RUN_FOV : BASE_FOV;
        float alpha = MathUtils.clamp(delta * FOV_LERP_SPEED, 0.0f, 1.0f);
        currentFov = MathUtils.lerp(currentFov, targetFov, alpha);
        camera.fieldOfView = currentFov;
        camera.update();
    }

    private void handleMicInput(float delta) {
        micPulseCooldownRemaining = Math.max(0f, micPulseCooldownRemaining - delta);

        if (!micEnabled || realtimeMicSystem == null || !realtimeMicSystem.isActive()) {
            lastMicSpeakingActive = false;
            lastMicLevelNormalized = Math.max(0f, lastMicLevelNormalized - (delta * 2.0f));
            pushMicLevelSample(lastMicLevelNormalized);
            return;
        }

        RealtimeMicSystem.Frame frame = realtimeMicSystem.update(delta);
        if (frame == null) {
            frame = RealtimeMicSystem.Frame.silent();
        }
        lastMicSpeakingActive = frame.speakingActive();
        float rms = frame.sample() == null ? 0f : frame.sample().rms();
        lastMicLevelNormalized = MathUtils.clamp(rms / (MIC_THRESHOLD_RMS * 2.0f), 0f, 1f);
        pushMicLevelSample(lastMicLevelNormalized);

        if (frame == null || !frame.shouldEmitSignal()) {
            return;
        }

        emitClapShoutPulse(true);
    }

    private void emitClapShoutPulse(boolean useCooldown) {
        if (useCooldown && micPulseCooldownRemaining > 0f) {
            return;
        }
        if (useCooldown) {
            micPulseCooldownRemaining = MIC_PULSE_COOLDOWN;
        }

        Vector3 pulsePos = new Vector3(camera.position);
        String sourceNode = findNearestNodeId(pulsePos);
        SoundEventData eventData = new SoundEventData(
            SoundEvent.CLAP_SHOUT,
            sourceNode,
            pulsePos,
            0.9f,
            elapsedSeconds
        );

        PropagationResult result = soundPropagationOrchestrator.emitSoundEvent(eventData, elapsedSeconds);
        cacheRevealedNodesForFlash(result);
    }

    private void renderSceneToFbo() {
        bodyCamFrameBuffer.beginScenePass();
        Gdx.gl.glViewport(0, 0, bodyCamFrameBuffer.width(), bodyCamFrameBuffer.height());
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderWorldMeshes();
        if (showBlindRadiusDebug) {
            renderBlindRadiusDebug3d();
        }
        renderPlayerViewModel();

        bodyCamFrameBuffer.endScenePass();
    }

    private void renderWorldMeshes() {
        worldShader.bind();
        worldShader.setUniformMatrix("u_projViewTrans", camera.combined);
        worldShader.setUniformf("u_cameraPos", camera.position);
        worldShader.setUniformf("u_lightDirection", 0.35f, 1.0f, 0.2f);
        worldShader.setUniformf("u_baseColor", 0.68f, 0.74f, 0.80f);
        worldShader.setUniformf("u_ambientColor", 0.04f, 0.04f, 0.05f);
        worldShader.setUniformf("u_shadowColor", 0.08f, 0.09f, 0.12f);
        worldShader.setUniformf("u_shadowStrength", 0.5f);
        worldShader.setUniformf("u_fogColor", 0.06f, 0.07f, 0.10f);
        worldShader.setUniformf("u_fogStart", 1.0f);
        worldShader.setUniformf("u_fogEnd", 4.0f);
        blindFogUniformUpdater.updateBlindUniforms(
            worldShader,
            blindEffectController.fogStartMeters(),
            blindEffectController.fogEndMeters(),
            blindEffectController.fogStrength(),
            blindEffectController.fogColor()
        );
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
        blindFogUniformUpdater.updateBlindUniforms(
            playerShaderProgram,
            blindEffectController.fogStartMeters(),
            blindEffectController.fogEndMeters(),
            blindEffectController.fogStrength(),
            blindEffectController.fogColor()
        );
        playerViewModelMesh.render(playerShaderProgram, GL20.GL_TRIANGLES);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    private void renderBlindRadiusDebug3d() {
        float radius = blindEffectController.visibilityMeters();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.98f, 0.84f, 0.3f, 0.95f);
        int segments = 28;
        float previousX = playerPos.x + radius;
        float previousZ = playerPos.z;
        float y = Math.max(0.05f, playerPos.y);
        for (int i = 1; i <= segments; i++) {
            float angle = (i * 360f) / segments;
            float x = playerPos.x + MathUtils.cosDeg(angle) * radius;
            float z = playerPos.z + MathUtils.sinDeg(angle) * radius;
            shapeRenderer.line(previousX, y, previousZ, x, y, z);
            previousX = x;
            previousZ = z;
        }
        shapeRenderer.end();
    }

    private void renderPostProcessToBackBuffer() {
        bodyCamSettings.validate();
        bodyCamVisualizer.renderToBackBuffer(elapsedSeconds, bodyCamSettings);
    }

    private void renderHudOverlays() {
        diagnosticOverlay.update();

        shapeRenderer.setProjectionMatrix(hudProjection);
        renderCrosshair();
        renderInventoryBar();
        drawControlsLegend();
        if (showGraphDebug) {
            renderAcousticGraphOverlay();
        }

        drawInteractionPrompt();
        diagnosticOverlay.draw(
            hudBatch,
            hudFont,
            shapeRenderer,
            hudProjection,
            activeZone,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            micRmsHistory,
            micHistoryCursor,
            lastMicLevelNormalized,
            playerController.getStamina() / Math.max(1f, playerController.getMaxStamina()),
            playerController.getMovementState(),
            micEnabled && realtimeMicSystem != null && realtimeMicSystem.isActive()
        );
    }

    private void drawInteractionPrompt() {
        String promptText = null;
        if (carrySystem.isHoldingItem()) {
            promptText = "[RMB] Throw   [F] Drop   [TAB] Stash";
        } else {
            CarriableItem nearestItem = findNearestFacingCarriable();
            if (nearestItem != null) {
                promptText = "[F] " + nearestItem.definition().displayName();
            } else {
                ConsumablePickup pickup = findNearestFacingConsumable();
                if (pickup != null) {
                    promptText = "[F] " + ItemDefinition.create(pickup.itemType).displayName();
                }
            }
        }

        if (promptText == null) {
            return;
        }

        float centerX = Gdx.graphics.getWidth() * 0.5f;
        float centerY = (Gdx.graphics.getHeight() * 0.5f) - 26f;
        float x = centerX - (promptText.length() * 2.8f);
        hudBatch.begin();
        hudFont.setColor(0.86f, 0.94f, 0.98f, 0.92f);
        hudFont.draw(hudBatch, promptText, x, centerY);
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        hudBatch.end();
    }

    private CarriableItem findNearestFacingCarriable() {
        CarriableItem nearest = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;
        tmpItemDirection.set(camera.direction).nor();

        for (CarriableItem carriableItem : worldCarriableItems) {
            if (carriableItem.isBroken()) {
                continue;
            }

            tmpToItem.set(carriableItem.worldPosition()).sub(camera.position);
            float distanceSquared = tmpToItem.len2();
            if (distanceSquared <= 0.0001f || distanceSquared > (PICKUP_RANGE * PICKUP_RANGE)) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            tmpToItem.scl(1f / distance);
            float facingDot = tmpItemDirection.dot(tmpToItem);
            if (facingDot < INTERACTION_FACING_DOT) {
                continue;
            }

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = carriableItem;
            }
        }

        return nearest;
    }

    private ConsumablePickup findNearestFacingConsumable() {
        ConsumablePickup nearest = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;
        tmpItemDirection.set(camera.direction).nor();

        for (ConsumablePickup pickup : worldConsumablePickups) {
            if (pickup.collected) {
                continue;
            }

            tmpToItem.set(pickup.position).sub(camera.position);
            float distanceSquared = tmpToItem.len2();
            if (distanceSquared <= 0.0001f || distanceSquared > (PICKUP_RANGE * PICKUP_RANGE)) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            tmpToItem.scl(1f / distance);
            float facingDot = tmpItemDirection.dot(tmpToItem);
            if (facingDot < INTERACTION_FACING_DOT) {
                continue;
            }

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = pickup;
            }
        }

        return nearest;
    }

    private void pushMicLevelSample(float normalizedLevel) {
        micRmsHistory[micHistoryCursor] = MathUtils.clamp(normalizedLevel, 0f, 1f);
        micHistoryCursor = (micHistoryCursor + 1) % micRmsHistory.length;
    }

    private void applyBreathingCameraOffsets(float deltaSeconds) {
        if (breathingApplied) {
            restoreCameraAfterBreathing();
        }

        float staminaNormalized = playerController.getStamina() / Math.max(1f, playerController.getMaxStamina());
        cameraBreathingController.update(deltaSeconds, playerController.getMovementState(), staminaNormalized);

        appliedBreathingY = cameraBreathingController.verticalOffset();
        appliedBreathingRollDeg = cameraBreathingController.rollDegrees();
        if (Math.abs(appliedBreathingY) <= 0.00001f && Math.abs(appliedBreathingRollDeg) <= 0.00001f) {
            return;
        }

        tmpBaseCameraUp.set(camera.up);
        camera.position.y += appliedBreathingY;
        if (Math.abs(appliedBreathingRollDeg) > 0.00001f) {
            tmpBreathingRotation.setToRotation(camera.direction, appliedBreathingRollDeg);
            camera.up.mul(tmpBreathingRotation).nor();
        }
        camera.update();
        breathingApplied = true;
    }

    private void restoreCameraAfterBreathing() {
        if (!breathingApplied) {
            return;
        }

        camera.position.y -= appliedBreathingY;
        camera.up.set(tmpBaseCameraUp).nor();
        camera.update();

        appliedBreathingY = 0f;
        appliedBreathingRollDeg = 0f;
        breathingApplied = false;
    }

    private void renderCrosshair() {
        float centerX = Gdx.graphics.getWidth() * 0.5f;
        float centerY = Gdx.graphics.getHeight() * 0.5f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.95f, 0.95f, 0.95f, 0.95f);

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

            shapeRenderer.setColor(slotIndex == inventorySystem.getActiveSlotIndex() ? 0.95f : 0.28f, slotIndex == inventorySystem.getActiveSlotIndex() ? 0.72f : 0.30f, slotIndex == inventorySystem.getActiveSlotIndex() ? 0.22f : 0.34f, 0.92f);
            shapeRenderer.rect(x, y, slotSize, slotSize);

            if (slotEntry != null) {
                shapeRenderer.setColor(0.24f, 0.74f, 0.66f, 0.9f);
                shapeRenderer.rect(x + 6.0f, y + 6.0f, slotSize - 12.0f, slotSize - 12.0f);
            }
        }

        if (inventoryFullMessageRemaining > 0.0f) {
            float alpha = MathUtils.clamp(inventoryFullMessageRemaining / INVENTORY_FULL_MESSAGE_DURATION, 0.0f, 1.0f);
            shapeRenderer.setColor(0.95f, 0.2f, 0.2f, alpha);
            shapeRenderer.rect(startX + (totalWidth * 0.2f), y + slotSize + 9.0f, totalWidth * 0.6f, 10.0f);
        }
        shapeRenderer.end();
    }

    private void drawControlsLegend() {
        String line1 = "[WASD] Move    [Mouse] Look    [F] Pick up item    [Tab] Stash to inventory";
        String line2 = "[1-4] Slots    [Q] Drop item   [E] Use item        [Right-click] Throw";
        String line3 = "[X/V] Sonar pulse (keyboard)   [M] Toggle mic      [G] Toggle graph debug";
        String line4 = "[B] Toggle VHS shader          [Ctrl+G] Blind radius debug";
        String line5 = "[F9] -> PlayerTestScreen       [F10] -> UniversalTestScene";

        float padding = 12.0f;
        float lineHeight = 14.0f;
        float startY = Gdx.graphics.getHeight() - padding;

        float textWidth = Math.max(
            hudFont.getRegion().getRegionWidth(),
            Math.max(line1.length(), Math.max(line2.length(), Math.max(line3.length(), Math.max(line4.length(), line5.length())))) * 6.2f
        );
        float startX = Gdx.graphics.getWidth() - textWidth - 16.0f;

        hudBatch.begin();
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.72f);
        hudFont.draw(hudBatch, line1, startX, startY);
        hudFont.draw(hudBatch, line2, startX, startY - lineHeight);
        hudFont.draw(hudBatch, line3, startX, startY - (lineHeight * 2f));
        hudFont.draw(hudBatch, line4, startX, startY - (lineHeight * 3f));
        hudFont.draw(hudBatch, line5, startX, startY - (lineHeight * 4f));
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        hudBatch.end();
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

            shapeRenderer.setColor(0.2f, 0.95f, 0.6f, 0.9f);
            shapeRenderer.circle(tmpProjected.x, tmpProjected.y, NODE_DOT_RADIUS, 14);

            if (flashAlpha > 0.0f && lastRevealedNodeIds.contains(entry.key, false)) {
                shapeRenderer.setColor(1.0f, 0.92f, 0.2f, flashAlpha);
                shapeRenderer.circle(tmpProjected.x, tmpProjected.y, NODE_FLASH_RADIUS, 16);
            }
        }
        shapeRenderer.end();
    }

    private void spawnInitialCarriableItems() {
        spawnCarriableItem(ItemType.GLASS_BOTTLE, HUB_X + 1.5f, 0.5f, HUB_Z + 1.0f, 0.15f, 0.55f, 0.15f);
        spawnCarriableItem(ItemType.METAL_PIPE, HUB_X - 1.5f, 0.5f, HUB_Z + 1.0f, 0.18f, 0.90f, 0.18f);
        spawnCarriableItem(ItemType.CARDBOARD_BOX, HUB_X, 0.5f, HUB_Z - 2.0f, 0.45f, 0.45f, 0.45f);
    }

    private void spawnInitialConsumablePickups() {
        worldConsumablePickups.add(new ConsumablePickup(ItemType.FLARE, new Vector3(HUB_X + 5f, 0.2f, HUB_Z + 5f)));
        worldConsumablePickups.add(new ConsumablePickup(ItemType.NOISE_DECOY, new Vector3(HUB_X - 5f, 0.2f, HUB_Z - 5f)));
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
        float spawnY = Math.max(y, FLOOR_Y + (height * 0.5f) + ITEM_SPAWN_Y_OFFSET);
        ItemDefinition definition = ItemDefinition.create(itemType);
        CarriableItem carriableItem = new CarriableItem(definition, new Vector3(x, spawnY, z));
        Mesh mesh = createCubeMesh(width, height, depth);
        Matrix4 transform = new Matrix4().setToTranslation(x, spawnY, z);

        btBoxShape collisionShape = new btBoxShape(new Vector3(width * 0.5f, height * 0.5f, depth * 0.5f));
        btDefaultMotionState motionState = new btDefaultMotionState(new Matrix4().setToTranslation(x, spawnY, z));
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
                tmpItemDirection.set(camera.direction).nor();
                thrownItem.setWorldPosition(tmpDropPosition.set(camera.position).mulAdd(tmpItemDirection, 0.8f).add(0f, 0.1f, 0f));
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
        if (tryPickupNearestConsumable()) {
            return;
        }

        CarriableItem nearestCarriable = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;

        for (CarriableItem carriableItem : worldCarriableItems) {
            if (carriableItem.isBroken()) {
                continue;
            }

            tmpToItem.set(carriableItem.worldPosition()).sub(camera.position);
            float distanceSquared = tmpToItem.len2();
            if (distanceSquared > (PICKUP_RANGE * PICKUP_RANGE) || distanceSquared <= 0.0001f) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            tmpToItem.scl(1.0f / distance);
            float facingDot = tmpItemDirection.set(camera.direction).nor().dot(tmpToItem);
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

    private boolean tryPickupNearestConsumable() {
        ConsumablePickup nearest = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;

        for (ConsumablePickup pickup : worldConsumablePickups) {
            if (pickup.collected) {
                continue;
            }

            float distanceSquared = pickup.position.dst2(camera.position);
            if (distanceSquared > (PICKUP_RADIUS * PICKUP_RADIUS)) {
                continue;
            }

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = pickup;
            }
        }

        if (nearest == null) {
            return false;
        }

        if (nearest.itemType == ItemType.FLARE && countInventoryItem(ItemType.FLARE) >= MAX_FLARE_COUNT) {
            inventoryFullMessageRemaining = INVENTORY_FULL_MESSAGE_DURATION;
            return true;
        }

        boolean added = inventorySystem.addItem(ItemDefinition.create(nearest.itemType));
        if (added) {
            nearest.collected = true;
        } else {
            inventoryFullMessageRemaining = INVENTORY_FULL_MESSAGE_DURATION;
        }
        return true;
    }

    private void autoCollectNearbyConsumables() {
        for (ConsumablePickup pickup : worldConsumablePickups) {
            if (pickup.collected) {
                continue;
            }

            float distanceSquared = pickup.position.dst2(playerPos);
            if (distanceSquared > (PICKUP_RADIUS * PICKUP_RADIUS)) {
                continue;
            }

            if (pickup.itemType == ItemType.FLARE && countInventoryItem(ItemType.FLARE) >= MAX_FLARE_COUNT) {
                continue;
            }

            boolean added = inventorySystem.addItem(ItemDefinition.create(pickup.itemType));
            if (added) {
                pickup.collected = true;
            }
        }
    }

    private int countInventoryItem(ItemType itemType) {
        int total = 0;
        for (int slotIndex = 0; slotIndex < InventorySystem.SLOT_COUNT; slotIndex++) {
            InventorySystem.InventorySlotEntry slotEntry = inventorySystem.getSlotEntry(slotIndex);
            if (slotEntry != null && slotEntry.itemDefinition().itemType() == itemType) {
                total += slotEntry.count();
            }
        }
        return total;
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
            blindEffectController.triggerFlareReveal();
            emitItemEvent(SoundEvent.OBJECT_DROP_OR_BREAK, tmpDropPosition.set(camera.position), 0.30f);
        }

        inventorySystem.consumeActiveItem();
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

    private void syncCarriableTransforms() {
        for (CarriableItem carriableItem : worldCarriableItems) {
            if (carriableItem.isBroken()) {
                continue;
            }

            Matrix4 transform = carriableTransforms.get(carriableItem);
            if (transform == null) {
                continue;
            }

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

    private void cacheGraphNodePositions() {
        graphNodePositions.clear();
        for (io.github.superteam.resonance.sound.GraphNode graphNode : acousticGraphEngine.getNodes()) {
            graphNodePositions.put(graphNode.id(), graphNode.position());
        }
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

    private void updateHudProjection(int width, int height) {
        hudProjection.setToOrtho2D(0.0f, 0.0f, width, height);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        updateHudProjection(width, height);
        recreateBodyCamFrameBuffers(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    @Override
    public void dispose() {
        if (realtimeMicSystem != null) {
            realtimeMicSystem.stop();
        }

        for (Mesh mesh : sceneMeshes) {
            if (mesh != null) {
                mesh.dispose();
            }
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
        if (bodyCamShaderLoader != null) {
            bodyCamShaderLoader.dispose();
        }
        if (bodyCamFrameBuffer != null) {
            bodyCamFrameBuffer.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (hudBatch != null) {
            hudBatch.dispose();
        }
        if (hudFont != null) {
            hudFont.dispose();
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

    private static final class ConsumablePickup {
        private final ItemType itemType;
        private final Vector3 position;
        private boolean collected;

        private ConsumablePickup(ItemType itemType, Vector3 position) {
            this.itemType = itemType;
            this.position = new Vector3(position);
        }
    }
}

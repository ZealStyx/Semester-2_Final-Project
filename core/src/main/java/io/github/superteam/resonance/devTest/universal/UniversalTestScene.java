package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
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
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
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
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import io.github.superteam.resonance.director.DirectorController;
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
import io.github.superteam.resonance.particles.ParticleDefinition;
import io.github.superteam.resonance.particles.ParticleEffect;
import io.github.superteam.resonance.particles.ParticleEmitter;
import io.github.superteam.resonance.particles.ParticleManager;
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
import io.github.superteam.resonance.sound.viz.AcousticBounce3DVisualizer;
import io.github.superteam.resonance.sound.viz.AcousticBounceConfig;
import io.github.superteam.resonance.sound.viz.AcousticBounceConfigStore;
import io.github.superteam.resonance.sound.viz.SoundPulseShaderRenderer;
import io.github.superteam.resonance.sound.DijkstraPathfinder;
import io.github.superteam.resonance.sound.GraphPopulator;
import io.github.superteam.resonance.sound.PhysicsNoiseEmitter;
import io.github.superteam.resonance.sound.PropagationResult;
import io.github.superteam.resonance.sound.RealtimeMicSystem;
import io.github.superteam.resonance.sound.SonarRenderer;
import io.github.superteam.resonance.sound.SoundBalancingConfigStore;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import io.github.superteam.resonance.sound.SpatialCueController;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * High-fidelity universal test environment with rendering, physics, sound, and item systems.
 */
public final class UniversalTestScene extends ScreenAdapter {
    private static final float HUB_X = 40.0f;
    private static final float HUB_Z = 40.0f;
    private static final float FLOOR_Y = 0.02f;
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
    private static final float ITEM_DESTROYED_MESSAGE_DURATION = 2.0f;

    private static final float VIEW_MODEL_SCALE = 0.18f;
    private static final float VIEW_MODEL_OFFSET_RIGHT = 0.28f;
    private static final float VIEW_MODEL_OFFSET_UP = -0.24f;
    private static final float VIEW_MODEL_OFFSET_FORWARD = 0.45f;

    private static final float CROSSHAIR_HALF_SIZE = 7.0f;
    private static final float CROSSHAIR_GAP = 3.0f;
    private static final float NODE_FLASH_DURATION = 0.55f;
    private static final float GRAPH_NODE_MARKER_RADIUS = 0.16f;
    private static final float GRAPH_NODE_FLASH_RADIUS = 0.28f;

    private static final float MIC_THRESHOLD_RMS = 0.08f;
    private static final float ITEM_SPAWN_Y_OFFSET = 0.02f;
    private static final int MIC_HISTORY_SIZE = 128;
    private static final float NODE_GRID_CELL_SIZE = 4.0f;

    private static boolean bulletInitialized;

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
    private final ObjectMap<Long, Array<NodeReference>> graphNodeSpatialBuckets = new ObjectMap<>();
    private final Array<NodeReference> graphNodeReferences = new Array<>();
    private final ObjectMap<String, Float> sonarRevealAlphaByNodeId = new ObjectMap<>();
    private final Array<String> lastRevealedNodeIds = new Array<>();

    private final Array<ConsumablePickup> worldConsumablePickups = new Array<>();
    private final Array<CarriableItem> worldCarriableItems = new Array<>();
    private final Array<CarriableItem> carriableItemRegistry = new Array<>();
    private final ObjectMap<CarriableItem, Mesh> carriableMeshes = new ObjectMap<>();
    private final ObjectMap<CarriableItem, Matrix4> carriableTransforms = new ObjectMap<>();
    private final ObjectMap<CarriableItem, btRigidBody> carriableRigidBodies = new ObjectMap<>();
    private final ObjectMap<CarriableItem, btCollisionShape> carriableCollisionShapes = new ObjectMap<>();
    private final ObjectMap<CarriableItem, btDefaultMotionState> carriableMotionStates = new ObjectMap<>();
    private final ObjectMap<CarriableItem, Integer> carriableRegistryIndices = new ObjectMap<>();
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
    private final Vector3 tmpImpactVelocity = new Vector3();
    private final Vector3 tmpInertia = new Vector3();
    private final Vector3 tmpProjected = new Vector3();
    private final Vector3 tmpBaseCameraUp = new Vector3();
    private final Vector3 tmpRayDirection = new Vector3();
    private final Vector3 tmpRayEnd = new Vector3();
    private final Vector3 tmpViewModelColor = new Vector3(0.12f, 0.78f, 0.74f);
    private final Vector3 lastNearestNodeQueryPosition = new Vector3(Float.NaN, Float.NaN, Float.NaN);

    private ShaderProgram worldShader;
    private ShaderProgram playerShaderProgram;
    private ShaderProgram particleShaderProgram;
    private BodyCamVHSShaderLoader bodyCamShaderLoader;
    private BodyCamPassFrameBuffer bodyCamFrameBuffer;
    private BodyCamVHSVisualizer bodyCamVisualizer;
    private BodyCamVHSSettings bodyCamSettings;
    private BlindEffectRevealConfig blindEffectConfig;
    private BlindEffectController blindEffectController;
    private final ParticleManager particleManager = new ParticleManager();
    private SoundPulseShaderRenderer soundPulseShaderRenderer;
    private AcousticBounceConfig acousticBounceConfig;
    private AcousticBounce3DVisualizer acousticBounce3DVisualizer;
    private final DirectorController directorController;
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
    private SoundPropagationZone cachedSoundPropagationZone;
    private final float[] micRmsHistory = new float[MIC_HISTORY_SIZE];

    private float elapsedSeconds;
    private float currentFov = BASE_FOV;
    private float revealedFlashRemaining;
    private float lastSoundIntensity;
    private float inventoryFullMessageRemaining;
    private float itemDestroyedMessageRemaining;
    private float lastMicLevelNormalized;
    private float appliedBreathingY;
    private float appliedBreathingRollDeg;
    private int micHistoryCursor;
    private int pendingScrollSteps;
    private boolean showGraphDebug;
    private boolean showBlindRadiusDebug;
    private boolean micEnabled;
    private boolean breathingApplied;
    private String lastItemDestroyedMessage;
    private String cachedNearestNodeId = "center";

    public UniversalTestScene() {
        camera = new PerspectiveCamera(BASE_FOV, Math.max(1, Gdx.graphics.getWidth()), Math.max(1, Gdx.graphics.getHeight()));
        camera.position.set(HUB_X, EYE_HEIGHT, HUB_Z);
        camera.lookAt(HUB_X + 1.0f, EYE_HEIGHT, HUB_Z);
        camera.near = 0.05f;
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
        loadParticleShader();
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
        directorController = new DirectorController();

        buildParticleSystems();
        soundPulseShaderRenderer = new SoundPulseShaderRenderer();

        buildProceduralHubScene();
        initializeZones();
        registerZoneColliders();
        playerController.setWorldColliders(worldColliders);
        playerViewModelMesh = createCubeMesh(1.0f, 1.0f, 1.0f);

        initializePhysicsWorld();
        registerStaticWorldBodies();

        carrySystem = new CarrySystem(camera);
        inventorySystem = new InventorySystem();

        acousticGraphEngine = new GraphPopulator().populate(worldColliders);
        cacheGraphNodePositions();
        if (cachedSoundPropagationZone != null) {
            cachedSoundPropagationZone.setNodePositions(graphNodePositions);
        }
        acousticBounceConfig = AcousticBounceConfigStore.loadOrDefault("config/acoustic_bounce_3d_config.json");
        acousticBounce3DVisualizer = new AcousticBounce3DVisualizer(acousticGraphEngine, worldColliders, acousticBounceConfig);

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
            directorController.onSoundEvent(soundEventData, propagationResult);
            blindEffectController.onSoundEvent(soundEventData);
            acousticBounce3DVisualizer.onSoundEvent(soundEventData, propagationResult);
            if (propagationResult != null) {
                cacheRevealedNodesForFlash(propagationResult);
            }
        });
        soundPropagationOrchestrator.registerEnemyListener(directorController);

        playerFootstepSoundEmitter = new PlayerFootstepSoundEmitter(
            playerController,
            soundPropagationOrchestrator,
            this::findNearestNodeId
        );

        playerFeatureExtractor = new PlayerFeatureExtractor();
        playerFeatureExtractor.setLastKnownPosition(camera.position);
        playerFeatureExtractor.addListener(directorController);

        spawnInitialCarriableItems();
        spawnInitialConsumablePickups();

        realtimeMicSystem = new RealtimeMicSystem(MIC_THRESHOLD_RMS);

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
            if (zone instanceof SoundPropagationZone soundPropagationZone) {
                cachedSoundPropagationZone = soundPropagationZone;
            }
        }
    }

    private void registerZoneColliders() {
        for (TestZone zone : zones) {
            Array<ColliderDescriptor> colliders = zone.getColliders();
            if (colliders == null || colliders.isEmpty()) {
                continue;
            }

            for (ColliderDescriptor collider : colliders) {
                addCollider(collider.cx(), collider.cy(), collider.cz(), collider.width(), collider.height(), collider.depth());
            }
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

    private void loadParticleShader() {
        ShaderProgram.pedantic = false;
        particleShaderProgram = new ShaderProgram(
            Gdx.files.internal("shaders/vert/particle_shader.vert"),
            Gdx.files.internal("shaders/frag/particle_shader.frag")
        );
        if (!particleShaderProgram.isCompiled()) {
            throw new GdxRuntimeException("Particle shader failed to compile: " + particleShaderProgram.getLog());
        }
    }

    private void buildParticleSystems() {
        ParticleDefinition placeholderDefinition = ParticleDefinition.createDefault();
        placeholderDefinition.emissionRate = 0f;
        placeholderDefinition.emissionDuration = 0f;
        placeholderDefinition.burstMode = false;
        placeholderDefinition.burstLoop = false;
        placeholderDefinition.burstStaggerDuration = 0f;
        placeholderDefinition.sanitize();

        ParticleEffect placeholderEffect = new ParticleEffect("Pulse Placeholder");
        ParticleEmitter placeholderEmitter = new ParticleEmitter(placeholderDefinition);
        placeholderEffect.addEmitter(placeholderEmitter);
        placeholderEffect.setActive(false);
        particleManager.addEffect(placeholderEffect);
    }

    private void recreateBodyCamFrameBuffers(int width, int height) {
        bodyCamFrameBuffer.resize(width, height);
    }

    private void initializePhysicsWorld() {
        if (!bulletInitialized) {
            Bullet.init();
            bulletInitialized = true;
        }

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
        itemDestroyedMessageRemaining = Math.max(0.0f, itemDestroyedMessageRemaining - clampedDelta);

        if (handleRuntimeInput()) {
            return;
        }
        updateGameplaySystems(clampedDelta);
        updateCameraFov(clampedDelta);
        applyBreathingCameraOffsets(clampedDelta);
        particleManager.update(clampedDelta, camera);
        acousticBounce3DVisualizer.update(clampedDelta);

        renderSceneToFbo(clampedDelta);
        renderPostProcessToBackBuffer();
        renderHudOverlays();
        restoreCameraAfterBreathing();

        if (cachedSoundPropagationZone != null) {
            float flashAlpha = revealedFlashRemaining > 0.0f
                ? MathUtils.clamp(revealedFlashRemaining / NODE_FLASH_DURATION, 0.0f, 1.0f)
                : 0.0f;
            cachedSoundPropagationZone.setSoundData(lastRevealedNodeIds, flashAlpha, playerPos);
        }

        if (activeZone != null) {
            activeZone.render(camera);
        }
    }

    private boolean handleRuntimeInput() {
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
            acousticBounceConfig.geometricLayer.renderRays = !acousticBounceConfig.geometricLayer.renderRays;
            acousticBounce3DVisualizer.reloadConfig(acousticBounceConfig);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            bodyCamSettings.enabled = !bodyCamSettings.enabled;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            emitSoundEventPulse(SoundEvent.CLAP_SHOUT, SoundEvent.CLAP_SHOUT.defaultBaseIntensity());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            emitSoundEventPulse(SoundEvent.CLAP_SHOUT, SoundEvent.CLAP_SHOUT.defaultBaseIntensity());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            toggleMicInput();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            blindEffectController.triggerFlareReveal();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            if (Gdx.app.getApplicationListener() instanceof Game game) {
                Screen previous = game.getScreen();
                game.setScreen(new PlayerTestScreen());
                if (previous != null && previous != game.getScreen()) {
                    Gdx.app.postRunnable(previous::dispose);
                }
                return true;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) {
            if (Gdx.app.getApplicationListener() instanceof Game game) {
                Screen previous = game.getScreen();
                game.setScreen(new UniversalTestScene());
                if (previous != null && previous != game.getScreen()) {
                    Gdx.app.postRunnable(previous::dispose);
                }
                return true;
            }
        }

        return false;
    }

    private void updateGameplaySystems(float deltaSeconds) {
        playerController.update(deltaSeconds);
        if (playerController.consumeJumpTriggered()) {
            emitSoundSourceEvent(SoundEvent.JUMP, SoundEvent.JUMP.defaultBaseIntensity());
        }
        handleCarryAndInventoryInput(deltaSeconds);
        carrySystem.update(deltaSeconds);

        stepPhysicsWorld(deltaSeconds);
        updateActiveZone(deltaSeconds);

        playerFeatureExtractor.update(deltaSeconds, playerController);
        playerFootstepSoundEmitter.update(deltaSeconds, elapsedSeconds);
        soundPropagationOrchestrator.update(deltaSeconds);
        updateSonarRevealSnapshot();
        handleMicInput(deltaSeconds);

        playerController.getPosition(playerPos);
        String listenerNodeId = findNearestNodeId(playerPos);
        spatialCueController.setListenerNode(listenerNodeId, playerPos);

        lastSoundIntensity = Math.max(0f, lastSoundIntensity - (deltaSeconds * 0.9f));
        directorController.update(deltaSeconds);
        panicModel.setThreatDistanceMeters(30f);
        panicModel.setLoudSoundIntensity(lastSoundIntensity);
        panicModel.setHealth(100f, 100f);
        panicModel.update(deltaSeconds);
        blindEffectController.update(deltaSeconds);
    }

    private void stepPhysicsWorld(float deltaSeconds) {
        if (physicsWorld == null) {
            return;
        }

        physicsWorld.stepSimulation(deltaSeconds, 5, 1f / 60f);
        processBulletImpacts();
        processDeferredBreaks();
        syncCarriableTransforms();
    }

    private void toggleMicInput() {
        if (micEnabled) {
            micEnabled = false;
            if (realtimeMicSystem != null) {
                realtimeMicSystem.stop();
                Gdx.app.log("Mic", "Disabled");
            }
            return;
        }

        if (realtimeMicSystem == null) {
            return;
        }

        if (startMicCaptureWithFallback()) {
            micEnabled = true;
            Gdx.app.log("Mic", "Enabled");
            return;
        }

        micEnabled = false;
        Gdx.app.log("Mic", "Failed to enable microphone input");
    }

    private boolean startMicCaptureWithFallback() {
        int[] sampleRates = {44100, 22050, 16000};
        for (int sampleRate : sampleRates) {
            try {
                realtimeMicSystem.start(sampleRate, 1024);
                if (realtimeMicSystem.isActive()) {
                    Gdx.app.log("Mic", "Capture started at " + sampleRate + " Hz");
                    return true;
                }
            } catch (Exception exception) {
                Gdx.app.log("Mic", "Capture start failed at " + sampleRate + " Hz: " + exception.getMessage());
            }
        }
        return false;
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
        if (!micEnabled || realtimeMicSystem == null || !realtimeMicSystem.isActive()) {
            lastMicLevelNormalized = Math.max(0f, lastMicLevelNormalized - (delta * 2.0f));
            pushMicLevelSample(lastMicLevelNormalized);
            return;
        }

        RealtimeMicSystem.Frame frame = realtimeMicSystem.update(delta);
        if (frame == null) {
            frame = RealtimeMicSystem.Frame.silent();
        }
        float normalizedLevel = frame.sample() == null ? 0f : frame.sample().normalizedLevel();
        lastMicLevelNormalized = MathUtils.clamp(normalizedLevel, 0f, 1f);
        pushMicLevelSample(lastMicLevelNormalized);

        if (!frame.shouldEmitSignal()) {
            return;
        }

        float voiceIntensity = MathUtils.lerp(0.30f, 1.15f, lastMicLevelNormalized);
        emitSoundEventPulse(SoundEvent.MIC_INPUT, voiceIntensity);
    }

    private void emitSoundEventPulse(SoundEvent eventType, float intensity) {
        Vector3 pulsePos = new Vector3(camera.position);
        String sourceNode = findNearestNodeId(pulsePos);
        SoundEventData eventData = new SoundEventData(
            eventType,
            sourceNode,
            pulsePos,
            Math.max(0f, intensity),
            elapsedSeconds
        );

        fireSoundPulseVisual(pulsePos, eventData.baseIntensity());
        PropagationResult result = soundPropagationOrchestrator.emitSoundEvent(eventData, elapsedSeconds);
        cacheRevealedNodesForFlash(result);
    }

    private void emitSoundSourceEvent(SoundEvent eventType, float intensity) {
        Vector3 sourcePosition = new Vector3(camera.position);
        String sourceNode = findNearestNodeId(sourcePosition);
        SoundEventData eventData = new SoundEventData(
            eventType,
            sourceNode,
            sourcePosition,
            Math.max(0f, intensity),
            elapsedSeconds
        );

        soundPropagationOrchestrator.emitSoundEvent(eventData, elapsedSeconds);
    }

    private void renderSceneToFbo(float deltaSeconds) {
        bodyCamFrameBuffer.beginScenePass();
        Gdx.gl.glViewport(0, 0, bodyCamFrameBuffer.width(), bodyCamFrameBuffer.height());
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderWorldMeshes();
        if (soundPulseShaderRenderer != null) {
            soundPulseShaderRenderer.render(camera, deltaSeconds);
        }
        if (showGraphDebug) {
            renderAcousticGraphWorldMarkers();
            acousticBounce3DVisualizer.render(camera);
        }
        particleManager.render(particleShaderProgram, camera);
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
            if (carriableItem.state() != CarriableItem.ItemState.WORLD) {
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
        if (!carrySystem.isHoldingItem()) {
            return;
        }

        CarriableItem heldItem = carrySystem.heldItem();
        if (heldItem == null || heldItem.state() != CarriableItem.ItemState.CARRIED) {
            return;
        }

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
        resolveViewModelColor(heldItem, tmpViewModelColor);
        playerShaderProgram.setUniformf("u_baseColor", tmpViewModelColor);
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

    private void drawCircleXZ(Vector3 center, float radius, int segments) {
        int clampedSegments = Math.max(8, segments);
        float previousX = center.x + radius;
        float previousZ = center.z;
        for (int i = 1; i <= clampedSegments; i++) {
            float angle = (i * 360f) / clampedSegments;
            float x = center.x + MathUtils.cosDeg(angle) * radius;
            float z = center.z + MathUtils.sinDeg(angle) * radius;
            shapeRenderer.line(previousX, center.y, previousZ, x, center.y, z);
            previousX = x;
            previousZ = z;
        }
    }

    private void renderPostProcessToBackBuffer() {
        bodyCamSettings.validate();
        bodyCamVisualizer.renderToBackBuffer(elapsedSeconds, bodyCamSettings);
    }

    private void renderHudOverlays() {
        diagnosticOverlay.update();

        CarriableItem focusedCarriable = carrySystem.isHoldingItem() ? null : findNearestFacingCarriable();
        ConsumablePickup focusedConsumable = carrySystem.isHoldingItem() || focusedCarriable != null
            ? null
            : findNearestFacingConsumable();

        shapeRenderer.setProjectionMatrix(hudProjection);
        renderCrosshair(focusedCarriable != null || focusedConsumable != null);
        renderInventoryBar();
        drawControlsLegend();

        drawInteractionPrompt(focusedCarriable, focusedConsumable);
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

    private void drawInteractionPrompt(CarriableItem focusedCarriable, ConsumablePickup focusedConsumable) {
        String promptText = null;
        if (carrySystem.isHoldingItem()) {
            promptText = "[RMB] Throw   [F] Drop   [TAB] Stash";
        } else {
            if (focusedCarriable != null) {
                promptText = "[F] Pick up " + focusedCarriable.definition().displayName();
            } else if (focusedConsumable != null) {
                promptText = "[F] Use " + ItemDefinition.create(focusedConsumable.itemType).displayName();
            }
        }

        if (promptText != null) {
            float centerX = Gdx.graphics.getWidth() * 0.5f;
            float centerY = (Gdx.graphics.getHeight() * 0.5f) - 26f;
            float x = centerX - (promptText.length() * 2.8f);
            hudBatch.begin();
            hudFont.setColor(0.86f, 0.94f, 0.98f, 0.92f);
            hudFont.draw(hudBatch, promptText, x, centerY);
            hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
            hudBatch.end();
        }

        if (itemDestroyedMessageRemaining <= 0f || lastItemDestroyedMessage == null || lastItemDestroyedMessage.isBlank()) {
            return;
        }

        float alpha = MathUtils.clamp(itemDestroyedMessageRemaining / ITEM_DESTROYED_MESSAGE_DURATION, 0f, 1f);
        hudBatch.begin();
        hudFont.setColor(0.98f, 0.52f, 0.35f, alpha);
        hudFont.draw(hudBatch, lastItemDestroyedMessage, 16f, 84f);
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        hudBatch.end();
    }

    private CarriableItem findNearestFacingCarriable() {
        CarriableItem raycastHit = findRaycastCarriable();
        if (raycastHit != null) {
            return raycastHit;
        }

        CarriableItem nearest = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;
        tmpItemDirection.set(camera.direction).nor();

        for (CarriableItem carriableItem : worldCarriableItems) {
            if (carriableItem.state() != CarriableItem.ItemState.WORLD) {
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

    private CarriableItem findRaycastCarriable() {
        if (physicsWorld == null || carriableItemRegistry.isEmpty()) {
            return null;
        }

        tmpRayDirection.set(camera.direction).nor();
        tmpRayEnd.set(camera.position).mulAdd(tmpRayDirection, PICKUP_RANGE);

        ClosestRayResultCallback rayResultCallback = new ClosestRayResultCallback(camera.position, tmpRayEnd);
        try {
            physicsWorld.rayTest(camera.position, tmpRayEnd, rayResultCallback);
            if (!rayResultCallback.hasHit()) {
                return null;
            }

            return resolveCarriableFromCollisionObject(rayResultCallback.getCollisionObject());
        } finally {
            rayResultCallback.dispose();
        }
    }

    private CarriableItem resolveCarriableFromCollisionObject(btCollisionObject collisionObject) {
        if (!(collisionObject instanceof btRigidBody)) {
            return null;
        }

        int userValue = collisionObject.getUserValue();
        if (userValue <= 0) {
            return null;
        }

        int registryIndex = userValue - 1;
        if (registryIndex < 0 || registryIndex >= carriableItemRegistry.size) {
            return null;
        }

        CarriableItem carriableItem = carriableItemRegistry.get(registryIndex);
        if (carriableItem == null || carriableItem.state() != CarriableItem.ItemState.WORLD) {
            return null;
        }

        return carriableItem;
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

    private void renderCrosshair(boolean interactableTargetVisible) {
        float centerX = Gdx.graphics.getWidth() * 0.5f;
        float centerY = Gdx.graphics.getHeight() * 0.5f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (interactableTargetVisible) {
            shapeRenderer.setColor(0.98f, 0.90f, 0.25f, 0.98f);
        } else {
            shapeRenderer.setColor(0.95f, 0.95f, 0.95f, 0.95f);
        }

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
        String line4 = "[B] Toggle bounce rays         [N] Toggle VHS shader";
        String line5 = "[F9] -> PlayerTestScreen       [F10] -> UniversalTestScene";
        String line6 = directorController.snapshot().hudLine();

        float padding = 12.0f;
        float lineHeight = 14.0f;
        float startY = Gdx.graphics.getHeight() - padding;

        float textWidth = Math.max(
            hudFont.getRegion().getRegionWidth(),
            Math.max(line1.length(), Math.max(line2.length(), Math.max(line3.length(), Math.max(line4.length(), Math.max(line5.length(), line6.length()))))) * 6.2f
        );
        float startX = Gdx.graphics.getWidth() - textWidth - 16.0f;

        hudBatch.begin();
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.72f);
        hudFont.draw(hudBatch, line1, startX, startY);
        hudFont.draw(hudBatch, line2, startX, startY - lineHeight);
        hudFont.draw(hudBatch, line3, startX, startY - (lineHeight * 2f));
        hudFont.draw(hudBatch, line4, startX, startY - (lineHeight * 3f));
        hudFont.draw(hudBatch, line5, startX, startY - (lineHeight * 4f));
        hudFont.draw(hudBatch, line6, startX, startY - (lineHeight * 5f));
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        hudBatch.end();
    }

    private void renderAcousticGraphWorldMarkers() {
        if (!showGraphDebug) {
            return;
        }

        float flashAlpha = revealedFlashRemaining > 0.0f
            ? MathUtils.clamp(revealedFlashRemaining / NODE_FLASH_DURATION, 0.0f, 1.0f)
            : 0.0f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (ObjectMap.Entry<String, Vector3> entry : graphNodePositions.entries()) {
            tmpProjected.set(entry.value);
            tmpProjected.y += 0.05f;

            float sonarAlpha = sonarRevealAlphaByNodeId.get(entry.key, 0f);
            float markerRadius = GRAPH_NODE_MARKER_RADIUS + (sonarAlpha * 0.12f);
            float markerGreen = 0.72f + (sonarAlpha * 0.23f);
            float markerBlue = 0.45f + (sonarAlpha * 0.35f);
            float markerAlpha = Math.max(0.35f, 0.65f + (sonarAlpha * 0.35f));

            shapeRenderer.setColor(0.2f, markerGreen, markerBlue, markerAlpha);
            drawCircleXZ(tmpProjected, markerRadius, 12);

            if (flashAlpha > 0.0f && lastRevealedNodeIds.contains(entry.key, false)) {
                shapeRenderer.setColor(1.0f, 0.92f, 0.2f, flashAlpha);
                drawCircleXZ(tmpProjected, GRAPH_NODE_FLASH_RADIUS, 16);
            }
        }
        shapeRenderer.end();
    }

    private void updateSonarRevealSnapshot() {
        sonarRevealAlphaByNodeId.clear();
        if (soundPropagationOrchestrator == null) {
            return;
        }

        List<SonarRenderer.SonarRevealView> reveals = soundPropagationOrchestrator.sonarSnapshot();
        for (SonarRenderer.SonarRevealView revealView : reveals) {
            float weightedAlpha = MathUtils.clamp(revealView.alpha() * revealView.intensity(), 0f, 1f);
            Float previous = sonarRevealAlphaByNodeId.get(revealView.nodeId());
            if (previous == null || weightedAlpha > previous) {
                sonarRevealAlphaByNodeId.put(revealView.nodeId(), weightedAlpha);
            }
        }
    }

    private void fireSoundPulseVisual(Vector3 worldPosition, float intensity) {
        if (soundPulseShaderRenderer == null || worldPosition == null) {
            return;
        }
        soundPulseShaderRenderer.fire(worldPosition, intensity);
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
        float smallestDimension = Math.min(width, Math.min(height, depth));
        rigidBody.setCcdMotionThreshold(Math.max(0.05f, smallestDimension * 0.25f));
        rigidBody.setCcdSweptSphereRadius(Math.max(0.05f, smallestDimension * 0.35f));
        physicsWorld.addRigidBody(rigidBody);

        carriableItem.setRigidBody(rigidBody);

        worldCarriableItems.add(carriableItem);
        carriableItemRegistry.add(carriableItem);
        int registryIndex = carriableItemRegistry.size - 1;
        carriableRegistryIndices.put(carriableItem, registryIndex);
        carriableMeshes.put(carriableItem, mesh);
        carriableTransforms.put(carriableItem, transform);
        carriableRigidBodies.put(carriableItem, rigidBody);
        carriableCollisionShapes.put(carriableItem, collisionShape);
        carriableMotionStates.put(carriableItem, motionState);
        rigidBodyToCarriable.put(rigidBody, carriableItem);
        rigidBody.setUserValue(registryIndex + 1);
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
            if (carriableItem.state() != CarriableItem.ItemState.WORLD) {
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
        tmpItemDirection.set(camera.direction).nor();

        for (ConsumablePickup pickup : worldConsumablePickups) {
            if (pickup.collected) {
                continue;
            }

            tmpToItem.set(pickup.position).sub(camera.position);
            float distanceSquared = tmpToItem.len2();
            if (distanceSquared <= 0.0001f || distanceSquared > (PICKUP_RADIUS * PICKUP_RADIUS)) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            tmpToItem.scl(1.0f / distance);
            float facingDot = tmpItemDirection.dot(tmpToItem);
            if (facingDot < INTERACTION_FACING_DOT) {
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
            float maxImpulse = 0f;
            boolean hasImpact = false;
            for (int contactIndex = 0; contactIndex < contactCount; contactIndex++) {
                btManifoldPoint contactPoint = manifold.getContactPoint(contactIndex);
                float appliedImpulse = contactPoint.getAppliedImpulse();
                if (appliedImpulse <= 0f) {
                    continue;
                }

                if (appliedImpulse > maxImpulse) {
                    maxImpulse = appliedImpulse;
                    contactPoint.getPositionWorldOnA(tmpImpulsePoint);
                    hasImpact = true;
                }
            }

            if (!hasImpact) {
                maxImpulse = estimateImpactImpulse(carriableItem);
                hasImpact = maxImpulse > 0f;
                if (hasImpact) {
                    tmpImpactVelocity.set(carriableItem.rigidBody().getLinearVelocity());
                    if (tmpImpactVelocity.len2() > 0f) {
                        tmpImpulsePoint.set(carriableItem.worldPosition()).add(tmpImpactVelocity.nor().scl(0.05f));
                    } else {
                        tmpImpulsePoint.set(carriableItem.worldPosition());
                    }
                }
            }

            if (hasImpact) {
                PropagationResult propagationResult = impactListener.onCarriableImpact(
                    carriableItem,
                    tmpImpulsePoint,
                    maxImpulse,
                    elapsedSeconds
                );
                if (propagationResult != null) {
                    cacheRevealedNodesForFlash(propagationResult);
                }
            }
        }
    }

    private float estimateImpactImpulse(CarriableItem carriableItem) {
        if (carriableItem == null || carriableItem.rigidBody() == null) {
            return 0f;
        }

        tmpImpactVelocity.set(carriableItem.rigidBody().getLinearVelocity());
        float speed = tmpImpactVelocity.len();
        if (speed <= 0.1f) {
            return 0f;
        }

        return Math.min(150f, speed * carriableItem.definition().mass() * 12f);
    }

    private CarriableItem resolveCarriableItem(btCollisionObject body0, btCollisionObject body1) {
        if (body0 instanceof btRigidBody) {
            btRigidBody rigidBody0 = (btRigidBody) body0;
            CarriableItem carriable0 = rigidBodyToCarriable.get(rigidBody0);
            if (carriable0 != null && carriable0.state() == CarriableItem.ItemState.WORLD) {
                return carriable0;
            }
        }

        if (body1 instanceof btRigidBody) {
            btRigidBody rigidBody1 = (btRigidBody) body1;
            CarriableItem carriable1 = rigidBodyToCarriable.get(rigidBody1);
            if (carriable1 != null && carriable1.state() == CarriableItem.ItemState.WORLD) {
                return carriable1;
            }
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
            lastItemDestroyedMessage = breakItem.definition().displayName() + " destroyed";
            itemDestroyedMessageRemaining = ITEM_DESTROYED_MESSAGE_DURATION;
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
        carriableItem.setState(CarriableItem.ItemState.BROKEN);
        carriableItem.markBroken();

        Integer registryIndex = carriableRegistryIndices.remove(carriableItem);
        if (registryIndex != null && registryIndex >= 0 && registryIndex < carriableItemRegistry.size) {
            carriableItemRegistry.set(registryIndex, null);
        }

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

    private void resolveViewModelColor(CarriableItem heldItem, Vector3 outColor) {
        if (heldItem == null || outColor == null) {
            return;
        }

        switch (heldItem.definition().itemType()) {
            case METAL_PIPE:
                outColor.set(0.50f, 0.50f, 0.55f);
                break;
            case GLASS_BOTTLE:
                outColor.set(0.40f, 0.80f, 0.60f);
                break;
            case CARDBOARD_BOX:
                outColor.set(0.60f, 0.45f, 0.30f);
                break;
            case CONCRETE_CHUNK:
                outColor.set(0.55f, 0.55f, 0.50f);
                break;
            default:
                outColor.set(0.12f, 0.78f, 0.74f);
                break;
        }
    }

    private void cacheGraphNodePositions() {
        graphNodePositions.clear();
        graphNodeSpatialBuckets.clear();
        graphNodeReferences.clear();
        for (io.github.superteam.resonance.sound.GraphNode graphNode : acousticGraphEngine.getNodes()) {
            Vector3 nodePosition = graphNode.position();
            graphNodePositions.put(graphNode.id(), nodePosition);

            NodeReference nodeReference = new NodeReference(graphNode.id(), nodePosition);
            graphNodeReferences.add(nodeReference);

            int cellX = worldToCell(nodePosition.x);
            int cellY = worldToCell(nodePosition.y);
            int cellZ = worldToCell(nodePosition.z);
            long cellKey = packGridCell(cellX, cellY, cellZ);
            Array<NodeReference> bucket = graphNodeSpatialBuckets.get(cellKey);
            if (bucket == null) {
                bucket = new Array<>();
                graphNodeSpatialBuckets.put(cellKey, bucket);
            }
            bucket.add(nodeReference);
        }

        if (cachedSoundPropagationZone != null) {
            cachedSoundPropagationZone.setNodePositions(graphNodePositions);
        }

        cachedNearestNodeId = "center";
        lastNearestNodeQueryPosition.set(Float.NaN, Float.NaN, Float.NaN);
    }

    private String findNearestNodeId(Vector3 worldPosition) {
        if (worldPosition == null || graphNodePositions.isEmpty()) {
            return "center";
        }

        if (lastNearestNodeQueryPosition.x == lastNearestNodeQueryPosition.x
            && worldPosition.dst2(lastNearestNodeQueryPosition) <= 0.04f
            && cachedNearestNodeId != null) {
            return cachedNearestNodeId;
        }

        String nearestNodeId = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;
        int centerCellX = worldToCell(worldPosition.x);
        int centerCellY = worldToCell(worldPosition.y);
        int centerCellZ = worldToCell(worldPosition.z);

        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    long bucketKey = packGridCell(centerCellX + dx, centerCellY + dy, centerCellZ + dz);
                    Array<NodeReference> bucket = graphNodeSpatialBuckets.get(bucketKey);
                    if (bucket == null || bucket.isEmpty()) {
                        continue;
                    }

                    for (int i = 0; i < bucket.size; i++) {
                        NodeReference reference = bucket.get(i);
                        float distanceSquared = reference.position.dst2(worldPosition);
                        if (distanceSquared < nearestDistanceSquared) {
                            nearestDistanceSquared = distanceSquared;
                            nearestNodeId = reference.nodeId;
                        }
                    }
                }
            }
        }

        if (nearestNodeId == null) {
            for (int i = 0; i < graphNodeReferences.size; i++) {
                NodeReference reference = graphNodeReferences.get(i);
                float distanceSquared = reference.position.dst2(worldPosition);
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearestNodeId = reference.nodeId;
                }
            }
        }

        cachedNearestNodeId = nearestNodeId == null ? "center" : nearestNodeId;
        lastNearestNodeQueryPosition.set(worldPosition);
        return cachedNearestNodeId;
    }

    private int worldToCell(float coordinate) {
        return MathUtils.floor(coordinate / NODE_GRID_CELL_SIZE);
    }

    private long packGridCell(int x, int y, int z) {
        long xBits = ((long) x) & 0x1FFFFFL;
        long yBits = ((long) y) & 0x1FFFFFL;
        long zBits = ((long) z) & 0x1FFFFFL;
        return (xBits << 42) | (yBits << 21) | zBits;
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
        fireSoundPulseVisual(worldPosition, intensity);
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
    public void hide() {
        if (realtimeMicSystem != null && realtimeMicSystem.isActive()) {
            realtimeMicSystem.stop();
        }
        micEnabled = false;
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void dispose() {
        if (realtimeMicSystem != null) {
            realtimeMicSystem.stop();
        }

        for (TestZone zone : zones) {
            if (zone instanceof Disposable disposableZone) {
                disposableZone.dispose();
            }
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
        if (particleManager != null) {
            particleManager.dispose();
        }
        if (soundPulseShaderRenderer != null) {
            soundPulseShaderRenderer.dispose();
        }
        if (particleShaderProgram != null) {
            particleShaderProgram.dispose();
        }
        if (acousticBounce3DVisualizer != null) {
            acousticBounce3DVisualizer.dispose();
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

    private static final class NodeReference {
        private final String nodeId;
        private final Vector3 position;

        private NodeReference(String nodeId, Vector3 position) {
            this.nodeId = nodeId;
            this.position = position;
        }
    }
}

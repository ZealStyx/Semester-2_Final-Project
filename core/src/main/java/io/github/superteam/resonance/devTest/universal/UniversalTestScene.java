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
import com.badlogic.gdx.utils.TimeUtils;
import io.github.superteam.resonance.director.DirectorController;
import io.github.superteam.resonance.devTest.universal.diagnostics.DiagnosticOverlay;
import io.github.superteam.resonance.devTest.universal.zones.BlindChamberZone;
import io.github.superteam.resonance.devTest.universal.zones.CrouchAlcoveZone;
import io.github.superteam.resonance.devTest.universal.zones.ItemInteractionZone;
import io.github.superteam.resonance.devTest.universal.zones.ParticleArenaZone;
import io.github.superteam.resonance.devTest.universal.zones.RampStairsZone;
import io.github.superteam.resonance.devTest.universal.zones.ShaderCorridorZone;
import io.github.superteam.resonance.devTest.universal.zones.SoundPropagationZone;
import io.github.superteam.resonance.interactable.door.DoorController;
import io.github.superteam.resonance.interactable.door.DoorBargeDetector;
import io.github.superteam.resonance.interactable.door.DoorLockState;
import io.github.superteam.resonance.interactable.door.DoorKnobCollider;
import io.github.superteam.resonance.interactable.InteractableRegistry;
import io.github.superteam.resonance.interactable.door.DoorGrabInteraction;
import io.github.superteam.resonance.interaction.RaycastInteractionSystem;
import io.github.superteam.resonance.interaction.InteractionPromptRenderer;
import io.github.superteam.resonance.interaction.InteractionResult;
import io.github.superteam.resonance.items.CarriableItem;
import io.github.superteam.resonance.items.ItemDefinition;
import io.github.superteam.resonance.items.ItemType;
import io.github.superteam.resonance.lighting.FlashlightController;
import io.github.superteam.resonance.lighting.FlickerController;
import io.github.superteam.resonance.lighting.GameLight;
import io.github.superteam.resonance.lighting.LightManager;
import io.github.superteam.resonance.lighting.LightingTier;
import io.github.superteam.resonance.footstep.FootstepSystem;
import io.github.superteam.resonance.footstep.SurfaceMaterial;
import io.github.superteam.resonance.player.CameraBreathingController;
import io.github.superteam.resonance.player.CarrySystem;
import io.github.superteam.resonance.player.ImpactListener;
import io.github.superteam.resonance.player.InventorySystem;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.player.PlayerController;
import io.github.superteam.resonance.player.PlayerFeatureExtractor;
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
import io.github.superteam.resonance.sanity.SanitySystem;
import io.github.superteam.resonance.sanity.drains.DarknessPresenceDrain;
import io.github.superteam.resonance.sanity.drains.EnemyProximityDrain;
import io.github.superteam.resonance.sanity.drains.EventDrain;
import io.github.superteam.resonance.sanity.effects.AudioGlitchEffect;
import io.github.superteam.resonance.sanity.effects.HallucinationEffect;
import io.github.superteam.resonance.sanity.effects.ScreenVignetteEffect;
import io.github.superteam.resonance.sanity.effects.ShaderDistortionEffect;
import io.github.superteam.resonance.scare.JumpScare;
import io.github.superteam.resonance.scare.JumpScareDirector;
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
import io.github.superteam.resonance.multiplayer.MultiplayerManager;
import io.github.superteam.resonance.multiplayer.MultiplayerLaunchConfig;
import io.github.superteam.resonance.multiplayer.RemotePlayer;
import io.github.superteam.resonance.multiplayer.VoiceCaptureSystem;
import io.github.superteam.resonance.multiplayer.VoicePlaybackSystem;
import io.github.superteam.resonance.multiplayer.packets.Packets;
import io.github.superteam.resonance.multiplayer.packets.Packets.VoiceChunkPacket;
import io.github.superteam.resonance.dialogue.DialogueSystem;
import io.github.superteam.resonance.save.SaveSystem;
import io.github.superteam.resonance.story.StorySystem;
import io.github.superteam.resonance.transition.RoomTransitionSystem;
import io.github.superteam.resonance.debug.DebugConsole;
import io.github.superteam.resonance.debug.DebugCommand;
import io.github.superteam.resonance.settings.SettingsSystem;
import io.github.superteam.resonance.behavior.BehaviorSystem;
import io.github.superteam.resonance.behavior.debug.BehaviorDebugOverlay;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * High-fidelity universal test environment with rendering, physics, sound, and
 * item systems.
 */
public final class UniversalTestScene extends ScreenAdapter {
    private static final float HUB_X = 40.0f;
    private static final float HUB_Z = 40.0f;
    private static final float ZONE_RING_OFFSET = 20.0f;
    private static final float BLIND_ZONE_OFFSET = 36.0f;
    private static final float DOOR_TEST_Z = HUB_Z + 10.0f;
    private static final float DOOR_TEST_SPAN = 1.6f;
    private static final float DOOR_INTERACT_RANGE = 2.8f;
    private static final float DOOR_INTERACT_FACING_DOT = 0.68f;
    private static final float DOOR_KNOB_INTERACT_RANGE = 2.5f;
    
    private MultiplayerLaunchConfig launchConfig = null;
    private boolean voiceCaptureStarted = false;

    public void setLaunchConfig(MultiplayerLaunchConfig config) {
        this.launchConfig = config;
        if (config == null || multiplayerManager == null) {
            return;
        }

        if (multiplayerManager.getRole() != MultiplayerManager.Role.OFFLINE) {
            return;
        }

        System.out.println("Initializing with launch config: " + config);
        if (config.role == MultiplayerLaunchConfig.Role.HOST) {
            multiplayerManager.startHost();
        } else if (config.role == MultiplayerLaunchConfig.Role.CLIENT) {
            if (config.hostIp != null && !config.hostIp.isBlank()) {
                multiplayerManager.connectAsClient(config.hostIp);
            }
        }

        tryStartVoiceCapture();
        this.launchConfig = null;
    }
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
    private static final float CROUCH_TUNNEL_CEILING = 1.4f; // v2 fix from master plan
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
    private TestMapLayout layoutInstance;

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
    private final Array<btRigidBody.btRigidBodyConstructionInfo> staticConstructionInfos = new Array<>();
    private final Array<btRigidBody.btRigidBodyConstructionInfo> carriableConstructionInfos = new Array<>();

    private final Matrix4 hudProjection = new Matrix4();
    private final Matrix4 playerViewModelTransform = new Matrix4();
    private final Matrix4 runtimeDoorTransform = new Matrix4();
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
    private final Vector3 runtimeDoorHingePosition = new Vector3();
    private final Vector3 runtimeDoorFaceTarget = new Vector3();
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
    private final LightManager lightManager;
    private FlashlightController flashlightController;
    private final SanitySystem sanitySystem;
    private final EventDrain sanityEventDrain;
    private final ScreenVignetteEffect sanityVignetteEffect;
    private final ShaderDistortionEffect sanityDistortionEffect;
    private final AudioGlitchEffect sanityAudioGlitchEffect;
    private final HallucinationEffect sanityHallucinationEffect;
    private final JumpScareDirector jumpScareDirector;
    private final SimplePanicModel panicModel = new SimplePanicModel();
    private final BlindFogUniformUpdater blindFogUniformUpdater = new BlindFogUniformUpdater();
    private EventTriggerRuntime eventTriggerRuntime;

    private final java.util.EnumMap<ItemType, Mesh> viewModelMeshes = new java.util.EnumMap<>(ItemType.class);
    private Mesh playerViewModelMesh;
    private Mesh runtimeDoorMesh;
    private Mesh fullscreenQuadMesh;
    private ClosestRayResultCallback cachedRayCallback;
    private DoorKnobCollider runtimeDoorKnobCollider;
    private ClosestRayResultCallback runtimeDoorKnobRayCallback;
    private io.github.superteam.resonance.particles.ParticleEffect mistEffect;
    private DoorController runtimeDoorController;
    private final Array<DoorController> layoutDoorControllers = new Array<>();
    private final DoorBargeDetector doorBargeDetector = new DoorBargeDetector();
    private InteractableRegistry interactableRegistry;
    private RaycastInteractionSystem raycastInteractionSystem;
    private DoorGrabInteraction doorGrabInteraction;
    private final InteractionPromptRenderer interactionPromptRenderer = new InteractionPromptRenderer();
    private final BehaviorDebugOverlay behaviorDebugOverlay = new BehaviorDebugOverlay();
    private String interactionPrompt = "";
    private boolean runtimeDoorKnobVisible;
    // Testbed systems (Group A-D)
    private DialogueSystem dialogueSystem;
    private SaveSystem saveSystem;
    private StorySystem storySystem;
    private RoomTransitionSystem roomTransitionSystem;
    private DebugConsole debugConsole;
    private SettingsSystem settingsSystem;
    private BehaviorSystem behaviorSystem;

    private btCollisionConfiguration physicsCollisionConfiguration;
    private btCollisionDispatcher physicsCollisionDispatcher;
    private btBroadphaseInterface physicsBroadphase;
    private btConstraintSolver physicsConstraintSolver;
    private btDiscreteDynamicsWorld physicsWorld;

    private AcousticGraphEngine acousticGraphEngine;
    private SoundPropagationOrchestrator soundPropagationOrchestrator;
    private SpatialCueController spatialCueController;
    private FootstepSystem footstepSystem;
    private PlayerFeatureExtractor playerFeatureExtractor;
    private PhysicsNoiseEmitter physicsNoiseEmitter;
    private ImpactListener impactListener;

    private CarrySystem carrySystem;
    private InventorySystem inventorySystem;

    private RealtimeMicSystem realtimeMicSystem;
    private SoundPropagationZone cachedSoundPropagationZone;
    private final float[] micRmsHistory = new float[MIC_HISTORY_SIZE];

    private float elapsedSeconds;
    private float runtimeSubtitleRemaining;
    private float currentFov = BASE_FOV;
    private float revealedFlashRemaining;
    private float lastSoundIntensity;
    private float inventoryFullMessageRemaining;
    private float itemDestroyedMessageRemaining;
    private float lastMicLevelNormalized;
    private float appliedBreathingY;
    private float appliedBreathingRollDeg;
    private float baseBarrelDistortionStrength;
    private float baseChromaticAberrationPixels;
    private float baseVignetteRadius;
    private float baseVignetteSoftness;
    private float baseVhsTapeNoiseAmount;
    private int micHistoryCursor;
    private int pendingScrollSteps;
    private boolean showGraphDebug;
    private boolean showBlindRadiusDebug;
    private boolean micEnabled;
    private boolean breathingApplied;
    private String lastItemDestroyedMessage;
    private String runtimeSubtitleText;
    private String cachedNearestNodeId = "center";
    private MultiplayerManager multiplayerManager;
    private VoiceCaptureSystem voiceCapture;
    private VoicePlaybackSystem voicePlayback;
    private boolean voiceCaptureStartBlocked = false;
    private short localPlayerId_forVoice;
    private float positionBroadcastTimer;
    private long lastVoicePlaybackMillis;
    private final Array<JoinBanner> joinBanners = new Array<>();
    private long lastMicStartAttemptMillis;
    private static final long MIC_START_RETRY_COOLDOWN_MS = 5000L;
    private static final long VOICE_OUTPUT_RECENT_WINDOW_MS = 140L;

    public UniversalTestScene() {
        camera = new PerspectiveCamera(BASE_FOV, Math.max(1, Gdx.graphics.getWidth()),
            Math.max(1, Gdx.graphics.getHeight()));
        camera.position.set(40.0f, PlayerController.CAPSULE_RADIUS, 39.5f);
        camera.lookAt(40.0f, EYE_HEIGHT, 43.0f);
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
        bodyCamShaderLoader = new BodyCamVHSShaderLoader("shaders/vert/body_cam_vhs.vert",
                "shaders/frag/body_cam_vhs.frag");
        bodyCamFrameBuffer = new BodyCamPassFrameBuffer();
        bodyCamSettings = BodyCamSettingsStore.loadOrDefault("config/body_cam_settings.json");
        captureBaseBodyCamSettings();
        blindEffectConfig = BlindEffectConfigStore.loadOrDefault("config/blind_effect_config.json");
        blindEffectController = new BlindEffectController(blindEffectConfig, panicModel);
        fullscreenQuadMesh = createFullscreenQuadMesh();
        bodyCamVisualizer = new BodyCamVHSVisualizer(
                bodyCamFrameBuffer,
                bodyCamShaderLoader.shader(),
                fullscreenQuadMesh,
                new BodyCamVHSAnimator());
        directorController = new DirectorController();
        lightManager = new LightManager(new FlickerController());
        flashlightController = new FlashlightController();
        setupLightingRig();

        sanitySystem = new SanitySystem();
        sanityEventDrain = new EventDrain();
        sanityVignetteEffect = new ScreenVignetteEffect();
        sanityDistortionEffect = new ShaderDistortionEffect();
        sanityAudioGlitchEffect = new AudioGlitchEffect();
        sanityHallucinationEffect = new HallucinationEffect();
        sanitySystem.addDrainSource(new DarknessPresenceDrain(1.4f));
        sanitySystem.addDrainSource(new EnemyProximityDrain(20f, 1.2f));
        sanitySystem.addDrainSource(sanityEventDrain);
        sanitySystem.addEffect(sanityVignetteEffect);
        sanitySystem.addEffect(sanityDistortionEffect);
        sanitySystem.addEffect(sanityAudioGlitchEffect);
        sanitySystem.addEffect(sanityHallucinationEffect);

        jumpScareDirector = new JumpScareDirector();

        // Lightweight systems for testbed demos
        dialogueSystem = new DialogueSystem();
        saveSystem = new SaveSystem();
        storySystem = new StorySystem();
        roomTransitionSystem = new RoomTransitionSystem();
        debugConsole = new DebugConsole();
        settingsSystem = new SettingsSystem();
        behaviorSystem = new BehaviorSystem();
        settingsSystem.loadOrCreate();

        // Register a simple debug "echo" command that shows subtitles
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "echo"; }
            @Override public String help() { return "echo <text> - show text as subtitle"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (args == null || args.length == 0) return;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.length; i++) { if (i > 0) sb.append(' '); sb.append(args[i]); }
                if (ctx != null) ctx.showSubtitle(sb.toString(), 3.0f);
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "settings"; }
            @Override public String help() { return "settings - show current settings snapshot"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (ctx == null) {
                    return;
                }
                io.github.superteam.resonance.settings.SettingsData data = settingsSystem.data();
                ctx.showSubtitle(
                    "Settings FOV=" + data.targetFov
                        + " Sens=" + String.format("%.2f", data.mouseSensitivity)
                        + " Vol=" + String.format("%.2f/%.2f/%.2f", data.masterVolume, data.musicVolume, data.sfxVolume),
                    3.5f
                );
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "story"; }
            @Override public String help() { return "story - show StorySystem debug status"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (ctx != null && storySystem != null) {
                    ctx.showSubtitle("Story: " + storySystem.debugStatus(), 3.0f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "sanity"; }
            @Override public String help() { return "sanity <delta> - queue sanity immediate delta (default -12)"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                float delta = -12f;
                if (args != null && args.length > 0) {
                    try {
                        delta = Float.parseFloat(args[0]);
                    } catch (NumberFormatException ignored) {
                        delta = -12f;
                    }
                }
                sanityEventDrain.queueDelta(delta);
                if (ctx != null) {
                    ctx.showSubtitle("Queued sanity delta: " + String.format("%.1f", delta), 2.5f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "savequick"; }
            @Override public String help() { return "savequick - write autosave from current testbed state"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                io.github.superteam.resonance.save.SaveData sd = new io.github.superteam.resonance.save.SaveData();
                sd.playerPosition = playerController.getPosition();
                sd.playerSanity = sanitySystem.getSanity();
                sd.savedAtMillis = TimeUtils.millis();
                sd.savedAtDisplay = String.format("%1$tF %1$tT", sd.savedAtMillis);
                saveSystem.save(sd);
                if (ctx != null) {
                    ctx.showSubtitle("savequick complete", 2.0f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "loadquick"; }
            @Override public String help() { return "loadquick - read autosave and show slot info"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                io.github.superteam.resonance.save.SaveData loaded = saveSystem.load();
                if (ctx == null) {
                    return;
                }
                if (loaded == null) {
                    ctx.showSubtitle("loadquick: no save found", 2.0f);
                } else {
                    ctx.showSubtitle("loadquick: " + loaded.saveSlotId + " @ " + loaded.savedAtDisplay, 2.5f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "behavior"; }
            @Override public String help() { return "behavior - show the current behavior archetype"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (ctx == null || ctx.behaviorSystem() == null) {
                    if (ctx != null) {
                        ctx.showSubtitle("BehaviorSystem not wired", 2.0f);
                    }
                    return;
                }
                ctx.showSubtitle("Behavior: " + ctx.behaviorSystem().currentArchetype(), 2.0f);
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "hallucinate"; }
            @Override public String help() { return "hallucinate <sanity> - drop sanity toward hallucination range"; }
            public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                float sanityTarget = 35f;
                if (args != null && args.length > 0) {
                    try {
                        sanityTarget = Float.parseFloat(args[0]);
                    } catch (NumberFormatException ignored) {
                        sanityTarget = 35f;
                    }
                }
                sanitySystem.setSanity(sanityTarget);
                showRuntimeSubtitle("Hallucination test sanity: " + String.format("%.1f", sanityTarget), 2.5f);
                if (ctx != null) {
                    ctx.showSubtitle("Hallucination test queued", 2.0f);
                }
            }
        });

        // Additional Group D console commands
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "fly"; }
            @Override public String help() { return "fly [speed] - toggle fly mode or set speed (m/s)"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (ctx != null) {
                    ctx.showSubtitle("Fly mode not yet wired in testbed", 2.0f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "beat"; }
            @Override public String help() { return "beat <beat-id> - force-complete a story beat"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (storySystem != null && args != null && args.length > 0) {
                    String beatId = String.join(" ", args);
                    if (ctx != null) {
                        ctx.showSubtitle("Beat forced: " + beatId, 2.0f);
                    }
                } else if (ctx != null) {
                    ctx.showSubtitle("beat requires an argument", 2.0f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "chapter"; }
            @Override public String help() { return "chapter <chapter-id> - force-start a chapter"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (storySystem != null && args != null && args.length > 0) {
                    String chapterId = String.join(" ", args);
                    if (ctx != null) {
                        ctx.showSubtitle("Chapter forced: " + chapterId, 2.0f);
                    }
                } else if (ctx != null) {
                    ctx.showSubtitle("chapter requires an argument", 2.0f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "god"; }
            @Override public String help() { return "god - toggle god mode (invulnerability + full vision)"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (ctx != null) {
                    ctx.showSubtitle("God mode not yet implemented", 2.0f);
                }
            }
        });
        debugConsole.register(new DebugCommand() {
            @Override public String name() { return "reload"; }
            @Override public String help() { return "reload - reload map and config files"; }
            @Override public void execute(String[] args, io.github.superteam.resonance.event.EventContext ctx) {
                if (ctx != null) {
                    ctx.showSubtitle("Reload not yet implemented for testbed", 2.0f);
                }
            }
        });

        buildParticleSystems();

        buildProceduralHubScene();
        setupRuntimeDoorTest();
        initializeZones();
        registerZoneColliders();
        soundPulseShaderRenderer = new SoundPulseShaderRenderer(worldColliders);
        playerController.setWorldColliders(worldColliders);
        playerViewModelMesh = createCubeMesh(1.0f, 1.0f, 1.0f);
        buildViewModelMeshes();

        initializePhysicsWorld();
        // Wall/floor/ceiling physics bodies are registered via registerStaticWorldBodies()
        // which reads worldColliders populated by buildBunkerWalls(). Do NOT call
        // layoutInstance.buildPhysics() — that would add room-volume boxes that block the player.
        registerRuntimeDoorKnobCollider();
        registerStaticWorldBodies();

        carrySystem = new CarrySystem(camera);
        inventorySystem = new InventorySystem();

        acousticGraphEngine = new GraphPopulator().populate(worldColliders);
        cacheGraphNodePositions();
        if (cachedSoundPropagationZone != null) {
            cachedSoundPropagationZone.setNodePositions(graphNodePositions);
        }
        acousticBounceConfig = AcousticBounceConfigStore.loadOrDefault("config/acoustic_bounce_3d_config.json");
        acousticBounce3DVisualizer = new AcousticBounce3DVisualizer(acousticGraphEngine, worldColliders,
                acousticBounceConfig);

        spatialCueController = new SpatialCueController();
        soundPropagationOrchestrator = new SoundPropagationOrchestrator(
                acousticGraphEngine,
                new DijkstraPathfinder(),
                new SonarRenderer(),
                spatialCueController,
                SoundBalancingConfigStore.loadOrDefault("config/balancing_config.json"));
        eventTriggerRuntime = EventTriggerRuntime.loadDefaults(
            "config/events.json",
            "config/triggers.json",
            this::showRuntimeSubtitle);
        if (eventTriggerRuntime != null) {
            eventTriggerRuntime.setOptionalSystems(sanitySystem, jumpScareDirector, dialogueSystem, debugConsole,
                storySystem, inventorySystem, null, behaviorSystem, roomTransitionSystem);
        }

        physicsNoiseEmitter = new PhysicsNoiseEmitter(soundPropagationOrchestrator);
        impactListener = new ImpactListener(physicsNoiseEmitter, this::findNearestNodeId);
        soundPropagationOrchestrator.registerDirectorListener((soundEventData, propagationResult) -> {
            lastSoundIntensity = Math.max(lastSoundIntensity, soundEventData.baseIntensity());
            directorController.onSoundEvent(soundEventData, propagationResult);
            lightManager.onSoundEvent(soundEventData);
            blindEffectController.onSoundEvent(soundEventData);
            acousticBounce3DVisualizer.onSoundEvent(soundEventData, propagationResult);
            sanityEventDrain.queueDelta(-0.65f * soundEventData.baseIntensity());
            if (propagationResult != null) {
                cacheRevealedNodesForFlash(propagationResult);
            }
        });
        soundPropagationOrchestrator.registerEnemyListener(directorController);

        footstepSystem = new FootstepSystem(
                playerController,
                soundPropagationOrchestrator,
                this::findNearestNodeId);
        // Resolve surface materials from TestMapLayout surface zones when available
        footstepSystem.setSurfaceResolver(position -> {
            if (layoutInstance == null) return SurfaceMaterial.CONCRETE;
            for (TestMapLayout.SurfaceZone sz : layoutInstance.surfaceZones()) {
                BoundingBox bb = sz.toBoundingBox();
                bb.min.add(HUB_X, 0f, HUB_Z);
                bb.max.add(HUB_X, 0f, HUB_Z);
                if (bb.contains(position)) {
                    return sz.material;
                }
            }
            return SurfaceMaterial.CONCRETE;
        });

        playerFeatureExtractor = new PlayerFeatureExtractor();
        playerFeatureExtractor.setLastKnownPosition(camera.position);
        playerFeatureExtractor.addListener(directorController);
        playerFeatureExtractor.addListener(behaviorSystem);

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
                    // Return to multiplayer menu
                    if (Gdx.app.getApplicationListener() instanceof Game game) {
                        Screen previous = game.getScreen();
                        game.setScreen(new MultiplayerTestMenuScreen());
                        if (previous != null && previous != game.getScreen()) {
                            Gdx.app.postRunnable(previous::dispose);
                        }
                    }
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

        // Register a zone-based sanity drain for the dark room defined in the layout
        if (layoutInstance != null && layoutInstance.darkRoomVolume() != null) {
            BoundingBox dark = new BoundingBox(layoutInstance.darkRoomVolume());
            dark.min.add(HUB_X, 0f, HUB_Z);
            dark.max.add(HUB_X, 0f, HUB_Z);
            sanitySystem.addDrainSource(new io.github.superteam.resonance.sanity.SanityDrainSource() {
                @Override
                public float drainPerSecond(io.github.superteam.resonance.sanity.SanitySystem.Context context) {
                    if (context == null) return 0f;
                    if (dark.contains(context.playerPosition())) return 1.4f;
                    return 0f;
                }

                @Override
                public float immediateDelta(io.github.superteam.resonance.sanity.SanitySystem.Context context) { return 0f; }
            });
        }

        Gdx.input.setCursorCatched(true);

        multiplayerManager = new MultiplayerManager();
        
        // Initialize networking from launch config if available
        if (launchConfig != null) {
            System.out.println("Initializing with launch config: " + launchConfig);
            if (launchConfig.role == MultiplayerLaunchConfig.Role.HOST) {
                multiplayerManager.startHost();
            } else if (launchConfig.role == MultiplayerLaunchConfig.Role.CLIENT) {
                if (launchConfig.hostIp != null) {
                    multiplayerManager.connectAsClient(launchConfig.hostIp);
                }
            }
            launchConfig = null; // Clear after use
        }
        
        voicePlayback = new VoicePlaybackSystem();
        voiceCapture = new VoiceCaptureSystem(this::onLocalVoiceChunkReady);
        tryStartVoiceCapture();
    }

    public UniversalTestScene(MultiplayerLaunchConfig config) {
        this();
        setLaunchConfig(config);
    }

    private void captureBaseBodyCamSettings() {
        if (bodyCamSettings == null) {
            return;
        }
        baseBarrelDistortionStrength = bodyCamSettings.barrelDistortionStrength;
        baseChromaticAberrationPixels = bodyCamSettings.chromaticAberrationPixels;
        baseVignetteRadius = bodyCamSettings.vignetteRadius;
        baseVignetteSoftness = bodyCamSettings.vignetteSoftness;
        baseVhsTapeNoiseAmount = bodyCamSettings.vhsTapeNoiseAmount;
    }

    private void setupLightingRig() {
        lightManager.register(new GameLight("hub-center", new Vector3(HUB_X, 2.2f, HUB_Z),
            new com.badlogic.gdx.graphics.Color(0.70f, 0.88f, 1.0f, 1f), 16f, 0.70f, true));
        lightManager.register(new GameLight("sound-zone", new Vector3(HUB_X, 2.1f, HUB_Z + ZONE_RING_OFFSET),
            new com.badlogic.gdx.graphics.Color(0.66f, 0.75f, 1.0f, 1f), 14f, 0.65f, true));
        lightManager.register(new GameLight("blind-zone", new Vector3(HUB_X, 2.0f, HUB_Z + BLIND_ZONE_OFFSET),
            new com.badlogic.gdx.graphics.Color(0.80f, 0.76f, 0.68f, 1f), 12f, 0.55f, false));
        lightManager.register(new GameLight("door-lane", new Vector3(HUB_X, 2.0f, DOOR_TEST_Z),
            new com.badlogic.gdx.graphics.Color(0.72f, 0.82f, 0.95f, 1f), 11f, 0.60f, true));
    }

    private static LightingTier toLightingTier(DirectorController.DirectorTier directorTier) {
        if (directorTier == null) {
            return LightingTier.CALM;
        }
        return switch (directorTier) {
            case CALM -> LightingTier.CALM;
            case TENSE -> LightingTier.TENSE;
            case PANICKED -> LightingTier.PANICKED;
        };
    }

    private void applySanityDrivenBodyCamEffects() {
        if (bodyCamSettings == null) {
            return;
        }

        float vignetteStrength = sanityVignetteEffect.strength();
        float distortionStrength = sanityDistortionEffect.distortion();
        float audioGlitch = sanityAudioGlitchEffect.glitchAmount();

        bodyCamSettings.vignetteRadius = MathUtils.clamp(
            baseVignetteRadius - (0.18f * vignetteStrength),
            0.25f,
            1.5f
        );
        bodyCamSettings.vignetteSoftness = MathUtils.clamp(
            baseVignetteSoftness + (0.36f * vignetteStrength),
            0.05f,
            1.0f
        );
        bodyCamSettings.barrelDistortionStrength = MathUtils.clamp(
            baseBarrelDistortionStrength + (0.20f * distortionStrength),
            0f,
            0.5f
        );
        bodyCamSettings.chromaticAberrationPixels = MathUtils.clamp(
            baseChromaticAberrationPixels + (3.0f * distortionStrength),
            0f,
            8f
        );
        bodyCamSettings.vhsTapeNoiseAmount = MathUtils.clamp(
            baseVhsTapeNoiseAmount + (0.16f * audioGlitch),
            0f,
            1f
        );
    }

    private void onLocalVoiceChunkReady(VoiceChunkPacket chunk) {
        int localId = multiplayerManager.getLocalPlayerId();
        if (localId > 0) {
            chunk.sourcePlayerId = localId;
        }

        if (multiplayerManager.getRole() == MultiplayerManager.Role.HOST) {
            multiplayerManager.broadcastVoice(chunk);
        } else if (multiplayerManager.getRole() == MultiplayerManager.Role.CLIENT) {
            multiplayerManager.sendVoiceToHost(chunk);
        }
    }

    private void tryStartVoiceCapture() {
        if (voiceCaptureStarted || voiceCaptureStartBlocked || voiceCapture == null || multiplayerManager == null) {
            return;
        }

        if (multiplayerManager.getRole() == MultiplayerManager.Role.OFFLINE) {
            return;
        }

        int localId = multiplayerManager.getLocalPlayerId();
        if (localId <= 0) {
            return;
        }

        if (multiplayerManager.hasLocalPlayerId()) {
            if (realtimeMicSystem != null && realtimeMicSystem.isActive()) {
                realtimeMicSystem.stop();
                micEnabled = false;
                Gdx.app.log("Mic", "Stopped realtime mic before multiplayer voice capture start");
            }

            if (voiceCapture.start(localId)) {
                voiceCaptureStarted = true;
            } else {
                voiceCaptureStartBlocked = true;
                Gdx.app.log("Voice", "Voice capture disabled after failed microphone start");
            }
        }
    }

    private void initializeZones() {
        zones.add(new RampStairsZone(new Vector3(HUB_X + ZONE_RING_OFFSET, 0.0f, HUB_Z)));
        zones.add(new CrouchAlcoveZone(new Vector3(HUB_X, 0.0f, HUB_Z - ZONE_RING_OFFSET)));
        zones.add(new SoundPropagationZone(new Vector3(HUB_X, 0.0f, HUB_Z + ZONE_RING_OFFSET)));
        zones.add(new ParticleArenaZone(new Vector3(HUB_X + ZONE_RING_OFFSET, 0.0f, HUB_Z + ZONE_RING_OFFSET)));
        zones.add(new ItemInteractionZone(new Vector3(HUB_X - ZONE_RING_OFFSET, 0.0f, HUB_Z + ZONE_RING_OFFSET)));
        zones.add(new ShaderCorridorZone(new Vector3(HUB_X - ZONE_RING_OFFSET, 0.0f, HUB_Z)));
        zones.add(new BlindChamberZone(new Vector3(HUB_X, 0.0f, HUB_Z + BLIND_ZONE_OFFSET)));

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
                addCollider(collider.cx(), collider.cy(), collider.cz(), collider.width(), collider.height(),
                        collider.depth());
            }
        }
    }

    private void loadWorldShader() {
        worldShader = new ShaderProgram(
                Gdx.files.internal("shaders/vert/retro_shader.vert"),
                Gdx.files.internal("shaders/frag/retro_shader.frag"));
        if (!worldShader.isCompiled()) {
            throw new GdxRuntimeException("World shader failed to compile: " + worldShader.getLog());
        }
    }

    private void loadPlayerShader() {
        try {
            playerShaderProgram = new ShaderProgram(
                    Gdx.files.internal("shaders/vert/player_shader.vert"),
                    Gdx.files.internal("shaders/frag/player_shader.frag"));
        } catch (Exception ignored) {
            String vertexShader = "attribute vec3 a_position;\n" +
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

            String fragmentShader = "#ifdef GL_ES\n" +
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
                Gdx.files.internal("shaders/frag/particle_shader.frag"));
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

        // Fix H â€” atmospheric mist emitter that follows the player each frame.
        ParticleDefinition mistDefinition = ParticleDefinition.createDefault();
        mistDefinition.emissionRate = 6f;
        mistDefinition.emissionDuration = 0f; // 0 = continuous in this system
        mistDefinition.burstMode = false;
        mistDefinition.burstLoop = false;
        mistDefinition.burstStaggerDuration = 0f;
        mistDefinition.lifetimeMin = 3.5f;
        mistDefinition.lifetimeMax = 5.5f;
        mistDefinition.startSizeMin = 0.30f;
        mistDefinition.startSizeMax = 0.55f;
        mistDefinition.endSizeMin = 0.80f;
        mistDefinition.endSizeMax = 1.20f;
        // Mist is nearly invisible â€” low alpha via color alpha channel.
        mistDefinition.startColor = new float[] { 0.55f, 0.62f, 0.70f, 0.07f };
        mistDefinition.endColor = new float[] { 0.45f, 0.52f, 0.60f, 0.00f };
        mistDefinition.speedMin = 0.04f;
        mistDefinition.speedMax = 0.12f;
        mistDefinition.spreadAngle = 80f;
        mistDefinition.directionX = 0f;
        mistDefinition.directionY = 1f;
        mistDefinition.directionZ = 0f;
        mistDefinition.gravity = 0f;
        mistDefinition.blendMode = "ALPHA";
        mistDefinition.maxParticles = 40;
        mistDefinition.sanitize();

        mistEffect = new ParticleEffect("Atmospheric Mist");
        mistEffect.addEmitter(new ParticleEmitter(mistDefinition));
        mistEffect.setActive(true);
        particleManager.addEffect(mistEffect);
    }

    /** INCON-06 / Fix L â€” per-item-type view model meshes with distinct shapes. */
    private void buildViewModelMeshes() {
        viewModelMeshes.put(ItemType.METAL_PIPE, createCubeMesh(0.06f, 0.80f, 0.06f));
        viewModelMeshes.put(ItemType.GLASS_BOTTLE, createCubeMesh(0.09f, 0.38f, 0.09f));
        viewModelMeshes.put(ItemType.CARDBOARD_BOX, createCubeMesh(0.32f, 0.32f, 0.32f));
        viewModelMeshes.put(ItemType.CONCRETE_CHUNK, createCubeMesh(0.22f, 0.20f, 0.22f));
        viewModelMeshes.put(ItemType.FLARE, createCubeMesh(0.05f, 0.28f, 0.05f));
        viewModelMeshes.put(ItemType.NOISE_DECOY, createCubeMesh(0.14f, 0.14f, 0.14f));
        viewModelMeshes.put(ItemType.KEY, createCubeMesh(0.06f, 0.12f, 0.04f));
    }

    private void recreateBodyCamFrameBuffers(int width, int height) {
        bodyCamFrameBuffer.resize(width, height);
    }

    private void initializePhysicsWorld() {
        if (!bulletInitialized) {
            Bullet.init();
            bulletInitialized = true;
        }
        // IMPROVE-01 â€” pre-allocate the ray callback so findRaycastCarriable() never
        // allocates per frame.
        cachedRayCallback = new ClosestRayResultCallback(Vector3.Zero, Vector3.Zero);

        physicsCollisionConfiguration = new btDefaultCollisionConfiguration();
        physicsCollisionDispatcher = new btCollisionDispatcher(physicsCollisionConfiguration);
        physicsBroadphase = new btDbvtBroadphase();
        physicsConstraintSolver = new btSequentialImpulseConstraintSolver();
        physicsWorld = new btDiscreteDynamicsWorld(
                physicsCollisionDispatcher,
                physicsBroadphase,
                physicsConstraintSolver,
                physicsCollisionConfiguration);
        physicsWorld.setGravity(new Vector3(0f, -9.8f, 0f));

        btStaticPlaneShape floorShape = new btStaticPlaneShape(new Vector3(0f, 1f, 0f), 0f);
        btDefaultMotionState floorMotionState = new btDefaultMotionState(new Matrix4().idt());
        btRigidBodyConstructionInfo floorInfo = new btRigidBodyConstructionInfo(0f, floorMotionState, floorShape,
            Vector3.Zero);
        btRigidBody floorBody = new btRigidBody(floorInfo);
        staticConstructionInfos.add(floorInfo);

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
            btDefaultMotionState motionState = new btDefaultMotionState(
                    new Matrix4().setToTranslation(centerX, centerY, centerZ));
                btRigidBodyConstructionInfo constructionInfo = new btRigidBodyConstructionInfo(0f, motionState, shape,
                    Vector3.Zero);
                btRigidBody rigidBody = new btRigidBody(constructionInfo);
                staticConstructionInfos.add(constructionInfo);

            physicsWorld.addRigidBody(rigidBody);
            staticCollisionShapes.add(shape);
            staticMotionStates.add(motionState);
            staticRigidBodies.add(rigidBody);
        }
    }

    private void buildProceduralHubScene() {
        // Thin world ground plane (catch-all below all rooms)
        addBox(HUB_X, -0.1f, HUB_Z, 96.0f, 0.1f, 96.0f, true);

        // Build layout instance (zones, doors, surfaces)
        layoutInstance = TestMapLayout.buildDefaultLayoutInstance();

        // Build all walled bunker geometry (walls, floors, ceilings, pillars)
        buildBunkerWalls();
    }

    /**
     * Builds individual wall, floor, and ceiling boxes for the horror bunker test map.
     * All coordinates are world-space. addBox(cx, cy, cz, width, height, depth, addCollider).
     * Wall thickness = 0.3 m. Floor slab center at Y = −0.05. Ceiling cap extends 0.3 m
     * beyond wall outer faces so corners are fully sealed.
     *
     * Every addBox(..., true) call here is an acoustic surface consumed by
     * GraphPopulator.populate(worldColliders). Dimensions and pillar placements are
     * sized so all graph hops stay within EDGE_MAX_DISTANCE_METERS = 4.0 m.
     * Do NOT resize or remove any collider box without re-checking acoustic connectivity.
     */
    private void buildBunkerWalls() {

        // ════════════════════════════════════════════════════════════════════════
        // ROOM 1 — SPAWN ROOM
        // Interior: X[36.5, 43.5], Z[37.0, 43.0], ceiling Y = 3.0
        // ════════════════════════════════════════════════════════════════════════

        // Floor
        addBox(40.0f, -0.1f, 40.0f,   7.0f, 0.10f,  6.0f,  true);
        // Ceiling (0.3 m wider each side to seal wall tops)
        addBox(40.0f,  3.05f, 40.0f,   7.6f, 0.10f,  6.6f,  true);

        // West wall  (X = 36.5, outer face X = 36.2)
        addBox(36.35f, 1.50f, 40.0f,   0.3f, 3.00f,  6.0f,  true);
        // East wall  (X = 43.5, outer face X = 43.8)
        addBox(43.65f, 1.50f, 40.0f,   0.3f, 3.00f,  6.0f,  true);
        // South wall — solid, no opening  (Z = 37.0)
        addBox(40.0f,  1.50f, 36.85f,  7.6f, 3.00f,  0.3f,  true);

        // North wall — door opening 1.2 m wide centered at X = 40.0, 2.1 m tall
        //   Gap occupies X[39.4, 40.6]
        //   Left segment  X[36.5, 39.4]  width = 2.9  center X = 37.95
        addBox(37.95f, 1.50f, 43.15f,  2.9f, 3.00f,  0.3f,  true);
        //   Right segment X[40.6, 43.5]  width = 2.9  center X = 42.05
        addBox(42.05f, 1.50f, 43.15f,  2.9f, 3.00f,  0.3f,  true);
        //   Door header   above 2.1 m → top at 3.0 m → height = 0.9  center Y = 2.55
        addBox(40.0f,  2.55f, 43.15f,  1.2f, 0.90f,  0.3f,  true);

        // ════════════════════════════════════════════════════════════════════════
        // CORRIDOR 1
        // Interior: X[38.5, 41.5], Z[43.0, 51.0], ceiling Y = 2.8
        // Connects Spawn (south) to Dark Room (north) — both ends are open passages.
        // West wall has a 1.5 m door opening to Crouch Alcove centered at Z = 47.0
        // ════════════════════════════════════════════════════════════════════════

        // Floor
        addBox(40.0f, -0.1f, 47.0f,   3.0f, 0.10f,  8.0f,  true);
        // Ceiling
        addBox(40.0f,  2.85f, 47.0f,   3.6f, 0.10f,  8.6f,  true);

        // East wall — solid
        addBox(41.65f, 1.40f, 47.0f,   0.3f, 2.80f,  8.0f,  true);

        // West wall — split for alcove door opening  (Z gap: [46.25, 47.75], width = 1.5 m)
        //   South segment  Z[43.0, 46.25]  depth = 3.25  center Z = 44.625
        addBox(38.35f, 1.40f, 44.625f, 0.3f, 2.80f,  3.25f, true);
        //   North segment  Z[47.75, 51.0]  depth = 3.25  center Z = 49.375
        addBox(38.35f, 1.40f, 49.375f, 0.3f, 2.80f,  3.25f, true);
        //   Header above alcove door (door height = 1.55 m, corridor ceiling = 2.8 m)
        //   Header height = 2.8 − 1.55 = 1.25 m,  center Y = 1.55 + 0.625 = 2.175
        addBox(38.35f, 2.175f, 47.0f,  0.3f, 1.25f,  1.5f,  true);

        // South end — no wall needed; shares space with Spawn north wall
        // North end — no wall needed; open into Dark Room

        // ════════════════════════════════════════════════════════════════════════
        // ROOM 2 — CROUCH ALCOVE
        // Interior: X[32.5, 38.5], Z[45.5, 48.5], ceiling Y = 1.55
        // Entry on the east face via the door matched to the corridor opening above.
        // ════════════════════════════════════════════════════════════════════════

        // Floor
        addBox(35.5f, -0.1f, 47.0f,   6.0f, 0.10f,  3.0f,  true);
        // Ceiling (tight — only 1.55 m headroom)
        addBox(35.5f,  1.60f, 47.0f,   6.6f, 0.10f,  3.6f,  true);

        // West wall
        addBox(32.35f, 0.775f, 47.0f,  0.3f, 1.55f,  3.0f,  true);
        // North wall
        addBox(35.5f,  0.775f, 48.65f, 6.6f, 1.55f,  0.3f,  true);
        // South wall
        addBox(35.5f,  0.775f, 45.35f, 6.6f, 1.55f,  0.3f,  true);

        // East wall — door opening 1.5 m wide centered at Z = 47.0
        //   Gap Z[46.25, 47.75]
        //   South segment  Z[45.5, 46.25]  depth = 0.75  center Z = 45.875
        addBox(38.65f, 0.775f, 45.875f, 0.3f, 1.55f, 0.75f, true);
        //   North segment  Z[47.75, 48.5]  depth = 0.75  center Z = 48.125
        addBox(38.65f, 0.775f, 48.125f, 0.3f, 1.55f, 0.75f, true);
        // No header: alcove ceiling IS 1.55 m — door fills the full height.

        // Relay obstacle — required for acoustic graph connectivity.
        // The alcove is 6 m wide (> EDGE_MAX_DISTANCE_METERS = 4.0 m), so without
        // this box GraphPopulator cannot bridge west wall to east face in a single hop.
        // Low enough to step over (0.6 m) but tall enough to generate nodes (> 0.5 m threshold).
        addBox(35.5f, 0.30f, 47.0f,   1.0f, 0.60f, 0.8f,  true);  // storage crate relay

        // ════════════════════════════════════════════════════════════════════════
        // ROOM 3 — DARK ROOM
        // Interior: X[35.5, 44.5], Z[51.0, 60.0], ceiling Y = 3.2
        // South face has an open 3 m passage aligned with Corridor 1 (X[38.5, 41.5]).
        // ════════════════════════════════════════════════════════════════════════

        // Floor
        addBox(40.0f, -0.1f, 55.5f,   9.0f, 0.10f,  9.0f,  true);
        // Ceiling
        addBox(40.0f,  3.25f, 55.5f,   9.6f, 0.10f,  9.6f,  true);

        // West wall
        addBox(35.35f, 1.60f, 55.5f,   0.3f, 3.20f,  9.0f,  true);
        // East wall
        addBox(44.65f, 1.60f, 55.5f,   0.3f, 3.20f,  9.0f,  true);
        // North wall — solid
        addBox(40.0f,  1.60f, 60.15f,  9.6f, 3.20f,  0.3f,  true);

        // South wall — 3 m passage opening aligned with corridor X[38.5, 41.5]
        //   Left segment   X[35.5, 38.5]  width = 3.0  center X = 37.0
        addBox(37.0f,  1.60f, 50.85f,  3.0f, 3.20f,  0.3f,  true);
        //   Right segment  X[41.5, 44.5]  width = 3.0  center X = 43.0
        addBox(43.0f,  1.60f, 50.85f,  3.0f, 3.20f,  0.3f,  true);
        // No door, no header — corridor ceiling (2.8) is lower than dark room (3.2)

        // ─── Pillars inside Dark Room (occlusion + acoustics) ───────────────────
        // Required to bridge the 9 m dark room width within 4 m acoustic hops:
        //   west wall → pillar SW → pillar N → pillar SE → east wall (all ≤ 4 m)
        // Pillar SW
        addBox(37.5f, 1.60f, 53.5f,    0.6f, 3.20f,  0.6f,  true);
        // Pillar SE
        addBox(42.5f, 1.60f, 53.5f,    0.6f, 3.20f,  0.6f,  true);
        // Pillar center-north
        addBox(40.0f, 1.60f, 57.5f,    0.6f, 3.20f,  0.6f,  true);
    }

    private void setupRuntimeDoorTest() {
        runtimeDoorHingePosition.set(HUB_X - (DOOR_TEST_SPAN * 0.5f), 1.05f, DOOR_TEST_Z);
        runtimeDoorController = new DoorController("runtime-door-test", runtimeDoorHingePosition, 2.2f);
        runtimeDoorMesh = createCubeMesh(DOOR_TEST_SPAN, 2.1f, 0.12f);
        refreshRuntimeDoorTransform();
        // v2: register runtime door for raycast interactions and door-grab
        interactableRegistry = new InteractableRegistry();
        interactableRegistry.register(runtimeDoorController);
        raycastInteractionSystem = new RaycastInteractionSystem(interactableRegistry);
        // Interact key defaults to F — do not override
        doorGrabInteraction = new DoorGrabInteraction();

        layoutDoorControllers.clear();
        // Spawn additional doors defined in the TestMapLayout
        if (layoutInstance != null) {
            for (TestMapLayout.DoorSpawn ds : layoutInstance.doorSpawns()) {
                Vector3 hinge = ds.hingeWorldPosition(HUB_X, HUB_Z);
                DoorController dc = new DoorController(ds.id, hinge, 2.2f);
                dc.setLockState(ds.locked ? DoorLockState.LOCKED : DoorLockState.UNLOCKED);
                // Creak speed and slam intensity are determined at runtime by the player's
                // mouse velocity and sprint state — not by per-door multiplier fields.
                interactableRegistry.register(dc);
                layoutDoorControllers.add(dc);
                // Add a simple visual mesh for spawned door
                Mesh doorMesh = createCubeMesh(2.0f, 2.1f, 0.12f);
                sceneMeshes.add(doorMesh);
                sceneTransforms.add(new Matrix4().setToTranslation(hinge));
            }
        }
    }

    private void updateRuntimeDoorTest(float deltaSeconds) {
        if (runtimeDoorController == null) {
            return;
        }

        // Build a minimal EventContext for interactive updates
        io.github.superteam.resonance.event.EventContext interactionContext = new io.github.superteam.resonance.event.EventContext(
            camera.position,
            camera.position,
            elapsedSeconds,
            soundPropagationOrchestrator,
            null,
            null,
            null,
            this::showRuntimeSubtitle
        );

        // Raycast interaction update
        runtimeDoorKnobVisible = isRuntimeDoorKnobTargeted();
        if (raycastInteractionSystem != null) {
            InteractionResult interactionResult = raycastInteractionSystem.update(
                camera.position,
                camera.direction,
                interactionContext
            );
            interactionPrompt = interactionResult != null && interactionResult.hasTarget()
                ? interactionPromptRenderer.formatPrompt(interactionResult)
                : "";

            DoorController grabbedDoor = doorGrabInteraction == null ? null : doorGrabInteraction.grabbedDoor();
            DoorController focusedDoor = interactionResult != null && interactionResult.hasTarget() && interactionResult.target() instanceof DoorController door
                ? door
                : null;
            DoorController grabTarget = focusedDoor != null
                ? focusedDoor
                : (grabbedDoor != null ? grabbedDoor : (runtimeDoorKnobVisible ? runtimeDoorController : null));

            if (doorGrabInteraction != null && grabTarget != null) {
                doorGrabInteraction.update(
                    deltaSeconds,
                    Gdx.input.getDeltaX(),
                    Gdx.input.isButtonPressed(Input.Buttons.LEFT),
                    grabTarget,
                    interactionContext
                );
            }
        }

        runtimeDoorController.update(deltaSeconds, interactionContext);
        syncRuntimeDoorKnobCollider();
        refreshRuntimeDoorTransform();

        if (!layoutDoorControllers.isEmpty()) {
            for (DoorController door : layoutDoorControllers) {
                door.update(deltaSeconds, interactionContext);
                if (doorBargeDetector.check(playerController, door, interactionContext)) {
                    // bargeOpen already emitted the slam audio; keep the controller hot for the enemy/sound systems below
                }

                float noiseIntensity = door.consumeNoiseIntensity();
                if (noiseIntensity < 0.15f || soundPropagationOrchestrator == null) {
                    continue;
                }

                String sourceNodeId = findNearestNodeId(door.worldPosition());
                if (sourceNodeId != null) {
                    soundPropagationOrchestrator.emitSoundEvent(
                        new io.github.superteam.resonance.sound.SoundEventData(
                            io.github.superteam.resonance.sound.SoundEvent.DOOR_SLAM,
                            sourceNodeId,
                            door.worldPosition(),
                            noiseIntensity,
                            elapsedSeconds
                        ),
                        elapsedSeconds
                    );
                }
            }
        }
    }

    private void registerRuntimeDoorKnobCollider() {
        if (physicsWorld == null || runtimeDoorController == null) {
            return;
        }
        if (runtimeDoorKnobCollider == null) {
            runtimeDoorKnobCollider = new DoorKnobCollider(runtimeDoorController.knobWorldPosition(), physicsWorld);
            return;
        }
        runtimeDoorKnobCollider.attachToWorld(physicsWorld);
    }

    private void syncRuntimeDoorKnobCollider() {
        if (runtimeDoorKnobCollider == null || runtimeDoorController == null) {
            return;
        }
        runtimeDoorKnobCollider.syncPosition(runtimeDoorController.knobWorldPosition());
    }

    private boolean isRuntimeDoorKnobTargeted() {
        if (physicsWorld == null || runtimeDoorKnobCollider == null || runtimeDoorController == null) {
            return false;
        }

        tmpRayDirection.set(camera.direction).nor();
        tmpRayEnd.set(camera.position).mulAdd(tmpRayDirection, DOOR_KNOB_INTERACT_RANGE);

        if (runtimeDoorKnobRayCallback == null) {
            runtimeDoorKnobRayCallback = new ClosestRayResultCallback(camera.position, tmpRayEnd);
        }
        runtimeDoorKnobRayCallback.setRayFromWorld(camera.position);
        runtimeDoorKnobRayCallback.setRayToWorld(tmpRayEnd);
        runtimeDoorKnobRayCallback.setCollisionObject(null);
        runtimeDoorKnobRayCallback.setClosestHitFraction(1f);
        physicsWorld.rayTest(camera.position, tmpRayEnd, runtimeDoorKnobRayCallback);
        return runtimeDoorKnobRayCallback.hasHit()
            && runtimeDoorKnobRayCallback.getCollisionObject() == runtimeDoorKnobCollider.body();
    }

    private void refreshRuntimeDoorTransform() {
        if (runtimeDoorController == null) {
            runtimeDoorTransform.idt();
            return;
        }

        runtimeDoorTransform.idt()
                .translate(runtimeDoorHingePosition)
                .rotate(Vector3.Y, -runtimeDoorController.currentAngle())
                .translate(DOOR_TEST_SPAN * 0.5f, 0f, 0f);
    }

    private void addBox(float centerX, float centerY, float centerZ, float width, float height, float depth,
            boolean addCollider) {
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
                centerZ - (depth * 0.5f));
        Vector3 max = new Vector3(
                centerX + (width * 0.5f),
                centerY + (height * 0.5f),
                centerZ + (depth * 0.5f));
        worldColliders.add(new BoundingBox(min, max));
    }

    private Mesh createCubeMesh(float width, float height, float depth) {
        Mesh mesh = new Mesh(
                true,
                24,
                36,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"));

        float w = width * 0.5f;
        float h = height * 0.5f;
        float d = depth * 0.5f;

        float[] vertices = {
                -w, -h, d, 0, 0, 1, 1, 1, 1, 1,
                w, -h, d, 0, 0, 1, 1, 1, 1, 1,
                w, h, d, 0, 0, 1, 1, 1, 1, 1,
                -w, h, d, 0, 0, 1, 1, 1, 1, 1,

                w, -h, -d, 0, 0, -1, 1, 1, 1, 1,
                -w, -h, -d, 0, 0, -1, 1, 1, 1, 1,
                -w, h, -d, 0, 0, -1, 1, 1, 1, 1,
                w, h, -d, 0, 0, -1, 1, 1, 1, 1,

                w, -h, d, 1, 0, 0, 1, 1, 1, 1,
                w, -h, -d, 1, 0, 0, 1, 1, 1, 1,
                w, h, -d, 1, 0, 0, 1, 1, 1, 1,
                w, h, d, 1, 0, 0, 1, 1, 1, 1,

                -w, -h, -d, -1, 0, 0, 1, 1, 1, 1,
                -w, -h, d, -1, 0, 0, 1, 1, 1, 1,
                -w, h, d, -1, 0, 0, 1, 1, 1, 1,
                -w, h, -d, -1, 0, 0, 1, 1, 1, 1,

                -w, h, -d, 0, 1, 0, 1, 1, 1, 1,
                -w, h, d, 0, 1, 0, 1, 1, 1, 1,
                w, h, d, 0, 1, 0, 1, 1, 1, 1,
                w, h, -d, 0, 1, 0, 1, 1, 1, 1,

                -w, -h, d, 0, -1, 0, 1, 1, 1, 1,
                -w, -h, -d, 0, -1, 0, 1, 1, 1, 1,
                w, -h, -d, 0, -1, 0, 1, 1, 1, 1,
                w, -h, d, 0, -1, 0, 1, 1, 1, 1
        };

        short[] indices = {
                0, 1, 2, 2, 3, 0,
                4, 5, 6, 6, 7, 4,
                8, 9, 10, 10, 11, 8,
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
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

        float[] vertices = {
                -1f, -1f, 0f, 0f, 0f,
                1f, -1f, 0f, 1f, 0f,
                1f, 1f, 0f, 1f, 1f,
                -1f, 1f, 0f, 0f, 1f
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
        // DEBUG KEY MAP
        // F1  â€” Toggle graph debug markers
        // F2  â€” Toggle blind radius debug overlay
        // F3  â€” Reload runtime configs
        // F4  â€” Toggle acoustic bounce rays
        // F5  â€” Toggle VHS shader
        // F6  â€” Toggle microphone input
        // F7  â€” Trigger blind flare reveal
        // F8  â€” Cycle diagnostic overlay tab
        // F9  â€” Switch to multiplayer menu
        // F10 â€” Switch to UniversalTestScene
        // F11 â€” Switch to GltfMapTestScene
        // F12 â€” Fire debug event flag
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            showGraphDebug = !showGraphDebug;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            showBlindRadiusDebug = !showBlindRadiusDebug;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            bodyCamSettings = BodyCamSettingsStore.loadOrDefault("config/body_cam_settings.json");
            captureBaseBodyCamSettings();
            blindEffectConfig = BlindEffectConfigStore.loadOrDefault("config/blind_effect_config.json");
            blindEffectController.reloadConfig(blindEffectConfig);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            acousticBounceConfig.geometricLayer.renderRays = !acousticBounceConfig.geometricLayer.renderRays;
            acousticBounce3DVisualizer.reloadConfig(acousticBounceConfig);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            bodyCamSettings.enabled = !bodyCamSettings.enabled;
        }

        // INCON-01 â€” V is the canonical sonar-pulse key; X was a duplicate and is
        // removed.
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            firePlayerPulse();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            toggleMicInput();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
            blindEffectController.triggerFlareReveal();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            // Backtick key opens debug console
            if (debugConsole != null) {
                showRuntimeSubtitle("Debug console: type commands like 'echo', 'sanity', 'story'", 2.5f);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            if (Gdx.app.getApplicationListener() instanceof Game game) {
                Screen previous = game.getScreen();
                game.setScreen(new MultiplayerTestMenuScreen());
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.app.getApplicationListener() instanceof Game game) {
                Screen previous = game.getScreen();
                game.setScreen(new GltfMapTestScene());
                if (previous != null && previous != game.getScreen()) {
                    Gdx.app.postRunnable(previous::dispose);
                }
                return true;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            fireDebugEventFlag();
        }

        // Quick demo keys for integrated systems
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (dialogueSystem != null) dialogueSystem.showSubtitle("Demo subtitle from testbed.", 3.0f);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            if (saveSystem != null) {
                io.github.superteam.resonance.save.SaveData sd = new io.github.superteam.resonance.save.SaveData();
                sd.playerPosition = playerController.getPosition();
                sd.playerSanity = sanitySystem.getSanity();
                sd.savedAtMillis = TimeUtils.millis();
                sd.savedAtDisplay = String.format("%1$tF %1$tT", sd.savedAtMillis);
                saveSystem.save(sd);
                showRuntimeSubtitle("Saved demo autosave.", 1.5f);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            if (flashlightController != null) {
                flashlightController.setEnabled(!flashlightController.isEnabled());
                showRuntimeSubtitle("Flashlight: " + (flashlightController.isEnabled() ? "ON" : "OFF"), 1.0f);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            if (roomTransitionSystem != null) {
                roomTransitionSystem.triggerRoomTransition("DemoRoom");
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            sanityEventDrain.queueDelta(-15f);
            showRuntimeSubtitle("Queued sanity hit (-15).", 1.8f);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            emitSoundEventPulse(SoundEvent.CLAP_SHOUT, 0.95f);
            showRuntimeSubtitle("Injected loud sound pulse.", 1.8f);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            if (storySystem != null) {
                showRuntimeSubtitle("Story: " + storySystem.debugStatus(), 2.0f);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            if (debugConsole != null) {
                debugConsole.executeLine("settings", new io.github.superteam.resonance.event.EventContext(
                    camera.position, camera.position, elapsedSeconds, soundPropagationOrchestrator, null, null, null, this::showRuntimeSubtitle));
            }
        }

        // Remove runtime multiplayer debug shortcuts; H/C are reserved for menu and crouch.

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

        playerController.getPosition(playerPos);
        updateRuntimeDoorTest(deltaSeconds);
        if (eventTriggerRuntime != null) {
            eventTriggerRuntime.update(deltaSeconds, playerPos, elapsedSeconds, soundPropagationOrchestrator);
        }

        behaviorSystem.captureFrameState(
            playerController.getMovementState(),
            flashlightController != null && flashlightController.isEnabled(),
            sanitySystem.getSanity(),
            lightManager.exposureAt(playerPos),
            sanitySystem.getLastDeltaThisFrame()
        );
        playerFeatureExtractor.update(deltaSeconds, playerController);
        footstepSystem.update(deltaSeconds, elapsedSeconds);
        soundPropagationOrchestrator.update(deltaSeconds);
        updateSonarRevealSnapshot();
        handleMicInput(deltaSeconds);

        String listenerNodeId = findNearestNodeId(playerPos);
        spatialCueController.setListenerNode(listenerNodeId, playerPos);

        if (runtimeSubtitleRemaining > 0f) {
            runtimeSubtitleRemaining = Math.max(0f, runtimeSubtitleRemaining - deltaSeconds);
            if (runtimeSubtitleRemaining <= 0f) {
                runtimeSubtitleText = null;
            }
        }

        // Fix H — move the atmospheric mist emitter to the player's current feet
        // position each frame.
        if (mistEffect != null) {
            mistEffect.setPosition(playerPos.x, playerPos.y, playerPos.z);
        }

        lastSoundIntensity = Math.max(0f, lastSoundIntensity - (deltaSeconds * 0.9f));
        directorController.update(deltaSeconds);
        behaviorSystem.update(deltaSeconds, lightManager, sanitySystem, jumpScareDirector);

        DirectorController.DirectorSnapshot directorSnapshot = directorController.snapshot();
        lightManager.setTier(toLightingTier(directorSnapshot.currentTier()));
        lightManager.update(deltaSeconds);

        panicModel.setThreatDistanceMeters(30f);
        panicModel.setLoudSoundIntensity(lastSoundIntensity);
        // IMPROVE-09 — placeholder: always full health until enemy damage is wired up.
        // TODO: Replace with actual player health once EnemyController is implemented.
        panicModel.setHealth(100f, 100f);
        panicModel.update(deltaSeconds);

        float estimatedEnemyDistance = MathUtils.lerp(20f, 2f, panicModel.currentPanicLevel());
        SanitySystem.Context sanityContext = new SanitySystem.Context(
            playerPos,
            lightManager.exposureAt(playerPos),
            estimatedEnemyDistance,
            lastSoundIntensity,
            elapsedSeconds
        );
        sanitySystem.update(deltaSeconds, sanityContext);
        applySanityDrivenBodyCamEffects();

        // Dialogue and room-transition updates (Group C/D demos)
        if (dialogueSystem != null) {
            dialogueSystem.update(deltaSeconds);
        }
        if (roomTransitionSystem != null) {
            roomTransitionSystem.update(deltaSeconds, dialogueSystem);
        }

        float tierFactor = switch (directorSnapshot.currentTier()) {
            case CALM -> 0.15f;
            case TENSE -> 0.55f;
            case PANICKED -> 1.0f;
        };
        jumpScareDirector.update(deltaSeconds, sanitySystem.getSanity(), tierFactor, lastSoundIntensity);
        JumpScare triggeredScare = jumpScareDirector.pollTriggeredScare();
        if (triggeredScare != null) {
            showRuntimeSubtitle("[Scare] " + triggeredScare.message(), 1.8f);
            if (triggeredScare.type() == io.github.superteam.resonance.scare.ScareType.FLASH
                    || triggeredScare.type() == io.github.superteam.resonance.scare.ScareType.ENVIRONMENT) {
                blindEffectController.triggerFlareReveal();
            }
            sanityEventDrain.queueDelta(-1.25f * triggeredScare.intensity());
        }

        blindEffectController.update(deltaSeconds);

        if (multiplayerManager.getRole() != MultiplayerManager.Role.OFFLINE) {
            tryStartVoiceCapture();
            multiplayerManager.processPendingEvents(this);
            processVoiceQueue();

            positionBroadcastTimer += deltaSeconds;
            if (positionBroadcastTimer >= 0.05f) {
                positionBroadcastTimer = 0f;
                multiplayerManager.broadcastPosition(camera.position, camera.direction);
            }

            for (RemotePlayer rp : multiplayerManager.remotePlayers.values()) {
                rp.update(deltaSeconds);
                if (rp.speakingTimer > 0) {
                    rp.speakingTimer -= deltaSeconds;
                    if (rp.speakingTimer <= 0) {
                        rp.isSpeaking = false;
                    }
                }
            }
        }
    }

    private boolean tryInteractRuntimeDoor() {
        if (!isFacingRuntimeDoor() || runtimeDoorController == null) {
            return false;
        }

        runtimeDoorController.onInteract(null);
        emitSoundEventPulse(SoundEvent.DOOR_SLAM, 0.48f);
        showRuntimeSubtitle("Door runtime test toggled.", 1.0f);
        return true;
    }

    private boolean isFacingRuntimeDoor() {
        if (runtimeDoorController == null) {
            return false;
        }

        runtimeDoorFaceTarget.set(runtimeDoorHingePosition).sub(camera.position);
        float distanceSquared = runtimeDoorFaceTarget.len2();
        if (distanceSquared <= 0.0001f || distanceSquared > (DOOR_INTERACT_RANGE * DOOR_INTERACT_RANGE)) {
            return false;
        }

        float distance = (float) Math.sqrt(distanceSquared);
        runtimeDoorFaceTarget.scl(1f / distance);
        float facingDot = runtimeDoorFaceTarget.dot(tmpItemDirection.set(camera.direction).nor());
        return facingDot >= DOOR_INTERACT_FACING_DOT;
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
        if (multiplayerManager != null
                && multiplayerManager.getRole() != MultiplayerManager.Role.OFFLINE
                && voiceCaptureStarted) {
            micEnabled = false;
            Gdx.app.log("Mic", "Realtime mic toggle is disabled in multiplayer while voice capture is active");
            return;
        }

        if (micEnabled) {
            micEnabled = false;
            if (realtimeMicSystem != null) {
                realtimeMicSystem.stop();
                Gdx.app.log("Mic", "Disabled");
            }
            return;
        }

        long nowMillis = TimeUtils.millis();
        if (nowMillis - lastMicStartAttemptMillis < MIC_START_RETRY_COOLDOWN_MS) {
            return;
        }
        lastMicStartAttemptMillis = nowMillis;

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
        int[] sampleRates = { 44100, 22050, 16000 };
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
            // IMPROVE-08 â€” soft acoustic ping on zone enter: reveals new zone nodes and
            // hints to Director.
            emitItemEvent(SoundEvent.FOOTSTEP, playerPos, 0.15f);
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
        if (voiceCaptureStarted && voiceCapture != null) {
            float normalizedLevel = voiceCapture.getNormalizedRms(3000f);
            lastMicLevelNormalized = MathUtils.clamp(normalizedLevel, 0f, 1f);
            pushMicLevelSample(lastMicLevelNormalized);
            return;
        }

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
                elapsedSeconds);

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
                elapsedSeconds);

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
        renderRemotePlayers(deltaSeconds);

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
                blindEffectController.fogColor());
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

        if (runtimeDoorMesh != null && runtimeDoorController != null) {
            worldShader.setUniformf("u_baseColor", 0.78f, 0.68f, 0.36f);
            worldShader.setUniformMatrix("u_modelTrans", runtimeDoorTransform);
            runtimeDoorMesh.render(worldShader, GL20.GL_TRIANGLES);
            worldShader.setUniformf("u_baseColor", 0.68f, 0.74f, 0.80f);
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
                        camera.position.z + tmpViewModelOffset.z)
                .rotate(Vector3.Y, playerController.getYaw())
                .rotate(Vector3.X, -playerController.getPitch())
                .scale(VIEW_MODEL_SCALE, VIEW_MODEL_SCALE, VIEW_MODEL_SCALE);

        // IMPROVE-10 â€” clear depth so the view model renders in front of world geometry
        // but is still subject to HUD compositing. Avoids depth-test disable artifacts.
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
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
                blindEffectController.fogColor());
        // INCON-06 / Fix L â€” use per-item-type mesh; fall back to unit cube for unknown
        // types.
        Mesh viewModelMesh = viewModelMeshes.getOrDefault(heldItem.definition().itemType(), playerViewModelMesh);
        viewModelMesh.render(playerShaderProgram, GL20.GL_TRIANGLES);
        // Depth test was already enabled above; nothing to restore here.
    }

    private void renderRemotePlayers(float delta) {
        if (multiplayerManager.getRole() == MultiplayerManager.Role.OFFLINE)
            return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (RemotePlayer remote : multiplayerManager.remotePlayers.values()) {
            if (remote.id == multiplayerManager.getLocalPlayerId()) {
                continue;
            }
            remote.render(shapeRenderer);
        }
        shapeRenderer.end();
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
        drainJoinBanners();

        CarriableItem focusedCarriable = carrySystem.isHoldingItem() ? null : findNearestFacingCarriable();
        ConsumablePickup focusedConsumable = carrySystem.isHoldingItem() || focusedCarriable != null
                ? null
                : findNearestFacingConsumable();
        boolean doorTargetVisible = !carrySystem.isHoldingItem()
            && runtimeDoorKnobVisible;

        shapeRenderer.setProjectionMatrix(hudProjection);
        renderCrosshair(doorTargetVisible || focusedCarriable != null || focusedConsumable != null);
        renderInventoryBar();
        drawControlsLegend();

        drawInteractionPrompt(focusedCarriable, focusedConsumable, doorTargetVisible);
        drawRuntimeSubtitle();

        if (dialogueSystem != null) dialogueSystem.render(hudBatch);
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
                (micEnabled && realtimeMicSystem != null && realtimeMicSystem.isActive())
                    || (voiceCaptureStarted && lastMicLevelNormalized > 0.02f),
                System.currentTimeMillis() - lastVoicePlaybackMillis < VOICE_OUTPUT_RECENT_WINDOW_MS,
                multiplayerManager);

            renderJoinBanners();

            // Render room transition fade overlay on top
            if (roomTransitionSystem != null && roomTransitionSystem.fadeAlpha() > 0f) {
                float a = roomTransitionSystem.fadeAlpha();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0f, 0f, 0f, a);
                shapeRenderer.rect(0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                shapeRenderer.end();
            }
        }

    private void drawRuntimeSubtitle() {
        if (runtimeSubtitleRemaining <= 0f || runtimeSubtitleText == null || runtimeSubtitleText.isBlank()) {
            return;
        }

        float alpha = MathUtils.clamp(runtimeSubtitleRemaining / 0.6f, 0f, 1f);
        float y = Math.max(96f, Gdx.graphics.getHeight() * 0.24f);
        float x = (Gdx.graphics.getWidth() * 0.5f) - (runtimeSubtitleText.length() * 2.95f);

        hudBatch.begin();
        hudFont.setColor(0.93f, 0.97f, 1.0f, alpha);
        hudFont.draw(hudBatch, runtimeSubtitleText, x, y);
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        hudBatch.end();
    }

    private void drainJoinBanners() {
        if (multiplayerManager == null) {
            return;
        }

        String banner;
        while ((banner = multiplayerManager.pendingJoinBanners.poll()) != null) {
            joinBanners.add(new JoinBanner(banner, 3.0f));
        }
    }

    private void renderJoinBanners() {
        if (joinBanners.isEmpty()) {
            return;
        }

        float delta = Gdx.graphics.getDeltaTime();
        for (int i = joinBanners.size - 1; i >= 0; i--) {
            JoinBanner banner = joinBanners.get(i);
            banner.remainingSeconds -= delta;
            if (banner.remainingSeconds <= 0f) {
                joinBanners.removeIndex(i);
            }
        }

        if (joinBanners.isEmpty()) {
            return;
        }

        hudBatch.begin();
        float x = 16f;
        float y = Gdx.graphics.getHeight() - 40f;
        for (int i = 0; i < joinBanners.size; i++) {
            JoinBanner banner = joinBanners.get(i);
            float alpha = MathUtils.clamp(banner.remainingSeconds / 3.0f, 0f, 1f);
            hudFont.setColor(0.88f, 0.95f, 1.0f, alpha);
            hudFont.draw(hudBatch, banner.text, x, y - (i * 22f));
        }
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        hudBatch.end();
    }

        // (Dialogue and transition overlays can be rendered here.)

    private void drawInteractionPrompt(CarriableItem focusedCarriable, ConsumablePickup focusedConsumable,
            boolean doorTargetVisible) {
        String promptText = null;
        if (carrySystem.isHoldingItem()) {
            promptText = "[RMB] Throw   [F] Drop   [TAB] Stash";
        } else {
            if (doorTargetVisible && interactionPrompt != null && !interactionPrompt.isBlank()) {
                promptText = interactionPrompt;
            } else if (focusedCarriable != null) {
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

        if (itemDestroyedMessageRemaining <= 0f || lastItemDestroyedMessage == null
                || lastItemDestroyedMessage.isBlank()) {
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

        // IMPROVE-01 â€” reuse the pre-allocated callback; reset hit state each call.
        if (cachedRayCallback == null) {
            cachedRayCallback = new ClosestRayResultCallback(camera.position, tmpRayEnd);
        }
        cachedRayCallback.setRayFromWorld(camera.position);
        cachedRayCallback.setRayToWorld(tmpRayEnd);
        cachedRayCallback.setCollisionObject(null);
        cachedRayCallback.setClosestHitFraction(1f);
        physicsWorld.rayTest(camera.position, tmpRayEnd, cachedRayCallback);
        if (!cachedRayCallback.hasHit()) {
            return null;
        }
        return resolveCarriableFromCollisionObject(cachedRayCallback.getCollisionObject());
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

            shapeRenderer.setColor(slotIndex == inventorySystem.getActiveSlotIndex() ? 0.95f : 0.28f,
                    slotIndex == inventorySystem.getActiveSlotIndex() ? 0.72f : 0.30f,
                    slotIndex == inventorySystem.getActiveSlotIndex() ? 0.22f : 0.34f, 0.92f);
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
        String line3 = "[V] Sonar pulse (keyboard)     [F6] Toggle mic     [F1] Toggle graph debug";
        String line4 = "[F2] Toggle blind radius debug [F4] Toggle bounce rays  [F5] Toggle VHS shader";
        String line5 = "[F3] Reload configs [F7] Trigger flare [F8] Cycle debug tab [F9/F10] Switch screens";
        String line6 = "Door test: (" + (int) HUB_X + ", " + (int) DOOR_TEST_Z + ")  Crouch: ("
            + (int) HUB_X + ", " + (int) (HUB_Z - ZONE_RING_OFFSET) + ")  Sound: (" + (int) HUB_X + ", "
            + (int) (HUB_Z + ZONE_RING_OFFSET) + ")";
        String line7 = "[F12] Debug event flag: " + (eventTriggerRuntime != null && eventTriggerRuntime.flag("debug.event.flag") ? "ON" : "OFF");
        String line8 = "[Zone] Sound event flag: " + (eventTriggerRuntime != null && eventTriggerRuntime.flag("zone.sound.entered") ? "ON" : "OFF");
        String line9 = "[Zone] Event chain flag: " + (eventTriggerRuntime != null && eventTriggerRuntime.flag("zone.sound.chain.completed") ? "ON" : "OFF");
        String line10 = directorController.snapshot().hudLine();
        String line11 = "[Door] Runtime test state: "
            + (runtimeDoorController != null && runtimeDoorController.currentAngle() > 40f ? "OPEN" : "CLOSED");
        String line12 = "[Light] Tier=" + lightManager.tier() + "  Count=" + lightManager.lightCount()
            + "  Exposure=" + String.format("%.2f", lightManager.exposureAt(playerPos));
        String line13 = "[Sanity] " + String.format("%.1f", sanitySystem.getSanity())
            + "  Tier=" + sanitySystem.tier()
            + "  Halluc=" + String.format("%.2f", sanityHallucinationEffect.pressure());
        String line14 = "[Scare] Tension=" + String.format("%.1f", jumpScareDirector.tension())
            + "  Cooldown=" + String.format("%.1fs", jumpScareDirector.cooldownRemaining());
        String line15 = "[Settings] FOV=" + settingsSystem.data().targetFov
            + " Sens=" + String.format("%.2f", settingsSystem.data().mouseSensitivity)
            + " Vol=" + String.format("%.2f/%.2f/%.2f",
                settingsSystem.data().masterVolume,
                settingsSystem.data().musicVolume,
                settingsSystem.data().sfxVolume);
        String line16 = "[B/N/J/H/D] Subtitle / sanity hit / loud pulse / story status / settings";
        String line17 = behaviorSystem == null ? "[Behavior] UNWIRED" : behaviorSystem.debugLine();

        float padding = 12.0f;
        float lineHeight = 14.0f;
        float startY = Gdx.graphics.getHeight() - padding;

        float textWidth = Math.max(
                hudFont.getRegion().getRegionWidth(),
                Math.max(line1.length(), Math.max(line2.length(),
                    Math.max(line3.length(), Math.max(line4.length(), Math.max(line5.length(), Math.max(line6.length(), Math.max(line7.length(), Math.max(line8.length(), Math.max(line9.length(), line10.length())))))))))
                        * 6.2f);
        textWidth = Math.max(textWidth, line11.length() * 6.2f);
        textWidth = Math.max(textWidth, line12.length() * 6.2f);
        textWidth = Math.max(textWidth, line13.length() * 6.2f);
        textWidth = Math.max(textWidth, line14.length() * 6.2f);
        textWidth = Math.max(textWidth, line15.length() * 6.2f);
        textWidth = Math.max(textWidth, line16.length() * 6.2f);
        textWidth = Math.max(textWidth, line17.length() * 6.2f);
        float startX = Gdx.graphics.getWidth() - textWidth - 16.0f;

        hudBatch.begin();
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.72f);
        hudFont.draw(hudBatch, line1, startX, startY);
        hudFont.draw(hudBatch, line2, startX, startY - lineHeight);
        hudFont.draw(hudBatch, line3, startX, startY - (lineHeight * 2f));
        hudFont.draw(hudBatch, line4, startX, startY - (lineHeight * 3f));
        hudFont.draw(hudBatch, line5, startX, startY - (lineHeight * 4f));
        hudFont.draw(hudBatch, line6, startX, startY - (lineHeight * 5f));
        hudFont.draw(hudBatch, line7, startX, startY - (lineHeight * 6f));
        hudFont.draw(hudBatch, line8, startX, startY - (lineHeight * 7f));
        hudFont.draw(hudBatch, line9, startX, startY - (lineHeight * 8f));
        hudFont.draw(hudBatch, line10, startX, startY - (lineHeight * 9f));
        hudFont.draw(hudBatch, line11, startX, startY - (lineHeight * 10f));
        hudFont.draw(hudBatch, line12, startX, startY - (lineHeight * 11f));
        hudFont.draw(hudBatch, line13, startX, startY - (lineHeight * 12f));
        hudFont.draw(hudBatch, line14, startX, startY - (lineHeight * 13f));
        hudFont.draw(hudBatch, line15, startX, startY - (lineHeight * 14f));
        hudFont.draw(hudBatch, line16, startX, startY - (lineHeight * 15f));
        hudFont.draw(hudBatch, line17, startX, startY - (lineHeight * 16f));
        hudFont.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        hudBatch.end();
    }

    private void showRuntimeSubtitle(String text, float durationSeconds) {
        if (text == null || text.isBlank()) {
            return;
        }
        runtimeSubtitleText = text;
        runtimeSubtitleRemaining = Math.max(0.1f, durationSeconds);
    }

    private void fireDebugEventFlag() {
        playerController.getPosition(playerPos);
        fireEventById("debug-set-flag", playerPos);
    }

    private void fireEventById(String eventId, Vector3 triggerPosition) {
        if (eventTriggerRuntime == null || eventId == null || eventId.isBlank()) {
            return;
        }

        Vector3 resolvedTrigger = triggerPosition == null ? playerPos : triggerPosition;
        eventTriggerRuntime.fireEvent(eventId, resolvedTrigger, playerPos, elapsedSeconds, soundPropagationOrchestrator);
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

    public void triggerSoundPulse(Vector3 worldPosition, float intensity) {
        Packets.SoundEventPacket packet = new Packets.SoundEventPacket();
        packet.position = new Vector3(worldPosition);
        packet.strength = intensity;
        packet.eventType = SoundEvent.CLAP_SHOUT.ordinal();
        applyRemoteSoundEvent(packet);
    }

    public void applyRemoteSoundEvent(Packets.SoundEventPacket packet) {
        if (packet == null || packet.position == null) {
            return;
        }

        Gdx.app.postRunnable(() -> {
            SoundEvent eventType = SoundEvent.CLAP_SHOUT;
            SoundEvent[] values = SoundEvent.values();
            if (packet.eventType >= 0 && packet.eventType < values.length) {
                eventType = values[packet.eventType];
            }

            Vector3 pulsePos = new Vector3(packet.position);
            String sourceNode = findNearestNodeId(pulsePos);
            SoundEventData eventData = new SoundEventData(
                    eventType,
                    sourceNode,
                    pulsePos,
                    Math.max(0f, packet.strength),
                    elapsedSeconds);

            fireSoundPulseVisual(pulsePos, eventData.baseIntensity());
            PropagationResult result = soundPropagationOrchestrator.emitSoundEvent(eventData, elapsedSeconds);
            cacheRevealedNodesForFlash(result);
        });
    }

    private void firePlayerPulse() {
        float intensity = SoundEvent.CLAP_SHOUT.defaultBaseIntensity();
        emitSoundEventPulse(SoundEvent.CLAP_SHOUT, intensity);

        if (multiplayerManager.getRole() != MultiplayerManager.Role.OFFLINE) {
            Packets.SoundEventPacket packet = new Packets.SoundEventPacket();
            packet.sourcePlayerId = multiplayerManager.getLocalPlayerId();
            packet.position = new Vector3(camera.position);
            packet.strength = intensity;
            packet.eventType = SoundEvent.CLAP_SHOUT.ordinal();
            multiplayerManager.broadcastSoundEvent(packet);
        }
    }

    private void spawnInitialCarriableItems() {
        spawnCarriableItem(ItemType.GLASS_BOTTLE, HUB_X + 1.5f, 0.5f, HUB_Z + 1.0f, 0.15f, 0.55f, 0.15f);
        spawnCarriableItem(ItemType.METAL_PIPE, HUB_X - 1.5f, 0.5f, HUB_Z + 1.0f, 0.18f, 0.90f, 0.18f);
        spawnCarriableItem(ItemType.CARDBOARD_BOX, HUB_X, 0.5f, HUB_Z - 2.0f, 0.45f, 0.45f, 0.45f);
    }

    private void spawnInitialConsumablePickups() {
        worldConsumablePickups.add(new ConsumablePickup(ItemType.FLARE, new Vector3(HUB_X + 5f, 0.2f, HUB_Z + 5f)));
        worldConsumablePickups
                .add(new ConsumablePickup(ItemType.NOISE_DECOY, new Vector3(HUB_X - 5f, 0.2f, HUB_Z - 5f)));
    }

    private void spawnCarriableItem(
            ItemType itemType,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth) {
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
            btDefaultMotionState motionState) {
        tmpInertia.setZero();
        collisionShape.calculateLocalInertia(definition.mass(), tmpInertia);

        btRigidBodyConstructionInfo constructionInfo = new btRigidBodyConstructionInfo(
            definition.mass(),
            motionState,
            collisionShape,
            tmpInertia);
        btRigidBody rigidBody = new btRigidBody(constructionInfo);
        carriableConstructionInfos.add(constructionInfo);

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
                thrownItem.setWorldPosition(
                        tmpDropPosition.set(camera.position).mulAdd(tmpItemDirection, 0.8f).add(0f, 0.1f, 0f));
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
            InventorySystem.InventorySlotEntry removedEntry = inventorySystem
                    .removeItem(inventorySystem.getActiveSlotIndex());
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
                            0.25f);
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
        // INCON-05 â€” consumable pickup now requires the same facing check as carriable
        // pickup.
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
            // INCON-05 fix: require INTERACTION_FACING_DOT (0.70) facing check, same as
            // carriable items.
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
            emitItemEvent(SoundEvent.NOISE_DECOY, tmpDropPosition.set(camera.position).mulAdd(camera.direction, 4.0f),
                    0.70f);
        } else if (itemDefinition.itemType() == ItemType.FLARE) {
            blindEffectController.triggerFlareReveal();
            emitItemEvent(SoundEvent.OBJECT_DROP_OR_BREAK, tmpDropPosition.set(camera.position), 0.30f);
        }

        inventorySystem.consumeActiveItem();
    }

    private void processBulletImpacts() {
        ObjectMap<CarriableItem, Float> strongestImpulseByItem = new ObjectMap<>();
        ObjectMap<CarriableItem, Vector3> impactPointByItem = new ObjectMap<>();

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
            boolean hasRealContact = false;
            for (int contactIndex = 0; contactIndex < contactCount; contactIndex++) {
                btManifoldPoint contactPoint = manifold.getContactPoint(contactIndex);
                if (contactPoint.getDistance() <= 0f) {
                    hasRealContact = true;
                }
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

            if (!hasImpact && hasRealContact) {
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
                Float strongestImpulse = strongestImpulseByItem.get(carriableItem);
                if (strongestImpulse == null || maxImpulse > strongestImpulse) {
                    strongestImpulseByItem.put(carriableItem, maxImpulse);
                    impactPointByItem.put(carriableItem, new Vector3(tmpImpulsePoint));
                }
            }
        }

        for (ObjectMap.Entry<CarriableItem, Float> entry : strongestImpulseByItem.entries()) {
            CarriableItem carriableItem = entry.key;
            Vector3 impactPoint = impactPointByItem.get(carriableItem);
            if (carriableItem == null || impactPoint == null) {
                continue;
            }

            PropagationResult propagationResult = impactListener.onCarriableImpact(
                    carriableItem,
                    impactPoint,
                    entry.value,
                    elapsedSeconds);
            if (propagationResult != null) {
                cacheRevealedNodesForFlash(propagationResult);
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
                elapsedSeconds);
        fireSoundPulseVisual(worldPosition, intensity);
        PropagationResult propagationResult = soundPropagationOrchestrator.emitSoundEvent(eventData, elapsedSeconds);
        cacheRevealedNodesForFlash(propagationResult);

        // Share with other players
        if (multiplayerManager.getRole() != MultiplayerManager.Role.OFFLINE) {
            Packets.SoundEventPacket packet = new Packets.SoundEventPacket();
            packet.sourcePlayerId = multiplayerManager.getLocalPlayerId();
            packet.position = new Vector3(worldPosition);
            packet.strength = intensity;
            packet.eventType = eventType.ordinal();
            multiplayerManager.broadcastSoundEvent(packet);
        }
    }

    private void processVoiceQueue() {
        VoiceChunkPacket chunk;
        while ((chunk = multiplayerManager.pendingVoiceChunks.poll()) != null) {
            if (chunk.sourcePlayerId == multiplayerManager.getLocalPlayerId()) {
                continue;
            }
            RemotePlayer rp = multiplayerManager.remotePlayers.get(chunk.sourcePlayerId);
            if (rp != null) {
                voicePlayback.receiveChunk(chunk, rp, camera.position, camera.direction);
                lastVoicePlaybackMillis = System.currentTimeMillis();
            }
        }
    }

    private void updateHudProjection(int width, int height) {
        hudProjection.setToOrtho2D(0.0f, 0.0f, width, height);
    }

    private static final class JoinBanner {
        private final String text;
        private float remainingSeconds;

        private JoinBanner(String text, float remainingSeconds) {
            this.text = text;
            this.remainingSeconds = remainingSeconds;
        }
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

        if (runtimeDoorMesh != null) {
            runtimeDoorMesh.dispose();
            runtimeDoorMesh = null;
        }
        if (runtimeDoorKnobCollider != null) {
            runtimeDoorKnobCollider.dispose();
            runtimeDoorKnobCollider = null;
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

        for (btRigidBody.btRigidBodyConstructionInfo info : staticConstructionInfos) {
            if (info != null) info.dispose();
        }
        staticConstructionInfos.clear();

        for (btRigidBody.btRigidBodyConstructionInfo info : carriableConstructionInfos) {
            if (info != null) info.dispose();
        }
        carriableConstructionInfos.clear();

        rigidBodyToCarriable.clear();

        if (playerViewModelMesh != null) {
            playerViewModelMesh.dispose();
        }
        // INCON-06/Fix L â€” dispose per-item view model meshes.
        for (Mesh viewMesh : viewModelMeshes.values()) {
            if (viewMesh != null) {
                viewMesh.dispose();
            }
        }
        viewModelMeshes.clear();
        // IMPROVE-01 â€” dispose the cached ray callback.
        if (cachedRayCallback != null) {
            cachedRayCallback.dispose();
            cachedRayCallback = null;
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
        if (voiceCapture != null)
            voiceCapture.dispose();
        if (voicePlayback != null)
            voicePlayback.dispose();
        if (multiplayerManager != null)
            multiplayerManager.dispose();
        if (hudBatch != null) {
            hudBatch.dispose();
        }
        if (hudFont != null) {
            hudFont.dispose();
        }
        if (spatialCueController != null) {
            spatialCueController.dispose();
        }
        if (eventTriggerRuntime != null) {
            eventTriggerRuntime.dispose();
            eventTriggerRuntime = null;
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

    public static final class ConsumablePickup {
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


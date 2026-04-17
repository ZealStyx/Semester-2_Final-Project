package io.github.superteam.resonance.devTest;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.superteam.resonance.sound.AcousticGraphEngine;
import io.github.superteam.resonance.sound.AcousticMaterial;
import io.github.superteam.resonance.sound.DijkstraPathfinder;
import io.github.superteam.resonance.sound.GraphEdge;
import io.github.superteam.resonance.sound.GraphNode;
import io.github.superteam.resonance.sound.MicVolumeLevel;
import io.github.superteam.resonance.sound.PropagationResult;
import io.github.superteam.resonance.sound.RealtimeMicSystem;
import io.github.superteam.resonance.sound.RoomAcousticProfile;
import io.github.superteam.resonance.sound.SonarRenderer;
import io.github.superteam.resonance.sound.SoundBalancingConfig;
import io.github.superteam.resonance.sound.SoundBalancingConfigStore;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;
import io.github.superteam.resonance.sound.SoundPulseVisualizer;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import io.github.superteam.resonance.sound.SpatialCueController;
import io.github.superteam.resonance.sound.WallType;
import java.util.ArrayList;
import java.util.List;

public class SoundTestScreen extends ScreenAdapter {
    private static final float CAMERA_HEIGHT = 10f;
    private static final float CAMERA_MOVE_SPEED = 6f;
    private static final float SOURCE_MOVE_SPEED = 3f;
    private static final float WORLD_BOUNDS = 10.5f;
    private static final float CAMERA_FOLLOW_LERP = 0.12f;

    private final PerspectiveCamera camera;
    private final Stage stage;
    private final Skin skin;
    private final ShapeRenderer shapes;
    private final AcousticGraphEngine acousticGraphEngine;
    private final SoundPulseVisualizer soundPulseVisualizer;
    private final SoundBalancingConfig soundBalancingConfig;
    private final SonarRenderer sonarRenderer;
    private final SpatialCueController spatialCueController;
    private final SoundPropagationOrchestrator soundPropagationOrchestrator;
    private final RealtimeMicSystem realtimeMicSystem;
    private final List<RoomWalls> roomWalls = new ArrayList<>();

    private final Vector3 origin = new Vector3(0f, 0.05f, 0f);
    private final Vector3 cameraPlanarPosition = new Vector3(0f, 0f, 0f);
    private final Vector3 listenerFloorPosition = new Vector3(0f, 0.05f, 0f);

    private boolean micEnabled;
    private float elapsedSeconds;
    private String currentSourceNodeId;

    private Label propagationStatusLabel;
    private Label micStatusLabel;
    private Label micLevelValueLabel;
    private Label micLevelBandLabel;
    private ProgressBar micLevelBar;
    private Texture micLevelBackgroundTexture;
    private Texture micLevelFillTexture;
    private Texture micLevelAccentTexture;

    public SoundTestScreen() {
        int initialWidth = Math.max(1, Gdx.graphics.getWidth());
        int initialHeight = Math.max(1, Gdx.graphics.getHeight());

        camera = new PerspectiveCamera(65f, initialWidth, initialHeight);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.up.set(0f, 0f, -1f);

        shapes = new ShapeRenderer();
        skin = createSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(new InputMultiplexer(stage));

        buildRoomLayout();
        acousticGraphEngine = buildMultiRoomGraph();
        soundPulseVisualizer = new SoundPulseVisualizer(11f, 6f);
        soundBalancingConfig = SoundBalancingConfigStore.loadOrDefault("config/balancing_config.json");
        sonarRenderer = new SonarRenderer();
        spatialCueController = new SpatialCueController();
        spatialCueController.registerSoundAsset(SoundEvent.CLAP_SHOUT, "audio/clap.wav");
        soundPropagationOrchestrator = new SoundPropagationOrchestrator(
            acousticGraphEngine,
            new DijkstraPathfinder(),
            sonarRenderer,
            spatialCueController,
            soundBalancingConfig
        );
        soundPropagationOrchestrator.setListenerRoomAcousticProfile(
            RoomAcousticProfile.fromRoomGeometry(19f, 8f, 2f, AcousticMaterial.CONCRETE)
        );
        realtimeMicSystem = new RealtimeMicSystem(0.14f);

        stage.getViewport().update(initialWidth, initialHeight, true);
        camera.viewportWidth = initialWidth;
        camera.viewportHeight = initialHeight;
        currentSourceNodeId = findNearestNodeId(origin);
        cameraPlanarPosition.set(origin.x, 0f, origin.z);
        updateCamera();
        buildUi();
        triggerSoundPulse(SoundEvent.CLAP_SHOUT, "Auto pulse");
    }

    @Override
    public void render(float delta) {
        handleKeyboardMovement(delta);
        followSourcePoint(delta);
        updateCamera();
        currentSourceNodeId = findNearestNodeId(origin);

        elapsedSeconds += Math.max(0f, delta);
        soundPropagationOrchestrator.update(delta);
        soundPulseVisualizer.update(delta);
        RealtimeMicSystem.Frame micFrame = RealtimeMicSystem.Frame.silent();
        if (micEnabled && realtimeMicSystem.isActive()) {
            micFrame = realtimeMicSystem.update(delta);
            if (micFrame.shouldEmitSignal()) {
                emitMicSignal(micFrame);
            }
        }
        updateMicLevelMeter(micFrame);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.07f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        camera.update();
        listenerFloorPosition.set(cameraPlanarPosition.x, origin.y, cameraPlanarPosition.z);
        String listenerNodeId = findNearestNodeId(listenerFloorPosition);
        spatialCueController.setListenerNode(listenerNodeId, listenerFloorPosition);
        shapes.setProjectionMatrix(camera.combined);

        drawRoom();
        drawGraphOverlay();
        drawSonarRevealOverlay();
        drawOriginMarker();
        drawReflectionOverlay();
        drawPulseOverlay();

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        stage.getViewport().update(width, height, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        updateCamera();
    }

    @Override
    public void dispose() {
        realtimeMicSystem.stop();
        spatialCueController.dispose();
        stage.dispose();
        skin.dispose();
        if (micLevelBackgroundTexture != null) {
            micLevelBackgroundTexture.dispose();
        }
        if (micLevelFillTexture != null) {
            micLevelFillTexture.dispose();
        }
        if (micLevelAccentTexture != null) {
            micLevelAccentTexture.dispose();
        }
        shapes.dispose();
    }

    private void drawSonarRevealOverlay() {
        List<SonarRenderer.SonarRevealView> sonarReveals = sonarRenderer.snapshot();
        if (sonarReveals.isEmpty()) {
            return;
        }

        shapes.begin(ShapeType.Line);
        for (SonarRenderer.SonarRevealView sonarReveal : sonarReveals) {
            GraphNode node = acousticGraphEngine.requireNode(sonarReveal.nodeId());
            Vector3 nodePosition = node.position();
            float alpha = sonarReveal.alpha();
            float intensity = Math.max(0.05f, sonarReveal.intensity());
            float radius = MathUtils.clamp(0.04f + (intensity * 0.12f), 0.04f, 0.2f);
            shapes.setColor(0.4f, 1f, 0.82f, alpha);
            drawCircleXZ(new Vector3(nodePosition.x, nodePosition.y + 0.01f, nodePosition.z), radius, 16);
        }
        shapes.end();
    }

    private void drawRoom() {
        shapes.begin(ShapeType.Line);
        for (RoomWalls roomWall : roomWalls) {
            shapes.setColor(roomWall.wallType.displayColor());
            roomWall.draw(shapes);
        }
        shapes.end();
    }

    private void drawGraphOverlay() {
        List<GraphEdge> graphEdges = acousticGraphEngine.getEdges();
        shapes.begin(ShapeType.Line);
        shapes.setColor(0.23f, 0.78f, 0.8f, 0.55f);
        for (GraphEdge edge : graphEdges) {
            Vector3 fromPosition = acousticGraphEngine.requireNode(edge.fromNodeId()).position();
            Vector3 toPosition = acousticGraphEngine.requireNode(edge.toNodeId()).position();
            shapes.line(
                fromPosition.x,
                fromPosition.y + 0.02f,
                fromPosition.z,
                toPosition.x,
                toPosition.y + 0.02f,
                toPosition.z
            );
        }
        shapes.end();

        shapes.begin(ShapeType.Line);
        for (GraphNode node : acousticGraphEngine.getNodes()) {
            Vector3 nodePosition = node.position();
            if (node.id().equals(currentSourceNodeId)) {
                shapes.setColor(1f, 0.77f, 0.24f, 1f);
                drawCircleXZ(nodePosition, 0.11f, 16);
            } else {
                shapes.setColor(0.36f, 0.88f, 0.95f, 0.92f);
                drawCircleXZ(nodePosition, 0.07f, 12);
            }
        }
        shapes.end();
    }

    private void drawOriginMarker() {
        shapes.begin(ShapeType.Line);
        shapes.setColor(1f, 0.75f, 0.2f, 1f);
        drawCircleXZ(origin, 0.08f, 16);
        shapes.end();
    }

    private void drawReflectionOverlay() {
        List<SoundPulseVisualizer.PulseView> pulseViews = soundPulseVisualizer.pulseViews();
        if (pulseViews.isEmpty()) {
            return;
        }

        shapes.begin(ShapeType.Line);
        for (SoundPulseVisualizer.PulseView pulseView : pulseViews) {
            for (SoundPulseVisualizer.SignalArc signalArc : pulseView.signalArcs()) {
                if (!signalArc.hasHitWall()) {
                    Vector3 currentPosition = signalArc.getCurrentPosition();
                    shapes.setColor(0.42f, 0.96f, 1f, signalArc.wallAlpha());
                    drawCircleXZ(currentPosition, 0.045f, 10);
                    continue;
                }

                Vector3 hitPoint = signalArc.hitPoint();
                Vector3 reflectedPosition = signalArc.getCurrentPosition();
                shapes.setColor(0.92f, 0.86f, 0.45f, 0.82f);
                drawCircleXZ(hitPoint, 0.055f, 12);
                shapes.setColor(1f, 0.68f, 0.30f, signalArc.bounceAlpha());
                drawCircleXZ(reflectedPosition, 0.05f, 10);
            }
        }
        shapes.end();
    }

    private void drawPulseOverlay() {
        List<SoundPulseVisualizer.PulseView> pulseViews = soundPulseVisualizer.pulseViews();
        if (pulseViews.isEmpty()) {
            return;
        }

        shapes.begin(ShapeType.Line);
        for (SoundPulseVisualizer.PulseView pulseView : pulseViews) {
            shapes.setColor(0.4f, 0.9f, 1f, 0.85f);
            drawCircleXZ(pulseView.sourcePosition(), pulseView.radius(), 64);
        }
        shapes.end();

        shapes.begin(ShapeType.Line);
        for (SoundPulseVisualizer.PulseView pulseView : pulseViews) {
            shapes.setColor(0.3f, 0.85f, 1f, 0.9f);
            for (SoundPulseVisualizer.SignalArc signalArc : pulseView.signalArcs()) {
                if (signalArc.hasHitWall()) {
                    drawCircleXZ(signalArc.hitPoint(), 0.06f, 12);
                }
            }
        }
        shapes.end();
    }

    private void buildUi() {
        Table root = new Table(skin);
        root.setFillParent(true);
        root.top().left().pad(8f);

        Label title = new Label("Sound Propagation Test", skin);
        title.setColor(0.9f, 0.75f, 0.45f, 1f);
        root.add(title).left().padBottom(10f);
        root.row();

        TextButton fireButton = new TextButton("Fire Sound Pulse", skin);
        fireButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                triggerSoundPulse(SoundEvent.CLAP_SHOUT, "Manual pulse");
            }
        });
        root.add(fireButton).left().width(180f).height(42f).padBottom(8f);
        root.row();

        TextButton micButton = new TextButton("Toggle Mic", skin);
        micButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleMicInput();
            }
        });
        root.add(micButton).left().width(180f).height(36f).padBottom(8f);
        root.row();

        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Gdx.app.getApplicationListener() instanceof Game) {
                    ((Game) Gdx.app.getApplicationListener()).setScreen(new ParticleTestScreen());
                }
            }
        });
        root.add(backButton).left().width(140f).height(36f).padBottom(10f);
        root.row();

        propagationStatusLabel = new Label("No pulse fired.", skin);
        propagationStatusLabel.setWrap(true);
        root.add(propagationStatusLabel).left().width(320f).padBottom(8f);
        root.row();

        micStatusLabel = new Label("Mic: OFF", skin);
        root.add(micStatusLabel).left().width(320f).padBottom(8f);
        root.row();

        Label instructions = new Label(
            "Arrow keys move the source through the bounce map. The camera follows the source so reflections stay in view.",
            skin
        );
        instructions.setWrap(true);
        root.add(instructions).left().width(320f);
        stage.addActor(root);

        Table micMeterOverlay = new Table(skin);
        micMeterOverlay.setFillParent(true);
        micMeterOverlay.top().right().pad(10f);
        micMeterOverlay.add(new Label("Mic Level", skin)).right().padBottom(4f);
        micMeterOverlay.row();
        micLevelBar = new ProgressBar(0f, 1f, 0.01f, false, createMicLevelBarStyle());
        micLevelBar.setAnimateDuration(0.08f);
        micLevelBar.setValue(0f);
        micMeterOverlay.add(micLevelBar).width(180f).height(18f).right().padBottom(2f);
        micMeterOverlay.row();
        micLevelValueLabel = new Label("0.00", skin);
        micLevelValueLabel.setAlignment(com.badlogic.gdx.utils.Align.right);
        micMeterOverlay.add(micLevelValueLabel).right();
        micMeterOverlay.row();
        micLevelBandLabel = new Label("LOW", skin);
        micLevelBandLabel.setAlignment(com.badlogic.gdx.utils.Align.right);
        micMeterOverlay.add(micLevelBandLabel).right();
        stage.addActor(micMeterOverlay);
    }

    private void buildRoomLayout() {
        roomWalls.add(new RoomWalls(0f, 0f, 8.5f, 2.4f, 5.2f, WallType.CONCRETE));
        roomWalls.add(new RoomWalls(-4.8f, -1.2f, 1.5f, 2.1f, 1.5f, WallType.WOOD));
        roomWalls.add(new RoomWalls(0f, 1.5f, 1.4f, 2.1f, 1.4f, WallType.METAL));
        roomWalls.add(new RoomWalls(4.8f, -1.2f, 1.5f, 2.1f, 1.5f, WallType.GLASS));
        roomWalls.add(new RoomWalls(0f, -2.1f, 2.4f, 1.9f, 0.6f, WallType.FABRIC));
    }

    private AcousticGraphEngine buildMultiRoomGraph() {
        List<GraphNode> graphNodes = new ArrayList<>();
        graphNodes.add(new GraphNode("west", new Vector3(-5.8f, 0.1f, 0f)));
        graphNodes.add(new GraphNode("westUpper", new Vector3(-5.8f, 0.1f, -2.6f)));
        graphNodes.add(new GraphNode("westLower", new Vector3(-5.8f, 0.1f, 2.4f)));
        graphNodes.add(new GraphNode("center", new Vector3(0f, 0.1f, 0f)));
        graphNodes.add(new GraphNode("centerUpper", new Vector3(0f, 0.1f, -2.6f)));
        graphNodes.add(new GraphNode("centerLower", new Vector3(0f, 0.1f, 2.4f)));
        graphNodes.add(new GraphNode("east", new Vector3(5.8f, 0.1f, 0f)));
        graphNodes.add(new GraphNode("eastUpper", new Vector3(5.8f, 0.1f, -2.6f)));
        graphNodes.add(new GraphNode("eastLower", new Vector3(5.8f, 0.1f, 2.4f)));

        List<GraphEdge> graphEdges = new ArrayList<>();
        connect(graphNodes, graphEdges, "west", "westUpper", 0.05f, 0.15f);
        connect(graphNodes, graphEdges, "west", "westLower", 0.05f, 0.15f);
        connect(graphNodes, graphEdges, "center", "centerUpper", 0.05f, 0.15f);
        connect(graphNodes, graphEdges, "center", "centerLower", 0.05f, 0.15f);
        connect(graphNodes, graphEdges, "east", "eastUpper", 0.05f, 0.15f);
        connect(graphNodes, graphEdges, "east", "eastLower", 0.05f, 0.15f);
        connect(graphNodes, graphEdges, "west", "center", 0.0f, 0.4f);
        connect(graphNodes, graphEdges, "center", "east", 0.0f, 0.4f);
        connect(graphNodes, graphEdges, "westUpper", "centerUpper", 0.14f, 0.25f);
        connect(graphNodes, graphEdges, "westLower", "centerLower", 0.14f, 0.25f);
        connect(graphNodes, graphEdges, "centerUpper", "eastUpper", 0.14f, 0.25f);
        connect(graphNodes, graphEdges, "centerLower", "eastLower", 0.14f, 0.25f);

        return new AcousticGraphEngine().buildGraphFromGeometry(graphNodes, graphEdges);
    }

    private void connect(List<GraphNode> nodes, List<GraphEdge> edges, String from, String to, float occlusion, float hallway) {
        GraphNode sourceNode = findNode(nodes, from);
        GraphNode targetNode = findNode(nodes, to);
        edges.add(GraphEdge.between(sourceNode, targetNode, WallType.CONCRETE.acousticMaterial(), occlusion, hallway));
    }

    private GraphNode findNode(List<GraphNode> nodes, String nodeId) {
        for (GraphNode node : nodes) {
            if (node.id().equals(nodeId)) {
                return node;
            }
        }
        throw new IllegalArgumentException("Missing graph node for id: " + nodeId);
    }

    private Skin createSkin() {
        Skin uiSkin = new Skin();
        BitmapFont font = new BitmapFont();
        uiSkin.add("default", font);

        Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 0.08f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        uiSkin.add("background", texture);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = uiSkin.getFont("default");
        buttonStyle.up = new TextureRegionDrawable(new TextureRegion(texture));
        buttonStyle.down = new TextureRegionDrawable(new TextureRegion(texture));
        buttonStyle.over = buttonStyle.up;
        buttonStyle.fontColor = Color.WHITE;
        uiSkin.add("default", buttonStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = uiSkin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        uiSkin.add("default", labelStyle);

        pixmap.dispose();
        return uiSkin;
    }

    private void updateCamera() {
        camera.position.set(cameraPlanarPosition.x, CAMERA_HEIGHT, cameraPlanarPosition.z);
        camera.direction.set(0f, -1f, 0f);
        camera.up.set(0f, 0f, -1f);
        camera.update();
    }

    private void followSourcePoint(float delta) {
        float followAmount = MathUtils.clamp(delta * 4f, 0f, CAMERA_FOLLOW_LERP);
        Vector3 targetCameraPosition = new Vector3(origin.x, 0f, origin.z);
        cameraPlanarPosition.lerp(targetCameraPosition, followAmount);
    }

    private void handleKeyboardMovement(float delta) {
        float clampedDelta = Math.max(0f, delta);
        float cameraStep = CAMERA_MOVE_SPEED * clampedDelta;
        float sourceStep = SOURCE_MOVE_SPEED * clampedDelta;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            cameraPlanarPosition.z -= cameraStep;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            cameraPlanarPosition.z += cameraStep;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            cameraPlanarPosition.x -= cameraStep;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            cameraPlanarPosition.x += cameraStep;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            origin.z -= sourceStep;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            origin.z += sourceStep;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            origin.x -= sourceStep;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            origin.x += sourceStep;
        }

        cameraPlanarPosition.x = MathUtils.clamp(cameraPlanarPosition.x, -WORLD_BOUNDS, WORLD_BOUNDS);
        cameraPlanarPosition.z = MathUtils.clamp(cameraPlanarPosition.z, -WORLD_BOUNDS, WORLD_BOUNDS);
        origin.x = MathUtils.clamp(origin.x, -WORLD_BOUNDS, WORLD_BOUNDS);
        origin.z = MathUtils.clamp(origin.z, -WORLD_BOUNDS, WORLD_BOUNDS);
    }

    private void drawCircleXZ(Vector3 center, float radius, int segments) {
        int clampedSegments = Math.max(8, segments);
        float previousX = center.x + radius;
        float previousZ = center.z;
        for (int i = 1; i <= clampedSegments; i++) {
            float angle = (i * 360f) / clampedSegments;
            float x = center.x + MathUtils.cosDeg(angle) * radius;
            float z = center.z + MathUtils.sinDeg(angle) * radius;
            shapes.line(previousX, center.y, previousZ, x, center.y, z);
            previousX = x;
            previousZ = z;
        }
    }

    private void triggerSoundPulse(SoundEvent soundEvent, String originReason) {
        String nearestNodeId = findNearestNodeId(origin);
        SoundEventData soundEventData = SoundEventData.atNode(soundEvent, nearestNodeId, origin, elapsedSeconds);
        PropagationResult propagationResult = soundPropagationOrchestrator.emitSoundEvent(soundEventData, elapsedSeconds);

        if (propagationResult != null) {
            soundPulseVisualizer.activate(origin);
            propagationStatusLabel.setText(
                originReason
                    + " | Source node: "
                    + nearestNodeId
                    + " | Revealed: "
                    + propagationResult.revealNodeIds().size()
                    + " | Source intensity: "
                    + String.format("%.2f", propagationResult.getIntensityOrZero(nearestNodeId))
            );
        } else {
            propagationStatusLabel.setText(originReason + " | Event on cooldown");
        }
    }

    private String findNearestNodeId(Vector3 worldPosition) {
        String nearestNodeId = null;
        float nearestDistance = Float.POSITIVE_INFINITY;

        for (GraphNode node : acousticGraphEngine.getNodes()) {
            Vector3 nodePosition = node.position();
            float distanceSquared = nodePosition.dst2(worldPosition);
            if (distanceSquared < nearestDistance) {
                nearestDistance = distanceSquared;
                nearestNodeId = node.id();
            }
        }

        return nearestNodeId == null ? "center" : nearestNodeId;
    }

    private void toggleMicInput() {
        if (micEnabled) {
            micEnabled = false;
            realtimeMicSystem.stop();
            micStatusLabel.setText("Mic: OFF");
            resetMicLevelMeter();
            return;
        }

        try {
            realtimeMicSystem.start(16000, 1024);
            micEnabled = realtimeMicSystem.isActive();
            micStatusLabel.setText(micEnabled ? "Mic: ON" : "Mic: unavailable");
            if (!micEnabled) {
                resetMicLevelMeter();
            }
        } catch (Exception microphoneFailure) {
            micEnabled = false;
            micStatusLabel.setText("Mic: unavailable");
            resetMicLevelMeter();
        }
    }

    private void emitMicSignal(RealtimeMicSystem.Frame micFrame) {
        MicVolumeLevel micVolumeLevel = micFrame.sample().volumeLevel();
        float normalizedLoudness = MathUtils.clamp(micFrame.sample().normalizedLevel() / 1.4f, 0f, 1f);
        float baseIntensity = MathUtils.lerp(0.30f, 1.15f, normalizedLoudness);

        String nearestNodeId = findNearestNodeId(origin);
        SoundEventData soundEventData = new SoundEventData(
            SoundEvent.NOISE_DECOY,
            nearestNodeId,
            origin,
            baseIntensity,
            elapsedSeconds
        );
        PropagationResult propagationResult = soundPropagationOrchestrator.emitSoundEvent(soundEventData, elapsedSeconds);
        if (propagationResult != null) {
            soundPulseVisualizer.activate(origin, normalizedLoudness);
            propagationStatusLabel.setText(
                "Mic signal "
                    + micVolumeLevel
                    + " ("
                    + String.format("%.2f", baseIntensity)
                    + ")"
                    + " | Source node: "
                    + nearestNodeId
                    + " | Revealed: "
                    + propagationResult.revealNodeIds().size()
            );
        }
    }

    private void updateMicLevelMeter(RealtimeMicSystem.Frame micFrame) {
        if (micLevelBar == null || micLevelValueLabel == null || micLevelBandLabel == null || micFrame == null) {
            return;
        }

        float rms = micEnabled ? micFrame.sample().rms() : 0f;
        float normalizedLevel = micEnabled ? micFrame.sample().normalizedLevel() : 0f;
        float level = MathUtils.clamp(normalizedLevel / 1.5f, 0f, 1f);
        micLevelBar.setValue(level);
        micLevelValueLabel.setText(String.format("%.2f", rms));
        MicVolumeLevel micVolumeLevel = micEnabled ? micFrame.sample().volumeLevel() : MicVolumeLevel.LOW;
        micLevelBandLabel.setText(micVolumeLevel.name());
    }

    private void resetMicLevelMeter() {
        if (micLevelBar != null) {
            micLevelBar.setValue(0f);
        }
        if (micLevelValueLabel != null) {
            micLevelValueLabel.setText("0.00");
        }
        if (micLevelBandLabel != null) {
            micLevelBandLabel.setText("LOW");
        }
    }

    private ProgressBarStyle createMicLevelBarStyle() {
        ProgressBarStyle progressBarStyle = new ProgressBarStyle();
        micLevelBackgroundTexture = createSolidTexture(new Color(0.10f, 0.12f, 0.16f, 0.85f));
        micLevelFillTexture = createSolidTexture(new Color(0.40f, 0.92f, 1f, 1f));
        micLevelAccentTexture = createSolidTexture(new Color(0.95f, 0.75f, 0.32f, 1f));
        progressBarStyle.background = new TextureRegionDrawable(new TextureRegion(micLevelBackgroundTexture));
        progressBarStyle.knobBefore = new TextureRegionDrawable(new TextureRegion(micLevelFillTexture));
        progressBarStyle.knob = new TextureRegionDrawable(new TextureRegion(micLevelAccentTexture));
        return progressBarStyle;
    }

    private Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static final class RoomWalls {
        private final float centerX;
        private final float centerZ;
        private final float halfWidth;
        private final float halfDepth;
        private final float height;
        private final WallType wallType;

        private RoomWalls(float centerX, float centerZ, float halfWidth, float height, float halfDepth, WallType wallType) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.halfWidth = halfWidth;
            this.halfDepth = halfDepth;
            this.height = height;
            this.wallType = wallType == null ? WallType.CONCRETE : wallType;
        }

        private void draw(ShapeRenderer shapes) {
            float minX = centerX - halfWidth;
            float maxX = centerX + halfWidth;
            float minZ = centerZ - halfDepth;
            float maxZ = centerZ + halfDepth;

            shapes.line(minX, 0f, minZ, maxX, 0f, minZ);
            shapes.line(minX, 0f, maxZ, maxX, 0f, maxZ);
            shapes.line(minX, 0f, minZ, minX, height, minZ);
            shapes.line(maxX, 0f, minZ, maxX, height, minZ);
            shapes.line(minX, 0f, maxZ, minX, height, maxZ);
            shapes.line(maxX, 0f, maxZ, maxX, height, maxZ);

            shapes.line(minX, height, minZ, maxX, height, minZ);
            shapes.line(minX, height, maxZ, maxX, height, maxZ);
            shapes.line(minX, height, minZ, minX, height, maxZ);
            shapes.line(maxX, height, minZ, maxX, height, maxZ);
        }
    }
}

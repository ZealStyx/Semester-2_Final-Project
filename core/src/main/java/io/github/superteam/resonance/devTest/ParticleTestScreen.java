package io.github.superteam.resonance.devTest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.superteam.resonance.particles.ParticleEffect;
import io.github.superteam.resonance.particles.ParticleBlendMode;
import io.github.superteam.resonance.particles.ParticleDefinition;
import io.github.superteam.resonance.particles.ParticleDirectionMode;
import io.github.superteam.resonance.particles.ParticleEmissionShape;
import io.github.superteam.resonance.particles.ParticleEmitter;
import io.github.superteam.resonance.particles.ParticleFormulas;
import io.github.superteam.resonance.particles.ParticleManager;
import io.github.superteam.resonance.particles.PointAttractorField;
import io.github.superteam.resonance.particles.TrailEmitter;
import io.github.superteam.resonance.particles.VectorField;
import io.github.superteam.resonance.particles.ParticlePresetStore;
import java.util.List;

/** Interactive screen for testing particle shaders and JSON presets. */
public class ParticleTestScreen extends ScreenAdapter {
    private final PerspectiveCamera camera;
    private final ShaderProgram shaderProgram;

    private final Stage stage;
    private final Skin skin;

    private ParticleDefinition definition;
    private ParticleEmitter emitter;
    private ParticleEffect previewEffect;
    private ParticleManager particleManager;
    private TrailEmitter trailEmitter;
    private final Vector3 trailHeadPosition = new Vector3();
    private float trailPhase;

    private Slider emissionSlider;
    private Slider lifetimeSlider;
    private Slider speedSlider;
    private Slider sizeSlider;
    private Slider emissiveSlider;
    private Slider radiusSlider;
    private Slider randomSlider;
    private Slider chaosSlider;
    private Slider multiDirectionSlider;
    private Slider directionXSlider;
    private Slider directionYSlider;
    private Slider directionZSlider;
    private Slider burstCountSlider;
    private Slider coneHalfAngleSlider;
    private Slider torusMajorRadiusSlider;
    private Slider torusMinorRadiusSlider;
    private Slider ringAxisXSlider;
    private Slider ringAxisYSlider;
    private Slider ringAxisZSlider;
    private Slider startAlphaSlider;
    private Slider endAlphaSlider;
    private Slider colorRSlider;
    private Slider colorGSlider;
    private Slider colorBSlider;

    private CheckBox normalsToggle;
    private CheckBox collisionToggle;
    private CheckBox randomToggle;
    private CheckBox chaosToggle;
    private CheckBox multiDirectionToggle;
    private CheckBox retroToggle;

    private TextField presetField;
    private Label blendLabel;
    private Label shapeLabel;
    private Label directionLabel;
    private Label rgbLabel;
    private Label statusLabel;
    private Label activeCountLabel;

    private boolean suppressAdvancedCallbacks;

    private static final float PANEL_WIDTH = 268f;
    private static final float SLIDER_WIDTH = 212f;
    private static final float FIELD_WIDTH = 212f;
    private static final float DOUBLE_BUTTON_WIDTH = 102f;
    private static final float TRIPLE_BUTTON_WIDTH = 68f;
    private static final float FIVE_BUTTON_WIDTH = 36f;

    public ParticleTestScreen() {
        camera = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(3.6f, 2.4f, 4.2f);
        camera.lookAt(0f, 0.4f, 0f);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        ShaderProgram.pedantic = false;
        shaderProgram = new ShaderProgram(
            Gdx.files.internal("shaders/vert/particle_shader.vert"),
            Gdx.files.internal("shaders/frag/particle_shader.frag")
        );
        if (!shaderProgram.isCompiled()) {
            throw new IllegalStateException("Particle shader failed to compile: " + shaderProgram.getLog());
        }

        ParticlePresetStore.ensureLocalPresetFromInternal("default", "particles/presets/default.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("smoke", "particles/presets/smoke.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("mist", "particles/presets/mist.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("explosion", "particles/presets/explosion.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("ring_shot", "particles/presets/ring_shot.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("sonar_pulse", "particles/presets/sonar_pulse.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("smoke_puff", "particles/presets/smoke_puff.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("spiral", "particles/presets/spiral.json");
        ParticlePresetStore.ensureLocalPresetFromInternal("fire", "particles/presets/fire.json");
        ParticlePresetStore.ensureLocalEffectBundleFromInternal("fireball", "particles/effects/fireball.json");
        definition = ParticlePresetStore.loadLocalPreset("default", ParticleDefinition.createDefault());

        emitter = new ParticleEmitter(definition);
        previewEffect = new ParticleEffect("Particle Preview");
        previewEffect.addEmitter(emitter);
        previewEffect.setPosition(0f, 0.35f, 0f);
        particleManager = new ParticleManager();
        particleManager.addEffect(previewEffect);
        particleManager.addForceField(new VectorField(new Vector3(0.18f, 0f, 0.08f), 0.22f));
        particleManager.addForceField(new PointAttractorField(new Vector3(0f, 0.6f, 0f), 0.7f).setFalloff(1.5f));
        trailEmitter = new TrailEmitter(96);
        trailEmitter.setWidth(0.22f, 0.03f);
        trailEmitter.setSampleDistance(0.04f);
        trailEmitter.setPointLifetime(0.95f);
        trailEmitter.setColors(new Color(0.8f, 0.9f, 1f, 0.95f), new Color(0.2f, 0.35f, 1f, 0f));
        particleManager.addTrailEmitter(trailEmitter);

        skin = createSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        buildUi();
        syncUiFromDefinition();
    }

    @Override
    public void render(float delta) {
        camera.update();
        trailPhase += delta;
        trailHeadPosition.set(
            MathUtils.cos(trailPhase * 1.2f) * 1.15f,
            0.95f + (MathUtils.sin(trailPhase * 1.8f) * 0.18f),
            MathUtils.sin(trailPhase * 1.2f) * 1.15f
        );
        trailEmitter.setHeadPosition(trailHeadPosition);
        particleManager.update(delta, camera);

        ScreenUtils.clear(0.03f, 0.02f, 0.06f, 1f, true);
        particleManager.render(shaderProgram, camera);

        activeCountLabel.setText("Active particles: " + particleManager.getActiveParticleCount());

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        particleManager.dispose();
        shaderProgram.dispose();
        stage.dispose();
        skin.dispose();
    }

    private void buildUi() {
        Table leftTable = createPanelTable();
        Table rightTable = createPanelTable();
        ScrollPane leftScrollPane = createPanelScroll(leftTable);
        ScrollPane rightScrollPane = createPanelScroll(rightTable);

        buildLeftPanel(leftTable);
        buildRightPanel(rightTable);

        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.pad(6f);

        Label header = new Label("Particle Command Test", skin);
        header.setColor(new Color(1f, 0.75f, 0.3f, 1f));
        header.setFontScale(0.92f);
        root.add(header).left().colspan(3).expandX().fillX().padBottom(4f);
        root.row();

        root.add(leftScrollPane).top().left().width(PANEL_WIDTH).expandY().fillY().padRight(8f);
        root.add().expandX().fillX();
        root.add(rightScrollPane).top().right().width(PANEL_WIDTH).expandY().fillY().padLeft(8f);

        stage.addActor(root);
    }

    private ScrollPane createPanelScroll(Table contentTable) {
        ScrollPane scrollPane = new ScrollPane(contentTable);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsVisible(true);
        scrollPane.setOverscroll(false, false);
        scrollPane.setSmoothScrolling(true);
        return scrollPane;
    }

    private Table createPanelTable() {
        Table panelTable = new Table(skin);
        panelTable.top().left();
        panelTable.defaults().pad(1.5f);
        return panelTable;
    }

    private void buildLeftPanel(Table table) {
        emissionSlider = createSlider(0f, 260f, 1f);
        addSliderRow(table, "Emission", emissionSlider, value -> {
            definition.emissionRate = value;
            definition.sanitize();
        });

        lifetimeSlider = createSlider(0.4f, 5f, 0.1f);
        addSliderRow(table, "Lifetime Max", lifetimeSlider, value -> {
            definition.lifetimeMax = value;
            definition.lifetimeMin = Math.max(0.2f, value * 0.55f);
            definition.sanitize();
        });

        speedSlider = createSlider(0.2f, 8f, 0.1f);
        addSliderRow(table, "Speed Max", speedSlider, value -> {
            definition.speedMax = value;
            definition.speedMin = Math.max(0f, value * 0.4f);
            definition.sanitize();
        });

        sizeSlider = createSlider(0.05f, 0.5f, 0.01f);
        addSliderRow(table, "Start Size Max", sizeSlider, value -> {
            definition.startSizeMax = value;
            definition.startSizeMin = Math.max(0.005f, value * 0.4f);
            definition.endSizeMax = Math.max(0.005f, value * 0.55f);
            definition.endSizeMin = Math.max(0.005f, definition.endSizeMax * 0.3f);
            definition.sanitize();
        });

        radiusSlider = createSlider(0f, 3f, 0.05f);
        addSliderRow(table, "Spawn Radius", radiusSlider, value -> {
            definition.spawnRadius = value;
            definition.sanitize();
        });

        emissiveSlider = createSlider(0f, 4f, 0.1f);
        addSliderRow(table, "Emissive", emissiveSlider, value -> {
            definition.emissiveStrength = value;
            definition.sanitize();
        });

        rgbLabel = new Label("RGB: 255, 166, 51", skin);
        table.add(rgbLabel).left().colspan(2).padTop(2f);
        table.row();

        colorRSlider = createSlider(0f, 255f, 1f);
        addSliderRow(table, "Color R", colorRSlider, value -> {
            definition.startColor[0] = value / 255f;
            definition.endColor[0] = Math.max(0f, definition.startColor[0] * 0.45f);
            definition.sanitize();
            updateRgbLabel();
        });

        colorGSlider = createSlider(0f, 255f, 1f);
        addSliderRow(table, "Color G", colorGSlider, value -> {
            definition.startColor[1] = value / 255f;
            definition.endColor[1] = Math.max(0f, definition.startColor[1] * 0.45f);
            definition.sanitize();
            updateRgbLabel();
        });

        colorBSlider = createSlider(0f, 255f, 1f);
        addSliderRow(table, "Color B", colorBSlider, value -> {
            definition.startColor[2] = value / 255f;
            definition.endColor[2] = Math.max(0f, definition.startColor[2] * 0.45f);
            definition.sanitize();
            updateRgbLabel();
        });

        startAlphaSlider = createSlider(0f, 1f, 0.01f);
        addSliderRow(table, "Start Alpha", startAlphaSlider, value -> {
            definition.startColor[3] = value;
            definition.sanitize();
        });

        endAlphaSlider = createSlider(0f, 1f, 0.01f);
        addSliderRow(table, "End Alpha", endAlphaSlider, value -> {
            definition.endColor[3] = value;
            definition.sanitize();
        });

        normalsToggle = new CheckBox("Use normals", skin);
        normalsToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                definition.useNormals = normalsToggle.isChecked();
                definition.sanitize();
            }
        });
        table.add(normalsToggle).left().colspan(2).padTop(2f);
        table.row();

        collisionToggle = new CheckBox("Enable floor collision", skin);
        collisionToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                definition.collisionEnabled = collisionToggle.isChecked();
                definition.sanitize();
            }
        });
        table.add(collisionToggle).left().colspan(2);
        table.row();

        retroToggle = new CheckBox("Retro effect", skin);
        retroToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                definition.retroEffect = retroToggle.isChecked();
                definition.sanitize();
            }
        });
        table.add(retroToggle).left().colspan(2);
        table.row();
    }

    private void buildRightPanel(Table table) {
        randomSlider = createSlider(0f, 1f, 0.01f);
        addSliderRow(table, "Random Strength", randomSlider, value -> {
            definition.randomStrength = value;
            definition.sanitize();
            if (randomToggle != null) {
                randomToggle.setChecked(definition.randomStrength > 0.001f);
            }
        });

        chaosSlider = createSlider(0f, 3f, 0.05f);
        addSliderRow(table, "Chaos Strength", chaosSlider, value -> {
            definition.chaosStrength = value;
            definition.sanitize();
            if (chaosToggle != null) {
                chaosToggle.setChecked(definition.chaosStrength > 0.001f);
            }
        });

        multiDirectionSlider = createSlider(1f, 12f, 1f);
        addSliderRow(table, "Multi Direction Count", multiDirectionSlider, value -> {
            definition.multiDirectionCount = (int) value;
            definition.sanitize();
        });

        directionXSlider = createSlider(-1f, 1f, 0.01f);
        addSliderRow(table, "Direction X", directionXSlider, value -> {
            definition.directionX = value;
            definition.sanitize();
        });

        directionYSlider = createSlider(-1f, 1f, 0.01f);
        addSliderRow(table, "Direction Y", directionYSlider, value -> {
            definition.directionY = value;
            definition.sanitize();
        });

        directionZSlider = createSlider(-1f, 1f, 0.01f);
        addSliderRow(table, "Direction Z", directionZSlider, value -> {
            definition.directionZ = value;
            definition.sanitize();
        });

        Label advancedLabel = new Label("Adv Params", skin);
        table.add(advancedLabel).left().colspan(2).padTop(2f);
        table.row();

        burstCountSlider = createSlider(1f, 500f, 1f);
        addSliderRow(table, "Burst Count", burstCountSlider, value -> {
            if (suppressAdvancedCallbacks) {
                return;
            }

            definition.burstCount = (int) value;
            definition.sanitize();
        });

        coneHalfAngleSlider = createSlider(0f, 89f, 1f);
        addSliderRow(table, "Cone Half", coneHalfAngleSlider, value -> {
            if (suppressAdvancedCallbacks) {
                return;
            }

            definition.coneHalfAngle = value;
            definition.sanitize();
        });

        torusMajorRadiusSlider = createSlider(0f, 3f, 0.01f);
        addSliderRow(table, "Torus Major", torusMajorRadiusSlider, value -> {
            if (suppressAdvancedCallbacks) {
                return;
            }

            definition.torusMajorRadius = value;
            definition.sanitize();
        });

        torusMinorRadiusSlider = createSlider(0f, 1.5f, 0.01f);
        addSliderRow(table, "Torus Minor", torusMinorRadiusSlider, value -> {
            if (suppressAdvancedCallbacks) {
                return;
            }

            definition.torusMinorRadius = value;
            definition.sanitize();
        });

        ringAxisXSlider = createSlider(-1f, 1f, 0.01f);
        addSliderRow(table, "Ring Axis X", ringAxisXSlider, value -> {
            if (suppressAdvancedCallbacks) {
                return;
            }

            definition.ringAxisX = value;
            definition.sanitize();
            syncRingAxisControls();
        });

        ringAxisYSlider = createSlider(-1f, 1f, 0.01f);
        addSliderRow(table, "Ring Axis Y", ringAxisYSlider, value -> {
            if (suppressAdvancedCallbacks) {
                return;
            }

            definition.ringAxisY = value;
            definition.sanitize();
            syncRingAxisControls();
        });

        ringAxisZSlider = createSlider(-1f, 1f, 0.01f);
        addSliderRow(table, "Ring Axis Z", ringAxisZSlider, value -> {
            if (suppressAdvancedCallbacks) {
                return;
            }

            definition.ringAxisZ = value;
            definition.sanitize();
            syncRingAxisControls();
        });

        randomToggle = new CheckBox("Enable random spread", skin);
        randomToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (randomToggle.isChecked()) {
                    definition.randomStrength = Math.max(definition.randomStrength, 0.35f);
                } else {
                    definition.randomStrength = 0f;
                }
                definition.sanitize();
                randomSlider.setValue(definition.randomStrength);
            }
        });
        table.add(randomToggle).left().colspan(2).padTop(2f);
        table.row();

        chaosToggle = new CheckBox("Enable chaos turbulence", skin);
        chaosToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (chaosToggle.isChecked()) {
                    definition.chaosStrength = Math.max(definition.chaosStrength, 0.5f);
                } else {
                    definition.chaosStrength = 0f;
                }
                definition.sanitize();
                chaosSlider.setValue(definition.chaosStrength);
            }
        });
        table.add(chaosToggle).left().colspan(2);
        table.row();

        multiDirectionToggle = new CheckBox("Enable multi-direction mode", skin);
        multiDirectionToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (multiDirectionToggle.isChecked()) {
                    definition.directionMode = ParticleDirectionMode.MULTI.name();
                } else if (ParticleDirectionMode.fromName(definition.directionMode) == ParticleDirectionMode.MULTI) {
                    definition.directionMode = ParticleDirectionMode.UP.name();
                }
                definition.sanitize();
                directionLabel.setText("Direction: " + ParticleDirectionMode.fromName(definition.directionMode).name());
            }
        });
        table.add(multiDirectionToggle).left().colspan(2);
        table.row();

        Label presetLabel = new Label("Preset name", skin);
        table.add(presetLabel).left().colspan(2).padTop(3f);
        table.row();

        presetField = new TextField("default", skin);
        table.add(presetField).width(FIELD_WIDTH).left().colspan(2);
        table.row();

        TextButton saveButton = new TextButton("Save JSON", skin);
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                savePreset(presetField.getText());
            }
        });

        TextButton loadButton = new TextButton("Load JSON", skin);
        loadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadPreset(presetField.getText());
            }
        });

        table.add(saveButton).width(DOUBLE_BUTTON_WIDTH).left();
        table.add(loadButton).width(DOUBLE_BUTTON_WIDTH).left();
        table.row();

        TextButton loadSmokeButton = new TextButton("Load smoke", skin);
        loadSmokeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                presetField.setText("smoke");
                loadPreset("smoke");
            }
        });

        TextButton loadMistButton = new TextButton("Load mist", skin);
        loadMistButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                presetField.setText("mist");
                loadPreset("mist");
            }
        });

        table.add(loadSmokeButton).width(DOUBLE_BUTTON_WIDTH).left();
        table.add(loadMistButton).width(DOUBLE_BUTTON_WIDTH).left();
        table.row();

        activeCountLabel = new Label("Active particles: 0", skin);
        table.add(activeCountLabel).left().colspan(2).padTop(3f);
        table.row();

        statusLabel = new Label("Ready", skin);
        statusLabel.setWrap(true);
        table.add(statusLabel).width(FIELD_WIDTH).left().colspan(2).padTop(2f);
        table.row();

        blendLabel = new Label("Blend: ADDITIVE", skin);
        table.add(blendLabel).left().colspan(2).padTop(3f);
        table.row();

        TextButton additiveButton = new TextButton("Additive", skin);
        additiveButton.addListener(createBlendModeListener(ParticleBlendMode.ADDITIVE));
        TextButton alphaButton = new TextButton("Alpha", skin);
        alphaButton.addListener(createBlendModeListener(ParticleBlendMode.ALPHA));
        TextButton screenButton = new TextButton("Screen", skin);
        screenButton.addListener(createBlendModeListener(ParticleBlendMode.SCREEN));

        table.add(additiveButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(alphaButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(screenButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();

        shapeLabel = new Label("Shape: POINT", skin);
        table.add(shapeLabel).left().colspan(2).padTop(3f);
        table.row();

        TextButton shapePointButton = new TextButton("Point", skin);
        shapePointButton.addListener(createShapeListener(ParticleEmissionShape.POINT));
        TextButton shapeSphereButton = new TextButton("Sphere", skin);
        shapeSphereButton.addListener(createShapeListener(ParticleEmissionShape.SPHERE));
        TextButton shapeBoxButton = new TextButton("Box", skin);
        shapeBoxButton.addListener(createShapeListener(ParticleEmissionShape.BOX));
        TextButton shapeRingButton = new TextButton("Ring", skin);
        shapeRingButton.addListener(createShapeListener(ParticleEmissionShape.RING));
        TextButton shapeDiscButton = new TextButton("Disc", skin);
        shapeDiscButton.addListener(createShapeListener(ParticleEmissionShape.DISC));
        TextButton shapeConeButton = new TextButton("Cone", skin);
        shapeConeButton.addListener(createShapeListener(ParticleEmissionShape.CONE));
        TextButton shapeLineButton = new TextButton("Line", skin);
        shapeLineButton.addListener(createShapeListener(ParticleEmissionShape.LINE));
        TextButton shapeSurfaceButton = new TextButton("Surf", skin);
        shapeSurfaceButton.addListener(createShapeListener(ParticleEmissionShape.SURFACE_SPHERE));
        TextButton shapeTorusButton = new TextButton("Torus", skin);
        shapeTorusButton.addListener(createShapeListener(ParticleEmissionShape.TORUS));

        table.add(shapePointButton).width(FIVE_BUTTON_WIDTH).left();
        table.add(shapeSphereButton).width(FIVE_BUTTON_WIDTH).left();
        table.add(shapeBoxButton).width(FIVE_BUTTON_WIDTH).left();
        table.add(shapeRingButton).width(FIVE_BUTTON_WIDTH).left();
        table.add(shapeDiscButton).width(FIVE_BUTTON_WIDTH).left();
        table.row();
        table.add(shapeConeButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(shapeLineButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(shapeSurfaceButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();
        table.add(shapeTorusButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();

        directionLabel = new Label("Direction: UP", skin);
        table.add(directionLabel).left().colspan(2).padTop(3f);
        table.row();

        TextButton directionUpButton = new TextButton("Up", skin);
        directionUpButton.addListener(createDirectionListener(ParticleDirectionMode.UP));
        TextButton directionOutwardButton = new TextButton("Out", skin);
        directionOutwardButton.addListener(createDirectionListener(ParticleDirectionMode.OUTWARD));
        TextButton directionVectorButton = new TextButton("Vector", skin);
        directionVectorButton.addListener(createDirectionListener(ParticleDirectionMode.VECTOR));
        TextButton directionRandomButton = new TextButton("Random", skin);
        directionRandomButton.addListener(createDirectionListener(ParticleDirectionMode.RANDOM));
        TextButton directionChaosButton = new TextButton("Chaos", skin);
        directionChaosButton.addListener(createDirectionListener(ParticleDirectionMode.CHAOS));
        TextButton directionMultiButton = new TextButton("Multi", skin);
        directionMultiButton.addListener(createDirectionListener(ParticleDirectionMode.MULTI));
        TextButton directionInwardButton = new TextButton("Inward", skin);
        directionInwardButton.addListener(createDirectionListener(ParticleDirectionMode.INWARD));
        TextButton directionTangentButton = new TextButton("Tangent", skin);
        directionTangentButton.addListener(createDirectionListener(ParticleDirectionMode.TANGENT));
        TextButton directionSurfaceButton = new TextButton("SurfN", skin);
        directionSurfaceButton.addListener(createDirectionListener(ParticleDirectionMode.SURFACE_NORMAL));

        table.add(directionUpButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(directionOutwardButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(directionVectorButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();
        table.add(directionRandomButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(directionChaosButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(directionMultiButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();
        table.add(directionInwardButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(directionTangentButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(directionSurfaceButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();

        Label formulaLabel = new Label("Formula Tests", skin);
        table.add(formulaLabel).left().colspan(2).padTop(3f);
        table.row();

        TextButton formulaRingButton = new TextButton("Ring", skin);
        formulaRingButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyFormula(ParticleFormulas.ring(new Vector3(0f, 1f, 0f), 0.5f, 6f, new Color(0.3f, 0.9f, 1f, 1f)));
            }
        });

        TextButton formulaExplosionButton = new TextButton("Explode", skin);
        formulaExplosionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyFormula(
                    ParticleFormulas.explosion(
                        0.3f,
                        8f,
                        180,
                        new Color(1f, 0.9f, 0.3f, 1f),
                        new Color(0.4f, 0.05f, 0f, 1f)
                    )
                );
            }
        });

        TextButton formulaConeButton = new TextButton("Cone", skin);
        formulaConeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyFormula(
                    ParticleFormulas.cone(new Vector3(0.2f, 1f, 0.1f).nor(), 28f, 4.5f, 1.6f, new Color(1f, 0.7f, 0.2f, 1f))
                );
            }
        });

        TextButton formulaSonarButton = new TextButton("Sonar", skin);
        formulaSonarButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyFormula(ParticleFormulas.sonarPulse(new Color(0.2f, 0.85f, 1f, 1f)));
            }
        });

        TextButton formulaFountainButton = new TextButton("Fount", skin);
        formulaFountainButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyFormula(ParticleFormulas.fountain(new Color(0.6f, 0.8f, 1f, 1f), 5f, 24f));
            }
        });

        TextButton formulaVortexButton = new TextButton("Vortex", skin);
        formulaVortexButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyFormula(ParticleFormulas.vortex(new Color(0.7f, 0.3f, 1f, 1f), 0.55f, 0.45f));
            }
        });

        table.add(formulaRingButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(formulaExplosionButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(formulaConeButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();
        table.add(formulaSonarButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(formulaFountainButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(formulaVortexButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();

        TextButton soundTestButton = new TextButton("Sound Test", skin);
        soundTestButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Gdx.app.getApplicationListener() instanceof Game) {
                    ((Game) Gdx.app.getApplicationListener()).setScreen(new SoundTestScreen());
                }
            }
        });
        table.add(soundTestButton).width(TRIPLE_BUTTON_WIDTH * 3 + 8f).left().colspan(3).padTop(8f);
        table.row();

        Label taskPresetLabel = new Label("Task Presets", skin);
        table.add(taskPresetLabel).left().colspan(2).padTop(3f);
        table.row();

        TextButton presetExplosionButton = new TextButton("Expl", skin);
        presetExplosionButton.addListener(createQuickPresetListener("explosion"));
        TextButton presetRingButton = new TextButton("Ring", skin);
        presetRingButton.addListener(createQuickPresetListener("ring_shot"));
        TextButton presetSonarButton = new TextButton("Sonar", skin);
        presetSonarButton.addListener(createQuickPresetListener("sonar_pulse"));
        TextButton presetSmokePuffButton = new TextButton("SmPuff", skin);
        presetSmokePuffButton.addListener(createQuickPresetListener("smoke_puff"));
        TextButton presetSpiralButton = new TextButton("Spiral", skin);
        presetSpiralButton.addListener(createQuickPresetListener("spiral"));
        TextButton presetFireButton = new TextButton("Fire", skin);
        presetFireButton.addListener(createQuickPresetListener("fire"));

        table.add(presetExplosionButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(presetRingButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(presetSonarButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();
        table.add(presetSmokePuffButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(presetSpiralButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.add(presetFireButton).width(TRIPLE_BUTTON_WIDTH).left();
        table.row();
    }

    private Slider createSlider(float min, float max, float step) {
        return new Slider(min, max, step, false, skin);
    }

    private void addSliderRow(Table table, String title, Slider slider, SliderCallback callback) {
        Label valueLabel = new Label("", skin);
        table.add(valueLabel).left().colspan(2);
        table.row();

        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                callback.onValueChanged(slider.getValue());
                valueLabel.setText(title + ": " + formatSliderValue(slider.getValue()));
            }
        });

        table.add(slider).width(SLIDER_WIDTH).left().colspan(2);
        table.row();

        callback.onValueChanged(slider.getValue());
        valueLabel.setText(title + ": " + formatSliderValue(slider.getValue()));
    }

    private ChangeListener createBlendModeListener(ParticleBlendMode blendMode) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                definition.blendMode = blendMode.name();
                blendLabel.setText("Blend: " + blendMode.name());
            }
        };
    }

    private ChangeListener createShapeListener(ParticleEmissionShape shape) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                definition.emissionShape = shape.name();
                definition.sanitize();
                shapeLabel.setText("Shape: " + shape.name());
            }
        };
    }

    private ChangeListener createDirectionListener(ParticleDirectionMode mode) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                definition.directionMode = mode.name();
                definition.sanitize();
                directionLabel.setText("Direction: " + mode.name());
                if (multiDirectionToggle != null) {
                    multiDirectionToggle.setChecked(mode == ParticleDirectionMode.MULTI);
                }
            }
        };
    }

    private ChangeListener createQuickPresetListener(String presetName) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                presetField.setText(presetName);
                loadPreset(presetName);
            }
        };
    }

    private void applyFormula(ParticleDefinition formulaDefinition) {
        definition = formulaDefinition;
        emitter.clear();
        emitter.setDefinition(definition);
        syncUiFromDefinition();
        ParticlePresetStore.ValidationResult validation = ParticlePresetStore.validate(definition);
        statusLabel.setText(buildValidationStatus("Applied formula: " + definition.name, validation));
    }

    private void savePreset(String presetName) {
        definition.sanitize();
        ParticlePresetStore.ValidationResult validation = ParticlePresetStore.validate(definition);
        if (!validation.valid) {
            statusLabel.setText(buildValidationStatus("Save blocked", validation));
            return;
        }

        ParticlePresetStore.saveLocalPreset(presetName, definition);
        statusLabel.setText(
            buildValidationStatus("Saved preset to " + ParticlePresetStore.getLocalPresetFile(presetName).path(), validation)
        );
    }

    private void loadPreset(String presetName) {
        definition = ParticlePresetStore.loadLocalPreset(presetName, ParticleDefinition.createDefault());
        emitter.clear();
        emitter.setDefinition(definition);
        syncUiFromDefinition();
        ParticlePresetStore.ValidationResult validation = ParticlePresetStore.validate(definition);
        statusLabel.setText(
            buildValidationStatus("Loaded preset from " + ParticlePresetStore.getLocalPresetFile(presetName).path(), validation)
        );
    }

    private void syncUiFromDefinition() {
        definition.sanitize();

        emissionSlider.setValue(definition.emissionRate);
        lifetimeSlider.setValue(definition.lifetimeMax);
        speedSlider.setValue(definition.speedMax);
        sizeSlider.setValue(definition.startSizeMax);
        radiusSlider.setValue(definition.spawnRadius);
        emissiveSlider.setValue(definition.emissiveStrength);
        randomSlider.setValue(definition.randomStrength);
        chaosSlider.setValue(definition.chaosStrength);
        multiDirectionSlider.setValue(definition.multiDirectionCount);
        directionXSlider.setValue(definition.directionX);
        directionYSlider.setValue(definition.directionY);
        directionZSlider.setValue(definition.directionZ);
        suppressAdvancedCallbacks = true;
        burstCountSlider.setValue(definition.burstCount);
        coneHalfAngleSlider.setValue(definition.coneHalfAngle);
        torusMajorRadiusSlider.setValue(definition.torusMajorRadius);
        torusMinorRadiusSlider.setValue(definition.torusMinorRadius);
        ringAxisXSlider.setValue(definition.ringAxisX);
        ringAxisYSlider.setValue(definition.ringAxisY);
        ringAxisZSlider.setValue(definition.ringAxisZ);
        suppressAdvancedCallbacks = false;
        colorRSlider.setValue(definition.startColor[0] * 255f);
        colorGSlider.setValue(definition.startColor[1] * 255f);
        colorBSlider.setValue(definition.startColor[2] * 255f);
        startAlphaSlider.setValue(definition.startColor[3]);
        endAlphaSlider.setValue(definition.endColor[3]);

        normalsToggle.setChecked(definition.useNormals);
        collisionToggle.setChecked(definition.collisionEnabled);
        retroToggle.setChecked(definition.retroEffect);
        randomToggle.setChecked(definition.randomStrength > 0.001f);
        chaosToggle.setChecked(definition.chaosStrength > 0.001f);
        multiDirectionToggle.setChecked(
            ParticleDirectionMode.fromName(definition.directionMode) == ParticleDirectionMode.MULTI
        );
        blendLabel.setText("Blend: " + ParticleBlendMode.fromName(definition.blendMode).name());
        shapeLabel.setText("Shape: " + ParticleEmissionShape.fromName(definition.emissionShape).name());
        directionLabel.setText("Direction: " + ParticleDirectionMode.fromName(definition.directionMode).name());
        updateRgbLabel();
    }

    private void syncRingAxisControls() {
        if (ringAxisXSlider == null || ringAxisYSlider == null || ringAxisZSlider == null) {
            return;
        }

        suppressAdvancedCallbacks = true;
        try {
            ringAxisXSlider.setValue(definition.ringAxisX);
            ringAxisYSlider.setValue(definition.ringAxisY);
            ringAxisZSlider.setValue(definition.ringAxisZ);
        } finally {
            suppressAdvancedCallbacks = false;
        }
    }

    private Skin createSkin() {
        Skin newSkin = new Skin();

        BitmapFont font = new BitmapFont();
        newSkin.add("default-font", font);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        newSkin.add("white", whiteTexture);

        TextureRegionDrawable panel = new TextureRegionDrawable(new TextureRegion(whiteTexture));

        LabelStyle labelStyle = new LabelStyle(font, Color.WHITE);
        newSkin.add("default", labelStyle);

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.up = tintedDrawable(panel, new Color(0.16f, 0.16f, 0.2f, 1f), 1f, 24f);
        buttonStyle.down = tintedDrawable(panel, new Color(0.26f, 0.26f, 0.33f, 1f), 1f, 24f);
        buttonStyle.over = tintedDrawable(panel, new Color(0.22f, 0.22f, 0.28f, 1f), 1f, 24f);
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        newSkin.add("default", buttonStyle);

        SliderStyle sliderStyle = new SliderStyle();
        sliderStyle.background = tintedDrawable(panel, new Color(0.12f, 0.12f, 0.17f, 1f), SLIDER_WIDTH, 6f);
        sliderStyle.knob = tintedDrawable(panel, new Color(0.95f, 0.95f, 1f, 1f), 12f, 16f);
        sliderStyle.knobBefore = tintedDrawable(panel, new Color(0.72f, 0.55f, 1f, 1f), SLIDER_WIDTH, 6f);
        newSkin.add("default-horizontal", sliderStyle);

        CheckBoxStyle checkBoxStyle = new CheckBoxStyle();
        checkBoxStyle.checkboxOff = tintedDrawable(panel, new Color(0.12f, 0.12f, 0.17f, 1f), 18f, 18f);
        checkBoxStyle.checkboxOn = tintedDrawable(panel, new Color(0.28f, 0.78f, 0.48f, 1f), 18f, 18f);
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;
        newSkin.add("default", checkBoxStyle);

        TextFieldStyle textFieldStyle = new TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = tintedDrawable(panel, new Color(0.1f, 0.1f, 0.14f, 1f), 1f, 24f);
        textFieldStyle.cursor = tintedDrawable(panel, Color.WHITE, 2f, 16f);
        textFieldStyle.selection = tintedDrawable(panel, new Color(0.3f, 0.3f, 0.45f, 1f), 1f, 16f);
        newSkin.add("default", textFieldStyle);

        return newSkin;
    }

    private Drawable tintedDrawable(TextureRegionDrawable base, Color tint, float minWidth, float minHeight) {
        Drawable drawable = base.tint(tint);
        if (drawable instanceof BaseDrawable baseDrawable) {
            baseDrawable.setMinSize(minWidth, minHeight);
        }
        return drawable;
    }

    private String formatSliderValue(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return Integer.toString((int) value);
        }
        return String.format("%.2f", value);
    }

    private void updateRgbLabel() {
        int red = Math.round(definition.startColor[0] * 255f);
        int green = Math.round(definition.startColor[1] * 255f);
        int blue = Math.round(definition.startColor[2] * 255f);
        rgbLabel.setText("RGB: " + red + ", " + green + ", " + blue);
    }

    private String buildValidationStatus(String prefix, ParticlePresetStore.ValidationResult validation) {
        if (validation == null) {
            return prefix;
        }

        if (!validation.valid) {
            return prefix + " | Errors: " + summarizeMessages(validation.errors);
        }

        if (!validation.warnings.isEmpty()) {
            return prefix + " | Warnings: " + summarizeMessages(validation.warnings);
        }

        return prefix;
    }

    private String summarizeMessages(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "none";
        }

        if (messages.size() == 1) {
            return messages.get(0);
        }

        return messages.get(0) + " (and " + (messages.size() - 1) + " more)";
    }

    private interface SliderCallback {
        void onValueChanged(float value);
    }
}

package io.github.superteam.resonance.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.enemy.EnemyController;
import io.github.superteam.resonance.event.EventBus;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.event.EventState;
import io.github.superteam.resonance.interactable.InteractableRegistry;
import io.github.superteam.resonance.interaction.RaycastInteractionSystem;
import io.github.superteam.resonance.settings.KeybindRegistry;
import io.github.superteam.resonance.settings.SettingsData;
import io.github.superteam.resonance.settings.SettingsSystem;

/**
 * Main gameplay screen scaffold.
 *
 * This class intentionally contains only lifecycle and render structure,
 * so gameplay systems can be integrated incrementally.
 */
public class GameScreen extends ScreenAdapter {

	// Final-game content anchors. These are intentionally constants so wiring can
	// happen in one place as systems are integrated.
	private static final String FINAL_MAP_MODEL_PATH = "models/resonance-map.gltf";
	private static final String FINAL_MAP_DOCUMENT_PATH = "maps/resonance-map.json";
	private static final String FINAL_EVENTS_PATH = "config/events.json";
	private static final String FINAL_TRIGGERS_PATH = "config/triggers.json";

	private boolean initialized;
	private SettingsSystem settingsSystem;
	private SettingsData settingsData;
	private KeybindRegistry keybindRegistry;
	private GameAudioSystem gameAudioSystem;
	private EventBus eventBus;
	private EventState eventState;
	private InteractableRegistry interactableRegistry;
	private RaycastInteractionSystem raycastInteractionSystem;
	private EnemyController enemyController;
	private final Vector3 bootstrapPlayerPosition = new Vector3();
	private final Vector3 bootstrapPlayerForward = new Vector3(0f, 0f, -1f);

	@Override
	public void show() {
		if (initialized) {
			return;
		}

		initializeMapAndCollision();
		initializeCoreSystems();
		initializeGameplaySystems();
		initializeUiAndOverlays();
		initialized = true;
	}

	@Override
	public void render(float delta) {
		float clampedDelta = Math.max(0f, delta);

		update(clampedDelta);

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		renderWorld(clampedDelta);
		renderUi(clampedDelta);
	}

	@Override
	public void resize(int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}

		// Reserve for viewport/camera/UI layout updates.
	}

	@Override
	public void pause() {
		// Reserve for pause-state handling.
	}

	@Override
	public void resume() {
		// Reserve for restoring runtime state after pause.
	}

	@Override
	public void hide() {
		// Called when another screen is shown.
	}

	@Override
	public void dispose() {
		if (gameAudioSystem != null) {
			gameAudioSystem.dispose();
		}
		if (interactableRegistry != null) {
			interactableRegistry.clear();
		}
		if (settingsSystem != null) {
			settingsSystem.save();
		}
		gameAudioSystem = null;
		eventBus = null;
		eventState = null;
		interactableRegistry = null;
		raycastInteractionSystem = null;
		enemyController = null;
		settingsSystem = null;
		settingsData = null;
		keybindRegistry = null;
	}

	private void update(float delta) {
		if (gameAudioSystem != null) {
			gameAudioSystem.update(delta);
		}
		if (eventBus != null) {
			eventBus.update(delta);
		}

		if (raycastInteractionSystem != null) {
			EventContext interactionContext = new EventContext(
				bootstrapPlayerPosition,
				bootstrapPlayerPosition,
				0f,
				null,
				gameAudioSystem,
				eventBus,
				eventState,
				null
			);
			raycastInteractionSystem.update(bootstrapPlayerPosition, bootstrapPlayerForward, interactionContext);
		}

		if (enemyController != null) {
			enemyController.update(delta, bootstrapPlayerPosition);
		}

		// Reserve for top-level per-frame sequencing.
		// Suggested order:
		// 1) Input
		// 2) Player and world simulation
		// 3) Events/triggers/story progression
		// 4) Enemy and atmosphere systems
		// 5) UI state
	}

	private void renderWorld(float delta) {
		// Reserve for map, entities, particles, and post-process world passes.
	}

	private void renderUi(float delta) {
		// Reserve for HUD/menu overlays.
	}

	private void initializeMapAndCollision() {
		// Final map bootstrap goes here.
		// Example future wiring:
		// - Load FINAL_MAP_MODEL_PATH
		// - Load FINAL_MAP_DOCUMENT_PATH
		// - Build/attach collision bodies
	}

	private void initializeCoreSystems() {
		settingsSystem = new SettingsSystem();
		settingsData = settingsSystem.loadOrCreate();
		keybindRegistry = new KeybindRegistry(settingsData);
		gameAudioSystem = new GameAudioSystem();
		eventBus = new EventBus();
		eventState = new EventState();

		// Future wiring:
		// - Event/Trigger runtime with FINAL_EVENTS_PATH and FINAL_TRIGGERS_PATH
		// - Audio runtime
		// - Save/session bootstrap
	}

	private void initializeGameplaySystems() {
		interactableRegistry = new InteractableRegistry();
		raycastInteractionSystem = new RaycastInteractionSystem(interactableRegistry);
		raycastInteractionSystem.setInteractKeycode(keybindRegistry.keyFor(KeybindRegistry.INTERACT));
		enemyController = new EnemyController();

		// Group A bootstrap complete:
		// - Interaction targeting path
		// - Door/interactable-ready registry
		// - Enemy perception/state scaffold
	}

	private void initializeUiAndOverlays() {
		// Reserve for subtitle, debug overlay, interaction prompts, and pause UI.
	}
}

package io.github.superteam.resonance.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.enemy.EnemyController;
import io.github.superteam.resonance.event.EventBus;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.event.EventState;
import io.github.superteam.resonance.interactable.InteractableRegistry;
import io.github.superteam.resonance.interactable.door.DoorController;
import io.github.superteam.resonance.interactable.door.DoorGrabInteraction;
import io.github.superteam.resonance.interactable.door.DoorLockState;
import io.github.superteam.resonance.interaction.InteractionPromptRenderer;
import io.github.superteam.resonance.interaction.InteractionResult;
import io.github.superteam.resonance.interaction.RaycastInteractionSystem;
import io.github.superteam.resonance.map.MapDocument;
import io.github.superteam.resonance.map.MapDocumentLoader;
import io.github.superteam.resonance.map.MapObject;
import io.github.superteam.resonance.map.MapObjectType;
import io.github.superteam.resonance.settings.KeybindRegistry;
import io.github.superteam.resonance.settings.SettingsData;
import io.github.superteam.resonance.settings.SettingsSystem;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;

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
	private float elapsedSeconds;
	private SettingsSystem settingsSystem;
	private SettingsData settingsData;
	private KeybindRegistry keybindRegistry;
	private GameAudioSystem gameAudioSystem;
	private EventBus eventBus;
	private EventState eventState;
	private InteractableRegistry interactableRegistry;
	private RaycastInteractionSystem raycastInteractionSystem;
	private DoorGrabInteraction doorGrabInteraction;
	private final InteractionPromptRenderer interactionPromptRenderer = new InteractionPromptRenderer();
	private String interactionPrompt = "";
	private EnemyController enemyController;
	private final Array<DoorController> doorControllers = new Array<>();
	private MapDocument mapDocument;
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
		elapsedSeconds += Math.max(0f, delta);

		if (gameAudioSystem != null) {
			gameAudioSystem.update(delta);
		}
		if (eventBus != null) {
			eventBus.update(delta);
		}

		EventContext interactionContext = new EventContext(
			bootstrapPlayerPosition,
			bootstrapPlayerPosition,
			elapsedSeconds,
			null,
			gameAudioSystem,
			eventBus,
			eventState,
			null
		);

		if (raycastInteractionSystem != null) {
			InteractionResult interactionResult = raycastInteractionSystem.update(
				bootstrapPlayerPosition,
				bootstrapPlayerForward,
				interactionContext
			);
			interactionPrompt = interactionPromptRenderer.formatPrompt(interactionResult);

			if (doorGrabInteraction != null && raycastInteractionSystem.focused() instanceof DoorController focusedDoor) {
				doorGrabInteraction.update(
					delta,
					Gdx.input.getDeltaX(),
					Gdx.input.isButtonPressed(Input.Buttons.LEFT),
					focusedDoor,
					interactionContext
				);
			}
		}

		for (DoorController doorController : doorControllers) {
			doorController.update(delta, interactionContext);
			if (enemyController == null) {
				continue;
			}

			float noiseIntensity = doorController.consumeNoiseIntensity();
			if (noiseIntensity < 0.15f) {
				continue;
			}

			enemyController.onSoundHeard(new SoundEventData(
				SoundEvent.DOOR_SLAM,
				"door-runtime",
				doorController.worldPosition(),
				noiseIntensity,
				elapsedSeconds
			));
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
		if (interactionPrompt == null || interactionPrompt.isBlank()) {
			return;
		}

		// Prompt string is prepared here and can be rendered by HUD integration.
		// Reserve for HUD/menu overlays.
	}

	private void initializeMapAndCollision() {
		mapDocument = new MapDocumentLoader().loadOrDefault(FINAL_MAP_DOCUMENT_PATH);
		if (mapDocument == null) {
			mapDocument = MapDocument.defaults();
		}

		if (mapDocument.objects != null) {
			for (MapObject mapObject : mapDocument.objects) {
				if (mapObject == null || mapObject.type == null) {
					continue;
				}

				if (mapObject.type == MapObjectType.SPAWN_POINT && mapObject.position != null) {
					bootstrapPlayerPosition.set(mapObject.position);
				}

				if (mapObject.type == MapObjectType.DOOR && mapObject.position != null) {
					doorControllers.add(buildDoorFromMapObject(mapObject));
				}
			}
		}

		if (doorControllers.size == 0) {
			doorControllers.add(new DoorController("bootstrap-door", new Vector3(1.5f, 0f, -1.2f), 1.3f));
		}

		// Final map path constants are anchored for follow-up map/collision integration.
		if (FINAL_MAP_MODEL_PATH.isBlank() || FINAL_EVENTS_PATH.isBlank() || FINAL_TRIGGERS_PATH.isBlank()) {
			throw new IllegalStateException("Final map runtime paths must not be blank.");
		}
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
		for (DoorController doorController : doorControllers) {
			interactableRegistry.register(doorController);
		}

		raycastInteractionSystem = new RaycastInteractionSystem(interactableRegistry);
		raycastInteractionSystem.setInteractKeycode(keybindRegistry.keyFor(KeybindRegistry.INTERACT));
		doorGrabInteraction = new DoorGrabInteraction();
		enemyController = new EnemyController();
		enemyController.setPose(new Vector3(6f, 0f, 6f), new Vector3(-1f, 0f, -1f));

		// Group A bootstrap complete:
		// - Interaction targeting path
		// - Door/interactable-ready registry
		// - Enemy perception/state scaffold
	}

	private void initializeUiAndOverlays() {
		// Reserve for subtitle, debug overlay, interaction prompts, and pause UI.
	}

	private DoorController buildDoorFromMapObject(MapObject mapObject) {
		String resolvedId = mapObject.id == null || mapObject.id.isBlank() ? "door" : mapObject.id;
		float radius = parseFloatProperty(mapObject, "interactionRadius", 1.2f);
		DoorController doorController = new DoorController(resolvedId, mapObject.position, radius);

		String lockState = mapObject.property("lockState", DoorLockState.UNLOCKED.name());
		try {
			doorController.setLockState(DoorLockState.valueOf(lockState.toUpperCase()));
		} catch (Exception ignored) {
			doorController.setLockState(DoorLockState.UNLOCKED);
		}

		return doorController;
	}

	private static float parseFloatProperty(MapObject mapObject, String propertyName, float fallback) {
		if (mapObject == null || propertyName == null || propertyName.isBlank()) {
			return fallback;
		}

		try {
			return Float.parseFloat(mapObject.property(propertyName, Float.toString(fallback)));
		} catch (Exception ignored) {
			return fallback;
		}
	}
}

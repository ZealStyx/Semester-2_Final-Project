package io.github.superteam.resonance.event;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.superteam.resonance.event.actions.FireEventAction;
import io.github.superteam.resonance.event.actions.PlaySoundAction;
import io.github.superteam.resonance.event.actions.SetFlagAction;
import io.github.superteam.resonance.event.actions.ShowSubtitleAction;
import io.github.superteam.resonance.event.actions.WaitAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads event definitions from JSON.
 */
public final class EventLoader {
    private EventLoader() {
    }

    public static EventBus loadOrDefault(String internalConfigPath) {
        EventBus eventBus = new EventBus();
        if (Gdx.files == null || internalConfigPath == null || internalConfigPath.isBlank()) {
            return eventBus;
        }

        FileHandle configFile = Gdx.files.internal(internalConfigPath);
        if (!configFile.exists()) {
            return eventBus;
        }

        try {
            JsonValue root = new JsonReader().parse(configFile);
            JsonValue events = root.get("events");
            if (events == null) {
                return eventBus;
            }

            for (JsonValue eventValue = events.child; eventValue != null; eventValue = eventValue.next) {
                GameEvent event = parseEvent(eventValue);
                if (event != null) {
                    eventBus.register(event);
                }
            }
        } catch (Exception ignored) {
            return eventBus;
        }

        return eventBus;
    }

    private static GameEvent parseEvent(JsonValue eventValue) {
        if (eventValue == null) {
            return null;
        }

        String id = eventValue.getString("id", null);
        if (id == null || id.isBlank()) {
            return null;
        }

        String displayName = eventValue.getString("displayName", id);
        GameEvent.RepeatMode repeatMode = parseRepeatMode(eventValue.getString("repeatMode", GameEvent.RepeatMode.REPEATING.name()));
        float cooldownSeconds = eventValue.getFloat("cooldownSeconds", 0f);
        List<EventAction> actions = parseActions(eventValue.get("actions"));
        return new GameEvent(id, displayName, actions, repeatMode, cooldownSeconds);
    }

    private static List<EventAction> parseActions(JsonValue actionsValue) {
        List<EventAction> actions = new ArrayList<>();
        if (actionsValue == null) {
            return actions;
        }

        for (JsonValue actionValue = actionsValue.child; actionValue != null; actionValue = actionValue.next) {
            EventAction action = parseAction(actionValue);
            if (action != null) {
                actions.add(action);
            }
        }
        return actions;
    }

    private static EventAction parseAction(JsonValue actionValue) {
        if (actionValue == null) {
            return null;
        }

        String type = actionValue.getString("type", "").trim().toUpperCase();
        return switch (type) {
            case "SET_FLAG" -> new SetFlagAction(
                actionValue.getString("flagName", actionValue.getString("flag", null)),
                actionValue.getBoolean("value", true)
            );
            case "PLAY_SOUND" -> new PlaySoundAction(
                actionValue.getString("soundPath", actionValue.getString("path", null)),
                actionValue.getFloat("volume", 1.0f)
            );
            case "FIRE_EVENT" -> new FireEventAction(
                actionValue.getString("eventId", actionValue.getString("targetEvent", null))
            );
            case "WAIT" -> new WaitAction(
                actionValue.getFloat("delaySeconds", 0f)
            );
            case "SHOW_SUBTITLE" -> new ShowSubtitleAction(
                actionValue.getString("text", null),
                actionValue.getFloat("duration", actionValue.getFloat("durationSeconds", 2f))
            );
            default -> null;
        };
    }

    private static GameEvent.RepeatMode parseRepeatMode(String repeatMode) {
        if (repeatMode == null || repeatMode.isBlank()) {
            return GameEvent.RepeatMode.REPEATING;
        }

        try {
            return GameEvent.RepeatMode.valueOf(repeatMode.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return GameEvent.RepeatMode.REPEATING;
        }
    }
}
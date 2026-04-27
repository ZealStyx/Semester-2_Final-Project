package io.github.superteam.resonance.trigger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.superteam.resonance.trigger.conditions.ZoneTrigger;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads trigger definitions from JSON.
 */
public final class TriggerLoader {
    private TriggerLoader() {
    }

    public static TriggerEvaluator loadOrDefault(String internalConfigPath) {
        if (Gdx.files == null || internalConfigPath == null || internalConfigPath.isBlank()) {
            return new TriggerEvaluator(List.of());
        }

        FileHandle configFile = Gdx.files.internal(internalConfigPath);
        if (!configFile.exists()) {
            return new TriggerEvaluator(List.of());
        }

        try {
            JsonValue root = new JsonReader().parse(configFile);
            JsonValue triggersNode = root.isArray() ? root : root.get("triggers");
            List<Trigger> triggers = parseTriggers(triggersNode);
            return new TriggerEvaluator(triggers);
        } catch (Exception ignored) {
            return new TriggerEvaluator(List.of());
        }
    }

    private static List<Trigger> parseTriggers(JsonValue triggersNode) {
        List<Trigger> triggers = new ArrayList<>();
        if (triggersNode == null) {
            return triggers;
        }

        for (JsonValue triggerValue = triggersNode.child; triggerValue != null; triggerValue = triggerValue.next) {
            Trigger trigger = parseTrigger(triggerValue);
            if (trigger != null) {
                triggers.add(trigger);
            }
        }
        return triggers;
    }

    private static Trigger parseTrigger(JsonValue triggerValue) {
        if (triggerValue == null) {
            return null;
        }

        String id = triggerValue.getString("id", null);
        String type = triggerValue.getString("type", "").trim().toUpperCase();
        String targetEvent = triggerValue.getString("targetEvent", null);
        float cooldownSeconds = triggerValue.getFloat("cooldownSeconds", 0f);

        if (id == null || id.isBlank() || targetEvent == null || targetEvent.isBlank()) {
            return null;
        }

        if (!"ZONE".equals(type)) {
            return null;
        }

        BoundingBox volume = parseBoundingBox(triggerValue.get("volume"));
        if (volume == null) {
            return null;
        }

        return new ZoneTrigger(id, targetEvent, cooldownSeconds, volume);
    }

    private static BoundingBox parseBoundingBox(JsonValue volumeValue) {
        if (volumeValue == null) {
            return null;
        }

        float minX = volumeValue.getFloat("minX", 0f);
        float minY = volumeValue.getFloat("minY", 0f);
        float minZ = volumeValue.getFloat("minZ", 0f);
        float maxX = volumeValue.getFloat("maxX", 0f);
        float maxY = volumeValue.getFloat("maxY", 0f);
        float maxZ = volumeValue.getFloat("maxZ", 0f);

        Vector3 min = new Vector3(Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ));
        Vector3 max = new Vector3(Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));
        return new BoundingBox(min, max);
    }
}

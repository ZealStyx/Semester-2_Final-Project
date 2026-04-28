package io.github.superteam.resonance.save;

import com.badlogic.gdx.math.Vector3;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal SaveData DTO used by SaveSystem. */
public final class SaveData {
    public String saveSlotId;
    public String mapName = "resonance-full-map";
    public String checkpointId;
    public Vector3 playerPosition = new Vector3();
    public float playerSanity = 100f;
    public float flashlightBattery = 100f;
    public Map<String, Integer> eventFireCounts = new LinkedHashMap<>();
    public Map<String, String> inventoryItems = new LinkedHashMap<>();
    public long savedAtMillis;
    public String savedAtDisplay;
}

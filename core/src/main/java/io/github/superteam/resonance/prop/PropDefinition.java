package io.github.superteam.resonance.prop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PropDefinition {
    public String id = "prop";
    public String displayName = "Prop";
    public String modelPath = "";
    public PropCategory category = PropCategory.DECOR;
    public PropBehavior behavior = PropBehavior.STATIC;
    public PropPhysicsHints physicsHints = new PropPhysicsHints();
    public List<PropAnchor> anchors = new ArrayList<>();
    public List<PropSoundSlot> soundSlots = new ArrayList<>();
    public Map<String, String> tags = new LinkedHashMap<>();

    public PropDefinition() {
    }

    public PropDefinition(String id, String displayName, String modelPath) {
        this.id = id;
        this.displayName = displayName;
        this.modelPath = modelPath;
    }

    public PropDefinition copy() {
        PropDefinition copy = new PropDefinition(id, displayName, modelPath);
        copy.category = category;
        copy.behavior = behavior;
        copy.physicsHints = new PropPhysicsHints(physicsHints.mass, physicsHints.kinematic, physicsHints.sensor, physicsHints.collidable);
        copy.anchors = new ArrayList<>(anchors);
        copy.soundSlots = new ArrayList<>(soundSlots);
        copy.tags = new LinkedHashMap<>(tags);
        return copy;
    }
}

package io.github.superteam.resonance.prop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PropDefinitionRegistry {
    private final Map<String, PropDefinition> definitions = new LinkedHashMap<>();

    public void register(PropDefinition definition) {
        if (definition == null || definition.id == null || definition.id.isBlank()) {
            return;
        }
        definitions.put(definition.id, definition);
    }

    public PropDefinition find(String id) {
        return id == null ? null : definitions.get(id);
    }

    public List<PropDefinition> all() {
        return new ArrayList<>(definitions.values());
    }

    public int size() {
        return definitions.size();
    }
}

package io.github.superteam.resonance.items;

import java.util.Objects;

/**
 * Immutable configuration describing item behavior and handling rules.
 */
public final class ItemDefinition {

    private final ItemType itemType;
    private final String displayName;
    private final float noiseMultiplier;
    private final float breakThreshold;
    private final float throwStrength;
    private final float mass;
    private final boolean carriable;
    private final boolean consumable;
    private final boolean stackable;

    private ItemDefinition(
        ItemType itemType,
        String displayName,
        float noiseMultiplier,
        float breakThreshold,
        float throwStrength,
        float mass,
        boolean carriable,
        boolean consumable,
        boolean stackable
    ) {
        this.itemType = Objects.requireNonNull(itemType, "itemType must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (noiseMultiplier < 0f) {
            throw new IllegalArgumentException("noiseMultiplier must be non-negative");
        }
        if (breakThreshold <= 0f) {
            throw new IllegalArgumentException("breakThreshold must be positive");
        }
        if (throwStrength < 0f) {
            throw new IllegalArgumentException("throwStrength must be non-negative");
        }
        if (mass <= 0f) {
            throw new IllegalArgumentException("mass must be positive");
        }

        this.noiseMultiplier = noiseMultiplier;
        this.breakThreshold = breakThreshold;
        this.throwStrength = throwStrength;
        this.mass = mass;
        this.carriable = carriable;
        this.consumable = consumable;
        this.stackable = stackable;
    }

    public static ItemDefinition create(ItemType itemType) {
        return switch (Objects.requireNonNull(itemType, "itemType must not be null")) {
            case METAL_PIPE -> new ItemDefinition(itemType, "Metal Pipe", 1.4f, 240f, 20f, 6.5f, true, false, false);
            case CARDBOARD_BOX -> new ItemDefinition(itemType, "Cardboard Box", 0.3f, 70f, 12f, 1.8f, true, false, false);
            case GLASS_BOTTLE -> new ItemDefinition(itemType, "Glass Bottle", 1.2f, 60f, 14f, 1.1f, true, false, false);
            case CONCRETE_CHUNK -> new ItemDefinition(itemType, "Concrete Chunk", 1.0f, 300f, 16f, 7.2f, true, false, false);
            case FLARE -> new ItemDefinition(itemType, "Flare", 0.4f, 25f, 8f, 0.4f, false, true, true);
            case BATTERY_CELL -> new ItemDefinition(itemType, "Battery Cell", 0.1f, 20f, 0f, 0.3f, false, true, true);
            case NOISE_DECOY -> new ItemDefinition(itemType, "Noise Decoy", 0.8f, 40f, 10f, 0.6f, false, true, true);
            case KEY -> new ItemDefinition(itemType, "Key", 0.05f, 15f, 0f, 0.1f, false, true, false);
        };
    }

    public ItemType itemType() {
        return itemType;
    }

    public String displayName() {
        return displayName;
    }

    public float noiseMultiplier() {
        return noiseMultiplier;
    }

    public float breakThreshold() {
        return breakThreshold;
    }

    public float throwStrength() {
        return throwStrength;
    }

    public float mass() {
        return mass;
    }

    public boolean isCarriable() {
        return carriable;
    }

    public boolean isConsumable() {
        return consumable;
    }

    public boolean isStackable() {
        return stackable;
    }
}

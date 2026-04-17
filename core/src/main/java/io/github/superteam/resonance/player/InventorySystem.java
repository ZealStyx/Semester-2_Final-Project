package io.github.superteam.resonance.player;

import io.github.superteam.resonance.items.ItemDefinition;
import java.util.Objects;

/**
 * Four-slot inventory for consumables/tools with stack support.
 */
public final class InventorySystem {

    public static final int SLOT_COUNT = 4;

    private final InventorySlotEntry[] slots = new InventorySlotEntry[SLOT_COUNT];
    private int activeSlotIndex;

    public boolean addItem(ItemDefinition itemDefinition) {
        ItemDefinition item = Objects.requireNonNull(itemDefinition, "itemDefinition must not be null");

        if (item.isStackable()) {
            int existingIndex = findStackableSlotIndex(item.itemType());
            if (existingIndex >= 0) {
                slots[existingIndex].incrementCount();
                return true;
            }
        }

        int firstFreeIndex = findFirstFreeSlotIndex();
        if (firstFreeIndex < 0) {
            return false;
        }

        slots[firstFreeIndex] = new InventorySlotEntry(item, 1);
        return true;
    }

    public InventorySlotEntry removeItem(int slotIndex) {
        validateSlotIndex(slotIndex);
        InventorySlotEntry removed = slots[slotIndex];
        slots[slotIndex] = null;
        return removed;
    }

    public boolean consumeActiveItem() {
        InventorySlotEntry activeEntry = slots[activeSlotIndex];
        if (activeEntry == null) {
            return false;
        }

        if (activeEntry.itemDefinition().isStackable() && activeEntry.count() > 1) {
            activeEntry.decrementCount();
            return true;
        }

        slots[activeSlotIndex] = null;
        return true;
    }

    public InventorySlotEntry getActiveSlotEntry() {
        return slots[activeSlotIndex];
    }

    public InventorySlotEntry getSlotEntry(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slots[slotIndex];
    }

    public void setActiveSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        activeSlotIndex = slotIndex;
    }

    public void cycleActiveSlot(int delta) {
        if (delta == 0) {
            return;
        }

        int wrapped = (activeSlotIndex + delta) % SLOT_COUNT;
        if (wrapped < 0) {
            wrapped += SLOT_COUNT;
        }
        activeSlotIndex = wrapped;
    }

    public int getActiveSlotIndex() {
        return activeSlotIndex;
    }

    public boolean isFull() {
        return findFirstFreeSlotIndex() < 0;
    }

    public boolean isEmpty() {
        for (InventorySlotEntry slot : slots) {
            if (slot != null) {
                return false;
            }
        }
        return true;
    }

    private int findFirstFreeSlotIndex() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private int findStackableSlotIndex(io.github.superteam.resonance.items.ItemType itemType) {
        for (int i = 0; i < slots.length; i++) {
            InventorySlotEntry entry = slots[i];
            if (entry != null && entry.itemDefinition().itemType() == itemType) {
                return i;
            }
        }
        return -1;
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            throw new IllegalArgumentException("slotIndex out of range: " + slotIndex);
        }
    }

    /**
     * Immutable item identity with mutable stack count.
     */
    public static final class InventorySlotEntry {
        private final ItemDefinition itemDefinition;
        private int count;

        private InventorySlotEntry(ItemDefinition itemDefinition, int count) {
            this.itemDefinition = itemDefinition;
            this.count = count;
        }

        public ItemDefinition itemDefinition() {
            return itemDefinition;
        }

        public int count() {
            return count;
        }

        private void incrementCount() {
            count++;
        }

        private void decrementCount() {
            count--;
        }
    }
}

package io.github.superteam.resonance.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.superteam.resonance.items.ItemDefinition;
import io.github.superteam.resonance.items.ItemType;
import org.junit.Test;

public class InventorySystemTest {

    @Test
    public void shouldAddAndRemoveFromSlots() {
        InventorySystem inventorySystem = new InventorySystem();

        assertTrue(inventorySystem.addItem(ItemDefinition.create(ItemType.KEY)));
        assertNotNull(inventorySystem.getSlotEntry(0));

        InventorySystem.InventorySlotEntry removed = inventorySystem.removeItem(0);
        assertNotNull(removed);
        assertNull(inventorySystem.getSlotEntry(0));
    }

    @Test
    public void shouldRejectAddWhenAllSlotsAreFull() {
        InventorySystem inventorySystem = new InventorySystem();

        assertTrue(inventorySystem.addItem(ItemDefinition.create(ItemType.KEY)));
        assertTrue(inventorySystem.addItem(ItemDefinition.create(ItemType.METAL_PIPE)));
        assertTrue(inventorySystem.addItem(ItemDefinition.create(ItemType.CONCRETE_CHUNK)));
        assertTrue(inventorySystem.addItem(ItemDefinition.create(ItemType.GLASS_BOTTLE)));

        assertTrue(inventorySystem.isFull());
        assertFalse(inventorySystem.addItem(ItemDefinition.create(ItemType.CARDBOARD_BOX)));
    }

    @Test
    public void shouldStackStackableItemsInSingleSlot() {
        InventorySystem inventorySystem = new InventorySystem();

        assertTrue(inventorySystem.addItem(ItemDefinition.create(ItemType.FLARE)));
        assertTrue(inventorySystem.addItem(ItemDefinition.create(ItemType.FLARE)));

        InventorySystem.InventorySlotEntry slotEntry = inventorySystem.getSlotEntry(0);
        assertNotNull(slotEntry);
        assertEquals(2, slotEntry.count());

        assertTrue(inventorySystem.consumeActiveItem());
        assertEquals(1, inventorySystem.getSlotEntry(0).count());

        assertTrue(inventorySystem.consumeActiveItem());
        assertNull(inventorySystem.getSlotEntry(0));
    }

    @Test
    public void shouldWrapActiveSlotWhenCycling() {
        InventorySystem inventorySystem = new InventorySystem();

        inventorySystem.setActiveSlot(0);
        inventorySystem.cycleActiveSlot(-1);
        assertEquals(3, inventorySystem.getActiveSlotIndex());

        inventorySystem.cycleActiveSlot(2);
        assertEquals(1, inventorySystem.getActiveSlotIndex());
    }
}

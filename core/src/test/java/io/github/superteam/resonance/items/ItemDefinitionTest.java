package io.github.superteam.resonance.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ItemDefinitionTest {

    @Test
    public void shouldCreateConfiguredCarriableProfiles() {
        ItemDefinition metalPipe = ItemDefinition.create(ItemType.METAL_PIPE);
        ItemDefinition cardboardBox = ItemDefinition.create(ItemType.CARDBOARD_BOX);
        ItemDefinition glassBottle = ItemDefinition.create(ItemType.GLASS_BOTTLE);

        assertTrue(metalPipe.isCarriable());
        assertTrue(cardboardBox.isCarriable());
        assertTrue(glassBottle.isCarriable());

        assertEquals(1.4f, metalPipe.noiseMultiplier(), 0.0001f);
        assertEquals(0.3f, cardboardBox.noiseMultiplier(), 0.0001f);
        assertEquals(1.2f, glassBottle.noiseMultiplier(), 0.0001f);
    }

    @Test
    public void shouldCreateStackableConsumables() {
        ItemDefinition flare = ItemDefinition.create(ItemType.FLARE);
        ItemDefinition battery = ItemDefinition.create(ItemType.BATTERY_CELL);

        assertTrue(flare.isConsumable());
        assertTrue(flare.isStackable());

        assertTrue(battery.isConsumable());
        assertTrue(battery.isStackable());
        assertFalse(battery.isCarriable());
    }
}

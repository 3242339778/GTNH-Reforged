package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerNeighborLookupTest {

    @Test
    void loadedBlockEntityRetainsExactObjectIdentity() {
        Object tile = new Object();

        LegacyPowerNeighborLookup result = LegacyPowerNeighborLookup.loadedWithBlockEntity(tile);

        assertEquals(LegacyPowerNeighborLookup.Status.LOADED_WITH_BLOCK_ENTITY, result.status());
        assertTrue(result.isLoaded());
        assertTrue(result.hasBlockEntity());
        assertSame(tile, result.tileOrNull());
    }

    @Test
    void loadedEmptyPositionIsLoadedButHasNoTile() {
        LegacyPowerNeighborLookup result = LegacyPowerNeighborLookup.loadedWithoutBlockEntity();

        assertEquals(LegacyPowerNeighborLookup.Status.LOADED_WITHOUT_BLOCK_ENTITY, result.status());
        assertTrue(result.isLoaded());
        assertFalse(result.hasBlockEntity());
        assertNull(result.tileOrNull());
    }

    @Test
    void unloadedPositionIsNotCollapsedIntoLoadedEmpty() {
        LegacyPowerNeighborLookup result = LegacyPowerNeighborLookup.unloaded();

        assertEquals(LegacyPowerNeighborLookup.Status.UNLOADED, result.status());
        assertFalse(result.isLoaded());
        assertFalse(result.hasBlockEntity());
        assertNull(result.tileOrNull());
    }

    @Test
    void loadedBlockEntityFactoryRejectsNull() {
        assertThrows(NullPointerException.class, () -> LegacyPowerNeighborLookup.loadedWithBlockEntity(null));
    }

    @Test
    void recordRejectsInconsistentStatusAndTilePairs() {
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyPowerNeighborLookup(LegacyPowerNeighborLookup.Status.LOADED_WITH_BLOCK_ENTITY, null));
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyPowerNeighborLookup(LegacyPowerNeighborLookup.Status.UNLOADED, new Object()));
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyPowerNeighborLookup(LegacyPowerNeighborLookup.Status.LOADED_WITHOUT_BLOCK_ENTITY,
                new Object()));
    }
}

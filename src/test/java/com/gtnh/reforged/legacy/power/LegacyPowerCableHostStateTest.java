package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerCableHostStateTest {

    @Test
    void hostBundleOwnsStableCableStateAndNeighborCacheInstances() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();

        assertSame(host.cableState(), host.cableState());
        assertSame(host.neighborCache(), host.neighborCache());
    }

    @Test
    void validateClearsOnlyNegativeCacheMarkers() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();
        Object neighbor = new Object();
        host.neighborCache().remember(
            LegacyPowerDirection.NORTH,
            LegacyPowerNeighborLookup.loadedWithoutBlockEntity());
        host.neighborCache().remember(
            LegacyPowerDirection.SOUTH,
            LegacyPowerNeighborLookup.loadedWithBlockEntity(neighbor));

        host.onValidate();

        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
            host.neighborCache().cachedResult(LegacyPowerDirection.NORTH, false, tile -> false).status());
        LegacyPowerNeighborCache.CacheResult positive =
            host.neighborCache().cachedResult(LegacyPowerDirection.SOUTH, false, tile -> false);
        assertEquals(LegacyPowerNeighborCache.CacheStatus.TILE, positive.status());
        assertSame(neighbor, positive.tile());
    }

    @Test
    void invalidateChunkUnloadAndNeighborChangeClearOnlyNegativeMarkers() {
        for (Lifecycle lifecycle : Lifecycle.values()) {
            LegacyPowerCableHostState host = new LegacyPowerCableHostState();
            Object neighbor = new Object();
            host.neighborCache().remember(
                LegacyPowerDirection.WEST,
                LegacyPowerNeighborLookup.loadedWithoutBlockEntity());
            host.neighborCache().remember(
                LegacyPowerDirection.EAST,
                LegacyPowerNeighborLookup.loadedWithBlockEntity(neighbor));

            switch (lifecycle) {
                case INVALIDATE -> host.onInvalidate();
                case CHUNK_UNLOAD -> host.onChunkUnload();
                case ADJACENT_CHANGE -> host.onAdjacentBlockChange();
            }

            assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
                host.neighborCache().cachedResult(LegacyPowerDirection.WEST, false, tile -> false).status());
            LegacyPowerNeighborCache.CacheResult positive =
                host.neighborCache().cachedResult(LegacyPowerDirection.EAST, false, tile -> false);
            assertEquals(LegacyPowerNeighborCache.CacheStatus.TILE, positive.status());
            assertSame(neighbor, positive.tile());
        }
    }

    @Test
    void chunkUnloadThenSetRemovedIsClassifiedAsChunkLifecycle() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();

        host.onChunkUnload();

        assertEquals(
            LegacyPowerCableHostState.SetRemovedContext.AFTER_CHUNK_UNLOAD,
            host.onSetRemoved());
    }

    @Test
    void setRemovedWithoutChunkUnloadIsNotAutomaticallyTreatedAsChunkUnload() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();

        assertEquals(
            LegacyPowerCableHostState.SetRemovedContext.WITHOUT_CHUNK_UNLOAD,
            host.onSetRemoved());
    }

    @Test
    void loadAfterChunkUnloadResetsPendingChunkUnloadContext() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();
        host.onChunkUnload();

        host.onValidate();

        assertEquals(
            LegacyPowerCableHostState.SetRemovedContext.WITHOUT_CHUNK_UNLOAD,
            host.onSetRemoved());
    }

    @Test
    void positionChangeClearsBothNegativeAndPositiveEntries() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();
        Object neighbor = new Object();
        host.neighborCache().remember(
            LegacyPowerDirection.NORTH,
            LegacyPowerNeighborLookup.loadedWithoutBlockEntity());
        host.neighborCache().remember(
            LegacyPowerDirection.SOUTH,
            LegacyPowerNeighborLookup.loadedWithBlockEntity(neighbor));

        host.onPositionChanged();

        for (LegacyPowerDirection side : LegacyPowerDirection.orderedValues()) {
            assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
                host.neighborCache().cachedResult(side, false, tile -> false).status());
        }
    }

    private enum Lifecycle {
        INVALIDATE,
        CHUNK_UNLOAD,
        ADJACENT_CHANGE
    }
}

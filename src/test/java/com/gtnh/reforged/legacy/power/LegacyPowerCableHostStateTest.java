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
    void neighborChangeClearsNegativeMarkersAndInvalidatesGraphWhenEndpointOccupancyChanges() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();
        LegacyPowerGraphNode root = new LegacyPowerGraphNode(1, new Object(), host.cableState());
        host.cableState().setNode(root);
        host.cableState().setOccupiedConnectedSides(0b000001);
        host.neighborCache().remember(
            LegacyPowerDirection.NORTH,
            LegacyPowerNeighborLookup.loadedWithoutBlockEntity());
        host.neighborCache().remember(
            LegacyPowerDirection.SOUTH,
            LegacyPowerNeighborLookup.loadedWithBlockEntity(new Object()));

        host.prepareAdjacentBlockChange();
        boolean changed = host.onAdjacentBlockChange(0b000101, 72L);

        assertTrue(changed);
        assertEquals(0b000101, host.cableState().getOccupiedConnectedSides());
        assertFalse(root.getGraphState().isNodeMapValid());
        assertEquals(72L, root.getGraphState().getTopologyChangeTick());
        assertEquals(1L, root.getGraphState().getTopologyVersion());
        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
            host.neighborCache().cachedResult(LegacyPowerDirection.NORTH, false, tile -> false).status());
        assertEquals(LegacyPowerNeighborCache.CacheStatus.TILE,
            host.neighborCache().cachedResult(LegacyPowerDirection.SOUTH, false, tile -> false).status());
    }

    @Test
    void neighborChangeWithoutDirectNodeOnlyClearsNegativeMarkers() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();
        host.neighborCache().remember(
            LegacyPowerDirection.NORTH,
            LegacyPowerNeighborLookup.loadedWithoutBlockEntity());

        host.prepareAdjacentBlockChange();
        assertFalse(host.onAdjacentBlockChange(0b000001, 90L));
        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
            host.neighborCache().cachedResult(LegacyPowerDirection.NORTH, false, tile -> false).status());
    }

    @Test
    void unchangedEndpointOccupancyDoesNotInvalidateGraphAgain() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();
        LegacyPowerGraphNode root = new LegacyPowerGraphNode(1, new Object(), host.cableState());
        host.cableState().setNode(root);
        host.cableState().setOccupiedConnectedSides(0b000001);

        assertFalse(host.onAdjacentBlockChange(0b000001, 100L));
        assertTrue(root.getGraphState().isNodeMapValid());
        assertEquals(0L, root.getGraphState().getTopologyVersion());
    }

    @Test
    void chunkUnloadImmediatelyClearsGraphAndNegativeCacheMarkers() {
        LegacyPowerCableHostState host = new LegacyPowerCableHostState();
        LegacyPowerGraphNode root = new LegacyPowerGraphNode(1, new Object(), host.cableState());
        host.cableState().setNode(root);
        host.neighborCache().remember(
            LegacyPowerDirection.NORTH,
            LegacyPowerNeighborLookup.loadedWithoutBlockEntity());

        host.onChunkUnload();

        assertTrue(root.isInvalid());
        assertNull(host.cableState().getNode());
        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
            host.neighborCache().cachedResult(LegacyPowerDirection.NORTH, false, tile -> false).status());
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

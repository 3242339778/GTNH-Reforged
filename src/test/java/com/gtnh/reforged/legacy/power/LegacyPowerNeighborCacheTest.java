package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerNeighborCacheTest {

    private final LegacyPowerNeighborCache cache = new LegacyPowerNeighborCache();

    @Test
    void loadedEmptyLookupIsCachedAsEmptySentinel() {
        cache.remember(LegacyPowerDirection.NORTH, LegacyPowerNeighborLookup.loadedWithoutBlockEntity());

        LegacyPowerNeighborCache.CacheResult result =
            cache.cachedResult(LegacyPowerDirection.NORTH, false, tile -> false);

        assertEquals(LegacyPowerNeighborCache.CacheStatus.EMPTY, result.status());
        assertNull(result.tile());
    }

    @Test
    void unloadedLookupRemainsUncached() {
        cache.remember(LegacyPowerDirection.NORTH, LegacyPowerNeighborLookup.unloaded());

        LegacyPowerNeighborCache.CacheResult result =
            cache.cachedResult(LegacyPowerDirection.NORTH, false, tile -> false);

        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS, result.status());
    }

    @Test
    void loadedTileIsCachedWithExactObjectIdentity() {
        Object tile = new Object();
        cache.remember(LegacyPowerDirection.EAST, LegacyPowerNeighborLookup.loadedWithBlockEntity(tile));

        LegacyPowerNeighborCache.CacheResult result =
            cache.cachedResult(LegacyPowerDirection.EAST, false, cached -> false);

        assertEquals(LegacyPowerNeighborCache.CacheStatus.TILE, result.status());
        assertSame(tile, result.tile());
    }

    @Test
    void crossChunkLookupAlwaysInvalidatesExistingEmptyMarker() {
        cache.remember(LegacyPowerDirection.WEST, LegacyPowerNeighborLookup.loadedWithoutBlockEntity());

        LegacyPowerNeighborCache.CacheResult result =
            cache.cachedResult(LegacyPowerDirection.WEST, true, tile -> false);

        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS, result.status());
    }

    @Test
    void crossChunkLookupAlwaysInvalidatesExistingTileReference() {
        Object tile = new Object();
        cache.remember(LegacyPowerDirection.WEST, LegacyPowerNeighborLookup.loadedWithBlockEntity(tile));

        LegacyPowerNeighborCache.CacheResult result =
            cache.cachedResult(LegacyPowerDirection.WEST, true, cached -> false);

        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS, result.status());
    }

    @Test
    void invalidCachedTileIsClearedAndRequiresFreshLookup() {
        Object tile = new Object();
        cache.remember(LegacyPowerDirection.SOUTH, LegacyPowerNeighborLookup.loadedWithBlockEntity(tile));

        LegacyPowerNeighborCache.CacheResult result =
            cache.cachedResult(LegacyPowerDirection.SOUTH, false, cached -> cached == tile);

        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS, result.status());
        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
            cache.cachedResult(LegacyPowerDirection.SOUTH, false, cached -> false).status());
    }

    @Test
    void clearEmptyMarkersPreservesPositiveTileReferences() {
        Object tile = new Object();
        cache.remember(LegacyPowerDirection.DOWN, LegacyPowerNeighborLookup.loadedWithoutBlockEntity());
        cache.remember(LegacyPowerDirection.UP, LegacyPowerNeighborLookup.loadedWithBlockEntity(tile));

        cache.clearEmptyMarkers();

        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
            cache.cachedResult(LegacyPowerDirection.DOWN, false, cached -> false).status());
        LegacyPowerNeighborCache.CacheResult positive =
            cache.cachedResult(LegacyPowerDirection.UP, false, cached -> false);
        assertEquals(LegacyPowerNeighborCache.CacheStatus.TILE, positive.status());
        assertSame(tile, positive.tile());
    }

    @Test
    void clearAllInvalidatesEveryDirection() {
        for (LegacyPowerDirection side : LegacyPowerDirection.orderedValues()) {
            cache.remember(side, LegacyPowerNeighborLookup.loadedWithoutBlockEntity());
        }

        cache.clearAll();

        for (LegacyPowerDirection side : LegacyPowerDirection.orderedValues()) {
            assertEquals(LegacyPowerNeighborCache.CacheStatus.MISS,
                cache.cachedResult(side, false, cached -> false).status());
        }
    }

    @Test
    void directionsHaveIndependentCacheSlots() {
        Object north = new Object();
        Object south = new Object();
        cache.remember(LegacyPowerDirection.NORTH, LegacyPowerNeighborLookup.loadedWithBlockEntity(north));
        cache.remember(LegacyPowerDirection.SOUTH, LegacyPowerNeighborLookup.loadedWithBlockEntity(south));

        assertSame(north, cache.cachedResult(LegacyPowerDirection.NORTH, false, cached -> false).tile());
        assertSame(south, cache.cachedResult(LegacyPowerDirection.SOUTH, false, cached -> false).tile());
    }

    @Test
    void cachedTileCoordinateMismatchReturnsNullEquivalentWithoutEviction() {
        Object tile = new Object();
        cache.remember(LegacyPowerDirection.NORTH, LegacyPowerNeighborLookup.loadedWithBlockEntity(tile));

        LegacyPowerNeighborCache.CacheResult mismatch =
            cache.cachedResult(LegacyPowerDirection.NORTH, false, cached -> false, cached -> false);

        assertEquals(LegacyPowerNeighborCache.CacheStatus.MISMATCH, mismatch.status());
        assertNull(mismatch.tile());

        LegacyPowerNeighborCache.CacheResult stillCached =
            cache.cachedResult(LegacyPowerDirection.NORTH, false, cached -> false, cached -> true);
        assertEquals(LegacyPowerNeighborCache.CacheStatus.TILE, stillCached.status());
        assertSame(tile, stillCached.tile());
    }

    @Test
    void unknownCannotAddressPhysicalNeighborCache() {
        assertThrows(IllegalArgumentException.class,
            () -> cache.cachedResult(LegacyPowerDirection.UNKNOWN, false, cached -> false));
        assertThrows(IllegalArgumentException.class,
            () -> cache.remember(LegacyPowerDirection.UNKNOWN, LegacyPowerNeighborLookup.unloaded()));
    }
}

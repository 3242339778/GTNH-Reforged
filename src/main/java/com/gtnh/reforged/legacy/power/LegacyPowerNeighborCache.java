package com.gtnh.reforged.legacy.power;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Pure-Java model of BaseTileEntity's six-slot adjacent-tile cache.
 *
 * <p>A null slot means unknown/not cached. A private sentinel means a lookup
 * completed and found no block entity. Any other value is the exact cached
 * host tile handle. Cross-chunk lookups always invalidate the slot before
 * consulting the world, because a neighboring chunk can load without issuing
 * a block update to this tile.</p>
 *
 * <p>This class does not access a world or load chunks. The host performs the
 * lookup only after {@link #cachedResult(LegacyPowerDirection, boolean,
 * Predicate)} returns {@link CacheStatus#MISS}, then records the result with
 * {@link #remember(LegacyPowerDirection, LegacyPowerNeighborLookup)}.</p>
 */
public final class LegacyPowerNeighborCache {

    private static final Object EMPTY = new Object();

    private final Object[] neighbors = new Object[6];

    /**
     * Returns the current cache result. For cross-chunk queries, the slot is
     * unconditionally cleared first, matching the legacy implementation.
     */
    public CacheResult cachedResult(
        LegacyPowerDirection side,
        boolean crossesChunkBorder,
        Predicate<Object> isInvalid) {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(isInvalid, "isInvalid");
        int index = requirePhysicalSide(side);

        if (crossesChunkBorder) {
            neighbors[index] = null;
            return CacheResult.miss();
        }

        Object cached = neighbors[index];
        if (cached == null) {
            return CacheResult.miss();
        }
        if (cached == EMPTY) {
            return CacheResult.empty();
        }
        if (isInvalid.test(cached)) {
            neighbors[index] = null;
            return CacheResult.miss();
        }
        return CacheResult.tile(cached);
    }

    /**
     * Stores a completed world lookup. Unloaded results remain uncached;
     * loaded-empty results use the private empty sentinel.
     */
    public void remember(LegacyPowerDirection side, LegacyPowerNeighborLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        int index = requirePhysicalSide(side);
        neighbors[index] = switch (lookup.status()) {
            case UNLOADED -> null;
            case LOADED_WITHOUT_BLOCK_ENTITY -> EMPTY;
            case LOADED_WITH_BLOCK_ENTITY -> lookup.tile();
        };
    }

    /**
     * Mirrors clearNullMarkersFromTileEntityBuffer(): only cached-empty
     * sentinels are cleared; positive tile references remain intact.
     */
    public void clearEmptyMarkers() {
        for (int i = 0; i < neighbors.length; i++) {
            if (neighbors[i] == EMPTY) {
                neighbors[i] = null;
            }
        }
    }

    /**
     * Mirrors clearTileEntityBuffer(): forget all six cached directions.
     */
    public void clearAll() {
        for (int i = 0; i < neighbors.length; i++) {
            neighbors[i] = null;
        }
    }

    private static int requirePhysicalSide(LegacyPowerDirection side) {
        Objects.requireNonNull(side, "side");
        int index = side.ordinalIndex();
        if (index < 0) {
            throw new IllegalArgumentException("UNKNOWN is not a physical neighbor-cache side");
        }
        return index;
    }

    public enum CacheStatus {
        MISS,
        EMPTY,
        TILE
    }

    public record CacheResult(CacheStatus status, Object tile) {

        public CacheResult {
            Objects.requireNonNull(status, "status");
            if ((status == CacheStatus.TILE) != (tile != null)) {
                throw new IllegalArgumentException("Only TILE cache results may carry a tile");
            }
        }

        public static CacheResult miss() {
            return new CacheResult(CacheStatus.MISS, null);
        }

        public static CacheResult empty() {
            return new CacheResult(CacheStatus.EMPTY, null);
        }

        public static CacheResult tile(Object tile) {
            return new CacheResult(CacheStatus.TILE, Objects.requireNonNull(tile, "tile"));
        }
    }
}

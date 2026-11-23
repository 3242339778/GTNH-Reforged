package com.gtnh.reforged.legacy.power.neoforge;

import com.gtnh.reforged.legacy.power.LegacyPowerDirection;
import com.gtnh.reforged.legacy.power.LegacyPowerNeighborCache;
import com.gtnh.reforged.legacy.power.LegacyPowerNeighborLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Server-world adapter for the legacy six-direction adjacent block-entity
 * lookup. The graph core remains independent of Minecraft classes.
 *
 * <p>Cross-chunk positions are checked with {@link ServerLevel#isLoaded}
 * before {@link ServerLevel#getBlockEntity} is called. This adapter never
 * requests a chunk and never force-loads an unloaded neighbor.</p>
 *
 * <p>Call from the logical server thread. The owning BlockEntity should retain
 * one instance and forward its legacy cache lifecycle callbacks to
 * {@link #clearEmptyMarkers()} and {@link #clearAll()}.</p>
 */
public final class NeoForgePowerNeighborAccess {

    private final ServerLevel level;
    private final BlockPos origin;
    private final LegacyPowerNeighborCache cache;

    public NeoForgePowerNeighborAccess(
        ServerLevel level,
        BlockPos origin,
        LegacyPowerNeighborCache cache) {
        this.level = Objects.requireNonNull(level, "level");
        this.origin = Objects.requireNonNull(origin, "origin").immutable();
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    /**
     * Resolves a neighbor while retaining the legacy cache's three states.
     * A coordinate-mismatched cached BlockEntity returns the legacy null
     * result without evicting that cached object.
     */
    public LegacyPowerNeighborLookup lookup(LegacyPowerDirection side) {
        Direction direction = toMinecraftDirection(side);
        BlockPos target = origin.relative(direction);
        boolean crossesChunkBorder = crossesChunkBorder(origin, target);

        LegacyPowerNeighborCache.CacheResult cached = cache.cachedResult(
            side,
            crossesChunkBorder,
            tile -> ((BlockEntity) tile).isRemoved(),
            tile -> ((BlockEntity) tile).getBlockPos().equals(target)
        );

        switch (cached.status()) {
            case EMPTY:
            case MISMATCH:
                return LegacyPowerNeighborLookup.loadedWithoutBlockEntity();
            case TILE:
                return LegacyPowerNeighborLookup.loadedWithBlockEntity((BlockEntity) cached.tile());
            case MISS:
                break;
        }

        // Match the legacy cross-chunk guard, but never ask the server to load
        // the missing chunk. Unloaded results are deliberately not cached.
        if (crossesChunkBorder && !level.isLoaded(target)) {
            LegacyPowerNeighborLookup unloaded = LegacyPowerNeighborLookup.unloaded();
            cache.remember(side, unloaded);
            return unloaded;
        }

        BlockEntity neighbor = level.getBlockEntity(target);
        LegacyPowerNeighborLookup result = neighbor == null
            ? LegacyPowerNeighborLookup.loadedWithoutBlockEntity()
            : LegacyPowerNeighborLookup.loadedWithBlockEntity(neighbor);
        cache.remember(side, result);
        return result;
    }

    /**
     * Nullable convenience view used by graph traversal, which historically
     * treats loaded-empty and unloaded neighbors identically.
     */
    public BlockEntity getNeighborOrNull(LegacyPowerDirection side) {
        return (BlockEntity) lookup(side).tileOrNull();
    }

    /** Mirrors BaseTileEntity.clearNullMarkersFromTileEntityBuffer(). */
    public void clearEmptyMarkers() {
        cache.clearEmptyMarkers();
    }

    /** Mirrors BaseTileEntity.clearTileEntityBuffer() on coordinate changes. */
    public void clearAll() {
        cache.clearAll();
    }

    public static Direction toMinecraftDirection(LegacyPowerDirection side) {
        Objects.requireNonNull(side, "side");
        return switch (side) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
            case UNKNOWN -> throw new IllegalArgumentException(
                "UNKNOWN is not a physical neighbor direction");
        };
    }

    private static boolean crossesChunkBorder(BlockPos origin, BlockPos target) {
        return (origin.getX() >> 4) != (target.getX() >> 4)
            || (origin.getZ() >> 4) != (target.getZ() >> 4);
    }
}

package com.gtnh.reforged.legacy.power;

import java.util.Objects;

/**
 * Minecraft-independent result of looking up an adjacent world position.
 *
 * <p>The three states deliberately keep an unloaded chunk distinct from a
 * loaded position without a tile. Callers must map these states according to
 * the specific legacy operation they are adapting; graph walking historically
 * maps both non-tile states to a null neighbor, while connection validation
 * must preserve the unloaded result.</p>
 *
 * <p>The tile handle is opaque and must retain host object identity.</p>
 */
public record LegacyPowerNeighborLookup(Status status, Object tile) {

    public LegacyPowerNeighborLookup {
        Objects.requireNonNull(status, "status");
        if (status == Status.LOADED_WITH_BLOCK_ENTITY && tile == null) {
            throw new IllegalArgumentException("A loaded block-entity result requires a tile handle");
        }
        if (status != Status.LOADED_WITH_BLOCK_ENTITY && tile != null) {
            throw new IllegalArgumentException("Only a loaded block-entity result may carry a tile handle");
        }
    }

    public static LegacyPowerNeighborLookup loadedWithBlockEntity(Object tile) {
        return new LegacyPowerNeighborLookup(Status.LOADED_WITH_BLOCK_ENTITY,
            Objects.requireNonNull(tile, "tile"));
    }

    public static LegacyPowerNeighborLookup loadedWithoutBlockEntity() {
        return new LegacyPowerNeighborLookup(Status.LOADED_WITHOUT_BLOCK_ENTITY, null);
    }

    public static LegacyPowerNeighborLookup unloaded() {
        return new LegacyPowerNeighborLookup(Status.UNLOADED, null);
    }

    public boolean isLoaded() {
        return status != Status.UNLOADED;
    }

    public boolean hasBlockEntity() {
        return status == Status.LOADED_WITH_BLOCK_ENTITY;
    }

    /**
     * Returns the exact host tile handle, or null for loaded-empty and unloaded.
     * This intentionally mirrors the nullable shape of legacy tile lookups.
     */
    public Object tileOrNull() {
        return tile;
    }

    public enum Status {
        LOADED_WITH_BLOCK_ENTITY,
        LOADED_WITHOUT_BLOCK_ENTITY,
        UNLOADED
    }
}

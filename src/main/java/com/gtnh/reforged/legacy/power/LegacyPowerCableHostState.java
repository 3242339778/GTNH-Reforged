package com.gtnh.reforged.legacy.power;

/**
 * Lifecycle-owned state bundle intended to be held by a NeoForge cable
 * BlockEntity.
 *
 * <p>The bundle deliberately does not extend BlockEntity and does not perform
 * world access. It keeps the extracted legacy graph state and six-direction
 * neighbor cache together, while the real host BlockEntity forwards its
 * lifecycle callbacks here.</p>
 *
 * <p>Legacy BaseTileEntity clears only negative-cache markers on validate,
 * invalidate, chunk unload, and adjacent-block changes. A coordinate change
 * clears every cached direction. Positive neighbor references therefore
 * survive the former callbacks but not a position change.</p>
 *
 * <p>Modern BlockEntity.setRemoved() is not equivalent to the old permanent
 * invalidate event: it can follow onChunkUnloaded(). The host therefore
 * records the chunk-unload callback so the adapter can distinguish that
 * sequence from a setRemoved() with no preceding chunk unload.</p>
 */
public final class LegacyPowerCableHostState {

    public enum SetRemovedContext {
        AFTER_CHUNK_UNLOAD,
        WITHOUT_CHUNK_UNLOAD
    }

    private final LegacyPowerCableState cableState = new LegacyPowerCableState();
    private final LegacyPowerNeighborCache neighborCache = new LegacyPowerNeighborCache();
    private boolean chunkUnloadPending;

    public LegacyPowerCableState cableState() {
        return cableState;
    }

    public LegacyPowerNeighborCache neighborCache() {
        return neighborCache;
    }

    /** Forward from BlockEntity.onLoad(); starts a new loaded lifecycle. */
    public void onValidate() {
        chunkUnloadPending = false;
        neighborCache.clearEmptyMarkers();
    }

    /** Forward from the old invalidate-equivalent cache cleanup only. */
    public void onInvalidate() {
        neighborCache.clearEmptyMarkers();
    }

    /** Forward from BlockEntity.onChunkUnloaded(). */
    public void onChunkUnload() {
        chunkUnloadPending = true;
        neighborCache.clearEmptyMarkers();
    }

    /**
     * Classifies the next setRemoved callback. This does not itself clear the
     * graph: the Minecraft-facing host must additionally establish whether
     * the block was actually replaced before requesting graph cleanup.
     */
    public SetRemovedContext onSetRemoved() {
        if (chunkUnloadPending) {
            chunkUnloadPending = false;
            return SetRemovedContext.AFTER_CHUNK_UNLOAD;
        }
        return SetRemovedContext.WITHOUT_CHUNK_UNLOAD;
    }

    /** Forward from the block's neighbor-update callback. */
    public void onAdjacentBlockChange() {
        neighborCache.clearEmptyMarkers();
    }

    /**
     * Forward only if the host's world position changes (for example, a
     * relocation-capable host). Normal Minecraft BlockEntities are position
     * bound and generally do not move.
     */
    public void onPositionChanged() {
        neighborCache.clearAll();
    }

    /**
     * The graph cleanup policy is intentionally explicit and separate from
     * cache lifecycle callbacks. Call this only for the upstream-equivalent
     * immediate graph invalidation event.
     */
    public void clearGraphImmediately() {
        cableState.clearGraphImmediately();
    }

    public void invalidateGraphForTopologyChange(long serverTick) {
        cableState.invalidateGraphForTopologyChange(serverTick);
    }
}

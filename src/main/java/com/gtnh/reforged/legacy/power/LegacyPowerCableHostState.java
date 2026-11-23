package com.gtnh.reforged.legacy.power;

import java.util.Objects;

/**
 * Lifecycle-owned state bundle intended to be held by a future NeoForge
 * cable BlockEntity.
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
 */
public final class LegacyPowerCableHostState {

    private final LegacyPowerCableState cableState = new LegacyPowerCableState();
    private final LegacyPowerNeighborCache neighborCache = new LegacyPowerNeighborCache();

    public LegacyPowerCableState cableState() {
        return cableState;
    }

    public LegacyPowerNeighborCache neighborCache() {
        return neighborCache;
    }

    /** Forward from BlockEntity.validate()/the corresponding load callback. */
    public void onValidate() {
        neighborCache.clearEmptyMarkers();
    }

    /** Forward from BlockEntity.invalidate()/the corresponding removal callback. */
    public void onInvalidate() {
        neighborCache.clearEmptyMarkers();
    }

    /** Forward from BlockEntity.onChunkUnloaded() or its 26.3 equivalent. */
    public void onChunkUnload() {
        neighborCache.clearEmptyMarkers();
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

    /**
     * Convenience guard for future host integration: the bundle is not
     * thread-safe and all graph/cache mutation must remain on the server thread.
     */
    public void requireServerThread(boolean isServerThread) {
        if (!isServerThread) {
            throw new IllegalStateException("Legacy power cable state must be accessed on the server thread");
        }
    }

    /**
     * Creates a world-query adapter over this host's own cache. The returned
     * adapter must not outlive the host BlockEntity's current position.
     */
    public NeoForgeLegacyPowerNeighborAccess createNeighborAccess(
        net.minecraft.server.level.ServerLevel level,
        net.minecraft.core.BlockPos position) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        return new NeoForgeLegacyPowerNeighborAccess(level, position, neighborCache);
    }
}

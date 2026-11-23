package com.gtnh.reforged.block;

import com.gtnh.reforged.GTNHReforged;
import com.gtnh.reforged.legacy.power.LegacyPowerCableHostState;
import com.gtnh.reforged.legacy.power.LegacyPowerDirection;
import com.gtnh.reforged.legacy.power.LegacyPowerNeighborLookup;
import com.gtnh.reforged.legacy.power.LegacyPowerRemovalDecision;
import com.gtnh.reforged.legacy.power.bridge.neoforge.NeoForgeLegacyPowerNeighborAccess;
import com.gtnh.reforged.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * First real NeoForge host for the extracted legacy cable graph state.
 *
 * <p>The host owns one stable pure-Java state bundle. World access is exposed
 * only through an explicitly-created server-side bridge.</p>
 */
public final class PrototypePowerCableBlockEntity extends BlockEntity {

    private final LegacyPowerCableHostState hostState = new LegacyPowerCableHostState();

    public PrototypePowerCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PROTOTYPE_POWER_CABLE_ENTITY.get(), pos, state);
    }

    public LegacyPowerCableHostState hostState() {
        return hostState;
    }

    /**
     * Creates a short-lived world adapter over this host's stable neighbor
     * cache. Call only on the logical server thread.
     */
    public NeoForgeLegacyPowerNeighborAccess createNeighborAccess() {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Power-cable neighbor access is server-side only");
        }
        return new NeoForgeLegacyPowerNeighborAccess(
            serverLevel,
            getBlockPos(),
            hostState.neighborCache());
    }

    /**
     * Called by the owning Block for each neighbor notification. The cache
     * marker reset is unconditional; endpoint occupancy scanning and graph
     * invalidation are server-only, matching the upstream callback.
     */
    public void onAdjacentBlockChange() {
        // This must precede the scan: clearing empty markers afterwards would
        // discard freshly observed empty results, while scanning beforehand
        // could hide a newly placed endpoint behind a stale empty marker.
        hostState.prepareAdjacentBlockChange();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        NeoForgeLegacyPowerNeighborAccess neighbors = createNeighborAccess();
        int connectedSides = hostState.cableState().getConnectionFlags();
        int occupiedSides = 0;
        for (LegacyPowerDirection side : LegacyPowerDirection.orderedValues()) {
            int flag = 1 << side.ordinalIndex();
            if ((connectedSides & flag) == 0) {
                continue;
            }
            LegacyPowerNeighborLookup lookup = neighbors.getNeighbor(side);
            if (lookup.hasBlockEntity()) {
                occupiedSides |= flag;
            }
        }
        hostState.onAdjacentBlockChange(occupiedSides, serverLevel.getGameTime());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        hostState.onValidate();
        logLifecycle("onLoad");
    }

    @Override
    public void onChunkUnloaded() {
        hostState.onChunkUnload();
        super.onChunkUnloaded();
        logLifecycle("onChunkUnloaded");
    }

    @Override
    public void setRemoved() {
        LegacyPowerCableHostState.SetRemovedContext context = hostState.onSetRemoved();
        super.setRemoved();

        LegacyPowerRemovalDecision.Result decision = classifyRemoval(context);
        boolean followedChunkUnload =
            context == LegacyPowerCableHostState.SetRemovedContext.AFTER_CHUNK_UNLOAD;
        boolean confirmedReplacement =
            decision == LegacyPowerRemovalDecision.Result.BLOCK_REPLACED
                || decision == LegacyPowerRemovalDecision.Result.HOST_REPLACED;

        // GT5U clears the graph in onUnload() for chunk unloads and in
        // invalidate() for permanent host/block replacement. Do not run the
        // permanent-removal path a second time after onChunkUnloaded().
        if (!followedChunkUnload && confirmedReplacement) {
            hostState.clearGraphImmediately();
        }
        if (!followedChunkUnload) {
            hostState.onInvalidate();
        }

        logLifecycle("setRemoved", decision.name());
    }

    /**
     * Inspects only an already-loaded server chunk. It never requests or
     * generates a chunk, and it never mutates the graph.
     */
    private LegacyPowerRemovalDecision.Result classifyRemoval(
        LegacyPowerCableHostState.SetRemovedContext context) {

        boolean chunkUnloadPending =
            context == LegacyPowerCableHostState.SetRemovedContext.AFTER_CHUNK_UNLOAD;
        if (chunkUnloadPending) {
            return LegacyPowerRemovalDecision.classify(true, false, false, false);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return LegacyPowerRemovalDecision.classify(false, false, false, false);
        }

        BlockPos pos = getBlockPos();
        LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(
            pos.getX() >> 4,
            pos.getZ() >> 4);
        if (chunk == null) {
            return LegacyPowerRemovalDecision.classify(false, false, false, false);
        }

        boolean currentBlockIsCable =
            chunk.getBlockState(pos).is(ModBlocks.PROTOTYPE_POWER_CABLE.get());
        BlockEntity currentBlockEntity = chunk.getBlockEntity(pos);

        return LegacyPowerRemovalDecision.classify(
            false,
            true,
            currentBlockIsCable,
            currentBlockEntity == this);
    }

    /** Temporary prototype diagnostic; remove or lower to debug after lifecycle validation. */
    private void logLifecycle(String callback) {
        logLifecycle(callback, "n/a");
    }

    private void logLifecycle(String callback, String context) {
        String side = level == null ? "detached" : level instanceof ServerLevel ? "server" : "client";
        GTNHReforged.LOGGER.info(
            "Prototype power cable lifecycle: callback={}, context={}, side={}, pos={}, hostState@{}",
            callback,
            context,
            side,
            getBlockPos(),
            Integer.toHexString(System.identityHashCode(hostState)));
    }
}

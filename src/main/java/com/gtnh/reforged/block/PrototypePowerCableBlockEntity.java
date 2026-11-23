package com.gtnh.reforged.block;

import com.gtnh.reforged.GTNHReforged;
import com.gtnh.reforged.legacy.power.LegacyPowerCableHostState;
import com.gtnh.reforged.legacy.power.bridge.neoforge.NeoForgeLegacyPowerNeighborAccess;
import com.gtnh.reforged.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
        hostState.onInvalidate();
        super.setRemoved();
        logLifecycle("setRemoved", context.name());
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

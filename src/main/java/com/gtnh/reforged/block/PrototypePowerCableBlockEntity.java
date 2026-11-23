package com.gtnh.reforged.block;

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
    }

    @Override
    public void onChunkUnloaded() {
        hostState.onChunkUnload();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        hostState.onInvalidate();
        super.setRemoved();
    }
}

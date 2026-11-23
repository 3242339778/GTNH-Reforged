package com.gtnh.reforged.block;

import com.gtnh.reforged.GTNHReforged;
import com.gtnh.reforged.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Temporary, read-only diagnostics for prototype power-cable chunk unloads.
 *
 * <p>This listener deliberately does not mutate cable or graph state. It
 * records the NeoForge chunk-unload event while the chunk and its block
 * entities are still available, allowing it to be compared with the
 * BlockEntity lifecycle callbacks.</p>
 */
public final class PrototypePowerCableChunkUnloadDiagnostics {

    private PrototypePowerCableChunkUnloadDiagnostics() {
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        LevelChunk chunk = event.getChunk();
        String side = chunk.getLevel().isClientSide() ? "client" : "server";
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        int cableCount = 0;

        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (!chunk.getBlockState(pos).is(ModBlocks.PROTOTYPE_POWER_CABLE.get())) {
                continue;
            }

            cableCount++;
            BlockEntity blockEntity = chunk.getBlockEntity(pos);
            if (blockEntity instanceof PrototypePowerCableBlockEntity cable) {
                GTNHReforged.LOGGER.info(
                    "Prototype power cable chunk unload: event=ChunkEvent.Unload, side={}, chunk=({}, {}), pos={}, hostState@{}",
                    side,
                    chunkX,
                    chunkZ,
                    pos,
                    Integer.toHexString(System.identityHashCode(cable.hostState())));
            } else {
                GTNHReforged.LOGGER.info(
                    "Prototype power cable chunk unload: event=ChunkEvent.Unload, side={}, chunk=({}, {}), pos={}, blockEntity={}",
                    side,
                    chunkX,
                    chunkZ,
                    pos,
                    blockEntity == null ? "null" : blockEntity.getClass().getName());
            }
        }

        GTNHReforged.LOGGER.info(
            "Prototype power cable chunk unload: event=ChunkEvent.Unload, side={}, chunk=({}, {}), cableCount={}",
            side,
            chunkX,
            chunkZ,
            cableCount);
    }
}

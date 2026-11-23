package com.gtnh.reforged.block;

import com.gtnh.reforged.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Deliberately minimal registered host for the legacy power-cable migration.
 *
 * <p>This prototype provides a real BlockEntity lifecycle owner. It does not
 * yet implement GT connection policy, cable geometry, covers, or energy IO.</p>
 */
public final class PrototypePowerCableBlock extends Block {

    public PrototypePowerCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrototypePowerCableBlockEntity(pos, state);
    }

    @Override
    protected @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        net.minecraft.world.level.Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType) {
        return null;
    }

    public static boolean isCorrectBlockEntity(BlockEntity blockEntity) {
        return ModBlocks.PROTOTYPE_POWER_CABLE_ENTITY.get().isValid(blockEntity);
    }
}

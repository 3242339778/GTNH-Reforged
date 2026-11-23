package com.gtnh.reforged.registry;

import com.gtnh.reforged.GTNHReforged;
import com.gtnh.reforged.block.PrototypePowerCableBlock;
import com.gtnh.reforged.block.PrototypePowerCableBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Block and BlockEntity registrations for GTNH: Reforged. */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(GTNHReforged.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GTNHReforged.MOD_ID);

    public static final DeferredBlock<PrototypePowerCableBlock> PROTOTYPE_POWER_CABLE =
        BLOCKS.registerBlock(
            "prototype_power_cable",
            PrototypePowerCableBlock::new,
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());

    public static final Supplier<BlockEntityType<PrototypePowerCableBlockEntity>> PROTOTYPE_POWER_CABLE_ENTITY =
        BLOCK_ENTITY_TYPES.register(
            "prototype_power_cable",
            () -> BlockEntityType.Builder.of(
                PrototypePowerCableBlockEntity::new,
                PROTOTYPE_POWER_CABLE.get()).build(null));

    private ModBlocks() {
    }
}

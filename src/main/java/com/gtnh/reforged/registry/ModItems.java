package com.gtnh.reforged.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.gtnh.reforged.GTNHReforged;

/** Registered item entries owned by GTNH: Reforged. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GTNHReforged.MOD_ID);

    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> IRON_INGOT = ITEMS.registerSimpleItem("iron_ingot");
    public static final DeferredItem<BlockItem> PROTOTYPE_POWER_CABLE =
        ITEMS.register("prototype_power_cable",
            () -> new BlockItem(ModBlocks.PROTOTYPE_POWER_CABLE.get(), new Item.Properties()));

    private ModItems() {
    }
}

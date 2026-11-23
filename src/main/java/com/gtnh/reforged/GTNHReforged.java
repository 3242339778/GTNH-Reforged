package com.gtnh.reforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.gtnh.reforged.materials.BuiltInMaterials;
import com.gtnh.reforged.materials.MaterialCatalog;
import com.gtnh.reforged.recipes.BuiltInRecipes;
import com.gtnh.reforged.recipes.RecipeCatalog;
import com.gtnh.reforged.registry.ModItems;
import com.gtnh.reforged.registry.ModBlocks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import com.gtnh.reforged.block.PrototypePowerCableChunkUnloadDiagnostics;

/** Main entry point for GTNH: Reforged. */
@Mod(GTNHReforged.MOD_ID)
public final class GTNHReforged {
    public static final String MOD_ID = "gtnh_reforged";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GTNHReforged(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(new PrototypePowerCableChunkUnloadDiagnostics());
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);

        MaterialCatalog materialCatalog = BuiltInMaterials.createInitialCatalog();
        RecipeCatalog recipeCatalog = BuiltInRecipes.createInitialCatalog(materialCatalog);
        LOGGER.info("GTNH: Reforged bootstrap initialized. Loaded {} material definition(s), {} recipe definition(s), and registered the initial iron dust/ingot items plus the prototype power-cable host.",
                materialCatalog.size(), recipeCatalog.size());
    }
}

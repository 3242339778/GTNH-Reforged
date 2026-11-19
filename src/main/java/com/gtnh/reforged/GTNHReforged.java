package com.gtnh.reforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.gtnh.reforged.materials.BuiltInMaterials;
import com.gtnh.reforged.materials.MaterialCatalog;
import com.gtnh.reforged.recipes.BuiltInRecipes;
import com.gtnh.reforged.recipes.RecipeCatalog;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Main entry point for GTNH: Reforged. */
@Mod(GTNHReforged.MOD_ID)
public final class GTNHReforged {
    public static final String MOD_ID = "gtnh_reforged";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GTNHReforged(IEventBus modEventBus, ModContainer modContainer) {
        MaterialCatalog materialCatalog = BuiltInMaterials.createInitialCatalog();
        RecipeCatalog recipeCatalog = BuiltInRecipes.createInitialCatalog();
        LOGGER.info("GTNH: Reforged bootstrap initialized. Loaded {} material definition(s) and {} recipe definition(s).",
                materialCatalog.size(), recipeCatalog.size());
    }
}
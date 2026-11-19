package com.gtnh.reforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Main entry point for GTNH: Reforged.
 *
 * <p>This is intentionally a small bootstrap. Gameplay systems will be added
 * behind explicit module boundaries after the platform skeleton is verified.</p>
 */
@Mod(GTNHReforged.MOD_ID)
public final class GTNHReforged {
    public static final String MOD_ID = "gtnh_reforged";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GTNHReforged(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("GTNH: Reforged bootstrap initialized for Minecraft 26.3 / NeoForge.");
    }
}

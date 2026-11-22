package com.gtnh.reforged.legacy.power;

/**
 * Narrow host contract used by the Minecraft-independent legacy path model.
 *
 * <p>A NeoForge-side adapter can implement this interface around a real
 * GTNH-compatible pipe object. The graph core only needs to read and replace
 * the pipe's current path binding; it does not need a Minecraft class.</p>
 */
public interface LegacyPowerPipeBinding {

    LegacyPowerPath getNodePath();

    void setNodePath(LegacyPowerPath path);
}

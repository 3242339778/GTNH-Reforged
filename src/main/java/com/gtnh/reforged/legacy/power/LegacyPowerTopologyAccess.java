package com.gtnh.reforged.legacy.power;

/**
 * World-facing operations required by the legacy degree-two topology walk.
 *
 * <p>Implementations must preserve object identity for tile, node, and map
 * handles. Direction order is supplied by {@link LegacyPowerDirection}.</p>
 */
public interface LegacyPowerTopologyAccess {

    boolean isPipe(Object tile);

    /** Returns the pipe's current node binding, or null. */
    Object getNode(Object pipe);

    /** Returns the pipe's current node-map owner, or null. */
    Object getNodeMapOwner(Object pipe);

    boolean isConnected(Object pipe, LegacyPowerDirection side);

    /** Returns the adjacent tile, or null when absent/unloaded. */
    Object getNeighbor(Object pipe, LegacyPowerDirection side);

    /** Whether this exact node object is already in the map being generated. */
    boolean containsNode(Object node);

    /** Whether the supplied map owner is already in the map being generated. */
    boolean containsMapOwner(Object owner);

    /** Clear a separate, previously valid graph before its cables are rebound. */
    void clearNodeMap(Object owner, int returnNodeValue);

    /** True only for a cable that is a known energy source. */
    boolean isEnergySource(Object pipe);
}

package com.gtnh.reforged.legacy.power;

import java.util.List;

/**
 * Host operations needed to extract GenerateNodeMap.generateNode and
 * generateNextNode without importing Minecraft or Forge types.
 *
 * <p>Implementations must preserve identity for tile, node, and pipe handles.
 * addConsumer must append the created consumer node to the supplied list when
 * it returns true, matching the upstream addConsumer contract.</p>
 */
public interface LegacyPowerNodeBuildAccess extends LegacyPowerTopologyAccess {

    boolean isTileInvalid(Object tile);

    int getConnectionCount(Object pipe);

    LegacyPowerNodeBinding getPipeBinding(Object pipe);

    /** Mirrors the overridable getEmptyNode hook; null means no dead-end node. */
    LegacyPowerGraphNode createEmptyNode(int nodeValue, LegacyPowerDirection side,
        Object tile, List<LegacyPowerGraphNode> consumers);

    /** Mirrors the overridable getPipeNode hook. */
    LegacyPowerGraphNode createPipeNode(int nodeValue, LegacyPowerDirection side,
        Object tile, List<LegacyPowerGraphNode> consumers);

    /** Mirrors addConsumer; on true, the new consumer must be the list's last entry. */
    boolean addConsumer(Object tile, LegacyPowerDirection side, int nodeValue,
        List<LegacyPowerGraphNode> consumers);

    /** Creates a path and binds all supplied pipe tiles to it, in supplied order. */
    LegacyPowerPath createPath(List<Object> pipeTiles);

    /** Legacy ForgeDirection.flag value, not ordinal. */
    int getSideFlag(LegacyPowerDirection side);

    void setOccupiedConnectedSides(Object pipe, int flags);

    void reloadPipeLocks(Object pipe);

    /** Mirrors NodePath.reloadLocks over every pipe in the path. */
    void reloadPathLocks(LegacyPowerPath path);
}

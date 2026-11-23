package com.gtnh.reforged.legacy.power;

import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Pure-Java state owner matching the graph-related fields and lifecycle rules
 * of GT5-Unofficial 5.09.54.275 BaseMetaPipeEntity.
 *
 * <p>This is not a Minecraft block entity. A future NeoForge cable block
 * entity can delegate its graph bindings and connection snapshots to this
 * class while keeping world access in a separate host adapter.</p>
 */
public final class LegacyPowerCableState
    implements LegacyPowerNodeBinding, LegacyPowerPipeBinding {

    private LegacyPowerGraphNode node;
    private LegacyPowerPath nodePath;
    private int connectionFlags;
    private boolean metaTilePresent;
    private int nodePathConnections;
    private int occupiedConnectedSides;

    @Override
    public LegacyPowerGraphNode getNode() {
        return node;
    }

    @Override
    public void setNode(LegacyPowerGraphNode node) {
        this.node = node;
    }

    @Override
    public LegacyPowerPath getNodePath() {
        return nodePath;
    }

    /**
     * Mirrors BaseMetaPipeEntity.setNodePath: capture the current connection
     * bitmask at bind time; do not continuously track later connection edits.
     */
    @Override
    public void setNodePath(LegacyPowerPath nodePath) {
        this.nodePath = nodePath;
        nodePathConnections = nodePath == null || !metaTilePresent ? 0 : connectionFlags;
    }

    /**
     * Supplies the current MetaPipeEntity connection bitmask and whether its
     * meta-tile object is present. The legacy path snapshot is not changed here.
     */
    public void setConnectionState(int connectionFlags, boolean metaTilePresent) {
        this.connectionFlags = connectionFlags;
        this.metaTilePresent = metaTilePresent;
    }

    public int getNodePathConnections() {
        return nodePathConnections;
    }

    /**
     * Mirrors BaseMetaPipeEntity.getNodeMap(): a direct node takes precedence;
     * otherwise the graph owner is resolved through the current path.
     */
    public Object getNodeMapOwner() {
        if (node != null) {
            return node;
        }
        return nodePath == null ? null : nodePath.getNodeMapOwner();
    }

    public int getConnectionFlags() {
        return connectionFlags;
    }

    public boolean hasMetaTile() {
        return metaTilePresent;
    }

    public boolean hasDirectNode() {
        return node != null;
    }

    public int getOccupiedConnectedSides() {
        return occupiedConnectedSides;
    }

    public void setOccupiedConnectedSides(int occupiedConnectedSides) {
        this.occupiedConnectedSides = occupiedConnectedSides;
    }

    /**
     * Applies a fresh world scan. Returns true only when endpoint occupancy
     * changed, even if the six connection bits themselves did not change.
     */
    public boolean updateOccupiedConnectedSides(int observedSides) {
        if (occupiedConnectedSides == observedSides) {
            return false;
        }
        occupiedConnectedSides = observedSides;
        return true;
    }

    /**
     * Mirrors markCableTopologyChanged(false): invalidate the shared map
     * root and record the server tick without immediately clearing the graph.
     */
    public void invalidateGraphForTopologyChange(long serverTick) {
        Object owner = getNodeMapOwner();
        if (owner instanceof LegacyPowerGraphNode graphNode) {
            graphNode.invalidateNodeMap(serverTick);
        }
    }

    /**
     * Mirrors BaseMetaPipeEntity.invalidateNodeMap(): a direct node causes
     * immediate graph cleanup; an intermediate path delegates cleanup to its
     * graph owner (or clears itself if unowned).
     */
    public void clearGraphImmediately() {
        if (node != null) {
            LegacyPowerGraphCleaner.clearNodeMap(node, -1);
        } else if (nodePath != null) {
            nodePath.invalidateNodeMap();
        }
    }

    /**
     * Mirrors invalidateNodePaths after a connection removal. The caller
     * supplies the six legacy ForgeDirection.flag values in ordinal order;
     * these are bitmasks, not direction ordinals.
     */
    public void invalidateAffectedPaths(
        int removedConnections,
        ToIntFunction<LegacyPowerDirection> legacySideFlag) {
        Objects.requireNonNull(legacySideFlag, "legacySideFlag");
        if (node != null) {
            for (LegacyPowerDirection side : LegacyPowerDirection.orderedValues()) {
                int flag = legacySideFlag.applyAsInt(side);
                if ((removedConnections & flag) != 0) {
                    LegacyPowerPath path = node.getPath(side);
                    if (path != null) {
                        path.invalidate();
                    }
                }
            }
        } else if (nodePath != null && (removedConnections & nodePathConnections) != 0) {
            nodePath.invalidate();
        }
    }
}

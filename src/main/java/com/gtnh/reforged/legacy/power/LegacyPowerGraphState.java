package com.gtnh.reforged.legacy.power;

/**
 * Minecraft-independent representation of the shared topology state used by
 * GTNH's legacy power graph.
 *
 * <p>This intentionally models only the node-map ownership and invalidation
 * contract found in GT5-Unofficial 5.09.54.275. It is not a replacement for
 * GenerateNodeMap, NodePath, PowerNodes, or the original distribution logic.</p>
 *
 * <p>Instances are expected to be mutated on the logical server thread, just
 * like the legacy graph they model. This class is not thread-safe.</p>
 */
public final class LegacyPowerGraphState {
    /**
     * GTNH 5.09.54.275 waits ten server ticks after invalidation before a
     * refresh is considered due.
     */
    public static final long REFRESH_DELAY_TICKS = 10L;

    private LegacyPowerGraphState nodeMapRoot = this;
    private boolean nodeMapValid = true;
    private long topologyVersion;
    private long topologyChangeTick;

    /**
     * Join this node's map ownership to the root currently used by
     * {@code previousNode}. A null previous node leaves this node unchanged.
     *
     * <p>This mirrors the upstream one-way root assignment; it is deliberately
     * not a union-find merge operation.</p>
     */
    public void joinNodeMap(LegacyPowerGraphState previousNode) {
        if (previousNode != null) {
            nodeMapRoot = previousNode.nodeMapRoot;
        }
    }

    public boolean isNodeMapValid() {
        return nodeMapRoot.nodeMapValid;
    }

    /**
     * Invalidates the shared root, increments its topology version, and
     * records the server tick at which the change was observed.
     */
    public void invalidateNodeMap(long currentServerTick) {
        LegacyPowerGraphState root = nodeMapRoot;
        root.nodeMapValid = false;
        root.topologyVersion++;
        root.topologyChangeTick = currentServerTick;
    }

    /**
     * Returns whether the invalid map has passed GTNH's delayed-refresh
     * threshold. This does not rebuild the graph.
     */
    public boolean isNodeMapRefreshDue(long currentServerTick) {
        LegacyPowerGraphState root = nodeMapRoot;
        return !root.nodeMapValid
            && currentServerTick - root.topologyChangeTick >= REFRESH_DELAY_TICKS;
    }

    public long getTopologyVersion() {
        return nodeMapRoot.topologyVersion;
    }

    public long getTopologyChangeTick() {
        return nodeMapRoot.topologyChangeTick;
    }

    /**
     * Returns this node's current map root. Exposed for diagnostics and tests;
     * callers must not mutate the returned state except through this API.
     */
    public LegacyPowerGraphState getNodeMapRoot() {
        return nodeMapRoot;
    }
}

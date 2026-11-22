package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java node structure for the legacy GTNH graph.
 *
 * <p>Neighbor and path slots are indexed by {@link LegacyPowerDirection}
 * ordinal, matching the six-element arrays in GT5-Unofficial 5.09.54.275.
 * This is a topology model, not the energy-distribution implementation.</p>
 */
public final class LegacyPowerGraphNode implements LegacyPowerPathOwner {

    private final Object tileIdentity;
    private final LegacyPowerNodeBinding pipeBinding;
    private final LegacyPowerGraphState graphState = new LegacyPowerGraphState();
    private final LegacyPowerGraphNode[] neighbors = new LegacyPowerGraphNode[6];
    private final LegacyPowerPath[] paths = new LegacyPowerPath[6];
    private final LegacyPowerPathLock[] locks = new LegacyPowerPathLock[6];
    private final List<LegacyPowerPath> unroutedPaths = new ArrayList<>();

    private LegacyPowerPath selfPath;
    private int nodeValue;
    private int highestNodeValue;
    private boolean invalid;
    private LegacyPowerPath returnPath;
    private LegacyPowerPathLock returnLock;
    private boolean hadVoltage;

    public LegacyPowerGraphNode(int nodeValue, Object tileIdentity,
        LegacyPowerNodeBinding pipeBinding) {
        this.nodeValue = nodeValue;
        this.highestNodeValue = nodeValue;
        this.tileIdentity = tileIdentity;
        this.pipeBinding = pipeBinding;
    }

    public Object getTileIdentity() {
        return tileIdentity;
    }

    public LegacyPowerNodeBinding getPipeBinding() {
        return pipeBinding;
    }

    public LegacyPowerGraphState getGraphState() {
        return graphState;
    }

    /** Mirrors Node.joinNodeMap: a one-way assignment to the previous node's current root. */
    public void joinNodeMap(LegacyPowerGraphNode previousNode) {
        if (previousNode != null) {
            graphState.joinNodeMap(previousNode.graphState);
        }
    }

    public void invalidateNodeMap(long currentServerTick) {
        graphState.invalidateNodeMap(currentServerTick);
    }

    public int getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(int nodeValue) {
        this.nodeValue = nodeValue;
    }

    public int getHighestNodeValue() {
        return highestNodeValue;
    }

    public void setHighestNodeValue(int highestNodeValue) {
        this.highestNodeValue = highestNodeValue;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void markInvalid() {
        invalid = true;
    }

    public LegacyPowerGraphNode getNeighbor(LegacyPowerDirection side) {
        return neighbors[side.ordinalIndex()];
    }

    public void setNeighbor(LegacyPowerDirection side, LegacyPowerGraphNode node) {
        neighbors[side.ordinalIndex()] = node;
    }

    public LegacyPowerPath getPath(LegacyPowerDirection side) {
        return paths[side.ordinalIndex()];
    }

    public void setPath(LegacyPowerDirection side, LegacyPowerPath path) {
        // One edge path is referenced by both endpoint nodes. Its owner remains
        // the node that created it, as in upstream NodePath.setNodeMap.
        paths[side.ordinalIndex()] = path;
    }

    public LegacyPowerPathLock getLock(LegacyPowerDirection side) {
        return locks[side.ordinalIndex()];
    }

    public void setLock(LegacyPowerDirection side, LegacyPowerPathLock lock) {
        locks[side.ordinalIndex()] = lock;
    }

    public boolean hasHadVoltage() {
        return hadVoltage;
    }

    public void setHadVoltage(boolean hadVoltage) {
        this.hadVoltage = hadVoltage;
    }

    public LegacyPowerPath getReturnPath() {
        return returnPath;
    }

    public LegacyPowerPathLock getReturnLock() {
        return returnLock;
    }

    public void setReturnValues(LegacyPowerPath path, LegacyPowerPathLock lock) {
        this.returnPath = path;
        this.returnLock = lock;
    }

    public LegacyPowerPath getSelfPath() {
        return selfPath;
    }

    public void setSelfPath(LegacyPowerPath selfPath) {
        this.selfPath = selfPath;
        if (selfPath != null) {
            selfPath.setNodeMapOwner(this);
        }
    }

    public void addUnroutedPath(LegacyPowerPath path) {
        path.setNodeMapOwner(this);
        unroutedPaths.add(path);
    }

    public List<LegacyPowerPath> getUnroutedPaths() {
        return List.copyOf(unroutedPaths);
    }

    List<LegacyPowerPath> mutableUnroutedPaths() {
        return unroutedPaths;
    }

    @Override
    public void clearNodeMap(int returnNodeValue) {
        LegacyPowerGraphCleaner.clearNodeMap(this, returnNodeValue);
    }
}

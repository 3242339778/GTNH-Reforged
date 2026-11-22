package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pure-Java extraction of GenerateNodeMap.generateNode and generateNextNode
 * from GT5-Unofficial 5.09.54.275.
 *
 * <p>This class preserves the upstream node/path/lock wiring. It is not an
 * energy distributor and does not replace PowerNodes or PowerNodePath.</p>
 */
public final class LegacyPowerNodeBuilder {

    private final LegacyPowerNodeBuildAccess access;
    private final Set<LegacyPowerGraphNode> nodeMap =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<LegacyPowerGraphNode> consumers = new ArrayList<>();

    public LegacyPowerNodeBuilder(LegacyPowerNodeBuildAccess access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    /** Builds a graph starting at the supplied tile, as upstream does with no previous node. */
    public LegacyPowerGraphNode build(Object firstTile, LegacyPowerDirection enteredFrom) {
        Objects.requireNonNull(enteredFrom, "enteredFrom");
        LegacyPowerGraphNode root = generateNode(firstTile, null, 1, List.of(), enteredFrom);
        return root;
    }

    public Set<LegacyPowerGraphNode> getNodeMap() {
        return Collections.unmodifiableSet(nodeMap);
    }

    public List<LegacyPowerGraphNode> getConsumers() {
        return List.copyOf(consumers);
    }

    private LegacyPowerGraphNode generateNode(Object tile, LegacyPowerGraphNode previousNode,
        int nextNodeValue, List<Object> intermediatePipes, LegacyPowerDirection side) {
        if (tile == null || access.isTileInvalid(tile)) {
            return null;
        }

        LegacyPowerDirection oppositeSide = side.opposite();
        LegacyPowerDirection invalidSide =
            previousNode == null ? null : oppositeSide;

        if (access.isPipe(tile)) {
            int connections = access.getConnectionCount(tile);
            LegacyPowerGraphNode pipeNode;
            if (connections == 1) {
                pipeNode = access.createEmptyNode(nextNodeValue, oppositeSide, tile, consumers);
                if (pipeNode == null) {
                    return null;
                }
            } else {
                pipeNode = access.createPipeNode(nextNodeValue, oppositeSide, tile, consumers);
                if (pipeNode == null) {
                    throw new IllegalStateException("createPipeNode returned null");
                }
            }

            pipeNode.joinNodeMap(previousNode);
            LegacyPowerNodeBinding binding = access.getPipeBinding(tile);
            if (binding == null) {
                throw new IllegalStateException("Pipe node has no LegacyPowerNodeBinding");
            }
            binding.setNode(pipeNode);
            nodeMap.add(pipeNode);

            LegacyPowerPath selfPath = requirePath(List.of(tile));
            pipeNode.setSelfPath(selfPath);
            if (invalidSide != null) {
                pipeNode.setNeighbor(invalidSide, previousNode);

                LegacyPowerPath incomingPath = requirePath(intermediatePipes);
                incomingPath.setNodeMapOwner(pipeNode);
                LegacyPowerPathLock lock = new LegacyPowerPathLock();
                incomingPath.setLock(lock);
                pipeNode.setPath(invalidSide, incomingPath);
                pipeNode.setLock(invalidSide, lock);
                previousNode.setReturnValues(incomingPath, lock);
            }

            if (connections > 1) {
                generateNextNode(tile, pipeNode, invalidSide, nextNodeValue);
            }
            return pipeNode;
        }

        int consumerCountBefore = consumers.size();
        if (access.addConsumer(tile, oppositeSide, nextNodeValue, consumers)) {
            if (consumers.size() != consumerCountBefore + 1) {
                throw new IllegalStateException("addConsumer must append exactly one consumer node");
            }
            LegacyPowerGraphNode consumerNode = consumers.get(consumers.size() - 1);
            if (consumerNode == null) {
                throw new IllegalStateException("addConsumer appended a null consumer");
            }
            consumerNode.joinNodeMap(previousNode);
            nodeMap.add(consumerNode);
            consumerNode.setNeighbor(oppositeSide, previousNode);

            LegacyPowerPath consumerPath = requirePath(intermediatePipes);
            consumerPath.setNodeMapOwner(consumerNode);
            LegacyPowerPathLock lock = new LegacyPowerPathLock();
            consumerPath.setLock(lock);
            consumerNode.setPath(oppositeSide, consumerPath);
            // Upstream ConsumerNode does not populate its direction-lock slot;
            // the shared Lock lives on the path and is returned to the parent.
            if (previousNode == null) {
                throw new IllegalStateException("A consumer cannot be the graph root");
            }
            previousNode.setReturnValues(consumerPath, lock);
            return consumerNode;
        }
        return null;
    }

    private void generateNextNode(Object pipe, LegacyPowerGraphNode pipeNode,
        LegacyPowerDirection invalidSide, int initialNodeValue) {
        int occupiedSides = invalidSide == null ? 0 : access.getSideFlag(invalidSide);
        int nextNodeValue = initialNodeValue;

        for (LegacyPowerDirection side : LegacyPowerDirection.orderedValues()) {
            if (side == invalidSide || !access.isConnected(pipe, side)) {
                continue;
            }
            Object nextTile = access.getNeighbor(pipe, side);
            if (nextTile == null) {
                continue;
            }

            occupiedSides |= access.getSideFlag(side);
            LegacyPowerTopologyWalker.WalkResult walk = createWalker().walkDetailed(nextTile, side);
            LegacyPowerGraphNode nextNode = null;
            if (walk != null && walk.endpoint() != null) {
                nextNode = generateNode(walk.endpoint(), pipeNode, nextNodeValue + 1,
                    walk.intermediatePipes(), walk.enteredFrom());
                if (nextNode != null) {
                    nextNodeValue = nextNode.getHighestNodeValue();
                    pipeNode.setHighestNodeValue(nextNode.getHighestNodeValue());
                    pipeNode.setNeighbor(side, nextNode);

                    LegacyPowerPath returnPath = pipeNode.getReturnPath();
                    LegacyPowerPathLock returnLock = pipeNode.getReturnLock();
                    if (returnPath == null || returnLock == null) {
                        throw new IllegalStateException("Child node did not provide its return path and lock");
                    }
                    pipeNode.setPath(side, returnPath);
                    pipeNode.setLock(side, returnLock);
                    access.reloadPathLocks(returnPath);
                }
            }

            if (nextNode == null && walk != null && !walk.intermediatePipes().isEmpty()) {
                LegacyPowerPath unrouted = requirePath(walk.intermediatePipes());
                pipeNode.addUnroutedPath(unrouted);
            }
        }

        access.setOccupiedConnectedSides(pipe, occupiedSides);
        access.reloadPipeLocks(pipe);
    }

    private LegacyPowerTopologyWalker createWalker() {
        LegacyPowerTopologyAccess scoped = new LegacyPowerTopologyAccess() {
            @Override public boolean isPipe(Object tile) { return access.isPipe(tile); }
            @Override public Object getNode(Object pipe) { return access.getNode(pipe); }
            @Override public Object getNodeMapOwner(Object pipe) { return access.getNodeMapOwner(pipe); }
            @Override public boolean isConnected(Object pipe, LegacyPowerDirection side) {
                return access.isConnected(pipe, side);
            }
            @Override public Object getNeighbor(Object pipe, LegacyPowerDirection side) {
                return access.getNeighbor(pipe, side);
            }
            @Override public boolean containsNode(Object node) {
                return nodeMap.contains(node);
            }
            @Override public boolean containsMapOwner(Object owner) {
                return owner instanceof LegacyPowerGraphNode graphNode && nodeMap.contains(graphNode);
            }
            @Override public void clearNodeMap(Object owner, int returnNodeValue) {
                if (owner instanceof LegacyPowerGraphNode graphNode) {
                    LegacyPowerGraphCleaner.clearNodeMap(graphNode, returnNodeValue);
                } else {
                    access.clearNodeMap(owner, returnNodeValue);
                }
            }
            @Override public boolean isEnergySource(Object pipe) {
                return access.isEnergySource(pipe);
            }
        };
        return new LegacyPowerTopologyWalker(scoped);
    }

    private LegacyPowerPath requirePath(List<Object> pipeTiles) {
        LegacyPowerPath path = access.createPath(pipeTiles);
        if (path == null) {
            throw new IllegalStateException("createPath returned null");
        }
        return path;
    }
}

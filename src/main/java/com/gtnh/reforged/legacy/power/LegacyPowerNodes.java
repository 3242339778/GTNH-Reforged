package com.gtnh.reforged.legacy.power;

/**
 * Minecraft-independent extraction of GTNH 5.09.54.275 PowerNodes.
 *
 * <p>Preserves node-value routing, direction scan order, consumer cursor
 * behavior, path/lock validation, voltage-loss order, topology-version
 * invalidation, and partial-transfer accounting. This class assumes it is
 * called on the logical server thread, as is the original implementation.</p>
 */
public final class LegacyPowerNodes {
    private LegacyPowerNodes() {}

    public static long powerNode(
            LegacyPowerGraphNode currentNode,
            LegacyPowerGraphNode previousNode,
            LegacyPowerConsumerList consumers,
            long voltage,
            long maxAmps) {
        long ampsUsed = 0;
        LegacyPowerConsumerAccess consumer = consumers.getNode();
        while (consumer != null && !currentNode.isInvalid()) {
            int consumerPosition = consumers.getPosition();
            int targetValue = consumer.getNode().getNodeValue();

            if (targetValue < currentNode.getNodeValue()
                    || targetValue > currentNode.getHighestNodeValue()) {
                for (int side = 0; side < 6; side++) {
                    LegacyPowerGraphNode next = currentNode.getNeighbor(LegacyPowerDirection.fromOrdinalIndex(side));
                    if (next != null && next.getNodeValue() < currentNode.getNodeValue()) {
                        if (next.getNodeValue() == targetValue) {
                            ampsUsed += processNodeInject(currentNode, consumer, consumers,
                                    LegacyPowerDirection.fromOrdinalIndex(side), maxAmps - ampsUsed, voltage);
                            consumer = consumers.getNextNode();
                        } else {
                            if (previousNode == next) return ampsUsed;
                            ampsUsed += processNextNode(currentNode, next, consumers,
                                    LegacyPowerDirection.fromOrdinalIndex(side), maxAmps - ampsUsed, voltage);
                            consumer = consumers.getNode();
                        }
                        break;
                    }
                }
            } else {
                for (int side = 5; side >= 0; side--) {
                    LegacyPowerGraphNode next = currentNode.getNeighbor(LegacyPowerDirection.fromOrdinalIndex(side));
                    if (next == null) continue;
                    if (next.getNodeValue() > currentNode.getNodeValue()
                            && next.getNodeValue() < targetValue) {
                        if (next == previousNode) return ampsUsed;
                        ampsUsed += processNextNodeAbove(currentNode, next, consumers,
                                LegacyPowerDirection.fromOrdinalIndex(side), maxAmps - ampsUsed, voltage);
                        consumer = consumers.getNode();
                        break;
                    } else if (next.getNodeValue() == targetValue) {
                        ampsUsed += processNodeInject(currentNode, consumer, consumers,
                                LegacyPowerDirection.fromOrdinalIndex(side), maxAmps - ampsUsed, voltage);
                        consumer = consumers.getNextNode();
                        break;
                    }
                }
            }

            if (maxAmps - ampsUsed <= 0) return ampsUsed;
            if (consumers.isStale() || currentNode.isInvalid()) return ampsUsed;
            if (consumerPosition == consumers.getPosition()) {
                throw new IllegalStateException("Power-node traversal made no progress");
            }
        }
        return ampsUsed;
    }

    public static long powerNodeAbove(
            LegacyPowerGraphNode currentNode,
            LegacyPowerGraphNode previousNode,
            LegacyPowerConsumerList consumers,
            long voltage,
            long maxAmps) {
        long ampsUsed = 0;
        LegacyPowerConsumerAccess consumer = consumers.getNode();
        while (consumer != null && !currentNode.isInvalid()) {
            int consumerPosition = consumers.getPosition();
            int targetValue = consumer.getNode().getNodeValue();
            if (targetValue > currentNode.getHighestNodeValue()
                    || targetValue < currentNode.getNodeValue()) {
                return ampsUsed;
            }
            for (int side = 5; side >= 0; side--) {
                LegacyPowerGraphNode next = currentNode.getNeighbor(LegacyPowerDirection.fromOrdinalIndex(side));
                if (next == null) continue;
                if (next.getNodeValue() > currentNode.getNodeValue()
                        && next.getNodeValue() < targetValue) {
                    if (next == previousNode) return ampsUsed;
                    ampsUsed += processNextNodeAbove(currentNode, next, consumers,
                            LegacyPowerDirection.fromOrdinalIndex(side), maxAmps - ampsUsed, voltage);
                    consumer = consumers.getNode();
                    break;
                } else if (next.getNodeValue() == targetValue) {
                    ampsUsed += processNodeInject(currentNode, consumer, consumers,
                            LegacyPowerDirection.fromOrdinalIndex(side), maxAmps - ampsUsed, voltage);
                    consumer = consumers.getNextNode();
                    break;
                }
            }
            if (maxAmps - ampsUsed <= 0) return ampsUsed;
            if (consumers.isStale() || currentNode.isInvalid()) return ampsUsed;
            if (consumerPosition == consumers.getPosition()) {
                throw new IllegalStateException("Power-node traversal made no progress");
            }
        }
        return ampsUsed;
    }

    private static long processNextNode(
            LegacyPowerGraphNode current, LegacyPowerGraphNode next,
            LegacyPowerConsumerList consumers, LegacyPowerDirection side,
            long maxAmps, long voltage) {
        LegacyPowerNodePath path = requirePowerPath(current.getPath(side));
        if (!path.isValid()) {
            consumers.markStale();
            return 0;
        }
        if (current.getLock(side).isLocked()) {
            consumers.getNextNode();
            return 0;
        }
        LegacyPowerNodePath selfPath = optionalPowerPath(current.getSelfPath());
        long voltageLoss = 0;
        if (selfPath != null) {
            voltageLoss += selfPath.getLoss();
            selfPath.applyVoltage(voltage, false);
        }
        if (current.isInvalid()) return 0;
        path.applyVoltage(voltage - voltageLoss, true);
        if (current.isInvalid()) return 0;
        voltageLoss += path.getLoss();
        long used = powerNode(next, current, consumers, voltage - voltageLoss, maxAmps);
        path.addAmps(used, voltage - voltageLoss);
        if (selfPath != null) selfPath.addAmps(used, voltage - selfPath.getLoss());
        return used;
    }

    private static long processNextNodeAbove(
            LegacyPowerGraphNode current, LegacyPowerGraphNode next,
            LegacyPowerConsumerList consumers, LegacyPowerDirection side,
            long maxAmps, long voltage) {
        LegacyPowerNodePath path = requirePowerPath(current.getPath(side));
        if (!path.isValid()) {
            consumers.markStale();
            return 0;
        }
        if (current.getLock(side).isLocked()) {
            consumers.getNextNode();
            return 0;
        }
        LegacyPowerNodePath selfPath = optionalPowerPath(current.getSelfPath());
        long voltageLoss = 0;
        if (selfPath != null) {
            voltageLoss += selfPath.getLoss();
            selfPath.applyVoltage(voltage, false);
        }
        if (current.isInvalid()) return 0;
        path.applyVoltage(voltage - voltageLoss, true);
        if (current.isInvalid()) return 0;
        voltageLoss += path.getLoss();
        long used = powerNodeAbove(next, current, consumers, voltage - voltageLoss, maxAmps);
        path.addAmps(used, voltage - voltageLoss);
        if (selfPath != null) selfPath.addAmps(used, voltage - selfPath.getLoss());
        return used;
    }

    private static long processNodeInject(
            LegacyPowerGraphNode current, LegacyPowerConsumerAccess consumer,
            LegacyPowerConsumerList consumers, LegacyPowerDirection side,
            long maxAmps, long voltage) {
        LegacyPowerNodePath path = requirePowerPath(current.getPath(side));
        if (!path.isValid()) {
            consumers.markStale();
            return 0;
        }
        if (current.getLock(side).isLocked()) return 0;

        LegacyPowerNodePath selfPath = optionalPowerPath(current.getSelfPath());
        long voltageLoss = 0;
        if (selfPath != null) {
            voltageLoss += selfPath.getLoss();
            selfPath.applyVoltage(voltage, false);
        }
        if (current.isInvalid()) return 0;
        path.applyVoltage(voltage - voltageLoss, true);
        if (current.isInvalid()) return 0;
        voltageLoss += path.getLoss();

        long topologyVersion = current.getGraphState().getTopologyVersion();
        long used = voltage > voltageLoss
                ? consumer.injectEnergy(voltage - voltageLoss, maxAmps)
                : 0;
        if (topologyVersion != current.getGraphState().getTopologyVersion()) {
            consumers.markStale();
        }
        path.addAmps(used, voltage - voltageLoss);
        if (selfPath != null) selfPath.addAmps(used, voltage - selfPath.getLoss());
        return used;
    }

    private static LegacyPowerNodePath requirePowerPath(LegacyPowerPath path) {
        if (!(path instanceof LegacyPowerNodePath powerPath)) {
            throw new IllegalStateException("Expected LegacyPowerNodePath, got " + path);
        }
        return powerPath;
    }

    private static LegacyPowerNodePath optionalPowerPath(LegacyPowerPath path) {
        return path == null ? null : requirePowerPath(path);
    }
}

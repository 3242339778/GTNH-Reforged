package com.gtnh.reforged.legacy.power;

/**
 * Ordered extraction of GenerateNodeMap.clearNodeMap from GT5-Unofficial
 * 5.09.54.275. The return-node-value recursion rule is intentionally kept.
 */
public final class LegacyPowerGraphCleaner {

    private LegacyPowerGraphCleaner() {
    }

    public static void clearNodeMap(LegacyPowerGraphNode node, int returnNodeValue) {
        node.markInvalid();

        LegacyPowerNodeBinding pipeBinding = node.getPipeBinding();
        if (pipeBinding != null) {
            if (pipeBinding.getNode() == node) {
                pipeBinding.setNode(null);
            }
            LegacyPowerPath selfPath = node.getSelfPath();
            if (selfPath != null) {
                selfPath.clearPath();
                node.setSelfPath(null);
            }
        }

        for (LegacyPowerPath path : node.mutableUnroutedPaths()) {
            path.clearPath();
        }
        node.mutableUnroutedPaths().clear();

        for (LegacyPowerDirection side : LegacyPowerDirection.orderedValues()) {
            LegacyPowerPath path = node.getPath(side);
            if (path != null) {
                path.clearPath();
                node.setPath(side, null);
            }

            LegacyPowerGraphNode nextNode = node.getNeighbor(side);
            if (nextNode == null) {
                continue;
            }
            if (nextNode.getNodeValue() != returnNodeValue) {
                clearNodeMap(nextNode, node.getNodeValue());
            }
            node.setNeighbor(side, null);
        }
    }
}

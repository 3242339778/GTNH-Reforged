package com.gtnh.reforged.legacy.power;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerGraphCleanerTest {

    @Test
    void clearsLineAndDetachesOnlyCurrentPipeNodeBindings() {
        FakePipe firstPipe = new FakePipe();
        FakePipe secondPipe = new FakePipe();
        LegacyPowerGraphNode first = node(1, firstPipe);
        LegacyPowerGraphNode second = node(2, secondPipe);
        firstPipe.node = first;
        secondPipe.node = second;
        first.setNeighbor(LegacyPowerDirection.EAST, second);
        second.setNeighbor(LegacyPowerDirection.WEST, first);

        LegacyPowerGraphCleaner.clearNodeMap(first, -1);

        assertTrue(first.isInvalid());
        assertTrue(second.isInvalid());
        assertNull(firstPipe.node);
        assertNull(secondPipe.node);
        assertNull(first.getNeighbor(LegacyPowerDirection.EAST));
        assertNull(second.getNeighbor(LegacyPowerDirection.WEST));
    }

    @Test
    void clearsBranchAndLoopClosingPathUsingOriginalTreeTopology() {
        LegacyPowerGraphNode root = node(1, null);
        LegacyPowerGraphNode left = node(2, null);
        LegacyPowerGraphNode right = node(3, null);
        LegacyPowerGraphNode branchEnd = node(4, null);
        FakePipe loopPipe = new FakePipe();
        LegacyPowerPath loopClosingPath = new LegacyPowerPath(List.of(loopPipe));

        root.setNeighbor(LegacyPowerDirection.NORTH, left);
        root.setNeighbor(LegacyPowerDirection.SOUTH, right);
        left.setNeighbor(LegacyPowerDirection.WEST, branchEnd);
        left.setNeighbor(LegacyPowerDirection.EAST, root);
        right.setNeighbor(LegacyPowerDirection.NORTH, root);
        branchEnd.setNeighbor(LegacyPowerDirection.EAST, left);

        // The upstream builder does not add a second neighbor edge for a loop.
        // It records the un-routed cable run on the owning node instead.
        root.addUnroutedPath(loopClosingPath);

        LegacyPowerGraphCleaner.clearNodeMap(root, -1);

        for (LegacyPowerGraphNode node : List.of(root, left, right, branchEnd)) {
            assertTrue(node.isInvalid());
        }
        assertFalse(loopClosingPath.isValid());
        assertNull(loopPipe.path);
        assertNull(root.getNeighbor(LegacyPowerDirection.NORTH));
        assertNull(root.getNeighbor(LegacyPowerDirection.SOUTH));
        assertNull(left.getNeighbor(LegacyPowerDirection.WEST));
        assertNull(left.getNeighbor(LegacyPowerDirection.EAST));
        assertNull(right.getNeighbor(LegacyPowerDirection.NORTH));
        assertNull(branchEnd.getNeighbor(LegacyPowerDirection.EAST));
    }

    @Test
    void clearingOldNodeDoesNotDetachPipeAlreadyReboundToReplacementNode() {
        FakePipe pipe = new FakePipe();
        LegacyPowerGraphNode oldNode = node(1, pipe);
        LegacyPowerGraphNode replacement = node(2, pipe);
        pipe.node = replacement;

        LegacyPowerGraphCleaner.clearNodeMap(oldNode, -1);

        assertTrue(oldNode.isInvalid());
        assertSame(replacement, pipe.node);
    }

    @Test
    void clearsSelfAndUnroutedPathsAndSixDirectionalPaths() {
        FakePipe selfPipe = new FakePipe();
        FakePipe selfPathPipe = new FakePipe();
        FakePipe edgePipe = new FakePipe();
        FakePipe unroutedPipe = new FakePipe();
        LegacyPowerGraphNode node = node(1, selfPipe);
        selfPipe.node = node;

        LegacyPowerPath selfPath = new LegacyPowerPath(List.of(selfPathPipe));
        LegacyPowerPath edgePath = new LegacyPowerPath(List.of(edgePipe));
        LegacyPowerPath unroutedPath = new LegacyPowerPath(List.of(unroutedPipe));
        node.setSelfPath(selfPath);
        node.setPath(LegacyPowerDirection.UP, edgePath);
        node.addUnroutedPath(unroutedPath);

        LegacyPowerGraphCleaner.clearNodeMap(node, -1);

        assertFalse(selfPath.isValid());
        assertFalse(edgePath.isValid());
        assertFalse(unroutedPath.isValid());
        assertNull(selfPathPipe.path);
        assertNull(edgePipe.path);
        assertNull(unroutedPipe.path);
        assertNull(node.getSelfPath());
        assertNull(node.getPath(LegacyPowerDirection.UP));
        assertTrue(node.getUnroutedPaths().isEmpty());
    }

    @Test
    void joinedNodesShareInvalidationThroughTheGraphRoot() {
        LegacyPowerGraphNode first = node(1, null);
        LegacyPowerGraphNode second = node(2, null);
        second.joinNodeMap(first);

        second.invalidateNodeMap(100);

        assertFalse(first.getGraphState().isNodeMapValid());
        assertEquals(1, first.getGraphState().getTopologyVersion());
        assertEquals(100, first.getGraphState().getTopologyChangeTick());
    }

    @Test
    void pathInvalidationDelegatesToItsOwningGraphNode() {
        FakePipe pipe = new FakePipe();
        LegacyPowerGraphNode node = node(1, null);
        LegacyPowerPath path = new LegacyPowerPath(List.of(pipe));
        path.setNodeMapOwner(node);
        node.addUnroutedPath(path);

        path.invalidateNodeMap();

        assertTrue(node.isInvalid());
        assertFalse(path.isValid());
        assertNull(pipe.path);
    }

    private static LegacyPowerGraphNode node(int value, FakePipe pipe) {
        return new LegacyPowerGraphNode(value, new Object(), pipe);
    }

    private static final class FakePipe implements LegacyPowerPipeBinding, LegacyPowerNodeBinding {
        private LegacyPowerPath path;
        private LegacyPowerGraphNode node;

        @Override
        public LegacyPowerPath getNodePath() {
            return path;
        }

        @Override
        public void setNodePath(LegacyPowerPath path) {
            this.path = path;
        }

        @Override
        public LegacyPowerGraphNode getNode() {
            return node;
        }

        @Override
        public void setNode(LegacyPowerGraphNode node) {
            this.node = node;
        }
    }
}

package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerNodeBuilderTest {

    @Test
    void buildsConsumerEdgesWithTheSameLockAtBothEndpoints() {
        FakeTile rootTile = FakeTile.pipe();
        FakeTile consumerTile = FakeTile.consumer();
        connect(rootTile, LegacyPowerDirection.EAST, consumerTile);
        // A second connected-but-unloaded side makes this a normal pipe node;
        // upstream skips its absent neighbor during branch expansion.
        rootTile.connections.put(LegacyPowerDirection.NORTH, true);

        FakeAccess access = new FakeAccess();
        LegacyPowerNodeBuilder builder = new LegacyPowerNodeBuilder(access);
        LegacyPowerGraphNode root = builder.build(rootTile, LegacyPowerDirection.DOWN);

        assertNotNull(root);
        assertEquals(0, root.getNodeValue());
        assertEquals(1, root.getHighestNodeValue());
        assertEquals(2, builder.getNodeMap().size());
        assertEquals(1, builder.getConsumers().size());

        LegacyPowerGraphNode consumer = builder.getConsumers().get(0);
        LegacyPowerPath rootPath = root.getPath(LegacyPowerDirection.EAST);
        LegacyPowerPath consumerPath = consumer.getPath(LegacyPowerDirection.WEST);
        assertSame(consumer, root.getNeighbor(LegacyPowerDirection.EAST));
        assertSame(root, consumer.getNeighbor(LegacyPowerDirection.WEST));
        assertSame(rootPath, consumerPath);
        assertNull(consumer.getLock(LegacyPowerDirection.WEST));
        assertSame(rootPath.getLock(), root.getLock(LegacyPowerDirection.EAST));
        assertSame(consumer, rootPath.getNodeMapOwner());
        assertEquals(1, access.pathReloads);
        assertEquals(1, access.pipeReloads);
        assertEquals(access.flag(LegacyPowerDirection.EAST), rootTile.occupiedSides);
    }

    @Test
    void incrementsNodeValuesAcrossSiblingBranchesAndTracksHighestValue() {
        FakeTile rootTile = FakeTile.pipe();
        FakeTile eastConsumer = FakeTile.consumer();
        FakeTile southConsumer = FakeTile.consumer();
        connect(rootTile, LegacyPowerDirection.EAST, eastConsumer);
        connect(rootTile, LegacyPowerDirection.SOUTH, southConsumer);

        FakeAccess access = new FakeAccess();
        LegacyPowerNodeBuilder builder = new LegacyPowerNodeBuilder(access);
        LegacyPowerGraphNode root = builder.build(rootTile, LegacyPowerDirection.DOWN);

        assertEquals(2, builder.getConsumers().size());
        // The map contains the root pipe node plus both consumer nodes.
        assertEquals(3, builder.getNodeMap().size());
        assertEquals(2, root.getHighestNodeValue());
        assertEquals(1, builder.getConsumers().get(0).getNodeValue());
        assertEquals(2, builder.getConsumers().get(1).getNodeValue());
    }

    @Test
    void createsDeadEndPipeNodesThroughTheEmptyNodeHook() {
        FakeTile rootTile = FakeTile.pipe();
        FakeTile deadEnd = FakeTile.pipe();
        FakeTile secondBranch = FakeTile.consumer();
        connect(rootTile, LegacyPowerDirection.UP, deadEnd);
        connect(rootTile, LegacyPowerDirection.EAST, secondBranch);

        FakeAccess access = new FakeAccess();
        LegacyPowerNodeBuilder builder = new LegacyPowerNodeBuilder(access);
        LegacyPowerGraphNode root = builder.build(rootTile, LegacyPowerDirection.DOWN);

        LegacyPowerGraphNode child = root.getNeighbor(LegacyPowerDirection.UP);
        assertNotNull(child);
        assertSame(child, deadEnd.binding.getNode());
        assertSame(root, child.getNeighbor(LegacyPowerDirection.DOWN));
        LegacyPowerPath edgePath = root.getPath(LegacyPowerDirection.UP);
        assertSame(edgePath, child.getPath(LegacyPowerDirection.DOWN));
        assertSame(edgePath.getLock(), child.getLock(LegacyPowerDirection.DOWN));
        assertSame(edgePath.getLock(), root.getLock(LegacyPowerDirection.UP));
        assertSame(child, edgePath.getNodeMapOwner());
        assertSame(child, child.getSelfPath().getNodeMapOwner());
        assertEquals(3, builder.getNodeMap().size());
    }

    @Test
    void recordsLoopClosingCableRunsAsUnroutedPaths() {
        FakeTile rootTile = FakeTile.pipe();
        FakeTile loopPipe = FakeTile.pipe();
        connect(rootTile, LegacyPowerDirection.NORTH, loopPipe);
        connect(rootTile, LegacyPowerDirection.EAST, loopPipe);

        FakeAccess access = new FakeAccess();
        LegacyPowerNodeBuilder builder = new LegacyPowerNodeBuilder(access);
        LegacyPowerGraphNode root = builder.build(rootTile, LegacyPowerDirection.DOWN);

        assertNotNull(root);
        assertEquals(2, root.getUnroutedPaths().size());
        assertTrue(root.getUnroutedPaths().stream().allMatch(
            path -> path.getNodeMapOwner() == root && path.getPipes().size() == 1));
        LegacyPowerPath currentlyBoundPath = loopPipe.binding.getNodePath();
        assertNotNull(currentlyBoundPath);
        assertTrue(root.getUnroutedPaths().contains(currentlyBoundPath));

        LegacyPowerGraphCleaner.clearNodeMap(root, -1);
        assertTrue(root.getUnroutedPaths().isEmpty());
        assertNull(loopPipe.binding.getNodePath());
    }

    private static void connect(FakeTile first, LegacyPowerDirection side, FakeTile second) {
        first.connections.put(side, true);
        first.neighbors.put(side, second);
        second.connections.put(side.opposite(), true);
        second.neighbors.put(side.opposite(), first);
    }

    private static final class FakeTile implements LegacyPowerNodeBinding {
        private final boolean pipe;
        private final boolean consumer;
        private final EnumMap<LegacyPowerDirection, Boolean> connections =
            new EnumMap<>(LegacyPowerDirection.class);
        private final EnumMap<LegacyPowerDirection, FakeTile> neighbors =
            new EnumMap<>(LegacyPowerDirection.class);
        private final FakePipeBinding binding;
        private LegacyPowerGraphNode node;
        private int occupiedSides;

        private FakeTile(boolean pipe, boolean consumer) {
            this.pipe = pipe;
            this.consumer = consumer;
            this.binding = pipe ? new FakePipeBinding() : null;
        }

        static FakeTile pipe() { return new FakeTile(true, false); }
        static FakeTile consumer() { return new FakeTile(false, true); }

        @Override public LegacyPowerGraphNode getNode() {
            return binding == null ? node : binding.getNode();
        }

        @Override public void setNode(LegacyPowerGraphNode node) {
            if (binding == null) this.node = node;
            else binding.setNode(node);
        }
    }

    private static final class FakePipeBinding implements LegacyPowerNodeBinding, LegacyPowerPipeBinding {
        private LegacyPowerGraphNode node;
        private LegacyPowerPath path;

        @Override public LegacyPowerGraphNode getNode() { return node; }
        @Override public void setNode(LegacyPowerGraphNode node) { this.node = node; }
        @Override public LegacyPowerPath getNodePath() { return path; }
        @Override public void setNodePath(LegacyPowerPath path) { this.path = path; }
    }

    private static final class FakeAccess implements LegacyPowerNodeBuildAccess {
        private int pathReloads;
        private int pipeReloads;

        int flag(LegacyPowerDirection direction) {
            return switch (direction) {
                case DOWN -> 1;
                case UP -> 2;
                case NORTH -> 4;
                case SOUTH -> 8;
                case WEST -> 16;
                case EAST -> 32;
            };
        }

        @Override public boolean isPipe(Object tile) { return ((FakeTile) tile).pipe; }
        @Override public Object getNode(Object pipe) { return ((FakeTile) pipe).getNode(); }
        @Override public Object getNodeMapOwner(Object pipe) { return ((FakeTile) pipe).getNode(); }
        @Override public boolean isConnected(Object pipe, LegacyPowerDirection side) {
            return ((FakeTile) pipe).connections.getOrDefault(side, false);
        }
        @Override public Object getNeighbor(Object pipe, LegacyPowerDirection side) {
            return ((FakeTile) pipe).neighbors.get(side);
        }
        @Override public boolean containsNode(Object node) { return false; }
        @Override public boolean containsMapOwner(Object owner) { return false; }
        @Override public void clearNodeMap(Object owner, int returnNodeValue) {
            LegacyPowerGraphCleaner.clearNodeMap((LegacyPowerGraphNode) owner, returnNodeValue);
        }
        @Override public boolean isEnergySource(Object pipe) { return false; }
        @Override public boolean isTileInvalid(Object tile) { return false; }
        @Override public int getConnectionCount(Object pipe) {
            int count = 0;
            for (boolean connected : ((FakeTile) pipe).connections.values()) if (connected) count++;
            return count;
        }
        @Override public LegacyPowerNodeBinding getPipeBinding(Object pipe) {
            return ((FakeTile) pipe).binding;
        }
        @Override public LegacyPowerGraphNode createEmptyNode(int value, LegacyPowerDirection side,
            Object tile, List<LegacyPowerGraphNode> consumers) {
            return new LegacyPowerGraphNode(value, tile, getPipeBinding(tile));
        }
        @Override public LegacyPowerGraphNode createPipeNode(int value, LegacyPowerDirection side,
            Object tile, List<LegacyPowerGraphNode> consumers) {
            return new LegacyPowerGraphNode(value, tile, getPipeBinding(tile));
        }
        @Override public boolean addConsumer(Object tile, LegacyPowerDirection side, int value,
            List<LegacyPowerGraphNode> consumers) {
            if (!((FakeTile) tile).consumer) return false;
            consumers.add(new LegacyPowerGraphNode(value, tile, null));
            return true;
        }
        @Override public LegacyPowerPath createPath(List<Object> pipeTiles) {
            List<LegacyPowerPipeBinding> bindings = new ArrayList<>();
            for (Object tile : pipeTiles) {
                bindings.add(((FakeTile) tile).binding);
            }
            return new LegacyPowerPath(bindings);
        }
        @Override public int getSideFlag(LegacyPowerDirection side) { return flag(side); }
        @Override public void setOccupiedConnectedSides(Object pipe, int flags) {
            ((FakeTile) pipe).occupiedSides = flags;
        }
        @Override public void reloadPipeLocks(Object pipe) { pipeReloads++; }
        @Override public void reloadPathLocks(LegacyPowerPath path) { pathReloads++; }
    }
}

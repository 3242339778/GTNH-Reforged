package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerTransferCoordinatorTest {

    @Test
    void rejectsClientSideAndDisconnectedTransfersBeforeMarkingSource() {
        FakeAccess access = new FakeAccess();
        access.serverSide = false;
        LegacyPowerTransferCoordinator coordinator = new LegacyPowerTransferCoordinator(access);

        assertEquals(0, coordinator.transfer(access.source, LegacyPowerDirection.EAST, 128, 4));
        assertEquals(0, access.markSourceCalls);

        access.serverSide = true;
        access.connected = false;
        assertEquals(0, coordinator.transfer(access.source, LegacyPowerDirection.EAST, 128, 4));
        assertEquals(0, access.markSourceCalls);

        // UNKNOWN bypasses the connected-side guard, as in ForgeDirection.UNKNOWN.
        assertEquals(0, coordinator.transfer(access.source, LegacyPowerDirection.UNKNOWN, 128, 4));
        assertEquals(1, access.markSourceCalls);
    }

    @Test
    void marksSourceBuildsMissingGraphAndPreservesFirstConsumerWarmup() {
        FakeAccess access = new FakeAccess();
        FakeConsumer consumer = new FakeConsumer(false);
        access.consumers = List.of(consumer);
        LegacyPowerTransferCoordinator coordinator = new LegacyPowerTransferCoordinator(access);

        assertEquals(0, coordinator.transfer(access.source, LegacyPowerDirection.EAST, 128, 4));
        assertTrue(access.energySource);
        assertEquals(1, access.buildCalls);
        assertTrue(access.node.hasHadVoltage());
        assertEquals(0, consumer.needsEnergyCalls,
            "first consumer-list construction must not call needsEnergy");

        // On subsequent calls, the same consumer is filtered out.
        assertEquals(0, coordinator.transfer(access.source, LegacyPowerDirection.EAST, 128, 4));
        assertEquals(1, consumer.needsEnergyCalls);
    }

    @Test
    void refreshesExpiredMapBeforeBuildingTheSourceGraph() {
        FakeAccess access = new FakeAccess();
        LegacyPowerGraphNode oldOwner = new LegacyPowerGraphNode(1, access.source, null);
        oldOwner.invalidateNodeMap(5);
        access.owner = oldOwner;
        access.node = oldOwner;
        access.tick = 15;
        LegacyPowerTransferCoordinator coordinator = new LegacyPowerTransferCoordinator(access);

        assertEquals(0, coordinator.transfer(access.source, LegacyPowerDirection.EAST, 128, 4));
        assertEquals(1, access.clearCalls);
        assertEquals(1, access.buildCalls);
        assertEquals(-1, access.lastClearReturnValue);
    }


    @Test
    void staleInjectionRebuildsGraphAndRetriesOnlyRemainingAmperage() {
        FakeAccess access = new FakeAccess();
        RetryConsumer consumer = new RetryConsumer(access);
        access.consumers = List.of(consumer);
        LegacyPowerTransferCoordinator coordinator = new LegacyPowerTransferCoordinator(access);

        long used = coordinator.transfer(
            access.source, LegacyPowerDirection.EAST, 128, 8);

        assertEquals(5, used, "the two passes must return the sum of their accepted amperage");
        assertEquals(2, consumer.injectionCount);
        assertEquals(List.of(8L, 6L), consumer.maxAmpsSeen,
            "the rebuilt graph must receive only the six amps not consumed by the first pass");
        assertEquals(2, access.buildCalls);
        assertEquals(1, access.clearCalls);
        assertNotNull(access.firstBuiltNode);
        assertNotNull(access.firstBuiltPath);
        assertNotNull(access.lastBuiltPath);
        assertTrue(access.firstBuiltNode.isInvalid());
        assertFalse(access.firstBuiltPath.isValid());
        assertTrue(access.lastBuiltPath.isValid());
    }

    private static final class RetryConsumer implements LegacyPowerConsumerAccess {
        private final FakeAccess access;
        private final LegacyPowerGraphNode node =
            new LegacyPowerGraphNode(2, new Object(), null);
        private final List<Long> maxAmpsSeen = new ArrayList<>();
        private int injectionCount;

        private RetryConsumer(FakeAccess access) {
            this.access = access;
        }

        @Override public LegacyPowerGraphNode getNode() { return node; }
        @Override public boolean needsEnergy() { return true; }

        @Override
        public long injectEnergy(long voltage, long maxAmps) {
            injectionCount++;
            maxAmpsSeen.add(maxAmps);
            if (injectionCount == 1) {
                // Simulate a topology mutation during the first consumer call.
                access.node.invalidateNodeMap(access.tick);
                return Math.min(2, maxAmps);
            }
            return Math.min(3, maxAmps);
        }
    }

    private static final class FakeConsumer implements LegacyPowerConsumerAccess {
        private final boolean needsEnergy;
        private final LegacyPowerGraphNode node =
            new LegacyPowerGraphNode(2, new Object(), null);
        private int needsEnergyCalls;

        private FakeConsumer(boolean needsEnergy) {
            this.needsEnergy = needsEnergy;
        }

        @Override public LegacyPowerGraphNode getNode() { return node; }
        @Override public boolean needsEnergy() {
            needsEnergyCalls++;
            return needsEnergy;
        }
        @Override public long injectEnergy(long voltage, long maxAmps) { return 0; }
    }

    private static final class FakeCable implements LegacyPowerCableAccess {
        private LegacyPowerPath nodePath;

        @Override public long getAmperageRating() { return 64; }
        @Override public long getVoltageRating() { return 512; }
        @Override public long getCableLossPerMeter() { return 0; }
        @Override public boolean hasBaseTile() { return true; }
        @Override public void setToFire() {}
        @Override public LegacyPowerPath getNodePath() { return nodePath; }
        @Override public void setNodePath(LegacyPowerPath path) { nodePath = path; }
    }

    private static final class FakeAccess implements LegacyPowerTransferAccess {
        private final Object source = new Object();
        private boolean serverSide = true;
        private boolean connected = true;
        private boolean energySource;
        private long tick = 0;
        private final AtomicInteger pathTick = new AtomicInteger();
        private LegacyPowerGraphNode node;
        private LegacyPowerGraphNode owner;
        private LegacyPowerGraphNode firstBuiltNode;
        private LegacyPowerNodePath firstBuiltPath;
        private LegacyPowerNodePath lastBuiltPath;
        private List<? extends LegacyPowerConsumerAccess> consumers = List.of();
        private int markSourceCalls;
        private int buildCalls;
        private int clearCalls;
        private int lastClearReturnValue;

        @Override public boolean isServerSide(Object sourceCable) { return serverSide; }
        @Override public boolean isConnected(Object sourceCable, LegacyPowerDirection side) { return connected; }
        @Override public void markEnergySource(Object sourceCable) {
            markSourceCalls++;
            energySource = true;
        }
        @Override public LegacyPowerGraphNode getNode(Object sourceCable) { return node; }
        @Override public LegacyPowerGraphNode getNodeMapOwner(Object sourceCable) { return owner; }
        @Override public long getServerTick(Object sourceCable) { return tick; }
        @Override public void clearNodeMap(LegacyPowerGraphNode owner, int returnNodeValue) {
            clearCalls++;
            lastClearReturnValue = returnNodeValue;
            LegacyPowerGraphCleaner.clearNodeMap(owner, returnNodeValue);
            if (node == owner) node = null;
            if (this.owner == owner) this.owner = null;
        }
        @Override public void buildNodeMap(Object sourceCable) {
            buildCalls++;
            node = new LegacyPowerGraphNode(1, sourceCable, null);
            owner = node;

            // Build a minimal but real PowerNodes route so the test exercises
            // first-list warm-up without passing a consumer with a null node.
            LegacyPowerConsumerAccess first = consumers.isEmpty() ? null : consumers.get(0);
            if (first != null) {
                LegacyPowerGraphNode consumerNode = first.getNode();
                node.setHighestNodeValue(consumerNode.getNodeValue());
                node.setNeighbor(LegacyPowerDirection.EAST, consumerNode);
                LegacyPowerNodePath edge = new LegacyPowerNodePath(
                    List.of(new FakeCable()), pathTick::get);
                node.setPath(LegacyPowerDirection.EAST, edge);
                node.setLock(LegacyPowerDirection.EAST, edge.getLock());
                lastBuiltPath = edge;
            }
            if (buildCalls == 1) {
                firstBuiltNode = node;
                firstBuiltPath = lastBuiltPath;
            }
        }
        @Override public boolean isPowerNode(LegacyPowerGraphNode candidate) { return candidate != null; }
        @Override public List<? extends LegacyPowerConsumerAccess> getConsumers(LegacyPowerGraphNode root) {
            return consumers;
        }
    }
}

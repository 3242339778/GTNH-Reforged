package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.List;

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

    private static final class FakeConsumer implements LegacyPowerConsumerAccess {
        private final boolean needsEnergy;
        private int needsEnergyCalls;

        private FakeConsumer(boolean needsEnergy) {
            this.needsEnergy = needsEnergy;
        }

        @Override public LegacyPowerGraphNode getNode() { return null; }
        @Override public boolean needsEnergy() {
            needsEnergyCalls++;
            return needsEnergy;
        }
        @Override public long injectEnergy(long voltage, long maxAmps) { return 0; }
    }

    private static final class FakeAccess implements LegacyPowerTransferAccess {
        private final Object source = new Object();
        private boolean serverSide = true;
        private boolean connected = true;
        private boolean energySource;
        private long tick = 0;
        private LegacyPowerGraphNode node;
        private LegacyPowerGraphNode owner;
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
        }
        @Override public boolean isPowerNode(LegacyPowerGraphNode candidate) { return candidate != null; }
        @Override public List<? extends LegacyPowerConsumerAccess> getConsumers(LegacyPowerGraphNode root) {
            return consumers;
        }
    }
}

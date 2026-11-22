package com.gtnh.reforged.legacy.power;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class LegacyPowerNodesTest {

    @Test
    void injectsAtAdjacentConsumerAfterSubtractingEdgeLoss() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerGraphNode root = node(0);
        LegacyPowerGraphNode consumerNode = node(1);
        root.setHighestNodeValue(1);
        root.setNeighbor(LegacyPowerDirection.EAST, consumerNode);

        LegacyPowerNodePath edge = path(tick, 10);
        LegacyPowerPathLock lock = edge.getLock();
        root.setPath(LegacyPowerDirection.EAST, edge);
        root.setLock(LegacyPowerDirection.EAST, lock);

        RecordingConsumer consumer = new RecordingConsumer(consumerNode, 3);
        LegacyPowerConsumerList consumers = new LegacyPowerConsumerList(List.of(consumer));

        long used = LegacyPowerNodes.powerNode(root, null, consumers, 100, 8);
        tick.set(2);

        assertEquals(3, used);
        assertEquals(90, consumer.lastVoltage);
        assertEquals(8, consumer.lastMaxAmps);
        assertEquals(3, edge.getAmperage());
        assertEquals(270, edge.getEnergy());
        assertNull(consumers.getNode());
    }

    @Test
    void routesThroughHigherValuedIntermediateNodeInReverseDirectionOrder() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerGraphNode root = node(0);
        LegacyPowerGraphNode middle = node(1);
        LegacyPowerGraphNode consumerNode = node(2);
        root.setHighestNodeValue(2);
        middle.setHighestNodeValue(2);
        root.setNeighbor(LegacyPowerDirection.EAST, middle);
        middle.setNeighbor(LegacyPowerDirection.EAST, consumerNode);

        LegacyPowerNodePath firstEdge = path(tick, 5);
        LegacyPowerNodePath secondEdge = path(tick, 7);
        root.setPath(LegacyPowerDirection.EAST, firstEdge);
        root.setLock(LegacyPowerDirection.EAST, firstEdge.getLock());
        middle.setPath(LegacyPowerDirection.EAST, secondEdge);
        middle.setLock(LegacyPowerDirection.EAST, secondEdge.getLock());

        RecordingConsumer consumer = new RecordingConsumer(consumerNode, 2);
        long used = LegacyPowerNodes.powerNode(root, null,
                new LegacyPowerConsumerList(List.of(consumer)), 100, 4);
        tick.set(2);

        assertEquals(2, used);
        assertEquals(88, consumer.lastVoltage);
        assertEquals(2, firstEdge.getAmperage());
        assertEquals(2, secondEdge.getAmperage());
        assertEquals(176, firstEdge.getEnergy());
        assertEquals(176, secondEdge.getEnergy());
    }

    @Test
    void lockedConsumerEdgeSkipsInjectionAndAdvancesConsumerCursor() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerGraphNode root = node(0);
        LegacyPowerGraphNode consumerNode = node(1);
        root.setHighestNodeValue(1);
        root.setNeighbor(LegacyPowerDirection.EAST, consumerNode);

        LegacyPowerNodePath edge = path(tick, 0);
        edge.getLock().addOwner(new Object());
        root.setPath(LegacyPowerDirection.EAST, edge);
        root.setLock(LegacyPowerDirection.EAST, edge.getLock());

        RecordingConsumer consumer = new RecordingConsumer(consumerNode, 5);
        LegacyPowerConsumerList consumers = new LegacyPowerConsumerList(List.of(consumer));

        assertEquals(0, LegacyPowerNodes.powerNode(root, null, consumers, 100, 5));
        assertEquals(0, consumer.injectionCount);
        assertNull(consumers.getNode());
    }

    @Test
    void topologyChangeDuringInjectionMarksConsumerListStaleButPreservesUsedAmps() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerGraphNode root = node(0);
        LegacyPowerGraphNode consumerNode = node(1);
        root.setHighestNodeValue(1);
        root.setNeighbor(LegacyPowerDirection.EAST, consumerNode);

        LegacyPowerNodePath edge = path(tick, 0);
        root.setPath(LegacyPowerDirection.EAST, edge);
        root.setLock(LegacyPowerDirection.EAST, edge.getLock());

        RecordingConsumer consumer = new RecordingConsumer(consumerNode, 4);
        consumer.onInject = () -> root.invalidateNodeMap(1);
        LegacyPowerConsumerList consumers = new LegacyPowerConsumerList(List.of(consumer));

        long used = LegacyPowerNodes.powerNode(root, null, consumers, 100, 10);

        assertEquals(4, used);
        assertTrue(consumers.isStale());
        assertEquals(1, consumer.injectionCount);
        assertEquals(4, edge.getAmperage());
    }

    @Test
    void insufficientVoltageDoesNotCallConsumerButStillProcessesPathVoltage() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerGraphNode root = node(0);
        LegacyPowerGraphNode consumerNode = node(1);
        root.setHighestNodeValue(1);
        root.setNeighbor(LegacyPowerDirection.EAST, consumerNode);

        LegacyPowerNodePath edge = path(tick, 20);
        root.setPath(LegacyPowerDirection.EAST, edge);
        root.setLock(LegacyPowerDirection.EAST, edge.getLock());

        RecordingConsumer consumer = new RecordingConsumer(consumerNode, 1);
        assertEquals(0, LegacyPowerNodes.powerNode(root, null,
                new LegacyPowerConsumerList(List.of(consumer)), 10, 2));
        assertEquals(0, consumer.injectionCount);
    }

    private static LegacyPowerGraphNode node(int value) {
        return new LegacyPowerGraphNode(value, new Object(), null);
    }

    private static LegacyPowerNodePath path(AtomicInteger tick, long loss) {
        return new LegacyPowerNodePath(List.of(new FakeCable(64, 512, loss)), tick::get);
    }

    private static final class RecordingConsumer implements LegacyPowerConsumerAccess {
        private final LegacyPowerGraphNode node;
        private final long acceptedAmps;
        private long lastVoltage;
        private long lastMaxAmps;
        private int injectionCount;
        private Runnable onInject = () -> {};

        private RecordingConsumer(LegacyPowerGraphNode node, long acceptedAmps) {
            this.node = node;
            this.acceptedAmps = acceptedAmps;
        }

        @Override public LegacyPowerGraphNode getNode() { return node; }
        @Override public boolean needsEnergy() { return true; }

        @Override
        public long injectEnergy(long voltage, long maxAmps) {
            injectionCount++;
            lastVoltage = voltage;
            lastMaxAmps = maxAmps;
            onInject.run();
            return Math.min(acceptedAmps, maxAmps);
        }
    }

    private static final class FakeCable implements LegacyPowerCableAccess {
        private final long amperage;
        private final long voltage;
        private final long loss;
        private LegacyPowerPath nodePath;
        private FakeCable(long amperage, long voltage, long loss) {
            this.amperage = amperage;
            this.voltage = voltage;
            this.loss = loss;
        }
        @Override public long getAmperageRating() { return amperage; }
        @Override public long getVoltageRating() { return voltage; }
        @Override public long getCableLossPerMeter() { return loss; }
        @Override public boolean hasBaseTile() { return true; }
        @Override public void setToFire() {}
        @Override public LegacyPowerPath getNodePath() { return nodePath; }
        @Override public void setNodePath(LegacyPowerPath path) { nodePath = path; }
    }
}

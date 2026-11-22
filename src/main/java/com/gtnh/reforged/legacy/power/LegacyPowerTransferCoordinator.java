package com.gtnh.reforged.legacy.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure-Java extraction of GT5-Unofficial 5.09.54.275
 * MTECable.transferElectricity and its private getConsumers lifecycle.
 */
public final class LegacyPowerTransferCoordinator {

    private final LegacyPowerTransferAccess access;

    public LegacyPowerTransferCoordinator(LegacyPowerTransferAccess access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    public long transfer(Object sourceCable, LegacyPowerDirection side, long voltage, long amperage) {
        Objects.requireNonNull(sourceCable, "sourceCable");
        Objects.requireNonNull(side, "side");

        if (amperage <= 0 || !access.isServerSide(sourceCable)
            || (!access.isConnected(sourceCable, side) && side != LegacyPowerDirection.UNKNOWN)) {
            return 0;
        }

        // Sticky for the lifetime of this cable instance, matching MTECable.energySource.
        access.markEnergySource(sourceCable);

        LegacyPowerGraphNode mapOwner = access.getNodeMapOwner(sourceCable);
        if (mapOwner != null
            && mapOwner.getGraphState().isNodeMapRefreshDue(access.getServerTick(sourceCable))) {
            access.clearNodeMap(mapOwner, -1);
        }

        if (access.getNode(sourceCable) == null) {
            access.buildNodeMap(sourceCable);
        }

        LegacyPowerGraphNode sourceNode = access.getNode(sourceCable);
        if (!access.isPowerNode(sourceNode)) {
            return 0;
        }

        LegacyPowerConsumerList consumers = getConsumers(sourceNode);
        if (consumers == null) {
            return 0;
        }

        // Upstream explicitly narrows both long arguments to signed 32-bit ints.
        long usedAmperage = LegacyPowerNodes.powerNode(
            sourceNode, null, consumers, (int) voltage, (int) amperage);

        if (!consumers.isStale() || sourceNode.isInvalid()) {
            return usedAmperage;
        }

        access.clearNodeMap(sourceNode, -1);
        access.buildNodeMap(sourceCable);

        LegacyPowerGraphNode rebuiltNode = access.getNode(sourceCable);
        if (usedAmperage >= amperage || !access.isPowerNode(rebuiltNode)) {
            return usedAmperage;
        }

        LegacyPowerConsumerList rebuiltConsumers = getConsumers(rebuiltNode);
        if (rebuiltConsumers == null) {
            return usedAmperage;
        }

        long remainingAmperage = amperage - usedAmperage;
        long retryUsed = LegacyPowerNodes.powerNode(
            rebuiltNode, null, rebuiltConsumers, (int) voltage, (int) remainingAmperage);
        return usedAmperage + retryUsed;
    }

    /**
     * Mirrors MTECable.getConsumers exactly: first invocation includes every
     * consumer and flips hadVoltage; later invocations filter via needsEnergy.
     */
    private LegacyPowerConsumerList getConsumers(LegacyPowerGraphNode node) {
        List<? extends LegacyPowerConsumerAccess> all = access.getConsumers(node);
        if (all == null) {
            throw new IllegalStateException("getConsumers returned null instead of a consumer list");
        }

        if (!node.hasHadVoltage()) {
            node.setHadVoltage(true);
            return new LegacyPowerConsumerList(all);
        }

        List<LegacyPowerConsumerAccess> eligible = new ArrayList<>(all.size());
        for (LegacyPowerConsumerAccess consumer : all) {
            if (consumer.needsEnergy()) {
                eligible.add(consumer);
            }
        }
        return eligible.isEmpty() ? null : new LegacyPowerConsumerList(eligible);
    }
}

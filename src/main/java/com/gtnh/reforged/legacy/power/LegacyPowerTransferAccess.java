package com.gtnh.reforged.legacy.power;

import java.util.List;

/**
 * Host boundary for the outer MTECable.transferElectricity lifecycle.
 *
 * <p>All object handles must preserve identity. Implementations map the source
 * cable to the same graph node/path objects used by the builder and provide
 * consumer adapters in the exact order recorded by graph construction.</p>
 */
public interface LegacyPowerTransferAccess {

    boolean isServerSide(Object sourceCable);

    boolean isConnected(Object sourceCable, LegacyPowerDirection side);

    void markEnergySource(Object sourceCable);

    LegacyPowerGraphNode getNode(Object sourceCable);

    LegacyPowerGraphNode getNodeMapOwner(Object sourceCable);

    long getServerTick(Object sourceCable);

    void clearNodeMap(LegacyPowerGraphNode owner, int returnNodeValue);

    void buildNodeMap(Object sourceCable);

    /** True only for the power-node subtype accepted by upstream MTECable. */
    boolean isPowerNode(LegacyPowerGraphNode node);

    /** All consumer adapters, preserving the upstream node consumer order. */
    List<? extends LegacyPowerConsumerAccess> getConsumers(LegacyPowerGraphNode root);
}

package com.gtnh.reforged.legacy.power;

/**
 * Host-side behavior for a consumer node in the legacy power graph.
 * The consumer list is expected to contain only consumers selected by the
 * upstream-equivalent discovery stage; PowerNodes itself does not call
 * needsEnergy(), matching GTNH 5.09.54.275.
 */
public interface LegacyPowerConsumerAccess {
    LegacyPowerGraphNode getNode();
    boolean needsEnergy();
    long injectEnergy(long voltage, long maxAmps);
}

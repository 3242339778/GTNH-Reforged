package com.gtnh.reforged.legacy.power;

/**
 * Result of applying BaseMetaPipeEntity.updateConnections() to a cable state.
 *
 * @param changed whether the stored connection mask changed
 * @param oldMask mask before the update
 * @param newMask mask after the update
 * @param addedConnections bits newly connected
 * @param removedConnections bits disconnected
 */
public record LegacyPowerConnectionMaskChange(
    boolean changed,
    int oldMask,
    int newMask,
    int addedConnections,
    int removedConnections) {

    public LegacyPowerConnectionMaskChange {
        if (!changed && (oldMask != newMask || addedConnections != 0 || removedConnections != 0)) {
            throw new IllegalArgumentException("An unchanged mask update cannot contain a delta");
        }
        if (changed && oldMask == newMask) {
            throw new IllegalArgumentException("A changed mask update must change the mask");
        }
        if (addedConnections != (newMask & ~oldMask)
            || removedConnections != (oldMask & ~newMask)) {
            throw new IllegalArgumentException("Connection deltas must match the old/new masks");
        }
    }

    public static LegacyPowerConnectionMaskChange unchanged(int mask) {
        return new LegacyPowerConnectionMaskChange(false, mask, mask, 0, 0);
    }
}

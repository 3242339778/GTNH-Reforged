package com.gtnh.reforged.legacy.power;

/**
 * Preserves MetaPipeEntity.connect(side)'s legacy tri-state result.
 *
 * <p>A result of {@code -1} means the target chunk is not loaded. It is not a
 * failed connection: checkConnections() must retain the previous connection
 * bit until the neighbor can be inspected.</p>
 */
public enum LegacyPowerConnectionResult {
    CONNECTED,
    NOT_CONNECTABLE,
    UNLOADED;

    public static LegacyPowerConnectionResult fromLegacyConnectResult(int result) {
        if (result > 0) {
            return CONNECTED;
        }
        if (result == 0) {
            return NOT_CONNECTABLE;
        }
        if (result == -1) {
            return UNLOADED;
        }
        throw new IllegalArgumentException("Unexpected legacy connect result: " + result);
    }

    /**
     * Applies the legacy checkConnections() rule to one direction flag.
     * Unloaded neighbors preserve the existing bit.
     */
    public int updateConnectionMask(int currentMask, int sideFlag) {
        if (sideFlag == 0) {
            throw new IllegalArgumentException("A physical side flag must be non-zero");
        }
        return switch (this) {
            case CONNECTED -> currentMask | sideFlag;
            case NOT_CONNECTABLE -> currentMask & ~sideFlag;
            case UNLOADED -> currentMask;
        };
    }
}

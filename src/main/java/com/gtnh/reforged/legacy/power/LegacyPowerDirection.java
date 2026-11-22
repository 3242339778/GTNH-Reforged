package com.gtnh.reforged.legacy.power;

/**
 * The six legacy ForgeDirection values in their ordinal order, plus the
 * non-indexed UNKNOWN sentinel used by MTECable.transferElectricity.
 *
 * <p>Do not reorder the six cardinal values: the upstream graph stores
 * neighbors and paths in direction-ordinal indexed arrays. UNKNOWN is
 * deliberately last and is never part of the six-direction iteration.</p>
 */
public enum LegacyPowerDirection {
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST,
    UNKNOWN;

    private static final LegacyPowerDirection[] CARDINAL_DIRECTIONS = {
        DOWN, UP, NORTH, SOUTH, WEST, EAST
    };

    /**
     * Returns the legacy direction-array index, or -1 for UNKNOWN.
     */
    public int ordinalIndex() {
        return this == UNKNOWN ? -1 : ordinal();
    }

    public LegacyPowerDirection opposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
            case UNKNOWN -> UNKNOWN;
        };
    }

    public static LegacyPowerDirection fromOrdinalIndex(int ordinalIndex) {
        return CARDINAL_DIRECTIONS[ordinalIndex];
    }

    /**
     * Returns only the six real directions, in the original Forge order.
     */
    public static LegacyPowerDirection[] orderedValues() {
        return CARDINAL_DIRECTIONS.clone();
    }
}

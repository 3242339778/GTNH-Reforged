package com.gtnh.reforged.legacy.power;

/**
 * The six legacy ForgeDirection values in their ordinal order.
 *
 * <p>Do not reorder: the upstream graph stores neighbors and paths in
 * direction-ordinal indexed arrays.</p>
 */
public enum LegacyPowerDirection {
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST;

    private static final LegacyPowerDirection[] VALUES = values();

    public int ordinalIndex() {
        return ordinal();
    }

    public LegacyPowerDirection opposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    public static LegacyPowerDirection fromOrdinalIndex(int ordinalIndex) {
        return VALUES[ordinalIndex];
    }

    public static LegacyPowerDirection[] orderedValues() {
        return VALUES.clone();
    }
}

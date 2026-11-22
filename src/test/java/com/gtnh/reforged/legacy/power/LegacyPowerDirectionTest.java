package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerDirectionTest {

    @Test
    void unknownIsNotPartOfTheSixDirectionArray() {
        assertEquals(-1, LegacyPowerDirection.UNKNOWN.ordinalIndex());
        assertArrayEquals(
            new LegacyPowerDirection[] {
                LegacyPowerDirection.DOWN,
                LegacyPowerDirection.UP,
                LegacyPowerDirection.NORTH,
                LegacyPowerDirection.SOUTH,
                LegacyPowerDirection.WEST,
                LegacyPowerDirection.EAST
            },
            LegacyPowerDirection.orderedValues()
        );
    }

    @Test
    void ordinalMappingOfTheSixDirectionsRemainsUnchanged() {
        for (int index = 0; index < 6; index++) {
            assertEquals(index, LegacyPowerDirection.fromOrdinalIndex(index).ordinalIndex());
        }
    }

    @Test
    void unknownOpposesItselfAsANonDirectionalSentinel() {
        assertSame(LegacyPowerDirection.UNKNOWN, LegacyPowerDirection.UNKNOWN.opposite());
    }
}

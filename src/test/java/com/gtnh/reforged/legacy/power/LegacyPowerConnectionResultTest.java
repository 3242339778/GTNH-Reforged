package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerConnectionResultTest {

    @Test
    void positiveLegacyResultMeansConnectedAndSetsSideBit() {
        LegacyPowerConnectionResult result = LegacyPowerConnectionResult.fromLegacyConnectResult(1);

        assertEquals(LegacyPowerConnectionResult.CONNECTED, result);
        assertEquals(0b1010, result.updateConnectionMask(0b0010, 0b1000));
    }

    @Test
    void zeroLegacyResultMeansNotConnectableAndClearsSideBit() {
        LegacyPowerConnectionResult result = LegacyPowerConnectionResult.fromLegacyConnectResult(0);

        assertEquals(LegacyPowerConnectionResult.NOT_CONNECTABLE, result);
        assertEquals(0b0010, result.updateConnectionMask(0b1010, 0b1000));
    }

    @Test
    void negativeOneMeansUnloadedAndPreservesExistingSideBit() {
        LegacyPowerConnectionResult result = LegacyPowerConnectionResult.fromLegacyConnectResult(-1);

        assertEquals(LegacyPowerConnectionResult.UNLOADED, result);
        assertEquals(0b1010, result.updateConnectionMask(0b1010, 0b1000));
        assertEquals(0b0010, result.updateConnectionMask(0b0010, 0b1000));
    }

    @Test
    void unexpectedNegativeResultIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> LegacyPowerConnectionResult.fromLegacyConnectResult(-2));
    }

    @Test
    void zeroSideFlagIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> LegacyPowerConnectionResult.CONNECTED.updateConnectionMask(0b0010, 0));
    }
}

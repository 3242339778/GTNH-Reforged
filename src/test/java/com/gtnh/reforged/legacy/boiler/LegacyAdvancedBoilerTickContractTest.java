package com.gtnh.reforged.legacy.boiler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyAdvancedBoilerTickContractTest {
    @Test void snapshotPreservesInputValues() {
        var snapshot = new LegacyAdvancedBoilerTickSnapshot(
            40, 120, 150, 3, 40, 12, 20, 1000, 500,
            true, true, true, 32000, 64000, -12, 500, 750, 10, 2, 1);
        assertEquals(40, snapshot.tick());
        assertEquals(120, snapshot.temperature());
        assertEquals(1000, snapshot.waterAmount());
        assertEquals(-12, snapshot.excessWaterRemainder());
        assertTrue(snapshot.waterIsValid());
        assertTrue(snapshot.machineAllowedToWork());
    }

    @Test void snapshotAllowsNegativeWaterButRejectsAmountBeyondCapacity() {
        var negativeWater = new LegacyAdvancedBoilerTickSnapshot(
            0, 20, 100, 0, 40, 0, 0, -1, 0, true, false, true, 100, 100, 0, 0, 750, 10, 2, 1);
        assertEquals(-1, negativeWater.waterAmount());
        assertThrows(IllegalArgumentException.class, () -> new LegacyAdvancedBoilerTickSnapshot(
            0, 20, 100, 0, 40, 0, 0, 101, 0, true, false, true, 100, 100, 0, 0, 750, 10, 2, 1));
    }

    @Test void snapshotRejectsInvalidHeatUpRate() {
        assertThrows(IllegalArgumentException.class, () -> new LegacyAdvancedBoilerTickSnapshot(
            0, 20, 100, 0, 40, 0, 0, 0, 0, true, false, true, 100, 100, 0, 0, 750, 0, 2, 1));
    }

    @Test void resultPreservesCalculatedValues() {
        var result = new LegacyAdvancedBoilerTickResult(
            121, 4, 10, 20, 990, 875, -5, 375, 2, 500,
            true, true, true, false);
        assertEquals(121, result.temperature());
        assertEquals(875, result.steamAmount());
        assertEquals(375, result.steamProducedThisTick());
        assertEquals(2, result.waterConsumedThisTick());
        assertEquals(500, result.burnTimeConsumed());
        assertTrue(result.fuelInjectionRequested());
        assertTrue(result.active());
        assertFalse(result.vented());
    }

    @Test void resultRejectsNegativeCounters() {
        assertThrows(IllegalArgumentException.class, () -> new LegacyAdvancedBoilerTickResult(
            20, -1, 0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> new LegacyAdvancedBoilerTickResult(
            20, 0, 0, 0, 0, 0, 0, -1, 0, 0, false, false, false, false));
    }
}

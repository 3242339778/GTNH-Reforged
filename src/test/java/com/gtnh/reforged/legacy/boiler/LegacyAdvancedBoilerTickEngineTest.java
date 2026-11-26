package com.gtnh.reforged.legacy.boiler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyAdvancedBoilerTickEngineTest {
    private LegacyAdvancedBoilerTickSnapshot snapshot(
        long tick, int temperature, int energy, int water, int steam, int remainder, int burnTime
    ) {
        return new LegacyAdvancedBoilerTickSnapshot(tick, temperature, 500, 0, 40,
            energy, energy, water, steam, true, true, true,
            32000, 64000, remainder, burnTime, 750, 10, 2, 1);
    }

    @Test void productionAndWaterConversionAreAppliedOnCadence() {
        var result = LegacyAdvancedBoilerTickEngine.calculate(snapshot(30, 110, 10, 100, 0, 0, 0), false);
        assertEquals(375, result.steamProducedThisTick());
        assertEquals(2, result.waterConsumedThisTick());
        assertEquals(98, result.waterAmount());
        assertEquals(375, result.steamAmount());
        assertEquals(-55, result.excessWaterRemainder());
        assertTrue(result.boiling());
    }

    @Test void productionIsSkippedOffCadence() {
        var result = LegacyAdvancedBoilerTickEngine.calculate(snapshot(31, 110, 10, 100, 0, 0, 0), false);
        assertEquals(0, result.steamProducedThisTick());
        assertEquals(100, result.waterAmount());
        assertEquals(0, result.steamAmount());
    }

    @Test void cooldownUsesGreaterThanInterval() {
        var input = new LegacyAdvancedBoilerTickSnapshot(21, 120, 500, 40, 40,
            0, 0, 0, 0, true, true, true, 32000, 64000, 0, 0, 750, 10, 2, 1);
        var result = LegacyAdvancedBoilerTickEngine.calculate(input, false);
        assertEquals(119, result.temperature());
        assertEquals(0, result.lossTimer());
    }

    @Test void ticksThroughTwentyAreSkippedEntirely() {
        var input = snapshot(20, 120, 10, 100, 0, 5, 500);
        var result = LegacyAdvancedBoilerTickEngine.calculate(input, true);
        assertEquals(120, result.temperature());
        assertEquals(0, result.lossTimer());
        assertEquals(10, result.processingEnergy());
        assertEquals(100, result.waterAmount());
        assertEquals(0, result.steamAmount());
        assertEquals(5, result.excessWaterRemainder());
        assertFalse(result.fuelInjectionRequested());
        assertFalse(result.vented());
        assertTrue(result.boiling());
    }

    @Test void fuelInjectionAndHeatUpFollowTickOrdering() {
        var result = LegacyAdvancedBoilerTickEngine.calculate(snapshot(30, 100, 0, 100, 0, 0, 500), false);
        assertTrue(result.fuelInjectionRequested());
        assertEquals(500, result.burnTimeConsumed());
        assertEquals(48, result.processingEnergy());
        assertEquals(102, result.temperature());
        assertTrue(result.active());
    }

    @Test void overflowUsesSteamCapacityAndVentsToThreeQuarters() {
        var input = new LegacyAdvancedBoilerTickSnapshot(21, 100, 500, 0, 40,
            0, 0, 0, 64001, true, true, true, 32000, 64000, 0, 0, 750, 10, 2, 1);
        var result = LegacyAdvancedBoilerTickEngine.calculate(input, false);
        assertTrue(result.vented());
        assertEquals(48000, result.steamAmount());
    }

    @Test void boilingStatePersistsAboveThresholdWithoutWaterButClearsAtLowTemperature() {
        var noWater = new LegacyAdvancedBoilerTickSnapshot(30, 110, 500, 0, 40,
            0, 0, 0, 0, false, true, true, 32000, 64000, 0, 0, 750, 10, 2, 1);
        assertTrue(LegacyAdvancedBoilerTickEngine.calculate(noWater, true).boiling());

        var cold = new LegacyAdvancedBoilerTickSnapshot(30, 100, 500, 0, 40,
            0, 0, 0, 0, false, true, true, 32000, 64000, 0, 0, 750, 10, 2, 1);
        assertFalse(LegacyAdvancedBoilerTickEngine.calculate(cold, true).boiling());
    }

    @Test void boilingFlagCanRemainTrueWhenProductionConsumesLastWater() {
        var input = snapshot(30, 110, 10, 2, 0, 0, 0);
        var result = LegacyAdvancedBoilerTickEngine.calculate(input, false);
        assertTrue(result.boiling());
        assertEquals(0, result.waterAmount());
    }

    @Test void insufficientWaterMatchesSourceAndProducesNegativeAmount() {
        var result = LegacyAdvancedBoilerTickEngine.calculate(snapshot(30, 110, 10, 1, 0, 0, 0), false);
        assertEquals(375, result.steamProducedThisTick());
        assertEquals(2, result.waterConsumedThisTick());
        assertEquals(-1, result.waterAmount());
        assertEquals(-55, result.excessWaterRemainder());
    }

    @Test void negativeWaterAmountBlocksNextProductionGate() {
        var input = new LegacyAdvancedBoilerTickSnapshot(40, 110, 500, 0, 40,
            10, 10, -1, 375, true, true, true, 32000, 64000, -55, 0, 750, 10, 2, 1);
        var result = LegacyAdvancedBoilerTickEngine.calculate(input, false);
        assertEquals(0, result.steamProducedThisTick());
        assertEquals(-1, result.waterAmount());
        assertEquals(375, result.steamAmount());
    }
}

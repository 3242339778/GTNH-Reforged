package com.gtnh.reforged.legacy.boiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyBoilerFluidStateTest {
    @Test
    void aggregateKeepsWaterAndSteamInventoriesSeparate() {
        var water = new LegacyBoilerWaterShadowState("minecraft:water", 100, -55);
        var state = new LegacyBoilerFluidState(water, "gtnh:steam", 1200);

        assertSame(water, state.water());
        assertEquals(100, state.water().legacyAmount());
        assertEquals("minecraft:water", state.water().legacyFluidId());
        assertEquals(-55, state.water().excessWaterRemainder());
        assertEquals(1200, state.steamAmount());
        assertEquals("gtnh:steam", state.steamFluidId());
    }

    @Test
    void waterAndSteamApiProjectionsRemainNonNegativeAndIndependent() {
        var water = new LegacyBoilerWaterShadowState("minecraft:water", -1, -55);
        var state = new LegacyBoilerFluidState(water, "gtnh:steam", 12);

        assertEquals(0, state.water().exposedAmount());
        assertNull(state.water().exposedFluidId());
        assertEquals(12, state.exposedSteamAmount());
        assertEquals("gtnh:steam", state.exposedSteamFluidId());
        assertEquals(-1, state.water().legacyAmount());
    }

    @Test
    void steamExtractionSimulationDoesNotMutateAggregate() {
        var state = new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState(null, 0, 0), "gtnh:steam", 100);

        assertEquals(40, state.extractSteam("gtnh:steam", 40, false));
        assertEquals(100, state.steamAmount());
        assertEquals(40, state.extractSteam("gtnh:steam", 40, true));
        assertEquals(60, state.steamAmount());
    }

    @Test
    void steamExtractionRejectsWrongIdentityAndNonPositiveRequests() {
        var state = new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState(null, 0, 0), "gtnh:steam", 100);

        assertEquals(0, state.extractSteam("minecraft:water", 20, true));
        assertEquals(0, state.extractSteam("gtnh:steam", 0, true));
        assertEquals(0, state.extractSteam("gtnh:steam", -1, true));
        assertEquals(100, state.steamAmount());
    }

    @Test
    void steamExtractionCannotProduceNegativeAmount() {
        var state = new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState(null, 0, 0), "gtnh:steam", 7);

        assertEquals(7, state.extractSteam("gtnh:steam", 100, true));
        assertEquals(0, state.steamAmount());
        assertNull(state.exposedSteamFluidId());
    }

    @Test
    void aggregateSnapshotRestoresBothTanksAndWaterRemainderTogether() {
        var water = new LegacyBoilerWaterShadowState("minecraft:water", 1, 0);
        var state = new LegacyBoilerFluidState(water, "gtnh:steam", 500);
        var before = state.snapshot();

        water.consumeWaterForSteam(375);
        state.extractSteam("gtnh:steam", 125, true);
        state.setSteam("gtnh:superheated_steam", 300);

        state.restore(before);

        assertEquals("minecraft:water", state.water().legacyFluidId());
        assertEquals(1, state.water().legacyAmount());
        assertEquals(0, state.water().excessWaterRemainder());
        assertEquals("gtnh:steam", state.steamFluidId());
        assertEquals(500, state.steamAmount());
        assertEquals("minecraft:water", before.waterFluidId());
        assertEquals(1, before.waterAmount());
        assertEquals(0, before.excessWaterRemainder());
    }

    @Test
    void snapshotIsImmutableAfterSubsequentMutations() {
        var state = new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState("minecraft:water", 10, 3), "gtnh:steam", 50);
        var snapshot = state.snapshot();

        state.water().fill("minecraft:water", 5, 32000, true);
        state.setSteam("gtnh:steam", 25);

        assertEquals(10, snapshot.waterAmount());
        assertEquals(3, snapshot.excessWaterRemainder());
        assertEquals(50, snapshot.steamAmount());
    }

    @Test
    void positiveSteamAmountRequiresFluidIdentity() {
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyBoilerFluidState(
                new LegacyBoilerWaterShadowState(null, 0, 0), null, 1));
    }

    @Test
    void negativeSteamAmountIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyBoilerFluidState(
                new LegacyBoilerWaterShadowState(null, 0, 0), "gtnh:steam", -1));
    }
}

package com.gtnh.reforged.legacy.boiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyBoilerWaterShadowStateTest {
    @Test
    void signedWaterAndRemainderArePreservedButProjectionIsNonNegative() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", 1, 0);
        assertEquals(2, state.consumeWaterForSteam(375));
        assertEquals(-1, state.legacyAmount());
        assertEquals(-55, state.excessWaterRemainder());
        assertEquals(0, state.exposedAmount());
        assertNull(state.exposedFluidId());
        assertEquals("minecraft:water", state.legacyFluidId());
    }

    @Test
    void negativeAmountBlocksPositiveWaterProjectionWithoutDestroyingShadowState() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", 1, 0);
        state.consumeWaterForSteam(375);
        assertEquals(0, state.exposedAmount());
        assertEquals(-1, state.legacyAmount());
        assertEquals(-55, state.excessWaterRemainder());
    }

    @Test
    void preTickCleanupOnlyRunsWhenFluidChangingIsAllowed() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", -1, -55);
        assertFalse(state.preTickCleanup(false));
        assertEquals(-1, state.legacyAmount());
        assertEquals("minecraft:water", state.legacyFluidId());

        assertTrue(state.preTickCleanup(true));
        assertEquals(0, state.legacyAmount());
        assertNull(state.legacyFluidId());
        assertEquals(-55, state.excessWaterRemainder());
    }

    @Test
    void zeroAmountStackIsAlsoClearedButRemainderIsRetained() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", 0, 17);
        assertTrue(state.preTickCleanup(true));
        assertNull(state.legacyFluidId());
        assertEquals(0, state.legacyAmount());
        assertEquals(17, state.excessWaterRemainder());
    }

    @Test
    void negativeWindowFillUsesSignedAmount() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", -1, -55);
        assertEquals(1, state.fill("minecraft:water", 1, 32000, false));
        assertEquals(-1, state.legacyAmount());

        assertEquals(1, state.fill("minecraft:water", 1, 32000, true));
        assertEquals(0, state.legacyAmount());
        assertEquals(0, state.exposedAmount());
        assertEquals("minecraft:water", state.legacyFluidId());
    }

    @Test
    void negativeWindowCanTemporarilyAcceptCapacityPlusOne() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", -1, -55);
        assertEquals(32001, state.fill("minecraft:water", 32001, 32000, false));
        assertEquals(-1, state.legacyAmount());

        assertEquals(32001, state.fill("minecraft:water", 32001, 32000, true));
        assertEquals(32000, state.legacyAmount());
        assertEquals(32000, state.exposedAmount());
    }

    @Test
    void fillRejectsDifferentFluidAndNonPositiveInput() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", 100, 0);
        assertEquals(0, state.fill("example:other", 10, 32000, true));
        assertEquals(0, state.fill("minecraft:water", 0, 32000, true));
        assertEquals(100, state.legacyAmount());
    }

    @Test
    void emptyTankFillMatchesLegacyCapacityBehavior() {
        var state = new LegacyBoilerWaterShadowState(null, 0, 0);
        assertEquals(32000, state.fill("minecraft:water", 32001, 32000, false));
        assertNull(state.legacyFluidId());

        assertEquals(32000, state.fill("minecraft:water", 32001, 32000, true));
        assertEquals("minecraft:water", state.legacyFluidId());
        assertEquals(32000, state.legacyAmount());
    }

    @Test
    void snapshotRestoreRollsBackIdentityAmountAndRemainderTogether() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", 1, 0);
        var before = state.snapshot();
        state.consumeWaterForSteam(375);
        state.fill("minecraft:water", 1, 32000, true);
        state.preTickCleanup(true);

        state.restore(before);
        assertEquals("minecraft:water", state.legacyFluidId());
        assertEquals(1, state.legacyAmount());
        assertEquals(0, state.excessWaterRemainder());
        assertEquals(1, state.exposedAmount());
    }

    @Test
    void committedMutationRemainsApplied() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", -1, -55);
        var before = state.snapshot();
        state.fill("minecraft:water", 1, 32000, true);

        assertEquals(0, state.legacyAmount());
        assertEquals(-55, state.excessWaterRemainder());
        assertEquals("minecraft:water", state.legacyFluidId());
        assertEquals(-1, before.amount());
    }

    @Test
    void positiveAmountRequiresFluidIdentity() {
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyBoilerWaterShadowState(null, 1, 0));
    }

    @Test
    void negativeSteamAmountIsRejected() {
        var state = new LegacyBoilerWaterShadowState("minecraft:water", 1, 0);
        assertThrows(IllegalArgumentException.class, () -> state.consumeWaterForSteam(-1));
    }
}

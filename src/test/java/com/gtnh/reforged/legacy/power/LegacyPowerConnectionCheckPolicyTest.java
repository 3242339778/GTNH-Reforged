package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerConnectionCheckPolicyTest {

    @Test
    void clientRequestsAreIgnored() {
        assertEquals(
            LegacyPowerConnectionCheckPolicy.Dispatch.CLIENT_IGNORED,
            LegacyPowerConnectionCheckPolicy.request(false, false, false, true));
    }

    @Test
    void builtinNonTickingCableUsesManagedQueueBeforeOtherBranches() {
        assertEquals(
            LegacyPowerConnectionCheckPolicy.Dispatch.MANAGED_CABLE_QUEUE,
            LegacyPowerConnectionCheckPolicy.request(true, true, true, false));
    }

    @Test
    void disabledTickingOrExplicitNoDeferRunsImmediately() {
        assertEquals(
            LegacyPowerConnectionCheckPolicy.Dispatch.IMMEDIATE,
            LegacyPowerConnectionCheckPolicy.request(true, false, true, true));
        assertEquals(
            LegacyPowerConnectionCheckPolicy.Dispatch.IMMEDIATE,
            LegacyPowerConnectionCheckPolicy.request(true, false, false, false));
    }

    @Test
    void ordinaryTickingPipeDefersCheckToItsMetaTileTick() {
        assertEquals(
            LegacyPowerConnectionCheckPolicy.Dispatch.DEFERRED_TO_META_TILE_TICK,
            LegacyPowerConnectionCheckPolicy.request(true, false, false, true));
    }

    @Test
    void addonCableTickChecksOnlyOnServerEveryTwentyTicksWhenPendingOrTraditional() {
        assertFalse(LegacyPowerConnectionCheckPolicy.shouldRunMetaTileTick(false, 20, true, false));
        assertFalse(LegacyPowerConnectionCheckPolicy.shouldRunMetaTileTick(true, 19, true, false));
        assertTrue(LegacyPowerConnectionCheckPolicy.shouldRunMetaTileTick(true, 20, true, true));
        assertFalse(LegacyPowerConnectionCheckPolicy.shouldRunMetaTileTick(true, 20, false, true));
        assertTrue(LegacyPowerConnectionCheckPolicy.shouldRunMetaTileTick(true, 20, false, false));
    }

    @Test
    void managedBuiltinCableRunsPendingCheckOnNextManagerPass() {
        assertTrue(LegacyPowerConnectionCheckPolicy.shouldRunManagedCableTick(true, true, 7));
        assertFalse(LegacyPowerConnectionCheckPolicy.shouldRunManagedCableTick(false, true, 20));
        assertTrue(LegacyPowerConnectionCheckPolicy.shouldRunManagedCableTick(false, false, 20));
        assertFalse(LegacyPowerConnectionCheckPolicy.shouldRunManagedCableTick(false, false, 19));
    }

    @Test
    void traditionalModeAttemptsEverySideIncludingPreviouslyOpenSides() {
        assertTrue(LegacyPowerConnectionCheckPolicy.shouldAttemptDirection(false, 0, 0b000001));
        assertTrue(LegacyPowerConnectionCheckPolicy.shouldAttemptDirection(false, 0, 0b100000));
    }

    @Test
    void gt6StyleOnlyRevalidatesExistingConnectionBits() {
        assertTrue(LegacyPowerConnectionCheckPolicy.shouldAttemptDirection(true, 0b001000, 0b001000));
        assertFalse(LegacyPowerConnectionCheckPolicy.shouldAttemptDirection(true, 0b001000, 0b000001));
    }

    @Test
    void unattemptedGt6SideAndUnloadedTargetPreserveExistingMask() {
        assertEquals(0b001000,
            LegacyPowerConnectionCheckPolicy.applyDirectionResult(
                true, 0b001000, 0b000001, LegacyPowerConnectionResult.NOT_CONNECTABLE));
        assertEquals(0b001000,
            LegacyPowerConnectionCheckPolicy.applyDirectionResult(
                false, 0b001000, 0b001000, LegacyPowerConnectionResult.UNLOADED));
    }

    @Test
    void successfulConnectionSetsBitAndFailedAttemptClearsBit() {
        assertEquals(0b001001,
            LegacyPowerConnectionCheckPolicy.applyDirectionResult(
                false, 0b001000, 0b000001, LegacyPowerConnectionResult.CONNECTED));
        assertEquals(0b000000,
            LegacyPowerConnectionCheckPolicy.applyDirectionResult(
                false, 0b001000, 0b001000, LegacyPowerConnectionResult.NOT_CONNECTABLE));
    }
}

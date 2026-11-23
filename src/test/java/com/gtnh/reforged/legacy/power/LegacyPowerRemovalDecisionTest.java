package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyPowerRemovalDecisionTest {

    @Test
    void chunkUnloadTakesPrecedenceOverWorldState() {
        assertDecision(
            true,
            false,
            false,
            false,
            LegacyPowerRemovalDecision.Result.CHUNK_UNLOAD);
    }

    @Test
    void unavailableWorldStateIsConservativeUnknown() {
        assertDecision(
            false,
            false,
            false,
            false,
            LegacyPowerRemovalDecision.Result.UNKNOWN);
    }

    @Test
    void differentCurrentBlockMeansBlockWasReplaced() {
        assertDecision(
            false,
            true,
            false,
            false,
            LegacyPowerRemovalDecision.Result.BLOCK_REPLACED);
    }

    @Test
    void missingOrDifferentHostWithCableBlockStillPresentMeansHostWasReplaced() {
        assertDecision(
            false,
            true,
            true,
            false,
            LegacyPowerRemovalDecision.Result.HOST_REPLACED);
    }

    @Test
    void stillRegisteredHostDoesNotProveRemovalReason() {
        assertDecision(
            false,
            true,
            true,
            true,
            LegacyPowerRemovalDecision.Result.UNKNOWN);
    }

    private static void assertDecision(
        boolean chunkUnloadPending,
        boolean worldStateAvailable,
        boolean currentBlockIsCable,
        boolean currentBlockEntityIsThis,
        LegacyPowerRemovalDecision.Result expected) {
        assertEquals(
            expected,
            LegacyPowerRemovalDecision.classify(
                chunkUnloadPending,
                worldStateAvailable,
                currentBlockIsCable,
                currentBlockEntityIsThis));
    }
}

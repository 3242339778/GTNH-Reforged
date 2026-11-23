package com.gtnh.reforged.legacy.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPowerConnectionNegotiationTest {

    private static LegacyPowerConnectionNegotiation.Decision evaluate(
        boolean color,
        boolean cover,
        LegacyPowerConnectionNegotiation.NeighborKind neighbor,
        boolean opposite,
        boolean gt6,
        boolean air,
        boolean endpoint,
        boolean loaded) {
        return LegacyPowerConnectionNegotiation.evaluate(
            color, cover, neighbor, opposite, gt6, air, endpoint, loaded);
    }

    @Test
    void colorMismatchRejectsBeforeCoverAndChunkHandling() {
        var decision = evaluate(false, true,
            LegacyPowerConnectionNegotiation.NeighborKind.OTHER_OR_EMPTY,
            false, false, false, false, false);
        assertEquals(LegacyPowerConnectionNegotiation.Action.NOT_CONNECTABLE, decision.action());
    }

    @Test
    void coverMustAllowConnectionBeforePipeOrEndpointNegotiation() {
        var decision = evaluate(true, false,
            LegacyPowerConnectionNegotiation.NeighborKind.COMPATIBLE_PIPE,
            false, false, false, true, false);
        assertEquals(LegacyPowerConnectionNegotiation.Action.NOT_CONNECTABLE, decision.action());
        assertFalse(decision.connectReciprocal());
    }

    @Test
    void compatiblePipeConnectsBackOnlyWhenOppositeSideIsMissing() {
        var connectBack = evaluate(true, true,
            LegacyPowerConnectionNegotiation.NeighborKind.COMPATIBLE_PIPE,
            false, false, false, false, true);
        assertEquals(LegacyPowerConnectionNegotiation.Action.CONNECT_LOCAL_AND_RECIPROCAL_IF_NEEDED,
            connectBack.action());
        assertTrue(connectBack.connectReciprocal());

        var alreadyConnected = evaluate(true, true,
            LegacyPowerConnectionNegotiation.NeighborKind.COMPATIBLE_PIPE,
            true, false, false, false, true);
        assertFalse(alreadyConnected.connectReciprocal());
    }

    @Test
    void gt6StyleAirAndCableSpecificEndpointCanConnectLocally() {
        var openAir = evaluate(true, true,
            LegacyPowerConnectionNegotiation.NeighborKind.OTHER_OR_EMPTY,
            false, true, true, false, true);
        assertEquals(LegacyPowerConnectionNegotiation.Action.CONNECT_LOCAL, openAir.action());

        var endpoint = evaluate(true, true,
            LegacyPowerConnectionNegotiation.NeighborKind.OTHER_OR_EMPTY,
            false, false, false, true, true);
        assertEquals(LegacyPowerConnectionNegotiation.Action.CONNECT_LOCAL, endpoint.action());
    }

    @Test
    void traditionalModeDoesNotConnectToAirWithoutEndpointApproval() {
        var decision = evaluate(true, true,
            LegacyPowerConnectionNegotiation.NeighborKind.OTHER_OR_EMPTY,
            false, false, true, false, true);
        assertEquals(LegacyPowerConnectionNegotiation.Action.NOT_CONNECTABLE, decision.action());
    }

    @Test
    void unloadedChunkIsPreservedOnlyAfterEarlierPredicatesFailToConnect() {
        var unloaded = evaluate(true, true,
            LegacyPowerConnectionNegotiation.NeighborKind.OTHER_OR_EMPTY,
            false, false, false, false, false);
        assertEquals(LegacyPowerConnectionNegotiation.Action.PRESERVE_EXISTING_UNLOADED,
            unloaded.action());
        assertEquals(LegacyPowerConnectionResult.UNLOADED, unloaded.asConnectionResult());

        var coverBlocked = evaluate(true, false,
            LegacyPowerConnectionNegotiation.NeighborKind.OTHER_OR_EMPTY,
            false, false, false, false, false);
        assertEquals(LegacyPowerConnectionNegotiation.Action.NOT_CONNECTABLE,
            coverBlocked.action());
    }

    @Test
    void resultCanBeAppliedThroughExistingTriStateMaskPolicy() {
        var decision = evaluate(true, true,
            LegacyPowerConnectionNegotiation.NeighborKind.OTHER_OR_EMPTY,
            false, false, false, false, false);
        assertEquals(0b001000,
            decision.asConnectionResult().updateConnectionMask(0b001000, 0b001000));
    }

    @Test
    void compatiblePipeClassTestIsSymmetricForSubclassRelationships() {
        class Parent {}
        class Child extends Parent {}
        assertTrue(LegacyPowerConnectionNegotiation.compatiblePipeClasses(Parent.class, Child.class));
        assertTrue(LegacyPowerConnectionNegotiation.compatiblePipeClasses(Child.class, Parent.class));
        assertFalse(LegacyPowerConnectionNegotiation.compatiblePipeClasses(Parent.class, String.class));
        assertFalse(LegacyPowerConnectionNegotiation.compatiblePipeClasses(null, Parent.class));
    }

    @Test
    void reciprocalDisconnectRequiresCompatiblePipeAndExistingOppositeBit() {
        assertTrue(LegacyPowerConnectionNegotiation.shouldDisconnectReciprocal(true, true));
        assertFalse(LegacyPowerConnectionNegotiation.shouldDisconnectReciprocal(true, false));
        assertFalse(LegacyPowerConnectionNegotiation.shouldDisconnectReciprocal(false, true));
    }
}

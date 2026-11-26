package com.gtnh.reforged.legacy.boiler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyBoilerSteamOutputTransferTest {
    private static final class FakePort implements LegacyBoilerSteamOutputTransfer.TransferPort {
        private final Map<LegacyBoilerSteamOutputTransfer.Side, Integer> simulatedAcceptance =
            new EnumMap<>(LegacyBoilerSteamOutputTransfer.Side.class);
        private final Map<LegacyBoilerSteamOutputTransfer.Side, Integer> actualAcceptance =
            new EnumMap<>(LegacyBoilerSteamOutputTransfer.Side.class);
        private final List<LegacyBoilerSteamOutputTransfer.Side> simulatedSides = new ArrayList<>();
        private final List<Integer> offeredAmounts = new ArrayList<>();
        private final List<Integer> actualFillAmounts = new ArrayList<>();
        private final List<LegacyBoilerSteamOutputTransfer.Side> actualFillSides = new ArrayList<>();

        @Override
        public boolean hasTarget(LegacyBoilerSteamOutputTransfer.Side side) {
            return simulatedAcceptance.containsKey(side);
        }

        @Override
        public int simulateFill(LegacyBoilerSteamOutputTransfer.Side side, int offeredAmount) {
            simulatedSides.add(side);
            offeredAmounts.add(offeredAmount);
            return Math.min(offeredAmount, simulatedAcceptance.get(side));
        }

        @Override
        public int fill(LegacyBoilerSteamOutputTransfer.Side side, int drainedAmount) {
            actualFillSides.add(side);
            actualFillAmounts.add(drainedAmount);
            return Math.min(drainedAmount, actualAcceptance.getOrDefault(side, drainedAmount));
        }

        FakePort target(
            LegacyBoilerSteamOutputTransfer.Side side,
            int simulatedAmount
        ) {
            simulatedAcceptance.put(side, simulatedAmount);
            return this;
        }

        FakePort actualAcceptance(
            LegacyBoilerSteamOutputTransfer.Side side,
            int actualAmount
        ) {
            actualAcceptance.put(side, actualAmount);
            return this;
        }
    }

    @Test
    void excludesFrontAndBottomAndPreservesSourceDirectionOrder() {
        var port = new FakePort()
            .target(LegacyBoilerSteamOutputTransfer.Side.DOWN, 100)
            .target(LegacyBoilerSteamOutputTransfer.Side.UP, 100)
            .target(LegacyBoilerSteamOutputTransfer.Side.NORTH, 100)
            .target(LegacyBoilerSteamOutputTransfer.Side.SOUTH, 100)
            .target(LegacyBoilerSteamOutputTransfer.Side.WEST, 100)
            .target(LegacyBoilerSteamOutputTransfer.Side.EAST, 100);

        LegacyBoilerSteamOutputTransfer.push(
            100, LegacyBoilerSteamOutputTransfer.Side.NORTH, port);

        assertEquals(List.of(
            LegacyBoilerSteamOutputTransfer.Side.UP,
            LegacyBoilerSteamOutputTransfer.Side.SOUTH,
            LegacyBoilerSteamOutputTransfer.Side.WEST,
            LegacyBoilerSteamOutputTransfer.Side.EAST
        ), port.simulatedSides);
    }

    @Test
    void partialAcceptanceDrainsOnlyTheSimulatedAcceptedAmount() {
        var port = new FakePort().target(LegacyBoilerSteamOutputTransfer.Side.UP, 20);

        var result = LegacyBoilerSteamOutputTransfer.push(
            100, LegacyBoilerSteamOutputTransfer.Side.NORTH, port);

        assertEquals(80, result.remainingSteamAmount());
        assertEquals(20, result.steamDrained());
        assertEquals(List.of(50), port.offeredAmounts);
        assertEquals(List.of(20), port.actualFillAmounts);
    }

    @Test
    void simulatedRejectionLeavesSteamUntouchedAndDoesNotPerformRealFill() {
        var port = new FakePort().target(LegacyBoilerSteamOutputTransfer.Side.UP, 0);

        var result = LegacyBoilerSteamOutputTransfer.push(
            100, LegacyBoilerSteamOutputTransfer.Side.NORTH, port);

        assertEquals(100, result.remainingSteamAmount());
        assertEquals(0, result.steamDrained());
        assertTrue(port.actualFillAmounts.isEmpty());
    }

    @Test
    void eachSideRecalculatesHalfRequestFromTheUpdatedSteamAmount() {
        var port = new FakePort()
            .target(LegacyBoilerSteamOutputTransfer.Side.UP, 20)
            .target(LegacyBoilerSteamOutputTransfer.Side.NORTH, 10)
            .target(LegacyBoilerSteamOutputTransfer.Side.SOUTH, 0);

        var result = LegacyBoilerSteamOutputTransfer.push(
            100, LegacyBoilerSteamOutputTransfer.Side.WEST, port);

        assertEquals(List.of(
            LegacyBoilerSteamOutputTransfer.Side.UP,
            LegacyBoilerSteamOutputTransfer.Side.NORTH,
            LegacyBoilerSteamOutputTransfer.Side.SOUTH
        ), port.simulatedSides);
        assertEquals(List.of(50, 40, 35), port.offeredAmounts);
        assertEquals(70, result.remainingSteamAmount());
        assertEquals(30, result.steamDrained());
    }

    @Test
    void oneUnitOfSteamStillRequestsOneUnit() {
        var port = new FakePort().target(LegacyBoilerSteamOutputTransfer.Side.UP, 1);

        var result = LegacyBoilerSteamOutputTransfer.push(
            1, LegacyBoilerSteamOutputTransfer.Side.NORTH, port);

        assertEquals(List.of(1), port.offeredAmounts);
        assertEquals(0, result.remainingSteamAmount());
        assertEquals(1, result.steamDrained());
    }

    @Test
    void legacyBehaviorIgnoresSmallerRealFillReturnAfterSuccessfulSimulation() {
        var port = new FakePort()
            .target(LegacyBoilerSteamOutputTransfer.Side.UP, 20)
            .actualAcceptance(LegacyBoilerSteamOutputTransfer.Side.UP, 7);

        var result = LegacyBoilerSteamOutputTransfer.push(
            100, LegacyBoilerSteamOutputTransfer.Side.NORTH, port);

        assertEquals(80, result.remainingSteamAmount());
        assertEquals(20, result.steamDrained());
        assertEquals(7, result.targetReportedFillAmount());
        assertEquals(20, port.actualFillAmounts.get(0));
    }

    @Test
    void emptySteamDoesNotQueryAnyTarget() {
        var port = new FakePort().target(LegacyBoilerSteamOutputTransfer.Side.UP, 100);

        var result = LegacyBoilerSteamOutputTransfer.push(
            0, LegacyBoilerSteamOutputTransfer.Side.NORTH, port);

        assertEquals(0, result.remainingSteamAmount());
        assertTrue(port.simulatedSides.isEmpty());
    }

    @Test
    void negativeSteamAmountIsRejected() {
        var port = new FakePort();
        assertThrows(IllegalArgumentException.class, () ->
            LegacyBoilerSteamOutputTransfer.push(
                -1, LegacyBoilerSteamOutputTransfer.Side.NORTH, port));
    }
}

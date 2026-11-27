package com.gtnh.reforged.legacy.boiler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.transfer.transaction.Transaction;

import static org.junit.jupiter.api.Assertions.*;

class LegacyBoilerFluidTransferCoreTest {
    private static final String WATER_ID = "minecraft:water";
    private static final String STEAM_ID = "gtnh:steam";

    private static final class MutablePolicy {
        private final AtomicBoolean allowed = new AtomicBoolean(true);

        boolean isAllowed() {
            return allowed.get();
        }
    }

    private static LegacyBoilerFluidState newState(int waterAmount, int steamAmount) {
        return new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState(
                waterAmount != 0 ? WATER_ID : null,
                waterAmount,
                -55
            ),
            steamAmount > 0 ? STEAM_ID : null,
            steamAmount
        );
    }

    private static LegacyBoilerFluidTransferCore newCore(LegacyBoilerFluidState state) {
        return new LegacyBoilerFluidTransferCore(
            state, WATER_ID, STEAM_ID, () -> 32000, () -> 64000
        );
    }

    @Test
    void waterInsertIsTransactionalAndRootAbortRestoresAggregate() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(25, core.insertWater(WATER_ID, 25, () -> true, () -> journal.beforeMutation(root)));
            assertEquals(125, state.water().legacyAmount());
            assertEquals(500, state.steamAmount());
            // Root aborts.
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(-55, state.water().excessWaterRemainder());
        assertEquals(0, callbacks.get());
    }

    @Test
    void waterInsertCommitsAndUsesLegacyNegativeWindowHeadroom() {
        var state = newState(-1, 0);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(32001, core.insertWater(WATER_ID, 32001, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(32000, state.water().legacyAmount());
            root.commit();
        }

        assertEquals(32000, state.water().legacyAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void steamExtractionCommitsExactSimulatedAmount() {
        var state = newState(100, 75);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(40, core.extractSteam(STEAM_ID, 40, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(35, state.steamAmount());
            root.commit();
        }

        assertEquals(35, state.steamAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void steamExtractionRollbackRestoresAmountAndIdentity() {
        var state = newState(100, 75);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(75, core.extractSteam(STEAM_ID, 100, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(0, state.steamAmount());
            // Root aborts.
        }

        assertEquals(75, state.steamAmount());
        assertEquals(STEAM_ID, state.steamFluidId());
        assertEquals(0, callbacks.get());
    }

    @Test
    void rejectsWrongFluidIdentityAndDoesNotRegisterJournal() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(0, core.insertWater(STEAM_ID, 20, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(0, core.extractSteam(WATER_ID, 20, () -> true,
                () -> journal.beforeMutation(root)));
            root.commit();
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());
    }

    @Test
    void liveAccessPolicyIsCheckedBeforeSimulationAndBeforeMutation() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);
        var policy = new MutablePolicy();

        policy.allowed.set(false);
        try (var root = Transaction.openRoot()) {
            assertEquals(0, core.insertWater(WATER_ID, 20, policy::isAllowed,
                () -> journal.beforeMutation(root)));
            assertEquals(0, core.extractSteam(STEAM_ID, 20, policy::isAllowed,
                () -> journal.beforeMutation(root)));
            root.commit();
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());

        policy.allowed.set(true);
        try (var root = Transaction.openRoot()) {
            assertEquals(20, core.insertWater(WATER_ID, 20, policy::isAllowed,
                () -> journal.beforeMutation(root)));
            assertEquals(20, core.extractSteam(STEAM_ID, 20, policy::isAllowed,
                () -> journal.beforeMutation(root)));
            root.commit();
        }

        assertEquals(120, state.water().legacyAmount());
        assertEquals(480, state.steamAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void deniesMutationWhenAccessChangesAfterSimulation() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);
        var waterChecks = new AtomicInteger();
        var steamChecks = new AtomicInteger();

        try (var root = Transaction.openRoot()) {
            assertEquals(0, core.insertWater(WATER_ID, 20,
                () -> waterChecks.incrementAndGet() == 1,
                () -> journal.beforeMutation(root)));
            assertEquals(0, core.extractSteam(STEAM_ID, 20,
                () -> steamChecks.incrementAndGet() == 1,
                () -> journal.beforeMutation(root)));
            root.commit();
        }

        assertEquals(2, waterChecks.get());
        assertEquals(2, steamChecks.get());
        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());
    }

    @Test
    void zeroAmountOperationsDoNotRegisterJournalOrNotify() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(0, core.insertWater(WATER_ID, 0, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(0, core.extractSteam(STEAM_ID, 0, () -> true,
                () -> journal.beforeMutation(root)));
            root.commit();
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());
    }

    @Test
    void rejectsNegativeAmountsAndClampsNegativeCapacityToZero() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = new LegacyBoilerFluidTransferCore(
            state, WATER_ID, STEAM_ID, () -> -1, () -> -1
        );

        try (var root = Transaction.openRoot()) {
            assertThrows(IllegalArgumentException.class,
                () -> core.insertWater(WATER_ID, -1, () -> true,
                    () -> journal.beforeMutation(root)));
            assertEquals(0, core.insertWater(WATER_ID, 20, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(20, core.extractSteam(STEAM_ID, 20, () -> true,
                () -> journal.beforeMutation(root)));
            root.commit();
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(480, state.steamAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void waterInsertAcceptsOnlyAvailableCapacity() {
        var state = newState(31990, 0);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(10, core.insertWater(WATER_ID, 25, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(32000, state.water().legacyAmount());
            root.commit();
        }

        assertEquals(32000, state.water().legacyAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void steamExtractionAcceptsOnlyAvailableAmount() {
        var state = newState(100, 10);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(10, core.extractSteam(STEAM_ID, 25, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(0, state.steamAmount());
            root.commit();
        }

        assertEquals(0, state.steamAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void steamExtractionRejectsNegativeAmount() {
        var state = newState(100, 500);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertThrows(IllegalArgumentException.class,
                () -> core.extractSteam(STEAM_ID, -1, () -> true,
                    () -> {
                        throw new AssertionError("journal must not be registered");
                    }));
        }

        assertEquals(500, state.steamAmount());
    }

    @Test
    void fullWaterTankAndEmptySteamTankDoNotRegisterJournal() {
        var state = newState(32000, 0);
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);
        var core = newCore(state);

        try (var root = Transaction.openRoot()) {
            assertEquals(0, core.insertWater(WATER_ID, 1, () -> true,
                () -> journal.beforeMutation(root)));
            assertEquals(0, core.extractSteam(STEAM_ID, 1, () -> true,
                () -> journal.beforeMutation(root)));
            root.commit();
        }

        assertEquals(32000, state.water().legacyAmount());
        assertEquals(0, state.steamAmount());
        assertEquals(0, callbacks.get());
    }
}

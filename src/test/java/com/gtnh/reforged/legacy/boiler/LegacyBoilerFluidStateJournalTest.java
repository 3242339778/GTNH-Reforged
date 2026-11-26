package com.gtnh.reforged.legacy.boiler;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LegacyBoilerFluidStateJournalTest {
    private static LegacyBoilerFluidState newState() {
        return new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState("minecraft:water", 100, 7),
            "gtnh:steam",
            500
        );
    }

    @Test
    void rootAbortRestoresWaterSteamAndRemainderTogether() {
        var state = newState();
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);

        try (var root = Transaction.openRoot()) {
            journal.beforeMutation(root);
            state.water().fill("minecraft:water", 25, 32000, true);
            state.extractSteam("gtnh:steam", 125, true);
            state.setSteam("gtnh:superheated_steam", 200);
            // No commit: closing the root aborts all aggregate mutations.
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(7, state.water().excessWaterRemainder());
        assertEquals("minecraft:water", state.water().legacyFluidId());
        assertEquals("gtnh:steam", state.steamFluidId());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());
        assertFalse(journal.isInTransaction());
    }

    @Test
    void rootCommitRetainsChangesAndRunsCommitActionOnceOutsideTransaction() {
        var state = newState();
        var callbacks = new AtomicInteger();
        var callbackSawOpenTransaction = new AtomicInteger(-1);
        LegacyBoilerFluidStateJournal journal = new LegacyBoilerFluidStateJournal(state, () -> {
            callbacks.incrementAndGet();
            callbackSawOpenTransaction.set(Transaction.getLifecycle() == Transaction.Lifecycle.OPEN ? 1 : 0);
        });

        try (var root = Transaction.openRoot()) {
            journal.beforeMutation(root);
            state.water().fill("minecraft:water", 25, 32000, true);
            state.extractSteam("gtnh:steam", 125, true);
            root.commit();
        }

        assertEquals(125, state.water().legacyAmount());
        assertEquals(375, state.steamAmount());
        assertEquals(1, callbacks.get());
        assertEquals(0, callbackSawOpenTransaction.get());
        assertFalse(journal.isInTransaction());
    }

    @Test
    void committedChildRemainsProvisionalUntilParentCommits() {
        var state = newState();
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);

        try (var root = Transaction.openRoot()) {
            try (var child = Transaction.open(root)) {
                journal.beforeMutation(child);
                state.water().fill("minecraft:water", 10, 32000, true);
                state.extractSteam("gtnh:steam", 50, true);
                child.commit();
            }

            assertEquals(110, state.water().legacyAmount());
            assertEquals(450, state.steamAmount());
            assertEquals(0, callbacks.get());
            // Parent intentionally aborts.
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(7, state.water().excessWaterRemainder());
        assertEquals(0, callbacks.get());
    }

    @Test
    void committedChildAndParentCommitRetainAggregateAndNotifyOnce() {
        var state = newState();
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);

        try (var root = Transaction.openRoot()) {
            try (var child = Transaction.open(root)) {
                journal.beforeMutation(child);
                state.water().fill("minecraft:water", 10, 32000, true);
                state.extractSteam("gtnh:steam", 50, true);
                child.commit();
            }
            root.commit();
        }

        assertEquals(110, state.water().legacyAmount());
        assertEquals(450, state.steamAmount());
        assertEquals(1, callbacks.get());
        assertFalse(journal.isInTransaction());
    }

    @Test
    void abortedChildRestoresOnlyChildChangesAndParentCanStillCommit() {
        var state = newState();
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);

        try (var root = Transaction.openRoot()) {
            journal.beforeMutation(root);
            state.water().fill("minecraft:water", 20, 32000, true);

            try (var child = Transaction.open(root)) {
                journal.beforeMutation(child);
                state.extractSteam("gtnh:steam", 200, true);
                state.setSteam("gtnh:superheated_steam", 300);
                // Child aborts on close.
            }

            assertEquals(120, state.water().legacyAmount());
            assertEquals("gtnh:steam", state.steamFluidId());
            assertEquals(500, state.steamAmount());
            root.commit();
        }

        assertEquals(120, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void repeatedMutationsAtOneDepthUseOneOriginalSnapshot() {
        var state = newState();
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);

        try (var root = Transaction.openRoot()) {
            journal.beforeMutation(root);
            state.water().fill("minecraft:water", 10, 32000, true);
            journal.beforeMutation(root);
            state.water().fill("minecraft:water", 20, 32000, true);
            journal.beforeMutation(root);
            state.extractSteam("gtnh:steam", 100, true);
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());
    }

    @Test
    void noMutationMeansNoJournalRegistrationOrCommitCallback() {
        var state = newState();
        var callbacks = new AtomicInteger();
        var journal = new LegacyBoilerFluidStateJournal(state, callbacks::incrementAndGet);

        try (var root = Transaction.openRoot()) {
            // Mirrors the handler contract: rejected/zero operations do not
            // call beforeMutation at all.
            assertEquals(0, state.extractSteam("minecraft:water", 50, true));
            assertEquals(0, state.extractSteam("gtnh:steam", 0, true));
            root.commit();
        }

        assertEquals(0, callbacks.get());
        assertFalse(journal.isInTransaction());
    }
}

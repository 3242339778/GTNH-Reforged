package com.gtnh.reforged.legacy.boiler;

import java.util.Objects;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * NeoForge transaction journal for the complete Advanced Boiler fluid aggregate.
 *
 * <p>All water and steam mutations that belong to an external transfer must use
 * this same journal. Normal machine ticking must not use it.</p>
 */
public final class LegacyBoilerFluidStateJournal
    extends SnapshotJournal<LegacyBoilerFluidState.Snapshot> {

    private final LegacyBoilerFluidState state;
    private final Runnable rootCommitAction;

    public LegacyBoilerFluidStateJournal(
        LegacyBoilerFluidState state,
        Runnable rootCommitAction
    ) {
        this.state = Objects.requireNonNull(state, "state");
        this.rootCommitAction = Objects.requireNonNull(rootCommitAction, "rootCommitAction");
    }

    /** Returns the authoritative aggregate protected by this journal. */
    public LegacyBoilerFluidState state() {
        return state;
    }

    /**
     * Records the aggregate before a transaction mutates it.
     * Call only after determining that the operation will actually change state.
     */
    public void beforeMutation(TransactionContext transaction) {
        updateSnapshots(Objects.requireNonNull(transaction, "transaction"));
    }

    @Override
    protected LegacyBoilerFluidState.Snapshot createSnapshot() {
        return state.snapshot();
    }

    @Override
    protected void revertToSnapshot(LegacyBoilerFluidState.Snapshot snapshot) {
        state.restore(snapshot);
    }

    @Override
    protected void onRootCommit(LegacyBoilerFluidState.Snapshot originalState) {
        // The supplied action must tolerate an unloaded/removed owner. The
        // journal deliberately knows nothing about levels or block entities.
        rootCommitAction.run();
    }
}

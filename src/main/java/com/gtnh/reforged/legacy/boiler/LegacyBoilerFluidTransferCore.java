package com.gtnh.reforged.legacy.boiler;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Game-registry-independent transfer logic for the Advanced Boiler's two
 * legacy fluid inventories.
 *
 * <p>This class deliberately knows only the legacy aggregate and ordinary
 * Java values. The NeoForge adapter is responsible for validating
 * {@code FluidResource} identity and supplies the transaction snapshot and
 * live access-policy callbacks.</p>
 */
public final class LegacyBoilerFluidTransferCore {
    private final LegacyBoilerFluidState state;
    private final String waterFluidId;
    private final String steamFluidId;
    private final IntSupplier waterCapacity;
    private final IntSupplier steamCapacity;

    public LegacyBoilerFluidTransferCore(
        LegacyBoilerFluidState state,
        String waterFluidId,
        String steamFluidId,
        IntSupplier waterCapacity,
        IntSupplier steamCapacity
    ) {
        this.state = Objects.requireNonNull(state, "state");
        this.waterFluidId = requireFluidId(waterFluidId, "waterFluidId");
        this.steamFluidId = requireFluidId(steamFluidId, "steamFluidId");
        this.waterCapacity = Objects.requireNonNull(waterCapacity, "waterCapacity");
        this.steamCapacity = Objects.requireNonNull(steamCapacity, "steamCapacity");
    }

    /**
     * Attempts a water fill. The access policy is sampled before simulation
     * and again immediately before the journal callback and actual mutation.
     */
    public int insertWater(
        String incomingFluidId,
        int amount,
        BooleanSupplier accessAllowed,
        Runnable beforeMutation
    ) {
        checkAmount(amount);
        Objects.requireNonNull(accessAllowed, "accessAllowed");
        Objects.requireNonNull(beforeMutation, "beforeMutation");

        if (!waterFluidId.equals(incomingFluidId) || amount == 0 || !accessAllowed.getAsBoolean()) {
            return 0;
        }

        int capacity = nonNegativeCapacity(waterCapacity);
        int accepted = Math.max(0,
            state.water().fill(waterFluidId, amount, capacity, false));
        if (accepted == 0 || !accessAllowed.getAsBoolean()) {
            return 0;
        }

        beforeMutation.run();
        int inserted = state.water().fill(waterFluidId, amount, capacity, true);
        if (inserted != accepted) {
            throw new IllegalStateException("water simulation and mutation results diverged");
        }
        return inserted;
    }

    /**
     * Attempts a steam extraction. The access policy is sampled before
     * simulation and again immediately before the journal callback and
     * actual mutation.
     */
    public int extractSteam(
        String requestedFluidId,
        int amount,
        BooleanSupplier accessAllowed,
        Runnable beforeMutation
    ) {
        checkAmount(amount);
        Objects.requireNonNull(accessAllowed, "accessAllowed");
        Objects.requireNonNull(beforeMutation, "beforeMutation");

        if (!steamFluidId.equals(requestedFluidId) || amount == 0 || !accessAllowed.getAsBoolean()) {
            return 0;
        }

        int extracted = state.extractSteam(steamFluidId, amount, false);
        if (extracted == 0 || !accessAllowed.getAsBoolean()) {
            return 0;
        }

        beforeMutation.run();
        int actual = state.extractSteam(steamFluidId, extracted, true);
        if (actual != extracted) {
            throw new IllegalStateException("steam simulation and extraction results diverged");
        }
        return actual;
    }

    private static int nonNegativeCapacity(IntSupplier capacitySupplier) {
        return Math.max(0, capacitySupplier.getAsInt());
    }

    private static void checkAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
    }

    private static String requireFluidId(String fluidId, String name) {
        Objects.requireNonNull(fluidId, name);
        if (fluidId.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return fluidId;
    }
}

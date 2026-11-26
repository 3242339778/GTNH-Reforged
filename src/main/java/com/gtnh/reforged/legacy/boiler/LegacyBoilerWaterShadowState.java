package com.gtnh.reforged.legacy.boiler;

import java.util.Objects;

/**
 * Authoritative, game-API-free representation of the GT5U boiler water tank.
 * The legacy amount and conversion remainder are signed. Never expose the
 * signed amount directly through NeoForge's non-negative transfer API.
 */
public final class LegacyBoilerWaterShadowState {
    private String fluidId;
    private int amount;
    private int excessWaterRemainder;

    public LegacyBoilerWaterShadowState(String fluidId, int amount, int excessWaterRemainder) {
        if (amount > 0 && !isValidFluidId(fluidId)) {
            throw new IllegalArgumentException("positive water amount requires a valid fluid id");
        }
        this.fluidId = fluidId;
        this.amount = amount;
        this.excessWaterRemainder = excessWaterRemainder;
    }

    /** Exact signed amount held by the legacy machine. */
    public int legacyAmount() { return amount; }

    /** Exact signed GT5U steam-to-water conversion remainder. */
    public int excessWaterRemainder() { return excessWaterRemainder; }

    /** Legacy fluid identity, including during the transient negative window. */
    public String legacyFluidId() { return fluidId; }

    /** Non-negative NeoForge-facing amount projection. */
    public int exposedAmount() { return Math.max(0, amount); }

    /** Non-positive legacy stacks project as an empty resource. */
    public String exposedFluidId() { return amount > 0 ? fluidId : null; }

    /**
     * Exact integer conversion arithmetic from MTEBoiler.produceSteam.
     * Caller must already have passed the original water production gate.
     *
     * @return number of water units subtracted from the signed legacy amount
     */
    public int consumeWaterForSteam(int steamAmount) {
        if (steamAmount < 0) throw new IllegalArgumentException("steamAmount must be non-negative");
        excessWaterRemainder -= steamAmount;
        int consumed = 0;
        if (excessWaterRemainder < 0) {
            consumed = -excessWaterRemainder / LegacyAdvancedBoilerSteamModel.STEAM_PER_WATER;
            amount -= consumed;
            excessWaterRemainder += LegacyAdvancedBoilerSteamModel.STEAM_PER_WATER * consumed;
        }
        return consumed;
    }

    /**
     * Mirrors MTEBasicTank.fill before the outer side/Cover/global gates.
     * In the negative window, capacity is calculated from the signed amount,
     * preserving the original temporary capacity+N headroom.
     */
    public int fill(String incomingFluidId, int incomingAmount, int capacity, boolean doFill) {
        if (!isValidFluidId(incomingFluidId) || incomingAmount <= 0 || capacity < 0) return 0;

        if (!isValidFluidId(fluidId)) {
            if (incomingAmount <= capacity) {
                if (doFill) {
                    fluidId = incomingFluidId;
                    amount = incomingAmount;
                }
                return incomingAmount;
            }
            if (doFill) {
                fluidId = incomingFluidId;
                amount = capacity;
            }
            return capacity;
        }

        if (!Objects.equals(fluidId, incomingFluidId)) return 0;

        int space = capacity - amount;
        if (incomingAmount <= space) {
            if (doFill) amount += incomingAmount;
            return incomingAmount;
        }
        if (doFill) amount = capacity;
        return space;
    }

    /** Mirrors MTEBasicTank.onPreTick cleanup; the conversion remainder survives. */
    public boolean preTickCleanup(boolean fluidChangingAllowed) {
        if (fluidChangingAllowed && isValidFluidId(fluidId) && amount <= 0) {
            fluidId = null;
            amount = 0;
            return true;
        }
        return false;
    }

    /** Captures all mutable water state for a transaction snapshot. */
    public Snapshot snapshot() {
        return new Snapshot(fluidId, amount, excessWaterRemainder);
    }

    /** Restores identity, signed amount and signed remainder atomically. */
    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        fluidId = snapshot.fluidId;
        amount = snapshot.amount;
        excessWaterRemainder = snapshot.excessWaterRemainder;
    }

    private static boolean isValidFluidId(String id) {
        return id != null && !id.isBlank();
    }

    public static final class Snapshot {
        private final String fluidId;
        private final int amount;
        private final int excessWaterRemainder;

        private Snapshot(String fluidId, int amount, int excessWaterRemainder) {
            this.fluidId = fluidId;
            this.amount = amount;
            this.excessWaterRemainder = excessWaterRemainder;
        }

        public String fluidId() { return fluidId; }
        public int amount() { return amount; }
        public int excessWaterRemainder() { return excessWaterRemainder; }
    }
}

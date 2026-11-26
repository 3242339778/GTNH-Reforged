package com.gtnh.reforged.legacy.boiler;

import java.util.Objects;

/**
 * Authoritative game-API-free aggregate for the Advanced Boiler's fluid state.
 *
 * <p>Water keeps its signed GT5U shadow representation. Steam remains a
 * non-negative output inventory. A single snapshot covers both tanks so a
 * future NeoForge transaction can roll them back coherently.</p>
 */
public final class LegacyBoilerFluidState {
    private final LegacyBoilerWaterShadowState water;
    private String steamFluidId;
    private int steamAmount;

    public LegacyBoilerFluidState(
        LegacyBoilerWaterShadowState water,
        String steamFluidId,
        int steamAmount
    ) {
        this.water = Objects.requireNonNull(water, "water");
        validateSteamState(steamFluidId, steamAmount);
        this.steamFluidId = steamFluidId;
        this.steamAmount = steamAmount;
    }

    /** Returns the authoritative signed GT5U water state. */
    public LegacyBoilerWaterShadowState water() {
        return water;
    }

    /** Exact non-negative steam inventory amount. */
    public int steamAmount() {
        return steamAmount;
    }

    /** Legacy steam fluid identity, retained independently of water identity. */
    public String steamFluidId() {
        return steamFluidId;
    }

    /** NeoForge-facing steam amount projection. */
    public int exposedSteamAmount() {
        return steamAmount;
    }

    /** Empty steam inventories project as an empty resource. */
    public String exposedSteamFluidId() {
        return steamAmount > 0 ? steamFluidId : null;
    }

    /**
     * Replaces the steam inventory state. This is a model operation; callers
     * must place it inside the aggregate transaction journal when invoked by
     * an external transfer.
     */
    public void setSteam(String fluidId, int amount) {
        validateSteamState(fluidId, amount);
        steamFluidId = fluidId;
        steamAmount = amount;
    }

    /**
     * Extracts matching steam without allowing a negative request or amount.
     * A simulation reports the same amount without changing this aggregate.
     */
    public int extractSteam(String fluidId, int requestedAmount, boolean doExtract) {
        if (!isValidFluidId(fluidId) || requestedAmount <= 0 || steamAmount <= 0
            || !Objects.equals(steamFluidId, fluidId)) {
            return 0;
        }
        int extracted = Math.min(requestedAmount, steamAmount);
        if (doExtract) {
            steamAmount -= extracted;
        }
        return extracted;
    }

    /** Captures water identity/amount/remainder and steam identity/amount together. */
    public Snapshot snapshot() {
        return new Snapshot(water.snapshot(), steamFluidId, steamAmount);
    }

    /** Restores both fluid inventories and the water conversion remainder together. */
    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        water.restore(snapshot.waterSnapshot);
        steamFluidId = snapshot.steamFluidId;
        steamAmount = snapshot.steamAmount;
    }

    private static void validateSteamState(String fluidId, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("steam amount must be non-negative");
        }
        if (amount > 0 && !isValidFluidId(fluidId)) {
            throw new IllegalArgumentException("positive steam amount requires a valid fluid id");
        }
    }

    private static boolean isValidFluidId(String id) {
        return id != null && !id.isBlank();
    }

    public static final class Snapshot {
        private final LegacyBoilerWaterShadowState.Snapshot waterSnapshot;
        private final String steamFluidId;
        private final int steamAmount;

        private Snapshot(
            LegacyBoilerWaterShadowState.Snapshot waterSnapshot,
            String steamFluidId,
            int steamAmount
        ) {
            this.waterSnapshot = waterSnapshot;
            this.steamFluidId = steamFluidId;
            this.steamAmount = steamAmount;
        }

        public String steamFluidId() {
            return steamFluidId;
        }

        public int steamAmount() {
            return steamAmount;
        }

        public String waterFluidId() {
            return waterSnapshot.fluidId();
        }

        public int waterAmount() {
            return waterSnapshot.amount();
        }

        public int excessWaterRemainder() {
            return waterSnapshot.excessWaterRemainder();
        }
    }
}

package com.gtnh.reforged.legacy.boiler;

import java.util.Objects;

/**
 * Game-API-free implementation of the GT5U boiler steam-push policy.
 *
 * <p>The caller supplies side-specific target access. This class preserves the
 * source iteration order and the simulate-drain / simulate-fill / real-drain /
 * real-fill sequence used by GTUtility.moveFluid for a boiler's single steam
 * tank.</p>
 */
public final class LegacyBoilerSteamOutputTransfer {
    private static final Side[] SOURCE_SIDE_ORDER = {
        Side.DOWN, Side.UP, Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST
    };

    private LegacyBoilerSteamOutputTransfer() {}

    public enum Side {
        DOWN, UP, NORTH, SOUTH, WEST, EAST
    }

    /**
     * Target-side operations for one adjacent inventory.
     *
     * <p>{@code simulateFill} must report how much of the offered amount the
     * target would accept. {@code fill} performs the real insertion and returns
     * the target-reported inserted amount.</p>
     */
    public interface TransferPort {
        boolean hasTarget(Side side);

        int simulateFill(Side side, int offeredAmount);

        int fill(Side side, int drainedAmount);
    }

    public static final class Result {
        private final int initialSteamAmount;
        private final int remainingSteamAmount;
        private final int steamDrained;
        private final int targetReportedFillAmount;
        private final int targetAttempts;

        private Result(
            int initialSteamAmount,
            int remainingSteamAmount,
            int steamDrained,
            int targetReportedFillAmount,
            int targetAttempts
        ) {
            this.initialSteamAmount = initialSteamAmount;
            this.remainingSteamAmount = remainingSteamAmount;
            this.steamDrained = steamDrained;
            this.targetReportedFillAmount = targetReportedFillAmount;
            this.targetAttempts = targetAttempts;
        }

        public int initialSteamAmount() { return initialSteamAmount; }
        public int remainingSteamAmount() { return remainingSteamAmount; }
        public int steamDrained() { return steamDrained; }
        public int targetReportedFillAmount() { return targetReportedFillAmount; }
        public int targetAttempts() { return targetAttempts; }
    }

    /**
     * Pushes steam to every eligible side in GT5U's ForgeDirection.VALID_DIRECTIONS
     * order. The front and bottom sides are excluded.
     *
     * <p>Each side requests half of the <em>current</em> steam amount, with a
     * minimum request of one. The request is recalculated after every preceding
     * transfer.</p>
     *
     * <p>GT5U ignores the return value of the real destination fill. Therefore,
     * the source amount is reduced by the simulated accepted amount even if the
     * subsequent real fill reports a smaller amount. This preserves the legacy
     * call sequence; callers should use a stable target or a transactional
     * adapter if they need atomic transfer semantics.</p>
     */
    public static Result push(int steamAmount, Side frontFacing, TransferPort port) {
        if (steamAmount < 0) {
            throw new IllegalArgumentException("steamAmount must be non-negative");
        }
        Objects.requireNonNull(frontFacing, "frontFacing");
        Objects.requireNonNull(port, "port");

        int remaining = steamAmount;
        int drainedTotal = 0;
        int reportedFillTotal = 0;
        int targetAttempts = 0;

        for (Side side : SOURCE_SIDE_ORDER) {
            if (side == Side.DOWN || side == frontFacing || remaining == 0) {
                continue;
            }
            if (!port.hasTarget(side)) {
                continue;
            }

            int requestedAmount = Math.max(1, remaining / 2);
            int offeredAmount = Math.min(requestedAmount, remaining);
            int simulatedAccepted = port.simulateFill(side, offeredAmount);
            if (simulatedAccepted <= 0) {
                continue;
            }

            // The source's real drain is bounded by the current source amount.
            int drainedAmount = Math.min(simulatedAccepted, remaining);
            if (drainedAmount <= 0) {
                continue;
            }

            remaining -= drainedAmount;
            drainedTotal += drainedAmount;
            targetAttempts++;
            reportedFillTotal += port.fill(side, drainedAmount);
        }

        return new Result(
            steamAmount, remaining, drainedTotal, reportedFillTotal, targetAttempts);
    }
}

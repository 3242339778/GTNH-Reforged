package com.gtnh.reforged.legacy.power;

import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Minecraft-independent extraction of GTNH 5.09.54.275
 * {@code gregtech.api.util.AveragePerTickCounter}.
 *
 * <p>All timestamps use the upstream signed 32-bit tick representation.
 * Instances are intended to be accessed from the logical server thread and
 * are not thread-safe.</p>
 */
public final class LegacyAveragePerTickCounter {

    private final long[] values;
    private final int[] timestamps;
    private final IntSupplier tickSupplier;
    private final int period;
    private int currentIndex;
    private double cachedAverage;
    private int lastCacheTick;

    /**
     * Creates a rolling counter driven by the supplied tick source.
     *
     * @param period number of ticks in the averaging window; must be positive
     * @param tickSupplier deterministic or runtime tick source
     */
    public LegacyAveragePerTickCounter(int period, IntSupplier tickSupplier) {
        if (period <= 0) {
            throw new InvalidParameterException("period should be a positive non-zero number");
        }

        this.period = period;
        this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier");
        int size = period + 1;
        this.values = new long[size];
        this.timestamps = new int[size];
    }

    /** Adds to the current tick's sum. Zero values are ignored. */
    public void addValue(long value) {
        if (value == 0) {
            return;
        }

        final int currentTick = tickSupplier.getAsInt();
        if (currentTick == timestamps[currentIndex]) {
            values[currentIndex] += value;
        } else if (currentTick > timestamps[currentIndex]) {
            currentIndex = (currentIndex + 1) % values.length;
            values[currentIndex] = value;
            timestamps[currentIndex] = currentTick;
        }
    }

    /**
     * Records the maximum value observed during the current tick rather than
     * summing multiple observations. Zero values are ignored.
     */
    public void addMaxValue(long value) {
        if (value == 0) {
            return;
        }

        final int currentTick = tickSupplier.getAsInt();
        if (currentTick == timestamps[currentIndex]) {
            values[currentIndex] = Math.max(values[currentIndex], value);
        } else if (currentTick > timestamps[currentIndex]) {
            currentIndex = (currentIndex + 1) % values.length;
            values[currentIndex] = value;
            timestamps[currentIndex] = currentTick;
        }
    }

    /**
     * Returns the average over the preceding {@code period} ticks. The
     * current tick is excluded, matching the upstream implementation.
     */
    public double getAverage() {
        final int currentTick = tickSupplier.getAsInt();
        if (lastCacheTick == currentTick) {
            return cachedAverage;
        }

        if (currentTick < timestamps[currentIndex]) {
            return 0;
        }

        return calculateAverage(currentTick);
    }

    /** Returns the accumulated value for exactly the previous tick, or zero. */
    public long getLast() {
        final int targetTick = tickSupplier.getAsInt() - 1;
        if (timestamps[currentIndex] == targetTick) {
            return values[currentIndex];
        }

        int previousIndex = (currentIndex - 1 + values.length) % values.length;
        if (timestamps[previousIndex] == targetTick) {
            return values[previousIndex];
        }

        return 0;
    }

    public int getPeriod() {
        return period;
    }

    private double calculateAverage(int currentTick) {
        long sum = 0;
        int minimumTick = currentTick - period;
        for (int i = 0; i < values.length; i++) {
            if (timestamps[i] >= minimumTick && timestamps[i] < currentTick) {
                sum += values[i];
            }
        }
        cachedAverage = sum / (double) period;
        lastCacheTick = currentTick;
        return cachedAverage;
    }
}

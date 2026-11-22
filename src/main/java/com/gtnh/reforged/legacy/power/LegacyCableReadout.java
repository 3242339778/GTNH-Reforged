package com.gtnh.reforged.legacy.power;

/** Immutable previous-tick throughput and cable-rating snapshot. */
public record LegacyCableReadout(
        long amps, long maxAmps, long voltage, long maxVoltage,
        long eut, long maxEut, double avgAmps, double avgEut) {
}

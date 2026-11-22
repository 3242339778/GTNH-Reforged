package com.gtnh.reforged.legacy.power;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Minecraft-independent extraction of GTNH 5.09.54.275 PowerNodePath.
 * Retains cable-derived limits, per-tick voltage selection, current decay,
 * overload/fire behavior, and rolling readout counters. Does not implement
 * graph traversal or energy distribution.
 */
public final class LegacyPowerNodePath extends LegacyPowerPath {
    public static final int TICKS_PER_SECOND = 20;
    private static final long OVERLOAD_TICK_MULTIPLIER = 40L;

    private final IntSupplier tickSupplier;
    private final LegacyAveragePerTickCounter avgAmperageCounter;
    private final LegacyAveragePerTickCounter avgVoltageCounter;
    private final LegacyAveragePerTickCounter avgEnergyCounter;

    private long maxAmps;
    private long amps;
    private long loss;
    private long voltage;
    private long maxVoltage;
    private int tick;
    private boolean countUp = true;
    private long currentVoltageAfterLoss;

    public LegacyPowerNodePath(List<? extends LegacyPowerPipeBinding> pipes, IntSupplier tickSupplier) {
        super(pipes);
        this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier");
        this.avgAmperageCounter = new LegacyAveragePerTickCounter(TICKS_PER_SECOND, tickSupplier);
        this.avgVoltageCounter = new LegacyAveragePerTickCounter(TICKS_PER_SECOND, tickSupplier);
        this.avgEnergyCounter = new LegacyAveragePerTickCounter(TICKS_PER_SECOND, tickSupplier);
        processPipes();
    }

    public long getLoss() {
        return loss;
    }

    public void applyVoltage(long inputVoltage, boolean inputCountUp) {
        currentVoltageAfterLoss = Math.max(inputVoltage - loss, 0);
        avgVoltageCounter.addMaxValue(currentVoltageAfterLoss);

        int newTick = tickSupplier.getAsInt();
        if (tick != newTick) {
            reset(newTick - tick);
            tick = newTick;
            voltage = inputVoltage;
            countUp = inputCountUp;
        } else if (countUp != inputCountUp && (inputVoltage - loss) > voltage
                || inputVoltage > voltage) {
            countUp = inputCountUp;
            voltage = inputVoltage;
        }

        if (inputVoltage > maxVoltage) {
            getLock().addOwner(null);
            for (LegacyPowerPipeBinding pipe : getPipes()) {
                if (pipe instanceof LegacyPowerCableAccess cable
                        && cable.getVoltageRating() < voltage && cable.hasBaseTile()) {
                    cable.setToFire();
                }
            }
        }
    }

    private void reset(int ticksPassed) {
        if (ticksPassed < 0 || ticksPassed > 100) {
            amps = 0;
            return;
        }
        amps = Math.max(0, amps - maxAmps * ticksPassed);
    }

    public void addAmps(long addedAmps) {
        addAmps(addedAmps, currentVoltageAfterLoss);
    }

    public void addAmps(long addedAmps, long voltageAfterLoss) {
        avgAmperageCounter.addValue(addedAmps);
        avgEnergyCounter.addValue(addedAmps * Math.max(voltageAfterLoss, 0));
        amps += addedAmps;
        if (amps > maxAmps * OVERLOAD_TICK_MULTIPLIER) {
            getLock().addOwner(null);
            for (LegacyPowerPipeBinding pipe : getPipes()) {
                if (pipe instanceof LegacyPowerCableAccess cable
                        && cable.getAmperageRating() * OVERLOAD_TICK_MULTIPLIER < amps
                        && cable.hasBaseTile()) {
                    cable.setToFire();
                }
            }
        }
    }

    public long getAmperage() { return avgAmperageCounter.getLast(); }
    public long getMaxAmperage() { return maxAmps; }
    public double getAvgAmperage() { return avgAmperageCounter.getAverage(); }

    /** Highest post-loss voltage observed during the previous tick. */
    public long getVoltage() { return avgVoltageCounter.getLast(); }
    public long getMaxVoltage() { return maxVoltage; }
    public double getAvgVoltage() { return avgVoltageCounter.getAverage(); }

    /** Energy transferred during the previous tick, in EU/t. */
    public long getEnergy() { return avgEnergyCounter.getLast(); }
    public double getAvgEnergy() { return avgEnergyCounter.getAverage(); }

    /** Returns a consistent, non-mutating snapshot for scanner/Waila adapters. */
    public LegacyCableReadout getReadout() {
        long maxVoltageAfterLoss = Math.max(0, maxVoltage - loss);
        return new LegacyCableReadout(
                getAmperage(), maxAmps, getVoltage(), maxVoltageAfterLoss,
                getEnergy(), maxVoltageAfterLoss * maxAmps,
                getAvgAmperage(), getAvgEnergy());
    }

    private void processPipes() {
        maxAmps = Integer.MAX_VALUE;
        maxVoltage = Integer.MAX_VALUE;
        loss = 0;
        for (LegacyPowerPipeBinding pipe : getPipes()) {
            if (pipe instanceof LegacyPowerCableAccess cable) {
                maxAmps = Math.min(cable.getAmperageRating(), maxAmps);
                loss += cable.getCableLossPerMeter();
                maxVoltage = Math.min(cable.getVoltageRating(), maxVoltage);
            }
        }
    }
}

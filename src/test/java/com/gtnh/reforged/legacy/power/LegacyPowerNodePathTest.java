package com.gtnh.reforged.legacy.power;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class LegacyPowerNodePathTest {

    @Test
    void derivesMinimumRatingsAndSummedCableLoss() {
        AtomicInteger tick = new AtomicInteger(1);
        FakeCable first = new FakeCable(32, 128, 3);
        FakeCable second = new FakeCable(16, 64, 7);
        LegacyPowerNodePath path = new LegacyPowerNodePath(List.of(first, second), tick::get);

        assertSame(path, first.getNodePath());
        assertSame(path, second.getNodePath());
        assertEquals(16, path.getMaxAmperage());
        assertEquals(64, path.getMaxVoltage());
        assertEquals(10, path.getLoss());
    }

    @Test
    void voltageUsesPostLossMaximumPerTick() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerNodePath path = new LegacyPowerNodePath(
                List.of(new FakeCable(32, 256, 10)), tick::get);

        path.applyVoltage(80, true);  // 70 after loss
        path.applyVoltage(95, true);  // 85 after loss, max for this tick
        path.applyVoltage(90, false); // lower post-loss voltage
        tick.set(2);

        assertEquals(85, path.getVoltage());
        assertEquals(85.0 / 20.0, path.getAvgVoltage());
    }

    @Test
    void oneArgumentAddAmpsUsesMostRecentlyAppliedPostLossVoltage() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerNodePath path = new LegacyPowerNodePath(
                List.of(new FakeCable(32, 256, 12)), tick::get);

        path.applyVoltage(100, true);
        path.addAmps(3);
        tick.set(2);

        assertEquals(3, path.getAmperage());
        assertEquals(264, path.getEnergy()); // 3 * (100 - 12)
    }

    @Test
    void explicitVoltageAddsCurrentAndEnergyCounters() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerNodePath path = new LegacyPowerNodePath(
                List.of(new FakeCable(32, 256, 0)), tick::get);

        path.addAmps(2, 50);
        path.addAmps(3, 40);
        tick.set(2);

        assertEquals(5, path.getAmperage());
        assertEquals(220, path.getEnergy());
        assertEquals(5.0 / 20.0, path.getAvgAmperage());
        assertEquals(220.0 / 20.0, path.getAvgEnergy());
    }

    @Test
    void voltageOverloadLocksPathAndFiresOnlyUnderratedLoadedCables() {
        AtomicInteger tick = new AtomicInteger(1);
        FakeCable weak = new FakeCable(32, 100, 0);
        FakeCable strong = new FakeCable(32, 200, 0);
        FakeCable unloaded = new FakeCable(32, 50, 0);
        unloaded.baseTilePresent = false;
        LegacyPowerNodePath path = new LegacyPowerNodePath(List.of(weak, strong, unloaded), tick::get);

        path.applyVoltage(150, true);

        assertTrue(path.getLock().isLocked());
        assertEquals(1, path.getLock().ownerCount());
        assertTrue(weak.onFire);
        assertFalse(strong.onFire);
        assertFalse(unloaded.onFire);
    }

    @Test
    void amperageOverloadUsesPathAndPerCableFortyTickThresholds() {
        AtomicInteger tick = new AtomicInteger(1);
        FakeCable weak = new FakeCable(5, 256, 0);
        FakeCable strong = new FakeCable(10, 256, 0);
        LegacyPowerNodePath path = new LegacyPowerNodePath(List.of(weak, strong), tick::get);

        path.addAmps(201, 20);

        assertTrue(path.getLock().isLocked());
        assertTrue(weak.onFire);
        assertFalse(strong.onFire);
    }

    @Test
    void currentDecayUsesElapsedTicksAndResetsAfterMoreThanOneHundred() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerNodePath path = new LegacyPowerNodePath(
                List.of(new FakeCable(5, 256, 0)), tick::get);

        path.addAmps(100, 1);
        tick.set(2);
        path.applyVoltage(1, true); // decay by maxAmps * 1 = 5
        path.addAmps(100, 1);       // accumulated internal current = 195
        tick.set(103);
        path.applyVoltage(1, true); // elapsed 101 ticks: hard reset to zero
        path.addAmps(1, 1);         // below overload threshold

        assertFalse(path.getLock().isLocked());
    }

    @Test
    void readoutIsConsistentAndUsesPostLossMaximumVoltage() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyPowerNodePath path = new LegacyPowerNodePath(
                List.of(new FakeCable(8, 120, 5), new FakeCable(4, 100, 7)), tick::get);

        path.applyVoltage(90, true);
        path.addAmps(2, 78);
        tick.set(2);

        LegacyCableReadout readout = path.getReadout();
        assertEquals(2, readout.amps());
        assertEquals(4, readout.maxAmps());
        assertEquals(78, readout.voltage());
        assertEquals(88, readout.maxVoltage()); // min cable rating 100 - total loss 12
        assertEquals(156, readout.eut());
        assertEquals(352, readout.maxEut());
        assertEquals(2.0 / 20.0, readout.avgAmps());
        assertEquals(156.0 / 20.0, readout.avgEut());
    }

    private static final class FakeCable implements LegacyPowerCableAccess {
        private final long amperageRating;
        private final long voltageRating;
        private final long loss;
        private LegacyPowerPath nodePath;
        private boolean baseTilePresent = true;
        private boolean onFire;

        private FakeCable(long amperageRating, long voltageRating, long loss) {
            this.amperageRating = amperageRating;
            this.voltageRating = voltageRating;
            this.loss = loss;
        }

        @Override public long getAmperageRating() { return amperageRating; }
        @Override public long getVoltageRating() { return voltageRating; }
        @Override public long getCableLossPerMeter() { return loss; }
        @Override public boolean hasBaseTile() { return baseTilePresent; }
        @Override public void setToFire() { onFire = true; }
        @Override public LegacyPowerPath getNodePath() { return nodePath; }
        @Override public void setNodePath(LegacyPowerPath path) { nodePath = path; }
    }
}

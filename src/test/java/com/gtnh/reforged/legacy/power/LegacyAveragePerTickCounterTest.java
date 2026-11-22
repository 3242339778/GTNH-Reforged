package com.gtnh.reforged.legacy.power;

import java.security.InvalidParameterException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyAveragePerTickCounterTest {

    @Test
    void rejectsNonPositivePeriods() {
        assertThrows(InvalidParameterException.class,
            () -> new LegacyAveragePerTickCounter(0, () -> 0));
        assertThrows(InvalidParameterException.class,
            () -> new LegacyAveragePerTickCounter(-1, () -> 0));
    }

    @Test
    void rejectsNullTickSupplier() {
        assertThrows(NullPointerException.class,
            () -> new LegacyAveragePerTickCounter(20, null));
    }

    @Test
    void sumsMultipleValuesInTheSameTick() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(20, tick::get);

        counter.addValue(3);
        counter.addValue(5);

        tick.set(2);
        assertEquals(8, counter.getLast());
    }

    @Test
    void maxModeKeepsOnlyTheHighestValueInTheSameTick() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(20, tick::get);

        counter.addMaxValue(120);
        counter.addMaxValue(80);
        counter.addMaxValue(160);

        tick.set(2);
        assertEquals(160, counter.getLast());
    }

    @Test
    void ignoresZeroValues() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(20, tick::get);

        counter.addValue(0);
        counter.addMaxValue(0);

        tick.set(2);
        assertEquals(0, counter.getLast());
    }

    @Test
    void averageUsesPrecedingWindowAndExcludesCurrentTick() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(4, tick::get);

        counter.addValue(8); // tick 1
        tick.set(2);
        counter.addValue(4); // tick 2
        tick.set(3);
        counter.addValue(100); // current tick must be excluded

        assertEquals(3.0, counter.getAverage()); // (8 + 4) / 4
    }

    @Test
    void averageWindowDropsValuesOlderThanPeriod() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(3, tick::get);

        counter.addValue(9); // tick 1
        tick.set(2);
        counter.addValue(6); // tick 2
        tick.set(4);

        assertEquals(2.0, counter.getAverage()); // tick 1 expired; 6 / 3
    }

    @Test
    void getLastReturnsOnlyTheImmediatelyPreviousTick() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(5, tick::get);

        counter.addValue(7);
        tick.set(3);

        assertEquals(0, counter.getLast());
    }

    @Test
    void multipleReadsInOneTickUseCachedAverage() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(2, tick::get);

        counter.addValue(10);
        tick.set(2);
        assertEquals(5.0, counter.getAverage());

        counter.addValue(100);
        assertEquals(5.0, counter.getAverage());
    }

    @Test
    void doesNotAdvanceForBackwardTickMovement() {
        AtomicInteger tick = new AtomicInteger(5);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(5, tick::get);

        counter.addValue(11);
        tick.set(4);
        counter.addValue(7);

        assertEquals(0, counter.getLast());
        tick.set(6);
        assertEquals(11, counter.getLast());
    }

    @Test
    void advancesRingBufferAcrossMultipleTicks() {
        AtomicInteger tick = new AtomicInteger(1);
        LegacyAveragePerTickCounter counter = new LegacyAveragePerTickCounter(2, tick::get);

        counter.addValue(1);
        tick.set(2);
        counter.addValue(2);
        tick.set(3);
        counter.addValue(3);
        tick.set(4);

        assertEquals(2.5, counter.getAverage()); // ticks 2 and 3, divided by 2
        assertEquals(3, counter.getLast());
    }
}

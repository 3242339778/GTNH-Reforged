package com.gtnh.reforged.legacy.boiler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyBoilerFluidResourceHandlerTest {
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);
    private static final FluidResource STEAM = FluidResource.of(Fluids.LAVA);
    private static final String WATER_ID = "minecraft:water";
    private static final String STEAM_ID = "minecraft:lava";

    private static final class MutablePolicy implements LegacyBoilerFluidResourceHandler.AccessPolicy {
        private final AtomicBoolean allowWaterInsert = new AtomicBoolean(true);
        private final AtomicBoolean allowSteamExtract = new AtomicBoolean(true);

        @Override
        public boolean canInsertWater(FluidResource resource) {
            return allowWaterInsert.get();
        }

        @Override
        public boolean canExtractSteam(FluidResource resource) {
            return allowSteamExtract.get();
        }
    }

    private static LegacyBoilerFluidState newState(int waterAmount, int steamAmount) {
        return new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState(
                waterAmount > 0 || waterAmount < 0 ? WATER_ID : null,
                waterAmount,
                -55
            ),
            steamAmount > 0 ? STEAM_ID : null,
            steamAmount
        );
    }

    private static LegacyBoilerFluidResourceHandler newHandler(
        LegacyBoilerFluidState state,
        AtomicInteger commitCallbacks,
        MutablePolicy policy
    ) {
        var journal = new LegacyBoilerFluidStateJournal(state, commitCallbacks::incrementAndGet);
        return new LegacyBoilerFluidResourceHandler(
            journal,
            WATER,
            WATER_ID,
            STEAM,
            STEAM_ID,
            () -> 32000,
            () -> 64000,
            policy
        );
    }

    @Test
    void exposesTwoStableIndicesAndNonNegativeFluidProjections() {
        var state = newState(-1, 500);
        var handler = newHandler(state, new AtomicInteger(), new MutablePolicy());

        assertEquals(2, handler.size());
        assertTrue(handler.getResource(0).isEmpty());
        assertEquals(0, handler.getAmountAsLong(0));
        assertEquals(STEAM, handler.getResource(1));
        assertEquals(500, handler.getAmountAsLong(1));
        assertEquals(32000, handler.getCapacityAsLong(0, WATER));
        assertEquals(64000, handler.getCapacityAsLong(1, STEAM));
    }

    @Test
    void waterInsertSimulationDoesNotMutateAndRootAbortRestoresAggregate() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var handler = newHandler(state, callbacks, new MutablePolicy());

        try (var root = Transaction.openRoot()) {
            assertEquals(25, handler.insert(0, WATER, 25, root));
            assertEquals(125, state.water().legacyAmount());
            assertEquals(500, state.steamAmount());
            // Root aborts.
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(-55, state.water().excessWaterRemainder());
        assertEquals(0, callbacks.get());
    }

    @Test
    void waterInsertCommitsAndUsesLegacyNegativeWindowHeadroom() {
        var state = newState(-1, 0);
        var callbacks = new AtomicInteger();
        var handler = newHandler(state, callbacks, new MutablePolicy());

        try (var root = Transaction.openRoot()) {
            assertEquals(32001, handler.insert(0, WATER, 32001, root));
            assertEquals(32000, state.water().legacyAmount());
            root.commit();
        }

        assertEquals(32000, handler.getAmountAsLong(0));
        assertEquals(1, callbacks.get());
    }

    @Test
    void waterCannotBeExtractedAndSteamCannotBeInserted() {
        var state = newState(100, 500);
        var handler = newHandler(state, new AtomicInteger(), new MutablePolicy());

        try (var root = Transaction.openRoot()) {
            assertEquals(0, handler.extract(0, WATER, 20, root));
            assertEquals(0, handler.insert(1, STEAM, 20, root));
            root.commit();
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
    }

    @Test
    void steamExtractionSimulationAndCommitUseExactAmount() {
        var state = newState(100, 75);
        var callbacks = new AtomicInteger();
        var handler = newHandler(state, callbacks, new MutablePolicy());

        try (var root = Transaction.openRoot()) {
            assertEquals(40, handler.extract(1, STEAM, 40, root));
            assertEquals(35, state.steamAmount());
            root.commit();
        }

        assertEquals(35, handler.getAmountAsLong(1));
        assertEquals(1, callbacks.get());
    }

    @Test
    void steamExtractionRollbackRestoresAmountAndIdentity() {
        var state = newState(100, 75);
        var callbacks = new AtomicInteger();
        var handler = newHandler(state, callbacks, new MutablePolicy());

        try (var root = Transaction.openRoot()) {
            assertEquals(75, handler.extract(1, STEAM, 100, root));
            assertEquals(0, state.steamAmount());
            // Root aborts.
        }

        assertEquals(75, state.steamAmount());
        assertEquals(STEAM_ID, state.steamFluidId());
        assertEquals(0, callbacks.get());
    }

    @Test
    void rejectsWrongResourceIdentityAndOutOfRangeIndices() {
        var state = newState(100, 500);
        var handler = newHandler(state, new AtomicInteger(), new MutablePolicy());

        try (var root = Transaction.openRoot()) {
            assertEquals(0, handler.insert(0, STEAM, 20, root));
            assertEquals(0, handler.extract(1, WATER, 20, root));
            root.commit();
        }

        assertThrows(IndexOutOfBoundsException.class, () -> handler.getResource(2));
        try (var root = Transaction.openRoot()) {
            assertThrows(IndexOutOfBoundsException.class,
                () -> handler.insert(2, WATER, 1, root));
        }
    }

    @Test
    void liveAccessPolicyIsCheckedAtEachMutation() {
        var state = newState(100, 500);
        var policy = new MutablePolicy();
        var callbacks = new AtomicInteger();
        var handler = newHandler(state, callbacks, policy);

        policy.allowWaterInsert.set(false);
        policy.allowSteamExtract.set(false);

        try (var root = Transaction.openRoot()) {
            assertEquals(0, handler.insert(0, WATER, 20, root));
            assertEquals(0, handler.extract(1, STEAM, 20, root));
            root.commit();
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());

        policy.allowWaterInsert.set(true);
        policy.allowSteamExtract.set(true);
        try (var root = Transaction.openRoot()) {
            assertEquals(20, handler.insert(0, WATER, 20, root));
            assertEquals(20, handler.extract(1, STEAM, 20, root));
            root.commit();
        }

        assertEquals(120, state.water().legacyAmount());
        assertEquals(480, state.steamAmount());
        assertEquals(1, callbacks.get());
    }

    @Test
    void zeroAmountOperationsDoNotRegisterJournalOrNotify() {
        var state = newState(100, 500);
        var callbacks = new AtomicInteger();
        var handler = newHandler(state, callbacks, new MutablePolicy());

        try (var root = Transaction.openRoot()) {
            assertEquals(0, handler.insert(0, WATER, 0, root));
            assertEquals(0, handler.extract(1, STEAM, 0, root));
            root.commit();
        }

        assertEquals(100, state.water().legacyAmount());
        assertEquals(500, state.steamAmount());
        assertEquals(0, callbacks.get());
    }

    @Test
    void rejectsNegativeAmountsAndComponentBearingConfiguredResources() {
        var state = newState(100, 500);
        var journal = new LegacyBoilerFluidStateJournal(state, () -> {});
        var policy = new MutablePolicy();

        var handler = new LegacyBoilerFluidResourceHandler(
            journal,
            WATER,
            WATER_ID,
            STEAM,
            STEAM_ID,
            () -> 32000,
            () -> 64000,
            policy
        );
        try (var root = Transaction.openRoot()) {
            assertThrows(IllegalArgumentException.class,
                () -> handler.insert(0, WATER, -1, root));
        }

        var patchedWater = WATER.with(
            net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.network.chat.Component.literal("patched water")
        );
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyBoilerFluidResourceHandler(
                journal,
                patchedWater,
                WATER_ID,
                STEAM,
                STEAM_ID,
                () -> 32000,
                () -> 64000,
                policy
            ));
    }
}

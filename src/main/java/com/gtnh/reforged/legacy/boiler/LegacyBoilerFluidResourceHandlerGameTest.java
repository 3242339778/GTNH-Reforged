package com.gtnh.reforged.legacy.boiler;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Consumer;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Registry-backed GameTests for the NeoForge fluid adapter.
 * These tests run in the GameTest server, not in the plain JUnit environment.
 */
public final class LegacyBoilerFluidResourceHandlerGameTest {
    private static final String WATER_ID = "minecraft:water";
    private static final String STEAM_ID = "minecraft:lava";

    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, "gtnh_reforged");
    private static final String TEST_TRANSFER = "legacy_boiler_fluid_adapter_transactional_transfer";
    private static final String TEST_ACCESS = "legacy_boiler_fluid_adapter_live_access_policy";

    static {
        TEST_FUNCTIONS.register(TEST_TRANSFER, () -> LegacyBoilerFluidResourceHandlerGameTest::adapterViewsAndTransactionalTransfer);
        TEST_FUNCTIONS.register(TEST_ACCESS, () -> LegacyBoilerFluidResourceHandlerGameTest::adapterRespectsLiveAccessPolicyAndResourceIdentity);
    }

    private LegacyBoilerFluidResourceHandlerGameTest() {}

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(LegacyBoilerFluidResourceHandlerGameTest::registerGameTests);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath("gtnh_reforged", "legacy_boiler"),
            new TestEnvironmentDefinition.AllOf()
        );
        registerTest(event, environment, TEST_TRANSFER);
        registerTest(event, environment, TEST_ACCESS);
    }

    private static ResourceKey<Consumer<GameTestHelper>> testFunctionKey(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath("gtnh_reforged", name));
    }

    private static void registerTest(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String name) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
            new TestData<>(environment, Identifier.parse("minecraft:empty"), 100, 0, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath("gtnh_reforged", name),
            new FunctionGameTestInstance(testFunctionKey(name), data));
    }
    public static void adapterViewsAndTransactionalTransfer(GameTestHelper helper) {
        var water = FluidResource.of(new FluidStack(Fluids.WATER, 1));
        var steam = FluidResource.of(new FluidStack(Fluids.LAVA, 1));
        var state = new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState(WATER_ID, 100, -55), STEAM_ID, 500
        );
        var commits = new int[1];
        var journal = new LegacyBoilerFluidStateJournal(state, () -> commits[0]++);
        var handler = new LegacyBoilerFluidResourceHandler(
            journal, water, WATER_ID, steam, STEAM_ID, () -> 1000, () -> 1000,
            new LegacyBoilerFluidResourceHandler.AccessPolicy() {
                @Override
                public boolean canInsertWater(FluidResource resource) {
                    return true;
                }

                @Override
                public boolean canExtractSteam(FluidResource resource) {
                    return true;
                }
            }
        );

        helper.assertTrue(handler.size() == 2, "Expected two indexed fluid inventories");
        helper.assertTrue(handler.getResource(0).equals(water), "Water resource identity mismatch");
        helper.assertTrue(handler.getResource(1).equals(steam), "Steam resource identity mismatch");
        helper.assertTrue(handler.isValid(0, water), "Water slot should accept its configured resource");
        helper.assertTrue(!handler.isValid(0, steam), "Water slot must reject steam");
        helper.assertTrue(handler.isValid(1, steam), "Steam slot should accept its configured resource");
        helper.assertTrue(!handler.isValid(1, water), "Steam slot must reject water");

        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(0, water, 50, tx) == 50, "Water simulation amount mismatch");
            helper.assertTrue(state.water().legacyAmount() == 100, "Simulation mutated water state");
            tx.commit();
        }
        helper.assertTrue(state.water().legacyAmount() == 150, "Committed water fill mismatch");
        helper.assertTrue(commits[0] == 1, "Expected one root commit notification");

        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(handler.extract(1, steam, 100, tx) == 100, "Steam simulation amount mismatch");
            helper.assertTrue(state.steamAmount() == 500, "Steam simulation mutated state");
            // Intentionally abort.
        }
        helper.assertTrue(state.steamAmount() == 500, "Aborted steam transaction changed state");

        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(handler.extract(1, steam, 125, tx) == 125, "Steam extraction mismatch");
            tx.commit();
        }
        helper.assertTrue(state.steamAmount() == 375, "Committed steam extraction amount mismatch");
        helper.assertTrue(commits[0] == 2, "Expected second root commit notification");

        helper.succeed();
    }
    public static void adapterRespectsLiveAccessPolicyAndResourceIdentity(GameTestHelper helper) {
        var water = FluidResource.of(Fluids.WATER);
        var steam = FluidResource.of(Fluids.LAVA);
        var state = new LegacyBoilerFluidState(
            new LegacyBoilerWaterShadowState(WATER_ID, 100, 0), STEAM_ID, 500
        );
        var commits = new int[1];
        var journal = new LegacyBoilerFluidStateJournal(state, () -> commits[0]++);
        var allowed = new boolean[] { false };
        var handler = new LegacyBoilerFluidResourceHandler(
            journal, water, WATER_ID, steam, STEAM_ID, () -> 1000, () -> 1000,
            new LegacyBoilerFluidResourceHandler.AccessPolicy() {
                @Override
                public boolean canInsertWater(FluidResource resource) {
                    return allowed[0];
                }

                @Override
                public boolean canExtractSteam(FluidResource resource) {
                    return allowed[0];
                }
            }
        );

        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(0, water, 20, tx) == 0, "Denied water insertion should return zero");
            helper.assertTrue(handler.extract(1, steam, 20, tx) == 0, "Denied steam extraction should return zero");
            helper.assertTrue(handler.insert(0, steam, 20, tx) == 0, "Wrong resource insertion should return zero");
            helper.assertTrue(handler.extract(1, water, 20, tx) == 0, "Wrong resource extraction should return zero");
            helper.assertTrue(handler.insert(0, water, 0, tx) == 0, "Zero insertion should be a no-op");
            helper.assertTrue(handler.extract(1, steam, 0, tx) == 0, "Zero extraction should be a no-op");
            tx.commit();
        }
        helper.assertTrue(state.water().legacyAmount() == 100, "Denied operations changed water state");
        helper.assertTrue(state.steamAmount() == 500, "Denied operations changed steam state");
        helper.assertTrue(commits[0] == 0, "Denied/no-op operations should not journal or notify");

        allowed[0] = true;
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(0, water, 25, tx) == 25, "Allowed insertion failed");
            tx.commit();
        }
        helper.assertTrue(state.water().legacyAmount() == 125, "Allowed insertion did not mutate water");
        helper.assertTrue(commits[0] == 1, "Allowed mutation should notify once");

        helper.succeed();
    }
}

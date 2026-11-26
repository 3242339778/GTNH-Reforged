package com.gtnh.reforged.legacy.boiler;

import java.util.Objects;
import java.util.function.IntSupplier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Two-index NeoForge fluid view over one authoritative legacy boiler aggregate.
 *
 * <p>Index 0 is the water input and index 1 is the steam output. All views must
 * share the same journal. The access policy is evaluated for every mutation,
 * so a direction-specific view can reflect live machine, side, and Cover gates.</p>
 *
 * <p>The current legacy aggregate stores fluid registry identities as strings,
 * not arbitrary data-component patches. Consequently this adapter accepts only
 * component-patch-empty configured resources. Incoming resources are compared
 * by full FluidResource equality, so component-bearing variants are rejected
 * rather than silently collapsed to a registry ID.</p>
 */
public final class LegacyBoilerFluidResourceHandler implements ResourceHandler<FluidResource> {
    public static final int WATER_INDEX = 0;
    public static final int STEAM_INDEX = 1;
    public static final int TANK_COUNT = 2;

    private final LegacyBoilerFluidStateJournal journal;
    private final LegacyBoilerFluidTransferCore transferCore;
    private final FluidResource waterResource;
    private final String waterFluidId;
    private final FluidResource steamResource;
    private final String steamFluidId;
    private final IntSupplier waterCapacity;
    private final IntSupplier steamCapacity;
    private final AccessPolicy accessPolicy;

    public LegacyBoilerFluidResourceHandler(
        LegacyBoilerFluidStateJournal journal,
        FluidResource waterResource,
        String waterFluidId,
        FluidResource steamResource,
        String steamFluidId,
        IntSupplier waterCapacity,
        IntSupplier steamCapacity,
        AccessPolicy accessPolicy
    ) {
        this.journal = Objects.requireNonNull(journal, "journal");
        this.waterResource = requirePlainResource(waterResource, "waterResource");
        this.waterFluidId = requireFluidId(waterFluidId, "waterFluidId");
        this.steamResource = requirePlainResource(steamResource, "steamResource");
        this.steamFluidId = requireFluidId(steamFluidId, "steamFluidId");
        this.waterCapacity = Objects.requireNonNull(waterCapacity, "waterCapacity");
        this.steamCapacity = Objects.requireNonNull(steamCapacity, "steamCapacity");
        this.transferCore = new LegacyBoilerFluidTransferCore(
            journal.state(), this.waterFluidId, this.steamFluidId,
            this.waterCapacity, this.steamCapacity
        );
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
    }

    private static FluidResource requirePlainResource(FluidResource resource, String name) {
        Objects.requireNonNull(resource, name);
        if (resource.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (!resource.isComponentsPatchEmpty()) {
            throw new IllegalArgumentException(
                name + " must not contain data-component patches; the legacy state stores registry IDs only");
        }
        return resource;
    }

    private static String requireFluidId(String fluidId, String name) {
        Objects.requireNonNull(fluidId, name);
        if (fluidId.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return fluidId;
    }

    @Override
    public int size() {
        return TANK_COUNT;
    }

    @Override
    public FluidResource getResource(int index) {
        return switch (Objects.checkIndex(index, TANK_COUNT)) {
            case WATER_INDEX -> {
                String exposedId = journal.state().water().exposedFluidId();
                if (exposedId == null) {
                    yield FluidResource.EMPTY;
                }
                requireExpectedIdentity(exposedId, waterFluidId, "water");
                yield waterResource;
            }
            case STEAM_INDEX -> {
                String exposedId = journal.state().exposedSteamFluidId();
                if (exposedId == null) {
                    yield FluidResource.EMPTY;
                }
                requireExpectedIdentity(exposedId, steamFluidId, "steam");
                yield steamResource;
            }
            default -> throw new AssertionError("validated index was outside the fixed tank range");
        };
    }

    @Override
    public long getAmountAsLong(int index) {
        return switch (Objects.checkIndex(index, TANK_COUNT)) {
            case WATER_INDEX -> {
                String exposedId = journal.state().water().exposedFluidId();
                if (exposedId == null) {
                    yield 0;
                }
                requireExpectedIdentity(exposedId, waterFluidId, "water");
                yield journal.state().water().exposedAmount();
            }
            case STEAM_INDEX -> {
                String exposedId = journal.state().exposedSteamFluidId();
                if (exposedId == null) {
                    yield 0;
                }
                requireExpectedIdentity(exposedId, steamFluidId, "steam");
                yield journal.state().exposedSteamAmount();
            }
            default -> throw new AssertionError("validated index was outside the fixed tank range");
        };
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, TANK_COUNT);
        Objects.requireNonNull(resource, "resource");

        if (resource.isEmpty()) {
            return nonNegativeCapacity(index);
        }
        if (index == WATER_INDEX && waterResource.equals(resource)) {
            return nonNegativeCapacity(index);
        }
        if (index == STEAM_INDEX && steamResource.equals(resource)) {
            return nonNegativeCapacity(index);
        }
        return 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, TANK_COUNT);
        if (resource == null || resource.isEmpty()) {
            return false;
        }
        return index == WATER_INDEX && waterResource.equals(resource)
            || index == STEAM_INDEX && steamResource.equals(resource);
    }

    @Override
    public int insert(
        int index,
        FluidResource resource,
        int amount,
        TransactionContext transaction
    ) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Objects.checkIndex(index, TANK_COUNT);
        Objects.requireNonNull(transaction, "transaction");

        if (index != WATER_INDEX || !waterResource.equals(resource)) {
            return 0;
        }
        return transferCore.insertWater(
            waterFluidId,
            amount,
            () -> accessPolicy.canInsertWater(resource),
            () -> journal.beforeMutation(transaction)
        );
    }

    @Override
    public int extract(
        int index,
        FluidResource resource,
        int amount,
        TransactionContext transaction
    ) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Objects.checkIndex(index, TANK_COUNT);
        Objects.requireNonNull(transaction, "transaction");

        if (index != STEAM_INDEX || !steamResource.equals(resource)) {
            return 0;
        }
        return transferCore.extractSteam(
            steamFluidId,
            amount,
            () -> accessPolicy.canExtractSteam(resource),
            () -> journal.beforeMutation(transaction)
        );
    }

    private int nonNegativeCapacity(int index) {
        int capacity = index == WATER_INDEX
            ? waterCapacity.getAsInt()
            : steamCapacity.getAsInt();
        return Math.max(0, capacity);
    }

    private static void requireExpectedIdentity(String actual, String expected, String tank) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                "Legacy boiler " + tank + " state contains " + actual
                    + " but its handler is configured for " + expected);
        }
    }

    /**
     * Operation-specific live gates for one handler view. Machine-wide,
     * physical-side, and Cover checks should be composed by the caller.
     */
    public interface AccessPolicy {
        boolean canInsertWater(FluidResource resource);

        boolean canExtractSteam(FluidResource resource);
    }
}

package com.gtnh.reforged.legacy.boiler;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Pure Java tick calculation for the audited Advanced Boiler mechanics.
 * Steam inventory transfer remains an integration-side operation.
 */
public final class LegacyAdvancedBoilerTickEngine {
    private LegacyAdvancedBoilerTickEngine() {}

    /**
     * Calculates one tick from a snapshot. The source boiler skips all tick
     * processing through tick 20. previousBoiling preserves the source behavior
     * where high temperature with invalid/absent water does not clear the flag;
     * previousActive preserves the machine state because the early return does not update it.
     */
    public static LegacyAdvancedBoilerTickResult calculate(
        LegacyAdvancedBoilerTickSnapshot input, boolean previousBoiling, boolean previousActive
    ) {
        return calculate(input, previousBoiling, previousActive, amount -> amount);
    }

    /**
     * Calculates one tick while allowing the integration layer to push the
     * steam that already exists in the tank.
     *
     * <p>The callback runs after cooldown and before steam production, matching
     * {@code MTEBoiler.onPostTick}: old steam is pushed before this tick can
     * create additional steam. It is not called for an empty tank or during the
     * startup early return. The callback must return the amount remaining in
     * the source tank and must not increase the offered amount.</p>
     */
    public static LegacyAdvancedBoilerTickResult calculate(
        LegacyAdvancedBoilerTickSnapshot input,
        boolean previousBoiling,
        boolean previousActive,
        IntUnaryOperator pushExistingSteam
    ) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(pushExistingSteam, "pushExistingSteam");
        int temperature = input.temperature();
        int lossTimer = input.lossTimer();
        int energy = input.processingEnergy();
        int fuelMaxEnergy = input.fuelMaxEnergy();
        int water = input.waterAmount();
        int steam = input.steamAmount();
        int remainder = input.excessWaterRemainder();

        // Source onPostTick returns before cooldown and all other operations at tick <= 20.
        if (input.tick() <= 20) {
            return new LegacyAdvancedBoilerTickResult(
                temperature, lossTimer, energy, fuelMaxEnergy, water, steam, remainder,
                0, 0, 0, false, previousActive, previousBoiling, false);
        }

        // 1. Cooldown
        if (temperature <= 20) {
            temperature = 20;
            lossTimer = 0;
        } else {
            lossTimer++;
            if (lossTimer > input.cooldownInterval()) {
                temperature--;
                lossTimer = 0;
            }
        }

        // 2. Push the steam that existed at tick start before producing more.
        // This callback represents MTEBoiler.pushSteamToInventories.
        if (steam > 0) {
            int remainingAfterPush = pushExistingSteam.applyAsInt(steam);
            if (remainingAfterPush < 0 || remainingAfterPush > steam) {
                throw new IllegalStateException(
                    "steam output callback must return an amount between zero and the offered amount");
            }
            steam = remainingAfterPush;
        }

        // 3. Steam production.
        int steamProduced = 0;
        int waterConsumed = 0;
        boolean hadValidWaterAtProductionGate = input.waterIsValid() && water > 0;
        if (input.tick() % 10 == 0 && temperature > 100 && hadValidWaterAtProductionGate) {
            steamProduced = input.productionPerSecond() / 2;
            int conversion = remainder - steamProduced;
            if (conversion < 0) {
                waterConsumed = (-conversion) / LegacyAdvancedBoilerSteamModel.STEAM_PER_WATER;
                water -= waterConsumed;
                conversion += waterConsumed * LegacyAdvancedBoilerSteamModel.STEAM_PER_WATER;
            }
            remainder = conversion;
            steam = input.steamIsValid() ? steam + steamProduced : steamProduced;
        }

        // Boiling flag uses the same pre-production water-validity gate as source code.
        boolean boiling = previousBoiling;
        if (input.tick() % 10 == 0) {
            if (temperature > 100 && hadValidWaterAtProductionGate) boiling = true;
            else if (temperature <= 100) boiling = false;
        }

        // 4. Overflow check is against getSteamCapacity(), not the water capacity.
        boolean vented = steam > input.steamCapacity();
        if (vented) steam = (input.steamCapacity() * 3) / 4;

        // 5. Fuel update occurs only when energy is empty and machine is allowed.
        boolean fuelInjectionRequested = false;
        int burnTimeConsumed = 0;
        if (energy <= 0 && input.machineAllowedToWork()
            && input.burnTimeAvailable() > 0 && temperature <= 101) {
            int burnTime = input.burnTimeAvailable();
            energy += burnTime / 10;
            temperature += burnTime / 500;
            fuelMaxEnergy = Math.max(fuelMaxEnergy, energy);
            fuelInjectionRequested = true;
            burnTimeConsumed = burnTime;
        }

        // 6. Heat-up calculation. Source cadence is tick % 10.
        boolean active = energy > 0;
        if (input.tick() % input.heatUpRate() == 0
            && temperature < input.maxTemperature() && energy > 0) {
            energy -= input.heatUpEnergyCost();
            temperature += input.heatUpAmount();
            if (energy <= 0) fuelMaxEnergy = 0;
            active = energy > 0;
        } else if (energy <= 0) {
            fuelMaxEnergy = 0;
            active = false;
        }

        return new LegacyAdvancedBoilerTickResult(
            temperature, lossTimer, energy, fuelMaxEnergy,
            water, steam, remainder, steamProduced, waterConsumed, burnTimeConsumed,
            fuelInjectionRequested, active, boiling, vented);
    }
}

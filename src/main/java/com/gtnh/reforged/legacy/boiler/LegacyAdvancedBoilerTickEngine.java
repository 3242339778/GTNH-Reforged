package com.gtnh.reforged.legacy.boiler;

/**
 * Pure Java tick calculation for the audited Advanced Boiler mechanics.
 * Steam inventory transfer remains an integration-side operation.
 */
public final class LegacyAdvancedBoilerTickEngine {
    private LegacyAdvancedBoilerTickEngine() {}

    /**
     * Calculates one tick from a snapshot. previousBoiling preserves the source
     * behavior where high temperature with invalid/absent water does not clear
     * the boiling flag.
     */
    public static LegacyAdvancedBoilerTickResult calculate(
        LegacyAdvancedBoilerTickSnapshot input, boolean previousBoiling
    ) {
        int temperature = input.temperature();
        int lossTimer = input.lossTimer();
        int energy = input.processingEnergy();
        int fuelMaxEnergy = input.fuelMaxEnergy();
        int water = input.waterAmount();
        int steam = input.steamAmount();
        int remainder = input.excessWaterRemainder();

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

        // 2. Steam production (inventory push is deliberately external).
        int steamProduced = 0;
        int waterConsumed = 0;
        if (input.tick() % 10 == 0 && temperature > 100
            && input.waterIsValid() && water > 0) {
            steamProduced = input.productionPerSecond() / 2;
            int conversion = remainder - steamProduced;
            if (conversion < 0) {
                waterConsumed = (-conversion) / LegacyAdvancedBoilerSteamModel.STEAM_PER_WATER;
                water -= waterConsumed;
                conversion += waterConsumed * LegacyAdvancedBoilerSteamModel.STEAM_PER_WATER;
            }
            remainder = conversion;
            steam += steamProduced;
        }

        // Boiling state is updated in this source phase, before fuel injection and heat-up.
        boolean boiling = previousBoiling;
        if (input.tick() % 10 == 0) {
            if (temperature > 100 && input.waterIsValid() && water > 0) boiling = true;
            else if (temperature <= 100) boiling = false;
        }

        // 3. Overflow behavior uses getCapacity() (waterCapacity), not steamCapacity.
        boolean vented = steam > input.waterCapacity();
        if (vented) steam = (input.waterCapacity() * 3) / 4;

        // 4. Fuel update occurs only when energy is empty and machine is allowed.
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

        // 5. Heat-up calculation. Source cadence is tick % 10.
        boolean active = energy > 0;
        if (input.tick() % input.heatUpRate() == 0
            && temperature < input.maxTemperature() && energy > 0) {
            energy -= input.heatUpEnergyCost();
            temperature += input.heatUpAmount();
            if (energy <= 0) fuelMaxEnergy = 0;
            active = energy > 0;
        }

        return new LegacyAdvancedBoilerTickResult(
            temperature, lossTimer, energy, fuelMaxEnergy,
            water, steam, remainder, steamProduced, waterConsumed, burnTimeConsumed,
            fuelInjectionRequested, active, boiling, vented);
    }
}

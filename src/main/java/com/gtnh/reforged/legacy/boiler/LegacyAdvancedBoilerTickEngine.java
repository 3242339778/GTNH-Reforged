package com.gtnh.reforged.legacy.boiler;

/**
 * Pure Java tick calculation for the audited Advanced Boiler mechanics.
 * Steam inventory transfer remains an integration-side operation.
 */
public final class LegacyAdvancedBoilerTickEngine {
    private LegacyAdvancedBoilerTickEngine() {}

    /**
     * Calculates one tick from a snapshot. The source boiler skips all tick
     * processing through tick 20. previousBoiling preserves the source behavior
     * where high temperature with invalid/absent water does not clear the flag.
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

        // Source onPostTick returns before cooldown and all other operations at tick <= 20.
        if (input.tick() <= 20) {
            return new LegacyAdvancedBoilerTickResult(
                temperature, lossTimer, energy, fuelMaxEnergy, water, steam, remainder,
                0, 0, 0, false, energy > 0, previousBoiling, false);
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

        // 2. Steam production (inventory push is deliberately external).
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
            steam += steamProduced;
        }

        // Boiling flag uses the same pre-production water-validity gate as source code.
        boolean boiling = previousBoiling;
        if (input.tick() % 10 == 0) {
            if (temperature > 100 && hadValidWaterAtProductionGate) boiling = true;
            else if (temperature <= 100) boiling = false;
        }

        // 3. Overflow check is against getSteamCapacity(), not the water capacity.
        boolean vented = steam > input.steamCapacity();
        if (vented) steam = (input.steamCapacity() * 3) / 4;

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

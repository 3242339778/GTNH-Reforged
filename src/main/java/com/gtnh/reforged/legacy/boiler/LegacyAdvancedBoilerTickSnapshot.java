package com.gtnh.reforged.legacy.boiler;

/**
 * Immutable, game-API-free input snapshot for one Advanced Boiler server tick.
 *
 * <p>The machine adapter is responsible for resolving fluid/fuel validity and
 * copying primitive state into this object.</p>
 */
public final class LegacyAdvancedBoilerTickSnapshot {
    private final long tick;
    private final int temperature, maxTemperature, lossTimer, cooldownInterval;
    private final int processingEnergy, fuelMaxEnergy, waterAmount, steamAmount;
    private final boolean waterIsValid, steamIsValid, machineAllowedToWork;
    private final int waterCapacity, steamCapacity, excessWaterRemainder;
    private final int burnTimeAvailable, productionPerSecond, heatUpRate, heatUpEnergyCost, heatUpAmount;

    public LegacyAdvancedBoilerTickSnapshot(
        long tick,
        int temperature,
        int maxTemperature,
        int lossTimer,
        int cooldownInterval,
        int processingEnergy,
        int fuelMaxEnergy,
        int waterAmount,
        int steamAmount,
        boolean waterIsValid,
        boolean steamIsValid,
        boolean machineAllowedToWork,
        int waterCapacity,
        int steamCapacity,
        int excessWaterRemainder,
        int burnTimeAvailable,
        int productionPerSecond,
        int heatUpRate,
        int heatUpEnergyCost,
        int heatUpAmount
    ) {
        if (tick < 0) throw new IllegalArgumentException("tick must be non-negative");
        if (maxTemperature < 0 || lossTimer < 0 || cooldownInterval < 0) throw new IllegalArgumentException("temperature limit and cooldown values must be non-negative");
        if (processingEnergy < 0 || fuelMaxEnergy < 0 || steamAmount < 0) throw new IllegalArgumentException("energy and steam amounts must be non-negative");
        // GT5U's produceSteam can subtract more water than is available, yielding a negative FluidStack amount.
        if (waterCapacity < 0 || steamCapacity < 0 || burnTimeAvailable < 0 || productionPerSecond < 0) throw new IllegalArgumentException("capacities and production values must be non-negative");
        if (heatUpRate <= 0 || heatUpEnergyCost < 0 || heatUpAmount < 0) throw new IllegalArgumentException("heat-up rate must be positive and heat-up values non-negative");
        if (waterAmount > waterCapacity) throw new IllegalArgumentException("water amount exceeds snapshot capacity");
        this.tick=tick; this.temperature=temperature; this.maxTemperature=maxTemperature;
        this.lossTimer=lossTimer; this.cooldownInterval=cooldownInterval;
        this.processingEnergy=processingEnergy; this.fuelMaxEnergy=fuelMaxEnergy;
        this.waterAmount=waterAmount; this.steamAmount=steamAmount;
        this.waterIsValid=waterIsValid; this.steamIsValid=steamIsValid;
        this.machineAllowedToWork=machineAllowedToWork; this.waterCapacity=waterCapacity;
        this.steamCapacity=steamCapacity; this.excessWaterRemainder=excessWaterRemainder;
        this.burnTimeAvailable=burnTimeAvailable; this.productionPerSecond=productionPerSecond;
        this.heatUpRate=heatUpRate; this.heatUpEnergyCost=heatUpEnergyCost; this.heatUpAmount=heatUpAmount;
    }

    public long tick(){return tick;}
    public int temperature(){return temperature;}
    public int maxTemperature(){return maxTemperature;}
    public int lossTimer(){return lossTimer;}
    public int cooldownInterval(){return cooldownInterval;}
    public int processingEnergy(){return processingEnergy;}
    public int fuelMaxEnergy(){return fuelMaxEnergy;}
    public int waterAmount(){return waterAmount;}
    public int steamAmount(){return steamAmount;}
    public boolean waterIsValid(){return waterIsValid;}
    public boolean steamIsValid(){return steamIsValid;}
    public boolean machineAllowedToWork(){return machineAllowedToWork;}
    public int waterCapacity(){return waterCapacity;}
    public int steamCapacity(){return steamCapacity;}
    public int excessWaterRemainder(){return excessWaterRemainder;}
    public int burnTimeAvailable(){return burnTimeAvailable;}
    public int productionPerSecond(){return productionPerSecond;}
    public int heatUpRate(){return heatUpRate;}
    public int heatUpEnergyCost(){return heatUpEnergyCost;}
    public int heatUpAmount(){return heatUpAmount;}
}

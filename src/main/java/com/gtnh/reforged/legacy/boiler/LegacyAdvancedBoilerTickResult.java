package com.gtnh.reforged.legacy.boiler;

/**
 * Immutable calculation output for a future Advanced Boiler tick orchestrator.
 * This DTO describes state and requested effects; it does not perform game actions.
 */
public final class LegacyAdvancedBoilerTickResult {
    private final int temperature, lossTimer, processingEnergy, fuelMaxEnergy;
    private final int waterAmount, steamAmount, excessWaterRemainder;
    private final int steamProducedThisTick, waterConsumedThisTick, burnTimeConsumed;
    private final boolean fuelInjectionRequested, active, boiling, vented;

    public LegacyAdvancedBoilerTickResult(
        int temperature, int lossTimer, int processingEnergy, int fuelMaxEnergy,
        int waterAmount, int steamAmount, int excessWaterRemainder,
        int steamProducedThisTick, int waterConsumedThisTick, int burnTimeConsumed,
        boolean fuelInjectionRequested, boolean active, boolean boiling, boolean vented
    ) {
        if (lossTimer < 0 || processingEnergy < 0 || fuelMaxEnergy < 0 || waterAmount < 0 || steamAmount < 0
            || steamProducedThisTick < 0 || waterConsumedThisTick < 0 || burnTimeConsumed < 0) {
            throw new IllegalArgumentException("result counters and amounts must be non-negative");
        }
        this.temperature=temperature; this.lossTimer=lossTimer;
        this.processingEnergy=processingEnergy; this.fuelMaxEnergy=fuelMaxEnergy;
        this.waterAmount=waterAmount; this.steamAmount=steamAmount;
        this.excessWaterRemainder=excessWaterRemainder;
        this.steamProducedThisTick=steamProducedThisTick; this.waterConsumedThisTick=waterConsumedThisTick;
        this.burnTimeConsumed=burnTimeConsumed; this.fuelInjectionRequested=fuelInjectionRequested;
        this.active=active; this.boiling=boiling; this.vented=vented;
    }

    public int temperature(){return temperature;}
    public int lossTimer(){return lossTimer;}
    public int processingEnergy(){return processingEnergy;}
    public int fuelMaxEnergy(){return fuelMaxEnergy;}
    public int waterAmount(){return waterAmount;}
    public int steamAmount(){return steamAmount;}
    public int excessWaterRemainder(){return excessWaterRemainder;}
    public int steamProducedThisTick(){return steamProducedThisTick;}
    public int waterConsumedThisTick(){return waterConsumedThisTick;}
    public int burnTimeConsumed(){return burnTimeConsumed;}
    public boolean fuelInjectionRequested(){return fuelInjectionRequested;}
    public boolean active(){return active;}
    public boolean boiling(){return boiling;}
    public boolean vented(){return vented;}
}

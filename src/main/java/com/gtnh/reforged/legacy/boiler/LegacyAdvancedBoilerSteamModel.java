package com.gtnh.reforged.legacy.boiler;

/** Source-audited pure-Java model of selected Advanced Boiler mechanics. */
public final class LegacyAdvancedBoilerSteamModel {
 public static final int STEAM_PER_WATER=160;
 public static final int HEAT_UP_RATE=10;
 public static final int HEAT_UP_ENERGY_COST=2;
 private final int tier;
 private int excessWater;
 private long steamProduced;
 public LegacyAdvancedBoilerSteamModel(int tier){if(tier<=0)throw new IllegalArgumentException("tier must be positive");this.tier=tier;}
 /** Returns steam added on this tick. Production requires tick%10==0, temperature>100 and water>0. */
 public int productionOnTick(long tick,int temperature,int waterAmount){if(tick%10!=0||temperature<=100||waterAmount<=0)return 0;int amount=750*tier/2;steamProduced+=amount;return amount;}
 /** Applies the audited integer water remainder arithmetic. */
 public WaterConversion convertSteamToWater(int steamAmount,int waterAmount){int remainder=excessWater-steamAmount;int consumed=0;if(remainder<0){consumed=(-remainder)/STEAM_PER_WATER;waterAmount-=consumed;remainder+=consumed*STEAM_PER_WATER;}excessWater=remainder;return new WaterConversion(waterAmount,consumed,excessWater);}
 /** Fuel injection eligibility is gated by temperature <= 101. */
 public static FuelInjection consumeFuel(int temperature,int burnTime,int processingEnergy,int fuelMaxEnergy){if(burnTime<=0||temperature>101)return new FuelInjection(temperature,processingEnergy,fuelMaxEnergy,false);int addedEnergy=burnTime/10;int energy=processingEnergy+addedEnergy;return new FuelInjection(temperature+burnTime/500,energy,Math.max(fuelMaxEnergy,energy),true);}
 /** Heat-up work occurs on heat-up cadence, below max temperature and with positive stored energy. */
 public static HeatUpResult heatUpTick(long tick,int temperature,int maxTemperature,int processingEnergy,int fuelMaxEnergy){if(tick%HEAT_UP_RATE!=0||temperature>=maxTemperature||processingEnergy<=0)return new HeatUpResult(temperature,processingEnergy,fuelMaxEnergy,false,false);int energy=processingEnergy-HEAT_UP_ENERGY_COST;int newMaxEnergy=energy<=0?0:fuelMaxEnergy;return new HeatUpResult(temperature+1,energy,newMaxEnergy,true,energy>0);}
 /** Only overflow strictly above capacity resets to three quarters capacity. */
 public static int ventIfOverCapacity(int steamAmount,int capacity){return steamAmount>capacity?(capacity*3)/4:steamAmount;}
 /** Counter increments and cools only when counter exceeds interval. */
 public static CooldownResult cooldownTick(int temperature,int lossTimer,int interval){if(temperature<=20)return new CooldownResult(20,0,false);lossTimer++;if(lossTimer>interval)return new CooldownResult(temperature-1,0,true);return new CooldownResult(temperature,lossTimer,false);}
 public long steamProduced(){return steamProduced;} public int excessWater(){return excessWater;}
 public record WaterConversion(int remainingWater,int waterConsumed,int remainder){}
 public record FuelInjection(int temperature,int processingEnergy,int fuelMaxEnergy,boolean injected){}
 public record HeatUpResult(int temperature,int processingEnergy,int fuelMaxEnergy,boolean heated,boolean active){}
 public record CooldownResult(int temperature,int lossTimer,boolean cooled){}
}

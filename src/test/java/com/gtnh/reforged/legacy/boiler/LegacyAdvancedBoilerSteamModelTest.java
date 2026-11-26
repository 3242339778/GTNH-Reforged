package com.gtnh.reforged.legacy.boiler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LegacyAdvancedBoilerSteamModelTest {
 @Test void tierOneProduces375EveryTenTicksWhenHotAndWet(){var m=new LegacyAdvancedBoilerSteamModel(1);assertEquals(0,m.productionOnTick(9,101,1));assertEquals(375,m.productionOnTick(10,101,1));assertEquals(0,m.productionOnTick(11,101,1));assertEquals(375,m.productionOnTick(20,101,1));}
 @Test void temperatureAndWaterGateProduction(){var m=new LegacyAdvancedBoilerSteamModel(1);assertEquals(0,m.productionOnTick(10,100,10));assertEquals(0,m.productionOnTick(20,101,0));assertEquals(375,m.productionOnTick(30,101,1));}
 @Test void tierOutputScales(){assertEquals(750,new LegacyAdvancedBoilerSteamModel(2).productionOnTick(10,101,1));assertEquals(1125,new LegacyAdvancedBoilerSteamModel(3).productionOnTick(10,101,1));}
 @Test void conversionCarriesRemainder(){var m=new LegacyAdvancedBoilerSteamModel(1);var a=m.convertSteamToWater(375,10);assertEquals(2,a.waterConsumed());assertEquals(-55,a.remainder());var b=m.convertSteamToWater(375,a.remainingWater());assertEquals(2,b.waterConsumed());assertEquals(-110,b.remainder());}
 @Test void ventStrictlyAboveCapacity(){assertEquals(100,LegacyAdvancedBoilerSteamModel.ventIfOverCapacity(100,100));assertEquals(75,LegacyAdvancedBoilerSteamModel.ventIfOverCapacity(101,100));}
 @Test void cooldownCounterMustExceedInterval(){var a=LegacyAdvancedBoilerSteamModel.cooldownTick(150,40,40);assertTrue(a.cooled());assertEquals(149,a.temperature());assertEquals(0,a.lossTimer());var b=LegacyAdvancedBoilerSteamModel.cooldownTick(150,39,40);assertFalse(b.cooled());assertEquals(40,b.lossTimer());}
 @Test void atOrBelowTwentyResets(){var r=LegacyAdvancedBoilerSteamModel.cooldownTick(20,8,40);assertEquals(20,r.temperature());assertEquals(0,r.lossTimer());assertFalse(r.cooled());}
}

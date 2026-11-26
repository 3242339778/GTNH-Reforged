# Advanced Boiler Pure-Java Steam Model & Verification Matrix

**Reference:** GT5-Unofficial 5.09.54.277 Advanced Boiler source audit  
**Status:** Extended pure-Java model and test cases authored; local Gradle verification pending for this extension.

## Scope

This narrow model captures source-audited selected behavior:
- Steam batch is 750 × tier / 2 on ticks divisible by 10, gated by temperature >100 and positive water.
- Steam/water remainder arithmetic using 160 steam per water, including the original signed integer remainder semantics.
- Vent threshold is strictly greater than capacity; reset amount is 3/4 capacity.
- Cooldown increments the counter and cools only when counter > interval (default 40 means 41 counter ticks).
- Fuel injection is eligible only at temperature <=101 and for positive burn time. Processing energy added is burnTime / 10; temperature added is burnTime / 500; fuelMaxEnergy tracks the maximum stored processing energy.
- Heat-up checks occur on ticks divisible by 10; require temperature below maximum and positive processing energy; subtract 2 processing energy and add 1 temperature. Exhaustion clears fuelMaxEnergy and active state.

## Verification matrix

| Case | Expected |
|---|---|
| Tick not divisible by 10 | no steam production |
| Temperature exactly 100 | no production |
| No water | no production |
| Tier 1 qualifying tick | 375 steam |
| Tier 2 qualifying tick | 750 steam |
| Tier 3 qualifying tick | 1125 steam |
| Conversion after 375 steam | consumes 2 water; remainder is -55 |
| Repeated 375 steam conversion | remainder progresses to -110 after second call |
| Steam == capacity | no vent/reset |
| Steam > capacity | reset to 75% |
| Cooldown counter reaches interval | no cooling yet |
| Cooldown counter exceeds interval | temperature decremented and timer reset |
| Temperature <=20 | temperature and timer reset |
| Fuel at temperature 102 | no injection |
| Fuel at temperature 101 | injection allowed |
| Fuel energy contribution | burnTime / 10 |
| Fuel temperature contribution | burnTime / 500 |
| Heat-up off cadence / no energy / at maximum | no heat-up |
| Qualifying heat-up tick | energy -2; temperature +1 |
| Heat-up energy exhaustion | fuelMaxEnergy reset; active false |

## Signed conversion remainder compatibility

The audited implementation subtracts the steam amount from the accumulator, then when the accumulator is negative calculates the water consumption with Java integer division (`-remainder / 160`) and adds the consumed-water multiple back. Java integer division truncates toward zero. For 375 steam, this yields 2 water and a remainder of -55—not a normalized positive remainder of 5. Subsequent calls must preserve this signed accumulator behavior. This is treated as compatibility behavior, not corrected arithmetic.

## Limitations

This remains a selected-mechanics harness, not a complete boiler tick simulation. It does not yet model fuel-slot decrement/ash RNG, fuel lookup/fallback, complete parent tick ordering, actual fluid movement and output directions, steam tank pressure/vent integration, persistent machine state, or world/client synchronization. These must be audited and implemented separately rather than inferred from these tests.

This is source analysis and pure-Java test verification, not an in-game reproduction.
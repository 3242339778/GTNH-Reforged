# Advanced Boiler Pure-Java Steam Model & Verification Matrix

**Reference:** GT5-Unofficial 5.09.54.277 Advanced Boiler source audit  
**Status:** Model authored; focused test and full local Gradle test suite passed on 2026-11-26.

## Scope

This narrow model captures source-audited behavior:
- Production batch is 750 × tier / 2 on ticks divisible by 10, gated by temperature >100 and positive water.
- Steam/water remainder arithmetic using 160 steam per water.
- Vent threshold is strictly greater than capacity; reset amount is 3/4 capacity.
- Cooldown increments the counter and cools only when counter > interval (default 40 means 41 counter ticks).

Not modeled: full fuel/inventory logic, actual fluid transfer, heat-up cadence, world tick integration, persistence. Tests must not imply these are covered.

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
| Counter reaches interval | no cooling yet |
| Counter exceeds interval | temperature decremented and timer reset |
| Temperature <=20 | temperature and timer reset |

## Signed conversion remainder compatibility

The audited implementation subtracts the steam amount from the accumulator, then when the accumulator is negative calculates the water consumption with Java integer division (`-remainder / 160`) and adds the consumed-water multiple back. Java integer division truncates toward zero. For 375 steam, this yields 2 water and a remainder of -55—not a normalized positive remainder of 5. Subsequent calls must preserve this signed accumulator behavior. This is treated as compatibility behavior, not corrected arithmetic.

## Caveats

This is source analysis and pure-Java test verification, not an in-game reproduction. In the audited parent tick sequence, steam pushing occurs before the production call; full-tick transfer, tank and vent interactions need separate tests.
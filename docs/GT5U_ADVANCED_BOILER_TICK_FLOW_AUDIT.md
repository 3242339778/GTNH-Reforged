# Advanced Boiler Tick Flow Audit (5.09.54.277)

**Reference source:** GT5-Unofficial tag/release 5.09.54.277 (commit `4060086`), `MTEAdvancedBoilerBase` and inherited `MTEBoiler`.
**Status:** Source audit; no integration adapter implemented.

## Scope and source classes
- `gtPlusPlus.xmod.gregtech.common.tileentities.generators.MTEAdvancedBoilerBase`
- `gregtech.common.tileentities.boilers.MTEBoiler`

## Server tick flow

The inherited `onPostTick` is the orchestration point:

1. `pollute(tick)`: if processing energy > 0 and tick divisible by 20, emit configured boiler pollution.
2. `isNotAllowedToWork`: return on client side or ticks <= 20. No later tick operations occur in those cases.
3. `calculateCooldown()`: temperature <=20 clamps to 20 and resets loss timer; otherwise increments timer, and only when it exceeds interval (Advanced Boiler: 40) decrements temperature by one and resets timer.
4. `pushSteamToInventories(tile)`: attempt external steam transfer before this tick's steam generation. All directions except front and DOWN. Per side, transfer limit is `max(1, steam.amount / 2)`.
5. `canNotCreateSteam(tile,tick)`: Advanced Boiler override returns false always, so it does not trigger parent's early-return path. On ticks divisible by 10: when temperature >100 and valid positive water exists, generates `productionPerSecond / 2` steam and sets `playBoiling=true`; when temperature <=100 it sets `playBoiling=false`. At high temperature without water it does not set parent `mHadNoWater`, nor clear boiling state in this override.
6. `ventSteamIfTankIsFull()`: if steam amount strictly exceeds capacity, emits sound and resets amount to 3/4 capacity.
7. `updateFuelTimed(tile,tick)`: only if processing energy <=0 and machine is allowed to work; Advanced Boiler fuel update validates fuel and temperature <=101.
8. `calculateHeatUp(tile,tick)`: if temperature < max, energy >0, and tick divisible by 10, subtract 2 processing energy and add 1 temperature. When energy <=0, clear fuelMaxEnergy; active flag reflects energy >0.

## Fuel injection and fuel side effects
- Fuel lookup: Forge GameRegistry fuel value, fallback to vanilla furnace fuel lookup.
- Eligibility: positive burn time and temperature <=101.
- Injection: processing energy += burnTime / 10; temperature += burnTime / 500.
- fuelMaxEnergy tracks max processing energy.
- Fuel slot decremented by one. With 1/3 random chance, ash/ dark ash is inserted according to fuel display name containing charcoal or coke; this is world/inventory/random side-effect and excluded from current pure model.

## Steam/water behavior
- Advanced tier output: 750 × tier steam per second nominally; production occurs on 10-tick cadence in batches of 375 × tier.
- Production gate: temperature >100, water fluid recognized as water, and amount >0.
- Conversion accumulator: inherited `mExcessWater` arithmetic subtracts steam batch; when negative, water consumption uses Java integer division with 160 steam/water and carries the signed remainder.
- Advanced Boiler water capacity: 16000 + 16000 × tier; steam capacity = 2 × water capacity.
- Steam push precedes production. Overflow vent check follows production.
- No-water explosion guard: Advanced Boiler override of `onDangerousWaterLack` is empty. In addition, its steam-production override avoids parent `mHadNoWater` tracking behavior; the parent's no-water danger path is not invoked by this override.

## Important non-equivalences and model boundaries
- A model method that only computes production amount does not simulate transfer, tanks, inventory, random ash, pollution, active-state toggling, sounds, GUI, NBT or world synchronization.
- Tick ordering matters: steam transfer occurs before production; venting and fuel update occur after the production hook; heat-up occurs last.
- The inherited parent boiler has a dangerous-water state path, but Advanced Boiler overrides `canNotCreateSteam` and `onDangerousWaterLack`. Do not accidentally model parent behavior as Advanced Boiler behavior.
- The production model's current `productionOnTick(tick,temperature,waterAmount)` is a stateless gate except for its accumulated diagnostic steam counter. It should not be used as a drop-in tick orchestrator.

## Suggested adapter contract (design only)
An eventual adapter should explicitly provide a tick snapshot (server-side tick, temperature, max temperature, processing energy, water type/amount, steam type/amount/capacity, fuel information, machine-work permission, output-side handlers) and apply model results through separate effects (fluid transfer, fuel consumption/container return/ash RNG, pollution, sound/state updates, NBT synchronization).

Do not implement the adapter until the state snapshot and effect contract are reviewed against the target 26.3 architecture.

## Engine consistency audit (follow-up)

A follow-up review against the fetched source at commit `e0518e35b896c3a0663065aea57287dbf49c77b9` identified and corrected several discrepancies in the pure engine:

- The inherited tick gate returns for ticks <= 20 before cooldown, steam transfer, production, venting, fuel, or heat-up. The pure engine now preserves state and returns no effects for that range.
- Overflow is checked against `getSteamCapacity()` (Advanced Boiler: twice water capacity), not `getCapacity()`. The engine and test now use steam capacity.
- Snapshot validation must permit steam amounts above capacity so that the overflow branch can be represented and tested; water capacity validation remains strict.
- The boiling flag is set based on the water validity/positive-amount gate before production mutates the water amount. A tick that consumes the last water still sets the flag true for that tick, matching the override.
- Fuel max tracking is done by the inherited `addProcessingEnergy` during fuel injection. The engine continues to capture the resulting maximum; a separate zero-energy clearing occurs in heat-up phase.

The pure engine still does not simulate server-side checks, pollution, steam transfer, item/container/ash handling, sounds, or synchronization. Those remain adapter responsibilities.

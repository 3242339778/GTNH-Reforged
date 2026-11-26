# Advanced Boiler Tick Snapshot / Result Contract (Design Proposal)

Status: design proposal only. No production code changes in this document.

## Purpose
Introduce a narrow, plain-Java boundary between a machine adapter and the audited mechanics. Snapshot is immutable input; result is immutable output describing calculated state and effects for the adapter to apply. Neither type imports Minecraft, NeoForge, GT APIs, world, or fluid/item classes.

## Proposed snapshot data
`LegacyAdvancedBoilerTickSnapshot` should represent one server tick and contain:
- tick: long
- temperature, maxTemperature: int
- lossTimer, cooldownInterval: int
- processingEnergy, fuelMaxEnergy: int
- waterAmount, steamAmount: int
- waterIsValid: boolean (adapter performs fluid identity check)
- steamIsValid: boolean (adapter performs fluid identity check)
- waterCapacity, steamCapacity: int
- excessWaterRemainder: int (carry from audited water/steam integer conversion)
- machineAllowedToWork: boolean
- burnTimeAvailable: int (0 when no valid fuel; adapter resolves fuel validity)
- productionPerSecond: int (tier-derived configuration)
- heatUpRate, heatUpEnergyCost, heatUpAmount: int (configuration; current audited values 10, 2, 1)

Snapshot should not contain ItemStack, FluidStack, tile entities, handlers, random sources, world references, GUI state, sound objects, or persistence objects.

## Proposed result data
`LegacyAdvancedBoilerTickResult` should report:
- resulting temperature, lossTimer, processingEnergy, fuelMaxEnergy
- resulting waterAmount, steamAmount, excessWaterRemainder
- steamProducedThisTick and waterConsumedThisTick (diagnostic accounting)
- fuelInjectionRequested and burnTimeConsumed (adapter must perform item-stack decrement/container handling/ash randomness)
- activeState (calculated from remaining processing energy)
- boilingState (modelled only when behavior is source-audited; otherwise adapter-owned)
- vented flag (adapter executes sound/effect; amount reduction is represented in resulting steam amount)
- notes: steam pushing is an external operation and must not be simulated by the calculation result unless a transfer API/result contract is added separately.

## Contract caveats
1. Validate water/steam identity and stack availability in adapter; pass explicit booleans and amounts.
2. Apply fuel effects only if result requests fuel injection and the snapshot's eligibility assumptions still hold at the authoritative tick.
3. Steam pushing is intentionally separate: audited tick ordering pushes existing steam before generation, so a future orchestration method must model this ordering explicitly.
4. Existing model methods remain unchanged during contract drafting. This proposal does not claim full tick orchestration or equivalence to the original machine.
5. Result should eventually include structured event/effect records rather than embedding game-specific side effects.

## Suggested implementation sequence
1. Implement immutable snapshot and result types, with validation and unit tests.
2. Add a pure orchestrator that consumes snapshot plus configuration and returns a result; keep transfer effects explicit/outside.
3. Test tick-order, production, conversion remainder, venting, fuel gate, heat-up and cooldown together.
4. Only then write the game adapter and integration tests.

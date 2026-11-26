# Advanced Boiler Compatibility Adapter Boundary Audit

**Status:** Boundary audit / planning only. No Minecraft 26.3 adapter implementation.
**Reference behavior:** GT5-Unofficial 5.09.54.277 source audit and current pure Java tick engine.

## Compatibility objective
The adapter is not a new boiler implementation. It is a bridge that hosts the audited legacy behavior in the target Minecraft/NeoForge environment. Target APIs may change the means of storing, transferring, synchronizing, and interacting with state, but should not silently redefine the audited legacy rules.

## Responsibility split

### Legacy pure calculation
- Temperature cooldown, production gate and cadence, water/steam integer conversion and signed remainder.
- Overflow threshold and post-vent steam quantity.
- Fuel-injection eligibility gates and arithmetic, fuel energy tracking.
- Heat-up cadence, energy cost, temperature increment and active/boiling calculated state.
- Preserve audited edge cases unless a later source audit demonstrates the current model is wrong.

### Target-side adapter responsibilities
- Server authority and tick scheduling.
- Resolve fluid identity and validity; construct primitive snapshot values.
- Interact with target fluid/item/energy handlers and inventories.
- Steam transfer to adjacent inventories, respecting the source direction exclusions and transfer limit.
- Resolve fuel items and fuel values, decrement/return containers, ash randomness and item insertion.
- Apply pollution, sound and other world effects.
- Apply state, NBT/save, GUI and client synchronization.
- Translate calculated state into target API state without changing legacy arithmetic.

## Tick-order constraint
The source order is significant: pollution; early server/tick gate; cooldown; push existing steam; production hook; overflow vent; fuel update; heat-up. The current pure engine represents cooldown, production, venting, fuel and heat-up in a single calculation but does not model steam transfer or pollution. A future adapter/orchestrator must not simply call the engine at an arbitrary point in a machine tick. It must preserve the order or introduce a separately audited orchestration/effect contract. Steam transfer occurs before production, and may change the steam quantity available to the later production/vent phases.

## Known representation hazard: negative water amount
The audited legacy conversion may calculate a negative water amount under scarcity. The pure model preserves this arithmetic. This does **not** establish that a target tank/FluidStack API accepts negative amounts or that such an amount can be persisted safely. The adapter must not blindly assign a negative amount to a target fluid handler. This is an unresolved compatibility boundary requiring inspection of the target API and legacy stack/tank semantics. Do not clamp or reinterpret without a documented compatibility decision and tests.

## Snapshot/result contract review
- Snapshot/result remain game-API-free.
- Adapter resolves input identity and external fuel/fluid availability.
- Result describes calculations; it does not itself execute external side effects.
- The existing `steamIsValid` snapshot field is currently not consumed by the engine; its intended role should be reviewed before adapter implementation rather than treated as a functional gate.
- Snapshot captures scalar amounts; adapter must establish at what exact point in the source tick these amounts are sampled.
- Ensure effects that consume external resources (fuel item decrement) are applied consistently with the model's requested injection, and handle any mismatch explicitly.

## Deferred investigation before implementation
1. Inspect target 26.3 machine tick, fluid storage and item/fuel APIs.
2. Determine how target storage represents or rejects negative fluid quantities and how legacy persistence handled the edge case.
3. Design a tick orchestration/effect contract preserving source ordering, particularly steam push before production and vent.
4. Define server/client state and save/load mappings.
5. Implement adapter only after these findings and contract tests are agreed.

## Current conclusion
The pure model is tested, but integration equivalence has not been established. This audit intentionally does not claim that the adapter is ready to implement.

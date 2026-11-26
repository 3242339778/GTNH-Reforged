# Advanced Boiler Tick Engine

The pure-Java engine consumes a primitive immutable tick snapshot and returns an immutable result. It does not access Minecraft, NeoForge, fluids, inventories, world state, or perform steam transfer.

## Current calculation order
1. Cooldown.
2. Steam production and water conversion on the audited 10-tick cadence.
3. Overflow venting when steam exceeds the audited `getCapacity()` threshold (represented by snapshot `waterCapacity`), resetting to 3/4 capacity.
4. Fuel injection when processing energy is empty, work is allowed, fuel is available, and temperature is at most 101.
5. Heat-up on the configured cadence while below maximum temperature and processing energy is positive.

## Boiling flag
The calculation accepts the previous boiling state. On a production cadence, it sets boiling when temperature is above 100 and water is valid and available; it clears boiling only when temperature is at or below 100. At higher temperature without valid water, the prior flag is retained, matching the audited source override.

## Integration boundary
Steam pushing, fuel-stack consumption/ash, pollution, GUI updates, persistence, and server/client authority remain adapter responsibilities. The snapshot's `burnTimeAvailable` is resolved by the adapter, and `burnTimeConsumed` is an effect request/record, not an actual inventory mutation.

## Important limitation
The snapshot does not model pre-production steam pushing. Adapter code must execute the source-audited transfer stage before applying the engine result, or a later contract revision must explicitly model transfers.

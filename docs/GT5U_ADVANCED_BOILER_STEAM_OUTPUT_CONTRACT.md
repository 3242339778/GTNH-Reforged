# GT5U Advanced Boiler Steam Output Contract

**Status:** Source-derived pure Java compatibility contract; no NeoForge machine adapter.
**Reference version:** GT5-Unofficial 5.09.54.277.

## Source path

The audited call chain is:

1. `MTEBoiler.onPostTick` invokes `pushSteamToInventories` after cooldown and before steam production.
2. `pushSteamToInventories` visits `ForgeDirection.VALID_DIRECTIONS` in declaration order.
3. It skips the boiler front and `DOWN`.
4. `pushSteamToSide` resolves the adjacent `IFluidHandler`; a missing handler is skipped.
5. It requests `Math.max(1, mSteam.amount / 2)`.
6. `GTUtility.moveFluid` performs simulated source drain, simulated destination fill, actual source drain, then actual destination fill.

Relevant source files, all at the audited revision:

- `gregtech/common/tileentities/boilers/MTEBoiler.java`
- `gregtech/api/util/GTUtility.java`
- `gregtech/api/metatileentity/implementations/MTEBasicTank.java`
- `gtPlusPlus/xmod/gregtech/common/tileentities/generators/MTEAdvancedBoilerBase.java`

## Observable behavior contract

- Output runs on the server tick path only, after cooldown and before production.
- The front and bottom sides are never output targets.
- Eligible side order is `UP, NORTH, SOUTH, WEST, EAST`, with the front-facing horizontal side removed.
- A side with no adjacent fluid handler is skipped.
- Each side requests half of the current steam amount, rounded down, with a minimum request of one.
- The request is recalculated after each preceding side's successful transfer.
- A destination that simulates accepting zero receives no real fill call and causes no source drain.
- A partial simulated acceptance drains and offers only the accepted amount.
- The next side sees the reduced source amount.
- Empty steam causes no target lookup or transfer.
- The boiler has a separate drainable steam tank; water input and steam output are not the same tank.

## Important legacy simulation caveat

`GTUtility.moveFluid` uses the destination's simulated fill return to decide how much to drain from the source. It then calls the real destination fill but ignores that call's return value.

Consequently, if a destination reports that it can accept 20 during simulation but accepts only 7 during the real fill, the source has already drained 20. The old helper does not restore the difference. The normal handler contract is expected to make simulation and execution consistent, but the source code does not enforce it.

The pure Java transfer policy preserves this observable sequence. A future NeoForge adapter may use a transaction to make source extraction and destination insertion atomic, but that would be a deliberate robustness improvement over this edge behavior and must be documented as such rather than described as exact reproduction.

## Pure Java implementation boundary

`LegacyBoilerSteamOutputTransfer` contains only the source-derived direction, request-size, and transfer-order policy. Its `TransferPort` is the integration seam for resolving adjacent target handlers and performing simulated/real fills. It has no Minecraft or NeoForge dependencies.

The class assumes the target obeys the ordinary fluid-handler contract and does not report accepting more than the offered amount. It rejects negative steam amounts because negative steam is outside the valid output-tank state; the separately audited negative-water arithmetic remains a distinct legacy edge case.

## Test coverage

`LegacyBoilerSteamOutputTransferTest` covers:

- front/bottom exclusions and source direction order;
- partial destination acceptance;
- simulated rejection;
- recalculation of each subsequent half-request;
- minimum one-unit request;
- the ignored real-fill return-value caveat;
- empty steam;
- invalid negative steam input.

These tests validate the pure compatibility policy. They do not yet prove NeoForge capability discovery, transaction behavior, chunk-boundary handling, or actual adjacent-block integration.

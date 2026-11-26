# GT5U Advanced Boiler Fluid Inventory and Handler Audit

**Status:** Legacy fluid-storage/handler audit. No NeoForge handler implementation.
**Legacy reference:** GT5-Unofficial 5.09.54.277, commit `e0518e35b896c3a0663065aea57287dbf49c77b9`.
**Target:** NeoForge 26.3.0.113 / Minecraft 26.3.

## Scope

This audit examines the boiler's own fluid inventories, legacy `MTEBasicTank` / `MTEBoiler` fluid methods, and the external `BaseMetaTileEntity` / Cover side-routing path. It defines the semantic requirements for a future `ResourceHandler<FluidResource>` adapter. The external side-routing path has now been traced; the exact rules are recorded in section 8.

## 1. The boiler has two independent fluid inventories

In `MTEBasicTank`:

- `mFluid` is the fillable/input stack by default.
- `getFillableStack()` returns `mFluid`.
- `getDrainableStack()` also returns `mFluid` by default.
- `isDrainableStackSeparate()` defaults to `false`.

In `MTEBoiler`:

- `mSteam` is a separate field.
- `steamTank` is a separate UI tank backed by `mSteam` and `getSteamCapacity()`.
- `getDrainableStack()` is overridden to return `mSteam`.
- `setDrainableStack()` writes `mSteam`.
- `isDrainableStackSeparate()` returns `true`.

Thus the boiler has two distinct logical inventories:
1. Water input: `mFluid`, capacity `getCapacity()`.
2. Steam output: `mSteam`, capacity `getSteamCapacity()`.

The Advanced Boiler overrides capacities so steam capacity is twice the water capacity. Do not assume both inventories have the same capacity.

## 2. Legacy fill and drain semantics

### Fill

`MTEBasicTank.fill(FluidStack, boolean)` is the input operation. It returns zero when:
- input is null;
- fluid ID is invalid;
- amount is non-positive;
- `canTankBeFilled()` is false;
- `isFluidInputAllowed(resource)` is false.

For an empty input inventory, it accepts up to `getCapacity()`. For a non-empty inventory, it only accepts a fluid equal to the existing fillable fluid and accepts up to remaining capacity.

`MTEBoiler.isFluidInputAllowed()` accepts water only (`GTModHandler.isWater`). Therefore automation filling the boiler adds water, not steam.

### Drain

`MTEBasicTank.drain(maxDrain, doDrain)` reads `getDrainableStack()`, checks `canTankBeEmptied()`, and removes up to the requested amount. Because the boiler overrides the drainable stack, automation draining the boiler extracts steam, not water.

The boiler currently returns true for both `canTankBeFilled()` and `canTankBeEmptied()`.

### Directional handler implication

The legacy tank's fill and drain methods do not themselves accept a side argument. Side-aware routing is performed by the surrounding GregTech tile-entity / cover / capability dispatch. This audit therefore does not claim that every side can fill or drain without restriction. The boiler's own `allowCoverOnSide` permits covers whose placer allows primitive blocks; that is not, by itself, proof of the final per-side fluid access decision.

The next source trace must inspect the base tile entity's external fluid-handler dispatch and cover interception before finalizing side rules.

## 3. Tank information and a capacity reporting discrepancy

When `isDrainableStackSeparate()` is true, `MTEBasicTank.getTankInfo(side)` returns two entries:
1. fillable stack with capacity `getCapacity()`;
2. drainable stack with capacity also reported as `getCapacity()`.

The second entry does not call `getSteamCapacity()`. For Advanced Boiler, this means the reported steam tank capacity can be lower than the actual steam capacity (which is twice the water capacity). Treat this as a legacy reporting discrepancy, not as the real steam storage limit.

The actual steam overflow logic uses `getSteamCapacity()` and vents only when `mSteam.amount > getSteamCapacity()`, then resets the amount to 75% of that capacity.

A NeoForge adapter should preserve actual storage/transfer behavior and should not accidentally adopt the old `getTankInfo` display hint as the true steam capacity. Whether to reproduce this inaccurate metadata in any compatibility-facing display is a separate decision.

## 4. Container behavior is distinct from automation fill/drain

The boiler enables both container directions:
- `doesEmptyContainers()` is true: filled water containers in the input slot can add water to `mFluid`.
- `doesFillContainers()` is true: empty containers can be filled from `mSteam`.

The input slot also accepts filled steam containers for the output-container path. This is item-container processing, not evidence that automation may fill the water input with steam.

The fluid UI exposes separate widgets:
- inherited `fluidTank` backed by `mFluid`;
- boiler `steamTank` backed by `mSteam`.

The boiler's GUI and container handling must not be conflated with the automation-facing ResourceHandler contract.

## 5. Persistence

`MTEBasicTank.saveNBTData` stores `mFluid` under `mFluid`; `MTEBoiler.saveNBTData` separately stores `mSteam` under `mSteam`, along with boiler thermal/fuel state.

Load follows the same split. A migration adapter must preserve the distinction and must not serialize the steam output into the water input field.

Fluid identity persistence also needs a separate migration decision: old GT5U FluidStack NBT is not automatically equivalent to NeoForge's modern fluid Data Components.

## 6. Proposed NeoForge handler shape

The closest semantic mapping is a side-aware, two-index `ResourceHandler<FluidResource>` view:

| Logical index | Backing state | Insert | Extract | Capacity |
|---|---|---|---|---|
| 0 | water / `mFluid` | Water only, subject to input rules | Reject | `getCapacity()` |
| 1 | steam / `mSteam` | Reject | Steam only, subject to output rules | `getSteamCapacity()` |

This is a design mapping, not implemented code. The handler must ensure:
- index 0 and index 1 never alias the same mutable stack;
- the resource identity for each occupied inventory includes the correct NeoForge Data Components;
- empty slots accept only their designated fluid type;
- water and steam are never merged or cross-inserted;
- amounts remain non-negative at the NeoForge boundary;
- side/cover restrictions are applied by a side-specific view or provider;
- mutations participate in NeoForge transactions and rollback correctly;
- root-commit callbacks perform dirty/synchronization work only after successful commit.

A single two-index handler is preferable as an initial compatibility representation because legacy `getTankInfo` exposes two tanks through one fluid-handler object. If the target automation ecosystem expects separate handlers, that would need an explicit compatibility decision rather than an accidental split.

## 7. Important unresolved compatibility edges

1. **Negative water arithmetic:** the pure GT5U calculation can drive `mFluid.amount` negative in the audited scarcity edge case. NeoForge ResourceHandler amounts are non-negative. The adapter must not write a negative amount into a ResourceHandler. A compatibility shadow value or a documented normalization/recovery policy is required; do not silently clamp without tests.
2. **Side/cover routing:** the legacy gate is now known (section 8). The future provider must reproduce it, including the `UNKNOWN` side bypass and cover-specific fluid permissions.
3. **Steam metadata capacity:** decide whether any legacy-facing tank-info presentation should preserve the inaccurate reported capacity, while actual storage continues using `getSteamCapacity()`.
4. **Fluid identity:** determine the registered steam fluid and Data Component policy in the target project.
5. **Transaction integration:** use journaled transactional mutation. Do not mutate the legacy model or backing state outside the transaction and then expect a handler rollback to undo it.
6. **Container/item paths:** preserve separate item-container behavior; do not implement it by bypassing the fluid handler's resource/amount contract.

## Conclusion

The legacy boiler is one external fluid handler with two separate internal inventories: water is fill-only and steam is drain-only. The best initial NeoForge representation is a two-index `ResourceHandler<FluidResource>` with separate capacities and strict per-index insert/extract rules, wrapped in a side-aware provider. Exact cover/side routing has been traced through GregTech's base tile-entity dispatch and is recorded in section 8. No machine adapter is implemented by this audit.


## 8. External fluid-handler side routing (resolved from source)

Source path:
- `BaseMetaTileEntity.fill(ForgeDirection, FluidStack, boolean)`
- `BaseMetaTileEntity.drain(ForgeDirection, int, boolean)`
- `BaseMetaTileEntity.drain(ForgeDirection, FluidStack, boolean)`
- `BaseMetaTileEntity.canFill/canDrain`
- `MetaTileEntity.isLiquidInput/isLiquidOutput`
- `CoverableTileEntity.getCoverAtSide`
- `Cover.letsFluidIn/letsFluidOut`
- `CoverNone`

### 8.1 Boiler's base side policy

The base `MetaTileEntity` implementation returns `true` from both `isLiquidInput(side)` and `isLiquidOutput(side)`. `MTEBoiler` does not override either method. Therefore, the boiler itself does not impose a front/back/vertical restriction on external fluid input or output: all six real directions pass the meta-tile side predicate.

This is distinct from the boiler's automatic steam-push loop, which independently skips its front-facing side and DOWN. That push policy does not restrict external automation drain access.

### 8.2 Base tile entity gates

For a real side (anything other than `ForgeDirection.UNKNOWN`), external fill is delegated only when all of these hold:

1. `mTickTimer > 5`;
2. `canAccessData()`;
3. the machine is currently running its own tick, or external input is not disabled (`mRunningThroughTick || !mInputDisabled`);
4. `mMetaTileEntity.isLiquidInput(side)` is true;
5. `getCoverAtSide(side).letsFluidIn(fluid)` is true.

External drain has the corresponding conditions, using:
- `mRunningThroughTick || !mOutputDisabled`;
- `mMetaTileEntity.isLiquidOutput(side)`;
- `getCoverAtSide(side).letsFluidOut(fluid)`.

The same gates are applied by `canFill` and `canDrain`. Tank-info lookup is different: `getTankInfo(side)` delegates when meta-tile data is accessible and does not apply these fill/drain cover gates.

For `drain(side, maxDrain, doDrain)`, the Cover receives the fluid currently returned by `mMetaTileEntity.getFluid()`. For `drain(side, requestedFluid, doDrain)`, it receives the requested fluid. This distinction should be retained if reproducing the old overload behavior exactly.

### 8.3 Cover defaults and consequences

`CoverableTileEntity` initializes all six sides with `CoverRegistry.NO_COVER`. The no-cover implementation (`CoverNone`) returns true for both `letsFluidIn` and `letsFluidOut`. Thus, with no cover and normal machine access enabled, all six sides are open for water insertion and steam extraction.

An installed Cover may override these methods. The base `Cover` implementation defaults both fluid permissions to false, so a custom Cover that does not explicitly permit fluid movement blocks it. Examples include:
- `CoverShutter`, which independently gates fluid-in and fluid-out;
- `CoverDecorative`, which uses its cover-data bit to permit or deny fluid movement;
- `CoverFluidLimiter`, which can gate input based on fill level.

The fluid type passed to the Cover is the relevant resource's legacy Fluid. The Cover check is per side and per direction, not a global boiler setting.

### 8.4 UNKNOWN side is a deliberate bypass

When the caller supplies `ForgeDirection.UNKNOWN`, the base fill/drain methods bypass both:
- `isLiquidInput/isLiquidOutput(side)`;
- `Cover.letsFluidIn/letsFluidOut(fluid)`.

The timer, data-access, and external input/output-disabled gates still apply. This is an intentional legacy path used by internal or compatibility callers; do not silently equate UNKNOWN with one of the six world directions.

### 8.5 Direction meaning at the adapter boundary

The direction passed to the boiler's external handler is the boiler-side face being accessed. When a neighboring block queries the boiler through a face, the provider must map that query to the corresponding side on the boiler. In NeoForge's block capability lookup, this is the face of the boiler exposed toward the querying neighbor (the opposite of the neighbor's outward face).

Keep this separate from the neighbor lookup performed by the boiler when it pushes steam outward: that lookup targets the adjacent block and uses the face of that adjacent block facing back toward the boiler.

### 8.6 Adapter contract derived from the trace

For each queried side, the future adapter/provider should apply the equivalent of:

- water index insertion: timer/data/input-enabled gate, boiler input-side predicate, and that side's Cover input permission;
- water index extraction: always reject at the tank semantic layer;
- steam index insertion: always reject at the tank semantic layer;
- steam index extraction: timer/data/output-enabled gate, boiler output-side predicate, and that side's Cover output permission.

Since the boiler inherits the default side predicates, all six sides are eligible absent a Cover or global disable gate. Do not bake the front/down steam auto-push exclusions into external extraction access.

The adapter still needs a target-side representation for Covers and machine-wide external input/output disable state. A bare two-index ResourceHandler without a side-aware wrapper would not preserve this legacy contract.

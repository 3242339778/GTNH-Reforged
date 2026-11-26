# GT5U Advanced Boiler Fluid Inventory and Handler Audit

**Status:** Legacy fluid-storage/handler audit. No NeoForge handler implementation.
**Legacy reference:** GT5-Unofficial 5.09.54.277, commit `e0518e35b896c3a0663065aea57287dbf49c77b9`.
**Target:** NeoForge 26.3.0.113 / Minecraft 26.3.

## Scope

This audit examines the boiler's own fluid inventories and the legacy `MTEBasicTank` / `MTEBoiler` fluid methods. It defines the semantic requirements for a future `ResourceHandler<FluidResource>` adapter. It does not yet establish all GregTech cover/tile-entity side-routing rules; those require tracing the base tile entity's external `IFluidHandler` dispatch.

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
2. **Side/cover routing:** trace the GregTech base tile entity's external fill/drain routing, including cover interception, before defining the side-specific provider.
3. **Steam metadata capacity:** decide whether any legacy-facing tank-info presentation should preserve the inaccurate reported capacity, while actual storage continues using `getSteamCapacity()`.
4. **Fluid identity:** determine the registered steam fluid and Data Component policy in the target project.
5. **Transaction integration:** use journaled transactional mutation. Do not mutate the legacy model or backing state outside the transaction and then expect a handler rollback to undo it.
6. **Container/item paths:** preserve separate item-container behavior; do not implement it by bypassing the fluid handler's resource/amount contract.

## Conclusion

The legacy boiler is one external fluid handler with two separate internal inventories: water is fill-only and steam is drain-only. The best initial NeoForge representation is a two-index `ResourceHandler<FluidResource>` with separate capacities and strict per-index insert/extract rules, wrapped in a side-aware provider. Exact cover/side routing remains to be traced through GregTech's base tile-entity dispatch before the provider can be considered complete. No machine adapter is implemented by this audit.

# GT5U Advanced Boiler NeoForge Steam Transfer Adapter Audit

**Status:** API/transaction boundary audit only. No Minecraft machine adapter or capability lookup implementation.
**Target:** NeoForge 26.3.0.113 / Minecraft 26.3.
**Legacy reference:** GT5-Unofficial 5.09.54.277.

## Scope

This audit connects the already-tested pure Java policy in `LegacyBoilerSteamOutputTransfer` to the target fluid resource APIs. It does not change the legacy policy, implement a block entity, or claim that adjacent capability discovery has been audited.

## Target API findings

### Resource identity and amount are separate

- `FluidResource` is an immutable resource identity, not a quantity-bearing stack.
- `FluidResource.of(FluidStack)` retains the fluid and its data-component identity while discarding the amount.
- Equality and matching account for both fluid identity and data components. A target tank containing the same registered fluid but different components is not necessarily the same resource.
- `FluidResource.toStack(amount)` rejects negative amounts; zero produces an empty stack.
- Consequently, the adapter must retain the exact steam resource identity (including components) separately from the current amount. It must not reduce identity to a fluid registry ID or assume legacy NBT and modern data components are interchangeable.

### ResourceHandler transfer contract

- `ResourceHandler` is an indexed resource-storage abstraction. Amounts and capacities exposed by its API are non-negative; insert/extract requests must be non-negative and use a `TransactionContext`.
- There is no legacy-style `simulate = true/false` argument on the audited insert/extract contract.
- A returned insertion/extraction amount is the authoritative amount accepted or removed. Capacity and validity queries are hints; the actual operation result is authoritative.
- `FluidStacksResourceHandler` is a list-backed helper for `FluidStack` storage. It can provide tank slots and capacity, but it is not by itself a complete machine implementation or a substitute for block capability discovery.
- The boiler's water input and steam output must remain separate logical tanks/resources, as in the audited GT5U implementation.

### Transaction behavior

- A root transaction commits when explicitly committed and then closed. Closing without commit rolls back its journaled changes.
- A nested transaction commit remains subject to rollback by its parent.
- Multiple handlers can participate in the same transaction by receiving the same transaction context, provided their implementations correctly journal their changes.
- `StacksResourceHandler` snapshots a slot before insert/extract mutation. Rollback restores the stack; root-commit callbacks run after the root transaction closes.
- `set(index, resource, amount)` is a direct mutation path with its own immediate contents-changed callback. It should not be used as a shortcut for a transactional transfer.
- Root-commit callbacks may run after the transaction has closed. Any callback that touches a block entity/world must tolerate the block entity having been removed or unloaded in the meantime.

## Mapping the legacy transfer sequence

The audited GT5U helper performs, per eligible side:

1. Simulate source drain for the current request.
2. Simulate destination fill using the simulated drained stack.
3. If the destination accepts a positive amount, actually drain that accepted amount from the source.
4. Actually fill the destination with the stack returned by the real source drain.
5. Ignore the actual destination fill return value.

NeoForge has no direct simulation flag in this API. A rollback transaction can provide a simulation-like probe:

1. Open a temporary root transaction.
2. Attempt the relevant `extract` or `insert` operation and record its returned amount.
3. Close without committing, so journaled storage changes roll back.
4. Use the returned amount as the probe result.

For a transfer, the adapter can probe source extraction and destination insertion separately, then perform the real source extraction and destination insertion with the same root transaction context. This makes the actual writes participate in one transaction. A per-side root transaction also preserves the legacy sequential behavior: an earlier side's completed transfer remains committed if a later side is rejected.

## Compatibility decision: simulation mismatch

The old helper intentionally has a source-level edge case: if destination simulation reports 20 but the real destination fill accepts only 7, the source has already drained 20 and the difference is lost. The pure Java policy explicitly preserves this behavior by decrementing its remaining amount from the simulated acceptance and recording, but not using, the real-fill return value.

A transactional adapter must not silently claim both exact legacy behavior and all-or-nothing conservation in this case; those goals conflict.

- **Exact legacy mismatch behavior:** commit the actual source extraction and whatever amount the real destination insertion reports, even if the latter is smaller. This preserves the old loss edge case.
- **Conserving transactional behavior:** roll back the entire transfer if actual insertion differs from the probed amount. This is a deliberate behavior improvement, not exact reproduction.

The project should choose and document one policy before adapter implementation. The current pure Java contract chooses exact legacy behavior.

## Current pure Java seam: limitation to keep visible

`LegacyBoilerSteamOutputTransfer.TransferPort` exposes `simulateFill` and `fill` separately. The policy subtracts the simulated accepted amount before invoking `fill`, and intentionally ignores the latter's return value. This seam is suitable for the tested legacy contract and a stable, contract-compliant target handler.

It does not itself express an atomic transfer result or a rollback outcome. If the project later chooses conserving transactional behavior, do not hide that behavior behind the existing `TransferPort` implementation: introduce a separately named policy/port contract or revise the pure contract and tests explicitly. Otherwise the pure model could report steam drained even when an adapter rolled the actual transfer back.

The adapter must also account for a possible difference between simulated and actual source extraction. The legacy helper passes the actual drained stack to the destination; it does not assume that the source's real drain necessarily returns the full simulated amount.

## Capability discovery remains unaudited

This audit does not yet establish:

- the exact NeoForge 26.3 block-fluid capability lookup call and direction semantics;
- how a boiler exposes its own steam tank capability;
- how the target capability behaves for absent, invalidated, unloaded, or replaced neighboring block entities;
- whether all intended third-party handlers support transaction rollback correctly.

Those must be verified against the exact 26.3 API/source and the project's chosen block-entity design before writing the adapter. Do not infer these details from the generic `ResourceHandler` contract.

## Recommended implementation boundary

1. Keep `LegacyBoilerSteamOutputTransfer` as the source-derived policy; do not make it depend on Minecraft or NeoForge classes.
2. Add a target-side adapter that resolves the adjacent fluid handler, creates/retains the correct `FluidResource`, and bridges probe/real operations.
3. Use one transaction context for the source and destination's actual writes for each side.
4. Preserve the legacy side iteration and recalculated request size.
5. Make the simulation-mismatch policy an explicit decision; current compatibility default is the legacy behavior.
6. Audit capability discovery and lifecycle behavior before implementing the adapter.
7. Add adapter-level tests with transactional fake handlers, including rollback, partial acceptance, source extraction mismatch, destination simulation/actual mismatch, and neighbor invalidation.

## Conclusion

The NeoForge resource/transaction API is capable of supporting a transaction-aware bridge, and rollback transactions can be used for simulation-like probes. However, transaction support alone does not define compatibility policy. The existing pure Java transfer contract deliberately preserves GT5U's ignored-real-fill-return edge case; a conserving all-or-nothing adapter would be a documented semantic change. Capability discovery is still a separate unresolved audit item.

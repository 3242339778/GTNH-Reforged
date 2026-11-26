# GT5U Advanced Boiler Fluid State and Transaction Contract

Status: design contract; no NeoForge handler or block-entity adapter implementation.

## Purpose

Define one authoritative aggregate for the Advanced Boiler's automation-visible fluid state and the transaction boundary that will protect it. This contract sits between the existing pure Java legacy behavior models and the future NeoForge 26.3 `ResourceHandler<FluidResource>` adapter.

The objective is compatibility with GT5U 5.09.54.277, not a redesign of the boiler's fluid behavior.

## 1. State ownership

The future `LegacyBoilerFluidState` is the single owner of fluid state that an external fluid transfer can mutate.

It composes the existing `LegacyBoilerWaterShadowState` rather than copying its fields:

- **Water input (index 0):** owned by `LegacyBoilerWaterShadowState`; retains nullable legacy fluid identity, signed amount, and signed `excessWaterRemainder`.
- **Steam output (index 1):** owned by the aggregate; retains steam fluid identity and a non-negative amount.
- **Snapshot:** captures both tanks and the water conversion remainder as one immutable value.

The aggregate must not create a second water amount or remainder. The water shadow state remains authoritative for those values.

Steam amount is constrained to be non-negative because the audited steam output path and transfer contract do not require negative steam quantities. A positive steam amount must have a valid fluid identity. The implementation must preserve the legacy distinction between water and steam identities; it must not infer one from the other.

## 2. NeoForge projection

The aggregate exposes read-only projections for the future handler:

| Handler index | Resource | Amount | Capacity |
|---|---|---|---|
| 0 | water resource, only while the legacy water amount is positive | `max(0, legacyWaterAmount)` | legacy `getCapacity()` |
| 1 | steam resource, only while steam amount is positive | non-negative steam amount | legacy `getSteamCapacity()` |

A non-positive legacy water stack projects as empty, but its signed amount, identity, and remainder remain intact internally until the original pre-tick cleanup runs.

The projection must never normalize or write back into authoritative state. Negative amounts must never be passed to `FluidResource.toStack`, `ResourceHandler`, or any other NeoForge transfer API.

The actual resource identity comparison must use `FluidResource` semantics (fluid plus data components). The adapter must explicitly define how legacy fluid identities map to component-bearing resources; it must not silently compare only registry IDs if components are present.

## 3. External mutation rules

The backing state has two stable indices:

### Water input — index 0

- Insert only; extraction always returns zero.
- Accept only the configured/mapped water resource.
- Apply the current machine-wide, side, and Cover input gates at operation time.
- Delegate tank-local arithmetic to `LegacyBoilerWaterShadowState.fill(...)`.
- Preserve the strict legacy transient-window behavior: if the existing signed amount is -1, a same-fluid fill can observe capacity+1 space before pre-tick cleanup. A capacity hint may understate this one-unit transient acceptance; the actual insert result is authoritative.
- A simulation must not mutate state.

### Steam output — index 1

- Extract only; insertion always returns zero.
- Accept only the configured/mapped steam resource.
- Apply the current machine-wide, side, and Cover output gates at operation time.
- Extraction reduces the authoritative steam amount by the exact returned amount.
- A simulation must not mutate state.

Bulk operations may use NeoForge's default index iteration only if every indexed operation independently validates the index, resource identity, non-negative request amount, direction, and current permissions.

`isValid` and capacity methods are hints only. The indexed insert/extract methods must repeat all security/permission checks immediately before mutation.

## 4. Aggregate snapshot contract

One immutable aggregate snapshot must contain:

- the complete `LegacyBoilerWaterShadowState.Snapshot` (identity, signed amount, signed conversion remainder);
- steam fluid identity;
- steam amount.

Snapshot and restore must be atomic from the adapter's perspective: a rollback restores both tanks and the conversion remainder to the same pre-transaction state.

The journal should snapshot immediately before the first successful mutation at each transaction depth. A rejected or zero-amount operation must not create a snapshot or trigger dirty/sync callbacks.

### Scope boundary

The external fluid handler is permitted to mutate only the fluid aggregate described above. It must not run the boiler tick engine, consume fuel, change temperature, update cooldown, or change boiling/active state.

Therefore those non-fluid tick fields do not belong in this fluid transaction snapshot today. If a later external operation is deliberately allowed to mutate any such field, the aggregate snapshot contract must be expanded before that operation is implemented. Do not snapshot unrelated tick state merely to make the transaction appear machine-wide.

## 5. NeoForge transaction lifecycle

The future adapter should use one boiler-owned `SnapshotJournal<LegacyBoilerFluidState.Snapshot>` (or an equivalent journal with the same semantics), shared by both tank indices and all side-specific views.

For each indexed insert/extract:

1. Validate index, resource, request amount, tank direction, and live machine/side/Cover permission.
2. Determine the actual amount that can be changed without mutating state.
3. If the result is zero, return zero without snapshotting.
4. Call the aggregate journal's snapshot update immediately before the first mutation at this transaction depth.
5. Apply the mutation to the authoritative aggregate state.
6. Return the exact amount changed.

Lifecycle requirements:

- Closing a transaction without commit restores the entire aggregate snapshot.
- A child transaction commit remains provisional; a later parent abort must still restore the parent's original aggregate.
- Root commit retains the mutation.
- Irreversible effects such as block-entity dirty marking, synchronization, and neighbor notifications occur only from the root-commit callback, not during tentative mutation.
- The callback must tolerate the block entity or level no longer being valid; it must not assume the world object is still loaded.
- Normal boiler ticking remains outside external transfer transactions.

A single aggregate journal is required even though the handler exposes two indices. Separate water and steam journals could permit partial rollback when one transaction touches both tanks.

## 6. Side-specific handler views

Expose one shared two-index backing state and stable direction-specific views. Each view delegates reads to the same aggregate and enforces independent insert/extract gates.

- Water index 0: insertion may be allowed; extraction is always denied.
- Steam index 1: extraction may be allowed; insertion is always denied.
- A side that denies insertion must still expose extraction if extraction is allowed, and vice versa.
- A nullable/unsided direction follows the already audited compatibility mapping: bypass physical-side and Cover checks, but retain machine-wide gates.
- Permission changes should be read live by each operation. Capability invalidation is needed when availability or returned-view validity/identity changes, not merely because a live permission predicate changed.

Do not expose a mutable `StacksResourceHandler.set(...)` path to capability consumers. Direct setters bypass transaction journaling.

## 7. Persistence and tick lifecycle

Persistence is separate from transfer transactions. Load and save the authoritative signed water state and steam state through the block-entity lifecycle; do not serialize a negative water amount through a NeoForge `FluidStack` or `ResourceHandler`.

The original pre-tick water cleanup remains a lifecycle operation on `LegacyBoilerWaterShadowState`. It must not run from a capability getter, resource projection, or transaction callback. The signed conversion remainder survives that cleanup.

The regular Advanced Boiler tick sequence remains governed by the existing tick-flow contract. The fluid aggregate is the shared source of truth for water and steam values read or written by the future adapter/orchestrator; no detached handler-side inventory copies are allowed.

## 8. Implementation sequence

1. Implement and test the pure Java `LegacyBoilerFluidState` aggregate and immutable snapshot/restore.
2. Review the aggregate API against the current water shadow state and steam output contracts.
3. Implement the NeoForge journal wrapper and verify nested commit, parent rollback, root commit, and no-op operations against NeoForge 26.3 transaction behavior.
4. Implement the two-index backing handler and direction-specific views.
5. Register the block capability and connect the adapter to the real boiler block entity only after the above contracts pass.

No NeoForge handler, journal, block entity, or capability provider is implemented by this document.

# GT5U Advanced Boiler NeoForge 26.3 Block Fluid Capability Audit

**Status:** Capability discovery and lifecycle audit only. No machine adapter implementation.
**Target project dependency:** NeoForge 26.3.0.113 / Minecraft 26.3.
**NeoForge source cross-check:** `neoforged/NeoForge` branch `26.3.x`.
**Legacy reference:** GT5-Unofficial 5.09.54.277.

## Scope

This document completes the block-capability portion of the Advanced Boiler steam-output adapter investigation. It covers how to query an adjacent block's fluid handler, which side context to pass, how to register block capabilities, and how cached references are invalidated.

It does not implement the boiler block entity, decide the complete shape of the boiler's own water/steam handler, or prove that every third-party handler correctly implements transactions.

## NeoForge fluid block capability

NeoForge's built-in fluid block capability is:

```java
Capabilities.Fluid.BLOCK
```

Its type is:

```java
BlockCapability<ResourceHandler<FluidResource>, @Nullable Direction>
```

This means:

- The exposed API is `ResourceHandler<FluidResource>`, not legacy Forge `IFluidHandler`.
- The query context is a nullable Minecraft `Direction`.
- The capability represents the automation-accessible fluid inventory of a block.
- A query returns the handler instance or `null` if no provider is available.

A direct query has this shape:

```java
ResourceHandler<FluidResource> handler =
    level.getCapability(Capabilities.Fluid.BLOCK, neighborPos, sideFromNeighbor);
```

Do not use deprecated `FluidUtil.getFluidHandler(level, pos, side)`; NeoForge directs callers to query `Capabilities.Fluid.BLOCK` directly.

## Direction mapping: confirmed from the legacy call chain

The direction passed to the adjacent handler is the face of the **neighbor** being accessed, not the boiler's outward direction.

The legacy source chain at the audited revision is explicit:

1. `MTEBoiler.pushSteamToInventories` iterates a `ForgeDirection direction`.
2. `pushSteamToSide(..., direction)` calls `getITankContainerAtSide(direction)` to obtain the adjacent tile's fluid handler.
3. It calls `GTUtility.moveFluid(source, target, drainSide, maxAmount, ...)`.
4. The five-argument `moveFluid` overload delegates to the overload with `fillSide = drainSide.getOpposite()`.
5. The target's simulated and actual `fill` calls both use that `fillSide`.

Therefore, for a boiler at `boilerPos` and an outward direction `outward`:

```java
BlockPos neighborPos = boilerPos.relative(outward);
Direction neighborSide = outward.getOpposite();
ResourceHandler<FluidResource> target =
    level.getCapability(Capabilities.Fluid.BLOCK, neighborPos, neighborSide);
```

This opposite-direction mapping is not a guess based on geometry; it follows directly from the audited GT5U `GTUtility.moveFluid` overload.

The pure Java policy's `Side` enum should remain game-API-free. The adapter should map its side to a Minecraft `Direction`, compute the adjacent position, and pass the opposite direction as the target context.

## Capability registration

A mod that exposes a fluid handler for a block entity registers it during `RegisterCapabilitiesEvent` on the mod event bus:

```java
event.registerBlockEntity(
    Capabilities.Fluid.BLOCK,
    BOILER_BLOCK_ENTITY_TYPE,
    (boiler, side) -> boiler.getFluidResourceHandler(side)
);
```

The provider receives a nullable side context and may return `null` when that side does not expose the capability.

For the boiler's own implementation, the provider must eventually preserve the legacy distinction between water input and steam output. The legacy boiler has separate fillable water and drainable steam stacks; `isDrainableStackSeparate()` is true. A single NeoForge `ResourceHandler` may expose multiple indexed tanks, or the adapter may expose a deliberately restricted view, but that design must be decided from the complete legacy side/cover rules and the intended automation contract. This audit does not prescribe a one-slot handler.

## Caching and lifecycle

### Direct lookup versus cache

- `Level#getCapability` is the straightforward lookup and is suitable for an initial adapter implementation.
- NeoForge recommends `BlockCapabilityCache` for repeated queries at a fixed position and context, such as a machine checking adjacent handlers every tick.
- A cache is created with a `ServerLevel`, the capability, the target position, and the side context. It is therefore a server-side mechanism, consistent with the legacy boiler's server-only transfer path.
- `BlockCapabilityCache#getCapability()` returns `null` when the target position is not loaded. The cache checks `level.isLoaded(pos)`; it does not force-load the target chunk.

A cache should be held by the querying block entity for each fixed neighbor position/context if profiling or the regular tick pattern justifies it. Do not build caches on the client or assume the cache API accepts a general client `Level`.

### Invalidation rules

NeoForge automatically invalidates block capability caches for:

- chunk load and unload;
- block entity load and unload;
- block entity placement and destruction.

A provider/mod must call `level.invalidateCapabilities(pos)` when a previously returned capability becomes invalid or a new capability becomes available in other cases, for example:

- a machine configuration changes which sides expose a handler;
- a plain block changes state or is placed/removed;
- a provider replaces the handler instance or changes its availability.

The boiler's own provider must issue invalidation when its exposed capability availability/view changes. A target mod is responsible for invalidating its own provider when its behavior changes.

### Invalidation listener constraints

The cache listener has important restrictions:

- Do not call `getCapability()` from inside the invalidation listener. The target may not be ready to query yet.
- Do not perform level/world access from the listener; it may run while a chunk is unloading.
- The listener is not notified until `getCapability()` has been called at least once.
- After invalidation, call `getCapability()` again on a later tick to refresh the cached value and re-arm notifications.

A listener should therefore only mark local adapter state as needing refresh or wake the next machine tick. Resolve the handler again during normal server tick processing.

## Proxyability

Block capabilities are non-proxyable by default. The built-in fluid capability is not automatically safe to forward through arbitrary blocks. The boiler adapter should query the immediate adjacent position and should not assume a pipe or proxy block forwards the fluid capability unless that block explicitly implements such forwarding.

This is separate from normal fluid-pipe behavior: a pipe can expose its own fluid handler at its own position without acting as a transparent capability proxy.

## Compatibility mapping to the pure Java policy

For each eligible `LegacyBoilerSteamOutputTransfer.Side`:

1. Map the legacy side to the corresponding Minecraft `Direction`.
2. Compute the adjacent block position from the boiler position.
3. Query `Capabilities.Fluid.BLOCK` at that position using `outward.getOpposite()`.
4. If the capability is `null`, treat it as no target and continue.
5. Use the returned `ResourceHandler<FluidResource>` as the target port for that side.
6. Keep the existing pure policy responsible for direction order, front/bottom exclusions, and recalculating `max(1, remaining / 2)`.

The adapter must not change the pure policy's side order or move the steam push to after production. The audited GT5U tick path pushes existing steam before production.

## Remaining implementation questions

These are not blockers for capability discovery, but they must be answered before declaring the adapter complete:

1. Define the boiler's own fluid handler view: indexed water and steam tanks, side restrictions, and cover interaction.
2. Define how `FluidResource` identity for GT steam is created and retained, including data components.
3. Decide the previously documented mismatch policy: exact legacy ignored-fill-return behavior versus conserving transactional rollback.
4. Test absent capability, partial acceptance, unloaded neighbor, capability invalidation/replacement, and a handler whose simulation and actual operation disagree.
5. Verify exact 26.3.0.113 source/API signatures against the project's pinned source JAR during implementation; this document's upstream code cross-check uses the NeoForge `26.3.x` source branch.

## Conclusion

The target lookup is `level.getCapability(Capabilities.Fluid.BLOCK, neighborPos, outward.getOpposite())`. The opposite-side argument is directly supported by the GT5U `moveFluid` source, which fills the adjacent handler on `drainSide.getOpposite()`.

For the initial adapter, direct lookup is the simplest correct integration. If the boiler performs this lookup every server tick, a per-neighbor `BlockCapabilityCache` is the intended optimization, provided invalidation and listener restrictions are respected. Capability discovery and lifecycle behavior are now understood well enough to design the adapter boundary; the boiler's own exposed tank view and transaction mismatch policy remain separate design decisions.


## Side-specific provider design (NeoForge 26.3.x source verified)

The pinned project dependency is NeoForge 26.3.0.113. The upstream `26.3.x` sources for `Capabilities`, `RegisterCapabilitiesEvent`, `IBlockCapabilityProvider`, `BlockCapabilityCache`, `BlockCapability`, and `ILevelExtension` were inspected for this design.

### Provider receives the queried block face

The built-in capability is declared as:

```java
BlockCapability<ResourceHandler<FluidResource>, @Nullable Direction>
```

The registered block-entity provider receives that nullable `Direction` as its second argument:

```java
event.registerBlockEntity(
    Capabilities.Fluid.BLOCK,
    BOILER_BLOCK_ENTITY_TYPE,
    (boiler, queriedSide) -> boiler.getFluidHandlerView(queriedSide)
);
```

Here `queriedSide` is the face of the boiler block being queried. It is the correct input for the boiler's own legacy side policy. It is not the outward direction used to locate a neighboring target during steam push.

### Do not use Provider null as a directional insert/extract denial

A single fluid capability represents both boiler tanks. A given side may permit water insertion while denying steam extraction, or the reverse, depending on its Cover. Returning `null` for that side would hide both operations together and would not reproduce the legacy contract.

The provider should normally return a side-specific `ResourceHandler<FluidResource>` view. That view delegates storage reads to the shared two-tank backing handler, but gates the actual operations independently:

| Operation | Required checks |
|---|---|
| Insert into water index | machine timer/data/input-enabled gates; side input predicate; Cover `letsFluidIn(resource fluid)` |
| Extract from water index | reject |
| Insert into steam index | reject |
| Extract from steam index | machine timer/data/output-enabled gates; side output predicate; Cover `letsFluidOut(resource fluid)` |

The view must also reject invalid indices. The backing inventories remain shared; the per-side views must not create independent copies of the water or steam amounts.

The `ResourceHandler` contract treats `isValid` and capacity as hints, not as authorization to mutate. The final insert/extract methods must enforce the side policy even if a caller skips hints or calls the handler directly.

### Nullable Direction and legacy UNKNOWN

NeoForge's side context is nullable. GT5U uses `ForgeDirection.UNKNOWN` as a real unsided call path, and that path bypasses the legacy side predicate and Cover fluid checks while retaining the timer, data-access, and global input/output-disabled gates.

For compatibility, the target's `null` side should be deliberately mapped to that unsided policy:
- skip side-specific input/output predicates;
- skip Cover fluid permission checks;
- retain machine-wide availability and input/output-disabled checks.

Do not map `null` to DOWN, UP, or an arbitrary default side. This mapping is a compatibility decision to preserve GT5U UNKNOWN semantics, not an assumption that all unsided NeoForge callers are internal.

### Return null only when the capability itself is unavailable

The provider should return `null` when the boiler does not expose a fluid capability at all (for example, the block entity is unavailable or removed). A side that denies one operation should still return its handler view so the other operation can remain available.

A future implementation may use one stable view per direction plus one unsided view. Those views should consult current machine/Cover state when operations occur rather than snapshotting permissions at construction. If the provider changes whether a capability exists or replaces an already-returned view with an incompatible object, it must call `level.invalidateCapabilities(pos)` (or the block entity's equivalent invalidation method) as required by the API.

### Cache and invalidation implications

The 26.3.x `BlockCapabilityCache` is explicitly bound to one target position and one context. A consumer therefore needs a distinct cache for each neighbor position/face pair it repeatedly queries. Its listener must only mark local state for later refresh; it must not query the capability or access the level from inside the listener.

For the boiler's own provider, Cover changes that only alter permissions can be handled by a stable view that reads live Cover state. If a Cover change alters whether the capability is exposed or changes the identity/validity of the returned view, invalidate the boiler position. The same rule applies to any machine configuration that changes capability availability.

### Design decision

Use:
1. one registered `Capabilities.Fluid.BLOCK` provider for the boiler block-entity type;
2. a shared two-index backing handler (water input index 0, steam output index 1);
3. a side-specific `ResourceHandler<FluidResource>` view for each queried `Direction`, plus an unsided/null view;
4. independent insert and extract gates inside those views;
5. live reads of machine and Cover permissions;
6. explicit capability invalidation only when capability availability or returned-view validity changes.

This preserves the legacy distinction between tank identity, operation direction, and side/Cover permission. It does not yet implement the handler or the boiler block entity.


## ResourceHandler method and transaction boundary audit

This section audits the NeoForge 26.3.x `ResourceHandler`, `StacksResourceHandler`, `FluidStacksResourceHandler`, `SnapshotJournal`, and `Transaction` source at upstream commit `84d0790633e43646f2710751a1a73db8fab30671`.

### 1. Methods the side-specific view must account for

The public `ResourceHandler<T>` surface consists of:

- `size()`
- `getResource(index)`
- `getAmountAsLong(index)` and the non-extendable `getAmountAsInt(index)`
- `getCapacityAsLong(index, resource)` and non-extendable int overload
- `isValid(index, resource)`
- `insert(index, resource, amount, transaction)`
- bulk `insert(resource, amount, transaction)`
- `extract(index, resource, amount, transaction)`
- bulk `extract(resource, amount, transaction)`

The bulk insert/extract methods are interface defaults. They validate non-empty resource and non-negative amount, then iterate indices and invoke the index-specific methods. Therefore a side view can safely inherit the bulk defaults only if its index-specific `insert` and `extract` methods enforce every permission and storage rule.

The side view must also implement `isValid` and `getCapacityAsLong` consistently as hints. A denied operation should report invalid / zero capacity where practical, but this is not the security boundary: the actual index-specific insert/extract must re-check permission at call time. NeoForge explicitly documents `isValid` and capacity as hints; callers must rely on the actual insert/extract result.

Read methods should report the shared underlying tank state. Do not make a separate copy of amounts per side. This matches the old tank-info behavior, which exposed tank contents separately from the fill/drain permission checks.

### 2. Two-index storage semantics

The backing handler has two stable indices:
- index 0: water input tank, water resource only, insertion only, capacity = legacy `getCapacity()`;
- index 1: steam output tank, steam resource only, extraction only, capacity = legacy `getSteamCapacity()`.

The side view must preserve those direction restrictions independently from the side/Cover policy. In particular:
- index 0 extraction always returns 0;
- index 1 insertion always returns 0;
- invalid indices are rejected consistently;
- water insertion checks the water fluid identity and current side input permission;
- steam extraction checks the steam fluid identity and current side output permission.

Resource equality must use `FluidResource` identity (fluid plus data components), not only the fluid registry key. Whether GT5U water/steam should accept non-default data components remains a compatibility policy to define; it must not be silently treated as equal if the legacy fluid identity has no equivalent metadata.

### 3. Why independent per-tank snapshots are insufficient

A single external transfer transaction may touch both indices or may be nested with other boiler state changes. The compatibility state is not just the two visible fluid amounts. It also includes legacy state that affects future behavior, such as:
- water conversion remainder (`mExcessWater`);
- processing energy / fuel energy;
- temperature and cooldown timer;
- boiling state and related tick state;
- any additional fields introduced into the pure boiler snapshot/result contract.

If the fluid handler changes the water amount but the pure legacy state remains outside the transaction journal, rollback can leave the model and exposed tanks inconsistent. Conversely, independently journaling water and steam while sharing model fields can produce a partial rollback if both are modified in one transaction.

The transaction journal should therefore snapshot one coherent aggregate representing every mutable field that can be changed by the transactional fluid operation. At minimum, the journal's snapshot and restore methods must cover every field the adapter is permitted to mutate. If external fluid operations only mutate tank contents, the snapshot can be narrower, but the adapter must prove that no conversion remainder, energy, temperature, or other model field is changed as a side effect.

### 4. Recommended transaction owner

Use one boiler-owned journal (a `SnapshotJournal<LegacyBoilerTransactionalState>` or equivalent) as the transaction boundary for the aggregate legacy state.

For each successful index-specific insert/extract:
1. validate index, resource, amount, tank direction, and current side/Cover/global permission;
2. compute the accepted/extracted amount without mutating state;
3. if the amount is zero, return zero and do not create a snapshot;
4. call `updateSnapshots(transaction)` on the aggregate journal immediately before the first mutation in this transaction depth;
5. apply the mutation to the authoritative boiler state;
6. return the exact amount changed.

On abort, `revertToSnapshot` restores the entire aggregate state, including both tank inventories and any coupled legacy fields. On root commit, keep the new state and use `onRootCommit` only for irreversible effects such as marking the block entity changed, syncing data, or notifying neighbors. These effects must not happen before the root commit if they cannot themselves be rolled back.

Nested commits remain provisional: child commit merges its snapshot responsibility into the parent, and a later parent abort must still restore the pre-parent state. The aggregate journal must preserve that behavior.

### 5. Critical distinction: transactional API state vs normal boiler ticking

The boiler tick engine and external transfer operations must share one authoritative state, but the transaction journal must not accidentally make the whole server tick transactional. The normal tick sequence (cooldown, steam push, production, overflow, fuel update, heat-up) is ordinary machine progression and should remain outside external transfer transactions unless a concrete caller explicitly supplies a transaction for it.

The adapter should not call the legacy tick engine from inside `insert` or `extract`. Fluid transfer only changes the fields defined by the legacy fill/drain semantics. The regular machine tick later consumes those values according to the existing tick-order contract.

### 6. Direct mutation and serialization boundary

`StacksResourceHandler.set(index, resource, amount)` is a direct mutation helper, not a transactional operation:
- it validates non-negative amount and empty-resource constraints;
- it changes the stack immediately;
- it invokes `onContentsChanged` immediately;
- it does not accept a `TransactionContext` and does not create a rollback snapshot.

Consequently, do not expose the backing handler's mutable `set` method to capability consumers. If using `FluidStacksResourceHandler` internally, keep it private and ensure all externally visible mutations go through the transactional wrapper. Do not use direct `set` during a transaction as a substitute for `insert` or `extract`.

Likewise, serialization/deserialization is persistence, not transactional transfer. Load the saved boiler state through the block entity lifecycle, outside active transfer transactions, and keep the existing GT5U distinction between water and steam inventories.

### 7. Current implementation decision

The preferred implementation boundary is now:

- **Authoritative boiler state:** one state object containing water, steam, and all legacy fields required by the model.
- **Aggregate transaction journal:** snapshots/restores that state coherently.
- **Backing handler:** two-index read/capacity/resource mapping; not exposed directly.
- **Side view:** delegates reads to the backing state; enforces per-index direction and live side/Cover/global gates in `isValid`, capacity hints, and especially actual insert/extract.
- **Commit callback:** mark changed / synchronize only after root commit.
- **Tick engine:** remains outside transfer transactions and retains the audited GT5U order.

No adapter implementation is added by this audit. Before coding, one remaining design issue must be resolved: the legacy boiler's water-scarcity arithmetic can produce a negative water amount and negative remainder, while NeoForge `ResourceHandler` requires non-negative amounts and `FluidResource.toStack` cannot represent negative quantities. This needs an explicit shadow-state / compatibility policy rather than normalization hidden inside the handler.


## Signed legacy water state and non-negative NeoForge projection

This section resolves the representation boundary by tracing the original GT5U source at `5.09.54.277`:

- `MTEBoiler.produceSteam(int)`
- `MTEBoiler.saveNBTData/loadNBTData`
- `MTEBasicTank.onPreTick`
- `MTEBasicTank.fill(FluidStack, boolean)`
- `MTEBasicTank.getFluidAmount/getFillableStack`

### 1. The negative amount is a real transient legacy state

The source production arithmetic is:

```java
mExcessWater -= aAmount;
if (mExcessWater < 0) {
    int tWaterToConsume = -mExcessWater / GTValues.STEAM_PER_WATER;
    mFluid.amount -= tWaterToConsume;
    mExcessWater += GTValues.STEAM_PER_WATER * tWaterToConsume;
}
```

It does not clamp `mFluid.amount` to zero and does not clear the fluid stack in this method. The negative amount and signed remainder are therefore part of the actual intermediate machine state.

For the audited Tier 1 scarcity case (375 steam, 1 water, remainder 0), integer arithmetic consumes 2 water and produces:
- legacy water amount = -1;
- legacy remainder = -55.

### 2. The original machine repairs the water stack on its next pre-tick

On the server, `MTEBasicTank.onPreTick` checks:

```java
if (isFluidChangingAllowed() && getFillableStack() != null
    && getFillableStack().amount <= 0) {
    setFillableStack(null);
}
```

Thus the negative stack is not immediately normalized by production. It is cleared by the subsequent server pre-tick, provided fluid changing is allowed (the default tank implementation allows it).

This ordering matters. The adapter must preserve the signed state between production and that cleanup point, while exposing only legal non-negative quantities through NeoForge's fluid transfer API.

### 3. There is a narrow external-fill edge before cleanup

The legacy `MTEBasicTank.fill` implementation rejects non-positive incoming amounts, but if an existing fillable stack is present and fluid-equal, it calculates:

```java
int space = getCapacity() - getFillableStack().amount;
```

It does not clamp the existing amount before calculating space. Therefore, while the transient legacy amount is -1, a same-fluid external fill sees `capacity + 1` space. A one-unit fill can be accepted and bring the internal amount to zero; the following pre-tick then clears that zero-amount stack.

This is an unusual timing-dependent legacy behavior. A compatibility implementation must not accidentally claim exact legacy equivalence while replacing it with a simpler `capacity - max(0, amount)` calculation. It must either model this edge explicitly or document that external transfers are intentionally unavailable during the transient signed-state window.

### 4. Recommended representation: signed legacy state plus a non-negative API projection

Keep two distinct concepts:

**Authoritative legacy state**
- `legacyWaterAmount`: signed integer, preserving the exact GT5U amount including transient negative values;
- `legacyWaterFluidIdentity`: retained while the legacy stack exists, including the transient non-positive window;
- `excessWaterRemainder`: signed integer, preserving the original conversion arithmetic;
- `legacySteamAmount`: non-negative output quantity (the audited steam output path does not require negative amounts).

**NeoForge-facing projection**
- exposed water amount = `max(0, legacyWaterAmount)`;
- exposed steam amount = `legacySteamAmount`;
- never construct a `FluidStack` or call ResourceHandler operations with a negative amount;
- a non-positive legacy water amount is exposed as an empty resource/zero amount, even while its signed backing state remains available to the legacy tick/cleanup logic.

The projection is read-only as a representation rule; it must not overwrite or normalize the authoritative signed state. A new water insertion must update the authoritative legacy amount according to the chosen compatibility policy, not mutate a detached projected stack.

### 5. Transaction journal must include the signed state

The aggregate journal snapshot must include:
- signed `legacyWaterAmount`;
- legacy water fluid identity / null-vs-present state;
- signed `excessWaterRemainder`;
- steam amount and identity;
- any other legacy fields modified by the transfer operation.

A rollback must restore both the signed backing state and the public projection source together. Root-commit callbacks may mark the block entity changed or synchronize the new state; those side effects must not happen before commit.

### 6. Persistence and lifecycle

GT5U saves `mFluid` through `FluidStack.writeToNBT` and separately saves `mExcessWater` as an integer. Loading restores the water stack and the signed remainder independently. The target implementation should likewise persist the signed legacy amount and remainder as explicit legacy state rather than trying to serialize the negative amount through a NeoForge `FluidStack` or `ResourceHandler`.

The pre-tick cleanup remains a legacy lifecycle operation: when `isFluidChangingAllowed()` is true and signed water amount is non-positive, clear the legacy water identity and amount at the same point in the tick lifecycle as the source. Do not perform this cleanup inside a ResourceHandler getter, capability query, or arbitrary render/sync callback.

### 7. Compatibility decision still required before implementation

There are two possible policies for the narrow interval between production and the next pre-tick:

A. **Strict legacy transfer window:** reproduce the source fill arithmetic against the signed backing amount, including the temporary `capacity + 1` headroom and the zero-then-clear case. This is closest to source behavior but means the NeoForge handler's reported capacity is only a hint and may understate the amount accepted by the legacy operation during that transient interval.

B. **Closed transfer window:** while signed water amount is non-positive, reject external water insertion until the next legacy pre-tick clears the stack. This is simpler and maintains a clean non-negative storage contract, but it intentionally changes the narrow legacy timing behavior described in section 3.

Do not choose policy B silently. The default compatibility target remains strict legacy behavior unless an explicit design decision changes it. In either policy, the negative amount and remainder must remain preserved internally until the source cleanup point.

### 8. Resolved architecture

The selected data model is now:
1. authoritative signed legacy water state and signed remainder;
2. non-negative NeoForge fluid projection;
3. explicit source-timed pre-tick cleanup;
4. aggregate transaction journal over both legacy and exposed state;
5. side-specific handler views that never pass negative amounts to NeoForge.

No machine adapter implementation is added by this audit.

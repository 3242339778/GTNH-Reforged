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

# Legacy Power Graph — NeoForge Host Adapter Audit

## Baseline and scope

This audit maps the pure-Java graph core to the actual GT5-Unofficial **5.09.54.275** host-side cable and world behavior. It is a design gate for the NeoForge 26.3 adapter; it does **not** claim that a runtime adapter or in-game cable implementation already exists.

Primary upstream sources:

- [BaseMetaPipeEntity.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/metatileentity/BaseMetaPipeEntity.java)
- [BaseTileEntity.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/metatileentity/BaseTileEntity.java)
- [MetaPipeEntity.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/metatileentity/MetaPipeEntity.java)
- [MTECable.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/metatileentity/implementations/MTECable.java)
- [GenerateNodeMap.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/GenerateNodeMap.java)
- [GenerateNodeMapPower.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/GenerateNodeMapPower.java)
- [Node.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/Node.java)
- [NodePath.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/paths/NodePath.java)

## Confirmed host-side state model

### Cable tile state

Upstream `BaseMetaPipeEntity` owns these distinct references:

- `node`: a graph node when this cable is a node endpoint.
- `nodePath`: the path binding when this cable is an intermediate cable in a path.
- `nodePathConnections`: the connection-bit snapshot captured when `setNodePath` is called.
- `occupiedConnectedSides`: connected sides that had an actual neighboring tile when the graph last walked this node.

Its `getNodeMap()` is deliberately not just `getNode()`: it returns the node when present, otherwise the owner node returned by `nodePath.getNodeMap()`. The new host implementation must preserve this precedence and fallback exactly.

`NodePath.processPipes()` binds every cable in the ordered path to that path. `BaseMetaPipeEntity.setNodePath(path)` also snapshots the current connection bits (or zero if the path/meta tile is null). Clearing a path detaches a cable only if that cable still points to the exact path object being cleared.

### Graph invalidation and rebuild timing

- Cable topology changes invalidate the shared graph root, rather than immediately rebuilding it.
- The root stores validity, topology version, and topology-change server tick.
- Refresh is due only when invalid and at least 10 server ticks have elapsed.
- A removed connection can invalidate an affected edge path; the pipe's saved `nodePathConnections` determines whether an intermediate path was affected.
- On invalidation/removal, graph cleanup and path cleanup are different operations and must not be conflated.
- The source transfer path checks the root refresh condition before deciding whether a graph must be built.

The pure-Java graph state already models shared root identity, topology version, change tick, and the 10-tick eligibility boundary. The host still must invoke invalidation at the correct cable lifecycle points.

### NeoForge 26.3 world lookup prototype

`NeoForgeLegacyPowerNeighborAccess` is the first Minecraft-facing adapter. It holds a `ServerLevel`, immutable origin `BlockPos`, and the pure-Java `LegacyPowerNeighborCache`. It maps the six physical legacy directions to Minecraft `Direction`, detects chunk-border crossings using block coordinates, and obtains the target chunk only through `ServerLevel.getChunkSource().getChunkNow(chunkX, chunkZ)`. A null result becomes `UNLOADED`; a present `LevelChunk` is queried with `LevelChunk.getBlockEntity(targetPos)`, yielding loaded-empty or the exact `BlockEntity` object. It never calls a chunk-loading `getChunk(...)` method or `Level.getBlockEntity(...)` for this lookup.

The adapter also supplies `BlockEntity.isRemoved()` and exact `getBlockPos()` matching to the legacy cache. It preserves the legacy coordinate-mismatch behavior (return null-equivalent without evicting the cached object). This is a compile-verified host adapter prototype, not yet validated in a running dedicated server or GameTest. Callers must invoke it on the logical server thread.

`LegacyPowerCableHostState` now bundles one stable `LegacyPowerCableState` and one stable `LegacyPowerNeighborCache` for a future cable BlockEntity to own. It exposes delegated lifecycle hooks: `onValidate()`, `onInvalidate()`, `onChunkUnload()`, and `onAdjacentBlockChange()` clear only negative-cache markers; `onPositionChanged()` clears every cache entry. Immediate graph cleanup and delayed topology invalidation are explicit separate methods, so a cache lifecycle callback cannot accidentally clear a graph. `createNeighborAccess(level, position)` builds the Minecraft-facing adapter over the bundle's own cache. The bundle is a component, not itself a registered BlockEntity.

### World and neighbor access

The original graph walker asks the pipe for the tile on a connected side. It does not imply that a missing or unloaded neighbor is an air block.

Do not force-load chunks to answer graph questions. The exact legacy behavior is operation-specific:

- `BaseTileEntity.getTileEntityAtSide` uses a six-entry neighbor cache. On a cross-chunk query it clears that side's cache first, specifically so a cached "no tile" marker cannot survive chunk loading. With the default `ignoreUnloadedChunks = true`, it returns null if the target across the chunk border is not loaded. Within the same chunk it reads the world directly. It also clears and retries an invalid cached tile entity.
- That method returns null both for a loaded position without a tile entity and for an unloaded cross-chunk position. The graph walk therefore historically treats both as "no adjacent tile" at this API boundary; a new implementation must not invent a different graph endpoint rule merely because its internal lookup can tell the cases apart.
- `MetaPipeEntity.connect(side)` has a separate, important tri-state result: positive means connected, zero means not connectable, and **-1 means the target chunk is unloaded**. `checkConnections()` disconnects a side only when `connect(side) == 0`; it preserves the existing connection when the result is -1. Collapsing unloaded into ordinary non-connectable would silently disconnect cables at chunk borders.
- `GenerateNodeMapPower.getAdjacentTileEntityIfLoaded` checks `blockExists` only when the adjacent position crosses a chunk boundary; otherwise it calls `getTileEntity` directly. This helper is used in the power consumer discovery path.
- IC2 sink discovery passes a null adjacent tile to `acceptsEnergyFrom` when that cross-chunk target is unloaded. Do not replace this with forced chunk access or a speculative lookup.

The NeoForge host boundary should expose enough information to preserve all these cases, for example a lookup result with `LOADED_WITH_BLOCK_ENTITY`, `LOADED_WITHOUT_BLOCK_ENTITY`, and `UNLOADED`. The pure-Java `LegacyPowerNeighborCache` now models the six per-direction cache slots, the unknown/known-empty/cached-object states, cross-chunk invalidation, invalid cached-object retry, coordinate-mismatch null results without eviction, and the two upstream cache-clear operations. It deliberately receives lookup results from the host rather than accessing a world itself. The legacy graph-walk adapter can deliberately map the latter two to the same null behavior where the 1.7.10 caller did so, while connection checking must retain the distinct unloaded result (-1 semantics).

### Neighbor cache and unloaded-chunk semantics

The world adapter must not treat all null-like lookups as interchangeable:

| Upstream call path | Loaded empty position | Unloaded cross-chunk position | Required behavior |
|---|---|---|---|
| `BaseTileEntity.getTileEntityAtSide` | null | null when `ignoreUnloadedChunks` is true | Same graph-facing null result, but clear cross-chunk cache before returning |
| `MetaPipeEntity.connect(side)` | 0 if no valid connection | -1 | `checkConnections` disconnects only on 0; -1 preserves the old bit |
| `GenerateNodeMapPower.getAdjacentTileEntityIfLoaded` | world lookup result (usually null) | null | Check loaded status only across chunk coordinates |
| IC2 sink discovery | actual adjacent tile | null passed to `acceptsEnergyFrom` | Preserve the IC2 callback's null argument |

`BaseTileEntity` also clears cached null markers on neighbor-block changes, tile validation, invalidation, and chunk unload. The cross-chunk cache is invalidated before the loaded check because loading a neighboring chunk does not necessarily issue the block update that would otherwise clear a cached empty marker.

This cache is a host-tile concern, not a graph-core concern. Keep it out of `LegacyPowerTopologyWalker`; `LegacyPowerNeighborCache` is the pure-Java behavioral model, while the future ported cable/base block entity must own one instance and invoke `clearEmptyMarkers()` for adjacent-block changes, `validate()`, `invalidate()`, and chunk unload, and `clearAll()` when its coordinates change.

### Connection and occupied-side semantics

- `MetaPipeEntity.isConnectedAtSide` reads the connection bitmask using the legacy direction flag.
- The six topology slots use the fixed legacy direction ordinal order.
- `ForgeDirection.flag` is a separate bitmask and must never be substituted with ordinal.
- `UNKNOWN` is a transfer sentinel, not a physical side and not a topology-array index.
- During node expansion, the incoming side is included in occupied-side flags, and a side is marked occupied only after an adjacent tile is actually found.
- After expansion, the pipe stores occupied-side flags and reloads its locks.
- Neighbor changes can add/remove an endpoint while leaving the connection bitmask unchanged. The server-side cable logic detects this by rescanning occupied connected sides and invalidates the graph if the mask changes.

This endpoint-presence behavior is important: a port must not rely only on connection-bit changes to detect topology changes.

### Power-specific node and consumer factories

`GenerateNodeMapPower` starts the source node at value **1** and only treats a `BaseMetaPipeEntity` whose meta tile is an `MTECable` as a traversable cable.

Its node factories are behaviorally distinct:

- Ordinary cable endpoint: `PowerNode`.
- One-connection dead end: `EmptyPowerConsumer`, which is also appended to the consumer list.
- Consumer endpoints are selected in this order: GT base meta tile, `IEnergyConnected`, Galacticraft handler, IC2 sink, then optional CoFH RF receiver.
- GT and `IEnergyConnected` consumers are gated by `inputEnergyFrom(side, false)`.
- IC2 sink acceptance receives the adjacent tile on the side supplied by the legacy helper, with the unloaded cross-chunk behavior described above.
- The RF branch is conditional on `GregTechAPI.mOutputRF`.

A generic NeoForge energy capability is not a drop-in replacement for these consumer types. Each old consumer family needs an explicit behavior adapter and tests for direction, acceptance, returned amperage, and invalidation.

## NeoForge 26.3 world-query adapter

The project is pinned to NeoForge **26.3.0.113**. The active adapter in the current source tree is:

`com.gtnh.reforged.legacy.power.bridge.neoforge.NeoForgeLegacyPowerNeighborAccess`

Its lookup sequence is:

1. Convert one of the six `LegacyPowerDirection` values to Minecraft `Direction`; reject `UNKNOWN`.
2. Compute the adjacent immutable `BlockPos` and detect whether its X/Z chunk coordinates differ from the origin.
3. Consult `LegacyPowerNeighborCache`. Cross-chunk lookup invalidates that direction's cache entry first. A cached removed BlockEntity is discarded; a cached object whose position mismatches returns the legacy null-equivalent result without eviction.
4. On a cache miss, call `ServerLevel.getChunkSource().getChunkNow(chunkX, chunkZ)`. A null result is `UNLOADED`; it is not cached as empty and does not trigger chunk loading.
5. For a loaded `LevelChunk`, call `LevelChunk.getBlockEntity(target)`, then cache and return either the exact BlockEntity object or a loaded-empty result.

This adapter uses the non-loading chunk lookup path and is explicitly server-side. Call it on the logical server thread. It does not itself mutate graph state.

**Package-boundary refactor completed:** this class now resides in `legacy.power.bridge.neoforge`, the Layer 3 Minecraft Bridge namespace. `LegacyPowerCableHostState` no longer imports or constructs Minecraft adapters; a future Layer 4 host should construct the bridge with `hostState.neighborCache()`.

The duplicate `legacy.power.neoforge.NeoForgePowerNeighborAccess` implementation (which used `ServerLevel.isLoaded/getBlockEntity`) has been removed. There is now one authoritative implementation, using `getChunkSource().getChunkNow(...)` and `LevelChunk.getBlockEntity(...)`.

The current adapter is a narrow adjacent-BlockEntity lookup, not a complete implementation of `LegacyPowerTopologyAccess`: cable classification, connection bits, node bindings, map membership, energy-source detection, and graph clearing still belong to the future GT cable host.

Pure-Java result contracts:
- `LegacyPowerNeighborLookup` retains loaded-with-BlockEntity, loaded-empty, and unloaded states while keeping the tile handle opaque.
- `LegacyPowerConnectionResult` models the legacy positive / zero / -1 connection result and preserves the existing connection bit on `UNLOADED`.
- `LegacyPowerNeighborCache` models the six per-direction cache slots, unknown/known-empty/cached-object states, cross-chunk invalidation, invalid cached-object retry, coordinate-mismatch null results without eviction, and the two upstream cache-clear operations.

## 26.3 registration and GameTest integration audit

The current mod entry point receives the NeoForge mod event bus, but only registers `ModItems.ITEMS`. There is not yet a block registry, a block-entity-type registry, or a concrete cable block/entity. Keep that fact explicit: the host-state bundle is not a registered game object.

The official NeoForge 26.3.x `DeferredRegister` source documents the expected registration shape:

- Blocks: `DeferredRegister.createBlocks(MOD_ID)`.
- Block entity types: `DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID)`.
- A block entity type is created from `BlockEntityType.Builder.of(factory, validBlock).build(null)`.
- Each deferred register is attached to the mod event bus.

For in-game tests, do not port Forge's legacy `@GameTestHolder` / `@GameTest` annotations by assumption. The 26.3.x NeoForge source exposes `RegisterGameTestsEvent` on the mod bus. It registers `GameTestInstance` values into the test-instance registry; the event is only run when `GameTestHooks.isGametestEnabled()` is true. NeoForge's own 26.3.x test sources use the newer `net.neoforged.testframework` API for their annotated test suites, but that framework is a separate project dependency in NeoForge's own test build and is not currently declared by this mod.

Practical consequence: first implement and compile the smallest real Block + BlockEntity registration using this project's pinned 26.3.0.113 artifacts. Then choose one test route deliberately:
1. Register a vanilla `GameTestInstance` from `RegisterGameTestsEvent`, with its exact 26.3.0.113 constructor/data contract verified from the local dependency sources; or
2. Add and configure the NeoForge testframework dependency intentionally if using its `DynamicTest` / `@TestHolder` authoring model.

Do not invent a GameTest constructor or add the internal testframework as a production dependency just because NeoForge's own test repository uses it.

Primary 26.3.x source references:
- [DeferredRegister.java](https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/registries/DeferredRegister.java)
- [RegisterGameTestsEvent.java](https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/event/RegisterGameTestsEvent.java)
- [GameTestHooks.java](https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/gametest/GameTestHooks.java)
- [NeoForge ItemTests.java](https://github.com/neoforged/NeoForge/blob/26.3.x/tests/src/main/java/net/neoforged/neoforge/debug/item/ItemTests.java)

## Mapping to the current pure-Java contracts

| Upstream responsibility | Current pure-Java boundary | Host implementation requirement |
|---|---|---|
| Cable `node` binding | `LegacyPowerNodeBinding` / `LegacyPowerCableState` | Store/retrieve the same node object by identity |
| Cable `nodePath` binding | `LegacyPowerPipeBinding` / `LegacyPowerCableState` | Store/retrieve the exact path object and update connection snapshot |
| `getNodeMap()` fallback | `LegacyPowerTopologyAccess.getNodeMapOwner` / `LegacyPowerTransferAccess.getNodeMapOwner` | Prefer node; otherwise resolve the bound path's graph owner |
| Six-way connection state | `LegacyPowerTopologyAccess.isConnected` / `LegacyPowerConnectionResult` | Use the real cable connection state and correct direction mapping; preserve the existing bit when neighbor lookup is unloaded |
| Neighbor lookup | `LegacyPowerNeighborLookup`, `LegacyPowerNeighborCache`, `NeoForgePowerNeighborAccess` | Keep loaded-empty and unloaded distinct at the host boundary; map them to legacy null only in the graph-walk caller |
| Pipe classification | `LegacyPowerTopologyAccess.isPipe` | Only GT power cables are traversable, not every pipe/fluid pipe |
| Power-specific node factories | `LegacyPowerNodeBuildAccess` | Construct the right pipe/dead-end/consumer node types |
| Path creation | `LegacyPowerNodeBuildAccess.createPath` | Bind every cable in order; preserve identity and owner |
| Lock refresh | `reloadPipeLocks` / `reloadPathLocks` | Recompute real cable-side lock state after graph installation |
| Cable connection snapshot / occupied sides | `LegacyPowerCableState` | Preserve bind-time connection bits and detect endpoint occupancy changes independently; neighbor-cache invalidation belongs to the host tile layer |
| Cable invalidation lifecycle | `LegacyPowerCableState` | Distinguish shared-root invalidation, immediate graph clearing, and affected-path invalidation |
| Consumer injection | `LegacyPowerConsumerAccess` | Preserve each consumer family's original acceptance and injection behavior |
| Server tick and side | `LegacyPowerTransferAccess` | Use logical server state and the same server tick source as graph invalidation |

## Important integration constraints

1. `LegacyPowerCableState` now models the graph-related `BaseMetaPipeEntity` fields and invalidation rules as a pure-Java state owner. It is not itself a Minecraft block entity.
2. The current repository does not yet contain a ported `BaseMetaPipeEntity`, `MetaPipeEntity`, or concrete GT cable block entity. Therefore a concrete adapter cannot be honestly implemented against real host objects yet.
3. The future ported cable block entity should own `LegacyPowerGraphNode` and `LegacyPowerPath` references directly. Do not attempt to put the old 1.7.10 `Node` class and the new pure-Java node into the same typed field or mix the two Minecraft class universes.
4. Preserve stable object identity for cable handles, node objects, path objects, map roots, and consumer adapters for the lifetime of a graph.
5. World access must be server-thread-only for graph mutation. `NeoForgeLegacyPowerNeighborAccess` documents this caller contract; it does not schedule work or silently move calls between threads. Any asynchronous chunk/world API must be marshalled to the logical server thread before touching this graph.
6. The graph core is not a replacement for NeoForge's energy capability. NeoForge capability exposure/transfer belongs at the consumer/source boundary; GTNH's graph remains responsible for its legacy routing, voltage loss, amperage limits, locks, overload, and partial-retry semantics.
7. Save/load behavior needs a separate decision. The original graph is runtime topology state and should be rebuilt from loaded world state rather than blindly serializing stale object references.

## Recommended implementation order

1. Integrate `LegacyPowerCableState` into the ported cable/base-tile state owner: node/path bindings, connection bits, occupied-side snapshot, and invalidation hooks.
2. Implement a read-only world adapter with explicit loaded-neighbor status, legacy graph-walk null mapping, six-side neighbor-cache invalidation, connection-check tri-state semantics, pipe classification, and energy-source detection. The narrow `NeoForgeLegacyPowerNeighborAccess` BlockEntity lookup portion is now implemented; the remaining topology operations still await the ported GT cable host.
3. Implement path binding and lock reload against the new cable objects.
4. Implement power-specific node factories and consumer discovery one family at a time.
5. Wire the source transfer coordinator to the logical server tick and cable source flag.
6. Add GameTests for loaded/unloaded chunk boundaries (including connect()'s -1 preserve-connection result), neighbor-cache invalidation after chunk load, endpoint appearance/disappearance without connection-bit changes, cable removal, graph refresh timing, and partial-transfer retry.
7. Only then connect external NeoForge energy capabilities and validate actual in-game behavior.

## Status

- Upstream host-state audit: completed against GT5-Unofficial 5.09.54.275.
- Pure-Java graph core: locally verified by the development machine's complete `legacy.power.*Test` Gradle test task.
- `LegacyPowerCableState`: implemented; the focused `LegacyPowerCableStateTest` and complete `legacy.power.*Test` tasks both reported `BUILD SUCCESSFUL` on the development machine.
- `LegacyPowerNeighborLookup`: implemented with five focused unit tests; `LegacyPowerNeighborLookupTest` and the complete `legacy.power.*Test` suite both reported `BUILD SUCCESSFUL` locally on 2026-11-23.
- `LegacyPowerNeighborCache`: implemented as a pure-Java six-direction cache model with eleven focused tests; the final focused suite and the full `legacy.power.*Test` suite both reported `BUILD SUCCESSFUL` locally on 2026-11-23.
- `LegacyPowerConnectionResult`: implemented to preserve the legacy positive / zero / -1 connection result semantics; `LegacyPowerConnectionResultTest` and the complete `legacy.power.*Test` suite both reported `BUILD SUCCESSFUL` locally on 2026-11-23.
- `NeoForgeLegacyPowerNeighborAccess` (`legacy.power.bridge.neoforge`): current active adapter uses `getChunkSource().getChunkNow(...)` and `LevelChunk.getBlockEntity(...)`; local `compileJava` success against the project's pinned NeoForge 26.3.0.113 was reported on 2026-11-23. The bridge package relocation is complete; re-run local compilation after the package move.
- Duplicate adapter consolidation: completed; only `legacy.power.bridge.neoforge.NeoForgeLegacyPowerNeighborAccess` remains.
- Concrete NeoForge GT cable/block-entity host: not implemented. The current main mod entry point registers only items.
- Block and BlockEntityType registration API: audited against the NeoForge 26.3.x DeferredRegister source; project integration pending.
- GameTest registration: audited against 26.3.x `RegisterGameTestsEvent`; exact `GameTestInstance` construction must be verified against the pinned 26.3.0.113 local dependency before implementation.
- Concrete GT consumer-family adapters: not implemented.
- In-game/GameTest verification: not implemented.

## Cable Block / BlockEntity implementation gate (2026-11-23)

The first registered NeoForge cable host must be a deliberately narrow **runtime shell**, not a premature replacement for the full GT meta-tile system.

### Responsibilities that must remain separate

| Concern | Legacy owner | NeoForge-side destination |
|---|---|---|
| Six connection bits and connect/disconnect result semantics | MetaPipeEntity / IConnectable | Cable host connection state + explicit connection policy |
| Cable material, thickness, voltage, amperage, insulation, shock flag, loss per meter | MTECable | Immutable cable specification/value object (not BlockState properties) |
| Graph node/path and topology invalidation | BaseMetaPipeEntity, NodePath, graph classes | Existing LegacyPowerCableState / LegacyPowerCableHostState |
| Six-direction adjacent BlockEntity lookup and cache | BaseTileEntity | Existing NeoForgeLegacyPowerNeighborAccess + host-owned cache |
| Block registration and BlockEntity construction | Minecraft/Forge host | NeoForge DeferredRegister integration |
| Rendering, collision shape, item interaction, covers, energy-family adapters | Multiple GT classes | Separate adapters; not silently dropped by the first host skeleton |

The cable's six connection bits are **not** enough to reconstruct the graph. The upstream occupiedConnectedSides scan detects a tile appearing or disappearing at an already-connected side, even when the connection mask does not change. The first BlockEntity must therefore expose a server-side topology-change entry point that can later be called by the neighbor callback after the occupied-side scan is implemented. Do not equate a vanilla neighbor callback with a completed GT checkConnections() implementation.

### Exact connection result contract

The upstream MetaPipeEntity.connect(side) behavior is not boolean:

- UNKNOWN returns 0.
- Missing base meta tile returns 0.
- A successful compatible pipe connection sets the side bit and reciprocally attempts the opposite connection, then returns 1.
- A successful GT6-style open-air connection or accepted machine/energy endpoint sets the side bit and returns 1.
- If no connection was accepted and the target chunk is not loaded, it returns **-1**.
- Otherwise it returns 0.

checkConnections() disconnects a side only when connect(side) == 0; a result of -1 preserves the existing bit. The existing LegacyPowerConnectionResult model already captures this tri-state distinction. A new host must use that model rather than converting it to a boolean.

### Cable specification is not a registry identity

MTECable stores thickness, material, loss per meter, amperage, voltage, insulation and shock behavior as per-cable data. Those values must not be collapsed into one universal registered cable block or encoded as six-direction connection flags. A future catalog/definition layer should describe cable variants; the initial registered host can be a test fixture or a clearly named prototype, but it must not pretend to represent all GT cable variants.

### 26.3 lifecycle mapping

For the pinned NeoForge 26.3 line, the current BlockEntity lifecycle surface includes the NeoForge IBlockEntityExtension callbacks onLoad() and onChunkUnloaded(), plus vanilla BlockEntity.setRemoved(). Map the host component callbacks as follows:

- On first attachment/load, call the host state's validation/cache initialization hook once the level is available.
- On onChunkUnloaded(), forward to LegacyPowerCableHostState.onChunkUnload().
- On setRemoved(), forward to LegacyPowerCableHostState.onInvalidate() and invalidate the cable's graph state according to the upstream removal path; do not confuse ordinary chunk unload with permanent block removal.
- On the block's neighbor callback, clear negative cache markers and schedule/perform server-side connection validation at the correct lifecycle point. The upstream code deliberately defers connection checking because neighboring structures may still be loading.
- If a host is ever moved/repositioned by a non-vanilla mechanism, call onPositionChanged(); normal position-bound BlockEntities do not need a speculative relocation system.

The callback signatures must be compiled against the exact pinned NeoForge 26.3.0.113 artifact before implementation. Javadocs for nearby 26.3.x patch builds are useful discovery aids but are not proof that every signature is identical in .113.

### First implementation boundary

The first code change should register a dedicated prototype cable Block, BlockEntityType, and BlockItem (if an item is needed for placement), and make the BlockEntity own one stable LegacyPowerCableHostState. It should initially implement only lifecycle forwarding and safe server-side neighbor access. It must not yet claim to:
- perform the complete GT cable connection policy;
- discover GT, IC2, Galacticraft, AE2 or CoFH consumers;
- inject or transfer energy;
- render the legacy cable geometry or covers;
- persist a finished GT cable catalog or replace legacy meta-tile IDs.

Those behaviors remain explicit subsequent milestones. This keeps the first in-game host testable without creating a misleading, functionally incomplete "GT cable port".


## Prototype cable host implementation (2026-11-23)

The first Layer 4 shell is now present on the feature branch:

- `block/PrototypePowerCableBlock`: a Block implementing `EntityBlock`, creating its dedicated BlockEntity.
- `block/PrototypePowerCableBlockEntity`: owns exactly one stable `LegacyPowerCableHostState`; forwards `onLoad()`, `onChunkUnloaded()`, and `setRemoved()` to the corresponding cache lifecycle hooks; exposes an explicitly server-only factory for `NeoForgeLegacyPowerNeighborAccess`.
- `registry/ModBlocks`: registers the prototype Block and its BlockEntityType using NeoForge DeferredRegister.
- `registry/ModItems`: registers a BlockItem for the prototype.
- `GTNHReforged`: registers the block, block-entity-type, and item DeferredRegisters on the mod event bus.

### Deliberate limits of this first code milestone

This is a registration/lifecycle shell only. It does not yet implement:
- the GT six-way connection policy or reciprocal pipe connection;
- occupied-connected-side scanning and topology invalidation on neighbor changes;
- cable variant/material properties or GT meta-tile identity;
- graph construction, consumer discovery, energy injection or transfer;
- cable geometry, collision adaptation, covers, rendering, or a creative-tab presentation.

The host does not clear the graph merely because `setRemoved()` runs: the precise permanent-removal versus unload semantics must be integrated with the real block-removal path and verified in-game. Chunk unload is forwarded separately. This avoids accidentally destroying graph state during ordinary chunk lifecycle transitions.

### Verification status

The source was authored against the NeoForge 26.3 DeferredRegister examples and 26.3.x BlockEntity extension lifecycle surface. It has **not yet been compiled against the project's pinned NeoForge 26.3.0.113 artifact**. Local `compileJava`, focused tests, and a launch/placement smoke test remain required before treating the shell as verified.

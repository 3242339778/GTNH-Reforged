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

### World and neighbor access

The original graph walker asks the pipe for the tile on a connected side. It does not imply that a missing or unloaded neighbor is an air block.

Do not force-load chunks to answer graph questions. The exact legacy behavior is operation-specific:

- `BaseTileEntity.getTileEntityAtSide` uses a six-entry neighbor cache. On a cross-chunk query it clears that side's cache first, specifically so a cached "no tile" marker cannot survive chunk loading. With the default `ignoreUnloadedChunks = true`, it returns null if the target across the chunk border is not loaded. Within the same chunk it reads the world directly. It also clears and retries an invalid cached tile entity.
- That method returns null both for a loaded position without a tile entity and for an unloaded cross-chunk position. The graph walk therefore historically treats both as "no adjacent tile" at this API boundary; a new implementation must not invent a different graph endpoint rule merely because its internal lookup can tell the cases apart.
- `MetaPipeEntity.connect(side)` has a separate, important tri-state result: positive means connected, zero means not connectable, and **-1 means the target chunk is unloaded**. `checkConnections()` disconnects a side only when `connect(side) == 0`; it preserves the existing connection when the result is -1. Collapsing unloaded into ordinary non-connectable would silently disconnect cables at chunk borders.
- `GenerateNodeMapPower.getAdjacentTileEntityIfLoaded` checks `blockExists` only when the adjacent position crosses a chunk boundary; otherwise it calls `getTileEntity` directly. This helper is used in the power consumer discovery path.
- IC2 sink discovery passes a null adjacent tile to `acceptsEnergyFrom` when that cross-chunk target is unloaded. Do not replace this with forced chunk access or a speculative lookup.

The NeoForge host boundary should expose enough information to preserve all these cases, for example a lookup result with `LOADED_WITH_BLOCK_ENTITY`, `LOADED_WITHOUT_BLOCK_ENTITY`, and `UNLOADED`. The legacy graph-walk adapter can deliberately map the latter two to the same null behavior where the 1.7.10 caller did so, while connection checking must retain the distinct unloaded result (-1 semantics).

### Neighbor cache and unloaded-chunk semantics

The world adapter must not treat all null-like lookups as interchangeable:

| Upstream call path | Loaded empty position | Unloaded cross-chunk position | Required behavior |
|---|---|---|---|
| `BaseTileEntity.getTileEntityAtSide` | null | null when `ignoreUnloadedChunks` is true | Same graph-facing null result, but clear cross-chunk cache before returning |
| `MetaPipeEntity.connect(side)` | 0 if no valid connection | -1 | `checkConnections` disconnects only on 0; -1 preserves the old bit |
| `GenerateNodeMapPower.getAdjacentTileEntityIfLoaded` | world lookup result (usually null) | null | Check loaded status only across chunk coordinates |
| IC2 sink discovery | actual adjacent tile | null passed to `acceptsEnergyFrom` | Preserve the IC2 callback's null argument |

`BaseTileEntity` also clears cached null markers on neighbor-block changes, tile validation, invalidation, and chunk unload. The cross-chunk cache is invalidated before the loaded check because loading a neighboring chunk does not necessarily issue the block update that would otherwise clear a cached empty marker.

This cache is a host-tile concern, not a graph-core concern. Keep it out of `LegacyPowerTopologyWalker`; implement it in the future ported cable/base block entity or a narrowly scoped host neighbor-cache helper.

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

## NeoForge 26.3 world-query candidate

The 26.3 API surface was checked against the 26.3.0.114 Javadocs as a preliminary design reference. The project currently targets 26.3.0.113, so exact signatures and behavior still require compilation against that pinned MDK before implementation is marked verified.

Candidate server-side lookup sequence:

1. Obtain the target chunk through `ServerLevel.getChunkSource().getChunkNow(chunkX, chunkZ)`.
2. If it returns null, report `UNLOADED` and do not call a chunk-loading method.
3. If it returns a `LevelChunk`, query that already-loaded chunk for the `BlockEntity` at the target `BlockPos`.
4. Return `LOADED_WITH_BLOCK_ENTITY` or `LOADED_WITHOUT_BLOCK_ENTITY` accordingly.

This is preferable to using `LevelReader.hasChunk` as the long-term abstraction: that method is present in the 26.3 Javadocs but deprecated. `ServerChunkCache.hasChunk` is also present, but `getChunkNow` provides the loaded chunk handle needed for the subsequent lookup without a second world-level retrieval. Do not use `getChunk(x, z, status, loadOrGenerate)` with loading enabled for graph discovery. The adapter should remain server-thread-only and should not enqueue chunk tickets or otherwise alter chunk residency.

Proposed pure-Java result contract (names are design names, not implemented API yet):

- `UNLOADED`: no loaded chunk handle; never infer air or a missing endpoint from this alone.
- `LOADED_WITHOUT_BLOCK_ENTITY`: target chunk is loaded and the target position has no block entity.
- `LOADED_WITH_BLOCK_ENTITY`: target chunk is loaded and a block entity exists; preserve that object's identity.

Callers then deliberately map the result according to their upstream contract: graph walking maps both non-object outcomes to null as the old `getTileEntityAtSide` path did; connection validation maps unloaded to the legacy -1/preserve-existing-connection result; IC2 sink discovery passes null to its acceptance callback for an unloaded cross-chunk target.

Reference API pages: [Level](https://aldak0.ru/javadoc/26.3.x/net/minecraft/world/level/Level.html), [LevelReader](https://aldak0.ru/javadoc/26.3.x/net/minecraft/world/level/LevelReader.html), [ServerChunkCache](https://aldak0.ru/javadoc/26.3.x/net/minecraft/server/level/ServerChunkCache.html).

## Mapping to the current pure-Java contracts

| Upstream responsibility | Current pure-Java boundary | Host implementation requirement |
|---|---|---|
| Cable `node` binding | `LegacyPowerNodeBinding` / `LegacyPowerCableState` | Store/retrieve the same node object by identity |
| Cable `nodePath` binding | `LegacyPowerPipeBinding` / `LegacyPowerCableState` | Store/retrieve the exact path object and update connection snapshot |
| `getNodeMap()` fallback | `LegacyPowerTopologyAccess.getNodeMapOwner` / `LegacyPowerTransferAccess.getNodeMapOwner` | Prefer node; otherwise resolve the bound path's graph owner |
| Six-way connection state | `LegacyPowerTopologyAccess.isConnected` | Use the real cable connection state and correct direction mapping |
| Neighbor lookup | `LegacyPowerTopologyAccess.getNeighbor` | Preserve the legacy null mapping for graph walking; the concrete world boundary must also expose loaded-empty vs unloaded for connection-check and consumer-specific callers |
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
5. World access must be server-thread-only for graph mutation. Any asynchronous chunk/world API must be marshalled to the logical server thread before touching this graph.
6. The graph core is not a replacement for NeoForge's energy capability. NeoForge capability exposure/transfer belongs at the consumer/source boundary; GTNH's graph remains responsible for its legacy routing, voltage loss, amperage limits, locks, overload, and partial-retry semantics.
7. Save/load behavior needs a separate decision. The original graph is runtime topology state and should be rebuilt from loaded world state rather than blindly serializing stale object references.

## Recommended implementation order

1. Integrate `LegacyPowerCableState` into the ported cable/base-tile state owner: node/path bindings, connection bits, occupied-side snapshot, and invalidation hooks.
2. Implement a read-only world adapter with explicit loaded-neighbor status, legacy graph-walk null mapping, six-side neighbor-cache invalidation, connection-check tri-state semantics, pipe classification, and energy-source detection.
3. Implement path binding and lock reload against the new cable objects.
4. Implement power-specific node factories and consumer discovery one family at a time.
5. Wire the source transfer coordinator to the logical server tick and cable source flag.
6. Add GameTests for loaded/unloaded chunk boundaries (including connect()'s -1 preserve-connection result), neighbor-cache invalidation after chunk load, endpoint appearance/disappearance without connection-bit changes, cable removal, graph refresh timing, and partial-transfer retry.
7. Only then connect external NeoForge energy capabilities and validate actual in-game behavior.

## Status

- Upstream host-state audit: completed against GT5-Unofficial 5.09.54.275.
- Pure-Java graph core: locally verified by the development machine's complete `legacy.power.*Test` Gradle test task.
- `LegacyPowerCableState`: implemented; the focused `LegacyPowerCableStateTest` and complete `legacy.power.*Test` tasks both reported `BUILD SUCCESSFUL` on the development machine.
- Concrete NeoForge cable/block-entity host: not implemented.
- Concrete GT consumer-family adapters: not implemented.
- In-game/GameTest verification: not implemented.

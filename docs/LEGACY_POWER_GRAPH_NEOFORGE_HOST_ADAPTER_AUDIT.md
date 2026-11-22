# Legacy Power Graph — NeoForge Host Adapter Audit

## Baseline and scope

This audit maps the pure-Java graph core to the actual GT5-Unofficial **5.09.54.275** host-side cable and world behavior. It is a design gate for the NeoForge 26.3 adapter; it does **not** claim that a runtime adapter or in-game cable implementation already exists.

Primary upstream sources:

- [BaseMetaPipeEntity.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/metatileentity/BaseMetaPipeEntity.java)
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

The new adapter must distinguish:

1. A loaded neighboring position with no block entity.
2. A loaded neighboring position with a block entity.
3. A position in an unloaded chunk.

Do not force-load chunks to answer graph questions. The original connection-update path also checks chunk existence before reading a neighbor across a chunk boundary. The IC2 consumer adapter has an additional loaded-neighbor rule: when its adjacent lookup crosses a chunk boundary and the target block is not loaded, it passes a null adjacent tile to `acceptsEnergyFrom`.

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

## Mapping to the current pure-Java contracts

| Upstream responsibility | Current pure-Java boundary | Host implementation requirement |
|---|---|---|
| Cable `node` binding | `LegacyPowerNodeBinding` / `LegacyPowerCableState` | Store/retrieve the same node object by identity |
| Cable `nodePath` binding | `LegacyPowerPipeBinding` / `LegacyPowerCableState` | Store/retrieve the exact path object and update connection snapshot |
| `getNodeMap()` fallback | `LegacyPowerTopologyAccess.getNodeMapOwner` / `LegacyPowerTransferAccess.getNodeMapOwner` | Prefer node; otherwise resolve the bound path's graph owner |
| Six-way connection state | `LegacyPowerTopologyAccess.isConnected` | Use the real cable connection state and correct direction mapping |
| Neighbor lookup | `LegacyPowerTopologyAccess.getNeighbor` | Return null for absent or unloaded neighbors without force-loading |
| Pipe classification | `LegacyPowerTopologyAccess.isPipe` | Only GT power cables are traversable, not every pipe/fluid pipe |
| Power-specific node factories | `LegacyPowerNodeBuildAccess` | Construct the right pipe/dead-end/consumer node types |
| Path creation | `LegacyPowerNodeBuildAccess.createPath` | Bind every cable in order; preserve identity and owner |
| Lock refresh | `reloadPipeLocks` / `reloadPathLocks` | Recompute real cable-side lock state after graph installation |
| Cable connection snapshot / occupied sides | `LegacyPowerCableState` | Preserve bind-time connection bits and detect endpoint occupancy changes independently |
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
2. Implement a read-only world adapter for loaded-neighbor lookup, connection checks, pipe classification, and energy-source detection.
3. Implement path binding and lock reload against the new cable objects.
4. Implement power-specific node factories and consumer discovery one family at a time.
5. Wire the source transfer coordinator to the logical server tick and cable source flag.
6. Add GameTests for loaded/unloaded chunk boundaries, endpoint appearance/disappearance without connection-bit changes, cable removal, graph refresh timing, and partial-transfer retry.
7. Only then connect external NeoForge energy capabilities and validate actual in-game behavior.

## Status

- Upstream host-state audit: completed against GT5-Unofficial 5.09.54.275.
- Pure-Java graph core: locally verified by the development machine's complete `legacy.power.*Test` Gradle test task.
- `LegacyPowerCableState`: implemented; local Gradle verification pending.
- Concrete NeoForge cable/block-entity host: not implemented.
- Concrete GT consumer-family adapters: not implemented.
- In-game/GameTest verification: not implemented.

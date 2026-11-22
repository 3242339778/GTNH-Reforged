# GTNH 2.9.1 Power Network Behavior Audit

**Source baseline:** GT5-Unofficial 5.09.54.275  
**Historical comparison:** 5.09.54.265  
**Scope:** first-pass source-level audit of power graph topology, invalidation, cable lifecycle, and transfer recovery.

This is an implementation specification derived from source inspection, not a claim that the modern implementation exists or has passed tests.

## 1. Core model

The 2.9.1 network is not just a set of adjacent cables. It maintains:
- graph nodes and consumer nodes;
- per-side neighbor nodes, paths, and locks;
- self paths and paths that close loops without reaching a new node;
- a graph-map owner/root shared by joined nodes;
- topology validity and monotonically increasing topology version;
- cable-side cached connection bits and occupied-neighbor-side bits;
- a distinction between built-in non-ticking cables and addon cable subclasses that must retain normal tick callbacks.

Key source files:
- `gregtech/api/graphs/Node.java`
- `gregtech/api/graphs/GenerateNodeMap.java`
- `gregtech/api/graphs/paths/NodePath.java`
- `gregtech/api/metatileentity/BaseMetaPipeEntity.java`
- `gregtech/api/metatileentity/implementations/MTECable.java`

## 2. Node-map ownership and invalidation

In 5.09.54.275, `Node` adds:
- `nodeMapRoot`, initially itself;
- `nodeMapValid`;
- `topologyVersion`;
- `topologyChangeTime`;
- `joinNodeMap(previousNode)`;
- `invalidateNodeMap()`;
- `isNodeMapRefreshDue()`;
- `getTopologyVersion()`.

Joining a node associates it with the previous node's root. Invalidating a map marks the root invalid, increments its topology version, and records the current server tick. A refresh is due when the map is invalid and at least 10 server ticks have elapsed since the recorded topology change.

Important: the 10-tick threshold is a refresh eligibility condition. It is not a universal periodic rebuild interval.

## 3. Graph construction and path ownership

`GenerateNodeMap` now:
- associates newly generated pipe and consumer nodes with the same map via `joinNodeMap`;
- assigns ownership of self paths and routed paths with `NodePath.setNodeMap`;
- records loop-closing cable runs in `Node.mUnroutedPaths`, assigning them to the graph owner so later edits can invalidate the topology;
- retires a previously separate graph before binding cables into a replacement graph;
- preserves energy-source cables as explicit graph nodes, even when they have two connections, so source injection can reuse paths and overload history;
- uses an iterative degree-two cable walk rather than recursive calls for every cable, avoiding one stack frame per cable.

When clearing a graph, the implementation marks nodes invalid, clears owned paths and unrouted paths, removes cable node references only when they still point to the node being cleared, and traverses neighbor nodes while avoiding a return to the previous depth marker.

`NodePath` tracks its owning node map. Invalidating a path can invalidate the owner graph; clearing a path removes references from the pipes only if those pipes still point to that exact path.

## 4. Topology change detection

The 2.9.1 cable wrapper tracks topology changes from several sources:

1. Cable connection-bit changes:
   - `updateConnections()` detects changes to the meta-pipe connection mask.
   - It marks the cable topology changed.
   - It invalidates paths corresponding to removed connection sides.
   - It updates the cached connection mask and issues the cable update notification.

2. Cable neighbor changes:
   - `onNeighborBlockChange()` schedules connection checking rather than immediately recalculating during world loading.
   - This delay is intentional; the source comment notes issues during AE2 GTEU P2P loading.
   - It separately scans occupied connected sides, because adding/removing an endpoint on an already-open connection can leave the connection bitmask unchanged.
   - If occupied-side state changes, the node map is invalidated.

3. Cable removal/unload:
   - invalidates node-map/path state and removes the cable from managed ticking when unloading.

4. Adjacent cable propagation:
   - `markCableTopologyChanged` invalidates the current cable's map and neighboring cable maps (or fully clears them in the explicit clear path).

These are distinct invalidation triggers and should remain distinct in the modern bridge.

## 5. Managed non-ticking cable lifecycle

Built-in cables can opt out of ordinary per-tick TileEntity updates. Addon subclasses are deliberately excluded from this optimization because they may depend on normal pre/post tick callbacks or the transfer method's passed-set behavior.

The source maintains an identity-based set of managed cable wrappers. Managed ticking:
- validates world/server side, block-entity identity, validity, and cable type;
- initializes the cable and schedules topology checks;
- checks automatic connections on a 20-world-tick cadence when GT6-style connection behavior is not enabled;
- performs pending connection checks, updates cached connections, and rejoins the energy network;
- updates cover behavior and synchronization data;
- removes cables from the managed set when they no longer need managed updates.

Chunk unload/load has explicit handling: unload removes managed references and invalidates graph state; load resets initialization/check state and schedules managed work again.

## 6. Energy transfer recovery

In `MTECable.transferElectricity`:
1. Reject invalid amperage, client-side calls, and disconnected sides (except UNKNOWN).
2. Mark the cable as an energy source.
3. If its current map is invalid and the 10-tick refresh condition is met, clear the old graph.
4. Build a power graph if the source has no node.
5. Resolve consumers and run power distribution.
6. If consumer data is stale while the node remains valid, clear and rebuild the graph.
7. If some amperage was already used, preserve that result and attempt to distribute only the remaining amperage on the rebuilt graph.

This partial-transfer recovery is a behavior invariant: rebuilding must not silently duplicate already delivered amperage.

## 7. Required compatibility-layer architecture

Do not replace the GTNH graph algorithm with NeoForge's energy capability or a generic energy network. Those APIs may serve as external ingress/egress adapters, but the GTNH graph remains authoritative for:
- source/consumer discovery;
- voltage and amperage rules;
- path and lock behavior;
- topology invalidation;
- stale-consumer recovery;
- partial transfer accounting.

Suggested modules:

- `LegacyPowerGraph`: retained/adapted graph, node, consumer, path, lock algorithms.
- `PowerTopologyTracker`: map ownership, topology version, invalidation causes, delayed refresh eligibility.
- `LegacyCableLifecycle`: managed cable scheduling, initialization, chunk load/unload, connection checks.
- `NeoForgePowerBridge`: modern BlockEntity/capability lifecycle and external energy adapters only.
- `PowerGraphDiagnostics`: topology version, graph validity, stale consumers, source/consumer/path inspection.

## 8. Behavior test matrix

Implement tests before optimizing or replacing algorithms.

| Scenario | Required invariant |
|---|---|
| Straight source-cable-consumer chain | Same consumer discovery and delivered amperage |
| Branching network | All valid consumers discovered exactly once |
| Closed cable loop | Loop path is owned and invalidated with its graph |
| Add/remove endpoint on an already-open side | Occupied-side scan invalidates graph even if connection bits do not change |
| Cable connection bit removed | Only affected paths are invalidated, then graph rebuild is correct |
| Merge two previously separate grids | Old graph is retired before replacement bindings are assigned |
| Split one grid | No stale node/path/lock remains reachable |
| Source added to degree-two cable run | Source becomes an explicit graph node |
| Consumer changes during transfer | Stale consumer list triggers rebuild |
| Partial transfer followed by rebuild | Already-used amperage is not delivered twice |
| Cable chunk unload/load | No stale managed cable or graph references survive |
| Addon cable subclass | Ordinary tick callbacks remain available |
| Long straight cable run | Iterative walk avoids recursion-depth failure |
| Delayed refresh | Invalid map is not treated as refresh-due before the 10-tick threshold |

## 9. Exact next implementation step

Before implementing modern NeoForge APIs, isolate the algorithmic core from Minecraft types:
- replace `TileEntity` references at the graph boundary with a narrow host interface;
- represent six directions as a stable legacy direction enum/value;
- retain source/consumer/path/lock semantics;
- inject a tick counter instead of calling `MinecraftServer.getServer()` directly;
- write deterministic graph fixtures for the test matrix.

Do not change voltage/amperage arithmetic or consumer ordering in this extraction step. The first milestone is a behavior-preserving graph core, not a redesigned power system.

## 10. Evidence limits

Inspected source ranges in GT5-Unofficial 5.09.54.275:
- `Node.java`
- `GenerateNodeMap.java`
- `NodePath.java`
- `BaseMetaPipeEntity.java`
- `MTECable.java`

The 5.09.54.265 to 5.09.54.275 comparison established that these files changed, but this document is a focused behavior audit, not a line-by-line review of every power-related class. Exact source behavior must be retained and tested during implementation.

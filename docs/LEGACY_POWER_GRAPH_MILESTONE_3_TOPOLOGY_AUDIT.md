# Legacy Power Graph — Milestone 3 Topology Audit

## Status

- Upstream baseline: GT5-Unofficial `5.09.54.275`
- Source reviewed: `GenerateNodeMap.java`, `Node.java`, `PowerNodes.java`, `paths/NodePath.java`, `paths/PowerNodePath.java`
- This document records source-level behavior and extraction constraints.
- No claim is made that the topology algorithm has been ported or behavior-tested yet.

## Upstream source locations

- `src/main/java/gregtech/api/graphs/GenerateNodeMap.java`
- `src/main/java/gregtech/api/graphs/Node.java`
- `src/main/java/gregtech/api/graphs/PowerNodes.java`
- `src/main/java/gregtech/api/graphs/paths/NodePath.java`
- `src/main/java/gregtech/api/graphs/paths/PowerNodePath.java`

## GenerateNodeMap behavior

### Degree-two walk

`getNextValidTileEntity` uses a labelled iterative `while` loop. It:

1. Returns a non-pipe tile as a candidate endpoint.
2. Stops if the encountered pipe's node is already in the current node-map set.
3. Requires the side facing the previous tile to be connected.
4. If the pipe belongs to a different node-map root, clears that old map before replacement bindings are created; if it belongs to the current map, stops.
5. Treats a pipe as an intermediate cable only when it has exactly two connected sides and is not an `MTECable` energy source.
6. Appends each intermediate `MetaPipeEntity` to the path list, advances to the next pipe, and continues the loop.
7. If the next tile is absent, returns the current pipe as the endpoint candidate; if the next connected tile is not a pipe, also returns the current pipe as endpoint candidate.

The energy-source exception is a topology rule: a known source remains a node even at degree two so repeated injections can reuse paths and overload history.

### Node creation and edge paths

`generateNode` distinguishes pipe endpoints from consumers:

- A pipe with exactly one connection uses `getEmptyNode`; if that returns null, no node is created.
- Other pipe endpoints use `getPipeNode`.
- New nodes join the previous node's map root, are bound to the base pipe, and are added to the current node-map set.
- Every pipe node receives a self path containing its own meta pipe; the path is assigned to that node map.
- When there is a previous node, the new node receives a reverse neighbor edge and a path containing the walked pipes. A shared `Lock` is installed into the corresponding path/edge lock slots and returned through the previous node's `returnValues`.
- Nodes with more than one connection recursively expand through `generateNextNode`.
- A consumer node is added only when `addConsumer` accepts the tile. It joins the previous map, stores the previous node as neighbor, and receives the path of walked pipes plus a new edge lock.

### Branch expansion and loop-closing paths

`generateNextNode` walks each valid direction except the side from which the current node was entered. It accumulates occupied-side flags, asks `getNextValidTileEntity` for an endpoint, and attempts to create the next node.

When a new node is created, the previous node receives the neighbor, return path, and lock; the path locks are reloaded. If no node is created but the walk accumulated intermediate pipes, the algorithm still creates a path, assigns it to the current node map, and stores it in `mUnroutedPaths`. This preserves ownership of loop-closing cable runs so topology edits can invalidate the routing tree.

### Graph clearing

`clearNodeMap(node, returnNodeValue)` performs ordered cleanup:

1. Marks the current node invalid.
2. If it is a base pipe, clears its node binding only if the binding still points to this exact node; clears its self path.
3. Clears every unrouted path and drops that collection.
4. Iterates all six sides. Each edge path is cleared and removed from the node slot.
5. Recurses into a neighbor only when its `mNodeValue` differs from the supplied return value.
6. Clears the neighbor slot after the conditional recursion.

The `mNodeValue` return-value rule is not interchangeable with an ordinary visited-set traversal without a proof of equivalence. Preserve its exact ordering and cycle behavior during extraction. Path cleanup itself uses identity checks before detaching a pipe binding.

## PowerNodes observations relevant to later transfer work

- Consumer traversal uses node values and `mHighestNodeValue` to select a direction through the graph instead of searching the entire graph.
- `processNextNode`, `processNextNodeAbove`, and `processNodeInject` validate paths and locks before forwarding.
- Path voltage loss is applied before recursive transfer; amperage is added to path statistics after the recursive call returns.
- `processNodeInject` snapshots the topology version before invoking the consumer and marks the consumer list stale if the topology version changes during injection.
- The caller must preserve already-used amperage when a stale consumer list causes a retry/rebuild. This is a separate transfer invariant and is not implemented by the topology-only extraction.

## Pure-Java extraction boundary

The algorithm needs an explicit world adapter rather than Minecraft classes:

- stable tile/pipe identity;
- six ordered directions and opposite-direction mapping;
- connected-side queries;
- neighbor lookup;
- pipe-vs-consumer classification;
- pipe connection count;
- energy-source classification;
- current node and node-map-root bindings;
- path factory and pipe-path binding;
- consumer-node factory/acceptance callback;
- occupied-side update and lock-reload callbacks.

Do not encode world access, NeoForge capabilities, or Minecraft objects in the pure topology core. The adapter must preserve upstream direction order and object-identity semantics.

## Required topology behavior tests before claiming parity

1. Straight degree-two cable run collapses into one path segment.
2. Branching pipe creates the same node/edge structure and node values as upstream.
3. Degree-two energy source remains a node.
4. A run closing onto an existing node creates an owned unrouted path.
5. Encountering a node already in the current map terminates the walk.
6. Encountering a different map clears the old map before rebinding.
7. Missing neighbor and disconnected incoming side follow the original endpoint/termination rules.
8. Clearing a line, branch, and cycle preserves the original return-node recursion behavior.
9. Clearing an old path does not detach a pipe already rebound to a replacement path.
10. Invalidated graph/path bindings cannot be reused by a subsequent traversal.

## Next implementation step

Implement the pure topology adapter contracts and deterministic fake-world fixtures first. Port the iterative degree-two walk and node/edge construction against those contracts. Port `clearNodeMap` separately with tests for its return-node rule. Only then connect the traversal to NeoForge world state.

No Minecraft/NeoForge energy capability should replace GTNH's graph traversal or distribution algorithm.

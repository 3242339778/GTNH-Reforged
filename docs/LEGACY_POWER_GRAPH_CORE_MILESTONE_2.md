# Legacy Power Graph — Milestone 2

**Branch:** `feature/legacy-power-graph-core`  
**Upstream behavior baseline:** GT5-Unofficial `5.09.54.275`  
**Status:** pure-Java path ownership and lock contracts added; local Windows test execution pending.

## Scope implemented

This milestone extracts the Minecraft-independent parts of upstream `NodePath` and `Lock`:

- path construction binds each pipe adapter to the new path;
- a path may have an owning graph callback;
- `invalidateNodeMap()` clears an unowned path, but delegates `clearNodeMap(-1)` to an owned graph;
- `invalidate()` marks a path invalid without detaching pipe bindings;
- `clearPath()` invalidates, drops ownership, and detaches a pipe only if it still points to this exact path instance;
- lock owners are deduplicated and removed by object identity, not `equals()`;
- null lock owners are supported, matching the upstream `addTileEntity(null)` call sites.

Files:
- `LegacyPowerPipeBinding`: narrow adapter contract for a pipe's current path binding.
- `LegacyPowerPathOwner`: callback for graph clearing.
- `LegacyPowerPath`: path validity, ownership, and cleanup.
- `LegacyPowerPathLock`: identity-based lock owner collection.

These classes have no Minecraft, Forge, or NeoForge imports.

## Upstream correspondence

GT5-Unofficial `5.09.54.275`:

- [NodePath.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/paths/NodePath.java)
  - constructor and `processPipes()` bind pipes to the path;
  - `setNodeMap` stores graph ownership;
  - `invalidateNodeMap()` delegates to `GenerateNodeMap.clearNodeMap(node, -1)` when owned;
  - `invalidate()` only changes validity;
  - `clearPath()` clears a pipe binding only when it still references this path.
- [Lock.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/Lock.java)
  - owner lookup uses reference identity (`==`);
  - duplicate owner identities are ignored;
  - null is a valid owner value.

## Deliberate boundaries

This is not yet a full `NodePath` / `PowerNodePath` port:

- The Minecraft pipe object is represented by `LegacyPowerPipeBinding`.
- Full graph destruction is represented by `LegacyPowerPathOwner.clearNodeMap(-1)`; the callback must later be wired to the extracted graph-clearing implementation.
- `PowerNodePath` voltage, amperage, cable-loss, overload/fire behavior, tick counters, and telemetry are not included. Its current upstream implementation directly uses Minecraft server ticks, GT cable classes, and `AveragePerTickCounter`.
- `GenerateNodeMap` traversal is not included yet.

## Tests added

`LegacyPowerPathTest` covers:
- construction-time pipe binding;
- conditional identity-based detach during clear;
- unowned-path invalidation;
- owned-path graph-clear delegation with return value `-1`;
- distinction between `invalidate()` and `clearPath()`;
- identity-based lock deduplication/removal and null owner handling.

## Local verification required

Run in the Windows project directory:

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerPathTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
```

The remote GitHub edits have not been compiled or executed by the assistant. Do not mark this milestone verified until the local XML test reports show zero failures and errors.

## Next extraction: GenerateNodeMap

The next source slice must preserve, rather than redesign, these observed upstream behaviors:

1. `clearNodeMap` invalidates the node, clears owned self/unrouted/side paths, recursively clears neighbors except the return node, and nulls neighbor references.
2. Degree-two cable runs are walked iteratively.
3. Already-known nodes terminate a walk rather than being traversed again.
4. Existing separate graph ownership is retired before replacement cable bindings are assigned.
5. A two-connection energy-source cable is retained as an explicit node.
6. Loop-closing paths are retained in the owning node's unrouted-path collection so topology edits can invalidate them.
7. Pipe connection masks / occupied-side bookkeeping are host-world concerns and must enter through explicit adapter interfaces.

Before implementing the traversal, add graph fixtures for chains, branches, degree-two runs, cycles, source nodes, and cleanup/return-node behavior. Power distribution and partial-transfer recovery remain a later milestone; they must not be approximated by replacing GTNH's algorithm with NeoForge Energy capability semantics.

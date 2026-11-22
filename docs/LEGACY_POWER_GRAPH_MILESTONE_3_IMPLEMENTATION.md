# Legacy Power Graph — Milestone 3 Implementation Progress

## Scope implemented

Baseline: GT5-Unofficial `5.09.54.275`.

This increment extracts the iterative degree-two cable walk from
`gregtech.api.graphs.GenerateNodeMap#getNextValidTileEntity` into a pure-Java
component. It does not yet port complete graph construction or graph clearing.

### Added production types

- `LegacyPowerDirection`: six directions in legacy ForgeDirection ordinal
  order, including opposite-direction mapping.
- `LegacyPowerTopologyAccess`: narrow host adapter for pipe classification,
  node/map identity, connected sides, neighbor lookup, graph retirement, and
  energy-source classification.
- `LegacyPowerTopologyWalker`: iterative walk with the upstream branch order
  and endpoint rules.

### Preserved upstream rules

- A node already present in the map being generated terminates the walk.
- The incoming side must be connected.
- A pipe owned by another graph causes that graph to be cleared with return
  value `-1` before traversal continues.
- Exactly-two-connection pipes are collapsed into the path, except known
  energy-source cables, which remain graph endpoints.
- A missing next tile or a non-pipe next tile returns the current pipe as the
  endpoint candidate.
- A disconnected incoming side, already-owned node, current-map owner, or
  exhausted connected directions returns no candidate.
- Intermediate pipe order is retained exactly as encountered.

## Tests added

`LegacyPowerTopologyWalkerTest` covers:

1. A straight degree-two run and its intermediate path list.
2. A degree-two energy source retained as an endpoint.
3. Termination on a node already in the current map.
4. Clearing a foreign graph before continuing.
5. Missing-neighbor endpoint behavior.
6. Disconnected incoming-side termination.

## Important limitations

- These tests have been authored but have not yet been executed in the
  repository's Gradle environment. Run the test class locally after pulling.
- This is the walk helper only. It does not create graph nodes, install edge
  paths/locks, recurse across branches, record loop-closing unrouted paths, or
  update occupied-side state.
- `clearNodeMap` remains a separate extraction task. Its return-node recursion
  behavior must not be replaced with a generic visited-set traversal.
- The `LegacyPowerTopologyAccess` adapter must be implemented against actual
  NeoForge-side/legacy-compatible objects before world integration.

## Local verification

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerTopologyWalkerTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
```

A successful test run verifies this isolated extraction only, not full GTNH
power-grid parity.

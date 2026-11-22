# Legacy Power Graph — Milestone 1

**Branch:** `feature/legacy-power-graph-core`  
**Upstream behavior baseline:** GT5-Unofficial `5.09.54.275`  
**Status:** initial pure-Java topology-state extraction; not yet connected to NeoForge.

## Scope implemented

The first code slice extracts the shared node-map state contract from upstream `gregtech.api.graphs.Node`:

- shared map-root ownership via `joinNodeMap`;
- root-level validity;
- root-level topology version;
- invalidation timestamp;
- the 10-server-tick delayed-refresh eligibility check.

The implementation is in `LegacyPowerGraphState` and has no Minecraft, Forge, or NeoForge imports.

## Deliberate non-goals

This milestone does **not** implement or replace:
- `GenerateNodeMap` graph traversal;
- `Node`, `ConsumerNode`, `PowerNode` behavior;
- `NodePath` ownership, clearing, or lock semantics;
- `PowerNodes.powerNode` voltage/amperage distribution;
- cable connection discovery or managed cable ticking;
- world/chunk lifecycle integration.

Those behaviors must be ported from the upstream implementation with their own tests. In particular, the state holder is not yet a functioning power grid.

## Upstream correspondence

GT5-Unofficial 5.09.54.275:
- `src/main/java/gregtech/api/graphs/Node.java`
  - `nodeMapRoot`, `nodeMapValid`, `topologyVersion`, `topologyChangeTime`
  - `joinNodeMap`
  - `invalidateNodeMap`
  - `isNodeMapRefreshDue`
  - `getTopologyVersion`

The refresh threshold remains ten server ticks. It is an eligibility check, not a periodic rebuild schedule.

## Tests added

`LegacyPowerGraphStateTest` covers:
- invalidation visibility across joined nodes;
- the exact 9/10-tick boundary;
- repeated invalidation/version increments;
- one-way root reassignment semantics (not union-find);
- null joins;
- tick values earlier than the invalidation tick.

## Verification still required

The source and tests have been added, but this remote repository edit has not been compiled or executed in this environment. Run:

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerGraphStateTest"
```

Then run the full:

```powershell
.\gradlew.bat check
```

## Next milestone

Extract the graph traversal and path ownership contracts while keeping Minecraft objects behind a narrow host interface. Do not begin NeoForge energy capability wiring until the pure-Java graph fixtures reproduce the upstream topology and transfer behavior.

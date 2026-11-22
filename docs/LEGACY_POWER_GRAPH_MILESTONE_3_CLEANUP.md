# Legacy Power Graph — Node Model and Cleanup Extraction

## Baseline

GT5-Unofficial `5.09.54.275`.

Upstream methods:
- `gregtech.api.graphs.GenerateNodeMap.clearNodeMap(Node, int)`
- `gregtech.api.graphs.Node.joinNodeMap(Node)`
- `gregtech.api.graphs.paths.NodePath.invalidateNodeMap()`
- `gregtech.api.graphs.paths.NodePath.clearPath()`

## Implemented pure-Java model

- `LegacyPowerNodeBinding`: identity-sensitive host binding for a pipe's current graph node.
- `LegacyPowerGraphNode`: six directional neighbor/path/lock slots, node values, invalid flag, self path, unrouted paths, and shared graph-state root.
- `LegacyPowerGraphCleaner`: ordered graph cleanup using the upstream return-node-value recursion rule.

## Cleanup behavior retained

The implementation follows the upstream operation order:

1. Mark the current node invalid.
2. If the node has a pipe binding, detach it only when the pipe still points to this exact node; then clear its self path.
3. Clear every unrouted path and empty the collection.
4. Iterate the six directions in legacy ordinal order.
5. Clear and remove the edge path for each side.
6. For a neighbor, recurse only when its node value differs from the supplied return value.
7. Clear the neighbor slot after the conditional recursive call.

The recursion rule is intentionally not replaced with a generic visited set. The neighbor links produced by the upstream builder form a routing tree; loop-closing runs are stored in `mUnroutedPaths`, not added as a second neighbor route. Identity checks remain in pipe-node and path detachment.

## Tests added

`LegacyPowerGraphCleanerTest` covers:
- line cleanup and node-binding detachment;
- branch cleanup plus loop-closing unrouted-path cleanup using the upstream spanning-tree topology;
- identity-safe cleanup when a pipe has already been rebound to a replacement node;
- self, unrouted, and directional edge path cleanup;
- path invalidation delegating to the owning graph node;
- shared graph-root invalidation across joined nodes.

## Not yet implemented

This is not full `GenerateNodeMap` extraction. Still outstanding:
- node creation for pipe endpoints and consumers;
- `getEmptyNode` / `getPipeNode` customization hooks;
- previous-node return path and shared edge lock wiring;
- recursive branch expansion and occupied-side updates;
- loop-closing unrouted path creation from the traversal result;
- integration of the traversal and node builder;
- a proof/fixture for long-chain recursion behavior in graph clearing.

The original `clearNodeMap` is recursive. This extraction preserves that behavior rather than silently changing stack behavior. A future iterative implementation would require equivalence tests and an explicit compatibility decision.

## Local verification

After pulling this branch:

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerGraphCleanerTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
```

These tests validate the isolated model only; they do not establish full power-grid parity.

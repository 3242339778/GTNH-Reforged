# Legacy Power Graph — Node Builder Extraction

## Baseline

GT5-Unofficial `5.09.54.275`.

Primary upstream methods:
- `gregtech.api.graphs.GenerateNodeMap.generateNode(...)`
- `gregtech.api.graphs.GenerateNodeMap.generateNextNode(...)`
- `gregtech.api.graphs.GenerateNodeMap.getNextValidTileEntity(...)`
- `gregtech.api.graphs.Node.joinNodeMap(...)`
- `gregtech.api.graphs.paths.NodePath.setNodeMap(...)`
- `gregtech.api.graphs.Lock`

## Implemented

- `LegacyPowerNodeBuildAccess`: host contract for tile validity, pipe connection count, node factories, consumer acceptance, path construction, legacy side flags, and lock reload callbacks.
- `LegacyPowerNodeBuilder`: pure-Java extraction of node creation and recursive branch expansion.
- `LegacyPowerGraphNode`: stores the upstream `returnValues` path/lock pair.
- `LegacyPowerPath`: accepts a shared endpoint lock.
- `LegacyPowerTopologyWalker.walkDetailed`: retains the intermediate pipe run even when upstream returns no endpoint, which is required to create an unrouted loop-closing path.

## Semantics deliberately retained

1. A pipe with exactly one connection uses the overridable empty-node factory; a null result ends that branch.
2. Other pipe nodes use the pipe-node factory.
3. New nodes join the previous node's current graph root, bind to the pipe, and create a self path.
4. Incoming edge paths are owned by the node that creates them.
5. The same edge path object is referenced by both endpoint nodes. Its owner must **not** be reassigned when the previous node installs its return path.
6. The edge path and both endpoint direction slots share the same Lock identity.
7. A child returns its path/lock through the previous node's return-values pair; the parent installs them in its outgoing direction slot.
8. Branches are visited in legacy direction order. Each subsequent branch starts after the highest node value produced by the previous branch.
9. A loop-closing run with no endpoint becomes an unrouted path owned by the node whose branch discovered it.
10. Occupied-side flags are updated only after a neighboring tile exists, then the pipe lock reload callback runs. Path lock reload runs after the parent installs a returned edge path.

## Host adapter obligations

A NeoForge integration must:
- provide exact legacy `ForgeDirection.flag` values (not enum ordinals);
- return stable identity handles for tiles, nodes, map owners, and pipe bindings;
- implement `createPath` so each pipe tile in the supplied ordered run is bound to the created path;
- implement `addConsumer` by appending exactly one consumer node when it returns true;
- provide the correct GTNH-specific empty-node and pipe-node subclasses/factories;
- implement path/pipe lock reloads using the real host-side cable behavior.

The builder is intentionally not coupled to Minecraft or NeoForge classes.

## Tests added

`LegacyPowerNodeBuilderTest` covers:
- consumer endpoint creation and bidirectional neighbor/path installation;
- shared path/endpoint Lock identity;
- sibling branch node-value progression and highest-node-value tracking;
- dead-end pipe creation through the empty-node factory hook;
- occupied-side flag and reload callback behavior.

## Not implemented / not claimed

- Full runtime integration with `BaseMetaPipeEntity`, `MetaPipeEntity`, or GT5U concrete `PowerNodePath`.
- Full loop-closing path fixture against a real cable topology.
- Consumer-specific machine acceptance rules and concrete subclasses.
- Power voltage/loss/amperage accounting and `PowerNodes` distribution.
- Proof of exact parity for every subclass override of `getEmptyNode`, `getPipeNode`, `addConsumer`, and `getNewPath`.

## Local verification

After pulling the branch:

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerNodeBuilderTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
```

The implementation is committed but has not yet been verified by the user's local Gradle run. Do not treat it as passed until those commands succeed.

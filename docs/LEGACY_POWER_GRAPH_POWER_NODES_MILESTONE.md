# Legacy Power Nodes — Milestone

## Baseline

Implementation target: GT5-Unofficial **5.09.54.275**.

Primary upstream sources:

- [PowerNodes.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/PowerNodes.java)
- [Node.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/Node.java)
- [NodeList.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/NodeList.java)
- [ConsumerNode.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/consumers/ConsumerNode.java)

## Implemented

- `LegacyPowerConsumerAccess`: host contract for a graph consumer, its tile-validity policy, and injection behavior.
- `LegacyPowerConsumerList`: cursor and stale marker matching `NodeList`.
- `LegacyPowerNodes`: pure-Java extraction of `powerNode`, `powerNodeAbove`, `processNextNode`, `processNextNodeAbove`, and `processNodeInject`.
- `LegacyPowerDirection.fromOrdinalIndex`: maps the six direction-array slots without changing the legacy order.

## Preserved algorithm invariants

- The graph is searched by node values and `mHighestNodeValue`, not by a generic BFS/DFS.
- Lower-value routing scans directions from index 0 to 5.
- Higher-value routing scans directions from index 5 down to 0.
- The consumer cursor advances exactly where the upstream code advances it; locked edge traversal and direct locked injection have distinct cursor behavior.
- Invalid paths mark the consumer list stale.
- Edge path lock checks happen before voltage application.
- Self-path voltage is applied first with `countUp=false`; edge-path voltage follows with `countUp=true`.
- Self-path loss is subtracted before edge-path voltage is applied; edge-path loss is then subtracted before recursion/injection.
- Path amperage/energy counters are updated only after recursive traversal or consumer injection returns.
- Consumer injection is skipped unless `voltage > totalVoltageLoss`.
- Topology version is snapshotted around consumer injection; a version change marks the consumer list stale.
- The already-used amperage is returned even when the consumer list becomes stale. The caller must retry only with the remaining amperage, never replay a completed injection.

## Adapter obligations

- Supply consumers in the same order as the upstream graph's consumer list.
- Apply the upstream-equivalent consumer discovery/needs-energy filtering before constructing `LegacyPowerConsumerList`; `PowerNodes` itself does not call `needsEnergy()`, matching upstream.
- Return the actual amount of amperage accepted by the consumer from `injectEnergy`.
- Attach `LegacyPowerNodePath` to every traversable edge and self path, and populate the corresponding direction locks exactly as the node builder does.
- Invoke on the logical server thread.

## Scope boundary

This milestone does not yet implement the outer source-injection loop in `MTECable.transferElectricity`, graph rebuild scheduling, actual GT consumer adapters, or NeoForge integration. In particular, callers still need to preserve the upstream partial-transfer retry protocol when `LegacyPowerConsumerList.isStale()` is true.

## Local verification

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerNodesTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
```

The implementation is not locally verified until both commands pass on the development machine.

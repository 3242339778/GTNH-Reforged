# Legacy Cable Transfer — Upstream Behavior Audit

## Baseline

Implementation target: GT5-Unofficial **5.09.54.275**.

Primary upstream sources:

- [MTECable.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/metatileentity/implementations/MTECable.java)
- [GenerateNodeMapPower.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/GenerateNodeMapPower.java)
- [GenerateNodeMap.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/GenerateNodeMap.java)
- [PowerNodes.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/PowerNodes.java)
- [NodeList.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/NodeList.java)
- [ConsumerNode.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.275/src/main/java/gregtech/api/graphs/consumers/ConsumerNode.java)

## Exact transfer sequence

`MTECable.transferElectricity(side, voltage, amperage, alreadyPassedSet)` performs these steps in order:

1. Return 0 when amperage <= 0, the base tile is not server-side, or the requested side is disconnected (unless side is `UNKNOWN`).
2. Set the cable's `energySource` flag to true. This flag is not reset by this method.
3. Read the cable's current node-map owner. If one exists and its map says refresh is due, clear that graph with return-node value -1.
4. If this cable has no node, construct `GenerateNodeMapPower` for the source pipe.
5. Continue only if the source node is a `PowerNode`.
6. Build the consumer list using the source node's `mHadVoltage` state.
7. If no consumer list is available, return 0.
8. Call `PowerNodes.powerNode` with **narrowed signed 32-bit values**: `(int) voltage` and `(int) amperage`.
9. Return the used amperage immediately if the consumer list is not stale, or if the original source node is invalid.
10. Otherwise clear the original graph, rebuild it from the source cable, and inspect the rebuilt source node.
11. If the first pass already used at least the original amperage, or rebuilding did not produce a `PowerNode`, return the first-pass used amperage.
12. Build a fresh consumer list for the rebuilt node. If it is null, return the first-pass used amperage.
13. Retry only the remaining amount: `(int) (amperage - usedAmperage)`. Return first-pass used plus second-pass used.

The partial-transfer rule is critical: a consumer may already have accepted energy before a later consumer causes topology invalidation. The second pass must never replay the first pass's accepted amperage.

## Consumer-list lifecycle

The private upstream `getConsumers(PowerNode)` has a one-time warm-up behavior:

- If `mHadVoltage` is false, it copies every consumer node into the list, sets `mHadVoltage = true`, and does not call `needsEnergy()` during this first list construction.
- On subsequent calls, it includes only consumers for which `needsEnergy()` returns true.
- If no consumer qualifies, it returns null.
- The flag belongs to the source `PowerNode`. A graph rebuild creates new nodes, so the rebuilt root has its own initial `mHadVoltage` state.
- `PowerNodes` itself does not call `needsEnergy()`; filtering is performed here.

For the base `ConsumerNode`, `needsEnergy()` means its backing tile entity is not invalid. Subclasses may implement their own behavior.

## Graph build and root value

`GenerateNodeMapPower(BaseMetaPipeEntity)` clears an existing node graph, or invalidates the existing node path's owner when there is no node, then calls:

`generateNode(sourcePipe, null, 1, null, UNKNOWN, ...)`

Therefore the source/root node starts at **node value 1**, not 0. The pure-Java `LegacyPowerNodeBuilder.build` previously started at 0; this was corrected in this milestone, and its tests now expect root value 1 and child values beginning at 2.

The power-specific generator also:
- recognizes only GT cable pipes as graph pipes;
- creates `PowerNode` for ordinary cable nodes;
- creates `EmptyPowerConsumer` for one-connection dead-end cable nodes;
- creates power-specific consumer adapters for GT, IEnergyConnected, Galacticraft, IC2, and RF receivers;
- creates `PowerNodePath` for all paths;
- performs IC2 adjacent-tile lookup with a chunk-loaded boundary check.

Those are host integration obligations; the generic builder alone does not implement these external consumer types.

## Numeric compatibility requirements

- The public transfer inputs are `long`, but upstream narrows voltage and amperage to signed Java `int` before the first PowerNodes call.
- The second pass independently narrows the remaining amperage to `int`.
- A compatibility implementation must preserve Java narrowing semantics, including wraparound for out-of-range values. Silently keeping full-width longs would change upstream behavior.
- `PowerNodes` returns a long count; the outer method accumulates first- and second-pass results as long.

## Existing implementation review

- `LegacyPowerNodes` reproduces the upstream traversal's no-progress guard; this guard is present in upstream `PowerNodes` and is not an added defensive behavior.
- `LegacyPowerNodeBuilder.build` root value was corrected from 0 to 1 to match `GenerateNodeMapPower`.
- `LegacyPowerConsumerAccess.needsEnergy()` exists, but the current consumer-list abstraction does not yet implement the source node's `mHadVoltage` warm-up/filter lifecycle.
- There is not yet a pure-Java outer transfer coordinator that owns the guard, delayed refresh, graph build/rebuild, and partial retry sequence.
- Actual source-pipe node/path ownership, power-node type discrimination, `mHadVoltage`, consumer adapter creation, and server tick access still need an explicit host boundary.

## Required next implementation

Add a pure-Java transfer coordinator and a narrow host interface that preserve:

- server-side / side-connected / positive-amperage guards;
- sticky energy-source marking;
- delayed map refresh and full graph clear/rebuild;
- root `PowerNode` eligibility;
- one-time all-consumer warm-up followed by `needsEnergy()` filtering;
- null consumer-list behavior;
- Java `long` to `int` narrowing at both PowerNodes call sites;
- stale-only rebuild and remaining-amperage retry;
- original-root invalidation short-circuit after the first pass.

The host adapter must construct the same ordered consumer set as `GenerateNodeMapPower`, including mod-specific consumer semantics. This audit does not claim that these external integrations are implemented.

## Verification status

- Upstream source audit: completed against 5.09.54.275.
- Root node value discrepancy: corrected in `LegacyPowerNodeBuilder`; local verification pending.
- Outer transfer coordinator: not implemented yet.
- Minecraft/NeoForge host integration: not implemented.

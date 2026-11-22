# Legacy Power Transfer Coordinator Milestone

## Scope

Implemented a pure-Java coordinator for the outer transfer lifecycle audited from GT5-Unofficial 5.09.54.275:

- `LegacyPowerTransferAccess`: host boundary for side/server checks, source flag, source node/map owner, tick, graph clear/build, power-node type check, and ordered consumer adapters.
- `LegacyPowerTransferCoordinator`: outer `MTECable.transferElectricity` control flow.
- `LegacyPowerGraphNode.hadVoltage`: source-node warm-up state used by `MTECable.getConsumers`.
- `LegacyPowerTransferCoordinatorTest`: guard behavior, first-list warm-up versus later `needsEnergy` filtering, and delayed graph refresh.

## Preserved upstream behavior

- Reject non-positive amperage, client-side calls, and disconnected sides except UNKNOWN before marking the cable as an energy source.
- `LegacyPowerDirection.UNKNOWN` is a non-indexed sentinel appended after the six cardinal directions; `orderedValues()` and `fromOrdinalIndex(0..5)` still expose exactly the original six-direction order.
- Energy-source marking is sticky and happens before graph refresh/build.
- Refresh an existing map owner only when the 10-tick refresh condition is due.
- Build a graph only when the source cable has no node.
- Stop if the source node is not the host-designated PowerNode subtype.
- First consumer-list construction includes every consumer and sets `hadVoltage`; it does not call `needsEnergy`.
- Subsequent consumer-list construction filters by `needsEnergy`; an empty filtered list returns null.
- First PowerNodes invocation narrows voltage and amperage from long to signed int.
- Return immediately when the first consumer list is not stale, or when the original source node became invalid.
- On stale topology, clear the original graph, rebuild from the source cable, and retry only `amperage - usedAmperage`.
- The retry independently narrows remaining amperage to signed int and adds the second pass result to the first.

## Host adapter obligations

The NeoForge host adapter must:

1. Preserve identity for source cable, graph nodes, map owners, paths, and consumers.
2. Return the actual shared graph owner for the source cable.
3. Implement graph clearing with the upstream return-node semantics.
4. Build using the power-specific generator, including PowerNode, EmptyPowerConsumer, PowerNodePath and the supported external consumer adapters.
5. Return consumer adapters in the exact order held by the built root's consumer list.
6. Implement `isPowerNode` as the equivalent of upstream `instanceof PowerNode`.
7. Supply the logical server tick and correct loaded-chunk/world behavior.

The coordinator deliberately does not know Minecraft, NeoForge, IC2, RF, Galacticraft, or GregTech tile classes.

## Verification

Tests added for:
- guards before source marking;
- UNKNOWN side bypass, while remaining outside the six-direction topology arrays;
- first consumer warm-up includes consumers without checking `needsEnergy`;
- later list construction filters by `needsEnergy`;
- delayed graph refresh clears and rebuilds the source graph.

The coordinator's end-to-end partial-injection retry still requires a dedicated test with real LegacyPowerNodes paths and a consumer that changes topology during injection. Existing LegacyPowerNodes tests separately cover stale marking and already-used amperage accounting, but they do not by themselves prove the coordinator's second-pass remainder behavior.

## Status

- Upstream audit: completed against 5.09.54.275.
- Coordinator implementation: committed; local Gradle verification pending.
- UNKNOWN direction sentinel and regression tests: committed; local Gradle verification pending.
- Host integration: not implemented.
- End-to-end stale retry test: pending.

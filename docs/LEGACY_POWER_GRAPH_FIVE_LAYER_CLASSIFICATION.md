# Legacy Power Graph — Five-Layer Classification

**Branch:** `feature/legacy-power-graph-core`  
**Scope:** current Java sources under `com.gtnh.reforged.legacy.power` and its `neoforge` subpackage.  
**Purpose:** classify the implementation as it exists, identify boundary violations, and define the smallest safe next refactor. This is a source review, not a claim that the in-game cable is implemented.

## 1. Summary

The graph algorithms and state transitions are predominantly Minecraft-independent. They form a usable Legacy Behavior Core candidate and expose narrow Java contracts. The current package layout, however, does not yet enforce the five-layer architecture:

1. The active `NeoForgeLegacyPowerNeighborAccess` has been moved to `legacy.power.bridge.neoforge`; it is the sole retained concrete world adapter.
2. `LegacyPowerCableHostState` is now a pure-Java state bundle; its former `createNeighborAccess(ServerLevel, BlockPos)` factory has been removed, eliminating the core-to-bridge dependency.
3. The duplicate `legacy.power.neoforge.NeoForgePowerNeighborAccess` implementation has been removed; the retained adapter uses `getChunkSource().getChunkNow` / `LevelChunk.getBlockEntity`.
4. No actual GT cable BlockEntity, block registration, BlockEntityType registration, or GameTest integration is present in this subsystem yet.

**Conclusion:** do not rewrite the graph algorithms. First correct the package/dependency boundaries and establish one authoritative world adapter. Then continue with a real registered cable host.

## 2. Layer assignment

### Layer 1 — Legacy API Runtime (contracts and ports)

These types define the legacy/domain vocabulary and host-facing contracts. They are Java-only today and should remain free of Minecraft/NeoForge implementation imports.

| Current type | Role |
|---|---|
| `LegacyPowerDirection` | Six physical directions plus the non-physical UNKNOWN sentinel |
| `LegacyPowerConnectionResult` | CONNECTED / NOT_CONNECTABLE / UNLOADED result contract |
| `LegacyPowerNeighborLookup` | Loaded-with-entity / loaded-empty / unloaded result contract; tile handle is opaque |
| `LegacyPowerCableAccess` | Cable-facing access contract |
| `LegacyPowerConsumerAccess` | Consumer acceptance/injection contract |
| `LegacyPowerNodeBinding` | Node binding contract |
| `LegacyPowerNodeBuildAccess` | Host operations required by graph construction |
| `LegacyPowerPathOwner` | Path owner contract |
| `LegacyPowerPipeBinding` | Path binding contract |
| `LegacyPowerTopologyAccess` | Topology read/host contract |
| `LegacyPowerTransferAccess` | Transfer host contract |

These are logical Layer 1 contracts, not a requirement to create a separate Gradle module immediately. Their package may later move to a dedicated API namespace once consumers stabilize.

### Layer 2 — Legacy Behavior Core

These classes implement algorithms, state transitions, ordering, counters, or cache semantics without importing Minecraft/Forge/NeoForge types.

| Current type | Preserved responsibility |
|---|---|
| `LegacyAveragePerTickCounter` | Legacy rolling per-tick counter behavior |
| `LegacyCableReadout` | Immutable cable readout data |
| `LegacyPowerCableState` | Node/path binding, connection snapshots, occupied sides, graph invalidation rules |
| `LegacyPowerConsumerList` | Consumer-list state and ordering |
| `LegacyPowerDirection` | Direction semantics consumed by the core (contract type; also listed in Layer 1) |
| `LegacyPowerGraphCleaner` | Legacy graph/path cleanup traversal |
| `LegacyPowerGraphNode` | Graph node state and ownership |
| `LegacyPowerGraphState` | Shared root, validity, topology version, refresh timing |
| `LegacyPowerNeighborCache` | Six-way cache state machine, empty markers, invalidation, identity and mismatch behavior |
| `LegacyPowerNodeBuilder` | Legacy node/path/lock construction algorithm |
| `LegacyPowerNodePath` | Power path state, cable limits, losses, overload and counters |
| `LegacyPowerNodes` | Legacy voltage/amperage traversal and injection behavior |
| `LegacyPowerPath` | Path ownership, validity and pipe bindings |
| `LegacyPowerPathLock` | Identity-based path locking |
| `LegacyPowerTransferCoordinator` | Transfer orchestration, stale-topology retry and remaining-amperage accounting |
| `LegacyPowerTopologyWalker` | Iterative degree-two topology walk |

The Layer 1 interfaces are ports used by these algorithms. The core may depend on those contracts, but must not depend on concrete Minecraft objects.

### Layer 3 — Minecraft Bridge

| Current type | Role and finding |
|---|---|
| `NeoForgeLegacyPowerNeighborAccess` (`legacy.power.bridge.neoforge` package) | Concrete 26.3 world/BlockEntity lookup adapter. It imports `BlockPos`, `Direction`, `ServerLevel`, `BlockEntity`, and `LevelChunk`; it belongs in the bridge namespace, not beside the pure core. |
| `LegacyPowerCableHostState.createNeighborAccess(...)` | Bridge construction currently embedded in a Java-only state bundle. Move this factory out; keep the bundle independent of Minecraft types. |
| `LegacyPowerNeighborLookup` | Bridge result contract with opaque object identity; logically Layer 1, consumed at this boundary. |
| `LegacyPowerNeighborCache` | Pure-Java cache behavior model used by the bridge and owned by host state; keep its implementation Minecraft-free. |

The bridge translates real world objects and chunk state into the core's opaque lookup contract. It must preserve unloaded versus loaded-empty, cached object identity, removed-object retry, and coordinate-mismatch behavior.

### Layer 4 — NeoForge Integration

**Current graph-subsystem implementation: none yet.**

This layer will own the real 26.3 game integration, including:
- registered cable Block and BlockEntityType;
- the actual BlockEntity that owns `LegacyPowerCableHostState`;
- DeferredRegister wiring and mod lifecycle;
- block/neighbor/validation/unload callbacks forwarded to the host state;
- capabilities, GameTests, and in-game diagnostics.

Do not move registration into the graph core or turn the host-state bundle itself into a BlockEntity.

### Layer 5 — Legacy Isolation Runtime

**No current graph class is assigned to this layer.**

The power graph is already being extracted into normal Java source and should stay on that path. There is no evidence that this subsystem currently requires a separate classloader or bytecode isolation.

## 3. Boundary violations and duplicate implementations

### 3.1 Minecraft imports in the base package

`NeoForgeLegacyPowerNeighborAccess.java` currently lives in `com.gtnh.reforged.legacy.power` despite importing Minecraft 26.3 classes. It now resides in:

```
com.gtnh.reforged.legacy.power.bridge.neoforge
```

Keep the pure contracts and graph classes in the core namespace. The move should be mechanical; do not change lookup behavior in the same commit.

### 3.2 Core state bundle constructs a concrete bridge

`LegacyPowerCableHostState` is intended to be a Minecraft-independent owner of `LegacyPowerCableState` and `LegacyPowerNeighborCache`. Its current `createNeighborAccess(ServerLevel, BlockPos)` method breaks that boundary.

The factory has been removed from the state bundle. The future BlockEntity (Layer 4), or a dedicated Layer 3 factory, should construct the bridge using the bundle's `neighborCache()`. This preserves stable cache identity without making the core know about `ServerLevel`.

### 3.3 Two world adapters

The repository contains both:
- `com.gtnh.reforged.legacy.power.NeoForgeLegacyPowerNeighborAccess`
- `com.gtnh.reforged.legacy.power.neoforge.NeoForgePowerNeighborAccess`

They are not equivalent implementations. The first uses `getChunkSource().getChunkNow(...)` and then `LevelChunk.getBlockEntity(...)`; the second uses `ServerLevel.isLoaded(...)` and `ServerLevel.getBlockEntity(...)`. `LegacyPowerCableHostState.createNeighborAccess` currently constructs the first.

Choose one authoritative adapter after checking the pinned NeoForge 26.3.0.113 compile and the no-force-load requirement. Do not keep two public implementations with subtly different world-query contracts. The older/unused implementation should only be removed after checking all source and test references.

### 3.4 Documentation drift

The host adapter audit previously named the subpackage class as the implemented adapter and described `isLoaded/getBlockEntity`. That does not match the root-package implementation currently constructed by `LegacyPowerCableHostState`. Update the audit to describe the actual class and implementation; retain any historical implementation notes only when clearly labeled as superseded.

## 4. Allowed dependency direction for this subsystem

```
Layer 1 contracts
      ^       ^
      |       |
Layer 2 core |
      ^       |
      |       |
Layer 3 Minecraft Bridge  <---- concrete 26.3 world objects
      ^
      |
Layer 4 NeoForge Integration (registered host + lifecycle)

Layer 5 remains isolated and unused for this subsystem.
```

More concretely:
- Layer 2 may depend on Layer 1.
- Layer 3 may depend on Layers 1 and 2 plus Minecraft 26.3 types.
- Layer 4 may depend on all normal layers and NeoForge.
- Layer 1 and Layer 2 must not import Minecraft, Forge, or NeoForge.
- Layer 2 must not construct Layer 3 adapters.
- No direct old 1.7.10 Minecraft objects may cross into the 26.3 world.

## 5. Recommended refactor sequence

1. Completed: move the active `NeoForgeLegacyPowerNeighborAccess` into `legacy.power.bridge.neoforge` without changing its lookup behavior.
2. Completed: remove `createNeighborAccess(ServerLevel, BlockPos)` from `LegacyPowerCableHostState`.
3. Completed: remove the duplicate `legacy.power.neoforge.NeoForgePowerNeighborAccess` implementation.
4. Next: add an automated boundary check ensuring core classes do not import `net.minecraft`, `net.neoforged`, or `net.minecraftforge`.
5. Continue with the actual cable Block + BlockEntity registration in Layer 4.
6. Add GameTests only after the real host exists; keep pure-Java unit tests as the fast behavior suite.

## 6. Verification status

- Source classification: completed from the current branch source files.
- Layer boundary refactor: bridge adapter moved, host-state factory removed, and duplicate adapter deleted in commits `5c03ad5`, `fd9fe13`, `170940b`, and `e7529dc`; local compile/test re-verification is pending.
- Pure Java graph unit tests: prior local user reports show the `legacy.power.*Test` suite passed; this review did not rerun local Gradle tasks.
- Minecraft bridge compilation: the current project conversation records a successful local `compileJava` for the root-package adapter on 2026-11-23. Re-run after any package move.
- Registered cable host and GameTests: not implemented / not verified in game.
- No code was moved or behavior changed by this classification document.

# Prototype BlockEntity lifecycle classification

**Date:** 2026-11-23  
**Target:** Minecraft 26.3 / NeoForge 26.3.0.113  
**Branch:** `feature/legacy-power-graph-core`

## Observed runtime sequences

The prototype BlockEntity emitted the following callback sequences in a real client world.

### Earlier chunk unload and reload observation

For the same server-side host instance, an earlier test recorded:

```text
onChunkUnloaded
setRemoved
```

The callback lines shared the same `hostState@` identity. A later `onLoad` at the same position had a new identity, which is expected because the BlockEntity and its runtime-only graph/cache state were reconstructed.

The client also emitted `onChunkUnloaded` followed by `setRemoved` during one observed client chunk unload.

### Latest user-provided runtime observations

The latest captured log showed a normal replacement/removal followed by a new placement:

```text
13:02:41 client setRemoved
13:02:41 server setRemoved, context=BLOCK_REPLACED
13:02:50 client onLoad
13:02:50 server onLoad
```

The user clarified that the cable was manually broken and placed again before walking away and returning. Therefore this sequence is **not** evidence of a chunk unload.

Subsequent distance-based tests still produced no `onChunkUnloaded` or `CHUNK_UNLOAD` line in the supplied log. The log alone does not prove whether the target chunk actually unloaded. A separate NeoForge chunk event diagnostic has therefore been added; do not infer unload from distance or from a new BlockEntity identity alone.

These observations demonstrate why absence of a preceding chunk-unload callback is not sufficient to infer player destruction.

## 26.3 source-level lifecycle findings

The inspected NeoForge `LevelChunk.clearAllBlockEntities()` patch explicitly calls `onChunkUnloaded()` on its BlockEntities before calling `setRemoved()` and clearing the map:

```java
this.blockEntities.values().forEach(BlockEntity::onChunkUnloaded);
this.blockEntities.values().forEach(BlockEntity::setRemoved);
this.blockEntities.clear();
```

The inspected `ChunkMap` patch posts `ChunkEvent.Unload` immediately before calling `this.level.unload(levelChunk)`. The inspected `ChunkEvent.Unload` documentation describes the event as firing just before the side-specific unload method. Taken together, the source-level expected order is:

```text
ChunkEvent.Unload
    -> LevelChunk unload
        -> LevelChunk.clearAllBlockEntities
            -> BlockEntity.onChunkUnloaded
            -> BlockEntity.setRemoved
```

This is a source-derived expectation, not yet a confirmed runtime sequence for the locally pinned 26.3.0.113 artifact. The event and callbacks must be compared in the actual client log before relying on the order.

Relevant inspected NeoForge source:
- [LevelChunk.java.patch](https://github.com/neoforged/NeoForge/blob/0da72d30889eb4d37eebbb70c84b83701829a6b5/patches/net/minecraft/world/level/chunk/LevelChunk.java.patch)
- [ChunkMap.java.patch](https://github.com/neoforged/NeoForge/blob/0da72d30889eb4d37eebbb70c84b83701829a6b5/patches/net/minecraft/server/level/ChunkMap.java.patch)
- [ChunkEvent.java](https://github.com/neoforged/NeoForge/blob/0da72d30889eb4d37eebbb70c84b83701829a6b5/src/main/java/net/neoforged/neoforge/event/level/ChunkEvent.java)

NeoForge's BlockEntity documentation identifies `preRemoveSideEffects` as a removal hook for side effects such as exporting container contents, but warns that some removal paths can skip it. It is not used as the sole graph-removal signal here.

The Minecraft 26.x `ChunkAccess`/chunk API exposes chunk-local block-state and BlockEntity lookup. The prototype bridge therefore uses `ServerChunkCache.getChunkNow` to inspect an already-loaded chunk; it does not request or generate a chunk as part of removal classification.

## Temporary ChunkEvent.Unload diagnostic

`PrototypePowerCableChunkUnloadDiagnostics` is a read-only NeoForge game-bus subscriber for `ChunkEvent.Unload`.

For each unloading chunk it:
- logs side and chunk coordinates;
- scans the chunk's BlockEntity positions for the prototype cable block;
- logs each matching cable position and its `hostState@` identity (or reports an unexpected/missing BlockEntity);
- logs the total number of prototype cable blocks found.

The listener does not mutate graph state, clear neighbor caches, force-load chunks, or change removal classification. It exists only to correlate the chunk event with the existing BlockEntity `onChunkUnloaded` and `setRemoved` diagnostics.

Expected server log sequence for a chunk containing one prototype cable:

```text
Prototype power cable chunk unload: event=ChunkEvent.Unload, side=server, chunk=(x, z), pos=..., hostState@...
Prototype power cable chunk unload: event=ChunkEvent.Unload, side=server, chunk=(x, z), cableCount=1
Prototype power cable lifecycle: callback=onChunkUnloaded, ...
Prototype power cable lifecycle: callback=setRemoved, context=CHUNK_UNLOAD, ...
```

The event's position relative to the BlockEntity callbacks is what the next runtime test must establish. A zero cable count means the event fired for a chunk without a registered prototype cable BlockEntity; it does not prove that the target chunk was the one expected.

## Conservative removal decision model

`LegacyPowerRemovalDecision` is a pure-Java decision model. Its results are:

- `CHUNK_UNLOAD`: the host observed `onChunkUnloaded()` before `setRemoved()`.
- `BLOCK_REPLACED`: the already-loaded world chunk reports a different block at the host position.
- `HOST_REPLACED`: the cable block is still present, but the chunk no longer maps that position to this exact BlockEntity instance.
- `UNKNOWN`: the chunk/world state is unavailable, or the observations do not prove a removal cause.

The order is deliberate: chunk unload takes precedence; missing world information is conservative; a different block is a block replacement; a missing/different BlockEntity under the same cable block is a host-instance replacement. A same-block state-property update is not classified as a block replacement merely because the state properties differ.

The Minecraft-facing `PrototypePowerCableBlockEntity` performs inspection only on the logical server and only through `getChunkNow`. Client-side callbacks remain diagnostic and do not perform world-based classification.

**No decision currently invokes `clearGraphImmediately()`.** The decision model and the new chunk-event listener are diagnostics only. Before wiring graph cleanup, the actual callback ordering and all removal paths must be validated against the pinned 26.3.0.113 runtime.

## Unit-test coverage

`LegacyPowerRemovalDecisionTest` covers:

- chunk-unload precedence;
- unavailable chunk/world state => `UNKNOWN`;
- a different current block => `BLOCK_REPLACED`;
- the cable block remaining while its exact host instance is no longer registered => `HOST_REPLACED`;
- the original host still being registered => `UNKNOWN`.

Existing `LegacyPowerCableHostStateTest` continues to cover callback-sequence tracking and neighbor-cache lifecycle behavior. The chunk-event diagnostic is a Minecraft/NeoForge integration listener and does not add pure-Java behavior to those tests.

## Local verification required

After pulling the branch, run:

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerRemovalDecisionTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerCableHostStateTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
.\gradlew.bat compileJava
```

Then launch the client and test:

1. player breaking the prototype cable;
2. replacing it with a different block;
3. replacing/removing the BlockEntity while retaining the cable block, if a controlled test path is available;
4. same-block state-property changes, if the prototype gains such a property;
5. chunk unload and reload without manually touching the cable.

For the final test, correlate the `ChunkEvent.Unload` line, cable position/host identity, `onChunkUnloaded`, `setRemoved`, and later `onLoad`. Do not interpret `UNKNOWN` as permission to clear the graph.

## Integration gate

Before graph cleanup is enabled:

1. Confirm the exact call order in the pinned 26.3.0.113 runtime for each test above.
2. Confirm that the chunk event identifies the expected chunk and cable host.
3. Confirm that `getChunkNow` and the chunk-local lookups behave as expected during `setRemoved()`.
4. Confirm that `HOST_REPLACED` cannot be produced by an ordinary same-host lifecycle transition.
5. Define whether a host-instance replacement should invalidate only its owned graph/path or the whole shared graph root.
6. Gate all graph mutation to the logical server.
7. Add runtime tests before enabling destructive cleanup.

Until these checks pass, `setRemoved()` remains a cache lifecycle callback plus conservative diagnostics, not an unconditional permanent graph-invalidation event.

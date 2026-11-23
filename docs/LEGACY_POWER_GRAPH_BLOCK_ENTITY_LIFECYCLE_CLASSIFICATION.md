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

### Latest user-provided runtime observation

The latest log contained two removal/reload sequences:

```text
12:41:21 client setRemoved
12:41:21 server setRemoved
12:41:22 client onLoad
12:41:22 server onLoad
12:41:47 client setRemoved
12:41:47 server setRemoved
```

All four `setRemoved` callbacks were previously classified as `WITHOUT_CHUNK_UNLOAD`. No `onChunkUnloaded` callback appeared in that captured interval. The first removal was followed by new BlockEntity instances at the same position; the log alone did not establish why.

These observations demonstrate why absence of a preceding chunk-unload callback is not sufficient to infer player destruction.

## 26.3 source-level lifecycle findings

The 26.3 Forge patch for `LevelChunk.clearAllBlockEntities()` explicitly calls `onChunkUnloaded()` on its BlockEntities before calling `setRemoved()` and clearing the map.

NeoForge's BlockEntity documentation identifies `preRemoveSideEffects` as a removal hook for side effects such as exporting container contents, but warns that some removal paths can skip it. It is not used as the sole graph-removal signal here.

The Minecraft 26.x `ChunkAccess`/chunk API exposes chunk-local block-state and BlockEntity lookup. The prototype bridge therefore uses `ServerChunkCache.getChunkNow` to inspect an already-loaded chunk; it does not request or generate a chunk as part of removal classification.

## Conservative removal decision model

`LegacyPowerRemovalDecision` is a pure-Java decision model. Its results are:

- `CHUNK_UNLOAD`: the host observed `onChunkUnloaded()` before `setRemoved()`.
- `BLOCK_REPLACED`: the already-loaded world chunk reports a different block at the host position.
- `HOST_REPLACED`: the cable block is still present, but the chunk no longer maps that position to this exact BlockEntity instance.
- `UNKNOWN`: the chunk/world state is unavailable, or the observations do not prove a removal cause.

The order is deliberate: chunk unload takes precedence; missing world information is conservative; a different block is a block replacement; a missing/different BlockEntity under the same cable block is a host-instance replacement. A same-block state-property update is not classified as a block replacement merely because the state properties differ.

The Minecraft-facing `PrototypePowerCableBlockEntity` now performs the inspection only on the logical server and only through `getChunkNow`. Client-side callbacks remain diagnostic and do not perform world-based classification.

**No decision currently invokes `clearGraphImmediately()`.** This change adds a tested decision model and runtime diagnostics only. Before wiring graph cleanup, the actual callback ordering and all removal paths must be validated against the pinned 26.3.0.113 runtime.

## Unit-test coverage

`LegacyPowerRemovalDecisionTest` covers:

- chunk-unload precedence;
- unavailable chunk/world state => `UNKNOWN`;
- a different current block => `BLOCK_REPLACED`;
- the cable block remaining while its exact host instance is no longer registered => `HOST_REPLACED`;
- the original host still being registered => `UNKNOWN`.

Existing `LegacyPowerCableHostStateTest` continues to cover callback-sequence tracking and neighbor-cache lifecycle behavior.

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
5. chunk unload and reload.

Expected diagnostic labels on the server are `CHUNK_UNLOAD`, `BLOCK_REPLACED`, `HOST_REPLACED`, or `UNKNOWN`. Do not interpret `UNKNOWN` as permission to clear the graph.

## Integration gate

Before graph cleanup is enabled:

1. Confirm the exact call order in the pinned 26.3.0.113 runtime for each test above.
2. Confirm that `getChunkNow` and the chunk-local lookups behave as expected during `setRemoved()`.
3. Confirm that `HOST_REPLACED` cannot be produced by an ordinary same-host lifecycle transition.
4. Define whether a host-instance replacement should invalidate only its owned graph/path or the whole shared graph root.
5. Gate all graph mutation to the logical server.
6. Add runtime tests before enabling destructive cleanup.

Until these checks pass, `setRemoved()` remains a cache lifecycle callback plus conservative diagnostics, not an unconditional permanent graph-invalidation event.

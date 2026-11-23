# Prototype BlockEntity lifecycle classification

**Date:** 2026-11-23  
**Target:** Minecraft 26.3 / NeoForge 26.3.0.113  
**Branch:** `feature/legacy-power-graph-core`

## Observed runtime sequences

The prototype BlockEntity emitted the following callback sequences in a real client world.

### Chunk unload and reload

For the same server-side host instance:

```text
onChunkUnloaded
setRemoved
```

The callback lines shared the same `hostState@` identity. A later `onLoad` at the same position had a new identity, which is expected because the BlockEntity and its runtime-only graph/cache state were reconstructed.

The client also emitted `onChunkUnloaded` followed by `setRemoved` during client chunk unload.

### Player breaking the prototype block

The observed break sequence ended with:

```text
client: setRemoved
server: setRemoved
```

No `onChunkUnloaded` was observed immediately before these two callbacks. The client and server had distinct BlockEntity/host-state instances.

## Conclusion

`BlockEntity.setRemoved()` is not by itself equivalent to the permanent-removal meaning of the old GTNH 1.7.10 `invalidate()` callback. The observed chunk-unload path invokes both `onChunkUnloaded()` and `setRemoved()`, while the observed player-break path invokes `setRemoved()` without a preceding `onChunkUnloaded()`.

This is evidence for distinguishing callback context, not proof that every possible removal path has been covered. In particular, replacement by another block, block-entity type changes, and unusual chunk teardown paths still require verification.

## Pure-Java lifecycle classifier

`LegacyPowerCableHostState` now records whether `onChunkUnload()` has occurred since the most recent `onValidate()`. The next `onSetRemoved()` returns one of:

- `AFTER_CHUNK_UNLOAD`: a chunk-unload callback was observed first; the pending marker is consumed.
- `WITHOUT_CHUNK_UNLOAD`: no chunk-unload callback was observed first.

The classifier does not access Minecraft state and does not clear the graph. It is deliberately only a callback-sequence classifier. The Minecraft-facing host must still inspect the world/block removal context before deciding whether to invoke immediate graph cleanup.

`PrototypePowerCableBlockEntity.setRemoved()` now records the classification in its temporary lifecycle log. The current implementation still only clears negative neighbor-cache markers; it does not yet clear the graph on a player break. This is intentional until the actual block-replacement condition and all removal paths are verified.

## Unit-test coverage added

`LegacyPowerCableHostStateTest` now covers:

- chunk unload followed by `setRemoved()` => `AFTER_CHUNK_UNLOAD`;
- `setRemoved()` without a preceding chunk unload => `WITHOUT_CHUNK_UNLOAD`;
- a subsequent `onValidate()` resets the pending chunk-unload context;
- existing negative-cache clearing and positive-cache retention behavior remains covered.

These are source-level test additions. They are **not yet reported as locally executed**; run the commands below after pulling the commits.

## Local verification

```powershell
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.LegacyPowerCableHostStateTest" --rerun
.\gradlew.bat test --tests "com.gtnh.reforged.legacy.power.*Test" --rerun
.\gradlew.bat compileJava
```

After launching the client, inspect the lifecycle log. Expected diagnostic context:

- chunk unload sequence: `onChunkUnloaded`, then `setRemoved context=AFTER_CHUNK_UNLOAD`;
- player break sequence: `setRemoved context=WITHOUT_CHUNK_UNLOAD` on the corresponding client/server instances.

Do not interpret `WITHOUT_CHUNK_UNLOAD` as definitive proof of player destruction. It means only that no chunk-unload callback was observed on that host instance before `setRemoved()`.

## Next integration gate

Before invoking `clearGraphImmediately()` from the real host:

1. Verify the block state currently stored at the BlockEntity position during `setRemoved()` for the pinned 26.3.0.113 artifact.
2. Confirm whether the current state still represents this exact prototype block or has been replaced.
3. Verify the same behavior for player break, replacement by another block, block-entity type change, and chunk unload.
4. Gate all graph mutation to the logical server.
5. Add tests for the removal decision itself; do not let a callback-name mapping silently decide graph semantics.

Until those checks pass, `setRemoved()` remains a cache lifecycle callback plus diagnostic classification, not a permanent graph-invalidation event.

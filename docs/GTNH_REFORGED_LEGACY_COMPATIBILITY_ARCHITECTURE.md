# GTNH-Reforged Legacy Compatibility Architecture

**Status:** Accepted architectural direction; implementation is incremental.  
**Target runtime:** Minecraft Java Edition 26.3 / NeoForge 26.3.0.113.  
**Historical behavior baseline:** GTNH 1.7.10, with the audited GT5-Unofficial 5.09.54.275 and GTNH 2.9.1 baselines where applicable.

## 1. Purpose

GTNH-Reforged aims to port the actual GTNH 1.7.10 experience to Minecraft 26.3. The default strategy is to preserve upstream source structure, content, rules, and observable gameplay behavior as far as practical, while adapting the old integration points to the modern runtime.

This is not a plan to run an entire Minecraft 1.7.10 installation inside 26.3. It is a compatibility architecture that lets us choose the least destructive migration technique for each subsystem:

1. retain and adapt source where direct recompilation is practical;
2. extract Minecraft-independent legacy behavior into testable code;
3. translate old host interactions through explicit bridges;
4. integrate those bridges with NeoForge lifecycle and services;
5. isolate only the exceptional components that cannot reasonably be brought across the normal source path.

A successful compile is not proof of a successful port. Compatibility is established by source audits, behavioral tests, and ultimately in-game verification.

## 2. Architectural overview

```text
                 Historical GTNH 1.7.10 sources
                              |
                 source preservation / porting
                              |
             +----------------+----------------+
             |                                 |
      Legacy API Runtime              Legacy Behavior Core
      old mod-facing contracts        original algorithms/rules
             |                                 |
             +----------------+----------------+
                              |
                       Minecraft Bridge
                  old concepts <-> 26.3 objects
                              |
                    NeoForge Integration
               registries / events / lifecycle /
                 capabilities / rendering / net
                              |
                    Minecraft 26.3 runtime

       Exceptional legacy components only:
             Legacy Isolation Runtime
       remap + isolated loading + explicit boundary
```

The five architectural layers are logical boundaries first. They do not require five Gradle subprojects on day one. Keep the current build simple until dependency direction and public contracts are stable; split into Gradle modules only when that gives enforceable isolation rather than administrative overhead.

## 3. The five layers

### 3.1 Legacy API Runtime

**Responsibility:** provide the old mod-facing contracts that migrated source needs, without pretending that the old Minecraft runtime exists.

Examples:
- compatibility interfaces and method contracts for old GT/Forge-era integrations;
- legacy lifecycle and callback abstractions where their semantics can be represented safely;
- adapters for old mod-facing APIs that are implemented by the bridge or NeoForge layer;
- explicit compatibility types for concepts such as legacy directions, connection results, and transfer requests.

Rules:
- Do not create counterfeit `net.minecraft.*`, `net.minecraftforge.*`, or `cpw.mods.fml.*` classes on the normal 26.3 application classpath merely to make old source compile.
- Do not claim an API is compatible just because a method with the same name exists. Preserve parameter meaning, return values, ordering, null behavior, side/thread assumptions, and failure semantics.
- Keep contracts small and document the upstream version/source they represent.
- If source needs an old type that is inseparable from the old game runtime, classify it for deeper porting or isolation instead of adding an unsafe placeholder.

### 3.2 Legacy Behavior Core

**Responsibility:** preserve original algorithms, rules, and state transitions wherever they can be expressed independently of Minecraft objects.

Examples:
- GT5U power graph topology, path ownership, locks, consumer ordering, and transfer behavior;
- recipe matching, recipe conditions, machine processing rules, and progression calculations;
- material properties and processing semantics;
- legacy search/filter/indexing behavior that is not inherently GUI-bound.

Rules:
- Prefer plain Java and stable domain types.
- No direct imports from Minecraft, Forge, or NeoForge.
- Keep upstream behavior traceable: record source file, commit/tag, and any intentional deviations.
- Tests should assert observable behavior and edge cases, not merely implementation details.
- Do not simplify behavior to fit a modern API. If a behavior cannot be preserved yet, document the gap explicitly.

### 3.3 Minecraft Bridge

**Responsibility:** translate between legacy/domain concepts and concrete Minecraft 26.3 objects.

Examples:
- world and block-entity lookup;
- old TileEntity-style host state mapped onto BlockEntity lifecycle;
- legacy item/fluid representations mapped to modern stacks and registries;
- neighbor and chunk lookup semantics;
- energy consumer adapters and machine host interfaces;
- data conversion between legacy NBT/config/recipe representations and modern data structures.

Rules:
- This is the deliberate boundary where Minecraft types are allowed to meet legacy contracts.
- Preserve important distinctions such as unloaded vs loaded-empty, absent vs invalid, and cached vs freshly queried.
- Never force-load chunks unless the original operation explicitly did so and the new design has a reviewed equivalent.
- Do not leak modern world objects into the pure behavior core.
- Conversion must be explicit, directional where necessary, and tested for lossiness.

### 3.4 NeoForge Integration

**Responsibility:** own the actual 26.3 mod lifecycle and register/connect the migrated systems to NeoForge.

Examples:
- DeferredRegister for blocks, items, block entities, menus, and other registries;
- mod-bus and game-bus event integration;
- BlockEntity and Block lifecycle callbacks;
- capabilities and modern energy/fluid/item integration;
- rendering, networking, commands, configuration, and data generation;
- GameTest registration and in-game diagnostics.

Rules:
- Use the pinned NeoForge 26.3 API, not remembered Forge 1.7.10 patterns.
- Keep registration and lifecycle code thin; move domain decisions into the core or bridge.
- Respect logical-side and server-thread requirements.
- Verify API signatures against the exact pinned dependency and compile locally.
- A registered placeholder is not a completed migrated feature.

### 3.5 Legacy Isolation Runtime

**Responsibility:** provide an exceptional compatibility path for components that cannot reasonably be source-ported or safely linked into the normal runtime.

Potential mechanisms:
- dedicated classloader boundaries;
- bytecode transformation and reference remapping;
- relocated legacy API namespaces;
- controlled proxies and DTO/message boundaries;
- explicit serialization and lifecycle translation.

Rules:
- This is a last-resort subsystem, not the default migration path.
- A separate ClassLoader alone does not solve duplicate Minecraft types, linkage, reflection, annotations, serialization, callbacks, or object identity.
- Never pass old Minecraft objects directly across the isolation boundary.
- Cross-boundary communication must use stable interfaces, primitive/domain DTOs, or explicit adapters.
- Evaluate each candidate with a proof-of-concept before committing to a general runtime.
- Do not attempt to boot a complete 1.7.10 Forge/FML environment unless a measured requirement justifies that much scope.

## 4. Dependency direction

The allowed dependency direction is:

```text
Legacy API Runtime <----- Legacy Behavior Core
       ^                         ^
       |                         |
       +------ Minecraft Bridge -+
                    ^
                    |
            NeoForge Integration

Legacy Isolation Runtime is separately bounded and may communicate
with the main runtime only through explicit bridge contracts.
```

More precisely:

| Layer | May depend on | Must not depend on |
|---|---|---|
| Legacy API Runtime | Java platform and narrowly scoped domain contracts | Minecraft 26.3, NeoForge implementation classes |
| Legacy Behavior Core | Java platform, Legacy API Runtime | Minecraft, Forge, NeoForge |
| Minecraft Bridge | Legacy API Runtime, Legacy Behavior Core, Minecraft 26.3 types | arbitrary UI/registration implementation details |
| NeoForge Integration | all normal layers plus NeoForge | old runtime implementation details leaking into public APIs |
| Legacy Isolation Runtime | its own relocated/isolated legacy environment and bridge protocol | direct shared Minecraft object identity |

Avoid cycles. In particular, the core must not call back into a concrete BlockEntity or ServerLevel. Supply a narrow interface from the host side instead.

## 5. Type and object boundary policy

The following rules apply to all migrated subsystems:

1. **No same-name type illusion.** Never define fake old Minecraft classes in the live 26.3 namespace.
2. **No cross-runtime object leakage.** A 1.7.10 object and a 26.3 object are not interchangeable even when they represent the same conceptual thing.
3. **Use domain contracts.** Examples include `LegacyPowerDirection`, `LegacyPowerNeighborLookup`, `LegacyPowerConnectionResult`, and narrow access interfaces.
4. **Preserve identity where behavior depends on identity.** Graph owners, path bindings, cached block entities, and lock owners may require reference identity rather than value equality.
5. **Preserve null and tri-state semantics.** Do not collapse unloaded, absent, invalid, and rejected into one generic null/false result.
6. **Make lossy conversion visible.** Document what cannot be represented and add tests for round trips where round trips are expected.
7. **Keep thread and side assumptions explicit.** World mutation and graph lifecycle are server-thread operations unless proven otherwise.

## 6. Migration workflow

Each subsystem should pass through these gates:

1. **Baseline audit** — identify exact upstream version, files, callers, data sources, and observable behavior.
2. **Classification** — decide which parts are API contracts, pure behavior, host integration, UI/rendering, or isolation candidates.
3. **Behavior extraction** — move Minecraft-independent logic into the Legacy Behavior Core without changing semantics.
4. **Contract design** — define the smallest host-facing interfaces and explicit result types.
5. **Bridge implementation** — implement modern world/object access and conversion.
6. **NeoForge integration** — register real game objects and connect lifecycle/events/capabilities.
7. **Verification** — unit tests, integration/GameTests, dedicated-server checks, and in-game behavior comparisons.
8. **Deviation record** — document every behavior not yet reproduced, intentional change, and unresolved uncertainty.

Do not skip the audit because a compile error appears easy to fix. A compile fix that changes semantics is a migration defect.

## 7. Verification strategy

Use a layered test strategy:

- **Core unit tests:** deterministic behavior, edge cases, identity, ordering, arithmetic, and state transitions.
- **Bridge tests:** conversion rules, cache behavior, unloaded chunks, invalid objects, and host callbacks.
- **NeoForge GameTests/integration tests:** actual registered blocks and block entities, lifecycle, neighbor updates, chunk boundaries, capabilities, and server-side behavior.
- **Dedicated-server/runtime tests:** startup, registry correctness, persistence, networking, and no client-only classloading on server.
- **Behavior comparison:** compare against the audited upstream implementation and representative fixtures.

Every milestone must state exactly what was verified. "Compiles" must not be described as "works in game."

## 8. Current reference implementation: Legacy Power Graph

The existing Legacy Power Graph work is the first reference implementation of this architecture:

- `LegacyPowerGraphState`, topology/path/node classes, and transfer coordination are Legacy Behavior Core candidates.
- `LegacyPowerNeighborLookup`, `LegacyPowerNeighborCache`, and host-facing access contracts define the bridge boundary.
- `NeoForgeLegacyPowerNeighborAccess` is a Minecraft-facing bridge prototype using loaded-chunk-only lookup.
- `LegacyPowerCableHostState` bundles host-owned state for a future real cable BlockEntity.
- Real block/block-entity registration and GameTest coverage remain subsequent integration work.

The pure-Java test suite and compile checks have passed in the user's local environment as recorded in project conversation. This does not yet establish in-game correctness or a registered working cable.

Use this subsystem to validate the architecture, but do not force unrelated systems to copy its exact class structure when their upstream behavior requires different boundaries.

## 9. Explicit non-goals and rejected shortcuts

This architecture does not authorize:

- a full 1.7.10 Minecraft/Forge emulator as the default strategy;
- fake `net.minecraft.*` classes on the main runtime classpath;
- blind mass replacement of old method names with superficially similar new APIs;
- replacing GTNH-specific consumer, recipe, rendering, or lifecycle semantics with generic NeoForge behavior without equivalence tests;
- treating JEI as the recipe viewer implementation (the project direction is first-party ReforgedRecipes + ReforgedRecipeViewer; JEI remains excluded);
- declaring a subsystem complete based only on compilation;
- deleting unsupported content to make the port easier without an explicit, documented decision.

## 10. Decision record

The accepted direction is:

**Source-compatible migration where practical + behavior-preserving pure-Java core + explicit Minecraft bridge + thin NeoForge integration + isolated legacy runtime only for exceptional components.**

This is a guiding architecture, not a claim that all five layers already exist as independent Gradle modules. The implementation should evolve from proven boundaries and tests.

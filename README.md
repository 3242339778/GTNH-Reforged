# GTNH: Reforged

A behavior-preserving porting project targeting Minecraft Java Edition **26.3** on **NeoForge**.

> **Status:** Early implementation. The repository contains a NeoForge bootstrap, a pure-Java Legacy Power Graph implementation, and a compile-verified NeoForge neighbor-access prototype. The local power-graph unit-test suite has passed. A real registered GT cable Block/BlockEntity and in-game GameTest verification are still pending.
>
> **Target:** Minecraft 26.3 / NeoForge 26.3.0.113 / Java 25.

## Project direction

The goal is to bring the actual GTNH 1.7.10 content and gameplay behavior to Minecraft 26.3—not merely to create a new mod inspired by GTNH.

The default migration policy is to preserve upstream source structure, content, rules, and observable behavior as far as practical. Modern Minecraft and NeoForge integration belongs at explicit compatibility boundaries. A compile or successful startup alone does not count as a completed port.

Historical baselines and audits are recorded in `docs/`, including the GTNH 2.9.1 modpack baseline and the GT5-Unofficial 5.09.54.275 power-network behavior audit.

## Architecture

The accepted long-term direction has five logical layers:

1. **Legacy API Runtime** — compatibility contracts for migrated old mod-facing APIs; it does not counterfeit old Minecraft classes.
2. **Legacy Behavior Core** — original algorithms and rules expressed independently of Minecraft objects.
3. **Minecraft Bridge** — explicit conversion and host access between legacy/domain contracts and 26.3 objects.
4. **NeoForge Integration** — registries, events, lifecycle, capabilities, rendering, networking, and GameTests.
5. **Legacy Isolation Runtime** — exceptional fallback for components that cannot reasonably be source-ported; not the default strategy.

These are logical boundaries first, not a requirement to create five Gradle subprojects immediately. See [Legacy Compatibility Architecture](docs/GTNH_REFORGED_LEGACY_COMPATIBILITY_ARCHITECTURE.md).

## Core principles

- Preserve GTNH 1.7.10 content and behavior; do not silently simplify or delete features.
- Audit the exact upstream version and callers before adapting an API.
- Keep Minecraft-independent behavior testable and free of Minecraft/Forge/NeoForge imports.
- Keep old/new object conversion explicit; never pass incompatible runtime objects across the boundary.
- Preserve identity, ordering, null behavior, unloaded-chunk distinctions, lifecycle timing, and thread/side assumptions.
- Use the pinned NeoForge 26.3 API rather than assuming legacy Forge patterns still apply.
- Record deviations and unverified behavior explicitly.
- Respect upstream licenses and asset provenance.

## Current implementation

The Legacy Power Graph is the first architecture reference implementation. It extracts GT5-Unofficial topology, path ownership, node traversal, voltage/amperage handling, transfer coordination, cable state, neighbor-cache semantics, and a NeoForge world-neighbor adapter.

The current work is not yet a complete in-game power network. Real cable block/block-entity registration, lifecycle wiring, GameTests, and end-to-end gameplay validation remain future milestones.

## Planned major systems

- **ReforgedCore** — lifecycle, identifiers, registries, configuration, and diagnostics.
- **Legacy API Runtime / Legacy Behavior Core** — compatibility contracts and preserved GTNH behavior.
- **Minecraft Bridge / NeoForge Integration** — modern game-object, lifecycle, capability, rendering, and networking integration.
- **ReforgedMaterials** — material definitions, forms, properties, and processing semantics.
- **ReforgedRecipes** — authoritative recipe data, validation, generation, and machine integration.
- **ReforgedRecipeViewer** — first-party recipe search, categories, ingredient display, machine/process views, and navigation. JEI is excluded.
- **ReforgedProgression** — technology stages, unlock rules, and dependency graphs.
- **ReforgedQuests** — quest integration and player-facing progression guidance.

## Technical baseline

| Area | Version |
|---|---|
| Minecraft | 26.3 |
| NeoForge | 26.3.0.113 |
| Java | 25 |
| ModDevGradle | 2.0.148 |
| Recipe viewer | First-party Reforged implementation; JEI excluded |

See [Build Setup](docs/BUILD_SETUP.md) for local build instructions. Exact build-tool wrapper settings must be taken from the checked-out branch, not inferred from this table.

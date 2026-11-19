# GTNH: Reforged

An independent, modernized reimplementation inspired by the GregTech: New Horizons (GTNH) experience.

> **Status:** Early implementation. A minimal NeoForge bootstrap and build configuration are now present, but the project has not yet been verified by a successful build or game launch.
>
> **Target:** Minecraft Java Edition 26.3 on NeoForge.

## Project direction

GTNH: Reforged is a clean modern re-architecture. Minecraft 1.7.10 and historical GTNH are research references only, not the runtime foundation.

The goal is to preserve GTNH's defining experience: long-term progression, demanding survival, complex industrial chains, deep material processing, and meaningful technological milestones.

## Technical baseline

| Area | Decision |
|---|---|
| Minecraft | Java Edition 26.3 |
| Loader | NeoForge 26.3.0.113 |
| Java | 25 |
| Build plugin | ModDevGradle 2.0.148 |
| Gradle | 9.2.1 |
| Recipe viewer | First-party Reforged implementation; JEI excluded |

Exact versions are recorded in `gradle.properties`. See [Build Setup](docs/BUILD_SETUP.md).

## Principles

- Build directly for Minecraft 26.3 and NeoForge.
- Study historical GTNH mechanics without assuming legacy APIs or code are reusable.
- Keep Reforged-owned systems modular, testable, and maintainable.
- Preserve meaningful complexity and progression.
- Prefer architectural performance work and measurement over mass mod removal.
- Build a first-party recipe viewer. JEI is explicitly excluded.
- Distinguish verified implementation facts from proposals and untested assumptions.
- Respect upstream licenses and asset provenance.

## Planned systems

- **ReforgedCore** — lifecycle, identifiers, registries, configuration, and diagnostics.
- **ReforgedMaterials** — material definitions, forms, properties, and processing semantics.
- **ReforgedRecipes** — recipe declarations, validation, generation, and machine integration.
- **ReforgedRecipeViewer** — custom recipe search, categories, ingredient display, machine/process views, and navigation.
- **ReforgedProgression** — technology stages, unlock rules, and dependency graphs.
- **ReforgedQuests** — quest integration and player-facing progression guidance.

## Current implementation

The first code entry point is deliberately minimal: it proves the mod bootstrap boundary without prematurely choosing material, machine, or recipe APIs.

Nothing is considered implemented or tested until a reproducible build or runtime test demonstrates it.

# GTNH: Reforged

An independent, modernized reimplementation inspired by the GregTech: New Horizons (GTNH) experience.

> Status: Early planning and architecture stage. This repository is not yet a playable modpack or a compilable mod.
> Target: Minecraft Java Edition 26.3. This is a project target, not a claim of current compatibility or a working build.

## Project direction

GTNH: Reforged targets Minecraft 26.3 on NeoForge with a clean modern architecture. Minecraft 1.7.10 and historical GTNH are research references only, not the runtime foundation.

The goal is to preserve GTNH's defining experience: long-term progression, demanding survival, complex industrial chains, deep material processing, and meaningful technological milestones.

## Principles

- Build directly for Minecraft 26.3 and NeoForge.
- Study historical GTNH mechanics without assuming legacy APIs or code are reusable.
- Keep Reforged-owned systems modular, testable, and maintainable.
- Preserve meaningful complexity and progression.
- Prefer architectural performance work and measurement over mass mod removal.
- Build a first-party recipe viewer. JEI is explicitly excluded.
- Distinguish verified implementation facts from proposals and untested assumptions.
- Respect upstream licenses and asset provenance.

## Initial technical direction

| Area | Decision | Status |
|---|---|---|
| Minecraft | Java Edition 26.3 | Project target; not yet implemented or verified |
| Historical reference | GTNH / Minecraft 1.7.10 | Research only |
| Java runtime | Java 25 or newer | Target requirement; verify against official metadata |
| Mod loader | NeoForge | Selected; exact build and prototype still pending |
| Build system / mappings | NeoForge-supported Gradle toolchain | Exact versions to be pinned |
| Recipe viewer | Reforged-owned implementation | JEI excluded |
| Development model | Independent clean re-architecture | Selected |

These are planning decisions, not claims that the project currently builds or runs.

## Planned systems

- ReforgedCore — lifecycle, identifiers, registries, configuration, and diagnostics.
- ReforgedMaterials — material definitions, forms, properties, and processing semantics.
- ReforgedRecipes — recipe declarations, validation, generation, and machine integration.
- ReforgedRecipeViewer — custom recipe search, categories, ingredient display, machine/process views, and navigation.
- ReforgedProgression — technology stages, unlock rules, and dependency graphs.
- ReforgedQuests — quest integration and player-facing progression guidance.
- NeoForge platform integration and narrow optional compatibility adapters.

## Development roadmap

1. Set up and verify a minimal NeoForge 26.3 client/server development project.
2. Specify the GTNH gameplay systems and progression to preserve, redesign, replace, or omit.
3. Define core lifecycle, identifiers, registries, configuration, diagnostics, and module boundaries.
4. Implement one material and one processing chain.
5. Build the first-party recipe viewer on top of a stable recipe query API.
6. Connect machines, progression, and quests.
7. Benchmark critical paths and publish reproducible builds.

## Research status

Historical GTNH research is reference material, not a modern implementation. Nothing is considered implemented or tested until a reproducible build or runtime test demonstrates it.

## License

No project-wide license has been selected. Until one is added, do not assume repository contents may be reused, redistributed, or relicensed.
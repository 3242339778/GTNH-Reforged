# GTNH: Reforged

An independent, deep-refactoring project built around the GregTech: New Horizons (GTNH) experience.

> **Status:** Early planning and architecture stage. This repository is not yet a playable modpack or a compilable mod.

## Project vision

GTNH: Reforged aims to preserve the core identity of GTNH—long-term progression, demanding survival, complex industrial chains, and meaningful technological milestones—while building an independently maintained progression, material, recipe, and content architecture.

The goal is not to replace complexity with simplification. The goal is to make complexity more deliberate, coherent, extensible, and maintainable.

## Principles

- Study the upstream implementation before changing it.
- Keep Reforged changes isolated from upstream repositories.
- Prefer small, reviewable changes with repeatable tests.
- Preserve compatibility where practical; document intentional breaks.
- Avoid treating mass mod removal as the primary performance strategy.
- Distinguish verified implementation facts from design proposals.
- Respect the licenses of all upstream code and assets.

## Initial technical baseline

| Area | Current decision |
|---|---|
| Minecraft | 1.7.10 |
| Upstream baseline | GTNH 2.9.0 Stable (exact source commit still to be verified) |
| Java | Java 21 is the proposed development target; legacy Forge/Gradle compatibility must be proven before it is treated as supported |
| Development model | Independent derivative / deep refactor |
| Default branch | `main` |

These are planning constraints, not claims that the project currently builds or runs.

## Planned modules

- **ReforgedCore** — shared registries, lifecycle, configuration, and compatibility contracts.
- **ReforgedMaterials** — material definitions, forms, properties, and registration adapters.
- **ReforgedRecipes** — recipe declarations, generation, validation, and migration tools.
- **ReforgedProgression** — progression stages, unlock rules, and technology dependencies.
- **ReforgedQuests** — quest integration and documentation of progression.
- **docs/** — research notes, decisions, compatibility records, and development plans.

## Development roadmap

1. **Baseline research:** pin the exact GTNH release/source revision; inventory mods, dependencies, scripts, configs, and build tooling.
2. **Core architecture:** define lifecycle, registries, IDs, configuration, and compatibility boundaries.
3. **Material vertical slice:** register one original material, generate its intended forms, and validate its processing chain.
4. **Recipe and progression integration:** connect recipes to machine systems and progression gates.
5. **Mod ecosystem review:** evaluate integrations and replacements (including recipe-viewer options) through compatibility and performance tests.
6. **Playtesting and release engineering:** automate checks, document known issues, and publish reproducible builds.

## Research status

Research documents in this repository are design notes unless explicitly marked as source-verified or tested. Code examples must not be assumed to compile until checked against the pinned upstream revision and the actual legacy toolchain.

## Contributing / workflow

Use focused branches such as `feature/material-registry` and `fix/recipe-validation`. Keep changes small, explain the motivation, and record relevant test results. Do not commit proprietary files, credentials, private mod jars, or unlicensed assets.

## License

No project-wide license has been selected yet. Until a license is added, do not assume that repository contents may be reused, redistributed, or relicensed. Any reused upstream code remains subject to its original license and notices.

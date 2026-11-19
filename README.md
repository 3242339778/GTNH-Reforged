# GTNH: Reforged

An independent, modernized reimplementation inspired by the GregTech: New Horizons (GTNH) experience.

> **Status:** Early planning and architecture stage. This repository is not yet a playable modpack or a compilable mod.
>
> **Target:** Minecraft Java Edition 26.3. This is a project target, not a claim of current compatibility or a working build.

## Project direction

GTNH: Reforged will be built directly for **Minecraft 26.3** using a new, modern architecture. Minecraft 1.7.10 and the historical GTNH codebase are research references only; they are not the runtime foundation of this project.

The goal is to preserve the defining GTNH experience—long-term progression, demanding survival, complex industrial chains, deep material processing, and meaningful technological milestones—while rethinking the implementation for the modern Minecraft ecosystem.

This is a clean re-architecture, not a version-number update or a promise that legacy mods can be moved unchanged.

## Principles

- Design for the selected modern Minecraft target instead of preserving legacy technical constraints.
- Study historical GTNH behavior and mechanics, but do not assume legacy APIs or code are reusable.
- Keep Reforged-owned systems modular, testable, and independently maintainable.
- Preserve complexity and meaningful progression; do not simplify the pack merely to make migration easier.
- Prefer architectural performance work and measurement over mass mod removal.
- Evaluate modern loaders, APIs, and integrations before committing to them.
- Distinguish verified implementation facts from proposals and untested assumptions.
- Respect the licenses of all upstream code, documentation, and assets.

## Initial technical direction

| Area | Decision | Status |
|---|---|---|
| Minecraft | Java Edition 26.3 | Project target; not yet implemented or verified |
| Historical reference | GTNH / Minecraft 1.7.10 | Research only; not the runtime baseline |
| Java runtime | Java 25 or newer for Minecraft 26.3 | Target requirement to verify against official runtime metadata |
| Java compatibility research | Java 17–26 | Investigation range, not a promise that MC 26.3 runs on every version |
| Mod loader | Undecided | Compare current supported options before selection |
| Build system | Undecided | Select after loader and development toolchain research |
| Development model | Independent clean re-architecture | Project decision |
| Default branch | `main` | Repository setting |

These are planning decisions, not claims that the project currently builds or runs.

## Planned systems

- **ReforgedCore** — lifecycle, stable identifiers, registries, configuration, diagnostics, and shared contracts.
- **ReforgedMaterials** — material definitions, forms, properties, and processing semantics.
- **ReforgedRecipes** — recipe declarations, validation, generation, and machine integration.
- **ReforgedProgression** — technology stages, unlock rules, and dependency graphs.
- **ReforgedQuests** — quest integration and player-facing progression guidance.
- **Modern compatibility layer** — narrow adapters for the selected loader and approved ecosystem dependencies.
- **docs/** — research notes, architecture decisions, compatibility records, and development plans.

## Development roadmap

1. **Modern platform research:** verify Minecraft 26.3 requirements; compare loader, mappings, APIs, build tooling, and ecosystem maturity.
2. **Gameplay specification:** document the GTNH mechanics and progression that must be preserved, redesigned, or replaced.
3. **Core architecture:** define lifecycle, identifiers, registries, configuration, diagnostics, and module boundaries.
4. **Material vertical slice:** implement one original material and validate its forms and processing chain.
5. **Recipes and progression:** connect recipes to machine systems, technology gates, and quest guidance.
6. **Performance and integration:** benchmark critical paths and validate selected integrations.
7. **Playtesting and release engineering:** automate checks, document known issues, and publish reproducible builds.

## Research status

Historical GTNH research is reference material, not a modern implementation. Any legacy API, code sample, or design proposal must be re-evaluated against the selected 26.3 platform. Nothing is considered implemented or tested until a reproducible build or runtime test demonstrates it.

## Contributing / workflow

Use focused branches such as `feature/material-registry` and `fix/recipe-validation`. Keep changes small, explain the motivation, and record relevant test results. Do not commit credentials, private mod jars, or unlicensed assets.

## License

No project-wide license has been selected yet. Until a license is added, do not assume that repository contents may be reused, redistributed, or relicensed. Any reused upstream code remains subject to its original license and notices.

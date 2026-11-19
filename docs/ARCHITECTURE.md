# Architecture overview

## Target

The project targets Minecraft Java Edition 26.3 on NeoForge. Exact NeoForge and Gradle versions will be pinned after the minimal project prototype.

Historical GTNH 1.7.10 is a source of gameplay research, not a runtime dependency.

## Intended layers

1. NeoForge platform integration — lifecycle hooks, registries, networking, data systems, and client/server boundaries.
2. Compatibility adapters — narrow integrations for explicitly selected third-party mods and APIs.
3. Reforged Core — identifiers, lifecycle coordination, configuration, diagnostics, validation, and shared contracts.
4. Domain systems — materials, recipes, machines, progression, quests, world content, and original gameplay.
5. Presentation — custom recipe viewer, tooltips, machine screens, search, and player-facing progression guidance.

## First-party recipe viewer

The recipe viewer is a Reforged-owned system and must not depend on JEI.

- Recipe model: normalized categories, item/fluid inputs and outputs, conditions, processing time, energy, and machine requirements.
- Query/index service: indexed lookup by ingredient, fluid, machine, category, and progression stage. Avoid full-database scans for every UI interaction.
- Presentation: category browser, recipe details, ingredient reverse lookup, processing-chain navigation, and search.
- Integration adapters: optional external recipe sources isolated from the viewer core.
- Diagnostics: missing references, duplicate recipe IDs, invalid categories, and missing presentation assets.

The viewer consumes a stable recipe-query API rather than directly inspecting every mod's internal registries. Cache invalidation and data reload behavior must be explicit and testable.

## Design rules

- Keep NeoForge-specific types at the platform boundary where practical.
- Keep third-party APIs behind narrow adapters.
- Separate common/server logic from client-only presentation code.
- Use stable namespaced IDs and deterministic data definitions.
- Fail early with actionable diagnostics.
- Avoid hidden global mutation and order-dependent registration.
- Measure performance with profiling and repeatable benchmarks.
- Keep JEI out of the dependency graph.

## First vertical slice

1. Create a minimal NeoForge 26.3 mod that loads on client and dedicated server.
2. Register one Reforged-owned material and its forms.
3. Define one processing recipe and expose it through a stable query API.
4. Display that recipe in a minimal first-party viewer screen.
5. Verify data reload, search responsiveness, and server/client separation.
6. Build reproducibly from a clean checkout.

All API names remain proposals until checked against the selected 26.3 toolchain.
# Architecture overview

## Target
The project targets Minecraft Java Edition 26.3 on NeoForge. The initial development baseline is NeoForge 26.3.0.113, ModDevGradle 2.0.148, Gradle 9.2.1, and Java 25. A successful build and runtime launch are still verification gates.

Historical GTNH 1.7.10 is the authoritative reference for gameplay content, source behavior, recipes, progression, and integration intent. Reuse original source where technically and legally possible. The old Minecraft/Forge runtime is not assumed to be embeddable unchanged.

## Compatibility strategy
1. Preserve original gameplay behavior, content, data, and progression as the primary requirement.
2. Retain original Java source unchanged when it is independent of old Minecraft/Forge APIs and its license permits reuse.
3. Where old APIs are involved, prefer source-compatible facades and narrow object/behavior bridges.
4. Reinstall hooks using NeoForge events, Mixin, or class processors where the old lifecycle or injection mechanism no longer exists.
5. Reconstruct only the runtime-specific portions that cannot safely be reused. Do not treat an unverified subsystem as impossible or silently omit it.
6. Track source provenance, license obligations, and behavioral regression tests for reused/adapted code.

A successful compile or launch is not proof of migration completeness. Each preserved behavior needs an explicit verification path.

## Intended layers
1. **NeoForge platform integration** — lifecycle hooks, registries, networking, data systems, rendering, and client/server boundaries.
2. **Legacy compatibility runtime** — old API facades, lifecycle dispatch, object bridges, accessors, behavior hooks, and narrowly scoped class transformation.
3. **Compatibility adapters** — isolated integrations for IC2 and other historical third-party mods/APIs.
4. **Reforged Core** — identifiers, lifecycle coordination, configuration, diagnostics, validation, and shared contracts.
5. **Domain systems** — materials, recipes, machines, progression, quests, world content, and preserved GTNH gameplay.
6. **Presentation** — custom recipe viewer, tooltips, machine screens, search, and player-facing progression guidance.

## Legacy compatibility boundaries
- Pure Java algorithms and data should be retained unchanged where possible and tested against historical outputs.
- Old ItemStack, Block, TileEntity, NBT, fluid, inventory, network, and GUI contracts need explicit facade/bridge designs; modern Minecraft objects must not be casually confused with old types.
- Old Mixin behavior must be documented by target, injection point, conditions, side effects, and regression test before choosing a modern hook.
- Old ASM transformers must be analyzed for every suppressed side effect; reproduce their intent, not necessarily their old bytecode edits.
- Rendering must preserve geometry, animation, color, depth, transparency, and visibility while using the modern submission pipeline.
- Optional third-party integrations must not leak their API types into the domain core.

## First-party recipe viewer
The recipe viewer is a Reforged-owned system and must not depend on JEI.
- **Recipe model:** normalized categories, item/fluid inputs and outputs, conditions, processing time, energy, and machine requirements.
- **Query/index service:** indexed lookup by ingredient, fluid, machine, category, and progression stage. Avoid full-database scans for every UI interaction.
- **Presentation:** category browser, recipe details, ingredient reverse lookup, processing-chain navigation, and search.
- **Integration adapters:** optional external recipe sources isolated from the viewer core.
- **Diagnostics:** missing references, duplicate recipe IDs, invalid categories, and missing presentation assets.
The viewer consumes a stable recipe-query API rather than directly inspecting every mod's internal registries. Cache invalidation and data reload behavior must be explicit and testable. The initial data-model proposal is documented in [Recipe Data Model](RECIPE_DATA_MODEL.md).

## Design rules
- Keep NeoForge-specific types at the platform boundary where practical.
- Keep third-party APIs behind narrow adapters.
- Separate common/server logic from client-only presentation code.
- Use stable namespaced IDs and deterministic data definitions.
- Fail early with actionable diagnostics.
- Avoid hidden global mutation and order-dependent registration.
- Measure performance with profiling and repeatable benchmarks.
- Keep JEI out of the dependency graph.
- Never remove historical content solely to reduce implementation effort; record a reason and an explicit project decision for any unavoidable deviation.

## First vertical slice
1. Verify the minimal NeoForge 26.3 mod on a clean build.
2. Launch a client and a dedicated server.
3. Register one Reforged-owned material and its forms.
4. Define one processing recipe and expose it through a stable query API.
5. Display that recipe in a minimal first-party viewer screen.
6. Verify data reload, search responsiveness, and server/client separation.
7. In parallel, establish a source/behavior ledger for the historical GTNH systems and a minimal Legacy API proof of concept.

All API names remain proposals until implemented and checked against the selected 26.3 toolchain.

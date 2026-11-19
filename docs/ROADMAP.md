# Roadmap

## Phase 0 — Direction and repository
- [x] Create project repository and initial documentation.
- [x] Select direct modernization for Minecraft 26.3.
- [x] Mark Minecraft 1.7.10 / historical GTNH as research-only.
- [x] Select NeoForge as the target mod loader.
- [x] Decide to build a first-party recipe viewer; JEI is excluded.
- [x] Publish preliminary Fabric / NeoForge platform research.
- [ ] Pin exact NeoForge, Gradle, and mappings/toolchain versions.
- [ ] Record licenses and provenance for research inputs.

## Phase 1 — Gameplay and architecture specification
- [ ] Inventory defining GTNH gameplay systems and progression.
- [ ] Classify each system: preserve, redesign, replace, or omit.
- [ ] Define module boundaries and client/server responsibilities.
- [ ] Define identifiers, registries, configuration, and diagnostics.
- [ ] Specify recipe data model and recipe-query API.
- [ ] Specify custom viewer UX, indexing, and search behavior.

## Phase 2 — Minimal NeoForge platform
- [ ] Create a minimal Minecraft 26.3 NeoForge development project.
- [ ] Build and launch a client.
- [ ] Build and launch a dedicated server.
- [ ] Add automated checks and reproducible build instructions.

## Phase 3 — Material and recipe vertical slice
- [ ] Specify MaterialDefinition and MaterialForms concepts.
- [ ] Implement material registration and one test material.
- [ ] Define and validate a small processing chain.
- [ ] Expose recipe metadata through the query API.
- [ ] Verify behavior in a running 26.3 instance.

## Phase 4 — Custom recipe viewer
- [ ] Implement recipe category and detail models.
- [ ] Implement indexed item/fluid/machine/category lookup.
- [ ] Build category browser, recipe detail, and ingredient navigation.
- [ ] Add cache invalidation and data-reload handling.
- [ ] Profile search and UI performance with a large synthetic recipe set.
- [ ] Verify JEI is absent from dependencies.

## Phase 5 — Machines and progression
- [ ] Implement initial machine and processing integration.
- [ ] Detect duplicate, invalid, and unreachable recipes.
- [ ] Implement progression dependencies and unlock rules.
- [ ] Connect progression to quest guidance.

## Phase 6 — Ecosystem, performance, and release
- [ ] Audit candidate integrations and licenses.
- [ ] Benchmark loading, recipe lookup, world generation, and UI paths.
- [ ] Run regression, multiplayer, and long-session tests.
- [ ] Publish setup documentation, known issues, and reproducible builds.

## Non-goals
- Blindly porting the 1.7.10 codebase.
- Claiming every historical GTNH mod has a 26.3 equivalent.
- Depending on JEI for recipe browsing.
- Optimizing performance solely by removing groups of mods.
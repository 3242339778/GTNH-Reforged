# Roadmap

## Phase 0 — Direction and repository
- [x] Create project repository.
- [x] Add initial documentation and module layout.
- [x] Select direct modernization for Minecraft 26.3 (Route A).
- [x] Mark Minecraft 1.7.10 / historical GTNH as research-only.
- [ ] Record authoritative Minecraft 26.3 Java/runtime requirements.
- [ ] Compare 26.3 mod loaders, mappings, APIs, and ecosystem support.
- [ ] Select and pin the development toolchain.
- [ ] Record licenses and provenance for all research inputs.

## Phase 1 — Gameplay and architecture specification
- [ ] Inventory defining GTNH gameplay systems and progression.
- [ ] Classify each system: preserve, redesign, replace, or intentionally omit.
- [ ] Define module boundaries, lifecycle, and client/server responsibilities.
- [ ] Define stable namespaced IDs, registries, configuration, and diagnostics.
- [ ] Document compatibility and dependency policy.

## Phase 2 — Minimal modern platform
- [ ] Create a minimal Minecraft 26.3 development project.
- [ ] Build and launch a client.
- [ ] Build and launch a dedicated server.
- [ ] Add automated checks and reproducible build instructions.

## Phase 3 — Material vertical slice
- [ ] Specify modern MaterialDefinition and MaterialForms concepts.
- [ ] Implement material registration using the selected platform.
- [ ] Add one test material and its initial forms.
- [ ] Define and validate a small processing chain.
- [ ] Verify behavior in a running 26.3 instance.

## Phase 4 — Recipes, machines, and progression
- [ ] Design recipe declaration and validation.
- [ ] Implement initial machine/processing integration.
- [ ] Detect duplicate, invalid, and unreachable recipes.
- [ ] Implement progression dependencies and unlock rules.
- [ ] Connect progression to quest documentation.

## Phase 5 — Ecosystem and performance
- [ ] Audit candidate mod integrations and conflicts.
- [ ] Evaluate recipe-viewer options available for the chosen loader/version.
- [ ] Benchmark loading, recipe lookup, world generation, and UI paths.
- [ ] Run regression, multiplayer, and long-session tests.

## Phase 6 — Playtesting and release
- [ ] Complete a representative progression slice.
- [ ] Publish installation and server setup documentation.
- [ ] Record known issues and compatibility matrix.
- [ ] Produce reproducible test builds before public release.

## Non-goals

- Blindly porting the 1.7.10 codebase.
- Claiming every historical GTNH mod has a 26.3 equivalent.
- Treating Java 17–26 as a guarantee that MC 26.3 runs on all those JDKs.
- Optimizing performance solely by removing large groups of mods.

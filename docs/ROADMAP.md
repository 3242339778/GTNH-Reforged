# Roadmap

## Phase 0 — Direction and repository
- [x] Create project repository and initial documentation.
- [x] Select Minecraft 26.3 and NeoForge as the target platform.
- [x] Establish historical GTNH 1.7.10 as the source, content, and behavior reference.
- [x] Decide to build a first-party recipe viewer; JEI is excluded.
- [x] Publish preliminary platform research.
- [x] Pin an initial NeoForge / Gradle / Java toolchain from the official 26.3 MDK.
- [x] Add a minimal NeoForge mod bootstrap and CI build workflow.
- [x] Add the initial Legacy Behavior Compatibility Audit.
- [ ] Verify a clean build and client/server launch.
- [ ] Record licenses and provenance for research inputs.

## Phase 1 — Historical source and behavior audit
- [ ] Inventory GT5-Unofficial, NewHorizonsCoreMod, modpack data, and exact release-pinned dependencies.
- [ ] Build a per-class source/import/API dependency index.
- [ ] Record every Mixin target, injection point, condition, and behavior.
- [ ] Record every Coremod/ASM transformation and all affected side effects.
- [ ] Inventory recipes, materials, machines, world generation, quests, configuration, and progression.
- [ ] Classify each behavior as direct reuse, API facade, object bridge, hook reinstallation, runtime reconstruction, or unverified.
- [ ] Record license, provenance, and source-version metadata for every reused/adapted source unit.
- [ ] Define behavioral regression tests for representative legacy systems.

## Phase 2 — Legacy compatibility runtime
- [ ] Implement lifecycle dispatcher and ordering model.
- [ ] Prototype original pure-Java source reuse (XSTR baseline).
- [ ] Prototype a narrow legacy registration facade.
- [ ] Define modern object bridges for ItemStack, BlockEntity, NBT/data components, fluids, inventory, and networking.
- [ ] Prototype Mixin/class-processor registration using the selected NeoForge 26.3 release.
- [ ] Implement legacy accessor contracts only where modern public APIs are insufficient.
- [ ] Establish common/client/server isolation.

## Phase 3 — Minimal NeoForge platform and verification
- [x] Create a minimal Minecraft 26.3 NeoForge development project.
- [ ] Build and launch a client.
- [ ] Build and launch a dedicated server.
- [ ] Add automated checks and reproducible build instructions.
- [ ] Verify all compatibility hooks on both logical sides where applicable.

## Phase 4 — Materials and recipes
- [x] Define the first material identity and logical forms.
- [ ] Register the material forms as Minecraft content.
- [ ] Import and validate historical material definitions and forms.
- [ ] Import historical recipe data with source provenance.
- [ ] Define and validate representative processing chains.
- [ ] Expose recipe metadata through the stable query API.
- [ ] Recreate historical recipe suppression/replacement behavior and test it.

## Phase 5 — Custom recipe viewer
- [ ] Implement recipe category and detail models.
- [ ] Implement indexed item/fluid/machine/category lookup.
- [ ] Build category browser, recipe detail, and ingredient navigation.
- [ ] Add cache invalidation and data-reload handling.
- [ ] Profile search and UI performance with a large synthetic recipe set.
- [ ] Verify JEI is absent from dependencies.

## Phase 6 — Machines, world, and progression
- [ ] Implement legacy machine behavior through modern block entities and bridges.
- [ ] Preserve energy, fluid, item, structure, and multiblock semantics.
- [ ] Migrate world generation, pollution, dimensions, and spawn rules.
- [ ] Import quests, progression dependencies, and unlock rules.
- [ ] Connect progression to quest guidance.
- [ ] Validate save/load and multiplayer behavior.

## Phase 7 — Ecosystem, performance, and release
- [ ] Audit candidate integrations and licenses.
- [ ] Benchmark loading, recipe lookup, world generation, rendering, and UI paths.
- [ ] Run regression, multiplayer, save migration, and long-session tests.
- [ ] Publish setup documentation, known issues, and reproducible builds.

## Non-goals
- Blindly compiling the entire 1.7.10 codebase against 26.3 without API/runtime analysis.
- Embedding the complete old Minecraft/Forge runtime unchanged inside the new runtime.
- Claiming every historical third-party mod has a ready-made 26.3 port.
- Depending on JEI for recipe browsing.
- Removing historical gameplay systems merely to reduce implementation effort.

# Minecraft 26.3 platform research

> Status: Research snapshot checked 2026-11-19. NeoForge has now been selected. This document does not claim a successful build in this repository.

## Project decisions

- Minecraft: Java Edition 26.3.
- Loader: NeoForge selected.
- Recipe viewer: first-party Reforged implementation.
- JEI: explicitly rejected; do not add it as a dependency.
- Build tooling: exact NeoForge, Gradle, mappings, and plugin versions remain to be pinned in a minimal prototype.

Fabric remains a viable loader in the wider ecosystem, but it is no longer the project target unless a future explicit decision changes the platform.

## Platform comparison summary

Both Fabric and NeoForge have documented Minecraft 26.3 development paths. General mod-count readiness was not decisive for this content-heavy industrial project. NeoForge is selected as the target platform; this does not imply that every historical Forge mod has a compatible 26.3 release.

Every proposed dependency still requires exact version, license, maintenance, client/server, and compatibility review.

## Toolchain notes

- NeoForge's official getting-started documentation provides a JDK 25 setup path.
- The NeoForge project listing provides the 26.3 version line.
- Select exact NeoForge, Gradle, and plugin versions from the current official template and documentation.
- Do not copy old ForgeGradle versions or 1.7.10 build scripts into this project.

## Recipe viewer decision

JEI is not acceptable for this project and will not be used. EMI and REI are not selected as the primary viewer either: the project will own its recipe-viewer experience.

The viewer will use a first-party recipe model and query/index service. This avoids coupling the UI to a third-party viewer's release schedule or interaction model.

Initial capabilities:
- Search by item, fluid, machine, recipe category, and eventually progression stage.
- Recipe details with inputs, outputs, processing time, energy, and machine requirements.
- Ingredient-to-recipe reverse lookup.
- Navigation across related recipes and processing chains.
- Indexed queries, caching, and explicit invalidation on data reload.
- Client-only presentation backed by a server-safe recipe data layer.

## Required implementation gates

- [ ] Pin NeoForge 26.3, Gradle, mappings, and JDK versions.
- [ ] Build and launch a minimal NeoForge client.
- [ ] Build and launch a dedicated server.
- [ ] Define the Reforged recipe schema and query API.
- [ ] Prototype a recipe category and searchable UI screen.
- [ ] Benchmark search latency and memory with a large synthetic recipe set.
- [ ] Verify JEI is absent from runtime and development dependencies.

## Sources

- Fabric 26.3: https://www.fabricmc.net/2026/09/15/263.html
- Fabric Develop: https://fabricmc.net/develop/
- NeoForged Getting Started: https://docs.neoforged.net/docs/gettingstarted/
- NeoForge project listing: https://projects.neoforged.net/neoforged/neoforge/
- NeoForge 26.2 to 26.3 primer: https://docs.neoforged.net/primer/docs/26.3/
- Mod readiness sample: https://modshift.app/version/26.3

These sources describe platform status at the time checked. They do not establish that this repository has built or launched successfully.
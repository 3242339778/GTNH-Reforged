# Technical baseline

## Project target

| Item | Baseline | Status |
|---|---|---|
| Minecraft | Java Edition 26.3 | Project target |
| Historical GTNH | Minecraft 1.7.10 | Research reference only |
| Java runtime/toolchain | Java 25 | Matches current 26.3 MDK target |
| Mod loader | NeoForge 26.3.0.113 | Initial version pinned from official MDK; build not yet verified |
| Build plugin | ModDevGradle 2.0.148 | Initial version pinned from official MDK |
| Gradle | 9.2.1 | Initial version pinned from official MDK |
| Recipe viewer | First-party Reforged implementation | JEI explicitly excluded |

## Compatibility policy

Minecraft 26.3 on NeoForge is the project target. Do not change loader or lower the Minecraft target without an explicit project decision.

Java 25 is the configured toolchain. Re-check the official MDK and runtime metadata before upgrading Minecraft or changing the JDK baseline.

## Platform setup

The initial Gradle project uses the official NeoForge 26.3 ModDevGradle development path. Client, dedicated-server, GameTest-server, and data-generation run configurations are declared in `build.gradle`.

## Recipe viewer policy

The project will implement its own recipe viewer and recipe-query infrastructure. JEI must not be added as a dependency. The viewer consumes Reforged-owned recipe and category metadata; optional external recipe integrations, if ever needed, must remain isolated behind adapters.

## Verification record

- [x] Add a minimal NeoForge mod entry point.
- [x] Add initial Gradle and ModDevGradle configuration.
- [x] Add CI build workflow.
- [ ] Obtain a successful clean build.
- [ ] Launch a client.
- [ ] Launch a dedicated server.

No successful 26.3 build or launch is claimed yet.

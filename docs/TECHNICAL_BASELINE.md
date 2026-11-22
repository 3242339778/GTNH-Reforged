# Technical baseline

## Project target

| Item | Baseline | Status |
|---|---|---|
| Minecraft | Java Edition 26.3 | Project target |
| Historical GTNH | Minecraft 1.7.10 | Source, content, and behavior reference |
| Java runtime/toolchain | Java 25 | Matches current 26.3 MDK target |
| Mod loader | NeoForge 26.3.0.113 | Initial version pinned from official MDK; build/runtime verification remains required |
| Build plugin | ModDevGradle 2.0.148 | Initial version pinned from official MDK |
| Gradle | 9.2.1 | Initial version pinned from official MDK |
| Recipe viewer | First-party Reforged implementation | JEI explicitly excluded |

## Compatibility policy

Minecraft 26.3 on NeoForge is the project target. Do not change loader or lower the Minecraft target without an explicit project decision.

Historical GTNH 1.7.10 is not merely inspiration or disposable research material. Its source, content, and observable behavior are the migration reference. Reuse original source where technically and legally possible. Do not assume that old Minecraft/Forge runtime classes can be loaded unchanged alongside 26.3 classes.

Java 25 is the configured toolchain. Re-check the official MDK and runtime metadata before upgrading Minecraft or changing the JDK baseline.

## Compatibility implementation policy

Prefer, in order:
1. unchanged reuse of pure Java/data source;
2. source-compatible legacy API facades;
3. explicit object and behavior bridges;
4. NeoForge events, Mixin, and class processors for hook/lifecycle changes;
5. targeted runtime reconstruction where the old engine contract no longer exists.

A risk classification is an estimate of effort and validation needs, not a declaration that a feature is impossible. Unverified behavior must remain tracked until investigated. Every deliberate deviation from historical behavior requires a documented reason and project decision.

## Platform setup

The initial Gradle project uses the official NeoForge 26.3 ModDevGradle development path. Client, dedicated-server, GameTest-server, and data-generation run configurations are declared in build.gradle.

## Recipe viewer policy

The project will implement its own recipe viewer and recipe-query infrastructure. JEI must not be added as a dependency. The viewer consumes Reforged-owned recipe and category metadata; optional external recipe integrations, if ever needed, must remain isolated behind adapters.

## Verification record

- [x] Add a minimal NeoForge mod entry point.
- [x] Add initial Gradle and ModDevGradle configuration.
- [x] Add CI build workflow.
- [ ] Obtain a successful clean build.
- [ ] Launch a client.
- [ ] Launch a dedicated server.

No successful clean 26.3 build or client/server launch is claimed here until the checks are actually observed.

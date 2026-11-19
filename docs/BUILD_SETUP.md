# Build setup — Minecraft 26.3 / NeoForge

## Pinned initial toolchain

These versions match the current official NeoForge 26.3 ModDevGradle MDK snapshot checked on 2026-11-19. They are an initial project baseline, not yet a successful local build record.

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Java toolchain | 25 |
| NeoForge | 26.3.0.113 |
| ModDevGradle | 2.0.148 |
| Gradle | 9.2.1 |

## Requirements

- Install a JDK 25 distribution and ensure `JAVA_HOME` points to it.
- Install Gradle 9.2.1, or import the project in an IDE with Gradle support.
- Use the Gradle project import/sync before attempting a launch.

## Common tasks

From the repository root:

```bash
gradle build
gradle runClient
gradle runServer
gradle runData
```

The first Gradle invocation downloads the Minecraft and NeoForge development dependencies. A network connection and sufficient disk space are required.

## Verification status

The project skeleton and CI workflow are committed, but no successful build or game launch is claimed yet. The CI workflow is the first reproducible verification gate. Once a local/CI build passes, record the exact result here and add the Gradle wrapper files.

# Build setup — Minecraft 26.3 / NeoForge

## Pinned initial toolchain

These versions match the official NeoForge 26.3 ModDevGradle MDK snapshot checked on 2026-11-19. They are an initial project baseline; CI is responsible for verifying the actual build.

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
- Use Gradle project import/sync before attempting a launch.

## Common tasks

From the repository root:

```bash
gradle build
gradle runClient
gradle runServer
gradle runData
```

The first Gradle invocation downloads Minecraft and NeoForge development dependencies. A network connection and sufficient disk space are required.

## Mod metadata

The NeoForge descriptor template is stored in `src/main/templates/META-INF/neoforge.mods.toml`. The `generateModMetadata` task expands the Gradle properties and adds the generated descriptor to the main resource set. The `check` lifecycle also runs `verifyModMetadata`, which confirms that the packaged JAR contains the expected descriptor and no unresolved placeholders remain.

## Verification status

The GitHub Actions workflow runs the Gradle build on Java 25 with Gradle 9.2.1. A successful build is not claimed until the workflow reports success. Client and dedicated-server launch checks remain separate follow-up gates.

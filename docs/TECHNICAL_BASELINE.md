# Technical baseline

## Project target

| Item | Baseline | Status |
|---|---|---|
| Minecraft | Java Edition 26.3 | Explicit project target; implementation not started |
| Historical GTNH | Minecraft 1.7.10 | Research reference only |
| Java runtime | Java 25 or newer | Target requirement; verify against official metadata |
| Mod loader | NeoForge | Selected; exact build not pinned |
| Mappings and APIs | NeoForge 26.3 toolchain | To be selected with prototype |
| Build system | Gradle with NeoForge-supported tooling | Exact versions to be pinned |
| Recipe viewer | First-party Reforged implementation | JEI explicitly excluded |

## Compatibility policy

Minecraft 26.3 on NeoForge is the project target. Do not change loader or lower the Minecraft target without an explicit project decision.

Java 25+ is the current runtime target to verify for Minecraft 26.3. Any broader Java compatibility investigation is not a claim that this Minecraft version runs on older JDKs.

## Platform setup gates

1. Verify the Minecraft 26.3 Java requirement using official version metadata.
2. Select the current NeoForge 26.3 build and supported Gradle plugin/toolchain.
3. Pin mappings, Gradle, and compiler settings.
4. Create a minimal NeoForge development project.
5. Build and launch both a client and dedicated server.
6. Record exact versions and test results here.

## Recipe viewer policy

The project will implement its own recipe viewer and recipe-query infrastructure. JEI must not be added as a dependency. The viewer consumes Reforged-owned recipe and category metadata; optional external recipe integrations, if ever needed, must remain isolated behind adapters.

## Historical research policy

Use GTNH 1.7.10 to understand gameplay semantics, recipes, material processing, and progression. Treat legacy code and APIs as non-portable until independently reviewed for licensing and modern compatibility.

## Verification record

NeoForge is selected, but exact NeoForge and Gradle versions are not pinned. No successful 26.3 build or launch is claimed.
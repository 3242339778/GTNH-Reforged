# Technical baseline

| Item | Baseline | Confidence |
|---|---|---|
| Minecraft | 1.7.10 | Project decision |
| GTNH pack | 2.9.0 Stable | Project decision; verify exact release artifacts |
| Upstream source commit | TBD | Not pinned yet |
| Java target | 21 proposed | Unverified against the selected legacy build chain |
| Forge / ForgeGradle | TBD | Must be identified from the pinned source |
| Gradle wrapper | TBD | Must be taken from the actual project baseline |

## Required verification before implementation
1. Resolve the exact GTNH 2.9.0 source revision and record its full commit SHA.
2. Inspect the actual Forge, ForgeGradle, Gradle, and compiler configuration.
3. Run a clean build using the intended JDK.
4. Launch a minimal development instance and record runtime behavior.
5. If Java 21 fails, document the failure and choose a compatible build/runtime strategy rather than silently changing the baseline.

Do not infer modern Forge APIs from the Minecraft version. This project targets a legacy 1.7.10 ecosystem.

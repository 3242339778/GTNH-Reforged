# Technical baseline

## Project target

| Item | Baseline | Confidence / status |
|---|---|---|
| Minecraft | Java Edition 26.3 | Explicit project decision; implementation not started |
| Historical GTNH baseline | GTNH on Minecraft 1.7.10 | Research reference only; not a runtime dependency |
| Java runtime | Java 25 or newer for Minecraft 26.3 | Target requirement; verify against official version metadata and launcher/runtime behavior |
| Java research range | Java 17–26 | Investigation range only; does not mean every JDK in this range can run MC 26.3 |
| Mod loader | Undecided | Requires current ecosystem comparison |
| Mappings / modding API | Undecided | Depends on loader choice |
| Build system | Undecided | Select after platform research |
| Project model | Independent modern re-architecture | Explicit project decision |

## Compatibility policy

Minecraft 26.3 is the primary target. Do not lower the target or introduce legacy compatibility constraints without an explicit project decision.

Java 17–26 is a range for investigating tooling, build compatibility, and possible future branches. It is **not** a claim that Minecraft 26.3 itself supports Java 17–24. The 26.3 runtime target is Java 25 or newer, subject to verification against authoritative version metadata.

If older Java support is desired later, treat it as a separate Minecraft target/branch decision rather than implying that one 26.3 build can run on every JDK from 17 through 26.

## Platform selection gates

Before implementation begins:

1. Verify Minecraft 26.3's Java requirement using official version metadata.
2. Compare mod loaders currently available for 26.3, including ecosystem coverage, update cadence, development tooling, and server support.
3. Identify the corresponding mappings, APIs, Gradle plugins, and supported JDK versions.
4. Create a minimal development project and record exact tool versions.
5. Produce a clean build and launch a minimal client and dedicated server.
6. Record the selected platform and all pinned versions in this document.

Do not choose a loader or build plugin based only on familiarity with the 1.7.10 ecosystem.

## Historical research policy

Use GTNH 1.7.10 and related projects to understand gameplay semantics, recipes, material processing, progression, and proven design patterns. Treat legacy code and APIs as non-portable until independently reviewed for licensing, architecture, and modern API compatibility.

## Verification record

No modern platform, loader, or build toolchain has been selected or verified yet. No successful 26.3 build or launch is claimed by this document.

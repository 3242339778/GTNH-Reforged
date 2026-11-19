# Project scope

## Target and direction

GTNH-Reforged is a clean modern re-architecture targeting Minecraft Java Edition 26.3. The project is not a direct port of the Minecraft 1.7.10 GTNH codebase.

Historical GTNH releases and related source repositories may be studied to understand gameplay, progression, materials, recipes, machines, balance, and technical lessons. They are research references only unless a specific component is independently reviewed and deliberately adopted.

## In scope

- Preserve and thoughtfully redesign the defining GTNH experience: demanding survival, long progression, complex industrial chains, material processing, and technology milestones.
- Build Reforged-owned systems for materials, recipes, machines, progression, and quests.
- Select a modern 26.3 loader and toolchain based on documented ecosystem research.
- Build narrow compatibility adapters for selected modern dependencies.
- Measure and improve performance through profiling and architecture.
- Validate changes incrementally with reproducible builds and tests.

## Out of scope for the initial phase

- Blindly porting every historical GTNH mod or API.
- Rewriting every third-party mod.
- Promising feature parity before each system is researched and implemented.
- Removing large groups of mods as a shortcut for performance.
- Claiming compatibility with a Java runtime or toolchain before a real build and launch test.
- Redistributing upstream binaries, code, or assets without checking their licenses.

## Quality goals

- Deterministic registration and stable namespaced IDs.
- Clear ownership boundaries between platform, compatibility adapters, and Reforged systems.
- Detectable recipe conflicts and missing progression links.
- Reliable client/server separation.
- Useful diagnostics, reproducible builds, and migration/research notes.
- Performance decisions backed by measurements.

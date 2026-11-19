# Project scope

## In scope
- Deeply refactor progression, materials, recipes, and quest integration while retaining the defining GTNH experience.
- Build Reforged-owned abstractions and compatibility adapters.
- Research upstream behavior and document exact source revisions.
- Validate changes incrementally with reproducible tests.

## Out of scope for the initial phase
- Rewriting every upstream mod.
- Removing large groups of mods as a shortcut for performance.
- Claiming compatibility with a Java runtime or toolchain before a real build and launch test.
- Redistributing upstream binaries or assets without checking their licenses.

## Quality goals
- Deterministic registration and stable IDs.
- Clear ownership boundaries between upstream and Reforged code.
- Detectable recipe conflicts and missing progression links.
- Useful diagnostics and migration notes.

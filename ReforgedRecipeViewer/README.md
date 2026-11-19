# ReforgedRecipeViewer

A first-party recipe discovery and browsing interface for GTNH-Reforged.

## Goals
- Fast, searchable access to every Reforged recipe category.
- Recipe details showing inputs, outputs, processing time, energy, fluids, and machine requirements.
- Reverse lookup from an ingredient to recipes that use or produce it.
- Navigation between related processing steps and industrial chains.
- Integration with progression and quests without coupling the viewer to those modules.
- Responsive behavior with a very large recipe database.

## Architecture
The viewer consumes the stable query API exposed by ReforgedRecipes. It does not own canonical recipe definitions and must not scan every recipe on each UI interaction.

Planned areas:
- Search and query presentation.
- Recipe category and detail screens.
- Ingredient and machine navigation.
- Client-side cache and explicit invalidation on data reload.
- Optional adapters for external recipe providers, isolated from the core.

## Dependency policy
JEI is explicitly excluded. EMI and REI are not selected as dependencies; the project will own its recipe-viewer experience.

No implementation is present yet. All APIs and UI designs are proposals until built and tested on NeoForge 26.3.
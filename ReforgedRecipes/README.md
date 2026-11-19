# ReforgedRecipes

Recipe declarations, validation, controlled generation, and machine integration for GTNH-Reforged.

## Responsibilities
- Define stable, namespaced recipe identifiers and categories.
- Represent item/fluid inputs and outputs, machine requirements, processing time, energy, and relevant conditions.
- Validate duplicate IDs, missing references, invalid quantities, and broken machine/category links.
- Expose a stable query API for the first-party ReforgedRecipeViewer.
- Support deterministic data reload and explicit cache invalidation.
- Keep external recipe-system integrations behind optional adapters.

## Dependency policy
The first-party ReforgedRecipeViewer is the intended recipe discovery UI. JEI is explicitly excluded and must not be added as a dependency. The recipe model and query API must not depend on any viewer implementation.

No implementation is present yet. API names and data structures remain proposals until implemented and tested against NeoForge 26.3.
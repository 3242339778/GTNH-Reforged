# ReforgedRecipes

Recipe declarations, validation, controlled generation, and machine integration.

## Current implementation
- Immutable recipe identity and recipe definition types.
- Material-form input/output quantities with positive-amount validation.
- Immutable recipe catalog with prebuilt indexes for input material, output material, and machine ID.
- Initial data-only example: iron dust → iron ingot in a basic electric furnace.
- Unit tests for reverse lookup, duplicate IDs, immutable results, and invalid quantities.

## Dependency policy
The recipe model is independent of the recipe viewer and Minecraft item classes. The first-party ReforgedRecipeViewer will consume its query API. JEI is explicitly excluded; EMI and REI are not core dependencies.

## Current limitation
The initial recipe refers to logical material forms and a machine identifier only. It does not yet register Minecraft items, a furnace block/entity, or an executable in-game recipe. Those platform adapters are the next integration step.
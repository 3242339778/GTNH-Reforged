# ReforgedMaterials

Material definitions, material forms, properties, and registration adapters.

## Current implementation
- Immutable `MaterialId`, `MaterialDefinition`, and `MaterialCatalog` domain types.
- Initial built-in iron material definition with dust, ingot, nugget, plate, and rod forms.
- Deterministic catalog ordering and duplicate-ID validation.
- Unit tests for lookup, form filtering, defensive immutability, and validation.

These are domain definitions only. They do **not** register Minecraft items, blocks, textures, or recipes yet. NeoForge registration adapters will be added after the bootstrap build is verified.

## Design boundary
The material domain model intentionally does not depend on Minecraft item classes. A later platform adapter will map logical material forms to registered game content while recipes continue to reference stable material identities.
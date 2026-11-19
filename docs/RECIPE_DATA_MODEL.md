# Recipe data model — initial design

**Status:** Architecture proposal. This document does not claim that these types or behaviors have been implemented. Concrete Java and NeoForge bindings are deferred until the 26.3 bootstrap passes CI.

## Goals
- One authoritative recipe definition shared by machines, progression checks, diagnostics, and the first-party viewer.
- Stable namespaced identity and deterministic data loading.
- Efficient reverse lookup by item, fluid, machine, and category.
- Clear separation between domain data and Minecraft/NeoForge presentation APIs.
- No dependency on JEI, EMI, or REI.

## Ownership boundaries
### ReforgedRecipes
Owns recipe identity, category identity, ingredient and output specifications, machine/process requirements, validation, recipe source loading, immutable snapshots, and the query contract.
### ReforgedRecipeViewer
Consumes read-only query results. Owns search UI, category browsing, recipe detail rendering, ingredient navigation, processing-chain navigation, and client presentation caches. It must not become the canonical recipe store.
### Platform integration
Adapts NeoForge registries, data loading/reload lifecycle, item/fluid identifiers, and client/server synchronization to the domain contracts. Minecraft classes should not leak through every recipe-model interface.

## Proposed domain concepts
| Concept | Responsibility |
|---|---|
| RecipeId | Stable namespace + path identity |
| RecipeCategoryId | Stable category identity, e.g. maceration or chemical processing |
| RecipeDefinition | Immutable complete recipe declaration |
| ItemIngredientSpec | One or more accepted item IDs/tags and required count |
| FluidIngredientSpec | One or more accepted fluid IDs/tags and required amount |
| ItemOutputSpec | Item result, count, and optional probability |
| FluidOutputSpec | Fluid result and amount |
| MachineRequirement | Required machine family/tier/capabilities |
| ProcessProperties | Duration, energy-per-tick, and optional process metadata |
| RecipeCondition | Optional progression/world/environment predicates |
| RecipeSnapshot | Immutable, validated view of all active recipes |
| RecipeQueryService | Read-only indexed lookup over a snapshot |
These are conceptual names, not committed public Java API names.

## Data semantics
- Item and fluid amounts are positive integers. Item amounts are stack counts; fluid amounts use millibuckets as the initial domain unit unless a later project-wide unit decision supersedes it.
- Process duration is expressed in game ticks.
- Energy is represented as a positive integral energy-per-tick value. The domain should not assume a particular machine voltage tier is sufficient to describe every future machine; tier and machine capability remain separate requirements.
- Ingredient alternatives are explicit. A recipe can accept any one of several item/fluid alternatives without expanding into duplicate recipe IDs.
- Outputs support guaranteed results and optional chance results. Chance representation must use deterministic integer precision (for example, basis points) rather than floating-point equality.
- Optional conditions are structured data, not arbitrary executable code embedded in datapack files.
- Recipe identity is independent of localized display names and textures.

## Validation
Reject or report, with the owning recipe ID:
- Duplicate recipe IDs.
- Missing or invalid category IDs.
- Empty ingredient groups or output sets where the recipe type requires them.
- Non-positive counts, fluid amounts, duration, or energy values.
- Unknown item/fluid/tag references after the relevant registries are available.
- Machine requirements that refer to unknown machine families or impossible capability combinations.
- Invalid chance values or malformed condition data.
Validation should collect actionable diagnostics where safe, rather than fail on the first unrelated error. Invalid recipes must not enter the active query snapshot.

## Query API shape
The first API should expose intent-oriented methods, not the underlying mutable map:
- Get one recipe by RecipeId.
- List recipes by category.
- Find recipes that accept an item or fluid.
- Find recipes that produce an item or fluid.
- Find recipes compatible with a machine capability profile.
- Filter by progression stage or visibility policy.
- Search by normalized text tokens across recipe/category/machine metadata.
Queries return immutable results tied to a snapshot/version. Pagination or bounded result windows should be supported for broad searches.

## Indexing and reload behavior
Build indexes once when a new recipe snapshot is loaded:
- Recipe ID → recipe.
- Category ID → recipe IDs.
- Item ingredient key → recipe IDs.
- Fluid ingredient key → recipe IDs.
- Item output key → recipe IDs.
- Fluid output key → recipe IDs.
- Machine family/capability key → recipe IDs.
- Progression key → recipe IDs.
Tag-based ingredients need a reload-aware expansion/index strategy. Avoid resolving tags repeatedly for every keystroke or viewer frame.
Reload should be atomic from the query consumer's perspective:
1. Parse candidate recipe sources.
2. Resolve references and validate definitions.
3. Build all indexes off to the side.
4. Publish one new immutable snapshot only if the candidate is accepted.
5. Notify subscribers with the new snapshot version so UI caches can invalidate.
Readers should never observe a half-built mixture of old and new indexes.

## Performance expectations
- Opening a category or recipe detail must not scan the full recipe collection.
- Ingredient reverse lookup should use indexes.
- Text search should operate on pre-normalized searchable metadata and be debounced at the UI boundary.
- Large results should be paged/virtualized in the viewer.
- Benchmarks should use synthetic large datasets as well as realistic project recipes.
- Measure load time, index memory, query latency, and UI frame cost separately.
No numeric performance target is set until a baseline dataset and profiling environment exist.

## Open decisions
- Exact Java type and serialization format for identifiers.
- How recipes are divided between datapack-defined and code-defined sources.
- Whether machine capability matching is exact, minimum-tier, or predicate-based per machine family.
- How progression-gated recipes are filtered in single-player and multiplayer.
- How recipe data is synchronized to clients without exposing server-only information.
- Final energy unit and compatibility rules for non-EU systems.
These decisions should be resolved with the first real material → recipe → machine vertical slice, not prematurely hard-coded into the viewer.
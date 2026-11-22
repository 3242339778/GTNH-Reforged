# Legacy Behavior Compatibility Audit — Minecraft 26.3

Status: research specification, not an implementation claim.

## 1. Compatibility policy

The historical GTNH 1.7.10 code is the behavioral and content reference. An absent old API is not grounds to discard a feature. Classify each behavior as direct reuse, API facade, object bridge, hook reinstallation, runtime reconstruction, or unverified. Unverified means continue research, not impossible. A successful compile or launch is not proof of behavioral equivalence.

## 2. NeoForge 26.3 mechanisms verified in source

### 2.1 Mixin and bytecode transformation still exist

NeoForge 26.3.x contains its own Mixin sources and mixin config. Its coremods subproject contains NeoForgeCoreMod, MethodRedirector, ReplaceFieldWithGetterAccess, and ReplaceFieldComparisonWithInstanceOf. NeoForgeCoreMod implements ClassProcessorProvider and registers processors through createProcessors(Context, Collector). The old IFMLLoadingPlugin / IClassTransformer entry point is not the modern contract, but class transformation remains part of the platform.

Sources:
- https://github.com/neoforged/NeoForge/tree/26.3.x/coremods
- https://github.com/neoforged/NeoForge/blob/26.3.x/coremods/src/main/java/net/neoforged/neoforge/coremods/NeoForgeCoreMod.java
- https://github.com/neoforged/NeoForge/tree/26.3.x/src/main/java/net/neoforged/neoforge/mixins

### 2.2 Modern rendering is staged and submission-based

Source-verified NeoForge APIs:
- ExtractLevelRenderStateEvent: extract custom state into LevelRenderState.
- SubmitCustomGeometryEvent: submit custom geometry through SubmitNodeCollector.
- RenderLevelStageEvent: stage-specific rendering hooks. Most stages provide an active RenderPass that must be reused.
- AddSectionGeometryEvent: static chunk-section geometry; its callback may run on a worker thread, so capture immutable/thread-safe data on the main thread.
- RegisterRenderPipelinesEvent: register custom pipelines.
- RegisterColorHandlersEvent.BlockTintSources: register block tint sources.

Sources:
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/ExtractLevelRenderStateEvent.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/SubmitCustomGeometryEvent.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/RenderLevelStageEvent.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/AddSectionGeometryEvent.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/RegisterRenderPipelinesEvent.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/client/java/net/neoforged/neoforge/client/event/RegisterColorHandlersEvent.java

Implication: old immediate-mode GL/Tessellator calls cannot simply be forwarded. Preserve geometry, animation, color, and state calculations in a renderer-neutral representation, then submit through modern render-state and node-collector APIs. Use chunk-section geometry only for static geometry; dynamic lasers and work-area overlays need per-frame state/submission. Validate transparency, depth, culling, and camera-relative transforms.

### 2.3 Recipe API limits

NeoForge 26.3.x has RecipePrioritiesProvider and RecipePriorityManager for layered recipe priority overrides. It also has RecipeOutput.withConditions / withContext for datagen conditions and RegisterRecipePropertiesEvent for recipe property sets.

Important limitation: recipe priority changes which matching recipe wins; it does not remove recipes. Datagen conditions govern generated recipes; they do not retroactively intercept arbitrary third-party runtime registration. These APIs alone are not a general-purpose late recipe deletion event.

Sources:
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/common/crafting/RecipePriorityManager.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/common/data/RecipePrioritiesProvider.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/common/extensions/IRecipeOutputExtension.java
- https://github.com/neoforged/NeoForge/blob/26.3.x/src/main/java/net/neoforged/neoforge/event/RegisterRecipePropertiesEvent.java

For historical GTNH recipe removals, investigate: target-mod config/datagen control; exact recipe-ID override; reload/manager filtering hook; then narrow Mixin/class processor if required. Add tests proving unwanted recipes are absent and intended replacements present. Do not call recipe priorities a recipe-nuking API.

## 3. GT5-Unofficial Mixin inventory

Current upstream tree observation: 53 Java files under src/mixin/java, 32 Early and 21 Late. The registry contains functional groups; one group can contain multiple classes. These are source-tree counts, not a completed semantic audit of all 53 classes.

| Area / group | Behavior to preserve | Candidate 26.3 route |
|---|---|---|
| World access tracking | Record relevant block reads/positions and exact filters | narrow world hook or Mixin |
| LanguageRegistry merge | locale selection, inflection JSON, language table reload | resource reload/localization service |
| Vanilla accessors | private text scroll, tessellator state, recipe inputs/output, fire immunity, chunk-cache world | public API first; accessor Mixin only if needed |
| Item cover interaction | sneak bypass for GT cover items | interaction event or Mixin |
| Tool mining hooks | GT tool speed/mining calculation | tool/block interaction hook or Mixin |
| Pipe bounds | separate render, collision, and selection bounds | modern shape/render split |
| Worldgen sorting | preserve generator ordering | worldgen ordering hook |
| Portal spawn protection | preserve portal spawn conditions and protection checks | spawn/portal hook or Mixin |
| Pollution | preserve amount, source conditions, and affected targets | shared Pollution API and adapters |
| Sound behavior | positional, looping, volume, playback-state behavior | sound events and sound adapter |
| IC2 wrench/harvest/drop | wrenching, harvest tool, damage and drops | isolated IC2 adapter |
| IC2 hazard | hazmat equipment protection checks | equipment protection adapter |
| Optional-mod Late Mixins | behavior only when target mod is present | conditional integration modules |

Accessor lesson: GT5U TessellatorMixin exposes isDrawing and whether vertexCount is positive; GuiTextFieldMixin exposes lineScrollOffset; VanillaShapedRecipeMixin reads/mutates recipe output and reads inputs; EntityMixin sets fire immunity; ChunkCacheMixin exposes its world. Search all call sites and implement the narrow semantic need. Do not emulate the entire old class, but do not omit mutating behavior.

Each of the 53 classes needs a record of target class/method/field, injection point and descriptor, side/mod/version conditions, state read or mutation, call sites, candidate event/API, fallback hook, and behavior regression test.

## 4. NewHorizonsCoreMod ASM behavior

### AdvancedSolarPanelTransformer
Targets advsolar.common.AdvancedSolarPanel. Replaces afterModsLoaded with manager wiring from MTRecipeManager.instance to MTAPI.manager. In preInit it removes instruction regions associated with four IC2 compressor addRecipe calls. Before migrating, inspect the exact target mod version and identify all four recipe inputs/outputs. Prefer supported configuration/data controls; otherwise use a narrow hook. Test all four removals and replacement behavior.

### BibliocraftTransformer
Targets BlockLoader, ItemLoader, and three WoodsLoader classes for Forestry, Natura, and Biomes O' Plenty. It empties no-argument addRecipies or initRecipes methods. Enumerate every recipe and non-recipe side effect suppressed by those methods in the exact target versions. Replace method-wide suppression with exact recipe IDs/predicates when possible.

### GraviSuiteTransformer
Targets gravisuite.GraviSuite. It finds the first NEW ItemStack in afterModsLoaded and deletes that instruction and the remainder of the method, then returns. This is broad truncation, not proof that exactly one recipe is removed. Inspect exact source/bytecode and enumerate every skipped side effect before rebuilding the behavior narrowly.

Sources:
- https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/master/src/main/java/com/dreammaster/coremod/transformers/recipenukers/AdvancedSolarPanelTransformer.java
- https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/master/src/main/java/com/dreammaster/coremod/transformers/recipenukers/BibliocraftTransformer.java
- https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/master/src/main/java/com/dreammaster/coremod/transformers/recipenukers/GraviSuiteTransformer.java
- https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/master/src/main/java/com/dreammaster/coremod/DreamClassTransformer.java

## 5. Renderer migration mapping

| Historical behavior | Modern route | Constraints |
|---|---|---|
| Static chunk geometry | AddSectionGeometryEvent | callback may be off-thread; capture safe data on main thread |
| Dynamic laser | ExtractLevelRenderStateEvent + SubmitCustomGeometryEvent | extract animation state then submit every frame |
| Work-area overlay | state extraction + custom geometry or suitable render stage | preserve depth, alpha, camera-relative transforms and visibility |
| Pollution block tint | RegisterColorHandlersEvent.BlockTintSources | preserve world/biome/position/state inputs |
| Custom blending/pipeline | RegisterRenderPipelinesEvent | define pipeline state and test classic transparency/OIT |
| GUI overlay | GUI layer/screen events | keep GUI and world rendering separate |

First renderer prototype: a small line/box overlay with camera-relative coordinates and depth testing. Only then migrate laser and work-area renderers.

## 6. Third-party compatibility research order

| Dependency family | Historical role to investigate | Compatibility task |
|---|---|---|
| IC2 Experimental | energy, machines, fluids, hazmat, recipes | high-priority semantic adapter; determine hard dependency surface |
| GTNHLib | shared utilities and mixin helpers | separate pure Java utilities from MC-bound APIs |
| StructureLib | multiblock definitions and checks | preserve matching semantics behind adapter |
| ModularUI / ModularUI2 | machine UI and widgets | UI model/input adapter |
| NotEnoughItems | historical item/recipe viewer integration | do not add as dependency; extract data semantics for first-party viewer |
| AE2 NH | storage, channels, crafting | optional integration boundary |
| Baubles-Expanded | accessory slots/equipment | equipment capability/slot adapter |
| Waila | contextual overlays | optional information provider adapter |
| ChunkAPI / EndlessIDs | extended chunk and identifier limits | inspect storage/world-format assumptions |
| Postea / Draconic Evolution | optional integration | conditional adapters after exact source/version audit |

This is a priority list, not a claim that modern ports exist. Resolve exact dependency versions from the selected GTNH 2.9.0 release and inspect matching source tags/commits. Gradle api, implementation, compileOnly, and runtimeOnly do not by themselves establish player-side hard dependencies.

## 7. Immediate implementation sequence

1. Create a machine-readable ledger for all 53 GT5U Mixin classes.
2. Enumerate all side effects suppressed by the three CoreMod transformers using their exact target mod versions.
3. Prototype modern class processing only for behavior that cannot use an event/API.
4. Prototype one dynamic renderer through state extraction and geometry submission.
5. Define a narrow IC2-like energy/fluid/hazard adapter without binding the core domain model to third-party types.
6. Add regression tests comparing state transitions and outputs to historical behavior.

## 8. Existing project-document alignment issue

Current docs/ARCHITECTURE.md and docs/ROADMAP.md describe 1.7.10 as research-only and blind source porting as a non-goal. That wording is narrower than the established requirement to preserve original GTNH source and behavior wherever technically possible. Update the policy to distinguish source/behavior preservation from blindly compiling the entire old codebase or embedding the old runtime unchanged. Licensing and provenance review remains mandatory for every reused or adapted source file.


# GTNH 2.9.1 Upstream Source Delta: GT5-Unofficial and NewHorizonsCoreMod

**Audit date:** 2026-11-22  
**Pack baseline:** GTNH 2.9.1  
**Compared source tags:** GT5-Unofficial `5.09.54.265..5.09.54.275`; NewHorizonsCoreMod `2.9.99..2.9.105`

This document records a first-pass GitHub tag comparison and selected source-file inspections. It does not claim a line-by-line semantic audit of every changed file.

## 1. GT5-Unofficial comparison

GitHub compare reports:

- 100 commits between the selected tags.
- 300 changed files.
- 8,358 additions and 4,089 deletions.

This is a substantial source delta, not merely a patch-number adjustment.

### 1.1 High-impact areas observed

**Recipe system**
- `gregtech/api/recipe/RecipeMaps.java` adds specialized Centrifuge and Large Centrifuge frontends and a Void Miner recipe map/frontend.
- Dynamite recipe input changes from an IC2 item lookup to `ItemList.Dynamite`.
- `gregtech/loaders/postload/recipes/AssemblyLineRecipes.java` has substantial changes (+388/-234 in GitHub file statistics), including new/changed compressed bus recipe definitions.
- Several other recipe loaders changed, including fuel, compressor, chemical bath, distillery, fermenter, pyrolysis, beam crafter, and Large Hadron Collider recipes.

**Machine and network behavior**
- `BaseMetaPipeEntity.java` grew from 1,193 to 1,439 lines. New state includes identity-based managed cable tracking, deferred connection checks, occupied-side tracking, and cable-specific graph integration.
- `GenerateNodeMap.java` changed graph invalidation and traversal behavior, including invalidating nodes, clearing paths, tracking occupied sides, and handling loop-closing paths.
- `BaseMetaTileEntity.java`, `MTECable.java`, and power graph node/path classes also changed substantially.
- These are gameplay-critical network/topology changes. A port must preserve graph rebuild/invalidation semantics, not merely reproduce cable rendering or connectivity flags.

**NEI**
- `GTNEIDefaultHandler.java` adds scroll-area drawing, drag polling, and mouse-click handling.
- `NEIScrollArea.java` is added (174 lines).
- This is a concrete extension to GTNH's recipe-viewer behavior. Preserve it in the custom viewer design; do not replace it with JEI.

**GTMod lifecycle and dependencies**
- `GTMod.java` changes the minimum GTNHLib requirement to 0.11.58, adds required-after EndlessIDs and Postea declarations, adds an Et Futurum ordering dependency, and conditionally registers Extra Bees mutator items.
- The constructor's pipe entity registration changes to a constructor reference; do not interpret this syntax-only change as a behavior change without further evidence.
- `dependencies.gradle` changes published compile dependencies and versions. It is a build dependency declaration, not by itself a player-facing required-mod list.

**Other notable changes**
- Added power-grid structure JSON/NBT resources.
- Added a large power-grid topology test source.
- Several GT++ storage classes and Kubatech EIG mode classes were removed/replaced; investigate save compatibility and replacement behavior before migration.
- Various machine, GUI, material, and language files changed.

### 1.2 Mixin finding

The compare result contained no changed files under the `src/mixin/` path. This means the selected tag diff did not report modifications to that source directory; it does **not** prove all Mixin behavior is unchanged. Check the complete registry, dependencies, and any non-Mixin hook/transformer changes separately.

## 2. NewHorizonsCoreMod comparison

GitHub compare reports:

- 22 commits between `2.9.99` and `2.9.105`.
- 47 changed files.
- 326 additions and 394 deletions.

### 2.1 Script changes

27 script classes changed, including:
- Applied Energistics 2
- GregTech
- EnderIO
- Forestry
- Galacticraft and GalaxySpace
- OpenComputers
- Thaumcraft
- Natura
- StevesCarts
- EFR
- Avaritia
- Witchery

This confirms the pack's integration and recipe behavior changed in 2.9.1. The old 2.9.0 script inventory cannot be treated as the final migration specification.

### 2.2 Recipe and integration changes

Changed recipe files include assembler, beam crafting, brewing, centrifuge, chemical bath/reactor, circuit assembler, compressor, DTPF, distillation, macerator, and mixer recipes.

Other changed areas include:
- `NEIGTNewHorizonsConfig`
- `IC2Converter`
- `HazardousItemsHandler`
- `RecipeRemover`
- dependency declarations

### 2.3 Coremod/Mixin finding

The selected compare result did not report changes under paths matching `coremod`, `transformer`, or `mixin`. This is a path-based observation, not proof that bytecode behavior is unchanged indirectly. The 27 script changes and recipe-removal code still need semantic review.

## 3. Migration decisions

1. Use the exact 2.9.1-selected source versions for migration, not current upstream default branches.
2. Prioritize GT5U cable/power graph behavior, recipe maps/loaders, machine state, NEI scroll-area behavior, and lifecycle dependencies.
3. Re-audit all 27 changed CoreMod scripts; retain their conditions, outputs, and removals as data/behavior rather than reducing them to a hand-written recipe subset.
4. Do not port old Mixin source mechanically. First determine whether a 2.9.1 behavior change exists and identify the modern hook or bridge.
5. Treat removed machine/storage classes as save-compatibility questions, not automatic permission to delete content.
6. Keep NEI as the legacy behavior reference and retain the project decision against JEI.

## 4. Evidence and limits

- **Confirmed:** GitHub compare metadata and changed-file paths/statistics for both repositories and tag pairs.
- **Inspected:** selected GT5U source files `GTMod.java`, `RecipeMaps.java`, `AssemblyLineRecipes.java`, `BaseMetaPipeEntity.java`, `GenerateNodeMap.java`, and `GTNEIDefaultHandler.java` at both tags.
- **Inspected:** NewHorizonsCoreMod changed-file categories and paths.
- **Not completed:** semantic review of all 300 GT5U files or all 47 CoreMod files.
- **Not completed:** runtime comparison of the two mod versions.
- **Not claimed:** that the 2.9.1 source changes are already compatible with Minecraft 26.3.

## 5. Next work

1. Diff GT5U cable graph implementation and add behavior invariants for connection changes, loops, endpoints, and graph invalidation.
2. Extract exact recipe changes from the 2.9.1 source and CoreMod scripts into an auditable recipe delta inventory.
3. Inspect removed storage/machine classes and determine item/block ID and world-save migration implications.
4. Review NEI scroll-area behavior in detail as requirements for ReforgedRecipes/ReforgedRecipeViewer.
5. Continue the 2.9.1 dependency/API compatibility matrix.

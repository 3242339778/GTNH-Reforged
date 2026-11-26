# GTNH 2.9.1 Baseline and 2.9.0 Delta Audit

**Audit date:** 2026-11-22  
**Target pack baseline:** GTNH 2.9.1  
**Historical comparison:** GTNH 2.9.0  
**Minecraft baseline:** 1.7.10  
**Purpose:** Freeze the content/configuration baseline for GTNH-Reforged migration research. This is a source audit, not a runtime compatibility claim.

## 1. Version policy

- Use the official GTNH Modpack tag `2.9.1` as the current migration content baseline.
- Keep `2.9.0` only as a historical comparison point.
- Treat nightly builds as observation-only; do not silently import their changes into the 2.9.1 baseline.
- Revalidate every dependency, config, quest, script, and recipe claim against 2.9.1 before implementation.
- A new release does not imply that its behavior has been sufficiently exercised in long-term play.

Sources:
- [GTNH Modpack 2.9.1 README](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.9.1/README.md)
- [2.9.0 to 2.9.1 compare](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/compare/2.9.0...2.9.1)
- [2.9.1 release](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/releases/tag/2.9.1)

## 2. Git comparison summary

The GitHub compare endpoint reports 2.9.1 as 30 commits ahead of 2.9.0, with 89 changed files in the returned comparison:

| Area | Changed files | Notes |
|---|---:|---|
| BetterQuesting quest data | 72 | Quest JSON additions and edits |
| Other config | 14 | Includes a new RWG2 config and NEI hidden-item changes |
| Release workflow | 1 | Release tag workflow changed |
| README / server list | 2 | Modlist and server metadata may change |

Aggregate diff reported by GitHub: 1,064 additions and 671 deletions.

Important interpretation:
- 72 changed quest files does **not** mean 72 new quests. Most are edits; two files are additions.
- The 14 config files are not all cosmetic. They include recipe overrides, block limits, world generation, microblocks, and LWJGL3ify settings.
- The comparison is a repository diff, not a complete semantic analysis of every changed JSON/config value.

## 3. Confirmed Modlist version changes

Versions below are read from the README at each exact tag, not inferred from current upstream repository heads.

| Component | 2.9.0 | 2.9.1 | Migration impact |
|---|---|---|---|
| GT5-Unofficial | 5.09.54.265 | 5.09.54.275 | Re-audit source behavior and APIs |
| GTNHLib | 0.11.54 | 0.11.58 | Recheck API and dependency baseline |
| NewHorizonsCoreMod | 2.9.99 | 2.9.105 | Re-audit scripts, mixins, and coremod behavior |
| NotEnoughItems | 2.8.171-GTNH | 2.8.175-GTNH | Re-audit NEI behavior; do not replace with JEI |
| ModularUI2 | 2.3.94-1.7.10 | 2.3.96-1.7.10 | Recheck GUI API assumptions |
| Postea | 1.2.7 | 1.2.9 | Recheck compatibility dependency |
| lwjgl3ify | 3.0.42 | 3.1.0 | Recheck SDL/LWJGL compatibility behavior |
| Applied Energistics 2 Unofficial | rv3-beta-1095-GTNH | rv3-beta-1104-GTNH | Recheck integration APIs |

The following sampled versions did not change in the README comparison:

| Component | 2.9.0 and 2.9.1 |
|---|---|
| StructureLib | 1.4.47 |
| ModularUI | 1.3.4 |
| IC2 | 2.2.2.828 |
| EndlessIDs | 1.7.4 |

This table is a focused sample of migration-critical dependencies, not a complete Modlist diff. The complete README Modlist at tag 2.9.1 remains authoritative.

## 4. Migration consequences

### Must be refreshed against 2.9.1

1. GT5-Unofficial source audit, especially Mixin registry, core plugin, recipe registration, and lifecycle.
2. NewHorizonsCoreMod scripts, transformers, mixins, and initialization behavior.
3. GTNHLib public API usage and version-specific implementation assumptions.
4. NEI integration and hidden-item configuration, while retaining the project decision not to use JEI.
5. ModularUI2 GUI behavior and Postea integration.
6. LWJGL3ify 3.1.0 SDL/window/input assumptions.
7. AE2 integration and recipe behavior.

### Data/configuration areas requiring semantic diff

- `config/NEI/hiddenitems.cfg`
- `config/gendustry/overrides/recipes.cfg`
- `config/rwg2.cfg`
- `config/lwjgl3ify.cfg`
- `config/microblocks.cfg`
- `config/gtnhintergalactic.cfg`
- `config/betterquesting/DefaultQuests/**`

The two added quest files are:
- `config/betterquesting/DefaultQuests/Quests/NoQuestLine/TriggerTheRootso-89FPlXAUS1iUHJlEg0iK7Q==.json`
- `config/betterquesting/DefaultQuests/Quests/NoQuestLine/TriggerTwilightP-yNfD7gXVSraw9bWbtfPzIw==.json`

Their names suggest trigger-related quest content, but their actual semantics must be read from the JSON before migration.

## 5. Evidence status

- **Confirmed:** exact tag-to-tag commit count and changed-file list from GitHub compare.
- **Confirmed:** selected dependency version strings from README files at tags 2.9.0 and 2.9.1.
- **Not yet completed:** semantic inspection of all 72 quest changes and all 14 config changes.
- **Not yet completed:** matching each Modlist version bump to the exact upstream source tag/commit and checking API/behavior changes.
- **Not claimed:** successful runtime execution of the full 2.9.1 pack or compatibility with Minecraft 26.3.

## 6. Next audit sequence

1. Inspect 2.9.1 README Modlist in full and extract every dependency version into a machine-readable inventory.
2. Compare exact upstream tags/commits for GT5-Unofficial, NewHorizonsCoreMod, GTNHLib, NEI, ModularUI2, Postea, lwjgl3ify, and AE2.
3. Read semantic diffs for recipe/config changes before porting them.
4. Read the two added quest JSON files and compare changed quest requirements/rewards.
5. Update the Mixin/Coremod behavior matrix using the exact versions selected by 2.9.1.

---
This document supersedes any use of 2.9.0 as the current GTNH content baseline. Historical findings from 2.9.0 remain provisional until checked against 2.9.1.


## 7. 2.9.2 Release Delta (Candidate Baseline, 2026-11-26)

GTNH 2.9.2 was published on 2026-11-25. Keep the 2.9.1 audit intact as a historical baseline; 2.9.2 is now the candidate current baseline pending semantic audit.

Sources:
- [GTNH Modpack 2.9.2 README](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/2.9.2/README.md)
- [2.9.1 to 2.9.2 compare](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/compare/2.9.1...2.9.2)
- [2.9.2 release](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/releases/tag/2.9.2)

### 7.1 Pack repository comparison

The compare reports 8 commits and the returned file list covers README, quest data, configuration, language templates, textures, and vending-machine trade data. Notable changes include removal of several LESU-related quest files, changes to glass-upgrade and endgame quest JSON, additions of IC2 cell textures, and 42 additions to the vending-machine trade database. This is a repository-level delta only, not a full semantic assessment.

### 7.2 Selected dependency changes

| Component | 2.9.1 | 2.9.2 |
|---|---|---|
| GT5-Unofficial | 5.09.54.275 | 5.09.54.277 |
| GTNHLib | 0.11.58 | 0.11.58 |
| NewHorizonsCoreMod | 2.9.105 | 2.9.106 |
| NotEnoughItems | 2.8.175-GTNH | 2.8.176-GTNH |
| ModularUI2 | 2.3.96-1.7.10 | 2.3.97-1.7.10 |
| VendingMachine | 0.4.104 | 0.4.105 |
| GTNHExtLib | 1.0.6 | 1.0.6 |

This is a focused selection, not a complete dependency inventory.

### 7.3 GT5-Unofficial 5.09.54.275 to 5.09.54.277

The upstream comparison reports 15 commits and 300 changed files. No direct changes to the previously audited core graph source files (`BaseMetaPipeEntity`, `MetaPipeEntity`, `MTECable`, `GenerateNodeMap`, `Node`, `NodePath`, `PowerNodePath`, `PowerNodes`) were present in the returned changed-file list. This is a preliminary path-level observation, not proof of unchanged runtime behavior across the full dependency set.

The comparison does contain other energy-adjacent and machine changes, including boiler and advanced boiler logic, plus changes in GTNH Intergalactic, TecTech, and other modules. These are outside the current cable graph core audit and should be tracked for their respective future migration work.

### 7.4 Upstream deltas requiring follow-up

- NewHorizonsCoreMod 2.9.105 to 2.9.106: 3 commits; ScriptChisel, ScriptThaumicEnergistics, TCHelper, and related cell textures changed.
- NEI 2.8.175-GTNH to 2.8.176-GTNH: 3 commits; bookmark panel/tree toolbar, drawable resources, recipe GUI/catalyst and scrollbar behavior, plus UI texture/theme additions. Preserve these behaviors in the custom viewer audit where applicable.
- ModularUI2 2.3.96 to 2.3.97: compare reports divergence as well as commits; changes include ClientProxy, LocatedWidget viewport behavior, TextFieldHandler, and test/workflow changes. Verify exact ancestry and semantics before assuming a simple tag upgrade.
- VendingMachine 0.4.104 to 0.4.105: one commit affecting configuration, vending GUI behavior, and localization; inspect before porting.

### 7.5 Status

- Confirmed: README versions at tags 2.9.1 and 2.9.2 for the selected dependencies.
- Confirmed: pack repository compare summary and selected upstream compare metadata/file lists.
- Preliminary: no direct filename-level change detected in the previously audited GT5U core power-graph files for .275 to .277.
- Not yet completed: semantic review of all changed GT5U files, quest/config/transaction changes, and the upstream changes listed above.
- Policy: retain 2.9.1 as a historical baseline; use 2.9.2 as candidate current baseline after remaining audits. Do not silently replace prior source evidence or claim full 2.9.2 behavioral verification.

---

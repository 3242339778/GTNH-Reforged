# Architecture overview

## Intended layers

1. **Upstream layer** — GTNH, GT5-Unofficial, GTNHLib, and other selected dependencies.
2. **Compatibility layer** — narrow adapters for upstream materials, items, fluids, recipes, and lifecycle hooks.
3. **Reforged Core** — stable IDs, registries, configuration, validation, and shared contracts.
4. **Feature modules** — materials, recipes, progression, quests, and original content.

## Design rules
- Do not modify upstream source as the default integration strategy.
- Keep upstream-specific types behind adapters where practical.
- Make registration order and lifecycle requirements explicit.
- Fail early with actionable diagnostics for duplicate IDs or invalid definitions.
- Avoid hidden global mutation and accidental changes to upstream recipe objects.

## First vertical slice
Register one Reforged-owned material profile, expose only explicitly selected forms, connect those forms to a small processing chain, and verify the resulting items and recipes in a real GTNH development instance.

All API names in early design notes are proposals until checked against the pinned source revision.

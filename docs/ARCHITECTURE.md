# Architecture overview

## Target

The project targets Minecraft Java Edition 26.3 and will use a clean modern architecture. The historical GTNH 1.7.10 implementation is a source of gameplay and design research, not a dependency layer to be carried forward.

The exact loader, mappings, APIs, and build system remain undecided until the platform research phase is complete.

## Intended layers

1. **Platform integration** — the selected 26.3 loader, lifecycle hooks, registries, networking, data systems, and client/server boundaries.
2. **Compatibility adapters** — narrow integrations for explicitly selected third-party mods and APIs.
3. **Reforged Core** — stable namespaced identifiers, lifecycle coordination, configuration, diagnostics, validation, and shared contracts.
4. **Domain systems** — materials, recipes, machines, progression, quests, world content, and original gameplay.
5. **Presentation layer** — user interfaces, tooltips, recipe discovery, and player-facing progression guidance.

## Design rules

- Do not make the architecture depend on a specific loader until the platform decision is documented.
- Keep loader-specific and third-party types behind narrow adapters where practical.
- Make registration, data loading, and lifecycle requirements explicit.
- Separate common/server logic from client-only presentation code.
- Use stable namespaced identifiers and deterministic data definitions.
- Fail early with actionable diagnostics for duplicate IDs, invalid recipes, and broken progression links.
- Avoid hidden global mutation and order-dependent registration.
- Design performance-sensitive systems to be measurable and profile-driven.
- Do not copy legacy APIs or assume 1.7.10 code can compile in the modern target.

## First vertical slice

After platform selection, build a minimal 26.3 development mod that:

1. Loads on the selected client and dedicated server.
2. Registers one Reforged-owned material and a small set of forms.
3. Defines a simple processing chain using the chosen modern data/recipe approach.
4. Exposes clear diagnostics and automated validation.
5. Can be built reproducibly from a clean checkout.

All API names in early design notes are proposals until checked against the selected 26.3 toolchain.

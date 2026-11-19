# Minecraft 26.3 platform research

> **Status:** Preliminary research, checked 2026-11-19. This document records evidence and a provisional direction; it does not claim that either loader has been tested in this repository.

## Executive summary

Both Fabric and NeoForge have active Minecraft 26.3 toolchains. Neither has a decisive advantage from general mod-count readiness alone. For a GTNH-scale technology and progression project, the loader decision must be driven by the specific industrial, storage, recipe-viewer, world-generation, and server-side dependencies we actually intend to use.

**Provisional direction: keep NeoForge as the leading candidate for the first technical prototype, but do not lock the project to it yet.** Fabric remains a serious alternative. The final decision is gated on a dependency shortlist and a minimal client/server prototype.

## Current platform snapshot

| Area | Fabric | NeoForge |
|---|---|---|
| 26.3 development support | Documented | Documented |
| Java | Fabric's current 26.3 toolchain should be checked against the selected Loom/template; Minecraft 26.3 runtime target is Java 25+ | Official getting-started docs require JDK 25 |
| Current toolchain evidence | Fabric Develop lists Loader 0.20.1, Loom 1.19-SNAPSHOT, Fabric API 0.166.0+26.3 | NeoForge project listing shows 26.3.0.103 |
| Build tooling | Fabric Loom + Gradle | ModDevGradle or NeoGradle + Gradle |
| Mod ecosystem readiness | Broad general-purpose and performance ecosystem | Broad content/technology ecosystem; many established Forge-family projects target NeoForge |
| Architecture character | Lightweight, modular toolchain; extensive direct use of Fabric APIs and mixins | More batteries-included modding API and event-driven integration model |
| Main risk | Some large content/technology projects may not target Fabric at the newest game version | Some cross-loader and client performance projects may be Fabric-first; individual compatibility still needs checking |

The Fabric blog's initial 26.3 release guidance named Loom 1.17 and Gradle 9.6, while the live Fabric Develop page now lists newer recommended values. Treat the live version selector/template as authoritative when creating the prototype, not stale copied version numbers.

## Ecosystem readiness

A Modrinth-based tracker checked on 2026-11-18 reports that, among its top 300 projects per loader, 66.3% of Fabric projects and 60.3% of NeoForge projects have a 26.3 release. This is a broad popularity sample, not a measure of technology-mod completeness or stability.

Important examples for our project:

- **Create:** no 26.3 release is listed in the checked Modrinth metadata. Do not make our industrial architecture depend on Create.
- **EMI:** no 26.3 release is listed for Fabric or NeoForge in the checked metadata. The user has previously preferred EMI's responsiveness over JEI, so we should keep the recipe-viewer interface abstract and benchmark available options later.
- **JEI:** a 26.3 release is listed for both Fabric and NeoForge. Its availability does not mean it is automatically our choice.
- **Technology mods:** some projects have partial, alpha, beta, unofficial, or loader-specific ports. These must be evaluated individually; never treat an unofficial port as a stable dependency by default.

Missing a Modrinth listing does not prove that a project has no build anywhere, and a published compatibility tag does not prove bug-free operation. Verify exact files, licenses, dependencies, and client/server behavior before adopting any dependency.

## Fit for GTNH-Reforged

### Fabric — strengths and trade-offs

Strengths:
- Current, documented 26.3 development path.
- Strong ecosystem for performance, client-side tools, and lightweight integrations.
- A reasonable choice if we want to own most of the industrial gameplay and keep third-party dependencies small.
- Useful for a deliberately modular, loader-light implementation.

Trade-offs:
- Some large technology/content mods have historically favored Forge-family loaders or have not yet released 26.3 builds.
- We must verify that required machine, energy, fluid, item transport, multiblock, and recipe APIs exist or are practical to implement ourselves.

### NeoForge — strengths and trade-offs

Strengths:
- Current, documented 26.3 development path and official JDK 25 setup guidance.
- A strong candidate when integrating content-heavy mods and established Forge-family technology projects is important.
- Official tooling offers ModDevGradle and NeoGradle paths, with dedicated documentation and migration primers.

Trade-offs:
- General top-mod readiness is not automatically better than Fabric's.
- Some performance/client ecosystem projects may have different availability or maturity.
- It still cannot solve missing 26.3 ports of major mods such as Create or EMI.

## Provisional recommendation

Use **NeoForge as the first prototype candidate**, with these constraints:

1. Keep domain logic in Reforged-owned modules and avoid leaking loader-specific classes into materials, recipes, progression, and machine-domain models.
2. Put loader interaction behind a small platform adapter.
3. Do not depend on Create, EMI, or any unverified legacy mod.
4. Treat JEI as an optional integration, not a hard dependency.
5. Keep the recipe viewer replaceable so EMI/REI/JEI can be reconsidered when their 26.3 support changes.
6. Revisit the loader choice if the dependency audit shows that Fabric has the better exact set of required libraries or if NeoForge blocks a critical requirement.

This is a provisional engineering recommendation, not a final platform lock.

## Decision gates

Before selecting the loader permanently:

- [ ] Define the must-have dependency shortlist: recipe viewer, storage, energy, fluids, world generation, quest UI, performance, and server administration.
- [ ] Check exact 26.3 files and licenses for each candidate on Modrinth, CurseForge, and official project pages.
- [ ] Create one minimal Fabric and one minimal NeoForge 26.3 prototype.
- [ ] Build and launch both client and dedicated server with JDK 25.
- [ ] Implement a small benchmark slice: one custom material, one block entity/machine, one recipe type, one data reload, and one client UI screen.
- [ ] Compare startup time, memory use, tick cost, recipe lookup, dev iteration, and multiplayer behavior.
- [ ] Record the final choice and pin exact versions only after the above checks.

## Sources

- Fabric, *Fabric for Minecraft 26.3* (2026-09-15): https://www.fabricmc.net/2026/09/15/263.html
- Fabric Develop (live recommended versions and templates): https://fabricmc.net/develop/
- Fabric Documentation, developer guides: https://docs.fabricmc.net/develop/
- NeoForged Documentation, Getting Started: https://docs.neoforged.net/docs/gettingstarted/
- NeoForge project listing and 26.3 version: https://projects.neoforged.net/neoforged/neoforge/
- NeoForge 26.2 → 26.3 migration primer: https://docs.neoforged.net/primer/docs/26.3/
- Mod readiness sample, top 300 projects (checked 2026-11-18): https://modshift.app/version/26.3
- Create 26.3 status (checked 2026-11-18): https://mcmodsready.com/mod/create
- EMI 26.3 status (checked 2026-11-18): https://mcmodsready.com/mod/emi
- JEI 26.3 status (checked 2026-11-17): https://modshift.app/mod/jei

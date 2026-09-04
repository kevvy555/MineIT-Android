# Phase 5 — Production Native UI, Map and Design-System Foundation

**Status:** Implemented; final exact-head CI and hands-on device acceptance pending  
**Android branch:** `feature/migration-phase-5`  
**Web behavioural/presentation baseline:** `kevvy555/MineIT` `develop` at `9e58983adaa7a15cd525451266ce9df3c17ae886`  
**Android build:** `0.6.0-migration` / version code `11`

## Purpose

Phase 5 replaces the compact migration-validation presentation with the first production MineIT native interaction language while continuing to consume the canonical Phase 1–4 state, simulation, survey and colony/infrastructure owners.

This phase does **not** move gameplay truth into Compose and does not redesign the resource economy. UI state such as tile selection, multi-selection, focus and filters remains transient presentation state.

## Source presentation reviewed

The native direction was grounded against the current web presentation rather than inventing an unrelated visual identity. Relevant source owners include:

- `css/app.css` — compact dark shell and semantic resource colours;
- `css/map-first.css` — map-first header/status/context/footer layout;
- `css/world.css` — map toolbar, focus/filter controls and interaction states;
- `js/ui/map-controls.js` — canonical map focus/filter vocabulary;
- `js/ui/land-ui.js` — landing-site, tile/resource and colony-land flows;
- `js/ui/land-art.js` — terrain/development art contracts;
- `assets/art/**` — current optimized runtime artwork and retained source artwork.

## Phase 4 acceptance and starting point

The user accepted the `0.5.1-migration` Phase 4 validation build on physical Android hardware. The exact accepted Phase 4 head `8167ef8d9d5c45381fad1c66865406ac37c6348b` was fast-forwarded to `main` before this phase was cut.

Phase 5 therefore starts from the real canonical native game rather than a POC or parallel UI engine.

## Design language

The native palette now mirrors the established MineIT semantic language:

- background `#050708`;
- panels `#0d1318` / `#131c23`;
- line `#25333d`;
- primary text `#eef5f7`;
- muted text `#91a3ad`;
- Food `#6bd986`;
- Build `#8ec5d9`;
- Fuel `#ff9f5f`;
- Ore `#c7a0ff`;
- Survey `#62b8ff`;
- warning `#ffd166`;
- critical `#ff7777`;
- accent `#76c6ff`.

Reusable Compose primitives now own repeated panel, status, resource and action styling so later screens can use the same language rather than accumulating isolated styling.

## Production map-first shell

The gameplay screen now has a native map-first structure:

1. compact MineIT/contract/date/cash header;
2. operational/resource status presentation;
3. map focus/filter controls;
4. central 8×8 colony map;
5. selected-sector / multi-selection context actions;
6. simulation speed/day/menu footer;
7. native colony/Power detail sheet.

The main menu was also moved onto the same design primitives instead of retaining the migration-validation styling.

## Native map interaction

The 8×8 map remains the canonical `-4..3` coordinate grid.

Implemented interaction conventions:

- normal tap selects one sector;
- long-press starts multi-selection and gives sparse native haptic feedback;
- dragging after long-press adds sectors to the transient selection;
- bulk selected-sector surveying dispatches through `SurveyGameService` and persists through `GameSession`;
- building/development actions require a single selected sector and continue to dispatch through the Phase 4 domain owner;
- Android Back clears active map selection first, then returns to the native main menu;
- map cells expose accessibility descriptions and selected semantics.

Pixel-to-sector gesture conversion has JVM regression coverage so edge coordinates and out-of-bounds input remain deterministic.

## Map focus and filters

Phase 5 introduces transient native map focus/filter presentation without changing world state.

The focus vocabulary follows the web map-first UI, including all/problems/buildings/housing/industry/power/upgradeable and Food/Build/Fuel/Ore views. State filters cover relevant surveyed/unsurveyed, active/queued, developed and problem states.

`MapPresentation` owns derived presentation matching. It consumes domain state/network output but does not mutate gameplay or duplicate domain eligibility rules.

Regression coverage verifies resource focus, building/Spaceport focus, problem detection and composition of focus with state filters.

## Bundled MineIT artwork

The current MineIT web asset hierarchy was copied into Android at:

```text
app/src/main/assets/
```

by commit:

`65e148ba0a439a4192fe918be2d4f64842294110` — `feat: add pinned MineIT art assets for native offline UI`

The directory structure is intentionally preserved rather than renaming hundreds of assets into Android resource identifiers. This keeps paths recognizable and allows one native resolver to consume the existing optimized WebPs.

This is a snapshot of the current MineIT game assets. It is **not** a substitute for the separate build-pinned `MineIT-Universe` bundling pipeline defined in `UNIVERSE_BUNDLING_DIRECTION.md`.

### Runtime-art packaging

`MineItAssetPaths` is a presentation-only path resolver. `AssetBitmapCache` decodes immutable bundled art off the main thread and keeps a bounded process-local bitmap cache.

The production colony map now uses:

- all four plains terrain variants;
- all four hills terrain variants;
- all four mountain terrain variants;
- all four lake terrain variants;
- `resource-atlas-256.webp` for revealed undeveloped resources;
- five-frame 256px development atlases for Housing, Industry and current extraction families.

Power and Headquarters currently remain clear native map markers because the migrated source asset set does not contain canonical current Power/HQ artwork. New artwork is deliberately not invented as part of migration parity.

Source `Originals/` directories remain available in Git as art source material but are excluded from Android APK asset packaging. Runtime consumes the optimized WebP assets beside them. A green asset-integrated run produced an APK artifact ZIP of approximately 40.8 MB, confirming the source originals are not bloating the install artifact.

Temporary `drawable-nodpi` terrain copies used during the first Phase 5 wiring pass were removed once the canonical asset hierarchy became the only runtime path.

## Resource atlas contract

The native resolver follows the order recorded by `assets/art/resources/resource-atlas-256.json`: 40 current resources, 256px frames and eight columns.

JVM tests lock representative frame positions and all current terrain/development path mappings. This is a presentation mapping only; the canonical resource identity/catalogue remains under `domain/resources`.

## Colony and Power detail

A native detail sheet now surfaces the useful Phase 3/4 derived network state without owning it, including:

- Power capacity, Fuel-limited generation and demand;
- life-support status;
- Industry/workforce state;
- Spaceport status;
- Headquarters command source/load/capacity/continuity;
- first-departure handover readiness.

All values come from the existing simulation/network/Headquarters/Spaceport owners.

## Architecture boundaries preserved

- `GameSession` remains the canonical root state owner.
- `DailySimulationEngine` remains the daily gameplay owner.
- `SurveyGameService` remains the survey owner.
- `ColonyDevelopmentService` remains the construction/development owner.
- `ColonyNetworkService`, `HeadquartersService` and `SpaceportService` remain the network owners.
- Compose only renders derived state and dispatches intent.
- Selection/focus/filter state is transient and is not added to the durable save schema.
- Artwork paths/caching live under `ui/art`; the domain has no Android asset dependency.
- No second production map or gameplay implementation is retained.

## Test coverage added/strengthened

Phase 5 adds JVM regression coverage for:

- map focus/filter semantics;
- problem presentation;
- 8×8 gesture pixel-to-sector conversion;
- canonical terrain asset paths;
- resource-atlas frame mapping;
- current development-atlas mapping.

All existing Phase 0–4 domain, parity, persistence and simulation tests remain authoritative.

## Build validation

The first complete asset-integrated implementation head `147e9118f60014420b499cb545f64cb8f23b038f` passed Android CI run `33871442488` (run number 203), including JVM tests, APK assembly, persistent development-signer verification and artifact upload.

A final CI run on the post-cleanup/documented head is required before the Phase 5 APK is handed off.

## Hands-on validation checklist

Before Phase 5 is accepted on device, verify:

1. install `0.6.0-migration` directly over `0.5.1-migration` without uninstalling;
2. existing healthy save can Continue and New Game still works;
3. choose a landing site and confirm the 8×8 map remains the dominant gameplay surface;
4. terrain uses actual MineIT plains/hills/mountain/lake art rather than flat placeholder colours;
5. survey a resource and confirm resource art/quality/status presentation appears;
6. build/develop a supported site and confirm level/development art/status appears;
7. tap selects one tile and exposes appropriate context actions;
8. long-press then drag selects multiple sectors and bulk survey can enqueue valid sectors;
9. focus/filter controls visibly isolate the intended map concepts;
10. colony/Power detail opens and reflects the current domain metrics;
11. Pause/1×/2×/4×/+1 Day still operate through the real Phase 3 simulation;
12. Android Back clears active selection before leaving gameplay;
13. colony death still returns to the native main menu;
14. no obvious clipping/unusable touch targets on the primary phone layout.

## Exit gate

Phase 5 is complete when the exact documented head is CI/signing green and the production map-first core colony experience passes physical-device acceptance.

After acceptance, promote the exact accepted Phase 5 head to `main` before starting Phase 6.

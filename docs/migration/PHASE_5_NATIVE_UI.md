# Phase 5 — Production Native UI, Map and Design-System Foundation

**Status:** Accepted — implementation, exact-head CI and physical-device validation complete  
**Accepted:** 4 September 2026  
**Android branch:** `feature/migration-phase-5`  
**Accepted Android head:** `b9364a4a8d7e9566df6e16d5f61e93f83bc6cd8d`  
**Accepted build:** `0.6.0-migration` / version code `11`  
**Final CI:** Android CI run `33871930819` / run number `211` — success  
**Web behavioural/presentation baseline:** `kevvy555/MineIT` `develop` at `9e58983adaa7a15cd525451266ce9df3c17ae886`

## Outcome

Phase 5 replaced the migration-validation presentation with MineIT's first production native interaction language while preserving the Phase 1–4 state, simulation, survey and infrastructure owners. The UI remains presentation only: Compose renders immutable state and dispatches intent; gameplay truth remains in domain services.

The user validated `0.6.0-migration` on physical Android hardware and confirmed the migration was in a sufficiently good state to continue. Screenshots demonstrated both landing-site selection and the settled 8×8 map with real MineIT terrain/building artwork, status HUD, selected-sector actions and production map-first layout.

The exact accepted Phase 5 head was fast-forwarded to `main` before `feature/migration-phase-6` was cut.

## Production native design language

Phase 5 established centralized MineIT semantic tokens and reusable Compose primitives for the compact dark industrial UI. The palette, spacing, radii, panels, status/stat treatments, resource cards, primary/secondary/destructive actions and preview components are shared rather than recreated per feature.

The production shell is map-first:

1. MineIT/contract/date/cash header;
2. resource and operational status;
3. map focus/filter controls;
4. central 8×8 colony map;
5. selected-sector or multi-selection context actions;
6. simulation footer;
7. native colony/Power detail sheet.

## Native map interaction

The canonical map remains the 8×8 `-4..3` sector grid. Phase 5 locked:

- tap selects one sector;
- long-press starts multi-selection with sparse haptic feedback;
- drag after long-press adds sectors;
- bulk survey dispatches through `SurveyGameService` and persists via `GameSession`;
- building/development actions remain owned by Phase 4 domain services;
- Android Back clears active selection before returning to the main menu;
- map cells expose accessibility descriptions and selected semantics;
- pixel-to-sector conversion has JVM edge/out-of-bounds regression coverage.

Transient focus/filter state supports all/problems/buildings/housing/industry/power/upgradeable and Food/Build/Fuel/Ore views without entering the save schema.

## Bundled MineIT artwork

The current MineIT web asset hierarchy is preserved under:

```text
app/src/main/assets/
```

The asset import was commit `65e148ba0a439a4192fe918be2d4f64842294110`.

`MineItAssetPaths` and `AssetBitmapCache` consume the optimized bundled WebPs. Runtime map presentation uses all current plains/hills/mountain/lake terrain variants, the resource atlas and current Housing/Industry/extraction development atlases. Power and Headquarters remain native markers because the source snapshot does not contain canonical current art for them.

Source `Originals/` directories remain in Git but are excluded from APK packaging. Temporary duplicate `drawable-nodpi` terrain copies were removed once the canonical asset hierarchy became the sole runtime path.

This asset snapshot does not replace the separate build-pinned `MineIT-Universe` pipeline defined in `UNIVERSE_BUNDLING_DIRECTION.md`.

## Architecture boundaries preserved

- `GameSession` owns root state and persistence coordination;
- `DailySimulationEngine` owns daily gameplay;
- `SurveyGameService` owns surveying;
- `ColonyDevelopmentService` owns construction/development;
- `ColonyNetworkService`, `HeadquartersService` and `SpaceportService` own network truth;
- Compose owns only transient selection/focus/filter/sheet state;
- artwork path/caching code lives under `ui/art` and the domain has no Android asset dependency;
- no second production map or gameplay implementation was retained.

## Regression and CI closure

Phase 5 added/strengthened regression coverage for map focus/filter semantics, problem presentation, gesture coordinate mapping, terrain paths, resource-atlas frames and development-atlas mappings. All earlier domain, parity, persistence and simulation tests remained active.

The final post-cleanup/documented head `b9364a4a8d7e9566df6e16d5f61e93f83bc6cd8d` passed Android CI run `33871930819` including JVM tests, debug APK assembly, persistent development-signer verification and artifact upload.

## Physical-device acceptance

Accepted observations from `0.6.0-migration` included:

- direct install/update path remained healthy;
- New Game and landing-site selection worked;
- settled map retained the dominant 8×8 gameplay surface;
- real terrain artwork rendered correctly;
- building/development artwork and selected-sector controls rendered correctly;
- core simulation controls and colony operational HUD remained usable;
- overall presentation was considered good enough to continue the migration.

Any later visual polish discovered during subsequent feature migration should use the shared Phase 5 design system rather than reopening Phase 5 or creating parallel UI implementations.

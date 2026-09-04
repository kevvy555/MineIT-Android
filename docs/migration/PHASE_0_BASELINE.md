# MineIT Android Migration — Phase 0 Baseline

**Phase:** 0 — Baseline and migration harness  
**Status:** In Progress  
**Captured:** 4 September 2026

## Source baseline

| Item | Baseline |
|---|---|
| Web repository | `kevvy555/MineIT` |
| Web branch | `develop` |
| Web commit | `9e58983adaa7a15cd525451266ce9df3c17ae886` |
| Web game version | `5.13.15` |
| Web save version | `16` |
| Android repository | `kevvy555/MineIT-Android` |
| Android branch | `feature/migration-phase-0` |
| Android POC version at phase start | `0.1.3-poc` / version code `4` |
| Canonical shared universe | `kevvy555/MineIT-Universe` |

The web baseline remains the behavioural reference for parity fixtures until a later baseline is deliberately recorded in the main migration guide.

## Web CI retained as behavioural reference

The source web repository keeps its existing CI unchanged during migration. In particular:

- `.github/workflows/test.yml` remains the authoritative broad web test workflow;
- the Node/domain/regression suite remains the source behaviour inventory;
- browser/mobile interaction probes remain the presentation/input reference where behaviour is still web-only;
- `rebuild-resource-atlas.yml` and `rebuild-building-atlases.yml` remain source asset-pipeline workflows and are not copied mechanically into Android.

Android parity work must not weaken source tests simply to make a native implementation easier to match.

## Source architecture inventory

### Application/composition

- `js/app.js` — composition root, simulation scheduling, cross-colony orchestration, corporate-event sequencing, lifecycle persistence and broad UI coordination.
- `js/core/game-store.js` — mutable root-state owner/subscription boundary.
- `js/core/config.js` — global gameplay configuration constants.
- `js/persistence/save-repository.js` — localStorage JSON persistence.

### Canonical gameplay/domain owners

- `game-state.js` / `game-state-runtime.js` — state creation, schema normalisation and legacy migration.
- `simulation-engine.js` — daily production, demand, shortages, mortality, site/engineering effects and network refresh.
- `resource-service.js` — resource rules/rates/valuation inputs.
- `inventory-service.js` — stock, quality bands and category consumption.
- `collection-service.js` — extraction/collection and depletion/renewable site behaviour.
- `colony-service.js` — workforce, Industry, Power, demand, Headquarters and colony network calculations.
- `site-service.js` — extraction-site construction/upgrades.
- `development-service.js` / `building-model.js` / `spaceport-model.js` — built infrastructure.
- `world-service.js` / `land-service.js` / `survey-service.js` — world generation, land ownership and scanning.
- `technology-service.js` — company/local technology and engineering deployment.
- `contract-service.js` / `portfolio-service.js` / `corporate-event-service.js` — contract and multi-colony lifecycle.
- `trade-service.js` / `buyer-service.js` — commercial trade/buyer contracts.
- `expansion-service.js` / `ship-market-service.js` / `transport-service.js` — ships, expansion, procurement and transport.
- `game-log-service.js` — player-visible history/telemetry.

## Important player-facing screen/flow inventory

The native feature matrix must account for at least these current web experiences before cutover:

1. main game HUD/resource status;
2. world/sector map, filters, survey queue, tap/inspect and hold/drag multi-select;
3. tile/resource inspection and extraction-site actions;
4. build choice and adaptive building/upgrade/demolition flows;
5. colony status, survival, Power, workforce, Industry and Headquarters information;
6. company/corporation overview and colony portfolio/switching;
7. technology and engineering deployment flows;
8. corporate trade ship, buy/sell and reserve controls;
9. conglomerate buyer offers, active contracts and collection events;
10. contract goals, deadlines, renewal/holdover, completion/failure and new-colony flow;
11. player/founding ship panel, cargo/passengers/accommodation and departure gating;
12. star/system navigation and planet/colony tables;
13. factory-new ship catalogue/procurement;
14. warnings, blocking corporate events, toasts/dialogs and colony/game-over flows;
15. how-to-play/onboarding/help;
16. development-task/debug tooling where it remains useful in native development builds.

This is an ownership/coverage inventory, not a requirement to reproduce every web layout literally.

## Parity-fixture convention

Cross-runtime fixtures live under:

`app/src/test/resources/parity/`

Initial web-v16 fixtures live under:

`app/src/test/resources/parity/web-v16/`

A fixture contains:

- metadata identifying the exact source repository/branch/commit/game/save versions;
- the source web state or input payload;
- an expected canonical summary;
- later, the action/day sequence and deterministic random inputs where relevant.

Tests compare gameplay meaning, not JavaScript object ordering or presentation details.

## POC isolation rule

The initial native demo remains useful as a smoke-test screen while the migration foundation is built, but it is not production MineIT state.

All demo domain types therefore live under `domain/poc` and use explicit `Poc*` names. Generic names such as `GameState` and `GameSimulation` are reserved for the real migrated implementation.

The POC package must be removed once the first real playable native slice replaces it.

## Phase 0 exit gate

Phase 0 is complete when:

- source baseline is recorded;
- source domain/screen owners are inventoried;
- web CI is explicitly retained as the behavioural reference;
- at least one representative web-v16 fixture exists;
- Android JVM tests can load and validate that fixture;
- the intentional-divergence log exists;
- temporary POC domain models no longer occupy production names.

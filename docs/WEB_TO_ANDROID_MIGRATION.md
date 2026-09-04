# MineIT Web-to-Native Android Migration Guide

**Status:** Living migration plan — Phases 0–5 complete/accepted; Phase 6 implementation and current-source refresh are CI-green, physical-device acceptance still pending  
**Source application:** `kevvy555/MineIT` (`develop`)  
**Current behavioural + UI-layout baseline:** `075b3d82fd88334b20b3cfe7d6e2731c8d840533`  
**Current source game version:** `5.13.22`  
**Current source save version:** `16`  
**Target application:** `kevvy555/MineIT-Android`  
**Current Android migration build:** `0.7.1-migration` / version code `13`  
**Current native save format:** `6`  
**Current Android branch:** `feature/migration-phase-6`  
**Current N05/UI checkpoint:** `35a03b32a0f4360a6f50e1a1e870ef9fced3bdc4`  
**Current checkpoint CI:** Android CI `33912720951` / run `275` — success  
**Canonical shared universe:** `kevvy555/MineIT-Universe`

## 1. Purpose

This document is the authoritative plan for migrating MineIT from vanilla HTML/CSS/modular JavaScript to a fully native Android application built with Kotlin and Jetpack Compose.

The migration is not a line-for-line rewrite. The current web `develop` implementation remains the behavioural reference and, for player-facing work, the layout/information-hierarchy reference until native parity is accepted.

The governing rule is:

> **Preserve gameplay by default. Improve architecture where the improvement does not materially change gameplay. Preserve recognisable UI hierarchy/workflow unless a deliberate refinement is approved.**

Clear defects may be corrected with regression coverage. Material balance, progression, economy or design changes remain separate approved work.

The target is a native, offline-first Android game with no WebView and no mandatory backend/server dependency.

## 2. Recovery documents and decisions

The migration is recoverable from repository state and these documents; chat history is never the only source of truth.

- [`migration/PHASE_0_BASELINE.md`](./migration/PHASE_0_BASELINE.md) — original frozen source baseline, owner/player-flow inventory and parity-fixture convention;
- [`migration/PHASE_1_FOUNDATION.md`](./migration/PHASE_1_FOUNDATION.md) — native state/persistence foundation;
- [`migration/PHASE_2_CORE_WORLD.md`](./migration/PHASE_2_CORE_WORLD.md) — Contract 01 resources/world/discovery/survey implementation;
- [`migration/PHASE_3_DAILY_SIMULATION.md`](./migration/PHASE_3_DAILY_SIMULATION.md) — accepted daily simulation/colony-survival implementation;
- [`migration/PHASE_4_BUILDINGS_HEADQUARTERS.md`](./migration/PHASE_4_BUILDINGS_HEADQUARTERS.md) — buildings, extraction, Power, Industry, Spaceport and Headquarters;
- [`migration/PHASE_5_NATIVE_UI.md`](./migration/PHASE_5_NATIVE_UI.md) — accepted production native map/UI/design-system foundation;
- [`migration/PHASE_6_CONTRACTS_TRADE_EVENTS.md`](./migration/PHASE_6_CONTRACTS_TRADE_EVENTS.md) — current commercial implementation, N05 source refresh and validation status;
- [`migration/INTENTIONAL_DIVERGENCES.md`](./migration/INTENTIONAL_DIVERGENCES.md) — deliberate semantic differences only;
- [`migration/RESOURCE_ARCHITECTURE_DIRECTION.md`](./migration/RESOURCE_ARCHITECTURE_DIRECTION.md) — architecture improvements allowed while the resource overhaul remains deferred;
- [`migration/DESIGN_SYSTEM_DIRECTION.md`](./migration/DESIGN_SYSTEM_DIRECTION.md) — MineIT-wide visual/interaction consistency and UI-led vertical-slice method;
- [`migration/UNIVERSE_BUNDLING_DIRECTION.md`](./migration/UNIVERSE_BUNDLING_DIRECTION.md) — build-time MineIT-Universe pinning/validation/offline bundling.

The source baseline recorded in Phase 0 is historical. The **active migration baseline is the one at the top of this document** and must be refreshed whenever current web `develop` materially changes behaviour already inside migrated native scope.

## 3. Migration goals

The migration is successful when:

1. MineIT runs fully natively with Kotlin and Jetpack Compose.
2. Existing gameplay behaviour is preserved unless a difference is deliberately approved and recorded.
3. One authoritative native root state owner controls gameplay state.
4. UI renders immutable state and dispatches intent; it does not own gameplay truth.
5. Simulation is independent of rendering/frame rate and deterministic where the source is deterministic.
6. Save data is versioned, atomic, recoverable and migration-tested.
7. Existing web saves can be imported where practical/required.
8. MineIT-Universe remains canonical and required gameplay data/art is bundled for offline play.
9. Native UI uses a consistent MineIT design language while preserving recognisable web hierarchy/workflows by default.
10. Android CI proves domain/parity tests, save migration, build validity and signing consistency.
11. Each player-facing vertical slice is validated on-device before acceptance.
12. The web implementation remains the behavioural/layout reference until native cutover.
13. After cutover, no permanent duplicate game engines remain unless a supported web edition is deliberately chosen.

## 4. Explicit non-goals

Do not allow the migration to become an uncontrolled redesign.

Do **not** automatically:

- redesign gameplay because code is moving;
- materially redesign a working screen because another Compose layout is convenient;
- perform the full resource-economy overhaul;
- define the final refined/manufactured catalogue;
- rebalance prices, yields, Fuel, nutrition or progression;
- preserve known bugs merely for parity;
- reproduce browser concepts when Android has a cleaner equivalent;
- introduce a mandatory backend/login;
- keep a WebView as compatibility architecture;
- split into many Gradle modules without measurable benefit;
- preserve POC/demo models as production contracts;
- create speculative abstractions/frameworks without a current migration or known-roadmap need;
- pull full Phase 8 ship market/travel/navigation work forward merely because N05 now requires a durable founding-ship foundation.

## 5. Source ownership being migrated

The web application already has useful separation:

- `js/core/` — infrastructure/utilities and `GameStore`;
- `js/data/` — static definitions/configuration;
- `js/domain/` — authoritative gameplay services/state behaviour;
- `js/persistence/` — save/development-task persistence;
- `js/ui/` — presentation/controllers;
- `views/` — reusable HTML structures;
- `css/` — visual/layout rules;
- `tests/` — architecture, unit, regression, simulation and browser probes.

`GameStore` owns mutable web root state. Domain services own gameplay decisions. UI renders and dispatches.

### 5.1 `app.js` is an architecture opportunity

Web `app.js` combines dependency construction, state normalisation, scheduling, cross-colony orchestration, event sequencing, lifecycle saving, UI coordination and browser-loop responsibilities.

Native code must not recreate it as one giant Activity/Application/manager class.

### 5.2 Current persistence

Web saves are inspectable JSON in `localStorage`, source schema version `16`.

Preserve:

- explicit versioning;
- inspectable state;
- realistic round-trip/migration knowledge.

Improve natively:

- atomic file writes;
- previous-known-good backup;
- ordered explicit migrations;
- validation/recovery diagnostics;
- durable/derived/transient separation.

### 5.3 Current simulation

Web `simulation-engine.js` remains the principal reference for production, Power, Fuel, Industry, workforce, Food, mortality, depletion, accidents, engineering and related daily effects.

Browser scheduling is not gameplay. Native scheduling is separated from the deterministic day transition.

## 6. Target Android architecture

Keep one Gradle app module until measurable build/ownership/reuse pressure justifies more.

```text
app/src/main/java/com/mineit/android/
├── app/
│   ├── GameSession.kt
│   ├── SimulationClock.kt
│   ├── AppComposition.kt
│   └── coordinators/
├── domain/
│   ├── model/
│   ├── config/
│   ├── simulation/
│   ├── resources/
│   ├── colony/
│   ├── world/
│   ├── technology/
│   ├── contracts/
│   ├── portfolio/
│   ├── trade/
│   ├── buyers/
│   ├── ships/
│   ├── events/
│   └── logging/
├── data/
│   ├── save/
│   ├── universe/
│   └── preferences/
└── ui/
    ├── design/
    ├── game/
    ├── map/
    ├── colony/
    ├── resources/
    ├── trade/
    ├── contracts/
    ├── ships/
    └── common/
```

### 6.1 Dependency direction

```text
Compose UI
    ↓
feature ViewModel / UI state
    ↓
application/session actions
    ↓
domain services / simulation
    ↓
domain models

Data adapters implement application/domain contracts.
Android platform APIs stay outside pure domain code.
```

The domain layer must not depend on Compose, Activity, `Context`, lifecycle, filesystem or network APIs.

## 7. Game-state ownership

### 7.1 `GameSession`

`GameSession` is the native application root owner:

```text
GameSession
  owns StateFlow<GameState>
  serialises gameplay commands
  invokes domain behaviour
  commits authoritative next state
  coordinates persistence
```

Compose observes via ViewModels. It never mutates root state directly.

### 7.2 State rules

- UI-visible state is immutable;
- domain operations return explicit results/next state;
- derived values are recomputed where safe;
- presentation/session state such as map selection/camera stays outside durable game state unless gameplay requires it;
- imported fields are not removed from a real migration path until corresponding owners exist and parity coverage supports the decision;
- strong IDs/value types are used where they prevent realistic mistakes, not cosmetically.

### 7.3 Canonical calendar

MineIT uses **360 days per game year**. Native `GameDate` preserves this.

## 8. Deterministic simulation and time

`SimulationClock` is an application-layer coroutine clock that maps speed to day cadence, pauses at zero/blocking events and asks `GameSession` to advance a day. It owns no gameplay rules.

Rendering never drives gameplay. The same domain day transition must produce the same result when invoked by the clock, tests or diagnostics, given the same durable/random state.

Randomness that affects gameplay must be explicit/reproducible where the source requires reproducibility.

## 9. Persistence and migration

### 9.1 Native envelope

Production saves use `kotlinx.serialization`:

```text
SaveEnvelope
- formatVersion
- gameVersion
- universeContentVersion
- universeSourceCommit
- savedAtEpochMillis
- state
```

`FileGameStatePersistence` writes app-private JSON atomically where supported, validates before promotion and preserves a previous-good backup.

Use preferences/DataStore for small settings only, not the root game state.

### 9.2 Web v16 import boundary

Keep JavaScript compatibility out of canonical native models:

```text
Web JSON v16
  ↓
WebSaveV16 DTO/importer
  ↓
validation
  ↓
explicit adapters as native owners exist
  ↓
current GameState
  ↓
normal native SaveEnvelope
```

Do not invent mappings for features whose native semantic owner does not yet exist.

### 9.3 Explicit native migration chain

```text
web v16 → native current
native v1 → v2
native v2 → v3
native v3 → v4
native v4 → v5
native v5 → v6
```

Every real native migration gets fixture coverage.

- v4 added durable development investment/resource-cover and Headquarters continuity state;
- v5 added commercial/trade, buyer, contract-decision, corporate-event and Game Log state;
- v6 adds durable N05 player-fleet/founding-establishment state.

The v5→v6 migration deliberately preserves existing Android colony inventory/residents **ashore**. Old saves are not retroactively converted into a fresh N05 start.

## 10. MineIT-Universe build-time bundling

Authoritative direction: [`migration/UNIVERSE_BUNDLING_DIRECTION.md`](./migration/UNIVERSE_BUNDLING_DIRECTION.md).

```text
MineIT-Universe
   ↓ pinned commit/content version
sync + validation
   ↓
Android bundled JSON/art
   ↓
APK/AAB install-time content
   ↓
read-only UniverseRepository
```

Required canonical data/art must be locally available after installation. CI should reject unsupported schema, malformed data, duplicate IDs, missing references and missing required image assets.

Android stores a generated/pinned snapshot, never a second manually maintained canonical Universe.

## 11. Resource architecture during migration

Authoritative direction: [`migration/RESOURCE_ARCHITECTURE_DIRECTION.md`](./migration/RESOURCE_ARCHITECTURE_DIRECTION.md).

Decision: **improve resource architecture now; defer the resource overhaul itself.**

Native direction includes:

- stable `ResourceId` independent of display names;
- generic quantity + quality-band storage;
- resource definitions/metadata outside UI;
- current Food/Build/Fuel/Ore categories without making those categories permanent architectural limits;
- one extraction-compatibility owner;
- room for future raw/refined/manufactured classifications and recipes without implementing them now;
- data-driven presentation metadata.

Deferred unless separately approved:

- final resource renames/merges/removals;
- new distribution/extraction rules;
- refined/manufactured catalogue;
- production-order/recipe gameplay;
- generator or ship-fuel redesign;
- nutrition/energy/quality/economy/progression rebalance.

## 12. MineIT design consistency and UI parity

Authoritative direction: [`migration/DESIGN_SYSTEM_DIRECTION.md`](./migration/DESIGN_SYSTEM_DIRECTION.md).

> **Current web MineIT is the layout and information-hierarchy reference. Preserve what works and improve deliberately.**

Before implementing or substantially revising a player-facing screen, inspect together:

- current HTML/view structure;
- CSS determining layout/hierarchy;
- `js/ui/` controller behaviour;
- relevant domain owner/tests.

For the screen, decide what is **preserved**, **refined** or **replaced**.

Native refinements are encouraged for safe areas, responsive sizing, typography, touch targets, accessibility, Android Back/navigation, sheets/dialogs and subtle haptics. A materially different hierarchy/workflow is a deliberate design decision, not an incidental Compose choice.

### 12.1 HTML/CSS mocks

HTML/CSS remains valid as a rapid visual specification/prototype:

```text
current web screen
 → inspect source layout/controllers
 → HTML/CSS mock if a material refinement is proposed
 → visual approval where useful
 → Compose using shared MineIT primitives
 → interaction/screenshot coverage
 → APK hands-on review
```

Production remains native Compose; no WebView.

### 12.2 Design primitives

Standardise semantic colours/states, typography, spacing, radii/borders/elevation, image/icon sizing, warnings/selection/progress, touch targets, animation and haptic intent.

Create reusable composables only where genuine repetition exists.

## 13. Interaction, accessibility and lifecycle

- taps use normal Compose semantic click handling;
- pointer input is reserved for genuine gestures such as drag/long-press/multi-select;
- do not create competing pointer-up/tap activation paths;
- Android Back behaves consistently through navigation/dialogs;
- touch targets/accessibility semantics are requirements;
- ViewModel/coroutine scopes reject stale asynchronous writes;
- haptics remain sparse and consistent;
- lifecycle persistence is explicit/testable.

## 14. Development diagnostics

A development-only diagnostics surface should progressively expose:

- app/game/native-save versions;
- Universe content/schema/source commit;
- current absolute day;
- deterministic random seed/state;
- active colony;
- pending corporate events;
- Power/workforce/Industry summaries;
- save/backup health;
- import/export save;
- copy diagnostics;
- deterministic day controls.

Debug actions stay out of release UX unless deliberately promoted.

## 15. Web-to-Android ownership map

| Web owner | Native target | Rule |
|---|---|---|
| `js/core/config.js` | `domain/config` | typed configuration/value objects |
| `js/core/game-store.js` | `app/GameSession` | immutable StateFlow boundary |
| `js/domain/game-state*.js` | `domain/model` + migrations | current schema separate from legacy import |
| `js/domain/simulation-engine.js` | `domain/simulation` | parity first, optimise later |
| `js/domain/*-service.js` | matching feature packages | preserve semantic ownership |
| `js/data/*.js` | typed definitions/bundled adapters | static data outside UI |
| `js/persistence/save-repository.js` | `data/save` | atomic files + backup + migrations |
| `js/app.js` | composition/session/clock/coordinators | split responsibilities |
| `js/ui/*.js` | ViewModels/composables | render state + dispatch intent |
| `views/*.html` | composables | preserve hierarchy, not DOM mechanics |
| `css/*` | design tokens/modifiers/components | consolidate visual rules |
| canvas map/star views | Compose Canvas/custom drawing | native rendering/input |
| browser lifecycle | coroutine/ViewModel/Compose lifecycle | scoped ownership |
| Node/browser tests | JUnit/Compose/screenshot/instrumentation | preserve behavioural intent |

## 16. Current domain migration inventory

### Core state / simulation

- [x] canonical root state foundation;
- [x] `GameSession` ownership;
- [x] current native save migration chain through v6;
- [x] canonical date/absolute day;
- [x] deterministic world RNG primitives;
- [x] daily simulation engine;
- [x] speed/pause/`SimulationClock`;
- [x] durable Game Log;
- [ ] full web-v16 conversion;
- [ ] explicit deterministic RNG state for later stochastic subsystems where needed.

### Resources / colony / infrastructure

- [x] current resource definitions;
- [x] generic inventory + quality bands;
- [x] extraction compatibility owner;
- [x] collection/depletion/renewables;
- [x] colony demand/workforce/Industry;
- [x] site development;
- [x] buildings/Spaceport;
- [x] Power network;
- [x] Headquarters command/outage/recovery;
- [x] current Food/Fuel/Ore/Build simulation;
- [x] N05 split ship/planetary resident survival/network behaviour;
- [x] N05 founding-colony establishment assessment/actions.

### World / survey

- [x] deterministic landing-site terrain;
- [x] persistent world/tile state;
- [x] deterministic discovery;
- [x] scanning/resurvey history;
- [x] survey queue/timing;
- [x] production native map;
- [x] camera/focus/filters;
- [x] tap/inspect/long-press/multi-select.

### Technology / progression

- [x] technology level state foundation;
- [x] Contract 01 required technology values;
- [ ] full technology definitions/service;
- [ ] company vs local engineering;
- [ ] engineering deployments;
- [ ] full progression eligibility beyond migrated building/site gates;
- [ ] progression visibility/unlocks.

### Contracts / corporation

- [x] Contract 01 canonical data;
- [x] Contract 01 contract service;
- [x] deadlines/renewal/holdover/end states;
- [x] commercial reputation/cash consequences;
- [x] corporate event sequencing;
- [ ] portfolio/multi-colony lifecycle;
- [ ] colony creation/switch/removal/relocation;
- [ ] portfolio-wide game-over.

### Trade / buyers

- [x] Corporate Ship trade service;
- [x] buyer market/service;
- [x] Corporate Ship events;
- [x] import/export capacity/reserves;
- [x] buyer offers/contracts/collections;
- [x] relationship/miss/termination behaviour;
- [ ] final Phase 6 web-layout refinement/device acceptance.

### Ships / expansion

N05 required a narrow permanent fleet foundation earlier than the broader Phase 8 work.

- [x] durable `FleetState` / `PlayerShipState` foundation;
- [x] founding-ship starter manifest ownership;
- [x] ship residents/crew/accommodation facts needed for first-colony survival;
- [x] exact ship↔colony inventory transfer foundation;
- [x] self-contained founding-ship 50 Industry behaviour;
- [x] founding handover/resident-transfer rules;
- [ ] full fleet/ship-management screen;
- [ ] expansion service beyond establishment subset;
- [ ] ship market/procurement;
- [ ] transport orders;
- [ ] general player-ship cargo/passenger management beyond N05;
- [ ] travel/location/navigation;
- [ ] star/system/planet navigation;
- [ ] current/future approved ship Fuel/wear/travel systems at their roadmap point.

### Shared Universe

- [ ] snapshot sync/validation;
- [ ] ship classes/runtime profiles;
- [ ] organisations/manufacturers used by MineIT;
- [ ] art/image-key mapping;
- [ ] provenance reporting;
- [ ] offline load validation.

## 17. Migration strategy — foundations then UI-led vertical slices

Phases 0–6 established substantial permanent foundations: state, persistence, world/survey, daily simulation, buildings/Power/HQ and commercial services.

From Phase 6 refinement onward, player-facing migration is **UI-led vertical feature work**:

1. inspect current web view/HTML, CSS, controller and domain/tests together;
2. decide preserve/refine/replace;
3. identify what native backend already exists;
4. implement only missing canonical domain/application behaviour;
5. recreate the screen in Compose using recognisable hierarchy/workflow;
6. add domain, interaction and presentation coverage;
7. run focused tests + full Android CI;
8. validate the complete slice on device;
9. update migration docs/status;
10. merge only the exact accepted head.

Cross-cutting source changes that alter already-migrated behaviour must be refreshed before accepting the active native phase. N05 is the first explicit example.

## 18. Phase roadmap and status

### Phase 0 — Baseline and harness — COMPLETE

- [x] original source baseline recorded;
- [x] owners/player flows inventoried;
- [x] web CI retained as reference;
- [x] parity-fixture structure created;
- [x] representative web-v16 fixture added;
- [x] JVM fixture loader/tests added;
- [x] divergence log created;
- [x] POC ownership isolated.

### Phase 1 — Native state/persistence — COMPLETE

- [x] canonical `GameState`/IDs;
- [x] generic resource foundation;
- [x] `GameSession`;
- [x] save envelope;
- [x] atomic active + previous-good backup;
- [x] migration/recovery tests;
- [x] web-import boundary;
- [x] `0.2.0-migration` signed update build.

### Phase 2 — Resources/world/survey — COMPLETE / ACCEPTED

- [x] 360-day calendar;
- [x] current 40-resource catalogue;
- [x] Contract 01 data;
- [x] deterministic eight-candidate 8×8 landing generation;
- [x] deterministic discovery;
- [x] survey/resurvey rules;
- [x] parity fixtures;
- [x] native v1→v2 migration;
- [x] hands-on acceptance.

### Phase 3 — Colony survival/daily simulation — COMPLETE / ACCEPTED

- [x] extraction/production day path;
- [x] network/demand/consumption;
- [x] Food/Power/Fuel survival;
- [x] mortality/death;
- [x] `SimulationClock`;
- [x] deterministic soak coverage;
- [x] `0.4.0-migration` hands-on acceptance.

### Phase 4 — Buildings/Power/Industry/HQ — COMPLETE / ACCEPTED

- [x] placement/demolition;
- [x] extraction development/upgrades;
- [x] Housing/Industry/Spaceport;
- [x] Power priority/allocation;
- [x] HQ capacity/load/departure/outage/recovery;
- [x] temporary ship command takeover;
- [x] costs and save migration coverage;
- [x] hands-on acceptance.

### Phase 5 — Native game UI/map/design foundation — COMPLETE / ACCEPTED

- [x] MineIT design tokens/components;
- [x] map tiles/art;
- [x] select/survey/multi-select;
- [x] queue/filter/focus;
- [x] building/site panels;
- [x] colony/Power detail foundation;
- [x] responsive phone layout;
- [x] accessibility/back/haptic conventions;
- [x] `0.6.0-migration` accepted on physical Android hardware;
- [x] exact accepted head promoted to `main`.

Phase 6 device review later identified that some secondary/detail screens built on this foundation had drifted too far from web hierarchy. That feedback changed the migration method, not the accepted Phase 5 foundation.

### Phase 6 — Trade/contracts/commercial events + current-source refresh — IMPLEMENTED / VALIDATION PENDING

Canonical commercial backend is implemented and regression-green. Initial `0.7.0-migration` device review exposed layout drift. During refinement, source `develop` advanced materially to `5.13.22`; N05 first-colony establishment was therefore refreshed before native acceptance.

Completed:

- [x] Corporate Ship arrival/departure and buy/sell;
- [x] visit capacity/reserve/quality pricing;
- [x] Corporate Ship colonist transfer;
- [x] Contract 01 lifecycle;
- [x] corporate event queue/recovery/pause;
- [x] buyer offers/contracts/collections/relationship outcomes;
- [x] durable Game Log;
- [x] save v5 commercial state;
- [x] source baseline refreshed to `075b3d82` / `5.13.22`;
- [x] durable N05 founding fleet/start state;
- [x] ship/planet resident survival split;
- [x] founding ship 50 self-powered Industry;
- [x] explicit quality-preserving ship/colony inventory transfer;
- [x] real Housing/Power/Spaceport resident-transfer gates + shortage confirmation;
- [x] establishment assessment/checklist and acknowledgement;
- [x] stacked Ship/Colony HUD;
- [x] founding handover Compose surface;
- [x] save v6 compatibility preserving old Android ashore state;
- [x] N05 regression suite including former day-31 failure and 90-day aboard survival;
- [x] current checkpoint `35a03b3` passes full Android CI run 275, signer verification and artifact upload.

Still required before acceptance:

- [ ] final Colony Details web-layout/device review;
- [ ] final Corporate Ship/Trade refinement/device review;
- [ ] final Contract refinement/device review;
- [ ] final Buyers refinement/device review;
- [ ] final Game Log refinement/device review;
- [ ] N05 S/C HUD and founding handover physical-device validation;
- [ ] revised Phase 6 APK hands-on acceptance.

**Do not mark Phase 6 accepted or merge it to `main` until those hands-on checks are accepted.**

Detailed record: [`migration/PHASE_6_CONTRACTS_TRADE_EVENTS.md`](./migration/PHASE_6_CONTRACTS_TRADE_EVENTS.md).

### Phase 7 — Portfolio and multi-colony vertical slices

Start from current web Colonies/Portfolio flows and migrate UI plus required domain behaviour together.

Expected:

- [ ] Colonies/Portfolio hierarchy/navigation;
- [ ] portfolio state owner;
- [ ] colony switching;
- [ ] inactive-colony simulation;
- [ ] local state capture/global date;
- [ ] cross-colony events;
- [ ] colony loss/portfolio game-over;
- [ ] complete device-validated multi-colony flow.

### Phase 8 — Ships, expansion and ship-market vertical slices

Build on the N05 durable fleet foundation rather than replacing it.

Expected:

- [ ] full fleet/founding-ship management screen;
- [ ] general cargo/passenger/accommodation UI/rules;
- [ ] docked/landed/travel state and presentation;
- [ ] factory-new catalogue/market/procurement;
- [ ] transport orders;
- [ ] star/system map/navigation;
- [ ] current Fuel/location/travel behaviour;
- [ ] deliberately approved future Fuel/wear/navigation additions when their roadmap work begins.

### Phase 9 — Remaining slices and UI consistency closure

Independent remaining slices may include Company/Corporation, Contract Board if still separate, Technology/Engineering, survival warnings, onboarding/help and remaining overlays/dialogs.

Final closure:

- [ ] screen-by-screen current-web parity matrix;
- [ ] full design consistency audit;
- [ ] no major accidental hierarchy/layout drift;
- [ ] deliberate changes documented/approved;
- [ ] landscape/large-screen sanity;
- [ ] final accessibility/navigation pass.

### Phase 10 — Production hardening and cutover

- [ ] representative real web-save imports;
- [ ] full deterministic soak;
- [ ] screenshot regression suite;
- [ ] key instrumentation tests;
- [ ] performance/memory/leak profiling;
- [ ] lifecycle recovery;
- [ ] production signing/Play upload key;
- [ ] R8 release verification;
- [ ] Universe snapshot validation + offline cold start;
- [ ] final feature matrix;
- [ ] hands-on approval;
- [ ] declare native implementation canonical.

## 19. N05 current-source establishment baseline

The active source baseline now requires the following first-colony semantics.

### 19.1 Fresh Contract 01 start

- 120 colonists begin aboard the founding ship;
- 10 crew remain assigned;
- minimum crew 10, maximum crew 40;
- accommodation capacity 290;
- starter ship manifest:
  - Food 1,300;
  - Fuel 675;
  - Build 520;
  - Ore 260;
- equivalent colony inventory begins at zero.

`population` remains total living colonists. Planetary residents are derived as total population minus ship-resident assignments.

### 19.2 Survival/network split

Residents aboard ship:

- eat ship Food;
- have ship-specific starvation/mortality progression;
- do not consume colony Food;
- do not count as planetary workforce;
- do not create planetary life-support Power demand.

The docked founding ship contributes 50 Industry that is crewed/self-powered and consumes neither planetary workforce nor planetary Power.

### 19.3 Establishment transfers

Ship and colony inventory are separate. Transfer preserves exact resource/quality bands.

The founding ship can bootstrap-unload initial supplies before powered Spaceport services exist. After bootstrap, normal transfer service gates apply.

Moving residents ashore is manual and requires:

- docked founding ship;
- real built planetary Housing with capacity;
- powered Spaceport transfer services;
- a real colony Power Plant;
- resident/Housing capacity.

The domain projects incremental life-support Power. A projected shortage warns and requires explicit confirmation rather than silently blocking or silently proceeding.

### 19.4 Establishment checklist

The canonical handover sequence is:

1. Deploy Build + Fuel;
2. Survey land;
3. Power + Housing;
4. Move residents ashore;
5. Establish Food;
6. Establish Fuel;
7. Replace ship Industry;
8. Establish Headquarters.

`BEGIN OPERATIONS • 1×` acknowledges establishment and begins time. It does not complete Headquarters command handover.

The authoritative HUD presents Ship (`S`) and Colony (`C`) values separately for Housing, Power, Industry, Workforce and Food/Build/Fuel/Ore.

Full ship market/travel/navigation remains Phase 8.

## 20. Cross-runtime and regression testing

Parity fixtures live under `app/src/test/resources/parity/` where applicable.

Important scenarios include:

- calendar conversion;
- deterministic terrain/discovery/survey;
- stable daily progression;
- beginning-of-day Fuel;
- Power shortages;
- depletion/renewables;
- starvation/mortality;
- HQ outage/recovery;
- building upgrades;
- trade transactions;
- buyer collections;
- save migrations;
- N05 fresh starter ownership;
- all-aboard survival;
- ship Food starvation;
- self-powered ship Industry;
- stock-conserving bootstrap unload;
- resident-transfer gates;
- existing-save N05 compatibility;
- later multi-colony/ship scenarios as those phases arrive.

Compare gameplay meaning, not object order, DOM shape or irrelevant floating formatting.

UI/layout parity is reviewed against current web hierarchy and representative device states/screenshots, not DOM equality.

### 20.1 Test migration map

| Web coverage | Native replacement |
|---|---|
| architecture/owner tests | package/dependency guards + review |
| domain tests | Kotlin/JUnit |
| save round-trip | serialization/migration/recovery fixtures |
| long simulation soak | deterministic JVM soak |
| browser interaction probes | Compose instrumentation |
| presentation regression | Compose screenshot tests + hierarchy review |
| viewport probes | device/configuration screenshot/instrumentation |
| lifecycle soak | ViewModel/navigation/lifecycle tests |
| canvas gesture probes | focused Compose gesture tests |

CI stays layered: fast JVM tests first, Android runtime/emulator tests only where needed.

## 21. Architecture improvements explicitly allowed

Allowed where gameplay meaning is preserved:

- split web `app.js` responsibilities into composition/session/clock/coordinators;
- constructor/narrow-interface dependencies instead of service back-patching;
- narrow ViewModel dependencies;
- explicit save migrations;
- durable/derived/transient separation;
- reproducible randomness;
- rendering/simulation decoupling;
- selected strong IDs/value types;
- generic resource identity/inventory;
- central resource/extractor compatibility;
- build-time Universe validation;
- consistent Compose design system;
- development-only reproducibility diagnostics;
- Android-native accessibility/back/haptics;
- low-risk UI refinements preserving purpose/hierarchy/usability;
- narrow permanent fleet architecture required by current N05 source semantics.

KISS/YAGNI remains mandatory.

## 22. Bugs and intentional divergence

### Clear defect

1. identify root cause;
2. add/strengthen regression coverage;
3. preferably correct the maintained web canonical owner first where shared;
4. port corrected behaviour;
5. log a difference only if source cannot reasonably be corrected.

### Platform/architecture debt

Fix browser-only debt natively without changing gameplay semantics. Equivalent behaviour is not an intentional divergence.

### Gameplay/design change

Material rule/balance/progression/design changes go through discovery/backlog unless explicitly approved.

Small native UI refinements preserving flow/hierarchy do not require a divergence entry. Material workflow/decision/information-priority differences do.

## 23. Branch and delivery workflow

For each remaining migration slice:

1. branch from current accepted `main` unless continuing an unaccepted phase;
2. read Android `AGENTS.md` in full;
3. read this guide and relevant migration records;
4. read source MineIT `AGENTS.md`;
5. inspect current source view/HTML, CSS, UI/controller, domain owner and tests;
6. record current source commit/version;
7. decide preserve/refine/replace;
8. identify what native backend actually remains missing;
9. add parity/regression coverage first where valuable;
10. implement gameplay only in canonical domain/application owners;
11. implement Compose using shared MineIT primitives;
12. add interaction/presentation coverage where appropriate;
13. run focused tests + full Android CI;
14. produce signed-development APK artifact;
15. validate the complete slice hands-on;
16. update migration docs/status;
17. merge only the exact accepted head.

Avoid giant branches containing unrelated phases or several half-finished screens.

## 24. Definition of done for a migrated feature/screen

A feature/screen is migrated only when applicable items are true:

- [ ] source HTML/view, CSS and UI/controller inspected;
- [ ] source domain owner/tests inspected;
- [ ] preserve/refine/replace decision understood;
- [ ] native semantic owner clear;
- [ ] gameplay implemented without UI truth leakage;
- [ ] save/import/migration handled where needed;
- [ ] domain/parity regression coverage exists;
- [ ] important UI interaction/presentation coverage exists;
- [ ] source hierarchy/workflow remains recognisable unless deliberately changed;
- [ ] design-system conventions used;
- [ ] no duplicate temporary production implementation remains;
- [ ] intentional divergences recorded where required;
- [ ] exact validation head passes Android CI and signer verification;
- [ ] hands-on test completed where relevant;
- [ ] migration status updated.

## 25. Production cutover criteria

Do not retire web as behavioural/layout reference until:

1. Contract 01 starts, plays, saves, reloads and completes.
2. Survival/economy fixtures pass.
3. Power/Industry/workforce/HQ behaviour is covered.
4. Survey/map/development interaction is complete.
5. Trade/corporate event flows are complete.
6. Multiple colonies simulate/switch correctly.
7. Current ship/fleet/market behaviour is complete.
8. MineIT-Universe data/art is bundled, validated and offline.
9. Representative web v16 saves import correctly if continuity is required.
10. Deterministic long soak passes.
11. Lifecycle/process recreation cannot silently lose valid state.
12. Production signing is complete.
13. No required web-only player flow remains.
14. Screen-by-screen review has no major accidental hierarchy/layout divergence.
15. Deliberate native improvements are consistent and approved.
16. Hands-on approval is given on a production-like build.

## 26. Current migration status

### Accepted

- [x] Phase 0 baseline/parity harness;
- [x] Phase 1 state/session/save/recovery/import foundation;
- [x] Phase 2 resources/world/discovery/survey;
- [x] Phase 3 survival/daily simulation;
- [x] Phase 4 buildings/Power/Industry/HQ;
- [x] Phase 5 native map/UI/design foundation promoted to `main`.

### Active Phase 6

Phase 6 commercial backend and the subsequent N05 current-source refresh are implemented on `feature/migration-phase-6`.

Current code checkpoint `35a03b32a0f4360a6f50e1a1e870ef9fced3bdc4` passed Android CI run `33912720951` / run `275`, including JVM tests, debug APK assembly, persistent development signer verification and artifact upload.

Artifact:

- ID `9952022146`;
- name `mineit-android-poc-debug`;
- digest `sha256:94f73c5e1b20e553f0b5c6db7945ca6ad0f566ffc8b4a12ad5488f540d86b04f`.

The branch is **not accepted yet**. The earlier `0.7.0-migration` physical-device review identified layout drift. The new `0.7.1-migration` build now also includes the source-compatible N05 founding handover and S/C HUD. These revised player-facing flows require hands-on device validation before Phase 6 can be accepted/promoted.

### Next milestone after Phase 6 acceptance

**Phase 7 — Portfolio and multi-colony vertical slices.**

Do not start Phase 7 from an unaccepted Phase 6 head.

## 27. Reference notes

At the current active migration baseline:

- web game: `5.13.22`;
- web save: `16`;
- source commit: `075b3d82fd88334b20b3cfe7d6e2731c8d840533`;
- calendar: 360 days/year;
- native save: v6;
- Android build: `0.7.1-migration` / version code 13;
- Android branch: `feature/migration-phase-6`;
- current implementation checkpoint: `35a03b32a0f4360a6f50e1a1e870ef9fced3bdc4`;
- current full CI: run 275 success;
- source resource catalogue remains 40 current definitions;
- Contract 01 still uses eight deterministic 8×8 landing candidates;
- Phase 3 survival/daily simulation is accepted;
- Phase 4 construction/Power/Spaceport/HQ is accepted;
- Phase 5 native map/design foundation is accepted/current `main`;
- Phase 6 commercial implementation is regression/build/signing green but awaiting revised device acceptance;
- N05 now makes the founding ship a real durable first-colony support owner; the broader fleet/market/travel system remains Phase 8;
- resource-economy overhaul remains separate from parity migration;
- web views/CSS/UI controllers are first-class migration references alongside domain owners/tests;
- standard activation remains one semantic click/tap path, with pointer handling reserved for gestures.

Update this guide whenever the active source baseline materially changes, a migration decision changes, or a phase completes.

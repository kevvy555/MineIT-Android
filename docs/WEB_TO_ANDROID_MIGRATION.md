# MineIT Web-to-Native Android Migration Guide

**Status:** Living migration plan — Phase 0 complete; Phase 1 complete; Phase 2 implementation complete and closing validation  
**Source application:** `kevvy555/MineIT` (`develop`)  
**Source behavioural baseline:** commit `9e58983adaa7a15cd525451266ce9df3c17ae886`  
**Source game version:** `5.13.15`  
**Source save version:** `16`  
**Target application:** `kevvy555/MineIT-Android`  
**Current Android migration build:** `0.3.0-migration` / version code `6`  
**Canonical shared universe:** `kevvy555/MineIT-Universe`

## 1. Purpose

This document is the authoritative guide for migrating MineIT from vanilla HTML/CSS/modular JavaScript to a fully native Android application built with Kotlin and Jetpack Compose.

The migration is **not** a line-for-line rewrite. The current web implementation remains the behavioural reference while systems are moved, but the rewrite is also a one-time opportunity to establish cleaner long-term architecture.

The governing rule is:

> **Preserve gameplay by default. Improve architecture where the improvement does not materially change gameplay.**

Clear defects may be corrected with regression coverage. Material balancing, progression or economy redesigns remain separate approved work.

The target is a native, offline-first Android game with no WebView and no mandatory backend/server dependency.

## 2. Supporting migration records and decisions

The following are part of the migration contract:

- [`migration/PHASE_0_BASELINE.md`](./migration/PHASE_0_BASELINE.md) — frozen source baseline, source-owner inventory, screen/flow inventory and parity-fixture convention;
- [`migration/PHASE_1_FOUNDATION.md`](./migration/PHASE_1_FOUNDATION.md) — permanent native state/persistence foundation;
- [`migration/PHASE_2_CORE_WORLD.md`](./migration/PHASE_2_CORE_WORLD.md) — current Contract 01 data, resource catalogue, deterministic world/discovery/survey implementation;
- [`migration/INTENTIONAL_DIVERGENCES.md`](./migration/INTENTIONAL_DIVERGENCES.md) — required log for deliberate semantic differences;
- [`migration/RESOURCE_ARCHITECTURE_DIRECTION.md`](./migration/RESOURCE_ARCHITECTURE_DIRECTION.md) — resource architecture improvements allowed during migration without implementing the resource overhaul;
- [`migration/DESIGN_SYSTEM_DIRECTION.md`](./migration/DESIGN_SYSTEM_DIRECTION.md) — MineIT-wide visual/interaction consistency workstream;
- [`migration/UNIVERSE_BUNDLING_DIRECTION.md`](./migration/UNIVERSE_BUNDLING_DIRECTION.md) — build-time MineIT-Universe pinning, validation and offline bundling.

Repository state and these documents are the recovery source; do not rely on chat history alone.

## 3. Migration goals

The migration is successful when:

1. MineIT runs fully natively with Kotlin and Jetpack Compose.
2. Existing gameplay behaviour is preserved unless a difference is deliberately approved and recorded.
3. One authoritative native state owner controls gameplay state.
4. UI renders state and dispatches intent; it never owns gameplay truth.
5. Simulation is independent of rendering/frame rate and can be run deterministically in JVM tests.
6. Save data is versioned, atomic, recoverable and migration-tested.
7. Existing web saves can be imported where practical.
8. MineIT-Universe remains canonical and required data/art is bundled into builds for offline play.
9. The migration produces a consistent MineIT design language without replacing successful core UX concepts merely for novelty.
10. HTML/CSS mocks remain available as rapid visual prototypes; approved production UI is Compose.
11. Android CI proves domain/parity tests, save migration, build validity and signing consistency.
12. The web game remains available as the behavioural reference until native parity is accepted.
13. After cutover, no permanent duplicate game engines are maintained unless a separate supported web edition is deliberately chosen.

## 4. Explicit non-goals

The migration must not become an uncontrolled redesign.

Do **not** automatically:

- redesign gameplay simply because code is moving;
- perform the full resource-economy overhaul;
- define the future refined/manufactured resource catalogue;
- rebalance prices, yields, Fuel, nutrition or progression;
- preserve known bugs merely for parity;
- recreate browser concepts where Android has a cleaner equivalent;
- introduce a mandatory backend/login;
- keep a WebView as a compatibility architecture;
- split into many Gradle modules without measurable benefit;
- preserve POC/demo models as production contracts;
- create speculative frameworks or future systems that current migration work does not require.

Gameplay/design changes discovered during migration follow the MineIT backlog/discovery process unless the user explicitly approves them or they are clear defect corrections.

## 5. Source architecture being migrated

The web application already has valuable separation and completed significant cleanup work. Preserve the successful ownership model rather than reverting to monolithic native code.

### 5.1 Current web ownership

- `js/core/` — infrastructure/utilities and `GameStore`;
- `js/data/` — static definitions/configuration/data;
- `js/domain/` — authoritative gameplay services and state behaviour;
- `js/persistence/` — save/development-task persistence;
- `js/ui/` — presentation/controllers;
- `views/` — reusable HTML fragments/templates;
- `css/` — presentation styling;
- `tests/` — architecture, unit, regression, simulation and browser probes.

`GameStore` owns the mutable root state. Domain services own gameplay decisions. UI renders and dispatches.

### 5.2 `app.js` is a migration opportunity

`js/app.js` combines dependency construction, state creation/normalisation, simulation scheduling, cross-colony orchestration, corporate-event sequencing, lifecycle saving, UI coordination, error handling and the browser animation loop.

Native code must not reproduce this as one giant manager/activity/application class.

### 5.3 Current persistence

Web saves are a single JSON root object in `localStorage`, currently schema version `16`.

Strengths to preserve:

- plain inspectable JSON;
- realistic round-trip tests;
- explicit state versioning;
- mature migration/normalisation knowledge.

Native improvements:

- atomic file writes;
- previous-known-good backup;
- explicit ordered migrations;
- validation/recovery diagnostics;
- clearer durable/derived/transient state separation.

### 5.4 Current simulation

`simulation-engine.js` is a useful canonical gameplay boundary for production, Power, Fuel, Industry, workforce, Food, mortality, depletion, accidents, engineering and related daily effects.

The browser schedules days using `requestAnimationFrame`; native separates scheduling from gameplay execution.

### 5.5 Current Universe integration

The web ship catalogue is runtime remote-first with a bundled fallback. Android changes that model: a pinned validated Universe snapshot becomes install-time content so core play does not require the network.

## 6. Target Android architecture

Keep one Gradle app module initially, with clear package boundaries:

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

Additional Gradle modules are introduced only when build time, ownership or reuse creates a measurable reason.

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

Data adapters implement domain/application contracts.
Android platform APIs stay outside pure domain code.
```

The domain layer must not depend on Compose, Activity, `Context`, lifecycle APIs, filesystem APIs or network clients.

## 7. Game-state ownership

### 7.1 `GameSession`

The native application-layer root owner is:

```text
GameSession
  owns StateFlow<GameState>
  serialises gameplay commands
  invokes domain behaviour
  commits authoritative next state
  coordinates persistence after committed changes
```

Compose observes state through ViewModels. UI never mutates root state directly.

### 7.2 Immutable boundaries

- state exposed to UI is immutable;
- domain operations return explicit results/next state;
- controlled internal mutation may be used later for measured simulation performance, but never leaks as public mutable state.

### 7.3 Durable vs derived vs transient

Persist durable gameplay truth and historical state required by future simulation. Recompute derived state where safe. Keep presentation/session state such as selection/camera outside the durable root unless a real gameplay requirement proves otherwise.

Do not remove imported web fields from a real migration path until corresponding native features exist and parity tests prove the decision.

### 7.4 Strong types where they prevent real bugs

Use typed IDs/value objects selectively for important concepts such as `ColonyId`, `ShipId`, `ResourceId`, `AbsoluteDay` / `GameDate`, quality/ranged factors and units where accidental mixing would be a realistic defect.

Do not wrap every primitive for architectural appearance. Prefer enums/sealed types for genuine finite state machines.

### 7.5 Canonical calendar

The pinned web source uses **360 days per game year**. Native `GameDate` must preserve this. Phase 2 corrected an early Phase 1 365-day implementation mistake before daily simulation was migrated.

## 8. Deterministic simulation and time

### 8.1 `SimulationClock`

Use an application-layer coroutine-backed clock that:

- maps game speed to day cadence;
- pauses at speed zero;
- pauses for blocking events where required;
- asks `GameSession` to advance a day;
- owns no gameplay rules;
- is lifecycle-aware without making gameplay correctness depend on frames/recomposition.

### 8.2 Rendering never drives gameplay

Compose recomposition, screen refresh rate and animation must have no effect on simulation results.

A domain `advanceDay` operation must yield the same result when invoked by the clock, a unit test, a debug command or a long-running soak test.

### 8.3 Reproducible randomness

Native world generation already uses source-compatible deterministic hash/random primitives. Simulation randomness must use similarly explicit domain-owned state/seed inputs.

Goal:

```text
save + deterministic random state + actions = reproducible outcome
```

## 9. Persistence and web-save migration

### 9.1 Native save envelope

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

The aggregate state remains JSON/file-based unless a demonstrated independently queried data need justifies a database later.

### 9.2 Storage

`FileGameStatePersistence` stores in app-private storage, writes/syncs/validates a temporary file, promotes it atomically where supported, preserves the previous validated active save as backup and provides explicit recovery diagnostics.

Use DataStore or equivalent only for small preferences, not the complete game state.

### 9.3 Web v16 import

Keep JavaScript compatibility out of canonical native models:

```text
Web JSON
  ↓
WebSaveV16 importer/DTO boundary
  ↓
validation
  ↓
explicit adapter/migration as native owners exist
  ↓
current native GameState
  ↓
normal native SaveEnvelope
```

The importer grows only as corresponding native semantic owners are migrated. It must not invent incomplete mappings for systems not yet ported.

### 9.4 Explicit migration chain

Avoid recreating one indefinitely growing normaliser.

```text
web v16 → native current
native v1 → native v2
native v2 → native v3 ...
```

Every real native migration gets fixture coverage. Phase 2 introduced the first production native migration, v1→v2.

## 10. MineIT-Universe build-time bundling

Authoritative detail: [`migration/UNIVERSE_BUNDLING_DIRECTION.md`](./migration/UNIVERSE_BUNDLING_DIRECTION.md).

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

Required canonical data and required gameplay artwork must be locally available after installation.

CI/build validation should catch MineIT-relevant invalid snapshots such as unsupported schema, malformed data, duplicate IDs, missing references and missing required `image.key` assets.

Android stores a generated/pinned snapshot, not a second manually edited canonical Universe. If artwork later becomes too large for straightforward packaging, prefer install-time asset delivery that retains offline play rather than network-only core art.

## 11. Resource architecture during migration

Authoritative detail: [`migration/RESOURCE_ARCHITECTURE_DIRECTION.md`](./migration/RESOURCE_ARCHITECTURE_DIRECTION.md).

### 11.1 Decision

**Improve resource architecture now; defer the resource overhaul itself.**

### 11.2 Permanent model direction

The native model supports/should support:

- stable `ResourceId` identity independent of display names;
- generic resource quantity and quality/quality-band storage;
- resource definitions/metadata;
- current Food/Build/Fuel/Ore categories without making those four categories the permanent limits of the architecture;
- one authoritative resource/extractor compatibility source;
- explicit compatibility queries rather than inferring every rule from a broad category;
- future raw/refined/manufactured classifications;
- future recipes/processing without building them now;
- future generator/ship-fuel compatibility without changing it now;
- data-driven presentation metadata.

Phase 2 implements the current 40-resource catalogue and a single `ExtractionCompatibility` owner while preserving current behaviour.

### 11.3 Deferred resource work

Do not implement during parity migration unless separately approved:

- final resource renames/merges/removals;
- new planetary distribution;
- new extraction buildings/rules;
- refined-material catalogue;
- manufacturing product catalogue;
- production-order/recipe gameplay;
- generator fuel redesign;
- Propellant/Fusion Fuel redesign;
- nutrition/energy/quality rebalance;
- buyer-market redesign for processed goods;
- economy/progression rebalance.

## 12. MineIT design consistency workstream

Authoritative detail: [`migration/DESIGN_SYSTEM_DIRECTION.md`](./migration/DESIGN_SYSTEM_DIRECTION.md).

> **Preserve successful MineIT UX concepts; standardise the language used to express them.**

The map-first structure, compact operational presentation and existing MineIT identity remain. Do not redesign every screen from scratch.

### 12.1 HTML/CSS mocks remain valid

```text
idea
 → HTML/CSS visual mock when useful
 → visual approval
 → Compose using MineIT design primitives
 → Preview/screenshot test
 → APK hands-on check where valuable
```

HTML is a visual specification, never a WebView production implementation.

### 12.2 Native design system

Standardise semantic tokens for:

- colours/states;
- typography;
- spacing;
- radii/borders/elevation;
- icon/image sizing;
- progress/selection/warning treatment;
- touch targets;
- animation timing;
- subtle haptic intent.

Create reusable composables when genuine repetition appears: panels, section headers, resource cards, stat rows, buttons, status badges, progress, list rows, dialogs/sheets, building/ship cards and common empty/error states.

### 12.3 Consistency audit

Normalise repeated concepts across features: resource quantities/quality, money/costs, capacity, Power/workforce/Industry, building levels/actions, ship capacities/status, contracts/buyers, warnings, progress, selection, numbers/dates/percentages and navigation/back behaviour.

## 13. Native interaction, accessibility and lifecycle

- normal taps use Compose semantic click handling;
- pointer input is reserved for real gestures such as drag/long-press/multi-select;
- do not recreate competing pointer-up/tap activation paths;
- Android Back behaves consistently through navigation/dialogs;
- touch targets and accessibility semantics are requirements;
- scoped coroutines/ViewModels reject stale asynchronous writes;
- haptics are sparse and consistent;
- lifecycle persistence is explicit and testable.

## 14. Development diagnostics

Build a development-only diagnostics surface as migration progresses. Useful information/actions include:

- app/game version;
- native save format version;
- Universe content/schema/source commit;
- current absolute day;
- deterministic random seed/state;
- active colony;
- pending corporate events;
- key Power/workforce/Industry summaries;
- save/backup health;
- import/export save;
- copy diagnostics;
- deterministic advance-day controls for tests.

Debug-only actions stay out of release UX unless deliberately promoted.

## 15. Web-to-Android ownership map

| Current web owner | Native target | Rule |
|---|---|---|
| `js/core/config.js` | `domain/config` | typed configuration/value objects |
| `js/core/game-store.js` | `app/GameSession` | immutable StateFlow boundary |
| `js/domain/game-state*.js` | `domain/model` + save migrations | separate current schema from legacy import |
| `js/domain/simulation-engine.js` | `domain/simulation` | parity first, optimise later |
| `js/domain/*-service.js` | matching feature packages | preserve semantic ownership |
| `js/data/*.js` | typed definitions/bundled adapters | static data outside UI |
| `js/persistence/save-repository.js` | `data/save` | atomic files + backup + migrations |
| `js/app.js` | composition/session/clock/coordinators | split responsibilities |
| `js/ui/*.js` | feature ViewModels/composables | render state + emit intent |
| `views/*.html` | composables | translate layout, not DOM mechanics |
| `css/*` | design tokens/modifiers/components | consolidate visual rules |
| map/star canvas | Compose Canvas/custom drawing | native input/rendering |
| browser lifecycle | coroutine/ViewModel/Compose lifecycle | scoped ownership |
| Node/browser tests | JUnit/Compose/screenshot/instrumentation | preserve behavioural intent |

## 16. Domain migration inventory

### Core state/simulation

- [x] canonical root state foundation;
- [x] current native save migration chain;
- [x] root-state ownership;
- [x] canonical game date/absolute day;
- [x] deterministic world RNG primitives;
- [ ] full web-v16 conversion;
- [ ] daily simulation engine;
- [ ] speed/pause/`SimulationClock`;
- [ ] deterministic simulation RNG state;
- [ ] game log/telemetry.

### Resources/colony/infrastructure

- [x] current resource definitions;
- [x] generic inventory + quality bands;
- [x] canonical resource/extractor compatibility owner;
- [ ] collection/depletion/renewables daily behaviour;
- [ ] colony demand/workforce/Industry;
- [ ] site development;
- [ ] buildings/Spaceport;
- [ ] Power network;
- [ ] Headquarters command/outage/recovery;
- [ ] current Food/Fuel/Ore/Build simulation behaviour.

### World/survey

- [x] deterministic landing-site/world terrain generation;
- [x] persistent tile/world state model;
- [x] deterministic resource discovery;
- [x] scanning/resurvey history rules;
- [x] survey queue/timing domain rules;
- [ ] production native map presentation;
- [ ] camera/focus/filters;
- [ ] tap/inspect/long-press/multi-select production UI.

### Technology/progression

- [x] technology level state foundation;
- [x] Contract 01 required technology values;
- [ ] technology definitions/service;
- [ ] company vs local engineering behaviour;
- [ ] engineering deployments;
- [ ] upgrade eligibility;
- [ ] progression visibility/unlocks.

### Contracts/corporation

- [x] Contract 01 canonical data/start-state foundation;
- [ ] full contract service;
- [ ] portfolio/multi-colony lifecycle;
- [ ] corporate event sequencing;
- [ ] colony creation/switch/removal/relocation;
- [ ] deadlines/renewal/holdover/end states;
- [ ] game-over;
- [ ] reputation/cash/asset behaviour beyond foundation.

### Trade/buyers

- [ ] trade service;
- [ ] buyer service/market definitions;
- [ ] corporate ship events;
- [ ] import/export capacity/reserves;
- [ ] offers/contracts/collections;
- [ ] relationship/miss/termination behaviour.

### Ships/expansion

- [ ] expansion service;
- [ ] ship market;
- [ ] transport;
- [ ] founding ship;
- [ ] passengers/accommodation;
- [ ] cargo/current Fuel behaviour;
- [ ] procurement;
- [ ] location/travel state;
- [ ] star/system/planet navigation;
- [ ] later approved Fuel/wear/navigation additions at their deliberate roadmap point.

### Shared Universe

- [ ] snapshot sync/validation;
- [ ] ship classes/runtime profiles;
- [ ] organisations/manufacturers used by MineIT;
- [ ] art/image-key mapping;
- [ ] provenance reporting;
- [ ] offline load validation.

## 17. Migration strategy — vertical slices, not big bang

Each phase ends with tests and a usable/inspectable result. Do not carry several half-ported engines in production simultaneously.

### Phase 0 — Baseline and migration harness — COMPLETE

Goal: freeze a measurable reference point and make parity executable.

- [x] source baseline recorded;
- [x] domain owners/player flows inventoried;
- [x] web CI retained as behavioural reference;
- [x] parity-fixture structure created;
- [x] representative web-v16 multi-colony/state fixture added;
- [x] JVM fixture loader/test added;
- [x] intentional-divergence log created;
- [x] throwaway POC domain isolated under `domain/poc` / `Poc*` names;
- [x] Phase 0 CI green.

### Phase 1 — Native state and persistence foundation — COMPLETE

Goal: production-quality root ownership before gameplay porting.

- [x] canonical `GameState` and selected ID/value types;
- [x] generic resource identity/quantity/quality foundation;
- [x] `GameSession`;
- [x] native `SaveEnvelope`;
- [x] atomic active-save + previous-good backup repository;
- [x] round-trip/recovery tests;
- [x] isolated web-v16 importer boundary;
- [x] explicit native migration framework;
- [x] transient-vs-durable state separation begun;
- [x] save/schema/source diagnostics;
- [x] `0.2.0-migration` / version code 5 signed update build.

Detailed record: [`migration/PHASE_1_FOUNDATION.md`](./migration/PHASE_1_FOUNDATION.md).

### Phase 2 — Core data, current resources and deterministic world — IMPLEMENTED / FINAL VALIDATION

Goal: replace illustrative domain data with real Contract 01 definitions and deterministic source-compatible world/survey rules while preserving the current resource economy.

- [x] typed current configuration subset;
- [x] source-canonical 360-day calendar;
- [x] all 40 current resource definitions using stable IDs;
- [x] current quality-band architecture;
- [x] current Contract 01 starter data;
- [x] current starter inventory;
- [x] JS-compatible deterministic hash/random primitives;
- [x] deterministic eight-candidate 8×8 landing-site generation;
- [x] deterministic surface/deep discovery;
- [x] survey/resurvey queue/timing rules;
- [x] one native extraction-compatibility owner;
- [x] parity golden fixtures;
- [x] native save v1→v2 migration and regression;
- [x] web-v16 importer preview extended for world/survey metadata;
- [ ] final documented branch head CI/signing/artifact validation.

The existing POC Compose screen remains a temporary smoke-test presentation; the production world is canonical in the domain, not in the POC UI. Header wording now identifies these as `Android Migration` builds.

Detailed record: [`migration/PHASE_2_CORE_WORLD.md`](./migration/PHASE_2_CORE_WORLD.md).

### Phase 3 — Colony survival and daily simulation

Goal: run real MineIT days against the permanent Phase 2 state/world.

- [ ] port collection/extraction daily behaviour;
- [ ] port colony network calculations;
- [ ] current resource demand/consumption;
- [ ] Food/Power/Fuel survival;
- [ ] mortality/colony death;
- [ ] production/operating costs;
- [ ] coroutine `SimulationClock`;
- [ ] deterministic simulation RNG path;
- [ ] long-running parity/soak fixtures.

Exit gate: Android day progression matches representative Contract 01 survival/economy fixtures.

### Phase 4 — Buildings, sites, Industry and Headquarters

- [ ] placement/demolition;
- [ ] extraction development/upgrades;
- [ ] housing/Industry;
- [ ] Spaceport;
- [ ] Power priority/allocation;
- [ ] Headquarters command capacity/load;
- [ ] first-departure gate;
- [ ] HQ outage/degradation/recovery;
- [ ] temporary ship command takeover;
- [ ] current construction/resource costs.

Exit gate: Headquarters/Power regression scenarios pass natively.

### Phase 5 — Main native game UI/map and design-system foundation

- [ ] production MineIT design tokens;
- [ ] first reusable native primitives;
- [ ] consistent resource/status header;
- [ ] real 8×8 map tiles/art;
- [ ] inspect/select/survey;
- [ ] hold/drag multi-select;
- [ ] queue/filter/focus presentation;
- [ ] building/site panels;
- [ ] colony/Power detail;
- [ ] responsive phone layout;
- [ ] previews/screenshot baselines;
- [ ] accessibility/back/haptic conventions;
- [ ] remove superseded `domain/poc` and POC UI source once production UI fully replaces it.

Exit gate: core first-colony gameplay is hands-on playable without POC/web UI and key concepts use one design language.

### Phase 6 — Trade, contracts and commercial events

- [ ] trade ship arrival/departure;
- [ ] buy/sell;
- [ ] capacity/reserves;
- [ ] contract goals/deadlines/decisions;
- [ ] corporate event queue;
- [ ] event pause/resume;
- [ ] buyer offers/contracts/collections;
- [ ] game log presentation.

Exit gate: Contract 01 commercial progression/resolution works natively.

### Phase 7 — Portfolio and multi-colony

- [ ] portfolio state;
- [ ] switching;
- [ ] inactive-colony simulation;
- [ ] local state capture;
- [ ] global date;
- [ ] cross-colony events;
- [ ] colony loss/game-over;
- [ ] multi-colony UI.

Exit gate: representative multi-colony fixtures round-trip and simulate correctly.

### Phase 8 — Ships, expansion and ship market

- [ ] fleet/founding ship;
- [ ] cargo/passengers/accommodation;
- [ ] docked/landed/travel state;
- [ ] factory-new catalogue/procurement;
- [ ] transport orders;
- [ ] star/system map;
- [ ] current navigation;
- [ ] integrate newly approved future ship systems only when their roadmap work is deliberately taken on.

Exit gate: current ship/fleet/market regression scenarios pass natively.

### Phase 9 — Remaining UI parity and consistency closure

- [ ] company/corporation;
- [ ] contract board;
- [ ] technology/engineering;
- [ ] buyers;
- [ ] survival warnings;
- [ ] trade/reserves;
- [ ] ship controls;
- [ ] overlays/toasts/dialogs;
- [ ] onboarding/help;
- [ ] lost-colony/game-over;
- [ ] full consistency audit;
- [ ] landscape/large-screen sanity.

Exit gate: no required web-only player flow and no major known visual/interaction inconsistency remains.

### Phase 10 — Production hardening and cutover

- [ ] representative real web-save imports;
- [ ] full deterministic soak;
- [ ] screenshot regression suite;
- [ ] key instrumentation tests;
- [ ] performance profiling;
- [ ] memory/leak checks;
- [ ] lifecycle save/recovery;
- [ ] production signing/Play upload key;
- [ ] R8 release verification;
- [ ] Universe snapshot validation + offline cold start;
- [ ] final feature matrix;
- [ ] hands-on approval;
- [ ] declare native implementation canonical.

## 18. Cross-runtime parity testing

Fixtures live under `app/src/test/resources/parity/`.

Each important parity scenario records:

- exact source commit/game/save version;
- starting state/input;
- action/day sequence;
- deterministic random state where relevant;
- expected canonical summary/important durable fields.

Examples include:

- calendar conversion;
- deterministic world terrain;
- surface/deep discovery;
- survey/resurvey;
- stable day advance;
- Power shortage;
- beginning-of-day Fuel use;
- depletion/renewable events;
- starvation progression;
- HQ outage/recovery;
- building upgrade;
- trade transaction;
- buyer collection;
- multi-colony day;
- ship cargo/passengers;
- web v16 import.

Compare gameplay meaning, not object ordering, DOM shape or irrelevant floating-point formatting.

## 19. Test migration map

| Web coverage | Native replacement |
|---|---|
| architecture/owner tests | package/dependency guards + repository contract/review |
| domain tests | Kotlin/JUnit domain tests |
| save round-trip | serialization/migration/recovery fixtures |
| long simulation soak | deterministic JVM soak tests |
| browser interaction probes | Compose instrumentation |
| presentation regression | Compose screenshot tests |
| mobile viewport probes | device/configuration screenshot/instrumentation matrix |
| lifecycle soak | ViewModel/navigation/lifecycle tests |
| canvas gesture probes | Compose focused gesture tests |

Keep CI layered: fast JVM tests first, emulator work only where Android runtime is necessary.

## 20. Architecture improvements explicitly allowed during the port

These preserve intended gameplay unless separately logged:

- split `app.js` responsibilities into composition/session/clock/coordinators;
- replace service back-patching with constructor/narrow-interface dependencies;
- narrow feature ViewModel dependencies;
- explicit save migrations;
- durable/derived/transient state separation;
- reproducible randomness;
- rendering/simulation decoupling;
- selected strong IDs/value types;
- generic/stable resource identity/inventory architecture;
- centralised resource/extractor compatibility ownership;
- build-time Universe snapshot validation;
- consistent Compose design system;
- development-only reproducibility diagnostics;
- Android-native accessibility/back/haptic behaviour.

KISS/YAGNI still applies: do not implement abstractions without a present migration or known-roadmap need.

## 21. Rules for bugs and intentional divergence

### Clear defect

1. identify root cause;
2. add/strengthen regression coverage;
3. preferably correct the web canonical owner first while it remains maintained when the defect is shared;
4. port corrected behaviour;
5. log the difference if the web source cannot reasonably be corrected.

### Platform/architecture debt

Fix browser-only implementation debt natively without changing gameplay semantics. No divergence entry is needed when behaviour is equivalent.

### Gameplay/design change

Route material rule/balance/progression changes through discovery/backlog unless explicitly approved as migration work.

All deliberate semantic differences are recorded in [`migration/INTENTIONAL_DIVERGENCES.md`](./migration/INTENTIONAL_DIVERGENCES.md).

The Phase 2 calendar change from 365 to 360 is a correction of an accidental native parity defect and therefore is **not** an intentional divergence.

## 22. Branch/delivery workflow

For each migration slice:

1. branch from current Android `main`;
2. read Android `AGENTS.md` and this guide;
3. read relevant `docs/migration/` decision/status files;
4. read source MineIT `AGENTS.md` and relevant source/tests;
5. record the exact source baseline;
6. add parity/regression fixtures first where valuable;
7. implement one canonical native owner;
8. migrate UI after/when the domain capability exists;
9. run focused tests;
10. run full Android CI;
11. produce an APK for hands-on checks when useful;
12. update migration docs/status;
13. merge the exact validated head after acceptance/phase completion.

Avoid giant migration branches containing unrelated phases.

## 23. Definition of done for a migrated feature

A feature is migrated only when applicable items are true:

- [ ] source owner/tests inspected;
- [ ] native semantic owner clear;
- [ ] gameplay implemented without UI ownership leakage;
- [ ] save/import/migration handled where needed;
- [ ] parity/domain regression coverage exists;
- [ ] important UI interaction coverage exists when applicable;
- [ ] design-system conventions used rather than isolated styling when production UI is involved;
- [ ] no duplicate temporary production implementation remains;
- [ ] intentional divergences recorded;
- [ ] Android CI green;
- [ ] hands-on test possible where relevant;
- [ ] migration checklist/status updated.

## 24. Production cutover criteria

Do not retire the web implementation as behavioural reference until:

1. Contract 01 starts, plays, saves, reloads and completes.
2. Survival/economy parity fixtures pass.
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
14. Consistency audit has no major unresolved visual/interaction split.
15. Hands-on approval is given on a production-like build.

## 25. Current migration status

### Proven by the initial native POC/pipeline

- [x] Kotlin/Compose project builds in CI;
- [x] native UI runs on physical Android hardware;
- [x] UI/domain separation is viable;
- [x] pure Kotlin domain work is JVM-testable;
- [x] CI produces a real APK;
- [x] build metadata is visible in-app;
- [x] persistent development signing is established;
- [x] successive APK updates install in place.

### Completed foundations

- [x] Phase 0 baseline/parity harness;
- [x] Phase 1 canonical state/`GameSession`/save/recovery/import foundation;
- [x] Phase 2 Contract 01/current resource/deterministic world/discovery/survey implementation;
- [x] resource overhaul remains deliberately deferred;
- [x] design consistency and Universe bundling directions remain active migration workstreams.

### Next milestone after Phase 2 validation

**Phase 3 — colony survival and daily simulation.**

Phase 3 should port current collection, resource demand/consumption, colony network calculations, Food/Power/Fuel survival, mortality/colony death, operating costs and a lifecycle-safe `SimulationClock`, all against the real deterministic Phase 2 state rather than the temporary POC model.

## 26. Reference notes

At the current migration baseline:

- web game version: `5.13.15`;
- web save version: `16`;
- source commit: `9e58983adaa7a15cd525451266ce9df3c17ae886`;
- canonical calendar: 360 days/year;
- current native save format: v2;
- current Android build: `0.3.0-migration` / version code 6;
- current source resource catalogue: 40 definitions;
- current Contract 01 uses eight deterministic 8×8 landing-site candidates;
- realistic web-save coverage includes multi-colony state, player-ship cargo/passengers/Fuel, engineering deployments, scan history and quality bands;
- current web simulation includes Headquarters/Power behaviour implemented before the migration baseline;
- the future resource-economy discovery remains separate from parity migration but informs permanent model shape;
- the August 2026 cleanup removed versioned production JS/CSS, import-map debt, application globals/document app-event debt and large embedded HTML-template debt;
- standard activation remains one normal click/tap path with pointer handling reserved for gestures.

Update this guide whenever the source baseline materially changes, a migration decision changes, or a phase completes.

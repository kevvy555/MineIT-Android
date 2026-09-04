# MineIT Web-to-Native Android Migration Guide

**Status:** Living migration plan — Phase 0 active  
**Source application:** `kevvy555/MineIT` (`develop`)  
**Source baseline:** commit `9e58983adaa7a15cd525451266ce9df3c17ae886`  
**Source game version:** `5.13.15`  
**Source save version:** `16`  
**Target application:** `kevvy555/MineIT-Android`  
**Initial native proof of concept:** `0.1.3-poc` / version code `4`  
**Canonical shared universe:** `kevvy555/MineIT-Universe`

## 1. Purpose

This document is the authoritative guide for migrating MineIT from vanilla HTML/CSS/modular JavaScript to a fully native Android application built with Kotlin and Jetpack Compose.

The migration is **not** a line-for-line rewrite. The current web implementation remains the behavioural reference while systems are moved, but the rewrite is also a one-time opportunity to establish cleaner long-term architecture.

The governing rule is:

> **Preserve gameplay by default. Improve architecture where the improvement does not materially change gameplay.**

Clear defects may be corrected with regression coverage. Material balancing, progression or economy redesigns remain separate approved work.

The target is a native, offline-first Android game with no WebView and no mandatory backend/server dependency.

## 2. Supporting migration decisions

Detailed decisions that form part of this plan live in:

- [`migration/PHASE_0_BASELINE.md`](./migration/PHASE_0_BASELINE.md) — exact source baseline, source-owner inventory, screen/flow inventory and parity-fixture contract;
- [`migration/INTENTIONAL_DIVERGENCES.md`](./migration/INTENTIONAL_DIVERGENCES.md) — required log for deliberate semantic differences;
- [`migration/RESOURCE_ARCHITECTURE_DIRECTION.md`](./migration/RESOURCE_ARCHITECTURE_DIRECTION.md) — resource architecture improvements allowed during migration without implementing the resource overhaul;
- [`migration/DESIGN_SYSTEM_DIRECTION.md`](./migration/DESIGN_SYSTEM_DIRECTION.md) — MineIT-wide visual/interaction consistency workstream;
- [`migration/UNIVERSE_BUNDLING_DIRECTION.md`](./migration/UNIVERSE_BUNDLING_DIRECTION.md) — build-time MineIT-Universe pinning, validation and offline bundling.

These are part of the migration contract, not optional notes.

## 3. Migration goals

The migration is successful when:

1. MineIT runs fully natively with Kotlin and Jetpack Compose.
2. Existing gameplay behaviour is preserved unless a difference is deliberately approved/recorded.
3. One authoritative native state owner controls gameplay state.
4. UI renders state and dispatches intent; it never owns gameplay truth.
5. Simulation is independent of rendering/frame rate and can be run deterministically in JVM tests.
6. Save data is versioned, atomic, recoverable and migration-tested.
7. Existing web saves can be imported where practical.
8. MineIT-Universe remains canonical and required data/art is bundled into builds for offline play.
9. The migration produces a consistent MineIT design language across features without replacing the successful core UX concepts.
10. HTML/CSS mocks remain available as rapid visual prototypes; approved production UI is Compose.
11. Android CI proves domain/parity tests, save migration, build validity and signing consistency.
12. The web game remains available as the behaviour reference until native parity is accepted.
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
- `js/data/` — static definitions/configuration;
- `js/domain/` — authoritative gameplay services and state behaviour;
- `js/persistence/` — save/development-task persistence;
- `js/ui/` — presentation/controllers;
- `views/` — external HTML fragments/templates;
- `css/` — presentation styling;
- `tests/` — architecture, unit, regression, simulation and browser probes.

`GameStore` owns the mutable root state. Domain services own gameplay decisions. UI renders and dispatches.

### 5.2 `app.js` is a migration opportunity

`js/app.js` currently combines:

- dependency construction;
- state creation/normalisation;
- simulation scheduling;
- cross-colony orchestration;
- corporate-event sequencing;
- lifecycle saving;
- UI coordination;
- error handling;
- the browser animation loop.

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
- better durable/derived/transient-state separation.

### 5.4 Current simulation

`simulation-engine.js` is a useful canonical gameplay boundary for production, Power, Fuel, Industry, workforce, Food, mortality, depletion, accidents, engineering and related day effects.

The browser schedules days using `requestAnimationFrame`; native must separate scheduling from gameplay execution.

### 5.5 Current Universe integration

The web ship catalogue is runtime remote-first with a bundled fallback. Android changes that model: a pinned validated Universe snapshot becomes install-time content so core play does not require the network.

## 6. Target Android architecture

Keep a single Gradle app module initially, but use clear package boundaries:

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
│   ├── inventory/
│   ├── colony/
│   ├── world/
│   ├── surveying/
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

The native equivalent of the useful `GameStore` concept is an application-layer owner:

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

Every current persisted field should be classified as:

1. durable gameplay truth;
2. historical state needed by future simulation;
3. derived/recomputable state;
4. transient session/UI state.

Do not blindly persist derived network summaries forever. Do not remove imported fields until migration tests prove behaviour remains correct.

### 7.4 Strong types where they prevent real bugs

Use typed IDs/value objects selectively for important concepts such as:

- `ColonyId`;
- `ShipId`;
- `ResourceId`;
- `AbsoluteDay` / `GameDate`;
- quality/ranged factors where validation matters;
- money/quantities where mixing units is a realistic defect risk.

Do not wrap every primitive for architectural appearance.

Prefer enums/sealed types for finite state machines such as ship state, colony state and corporate-event kinds where this makes invalid states harder to express.

## 8. Deterministic simulation and time

### 8.1 `SimulationClock`

Use an application-layer coroutine-backed clock that:

- maps game speed to day cadence;
- pauses at speed zero;
- pauses for blocking events where required;
- asks `GameSession` to advance a day;
- owns no gameplay rules;
- is lifecycle-aware without making gameplay correctness depend on lifecycle/render frames.

### 8.2 Rendering never drives gameplay

Compose recomposition, screen refresh rate and animation must have no effect on simulation results.

A domain `advanceDay` operation must yield the same result when invoked by:

- the clock;
- a unit test;
- a debug command;
- a long-running soak test.

### 8.3 Reproducible randomness

Replace hidden/global randomness with a domain-owned random source whose seed/state is persisted where needed.

Goal:

```text
save + random state + actions = reproducible outcome
```

This is important both for parity migration and future bug reports.

## 9. Persistence and web-save migration

### 9.1 Native save envelope

Use `kotlinx.serialization` for production save models.

Recommended envelope:

```text
SaveEnvelope
- formatVersion
- gameVersion
- universeContentVersion
- universeSourceCommit
- savedAt
- state
```

The aggregate game state may remain JSON/file-based. Do not add Room merely because it is an Android database option. Introduce a database later only for a demonstrated independently queried data need.

### 9.2 Storage

Production `SaveRepository` should:

- store in app-private storage;
- write to a temporary file first;
- flush/close before promotion;
- atomically replace active save where supported;
- retain one previous known-good backup;
- validate before accepting a loaded save;
- provide actionable recovery diagnostics.

Use DataStore or equivalent only for small preferences, not the complete game state.

### 9.3 Web v16 import

Keep JavaScript compatibility out of canonical native models:

```text
Web JSON
  ↓
WebSaveV16 DTO/import representation
  ↓
validation
  ↓
explicit adapter/migration
  ↓
current native GameState
  ↓
normal native SaveEnvelope
```

If player continuity requires it, add explicit web export-to-file and Android import. Do not assume Android can access browser `localStorage` directly.

### 9.4 Explicit migration chain

Avoid recreating one indefinitely growing `normalizeState()`.

Prefer ordered migrations such as:

```text
web v16 → native v1
native v1 → native v2
native v2 → native v3
```

Every migration receives fixture coverage.

## 10. MineIT-Universe build-time bundling

The authoritative detail is in [`migration/UNIVERSE_BUNDLING_DIRECTION.md`](./migration/UNIVERSE_BUNDLING_DIRECTION.md).

Core rule:

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

CI/build validation should detect the MineIT-relevant classes of invalid snapshot: unsupported schema, malformed data, duplicate IDs, missing references and missing required `image.key` assets.

The Android repository stores a generated/pinned snapshot, not a second manually edited canonical Universe.

If artwork later becomes too large for straightforward packaging, prefer install-time asset delivery that retains offline play rather than network-only core art.

## 11. Resource architecture during migration

The authoritative boundary is in [`migration/RESOURCE_ARCHITECTURE_DIRECTION.md`](./migration/RESOURCE_ARCHITECTURE_DIRECTION.md), informed by the September 2026 resource-economy discovery.

### 11.1 Decision

**Improve the resource architecture now; defer the resource overhaul itself.**

The migration may make structural improvements that preserve current behaviour, but it must not define/rebalance the future resource economy as part of parity work.

### 11.2 Permanent model direction

The native model should naturally support:

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

### 11.3 What remains deferred

Do not implement during migration unless separately approved:

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

Current behaviour remains the parity reference even where the later discovery intends to replace it.

## 12. MineIT design consistency workstream

The authoritative detail is in [`migration/DESIGN_SYSTEM_DIRECTION.md`](./migration/DESIGN_SYSTEM_DIRECTION.md).

The migration is a deliberate opportunity to make the game feel designed as one product rather than a set of independently evolved screens.

### 12.1 Principle

> **Preserve successful MineIT UX concepts; standardise the language used to express them.**

The map-first structure, compact operational presentation and existing MineIT identity remain. We are not redesigning every screen from scratch.

### 12.2 HTML/CSS mocks remain part of the workflow

```text
idea
 → HTML/CSS visual mock when useful
 → visual approval
 → Compose using MineIT design primitives
 → Preview/screenshot test
 → APK hands-on check where valuable
```

HTML is a visual specification, never a WebView production implementation.

### 12.3 Design system

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

### 12.4 Consistency audit

When migrating screens, explicitly normalise repeated concepts such as resource quantities, money, capacity, Power/workforce/Industry state, building levels/actions, ship capacities/status, contract/buyer status, warnings, progress, selection and formatting.

## 13. Native interaction, accessibility and lifecycle

- normal taps use Compose semantic click handling;
- pointer input is for real gestures such as drag/long-press/multi-select;
- do not recreate competing pointer-up/tap activation paths;
- Android Back should behave consistently through navigation/dialogs;
- touch targets and accessibility semantics are requirements;
- use scoped coroutines/ViewModels to reject stale asynchronous writes;
- use haptics sparingly and consistently for meaningful actions;
- lifecycle persistence is explicit and testable.

## 14. Developer diagnostics opportunity

Create a development-only diagnostics surface as migration progresses. It should expose useful reproducibility information, not production cheats by accident.

Expected debug information/actions include, where implemented:

- app/game version;
- native save format version;
- Universe content/schema version and source commit;
- current absolute day;
- random seed/state;
- active colony;
- pending corporate events;
- key Power/workforce/Industry summaries;
- save/backup health;
- import/export save;
- copy diagnostics;
- deterministic advance-day controls for testing.

Keep debug-only actions out of release UX unless deliberately promoted to a supported feature.

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

Every current semantic owner must be accounted for before web retirement.

### Core state/simulation

- [ ] state creation/current schema;
- [ ] web v16 import/migrations;
- [ ] simulation engine;
- [ ] root-state ownership;
- [ ] game date/absolute day;
- [ ] speed/pause;
- [ ] deterministic RNG;
- [ ] game log/telemetry.

### Resources/colony/infrastructure

- [ ] resources and definitions;
- [ ] generic inventory + quality bands;
- [ ] collection/depletion/renewables;
- [ ] colony demand/workforce/Industry;
- [ ] site development;
- [ ] buildings/Spaceport;
- [ ] Power network;
- [ ] Headquarters command/outage/recovery;
- [ ] current Food/Fuel/Ore/Build behaviour.

### World/survey

- [ ] world/land generation;
- [ ] tile state;
- [ ] scanning/resurvey history;
- [ ] scan queue;
- [ ] camera/focus/filters;
- [ ] tap/inspect/long-press/multi-select.

### Technology/progression

- [ ] technology definitions/service;
- [ ] company vs local technology;
- [ ] engineering deployments;
- [ ] upgrade eligibility;
- [ ] progression visibility/unlocks.

### Contracts/corporation

- [ ] contract service;
- [ ] portfolio/multi-colony lifecycle;
- [ ] corporate event sequencing;
- [ ] colony creation/switch/removal/relocation;
- [ ] deadlines/renewal/holdover/end states;
- [ ] game-over;
- [ ] reputation/cash/asset behaviour already present.

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

### Phase 0 — Baseline and migration harness

**Goal:** freeze a measurable reference point and make parity executable.

Tasks:

- [x] record source web commit/version/save version;
- [x] inventory domain owners and important player flows;
- [x] explicitly retain current web CI as behavioural reference;
- [x] create parity-fixture structure in Android;
- [x] add the first representative web-v16 multi-colony/state fixture;
- [x] add a JVM fixture loader/test;
- [x] define the intentional-divergence log;
- [x] isolate the throwaway POC domain under explicit `domain/poc` / `Poc*` names;
- [ ] obtain final green Android CI for the Phase 0 head.

Exit gate: Android JVM tests load/validate representative web state without production gameplay being implemented yet.

### Phase 1 — Native state and persistence foundation

**Goal:** production-quality root ownership before gameplay porting.

- [ ] define canonical `GameState` and selected value/ID types;
- [ ] establish resource-safe generic identity/quantity/quality foundations without changing the catalogue;
- [ ] implement `GameSession`;
- [ ] implement `SaveEnvelope`;
- [ ] implement atomic active-save + backup repository;
- [ ] add round-trip/recovery tests;
- [ ] implement web-v16 importer skeleton;
- [ ] implement explicit native migration chain;
- [ ] separate proven transient preferences from durable gameplay state;
- [ ] add initial development diagnostics for save/schema/source provenance.

Exit gate: canonical simple native state survives save/load/process-style recreation tests and the v16 importer can parse/validate representative source state.

### Phase 2 — Core data, current resources and deterministic world

**Goal:** replace illustrative POC data/map with real MineIT Contract 01 definitions while preserving current behaviour.

- [ ] typed configuration;
- [ ] current resource definitions using stable IDs/generic inventory architecture;
- [ ] current quality bands;
- [ ] current Contract 01 starter data;
- [ ] seeded world/tile generation;
- [ ] survey discovery rules;
- [ ] central resource compatibility ownership where parity-safe;
- [ ] generation/discovery parity fixtures.

Exit gate: a real Contract 01 world can be generated and surveyed with reference-compatible outcomes.

### Phase 3 — Colony survival and daily simulation

- [ ] collection;
- [ ] colony networks;
- [ ] current resource demand/consumption;
- [ ] Food/Power/Fuel survival;
- [ ] mortality/colony death;
- [ ] production/operating costs;
- [ ] `SimulationClock`;
- [ ] deterministic RNG path;
- [ ] long-running soak fixtures.

Exit gate: Android day progression matches representative Contract 01 survival fixtures.

### Phase 4 — Buildings, sites, Industry and Headquarters

- [ ] placement/demolition;
- [ ] extraction development/upgrades;
- [ ] housing/Industry;
- [ ] Spaceport;
- [ ] Power priority/allocation;
- [ ] Headquarters command capacity/load;
- [ ] departure gate;
- [ ] HQ outage/degradation/recovery;
- [ ] temporary ship command takeover;
- [ ] current construction/resource costs.

Exit gate: Headquarters/Power regression scenarios pass natively.

### Phase 5 — Main native game UI/map and design-system foundation

- [ ] production MineIT design tokens;
- [ ] first reusable native primitives;
- [ ] consistent resource/status header;
- [ ] real map tiles/art;
- [ ] inspect/select/survey;
- [ ] hold/drag multi-select;
- [ ] queue/filter/focus presentation;
- [ ] building/site panels;
- [ ] colony/Power detail;
- [ ] responsive phone layout;
- [ ] previews/screenshot baselines;
- [ ] accessibility/back/haptic conventions.

Exit gate: core first-colony gameplay is hands-on playable without web UI and key visual concepts use one design language.

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
- [ ] full consistency audit across migrated concepts;
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

- stable day advance;
- Power shortage;
- beginning-of-day Fuel use;
- depletion/renewable events;
- starvation progression;
- HQ outage/recovery;
- survey discovery;
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
| architecture/owner tests | package/dependency guards + code review rules |
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
- centralised compatibility ownership;
- build-time Universe snapshot validation;
- consistent Compose design system;
- development-only reproducibility diagnostics;
- Android-native accessibility/back/haptic behaviour.

KISS/YAGNI still applies: do not implement abstractions without a present migration or known-roadmap need.

## 21. Rules for bugs and intentional divergence

### Clear defect

1. identify root cause;
2. add/strengthen regression coverage;
3. preferably correct the web canonical owner first while it remains maintained;
4. port corrected behaviour;
5. log the difference if web cannot reasonably be corrected.

### Platform/architecture debt

Fix browser-only implementation debt natively without changing gameplay semantics; no divergence entry is needed when behaviour is equivalent.

### Gameplay/design change

Route material rule/balance/progression changes through discovery/backlog unless explicitly approved as migration work.

All deliberate semantic differences are recorded in [`migration/INTENTIONAL_DIVERGENCES.md`](./migration/INTENTIONAL_DIVERGENCES.md).

## 22. Branch/delivery workflow

For each migration slice:

1. branch from current Android `main`;
2. read Android `AGENTS.md` and this guide;
3. read source MineIT `AGENTS.md` and relevant source/tests;
4. record exact source commit;
5. add parity/regression fixtures first where valuable;
6. implement one canonical native owner;
7. migrate UI after domain capability exists;
8. run focused tests;
9. run full Android CI;
10. produce an APK for hands-on checks when useful;
11. update migration docs/status;
12. merge after acceptance.

Avoid giant migration branches containing unrelated phases.

## 23. Definition of done for a migrated feature

A feature is migrated only when applicable items are true:

- [ ] source owner/tests inspected;
- [ ] native semantic owner clear;
- [ ] gameplay implemented without UI ownership leakage;
- [ ] save/import/migration handled where needed;
- [ ] parity/domain regression coverage exists;
- [ ] important UI interaction coverage exists;
- [ ] design-system conventions used rather than isolated styling;
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
9. representative web v16 saves import correctly if continuity is required.
10. deterministic long soak passes.
11. lifecycle/process recreation cannot silently lose valid state.
12. production signing is complete.
13. no required web-only player flow remains.
14. consistency audit has no major unresolved visual/interaction split.
15. hands-on approval is given on a production-like build.

## 25. Current migration status

### Proven by the POC

- [x] Kotlin/Compose project builds in CI;
- [x] native UI runs on physical Android hardware;
- [x] UI/domain separation is viable;
- [x] pure Kotlin demo simulation is unit-testable;
- [x] CI produces a real APK;
- [x] build metadata is visible in-app;
- [x] persistent development signing is established;
- [x] successive APK updates install in place.

### Phase 0 work created

- [x] `docs/migration/PHASE_0_BASELINE.md`;
- [x] parity-fixture directory and representative web-v16 fixture;
- [x] JVM parity fixture loader/test;
- [x] intentional-divergence log;
- [x] POC domain isolated under `domain/poc`;
- [x] resource architecture direction captured;
- [x] design consistency direction captured;
- [x] Universe bundling direction captured;
- [ ] final Phase 0 branch CI confirmation.

### Next milestone after Phase 0

**Phase 1 — real native state and persistence foundation.**

The first production migration code should establish canonical state/value types, resource-safe identity/inventory foundations, `GameSession`, save envelope/atomic backup storage, web-v16 import validation and initial diagnostics before real gameplay UI replaces the POC.

## 26. Reference notes

At the migration baseline:

- web game version: `5.13.15`;
- web save version: `16`;
- source commit: `9e58983adaa7a15cd525451266ce9df3c17ae886`;
- realistic web save coverage already includes multi-colony state, player-ship cargo/passengers/Fuel, engineering deployments, scan history and resource-coverage migration;
- current web simulation includes Stage 6 Headquarters/Power behaviour;
- the future resource-economy discovery intentionally remains separate from parity migration, but informs permanent native model shape;
- the August 2026 cleanup removed versioned production JS/CSS, import-map debt, application globals/document app-event debt and large embedded HTML-template debt;
- standard activation remains one normal click/tap path with pointer handling reserved for gestures.

Update this guide whenever the source baseline materially changes, a migration decision changes, or a phase completes.

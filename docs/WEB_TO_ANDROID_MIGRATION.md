# MineIT Web-to-Native Android Migration Guide

**Status:** Living migration plan  
**Source application:** `kevvy555/MineIT` (`develop`)  
**Source baseline:** commit `9e58983adaa7a15cd525451266ce9df3c17ae886`  
**Source game version:** `5.13.15`  
**Source save version:** `16`  
**Target application:** `kevvy555/MineIT-Android`  
**Initial native proof of concept:** `0.1.3-poc` / version code `4`  
**Canonical shared universe:** `kevvy555/MineIT-Universe`

---

## 1. Purpose

This document is the authoritative guide for migrating MineIT from its current vanilla HTML/CSS/modular-JavaScript implementation to a fully native Android application built with Kotlin and Jetpack Compose.

The migration is not intended to be a mechanical line-for-line translation. The existing web implementation remains the behavioural reference while each system is moved, but the migration is also an opportunity to remove browser-specific architecture, tighten state ownership, improve persistence, make simulation behaviour more deterministic and testable, and establish a clean native foundation that can support the full future MineIT roadmap.

The target is a native, offline-first Android game with no WebView and no mandatory server dependency.

The migration must preserve the key architectural improvements already achieved in the web repository: one canonical implementation per responsibility, domain-owned gameplay rules, explicit state ownership, directional dependencies, bounded lifecycle ownership, mobile-first interaction and strong regression coverage.

---

## 2. Migration goals

The migration is successful when:

1. MineIT runs as a fully native Android app using Kotlin and Jetpack Compose.
2. Existing gameplay behaviour is preserved unless an intentional bug fix or approved design change is recorded.
3. The native app has one authoritative game-state owner and UI never becomes the source of gameplay truth.
4. The day simulation can run and be tested independently of Android UI APIs.
5. Save data is versioned, recoverable and migration-tested.
6. Existing web saves can be imported where practical.
7. MineIT-Universe remains the canonical source for shared lore/data/art.
8. The game remains fully playable offline after installation.
9. HTML/CSS mocks can continue to be used as rapid visual prototypes, while production UI is implemented in Compose.
10. Android CI proves domain tests, save migration, build validity and signing consistency on every relevant change.
11. The old web game can remain available as a reference until native parity is accepted.
12. Once native parity is accepted, no permanent duplicate game engines are maintained.

---

## 3. Explicit non-goals

The migration must not become an uncontrolled redesign.

The following are not automatic parts of the migration:

- redesigning every gameplay rule simply because the code is being moved;
- preserving known bugs for the sake of exact implementation parity;
- adding speculative frameworks or abstractions with no current requirement;
- recreating browser concepts such as DOM templates, CSS inheritance or `requestAnimationFrame` when Android has a cleaner native equivalent;
- introducing a mandatory backend or online login;
- keeping a WebView as a permanent compatibility layer;
- splitting the Android project into many Gradle modules before the codebase is large enough to justify them;
- copying temporary POC models into the production architecture.

Gameplay changes discovered during migration still follow the MineIT discovery/backlog process unless they are clear defect corrections.

---

## 4. Source architecture being migrated

The current web application already has useful separation that should guide the native architecture.

### 4.1 Current ownership

The web repository uses these primary areas:

- `js/core/` — infrastructure and utilities;
- `js/data/` — static definitions/configuration;
- `js/domain/` — gameplay rules, services and state behaviour;
- `js/persistence/` — save and development-task repositories;
- `js/ui/` — UI controllers/presentation;
- `views/` — external HTML fragments/templates;
- `css/` — presentation styling;
- `tests/` — architecture, unit, regression, simulation and browser probes.

`GameStore` is currently the mutable root state owner. Domain services own gameplay rules. UI controllers render state and invoke domain behaviour.

That ownership model is sound and should be retained conceptually, while the mutable JavaScript implementation is replaced with safer Kotlin state boundaries.

### 4.2 Current composition root

`js/app.js` currently performs several responsibilities at once:

- constructs all domain services;
- constructs persistence;
- creates/normalises state;
- constructs the UI and map runtime;
- coordinates colony switching and corporate events;
- schedules simulation days;
- saves state;
- runs global error handling;
- runs the `requestAnimationFrame` application loop;
- triggers broad UI rendering.

This is one of the main opportunities for improvement. Native Android should not create a new giant `MineITApp.kt` that reproduces all of those responsibilities.

### 4.3 Current persistence

The web `SaveRepository` stores the complete game state as JSON in `localStorage`.

Current strengths:

- the save format is plain JSON;
- realistic round-trip tests already exist;
- state normalisation/migration already has an explicit state version;
- current runtime state is version `16`.

Current limitations to improve natively:

- `localStorage` provides no atomic file replacement;
- there is no automatic previous-save backup;
- corruption recovery is limited;
- save-schema migrations are increasingly concentrated in large normalisation functions;
- durable gameplay state and some session/UI concerns are mixed in the same root object.

### 4.4 Current simulation

The web simulation engine already provides a useful canonical gameplay boundary. It handles production, workforce, Power, Fuel, Industry, Food, deaths, site depletion, accidents, engineering, ships and related daily effects.

However, scheduling of simulation days is tied to the browser animation loop. The native version should make rendering and simulation timing independent.

### 4.5 Current Universe integration

The current ship catalogue reads canonical MineIT-Universe data remotely from GitHub Pages and falls back to a bundled snapshot.

For Android the preferred model is different: the app must be independently playable offline, so a pinned Universe snapshot should be bundled into the application. Optional refresh behaviour can be added later, but the game must never require the network to load core canonical data.

---

## 5. Target Android architecture

The POC has already proved a small `domain` / `ui` separation. The production migration should extend that model without prematurely introducing many Gradle modules.

Initial target layout:

```text
app/src/main/java/com/mineit/android/
├── app/
│   ├── GameSession.kt
│   ├── SimulationClock.kt
│   └── AppComposition.kt
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

This is package separation inside the existing app module. Additional Gradle modules should only be introduced when build time, ownership or reuse provides a measurable reason.

### 5.1 Dependency direction

The intended direction is:

```text
Compose UI
    ↓
ViewModel / UI state holder
    ↓
Application/session actions
    ↓
Domain services / simulation
    ↓
Domain models

Data adapters → domain contracts
Android platform APIs remain outside pure domain code.
```

The domain layer must not depend on Compose, Android `Context`, Activity, lifecycle classes, filesystem APIs or network clients.

---

## 6. Game-state ownership

### 6.1 Native equivalent of GameStore

The web `GameStore` concept is worth preserving, but the Android implementation should expose immutable state.

Recommended owner:

```text
GameSession
    owns StateFlow<GameState>
    serialises gameplay commands
    invokes domain services
    saves committed state
```

Compose observes `StateFlow<GameState>` through ViewModels. UI never mutates state directly.

### 6.2 Immutable at boundaries

The preferred rule is:

- `GameState` exposed to UI is immutable;
- domain operations return explicit results and the next authoritative state;
- if controlled internal mutation is later needed for simulation performance, it remains private to a domain transaction and never leaks to UI.

Do not reproduce widespread public field mutation simply because JavaScript currently allows it.

### 6.3 Derived state

During migration, audit fields currently stored under `metrics` and other runtime objects.

For each field decide whether it is:

1. durable gameplay truth;
2. historical state required for future simulation;
3. a derived calculation that can be recomputed;
4. transient UI/session state.

Derived values should generally not be persisted unless keeping them is required for exact behaviour or performance.

Examples that require explicit review include Power network summaries, current production forecasts, selected camera position and game speed.

Do not remove fields from imported web saves until a migration test proves the new representation preserves behaviour.

---

## 7. Time and simulation architecture

The web application currently drives game days from `requestAnimationFrame`, accumulating elapsed time according to `state.speed`.

Native Android should separate these concerns.

### 7.1 SimulationClock

Create an application-layer `SimulationClock` backed by coroutines.

Responsibilities:

- map selected game speed to a day cadence;
- pause when speed is zero;
- pause for blocking corporate events where required;
- dispatch `advanceDay` to `GameSession`;
- never own gameplay rules;
- respect lifecycle without binding simulation correctness to frame rendering.

### 7.2 Rendering must not drive simulation

Compose recomposition, device refresh rate and animation frames must have no influence over game results.

`SimulationEngine.advanceDay(state)` should produce the same result whether invoked by a timer, a unit test, a debug command or a soak test.

### 7.3 Determinism

Where gameplay uses randomness, introduce a domain-owned random source with persisted seed/state so tests can reproduce outcomes.

The current web state creation already uses generated seeds. During each ported feature, identify any direct random calls outside the authoritative seeded path. Replace those with an injected/persisted random source instead of carrying hidden nondeterminism into Kotlin.

---

## 8. Persistence and save migration

Persistence is an early migration priority because every later vertical slice benefits from a stable save contract.

### 8.1 Native save format

Use `kotlinx.serialization` for the root save model.

Recommended envelope:

```text
SaveEnvelope
- formatVersion
- gameVersion
- universeContentVersion
- savedAt
- state
```

The root game state may remain a JSON document initially. MineIT is naturally an aggregate simulation state, so Room should not be introduced merely because it is an Android database option.

Use Room later only if a real need emerges for independently queried large datasets such as extensive telemetry/history.

### 8.2 Storage mechanism

Recommended production save repository:

- app-private file storage;
- write new save to a temporary file;
- flush/close;
- atomically replace the active save where supported;
- retain one previous known-good backup;
- validate before promoting a loaded save;
- surface meaningful recovery diagnostics.

Use DataStore for small preferences such as UI options, not the complete game state.

### 8.3 Web save compatibility

Current web save state is JSON version `16`.

Create a dedicated `WebSaveV16Importer` rather than contaminating normal native domain models with JavaScript compatibility fields.

Suggested flow:

```text
Web JSON
  ↓
WebSaveV16 DTO
  ↓
validated import
  ↓
explicit migration/adaptation
  ↓
current Android GameState
  ↓
normal Android SaveEnvelope
```

If existing player saves must move from the browser version, add an export-to-file feature to the web game and an import action to Android. Direct access to browser `localStorage` should not be assumed.

### 8.4 Migration chain

Avoid one permanently growing `normalizeState()` function.

Use explicit ordered migrations, for example:

```text
v16 web import → native v1
native v1 → native v2
native v2 → native v3
```

Each migration must have fixture tests.

---

## 9. MineIT-Universe integration

`MineIT-Universe` remains canonical. The Android repository stores only the versioned snapshot/cache needed to build and run the game.

Recommended flow:

```text
MineIT-Universe canonical JSON/art
        ↓
explicit sync tool / CI validation
        ↓
pinned content version + source commit
        ↓
Android bundled assets
        ↓
UniverseRepository
        ↓
domain read-only definitions
```

Requirements:

- core gameplay must work without internet;
- bundled data provenance must include Universe content version/source commit;
- malformed or incompatible Universe data must fail validation during development/CI;
- runtime gameplay state must not mutate canonical Universe definitions;
- images should continue to use canonical `image.key` identifiers where practical;
- optional remote catalogue refresh can be considered later, but bundled data remains sufficient to play.

The existing web ship catalogue fallback model can inform this design, but Android should prefer build-time pinning over a runtime remote-first dependency.

---

## 10. HTML/CSS to Compose migration workflow

HTML/CSS remains useful during migration as a rapid design tool.

Recommended workflow:

```text
Feature idea
  ↓
HTML/CSS visual mock when useful
  ↓
user approves visual direction
  ↓
Compose implementation using MineIT design components
  ↓
Compose preview/screenshot test
  ↓
APK hands-on check when required
```

The HTML mock is a visual specification, not production code.

### 10.1 Common mappings

| Web | Native Compose |
|---|---|
| HTML `div` containers | `Box`, `Row`, `Column` |
| Flexbox | `Row` / `Column` + weights/arrangement |
| CSS grid | `LazyVerticalGrid`, custom layouts |
| CSS variables | Compose design tokens / theme |
| CSS classes | reusable composables + modifiers |
| external HTML templates | composable functions |
| modal overlays | Dialog, bottom sheet or full-screen route |
| canvas world map | Compose `Canvas` / custom drawing |
| media queries | window constraints / size classes |
| click handlers | `clickable` / semantic buttons |
| pointer gestures | `pointerInput` gesture handling |
| animation | Compose animation APIs |

### 10.2 Design system first

Before mass UI migration, create reusable MineIT-native primitives:

- resource/status card;
- compact stat row;
- section header;
- primary/secondary/icon action buttons;
- warning/critical states;
- modal/sheet shell;
- list/card containers;
- progress meters;
- resource icon/image treatment;
- game typography and spacing tokens.

This prevents every screen from inventing independent styling.

### 10.3 Preserve the successful activation rule

The web cleanup established an important interaction rule: normal activation uses one standard click path while pointer events are reserved for gestures.

Preserve the same intent natively:

- normal taps use Compose semantic click handling;
- pointer input is used for drag, long press and multi-select;
- do not create competing tap and pointer-up activation paths;
- gesture consumption must suppress the associated normal activation when appropriate.

### 10.4 Async UI safety

Web code currently needs explicit stale-DOM protection. Compose makes this easier, but the underlying requirement remains.

Use coroutine cancellation, scoped ViewModels and state identity. Results for a ship/colony/tile that is no longer selected must not update a newly selected object.

---

## 11. Web-to-Android ownership map

| Current web owner | Native target | Migration rule |
|---|---|---|
| `js/core/config.js` | `domain/config` | Convert magic/global constants into typed configuration/value objects. |
| `js/core/game-store.js` | `app/GameSession` | Replace mutable observable root with immutable `StateFlow` boundary. |
| `js/domain/game-state*.js` | `domain/model` + `data/save/migration` | Separate current schema from legacy migration logic. |
| `js/domain/simulation-engine.js` | `domain/simulation` | Port behaviour with parity fixtures before optimisation. |
| `js/domain/*-service.js` | matching feature packages | Preserve semantic owners; remove browser dependencies. |
| `js/data/*.js` | typed Kotlin definitions or bundled JSON adapters | Keep static data out of UI. |
| `js/persistence/save-repository.js` | `data/save/SaveRepository` | Atomic files, backup and explicit migration. |
| `js/app.js` | composition + session + clock + coordinators | Split responsibilities; do not create another monolith. |
| `js/ui/*.js` | feature ViewModels + composables | UI renders state and emits intent only. |
| `views/*.html` | composables | Translate layout, not DOM mechanics. |
| `css/*` | theme/design tokens/modifiers | Consolidate reusable visual rules. |
| world/star-map canvas | Compose Canvas | Preserve interaction and rendering behaviour with native input. |
| browser lifecycle cleanup | coroutine/ViewModel/Compose lifecycle | Use explicit scopes and `DisposableEffect` only where needed. |
| Node/browser tests | JUnit + Compose + screenshot/instrumentation tests | Port behavioural intent, not implementation-string assertions. |

---

## 12. Domain migration inventory

Every current semantic owner must be accounted for before web retirement.

### Core state and simulation

- [ ] `game-state.js`
- [ ] `game-state-runtime.js`
- [ ] `simulation-engine.js`
- [ ] `game-store.js` responsibilities
- [ ] `config.js`
- [ ] game date / absolute-day handling
- [ ] speed and pause behaviour
- [ ] game log/telemetry

### Resources, production and infrastructure

- [ ] `resource-service.js`
- [ ] `inventory-service.js`
- [ ] `collection-service.js`
- [ ] `colony-service.js`
- [ ] `site-service.js`
- [ ] `development-service.js`
- [ ] `building-model.js`
- [ ] `spaceport-model.js`
- [ ] `extraction-overdrive.js`
- [ ] Power network
- [ ] workforce network
- [ ] Industry network
- [ ] Headquarters command/outage/recovery
- [ ] Food/Fuel/Ore/Build production and consumption
- [ ] depletion/renewable behaviour

### World and surveying

- [ ] `world-service.js`
- [ ] `land-service.js`
- [ ] `survey-service.js`
- [ ] tile generation/state
- [ ] scan queue and resurvey history
- [ ] map camera / focus / filters
- [ ] tap, inspect, long-press and multi-select

### Technology and progression

- [ ] `technology-service.js`
- [ ] technology definitions
- [ ] engineering deployments
- [ ] local vs company technology
- [ ] upgrade eligibility
- [ ] progression visibility/unlocks

### Contracts and corporation

- [ ] `contract-service.js`
- [ ] `portfolio-service.js`
- [ ] `corporate-event-service.js`
- [ ] colony creation/switch/removal/relocation
- [ ] deadlines, renewal/holdover/end states
- [ ] corporation game-over handling
- [ ] reputation
- [ ] asset/cash policy behaviour already present

### Trade and buyers

- [ ] `trade-service.js`
- [ ] `buyer-service.js`
- [ ] buyer market definitions/balance
- [ ] corporate trade ship events
- [ ] import/export capacity
- [ ] reserves
- [ ] buyer offers/contracts/collections
- [ ] relationship/happiness/misses/termination

### Ships and expansion

- [ ] `expansion-service.js`
- [ ] `ship-market-service.js`
- [ ] `transport-service.js`
- [ ] starter/founding ship
- [ ] passenger/accommodation allocation
- [ ] cargo/Fuel
- [ ] factory-new ship purchasing
- [ ] ship locations/states
- [ ] star/system navigation already implemented
- [ ] future approved system navigation/fuel/wear items integrated as they land in the web source or are deliberately implemented native-first

### Shared Universe

- [ ] ship classes/runtime profiles
- [ ] manufacturer/organisation data
- [ ] art/image-key mapping
- [ ] manifest/content-version validation
- [ ] bundled fallback/snapshot strategy

---

## 13. Migration strategy: vertical slices, not a big-bang rewrite

After the native foundation is established, migrate playable vertical slices. Each slice should finish with domain tests, persistence coverage and enough UI to hands-on test the behaviour.

### Phase 0 — Baseline and migration harness

**Goal:** freeze a measurable reference point.

Tasks:

- [ ] record source web commit/version/save version in this document;
- [ ] inventory domain owners and important screens;
- [ ] preserve current web CI as behavioural reference;
- [ ] create cross-runtime JSON fixtures from representative web saves;
- [ ] create a parity-fixture directory in Android;
- [ ] define intentional-divergence log format;
- [ ] remove/rename the POC `DemoGame` types before production models use those names.

Exit gate:

- Android can load test fixtures representing realistic web state without implementing gameplay yet.

### Phase 1 — Native state and persistence foundation

**Goal:** establish production-quality root ownership before porting gameplay.

Tasks:

- [ ] define canonical Android `GameState`/value types;
- [ ] implement `GameSession`;
- [ ] implement `SaveEnvelope`;
- [ ] implement atomic save + backup;
- [ ] add save round-trip tests;
- [ ] implement web-v16 importer skeleton;
- [ ] move UI preferences out of durable gameplay state where proven safe;
- [ ] implement explicit native migration chain.

Exit gate:

- a simple state survives process restart and schema migration tests.

### Phase 2 — Core data, resources and deterministic world

**Goal:** replace illustrative POC resources/map with real MineIT definitions.

Tasks:

- [ ] migrate configuration constants into typed Kotlin;
- [ ] migrate resource definitions;
- [ ] migrate inventory quality bands;
- [ ] migrate contract starter data needed by Contract 01;
- [ ] migrate world/tile generation;
- [ ] preserve seeded generation;
- [ ] migrate survey discovery rules;
- [ ] add fixture tests for generated sectors and discovered resources.

Exit gate:

- a new real Contract 01 world can be generated and surveyed identically enough to the reference rules.

### Phase 3 — Colony survival and daily simulation

**Goal:** make the first colony genuinely playable.

Tasks:

- [ ] port CollectionService behaviour;
- [ ] port ColonyService networks;
- [ ] port resource demand/consumption;
- [ ] port Food/Power/Fuel survival;
- [ ] port mortality and colony death;
- [ ] port production and operating costs;
- [ ] implement `SimulationClock`;
- [ ] port relevant long-running soak fixtures.

Exit gate:

- advancing days in Android produces matching Contract 01 survival outcomes for reference fixtures.

### Phase 4 — Buildings, sites, Industry and Headquarters

**Goal:** migrate colony development and current Stage 1–6 infrastructure.

Tasks:

- [ ] building placement/demolition;
- [ ] extraction-site development/upgrades;
- [ ] housing and Industry;
- [ ] Spaceport;
- [ ] Power generation and priority allocation;
- [ ] Headquarters command capacity/load;
- [ ] first-departure gate;
- [ ] Headquarters outage/degradation/recovery;
- [ ] ship temporary command takeover;
- [ ] construction/resource costs.

Exit gate:

- the existing Headquarters and Power regression scenarios pass natively.

### Phase 5 — Main native game UI and map interaction

**Goal:** replace the POC screen with the real playable MineIT shell.

Tasks:

- [ ] native MineIT status/resource header;
- [ ] real map tiles/art;
- [ ] tap to inspect/select;
- [ ] tap unrevealed sector to survey;
- [ ] hold/drag multi-select;
- [ ] queue/status presentation;
- [ ] map filters/focus;
- [ ] building/site panels;
- [ ] colony status/power detail;
- [ ] responsive phone layouts;
- [ ] Compose previews and screenshot baselines.

Exit gate:

- core first-colony gameplay can be played without the web app.

### Phase 6 — Trade, contracts and commercial events

**Goal:** reproduce the commercial loop.

Tasks:

- [ ] corporate trade ship arrival/departure;
- [ ] buy/sell flows;
- [ ] trade capacity and reserves;
- [ ] contract goals/deadlines/decisions;
- [ ] corporate event queue;
- [ ] pause/resume behaviour around events;
- [ ] buyer offers/contracts/collections;
- [ ] game log events.

Exit gate:

- Contract 01 can be commercially progressed and resolved in Android.

### Phase 7 — Portfolio and multi-colony management

**Goal:** reproduce corporation-level play.

Tasks:

- [ ] portfolio state model;
- [ ] colony switching;
- [ ] inactive-colony daily simulation;
- [ ] colony-local state capture;
- [ ] global date handling;
- [ ] cross-colony pending event sequencing;
- [ ] colony loss/game-over behaviour;
- [ ] multi-colony UI.

Exit gate:

- realistic multi-colony web save fixtures round-trip and simulate correctly natively.

### Phase 8 — Ships, expansion and ship market

**Goal:** reproduce current Stage 6–8 ship gameplay.

Tasks:

- [ ] fleet state;
- [ ] founding ship;
- [ ] cargo/passengers/accommodation;
- [ ] docked/landed/travel states;
- [ ] factory-new ship catalogue;
- [ ] ship purchase rules;
- [ ] transport orders;
- [ ] star/system map presentation;
- [ ] current navigation behaviour;
- [ ] integrate newly approved spacecraft Fuel/navigation/wear systems at the correct point in the migration.

Exit gate:

- current ship/fleet/market web regression scenarios pass natively.

### Phase 9 — Remaining UI parity and polish

**Goal:** close all presentation gaps.

Tasks include:

- [ ] company/corporation views;
- [ ] contract board;
- [ ] technology and engineering screens;
- [ ] buyer screens;
- [ ] survival warnings;
- [ ] trade/reserve controls;
- [ ] ship controls;
- [ ] overlays/toasts/dialogs;
- [ ] how-to-play/onboarding;
- [ ] game-over/lost-colony flows;
- [ ] accessibility labels and touch targets;
- [ ] haptics where useful;
- [ ] landscape/large-screen sanity.

Exit gate:

- a feature inventory comparison identifies no required web-only player flow.

### Phase 10 — Production hardening and cutover

**Goal:** make native Android the primary implementation.

Tasks:

- [ ] import real representative web saves;
- [ ] full simulation soak;
- [ ] Compose screenshot regression suite;
- [ ] key instrumentation tests;
- [ ] performance profiling on representative Android hardware;
- [ ] memory/leak checks around map/screens;
- [ ] app lifecycle save/recovery testing;
- [ ] release signing/upload-key configuration;
- [ ] R8 release verification;
- [ ] offline cold-start test;
- [ ] final source feature matrix;
- [ ] user hands-on approval;
- [ ] declare native canonical implementation.

The web repository should remain available as history/reference, but new gameplay development should then move to Android unless a web edition is deliberately maintained as a separate supported product.

---

## 14. Behaviour parity and cross-runtime testing

A rewrite is most dangerous when both implementations appear correct but differ subtly. MineIT should use fixture-driven parity during migration.

### 14.1 Golden fixtures

For important systems create JSON fixtures containing:

- initial state;
- action/day count;
- deterministic random seed where applicable;
- expected result summary;
- expected important state fields.

Examples:

- one day of stable survival;
- Power shortage;
- beginning-of-day Fuel use;
- extraction-site depletion;
- starvation progression;
- Headquarters outage day 1/5/10;
- survey discovery;
- building upgrade;
- trade transaction;
- buyer collection;
- multi-colony day advance;
- ship cargo/passengers;
- save v16 migration.

During migration, the web implementation can generate/validate the fixture and the Kotlin implementation must satisfy the same behavioural contract.

### 14.2 Do not compare irrelevant representation

Parity tests should compare gameplay meaning rather than object ordering, floating-point formatting or DOM presentation.

Where Kotlin intentionally improves representation, compare canonical summaries.

---

## 15. Test migration map

The current web test suite is extensive and must be treated as a behaviour inventory.

| Web test class | Android replacement |
|---|---|
| architecture baseline / owner-map tests | package/dependency architecture tests + code review rules |
| domain service tests | Kotlin/JUnit domain tests |
| save round-trip tests | serialization + migration fixture tests |
| long simulation soak | deterministic JVM soak tests |
| browser interaction probes | Compose instrumentation tests |
| layout/presentation regression | Compose screenshot tests |
| mobile viewport probes | device/configuration screenshot/instrumentation matrix |
| lifecycle soak | Compose/navigation/ViewModel lifecycle tests |
| canvas interaction probes | Compose UI tests plus focused gesture tests |

CI should remain layered so fast JVM tests run first and emulator work is reserved for interactions that cannot be proven without Android runtime.

---

## 16. Known architecture improvements to make during the port

These are migration opportunities, not criticism of the cleaned web architecture.

### 16.1 Split `app.js` responsibilities

Do not recreate its current breadth in one Kotlin class.

Separate:

- dependency construction;
- GameSession/state ownership;
- simulation scheduling;
- corporate-event coordination;
- UI navigation;
- lifecycle persistence.

### 16.2 Remove service back-patching

The web composition root currently constructs services and then assigns some collaborators after construction.

Native code should prefer constructor dependencies or narrow interfaces so dependency relationships are explicit and immutable after creation.

### 16.3 Narrow UI dependencies

Some web UI controllers receive a very broad bundle of services/callbacks.

Native feature ViewModels should depend only on the use cases/state they require.

### 16.4 Make save migrations explicit

Move legacy compatibility out of canonical model construction. Keep migrations ordered, named and testable.

### 16.5 Improve durable/transient state separation

Audit persisted camera, speed, diagnostics/metrics and presentation-related selections. Store preferences/session state separately when doing so does not change gameplay behaviour.

### 16.6 Make randomness reproducible

Any hidden use of global randomness should become an injected/persisted domain random source.

### 16.7 Separate simulation cadence from rendering

No gameplay progression should depend on Compose frame rate or UI recomposition.

### 16.8 Prefer typed value objects for important concepts

Candidates include:

- `AbsoluteDay` / `GameDate`;
- `ColonyId`;
- `ShipId`;
- resource identifiers;
- money/quantity where useful;
- quality bands;
- percentage/factor values where range validation prevents bugs.

Use this selectively; do not wrap every primitive merely for architectural appearance.

---

## 17. Rules for fixing bugs during migration

The migration is explicitly allowed to improve code and correct issues, but every divergence from the web reference must be intentional.

### Clear gameplay bug

If an existing web behaviour is clearly defective:

1. identify the root cause;
2. add/strengthen a regression test;
3. preferably correct the canonical web reference first while it remains actively maintained;
4. port the corrected behaviour to Android;
5. record the divergence if the web fix cannot reasonably be made.

Do not intentionally reproduce a known bug simply to make parity tests green.

### Architecture/browser-specific debt

If the problem exists only because of web technology, fix it in the native design without changing gameplay semantics.

Examples include DOM lifecycle, stale template writes and animation-frame scheduling.

### Gameplay/design change

If a change alters intended rules, balancing or progression, it is not merely migration cleanup. Route it through the progression backlog/discovery process unless the user explicitly approves doing it as part of the migration.

### Migration decision log

Add short dated entries to this document whenever an intentional behavioural divergence is introduced:

```text
YYYY-MM-DD — Area — Web behaviour — Native behaviour — Reason — Tests
```

---

## 18. Resource-overhaul interaction with the migration

The current web game still has a resource-model audit and later refining/manufacturing work in the progression backlog. Current categories remain Food, Build, Fuel and Ore with individual raw resource definitions.

Do not hard-code the native architecture so tightly around those four categories that the approved resource overhaul becomes another rewrite.

The native model should support:

- stable resource IDs;
- resource category/type definitions;
- raw/refined/manufactured classifications;
- qualities/quantity;
- extraction/production compatibility;
- future recipes;
- future ship Fuel types;
- data-driven display metadata.

However, do not implement unapproved refining/manufacturing mechanics merely to anticipate them. Design the model to accommodate the known roadmap, then migrate current behaviour first.

---

## 19. Branch and delivery workflow

Recommended migration workflow:

1. branch from current Android `main` for one migration slice;
2. read this document and `AGENTS.md` before implementation;
3. record the source MineIT commit being used for that slice;
4. inspect the source implementation and its tests;
5. add parity/regression fixtures first where valuable;
6. implement the canonical native owner;
7. update UI only after domain behaviour is available;
8. run focused tests;
9. run full required Android CI;
10. produce a directly downloadable APK for hands-on checks when appropriate;
11. update this document's checklist/status;
12. merge after acceptance.

Avoid enormous migration branches containing unrelated systems.

---

## 20. Definition of done for a migrated feature

A web feature is considered migrated only when all applicable conditions are true:

- [ ] its canonical web implementation and relevant tests were inspected;
- [ ] its native semantic owner is clear;
- [ ] gameplay behaviour is implemented without UI ownership leakage;
- [ ] persistence is handled where the feature adds durable state;
- [ ] legacy/import migration is handled where needed;
- [ ] domain regression/parity tests exist;
- [ ] Compose/instrumentation coverage exists for important interactions;
- [ ] no duplicate temporary production implementation remains;
- [ ] known divergences are documented;
- [ ] Android CI is green;
- [ ] the feature can be hands-on tested in the APK;
- [ ] this migration checklist is updated.

---

## 21. Production cutover criteria

Do not retire the web implementation as the behavioural reference until the native app satisfies all of the following:

1. Contract 01 can be started, played, saved, reloaded and completed.
2. Survival/economy results pass representative parity fixtures.
3. Power, Industry, workforce and Headquarters behaviour is covered.
4. Surveying, map gestures and development are complete.
5. Trade and corporate event flows are complete.
6. Multiple colonies simulate and switch correctly.
7. Current ship/fleet/market behaviour is complete.
8. MineIT-Universe data/art is bundled and validated.
9. Representative web v16 saves import correctly if save continuity is required.
10. Long simulation soak passes.
11. Android lifecycle/process restart does not lose valid state.
12. Production signing strategy is complete.
13. No required player flow remains web-only.
14. Hands-on approval is given on a production-like Android build.

---

## 22. Current migration status

### Proven by the POC

- [x] Kotlin/Jetpack Compose project builds in CI.
- [x] Native UI runs on a physical Android device.
- [x] UI and domain code can be separated cleanly.
- [x] Pure Kotlin simulation can be unit-tested.
- [x] APK can be produced entirely through CI.
- [x] App version is visible from real Android build metadata.
- [x] Persistent development signing is established.
- [x] Successive APKs update in place without uninstalling.

### Next migration milestone

**Phase 0 + Phase 1: replace the illustrative POC state with the real migration foundation.**

The next implementation should focus on:

1. production package structure inside the current module;
2. canonical root `GameState` direction;
3. `GameSession` ownership;
4. SaveEnvelope/atomic persistence;
5. web-v16 fixture/import harness;
6. the first real Contract 01 state fixture.

This gives every later gameplay port a stable foundation and avoids converting UI before the real state model exists.

---

## 23. Migration decision log

No intentional gameplay divergences recorded yet.

---

## 24. Reference notes

At the source baseline used to create this plan:

- MineIT package version is `5.13.15`;
- runtime save state is version `16`;
- realistic save-roundtrip coverage includes multi-colony state, ship cargo/passengers/Fuel, engineering deployments, scan history and resource-coverage migration;
- the current simulation includes the Stage 6 Headquarters/Power changes;
- the progression backlog includes the resource-model audit, spacecraft Fuel, system navigation, Veyrite wear, refining and later manufacturing/logistics work;
- the August 2026 cleanup removed versioned JS/CSS implementations, import-map debt, application globals/document event-bus debt and large embedded HTML-template debt;
- the web interaction architecture now uses normal click activation and pointer events only for gestures, a rule that should remain conceptually true in Compose.

This document should be updated whenever the source baseline materially changes or a migration phase is completed.

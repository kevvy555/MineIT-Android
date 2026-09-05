# MineIT Web-to-Native Android Migration Guide

**Status:** Living migration plan — Phases 0–5 complete/accepted; Phase 6 implementation is CI-green and under physical-device acceptance; Phase 7 single-colony parity closure is the next approved migration phase  
**Source application:** `kevvy555/MineIT` (`develop`)  
**Current behavioural + UI-layout baseline:** `075b3d82fd88334b20b3cfe7d6e2731c8d840533`  
**Current source game version:** `5.13.22`  
**Current source save version:** `16`  
**Target application:** `kevvy555/MineIT-Android`  
**Current Android validation build:** `0.7.2-migration` / version code `14`  
**Current native save format:** `6`  
**Current Android branch:** `feature/migration-phase-6`  
**Current Phase 6 validation-code checkpoint:** `41a3859957cc382d74b0e47093ff41e9a4cb7c54`  
**Current Phase 7 planning record:** `docs/migration/PHASE_7_SINGLE_COLONY_PARITY.md`  
**Canonical shared universe:** `kevvy555/MineIT-Universe`

## 1. Purpose

This document is the authoritative master plan for migrating MineIT from vanilla HTML/CSS/modular JavaScript to a fully native Android application built with Kotlin and Jetpack Compose.

The migration is not a line-for-line rewrite. The maintained web `develop` implementation remains the behavioural reference and, for player-facing work, the layout/information-hierarchy reference until native parity/cutover is explicitly accepted.

The governing rule is:

> **Preserve gameplay by default. Improve architecture where the improvement does not materially change gameplay. Preserve recognisable UI hierarchy/workflow unless a deliberate refinement is approved.**

The target is a native, offline-first Android game with no WebView and no mandatory backend/server dependency.

## 2. Recovery documents

Repository documents, not chat history, are the recovery source of truth.

- `docs/migration/PHASE_0_BASELINE.md` — original source baseline and parity harness.
- `docs/migration/PHASE_1_FOUNDATION.md` — state/persistence foundation.
- `docs/migration/PHASE_2_CORE_WORLD.md` — resources/world/survey foundation.
- `docs/migration/PHASE_3_DAILY_SIMULATION.md` — daily simulation and survival.
- `docs/migration/PHASE_4_BUILDINGS_HEADQUARTERS.md` — buildings, Power, Industry, Spaceport and Headquarters.
- `docs/migration/PHASE_5_NATIVE_UI.md` — accepted native map/UI/design-system foundation.
- `docs/migration/PHASE_6_CONTRACTS_TRADE_EVENTS.md` — commercial systems, N05 source refresh and Phase 6 validation state.
- `docs/migration/PHASE_7_SINGLE_COLONY_PARITY.md` — approved next-phase single-colony parity closure plan.
- `docs/migration/DESIGN_SYSTEM_DIRECTION.md` — visual/interaction consistency and UI-led parity method.
- `docs/migration/RESOURCE_ARCHITECTURE_DIRECTION.md` — resource architecture allowed during migration while the resource overhaul stays deferred.
- `docs/migration/UNIVERSE_BUNDLING_DIRECTION.md` — build-time Universe pinning/validation/offline bundling.
- `docs/migration/INTENTIONAL_DIVERGENCES.md` — deliberate semantic differences only.

The Phase 0 source commit is historical. The active source baseline at the top of this document must be refreshed whenever current web `develop` materially changes behaviour already inside migrated native scope.

## 3. Migration goals

The migration is successful when:

1. MineIT runs fully natively with Kotlin and Jetpack Compose.
2. Existing gameplay behaviour is preserved unless a difference is deliberately approved and recorded.
3. One authoritative native root state owner controls gameplay state.
4. UI renders immutable state and dispatches intent; it does not own gameplay truth.
5. Simulation is independent of rendering/frame rate and deterministic where source behaviour is deterministic.
6. Save data is versioned, atomic, recoverable and migration-tested.
7. Existing web saves can be imported where practical/required.
8. Required MineIT-Universe gameplay data/art is bundled and usable offline.
9. Native UI uses a consistent MineIT design language while preserving recognisable web hierarchy/workflows by default.
10. Android CI proves domain/parity tests, save migration, build validity and development signing consistency.
11. Every player-facing vertical slice is reviewed on-device before acceptance.
12. The native implementation becomes canonical only after final cutover criteria are met.

## 4. Explicit non-goals during parity migration

Do not allow migration to become an uncontrolled redesign.

Do not automatically:

- redesign gameplay because code is moving;
- replace a working web information hierarchy merely because Compose makes another layout convenient;
- perform the full resource-economy overhaul;
- define the final refined/manufactured resource catalogue;
- rebalance economy, yields, nutrition, progression or ship Fuel systems;
- introduce a mandatory backend/login;
- keep a WebView as compatibility architecture;
- create speculative frameworks or premature module splits;
- pull full multi-colony or interstellar ship gameplay forward before the single-colony game is coherent.

Clear defects should be corrected with regression coverage. Where practical, fix a clear maintained-web defect first and then port the corrected rule.

## 5. Target architecture

Keep one Gradle app module until measurable build/ownership/reuse pressure justifies more.

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

Primary ownership:

- `app/` — `GameSession`, clock, composition and coordinators;
- `domain/` — gameplay rules and immutable gameplay models;
- `data/` — save/universe/preferences adapters;
- `ui/` — Compose presentation and transient view state.

`GameSession` owns the authoritative `StateFlow<GameState>`, serialises gameplay commands, commits authoritative next state and coordinates persistence.

Compose never becomes the source of truth for economy, survival, pricing, progression, inventory, warnings or simulation rules.

## 6. Time, persistence and compatibility

MineIT uses a canonical 360-day year.

`SimulationClock` maps selected speed to day cadence and asks `GameSession` to advance deterministic simulation. Rendering does not drive gameplay.

Native saves use an explicit envelope with format version, game version, Universe provenance and game state. Production persistence remains atomic with previous-known-good recovery.

Current native migration chain:

```text
web v16 → native current
native v1 → v2
native v2 → v3
native v3 → v4
native v4 → v5
native v5 → v6
```

Native v6 introduced the durable N05 founding fleet/establishment state. Existing pre-N05 Android saves deliberately keep their established colony inventory and residents ashore rather than being transformed into a fresh-game founding state.

## 7. MineIT-Universe direction

MineIT-Universe remains canonical for shared lore/data/art.

Required runtime data must eventually flow through:

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

Android may store a generated pinned snapshot but must not become a second manually maintained canonical Universe.

The full resource overhaul remains separate. During migration, stable IDs, generic quantity/quality storage and data-driven definitions are allowed architectural improvements without redefining the resource economy.

## 8. UI-led parity method — mandatory for remaining player-facing work

The current web MineIT UI is the layout and information-hierarchy reference as well as the behavioural reference.

For each remaining screen/feature, inspect together:

1. current HTML/view structure;
2. CSS determining hierarchy, density and responsive behaviour;
3. active `js/ui/` controller/presenter behaviour;
4. relevant domain owner and tests;
5. current native owner/presentation.

Then explicitly decide what to **preserve**, **minorly refine** or **replace because the source is defective/unsuitable**.

Preserve by default:

- overall purpose and navigation relationship;
- major information ordering;
- established resource/status groupings;
- map-first density;
- primary action placement;
- terminology/status meaning;
- recognisable trade, contract, buyer, colony, scanning and ship workflows.

Minor Android refinements are encouraged where they genuinely improve:

- safe-area/system-bar handling;
- touch targets;
- accessibility semantics;
- Android Back behaviour;
- typography/readability;
- responsive phone sizing;
- sheets/dialogs where native behaviour is better;
- subtle consistent haptics;
- repeated visual grammar through shared MineIT primitives.

The target is not pixel-for-pixel DOM recreation, but a web player should immediately recognise the screen and workflow.

Material workflow/gameplay differences require explicit approval and, where semantic, an intentional-divergence record.

## 9. Testing method

Use the cheapest test that proves the required behaviour:

- Kotlin/JUnit for domain/service behaviour;
- save round-trip/migration fixtures for persistence;
- parity fixtures where cross-runtime values materially reduce risk;
- Compose/instrumentation for interactions not provable at unit level;
- screenshot/presentation coverage for representative states where useful;
- deterministic soak tests for long-running simulation;
- physical-device review for complete player-facing slices.

Bug fixes should reproduce the failure in a regression test whenever practical. Do not weaken architecture or regression guards merely to obtain green CI.

## 10. Current migrated capability

### Foundation/world/simulation

- [x] canonical native root state and `GameSession`;
- [x] native save migrations through v6;
- [x] 360-day calendar;
- [x] current resource definitions and quality-band inventory;
- [x] deterministic landing/world/discovery foundation;
- [x] survey queue/timing foundation;
- [x] daily production/survival simulation;
- [x] Food/Power/Fuel/mortality behaviour;
- [x] native simulation clock;
- [x] production map/selection/multi-select foundation.

### Colony/infrastructure

- [x] development placement/demolition/upgrades;
- [x] extraction/renewables/depletion foundation;
- [x] Housing/Power/Industry/Spaceport;
- [x] differentiated Power network;
- [x] Headquarters command/load/outage/recovery;
- [x] temporary founding-ship command support;
- [x] current N05 ship/planet resident survival split;
- [x] founding ship self-powered 50 Industry;
- [x] explicit ship↔colony quality-preserving inventory transfers;
- [x] establishment assessment/resident transfer gates/acknowledgement;
- [x] stacked Ship/Colony HUD foundation.

### Commercial

- [x] Corporate Ship service/domain rules;
- [x] import/export/passenger capacity and reserves;
- [x] buyer offers/contracts/collections/relationship outcomes;
- [x] Contract 01 deadline/renewal/holdover/failure state;
- [x] corporate event queue and durable Game Log;
- [x] commercial save state;
- [x] Phase 6 trade/contract/buyer/Colony Control UI refinement baseline.

### Still incomplete before broader expansion

- [ ] single-colony scanning presentation/workflow parity;
- [ ] full rich building/extraction information panels;
- [ ] complete single-colony Headquarters/Colony Control workflow parity;
- [ ] proper founding/player-ship control panel;
- [ ] Corporate Ship presentation parity polish;
- [ ] Conglomerate Buyers Service parity polish;
- [ ] single-colony technology/engineering progression needed by existing upgrade gates;
- [ ] resource detail/production explanation views;
- [ ] complete Spaceport service panel;
- [ ] source-compatible warning/attention system and warning bug cleanup;
- [ ] final Contract/company/Game Log single-colony parity sweep;
- [ ] representative real web-v16 conversion;
- [ ] Portfolio/multi-colony lifecycle;
- [ ] full fleet market/travel/navigation;
- [ ] Universe bundling/validation production path.

## 11. Phase roadmap and status

### Phase 0 — Baseline and harness — COMPLETE

Original source baseline, ownership inventory, parity fixtures and divergence log.

### Phase 1 — Native state/persistence — COMPLETE

Canonical state, `GameSession`, save envelope, atomic persistence and migration/recovery foundation.

### Phase 2 — Resources/world/survey — COMPLETE / ACCEPTED

Calendar, resource catalogue, Contract 01 data, landing terrain, discovery and survey domain foundation.

### Phase 3 — Colony survival/daily simulation — COMPLETE / ACCEPTED

Production, demand, workforce, Food/Power/Fuel survival, mortality and deterministic simulation clock.

### Phase 4 — Buildings/Power/Industry/HQ — COMPLETE / ACCEPTED

Buildings, extraction development, Spaceport, Power allocation, Headquarters command/continuity and related save coverage.

### Phase 5 — Native game UI/map/design foundation — COMPLETE / ACCEPTED

Native design primitives, production map, map interactions, responsive shell and physical-device acceptance.

### Phase 6 — Trade/contracts/commercial events + N05 source refresh — IMPLEMENTED / DEVICE ACCEPTANCE IN PROGRESS

Implemented/regression-green:

- Corporate Ship buy/sell/colonist transfer;
- reserves/quality/pricing/capacity;
- Contract 01 commercial lifecycle;
- buyer contracts and recurring collections;
- corporate event queue and Game Log;
- current source refresh to `5.13.22`;
- N05 founding ship/colony split;
- ship residents and self-contained ship Industry;
- explicit transfers and resident-transfer safety gates;
- founding handover UI;
- stacked S/C HUD;
- refined Colony Control/commercial presentations;
- native save v6 compatibility.

The current `0.7.2-migration` APK is the Phase 6 device-validation build. Do not mark Phase 6 accepted or merge its exact head to `main` until hands-on acceptance is explicitly given.

### Phase 7 — Single-Colony Gameplay and UI Parity Closure — NEXT APPROVED PHASE

Detailed authoritative scope: `docs/migration/PHASE_7_SINGLE_COLONY_PARITY.md`.

The reason for inserting this phase is deliberate: **make one colony function and feel substantially complete before multiplying incomplete workflows across a portfolio.**

Ordered slices:

1. surveying/scanning parity;
2. rich building/extraction panels;
3. Headquarters/Colony Control completion;
4. founding/player ship panel;
5. Corporate Ship/Trade presentation parity;
6. Conglomerate Buyers Service parity;
7. Technology/Engineering required by one-colony progression;
8. resource detail/production visibility;
9. Spaceport panel;
10. warnings/attention system plus bug cleanup;
11. Contract/corporation/Game Log parity sweep;
12. coherent single-colony edge-flow/device closure.

#### Warning audit requirement

Warnings must be treated as gameplay guidance rather than cosmetic messages.

The current web provides a prioritised persistent attention strip plus a one-shot critical survival warning for colony Food/Fuel and occupied-ship Food. The native game currently lacks equivalent typed/prioritised warning semantics and needs a proper parity pass.

A concrete source defect has already been identified during planning: the current web Housing-near-capacity warning uses **total population** instead of **planetary residents**, which can falsely warn while N05 residents remain aboard the founding ship. This should be regression-tested and corrected in the maintained web owner first where practical, then ported natively.

The Phase 7 warning slice must also audit:

- thresholds and severity;
- priority when several problems coexist;
- one-shot critical episode behaviour;
- clearing and re-entering warnings;
- stale/duplicate warnings after recovery or save/reload;
- direct action/navigation targets;
- ship vs colony inventory distinction;
- starvation/death countdown presentation;
- Housing/Power/workforce/Industry/Ore warning correctness.

### Phase 8 — Portfolio and Multi-Colony

The previous Phase 7 scope moves here.

Expected:

- Colonies/Portfolio hierarchy/navigation;
- authoritative portfolio state/lifecycle;
- colony creation/switch/removal/relocation as source behaviour requires;
- inactive-colony simulation;
- local state capture with shared global date;
- cross-colony events;
- individual colony loss vs portfolio-wide game-over;
- full save/reload/device-validated multi-colony flow.

Phase 8 must build on a completed one-colony interaction model rather than creating a second set of management surfaces.

### Phase 9 — Ships, Fleet Procurement, Expansion and Navigation

Build on N05 fleet foundations and the Phase 7 single-colony ship panel.

Expected:

- full fleet manager;
- general cargo/passenger/accommodation management;
- docked/orbiting/travelling states;
- factory-new catalogue/market/procurement;
- transport orders;
- star/system/planet navigation;
- current canonical Fuel/location/travel behaviour;
- separately approved future Veyrite wear/Fuel/travel additions when their roadmap work begins.

### Phase 10 — Remaining closure, Universe integration and production hardening

Sequence/split as implementation size justifies; do not combine unrelated work merely to preserve a phase number.

Remaining closure includes as applicable:

- any Company/Corporation, onboarding/help or late overlay still outside prior slices;
- screen-by-screen parity matrix;
- final design consistency/accessibility/navigation audit;
- Universe snapshot sync/schema/reference/art validation;
- representative real web-save imports;
- deterministic long soak;
- screenshot/instrumentation coverage;
- lifecycle/performance/memory/leak profiling;
- production signing/Play upload key;
- R8/release verification;
- offline cold start;
- final feature matrix and hands-on approval;
- declare native implementation canonical.

## 12. Phase 7 acceptance principle

Phase 7 is intentionally player-outcome based rather than screen-count based.

A player must be able to run a fresh Contract 01 colony through normal play — establishment, scanning, construction, upgrades, required technology, ship management, shortages/warnings, Corporate Ship trade, buyers, Headquarters, contract/company/log state and save/reload — without encountering a major temporary Android screen or missing one-colony management workflow already present in the web game.

Representative acceptance must include at least one warning entering and clearing correctly, a Corporate Ship visit, a buyer interaction/collection, ship/colony transfer use and save/reload.

## 13. Branch and delivery workflow

For each remaining vertical slice:

1. start from the current accepted base unless explicitly continuing an unaccepted phase;
2. read Android `AGENTS.md` in full;
3. read this master guide and the relevant phase/decision records;
4. read source MineIT `AGENTS.md`;
5. inspect current HTML/view + CSS + UI/controller + domain/tests;
6. record/confirm the current source commit/version;
7. decide preserve/refine/replace;
8. identify the real missing native semantic owner/capability;
9. add parity/regression coverage first where useful;
10. implement gameplay only in canonical domain/application owners;
11. implement Compose using shared MineIT primitives;
12. add interaction/presentation coverage where appropriate;
13. run focused tests and full Android CI for significant checkpoints;
14. produce the signed-development APK artifact;
15. validate the complete slice hands-on;
16. update migration docs/status;
17. merge only the exact accepted head.

Avoid giant branches containing several unrelated half-finished phases.

## 14. Definition of done for a migrated feature/screen

A feature/screen is migrated only when applicable items are true:

- [ ] source HTML/view inspected;
- [ ] source CSS inspected;
- [ ] active UI/controller inspected;
- [ ] source domain owner/tests inspected;
- [ ] preserve/refine/replace decision understood;
- [ ] native semantic owner clear;
- [ ] gameplay implemented without UI truth leakage;
- [ ] save/import/migration handled where required;
- [ ] recognisable source hierarchy/workflow preserved unless deliberately changed;
- [ ] bug fixes have regression coverage where practical;
- [ ] important enabled/disabled/warning/error states covered;
- [ ] touch/Back/accessibility behaviour is appropriate;
- [ ] focused tests pass;
- [ ] full required CI passes for significant work;
- [ ] migration documentation is updated;
- [ ] representative physical-device validation is complete before phase acceptance.

## 15. Cutover rule

The web game remains canonical until final native cutover is explicitly approved.

Native cutover requires:

- all required gameplay slices migrated;
- real save compatibility/import confidence;
- Universe/offline content path complete;
- no major accidental UI hierarchy drift;
- production signing/release validation;
- performance/lifecycle confidence;
- final physical-device approval.

Only then should MineIT Android become the canonical maintained game implementation.

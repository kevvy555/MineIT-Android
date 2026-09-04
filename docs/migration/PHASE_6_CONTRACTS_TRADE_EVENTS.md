# Phase 6 — Contracts, Trade, Commercial Events and Current-Source Refresh

**Status:** Implementation/regression and web-layout refinement implemented; revised physical-device validation still required before acceptance  
**Started:** 4 September 2026  
**Android branch:** `feature/migration-phase-6`  
**Accepted Phase 5 base:** `b9364a4a8d7e9566df6e16d5f61e93f83bc6cd8d`  
**Current validation-code checkpoint before this documentation update:** `41a3859957cc382d74b0e47093ff41e9a4cb7c54`  
**Current validation build:** `0.7.2-migration` / version code `14`  
**Native save format:** `6`  
**Current web behavioural + UI-layout baseline:** `kevvy555/MineIT` `develop` at `075b3d82fd88334b20b3cfe7d6e2731c8d840533` / game `5.13.22` / web save `16`

## Goal

Restore the current Contract 01 commercial loops and consequences natively, keep gameplay truth in domain/application owners rather than Compose, and keep the active Phase 6 branch aligned with material source changes that land on web `develop` before native acceptance.

Phase 6 originally covered Corporate Ship trade, import/export capacity and reserves, colonist transfer, contract scoring/deadline decisions, corporate events, buyers and recurring collection ships, reputation consequences and Game Log presentation.

During the Phase 6 UI-refinement period the web baseline advanced materially from game `5.13.15` to `5.13.22`. The most important new first-colony behaviour is the N05 founding-ship/colony-establishment flow. Because it changes the Contract 01 start state and the behaviour of already-migrated colony survival, Power, workforce and Industry systems, it is treated as a **required source-baseline refresh**, not postponed as a generic Phase 8 fleet feature.

Full ship travel, market/procurement, navigation and fleet gameplay remain Phase 8 work.

The post-migration resource overhaul also remains explicitly out of scope.

## Migration method

Player-facing work from this phase onward uses UI-led vertical slices:

1. inspect current web HTML/view, CSS, UI/controller, domain owner and tests together;
2. identify what should be preserved, deliberately refined or replaced;
3. confirm which native domain/application capability already exists;
4. migrate only missing canonical backend behaviour required by the slice;
5. recreate the recognisable web hierarchy/workflow in Compose;
6. apply deliberate Android refinements rather than incidental redesign;
7. add domain/regression and appropriate interaction/presentation coverage;
8. run full Android CI and signer verification;
9. validate the resulting APK on a physical device before acceptance.

Cross-cutting backend prerequisites may still be implemented first when a current source change affects several already-migrated systems, as N05 did.

## Source owners reviewed

Commercial Phase 6 work was based on the relevant canonical owners under `js/domain/`, Contract 01 data/configuration and current trade/quality/reserve tests, including:

- `js/core/config.js`;
- `js/domain/game-state.js`;
- `js/domain/trade-service.js`;
- `js/domain/contract-service.js`;
- `js/domain/corporate-event-service.js`;
- `js/domain/game-log-service.js`;
- `js/domain/reputation-service.js`;
- `js/domain/buyer-service.js`;
- `js/domain/spaceport-model.js`;
- `js/data/contracts.js`.

The UI-parity pass additionally treats matching `views/`, CSS and `js/ui/` files as first-class references. The current refinement specifically reviewed:

- `views/colony-control.html`, `css/adaptive-building-details.css` and the current Colony Control presenter in `ship-preparation-ui.js`;
- `corporate-trade-ui.js`, `quick-trade-ui.js` and `views/quick-trade-shell.html` plus the Sell/Buy/Colonists fragments;
- `contract-ui.js` and current contract decision/completion presentation;
- `buyer-ui.js` and the current Conglomerate Buyers Service catalogue/profile/collection presentation;
- current Game Log presentation/usage alongside its durable domain owner.

The N05 refresh reviewed the current source establishment path directly, including:

- the founding-ship state created by the current Contract 01 start;
- `ExpansionService.establishmentAssessment` and resident-transfer rules;
- `colony-establishment-ui.js`;
- `views/colony-establishment.html`;
- ship-preparation/HUD presentation owners;
- current source tests around ship-supported establishment and survival.

Both repository root `AGENTS.md` files were read before implementation.

## Native ownership

Phase 6 keeps gameplay truth out of Compose:

- `CorporateTradeService` owns Corporate Ship arrival/departure, buy/sell, visit capacity, reserves, pricing and Corporate Ship colonist transfer;
- `ContractService` owns Contract 01 goal scoring, deadline states, extensions, holdover/failure/renewal and liabilities;
- `BuyerService` owns deterministic offers, accepted buyer contracts, recurring collection ships, shipment resolution, relationships and reputation consequences;
- `CorporateEventService` owns durable global event ordering and deduplication;
- `GameLogService` owns durable monotonic game events;
- `CommercialDayService` coordinates commercial work after the canonical daily simulation transition;
- `PlayerFleetService` owns the durable corporation fleet facts required by N05 establishment;
- `ColonyEstablishmentService` owns the one-time founding-ship-to-colony establishment assessment/actions;
- `DailySimulationEngine`, `ColonyNetworkService` and Headquarters owners consume the same permanent fleet state for survival/network behaviour;
- `GameSession` remains the authoritative root state/persistence owner;
- Compose renders state and dispatches intents only.

The N05 fleet state is deliberately extensible for Phase 8 but does not pre-implement ship market/travel systems.

The Phase 6 UI refinement also removed the unused generic Trade/Contract production presentations from the shared commercial screen. Trade and Contract now each have one dedicated production Compose surface; Buyers/Game Log share only their navigation container.

## Corporate Ship trade parity

The migrated Corporate Ship loop retains the current source rules represented by native services/regressions:

- Contract 01 first Corporate Ship visit occurs at absolute day 181;
- normal recurrence is every 180 days;
- visit import/export capacities are pinned from arrival state using the current reputation curves;
- passenger capacity is 250 per visit;
- buy prices use current source value/markup rules;
- sell prices retain quality-band and processing multipliers;
- processing bonus remains capped at 50%;
- the colony-wide reserve protects the configured amount independently on each qualifying stock entry;
- general export consumes highest-value quality lots first;
- imported stock uses the current excellent/default quality band;
- trade uses the Basic Spaceport service gate;
- Corporate Ship export reputation is awarded once per relevant visit.

Cash remains `Double`, matching JavaScript Number semantics for fractional pricing.

### Current Corporate Trade presentation

The dedicated native Trade surface preserves the web quick-trade hierarchy:

1. Corporate Trade Ship / docked-arrival state;
2. Cash, Import, Export and Passenger visit summary;
3. Sell / Buy / Colonists modes;
4. four-row paging where applicable;
5. reserve-aware stock rows and category/all-sell actions;
6. Buy resource categories and reserve-shortfall actions;
7. colonist safe/hard transfer projections;
8. explicit `SHIP DEPARTS` action.

The web audit found two small native defaults that had drifted: the trade amount now starts at **10,000** rather than 100, and Buy opens on **Fuel** rather than All. The native 2×2 summary grid is retained as a deliberate responsive phone refinement of the web four-metric row.

## Corporate Ship colonist transfer

Corporate Ship passenger transfer remains owned by `CorporateTradeService` and is separate from N05 founding-ship resident movement.

The current hard limits remain around passenger capacity, Housing and Power/service availability. Food is a safety/convenience projection, not an invented hard transfer gate.

## Contract lifecycle

`ContractService` carries the current Contract 01 lifecycle needed for native completion/failure play:

- goal evaluation uses completed-day Food/Industry metrics;
- deadline state is deterministic from durable contract/date state;
- decisions survive save/load and process recreation;
- extension/holdover/failure/renewal paths are durable;
- applicable cash/liability/reputation consequences belong to the contract owner;
- pending decisions use the shared corporate-event queue.

### Current Contract presentation

The dedicated `ContractCommercialPanelScreen` was compared against current `contract-ui.js`. It already preserves the source information hierarchy closely enough that a rewrite would create risk without improving parity:

- colony/tier/contract identity and current state first;
- current Food / Industry / Population objectives and profit context;
- performance bands;
- pending action state;
- renewal/holdover/liability decisions.

It is therefore **retained**, with domain decisions continuing through `ContractService`/corporate events rather than being duplicated in presentation.

## Buyers and recurring collection ships

The buyer loop remains on permanent native buyer state:

- deterministic offers from canonical state;
- durable accepted buyer contracts;
- recurring collection attempts at +0, +5, +10 and +15 days;
- docked/orbital holding based on berth availability;
- minimum quality and qualifying stock calculations in `BuyerService`;
- shipment fulfilment, revenue, happiness, misses and lifetime revenue persisted;
- happiness loss feeds the shared reputation owner;
- repeated Red cycles/misses can terminate relationships;
- buyer shipments do not consume Corporate Ship export capacity;
- waiting, final-attempt miss and cancellation are explicit actions;
- pending buyer events recover from durable state after process recreation.

### Current Buyers presentation

The native Buyers surface now restores the recognisable `Conglomerate Buyers Service` hierarchy from current web `buyer-ui.js`:

- command-network terminal state with `NODE KPL-CN08` / Primary HQ link loss;
- corporate reputation value and canonical reputation level;
- existing commitments remain usable during network outage while **new contacts are blocked**;
- Current Contracts are separated from the Buyer Directory;
- relationship happiness uses Green / Amber / Red bands;
- next due date, ready quantity, lateness, fulfilment/miss counts and lifetime revenue are visible;
- directory supports All / Eligible / Locked filtering;
- each row exposes buyer/company, resource, quality, load, unit rate, frequency and reputation requirement;
- compact buyer profile exposes contract value/details before `ENTER CONTRACT`;
- active collection actions remain Transfer / Wait / Miss, with cancellation subject to existing domain rules.

The current native buyer data model does not yet contain the full Universe-backed portrait/role/home/ship-profile metadata used by the richer web profile. That content belongs with the planned MineIT-Universe/ship data work rather than being fabricated in Compose during parity migration.

## Corporate events and Game Log

Commercial blocking events use one durable queue with source-compatible priority:

1. emergency Food;
2. recovered ship/buyer event;
3. buyer;
4. contract decision;
5. Corporate Ship;
6. other/unknown.

Equivalent keys deduplicate. `CommercialDayService` returns pause state so the application layer can control the clock without presentation owning event truth.

The Game Log remains a compact reverse-chronological native list. The refinement reduced generic panel weight so date/type/id/message rows read as a log rather than a stack of dashboard cards.

## Daily orchestration

`CommercialDayService` remains intentionally narrow:

1. run `DailySimulationEngine.advanceDay()`;
2. stop commercial processing if the colony died;
3. process scheduled Corporate Ship arrival;
4. process buyer collection state/events;
5. evaluate the contract deadline against completed-day metrics;
6. enqueue resulting corporate events;
7. return completed-day metrics plus pause state.

This keeps rendering independent of simulation and avoids recreating web `app.js` as a monolithic native manager.

## N05 source-baseline refresh

### Contract 01 now begins ship-supported

Fresh native Contract 01 games now match the current web start:

- 120 colonists begin accommodated aboard the founding ship;
- 10 crew remain assigned to the ship;
- ship minimum crew is 10 and maximum crew is 40;
- founding ship accommodation capacity is 290;
- starter manifest begins aboard ship rather than appearing magically in colony storage:
  - Food: 1,300;
  - Fuel: 675;
  - Build: 520;
  - Ore: 260;
- colony inventory initially contains none of that manifest;
- `population` remains total living colonists, while planetary residents are derived as total population minus ship-accommodation assignments.

### Ship residents are a separate survival population

Ship residents:

- consume Food from ship inventory;
- do not consume colony Food until moved ashore;
- have their own starvation-day progression and mortality path;
- do not count as planetary workforce;
- do not create planetary life-support Power demand while aboard.

The previous native day-31 failure scenario caused by treating all 120 colonists as unsupported planetary residents is now covered by N05 regressions.

### Founding ship Industry is self-contained

The docked founding ship contributes 50 Industry while available.

That 50 Industry is crewed/self-powered and therefore:

- does not consume planetary workforce;
- does not consume planetary Power;
- is shown separately from colony-built Industry in the S/C HUD.

This corrects the earlier native approximation that charged ship Industry against colony Power/workforce.

### Explicit resource transfer

Ship and colony inventories are now separate durable owners.

Transfers preserve exact stock/resource quality bands. During the initial founding handover, the founding ship may bootstrap-unload supplies before normal powered Spaceport transfer services are available. After bootstrap conditions end, normal Spaceport service gates apply.

No stock is automatically teleported into the colony.

### Explicit resident transfer

Moving founding-ship residents ashore is manual and requires the current web conditions:

- the founding ship is docked;
- real built planetary Housing has free capacity;
- powered Spaceport transfer services are available;
- a real colony Power Plant has capacity;
- requested residents fit both ship-resident count and Housing capacity.

Before committing the transfer, the domain projects incremental life-support Power demand. If projected demand exceeds available Fuel-limited generation, the UI presents the current warning and requires explicit confirmation to continue.

The test-only `EstablishedColonyFixture` constructs historical established-colony state directly; it does not weaken these live transfer gates.

### Establishment assessment and handover UI

`ColonyEstablishmentService` owns the source-compatible establishment phase/checklist:

1. deploy Build + Fuel;
2. survey land;
3. establish Power + Housing;
4. move residents ashore;
5. establish Food;
6. establish Fuel;
7. replace ship Industry;
8. establish Headquarters.

The assessment exposes SHIP / HYBRID / COLONY / READY / INDEPENDENT phase semantics and per-step support status.

The Compose handover surface mirrors the web information hierarchy:

- `FOUNDING HANDOVER` / establish colony operations;
- S/C Food, Build, Fuel and Ore split;
- establishment checklist;
- ship residents, planet residents and ship-Food status;
- explicit ship-transfer controls;
- `BEGIN OPERATIONS • 1×`.

Beginning operations sets only `establishmentAcknowledged` and starts time at 1×. It does **not** complete Headquarters command handover. Until operations are acknowledged, attempts to start or manually advance simulation reopen the establishment flow instead.

### Authoritative stacked S/C HUD

The native game header follows the current source Ship/Colony split for already-migrated operational concepts:

- Housing: ship accommodation vs planetary Housing;
- Power: ship `SELF` vs colony generation/demand;
- Industry: ship support vs colony-built Industry;
- Workforce: ship crew/minimum vs colony free workforce;
- Food, Build, Fuel and Ore: ship (`S`) and colony (`C`) quantities.

This is presentation of canonical state, not duplicated UI-owned gameplay logic.

## Colony Control web-layout refinement

The earlier native Colony Details sheet was functionally rich but visually behaved like a generic stacked dashboard. Current web Colony Control is instead a compact command hub.

The revised native hierarchy is now:

1. Colony Control hero/status;
2. outage/handover alerts;
3. compact Overview metrics;
4. compact Operations grid in the source order: Command, Power, Workforce, Industry, Spaceport;
5. Koplin Deep Reach Corporation / command-network link context;
6. optional `SYSTEM DETAIL` expansion for the deeper Power/workforce/Industry/command metrics that Android already had.

Preserved:

- authoritative native metrics and warning logic;
- command/HQ continuity state;
- Android bottom-sheet/back behaviour where it remains appropriate.

Refined:

- four-column Overview on wider phone widths, two columns below 370dp;
- source-like compact operation tiles;
- deep diagnostic metrics no longer dominate normal play.

Not fabricated during this slice:

- Colony Services buttons whose destination feature is not yet natively complete. Portfolio/ship service navigation remains owned by the corresponding later vertical slices rather than adding dead controls.

Implementation commits:

- `949fdc310289c2c84a5c2669cebb9486c51274c6` — Colony Control hierarchy;
- `23b179d5344333ba40131111cf092c850040ae1a` — presentation regression coverage.

Android CI run `33918672340` / run 279 passed tests, APK assembly, signer verification and artifact upload for that checkpoint.

## Persistence — native save v6

The current native save format is **v6**.

The v5→v6 migration adds durable N05 fleet/establishment state while protecting existing Android players:

- existing colony stock remains ashore;
- existing residents remain ashore;
- old saves are not retroactively transformed into a fresh N05 start;
- a compatible durable founding ship/fleet record is introduced where required by current owners;
- earlier commercial/buyer/event/log/contract state remains intact.

Coverage includes direct old-save migration, cumulative migration and non-default current-state round trip.

## Regression coverage

Phase 6/N05 coverage includes:

- Corporate Ship arrival/next scheduling;
- visit capacities;
- reserve and quality/value export ordering;
- quality-banded pricing and fractional cash updates;
- Corporate Ship buy/cargo/cash behaviour;
- Spaceport service gating;
- one reputation award per Corporate Ship visit;
- Corporate Ship colonist transfer constraints;
- deterministic buyer offers and recurring collections;
- buyer shipment/revenue/reputation/relationship behaviour;
- buyer waiting/miss/termination;
- contract extension/failure/renewal/liability lifecycle;
- scheduled commercial orchestration;
- event recovery, priority and deduplication;
- monotonic Game Log IDs/dates;
- v5→v6 and cumulative native save compatibility;
- fresh N05 starter manifest ownership;
- 90-day all-aboard survival;
- former day-31 failure prevention;
- ship-resident Food/starvation behaviour;
- self-powered/self-crewed ship Industry;
- exact stock conservation during bootstrap unload;
- real Housing/Power/Spaceport resident-transfer gates;
- establishment acknowledgement remaining independent of command handover;
- Colony Control presentation hierarchy/status/shortage regressions;
- all earlier Phase 0–5 regression suites remaining active.

## CI and validation checkpoints

Earlier commercial implementation checkpoint `9380c130c63ca540e2e778810ff0cac570cd48af` passed Android CI run `33897074590` / run 254.

N05/UI checkpoints:

- N05 UI commit `35a03b32a0f4360a6f50e1a1e870ef9fced3bdc4` passed run `33912720951` / 275 with artifact `9952022146`;
- exact N05 documentation head passed run `33913292639` / 277;
- Colony Control checkpoint `23b179d5344333ba40131111cf092c850040ae1a` passed run `33918672340` / 279;
- Buyers/commercial-container checkpoint `b456e045f79399d656a069e4ae64b4a59138612d` passed run `33919412819` / 282.

The final `0.7.2-migration` code adds the audited Corporate Trade defaults/title and version bump after those checkpoints. The exact documentation head created by this update must pass full Android CI before it is used as the device-validation artifact.

None of these CI checkpoints constitute Phase 6 physical-device acceptance.

## Hands-on feedback and completion target

The earlier `0.7.0-migration` physical-device review proved the commercial/backend integration but identified excessive layout drift, particularly generic vertically stacked dashboard treatment where the web MineIT hierarchy is more compact.

The agreed correction remains:

- retain canonical Phase 6 backend/domain implementation;
- use current web `develop` HTML/views, CSS, UI controllers and domain owners together as the layout/behaviour reference;
- retain web hierarchy, compactness, primary control placement and recognisable workflows by default;
- apply deliberate native improvements for safe areas, accessibility, touch sizing, Back/navigation, sheets/dialogs and responsive sizing;
- do not introduce materially different layouts merely because Compose makes them convenient.

That implementation refinement is now complete for Colony Control, Corporate Trade, Contract, Buyers and Game Log. It still requires physical review before acceptance.

## Revised hands-on validation target

Install the latest **`0.7.2-migration`** artifact over the current migration build and validate at least:

### New game / N05 establishment

- choose a Contract 01 landing site;
- founding handover opens while simulation remains paused;
- HUD shows starter Food/Build/Fuel/Ore aboard ship and zero equivalent colony stock;
- all 120 residents initially appear aboard ship;
- ship 50 Industry appears without planetary Power/workforce demand;
- bootstrap-unload Build/Fuel and confirm S/C quantities move without loss;
- construct Power + Housing using unloaded materials;
- resident transfer remains blocked before the required Housing/Power/Spaceport conditions;
- after conditions are met, move residents ashore and verify S/C Housing/workforce/Power values update;
- exercise the projected-Power-shortage confirmation path if available;
- `BEGIN OPERATIONS • 1×` acknowledges establishment and starts simulation;
- restarting/reloading preserves ship inventory, residents and acknowledgement state;
- Headquarters command handover remains a later distinct requirement.

### Existing save compatibility

- an existing `0.7.0` or `0.7.1-migration` save loads successfully;
- its existing colony stock and residents remain ashore rather than being moved back aboard;
- normal map/survey/building/commercial play remains intact.

### Colony Control

- opening Colony Control presents the compact hero → Overview → Operations hierarchy rather than the old stack of large diagnostic cards;
- Command/Power/Workforce/Industry/Spaceport status is readable without scrolling through deep details;
- `SYSTEM DETAIL` exposes the deeper metrics when wanted;
- HQ outage/recovery and founding-handover warnings remain prominent;
- Koplin link state matches the real command network.

### Corporate Trade

- Cash / Import / Export / PAX summary is immediately visible when docked;
- Sell / Buy / Colonists tabs follow the current web workflow;
- default trade amount is 10K and Buy opens on Fuel;
- reserve/category/all-sell actions and four-row paging remain usable on the phone;
- buy/sell/reserve updates stock/cash correctly;
- `SHIP DEPARTS` is explicit and coherent.

### Contract

- contract identity/state is first;
- Food / Industry / Population objectives and profit are clear;
- performance bands and pending decisions are visible;
- renewal/holdover/liability choices still route through durable contract events correctly.

### Buyers

- Conglomerate Buyers Service shows network state, KPL-CN08 context and reputation level;
- HQ/network outage blocks new contacts but does not disable existing commitments;
- Current Contracts show happiness band, due date, readiness/late state and relationship history summary;
- Buyer Directory All / Eligible / Locked filters work coherently;
- buyer profile shows resource/quality/load/rate/value/frequency/rep requirement before entering a contract;
- collection Transfer / Wait / Miss / Cancel behaviour remains correct.

### Game Log and lifecycle

- Game Log remains compact and readable;
- blocking commercial events pause/resume coherently;
- closing/reopening preserves commercial state and reconstructs unresolved events.

## Exit gate

- [x] Corporate Ship trade rules migrated into canonical native owner.
- [x] Buy/sell/capacity/reserve/quality pricing regression coverage passes.
- [x] Corporate Ship colonist transfer migrated without inventing a Food hard gate.
- [x] Contract goal/deadline/decision/lifecycle rules migrated.
- [x] Corporate event ordering/deduplication/recovery migrated.
- [x] Buyer offers/contracts/recurring collections/relationship outcomes migrated.
- [x] Game Log durable state and native presentation foundation migrated.
- [x] Daily commercial orchestration uses completed-day metrics.
- [x] Native save v5 commercial migration coverage added.
- [x] current source baseline refreshed to `075b3d82` / game `5.13.22`.
- [x] N05 durable fleet/start-state/survival/network semantics migrated.
- [x] Native save v6 migration protects existing Android saves.
- [x] N05 founding handover and S/C HUD implemented.
- [x] N05 domain/regression coverage passes.
- [x] initial Phase 6 physical-device review completed and layout drift identified.
- [x] Colony Control recreated around the current web hierarchy with deep diagnostics deliberately retained behind `SYSTEM DETAIL`.
- [x] Corporate Trade audited against current quick-trade shell/flow and aligned where drift remained.
- [x] Contract presentation audited against current web and retained rather than unnecessarily redesigned.
- [x] Buyers recreated around the current Conglomerate Buyers Service hierarchy and network/reputation context.
- [x] Game Log compact presentation refinement completed.
- [x] unused duplicate generic Trade/Contract production presentations removed.
- [x] Colony Control checkpoint passed full CI/signing on run 279.
- [x] Buyers/commercial-container checkpoint passed full CI/signing on run 282.
- [ ] exact final `0.7.2-migration` documentation head passes full Android CI/signing/artifact upload.
- [ ] Colony Control / Trade / Contract / Buyers / Game Log revised layouts accepted on physical device.
- [ ] N05 founding handover/S-C HUD receives physical-device validation.
- [ ] revised Phase 6 validation APK receives hands-on acceptance.

Only after that hands-on acceptance should Phase 6 be marked complete/accepted and promoted to `main`. Phase 7 then begins from the exact accepted baseline using the same UI-led vertical-slice method.

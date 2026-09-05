# Phase 7 — Single-Colony Gameplay and UI Parity Closure

**Status:** Planned / approved direction  
**Created:** 5 September 2026  
**Predecessor:** Phase 6 commercial/N05 implementation and device refinement  
**Source reference:** current `kevvy555/MineIT` `develop` at the active migration baseline  
**Target:** `kevvy555/MineIT-Android`

## Purpose

Before MineIT expands into multi-colony gameplay, finish the experience of operating one colony so the native game feels and functions like MineIT rather than a collection of simplified migration surfaces.

The phase is complete only when a player can start Contract 01, establish the colony, survey, build, upgrade, research, trade, manage buyers, manage the founding ship, respond to shortages/events, progress the contract and save/reload without encountering a major placeholder, simplified management surface or missing single-colony workflow that already exists in the maintained web game.

This phase deliberately moves ahead of the previously planned Portfolio/multi-colony phase. Multi-colony now follows single-colony parity closure.

## Governing migration method

The existing UI-led parity method remains mandatory.

For **every player-facing slice**:

1. inspect the current web HTML/view structure;
2. inspect the CSS that creates layout, hierarchy and responsive behaviour;
3. inspect the active `js/ui/` controller/presenter behaviour;
4. inspect the relevant domain owner and tests;
5. compare the existing native implementation and identify what is missing or semantically different;
6. explicitly classify changes as **preserve**, **minor deliberate refinement** or **replace because the source is defective/unsuitable**;
7. preserve the recognisable web hierarchy, information ordering, terminology and workflow by default;
8. use Android-native improvements only where they are low-risk and genuinely improve safe areas, touch targets, accessibility, Back behaviour, typography, responsive sizing, dialogs/sheets or similar platform concerns;
9. keep gameplay truth in domain/application owners, never in Compose;
10. add regression/interaction coverage and validate the complete slice on a physical device.

The target is not pixel-for-pixel DOM duplication. A player who knows the web game should, however, immediately recognise the same feature and understand the same workflow.

## Scope and implementation order

### 7.1 Surveying and scanning parity — first priority

Surveying is one of the most frequent game interactions and must be brought close to current web behaviour before broader expansion.

Review and migrate together:

- scan/survey slot count and active/queued state;
- single-tile scan interaction;
- hold/drag or equivalent multi-selection interaction;
- multi-select queueing;
- scan progress presentation;
- active vs queued distinction;
- resurvey/history behaviour;
- scanning technology effects;
- sector naming and selected-sector context;
- map visual state for unknown, queued, active, complete and resurveyable sectors;
- queue limits/order and disabled reasons;
- current web scanning HUD/status information;
- relevant touch/mobile behaviour.

Minor Android refinements are allowed, but the normal player workflow and information density should remain recognisable.

### 7.2 Building and extraction information panels

Bring the native building/site detail experience up to current `adaptive-building` parity.

Applicable building/site panels should expose, where relevant:

- building/site image, name, family and level;
- current contribution/output;
- colony-wide installed/effective capacity where useful;
- staffing requirement and delivered staffing factor;
- Power requested/delivered/factor and priority;
- resource quality;
- resource stock;
- reserve/resource remaining and estimated life;
- renewable condition and harvest intensity;
- overdrive/operating mode and risk exposure;
- shutdown/depletion/emergency state;
- next-level improvement;
- Build/Ore/Technology/Power/Industry/workforce upgrade requirements;
- clear ready/blocked/max-level state;
- upgrade action;
- demolition implications and confirmation.

The panel should explain *why* a facility is underperforming rather than only expose a number.

### 7.3 Headquarters and Colony Control completion

The improved Phase 6 Colony Control hierarchy remains the foundation, but the single-colony flow must be completed and checked against current web Headquarters behaviour.

Review:

- Headquarters image/name/level/status;
- command capacity and load;
- staff / required staff;
- Power requested/delivered;
- Primary vs Expansion role;
- make-primary eligibility/action;
- command efficiency, bonus and overload penalty;
- Conglomerate network state;
- outage/recovery continuity and effective output;
- first-departure handover status;
- upgrade requirements/action;
- Primary-HQ demolition warning/consequences;
- Colony Control entry points;
- Koplin terminal/service entry points that are actually natively available.

Do not add dead buttons for later-phase services.

### 7.4 Founding/player ship panel

Complete the single-colony docked-ship control surface without prematurely implementing full interstellar expansion.

Required information/actions include, where supported by current source behaviour:

- ship identity/class/status/location;
- docked/landed state;
- accommodation capacity;
- residents aboard vs ashore;
- crew/minimum/maximum crew;
- Food stock and resident Food runway;
- Fuel stock;
- cargo/manifest by category and quality band where applicable;
- total load/capacity;
- founding-ship self-powered Industry contribution;
- ship↔colony resource transfer;
- resident transfer and Power/Housing warning confirmation;
- crew/passenger management required for single-colony operation;
- establishment/handover state;
- departure readiness information where already inside migrated semantics.

Full star-map navigation, factory ship purchasing and general interstellar travel remain later work.

### 7.5 Corporate Ship and Trade UI parity

The Corporate Ship should feel like an arriving ship/event, not merely a generic commercial menu.

Review and mirror the current web quick-trade flow:

- Corporate Ship identity/arrival context;
- blocking/docked state and pause behaviour;
- Cash, Import, Export and Passenger capacity summary;
- Sell / Buy / Colonists modes;
- resource category filtering and paging;
- stock, quality and price visibility;
- colony reserve behaviour and shortfall actions;
- buy quantities and costs;
- sell quantities and revenue;
- quality-aware sale behaviour;
- colonist transfer projections and hard/safe limits;
- explicit `SHIP DEPARTS` action;
- post-departure state and event logging.

Use the existing Phase 6 domain services; do not duplicate pricing/trade rules in presentation.

### 7.6 Conglomerate Buyers Service parity

Refine the Buyers experience to closely follow current `conglomerate-buyers` hierarchy and workflow.

Required review includes:

- terminal/network state and Primary-HQ link context;
- corporate reputation value and level;
- Current Contracts separated from Buyer Directory;
- relationship Green / Amber / Red state;
- next due/collection date;
- ready quantity;
- lateness and attempt state;
- fulfilment/miss counts;
- lifetime revenue;
- All / Eligible / Locked directory filtering;
- buyer/company/resource/quality/load/unit-rate/frequency/reputation requirement;
- buyer profile/contract review before entering a contract;
- Transfer / Wait / Miss collection actions;
- cancellation rules;
- network-outage rule: existing commitments continue, new contacts are blocked.

Use canonical Universe-backed data when it is already available through an approved native owner. Do not fabricate missing portrait/lore metadata in Compose.

### 7.7 Technology and engineering progression needed by one-colony play

Move the technology/engineering capability required by normal single-colony progression forward from the old late closure phase.

At minimum review the current web flow for:

- Housing Technology;
- Power Technology;
- Food Production Technology;
- Industry Technology;
- Mining/Extraction Technology;
- Scanning/Prospecting Technology;
- unlock/current/next-level visibility;
- costs and acquisition/commissioning state;
- local vs company capability where current gameplay distinguishes them;
- engineering deployments needed by currently migrated upgrades.

Only migrate technology/engineering required to make the existing single-colony loop coherent. Do not pull unrelated late-game systems forward.

### 7.8 Resource detail and production visibility

Provide a proper detail route for Food, Build, Fuel and Ore so the compact HUD can stay compact.

Where applicable expose:

- Ship (`S`) stock;
- Colony (`C`) stock;
- production/day;
- consumption/day;
- surplus/deficit;
- runway;
- relevant quality bands/resources;
- reserve amount;
- production sources and major consumers;
- shortage reason and useful navigation/action links.

### 7.9 Spaceport panel

Complete the Spaceport as a coherent single-colony service surface:

- operational state;
- Power factor/reason;
- berth capacity and occupancy;
- service slots;
- cargo/passenger throughput;
- Corporate Ship presence;
- player ship presence;
- transfer availability/reasons;
- links to ship/trade functions that actually exist.

### 7.10 Colony warnings, attention system and bug cleanup

Warnings are gameplay guidance, not decoration. This slice must be audited against current web behaviour **and checked for defects before porting**.

#### Current web behaviour reviewed

The web game currently has two complementary warning layers:

1. a persistent, clickable `attention-strip` with good/warn/bad severity and a direct navigation/action target;
2. a one-shot critical survival modal for colony Food, colony Fuel and occupied-ship Food when their critical runway threshold is entered.

Current attention priority includes:

- colony lost;
- landing-site selection;
- Corporate Ship docked;
- occupied-ship Food low/critical/starvation/deaths;
- colony Food low;
- Power shortage;
- colony Fuel low;
- workforce shortage;
- Industry overload;
- Housing near capacity;
- Ore low;
- unacknowledged colony establishment;
- stable/no-immediate-problem state.

The critical survival warning currently triggers:

- colony Food at 10 days or less;
- colony Fuel at 10 days or less;
- occupied-ship Food below 10 days;
- once per continuous critical episode, resetting after the condition clears.

The critical modal explicitly reminds the player that ship and colony inventories are separate.

#### Native gap found during planning audit

The current native surface does **not** yet provide equivalent semantics:

- `statusMessage` is a transient generic strip rather than an authoritative prioritised attention state;
- severity is inferred from message text instead of an explicit warning model;
- there is no source-equivalent critical survival modal/episode guard;
- Housing has no near-capacity HUD warning tone;
- occupied-ship Food runway is displayed but does not yet reproduce the source 30-day attention / under-10-day critical treatment;
- native HUD Power/Industry colouring currently uses simplified thresholds and must be compared against source warning meaning rather than assumed equivalent;
- warning actions do not yet consistently navigate directly to the affected resource/system.

These are Phase 7 work items, not reasons to move gameplay rules into Compose.

#### Source bug identified during planning audit

Current web `attentionStatus()` calculates **Housing near capacity using total colony population (`s.pop`) rather than planetary residents**. Under N05, residents still accommodated aboard the founding ship must not consume planetary Housing. This can therefore produce a false Housing warning during establishment.

Before native warning parity is finalised:

1. reproduce this condition in a source regression test;
2. correct the maintained web canonical warning to use planetary resident count;
3. verify displayed free spaces use the same planetary value;
4. port the corrected semantic rule to native;
5. add native regression/presentation coverage.

The broader warning audit must similarly check thresholds, priority, clear/re-enter behaviour, save/reload behaviour, stale warnings after recovery, duplicate warnings and navigation targets. Any clear source bug should be corrected in the canonical source first where practical, then migrated.

#### Warning architecture direction

Native should expose a small typed/domain-or-application-derived attention model, for example severity/title/detail/action plus critical-episode state where persistence is genuinely required. Compose should render that model and dispatch its action; it should not derive survival rules from text.

Do not over-engineer a generic notification framework. KISS applies.

### 7.11 Contract, corporation and Game Log parity sweep

These are substantially implemented and should be refined rather than rebuilt.

Check:

- Contract 01 objective/progress hierarchy;
- deadline, extension, holdover, failure and renewal decisions;
- company cash/reputation context;
- event pause/resume behaviour;
- Game Log density/readability/date/type/message ordering;
- direct navigation between relevant single-colony surfaces.

### 7.12 Single-colony edge-flow closure

Validate the complete game rather than isolated screens:

- landing-site selection;
- founding handover;
- starting/stopping time;
- emergency Food;
- ship Food shortage/starvation;
- colony Food/Fuel shortage;
- Power shortage;
- workforce/Industry throttling;
- Corporate Ship arrival/trade/departure;
- buyer collection event;
- contract decision;
- HQ outage/recovery;
- colony death/game-over;
- save/reload while normal, paused and inside recoverable durable event states;
- Android Back/dialog behaviour.

## Out of scope for Phase 7

Do not pull these forward merely to make the phase larger:

- Portfolio/multi-colony simulation and switching;
- full fleet manager across many ships;
- factory-new ship market/procurement;
- general transport orders;
- full star/system/planet navigation;
- interstellar travel implementation beyond single-colony ship readiness semantics;
- final approved future Veyrite wear/Fuel/travel redesign;
- full resource overhaul/refining/manufacturing catalogue;
- production hardening/cutover work that belongs at the end of migration.

## Revised roadmap after this decision

- **Phase 7 — Single-Colony Gameplay and UI Parity Closure** — this document.
- **Phase 8 — Portfolio and Multi-Colony** — previous Phase 7 scope moves here.
- **Phase 9 — Ships, Fleet Procurement, Expansion and Navigation** — previous Phase 8 scope moves here and builds on the N05 fleet foundation plus the completed single-colony ship panel.
- **Phase 10 — Remaining/Universe/production closure** — remaining slices, Universe bundling validation, UI consistency closure and production hardening are sequenced after the core game is functionally complete. This may be split further if implementation size justifies it; do not combine unrelated work merely to preserve a phase number.

## Acceptance criteria

Phase 7 is not accepted merely because all screens exist.

A physical-device acceptance run must demonstrate a coherent one-colony game:

1. start a fresh Contract 01;
2. select a landing site and complete/acknowledge founding handover;
3. survey individual and multiple sectors, including queue/progress behaviour;
4. inspect discovered resources and built facilities through rich detail panels;
5. build and upgrade Power/Housing/Industry/extraction facilities with understandable requirements;
6. use Technology/Scanning progression required by those upgrades;
7. move founding residents/supplies and manage the docked founding ship;
8. inspect and resolve at least one warning/shortage path and verify it clears correctly;
9. interact with Headquarters/Colony Control;
10. receive and use the Corporate Ship trade flow;
11. inspect/enter/manage a buyer contract and resolve a collection event;
12. inspect contract/company/Game Log state;
13. save, terminate/restart and reload without losing or misrepresenting the above state.

Automated tests must cover the same semantic boundaries where practical, including warning threshold/priority/regression scenarios and the N05 Housing-warning bug.

## Definition of done for each slice

A slice is done only when:

- source HTML/view inspected;
- source CSS inspected;
- active source UI/controller inspected;
- source domain/tests inspected;
- preserve/refine/replace decision understood;
- native semantic owner identified;
- missing canonical behaviour implemented in domain/application code;
- Compose mirrors the recognisable source hierarchy/workflow;
- no gameplay truth is duplicated in presentation;
- bug fixes have regression coverage where practical;
- interaction/presentation coverage exists where needed;
- focused tests pass;
- full Android CI/signing checks pass for significant checkpoints;
- migration documentation is updated;
- representative physical-device review is complete before final phase acceptance.

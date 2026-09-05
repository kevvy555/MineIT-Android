# Phase 7 — Single-Colony Gameplay and UI Parity Closure

**Status:** In progress — warning/attention and surveying/scanning slices implemented and CI-green; device validation pending  
**Created:** 5 September 2026  
**Started:** 5 September 2026  
**Android branch:** `feature/migration-phase-7`  
**Predecessor:** Phase 6 commercial/N05 implementation and device refinement  
**Source reference:** current `kevvy555/MineIT` `develop` at `075b3d82fd88334b20b3cfe7d6e2731c8d840533` / game `5.13.22` / web save `16`  
**Source warning-correction branch:** `feature/android-parity-warning-fixes` at `25d3f02f114f6cdc92534f3190554351073bc946` / game `5.13.23`  
**Target:** `kevvy555/MineIT-Android`  
**Current validation build:** `0.7.3-migration` / version code `15`  
**Native save format:** `6`

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

## Implementation progress

### Warning/attention checkpoint — implemented / CI-green

The first Phase 7 slice audited the maintained web attention system rather than simply copying native status text.

Source behavior confirmed:

- persistent prioritised `attention-strip` with good/warn/bad severity and direct action target;
- critical Food/Fuel/ship-Food survival modal;
- colony Food and Fuel critical at 10 days or less;
- occupied-ship Food critical below 10 days;
- one modal per continuous critical episode, resetting after recovery;
- ship Food warning has priority over colony Food because ship and colony inventories are separate.

A real maintained-web N05 defect was found and fixed on `feature/android-parity-warning-fixes`:

- Housing-near-capacity incorrectly used total colony population and therefore counted residents still aboard the founding ship against planetary Housing;
- source commit `25d3f02f114f6cdc92534f3190554351073bc946` changes both warning ratio and free-space copy to use planetary residents;
- source game version bumped to `5.13.23` on that correction branch;
- source CI run `33945700891` / run `1356` passed.

Native implementation:

- `ColonyAttentionPolicy` owns typed priority/threshold semantics outside Compose;
- `CriticalResourceEpisodeTracker` owns one-alert-per-critical-episode behavior;
- persistent native attention strip mirrors source severity/title/detail/action hierarchy;
- critical resource dialog mirrors the source Food/Fuel/ship-Food hierarchy and inventory-separation warning;
- the clock pauses for an active critical alert and cannot resume until acknowledgement;
- attention actions route to the affected resource/system where that native destination exists;
- N05 Housing semantics use planetary residents only;
- POWER, INDUSTRY and HOUSING map focuses were added for direct warning navigation.

Implementation commits:

- `c19bd0dc7427b8d59d7ce8795a7a1755b03d171f` — typed attention policy and regression tests;
- `9987e43ab7c15cc77482e394dedc85656c34cfa1` — attention strip, critical warning UI and application wiring.

Android CI run `33946047112` / run `297` passed unit/regression tests, debug APK assembly, persistent development signer verification and artifact upload.

Remaining warning cleanup before Phase 7 acceptance:

- player-ship warning target will open the proper ship panel after slice 7.4 instead of using establishment as the temporary destination;
- audit the map `PROBLEMS` Housing calculation for the same N05 planetary-resident rule;
- prevent a critical-resource modal from competing with an already-open blocking corporate event;
- re-check begin-establishment speed behavior when a critical alert is simultaneously entered;
- physical-device threshold/clear/re-entry/navigation checks.

### Surveying/scanning checkpoint — implemented / CI-green

The current web survey interaction/domain were compared against native before changes. The native domain already matched most core rules, including scanning levels/slots, queue ordering, deterministic discovery, 50% resurvey time, same-level no-reroll behavior and center-tile exemption. The main drift was interaction/presentation.

Native parity changes now include:

- tapping a currently surveyable sector immediately queues it, matching current web routine play;
- drag selection begins after normal drag slop rather than requiring a long press;
- a drag must begin on a surveyable tile and only surveyable tiles join the drag selection;
- the selected drag set is automatically queued on release;
- active unsurveyed tiles show `SCANNING` plus remaining days;
- queued unsurveyed tiles show `QUEUED`;
- a compact scan HUD shows scanning technology level, active/slot count, queued count, lead task, remaining days and progress;
- older completed scans show the source-style yellow `?` resurvey opportunity when `lastScannedAtLevel < currentScanningLevel`;
- the resurvey marker is derived only from scan history/technology and never leaks hidden resource truth;
- resurvey state remains visible over a developed/resource tile rather than replacing its primary tile identity;
- accessibility copy distinguishes survey, queued survey, resurvey and queued resurvey.

Implementation commit:

- `974f616f81cf65c07bdcddfb3f931f889e33bc4c` — survey map interaction/presentation parity and regression coverage.

Android CI run `33946302610` / run `298` passed unit/regression tests, debug APK assembly, persistent development signer verification and artifact upload.

Device checks still required:

- tap unknown tile queues exactly once;
- drag several unknown tiles without a long hold and verify auto-queue on release;
- scan HUD remains readable on the target phone and progress advances correctly;
- after a Scanning Technology increase, older scanned tiles visibly show yellow resurvey `?` without leaking whether a hidden deposit exists;
- tapping/dragging a resurvey candidate uses the expected shorter resurvey duration;
- center Spaceport/founding-ship sector never becomes resurveyable.

## Scope and implementation order

### 7.1 Surveying and scanning parity — implemented / device validation pending

Surveying is one of the most frequent game interactions and must be close to current web behavior before broader expansion.

Parity scope includes:

- scan/survey slot count and active/queued state;
- single-tile scan interaction;
- drag multi-selection and auto-queueing;
- scan progress presentation;
- active vs queued distinction;
- resurvey/history behavior;
- scanning technology effects;
- sector naming and selected-sector context;
- map visual state for unknown, queued, active, complete and resurveyable sectors;
- queue order/disabled reasons;
- current web scanning HUD/status information;
- touch/mobile behavior.

### 7.2 Building and extraction information panels — next implementation slice

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

The improved Phase 6 Colony Control hierarchy remains the foundation, but the single-colony flow must be completed and checked against current web Headquarters behavior.

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

Required information/actions include, where supported by current source behavior:

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
- blocking/docked state and pause behavior;
- Cash, Import, Export and Passenger capacity summary;
- Sell / Buy / Colonists modes;
- resource category filtering and paging;
- stock, quality and price visibility;
- colony reserve behavior and shortfall actions;
- buy quantities and costs;
- sell quantities and revenue;
- quality-aware sale behavior;
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

### 7.10 Colony warnings, attention system and bug cleanup — implementation checkpoint complete / cleanup + device validation pending

Warnings are gameplay guidance, not decoration. The source and native behavior are recorded in the implementation-progress section above.

Before Phase 7 acceptance, finish the remaining cross-slice warning navigation/competition cleanup and device checks; do not move warning semantics into Compose.

### 7.11 Contract, corporation and Game Log parity sweep

These are substantially implemented and should be refined rather than rebuilt.

Check:

- Contract 01 objective/progress hierarchy;
- deadline, extension, holdover, failure and renewal decisions;
- company cash/reputation context;
- event pause/resume behavior;
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
- Android Back/dialog behavior.

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
3. survey individual and multiple sectors, including queue/progress behavior;
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
- missing canonical behavior implemented in domain/application code;
- Compose mirrors the recognisable source hierarchy/workflow;
- no gameplay truth is duplicated in presentation;
- bug fixes have regression coverage where practical;
- interaction/presentation coverage exists where needed;
- focused tests pass;
- full Android CI/signing checks pass for significant checkpoints;
- migration documentation is updated;
- representative physical-device review is complete before final phase acceptance.

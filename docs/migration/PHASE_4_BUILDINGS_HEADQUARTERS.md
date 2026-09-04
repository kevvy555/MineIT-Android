# MineIT Android Migration — Phase 4 Buildings, Sites, Industry and Headquarters

**Phase:** 4 — Buildings, sites, Industry and Headquarters  
**Status:** Implementation and regression exit gate complete — `0.5.0-migration` hands-on validation pending  
**Started:** 4 September 2026  
**Source behavioural baseline:** `kevvy555/MineIT` commit `9e58983adaa7a15cd525451266ce9df3c17ae886`, game `5.13.15`, web save `16`  
**Android branch:** `feature/migration-phase-4`  
**Android version:** `0.5.0-migration` / version code `9`  
**Native save format:** `4`

## Goal

Make the permanent Phase 3 colony state meaningfully operable by migrating the current web game's local construction, extraction-site development, Power allocation, Industry infrastructure, Basic Spaceport and Headquarters command rules into canonical pure-Kotlin domain owners.

Phase 4 preserves the pinned web balance/rules. It does not implement the planned resource overhaul and it does not perform the production UI/design-system migration reserved for Phase 5.

## Source reviewed

Phase 4 was implemented against the pinned web owners and accepted A08a/A08b Headquarters decisions rather than inferred from the temporary Android validation UI. Relevant source includes:

- `js/core/config.js`;
- `js/domain/building-model.js`;
- `js/domain/development-service.js`;
- `js/domain/site-service.js`;
- `js/domain/colony-service.js`;
- `js/domain/resource-service.js`;
- the A08a Operational Headquarters departure-gate specification;
- the A08b Headquarters outage/recovery specification;
- existing web Power, colony economy, survival and building regression coverage.

The root `AGENTS.md` files in both Android and web repositories were read before implementation work.

## Canonical native ownership

Phase 4 does not introduce a parallel building model. It extends the permanent `WorldTile` / `TileDevelopment` representation created for Phase 3.

Canonical owners are now:

- `InfrastructureRules` — current capacity, Power, cost, command and upgrade-gate tables;
- `SiteOperationRules` — shared extraction workforce and Industry-load calculations;
- `ColonyDevelopmentService` — placement, extraction development, upgrades and demolition;
- `ColonyNetworkService` — one derived workforce/Industry/Power/HQ network used by simulation and presentation;
- `HeadquartersService` — Primary HQ, staffing, command capacity/load, departure gate and continuity;
- `SpaceportService` — Basic Spaceport operational consequences;
- `DailySimulationEngine` — consumes `ColonyNetworkService` instead of retaining duplicate Phase 3 network arithmetic.

Compose renders these results and dispatches intent. It does not own construction eligibility, Power priority or Headquarters rules.

## Durable state additions

Native save v4 adds durable state needed by migrated Phase 4 behavior:

- construction investment in Build and Ore on `TileDevelopment`;
- resource-covered state when a normal building occupies a resource tile;
- exhausted-resource identity needed after depleted-site clearing;
- Primary Headquarters identity;
- whether a Primary has ever been assigned;
- founding-ship command-handover completion;
- Headquarters outage/recovery history.

Derived values such as Power allocation, staffing, command load/capacity and operational status are recomputed rather than duplicated in the save.

`NativeSaveV3ToV4` is registered in the ordered migration chain. Existing Phase 3 saves rely on semantic defaults for the new fields and are covered by a real v3 JSON compatibility fixture plus a non-default v4 round-trip fixture.

## Current infrastructure curves

The native rules retain the pinned source values.

### Capacity

| Level | Housing | Power generation | Industry | HQ command capacity |
|---:|---:|---:|---:|---:|
| 1 | 160 | 75 | 100 | 16 |
| 2 | 360 | 165 | 230 | 36 |
| 3 | 650 | 300 | 420 | 64 |
| 4 | 1,050 | 500 | 700 | 100 |
| 5 | 1,600 | 800 | 1,100 | 150 |

### Headquarters minimum staff

| HQ level | Reserved staff |
|---:|---:|
| 1 | 5 |
| 2 | 10 |
| 3 | 18 |
| 4 | 28 |
| 5 | 40 |

### Building construction resources

Build costs by L1→L5:

- Housing: `55 / 95 / 165 / 285 / 480`;
- Power: `70 / 125 / 220 / 390 / 680`;
- Industry: `80 / 145 / 255 / 435 / 720`;
- Headquarters: `90 / 170 / 300 / 510 / 850`.

Ore costs by L1→L5:

- Housing: `0 / 10 / 25 / 50 / 90`;
- Power: `0 / 15 / 45 / 90 / 170`;
- Industry: `0 / 20 / 55 / 110 / 200`;
- Headquarters: `0 / 25 / 65 / 130 / 240`.

Terrain multipliers remain source-compatible. Standard buildings cannot be placed on lakes. Power retains its hill/mountain-specific multipliers; Housing and Industry retain their terrain penalties.

## Construction and demolition

`ColonyDevelopmentService` owns local construction actions immutably.

Current rules include:

- a sector must be surveyed before normal construction;
- the fixed `(0,0)` Spaceport tile cannot be used for another development;
- standard buildings cannot be placed on lakes;
- existing developments must be demolished before replacement;
- required Build/Ore is consumed from the generic quality-aware inventory;
- construction remains immediate under the current source lifecycle;
- upgrades are capped at L5;
- current local technology gates are enforced for Housing/Power/Industry;
- Headquarters upgrades use their dedicated current progression;
- building over a discovered resource marks the resource as covered rather than deleting it;
- demolition uncovers a non-exhausted resource;
- demolition returns `floor(25%)` of invested Build and Ore;
- depleted extraction developments retain enough state to be cleared/demolished correctly and preserve exhausted-resource identity.

Timed construction remains a separate future backlog item and has not been invented for Android.

## Extraction sites and upgrades

Extraction development reuses the Phase 2 resource catalogue and `ExtractionCompatibility` owner.

The current source rules are represented for:

- required Mining level/technology;
- resource-covered/unavailable checks;
- current terrain/distance/complexity/deposit-size Build cost;
- operational workforce availability;
- Food vs industrial upgrade Industry requirements;
- extractor-family installed-Power upgrade gates;
- Food Production technology for Food-site upgrades;
- additional workforce required by an upgrade;
- Build and Ore upgrade costs;
- L5 maximum level.

No future refined/manufactured resource system is introduced in this phase.

## One colony network owner

Phase 3 originally needed private workforce/Industry/Power calculations before player construction existed. Phase 4 removes that duplication.

`ColonyNetworkService` now derives the complete current colony operating network and is consumed by the daily simulation, ViewModel/presentation and Phase 4 services.

This preserves the architectural invariant that the UI and simulation cannot develop conflicting versions of the same Power or workforce result.

## Power priority and Fuel semantics

The source priority order is preserved:

1. staffed/constructed Headquarters, binary by facility;
2. Housing fixed demand + planetary life support, proportional within the band;
3. Food/Fuel extraction and supporting survival Industry, proportional;
4. Basic Spaceport, binary;
5. commercial Industry and Build/Ore extraction, proportional.

Other locked behavior:

- the founding ship contributes **zero colony Power**;
- the founding ship contributes the current **50 Industry** only while docked;
- generation is limited by beginning-of-day Fuel;
- Fuel extracted during a day is available only from the next day;
- partial Power scales proportional bands;
- the Basic Spaceport requires 10 Power and is binary;
- the daily simulation consumes the same network owner as construction/status presentation.

## Basic Spaceport

The fixed Basic Spaceport remains at `(0,0)` and does not consume command capacity.

When powered and the colony is operational it exposes its current operational capacities. When unpowered:

- arrivals remain possible;
- emergency departure remains possible;
- normal departure is disabled;
- trade/loading/transfers/engineering/ship-market actions are disabled.

Future trade/fleet phases will consume this same `SpaceportStatus` instead of duplicating the Power check.

## Headquarters command network

### Primary and staffing

- multiple Headquarters may exist;
- the first fully constructed/staffed HQ is automatically eligible to become Primary;
- a different fully constructed/staffed HQ may later be selected explicitly;
- each staffed HQ reserves its own minimum workforce;
- an understaffed HQ contributes no command capacity or positive bonus;
- Headquarters receives staffing priority before extraction workforce.

### Command load

Facility command points per level remain:

- Housing `1`;
- Power `2`;
- Food/Fuel/Build extraction `2`;
- Industry `3`;
- Ore extraction `3`;
- Headquarters `0`;
- fixed Basic Spaceport `0`.

### Capacity, bonus and overload

- active staffed/powered HQ capacity uses the `16 / 36 / 64 / 100 / 150` curve;
- positive HQ efficiency is `+2%` per HQ level with diminishing contributions for additional HQs (`1 / .5 / .25 / .125`) and a global `+15%` cap;
- command overload is soft rather than a construction hard-stop;
- overload penalty is proportional to the over-capacity ratio and capped at `-50%`;
- effective command efficiency is consumed by current resource production and survey progression through canonical domain paths.

## Founding-ship command handover

The A08a first-departure rule is preserved as a domain gate ready for the fleet domain to consume when Phase 8 migrates actual launch/travel actions.

For the founding ship's first departure from its newly founded colony:

- a Primary Headquarters is required;
- it must be fully constructed;
- it must be fully staffed;
- **Headquarters Power is not a departure requirement**;
- successful handover is persisted once and later departures are not trapped by this one-time gate.

The staged `foundingShipDocked` fact remains the temporary ownership bridge until the full fleet model is migrated. There is still no second native fleet implementation.

## Temporary/emergency ship command

While the founding ship is docked it can provide temporary/emergency command capacity equivalent to an L1 Headquarters (`16`).

Before permanent handover this is the normal establishment command source. After handover, if the Primary HQ becomes non-operational, the docked command-capable founding ship can keep local command functioning, but it **does not restore the conglomerate network**.

This distinction is important to A08b outage consequences.

## Headquarters outage and recovery

After command handover is established:

- an offline/non-operational Primary HQ begins an outage at a `10%` continuity penalty;
- the penalty worsens by `1 percentage point` per complete outage day;
- emergency ship command can provide local capacity but cannot make the conglomerate network available;
- when the Primary HQ is restored, the accrued penalty recovers linearly over `10 days`;
- at the end of recovery the continuity penalty returns to zero;
- continuity history is durable in native save v4;
- production and survey progression consume the continuity factor rather than approximating it in UI.

## Validation UI

The Phase 4 Compose changes are deliberately a validation surface, not the Phase 5 design-system rewrite.

A selected sector can currently expose:

- Build Power;
- Build Housing;
- Build Industry;
- Build Headquarters;
- develop a discovered extraction resource;
- upgrade an existing development;
- demolish an existing development;
- select a non-Primary Headquarters as Primary;
- survey an unsurveyed sector.

The map shows development type/level and the status panel exposes Power, Spaceport, command capacity/load/efficiency, continuity phase and command-handover readiness.

Production interaction polish, final confirmation/dialog treatment, final map art, long-press/drag multi-select and the full design system remain Phase 5 work. Gameplay eligibility remains domain-owned regardless of presentation.

## Regression coverage

Phase 4 adds JVM coverage that locks:

- building placement resource cost;
- resource covering by normal construction;
- demolition uncovering and 25% Build/Ore recovery;
- extraction development and installed Industry/Power upgrade gates;
- extraction upgrade resource consumption;
- Power allocation with Headquarters/life support ahead of Spaceport/commercial Industry;
- first-departure failure without a Primary HQ;
- first-departure success with a constructed/staffed but unpowered Primary HQ;
- Headquarters capacity, weighted command load, +2% bonus and overload penalty;
- founding-ship emergency command capacity;
- initial 10% HQ outage penalty;
- +1%/day outage degradation;
- linear 10-day recovery;
- conglomerate-network unavailability under emergency ship command;
- cumulative native v1→v4 save compatibility;
- explicit Phase 3 v3→v4 semantic-default migration;
- exact round-trip of non-default v4 development/HQ/outage state;
- all prior Phase 0–3 simulation, persistence, parity and soak tests remain active.

The Phase 4 regression head `606bff57b68e4420a6061235bb249fb915149109` passed Android CI run `33862758510`, including JVM tests, debug APK assembly, persistent signer verification and artifact upload.

## Explicit non-goals retained

Phase 4 does **not** implement:

- the resource-economy overhaul;
- refining/manufacturing/new resources;
- timed construction;
- final production Compose/map design (Phase 5);
- trade/buyers/contracts beyond existing foundation;
- portfolio/multi-colony orchestration;
- the full fleet/travel/ship-market domain (Phase 8);
- final web-v16 conversion;
- production Play signing.

## Phase 4 exit gate

- [x] one permanent physical development model is used by construction and simulation;
- [x] building placement/upgrade/demolition rules are native and domain-owned;
- [x] extraction development/upgrade gates are native and domain-owned;
- [x] current infrastructure/resource costs are locked;
- [x] Power priority/allocation and beginning-of-day Fuel remain source-compatible;
- [x] Basic Spaceport operational Power status exists for later domains to consume;
- [x] Headquarters Primary/staff/capacity/load/bonus/overload rules are represented;
- [x] first-departure handover rule is represented independently of UI;
- [x] temporary/emergency founding-ship command behavior is represented;
- [x] HQ outage degradation and ten-day recovery are represented;
- [x] daily simulation consumes the shared colony-network owner instead of a duplicate network engine;
- [x] native save v4 migration and round-trip coverage exists;
- [x] Headquarters/Power regression scenarios pass natively;
- [x] implementation regression head passed Android CI, signer verification and artifact upload;
- [ ] `0.5.0-migration` receives hands-on device validation.

After hands-on acceptance, fast-forward the exact accepted Phase 4 head to `main` before beginning Phase 5.

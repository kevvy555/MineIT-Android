# MineIT Android Migration — Phase 3 Daily Simulation and Colony Survival

**Phase:** 3 — Colony survival and daily simulation  
**Status:** Implementation in validation  
**Started:** 4 September 2026  
**Source behavioural baseline:** `kevvy555/MineIT` commit `9e58983adaa7a15cd525451266ce9df3c17ae886`, game `5.13.15`, web save `16`  
**Android branch:** `feature/migration-phase-3`  
**Android version:** `0.4.0-migration` / version code `8`  
**Native save format:** `3`

## Goal

Replace the Phase 2 survey-only day scaffolding with one canonical native daily simulation owner covering the current web game's resource collection, resource consumption, Power, Industry, workforce, survival and mortality behavior.

The simulation must remain pure Kotlin and deterministic with respect to its inputs. Android lifecycle/rendering/timers may request days but must never define gameplay results.

Phase 3 deliberately does **not** implement the planned resource overhaul or the player-facing building construction system. It creates the permanent state/rules those later systems consume.

## Source reviewed

Phase 3 was grounded against the pinned source rather than inferred from the validation UI. Relevant source owners/tests reviewed include:

- `js/core/config.js`;
- `js/data/technologies.js`;
- `js/domain/simulation-engine.js`;
- `js/domain/collection-service.js`;
- `js/domain/colony-service.js`;
- `js/domain/inventory-service.js`;
- `js/domain/resource-service.js`;
- `js/domain/building-model.js`;
- `js/domain/game-state.js` / runtime state normalization;
- `tests/colony-economy.test.js`;
- `tests/survival-rebalance.test.js`;
- `tests/industry-purpose.test.js`;
- `tests/startup-coherence.test.js`;
- `tests/long-simulation-soak.test.js`.

## Canonical daily owner

`domain/simulation/DailySimulationEngine.kt` is the canonical native owner for one complete colony day.

It has no dependency on Compose, Android lifecycle, filesystem APIs, ViewModels or timers. The same `advanceDay()` path is used by:

- the manual validation `+1 DAY` action;
- the application `SimulationClock`;
- JVM parity/regression tests;
- future deterministic soak/debug tooling.

The engine also exposes a pure `recalculate()` path for current derived colony metrics without advancing time.

## Day ordering

The native order follows the source semantics:

1. capture **beginning-of-day Fuel**;
2. calculate current workforce, Industry and Power networks;
3. collect resource production from active extraction sites;
4. store production at its real resource identity/quality;
5. add current synthetic Food output when technology permits;
6. consume Food;
7. consume Fuel for the generation capacity that was available at the beginning of the day;
8. consume Ore for staffed Industry;
9. calculate Food/Power survival state;
10. apply starvation progression/mortality;
11. apply colony death state/reputation impact if population reaches zero;
12. progress the real survey subsystem;
13. advance the canonical 360-day calendar;
14. return the exact network/consumption metrics that actually governed that completed day.

This preserves the approved source behavior that **Fuel extracted during a day cannot power that same day's generation**. It becomes available for the next day.

## Inventory operations

Phase 3 extends the permanent generic `Inventory` rather than creating Food/Fuel/Ore-specific stores.

New immutable operations:

- `store(ResourceId, category, amount, quality)`;
- `consumeCategory(category, requested)`;
- quality-band/value metadata through `ResourceQuality`;
- cheapest-value inventory lots are consumed first, matching current web semantics.

Resource identity remains independent of the four current broad categories, preserving the future resource-overhaul direction without defining new resources now.

## Permanent physical development state

Phase 3 introduces `TileDevelopment` on the existing `WorldTile` model with kinds:

- Extract;
- Housing;
- Power;
- Industry;
- Headquarters.

This is the permanent representation of physical colony development. Phase 3 **simulates** that representation. Phase 4 must add player construction/upgrade/demolition by extending the same owner rather than introducing another building/site model.

This avoids implementing a temporary abstract Power/Industry counter that would need to be replaced immediately in Phase 4.

## Infrastructure and technology rules

`InfrastructureRules` owns the physical capacity/Power curves required by simulation, including the current source values for:

- Power generation;
- Housing capacity/fixed Power;
- Industry capacity/idle/variable Power;
- current extractor-family Power demand;
- basic Spaceport Power demand;
- founding ship's current 50 Industry support.

`TechnologyCapabilities` owns the current deployed effects needed by simulation:

- generator Fuel intensity;
- Food production multiplier;
- synthetic Food output;
- Industry workforce efficiency;
- Industry Ore efficiency;
- Mining workforce efficiency.

Phase 4 must reuse these owners for player-facing building behavior.

## Workforce network

Current source behavior is preserved:

- normally available workforce is floor(population × 50%);
- Food/Fuel extraction is treated as survival work and receives workforce first;
- Build/Ore extraction is commercial work and receives remaining workforce;
- site workforce requirements scale with site level, extraction complexity and current technology;
- the current renewable/food/fiber/biomass harvest-intensity modifier is represented;
- once a colony has entered zero-Food starvation state, available workforce becomes zero immediately on subsequent days;
- emergency mode disables commercial work.

## Industry network

Current source behavior is preserved:

- docked founding ship contributes 50 Industry but no Power;
- Industry buildings add installed capacity from the current curves;
- population/staffing scales usable Industry;
- Food/Fuel extraction load receives Industry capacity first;
- commercial extraction uses remaining Industry;
- Ore demand derives from staffed installed Industry using the current Industry Ore-efficiency technology factor;
- emergency mode disables commercial Industry throughput.

A staged `foundingShipDocked` durable fact exists until the full ship domain is migrated in Phase 8. It represents the canonical ship's current physical state, not a second fleet implementation. Phase 8 must migrate this fact into the full ship model.

## Power network

Current source behavior is preserved:

- the founding ship provides **0 Power**;
- a new settled colony therefore has no colony generation until a Power building exists;
- Power building generation is Fuel-limited using beginning-of-day Fuel;
- generator Fuel burn uses the current Power technology intensity;
- life support/housing receives priority;
- survival extraction/supporting Industry receives priority next;
- Spaceport receives its current medium-priority allocation;
- commercial sites/Industry receive the remaining Power;
- partial Power scales throughput by delivered fraction.

The Phase 3 result metrics intentionally distinguish:

- current/end-of-day stock/runway;
- the beginning-of-day network that actually powered the completed day.

This prevents same-day produced Fuel from being falsely reported as having powered that day.

## Collection and depletion

Current site output/rate behavior is represented using the permanent resource deposit and development models:

- level output curve;
- finite deposit scale factor;
- terrain yield factor;
- Food technology output factor;
- workforce/Industry/Power throughput factors;
- finite reserve reduction and exhaustion;
- renewable abundance factor;
- harvest intensity;
- renewable degradation/recovery;
- renewable wipeout/development removal.

Phase 4 adds the player actions that create/upgrade/control extraction sites; Phase 3 owns the simulation of sites that exist.

## Food, starvation and mortality

Current source survival rules are locked:

- Food demand is population × `0.12` per day;
- if Food consumption is zero, starvation days increase;
- the first **30 complete zero-Food days** have no Food-driven deaths;
- Food shortage still removes workforce after starvation state begins;
- failed life-support Power can cause mortality immediately even during the Food grace period;
- survival uses the lower of Food supply and life-support Power supply;
- mortality begins when stable supply is below 70%;
- the source critical/collapse mortality curve and minimum daily death are preserved;
- population is fractional-safe because the source mortality calculation is fractional;
- population below `0.5` collapses to zero;
- population reaching zero marks the colony dead, ends its contract and applies the current reputation penalty;
- there is no automatic/natural population growth in current source behavior.

## Operating cost

The pinned current source `ResourceService.operatingCost()` returns zero. Phase 3 therefore does **not** invent a daily operating-cost economy merely to fill the migration checklist.

When/if the canonical web design changes this behavior, the native simulation should migrate that approved rule explicitly.

## SimulationClock

`app/SimulationClock.kt` owns cadence only.

It supports:

- Pause;
- 1×;
- 2×;
- 4×.

The validation build starts **paused** so selecting a landing site cannot silently advance a survival-critical colony while the player inspects the screen.

Clock tests use virtual coroutine time to prove cadence/pause behavior independently of gameplay state.

## Save format v3

Phase 3 advances native saves from v2 to **v3**.

Durable additions include:

- fractional-safe population representation;
- emergency mode;
- starvation-day history;
- staged founding-ship-docked state;
- tile developments;
- renewable health/original-rank/intensity/wipeout state.

`NativeSaveV2ToV3` is registered in the ordered migration chain. Added fields use semantic defaults; existing integer population JSON remains valid for the new numeric representation.

A representative Phase 1 native-v1 save is tested through the complete v1→v2→v3 chain so migration remains cumulative rather than only testing the immediately previous schema.

## Phase 3 validation UI

The UI remains deliberately lightweight; the full MineIT design-system/map visual migration is still Phase 5.

After landing-site selection the temporary screen now exposes real simulation state:

- Food/Build/Fuel/Ore stocks;
- population/cash/status;
- Power Fuel-limited generation vs demand;
- life-support percentage;
- Food production vs demand;
- generator Fuel burn;
- current Industry vs installed Industry;
- workforce available/required;
- starvation-day count;
- survival-shortage warning;
- Pause/1×/2×/4×;
- manual `+1 DAY` while paused;
- the existing real 8×8 survey map and queue.

`ADVANCE SURVEY DAY` no longer exists as a partial second day engine.

### Important expected validation behavior

At the current Contract 01 landing baseline there is **no Power Plant yet**, and the founding ship intentionally supplies no Power. Therefore immediately after settlement the source-compatible native metrics show:

- Power capacity `0`;
- life-support Power `0%`;
- total initial Power demand about `30.9`;
- founding ship Industry `50`;
- Food demand `14.4/day`.

If the player manually advances a day before Phase 4 provides construction controls, mortality is expected because life support is unpowered. This is not a Phase 3 bug; it is the consequence of faithfully migrating the current Power rules before the Phase 4 build actions. The clock therefore starts paused.

Phase 4 is the next phase that makes the colony meaningfully playable by exposing Power/Housing/Industry/extraction construction and upgrades.

## Tests

Phase 3 regression coverage includes:

- starter settled-colony Food/Power/Industry/Ore demand values;
- immediate life-support mortality with no Power Plant;
- L1 Power Plant capacity/Fuel burn/survival behavior;
- same-day Food production before Food consumption;
- beginning-of-day Fuel cannot be supplemented by same-day Fuel extraction;
- 30 complete zero-Food days before Food-driven mortality;
- starvation workforce shutdown;
- 25-year supplied-colony deterministic soak with no natural growth and finite metrics;
- native v1→v2→v3 save compatibility;
- SimulationClock virtual-time cadence and pause behavior;
- all prior Phase 0–2 parity/save/session regressions remain active.

## Explicit non-goals retained

Phase 3 does **not** implement:

- the planned resource overhaul;
- new resources/refining/manufacturing;
- player-facing building/site construction, upgrade or demolition;
- the full Headquarters command/outage/recovery system (Phase 4);
- final building/resource art integration;
- final Compose design system (Phase 5);
- trade/contracts/buyers;
- multi-colony simulation orchestration;
- full fleet/ship domain;
- final web-save conversion;
- invented operating costs absent from the source baseline.

## Phase 3 exit gate

- [x] canonical pure-Kotlin daily simulation owner exists;
- [x] current Food/Fuel/Ore consumption rules are represented;
- [x] current collection/depletion/renewable behavior is represented;
- [x] current workforce/Industry/Power networks are represented;
- [x] beginning-of-day Fuel semantics are locked by regression coverage;
- [x] current starvation/mortality/colony-death behavior is represented;
- [x] no automatic population growth is introduced;
- [x] SimulationClock is separate from gameplay and starts paused in validation UI;
- [x] native save v3 and cumulative migration coverage exist;
- [x] 25-year supplied-colony soak fixture exists;
- [x] Phase 2 survey-only day path is replaced rather than kept in parallel;
- [ ] final Phase 3 code/documentation head passes Android CI, signer verification and artifact upload;
- [ ] `0.4.0-migration` receives hands-on device validation.

Phase 3 remains on its feature branch until the final CI build and hands-on validation are accepted.

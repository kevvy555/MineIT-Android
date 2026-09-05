# Phase 7.4 — Founding / Player Ship Control Checkpoint

**Status:** Implementation and regression coverage complete; physical-device validation pending  
**Date:** 5 September 2026  
**Branch:** `feature/migration-phase-7`  
**Source reference:** `kevvy555/MineIT` `develop` at `075b3d82fd88334b20b3cfe7d6e2731c8d840533` / game `5.13.22`  
**Implementation checkpoint:** `0299e84678de17a2ee7c10d6ff2d458b377f87c5`  
**Validation build:** `0.7.6-migration` / version code `18`  
**Native save format:** `7`

## Source parity audit

The maintained web `ship-control.html`, `player-ship-ui.js`, `ship-navigation-ui.js`, `ship-preparation-ui.js` and `expansion-service.js` were reviewed before native changes.

For the current single-colony phase, the web Ship Control experience breaks down into two groups:

### Required now

- player/founding ship identity and docked location;
- Cargo, Fuel, Food and Crew summary;
- Colony Support Module / command-link state;
- cargo and dedicated Food/Fuel storage;
- accommodation and colony residents aboard;
- ship Food runway for residents aboard;
- founding-ship self-powered Industry contribution;
- ship↔colony resource transfer with exact quality preservation;
- colony-resident transfer ship↔planet;
- Spaceport service gates;
- founding bootstrap unload exception;
- first-departure Headquarters handover readiness and dependency warnings.

### Deliberately deferred

- full route planning and target selection;
- Star Map travel workflow;
- general interstellar launch/travel state;
- broad fleet management;
- factory-new ship procurement;
- later travel Fuel/Veyrite redesign.

Those remain later migration phases. No dead buttons were added merely to copy web destinations that are not yet native.

## Native domain gaps closed

`PlayerFleetService` now also owns the source rules required by the docked Ship Control surface:

- moving planetary residents back aboard a docked player ship;
- powered-Spaceport gating for boarding;
- accommodation-capacity enforcement;
- homeless planetary residents board first before occupied Housing is reduced;
- Build and Ore share the general cargo hold;
- Food uses the dedicated Food store;
- Fuel uses the dedicated Fuel tank;
- colony→ship loading is clipped to the correct remaining store/hold capacity;
- full-store attempts are rejected with explicit reasons;
- ship/colony quality-band stock remains conserved exactly.

The existing N05 rules remain unchanged:

- founding bootstrap ship→colony unload is allowed before command handover even without normal transfer services;
- normal later transfers require powered Spaceport services;
- residents aboard stay outside planetary workforce/Power and consume ship Food;
- the founding ship contributes 50 self-powered Industry while docked.

## Ship Control surface

Selecting the center Spaceport sector while a player ship is docked now opens native Ship Control.

The surface includes:

- founding/player ship hero and docked colony location;
- web-order top metrics: Cargo, Fuel, Food and Crew;
- Colony Support Module state;
- `OPEN COLONY CONTROL` when this vessel is the current command source;
- total physical load and separate general/Food/Fuel capacities;
- crew minimum/maximum and current travel passenger manifest;
- ship Industry and command capability/status;
- ship residents, planetary residents, planetary Housing and ship Food runway;
- Move 10 / Move Max ashore;
- Move 10 / Move Max aboard;
- projected-Power shortage confirmation before moving residents ashore;
- per-resource ship/colony stock, category and quality-band visibility;
- Load 10 / Load Max and Unload 10 / Unload Max actions;
- founding bootstrap-unload explanation when normal Spaceport transfer services are unavailable;
- command-handover readiness;
- resident dependency warning;
- ship-Industry dependency warning;
- minimum-crew warning where applicable.

The PLAYER_SHIP attention target now opens this Ship Control route rather than using the establishment dialog as a temporary destination.

## Architecture

Gameplay truth remains outside Compose:

- `PlayerFleetService` owns ship inventory, accommodation, resident assignment and capacity rules;
- `ColonyNetworkService` owns command/workforce/Industry/Power effects;
- `SpaceportService` owns service availability;
- `HeadquartersService` owns departure/handover readiness;
- `GameViewModel` only commits fleet actions through `GameSession`, refreshes derived state and reports persistence failures;
- `PlayerShipControlPresentation` is a pure presentation mapping;
- `PlayerShipControlSheet` owns only transient dialog state and dispatches actions.

## Implementation commits

- `39c0913f7e9f80f7fbc671be9042371d9925226b` — close docked ship transfer-domain gaps;
- `240aba7b0ce59cc465212958623d515c25b424bd` — regression coverage for resident boarding and separate ship capacities;
- `416a75fd13f3ae77c19f0462bfeececccfe9dd7b` — persisted ViewModel/session ship actions;
- `68aa340cbb9496fe4e0d3292196a2c6ad40fd3bf` — native Ship Control presentation and controls;
- `6da7652601ce245f47ccece0491c793351aa8127` — route docked center-sector selection to Ship Control;
- `708eaba6b81522d0c7a0151af1d946a6e70967da` — application callback wiring and PLAYER_SHIP warning navigation;
- `0299e84678de17a2ee7c10d6ff2d458b377f87c5` — Ship Control presentation regressions;
- `bc271424eb65ccc80e5b2e0ac1a797714ec20c31` — cut `0.7.6-migration` / code 18 validation build.

## CI evidence

- CI run `33950687242` / run `319` passed the new fleet-domain rules and regression tests, debug APK assembly, persistent development signer verification and artifact upload.
- CI run `33951061510` / run `324` passed the complete Ship Control path and Ship Control presentation regressions, plus debug APK assembly, signer verification and artifact upload.
- A fresh full pipeline is required for the final `0.7.6-migration` build/docs head before that artifact is treated as the physical-device checkpoint.

## Regression coverage

`ShipEstablishmentParityTest` now additionally pins:

- ship boarding is Spaceport gated;
- boarding is bounded by ship accommodation;
- homeless residents board before planetary Housing occupancy is reduced;
- Build and Ore share the general cargo limit;
- Food and Fuel use independent capacities;
- loading clips at capacity and rejects a full hold/store;
- the total physical load is the sum of the three separate storage families.

`PlayerShipControlPresentationTest` pins:

- the founding Ship Control metric hierarchy;
- CSM linked state while ship command is active;
- CSM offline state when Headquarters is the active command source;
- 120 founding residents remain visibly aboard at initial establishment;
- bootstrap unload availability before normal Spaceport transfers;
- no loading while normal transfer services are unavailable;
- separate store headroom in manifest rows;
- command-handover, resident-dependency and Industry-dependency readiness warnings.

## Physical-device validation checklist

Before Phase 7 acceptance, validate on the target phone:

1. With the founding ship docked, tap the center Spaceport sector and confirm Ship Control opens.
2. Confirm the top row shows Cargo, Fuel, Food and Crew clearly on the target phone.
3. Confirm founding ship identity and current colony location are obvious.
4. During initial N05 establishment, verify 120 residents are shown aboard and colony residents ashore are zero.
5. Confirm ship Food runway is derived from residents aboard and ship Food, not colony Food.
6. Before normal Spaceport services are powered, verify founding Build/Fuel/Food/Ore can be unloaded using the bootstrap exception but colony→ship loading is blocked.
7. After Power/Housing/Spaceport are operational, move 10 residents ashore and verify ship/planet/Housing counts remain consistent.
8. Use Move Max ashore and confirm it stops at available real Housing.
9. Create a projected Power shortage and confirm the warning appears before transfer; cancel once and confirm once.
10. Move residents back aboard and verify ship accommodation limits the transfer.
11. Where homeless planetary residents exist, verify boarding them does not incorrectly reduce occupied Housing first.
12. Load Build/Ore and confirm they share one general cargo limit.
13. Fill the general cargo hold and confirm Food and Fuel can still load into their dedicated stores.
14. Fill Food/Fuel stores and confirm further loading is blocked with a useful message.
15. Verify quality-band stock totals remain conserved across repeated load/unload operations.
16. Confirm the CSM shows LINKED while the docked founding ship is the command source and opens Colony Control.
17. Once an operational Primary HQ becomes command source, confirm the CSM changes to offline/unlinked rather than claiming it still manages the colony.
18. Confirm the departure section reflects Headquarters handover BLOCKED/READY/COMPLETE correctly.
19. Confirm the 50 Industry dependency warning is present while the founding ship still supplies colony Industry.
20. Trigger a PLAYER_SHIP attention item and confirm it navigates to Ship Control, not the establishment dialog.
21. Press Android Back/dismiss and confirm the map selection is cleared cleanly.
22. Save/reload after resident and cargo transfers and confirm ship manifest, residents aboard, accommodation and capacities persist correctly.

## Remaining Phase 7.4 work

No additional single-colony gameplay implementation is currently known for this slice. Physical-device findings may still require layout or interaction refinement. General travel, route planning, launch and multi-ship fleet operation remain later work by design.

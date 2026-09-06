# Phase 7 — Manual Stabilization Checkpoint

**Status:** Implementation/regression complete; second physical-device validation pending  
**Date:** 6 September 2026  
**Branch:** `feature/migration-phase-7`  
**Previous device build:** `0.7.7-migration` / version code `19`  
**Stabilization validation build:** `0.7.8-migration` / version code `20`  
**Native save format:** `7` (unchanged)  
**Code CI before build bump:** Android CI run `34038242584` / run `343` — success

## Why this checkpoint exists

The first Phase 7.1–7.5 physical-device playthrough reached a real continuous-game blocker rather than merely visual polish. The run progressed through normal colony development until the Corporate Ship/Fuel/Power recovery path became unusable.

Manual findings from `0.7.7-migration`:

1. a docked Corporate Ship could be visible while every Buy action was unavailable once the Spaceport had lost Power;
2. this could create a Fuel → Power → Spaceport → Fuel deadlock and lead to otherwise avoidable colony death;
3. a fully staffed Primary Headquarters could show command handover as READY but there was no native action capable of completing it because source completion normally happens inside the later, still-deferred launch workflow;
4. the colony map visibly changed size whenever the variable-height selected-sector context bar opened/changed;
5. full Conglomerate Buyers-service discoverability is still intentionally Phase 7.6 and was not pulled into this stabilization pass.

The `0.7.7` device checkpoint is therefore **not accepted**. These blockers were addressed before adding more migration surface area.

## Stabilization changes

### 1. Corporate Ship emergency Fuel recovery

Normal maintained-web Spaceport gating remains intact for routine operation:

- selling still requires a powered Spaceport;
- Food, Build and Ore imports still require a powered Spaceport;
- colonist transfer still requires powered transfer services;
- normal ship loading/transfer behavior is unchanged.

A focused recovery exception is now provided while a Corporate Ship is already docked:

- Fuel purchases remain available even if the Spaceport is currently offline;
- the purchase still uses the normal corporate price;
- the purchase still consumes company cash;
- the purchase still consumes the visit's import cargo capacity;
- the UI explicitly labels the state `SPACEPORT OFFLINE • EMERGENCY FUEL ONLY`;
- Fuel rows identify the purchase as an emergency transfer;
- non-Fuel rows remain disabled until Spaceport Power is restored.

This deliberate semantic difference from the current web source is recorded in `INTENTIONAL_DIVERGENCES.md`.

Regression coverage:

- `CorporateTradeServiceTest.buyKeepsNormalSpaceportGateButAllowsEmergencyFuelRecovery`;
- `CorporateTradePresentationTest.offline Spaceport leaves only Fuel buying available for recovery`.

### 2. Stable colony-map viewport

`SectorContextBar` now occupies one constant-height slot beneath the map regardless of whether it is:

- empty;
- showing one sector;
- showing several survey sectors;
- showing a developed sector with actions/requirements.

Richer context content scrolls inside that slot when necessary. It no longer changes the amount of vertical space assigned to the `ColonyMap`, so tapping/clearing/changing sectors must not make the map jump in size.

The obsolete `hold then drag` instruction was also corrected to the current normal-drag surveying interaction.

### 3. Explicit founding command handover

The authoritative `HeadquartersService.completeCommandHandover` state transition already existed, but the source normally triggers it from the first successful ship launch. Full native launch/travel remains deliberately deferred.

During Phase 7 stabilization:

- a selected Primary Headquarters whose canonical departure gate is satisfied shows `COMPLETE COMMAND HANDOVER`;
- the same gate remains: Primary HQ fully constructed and fully staffed; Headquarters Power is not a handover requirement;
- activating the command persists the existing `commandHandoverComplete` field;
- the HQ changes from HANDOVER READY to COMPLETE;
- the action does not launch or move the founding ship;
- route planning/interstellar travel remain later work.

This temporary/pre-travel semantic difference is recorded in `INTENTIONAL_DIVERGENCES.md`.

Regression coverage:

- existing Phase 4 Headquarters gate/handover regression;
- `Phase7StabilizationTest.primary Headquarters action completes founding handover before travel migration`.

## CI evidence

Android CI run `34038242584` / run `343` passed the complete stabilization code/regression head before the validation build bump:

- Kotlin/JUnit regression suite;
- debug APK assembly;
- persistent development signer verification;
- APK artifact upload.

A fresh exact-head CI run for the `0.7.8-migration` build/docs checkpoint must pass before its artifact is supplied for device testing.

## Second physical-device validation

Install `0.7.8-migration` and concentrate first on the previously blocked continuous path.

### Map

1. Start/continue a colony and note the map's physical dimensions with no sector selected.
2. Tap an empty surveyed sector, a resource sector and a developed sector.
3. Open/close different sector contexts repeatedly.
4. Confirm the map remains exactly the same size throughout.
5. Confirm any unusually rich sector context can scroll within its own fixed slot rather than resizing the map.

### Headquarters handover

6. Build/identify the Primary Headquarters.
7. Before its staffing gate is satisfied, confirm handover reports BLOCKED.
8. Once fully constructed/staffed, open the Primary HQ and confirm `COMPLETE COMMAND HANDOVER` appears.
9. Activate it and confirm the handover becomes COMPLETE without launching/moving the founding ship.
10. Close/reopen HQ and save/reload; confirm COMPLETE persists.
11. Confirm normal Headquarters/network information remains usable after handover.

### Corporate Ship and Fuel recovery

12. During a powered visit, confirm ordinary Buy/Sell/Colonists behavior still works.
13. Reproduce or approach a Fuel shortage until the Spaceport becomes unpowered while the Corporate Ship is docked.
14. Confirm the Corporate Ship clearly reports `SPACEPORT OFFLINE • EMERGENCY FUEL ONLY`.
15. Open Buy → Fuel and confirm Fuel rows remain purchasable.
16. Confirm Food, Build and Ore remain disabled while Spaceport services are offline.
17. Buy enough Fuel to restore generation and confirm company cash/import cargo/Fuel stock update correctly.
18. Advance/recalculate until Spaceport Power returns and confirm normal Corporate Ship services become available again.
19. Confirm the colony can recover instead of being forced into the previous Power/Fuel death spiral.
20. Save/reload around the recovery path if practical.

## Not yet expected in this build

Do not treat these as failures of the stabilization checkpoint:

- full Conglomerate Buyers Service/profile/directory parity — Phase 7.6;
- full Technology/Engineering progression — Phase 7.7;
- dedicated resource-detail views — Phase 7.8;
- full Spaceport service panel — Phase 7.9;
- general ship launch, Star Map navigation and interstellar travel — later fleet/travel phase.

If this second manual pass clears the three blockers, continue into Phase 7.6–7.9 as the next migration round.

# Phase 7 — Manual Stabilization Checkpoint

**Status:** Follow-up implementation/regression complete; physical-device revalidation pending  
**Date:** 6 September 2026  
**Branch:** `feature/migration-phase-7`  
**Previous device build:** `0.7.8-migration` / version code `20`  
**Follow-up validation build:** `0.7.9-migration` / version code `21`  
**Native save format:** `7` (unchanged)

## Why this checkpoint exists

The first Phase 7.1–7.5 physical-device playthrough exposed integration blockers that unit-level parity work had not revealed. A first stabilization build fixed the map viewport and exposed pre-travel command handover, but the device review then clarified the intended Corporate Ship purchase rule and exposed an HQ-build crash.

The `0.7.7` and `0.7.8` device checkpoints are therefore **not accepted**. This `0.7.9` checkpoint supersedes the temporary Fuel-only recovery rule and is the next manual validation target.

## Stabilization changes

### 1. Corporate Ship purchases use a working computer link

Corporate purchases are no longer a powered-Spaceport operation.

While a Corporate Ship is docked, the colony can buy **Food, Build, Fuel or Ore** when either:

- any player ship is docked at the active colony; or
- a Headquarters is constructed, fully staffed and powered.

The visiting Corporate Ship unloads purchased supplies itself. Therefore Spaceport Power is not required for buying.

All normal purchase economics remain authoritative:

- normal Corporate Ship buy price;
- company cash;
- visit import-cargo capacity;
- resource stock goes into the colony inventory.

Separate Spaceport-owned operations remain gated by powered Spaceport services:

- selling/loading colony stock;
- colonist/passenger transfer;
- engineering and later market/departure services.

The Corporate Ship UI now distinguishes:

- `CORPORATE PURCHASE LINK OFFLINE` when neither computer source exists;
- `SPACEPORT OFFLINE • PURCHASE LINK ONLINE` when the computer link works but Spaceport services do not;
- `CORPORATE UNLOAD` on purchases made while the Spaceport is offline.

This deliberate semantic correction is recorded in `INTENTIONAL_DIVERGENCES.md`. It supersedes the short-lived `emergency Fuel only` rule from the first stabilization pass.

Regression coverage:

- `CorporatePurchaseAccessTest` — docked player ship, powered/staffed HQ, and no-link states;
- `CorporateTradeServiceTest.buyUsesComputerLinkRatherThanSpaceportPower`;
- `CorporateTradePresentationTest.buy buttons follow corporate computer-link availability for every import category`.

### 2. Headquarters build crash

The physical-device report was consistent with a state/derived-state race:

1. building an HQ commits the new authoritative `GameState`;
2. the session state flow can recompose the selected tile immediately;
3. the derived `ColonyNetworkSnapshot` is recalculated just afterward;
4. for one composition frame, the selected tile can therefore be an HQ while the previous network snapshot has no matching HQ row;
5. the HQ presentation previously required that row with `requireNotNull`, causing the app to terminate;
6. after reload the network is already rebuilt, which explains why the same HQ then appeared correctly.

`HeadquartersControlReadiness` now guards the HQ sheet until the matching derived network row exists. Gameplay state is not delayed or duplicated; only presentation waits for the derived snapshot it requires.

Regression coverage:

- `HeadquartersControlReadinessTest.new Headquarters waits for matching derived network row before sheet renders`.

### 3. Stable colony-map viewport

The first stabilization pass remains in force:

- `SectorContextBar` occupies a constant-height slot;
- richer selected-sector content scrolls inside that slot;
- selecting/clearing/changing sectors must not resize the map;
- current surveying instruction reflects normal drag selection rather than obsolete hold-then-drag wording.

### 4. Explicit founding command handover

The first stabilization pass also remains in force:

- a fully constructed and staffed Primary Headquarters exposes `COMPLETE COMMAND HANDOVER`;
- Headquarters Power is not part of the canonical handover gate;
- completion persists `commandHandoverComplete` without launching/moving the founding ship;
- full route planning/interstellar travel remains deferred.

## Manual validation for 0.7.9

### Headquarters crash and handover

1. Build the first Headquarters while its sector remains selected.
2. Confirm the app does **not** crash immediately after construction.
3. Open/close the HQ several times and confirm its detail/network panel renders normally.
4. Save/reload and confirm the HQ remains usable.
5. If practical, build/upgrade another HQ and confirm no equivalent crash.
6. Before Primary-HQ staffing is satisfied, confirm command handover is blocked.
7. Once the Primary HQ is fully staffed, activate `COMPLETE COMMAND HANDOVER`.
8. Confirm handover becomes COMPLETE and remains complete after save/reload.

### Corporate Ship purchases

9. With the founding/player ship still docked, allow the Corporate Ship to arrive.
10. Confirm Buy works for Fuel, Food, Ore and Build even if the Spaceport is unpowered.
11. Confirm purchases deduct cash and visit import capacity and add the selected resource to colony stock.
12. Confirm the offline-Spaceport UI explains that the Corporate Ship is unloading the purchase itself.
13. Confirm selling and colonist transfer remain unavailable while Spaceport services are offline.
14. Restore Spaceport Power and confirm those normal Spaceport operations return.
15. If practical, test a state with no docked player ship but a powered/staffed HQ and confirm all Corporate Ship purchase categories remain available.
16. If practical, test with neither a docked player ship nor powered/staffed HQ and confirm Buy reports the purchase link offline.

### Map

17. Note the map dimensions with no sector selected.
18. Tap empty, resource and developed sectors repeatedly.
19. Confirm the map remains exactly the same size while the bottom context changes.
20. Confirm rich context content scrolls within its fixed area instead of resizing the map.

### Continuous survival path

21. Re-run the low-Fuel path that previously killed the colony.
22. Confirm a docked player ship or operational HQ lets you buy Fuel from the Corporate Ship without Spaceport Power.
23. Buy sufficient Fuel, advance/recalculate, and confirm the colony can recover Power.
24. Continue beyond the point where the `0.7.7` run became blocked.

## Not yet expected in this build

Do not treat these as failures of this checkpoint:

- full Conglomerate Buyers Service/profile/directory parity — Phase 7.6;
- full Technology/Engineering progression — Phase 7.7;
- dedicated resource-detail views — Phase 7.8;
- full Spaceport service panel — Phase 7.9;
- general ship launch, Star Map navigation and interstellar travel — later fleet/travel phase.

If this manual pass clears the blockers, continue into Phase 7.6–7.9 as the next migration round.

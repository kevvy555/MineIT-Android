# Phase 7.5 — Corporate Ship / Trade Parity Checkpoint

**Status:** Implementation and regression coverage complete; physical-device validation pending  
**Date:** 5 September 2026  
**Branch:** `feature/migration-phase-7`  
**Source reference:** `kevvy555/MineIT` `develop` at `075b3d82fd88334b20b3cfe7d6e2731c8d840533` / game `5.13.22` / web save `16`  
**Implementation checkpoint:** `ab4c71cbab433ad20dba3d177ebe3416d4084408`  
**Implementation CI:** Android CI run `33952381683` / run `331` — success  
**Validation build:** `0.7.7-migration` / version code `19`  
**Native save format:** `7`

## Purpose

Complete the single-colony Corporate Ship interaction before manual Phase 7 review. The goal is to make the arriving corporate vessel feel and behave like the maintained web quick-trade experience rather than a generic Android commercial dashboard, while continuing to use the already-migrated Phase 6 trade domain as the sole gameplay owner.

## Source parity audit

The maintained web implementation was inspected across:

- `views/quick-trade-shell.html`;
- `views/quick-trade-sell.html`;
- `views/quick-trade-buy.html`;
- `views/quick-trade-colonists.html`;
- `views/quick-trade-amount.html`;
- `css/trade-quick.css`;
- `js/ui/corporate-trade-ui.js`;
- `js/ui/quick-trade-ui.js`;
- existing trade-domain behavior and tests.

The source hierarchy is:

1. Corporate Ship / docked-visit context;
2. compact Cash / Import / Export / PAX summary;
3. fixed Sell / Buy / Colonists modes;
4. compact four-row trading work area with paging;
5. fixed `SHIP DEPARTS` action.

## Native parity changes

`TradeCommercialPanelScreen` now follows that hierarchy as a full-screen Corporate Ship surface instead of a long generic dashboard.

### Shell and visit context

- `CORPORATE TRADE SHIP` is the primary identity;
- Koplin Deep Reach logistics/docked-colony context is visible in the hero;
- Cash, remaining import cargo, remaining export capacity and remaining passenger capacity remain visible at the top;
- the summary is four-across where the phone width allows and intentionally falls back to 2x2 below 370dp;
- Sell / Buy / Colonists remain directly accessible under the summary;
- the working area owns the available remaining vertical space rather than allowing the whole screen to become a long scroll;
- `SHIP DEPARTS` remains fixed at the bottom and closes the trade surface after dispatching the canonical departure action.

### Sell

- amount defaults to `10,000`, matching the source;
- amount controls use ±1,000 plus 100 / 1K / 10K / MAX presets;
- source minimum of one unit and 100,000 maximum are retained;
- resources completely protected by the per-resource colony reserve are no longer shown as sellable rows;
- stock, reserve and sellable quantity are shown together;
- quote quantity and expected revenue are shown on the action;
- source quality/value ordering remains owned by `CorporateTradeService`;
- category bulk actions remain Food / Build / Fuel / Ore;
- Sell All remains available;
- four-row paging mirrors the source working set;
- native reserve increment controls remain as a small deliberate touch refinement while preserving the canonical reserve semantics.

### Buy

- Buy opens on Fuel;
- categories are source-order Fuel / Food / Ore / Build, with no non-source `ALL` category;
- four-row paging mirrors the source;
- resources below the colony reserve are prioritised first, largest reserve shortfall first;
- rows expose colony stock, reserve/shortfall and unit price;
- Buy quantity/cost remains clipped canonically by requested amount, corporate import cargo and company cash;
- a direct `TO RESERVE` action is retained as a compact Android interpretation of the source reserve-shortfall convenience.

### Colonists

- Colonists is unavailable once the local contract has ended or the colony is a liability/dead;
- the first value for each Corporate Ship visit defaults to current `MAX SAFE`, matching the source;
- ±1 / ±10 and MAX SAFE controls remain available;
- current hard maximum and Food-safe maximum remain distinct;
- Food remains guidance rather than a hard gate;
- Housing/Power support, passenger capacity, cash and powered Spaceport transfer services remain hard limits in the domain.

### Departure

The maintained web closes the Corporate Ship modal when `SHIP DEPARTS` is selected. Native now does the same after dispatching `CorporateTradeService.depart`, rather than leaving the player on an inactive trade screen.

## Architecture

No trade gameplay rules were moved into Compose.

- `CorporateTradeService` remains the owner for visits, capacity, reserve, quality-aware selling, bulk selling, import pricing/capacity, colonist limits/costs and departure;
- `SpaceportService` remains the owner of trade/transfer availability;
- `GameViewModel` remains the persisted action boundary;
- `CorporateTradePresentation` contains only pure source-compatible row ordering/visibility/default-selection policy;
- `TradeCommercialPanelScreen` owns only transient tab, amount and page selection.

Native save format remains `7`; Phase 7.5 introduces no save migration.

## Implementation commits

- `79601433f4d992487d1ece99f5b1e92bffff33ab` — add Corporate Ship presentation policy;
- `10fe326700f11a0e07dcd4d1cc3d498b69ae4db5` — mirror the Corporate Ship quick-trade surface;
- `9f9c07d79735409468eb3e13bb64add65ae1110b` — presentation parity regressions;
- `aa906500feb0c651acfb67cc485c3015166c921d` — typed colony-status eligibility;
- `ab4c71cbab433ad20dba3d177ebe3416d4084408` — compile correction, source amount floor and source departure-close behavior;
- `9d1d7328f9864dd57664d940033b53ecc7c3c7e2` — cut `0.7.7-migration` / code 19 validation build.

## CI evidence

Android CI run `33952381683` / run `331` passed the complete Phase 7.5 implementation and presentation regression suite, debug APK assembly, persistent development signer verification and artifact upload.

A fresh full CI pipeline is required for the final build/docs head before the `0.7.7-migration` artifact is treated as the physical-device checkpoint.

## Physical-device validation checklist

Use `0.7.7-migration` on the target phone and validate:

1. Advance/reach a Corporate Ship arrival and confirm the blocking arrival event pauses play.
2. Tap `OPEN TRADE` and confirm the event clears into the full-screen Corporate Ship surface.
3. Confirm Corporate Ship identity, Koplin Deep Reach context and current colony are obvious.
4. Confirm Cash / Import / Export / PAX are readable without scrolling.
5. Confirm Sell / Buy / Colonists tabs remain easy to reach and the working area feels materially closer to the web quick-trade screen than the previous stacked dashboard.
6. Confirm `SHIP DEPARTS` remains visible at the bottom while changing tabs/pages.
7. SELL opens with amount 10K; verify 100 / 1K / 10K / MAX and ±1K controls.
8. Set a reserve and confirm stock fully covered by that reserve disappears from the sell list while stock above reserve reports the correct sellable amount.
9. Sell an individual resource and confirm quantity, cash, export capacity and inventory update correctly.
10. Confirm quality-aware sale value remains correct for mixed-quality stock where practical.
11. Test ALL FOOD / BUILD / FUEL / ORE and SELL ALL; confirm reserve is protected per resource and export room is respected globally.
12. BUY opens on FUEL and only exposes FUEL / FOOD / ORE / BUILD categories.
13. With a non-zero reserve, confirm resources below reserve rise to the top and show their shortfall.
14. Test normal Buy and `TO RESERVE`; confirm company cash and import cargo limit the result.
15. Open COLONISTS and confirm the initial selected quantity for the visit is MAX SAFE, not 1.
16. Use ±1 / ±10 and MAX SAFE; confirm the player may go above Food-safe quantity but not above the hard Housing/Power/PAX limit.
17. Confirm an unsafe Food choice is visibly identifiable and the domain result warns after transfer.
18. Confirm an ended/liability colony does not offer Colonists.
19. Remove/deny powered Spaceport services where practical and confirm Sell/Buy/Colonist actions are unavailable/rejected without corrupting state.
20. Close Trade without departure, reopen it and confirm the same visit/capacity usage remains active.
21. Save/reload during an active visit and confirm visit state, pinned capacities, cargo/export/passenger usage and reserve persist.
22. Tap `SHIP DEPARTS`; confirm the screen closes immediately, the visit ends and the next-arrival countdown is restored.
23. Confirm simulation/event state resumes correctly after departure according to the existing event flow.
24. Check portrait and landscape for clipped buttons, overlapping summary metrics, unusable row heights or excessive scrolling.
25. Confirm Android Back/Close does not accidentally depart the ship and leaves the active visit available to reopen.

## Manual-review gate

Stop after this checkpoint. Do not proceed automatically into Phase 7.6 Buyers parity until the `0.7.7-migration` physical-device review has been completed and any Phase 7.1–7.5 findings that Kev wants addressed have been recorded/fixed.

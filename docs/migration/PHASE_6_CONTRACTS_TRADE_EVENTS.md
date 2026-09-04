# Phase 6 — Contracts, Trade and Commercial Events

**Status:** In progress — commercial foundation implemented; player-facing trade/contract flows still to migrate  
**Started:** 4 September 2026  
**Android branch:** `feature/migration-phase-6`  
**Starting Android head:** `b9364a4a8d7e9566df6e16d5f61e93f83bc6cd8d` (accepted Phase 5)  
**Web behavioural baseline:** `kevvy555/MineIT` `develop` at `9e58983adaa7a15cd525451266ce9df3c17ae886` / game `5.13.15` / web save `16`

## Goal

Restore the current commercial game loops and consequences natively without moving gameplay truth into Compose or redesigning the economy.

The web implementation remains the behavioural reference. Phase 6 covers the corporate trade ship, buy/sell/reserve/passenger rules, contract lifecycle and decisions, global corporate-event queue and Game Log. Buyer/demand layers are added only after the underlying commercial owners are stable.

## Source owners reviewed

- `js/core/config.js`;
- `js/domain/game-state.js`;
- `js/domain/trade-service.js`;
- `js/domain/contract-service.js`;
- `js/domain/corporate-event-service.js`;
- `js/domain/game-log-service.js`;
- `js/domain/reputation-service.js`;
- `js/data/contracts.js`;
- `tests/trade-reserve.test.js`;
- `tests/quality-pricing.test.js`.

Both root `AGENTS.md` files were read before implementation.

## Commercial foundation now represented

The first Phase 6 slice adds permanent domain owners rather than UI-local approximations:

- `TradeState` is durable per colony;
- one colony-wide trade reserve is durable per colony;
- `CorporateTradeService` owns arrival/departure, pinned visit cargo/export capacities, reserve-protected exports, quality-banded prices, processing bonus, corporate buy markup and imported stock;
- company cash is now represented as a `Double`, matching JavaScript Number semantics so fractional quality prices are not rounded away;
- `ContractState` now carries durable local revenue/local costs and decision fields needed by the current web lifecycle;
- `CorporateEventService` owns global priority, sequence and deduplication of corporate events;
- `GameLogService` owns durable monotonic event logging independently of presentation;
- native save format v5 registers a v4→v5 migration. New fields use semantic defaults so accepted Phase 5 saves remain loadable.

The current source resource catalogue and quality bands remain unchanged. This work does not implement the post-migration resource overhaul.

## Locked trade rules in this slice

- first corporate trade visit: absolute day 181 for Contract 01;
- recurrence: every 180 days;
- import cargo: `min(12,000, 4,000 + reputation × 250)` and pinned for a visit;
- export cargo: `min(500,000, 100,000 + reputation × 10,000)` and pinned for a visit;
- passenger capacity: 250 per visit;
- buy price: current quality-neutral sell price × resource value scale 5 × corporate markup 1.5;
- sell price: resource sell price × value scale 5 × quality multiplier × processing multiplier;
- processing bonus capped at 50%;
- one colony reserve protects that quantity independently on every stock entry;
- general selling consumes the highest-value quality lots first;
- imported stock enters the current excellent quality band, matching the web inventory default;
- trade operations require the Basic Spaceport service gate supplied by the existing Phase 4 network owner.

## Event ordering locked

Corporate event priority follows the current source:

1. emergency Food;
2. recovered ship/buyer event;
3. buyer;
4. contract decision;
5. corporate ship;
6. other/unknown.

Equivalent event keys deduplicate rather than spawning parallel decisions.

## Regression coverage added

The first slice locks:

- first-arrival date and next visit scheduling;
- pinned cargo/export visit capacities;
- colony-wide reserve protection;
- exact quality-banded Gold prices from source parity fixtures;
- highest-value quality export ordering;
- cash/revenue updates without integer rounding;
- corporate buy markup, cash and cargo consumption;
- Spaceport service gating;
- corporate-event priority/deduplication;
- monotonic Game Log event IDs and canonical game dates.

## Remaining Phase 6 work

- integrate scheduled ship arrival/event recovery into the application simulation loop;
- migrate colonist transfer including Housing/Power/passenger hard limits and MAX SAFE Food convenience;
- migrate full ContractService scoring/deadline/holdover/failure/liability decisions;
- migrate corporate export reputation at the source-compatible fractional reputation scale;
- extend Game Log with current telemetry/export shape where useful on Android;
- migrate buyer/demand collection loops after the core owners are stable;
- add production Compose trade/contract/event/log surfaces using the Phase 5 design system;
- add save v5 non-default round-trip and v4 compatibility fixtures;
- run full parity/soak/CI and produce the Phase 6 hands-on APK.

## Exit gate

Phase 6 is accepted only when equivalent source/native scenarios produce equivalent cash, stock, contract and event outcomes and the commercial flows pass physical-device validation.

# Phase 6 — Contracts, Trade and Commercial Events

**Status:** Implementation complete — `0.7.0-migration` hands-on validation pending  
**Started:** 4 September 2026  
**Android branch:** `feature/migration-phase-6`  
**Starting Android head:** `b9364a4a8d7e9566df6e16d5f61e93f83bc6cd8d` (accepted Phase 5)  
**Implementation checkpoint head:** `9380c130c63ca540e2e778810ff0cac570cd48af`  
**Implementation checkpoint CI:** Android CI run `33897074590` / run number `254` — success  
**Validation build:** `0.7.0-migration` / version code `12`  
**Native save format:** `5`  
**Web behavioural baseline:** `kevvy555/MineIT` `develop` at `9e58983adaa7a15cd525451266ce9df3c17ae886` / game `5.13.15` / web save `16`

## Goal

Restore the current Contract 01 commercial game loops and consequences natively without moving gameplay truth into Compose or redesigning the economy.

Phase 6 covers the Corporate Ship trade loop, import/export capacity and reserves, colonist transfer, contract scoring/deadline decisions, commercial/corporate events, buyers and recurring collection ships, reputation consequences and Game Log presentation. The web implementation remains the behavioural reference.

The post-migration resource overhaul remains explicitly out of scope.

## Source owners reviewed

The Phase 6 implementation was based on the pinned web source and its tests, including the commercial owners under `js/domain/`, Contract 01 data/configuration and trade/quality/reserve regression coverage. The source areas reviewed during the phase include:

- `js/core/config.js`;
- `js/domain/game-state.js`;
- `js/domain/trade-service.js`;
- `js/domain/contract-service.js`;
- `js/domain/corporate-event-service.js`;
- `js/domain/game-log-service.js`;
- `js/domain/reputation-service.js`;
- `js/domain/buyer-service.js` and related buyer/spaceport behaviour;
- `js/domain/spaceport-model.js`;
- `js/data/contracts.js`;
- source tests covering trade reserve, quality pricing and commercial behaviour.

Both repository root `AGENTS.md` files were read before implementation and the Android contract was re-read during closeout.

## Native ownership

Phase 6 keeps commercial truth out of Compose and splits the web application's cross-feature orchestration into explicit native owners:

- `CorporateTradeService` owns Corporate Ship arrival/departure, buy/sell, import/export capacity, reserves, pricing and colonist transfer rules;
- `ContractService` owns Contract 01 goal scoring, deadline states, extensions, holdover/failure/renewal and liabilities;
- `BuyerService` owns deterministic offers, accepted buyer contracts, recurring collection ships, shipment resolution, relationship happiness/misses/termination and buyer reputation consequences;
- `CorporateEventService` owns durable global commercial-event ordering and deduplication;
- `GameLogService` owns durable monotonic game events independently of presentation;
- `CommercialDayService` coordinates the commercial work that occurs after the canonical `DailySimulationEngine` day transition without becoming a second simulation engine;
- `GameSession` remains the root state/persistence owner;
- Compose renders commercial state and dispatches intent only.

No parallel trade, buyer, contract or event implementation was introduced in UI state.

## Corporate Ship trade parity

The migrated Corporate Ship loop preserves the current source rules represented in the native services and regression suite:

- Contract 01 first Corporate Ship visit occurs at absolute day 181;
- normal recurrence is every 180 days;
- visit import capacity is pinned from the arrival state using the current reputation curve;
- visit export capacity is pinned from the arrival state using the current reputation curve;
- passenger capacity is 250 per visit;
- buy prices use the current source resource value scale and Corporate Ship markup;
- sell prices retain quality-band and processing multipliers;
- processing bonus remains capped at 50%;
- the colony-wide reserve protects the configured amount independently on each qualifying stock entry;
- general export consumes the highest-value quality lots first;
- imported stock uses the current excellent/default quality band;
- trade actions use the existing Basic Spaceport operational/service gate;
- Corporate Ship export reputation is awarded once for the relevant visit rather than repeatedly per transaction.

Cash remains `Double`, matching JavaScript Number semantics so fractional quality pricing is not silently rounded.

## Colonist transfer

Corporate Ship passenger transfer is migrated through the trade owner rather than UI calculations.

The current hard limits are preserved around passenger capacity, Housing and Power/service availability. Food is not treated as an additional hard transfer gate where the source presents it as a convenience/safety calculation instead. The native surface exposes the applicable safe/max transfer information while domain rules remain authoritative.

This does not replace the later full player-fleet/passenger system planned for Phase 8.

## Contract lifecycle

`ContractService` now carries the current Contract 01 commercial lifecycle needed for native completion/failure play:

- goal evaluation uses the same completed-day Food/Industry metrics that governed simulation;
- deadline state is generated deterministically from durable contract/date state;
- decision state survives save/load and process recreation;
- extension/holdover/failure/renewal paths are represented;
- applicable costs/liabilities/reputation/cash consequences are applied in the contract owner;
- pending contract decisions enter the shared corporate-event queue rather than being owned by a screen.

Commercial day orchestration queues deadline events after the canonical day transition.

## Buyers and recurring collection ships

Phase 6 migrates the current buyer loop onto permanent native buyer state:

- deterministic buyer offers are produced from canonical state rather than UI randomness;
- accepted offers become durable buyer contracts;
- recurring collection cycles use the source-compatible due-day attempts at +0, +5, +10 and +15 days;
- collection ships can dock or remain in orbital holding depending on berth availability;
- qualifying stock and minimum quality are calculated by `BuyerService`;
- a shipment requires the current minimum accepted fulfilment before transfer;
- shipment revenue, relationship happiness, missed shipments, fulfilled shipments and lifetime revenue are durable;
- buyer happiness loss feeds the shared reputation owner;
- consecutive Red cycles / repeated misses can terminate the relationship;
- buyer shipments do **not** consume the Corporate Ship's export capacity;
- waiting, final-attempt miss resolution and cancellation are explicit domain actions.

Buyer event recovery reconstructs blocking presentation events from durable buyer/contract state after load or process recreation.

## Corporate events and Game Log

Commercial blocking events use one durable queue with the current source priority ordering:

1. emergency Food;
2. recovered ship/buyer event;
3. buyer;
4. contract decision;
5. Corporate Ship;
6. other/unknown.

Equivalent event keys deduplicate rather than spawning parallel decisions. `CommercialDayService` returns whether the resulting state should pause so the application clock/UI can respond without embedding event truth in presentation.

The production native commercial panel exposes Trade, Contract, Buyers and Game Log views using the Phase 5 design system. Game Log entries retain monotonic IDs and canonical game dates.

## Daily orchestration

`CommercialDayService` is the cross-domain day coordinator for Phase 6. Its ordering is intentionally narrow:

1. run the canonical `DailySimulationEngine.advanceDay()`;
2. stop commercial processing if the colony died;
3. process scheduled Corporate Ship arrival;
4. process buyer collection-ship state/events;
5. evaluate the contract deadline against the completed-day metrics;
6. enqueue resulting corporate events;
7. return the same completed-day simulation metrics plus pause state.

This preserves the Phase 3 rule that rendering does not drive simulation and avoids recreating the web `app.js` as a monolithic native owner.

## Persistence — native save v5

Phase 6 raises the native save format to **v5** for durable commercial state. The ordered migration chain now includes v4→v5 while preserving all earlier migration steps.

Coverage verifies:

- accepted Phase 5/v4 saves migrate with semantic Phase 6 defaults;
- a non-default v5 state containing commercial/buyer/event/log/contract data round-trips without loss;
- the cumulative older-native migration chain still reaches the current schema;
- commercial blocking state can be recovered from durable data rather than relying on transient dialogs.

JavaScript compatibility remains isolated at the web-save importer boundary; no legacy compatibility fields were added to canonical production models.

## Regression coverage

Phase 6 regression coverage now includes the commercial foundation and the completed player-facing flows, including:

- first Corporate Ship arrival and next-visit scheduling;
- pinned import/export visit capacities;
- reserve protection and quality/value export ordering;
- quality-banded pricing and fractional cash/revenue updates;
- corporate buy markup/cargo/cash behaviour;
- Spaceport service gating;
- one reputation award per Corporate Ship visit;
- colonist transfer constraints and Food-not-a-hard-gate behaviour;
- deterministic buyer offers;
- buyer offer acceptance and recurring collection attempts;
- buyer shipment transfer/revenue/reputation/relationship state;
- buyer shipments remaining independent of Corporate Ship export capacity;
- buyer waiting/miss/termination behaviour;
- contract extension/failure/renewal/liability lifecycle;
- scheduled commercial day orchestration;
- event recovery after load/process recreation;
- corporate-event priority/deduplication;
- monotonic Game Log event IDs and canonical dates;
- native v4→v5 compatibility and non-default v5 round-trip;
- all earlier Phase 0–5 regression suites remaining active.

## CI and validation checkpoint

Implementation checkpoint `9380c130c63ca540e2e778810ff0cac570cd48af` passed Android CI run `33897074590` (run 254):

- JVM unit/regression tests succeeded;
- debug APK assembly succeeded;
- signer verification succeeded with SHA-256 `fac61745dc0903786fb9ede62a962b399f7348f0bb6f899b8332667591033b9c`;
- debug artifact upload succeeded;
- artifact ID: `9946197390`;
- uploaded artifact ZIP digest: `sha256:d660804a5792f98684a7380ed4c5bb22acb084ac00fb027c0eae29d36cdf729f`.

The documentation closeout changes made after that checkpoint must themselves receive exact-head CI before the APK is handed off for physical-device validation.

## Hands-on validation target

Install `0.7.0-migration` over the accepted `0.6.0-migration` build rather than uninstalling it. This validates both normal application update signing and native v4→v5 save migration.

The physical-device pass should cover at least:

- existing Phase 5 save loads successfully;
- normal map/survey/building play remains intact;
- commercial panel opens and Trade/Contract/Buyers/Game Log are usable;
- Corporate Ship arrival/event can be observed or deterministically advanced to;
- buy/sell/reserve values update stock/cash correctly;
- colonist transfer feedback/limits are coherent;
- contract decision state is visible when due;
- buyer offers/contracts and collection events can be inspected/resolved;
- blocking commercial events pause/resume the simulation coherently;
- closing/reopening the app preserves commercial state and reconstructs unresolved events.

## Exit gate

- [x] Corporate Ship trade rules migrated into a canonical native domain owner.
- [x] Buy/sell/capacity/reserve/quality pricing regression coverage passes.
- [x] Colonist transfer migrated without inventing a Food hard gate.
- [x] Contract goal/deadline/decision/lifecycle rules migrated.
- [x] Corporate event ordering/deduplication/recovery migrated.
- [x] Buyer offers/contracts/recurring collections/relationship outcomes migrated.
- [x] Game Log durable state and native presentation migrated.
- [x] Daily commercial orchestration uses completed-day simulation metrics.
- [x] Native save v5 migration and non-default round-trip coverage added.
- [x] `0.7.0-migration` code compiles, tests and builds with the persistent signer.
- [ ] final documentation head passes full Android CI.
- [ ] `0.7.0-migration` receives hands-on physical-device validation.

After hands-on acceptance, update this record and the master migration guide to accepted, fast-forward the exact accepted Phase 6 head to `main`, and only then begin Phase 7 from that baseline.

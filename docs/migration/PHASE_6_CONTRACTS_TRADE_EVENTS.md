# Phase 6 — Contracts, Trade and Commercial Events

**Status:** Implementation/regression complete — UI/layout parity refinement required before acceptance  
**Started:** 4 September 2026  
**Android branch:** `feature/migration-phase-6`  
**Starting Android head:** `b9364a4a8d7e9566df6e16d5f61e93f83bc6cd8d` (accepted Phase 5)  
**Implementation checkpoint head:** `9380c130c63ca540e2e778810ff0cac570cd48af`  
**Implementation checkpoint CI:** Android CI run `33897074590` / run number `254` — success  
**Initial validation build:** `0.7.0-migration` / version code `12`  
**Native save format:** `5`  
**Web behavioural + UI-layout baseline:** `kevvy555/MineIT` `develop` at `9e58983adaa7a15cd525451266ce9df3c17ae886` / game `5.13.15` / web save `16`

## Goal

Restore the current Contract 01 commercial game loops and consequences natively without moving gameplay truth into Compose or redesigning the economy.

Phase 6 covers the Corporate Ship trade loop, import/export capacity and reserves, colonist transfer, contract scoring/deadline decisions, commercial/corporate events, buyers and recurring collection ships, reputation consequences and Game Log presentation.

Following physical-device review of `0.7.0-migration`, the web implementation is now explicitly the **layout/information-hierarchy reference as well as the behavioural reference**. Phase 6 is therefore not accepted until its player-facing commercial screens and the current Colony Details presentation are refined toward recognisable web layout parity while retaining deliberate native improvements.

The post-migration resource overhaul remains explicitly out of scope.

## Revised migration method from this phase onward

The Phase 1–6 backend-first work established the permanent state, persistence, simulation and domain foundations. From this point onward player-facing migration is performed as **UI-led vertical feature slices**:

1. inspect the existing web HTML/view, CSS, UI/controller and domain owner/tests together;
2. identify what should be preserved, deliberately refined or replaced;
3. confirm which native domain/application capability already exists;
4. migrate any missing backend functionality in canonical native owners;
5. recreate the screen in Compose with the same recognisable hierarchy and workflow;
6. apply agreed native refinements rather than redesigning by default;
7. validate the complete player-facing slice before moving on.

Cross-cutting backend prerequisites may still be implemented first when several screens depend on them, but they should be driven by the next complete player-facing slice rather than accumulated for a distant UI phase.

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

The UI-parity refinement additionally treats the corresponding `views/`, `css/` and `js/ui/` files as first-class migration references rather than merely styling inspiration.

Both repository root `AGENTS.md` files were read before implementation and the Android contract was re-read during closeout and the migration-plan revision.

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

No parallel trade, buyer, contract or event implementation was introduced in UI state. The UI refinement must continue to consume these same owners rather than creating layout-specific gameplay logic.

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

This does not replace the later full player-fleet/passenger system planned for the Ships/Expansion vertical slices.

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

The native commercial UI exposes Trade, Contract, Buyers and Game Log views. During the refinement pass these screens must be compared directly with the corresponding web views/CSS/controllers so their hierarchy and workflows remain recognisably MineIT rather than becoming generic Android dashboards.

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

Phase 6 regression coverage now includes the commercial foundation and the completed backend/player-flow rules, including:

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

The UI refinement should add/strengthen interaction or screenshot coverage where needed, but gameplay assertions remain in domain tests.

## CI and validation checkpoints

Implementation checkpoint `9380c130c63ca540e2e778810ff0cac570cd48af` passed Android CI run `33897074590` (run 254):

- JVM unit/regression tests succeeded;
- debug APK assembly succeeded;
- signer verification succeeded with SHA-256 `fac61745dc0903786fb9ede62a962b399f7348f0bb6f899b8332667591033b9c`;
- debug artifact upload succeeded;
- artifact ID: `9946197390`;
- uploaded artifact ZIP digest: `sha256:d660804a5792f98684a7380ed4c5bb22acb084ac00fb027c0eae29d36cdf729f`.

The first completed documentation head `d977012bbe09b434df7088f4159693510e7af9ad` also passed exact-head Android CI run `33898301473` / run number `256`, and produced the `0.7.0-migration` validation APK.

That APK proved the commercial/backend integration was usable, but physical-device review identified excessive native layout drift. It is therefore a **feedback build, not the Phase 6 acceptance build**.

## Hands-on feedback and revised completion target

Physical-device review of `0.7.0-migration` confirmed that the migrated functionality is moving in the right direction, but some screens were substantially more redesigned than intended. The Colony Details sheet in particular demonstrated a generic vertically stacked dashboard layout rather than preserving the compact web MineIT hierarchy.

The agreed correction is:

- keep the current canonical Phase 6 backend/domain implementation;
- inspect the current web `develop` layout for Colony Details, Corporate Ship/Trade, Contract, Buyers and Game Log using their HTML/views, CSS and UI controllers;
- retain the web screen hierarchy, compactness, primary control placement and recognisable workflows by default;
- apply deliberate refinements where the current web UI is weak or inconsistent;
- use native Android improvements for safe areas, accessibility, touch sizing, Back/navigation, sheets/dialogs and responsive sizing;
- do not introduce a materially different layout merely because it is convenient in Compose.

This refinement is the first application of the new UI-led vertical-slice migration method documented in `DESIGN_SYSTEM_DIRECTION.md` and the master migration guide.

## Revised hands-on validation target

The next Phase 6 validation build should be installed over the current `0.7.0-migration` build and should cover at least:

- existing save loads successfully;
- normal map/survey/building play remains intact;
- Colony Details is recognisably based on the web layout/information hierarchy while retaining useful native refinements;
- Corporate Ship/Trade is recognisably based on the existing web flow;
- Contract presentation preserves the existing hierarchy and decisions;
- Buyers presentation preserves the existing offer/contract/collection workflow;
- Game Log remains compact and coherent with the rest of MineIT;
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
- [x] Game Log durable state and native presentation foundation migrated.
- [x] Daily commercial orchestration uses completed-day simulation metrics.
- [x] Native save v5 migration and non-default round-trip coverage added.
- [x] `0.7.0-migration` code compiles, tests and builds with the persistent signer.
- [x] initial `0.7.0-migration` exact documentation head passed full Android CI.
- [x] initial physical-device review completed and UI-layout drift identified.
- [ ] Colony Details reviewed/recreated from the web layout with deliberate refinements only.
- [ ] Corporate Ship/Trade, Contract, Buyers and Game Log reviewed/recreated as complete UI-led vertical slices.
- [ ] revised Phase 6 UI interaction/regression coverage passes.
- [ ] revised Phase 6 exact head passes full Android CI and signer verification.
- [ ] revised Phase 6 validation build receives hands-on physical-device acceptance.

Only after that acceptance should Phase 6 be marked complete/accepted and promoted to `main`. Phase 7 then begins from the exact accepted baseline using the same UI-led vertical-slice method.

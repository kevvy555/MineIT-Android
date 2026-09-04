# MineIT Android Migration — Phase 2 Core Data, Resources and Deterministic World

**Phase:** 2 — Core data, current resources and deterministic world  
**Status:** Complete — accepted on device  
**Started:** 4 September 2026  
**Accepted:** 4 September 2026  
**Source behavioural baseline:** `kevvy555/MineIT` commit `9e58983adaa7a15cd525451266ce9df3c17ae886`, game `5.13.15`, web save `16`  
**Android branch:** `feature/migration-phase-2`  
**Accepted Android head:** `95995680a0014532de29540dcab665f077b15072`  
**Android version:** `0.3.1-migration` / version code `7`

## Source reviewed

Phase 2 was grounded against the pinned source implementation rather than inferred from the POC. Relevant owners reviewed include:

- `js/core/config.js`;
- `js/core/utils.js`;
- `js/data/resources.js`;
- `js/data/contracts.js`;
- `js/data/technologies.js`;
- `js/domain/resource-service.js`;
- `js/domain/contract-service.js`;
- `js/domain/game-state.js`;
- `js/domain/technology-service.js`;
- `js/domain/land-service.js`;
- `js/domain/world-service.js`;
- `js/domain/survey-service.js`;
- `tests/land-view.test.js`;
- Phase 1 native save/state tests;
- the Phase 0 representative web-v16 fixture.

## Important parity correction discovered

The source web game uses a **360-day year** (`CONFIG.DAYS_PER_YEAR = 360`).

Phase 1 had mistakenly introduced a 365-day native `GameDate`. Phase 2 corrected the permanent native calendar to 360 days and added regression coverage:

- Year 4 Day 360 = absolute day 1440;
- absolute day 361 = Year 2 Day 1;
- `GameDate.nextDay()` rolls Year 4 Day 360 to Year 5 Day 1.

This is a migration defect correction, not an intentional gameplay divergence.

## Current Contract 01 foundation

The native `Contract01` definition now uses the pinned current web values, including:

- `Koplin Mining Charter — Contract 01`;
- temperate / breathable archetype;
- tier/colony tier 1;
- direct technology access;
- required Power/Food/Mining/Scanning technology level 1;
- current resource weights Food `.35`, Build `.25`, Fuel `.18`, Ore `.22`;
- 10-year duration;
- goals Food 120, Industry 520, Population 1050;
- Silver/Gold/Platinum bands 450,000 / 1,000,000 / 2,200,000.

`NewGameFactory` creates a deterministic real Contract 01 start state with the current starter values:

- cash 32,000;
- population 120;
- Food 1,300;
- Build 520;
- Fuel 420;
- Ore 260;
- all current local/company technology levels start at 1;
- eight deterministic landing-site candidates are generated before settlement.

The starter inventory uses the existing current resources and excellent quality band exactly as the web source does:

- `fungal` / Fungal Shelf;
- `fiber` / Construction Fibre;
- `biomass` / Biomass;
- `surface-iron` / Surface Iron Nodules.

## Current resource catalogue

All **40 current web resource definitions** are represented by typed native `ResourceDefinition` records using stable `ResourceId` identity.

Fields ported include current category, display name, rarity, selection weight, production multiplier, quality bias, renewable/finite status, Mining level, Scanning level, unlock description, sell price and the current manufactured marker where applicable.

No new resource catalogue, recipe, refined material, manufacturing product, ship Fuel, balance or distribution design has been introduced. The planned resource overhaul remains separate.

## Central extraction compatibility

`ExtractionCompatibility` is the single native owner for the current resource-to-extractor-family mapping.

It preserves current web behaviour while avoiding the web architecture's duplicated compatibility maps. Native world, extraction and later UI rules query this owner rather than create parallel maps.

Current families represented are Farm, Ranch, Bio, Algae, Quarry, Rig, Mine and Deep Mine.

## JavaScript-compatible deterministic random primitives

Native Phase 2 ports the source FNV-1a-style hash and Mulberry32-style seeded random implementation using explicit unsigned 32-bit arithmetic.

Parity fixtures lock representative outputs, including:

- `hashString("hello") = 1335831723`;
- first seeded random values `0.6311965801287442`, `0.7983490515034646`, `0.30862852558493614`.

This gives later simulation/world code a reproducible source-compatible random foundation.

## Landing-site generation

`LandGenerator` ports the current deterministic `LandService` rules:

- 8×8 grid, coordinates `-4..3`;
- eight site-selection candidates;
- archetype-specific terrain profiles;
- deterministic mountain/lake centres;
- current terrain jitter and two smoothing passes;
- cardinal-neighbour smoothing semantics;
- `(0,0)` forced plain for the founding ship;
- deterministic terrain-art variant numbers 1–4;
- current terrain yield factors.

Landing settlement itself remains owned by `NewGameFactory`; the generator only generates terrain. This prevents a second state-transition path from developing.

A pinned golden scenario for seed `123456789`, Contract uid `intro-123456789`, temperate candidate 0 asserts:

- candidate seed `730361737`;
- 64 cells;
- Plain 38, Hill 6, Mountain 14, Lake 6;
- first cell terrain/variant and ship-tile terrain match the web algorithm.

## Resource discovery

`WorldDiscovery` ports current deterministic surface/deep discovery semantics:

- terrain-specific surface-resource chance;
- current contract/terrain family weighting;
- current terrain-specific resource ID pools;
- deep-resource pool;
- weighted resource selection;
- current quality distribution algorithm;
- renewable abundance bands;
- finite deposit scale and reserve calculation;
- current terrain yield multiplier;
- Scanning-level gating;
- deterministic later resurvey discovery of resources hidden from lower-level scans.

Golden parity cases include:

- `(-2,-4)` at scanning level 1 → Nutrient Crop, quality 49, Established renewable abundance;
- `(-3,-4)` at level 1 → clear reading;
- the same `(-3,-4)` resurveyed at level 5 → Natural Gas, quality 449, Large deposit, reserve 126126.

## Scanning capability

`ScanningTechnology` owns the current source scanning slot/time-factor/hint-tier table rather than letting UI code hard-code those values.

Representative locked values:

- level 1 → 1 slot, 1.000 scan-time factor;
- level 3 → 2 slots, 0.950 factor;
- level 10 → 5 slots, 0.775 factor.

## Surveying

`SurveyService` ports the current queue/timing behaviour using immutable `WorldState` transitions:

- one to five active slots;
- queue fill;
- ship tile exclusion;
- distance/contract/survey/command timing factors;
- resurvey eligibility;
- current 50% resurvey time;
- Headquarters continuity progress factor;
- completion through `WorldDiscovery`;
- deterministic survey state suitable for JVM tests.

`SurveyGameService` is the aggregate-level owner used by the application/UI boundary. It applies current scanning capability to the active colony and returns a new `GameState`; the UI never mutates world/game state directly.

The parity fixture locks `(-2,-4)` to 9 initial survey days and 5 resurvey days under baseline factors.

## Native state and save v2

The permanent `GameState` foundation grows only for domains actually migrated in Phase 2.

`CompanyState` gains typed technology levels. `ColonyState` gains current Contract state, colony status, local technology levels and persistent `WorldState` containing landing candidates, chosen site, tiles, survey activity and queue.

Phase 2 advances the native save format from `1` to `2` and registers `NativeSaveV1ToV2`. The added state fields have semantic defaults, allowing representative Phase 1 state to decode through the ordered migration path while retaining cash, reputation, population and inventory amounts.

The validation UI uses the real `GameSession` and production `FileGameStatePersistence`, so landing-site selection and survey progress are persisted rather than living in an isolated demo StateFlow.

## Web-v16 importer growth

The isolated `WebSaveV16Importer` remains the only JavaScript-save compatibility boundary.

Its preview additionally validates/exposes the current source world's contract archetype, tile count, revealed tile count, active survey count and queued survey count.

It still does not pretend that the entire web world/ship/contract portfolio is natively importable before those semantic owners exist.

## Phase 2 native validation screen

The old 6×6 `domain/poc` state/simulation was removed completely. There is no second demo gameplay engine behind the Android screen.

`0.3.1-migration` exposes the real Phase 2 state through Compose:

1. starts from the real Contract 01 `SITE_SELECTION` state;
2. shows all eight deterministic landing-site candidates with actual terrain counts/seeds;
3. selecting a site calls canonical `NewGameFactory.settleLandingSite()` through `GameSession`;
4. shows the real 8×8 `-4..3` terrain grid;
5. geological sectors begin unsurveyed, with the founding ship tile excluded;
6. tapping a sector shows its real terrain/status;
7. Survey uses `SurveyGameService` and displays the real duration for current scanning capability;
8. active/queued scan state appears directly on the map;
9. completion reveals deterministic resource/clear results including quality and abundance/deposit information;
10. state changes use the Phase 1 atomic save/backup path.

### Temporary validation-day control

Phase 2 contained `ADVANCE SURVEY DAY` solely to make surveying testable before Phase 3 existed. It advanced the canonical date and survey subsystem but intentionally did not run Food, Fuel, production, Power or mortality.

Phase 3 removes/replaces that partial behavior with the canonical complete day simulation; it must not remain as a second day engine.

## Tests and CI

Phase 2 coverage includes the source-canonical calendar, deterministic hash/random output, Contract 01 starter values, resource catalogue/compatibility, scanning capability, landing terrain, surface/deep discovery, resurvey, survey timing/queue/completion, aggregate `SurveyGameService`, web-v16 preview growth, native v1→v2 save compatibility and persistence/session regressions.

The `0.3.1-migration` implementation head `55b626f7908256eced7cea86ec1f19dd28e0be6b` passed Android CI run `33855588637`, including unit tests, APK assembly, persistent signer verification and artifact upload. The final documented/accepted branch head `95995680a0014532de29540dcab665f077b15072` also passed CI before acceptance.

## Device acceptance

The user installed `0.3.1-migration` over the previous build and confirmed:

- the landing-site selection view was present;
- the real 8×8 terrain grid was visible;
- the screen was basic but going in the right direction;
- there was no observed issue blocking progression to Phase 3.

This hands-on check satisfied the final Phase 2 acceptance gate. The accepted head was fast-forwarded into `main` before Phase 3 was branched.

## Explicit non-goals retained

Phase 2 did **not** implement the planned resource overhaul, new resource definitions, refining/manufacturing recipes, new extractor families/buildings, ship Propellant/Fusion Fuel, generator Fuel redesign, economic/progression rebalance, daily production/consumption/survival simulation, final native UI/design system or full web-save conversion.

## Phase 2 exit gate

- [x] typed current configuration subset exists;
- [x] current resource definitions use stable IDs and generic inventory architecture;
- [x] current quality-band architecture is preserved;
- [x] current Contract 01 starter data exists;
- [x] seeded landing/world generation is source-compatible and deterministic;
- [x] resource discovery and resurvey rules are ported;
- [x] resource/extractor compatibility has one native owner;
- [x] current scanning capability data has one typed native owner;
- [x] parity golden tests exist for randomness, terrain, discovery and surveying;
- [x] native save v1→v2 compatibility is covered;
- [x] old POC gameplay state/simulation has been removed;
- [x] real landing-site/world/survey state is exposed in the Android validation UI;
- [x] final branch head passed Android CI, signer verification and artifact upload;
- [x] `0.3.1-migration` passed hands-on device validation and was accepted.

**Phase 2 is complete. Phase 3 starts from the exact accepted Phase 2 head.**

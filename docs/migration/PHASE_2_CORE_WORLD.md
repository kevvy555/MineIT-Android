# MineIT Android Migration — Phase 2 Core Data, Resources and Deterministic World

**Phase:** 2 — Core data, current resources and deterministic world  
**Status:** Implementation complete — `0.3.1-migration` hands-on validation pending  
**Started:** 4 September 2026  
**Source behavioural baseline:** `kevvy555/MineIT` commit `9e58983adaa7a15cd525451266ce9df3c17ae886`, game `5.13.15`, web save `16`  
**Android branch:** `feature/migration-phase-2`  
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

Fields ported include:

- current category;
- display name;
- rarity;
- selection weight;
- production multiplier;
- quality bias;
- renewable/finite status;
- Mining level;
- Scanning level;
- unlock description;
- sell price;
- current manufactured marker where applicable.

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

`ScanningTechnology` now owns the current source scanning slot/time-factor/hint-tier table rather than letting UI code hard-code those values.

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

## Native state changes

The permanent `GameState` foundation grows only for domains actually migrated in Phase 2.

`CompanyState` gains typed technology levels.

`ColonyState` gains:

- current Contract state;
- colony status (`site-selection`, `playing`, `holdover`, `liability`, `dead`);
- local technology levels;
- persistent `WorldState` containing landing candidates, chosen site, tiles, survey activity and queue.

New fields use safe defaults so Phase 1 save data remains decodable through the explicit migration path.

## Native save v2

Phase 2 advances the native save format from `1` to `2` and registers a real `NativeSaveV1ToV2` migration.

Because Phase 2's added state fields have semantic defaults, the migration advances the envelope version and the serializer supplies those defaults when decoding a Phase 1 state.

Regression coverage proves a representative `0.2.0-migration` / native-v1 save becomes native v2 while retaining cash, reputation, population and inventory amounts.

The Phase 2 validation UI now uses the real `GameSession` and production `FileGameStatePersistence`, so landing-site selection and survey progress are persisted rather than living in an isolated demo StateFlow.

## Web-v16 importer growth

The isolated `WebSaveV16Importer` remains the only JavaScript-save compatibility boundary.

Its preview additionally validates/exposes the current source world's:

- contract archetype;
- tile count;
- revealed tile count;
- active survey count;
- queued survey count.

It still does not pretend that the entire web world/ship/contract portfolio is natively importable before those semantic owners exist.

## Phase 2 native validation screen

The old 6×6 `domain/poc` state/simulation has been removed completely. There is no longer a second demo gameplay engine behind the Android screen.

`0.3.1-migration` exposes the real Phase 2 state through Compose:

1. starts from the real Contract 01 `SITE_SELECTION` state;
2. shows all eight deterministic landing-site candidates with actual terrain counts/seeds;
3. selecting a site calls canonical `NewGameFactory.settleLandingSite()` through `GameSession`;
4. shows the real 8×8 `-4..3` terrain grid;
5. all geological sectors begin unsurveyed (the founding ship tile is excluded);
6. tapping a sector shows its real terrain/status;
7. the Survey action uses `SurveyGameService` and displays the real duration for current scanning capability;
8. active/queued scan state appears directly on the map;
9. completing a scan reveals the real deterministic resource/clear result, including resource name, category, quality, abundance/deposit scale and finite reserve where applicable;
10. state changes are written through the Phase 1 atomic save/backup path.

### Temporary validation-day control

The screen contains **ADVANCE SURVEY DAY** solely to make Phase 2 testable before the full Phase 3 simulation exists.

It advances:

- the canonical 360-day `GameDate` by one day;
- the real survey subsystem by one day.

It intentionally does **not** run Food, Fuel, production, Power, mortality or other daily simulation rules. The screen explicitly labels this limitation. Phase 3 replaces this partial validation action with the complete daily simulation pipeline rather than keeping two day engines.

This is presentation/application scaffolding around canonical domain behaviour, not a second survey implementation.

## Tests added/strengthened

Phase 2 coverage includes:

- source-canonical 360-day calendar and next-day rollover;
- JS-compatible hash/random output;
- Contract 01 starter values;
- all-current-resource catalogue size and selected definitions;
- central extraction compatibility;
- source scanning capability slots/time factors;
- deterministic landing candidate golden fixture;
- deterministic surface/deep discovery golden fixtures;
- lower-level scan then higher-level resurvey;
- survey timing/queue/completion rules;
- aggregate `SurveyGameService` path used by the native UI;
- web-v16 world preview fields;
- native v1→v2 save compatibility;
- existing save/session tests updated to assert `NativeSaveFormat.CURRENT_VERSION` rather than hard-code an obsolete schema number;
- existing persistence recovery regression coverage retained unchanged.

The `0.3.1-migration` implementation head `55b626f7908256eced7cea86ec1f19dd28e0be6b` passed Android CI run `33855588637`, including unit tests, APK assembly, persistent signer verification and artifact upload.

## Explicit non-goals retained

Phase 2 does **not** implement:

- the planned resource overhaul;
- new resource definitions;
- refining/manufacturing recipes;
- new extractor families/buildings;
- ship Propellant/Fusion Fuel;
- generator Fuel redesign;
- economic/progression rebalance;
- daily production/consumption/survival simulation;
- final native UI/design system;
- full web-save conversion.

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
- [x] implementation head passed Android CI, signer verification and artifact upload;
- [ ] hands-on validation of `0.3.1-migration` confirms landing selection, real 8×8 map, survey timing and resource reveal on device.

Phase 3 should begin only after the hands-on validation build is accepted. Phase 3 will replace the temporary survey-only day control with the complete colony survival/daily simulation pipeline.

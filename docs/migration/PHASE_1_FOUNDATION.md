# MineIT Android Migration — Phase 1 Native State and Persistence Foundation

**Phase:** 1 — Native state and persistence foundation  
**Status:** Complete  
**Started:** 4 September 2026  
**Completed:** 4 September 2026  
**Source behavioural baseline:** `kevvy555/MineIT` commit `9e58983adaa7a15cd525451266ce9df3c17ae886`, game `5.13.15`, web save `16`  
**Android branch:** `feature/migration-phase-1`  
**Final validated head:** `b6757ed2dbc523f6e2a93d6131be258470ad3470`  
**Final validation:** Android CI run `33850648863` — success

## Source reviewed before implementation

The Phase 1 implementation was grounded against:

- source and Android `AGENTS.md` contracts;
- `js/core/game-store.js` and `tests/game-store.test.js`;
- `js/persistence/save-repository.js`;
- `js/domain/game-state.js`;
- `js/domain/game-state-runtime.js`;
- `js/domain/inventory-service.js`;
- `js/data/resources.js`;
- `tests/save-roundtrip.test.js`;
- the Phase 0 representative web-v16 parity fixture.

## Architecture implemented

### Canonical root state

The production native state begins deliberately small:

```text
GameState
├── GameDate
├── CompanyState
├── List<ColonyState>
└── activeColonyId
```

The root already enforces unique colony IDs and a valid active-colony reference. It will be extended as later migration phases port the real world, contract, trade, ship and infrastructure domains.

The POC remains isolated under `domain/poc` and is not reused as production state.

### Strong identity/value types

Phase 1 introduces:

- `ColonyId`;
- `ShipId`;
- `ResourceId`;
- `AbsoluteDay`;
- `GameDate`.

Phase 2 source-parity review identified that Phase 1 had initially set `GameDate.DAYS_PER_YEAR` to 365 while the canonical web source uses **360**. Phase 2 corrected the permanent native calendar to 360 days and added regression coverage. The incorrect 365 value never represented an approved gameplay divergence.

These types are used where cross-domain ID/unit confusion would create real defects; the migration does not wrap every primitive.

### Resource-safe inventory foundation

The native inventory is generic and resource-ID based. A resource stock records:

- stable `ResourceId`;
- current `ResourceCategory` (`food`, `build`, `fuel`, `ore`);
- quality-band quantities.

Total amount is derived from the quality bands rather than persisted redundantly.

The current six web quality bands are preserved: common, good, excellent, exceptional, rare and extraordinary.

This improves architecture without adding, renaming or rebalancing resources. The later resource overhaul remains separate.

### Root state ownership

`GameSession` replaces the role of browser `GameStore` at the native application boundary:

- owns `StateFlow<GameState>`;
- exposes immutable state;
- serialises commits through a mutex;
- applies explicit state transitions;
- persists committed state;
- exposes revision and persistence diagnostics.

Unlike web `GameStore`, native code does not preserve mutable object identity for UI/service references. Compose consumes immutable state emissions instead.

### Save format

Native saves start at format version `1` and use a versioned `SaveEnvelope`:

```text
SaveEnvelope
- formatVersion
- gameVersion
- universeContentVersion
- universeSourceCommit
- savedAtEpochMillis
- state
```

The game version is bumped to `0.2.0-migration`, Android version code `5`.

### Explicit native migration chain

Native save migrations are ordered one-version-at-a-time transformations. Future-version saves and missing migration steps fail explicitly.

No fake production `v0 → v1` migration is included. Tests inject a test-only migration to prove the mechanism until a real native v2 exists. Phase 2 later introduced the first real production migration, `v1 → v2`.

### File persistence and recovery

`FileGameStatePersistence` stores app-private JSON using:

1. encode current envelope;
2. write a temporary file;
3. flush and filesystem-sync the temporary file;
4. decode/validate the temporary file;
5. preserve the currently active save as previous-good backup only if that active save itself validates;
6. atomically replace the active file where supported, with a replace fallback when atomic move is unavailable.

Files:

- `mineit-save.json` — active save;
- `mineit-save.previous.json` — previous validated active save;
- temporary files exist only during promotion.

Load behavior:

- valid active → load active;
- invalid/missing active + valid backup → recover backup and report recovery;
- neither valid → explicit failure diagnostics;
- neither exists → `NotFound`.

A corrupt active save is never copied over a healthy previous-good backup.

### Web-v16 compatibility boundary

`WebSaveV16Importer` is isolated under `data/save/importer`.

Phase 1 validates and extracts a preview of the real v16 source shape, including:

- source version;
- game date;
- active colony ID/name/population/seed;
- company cash/reputation;
- colony count;
- active-colony generic inventory and quality bands.

It intentionally does **not** pretend to fully map ships, tiles, contracts, buyers, events and multi-colony simulation before those native domains are implemented. Those mappings will be extended through this one importer boundary as the corresponding phases land.

### Durable versus transient state

The initial canonical native `GameState` does not persist web `speed`, camera coordinates or selected UI state.

This is deliberate separation, not lost functionality:

- game speed belongs to session/preferences unless a later port proves durable gameplay semantics require it;
- camera/selection are presentation/session state;
- imported web fields are not discarded from a real migration path until the relevant feature is implemented and parity-tested.

## Development diagnostics foundation

`GameSessionDiagnostics` currently exposes:

- revision;
- last action;
- save/load/failure/not-found status;
- active vs backup load source;
- backup-recovery flag;
- save format version;
- game version;
- Universe content version/source commit when provided by the build/runtime composition;
- save timestamp.

A later debug UI can render this without moving diagnostics into gameplay state.

## Test coverage added

Phase 1 adds tests for:

- GameDate/AbsoluteDay conversion;
- canonical active-colony invariants;
- unique generic resource identity;
- quality-band-derived resource totals;
- explicit migration sequencing/future-version rejection/missing-step rejection;
- native save round-trip and metadata;
- previous-good backup creation;
- corrupt-active backup recovery;
- protection of a healthy backup when the active save is already corrupt;
- explicit failure when no valid save exists;
- web-v16 fixture parsing and quality-band preservation;
- `GameSession` commit ownership/persistence diagnostics;
- fresh-session restore of an identical previously saved canonical state.

## Phase 1 exit gate

Phase 1 is complete:

- [x] canonical `GameState` and selected value/ID types exist;
- [x] generic resource identity/quantity/quality foundation exists without resource-catalogue redesign;
- [x] `GameSession` owns immutable native state;
- [x] native `SaveEnvelope` v1 exists;
- [x] atomic active-save + previous-good backup persistence exists;
- [x] save/recovery tests exist;
- [x] web-v16 importer boundary parses the representative source fixture;
- [x] explicit native migration chain exists;
- [x] obvious transient UI/session state is excluded from the initial durable root;
- [x] initial save/schema/source diagnostics exist;
- [x] Android CI passed unit tests, APK assembly, signer verification and artifact upload on the final branch head;
- [x] `0.2.0-migration` / version code `5` is configured with the existing persistent development signer.

## Next phase

Phase 2 replaces illustrative domain data with real MineIT Contract 01 definitions: typed configuration, current resource catalogue on the generic inventory foundation, seeded world/tile generation, survey discovery rules and parity fixtures. It preserves the current resource economy while continuing to avoid the later resource-overhaul redesign.

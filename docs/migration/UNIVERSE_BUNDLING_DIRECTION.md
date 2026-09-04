# MineIT Android Migration — MineIT-Universe Bundling Direction

## Decision

Core MineIT-Universe data and required artwork will be **pinned and bundled into the Android application at build time** so the installed game is fully playable offline.

`kevvy555/MineIT-Universe` remains canonical. Android must not become a separately hand-maintained copy of canonical Universe records.

## Intended pipeline

```text
MineIT-Universe canonical repository
        ↓
pinned source commit + content version
        ↓
sync/validation task
        ├─ validate manifest/schema compatibility
        ├─ validate IDs/references
        ├─ validate required image.keys/assets
        └─ select/copy game-required data and art
        ↓
Android bundled assets
        ↓
APK/AAB install-time content
        ↓
UniverseRepository (read-only definitions)
```

## Runtime rule

The game must not need a network request to start a new game, load a save, display required canonical data, or display required gameplay artwork.

A future optional content-refresh mechanism may be considered separately, but it must not make the network a core dependency.

## Provenance

Every Android build should be able to report:

- Universe schema version;
- Universe content version;
- pinned Universe source commit;
- validation/snapshot version where useful.

This metadata should also be available in development diagnostics and may be stored in save metadata where it helps migration/reproducibility.

## Validation expectations

The build/CI integration should fail clearly when required canonical data is inconsistent, including examples such as:

- unsupported manifest/schema version;
- duplicate canonical IDs;
- references to missing organisations/manufacturers/classes/etc.;
- runtime profiles referencing missing entities;
- required `image.key` assets missing;
- malformed required JSON;
- a snapshot provenance mismatch.

Validation should be scoped to constraints MineIT actually depends on; Android should not attempt to become a second full Universe validator if the Universe repository already owns a rule.

## Data representation

Prefer bundling canonical JSON/data and adapting it through `UniverseRepository` rather than translating every canonical record into manually maintained Kotlin constants.

Kotlin domain models may provide typed read-only views over bundled data where that improves safety.

Runtime game state must never mutate the canonical Universe definitions.

## Artwork size strategy

Initially bundle required artwork directly with the app. If total art size later becomes large enough to materially affect distribution, use an install-time Android/Play asset-delivery strategy that still gives the player the required assets locally after installation.

Do not move required core art to network-only loading merely to reduce APK size.

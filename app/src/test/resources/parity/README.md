# Cross-runtime parity fixtures

These fixtures let the native Kotlin migration consume representative state from the web MineIT implementation without requiring production Android models to preserve JavaScript representation details.

## Layout

- `web-v16/` — fixtures captured from the MineIT web save schema version 16 baseline.
- Later native schema fixtures should use their own versioned directories when needed.

## Required fixture metadata

Every fixture must identify:

- source repository;
- source branch;
- exact source commit;
- source game version;
- source save/schema version;
- a human-readable scenario name/description.

## Comparison rule

Parity tests compare canonical gameplay outcomes and important durable state. They must not fail merely because Kotlin uses different object ordering, type names, internal data structures or UI presentation.

Any intentional semantic difference must be recorded in `docs/migration/INTENTIONAL_DIVERGENCES.md`.

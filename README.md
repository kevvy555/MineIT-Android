# MineIT Android

Native Android proof of concept for MineIT, built with Kotlin and Jetpack Compose.

## Purpose

This repository exists to prove that MineIT can move from its current HTML/JavaScript implementation to a maintainable native Android client without committing to a full rewrite up front.

The POC intentionally stays small. It proves:

- native Compose rendering with no WebView;
- a mobile-first MineIT-style status/resource layout;
- a tappable sector grid;
- immutable gameplay state;
- gameplay rules isolated from the UI;
- deterministic day simulation with unit tests;
- CI that builds a real debug APK.

## Architecture

```text
MainActivity
    |
    v
MineItApp / Compose UI
    |
    v
GameViewModel
    |
    v
GameState + GameSimulation
```

### `domain/`

Pure Kotlin gameplay models and rules. This is the authoritative source of gameplay behaviour and must not depend on Compose or Android UI APIs.

### `ui/`

Jetpack Compose presentation and Android lifecycle state holders. UI renders domain state and dispatches player intent; it does not own gameplay rules.

### Persistence and external data

Not introduced in the first POC because there is no real persistence requirement yet. If the conversion continues, persistence and MineIT-Universe adapters will be added at a clear data/infrastructure boundary rather than inventing unused abstractions now.

## POC behaviour

The app starts with a small founding colony and a deterministic 6x6 sector map. A player can:

- inspect Food, Water, Ore and Credits;
- see colony Population and Power status;
- tap sectors and inspect survey/richness information;
- advance the simulation by one day.

Advancing a day currently consumes Food and Water and produces Ore. A power shortage reduces Ore output. These values are intentionally illustrative rather than a port of the current MineIT balance.

## Build

Current stable POC toolchain:

- Android Gradle Plugin 9.3.0
- Kotlin 2.3.21
- Jetpack Compose BOM 2026.06.00
- compile/target SDK 36
- JDK 17

API 37 / Android 17 remains a preview SDK at the time this POC was created, so the POC deliberately uses stable API 36 rather than taking a preview-platform dependency.

GitHub Actions runs unit tests and builds `app-debug.apk` on pushes to `main` and feature branches. Superseded builds on the same branch are cancelled to avoid wasting CI minutes.

## If the POC is accepted

The next step should not be a large rewrite. Port vertical slices while preserving the current web game as the behavioural reference. A sensible order is:

1. persistence/save state;
2. canonical game clock and root state ownership;
3. colony/building domain rules;
4. resource extraction and power;
5. map/survey interactions;
6. ships and travel;
7. contracts/trading;
8. MineIT-Universe data/art integration.

Only split the project into additional Gradle modules when the codebase reaches a size where module boundaries provide a measurable maintenance or build benefit.

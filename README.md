# MineIT Android

Native Android proof of concept for MineIT, built with Kotlin and Jetpack Compose.

## Purpose

This repository exists to migrate MineIT from its current HTML/JavaScript implementation to a maintainable native Android client. The initial POC has proven native rendering, clean domain/UI separation, CI-built APKs and in-place signed updates; the project can now move forward as an incremental native migration rather than a throwaway experiment.

The migration remains deliberately incremental. The existing web game is the behavioural reference while features are ported and verified.

## Migration guide

The authoritative living migration plan is [docs/WEB_TO_ANDROID_MIGRATION.md](docs/WEB_TO_ANDROID_MIGRATION.md).

It defines the target architecture, web-to-native ownership map, save-v16 import strategy, MineIT-Universe integration, HTML/CSS-mock-to-Compose workflow, parity testing, migration phases, cleanup rules and production cutover criteria.

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

The POC structure above will evolve into the production architecture described in the migration guide. The key boundary remains unchanged: gameplay truth belongs to the domain/application state owner; Compose renders state and dispatches intent.

### `domain/`

Pure Kotlin gameplay models and rules. This is the authoritative source of gameplay behaviour and must not depend on Compose or Android UI APIs.

### `data/`

Persistence, MineIT-Universe adapters and external data boundaries will live here as the migration begins.

### `ui/`

Jetpack Compose presentation and Android lifecycle state holders. UI renders domain state and dispatches player intent; it does not own gameplay rules.

## Current POC behaviour

The current app still contains the lightweight proof-of-concept gameplay used to validate the native stack. It provides a small founding colony, a deterministic 6x6 sector map, resource/status display, sector selection and a simple advance-day simulation. These values are illustrative and will be replaced by the real MineIT state/domain in the first migration phases.

## Build

Current stable toolchain:

- Android Gradle Plugin 9.3.0
- Kotlin 2.3.21
- Jetpack Compose BOM 2026.06.00
- compile/target SDK 36
- JDK 17

API 37 / Android 17 remains a preview SDK at the time this POC was created, so the project deliberately uses stable API 36 rather than taking a preview-platform dependency.

GitHub Actions runs unit tests and builds `app-debug.apk` on pushes to `main` and feature branches. Superseded builds on the same branch are cancelled to avoid wasting CI minutes.

### Development APK signing

CI test APKs use the pinned public development keystore from `@react-native-community/template@0.83.1`. This is intentionally a development-only signing identity so successive test APKs can update an installed build in place. It must never be used for a production or Google Play release.

The expected development signing certificate SHA-256 fingerprint is:

`fac61745dc0903786fb9ede62a962b399f7348f0bb6f899b8332667591033b9c`

CI verifies the APK signer before uploading the artifact. A future production release will use a separate private release/upload key and normal Google Play signing.

## Next milestone

Begin Phase 0 and Phase 1 of the migration guide:

1. replace the illustrative POC state with the production migration foundation;
2. establish canonical `GameSession`/root state ownership;
3. implement versioned atomic persistence;
4. add web save-v16 import/parity fixtures;
5. introduce the first real Contract 01 state fixture.

Only split the project into additional Gradle modules when the codebase reaches a size where module boundaries provide a measurable maintenance or build benefit.

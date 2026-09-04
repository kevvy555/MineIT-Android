# Phase 4 validation support — native start/restart flow

**Added:** 4 September 2026  
**Build:** `0.5.1-migration` / version code `10`  
**Branch:** `feature/migration-phase-4`  
**Purpose:** remove the browser-era requirement to clear local data manually when a test colony is dead.

## Behaviour

The Android migration build now enters through a small native main menu instead of dropping directly into the persisted colony.

- `CONTINUE` is shown only when a valid persisted game exists and its active colony is not dead.
- `NEW GAME` creates the canonical Contract 01 start state through the existing `NewGameFactory` and `GameSession` owners.
- New Game performs a deliberate persistence reset: it atomically writes the fresh active save and removes recovery history from the previous run, so a dead previous colony cannot later be resurrected from the previous-good backup.
- If the active colony dies during daily simulation, the simulation clock is paused immediately and the app returns to the main menu.
- A persisted dead colony is recognised on the next app launch; Continue is withheld and New Game remains available.
- Save loading finishes before the menu actions become enabled, preventing a startup race between restore and reset.

This is intentionally a minimal validation/start flow. Final main-menu presentation, navigation structure and production design-system treatment remain Phase 5 work.

## Architecture

No alternate game state or reset engine was introduced.

- `NewGameFactory` remains the canonical new-game state creator.
- `GameSession` remains the authoritative root-state owner.
- `GameStatePersistence.reset()` is the persistence boundary for deliberately replacing an old run.
- `FileGameStatePersistence` implements reset without retaining the old active/backup save history.
- `GameViewModel` owns only transient menu/game presentation state and pauses/routes away from a dead colony.
- Compose renders `MainMenuScreen` or the existing game validation surface and dispatches intent.

## Regression coverage

Added JVM coverage proves that:

- a persistence reset writes the requested fresh state;
- previous-run backup history is removed;
- corrupting the fresh active save cannot fall back to the discarded previous run;
- `GameSession.reset()` replaces the authoritative root state and clears previous load/recovery diagnostics.

The existing Phase 0–4 regression suite remains active. The final signed APK/CI result for this validation-support build should be recorded when the build completes.

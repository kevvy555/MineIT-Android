# Phase 7 Commercial Safe-Area Regression Checkpoint

## Trigger

Manual Android validation of `0.7.12-migration` found a blocking edge-to-edge layout defect: opening the Corporate Trade Ship between visits placed the full-screen header and its `CLOSE` action underneath the Android status-bar UI.

## Root cause

`MineItScreen` is hosted inside a `Scaffold`, so normal gameplay receives system-bar insets. The full-screen commercial surfaces are layered directly over the game from `MineItApp` and bypass that `Scaffold`, so they previously rendered from the physical display edge.

## Fix

`MineItApp` now hosts every full-screen commercial surface inside a container using Compose `safeDrawingPadding()`.

This applies consistently to:

- Corporate Trade Ship
- Contract
- Conglomerate Buyers Service
- Game Log

The commercial background remains full-screen relative to its safe container while interactive content is kept clear of status bars, display cutouts and bottom navigation/gesture UI.

## Validation build

- Version: `0.7.13-migration`
- Version code: `25`

## Manual regression checks

1. Open Corporate Trade Ship while no ship is docked; verify `CLOSE` is fully visible and tappable below the phone status area.
2. Open Corporate Trade Ship while docked; verify the header remains clear of the status area and `SHIP DEPARTS` remains above the bottom gesture/navigation area.
3. Switch to Contract, Buyers and Log; verify each screen's top-right `CLOSE` control remains visible and tappable.
4. Rotate through screens using commercial tabs and close back to the map.
5. Verify Android Back still closes the commercial surface as before.

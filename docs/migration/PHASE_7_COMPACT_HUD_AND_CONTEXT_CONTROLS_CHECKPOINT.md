# Phase 7 Compact HUD and Context Controls Checkpoint

## Purpose

This checkpoint follows the 0.7.11 on-device layout review. The player explicitly preferred the larger full-viewport colony map, but identified three remaining layout problems:

1. sector/tile actions were covering too much of the map,
2. the top HUD used more vertical space than the information required,
3. the standalone `COLONY / DETAILS` strip duplicated information that belongs in Headquarters / Colony Control.

## Implemented in 0.7.12-migration

### Static map and sector controls

- Kept the large rectangular 8x8 map introduced in 0.7.11.
- Removed the large sector-action panel from over the map.
- Added a fixed 26dp sector information rail immediately below the map.
- The information rail is always present so selection changes do not resize the map.
- When no sector is selected it provides the compact interaction hint.
- When one or more sectors are selected it shows coordinates / terrain / deposit or requirement context.
- Sector actions now replace the normal bottom control rail while a selection exists.
- Clear/revealed sectors expose compact Power, Housing, Industry, HQ, Develop (when applicable), and Close actions in the bottom rail.
- Unsurveyed sectors expose Survey and Close in the bottom rail.
- Multi-selection exposes Queue and Close in the bottom rail.
- Developed sectors expose Upgrade, Primary (when applicable), Demolish and Close in the bottom rail.
- The footer remains the same height when switching between simulation and sector actions.

### Compact top HUD

- Retained the same Housing, Power, Industry, Workforce, Food, Build, Fuel and Ore information.
- Each HUD card is now two compact lines rather than stacked ship/colony/detail rows.
- Ship and colony values are presented inline inside each card.
- Resource runway / production detail is folded into the same value line.
- Removed the standalone `COLONY / DETAILS` strip entirely.
- The critical/attention notification remains directly below the MineIT title row.

### Colony Control ownership

- Once a usable Headquarters exists, Colony Control resolves to the Headquarters surface rather than a separate duplicate colony-detail sheet.
- The Headquarters / Colony Control surface now includes colony-wide:
  - Population
  - Power
  - Workforce
  - Industry
  - Command
  - Spaceport
  - operational status and detail
- Existing HQ-specific command, staffing, power, handover, corporate-link and upgrade information remains present.
- Before a Primary HQ exists, the early-establishment colony-detail fallback is retained internally so setup remains usable.

## Manual validation requested

On the 0.7.12 APK verify:

1. The map remains the same size when selecting and clearing sectors.
2. No sector action buttons cover the map.
3. Sector information appears only in the narrow rail directly above the footer.
4. Sector actions use the bottom button rail.
5. Closing the selection restores Pause / 1x / 2x / 4x / +1D / Comm / Handover / Menu controls as applicable.
6. The top HUD is visibly shorter while retaining all eight headline metrics.
7. The standalone Colony / Details strip is gone.
8. Notification remains directly beneath the title row.
9. Tapping a Headquarters opens the consolidated Headquarters / Colony Control surface.
10. The HQ surface includes colony Power, Workforce, Industry and Spaceport status in addition to command/HQ information.
11. Opening Colony Control from a ship/attention route uses the Primary HQ surface once one exists.
12. Map drag-survey hit testing still aligns with the rectangular cells.

## Validation state

- Source implementation compiled and passed the full Android CI suite before the validation build bump.
- Validation build: `0.7.12-migration`, versionCode `24`.
- Exact-head CI and device acceptance remain required before this checkpoint is accepted.

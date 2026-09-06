# Phase 7 — Map Viewport Layout Checkpoint

**Status:** Native layout rewrite implemented; physical-device validation pending  
**Date:** 6 September 2026  
**Branch:** `feature/migration-phase-7`  
**Validation build:** `0.7.10-migration` / version code `22`  
**Native save format:** `7` (unchanged)

## Trigger

Physical-device testing of `0.7.9-migration` showed that the gameplay screen was functionally usable but the colony map was still substantially smaller than it should be and visibly changed position/size during routine interaction.

The supplied Pixel screenshot showed four structural causes rather than one isolated spacing defect:

1. the top-level screen consumed system-bar insets through `Scaffold` and then applied `statusBarsPadding()` / `navigationBarsPadding()` a second time, producing avoidable black space above and below the game;
2. transient `statusMessage` content participated in the vertical `Column`, so messages appearing/disappearing changed the remaining map height;
3. `SectorContextBar` reserved 148dp even for a simple clear-sector selection;
4. map filters, commercial access and simulation controls all used the same full-height button treatment as primary dialog actions.

The top game-information HUD itself was explicitly retained unchanged.

## Layout rewrite

The established-colony portion of `MineItScreen` is now treated as a stable gameplay viewport beneath the unchanged HUD and attention strip.

### Static vertical structure

The viewport now consists of fixed-height control rails around one weighted map region:

- compact Commercial rail;
- compact horizontally-scrollable map/filter rail;
- one weighted map region;
- fixed 88dp sector-context rail;
- compact simulation/footer rail.

The map therefore receives all remaining height and no longer competes with variable-height selection/status content.

### System insets

`Scaffold` remains the single system-inset owner. The duplicate explicit status/navigation bar padding was removed from the content column. This recovers the unnecessary black space while retaining safe-area handling.

### Transient status messages

Operational-game `statusMessage` is now rendered as a small overlay at the top of the map region. It does not enter or leave the vertical layout, so a new message cannot resize the map.

Site-selection status remains in normal flow because the colony map is not present in that mode.

### Sector context

`SectorContextBar` remains constant-height, but its fixed allocation has been reduced from 148dp to 88dp.

- common sector title/summary and action controls fit in the compact rail;
- richer deposit/development/requirement content scrolls inside that same rail;
- selection, deselection, multi-select and action availability do not change the rail height.

### Compact controls

The shared design components now support an opt-in `compact` treatment. Normal dialog/detail controls retain their existing sizing. Only high-frequency gameplay rails use the compact treatment.

This avoids globally shrinking controls throughout the application while reclaiming vertical space where map area has higher priority.

## CI evidence

The complete layout rewrite at commit `f48edaa440ef44c51285be4b987ffe6c7d884ee2` passed Android CI run `34044337182` / run `364`, including the unit/regression suite, debug APK build, persistent signer verification and artifact upload.

A fresh exact-head run for the `0.7.10-migration` build/docs checkpoint must pass before the APK is supplied for device testing.

## Physical-device validation

The next device pass should focus on geometry rather than feature parity:

1. Confirm the large black gap above `MINEIT` is substantially reduced while the status bar remains safe/readable.
2. Confirm the top information HUD itself is visually unchanged.
3. Confirm the map is materially larger than in `0.7.9`, ideally using most of the available screen width.
4. With no sector selected, note the exact map size and position.
5. Select a clear surveyed sector, resource sector, developed building and Headquarters; the map must not resize or shift.
6. Clear selection; the map must remain identical in size and position.
7. Trigger a normal status message such as simulation pause/action feedback; the message may overlay the map briefly but must not resize it.
8. Switch between ALL / PROBLEMS / BUILDINGS / POWER / INDUSTRY and other map filters; the map geometry must remain static.
9. Confirm the compact Commercial, filter and simulation controls remain comfortably tappable on the physical device.
10. Confirm richer sector details can be reached by scrolling inside the 88dp context rail rather than expanding the page.
11. Confirm the bottom controls remain above the Android gesture/navigation area.
12. Confirm no clipping/overlap occurs on the current Pixel device.

If the map is still materially narrower/smaller than the screen after this rewrite, the next optimization should be structural consolidation of the Commercial and filter rails rather than further shrinking the unchanged top HUD.

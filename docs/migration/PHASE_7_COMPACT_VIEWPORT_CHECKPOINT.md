# Phase 7 — Compact Gameplay Viewport Checkpoint

**Status:** Implementation complete; physical-device validation pending  
**Date:** 6 September 2026  
**Branch:** `feature/migration-phase-7`  
**Validation build:** `0.7.11-migration` / version code `23`  
**Native save format:** `7` (unchanged)

## Device feedback driving this pass

The `0.7.10-migration` physical-device screenshot showed that the gameplay screen was still spending too much vertical space outside the colony grid even after the first static-map rewrite.

Requested changes:

1. move the colony notification directly below the title header;
2. make the survey/scanning panel as compact as possible;
3. fit Commercial and Handover into the bottom gameplay control strip;
4. remove wasted black space and maximise the map while keeping it static.

## Layout changes

### Header and notification

- The existing MineIT title/cash/date and operational/resource cards remain structurally unchanged.
- The colony attention notification now renders directly after the title row inside `GameHeader`, before the operational/resource cards.
- The attention presentation is a single compact line: title, detail and action remain visible without a two-line card.

### Map-first viewport

- The previous dedicated Commercial rail has been removed.
- Horizontal gameplay padding is reduced to 2dp and vertical layout gaps to 1dp.
- The map toolbar uses the compact 32dp control treatment.
- The colony grid now consumes the complete map viewport rather than forcing itself into a square and leaving unused black space above/below it. The 8x8 coordinate mapping remains based on the actual measured width and height, so touch/drag surveying remains aligned to the visible cells.
- Transient status messages remain overlays and therefore do not alter the map dimensions.

### Sector context

- The persistent empty `SELECT A SECTOR` panel has been removed from the normal layout.
- Sector context now appears only when a sector is selected.
- It is a compact 58dp overlay on the bottom of the map, so opening/closing it never changes map size.
- Build, survey, upgrade, set-primary, demolish and close actions are arranged horizontally and can scroll within the overlay.
- Resource quality and key sector information remain in the one-line summary.

### Survey HUD

- Survey status is now a small two-line overlay in the map corner.
- It shows scanning level, active-slot usage, queue count, lead coordinate/type/days and a 2dp progress bar.
- Its width is limited to 82–116dp and its internal padding/text size is reduced.

### Bottom control rail

The separate floating Handover button and full-width Commercial button are removed.

The bottom strip now contains the relevant controls together:

- PAUSE
- 1×
- 2×
- 4×
- +1D
- COMM
- HANDOVER (only while establishment/handover is still required)
- MENU

Compact gameplay buttons use a 32dp visual height and 4dp horizontal content padding.

## Behaviour preserved

This pass is presentation/layout only. It does not change:

- simulation speed semantics;
- surveying rules or queue behaviour;
- Commercial panel behaviour;
- establishment/handover rules;
- building eligibility or upgrade rules;
- save format.

## Physical-device validation

Install `0.7.11-migration` and verify:

1. the alert sits directly under the MineIT title row;
2. the operational/resource cards remain readable and otherwise unchanged;
3. there is no large empty black band between header, alert, controls or map;
4. the 8x8 map uses the available gameplay viewport, including taller phones;
5. tapping/clearing different sectors does not resize or move the map;
6. the sector overlay appears over the map only while a sector is selected;
7. all sector actions remain reachable by horizontal scrolling when needed;
8. the scan overlay is substantially smaller but still readable;
9. COMM opens the Commercial panel from the bottom strip;
10. HANDOVER opens the establishment/handover flow from the bottom strip and the old floating button is gone;
11. speed, +1D and MENU remain usable;
12. drag surveying still maps correctly across all eight rows and columns despite the map now using the full rectangular viewport.

A screenshot from the same device/game state as the `0.7.10` review is the preferred visual comparison.

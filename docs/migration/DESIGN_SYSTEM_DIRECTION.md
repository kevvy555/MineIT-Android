# MineIT Android Migration — Design System Direction

## Goal

Use the native migration to make MineIT visually and interactively consistent without turning the migration into a wholesale redesign.

The existing game's successful concepts remain the starting point: map-first play, compact operational information, resource/status visibility, touch-first actions, dark industrial presentation and modal/detail flows.

The migration should standardise how those concepts are expressed across every feature.

## Rule

**Preserve successful UX concepts; standardise the visual and interaction language.**

For each migrated screen:

1. keep existing behaviour/layout intent when it works;
2. normalise inconsistent styling/interaction through the design system;
3. improve genuinely poor UX when the improvement is low-risk and does not change gameplay rules;
4. record any material player-flow divergence in the migration divergence log.

## HTML mock workflow remains valid

HTML/CSS remains the fast visual prototyping layer when useful:

```text
feature idea
  → HTML/CSS mock
  → visual approval
  → Compose implementation using MineIT primitives
  → Preview/screenshot regression
  → APK hands-on check when required
```

HTML is a visual specification, not runtime production UI.

## Design tokens

The native design system should centralise at least:

- semantic colours (surface, raised surface, accent, success, warning, critical, disabled, selection);
- typography scale and weights;
- spacing scale;
- corner-radius scale;
- border/elevation rules;
- icon/image sizing;
- progress/status treatments;
- animation durations/easing;
- minimum touch targets;
- haptic intent where appropriate.

Avoid one-off literal values scattered through feature screens when a shared semantic token exists.

## Core native primitives

Create reusable composables as real repetition appears. Expected examples include:

- `MineItPanel`;
- `MineItSectionHeader`;
- `MineItResourceCard`;
- `MineItStatRow`;
- `MineItPrimaryButton` / secondary/destructive variants;
- `MineItProgressMeter`;
- `MineItStatusBadge`;
- `MineItDialog` / sheet/full-screen shell;
- `MineItListRow`;
- `MineItBuildingCard`;
- `MineItShipCard`;
- consistent empty/loading/error states.

Names are directional, not a requirement to create all components before they are needed. KISS/YAGNI still applies.

## Consistency audit areas

During migration, explicitly compare presentation of the same concepts across:

- resource quantities and quality;
- money/prices/costs;
- Power/workforce/Industry capacity and shortages;
- building levels, construction, upgrades and disabled actions;
- ship capacity/cargo/passengers/Fuel/status;
- contract/buyer status;
- warnings and destructive confirmations;
- selection/highlight states;
- progress/queue states;
- modal/back/navigation behaviour;
- number/date/percentage formatting.

A concept should not acquire a different visual grammar merely because it appears in another feature.

## Interaction principles

- standard taps use Compose semantic click handling;
- pointer input is reserved for gestures such as drag/long-press/multi-select;
- avoid competing activation paths;
- Android Back must behave predictably through panels/screens/dialogs;
- important actions have adequate touch targets;
- accessibility semantics are part of the component contract;
- haptics may reinforce meaningful native actions but must remain subtle and consistent.

## Testing

As the production design system replaces the POC UI:

- add Compose previews for reusable components;
- add screenshot baselines for representative states, not every trivial permutation;
- test important enabled/disabled/warning/selected states;
- keep gameplay assertions in domain tests rather than screenshot tests.

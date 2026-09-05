# MineIT Android Migration — Design System Direction

## Goal

Use the native migration to make MineIT visually and interactively consistent without turning the migration into a wholesale redesign.

The existing game's successful concepts remain the starting point: map-first play, compact operational information, resource/status visibility, touch-first actions, dark industrial presentation and modal/detail flows.

The migration should standardise how those concepts are expressed across every feature while preserving the recognisable MineIT information hierarchy.

## Rule

**The current web MineIT UI is the layout and information-hierarchy reference as well as the behavioural reference. Preserve what works, and improve deliberately rather than redesigning by default.**

For each migrated screen:

1. inspect the current web HTML/view, CSS, UI/controller and relevant domain owner together;
2. preserve the recognisable screen structure, major control placement, information ordering and interaction model when they work;
3. identify any weak or inconsistent UX explicitly before changing it;
4. normalise inconsistent styling/interaction through the native design system;
5. improve genuinely poor UX where the improvement is deliberate, low-risk and does not move gameplay truth into presentation;
6. use Android-native behaviour where it genuinely improves the experience, such as safe-area handling, Back behaviour, accessibility, touch targets, sheets/dialogs, haptics and responsive sizing;
7. record material player-flow or gameplay-semantic divergence in the migration divergence log.

The target is not a pixel-for-pixel DOM recreation, but a player familiar with the web game should immediately recognise the same screen, hierarchy and workflow unless a deliberate improvement has been agreed.

## UI-led vertical migration workflow

From Phase 6 refinement onward, player-facing migration proceeds as complete UI/feature slices rather than large backend-only phases followed by a later UI catch-up.

For each slice:

```text
existing web screen
  + HTML/view
  + CSS
  + UI/controller
  + domain owner/tests
      ↓
identify preserve / refine / replace decisions
      ↓
confirm native backend capability already exists
      ↓
port any missing domain/application behaviour in canonical owners
      ↓
Compose screen using MineIT design primitives
      ↓
interaction/regression coverage
      ↓
APK hands-on validation
```

The UI does not own the roadmap mechanically. Cross-cutting domain prerequisites may still be implemented first when several screens depend on them, but they should be driven by the next complete player-facing slice rather than accumulated as invisible migration work.

## Preserve vs improve

### Preserve by default

Unless there is a clear reason to change them, preserve:

- overall screen purpose and navigation relationship;
- major information hierarchy and ordering;
- map-first proportions and compactness;
- placement of the primary action area;
- established resource/status groupings;
- recognisable scanning, trade, contract, buyer, colony, Headquarters and ship workflows;
- familiar terminology and status meaning.

### Improve deliberately

Native migration is a good opportunity to improve:

- screens that are currently cramped, unclear or inconsistent;
- touch target sizing;
- safe-area/system-bar handling;
- typography/readability;
- responsive sizing across phones;
- modal/sheet behaviour where Android conventions are better;
- Back/navigation behaviour;
- accessibility semantics;
- repeated visual grammar through shared tokens/components;
- excessive information duplication or unclear status emphasis.

A new layout should not be introduced merely because Compose makes it convenient.

## HTML mock workflow remains valid

HTML/CSS remains the fast visual prototyping layer when useful:

```text
feature idea or proposed refinement
  → inspect current web screen first
  → HTML/CSS mock when a material layout change is proposed
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
- survey/scan queue, progress, selected and completed states;
- ship capacity/cargo/passengers/Fuel/status;
- contract/buyer status;
- warnings and destructive confirmations;
- selection/highlight states;
- progress/queue states;
- modal/back/navigation behaviour;
- number/date/percentage formatting.

A concept should not acquire a different visual grammar merely because it appears in another feature.

## Warning and attention consistency

Warnings are gameplay guidance, not merely styling.

When migrating warning/attention behaviour:

- inspect current source HTML/view, CSS, controller logic and the metric/domain owners that determine the condition;
- preserve the source severity meaning and action/navigation target unless a defect is found;
- distinguish persistent attention state from one-shot critical interruption/modals;
- use typed state rather than parsing or classifying message strings in Compose;
- ensure entering, clearing and re-entering a condition works correctly;
- check simultaneous-warning priority explicitly;
- verify ship and colony populations/inventories are not accidentally mixed;
- add regression coverage for any clear defect before or with the fix;
- where practical, correct a clear maintained-web bug first and then port the corrected rule.

The current Phase 7 planning audit has already identified one source defect: Housing-near-capacity warning logic uses total population instead of planetary resident count, which is incorrect while N05 residents remain aboard the founding ship. This is recorded in `PHASE_7_SINGLE_COLONY_PARITY.md` and must be regression-tested/corrected before native warning parity is accepted.

Do not build a speculative generic notification framework. Use the smallest explicit attention model that cleanly represents the current game.

## Interaction principles

- standard taps use Compose semantic click handling;
- pointer input is reserved for gestures such as drag/long-press/multi-select;
- avoid competing activation paths;
- Android Back must behave predictably through panels/screens/dialogs;
- important actions have adequate touch targets;
- accessibility semantics are part of the component contract;
- haptics may reinforce meaningful native actions but must remain subtle and consistent.

## Testing

As the production design system replaces and refines migrated UI:

- add Compose previews for reusable components;
- add screenshot baselines for representative states, not every trivial permutation;
- test important enabled/disabled/warning/selected states;
- add interaction coverage for meaningful screen workflows;
- keep gameplay assertions in domain tests rather than screenshot tests;
- compare representative native screens against the current web hierarchy during hands-on review.

## Phase 5, Phase 6 and Phase 7 direction

Phase 5 established the native design system and production map shell successfully, but subsequent device review showed that some detail/commercial presentation had drifted too far from the web layout and information hierarchy.

That feedback changes the migration method, not the underlying Phase 1–6 domain architecture:

- the accepted Phase 5 map-first foundation remains valid;
- shared tokens/primitives remain valid;
- the current Phase 6 commercial/domain implementation remains the canonical backend;
- Phase 6 is accepted only after its revised physical-device validation is explicitly approved;
- the next migration phase is now **Phase 7 — Single-Colony Gameplay and UI Parity Closure**, before Portfolio/multi-colony;
- Phase 7 applies this inspect-and-mirror method to scanning, building/extraction panels, Headquarters/Colony Control, founding/player ship, Corporate Ship trade, Buyers, required Technology/Engineering, resource detail, Spaceport, warnings and remaining single-colony closure;
- the previous Portfolio/multi-colony phase moves to Phase 8;
- broader fleet procurement/expansion/navigation follows after single-colony and multi-colony management foundations are coherent;
- a later consistency phase remains, but it is a closure/audit phase rather than the first time major feature UIs are built.

Detailed implementation/recovery state is recorded in `PHASE_5_NATIVE_UI.md`, `PHASE_6_CONTRACTS_TRADE_EVENTS.md`, `PHASE_7_SINGLE_COLONY_PARITY.md` and the master `WEB_TO_ANDROID_MIGRATION.md` guide.

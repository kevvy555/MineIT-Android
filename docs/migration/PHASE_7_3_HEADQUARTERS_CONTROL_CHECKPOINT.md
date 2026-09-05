# Phase 7.3 — Headquarters and Colony Control Checkpoint

**Status:** Implementation and regression coverage complete; physical-device validation pending  
**Date:** 5 September 2026  
**Branch:** `feature/migration-phase-7`  
**Source reference:** `kevvy555/MineIT` `develop` at `075b3d82fd88334b20b3cfe7d6e2731c8d840533` / game `5.13.22`  
**Implementation checkpoint:** `f66ec924a67f974f6e77d3667137243be58f6acb`  
**Validation build:** `0.7.5-migration` / version code `17`  
**Native save format:** `7`

## Source parity audit

The maintained web `colony-control.html`, adaptive Headquarters presentation and ship-command continuity flow were reviewed before native changes. The source uses the same Colony Control concept for both colony-wide access and Headquarters-specific access. The Headquarters context adds the building identity and actions without creating a second command model.

The native implementation therefore preserves two entry contexts:

- the game header continues to open the compact colony-wide Colony Control overview;
- selecting a developed Headquarters sector opens the Headquarters-context Colony Control surface.

No later-phase service buttons were invented merely to mimic empty web destinations.

## Implemented Headquarters context

The Headquarters sheet now exposes the source hierarchy and current native domain truth:

- Headquarters artwork, level and operational status;
- Primary vs Expansion role;
- this-HQ Command Capacity;
- assigned/minimum staff state;
- requested/delivered Power and whether the HQ contributes command;
- network source: Headquarters, emergency ship command, or none;
- conglomerate link availability;
- total command capacity and command load;
- command efficiency, positive HQ bonus and overload penalty;
- outage continuity factor and recovery state;
- effective command output after continuity loss;
- founding-ship command-handover state;
- next-level command-capacity improvement;
- Build/Ore upgrade requirements and ready/blocked/max state;
- Set Primary eligibility/action for staffed Expansion HQs;
- canonical Upgrade action;
- explicit Primary-HQ demolition warning, including whether a docked command-capable ship can take emergency command.

All action state still belongs to `HeadquartersService`, `ColonyDevelopmentService`, `ColonyNetworkService` and the existing application/session owners. Compose only presents derived state and dispatches the existing actions.

## Implementation commits

- `26e411d192fc62f0947a0018813754dc2c953c52` — initial Headquarters-context Colony Control surface;
- `f15aeb4463ad7aa91f9da574e96c904d01f50263` — presentation helper stabilization after CI exposed a Kotlin helper-name collision;
- `65a13aa8fe7a40d69e9817dd0de77e6bf0326417` — route selected Headquarters sectors to the dedicated Colony Control context;
- `f66ec924a67f974f6e77d3667137243be58f6acb` — Headquarters-specific presentation regression coverage;
- `54e5ab84c14b52a246b1fcb61551f24dd7e18cf4` — cut the `0.7.5-migration` physical-device build.

## CI evidence

- CI run `33950236650` / run `314` passed the wired Headquarters surface, unit/regression tests, debug APK assembly, persistent development signer verification and artifact upload.
- CI run `33950365956` / run `315` passed the Headquarters-specific presentation regression suite plus the full Android pipeline.
- A fresh full pipeline is required for the final `0.7.5-migration` build/docs head before the APK is treated as the physical-device checkpoint.

## Regression coverage

`HeadquartersControlPresentationTest` now pins:

- Primary Headquarters information ordering and role;
- Headquarters network source and conglomerate-link state;
- command efficiency and effective output display;
- founding command-handover ready/blocked state;
- upgrade improvement and Build/Ore requirement visibility;
- staffed Expansion HQ eligibility for Set Primary;
- Primary outage visibility while emergency ship command is active;
- L5 maximum-level state without fabricated requirements.

Existing domain tests continue to own the underlying A08a/A08b semantics, including staffing, first-departure gate, command weights/capacity, positive bonus, overload penalty, ship-command fallback, daily outage degradation and ten-day recovery.

## Physical-device validation checklist

Before Phase 7 acceptance, validate at least the following on the target phone:

1. Tap a developed Headquarters sector and confirm Headquarters Colony Control opens instead of the generic building dialog.
2. Confirm artwork, level, operational status and Primary/Expansion badge are readable without confusing them with colony-wide values.
3. Confirm Command Capacity, Staff, Power and Role match the selected Headquarters.
4. Confirm total command capacity/load, efficiency, bonus and overload penalty react correctly as infrastructure changes.
5. Build a staffed Expansion Headquarters and confirm `SET PRIMARY` becomes available only when canonical eligibility is met.
6. Set the Expansion HQ Primary and confirm the sheet immediately reflects the new role and network state.
7. Verify upgrade ready/blocked state, costs and level improvement; verify L5 presents MAX LEVEL.
8. Make the Primary HQ unpowered/offline and confirm the conglomerate link becomes unavailable while an eligible docked command ship provides emergency command only.
9. Advance through an outage/recovery sequence and confirm continuity/effective-output and recovery-days presentation tracks domain state.
10. Before first ship departure, confirm handover shows BLOCKED until a Primary HQ is constructed/staffed and READY once the existing gate is satisfied.
11. Open Primary-HQ demolition confirmation with a command-capable ship docked and confirm the fallback consequence is explicit.
12. Repeat without a command-capable fallback where practical and confirm the zero-command consequence is explicit.
13. Dismiss/Back from Headquarters Control and confirm normal map selection/navigation is restored.
14. Reopen the header-level Colony Control and confirm the compact colony-wide overview remains distinct from the selected-HQ context.

## Remaining Phase 7.3 work

No additional gameplay implementation is currently known for this slice. Physical-device findings may still require refinement. Phase 7 remains unaccepted until the broader single-colony acceptance run is completed.

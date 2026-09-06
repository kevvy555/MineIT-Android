# MineIT Android Migration — Intentional Divergences

This log records deliberate differences between the web behavioural reference and the native Android implementation.

An entry is required when Android intentionally changes gameplay semantics, save semantics, or player-visible behaviour that parity tests would otherwise treat as a regression. Pure implementation changes that preserve behaviour do not need an entry.

## Entry format

```text
Date: YYYY-MM-DD
Area: <feature/system>
Source baseline: <MineIT commit + game/save version>
Web behaviour: <what the reference does>
Android behaviour: <what native intentionally does instead>
Reason: <bug fix / approved design change / platform-specific semantic improvement>
Approval/reference: <issue, backlog item, user decision, or clear-defect rationale>
Tests: <parity/regression tests proving the chosen behaviour>
Migration impact: <save/import/UI/domain impact, or none>
```

## Rules

- Do not add an entry merely because Kotlin represents the same behaviour differently.
- Do not use this log to bypass discovery for material gameplay/design changes.
- Clear defects may be corrected during migration, but must receive regression coverage.
- Where practical while the web version remains maintained, correct a clear shared gameplay defect in the web canonical owner first and then port the corrected behaviour.
- Every active divergence must remain explainable at production cutover.

## Entries

### 2026-09-06 — Corporate Ship emergency Fuel recovery

**Area:** Corporate Ship / Spaceport / Fuel recovery  
**Source baseline:** MineIT `075b3d82fd88334b20b3cfe7d6e2731c8d840533`, game `5.13.22`, web save `16`  
**Web behaviour:** Corporate Ship buy, sell and passenger/colonist services require the Basic Spaceport to receive its full 10 Power. If the Spaceport is offline, all purchases are rejected, including Fuel.  
**Android behaviour:** Normal Corporate Ship services retain the same powered-Spaceport gate, but while a Corporate Ship is already docked, **Fuel purchases only** remain available as an emergency transfer. Food, Build, Ore, selling and colonist transfer remain unavailable until Spaceport Power is restored. Emergency Fuel still consumes normal company cash and visit import-cargo capacity and uses the normal corporate buy price.  
**Reason:** The `0.7.7-migration` physical-device playthrough exposed a Fuel → Power → Spaceport → Fuel deadlock: a colony could run short of Fuel, lose Spaceport Power, receive the already-scheduled Corporate Ship, then be unable to buy the Fuel required to recover Power. That made a recoverable colony effectively unrecoverable and blocked continued migration validation.  
**Approval/reference:** Kev approved the focused Phase 7 stabilization pass after the 6 September 2026 `0.7.7-migration` manual test.  
**Tests:** `CorporateTradeServiceTest.buyKeepsNormalSpaceportGateButAllowsEmergencyFuelRecovery`; `CorporateTradePresentationTest.offline Spaceport leaves only Fuel buying available for recovery`.  
**Migration impact:** No save-format change. Deliberate trade-service and Corporate Ship UI semantic difference only.

### 2026-09-06 — Explicit founding command handover before travel migration

**Area:** Founding ship / Primary Headquarters command handover  
**Source baseline:** MineIT `075b3d82fd88334b20b3cfe7d6e2731c8d840533`, game `5.13.22`, web save `16`  
**Web behaviour:** The first successful player-ship launch completes `commandHandoverComplete` after the Primary Headquarters is fully constructed and staffed. There is no separate manual handover action because the launch workflow supplies the transition.  
**Android behaviour:** During Phase 7, once the same canonical Headquarters departure gate is satisfied, the selected Primary Headquarters exposes an explicit **COMPLETE COMMAND HANDOVER** action while the founding ship remains docked. The existing persisted `commandHandoverComplete` state is set immediately. Full launch, route selection and travel remain deferred to the later fleet/travel phase.  
**Reason:** The web trigger cannot currently be reached natively because interstellar launch/travel was deliberately deferred. Leaving command handover permanently at READY made the HQ/network lifecycle impossible to exercise in the otherwise-complete single-colony game.  
**Approval/reference:** Kev approved the focused Phase 7 stabilization pass after the 6 September 2026 `0.7.7-migration` manual test.  
**Tests:** `Phase4ColonyDomainTest.first founding ship departure requires staffed Headquarters but not Headquarters Power`; `Phase7StabilizationTest.primary Headquarters action completes founding handover before travel migration`.  
**Migration impact:** No save-format change; uses the existing native-v7 handover field. When full launch migration arrives, the explicit action can either remain as a preparatory handover or be reconciled back into launch after device review.

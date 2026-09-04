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

No intentional gameplay divergences recorded yet.

# Phase 7.2 — Building and Extraction Information Checkpoint

**Status:** Implementation and automated regression complete; physical-device validation pending  
**Android branch:** `feature/migration-phase-7`  
**Validation build:** `0.7.4-migration` / version code `16`  
**Native save format:** `7`  
**Web behavioural reference:** `kevvy555/MineIT/develop` at the Phase 7 source baseline  

## Scope completed

Phase 7.2 brings the normal building/extraction detail workflow up to the maintained web `adaptive-building` hierarchy without creating a second gameplay owner in Compose.

The native detail surface now exposes, where applicable:

- building/site artwork, identity, family, level and operational status;
- current contribution/output and useful colony-wide capacity context;
- staffing requirement and delivered workforce factor;
- Power requested/delivered/factor and priority context;
- Industry and command constraints;
- resource identity, quality, stock, reserve/remaining and estimated life;
- renewable condition and harvest intensity;
- next-level improvement;
- Build/Ore/Technology/Power/Industry/workforce requirements;
- explicit ready/blocked/max-level upgrade state;
- upgrade action;
- demolition confirmation and known-resource-under-building behaviour.

Headquarters remains intentionally routed through Colony Control and is completed in Phase 7.3 rather than creating a competing generic HQ detail surface.

## Shared gameplay ownership

`SiteProductionRules` is the canonical extraction-throughput owner used by both simulation and the detail presentation. The panel does not reproduce extraction arithmetic.

`SiteOperationRules` is the canonical extraction staffing/Industry-requirement owner. This prevents the detail surface, Power/workforce allocation and daily simulation from drifting apart.

Renewable harvest changes are committed through `ExtractionOperationService` and persist through `GameSession`.

## Renewable harvesting

Renewable sites expose 25% harvest-intensity steps across the canonical 25%–200% range.

- below 100% trades output for renewable recovery;
- 100% is the sustainable baseline;
- above 100% increases output and accelerates condition degradation;
- the same persisted intensity drives the daily renewable resource rules and the displayed production projection.

Implementation checkpoint:

- `bedc4db25cc83750b542726879f4417fadbfde01` — renewable harvest controls and regression coverage.
- Android CI run `33947780050` / run `301` passed tests, APK assembly, signer verification and artifact upload.

## Industrial extraction overdrive

Finite industrial Quarry/Mine/Deep Mine/Rig sites now carry the maintained web operating-mode mechanic. Renewable and Food sites do not expose industrial overdrive.

Modes are source-compatible:

| Mode | Output | Workforce | Exposure/day | Presented risk |
| --- | ---: | ---: | ---: | --- |
| Normal | 100% | 100% | -1.0 | None |
| Pushed | 115% | 125% | +0.3 | Low |
| Hard | 130% | 150% | +1.0 | High |

Every 30 exposure points performs the canonical 25% accident check. An accident:

- records the source-compatible family-specific incident;
- can be machinery-only or fatal;
- applies fatalities only to planetary residents because residents aboard the N05 ship are not extraction workers;
- immediately returns the site to Normal;
- resets exposure;
- closes production for exactly three subsequent simulation days;
- persists the last incident, risk-check count and shutdown state;
- writes the incident to the Game Log and surfaces immediate status feedback.

The deterministic accident roll follows the maintained web seed/sector/date/salt rule when no test RNG is injected.

Native save format `6 -> 7` is an additive/default migration so existing sites load at Normal, zero exposure and no shutdown.

Relevant implementation checkpoints:

- `68fa16d5f84e51c58fe3a5bfcc6c5fdf52aa06d0` — persisted overdrive domain/save foundation;
- `335cd6e7b3bc6ad6c14a3a8ba1e0c63e7a97c3e6` — daily exposure, accidents, fatalities and shutdown recovery;
- `32f8ecd5006ddbd91345bdfe46f4f4819f8b182b` — persisted player mode action and accident feedback;
- `6fe638464cee08172b0f9843978c1f086b78497a` — adaptive-building overdrive presentation;
- `fd9bd3d74d38bbaaf398b918a39f2f64d7d84554` — application action wiring;
- `5bc4cd98f5c0bb754945772ac0c142c4e53cedc6` — presentation/domain profile-name adapter fix.

Android CI run `33949090515` / run `309` passed unit/regression tests, debug APK assembly, persistent development signer verification and artifact upload for the complete Phase 7.2 implementation before the validation-build metadata bump.

## Physical-device validation still required

Use `0.7.4-migration` for the Phase 7.2 device pass.

1. Open Housing, Power, Industry and extraction developments and confirm the hierarchy is readable and recognisably follows the web adaptive-building panel.
2. Confirm Power/workforce/Industry/command reasons update when colony constraints change.
3. On a renewable extraction site, use -25%/+25%; confirm production changes immediately and the selected intensity survives save/reload.
4. On an eligible finite industrial site, switch Normal -> Pushed -> Hard and verify the displayed output/workforce multipliers and selected mode update immediately.
5. Save/reload while an industrial site is Pushed or Hard and confirm mode/exposure persist.
6. Exercise enough Hard/Pushed operation to confirm exposure advances only on productive days and Normal operation reduces exposure.
7. If an accident occurs, confirm the site immediately returns to Normal, production closes, incident details are visible, and Game Log/status feedback is understandable.
8. Confirm accident closure lasts three full subsequent days and the site automatically becomes operational afterwards.
9. Save/reload during an accident shutdown and verify remaining closure days and last-incident details persist.
10. Confirm demolition warnings/actions and upgrades remain usable with touch targets on the target phone.

## Next slice

Proceed with **Phase 7.3 — Headquarters and Colony Control completion**. Reuse the Phase 6 compact Colony Control hierarchy and existing Headquarters/continuity domain owners. Do not add dead buttons for later-phase services.

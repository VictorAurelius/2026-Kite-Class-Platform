# GAP-353d: DPIA Documentation — Decree 13/2023/NĐ-CP Art 24-30 (PDPL Phase 2)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Decree 13/2023 mandates DPIA for orgs processing >100k PII subjects; MVP solo-dev <<100k subjects → not yet binding, but pre-MVP launch documentation = good practice)
**Domain:** Compliance / BRD / Documentation
**Found:** 2026-05-06 (Wave 23 closure follow-up)
**Affects:** `documents/00-brd/dpia.md` (NEW) + DPO designation

## Problem

Vietnamese Decree 13/2023/NĐ-CP Articles 24-30 mandate Data Protection Impact Assessment (DPIA) and Data Protection Officer (DPO) designation for organizations processing personal data of >100,000 subjects OR processing sensitive PII at scale. KiteClass MVP solo-dev is well below 100k threshold currently, but:

- DPIA documentation should exist BEFORE crossing threshold (lead-time for proper assessment)
- DPO designation is required by Art 27 (must be a real person with privacy expertise OR contracted DPO)
- Pre-launch DPIA = market-ready compliance posture for procurement / enterprise sales

Per Wave 23 plan §1 trade-off Q2 — DPIA documentation deferred from Wave 23 critical path because MVP <<100k subjects.

## Current State (verified 2026-05-06)

| Artifact | Status |
|---|---|
| `documents/00-brd/privacy-policy.md` §2 DPO field | ⚠️ placeholder (Phase 1 skeleton from GAP-182) |
| Formal DPO designation document | ❌ missing |
| DPIA per processing activity | ❌ missing |
| Risk assessment matrix | ❌ missing |
| Mitigation controls inventory | ❌ missing |
| MPS A05 registration check | ❌ missing |

## Proposed Fix

**Layer 1 — DPO designation** (`documents/00-brd/dpo-designation.md`):
- Designated DPO contact (acting solo-dev OR external counsel name)
- Scope of DPO authority (Art 27)
- Communication channels
- Independence guarantees
- Reporting line

**Layer 2 — DPIA per processing activity** (`documents/00-brd/dpia.md`):
- Processing inventory (per data category × purpose × legal basis from privacy-policy.md §4-6)
- Per-activity risk assessment:
  - Probability × Impact matrix
  - Mitigations applied (encryption / access controls / retention / consent / etc.)
  - Residual risk rating
- High-risk activities flagged for additional controls
- Annual review cadence

**Layer 3 — MPS A05 registration check** (Art 28):
- Decree 13/2023 Art 28(1): processing of >100k subjects' PII OR sensitive data → register with MPS A05
- Pre-emptive check: subscription growth threshold + auto-trigger registration when crossed
- Document registration intent + responsible party

**Layer 4 — Cross-references**:
- Update privacy-policy.md §2 DPO field with link to designation doc
- Update privacy-policy.md §13 Security with DPIA mitigation summary
- Cross-link from `meta-gap-priority.md` Compliance section
- ROADMAP entry for 100k-subject threshold trigger

## Acceptance Criteria

- [ ] `documents/00-brd/dpo-designation.md` skeleton
- [ ] `documents/00-brd/dpia.md` skeleton with processing inventory + risk matrix
- [ ] `documents/00-brd/mps-a05-registration-check.md` (procedure + threshold trigger)
- [ ] Privacy Policy §2 + §13 updated
- [ ] Quarterly DPIA review cadence documented
- [ ] Threshold-trigger automation (subscription count check → flag DPO when approaching 100k)
- [ ] Sample mitigation controls inventory (encryption / access / retention / consent)
- [ ] Cross-links in `documents/00-brd/README.md`

## Related

- Parent gap: GAP-353 (PDPL master)
- BRD doc: GAP-182 PARTIAL (privacy-policy.md DPO field placeholder)
- Sister Phase 2: GAP-353b (server consent API), GAP-353c (DSAR form)
- Decree 13/2023/NĐ-CP Art 24-30
- `business-logic-review.md` 5-attribute mandate for any business rule extracted

## Why P2 (not P1 nor P0)

- MVP solo-dev currently <<100k subjects → not yet legally binding
- Documentation-only work (no code/infra impact)
- Pre-launch shipping benefits enterprise sales motion + procurement audits
- Once subscription crosses 50k subjects (~50% of trigger), bump to P1
- Once crossing 90k subjects (~90% of trigger), bump to P0 (legal hard-deadline)

## Effort estimate

~4-6h (~0.5 day). Documentation-only. Single agent bucket OR coordinator-only PR.

## Log

- **2026-05-06:** Filed at Wave 23 closure per wave plan §7 Closure Protocol. Decree 13/2023 Art 24-30 DPIA Phase 2 — pre-launch documentation while subject count below 100k threshold. Bump to P1 when approaching 50k subscribers; P0 at 90k (legal hard-deadline).

# GAP-341: Phổ cập Giáo dục Mandatory Escalation (Luật GD 2019 Đ.13)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 K-12 review Finding 4)
**Related Docs:**
- `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` Finding 4
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-EDGE-002
- Luật Giáo dục 2019 Điều 13 (phổ cập giáo dục tiểu học + THCS)

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| 3-day vắng không phép → GVCN alert | — | ❌ missing |
| 5-day vắng không phép → Phòng GD escalation | — | ❌ missing |
| Threshold-alert engine | — | ❌ missing — no scheduler/cron polling attendance |
| Phòng GD contact directory + report template | — | ❌ missing |

```bash
grep -rl "phổ cập\|pho.cap\|absent.threshold" kiteclass/ --include="*.java"
grep -rl "AbsenceAlert\|attendance.threshold" kiteclass/ --include="*.java"
```
Result: zero matches.

## Problem

Cấp 1 + cấp 2 mandatory by Luật GD Đ.13. Schools required to track + report HS bỏ học to Phòng GD&ĐT. Without escalation:
- School violates Đ.13
- HS may drop out unobserved (intersects child protection — GAP-322)
- Phòng GD audit fails

## Evidence

- Luật Giáo dục 2019 Điều 13: "Phổ cập giáo dục tiểu học, phổ cập giáo dục trung học cơ sở" — bắt buộc
- AC-EDGE-002 P5-k12-school.md: 3-day GVCN alert / 5-day Phòng GD escalation
- Cross-cut: GAP-322 child protection (chronic absence = safety signal)

## Proposed Fix

1. **Scheduled job:** Daily 06:00 scan attendance for streaks ≥3d unexcused per HS
2. **Alert engine:** Send to GVCN + Phó CM + 2 PH (Zalo/SMS via GAP-063)
3. **Auto-escalation at 5d:** Generate Phòng GD report PDF (HS info + attendance log + parent contact attempts) + queue for HT signature
4. **Phòng GD directory:** Tenant-config table mapping `tenant.district_id → district_office_email/phone/address`
5. **Audit log:** Full chain (3d alert → 5d escalation → HT sign → Phòng GD send) immutable

## Acceptance Criteria

- [ ] Scheduler job `PhoCapEscalationJob` runs daily 06:00, configurable cron
- [ ] 3-day threshold triggers GVCN + Phó CM + PH alert via Zalo/SMS/email/push (multi-channel via GAP-063)
- [ ] 5-day threshold generates Phòng GD report PDF + queues HT approval
- [ ] PDF format includes: HS info, attendance log table, parent contact log, GVCN summary
- [ ] Phòng GD directory table seeded for tenant district at onboarding
- [ ] Audit log entries for every alert/escalation (immutable hash-chain)
- [ ] Test scenario: Mock 5-day vắng không phép → escalation fires correctly with HT in approval queue
- [ ] Documentation 3-layer per `documents/01-business/kiteclass/phocap-escalation/`
- [ ] business-logic-review.md 5-attribute (Source: Luật GD Đ.13; Compliance: Compliant; Cadence: Annual + event-driven on Luật GD amendment)

## Related

- **Depends on:** GAP-323 (period attendance schema), GAP-063 (multi-channel notification), GAP-321 (parent portal — alerts shown there)
- **Cross-cuts:** GAP-322 (child protection — chronic absence flag)
- **Wave plan:** Bucket D Stage 1

## Log

- **2026-05-04** — Filed during Wave 17 Bucket D P5 review. State-check: zero pre-existing implementation.

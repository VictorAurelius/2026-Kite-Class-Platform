# GAP-339: Complaint Escalation 4-Level (GVCN → Phó CM → HT → Phòng GD)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 review)
**Related Docs:**
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-COMM-006
- Luật Khiếu nại 2011 + Consumer Protection 2023

## Current State (verified 2026-05-04)

```bash
grep -rl "complaint\|khiếu nại\|escalation" kiteclass/ --include="*.java" | head
```
Result: zero matches.

| Piece | Status |
|-------|--------|
| Complaint ticket entity | ❌ missing |
| 4-level routing (GVCN → Phó CM → HT → Phòng GD) | ❌ missing |
| SLA per level (5d / 5d / 7d / external) | ❌ missing |
| Audit trail | ❌ missing |
| Phòng GD external export package | ❌ missing |

## Problem

PH cần kênh khiếu nại theo Consumer Protection Law 2023 + Luật Khiếu nại 2011. Without escalation:
- PH phải đến trực tiếp / gọi điện
- Không có audit trail khi tranh chấp pháp lý
- AC-COMM-006 FAIL

## Proposed Fix

1. **Entity:** `Complaint (id, parent_id, student_id, subject, description, current_level, sla_due_at, status, audit_log)`
2. **State machine:** L1 (GVCN) → L2 (Phó CM) → L3 (HT) → L4 (Phòng GD external)
3. **Auto-escalate:** When SLA missed, advance level + notify next level + notify PH
4. **L4 export:** Generate full case PDF for PH to file with Phòng GD

## Acceptance Criteria

- [ ] Complaint entity + state machine (per `design-patterns.md` §3.3)
- [ ] PH submission via parent portal (GAP-321)
- [ ] SLA enforcement: 5d/5d/7d cron escalation
- [ ] Audit trail immutable hash-chain
- [ ] L4 export PDF with full case history + evidence
- [ ] Test: PH submits L1 → ignore 5d → auto L2 → notify Phó CM
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute (Source: Luật Khiếu nại 2011 + Consumer Protection 2023; Compliance: Compliant)

## Related

- **Depends on:** GAP-321 (parent portal submission UI), GAP-322 (separate workflow for child-safety incidents)
- **Cross-cuts:** GAP-058 (role hierarchy)
- **Wave plan:** Bucket D Stage 1

## Log

- **2026-05-04** — Filed Wave 17 Bucket D. State-check: zero pre-existing.

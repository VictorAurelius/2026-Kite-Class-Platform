# GAP-332: Homework Module (Assign / Submit / On-time + Late + Missing Tracking)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend + Frontend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-OPS-009

## Current State (verified 2026-05-04)

```bash
grep -rl "Homework\|BTVN\|assignment" kiteclass/ --include="*.java" --include="*.tsx"
```
Result: zero matches.

## Problem

GV bộ môn assign BTVN, HS submit, system track on-time / late / missing, parent visibility. Currently: no module → GV phải email cho HS.

## Proposed Fix

1. **Homework entity:** assigned_at, due_at, attachments, late_penalty_pct
2. **Submission entity:** submitted_at, status auto-derived (on-time / late / missing)
3. **Parent portal integration:** PH thấy con đã/chưa nộp (GAP-321)
4. **Late penalty config** per school

## Acceptance Criteria

- [ ] Homework + Submission entities
- [ ] Status auto-derive
- [ ] Parent portal integration
- [ ] File upload via MinIO
- [ ] Test: GV assign → 42 HS see task → HS submit on/late/missing → PH visibility
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-321 (parent portal), GAP-323 (multi-subject), GAP-054
- **Wave plan:** Bucket D Stage 3

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.

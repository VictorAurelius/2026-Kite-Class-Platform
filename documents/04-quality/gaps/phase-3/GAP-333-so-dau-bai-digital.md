# GAP-333: Sổ Đầu Bài Digital (TT 32/2020 mandate)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL
**Domain:** Backend + Frontend
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 K-12 review Finding 5)
**Related Docs:**
- `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` Finding 5
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-OPS-010
- TT 32/2020/TT-BGDĐT điều lệ trường THCS-THPT

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Sổ đầu bài entity | — | ❌ missing |
| GV per-period entry form | — | ❌ missing |
| GVCN weekly signoff | — | ❌ missing |
| Phó CM monthly review | — | ❌ missing |

```bash
grep -rl "soDauBai\|class.log.book\|teaching.log" kiteclass/ --include="*.java" --include="*.tsx"
```
Result: zero matches.

## Problem

TT 32/2020/TT-BGDĐT điều lệ yêu cầu mỗi tiết phải có sổ đầu bài (nội dung dạy, HS vắng, đánh giá tiết). Without:
- MOET inspection fails
- Phó CM cannot review monthly teaching quality
- AC-OPS-010 FAIL

## Proposed Fix

1. **Entity:** `ClassLogBook (id, class_id, subject_id, period_no, date, content, attendance_summary, evaluation, recorded_by, gvcn_signed_at, vp_reviewed_at)`
2. **Auto-populate vắng** from period attendance (GAP-323)
3. **Weekly GVCN signoff:** UI for batch sign 35 tiết/week
4. **Monthly Phó CM dashboard:** review aggregated tổng kết tháng

## Acceptance Criteria

- [ ] `ClassLogBook` entity + migration shipped
- [ ] GV per-period form post-điểm danh (nội dung, đánh giá Tốt/Khá/TB/Yếu)
- [ ] GVCN batch weekly signoff UI
- [ ] Phó CM monthly dashboard with department breakdown
- [ ] Test: 35 tiết/week per class signed by GVCN ≤5 min
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute (Source: TT 32/2020; Compliance: Compliant)

## Related

- **Depends on:** GAP-323 (period attendance for vắng auto-populate), GAP-058 (role hierarchy GVCN/Phó CM)
- **Wave plan:** Bucket D Stage 1

## Log

- **2026-05-04** — Filed during Wave 17 Bucket D P5 review. State-check: zero pre-existing.

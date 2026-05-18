# GAP-327: MOET Subject Taxonomy Seed (TT 32/2018 GDPT 2018)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Data + Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-ONBOARD-006; GAP-054

## Current State (verified 2026-05-04)

```bash
ls kiteclass/kiteclass-core/src/main/resources/db/migration/ | grep -i subject
```
Result: no MOET subject seed migration.

## Problem

K-12 needs 13 môn THCS / 17 môn THPT per chương trình GDPT 2018. Manual creation per tenant infeasible. Số tiết / môn / tuần phải đúng quy định.

## Proposed Fix

1. **Seed migration:** `V<N>__moet_subject_taxonomy.sql` với 13 môn THCS + 17 môn THPT + cấp 1 môn list, mỗi entry với (subject_code, name_vi, grade_level, periods_per_week per khối)
2. **Tenant onboarding:** auto-link subjects khi cấp học chosen
3. **Editable:** admin có thể thêm môn tự chọn / CLB

## Acceptance Criteria

- [ ] Seed migration shipped
- [ ] Subject auto-linked when khối created
- [ ] Editable per tenant
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute (Source: TT 32/2018)

## Related

- **Depends on:** GAP-323 (gradebook references subjects)
- **Wave plan:** Bucket D Stage 2

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.

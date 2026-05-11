# GAP-344: School Closure 30-Year MOET-Coordinated Archive Workflow (Luật Lưu trữ 2011)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend (rare event)
**Detected:** 2026-05-04 (Wave 17 Bucket D — NEW from P5 §"NEW-5")
**Related:** P5-k12-school.md AC-EXIT-004

## Current State (verified 2026-05-04)

No school closure workflow. Tenant deletion = data loss.

## Problem

Trường giải thể (private phá sản, công lập sáp nhập). Data MUST archive 30 năm theo Luật Lưu trữ 2011 + TT 32/2020 Đ.40. Cựu HS query học bạ năm 30 sau cần đáp ứng được.

## Proposed Fix

1. **SchoolClosure workflow:** Hiệu trưởng + HĐQT decision → 6-month notice + transfer 800 HS sang 5 trường khác (MOET coordination) → close active operations → bulk-archive 30 năm
2. **Archive storage:** offload to MOET storage or third-party (cold tier S3 Glacier)
3. **Read-only access** for cựu HS + admin queries

## Acceptance Criteria

- [ ] SchoolClosure workflow
- [ ] 6-month parent notification
- [ ] Bulk transfer pipeline
- [ ] 30y archive enforced
- [ ] Read-only cựu HS portal
- [ ] business-logic-review.md 5-attribute (Source: Luật Lưu trữ 2011 + TT 32/2020 Đ.40; Compliance: Compliant)

## Related

- **Depends on:** GAP-184 (extend retention to 30y), GAP-340 (transfer API)
- **Wave plan:** Bucket D Stage 5 (rare event — low priority but legal)

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.

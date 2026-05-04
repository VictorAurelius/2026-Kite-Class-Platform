# GAP-325: Parent-Student Auto-Link Bulk Import (sibling dedup, dual-parent)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-ONBOARD-002; GAP-051

## Current State (verified 2026-05-04)

```bash
grep -rl "ParentStudentRelationship\|sibling.dedup" kiteclass/ --include="*.java"
ls kiteclass/kiteclass-core/src/main/resources/db/migration/ | grep -i parent
```
Result: zero. GAP-051 generic xlsx, no parent-link columns.

## Problem

K-12 onboarding 800 HS + ~1500 PH (1.5/HS dedup siblings). Without auto-link:
- Manual relationship creation = days of work
- Duplicate parent when sibling cùng trường
- AC-ONBOARD-002 FAIL (4h SLA)

## Proposed Fix

1. **Xlsx schema:** Cột `Mã HS, Tên, DOB, Lớp, Tên Cha, SĐT Cha, Email Cha, Tên Mẹ, SĐT Mẹ, Email Mẹ`
2. **Sibling dedup:** Match on (SĐT, Email) — if exists, link new HS to existing parent
3. **Auto-create parent accounts** with Zalo OTP credentials
4. **Dispatch credentials:** SMS/Zalo cho parents, email/in giấy cho students <16 (Decree 13/2023 Art 16)

## Acceptance Criteria

- [ ] Xlsx template with parent columns + downloadable from admin UI
- [ ] Import 800 HS + auto-create ~1500 dedup parents in ≤4h
- [ ] Sibling dedup verified (HS A + HS B same parent → 1 parent account 2 children)
- [ ] Credentials dispatched (Zalo confirmed via OA sandbox)
- [ ] Test: real xlsx of 50 HS with 30 sibling-pair scenarios → 70 parents not 100
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute (Source: PDPL Decree 13/2023 Art 16 children data)

## Related

- **Depends on:** GAP-051 (xlsx import infra), GAP-321 (parent role exists)
- **Blocks:** GAP-321 (PH cannot login until accounts created)
- **Wave plan:** Bucket D Stage 3

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.

# GAP-1454: KC-12 G2 recipe refresh — seed/credential stale, GAP-1041 đã FIXED, owner-403 assumption sai

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Docs
**Found:** 2026-06-16 (Phase-2 browser walk flow KC-12)
**Affects:** KC-12 — `documents/05-guides/operations/2026-06-06-g2-recipe-kc12-reschedule-payroll.md` §2/§3 + Bước 4/5 + §5

## Problem
Discovered Phase-2 browser walk KC-12. Recipe drift nhiều mục:
- §2/§3: seed/credentials stale — recipe dùng tenant `aaaabbbb` / `owner.test@test.vn` / classes 4,5; seed thực tế = tenant `271dc912` (sky-education-171900) / `owner+171900@skyedu.vn` / classes 31,32 (SCHEDULED, owner là teacher). Human theo recipe verbatim không reproduce được.
- Bước 5 + §5: KNOWN-ISSUE GAP-1041 (payroll gateway 404) stale — GAP-1041 DONE 2026-06-06, gateway `application.yml:608-611` thêm route `kiteclass-payroll` trước catch-all; live `GET :9000/api/v1/admin/payroll/periods` → 200.
- Bước 4: assumption OWNER → 403 invalid — owner là teacher của chính classes 31/32 nên `@authz.hasAccessToClass` grant → 200. Muốn test 403 non-teacher cần class thuộc teacher khác trong tenant.

## Proposed Fix
- Cập nhật §2/§3 setup sang seed hiện tại: `owner+171900@skyedu.vn` / `SkyEdu@2026`, subdomain `sky-education-171900`, classes 31/32.
- Bước 5/§5: flip/bỏ GAP-1041 known-issue, assert 200 (không 404).
- Bước 4: dùng class của teacher khác để test 403, hoặc note đây là seed-dependent.

## Acceptance Criteria
- [ ] §2/§3 trỏ tenant/credential/classes tồn tại, reproduce được reschedule + payroll
- [ ] Bước 5 assert payroll 200 (bỏ GAP-1041 known-issue)
- [ ] Bước 4 sad-path 403 dùng class non-teacher hoặc note seed-dependent

## Related
- Discovered in: Phase-2 browser walk (flow KC-12), 2026-06-16
- Payroll gateway đã FIXED: GAP-1041 (DONE); reschedule past-date còn OPEN: GAP-1043

# GAP-1035: BrandingController thiếu @PreAuthorize → non-OWNER mutate tenant branding (A01)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core)
**Found:** 2026-06-06 (KC-10 G1 walk, FM-7)
**Affects:** `kiteclass-core .../module/settings/controller/BrandingController.java` (`/api/v1/settings/branding`)

## Problem

KC-10 G1 walk: `BrandingController` có **ZERO `@PreAuthorize`** trên `PUT /api/v1/settings/branding` + `POST /logo` + `POST /favicon`. Bất kỳ authenticated user nào (STAFF/TEACHER — không chỉ OWNER/ADMIN) đều sửa được branding của tenant + upload logo/favicon. OWASP A01 Broken Access Control.

**Walk evidence (runtime CONFIRMED):**
```
PUT :9000/api/v1/settings/branding  (STAFF token, tenant aaaabbbb-...-0001)
  {"displayName":"HACKED-BY-STAFF","primaryColor":"#000000",...}
→ HTTP 200 {"success":true,"data":{"id":2,"displayName":"HACKED-BY-STAFF",...}}
```

STAFF (non-owner) ghi đè branding thành công → tenant branding defacement risk. Contrast: `BrandingVersionController` CÓ `@PreAuthorize("hasAnyRole('ADMIN','OWNER')")` (đúng); `BrandingController` quên.

**Lưu ý:** hiện live qua gateway (`/api/v1/settings/branding` KHÔNG bị GAP-1034 routing shadow) — đây là lỗ hổng thực, không latent.

## Root Cause

`BrandingController` không annotate `@PreAuthorize`; SecurityConfig kiteclass-core authenticate-only cho path này (không role-gate). Sister `BrandingVersionController` có gate → inconsistency.

## Proposed Fix

Thêm `@PreAuthorize("hasAnyRole('ADMIN','OWNER')")` trên `updateBranding` (PUT), `uploadLogo`, `uploadFavicon`. GET có thể giữ authenticated-only (đọc branding settings OK cho mọi tenant member).

## Acceptance Criteria

- [ ] STAFF/TEACHER `PUT /api/v1/settings/branding` → 403
- [ ] STAFF/TEACHER `POST /logo` + `/favicon` → 403
- [ ] OWNER/ADMIN PUT/upload → 200 (no regression)
- [ ] Test: integration test cho 403 path

## Related

- Discovered in: KC-10 G1 walk (Wave flow-kc10), pre-walk FM-7
- Sister authz-gap class: GAP-999 (KC-6 grade authz A01), GAP-1005 (KC-7 InvoiceController authz). Batch Wave security-1.
- Consistent-fix reference: `BrandingVersionController` đã có `@PreAuthorize` đúng

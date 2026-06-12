# GAP-1239: slug-availability không exempt own subdomain — owner re-brand bị ép đổi slug

**Status:** 🟡 PARTIAL (fix shipped wave/branding-100-g1-fixes; chờ re-walk G1 verify)
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-12 (G1 browser walk wave branding-100 — Bug #2)
**Affects:** kitehub-branding `SlugAvailabilityService` + `BrandingWizardController` + wizard re-brand UX toàn chuỗi

## Problem

G1 browser walk (owner@skyedu.vn re-brand tenant Sky hiện hữu): bước 1 wizard nhập slug
`sky-education` (chính subdomain của mình) → `wizard-slug-conflict` "đã có người dùng" +
suggestions (`sky-education-2`...). Owner re-brand KHÔNG THỂ giữ slug mình đang có vì
`SlugAvailabilityService.isTaken` query `instances.subdomain` toàn cục (GAP-1111) không
exempt own instance.

**Chain hệ quả (walk evidence):** workaround chọn `sky-education-2` → `branding_jobs` mang
slug sai → `BrandingDeployedPublisher` publish `frontendUrl=http://localhost:3000/?tenant=sky-education-2`
+ MinIO asset path `instances/sky-education-2/...` → DoneStep link trỏ tenant không tồn tại.

## Proposed Fix (shipped)

`SlugAvailabilityService.check(slug, ownInstanceId)` — exempt khi `instances.id = X-Tenant-Id`
(gateway-trusted header) khớp row subdomain. Controller đọc header optional, anonymous flow
giữ nguyên semantics cũ.

## Acceptance Criteria

- [x] Own subdomain → `available: true` cho owner đã login (unit/controller tests PASS)
- [x] Slug của tenant KHÁC → vẫn `available: false`
- [ ] Re-walk G1: owner Sky nhập `sky-education` → không conflict → frontendUrl/asset path đúng slug

## Related

- Discovered in: G1 walk wave branding-100 2026-06-12 (PR wave/branding-100-g1-fixes)
- GAP-1111 (slug source-of-truth instances.subdomain — nền của check này)
- GAP-1108 (DoneStep landing link — consumer của frontendUrl đúng slug)

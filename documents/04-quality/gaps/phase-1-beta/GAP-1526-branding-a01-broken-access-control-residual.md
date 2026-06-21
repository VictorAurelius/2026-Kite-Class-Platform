# GAP-1526: kitehub-branding financial/admin controllers missing method-level @PreAuthorize + tenant ownership (OWASP A01 residual)

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-22 (security wave continuing GAP-1491 — kitehub-branding not covered bởi cluster trước)
**Affects:** `kitehub-branding` — `AssetStorageController`, `BrandingJobV1Controller`, `PreviewController`, `QualityScoreController`, `ContentGenerationController`, `TemplateGalleryController`, `LifecycleEventsController`

## Problem

GAP-1491 đã đóng cluster financial+admin controllers OWASP A01 (Broken Access Control) trên kiteclass-core + kitehub-subscription/platform, NHƯNG bỏ sót `kitehub-branding`. `SecurityConfig` của kitehub-branding dùng `anyRequest().authenticated()` (không phải `permitAll()` như cluster trước), nên unguarded endpoint vẫn yêu cầu authenticated — nhưng KHÔNG có per-resource authorization: bất kỳ authenticated user (kể cả STUDENT/TEACHER) đều gọi được các write endpoint paid/owner-tier, và đọc/ghi cross-tenant qua jobId/instanceId (IDOR — OWASP A01).

Các site cụ thể (trước fix):
- `AssetStorageController` — upload (POST) / list (GET) / delete (DELETE) asset: KHÔNG có `@PreAuthorize`, KHÔNG bind instanceId vào tenant → bất kỳ ai upload/đọc/xoá asset của tenant khác.
- `BrandingJobV1Controller.approve` + `getJob` — có role gate (READ/WRITE) nhưng `findById` không bind job.instanceId vào tenant → IDOR (OWNER approve/đọc job của tenant khác qua jobId).
- `PreviewController.getPreview` / `QualityScoreController.getQualityScore` — KHÔNG `@PreAuthorize`, không bind tenant → đọc preview/quality-score của job tenant khác.
- `ContentGenerationController.generate` — KHÔNG `@PreAuthorize` → bất kỳ ai trigger AI content-gen (paid action).
- `TemplateGalleryController.applyTemplate` — KHÔNG `@PreAuthorize`, không bind X-Instance-Id → apply template vào instance tenant khác.
- `LifecycleEventsController.deploy-status` + `lifecycle/events` — KHÔNG `@PreAuthorize`, không bind instanceId → đọc deploy/lifecycle state tenant khác.

Kèm theo P2 SVG-XSS (GAP-1490): `AssetStorageController` persist `file.getContentType()` (client-reported) vào asset metadata → nếu allowlist upload chấp nhận SVG / không content-sniff thì cùng class stored-SVG-XSS.

## Proposed Fix

1. Thêm method-level `@PreAuthorize` (OWNER write-tier / OWNER+STAFF read-tier) cho mọi endpoint, mirror role conventions của `BrandingJobController` (`hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')` write; `+MANAGER/TEACHER/ACCOUNTANT/STAFF` read) qua gateway X-User-Roles→ROLE_* bridge.
2. Thêm `TenantOwnershipGuard.requireInstanceOwnership(instanceId|job.getInstanceId(), X-Tenant-Id)` để bind resource vào gateway-trusted tenant (platform-admin bypass) — chống IDOR cross-tenant.
3. P2 SVG-XSS (GAP-1490): tại upload path force safe Content-Type — reject SVG outright + magic-byte sniff (PNG/JPEG/GIF/WebP) + reject markup-shaped payload spoof image type; persist sniffed type, không persist client-reported type.
4. Web-slice AuthzTest cho mỗi endpoint (allow right-role / deny STUDENT-TEACHER / IDOR cross-tenant 403).

## Acceptance Criteria

- [x] `@PreAuthorize` trên mọi unguarded endpoint của 6 controller (write OWNER-tier, read OWNER+STAFF-tier)
- [x] `TenantOwnershipGuard` ownership check sau findById / trên path-instanceId / trên X-Instance-Id (IDOR fix)
- [x] P2 SVG-XSS (GAP-1490): upload reject SVG + magic-byte sniff + force safe content-type
- [x] AuthzTest web-slice cho mỗi controller (allow + deny role + IDOR cross-tenant) — 30 tests PASS
- [x] strict-warnings compile clean; existing 52 controller-tests PASS sau khi cập nhật call-site
- [ ] CI green (PR pending)
- [ ] G2 runtime walk (deferred per `feature-ship-runtime-walk-mandate.md` — code+test DONE, human walk pending wave kế)

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

Runtime G2 walk DEFERRED — fix là method-level authz + IDOR guard, verified bằng web-slice `*AuthzTest` (30 tests, enforcing `rbac-test` profile → real `@PreAuthorize` + `TenantOwnershipGuard` chain). Walk trên local Docker stack queued cho wave kế.

`FEATURE_SHIP_WALK_DEFER: GAP-1526 — method-authz + IDOR fix verified via enforcing web-slice AuthzTest (rbac-test profile); runtime UI walk queued next wave`

## Related

- Parent: GAP-1491 (financial+admin controllers @PreAuthorize A01 cluster — kiteclass-core/subscription/platform)
- Closes (P2 serve fix): GAP-1490 (kitehub AssetStorageController serves asset with client content-type — SVG-XSS class)
- Sibling guard: `kitehub/kitehub-branding/.../security/TenantOwnershipGuard.java` (GAP-1019)
- Discovered in: security wave `fix/branding-a01-authz-2026-06-22`

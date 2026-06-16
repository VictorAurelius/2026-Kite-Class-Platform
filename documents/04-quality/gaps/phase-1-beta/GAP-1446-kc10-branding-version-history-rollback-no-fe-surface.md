# GAP-1446: Version-history + rollback branding KC không có FE surface (BE-only) — flow KC-10 quảng cáo nhưng FE chỉ name/colors/logo

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KC-10)
**Affects:** KC-10 branding — `kiteclass-frontend/src/components/settings/branding-settings.tsx` + `hooks/use-branding.ts` + `lib/api/branding.ts` ↔ BE `BrandingVersionController` `/api/v1/branding/{instanceId}/versions[/rollback]`

## Problem
Discovered Phase-2 browser walk KC-10. FE branding chỉ có name/colors/logo, không import versions/rollback. BE `BrandingVersionController` (`/api/v1/branding/{instanceId}/versions` + `/rollback`) hoạt động qua gateway 200 (gateway routing đã FIXED per GAP-1034) nhưng không có affordance FE.

## Proposed Fix
Thêm hook `useBrandingVersions`/`useRollback` + `brandingApi.listVersions`/`rollback` gọi `GET/POST /api/v1/branding/{instanceId}/versions[/{n}/rollback]`, thêm card/tab "Lịch sử phiên bản" trong `BrandingSettings` hiển thị danh sách version + nút Rollback (confirm dialog). `instanceId` lấy từ `auth-store` tenantId.

## Acceptance Criteria
- [x] FE hiển thị danh sách version branding (code implemented, pending re-walk)
- [x] Nút Rollback (có confirm) gọi POST rollback → version restore + UI cập nhật (code implemented, pending re-walk)

## Fix (PARTIAL — code implemented, pending G2 re-walk)
Branch `fix/phase3-bucketD2-kc-branding`. FE surface thêm mới (KiteClass `:3000`):
- `kiteclass-frontend/src/types/branding.ts` — thêm `BrandingVersion` + `BrandingVersionPage` (Spring Page raw shape, KHÔNG ApiResponse-wrapped — khớp `BrandingVersionController` trả raw `Page<BrandingVersion>`).
- `kiteclass-frontend/src/lib/api/branding.ts` — `brandingApi.listVersions(instanceId, page, size)` + `brandingApi.rollback(instanceId, versionNumber)` gọi `GET/POST /api/v1/branding/{instanceId}/versions[/{n}/rollback]` (VERSION_BASE_URL riêng, không phải `/settings/branding`).
- `kiteclass-frontend/src/hooks/use-branding.ts` — `useBrandingVersions(instanceId)` (query `enabled: !!instanceId`) + `useRollbackBranding(instanceId)` (invalidate branding + versions, toast VN).
- `kiteclass-frontend/src/components/settings/branding-version-history.tsx` (mới) — card "Lịch sử phiên bản": list version (số + active badge + rollback-from badge + thời gian VN), nút "Khôi phục" gated bằng `ConfirmDialog`, disabled cho version đang dùng. `instanceId` lấy từ `auth-store.tenantId`.
- `kiteclass-frontend/src/components/settings/branding-settings.tsx` — render `<BrandingVersionHistory />` (lazy `next/dynamic` ssr:false, bundle budget) cuối form.

Tests: `src/lib/api/__tests__/branding-versions.test.ts` (3 PASS — endpoint URL + raw-body + paging). Build `pnpm --filter kiteclass-frontend build` + targeted vitest verified.

Pending: G2 browser re-walk thật trên stack production-equivalent (login owner KC → settings branding → list version → rollback → UI cập nhật) trước khi flip DONE.

## Related
- Discovered in: Phase-2 browser walk (flow KC-10), 2026-06-16
- Gateway routing đã FIXED: GAP-1034
- Fixed in: `fix/phase3-bucketD2-kc-branding` (Phase-3 Bucket D2)

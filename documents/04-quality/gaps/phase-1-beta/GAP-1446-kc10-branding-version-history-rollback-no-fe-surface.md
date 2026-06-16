# GAP-1446: Version-history + rollback branding KC không có FE surface (BE-only) — flow KC-10 quảng cáo nhưng FE chỉ name/colors/logo

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KC-10)
**Affects:** KC-10 branding — `kiteclass-frontend/src/components/settings/branding-settings.tsx` + `hooks/use-branding.ts` + `lib/api/branding.ts` ↔ BE `BrandingVersionController` `/api/v1/branding/{instanceId}/versions[/rollback]`

## Problem
Discovered Phase-2 browser walk KC-10. FE branding chỉ có name/colors/logo, không import versions/rollback. BE `BrandingVersionController` (`/api/v1/branding/{instanceId}/versions` + `/rollback`) hoạt động qua gateway 200 (gateway routing đã FIXED per GAP-1034) nhưng không có affordance FE.

## Proposed Fix
Thêm hook `useBrandingVersions`/`useRollback` + `brandingApi.listVersions`/`rollback` gọi `GET/POST /api/v1/branding/{instanceId}/versions[/{n}/rollback]`, thêm card/tab "Lịch sử phiên bản" trong `BrandingSettings` hiển thị danh sách version + nút Rollback (confirm dialog). `instanceId` lấy từ `auth-store` tenantId.

## Acceptance Criteria
- [ ] FE hiển thị danh sách version branding
- [ ] Nút Rollback (có confirm) gọi POST rollback → version restore + UI cập nhật

## Related
- Discovered in: Phase-2 browser walk (flow KC-10), 2026-06-16
- Gateway routing đã FIXED: GAP-1034

# GAP-1213: Wizard deploy = MOCK — không propagate theme/assets sang KC-core → landing thật không bao giờ đổi

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed
**Found:** 2026-06-11 (branding-100 failure-mode audit — finding #1)
**Affects:** `kitehub-branding` MockProvisioningService + chuỗi KH-branding → kiteclass-core

## Problem

`MockProvisioningService` ghi `BrandingAsset[]` lên `BrandingJob` entity + lifecycle marker rồi PROCESSING→COMPLETED — KHÔNG có propagation cross-service (event/API) sang kiteclass-core (`BrandingResourceRepository`/`FrontendInstance`/landing_pages). Branding mới chỉ tồn tại trong preview iframe client-side. User "Deploy thành công" nhưng landing per-tenant thật giữ nguyên — toàn chuỗi giá trị của AI branding đứt ở mắt cuối. GAP-1021 chạm phần persist active theme; mảnh cross-service propagation này chưa có gap.

## Proposed Fix

Outbox event `branding.deployed` (per design-patterns §3.5) từ kitehub-branding → consumer kiteclass-core áp theme/assets vào Branding/LandingPage (+ evict cache per GAP-1203) — HOẶC nếu giữ mock Phase 1: relabel UI "mô phỏng" + disclaimer (chống mislead). Quyết định trong wave branding-100 bucket C.

## Acceptance Criteria

- [ ] Wizard deploy xong → landing per-tenant thật đổi theme/banner (browser verify)
- [ ] Hoặc (interim) UI ghi rõ mô phỏng, không toast "deploy thành công"

## Related

- Audit: `2026-06-11-branding-100-failure-mode-matrix.md` #1; sister GAP-1021/1108; wave branding-100 bucket C

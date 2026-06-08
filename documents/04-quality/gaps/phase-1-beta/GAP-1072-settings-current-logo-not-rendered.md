# GAP-1072: Settings không render logo hiện tại + presigned URL hết hạn

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend (+ Backend presigned URL TTL)
**Found:** 2026-06-08 (KC-1 G2 Bước 3)
**Affects:** `components/settings/branding-settings.tsx` logo section; MinIO presigned URL TTL

## Problem

`branding.logo_url` CÓ giá trị (sky-logo.png presigned MinIO :9100) nhưng UI Settings ghi "Logo hiện tại / Không có tệp nào được chọn" → **không render preview logo hiện tại**, chỉ hiện control upload trống. Hai vấn đề:
1. **FE:** BrandingSettings không bind/hiển thị `logo_url` hiện tại (img preview).
2. **BE/infra:** presigned URL `X-Amz-Date=20260529 + X-Amz-Expires=604800` (7 ngày) → **hết hạn ~2026-06-05**; render cũng 403. Logo URL nên regenerate khi GET branding HOẶC dùng TTL dài/proxy.

## Proposed Fix

(1) BrandingSettings render `<img src={branding.logoUrl}>` khi có logo_url + fallback "chưa có logo"; (2) BE regenerate presigned URL mỗi lần serve branding (hoặc proxy qua gateway, không trả URL hết hạn).

## Acceptance Criteria

- [ ] Settings hiển thị logo hiện tại khi logo_url có
- [ ] Logo URL không hết hạn lúc render (regenerate on read hoặc proxy)
- [ ] Re-walk browser → logo preview hiện

## Related

- Discovered in: KC-1 G2 Bước 3 walk 2026-06-08
- GAP-1071 (sister — layout shell, cùng /settings walk)

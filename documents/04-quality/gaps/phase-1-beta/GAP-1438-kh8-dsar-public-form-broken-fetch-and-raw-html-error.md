# GAP-1438: DSAR public form vỡ end-to-end — bare relative fetch 404 + catch dump raw HTML vào alert

**Status:** 🔵 OPEN
**Priority:** 🔴 P1
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-8)
**Affects:** KH-8 off-boarding/PDPL — `kitehub-frontend/src/app/(public)/legal/data-rights/DataRightsForm.tsx:84,98-101` + `next.config.*`

## Problem
Discovered Phase-2 browser walk KH-8. Form DSAR public gọi bare relative `fetch('/api/v1/dsar/request')` → 404 ở FE origin `:3001` (`next.config` chỉ có `redirects()`, KHÔNG có `rewrites()` proxy; các form khác dùng `apiClient`/`NEXT_PUBLIC_API_URL` → gateway `:9000`). Sau đó catch (`:98-101`) `throw new Error(text)` dump nguyên trang HTML 404 vào `[role=alert]` → user thấy khối HTML thay vì message thân thiện (generic-catch class, sibling GAP-926).

## Proposed Fix
1. Đổi `DataRightsForm.tsx:84` sang `apiClient.post(...)` HOẶC `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000'}/api/v1/dsar/request` — mirror `beta-status.ts:31` / `BetaSignupForm` pattern.
2. Phân biệt errorCode/status (404 routing / 401 auth / 400 validation) → message tiếng Việt cụ thể; không throw raw body khi content-type là text/html.

## Acceptance Criteria
- [ ] Submit form DSAR reach gateway `:9000` (không 404 ở FE origin)
- [ ] Lỗi hiển thị message tiếng Việt cụ thể theo status, không dump HTML thô

## Related
- Discovered in: Phase-2 browser walk (flow KH-8), 2026-06-16
- Sibling generic-catch: GAP-926; env-coverage class: GAP-802

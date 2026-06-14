# GAP-1375: KH admin dashboard null-render khi no-data → blank screen, thiếu empty state

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-14 (UI review full audit, AUDIT-2026-06-14-ui-review-full)
**Affects:** `kitehub/kitehub-frontend/src/app/(admin)/admin/page.tsx`

## Problem

Admin dashboard có 3 nhánh render: loading (skeleton ✅), error (message ✅), nhưng nhánh thứ 3 `if (!stats) return null;` (line 71-73) → render **màn hình trắng hoàn toàn** khi API trả về data rỗng/undefined mà không phải loading cũng không phải error.

Admin đăng nhập thành công nhưng nếu `stats` undefined (vd backend trả 204/empty, hoặc instance mới chưa có data) sẽ thấy trang trắng không feedback — confusing, trông như app crash.

## Root Cause

Defensive `return null` để tránh crash khi `stats` undefined, nhưng không kèm empty-state UI. Đây là anti-pattern "silent blank" thay vì "explicit empty state".

## Proposed Fix

Thay `return null` bằng empty-state UI: heading "Dashboard" + message thân thiện ("Chưa có dữ liệu thống kê" + gợi ý next action như tạo instance đầu tiên). Giữ layout shell nhất quán để không thấy trang trắng.

## Acceptance Criteria

- [ ] `stats` undefined/empty → render empty-state UI (heading + message + optional CTA), KHÔNG blank
- [ ] Phân biệt rõ 3 trạng thái: loading / error / empty / có-data
- [ ] Empty state Vietnamese narrative nhất quán

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P2)
- Source: `kitehub/kitehub-frontend/src/app/(admin)/admin/page.tsx:71-73`

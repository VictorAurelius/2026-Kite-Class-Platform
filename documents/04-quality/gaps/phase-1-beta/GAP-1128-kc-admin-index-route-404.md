# GAP-1128: KiteClass `/admin` index route thiếu page.tsx → 404

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend (kiteclass-frontend `:3000`)
**Found:** 2026-06-10 (discovered trong GAP-1122 G1 browser-walk)
**Affects:** `kiteclass-frontend/src/app/(dashboard)/admin/` — index route `/admin`

## Problem

Route group `(dashboard)/admin/` chỉ có `layout.tsx` (RoleGuard allow[OWNER,ADMIN]) + các sub-page (`admin/payroll`, `admin/bulk-import`, `admin/attendance/stats`, `admin/vetting/[vettingId]/upload`) — **không có index `page.tsx`** cho `/admin`. Mở `/admin` (không sub-path) → Next.js 404 (URL ở lại `/admin` nhưng là trang not-found).

Không phải lỗ hổng IDOR — RoleGuard layout vẫn bảo vệ các sub-page (đã verify: TEACHER/PARENT bounce khỏi `/admin/payroll`, OWNER vào được). Chỉ là thiếu admin landing → UX gap (owner gõ `/admin` gặp 404 thay vì admin home).

## Proposed Fix

Thêm `(dashboard)/admin/page.tsx` redirect tới sub-page mặc định (vd `/admin/payroll`) HOẶC render admin home với link sang các admin tool. Low-risk, single-page.

## Acceptance Criteria

- [ ] `/admin` (owner) → render admin home HOẶC redirect sub-page mặc định (không 404)
- [ ] Non-owner/admin → vẫn bounce role-home (RoleGuard layout giữ nguyên)

## Related

- Discovered in: GAP-1122 G1 browser-walk session 2026-06-10
- Route: `kiteclass-frontend/src/app/(dashboard)/admin/`

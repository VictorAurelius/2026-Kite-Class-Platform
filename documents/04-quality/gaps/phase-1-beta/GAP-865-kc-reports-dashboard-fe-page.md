---
audience: dev
---

# GAP-865 — KC reports dashboard FE page (revenue + attendance)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-02 (GAP-775 BE wave — FE deferral follow-up)
**Affects:** B11 Báo cáo — Owner dashboard (top-level `/reports`)
**Phase:** phase-1-beta

## Problem

GAP-775 đã ship backend `ReportController` (2 endpoint `GET /api/v1/reports/{revenue,attendance}` — monthly aggregation, `hasRole('ADMIN')`, tenant-scoped). NHƯNG chưa có FE page tiêu thụ. Owner (P2) cần dashboard top-level xem doanh thu + tỷ lệ điểm danh.

Catalog hiện tại: chỉ `(dashboard)/attendance/reports` (nested attendance-only) + `(teacher)/teacher/reports`. KHÔNG có standalone `(dashboard)/reports` cho Owner revenue+attendance dashboard.

## Proposed Fix

`kiteclass-frontend/src/app/(dashboard)/reports/page.tsx`:
- 2 KPI card: "Doanh thu tháng" (từ `totalRevenue`) + "Tỷ lệ điểm danh" (từ `overallPresentRate`).
- 2 chart 12 tháng (revenue series + attendance present-rate series).
- Gọi `GET /api/v1/reports/revenue?months=12` + `GET /api/v1/reports/attendance?months=12` (gắn `X-Tenant-Id` qua apiClient interceptor).
- VND format `1.500.000đ` + phần trăm `92,5%` + nhãn tiếng Việt per `vn-localization-audit-checklist.md` §1 + §2.
- Chỉ Owner/admin thấy menu item (FE role guard) — khớp BE `hasRole('ADMIN')`.

## Acceptance Criteria

- [ ] `(dashboard)/reports/page.tsx` render 2 KPI card + 2 chart 12 tháng
- [ ] VND format `1.500.000đ` + phần trăm dấu phẩy VN + nhãn tiếng Việt
- [ ] Empty-state graceful (series toàn 0 → chart hiển thị "Chưa có dữ liệu")
- [ ] FE role guard: menu/route Owner-only
- [ ] **Live RST walk end-to-end** per `feature-ship-runtime-walk-mandate.md` trên full stack: Owner login → mở /reports → 2 endpoint trả 200 + render đúng + non-admin bị chặn (deferred từ GAP-775 do isolated worktree không có stack)

## Related

- GAP-775 (BE ReportController — DONE/PARTIAL, branch `feature/GAP-775-kc-report-controller`, PR #2052)
- `documents/01-business/kiteclass/analytics-report/api-contract.md` (endpoint contract)
- `vn-localization-audit-checklist.md` §1 (VND) + §2 (VN label)

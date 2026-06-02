---
audience: dev
---

# GAP-865 — KC reports dashboard FE page (revenue + attendance)

**Status:** 🟡 PARTIAL (80% — FE code shipped; live RST walk deferred, stack down)
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

- [x] `(dashboard)/reports/page.tsx` render 2 KPI card + 2 chart 12 tháng (code shipped + reports-page.test.tsx)
- [x] VND format `1.500.000đ` + phần trăm dấu phẩy VN + nhãn tiếng Việt (code-level)
- [x] Empty-state graceful (series toàn 0 → chart hiển thị "Chưa có dữ liệu") (code-level)
- [x] FE role guard: menu/route Owner-only (code-level)
- [ ] **Live RST walk end-to-end** per `feature-ship-runtime-walk-mandate.md` trên full stack: Owner login → mở /reports → 2 endpoint trả 200 + render đúng + non-admin bị chặn (deferred — stack down per GAP-612 AWS / local stack not up)

## Current State (verified 2026-06-02)

FE code shipped this PR (branch `feature/GAP-865-kc-reports-fe-page`):
- `(dashboard)/reports/page.tsx` — 2 KPI cards + 12-month charts consuming GET /api/v1/reports/{revenue,attendance}.
- `types/report.ts` + `hooks/use-reports.ts` + `lib/api/reports.ts` + `components/reports/monthly-bar-chart.tsx`.
- Test: `(dashboard)/reports/__tests__/reports-page.test.tsx`.
- Verify: `pnpm --filter kiteclass-frontend lint` exit 0 + `next build` exit 0 + vitest 768 passed / 0 failed (3 clean runs; 1 flaky failure on first run cleared on re-run).

Live RST walk (AC #5) deferred — full stack down (`FEATURE_SHIP_WALK_DEFER: GAP-865 — stack down, walk on full stack restore`).

## Related

- GAP-775 (BE ReportController — DONE/PARTIAL, branch `feature/GAP-775-kc-report-controller`, PR #2052)
- `documents/01-business/kiteclass/analytics-report/api-contract.md` (endpoint contract)
- `vn-localization-audit-checklist.md` §1 (VND) + §2 (VN label)

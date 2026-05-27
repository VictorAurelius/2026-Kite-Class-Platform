---
audience: dev
---

# GAP-775 — KC ReportController missing (Mảng B11 blocker)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B11 catalog probe)
**Affects:** B11 Báo cáo — Doanh thu tháng + tỷ lệ điểm danh
**Phase:** phase-1-beta

## Problem

Wave 106 plan §3 B11: "Báo cáo — Doanh thu tháng + tỷ lệ điểm danh".

Catalog probe:
```bash
grep -rE "(/api/v1/reports|ReportController)" kiteclass/kiteclass-core/src/main/java
# 0 results — no ReportController exists
```

Probe gateway: `GET /api/v1/reports/revenue` → 400 (empty body — endpoint không tồn tại, generic 400 vì validation fail trước routing).

FE catalog: only `(dashboard)/attendance/reports` exists (nested attendance-only). NO standalone `/reports` route for revenue dashboard.

## Root Cause

Phase 1 BETA scope: revenue reporting + attendance reporting đều cần aggregation queries (GROUP BY month + JOIN multiple tables). Likely deferred.

## Proposed Fix

Option A — Build minimal `ReportController`:
- `GET /api/v1/reports/revenue?period=month` — `SUM(payment.amount) GROUP BY month` từ `payments` table
- `GET /api/v1/reports/attendance?period=month` — `COUNT(present) / COUNT(*)` từ `attendance_records` table
- FE `(dashboard)/reports/page.tsx` — 2 KPI cards + 12-month chart per `vn-localization-audit-checklist.md` §1 (VND format `1.500.000đ` + label tiếng Việt "Doanh thu tháng" / "Tỷ lệ điểm danh")

Option B — Defer Phase 1.5+: B11 đánh dấu out-of-scope Phase 1 BETA. Beta tenant chấp nhận xem raw data trong /billing + /attendance pages.

## Acceptance Criteria

- [ ] Decision logged: Option A vs B
- [ ] Nếu A: 2 endpoints + FE page + VND format + label tiếng Việt
- [ ] Nếu B: plan §3 B11 đánh dấu out-of-scope explicit

## Related

- Wave 106 plan §3 B11
- `vn-localization-audit-checklist.md` §1 (VND format) + §2 (VN label)
- Existing related: `(dashboard)/attendance/reports` (nested only — Owner cần top-level dashboard)

---
audience: dev
---

# GAP-775 — KC ReportController missing (Mảng B11 blocker)

**Status:** 🟡 PARTIAL
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

## Decision (2026-06-02 — GAP-775 BE wave)

**Option A chosen** — build minimal backend `ReportController`. State-check confirmed: no `ReportController` existed in kiteclass-core; infrastructure (Payment + Attendance entities/repos + Hibernate tenant filter + `ApiResponse` envelope + `@PreAuthorize` patterns) all present. Option B (defer) rejected — Owner persona (P2) needs the dashboard data for Phase 1 BETA beta-tenant experience.

New domain `analytics-report` (tenant-wide analytics aggregation) — distinct from existing `report-card` domain (per-student K-12 PDF, Phase 3).

## Acceptance Criteria

- [x] Decision logged: Option A vs B → **Option A**
- [x] 2 BE endpoints: `GET /api/v1/reports/revenue` + `GET /api/v1/reports/attendance` (monthly aggregation, zero-fill, totals)
- [x] OWASP A01 authz `@PreAuthorize("hasRole('ADMIN')")` — consistent with sibling `PayrollController` financial-admin pattern
- [x] 3-layer business docs `documents/01-business/kiteclass/analytics-report/` (rules + use-cases + api-contract) per Living Docs
- [x] Tests: `ReportServiceImplTest` (6) + `ReportControllerIT` (5, incl. authz-denied 403) — 11/11 PASS; strict-warnings compile clean
- [ ] **DEFER (FE scope)**: `(dashboard)/reports/page.tsx` 2 KPI cards + 12-month chart + VND format `1.500.000đ` + label tiếng Việt → follow-up **GAP-865** (Frontend, kiteclass-frontend — out of this Backend-scope wave)
- [ ] **DEFER (live walk)**: RST end-to-end walk per `feature-ship-runtime-walk-mandate.md` — cannot run in isolated worktree (no stack up); BE verified via Testcontainers-capable WebMvcTest + unit tests. Live walk to run when FE lands (GAP-865) on full stack.

## Related

- Wave 106 plan §3 B11
- `vn-localization-audit-checklist.md` §1 (VND format) + §2 (VN label) — FE rendering responsibility (GAP-865)
- Sibling authz pattern: `PayrollController` `hasRole('ADMIN')` (tenant-wide financial admin views)
- Existing related: `(dashboard)/attendance/reports` (nested attendance-only — Owner cần top-level revenue+attendance dashboard via GAP-865)
- Follow-up: **GAP-865** — FE reports dashboard page consuming `/api/v1/reports/{revenue,attendance}`

## Log

- **2026-06-02** — Backend ReportController shipped (Option A). State-check per `audit-to-gap-pipeline.md` §2.8 confirmed greenfield (0 ReportController, infra present). New module `com.kiteclass.core.module.report` (controller + service + 2 read-only aggregation repos + 4 DTOs + package-info). Authz `hasRole('ADMIN')` consistent with `PayrollController`. 3-layer business docs `analytics-report` created. Tests 11/11 PASS (`ReportServiceImplTest` 6 + `ReportControllerIT` 5), strict-warnings main+test compile clean. Status → 🟡 PARTIAL: BE complete; FE page deferred to GAP-865 (Frontend scope); live RST walk deferred (no stack in isolated worktree) — to run when FE lands. Cross-flow sweep: no existing FE caller of `/api/v1/reports`; authz pattern consistent with sibling `PayrollController`. Branch `feature/GAP-775-kc-report-controller`.

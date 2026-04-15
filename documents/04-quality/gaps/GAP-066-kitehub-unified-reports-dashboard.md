# GAP-066: KiteHub Unified Reports / Analytics Dashboard

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Admin / Product / Backend
**Detected:** 2026-04-14 (post-Wave-2 stakeholder review)

## Problem

KiteHub admin console hiện có 3 trang rời rạc (`/admin/instances`, `/admin/payments`, `/admin/revenue`) — **không có unified reports/analytics dashboard**. Platform operator không có single-pane view để:

- Track MRR / ARR growth theo thời gian
- Monitor churn rate + cohort retention
- Instance growth (new / active / suspended / deleted)
- Tier distribution (FREE / BASIC / PREMIUM / ENTERPRISE)
- AI usage & cost trends per tier
- Trial-to-paid conversion funnel
- Support ticket volume (khi GAP-040 land)

Khác với GAP-047 (document generation infrastructure — invoice/certificate PDF): đây là **real-time operational reports** cho platform ops.

## Evidence

- `kitehub/kitehub-frontend/src/app/(admin)/admin/` → `instances/`, `payments/`, `revenue/`, `page.tsx` (dashboard summary cards only)
- Không có `/admin/reports`, `/admin/analytics`, `/admin/metrics`
- Không có time-series charts trên admin pages
- Revenue page hiển thị current state — không có historical trending

## Proposed Fix

### Backend (kitehub-platform + kitehub-billing)

- New service: `PlatformAnalyticsService`
- Time-series queries (Postgres + materialized views for heavy aggregations)
- Metrics persisted: `platform_metrics` table
  - dimensions: `tenant_tier`, `date`, `metric_name`, `value`
  - metrics: `new_signups`, `active_instances`, `mrr`, `churn`, `ai_calls`, `ai_cost_vnd`
- Daily Spring `@Scheduled` to aggregate & persist
- Export endpoints (CSV + future PDF via GAP-047)

### Frontend (kitehub-frontend admin)

Route: `/admin/reports`

Sections:
1. **Revenue Growth** — MRR + ARR chart (90d / 12m / 5y)
2. **Instance Growth** — stacked area by status
3. **Tier Distribution** — pie + stacked bar over time
4. **Conversion Funnel** — signup → trial → paid → renewed
5. **AI Usage & Cost** — requests/day split by category + cost (VND) per tier
6. **Cohort Retention** — heatmap (month-of-signup × retention %)
7. **Top Tenants** — sortable table by MRR / AI spend / growth

Filters: date range, tier, region (future).
Export: CSV (Phase 1), PDF via GAP-047 (Phase 2).

## Acceptance Criteria

- [ ] `platform_metrics` table + daily aggregation job
- [ ] `/admin/reports` page with 7 sections above
- [ ] Each chart ≤ 500ms load (p95) với 12 months data
- [ ] CSV export for every section
- [ ] Mobile responsive (at least readable on tablet)
- [ ] 3-layer docs: `01-business/kitehub/platform-reports/`
- [ ] Unit tests: aggregation math
- [ ] E2E: reports page renders + filter works

## Dependencies

- GAP-019 (AI observability) — source of AI usage metrics
- GAP-017 (AI usage → billing) — cost calculations
- GAP-047 (doc generation) — Phase 2 PDF export

## Target Wave

**Wave 6 Ops Readiness** (Sprint 6).

## Log

- 2026-04-14 — Detected during stakeholder review before Wave 3 kickoff

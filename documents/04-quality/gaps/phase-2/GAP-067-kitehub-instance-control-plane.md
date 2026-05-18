# GAP-067: KiteHub Instance Control Plane (AWS-/Vercel-Style Ops Console)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Admin / DevOps / Backend
**Detected:** 2026-04-14 (post-Wave-2 stakeholder review)

## Problem

KiteHub admin instance management hiện **chỉ ở ~15% feature parity** với ops consoles tiêu chuẩn (AWS EC2, Vercel dashboard, Stripe admin, Render). Current state:

- List instances (basic columns)
- Detail page với 3 actions: suspend / activate / extend trial

Thiếu nghiêm trọng để vận hành SaaS ở quy mô:

| Capability | Present | Missing |
|-----------|:-------:|---------|
| Instance list | ✅ | Bulk actions, saved filters, column customization |
| Lifecycle actions | ✅ (3) | Force resync, hard restart, rollback branding version, re-seed data, cache flush |
| Log viewer per-instance | ❌ | Real-time tail, search, filter by level/tenant |
| Health metrics | ❌ | Uptime %, request rate, error rate, p50/p95/p99 latency |
| Event timeline / audit | ❌ | All admin actions + tenant activity + system events |
| Resource usage | ❌ | DB rows, storage GB, AI calls, bandwidth, seat count |
| Alerts + SLA | ❌ | Alert rules, SLA breach indicator, incident link |
| Maintenance window | ❌ | Schedule + auto-notify tenant |
| Cost analysis | ❌ | Per-instance cost (infra + AI + support time) |
| Bug-fix workflow | ❌ | Inline DB query runner, feature flag toggles, debug mode |
| Tenant impersonation | ⚠️ Partial (GAP-040) | — |
| Export support bundle | ❌ | One-click ZIP of logs + events + config snapshot |

Khác với GAP-040 (impersonation = support flow): đây là **full ops control plane** để platform ops team vận hành mọi instance hàng ngày.

## Reference best practices

- **AWS EC2 instance detail** — tabs: Description / Status Checks / Monitoring / Events / Tags / Actions menu
- **Vercel project dashboard** — Deployments / Analytics / Speed Insights / Logs / Functions / Storage / Settings
- **Stripe admin customer page** — Summary / Payments / Subscriptions / Invoices / Events / Logs
- **Render service dashboard** — Events / Logs / Metrics / Environment / Shell / Disks

## Proposed Fix (phased)

### Phase 1: Observability (4 weeks)
- Log viewer per-instance (backend: Loki + Grafana query; frontend: search + tail)
- Basic metrics (request rate, error rate, latency) from existing Prometheus
- Event timeline từ `outbox_events` (lands in Wave 3 Sub-PR 3.1) + existing audit

### Phase 2: Resource tracking (2 weeks)
- Per-instance resource-usage aggregation job
- UI: utilization cards + quota bars
- Alerts khi approach tier limit

### Phase 3: Advanced ops (3 weeks)
- Bulk actions table (checkbox + batch operations)
- Maintenance window scheduler
- Cost analysis (infra from k8s metrics + AI from GAP-019)
- Support bundle export (ZIP)

### Phase 4: Bug-fix workflow (2 weeks)
- Feature flag toggles per-instance
- Cache flush button per-key
- Force resync (tenant-safe — read-only DB inspector)
- Branding version history + rollback (ties to GAP-033)

## Acceptance Criteria

- [ ] `/admin/instances/{id}` transforms from current 3-action page → multi-tab ops console
- [ ] Tabs: Overview / Events / Logs / Metrics / Resources / Actions / Settings
- [ ] Real-time log tail (SSE)
- [ ] Health dashboard equivalent detail to Vercel/Stripe
- [ ] Audit every admin action with reason + notification to tenant (configurable)
- [ ] RBAC: only SUPPORT / ADMIN / SRE roles can see; tier-based feature gating
- [ ] 3-layer docs: `01-business/kitehub/ops-console/`
- [ ] Contract tests for each admin endpoint
- [ ] E2E: full ops workflow cho typical incident scenario

## Dependencies

- GAP-019 (observability infra) — backbone for logs/metrics
- GAP-040 (impersonation) — one of the tabs
- GAP-033 (branding version history) — rollback UX
- GAP-009 (instance lifecycle) → DONE ✅ — events surfaced here
- Wave 3 outbox (GAP-009 deferred) — event timeline data source

## Target Wave

**New "Wave 8+: Ops Control Plane"** or extend Wave 6 scope.

Given size (11 weeks total phased), likely its own dedicated wave post-GA. Phase 1 could join Wave 6 Sprint 6 as critical for launch.

## Log

- 2026-04-14 — Detected during stakeholder review; platform operators flagged as blocker for scaling past ~20 tenants

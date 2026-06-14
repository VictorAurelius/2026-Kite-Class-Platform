# GAP-1365: API P95 SLO chưa load-test + Postgres slow-query-log chưa bật (AWS-gated)

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 2.1/2.3 ❓ UNCHECKED)
**Updated:** 2026-06-15 — load-test targets now documented (slo.md); measurement + RDS slow-query-log remain AWS-gated
**Affects:** Toàn fleet API + Postgres (RDS prod)

## Problem

- Sub-check 2.1 (P95 <2s top-10 endpoint): chưa load-test prod-scale. Local health probe gateway HTTP 200 ~2.4ms nhưng KHÔNG đại diện hot-path dưới tải.
- Sub-check 2.3 (slow query log): Postgres `log_min_duration_statement` chưa bật ở prod (RDS parameter group không truy cập — GAP-612 AWS suspended).

Cả hai `❓ UNCHECKED` do AWS suspended → không thể đo prod-scale. KHÔNG default PASS.

## Proposed Fix

Sau khi AWS restored (GAP-612):
- Bật `log_min_duration_statement=1000` (1s) trên RDS parameter group.
- Chạy load test (k6/Gatling) top-10 endpoint, capture P95, set SLO.

## Acceptance Criteria

- [ ] `log_min_duration_statement` ≤1s trên prod Postgres
- [ ] P95 top-10 endpoint đo + so SLO <2s
- [ ] Kết quả cập nhật vào audit performance

## Resolution (2026-06-15) — PARTIAL (AWS-gated)

Cannot load-test or set RDS parameters without a running scaled prod env (stack stopped on-demand; RDS parameter group not reachable). What's now in place to unblock the eventual measurement:
- **SLO targets documented** — `documents/02-architecture/slo.md` §1 defines the per-endpoint-class p95 budgets (auth/read <200ms, list <500ms, write <800ms, heavy-gen <5s) so the load test has concrete pass/fail targets (was previously undefined — half the reason 2.1 was UNCHECKED).
- **Plan (post AWS-restore):** (1) set `log_min_duration_statement=1000` on the RDS parameter group; (2) run k6/Gatling against top-10 endpoints, capture p95, compare to slo.md budgets; (3) update the performance audit.

Remaining work is AWS-gated (live measurement + RDS param) — kept PARTIAL, not closed.

## Related

- Discovered in: 2026-06-14 performance audit (F-009)
- Blocked by: GAP-612 (AWS restore — note: GAP-612 now DONE per gap-status.csv, but live stack is stopped on-demand; load-test still needs a scaled env spun up)
- SLO budgets to test against: `documents/02-architecture/slo.md` (GAP-1366)

# GAP-1365: API P95 SLO chưa load-test + Postgres slow-query-log chưa bật (AWS-gated)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 2.1/2.3 ❓ UNCHECKED)
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

## Related

- Discovered in: 2026-06-14 performance audit (F-009)
- Blocked by: GAP-612 (AWS suspended)

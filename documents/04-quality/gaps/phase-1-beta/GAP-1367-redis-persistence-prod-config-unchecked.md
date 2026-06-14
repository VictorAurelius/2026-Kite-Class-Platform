# GAP-1367: Redis persistence (RDB/AOF) prod config UNCHECKED (AWS-gated)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 4.6 ❓ UNCHECKED)
**Affects:** Redis prod (kite-redis / ElastiCache)

## Problem

Sub-check 4.6 (Redis persistence RDB hoặc AOF configured) `❓ UNCHECKED` — prod Redis config không verify được (AWS suspended; local docker dùng default). Nếu prod Redis không bật persistence, restart → mất toàn bộ session/rate-limit/cache state → thundering herd khi cache cold + user bị logout.

KHÔNG default PASS per rubric §4.5.

## Proposed Fix

Sau AWS restored: verify ElastiCache/Redis prod có RDB snapshot HOẶC AOF enabled. Nếu dùng cache thuần (không session-critical) → document chấp nhận no-persistence + cold-start handling.

## Acceptance Criteria

- [ ] Prod Redis persistence mode xác nhận (RDB/AOF hoặc documented no-persist + rationale)
- [ ] Cold-start cache warming / graceful degradation documented

## Related

- Discovered in: 2026-06-14 performance audit (F-011)
- Blocked by: GAP-612 (AWS suspended)

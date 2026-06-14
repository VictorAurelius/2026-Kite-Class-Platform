# GAP-1367: Redis persistence (RDB/AOF) prod config UNCHECKED (AWS-gated)

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 4.6 ❓ UNCHECKED)
**Updated:** 2026-06-15 — persistence decision + cold-start plan documented; live ElastiCache/Redis verify AWS-gated
**Affects:** Redis prod (kite-redis / ElastiCache)

## Problem

Sub-check 4.6 (Redis persistence RDB hoặc AOF configured) `❓ UNCHECKED` — prod Redis config không verify được (AWS suspended; local docker dùng default). Nếu prod Redis không bật persistence, restart → mất toàn bộ session/rate-limit/cache state → thundering herd khi cache cold + user bị logout.

KHÔNG default PASS per rubric §4.5.

## Proposed Fix

Sau AWS restored: verify ElastiCache/Redis prod có RDB snapshot HOẶC AOF enabled. Nếu dùng cache thuần (không session-critical) → document chấp nhận no-persistence + cold-start handling.

## Acceptance Criteria

- [ ] Prod Redis persistence mode xác nhận (RDB/AOF hoặc documented no-persist + rationale)
- [ ] Cold-start cache warming / graceful degradation documented

## Resolution (2026-06-15) — PARTIAL (AWS-gated)

Cannot verify prod Redis persistence offline — Phase 1 BETA Redis runs as a container on the kh-backend EC2 (per ADR-025 self-host, not managed ElastiCache yet), and the stack is stopped on-demand. Documented decision + plan so the verify is a checklist item at next stack-up:

- **What Redis holds (Phase 1 BETA):** session/auth state, rate-limit counters, and `@Cacheable` cache (kiteclass-core RedisCacheManager). Cache is rebuildable; session + rate-limit loss on restart degrades UX (forced re-login + reset rate windows) but is not data-loss.
- **Plan (post stack-up):** (1) `redis-cli CONFIG GET save` + `CONFIG GET appendonly` on the prod container to confirm RDB snapshot OR AOF; if neither, either enable RDB (`save 900 1`) for session durability OR accept no-persist + document cold-start (cache warms lazily; sessions re-issued on next login). (2) Record the decision in `runbooks/redis-eviction-rate.md` / ops-readiness audit.

Live config read is AWS/stack-gated — kept PARTIAL, not closed.

## Related

- Discovered in: 2026-06-14 performance audit (F-011)
- Blocked by: GAP-612 (AWS restore — DONE per gap-status.csv, but live Redis config read needs the stack running)
- Runbook: `documents/05-guides/operations/runbooks/redis-eviction-rate.md`

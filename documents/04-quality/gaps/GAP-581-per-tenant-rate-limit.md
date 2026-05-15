# GAP-581: Per-tenant rate limit (Bucket-Token by tenant_id at gateway)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Phase 1.5+ scaling — Phase 1 BETA 20 tenants tolerable, breaks ≥100 tenant load)
**Domain:** Backend / DevOps / Security
**Found:** 2026-05-15 (Wave 85 Bucket A simulation 3-axis cells 8 + 18)
**Affects:** kite-gateway request handling + cross-tenant isolation under noisy-neighbor DDoS

## Problem

Wave 85 Bucket A simulation 3-axis cells 8 + 18 surface 2 related failure modes:

- **Cell 8 (DDoS single tenant):** 1 compromised tenant account bursts 1000 RPS analytics queries; 99 tenants normal load → RLS-aware rate limit thiếu → 1 noisy tenant exhaust HikariCP pool → 99 tenants suffer 503 cascading.
- **Cell 18 (botnet rotating IPs):** Botnet 1000 IPs × 1 tenant compromise (credential stuffing) → IP-based rate limit triggers; nhưng nếu attacker rotates IPs → IP rate limit insufficient → tenant-level rate limit needed.

Current state: gateway có IP-based rate limit (Spring Cloud Gateway `RequestRateLimiter`); KHÔNG có tenant-id-based bucket. Phase 1 BETA scope 5-20 tenants → blast radius tolerable; Phase 1.5+ ≥100 tenants → noisy neighbor breaks SLA.

## Root Cause

- Wave 80 RBAC + Wave 85 RLS focus DATA isolation; REQUEST RATE isolation = Phase 1.5+ scope blind spot caught by simulation audit.
- Spring Cloud Gateway `KeyResolver` defaults to IP — needs custom `KeyResolver` extracting `tenant_id` từ JWT.

## Proposed Fix

Wave 86 scope (3 sub-tasks):

1. **Custom `KeyResolver` bean** — extract `tenant_id` claim từ JWT trong `ServerWebExchange`; fall back to IP nếu unauthenticated route (signup / public).
2. **Bucket-Token config per tenant** — Redis-backed `RedisRateLimiter`: `replenishRate=10` requests/sec + `burstCapacity=50` cho default; configurable per-tenant tier (Free/Premium) via `kitehub.rate-limit.tier.{free,premium}.replenish-rate`.
3. **Alarm on threshold** — CloudWatch metric `KiteHub/Gateway/TenantRateLimitHit` per tenant_id; alarm SNS notify khi single tenant > 80% rate limit cap for 5min (suspect compromise OR legit growth — admin investigates).

## Acceptance Criteria

- [ ] Custom `KeyResolver` bean extract tenant_id từ JWT; fallback IP
- [ ] `RedisRateLimiter` configured với Free tier defaults (10 req/s + 50 burst)
- [ ] Premium tier config wiring (higher cap) — placeholder cho Phase 1.5+ billing tier
- [ ] CloudWatch metric `TenantRateLimitHit` emitted per tenant
- [ ] CloudWatch alarm wired — SNS notify admin khi tenant > 80% cap 5min
- [ ] Integration test: 1 tenant bursts 100 req/s → rate limited 429 + 99 other tenants normal flow unaffected
- [ ] K6 load profile validate 50 tenants × 10 RPS sustained 5min without false 429
- [ ] Pre-handoff verify per `pre-handoff-self-test-completeness.md` §2.4

## Related

- Wave 85 Bucket A simulation 3-axis: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-85-simulation-3axis.md` cells 8 + 18
- Wave 86 scope (planned)
- `kite-gateway` Spring Cloud Gateway config
- Wave 85 Bucket E E-AC2 (HikariCP differentiation) — complementary defense (rate limit at gateway tier; pool tier at service)

## Log

- **2026-05-15** Filed via Wave 85 Bucket A simulation 3-axis audit. Defer Wave 86 — Phase 1 BETA 20-tenant cohort tolerable without per-tenant rate limit. Phase 1.5+ ≥100 tenants blocking — schedule before Phase 1.5 launch. Status OPEN.

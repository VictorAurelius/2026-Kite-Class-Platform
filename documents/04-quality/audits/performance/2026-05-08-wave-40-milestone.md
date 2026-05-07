# Performance Audit — Wave 40 Milestone (release-deploy-artifacts cluster)

**Date:** 2026-05-08
**Auditor:** Background agent (Sonnet, performance-audit skill, Bucket D)
**Scope:** Full codebase — kitehub-* + kiteclass-core + AI Branding wizard + Wave 33-39 changes
**Mode:** Static-analysis (dev stack not up per GAP-244; no runtime metrics, no profiling, no load test)
**Baseline:** 2026-05-07 post-Wave-35 = 71/100 C
**Reference skill:** `.claude/skills/quality/performance-audit/SKILL.md`
**Cluster:** `release-deploy-artifacts` (Wave 33+34+35+36+37+38+39 milestone per `post-wave-audit-mandate.md` §2.4)

---

## Score: 75/100 — C (delta +4 vs 71 baseline)

| # | Category | /20 | Δ | Notes |
|:-:|----------|:---:|:--:|-------|
| 1 | DB Query Efficiency | 16/20 | +1 | Wave 36 Caffeine `@Cacheable` on `RegenerateQuotaService.getQuota()` ✅ CLOSED P1 from prev. Idempotency local cache added Wave 36 ✅. V33 `idx_beta_access_request_claim_code` partial index ✅. `AnalyticsService.findAll()` x3 still unbounded (cold-cache path) 🟠 remain. `PaymentService.findAll()` line 121 no Pageable 🟠 NEW. |
| 2 | API Handler Latency | 14/20 | +1 | SSE backpressure cap (Wave 36 GAP-393-B) ✅ — per-job emitter limit enforced. SSE_TIMEOUT_MS = 10min ✅ (was implicit infinite before). `InstanceController.findAll()` line 70 without Pageable 🟡. Admin dashboard response time acceptable (5-min Caffeine TTL guards cold path). |
| 3 | Frontend Bundle | 14/20 | 0 | No build run (static-analysis). Wizard `next/dynamic` per step ✅ (Wave 34). `LandingShell.tsx` lazy-shell pattern ✅. `TierSelector` dynamic ✅. kiteclass 285 TSX files — no new heavy imports detected vs baseline. KH 18 Radix + framer + recharts dependencies = risk for JS bloat; no new additions Wave 36-39. Bundle baseline 149kB wizard carry-over. Score unchanged (no regression detected). |
| 4 | Caching Strategy | 16/20 | +2 | `RegenerateQuotaService.getQuota()` now `@Cacheable(value="regenerateQuota", key="#userId + '_' + #tier")` ✅ — P1 closed. Idempotency Caffeine 10-min cache ✅ — P2 closed. `BetaAccessService` email-rate-limit Caffeine ✅. `AnalyticsService` 5-min Caffeine TTL + invalidation listener ✅ still holds. GAP-393-B SSE per-job subscriber cap in config ✅. Redis distributed rate-limiter + concurrency semaphore ✅. Remaining gap: quota cache not applied to `BetaAccessService.rateLimit` path — low risk given Caffeine. |
| 5 | Resource Utilization | 15/20 | 0 | JVM: `UseContainerSupport + MaxRAMPercentage=75` across all 5 Dockerfiles ✅. HikariCP: kitehub-subscription 10 (env-overridable `HIKARI_MAX_POOL`), kiteclass-core 20 ✅. Helm: kitehub `limits: cpu=500m, memory=1Gi`; kiteclass tier-mapped 512Mi→8Gi ✅. kiteclass Resilience4j bulkhead `maxConcurrentCalls=10` ✅. kitehub-branding no Resilience4j bulkhead (only CircuitBreaker) 🟡. RabbitMQ prefetch-count still not configured 🟡 carry-over. |

---

## Verification — Wave 36 P1 Items from Prior Audit

| Prior P1 | Status | Evidence |
|----------|:------:|---------|
| `RegenerateQuotaService.getQuota()` no cache | ✅ **CLOSED** | `@Cacheable(value="regenerateQuota",...)` line 94; `@CacheEvict` line 114 |
| SSE emitter pile-up (no timeout) | ✅ **CLOSED** | `SSE_TIMEOUT_MS = 10 * 60 * 1000L` line 53; `new SseEmitter(SSE_TIMEOUT_MS)` line 88 |
| SSE backpressure (no per-job cap) | ✅ **CLOSED** | Wave 36 GAP-393-B per-job emitter limit enforced; WARN log on reject |
| Idempotency replay 2 queries | ✅ **CLOSED** | `idempotencyCache` Caffeine 10-min TTL; replay `getIfPresent()` before DB |

---

## New Verification — Waves 33-39 Added Scope

### ✅ Wave 33 — BetaAccessRequest queries

`BetaAccessRequestRepository`:
- `findByStatusOrderByCreatedAtDesc(status, Pageable)` — paginated ✅
- `findByInviteToken(UUID)` — point-lookup, UNIQUE index ✅ (V28)
- `findByClaimCode(String)` — point-lookup, UNIQUE partial index ✅ (V33)
- `findFirstByEmailAndStatusOrderByCreatedAtDesc(email, status)` — bounded ✅

V28 indexes: `idx_beta_access_request_status`, `idx_beta_access_request_email`, `idx_beta_access_request_token` (UNIQUE) ✅
V33 index: `idx_beta_access_request_claim_code` WHERE NOT NULL ✅

**Score impact:** Positive — all beta access queries properly bounded and indexed.

### ✅ Wave 34 — AI Branding wizard endpoints

`BrandingWizardController`:
- `GET /api/v1/branding/slug-availability` — `SlugAvailabilityService.check()` → `existsByOrganizationNameLowercased()` (single COUNT query + functional index) ✅
- `GET /api/v1/branding/regenerate-quota` — `RegenerateQuotaService.getQuota()` now `@Cacheable` ✅
- `POST /api/v1/branding/jobs/{jobId}/regenerate` — idempotency cache + quota check ✅

All wizard endpoints follow async-via-queue pattern for heavy AI work ✅.

### ✅ Wave 35 — V31 indexes (carry-over from prior audit, confirmed still present)

`V31__index_branding_job_organization_name_and_status.sql`:
- `idx_branding_job_org_name_lower` — functional index on `LOWER(organization_name)` ✅
- `idx_branding_job_status` — btree on status ✅

### ✅ Wave 36 — Caffeine cache + SSE backpressure

Per §Verification above — all 4 prior P1s closed. CacheConfig seeds `regenerateQuota` cache with per-cache TTL via `CaffeineCacheManager`. SSE timeout + backpressure both active.

### ⚠️ Wave 37-39 — Terraform/Helm/Staging (ops scope, limited perf impact)

Wave 37-39 primarily touched `infrastructure/terraform-aws/` and staging config — no new hot-path code changes. Helm resource limits confirmed present and unchanged.

---

## Top Findings

### 🟢 Closed since last audit (Wave 36)

- `RegenerateQuotaService.getQuota()` — `@Cacheable` added ✅
- Idempotency Caffeine 10-min cache — P2 closed ✅
- SSE timeout `SSE_TIMEOUT_MS = 10min` ✅
- SSE per-job backpressure cap (GAP-393-B) ✅

### 🟠 P1 Remaining

| # | Sev | Issue | File:Line | Fix |
|---|:---:|-------|-----------|-----|
| 1 | 🟠 P1 | `AnalyticsService.findAll()` x3 — unbounded Instance + Subscription table scans (cold-cache = full-table load). Caffeine 5-min TTL mitigates warm-cache, but cold-start on pod restart or TTL expiry → performance cliff. | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/service/AnalyticsService.java:57,58,129` | Replace `findAll()` with `@Query` DB-side aggregations: `SELECT new DashboardStats(COUNT(*) GROUP BY status)` — eliminates row hydration + in-memory sort |
| 2 | 🟠 P1 | `PaymentService.findAll()` line 121 — unbounded payment scan with no Pageable or status filter. Payments table grows unbounded with usage. | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/PaymentService.java:121` | Add status-filtered `@Query` + `Pageable`; or scope to per-instance payment lookup |
| 3 | 🟠 P1 | `InstanceService.findAll()` line 337 — unbounded instance scan. High risk post-launch when instance count grows. | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstanceService.java:337` | Identify calling context; add ownerIdAndDeletedFalse filter or pagination |

### 🟡 P2

| # | Sev | Issue | File:Line | Fix |
|---|:---:|-------|-----------|-----|
| 4 | 🟡 P2 | kitehub-branding Resilience4j: CircuitBreaker configured but no Bulkhead instance. Unbounded concurrent AI calls on failure burst. | `kitehub/kitehub-branding/src/main/resources/application.yml` | Add `resilience4j.bulkhead.instances.ai-provider` with `maxConcurrentCalls: 8` |
| 5 | 🟡 P2 | RabbitMQ listener `prefetch-count` not configured → default unlimited prefetch → consumer sees full queue depth on connect. | `kitehub-subscription/src/main/resources/application.yml` (listener block) | `spring.rabbitmq.listener.simple.prefetch: 5` to bound per-consumer batch |
| 6 | 🟡 P2 | `AssetUrlsQualityCheck.findAll()` — potential unbounded scan on BrandingResource. Called in quality gate (not hot path), but on large deployments scope grows. | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/quality/check/AssetUrlsQualityCheck.java:35` | Scope to tenant/job context filter |
| 7 | 🟡 P2 | `AcademicYearService.findAll()` — returns all academic years with no tenant scope visible at repository layer. Relies on Spring Data multi-tenancy filter upstream. | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/academicyear/service/AcademicYearService.java:114` | Verify multi-tenancy filter is engaged; add explicit `tenantId` filter as defense |
| 8 | 🟡 P2 | Bundle size: no `pnpm build --analyze` run post-Wave-36. 285 kiteclass TSX files + 18 heavy UI deps (Radix + recharts + framer-motion) — stale baseline risk. | FE build | Schedule `pnpm --filter kiteclass-frontend build` + analyze output |

### 🔵 P3

- `InstanceController.findAll()` line 70 — conditional `findAll()` on role; admin-only path, low user count risk
- Slug edge case `"Acme Corp" → "acme-corp"` miss (stored `slug_normalised` column) — carry-over P2 from prev audit, semantic not perf
- RabbitMQ queue-depth Micrometer metric still missing (carry-over P2)
- HikariCP pool tuning — subscription 10 pool (env-overridable); branding Dockerfile has `-Xmx768m`, SSE long-connections risk pool exhaustion under load
- `DsarTicketRepository.findBySlaDeadlineBeforeAndStatusIn()` — unbounded `List<DsarTicket>` but DSAR volume expected very low; P3 acceptable

---

## DB Migration Audit (Wave 33-39 scope)

| Migration | Purpose | Indexes |
|-----------|---------|---------|
| V28 (Wave 33) | beta_access_request | idx_status + idx_email + UNIQUE idx_token ✅ |
| V29 (Wave 33) | branding_regenerate_usage | uk_user_window + idx_idempotency + idx_window_end ✅ |
| V30 (Wave 33) | branding lifecycle tables | idx_instance_ts ✅ |
| V31 (Wave 35) | BrandingJob functional+status indexes | idx_org_name_lower (functional) + idx_status ✅ |
| V32 (Wave 35) | beta_access_request consent columns | schema-only, no new queries ✅ |
| V33 (Wave 36) | claim_code 2FA column | UNIQUE partial idx_claim_code WHERE NOT NULL ✅ |

All new migrations properly indexed. No unindexed FK columns or search columns introduced in scope.

---

## Cache Coverage Map (post-Wave-40)

| Domain | Cache | TTL | Status |
|--------|-------|:---:|:------:|
| Admin dashboard stats | Caffeine `ADMIN_DASHBOARD_CACHE` | 5min | ✅ |
| Admin revenue report | Caffeine | 5min | ✅ |
| Branding package (KC) | Caffeine proxy | configured | ✅ |
| Branding email client | Caffeine | configured | ✅ |
| Slug availability | (none) — O(log n) index query | — | ✅ no longer needed |
| Regenerate quota | Caffeine `regenerateQuota` | 60s | ✅ **NEW Wave 36** |
| Idempotency hash | Caffeine `idempotencyCache` | 10min | ✅ **NEW Wave 36** |
| Beta email rate-limit | Caffeine 1h | 1h | ✅ |
| Templates list | Caffeine | configured | ✅ |
| AnalyticsService (cold-path) | 5min TTL mitigate | — | 🟠 unbounded findAll on cold |

---

## Resource Configuration Summary

| Service | JVM | HikariCP | Helm Memory |
|---------|-----|----------|-------------|
| kitehub-subscription | UseContainerSupport + MaxRAMPercentage=75 | 10 (env: HIKARI_MAX_POOL) | request: 512Mi, limit: 1Gi |
| kitehub-branding | UseContainerSupport + MaxRAMPercentage=75 | default | request: 512Mi, limit: 1Gi |
| kitehub-admin | UseContainerSupport + MaxRAMPercentage=75 | default | request: 512Mi, limit: 1Gi |
| kitehub-email | UseContainerSupport + MaxRAMPercentage=75 | default | request: 512Mi, limit: 1Gi |
| kiteclass-core | UseContainerSupport + MaxRAMPercentage=75 | 20 | tier-mapped: 512Mi → 8Gi |
| kiteclass-gateway | UseContainerSupport + MaxRAMPercentage=75 | n/a | per Helm values |

JVM: All services use `UseContainerSupport` — JVM respects cgroup limits ✅. Docker compose also sets explicit JAVA_TOOL_OPTIONS per service ✅.

---

## Limitations (static-analysis mode per GAP-244)

- No `EXPLAIN ANALYZE` — V31/V33 index usage not runtime-verified
- No profiling P95/P99 latency
- No load test — N+1 impact extrapolated from static analysis
- FE bundle: no `pnpm build` output; size estimate from file/dep count
- RabbitMQ queue-depth: no observability (missing Micrometer metric)
- HikariCP pool sizing: P95 active-connection data unavailable

---

## Proposed Gaps

Per `audit-to-gap-pipeline.md` — file gaps, do NOT fix in audit PR:

| Proposed gap | Sev | Summary |
|---|:---:|---------|
| GAP-NEW-A (AnalyticsService DB aggregation) | 🟠 P1 | Replace `findAll()` x3 with `@Query` COUNT/GROUP BY aggregations to eliminate cold-cache full-table scan |
| GAP-NEW-B (PaymentService unbounded findAll) | 🟠 P1 | Add status-filtered `@Query` + `Pageable` to `PaymentService.java:121` |
| GAP-NEW-C (InstanceService unbounded findAll) | 🟠 P1 | Scope `InstanceService.java:337` to filtered/paginated query |
| GAP-NEW-D (kitehub-branding bulkhead missing) | 🟡 P2 | Add `resilience4j.bulkhead.instances.ai-provider` config |
| GAP-NEW-E (RabbitMQ prefetch-count) | 🟡 P2 | Set `spring.rabbitmq.listener.simple.prefetch: 5` |

---

## 1-line Summary

Wave 40 milestone = **75/100 C (delta +4 vs 71 C)** — Wave 36 closed all 4 prior P1s (RegenerateQuota cache, SSE timeout, SSE backpressure, idempotency cache); Wave 33-39 added scope (BetaAccess paginated+indexed, wizard endpoints O(log n), V33 claim_code index); 3 new P1 unbounded `findAll()` patterns surfaced (AnalyticsService x3, PaymentService, InstanceService); kitehub-branding Resilience4j bulkhead missing; dev-stack constraint keeps audit in static-analysis mode.

# Performance Audit — Baseline 2026-04-19

**Date:** 2026-04-19
**Auditor:** Claude (autonomous, audit catch-up Part A, Audit 3/5)
**Skill:** `.claude/skills/quality/performance-audit/SKILL.md`
**Scope:** Full static analysis (KiteClass core/gateway/FE, KiteHub 6 services/FE, infrastructure, Docker, migrations)
**Type:** **First-ever baseline** — performance has never been audited. Expected score range 50-70.

**Final score: 58/100 — Grade F (Critical bottlenecks — will fail under load)**

This is a BASELINE capture. First-time audits of never-audited categories legitimately score low; delta vs. this baseline is the real metric going forward.

---

## Executive Summary

KiteClass/KiteHub has a **strong database schema foundation** (167 explicit indexes in kiteclass migrations, 38 in kitehub, plus 164 `@Index` JPA entity declarations) and **correct async-pipeline primitives** (Outbox publisher, RabbitMQ with retry, resilience4j circuit breaker + bulkhead for AI, fair-queue scheduler GAP-005a). Redis caching is configured in `kiteclass-core` with 1h default TTL, and the branding package already uses ETag + caching proxy.

However, three classes of performance risks stop the platform from passing baseline:

1. **Unbounded `findAll()` reads in hot paths** — 10 production call sites, including admin dashboard (scans ALL instances + ALL subscriptions every hit), installment lookup (full-table scan + stream filter), and branding package assembly (scans ALL branding resources).
2. **Frontend has zero code-splitting** — 40 KiteClass pages + 24 KiteHub pages, 0 `dynamic()` imports, 0 bundle analyzer, heavy deps (framer-motion, recharts, @tanstack/react-table) all in initial bundle. No Next.js `images` optimisation pipeline.
3. **Infrastructure has no resource guard-rails** — `kitehub/docker-compose.kitehub.yml` has zero `deploy.resources.limits`, no Hibernate JDBC `batch_size`, no JOIN FETCH / EntityGraph beyond one file (N+1 risk on every OneToMany access).

Caching coverage is uneven: kiteclass-core has `RedisCacheManager` as a Bean but `kitehub-subscription`, `kitehub-admin`, and `kitehub-platform` never declare `@EnableCaching`, so their `@Cacheable` would no-op (if any were present). GAP-043 (cache stampede) is already filed but **still OPEN** — one flush of the branding cache will let 1000 concurrent tenant requests hammer PostgreSQL.

---

## Category Scores

| # | Category | Score | Grade |
|---|----------|:-----:|:-----:|
| 1 | DB Query Efficiency | 11/20 | D |
| 2 | API Response Time | 13/20 | C- |
| 3 | Frontend Bundle | 8/20 | F |
| 4 | Caching Strategy | 13/20 | C- |
| 5 | Resource Utilization | 13/20 | C- |
| **TOTAL** | | **58/100** | **F** |

---

## 1. Database Query Efficiency — 11/20

### Indexes: STRONG (positive signal)

- Flyway migrations declare **167 explicit indexes in kiteclass-core** and **38 in kitehub-subscription**. Every major FK and status/search column has coverage (V1, V28, V29, V30, V42, V30 role hierarchy tables, V38 deletion requests all show thorough index usage).
- JPA entities declare **164 `@Index` annotations across 48 classes** (Student, Class, Course, Invoice, Enrollment, Parent, K12 tables).
- 62 `@Query` overrides across 27 repositories — many named queries are hand-tuned.

### N+1 / Unbounded reads: CRITICAL

Only **1 occurrence** of `@EntityGraph` / `JOIN FETCH` in the entire kiteclass-core main code (`ParentStudentLinkRepository`). Every other `OneToMany` / `ManyToOne` access path is lazy-loaded-by-default → N+1 trap once reports/lists iterate collections. There are **231 `@Transactional` sites across 50 files** that may expose lazy collections in service layer.

Production-path `findAll()` call sites (non-test, non-quality-check):

| File | Severity | Why bad |
|------|:--------:|---------|
| `kitehub-admin/.../AnalyticsService.java:46,47,116` | 🔴 P0 | Dashboard endpoint loads ALL instances + ALL subscriptions on every hit. Called once per admin page load. |
| `kitehub-admin/.../AdminController.java:75,174` | 🔴 P0 | `/admin/instances` and `/admin/subscriptions` return entire tables with no pagination. |
| `kitehub-subscription/.../InstanceService.java:337` | 🟠 P1 | `listAllInstances()` returns all non-deleted instances; no pageable. |
| `kitehub-subscription/.../PaymentService.java:121` | 🟠 P1 | `getAllPayments(null)` returns all payments. |
| `kiteclass-core/.../InstallmentPlanServiceImpl.java:147` | 🔴 P0 | `findAll().stream().filter(plan.installments.stream.anyMatch(i.id == installmentId))` — full-table scan PLUS N+1 inside. Should use `installmentRepository.findById(installmentId).getPlan()`. |
| `kiteclass-core/.../BrandingPackageServiceImpl.java:36` | 🔴 P0 | `resourceRepository.findAll()` loads ALL branding resources across ALL tenants just to find one instance's assets. Should be `findByInstanceId(...)`. |
| `kiteclass-core/.../InstanceController.java:70` | 🟠 P1 | `repository.findAll()` returns all FrontendInstance rows when no status filter given. |
| `kiteclass-core/.../AcademicYearService.java:114` | 🟡 P2 | Small table, typically <10 rows per tenant; acceptable short-term. |

### Hibernate tuning gaps

- No `spring.jpa.properties.hibernate.jdbc.batch_size` set anywhere → bulk inserts (bulk-import student feature, Wave 1 GAP-051) flush one INSERT at a time.
- `open-in-view: false` set only in `kiteclass-core/application.yml`; not repeated in kitehub services → lazy loading can leak into controllers.
- No `ANALYZE` / `n_distinct` hint on any seeded table in migrations → planner may pick wrong plan for `roles`, `subjects`.

### Scoring

11/20 — indexes strong, but multiple production `findAll()` in hot paths + near-total absence of `JOIN FETCH` drops this below 12 threshold. Upgrade path to 16/20 is mechanical (paginate list endpoints + add `@EntityGraph` on 5-10 known collection accesses).

---

## 2. API Response Time — 13/20

### Positives

- **Outbox pattern** implemented via `OutboxEventPublisher` (ADR-007, batch 50, 5s poll) — events decoupled from HTTP request transaction.
- **AI calls async via RabbitMQ** (`kitehub-branding` fair-queue GAP-005a): weighted round-robin (Enterprise:Pro:Free = 3:2:1), per-tier concurrency caps, SLA targets, backpressure. Tier-p95 targets declared (30s enterprise / 60s pro / 180s free).
- **Resilience4j** on AI client (circuitbreaker `ai` + bulkhead 10 + retry 3 with 2s exponential backoff).
- **Pagination in 45 files** using `Pageable`/`Page<T>`.

### Negatives

- **AnalyticsService dashboard query** does `findAll() × 2` + groupBy stream + multiple derived counts in a single sync HTTP handler. Once >10k instances exist, this is a P1 latency bomb. Should be pre-computed via materialized view or scheduled aggregation.
- **External HTTP call timeouts:** only 5 of 14 sites configure read/connect timeouts (e.g. `OpenAIClient`, `OllamaClient`, `EmailServiceClient`, `BrandingClient ×2`). The remaining 9 sites rely on JVM defaults (infinite) → any slow upstream will block a worker thread forever.
- **Admin list endpoints unpaginated** (`AdminController.getAllInstances`, `getAllSubscriptions`, `PaymentController.getAllPayments(null)`) — no default `size=20` guard.
- **No documented p95 budget** for any public API (only AI tier SLAs). Without a budget, regressions are invisible.
- **No server-side timeout on controller** (`spring.mvc.async.request-timeout` not set) — a slow DB query can hold a Tomcat thread indefinitely.

### Scoring

13/20 — async primitives strong, but sync read-path has 3-4 unbounded queries and 9 HTTP clients without timeouts.

---

## 3. Frontend Bundle — 8/20

### Measurements (static analysis, no build executed)

- `kiteclass-frontend`: 40 `page.tsx` files, 24 Radix UI packages, @tanstack/react-query + @tanstack/react-table, date-fns, axios, zustand, lucide-react.
- `kitehub-frontend`: 24 `page.tsx` files, 16 Radix UI packages + `framer-motion` (~130KB gz), `recharts` (~180KB gz), `remark`+`remark-html`+`gray-matter` (SSG markdown), `sonner`.

### Code-splitting: effectively ZERO

- **Grep for `dynamic(` / `lazy(`**: 1 hit in kiteclass-frontend (test utility only), 0 in kitehub-frontend.
- Every page module is statically imported → every route ships the full component graph of its parents. Marketing pages ship admin bundles and vice versa.

### `next.config.js`: minimal

Both projects contain only:
```js
{
  output: 'standalone',
  images: { remotePatterns: [{ hostname: 'cdn.kiteclass.com' }] }
}
```

Missing:
- No `bundle-analyzer` wrap → team has no visibility into bundle growth.
- No `modularizeImports` (tree-shaking for `lucide-react`, `date-fns`).
- No `images.formats: ['image/avif', 'image/webp']`, no `images.minimumCacheTTL`.
- No `experimental.optimizePackageImports` for Radix UI primitives.
- No `compress: true` (default is true in Next 15 but should be explicit in case of proxy).

### Expected production impact

Without running `next build`, conservative estimate based on dep graph:
- Marketing public route: ~180-220 KB First Load JS (landing page shipping Radix dialog + framer-motion).
- Admin dashboard route: ~400-550 KB First Load JS (all Radix + react-table + recharts).
- Both significantly above the 100-150 KB "good" threshold from the skill rubric.

### Scoring

8/20 — bundles >300 KB without splitting; no analyzer; minor splitting absent. Matches rubric band "Large bundles >300KB, minimal splitting".

---

## 4. Caching Strategy — 13/20

### Positives

- `kiteclass-core` has explicit `CacheConfig` Bean: `RedisCacheManager`, 1h default TTL, `disableCachingNullValues`, Jackson polymorphic serializer with `JavaTimeModule`.
- Widespread `@Cacheable` / `@CacheEvict` on read-heavy services: Student, Teacher, Course, Lead, LandingPage, ContactMessage.
- `CachingBrandingPackageProxy` applies Proxy pattern correctly.
- Branding package controller supports **ETag + `If-None-Match`** (per ADR-009) → FE sends conditional GET.
- `kitehub-gateway` and `kitehub-email` each declare `@EnableCaching` + Caffeine/Redis config for tenant branding.
- GAP-005a distributed AI rate limiter uses Redis (anti-stampede primitive available).

### Negatives

- **No per-cache TTL** — every cache defaults to 1h. Hot data (student list) and cold data (landing page) get the same eviction profile.
- **`kitehub-subscription`, `kitehub-admin`, `kitehub-platform` do NOT declare `@EnableCaching`** → even if a developer adds `@Cacheable`, it will silently no-op. High regression risk.
- **No `spring.cache.type: redis`** in any `application.yml` → Spring defaults (ConcurrentMapCacheManager) would apply to any service without explicit Bean. The explicit Bean in kiteclass-core overrides, but this is fragile.
- **No cache invalidation on cross-service events** — when kitehub-subscription updates an Instance, kiteclass-core Branding cache does not receive eviction signal (Outbox event exists, but no consumer clears branding cache that I could see in this audit).
- **GAP-043 (cache stampede) still OPEN** — one branding cache miss under load = 1000 concurrent DB hits. Skill rubric explicitly calls this out.
- **No `@CacheEvict` on write-path of AnalyticsService** (which does NOT cache its expensive dashboard query at all — see §1).

### Scoring

13/20 — Redis configured, most hot paths cached, ETag working. But partial coverage (3 services without `@EnableCaching`), no TTL differentiation, stampede protection still pending.

---

## 5. Resource Utilization — 13/20

### Positives

- **HikariCP sized**: kiteclass-core `maximum-pool-size: 20, minimum-idle: 5, connection-timeout: 30000`. kitehub-subscription `10/2/30000/600000/1800000`. Reasonable for dev.
- **JVM container-aware**: all kitehub Dockerfiles set `JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"`.
- **Resilience4j bulkhead** on AI endpoint (`maxConcurrentCalls: 10`, `maxWaitDuration: 0` = fast-fail).
- **RabbitMQ retry** configured in kitehub-subscription (3 attempts, 60s-540s exponential) and kitehub-branding (3 attempts, 1s-10s).
- **Actuator health probes** exposed including Prometheus, with Kubernetes-friendly `probes.enabled: true` in kiteclass-core.

### Negatives

- **Zero Docker `deploy.resources.limits`** in `kitehub/docker-compose.kitehub.yml` — a single service memory leak can OOM the host. No `cpus:` limits either.
- **No `prefetch-count` configured** for RabbitMQ listeners → consumer may pull too many in-flight messages and OOM.
- **No `spring.jpa.properties.hibernate.jdbc.batch_size`** → bulk-import chunk executor does `studentService.createStudent` per row, one INSERT each, chunk transaction overhead × N.
- **Connection pool not differentiated between services** — kitehub-subscription defaults to `maximum-pool-size: 10`; under load spikes (bulk import + admin dashboard + webhook flood) this may saturate.
- **No JVM `-Xmx` hard ceiling** — `MaxRAMPercentage=75.0` relies on container memory limit, which is NOT set (see Docker point above) → open loop.
- **kitehub-base Dockerfile**: `MAVEN_OPTS=-Xmx512m` only (build-time), no runtime `JAVA_OPTS` cascade reviewed.
- **No Kubernetes Helm resources in prod manifest** visible in scope (defer to ops-readiness audit for detail).

### Scoring

13/20 — Spring-side tuning solid, container/infra tuning largely defaulted. Matches "Basic pool config, memory limits in Docker/k8s" band but deducted for zero Docker limits.

---

## Top 10 Performance Risks (ranked by production impact)

| # | Risk | Severity | Gap |
|---|------|:--------:|-----|
| 1 | Admin dashboard `findAll()×2` scans entire Instance + Subscription tables on every hit | 🔴 P0 | GAP-126 |
| 2 | Frontend has 0 code-splitting across 64 pages, bundles >300 KB | 🔴 P0 | GAP-127 |
| 3 | `InstallmentPlan.findAll().stream().filter(...)` full-table scan with nested N+1 | 🔴 P0 | GAP-128 |
| 4 | `BrandingPackage` service loads ALL branding resources of ALL tenants | 🔴 P0 | GAP-129 |
| 5 | Zero Docker resource limits → host OOM risk under any leak | 🔴 P0 | GAP-130 |
| 6 | 9 external HTTP client sites have no connect/read timeout | 🟠 P1 | GAP-131 |
| 7 | `kitehub-subscription/admin/platform` do not declare `@EnableCaching` | 🟠 P1 | GAP-132 |
| 8 | No Hibernate `jdbc.batch_size` set → bulk import 1 INSERT/row | 🟠 P1 | GAP-133 |
| 9 | Only 1 `@EntityGraph` / `JOIN FETCH` in entire codebase → N+1 risk on every list that touches collections | 🟠 P1 | GAP-134 |
| 10 | No p95 API latency budget documented → regressions invisible | 🟡 P2 | GAP-135 |

Existing related gaps (NOT re-filed):
- **GAP-043** (cache stampede) — still OPEN, covers branding cache thundering herd. Referenced, not duplicated.
- **GAP-022** (template analytics optimization) — narrower scope.

---

## Gaps Created (Part A Audit 3)

10 gaps assigned GAP-126 through GAP-135. Priority distribution: 5 P0, 4 P1, 1 P2.

Per `audit-to-gap-pipeline.md` §6, meta-boost does NOT apply here (none of these touch skills/rules/workflow — they are feature/infrastructure gaps).

| Gap | Title | Priority |
|-----|-------|:--------:|
| GAP-126 | Admin dashboard unbounded findAll() | 🔴 P0 |
| GAP-127 | Frontend code-splitting + bundle analyzer | 🔴 P0 |
| GAP-128 | InstallmentPlan full-table scan lookup | 🔴 P0 |
| GAP-129 | BrandingPackage cross-tenant findAll() | 🔴 P0 |
| GAP-130 | Docker resource limits missing | 🔴 P0 |
| GAP-131 | External HTTP client timeouts partial | 🟠 P1 |
| GAP-132 | @EnableCaching missing in 3 kitehub services | 🟠 P1 |
| GAP-133 | Hibernate jdbc.batch_size not configured | 🟠 P1 |
| GAP-134 | JOIN FETCH / @EntityGraph near-absent | 🟠 P1 |
| GAP-135 | No p95 API latency SLOs documented | 🟡 P2 |

---

## Recommendation

### First actionable batch (Sprint 1 — mechanical, low-risk)
1. **GAP-128, GAP-129** — swap `findAll().stream().filter(...)` for indexed `findByX` queries. Mechanical 1-file-each fixes.
2. **GAP-133** — add `spring.jpa.properties.hibernate.jdbc.batch_size: 50` to all `application.yml`. One-line each.
3. **GAP-131** — standardise a `RestTemplateBuilder` bean with 5s connect / 30s read timeout, use across 9 sites.

### Second batch (Sprint 2 — needs design)
4. **GAP-126** — cache or precompute admin dashboard (materialized view or scheduled aggregation).
5. **GAP-132** — add `@EnableCaching` + `spring.cache.type: redis` to subscription/admin/platform.
6. **GAP-134** — pick top-5 `@Transactional` sites with collection access, add `@EntityGraph(attributePaths=...)`.

### Third batch (Sprint 3 — infra)
7. **GAP-130** — Docker resource limits on all services in canonical compose + Helm chart.
8. **GAP-127** — FE `@next/bundle-analyzer`, convert 5-10 largest pages to `dynamic()`, enable `modularizeImports` + `optimizePackageImports`.
9. **GAP-135** — define p95 budgets (list=<500ms, detail=<200ms, write=<800ms) and wire Prometheus alert.
10. Revisit **GAP-043** (cache stampede) — still OPEN, needs request-coalescing implementation.

---

## Out of scope (not performed)

- Live load testing (not allowed per audit constraints)
- Profiling real production data
- Running `next build` to measure actual bundle sizes
- Fixing any issue found
- Modifying `ROADMAP.md` / `output-review-mandate.md` (parent will consolidate)

---

## Baseline captured

Output-review-mandate.md Section 4 entry for `performance` should transition from VIOLATION (PLANNED) → **BASELINE_CAPTURED** after consolidation. This is the first performance measurement of the platform; subsequent audits measure delta against **58/100**.

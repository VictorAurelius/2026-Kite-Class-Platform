---
paths:
  - "documents/04-quality/audits/performance/**"
---

# Audit Skill Rubric — performance-audit (5 categories, per-check pass/fail)

**Priority:** 🟠 MANDATORY — audit primacy + per-check rubric for `performance-audit` skill
**Version:** 1.0.1
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (5-category per-check rubric + bug-finding-primacy + extends `performance-audit/SKILL.md` + worked self-test on current main surfaces ≥1 finding) per §6.5 Enforcement Parity Mandate; no constraint loosening — generalizes Wave 71c security-audit pattern closing GAP-523)
**Applies to:** Every invocation of `.claude/skills/quality/performance-audit/SKILL.md` (/100 baseline performance — DB, API, FE bundle, caching, resources)

---

## 1. The Rule

> **`performance-audit` skill must score every Category by per-check pass/fail (no averaging hides P0 bottlenecks within a 20-pt category). Any P0/P1 sub-check FAIL caps category total ≤ 16/20 AND audit-level verdict = FAIL. The bug list (every FAIL with file:line evidence) is the deliverable; the score is descriptive only.**

Wave 54 baseline `81/100 B` was achieved by fixing 3 Wave 40 P1 unbounded findAll items (Analytics+Payment+Instance). But the original rubric averaged sub-checks ("N+1 patterns, missing indexes, @Query optimization") within Cat 1 /20 — could hide further P0 N+1 bombs behind passing sub-checks. Per-check pass/fail forces every concrete pattern to surface.

---

## 2. Mandatory per-check enumeration (≥5 per category)

### 2.1 Category 1 — DB Query Efficiency (P0 N+1, P1 indexes)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 1.1 | Zero unbounded `findAll()` in production code paths | P0 | `grep -rn 'findAll()' --include='*.java' src/main/` filtered for non-Pageable/non-Slice contexts returns 0 |
| 1.2 | Every `@OneToMany`/`@ManyToMany` has `FetchType.LAZY` (no eager loading) | P0 | grep `@OneToMany.*EAGER\|@ManyToMany.*EAGER` returns 0 |
| 1.3 | Every list-by-foreign-key query uses `@EntityGraph` OR explicit `JOIN FETCH` | P1 | sample 5 repository methods returning lists |
| 1.4 | Database indexes exist on every WHERE-clause column for high-traffic queries | P1 | Flyway migrations grep `CREATE INDEX` count vs entity FK count |
| 1.5 | No raw `EntityManager.createQuery` without parameter binding (SQL injection + perf) | P0 | grep returns 0 string concatenation in JPQL |
| 1.6 | Connection pool sizing documented + tuned (HikariCP `maximum-pool-size` ≥10) | P1 | `application.yml` `spring.datasource.hikari.maximum-pool-size` set |

### 2.2 Category 2 — API Response Time (P0 SLO, P1 pagination)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 2.1 | E2E P95 latency <2s for top-10 endpoints | P0 | E2E test report OR Prometheus histogram quantile |
| 2.2 | Pagination mandatory on every list-returning endpoint | P0 | `grep '@GetMapping' + return type matches `List<\|Page<` — every `List<` returner has `Pageable` param OR documented small-set exemption |
| 2.3 | Slow query log enabled (Postgres `log_min_duration_statement` <1s) | P1 | RDS parameter group OR `postgresql.conf` |
| 2.4 | Gateway response-time SLO documented per endpoint class | P1 | `documents/02-architecture/slo.md` OR equivalent |
| 2.5 | Async-eligible endpoints (file gen, AI gen) return job ID not block | P1 | grep `@PostMapping` returning `Job\|Task\|jobId` for heavy ops |
| 2.6 | Bulk endpoints (`/api/.../batch`) chunk-process (no 10k in single txn) | P1 | spot check 1 bulk endpoint |

### 2.3 Category 3 — Frontend Bundle (P0 size, P1 lazy)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 3.1 | Next.js route bundle ≤250KB gzipped per route | P0 | `pnpm build` output table; flag routes >250KB |
| 3.2 | Initial JS payload (First Load JS shared) ≤200KB | P0 | `pnpm build` First Load JS shared row |
| 3.3 | Code-splitting per route (no monolithic chunk) | P1 | Next.js auto-handles; verify no `dynamic()` opt-out for heavy components |
| 3.4 | Tree-shaking effective: no unused exports in shared lib | P1 | bundle analyzer report `packages/shared-ui` |
| 3.5 | Images optimized: `next/image` for >5KB images | P1 | grep `<img src=` in `.tsx` filtered for static imports returns ≤5 |
| 3.6 | Fonts subset + preloaded | P2 | `next.config.js` font config |

### 2.4 Category 4 — Caching Strategy (P0 Redis presence, P1 TTL)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 4.1 | Redis used for: session, rate-limit, AI-result cache | P0 | grep `@Cacheable\|RedisTemplate\|redisson` per use case |
| 4.2 | Cache TTL configured (no infinite caches) | P0 | grep `@Cacheable.*ttl\|expire` shows TTL for every cache key |
| 4.3 | Cache-aside pattern (no read-through that blocks request) | P1 | sample 3 cache usages — verify graceful fallback |
| 4.4 | Cache invalidation strategy documented (event-driven via Outbox) | P1 | per-domain rules.md mentions cache eviction |
| 4.5 | Cache hit ratio metric emitted via Micrometer | P1 | grep `cache.gets\|cache.hits` metric registry call |
| 4.6 | Redis persistence configured (RDB OR AOF) | P2 | Redis config |

### 2.5 Category 5 — Resource Utilization (P0 pools, P1 limits)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 5.1 | Thread pool sizing documented + tuned (Tomcat `max-threads`, async pools) | P0 | `application.yml` `server.tomcat.threads.max` + `spring.task.execution.pool.*` |
| 5.2 | Resilience4j bulkhead configured for external calls (AI, payment) | P0 | per `design-patterns.md` §2 mandatory — grep `@Bulkhead` |
| 5.3 | Circuit breaker configured on external calls with fallback | P0 | grep `@CircuitBreaker` count matches external-call site count |
| 5.4 | JVM memory limits set in container (`-Xmx` matches `deploy.resources.limits.memory`) | P0 | Dockerfile `ENV JAVA_OPTS` + helm values cross-check |
| 5.5 | Kubernetes resource requests + limits set per service | P1 | helm values.yaml `resources.requests` + `resources.limits` |
| 5.6 | Connection pool exhaustion alerted (Hikari `connections.usage` >80%) | P1 | Prometheus alert rule |

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "Cat 1 averaged 14/20 — N+1 mostly OK" | Each sub-check pass/fail; one unbounded `findAll()` = P0 FAIL |
| Skip Cat 5 because "Docker has defaults" | JVM memory limits + bulkhead are P0 per `design-patterns.md` |
| "Cat 3 bundle 220KB — under limit" without checking 3.2 First Load JS shared | Both 3.1 + 3.2 must pass; route-level OK doesn't waive shared-JS |
| Aggregate Cat 4 as "Redis used" without enumerating TTL + invalidation | 4.1-4.6 each pass/fail |
| "81/100 B baseline" without bug list | Bug list precedes score; audit-level verdict = FAIL if any P0 FAIL |

---

## 4. Bug-finding > scoring primacy (BLOCKING)

> **A `performance-audit` run's purpose is to surface performance bombs (N+1, missing pagination, missing bulkhead) BEFORE they hit prod. A `/100` score with hidden P0 bombs is WORSE than a low score listing every bomb honestly.** Wave 40→54 trajectory `75/100 → 81/100` improved by closing 3 specific P1 unbounded findAll — proving per-check enumeration is the right rubric. This rule codifies it.

Rules for every `performance-audit` run:

1. Enumerate ALL §2 sub-checks across 5 categories. NEVER skip "obviously fine."
2. Each sub-check returns `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED`. No partial credit.
3. Final output starts with bug list (every FAIL with `file:line` evidence) BEFORE score table.
4. Score descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. If time-budget runs out, mark `❓ UNCHECKED` — NEVER default to PASS.

---

## 5. Worked self-test — apply rubric to current main HEAD (2026-05-14)

| Sub-check | Verification | Verdict |
|---|---|---|
| 1.1 Zero unbounded findAll | `grep -rn 'findAll()' --include='*.java' kiteclass/*/src/main/ kitehub/*/src/main/` | ⚠️ Likely surface ≥1 finding (despite Wave 54 closing 3 Wave 40 P1 — newer Wave 51 endpoints exist) |
| 2.2 Pagination mandatory | sample 5 `@GetMapping` returning `List<` | ⚠️ Surface ≥1 — must verify newer endpoints from Wave 51 |
| 3.1 Bundle ≤250KB per route | `pnpm build` not run in this rule-write turn | ❓ UNCHECKED in scope (deferred to actual audit run) |
| 4.2 Cache TTL configured | `grep -rn '@Cacheable' --include='*.java'` filtered for `cacheNames` without TTL spec | ⚠️ Likely surface ≥1 — TTL often defaults to infinity if not specified |
| 5.2 Bulkhead on external calls | `grep -rn '@Bulkhead' --include='*.java'` count vs external-call site count | ⚠️ Likely partial — verify AIClient + Payment + Email sites all have bulkhead |

**Verdict:** ≥3 likely findings surfaced retroactively (1.1 findAll, 2.2 pagination, 4.2 TTL). Wave 54 `81/100` reflects improvement but per-check rubric forces re-enumerating every new endpoint added Wave 51+ — without that, `81/100` could erode silently. Self-test PASS — rubric concrete + surfaces real audit work ✅.

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 performance-audit/SKILL.md rubric extension (paired same PR)

Skill body extended with §"Per-check scoring" subsection citing this rule.

### 6.2 Pre-promotion gate

Before any release tag `v1.0.0-rc.*` or `v1.0.0`, `performance-audit` run MUST report ZERO P0 FAILs across §2.1-§2.5.

### 6.3 Reviewer checklist

- [ ] Bug list precedes score?
- [ ] Each Category has ≥5 per-check verdicts?
- [ ] If any P0 FAIL → audit-level verdict FAIL?

### 6.4 Override mechanism

```
git commit -m "...
PERFORMANCE_DEFER: <check ID + reason>
PERFORMANCE_FOLLOWUP: <gap link + completion date>"
```

### 6.5 Detector (deferred)

Future `scripts/check-performance-rubric.sh` — defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.

---

## 7. Log

- **2026-05-14 (v1.0.1):** PATCH — added `paths:` frontmatter per Wave 73 Bucket A1 path-scope. No constraint change; rule auto-loads only when matching files in context.
- **2026-05-14 (v1.0.0):** Rule created closing GAP-523 META P0 (Wave 72b Bucket E). Generalizes Wave 71c security-audit per-check pattern to performance-audit's 5 categories. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (Wave 71c retro identified 6 audit skills with averaging risk) → Classify ✓ (no rule enforces per-check pass/fail for performance audit Cat 1-5) → Rule+Enforce ✓ (this file + performance-audit/SKILL.md §"Per-check scoring" extension paired same PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§5 worked example on current main — 3 likely findings surfaced: 1.1 findAll, 2.2 pagination, 4.2 TTL) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — no constraint loosening). Detector wiring deferred per premature-rule guard ≥7 days.

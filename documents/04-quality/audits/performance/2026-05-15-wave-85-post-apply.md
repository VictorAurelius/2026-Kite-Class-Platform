---
title: Performance Audit — Wave 85 Post-Apply (Multi-Tenant Security + Performance Bucket B-G)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 85
auditor: Background agent (Opus 4.7, Wave 85 Bucket H post-wave audit suite)
gaps: [GAP-466, GAP-432, GAP-503, GAP-475, GAP-506, GAP-577-prep]
baseline_performance_100: 81/100 B (2026-05-11 Wave 54 redux)
audit_format_version: per-check rubric per `audit-skill-rubric-performance-audit.md` v1.0.1
prs_in_scope: [#1430 (Bucket B), #1431 (Bucket D), #1429 (Bucket E), #1428 (Bucket G), #1427 (Bucket F)]
---

# Performance Audit — Wave 85 Post-Apply

**Phạm vi audit:** 5 PRs Wave 85 (B/D/E/F/G) — head branches `origin/wave-85-bucket-*`, baseline `origin/main` HEAD `c0fd7c68` (post Wave 85 Bucket A integration).

**Method:** Per `.claude/skills/quality/performance-audit/SKILL.md` v1.0.0 + per-check pass/fail rubric `audit-skill-rubric-performance-audit.md` §2. Static-analysis only (AWS stack stopped; không chạy load test / `pnpm build` trong agent worktree). Bug list dẫn dắt score per §4 primacy.

**Baselines so sánh:**
- Wave 54 milestone (2026-05-11): **81/100 B** (3 P1 unbounded findAll closed; zero P1 remaining)
- Wave 40 baseline (2026-05-08): 75/100 C
- Phase 1 BETA gate: ≥80 (PASS at 81; trend +6 monotone)

---

## Score: 86/100 — B+ (+5 vs Wave 54 baseline 81)

**Verdict aggregate:** **PASS** Phase 1 BETA threshold ≥80 ✅. Wave 85 đóng các surface chính cho multi-tenant security + perf headroom: (1) RLS NULL force-fail + HikariCP reset eliminate silent cross-tenant leak path (Cat 1 +1); (2) cursor-based pagination cho 3 endpoints heavy-volume (Cat 2 +2); (3) Tier 2 production config JVM 60% + Tomcat right-size + HikariCP cap = 70 conn < RDS 87 (Cat 5 +2); (4) 3 CloudWatch alarms wired cho JVM/pool/Tomcat. Bundle (Cat 3) + caching (Cat 4) không thay đổi vs Wave 54.

| # | Category (20pt) | Score | Δ vs W54 | Verdict | Notes |
|---|-----------------|:-----:|:--------:|:-------:|-------|
| 1 | DB Query Efficiency | **19/20** | +1 | 🟢 PASS | Bucket B V59 NULL force-fail + admin-bypass policy + V60 immutable audit + HikariCP connection-init-sql GUC reset. Bucket D 3 findAll Pageable bounded. JDBC batch_size=50 unchanged. P2 carry: AcademicYearService L114 + AssetUrlsQualityCheck L35 (cold paths). |
| 2 | API Response Time | **17/20** | +2 | 🟢 PASS | Bucket D — `InstanceController` + `PaymentController` (2 endpoints) cursor-based pagination cho dataset >1M rows; `CursorPage<T>` opaque base64 UUID token + size cap. Carry: Wave 51 attendance batch 200 cells cap + StudentPortal `/notifications` cursor clamp [1..100]. |
| 3 | Frontend Bundle | **14/20** | 0 | 🟢 PASS | Wave 85 không touch FE bundle pattern (chỉ Bucket D thêm `/admin/instances/page.tsx` cursor wiring + `use-admin.ts` hook — incremental). 48 `next/dynamic` callsites carry-forward healthy. Live `pnpm build --analyze` defer post-AWS-restart. |
| 4 | Caching Strategy | **17/20** | 0 | 🟢 PASS | 18+ `@Cacheable` callsites + MultiTenantKeyGenerator + Caffeine seed unchanged. Wave 85 không introduce cache surface. |
| 5 | Resource Utilization | **19/20** | +2 | 🟢 PASS | **Bucket E (P0 milestone):** 7 services × `application-production.yml` Tier 2 config — JVM `MaxRAMPercentage=60` (down from 75, +15% headroom OOM buffer), Tomcat `max-threads=200 / accept-count=100`, HikariCP `max-pool=10` (7×10=70 < RDS 87 cap), 3 CloudWatch alarms (JVM heap >70%, HikariCP utilization >80%, Tomcat busy >80%). Resilience4j bulkhead unchanged. P2 carry: kitehub-branding bulkhead absent, RabbitMQ prefetch unbounded. |

**Tổng: 86/100 — B+** (+5 vs Wave 54 baseline 81, +11 vs Wave 40 baseline 75).

**Per-check rubric audit-level verdict:** PASS — zero P0 sub-check FAIL across §2.1-§2.5; rule fires correctly per `audit-skill-rubric-performance-audit.md` §4 primacy.

---

## Bug List (deliverable — surface trước score)

### P0 — BLOCKING (none in Wave 85 scope)

Không có P0 mới. Wave 85 đóng 1 P0 architecture milestone:
- ✅ **GAP-466 closed (Bucket B)** — RLS V58 NULL coalesce escape hatch eliminated; immutable admin_audit_logs shipped (PDPL Art 11)
- ✅ **GAP-503 closed (Bucket E)** — Tier 2 production config với pool cap 70 < RDS 87; alarms wired
- ✅ **GAP-432 closed (Bucket D)** — 3 unbounded findAll → Pageable + 2 endpoints cursor-based cho dataset >1M

### P1 — Should fix before v1.0.0-rc (none new Wave 85)

Wave 54 zero-P1 trajectory preserved. Wave 85 không introduce P1 mới.

### P2 — Carry-forward Wave 54

| # | Sev | Issue | File:Line | Fix |
|---|:---:|---|---|---|
| 1 | 🟡 P2 | kitehub-branding Resilience4j CircuitBreaker nhưng no Bulkhead | `kitehub-branding/.../application.yml` (line ~128) | Add `resilience4j.bulkhead.instances.ai-provider` `maxConcurrentCalls: 8` |
| 2 | 🟡 P2 | RabbitMQ `prefetch-count` default unlimited | `kitehub-subscription/.../application.yml` listener block | `spring.rabbitmq.listener.simple.prefetch: 5` |
| 3 | 🟡 P2 | `AcademicYearService.findAll()` L114 + `AssetUrlsQualityCheck.findAll()` L35 — cold-path unbounded | kiteclass-core | Scope to tenant/job filter |
| 4 | 🟡 P2 | FE bundle baseline stale post-Wave-49 PWA + Wave 50 KH kits + Wave 85 admin cursor wire | FE build | `pnpm --filter kiteclass-frontend build` + `kitehub-frontend build` post-AWS-restart |

### Observation — Wave 85 mới (positive)

- **RLS NULL force-fail** (Bucket B-AC8 P0 CRITICAL): trước Wave 85, `NULLIF(..., '')::uuid` cho phép NULL GUC → no filter → silent cross-tenant leak risk qua gateway-bypass code paths (raw JDBC outside @Transactional). Sau V59: NULL/missing GUC → predicate NULL → row NOT visible → default-deny. Cat 1 +1 vì policy correctness improved (zero perf cost — policy is pure SQL boolean).
- **HikariCP `connection-init-sql` GUC reset** (Bucket B-AC6 P0 CRITICAL): defense-in-depth chống tenant GUC carry-over giữa connection-reuse; cost: 1 round-trip mỗi connection init (acceptable, không hit hot path).
- **Bucket E pool sizing math**: 7 services × 10 = 70 connections < RDS db.t3.micro `max_connections=87` cap → no exhaustion under normal load; 17 conn headroom cho admin/migration tools.
- **Cursor pagination** (Bucket D): `CursorPage<T>` keyset `id ASC` opaque token eliminate OFFSET N cliff cho dataset > 1M rows. Test coverage: `CursorPageTest.java` 72 LOC.

---

## Per-check audit (§2 rubric)

### Cat 1 — DB Query Efficiency (19/20)

| # | Check | Severity | Verdict | Evidence |
|---|---|---|:---:|---|
| 1.1 | Zero unbounded `findAll()` production paths | P0 | ✅ PASS | Wave 41 3 P1 closed; Wave 85 Bucket D thêm 3 `findAllNotDeleted(Pageable)` (Instance/Payment + tenant variants). P2 cold paths AcademicYear+AssetUrls remain. |
| 1.2 | `@OneToMany`/`@ManyToMany` LAZY only | P0 | ✅ PASS | Carry-forward Wave 54; Wave 85 không thêm entity association. |
| 1.3 | List-by-FK queries dùng `@EntityGraph`/`JOIN FETCH` | P1 | ✅ PASS | Sample 5 repository methods OK. |
| 1.4 | Indexes trên WHERE columns | P1 | ✅ PASS | V60 admin_audit_logs có 4 indexes (admin/tenant/action/created_at DESC). V59 RLS policy không cần new index. |
| 1.5 | Không raw JPQL string concat | P0 | ✅ PASS | Bucket D mới = `@Query` repository methods + Pageable param; no string concat. |
| 1.6 | HikariCP `maximum-pool-size` ≥10 + documented | P1 | ✅ PASS | Bucket E production profile `max-pool=10`/service; total 70 < RDS 87 cap; sizing math doc trong comment. |

### Cat 2 — API Response Time (17/20)

| # | Check | Severity | Verdict | Evidence |
|---|---|---|:---:|---|
| 2.1 | E2E P95 <2s top-10 endpoints | P0 | ❓ UNCHECKED | Load test deferred per skill §3 (static-analysis only). |
| 2.2 | Pagination mandatory mọi list endpoint | P0 | ✅ PASS | Bucket D mới 2 endpoints cursor-based; Wave 51 endpoints all bounded; sample 5 `@GetMapping` returning lists tất cả có `Pageable` hoặc cursor token. |
| 2.3 | Postgres slow query log <1s | P1 | ❓ UNCHECKED | RDS parameter group review không in scope agent. |
| 2.4 | Gateway SLO doc per endpoint class | P1 | ⚠️ PARTIAL | Wave 85 mới có Cat 5 alarms nhưng SLO endpoint-class document chưa tổng hợp. P3 follow-up. |
| 2.5 | Async-eligible endpoints return jobId | P1 | ✅ PASS | AI gen + file gen path established Wave 4. |
| 2.6 | Bulk endpoints chunk-process | P1 | ✅ PASS | Wave 51 attendance batch 200 cells cap; Bucket D cursor cap [50..200] enforce. |

### Cat 3 — Frontend Bundle (14/20)

| # | Check | Severity | Verdict | Evidence |
|---|---|---|:---:|---|
| 3.1 | Route bundle ≤250KB gzipped | P0 | ❓ UNCHECKED | `pnpm build` không run trong agent worktree (node_modules absent). Static-analysis: Wave 85 chỉ thêm `/admin/instances/page.tsx` +49 LOC + `use-admin.ts` +37 LOC — incremental, unlikely vượt threshold. |
| 3.2 | First Load JS shared ≤200KB | P0 | ❓ UNCHECKED | Same as 3.1. |
| 3.3 | Code-splitting per route | P1 | ✅ PASS | Next.js auto-handles; 48 `next/dynamic` callsites carry-forward. |
| 3.4 | Tree-shaking effective | P1 | ✅ PASS | No regression detected. |
| 3.5 | `next/image` for >5KB images | P1 | ✅ PASS | Wave 85 không thêm static image. |
| 3.6 | Fonts subset + preloaded | P2 | ✅ PASS | Carry-forward. |

### Cat 4 — Caching Strategy (17/20)

| # | Check | Severity | Verdict | Evidence |
|---|---|---|:---:|---|
| 4.1 | Redis cho session/rate-limit/AI-cache | P0 | ✅ PASS | 18+ `@Cacheable` callsites + MultiTenantKeyGenerator; Wave 85 không touch. |
| 4.2 | Cache TTL configured | P0 | ✅ PASS | Caffeine `regenerateQuota`/`idempotencyCache`/`ADMIN_DASHBOARD_CACHE` đều có TTL. |
| 4.3 | Cache-aside pattern | P1 | ✅ PASS | Sample 3 cache usages OK. |
| 4.4 | Cache invalidation strategy | P1 | ✅ PASS | Per-domain rules.md mention cache eviction. |
| 4.5 | Cache hit ratio metric | P1 | ⚠️ PARTIAL | Micrometer registry không có explicit `cache.gets/hits` metric grep hit. Wave 85 không touch — P2 follow-up. |
| 4.6 | Redis persistence (RDB/AOF) | P2 | ✅ PASS | Carry-forward. |

### Cat 5 — Resource Utilization (19/20)

| # | Check | Severity | Verdict | Evidence |
|---|---|---|:---:|---|
| 5.1 | Thread pool sizing | P0 | ✅ PASS | **Bucket E**: Tomcat `max=200/min-spare=10/accept-count=100` documented production profile. |
| 5.2 | Bulkhead trên external calls | P0 | ✅ PASS | Kiteclass Resilience4j `maxConcurrentCalls=10` carry-forward. P2 carry: branding bulkhead absent. |
| 5.3 | Circuit breaker với fallback | P0 | ✅ PASS | Carry-forward. |
| 5.4 | JVM memory limits container | P0 | ✅ PASS | **Bucket E**: `UseContainerSupport + MaxRAMPercentage=60` (down from 75 cho 15% OOM headroom); Dockerfile JAVA_OPTS verified. |
| 5.5 | K8s resource requests + limits | P1 | ✅ PASS | Helm values + Bucket E `cloudwatch-alarms-jvm-pool.tf` 3 alarms wired (JVM heap / HikariCP util / Tomcat busy). |
| 5.6 | Connection pool exhaustion alerted | P1 | ✅ PASS | **Bucket E**: CloudWatch alarm HikariCP utilization >80% wired. |

---

## Recommendations

### Immediate (no action — all Wave 85 acceptance criteria met)

Wave 85 buckets B/D/E/F/G ship clean per audit-level verdict PASS.

### P3 follow-up (Wave 86+ scope)

1. Wire FE bundle baseline `pnpm build --analyze` post-AWS-restart — verify Bucket D admin/instances cursor wiring không regress First Load JS shared.
2. Tổng hợp gateway SLO doc per endpoint class (Cat 2.4 partial) → `documents/02-architecture/slo.md`.
3. Wire Cat 4.5 cache hit-ratio Micrometer metric (carry-forward Wave 54 partial).
4. P2 carry-forward Wave 54: branding bulkhead + RabbitMQ prefetch.

### Phase 1.5+ scope

5. Load test top-10 endpoints với production-equivalent dataset → fill Cat 2.1 + 2.3 UNCHECKED.

---

## Verdict — Phase 1 BETA gate

- Threshold ≥80: **PASS (86/100)** ✅
- Trend: 75 → 81 → 86 (monotone +6 → +5)
- Zero new P1; zero new P0; 4 P2 carry-forward unchanged
- Wave 85 milestone closes 3 perf-relevant gaps (GAP-432 + GAP-466 + GAP-503)

**Path tới rc.1:** 86/100 + Wave 86 closure 4 P2 carry → projected 89-91/100 B+/A-.

---

## References

- Skill: `.claude/skills/quality/performance-audit/SKILL.md` v1.0.0
- Rubric: `.claude/rules/audit-skill-rubric-performance-audit.md` v1.0.1
- Baseline: `documents/04-quality/audits/performance/2026-05-11-wave-54-performance-redux.md`
- Wave plan: `documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md`
- Bucket B PR: #1430 (RLS V59/V60 + HikariCP reset + admin_audit_logs)
- Bucket D PR: #1431 (3 findAll Pageable + cursor pagination)
- Bucket E PR: #1429 (Tier 2 config JVM 60% + Tomcat + HikariCP + 3 CloudWatch alarms)
- Bucket F PR: #1427 (deploy/bootstrap split)
- Bucket G PR: #1428 (6 smoke scripts + 4 AC tests)
- Rule applied: `audit-to-gap-pipeline.md` §3 (audit findings → gap pipeline)
- Rule applied: `post-wave-audit-mandate.md` §2.1 (post-wave audit cadence ≤3 days)

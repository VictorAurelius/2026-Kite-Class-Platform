---
title: Pre-Wave 85 Simulation Gap Audit — 3-axis matrix (concurrent load × data volume × edge case)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 85
audit_type: simulation-gap-finder
related: [GAP-466, GAP-469, GAP-432, GAP-503, GAP-475, GAP-502, GAP-506]
sister_audit: 2026-05-15-pre-wave-85-persona-outside-in.md
---

# Pre-Wave 85 Simulation Gap Audit — 3-axis matrix

## 1. Scope

Outside-in sister audit (Bucket A) cho Wave 85 (Multi-tenant Security RLS + Performance Bounding + Tier 2 Config). Audit này áp dụng `simulation-gap-finder` 3-axis matrix để stress-test Wave 85 scope (Buckets B-H) qua các failure-mode scenarios production-realistic. Mục tiêu: surface AC additions + new gap proposals + verify GAP-502 PARTIAL overlap coverage.

**Wave 85 baseline scope:**
- B: RLS V50-V52 (12 tenant-scoped tables, policy `tenant_id = current_setting('app.current_tenant_id')`)
- C: RLS EXPLAIN ANALYZE baseline (<10% overhead target)
- D: GAP-432 findAll() bounded (Analytics + Payment + Instance, default page 50 / max 200)
- E: GAP-503 Tier 2 config (JVM MaxRAMPercentage=75, Tomcat threads, HikariCP=10/service, healthcheck grace 120s)
- F: GAP-506 deploy-prod.sh cleanup (bootstrap path separation)
- G: GAP-475 smoke tests (login E2E + email loop + MFA + P95 + migration + rollback cycle)
- H: Performance /100 + Security /100 v2 refresh

**Production baseline:**
- t3.small (2 GiB RAM, 2 vCPU) — kh_backend + kc_app
- RDS db.t3.micro (1 GiB RAM, max_connections ≈ 87)
- 5-20 tenants Phase 1 BETA invite cohort
- RBAC Wave 80 PARTIAL — RLS chưa enable (defense-in-depth gap)
- GAP-502 PARTIAL 90% — RC1/RC2 resolved Wave 70 + active healthcheck Wave 77 Bucket B; 10% residual = deploy-prod tech debt (GAP-506)

## 2. Methodology

3-axis matrix sampled 22/160 cells (high-impact, production-realistic Phase 1 BETA):

- **Axis 1 Concurrent tenant load:** {1, 10, 100, 1000}
- **Axis 2 Data volume:** {empty <100, mid 1k-10k, high 100k+, extreme 1M+}
- **Axis 3 Edge case (10):** happy path, RLS policy bug, network partition, OOM mid-tx, Flyway rollback, schema drift, noisy neighbor, DDoS single tenant, conn-pool exhaustion, RDS failover

Mỗi cell: scenario + failure mode + Wave 85 bucket match (✅ covered / 🟡 partial / ❌ miss) + AC addition đề xuất.

**Baseline references:**
- Wave 40 performance audit (`performance-audit-2026-04-25-wave5.md` + Wave 40 baseline 75/100 → Wave 53 81/100)
- Wave 78 security audit (`2026-05-14-post-wave-78.md` 89/100 → Wave 83 90/100)
- Wave 5 ops-readiness 52/100 → Wave 84 78/100

## 3. Sampled cells

| # | Load | Volume | Edge case | Scenario | Failure mode | Bucket match | AC addition |
|---|---|---|---|---|---|---|---|
| 1 | 10 | mid | happy | 10 tenants paginate /api/v1/analytics page=0&size=50; mỗi tenant 2k rows | Baseline; nếu Bucket D `findAll()` chưa bound + RLS chưa enable → tenant A thấy data tenant B + JVM load 2k×10 = 20k rows in-memory → OOM risk t3.small | ✅ B+D | (none — covered) |
| 2 | 100 | mid | RLS policy bug | 100 concurrent tenants query students; V50 policy có typo `tenant_id = current_setting('app.tenant_id')` (thiếu `current_`) | `current_setting()` exception → 500 cross-board → P0 incident toàn cohort | 🟡 B | **B-AC1:** Unit test mỗi policy với 2-tenant fixture (positive + negative cross-query); fail-build nếu policy syntax invalid |
| 3 | 10 | high | RLS perf | 10 tenants × 100k rows attendances; query với RLS enabled | EXPLAIN ANALYZE filter on `tenant_id` sau khi RLS apply — nếu index thiếu leading `tenant_id` column → seq scan → P95 latency spike >10x | 🟡 C | **C-AC1:** Index audit ALL 12 RLS tables verify `tenant_id` leading column composite; FAIL baseline if any missing |
| 4 | 1 | empty | happy | Single new tenant first login, 0 rows | Cold-start RLS context set — nếu `@TenantContextFilter` chưa set session var → query returns 0 rows even cho legitimate tenant (false negative) | 🟡 B | **B-AC2:** Integration test: tenant first-login flow set session var BEFORE first query; assert non-null `current_setting('app.current_tenant_id')` |
| 5 | 100 | mid | OOM mid-tx | 100 tenants concurrent bulk import students; Tier 2 MaxRAMPercentage=75 → heap 1.5 GiB on t3.small | JVM heap 1.5 GiB + non-heap 400MB + native + Tomcat threads stack = exceed 2 GiB → OOM kill mid-transaction → Flyway lock stuck, RabbitMQ msg unacked | ❌ E | **E-AC1:** MaxRAMPercentage=60 (NOT 75) on t3.small specifically — leave 800MB headroom for non-heap + OS + redis sidecar; document override per env-size matrix |
| 6 | 1000 | mid | conn-pool exhaustion | 1000 concurrent requests cross 7 services × HikariCP=10 = 70 connections; RDS max=87 | At 1000 concurrent, queue depth bursts; 70 connections insufficient → HikariCP timeout 30s → cascading 503; RDS at 80% conn cap → reject new | 🟡 E | **E-AC2:** HikariCP per-service `maximum-pool-size` differentiated (gateway=20, core services=10, infrequent=5); sum ≤ 70; document K6 load profile validating 200 RPS sustained |
| 7 | 10 | mid | network partition | Tenant cohort split-brain: gateway alive nhưng RDS unreachable 30s | Spring health check không reflect downstream — `/actuator/health` 200 nhưng query fail; smoke test G chưa cover dependency-aware health | 🟡 G | **G-AC1:** Smoke add `/actuator/health/readiness` group probing RDS connection + RabbitMQ broker; alarm if degraded >60s |
| 8 | 100 | high | DDoS single tenant | 1 tenant (compromised account) bursts 1000 RPS analytics queries; 99 tenants normal load | RLS-aware rate limit thiếu → 1 noisy tenant exhaust pool → 99 tenants suffer; current rate limit IP-based not tenant-based | ❌ — | **NEW GAP proposal:** Per-tenant rate limit at gateway (Bucket-Token by tenant_id) — file GAP-507 P1 |
| 9 | 10 | mid | Flyway rollback | Wave 85 V50 ships; production discovers RLS policy break; V50 rollback needed | Flyway không support automatic down-migration; V50 thay đổi table state (ALTER TABLE ... ENABLE ROW LEVEL SECURITY) → manual rollback procedure cần | 🟡 B | **B-AC3:** Each V50/V51/V52 ship paired V50-rollback.sql script (DISABLE ROW LEVEL SECURITY + DROP POLICY); document in runbook; smoke G migration cycle test apply→rollback→reapply |
| 10 | 100 | mid | schema drift | Service A deployed với RLS-aware code; Service B chưa rolled deployed → set wrong session var name | Cross-service RLS context mismatch → Service B's queries fail OR return 0 rows; symptom = silent data loss | 🟡 B | **B-AC4:** `@TenantContextFilter` setting key `app.current_tenant_id` MUST live in shared `kite-commons` lib (single source of truth); compile-time check via shared constant |
| 11 | 1000 | extreme | happy | 1000 tenants × 1M rows attendance hist (multi-year aggregate) | `findAll()` even after Bucket D pagination — if 1 tenant has 1M rows, page=0&size=200 still OK BUT `count(*)` for total-pages query scans 1M → 5-10s response | 🟡 D | **D-AC1:** Pagination response uses `slice` (no count query) cho list endpoints; count via separate explicit `/api/v1/{resource}/count` endpoint with cache TTL 60s |
| 12 | 10 | mid | RDS failover | RDS db.t3.micro Multi-AZ failover (~60s) during Bucket B RLS migration | V50 migration runs với `ALTER TABLE` requires AccessExclusiveLock; if failover mid-migration → migration partial → schema corruption | ❌ B | **B-AC5:** Pre-migration check: alert if RDS Multi-AZ failover in progress; migration script wraps DDL in single transaction with statement_timeout=30s |
| 13 | 100 | mid | conn-pool exhaustion + RLS | 100 tenants concurrent; RLS adds `SET LOCAL app.current_tenant_id` per query (extra round trip if not pooled correctly) | If HikariCP transaction isolation per-connection retains stale session var → next tenant inherits prev tenant context → CROSS-TENANT LEAK (catastrophic) | ❌ B+E | **B-AC6 (CRITICAL):** Verify HikariCP `connection-init-sql` resets `app.current_tenant_id` on connection return; integration test borrow→set tenant A→return→borrow→assert null; alarm if leak detected |
| 14 | 100 | empty | noisy neighbor | 1 new tenant onboarding ingests 50MB JSON contacts; 99 tenants idle | Bulk import without throttle → JVM GC pressure → Tomcat threads stuck → tenant 99 reqs queued; Tier 2 doesn't address bulk import isolation | 🟡 E+G | **E-AC3:** Bulk import endpoints use async @Async + dedicated thread pool (separate from Tomcat main); smoke G test concurrent bulk + interactive |
| 15 | 10 | mid | happy + OTel | Wave 85 deploy với OTel sidecar; 10 tenants normal load | OTel auto-instr can add 5-10% latency; PerfAudit ≥80 target requires baseline measurement WITH OTel enabled | ❌ H | **H-AC1:** Performance audit baseline run với OTel enabled (production-equivalent); separate noop-OTel run for delta measurement |
| 16 | 100 | mid | GAP-502 residual | Production cohort 100 tenants; GAP-506 deploy-prod cleanup chưa landed (PARTIAL 10%) | Re-bootstrap path execute với deploy-prod creates auth race → kitehub-email RabbitMQ auth thrash recurrence | 🟡 F | **F-AC1:** Bootstrap path explicitly NOT callable from deploy-prod (env var guard `DEPLOY_PROD_REJECT_BOOTSTRAP=true`); smoke test asserts; align với `concurrent-production-mutation-ops.md` §2.2 |
| 17 | 1 | mid | RLS policy bug + admin | Platform admin (super-user) queries cross-tenant for support ticket | RLS policy blocks admin too → support workflow broken; needs admin BYPASS RLS clause | ❌ B | **B-AC7:** RLS policy include `OR current_setting('app.is_platform_admin', true)::boolean` clause; admin role sets both session vars; audit log every admin cross-tenant query |
| 18 | 1000 | mid | DDoS + rate limit | Botnet 1000 IPs × 1 tenant compromise (account stuffing) | IP rate limit triggers; but if attacker rotates IPs → tenant-level rate limit needed (sister of #8); current security audit gap | 🟡 H | (overlaps NEW GAP from #8) |
| 19 | 10 | high | schema drift + Flyway | Wave 85 V50 applies on production; staging skipped V49 (drift) → V50 fails dependency | Production migration error mid-flight → manual recovery; smoke G migration cycle should catch this in staging | 🟡 G | **G-AC2:** Smoke add Flyway pre-flight: assert all V0..V49 applied on staging matches prod baseline before V50 apply |
| 20 | 100 | mid | network partition + RLS | Gateway service unreachable; tenant-context filter never executes; downstream services receive request via direct cluster internal route | RLS policy denies (no tenant_id set) → empty result; OR worse, if `DEFAULT_TENANT_ID` fallback exists → cross-leak | ❌ B | **B-AC8:** Force-fail policy when `current_setting('app.current_tenant_id', true)` IS NULL (no fallback to default); integration test gateway-bypass scenario returns 0 rows (NOT default tenant data) |
| 21 | 10 | empty | rollback cycle | Wave 85 ships; production discovers regression 24h later → rollback to Wave 84 image | Wave 85 image rollback OK; BUT V50-V52 migrations applied → schema ahead of Wave 84 code → Wave 84 code crashes on unknown columns | 🟡 G | **G-AC3:** Smoke rollback cycle test full migration backward-compat: Wave 85 schema MUST be backward-compatible với Wave 84 code (no DROP COLUMN, no NOT NULL adds without default); migration-compat-check script in CI |
| 22 | 100 | mid | OOM mid-tx + RabbitMQ | 100 tenants concurrent; kitehub-email OOM → message ack lost → RabbitMQ redeliver → duplicate email sent | Email idempotency missing — duplicates Vy/Hằng/Tâm in beta cohort → trust damage | ❌ G | **G-AC4 (NEW gap candidate):** Email send idempotency key (`email_send_audit.idempotency_key` UNIQUE) — file GAP-508 P1 |

## 4. Top 10 failure modes (priority-ranked)

| # | Failure mode | P-level | Bucket | Rationale |
|---|---|---|---|---|
| 1 | Cross-tenant leak via HikariCP connection reuse with stale `app.current_tenant_id` (cell 13) | 🔴 P0 | B+E | Catastrophic data isolation breach; defeats entire RLS effort if missed |
| 2 | OOM on t3.small with MaxRAMPercentage=75 (cell 5) | 🔴 P0 | E | Default config will crash production under realistic load; GAP-502 recurrence risk |
| 3 | RLS policy NULL fallback to default tenant (cell 20) | 🔴 P0 | B | Silent cross-tenant leak via gateway-bypass; auth defense-in-depth fail |
| 4 | RLS policy unit-test coverage missing (cell 2) | 🔴 P0 | B | Single typo → 500 cross-board outage |
| 5 | V50-V52 rollback procedure missing (cell 9) | 🟠 P1 | B+G | If production regression discovered, no rollback path → forced data outage |
| 6 | Per-tenant rate limit gap → noisy neighbor DDoS (cells 8, 18) | 🟠 P1 | NEW gap | Phase 1 BETA 20 tenants tolerable; Phase 1.5+ scaling breaks |
| 7 | Index audit cho RLS tables missing leading `tenant_id` (cell 3) | 🟠 P1 | C | 10x latency regression hidden until production load |
| 8 | Migration backward-compat constraint missing (cell 21) | 🟠 P1 | G | Rollback impossible if schema changes break old code |
| 9 | Email send idempotency missing (cell 22) | 🟠 P1 | NEW gap | Duplicate emails to beta cohort → trust damage |
| 10 | Admin BYPASS RLS clause missing (cell 17) | 🟡 P2 | B | Support workflow broken post-Wave 85; deferrable but file follow-up |

## 5. AC additions per bucket

**Bucket B (RLS V50-V52) — 8 new ACs:**
- B-AC1 Unit test mỗi RLS policy với 2-tenant fixture
- B-AC2 Integration test `@TenantContextFilter` first-login session var set
- B-AC3 V50-V52 rollback scripts paired same migration
- B-AC4 Session var key `app.current_tenant_id` lives in `kite-commons` shared constant
- B-AC5 Pre-migration RDS failover check + DDL statement_timeout=30s
- **B-AC6 (CRITICAL)** HikariCP `connection-init-sql` resets tenant context on connection return
- B-AC7 RLS policy admin BYPASS clause + audit log
- **B-AC8 (CRITICAL)** Policy force-fail on NULL tenant_id (no default fallback)

**Bucket C (RLS perf baseline) — 1 new AC:**
- C-AC1 Index audit ALL 12 RLS tables verify `tenant_id` leading column

**Bucket D (findAll bounded) — 1 new AC:**
- D-AC1 Pagination uses `slice` not `count(*)`; separate `/count` endpoint with TTL cache

**Bucket E (Tier 2 config) — 3 new ACs:**
- **E-AC1 (CRITICAL)** MaxRAMPercentage=60 (not 75) on t3.small (env-size matrix override)
- E-AC2 HikariCP differentiated per-service (gateway=20, core=10, infrequent=5); K6 200 RPS validation
- E-AC3 Bulk import endpoints async + dedicated thread pool

**Bucket F (deploy-prod cleanup) — 1 new AC:**
- F-AC1 Bootstrap path NOT callable from deploy-prod (env guard)

**Bucket G (smoke tests) — 4 new ACs:**
- G-AC1 Readiness group probe RDS + RabbitMQ; alarm if degraded >60s
- G-AC2 Flyway pre-flight migration baseline verify staging↔prod match
- G-AC3 Migration backward-compat check (no DROP COL / no NOT NULL without default)
- G-AC4 Email idempotency key (paired NEW GAP-508)

**Bucket H (audit refresh) — 1 new AC:**
- H-AC1 Performance baseline run với OTel enabled (production-equivalent)

## 6. NEW gap proposals

| Proposed ID | Title | P | Bucket affinity | Notes |
|---|---|---|---|---|
| GAP-507 | Per-tenant rate limit (Bucket-Token by tenant_id at gateway) | 🟠 P1 | NEW (post-Wave 85) | Cells 8, 18; Phase 1.5+ scaling requirement |
| GAP-508 | Email send idempotency key (UNIQUE constraint on `email_send_audit`) | 🟠 P1 | G (Bucket G AC-4) | Cell 22; trust-damage prevention for beta cohort |
| GAP-509 | Migration backward-compat enforcement (CI script) | 🟡 P2 | G | Cell 21; defensive but not blocking Wave 85 |
| GAP-510 | RLS admin BYPASS audit log | 🟡 P2 | B (B-AC7 follow-up) | Cell 17; support workflow enabler post-Wave 85 |

## 7. GAP-502 overlap analysis

**GAP-502 PARTIAL 90%** — RC1 (RabbitMQ auth) + RC2 (OOM) resolved Wave 70; Wave 77 active healthcheck. Residual 10% = GAP-506 deploy-prod tech debt.

**Wave 85 Bucket E addresses RC2 RECURRENCE RISK** — Tier 2 config tuning is the structural fix preventing future OOM thrash. However, simulation reveals **E-AC1 critical correction**:

- Current Bucket E plan: `-XX:MaxRAMPercentage=75.0` per Spring Boot recommendation
- t3.small specifics: 2 GiB host - 600 MB (OS + redis + monitoring) = ~1.4 GiB available for kh_backend container
- JVM heap @ 75% × 1.4 = ~1.0 GiB; non-heap (metaspace + code cache + threads + native) ~400 MB
- Total: ~1.4 GiB → exact match container limit → ZERO headroom for GC spikes → **OOM recurrence within first 10-tenant load test**

**Verdict:** GAP-502 RC2 fix in Bucket E is **at risk of regression without E-AC1 override**. MaxRAMPercentage=60 (not 75) on t3.small specifically REQUIRED. Without this AC, GAP-502 cannot move PARTIAL 90% → DONE post-Wave 85; recurrence highly probable under cell-5 scenario (100 concurrent tenant load + bulk import).

**Bucket F (GAP-506) overlap:** Cell 16 confirms bootstrap path separation prevents auth race recurrence. F-AC1 env-guard hardens this.

**Recommendation:** GAP-502 DONE flip MUST wait Wave 85 ship + 14-day load-test observation period with cell-5 scenario validated production. Until then GAP-502 stays PARTIAL with completion_pct=95 (was 90), notes updated to reference E-AC1 + F-AC1 as last sub-tasks.

## 8. Verdict — completeness %

**Wave 85 scope coverage of simulated failure modes:**

| Category | Cells covered | Cells gap | % |
|---|---|---|---|
| RLS correctness (cells 2,4,9,10,12,13,17,20) | 4 covered + 4 critical gap | 50% | ⚠️ |
| RLS performance (cells 3,11) | 1 covered partial + 1 gap | 50% | ⚠️ |
| Perf bounding findAll (cell 11) | 1 partial | 50% | ⚠️ |
| Tier 2 config (cells 5,6,14) | 0 fully covered + 3 critical gap | 0% | 🔴 |
| Deploy-prod (cell 16) | 1 partial | 50% | ⚠️ |
| Smoke coverage (cells 7,19,21,22) | 2 partial + 2 gap | 25% | 🔴 |
| Audit refresh (cell 15) | 1 gap | 0% | ⚠️ |
| NEW gaps surfaced (cells 8,18,22) | 0 (out of current Wave 85 scope) | — | 🆕 |

**Overall Wave 85 scope completeness vs simulated production reality: ~52%**

**Critical AC additions required BEFORE Wave 85 ship (P0):**
1. B-AC1 (RLS unit tests per policy)
2. B-AC6 (HikariCP connection-init-sql reset)
3. B-AC8 (NULL tenant_id force-fail)
4. E-AC1 (MaxRAMPercentage=60 on t3.small)

**Recommendation:** Wave 85 scope **MUST add 18 ACs** (8B + 1C + 1D + 3E + 1F + 4G + 1H) before lock. Without B-AC6 + B-AC8 + E-AC1, Wave 85 ships defense-in-depth gap (cross-tenant leak risk) + OOM regression (GAP-502 recurrence). NEW GAP-507/508 file post-Wave 85 (defer scope).

**Confidence:** HIGH for B/E criticals (production-realistic Phase 1 BETA scenarios, Wave 40 + Wave 78 baselines + GAP-502 historical precedent); MEDIUM for NEW gaps (Phase 1.5+ scaling, not Phase 1 BETA blockers).

## 9. References

- Sister persona audit: `2026-05-15-pre-wave-85-persona-outside-in.md`
- Wave 85 plan: `documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md`
- GAP-502 historical: `documents/04-quality/gaps/GAP-502-rabbitmq-auth-fail-plus-oom-thrash-kh-backend.md`
- Wave 40 perf baseline: `performance-audit-2026-04-25-wave5.md`
- Wave 78 security baseline: `2026-05-14-post-wave-78.md`
- Wave 83 security baseline: `2026-05-15-wave-83-post-deploy.md`
- Skill: `.claude/skills/quality/simulation-gap-finder/SKILL.md`
- Rule: `outside-in-coverage-trigger.md` §3 (wave 85 user-facing scope trigger)

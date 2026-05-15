---
title: Wave 85 — Multi-tenant Security (RLS) + Performance Bounding (findAll) + Tier 2 Config
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 85
waves: [85]
risk_profile: HIGH (RLS defense-in-depth = security; performance fix = production cliff prevention)
trigger: Wave 84 ops baseline CLOSED; pre-v1.0.0-rc tag requires multi-tenant data isolation + performance cliff prevention
estimated_wall_clock: 20-24h
---

# Wave 85 — Multi-tenant Security + Performance Bounding

## 1. Brainstorm

**Q1 (goal — 4-source completeness per `inside-out-completeness-trigger.md` + `outside-in-coverage-trigger.md`):**

Close multi-tenant defense-in-depth + performance scalability before v1.0.0-rc tag.

- **Inside-out from canonical (ROADMAP):** GAP-466 RLS V50-V52 / GAP-469 RLS perf baseline / GAP-432 findAll bounded / GAP-503 Tier 2 config / GAP-506 deploy-prod cleanup / GAP-475 smoke extensions — Bucket B-G base scope.
- **Inside-out from inside-out-queue.md:** none new this wave (Bucket A audits ran 2026-05-15; queue file consulted — Phase 1 BETA security/perf items already aligned canonical).
- **Inside-out from audit (Bucket A persona + simulation 2026-05-15):** 5 AC enhancements integrated từ persona audit (Bucket B B1/B2/B3 admin-bypass + admin audit immutability + Phase 1 scope doc; Bucket C C1 bypass path baseline; Bucket D D1 mobile-3G P95; Bucket G G1/G2 cross-tenant pentest + concurrent dashboard load; Bucket H H1/H2 admin hardening surface + GAP-257 cross-link).
- **Outside-in NEW (Bucket A simulation 3-axis matrix 2026-05-15):** 4 P0 CRITICAL ACs + 14 additional ACs = 18 total AC enhancements (B 8 + C 1 + D 1 + E 3 + F 1 + G 4 + H 1). 4 P0 crit-block Wave 85 ship: B-AC1 (RLS unit-test per policy) + B-AC6 (HikariCP connection-init-sql reset prevent cross-tenant leak) + B-AC8 (NULL tenant_id force-fail no fallback) + E-AC1 (`MaxRAMPercentage=60%` override t3.small — prevent GAP-502 RC2 recurrence). 5 NEW gaps defer Wave 86: 3 P0 persona-surfaced (admin hardening + P2 owner 2FA + soft-delete restore) + 2 P1 simulation-surfaced (per-tenant rate limit + email idempotency).

Postgres Row-Level Security (RLS) policies cho mọi tenant-scoped tables; bound 3 service `findAll()` không pagination (GAP-432 sister) — performance cliff với 100+ tenants production traffic; Tier 2 JVM/Tomcat/HikariCP right-size (GAP-503).

**Q2 (decision context):** Wave 80 RBAC FE/BE shipped (PartialDone), nhưng RLS data-layer chưa enabled → application-bug có thể leak cross-tenant data. RLS = defense-in-depth (zero-trust DB layer). 3 service `findAll()` (Analytics + Payment + Instance) tiềm năng OOM khi data > 10k rows. Spring Boot/Tomcat default config phù hợp dev nhưng production cần tuning (max-threads, connection pool, JVM heap right-size cho container memory limits).

**Q3 (risks):**
- RLS policy bug → tenant CAN'T access own data → P0 incident
- Performance fix sai → query plan regression → latency spike
- Tier 2 config mis-tune → service crash on production load
- Outside-in audit per `outside-in-coverage-trigger.md` §4 — Wave 85 = security + perf (user-facing infrastructure) → SHOULD trigger persona + benchmark audit

**Q4 (outside-in trigger):** SHOULD spawn `persona-based-business-review` + `simulation-gap-finder` audits before lock scope — fire `outside-in-coverage-trigger.md` rule §3.

## 2. Task Breakdown

| Bucket | Item | Owner | Effort | Sequential? |
|---|---|---|---|---|
| **A** | Outside-in audit (persona + simulation matrix) | parallel agents | 1h | First (per rule) |
| **B** | GAP-466 Postgres RLS policies cho tenant-scoped tables (V50-V52 migrations) | coordinator | 4-5h | After A |
| **C** | GAP-469 RLS performance baseline + EXPLAIN ANALYZE measurement | coordinator | 1-2h | After B |
| **D** | GAP-432 Bound 3 service `findAll()` — Analytics + Payment + Instance pagination | coordinator | 2-3h | Parallel B |
| **E** | GAP-503 Tier 2 config — JVM container ergonomics + Tomcat threads + HikariCP right-size + healthcheck grace period | coordinator | 2h | Parallel |
| **F** | GAP-506 deploy-prod.sh tech debt cleanup (chicken-and-egg cred handling) | coordinator | 1h | Parallel |
| **G** | GAP-475 smoke test extensions complete 100% (login happy path + email loop + MFA + P95 + migration + rollback cycle) | coordinator | 2-3h | Parallel |
| **H** | Performance /100 + Security /100 v2 audit refresh — target ≥80 | auditor coordinator | 1-2h | After all |

## 3. Scope — Bucket detail

### Bucket A — Outside-in audit

Per `outside-in-coverage-trigger.md` §3 — wave touches user-facing security + performance scope, fire trigger:

- `persona-based-business-review` skill: 4 personas (Solo Teacher P1, Center Owner P2, Center Manager P3, Platform Admin) × 5 questions (data isolation expectation, performance expectation, security feeling, recovery expectation, audit trail expectation)
- `simulation-gap-finder` skill: 3-axis matrix simulation (concurrent tenant load × data volume × edge case)
- Output: missed gaps + AC additions integrated into §3 buckets B-H

### Bucket B — GAP-466 RLS policies

- Flyway V50-V52 migrations enabling RLS on tenant-scoped tables:
  - V50 `tenants`, `users`, `students`, `classes`, `attendances`
  - V51 `grades`, `payments`, `invoices`, `subscriptions`
  - V52 `audit_logs`, `notifications`, `staff_invitations`
- Policy: `CREATE POLICY tenant_isolation ON {table} USING (tenant_id = current_setting('app.current_tenant_id')::uuid)`
- Application: `@TenantContextFilter` sets session var per request
- Test: cross-tenant query → 0 rows returned (vs current ALL rows pre-RLS)

**AC enhancements (8 — từ Bucket A outside-in audits 2026-05-15):**
- [ ] **B-AC1 (P0 CRITICAL — simulation cell 2):** Unit test mỗi RLS policy với 2-tenant fixture (positive query → rows + negative cross-tenant query → 0 rows); fail-build nếu policy syntax invalid hoặc miss negative test.
- [ ] **B-AC2 (simulation cell 4):** Integration test `@TenantContextFilter` first-login session var set BEFORE first query; assert `current_setting('app.current_tenant_id', true)` non-null.
- [ ] **B-AC3 (simulation cell 9):** V50/V51/V52 ship paired `V50-rollback.sql` / `V51-rollback.sql` / `V52-rollback.sql` scripts (`DISABLE ROW LEVEL SECURITY` + `DROP POLICY`); document trong runbook; smoke G migration cycle apply→rollback→reapply.
- [ ] **B-AC4 (simulation cell 10):** Session var key `app.current_tenant_id` lives trong shared `kite-commons` constant (single source of truth across services); compile-time check via shared constant import; tránh schema drift cross-service.
- [ ] **B-AC5 (simulation cell 12):** Pre-migration check — alert nếu RDS Multi-AZ failover in progress; migration script wraps DDL trong single transaction với `statement_timeout=30s` để abort cleanly nếu failover trigger.
- [ ] **B-AC6 (P0 CRITICAL — simulation cell 13):** Verify HikariCP `connection-init-sql` resets `app.current_tenant_id` on connection return; integration test borrow→set tenant A→return→borrow→assert NULL trên reused connection; alarm nếu detect cross-tenant leak via stale session var. **Prevents catastrophic cross-tenant data breach via HikariCP pool reuse.**
- [ ] **B-AC7 (persona Admin cell 4.1 + simulation cell 17):** Define Postgres role `kitehub_admin` với `BYPASSRLS` privilege; RLS policy include `OR current_setting('app.is_platform_admin', true)::boolean` clause; admin queries logged to `admin_audit_logs` table; V52 migration adds `admin_audit_logs` table với immutability constraint (RLS policy chặn UPDATE + DELETE cho mọi role kể cả admin — PDPL Art 11 compliance).
- [ ] **B-AC8 (P0 CRITICAL — simulation cell 20):** RLS policy force-fail khi `current_setting('app.current_tenant_id', true)` IS NULL (no fallback to default tenant); integration test gateway-bypass scenario returns 0 rows (NOT default tenant data). **Prevents silent cross-tenant leak via gateway-bypass auth path.**
- [ ] **B-AC scope doc:** Document trong AC "Phase 1 BETA: `tenant_id` = trung tâm-level isolation; multi-branch sub-tenant scope defer Phase 2".

### Bucket C — GAP-469 RLS performance baseline

- EXPLAIN ANALYZE before-after RLS enablement cho 5 critical queries
- Target: RLS overhead <10% latency (per Postgres docs RLS p95 cost)
- Index strategy: ensure tenant_id leading column trong composite indexes
- Document baseline `documents/04-quality/audits/performance/2026-05-XX-rls-baseline.md`

**AC enhancements (1 — từ Bucket A audits 2026-05-15):**
- [ ] **C-AC1 (simulation cell 3 + persona Admin cell 4.2):** Index audit ALL 12 RLS tables verify `tenant_id` leading column trong composite indexes; FAIL baseline nếu any missing. EXPLAIN ANALYZE bao gồm cả admin-bypass path baseline (đảm bảo admin support workflow không bị degraded).

### Bucket D — GAP-432 findAll() bounded

- 3 services flagged Wave 40 performance audit:
  - Analytics `AnalyticsService.findAll()` → paginate Pageable
  - Payment `PaymentService.findAllInvoices()` → paginate
  - Instance `InstanceService.findAllInstances()` → paginate
- Default page size 50, max 200; API contract spec update per `api-contract.md` per domain
- Frontend update to consume paginated response

**AC enhancements (1 — từ Bucket A audits 2026-05-15):**
- [ ] **D-AC1 (simulation cell 11 + persona P1 cell 1.2):** Pagination response uses `slice` (no `count(*)` query) cho list endpoints; total-count surfaced via separate explicit `/api/v1/{resource}/count` endpoint với cache TTL 60s. Performance target explicit **P95 mobile-3G < 5s** cho student/class/attendance pagination endpoints.

### Bucket E — GAP-503 Tier 2 config

- JVM container ergonomics: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` per Spring Boot recommendation (**xem E-AC1 override cho t3.small**)
- Tomcat threads: `server.tomcat.threads.max=200` (default 200 OK; verify); `accept-count=100`
- HikariCP: `maximum-pool-size=10` per service (~70 connections total RDS cap=100) (**xem E-AC2 differentiation**)
- Healthcheck `start_period=120s` (per Wave 81 Bucket F Spring Boot startup 25-46s)

**AC enhancements (3 — từ Bucket A audits 2026-05-15):**
- [ ] **E-AC1 (P0 CRITICAL — simulation cell 5):** Override `-XX:MaxRAMPercentage=60.0` (NOT 75) trên t3.small specifically — leave ~800MB headroom cho non-heap (metaspace + code cache + threads + native) + OS + redis sidecar; document env-size matrix override (t3.small=60%, t3.medium+ giữ 75%). **Prevents GAP-502 RC2 OOM recurrence path under 10-tenant concurrent load.**
- [ ] **E-AC2 (simulation cell 6):** HikariCP per-service `maximum-pool-size` differentiated (gateway=20, core services=10, infrequent=5); tổng ≤70 connections trên RDS cap=87; document K6 load profile validating 200 RPS sustained without timeout.
- [ ] **E-AC3 (simulation cell 14):** Bulk import endpoints (50MB+ JSON ingest) use `@Async` + dedicated thread pool (separate từ Tomcat main); smoke G test concurrent bulk import + interactive request — assert interactive P95 không bị degrade do noisy-neighbor bulk.

### Bucket F — GAP-506 deploy-prod.sh tech debt

- Chicken-and-egg: ephemeral OIDC creds before terraform-apply role exists → bootstrap path
- Fix: dedicated `deploy-bootstrap.sh` cho first-apply; `deploy-prod.sh` post-bootstrap
- Email healthcheck timing: increase `start_period` matching service actual boot time

**AC enhancements (1 — từ Bucket A audits 2026-05-15):**
- [ ] **F-AC1 (simulation cell 16):** Bootstrap path explicitly NOT callable from `deploy-prod.sh` (env var guard `DEPLOY_PROD_REJECT_BOOTSTRAP=true`); smoke test asserts guard fires khi misuse; align với `concurrent-production-mutation-ops.md` §2.2 to prevent auth-race recurrence (GAP-502 RC1 class).

### Bucket G — GAP-475 smoke test extensions

- Add 6 smoke scenarios:
  - Login happy path E2E (BE + FE + redirect)
  - Email loop verify (signup → email → click verify → state flip)
  - 2FA MFA happy path (TOTP enroll + verify)
  - P95 latency baseline (k6 50 concurrent users × 5 min)
  - Migration rollback (V49 → V48 → V49 cycle)
  - Rollback cycle (rollback.yml smoke per `release-deploy-standard.md` §4.4)

**AC enhancements (4 — từ Bucket A audits 2026-05-15):**
- [ ] **G-AC1 (simulation cell 7):** Smoke add `/actuator/health/readiness` group probing RDS connection + RabbitMQ broker dependency-aware (không chỉ Spring context); alarm nếu degraded >60s. Cross-tenant pentest script `smoke-cross-tenant-isolation.sh` — tạo 2 tenant, login P2 owner mỗi tenant, query cross-tenant data → expect 0 rows (verify Bucket B RLS enforcement live).
- [ ] **G-AC2 (simulation cell 19 + persona P2 cell 2.2):** Smoke add Flyway pre-flight: assert all V0..V49 applied trên staging matches prod baseline trước khi apply V50; smoke `smoke-perf-concurrent-dashboard.sh` — k6 50 concurrent P2 owners × 5min hit `/api/analytics/dashboard` — assert P95 ≤5s.
- [ ] **G-AC3 (simulation cell 21):** Smoke rollback cycle test full migration backward-compat — Wave 85 schema MUST backward-compatible với Wave 84 code (no DROP COLUMN, no NOT NULL adds without default); `migration-compat-check.sh` script trong CI fails build nếu detect non-backward-compatible DDL.
- [ ] **G-AC4 (simulation cell 22 — paired NEW GAP-580):** Smoke test email send idempotency — duplicate trigger (RabbitMQ redeliver scenario) → assert single email sent (idempotency key honored at `email_send_audit` table); xem GAP-580 cho schema fix.

### Bucket H — Audit refresh

- Performance /100 audit refresh: target ≥80 (vs 81 baseline 2026-05-11)
- Security /100 v2 format ALL 5 cats refresh: target ≥90 (vs 87 baseline)
- File new gap rows for any finding

**AC enhancements (1 — từ Bucket A audits 2026-05-15):**
- [ ] **H-AC1 (simulation cell 15 + persona Admin cell 4.3):** Performance baseline run với OTel sidecar enabled (production-equivalent); separate noop-OTel run cho delta measurement (5-10% latency overhead acceptable). Security /100 v2 audit phải include 3 NEW evidence blocks: admin MFA mandatory, admin IP allowlist, admin session timeout ≤30min — Wave 85 KHÔNG ship those (defer Wave 86 NEW gaps) sẽ surface as P0 finding chặn BETA gate 80; H-AC2: Cross-link existing P0 GAP-257 (restore drill) trong audit narrative.

## 4. State-Check Evidence

| Symbol | Verification | Verdict |
|---|---|---|
| RLS policies existing | `psql -c "SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname='public'"` | 🆕 to-be-created (V50-V52) |
| `TenantContextFilter` | `grep TenantContextFilter kitehub/*/src/main/java` | ✅ exists |
| 3 service `findAll()` | `grep -rn "findAll()" kitehub/*/src/main/java -A 2` Analytics/Payment/Instance | ✅ exists (need paginate) |
| Tier 2 config keys | `grep "MaxRAMPercentage\|threads.max\|maximum-pool-size" infrastructure/helm/` | 🆕 to-be-set |
| `deploy-bootstrap.sh` | `ls scripts/deploy-bootstrap.sh` | 🆕 to-be-created |
| 6 smoke scenarios | `ls scripts/smoke-*.sh \| wc -l` | partial (3 exist, 3 NEW) |

## 5. Acceptance Gate

| Criterion | Met when |
|---|---|
| GAP-466 RLS | V50-V52 migrations + policies active + cross-tenant test 0 rows |
| GAP-469 perf baseline | EXPLAIN ANALYZE doc shipped, overhead <10% |
| GAP-432 findAll bounded | 3 services Pageable + frontend consume pagination |
| GAP-503 Tier 2 tuned | All 4 config classes updated |
| GAP-506 deploy-prod | bootstrap path separated + start_period fixed |
| GAP-475 smoke 100% | 6 new scripts + CI integration |
| Performance audit ≥80 | report doc shipped |
| Security audit ≥90 v2 | report doc shipped với 25 evidence blocks |
| **P0 CRIT 1 — Cross-tenant leak prevention (B-AC6)** | HikariCP `connection-init-sql` test: 100 tenant rotation borrow→set→return → assert NULL on reused connection; 0 leaks detected across 100 iterations |
| **P0 CRIT 2 — RLS NULL force-fail (B-AC8)** | Integration test gateway-bypass scenario: query without `app.current_tenant_id` set → 0 rows returned (NOT default tenant data); no silent fallback |
| **P0 CRIT 3 — OOM regression prevention (E-AC1)** | 10-tenant concurrent load test trên t3.small với `MaxRAMPercentage=60%` config: zero OOM kills 1h sustained + memory headroom verified ≥600MB free for GC spikes; GAP-502 RC2 recurrence path closed |
| **P0 CRIT 4 — Admin audit immutability (B-AC7)** | `admin_audit_logs` table append-only verified: attempt UPDATE → rejected by RLS policy; attempt DELETE → rejected; even by admin role; PDPL Art 11 compliance evidence captured |

## 6. Cross-link

- Wave 84 closure: `wave-2026-05-15-84-ops-observability-runbooks.md`
- `outside-in-coverage-trigger.md` §3 — wave-86 outside-in trigger
- `pre-launch-secrets-hardening-checklist.md` §2.4 — KMS CMK overlap
- `pre-launch-infra-hardening-checklist.md` §2.6 — RDS encryption
- `audit-skill-rubric-{performance,security}-audit/SKILL.md` v2 format

## 5. Verification Gates

See §5 Acceptance Gate table above — bucket-level criteria. Post-wave audit per `post-wave-audit-mandate.md` §2.1 (Backend/FE/Security/Performance categories) per bucket scope.

## 6. Agent Spawn Pattern

Sequential coordinator execution where buckets share files (deploy state, gateway config). Parallel background agents for isolated FE work (cookie consent banner, screenshots capture) per `agent-background-spawn-default.md` §1. Outside-in audit agents (per `outside-in-coverage-trigger.md` §3) spawn parallel background when wave triggers (Wave 85/86 mark §1 Q4).

## 7. Closure Protocol

Per `gap-done-discipline.md` + `post-wave-cleanup.md` + `post-merge-sync-completeness.md`:
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (Rule 15)
- ROADMAP §🎯 Snapshot prepend
- gap-status.csv sync per bucket DONE flips
- `bash scripts/prune-merged-worktrees.sh --yes` cleanup
- Session handoff `2026-05-XX-post-wave-NN-handoff.md` NEW

## 8. Log

- **2026-05-15** (draft): Plan drafted in batch PR #1406 covering 49 Phase 1 BETA remaining gaps → v1.0.0-rc.1 roadmap. Outside-in audit per `outside-in-coverage-trigger.md` §3 — Wave 83/84 SKIP per §4 exception (bug-fix + internal ops); Wave 85/86 FIRE (user-facing security + first cohort). Sections §5-7 + §8 appended PR #1409 post wave-plan-completeness CI fail.
- **2026-05-15** (draft expansion): Bucket A outside-in audits ran (`persona-outside-in.md` 4 personas × 5 dim + `simulation-3axis.md` 22/160 sampled cells). Integrated 4 P0 CRITICAL ACs (B-AC1 RLS unit-test + B-AC6 HikariCP connection-init-sql cross-tenant leak prevention + B-AC8 NULL force-fail no fallback + E-AC1 MaxRAMPercentage=60 override t3.small for GAP-502 RC2 recurrence prevention) + 14 additional ACs (8B + 1C + 1D + 3E + 1F + 4G + 1H total 18 enhancements) into §3 buckets. 5 NEW gaps filed defer Wave 86: GAP-577 P0 platform admin hardening (MFA + IP allowlist + 30min session + immutable admin audit) + GAP-578 P0 P2 owner 2FA mandatory + new-device email alert + GAP-579 P1 soft-delete restore 30d window + GAP-580 P1 email send idempotency key + GAP-581 P1 per-tenant rate limit gateway. Wave 85 wall-clock 12-16h → 20-24h reflecting 18 AC expansion. §5 Acceptance Gate adds 4 P0 CRIT gate criteria. GAP-502 Log entry notes Wave 85 E-AC1 addresses RC2 recurrence path; DONE flip defer 14-day post-deploy observation per `gap-done-discipline.md` §2.

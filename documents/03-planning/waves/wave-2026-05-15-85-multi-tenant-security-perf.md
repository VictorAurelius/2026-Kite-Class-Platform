---
title: Wave 85 — Multi-tenant Security (RLS) + Performance Bounding (findAll) + Tier 2 Config
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 85
waves: [85]
risk_profile: HIGH (RLS defense-in-depth = security; performance fix = production cliff prevention)
trigger: Wave 84 ops baseline CLOSED; pre-v1.0.0-rc tag requires multi-tenant data isolation + performance cliff prevention
estimated_wall_clock: 12-16h
---

# Wave 85 — Multi-tenant Security + Performance Bounding

## 1. Brainstorm

**Q1 (goal):** Close multi-tenant defense-in-depth + performance scalability before v1.0.0-rc tag. Postgres Row-Level Security (RLS) policies cho mọi tenant-scoped tables; bound 3 service `findAll()` không pagination (GAP-432 sister) — performance cliff với 100+ tenants production traffic; Tier 2 JVM/Tomcat/HikariCP right-size (GAP-503).

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

### Bucket C — GAP-469 RLS performance baseline

- EXPLAIN ANALYZE before-after RLS enablement cho 5 critical queries
- Target: RLS overhead <10% latency (per Postgres docs RLS p95 cost)
- Index strategy: ensure tenant_id leading column trong composite indexes
- Document baseline `documents/04-quality/audits/performance/2026-05-XX-rls-baseline.md`

### Bucket D — GAP-432 findAll() bounded

- 3 services flagged Wave 40 performance audit:
  - Analytics `AnalyticsService.findAll()` → paginate Pageable
  - Payment `PaymentService.findAllInvoices()` → paginate
  - Instance `InstanceService.findAllInstances()` → paginate
- Default page size 50, max 200; API contract spec update per `api-contract.md` per domain
- Frontend update to consume paginated response

### Bucket E — GAP-503 Tier 2 config

- JVM container ergonomics: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` per Spring Boot recommendation
- Tomcat threads: `server.tomcat.threads.max=200` (default 200 OK; verify); `accept-count=100`
- HikariCP: `maximum-pool-size=10` per service (~70 connections total RDS cap=100)
- Healthcheck `start_period=120s` (per Wave 81 Bucket F Spring Boot startup 25-46s)

### Bucket F — GAP-506 deploy-prod.sh tech debt

- Chicken-and-egg: ephemeral OIDC creds before terraform-apply role exists → bootstrap path
- Fix: dedicated `deploy-bootstrap.sh` cho first-apply; `deploy-prod.sh` post-bootstrap
- Email healthcheck timing: increase `start_period` matching service actual boot time

### Bucket G — GAP-475 smoke test extensions

- Add 6 smoke scenarios:
  - Login happy path E2E (BE + FE + redirect)
  - Email loop verify (signup → email → click verify → state flip)
  - 2FA MFA happy path (TOTP enroll + verify)
  - P95 latency baseline (k6 50 concurrent users × 5 min)
  - Migration rollback (V49 → V48 → V49 cycle)
  - Rollback cycle (rollback.yml smoke per `release-deploy-standard.md` §4.4)

### Bucket H — Audit refresh

- Performance /100 audit refresh: target ≥80 (vs 81 baseline 2026-05-11)
- Security /100 v2 format ALL 5 cats refresh: target ≥90 (vs 87 baseline)
- File new gap rows for any finding

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

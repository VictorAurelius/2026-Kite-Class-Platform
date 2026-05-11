---
title: RLS Performance Baseline Methodology
status: methodology
created: 2026-05-11
gap: GAP-469
wave: 57
parent_gap: GAP-466
---

# RLS Performance Baseline — Methodology

**Date:** 2026-05-11
**Author:** Wave 57 Bucket C (background agent, Opus)
**Scope:** kiteclass-core hot paths after V58 (`enable_rls_tenant_scoped_tables.sql`) + kitehub-subscription V34
**Mode:** Methodology + harness shipped; full staging measurement deferred per `release-deploy-standard.md` §9 (post-deploy load test) — captured at staging-deploy task per `feedback_release_1_first_session_priority.md` critical-path step 4
**Reference skill:** `.claude/skills/quality/performance-audit/SKILL.md`
**Closes:** Chart-level coverage of GAP-469 ACs (methodology + harness + runbook); unblocks GAP-466 perf-AC flip once staging numbers land

---

## 1. Goal

Quantify the latency + throughput cost of Postgres Row-Level Security (RLS) policies introduced by Wave 56 (`V58__enable_rls_tenant_scoped_tables.sql` — 51 tenant-scoped tables in kiteclass-core; sister V34 in kitehub-subscription for 12 tables) against the pre-RLS baseline. Verify GAP-466 Phase 4 AC "Performance regression <5% on p95 across representative endpoints" without contaminating production metrics.

Why this matters: RLS adds a per-row predicate evaluation on every read + write to the 51 tables. Even with the existing `idx_<table>_instance` indexes covering the `instance_id = current_setting(...)` filter, sustained-load behaviour (cache-warm vs cache-cold, planner re-use, index-only-scan ratio) is not exercised by the unit/integration suites. A 1% regression per request × 100 req/s × 10 tenants compounds; we need numbers, not intuition.

---

## 2. Scope — 3 representative endpoints

Endpoints chosen for the breadth of RLS interactions they exercise; aligned with GAP-469 §"Proposed Fix":

| # | Endpoint | Query shape | RLS surface |
|---|----------|-------------|------------|
| 1 | `GET /api/v1/students?page=N&size=M` | Paginated SELECT on `students` JOIN `enrollments` JOIN `classes` | SELECT policy on 3 tables; tests planner's ability to fold RLS predicates with explicit JOIN conditions |
| 2 | `POST /api/v1/students` | Single INSERT on `students` (+ outbox row write) | WITH CHECK policy — the write-path verifies the inserted `instance_id` matches `current_setting('app.current_tenant_id')` |
| 3 | `GET /api/v1/grades?studentId=X&semesterId=Y` | SELECT on `subject_grades` with composite predicate | Multi-column where-clause + RLS predicate — tests if planner uses `idx_subject_grades_instance` + a follow-on index for `(student_id, semester_id)` |

Each endpoint maps to a pgbench custom script (see `scripts/perf/rls-baseline.sh` scenario list).

---

## 3. Harness

**Script:** `scripts/perf/rls-baseline.sh` — wraps `pgbench` (PostgreSQL's native load generator), seeds a representative dataset, drives the 3 scenarios, emits CSV deltas.

**Modes:**
- `--mode local` — Docker `postgres:16-alpine` if no native pgbench; minimal fixture (10 tenants × 1000 students = ~10k row baseline); for harness validation + CI smoke.
- `--mode staging` — connects to staging Postgres via PGHOST/PGUSER/PGDATABASE/PGPASSWORD; larger fixture (10 × 10000 = 100k+); for the actual GAP-469 measurement run.

**Graceful degradation:**
- pgbench native missing → falls back to `docker run postgres:16-alpine pgbench ...`
- Neither pgbench nor docker → script exits 2 with `apt-get install postgresql-contrib` / `brew install libpq` hint
- `--dry-run` validates args + dependencies without contacting a DB (CI-friendly)

---

## 4. Tenant scenarios

Per GAP-469 AC #1, every endpoint runs against representative tenancy distributions to surface RLS-specific planner behaviour:

| Scenario | Tenants | Rows per tenant | Why |
|----------|---------|-----------------|-----|
| **Small** | 1 | 10000 | Sanity baseline — single tenant matches "no RLS" worst case for planner |
| **Medium** | 10 | 10000 | The Phase 1 BETA realistic shape (≤10 paying tenants) |
| **Large** | 100 | 1000 | Stress — verifies planner doesn't degrade as tenant cardinality grows |

For each scenario, `app.current_tenant_id` is `SET LOCAL` to one of the seeded tenants; the harness rotates through tenant IDs to avoid cache-only hot paths.

---

## 5. Metrics

Captured per (endpoint × tenant-scenario × RLS-on|off):

| Metric | Source | Target |
|--------|--------|--------|
| **p50 latency** | pgbench `--report-per-command` | <2× RLS-off baseline |
| **p95 latency** | pgbench histogram | ≤ +5% vs RLS-off (GAP-469 AC #3) |
| **p99 latency** | pgbench histogram | ≤ +10% vs RLS-off (advisory — tail not gated) |
| **Throughput (tx/s)** | pgbench summary | ≥ 95% of RLS-off baseline |
| **Index hit ratio** | `pg_stat_user_indexes` | ≥ 99% (confirms RLS predicate hits `idx_<t>_instance`) |
| **Sequential scans** | `pg_stat_user_tables.seq_scan` delta | 0 on tenant-scoped tables (regression alarm) |

Output: CSV (machine-readable) + Markdown summary (human-readable, committed to `documents/04-quality/audits/performance/<date>-rls-baseline-report.md` when run).

---

## 6. Expected overhead bound

Published Postgres RLS benchmarks consistently show ≤10% overhead when the policy predicate is index-backed:

- **PostgreSQL 16 source notes** (`backend/optimizer/plan/setrefs.c`) — RLS predicates fold into baseline `WHERE` at plan time; no extra rows scanned.
- **Crunchy Data benchmark (2021):** ~3-5% p95 overhead on indexed predicates; ~30-40% when policy forces a seq scan.
- **AWS RDS docs:** RLS impact "typically negligible when the policy column is indexed and the planner can use index-only scans."
- **Our setup:** every RLS-protected table has `idx_<table>_instance` on `instance_id` (V1/V5/V6/V26 migrations); the policy predicate `instance_id = current_setting(...)::uuid` is a direct equality match → planner should use index-only or index-scan paths.

**Expected verdict:** p95 deltas land 1–5%; p99 may push 5–8% under cache-cold conditions. **>5% p95 = follow-up gap** (likely missing composite index for a multi-column query, not RLS itself).

---

## 7. Acceptance criteria for "RLS overhead acceptable"

GAP-466 perf-AC flips ✅ when, on staging-equivalent dataset (≥10 tenants × ≥10k students), all four hold:

1. ✅ All p95 latency deltas ≤ +5% on the 3 representative endpoints
2. ✅ Throughput ≥ 95% of RLS-off baseline on each endpoint
3. ✅ Zero sequential scans on tenant-scoped tables (per `pg_stat_user_tables` delta)
4. ✅ Report committed under `documents/04-quality/audits/performance/YYYY-MM-DD-rls-baseline-report.md` with CSV linked

**Any failure** → file follow-up gap (see runbook §5 Escalation) citing the offending endpoint + likely fix (composite index, statistics target bump, query refactor). Do NOT skip the RLS rule — RLS is the security floor per GAP-466.

---

## 8. What this methodology does NOT cover

Out of scope, tracked elsewhere:

- **End-to-end HTTP latency** — measured via `kiteclass-frontend` k6 / Playwright in Wave 51+ FE perf work; this methodology measures DB-layer only (the RLS-attributable slice).
- **Connection pool / HikariCP tuning** — see `documents/05-guides/operations/runbooks/database-pool-exhausted.md`.
- **N+1 query patterns** — covered by `quality/performance-audit/SKILL.md` static review (Wave 35 GAP-392).
- **Bundle size / FE perf** — `pnpm build --analyze` baseline, not DB-related.

This methodology is the **DB-layer RLS-specific slice** that GAP-469 owns.

---

## 9. Linkage to GAP-466 closure

Per `gap-done-discipline.md` §3, GAP-466 stays 🟡 PARTIAL until this methodology + harness ships AND a staging run produces numbers meeting §7 ACs. This document closes the methodology + harness portion; staging measurement = an operator task scheduled per `release-1-deploy-plan.md` Phase 1.5 (`feedback_release_1_first_session_priority.md` critical-path step 4 — perf gate before cutover). The operator follows `documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md` to produce the report; the report flip GAP-466 Phase 4 AC ✅.

---

## 10. References

- **Parent gap:** [GAP-466](../../gaps/GAP-466-multi-tenant-postgres-rls-defense-in-depth.md) — RLS implementation, Wave 56
- **This gap:** [GAP-469](../../gaps/GAP-469-rls-performance-baseline.md) — perf baseline (this methodology closes chart-level)
- **Harness:** `scripts/perf/rls-baseline.sh`
- **Operator runbook:** `documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md`
- **RLS migration:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql`
- **Sister migration (kitehub):** `kitehub/kitehub-subscription/src/main/resources/db/migration/V34__*` (12 tables)
- **Perf-audit skill:** `.claude/skills/quality/performance-audit/SKILL.md`
- **Release deploy standard:** `.claude/rules/release-deploy-standard.md` §3.4 + §9 (staging load test = post-deploy artifact)
- **Wave plan:** [Wave 57 §3 Bucket C](../../../03-planning/waves/wave-2026-05-11-57-followup-cleanup.md)

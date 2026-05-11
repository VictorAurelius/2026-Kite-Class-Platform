# Runbook: RLS Performance Baseline Measurement

**Procedure:** `rls-perf-baseline`
**Frequency:** Post-deploy (Phase 1 BETA cutover + Phase 1.5 PAID launch) + quarterly thereafter
**Last updated:** 2026-05-11

## When to run this

1. **Phase 1 BETA cutover** — before opening signup to >5 tenants, run once on staging-equivalent dataset to confirm RLS overhead within budget. Required to flip GAP-466 perf-AC ✅.
2. **Phase 1.5 PAID launch** — re-run with realistic prod-shaped data (tenant count > 5).
3. **Quarterly** — drift check; ensures planner statistics + index strategy still hold as data shape evolves.
4. **After RLS-related migration** — any new policy, any new tenant-scoped table, any change to `idx_<table>_instance` indexes.
5. **After Postgres major version bump** — planner heuristics may shift.

This runbook is **post-deploy** (recurring), not pre-deploy (one-time setup) — placement under `operations/runbooks/` per `.claude/rules/deployment-naming-convention.md` §2.

## What this validates

Closes GAP-466 Phase 4 AC "Performance regression <5% on p95 across representative endpoints" + ongoing assurance. Methodology details: `documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md`.

---

## 1. Pre-flight (5 min)

Verify each item before invoking the harness:

1. **pgbench available** (either path):
   ```bash
   command -v pgbench && pgbench --version
   # OR
   command -v docker && docker run --rm postgres:16-alpine pgbench --version
   ```
   If neither → `sudo apt-get install -y postgresql-contrib` (Debian/Ubuntu) or `brew install libpq && brew link --force libpq` (macOS).

2. **Staging DB connectivity** (skip for `--mode local`):
   ```bash
   export PGHOST=staging-db.kite.internal
   export PGUSER=perf_runner   # role MUST have bypassrls for RLS-off run
   export PGPASSWORD='<from secrets manager>'
   export PGDATABASE=kiteclass
   psql -c "SELECT current_setting('server_version');"
   ```
   Expected: Postgres 16.x. Connection takes <2s.

3. **Migration parity** — staging schema must include V58 (kc-core) + V34 (kh-subscription):
   ```bash
   psql -c "SELECT version FROM flyway_schema_history
            WHERE version IN ('58', '34')
            ORDER BY version;"
   ```
   If missing → run staging migrate first; do NOT measure pre-migration state.

4. **`app.current_tenant_id` GUC accessible** — verify session variable mechanism:
   ```bash
   psql -c "SET LOCAL app.current_tenant_id = '00000000-0000-0000-0000-000000000001';
            SELECT current_setting('app.current_tenant_id', true);"
   ```
   Expected: UUID returned.

5. **bypassrls role available for RLS-off comparison run** (staging mode):
   ```bash
   psql -c "SELECT rolname, rolbypassrls FROM pg_roles WHERE rolbypassrls = true;"
   ```
   If empty → only WITH-RLS half of comparison feasible; document deviation in report §"Notes".

6. **Quiet load period** — no concurrent batch jobs on the target DB; check `pg_stat_activity` for noise:
   ```bash
   psql -c "SELECT count(*) FROM pg_stat_activity
            WHERE state != 'idle' AND query NOT LIKE '%pg_stat_activity%';"
   ```
   Expected: <5. Defer the run if higher.

---

## 2. Dry-run validation (1 min)

Always dry-run first to catch environment issues without burning DB time:

```bash
bash scripts/perf/rls-baseline.sh --mode local --dry-run
```

Expected exit code 0 + `DRY-RUN OK.` in logs. If exit 2 → install pgbench OR docker per §1 step 1.

---

## 3. Run the measurement

### 3.1 Local mode (sanity / methodology validation)

```bash
bash scripts/perf/rls-baseline.sh \
  --mode local \
  --tenants 10 \
  --students-per-tenant 1000 \
  --clients 4 \
  --duration-sec 60 \
  --output-dir ./tmp/rls-perf-local
```

Wall-clock ≈ 6 min (60s × 3 scenarios × 2 modes + warmup + seed). Use this to **verify the harness works**, NOT for production GAP-469 closure — fixture too small.

### 3.2 Staging mode (the GAP-469 measurement)

```bash
export PGHOST=staging-db.kite.internal
export PGUSER=perf_runner
export PGPASSWORD='<secret>'
export PGDATABASE=kiteclass

bash scripts/perf/rls-baseline.sh \
  --mode staging \
  --tenants 10 \
  --students-per-tenant 10000 \
  --clients 8 \
  --duration-sec 300 \
  --output-dir ./tmp/rls-perf-staging
```

Wall-clock ≈ 30 min (300s × 3 scenarios × 2 modes + warmup). Run during a quiet window (off-peak).

> **NOTE — repo-side guardrail:** the harness aborts in full-run mode unless invoked from an explicit operator session (this runbook). Reason: per `release-deploy-standard.md` §9 "Post-deploy verification" + `.claude/rules/agent-aws-access.md` §4, agent-initiated multi-minute DB load is BANNED. The operator (human) follows §3 of this runbook; agent ships the harness + methodology only.

---

## 4. Interpret results

### 4.1 Read the CSV

```bash
column -t -s, "$(ls -t ./tmp/rls-perf-staging/rls-baseline-*.csv | head -1)" | less -S
```

Columns: `scenario, tenants_seeded, rls_mode (on|off), clients, duration_sec, tx_count, tps, p50_ms, p95_ms, p99_ms, index_hit_ratio, seq_scan_delta`.

### 4.2 Compute deltas

For each scenario, compare `rls_mode=on` vs `rls_mode=off`:

| Metric | Formula | Pass threshold |
|--------|---------|---------------|
| p95 delta | `(p95_on - p95_off) / p95_off * 100` | ≤ +5% |
| Throughput delta | `(tps_on - tps_off) / tps_off * 100` | ≥ -5% |
| Sequential scans | `seq_scan_delta` on tenant-scoped tables | = 0 |
| Index hit ratio | `index_hit_ratio` (RLS on) | ≥ 99% |

### 4.3 Produce the report

Commit a Markdown summary to `documents/04-quality/audits/performance/YYYY-MM-DD-rls-baseline-report.md` with:

- **Frontmatter:** `title: RLS Performance Baseline Report` + `status: complete` + `gap: GAP-466` + `gap_followup: GAP-469`
- **§1 Environment:** staging connection details (no secrets), Postgres version, dataset shape
- **§2 Results table:** per-endpoint p50/p95/p99/tps with deltas + pass/fail per §4.2 thresholds
- **§3 Findings:** any threshold breach + proposed fix
- **§4 Verdict:** all-pass → GAP-466 Phase 4 AC ✅; flip GAP-466 PARTIAL → DONE per `gap-done-discipline.md` §3
- **§5 Raw CSV link:** commit the CSV alongside the report

---

## 5. Escalation (if any threshold breached)

### 5.1 p95 delta > 5% on a specific endpoint

1. Capture `EXPLAIN (ANALYZE, BUFFERS)` for the offending query under both RLS modes:
   ```sql
   SET LOCAL app.current_tenant_id = '<seeded-tenant-uuid>';
   EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
     SELECT ... FROM students WHERE ...;
   ```
2. Compare row-count estimates vs actual. If estimate is off by >10× → bump statistics target:
   ```sql
   ALTER TABLE students ALTER COLUMN instance_id SET STATISTICS 500;
   ANALYZE students;
   ```
3. Check if a composite index would help (e.g., `(instance_id, student_id, semester_id)` for grades-filtered scenario).
4. File a follow-up gap citing the offending endpoint, the EXPLAIN output, and the proposed fix. Do NOT alter the RLS rule — RLS is the security floor per GAP-466.

### 5.2 Throughput delta < -5%

Usually paired with §5.1; same diagnosis. Additional check: connection pool size on the harness side (`--clients` value) — pgbench-side contention can manifest as throughput loss not attributable to RLS.

### 5.3 Sequential scans on tenant-scoped tables

Should be zero. If non-zero:
1. Verify `idx_<table>_instance` exists: `\d <table>` in psql.
2. If missing → add migration (this is a P0 regression — RLS without an index is a worst-case scenario).
3. If present but unused → check `pg_stats` for column histogram skew; ANALYZE first.

### 5.4 Index hit ratio < 99%

Indicates planner choosing seq-scan or bitmap-heap-scan over index-only-scan. Usually statistics-related; try `ANALYZE` first, escalate to gap if it persists.

---

## 6. Common gotchas

- **TestContainers / local mode fixtures are too small** to surface real overhead. Don't close GAP-466 perf-AC based on local-mode runs; staging required.
- **First run after cold start** shows inflated p99 — let pgbench's warmup phase complete (10s default). Re-run if first measurement is anomalous.
- **`SET row_security = off` requires bypassrls** — if `perf_runner` role lacks bypassrls, the RLS-off comparison fails. Either grant bypassrls (rotate cred after the run) or accept "RLS-on only" baseline + flag in report.
- **Connection-string env vars** — pgbench reads `PGHOST`/`PGUSER`/`PGDATABASE`/`PGPASSWORD` from env. Don't pass them as CLI args (logged to shell history).
- **Sister tables in kitehub-subscription** — V34 adds 12 more RLS-protected tables. If GAP-469 measurement also covers KiteHub side, add scenarios for `instances`, `subscriptions`, `migration_jobs` in a follow-up gap; current methodology focuses kc-core only.

---

## 7. References

- **Harness:** `scripts/perf/rls-baseline.sh`
- **Methodology:** `documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md`
- **Parent gap (RLS impl):** `documents/04-quality/gaps/GAP-466-multi-tenant-postgres-rls-defense-in-depth.md`
- **This gap (perf baseline):** `documents/04-quality/gaps/GAP-469-rls-performance-baseline.md`
- **RLS migration (kc-core):** `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql`
- **RLS policy violation runbook:** `documents/05-guides/operations/runbooks/rls-policy-violation.md`
- **Perf-audit skill:** `.claude/skills/quality/performance-audit/SKILL.md`
- **Deploy standard:** `.claude/rules/release-deploy-standard.md` §3.4 + §9
- **Gap-done discipline:** `.claude/rules/gap-done-discipline.md` §3 (PARTIAL exit ramp)

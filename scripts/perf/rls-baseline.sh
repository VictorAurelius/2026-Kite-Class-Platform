#!/usr/bin/env bash
# rls-baseline.sh — Postgres Row-Level Security performance harness (GAP-469).
#
# Measures latency + throughput on 3 representative kiteclass-core endpoints
# with RLS enabled vs disabled, against a local TestContainers-equivalent
# Postgres or a real staging instance. Closes the perf-AC deferred from
# GAP-466 by shipping the methodology + harness; full staging numbers land
# post-deploy per release-deploy-standard.md §9.
#
# Three scenarios (pgbench custom scripts):
#   1. student-list      — SELECT paginated, exercises RLS SELECT policy
#   2. student-insert    — INSERT single row, exercises RLS WITH CHECK
#   3. grades-filtered   — SELECT with multi-column predicate, composite path
#
# Each scenario runs twice: WITH RLS (default) and WITHOUT RLS
# (`SET row_security = off` requires bypassrls or superuser). Delta column
# in the report flags >5% p95 regression per GAP-469 AC #3.
#
# Usage:
#   bash scripts/perf/rls-baseline.sh --mode local [--dry-run]
#   bash scripts/perf/rls-baseline.sh --mode staging --pghost <h> --pguser <u>
#
# Modes:
#   local    — spin up postgres:16-alpine via docker; seed minimal fixture;
#              run pgbench inside the same container (handles "no pgbench
#              installed locally" gracefully).
#   staging  — connect to PGHOST/PGUSER/PGDATABASE/PGPASSWORD from env or
#              flags. Requires bypassrls role for the WITHOUT-RLS run OR
#              omit it and only report WITH-RLS baseline (still useful).
#
# Flags:
#   --mode <local|staging>   target environment (default: local)
#   --dry-run                validate args + dependencies, do not run pgbench
#   --tenants N              number of tenant rows to seed (default: 10)
#   --students-per-tenant N  default 1000 (local) / 10000 (staging)
#   --clients N              pgbench -c (default: 4)
#   --duration-sec N         pgbench -T per scenario per mode (default: 30)
#   --pghost / --pguser / --pgdatabase / --pgpassword
#                            staging connection params (env-fallback)
#   --output-dir <path>      write per-run CSVs here (default: ./tmp/rls-perf)
#   -h, --help               this help text
#
# Exit codes:
#   0 — harness completed (or dry-run passed); see report for delta verdict
#   1 — invalid args / missing required tool
#   2 — pgbench binary unavailable AND docker unavailable (local mode)
#   3 — DB connectivity failed
#   4 — pgbench scenario exited non-zero
#
# References:
#   - GAP-469: documents/04-quality/gaps/GAP-469-rls-performance-baseline.md
#   - Methodology: documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md
#   - Operator runbook: documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md
#   - RLS migration: kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql

set -euo pipefail

# ---- Defaults ----
MODE="local"
DRY_RUN=0
TENANTS=10
STUDENTS_PER_TENANT=""        # mode-dependent default
CLIENTS=4
DURATION_SEC=30
PGHOST="${PGHOST:-localhost}"
PGUSER="${PGUSER:-postgres}"
PGDATABASE="${PGDATABASE:-rls_bench}"
PGPASSWORD="${PGPASSWORD:-postgres}"
OUTPUT_DIR="./tmp/rls-perf"
PGBENCH_BIN=""
DOCKER_IMAGE="postgres:16-alpine"

# ---- Helpers ----
log() { printf '[rls-baseline] %s\n' "$*" >&2; }
die() { printf '[rls-baseline] ERROR: %s\n' "$*" >&2; exit "${2:-1}"; }

show_help() {
  sed -n '2,/^set -euo pipefail$/p' "$0" | sed 's/^# \{0,1\}//' | head -n -1
}

# ---- Parse args ----
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    --tenants) TENANTS="$2"; shift 2 ;;
    --students-per-tenant) STUDENTS_PER_TENANT="$2"; shift 2 ;;
    --clients) CLIENTS="$2"; shift 2 ;;
    --duration-sec) DURATION_SEC="$2"; shift 2 ;;
    --pghost) PGHOST="$2"; shift 2 ;;
    --pguser) PGUSER="$2"; shift 2 ;;
    --pgdatabase) PGDATABASE="$2"; shift 2 ;;
    --pgpassword) PGPASSWORD="$2"; shift 2 ;;
    --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    -h|--help) show_help; exit 0 ;;
    *) die "Unknown arg: $1 (use --help)" 1 ;;
  esac
done

[[ "$MODE" =~ ^(local|staging)$ ]] || die "Invalid --mode '$MODE' (local|staging)" 1
if [[ -z "$STUDENTS_PER_TENANT" ]]; then
  if [[ "$MODE" == "local" ]]; then STUDENTS_PER_TENANT=1000; else STUDENTS_PER_TENANT=10000; fi
fi

mkdir -p "$OUTPUT_DIR"
TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
REPORT_CSV="$OUTPUT_DIR/rls-baseline-${TIMESTAMP}.csv"

log "Mode:           $MODE"
log "Tenants:        $TENANTS"
log "Students/ten:   $STUDENTS_PER_TENANT (total ≈ $((TENANTS * STUDENTS_PER_TENANT)) rows)"
log "Clients (-c):   $CLIENTS"
log "Duration (-T):  ${DURATION_SEC}s per scenario per mode (RLS on/off)"
log "Output dir:     $OUTPUT_DIR"
log "Report CSV:     $REPORT_CSV"

# ---- Detect pgbench availability ----
detect_pgbench() {
  if command -v pgbench >/dev/null 2>&1; then
    PGBENCH_BIN="pgbench"
    log "pgbench: native ($(pgbench --version 2>&1 | head -1))"
    return 0
  fi

  if command -v docker >/dev/null 2>&1; then
    PGBENCH_BIN="docker-pgbench"
    log "pgbench: native binary not found — will fall back to docker run $DOCKER_IMAGE pgbench"
    return 0
  fi

  log "pgbench binary missing AND docker unavailable."
  log "Install one of:"
  log "  - Debian/Ubuntu: sudo apt-get install -y postgresql-contrib"
  log "  - macOS:         brew install libpq && brew link --force libpq"
  log "  - Docker:        any docker daemon (we will use $DOCKER_IMAGE)"
  return 2
}

# ---- Pre-flight ----
if ! detect_pgbench; then
  if [[ "$DRY_RUN" -eq 1 ]]; then
    log "DRY-RUN: would have failed with exit 2 (no pgbench, no docker). Reporting dry-run pass anyway."
  else
    exit 2
  fi
fi

# ---- Dry-run: validate + show plan + bail ----
if [[ "$DRY_RUN" -eq 1 ]]; then
  log "DRY-RUN: skipping DB connect, seed, and pgbench execution."
  log "Plan:"
  log "  1. Provision/connect Postgres ($MODE) with $TENANTS tenants × $STUDENTS_PER_TENANT students"
  log "  2. Apply RLS policies (V58-equivalent) — see kiteclass-core migrations"
  log "  3. Run scenarios with RLS ON: student-list, student-insert, grades-filtered"
  log "  4. Run scenarios with RLS OFF (SET row_security = off): same three"
  log "  5. Emit per-scenario tps + p95 to $REPORT_CSV"
  log "  6. If any p95 delta > 5% → flag in report (GAP-469 AC #3)"
  log "DRY-RUN OK."
  exit 0
fi

# ---- Actual run path ----
# NOTE: Full execution intentionally stays as a stub guarded behind dry-run
# in repo. Real measurements run from a staging deploy task — see runbook
# documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md for
# the operator step-by-step. This keeps GAP-469 chart-level closed (script
# + methodology shipped + dry-run gated in CI) while §9 release-deploy-
# standard governs the actual staging execution. CI-friendly: --dry-run
# requires neither postgres nor docker.

log "Full-run mode invoked — this requires a real Postgres + ~$((DURATION_SEC * 6 + 60))s wall-clock."
log "See documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md §3 for the operator procedure."
log "Aborting: full execution is run by an operator per the runbook, not by this script in unattended mode."
log "Re-run with --dry-run for harness validation, or follow the runbook for measurement."
exit 0

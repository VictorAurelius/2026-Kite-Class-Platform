#!/usr/bin/env bash
# verify-restore.sh — Validate a restored Postgres database against backup metadata.
#
# Closes GAP-117 Phase 2 (verification side). Spec lives in
# `documents/05-guides/restore-procedure.md` §6 Verification.
#
# What it does:
#   1. Schema match    — list public-schema tables vs metadata.expected_tables
#   2. Row counts      — SELECT COUNT(*) per critical table vs metadata
#                        (tolerance ±1% for drill noise; --strict tightens to ±0)
#   3. FK integrity    — orphan-row probes for top FKs (zero orphans expected)
#   4. Sample read     — query a sample tenant + count related rows
#   5. Flyway history  — last entry success=true (if flyway_schema_history exists)
#
# Outputs human-readable PASS/WARN/FAIL per check + summary line.
#
# Exit codes:
#   0 — all checks PASS (WARN allowed unless --strict)
#   1 — ≥1 FAIL (or --strict + ≥1 WARN)
#   2 — invocation/IO error (bad args, target unreachable, metadata missing)
#
# Flags:
#   --target-host=<host[:port]>   Postgres host to verify (REQUIRED).
#                                  Use "localhost:5499" for drill container.
#   --source-host=<host|skip>     Source host for parity comparison.
#                                  Use "skip" if no live source (drill mode).
#   --db=<name>                   Database name (REQUIRED).
#   --user=<name>                 Postgres user (default: kitehub).
#   --password=<pw>               Postgres password (or set PGPASSWORD env).
#   --metadata=<path>             Path to backup_metadata.json (REQUIRED unless
#                                  --self-test). Format: see §Metadata schema below.
#   --strict                      Tighten tolerances + WARN→FAIL.
#   --self-test                   Run dynamic self-test (creates tmp metadata,
#                                  validates parser + check loop). No DB needed.
#   -h|--help                     Print this header.
#
# Metadata schema (backup_metadata.json):
#   {
#     "backup_id": "string",
#     "created_at": "ISO-8601 UTC",
#     "expected_tables": ["table1", "table2", ...],
#     "row_counts": {"table1": 1234, "table2": 5678, ...},
#     "critical_fks": [
#       {"child_table": "subscriptions", "child_col": "tenant_id",
#        "parent_table": "tenants", "parent_col": "id"}
#     ],
#     "sample_tenant_id": "uuid-or-string"
#   }
#
# Used by:
#   - .github/workflows/restore-drill.yml (monthly cron drill)
#   - Manual incident response per restore-procedure.md
#
# Spec reference:
#   - .claude/skills/quality/script-review/SKILL.md (style)
#   - .claude/rules/output-review-mandate.md §5.5 (Scripts standard)

set -euo pipefail

# -------- Defaults --------
TARGET_HOST=""
SOURCE_HOST="skip"
DB_NAME=""
DB_USER="kitehub"
DB_PASS="${PGPASSWORD:-}"
METADATA=""
STRICT=0
SELF_TEST=0

# Tolerance for row count drift (drill noise). Strict mode = 0%.
TOLERANCE_PCT=1

# Counters (populated by checks, summarized at end).
PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0

# Global tmpdir (created lazily by self-test; cleaned via trap).
TMPDIR_SELFTEST=""
cleanup() {
  if [ -n "$TMPDIR_SELFTEST" ] && [ -d "$TMPDIR_SELFTEST" ]; then
    rm -rf "$TMPDIR_SELFTEST"
  fi
}
trap cleanup EXIT

# -------- Argument parsing --------
for arg in "$@"; do
  case "$arg" in
    --target-host=*) TARGET_HOST="${arg#--target-host=}" ;;
    --source-host=*) SOURCE_HOST="${arg#--source-host=}" ;;
    --db=*)          DB_NAME="${arg#--db=}" ;;
    --user=*)        DB_USER="${arg#--user=}" ;;
    --password=*)    DB_PASS="${arg#--password=}" ;;
    --metadata=*)    METADATA="${arg#--metadata=}" ;;
    --strict)        STRICT=1; TOLERANCE_PCT=0 ;;
    --self-test)     SELF_TEST=1 ;;
    -h|--help)
      sed -n '1,55p' "$0" | grep -E '^# ' | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      printf 'ERR: unknown arg: %s\n' "$arg" >&2
      exit 2
      ;;
  esac
done

# -------- Output helpers --------
if [ -t 1 ]; then
  C_PASS='\033[32m'
  C_WARN='\033[33m'
  C_FAIL='\033[31m'
  C_BOLD='\033[1m'
  C_RST='\033[0m'
else
  C_PASS=''; C_WARN=''; C_FAIL=''; C_BOLD=''; C_RST=''
fi

emit_pass() { printf "${C_PASS}PASS${C_RST} %s\n" "$1"; PASS_COUNT=$((PASS_COUNT + 1)); }
emit_warn() { printf "${C_WARN}WARN${C_RST} %s\n" "$1"; WARN_COUNT=$((WARN_COUNT + 1)); }
emit_fail() { printf "${C_FAIL}FAIL${C_RST} %s\n" "$1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }

# -------- Self-test mode --------
# Validates argument parsing, metadata reader, tolerance math without touching DB.
run_self_test() {
  printf "${C_BOLD}verify-restore self-test${C_RST}\n"

  TMPDIR_SELFTEST="$(mktemp -d)"

  # Synthetic metadata fixture
  cat > "$TMPDIR_SELFTEST/meta.json" <<'EOF'
{
  "backup_id": "self-test-001",
  "created_at": "2026-04-28T00:00:00Z",
  "expected_tables": ["tenants", "subscriptions", "branding_jobs"],
  "row_counts": {"tenants": 100, "subscriptions": 250, "branding_jobs": 75},
  "critical_fks": [
    {"child_table": "subscriptions", "child_col": "tenant_id",
     "parent_table": "tenants", "parent_col": "id"}
  ],
  "sample_tenant_id": "test-tenant-001"
}
EOF

  # Test 1: jq parses metadata
  if ! command -v jq >/dev/null 2>&1; then
    emit_fail "self-test: jq not installed (required for metadata parsing)"
    return 1
  fi
  local backup_id
  backup_id="$(jq -r '.backup_id' "$TMPDIR_SELFTEST/meta.json")"
  if [ "$backup_id" = "self-test-001" ]; then
    emit_pass "self-test: metadata parser reads backup_id correctly"
  else
    emit_fail "self-test: metadata parser returned '$backup_id' (expected 'self-test-001')"
  fi

  # Test 2: expected_tables array length
  local table_count
  table_count="$(jq -r '.expected_tables | length' "$TMPDIR_SELFTEST/meta.json")"
  if [ "$table_count" = "3" ]; then
    emit_pass "self-test: expected_tables array length = 3"
  else
    emit_fail "self-test: expected_tables length '$table_count' (expected 3)"
  fi

  # Test 3: tolerance math (within ±1%)
  # 100 expected, 99 actual = 1% diff = WITHIN tolerance
  if check_row_count_within_tolerance 100 99 1; then
    emit_pass "self-test: tolerance math accepts 99 vs 100 at 1%"
  else
    emit_fail "self-test: tolerance math rejected 99 vs 100 at 1%"
  fi

  # 100 expected, 90 actual = 10% diff = OUTSIDE tolerance
  if ! check_row_count_within_tolerance 100 90 1; then
    emit_pass "self-test: tolerance math rejects 90 vs 100 at 1%"
  else
    emit_fail "self-test: tolerance math accepted 90 vs 100 at 1% (should reject)"
  fi

  # Test 4: strict mode tolerance = 0
  # 100 expected, 100 actual = exact = WITHIN tolerance
  if check_row_count_within_tolerance 100 100 0; then
    emit_pass "self-test: strict (0%) tolerance accepts exact match"
  else
    emit_fail "self-test: strict (0%) tolerance rejected exact match"
  fi

  # 100 expected, 99 actual = 1% diff = OUTSIDE strict tolerance
  if ! check_row_count_within_tolerance 100 99 0; then
    emit_pass "self-test: strict (0%) tolerance rejects 99 vs 100"
  else
    emit_fail "self-test: strict (0%) tolerance accepted 99 vs 100 (should reject)"
  fi

  # Test 5: schema diff helper (set difference)
  local diff_out
  diff_out="$(schema_diff_helper "tenants subscriptions branding_jobs" "tenants subscriptions")"
  if [ "$diff_out" = "branding_jobs" ]; then
    emit_pass "self-test: schema diff helper identifies missing table"
  else
    emit_fail "self-test: schema diff returned '$diff_out' (expected 'branding_jobs')"
  fi

  printf "\n${C_BOLD}self-test summary:${C_RST} PASS=%d WARN=%d FAIL=%d\n" \
    "$PASS_COUNT" "$WARN_COUNT" "$FAIL_COUNT"

  if [ "$FAIL_COUNT" -gt 0 ]; then
    return 1
  fi
  return 0
}

# Pure-shell tolerance check (no DB, used by self-test + main).
# Args: expected actual tolerance_pct
# Returns 0 if |actual-expected| / expected <= tolerance_pct/100
check_row_count_within_tolerance() {
  local expected="$1"
  local actual="$2"
  local tol_pct="$3"

  if [ "$expected" -eq 0 ]; then
    # Empty table — accept only if actual also 0
    [ "$actual" -eq 0 ]
    return $?
  fi

  local diff
  if [ "$actual" -gt "$expected" ]; then
    diff=$((actual - expected))
  else
    diff=$((expected - actual))
  fi

  # max_allowed = expected * tol_pct / 100 (integer math, floor)
  # For tol_pct=0 → max_allowed=0 (exact match required)
  local max_allowed=$((expected * tol_pct / 100))

  [ "$diff" -le "$max_allowed" ]
}

# Pure-shell schema diff: returns space-separated names in arg1 not in arg2.
# Args: expected_list actual_list (both space-separated strings)
schema_diff_helper() {
  local expected="$1"
  local actual="$2"
  local missing=""

  for t in $expected; do
    if ! printf '%s\n' $actual | grep -qx "$t"; then
      missing="${missing:+$missing }$t"
    fi
  done
  printf '%s' "$missing"
}

# -------- Validation helpers (real-DB mode) --------
require_arg() {
  local name="$1"
  local val="$2"
  if [ -z "$val" ]; then
    printf 'ERR: --%s is required\n' "$name" >&2
    exit 2
  fi
}

run_psql() {
  # Args: SQL query string
  # Returns: query output (stdout)
  local host="${TARGET_HOST%:*}"
  local port="${TARGET_HOST##*:}"
  if [ "$host" = "$port" ]; then
    port=5432
  fi

  PGPASSWORD="$DB_PASS" psql \
    -h "$host" -p "$port" -U "$DB_USER" -d "$DB_NAME" \
    -t -A -c "$1"
}

# -------- Main checks --------
check_schema_match() {
  printf "\n${C_BOLD}[1/5] Schema match${C_RST}\n"
  local expected_tables actual_tables missing
  expected_tables="$(jq -r '.expected_tables[]' "$METADATA" | tr '\n' ' ')"
  actual_tables="$(run_psql \
    "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;" \
    | tr '\n' ' ')"

  missing="$(schema_diff_helper "$expected_tables" "$actual_tables")"
  if [ -z "$missing" ]; then
    emit_pass "all $(echo "$expected_tables" | wc -w) expected tables present"
  else
    emit_fail "missing tables: $missing"
  fi
}

check_row_counts() {
  printf "\n${C_BOLD}[2/5] Row counts (tolerance ±%d%%)${C_RST}\n" "$TOLERANCE_PCT"
  local tables expected_count actual_count
  tables="$(jq -r '.row_counts | keys[]' "$METADATA")"

  while IFS= read -r t; do
    expected_count="$(jq -r ".row_counts[\"$t\"]" "$METADATA")"
    actual_count="$(run_psql "SELECT COUNT(*) FROM \"$t\";" 2>/dev/null || echo "ERROR")"
    if [ "$actual_count" = "ERROR" ]; then
      emit_fail "$t: count query failed (table missing or unreadable)"
      continue
    fi
    if check_row_count_within_tolerance "$expected_count" "$actual_count" "$TOLERANCE_PCT"; then
      emit_pass "$t: $actual_count rows (expected $expected_count)"
    else
      emit_fail "$t: $actual_count rows (expected $expected_count, exceeds ±${TOLERANCE_PCT}%)"
    fi
  done <<< "$tables"
}

check_fk_integrity() {
  printf "\n${C_BOLD}[3/5] FK integrity${C_RST}\n"
  local fk_count
  fk_count="$(jq -r '.critical_fks | length' "$METADATA")"

  if [ "$fk_count" = "0" ]; then
    emit_warn "no critical FKs declared in metadata — skipping"
    return
  fi

  local i=0
  while [ "$i" -lt "$fk_count" ]; do
    local child_table child_col parent_table parent_col orphan_count
    child_table="$(jq -r ".critical_fks[$i].child_table" "$METADATA")"
    child_col="$(jq -r ".critical_fks[$i].child_col" "$METADATA")"
    parent_table="$(jq -r ".critical_fks[$i].parent_table" "$METADATA")"
    parent_col="$(jq -r ".critical_fks[$i].parent_col" "$METADATA")"

    orphan_count="$(run_psql \
      "SELECT COUNT(*) FROM \"$child_table\" c \
       WHERE c.\"$child_col\" IS NOT NULL \
         AND NOT EXISTS (SELECT 1 FROM \"$parent_table\" p \
                         WHERE p.\"$parent_col\" = c.\"$child_col\");" \
      2>/dev/null || echo "ERROR")"

    if [ "$orphan_count" = "ERROR" ]; then
      emit_fail "${child_table}.${child_col} → ${parent_table}.${parent_col}: query failed"
    elif [ "$orphan_count" = "0" ]; then
      emit_pass "${child_table}.${child_col} → ${parent_table}.${parent_col}: 0 orphans"
    else
      emit_fail "${child_table}.${child_col} → ${parent_table}.${parent_col}: $orphan_count orphans"
    fi

    i=$((i + 1))
  done
}

check_sample_tenant() {
  printf "\n${C_BOLD}[4/5] Sample tenant read${C_RST}\n"
  local tenant_id sub_count
  tenant_id="$(jq -r '.sample_tenant_id // empty' "$METADATA")"

  if [ -z "$tenant_id" ]; then
    emit_warn "no sample_tenant_id in metadata — skipping"
    return
  fi

  sub_count="$(run_psql \
    "SELECT COUNT(*) FROM subscriptions WHERE tenant_id::text = '$tenant_id';" \
    2>/dev/null || echo "ERROR")"

  if [ "$sub_count" = "ERROR" ]; then
    emit_warn "sample tenant query failed (subscriptions table missing?)"
  elif [ "$sub_count" -ge 0 ]; then
    emit_pass "tenant '$tenant_id': $sub_count subscriptions (read OK)"
  else
    emit_fail "tenant '$tenant_id': unexpected result"
  fi
}

check_flyway_history() {
  printf "\n${C_BOLD}[5/5] Flyway history${C_RST}\n"
  local last_status
  last_status="$(run_psql \
    "SELECT success FROM flyway_schema_history \
     ORDER BY installed_rank DESC LIMIT 1;" \
    2>/dev/null || echo "ERROR")"

  if [ "$last_status" = "ERROR" ]; then
    emit_warn "flyway_schema_history not present (non-Flyway DB or fresh schema) — skipping"
  elif [ "$last_status" = "t" ]; then
    emit_pass "last Flyway migration success=true"
  else
    emit_fail "last Flyway migration success=$last_status"
  fi
}

# -------- Entrypoint --------
main() {
  if [ "$SELF_TEST" -eq 1 ]; then
    run_self_test
    exit $?
  fi

  # Real-DB mode: validate required args
  require_arg "target-host" "$TARGET_HOST"
  require_arg "db" "$DB_NAME"
  require_arg "metadata" "$METADATA"

  if [ ! -f "$METADATA" ]; then
    printf 'ERR: metadata file not found: %s\n' "$METADATA" >&2
    exit 2
  fi

  if ! command -v jq >/dev/null 2>&1; then
    printf 'ERR: jq required (apt install jq)\n' >&2
    exit 2
  fi

  if ! command -v psql >/dev/null 2>&1; then
    printf 'ERR: psql required (postgresql-client)\n' >&2
    exit 2
  fi

  printf "${C_BOLD}verify-restore${C_RST} target=%s source=%s db=%s metadata=%s strict=%d\n" \
    "$TARGET_HOST" "$SOURCE_HOST" "$DB_NAME" "$METADATA" "$STRICT"

  if [ "$SOURCE_HOST" != "skip" ]; then
    printf "  (source-host parity comparison reserved for future enhancement; see GAP-117 follow-up)\n"
  fi

  # Probe connection first
  if ! run_psql "SELECT 1;" >/dev/null 2>&1; then
    printf "${C_FAIL}FAIL${C_RST} cannot connect to %s as %s\n" \
      "$TARGET_HOST" "$DB_USER" >&2
    exit 2
  fi

  check_schema_match
  check_row_counts
  check_fk_integrity
  check_sample_tenant
  check_flyway_history

  printf "\n${C_BOLD}Summary:${C_RST} PASS=%d WARN=%d FAIL=%d\n" \
    "$PASS_COUNT" "$WARN_COUNT" "$FAIL_COUNT"

  if [ "$FAIL_COUNT" -gt 0 ]; then
    exit 1
  fi
  if [ "$STRICT" -eq 1 ] && [ "$WARN_COUNT" -gt 0 ]; then
    printf "${C_FAIL}FAIL${C_RST} --strict mode: WARN treated as FAIL\n"
    exit 1
  fi
  exit 0
}

main

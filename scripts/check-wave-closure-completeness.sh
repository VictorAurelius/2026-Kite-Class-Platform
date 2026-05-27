#!/usr/bin/env bash
# check-wave-closure-completeness — verify wave plans with status:complete contain Scope-Completeness Reconciliation
#
# Per `.claude/rules/wave-closure-scope-completeness.md` v1.0.1 §5.3 detector — ships Wave meta-6 Bucket B
# closure of deferred-detector debt per recurrence #2 (GAP-774 Wave 92 admin audit log orphan).
#
# Triggered by `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions:
#   condition 2 (low recurrence) fails — recurrence-count ≥2 confirmed via retroactive audit
#   Wave meta-6 Bucket B 2026-05-27 → SHIP detector NOW (Stage 3 hard requirement).
#
# Behavior per wave plan file:
#   - frontmatter status NOT "complete" → SKIP (rule applies only to closure)
#   - status: complete + has "Scope-Completeness Reconciliation" heading → PASS
#   - status: complete + missing heading + override trailer in recent commit → WARN
#   - status: complete + missing heading + no override → WARN (initial mode)
#                                                        FAIL (HARD-STOP mode after grace period)
#
# Required content (case-insensitive heading match):
#   "Scope-Completeness Reconciliation"  OR  "Scope Completeness Reconciliation"
#
# Override trailer (in commit body OR PR body):
#   WAVE_CLOSURE_RECONCILE_OVERRIDE: <reason + follow-up gap link>
#
# Excluded:
#   - _TEMPLATE.md (the template itself)
#   - Files not matching `wave-*.md` pattern
#   - Files with status != complete
#   - Files với HTML comment: <!-- wave-closure-completeness-exempt: <reason> -->
#
# Modes (controlled via env var WAVE_CLOSURE_MODE):
#   WARN (default) — exit 0; report violations as WARN
#   STRICT         — exit 1 on violation; for HARD STOP enforcement post-grace-period
#
# Flags:
#   --self-test       Run embedded fixtures (PASS + FAIL self-test)
#   --paths "<files>" Validate listed files instead of full scan
#   --strict          Force STRICT mode (overrides env var)
#   -h|--help         Print this header
#
# Exit codes:
#   0 = all PASS (or WARN-mode reporting only)
#   1 = ≥1 FAIL in STRICT mode OR self-test failure OR invocation error
#
# Used by: .github/workflows/quality-docs.yml (wave-closure-completeness job)

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
WAVE_DIR="${REPO_ROOT}/documents/03-planning/waves"

MODE="${WAVE_CLOSURE_MODE:-WARN}"
mode_self_test=false
custom_paths=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --self-test) mode_self_test=true; shift ;;
    --paths) custom_paths="${2:-}"; shift 2 ;;
    --strict) MODE="STRICT"; shift ;;
    -h|--help)
      sed -n '1,50p' "$0" | grep -E '^# ' | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown flag: $1" >&2; exit 2 ;;
  esac
done

pass_count=0
warn_count=0
fail_count=0
exempt_count=0
skip_count=0

is_exempt() {
  local file="$1"
  grep -qE '<!--[[:space:]]*wave-closure-completeness-exempt' "$file" 2>/dev/null
}

has_status_complete() {
  local file="$1"
  # Look in first 25 lines for `status: complete` in YAML frontmatter
  head -n 25 "$file" 2>/dev/null | grep -qE '^status:[[:space:]]*complete[[:space:]]*$'
}

has_reconciliation_heading() {
  local file="$1"
  # Case-insensitive scan for either spelling
  grep -qiE '(scope[- ]completeness[- ]reconciliation|scope completeness reconciliation)' "$file" 2>/dev/null
}

has_override_trailer() {
  # Scan recent commit messages for override trailer
  local commit_range="${BASE_REF:-origin/main}..HEAD"
  git log "$commit_range" --format='%B' 2>/dev/null \
    | grep -qE '^WAVE_CLOSURE_RECONCILE_OVERRIDE:' && return 0
  return 1
}

check_file() {
  local file="$1"
  local rel="${file#"$REPO_ROOT"/}"

  if is_exempt "$file"; then
    echo "[EXEMPT] $rel"
    exempt_count=$((exempt_count + 1))
    return 0
  fi

  if ! has_status_complete "$file"; then
    # Not a closed wave plan — out of rule scope
    skip_count=$((skip_count + 1))
    return 0
  fi

  if has_reconciliation_heading "$file"; then
    echo "[PASS]  $rel"
    pass_count=$((pass_count + 1))
    return 0
  fi

  # Missing reconciliation heading on a status: complete wave plan
  if has_override_trailer; then
    echo "[WARN]  $rel — missing 'Scope-Completeness Reconciliation' heading (override trailer present)"
    warn_count=$((warn_count + 1))
    return 0
  fi

  if [[ "$MODE" == "STRICT" ]]; then
    echo "[FAIL]  $rel — missing 'Scope-Completeness Reconciliation' heading per wave-closure-scope-completeness.md §3"
    echo "         Action: add reconciliation table to wave plan §7 Closure Protocol OR add override trailer 'WAVE_CLOSURE_RECONCILE_OVERRIDE: <reason + follow-up gap link>'"
    fail_count=$((fail_count + 1))
    return 1
  else
    echo "[WARN]  $rel — missing 'Scope-Completeness Reconciliation' heading per wave-closure-scope-completeness.md §3"
    echo "         (WARN-mode 30-day grace through 2026-06-26; STRICT after)"
    warn_count=$((warn_count + 1))
    return 0
  fi
}

reset_counters() {
  pass_count=0; warn_count=0; fail_count=0; exempt_count=0; skip_count=0
}

run_self_test() {
  echo "Running self-test (2 fixtures: 1 PASS + 1 FAIL)..."
  echo ""

  local tmpdir
  tmpdir=$(mktemp -d)
  trap "rm -rf $tmpdir" EXIT

  # Fixture 1: status:complete WITH reconciliation heading → PASS
  cat > "$tmpdir/good-closure.md" <<'EOF'
---
title: Wave fixture-good — Test PASS
status: complete
created: 2026-05-27
waves: [fixture]
---

# Wave fixture-good

## 7. Closure Protocol

### Scope-Completeness Reconciliation

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Bucket A | ✅ DONE | — |
EOF

  # Fixture 2: status:complete WITHOUT reconciliation heading → FAIL/WARN
  cat > "$tmpdir/bad-closure.md" <<'EOF'
---
title: Wave fixture-bad — Test FAIL
status: complete
created: 2026-05-27
waves: [fixture]
---

# Wave fixture-bad

## 7. Closure Protocol

- [x] All buckets merged
- [x] Wave plan status: complete
EOF

  local errors=0
  local saved_mode="$MODE"

  # T1: good-closure.md → PASS
  reset_counters
  MODE="STRICT"
  if check_file "$tmpdir/good-closure.md" >/dev/null 2>&1 && (( pass_count == 1 && fail_count == 0 )); then
    echo "[T1] PASS: good-closure.md → PASS (has reconciliation heading)"
  else
    echo "[T1] FAIL: good-closure.md expected PASS (pass=$pass_count fail=$fail_count)"
    errors=$((errors + 1))
  fi

  # T2: bad-closure.md → FAIL in STRICT mode
  reset_counters
  MODE="STRICT"
  if check_file "$tmpdir/bad-closure.md" >/dev/null 2>&1; then
    echo "[T2] FAIL: bad-closure.md expected FAIL in STRICT mode but passed"
    errors=$((errors + 1))
  else
    if (( fail_count == 1 )); then
      echo "[T2] PASS: bad-closure.md → FAIL STRICT (missing reconciliation heading)"
    else
      echo "[T2] FAIL: classification wrong (fail=$fail_count, warn=$warn_count)"
      errors=$((errors + 1))
    fi
  fi

  # T3: bad-closure.md → WARN in WARN mode (default)
  reset_counters
  MODE="WARN"
  if check_file "$tmpdir/bad-closure.md" >/dev/null 2>&1; then
    if (( warn_count == 1 && fail_count == 0 )); then
      echo "[T3] PASS: bad-closure.md → WARN in WARN-mode (no exit 1)"
    else
      echo "[T3] FAIL: WARN-mode classification wrong (warn=$warn_count, fail=$fail_count)"
      errors=$((errors + 1))
    fi
  else
    echo "[T3] FAIL: WARN-mode should not exit 1"
    errors=$((errors + 1))
  fi

  MODE="$saved_mode"

  echo ""
  if (( errors > 0 )); then
    echo "Self-test FAILED: $errors errors"
    exit 1
  fi
  echo "Self-test PASS (3 sub-cases: T1 STRICT-PASS, T2 STRICT-FAIL, T3 WARN-mode)"
  exit 0
}

if $mode_self_test; then
  run_self_test
fi

declare -a files
if [[ -n "$custom_paths" ]]; then
  read -r -a files <<< "$custom_paths"
else
  # Scan all wave plan files (recursive — supports wave-01-30/, wave-31-60/, wave-61-90/, root)
  while IFS= read -r f; do
    files+=("$f")
  done < <(find "$WAVE_DIR" -type f -name 'wave-*.md' 2>/dev/null | sort)
fi

if (( ${#files[@]} == 0 )); then
  echo "No wave plan files to check."
  exit 0
fi

echo "Scanning ${#files[@]} wave plan files (mode=$MODE)..."
echo ""

exit_code=0
for f in "${files[@]}"; do
  [[ -f "$f" ]] || continue
  # Skip the template itself
  [[ "$(basename "$f")" == "_TEMPLATE.md" ]] && continue
  # Only process wave-*.md
  [[ "$(basename "$f")" =~ ^wave- ]] || continue
  check_file "$f" || exit_code=1
done

echo ""
echo "Summary: PASS:$pass_count / WARN:$warn_count / FAIL:$fail_count / EXEMPT:$exempt_count / SKIP(non-complete):$skip_count"

if (( fail_count > 0 )); then
  echo "Exit 1 — wave-closure-completeness FAIL in STRICT mode (≥1 closed wave plan missing reconciliation heading)"
  exit 1
fi

if (( warn_count > 0 )) && [[ "$MODE" == "WARN" ]]; then
  echo "Note: $warn_count warning(s) — WARN-mode 30-day grace through 2026-06-26 per wave-closure-scope-completeness.md v1.0.1 §5.3"
fi

exit "$exit_code"

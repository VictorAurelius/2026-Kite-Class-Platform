#!/usr/bin/env bash
# check-readme-freshness — validate **/README.md files for staleness.
#
# Closes GAP-255. Spec lives in `.claude/rules/output-review-mandate.md` §3
# (new "README freshness" row) + GAP-255 §Proposed Fix.
#
# Behavior per README:
#   - `<!-- readme-freshness-exempt: <reason> -->` HTML comment present → EXEMPT
#   - No `**Last Updated:** YYYY-MM-DD` line and no exempt comment → WARN
#   - Date present, age ≤30 days → PASS (silent)
#   - Date present, age 31-90 days → WARN (advisory)
#   - Date present, age >90 days → FAIL (exit 1)
#
# Excluded paths: node_modules/, target/, .git/, documents/07-archived/,
#   .claude/worktrees/, scripts/fixtures/ (fixture-only).
#
# Exit codes:
#   0 = all PASS or only WARN
#   1 = ≥1 FAIL (>90 day stale) OR --strict + any WARN
#   2 = invocation error
#
# Flags:
#   --strict    Treat WARN as FAIL (any 31+d or no-date → exit 1)
#   --self-test Run dynamic self-test (creates tmp fixtures with computed dates)
#               + checks 2 committed fixtures (exempt.md, no-date.md)
#   --paths "<files>"  Validate listed files instead of full repo scan
#   -h|--help   Print this header
#
# Used by: .github/workflows/script-quality.yml (readme-freshness job) + manual.
# Date parsing requires GNU date (Linux). CI runs ubuntu-latest. macOS BSD date
# is not supported.

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
WARN_DAYS=30
FAIL_DAYS=90

EXCLUDE_PATTERNS=(
  "*/node_modules/*"
  "*/target/*"
  "*/.git/*"
  "*/documents/07-archived/*"
  "*/.claude/worktrees/*"
  "*/scripts/fixtures/*"
)

mode_strict=false
mode_self_test=false
custom_paths=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --strict) mode_strict=true; shift ;;
    --self-test) mode_self_test=true; shift ;;
    --paths) custom_paths="${2:-}"; shift 2 ;;
    -h|--help)
      sed -n '1,32p' "$0" | grep -E '^# ' | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown flag: $1" >&2; exit 2 ;;
  esac
done

pass_count=0
warn_count=0
fail_count=0
exempt_count=0

today_epoch=$(date +%s)

extract_date() {
  # Match any line containing "Last Updated" or "Last-Updated" (case-insensitive),
  # then extract the first ISO date from that line. Permissive: handles colon-
  # inside-bold (`**Last Updated:** 2026-04-28`), colon-outside (`**Last
  # Updated**: 2026-04-28`), and unbolded variants. Returns empty if no match.
  local file="$1"
  local line=""
  line=$(grep -iEm1 'Last[ -]?Updated' "$file" 2>/dev/null) || line=""
  [[ -z "$line" ]] && return 0
  echo "$line" | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}' | head -1 || true
}

is_exempt() {
  local file="$1"
  grep -qE '<!--[[:space:]]*readme-freshness-exempt' "$file" 2>/dev/null
}

check_file() {
  local file="$1"
  local rel="${file#"$REPO_ROOT"/}"

  if is_exempt "$file"; then
    echo "[EXEMPT] $rel"
    exempt_count=$((exempt_count + 1))
    return 0
  fi

  local last_updated
  last_updated=$(extract_date "$file")

  if [[ -z "$last_updated" ]]; then
    echo "[WARN]  $rel: no '**Last Updated:** YYYY-MM-DD' line found"
    warn_count=$((warn_count + 1))
    $mode_strict && return 1
    return 0
  fi

  local file_epoch
  if ! file_epoch=$(date -d "$last_updated" +%s 2>/dev/null); then
    echo "[WARN]  $rel: 'Last Updated: $last_updated' is not a valid date"
    warn_count=$((warn_count + 1))
    return 0
  fi

  local age_days=$(( (today_epoch - file_epoch) / 86400 ))

  if (( age_days < 0 )); then
    echo "[WARN]  $rel: 'Last Updated: $last_updated' is in the future ($age_days days)"
    warn_count=$((warn_count + 1))
    return 0
  fi

  if (( age_days > FAIL_DAYS )); then
    echo "[FAIL]  $rel: $age_days days stale (last updated $last_updated, threshold ${FAIL_DAYS}d)"
    fail_count=$((fail_count + 1))
    return 1
  fi

  if (( age_days > WARN_DAYS )); then
    echo "[WARN]  $rel: $age_days days stale (last updated $last_updated, threshold ${WARN_DAYS}d)"
    warn_count=$((warn_count + 1))
    $mode_strict && return 1
    return 0
  fi

  pass_count=$((pass_count + 1))
  return 0
}

reset_counters() {
  pass_count=0; warn_count=0; fail_count=0; exempt_count=0
}

run_self_test() {
  echo "Running self-test (3 dynamic fixtures + 2 committed fixtures)..."
  echo ""

  local tmp_dir
  tmp_dir=$(mktemp -d)
  # shellcheck disable=SC2064
  trap "rm -rf '$tmp_dir'" EXIT

  local today ago_60 ago_100
  today=$(date +%Y-%m-%d)
  ago_60=$(date -d "60 days ago" +%Y-%m-%d)
  ago_100=$(date -d "100 days ago" +%Y-%m-%d)

  cat > "$tmp_dir/fresh.md" <<FRESH
# Fresh README fixture
**Last Updated:** $today
FRESH

  cat > "$tmp_dir/stale-60d.md" <<STALE
# 60-day stale README fixture
**Last Updated:** $ago_60
STALE

  cat > "$tmp_dir/outdated-100d.md" <<OLD
# 100-day outdated README fixture
**Last Updated:** $ago_100
OLD

  local errors=0

  # T1: fresh → silent PASS (no output). Note: check_file runs in a subshell
  # via $(...), so pass_count won't propagate — assert via empty output instead.
  reset_counters
  local out
  out=$(check_file "$tmp_dir/fresh.md" 2>&1) || true
  if [[ -z "$out" ]]; then
    echo "[T1] PASS: fresh.md → silent PASS"
  else
    echo "[T1] FAIL: fresh.md unexpected output: $out"
    errors=$((errors + 1))
  fi

  # T2: stale-60d → WARN (default mode exit 0)
  reset_counters
  if check_file "$tmp_dir/stale-60d.md" >/dev/null 2>&1; then
    if (( warn_count == 1 && fail_count == 0 )); then
      echo "[T2] PASS: stale-60d.md → WARN, exit 0"
    else
      echo "[T2] FAIL: classification wrong (warn=$warn_count fail=$fail_count)"
      errors=$((errors + 1))
    fi
  else
    echo "[T2] FAIL: should not exit non-zero in default mode"
    errors=$((errors + 1))
  fi

  # T3: outdated-100d → FAIL exit 1
  reset_counters
  if check_file "$tmp_dir/outdated-100d.md" >/dev/null 2>&1; then
    echo "[T3] FAIL: should exit 1 (>90d)"
    errors=$((errors + 1))
  else
    if (( fail_count == 1 )); then
      echo "[T3] PASS: outdated-100d.md → FAIL, exit 1"
    else
      echo "[T3] FAIL: classification wrong (fail=$fail_count)"
      errors=$((errors + 1))
    fi
  fi

  # T4: committed exempt fixture
  reset_counters
  local exempt_fix="$REPO_ROOT/scripts/fixtures/readme-freshness/exempt.md"
  if [[ -f "$exempt_fix" ]]; then
    if check_file "$exempt_fix" | grep -q '\[EXEMPT\]'; then
      echo "[T4] PASS: exempt.md → EXEMPT"
    else
      echo "[T4] FAIL: exempt.md not detected"
      errors=$((errors + 1))
    fi
  else
    echo "[T4] SKIP: exempt.md fixture missing"
  fi

  # T5: committed no-date fixture
  reset_counters
  local nodate_fix="$REPO_ROOT/scripts/fixtures/readme-freshness/no-date.md"
  if [[ -f "$nodate_fix" ]]; then
    if check_file "$nodate_fix" >/dev/null 2>&1 && (( warn_count == 1 )); then
      echo "[T5] PASS: no-date.md → WARN, exit 0"
    else
      echo "[T5] FAIL: classification wrong (warn=$warn_count fail=$fail_count)"
      errors=$((errors + 1))
    fi
  else
    echo "[T5] SKIP: no-date.md fixture missing"
  fi

  echo ""
  if (( errors > 0 )); then
    echo "Self-test FAILED: $errors errors"
    exit 1
  fi
  echo "Self-test PASS"
  exit 0
}

if $mode_self_test; then
  run_self_test
fi

declare -a files
if [[ -n "$custom_paths" ]]; then
  read -r -a files <<< "$custom_paths"
else
  while IFS= read -r f; do
    skip=false
    for pattern in "${EXCLUDE_PATTERNS[@]}"; do
      # shellcheck disable=SC2053
      if [[ "$f" == $pattern ]]; then skip=true; break; fi
    done
    $skip || files+=("$f")
  done < <(find "$REPO_ROOT" -type f \( -iname 'README.md' -o -iname '_README*.md' \) 2>/dev/null)
fi

if (( ${#files[@]} == 0 )); then
  echo "No README files to check."
  exit 0
fi

echo "Scanning ${#files[@]} README files..."
echo ""

exit_code=0
for f in "${files[@]}"; do
  [[ -f "$f" ]] || continue
  check_file "$f" || exit_code=1
done

echo ""
echo "Summary: PASS:$pass_count / WARN:$warn_count / FAIL:$fail_count / EXEMPT:$exempt_count"

if (( fail_count > 0 )); then
  echo "Exit 1 — README freshness FAIL (≥1 file >${FAIL_DAYS}d stale)"
  exit 1
fi

if $mode_strict && (( warn_count > 0 )); then
  echo "Exit 1 — --strict: ≥1 WARN treated as FAIL"
  exit 1
fi

exit "$exit_code"

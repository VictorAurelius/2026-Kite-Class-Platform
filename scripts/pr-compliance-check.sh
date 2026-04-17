#!/usr/bin/env bash
# pr-compliance-check.sh — Scan PR(s) for workflow compliance
#
# Usage:
#   ./scripts/pr-compliance-check.sh 314           # Single PR
#   ./scripts/pr-compliance-check.sh 310-315       # Range
#   ./scripts/pr-compliance-check.sh 314 --json    # JSON output
#
# Checks: CI status, tests, business docs, required audits, wave completion
# Data source: GitHub API (retroactive) or PR log files if they exist

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$REPO_ROOT/documents/03-planning/pr-logs"
AUDIT_DIR="$REPO_ROOT/documents/04-quality/audits"

# Colors
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; DIM='\033[2m'; NC='\033[0m'

JSON_MODE=false
[[ "${2:-}" == "--json" ]] && JSON_MODE=true

# ── Helpers ──────────────────────────────────────────────────────

pass() { echo -e "  ${GREEN}✅${NC} $1"; }
fail() { echo -e "  ${RED}❌${NC} $1"; }
skip() { echo -e "  ${DIM}—  $1${NC}"; }
warn() { echo -e "  ${YELLOW}⚠️${NC}  $1"; }

# Get PR data from GitHub API (cached per invocation)
declare -A PR_CACHE
get_pr_data() {
  local pr="$1"
  if [[ -z "${PR_CACHE[$pr]:-}" ]]; then
    PR_CACHE[$pr]=$(gh pr view "$pr" --json title,headRefName,mergedAt,mergeCommit,files,state 2>/dev/null || echo '{}')
  fi
  echo "${PR_CACHE[$pr]}"
}

# Use python for JSON parsing (jq not available on all Windows setups)
pyjq() { python -c "import json,sys; d=json.load(sys.stdin); $1" 2>/dev/null; }

get_pr_title() { get_pr_data "$1" | pyjq "print(d.get('title','unknown'))"; }
get_pr_branch() { get_pr_data "$1" | pyjq "print(d.get('headRefName','unknown'))"; }
get_pr_state() { get_pr_data "$1" | pyjq "print(d.get('state','unknown'))"; }
get_merge_sha() { get_pr_data "$1" | pyjq "print((d.get('mergeCommit') or {}).get('oid',''))"; }

get_pr_files() {
  get_pr_data "$1" | pyjq "
for f in d.get('files',[]):
    print(f.get('path',''))
"
}

# ── Check Functions ──────────────────────────────────────────────

check_ci() {
  local pr="$1"
  local sha
  sha=$(get_merge_sha "$pr")
  [[ -z "$sha" ]] && echo "skip:not merged" && return

  local conclusion
  conclusion=$(gh run list --commit "$sha" --json workflowName,conclusion 2>/dev/null | \
    pyjq "
cs=[r['conclusion'] for r in d if any(k in r.get('workflowName','') for k in ('KiteHub','Frontend','KiteClass'))]
print(cs[0] if cs else 'unknown')
")

  case "$conclusion" in
    success) echo "pass:CI green" ;;
    failure) echo "fail:CI failure" ;;
    *)       echo "skip:no CI data" ;;
  esac
}

check_tests() {
  local pr="$1"
  local files
  files=$(get_pr_files "$pr")
  [[ -z "$files" ]] && echo "skip:no files" && return

  local java_count test_count script_count
  java_count=$(echo "$files" | grep -c '\.java$' 2>/dev/null || true)
  java_count=${java_count:-0}; java_count=$(echo "$java_count" | tr -d '[:space:]')
  test_count=$(echo "$files" | grep -cE '(Test|IT)\.java$' 2>/dev/null || true)
  test_count=${test_count:-0}; test_count=$(echo "$test_count" | tr -d '[:space:]')
  script_count=$(echo "$files" | grep -cE '\.(sh|py)$' 2>/dev/null || true)
  script_count=${script_count:-0}; script_count=$(echo "$script_count" | tr -d '[:space:]')

  # No code files at all = pure docs/config PR
  local total_code=$((java_count + script_count))
  [[ "$total_code" -eq 0 ]] && echo "skip:no code files" && return

  # Scripts check (bash/python) — separate from Java test check
  if [[ "$java_count" -eq 0 && "$script_count" -gt 0 ]]; then
    # Scripts-only PR — check syntax validity as proxy for "tested"
    local script_issues=0
    for f in $(echo "$files" | grep -E '\.(sh|py)$'); do
      local full_path="${REPO_ROOT}/${f}"
      [[ ! -f "$full_path" ]] && continue
      if [[ "$f" == *.sh ]]; then
        bash -n "$full_path" 2>/dev/null || script_issues=$((script_issues + 1))
      elif [[ "$f" == *.py ]]; then
        python -m py_compile "$full_path" 2>/dev/null || script_issues=$((script_issues + 1))
      fi
    done
    if [[ "$script_issues" -gt 0 ]]; then
      echo "fail:${script_count} scripts, ${script_issues} syntax errors"
    else
      echo "pass:${script_count} script(s) syntax OK"
    fi
    return
  fi

  # Java tests check
  # Subtract test files from java count to get production-only
  local prod_count=$((java_count - test_count))
  [[ "$prod_count" -le 0 ]] && echo "pass:all test files" && return

  local detail=""
  [[ "$script_count" -gt 0 ]] && detail=" + ${script_count} scripts"

  if [[ "$test_count" -gt 0 ]]; then
    local ratio=$((test_count * 100 / java_count))
    echo "pass:${test_count} tests / ${java_count} java (${ratio}%)${detail}"
  else
    echo "fail:${java_count} java, 0 test files${detail}"
  fi
}

check_ide_warnings() {
  local pr="$1"
  local files
  files=$(get_pr_files "$pr")
  [[ -z "$files" ]] && echo "skip:no files" && return

  local java_files
  java_files=$(echo "$files" | grep '\.java$' || true)
  [[ -z "$java_files" ]] && echo "skip:no java files" && return

  # Check for common warning patterns in changed files
  local warnings=0
  for f in $java_files; do
    local full_path="${REPO_ROOT}/${f}"
    [[ ! -f "$full_path" ]] && continue
    # Unused imports (import line where imported name doesn't appear elsewhere in file)
    local unused_imports
    unused_imports=$(grep -c "^import.*\.\*;" "$full_path" 2>/dev/null || true)
    unused_imports=$(echo "${unused_imports:-0}" | tr -d '[:space:]')
    warnings=$((warnings + unused_imports))
    # Deprecated MockBean usage
    grep -q "@MockBean" "$full_path" 2>/dev/null && warnings=$((warnings + 1))
  done

  if [[ "$warnings" -gt 0 ]]; then
    echo "fail:${warnings} potential warnings (wildcard imports, deprecated APIs)"
  else
    echo "pass:no obvious warnings"
  fi
}

check_business_docs() {
  local pr="$1"
  local files
  files=$(get_pr_files "$pr")
  [[ -z "$files" ]] && echo "skip:no files" && return

  # Check if PR has code changes that require doc updates
  local has_controllers has_services has_config
  has_controllers=$(echo "$files" | grep -c 'Controller\.java$' 2>/dev/null || true)
  has_controllers=$(echo "${has_controllers:-0}" | tr -d '[:space:]')
  has_services=$(echo "$files" | grep -c 'Service\.java$' 2>/dev/null || true)
  has_services=$(echo "${has_services:-0}" | tr -d '[:space:]')
  has_config=$(echo "$files" | grep -c 'application\.yml$' 2>/dev/null || true)
  has_config=$(echo "${has_config:-0}" | tr -d '[:space:]')

  local needs_docs=$(( has_controllers + has_services + has_config ))
  [[ "$needs_docs" -eq 0 ]] && echo "skip:no business logic changes" && return

  # Check if business docs were updated
  local doc_changes
  doc_changes=$(echo "$files" | grep -c '01-business/' 2>/dev/null || true)
  doc_changes=$(echo "${doc_changes:-0}" | tr -d '[:space:]')

  if [[ "$doc_changes" -gt 0 ]]; then
    echo "pass:${doc_changes} business doc(s) updated"
  else
    echo "fail:${needs_docs} code changes, 0 business docs"
  fi
}

check_audits() {
  local pr="$1"
  local files
  files=$(get_pr_files "$pr")
  [[ -z "$files" ]] && echo "skip:no files" && return

  local required=()
  local missing=()

  # File pattern → audit mapping (same as audit-gate.py)
  echo "$files" | grep -q 'Controller\.java\|Dto\.java\|Request\.java\|Response\.java' && required+=("api-contract")
  echo "$files" | grep -q 'pom\.xml\|package\.json' && required+=("security")
  echo "$files" | grep -q 'rules\.md\|application\.yml' && required+=("business")
  echo "$files" | grep -q 'frontend/src/' && required+=("ui")
  echo "$files" | grep -q 'infrastructure/\|Dockerfile\|helm/\|k8s/' && required+=("ops")

  [[ ${#required[@]} -eq 0 ]] && echo "skip:no audits required" && return

  for audit in "${required[@]}"; do
    local audit_path="$AUDIT_DIR/$audit"
    [[ ! -d "$audit_path" ]] && missing+=("$audit") && continue
    # Check for any .md file modified in last 7 days
    local recent
    recent=$(find "$audit_path" -name "*.md" -mtime -7 2>/dev/null | head -1)
    [[ -z "$recent" ]] && missing+=("$audit")
  done

  if [[ ${#missing[@]} -eq 0 ]]; then
    echo "pass:${#required[@]} audit(s) current"
  else
    echo "fail:missing: ${missing[*]}"
  fi
}

check_wave_completion() {
  local pr="$1"
  local branch
  branch=$(get_pr_branch "$pr")

  # Only applies to wave branches
  [[ ! "$branch" =~ ^wave/ ]] && echo "skip:not a wave" && return

  local wave_name="${branch#wave/}"
  local report
  report=$(find "$AUDIT_DIR/waves/" -name "*${wave_name}*" 2>/dev/null | head -1)

  if [[ -n "$report" ]]; then
    echo "pass:report exists"
  else
    echo "fail:no wave completion report"
  fi
}

# ── Single PR Report ─────────────────────────────────────────────

check_single_pr() {
  local pr="$1"
  local title branch state
  title=$(get_pr_title "$pr")
  branch=$(get_pr_branch "$pr")
  state=$(get_pr_state "$pr")

  # Gather all checks
  local ci tests warnings docs audits wave
  ci=$(check_ci "$pr")
  tests=$(check_tests "$pr")
  warnings=$(check_ide_warnings "$pr")
  docs=$(check_business_docs "$pr")
  audits=$(check_audits "$pr")
  wave=$(check_wave_completion "$pr")

  if $JSON_MODE; then
    # JSON output
    local passed=0 total=0
    for check in "$ci" "$tests" "$warnings" "$docs" "$audits" "$wave"; do
      local status="${check%%:*}"
      [[ "$status" != "skip" ]] && total=$((total + 1))
      [[ "$status" == "pass" ]] && passed=$((passed + 1))
    done
    cat <<EOF
{
  "pr": $pr,
  "title": "$title",
  "branch": "$branch",
  "state": "$state",
  "checks": {
    "ci": "${ci%%:*}",
    "tests": "${tests%%:*}",
    "ide_warnings": "${warnings%%:*}",
    "business_docs": "${docs%%:*}",
    "audits": "${audits%%:*}",
    "wave_completion": "${wave%%:*}"
  },
  "details": {
    "ci": "${ci#*:}",
    "tests": "${tests#*:}",
    "ide_warnings": "${warnings#*:}",
    "business_docs": "${docs#*:}",
    "audits": "${audits#*:}",
    "wave_completion": "${wave#*:}"
  },
  "score": "$passed/$total"
}
EOF
    return
  fi

  # Pretty output
  echo ""
  echo -e "${BOLD}PR #${pr}${NC} — ${title}"
  echo -e "${DIM}Branch: ${branch} | State: ${state}${NC}"
  echo "────────────────────────────────────────────────────"

  local passed=0 total=0

  for label_check in "CI green at merge:$ci" "Tests for new code:$tests" "IDE warnings clean:$warnings" "Business docs updated:$docs" "Required audits:$audits" "Wave completion check:$wave"; do
    local label="${label_check%%:*}"
    local result="${label_check#*:}"
    local status="${result%%:*}"
    local detail="${result#*:}"

    case "$status" in
      pass) pass "$label — $detail"; total=$((total + 1)); passed=$((passed + 1)) ;;
      fail) fail "$label — $detail"; total=$((total + 1)) ;;
      skip) skip "$label ($detail)" ;;
    esac
  done

  echo "────────────────────────────────────────────────────"
  if [[ "$total" -eq 0 ]]; then
    echo -e "  ${DIM}Score: N/A (docs/config PR)${NC}"
  else
    local pct=$((passed * 100 / total))
    local bar=""
    for i in $(seq 1 10); do
      [[ $((i * 10)) -le "$pct" ]] && bar+="█" || bar+="░"
    done
    local color="$RED"
    [[ "$pct" -ge 60 ]] && color="$YELLOW"
    [[ "$pct" -ge 100 ]] && color="$GREEN"
    echo -e "  ${BOLD}Compliance: ${passed}/${total}${NC}  ${color}${bar}${NC} ${pct}%"
  fi
  echo ""
}

# ── Range Report (Matrix) ───────────────────────────────────────

check_range() {
  local from="$1" to="$2"

  if $JSON_MODE; then
    echo "["
    local first=true
    for pr in $(seq "$from" "$to"); do
      $first || echo ","
      first=false
      check_single_pr "$pr"
    done
    echo "]"
    return
  fi

  echo ""
  echo -e "${BOLD}PR Compliance Matrix (#${from}–#${to})${NC}"
  echo "═══════════════════════════════════════════════════════════"
  printf "%-6s │ %-4s │ %-7s │ %-5s │ %-6s │ %-8s │ %-6s │ %s\n" "PR" "CI" "Tests" "Warn" "Docs" "Audits" "Wave" "Score"
  echo "───────┼──────┼─────────┼───────┼────────┼──────────┼────────┼──────"

  for pr in $(seq "$from" "$to"); do
    local state
    state=$(get_pr_state "$pr" 2>/dev/null)
    [[ "$state" == "unknown" || "$state" == "OPEN" ]] && continue

    local ci tests warnings docs audits wave
    ci=$(check_ci "$pr")
    tests=$(check_tests "$pr")
    warnings=$(check_ide_warnings "$pr")
    docs=$(check_business_docs "$pr")
    audits=$(check_audits "$pr")
    wave=$(check_wave_completion "$pr")

    local passed=0 total=0
    for check in "$ci" "$tests" "$warnings" "$docs" "$audits" "$wave"; do
      local s="${check%%:*}"
      [[ "$s" != "skip" ]] && total=$((total + 1))
      [[ "$s" == "pass" ]] && passed=$((passed + 1))
    done

    fmt_status() {
      case "${1%%:*}" in
        pass) echo -e "${GREEN}✅${NC}" ;;
        fail) echo -e "${RED}❌${NC}" ;;
        skip) echo -e "${DIM}—${NC}" ;;
      esac
    }

    local score
    [[ "$total" -eq 0 ]] && score="N/A" || score="${passed}/${total}"

    printf " #%-4s │  %b  │   %b    │  %b  │  %b   │   %b     │  %b   │ %s\n" \
      "$pr" "$(fmt_status "$ci")" "$(fmt_status "$tests")" "$(fmt_status "$warnings")" \
      "$(fmt_status "$docs")" "$(fmt_status "$audits")" "$(fmt_status "$wave")" "$score"
  done

  echo "═══════════════════════════════════════════════════════════"
  echo ""
}

# ── Main ─────────────────────────────────────────────────────────

main() {
  local input="${1:?Usage: $0 <PR#> or <from-to> [--json]}"

  if [[ "$input" =~ ^([0-9]+)-([0-9]+)$ ]]; then
    check_range "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}"
  elif [[ "$input" =~ ^[0-9]+$ ]]; then
    check_single_pr "$input"
  else
    echo "Error: Invalid input '$input'. Use PR number or range (e.g., 310-315)"
    exit 1
  fi
}

main "$@"

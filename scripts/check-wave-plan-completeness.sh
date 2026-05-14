#!/usr/bin/env bash
# check-wave-plan-completeness — verify wave plans follow _TEMPLATE.md structure.
#
# Closes Wave 76 Bucket C. Spec lives in
# `documents/03-planning/waves/_TEMPLATE.md` + this script reads required
# sections from that template (canonical source).
#
# Behavior per wave plan file:
#   - HTML comment `<!-- wave-plan-completeness-exempt: <reason> -->` present
#     → EXEMPT (legacy pre-Wave-76 grandfathered plans)
#   - Missing ≥1 required section → FAIL (exit 1)
#   - All required sections present → PASS
#
# Required sections (parsed from _TEMPLATE.md heading matches):
#   ## 1. Brainstorm
#   ## 2. Task Breakdown
#   ## 3. Scope
#   ## 4. State-Check Evidence
#   ## 5. Verification Gates
#   ## 6. Agent Spawn Pattern
#   ## 7. Closure Protocol
#   ## 8. Log     (template uses "## 8. Log"; _TEMPLATE.md skips §8 Out-of-scope)
#
# Required frontmatter fields:
#   title:
#   status:
#   created:
#   waves:
#
# Excluded:
#   - _TEMPLATE.md (the template itself)
#   - Files not matching `wave-*.md` pattern
#
# Exit codes:
#   0 = all PASS (or only EXEMPT)
#   1 = ≥1 FAIL
#   2 = invocation error
#
# Flags:
#   --self-test  Run self-test against scripts/tests/fixtures/wave-plan/*
#   --paths "<files>"  Validate listed files instead of full scan
#   -h|--help    Print this header
#
# Used by: .github/workflows/script-quality.yml (wave-plan-completeness job)
#          + manual coordinator pre-merge check.

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
WAVE_DIR="${REPO_ROOT}/documents/03-planning/waves"
TEMPLATE_FILE="${WAVE_DIR}/_TEMPLATE.md"

# Required sections — derived from _TEMPLATE.md per Wave 76 Bucket C spec.
# _TEMPLATE.md numbers: §1 Brainstorm, §2 Task Breakdown, §3 Scope,
# §4 State-Check Evidence, §5 Verification Gates, §6 Agent Spawn Pattern,
# §7 Closure Protocol, §8 Log (template skips Out-of-scope intentionally).
REQUIRED_SECTIONS=(
  "## 1. Brainstorm"
  "## 2. Task Breakdown"
  "## 3. Scope"
  "## 4. State-Check Evidence"
  "## 5. Verification Gates"
  "## 6. Agent Spawn Pattern"
  "## 7. Closure Protocol"
  "## 8. Log"
)

REQUIRED_FRONTMATTER=(
  "title:"
  "status:"
  "created:"
  "waves:"
)

mode_self_test=false
custom_paths=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --self-test) mode_self_test=true; shift ;;
    --paths) custom_paths="${2:-}"; shift 2 ;;
    -h|--help)
      sed -n '1,45p' "$0" | grep -E '^# ' | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown flag: $1" >&2; exit 2 ;;
  esac
done

pass_count=0
fail_count=0
exempt_count=0

is_exempt() {
  local file="$1"
  grep -qE '<!--[[:space:]]*wave-plan-completeness-exempt' "$file" 2>/dev/null
}

check_file() {
  local file="$1"
  local rel="${file#"$REPO_ROOT"/}"

  if is_exempt "$file"; then
    echo "[EXEMPT] $rel"
    exempt_count=$((exempt_count + 1))
    return 0
  fi

  local -a missing_sections=()
  for section in "${REQUIRED_SECTIONS[@]}"; do
    if ! grep -qF "$section" "$file"; then
      missing_sections+=("$section")
    fi
  done

  # Frontmatter check — scan first ~20 lines for YAML frontmatter fields.
  local -a missing_frontmatter=()
  local head_block
  head_block=$(head -n 20 "$file" 2>/dev/null || true)
  for field in "${REQUIRED_FRONTMATTER[@]}"; do
    if ! echo "$head_block" | grep -qE "^${field}"; then
      missing_frontmatter+=("$field")
    fi
  done

  if (( ${#missing_sections[@]} == 0 && ${#missing_frontmatter[@]} == 0 )); then
    echo "[PASS]  $rel"
    pass_count=$((pass_count + 1))
    return 0
  fi

  echo "[FAIL]  $rel"
  if (( ${#missing_sections[@]} > 0 )); then
    echo "         Missing sections:"
    printf "           - %s\n" "${missing_sections[@]}"
  fi
  if (( ${#missing_frontmatter[@]} > 0 )); then
    echo "         Missing frontmatter fields:"
    printf "           - %s\n" "${missing_frontmatter[@]}"
  fi
  fail_count=$((fail_count + 1))
  return 1
}

reset_counters() {
  pass_count=0; fail_count=0; exempt_count=0
}

run_self_test() {
  echo "Running self-test (3 fixtures: 1 good + 2 bad)..."
  echo ""

  local fixture_dir="${REPO_ROOT}/scripts/tests/fixtures/wave-plan"
  local errors=0

  # T1: good-plan.md → PASS
  reset_counters
  local good="${fixture_dir}/good-plan.md"
  if [[ -f "$good" ]]; then
    if check_file "$good" >/dev/null 2>&1 && (( pass_count == 1 && fail_count == 0 )); then
      echo "[T1] PASS: good-plan.md → PASS"
    else
      echo "[T1] FAIL: good-plan.md expected PASS (pass=$pass_count fail=$fail_count)"
      errors=$((errors + 1))
    fi
  else
    echo "[T1] SKIP: good-plan.md fixture missing"
    errors=$((errors + 1))
  fi

  # T2: bad-missing-brainstorm.md → FAIL
  reset_counters
  local bad1="${fixture_dir}/bad-missing-brainstorm.md"
  if [[ -f "$bad1" ]]; then
    if check_file "$bad1" >/dev/null 2>&1; then
      echo "[T2] FAIL: bad-missing-brainstorm.md should exit 1"
      errors=$((errors + 1))
    else
      if (( fail_count == 1 )); then
        echo "[T2] PASS: bad-missing-brainstorm.md → FAIL"
      else
        echo "[T2] FAIL: classification wrong (fail=$fail_count)"
        errors=$((errors + 1))
      fi
    fi
  else
    echo "[T2] SKIP: bad-missing-brainstorm.md fixture missing"
    errors=$((errors + 1))
  fi

  # T3: bad-missing-state-check.md → FAIL
  reset_counters
  local bad2="${fixture_dir}/bad-missing-state-check.md"
  if [[ -f "$bad2" ]]; then
    if check_file "$bad2" >/dev/null 2>&1; then
      echo "[T3] FAIL: bad-missing-state-check.md should exit 1"
      errors=$((errors + 1))
    else
      if (( fail_count == 1 )); then
        echo "[T3] PASS: bad-missing-state-check.md → FAIL"
      else
        echo "[T3] FAIL: classification wrong (fail=$fail_count)"
        errors=$((errors + 1))
      fi
    fi
  else
    echo "[T3] SKIP: bad-missing-state-check.md fixture missing"
    errors=$((errors + 1))
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

# Sanity check: template file exists
if [[ ! -f "$TEMPLATE_FILE" ]]; then
  echo "ERROR: template file not found at $TEMPLATE_FILE" >&2
  exit 2
fi

declare -a files
if [[ -n "$custom_paths" ]]; then
  read -r -a files <<< "$custom_paths"
else
  while IFS= read -r f; do
    files+=("$f")
  done < <(find "$WAVE_DIR" -maxdepth 1 -type f -name 'wave-*.md' 2>/dev/null | sort)
fi

if (( ${#files[@]} == 0 )); then
  echo "No wave plan files to check."
  exit 0
fi

echo "Scanning ${#files[@]} wave plan files..."
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
echo "Summary: PASS:$pass_count / FAIL:$fail_count / EXEMPT:$exempt_count"

if (( fail_count > 0 )); then
  echo "Exit 1 — wave-plan-completeness FAIL (≥1 plan missing required sections/frontmatter)"
  exit 1
fi

exit "$exit_code"

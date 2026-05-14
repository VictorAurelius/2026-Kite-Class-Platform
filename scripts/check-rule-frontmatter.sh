#!/usr/bin/env bash
# check-rule-frontmatter — validate `.claude/rules/*.md` frontmatter compliance.
#
# Closes GAP-250 (CI gate paired with GAP-249 backfill). Spec lives in
# `.claude/rules/rule-change-process.md` §3 (mandatory fields) + §6 (enforcement).
#
# Each rule file MUST contain (markdown-header style, NOT YAML frontmatter):
#   1. `**Version:** \d+\.\d+(\.\d+)?` line
#   2. `**Last-Reviewed:** YYYY-MM-DD` line (date sanity ≤ today)
#   3. `**Reviewer-Approver:** @<handle>` line
#   4. `**Applies to:** ...` line (any non-empty content)
#   5. A `## ... Log` heading + ≥1 list item below it
#
# Exit codes:
#   0 = all rules compliant
#   1 = ≥1 rule failed validation (file:line evidence printed)
#   2 = invocation error (no rules found, bad arg, etc.)
#
# Flags:
#   --all      Report every failure across all files (default: stop after first
#              file with any failure to keep CI output focused).
#   --self-test
#              Run against synthetic fixtures under
#              `scripts/fixtures/rule-frontmatter/` and assert PASS/FAIL outputs.
#   --paths "<files>"
#              Validate the listed files instead of `.claude/rules/*.md`.
#              Used by CI to pass the `git diff --name-only` filter result.
#
# Used by:
#   - `.github/workflows/script-quality.yml` job `rule-frontmatter` (PR gate)
#   - manual local run before opening rule-change PR

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
RULES_DIR="$REPO_ROOT/.claude/rules"
FIXTURES_DIR="$REPO_ROOT/scripts/fixtures/rule-frontmatter"

mode_all=false
mode_self_test=false
custom_paths=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --all) mode_all=true; shift ;;
    --self-test) mode_self_test=true; shift ;;
    --paths) custom_paths="${2:-}"; shift 2 ;;
    -h|--help)
      sed -n '1,40p' "$0" | grep -E '^# ' | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown arg: $1" >&2; exit 2 ;;
  esac
done

# ─────────────────────────────────────────────────────────────
# Per-file validator. Returns 0 on PASS, 1 on FAIL.
# Prints `FAIL <file>:<lineHint> reason` on failure.
# ─────────────────────────────────────────────────────────────
validate_file() {
  local file="$1"
  local fail_count=0
  local today
  today="$(date -u +%Y-%m-%d)"

  if [[ ! -f "$file" ]]; then
    echo "FAIL $file:0 file not found"
    return 1
  fi

  # Field 1 — Version
  local version_line
  version_line="$(grep -nE '^\*\*Version:\*\*[[:space:]]+[0-9]+\.[0-9]+(\.[0-9]+)?' "$file" | head -1 || true)"
  if [[ -z "$version_line" ]]; then
    local first_line
    first_line="$(grep -nE '^\*\*Version:\*\*' "$file" | head -1 | cut -d: -f1 || echo 1)"
    echo "FAIL $file:${first_line:-1} missing or malformed **Version:** field (expected MAJOR.MINOR or MAJOR.MINOR.PATCH)"
    fail_count=$((fail_count + 1))
  fi

  # Field 2 — Last-Reviewed (date sanity)
  local lr_line lr_value lr_lineno
  lr_line="$(grep -nE '^\*\*Last-Reviewed:\*\*[[:space:]]+[0-9]{4}-[0-9]{2}-[0-9]{2}' "$file" | head -1 || true)"
  if [[ -z "$lr_line" ]]; then
    echo "FAIL $file:1 missing or malformed **Last-Reviewed:** field (expected YYYY-MM-DD)"
    fail_count=$((fail_count + 1))
  else
    lr_lineno="$(echo "$lr_line" | cut -d: -f1)"
    lr_value="$(echo "$lr_line" | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}' | head -1)"
    if [[ "$lr_value" > "$today" ]]; then
      echo "FAIL $file:$lr_lineno **Last-Reviewed:** $lr_value is in the future (today is $today)"
      fail_count=$((fail_count + 1))
    fi
  fi

  # Field 3 — Reviewer-Approver (must include @handle)
  local ra_line
  ra_line="$(grep -nE '^\*\*Reviewer-Approver:\*\*[[:space:]]+.*@[A-Za-z0-9_-]+' "$file" | head -1 || true)"
  if [[ -z "$ra_line" ]]; then
    echo "FAIL $file:1 missing or malformed **Reviewer-Approver:** field (expected @handle)"
    fail_count=$((fail_count + 1))
  fi

  # Field 4 — Applies to (must have non-empty content)
  local at_line at_lineno at_content
  at_line="$(grep -nE '^\*\*Applies to:\*\*[[:space:]]' "$file" | head -1 || true)"
  if [[ -z "$at_line" ]]; then
    echo "FAIL $file:1 missing **Applies to:** field"
    fail_count=$((fail_count + 1))
  else
    at_lineno="$(echo "$at_line" | cut -d: -f1)"
    at_content="$(echo "$at_line" | sed -E 's/^[0-9]+:\*\*Applies to:\*\*[[:space:]]+//')"
    if [[ -z "${at_content// /}" ]]; then
      echo "FAIL $file:$at_lineno **Applies to:** field is empty"
      fail_count=$((fail_count + 1))
    fi
  fi

  # Field 5 — Log section: heading ending in the standalone word "Log".
  # Accept either `## Log` (bare) or `## <prefix> Log` (numbered/labeled).
  # "Log" must be at end-of-line and preceded by either the heading marker
  # or whitespace — never mid-word — so headings like `## 4. Examples from
  # Current Backlog` or `## Catalog` don't false-trigger.
  local log_lineno
  log_lineno="$(grep -nE '^## ([Ll]og|.+[[:space:]][Ll]og)[[:space:]]*$' "$file" | head -1 | cut -d: -f1 || true)"
  if [[ -z "$log_lineno" ]]; then
    echo "FAIL $file:1 missing '## ... Log' heading"
    fail_count=$((fail_count + 1))
  else
    # Look at lines after the heading, stopping at next "## " heading or EOF.
    local log_body
    log_body="$(awk -v start="$log_lineno" 'NR>start && /^## /{exit} NR>start{print}' "$file")"
    if ! echo "$log_body" | grep -qE '^[[:space:]]*-[[:space:]]+'; then
      echo "FAIL $file:$log_lineno '## ... Log' heading has no list entries below it"
      fail_count=$((fail_count + 1))
    fi
  fi

  if [[ $fail_count -gt 0 ]]; then
    return 1
  fi
  return 0
}

# ─────────────────────────────────────────────────────────────
# Self-test mode — run against fixtures, assert behavior.
# ─────────────────────────────────────────────────────────────
self_test() {
  if [[ ! -d "$FIXTURES_DIR" ]]; then
    echo "Self-test ERROR: fixtures dir missing: $FIXTURES_DIR" >&2
    return 2
  fi

  echo "Self-test mode — running against fixtures in $FIXTURES_DIR"
  echo "─────────────────────────────────────────────────────────"
  local passed=0 failed=0

  for fixture in "$FIXTURES_DIR"/*.md; do
    [[ -f "$fixture" ]] || continue
    local fname
    fname="$(basename "$fixture")"
    # Fixture naming convention: good*.md = expect PASS; bad*.md = expect FAIL.
    local expect
    case "$fname" in
      good*) expect="PASS" ;;
      bad*) expect="FAIL" ;;
      *) echo "Self-test SKIP: unrecognized fixture name $fname" >&2; continue ;;
    esac

    local actual="PASS"
    local out
    if ! out="$(validate_file "$fixture" 2>&1)"; then
      actual="FAIL"
    fi

    if [[ "$actual" == "$expect" ]]; then
      printf "  ✓ %-40s expected=%s got=%s\n" "$fname" "$expect" "$actual"
      passed=$((passed + 1))
    else
      printf "  ✗ %-40s expected=%s got=%s\n" "$fname" "$expect" "$actual"
      [[ -n "$out" ]] && printf "    output: %s\n" "$out"
      failed=$((failed + 1))
    fi
  done

  echo
  echo "Self-test summary: $passed passed, $failed failed"
  if [[ $failed -gt 0 ]]; then
    return 1
  fi
  return 0
}

# ─────────────────────────────────────────────────────────────
# Main flow.
# ─────────────────────────────────────────────────────────────
if [[ "$mode_self_test" == true ]]; then
  self_test
  exit $?
fi

# Determine which files to validate.
declare -a files=()
if [[ -n "$custom_paths" ]]; then
  # Caller passed an explicit space-separated list (CI uses this with the
  # diff-filter result so unrelated rule files don't fail an unrelated PR).
  for f in $custom_paths; do
    files+=("$f")
  done
else
  if [[ ! -d "$RULES_DIR" ]]; then
    echo "ERROR: rules dir not found: $RULES_DIR" >&2
    exit 2
  fi
  while IFS= read -r -d '' f; do
    # Skip README.md — folder index, not a rule (per rule-change-process.md §3)
    if [[ "$(basename "$f")" == "README.md" ]]; then
      continue
    fi
    files+=("$f")
  done < <(find "$RULES_DIR" -maxdepth 1 -name '*.md' -type f -print0 | sort -z)
fi

if [[ ${#files[@]} -eq 0 ]]; then
  echo "No rule files to validate."
  exit 0
fi

total=0
fail_total=0
for file in "${files[@]}"; do
  # Skip README.md — folder index, not a rule (per rule-change-process.md §3)
  # Applied in for-loop so it works for both auto-discover and --paths modes
  if [[ "$(basename "$file")" == "README.md" ]]; then
    continue
  fi
  # Skip _examples/ subdirectory — deferred-load companion files per Wave 76 Bucket E
  # streamline (context-budget-mandate.md §3). Companion files reference their
  # parent rule via YAML frontmatter (parent_rule:) but don't carry full rule
  # frontmatter themselves.
  if [[ "$file" == *"/_examples/"* ]]; then
    continue
  fi
  total=$((total + 1))
  if ! validate_file "$file"; then
    fail_total=$((fail_total + 1))
    if [[ "$mode_all" != true ]]; then
      # Default: stop on first FAIL'd file (cleaner CI signal, faster fail).
      echo
      echo "Stopping on first failing file. Re-run with --all to see every failure."
      echo "Total scanned so far: $total / ${#files[@]}"
      exit 1
    fi
  fi
done

echo
echo "Rule frontmatter check"
echo "──────────────────────"
echo "  Files scanned: $total"
echo "  Failures:      $fail_total"
if [[ $fail_total -gt 0 ]]; then
  exit 1
fi
echo "  ✓ All rule files comply with rule-change-process.md §3 frontmatter spec."
exit 0

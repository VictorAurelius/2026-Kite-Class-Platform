#!/usr/bin/env bash
# run-rules.sh — self-test runner for session-docs-check fixtures
#
# For each fixture under test/fixtures/<group>/<case>/{before,after}/, builds a
# disposable git repo, commits before-state on `main`, checks out a feature
# branch, applies after-state, and invokes check-docs.sh against it.
# Asserts each case produces the expected verdict (PASS / WARN / FAIL).
#
# Usage:  ./test/run-rules.sh                # run all fixture groups
#         ./test/run-rules.sh wave-history   # run one group
#
# Exit codes: 0 = all expectations met, 1 = at least one fixture failed expectation.

set -u

SKILL_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CHECK="$SKILL_DIR/scripts/check-docs.sh"
FIXTURES_ROOT="$SKILL_DIR/test/fixtures"

if [ ! -x "$CHECK" ]; then
  chmod +x "$CHECK"
fi

GROUP="${1:-}"

# Expectation matrix: per fixture, we encode the expected severity + matching
# substring of the rule output. Format: GROUP/CASE|EXPECT|GREP_PATTERN
# EXPECT ∈ {PASS, WARN, FAIL}
declare -a EXPECTATIONS
EXPECTATIONS=(
  "wave-history/good-flip-with-append|PASS|Rule 15.*status:complete \\+ wave-history.jsonl appended"
  "wave-history/bad-flip-no-append|FAIL|Rule 15.*was not appended"
  "wave-history/bad-flip-bad-json|FAIL|Rule 15.*failed JSON parse"
  "wave-plan-state-check/good-symbol-with-evidence|PASS|Rule 16.*all have State-Check Evidence rows"
  "wave-plan-state-check/bad-symbol-no-evidence|FAIL|Rule 16.*missing State-Check Evidence row for"
  "wave-plan-state-check/forward-flagged-allowed|PASS|Rule 16.*all have State-Check Evidence rows"
  "post-merge-sync/good-status-flip-with-csv-sync|PASS|Rule 17.*Status flipped \\+ gap-status.csv touched"
  "post-merge-sync/bad-status-flip-no-csv-sync|FAIL|Rule 17.*Status flipped but gap-status.csv NOT updated in same PR"
  "post-merge-sync/bad-status-flip-no-csv-sync-with-override|WARN|Rule 17.*override trailer present"
)

PASS_COUNT=0
FAIL_COUNT=0
RESULTS=""

run_fixture() {
  local key="$1" expect="$2" pattern="$3"
  local case_dir="$FIXTURES_ROOT/$key"
  local before="$case_dir/before"
  local after="$case_dir/after"

  if [ ! -d "$before" ] || [ ! -d "$after" ]; then
    RESULTS+="[SKIP] $key — missing before/ or after/"$'\n'
    return
  fi

  local tmp
  tmp="$(mktemp -d -t session-docs-check-fixture-XXXXXX)"
  trap "rm -rf '$tmp'" RETURN

  (
    cd "$tmp" || exit 2
    git init -q -b main
    git config user.email fixture@local
    git config user.name fixture

    # Stage 1: copy `before/` and commit on main
    cp -a "$before/." .
    git add -A
    git commit -q -m "fixture: before"

    # Stage 2: branch + replace files with `after/`
    git checkout -q -b fixture/test
    # Wipe tracked files (preserve .git) and apply after/
    git ls-files -z | xargs -0 -r rm -f
    cp -a "$after/." .
    git add -A
    # Optional commit-message override per-fixture (lives at case_dir root, NOT
    # under before/ or after/ so it isn't applied as a tracked file).
    if [ -f "$case_dir/commit-message.txt" ]; then
      git commit -q -F "$case_dir/commit-message.txt"
    else
      git commit -q -m "fixture: after — apply scenario"
    fi

    # Run check-docs.sh against this fixture repo
    bash "$CHECK" --base=main --branch=fixture/test 2>&1 || true
  ) > "$tmp.out"

  local out
  out="$(cat "$tmp.out")"

  local actual=""
  if echo "$out" | grep -qE "^\[FAIL\].*$pattern"; then
    actual="FAIL"
  elif echo "$out" | grep -qE "^\[WARN\].*$pattern"; then
    actual="WARN"
  elif echo "$out" | grep -qE "^\[OK\].*$pattern"; then
    actual="PASS"
  else
    actual="MISS"
  fi

  if [ "$actual" = "$expect" ]; then
    PASS_COUNT=$((PASS_COUNT+1))
    RESULTS+="[OK]   $key — expected $expect, got $actual"$'\n'
  else
    FAIL_COUNT=$((FAIL_COUNT+1))
    RESULTS+="[FAIL] $key — expected $expect, got $actual"$'\n'
    RESULTS+="       output excerpt:"$'\n'
    RESULTS+="$(echo "$out" | grep -E "^\[(OK|WARN|FAIL)\].*(Rule 15|Rule 16|Rule 17)" | sed 's/^/         /')"$'\n'
  fi

  rm -f "$tmp.out"
}

for entry in "${EXPECTATIONS[@]}"; do
  IFS='|' read -r key expect pattern <<< "$entry"
  if [ -n "$GROUP" ] && [[ "$key" != "$GROUP/"* ]]; then
    continue
  fi
  run_fixture "$key" "$expect" "$pattern"
done

echo "## session-docs-check fixture run"
echo
echo "$RESULTS"
echo "Summary: $PASS_COUNT passed, $FAIL_COUNT failed."

if [ "$FAIL_COUNT" -gt 0 ]; then
  exit 1
fi
exit 0

#!/usr/bin/env bash
# deploy-preflight-simulator runner.
#
# Discovers every executable script under scripts/checks/ and runs them
# sequentially. Each check is independent and self-reports via
# `[LEVEL][cat-N][check-name] message` lines. Aggregate exit = 0 only when
# every check exits 0.
#
# Convention: checks may emit FAIL (exit 1) or PASS/WARN (exit 0).
#
# Usage:  bash preflight.sh [check-name-substring]
#
# Filter: pass a substring to run only matching check files (debug aid).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKS_DIR="$SCRIPT_DIR/checks"
FILTER="${1:-}"

if [[ ! -d "$CHECKS_DIR" ]]; then
  echo "[preflight] no checks/ directory at $CHECKS_DIR" >&2
  exit 2
fi

# Always run from repo root so check scripts can use relative paths.
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$PWD")"
cd "$REPO_ROOT"

shopt -s nullglob
checks=( "$CHECKS_DIR"/*.sh )
shopt -u nullglob

if [[ ${#checks[@]} -eq 0 ]]; then
  echo "[preflight] no check scripts found in $CHECKS_DIR" >&2
  exit 2
fi

# Sort for deterministic output
IFS=$'\n' checks=($(sort <<<"${checks[*]}"))
unset IFS

total=0
passed=0
failed=0
warned=0
failures=()

echo "==> deploy-preflight-simulator — running ${#checks[@]} check(s) at $(date -u +%FT%TZ)"
echo

for check in "${checks[@]}"; do
  name=$(basename "$check" .sh)
  if [[ -n "$FILTER" ]] && [[ "$name" != *"$FILTER"* ]]; then
    continue
  fi
  total=$((total + 1))
  echo "--- [$total] $name ---"

  if bash "$check"; then
    # Check stdout for any [WARN] markers to bump warned count
    # (rerun cheap: check captured nothing; rely on script summary line)
    passed=$((passed + 1))
  else
    failed=$((failed + 1))
    failures+=("$name")
  fi
  echo
done

echo "==> summary: $total run / $passed pass / $failed fail"
if [[ ${#failures[@]} -gt 0 ]]; then
  echo "==> failed checks:"
  printf '    - %s\n' "${failures[@]}"
  exit 1
fi

echo "==> all checks PASS (warnings may still be present — review output)"
exit 0

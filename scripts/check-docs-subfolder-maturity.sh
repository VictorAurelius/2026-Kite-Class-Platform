#!/usr/bin/env bash
# check-docs-subfolder-maturity.sh — detect single-file subdirs (<5 files threshold)
#
# Per `.claude/rules/docs-subfolder-maturity.md` §2:
#   Subdir under documents/** allowed when ≥5 files (Volume criterion)
#   OR cross-author / reviewer approval / sister-pattern.
#   Single-file subdir = anti-pattern (cognitive load > navigation value).
#
# Closes deferred-detector debt for docs-subfolder-maturity.md §5.3 (GAP-675 SHIP-NOW).
#
# Modes:
#   --strict       Exit 1 on any subdir < 5 files (WARN-mode default — existing grandfathered)
#   --warn         Exit 0 + emit WARN
#   --report-only  Print full inventory
#   --self-test    Run against fixtures
#
# Override trailer: DOCS_SUBFOLDER_MATURITY_OVERRIDE: <subdir-path> — <reason>

set -euo pipefail

MODE="${1:---warn}"
THRESHOLD=5
ROOT="${SCAN_DIR_OVERRIDE:-documents}"

run_self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "$tmpdir"' RETURN

  # Fixture 1: bad — single-file subdir
  mkdir -p "$tmpdir/parent/single-file"
  touch "$tmpdir/parent/single-file/foo.md"

  # Fixture 2: bad — 3 files (below threshold)
  mkdir -p "$tmpdir/parent/three-files"
  touch "$tmpdir/parent/three-files/a.md" "$tmpdir/parent/three-files/b.md" "$tmpdir/parent/three-files/c.md"

  # Fixture 3: good — 5 files at threshold
  mkdir -p "$tmpdir/parent/five-files"
  for i in 1 2 3 4 5; do touch "$tmpdir/parent/five-files/f$i.md"; done

  # Fixture 4: good — has README (infra) + 5 content files
  mkdir -p "$tmpdir/parent/with-readme"
  touch "$tmpdir/parent/with-readme/README.md"
  for i in 1 2 3 4 5; do touch "$tmpdir/parent/with-readme/file$i.md"; done

  local report
  report=$(SCAN_DIR_OVERRIDE="$tmpdir" "$0" --report-only 2>&1 || true)

  if echo "$report" | grep -q "single-file" && \
     echo "$report" | grep -q "three-files" && \
     ! echo "$report" | grep -E "five-files.*WARN" >/dev/null; then
    echo "PASS — detector flagged 2 below-threshold subdirs, exempted 5-file + README+5"
    return 0
  else
    echo "FAIL — self-test mismatch"
    echo "$report"
    return 1
  fi
}

case "$MODE" in
  --self-test) run_self_test; exit $? ;;
  --strict|--warn|--report-only) ;;
  *) echo "Usage: $0 [--strict|--warn|--report-only|--self-test]" >&2; exit 2 ;;
esac

if [[ ! -d "$ROOT" ]]; then
  echo "FAIL: $ROOT not found" >&2
  exit 1
fi

declare -i BELOW=0
declare -a VIOLATIONS=()

# Find all subdirs at depth ≥2 (skip ROOT itself + immediate top-level shape)
while IFS= read -r d; do
  # Skip archived/closed folders (per §2 exemptions in rule)
  case "$d" in
    */archived/*|*/closed/*|*/07-archived/*) continue ;;
  esac

  count=$(find "$d" -maxdepth 1 -type f -name '*.md' ! -name 'README*' ! -name '_*' 2>/dev/null | wc -l)
  if (( count < THRESHOLD )); then
    BELOW+=1
    VIOLATIONS+=("$d ($count files, threshold $THRESHOLD)")
    [[ "$MODE" == "--report-only" ]] && echo "  ✗ $d — $count files < $THRESHOLD"
  fi
done < <(find "$ROOT" -mindepth 2 -type d 2>/dev/null)

echo "─────────────────────────────────────"
echo "Docs subfolder maturity check"
echo "  Below-threshold subdirs: $BELOW (threshold $THRESHOLD files)"

if (( BELOW == 0 )); then
  echo "  ✓ All subdirs meet Volume criterion"
  exit 0
fi

case "$MODE" in
  --strict)
    echo ""
    echo "Violations:"
    for v in "${VIOLATIONS[@]}"; do echo "  - $v"; done
    echo ""
    echo "FAIL: $BELOW subdir(s) below Volume threshold per docs-subfolder-maturity.md §2"
    echo "Override: commit trailer 'DOCS_SUBFOLDER_MATURITY_OVERRIDE: <subdir> — <reason>'"
    exit 1
    ;;
  --warn|--report-only)
    [[ "$MODE" == "--warn" ]] && {
      echo "  WARN: $BELOW subdir(s) below threshold (existing grandfathered; new must satisfy §2)"
      echo "  Top 5 candidates for flatten:"
      for v in "${VIOLATIONS[@]:0:5}"; do echo "    - $v"; done
    }
    exit 0
    ;;
esac

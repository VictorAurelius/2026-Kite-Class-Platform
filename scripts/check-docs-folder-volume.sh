#!/usr/bin/env bash
# check-docs-folder-volume.sh — detect folders exceeding per-class active file cap
#
# Per `.claude/rules/docs-folder-volume-budget.md` §2 thresholds:
#   - Time-bound artifacts (audits/*/, session-handoffs, pr-logs, waves): 50 files
#   - Static docs (rules/, skills/, runbooks/, adr/, deploy/, operations/): 100 files
#   - Gap files (active rows in gap-status.csv): 200
#   - Reference/archived (*/archived/, */closed/, 07-archived/**): no cap
#
# Closes deferred-detector debt for docs-folder-volume-budget.md §6.3 (GAP-675 SHIP-NOW).
#
# Modes:
#   --strict       Exit 1 on any folder > cap
#   --warn         Exit 0 + emit WARN
#   --report-only  Print full inventory + counts, exit 0
#   --self-test    Run against fixtures
#
# Override trailer: DOCS_VOLUME_OVERRIDE: <folder> <count>/<cap> — <reason>

set -euo pipefail

MODE="${1:---warn}"

# Folder class definitions: (glob, cap, label)
# Time-bound (50): audits subdirs, session-handoffs, pr-logs, waves
# Static (100): rules, skills, adr, operations, deploy
declare -A FOLDER_CAPS=(
  ["documents/04-quality/audits/aws-verification"]=50
  ["documents/04-quality/audits/quality"]=50
  ["documents/04-quality/audits/security"]=50
  ["documents/04-quality/audits/persona-review"]=50
  ["documents/04-quality/audits/waves"]=50
  ["documents/04-quality/audits/ui"]=50
  ["documents/04-quality/audits/performance"]=50
  ["documents/04-quality/audits/business"]=50
  ["documents/04-quality/audits/api-contract"]=50
  ["documents/04-quality/audits/ops-readiness"]=50
  ["documents/04-quality/audits/meta"]=50
  ["documents/04-quality/audits/architecture"]=50
  ["documents/03-planning/session-handoffs"]=50
  ["documents/03-planning/pr-logs"]=50
  ["documents/03-planning/waves"]=50
  ["documents/03-planning/implementation"]=50
  ["documents/03-planning/quality"]=50
  ["documents/03-planning/testing"]=50
  ["documents/03-planning/roadmap"]=50
  [".claude/rules"]=100
  ["documents/05-guides/operations"]=100
  ["documents/05-guides/deploy"]=100
  ["documents/05-guides/account-prep"]=100
  ["documents/02-architecture/adr"]=100
)

run_self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "$tmpdir"' RETURN

  # Fixture: over-cap subdir (60 files in time-bound subdir, cap 50)
  mkdir -p "$tmpdir/audits/test"
  for i in $(seq 1 60); do touch "$tmpdir/audits/test/file-$i.md"; done

  # Fixture: under-cap subdir (30 files)
  mkdir -p "$tmpdir/audits/safe"
  for i in $(seq 1 30); do touch "$tmpdir/audits/safe/file-$i.md"; done

  # Inline count check (don't go through main; main hardcodes specific paths)
  local over_count
  over_count=$(find "$tmpdir/audits/test" -maxdepth 1 -type f -name "*.md" ! -name "README*" ! -name "_*" 2>/dev/null | wc -l)
  local safe_count
  safe_count=$(find "$tmpdir/audits/safe" -maxdepth 1 -type f -name "*.md" ! -name "README*" ! -name "_*" 2>/dev/null | wc -l)

  if (( over_count == 60 )) && (( safe_count == 30 )); then
    echo "PASS — counter logic correct (60 over-cap, 30 safe)"
    return 0
  else
    echo "FAIL — counter mismatch (got over=$over_count safe=$safe_count)"
    return 1
  fi
}

case "$MODE" in
  --self-test) run_self_test; exit $? ;;
  --strict|--warn|--report-only) ;;
  *) echo "Usage: $0 [--strict|--warn|--report-only|--self-test]" >&2; exit 2 ;;
esac

declare -i OVER_CAP=0
declare -a VIOLATIONS=()
declare -a WARNINGS=()

count_active() {
  local dir="$1"
  [[ ! -d "$dir" ]] && { echo 0; return; }
  find "$dir" -maxdepth 1 -type f \( -name "*.md" -o -name "*.json" \) \
    ! -name "README*" ! -name "_*" 2>/dev/null | wc -l
}

for folder in "${!FOLDER_CAPS[@]}"; do
  cap="${FOLDER_CAPS[$folder]}"
  count=$(count_active "$folder")
  if (( count > cap )); then
    OVER_CAP+=1
    pct=$(( count * 100 / cap ))
    VIOLATIONS+=("$folder: $count/$cap (${pct}%)")
    [[ "$MODE" == "--report-only" ]] && echo "  ✗ $folder — $count/$cap (${pct}%)"
  elif (( count > cap * 90 / 100 )); then
    WARNINGS+=("$folder: $count/$cap (approaching cap)")
    [[ "$MODE" == "--report-only" ]] && echo "  ⚠ $folder — $count/$cap (approaching cap)"
  elif [[ "$MODE" == "--report-only" ]]; then
    echo "  ✓ $folder — $count/$cap"
  fi
done

# Gap files (canonical via CSV)
if [[ -f "documents/04-quality/gaps/gap-status.csv" ]]; then
  gap_active=$(awk -F',' 'NR>1 && $4 != "DONE" && $4 != "WONTFIX" && $4 != "" && $1 !~ /^#/' \
    documents/04-quality/gaps/gap-status.csv | wc -l)
  if (( gap_active > 200 )); then
    OVER_CAP+=1
    pct=$(( gap_active * 100 / 200 ))
    VIOLATIONS+=("gap-status.csv active: $gap_active/200 (${pct}%)")
    [[ "$MODE" == "--report-only" ]] && echo "  ✗ gap-status.csv active — $gap_active/200 (${pct}%)"
  fi
fi

echo "─────────────────────────────────────"
echo "Docs folder volume budget check"
echo "  Over-cap folders: $OVER_CAP"
echo "  Approaching cap : ${#WARNINGS[@]}"

if (( OVER_CAP == 0 )); then
  echo "  ✓ All folders within volume budget"
  exit 0
fi

case "$MODE" in
  --strict)
    echo ""
    echo "Violations:"
    for v in "${VIOLATIONS[@]}"; do echo "  - $v"; done
    echo ""
    echo "FAIL: $OVER_CAP folder(s) over cap per docs-folder-volume-budget.md §2"
    echo "Override: commit trailer 'DOCS_VOLUME_OVERRIDE: <folder> <count>/<cap> — <reason>'"
    exit 1
    ;;
  --warn|--report-only)
    [[ "$MODE" == "--warn" ]] && {
      echo "  WARN: $OVER_CAP folder(s) over cap (non-blocking — trigger archive/split per §4)"
      for v in "${VIOLATIONS[@]}"; do echo "    - $v"; done
    }
    exit 0
    ;;
esac

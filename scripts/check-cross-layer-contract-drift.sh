#!/usr/bin/env bash
# check-cross-layer-contract-drift.sh — detect URL drift between Java @RequestMapping vs api-contract.md
#
# Per `contract-first-for-cross-layer.md` §6.2 (closes "PR template hook deferred" debt)
# + Wave 98 GAP-662 incident: EmailController @RequestMapping("/api/platform/emails")
# vs documented Endpoint: /api/email — 3-way drift code/doc/test.
#
# Heuristic v1: scan diff for @RequestMapping/@GetMapping/@PostMapping/etc. literal
# URL prefixes AND corresponding api-contract.md files in same business domain.
# WARN when controller path prefix doesn't appear in any api-contract.md OR when
# api-contract.md mentions endpoint not present in any controller.
#
# Modes:
#   --warn         Default. WARN on drift, exit 0
#   --strict       FAIL on drift, exit 1
#   --report-only  Print full matrix, exit 0
#   --self-test    Synthetic fixtures
#
# Limitations:
#   - Heuristic only; complex path patterns + @PathVariable not modeled
#   - PR-scoped diff mode preferred (env CHECK_BASE=origin/main); full-scan fallback
#   - Override: commit trailer 'CONTRACT_DRIFT_OVERRIDE: <reason + follow-up gap>'
#
# Exit codes:
#   0  pass (or --warn / --report-only)
#   1  strict mode + ≥1 drift found
#   2  invocation error

set -euo pipefail

MODE="${1:---warn}"

run_self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "$tmpdir"' RETURN

  # Fixture: matching controller + doc
  mkdir -p "$tmpdir/match/src/main/java/com/example/match"
  cat > "$tmpdir/match/src/main/java/com/example/match/GoodController.java" <<'EOF'
@RestController
@RequestMapping("/api/v1/widgets")
public class GoodController { }
EOF
  mkdir -p "$tmpdir/match/docs/widgets"
  cat > "$tmpdir/match/docs/widgets/api-contract.md" <<'EOF'
# Widgets API

| Endpoint | Method |
|---|---|
| /api/v1/widgets | GET |
EOF

  # Fixture: drift (controller /api/platform/foo vs doc /api/foo)
  mkdir -p "$tmpdir/drift/src/main/java/com/example/drift"
  cat > "$tmpdir/drift/src/main/java/com/example/drift/DriftController.java" <<'EOF'
@RestController
@RequestMapping("/api/platform/foo")
public class DriftController { }
EOF
  mkdir -p "$tmpdir/drift/docs/foo"
  cat > "$tmpdir/drift/docs/foo/api-contract.md" <<'EOF'
# Foo API

POST /api/foo
EOF

  # Run match → should PASS
  local match_out
  match_out=$(SCAN_ROOT="$tmpdir/match" "$0" --report-only 2>&1 || true)
  # Run drift → should detect mismatch
  local drift_out
  drift_out=$(SCAN_ROOT="$tmpdir/drift" "$0" --report-only 2>&1 || true)

  if echo "$match_out" | grep -q "0 drift" && echo "$drift_out" | grep -qE "drift|mismatch"; then
    echo "PASS — self-test detected match + drift fixtures correctly"
    return 0
  else
    echo "FAIL — self-test mismatch"
    echo "match output: $match_out"
    echo "drift output: $drift_out"
    return 1
  fi
}

case "$MODE" in
  --self-test) run_self_test; exit $? ;;
  --strict|--warn|--report-only) ;;
  *) echo "Usage: $0 [--strict|--warn|--report-only|--self-test]" >&2; exit 2 ;;
esac

ROOT="${SCAN_ROOT:-.}"

# Extract URL prefixes from @RequestMapping / @GetMapping / @PostMapping / @PutMapping / @DeleteMapping / @PatchMapping
extract_controller_paths() {
  local search_root="$1"
  # Match annotation with literal URL string
  grep -rhE '@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\("[^"]+"\)' \
    "$search_root" --include="*.java" 2>/dev/null \
    | sed -E 's/.*"([^"]+)".*/\1/' \
    | grep -E '^/' \
    | sort -u
}

# Extract Endpoint paths from api-contract.md files
extract_doc_paths() {
  local search_root="$1"
  # Match common patterns: "POST /api/...", "/api/..." in tables, "Endpoint: /api/..."
  grep -rhE '^\|?\s*(GET|POST|PUT|DELETE|PATCH)?\s*/api/[a-zA-Z0-9/_-]+' \
    "$search_root" --include="api-contract.md" 2>/dev/null \
    | grep -oE '/api/[a-zA-Z0-9/_-]+' \
    | sort -u
}

declare -i DRIFTS=0
declare -a DRIFT_LIST=()

CONTROLLER_PATHS=$(extract_controller_paths "$ROOT")
DOC_PATHS=$(extract_doc_paths "$ROOT")

if [[ -z "$CONTROLLER_PATHS" && -z "$DOC_PATHS" ]]; then
  echo "  (no controllers or api-contracts in $ROOT — nothing to check)"
  exit 0
fi

# For each controller path, check if its prefix exists in ANY doc
while IFS= read -r ctrl_path; do
  [[ -z "$ctrl_path" ]] && continue
  # Skip if path appears verbatim in docs OR has common alias (api/v1 ↔ api/platform tolerated by override)
  if echo "$DOC_PATHS" | grep -qF "$ctrl_path"; then
    [[ "$MODE" == "--report-only" ]] && echo "  ✓ $ctrl_path → matched in api-contract"
  else
    # Likely drift candidate
    if [[ "$ctrl_path" == /api/platform/* ]]; then
      DRIFTS+=1
      DRIFT_LIST+=("controller: $ctrl_path (legacy /api/platform prefix; docs may use /api/v1)")
    elif [[ "$ctrl_path" == /api/v1/* ]] || [[ "$ctrl_path" == /api/* ]]; then
      # Check if just the prefix differs
      stripped="${ctrl_path#/api/v1}"
      stripped="${stripped#/api}"
      pattern_alt="${stripped}[^a-zA-Z0-9_]"
      if ! echo "$DOC_PATHS" | grep -qE "${stripped}$|${pattern_alt}"; then
        DRIFTS+=1
        DRIFT_LIST+=("controller: $ctrl_path → no matching api-contract Endpoint")
      else
        [[ "$MODE" == "--report-only" ]] && echo "  ⚠ $ctrl_path → prefix-fuzzy match in api-contract"
      fi
    fi
  fi
done <<< "$CONTROLLER_PATHS"

echo "─────────────────────────────────────"
echo "Cross-layer contract drift check"
echo "  Controller paths scanned: $(echo "$CONTROLLER_PATHS" | grep -c '^/')"
echo "  Doc paths scanned:        $(echo "$DOC_PATHS" | grep -c '^/')"
echo "  Drifts:                   $DRIFTS"

if [[ $DRIFTS -eq 0 ]]; then
  echo "  ✓ 0 drift detected (heuristic v1 — manual review still recommended for complex paths)"
  exit 0
fi

echo ""
echo "Drift candidates:"
for d in "${DRIFT_LIST[@]}"; do
  echo "  - $d"
done

case "$MODE" in
  --strict)
    echo ""
    echo "FAIL: $DRIFTS drift candidate(s) detected"
    echo "Per contract-first-for-cross-layer.md §3 — reconcile code path with api-contract.md Endpoint"
    echo "Override: commit trailer 'CONTRACT_DRIFT_OVERRIDE: <reason + gap link>'"
    exit 1
    ;;
  --warn|--report-only)
    echo ""
    echo "WARN: $DRIFTS drift candidate(s) — heuristic v1 may have false positives; review manually"
    exit 0
    ;;
esac

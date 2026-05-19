#!/usr/bin/env bash
# check-mermaid-sequence-state-br.sh — detect <br/> in Mermaid sequenceDiagram + stateDiagram blocks
#
# Per `diagram-format-selection.md` v1.0.2 §4 HARD RULE:
# Mermaid sequence/state parsers unreliable với HTML breaks. Use ` — ` separator instead.
# flowchart blocks DO support <br/> reliably (not flagged).
#
# Triggered by Wave 99B recurrence #6+#7 (multi-tenant §3 + kitehub admin flow §3.3).
#
# Modes:
#   --warn         Default. WARN + exit 0
#   --strict       FAIL + exit 1
#   --report-only  Print + exit 0
#   --self-test    Synthetic fixture
#
# Exit codes:
#   0 PASS (or --warn / --report-only)
#   1 strict FAIL
#   2 invocation error

set -euo pipefail

MODE="${1:---warn}"

run_self_test() {
  local tmpdir
  tmpdir=$(mktemp -d)
  trap 'rm -rf "$tmpdir"' RETURN

  # Fixture: sequence with <br/> in Note + message
  cat > "$tmpdir/bad-sequence.md" <<'EOF'
# Test

```mermaid
sequenceDiagram
    A->>B: msg<br/>continued
    Note over B: text<br/>more
```
EOF

  # Fixture: state with <br/>
  cat > "$tmpdir/bad-state.md" <<'EOF'
# Test

```mermaid
stateDiagram-v2
    A --> B: trans<br/>label
```
EOF

  # Fixture: flowchart with <br/> (should NOT flag)
  cat > "$tmpdir/good-flowchart.md" <<'EOF'
# Test

```mermaid
flowchart TD
    A[Node<br/>Subtitle] --> B
```
EOF

  local bad_seq_count bad_state_count good_count
  bad_seq_count=$(SCAN_ROOT="$tmpdir" "$0" --report-only 2>&1 | grep -c "bad-sequence" || true)
  bad_state_count=$(SCAN_ROOT="$tmpdir" "$0" --report-only 2>&1 | grep -c "bad-state" || true)
  good_count=$(SCAN_ROOT="$tmpdir" "$0" --report-only 2>&1 | grep -c "good-flowchart" || true)

  if [[ $bad_seq_count -ge 1 ]] && [[ $bad_state_count -ge 1 ]] && [[ $good_count -eq 0 ]]; then
    echo "PASS — self-test detected bad sequence + bad state, ignored good flowchart"
    return 0
  else
    echo "FAIL — bad_seq=$bad_seq_count bad_state=$bad_state_count good=$good_count (expected ≥1/≥1/0)"
    return 1
  fi
}

case "$MODE" in
  --self-test) run_self_test; exit $? ;;
  --strict|--warn|--report-only) ;;
  *) echo "Usage: $0 [--strict|--warn|--report-only|--self-test]" >&2; exit 2 ;;
esac

SCAN_DIR="${SCAN_ROOT:-.}"

declare -i VIOLATIONS=0
declare -a VIOLATION_LIST=()

# Find all .md files (excluding archived)
while IFS= read -r -d '' f; do
  # Awk scans each file: track entering/leaving mermaid block + diagram type
  awk '
    /^```mermaid$/ { in_block=1; type=""; next }
    in_block && /^(sequenceDiagram|stateDiagram)/ { type=$1; next }
    in_block && type != "" && /<br\/>/ {
      print FILENAME ":" NR " [" type "]: " $0
    }
    /^```$/ { in_block=0; type="" }
  ' "$f" 2>/dev/null
done < <(find "$SCAN_DIR" \( -path "*/07-archived" -o -path "*/node_modules" -o -path "*/.git" -o -path "*/.claude/worktrees" -o -path "*/target" -o -path "*/build" \) -prune -o -name "*.md" -type f -print0 2>/dev/null) | while IFS= read -r line; do
  [[ -n "$line" ]] && echo "$line"
done > /tmp/mermaid-br-violations.txt

VIOLATIONS=$(wc -l < /tmp/mermaid-br-violations.txt)

echo "─────────────────────────────────────"
echo "Mermaid <br/> in sequenceDiagram/stateDiagram check"
echo "  Scan root: $SCAN_DIR"
echo "  Violations: $VIOLATIONS"

if [[ $VIOLATIONS -eq 0 ]]; then
  echo "  ✓ No <br/> in sequence/state diagrams"
  rm -f /tmp/mermaid-br-violations.txt
  exit 0
fi

echo ""
echo "Violations (replace <br/> với ' — ' separator):"
cat /tmp/mermaid-br-violations.txt | head -20
[[ $VIOLATIONS -gt 20 ]] && echo "  ... ($((VIOLATIONS - 20)) more)"
rm -f /tmp/mermaid-br-violations.txt

case "$MODE" in
  --strict)
    echo ""
    echo "FAIL: $VIOLATIONS <br/> instance(s) in sequence/state diagrams"
    echo "Per diagram-format-selection.md v1.0.2 §4 HARD RULE — replace với ' — ' or single-line"
    echo "Override: commit trailer 'MERMAID_BR_OVERRIDE: <reason>'"
    exit 1
    ;;
  --warn|--report-only)
    echo ""
    echo "WARN: $VIOLATIONS violations (non-blocking — will HARD STOP post 7-day grace)"
    exit 0
    ;;
esac

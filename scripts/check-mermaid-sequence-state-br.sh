#!/usr/bin/env bash
# check-mermaid-sequence-state-br.sh — detect Mermaid sequenceDiagram + stateDiagram parser hazards
#
# Per `diagram-format-selection.md` v1.0.3 §4 HARD RULES:
# 1. <br/> ANYWHERE in sequenceDiagram + stateDiagram blocks (recurrence #6+#7, 2026-05-19)
# 2. ';' (semicolon) in Note over/left of/right of text in sequenceDiagram + stateDiagram (recurrence #8, 2026-05-19)
#    Reason: Mermaid parser treats ';' as statement terminator inside Note text → orphans remainder
#    + concatenates next line → "Expecting ARROW, got NEWLINE" error
# Replace banned chars with ' — ' separator or '.' period or omit subtitle.
# flowchart blocks DO support <br/> reliably (not flagged).
#
# Triggered by Wave 99B recurrence #6+#7 (multi-tenant §3 + kitehub admin flow §3.3 — <br/>)
# + recurrence #8 (multi-tenant §3 Note over with ';' — 2026-05-19 user-flagged after Wave 99B closure).
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

  # Fixture 1: sequence with <br/> in Note + message
  cat > "$tmpdir/bad-sequence-br.md" <<'EOF'
# Test

```mermaid
sequenceDiagram
    A->>B: msg<br/>continued
    Note over B: text<br/>more
```
EOF

  # Fixture 2: state with <br/>
  cat > "$tmpdir/bad-state-br.md" <<'EOF'
# Test

```mermaid
stateDiagram-v2
    A --> B: trans<br/>label
```
EOF

  # Fixture 3: sequence with ';' in Note (recurrence #8)
  cat > "$tmpdir/bad-sequence-semicolon.md" <<'EOF'
# Test

```mermaid
sequenceDiagram
    A->>B: msg
    Note over A,B: First clause; second clause that orphans
```
EOF

  # Fixture 4: flowchart with <br/> + ';' (should NOT flag — flowchart parser tolerant)
  cat > "$tmpdir/good-flowchart.md" <<'EOF'
# Test

```mermaid
flowchart TD
    A[Node<br/>Subtitle; also OK] --> B
```
EOF

  local bad_br_seq bad_br_state bad_semi good_count
  bad_br_seq=$(SCAN_ROOT="$tmpdir" "$0" --report-only 2>&1 | grep -c "bad-sequence-br" || true)
  bad_br_state=$(SCAN_ROOT="$tmpdir" "$0" --report-only 2>&1 | grep -c "bad-state-br" || true)
  bad_semi=$(SCAN_ROOT="$tmpdir" "$0" --report-only 2>&1 | grep -c "bad-sequence-semicolon" || true)
  good_count=$(SCAN_ROOT="$tmpdir" "$0" --report-only 2>&1 | grep -c "good-flowchart" || true)

  if [[ $bad_br_seq -ge 1 ]] && [[ $bad_br_state -ge 1 ]] && [[ $bad_semi -ge 1 ]] && [[ $good_count -eq 0 ]]; then
    echo "PASS — self-test detected 3 bad cases (sequence-br, state-br, sequence-semicolon), ignored good flowchart"
    return 0
  else
    echo "FAIL — bad_br_seq=$bad_br_seq bad_br_state=$bad_br_state bad_semi=$bad_semi good=$good_count (expected ≥1/≥1/≥1/0)"
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

# Find all .md files (excluding archived)
while IFS= read -r -d '' f; do
  # Awk scans each file: track entering/leaving mermaid block + diagram type
  # Check 1: <br/> anywhere in sequence/state block
  # Check 2: ';' inside Note over/left of/right of text in sequence/state block
  awk '
    /^```mermaid$/ { in_block=1; type=""; next }
    in_block && /^(sequenceDiagram|stateDiagram)/ { type=$1; next }
    in_block && type != "" && /<br\/>/ {
      print FILENAME ":" NR " [" type " <br/>]: " $0
    }
    in_block && type != "" && /^[[:space:]]*[Nn]ote (over|left of|right of)[^:]*:.*;/ {
      print FILENAME ":" NR " [" type " Note semicolon]: " $0
    }
    /^```$/ { in_block=0; type="" }
  ' "$f" 2>/dev/null
done < <(find "$SCAN_DIR" \( -path "*/07-archived" -o -path "*/node_modules" -o -path "*/.git" -o -path "*/.claude/worktrees" -o -path "*/target" -o -path "*/build" \) -prune -o -name "*.md" -type f -print0 2>/dev/null) | while IFS= read -r line; do
  [[ -n "$line" ]] && echo "$line"
done > /tmp/mermaid-br-violations.txt

VIOLATIONS=$(wc -l < /tmp/mermaid-br-violations.txt)

echo "─────────────────────────────────────"
echo "Mermaid sequence/state parser hazards check"
echo "  Scan root: $SCAN_DIR"
echo "  Violations: $VIOLATIONS"
echo "  Detects: (1) <br/> in sequence/state blocks; (2) ';' in sequence/state Note text"

if [[ $VIOLATIONS -eq 0 ]]; then
  echo "  ✓ No parser hazards detected"
  rm -f /tmp/mermaid-br-violations.txt
  exit 0
fi

echo ""
echo "Violations (replace <br/> với ' — ' separator; replace ';' với ' — ' or '.'):"
cat /tmp/mermaid-br-violations.txt | head -20
[[ $VIOLATIONS -gt 20 ]] && echo "  ... ($((VIOLATIONS - 20)) more)"
rm -f /tmp/mermaid-br-violations.txt

case "$MODE" in
  --strict)
    echo ""
    echo "FAIL: $VIOLATIONS parser-hazard instance(s) in sequence/state diagrams"
    echo "Per diagram-format-selection.md v1.0.3 §4 HARD RULES — replace với ' — ' or '.' or single-line"
    echo "Override: commit trailer 'MERMAID_BR_OVERRIDE: <reason>'"
    exit 1
    ;;
  --warn|--report-only)
    echo ""
    echo "WARN: $VIOLATIONS violations (non-blocking — will HARD STOP post 7-day grace)"
    exit 0
    ;;
esac

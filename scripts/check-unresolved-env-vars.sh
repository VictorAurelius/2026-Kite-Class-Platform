#!/usr/bin/env bash
# =============================================================================
# check-unresolved-env-vars.sh — fail when rendered output has unresolved {{var}} placeholder
# =============================================================================
#
# Per .claude/rules/markdown-variable-reference.md v1.0.0
# Sister script: scripts/render-env-vars.sh
#
# Usage:
#   bash scripts/render-env-vars.sh production env-reference.yaml input.md \
#     | bash scripts/check-unresolved-env-vars.sh
#
#   bash scripts/check-unresolved-env-vars.sh rendered-output.md
#
# Exits non-zero on first unresolved {{...}} placeholder (excluding escaped \{{...}}).
# Reports first 10 unresolved occurrences for triage.
#
# Per GAP-692 Phase 1 (Wave 102.8 Bucket B 2026-05-21)
# =============================================================================

set -euo pipefail

INPUT="${1:-/dev/stdin}"

CONTENT=$(cat "$INPUT")

# Scan for {{var_name}} patterns. Exclude lines where the placeholder is preceded by
# backslash (escaped literal per render-env-vars.sh §Escape).
UNRESOLVED=$(echo "$CONTENT" \
  | grep -oE '\\?\{\{[a-z_][a-z0-9_]*\}\}' \
  | grep -vE '^\\\{\{' \
  | head -10 || true)

if [ -n "$UNRESOLVED" ]; then
  echo "ERROR: unresolved env-var placeholders detected (first 10):" >&2
  echo "$UNRESOLVED" >&2
  echo "" >&2
  echo "Hint: ensure variable name exists in documents/02-architecture/env-reference.yaml" >&2
  echo "      per .claude/rules/markdown-variable-reference.md v1.0.0 §3 syntax." >&2
  exit 1
fi

echo "PASS: no unresolved {{...}} placeholders detected."
exit 0

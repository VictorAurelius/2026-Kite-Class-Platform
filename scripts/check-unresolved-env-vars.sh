#!/usr/bin/env bash
# scripts/check-unresolved-env-vars.sh — Fail if rendered markdown contains unresolved {{var}} references.
#
# Usage: check-unresolved-env-vars.sh <path>
#   path: file or directory to scan (recursive *.md)
#
# Exits 1 if ANY unresolved {{var}} found in scanned files.
# Per .claude/rules/markdown-variable-reference.md §4.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REF_YAML="${REPO_ROOT}/documents/02-architecture/env-reference.yaml"

[ "$#" -ge 1 ] || { echo "Usage: $0 <path>" >&2; exit 2; }
TARGET="$1"

# Collect markdown files
if [ -d "$TARGET" ]; then
  mapfile -d '' FILES < <(find "$TARGET" -type f -name '*.md' -print0)
elif [ -f "$TARGET" ]; then
  FILES=("$TARGET")
else
  echo "ERROR: path not found: $TARGET" >&2
  exit 2
fi

[ "${#FILES[@]}" -gt 0 ] || { echo "No markdown files in: $TARGET (skipping)"; exit 0; }

# Known vars (informational) — read từ env-reference.yaml cho error hint
KNOWN=""
if [ -f "$REF_YAML" ] && command -v python3 >/dev/null 2>&1; then
  KNOWN=$(python3 -c "
import yaml
with open('$REF_YAML') as f:
    data = yaml.safe_load(f)
print(', '.join(sorted((data.get('vars') or {}).keys())))
" 2>/dev/null || true)
fi

FAIL=0
for f in "${FILES[@]}"; do
  # Find unresolved {{var_name}} excluding escaped \{{var}}
  unresolved=$(python3 - "$f" <<'PY'
import re, sys
src = sys.argv[1]
with open(src, "r", encoding="utf-8") as fh:
    text = fh.read()
# Drop escaped \{{var}} so they don't match
text = re.sub(r"\\\{\{(\w+)\}\}", "", text)
hits = sorted(set(re.findall(r"\{\{(\w+)\}\}", text)))
for h in hits:
    print(h)
PY
)
  if [ -n "$unresolved" ]; then
    FAIL=1
    echo "FAIL: $f" >&2
    while read -r v; do
      echo "  unresolved: {{$v}}" >&2
    done <<< "$unresolved"
  fi
done

if [ "$FAIL" -ne 0 ]; then
  echo "" >&2
  echo "Unresolved env-var references detected." >&2
  echo "Either (a) render the doc first via scripts/render-env-vars.sh," >&2
  echo "       (b) add the var to documents/02-architecture/env-reference.yaml," >&2
  echo "    or (c) escape literal references as \\{{var}}." >&2
  if [ -n "$KNOWN" ]; then
    echo "Known vars: $KNOWN" >&2
  fi
  exit 1
fi

echo "PASS: scanned ${#FILES[@]} file(s); no unresolved env-var references." >&2

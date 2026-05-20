#!/usr/bin/env bash
# scripts/render-env-vars.sh — Render markdown doc với env-specific value substitution.
#
# Usage: render-env-vars.sh <env> <source.md> [<output.md>]
# Env values: prod | test | dev (per documents/02-architecture/env-reference.yaml)
#
# Substitution syntax (mkdocs-macros compatible):
#   {{var_name}}      → replaced với value từ env-reference.yaml cho given env
#   \{{var_name}}     → stays literal (backslash escape)
#
# Per .claude/rules/markdown-variable-reference.md §3.
#
# Reads YAML via `yq` (preferred) or Python PyYAML fallback (local dev convenience).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REF_YAML="${REPO_ROOT}/documents/02-architecture/env-reference.yaml"

usage() {
  cat >&2 <<'EOF'
Usage: render-env-vars.sh <env> <source.md> [<output.md>]

  env        prod | test | dev
  source.md  markdown source file containing {{var_name}} references
  output.md  optional; default = stdout

Examples:
  bash scripts/render-env-vars.sh prod docs/source.md /tmp/rendered.md
  bash scripts/render-env-vars.sh dev docs/source.md          # stdout

Reference: .claude/rules/markdown-variable-reference.md
EOF
  exit 2
}

[ "$#" -ge 2 ] || usage

ENV="$1"
SRC="$2"
OUT="${3:-/dev/stdout}"

case "$ENV" in
  prod|test|dev) ;;
  *) echo "ERROR: env must be prod|test|dev (got '$ENV')" >&2; exit 2 ;;
esac

[ -f "$SRC" ] || { echo "ERROR: source file not found: $SRC" >&2; exit 2; }
[ -f "$REF_YAML" ] || { echo "ERROR: env-reference.yaml not found: $REF_YAML" >&2; exit 2; }

# Build var → value map for chosen env. Output: one "key<TAB>value" per line.
build_var_map() {
  if command -v yq >/dev/null 2>&1; then
    # yq v4 (Mike Farah Go-based)
    yq eval ".vars | to_entries | .[] | .key + \"\t\" + (.value.${ENV} // \"\")" "$REF_YAML"
  elif command -v python3 >/dev/null 2>&1; then
    python3 - "$REF_YAML" "$ENV" <<'PY'
import sys
import yaml
ref_path, env = sys.argv[1], sys.argv[2]
with open(ref_path, "r", encoding="utf-8") as f:
    data = yaml.safe_load(f)
for key, spec in (data.get("vars") or {}).items():
    val = spec.get(env, "") if isinstance(spec, dict) else ""
    print(f"{key}\t{val}")
PY
  else
    echo "ERROR: neither 'yq' nor 'python3' (with PyYAML) found. Install one:" >&2
    echo "  yq:     https://github.com/mikefarah/yq/releases/latest" >&2
    echo "  python: apt-get install python3-yaml" >&2
    exit 3
  fi
}

# Build var map as JSON via Python (safe transport for values containing dots/colons/slashes).
VAR_MAP_JSON=$(build_var_map | python3 -c "
import json, sys
m = {}
for line in sys.stdin:
    line = line.rstrip('\n')
    if not line or '\t' not in line:
        continue
    k, v = line.split('\t', 1)
    m[k] = v
print(json.dumps(m))
")

render_to() {
  local out_file="$1"
  VAR_MAP_JSON="$VAR_MAP_JSON" python3 - "$out_file" "$SRC" <<'PY'
import json
import os
import re
import sys

var_map = json.loads(os.environ["VAR_MAP_JSON"])
out_path, src_path = sys.argv[1], sys.argv[2]

with open(src_path, "r", encoding="utf-8") as f:
    text = f.read()

ESC_OPEN = "\x00ESC_OPEN\x00"
ESC_CLOSE = "\x00ESC_CLOSE\x00"

# Step 1: protect \{{var}} escape
text = re.sub(r"\\\{\{(\w+)\}\}", lambda m: f"{ESC_OPEN}{m.group(1)}{ESC_CLOSE}", text)

# Step 2: substitute {{var}} (unknown vars left as-is for validator to catch)
def repl(m):
    name = m.group(1)
    return var_map.get(name, m.group(0))

text = re.sub(r"\{\{(\w+)\}\}", repl, text)

# Step 3: restore protected escapes as literal {{var}}
text = text.replace(ESC_OPEN, "{{").replace(ESC_CLOSE, "}}")

if out_path == "/dev/stdout":
    sys.stdout.write(text)
else:
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(text)
PY
}

render_to "$OUT"

if [ "$OUT" != "/dev/stdout" ]; then
  echo "Rendered: $SRC → $OUT (env=$ENV)" >&2
fi

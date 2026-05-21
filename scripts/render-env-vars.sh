#!/usr/bin/env bash
# =============================================================================
# render-env-vars.sh — substitute {{var_name}} placeholders với values từ env-reference.yaml per env
# =============================================================================
#
# Per .claude/rules/markdown-variable-reference.md v1.0.0
# Sister script: scripts/check-unresolved-env-vars.sh
# Canonical source: documents/02-architecture/env-reference.yaml
#
# Usage:
#   bash scripts/render-env-vars.sh <env> <yaml> <input.md> [output.md]
#
# Example:
#   bash scripts/render-env-vars.sh production \
#     documents/02-architecture/env-reference.yaml \
#     documents/05-guides/deploy/release-1-deploy-plan.md \
#     /tmp/rendered.md
#
# Escape: \{{var_name}} renders as literal {{var_name}} (no substitution).
#
# Per GAP-692 Phase 1 (Wave 102.8 Bucket B 2026-05-21)
# =============================================================================

set -euo pipefail

ENV="${1:-}"
YAML="${2:-}"
INPUT="${3:-}"
OUTPUT="${4:-/dev/stdout}"

usage() {
  cat <<EOF
Usage: bash scripts/render-env-vars.sh <env> <yaml> <input.md> [output.md]

Arguments:
  env       Environment key (production / test / dev) — must match yaml schema
  yaml      Path to env-reference.yaml
  input.md  Source markdown/text file containing {{var_name}} placeholders
  output.md Destination (default: stdout)

Per .claude/rules/markdown-variable-reference.md v1.0.0
EOF
  exit 1
}

if [ -z "$ENV" ] || [ -z "$YAML" ] || [ -z "$INPUT" ]; then
  usage
fi

if [ ! -f "$YAML" ]; then
  echo "ERROR: yaml file not found: $YAML" >&2
  exit 1
fi

if [ ! -f "$INPUT" ]; then
  echo "ERROR: input file not found: $INPUT" >&2
  exit 1
fi

# Extract variables from YAML using Python (PyYAML stdlib alternative to yq).
# yq not always available in dev environments; Python yaml is broadly available.
# Output format: one "name=value" per line for shell consumption.
EXTRACTED=$(python3 - "$YAML" "$ENV" <<'PYEOF'
import sys
import yaml

yaml_path = sys.argv[1]
env_key = sys.argv[2]

with open(yaml_path, 'r', encoding='utf-8') as f:
    data = yaml.safe_load(f)

variables = data.get('variables', {})
if not variables:
    sys.stderr.write(f"ERROR: no 'variables' key in {yaml_path}\n")
    sys.exit(1)

for name, spec in variables.items():
    if not isinstance(spec, dict):
        continue
    if env_key not in spec:
        sys.stderr.write(f"WARN: variable '{name}' missing env '{env_key}'\n")
        continue
    value = spec.get(env_key, '')
    # Print "name<TAB>value" to allow values containing '=' or spaces
    print(f"{name}\t{value}")
PYEOF
) || { echo "ERROR: yaml extraction failed" >&2; exit 1; }

# Apply substitutions via Python to handle escape \{{...}} correctly.
# Bash sed approaches struggle with escape semantics + special chars in values.
python3 - "$INPUT" "$OUTPUT" <<PYEOF
import sys
import re

input_path = sys.argv[1]
output_path = sys.argv[2]

# Parse extracted variables from stdin-equivalent (passed via env)
vars_raw = """$EXTRACTED"""
variables = {}
for line in vars_raw.strip().split('\n'):
    if not line or '\t' not in line:
        continue
    name, value = line.split('\t', 1)
    variables[name] = value

with open(input_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Step 1: protect escaped \{{var}} occurrences with sentinel
SENTINEL_OPEN = '\x00ESC_OPEN\x00'
SENTINEL_CLOSE = '\x00ESC_CLOSE\x00'
content = content.replace(r'\{{', SENTINEL_OPEN).replace(r'\}}', SENTINEL_CLOSE)

# Step 2: substitute {{var_name}} → value
def replace_var(match):
    var_name = match.group(1)
    if var_name in variables:
        return variables[var_name]
    # Leave unresolved placeholders for check-unresolved-env-vars.sh to catch
    return match.group(0)

content = re.sub(r'\{\{([a-z_][a-z0-9_]*)\}\}', replace_var, content)

# Step 3: restore escaped sequences as literal {{...}}
content = content.replace(SENTINEL_OPEN, '{{').replace(SENTINEL_CLOSE, '}}')

# Write output
if output_path == '/dev/stdout':
    sys.stdout.write(content)
else:
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(content)
PYEOF

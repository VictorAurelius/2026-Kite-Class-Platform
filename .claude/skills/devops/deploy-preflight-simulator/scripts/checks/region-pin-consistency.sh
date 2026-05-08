#!/usr/bin/env bash
# Check #5 — Region pin consistency
# Standard: Twelve-Factor App Factor V (Build/release/run separation) + AWS Well-Architected OPS-04
#
# Verify the canonical AWS region (declared in variables.tf default) is consistent across:
#   - .github/workflows/*.yml      env.AWS_REGION + aws-region: + --region flags
#   - infrastructure/terraform-aws/variables.tf  (canonical source)
#   - documents/05-guides/deploy/*.md  runbook references
#   - kitehub/scripts/*.sh         AWS CLI invocations
#
# Detection: any file referencing a DIFFERENT region (e.g. us-east-1, eu-west-1, ap-northeast-1)
# while the canonical region is set elsewhere → FAIL with file:line.
set -uo pipefail

VARIABLES_TF="infrastructure/terraform-aws/variables.tf"
[[ -f "$VARIABLES_TF" ]] || { echo "  └─ $VARIABLES_TF not found (skip)"; exit 0; }

# Canonical region = aws_region variable default
CANONICAL=$(awk '/variable "aws_region"/,/^}/' "$VARIABLES_TF" \
  | grep -oE 'default\s*=\s*"[a-z]+-[a-z]+-[0-9]+"' \
  | head -1 \
  | sed -E 's/.*"([^"]+)".*/\1/')

if [[ -z "$CANONICAL" ]]; then
  echo "  └─ no aws_region default in $VARIABLES_TF (skip)"
  exit 0
fi

ISSUES=0
WARN_ISSUES=0

# Region regex (AWS region naming: 2-letter prefix + word + 1-digit, e.g. ap-southeast-1)
REGION_RE='\b(af|ap|ca|eu|me|sa|us)-(north|south|east|west|central|northeast|northwest|southeast|southwest)-[0-9]+\b'

scan_dir_files() {
  local dir="$1"
  local glob="$2"
  local label="$3"
  local f
  shopt -s nullglob
  # shellcheck disable=SC2206  # intentional glob expansion of $glob (e.g. "*.yml")
  local files=( "${dir}"/${glob} )
  shopt -u nullglob
  if [[ ${#files[@]} -eq 0 ]]; then
    return 0
  fi
  for f in "${files[@]}"; do
    [[ -f "$f" ]] || continue
    # Find every region match (file:line:region)
    while IFS= read -r match; do
      [[ -z "$match" ]] && continue
      local line region context stripped
      line=$(echo "$match" | cut -d: -f1)
      # The matched line content (everything after first colon)
      context=$(echo "$match" | cut -d: -f2-)
      # Skip pure-comment lines (terraform `#`/`//`, shell `#`, markdown blockquote `>`)
      stripped=$(echo "$context" | sed -E 's/^[[:space:]]+//')
      case "$stripped" in
        '#'*|'//'*|'>'*) continue ;;
      esac
      region=$(echo "$context" | grep -oE "$REGION_RE" | head -1)
      [[ -z "$region" ]] && continue
      if [[ "$region" != "$CANONICAL" ]]; then
        # Distinguish hard-FAIL paths (workflows, terraform, scripts) from soft-WARN paths (docs)
        case "$label" in
          docs)
            echo "  ⚠️  $f:$line: references region \`$region\` (canonical: \`$CANONICAL\`)"
            echo "       context: $(echo "$context" | sed 's/^[[:space:]]*//' | cut -c1-80)"
            echo "       fix: prefer canonical region OR mark line as future-DR/multi-region note"
            echo "       standard: Twelve-Factor App Factor V"
            WARN_ISSUES=$((WARN_ISSUES + 1))
            ;;
          *)
            echo "  ❌ FAIL $f:$line: references region \`$region\` (canonical: \`$CANONICAL\` per $VARIABLES_TF)"
            echo "       context: $(echo "$context" | sed 's/^[[:space:]]*//' | cut -c1-80)"
            echo "       fix: align $label region pin to \`$CANONICAL\` OR update terraform variables.tf default"
            echo "       standard: AWS Well-Architected OPS-04 (Operational Excellence — pinned config)"
            ISSUES=$((ISSUES + 1))
            ;;
        esac
      fi
    done < <(grep -nE "$REGION_RE" "$f" 2>/dev/null || true)
  done
}

scan_dir_files ".github/workflows" "*.yml" "workflow"
scan_dir_files "infrastructure/terraform-aws" "*.tf" "terraform"
scan_dir_files "documents/05-guides/deploy" "*.md" "docs"
scan_dir_files "kitehub/scripts" "*.sh" "script"

if [[ $ISSUES -gt 0 ]]; then
  echo "  └─ $ISSUES FAIL + $WARN_ISSUES WARN — region drift detected (canonical: \`$CANONICAL\`)"
  exit 2
fi
if [[ $WARN_ISSUES -gt 0 ]]; then
  echo "  └─ $WARN_ISSUES WARN — non-canonical regions in docs (often future-DR notes; review manually)"
  exit 1
fi
echo "  └─ all region pins match canonical \`$CANONICAL\`"
exit 0

#!/usr/bin/env bash
# Check #4 — Secret naming drift
# Standard: Twelve-Factor App Factor III (Config) + AWS Secrets Manager naming consistency
#
# Cross-grep secret IDs in 4 sources, report mismatches:
#   A: infrastructure/terraform-aws/secrets.tf      (resource definitions)
#   B: scripts/populate-secrets.sh                  (helper script values)
#   C: documents/05-guides/deploy/secrets-populate-phase-2-4.md (runbook references)
#   D: infrastructure/terraform-aws/iam.tf          (Resource ARN patterns)
#
# Per .claude/rules/agent-aws-access.md: read-only static check, no AWS API calls.
set -uo pipefail

SECRETS_TF="infrastructure/terraform-aws/secrets.tf"
POPULATE_SH="scripts/populate-secrets.sh"
RUNBOOK="documents/05-guides/deploy/secrets-populate-phase-2-4.md"
IAM_TF="infrastructure/terraform-aws/iam.tf"
VARIABLES_TF="infrastructure/terraform-aws/variables.tf"

ISSUES=0
WARN_ISSUES=0

# Skip gracefully if any source missing
for f in "$SECRETS_TF" "$POPULATE_SH" "$RUNBOOK" "$IAM_TF"; do
  if [[ ! -f "$f" ]]; then
    echo "  └─ $f not found (skip)"
    exit 0
  fi
done

# Resolve terraform variable defaults to literals so we can compare apples-to-apples
PROJECT_NAME="kitehub"
ENVIRONMENT="production"
if [[ -f "$VARIABLES_TF" ]]; then
  PN=$(awk '/variable "project_name"/,/^}/' "$VARIABLES_TF" | grep -oE 'default\s*=\s*"[^"]+"' | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
  EN=$(awk '/variable "environment"/,/^}/'  "$VARIABLES_TF" | grep -oE 'default\s*=\s*"[^"]+"' | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
  [[ -n "$PN" ]] && PROJECT_NAME="$PN"
  [[ -n "$EN" ]] && ENVIRONMENT="$EN"
fi

# Source A — extract secret names from terraform secrets.tf
# Resolve ${var.project_name}/${var.environment} → literal
SECRETS_A=$(grep -hE '^\s*name\s*=\s*"[^"]*\$\{var\.project_name\}' "$SECRETS_TF" 2>/dev/null \
  | sed -E 's/.*name\s*=\s*"([^"]+)".*/\1/' \
  | sed -E "s|\\\$\\{var\\.project_name\\}|${PROJECT_NAME}|g" \
  | sed -E "s|\\\$\\{var\\.environment\\}|${ENVIRONMENT}|g" \
  | grep -vE '\$\{each\.key\}' \
  | sort -u)

# Plus the for_each placeholder set — resolve each.key from local.placeholder_secrets
PLACEHOLDER_KEYS=$(awk '/placeholder_secrets\s*=\s*{/,/^\s*}/' "$SECRETS_TF" 2>/dev/null \
  | grep -oE '"[a-z0-9][a-z0-9-]*"\s*=' \
  | sed -E 's/.*"([^"]+)".*/\1/' \
  | sort -u)

for key in $PLACEHOLDER_KEYS; do
  SECRETS_A+=$'\n'"${PROJECT_NAME}/${ENVIRONMENT}/${key}"
done
SECRETS_A=$(echo "$SECRETS_A" | grep -v '^$' | sort -u)

# Source B — extract from populate-secrets.sh (kite/<env>/<id> pattern in code, comments OK)
SECRETS_B=$(grep -hoE 'kite[a-z]*/[a-z0-9][a-z0-9/_-]+' "$POPULATE_SH" 2>/dev/null \
  | grep -vE '^kite/staging/?$|^kite/prod/?$' \
  | sort -u)

# Source C — extract from runbook (backtick-quoted kite/...)
SECRETS_C=$(grep -hoE '`kite[a-z]*/[a-z0-9][a-z0-9/_-]+`' "$RUNBOOK" 2>/dev/null \
  | tr -d '`' \
  | grep -vE '^kite/staging/?$|^kite/prod/?$|^kite/staging/\*$|^kite/prod/\*$' \
  | sort -u)

# Source D — extract from iam.tf secret: ARN patterns (resolve var.project_name + var.environment)
SECRETS_D=$(grep -hoE 'secret:[^"]+' "$IAM_TF" 2>/dev/null \
  | sed -E 's|^secret:||' \
  | sed -E "s|\\\$\\{var\\.project_name\\}|${PROJECT_NAME}|g" \
  | sed -E "s|\\\$\\{var\\.environment\\}|${ENVIRONMENT}|g" \
  | sort -u)

if [[ -z "$SECRETS_A" ]]; then
  echo "  └─ no secret resources in $SECRETS_TF (skip)"
  exit 0
fi

# Helper: check if a literal name is "covered" by any pattern (literal or wildcard) in a list
covered_by() {
  local needle="$1"
  local haystack="$2"
  while IFS= read -r pat; do
    [[ -z "$pat" ]] && continue
    # Convert literal regex metachars → escaped, then wildcard `*` → `.*`
    # (handle each metachar group with its own sed expression to avoid character-class pitfalls)
    local regex="$pat"
    regex=${regex//\\/\\\\}
    regex=${regex//./\\.}
    regex=${regex//+/\\+}
    regex=${regex//?/\\?}
    regex=${regex//(/\\(}
    regex=${regex//)/\\)}
    regex=${regex//\{/\\\{}
    regex=${regex//\}/\\\}}
    regex=${regex//|/\\|}
    regex=${regex//^/\\^}
    regex=${regex//\$/\\\$}
    regex=${regex//\*/.*}
    if [[ "$needle" =~ ^${regex}$ ]]; then
      return 0
    fi
  done <<< "$haystack"
  return 1
}

# 1. Cross-check Source A (terraform-defined) vs Source B (script) — orphan in script
while IFS= read -r script_secret; do
  [[ -z "$script_secret" ]] && continue
  # Strip trailing slash if any
  script_secret="${script_secret%/}"
  # Skip if it is an ARN-pattern wildcard like "kite/prod/*" — those are scope hints
  [[ "$script_secret" == *"*"* ]] && continue
  # Allow wildcard match against either Source A (literal terraform) or Source D (IAM ARN pattern)
  if ! covered_by "$script_secret" "$SECRETS_A" && ! covered_by "$script_secret" "$SECRETS_D"; then
    LINE=$(grep -n -F "$script_secret" "$POPULATE_SH" 2>/dev/null | head -1 | cut -d: -f1)
    echo "  ❌ FAIL $POPULATE_SH:${LINE:-?}: secret \`$script_secret\` not defined in $SECRETS_TF or matched by IAM ARN pattern"
    echo "       terraform-defined names use \`${PROJECT_NAME}/${ENVIRONMENT}/<id>\` namespace; got \`${script_secret}\`"
    echo "       fix: align populate-secrets.sh secret IDs with terraform names (rename in script OR update terraform)"
    echo "       standard: Twelve-Factor App Factor III (Config) — single source of truth for config keys"
    ISSUES=$((ISSUES + 1))
  fi
done <<< "$SECRETS_B"

# 2. Cross-check runbook references (Source C) — orphan in docs
while IFS= read -r doc_secret; do
  [[ -z "$doc_secret" ]] && continue
  doc_secret="${doc_secret%/}"
  [[ "$doc_secret" == *"*"* ]] && continue
  if ! covered_by "$doc_secret" "$SECRETS_A" && ! covered_by "$doc_secret" "$SECRETS_D"; then
    LINE=$(grep -n -F "$doc_secret" "$RUNBOOK" 2>/dev/null | head -1 | cut -d: -f1)
    echo "  ⚠️  $RUNBOOK:${LINE:-?}: runbook references \`$doc_secret\` but not defined in $SECRETS_TF or matched by IAM ARN"
    echo "       fix: update runbook to use canonical name \`${PROJECT_NAME}/${ENVIRONMENT}/<id>\` OR add resource to terraform"
    echo "       standard: Twelve-Factor Factor III"
    WARN_ISSUES=$((WARN_ISSUES + 1))
  fi
done <<< "$SECRETS_C"

# 3. Sanity-check: terraform-defined secrets should be reachable by IAM ARN patterns (Source D)
while IFS= read -r tf_secret; do
  [[ -z "$tf_secret" ]] && continue
  if ! covered_by "$tf_secret" "$SECRETS_D"; then
    echo "  ⚠️  $IAM_TF: terraform secret \`$tf_secret\` not covered by any IAM Resource ARN pattern"
    echo "       fix: add ARN pattern \`arn:aws:secretsmanager:*:*:secret:${PROJECT_NAME}/${ENVIRONMENT}/*\` to relevant policy"
    echo "       standard: NIST SP 800-53 AC-6 + Twelve-Factor Factor III"
    WARN_ISSUES=$((WARN_ISSUES + 1))
  fi
done <<< "$SECRETS_A"

if [[ $ISSUES -gt 0 ]]; then
  echo "  └─ $ISSUES FAIL + $WARN_ISSUES WARN — orphan secrets need rename or terraform alignment"
  exit 2
fi
if [[ $WARN_ISSUES -gt 0 ]]; then
  echo "  └─ $WARN_ISSUES WARN — runbook/IAM coverage drift"
  exit 1
fi
echo "  └─ all secret IDs consistent across terraform / script / runbook / IAM"
exit 0

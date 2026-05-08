#!/usr/bin/env bash
# Check #2 — IAM resource ARN naming drift
# Standard: NIST SP 800-53 AC-6 + AWS IAM Best Practices + HashiCorp Terraform style
set -uo pipefail

IAM_TF="infrastructure/terraform-aws/iam.tf"
ECR_TF="infrastructure/terraform-aws/ecr.tf"
[[ -f $IAM_TF ]] || { echo "  └─ $IAM_TF not found (skip)"; exit 0; }
[[ -f $ECR_TF ]] || { echo "  └─ $ECR_TF not found (skip)"; exit 0; }

ISSUES=0

# Cross-check ECR repos: extract Resource patterns matching repository/* in iam.tf
ECR_RESOURCES=$(grep -oE 'arn:aws:ecr:[^"]*repository/[^"]*' "$IAM_TF" | sort -u)
ECR_REPOS=$(grep -oE '"kite/[a-z][a-z0-9-]+"' "$ECR_TF" | tr -d '"' | sort -u)

if [[ -z "$ECR_REPOS" ]]; then
  echo "  └─ no ECR repos defined in $ECR_TF (skip)"
  exit 0
fi

for resource_pattern in $ECR_RESOURCES; do
  # Extract just the repository part: everything after `repository/`
  RESOURCE_PATH=$(sed -E 's|.*repository/||' <<< "$resource_pattern")
  # Resolve ${var.project_name} to literal "kitehub" (per terraform variables.tf)
  RESOLVED=$(sed -E 's|\$\{var\.project_name\}|kitehub|g' <<< "$RESOURCE_PATH")
  # Convert wildcard to regex: * → .*
  REGEX=$(sed -E 's|\*|.*|g' <<< "$RESOLVED")

  # Test each actual ECR repo name against this pattern
  MATCHED=0
  TOTAL=0
  for repo in $ECR_REPOS; do
    TOTAL=$((TOTAL + 1))
    if [[ "$repo" =~ ^${REGEX}$ ]]; then
      MATCHED=$((MATCHED + 1))
    fi
  done

  if [[ $MATCHED -eq 0 ]]; then
    LINE=$(grep -n "$resource_pattern" "$IAM_TF" 2>/dev/null | head -1 | cut -d: -f1)
    echo "  ❌ FAIL $IAM_TF:${LINE:-?}: pattern \`$RESOLVED\` matches 0/$TOTAL ECR repos"
    echo "       actual ECR repo names use \`kite/\` namespace prefix:"
    echo "$ECR_REPOS" | sed 's/^/         - /' | head -3
    echo "       fix: change Resource pattern to \`repository/kite/*\` (least-privilege, matches all)"
    echo "       standard: NIST SP 800-53 AC-6 (Least Privilege)"
    ISSUES=$((ISSUES + 1))
  elif [[ $MATCHED -lt $TOTAL ]]; then
    echo "  ⚠️  $IAM_TF: pattern \`$RESOLVED\` matches only $MATCHED/$TOTAL ECR repos"
    ISSUES=$((ISSUES + 1))
  fi
done

# Cross-check Secrets Manager paths
SECRETS_RESOURCES=$(grep -oE 'arn:aws:secretsmanager:[^"]*secret:[^"]*' "$IAM_TF" | sort -u)
if [[ -n "$SECRETS_RESOURCES" ]]; then
  # Verify pattern uses proper namespace per CLAUDE.md
  for sec_pattern in $SECRETS_RESOURCES; do
    if ! grep -qE 'kite/' <<< "$sec_pattern" && ! grep -qE 'project_name' <<< "$sec_pattern"; then
      echo "  ⚠️  Secrets Manager pattern \`$sec_pattern\` may not follow kite/ namespace convention"
    fi
  done
fi

if [[ $ISSUES -gt 0 ]]; then
  echo "  └─ $ISSUES IAM ARN drift issue(s)"
  exit 2
fi
echo "  └─ all IAM Resource patterns match actual resource ARNs"
exit 0

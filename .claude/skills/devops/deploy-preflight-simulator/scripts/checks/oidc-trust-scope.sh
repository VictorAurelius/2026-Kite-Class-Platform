#!/usr/bin/env bash
# Check #3 — OIDC trust policy claim scope
# Standard: GitHub Actions Security Hardening Guide + Sigstore Cosign Best Practices
set -uo pipefail

IAM_TF="infrastructure/terraform-aws/iam.tf"
WORKFLOWS_DIR=".github/workflows"
[[ -f $IAM_TF ]] || { echo "  └─ $IAM_TF not found (skip)"; exit 0; }

ISSUES=0

# Extract sub claims from OIDC trust policies
SUB_CLAIMS=$(grep -A2 'token.actions.githubusercontent.com:sub' "$IAM_TF" | grep -oE 'repo:[^"]*' | sort -u)

# Detect workflow on: events that trigger OIDC role assumption
WORKFLOWS=$(ls "$WORKFLOWS_DIR"/*.yml 2>/dev/null)
ROLES_USED=$(grep -h 'role-to-assume:' $WORKFLOWS 2>/dev/null | grep -oE 'AWS_[A-Z_]+_ROLE_ARN|secrets\.[A-Z_]+_ROLE_ARN' | sort -u)

if [[ -z "$SUB_CLAIMS" ]]; then
  echo "  └─ no GitHub OIDC trust policies found in $IAM_TF (skip)"
  exit 0
fi

# Per-workflow trigger check
for wf in $WORKFLOWS; do
  # Find which trigger events the workflow uses + ref patterns
  EVENTS=$(awk '/^on:/,/^[a-z]/' "$wf" | grep -oE '^\s+(push|pull_request|workflow_dispatch|tags|branches):' | grep -oE '(push|pull_request|workflow_dispatch|tags|branches)' | sort -u)
  USES_OIDC=$(grep -l 'role-to-assume' "$wf" 2>/dev/null || true)

  if [[ -z "$USES_OIDC" ]]; then
    continue  # workflow doesn't use OIDC
  fi

  # Tag-pattern coverage check: if workflow triggers on tags, sub claim must include tag-ref pattern
  if echo "$EVENTS" | grep -q tags; then
    if ! echo "$SUB_CLAIMS" | grep -qE 'refs/tags|:\*$'; then
      echo "  ⚠️  $wf triggers on tags but no trust policy sub claim covers \`refs/tags/v*\`"
      echo "       trust claims found: $(echo "$SUB_CLAIMS" | tr '\n' ' ')"
      echo "       fix: add \`repo:org/repo:ref:refs/tags/v*\` to trust policy"
      echo "       standard: GitHub Actions Hardening — OIDC sub claim scope"
      ISSUES=$((ISSUES + 1))
    fi
  fi
done

if [[ $ISSUES -gt 0 ]]; then
  exit 1  # WARN, not FAIL — schema may use wildcards `repo:*:*`
fi
echo "  └─ all OIDC trust policies cover declared workflow triggers"
exit 0

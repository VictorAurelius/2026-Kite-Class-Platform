#!/usr/bin/env bash
#
# test-secret-rotation.sh
#
# Integration test for Wave 84 Bucket B (GAP-379) Secrets Manager rotation.
#
# What it does:
#   1. Pick an in-house secret under rotation (default: jwt-secret).
#   2. Capture AWSCURRENT version id BEFORE rotation.
#   3. Trigger ad-hoc rotation via `aws secretsmanager rotate-secret`.
#   4. Poll until a new AWSCURRENT version id appears (max ~2 min).
#   5. Verify AWSPREVIOUS now holds the original version id (versioning intact).
#
# Prerequisites:
#   - terraform apply landed Wave 84 Bucket B changes (Lambda + rotation wiring).
#   - AWS_PROFILE / AWS_DEFAULT_REGION set (or pass --profile / --region).
#   - IAM identity has secretsmanager:DescribeSecret + RotateSecret + ListSecretVersionIds.
#
# Usage:
#   bash scripts/test-secret-rotation.sh                          # default jwt-secret
#   bash scripts/test-secret-rotation.sh encryption-key
#   bash scripts/test-secret-rotation.sh seed-admin-password
#   bash scripts/test-secret-rotation.sh --region us-east-1 jwt-secret
#
# Safe by default: this script ONLY targets the 3 in-house secrets registered
# in the Lambda generator (jwt-secret / encryption-key / seed-admin-password).
# It refuses to rotate db-password (RDS managed) or vendor API keys (manual).

set -euo pipefail

SECRET_KIND="jwt-secret"
AWS_FLAGS=()
PROJECT="${KITE_PROJECT:-kitehub}"
ENVIRONMENT="${KITE_ENV:-production}"
MAX_POLL_SECONDS=180
POLL_INTERVAL=5

ALLOWED_KINDS=("jwt-secret" "encryption-key" "seed-admin-password")

usage() {
  cat <<EOF
Usage: $0 [--profile PROFILE] [--region REGION] [--project NAME] [--env ENV] [SECRET_KIND]

SECRET_KIND (default: jwt-secret) must be one of:
  ${ALLOWED_KINDS[*]}

Environment overrides:
  KITE_PROJECT    project name prefix (default: kitehub)
  KITE_ENV        environment (default: production)
  AWS_PROFILE     AWS CLI profile (or use --profile)
  AWS_DEFAULT_REGION  AWS region (or use --region)
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --profile) AWS_FLAGS+=(--profile "$2"); shift 2 ;;
    --region) AWS_FLAGS+=(--region "$2"); shift 2 ;;
    --project) PROJECT="$2"; shift 2 ;;
    --env) ENVIRONMENT="$2"; shift 2 ;;
    --*) echo "Unknown flag: $1" >&2; usage; exit 2 ;;
    *) SECRET_KIND="$1"; shift ;;
  esac
done

is_allowed=false
for k in "${ALLOWED_KINDS[@]}"; do
  if [[ "$SECRET_KIND" == "$k" ]]; then is_allowed=true; break; fi
done
if [[ "$is_allowed" != true ]]; then
  echo "ERROR: '$SECRET_KIND' is not a custom-rotated secret." >&2
  echo "Allowed: ${ALLOWED_KINDS[*]}" >&2
  echo "(db-password uses AWS-managed rotation; vendor keys are manual.)" >&2
  exit 2
fi

SECRET_ID="${PROJECT}/${ENVIRONMENT}/${SECRET_KIND}"
echo "==> Testing rotation for $SECRET_ID"

# 0) Sanity check: secret exists and rotation is enabled.
metadata=$(aws "${AWS_FLAGS[@]}" secretsmanager describe-secret \
  --secret-id "$SECRET_ID" \
  --output json)

rotation_enabled=$(echo "$metadata" | python3 -c "import sys, json; d=json.load(sys.stdin); print(str(d.get('RotationEnabled', False)).lower())")
if [[ "$rotation_enabled" != "true" ]]; then
  echo "ERROR: rotation NOT enabled on $SECRET_ID" >&2
  echo "Run terraform apply first (Wave 84 Bucket B) to wire aws_secretsmanager_secret_rotation." >&2
  exit 1
fi

before_current=$(aws "${AWS_FLAGS[@]}" secretsmanager describe-secret \
  --secret-id "$SECRET_ID" \
  --query 'VersionIdsToStages' --output json \
  | python3 -c "import sys, json; d=json.load(sys.stdin); print([v for v,s in d.items() if 'AWSCURRENT' in s][0])")

echo "    AWSCURRENT (before): $before_current"

# 1) Trigger rotation.
echo "==> Triggering rotation..."
aws "${AWS_FLAGS[@]}" secretsmanager rotate-secret \
  --secret-id "$SECRET_ID" \
  --output json >/dev/null

# 2) Poll for new AWSCURRENT.
echo "==> Polling for new AWSCURRENT (max ${MAX_POLL_SECONDS}s)..."
elapsed=0
new_current=""
while [[ $elapsed -lt $MAX_POLL_SECONDS ]]; do
  sleep $POLL_INTERVAL
  elapsed=$((elapsed + POLL_INTERVAL))
  candidate=$(aws "${AWS_FLAGS[@]}" secretsmanager describe-secret \
    --secret-id "$SECRET_ID" \
    --query 'VersionIdsToStages' --output json \
    | python3 -c "import sys, json; d=json.load(sys.stdin); cur=[v for v,s in d.items() if 'AWSCURRENT' in s]; print(cur[0] if cur else '')")
  if [[ -n "$candidate" && "$candidate" != "$before_current" ]]; then
    new_current="$candidate"
    echo "    AWSCURRENT (after):  $new_current  (elapsed: ${elapsed}s)"
    break
  fi
  echo "    ...still $before_current (elapsed: ${elapsed}s)"
done

if [[ -z "$new_current" ]]; then
  echo "ERROR: AWSCURRENT did not advance within ${MAX_POLL_SECONDS}s" >&2
  echo "Check Lambda logs: aws logs tail /aws/lambda/${PROJECT}-${ENVIRONMENT}-rotate-secret-handler" >&2
  exit 1
fi

# 3) Verify AWSPREVIOUS == old version.
previous=$(aws "${AWS_FLAGS[@]}" secretsmanager describe-secret \
  --secret-id "$SECRET_ID" \
  --query 'VersionIdsToStages' --output json \
  | python3 -c "import sys, json; d=json.load(sys.stdin); prev=[v for v,s in d.items() if 'AWSPREVIOUS' in s]; print(prev[0] if prev else '')")

if [[ "$previous" == "$before_current" ]]; then
  echo "==> PASS: rotation completed."
  echo "    AWSCURRENT  = $new_current"
  echo "    AWSPREVIOUS = $previous (was AWSCURRENT before rotation — versioning intact)"
  exit 0
else
  echo "ERROR: AWSPREVIOUS ($previous) does NOT match the pre-rotation AWSCURRENT ($before_current)" >&2
  echo "Versioning chain broken; investigate before promoting." >&2
  exit 1
fi

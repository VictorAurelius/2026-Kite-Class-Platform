#!/usr/bin/env bash
# shellcheck disable=SC2059,SC2016
# SC2059: color escape codes (${C_FAIL}/${C_OK}/${C_RST}) in printf format are intentional
# SC2016: AWS JMESPath queries use backticks for string literals; single-quote required to prevent shell expansion
# smoke-aws-phase-2-3.sh — Phase 2.3 production infra read-only verification
#
# Usage: bash scripts/smoke-aws-phase-2-3.sh
#
# Tier 1 (read-only) only per .claude/rules/agent-aws-access.md §2.1.
# NO create/delete/modify; NO get-secret-value/decrypt.
# Output: pass/fail summary + suggested artifact path.
#
# Reference:
#   - .claude/rules/agent-aws-access.md
#   - .claude/skills/devops/aws-smoke-test/reference/phase-2-3-production.md
#   - documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md

set -euo pipefail

REGION="ap-southeast-1"
EXPECTED_ACCOUNT="906286017800"
TODAY="$(date -u +%Y-%m-%d)"
ARTIFACT_PATH="documents/04-quality/audits/aws-verification/${TODAY}-phase-2-3-smoke.md"

PASS=0
FAIL=0
WARN=0

# Colors (only if stdout is a TTY)
if [ -t 1 ]; then
  C_OK="\033[0;32m"
  C_FAIL="\033[0;31m"
  C_WARN="\033[0;33m"
  C_INFO="\033[0;36m"
  C_RST="\033[0m"
else
  C_OK=""; C_FAIL=""; C_WARN=""; C_INFO=""; C_RST=""
fi

ok()   { printf "  ${C_OK}PASS${C_RST}  %s\n" "$1"; PASS=$((PASS + 1)); }
fail() { printf "  ${C_FAIL}FAIL${C_RST}  %s\n" "$1"; FAIL=$((FAIL + 1)); }
warn() { printf "  ${C_WARN}WARN${C_RST}  %s\n" "$1"; WARN=$((WARN + 1)); }
info() { printf "${C_INFO}==>${C_RST} %s\n" "$1"; }

# Pre-flight: aws CLI present
if ! command -v aws >/dev/null 2>&1; then
  printf "${C_FAIL}ERROR${C_RST} aws CLI not found. Install AWS CLI v2 first.\n"
  exit 2
fi

info "Phase 2.3 production infra smoke test"
info "Region: ${REGION}  |  Expected account: ${EXPECTED_ACCOUNT}"
echo

# ---------- Section 0: Account verify ----------
info "Section 0 — Account verify"
ACTUAL_ACCOUNT="$(aws sts get-caller-identity --query 'Account' --output text 2>/dev/null || echo "")"
if [ -z "${ACTUAL_ACCOUNT}" ]; then
  fail "aws sts get-caller-identity — credentials not configured"
  printf "\n${C_FAIL}ABORT${C_RST}: cannot verify account; configure credentials and retry.\n"
  exit 2
fi
if [ "${ACTUAL_ACCOUNT}" != "${EXPECTED_ACCOUNT}" ]; then
  fail "Account mismatch: expected ${EXPECTED_ACCOUNT}, got ${ACTUAL_ACCOUNT}"
  printf "\n${C_FAIL}ABORT${C_RST}: wrong-account verification could leak cross-tenant data. Switch profile.\n"
  exit 2
fi
ok "Account = ${ACTUAL_ACCOUNT}"
echo

# ---------- Section A: VPC + Networking ----------
info "Section A — VPC + Networking"
VPC_ID="$(aws ec2 describe-vpcs --region "${REGION}" \
  --filters "Name=tag:Name,Values=kitehub-vpc" \
  --query 'Vpcs[0].VpcId' --output text 2>/dev/null || echo "None")"
if [ "${VPC_ID}" = "None" ] || [ -z "${VPC_ID}" ]; then
  fail "VPC kitehub-vpc not found"
else
  ok "VPC: ${VPC_ID}"
  SUBNET_COUNT="$(aws ec2 describe-subnets --region "${REGION}" \
    --filters "Name=vpc-id,Values=${VPC_ID}" \
    --query 'length(Subnets)' --output text 2>/dev/null || echo "0")"
  if [ "${SUBNET_COUNT}" = "4" ]; then
    ok "Subnets: 4 (2 public + 2 private)"
  else
    warn "Subnets: ${SUBNET_COUNT} (expected 4)"
  fi
  IGW="$(aws ec2 describe-internet-gateways --region "${REGION}" \
    --filters "Name=attachment.vpc-id,Values=${VPC_ID}" \
    --query 'InternetGateways[0].InternetGatewayId' --output text 2>/dev/null || echo "None")"
  if [ "${IGW}" != "None" ] && [ -n "${IGW}" ]; then
    ok "Internet Gateway: ${IGW}"
  else
    fail "Internet Gateway not attached to VPC"
  fi
fi
echo

# ---------- Section B: EC2 + ALB ----------
info "Section B — EC2 + ALB"
EC2_RUNNING="$(aws ec2 describe-instances --region "${REGION}" \
  --filters "Name=tag:Project,Values=kitehub" "Name=instance-state-name,Values=running" \
  --query 'length(Reservations[].Instances[])' --output text 2>/dev/null || echo "0")"
if [ "${EC2_RUNNING}" -ge 2 ] 2>/dev/null; then
  ok "EC2 running: ${EC2_RUNNING} instance(s)"
else
  warn "EC2 running: ${EC2_RUNNING} (expected 2)"
fi

ALB_STATE="$(aws elbv2 describe-load-balancers --region "${REGION}" \
  --names kitehub-alb \
  --query 'LoadBalancers[0].State.Code' --output text 2>/dev/null || echo "missing")"
if [ "${ALB_STATE}" = "active" ]; then
  ok "ALB kitehub-alb: active"
elif [ "${ALB_STATE}" = "missing" ]; then
  fail "ALB kitehub-alb not found"
else
  warn "ALB kitehub-alb state: ${ALB_STATE}"
fi

TG_COUNT="$(aws elbv2 describe-target-groups --region "${REGION}" \
  --query 'length(TargetGroups[?contains(LoadBalancerArns[0] || `none`,`kitehub-alb`)])' \
  --output text 2>/dev/null || echo "0")"
if [ "${TG_COUNT}" -ge 2 ] 2>/dev/null; then
  ok "Target groups: ${TG_COUNT}"
else
  warn "Target groups: ${TG_COUNT} (expected ≥2)"
fi
echo

# ---------- Section C: RDS ----------
info "Section C — RDS"
RDS_STATUS="$(aws rds describe-db-instances --region "${REGION}" \
  --db-instance-identifier kitehub-postgres \
  --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "missing")"
if [ "${RDS_STATUS}" = "available" ]; then
  ok "RDS kitehub-postgres: available"
elif [ "${RDS_STATUS}" = "missing" ]; then
  fail "RDS kitehub-postgres not found"
else
  warn "RDS kitehub-postgres status: ${RDS_STATUS}"
fi

RDS_PUBLIC="$(aws rds describe-db-instances --region "${REGION}" \
  --db-instance-identifier kitehub-postgres \
  --query 'DBInstances[0].PubliclyAccessible' --output text 2>/dev/null || echo "unknown")"
if [ "${RDS_PUBLIC}" = "False" ]; then
  ok "RDS PubliclyAccessible=false (defense-in-depth)"
elif [ "${RDS_PUBLIC}" = "True" ]; then
  fail "RDS PubliclyAccessible=true — security risk"
else
  warn "RDS PubliclyAccessible: ${RDS_PUBLIC}"
fi
echo

# ---------- Section D: ECR ----------
info "Section D — ECR"
ECR_COUNT="$(aws ecr describe-repositories --region "${REGION}" \
  --query 'length(repositories[?starts_with(repositoryName,`kite/`)])' \
  --output text 2>/dev/null || echo "0")"
if [ "${ECR_COUNT}" = "10" ]; then
  ok "ECR repos: 10"
else
  warn "ECR repos: ${ECR_COUNT} (expected 10)"
fi
echo

# ---------- Section E: Secrets Manager (METADATA ONLY) ----------
info "Section E — Secrets Manager (metadata only — NO get-secret-value)"
SECRET_COUNT="$(aws secretsmanager list-secrets --region "${REGION}" \
  --query 'length(SecretList[?starts_with(Name,`kitehub/production/`)])' \
  --output text 2>/dev/null || echo "0")"
if [ "${SECRET_COUNT}" = "8" ]; then
  ok "Production secrets: 8"
elif [ "${SECRET_COUNT}" -gt 0 ] 2>/dev/null; then
  warn "Production secrets: ${SECRET_COUNT} (expected 8)"
else
  fail "No production secrets found"
fi
echo

# ---------- Section F: S3 + DynamoDB (state backend) ----------
info "Section F — S3 + DynamoDB (state backend)"
for bucket in \
    "kitehub-terraform-state-${EXPECTED_ACCOUNT}" \
    "kitehub-cloudtrail-logs-${EXPECTED_ACCOUNT}" \
    "kitehub-assets-production-${EXPECTED_ACCOUNT}"; do
  if aws s3api head-bucket --region "${REGION}" --bucket "${bucket}" 2>/dev/null; then
    ok "S3 bucket: ${bucket}"
  else
    fail "S3 bucket missing/inaccessible: ${bucket}"
  fi
done

LOCK_STATUS="$(aws dynamodb describe-table --region "${REGION}" \
  --table-name kitehub-terraform-locks \
  --query 'Table.TableStatus' --output text 2>/dev/null || echo "missing")"
if [ "${LOCK_STATUS}" = "ACTIVE" ]; then
  ok "DynamoDB lock table: ACTIVE"
else
  fail "DynamoDB lock table status: ${LOCK_STATUS}"
fi
echo

# ---------- Section G: CloudTrail ----------
info "Section G — CloudTrail audit baseline"
TRAIL_LOGGING="$(aws cloudtrail get-trail-status --region "${REGION}" \
  --name kitehub-main \
  --query 'IsLogging' --output text 2>/dev/null || echo "missing")"
if [ "${TRAIL_LOGGING}" = "True" ]; then
  ok "CloudTrail kitehub-main: IsLogging=true"
elif [ "${TRAIL_LOGGING}" = "False" ]; then
  fail "CloudTrail kitehub-main: IsLogging=false (audit blind spot!)"
else
  fail "CloudTrail kitehub-main: not found"
fi

TRAIL_MULTIREGION="$(aws cloudtrail describe-trails --region "${REGION}" \
  --query 'trailList[?Name==`kitehub-main`].IsMultiRegionTrail | [0]' \
  --output text 2>/dev/null || echo "unknown")"
if [ "${TRAIL_MULTIREGION}" = "True" ]; then
  ok "CloudTrail: multi-region"
else
  warn "CloudTrail multi-region: ${TRAIL_MULTIREGION}"
fi
echo

# ---------- Section H: Endpoint accessibility (HEAD only) ----------
info "Section H — Endpoint accessibility (curl -sI HEAD only)"
probe_head() {
  local url="$1"
  local label="$2"
  local code
  code="$(curl -sI -m 10 -o /dev/null -w "%{http_code}" "${url}" 2>/dev/null || echo "000")"
  if [ "${code}" = "200" ]; then
    ok "${label}: HTTP 200"
  elif [ "${code}" = "000" ]; then
    warn "${label}: timeout/unreachable"
  else
    warn "${label}: HTTP ${code}"
  fi
}

probe_head "https://kitehub.vercel.app/" "Vercel kitehub"
probe_head "https://kiteclass.vercel.app/" "Vercel kiteclass"

ALB_DNS="$(aws elbv2 describe-load-balancers --region "${REGION}" \
  --names kitehub-alb \
  --query 'LoadBalancers[0].DNSName' --output text 2>/dev/null || echo "")"
if [ -n "${ALB_DNS}" ] && [ "${ALB_DNS}" != "None" ]; then
  probe_head "http://${ALB_DNS}/" "AWS ALB (HTTP 502/503 OK if Phase 3 pending)"
fi
echo

# ---------- Summary ----------
TOTAL=$((PASS + FAIL + WARN))
printf "%s\n" "------------------------------------------------------------"
printf "Summary: ${C_OK}%d PASS${C_RST}  ${C_FAIL}%d FAIL${C_RST}  ${C_WARN}%d WARN${C_RST}  (total: %d)\n" \
  "${PASS}" "${FAIL}" "${WARN}" "${TOTAL}"
printf "%s\n" "------------------------------------------------------------"
echo
info "Next step: save audit artifact to:"
printf "  ${C_INFO}%s${C_RST}\n" "${ARTIFACT_PATH}"
info "Use template: .claude/skills/devops/aws-smoke-test/reference/audit-artifact-template.md"
echo

if [ "${FAIL}" -gt 0 ]; then
  printf "${C_FAIL}Verification FAILED${C_RST} — investigate %d failure(s) and file follow-up gap per audit-to-gap-pipeline.md\n" "${FAIL}"
  exit 1
fi

if [ "${WARN}" -gt 0 ]; then
  printf "${C_WARN}Verification PASSED with warnings${C_RST} — review %d warning(s); document in audit artifact\n" "${WARN}"
  exit 0
fi

printf "${C_OK}Verification PASSED${C_RST}\n"
exit 0

#!/usr/bin/env bash
# shellcheck disable=SC2059,SC2016
# SC2059: color escape codes in printf format are intentional
# SC2016: AWS JMESPath queries use backticks for string literals; single-quote required
# smoke-aws-phase-4.sh — Phase 4 staging environment read-only verification
#
# Usage: bash scripts/smoke-aws-phase-4.sh
#
# Tier 1 (read-only) only per .claude/rules/agent-aws-access.md §2.1.
# Phase 4 staging may be partial — script verifies whichever staging
# resources have been provisioned and reports gaps as warnings.
#
# Reference:
#   - .claude/rules/agent-aws-access.md
#   - .claude/skills/devops/aws-smoke-test/reference/phase-4-staging.md

set -euo pipefail

REGION="ap-southeast-1"
EXPECTED_ACCOUNT="906286017800"
TODAY="$(date -u +%Y-%m-%d)"
ARTIFACT_PATH="documents/04-quality/audits/aws-verification/${TODAY}-phase-4-staging-smoke.md"

PASS=0
FAIL=0
WARN=0

if [ -t 1 ]; then
  C_OK="\033[0;32m"; C_FAIL="\033[0;31m"; C_WARN="\033[0;33m"
  C_INFO="\033[0;36m"; C_RST="\033[0m"
else
  C_OK=""; C_FAIL=""; C_WARN=""; C_INFO=""; C_RST=""
fi

ok()   { printf "  ${C_OK}PASS${C_RST}  %s\n" "$1"; PASS=$((PASS + 1)); }
fail() { printf "  ${C_FAIL}FAIL${C_RST}  %s\n" "$1"; FAIL=$((FAIL + 1)); }
warn() { printf "  ${C_WARN}WARN${C_RST}  %s\n" "$1"; WARN=$((WARN + 1)); }
info() { printf "${C_INFO}==>${C_RST} %s\n" "$1"; }

if ! command -v aws >/dev/null 2>&1; then
  printf "${C_FAIL}ERROR${C_RST} aws CLI not found.\n"
  exit 2
fi

info "Phase 4 staging smoke test"
info "Region: ${REGION}  |  Expected account: ${EXPECTED_ACCOUNT}"
echo

# Account verify
ACTUAL_ACCOUNT="$(aws sts get-caller-identity --query 'Account' --output text 2>/dev/null || echo "")"
if [ "${ACTUAL_ACCOUNT}" != "${EXPECTED_ACCOUNT}" ]; then
  fail "Account mismatch: expected ${EXPECTED_ACCOUNT}, got '${ACTUAL_ACCOUNT}'"
  exit 2
fi
ok "Account = ${ACTUAL_ACCOUNT}"
echo

# Staging EC2
info "Staging EC2 instances"
EC2_STAGING="$(aws ec2 describe-instances --region "${REGION}" \
  --filters "Name=tag:Environment,Values=staging" \
  --query 'length(Reservations[].Instances[?State.Name==`running` || State.Name==`stopped`])' \
  --output text 2>/dev/null || echo "0")"
if [ "${EC2_STAGING}" -gt 0 ] 2>/dev/null; then
  ok "Staging EC2 instances: ${EC2_STAGING}"
else
  warn "No staging EC2 instances found (Phase 4 may not be applied yet)"
fi
echo

# Staging ALB
info "Staging ALB"
STAGING_ALB="$(aws elbv2 describe-load-balancers --region "${REGION}" \
  --query 'LoadBalancers[?contains(LoadBalancerName,`staging`)].LoadBalancerName | [0]' \
  --output text 2>/dev/null || echo "None")"
if [ "${STAGING_ALB}" != "None" ] && [ -n "${STAGING_ALB}" ]; then
  ok "Staging ALB: ${STAGING_ALB}"
else
  warn "No staging ALB found"
fi
echo

# Staging RDS (may share with prod)
info "Staging RDS"
STAGING_RDS="$(aws rds describe-db-instances --region "${REGION}" \
  --query 'DBInstances[?contains(DBInstanceIdentifier,`staging`)].DBInstanceIdentifier | [0]' \
  --output text 2>/dev/null || echo "None")"
if [ "${STAGING_RDS}" != "None" ] && [ -n "${STAGING_RDS}" ]; then
  ok "Staging RDS: ${STAGING_RDS}"
else
  warn "No dedicated staging RDS (may share production DB with schema separation)"
fi
echo

# Staging secrets
info "Staging secrets (metadata only)"
STAGING_SECRETS="$(aws secretsmanager list-secrets --region "${REGION}" \
  --query 'length(SecretList[?starts_with(Name,`kitehub/staging/`)])' \
  --output text 2>/dev/null || echo "0")"
if [ "${STAGING_SECRETS}" -gt 0 ] 2>/dev/null; then
  ok "Staging secrets: ${STAGING_SECRETS}"
else
  warn "No kitehub/staging/* secrets (may be Phase 4 deferred)"
fi
echo

# Summary
TOTAL=$((PASS + FAIL + WARN))
printf "%s\n" "------------------------------------------------------------"
printf "Summary: ${C_OK}%d PASS${C_RST}  ${C_FAIL}%d FAIL${C_RST}  ${C_WARN}%d WARN${C_RST}  (total: %d)\n" \
  "${PASS}" "${FAIL}" "${WARN}" "${TOTAL}"
printf "%s\n" "------------------------------------------------------------"
echo
info "Save audit artifact to:"
printf "  ${C_INFO}%s${C_RST}\n" "${ARTIFACT_PATH}"
info "Template: .claude/skills/devops/aws-smoke-test/reference/audit-artifact-template.md"
echo

if [ "${FAIL}" -gt 0 ]; then
  exit 1
fi
exit 0

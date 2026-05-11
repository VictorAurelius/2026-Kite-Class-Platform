#!/usr/bin/env bash
# smoke-ses.sh — AWS SES read-only verification + sandbox/production state check
#
# Usage:
#   bash scripts/smoke-ses.sh              # full read-only verification
#   bash scripts/smoke-ses.sh --domain     # check domain identity DKIM/SPF/DMARC records via DNS only
#
# Tier 1 (read-only) per .claude/rules/agent-aws-access.md §2.1.
# NO send-email, NO put/update/delete; only describe-*, list-*, get-account.
#
# Wave 61 Bucket B — GAP-370 SES production approval preparation.
#
# Reference:
#   - .claude/rules/agent-aws-access.md
#   - documents/05-guides/deploy/email-ses-setup-runbook.md
#   - documents/04-quality/gaps/GAP-370-email-transactional-infrastructure.md

set -euo pipefail

REGION="${AWS_DEFAULT_REGION:-ap-southeast-1}"
EXPECTED_ACCOUNT="906286017800"
DOMAIN="${SES_DOMAIN:-kitehub.me}"
TODAY="$(date -u +%Y-%m-%d)"
ARTIFACT_PATH="documents/04-quality/audits/aws-verification/${TODAY}-ses-smoke.md"

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

# ---------------------------------------------------------------------------
# 0. Pre-flight — AWS CLI + caller identity
# ---------------------------------------------------------------------------
info "Pre-flight checks"

if ! command -v aws >/dev/null 2>&1; then
  fail "aws CLI not on PATH"
  exit 1
fi
ok "aws CLI available"

CALLER_ACCOUNT=$(aws sts get-caller-identity --query 'Account' --output text 2>/dev/null || echo "")
if [ "$CALLER_ACCOUNT" = "$EXPECTED_ACCOUNT" ]; then
  ok "Caller identity = $EXPECTED_ACCOUNT"
else
  fail "Caller identity '$CALLER_ACCOUNT' != expected $EXPECTED_ACCOUNT (run AWS_PROFILE=dev-admin ...)"
  exit 1
fi

# ---------------------------------------------------------------------------
# 1. SES account state — sandbox vs production
# ---------------------------------------------------------------------------
info "SES account state ($REGION)"

ACCOUNT_JSON=$(aws sesv2 get-account --region "$REGION" 2>/dev/null || echo "{}")

PROD_ENABLED=$(echo "$ACCOUNT_JSON" | jq -r '.ProductionAccessEnabled // "unknown"')
SEND_ENABLED=$(echo "$ACCOUNT_JSON" | jq -r '.SendingEnabled // "unknown"')
ENFORCEMENT=$(echo "$ACCOUNT_JSON" | jq -r '.EnforcementStatus // "unknown"')
MAX_24H=$(echo "$ACCOUNT_JSON" | jq -r '.SendQuota.Max24HourSend // 0')
MAX_RATE=$(echo "$ACCOUNT_JSON" | jq -r '.SendQuota.MaxSendRate // 0')
SENT_LAST_24H=$(echo "$ACCOUNT_JSON" | jq -r '.SendQuota.SentLast24Hours // 0')

if [ "$ENFORCEMENT" = "HEALTHY" ]; then
  ok "EnforcementStatus = HEALTHY"
else
  warn "EnforcementStatus = $ENFORCEMENT (expected HEALTHY)"
fi

if [ "$SEND_ENABLED" = "true" ]; then
  ok "SendingEnabled = true"
else
  fail "SendingEnabled = $SEND_ENABLED (account is paused; check Reputation dashboard)"
fi

if [ "$PROD_ENABLED" = "true" ]; then
  ok "ProductionAccessEnabled = true (Max24h=${MAX_24H}, Rate=${MAX_RATE}/sec, Sent24h=${SENT_LAST_24H})"
else
  warn "ProductionAccessEnabled = false — SANDBOX mode (Max24h=${MAX_24H}, Rate=${MAX_RATE}/sec)"
  warn "  → Action: submit production access request per email-ses-setup-runbook.md §4"
fi

# ---------------------------------------------------------------------------
# 2. Email identities — domain verification + DKIM
# ---------------------------------------------------------------------------
info "SES email identities"

IDENTITIES_JSON=$(aws sesv2 list-email-identities --region "$REGION" 2>/dev/null || echo '{"EmailIdentities":[]}')
IDENTITY_COUNT=$(echo "$IDENTITIES_JSON" | jq '.EmailIdentities | length')

if [ "$IDENTITY_COUNT" -eq 0 ]; then
  warn "No email identities registered"
  warn "  → Action: verify domain '$DOMAIN' per email-ses-setup-runbook.md §3"
else
  ok "Registered identities: $IDENTITY_COUNT"
  echo "$IDENTITIES_JSON" | jq -r '.EmailIdentities[] | "    - \(.IdentityType): \(.IdentityName) (verified=\(.SendingEnabled // false))"'

  # Check our expected domain if listed
  if echo "$IDENTITIES_JSON" | jq -e --arg d "$DOMAIN" '.EmailIdentities[] | select(.IdentityName == $d)' >/dev/null; then
    info "Inspecting identity: $DOMAIN"
    IDENTITY_JSON=$(aws sesv2 get-email-identity --email-identity "$DOMAIN" --region "$REGION" 2>/dev/null || echo "{}")

    VERIFIED=$(echo "$IDENTITY_JSON" | jq -r '.VerifiedForSendingStatus // false')
    DKIM_STATUS=$(echo "$IDENTITY_JSON" | jq -r '.DkimAttributes.Status // "unknown"')
    MAIL_FROM=$(echo "$IDENTITY_JSON" | jq -r '.MailFromAttributes.MailFromDomain // "none"')

    if [ "$VERIFIED" = "true" ]; then
      ok "Domain $DOMAIN verified for sending"
    else
      warn "Domain $DOMAIN VerifiedForSendingStatus = $VERIFIED"
    fi

    if [ "$DKIM_STATUS" = "SUCCESS" ]; then
      ok "DKIM status = SUCCESS"
    else
      warn "DKIM status = $DKIM_STATUS (expected SUCCESS — add 3 CNAME records per runbook §3.2)"
    fi

    if [ "$MAIL_FROM" != "none" ] && [ "$MAIL_FROM" != "null" ]; then
      ok "MAIL FROM domain configured: $MAIL_FROM"
    else
      warn "MAIL FROM domain not configured (optional but recommended)"
    fi
  fi
fi

# ---------------------------------------------------------------------------
# 3. Suppression list size (sandbox prep visibility)
# ---------------------------------------------------------------------------
info "Suppression list"

SUPP_JSON=$(aws sesv2 list-suppressed-destinations --region "$REGION" --page-size 5 2>/dev/null || echo '{"SuppressedDestinationSummaries":[]}')
SUPP_COUNT=$(echo "$SUPP_JSON" | jq '.SuppressedDestinationSummaries | length')

if [ "$SUPP_COUNT" -eq 0 ]; then
  ok "Suppression list empty (no past bounces/complaints to clean before launch)"
else
  warn "Suppression list contains $SUPP_COUNT+ entries (sampled; total may be higher) — review before launch"
fi

# ---------------------------------------------------------------------------
# 4. DNS records (optional --domain mode skips AWS calls and ONLY checks DNS)
# ---------------------------------------------------------------------------
if [ "${1:-}" = "--domain" ] || [ "$IDENTITY_COUNT" -gt 0 ]; then
  info "DNS record verification for $DOMAIN"

  if command -v dig >/dev/null 2>&1; then
    SPF=$(dig +short TXT "$DOMAIN" | grep -o 'v=spf1[^"]*' | head -1 || echo "")
    if [ -n "$SPF" ]; then
      ok "SPF: $SPF"
    else
      warn "SPF TXT not found (expected: v=spf1 include:amazonses.com -all)"
    fi

    DMARC=$(dig +short TXT "_dmarc.$DOMAIN" | grep -o 'v=DMARC1[^"]*' | head -1 || echo "")
    if [ -n "$DMARC" ]; then
      ok "DMARC: $DMARC"
    else
      warn "DMARC TXT not found at _dmarc.$DOMAIN"
    fi
  else
    warn "dig not available — skipping DNS check (install bind-utils or dnsutils)"
  fi
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
info "Summary"
echo "  PASS: $PASS"
echo "  WARN: $WARN"
echo "  FAIL: $FAIL"
echo ""
echo "Production access: ${PROD_ENABLED:-unknown}"
if [ "$PROD_ENABLED" != "true" ]; then
  echo "  → Submit support case per email-ses-setup-runbook.md §4.1 (template in §4.1.1)"
fi
echo ""
echo "Verification artifact path (save findings):"
echo "  $ARTIFACT_PATH"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0

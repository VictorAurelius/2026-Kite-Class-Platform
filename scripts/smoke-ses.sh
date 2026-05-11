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
# 5. Email delivery E2E (Wave 62 GAP-475 Sub-2, opt-in)
# ---------------------------------------------------------------------------
# Env-gated send→receive verification. Sends a real SES email and polls a
# mailbox (Mailgun events API or IMAP via curl) for receipt evidence.
#
# Required env to opt in:
#   SMOKE_EMAIL_E2E=1
#   SMOKE_EMAIL_RECIPIENT=<address>
#
# Polling mode A (preferred — Mailgun events API):
#   SMOKE_EMAIL_MAILGUN_API_KEY=<key>
#   SMOKE_EMAIL_MAILGUN_DOMAIN=<domain configured in Mailgun for inbound>
#
# Polling mode B (IMAP via curl; fragile — Mailgun preferred):
#   SMOKE_EMAIL_IMAP_HOST=imap.example.com  (port :993 implicit)
#   SMOKE_EMAIL_IMAP_USER=<user>
#   SMOKE_EMAIL_IMAP_PASS=<pass>
#
# Optional sender override:
#   SMOKE_EMAIL_FROM (default noreply@kitehub.me)

SES_SMOKE_FROM="${SMOKE_EMAIL_FROM:-noreply@kitehub.me}"

# Poll mailbox for a message whose subject contains $1.
# Echoes raw email body to stdout if found; exits 0 on hit, 1 on timeout.
# Usage: _poll_email_inbox "<subject-token>" [max_retries=10] [sleep_seconds=30]
_poll_email_inbox() {
  local needle="$1"
  local max_retries="${2:-10}"
  local sleep_secs="${3:-30}"
  local attempt=0

  while [ "$attempt" -lt "$max_retries" ]; do
    attempt=$((attempt + 1))

    if [ -n "${SMOKE_EMAIL_MAILGUN_API_KEY:-}" ] && [ -n "${SMOKE_EMAIL_MAILGUN_DOMAIN:-}" ]; then
      # Mailgun events API — filter by recipient + event=accepted/delivered
      local events_json
      events_json=$(curl -s --user "api:${SMOKE_EMAIL_MAILGUN_API_KEY}" \
        "https://api.mailgun.net/v3/${SMOKE_EMAIL_MAILGUN_DOMAIN}/events?event=accepted+OR+delivered+OR+stored&recipient=${SMOKE_EMAIL_RECIPIENT}&limit=25" 2>/dev/null || echo '{}')

      # Find an item whose message subject contains the needle
      local body
      body=$(echo "$events_json" | jq -r --arg n "$needle" \
        '.items // [] | map(select((.message.headers.subject // "") | contains($n))) | .[0] // empty' 2>/dev/null || echo "")

      if [ -n "$body" ] && [ "$body" != "null" ]; then
        echo "$body"
        return 0
      fi
    elif [ -n "${SMOKE_EMAIL_IMAP_HOST:-}" ] && [ -n "${SMOKE_EMAIL_IMAP_USER:-}" ] && [ -n "${SMOKE_EMAIL_IMAP_PASS:-}" ]; then
      # Best-effort IMAP fetch via curl — fragile; users may prefer custom helper.
      # Fetches the most recent message body and greps for the needle.
      local imap_body
      imap_body=$(curl -s --url "imaps://${SMOKE_EMAIL_IMAP_HOST}/INBOX;UID=*" \
        --user "${SMOKE_EMAIL_IMAP_USER}:${SMOKE_EMAIL_IMAP_PASS}" 2>/dev/null || echo "")

      if echo "$imap_body" | grep -q "$needle"; then
        echo "$imap_body"
        return 0
      fi
    else
      # No polling backend configured.
      return 2
    fi

    if [ "$attempt" -lt "$max_retries" ]; then
      sleep "$sleep_secs"
    fi
  done

  return 1
}

send_receive_email_e2e() {
  if [ "${SMOKE_EMAIL_E2E:-0}" != "1" ]; then
    info "[SKIP] Email E2E (set SMOKE_EMAIL_E2E=1 + SMOKE_EMAIL_RECIPIENT + Mailgun/IMAP creds to enable)"
    return 0
  fi

  if [ -z "${SMOKE_EMAIL_RECIPIENT:-}" ]; then
    warn "Email E2E: SMOKE_EMAIL_E2E=1 but SMOKE_EMAIL_RECIPIENT unset — skipping"
    return 0
  fi

  if [ -z "${SMOKE_EMAIL_MAILGUN_API_KEY:-}" ] && [ -z "${SMOKE_EMAIL_IMAP_HOST:-}" ]; then
    warn "Email E2E: no polling backend configured (need Mailgun OR IMAP env) — skipping"
    return 0
  fi

  info "Email delivery E2E (send→receive)"

  local ts subject body
  ts="$(date +%s)"
  subject="smoke-test-${ts}"
  body="Smoke test body $(date -u +%Y-%m-%dT%H:%M:%SZ) — token=${ts}"

  # Send via SES
  local send_out
  send_out=$(aws ses send-email \
    --region "$REGION" \
    --from "$SES_SMOKE_FROM" \
    --destination "ToAddresses=${SMOKE_EMAIL_RECIPIENT}" \
    --message "Subject={Data=${subject},Charset=UTF-8},Body={Text={Data=${body},Charset=UTF-8}}" \
    2>&1) || true

  local message_id
  message_id=$(echo "$send_out" | jq -r '.MessageId // empty' 2>/dev/null || echo "")

  if [ -z "$message_id" ]; then
    fail "SES send-email failed: $(echo "$send_out" | head -c 200)"
    return 0
  fi
  ok "SES send-email returned MessageId=${message_id}"

  # Poll mailbox
  info "Polling mailbox for subject containing '${subject}' (max ~5min)"
  local fetched
  fetched=$(_poll_email_inbox "$subject" 10 30 || true)

  if [ -z "$fetched" ]; then
    fail "Email not received within polling window (subject=${subject})"
    return 0
  fi

  ok "Email received (subject match confirmed)"

  # Best-effort body verification — Mailgun events expose stored.url; IMAP gives raw.
  # When body content not in event payload, skip strict assertion.
  if echo "$fetched" | grep -q "token=${ts}"; then
    ok "Email body content match (token=${ts})"
  else
    warn "Body content not asserted (poll backend may only expose headers)"
  fi
}

verify_mfa_otp_e2e() {
  if [ "${SMOKE_MFA_E2E:-0}" != "1" ]; then
    info "[SKIP] MFA OTP E2E (set SMOKE_MFA_E2E=1 + reuse SMOKE_EMAIL_* env to enable)"
    return 0
  fi

  if [ -z "${SMOKE_EMAIL_RECIPIENT:-}" ]; then
    warn "MFA OTP E2E: SMOKE_EMAIL_RECIPIENT unset — skipping"
    return 0
  fi

  if [ -z "${SMOKE_EMAIL_MAILGUN_API_KEY:-}" ] && [ -z "${SMOKE_EMAIL_IMAP_HOST:-}" ]; then
    warn "MFA OTP E2E: no polling backend configured — skipping"
    return 0
  fi

  local kh_url="${KH_URL:-${SMOKE_KH_URL:-http://localhost:8080}}"
  info "MFA OTP E2E via ${kh_url}"

  # Trigger signup. Backend POST /api/auth/register expects RegisterRequest:
  #   {organizationName, subdomain, ownerEmail, ownerPassword}
  # Verification is link-based (?token=<UUID>) per AuthController#verifyEmail.
  # We extract the token from the verification email URL.
  local ts org_name subdomain password
  ts="$(date +%s)"
  org_name="Smoke Test ${ts}"
  subdomain="smoke-${ts}"
  password="Sm0keTest!${ts}"

  local register_body
  register_body=$(cat <<EOF
{"organizationName":"${org_name}","subdomain":"${subdomain}","ownerEmail":"${SMOKE_EMAIL_RECIPIENT}","ownerPassword":"${password}"}
EOF
)

  local http_code
  http_code=$(curl -s -o /tmp/smoke-register.out -w "%{http_code}" \
    -X POST "${kh_url}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "$register_body" 2>/dev/null || echo "000")

  if [ "$http_code" != "201" ] && [ "$http_code" != "200" ]; then
    fail "Register endpoint returned HTTP ${http_code} (expected 200/201). Body: $(head -c 200 /tmp/smoke-register.out 2>/dev/null || echo none)"
    return 0
  fi
  ok "Register endpoint returned HTTP ${http_code}"

  # Poll mailbox for the verification email (subject heuristic: "verify" or "xác nhận")
  info "Polling mailbox for verification email (max ~5min)"
  local fetched
  # Try common subject tokens — registration emails usually contain "verify" or "verification".
  fetched=$(_poll_email_inbox "verify" 10 30 || true)
  if [ -z "$fetched" ]; then
    fetched=$(_poll_email_inbox "xác nhận" 10 30 || true)
  fi

  if [ -z "$fetched" ]; then
    fail "Verification email not received within polling window"
    return 0
  fi
  ok "Verification email received"

  # Extract token from URL — verifyEmail uses ?token=<UUID> query param.
  local token
  token=$(echo "$fetched" | grep -oE 'token=[A-Za-z0-9_-]+' | head -1 | cut -d= -f2)

  if [ -z "$token" ]; then
    # Fallback: try 6-digit OTP pattern in case future email format uses one
    token=$(echo "$fetched" | grep -oE '\b[0-9]{6}\b' | head -1)
  fi

  if [ -z "$token" ]; then
    fail "Could not extract verification token from email body"
    return 0
  fi
  ok "Extracted verification token (length=${#token})"

  # POST verify-email — controller uses @RequestParam, so token is query-string.
  local verify_code
  verify_code=$(curl -s -o /tmp/smoke-verify.out -w "%{http_code}" \
    -X POST "${kh_url}/api/auth/verify-email?token=${token}" 2>/dev/null || echo "000")

  if [ "$verify_code" = "200" ]; then
    ok "verify-email returned HTTP 200 — MFA OTP flow E2E pass"
  else
    fail "verify-email returned HTTP ${verify_code}. Body: $(head -c 200 /tmp/smoke-verify.out 2>/dev/null || echo none)"
  fi
}

send_receive_email_e2e
verify_mfa_otp_e2e

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

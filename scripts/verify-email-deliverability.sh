#!/usr/bin/env bash
# verify-email-deliverability.sh - Spam-score smoke test for kitehub.me sender
#
# Tier 1 (read-only) per .claude/rules/agent-aws-access.md - only sends from the
# already-configured Resend production secret; no AWS mutations.
#
# Usage:
#   bash scripts/verify-email-deliverability.sh
#   bash scripts/verify-email-deliverability.sh --manual   # print manual procedure only
#
# Behaviour:
#   - If MAIL_TESTER_API_KEY env set + Resend API key resolvable -> automated:
#       1. Request fresh mail-tester inbox address
#       2. Send test invite email from production Resend
#       3. Poll mail-tester for score
#       4. Exit 0 if score >= 8/10, exit 1 otherwise
#   - If either prerequisite missing -> print manual procedure (still exit 0,
#     since the script's job is to guide; the gate is the operator's read)
#
# Wave 77 Bucket A - GAP-533 deliverability warm-up.
#
# Reference:
#   - documents/05-guides/deploy/email-deliverability-runbook.md sec 4
#   - documents/04-quality/gaps/GAP-533-resend-deliverability-warmup-dkim-dmarc-spf.md
#   - https://www.mail-tester.com/api

set -euo pipefail

PASS_THRESHOLD="${SPAM_SCORE_PASS:-8}"
POLL_SECONDS="${MAIL_TESTER_POLL_SECONDS:-30}"
POLL_MAX="${MAIL_TESTER_POLL_MAX:-3}"
RESEND_FROM="${RESEND_FROM:-noreply@kitehub.me}"
RESEND_ENDPOINT="https://api.resend.com/emails"
MAIL_TESTER_API="https://www.mail-tester.com/api"
MANUAL_ONLY=0

if [ -t 1 ]; then
  C_OK="\033[0;32m"; C_FAIL="\033[0;31m"; C_WARN="\033[0;33m"
  C_INFO="\033[0;36m"; C_RST="\033[0m"
else
  C_OK=""; C_FAIL=""; C_WARN=""; C_INFO=""; C_RST=""
fi

ok()   { printf "  ${C_OK}PASS${C_RST}  %s\n" "$1"; }
fail() { printf "  ${C_FAIL}FAIL${C_RST}  %s\n" "$1"; }
warn() { printf "  ${C_WARN}WARN${C_RST}  %s\n" "$1"; }
info() { printf "${C_INFO}==>${C_RST} %s\n" "$1"; }

while [ $# -gt 0 ]; do
  case "$1" in
    --manual) MANUAL_ONLY=1; shift;;
    -h|--help)
      sed -n '1,30p' "$0"
      exit 0
      ;;
    *) echo "Unknown arg: $1"; exit 2;;
  esac
done

print_manual() {
  cat <<'EOF'

Manual procedure (no API key):
  1. Open https://www.mail-tester.com/ in a browser.
  2. Copy the unique test address (test-XXXX@srv1.mail-tester.com).
  3. From the production Resend account, send 1 invite-shaped email to that address.
     Use a realistic template (beta-invite.html) - subject + HTML body + links.
  4. After ~30 seconds, click "Then check your score" on the same page.
  5. Verify score >= 8/10. If lower, review the breakdown:
     - SPF/DKIM/DMARC: see runbook sec 4.3 troubleshooting matrix
     - Content: avoid ALL CAPS, > 5 links, spam-trigger words
     - Blacklist: check https://mxtoolbox.com/blacklists.aspx?domain=kitehub.me
  6. Document the score + breakdown in the session log or audit artifact:
     documents/04-quality/audits/email/YYYY-MM-DD-deliverability-baseline.md

Gate criteria (per email-deliverability-runbook.md sec 4.3):
  - 3 consecutive runs >= 8/10 = allow cohort expansion
  - 1 run < 8 = STOP, fix breakdown, retry after DNS / content propagation
EOF
}

if [ "$MANUAL_ONLY" -eq 1 ]; then
  info "Manual procedure mode"
  print_manual
  exit 0
fi

# ---------------------------------------------------------------------------
# 1. Pre-flight - check prerequisites
# ---------------------------------------------------------------------------
info "Pre-flight"

if ! command -v curl >/dev/null 2>&1; then
  fail "curl not on PATH"
  exit 1
fi
ok "curl available"

if ! command -v jq >/dev/null 2>&1; then
  fail "jq not on PATH"
  exit 1
fi
ok "jq available"

if [ -z "${MAIL_TESTER_API_KEY:-}" ]; then
  warn "MAIL_TESTER_API_KEY not set - falling back to manual procedure"
  print_manual
  exit 0
fi
ok "MAIL_TESTER_API_KEY present"

if [ -z "${RESEND_API_KEY:-}" ]; then
  warn "RESEND_API_KEY env var not set"
  warn "Resolve via: export RESEND_API_KEY=\$(aws secretsmanager get-secret-value --secret-id kitehub/production/resend --query SecretString --output text | jq -r .api_key)"
  print_manual
  exit 0
fi
ok "RESEND_API_KEY present"

# ---------------------------------------------------------------------------
# 2. Request mail-tester inbox
# ---------------------------------------------------------------------------
info "Requesting mail-tester inbox"

INBOX_JSON=$(curl -fsS -H "Authorization: Bearer $MAIL_TESTER_API_KEY" \
  "$MAIL_TESTER_API/inbox" 2>&1) || {
  fail "mail-tester /inbox API call failed: $INBOX_JSON"
  print_manual
  exit 1
}

TEST_ADDR=$(echo "$INBOX_JSON" | jq -r '.email // empty')
TEST_ID=$(echo "$INBOX_JSON" | jq -r '.id // empty')

if [ -z "$TEST_ADDR" ] || [ -z "$TEST_ID" ]; then
  fail "mail-tester response missing email/id: $INBOX_JSON"
  exit 1
fi
ok "mail-tester inbox: $TEST_ADDR (id=$TEST_ID)"

# ---------------------------------------------------------------------------
# 3. Send test email via Resend
# ---------------------------------------------------------------------------
info "Sending test email via Resend"

SUBJECT="KiteHub Beta Access Test - deliverability smoke ($(date -u +%Y-%m-%dT%H:%M:%SZ))"
HTML_BODY='<html><body><h2>KiteHub Beta - Deliverability Smoke</h2><p>This is a smoke test email from the kitehub.me production sender to verify SPF/DKIM/DMARC alignment + mail-tester spam-score baseline. Generated by scripts/verify-email-deliverability.sh.</p><p>If you received this in error, please disregard.</p><p>Reference: <a href="https://kitehub.me/">https://kitehub.me/</a></p><hr><small>Phase 1 BETA - kitehub.me</small></body></html>'

SEND_PAYLOAD=$(jq -nc \
  --arg from "$RESEND_FROM" \
  --arg to "$TEST_ADDR" \
  --arg subj "$SUBJECT" \
  --arg html "$HTML_BODY" \
  '{from: $from, to: [$to], subject: $subj, html: $html}')

SEND_RESP=$(curl -fsS -X POST "$RESEND_ENDPOINT" \
  -H "Authorization: Bearer $RESEND_API_KEY" \
  -H "Content-Type: application/json" \
  --data-binary "$SEND_PAYLOAD" 2>&1) || {
  fail "Resend send failed: $SEND_RESP"
  exit 1
}

MSG_ID=$(echo "$SEND_RESP" | jq -r '.id // empty')
if [ -z "$MSG_ID" ]; then
  fail "Resend response missing id: $SEND_RESP"
  exit 1
fi
ok "Resend message dispatched (id=$MSG_ID)"

# ---------------------------------------------------------------------------
# 4. Poll mail-tester for score
# ---------------------------------------------------------------------------
info "Polling mail-tester for score (interval ${POLL_SECONDS}s, max ${POLL_MAX} attempts)"

SCORE=""
BREAKDOWN_URL=""
ATTEMPTS=0
while [ "$ATTEMPTS" -lt "$POLL_MAX" ]; do
  ATTEMPTS=$((ATTEMPTS + 1))
  sleep "$POLL_SECONDS"

  RESULT_JSON=$(curl -fsS -H "Authorization: Bearer $MAIL_TESTER_API_KEY" \
    "$MAIL_TESTER_API/inbox/$TEST_ID" 2>&1) || {
    warn "Attempt $ATTEMPTS: mail-tester /inbox/$TEST_ID failed: $RESULT_JSON"
    continue
  }

  SCORE=$(echo "$RESULT_JSON" | jq -r '.score // empty')
  BREAKDOWN_URL=$(echo "$RESULT_JSON" | jq -r '.url // empty')

  if [ -n "$SCORE" ] && [ "$SCORE" != "null" ]; then
    break
  fi
  warn "Attempt $ATTEMPTS: score not ready yet"
done

if [ -z "$SCORE" ] || [ "$SCORE" = "null" ]; then
  fail "mail-tester did not return a score after $POLL_MAX attempts"
  fail "Check inbox manually: $BREAKDOWN_URL"
  exit 1
fi

info "mail-tester score: $SCORE / 10"
info "Breakdown URL: $BREAKDOWN_URL"

SCORE_INT=$(printf '%.0f' "$SCORE")
if [ "$SCORE_INT" -ge "$PASS_THRESHOLD" ]; then
  ok "Score $SCORE >= $PASS_THRESHOLD threshold"
  exit 0
else
  fail "Score $SCORE < $PASS_THRESHOLD threshold"
  fail "Review breakdown + apply fixes per email-deliverability-runbook.md sec 4.3"
  exit 1
fi

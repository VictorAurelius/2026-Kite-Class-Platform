#!/usr/bin/env bash
# smoke-email-links.sh — email-link resolvability smoke check
#
# After a transactional-email flow has run, this script reads the emails that
# were actually sent (via MailHog API), extracts EVERY link (href + plaintext
# URL) from each email body, curls each link, and asserts:
#   1. Every link returns a status != 404 (link is resolvable).
#   2. In --local mode (default): no link host is the production domain
#      (kitehub.me). Emails sent on a local stack MUST point at localhost — a
#      link pointing at the prod domain on local is a dead-link (GAP-801 part 3).
#      In --prod mode: the domain check is skipped.
#
# Per GAP-802 cơ chế #1 (BE↔FE contract-drift detection: email-link smoke).
# Bug class motivating this: email flow shipped with API + unit tests green,
# but the actual link in the email was 404 (wrong FE route) and/or pointed at
# the prod domain while sent from local. Only a manual browser walk caught it.
#
# Requires: jq (MailHog bodies are JSON-escaped; a raw grep would miss links).
#
# Exit codes:
#   0 — all extracted links resolvable (and, in --local mode, none prod-domain)
#   1 — at least one link returned 404 OR (--local) pointed at the prod domain
#
# Offline / test hooks (deterministic, no live MailHog or network needed):
#   MAILHOG_FIXTURE   — path to a JSON file with the MailHog /api/v2/messages
#                       shape; read instead of curling the MailHog API.
#   LINK_CHECK_FIXTURE — path to a "url status" map file (one per line,
#                       whitespace-separated). When set, link status is looked
#                       up from this map instead of curling the URL. A URL not
#                       present in the map is treated as status 000 (unknown →
#                       does NOT fail the 404 check, but is reported).
#
# Usage:
#   bash scripts/smoke-email-links.sh                      # --local (default)
#   bash scripts/smoke-email-links.sh --prod               # skip domain check
#   bash scripts/smoke-email-links.sh --json               # machine-readable
#   bash scripts/smoke-email-links.sh --prod-domain x.com --local-host 127.0.0.1

set -euo pipefail

# --- Defaults (overridable via env / flags) ---------------------------------
MAILHOG_URL="${MAILHOG_URL:-http://localhost:8025}"
MODE="local"            # local | prod
PROD_DOMAIN="kitehub.me"
LOCAL_HOST="localhost"  # informational; the local check is "host must NOT be prod domain"
JSON_OUTPUT="false"

# --- Arg parsing ------------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --local)        MODE="local"; shift ;;
    --prod)         MODE="prod"; shift ;;
    --json)         JSON_OUTPUT="true"; shift ;;
    --prod-domain)  PROD_DOMAIN="${2:?--prod-domain requires a value}"; shift 2 ;;
    --local-host)   LOCAL_HOST="${2:?--local-host requires a value}"; shift 2 ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^# \{0,1\}//'
      exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

# --- Fetch raw MailHog messages JSON ----------------------------------------
# MAILHOG_FIXTURE lets fixture-based unit tests inject the API payload offline
# (per scripts/tests/test-smoke-email-links.sh). Default = live MailHog API.
fetch_messages() {
  if [[ -n "${MAILHOG_FIXTURE:-}" ]]; then
    cat "$MAILHOG_FIXTURE"
  else
    curl -fsS "${MAILHOG_URL}/api/v2/messages" 2>/dev/null || true
  fi
}

# --- Extract links from a blob of email body text ---------------------------
# Pulls href="..." attributes AND bare plaintext http(s):// URLs. Trailing
# punctuation common in prose (.,;) and closing markup chars are stripped.
extract_links() {
  local body="$1"
  {
    # href="..." and href='...'
    printf '%s\n' "$body" | grep -oE "href=[\"'][^\"']+[\"']" 2>/dev/null \
      | sed -E "s/^href=[\"']//; s/[\"']$//" || true
    # bare http(s) URLs in plaintext
    printf '%s\n' "$body" | grep -oE 'https?://[^"'"'"' <>)]+' 2>/dev/null || true
  } \
    | sed -E 's/[.,;:]+$//' \
    | sort -u
}

# --- Resolve a link's HTTP status -------------------------------------------
# LINK_CHECK_FIXTURE maps "url status" so tests run offline + deterministic.
# Live mode uses curl -o /dev/null -w '%{http_code}'. A redirect chain is
# followed (-L) so the final status reflects the real destination.
link_status() {
  local url="$1"
  if [[ -n "${LINK_CHECK_FIXTURE:-}" ]]; then
    local code
    code=$(awk -v u="$url" '$1 == u { print $2; exit }' "$LINK_CHECK_FIXTURE")
    [[ -n "$code" ]] && echo "$code" || echo "000"
  else
    curl -s -L -o /dev/null -w '%{http_code}' --max-time 10 "$url" 2>/dev/null || echo "000"
  fi
}

# --- Extract host from a URL ------------------------------------------------
url_host() {
  # strip scheme, then take everything up to the first / : ? #
  echo "$1" | sed -E 's#^[a-zA-Z]+://##; s#[/:?#].*$##'
}

# jq is required: MailHog bodies are JSON-escaped (href=\"...\"), so a naive
# grep of the raw payload would miss links and silently PASS. Fail fast instead.
if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required (parses MailHog JSON message bodies). Install jq." >&2
  exit 2
fi

# --- Main -------------------------------------------------------------------
RAW_JSON="$(fetch_messages)"

# Pull each email Body via jq (robust against JSON escaping).
declare -a BODIES=()
if [[ -n "$RAW_JSON" ]]; then
  while IFS= read -r b; do
    BODIES+=("$b")
  done < <(printf '%s' "$RAW_JSON" | jq -r '.items[]?.Content.Body // empty' 2>/dev/null || true)
fi

declare -a CHECK_LINES=()   # human-readable per-link verdict lines
CHECKED=0
FAILED=0

for body in "${BODIES[@]}"; do
  [[ -z "$body" ]] && continue
  while IFS= read -r link; do
    [[ -z "$link" ]] && continue
    # Skip non-http(s) links (mailto:, tel:, #anchor, relative, cid:)
    [[ "$link" =~ ^https?:// ]] || continue

    CHECKED=$((CHECKED + 1))
    host="$(url_host "$link")"
    status="$(link_status "$link")"
    verdict="PASS"
    reason=""

    # Check 1 — resolvable (not 404)
    if [[ "$status" == "404" ]]; then
      verdict="FAIL"
      reason="404 not found"
    fi

    # Check 2 — local mode must NOT point at prod domain
    if [[ "$MODE" == "local" && "$verdict" == "PASS" ]]; then
      if [[ "$host" == "$PROD_DOMAIN" || "$host" == *".$PROD_DOMAIN" ]]; then
        verdict="FAIL"
        reason="points at prod domain ($PROD_DOMAIN) on local"
      fi
    fi

    [[ "$verdict" == "FAIL" ]] && FAILED=$((FAILED + 1))
    if [[ -n "$reason" ]]; then
      CHECK_LINES+=("[$verdict] $link (status=$status) — $reason")
    else
      CHECK_LINES+=("[$verdict] $link (status=$status)")
    fi
  done < <(extract_links "$body")
done

# --- Output -----------------------------------------------------------------
if [[ "$JSON_OUTPUT" == "true" ]]; then
  printf '{"checked":%d,"failed":%d}\n' "$CHECKED" "$FAILED"
  [[ $FAILED -eq 0 ]] && exit 0 || exit 1
fi

echo "=== Email-link resolvability smoke (mode=$MODE, prod-domain=$PROD_DOMAIN) ==="
echo "MailHog: ${MAILHOG_FIXTURE:-$MAILHOG_URL}"
[[ "$MODE" == "local" ]] && echo "Expected local host: $LOCAL_HOST (links must NOT point at $PROD_DOMAIN)"
echo

if [[ $CHECKED -eq 0 ]]; then
  echo "⚠️  No http(s) links found in sent emails."
  echo "    (No emails sent yet, or MailHog unreachable, or bodies link-free.)"
  echo
  echo "PASS: nothing to check."
  exit 0
fi

for line in "${CHECK_LINES[@]}"; do
  if [[ "$line" == \[FAIL\]* ]]; then
    echo "❌ ${line#\[FAIL\] }"
  else
    echo "✅ ${line#\[PASS\] }"
  fi
done
echo

if [[ $FAILED -gt 0 ]]; then
  echo "FAIL: $FAILED of $CHECKED link(s) failed."
  echo "Fix: correct the email-link route (404) OR the base-URL config (prod domain on local)."
  exit 1
fi

echo "PASS: all $CHECKED link(s) resolvable$([[ "$MODE" == "local" ]] && echo " and none point at prod domain")."
exit 0

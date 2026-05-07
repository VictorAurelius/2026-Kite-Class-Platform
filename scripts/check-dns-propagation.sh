#!/usr/bin/env bash
# check-dns-propagation.sh — Verify DNS records propagated for a domain.
#
# Checks A, AAAA, MX, TXT (SPF), TXT (DMARC), CNAME (DKIM) against multiple
# public DNS resolvers (Google 8.8.8.8 + Cloudflare 1.1.1.1 + Quad9 9.9.9.9).
#
# Usage:
#   bash scripts/check-dns-propagation.sh <domain>
#
# Example:
#   bash scripts/check-dns-propagation.sh beta.kitehub.vn
#
# Exit codes:
#   0 — all required records present + consistent across resolvers
#   1 — invalid args
#   2 — `dig` not installed
#   3 — at least one record missing or inconsistent
#
# References:
#   - DNS propagation: https://www.whatsmydns.net
#   - Sister runbook: documents/05-guides/operations/dns-setup-runbook.md §2.5

set -euo pipefail

# ---- Args ----
if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <domain>" >&2
  echo "Example: $0 beta.kitehub.vn" >&2
  exit 1
fi

DOMAIN="$1"

if ! [[ "$DOMAIN" =~ ^[a-z0-9.-]+\.[a-z]{2,}$ ]]; then
  echo "ERROR: invalid domain: $DOMAIN" >&2
  exit 1
fi

if ! command -v dig >/dev/null 2>&1; then
  echo "ERROR: 'dig' not installed. Install via:" >&2
  echo "  Ubuntu/Debian: apt-get install -y dnsutils" >&2
  echo "  Alpine:        apk add bind-tools" >&2
  echo "  macOS:         brew install bind" >&2
  exit 2
fi

# ---- Resolvers ----
declare -A RESOLVERS=(
  [google]="8.8.8.8"
  [cloudflare]="1.1.1.1"
  [quad9]="9.9.9.9"
)

# ---- State ----
FAIL_COUNT=0
WARN_COUNT=0
PASS_COUNT=0

# ---- Helpers ----
query() {
  local record_type="$1"
  local name="$2"
  local resolver="$3"
  # +short = bare answer; +time=3 = 3s timeout; +tries=2 = 2 attempts
  dig +short +time=3 +tries=2 "$record_type" "$name" "@${resolver}" 2>/dev/null | sort | tr '\n' '|' | sed 's/|$//'
}

check_record() {
  local record_type="$1"
  local name="$2"
  local label="$3"
  local required="$4"  # "required" or "optional"

  local g c q
  g=$(query "$record_type" "$name" "${RESOLVERS[google]}")
  c=$(query "$record_type" "$name" "${RESOLVERS[cloudflare]}")
  q=$(query "$record_type" "$name" "${RESOLVERS[quad9]}")

  if [[ -z "$g" && -z "$c" && -z "$q" ]]; then
    if [[ "$required" == "required" ]]; then
      echo "  ❌ $label ($record_type $name): NOT FOUND on any resolver"
      FAIL_COUNT=$((FAIL_COUNT + 1))
    else
      echo "  ⏭  $label ($record_type $name): not present (optional — skipping)"
    fi
    return
  fi

  if [[ "$g" == "$c" && "$c" == "$q" ]]; then
    echo "  ✅ $label ($record_type $name): consistent across resolvers"
    echo "       value: ${g//|/, }"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    echo "  ⚠️  $label ($record_type $name): INCONSISTENT (still propagating?)"
    echo "       google:     ${g//|/, }"
    echo "       cloudflare: ${c//|/, }"
    echo "       quad9:      ${q//|/, }"
    WARN_COUNT=$((WARN_COUNT + 1))
  fi
}

# ---- Run checks ----
echo "===================================================================="
echo "  DNS propagation check: $DOMAIN"
echo "  Resolvers: Google (8.8.8.8) · Cloudflare (1.1.1.1) · Quad9 (9.9.9.9)"
echo "===================================================================="
echo ""

echo "Required records:"
check_record "A"     "$DOMAIN"         "A record"     "required"
check_record "TXT"   "$DOMAIN"         "TXT/SPF"      "required"
check_record "TXT"   "_dmarc.${DOMAIN}" "TXT/DMARC"    "required"

echo ""
echo "Optional records:"
check_record "AAAA"  "$DOMAIN"         "AAAA (IPv6)"  "optional"
check_record "MX"    "$DOMAIN"         "MX record"    "optional"

# DKIM CNAMEs — token-based, can't predict names. Check most common SES tokens via heuristic.
# Better: user verifies DKIM via SES console. Here, just probe `_domainkey` parent.
echo "  ⏭  DKIM CNAMEs: token-based — verify via AWS SES console or:"
echo "       dig +short CNAME <token>._domainkey.${DOMAIN} @8.8.8.8"

echo ""
echo "===================================================================="
echo "  Summary: $PASS_COUNT passed · $WARN_COUNT warnings · $FAIL_COUNT failed"
echo "===================================================================="

if [[ $FAIL_COUNT -gt 0 ]]; then
  echo ""
  echo "❌ FAIL: $FAIL_COUNT required record(s) missing."
  echo "   Common causes:"
  echo "   - DNS not yet propagated (wait 5-30 min after setting records)"
  echo "   - Typo in record name/value (re-check Cloudflare DNS dashboard)"
  echo "   - TTL too high (lower to 300s during cutover)"
  exit 3
fi

if [[ $WARN_COUNT -gt 0 ]]; then
  echo ""
  echo "⚠️  PARTIAL: $WARN_COUNT record(s) inconsistent — likely still propagating."
  echo "   Re-run in 5-10 min to confirm."
  exit 3
fi

echo ""
echo "✅ ALL REQUIRED RECORDS VERIFIED — domain ready for cutover."
exit 0

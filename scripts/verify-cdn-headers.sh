#!/usr/bin/env bash
# verify-cdn-headers.sh — Verify Cloudflare CDN headers for a domain
#
# Usage:
#   bash scripts/verify-cdn-headers.sh <domain>
#   bash scripts/verify-cdn-headers.sh --help
#
# Exit codes:
#   0 — all required Cloudflare headers present
#   1 — one or more headers missing or domain argument not provided

set -euo pipefail

REQUIRED_HEADERS=(
  "CF-Ray"
  "CF-Cache-Status"
  "server: cloudflare"
  "Strict-Transport-Security"
)

usage() {
  cat <<'EOF'
Usage: verify-cdn-headers.sh <domain>

Check that a domain is proxied through Cloudflare CDN by verifying
the presence of required response headers.

Arguments:
  <domain>   Domain to check (e.g. kitehub.vn or kitehub.me)

Options:
  --help     Show this help message and exit

Required headers checked:
  CF-Ray                   — Cloudflare request trace ID
  CF-Cache-Status          — Cloudflare cache status (HIT/MISS/BYPASS/etc)
  Server: cloudflare       — Server identity header
  Strict-Transport-Security — HSTS enforcement header

Exit codes:
  0  All required headers present — domain is behind Cloudflare CDN
  1  One or more headers missing — domain may not be behind Cloudflare

Examples:
  bash scripts/verify-cdn-headers.sh kitehub.vn
  bash scripts/verify-cdn-headers.sh kitehub.me
EOF
}

# --- Argument parsing ---

if [[ $# -eq 0 ]]; then
  echo "ERROR: domain argument required." >&2
  echo "Run with --help for usage." >&2
  exit 1
fi

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
  usage
  exit 0
fi

DOMAIN="$1"

# Strip protocol prefix if user passes https://domain
DOMAIN="${DOMAIN#https://}"
DOMAIN="${DOMAIN#http://}"
# Strip trailing slash
DOMAIN="${DOMAIN%/}"

echo "=== Cloudflare CDN Header Verification ==="
echo "Domain : https://${DOMAIN}"
echo "Time   : $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo ""

# --- Fetch headers ---

HEADERS=$(curl -sI --max-time 10 --location "https://${DOMAIN}" 2>&1) || {
  echo "ERROR: Failed to connect to https://${DOMAIN}" >&2
  echo "       Check that the domain resolves and is reachable." >&2
  exit 1
}

# --- Check each required header ---

all_pass=true

check_header() {
  local label="$1"
  local pattern="$2"

  if echo "${HEADERS}" | grep -qi "${pattern}"; then
    printf "  [PASS] %s\n" "${label}"
  else
    printf "  [FAIL] %s — header not found\n" "${label}"
    all_pass=false
  fi
}

check_header "CF-Ray"                    "^CF-Ray:"
check_header "CF-Cache-Status"           "^CF-Cache-Status:"
check_header "Server: cloudflare"        "^Server:.*cloudflare"
check_header "Strict-Transport-Security" "^Strict-Transport-Security:"

echo ""

if [[ "${all_pass}" == "true" ]]; then
  echo "RESULT: PASS — all Cloudflare CDN headers present."
  echo "        Domain ${DOMAIN} is correctly proxied through Cloudflare."
  exit 0
else
  echo "RESULT: FAIL — one or more required headers missing."
  echo "        Ensure Cloudflare proxy is enabled (orange cloud) and"
  echo "        SSL/TLS mode is set to Full (strict)."
  echo ""
  echo "Raw response headers:"
  echo "${HEADERS}" | head -30
  exit 1
fi

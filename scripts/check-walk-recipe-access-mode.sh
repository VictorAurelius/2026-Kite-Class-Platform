#!/usr/bin/env bash
# check-walk-recipe-access-mode.sh — detector for g1-browser-walk-before-flip.md §3.1/§3.2.
#
# Walk artifacts (G2 recipes, flow wave plans, the campaign doc) must reference
# KiteClass (multi-tenant, subdomain-resolved) tenant-flow URLs via a real
# subdomain Host (nip.io: `<slug>.127.0.0.1.nip.io:3000`), NOT bare
# `localhost:3000` / `127.0.0.1:3000` nor `?tenant=` query-override — those
# bypass the Host-resolution path the flow needs to verify.
#
# KiteHub (`:3001`, platform single-domain console) is exempt — only KC `:3000`.
#
# Recurrence #2 (2026-06-16): GAP-811 `?tenant=` slip (2026-06-08, →§3.1) +
# Phase-3 consolidated recipe `localhost:3000` slip → detector shipped per
# incident-to-rule-pipeline.md §3.1 (recurrence ≥2) + cross-flow-bug-class-sweep
# §4.1 (statically-detectable class → persistent CI detector).
#
# Usage: bash scripts/check-walk-recipe-access-mode.sh [--strict]
#   default = WARN (exit 0, prints findings); --strict = exit 1 on any finding.

set -uo pipefail
cd "$(dirname "$0")/.."

STRICT=0
[ "${1:-}" = "--strict" ] && STRICT=1

# Walk-artifact globs (nullglob so empty globs vanish).
shopt -s nullglob
FILES=(
  documents/05-guides/operations/*g2-recipe*.md
  documents/03-planning/roadmap/flow-verification-campaign.md
  documents/03-planning/waves/*flow*.md
)
shopt -u nullglob

findings=0
for f in "${FILES[@]}"; do
  [ -f "$f" ] || continue
  # Only flag ACTUAL URLs (http:// scheme), not prose documenting the ban
  # (e.g. "CẤM `localhost:3000` / `?tenant=`"). FP-guard per g1-browser-walk §7.4.
  #
  # Bare KC host URL (port 3000 on plain localhost/127.0.0.1, no subdomain).
  # `http://<sub>.127.0.0.1.nip.io:3000` does NOT match (dot before nip.io, not colon).
  while IFS=: read -r line content; do
    [ -n "$line" ] || continue
    echo "  $f:$line — bare KC host URL (use http://<slug>.127.0.0.1.nip.io:3000): ${content#"${content%%[![:space:]]*}"}"
    findings=$((findings + 1))
  done < <(grep -nE 'https?://(localhost|127\.0\.0\.1):3000' "$f" 2>/dev/null)

  # ?tenant= inside an actual URL (banned as G1/G2 evidence). Prose mentions exempt.
  while IFS=: read -r line content; do
    [ -n "$line" ] || continue
    echo "  $f:$line — ?tenant= query-override URL (bypasses Host-resolution): ${content#"${content%%[![:space:]]*}"}"
    findings=$((findings + 1))
  done < <(grep -nE 'https?://[^ )`]*\?tenant=' "$f" 2>/dev/null)
done

if [ "$findings" -eq 0 ]; then
  echo "PASS: walk-artifact access-mode clean (KC tenant URLs use nip.io subdomain)"
  exit 0
fi

echo ""
echo "$findings access-mode violation(s) per g1-browser-walk-before-flip.md §3.1/§3.2."
echo "Fix: KC tenant-flow URL → http://<slug>.127.0.0.1.nip.io:3000/... (NOT bare localhost:3000 / ?tenant=)."
if [ "$STRICT" -eq 1 ]; then exit 1; fi
echo "(WARN mode — non-blocking; run with --strict to enforce)"
exit 0

#!/usr/bin/env bash
# check-ui-kits-landing.sh — landing page parity check for ui_kits/
#
# Verify documents/02-architecture/design-system/ui_kits/index.html lists every
# kit folder under ui_kits/ (excluding _shared/, _v1-baseline/).
#
# Usage:
#   bash documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh
#
# Exit codes:
#   0 — parity OK (landing has card for every kit folder)
#   1 — parity FAIL (kit folder exists without card OR card without folder)
#   2 — usage / file-not-found error
#
# Spec source: `.claude/rules/output-review-mandate.md` v1.3.0 §3 row "HTML/JSX
# prototypes" Process column. Tier 1 of `wave-2026-04-29-review-process-improvement.md`.
# Stricter version with link-resolution + per-screen state coverage lives in
# `.claude/skills/quality/ui-review-prototype/` (Tier 2, GAP-264).

set -euo pipefail

# ---- Locate paths -----------------------------------------------------------

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../../.." && pwd)"
KITS_DIR="$REPO_ROOT/documents/02-architecture/design-system/ui_kits"
LANDING="$KITS_DIR/index.html"

if [[ ! -d "$KITS_DIR" ]]; then
  echo "ERROR: ui_kits/ directory not found at $KITS_DIR" >&2
  exit 2
fi

if [[ ! -f "$LANDING" ]]; then
  echo "ERROR: landing page not found at $LANDING" >&2
  exit 2
fi

# ---- Enumerate actual kit folders ------------------------------------------

# Exclude _shared/, _v1-baseline/ subdirs (those are infra/reference, not kits)
mapfile -t KIT_FOLDERS < <(
  find "$KITS_DIR" -maxdepth 1 -mindepth 1 -type d -printf '%f\n' \
    | grep -vE '^_(shared|v1-baseline)$' \
    | sort
)

# ---- Enumerate cards in landing index.html ---------------------------------

# Grep `href="<slug>/"` patterns where slug starts with a-z (kit folders)
mapfile -t CARD_SLUGS < <(
  grep -oE 'href="[a-z][a-z0-9-]+/"' "$LANDING" \
    | sed -E 's|href="([^"]+)/"|\1|' \
    | sort -u
)

# ---- Compare ----------------------------------------------------------------

# Folders not in landing
mapfile -t MISSING_CARDS < <(comm -23 <(printf '%s\n' "${KIT_FOLDERS[@]}") <(printf '%s\n' "${CARD_SLUGS[@]}"))

# Cards pointing to non-existent folders
mapfile -t ORPHAN_CARDS < <(comm -13 <(printf '%s\n' "${KIT_FOLDERS[@]}") <(printf '%s\n' "${CARD_SLUGS[@]}"))

# ---- Report -----------------------------------------------------------------

echo "Landing page parity check"
echo "  Landing:  $LANDING"
echo "  Kits dir: $KITS_DIR"
echo
echo "Kit folders (${#KIT_FOLDERS[@]}):"
printf '  - %s\n' "${KIT_FOLDERS[@]}"
echo
echo "Cards in landing (${#CARD_SLUGS[@]}):"
printf '  - %s\n' "${CARD_SLUGS[@]}"
echo

if [[ ${#MISSING_CARDS[@]} -eq 0 && ${#ORPHAN_CARDS[@]} -eq 0 ]]; then
  echo "✅ Parity OK — every kit folder has a landing card and vice versa."
  exit 0
fi

if [[ ${#MISSING_CARDS[@]} -gt 0 ]]; then
  echo "❌ Missing landing cards (folder exists, no card):"
  printf '  - %s/\n' "${MISSING_CARDS[@]}"
fi

if [[ ${#ORPHAN_CARDS[@]} -gt 0 ]]; then
  echo "❌ Orphan landing cards (card exists, no folder):"
  printf '  - %s/\n' "${ORPHAN_CARDS[@]}"
fi

echo
echo "Fix: edit $LANDING to align cards with folder list."
exit 1

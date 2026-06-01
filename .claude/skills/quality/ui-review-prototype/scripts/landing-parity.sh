#!/usr/bin/env bash
# landing-parity.sh — stricter landing-page parity check (Tier 2).
#
# Extends Tier 1 (`_shared/scripts/check-ui-kits-landing.sh`) by also verifying:
#   • Card slug matches a real kit folder (REUSES Tier 1 logic via call-out)
#   • Each card displays a score number `<NUM>/128` (and presence of any
#     score in the card; we tolerate stars / decimals / plain int)
#   • Each card displays a persona pill (some pill text — sanity check)
#   • Each card's "screens · WaveX" or "demos · WaveX" count is plausible
#     vs the actual `find screens/ -name '*.html' | wc -l` per kit
#     (loose match: number printed in card must equal count OR be within
#     the same order of magnitude — kits with sub-folder structure count
#     differently; landing showing 29 demos for components/ across G2..G12
#     is expected)
#
# Usage:
#   bash .claude/skills/quality/ui-review-prototype/scripts/landing-parity.sh [--help]
#
# Exit codes:
#   0 — all parity checks pass
#   1 — at least one parity violation (folder/card/score/persona/count drift)
#   2 — usage / env error
#
# Spec source: `.claude/rules/output-review-mandate.md` v1.3.0 §3 row "HTML/JSX
# prototypes". Tier 2 of `wave-2026-04-29-review-process-improvement.md`.

set -euo pipefail

usage() {
  cat <<'EOF'
landing-parity.sh — stricter landing parity than Tier 1.

Usage: bash landing-parity.sh [--help]

Checks per kit folder:
  1. Folder ↔ landing card present (Tier 1 invariant — runs Tier 1 first)
  2. Card displays score "<NUM>/128"
  3. Card displays a persona-pill chip (any pill following kit title)
  4. Card "X screens · WaveY" / "X demos · WaveY" count is plausible
     vs actual screens/ HTML count
  5. Kit has its own index.html (GitHub Pages serves directory listing as
     404 without index — caught 2026-04-29 components/ live-demo miss)

Exit codes:
  0   pass
  1   parity violation (printed to stderr)
  2   environment / setup error
EOF
}

if [[ "${1-}" == "--help" || "${1-}" == "-h" ]]; then
  usage
  exit 0
fi

# ---- Locate paths -----------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
KITS_DIR="$REPO_ROOT/documents/02-architecture/design-system/ui_kits"
LANDING="$KITS_DIR/index.html"
TIER1_SCRIPT="$KITS_DIR/_shared/scripts/check-ui-kits-landing.sh"
LOG_FILE="$SKILL_DIR/data/runs.log"

if [[ ! -d "$KITS_DIR" ]]; then
  echo "ERROR: ui_kits/ not found at $KITS_DIR" >&2
  exit 2
fi

if [[ ! -f "$LANDING" ]]; then
  echo "ERROR: landing index.html not found at $LANDING" >&2
  exit 2
fi

mkdir -p "$(dirname "$LOG_FILE")"

ts() { date -u '+%Y-%m-%dT%H:%M:%SZ'; }
log_run() {
  local exit_code="$1" summary="$2"
  echo "$(ts) | landing-parity.sh | exit=$exit_code | $summary" >> "$LOG_FILE"
}

VIOLATIONS=()

# ---- Step 1: REUSE Tier 1 invariant -----------------------------------------
# Run Tier 1 silently. If Tier 1 fails, Tier 2 inherits the failure.

if [[ -f "$TIER1_SCRIPT" ]]; then
  if ! bash "$TIER1_SCRIPT" >/dev/null 2>&1; then
    # Re-run with output for users so they see WHY Tier 1 failed.
    echo "Tier 1 base parity FAILED — running with full output:" >&2
    bash "$TIER1_SCRIPT" >&2 || true
    VIOLATIONS+=("Tier 1 base parity (folder ↔ card slug mismatch)")
  fi
else
  echo "WARN: Tier 1 script not found at $TIER1_SCRIPT — Tier 2 will run base check inline" >&2
  # Inline Tier-1-equivalent base check.
  mapfile -t KIT_FOLDERS < <(
    find "$KITS_DIR" -maxdepth 1 -mindepth 1 -type d -printf '%f\n' \
      | grep -vE '^(_shared|_v1-baseline|marketing-site)$' \
      | sort
  )
  mapfile -t CARD_SLUGS < <(
    grep -oE 'href="[a-z][a-z0-9-]+/"' "$LANDING" \
      | sed -E 's|href="([^"]+)/"|\1|' \
      | sort -u
  )
  mapfile -t MISSING < <(comm -23 <(printf '%s\n' "${KIT_FOLDERS[@]}") <(printf '%s\n' "${CARD_SLUGS[@]}"))
  mapfile -t ORPHAN  < <(comm -13 <(printf '%s\n' "${KIT_FOLDERS[@]}") <(printf '%s\n' "${CARD_SLUGS[@]}"))
  for m in "${MISSING[@]}"; do VIOLATIONS+=("Missing card for kit folder: $m/"); done
  for o in "${ORPHAN[@]}";  do VIOLATIONS+=("Orphan card (no folder): $o/"); done
fi

# Re-enumerate kit folders for stricter checks (works regardless of Tier 1 path).
# Exempt `marketing-site`: it's a platform marketing/beta-signup landing scored on the
# /100 landing-checklist rubric (landing-page-review-checklist.md) — a single page with no
# screens/ dir — NOT a /128 multi-screen UI kit. It deploys to Pages via ui_kits/ + has a
# gallery card (Tier-1 folder↔card still enforced), but the /128-score + N-screens strict
# checks don't apply. Authoritative review = production (public)/page.tsx post-port.
mapfile -t KIT_FOLDERS < <(
  find "$KITS_DIR" -maxdepth 1 -mindepth 1 -type d -printf '%f\n' \
    | grep -vE '^(_shared|_v1-baseline|marketing-site)$' \
    | sort
)

# ---- Step 2/3/4: stricter per-kit checks -----------------------------------
# Strategy: extract per-card block from landing index.html.
# Each card matches: <a href="<slug>/" ...>...</a> ending tag.
#
# Use awk to slice by anchor tags. We don't trust HTML parsing so we use a
# coarse approach: find the line containing href="$slug/" and slurp the next
# ~25 lines (cards in this index are short).

extract_card_block() {
  local slug="$1"
  awk -v slug="$slug" '
    BEGIN { capture=0; depth=0; }
    {
      if (capture == 0) {
        if ($0 ~ "href=\"" slug "/\"") { capture=1; depth=1; print; next; }
      } else {
        print;
        if ($0 ~ /<a[ \t]/)      depth++;
        if ($0 ~ /<\/a>/)        depth--;
        if (depth <= 0) { exit; }
      }
    }
  ' "$LANDING"
}

count_kit_screens() {
  local slug="$1"
  local kit_dir="$KITS_DIR/$slug"
  local count=0
  if [[ -d "$kit_dir/screens" ]]; then
    # Flat layout: screens/*.html
    count=$(find "$kit_dir/screens" -maxdepth 1 -type f -name '*.html' 2>/dev/null | wc -l)
  fi
  if [[ "$count" == "0" ]]; then
    # Sub-folder layout (components/G2-attendance-roster/default.html etc.)
    count=$(find "$kit_dir" -maxdepth 3 -type f -name '*.html' \
              -not -name 'index.html' -not -name '_partials.html' \
              -not -path "$kit_dir/_v1-baseline/*" 2>/dev/null | wc -l)
  fi
  echo "$count"
}

for slug in "${KIT_FOLDERS[@]}"; do
  # Check 5: each kit folder has its own index.html (GitHub Pages 404 guard)
  if [[ ! -f "$KITS_DIR/$slug/index.html" ]]; then
    VIOLATIONS+=("Kit '$slug/' missing index.html (GitHub Pages will 404 on live demo)")
  fi

  block="$(extract_card_block "$slug" || true)"
  if [[ -z "$block" ]]; then
    # If Tier 1 already flagged missing card, skip stricter checks for this slug.
    continue
  fi

  # Check 2: score "NUM/128" present
  if ! echo "$block" | grep -qE '[0-9]+(\.[0-9]+)?[[:space:]]*/[[:space:]]*128'; then
    VIOLATIONS+=("Card '$slug' missing score '<NUM>/128'")
  fi

  # Check 3: persona pill — heuristic: at least one <span class="... px-2 ...">
  # with non-empty text inside the card block. Pills in this design use
  # `class="px-2 py-0.5 bg-... rounded">PERSONA</span>` pattern.
  pill_count=$(echo "$block" | grep -cE 'class="[^"]*px-2[^"]*rounded[^"]*">[^<]+<' || true)
  if (( pill_count < 1 )); then
    VIOLATIONS+=("Card '$slug' has no persona pill (no px-2 rounded chip with text)")
  fi

  # Check 4: screens count plausibility
  card_count=$(echo "$block" | grep -oE '[0-9]+[[:space:]]+(screens|demos)' | head -1 | grep -oE '^[0-9]+' || true)
  actual_count="$(count_kit_screens "$slug")"

  if [[ -z "$card_count" ]]; then
    VIOLATIONS+=("Card '$slug' missing 'N screens' or 'N demos' count")
  elif [[ "$card_count" -gt 0 && "$actual_count" -gt 0 ]]; then
    # Tolerance: components-style sub-folder layouts can diverge (29 demos = 5 components × 5-6 states).
    # Accept if card_count == actual_count OR ratio within 2x in either direction.
    ratio_ok=0
    if [[ "$card_count" -eq "$actual_count" ]]; then
      ratio_ok=1
    elif (( card_count <= actual_count * 2 && actual_count <= card_count * 2 )); then
      ratio_ok=1
    fi
    if [[ "$ratio_ok" -eq 0 ]]; then
      VIOLATIONS+=("Card '$slug' shows $card_count screens/demos but actual is $actual_count (>2x drift)")
    fi
  fi
done

# ---- Report -----------------------------------------------------------------

echo "Landing parity (Tier 2 — stricter)"
echo "  Landing: $LANDING"
echo "  Kits:    ${#KIT_FOLDERS[@]}"
echo

if [[ ${#VIOLATIONS[@]} -eq 0 ]]; then
  echo "PASS — folder/card/score/persona/count/index parity holds for ${#KIT_FOLDERS[@]} kits."
  log_run 0 "kits=${#KIT_FOLDERS[@]} violations=0"
  exit 0
fi

echo "FAIL — ${#VIOLATIONS[@]} parity violation(s):" >&2
for v in "${VIOLATIONS[@]}"; do
  echo "  - $v" >&2
done
log_run 1 "kits=${#KIT_FOLDERS[@]} violations=${#VIOLATIONS[@]}"
exit 1

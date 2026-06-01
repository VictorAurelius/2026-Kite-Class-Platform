#!/usr/bin/env bash
# state-coverage.sh — verify each kit ships the minimum required state files.
#
# Per `dossier/10-acceptance-criteria.md` §4 "States" each kit screen MUST
# have these state variants in `screens/`: default / loading / empty / error
# / success / dark.
#
# This script enforces a MINIMUM bar (not the full §4 list) so that real-world
# kits which intentionally skip some states (e.g. components/ where every
# subfolder is one screen and some states are N/A) still pass.
#
# Minimum bar per kit:
#   - At least 1 file matching `*default*.html` OR `default.html`
#   - At least 1 of: loading, empty, error (in any *<state>*.html naming)
#
# Missing OPTIONAL states (success, dark) are reported as warnings (do NOT
# fail the run) so reviewers see drift over time without blocking merges.
#
# Usage:
#   bash .claude/skills/quality/ui-review-prototype/scripts/state-coverage.sh [--help]
#
# Exit codes:
#   0 — minimum coverage met for all kits (warnings allowed)
#   1 — at least one kit missing the minimum bar
#   2 — usage / env error
#
# Spec source: `.claude/rules/output-review-mandate.md` v1.3.0 §3 row "HTML/JSX
# prototypes" + `dossier/10-acceptance-criteria.md` §4. Tier 2 of
# `wave-2026-04-29-review-process-improvement.md`.

set -euo pipefail

usage() {
  cat <<'EOF'
state-coverage.sh — minimum state-file coverage per kit.

Usage: bash state-coverage.sh [--help]

Minimum bar per kit (FAIL if not met):
  • ≥1 *default*.html OR default.html (or every component subfolder has default.html)
  • ≥1 of: loading | empty | error

Optional states (WARN, don't fail):
  • success
  • dark

Exit codes:
  0   minimum bar met (warnings allowed)
  1   minimum bar missing for ≥1 kit
  2   environment error
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
LOG_FILE="$SKILL_DIR/data/runs.log"

if [[ ! -d "$KITS_DIR" ]]; then
  echo "ERROR: ui_kits/ not found at $KITS_DIR" >&2
  exit 2
fi

mkdir -p "$(dirname "$LOG_FILE")"

ts() { date -u '+%Y-%m-%dT%H:%M:%SZ'; }
log_run() {
  local exit_code="$1" summary="$2"
  echo "$(ts) | state-coverage.sh | exit=$exit_code | $summary" >> "$LOG_FILE"
}

# ---- Enumerate kits ---------------------------------------------------------

mapfile -t KIT_FOLDERS < <(
  # Exempt `marketing-site`: platform marketing/beta-signup landing (single page, /100
  # landing-checklist rubric) — not a /128 multi-state screen-kit, so state-file coverage
  # (default/loading/empty/error/success/dark) does not apply. Same exemption as landing-parity.sh.
  find "$KITS_DIR" -maxdepth 1 -mindepth 1 -type d -printf '%f\n' \
    | grep -vE '^(_shared|_v1-baseline|marketing-site)$' \
    | sort
)

if [[ ${#KIT_FOLDERS[@]} -eq 0 ]]; then
  echo "ERROR: no kit folders found under $KITS_DIR" >&2
  log_run 2 "no kits"
  exit 2
fi

# ---- Per-kit check ----------------------------------------------------------

FAILURES=()
WARNINGS=()

# Kits that PREDATE the Tier-2 state-coverage convention (GAP-264). They ship
# real screens but not the `<screen>-<state>.html` naming, OR are single-screen
# marketing kits where app-states (loading/empty/error) are N/A. Grandfathered:
# minimum-coverage misses report as WARN (not FAIL) so the gate enforces the
# convention prospectively on NEW kits without retro-blocking legacy ones.
# Remove a slug here once the kit is brought up to full state coverage.
GRANDFATHERED_KITS=(
  "kiteclass-student"   # Round-3 app kit; 13 screens, no -default-named file
  "kitehub-admin"       # Round-3 app kit; 12 screens, no -default-named file
  "kitehub-story-v2"    # single-screen marketing/story kit — app-states N/A
)
is_grandfathered() {
  local s="$1"
  for g in "${GRANDFATHERED_KITS[@]}"; do [[ "$g" == "$s" ]] && return 0; done
  return 1
}

# kit_state_files <kit-slug> — emits HTML basenames considered state files
kit_state_files() {
  local slug="$1"
  local kit_dir="$KITS_DIR/$slug"

  if [[ -d "$kit_dir/screens" ]]; then
    find "$kit_dir/screens" -maxdepth 1 -type f -name '*.html' -printf '%f\n' 2>/dev/null
  else
    # Sub-folder layout: each subfolder is one screen with state files inside.
    find "$kit_dir" -maxdepth 3 -type f -name '*.html' -printf '%f\n' \
      -not -name 'index.html' -not -name '_partials.html' \
      -not -path "$kit_dir/_v1-baseline/*" 2>/dev/null
  fi
}

state_present() {
  local state="$1"
  shift
  local files=("$@")
  for f in "${files[@]}"; do
    # Match either bare `<state>.html` or `<screen>-<state>.html` or any
    # filename containing the state token (e.g., `dashboard-loading.html`).
    if [[ "$f" =~ (^|-)"$state"(\.|-) ]]; then
      return 0
    fi
  done
  return 1
}

echo "State coverage per kit"
echo "  Kits dir: $KITS_DIR"
echo

for slug in "${KIT_FOLDERS[@]}"; do
  mapfile -t files < <(kit_state_files "$slug")
  total="${#files[@]}"

  echo "  $slug ($total HTML files):"

  # Minimum: default + (loading | empty | error)
  has_default="no"
  has_one_of="no"

  if state_present default "${files[@]}"; then has_default="yes"; fi
  if state_present loading "${files[@]}" || \
     state_present empty   "${files[@]}" || \
     state_present error   "${files[@]}"; then
    has_one_of="yes"
  fi

  # Optional states (warn-only)
  has_success="no"; has_dark="no"
  if state_present success "${files[@]}"; then has_success="yes"; fi
  if state_present dark    "${files[@]}"; then has_dark="yes"; fi

  printf '    default: %s | one_of(loading|empty|error): %s | success: %s | dark: %s\n' \
    "$has_default" "$has_one_of" "$has_success" "$has_dark"

  if is_grandfathered "$slug"; then
    [[ "$has_default" == "no" ]] && WARNINGS+=("Kit '$slug' missing 'default' state file (grandfathered — predates Tier-2 convention)")
    [[ "$has_one_of" == "no" ]] && WARNINGS+=("Kit '$slug' missing {loading,empty,error} (grandfathered — predates Tier-2 convention)")
  else
    if [[ "$has_default" == "no" ]]; then
      FAILURES+=("Kit '$slug' missing 'default' state file")
    fi
    if [[ "$has_one_of" == "no" ]]; then
      FAILURES+=("Kit '$slug' missing all of {loading, empty, error} — need at least one")
    fi
  fi
  if [[ "$has_success" == "no" ]]; then
    WARNINGS+=("Kit '$slug' missing optional 'success' state")
  fi
  if [[ "$has_dark" == "no" ]]; then
    WARNINGS+=("Kit '$slug' missing optional 'dark' state")
  fi
done

# ---- Report -----------------------------------------------------------------

echo

if [[ ${#WARNINGS[@]} -gt 0 ]]; then
  echo "Warnings (optional states):"
  for w in "${WARNINGS[@]}"; do
    echo "  ! $w"
  done
  echo
fi

if [[ ${#FAILURES[@]} -eq 0 ]]; then
  echo "PASS — minimum state coverage met for ${#KIT_FOLDERS[@]} kits."
  log_run 0 "kits=${#KIT_FOLDERS[@]} fails=0 warns=${#WARNINGS[@]}"
  exit 0
fi

echo "FAIL — ${#FAILURES[@]} kit(s) missing minimum state coverage:" >&2
for fline in "${FAILURES[@]}"; do
  echo "  - $fline" >&2
done
log_run 1 "kits=${#KIT_FOLDERS[@]} fails=${#FAILURES[@]} warns=${#WARNINGS[@]}"
exit 1

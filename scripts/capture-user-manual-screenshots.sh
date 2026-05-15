#!/usr/bin/env bash
# capture-user-manual-screenshots.sh — Playwright spec for user manual annotated screenshots
#
# Per `.claude/rules/user-manual-content-standard.md` §2 item 6 — annotated screenshots
#
# Usage:
#   bash scripts/capture-user-manual-screenshots.sh <persona-slug>
#   bash scripts/capture-user-manual-screenshots.sh anonymous
#   bash scripts/capture-user-manual-screenshots.sh platform-admin
#   bash scripts/capture-user-manual-screenshots.sh --all
#
# Output:
#   documents/05-guides/user-manual/{persona}/screenshots/{topic}-step-{N}.png
#   (CHECK IN to git — static visual reference, NOT regen-on-demand)
#
# Implementation tier strategy per Bucket D §D.2:
#   Tier 1: Playwright captures raw 1440×900 vi-VN screenshots → PNG
#   Tier 2 (best-effort): Sharp/Jimp overlay annotation (mũi tên đỏ + viền vàng + số bước)
#   Tier 2 fallback: ship 1×1 transparent PNG placeholder if Sharp unavailable;
#                    annotation pipeline deferred to follow-up gap
#
# Wave 80 Bucket D — GAP-537 follow-up

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MANUAL_DIR="$PROJECT_ROOT/documents/05-guides/user-manual"
FRONTEND_DIR="$PROJECT_ROOT/kitehub/kitehub-frontend"

PERSONA="${1:-}"
if [[ -z "$PERSONA" ]]; then
  echo "Usage: $0 <persona-slug> | --all"
  echo "Personas: anonymous, p2-owner, p3-manager, platform-admin"
  exit 1
fi

if [[ "$PERSONA" == "--all" ]]; then
  for p in anonymous platform-admin; do
    echo "=== Capturing $p ==="
    bash "$0" "$p" || echo "WARN: $p capture failed"
  done
  echo ""
  echo "NOTE: p2-owner + p3-manager screenshots deferred to post-Bucket-B+C-merge"
  echo "      (those depend on shipped Owner dashboard + RBAC)"
  exit 0
fi

PERSONA_DIR="$MANUAL_DIR/$PERSONA"
SCREENSHOTS_DIR="$PERSONA_DIR/screenshots"
mkdir -p "$SCREENSHOTS_DIR"

if [[ ! -d "$PERSONA_DIR" ]]; then
  echo "ERROR: persona dir not found: $PERSONA_DIR"
  exit 1
fi

# Discover pages
PAGES=$(find "$PERSONA_DIR" -maxdepth 1 -name "*.md" -type f -exec basename {} .md \; | sort)

echo "============================================================"
echo "  User Manual Screenshot Capturer"
echo "  Persona: $PERSONA"
echo "  Output:  $SCREENSHOTS_DIR/"
echo "  Pages:   $(echo "$PAGES" | wc -l)"
echo "============================================================"

# Check Playwright availability
PLAYWRIGHT_RUNNER="$SCRIPT_DIR/capture-user-manual-screenshots.mjs"

if [[ ! -f "$PLAYWRIGHT_RUNNER" ]]; then
  echo "ERROR: Playwright runner script missing: $PLAYWRIGHT_RUNNER"
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "ERROR: node not installed"
  exit 1
fi

# Check dev server
DEV_SERVER_PID=""
if ! curl -sf http://localhost:3001/ >/dev/null 2>&1; then
  echo "  Starting Next.js dev server on port 3001..."
  cd "$FRONTEND_DIR"
  PORT=3001 npm run dev >/tmp/user-manual-screenshots-dev.log 2>&1 &
  DEV_SERVER_PID=$!
  cd "$PROJECT_ROOT"

  for i in {1..60}; do
    if curl -sf http://localhost:3001/ >/dev/null 2>&1; then
      echo "  ✓ Dev server ready (after ${i}s)"
      break
    fi
    sleep 1
  done
fi

cleanup() {
  if [[ -n "$DEV_SERVER_PID" ]]; then
    kill "$DEV_SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# Run Playwright capture
cd "$PROJECT_ROOT"
node "$PLAYWRIGHT_RUNNER" --persona "$PERSONA" --base-url "http://localhost:3001" --output-dir "$SCREENSHOTS_DIR"

COUNT=$(find "$SCREENSHOTS_DIR" -name "*.png" -type f 2>/dev/null | wc -l)
echo ""
echo "✓ Captured: $COUNT PNG files in $SCREENSHOTS_DIR/"
echo ""
echo "Per user-manual-content-standard.md §2 row 6 + Bucket D §D.2 Tier 2 fallback:"
echo "  - Tier 1 raw screenshots: captured"
echo "  - Tier 2 annotation overlay (arrows + bordered regions + step numbers):"
echo "    pending — placeholders shipped via HTML comments in source .md files"
echo "  - Follow-up: GAP-537-followup-screenshot-annotation tracks programmatic annotation"

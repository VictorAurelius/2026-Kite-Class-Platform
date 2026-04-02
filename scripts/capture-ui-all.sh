#!/usr/bin/env bash
# capture-ui-all.sh — Capture screenshots for BOTH KiteClass + KiteHub frontends
#
# Usage:
#   ./scripts/capture-ui-all.sh                     → latest/
#   ./scripts/capture-ui-all.sh --label pr-XXX      → pr-XXX/ + latest/ (both apps)
#   APP=kiteclass ./scripts/capture-ui-all.sh        → KiteClass only
#   APP=kitehub ./scripts/capture-ui-all.sh          → KiteHub only
#
# Prerequisites:
#   - Node + tsx installed globally (npm install -g tsx)
#   - Playwright chromium: npx playwright install chromium (in each frontend dir)
#   - Dev servers may be started automatically (npm run dev) if not already running
#
# WSL2 + NTFS note:
#   If running on /mnt/f/ (NTFS mount), start dev servers from Windows PowerShell first:
#     cd F:\nam4\doan\2026-Kite-Class-Platform\kiteclass\kiteclass-frontend && npm run dev
#     cd F:\nam4\doan\2026-Kite-Class-Platform\kitehub\kitehub-frontend && npm run dev
#   Then run this script from WSL2 (it connects to already-running servers).

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Parse --label argument
LABEL=""
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --label) LABEL="$2"; shift 2 ;;
    *) shift ;;
  esac
done

LABEL_ARGS=""
if [[ -n "$LABEL" ]]; then
  LABEL_ARGS="--label $LABEL"
fi

# Which apps to capture (default: both)
APP="${APP:-all}"

echo "============================================"
echo "  KiteClass + KiteHub UI Capture"
echo "  Label: ${LABEL:-latest}"
echo "  App:   $APP"
echo "============================================"
echo ""

if [[ "$APP" == "all" || "$APP" == "kiteclass" ]]; then
  echo "▶ Capturing KiteClass (port 3000)..."
  echo ""
  cd "$PROJECT_ROOT/kiteclass/kiteclass-frontend"
  npx tsx scripts/capture-screenshots.ts $LABEL_ARGS
  echo ""
fi

if [[ "$APP" == "all" || "$APP" == "kitehub" ]]; then
  echo "▶ Capturing KiteHub (port 3001)..."
  echo ""
  cd "$PROJECT_ROOT/kitehub/kitehub-frontend"
  npx tsx scripts/capture-screenshots.ts $LABEL_ARGS
  echo ""
fi

echo "============================================"
echo "  ✅ All captures complete"
echo ""
if [[ -n "$LABEL" ]]; then
  echo "  📁 KiteClass: documents/screenshots/$LABEL/"
  echo "  📁 KiteHub:   documents/screenshots/kitehub-$LABEL/"
else
  echo "  📁 KiteClass: documents/screenshots/latest/"
  echo "  📁 KiteHub:   documents/screenshots/kitehub-latest/"
fi
echo "  📋 Manifests: manifest.md in each folder"
echo "============================================"

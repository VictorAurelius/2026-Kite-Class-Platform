#!/usr/bin/env bash
# render-user-manual-pdf.sh — Render user manual Markdown → A4 PDF per persona
#
# Per `.claude/rules/user-manual-content-standard.md` §2 item 15 — PDF auto-generation
# Per `.claude/rules/test-artifact-format-standard.md` §4.2 — gitignored, regen-on-demand
#
# Usage:
#   bash scripts/render-user-manual-pdf.sh <persona-slug>
#   bash scripts/render-user-manual-pdf.sh anonymous
#   bash scripts/render-user-manual-pdf.sh p2-owner
#   bash scripts/render-user-manual-pdf.sh p3-manager
#   bash scripts/render-user-manual-pdf.sh platform-admin
#   bash scripts/render-user-manual-pdf.sh --all              # render all 4 personas
#
# Output:
#   documents/05-guides/user-manual/{persona-slug}-manual.pdf
#   (gitignored — regen on demand)
#
# Engine: Puppeteer headless Chrome via local Next.js dev server (port 3001).
# Falls back to wkhtmltopdf if Puppeteer unavailable.
#
# Prerequisites:
#   - Node.js + pnpm installed
#   - kitehub-frontend dev server can start on port 3001 (auto-spawned)
#   - puppeteer + pdf-lib OR puppeteer alone (cli concatenates PDFs internally)
#
# Wave 80 Bucket D — GAP-537 follow-up — F2 user manual full retrofit

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MANUAL_DIR="$PROJECT_ROOT/documents/05-guides/user-manual"
FRONTEND_DIR="$PROJECT_ROOT/kitehub/kitehub-frontend"

# Persona resolution
PERSONA="${1:-}"
if [[ -z "$PERSONA" ]]; then
  echo "Usage: $0 <persona-slug> | --all"
  echo "Personas: anonymous, p2-owner, p3-manager, platform-admin"
  exit 1
fi

if [[ "$PERSONA" == "--all" ]]; then
  for p in anonymous p2-owner p3-manager platform-admin; do
    echo "=== Rendering $p ==="
    bash "$0" "$p" || echo "WARN: $p render failed, continuing..."
  done
  exit 0
fi

# Validate persona folder exists
PERSONA_DIR="$MANUAL_DIR/$PERSONA"
if [[ ! -d "$PERSONA_DIR" ]]; then
  echo "ERROR: persona folder not found: $PERSONA_DIR"
  echo "Available personas:"
  ls -d "$MANUAL_DIR"/*/ 2>/dev/null | xargs -n1 basename
  exit 1
fi

# Count source files
SOURCE_COUNT=$(find "$PERSONA_DIR" -maxdepth 1 -name "*.md" -type f | wc -l)
if [[ "$SOURCE_COUNT" -eq 0 ]]; then
  echo "ERROR: no Markdown source files in $PERSONA_DIR"
  exit 1
fi

echo "============================================================"
echo "  User Manual PDF Renderer"
echo "  Persona: $PERSONA"
echo "  Sources: $SOURCE_COUNT Markdown files"
echo "  Output:  $MANUAL_DIR/$PERSONA-manual.pdf"
echo "============================================================"

# Detect engine
USE_PUPPETEER=true
if ! command -v node >/dev/null 2>&1; then
  echo "WARN: node not installed; cannot use Puppeteer engine"
  USE_PUPPETEER=false
fi

# Path to renderer
RENDERER="$SCRIPT_DIR/render-user-manual-pdf.mjs"

if [[ "$USE_PUPPETEER" == "true" ]] && [[ -f "$RENDERER" ]]; then
  echo "▶ Using Puppeteer renderer..."
  cd "$PROJECT_ROOT"

  # Check if dev server is reachable on 3001 — spawn if not
  DEV_SERVER_PID=""
  if ! curl -sf http://localhost:3001/ >/dev/null 2>&1; then
    echo "  Starting Next.js dev server on port 3001..."
    cd "$FRONTEND_DIR"
    PORT=3001 npm run dev >/tmp/user-manual-pdf-dev-server.log 2>&1 &
    DEV_SERVER_PID=$!
    cd "$PROJECT_ROOT"

    # Wait up to 60s for server ready
    for i in {1..60}; do
      if curl -sf http://localhost:3001/ >/dev/null 2>&1; then
        echo "  ✓ Dev server ready (after ${i}s)"
        break
      fi
      sleep 1
    done

    if ! curl -sf http://localhost:3001/ >/dev/null 2>&1; then
      echo "ERROR: dev server failed to start. See /tmp/user-manual-pdf-dev-server.log"
      [[ -n "$DEV_SERVER_PID" ]] && kill "$DEV_SERVER_PID" 2>/dev/null
      exit 1
    fi
  fi

  # Cleanup trap
  cleanup() {
    if [[ -n "$DEV_SERVER_PID" ]]; then
      echo "  Stopping dev server (PID $DEV_SERVER_PID)..."
      kill "$DEV_SERVER_PID" 2>/dev/null || true
    fi
  }
  trap cleanup EXIT

  # Run Puppeteer renderer
  node "$RENDERER" --persona "$PERSONA" --base-url "http://localhost:3001" --output "$MANUAL_DIR/$PERSONA-manual.pdf"

  if [[ -f "$MANUAL_DIR/$PERSONA-manual.pdf" ]]; then
    SIZE=$(du -h "$MANUAL_DIR/$PERSONA-manual.pdf" | cut -f1)
    echo "✓ Generated: $MANUAL_DIR/$PERSONA-manual.pdf ($SIZE)"
  else
    echo "ERROR: PDF not generated"
    exit 1
  fi
else
  # Fallback: pandoc + wkhtmltopdf
  echo "▶ Using fallback engine (pandoc → wkhtmltopdf)..."
  if ! command -v pandoc >/dev/null 2>&1; then
    echo "ERROR: pandoc not installed. Install via: apt-get install -y pandoc wkhtmltopdf"
    exit 1
  fi

  # Concatenate all .md (sorted: index first)
  TMP_MD=$(mktemp /tmp/user-manual-$PERSONA-XXXXX.md)
  trap "rm -f $TMP_MD" EXIT

  if [[ -f "$PERSONA_DIR/index.md" ]]; then
    cat "$PERSONA_DIR/index.md" > "$TMP_MD"
    echo -e "\n\n\\\\newpage\n\n" >> "$TMP_MD"
  fi

  for md in "$PERSONA_DIR"/*.md; do
    if [[ "$(basename "$md")" == "index.md" ]]; then continue; fi
    cat "$md" >> "$TMP_MD"
    echo -e "\n\n\\\\newpage\n\n" >> "$TMP_MD"
  done

  pandoc "$TMP_MD" \
    --pdf-engine=wkhtmltopdf \
    --metadata title="KiteHub Hướng dẫn - $PERSONA" \
    --metadata lang=vi-VN \
    --variable papersize=a4 \
    --variable geometry:margin=2cm \
    -o "$MANUAL_DIR/$PERSONA-manual.pdf"

  echo "✓ Generated (pandoc fallback): $MANUAL_DIR/$PERSONA-manual.pdf"
fi

echo ""
echo "Done. PDF gitignored per .gitignore — regen on demand."

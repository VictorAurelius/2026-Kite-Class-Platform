#!/usr/bin/env bash
# Wave thesis-2 Round 2 Issue 1 — auto-screenshot thesis docx pages for visual verify
#
# Usage: bash scripts/screenshot-thesis-docx.sh [docx-path] [out-dir]
#   Defaults: docx=documents/08-thesis/thesis-v1.docx, out=/tmp/thesis-screenshots
#
# Pipeline: docx --LibreOffice--> pdf --pypdfium2--> PNGs
# Renders sample pages (every 5th + key pages) for Claude/dev visual review.

set -uo pipefail

DOCX="${1:-documents/08-thesis/thesis-v1.docx}"
OUT_DIR="${2:-/tmp/thesis-screenshots}"

if [[ ! -f "$DOCX" ]]; then
    echo "FAIL: docx file not found: $DOCX"
    exit 1
fi

echo "============================================================"
echo "Auto-screenshot thesis docx → PNGs"
echo "  docx: $DOCX"
echo "  out:  $OUT_DIR"
echo "============================================================"

# Step 1: docx → pdf via LibreOffice headless
pkill -9 -f soffice 2>/dev/null || true
sleep 3
rm -rf /tmp/thesis-render
mkdir -p /tmp/thesis-render
rm -rf /tmp/lo-profile-screenshot && mkdir -p /tmp/lo-profile-screenshot

echo ""
echo "[1/2] LibreOffice docx → pdf..."
timeout 180 soffice -env:UserInstallation=file:///tmp/lo-profile-screenshot \
    --headless --norestore --nologo --nofirststartwizard \
    --convert-to pdf --outdir /tmp/thesis-render/ \
    "$DOCX" 2>&1 | tail -3

PDF="/tmp/thesis-render/$(basename "${DOCX%.docx}").pdf"
if [[ ! -f "$PDF" ]]; then
    echo "FAIL: PDF conversion failed"
    exit 1
fi
echo "  → $PDF ($(stat -c%s "$PDF" | awk '{printf "%.1f MB", $1/1024/1024}'))"

# Step 2: pdf → PNGs via pypdfium2
echo ""
echo "[2/2] pypdfium2 pdf → PNGs..."

rm -rf "$OUT_DIR" && mkdir -p "$OUT_DIR"

python3 <<PYEOF
import sys
sys.path.insert(0, '/home/nguyenvankiet/.local/lib/python3.12/site-packages')
import pypdfium2 as pdfium
import os

pdf = pdfium.PdfDocument("$PDF")
n_pages = len(pdf)
print(f"  PDF has {n_pages} pages")

# Sample strategy:
# - First 8 pages (cover, bìa phụ, lời cảm ơn, mục lục, danh mục)
# - Every 5th page after that
# - Last 2 pages (TLTK end)
sample = list(range(min(8, n_pages)))
sample.extend(range(8, n_pages, 5))
sample.extend([n_pages - 2, n_pages - 1])
sample = sorted(set(p for p in sample if 0 <= p < n_pages))

for i in sample:
    page = pdf[i]
    bitmap = page.render(scale=1.5)  # 108 DPI for good quality
    img = bitmap.to_pil()
    out_path = f"$OUT_DIR/page-{i+1:03d}.png"
    img.save(out_path, optimize=True)
    size_kb = os.path.getsize(out_path) // 1024
    print(f"  page-{i+1:03d}.png  {img.size}  {size_kb}KB")

pdf.close()
print(f"\n  Total {len(sample)} pages rendered")
PYEOF

echo ""
echo "============================================================"
echo "Done. Browse PNGs:"
echo "  ls $OUT_DIR/"
echo "  Or open in image viewer."
echo "============================================================"

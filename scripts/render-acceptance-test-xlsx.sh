#!/usr/bin/env bash
# render-acceptance-test-xlsx.sh
#
# Render acceptance-test CSV (canonical) → XLSX (generated, gitignored)
# Per .claude/rules/test-artifact-format-standard.md §4 — XLSX features:
#   - Header row bold + frozen pane
#   - Auto-fit column widths
#   - UTF-8 BOM tolerated (BOM stripped during read)
#
# Usage:
#   bash scripts/render-acceptance-test-xlsx.sh <path-to.csv>
#
# Output: <same-basename>.xlsx in same directory as input.
#
# Engines (tried in order):
#   1. Python openpyxl  (preferred — fine-grained control)
#   2. LibreOffice headless  (--convert-to xlsx)
#
# Install:
#   - Python:       pip install openpyxl
#   - LibreOffice:  apt install libreoffice  (or brew install --cask libreoffice)
#
# Exit codes:
#   0 = success
#   1 = bad usage
#   2 = input file missing or not .csv
#   3 = no rendering engine available

set -euo pipefail

CSV="${1:-}"

if [[ -z "$CSV" ]]; then
  echo "Usage: bash scripts/render-acceptance-test-xlsx.sh <path-to.csv>" >&2
  exit 1
fi

if [[ ! -f "$CSV" ]]; then
  echo "ERROR: file not found: $CSV" >&2
  exit 2
fi

if [[ "$CSV" != *.csv ]]; then
  echo "ERROR: input must be .csv (got: $CSV)" >&2
  exit 2
fi

XLSX="${CSV%.csv}.xlsx"
SHEET_NAME="$(basename "${CSV%.csv}")"

# Engine 1 — Python openpyxl
if python3 -c "import openpyxl" 2>/dev/null; then
  echo "[render] using engine: python3 + openpyxl"
  python3 - "$CSV" "$XLSX" "$SHEET_NAME" <<'PY'
import csv
import sys
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.utils import get_column_letter

csv_path, xlsx_path, sheet_name = sys.argv[1], sys.argv[2], sys.argv[3]

# utf-8-sig strips BOM if present
with open(csv_path, encoding="utf-8-sig", newline="") as f:
    rows = list(csv.reader(f))

if not rows:
    print(f"ERROR: empty CSV: {csv_path}", file=sys.stderr)
    sys.exit(2)

wb = Workbook()
ws = wb.active
ws.title = sheet_name[:31]  # Excel sheet name max 31 chars

header_font = Font(bold=True, color="FFFFFF")
header_fill = PatternFill("solid", fgColor="2E5C8A")
header_align = Alignment(horizontal="left", vertical="center", wrap_text=True)

# Write rows
for row_idx, row in enumerate(rows, start=1):
    for col_idx, val in enumerate(row, start=1):
        cell = ws.cell(row=row_idx, column=col_idx, value=val)
        if row_idx == 1:
            cell.font = header_font
            cell.fill = header_fill
            cell.alignment = header_align
        else:
            cell.alignment = Alignment(vertical="top", wrap_text=True)

# Freeze header row
ws.freeze_panes = "A2"

# Auto-fit column widths (cap at 60 to avoid super-wide cells)
for col_idx in range(1, len(rows[0]) + 1):
    col_letter = get_column_letter(col_idx)
    max_len = 0
    for row in rows:
        if col_idx <= len(row):
            cell_len = len(str(row[col_idx - 1]).split("\n")[0])
            if cell_len > max_len:
                max_len = cell_len
    ws.column_dimensions[col_letter].width = min(max(max_len + 2, 12), 60)

# Header row height for wrap
ws.row_dimensions[1].height = 24

wb.save(xlsx_path)
print(f"[render] wrote {xlsx_path} ({len(rows)} rows, {len(rows[0])} cols)")
PY
  exit 0
fi

# Engine 2 — LibreOffice headless
if command -v libreoffice >/dev/null 2>&1 || command -v soffice >/dev/null 2>&1; then
  CMD="$(command -v libreoffice 2>/dev/null || command -v soffice)"
  echo "[render] using engine: $CMD --headless --convert-to xlsx"
  OUT_DIR="$(dirname "$CSV")"
  "$CMD" --headless --convert-to xlsx --outdir "$OUT_DIR" "$CSV" >/dev/null
  if [[ -f "$XLSX" ]]; then
    echo "[render] wrote $XLSX"
    echo "[render] NOTE: LibreOffice does not apply header-bold/frozen-pane. For best UX install openpyxl: pip install openpyxl"
    exit 0
  else
    echo "ERROR: libreoffice failed to produce $XLSX" >&2
    exit 3
  fi
fi

echo "ERROR: no XLSX engine found." >&2
echo "Install one of:" >&2
echo "  - Python openpyxl:  pip install openpyxl  (preferred)" >&2
echo "  - LibreOffice:      apt install libreoffice  (or brew install --cask libreoffice)" >&2
exit 3

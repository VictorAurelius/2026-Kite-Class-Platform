#!/usr/bin/env bash
# Wave thesis-2 Bucket Issue 3 — Thesis docx quality check tool
#
# Usage: bash scripts/check-thesis-docx-quality.sh [path/to/thesis-v*.docx]
#   Default path: documents/08-thesis/thesis-v1.docx
#
# Checks:
#   1. Blank pages (consecutive empty paragraphs ≥3)
#   2. Heading hierarchy violations (skipped levels H1→H3, H2→H4)
#   3. Banned non-khung sections per thesis-content-standard.md v2.0.0 §3
#   4. Missing required sections (Lời cảm ơn, Mục lục, danh mục, Mở đầu, Ch.1-4, Kết luận, TLTK)
#   5. SEQ field count (Hình/Bảng) — should be ≥1 each for Word Table of Figures population
#   6. updateFields flag in settings.xml — must = "true" for auto-render fields
#   7. Page count proxy (paragraph count → estimated pages)
#   8. Cover page logo embed (NOT placeholder text "[LOGO UTC]")
#   9. Bibliography heading + entries ≥30 (cử nhân threshold per rule §2 C3)

set -uo pipefail

DOCX="${1:-documents/08-thesis/thesis-v1.docx}"

if [[ ! -f "$DOCX" ]]; then
    echo "FAIL: docx file not found: $DOCX"
    exit 1
fi

echo "============================================================"
echo "Thesis docx quality check: $DOCX"
echo "Rule reference: .claude/rules/thesis-content-standard.md v2.0.0"
echo "============================================================"

# Extract XML once
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT
unzip -q -p "$DOCX" word/document.xml > "$TMPDIR/document.xml"
unzip -q -p "$DOCX" word/settings.xml > "$TMPDIR/settings.xml" 2>/dev/null || echo "" > "$TMPDIR/settings.xml"

FAIL_COUNT=0
WARN_COUNT=0
PASS_COUNT=0

pass() { echo "  ✓ $*"; PASS_COUNT=$((PASS_COUNT + 1)); }
warn() { echo "  ⚠ $*"; WARN_COUNT=$((WARN_COUNT + 1)); }
fail() { echo "  ✗ $*"; FAIL_COUNT=$((FAIL_COUNT + 1)); }

# ============================================================
echo ""
echo "[1] Blank pages check (consecutive empty paragraphs ≥3)"
# Extract paragraph text content
python3 - "$TMPDIR/document.xml" <<'PYEOF'
import sys
import re
import xml.etree.ElementTree as ET

ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
tree = ET.parse(sys.argv[1])
paragraphs = tree.findall('.//w:body/w:p', ns)

empty_streak = 0
max_streak = 0
streak_count = 0
for p in paragraphs:
    text_content = ''.join(t.text or '' for t in p.findall('.//w:t', ns))
    if text_content.strip() == '':
        empty_streak += 1
        if empty_streak > max_streak:
            max_streak = empty_streak
    else:
        if empty_streak >= 3:
            streak_count += 1
        empty_streak = 0

if streak_count == 0:
    print("  ✓ No suspicious blank-page streaks detected")
else:
    print(f"  ⚠ {streak_count} blank-paragraph streak(s) ≥3 — review docx for orphan page breaks")
print(f"    (max consecutive empty: {max_streak})")
PYEOF

# ============================================================
echo ""
echo "[2] Heading hierarchy + skipped levels"
python3 - "$TMPDIR/document.xml" <<'PYEOF'
import sys
import xml.etree.ElementTree as ET

ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
tree = ET.parse(sys.argv[1])
paragraphs = tree.findall('.//w:body/w:p', ns)

level_counts = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}
skips = []
prev_level = 0

for p in paragraphs:
    pStyle = p.find('.//w:pStyle', ns)
    if pStyle is not None:
        style_val = pStyle.get('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}val', '')
        if style_val.startswith('Heading'):
            try:
                level = int(style_val.replace('Heading', ''))
                level_counts[level] = level_counts.get(level, 0) + 1
                if prev_level > 0 and level > prev_level + 1:
                    text = ''.join(t.text or '' for t in p.findall('.//w:t', ns))
                    skips.append(f"H{prev_level} → H{level}: {text[:60]}")
                prev_level = level
            except ValueError:
                pass

print(f"  Heading counts: H1={level_counts.get(1,0)} H2={level_counts.get(2,0)} H3={level_counts.get(3,0)} H4={level_counts.get(4,0)} H5={level_counts.get(5,0)}")
if skips:
    print(f"  ⚠ {len(skips)} skipped-level violation(s):")
    for s in skips[:5]:
        print(f"      {s}")
else:
    print("  ✓ No skipped heading levels detected")
PYEOF

# ============================================================
echo ""
echo "[3] Banned non-khung sections (rule v2.0.0 §3)"
for banned in "LỜI CAM ĐOAN" "TÓM TẮT" "NHẬN XÉT CỦA GIẢNG VIÊN" "Phụ lục A" "Phụ lục C" "Đóng góp khoa học"; do
    count=$(grep -oF "$banned" "$TMPDIR/document.xml" 2>/dev/null | wc -l)
    if [[ "$count" -eq 0 ]]; then
        pass "Zero '$banned' (banned per rule §3)"
    else
        fail "$count instance(s) of '$banned' — should be 0"
    fi
done
# ABSTRACT page (separate EN page riêng) — check as standalone heading, not citation
abstract_heading=$(grep -oE "<w:t[^>]*>ABSTRACT</w:t>" "$TMPDIR/document.xml" 2>/dev/null | wc -l)
if [[ "$abstract_heading" -eq 0 ]]; then
    pass "Zero 'ABSTRACT' page heading (banned per rule §3)"
else
    fail "$abstract_heading 'ABSTRACT' heading(s) — should be 0"
fi

# ============================================================
echo ""
echo "[4] Required sections per khung primary §1"
for req in "LỜI CẢM ƠN" "MỤC LỤC" "MỞ ĐẦU" "KẾT LUẬN" "TÀI LIỆU THAM KHẢO" "TỔNG QUAN VỀ BÀI TOÁN" "PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG" "PHÂN TÍCH, THIẾT KẾ VÀ TRIỂN KHAI HỆ THỐNG" "ĐÁNH GIÁ KẾT QUẢ VÀ KẾT LUẬN"; do
    if grep -qF "$req" "$TMPDIR/document.xml"; then
        pass "Required section present: '$req'"
    else
        fail "MISSING required section: '$req'"
    fi
done

# Ch.1 sub-sections (Round 2 Item 2: §1.3 Công nghệ bỏ — Ch.1 chỉ §1.1 + §1.2)
for sub in "1.1 Hiện trạng" "1.2 Bài toán"; do
    if grep -qF "$sub" "$TMPDIR/document.xml"; then
        pass "Ch.1 sub-section present: '$sub'"
    else
        fail "MISSING Ch.1 sub-section: '$sub'"
    fi
done

# ============================================================
echo ""
echo "[5] SEQ fields (Word Table of Figures population)"
fig_seq=$(grep -o "SEQ Figure" "$TMPDIR/document.xml" 2>/dev/null | wc -l)
tbl_seq=$(grep -o "SEQ Table" "$TMPDIR/document.xml" 2>/dev/null | wc -l)
if [[ "$fig_seq" -ge 1 ]]; then
    pass "Figure SEQ count: $fig_seq (Hình captions)"
else
    fail "Figure SEQ count: 0 — 'Danh mục Hình' sẽ EMPTY khi mở docx"
fi
if [[ "$tbl_seq" -ge 1 ]]; then
    pass "Table SEQ count: $tbl_seq (Bảng captions)"
else
    fail "Table SEQ count: 0 — 'Danh mục Bảng' sẽ EMPTY khi mở docx"
fi

# ============================================================
echo ""
echo "[6] updateFields flag (auto-render fields on open)"
if grep -qF 'updateFields w:val="true"' "$TMPDIR/settings.xml"; then
    pass "updateFields=true — Word/LibreOffice tự update TOC khi mở"
else
    fail "updateFields=true MISSING — user phải manual Ctrl+A + F9"
fi

# ============================================================
echo ""
echo "[7] Page count proxy (paragraph estimate)"
para_count=$(grep -o "<w:p[ >]" "$TMPDIR/document.xml" 2>/dev/null | wc -l)
est_min=$((para_count / 8))
est_max=$((para_count / 6))
echo "  Paragraph count: $para_count → estimated $est_min-$est_max pages (6-8 par/page)"
if [[ "$est_max" -le 80 ]]; then
    pass "Page count target: cử nhân ≤80 (per rule §4)"
elif [[ "$est_max" -le 90 ]]; then
    warn "Page count 81-90 — soft deduct per rule §4 (cử nhân threshold 80)"
else
    fail "Page count >90 — auto-FAIL category C2 per rule §4 — TRIM required"
fi

# ============================================================
echo ""
echo "[8] Cover page logo embed (not placeholder)"
if grep -qF "[LOGO UTC]" "$TMPDIR/document.xml"; then
    fail "Cover has '[LOGO UTC]' placeholder text — actual PNG NOT embedded"
else
    pass "No [LOGO UTC] placeholder — likely actual PNG embedded (verify visual)"
fi

# ============================================================
echo ""
echo "[9.5] Long figure/table captions (Round 2 Item 1b — visual A4 fit)"
python3 - "$TMPDIR/document.xml" <<'PYEOF'
import sys
import re
import xml.etree.ElementTree as ET

ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
tree = ET.parse(sys.argv[1])
paragraphs = tree.findall('.//w:body/w:p', ns)

long_captions = []
for p in paragraphs:
    text = ''.join(t.text or '' for t in p.findall('.//w:t', ns))
    m = re.match(r'^(Hình|Bảng)\s+\d+\.\d+\.?\s*(.+)$', text.strip())
    if m:
        caption_body = m.group(2).strip()
        if len(caption_body) > 120:
            long_captions.append((m.group(1), len(caption_body), caption_body[:80]))

if long_captions:
    print(f"  ⚠ {len(long_captions)} long caption(s) >120 chars:")
    for label, length, preview in long_captions[:5]:
        print(f"      {label} caption ({length} chars): {preview}...")
    if len(long_captions) > 5:
        print(f"      ... and {len(long_captions) - 5} more")
else:
    print("  ✓ No suspicious long captions (>120 chars) detected")
PYEOF

# ============================================================
echo ""
echo "[10] KẾT LUẬN heading discipline (Round 2 Item 7)"
kn_full=$(grep -oE "KẾT LUẬN VÀ KIẾN NGHỊ" "$TMPDIR/document.xml" 2>/dev/null | wc -l)
if [[ "$kn_full" -eq 0 ]]; then
    pass "Heading 'KẾT LUẬN' (not 'KẾT LUẬN VÀ KIẾN NGHỊ') per user direction"
else
    fail "$kn_full 'KẾT LUẬN VÀ KIẾN NGHỊ' heading(s) — should be just 'KẾT LUẬN'"
fi

# ============================================================
echo ""
echo "[9] Bibliography entries (≥30 cử nhân threshold)"
bib_entries=$(grep -oE "\[[0-9]+\]" "$TMPDIR/document.xml" 2>/dev/null | sort -u | wc -l)
if [[ "$bib_entries" -ge 30 ]]; then
    pass "Bibliography unique entries: $bib_entries (≥30)"
elif [[ "$bib_entries" -ge 20 ]]; then
    warn "Bibliography entries: $bib_entries (cử nhân threshold 30 per rule §2 C3)"
else
    fail "Bibliography entries: $bib_entries — below 20 minimum"
fi

# ============================================================
echo ""
echo "============================================================"
echo "Summary: $PASS_COUNT PASS / $WARN_COUNT WARN / $FAIL_COUNT FAIL"
echo "============================================================"

if [[ "$FAIL_COUNT" -gt 0 ]]; then
    exit 1
fi
exit 0

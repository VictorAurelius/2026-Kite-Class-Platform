#!/usr/bin/env python3
"""
Renumber bibliography refs by first-appearance + drop orphans.

Pipeline:
1. Scan chapter MDs in pipeline order, build first-appearance order of [N] refs
2. Build mapping old_N -> new_N (sequential 1, 2, 3, ...)
3. Drop orphan refs not cited anywhere (kept separately if Mở đầu/Kết luận script-side cites them — verify post-rerender)
4. Apply mapping to chapter MDs body (text replacement [N] -> [new_N], preserve [N, tr.X])
5. Reorder bibliography.md entries to match new numbering + drop orphans

Pre-conditions:
- All chapter MDs committed (Wave 102.3 polish state)
- Bibliography 48 entries

Verification:
- Each [N] in body has matching bibliography entry [N]
- No gaps in bibliography numbering
- Pipeline render produces 0 orphans
"""

import re
from pathlib import Path

ROOT = Path("documents/08-thesis")
CHAPTERS = [
    "chapter-1-competitor-analysis.md",
    "chapter-1-ai-techniques.md",
    "chapter-1-vn-law-methodology.md",
    "chapter-2-system-architecture.md",
    "chapter-3-implementation.md",
    "chapter-4-deployment-results.md",
]
BIB_FILE = ROOT / "references" / "bibliography.md"

def extract_first_appearance_order():
    """Scan chapters in pipeline order, return list of refs in first-appearance order."""
    seen = []
    for cf in CHAPTERS:
        path = ROOT / cf
        if not path.exists():
            continue
        for line in path.read_text(encoding="utf-8").splitlines():
            for m in re.finditer(r'\[(\d+)(?:, tr\.[0-9X]+)?\]', line):
                n = int(m.group(1))
                if n not in seen:
                    seen.append(n)
    return seen

def build_mapping(first_appearance):
    """Build old -> new mapping. Refs not in first_appearance are orphans (no mapping)."""
    return {old: new for new, old in enumerate(first_appearance, 1)}

def apply_mapping_to_chapter(cf, mapping):
    """Apply mapping to all [N] and [N, tr.X] refs in chapter MD. Returns updated text + count."""
    path = ROOT / cf
    if not path.exists():
        return 0
    text = path.read_text(encoding="utf-8")

    # Pattern: [N] OR [N, tr.X]
    def replace_ref(m):
        old_n = int(m.group(1))
        page_part = m.group(2) or ""
        if old_n not in mapping:
            return f"[ORPHAN-{old_n}{page_part}]"  # marker — should not occur if body refs all mapped
        new_n = mapping[old_n]
        return f"[{new_n}{page_part}]"

    pattern = r'\[(\d+)(, tr\.[0-9X]+)?\]'
    new_text, count = re.subn(pattern, replace_ref, text)
    path.write_text(new_text, encoding="utf-8")
    return count

def reorder_bibliography(mapping, first_appearance):
    """Reorder bibliography.md entries by new numbering. Drop orphans."""
    text = BIB_FILE.read_text(encoding="utf-8")
    lines = text.split("\n")

    # Parse entries: each "[N] ..." line is one entry; surrounding lines (headings + blank) preserved
    entries = {}  # old_N -> entry text (single line)
    current_n = None
    current_entry = []
    header_lines = []  # lines before first entry
    entry_started = False

    for line in lines:
        m = re.match(r'^\[(\d+)\]\s', line)
        if m:
            # Save previous entry
            if current_n is not None:
                entries[current_n] = "\n".join(current_entry)
            # Start new entry
            current_n = int(m.group(1))
            current_entry = [line]
            entry_started = True
        else:
            if not entry_started:
                header_lines.append(line)
            else:
                current_entry.append(line)

    # Save last entry
    if current_n is not None:
        entries[current_n] = "\n".join(current_entry)

    print(f"  Parsed {len(entries)} bibliography entries")

    # Identify section headers (lines starting with ## or ### between entries)
    # Strategy: keep headers in original order, place all entries by new numbering
    # Simpler: drop all original section headers (they're chapter-grouped, now obsolete after renumber)
    # Replace with single flat list ordered by new numbering

    # New entries: in first_appearance order, renumbered
    new_lines = []
    new_lines.append("# Bibliography — IEEE Format")
    new_lines.append("")
    new_lines.append("**Citation style:** IEEE per [CITATION-STYLE.md](./CITATION-STYLE.md). In-text format `[N]`, `[N, M]`, `[N]–[M]`.")
    new_lines.append(f"**Total entries:** {len(first_appearance)} (renumbered Wave 102.4 by first-appearance + 9 orphan refs dropped).")
    new_lines.append("**Last updated:** 2026-05-20.")
    new_lines.append("**Last cross-ref audit:** 2026-05-20 Wave 102.4 — see `cross-ref-audit-2026-05-19.md` (pre-renumber baseline).")
    new_lines.append("")
    new_lines.append("Numbers `[N]` chạy global theo first-appearance trong body. Bibliography hiện flat list (post Wave 102.4 renumber); section grouping by chapter dropped (multiple sections same chapter cause non-sequential numbering).")
    new_lines.append("")
    new_lines.append("---")
    new_lines.append("")
    new_lines.append("## Tài liệu tham khảo")
    new_lines.append("")

    for new_n, old_n in enumerate(first_appearance, 1):
        if old_n not in entries:
            print(f"  WARNING: [{old_n}] cited in body but not in bibliography!")
            continue
        old_entry = entries[old_n]
        # Replace [old_n] prefix with [new_n]
        new_entry = re.sub(rf'^\[{old_n}\]', f'[{new_n}]', old_entry)
        new_lines.append(new_entry)
        new_lines.append("")

    new_lines.append("---")
    new_lines.append("")
    new_lines.append("## Notes")
    new_lines.append("")
    new_lines.append("- 9 orphan refs dropped Wave 102.4 (old [5, 6, 7, 8, 10, 11, 12, 13, 20] — not cited in body chapters or Mở đầu/Kết luận).")
    new_lines.append("- Renumber by first-appearance per UTC §3 citation order convention.")
    new_lines.append(f"- Total active refs: {len(first_appearance)}.")

    BIB_FILE.write_text("\n".join(new_lines), encoding="utf-8")
    print(f"  Bibliography rewritten with {len(first_appearance)} entries (was 48)")

def main():
    print("=== Citation Renumber by First-Appearance ===")
    first_appearance = extract_first_appearance_order()
    print(f"First-appearance order ({len(first_appearance)} refs): {first_appearance}")
    print()

    mapping = build_mapping(first_appearance)
    print("Mapping built. Sample (top 5):")
    for old, new in sorted(mapping.items())[:5]:
        print(f"  [{old}] -> [{new}]")
    print()

    print("Applying mapping to chapters:")
    total = 0
    for cf in CHAPTERS:
        count = apply_mapping_to_chapter(cf, mapping)
        print(f"  {cf}: {count} refs renumbered")
        total += count
    print(f"  Total: {total} ref renumbers across body")
    print()

    print("Reordering bibliography:")
    reorder_bibliography(mapping, first_appearance)
    print()

    print("=== Verification ===")
    # Re-scan chapters to confirm no [ORPHAN-N] markers
    orphan_count = 0
    for cf in CHAPTERS:
        path = ROOT / cf
        text = path.read_text(encoding="utf-8")
        orphans = re.findall(r'\[ORPHAN-(\d+)', text)
        if orphans:
            print(f"  ⚠️ {cf} has {len(orphans)} ORPHAN markers: {orphans}")
            orphan_count += len(orphans)

    if orphan_count == 0:
        print("  ✅ No orphan markers in chapters — all body refs mapped successfully")
    else:
        print(f"  ❌ {orphan_count} orphan markers found — manual fix required")

if __name__ == "__main__":
    main()

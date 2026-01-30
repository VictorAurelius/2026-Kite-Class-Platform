# Skill: Internship Report Generator

Generate Word document (.docx) for Internship Report following UTC (University of Transport) template.

## Folder Structure
All internship report files are organized in: `documents/word-reports/bao-cao-thuc-tap/`

## Files
| File | Path |
|------|------|
| Main script | `bao-cao-thuc-tap/create_bao_cao_thuc_tap.py` |
| Output (Main) | `bao-cao-thuc-tap/BAO_CAO_THUC_TAP.docx` |
| Output (SORA) | `bao-cao-thuc-tap/BAO_CAO_THUC_TAP_SORA.docx` |
| Context | `bao-cao-thuc-tap/CONTEXT_BAO_CAO_THUC_TAP.md` |
| Logo | `templates/logo_utc.png` |
| Template guide | `templates/Huong dan trinh bay bao cao TTTN.pdf` |
| Student info | `.claude/skills/student-info.md` |

## When to Use
- Generate internship report
- Update report content
- Add tables/figures with auto-numbering
- Add IEEE references

## Document Structure

### Cover Pages
1. Main cover (university, department, logo, student info table)
2. Secondary cover (+ internship company)
3. Company evaluation form
4. Acknowledgment page

### Content (Using Heading Styles)
1. Chapter 1: Company Introduction (Heading 1)
   - 1.1 General info (Heading 2)
   - 1.2 Functions, duties (Heading 2)
   - 1.3 Work environment (Heading 2)

2. Chapter 2: Internship Content (Heading 1)
   - 2.1 Objectives and requirements (Heading 2)
   - 2.2 Schedule (Heading 2)
   - 2.3 Tasks completed (Heading 2)
     - 2.3.1 DB Design (Heading 3)
     - 2.3.2 Screen Design (Heading 3)
   - 2.4 Technologies, tools (Heading 2)

3. Chapter 3: Results and Evaluation (Heading 1)
4. Chapter 4: Reflections and Orientation (Heading 1)
5. References (IEEE format)
6. Appendix

## Auto-numbering Features

### Heading Styles (for TOC)
- **Heading 1**: Chapter title (14pt Bold)
- **Heading 2**: Section (13pt Bold)
- **Heading 3**: Subsection (13pt Bold Italic)

### SEQ Fields (for Table/Figure numbering)
```python
# Add table with auto-numbering
add_table_with_caption(doc,
    chapter_num=2,
    caption_text="Table name",
    headers=["Col1", "Col2"],
    rows=[("A", "B")],
    col_widths=[5.0, 11.0]
)

# Add figure with auto-numbering
add_figure_placeholder(doc,
    chapter_num=2,
    caption_text="Figure name"
)
```

### IEEE References
```python
add_ieee_reference(doc,
    ref_num=1,
    author="Author Name",
    title="Document Title",
    source_type="Online",
    year="2024",
    url="https://example.com",
    accessed="Jan. 2026"
)
```

## Format Specifications (UTC Standards)

### Margins
- Top: 2.5cm, Bottom: 2.5cm (Updated from 2cm)
- Left: 3.0cm, Right: 2.0cm
- Page number: Center, header (starts from TOC, not covers)

### Font (Times New Roman)
- **Chapter (Heading 1)**: 18pt Bold, **CENTER** aligned
- **Section (Heading 2)**: 16pt Bold, left aligned
- **Subsection (Heading 3)**: 14pt Bold, left aligned
- **Normal text**: 13pt, justify, indent 1.0cm, line spacing 1.2

### Cover Pages
- **Main cover**: Has logo, bordered, no page number
- **Secondary cover**: Same layout as main (no logo), bordered, no page number
- **Table cells**: Label + Value (NO colon separator)

### Bullet Lists
- Hanging indent: left_indent=1.5cm, first_line_indent=-0.5cm
- Bullet hangs out 0.5cm, wrapped text aligns at 1.5cm

## Actions

### Generate report
```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-reports/bao-cao-thuc-tap
python3 create_bao_cao_thuc_tap.py
```

### Update in Word
After opening .docx:
1. Press **Ctrl+A** then **F9** to update all fields
2. Insert TOC: References > Table of Contents
3. Insert Table list: References > Insert Table of Figures (Label: Table)
4. Insert Figure list: References > Insert Table of Figures (Label: Figure)

## Dependencies
```bash
pip install python-docx --user
```

## Troubleshooting & Common Fixes

### Issue 1: Borders on All Pages
**Problem**: Page borders appear on all pages instead of just covers.
**Solution**:
1. Apply borders BEFORE margins and page numbers
2. Use `remove_page_border()` to explicitly remove borders from section 2+
3. Order: borders → remove borders → margins → page numbers

### Issue 2: Margins Not Applied
**Problem**: Left margin (3cm) not applied to some sections.
**Solution**: Call `set_document_margins()` AFTER all sections are created, not before.

### Issue 3: Chapter Headings Not Centered
**Problem**: Chapter headings aligned left instead of centered.
**Solution**: Add `p.alignment = WD_ALIGN_PARAGRAPH.CENTER` to Heading 1 paragraphs.

### Issue 4: Bullet List Indentation
**Problem**: Bullets less indented than paragraphs, wrapped text over-indented.
**Solution**: Use hanging indent:
```python
p.paragraph_format.left_indent = Cm(1.5)  # Text position
p.paragraph_format.first_line_indent = Cm(-0.5)  # Bullet hangs out
```

### Issue 5: Page Numbering Starts Too Early
**Problem**: Page numbers appear on cover pages.
**Solution**: Skip sections 0-1 in `add_page_number_header()`, start from section 2 (TOC).

### Issue 6: Table Cells Have Colon
**Problem**: Table cells show `: Value` instead of just `Value`.
**Solution**: Change `row.cells[1].text = f": {value}"` to `row.cells[1].text = value`.

### Issue 7: Secondary Cover Layout Different
**Problem**: Secondary cover doesn't match main cover structure.
**Solution**:
- Add empty paragraph with space_before=Pt(24), space_after=Pt(24) where logo would be
- Match spacing: KHOA space_after=Pt(0), BÁO CÁO space_before=Pt(12)
- Use 3 empty paragraphs before "Hà Nội – 2026"

## Recent Updates (2026-01-30)
- ✅ Updated CBHD to "Trịnh Công Vượng (Project Manager)"
- ✅ Removed colons from cover page table cells
- ✅ Fixed secondary cover to match main cover layout (no logo)
- ✅ Fixed page numbering to start from TOC (section 2+)
- ✅ Applied UTC formatting standards (1.2 spacing, 1.0cm indent)
- ✅ Fixed chapter headings to 18pt center aligned
- ✅ Fixed bullet list hanging indent
- ✅ Fixed margins (2.5cm top/bottom)

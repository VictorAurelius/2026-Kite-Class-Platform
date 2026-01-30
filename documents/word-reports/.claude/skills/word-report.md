# Skill: Internship Report Generator

Generate Word document (.docx) for Internship Report following UTC (University of Transport) template.

## Files
| File | Path |
|------|------|
| Main script | `create_bao_cao_thuc_tap.py` |
| Logo | `logo_utc.png` |
| Output | `BAO_CAO_THUC_TAP.docx` |
| Context | `CONTEXT_BAO_CAO_THUC_TAP.md` |
| Template guide | `Huong dan trinh bay bao cao TTTN.pdf` |
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

## Format Specifications

### Margins
- Top: 2cm, Bottom: 2cm
- Left: 3cm, Right: 2cm
- Page number: Center, header

### Font (Times New Roman)
- Heading 1: 14pt Bold, left
- Heading 2: 13pt Bold, left
- Heading 3: 13pt Bold Italic, left
- Normal: 13pt, justify, indent 1.27cm, spacing 1.5

## Actions

### Generate report
```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-report
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

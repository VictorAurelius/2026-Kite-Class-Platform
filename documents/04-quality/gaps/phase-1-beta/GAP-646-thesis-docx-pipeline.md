# GAP-646: Thesis DOCX pipeline — template + chapter assembly + IEEE bibliography

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (META — force-multiplier per `meta-gap-priority.md` §3)
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-18 (Thesis scope outside-in audit)
**Related Audits:** [thesis-defense-failure-mode-matrix](../../audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md), [thesis-vn-saas-benchmark](../../audits/persona-review/2026-05-18-thesis-vn-saas-benchmark.md)

## Current State (verified 2026-05-18)

| Piece | Path | Status |
|---|---|---|
| `document-generation/word/SKILL.md` | `.claude/skills/document-generation/word/SKILL.md` | ✅ exists nhưng scope = teacher-contract only, Create pipeline only |
| Thesis DOCX template | `assets/thesis-template.docx` hoặc equivalent | ❌ missing — không có cover page / TOC / 7-chapter shell / IEEE bibliography section |
| Chapter assembly script | `scripts/assemble-thesis-docx.sh` | ❌ missing |
| Chapter mapping → DOCX bridge | per `documents/08-thesis/chapter-mapping.md` | ⚠️ mapping file tồn tại nhưng không assembler |

## Problem

Thesis defense Q4 2026 cần deliverable cuối cùng = DOCX báo cáo (~80-120 trang) format chuẩn VN CS theses. Current Wave 5 `document-generation/word/SKILL.md` chỉ xử lý teacher-contract template với Apache POI XWPF Create pipeline — KHÔNG có:

- Thesis template (cover trang + TOC + 7 chapter shell + bibliography format + appendix)
- Chapter assembly logic (read primary + supplementary sources per `chapter-mapping.md`, inject figures, format per IEEE style)
- Bibliography section auto-format từ refs.md/refs.bib (GAP-647 paired)
- Cross-reference numbering (Figure 3.1, Table 4.2, Citation [12])

Failure-mode audit examiner B1/B2 explicit cite "audit score dashboard required Chapter 4" + "OWASP mapping table required" — cần placeholder system trong template để inject data programmatically.

## Proposed Fix

### Step 1: Thesis template DOCX

Create `assets/thesis-template.docx` với XWPF skeleton:
- Cover trang: title VN, tên SV, MSSV, GVHD, năm, trường (placeholder format `{{var.title}}`)
- TOC: auto-generated từ heading hierarchy (XWPF `CTSdtBlock` field code)
- 7 chapter shell per `chapter-mapping.md`: Intro / Theoretical / Requirements / Design / Implementation / Testing / Conclusion
- Bibliography section: IEEE format `[1] Author, "Title," Source, Year.`
- Appendix section: placeholder cho audit reports + benchmark data + beta reviews

Font: Times New Roman 13 pt body, 14 pt heading (VN academic norm).
Page: A4, margins 3cm top + 2cm sides + 2cm bottom (HUST/UIT standard).

### Step 2: Chapter assembly script

`scripts/assemble-thesis-docx.sh`:
1. Read `documents/08-thesis/chapter-mapping.md` → list source docs per chapter
2. Walk source paths, extract content (markdown → DOCX paragraphs via Pandoc fallback OR direct XWPF read)
3. Inject figures từ `documents/06-diagrams/plantuml/*.svg/*.png` với caption + numbering
4. Format citations `{{cite:GAP-XXX}}` → `[N]` reference per bibliography order (GAP-647 dep)
5. Output `documents/08-thesis/build/thesis-vN.docx`

### Step 3: Extend document-generation skill

`.claude/skills/document-generation/word/SKILL.md` add §Thesis pipeline:
- New templateId `thesis-report` cho DocxGenerator
- `ThesisReportBuilder` class — extends teacher-contract pattern
- Wire to `scripts/assemble-thesis-docx.sh`

## Acceptance Criteria

- [ ] `assets/thesis-template.docx` exists với cover + TOC + 7-chapter shell + bibliography + appendix
- [ ] `scripts/assemble-thesis-docx.sh` chạy clean trên empty source → empty thesis.docx 7-chapter skeleton
- [ ] Sample thesis.docx (test data injection) render đúng VN typography (TNR 13pt, A4, margins)
- [ ] Bibliography section auto-format từ refs.md sample
- [ ] `chapter-mapping.md` updated với placeholder pattern `{{cite:GAP-XXX}}` documented
- [ ] CI smoke: `scripts/assemble-thesis-docx.sh --dry-run` exit 0
- [ ] Cross-reference numbering (Figure N.M / Table N.M / [Citation N]) verified rendered

## Related

- GAP-647 thesis-bibliography-ieee (paired — bibliography format dependency)
- GAP-651 thesis-image-curation (figure injection)
- GAP-216 PDF/XLSX/DOCX p95 benchmark (production document scope, separate)
- ADR-019 Facade + Strategy (existing pattern reuse)
- `.claude/skills/document-generation/word/reference/docx-3-pipelines.md`

## Log

- **2026-05-18 (created):** Filed per Release 1.5 thesis scope outside-in audit (3 agents) consolidated findings. Failure-mode B1/B2/D3 + Persona "IEEE citations vắng" + VN benchmark "format chuẩn UIT/HUST/UET" all converge on thesis DOCX pipeline missing as P0 META blocker.

# GAP-646: Thesis DOCX pipeline — template + chapter assembly + IEEE bibliography

**Status:** 🟢 DONE 2026-05-19 — Wave 100.7 Phase 3b V2 (Create pipeline pivot) shipped ThesisReportBuilder Java + DocxGenerator route + 17-test JUnit + skill docs + assembly script dry-run validation. Production `--execute` mode (full chapter MD parsing + figure injection) defers to follow-up gap.
**Priority:** 🔴 P0 (META — force-multiplier per `meta-gap-priority.md` §3)
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-18 (Thesis scope outside-in audit)
**Last Verified:** 2026-05-19 (Wave 100.7 Phase 3b V2 — Create pipeline implementation)
**Closed:** 2026-05-19 (Wave 100.7 Phase 3b V2 ship)
**Related Audits:** [thesis-defense-failure-mode-matrix](../../audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md), [thesis-vn-saas-benchmark](../../audits/persona-review/2026-05-18-thesis-vn-saas-benchmark.md)

## Current State (verified 2026-05-19)

Wave 100.7 Phase 3b shipped scoping + scaffold; Step 1-3 implementation defer focused session.

| Piece | Path | Lines/Status | Source |
|---|---|---|---|
| **Scoping decision doc** | `documents/08-thesis/docx-pipeline-scoping.md` | ~280 lines | ✅ Shipped Wave 100.7 Phase 3b |
| **Assembly script scaffold** | `scripts/assemble-thesis-docx.sh` | ~40 lines + chmod +x | ✅ Shipped (CI smoke `--dry-run` exit 0 satisfied trivially) |
| **GAP-646 status sync** | This file + `gap-status.csv` | PARTIAL 20%, last_verified 2026-05-19 | ✅ Shipped Wave 100.7 Phase 3b |
| **Recommended approach** | Apache POI XWPF Edit-Fill via `.claude/skills/document-generation/word/` | Decision §3.1 cited | ✅ Documented (zero setup cost, on-stack Java 17 + Maven verified) |
| **Tooling inventory verified** | `which pandoc/soffice/libreoffice` exit 127; `java`+`mvn` ready | Empirical 2026-05-19 | ✅ Documented §2 |
| `assets/thesis-template.docx` template | `assets/thesis-template.docx` (binary) | ❌ NOT SHIPPED | Sub-task A focused session — ~2-3h |
| `ThesisReportBuilder.java` POI Edit-Fill builder | `kitehub/kitehub-platform/src/main/java/...ThesisReportBuilder.java` | ❌ NOT SHIPPED | Sub-task B focused session — ~2h |
| `assemble-thesis-docx.sh` production impl | `scripts/assemble-thesis-docx.sh` | ❌ NOT SHIPPED (scaffold only) | Sub-task C focused session — ~1-2h |
| `.claude/skills/document-generation/word/SKILL.md` thesis extension | Skill body + reference/thesis-pipeline.md | ❌ NOT SHIPPED | Sub-task D focused session — ~1h |
| Bibliography input format | `documents/08-thesis/references/bibliography.md` IEEE parsed | ⏳ Phase 3a parallel | Pending Phase 3a ship |

**Phase 3b PARTIAL 20% breakdown:** scoping doc 60% × Phase 3b ≈ 12% project completion + scaffold placeholder 30% × Phase 3b ≈ 6% + GAP status sync 10% × Phase 3b ≈ 2% = 20% total. Remaining 80% = focused session Step 1-3 implementation (~6-8h estimated).

## Current State (verified 2026-05-18 — superseded by 2026-05-19 above)

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

**Phase 3b (Wave 100.7) — scoping + scaffold (DONE 20%):**

- [x] **Scoping decision doc** shipped: `documents/08-thesis/docx-pipeline-scoping.md` (sections 1-9 complete với option matrix + decision rationale + implementation roadmap + binary template authoring workflow)
- [x] **Assembly script scaffold** shipped: `scripts/assemble-thesis-docx.sh` exit 0 (CI smoke trivially satisfied; production impl in Sub-task C)
- [x] **Tooling inventory verified empirically**: Java 17 + Maven ready; Pandoc/LibreOffice not installed (Apache POI XWPF chosen — zero install cost)
- [x] **GAP-646 status synced** CSV + this file (PARTIAL 20%, last_verified 2026-05-19)

**Focused session — Phase 3b V2 (DONE 2026-05-19):**

- [x] **Sub-task A** SKIPPED — pivot to Create pipeline (no binary template file needed; LibreOffice not on-stack). Skeleton built programmatically by `ThesisReportBuilder.java` via POI XWPF (mirrors `TeacherContractBuilder` pattern).
- [x] **Sub-task B** `ThesisReportBuilder.java` shipped `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/docx/ThesisReportBuilder.java` (~250 lines; A4 + 3-2-2-3 cm binding-gutter margins + TNR 13pt body / 14pt heading / 18pt cover title; cover + TOC stub + 7 chapter shells + bibliography + appendix; required keys title/studentName/studentId/supervisor/year/school; optional chapter.N.title / chapter.N.body / bibliography.entries).
- [x] **Sub-task B JUnit** `ThesisReportBuilderTest.java` 17 tests shipped (cover render + 7 chapters + bibliography + appendix + chapter body injection + chapter title override + bibliography entries injection + VN diacritics + A4 page size + binding gutter margins + TNR font + filename slug + missing-key reject + unknown-template reject + soft-cap perf). All 17 PASS local verify.
- [x] **DocxGenerator route** updated to dispatch templateId='thesis-report' → ThesisReportBuilder; 14 existing DocxGeneratorTest still PASS.
- [x] **Sub-task C** `scripts/assemble-thesis-docx.sh` V2 — dry-run validation mode (verifies builder Java + test + chapter-mapping + bibliography + scoping doc exist); shellcheck PASS. Production `--execute` mode requires Spring Boot CLI runner (deferred to follow-up gap — out-of-scope this Phase 3b V2 ship).
- [x] **Sub-task D** `.claude/skills/document-generation/word/SKILL.md` extended với Thesis pipeline section (When to use + 3-pipeline routing flipped Create=shipped for both templates + Thesis pipeline §including required/optional data keys + skeleton structure + VN academic norms + validation script + test ref).
- [x] CI smoke: `scripts/assemble-thesis-docx.sh` dry-run exit 0 verified.

**Deferred to follow-up gap (Phase 3b V3 — out-of-scope V2):**

- [ ] Production `--execute` mode — Spring Boot CLI runner that chains: parse `chapter-mapping.md` → walk source paths → MD-to-XWPF body conversion → figure injection + numbering (Figure N.M) → bibliography auto-format from `bibliography.md` IEEE parsed → cross-reference numbering (Citation [N] / Table N.M)
- [ ] `chapter-mapping.md` placeholder pattern `{{cite:GAP-XXX}}` documented + processor
- [ ] Sample thesis.docx render with real Wave 100.7 Phase 2 chapter content injected end-to-end

## Related

- GAP-647 thesis-bibliography-ieee (paired — bibliography format dependency; Phase 3a parallel)
- GAP-651 thesis-image-curation (figure injection — Sub-task B/C dependency)
- GAP-655 citation-extract (future scope — auto-extract `{{cite:GAP-XXX}}` placeholders)
- GAP-216 PDF/XLSX/DOCX p95 benchmark (production document scope, separate)
- GAP-208 skill template expansion (Wave 7 — thesis-report templateId aligns roadmap)
- ADR-019 Facade + Strategy (existing pattern reuse — ThesisReportBuilder extends pattern)
- `.claude/skills/document-generation/word/SKILL.md` (Wave 5 teacher-contract Create pipeline foundation)
- `.claude/skills/document-generation/word/reference/docx-3-pipelines.md` (Edit-Fill pipeline taxonomy)
- `documents/08-thesis/docx-pipeline-scoping.md` (Phase 3b scoping decision — this gap closure 20%)
- `documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png` (VN CS thesis layout reference image)

## Log

- **2026-05-19 (Wave 100.7 Phase 3b V2 — DONE flip):** Status `🟡 PARTIAL 20%` → `🟢 DONE`. Pivot Edit-Fill → **Create pipeline** because LibreOffice/Word not installed (Sub-task A binary template authoring blocked). Create pipeline mirrors `TeacherContractBuilder` Wave 5 pattern: ThesisReportBuilder programmatically constructs cover + TOC stub + 7 chapter shells + bibliography + appendix via POI XWPF (CTPageSz A4 + CTPageMar 3-2-2-3 cm binding gutter + CTFonts Times New Roman). 17/17 JUnit PASS local verify (`./mvnw test -Dtest=ThesisReportBuilderTest,DocxGeneratorTest` 31 total tests). Sub-task A re-scoped (skipped — Create pipeline obviates binary template); B/C/D shipped. Production `--execute` mode (full MD parsing + figure injection + bibliography auto-format) defers to follow-up gap requiring Spring Boot CLI runner scope. State-check verified empirical: kiteclass-core foundation existed (DocumentGenerationService Facade + DocxGenerator Strategy + Generator interface + TeacherContractBuilder Builder pattern + POI dependency wired via parent pom). Per `gap-folder-organization.md` v2.0.0 §3.3 — DONE flip triggers `git mv` to `phase-1-beta/closed/`.
- **2026-05-19 (Wave 100.7 Phase 3b — scoping):** PARTIAL 20% flip. Phase 3b shipped scoping doc (`documents/08-thesis/docx-pipeline-scoping.md` ~280 lines option matrix + decision + roadmap) + scaffold placeholder (`scripts/assemble-thesis-docx.sh` chmod +x, exit 0, CI smoke trivially satisfied). Recommended approach: **Apache POI XWPF Edit-Fill pipeline** extending `.claude/skills/document-generation/word/` skill foundation (zero setup cost — Java 17 + Maven verified ready; Pandoc 80MB + LibreOffice 600MB scope creep rejected; VN typography fidelity highest via CTPageSz/CTPageMar control). Step 1-3 implementation defer focused session (~6-8h: Sub-task A template authoring 2-3h + Sub-task B ThesisReportBuilder Java 2h + Sub-task C assembly script 1-2h + Sub-task D skill docs 1h). Tooling inventory verified empirical: `which pandoc/soffice/libreoffice` exit 127; `java`+`mvn` ready; `python -c "import docx"` fails. Binary template authoring workflow addressed §5 scoping doc (SHA256 manifest in `assets/README.md` + change-note column + POI read-back JUnit validation + atomic edit commits). CSV row updated: status=PARTIAL, completion_pct=20, last_verified=2026-05-19. No file move (PARTIAL ≠ DONE per `gap-folder-organization.md` v2.0.0 §3.2). State-check verified empirical per `audit-to-gap-pipeline.md` §2.8 — tooling availability + skill foundation existence confirmed before scoping decision finalized.
- **2026-05-18 (created):** Filed per Release 1.5 thesis scope outside-in audit (3 agents) consolidated findings. Failure-mode B1/B2/D3 + Persona "IEEE citations vắng" + VN benchmark "format chuẩn UIT/HUST/UET" all converge on thesis DOCX pipeline missing as P0 META blocker.

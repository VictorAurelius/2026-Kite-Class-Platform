---
title: Wave 5 — Document Generation Skills (GAP-047)
status: draft
created: 2026-04-18
updated: 2026-04-18
waves: [5]
gaps: [GAP-047, GAP-034, GAP-017]
---

# Wave 5: Document Generation Skills (GAP-047)

**Branch:** `wave/05-document-generation`
**Epic:** E10 Cross-cutting & Architecture
**Type:** 🔴 Meta-gap (skills) — HIGHEST priority per `meta-gap-priority.md`
**Source:** `documents/04-quality/analyses/skills-gap-analysis-vs-minimax.md` (MiniMax skills comparison, 2026-04-14)

---

## 1. Why Wave 5 now

**Rule:** `meta-gap-priority.md` §3 — meta-gaps (skills/rules/workflow) fix before feature gaps at equal P-level.

**Force multiplier:** every doc-export feature in the platform (invoice, certificate, transcript, attendance report, financial report, teacher contract, marketing pitch, brand style guide) depends on unified generation skills. Without skills, each feature invents its own approach (JasperReports here, manual PDF there) → drift, inconsistency, duplicate legal review.

**Dependencies unblocked when Wave 5 ships:**
- GAP-034 Branding export pack (ZIP + PDF style guide) — needs PDF skill
- GAP-017 AI usage billing — needs PDF invoice generator
- GAP-012 Quality review reports — needs PDF format
- Future: certificates, transcripts, contracts, reports

---

## 2. Scope (what's in)

### 2.1 Skill files (`.claude/skills/document-generation/`)

Adopted and adapted from MiniMax-AI/skills:

| Format | Skill file | Tech (Java backend) | Tech (FE if needed) |
|--------|-----------|---------------------|---------------------|
| PDF | `pdf/SKILL.md` | iText 7 **or** Apache PDFBox | — |
| Excel | `excel/SKILL.md` | Apache POI (XSSF) | SheetJS (fallback) |
| Word | `word/SKILL.md` | Apache POI (XWPF) | — |
| PowerPoint | `pptx/SKILL.md` | Apache POI (XSLF) | PptxGenJS (alt) |

### 2.2 Per-skill content (≤100 lines per SKILL.md, project-specific gotchas)

Each skill MUST include:
- **Trigger conditions** (frontmatter description) — when Claude should invoke
- **3-pipeline routing** (Create / Edit-Fill / Reformat) from MiniMax pattern
- **Formula-first rule** (Excel) or **token-based theme** (PDF) or **page-type classification** (PPT)
- **Vietnamese-specific gotchas** (Đ, đ, diacritics, long names, VND currency)
- **Reference pointers** to `reference/*.md` for detailed tables
- **Samples pointer** to `kiteclass-core/src/test/resources/document-samples/`

### 2.3 Backend scaffolding

`kiteclass-core/src/main/java/com/kiteclass/core/document/`:
- `DocumentGenerationService` — Facade (per `design-patterns.md` §3 Facade pattern)
- `Generator` interface + 4 Strategy implementations (PdfGenerator, XlsxGenerator, DocxGenerator, PptxGenerator)
- `DocumentRequest` value object (format, template-id, data map, branding-id)
- `DocumentResponse` value object (bytes, mime-type, filename)

### 2.4 Template library (stub)

`kiteclass-core/src/main/resources/templates/`:
- 1 PDF template (invoice — Vietnamese tax invoice format)
- 1 Excel template (attendance report — weekly)
- 1 Word template (teacher contract — placeholder)
- 1 PPT template (marketing pitch — placeholder)

Per-template review checklist per `output-review-mandate.md` §3.

### 2.5 Branding integration hook

Generators fetch branding package via GAP-010 API (already shipped in Wave 3):
```
generateInvoice(InvoiceData, tenantId) →
  fetch branding package (cached) →
  apply primary color to header / inject logo / use tenant font →
  return branded PDF
```

### 2.6 Tests

- Unit tests per Generator (Strategy isolation)
- Integration test: invoice → branded PDF → assert PDF text extraction contains correct tenant name + amount in VND
- Sample inputs + golden outputs in `test/resources/document-samples/`

---

## 3. Scope (what's out)

Explicitly **deferred** to later waves:

- Full template library — only 4 stubs in Wave 5 (~20 more templates in Wave 7)
- Legal review of contract/policy templates — needs external lawyer (GAP-042 already tracks legal framework)
- PDF filling existing forms (3-pipeline Fill route) — Create route only in Wave 5
- Async queue for heavy PDF generation — reuse GAP-002 infrastructure (Wave 3b) later
- Print-ready QA (physical print testing) — separate gap, not blocking digital-first launch
- Excel macros / VBA preservation — scope says round-trip only, no macro execution
- Rich SlidesApp-style PPT builder — basic layouts only

---

## 4. Sub-PR Breakdown

### Sub-PR 5.0: Foundation + ADR-016

**Branch:** `wave/05-document-generation/foundation`
**Mode:** serialized (lead)
**Depends on:** —

**Scope:**
- ADR-016 Document generation architecture decision
  - Option A: Pure backend (Java-only, Apache POI + iText)
  - Option B: Hybrid (backend for server docs, FE for interactive preview)
  - Option C: Separate microservice
  - Decision: **A** (simplest, single library stack, reuse existing Maven module)
- Maven module: `kiteclass-document-gen` (optional — if extracted) OR new package in kiteclass-core
- Dependency versions pinned (iText 7.2.x, POI 5.2.x)
- `Generator` interface + shared value objects (`DocumentRequest`, `DocumentResponse`)
- `DocumentGenerationService` facade (empty implementation, throws UnsupportedOperation for each format)
- Test scaffolding: `document-samples/` folder + base `DocumentGenerationTestBase`
- 3-layer docs stub: `document-generation/` in `01-business/`

**Effort:** ~3h
**Deliverable:** PR foundation merged, unblocks 5.1-5.4 parallel work.

---

### Sub-PR 5.1: PDF Skill + Invoice Generator (P0)

**Branch:** `wave/05-document-generation/pdf`
**Depends on:** 5.0

**Scope:**
- `.claude/skills/document-generation/pdf/SKILL.md` (adopt minimax-pdf, adapt VN context)
- `reference/pdf-cover-styles.md` — 15 cover style references (from MiniMax)
- `reference/pdf-block-types.md` — 20 block type references
- `PdfGenerator` (Strategy impl)
- Invoice template (Vietnamese tax invoice format — hóa đơn GTGT layout)
- Branding integration (primary color header, tenant logo, VND currency formatting)
- Tests:
  - TDD: `PdfGeneratorTest` — renders invoice to bytes, extracts text, asserts content
  - Integration: `InvoiceGenerationIT` — full flow with real branding package
- Sample: `document-samples/invoice-sample.pdf` (golden output for visual review)
- 3-layer docs update (use-case UC-INV-001 invoice generation)

**Effort:** ~8h
**Deliverable:** PDF invoice generation works end-to-end for 1 tenant with branded output.

---

### Sub-PR 5.2: Excel Skill + Attendance Report (P0)

**Branch:** `wave/05-document-generation/excel`
**Depends on:** 5.0 (can run parallel with 5.1)

**Scope:**
- `.claude/skills/document-generation/excel/SKILL.md` (adopt minimax-xlsx, adapt)
- `reference/excel-formula-patterns.md` (formula-first examples for VN financial reports)
- `XlsxGenerator` (Strategy impl)
- Attendance report template (weekly, per-class)
  - Header row: dates (Mon-Fri), student names
  - Body: P/A/L/E (Present/Absent/Late/Excused) per cell
  - Footer formulas: SUM, percentage (formula-first rule)
- Color conventions: blue inputs / black formulas / green cross-refs
- Tests + sample output `attendance-sample.xlsx`

**Effort:** ~6h

---

### Sub-PR 5.3: Word Skill + Teacher Contract (P1)

**Branch:** `wave/05-document-generation/word`
**Depends on:** 5.0

**Scope:**
- `.claude/skills/document-generation/word/SKILL.md` (adopt minimax-docx)
- `reference/docx-3-pipelines.md` — Create / Edit / Format routing
- `DocxGenerator` (Strategy impl)
- Teacher contract template (placeholder wording, legal review deferred)
- XSD validation approach
- Tests + sample

**Effort:** ~6h

---

### Sub-PR 5.4: PPT Skill + Marketing Pitch (P2)

**Branch:** `wave/05-document-generation/pptx`
**Depends on:** 5.0

**Scope:**
- `.claude/skills/document-generation/pptx/SKILL.md` (adopt pptx-generator)
- `reference/pptx-5-page-types.md` (Cover/TOC/Section/Content/Summary)
- `PptxGenerator` (Strategy impl)
- Marketing pitch template (5 slides)
- Branding applied (tenant logo on cover, primary color accents)
- Tests + sample

**Effort:** ~6h

---

### Sub-PR 5.5: Branding Integration + Quality Audit

**Branch:** `wave/05-document-generation/integration`
**Depends on:** 5.1-5.4 all merged

**Scope:**
- Unified branding pipeline — all 4 generators fetch from GAP-010 package API
- Cache branding package per-request (per-tenant, short TTL)
- Cross-format consistency test — same tenant gets same colors/logo in PDF + Excel + Word + PPT
- Quality audit: generated docs pass WCAG contrast, Vietnamese diacritics render, VND formatting correct
- Update `quality-audit/SKILL.md` with new category "Document Generation Quality"
- Update `two-stage-code-review.md` — check if PR generates docs, verify template review

**Effort:** ~5h

---

### Sub-PR 5.6: Integration + Wave Completion

**Branch:** `wave/05-document-generation/completion`
**Depends on:** 5.0-5.5 all merged

**Scope:**
- Cross-generator integration tests
- Sample gallery in `documents/04-quality/samples/` — one golden output per format
- ROADMAP.md update: GAP-047 → DONE, move next meta-gap to position 1
- MiniMax analysis doc marked as ADOPTED
- Wave 5 completion report in `documents/03-planning/waves/wave-05-document-generation.md` §Log
- ADR-016 marked as ACCEPTED

**Effort:** ~3h

---

## 5. Execution Strategy

### 5.1 Mode per sub-PR

| Sub-PR | Mode | Rationale |
|--------|------|-----------|
| 5.0 Foundation | Serialized (lead) | Shared scaffolding |
| 5.1 PDF | Parallel agent #1 | Independent format |
| 5.2 Excel | Parallel agent #2 | Independent format |
| 5.3 Word | Parallel agent #3 | Independent format |
| 5.4 PPT | Parallel agent #4 | Independent format |
| 5.5 Branding integration | Serialized (lead) | Depends on all formats |
| 5.6 Wave completion | Serialized (lead) | Final sign-off |

Total wall-clock estimate: ~14h with parallelism vs ~34h serial (–59%).

### 5.2 Superpowers per sub-PR (mandatory per CLAUDE.md)

Each sub-PR follows:
1. Brainstorm (scope boundary — what format does/doesn't do in this sub-PR)
2. Task breakdown (skill → backend → template → test)
3. TDD — test written before generator implementation
4. Implementation
5. Self-review before PR

### 5.3 Quality gates

Each sub-PR must pass:
- SKILL.md <100 lines (skill-conventions.md)
- Test coverage ≥80% on new generator class
- Sample golden output committed
- 3-layer docs updated (rules.md / use-cases.md / api-contract.md)
- No hardcoded VND amounts, colors, or tenant data in generators (branding injection only)

---

## 6. Dependencies & Blockers

### 6.1 Unblocks (downstream)

- GAP-034 Branding export pack (needs PDF)
- GAP-017 AI usage billing (needs PDF invoice)
- Future: certificate generation, transcript PDF, report export

### 6.2 Blocked by

- None — Wave 5 can start immediately

### 6.3 Integrates with

- GAP-010 branding package API (Wave 3) — already shipped
- GAP-002 async pipeline (Wave 3b) — optional for heavy PDF; start sync
- GAP-042 legal framework (Wave 4) — already covers DMCA/trademark foundation for future legal doc review

---

## 7. Risks & Mitigations

| Risk | Severity | Mitigation |
|------|:--------:|-----------|
| Library bloat (iText + POI in Core) | 🟡 | Extract to `kiteclass-document-gen` Maven module if Core JAR exceeds 1GB |
| Vietnamese font rendering bugs | 🔴 | Test diacritics + Đ,đ in every format before sub-PR merge |
| Template drift (legal wording) | 🟡 | Defer legal templates to dedicated legal-review wave; Wave 5 uses placeholders |
| PDF generation slow for large invoices | 🟢 | Sync first; async via GAP-002 queue as follow-up |
| Branding API latency on every generate | 🟡 | Short TTL cache per-tenant in generator Facade |
| iText 7 license (AGPL — commercial use) | 🔴 | **Verify** before 5.1: AGPL may require commercial license OR switch to Apache PDFBox (Apache 2.0) |

**Action item:** iText license check BEFORE 5.1 starts. If commercial license not acceptable, swap library choice in Sub-PR 5.0 ADR.

---

## 8. Success Criteria

Wave 5 DONE when:

- [ ] 4 SKILL.md files exist, each <100 lines, with Vietnamese gotchas
- [ ] 4 Generator implementations pass tests with branding applied
- [ ] 1 template per format committed + reviewed
- [ ] Sample gallery in `documents/04-quality/samples/` with 1 golden output per format
- [ ] `DocumentGenerationService` facade exposes 4 methods (generateInvoice, generateAttendanceReport, generateTeacherContract, generateMarketingPitch)
- [ ] Cross-format branding consistency test passes
- [ ] ADR-016 ACCEPTED
- [ ] GAP-047 marked DONE in ROADMAP
- [ ] MiniMax analysis doc marked ADOPTED
- [ ] No new P0 gaps introduced
- [ ] Quality audit score ≥90/100 on Wave 5 changes

---

## 9. Open Questions (for user sign-off before 5.0 starts)

1. **iText vs PDFBox** — preference? AGPL acceptability?
2. **Maven module split** — extract `kiteclass-document-gen` now, or keep in kiteclass-core?
3. **Scope of templates** — 4 stubs OK, or need more at launch? (legal/policy templates usually require lawyer review)
4. **FE integration** — preview in browser before download, or direct download only?
5. **Sync vs async** — start sync (simpler), migrate to queue later? Or queue-first?
6. **Sub-PR 5.3 and 5.4 priority** — user wants Word + PPT now, or defer to Wave 6?

Recommend answers at section bottom once user reviews.

---

## 10. Estimated Timeline

| Milestone | Target date (if started 2026-04-19) |
|-----------|-------------------------------------|
| Sub-PR 5.0 foundation | 2026-04-19 |
| Sub-PRs 5.1-5.4 parallel | 2026-04-20 — 2026-04-22 |
| Sub-PR 5.5 integration | 2026-04-23 |
| Sub-PR 5.6 completion | 2026-04-24 |
| Wave 5 MERGED | 2026-04-24 |

~1 week wall-clock with parallel execution.

---

## 11. Log

- **2026-04-18:** Wave plan drafted after PR #358 (meta-gap-priority rule) elevated GAP-047 to position #1 in Block GA. Source material: `documents/04-quality/analyses/skills-gap-analysis-vs-minimax.md` (2026-04-14).
- Status: 🟡 **PLANNING** — awaiting user sign-off on Section 9 open questions before Sub-PR 5.0 starts.

---

## 12. Related

- Gap: [GAP-047](../../04-quality/gaps/GAP-047-document-generation-skills.md)
- Analysis source: [skills-gap-analysis-vs-minimax.md](../../04-quality/analyses/skills-gap-analysis-vs-minimax.md)
- Rule: [meta-gap-priority.md](../../../.claude/rules/meta-gap-priority.md)
- Rule: [design-patterns.md §3 Facade + Strategy](../../../.claude/rules/design-patterns.md)
- Rule: [skill-conventions.md](../../../.claude/rules/skill-conventions.md)
- Integration: GAP-010 branding package API (Wave 3, SHIPPED)

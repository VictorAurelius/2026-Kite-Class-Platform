---
title: Wave 5 — Document Generation Skills (GAP-047)
status: approved
created: 2026-04-18
updated: 2026-04-24
waves: [5]
gaps: [GAP-047, GAP-034, GAP-017]
approved_by: nguyenvankiet
approved_at: 2026-04-24
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

Adopted and adapted from MiniMax-AI/skills. **After 2026-04-24 approval: 3 formats in Wave 5; PPT deferred to Wave 6.**

| Format | Skill file | Tech (Java backend) | Wave |
|--------|-----------|---------------------|:----:|
| PDF | `pdf/SKILL.md` | **OpenHTMLtoPDF + PDFBox** (Apache 2.0 / LGPL 3) | 5 |
| Excel | `excel/SKILL.md` | Apache POI (XSSF) | 5 |
| Word | `word/SKILL.md` | Apache POI (XWPF) | 5 |
| ~~PowerPoint~~ | ~~`pptx/SKILL.md`~~ | Apache POI (XSLF) | **Deferred to Wave 6** |

### 2.2 Per-skill content (≤100 lines per SKILL.md, project-specific gotchas)

Each skill MUST include:
- **Trigger conditions** (frontmatter description) — when Claude should invoke
- **3-pipeline routing** (Create / Edit-Fill / Reformat) from MiniMax pattern
- **Formula-first rule** (Excel) or **token-based theme** (PDF) or **page-type classification** (PPT)
- **Vietnamese-specific gotchas** (Đ, đ, diacritics, long names, VND currency)
- **Reference pointers** to `reference/*.md` for detailed tables
- **Samples pointer** to `kiteclass-core/src/test/resources/document-samples/`

### 2.3 Backend scaffolding

`kiteclass-core/src/main/java/com/kiteclass/core/document/` (inline in `kiteclass-core`, no new Maven module per 2026-04-24 Q2 decision):
- `DocumentGenerationService` — Facade (per `design-patterns.md` §3 Facade pattern)
- `Generator` interface + 3 Strategy implementations (PdfGenerator, XlsxGenerator, DocxGenerator)
- `DocumentRequest` value object (format, template-id, data map, branding-id)
- `DocumentResponse` value object (bytes, mime-type, filename)

### 2.4 Template library (stub)

`kiteclass-core/src/main/resources/templates/` (3 stubs; PPT deferred):
- 1 PDF template (invoice — Vietnamese tax invoice format, Thymeleaf HTML)
- 1 Excel template (attendance report — weekly)
- 1 Word template (teacher contract — placeholder)
- ~~1 PPT template (marketing pitch)~~ — **Wave 6**

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

### Sub-PR 5.0: Foundation + ADR-019

**Branch:** `wave/05-document-generation/foundation`
**Mode:** serialized (lead)
**Depends on:** —

**Scope:**
- **ADR-019** Document generation architecture decision (next free ADR number; original plan said ADR-016 but that slot is taken by fe-be-contract-strategy)
  - Option A: Pure backend (Java-only, OpenHTMLtoPDF + POI) ✅ **Decision per 2026-04-24 Q1**
  - Option B: Hybrid (backend for server docs, FE for interactive preview)
  - Option C: Separate microservice
- Inline in `kiteclass-core` (no new Maven module per Q2 decision)
- Dependency versions pinned:
  - OpenHTMLtoPDF 1.0.x + PDFBox 3.0.x (PDF)
  - Apache POI 5.2.x (Excel XSSF + Word XWPF)
  - Thymeleaf — reuse version from `kiteclass-gateway` for consistency
- `Generator` interface + shared value objects (`DocumentRequest`, `DocumentResponse`)
- `DocumentGenerationService` facade (empty implementation, throws UnsupportedOperation for each format)
- Test scaffolding: `document-samples/` folder + base `DocumentGenerationTestBase`
- 3-layer docs stub: `document-generation/` in `01-business/`

**Effort:** ~3h
**Deliverable:** PR foundation merged, unblocks 5.1-5.3 parallel work.

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

### Sub-PR 5.4: ~~PPT Skill + Marketing Pitch~~ — **DEFERRED to Wave 6** (2026-04-24 Q6 decision)

Rationale: PPT is NICE-HAVE — Canva/Google Slides are viable alternatives. Deferring tightens Wave 5 scope from 4 → 3 parallel agents (~12h vs ~14h wall-clock) and reduces coordination risk. Wave 6 plan will scope: PPT generator + marketing pitch template + training slides template + optional FE preview enhancements for Excel/Word.

---

### Sub-PR 5.5: Branding Integration + Quality Audit

**Branch:** `wave/05-document-generation/integration`
**Depends on:** 5.1-5.3 all merged

**Scope:**
- Unified branding pipeline — all 3 generators fetch from GAP-010 package API
- Cache branding package per-request (per-tenant, short TTL)
- Cross-format consistency test — same tenant gets same colors/logo in PDF + Excel + Word
- PDF `/preview` endpoint returns `Content-Disposition: inline`; `/download` returns `attachment` (Q4 decision)
- Excel + Word: download-only endpoints
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
| ~~5.4 PPT~~ | — | Deferred to Wave 6 |
| 5.5 Branding integration | Serialized (lead) | Depends on all 3 formats |
| 5.6 Wave completion | Serialized (lead) | Final sign-off |

Total wall-clock estimate: ~12h with parallelism vs ~28h serial (–57%).

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
| Library bloat (PDFBox + POI in Core, ~18MB) | 🟢 | Accept; extract to module only when kitehub consumes (follow-up GAP-209) |
| Vietnamese font rendering bugs | 🔴 | Test diacritics + Đ,đ in every format before sub-PR merge; preload `NotoSans` or `DejaVuSans` TTF into PDFBox font resolver |
| Template drift (legal wording) | 🟡 | Defer legal templates to dedicated legal-review wave; Wave 5 uses placeholders |
| PDF generation slow for large invoices | 🟢 | Sync first per Q5; async via GAP-002 queue as follow-up GAP-210 |
| Branding API latency on every generate | 🟡 | Short TTL cache per-tenant in generator Facade |
| ~~iText 7 license (AGPL)~~ | ✅ | **Resolved by Q1 decision** — OpenHTMLtoPDF (LGPL 3/MIT) + PDFBox (Apache 2.0) has no copyleft network clause |
| Thymeleaf CSS subset limits | 🟡 | Test invoice template CSS before 5.1; fallback to PDFBox manual layout if complex styling breaks |

---

## 8. Success Criteria

Wave 5 DONE when:

- [ ] 3 SKILL.md files exist, each <100 lines, with Vietnamese gotchas
- [ ] 3 Generator implementations pass tests with branding applied
- [ ] 1 template per format committed + reviewed (invoice PDF, attendance Excel, teacher contract Word)
- [ ] Sample gallery in `documents/04-quality/samples/` with 1 golden output per format
- [ ] `DocumentGenerationService` facade exposes 3 methods (generateInvoice, generateAttendanceReport, generateTeacherContract)
- [ ] Cross-format branding consistency test passes
- [ ] PDF `/preview` endpoint returns inline + `/download` endpoint returns attachment
- [ ] **ADR-019** ACCEPTED (originally ADR-016 — renumbered after conflict)
- [ ] GAP-047 marked DONE in ROADMAP
- [ ] MiniMax analysis doc marked ADOPTED
- [ ] No new P0 gaps introduced
- [ ] Quality audit score ≥90/100 on Wave 5 changes
- [ ] 4 follow-up gaps FILED (GAP-208 template expansion, GAP-209 module extract trigger, GAP-210 async queue, GAP-211 Excel/Word preview) — see decision guide §follow-up-gaps

---

## 9. Open Questions — ✅ ALL RESOLVED 2026-04-24

All 6 defaults from `wave-05-decision-guide.md` approved by user nguyenvankiet on 2026-04-24. See decision guide for full rationale per question.

| # | Question | Resolution |
|:-:|----------|------------|
| 1 | iText vs PDFBox? | **OpenHTMLtoPDF + PDFBox** (Apache 2.0 / LGPL 3) |
| 2 | Maven module split? | **Inline in `kiteclass-core`** — YAGNI, extract when kitehub consumes |
| 3 | 4 template stubs enough for launch? | **Sufficient for Wave 5 acceptance; NOT for launch** — file GAP-208 for Wave 7 template library expansion |
| 4 | FE preview? | **PDF preview in-browser (inline); Excel/Word download-only** |
| 5 | Sync vs queue? | **Sync in Wave 5** — queue when bulk/large use case emerges |
| 6 | Sub-PR 5.3 + 5.4 priority? | **Keep 5.3 Word in Wave 5; defer 5.4 PPT to Wave 6** |

Wave 5 scope is now **LOCKED**. Any change requires explicit user decision + log entry.

---

## 10. Estimated Timeline (revised 2026-04-24 after PPT defer)

| Milestone | Target (if started 2026-04-25) |
|-----------|--------------------------------|
| Sub-PR 5.0 foundation + ADR-019 | 2026-04-25 |
| Sub-PRs 5.1 PDF, 5.2 Excel, 5.3 Word (parallel) | 2026-04-26 — 2026-04-28 |
| Sub-PR 5.5 branding integration | 2026-04-29 |
| Sub-PR 5.6 wave completion | 2026-04-30 |
| Wave 5 MERGED | 2026-04-30 |

~5-6 days wall-clock with 3 parallel agents (vs ~7 days serial).

---

## 11. Log

- **2026-04-24 (APPROVED):** All 6 defaults from `wave-05-decision-guide.md` approved by user. Wave status PLANNING → APPROVED. Key scope changes captured in this file:
  - Q1: PDF library = OpenHTMLtoPDF + PDFBox (not iText — AGPL conflict with closed-source SaaS)
  - Q2: Inline in kiteclass-core (no new Maven module)
  - Q3: 4 template stubs cover Wave 5 acceptance; launch needs GAP-208 Wave 7 expansion
  - Q4: PDF preview inline; Excel/Word download-only
  - Q5: Sync endpoints only; queue deferred (GAP-210)
  - Q6: PPT dropped from Wave 5 (→ Wave 6); scope 4 → 3 formats; 6 sub-PRs → 5
  - ADR number: 016 → **019** (016 already taken by fe-be-contract-strategy)
  - Timeline shifted: start 2026-04-25, MERGED ~2026-04-30 (5-6 days with 3 parallel agents)
- **2026-04-18:** Wave plan drafted after PR #358 (meta-gap-priority rule) elevated GAP-047 to position #1 in Block GA. Source material: `documents/04-quality/analyses/skills-gap-analysis-vs-minimax.md` (2026-04-14).

---

## 12. Related

- Gap: [GAP-047](../../04-quality/gaps/GAP-047-document-generation-skills.md)
- Analysis source: [skills-gap-analysis-vs-minimax.md](../../04-quality/analyses/skills-gap-analysis-vs-minimax.md)
- Rule: [meta-gap-priority.md](../../../.claude/rules/meta-gap-priority.md)
- Rule: [design-patterns.md §3 Facade + Strategy](../../../.claude/rules/design-patterns.md)
- Rule: [skill-conventions.md](../../../.claude/rules/skill-conventions.md)
- Integration: GAP-010 branding package API (Wave 3, SHIPPED)

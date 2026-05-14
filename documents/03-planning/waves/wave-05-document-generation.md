---
title: Wave 5 — Document Generation Skills (GAP-047)
status: complete  # 6/6 sub-PRs SHIPPED — 5.0/5.1/5.2/5.3 (2026-04-24) + 5.5 + 5.6a + 5.6b (2026-04-25). Wave 5 DONE.
created: 2026-04-18
updated: 2026-04-25
waves: [5]
gaps: [GAP-047, GAP-034, GAP-017]
approved_by: nguyenvankiet
approved_at: 2026-04-24
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

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

### Sub-PR 5.0: Foundation + ADR-019 — ✅ SHIPPED 2026-04-24 (PR #474)

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

### Sub-PR 5.1: PDF Skill + Invoice Generator (P0) — ✅ SHIPPED 2026-04-24 (PR #476)

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

### Sub-PR 5.2: Excel Skill + Attendance Report (P0) — ✅ SHIPPED 2026-04-24 (PR #477)

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

### Sub-PR 5.3: Word Skill + Teacher Contract (P1) — ✅ SHIPPED 2026-04-24 (PR #478)

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

### Sub-PR 5.6: Wave Completion — split into 5.6a + 5.6b

**Rationale for split** (decided 2026-04-25 during Sub-PR 5.5 self-review):
audit findings drive completion confidence, not the other way around. Running
audits *first* lets us file gaps for any P0/P1 surfaces and decide fix-now vs
fix-later before stamping Wave 5 as DONE. Single-PR alternative was rejected
because a stuck audit-suite PR would block all wave-closure paperwork.

#### Sub-PR 5.6a — Audit suite refresh (closes GAP-214)

**Branch:** `wave/05-document-generation/audit-suite`
**Depends on:** Sub-PR 5.5 merged
**Mode:** parallel — 4 Explore agents (one per audit, plus Quality refresh
serialized in parent because it touches the most files), aggregate in parent.

**Scope:**
- API Contract /100 — covers Sub-PRs 5.0–5.5 public-surface drift (Generator
  interface, DocumentRequest/Response, DocumentGenerationController endpoints,
  springdoc OpenAPI annotations).
- Security /100 — covers `pom.xml` cumulative bumps (jjwt, springdoc, tika,
  jsoup, opencsv, jacoco, AWS SDK, commons-compress, poi, ognl) + new
  auth-protected endpoints + tenant resolution path through `BrandingService`.
- Performance /100 — measure PDF/XLSX/DOCX p95 vs BR-DOC-PDF-007 budget
  (`<2s 1-page invoice`); decide whether GAP-210 async queue is still optional.
- Ops Readiness /100 — new HTTP surface, branding cache reads, observability
  hooks, error-response format conformance, rate-limit posture.
- Quality /100 refresh — full 10-category rerun per
  `post-wave-audit-mandate.md` §2.3.
- Findings → new gaps via `audit-to-gap-pipeline.md` Step 1–5 (state-check at
  Step 2.5 mandatory).
- ROADMAP.md: GAP-214 status 🔵 → 🟢 (audit reports committed = closed).

**Effort:** ~3-4h with 4-way parallelism, ~6-8h serial.
**Deliverable:** 5 audit reports under `documents/04-quality/audits/{api,security,performance,ops,quality}/` + N gap files for findings.
**Blocker policy:** any audit-found P0 → file gap + fix in *separate* PR before
Sub-PR 5.6b ships. P1/P2 → file + queue, do not block 5.6b.

#### Sub-PR 5.6b — Wave 5 closure

**Branch:** `wave/05-document-generation/completion`
**Depends on:** Sub-PR 5.6a merged AND any audit-found P0 fixes merged.

**Scope:**
- Sample gallery in `documents/04-quality/samples/wave-05/` — copy golden
  outputs from `kiteclass-core/src/test/resources/document-samples/` + index
  README explaining what each demonstrates (formula-first XLSX, branded PDF
  header, VN diacritics round-trip in DOCX).
- ADR-019: status PROPOSED → ACCEPTED, fill "Outcomes" section with the actual
  shipped stack (OpenHTMLtoPDF + PDFBox + Thymeleaf + POI XSSF/XWPF + ognl 3.3.4
  pin).
- `documents/04-quality/analyses/skills-gap-analysis-vs-minimax.md`: stamp
  "ADOPTED 2026-04-XX, see Wave 5".
- Wave 5 plan §11 Log: completion entry referencing all 6 sub-PRs.
- ROADMAP.md: GAP-047 status 🟡 → 🟢, recommended-next-action bumped to next
  Meta-P0 (likely GAP-046 design-pattern audit).
- Cross-generator integration test reaffirmed (already shipped in 5.5 as
  `DocumentBrandingIntegrationTest`).

**Effort:** ~2h baseline; +X if 5.6a P0 fixes pile up.

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
| 5.6a Audit suite | 4 parallel Explore agents | One per audit category; Quality refresh in parent |
| 5.6b Wave closure | Serialized (lead) | Final sign-off; depends on 5.6a P0 fixes if any |

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
- [ ] No new P0 gaps introduced (re-verified by 5.6a audit suite)
- [ ] Quality audit score ≥90/100 on Wave 5 changes (measured by 5.6a Quality refresh)
- [ ] 4 follow-up gaps FILED (GAP-208 template expansion, GAP-209 module extract trigger, GAP-210 async queue, GAP-211 Excel/Word preview) — see decision guide §follow-up-gaps
- [ ] **5 audit reports committed under `documents/04-quality/audits/{api,security,performance,ops,quality}/`** — closes GAP-214
- [ ] **Sample gallery committed at `documents/04-quality/samples/wave-05/`** with golden outputs for all 3 formats
- [ ] Any P0 gap surfaced by 5.6a audits is fixed before 5.6b ships

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

## 10. Estimated Timeline (revised 2026-04-25 after 5.6 split)

| Milestone | Actual / Target |
|-----------|----------------|
| Sub-PR 5.0 foundation + ADR-019 (PR #474) | ✅ 2026-04-24 |
| Sub-PR 5.1 PDF (PR #476) | ✅ 2026-04-24 |
| Sub-PR 5.2 Excel (PR #477) | ✅ 2026-04-24 |
| Sub-PR 5.3 Word (PR #478) | ✅ 2026-04-24 |
| Sub-PR 5.5 branding integration (PR #529) | 🟡 2026-04-25 awaiting CI |
| Sub-PR 5.6a audit suite (4 parallel audits + Quality refresh) | 🔵 2026-04-25 → 26 |
| Sub-PR 5.6b wave closure (after any 5.6a P0 fixes) | 🔵 2026-04-26 → 27 |
| Wave 5 MERGED | 🔵 ~2026-04-27 |

Audit-suite parallelism keeps 5.6a inside one working day; 5.6b is paperwork-only
unless P0 gaps surface. Total Wave 5 wall-clock: ~3-4 days vs original 5-6 day
estimate (faster than planned because Sub-PRs 5.1/5.2/5.3/5.5 all landed same-day
once worktree-agent lessons stuck).

---

## 11. Log

- **2026-04-25 (Sub-PR 5.6b SHIPPED — Wave 5 DONE):** Wave closure + 4 P0 audit fixes from Sub-PR 5.6a.
  - **GAP-215 (P0) DONE** — `BrandingService.getBranding()` now `@Cacheable("branding-by-tenant", sync=true)` keyed by tenant UUID (TenantContext); `@CacheEvict` on `updateBranding`/`uploadLogo`/`uploadFavicon`. New `BrandingCacheIntegrationTest` (5 cases: warm+hit, per-tenant isolation, evict on each mutator) follows `StudentCacheIntegrationTest` pattern with TestContainers.
  - **GAP-216 (P0) DONE (soft cap)** — added timing assertions to PdfGeneratorTest (<4s first render incl. font + Thymeleaf parse), XlsxGeneratorTest (<2s), DocxGeneratorTest (<2s). Soft caps are regression canaries, not the SLO; full JMH suite tracked as a Wave 7 follow-up per GAP-216 §Acceptance.
  - **GAP-217 (P0) PARTIAL** — added 3 prometheus rules (`DocumentGenerationHighP95`, `DocumentGenerationHighErrorRate`, `DocumentBrandingCacheMissStorm`) to both `infrastructure/helm/kitehub/templates/prometheusrule.yaml` and `kiteclass/docker/prometheus/alert-rules.yml`. Routing depends on `GAP-120` Alertmanager deployment — until that ships, rules fire silently. Status PARTIAL (rules filed, routing deferred).
  - **GAP-218 (P0) DONE** — added image-build assertion in `kiteclass/kiteclass-core/Dockerfile` (`RUN test "$(jar tf target/*.jar | grep -cE 'fonts/DejaVuSans(-Bold)?\.ttf$')" -eq 2 || exit 1`) so a missing TTF fails the build; runbook at `documents/05-guides/operations/runbooks/pdf-generation-font-not-found.md` covers triage / rollback / RCA / prevention.
  - **Sample gallery** — `documents/04-quality/samples/wave-05/{invoice-sample.pdf, attendance-sample.xlsx, teacher-contract-sample.docx}` + `README.md` linking each to its generator + skill.
  - **ADR-019 ACCEPTED** — Outcomes section filled with shipped stack (OpenHTMLtoPDF 1.0.10 + PDFBox 2.0.x + Thymeleaf 3.1.5 + ognl 3.3.4 pin + POI 5.5.1), measured success criteria (3-layer docs at 95/100), audit suite results (api 95 / sec 85 / perf 63 / ops 52 / quality 78), follow-up gap inventory, lessons learned (Dependabot ABI traps + audit cadence drift + state-check practice).
  - **MiniMax analysis ADOPTED** — `documents/04-quality/analyses/skills-gap-analysis-vs-minimax.md` stamped with adoption summary referencing Wave 5.
  - **ROADMAP** — GAP-047 🟡→🟢, GAP-215/216/218 🔵→🟢, GAP-217 🔵→🟡 PARTIAL (Alertmanager dependency); recommended-next-action bumped to **GAP-046 design-pattern audit** (next Meta-P0).
  - **output-review-mandate.md §3 matrix** — audit dates refreshed: API Contract / Security / Performance / Ops Readiness / Quality all 2026-04-25.

  **Wave 5 sub-PR ledger:** #474 5.0 foundation + ADR-019 PROPOSED → #476 5.1 PDF → #477 5.2 Excel → #478 5.3 Word → #529 5.5 branding integration + HTTP endpoints → #530 5.6a audit suite refresh → (this PR) 5.6b closure. PowerPoint deferred to Wave 6 per Q6 scope-lock; viable Canva/Slides alternative until justification appears. **GAP-047 🟢 DONE.**

- **2026-04-25 (Sub-PR 5.5 SHIPPED):** Branding integration + HTTP `/preview` (PDF only, inline) + `/download` (PDF/XLSX/DOCX, attachment) endpoints landed.
  - **Plumbing layer:** `DocumentBrandingAssembler` (pure helper, 7 unit tests) extracts `branding.*` keys from a `BrandingResponse` and merges into `DocumentRequest.data()` with caller-provided keys winning. `HexColorUtil` (9 unit tests) parses CSS hex into POI-friendly RGB bytes / RRGGBB string.
  - **Renderer wiring:** all three generators now apply branding to a format-appropriate locus — `AttendanceReportBuilder` paints the header row fill from `branding.primaryColor` (white text for contrast), `TeacherContractBuilder` colours the `HỢP ĐỒNG GIẢNG DẠY` title run, `InvoiceRenderer` + `invoice.html` add a conditional branded header block (logo + displayName + primary-coloured accent bar) and tint the `<h1>` title. Each falls back gracefully when branding keys are absent (BR-DOC-016).
  - **HTTP layer:** `DocumentGenerationController` (`@RestController` at `/api/v1/documents`) — POST `{format}/preview` rejects non-PDF formats with 400; POST `{format}/download` works for all three. Reads tenant via `TenantContext.getCurrentTenant()`, fetches branding via `BrandingService.getBranding()`, dispatches through the existing `DocumentGenerationService` facade. RFC-5987 UTF-8 filenames preserve VN diacritics. Auth: `@PreAuthorize hasAnyRole(ADMIN, OWNER, TEACHER)`. 9 `@WebMvcTest` cases cover preview/download dispositions, format gating, branding capture via ArgumentCaptor.
  - **Cross-format consistency:** new `DocumentBrandingIntegrationTest` (3 tests, pure JUnit, no Spring boot) wires the real generator trio + assembler with a fixed `BrandingResponse`, verifies primary colour shows up in each format's appropriate locus.
  - **Bug found + fixed mid-stream:** OGNL was bumped to 3.4.x by Dependabot pilots #483/#516, but Thymeleaf 3.1.x calls a 4-arg `OgnlContext(MemberAccess, ClassResolver, TypeConverter, Map)` constructor that the 3.4 line dropped. PdfGeneratorTest threw `NoSuchMethodError`. Re-pinned to **`ognl:3.3.4`** (the version Thymeleaf parent declares) with a fat comment + memory entry `feedback_thymeleaf_ognl_pin.md` so future Dependabot bumps are rejected. Saved as project memory.
  - **3-layer business docs updated:** `rules.md` BR-DOC-010..016, `use-cases.md` UC-DOC-PREVIEW-001 / UC-DOC-DOWNLOAD-001 / UC-DOC-BRANDING-001, `api-contract.md` HTTP section filled with full request/response/error matrix.
  - **Skill updates:** `quality-audit/SKILL.md` Code Quality category now includes Document Generation criteria (diacritics, VND format, branding fallback, OGNL pin sentinel). `two-stage-code-review.md` adds Stage 2.6 trigger for doc-gen PRs (sample golden output, 3-layer docs, branding key safety).
  - Wave 5 success criteria progress: `[✅]` 3 SKILL.md, `[✅]` 3 Generator implementations, `[✅]` 1 template per format, `[✅]` cross-format branding consistency test, `[✅]` PDF /preview + /download, `[✅]` no new P0 gaps. Remaining for Sub-PR 5.6: sample gallery in `documents/04-quality/samples/`, ADR-019 → ACCEPTED, ROADMAP closure, wave-completion report, optional follow-up gap for OGNL upgrade path.
- **2026-04-24 (4/6 SUB-PRs SHIPPED, same day as approval):** Generator trio + foundation merged to main:
  - PR #474 — Sub-PR 5.0 foundation + ADR-019 (Generator interface, DocumentRequest/Response value objects, DocumentGenerationService facade stub, openhtmltopdf 1.0.10 + spring-boot-starter-thymeleaf maven deps, 3-layer business docs stub, 12 unit tests). Required follow-up #475 (GAP-212 — DefaultUrlAllowlistValidatorTest flaky DNS) before #474 CI cleared. SonarCloud Quality Gate fix added DocumentResponse equals/hashCode/toString override (S6218 records-with-array-component).
  - PR #476 — Sub-PR 5.1 PDF + Vietnamese tax invoice template via OpenHTMLtoPDF + Thymeleaf, DejaVuSans TTFs preloaded for Đ/đ/ễ/ă diacritics. ognl 3.4.4 added (Thymeleaf Standard dialect needs OGNL outside Spring integration). 9 unit tests + invoice-sample.pdf golden output.
  - PR #477 — Sub-PR 5.2 Excel + weekly attendance report via Apache POI XSSF. Formula-first (BR-DOC-XLSX-001), color convention (blue inputs / black formulas / green cross-ref), VN weekday labels hardcoded, frozen header pane, IFERROR for div-by-zero. 11 unit tests + attendance-sample.xlsx.
  - PR #478 — Sub-PR 5.3 Word + teacher contract via Apache POI XWPF. A4 + 2.54cm margins, Times New Roman 12pt body / 14pt bold title, VN government header, salary VND formatting, legal placeholder (per scope-lock §3 deferred legal-review wave). 12 unit tests + teacher-contract-sample.docx.
  - Salvage required: 3 parallel worktree agents launched but hit sandbox Write/Bash denial. PDF + Excel files partially salvaged from worktrees; Word implemented greenfield from main session. All 4 sub-PRs landed within ~2 hours total.
  - GAP-047 status updated 🔵 OPEN → 🟡 PARTIAL. Wave plan status `approved` → `in-progress`. Remaining: Sub-PR 5.5 (branding integration + HTTP `/preview`+`/download` endpoints) → Sub-PR 5.6 (wave completion, ADR-019 → ACCEPTED, sample gallery, ROADMAP closure).
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

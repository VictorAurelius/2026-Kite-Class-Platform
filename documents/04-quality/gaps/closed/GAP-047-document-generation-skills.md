# GAP-047: Document Generation Skills + Infrastructure

**Status:** 🟢 DONE — Wave 5 closed 2026-04-25. PDF (#476), Excel (#477), Word (#478) shipped on foundation #474; branding integration + HTTP endpoints shipped Sub-PR 5.5 (#529); audit suite refresh shipped Sub-PR 5.6a (#530); wave closure + 4 P0 audit fixes shipped Sub-PR 5.6b (#532). PowerPoint deferred to Wave 6 per Q6 scope-lock (Canva/Slides viable alternative).
**Priority:** 🔴 P0 (tenant-facing feature parity)
**Domain:** Skills / Backend / Product
**Detected:** 2026-04-14 (MiniMax-AI/skills review)
**Wave 5 PRs:**
- #473 plan(wave-05) approve 6 defaults + scope lock
- #474 Sub-PR 5.0 foundation + ADR-019 (Generator interface, DocumentRequest/Response, facade stub)
- #476 Sub-PR 5.1 PDF + Vietnamese tax invoice (OpenHTMLtoPDF + Thymeleaf, DejaVuSans diacritics)
- #477 Sub-PR 5.2 Excel + weekly attendance report (Apache POI XSSF, formula-first)
- #478 Sub-PR 5.3 Word + teacher contract (Apache POI XWPF, A4 + 2.54cm margins)
- (this PR) Sub-PR 5.5 branding integration + HTTP endpoints — `DocumentBrandingAssembler`, `DocumentGenerationController` (POST `{format}/preview` + `{format}/download` with RFC-5987 filenames), branded renderers (XLSX header fill, DOCX title color, PDF branded header), cross-format integration test, OGNL pin fix (3.3.4)

**Follow-ups (filed as separate gaps, do NOT block this closure):**
- Wave 6: PowerPoint format (deferred per Q6 scope-lock — Canva/Slides viable alternative)
- [GAP-208](GAP-208-template-library-expansion.md) — template library expansion (~20 templates per format for launch)
- [GAP-210](GAP-210-document-generation-async-queue.md) — async queue (Wave 5 sync-only)
- [GAP-217](../GAP-217-document-endpoints-alert-rules.md) 🟡 PARTIAL — Alertmanager routing depends on GAP-120
- [GAP-219](../GAP-219-wave5-audit-followups-p1-p2.md) — umbrella for 5 P1 + 8 P2/P3 audit follow-ups

**Related Docs:**
- `documents/04-quality/skills-gap-analysis-vs-minimax.md` (full analysis)
- `documents/03-planning/waves/wave-05-document-generation.md` (wave plan, status APPROVED → in progress)
- `documents/02-architecture/adr/ADR-019-document-generation-architecture.md`

## Problem

KiteClass **ZERO** document generation skills/infrastructure. MiniMax-AI/skills repo có 4 comprehensive skills cho Excel/Word/PDF/PPT. SaaS giáo dục cần:

- ❌ Hóa đơn học phí (PDF)
- ❌ Chứng chỉ hoàn thành (PDF)
- ❌ Bảng điểm học kỳ (PDF)
- ❌ Báo cáo điểm danh (Excel với formulas)
- ❌ Financial dashboard (Excel)
- ❌ Teacher contracts (Word)
- ❌ Marketing pitch decks (PPT)
- ❌ Brand style guide (PDF — ties to GAP-034)

Currently: ad-hoc HTML→PDF hacks, CSV exports. Tenant phải tự tạo docs ngoài hệ thống.

## Proposed Fix — 5 Phases

### Phase 1: Adopt MiniMax Document Skills (adapt for VN context)

Create 4 skills trong `.claude/skills/document-generation/`:
- `excel/SKILL.md` — dựa minimax-xlsx, Apache POI, formula-first
- `word/SKILL.md` — dựa minimax-docx, Apache POI XWPF, 3 pipelines
- `pdf/SKILL.md` — dựa minimax-pdf, iText 7, 3 routes (CREATE/FILL/REFORMAT)
- `powerpoint/SKILL.md` — dựa pptx-generator, Apache POI XSLF or PptxGenJS

Vietnamese adaptations:
- Font support (Đ, diacritics)
- Currency VND formatting
- Vietnamese legal wording (contracts)
- Template examples relevant to education sector

### Phase 2: Backend Infrastructure

`kiteclass-core/document-generation/` module:

```java
@Service
public class DocumentGenerationService {
  public byte[] generateInvoice(InvoiceData d);
  public byte[] generateCertificate(CertificateData d);
  public byte[] generateTranscript(TranscriptData d);
  public byte[] generateAttendanceReport(AttendanceData d);
  public byte[] generateFinancialReport(FinancialData d);
  public byte[] generateTeacherContract(ContractData d);
  public byte[] generateBrandStyleGuide(BrandingData d);
}

// Design pattern: Strategy (doc type) + Template Method (workflow)
```

### Phase 3: Template Library

`kiteclass-core/src/main/resources/templates/`:
- invoice/, certificate/, transcript/ (PDF)
- report/ (Excel)
- contract/, policy/ (Word)
- marketing/, training/ (PowerPoint)

Each template:
- Legal review (contracts)
- Vietnamese content
- Branding placeholders
- Review via 5 criteria (adapt GAP-011)

### Phase 4: Branding Integration

All generated docs inherit tenant branding (GAP-010 package):
- Logo header
- Primary/secondary color scheme
- Tenant font family
- Consistent design identity

### Phase 5: Quality & Review

- Automated tests: generate sample docs, verify format
- Visual regression: compare generated vs golden templates
- Legal review quarterly for contracts
- Print QA (physical printing for certificates)

## Key Patterns from MiniMax

1. **Formula-first** (Excel) — calculated cells = formulas, never hardcoded
2. **Edit integrity** — load→modify→save, NEVER new Workbook() for edits
3. **3-pipeline routing** (Word/PDF) — Create vs Edit vs Reformat per user intent
4. **Token-based design** (PDF) — consistent theming via tokens
5. **Page-type classification** (PPT) — 5 types, structured decks
6. **Samples-driven learning** — library of working examples

## Acceptance Criteria

### Skills
- [ ] 4 skills created in `.claude/skills/document-generation/`
- [ ] Skills adapt MiniMax concepts cho Vietnamese context
- [ ] Gotchas section per skill

### Backend
- [ ] `document-generation` Maven module
- [ ] `DocumentGenerationService` với 7+ methods
- [ ] Apache POI + iText dependencies
- [ ] Async job queue cho heavy generation (reuse GAP-002)

### Templates
- [ ] 15+ templates across 4 formats
- [ ] Legal review cho contracts
- [ ] Vietnamese localization

### Integration
- [ ] Branding package used in all generators
- [ ] Download endpoints trong tenant/admin dashboards
- [ ] Email delivery with generated attachment

### Quality
- [ ] 90%+ test coverage cho generation code
- [ ] Visual regression tests
- [ ] Print QA sign-off
- [ ] User acceptance test: generate 10+ doc types successfully

## Dependencies

- GAP-010 (branding package) — visual consistency
- GAP-034 (export pack) — consumer of this infrastructure
- GAP-017 (billing) — invoice generation
- GAP-002 (async pipeline) — heavy doc generation

## Implementation Order

Sprint 1: PDF (invoice + certificate) — highest business value
Sprint 2: Excel (attendance + financial reports)
Sprint 3: Word (teacher contracts)
Sprint 4: PowerPoint (marketing pitch)
Sprint 5: Branding integration + quality

## References

- Analysis: `documents/04-quality/skills-gap-analysis-vs-minimax.md`
- MiniMax repo: https://github.com/MiniMax-AI/skills
- Apache POI: https://poi.apache.org/
- iText 7: https://itextpdf.com/

## Log

- 2026-04-25 — **Status 🟡 PARTIAL → 🟢 DONE.** Sub-PR 5.6b SHIPPED (#532): closes 4 P0 audit gaps (GAP-215 branding cache, GAP-216 doc-gen p95 soft-cap, GAP-217 alert rules PARTIAL, GAP-218 PDF font runbook), sample gallery at `documents/04-quality/samples/wave-05/`, ADR-019 PROPOSED → ACCEPTED, MiniMax analysis ADOPTED. Wave 5 ledger: #474 (5.0 foundation) + #476 (5.1 PDF) + #477 (5.2 Excel) + #478 (5.3 Word) + #529 (5.5 branding+HTTP) + #530 (5.6a audit suite) + #532 (5.6b closure). PowerPoint deferred to Wave 6. Audit deltas — Quality 78/100 (+1), API contract 95/100 (A), Security 85/100 (+9), Performance 63/100 (+5 vs baseline), Ops Readiness 52/100 (+3 vs baseline).
- 2026-04-25 — Sub-PR 5.6a SHIPPED (#530): post-wave audit suite refresh per GAP-214 — 5 audits ran in parallel (API contract, security, performance, ops readiness, quality). 4 P0 + 5 P1 + 8 P2/P3 gaps filed (GAP-215..219). Output-review-mandate §3 matrix: Ops + Performance flipped from ⚠️ BASELINE to ✅ REFRESHED.
- 2026-04-25 — Sub-PR 5.5 SHIPPED (#529): branding integration + HTTP endpoints. New artifacts: `DocumentBrandingAssembler` + `HexColorUtil` + `DocumentGenerationController` (POST `/api/v1/documents/{format}/preview|download`), branded renderers across all 3 formats, cross-format integration test, OGNL re-pinned to 3.3.4 (Thymeleaf compatibility). 3-layer business docs and quality-audit / two-stage-code-review skills updated to match.
- 2026-04-24 — Status 🔵 OPEN → 🟡 PARTIAL. Wave 5 generator trio shipped (PDF #476, Excel #477, Word #478) on top of foundation (#474, ADR-019). PowerPoint deferred to Wave 6 per scope-lock (PR #473 Q6). Remaining before 🟢 DONE: Sub-PR 5.5 branding integration + 5.6 wave completion.
- 2026-04-14 — Gap identified via MiniMax skills repo review

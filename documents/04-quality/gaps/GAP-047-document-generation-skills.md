# GAP-047: Document Generation Skills + Infrastructure

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (tenant-facing feature parity)
**Domain:** Skills / Backend / Product
**Detected:** 2026-04-14 (MiniMax-AI/skills review)
**Related Docs:**
- `documents/04-quality/skills-gap-analysis-vs-minimax.md` (full analysis)

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

- 2026-04-14 — Gap identified via MiniMax skills repo review

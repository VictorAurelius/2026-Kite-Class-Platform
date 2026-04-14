# Skills Gap Analysis: KiteClass vs MiniMax-AI/skills

**Ngày:** 2026-04-14
**Reviewed repo:** https://github.com/MiniMax-AI/skills
**Mục tiêu:** So sánh, identify gaps, đưa ra update plan

---

## 1. Executive Summary

**Finding:** KiteClass `.claude/skills/` hiện tại **thiếu nghiêm trọng** document generation skills (Excel, Word, PPT, PDF). MiniMax repo cover rất mạnh mảng này — cần adopt.

**Recommendation:** Adopt + adapt 4 document skills từ MiniMax cho Vietnamese SaaS education context.

---

## 2. MiniMax Skills Inventory (17 skills)

### 2.1 Document Creation (4 skills) — STRONG

| Skill | Tech | Capabilities |
|-------|------|--------------|
| **minimax-xlsx** | XML + xlsx_pack.py | Excel creation, formula-first, color-coded (blue inputs/black formulas/green cross-refs), unpack→edit→repack cho existing files |
| **minimax-docx** | OpenXML SDK (.NET) | Word creation với 3 pipelines (Create/Edit/Format), XSD validation, Samples-driven |
| **pptx-generator** | PptxGenJS + Node.js | PowerPoint 7-step process, 5 page types, design system (16:9, themes, fonts) |
| **minimax-pdf** | Token-based | PDF với 3 routes (CREATE/FILL/REFORMAT), 15+ cover styles, 20+ block types, print-ready |

### 2.2 Development (7 skills)

- Frontend (React/Next.js với animations)
- Full-stack backend (API design)
- Native Android (Kotlin/Jetpack Compose)
- iOS (UIKit, SwiftUI)
- Flutter + React Native
- GLSL shaders

### 2.3 Media & Creative (6 skills)

- minimax-multimodal-toolkit (voice/music/video/image unified API)
- minimax-music-gen
- gif-sticker-maker
- vision-analysis
- + others

---

## 3. KiteClass Skills Current State

### 3.1 Process/Methodology Skills ✅ (mạnh)

- `core/brainstorming-methodology.md`
- `core/tdd-enforcement.md`
- `core/two-stage-code-review.md`
- `core/systematic-debugging.md`
- `core/task-breakdown-guide.md`

### 3.2 Check/Audit Skills ✅ (mạnh, vừa cải thiện)

- `pre-flight-check.md`
- `business-gap-check.md` (v1.3 với AI Branding + Design Patterns)
- `quality-audit/SKILL.md`
- `quality/ui-review/SKILL.md`
- `wave-completion-check.md`
- `workflow/repo-status/SKILL.md`
- `simulation-gap-finder.md` (mới)
- `design-pattern-advisor.md` (mới)

### 3.3 Technical Standards ✅ (đủ)

- `backend/backend-standards.md`
- `frontend/frontend-standards.md`
- `testing/testing-standards.md`
- `devops/devops-standards.md`
- `devops/terraform-cloud-deploy/SKILL.md`

### 3.4 Document Generation ❌ (ZERO coverage)

**Không có skill nào cho:**
- ❌ Excel generation (student grades, financial reports, attendance stats)
- ❌ Word generation (contracts, policies, student handbooks)
- ❌ PDF generation (invoices, transcripts, certificates, receipts)
- ❌ PowerPoint generation (pitch decks, teacher training)

---

## 4. Why This Matters for KiteClass

### Use cases thiếu mà SaaS giáo dục cần:

| Use case | Format | User | Currently |
|----------|--------|------|-----------|
| Hóa đơn học phí | PDF | Student/parent | Crude HTML→PDF |
| Chứng chỉ hoàn thành | PDF | Student | Not implemented |
| Bảng điểm học kỳ | PDF | Student/parent | Not implemented |
| Báo cáo điểm danh | Excel | Teacher/admin | CSV only (GAP-012 stats) |
| Financial dashboard | Excel | Admin | CSV only |
| Student performance analytics | Excel | Teacher | Not implemented |
| Teacher contract | Word | HR | Manual Word outside system |
| School policy document | Word | Admin | Manual Word outside |
| Marketing pitch | PPT | Center owner | Manual PPT outside |
| Teacher training materials | PPT/PDF | Trainer | Manual |
| Monthly admin report | PDF | Center owner | Not implemented |
| Brand style guide (from AI branding) | PDF | Center owner | GAP-034 planned |

**Impact:** Tenant phải tự tạo docs ngoài hệ thống → UX kém → churn risk.

---

## 5. Gap Comparison Matrix

| Category | KiteClass | MiniMax | Gap |
|----------|-----------|---------|-----|
| Process/methodology | ⭐⭐⭐⭐⭐ | ⭐⭐ | KiteClass stronger |
| Check/audit | ⭐⭐⭐⭐⭐ | ⭐ | KiteClass stronger |
| Design patterns | ⭐⭐⭐⭐ (vừa add) | ⭐⭐ | KiteClass stronger |
| Technical standards | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Equal |
| **Document generation** | **❌ (0%)** | **⭐⭐⭐⭐⭐** | **🔴 Major gap** |
| Media generation | ❌ | ⭐⭐⭐⭐ | Major gap (low priority for now) |
| Mobile dev | ❌ | ⭐⭐⭐⭐ | N/A (no mobile app) |

---

## 6. Update Plan — 5 Phases

### Phase 1: Adopt Document Creation Skills (P0) 🔴

Adapt 4 MiniMax document skills cho KiteClass:

#### 6.1 `skills/document-generation/excel/SKILL.md`
**Based on:** minimax-xlsx
**Adaptations for KiteClass:**
- Tiếng Việt field names, currency VND formatting
- Color conventions adapted (giữ formula-first pattern)
- Templates: attendance report, financial dashboard, grade sheet, student roster
- Integration với backend: `XlsxGeneratorService` in kiteclass-core
- Library: Apache POI (Java) thay cho XML direct (enterprise-ready)

#### 6.2 `skills/document-generation/word/SKILL.md`
**Based on:** minimax-docx
**Adaptations:**
- Templates: teacher contract (Vietnamese law), student agreement, parent consent, policy docs
- Library: Apache POI XWPF (Java, OpenXML compatible)
- XSD validation approach giữ nguyên
- 3 pipelines (Create/Edit/Format) giữ nguyên

#### 6.3 `skills/document-generation/pdf/SKILL.md`
**Based on:** minimax-pdf
**Adaptations:**
- Templates: invoice, transcript, certificate, receipt, brand style guide (GAP-034)
- Library: iText 7 hoặc Apache PDFBox
- 3 routes (CREATE/FILL/REFORMAT)
- Token-based theming integrates với KiteClass branding (consistency!)
- Print-ready (quan trọng cho chứng chỉ, hóa đơn)

#### 6.4 `skills/document-generation/powerpoint/SKILL.md`
**Based on:** pptx-generator
**Adaptations:**
- Templates: marketing pitch cho center owners, teacher training slides, parent presentation
- Tech: PptxGenJS (Node.js) — FE-side generation
- OR Apache POI XSLF (Java, server-side)
- 5 page types giữ nguyên
- Tích hợp branding (tenant logo, colors)

### Phase 2: Service Integration (P0) 🔴

Implement backend services trong kiteclass-core:

```java
@Service
public class DocumentGenerationService {
  @Autowired XlsxGenerator xlsx;
  @Autowired DocxGenerator docx;
  @Autowired PdfGenerator pdf;
  @Autowired PptxGenerator pptx;

  // Use cases
  public byte[] generateInvoice(InvoiceData data) { ... }        // PDF
  public byte[] generateCertificate(CertificateData data) { ... } // PDF
  public byte[] generateTranscript(TranscriptData data) { ... }  // PDF
  public byte[] generateAttendanceReport(AttendanceReportData d) { ... } // Excel
  public byte[] generateFinancialReport(FinancialData d) { ... } // Excel
  public byte[] generateTeacherContract(ContractData d) { ... }  // Word
  public byte[] generateMarketingPitch(PitchData d) { ... }     // PPT
}
```

Design pattern: **Strategy + Template Method** (per design-patterns.md rules)

### Phase 3: Templates Library (P1) 🟠

Create template library trong `kiteclass-core/src/main/resources/templates/`:

```
templates/
├── invoice/
│   ├── standard.pdf.ftl
│   ├── tax-invoice.pdf.ftl
│   └── receipt.pdf.ftl
├── certificate/
│   ├── course-completion.pdf
│   └── attendance-excellence.pdf
├── transcript/
│   ├── semester.pdf
│   └── final.pdf
├── report/
│   ├── attendance.xlsx
│   ├── financial-monthly.xlsx
│   ├── financial-yearly.xlsx
│   └── student-performance.xlsx
├── contract/
│   ├── teacher-full-time.docx
│   ├── teacher-part-time.docx
│   └── student-enrollment.docx
└── marketing/
    ├── center-overview.pptx
    └── teacher-training.pptx
```

Mỗi template:
- Review theo checklist (like GAP-011 for branding templates)
- Vietnamese content, proper legal wording
- Branded với tenant colors/logo injection

### Phase 4: Branding Integration (P1) 🟠

Documents phải respect tenant branding (GAP-021 scope):

```
Invoice PDF generated:
  ↓ fetch branding package (GAP-010)
  ↓ apply primary color to header
  ↓ inject tenant logo
  ↓ use tenant font (or fallback)
  ↓ output branded PDF
```

Tất cả generated docs (PDF/Word/Excel/PPT) inherit tenant branding.

### Phase 5: Review & Extend Process (P2) 🟡

Update existing skills để include document generation:

- `two-stage-code-review.md`: check nếu PR có generate docs → verify template review
- `quality-audit/SKILL.md`: thêm category "Document Generation Quality"
- `pre-flight-check.md`: check templates exist trước khi implement feature

---

## 7. Key Insights từ MiniMax

### 7.1 Formula-First (Excel)
**Rule:** Every calculated cell MUST use Excel formula, KHÔNG hardcode.
**Apply to KiteClass:** Financial reports phải có formulas (totals, avg, percentage) để user có thể edit và recalc.

### 7.2 Edit Integrity (Word/Excel)
**Rule:** NEVER create new Workbook()/Document() for edits. Always load original → modify → save.
**Reason:** Round-trip preserves VBA, pivot tables, macros, advanced features.
**Apply:** Khi update existing contract/report, preserve formatting user manually added.

### 7.3 Three-Pipeline Routing (Word/PDF)
**Rule:** Input file exists? → Edit/Fill/Reformat. No input? → Create from template.
**Apply:** Clear UX flow cho tenant: "Upload your contract" vs "Generate new".

### 7.4 Token-Based Design (PDF)
**Rule:** Single accent color + typography tokens cascade across all pages.
**Apply:** Tenant branding (primary/secondary colors, fonts) applied consistently across ALL generated docs. Integrate với GAP-010 branding package.

### 7.5 Page-Type Classification (PPT)
**Rule:** Every slide = exactly 1 of 5 page types (Cover/TOC/Section/Content/Summary).
**Apply:** Structured training materials, consistent deck quality.

### 7.6 Samples-Driven Learning (Word)
**Rule:** Before writing code, study `Samples/*.cs` library of compilable patterns.
**Apply:** Create `kiteclass-core/src/test/resources/document-samples/` với example docs + generation code cho reference.

---

## 8. Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Library bloat (4 new Java libs) | Modularize: separate Maven module `kiteclass-document-gen` |
| Template drift (legal docs) | Legal review quarterly; version templates |
| Performance (PDF gen slow) | Async queue (reuse GAP-002 infrastructure) |
| Branding inconsistency | Integrate GAP-010 package API mandatory |
| Vietnamese font issues | Test comprehensive: Đ, đ, diacritics, long names |
| Print quality | QA process với physical printing test |

---

## 9. Implementation Priorities

**Sprint 1 (P0):** PDF skill + Invoice generator + Certificate generator
**Sprint 2 (P0):** Excel skill + Attendance report + Financial report
**Sprint 3 (P1):** Word skill + Teacher contract templates
**Sprint 4 (P2):** PPT skill + Marketing pitch template
**Sprint 5 (P2):** Branding integration across all generators
**Sprint 6 (P2):** Quality audit + legal review

---

## 10. Related Gaps

Implementing này sẽ close/partially close:
- GAP-034: Branding export pack (ZIP + PDF style guide) — PDF skill enables this
- GAP-017: AI usage billing — PDF invoices needed
- GAP-012: Quality review reports — PDF format

Create new gap to track:
- **GAP-047:** Document Generation Skills Adoption (from MiniMax)

---

## 11. Comparison with Existing Project Approach

**Current:** Ad-hoc library usage per feature (JasperReports here, manual PDF there)
**Proposed:** Unified document generation infrastructure + skill library

**Benefits:**
- Consistent quality across all documents
- Tenant branding applied uniformly
- Reusable templates reduce dev time
- Legal/compliance reviewed once per template
- Better UX (download anything as PDF/Excel)

---

## 12. Log

- 2026-04-14 — Review MiniMax skills repo, identify document generation gap, plan adoption

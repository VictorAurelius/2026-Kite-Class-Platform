# Document Generation — Business Rules

**Domain:** document-generation
**Source:** GAP-047, Wave 5 (all sub-PRs), ADR-019
**Status:** Sub-PR 5.0 stub — rules below are placeholders; sub-PRs 5.1–5.3 fill per format.

## Rules

### Core invariants (Sub-PR 5.0 — foundation)

| ID | Rule |
|----|------|
| BR-DOC-001 | Every generation call goes through `DocumentGenerationService` facade; no direct Generator calls from controllers |
| BR-DOC-002 | Generators MUST NOT leak format-specific types (PDFBox / POI / XWPF) to callers (per design-patterns.md §3.10) |
| BR-DOC-003 | Every request MUST carry non-blank `tenantId` — generators enforce tenant isolation via branding package fetch |
| BR-DOC-004 | Branding colors, logos, fonts come ONLY from Branding Package API (ADR-009); zero hardcoded tenant visuals |
| BR-DOC-005 | Wave 5 scope: 3 formats (PDF, XLSX, DOCX). PPT deferred to Wave 6. |
| BR-DOC-006 | Generation is synchronous in Wave 5; async queue (GAP-210) introduced when p95 > 5s or bulk flows appear |

### Per-format rules (to be filled)

### PDF rules (Sub-PR 5.1)

| ID | Rule |
|----|------|
| BR-DOC-PDF-001 | Supported templates must be whitelisted in `PdfGenerator`; unknown `templateId` → `IllegalArgumentException`. Wave 5 whitelist: `invoice` only. |
| BR-DOC-PDF-002 | Vietnamese diacritics (Đ, ễ, ă, ô, ơ, ư) MUST render correctly. DejaVuSans TTF is preloaded via OpenHTMLtoPDF font resolver; built-in PDFBox fonts are insufficient. |
| BR-DOC-PDF-003 | VND monetary values formatted with `.` thousand separator (`vi-VN` locale), no decimal places. Example: `2.700.000` not `2,700,000`. |
| BR-DOC-PDF-004 | Invoice template follows VN tax invoice (hóa đơn GTGT) layout: header line, buyer meta (name, tax code, address), items table, VAT summary, grand total, attribution footer. |
| BR-DOC-PDF-005 | Filename generated as `invoice-{invoiceNumber}.pdf`; fallback to `invoice.pdf` if number missing. |
| BR-DOC-PDF-006 | HTML templates use Thymeleaf with `vi-VN` locale; avoid HTML entities outside XHTML core (`&middot;`, `&nbsp;` break OpenHTMLtoPDF's strict SAX parser — use Unicode chars). |
| BR-DOC-PDF-007 | PDF rendering is synchronous; p95 budget <2s for 1-page invoice. Async queue (GAP-210) once bulk or large documents appear. |

### Excel rules (Sub-PR 5.2)

| ID | Rule |
|----|------|
| BR-DOC-XLSX-001 | **Formula-first:** any derivable value (counts, sums, percentages) MUST be a cell formula, not a pre-computed literal. Users editing a P/A/L/E cell must see totals recalculate. |
| BR-DOC-XLSX-002 | Supported templates whitelisted in `XlsxGenerator`; unknown `templateId` → `IllegalArgumentException`. Wave 5 whitelist: `attendance` only. |
| BR-DOC-XLSX-003 | Color convention: **blue** inputs, **black** formulas, **green** cross-reference/ratio cells. |
| BR-DOC-XLSX-004 | VN weekday labels `Thứ 2` through `Thứ 7` hardcoded in header (POI does not localise). |
| BR-DOC-XLSX-005 | Attendance rate column uses `IFERROR(...)` to guard zero-denominator (student with no recorded days). |
| BR-DOC-XLSX-006 | Percent cells use Excel number format `0.00%` (2 decimal places); never pre-format as string. |
| BR-DOC-XLSX-007 | Header row frozen via `createFreezePane(1, 3)` so student column + header stay visible while scrolling. |
| BR-DOC-XLSX-008 | Filename pattern: `attendance-{weekStart}.xlsx` (e.g. `attendance-2026-04-20.xlsx`). |

### Word rules (Sub-PR 5.3)

| ID | Rule |
|----|------|
| BR-DOC-DOCX-001 | **Create pipeline only** in Wave 5. Edit-Fill + Reformat pipelines deferred to later waves. Implementations must reject unknown pipelines with clear error. |
| BR-DOC-DOCX-002 | Supported templates whitelisted in `DocxGenerator`; unknown `templateId` → `IllegalArgumentException`. Wave 5 whitelist: `teacher-contract` only. |
| BR-DOC-DOCX-003 | Required keys fail-fast with `IllegalArgumentException` naming the missing key (teacher-contract: `teacherName`, `teacherIdNumber`, `tenantName`, `tenantAddress`, `startDate`, `endDate`). |
| BR-DOC-DOCX-004 | Vietnamese typography defaults: Times New Roman, 12 pt body, 14 pt bold title, A4 portrait, 2.54 cm margins (1440 twips). |
| BR-DOC-DOCX-005 | Legal wording for contracts is a **placeholder** until a dedicated legal-review wave ships. Rendered output must call this out explicitly ("[sẽ được pháp lý duyệt ở wave sau]"). |
| BR-DOC-DOCX-006 | Salary / monetary values formatted with `vi-VN` locale (`#.##0` grouping). |
| BR-DOC-DOCX-007 | Validation: unit tests must re-open the generated .docx via POI `XWPFDocument` and assert paragraph count ≥ minimum + expected text content round-trips. |
| BR-DOC-DOCX-008 | Filename pattern: `teacher-contract-<slug>-<startDate>.docx` (slug = teacher name lower-cased with spaces → hyphens, VN locale preserved). |

### Delivery (Sub-PR 5.5)

| ID | Rule |
|----|------|
| BR-DOC-010 | PDF: `/preview` endpoint returns `Content-Disposition: inline`; `/download` returns `attachment` |
| BR-DOC-011 | XLSX + DOCX: download-only endpoints (`attachment`); no inline preview |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `document.generation.pdf.font.family` | `DejaVuSans` (added in 5.1) | Vietnamese-safe font registered into OpenHTMLtoPDF font resolver; loaded from `resources/fonts/` |
| `document.generation.cache.branding.ttl-seconds` | `60` (to be added in 5.5) | Per-tenant branding package cache |

## Log

- 2026-04-24 — DOCX rules filled (Sub-PR 5.3). 8 rules BR-DOC-DOCX-001..008 covering Create-only pipeline, template whitelist, required keys, VN typography, legal-placeholder disclosure, filename.
- 2026-04-24 — XLSX rules filled (Sub-PR 5.2). 8 rules BR-DOC-XLSX-001..008 covering formula-first, color convention, VN labels, percent format, freeze pane, filename. Word section untouched (Sub-PR 5.3).
- 2026-04-24 — PDF rules filled (Sub-PR 5.1). 7 rules BR-DOC-PDF-001..007 covering template whitelist, diacritic contract, VND formatting, layout, filename, entity handling, sync timing budget. Font config key finalised (`DejaVuSans`). Excel + Word sections untouched (Sub-PRs 5.2/5.3).
- 2026-04-24 — Stub rules file created (Sub-PR 5.0 foundation / GAP-047). Per-format rules to be filled by Sub-PRs 5.1–5.3; delivery rules by 5.5.

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

- XLSX rules → Sub-PR 5.2 (formula-first, attendance P/A/L/E conventions)
- DOCX rules → Sub-PR 5.3 (teacher contract placeholder, XSD validation)

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

- 2026-04-24 — PDF rules filled (Sub-PR 5.1). 7 rules BR-DOC-PDF-001..007 covering template whitelist, diacritic contract, VND formatting, layout, filename, entity handling, sync timing budget. Font config key finalised (`DejaVuSans`). Excel + Word sections untouched (Sub-PRs 5.2/5.3).
- 2026-04-24 — Stub rules file created (Sub-PR 5.0 foundation / GAP-047). Per-format rules to be filled by Sub-PRs 5.1–5.3; delivery rules by 5.5.

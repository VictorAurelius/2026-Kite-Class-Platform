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

- PDF rules → Sub-PR 5.1 (invoice template, VND formatting, Vietnamese diacritic validation)
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
| `document.generation.pdf.font.primary` | `NotoSans` (to be added in 5.1) | Vietnamese-safe font for PDFBox resolver |
| `document.generation.cache.branding.ttl-seconds` | `60` (to be added in 5.5) | Per-tenant branding package cache |

## Log

- 2026-04-24 — Stub rules file created (Sub-PR 5.0 foundation / GAP-047). Per-format rules to be filled by Sub-PRs 5.1–5.3; delivery rules by 5.5.

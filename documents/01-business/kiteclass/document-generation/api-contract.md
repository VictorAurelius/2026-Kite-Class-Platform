# Document Generation — API Contract

**Domain:** document-generation
**Source:** GAP-047, Wave 5, ADR-019
**Status:** Sub-PR 5.0 stub — no HTTP endpoints ship in this sub-PR. Endpoints arrive with Sub-PR 5.5 (branding integration) which exposes `/preview` and `/download` per format.

## Internal library contract (Sub-PR 5.0)

Not an HTTP contract — consumed in-process by domain services.

**Facade:** `com.kiteclass.core.module.document.DocumentGenerationService#generate(DocumentRequest): DocumentResponse`

### Inputs — `DocumentRequest`

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| `format` | `DocumentFormat` (enum PDF / XLSX / DOCX) | ✅ | non-null |
| `templateId` | `String` | ✅ | non-blank, matches a template known to the selected Generator |
| `tenantId` | `String` | ✅ | non-blank; used to fetch branding package (ADR-009) |
| `data` | `Map<String, Object>` | optional | null coerced to empty map; values serialisable by the template engine |

### Output — `DocumentResponse`

| Field | Type | Notes |
|-------|------|-------|
| `bytes` | `byte[]` | generated payload |
| `mimeType` | `String` | matches `format.mimeType()` |
| `filename` | `String` | suggestion for `Content-Disposition` in Sub-PR 5.5 endpoints |

### Errors

| Condition | Thrown |
|-----------|--------|
| null format / blank templateId / blank tenantId | `IllegalArgumentException` |
| No Generator registered for the requested format | `UnsupportedOperationException` |
| Template not found in registered Generator | `IllegalArgumentException` (per-generator — defined in Sub-PR 5.1–5.3) |

## PDF Generator data-map schema (Sub-PR 5.1)

`templateId = "invoice"` expects the following keys in `DocumentRequest#data()`:

| Key | Type | Required | Description |
|-----|------|:--------:|-------------|
| `invoiceNumber` | `String` | ✅ | Unique invoice identifier; shipped into `Content-Disposition` filename |
| `issueDate` | `String` (`yyyy-MM-dd`) | ✅ | Date printed on header subtitle |
| `buyerName` | `String` | ✅ | Payer name (supports Vietnamese diacritics, Đ/đ) |
| `buyerTaxCode` | `String` | ✅ | Vietnamese tax code (10 or 13 digits) |
| `buyerAddress` | `String` | ✅ | Single-line address; wraps on narrow columns |
| `items` | `List<Map<String, Object>>` | ✅ | Each item: `description` (String), `qty` (Number), `unitPrice` (BigDecimal), `lineTotal` (BigDecimal) |
| `subtotal` | `BigDecimal` | ✅ | Sum of item totals, pre-VAT |
| `vatRate` | `BigDecimal` | ✅ | VAT rate as decimal (e.g. `0.08` for 8%) |
| `vatAmount` | `BigDecimal` | ✅ | `subtotal * vatRate` |
| `total` | `BigDecimal` | ✅ | `subtotal + vatAmount`, printed as grand total |

Monetary values are formatted by the renderer with `.` thousand separator (`vi-VN` locale). Callers pass raw `BigDecimal` — do NOT pre-format.

## XLSX Generator data-map schema (Sub-PR 5.2)

`templateId = "attendance"` expects the following keys in `DocumentRequest#data()`:

| Key | Type | Required | Description |
|-----|------|:--------:|-------------|
| `weekStart` | `String` (`yyyy-MM-dd`) | ✅ | First day of week; printed in title + filename |
| `className` | `String` | ✅ | Class identifier (e.g. `10A1`), printed in title |
| `students` | `List<Map<String, Object>>` | ✅ | Each entry: `id` (String), `name` (String) |
| `attendance` | `Map<String, Map<String, String>>` | optional | Outer key student id, inner key VN weekday label (`Thứ 2..Thứ 7`), value `P`/`A`/`L`/`E`. Missing entries render as blank input cells. |

Empty `students` list is valid — produces header + summary only.

## HTTP endpoints (Sub-PR 5.5 — upcoming)

Placeholder table filled by Sub-PR 5.5:

| Method | Path | Disposition | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/documents/{type}/{id}/preview` | `inline` (PDF only) | bearer |
| `GET` | `/api/v1/documents/{type}/{id}/download` | `attachment` | bearer |

Error codes, rate limits, response schemas, OpenAPI annotations — all added with 5.5.

## Log

- 2026-04-24 — XLSX data-map schema for `attendance` template documented (Sub-PR 5.2). Word schema untouched.
- 2026-04-24 — PDF data-map schema for `invoice` template documented (Sub-PR 5.1). Excel + Word schemas untouched (Sub-PRs 5.2/5.3).
- 2026-04-24 — Stub API contract for Sub-PR 5.0 (foundation, library-only). HTTP surface defined when 5.5 ships.

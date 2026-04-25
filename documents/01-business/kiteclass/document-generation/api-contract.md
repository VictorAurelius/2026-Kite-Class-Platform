# Document Generation — API Contract

**Domain:** document-generation
**Source:** GAP-047, Wave 5, ADR-019
**Status:** Sub-PR 5.5 SHIPPED — HTTP endpoints `/api/v1/documents/{format}/preview` + `/api/v1/documents/{format}/download` are live. Sub-PR 5.6 wave-completion still pending.

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

## DOCX Generator data-map schema (Sub-PR 5.3)

`templateId = "teacher-contract"` expects the following keys in `DocumentRequest#data()`:

| Key | Type | Required | Description |
|-----|------|:--------:|-------------|
| `teacherName` | `String` | ✅ | Full VN name; supports diacritics, slugged into filename |
| `teacherIdNumber` | `String` | ✅ | Vietnamese CMND / CCCD number |
| `tenantName` | `String` | ✅ | School / tenant name |
| `tenantAddress` | `String` | ✅ | Tenant address line |
| `startDate` | `String` | ✅ | ISO date `yyyy-MM-dd`; printed in body + filename |
| `endDate` | `String` | ✅ | ISO date `yyyy-MM-dd` |
| `salaryVnd` | `BigDecimal` | ✅ | Monthly salary in VND; formatted with `.` thousand separator |
| `subjects` | `String` | optional | Comma-separated subject list; defaults to `—` |

Missing a required key → `IllegalArgumentException` with the key name in the message.

## HTTP endpoints (Sub-PR 5.5)

Two POST endpoints, format selected by path variable, body carries template id + data map.

| Method | Path | Disposition | Allowed formats | Auth |
|--------|------|-------------|------------------|------|
| `POST` | `/api/v1/documents/{format}/preview` | `inline` | PDF only (xlsx/docx → 400) | `hasAnyRole('ADMIN','OWNER','TEACHER')` |
| `POST` | `/api/v1/documents/{format}/download` | `attachment` | pdf, xlsx, docx | `hasAnyRole('ADMIN','OWNER','TEACHER')` |

`{format}` is the lower-case extension: `pdf`, `xlsx`, `docx`.

### Request body — `DocumentGenerationRequestDto`

```json
{
  "templateId": "invoice",
  "data": { /* template-specific keys, see per-format schemas above */ }
}
```

| Field | Type | Required | Validation |
|-------|------|:--------:|------------|
| `templateId` | `String` | ✅ | `@NotBlank`; must match a template the chosen Generator whitelists |
| `data` | `Map<String, Object>` | optional | null coerced to empty map. Caller-supplied `branding.*` keys WIN over server-resolved branding (BR-DOC-015) |

Server-side overrides (clients MUST NOT supply, BR-DOC-012):
- `branding.primaryColor`, `branding.secondaryColor`, `branding.accentColor`, `branding.logoUrl`, `branding.displayName` — injected by `DocumentBrandingAssembler` from `BrandingService.getBranding()` (TenantContext-scoped lookup of the `Branding` entity).

Tenant resolution: `X-Tenant-Id` header → `TenantContext` (UUID) → `BrandingService.getBranding()`. The body's lack of `tenantId` field is intentional.

### Response

| Header | Value |
|--------|-------|
| `Content-Type` | `application/pdf` / `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` / `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| `Content-Disposition` | `inline; filename*=UTF-8''<encoded>` (preview) or `attachment; filename*=UTF-8''<encoded>` (download). RFC-5987 UTF-8 form preserves Vietnamese diacritics in filenames (BR-DOC-014) |
| Body | Raw bytes — PDF/PDFBox stream, XLSX OOXML zip, or DOCX OOXML zip |

Filename comes from the generator's `DocumentResponse.filename()` — see per-format BR-DOC-*-005/008 rules for naming patterns.

### Error codes

| HTTP | Trigger |
|------|---------|
| `400 Bad Request` | non-PDF format on `/preview` (`Preview only supported for PDF; requested format: XLSX/DOCX`); unsupported `format` path variable (`Unsupported format: ...`); `@Valid` violation on `templateId`; unknown `templateId` for the chosen Generator; missing required data key for the chosen template (e.g. `teacherName` missing for `teacher-contract`) |
| `401 Unauthorized` | request not authenticated |
| `403 Forbidden` | authenticated user lacks ADMIN/OWNER/TEACHER role |
| `500 Internal Server Error` | renderer I/O failure (font missing, template engine error, etc.) — should never happen with the bundled templates |

Rate limits: not enforced in Wave 5; relies on existing global gateway throttling. Per-format rate limits + async queue are GAP-210 (Wave 5 follow-up).

OpenAPI: auto-generated by springdoc; tag `DocumentGeneration`, operation summaries on each method.

## Log

- 2026-04-25 — Sub-PR 5.5 SHIPPED. HTTP endpoints `/api/v1/documents/{format}/preview` + `/download` documented in full: request body, server-side branding override list, response headers, error code matrix, auth roles, rate-limit posture. Status header bumped from "5.0 stub" to "5.5 SHIPPED".
- 2026-04-24 — DOCX data-map schema for `teacher-contract` template documented (Sub-PR 5.3).
- 2026-04-24 — XLSX data-map schema for `attendance` template documented (Sub-PR 5.2). Word schema untouched.
- 2026-04-24 — PDF data-map schema for `invoice` template documented (Sub-PR 5.1). Excel + Word schemas untouched (Sub-PRs 5.2/5.3).
- 2026-04-24 — Stub API contract for Sub-PR 5.0 (foundation, library-only). HTTP surface defined when 5.5 ships.

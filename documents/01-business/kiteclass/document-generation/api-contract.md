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

## HTTP endpoints (Sub-PR 5.5 — upcoming)

Placeholder table filled by Sub-PR 5.5:

| Method | Path | Disposition | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/documents/{type}/{id}/preview` | `inline` (PDF only) | bearer |
| `GET` | `/api/v1/documents/{type}/{id}/download` | `attachment` | bearer |

Error codes, rate limits, response schemas, OpenAPI annotations — all added with 5.5.

## Log

- 2026-04-24 — Stub API contract for Sub-PR 5.0 (foundation, library-only). HTTP surface defined when 5.5 ships.

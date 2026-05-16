# Student Bulk Import — API Contract

**Domain:** bulk-import
**Base path:** `/api/v1/students/bulk-import`
**Controller:** `BulkImportController` (kiteclass-core)
**Auth:** Bearer token; permission `STUDENT_WRITE`; `X-Tenant-Id` header required on every call
**Last verified:** 2026-04-21

## Endpoints

### POST /preview — dry-run

Parse + validate uploaded xlsx without any DB writes.

| Field | Value |
|-------|-------|
| HTTP | `POST /api/v1/students/bulk-import/preview` |
| Consumes | `multipart/form-data` |
| Produces | `application/json` |
| Headers | `Authorization: Bearer <token>`, `X-Tenant-Id: <uuid>` |
| Body | `file=<xlsx>` (field name `file`) |
| Success | `200 OK` → `ApiResponse<BulkImportResult>` |

### POST /commit — write

Same parsing + validation as preview, then persists valid rows and creates a `BulkImportJob` audit row.

| Field | Value |
|-------|-------|
| HTTP | `POST /api/v1/students/bulk-import/commit` |
| Consumes | `multipart/form-data` |
| Produces | `application/json` |
| Headers | same as preview |
| Body | `file=<xlsx>` |
| Success | `201 Created` + `Location: /api/v1/students/bulk-import/jobs/{jobId}` → `ApiResponse<BulkImportResult>` |

### POST /jobs/{id}/errors — download error report

Re-validate and stream xlsx report. MVP is stateless — client re-uploads the file.

| Field | Value |
|-------|-------|
| HTTP | `POST /api/v1/students/bulk-import/jobs/{id}/errors` |
| Consumes | `multipart/form-data` |
| Produces | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| Headers | same as preview |
| Path | `id` = job id returned by `/commit` |
| Body | `file=<xlsx>` (re-upload the original) |
| Success | `200 OK` + `Content-Disposition: attachment; filename="bulk-import-errors-{id}.xlsx"` |

### GET /jobs/{id}/errors — placeholder

Present only so the route appears in OpenAPI. Always responds `405 METHOD_NOT_ALLOWED`. Use the POST variant above.

## Request schema

xlsx data rows (header = row 1). All cells parsed as strings first to preserve user formatting.

| Column | Required | Rule | Max length |
|--------|:--------:|------|:----------:|
| name | ✅ | 2–100 chars after trim | 100 |
| email | ✅ | RFC-ish regex, must match `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` | 255 |
| phone | ❌ | `^0\d{9}$` (VN mobile, 10 digits starting with 0) | 10 |
| dateOfBirth | ❌ | `dd/MM/yyyy`, past-or-present | — |
| gender | ❌ | case-insensitive `MALE` / `FEMALE` | — |
| address | ❌ | free text | 1000 |
| note | ❌ | free text | — |

File constraints:
- Format: `.xlsx` only
- Max file size: **5 MB** (enforced by `spring.servlet.multipart.max-file-size`)
- Max data rows: **10 000**

## Response schema

### ApiResponse wrapper

```json
{
  "success": true,
  "message": "Preview xong",
  "data": { /* BulkImportResult */ }
}
```

### BulkImportResult

```json
{
  "jobId": 42,                // null for preview
  "totalRows": 1234,
  "successCount": 1200,
  "errorCount": 34,
  "errors": [                 // truncated to first 10
    { "rowNumber": 7, "field": "email", "message": "Email không hợp lệ" },
    { "rowNumber": 12, "field": "phone", "message": "Số điện thoại không hợp lệ..." },
    { "rowNumber": 28, "field": "email", "message": "Email trùng với dòng 7 trong cùng file" }
  ]
}
```

### Error-report xlsx (POST /jobs/{id}/errors)

xlsx binary; columns:

| Column | Description |
|--------|-------------|
| rowNumber | 1-indexed row number from original file |
| field | Offending field name (`name`, `email`, `phone`, `date_of_birth`, `gender`, `address`, `row`) |
| message | Vietnamese user-facing error |

## Error codes

| HTTP | Code | When |
|------|------|------|
| 400 | `BULK_IMPORT_EMPTY_FILE` | Missing `file` part or empty upload |
| 413 | `BULK_IMPORT_ROW_LIMIT_EXCEEDED` | `totalRows > 1_000` (Wave 86 E-AC5 — giảm từ 10_000 → 1_000; FE phải chunk client-side nếu file > 1000 dòng) |
| 400 | `FILE_TOO_LARGE` (advice) | Multipart size exceeds 5 MB |
| 400 | `VALIDATION_ERROR` | Row-level validation errors returned INLINE (not thrown) |
| 401 | `UNAUTHENTICATED` | Missing/invalid Bearer token |
| 403 | `FORBIDDEN` | Admin lacks `STUDENT_WRITE` |
| 405 | `METHOD_NOT_ALLOWED` | GET on `/jobs/{id}/errors` |
| 500 | `INTERNAL_ERROR` | Unexpected parse error — should never happen; audit log |

> **Wave 86 E-AC5 (2026-05-16):** `BULK_IMPORT_ROW_LIMIT_EXCEEDED` chuyển HTTP 400 → **413 PAYLOAD_TOO_LARGE** và `MAX_ROWS` giảm `10_000 → 1_000`. Lý do: spec §3 Bucket E E-AC5 mandate cap = 1000 rows/request để align với Phase 1 BETA performance envelope (t3.micro RAM 1GB) + RFC 7231 §6.5.11 semantic (413 = request entity too large, semantically đúng hơn 400 cho row-cap exceeded). FE phải implement client-side chunking nếu user upload file > 1000 dòng.

Row-level errors are NOT thrown — they're collected into the `errors` array of the 2xx response. This matches skip-and-report behavior (BR-BI-032).

## Examples

### Preview — 2 errors

```http
POST /api/v1/students/bulk-import/preview HTTP/1.1
X-Tenant-Id: 11111111-2222-3333-4444-555555555555
Authorization: Bearer ...
Content-Type: multipart/form-data; boundary=...

<file=students.xlsx>
```

```json
200 OK
{
  "success": true,
  "message": "Preview xong",
  "data": {
    "jobId": null,
    "totalRows": 5,
    "successCount": 3,
    "errorCount": 2,
    "errors": [
      { "rowNumber": 3, "field": "email", "message": "Email không hợp lệ" },
      { "rowNumber": 5, "field": "phone", "message": "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)" }
    ]
  }
}
```

### Commit — job id returned

```http
POST /api/v1/students/bulk-import/commit HTTP/1.1
X-Tenant-Id: ...
```

```json
201 Created
Location: /api/v1/students/bulk-import/jobs/42
{
  "success": true,
  "message": "Import hoàn tất",
  "data": {
    "jobId": 42,
    "totalRows": 5,
    "successCount": 3,
    "errorCount": 2,
    "errors": [ /* first 10 */ ]
  }
}
```

## Related

- BR-BI-* rules → [rules.md](./rules.md)
- Use cases UC-BI-01..05 → [use-cases.md](./use-cases.md)
- Reuses BR-STU-001..006 from [student-enrollment/rules.md](../student-enrollment/rules.md)
- OpenAPI group: `Student Bulk Import` (tag on `BulkImportController`)

## Log
- 2026-04-21 — GAP-109: contract documented from `BulkImportController` source. No new endpoints.

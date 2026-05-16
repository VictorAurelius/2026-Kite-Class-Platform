# Student Bulk Import — Use Cases

**Domain:** bulk-import
**Actors:** Tenant admin (browser), `BulkImportController` → `StudentBulkImportService`
**Last verified:** 2026-04-21

## UC-BI-01 — Admin previews a bulk-import xlsx

- **Actor:** Tenant admin
- **Precondition:** Admin authenticated, holds `STUDENT_WRITE` permission, has xlsx ready
- **Trigger:** `POST /api/v1/students/bulk-import/preview` (multipart) with `X-Tenant-Id` header
- **Happy path:**
  1. Client uploads xlsx (≤5 MB, ≤10 000 rows)
  2. `XlsxParser` reads all data rows (row 1 = header)
  3. `RowValidator.validate` runs per row — collects all field errors per row (BR-BI-016)
  4. `detectInFileDuplicates` scans email + phone → flags subsequent occurrences (BR-BI-020/021)
  5. Service returns `BulkImportResult(jobId=null, totalRows, successCount, errorCount, errors[≤10])`
- **FE behavior:** shows success/failed counts + first 10 errors; user clicks "Sửa file" to correct before committing
- **Errors:**
  - Empty file → HTTP 400 `BULK_IMPORT_EMPTY_FILE`
  - >10 000 rows → HTTP 400 `BULK_IMPORT_ROW_LIMIT_EXCEEDED`
  - Parse failure (corrupt xlsx, wrong column order) → HTTP 400 from `BulkImportParseException` handler
- **Postcondition:** zero DB writes; no `BulkImportJob` row persisted

## UC-BI-02 — Admin commits a validated xlsx

- **Actor:** Tenant admin
- **Precondition:** Admin reviewed preview, happy with the delta
- **Trigger:** `POST /api/v1/students/bulk-import/commit` (multipart, same file as preview)
- **Happy path:**
  1. File parsed + row-validated + duplicate-scanned (identical to UC-BI-01)
  2. `BulkImportChunkExecutor.createJob` persists `BulkImportJob` row (status=PENDING)
  3. Duplicate rows added to `skipRowNumbers` set
  4. Rows batched in chunks of 500 (BR-BI-030); each chunk runs in its own transaction (BR-BI-031)
  5. Each chunk: valid rows → `StudentService.create`; invalid rows logged in `allErrors`
  6. After all chunks: `finalizeJob` sets status=COMPLETED + persists counts
  7. Response: HTTP 201 + `Location: /api/v1/students/bulk-import/jobs/{jobId}`
- **FE behavior:** displays final success/failure split; offers "Tải file lỗi" (→ UC-BI-03) if `errorCount > 0`
- **Errors (per row, collected; not aborting batch):**
  - Field validation failures → `RowError(rowNumber, field, message)`
  - In-file duplicate → `"Email trùng với dòng {N} trong cùng file"`
  - DB unique constraint (if in-file detection misses edge case) → row error, chunk continues
- **Postcondition:** valid rows = new `Student` rows tagged with current tenant; invalid rows skipped; `BulkImportJob` row finalized with counts

## UC-BI-03 — Admin downloads the xlsx error report

- **Actor:** Tenant admin
- **Precondition:** A prior commit returned `errorCount > 0`; admin re-uploads the original file
- **Trigger:** `POST /api/v1/students/bulk-import/jobs/{jobId}/errors` (multipart)
- **Happy path:**
  1. Service re-parses + re-validates the file (stateless MVP — see BR-BI-052)
  2. `ErrorReportGenerator` emits xlsx with columns `rowNumber | field | message`
  3. Response streams xlsx as attachment
- **FE behavior:** browser triggers download as `bulk-import-errors-{jobId}.xlsx`
- **Errors:** same 400s as UC-BI-01 (empty / oversized file)
- **Postcondition:** no state change

## UC-BI-04 — System rejects oversized upload

- **Actor:** Tenant admin (anyone posting to bulk-import)
- **Trigger:** File larger than `spring.servlet.multipart.max-file-size` (5 MB)
- **Steps:**
  1. Spring multipart resolver rejects before controller is invoked
  2. `MaxUploadSizeExceededException` caught by global advice → HTTP 400 with code `FILE_TOO_LARGE`
- **FE behavior:** inline error "File quá lớn (tối đa 5MB)"
- **Postcondition:** no side effects

## UC-BI-05 — System rejects row-count overflow

- **Actor:** Any admin
- **Trigger:** xlsx with >1 000 data rows (Wave 86 E-AC5 — giảm từ 10 000)
- **Steps:**
  1. `XlsxParser.parse` returns all rows
  2. `StudentBulkImportService.assertRowLimit` throws `BusinessException("BULK_IMPORT_ROW_LIMIT_EXCEEDED", 413)` — HTTP 413 PAYLOAD_TOO_LARGE
- **FE behavior:** alert "Số dòng vượt quá giới hạn 1000 — vui lòng chia file thành nhiều file ≤ 1000 dòng và upload từng file" + (Phase 1.5+) tự động chunk client-side nếu rowCount > 1000
- **Postcondition:** no side effects, no `BulkImportJob` row

## Notes

- No async / queued processing: commit blocks the HTTP connection until all chunks complete. 1 000 rows × 500-row chunks = 2 round-trips; staged under 10 s in practice (Wave 86 E-AC5 cap = 1000 rows/request thay vì 10 000).
- Rate-limit per admin is NOT enforced at the application layer today — relies on gateway throttling. Track as future enhancement if admins abuse.

## Log
- 2026-05-16 — Wave 86 Bucket E E-AC5: row cap lowered 10_000 → 1_000; HTTP 400 → HTTP 413 PAYLOAD_TOO_LARGE per spec; FE chunk client-side mandate. Same-PR test extended `StudentBulkImportServiceTest#rejectsOverMaxRows` to assert `HttpStatus.PAYLOAD_TOO_LARGE`. Cross-ref api-contract.md row + rules.md BR-BI-003/BR-BI-005.
- 2026-04-21 — GAP-109: UC-BI-01..05 captured from shipped code.

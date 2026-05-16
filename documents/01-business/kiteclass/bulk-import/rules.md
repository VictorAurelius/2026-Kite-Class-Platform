# Student Bulk Import — Business Rules

**Domain:** bulk-import
**Source:** GAP-051 (Wave 1 MVP), PR #332 (parser + chunked commit), PR #338 (in-file duplicate detection), GAP-109 (this 3-layer doc)
**Last verified:** 2026-04-21

Admin-facing bulk create of students via xlsx upload. Two phases: dry-run `preview` (no DB writes) then `commit` (creates valid rows, skips invalid, returns an xlsx error report for rejected rows). Rules below mirror the existing implementation — no new behavior introduced by GAP-109.

## Rules

### File + parsing
| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-BI-001 | Supported format | `.xlsx` only (Apache POI) | `XlsxParser` |
| BR-BI-002 | Max file size | 5 MB (request 6 MB) | `spring.servlet.multipart.max-file-size` in `kiteclass-core/application.yml` |
| BR-BI-003 | Hard upper bound on rows per upload | **1 000 data rows** (Wave 86 E-AC5 — giảm từ 10 000) | `StudentBulkImportService.MAX_ROWS` |
| BR-BI-004 | Header row required | Row 1 = header, data starts row 2 | `XlsxParser` |
| BR-BI-005 | Error code on row limit exceeded | `BULK_IMPORT_ROW_LIMIT_EXCEEDED` + **HTTP 413 PAYLOAD_TOO_LARGE** (Wave 86 E-AC5 — từ HTTP 400) | `StudentBulkImportService.assertRowLimit` |
| BR-BI-006 | Error code on empty upload | `BULK_IMPORT_EMPTY_FILE` + HTTP 400 | `StudentBulkImportService.assertFilePresent` |

### Per-row validation (mirrors `CreateStudentRequest`)
| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-BI-010 | `name` required | 2–100 chars after trim | `RowValidator` |
| BR-BI-011 | `email` required | RFC-ish regex, ≤255 chars | `RowValidator.EMAIL_PATTERN` |
| BR-BI-012 | `phone` optional but regex-enforced when present | `^0\d{9}$` (VN mobile) | `RowValidator.PHONE_PATTERN` |
| BR-BI-013 | `dateOfBirth` optional | `dd/MM/yyyy`, past-or-present | `RowValidator.DATE_FORMATTER` |
| BR-BI-014 | `gender` optional | case-insensitive MALE/FEMALE (OTHER rejected until `Gender` enum extended) | `RowValidator.parseGender` |
| BR-BI-015 | `address` optional | ≤1000 chars | `RowValidator` |
| BR-BI-016 | Collect ALL field errors per row | Do not short-circuit on first failure | `RowValidator.validate` |
| BR-BI-017 | Field-level error messages in Vietnamese | user-facing | `RowValidator` |

### Duplicate detection
| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-BI-020 | In-file duplicate detection — email | case-insensitive; 1st occurrence wins; subsequent rows flagged | `StudentBulkImportService.detectInFileDuplicates` |
| BR-BI-021 | In-file duplicate detection — phone | exact match; 1st occurrence wins | `StudentBulkImportService.detectInFileDuplicates` |
| BR-BI-022 | DB-level uniqueness | Reuses BR-STU-002 (email unique per tenant) + BR-STU-003 (phone unique global) | `student-enrollment/rules.md` + `StudentRepository` unique constraints |
| BR-BI-023 | In-file dup check runs BEFORE DB commit | Prevents 500 from unique-constraint violation | `StudentBulkImportService.commit` |
| BR-BI-024 | Error message format | `"Email trùng với dòng {N} trong cùng file"` / phone equivalent | `StudentBulkImportService.detectInFileDuplicates` |

### Atomicity + chunking
| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-BI-030 | Chunk size | 500 rows per transaction | `StudentBulkImportService.CHUNK_SIZE` |
| BR-BI-031 | Partial success | Failed chunk does NOT roll back earlier chunks (skip-and-report) | `BulkImportChunkExecutor` |
| BR-BI-032 | Per-row errors do NOT abort batch | Invalid rows skipped, valid siblings persisted | `BulkImportChunkExecutor.processChunk` |
| BR-BI-033 | Duplicate rows skipped at insert | `detectInFileDuplicates` output drives `skipRowNumbers` before chunk runs | `StudentBulkImportService.commit` |

### Tenant isolation + audit
| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-BI-040 | Tenant tagging | Every row uses `X-Tenant-Id` header value; no row falls through to another tenant | `BulkImportController` + `BaseEntity.instanceId` |
| BR-BI-041 | Job persistence | Every commit creates `student_bulk_import_jobs` row for audit (regardless of outcome) | `BulkImportChunkExecutor.createJob` + `finalizeJob` |
| BR-BI-042 | Job status enum | `PENDING → IN_PROGRESS → COMPLETED / FAILED` | `BulkImportStatus` |
| BR-BI-043 | Job fields | filename, totalRows, successCount, errorCount, completedAt | `BulkImportJob` |

### Reporting
| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-BI-050 | Inline error truncation | First 10 errors inline in response body | `BulkImportResult.MAX_RETURNED_ERRORS` |
| BR-BI-051 | Full error report | Separate endpoint generates xlsx listing every rejected row | `ErrorReportGenerator` + `BulkImportController.downloadErrors` |
| BR-BI-052 | Stateless error download (MVP) | Client re-uploads original file to regenerate report | `BulkImportController.downloadErrors` |
| BR-BI-053 | Error report columns | rowNumber, field, message | `ErrorReportGenerator` |

### Out of scope (Wave 1 MVP)
- Rate limiting per admin (deferred — feature flag only at `student.bulk-import.*` scope)
- Rollback after commit (audit trail persists; manual delete required)
- Async processing (>10k rows today would be rejected — future GAP extends)
- Email notification on completion (out of scope, admin sees inline result)

## Config keys

| Key | Value | Purpose |
|-----|-------|---------|
| `spring.servlet.multipart.max-file-size` | 5MB | File upload cap (BR-BI-002) |
| `spring.servlet.multipart.max-request-size` | 6MB | Total multipart request cap |

No domain-level feature flag today — `StudentBulkImportService.MAX_ROWS` + `CHUNK_SIZE` are code constants.

## Related domains

- **student-enrollment** — BR-STU-001..006 rules reused per row (BR-BI-010..015)
- **security-foundation** — X-Tenant-Id header required (BR-BI-040)
- **audit** — `student_bulk_import_jobs` table powers admin history UI

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — PDPL 2023 Art 11 (consent for student/parent data import); Luật Giáo dục 2019 (student record handling).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: PDPL implementing-decree, MoET data-import regulation update.

## Log
- 2026-04-21 — GAP-109: 3-layer docs created capturing shipped Wave 1 behavior. No code change.
- 2026-04-14 — Feature delivered via GAP-051 (PR #332) + in-file duplicate fix (PR #338); docs never backfilled until today.

# GAP-051: Bulk Import Users via xlsx/CSV

**Status:** 🟢 DONE (Wave 1 MVP — 2026-04-17)
**Priority:** 🔴 P0 (school persona blocker)
**Domain:** Backend / Frontend / Product
**Detected:** 2026-04-14 (user-raised example)
**Persona blocked:** P5 K-12 School, P3 Medium Center, P4 Chain

## Wave 1 MVP — Implementation Notes (2026-04-17)

Shipped on `wave/1-bulk-import`:

- `kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/` — new
  package with entity, repository, dto, service, and controller layers.
- `POST /api/v1/students/bulk-import/preview` — dry-run, no DB writes.
- `POST /api/v1/students/bulk-import/commit` — parse + validate + create;
  returns `jobId` + `Location` header; persists `BulkImportJob` audit row.
- `POST /api/v1/students/bulk-import/jobs/{id}/errors` — stream xlsx error
  report (stateless MVP: caller re-uploads original file).
- Flyway `V41__create_student_bulk_import_job.sql` adds the jobs table.
- Spring multipart limit raised to 5 MB; hard row cap = 10 000.
- Per-chunk transactions via `BulkImportChunkExecutor` (REQUIRES_NEW,
  500-row chunks) so one invalid chunk cannot roll back others.
- Deps added: Apache POI 5.2.5 (`poi-ooxml`), OpenCSV 5.9.
- Tests: `XlsxParserTest` (5), `RowValidatorTest` (13),
  `StudentBulkImportServiceTest` (7), `StudentBulkImportIT` (2, Testcontainers).

MVP **does not** include: auto-assign to classes, welcome emails, undo
within 24 h, teacher/parent/staff variants, >100-row async progress —
these remain open for a follow-up wave and are tracked below.

## Problem

**Không có bulk import** → tenant với nhiều users phải tạo thủ công:

**User scenario:**
> Trường cấp 3, 500 học sinh đăng ký đầu năm. Hiện tại:
> 1. 500 học sinh tự đăng ký tài khoản → ~2-4 tuần
> 2. Gửi credentials cho giáo viên
> 3. Giáo viên manually assign từng học sinh vào class
> 4. Vỡ vụn nghiệp vụ, delay cả năm học

**Expected:**
1. Admin upload xlsx (500 rows)
2. System auto-create accounts + send welcome emails
3. System auto-assign students to classes (based on class column)
4. School ready trong vài giờ, không phải tuần

## Proposed Fix

### 1. Upload UI

`/admin/users/import` page:
- Download template xlsx (with column headers)
- Upload filled xlsx
- Preview parsed data với validation warnings
- Confirm → process

### 2. XLSX Template

**students-import-template.xlsx:**
| fullName | email | phoneNumber | dateOfBirth | gender | parentEmail | className | status |
|----------|-------|-------------|-------------|--------|-------------|-----------|--------|
| Nguyễn Văn A | an@...  | 090... | 2010-05-15 | MALE | parent@... | 10A1 | ACTIVE |

Required: fullName + email
Optional: rest
Validation: email unique, class exists, date format, gender enum

### 3. Backend processing

```java
@PostMapping("/api/v1/admin/students/bulk-import")
public BulkImportResult importStudents(@RequestParam MultipartFile file) {
  // 1. Parse xlsx (Apache POI — reuse từ GAP-047)
  // 2. Validate each row
  // 3. Duplicate detection
  // 4. Batch create accounts
  // 5. Send welcome emails (async via queue)
  // 6. Auto-assign to classes if className provided
  // 7. Return summary: created/skipped/errors
}
```

### 4. Design Pattern

Use **Saga Pattern** (per design-patterns.md) cho batch:
- Step 1: validate all rows
- Step 2: create accounts (compensate: delete if later step fails)
- Step 3: assign classes
- Step 4: send emails

### 5. Error Handling

```json
BulkImportResult:
{
  "totalRows": 500,
  "created": 487,
  "skipped": 10, // duplicates
  "errors": [
    { "row": 23, "field": "email", "error": "Invalid format" },
    { "row": 45, "field": "className", "error": "Class 10X1 not found" }
  ],
  "downloadErrorReport": "/api/v1/admin/imports/{id}/errors.xlsx"
}
```

Admin download errors xlsx → fix → re-upload only failed rows.

### 6. Teacher + Staff Variants

Similar endpoints:
- `/api/v1/admin/teachers/bulk-import`
- `/api/v1/admin/staff/bulk-import`
- `/api/v1/admin/parents/bulk-import` (linked to students)

### 7. Undo / Rollback

```
/admin/imports/history
- Import 2026-04-14 09:00 — 487 students created
  [View details] [Download report] [Rollback (within 24h)]
```

### 8. Progress Tracking

Large imports (>100 rows) async:
- Upload → jobId
- UI shows progress bar
- Email notification khi complete

## Acceptance Criteria

- [ ] xlsx template generated + downloadable
- [ ] Upload UI với preview
- [ ] Validation errors shown per row
- [ ] Batch create accounts (handle 500+ efficiently)
- [ ] Auto-assign to classes (if className column present)
- [ ] Welcome emails sent (via queue, not blocking)
- [ ] Error report xlsx downloadable
- [ ] Undo/rollback within 24h
- [ ] Async processing for >100 rows với progress
- [ ] Duplicate detection (email unique per tenant)
- [ ] Works for students, teachers, parents, staff (4 endpoints)
- [ ] Load test: import 1000 rows trong <2 min
- [ ] Integration test: school 500-student scenario end-to-end
- [ ] Documentation: template xlsx, column definitions

## Dependencies

- GAP-047 (document generation skills) — Apache POI cho xlsx parsing
- GAP-002 (async pipeline) — queue cho email sending
- GAP-058 (role hierarchy) — parent linking

## Impact Measurement

Before fix:
- School onboarding: 2-4 weeks (blocked)
- Support tickets: high (manual setup issues)

After fix:
- School onboarding: 1 day
- Eliminates #1 persona blocker for K-12 market

## Log

- 2026-04-14 — User raised as canonical example of missing business logic review
- 2026-04-17 — Wave 1 MVP shipped on `wave/1-bulk-import`:
  xlsx parse + validate + batch-create + audit job row + error-report xlsx.
  Async/email/class-assign deferred to follow-up wave.

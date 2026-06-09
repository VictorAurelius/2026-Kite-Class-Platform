# GAP-1104: Ghi danh hàng loạt vào lớp qua xlsx + template tải về

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-10 (Wave KC enrollment — không có bulk-enroll endpoint/UI/template)
**Affects:** `kiteclass-core` module `enrollment/bulkimport/**` + `kiteclass-frontend` `lib/api/enrollment-bulk.ts`, `app/(dashboard)/classes/[id]/bulk-enroll/page.tsx`, `components/enrollment/bulk-enroll-panel.tsx`

## Problem

Chỉ có single-enroll (1 học sinh/lần). Trung tâm khai giảng lớp mới cần ghi danh hàng chục học sinh — không có endpoint/UI/template bulk. Student bulk-import (GAP-051/GAP-137) đã có pattern xlsx nhưng chưa có cho enrollment.

## Proposed Fix

Mirror student bulk-import module: BE module `enrollment/bulkimport/` (XlsxParser + TemplateGenerator + Service + Controller) resolve học sinh (email→phone) + lớp (class_code) tenant-scoped rồi delegate `EnrollmentService.enrollStudent` skip-and-report từng dòng; FE page bulk-enroll (tải template + upload + preview + commit). class_code trong file là canonical (mỗi dòng ghi danh vào lớp theo class_code).

## Acceptance Criteria

- [x] BE module `enrollment/bulkimport/`: `EnrollmentXlsxParser` (header `student_email|student_phone|class_code|tuition_amount|discount_percent|note`, required class_code + email/phone) + `EnrollmentTemplateGenerator` (sheet GhiDanh + HuongDan) + `EnrollmentBulkImportService` (preview/commit, MAX_ROWS 1000, in-file dup, xlsx-only guard) + `EnrollmentBulkImportController`
- [x] 3 endpoint: `GET /template` (no X-Tenant-Id), `POST /preview` (X-Tenant-Id), `POST /commit` (X-Tenant-Id → 201)
- [x] Resolve student tenant-scoped (`findByEmailAndInstanceIdAndDeletedFalse` + new `findByPhoneAndInstanceIdAndDeletedFalse`) + class (`findByClassCodeAndInstanceIdAndDeletedFalse`)
- [x] FE `lib/api/enrollment-bulk.ts` (downloadTemplate/preview/commit) + bulk-enroll page (tải template + upload + preview + commit, mirror admin/bulk-import UX)
- [x] BE tests: `EnrollmentXlsxParserTest` (7) + `EnrollmentTemplateGeneratorTest` (4) + `EnrollmentBulkImportControllerTest` GET /template (1) = 12 PASS
- [x] FE test: bulk-enroll panel renders + download template + preview→commit flow (3 PASS)
- [ ] Runtime-walk pending coordinator (browser: tải template → điền → upload → preview lỗi/hợp lệ → commit → roster refresh; verify MAX_ROWS 413 + already-enrolled per-row)

## Related

- Discovered in: Wave KC enrollment build 2026-06-10
- Sister GAP-1103 (single-enroll dialog)
- Mirrors student bulk-import (GAP-051 BE / GAP-137 FE)
- BE single-enroll reused: `EnrollmentService.enrollStudent` (UC-STU-05, UC-STU-10/11)

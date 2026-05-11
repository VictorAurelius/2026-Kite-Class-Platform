# GAP-109: Student Bulk-Import Rules Undocumented

**Status:** 🟢 DONE (Wave 9-D, 2026-04-21)
**Priority:** 🟠 P1
**Domain:** KiteClass / Student Enrollment / Business Docs
**Found:** 2026-04-19 (business-logic audit)
**Affects:** kiteclass-core student/bulkimport module, admin UI integration, audit traceability

## Problem

Wave 1 GAP-051 đã ship `StudentBulkImportService` (PR #332) + in-file duplicate detection fix (PR #338). Code flow:
- `kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/service/StudentBulkImportService.java`
- Test: `StudentBulkImportServiceTest.java`

Nhưng `documents/01-business/kiteclass/student-enrollment/rules.md` CHỈ có BR-STU-* (6 rules) + BR-ENROLL-* (6 rules), **ZERO** rules cho bulk-import:

```
$ grep -n "BR-BULK\|bulk.import\|CSV" documents/01-business/kiteclass/student-enrollment/rules.md
# 0 hits
```

Rules implicit trong code nhưng không documented:
- Max rows per import (có enforce? nếu có giá trị nào?)
- Duplicate policy: reject whole batch, skip duplicates, hoặc partial import?
- In-file duplicate: PR #338 fix specifically — code detect trước khi DB check (rule documented ở đâu?)
- Tenant isolation: bulk import có enforce instanceId per row?
- Error reporting: per-row error vs transaction-level rollback?
- Atomicity: all-or-nothing hay partial success?
- Email validation: same rule như single-create (BR-STU-002)?
- Rate limit: có limit imports per minute?

## Root Cause

GAP-051 shipped với scope focus on Wave 1 delivery (imports work). Docs update slipped qua. PR #338 thêm in-file duplicate detection but `fix(bulk-import): detect in-file duplicates before DB` commit message + PR body focus on bug fix, không update rules.md.

Living Docs rule violation same pattern như GAP-104 (fair-queue) + GAP-105 (parent-portal) — feature shipped faster than docs can catch up.

## Proposed Fix

Thêm section mới trong `documents/01-business/kiteclass/student-enrollment/rules.md`:

```markdown
### Bulk Import (Wave 1, GAP-051)

| ID | Rule | Detail |
|----|------|--------|
| BR-BULK-001 | CSV format | Header row required, UTF-8 encoding, comma delimiter |
| BR-BULK-002 | Max rows per import | `student.bulk-import.max-rows` config (default 1000) |
| BR-BULK-003 | In-file duplicate detection | Check email/phone duplicates trong CSV TRƯỚC khi query DB |
| BR-BULK-004 | DB-level uniqueness | Reuse BR-STU-002 (email unique per tenant) + BR-STU-003 (phone unique global) |
| BR-BULK-005 | Duplicate policy | Skip duplicates, continue import; report in ImportResult |
| BR-BULK-006 | Per-row validation | Name (BR-STU-001), email format, phone format |
| BR-BULK-007 | Atomicity | Per-row transaction (failed row không rollback successful rows) |
| BR-BULK-008 | Error report | ImportResult includes rowNumber + errorMessage per failed row |
| BR-BULK-009 | Tenant isolation | All rows tagged với current TenantContext.getInstanceId() |
| BR-BULK-010 | Audit log | `student.bulk_import` actionType per batch (rows imported + rows failed) |
```

Thêm use-case UC-BULK-01 vào `use-cases.md`. Config key `student.bulk-import.max-rows` vào `application.yml`.

## Acceptance Criteria
- [ ] `student-enrollment/rules.md` có ≥8 BR-BULK-* rules
- [ ] Mỗi rule có code reference pointer
- [ ] `use-cases.md` UC-BULK-01 "Admin bulk import students via CSV" với happy path + 3 errors
- [ ] `api-contract.md` document endpoint `POST /api/v1/students/bulk-import` với multipart/CSV + ImportResult DTO
- [ ] Config key `student.bulk-import.max-rows` exist trong application.yml (nếu rule keep rule)
- [ ] Rules.md "Log" section reference 2026-04-19 audit + GAP-109 fix

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Original feature: GAP-051 (Wave 1 bulk import)
- Related PRs: #332 (bulk import MVP), #338 (in-file duplicate fix)
- Pattern echoes: GAP-104 (fair-queue undocumented), GAP-105 (parent-portal no 3-layer)

## Log
- 2026-04-21 (Wave 9-D) — Closed. Created new 3-layer domain folder `documents/01-business/kiteclass/bulk-import/` with:
  - `rules.md` — 30+ BR-BI-* rules covering file/parsing/per-row-validation/duplicate-detection/atomicity/tenant isolation/reporting + code references per rule
  - `use-cases.md` — UC-BI-01..05 (preview, commit, error download, oversize reject, row-limit reject)
  - `api-contract.md` — full contract for `POST /preview`, `POST /commit`, `POST /jobs/{id}/errors` including xlsx schema, error codes, examples
  
  Docs capture shipped Wave 1 behavior (no code change). Placed under its own domain rather than extending `student-enrollment/rules.md` — cleaner separation, matches 3-layer contract per CLAUDE.md.

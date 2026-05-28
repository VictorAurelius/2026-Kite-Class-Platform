---
audience: dev
---

# GAP-799 — Cross-tenant uniqueness leak: course-code + student-phone service checks not tenant-scoped

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Security (multi-tenancy isolation) — kiteclass-core
**Found:** 2026-05-28 (seed-script live walk — `scripts/seed-sky-education-demo.sh` build)
**Phase:** phase-1-beta
**Affects:** Course creation (`CourseServiceImpl`), Student creation + update (`StudentServiceImpl`), bulk import (`BulkImportChunkExecutor`)

## Problem

Service-layer uniqueness pre-checks for **course code** and **student phone** are **global (not tenant-scoped)**, while the corresponding DB constraints ARE tenant-scoped. A code/phone already used by tenant A blocks tenant B from using it — a cross-tenant collision + enumeration leak.

**Empirical evidence (live, local stack, 2026-05-28):**
- Course code `IELTS-RW01` created in tenant `877dff9d` (prior re-walk) → creating the SAME code in freshly-provisioned tenant `e8ff87e1` returned `COURSE_CODE_EXISTS` (HTTP 409). The fresh tenant had zero courses.
- Student phone `0987654321` used in one tenant → blocked in a different freshly-provisioned tenant with `STUDENT_PHONE_EXISTS`.

Both reproduced reliably while building `scripts/seed-sky-education-demo.sh` (worked around by suffixing codes/phones per run).

## Root Cause

Repository existence checks lack an `instance_id` (tenant) filter:

- `CourseRepository.existsByCodeAndDeletedFalse(String code)` — used `CourseServiceImpl:94` → throws `COURSE_CODE_EXISTS` if ANY tenant has the code.
- `StudentRepository.existsByPhoneAndDeletedFalse(String phone)` — used `StudentServiceImpl:67` (create) + `:170` (update) + `BulkImportChunkExecutor:114` → global phone check.

DB constraints are correctly tenant-scoped: `uk_courses_instance_code UNIQUE (instance_id, code)` (V1__create_core_schema.sql:132). So the service pre-check is **stricter than the DB** and blocks legitimate cross-tenant reuse.

**Open investigation point:** kiteclass-core uses RLS tenant isolation (V34-era). Either (a) RLS GUC is not set when these `existsBy*` queries run, OR (b) these queries bypass RLS. The empirical collision proves RLS did NOT prevent it — confirm whether the fix is "add explicit `instance_id` filter to the repository methods" vs "ensure RLS context is set on these read paths".

## Impact

- **Correctness:** two centers cannot both have a course `IELTS-RW01` or two students sharing a parent's phone (`0987654321`) — realistic in VN edu (siblings, shared parent phone).
- **Info leak (enumeration):** tenant B can probe whether tenant A uses a given code/phone via the 409 response — minor cross-tenant disclosure.

## Proposed Fix

1. Add tenant-scoped variants: `existsByCodeAndInstanceIdAndDeletedFalse(code, instanceId)` + `existsByPhoneAndInstanceIdAndDeletedFalse(phone, instanceId)` (or rely on RLS if confirmed it should apply — investigate first per §Root Cause open point).
2. Update `CourseServiceImpl:94`, `StudentServiceImpl:67/:170`, `BulkImportChunkExecutor` to pass current tenant id.
3. Testcontainers IT: tenant A creates code/phone X; tenant B creates the SAME X → PASS (no collision). Cross-tenant isolation regression-guard.

## Acceptance Criteria

- [ ] Investigate RLS-should-apply vs explicit-filter; document decision
- [ ] Course code uniqueness scoped to `instance_id` (service + repo)
- [ ] Student phone uniqueness scoped to `instance_id` (create + update + bulk import)
- [ ] Testcontainers IT: same code/phone in 2 tenants both succeed
- [ ] No enumeration leak: tenant B 409 cannot reveal tenant A data

## Related

- Surfaced building `scripts/seed-sky-education-demo.sh` (PR #1952)
- `audit-to-gap-pipeline.md` §1 (discovery → gap)
- `postgres-specific-type-testcontainers.md` (Testcontainers IT mandate for the regression-guard)
- RLS migration family (V34 enable_rls_tenant_scoped_tables — subscription side; kiteclass-core RLS separate)

## Log

- **2026-05-28:** Filed from seed-script live walk. Empirical cross-tenant collision (course code `IELTS-RW01` + student phone `0987654321`) reproduced reliably. Root cause confirmed: `existsByCodeAndDeletedFalse` / `existsByPhoneAndDeletedFalse` lack `instance_id` filter; DB constraint `uk_courses_instance_code` IS tenant-scoped. RLS-vs-explicit-filter resolution flagged for investigation.

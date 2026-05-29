---
audience: dev
---

# GAP-799 — Cross-tenant uniqueness leak: course-code + student-phone service checks not tenant-scoped

**Status:** 🟢 DONE (2026-05-28, PR #1954 — fix + cross-tenant regression guards + live re-walk)
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

- [x] Investigate RLS-should-apply vs explicit-filter; document decision → **explicit `instance_id` filter** (shared `kiteclass_shared` DB + app-layer filtering is the isolation mechanism; Hibernate `tenantFilter` not applied to derived `existsBy`; RLS is secondary net, app role not effectively isolated on this path)
- [x] Course code uniqueness scoped to `instance_id` (service + repo) — `existsByCodeAndInstanceIdAndDeletedFalse` + `CourseServiceImpl`
- [x] Student phone uniqueness scoped to `instance_id` (create + update + bulk import) — `existsByPhoneAndInstanceIdAndDeletedFalse` + `StudentServiceImpl` (bulk via `createStudent`)
- [x] Testcontainers IT: same code/phone in 2 tenants both succeed — `CourseRepositoryTest` + `StudentRepositoryTest` cross-tenant guards (gated `INTEGRATION_TEST=true`)
- [x] No enumeration leak: tenant B reuse returns false → no 409 disclosure of tenant A data

## Walk evidence (live re-walk per pre-handoff-self-test-completeness.md §3, 2026-05-28)

Rebuilt kiteclass-core with fix; 2 seeded tenants (A=164019, B=163924) on gateway :9000:

| Case | HTTP | Verdict |
|---|---|---|
| A creates course code `DUPTEST-799` | 201 | ✅ |
| B creates SAME code `DUPTEST-799` (cross-tenant) | 201 | ✅ FIX (was 409) |
| A creates `DUPTEST-799` again (same-tenant dup) | 409 | ✅ no regression |
| A creates student phone `0911111799` | 201 | ✅ |
| B creates SAME phone `0911111799` (cross-tenant) | 201 | ✅ FIX (was 409) |
| A creates phone `0911111799` again (same-tenant dup) | 409 | ✅ no regression |

Originating symptom resolved + same-tenant uniqueness preserved.

## Related

- Surfaced building `scripts/seed-sky-education-demo.sh` (PR #1952)
- `audit-to-gap-pipeline.md` §1 (discovery → gap)
- `postgres-specific-type-testcontainers.md` (Testcontainers IT mandate for the regression-guard)
- RLS migration family (V34 enable_rls_tenant_scoped_tables — subscription side; kiteclass-core RLS separate)

## Log

- **2026-05-28:** Filed from seed-script live walk. Empirical cross-tenant collision (course code `IELTS-RW01` + student phone `0987654321`) reproduced reliably. Root cause confirmed: `existsByCodeAndDeletedFalse` / `existsByPhoneAndDeletedFalse` lack `instance_id` filter; DB constraint `uk_courses_instance_code` IS tenant-scoped. RLS-vs-explicit-filter resolution flagged for investigation.
- **2026-05-28 (DONE, PR #1954):** Investigation resolved — single shared `kiteclass_shared` DB + app-layer `instance_id` filtering is the isolation mechanism (RLS secondary net, not effective on this path); Hibernate `tenantFilter` not applied to derived `existsBy` → explicit `instance_id` predicate required. Fix: added `existsByCodeAndInstanceIdAndDeletedFalse` + `existsByPhoneAndInstanceIdAndDeletedFalse` (deprecated globals), scoped checks in `CourseServiceImpl` + `StudentServiceImpl` (create + update; bulk via `createStudent`). Cross-tenant regression-guard tests added. Live re-walk PASS (6/6 — see §Walk evidence): cross-tenant reuse 201 (was 409), same-tenant dup 409. Mirrors existing `existsByEmailAndInstanceId` precedent.

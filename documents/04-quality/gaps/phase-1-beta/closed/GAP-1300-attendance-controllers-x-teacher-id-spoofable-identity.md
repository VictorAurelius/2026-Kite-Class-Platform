---
audience: dev
---

# GAP-1300 — Attendance controllers trust client `X-Teacher-Id` as recording-teacher identity (spoofable)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend / Security (authz)
**Found:** 2026-06-14 (cross-flow sweep of GAP-1299 — same bug class)
**Closed:** 2026-06-14 (PR `fix/gap-1300-1301-xteacherid-spoof`)
**Affects:** `AttendancePeriodController` (2 sites: `upsertBatch`, `update`), `AttendanceController` (2 sites), `AttendanceClassBatchController` (1 site)

## Problem

Same bug class as GAP-1299: these controllers read the **client-supplied `X-Teacher-Id` `@RequestHeader`** as the recording-teacher actor identity (`recordedBy` / GVCN). The gateway does NOT control/strip `X-Teacher-Id` (only `X-User-Id`/`X-User-Roles`/`X-User-Reference-Id`/`X-Tenant-Id` per GAP-814), so a caller can attribute attendance records to an arbitrary teacher by setting the header. Surfaced by the GAP-1299 cross-flow sweep (`grep '@RequestHeader("X-Teacher-Id")'`).

Severity note: needs per-site triage — some attendance endpoints may already have a `@PreAuthorize`/`@authz` ownership gate that bounds the role-escalation dimension, but the **attribution** dimension (recordedBy = spoofed teacher) remains.

## Proposed Fix

Mirror GAP-1299 / GAP-1000: derive the recording teacher from the authenticated principal (`UserContext.getCurrentReferenceId()` ← gateway `X-User-Reference-Id`), drop `X-Teacher-Id` as an identity source; add/confirm `@PreAuthorize` role gate. Sweep callers (incl. test headers) per `api-contract-change-caller-sweep.md`.

## Acceptance Criteria

- [x] Attendance recording-teacher id derived from token (`X-User-Reference-Id`), not client `X-Teacher-Id`
- [x] Role gate present on attendance write endpoints (STUDENT/PARENT blocked)
- [x] Tests: spoofed `X-Teacher-Id` ignored; happy path + ADMIN/OWNER preserved; `./mvnw test` green

## Resolution (2026-06-14)

Mirrored GAP-1299. All 5 attendance write sites fixed:

- **`AttendancePeriodController`** `upsertBatch` + `update` — added role gate `@PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")`; `recordedBy` now derived from `UserContext.getCurrentReferenceId()` (gateway `X-User-Reference-Id`); dropped `@RequestHeader("X-Teacher-Id")`.
- **`AttendanceController`** `markBulkAttendance` (kept per-resource `@authz.hasAccessToClass`) + `updateAttendanceStatus` (added role gate) — teacher/`markedBy` from token; `X-Teacher-Id` dropped. `AttendanceServiceImpl` adds ADMIN/OWNER bypass of the MAIN_TEACHER check via `AuthorizationBean.isAdmin()` (markedBy nullable).
- **`AttendanceClassBatchController`** `saveClassBatch` (kept per-resource `@authz.hasAccessToClass`) — recording teacher from token.

**Design note (documented, not a guess):** `attendance_period.recorded_by` is NOT NULL, so per-period writes are realistically performed by a TEACHER carrying a numeric reference id; OWNER/ADMIN/STAFF (no reference id) pass the role gate for defense-in-depth but do not record per-period attendance in normal operation (no privilege escalation — the spoof is closed). The mark/update paths (`marked_by` nullable) DO support the ADMIN/OWNER service bypass.

**Tests (full `kiteclass-core` suite green, strict-warnings — 1737 run / 0 fail / 0 error):**
- `AttendanceAuthzTest` (`@WebMvcTest`) — STUDENT/PARENT → 403 (role gate, service not invoked); spoofed `X-Teacher-Id` ignored → service invoked with token reference id (not the spoofed value).
- `AttendanceServiceTest` — added `shouldUpdateAttendanceStatus_whenAdminBypassesOwnership` (ADMIN/OWNER bypass MAIN_TEACHER) + `@Mock AuthorizationBean authz` (caller-sweep per `api-contract-change-caller-sweep.md`).
- `AttendanceClassBatchControllerIT` migrated: `X-Teacher-Id` dropped; `UserContext` seeded; "missing header" test repurposed → spoof-ignored test.
- `AttendancePeriodIntegrationTest` + `AttendanceIntegrationTest` header migrated `X-Teacher-Id` → `X-User-Reference-Id`.

## Related

- Discovered in: GAP-1299 cross-flow sweep (PR `fix/gap-1299-lms-authoring-authz`)
- Fixed in: PR `fix/gap-1300-1301-xteacherid-spoof` (2026-06-14)
- GAP-1299 (LMS authoring — same class, fixed), GAP-1000 (grade finalize precedent), GAP-798 (umbrella actor↔owner bridge), GAP-814 (gateway-controlled headers), GAP-1301 (assignment/grade — same wave)

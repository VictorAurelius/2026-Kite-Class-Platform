---
audience: dev
---

# GAP-1300 — Attendance controllers trust client `X-Teacher-Id` as recording-teacher identity (spoofable)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Security (authz)
**Found:** 2026-06-14 (cross-flow sweep of GAP-1299 — same bug class)
**Affects:** `AttendancePeriodController` (2 sites: `upsertBatch`, `update`), `AttendanceController` (2 sites), `AttendanceClassBatchController` (1 site)

## Problem

Same bug class as GAP-1299: these controllers read the **client-supplied `X-Teacher-Id` `@RequestHeader`** as the recording-teacher actor identity (`recordedBy` / GVCN). The gateway does NOT control/strip `X-Teacher-Id` (only `X-User-Id`/`X-User-Roles`/`X-User-Reference-Id`/`X-Tenant-Id` per GAP-814), so a caller can attribute attendance records to an arbitrary teacher by setting the header. Surfaced by the GAP-1299 cross-flow sweep (`grep '@RequestHeader("X-Teacher-Id")'`).

Severity note: needs per-site triage — some attendance endpoints may already have a `@PreAuthorize`/`@authz` ownership gate that bounds the role-escalation dimension, but the **attribution** dimension (recordedBy = spoofed teacher) remains.

## Proposed Fix

Mirror GAP-1299 / GAP-1000: derive the recording teacher from the authenticated principal (`UserContext.getCurrentReferenceId()` ← gateway `X-User-Reference-Id`), drop `X-Teacher-Id` as an identity source; add/confirm `@PreAuthorize` role gate. Sweep callers (incl. test headers) per `api-contract-change-caller-sweep.md`.

## Acceptance Criteria

- [ ] Attendance recording-teacher id derived from token (`X-User-Reference-Id`), not client `X-Teacher-Id`
- [ ] Role gate present on attendance write endpoints (STUDENT/PARENT blocked)
- [ ] Tests: spoofed `X-Teacher-Id` ignored; happy path + ADMIN/OWNER preserved; `./mvnw test` green

## Related

- Discovered in: GAP-1299 cross-flow sweep (PR `fix/gap-1299-lms-authoring-authz`)
- GAP-1299 (LMS authoring — same class, fixed), GAP-1000 (grade finalize precedent), GAP-798 (umbrella actor↔owner bridge), GAP-814 (gateway-controlled headers)

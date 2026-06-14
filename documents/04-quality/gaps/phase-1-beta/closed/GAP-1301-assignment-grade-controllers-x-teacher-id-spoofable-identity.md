---
audience: dev
---

# GAP-1301 — Assignment + Grade controllers trust client `X-Teacher-Id` as actor identity (spoofable)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend / Security (authz)
**Found:** 2026-06-14 (cross-flow sweep of GAP-1299 — same bug class)
**Closed:** 2026-06-14 (PR `fix/gap-1300-1301-xteacherid-spoof`)
**Affects:** `AssignmentController` (7 sites: create/update/delete/publish/etc.), `GradeController.deleteComponent` (1 site)

## Problem

Same bug class as GAP-1299: these controllers read the **client-supplied `X-Teacher-Id` `@RequestHeader`** as the actor identity passed to the service (teacher attribution / ownership input). The gateway does NOT control/strip `X-Teacher-Id` (GAP-814) → spoofable. Surfaced by the GAP-1299 cross-flow sweep (`grep '@RequestHeader("X-Teacher-Id")'`).

Severity note: `GradeController.deleteComponent` already has `@PreAuthorize("@authz.hasAccessToGradeComponent(#id)")` (resource ownership bounds WHO can act), but still forwards the spoofable `X-Teacher-Id` for attribution. `AssignmentController` sites need per-site triage for both role-gate and identity-source.

## Proposed Fix

Mirror GAP-1299 / GAP-1000: derive the acting teacher from the authenticated principal (`UserContext.getCurrentReferenceId()` ← gateway `X-User-Reference-Id`), drop `X-Teacher-Id` as identity source; add/confirm `@PreAuthorize` role gate. Sweep callers (incl. test headers) per `api-contract-change-caller-sweep.md`.

## Acceptance Criteria

- [x] Assignment + grade-component acting teacher id derived from token (`X-User-Reference-Id`), not client `X-Teacher-Id`
- [x] Role gate present on assignment write endpoints (STUDENT/PARENT blocked)
- [x] Tests: spoofed `X-Teacher-Id` ignored; happy path + ADMIN/OWNER preserved; `./mvnw test` green

## Resolution (2026-06-14)

Mirrored GAP-1299. All 8 sites fixed:

- **`AssignmentController`** (7 sites: create / update / publish / close / delete / gradeSubmission / returnSubmission) — added role gate `@PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")`; acting teacher from `UserContext.getCurrentReferenceId()` (gateway `X-User-Reference-Id`); dropped `@RequestHeader("X-Teacher-Id")` (kept the `X-User-Id` student-submit header). `AssignmentServiceImpl.createAssignment` + `validateTeacherPermission` add ADMIN/OWNER bypass via `AuthorizationBean.isAdmin()`.
- **`GradeController.deleteComponent`** (1 site) — kept the existing `@PreAuthorize("@authz.hasAccessToGradeComponent(#id)")` resource gate; acting teacher now from token, `X-Teacher-Id` dropped. `GradeServiceImpl.validateTeacherPermission` switched from a private ADMIN/PLATFORM_ADMIN-only check to the shared **OWNER-inclusive** `AuthorizationBean.isAdmin()` (so an OWNER that passes the `@authz` controller gate is not then rejected at the service layer). Removed the now-redundant private `isAdmin()` + its `Authentication`/`SecurityContextHolder` imports.

**Tests (full `kiteclass-core` suite green, strict-warnings — 1737 run / 0 fail / 0 error):**
- `AssignmentAuthzTest` (`@WebMvcTest`) — STUDENT/PARENT → 403 (role gate, service not invoked); spoofed `X-Teacher-Id` ignored → service invoked with token reference id.
- `GradeComponentAuthzTest` (`@WebMvcTest`) — non-owner denied (service not invoked); owner deletes with spoofed `X-Teacher-Id` ignored → service invoked with token reference id.
- `AssignmentServiceTest` — added `shouldCreateAssignment_whenAdminBypassesOwnership` + `@Mock AuthorizationBean authz`.
- `GradeServiceTest` — added `shouldBypassMainTeacherCheck_whenAdmin` (OWNER-inclusive) + `@Mock AuthorizationBean authz`.
- Existing ITs migrated `X-Teacher-Id` → `X-User-Reference-Id`: `AssignmentIntegrationTest`, `AssignmentFlowIntegrationTest`.

## Related

- Discovered in: GAP-1299 cross-flow sweep (PR `fix/gap-1299-lms-authoring-authz`)
- Fixed in: PR `fix/gap-1300-1301-xteacherid-spoof` (2026-06-14)
- GAP-1299 (LMS authoring — same class, fixed), GAP-1000 (grade finalize precedent), GAP-798 (umbrella actor↔owner bridge), GAP-814 (gateway-controlled headers), GAP-1300 (attendance — same wave)

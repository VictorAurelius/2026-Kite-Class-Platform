---
audience: dev
---

# GAP-1301 — Assignment + Grade controllers trust client `X-Teacher-Id` as actor identity (spoofable)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Security (authz)
**Found:** 2026-06-14 (cross-flow sweep of GAP-1299 — same bug class)
**Affects:** `AssignmentController` (7 sites: create/update/delete/publish/etc.), `GradeController.deleteComponent` (1 site)

## Problem

Same bug class as GAP-1299: these controllers read the **client-supplied `X-Teacher-Id` `@RequestHeader`** as the actor identity passed to the service (teacher attribution / ownership input). The gateway does NOT control/strip `X-Teacher-Id` (GAP-814) → spoofable. Surfaced by the GAP-1299 cross-flow sweep (`grep '@RequestHeader("X-Teacher-Id")'`).

Severity note: `GradeController.deleteComponent` already has `@PreAuthorize("@authz.hasAccessToGradeComponent(#id)")` (resource ownership bounds WHO can act), but still forwards the spoofable `X-Teacher-Id` for attribution. `AssignmentController` sites need per-site triage for both role-gate and identity-source.

## Proposed Fix

Mirror GAP-1299 / GAP-1000: derive the acting teacher from the authenticated principal (`UserContext.getCurrentReferenceId()` ← gateway `X-User-Reference-Id`), drop `X-Teacher-Id` as identity source; add/confirm `@PreAuthorize` role gate. Sweep callers (incl. test headers) per `api-contract-change-caller-sweep.md`.

## Acceptance Criteria

- [ ] Assignment + grade-component acting teacher id derived from token (`X-User-Reference-Id`), not client `X-Teacher-Id`
- [ ] Role gate present on assignment write endpoints (STUDENT/PARENT blocked)
- [ ] Tests: spoofed `X-Teacher-Id` ignored; happy path + ADMIN/OWNER preserved; `./mvnw test` green

## Related

- Discovered in: GAP-1299 cross-flow sweep (PR `fix/gap-1299-lms-authoring-authz`)
- GAP-1299 (LMS authoring — same class, fixed), GAP-1000 (grade finalize precedent), GAP-798 (umbrella actor↔owner bridge), GAP-814 (gateway-controlled headers)

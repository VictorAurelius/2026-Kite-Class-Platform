---
audience: dev
---

# GAP-1299 — LMS authoring privilege-escalation (no `@PreAuthorize` + spoofable `X-Teacher-Id`)

**Status:** 🟢 DONE (2026-06-14)
**Priority:** 🟠 P1
**Domain:** Backend / Security (authz)
**Found:** 2026-06-14 (G1 runtime walk RBAC+LMS — PoC appended to GAP-798)
**Phase:** phase-1-beta
**Affects:** `LmsController` authoring/mutation endpoints (module/lesson/resource CRUD + reorder + upload-url + completion-roster) — 14 endpoints

## Problem

The G1 runtime walk (`documents/04-quality/audits/rst-html/2026-06-14-g1-runtime-walk-rbac-lms.md`, PoC appended to GAP-798 §Log) confirmed an **exploitable privilege-escalation + impersonation** hole in LMS authoring:

- The authoring endpoints had **zero `@PreAuthorize`** — any authenticated role (incl. STUDENT/PARENT) reached the handler.
- The acting teacher identity was the **client-supplied `X-Teacher-Id` header**, which the gateway does NOT control/strip (only `X-User-Id` / `X-User-Roles` / `X-User-Reference-Id` / `X-Tenant-Id` are gateway-controlled per GAP-814) → spoofable.

PoC: mint a STUDENT token + `X-Teacher-Id: 3` → `POST /api/v1/lms/courses/13/modules` → **HTTP 201**, creating LMS content AS teacher 3 (the service "teacherId must be course owner" check passed because the attacker set `X-Teacher-Id` = the real owner). Same class as GAP-1000 (finalize teacherId self-asserted) — the actor-identity-from-client-header anti-pattern.

## Root Cause

Two missing controls: (1) no role gate; (2) actor identity sourced from a client-spoofable request header instead of the gateway-injected authenticated principal.

## Fix (two layers — defense in depth)

1. **Role gate** — `@PreAuthorize("hasAnyRole('TEACHER','OWNER','ADMIN')")` on all 14 LMS authoring/mutation endpoints (`@EnableMethodSecurity` already active; `GatewayHeaderAuthenticationFilter` bridges `X-User-Roles` → authorities). STUDENT/PARENT blocked with 403 before any handler logic.
2. **Identity from token** — the acting teacher id is derived from the authenticated principal (`UserContext.getCurrentReferenceId()` ← gateway-injected `X-User-Reference-Id` = `teachers.id`). The `X-Teacher-Id` `@RequestHeader` is **removed entirely** from the controller — there is nothing left to spoof. Mirrors the GAP-1000 `GradeServiceImpl.finalizeGrade` precedent.
3. **ADMIN/OWNER bypass** — `verifyCourseOwnership` bypasses per-course ownership when `authz.isAdmin()` (ROLE_PLATFORM_ADMIN / ROLE_ADMIN / ROLE_OWNER); they carry no numeric reference id. `AuthorizationBean.isAdmin()` exposed `public` for service-layer reuse. RLS `@Filter` + tenant isolation unchanged.

## Acceptance Criteria

- [x] All LMS authoring/mutation endpoints role-gated `hasAnyRole('TEACHER','OWNER','ADMIN')` (STUDENT/PARENT → 403)
- [x] Acting teacher derived from `X-User-Reference-Id` (token), NOT client `X-Teacher-Id` (header dropped)
- [x] Teacher A cannot impersonate teacher B via `X-Teacher-Id` (spoof ignored → 403)
- [x] Course-owning teacher happy path preserved (201)
- [x] OWNER/ADMIN bypass ownership (201)
- [x] RLS `@Filter` intact; tenant isolation unchanged
- [x] LMS api-contract + use-cases auth notes updated
- [x] `./mvnw test` green, no regression

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 — automated, BE authz fix, no human G2 walk needed)

`LmsAuthoringAuthzTest` (Testcontainers + real method security + seeded course owned by teacher 100) — 6/6 PASS:
- STUDENT POST module + spoofed `X-Teacher-Id: 100` → **403** (was 201 PoC) ✓
- PARENT POST module → **403** ✓
- TEACHER (`X-User-Reference-Id: 999`) + spoof `X-Teacher-Id: 100` → **403** (impersonation blocked) ✓
- TEACHER (`X-User-Reference-Id: 100`, owner) → **201** (happy path) ✓
- OWNER (no reference id) → **201** (admin bypass) ✓
- ADMIN (no reference id) → **201** (admin bypass) ✓

`LmsServiceTest` (+2: admin bypass, null-non-admin deny) + `LmsIntegrationTest` (header migrated `X-Teacher-Id`→`X-User-Reference-Id`) + `LmsPhase0ServiceTest` all green. Total run: **46 tests, 0 failures** (`./mvnw -Dtest='LmsServiceTest,LmsPhase0ServiceTest,LmsIntegrationTest,LmsAuthoringAuthzTest' test` → BUILD SUCCESS).

## Related

- **GAP-798** (umbrella — domain-entity actor↔numeric-owner bridge "Gateway convention V2"; LMS-authoring instance closed here, broader bridge still PARTIAL)
- **GAP-1297** (sister — LMS GET/progress `X-User-Id`→`X-User-Reference-Id`; same controllers, identity-header direction)
- **GAP-1000** (precedent — `GradeServiceImpl.finalizeGrade` derive actor from `UserContext` not client `request.getTeacherId()`)
- **GAP-814** (gateway controls only `X-User-Id`/`X-User-Roles`/`X-User-Reference-Id`/`X-Tenant-Id`; `X-Teacher-Id` spoofable)
- `cross-flow-bug-class-sweep.md` — sister class: other controllers reading client `X-Teacher-Id` as identity (`GradeController`, `AttendancePeriodController`) — see §sweep note in PR

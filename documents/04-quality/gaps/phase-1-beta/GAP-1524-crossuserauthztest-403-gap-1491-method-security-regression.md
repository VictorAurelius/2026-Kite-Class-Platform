# GAP-1524: CrossUserAuthzTest 13 failures — GAP-1491 method-security regression (kiteclass-core CI red on main)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend (test-infra / security)
**Found:** 2026-06-22 (discovered during GAP-1523 investigation — full-suite + isolation + main-baseline runs)
**Affects:** `kiteclass/kiteclass-core` — `CrossUserAuthzTest` (13/13 tests) → **kiteclass-core CI red on main since #2525**

## Problem

`CrossUserAuthzTest` fails **13/13** with `Status expected:<201> but was:<403>` at the FIRST setup line `createClassOwnedBy:178` → `testDataBuilder.createTestTeacher(...)`. **Pre-existing on `main` HEAD** (verified by isolated run + main-baseline run, both without GAP-1523's cache change → identical 403s). NOT caused by GAP-1523.

**Why it was never caught:** #2525 (GAP-1491) was `--admin` merged after the kiteclass-core suite hit the 25min trip-wire timeout (`ADMIN_MERGE_OVERRIDE: GAP-1393`). The timeout killed the run **before** `CrossUserAuthzTest` (package `...integration`, alphabetical) executed → its failures never surfaced as "Failures". GAP-1491's gap note claim *"No regression (test-profile TestSecurityConfig has no @EnableMethodSecurity so existing tests unaffected)"* was **unverified** for this `@SpringBootTest`.

## Root Cause

GAP-1491 (#2525, OWASP A01 fix) added method-level `@PreAuthorize` to 8 controllers with `@EnableMethodSecurity` active. `CrossUserAuthzTest` is a pre-existing `@SpringBootTest` that **tests IDOR guards → method security IS enforced in its context**. Its setup helpers POST to endpoints that GAP-1491 newly guarded, but **without an `X-User-Roles` header**:

| Setup call | Endpoint | New `@PreAuthorize` (GAP-1491) | Header sent | Result |
|---|---|---|---|---|
| `createTestTeacher` | `POST /api/v1/teachers` | `hasAnyRole('OWNER','ADMIN','PRINCIPAL','PLATFORM_ADMIN')` | `X-Tenant-Id` only — **no `X-User-Roles`** | **403** ← fails here |
| `createPublishableCourse` | `POST /api/v1/courses` | `hasAnyRole('TEACHER','ADMIN','OWNER','PLATFORM_ADMIN','STAFF')` | no `X-User-Roles` | (would 403) |
| publish | `POST /api/v1/courses/{id}/publish` | same | no `X-User-Roles` | (would 403) |
| create class | `POST /api/v1/courses/{id}/classes` | none (no guard) | — | ok |

The `X-User-Roles` header → `ROLE_*` authority bridge is `GatewayHeaderAuthenticationFilter`. Other tests (Vetting/IncidentReporting ITs) pass `X-User-Roles`; `CrossUserAuthzTest`'s shared `TestDataBuilder` setup helpers do not. Only `CrossUserAuthzTest` fails (the other 15 callers of these helpers don't enforce method security → `@PreAuthorize` is a no-op there → setup returns 201).

## Proposed Fix

Give the setup-actor requests an authorized role header. `ADMIN` (or `OWNER`/`PLATFORM_ADMIN`) satisfies all 3 guarded setup endpoints.

- **(a) preferred:** Add `.header("X-User-Roles", "ADMIN")` to `TestDataBuilder.createTestTeacher` + `createPublishableCourse` setup POSTs (harmless for the 15 callers without method security; satisfies the guard for `CrossUserAuthzTest`). Add it to the `/publish` + create-class POSTs in `CrossUserAuthzTest.createClassOwnedBy` too.
- **(b) alt:** Overload setup helpers with a `roles` param so the setup actor is explicit and IDOR-assertion actors (teacher1/teacher2/parent) stay role-correct.

Blast radius: 16 test files use the helpers — verify the change keeps them green (`./mvnw test` affected). The setup-actor role must be distinct from the IDOR-assertion actors so the negative tests still get their 403.

## Acceptance Criteria

- [x] `CrossUserAuthzTest` passes 13/13 (setup returns 201; IDOR negative assertions still get their expected 403) — local full-suite 2026-06-22
- [x] No regression in the other 15 test files calling `createTestTeacher` / `createPublishableCourse` / `createTestStudent` — full suite 1830 tests, 0 failures
- [ ] kiteclass-core "Test Core Service" suite green (0 failures) on a PR CI run — *PR pending*

## Fix shipped

Authenticated the fixture-setup POSTs as ADMIN via `.with(user("fixture-admin").roles("ADMIN"))` (SecurityContext-level, the canonical spring-security-test idiom — a bare `X-User-Roles` header is inert because `TestSecurityConfig`'s filter chain has no header→authority bridge). Endpoints fixed (all GAP-1491-guarded, all include ADMIN in their `hasAnyRole`):
- `TestDataBuilder.createTestTeacher` → `POST /teachers`
- `TestDataBuilder.createTestCourse` + `createPublishableCourse` → `POST /courses`
- `TestDataBuilder.createTestStudent` (×2 overloads) → `POST /students`
- `CrossUserAuthzTest.createClassOwnedBy` → `POST /courses/{id}/publish`

`X-User-Id` still drives the `@authz.hasAccessTo*` ownership guards (the IDOR assertions), so the negative tests still get their 403. Iteration: 13 → 3 (after teacher/course) → 0 (after student).

## Related

- **Cause:** GAP-1491 (#2525, `closed/`) — added `@PreAuthorize` to 8 controllers; "no regression" claim unverified (timeout-masked)
- **Discovered during:** GAP-1523 (context-cache thrash fix) — local full-suite run surfaced the 13 failures; isolation + main-baseline confirmed pre-existing
- **Discovery method:** per `discovery-to-gap-inline-filing.md` §1 + `cross-flow-bug-class-sweep.md` (sweep: do the 7 other GAP-1491 `*AuthzTest` slices + any other `@SpringBootTest` using guarded setup endpoints have the same gap?)
- `admin-merge-discipline.md` §2 — illustrates the "post-rebase wait + verify exact candidate" gap that let #2525 land red
- `feature-ship-runtime-walk-mandate.md` — full-suite run-not-just-compile would have caught it at #2525

## Log

- 2026-06-22 — **Fixed (→ PARTIAL).** Setup helpers authenticate as ADMIN via `.with(user().roles("ADMIN"))` on the 4 GAP-1491-guarded endpoint families (teachers/courses/students/publish). Local full suite green: 1830 tests, 0 failures (was 13). `X-User-Roles` header first attempt was inert (no filter bridge in `TestSecurityConfig`) — switched to SecurityContext-level `.with(user())` per the canonical spring-security-test idiom (mirrors GAP-1491's own `@WithMockUser` tests). Stays PARTIAL until the PR's CI run confirms green.
- 2026-06-22 — Filed. Discovered during GAP-1523 fix: local full-suite (13 fail, all `CrossUserAuthzTest`) → isolation run (13/13 fail alone → not cache-related) → main-baseline run (same 403s on main HEAD without cache change → pre-existing). Root cause traced to GAP-1491 `@PreAuthorize` + method-security-active `@SpringBootTest` + setup helpers missing `X-User-Roles`. kiteclass-core CI red on main since #2525 (timeout-masked).

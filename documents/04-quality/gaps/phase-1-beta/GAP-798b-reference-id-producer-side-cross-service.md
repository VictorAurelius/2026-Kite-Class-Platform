---
audience: dev
---

# GAP-798b — X-User-Reference-Id producer side (cross-service)

**Status:** 🔵 OPEN — ⛔ BLOCKED on parent/teacher/student login-wiring (see §Blocker)
**Priority:** 🟠 P1
**Domain:** Backend / Security (authz) — cross-service (subscription + gateway + kiteclass-core)
**Found:** 2026-05-28 (GAP-798 consumer-side implementation — architectural floor)
**Phase:** phase-1-beta
**Affects:** Parent flow runtime (fail-closed until shipped), Storage/Assignment/LessonProgress/Lms controllers, AuthService JWT, gateway filter

## ⛔ Blocker (investigation 2026-05-28, per release-fix-retry-budget.md §3.5)

Empirical state-check of producer-side surfaces surfaced a **prerequisite blocker outside this gap's scope** — GAP-798b cannot reach DONE until parent/teacher/student login-token issuance is wired.

**Evidence:**
- `AuthService.resolveTenantIdForRole` javadoc (kitehub-subscription, ~line 630, verbatim): *"Other tenant-scoped roles (STAFF/TEACHER/PARENT/STUDENT) → not yet wired here... those roles do not currently issue tokens via this service (staff invitations + parent/student logins ship in later waves)."*
- `StaffInvitationController.accept` (line 235-242) mints a `User` UUID but never touches kiteclass-core `teachers` row → subscription does not know `teachers.id` at accept-time → teacher `reference_id` population needs a cross-service wire that does not exist.
- `users` table latest migration = V57; `reference_id` column absent (confirmed grep).
- `User` entity (kitehub-platform) has no `referenceId` field.

**Why this blocks DONE:**
- `reference_id` would only ever be non-null for parent/teacher/student — but those roles **do not log in / receive tokens yet**, so the population step (`users.reference_id = parents.id / teachers.id`) has **no trigger point**.
- AC *"RST re-walk: parent login → child grade/attendance"* **cannot be performed** — parent login does not exist.
- Building the migration + JWT claim + gateway forward now = forward-compatible dormant plumbing with **no live consumer and no end-to-end verification possible** = trust-pass anti-pattern (per `feature-ship-runtime-walk-mandate.md` + `pre-handoff-self-test-completeness.md`). Deliberately NOT built.

**Correct sequencing:** unblock when parent/teacher/student login-token issuance lands (extends `AuthService` per the GAP-531 follow-up family). At that point, set `reference_id` at the provisioning moment (parent redeem `RedeemInvitationResult.parentId` → consumed by whoever provisions the parent auth user; staff-accept → cross-service event/internal-API so `reference_id = teachers.id`). Then build §Proposed Fix below + RST re-walk.

**Decision 2026-05-28:** GAP-798b stays OPEN + BLOCKED (no work shipped). Session pivoted to unblocked work (seed script). Per `release-fix-retry-budget.md` §3.5 — investigation reshaped the plan before writing unverifiable security code.

## Problem

GAP-798 consumer side shipped (#1948): kiteclass-core authz (`hasAccessToChild`, `UserPreferencesController.validateUserAccess`) now reads the numeric `X-User-Reference-Id` header for ownership, while `X-User-Id` (UUID) is used for audit. Tests pass by injecting the header directly.

**BUT the producer side does not exist** — discovered during GAP-798 implementation:
- The gateway (`JwtAuthenticationGatewayFilter`) injects ONLY `X-User-Id` (UUID) + `X-User-Roles` + `X-User-Email`. It does NOT inject `X-User-Reference-Id`.
- The JWT carries no `referenceId` claim (claims: sub, email, role, type, tenantId).
- **`users.reference_id` column does NOT exist** in the kitehub DB. The "V1 convention users.reference_id = parents.id/teachers.id" referenced in old docs (StudentPortal) was documented but never implemented.

So at runtime, no request carries `X-User-Reference-Id` → parent flow + the 4 controllers stay **fail-closed** (deny). The auth-user ↔ domain-row link is absent in BOTH directions (no `parents.user_id`, no `users.reference_id`).

## Root Cause

The kitehub auth user (UUID, kitehub-subscription) and KiteClass domain entities (numeric PK, kiteclass-core) live in different services/DBs and were never linked. The numeric domain id is created in kiteclass-core (parent redeem returns `parentId`; teacher row created via TeacherController), while the auth user UUID is minted in kitehub-subscription (StaffInvitationController.accept) / gateway provisioning — neither writes the other's id.

## Proposed Fix (cross-service, security-sensitive, investigation-first)

1. **Migration** (kitehub-subscription): add `reference_id BIGINT` (nullable) to `users` table.
2. **Cross-service population** at the link points (the hard part — choose mechanism):
   - **Parent**: parent-invitation redeem returns `parentId`; whoever provisions the parent auth user sets `users.reference_id = parentId`. Verify whether parent login is even wired in production first.
   - **Teacher/Staff**: `StaffInvitationController.accept` (subscription) mints the User UUID but never touches kiteclass `teachers`. Add a cross-service step (event/internal API) so `users.reference_id = teachers.id`.
   - **Student**: students have no auth-user/login concept — decide if/when students log in before assigning reference_id.
3. **AuthService + TwoFactorController** (subscription): add `referenceId` claim (nullable) to access JWT at issue time.
4. **Gateway** (`JwtAuthenticationGatewayFilter`): read `referenceId` claim → inject `X-User-Reference-Id` header when present.
5. **4 deferred controllers** (Storage/Assignment-submit/LessonProgress/Lms): `@RequestHeader("X-User-Id") Long` → `X-User-Reference-Id` (reverted from GAP-798 attempt because their integration tests send X-User-Id — do the swap WITH the test sweep here, per cross-flow-bug-class-sweep).
6. **Tests**: update Storage/Assignment/LessonProgress/Lms integration tests to send X-User-Reference-Id.
7. **RST re-walk** parent flow live: parent login → view child grade/attendance (fail-closed → fail-correct).

⚠️ Security-sensitive cross-service. Dedicated clean-context session. Do NOT rush.

## Acceptance Criteria

- [ ] `users.reference_id BIGINT` column (migration) + populated at parent/teacher link points
- [ ] AuthService + TwoFactorController add `referenceId` JWT claim
- [ ] Gateway forwards `X-User-Reference-Id` header
- [ ] 4 controllers read `X-User-Reference-Id` + their integration tests updated (cross-flow sweep)
- [ ] RST re-walk: parent login → child grade/attendance NOT fail-closed-denied
- [ ] Decision documented: do students get reference_id / login?

## Related

- **GAP-798** (consumer side DONE #1948 — this is the producer-side remaining piece)
- **GAP-795** (audit chain UUID — DONE)
- `release-fix-retry-budget.md` §3.5 (investigate-first)
- `cross-flow-bug-class-sweep.md` (4-controller header swap + test sweep)

## Log

- **2026-05-28:** Filed from GAP-798 consumer-side implementation. Architectural floor: `users.reference_id` doesn't exist → producer side is genuine cross-service multi-session work. Consumer authz bridge shipped #1948 (ready for when producer lands). Parent flow fail-closed at runtime until this ships.
- **2026-05-28 (investigation):** Picked up for fix; investigate-first (per `release-fix-retry-budget.md` §3.5) surfaced a prerequisite blocker — parent/teacher/student login-token issuance not wired (`AuthService:630`, "ship later waves"). reference_id population has no trigger; RST re-walk AC unperformable (parent login doesn't exist). Marked ⛔ BLOCKED (see §Blocker). No code shipped — forward-compat plumbing deliberately NOT built (unverifiable security code = trust-pass anti-pattern). Session pivoted to unblocked seed-script work. Unblock when login-wiring lands.

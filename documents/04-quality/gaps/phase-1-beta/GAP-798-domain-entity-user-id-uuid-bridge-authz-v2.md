---
audience: dev
---

# GAP-798 — Domain-entity `user_id` UUID bridge (authz "Gateway convention V2")

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Security (authz)
**Found:** 2026-05-28 (GAP-795 fix-time investigation — the remaining "fix triệt để" piece)
**Phase:** phase-1-beta
**Affects:** kiteclass-core authz ownership checks — `hasAccessToChild` (parent), `UserPreferencesController.validateUserAccess`, 4 controllers (Storage/Assignment-submit/LessonProgress/Lms) using `@RequestHeader("X-User-Id") Long`

## Problem

GAP-795 migrated the AUDIT chain (`created_by`/`updated_by` + UserContext) to UUID. But authz OWNERSHIP checks still cannot match the actor: KiteClass domain entities (`parents`, `teachers`, `students`) have numeric PKs with **NO `user_id` UUID column linking them to the kitehub auth user**. Gateway forwards actor = UUID (`X-User-Id` = JWT `sub`); authz needs `actor-UUID == domain-entity-owner` but the domain row only has a numeric id → no bridge → cannot evaluate.

GAP-795 agent handled this **fail-closed** (deny non-admin, admin bypass intact) — security-safe but **blocks the parent flow** (parents can't access their child's data) + the 4 numeric-X-User-Id controllers stay broken (pre-existing).

This is the remaining piece for GAP-795 to be 100% thorough ("fix triệt để").

## Root Cause

Two id-spaces never unified: kitehub auth user = UUID (`users.id` UUID, JWT `sub`) vs KiteClass domain entities = numeric BIGINT PK ("Gateway convention V1" legacy). No FK bridges actor-UUID → domain-row.

## Proposed Fix — REVISED 2026-05-28 post-investigation (X-User-Reference-Id claim, NOT user_id columns)

**Investigation finding (2026-05-28, per release-fix-retry-budget §3.5):** the user_id-UUID-column approach (original draft below, superseded) is REDUNDANT + needlessly cross-service. The numeric bridge ALREADY exists as `users.reference_id` (V1 convention: Gateway sets `users.reference_id = parents.id / teachers.id` at provision). The intended mechanism is `X-User-Reference-Id` — a header already documented + consumed by kiteclass-core StudentPortal, and already sent by CrossUserAuthzTest — but **the gateway never injects it and the JWT never carries it**. That is the actual gap.

**Revised design (minimal, no new DB columns, no cross-service callback, no student-login decision):**
1. **AuthService + TwoFactorController** (kitehub-subscription): add `referenceId` claim (= `user.referenceId`, Long, nullable for admin/owner) to the access JWT at issue time.
2. **JwtAuthenticationGatewayFilter** (kitehub-gateway): read `referenceId` claim → inject `X-User-Reference-Id` header (only when present).
3. **UserContext + TenantFilterInterceptor** (kiteclass-core): add `currentReferenceId` (Long) read from `X-User-Reference-Id`. Keep `currentUser` (UUID) for audit (created_by per GAP-795).
4. **AuthorizationBean** `hasAccessToChild` + `hasAccessToClass`: use `getCurrentReferenceId()` (numeric = parents.id/teachers.id) instead of `getCurrentUser()` (UUID).
5. **UserPreferencesController.validateUserAccess**: compare `getCurrentReferenceId()` to path id.
6. **4 deferred controllers** (Storage/Assignment-submit/LessonProgress/Lms): `@RequestHeader("X-User-Id") Long` → `@RequestHeader("X-User-Reference-Id") Long` (numeric student/uploader id).
7. **Tests**: UserPreferencesControllerTest send `X-User-Reference-Id`; CrossUserAuthzTest already sends it; Wave02MigrationsTest `created_by`/`updated_by` expected type `bigint`→`uuid` (post-V73).

Audit (created_by) = UUID via X-User-Id; ownership/authz = numeric via X-User-Reference-Id. Clean separation. Students need no user_id (no login concept) — referenceId for student-portal already wired via X-User-Reference-Id.

⚠️ **Security-sensitive (authz).** Verify fail-closed → fail-correct (deny only true non-owners) via Testcontainers IT + RST re-walk parent flow.

### Superseded draft (user_id UUID columns — DO NOT implement)
~~Add user_id UUID FK to parents/teachers/students + populate at invite-accept + cross-service event subscription→core.~~ Rejected: redundant with `users.reference_id`; teacher case needs new cross-service wire (UUID minted in subscription, teachers row in core); student has no auth user. Investigation 2026-05-28 confirmed reference-id mechanism is simpler + correct.

## Acceptance Criteria

- [ ] `parents`/`teachers`/`students` have `user_id UUID` FK to kitehub auth user (migration)
- [ ] invite-accept populates `user_id` with the kitehub-user UUID
- [ ] `hasAccessToChild` evaluates `actor-UUID == parents.user_id` (parent can access own child, denies others)
- [ ] `UserPreferencesController.validateUserAccess` UUID path works
- [ ] 4 controllers (Storage/Assignment-submit/LessonProgress/Lms) resolve actor via bridge (no `Long.parseLong` on UUID)
- [ ] Authz Testcontainers IT: parent→own-child PASS, parent→other-child DENY, teacher→own-class PASS
- [ ] RST re-walk parent flow live: parent login → view child grade/attendance (not fail-closed denied)

## Related

- **GAP-795** (X-User-Id UUID migration — audit chain done; this gap = the authz-ownership remaining piece)
- `audit-to-gap-pipeline.md` §2.8 (fix-time investigation surfaced this)
- `release-fix-retry-budget.md` §3.5 (investigate invite-accept link point before implementing)
- Recurrence class: Wave meta-6 Bucket A #13/#16 (UUID auth vs numeric domain model)

## Log

- **2026-05-28:** Filed from GAP-795 fix-time investigation. GAP-795 agent fixed audit chain (created_by/updated_by + UserContext → UUID) but flagged authz ownership PARTIAL (fail-closed) because domain entities have no actor-UUID bridge. This gap = the "fix triệt để" remaining piece = "Gateway convention V2" (domain-entity user_id UUID bridge + invite-accept population + authz). Security-sensitive → investigation-first, dedicated session. User directive 2026-05-28 "phải fix hết gap triệt để".

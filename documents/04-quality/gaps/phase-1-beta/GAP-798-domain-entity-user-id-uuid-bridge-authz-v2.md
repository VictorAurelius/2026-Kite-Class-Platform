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

## Proposed Fix ("Gateway convention V2" — investigation-first per release-fix-retry-budget §3.5)

**Investigate first:** how parent/teacher/student accounts are created (invite-accept flow) — that's where the kitehub-user-UUID ↔ domain-row link must be established.

Then:
1. Add `user_id UUID` FK column to `parents` (+ `teachers` + `students` as needed) → references kitehub auth user UUID
2. Populate at **invite-accept** time (ParentInvitation/StaffInvitation accept → set domain-row `user_id` = the newly-created/linked kitehub user UUID). Backfill existing rows where a link is derivable (else NULL, fail-closed acceptable until linked)
3. Update authz: `hasAccessToChild` → `actor-UUID == parents.user_id`; `UserPreferencesController.validateUserAccess` → UUID path
4. Fix the 4 deferred controllers (Storage/Assignment-submit/LessonProgress/Lms) to resolve actor via the bridge instead of `Long.parseLong(X-User-Id)`
5. Flyway migration for the new columns + populate-on-accept logic
6. Authz IT (Testcontainers) covering parent→child access + teacher→class via the bridge

⚠️ **Security-sensitive (authz).** Do NOT spawn an agent blind — investigate invite-accept link point + design the bridge before implementing. Verify fail-closed → fail-correct (deny only true non-owners).

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

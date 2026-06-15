# GAP-1417: RBAC `user_roles` assignment layer disconnected from authorization (write-only)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-15 (RBAC assign-role G2★ walk — design-first investigation)
**Affects:** `kiteclass-core` module `role` (`RoleService`, `RoleController` `/api/v1/roles/assignments`), FE `/admin/roles`

## Problem

The owner-shell role-assignment surface (GAP-1119 Bucket D) writes rows to `user_roles`, but **nothing reads `user_roles` for authorization**. Real authz runs off the login role:

- Login role = `entity_type` (TEACHER/PARENT/STUDENT via `tenant-auth`; OWNER/STAFF via KiteHub) → JWT claim.
- Gateway forwards it as `X-User-Roles` → `GatewayHeaderAuthenticationFilter` → Spring `ROLE_*` → `@PreAuthorize` + `@authz.*` resource beans.
- `RoleService.getUserRoles / getUserPermissionNames / userHasPermission` (the `user_roles`-backed permission system) have **0 consumers outside `RoleService`** (verified by grep).

Consequences surfaced during the walk:
1. Assigning a role via `/admin/roles` has **no functional effect** (writes a row nothing reads).
2. "Người dùng & vai trò" roster shows "Chưa có người dùng nào được gán vai trò" even though teachers/students exist — because `user_roles` is empty (they function via `entity_type`, never written here).
3. "What role is a user before assignment?" is undefined in the `user_roles` model (empty), while functionally they already have their `entity_type` role. Two parallel role notions, no defined relationship.
4. No duplicate/feedback guard (BE `assignRoleToUser` is idempotent no-op) — moot while inert.

This is **deferred-by-design**: GAP-1119 decision 1 keeps the BE "dynamic-capable" and defers the rich permission layer to Phase 3. The Phase-1 assign UI was a premature scaffold that misled beta owners.

## Proposed Fix

**Phase 1 (this PR, mitigation):** gate `/admin/roles` to a read-only overview + explicit Phase-3 notice ("vai trò gán tự động khi tạo tài khoản"); remove the nav entry. Interactive assign UI (searchable picker #2441) preserved in git history.

**Phase 3 (full fix):** decide + wire the authz source — either (a) make `user_roles` the authoritative role/permission source (login + gateway derive `X-User-Roles` from `user_roles`, `entity_type` becomes the provisioning bootstrap), or (b) keep `entity_type` as role + use `user_roles` only for additive per-tenant custom permissions. Then re-enable the assign UI + define the `entity_type ↔ user_roles` relationship + dup-guard + roster of effective roles.

## Acceptance Criteria

- [ ] Phase 1: `/admin/roles` shows read-only overview + Phase-3 notice; no inert assign form; nav entry removed (this PR).
- [ ] Phase 3: a single documented authz source; assigning/revoking a role measurably changes `@PreAuthorize` outcomes (IT proving allow/deny flips).
- [ ] Phase 3: `entity_type ↔ user_roles` relationship documented in `role` rules.md.

## Related

- Discovered in: RBAC assign-role G2★ walk 2026-06-15 (PR #2441 picker → design-first revealed disconnect)
- Design: GAP-1119 (RBAC shell + Bucket D, defers permission depth Phase 3)
- Sister: GAP-1298 (assign/list LazyInit 500 — same Bucket D surface)

# GAP-981: STAFF login JWT thiếu tenantId — `resolveTenantIdForRole` không có nhánh STAFF (cross-tenant RBAC hole)

**Status:** 🟢 DONE
**Priority:** P0
**Domain:** Backend
**Found:** 2026-06-05 (Wave flow-kc2 KC-2 G1 walk — coordinator walk production-equivalent)
**Affects:** Mọi STAFF user nhận lời mời + accept → login → JWT thiếu `tenantId` → không truy cập được endpoint tenant-scoped (cross-tenant RBAC hole)

## Problem

Walk KC-2 (Owner mời staff → accept → STAFF login) 2026-06-05 phát hiện: sau khi STAFF accept invitation, STAFF login thành công nhưng **JWT `tenantId: null`** → STAFF không thuộc tenant nào → gọi endpoint tenant-scoped bị reject.

Empirical (sky-education tenant):
- Owner invite `staff4@skyedu.vn` → HTTP 201, accept → STAFF user `c3d91428` tạo.
- STAFF login → JWT `role=STAFF, tenantId=null` ❌.

`users.tenant_id` NULL by-design (per `AuthService:602` "users.tenant_id is NULL post-signup by current schema design"). Tenant resolve runtime qua `resolveTenantIdForRole(userId, role)`:
- OWNER → `instances.owner_id` lookup ✅
- **STAFF/TEACHER/PARENT/STUDENT → return null** (comment "wire when those auth paths land — currently not issuing tokens via this service"). Nhưng staff invitation ĐÃ ship (KC-2) → STAFF giờ issue token nhưng nhánh chưa wire.

## Root Cause

`AuthService.resolveTenantIdForRole()` viết khi staff invitation chưa ship → STAFF branch để null (GAP-531 follow-up). KC-2 ship staff invite → STAFF login path live nhưng tenant resolution chưa wire → JWT unscoped.

## Proposed Fix (SHIPPED same PR)

Thêm nhánh STAFF trong `resolveTenantIdForRole`: tenant = `staff_invitations.tenant_id WHERE accepted_user_id = userId AND status = ACCEPTED`.
1. `StaffInvitationRepository.findFirstByAcceptedUserIdAndStatus(UUID, StaffInvitationStatus)`.
2. `AuthService` field-inject `staffInvitationRepository` (`@Autowired(required=false)`, non-final → zero ctor ripple → 3 legacy AuthService test không vỡ; null-guard cho test).
3. STAFF branch resolve tenant từ accepted invitation.

## Acceptance Criteria

- [x] STAFF login JWT chứa `tenantId` đúng (verified: `tenantId=0edaee10` post-fix)
- [x] STAFF truy cập tenant-scoped được; STAFF → owner-only endpoint → 403 (RBAC enforced, verified)
- [x] 3 legacy AuthService test pass (field injection, exit 0)
- [x] Re-walk full chain PASS production-equivalent (invite→email→accept→STAFF login tenantId→RBAC 403)

## Related

- Discovered + fixed in: Wave flow-kc2 KC-2 G1 walk 2026-06-05 (PR này)
- Pre-walk persona sim FM-1: `persona-review/2026-06-05-pre-walk-kc2-staff-invitation.md`
- GAP-531 (original "wire STAFF/TEACHER/PARENT/STUDENT tenant resolution" follow-up — STAFF portion closed here; TEACHER/PARENT/STUDENT remain deferred to their waves)
- `pre-handoff-self-test-completeness.md` §2.7 multi-tenant isolation

# GAP-1123: Document invite split (STAFF KH / TEACHER KC) + role-hierarchy 5-role beta sync + staff-invitation doc-drift reconcile

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Documentation
**Found:** 2026-06-10 (Wave RBAC-Shell 1 Bucket E)
**Affects:** `documents/01-business/kiteclass/staff-invitation/`, `documents/01-business/kiteclass/role-hierarchy/`, `documents/01-business/roles/`

## Problem

Wave RBAC-Shell 1 §"Quyết định đã chốt" #4 yêu cầu document split invite STAFF(KiteHub)/TEACHER(KiteClass). Điều tra design-first (2026-06-10) surface 2 drift:

1. **Doc-vs-code drift `kiteclass/staff-invitation/rules.md` (v1.0, Wave meta-6):** doc ghi source = `kiteclass-core/.../module/staff/` + role ∈ {STAFF/TEACHER/MANAGER}. Verify trên `origin/main`:
   - V71 `staff_invitations` table **CÓ** tồn tại (`kiteclass-core/.../db/migration/V71__create_staff_invitations.sql`).
   - NHƯNG **KHÔNG có** KC `module/staff/` Java (entity/service/controller chưa build).
   - Production invite thật = **KiteHub-side** (`kitehub-subscription/staff/`): `CreateStaffInvitationRequest` chỉ `email`+`fullName` (KHÔNG có `role`); `StaffInvitationResponse.role // always "STAFF"`; canonical doc = `documents/01-business/roles/`. → KC staff-invitation doc mô tả 3-role invite là **planned/aspirational** (table seeded, Java chưa có), KHÔNG khớp shipped KH 2-role MVP (GAP-784 DONE confirm).

2. **Role-hierarchy doc chưa phản ánh quyết định beta 5-role fixed-curated (GAP-1119):** `role-hierarchy/rules.md` mô tả role-hierarchy dynamic Level 1-8 (TENANT_OWNER/PRINCIPAL/.../SUBJECT_TEACHER/ACCOUNTANT/STUDENT/PARENT — ADR-003). Quyết định GAP-1119 chốt beta = **5-role fixed-curated** (OWNER/STAFF/TEACHER/PARENT/STUDENT) + login-location split KH/KC; doc chưa có lớp ánh xạ beta-subset → dynamic design.

## Proposed Fix

Docs-only (KHÔNG đổi code, KHÔNG tự chốt invite redesign — redesign chờ user quyết Q-A..Q-D trong `documents/03-planning/plans/invite-flow-redesign-discussion-2026-06-09.md`):
1. Thêm section "⚠️ Implementation Status & Provisioning Split" vào `staff-invitation/rules.md` — phân biệt SHIPPED (KH 2-role) vs PLANNED (KC 3-role, V71 table only) + Mermaid sơ đồ split STAFF→KH / TEACHER→KC / PARENT→KC.
2. Thêm section "Phase 1 BETA — 5-role fixed-curated subset (GAP-1119)" vào `role-hierarchy/rules.md` — bảng 5-role + login-location + ánh xạ sang dynamic design.

## Acceptance Criteria

- [x] `staff-invitation/rules.md` có section phân biệt shipped-vs-planned + sơ đồ split provisioning
- [x] `role-hierarchy/rules.md` có bảng beta 5-role fixed-curated + login-location + ánh xạ dynamic
- [ ] Sister discovery gaps filed (GAP-1124 teacher-invite, GAP-1125 bulk-invite) — feed invite-redesign wave (chờ user Q-A..Q-D)

## Related

- Discovered in: Wave RBAC-Shell 1 Bucket E (inline, session 2026-06-10)
- Plan: `documents/03-planning/waves/wave-rbac-shell-1.md` Bucket E
- Discussion (user decision pending): `documents/03-planning/plans/invite-flow-redesign-discussion-2026-06-09.md`
- Sister docs gaps: GAP-686 (kitehub-branding RBAC 3-layer sync), GAP-709 (01-business/auth RBAC+JWT+2FA sync) — different scope (branding/auth), this = invite-split + role-hierarchy
- Discovery feature gaps (filed same session): GAP-1124, GAP-1125
- GAP-784 (DONE) — FE invite role-param drift; GAP-597 (OPEN P2) — invite resend/revoke/reminder

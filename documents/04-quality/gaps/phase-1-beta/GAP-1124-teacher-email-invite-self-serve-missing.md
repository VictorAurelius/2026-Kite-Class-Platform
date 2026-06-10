# GAP-1124: TEACHER email-invite self-serve thiếu (provision = admin-set-password thủ công)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-10 (Wave RBAC-Shell 1 Bucket E — discovery từ invite-flow-redesign-discussion)
**Affects:** `kiteclass-core/module/auth` + `module/teacher`, KC tenant-auth, owner-shell invite UI

## Problem

Discovery khi document invite split (GAP-1123). Hiện tại owner mời 1 giáo viên mới phải **tự đặt mật khẩu thủ công** qua `AuthCredentialProvisioningService.setPassword` + báo GV ngoài hệ thống (Zalo/điện thoại) — KHÔNG có email-invite link self-serve như STAFF (STAFF được email link tự set password qua `kitehub-subscription/staff/`). UX kém + không scale.

Nguyên nhân: invite production = KH-side, 2-role MVP (OWNER+STAFF) by design (BR-ROLE-001/005, GAP-784 DONE); TEACHER là KC-domain nhưng KC staff-invitation (V71 table seeded) chưa build Java module.

## Proposed Fix

Per `invite-flow-redesign-discussion-2026-06-09.md` Option 1 (RECOMMEND, **chờ user quyết Q-A..Q-D**): build KC-native TEACHER email-invite — token + email + accept + set-password → tạo KC `auth_credentials` (entity_type TEACHER). Mirror staff-invite pattern (KH). Surface trong KC owner-shell (post cross-product SSO RBAC-Shell Bucket C).

## Acceptance Criteria

- [ ] Owner gửi email-invite cho TEACHER → GV nhận link → set password → login KC `:3000` thành công (entity_type TEACHER)
- [ ] Tôn trọng KH/KC boundary (TEACHER auth ở KC, không re-home sang KH)
- [ ] 3-layer doc cập nhật (rules/use-cases/api-contract)

## Related

- Discovered in: Wave RBAC-Shell 1 Bucket E (session 2026-06-10)
- Design discussion (user decision pending): `documents/03-planning/plans/invite-flow-redesign-discussion-2026-06-09.md` §2 vấn đề #1, §5 Option 1
- Doc reconcile: GAP-1123; sister: GAP-1125 (bulk-invite); GAP-597 (invite resend/revoke/reminder); GAP-784 (DONE — FE role-param drift)


## Decision locked (2026-06-10, user — Q-A/Q-C/Q-D)

Option 1 split: build **KC-native TEACHER email-invite** (token + email + accept + set-password → `auth_credentials` entity_type=TEACHER), mirror KH staff-invite pattern. MANAGER **defer Phase 2** (BR-ROLE-005). Bulk path → GAP-1125. Run sau RBAC-Shell (owner-shell surface qua SSO Bucket C).

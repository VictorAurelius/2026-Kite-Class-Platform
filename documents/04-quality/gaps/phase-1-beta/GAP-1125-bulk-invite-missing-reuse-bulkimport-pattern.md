# GAP-1125: Bulk-invite thiếu (invite single-only; bulk pattern có sẵn chưa áp dụng)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-10 (Wave RBAC-Shell 1 Bucket E — discovery từ invite-flow-redesign-discussion)
**Affects:** `kitehub-subscription/staff` (KH staff invite) + KC teacher-invite (GAP-1124), owner-shell invite UI

## Problem

Discovery khi document invite split (GAP-1123). `CreateStaffInvitationRequest` chỉ nhận 1 email/request → owner setup đầu kỳ (vd 20 staff + 15 GV) phải gửi từng email một, không import từ sheet HR sẵn có, không preview lỗi trước khi gửi. Pattern bulk-import preview/commit/template/error-download **đã có sẵn** cho student (`kiteclass-core/module/student/bulkimport/` + `XlsxParser`) + enrollment (`module/enrollment/bulkimport/`) nhưng chưa áp cho invite.

## Proposed Fix

Per `invite-flow-redesign-discussion-2026-06-09.md` Q2/Q3 (**chờ user quyết** textarea vs CSV/XLSX vs cả hai): tái dùng `BulkImportController` pattern (preview→validate→commit + error-download) cho cả KH-staff-invite + KC-teacher-invite (GAP-1124). Single-invite giữ làm quick-path. Bulk-invite = surface riêng (tab/nút "Mời hàng loạt") → preview validation (email trùng / sai format / vượt quota) → per-row result.

## Acceptance Criteria

- [ ] Owner upload CSV/XLSX (hoặc paste multi-email) → preview validate (trùng/sai format/quota) → commit → kết quả per-row
- [ ] Tái dùng `BulkImportController` pattern (không build mới from scratch)
- [ ] Áp cho cả staff (KH) + teacher (KC, sau GAP-1124)

## Related

- Discovered in: Wave RBAC-Shell 1 Bucket E (session 2026-06-10)
- Design discussion (user decision pending): `documents/03-planning/plans/invite-flow-redesign-discussion-2026-06-09.md` §2 vấn đề #2, §3 Q2/Q3, §1.4 bulk-import pattern
- Reuse pattern: `kiteclass-core/module/student/bulkimport/` + `module/enrollment/bulkimport/`
- Sister: GAP-1123 (doc reconcile), GAP-1124 (teacher-invite), GAP-597 (resend/revoke/reminder)


## Decision locked (2026-06-10, user — Q-B)

**Cả hai** input mode: textarea quick (paste ≤10 email/dòng) + CSV/XLSX upload (>10, reuse `BulkImportController` preview/validate/commit/template/error-download). Áp cho **cả** KH-staff-invite + KC-teacher-invite (GAP-1124). Single-invite giữ làm quick-path 1 người.

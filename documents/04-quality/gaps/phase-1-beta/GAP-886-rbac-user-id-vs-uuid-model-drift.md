# GAP-886: RBAC `user_id`/`teacher_id` còn BIGINT/Long — lệch mô hình actor UUID

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC rbac)
**Affects:** `kiteclass-core` module rbac; `user_roles`, `vettings`

## Problem

User identity giờ là UUID (qua `X-User-Id` JWT) sau V73. Nhưng các cột định danh user trong RBAC vẫn `Long`/`BIGINT`:

- `UserRole.userId` Long → DB `BIGINT`
- `Vetting.teacherId` Long → DB `BIGINT` (FK logic tới `users.id`)

Khớp DB hiện tại nhưng lệch mô hình — cùng class drift với GAP-877 nhưng cụ thể cho user-identity columns (không phải actor audit). Nếu wave nâng cấp UUID full → cần migration cùng GAP-877.

## Proposed Fix

Batch chung với GAP-877 actor sweep — migration convert `user_roles.user_id`, `vettings.teacher_id`, `vettings.decided_by_user_id`, `user_roles.assigned_by` sang UUID. Update entity Long→UUID.

## Acceptance Criteria

- [ ] Migration converge với GAP-877
- [ ] Entity Long→UUID
- [ ] Reference cluster doc 05-rbac §A2

## Discovered in

`documents/02-architecture/database/kiteclass/05-rbac.md` §A2

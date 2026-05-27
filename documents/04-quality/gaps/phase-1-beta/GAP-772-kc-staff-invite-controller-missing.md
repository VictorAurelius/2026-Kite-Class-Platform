---
audience: dev
---

# GAP-772 — KC staff invite controller missing (Mảng B13 + C blocker)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B13 + Mảng C catalog probe)
**Affects:** B13 Owner mời nhân viên + C1 Staff nhận thư mời + C2 Staff đăng nhập (cascade)
**Phase:** phase-1-beta

## Problem

Wave 106 plan §3 mô tả:
- B13 "Mời Nhân viên qua thư + xem trạng thái lời mời"
- C1 "Nhân viên nhận thư mời → đăng ký tài khoản"

Catalog probe `grep @RequestMapping` trong `kiteclass/kiteclass-core/src/main/java/**/*Controller.java` ra:

```
@RequestMapping("/api/v1/parent-invitations")    # PARENT only
# KHÔNG có /api/v1/staff/invitations OR /api/v1/staff/invite
```

Toàn bộ B13 luồng + Mảng C (3 luồng C1+C2+C3) blocked tại BE layer — không có endpoint nào cho Owner gửi invite cho Staff.

Đối chiếu users table: `staff.test@test.vn` đã seed với role STAFF (đăng nhập 200), nhưng không có code path để Owner provision STAFF role through invite flow.

## Root Cause

Suy đoán: Phase 1 BETA scope decision deferred staff invite implementation. Plan Đợt 106 surface bị ẩn vì luồng B13/C tham chiếu placeholder.

## Proposed Fix

Option A — Implement: `StaffInviteController` + entity + invite token + email template + claim endpoint + Owner role-guard. Mirror `parent-invitations` pattern (đã có code template).

Option B — Defer Phase 1.5+: Update plan §3 đánh dấu B13/C scope explicit out-of-scope Phase 1 BETA + sửa B12 (Cài đặt) gộp invite UI placeholder "coming soon".

## Acceptance Criteria

- [ ] Decision logged: Option A (implement) hoặc Option B (defer)
- [ ] Nếu A: `StaffInvitationController` + DB schema + email + claim flow + RST re-walk
- [ ] Nếu B: plan §3 B13+C đánh dấu out-of-scope + handoff doc Phase 1 BETA persona scope explicit (Owner solo, no staff Phase 1)

## Related

- Wave 106 plan §3 B13 + C1-C3 — `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`
- Sister code template: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/` (ParentInvitationController pattern)
- Sister: GAP-773 (FE route `/staff/accept-invite` cũng missing — paired)

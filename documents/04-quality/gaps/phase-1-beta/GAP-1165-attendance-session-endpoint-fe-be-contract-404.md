# GAP-1165: `/attendance/reports` "Không thể tải dữ liệu" — FE gọi `GET /api/v1/attendance/session/{id}` mà BE chưa expose (404)

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-11 (KC attendance reports demo — user-flagged "Không thể tải dữ liệu. Vui lòng thử lại.")
**Affects:** `kiteclass-core` `AttendanceController` ; `kiteclass-frontend` `lib/api/attendance.ts:79` `getAttendanceBySession` → `use-attendance.ts:72,434` → `(dashboard)/attendance/reports/page.tsx`

## Problem

Trang `/attendance/reports` (KC, `:3000`) hiển thị banner đỏ **"Không thể tải dữ liệu. Vui lòng thử lại."** khi chọn lớp. Console: hàng loạt `GET http://localhost:9000/api/v1/attendance/session/{id}?page=0&size=1000 → 404`.

Root cause = **FE↔BE contract drift**: FE `attendanceApi.getAttendanceBySession(sessionId)` gọi `GET /api/v1/attendance/session/{sessionId}` (chỉ sessionId, không classId), nhưng `AttendanceController` KHÔNG có mapping đó — chỉ có `GET /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance`. FE comment dòng 140-142 tự thừa nhận "workaround since backend doesn't have a direct endpoint" nhưng workaround lại trỏ vào endpoint không tồn tại.

Flow gãy: `reports/page.tsx` → `useAttendanceByClass(classId)` → `getAttendanceByClass()` → `getSessions(classId)` (OK) → per-session `getAttendanceBySession(session.id)` → **404 mỗi session** → `attendanceError` set → banner đỏ.

Cơ chế detect tĩnh `check-fe-be-api-contract.sh` MISS drift này vì path là template-literal động (`${sessionId}`) — đúng giới hạn detector.

## Fix (shipped)

Thêm endpoint BE `GET /api/v1/attendance/session/{sessionId}` vào `AttendanceController` — sibling của bản class-scoped, resolve ownership từ chính session:
- Service method `attendanceService.getAttendanceBySession(sessionId, pageable)` đã tồn tại sẵn (chỉ thiếu controller mapping).
- Authz mới `AuthorizationBean.hasAccessToSession(sessionId)` (mirror `hasAccessToEnrollment`): resolve `class_sessions.id → class_id → hasAccessToClass`, OR admin. OWASP A01 per-resource guard.

## Acceptance Criteria

- [x] `GET /api/v1/attendance/session/{sessionId}` trả 200 (không còn 404)
- [x] Authz: admin OR teacher-of-class pass; cross-tenant/non-owner deny
- [x] Trang `/attendance/reports` load đủ data + export chạy

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

Stack: `kiteclass-core` rebuilt từ worktree fix + recreate container → healthy. Verify qua gateway `:9000`:
- Login `admin@test.com` (ADMIN, tenant skytest) → JWT 200.
- Sweep toàn bộ 12 session của lớp "Lớp Toán 10A1" (class 18, session 118-129): **12/12 → HTTP 200, tổng 120 attendance records** (10 HS × 12 buổi, khớp seed).
- Trước fix: 12/12 → 404. Sau fix: 12/12 → 200. Banner "Không thể tải dữ liệu" hết.

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** FE call site trong `lib/api/attendance.ts` gọi path/method KHÔNG khớp `AttendanceController` mapping.

**Sites found + verdict:**

| # | FE call | BE mapping | Verdict |
|---|---|---|---|
| 1 | `getAttendanceBySession` → `GET /attendance/session/{id}` | (thiếu) | **FIX** this PR (covers `use-attendance.ts:72` + `:434`) |
| 2 | `markBulkAttendance` → `POST /attendance/bulk` (`use-attendance.ts:133`) | BE = `POST /attendance/classes/{classId}/sessions/{sessionId}/attendance` | **DEFER** → GAP-1166 |
| 3 | `updateAttendanceStatus` → `PUT /attendance/{id}` (`use-attendance.ts:168`) | BE = `PATCH /attendance/{id}` | **DEFER** → GAP-1167 |
| 4 | `markAttendance` `POST /attendance` ; `getAttendance` `GET /{id}` ; `getAttendanceByEnrollment` ; `deleteAttendance` ; `getStudentStats` ; `getClassStats` | khớp | EXEMPT (no drift) |

## Related

- Discovered in: KC attendance reports demo session 2026-06-11
- Follow-up: GAP-1166 (bulk-mark drift), GAP-1167 (PUT-vs-PATCH drift)
- Detector gap: `check-fe-be-api-contract.sh` không bắt template-literal dynamic path

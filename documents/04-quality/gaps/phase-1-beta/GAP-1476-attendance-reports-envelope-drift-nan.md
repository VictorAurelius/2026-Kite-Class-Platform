# GAP-1476: Báo cáo điểm danh — "Không thể tải dữ liệu" (envelope drift) + NaN% (chia 0)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-17 (KC-3 G2 walk — trang `/attendance/reports`)
**Affects:** `kiteclass/kiteclass-frontend/src/lib/api/attendance.ts`, `.../app/(dashboard)/attendance/reports/page.tsx`

## Problem

Trang **Báo cáo điểm danh** (`/attendance/reports`) lỗi 2 chỗ khi walk:

### A. "Không thể tải dữ liệu. Vui lòng thử lại." (FE↔BE envelope drift)
Page render error khi `attendanceError` set. Truy nguồn:
- `useAttendanceByClass` → `getAttendanceByClass` → gọi `getAttendanceBySession(sessionId)` cho mỗi session rồi `Promise.all` + `result.content`.
- `getAttendanceBySession` (attendance.ts:88) làm `return response.data.data!` — **mong response wrapped `ApiResponse<PaginatedResponse>` (`{success, data:{content}}`)**.
- NHƯNG BE `AttendanceController.getAttendanceBySessionId` trả `ResponseEntity<Page<AttendanceResponse>>` **UNWRAPPED** (`{content:[...]}` trực tiếp, KHÔNG envelope — khác classes API). → `response.data.data` = `undefined` → `result.content` ở `getAttendanceByClass` ném TypeError → `attendanceError`.

Verify live (gateway :9000): `GET /api/v1/attendance/session/182` → `{"content":[...]}` (unwrapped) trong khi `GET /api/v1/classes?...` → `{"success":true,"data":{...}}` (wrapped).

### B. NaN% (chia cho 0 không guard)
Khối "Phân bố trạng thái" có 10 chỗ `(stats.X / stats.total) * 100` **không guard zero**. Khi lớp 0 bản ghi điểm danh → `stats.total=0` → `0/0` → render **"NaN%"**. (Page đã guard `presentRate`/`absentRate` line 64-65 nhưng quên khối distribution.)

## Fix (shipped, FE-side)

- `attendance.ts:getAttendanceBySession` → `apiClient.get<PaginatedResponse<Attendance>>` + `return response.data` (khớp shape BE unwrapped). Fixes A cho report page.
- `reports/page.tsx` → thêm `safePct(n) = stats.total > 0 ? n/stats.total*100 : 0`; thay 10 chỗ chia inline. Fixes B.

## Acceptance Criteria

- [ ] `/attendance/reports` chọn lớp → load được data (hết "Không thể tải dữ liệu").
- [ ] Lớp 0 bản ghi → "Phân bố trạng thái" hiện `0%` (không NaN%).
- [ ] `pnpm build` + test PASS.
- [ ] Human G2 re-walk báo cáo điểm danh.

## Related

- Discovered in: KC-3 G2 walk 2026-06-17
- **Cross-flow sweep → GAP-1477**: TOÀN BỘ `AttendanceController` trả `ResponseEntity<X>` unwrapped (bypass global ApiResponse wrapper) nhưng cả `attendance.ts` dùng `.data.data!` → 7 fn còn lại (markAttendance/markBulkAttendance/getAttendance/getAttendanceByEnrollment/getStudentStats/getClassStats/updateAttendanceStatus) cùng bug class, surface khác (admin/stats, student-attendance) — DEFER → GAP-1477.

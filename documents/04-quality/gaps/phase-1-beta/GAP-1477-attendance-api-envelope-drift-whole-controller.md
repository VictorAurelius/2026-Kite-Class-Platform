# GAP-1477: Toàn bộ Attendance API envelope drift — FE `.data.data!` vs BE unwrapped `ResponseEntity<X>`

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-17 (cross-flow sweep từ GAP-1476)
**Affects:** `kiteclass/kiteclass-frontend/src/lib/api/attendance.ts` (7 fn), `kiteclass-core AttendanceController`

## Problem

Cross-flow sweep từ GAP-1476: **TOÀN BỘ** `AttendanceController` (kc-core) trả `ResponseEntity<X>` **unwrapped** (bypass global ApiResponse wrapper mà các controller khác dùng) — verify từ controller return types + 3 live probe (`/attendance/802` → `{"id":...}`, `/attendance/enrollment/109` → `{"content":...}`, `/attendance/stats/class/27` → `{"targetId":...}` đều KHÔNG envelope). NHƯNG `attendance.ts` cả file dùng `return response.data.data!` (mong wrapped) → **mọi đọc trả `undefined`**.

GAP-1476 đã fix 1 fn (`getAttendanceBySession`) vì nó crash trang `/attendance/reports`. 7 fn còn lại cùng bug class, surface khác (chưa walk):

| FE fn (attendance.ts) | BE endpoint | BE return | Surface ảnh hưởng |
|---|---|---|---|
| `markAttendance` | POST /attendance | `ResponseEntity<AttendanceResponse>` | điểm danh save (side-effect OK, return undefined) |
| `markBulkAttendance` | POST /classes/{}/sessions/{}/attendance | `ResponseEntity<List<...>>` | điểm danh batch save |
| `getAttendance` | GET /attendance/{id} | `ResponseEntity<AttendanceResponse>` | chi tiết 1 record |
| `getAttendanceByEnrollment` | GET /attendance/enrollment/{id} | `ResponseEntity<Page<...>>` | student attendance page |
| `getStudentStats` | GET /attendance/stats/student/{id} | `ResponseEntity<AttendanceStatsResponse>` | student stats |
| `getClassStats` | GET /attendance/stats/class/{id} | `ResponseEntity<AttendanceStatsResponse>` | admin/attendance/stats page |
| `updateAttendanceStatus` | PATCH /attendance/{id} | `ResponseEntity<AttendanceResponse>` | sửa trạng thái điểm danh |

## Proposed Fix

FE-side (cùng hướng GAP-1476 — KHÔNG wrap BE để tránh phá fix GAP-1476): đổi 7 fn `apiClient.METHOD<ApiResponse<X>>` → `<X>` + `return response.data`. Cập nhật test mock nếu có (hiện không có test assert wrapped shape cho attendance.ts). Per `cross-flow-bug-class-sweep.md` §4.1 (statically-detectable) — cân nhắc detector grep `\.data\.data` trên FE fn gọi attendance endpoint unwrapped.

## Acceptance Criteria

- [x] 7 fn attendance.ts trả đúng data (không undefined) — `apiClient.METHOD<ApiResponse<X>>` → `<X>` + `return response.data`. Verified per-fn vs BE controller return type (xem §Fix evidence) + vitest 7/7.
- [ ] admin/attendance/stats + student attendance + điểm danh save verify live (human G2 walk pending per `feature-ship-runtime-walk-mandate.md`).
- [x] `pnpm --filter kiteclass-frontend build` PASS (exit 0) + vitest `attendance.test.ts` 7/7 PASS + eslint 0 errors.

## Fix evidence (2026-06-17, wave-flow-kc3)

Per-fn BE return type confirmed UNWRAPPED `ResponseEntity<X>` (NOT `ResponseEntity<ApiResponse<X>>`) trong `AttendanceController.java`:

| FE fn | endpoint | BE return type (line) | Verdict |
|---|---|---|---|
| `markAttendance` | POST /attendance | `ResponseEntity<AttendanceResponse>` (L88) | ✅ unwrapped → fixed |
| `markBulkAttendance` | POST /classes/{}/sessions/{}/attendance | `ResponseEntity<List<AttendanceResponse>>` (L112) | ✅ unwrapped → fixed |
| `getAttendance` | GET /attendance/{id} | `ResponseEntity<AttendanceResponse>` (L133) | ✅ unwrapped → fixed |
| `getAttendanceByEnrollment` | GET /attendance/enrollment/{id} | `ResponseEntity<Page<AttendanceResponse>>` (L151) | ✅ unwrapped → fixed |
| `getStudentStats` | GET /attendance/stats/student/{id} | `ResponseEntity<AttendanceStatsResponse>` (L240) | ✅ unwrapped → fixed |
| `getClassStats` | GET /attendance/stats/class/{id} | `ResponseEntity<AttendanceStatsResponse>` (L263) | ✅ unwrapped → fixed |
| `updateAttendanceStatus` | PATCH /attendance/{id} | `ResponseEntity<AttendanceResponse>` (L286) | ✅ unwrapped → fixed |

0/7 wrapped → all 7 fixed FE-side (cùng hướng GAP-1476, KHÔNG sửa BE). Unused `ApiResponse` import removed.

**Caller regression check:** `markAttendance`/`markBulkAttendance`/`updateAttendanceStatus` consumers (`hooks/use-attendance.ts` mutations) dùng `onSuccess: (_, variables) => ...` / `() => ...` → bỏ qua return value, refetch via invalidate → 0 regression (broken `undefined` → real data, không ai phụ thuộc undefined). `getClassStats`/`getStudentStats`/`getAttendanceByEnrollment` query consumers giờ nhận data thật thay vì undefined (stats aggregate trong `useSystemAttendanceStats` đổi từ 0 → giá trị thật — fix, không phải regression).

## Related

- Parent: GAP-1476 (report page slice fixed)
- Sweep evidence: GAP-1476 §Related
- Rule: `cross-flow-bug-class-sweep.md` §4.1, `discovery-to-gap-inline-filing.md`

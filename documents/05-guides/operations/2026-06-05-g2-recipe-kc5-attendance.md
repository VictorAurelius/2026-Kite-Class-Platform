---
title: G2 Human Test Recipe — KC-5 Attendance (mark + bulk + stats)
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign G2 handoff cho KC-5 — điểm danh học sinh (single + bulk) + xem stats
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-05-flow-kc5-attendance.md
  - documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc5-attendance.md
---

# G2 Human Test Recipe — KC-5 Attendance

## Mục tiêu

Giáo viên/Admin điểm danh học sinh trong buổi học (single + bulk grid) + xem thống kê điểm danh. Verify 6 bug đã fix ở G1: **GAP-991** (authz), **GAP-992** (session-status guard), **GAP-993** (EXCUSED cần ghi chú), **GAP-994** (tỉ lệ tính cả LATE), **GAP-995** (enum docs), **GAP-996 P0** (schema drift V87 — write path chạy được lần đầu trên schema thật).

**Prereq:**
- Stack local UP: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8088/actuator/health` → 200.
- Data sẵn (tenant `sky-education` `0edaee10-...`): class **14** "Lớp A1 - Ca tối T2-T4" (SCHEDULED, 27 sessions id 1-27), enrollment **32** (student 4, đã set ACTIVE), teacher class 14 = UUID `00aa4ce9-0f7c-48a9-bf8d-6e974ba30023` + teacher_classes MAIN_TEACHER (teacher id 3). Tenant `khanh-phapluat` (`126eaa8c-...`) để test isolation.

**Thời lượng:** ~10-15 phút.

## ⚠️ Lưu ý quan trọng (contract surprises)

- **API điểm danh dùng `enrollmentId` + `sessionId`, KHÔNG phải `studentId` + `classId`.** Body single-mark = `{enrollmentId, sessionId, status, notes?}`.
- **Enum trạng thái = `PRESENT` / `ABSENT` / `LATE` / `EXCUSED` / `MAKEUP`. KHÔNG có `EXCUSED_ABSENCE`** (docs cũ sai, đã sửa).
- **Authz single-mark:** cần header `X-User-Id` = teacher của lớp (`00aa4ce9-...`) hoặc admin. **Bulk** cần thêm `X-Teacher-Id` (numeric, MAIN_TEACHER = `3`).

## Setup

- Mở browser + DevTools → Network tab (filter `attendance`).
- Terminal để query DB verify: `docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tA -c "..."`.
- Verify state đầu: `SELECT count(*) FROM attendance WHERE enrollment_id=32;` (G1 đã tạo vài row — có thể `DELETE FROM attendance WHERE enrollment_id=32;` để bắt đầu sạch).

Walk ưu tiên qua UI (frontend); mỗi bước có **curl fallback** (gọi thẳng core 8088). Biến tắt:
```
SKY=0edaee10-2d13-44be-9151-12b78b7c5fd4
TID=00aa4ce9-0f7c-48a9-bf8d-6e974ba30023
```

## Các bước

### Bước 1 — Điểm danh đơn (happy path)

- **Hành động:** Mở lớp 14 → buổi học (session) → chọn học sinh (enrollment 32) → đặt trạng thái `PRESENT` → lưu.
- **✅ Kỳ vọng (PASS):** HTTP 201, record được tạo, toast thành công.
- **⚠️ Sad path:** Nếu 500 → schema drift chưa fix (V87 chưa apply — báo ngay). Nếu 403 → thiếu/sai `X-User-Id` (không phải teacher lớp).
- **🔍 Verify:** `curl -s -X POST http://localhost:8088/api/v1/attendance -H Content-Type:application/json -H X-Tenant-Id:$SKY -H X-User-Id:$TID -d '{"enrollmentId":32,"sessionId":1,"status":"PRESENT"}'` → 201. DB: `SELECT status FROM attendance WHERE enrollment_id=32 AND session_id=1;` → `PRESENT`.

### Bước 2 — Các trạng thái khác (LATE / ABSENT / MAKEUP)

- **Hành động:** Điểm danh enrollment 32 ở các session khác với `LATE` (sess 2), `ABSENT` (sess 6), `MAKEUP` (sess 7).
- **✅ Kỳ vọng:** Mỗi cái → 201. (MAKEUP quan trọng — trước V87 bị CHECK constraint chặn.)
- **🔍 Verify:** đổi `sessionId` + `status` trong curl Bước 1.

### Bước 3 — EXCUSED cần ghi chú (GAP-993)

- **Hành động:** Điểm danh `EXCUSED` **không** nhập ghi chú → lưu.
- **✅ Kỳ vọng:** HTTP **400** `EXCUSED_REQUIRES_NOTE` (FE nên hiện lỗi "Vắng có phép cần ghi chú"). Sau đó nhập ghi chú "Ốm có phép" → lưu → **201**.
- **🔍 Verify:** `...-d '{"enrollmentId":32,"sessionId":8,"status":"EXCUSED"}'` → 400; thêm `"notes":"Ốm có phép"` → 201.

### Bước 4 — Guard session đã đóng (GAP-992)

- **Hành động:** Thử điểm danh vào buổi đã `COMPLETED`/`CANCELLED` (session 5 đã set COMPLETED ở G1).
- **✅ Kỳ vọng:** HTTP **400** `SESSION_NOT_MARKABLE`. Buổi không tồn tại → **404** `SESSION_NOT_FOUND`.
- **🔍 Verify:** `...-d '{"enrollmentId":32,"sessionId":5,"status":"PRESENT"}'` → 400; `"sessionId":99999` → 404.

### Bước 5 — Điểm danh hàng loạt (bulk grid)

- **Hành động:** Mở grid điểm danh cả buổi → đặt trạng thái cho học sinh → submit (cần đăng nhập là MAIN_TEACHER).
- **✅ Kỳ vọng:** HTTP 201, các record được tạo. Bulk vào buổi COMPLETED → 400.
- **🔍 Verify:** `curl -s -X POST "http://localhost:8088/api/v1/attendance/classes/14/sessions/11/attendance" -H Content-Type:application/json -H X-Tenant-Id:$SKY -H X-User-Id:$TID -H X-Teacher-Id:3 -d '{"sessionId":11,"records":[{"enrollmentId":32,"status":"PRESENT"}]}'` → 201.

### Bước 6 — Thống kê điểm danh (GAP-994)

- **Hành động:** Xem stats của học sinh (tỉ lệ điểm danh).
- **✅ Kỳ vọng:** `attendanceRate` = **(PRESENT + LATE) / tổng** × 100 (LATE được tính là có mặt). Vd 1 PRESENT + 1 LATE / 5 buổi = **40%** (không phải 20%).
- **🔍 Verify:** `curl -s "http://localhost:8088/api/v1/attendance/stats/student/4" -H X-Tenant-Id:$SKY -H X-User-Id:$TID` → field `attendanceRate`.

## Sad path quick checks (tổng hợp)

- **Enum sai:** gửi `"status":"EXCUSED_ABSENCE"` → 400 (chỉ chấp nhận `EXCUSED`).
- **Authz:** điểm danh không có `X-User-Id` hoặc user không phải teacher lớp → **403**.
- **Cách ly tenant:** đổi sang tenant `khanh`, GET/POST attendance của sky → **404/403** (không thấy resource tenant khác). ⚠️ Nếu gọi thẳng core **không** kèm `X-Tenant-Id` → trả data (200) — đây là GAP-997 P3 đã biết (gateway production luôn set tenant; không phải lỗi reachable qua gateway).

## Báo kết quả

**Khi G2 xong, báo lại 1 trong 4:**
- ✅ **FULL PASS** → Claude xác nhận KC-5 G1+G2, chờ G3 (production parity).
- ⚠️ **MOSTLY PASS** với lỗi cosmetic (UI label, toast) → catalog gap polish.
- 🔴 **BLOCKING ISSUE** (vd happy path 500, FE không gọi đúng endpoint) → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm screenshot/error + Network tab.

## Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| Happy 500 | V87 chưa apply — `SELECT version FROM flyway_schema_history WHERE version='87';` → nếu thiếu, rebuild kiteclass-core |
| 403 mọi mark | Thiếu `X-User-Id` = teacher lớp (`00aa4ce9-...`) |
| Bulk 403 | Thiếu `X-Teacher-Id:3` (MAIN_TEACHER) hoặc thiếu teacher_classes row |
| Enrollment not active | `UPDATE enrollments SET status='ACTIVE' WHERE id=32;` |

**G3 (production parity, post AWS restore):** verify multi-tenant attendance isolation thật + period attendance K12 (`/api/v1/attendance/periods` + daily-rollup) — secondary scope chưa walk ở G1.

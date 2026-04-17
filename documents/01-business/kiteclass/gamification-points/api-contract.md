# Gamification Points — API Contract

## Overview

Gamification points có **KHÔNG có REST API trực tiếp**. Points được quản lý nội bộ qua `PointService`, triggered khi attendance được đánh dấu hoặc cập nhật.

Points được tiêu thụ gián tiếp qua **Attendance API** (`/api/v1/attendance`).

---

## Internal Service API

### PointService.awardAttendancePoints

**Triggered by:** AttendanceService khi mark attendance
**Use Case:** UC-GAM-01

**Parameters:**
```
studentId: Long — ID học sinh
attendanceId: Long — ID bản ghi attendance
points: Integer — Điểm thưởng (từ config)
description: String — Lý do (e.g., "Attendance bonus")
```

**Behavior:**
- Tạo point record gắn với attendance
- Cộng vào tổng điểm của student
- Không duplicate nếu attendanceId đã có point record

---

### PointService.updateAttendancePoints

**Triggered by:** AttendanceService khi thay đổi status (PRESENT → ABSENT, etc.)
**Use Case:** UC-GAM-02

**Parameters:**
```
studentId: Long — ID học sinh
attendanceId: Long — ID bản ghi attendance
newPoints: Integer — Điểm mới (0 nếu ABSENT)
description: String — Lý do thay đổi
```

**Behavior:**
- Cập nhật point record theo attendanceId
- Recalculate tổng điểm student
- Nếu không tìm thấy record → log warning, không throw

---

### PointService.getTotalPoints

**Triggered by:** Leaderboard display, student profile
**Use Case:** UC-GAM-03

**Parameters:**
```
studentId: Long — ID học sinh
```

**Returns:** `Integer` — Tổng điểm tích lũy

---

## Consumed Via

| Feature | Endpoint | Ghi chú |
|---------|----------|---------|
| Mark attendance | POST /api/v1/attendance | Auto-award points |
| Update attendance | PUT /api/v1/attendance/{id} | Auto-update points |
| Student profile | GET /api/v1/students/{id} | Includes totalPoints |

## Error Codes (Internal)

| Code | Mô tả |
|------|--------|
| POINT_RECORD_NOT_FOUND | AttendanceId chưa có point record |
| STUDENT_NOT_FOUND | StudentId không tồn tại |
| DUPLICATE_AWARD | Đã award cho attendanceId này |

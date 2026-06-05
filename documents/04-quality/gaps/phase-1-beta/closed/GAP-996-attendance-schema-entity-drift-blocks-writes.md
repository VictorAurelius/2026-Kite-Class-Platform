# GAP-996: attendance schema↔entity drift chặn TOÀN BỘ write path (masked by test ddl-auto)

**Status:** 🟢 DONE (Wave flow-kc5 G1 re-walk PASS, 2026-06-05)
**Priority:** 🔴 P0
**Domain:** Backend (schema/migration — KC-5)
**Found:** 2026-06-05 (Wave flow-kc5 G1 walk — W-1, production-equivalent)
**Affects:** `attendance` table + `AttendanceServiceImpl.markAttendance` + `markBulkAttendance` (mọi attendance write)

## Problem

Attendance write path (single + bulk) trả **HTTP 500 trên mọi happy-path** khi chạy trên Flyway-migrated schema (production-equivalent). 3 drift giữa V1 schema và entity model (entity đã chuyển student_id → enrollment_id ở V79 nhưng schema legacy không sync):

1. `attendance.student_id BIGINT NOT NULL` (V1) — entity KHÔNG còn map `studentId` → INSERT bỏ qua cột → `null value in column "student_id" violates not-null`.
2. `chk_attendance_status` CHECK chỉ cho **lowercase** `'present','absent','late','excused'` — enum lưu **UPPERCASE** (`PRESENT`...) + thiếu `MAKEUP` → CHECK violation.
3. `uk_attendance UNIQUE (session_id, student_id)` stale — duplicate check thực tế ở (enrollment_id, session_id).

**Tại sao IT không bắt:** test profile (`application-test.yml`) dùng `flyway.enabled: false` + `ddl-auto: create-drop` → schema test sinh từ ENTITY (không có student_id, check đúng enum) → INSERT pass. IT mù hoàn toàn với Flyway schema thật. Class lỗi: `postgres-specific-type-testcontainers.md` (ddl-auto masks migration drift) + `design-patterns.md` §3.12 entity-migration triad drift. Bằng chứng: `attendance` table có **0 rows** — flow chưa bao giờ chạy trên DB thật.

## Proposed Fix

Migration `V87__fix_attendance_enrollment_model_drift.sql`:
- `ALTER COLUMN student_id DROP NOT NULL` (enrollment_id là canonical key).
- Drop lowercase `chk_attendance_status` → add CHECK uppercase enum (PRESENT/ABSENT/LATE/EXCUSED/MAKEUP).
- Add `uk_attendance_enrollment_session UNIQUE (enrollment_id, session_id)` (khớp entity @Table + BR-ATTEND-001 DB-level).

**Follow-up (defer):** test infra dùng ddl-auto thay vì Flyway → toàn bộ kiteclass-core IT mù với migration drift. Cân nhắc Testcontainers + flyway.enabled trong test (GAP follow-up — broad infra change).

## Acceptance Criteria
- [x] Happy PRESENT/LATE/ABSENT/MAKEUP single-mark → 201 (was 500) (W1/W1b/W1c/W1d)
- [x] Bulk mark → 201 (W9)
- [x] Duplicate (enrollment+session) → service 400 + DB unique defense (W2 → 400)
- [x] V87 applies clean trên live kiteclass_shared (Flyway V87 success=t; student_id nullable; CHECK uppercase)

## Related
- Discovered in: Wave flow-kc5 G1 walk 2026-06-05 (W-1)
- Class: `postgres-specific-type-testcontainers.md` + `design-patterns.md` §3.12
- Sibling guards verified working same walk: GAP-992 (404/400) + GAP-993 (400) + GAP-995 (400)
- Follow-up (separate gap candidate): kiteclass-core IT dùng ddl-auto thay Flyway → mù migration drift (broad test-infra change)

## Log

- **2026-06-05 (Wave flow-kc5 — DONE):** V87 (student_id nullable + uppercase status CHECK incl MAKEUP + enrollment-session unique) applied Flyway success; G1 re-walk PASS — happy 201 (5 statuses), bulk 201, duplicate 400. Attendance write path functional on production-equivalent schema lần đầu.

-- seed-attendance.sql — seed buổi học + điểm danh đầy đủ cho mọi lớp có học sinh.
--
-- Mục tiêu: tính năng xuất file điểm danh (AttendanceReportBuilder / DocumentGenerationController)
-- đã có sẵn nhưng DB chỉ có ~6 bản ghi attendance. Script này tạo:
--   1. Buổi học (class_sessions) — đảm bảo mỗi lớp có học sinh đạt tối thiểu 10 buổi.
--   2. Điểm danh (attendance) — 1 bản ghi cho MỌI (enrollment × buổi) chưa có, status phân bố
--      thực tế (~80% PRESENT, 8% LATE, 7% ABSENT, 5% EXCUSED) — deterministic theo (enroll,session)
--      nên re-run idempotent (NOT EXISTS bỏ qua row đã có, status không đổi).
--
-- Chạy:  docker exec -i kite-postgres psql -U kitehub -d kiteclass_shared < kitehub/scripts/seed-attendance.sql
-- DB:    kiteclass_shared (shared-DB + RLS per ADR-023). Role kitehub = superuser/bypassrls.
-- Enum:  @Enumerated(STRING) → giá trị UPPERCASE (SessionStatus / AttendanceStatus).

\set TARGET_SESSIONS 10

-- ============================================================
-- 1. Buổi học — bổ sung tới TARGET_SESSIONS buổi cho mỗi lớp có >=1 enrollment.
--    Buổi cũ giữ nguyên (chỉ thêm session_number > max hiện tại). Lịch: 2 buổi/tuần
--    (cách 3-4 ngày), lùi về quá khứ kết thúc gần hôm nay; status COMPLETED.
-- ============================================================
WITH cls AS (
    SELECT c.id AS class_id,
           c.instance_id,
           COALESCE(MAX(s.session_number), 0) AS maxnum
    FROM classes c
    JOIN enrollments e
      ON e.class_id = c.id AND e.deleted = false
    LEFT JOIN class_sessions s
      ON s.class_id = c.id AND s.deleted = false
    WHERE c.deleted = false
    GROUP BY c.id, c.instance_id
)
INSERT INTO class_sessions
    (class_id, session_number, session_date, start_time, end_time,
     topic, status, attendance_taken, instance_id, version, deleted, created_at, updated_at)
SELECT cls.class_id,
       g                                                   AS session_number,
       (CURRENT_DATE - ((:TARGET_SESSIONS - g) * 3))        AS session_date,
       TIME '18:00'                                        AS start_time,
       TIME '20:00'                                        AS end_time,
       'Buổi ' || g                                        AS topic,
       'COMPLETED'                                         AS status,
       true                                               AS attendance_taken,
       cls.instance_id,
       0, false, now(), now()
FROM cls
CROSS JOIN generate_series(1, :TARGET_SESSIONS) AS g
WHERE g > cls.maxnum;

-- ============================================================
-- 2. Điểm danh — 1 bản ghi cho mỗi (enrollment × buổi cùng lớp) chưa có.
--    Status deterministic theo md5(enrollment_id-session_id) → phân bố thực tế.
-- ============================================================
INSERT INTO attendance
    (instance_id, session_id, student_id, enrollment_id, status,
     marked_date, marked_at, points_awarded, version, deleted, created_at, updated_at)
SELECT e.instance_id,
       s.id                                               AS session_id,
       e.student_id,
       e.id                                               AS enrollment_id,
       pick.status,
       (s.session_date + s.start_time)                    AS marked_date,
       (s.session_date + s.start_time)                    AS marked_at,
       pick.points,
       0, false, now(), now()
FROM enrollments e
JOIN class_sessions s
  ON s.class_id = e.class_id AND s.deleted = false
CROSS JOIN LATERAL (
    SELECT r,
           CASE WHEN r < 80 THEN 'PRESENT'
                WHEN r < 88 THEN 'LATE'
                WHEN r < 95 THEN 'ABSENT'
                ELSE 'EXCUSED' END AS status,
           CASE WHEN r < 80 THEN 0
                WHEN r < 88 THEN -5
                WHEN r < 95 THEN -10
                ELSE 0 END         AS points
    FROM (
        SELECT (('x' || substr(md5(e.id::text || '-' || s.id::text), 1, 6))::bit(24)::int) % 100 AS r
    ) h
) AS pick
WHERE e.deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM attendance a
      WHERE a.enrollment_id = e.id
        AND a.session_id = s.id
        AND a.deleted = false
  );

-- ============================================================
-- 3. Đánh dấu các buổi đã có điểm danh là attendance_taken = true.
-- ============================================================
UPDATE class_sessions s
SET attendance_taken = true, updated_at = now()
WHERE s.deleted = false
  AND s.attendance_taken = false
  AND EXISTS (SELECT 1 FROM attendance a WHERE a.session_id = s.id AND a.deleted = false);

-- ============================================================
-- Tổng kết
-- ============================================================
SELECT 'class_sessions' AS tbl, count(*) FROM class_sessions WHERE deleted = false
UNION ALL
SELECT 'attendance', count(*) FROM attendance WHERE deleted = false;

-- seed-toan10a1-demo.sql — tạo lớp DEMO "Lớp Toán 10A1" giàu data để test export
-- trên trang /attendance/reports bằng tài khoản teacher_a@test.com.
--
-- Tenant: aaaabbbb-0000-0000-0000-000000000001 (tenant DUY NHẤT có account login KC).
-- Lớp này status IN_PROGRESS → hiện trong useAllActiveClasses (FE lọc SCHEDULED/IN_PROGRESS).
-- Self-contained + idempotent: class + 10 học sinh + enrollment + 12 buổi + điểm danh đầy đủ.
--
-- Chạy: docker exec -i kite-postgres psql -U kitehub -d kiteclass_shared < kitehub/scripts/seed-toan10a1-demo.sql
-- Login test: teacher_a@test.com / Test@12345  → /attendance/reports → chọn "Lớp Toán 10A1".

-- ============================================================
-- 1. Class + 10 học sinh + enrollment (plpgsql để reuse course_id + idempotent).
-- ============================================================
DO $$
DECLARE
    v_inst    uuid   := 'aaaabbbb-0000-0000-0000-000000000001';
    v_course  bigint;
    v_class   bigint;
    v_teacher uuid;
    v_student bigint;
    i         int;
    v_names   text[] := ARRAY['Nguyễn Văn An','Trần Thị Bình','Lê Hoàng Chi','Phạm Thu Dung','Hoàng Minh Đức',
                              'Vũ Ngọc Phúc','Đỗ Quang Giang','Bùi Thanh Hà','Đặng Hải Khoa','Phan Khánh Linh'];
BEGIN
    -- reuse 1 course có sẵn của tenant cho FK course_id
    SELECT course_id INTO v_course
    FROM classes WHERE instance_id = v_inst AND deleted = false ORDER BY id LIMIT 1;

    -- teacher_id là uuid (user_uuid) — lấy của teacher_a@test.com cho lớp này
    SELECT user_uuid INTO v_teacher
    FROM auth_credentials WHERE email = 'teacher_a@test.com' AND instance_id = v_inst LIMIT 1;

    -- class (idempotent theo tên)
    SELECT id INTO v_class
    FROM classes WHERE instance_id = v_inst AND name = 'Lớp Toán 10A1' AND deleted = false;
    IF v_class IS NULL THEN
        INSERT INTO classes
            (instance_id, course_id, name, teacher_id, start_date, max_students,
             current_enrolled, status, version, deleted, created_at, updated_at)
        VALUES
            (v_inst, v_course, 'Lớp Toán 10A1', v_teacher, CURRENT_DATE - 60, 20,
             0, 'IN_PROGRESS', 0, false, now(), now())
        RETURNING id INTO v_class;
    END IF;

    -- 10 học sinh + enrollment (idempotent theo email demo)
    FOR i IN 1..10 LOOP
        SELECT id INTO v_student
        FROM students WHERE instance_id = v_inst
          AND email = 'toan10a1.hs' || i || '@demo.local' AND deleted = false;
        IF v_student IS NULL THEN
            INSERT INTO students (instance_id, name, email, status, version, deleted, created_at, updated_at)
            VALUES (v_inst, v_names[i], 'toan10a1.hs' || i || '@demo.local', 'ACTIVE', 0, false, now(), now())
            RETURNING id INTO v_student;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM enrollments e
                       WHERE e.class_id = v_class AND e.student_id = v_student AND e.deleted = false) THEN
            INSERT INTO enrollments
                (instance_id, class_id, student_id, status, enrolled_at, enrollment_date,
                 tuition_amount, discount_percent, final_amount, version, deleted, created_at, updated_at)
            VALUES
                (v_inst, v_class, v_student, 'ACTIVE', now(), now(),
                 2000000, 0, 2000000, 0, false, now(), now());
        END IF;
    END LOOP;

    UPDATE classes
    SET current_enrolled = (SELECT count(*) FROM enrollments WHERE class_id = v_class AND deleted = false),
        updated_at = now()
    WHERE id = v_class;
END $$;

-- ============================================================
-- 2. 12 buổi học (COMPLETED, lịch lùi quá khứ ~5 tuần).
-- ============================================================
INSERT INTO class_sessions
    (class_id, session_number, session_date, start_time, end_time,
     topic, status, attendance_taken, instance_id, version, deleted, created_at, updated_at)
SELECT cl.id, g, (CURRENT_DATE - ((12 - g) * 3)), TIME '18:00', TIME '20:00',
       'Buổi ' || g, 'COMPLETED', true, cl.instance_id, 0, false, now(), now()
FROM (SELECT id, instance_id FROM classes
      WHERE instance_id = 'aaaabbbb-0000-0000-0000-000000000001'
        AND name = 'Lớp Toán 10A1' AND deleted = false) cl
CROSS JOIN generate_series(1, 12) AS g
WHERE NOT EXISTS (SELECT 1 FROM class_sessions s
                  WHERE s.class_id = cl.id AND s.session_number = g AND s.deleted = false);

-- ============================================================
-- 3. Điểm danh đầy đủ (enrollment × buổi), status phân bố thực tế, deterministic.
-- ============================================================
INSERT INTO attendance
    (instance_id, session_id, student_id, enrollment_id, status,
     marked_date, marked_at, points_awarded, version, deleted, created_at, updated_at)
SELECT e.instance_id, s.id, e.student_id, e.id, pick.status,
       (s.session_date + s.start_time), (s.session_date + s.start_time),
       pick.points, 0, false, now(), now()
FROM enrollments e
JOIN classes c        ON c.id = e.class_id
                     AND c.instance_id = 'aaaabbbb-0000-0000-0000-000000000001'
                     AND c.name = 'Lớp Toán 10A1' AND c.deleted = false
JOIN class_sessions s ON s.class_id = e.class_id AND s.deleted = false
CROSS JOIN LATERAL (
    SELECT CASE WHEN r < 80 THEN 'PRESENT' WHEN r < 88 THEN 'LATE'
                WHEN r < 95 THEN 'ABSENT' ELSE 'EXCUSED' END AS status,
           CASE WHEN r < 80 THEN 0 WHEN r < 88 THEN -5
                WHEN r < 95 THEN -10 ELSE 0 END             AS points
    FROM (SELECT (('x' || substr(md5(e.id::text || '-' || s.id::text), 1, 6))::bit(24)::int) % 100 AS r) h
) AS pick
WHERE e.deleted = false
  AND NOT EXISTS (SELECT 1 FROM attendance a
                  WHERE a.enrollment_id = e.id AND a.session_id = s.id AND a.deleted = false);

-- Tổng kết lớp Toán 10A1 (tenant teacher_a)
SELECT c.id, c.name, c.status,
       (SELECT count(*) FROM enrollments e WHERE e.class_id = c.id AND e.deleted = false)   AS hoc_sinh,
       (SELECT count(*) FROM class_sessions s WHERE s.class_id = c.id AND s.deleted = false) AS buoi,
       (SELECT count(*) FROM attendance a JOIN enrollments e2 ON a.enrollment_id = e2.id
        WHERE e2.class_id = c.id AND a.deleted = false)                                      AS diem_danh
FROM classes c
WHERE c.instance_id = 'aaaabbbb-0000-0000-0000-000000000001'
  AND c.name = 'Lớp Toán 10A1' AND c.deleted = false;

-- ============================================================
-- 4. Reset password teacher_a@test.com -> Test@12345 (bcrypt) để test đăng nhập.
--    Hash bcrypt self-contained (verify trên mọi DB). Chỉ tài khoản test.
-- ============================================================
UPDATE auth_credentials
SET password_hash = '$2b$10$lDf1LjHMLRImbleevPXhye70vXD7XNpnG8P6PdyHE9YyC457FpbZ2', updated_at = now()
WHERE email = 'teacher_a@test.com';

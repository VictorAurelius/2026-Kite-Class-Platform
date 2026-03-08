-- V16__seed_test_data.sql
-- Seed test data for local development and testing
-- This migration creates sample data across all modules for testing workflows

-- =====================================================
-- 1. Instance (Tenant) Data
-- =====================================================
-- Create test tenant: "Demo School"
INSERT INTO instances (id, name, domain, status, max_students, max_teachers, max_storage_mb, created_at, updated_at, deleted)
VALUES
    ('11111111-1111-1111-1111-111111111111'::uuid,
     'Demo School',
     'demo.kiteclass.com',
     'ACTIVE',
     1000,
     50,
     10240, -- 10GB
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP,
     false);

-- =====================================================
-- 2. Teacher Data
-- =====================================================
INSERT INTO teachers (id, instance_id, email, name, phone, specialization, bio, status, hire_date, created_at, updated_at, deleted)
VALUES
    -- Main teacher for Math courses
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 'john.math@demo.com', 'John Smith', '+84901234567',
     'Mathematics', 'Experienced Math teacher with 15 years of teaching', 'ACTIVE', '2020-01-15',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Main teacher for English courses
    (2, '11111111-1111-1111-1111-111111111111'::uuid, 'jane.english@demo.com', 'Jane Doe', '+84901234568',
     'English Literature', 'Passionate about English literature and creative writing', 'ACTIVE', '2019-08-20',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Main teacher for Science courses
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 'david.science@demo.com', 'David Chen', '+84901234569',
     'Physics & Chemistry', 'PhD in Physics, loves hands-on experiments', 'ACTIVE', '2021-03-10',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Assistant teacher
    (4, '11111111-1111-1111-1111-111111111111'::uuid, 'sarah.assistant@demo.com', 'Sarah Wilson', '+84901234570',
     'General Education', 'Dedicated assistant teacher helping across multiple subjects', 'ACTIVE', '2022-06-01',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 3. Course Data
-- =====================================================
INSERT INTO courses (id, instance_id, code, name, description, credits, syllabus, status, created_at, updated_at, deleted)
VALUES
    -- Published Math course
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 'MATH101', 'Algebra Fundamentals',
     'Introduction to algebraic concepts including equations, functions, and graphs',
     3.0, 'Week 1-4: Linear equations; Week 5-8: Quadratic functions; Week 9-12: Systems of equations; Week 13-15: Graphing; Week 16: Final exam',
     'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Published English course
    (2, '11111111-1111-1111-1111-111111111111'::uuid, 'ENG201', 'English Literature',
     'Study of classic and modern literature with focus on critical analysis',
     4.0, 'Week 1-3: Poetry analysis; Week 4-7: Short stories; Week 8-11: Novels; Week 12-15: Drama; Week 16: Portfolio submission',
     'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Published Science course
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 'SCI301', 'General Physics',
     'Comprehensive introduction to mechanics, thermodynamics, and electromagnetism',
     4.0, 'Week 1-5: Mechanics; Week 6-10: Thermodynamics; Week 11-15: Electromagnetism; Week 16: Lab final',
     'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Draft course (not yet active)
    (4, '11111111-1111-1111-1111-111111111111'::uuid, 'CS101', 'Introduction to Programming',
     'Beginner-friendly programming course using Python',
     3.0, 'Draft syllabus - to be finalized',
     'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 4. Teacher-Course Assignments
-- =====================================================
INSERT INTO teacher_courses (id, teacher_id, course_id, role, assigned_at, created_at, updated_at, deleted)
VALUES
    (1, 1, 1, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, 2, 2, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, 3, 3, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (4, 4, 1, 'ASSISTANT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 5. Class Data
-- =====================================================
INSERT INTO classes (id, instance_id, course_id, class_code, class_name, semester, academic_year,
                     schedule, start_date, end_date, max_students, status, created_at, updated_at, deleted)
VALUES
    -- Active Math class
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 1, 'MATH101-A', 'Algebra Fundamentals - Section A',
     'Spring 2026', 2026,
     '[{"dayOfWeek":"MONDAY","startTime":"08:00","endTime":"09:30","room":"Room 101"},{"dayOfWeek":"WEDNESDAY","startTime":"08:00","endTime":"09:30","room":"Room 101"}]'::jsonb,
     '2026-01-15', '2026-05-15', 30,
     'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Active English class
    (2, '11111111-1111-1111-1111-111111111111'::uuid, 2, 'ENG201-A', 'English Literature - Section A',
     'Spring 2026', 2026,
     '[{"dayOfWeek":"TUESDAY","startTime":"10:00","endTime":"11:30","room":"Room 202"},{"dayOfWeek":"THURSDAY","startTime":"10:00","endTime":"11:30","room":"Room 202"}]'::jsonb,
     '2026-01-20', '2026-05-20', 25,
     'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Scheduled Science class (future)
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 3, 'SCI301-A', 'General Physics - Section A',
     'Summer 2026', 2026,
     '[{"dayOfWeek":"MONDAY","startTime":"14:00","endTime":"16:00","room":"Lab 301"},{"dayOfWeek":"FRIDAY","startTime":"14:00","endTime":"16:00","room":"Lab 301"}]'::jsonb,
     '2026-06-01', '2026-08-15', 20,
     'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 6. Teacher-Class Assignments
-- =====================================================
INSERT INTO teacher_classes (id, teacher_id, class_id, role, assigned_at, created_at, updated_at, deleted)
VALUES
    (1, 1, 1, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, 4, 1, 'ASSISTANT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, 2, 2, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (4, 3, 3, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 7. Class Sessions (auto-generated from schedules)
-- =====================================================
-- Math class sessions (16 sessions over 16 weeks)
INSERT INTO class_sessions (id, class_id, session_number, scheduled_date, start_time, end_time, topic, room,
                            status, actual_start_time, actual_end_time, notes, created_at, updated_at, deleted)
VALUES
    (1, 1, 1, '2026-01-15', '08:00', '09:30', 'Introduction to Algebra', 'Room 101', 'COMPLETED', '08:00', '09:30', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, 1, 2, '2026-01-20', '08:00', '09:30', 'Linear Equations', 'Room 101', 'COMPLETED', '08:05', '09:35', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, 1, 3, '2026-01-22', '08:00', '09:30', 'Solving Equations', 'Room 101', 'COMPLETED', '08:00', '09:30', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (4, 1, 4, '2026-01-27', '08:00', '09:30', 'Functions Basics', 'Room 101', 'IN_PROGRESS', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (5, 1, 5, '2026-01-29', '08:00', '09:30', 'Quadratic Functions', 'Room 101', 'SCHEDULED', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- English class sessions (12 sessions)
INSERT INTO class_sessions (id, class_id, session_number, scheduled_date, start_time, end_time, topic, room,
                            status, actual_start_time, actual_end_time, notes, created_at, updated_at, deleted)
VALUES
    (6, 2, 1, '2026-01-21', '10:00', '11:30', 'Introduction to Poetry', 'Room 202', 'COMPLETED', '10:00', '11:30', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (7, 2, 2, '2026-01-23', '10:00', '11:30', 'Analyzing Sonnets', 'Room 202', 'COMPLETED', '10:00', '11:30', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (8, 2, 3, '2026-01-28', '10:00', '11:30', 'Modern Poetry', 'Room 202', 'SCHEDULED', NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 8. Student Data
-- =====================================================
INSERT INTO students (id, instance_id, email, name, date_of_birth, phone, address, enrollment_date,
                     status, created_at, updated_at, deleted)
VALUES
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 'alice.student@demo.com', 'Alice Johnson',
     '2008-05-15', '+84912345671', '123 Main St, Hanoi', '2024-09-01',
     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (2, '11111111-1111-1111-1111-111111111111'::uuid, 'bob.student@demo.com', 'Bob Lee',
     '2008-08-22', '+84912345672', '456 Oak Ave, Hanoi', '2024-09-01',
     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (3, '11111111-1111-1111-1111-111111111111'::uuid, 'charlie.student@demo.com', 'Charlie Brown',
     '2008-11-10', '+84912345673', '789 Pine Rd, Hanoi', '2024-09-01',
     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (4, '11111111-1111-1111-1111-111111111111'::uuid, 'diana.student@demo.com', 'Diana Prince',
     '2008-03-28', '+84912345674', '321 Elm St, Hanoi', '2024-09-01',
     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (5, '11111111-1111-1111-1111-111111111111'::uuid, 'ethan.student@demo.com', 'Ethan Hunt',
     '2008-07-04', '+84912345675', '654 Maple Dr, Hanoi', '2024-09-01',
     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 9. Enrollment Data
-- =====================================================
INSERT INTO enrollments (id, instance_id, student_id, class_id, enrollment_date, status,
                        payment_status, grade, version, created_at, updated_at, deleted)
VALUES
    -- Math class enrollments
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 1, 1, '2026-01-10', 'ACTIVE', 'PAID', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, '11111111-1111-1111-1111-111111111111'::uuid, 2, 1, '2026-01-10', 'ACTIVE', 'PAID', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 3, 1, '2026-01-10', 'ACTIVE', 'PENDING', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- English class enrollments
    (4, '11111111-1111-1111-1111-111111111111'::uuid, 1, 2, '2026-01-15', 'ACTIVE', 'PAID', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (5, '11111111-1111-1111-1111-111111111111'::uuid, 4, 2, '2026-01-15', 'ACTIVE', 'PAID', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Science class enrollments (future class)
    (6, '11111111-1111-1111-1111-111111111111'::uuid, 2, 3, '2026-05-20', 'ACTIVE', 'PENDING', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (7, '11111111-1111-1111-1111-111111111111'::uuid, 5, 3, '2026-05-20', 'ACTIVE', 'PENDING', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 10. Grading Scale Data (Standard 4.0 scale)
-- =====================================================
INSERT INTO grading_scales (id, instance_id, scale_name, letter_grade, min_score, max_score, gpa_value, description,
                           created_at, updated_at, deleted)
VALUES
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 'Standard', 'A', 90.0, 100.0, 4.0, 'Excellent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, '11111111-1111-1111-1111-111111111111'::uuid, 'Standard', 'B+', 85.0, 89.9, 3.5, 'Very Good', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 'Standard', 'B', 80.0, 84.9, 3.0, 'Good', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (4, '11111111-1111-1111-1111-111111111111'::uuid, 'Standard', 'C+', 75.0, 79.9, 2.5, 'Above Average', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (5, '11111111-1111-1111-1111-111111111111'::uuid, 'Standard', 'C', 70.0, 74.9, 2.0, 'Average', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (6, '11111111-1111-1111-1111-111111111111'::uuid, 'Standard', 'D', 60.0, 69.9, 1.0, 'Below Average', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (7, '11111111-1111-1111-1111-111111111111'::uuid, 'Standard', 'F', 0.0, 59.9, 0.0, 'Fail', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 11. Grade Data (auto-initialized for enrollments)
-- =====================================================
INSERT INTO grades (id, instance_id, student_id, class_id, status, pass_threshold, final_score, letter_grade,
                   gpa, calculated_at, finalized_at, finalized_by, created_at, updated_at, deleted)
VALUES
    -- Grades for Math class (some finalized, some in progress)
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 1, 1, 'IN_PROGRESS', 50.0, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, '11111111-1111-1111-1111-111111111111'::uuid, 2, 1, 'IN_PROGRESS', 50.0, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 3, 1, 'IN_PROGRESS', 50.0, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Grades for English class
    (4, '11111111-1111-1111-1111-111111111111'::uuid, 1, 2, 'IN_PROGRESS', 50.0, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (5, '11111111-1111-1111-1111-111111111111'::uuid, 4, 2, 'IN_PROGRESS', 50.0, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 12. Invoice Data
-- =====================================================
INSERT INTO invoices (id, instance_id, student_id, class_id, invoice_number, issue_date, due_date,
                     amount, discount, tax, total_amount, payment_status, notes, created_at, updated_at, deleted)
VALUES
    -- Paid invoices
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 1, 1, 'INV-2026-001', '2026-01-10', '2026-01-20',
     5000000, 0, 0, 5000000, 'PAID', 'Algebra Fundamentals tuition', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (2, '11111111-1111-1111-1111-111111111111'::uuid, 2, 1, 'INV-2026-002', '2026-01-10', '2026-01-20',
     5000000, 500000, 0, 4500000, 'PAID', 'Early bird discount applied', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (4, '11111111-1111-1111-1111-111111111111'::uuid, 1, 2, 'INV-2026-004', '2026-01-15', '2026-01-25',
     6000000, 0, 0, 6000000, 'PAID', 'English Literature tuition', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (5, '11111111-1111-1111-1111-111111111111'::uuid, 4, 2, 'INV-2026-005', '2026-01-15', '2026-01-25',
     6000000, 0, 0, 6000000, 'PAID', 'English Literature tuition', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Pending invoices
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 3, 1, 'INV-2026-003', '2026-01-10', '2026-01-20',
     5000000, 0, 0, 5000000, 'PENDING', 'Payment reminder sent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (6, '11111111-1111-1111-1111-111111111111'::uuid, 2, 3, 'INV-2026-006', '2026-05-20', '2026-05-30',
     7000000, 0, 0, 7000000, 'PENDING', 'General Physics tuition - future', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 13. Invoice Line Items
-- =====================================================
INSERT INTO invoice_line_items (id, invoice_id, description, quantity, unit_price, total_price, created_at, updated_at, deleted)
VALUES
    (1, 1, 'Tuition Fee - Algebra Fundamentals (Spring 2026)', 1, 4500000, 4500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, 1, 'Registration Fee', 1, 500000, 500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (3, 2, 'Tuition Fee - Algebra Fundamentals (Spring 2026)', 1, 4500000, 4500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (4, 2, 'Registration Fee', 1, 500000, 500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (5, 2, 'Early Bird Discount', 1, -500000, -500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (6, 3, 'Tuition Fee - Algebra Fundamentals (Spring 2026)', 1, 4500000, 4500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (7, 3, 'Registration Fee', 1, 500000, 500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (8, 4, 'Tuition Fee - English Literature (Spring 2026)', 1, 5500000, 5500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (9, 4, 'Registration Fee', 1, 500000, 500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (10, 5, 'Tuition Fee - English Literature (Spring 2026)', 1, 5500000, 5500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (11, 5, 'Registration Fee', 1, 500000, 500000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 14. Payment Data (for paid invoices)
-- =====================================================
INSERT INTO payments (id, instance_id, invoice_id, payment_method, transaction_id, amount, payment_date,
                     status, gateway_response, notes, created_at, updated_at, deleted)
VALUES
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 1, 'BANK_TRANSFER', 'TXN-2026-001', 5000000,
     '2026-01-12 10:30:00', 'COMPLETED', '{"bank":"Vietcombank","account":"123456789"}',
     'Payment received via bank transfer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (2, '11111111-1111-1111-1111-111111111111'::uuid, 2, 'BANK_TRANSFER', 'TXN-2026-002', 4500000,
     '2026-01-11 14:20:00', 'COMPLETED', '{"bank":"Techcombank","account":"987654321"}',
     'Payment with early bird discount', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (3, '11111111-1111-1111-1111-111111111111'::uuid, 4, 'CASH', 'CASH-2026-001', 6000000,
     '2026-01-16 09:15:00', 'COMPLETED', '{}',
     'Cash payment at reception', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    (4, '11111111-1111-1111-1111-111111111111'::uuid, 5, 'BANK_TRANSFER', 'TXN-2026-003', 6000000,
     '2026-01-17 11:45:00', 'COMPLETED', '{"bank":"ACB","account":"456789123"}',
     'Payment received', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 15. Attendance Data (for completed sessions)
-- =====================================================
INSERT INTO attendance_records (id, instance_id, session_id, student_id, status, check_in_time, notes,
                               created_at, updated_at, deleted)
VALUES
    -- Session 1 (Math class)
    (1, '11111111-1111-1111-1111-111111111111'::uuid, 1, 1, 'PRESENT', '2026-01-15 08:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, '11111111-1111-1111-1111-111111111111'::uuid, 1, 2, 'PRESENT', '2026-01-15 08:02:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, '11111111-1111-1111-1111-111111111111'::uuid, 1, 3, 'LATE', '2026-01-15 08:15:00', '15 minutes late', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Session 2 (Math class)
    (4, '11111111-1111-1111-1111-111111111111'::uuid, 2, 1, 'PRESENT', '2026-01-20 08:05:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (5, '11111111-1111-1111-1111-111111111111'::uuid, 2, 2, 'PRESENT', '2026-01-20 08:03:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (6, '11111111-1111-1111-1111-111111111111'::uuid, 2, 3, 'ABSENT', NULL, 'Sick leave approved', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Session 3 (Math class)
    (7, '11111111-1111-1111-1111-111111111111'::uuid, 3, 1, 'PRESENT', '2026-01-22 08:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (8, '11111111-1111-1111-1111-111111111111'::uuid, 3, 2, 'PRESENT', '2026-01-22 08:01:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (9, '11111111-1111-1111-1111-111111111111'::uuid, 3, 3, 'PRESENT', '2026-01-22 08:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Session 6 (English class)
    (10, '11111111-1111-1111-1111-111111111111'::uuid, 6, 1, 'PRESENT', '2026-01-21 10:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (11, '11111111-1111-1111-1111-111111111111'::uuid, 6, 4, 'PRESENT', '2026-01-21 10:05:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Session 7 (English class)
    (12, '11111111-1111-1111-1111-111111111111'::uuid, 7, 1, 'PRESENT', '2026-01-23 10:00:00', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (13, '11111111-1111-1111-1111-111111111111'::uuid, 7, 4, 'LATE', '2026-01-23 10:20:00', '20 minutes late', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- END OF SEED DATA
-- =====================================================

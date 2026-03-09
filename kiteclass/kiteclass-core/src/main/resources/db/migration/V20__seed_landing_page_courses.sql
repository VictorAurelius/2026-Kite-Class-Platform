-- V20__seed_landing_page_courses.sql
-- Enhanced seed data for landing pages with full course details and LMS content
-- @since 3.4

-- =====================================================
-- 1. Update existing courses with landing page fields
-- =====================================================
UPDATE courses
SET
    price = 3000000,
    duration_weeks = 12,
    total_sessions = 24,
    max_students = 20,
    level = 'Beginner',
    objectives = '["Hiểu rõ các khái niệm đại số cơ bản","Giải quyết các phương trình tuyến tính và bậc hai","Vẽ đồ thị hàm số một cách chính xác","Áp dụng kiến thức vào bài toán thực tế"]'::text,
    cover_image_url = NULL
WHERE id = 1; -- MATH101

UPDATE courses
SET
    price = 3500000,
    duration_weeks = 16,
    total_sessions = 32,
    max_students = 25,
    level = 'Intermediate',
    objectives = '["Phân tích tác phẩm văn học một cách sâu sắc","Viết luận văn chuyên nghiệp","Hiểu bối cảnh lịch sử và văn hóa","Trình bày ý kiến một cách logic"]'::text,
    cover_image_url = NULL
WHERE id = 2; -- ENG201

UPDATE courses
SET
    price = 4000000,
    duration_weeks = 16,
    total_sessions = 32,
    max_students = 15,
    level = 'Advanced',
    objectives = '["Nắm vững các định luật vật lý cơ bản","Thực hành thí nghiệm an toàn và chính xác","Giải quyết bài toán phức tạp","Áp dụng lý thuyết vào thực tiễn"]'::text,
    cover_image_url = NULL
WHERE id = 3; -- SCI301

-- =====================================================
-- 2. Add Vietnamese courses for landing page demo
-- =====================================================
INSERT INTO courses (
    id, instance_id, code, name, description, syllabus, status,
    price, duration_weeks, total_sessions, max_students, level, objectives,
    created_at, updated_at, deleted
)
VALUES
    -- Tiếng Anh Giao Tiếp Cơ Bản
    (5, '11111111-1111-1111-1111-111111111111'::uuid,
     'ENG-101', 'Tiếng Anh Giao Tiếp Cơ Bản',
     'Khóa học tiếng Anh giao tiếp dành cho người mới bắt đầu. Tập trung vào kỹ năng nghe - nói trong các tình huống hàng ngày như: tự giới thiệu, hỏi đường, đặt hàng, mua sắm, v.v.',
     'Chương trình học bao gồm 12 chủ đề chính từ cơ bản đến nâng cao: Giới thiệu bản thân, Gia đình, Mua sắm, Ăn uống, Du lịch, Y tế, Giao thông, Công việc, Giải trí, Thể thao, Môi trường, Công nghệ.',
     'PUBLISHED',
     3000000, -- 3 triệu VNĐ
     12, -- 12 tuần
     24, -- 24 buổi
     20, -- max 20 học viên
     'Beginner',
     '["Giao tiếp cơ bản trong các tình huống hàng ngày","Phát âm chuẩn và tự tin khi nói","Nghe hiểu đoạn hội thoại ngắn","Viết email và tin nhắn đơn giản"]'::text,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Tiếng Anh Thương Mại
    (6, '11111111-1111-1111-1111-111111111111'::uuid,
     'ENG-201', 'Tiếng Anh Thương Mại',
     'Khóa học tiếng Anh dành cho người đi làm, tập trung vào kỹ năng giao tiếp trong môi trường công sở: họp hành, thuyết trình, email, đàm phán.',
     'Nội dung: Email công việc chuyên nghiệp, Họp và thảo luận, Thuyết trình hiệu quả, Đàm phán kinh doanh, Điện thoại công việc, Viết báo cáo.',
     'PUBLISHED',
     4500000, -- 4.5 triệu VNĐ
     16, -- 16 tuần
     32, -- 32 buổi
     15, -- max 15 học viên
     'Intermediate',
     '["Viết email công việc chuyên nghiệp","Tự tin thuyết trình bằng tiếng Anh","Tham gia họp và thảo luận hiệu quả","Đàm phán và thương lượng thành công"]'::text,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- IELTS 7.0+
    (7, '11111111-1111-1111-1111-111111111111'::uuid,
     'IELTS-ADV', 'IELTS 7.0+ Chuyên Sâu',
     'Khóa học IELTS chuyên sâu giúp học viên đạt band 7.0 trở lên với lộ trình học tập cá nhân hóa, luyện đề thực chiến và feedback chi tiết từ giáo viên.',
     '4 kỹ năng: Listening (Academic + General), Reading (skimming, scanning, inference), Writing (Task 1 + Task 2), Speaking (Part 1, 2, 3). Luyện đề Cambridge IELTS 14-18.',
     'PUBLISHED',
     8000000, -- 8 triệu VNĐ
     20, -- 20 tuần
     60, -- 60 buổi (3 buổi/tuần)
     10, -- max 10 học viên (lớp nhỏ)
     'Advanced',
     '["Đạt band điểm IELTS 7.0 trở lên","Thành thạo 4 kỹ năng Listening, Reading, Writing, Speaking","Làm quen với format và chiến lược làm bài","Tự tin trong phòng thi"]'::text,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),

    -- Tiếng Anh Thiếu Nhi
    (8, '11111111-1111-1111-1111-111111111111'::uuid,
     'KID-ENG', 'Tiếng Anh Thiếu Nhi (6-12 tuổi)',
     'Khóa học tiếng Anh vui nhộn dành cho trẻ em 6-12 tuổi với phương pháp học qua chơi, giúp bé yêu thích tiếng Anh và tự tin giao tiếp.',
     'Học qua: Game, Bài hát, Truyện tranh, Video hoạt hình, Hoạt động nhóm. Chủ đề: Động vật, Màu sắc, Số đếm, Gia đình, Trường học, Đồ chơi, Thức ăn.',
     'PUBLISHED',
     2500000, -- 2.5 triệu VNĐ
     12, -- 12 tuần
     24, -- 24 buổi
     12, -- max 12 học viên (lớp nhỏ cho trẻ)
     'Beginner',
     '["Làm quen với tiếng Anh qua trò chơi","Phát âm chuẩn từ nhỏ","Tự tin giao tiếp đơn giản","Yêu thích học tiếng Anh"]'::text,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 3. Assign teachers to new Vietnamese courses
-- =====================================================
INSERT INTO teacher_courses (id, teacher_id, course_id, role, assigned_at, created_at, updated_at, deleted)
VALUES
    (5, 2, 5, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false), -- Jane teaches Basic English
    (6, 2, 6, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false), -- Jane teaches Business English
    (7, 2, 7, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false), -- Jane teaches IELTS
    (8, 2, 8, 'MAIN_TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false); -- Jane teaches Kids English

-- =====================================================
-- 4. LMS Modules for "Tiếng Anh Giao Tiếp Cơ Bản" (Course 5)
-- =====================================================
INSERT INTO lms_modules (id, course_id, title, description, order_number, is_published, created_at, updated_at, deleted)
VALUES
    (1, 5, 'Module 1: Giới thiệu bản thân', 'Học cách tự giới thiệu, hỏi thăm và làm quen với người mới', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, 5, 'Module 2: Tình huống hàng ngày', 'Giao tiếp trong nhà hàng, cửa hàng, hỏi đường', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, 5, 'Module 3: Ngữ pháp cơ bản', 'Các thì cơ bản: Hiện tại đơn, Hiện tại tiếp diễn, Quá khứ đơn', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 5. LMS Lessons for Module 1
-- =====================================================
INSERT INTO lms_lessons (id, module_id, title, content, order_number, duration_minutes, lesson_type, is_trial, is_published, created_at, updated_at, deleted)
VALUES
    (1, 1, 'Lesson 1: Greetings', '{"type":"video","url":"https://example.com/greetings.mp4","description":"Học cách chào hỏi bằng tiếng Anh"}', 1, 30, 'VIDEO', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (2, 1, 'Lesson 2: Self Introduction', '{"type":"text","content":"Hi, my name is... I am from... I like... Nice to meet you!"}', 2, 45, 'TEXT', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (3, 1, 'Lesson 3: Talking about hobbies', '{"type":"text","content":"I like playing sports. My hobby is reading. I enjoy listening to music."}', 3, 45, 'TEXT', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 6. LMS Lessons for Module 2
-- =====================================================
INSERT INTO lms_lessons (id, module_id, title, content, order_number, duration_minutes, lesson_type, is_trial, is_published, created_at, updated_at, deleted)
VALUES
    (4, 2, 'Lesson 4: At the restaurant', '{"type":"text","content":"Can I have the menu, please? I would like to order... May I have the bill?"}', 1, 45, 'TEXT', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (5, 2, 'Lesson 5: Shopping', '{"type":"text","content":"How much is this? Can I try this on? Do you have this in a different color?"}', 2, 45, 'TEXT', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (6, 2, 'Lesson 6: Asking for directions', '{"type":"text","content":"Excuse me, how do I get to...? Where is the nearest...? Is it far from here?"}', 3, 45, 'TEXT', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- 7. LMS Lessons for Module 3
-- =====================================================
INSERT INTO lms_lessons (id, module_id, title, content, order_number, duration_minutes, lesson_type, is_trial, is_published, created_at, updated_at, deleted)
VALUES
    (7, 3, 'Lesson 7: Present Simple', '{"type":"quiz","questions":[{"q":"I ___ to school every day","options":["go","goes","going"],"answer":0}]}', 1, 60, 'QUIZ', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (8, 3, 'Lesson 8: Present Continuous', '{"type":"text","content":"I am studying. He is playing. They are watching TV."}', 2, 60, 'TEXT', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
    (9, 3, 'Lesson 9: Past Simple', '{"type":"text","content":"I went to the park yesterday. She ate breakfast this morning. We played soccer last week."}', 3, 60, 'TEXT', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE lms_modules IS 'Added sample LMS modules for landing page course structure preview';
COMMENT ON TABLE lms_lessons IS 'Added sample lessons with trial/locked status for guest users';

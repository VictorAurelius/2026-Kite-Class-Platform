-- Seed nội dung landing đầy đủ cho 3 giảng viên độc lập (wave-thesis-4).
-- Chạy SAU khi migration V76 (landing_sections) đã apply vào kiteclass_shared.
-- Mỗi GV: about / teachers / programs / pricingTiers / testimonials / faqs / stats.
-- Nội dung theo môn: cô Khánh (Pháp luật THPT) / cô Hà (Toán Tiểu học) / thầy Nhì (Hóa THCS).
-- VN-localization: VND 1.500.000đ, tên VN, không IELTS/TOEIC, "phát triển sau".
--
-- Usage: docker exec -i kite-postgres psql -U kitehub -d kiteclass_shared < kitehub/scripts/seed-landing-content.sql
--
-- ⚠️ SAU KHI CHẠY: PHẢI evict Spring @Cacheable Redis (landing response cache theo tenantId)
-- nếu không landing GET trả stale (sections null). Lệnh:
--   docker exec kite-redis redis-cli --scan --pattern "landingPages*" | xargs -r -I{} docker exec kite-redis redis-cli DEL "{}"
--
-- Banner: 3 PNG đã copy vào kiteclass-frontend/public/demo-banners/ (hero_image_url trỏ tới).
-- Reproduce note: nội dung này lý tưởng nên set qua PUT /api/v1/tenants/{id}/landing
-- (admin/teacher) sau khi BE expose request fields (GAP-815); SQL trực tiếp dùng cho demo local.

-- ============================================================
-- 1. Cô Đỗ Lan Khánh — THPT, Pháp luật & Đời sống (navy + gold)
-- ============================================================
UPDATE landing_pages SET
  about_text = 'Tôi là Đỗ Lan Khánh, giảng viên độc lập với 9 năm kinh nghiệm luyện thi môn Giáo dục công dân và Pháp luật bậc THPT. Lớp học tập trung vào tư duy pháp lý thực tiễn, giúp học sinh tự tin trong kỳ thi tốt nghiệp và vững kiến thức cho cuộc sống.',
  teachers = '[{"name":"Đỗ Lan Khánh","subject":"Pháp luật & Giáo dục công dân (THPT)","credentials":["Cử nhân Luật","9 năm luyện thi tốt nghiệp THPT","Chuyên đề Pháp luật đời sống"]}]'::jsonb,
  programs = '[{"name":"GDCD lớp 12 — Luyện thi tốt nghiệp","description":"Bám sát cấu trúc đề thi tốt nghiệp THPT môn Giáo dục công dân","detail":["Lý thuyết trọng tâm","Luyện đề theo chuyên đề","Mẹo xử lý câu vận dụng cao"]},{"name":"Pháp luật & Đời sống lớp 10-11","description":"Nền tảng kiến thức pháp luật phổ thông gắn tình huống thực tế","detail":["Quyền và nghĩa vụ công dân","Tình huống pháp lý đời sống","Kỹ năng phân tích điều luật"]},{"name":"Chuyên đề ôn cấp tốc","description":"Ôn tập tăng tốc giai đoạn nước rút trước kỳ thi","detail":["Hệ thống hóa kiến thức","Đề thi thử bám sát","Chữa đề chi tiết"]}]'::jsonb,
  pricing_tiers = '[{"name":"Cơ bản","price":"1.500.000đ","period":"/tháng","features":["2 buổi/tuần","Tài liệu chuyên đề","Bài tập về nhà","Hỗ trợ qua Zalo"],"highlighted":false},{"name":"Luyện thi","price":"2.800.000đ","period":"/khóa","features":["3 buổi/tuần","Luyện đề tốt nghiệp","Thi thử định kỳ","Chữa đề chi tiết"],"highlighted":true},{"name":"Cấp tốc 1-1","price":"4.500.000đ","period":"/khóa","features":["Kèm riêng 1-1","Lộ trình cá nhân hóa","Cam kết tiến bộ","Ôn sát kỳ thi"],"highlighted":false}]'::jsonb,
  testimonials = '[{"author":"Chị Phạm Thị Mai","role":"Phụ huynh em Trần Thị Hồng — lớp 12A1","content":"Con tôi từ sợ môn GDCD nay đã đạt 9 điểm thi thử. Cô Khánh dạy dễ hiểu, gắn với đời sống.","rating":5},{"author":"Em Nguyễn Văn An","role":"Học sinh lớp 12 — đạt 9,25 GDCD tốt nghiệp","content":"Cô luyện đề rất sát, mẹo làm câu vận dụng cao giúp em làm bài nhanh và chắc.","rating":5},{"author":"Chị Lê Thị Quỳnh","role":"Phụ huynh em Lê Văn Quang — lớp 11","content":"Lớp ít học sinh nên con được kèm sát, kiến thức pháp luật áp dụng được vào thực tế.","rating":5}]'::jsonb,
  faqs = '[{"question":"Lớp học phù hợp với học sinh khối nào?","answer":"Lớp nhận học sinh THPT khối 10, 11, 12, đặc biệt tập trung luyện thi tốt nghiệp môn Giáo dục công dân cho lớp 12."},{"question":"Có học thử miễn phí không?","answer":"Có. Học sinh được học thử 1 buổi miễn phí trước khi quyết định đăng ký khóa."},{"question":"Hình thức thanh toán học phí?","answer":"Thanh toán qua chuyển khoản ngân hàng hoặc quét mã VietQR. Có hỗ trợ chia học phí theo đợt."},{"question":"Lịch học thế nào?","answer":"Các buổi học vào buổi tối trong tuần và cuối tuần, sắp xếp linh hoạt theo lịch học chính khóa của học sinh."}]'::jsonb,
  stats = '[{"value":"9+","label":"Năm kinh nghiệm"},{"value":"500+","label":"Học sinh đã luyện thi"},{"value":"95%","label":"Đạt mục tiêu điểm số"}]'::jsonb
WHERE instance_id = '126eaa8c-1f63-4c30-81b5-a5921b384b3b';

-- ============================================================
-- 2. Cô Nguyễn Thị Hà — Tiểu học, Toán (blue) — gói FREE
-- ============================================================
UPDATE landing_pages SET
  about_text = 'Tôi là Nguyễn Thị Hà, giáo viên Tin học trường tiểu học công lập và dạy thêm Toán cho học sinh tiểu học ngoài giờ. Lớp học chú trọng xây nền tảng tư duy Toán vững chắc qua trò chơi và bài tập trực quan, phù hợp lứa tuổi nhỏ.',
  teachers = '[{"name":"Nguyễn Thị Hà","subject":"Toán Tiểu học","credentials":["Cử nhân Sư phạm Toán","5 năm dạy thêm tiểu học","Phương pháp học qua trò chơi"]}]'::jsonb,
  programs = '[{"name":"Toán tư duy lớp 1-3","description":"Xây nền tảng đếm, tính toán và tư duy logic cơ bản","detail":["Phép tính cơ bản","Tư duy hình ảnh","Học qua trò chơi"]},{"name":"Toán nâng cao lớp 4-5","description":"Bồi dưỡng học sinh khá giỏi, chuẩn bị chuyển cấp","detail":["Giải toán có lời văn","Bồi dưỡng tư duy","Ôn tập chuyển cấp"]}]'::jsonb,
  pricing_tiers = '[{"name":"Miễn phí học thử","price":"0đ","period":"/buổi đầu","features":["1 buổi học thử","Đánh giá trình độ","Tư vấn lộ trình"],"highlighted":true},{"name":"Lớp nhóm","price":"1.200.000đ","period":"/tháng","features":["2 buổi/tuần","Lớp nhỏ 6-8 em","Bài tập về nhà","Báo cáo tiến độ"],"highlighted":false}]'::jsonb,
  testimonials = '[{"author":"Chị Trần Thị Bình","role":"Phụ huynh em Hoàng Minh — lớp 4","content":"Con tôi tiến bộ rõ sau 2 tháng, từ sợ Toán nay rất thích học. Cô Hà rất kiên nhẫn.","rating":5},{"author":"Anh Lê Văn Tùng","role":"Phụ huynh em Lê Thảo — lớp 5","content":"Lớp nhỏ, cô kèm sát từng em. Con tôi tự tin hơn hẳn khi làm bài kiểm tra.","rating":5}]'::jsonb,
  faqs = '[{"question":"Con tôi học lớp mấy thì tham gia được?","answer":"Lớp nhận học sinh tiểu học từ lớp 1 đến lớp 5, chia nhóm theo trình độ."},{"question":"Buổi học thử có mất phí không?","answer":"Hoàn toàn miễn phí. Phụ huynh đăng ký để con học thử và được tư vấn lộ trình."},{"question":"Sĩ số lớp bao nhiêu?","answer":"Lớp nhóm nhỏ 6-8 học sinh để đảm bảo cô kèm sát từng em."}]'::jsonb,
  stats = '[{"value":"5+","label":"Năm kinh nghiệm"},{"value":"6-8","label":"Học sinh mỗi lớp"},{"value":"100%","label":"Phụ huynh hài lòng"}]'::jsonb
WHERE instance_id = 'ad0fa96e-af24-49cb-b3e5-19d44f182d85';

-- ============================================================
-- 3. Thầy Nguyễn Đình Nhì — THCS, Hóa (green) — gói PREMIUM
-- ============================================================
UPDATE landing_pages SET
  about_text = 'Tôi là Nguyễn Đình Nhì, giảng viên độc lập với 7 năm kinh nghiệm dạy Hóa học bậc THCS. Lớp học giúp học sinh hiểu bản chất phản ứng hóa học qua thí nghiệm minh họa và phương pháp giải bài tập hệ thống, sẵn sàng cho kỳ thi vào lớp 10.',
  teachers = '[{"name":"Nguyễn Đình Nhì","subject":"Hóa học (THCS)","credentials":["Cử nhân Sư phạm Hóa","7 năm luyện thi vào 10","Phương pháp thí nghiệm trực quan"]}]'::jsonb,
  programs = '[{"name":"Hóa học lớp 8-9","description":"Kiến thức Hóa THCS từ căn bản đến nâng cao","detail":["Bản chất phản ứng","Cân bằng phương trình","Bài tập định lượng"]},{"name":"Luyện thi vào lớp 10","description":"Ôn tập chuyên sâu chuẩn bị thi tuyển sinh vào 10","detail":["Hệ thống chuyên đề","Luyện đề tuyển sinh","Thi thử định kỳ"]},{"name":"Bồi dưỡng học sinh giỏi","description":"Nâng cao cho học sinh dự thi học sinh giỏi","detail":["Bài tập nâng cao","Đề thi HSG các năm","Kèm sát cá nhân"]}]'::jsonb,
  pricing_tiers = '[{"name":"Lớp nhóm","price":"1.800.000đ","period":"/tháng","features":["3 buổi/tuần","Tài liệu chuyên đề","Bài tập định kỳ","Hỗ trợ Zalo"],"highlighted":false},{"name":"Luyện thi vào 10","price":"3.200.000đ","period":"/khóa","features":["Lộ trình chuyên sâu","Luyện đề tuyển sinh","Thi thử hàng tuần","Chữa đề chi tiết"],"highlighted":true},{"name":"Kèm riêng 1-1","price":"5.000.000đ","period":"/khóa","features":["Giáo viên kèm riêng","Lộ trình cá nhân hóa","Cam kết đầu ra","Ôn sát kỳ thi"],"highlighted":false}]'::jsonb,
  testimonials = '[{"author":"Chị Nguyễn Thị Lan","role":"Phụ huynh em Nguyễn Đức — lớp 9","content":"Con tôi đỗ vào trường THPT công lập tốp đầu, điểm Hóa 9,5. Thầy Nhì dạy rất bài bản.","rating":5},{"author":"Em Phạm Thu Hà","role":"Học sinh lớp 9 — đạt giải HSG cấp quận","content":"Thầy giảng bản chất nên em hiểu sâu, làm bài tập khó không còn ngại nữa.","rating":5},{"author":"Anh Trần Minh Đức","role":"Phụ huynh em Trần Bảo — lớp 8","content":"Lớp có thí nghiệm minh họa nên con rất hứng thú, điểm Hóa tăng từ 6 lên 9.","rating":5}]'::jsonb,
  faqs = '[{"question":"Lớp dạy cho học sinh khối nào?","answer":"Lớp nhận học sinh THCS khối 8, 9, đặc biệt tập trung luyện thi tuyển sinh vào lớp 10 và bồi dưỡng học sinh giỏi."},{"question":"Gói PREMIUM khác gói thường thế nào?","answer":"Gói luyện thi và kèm 1-1 có lộ trình chuyên sâu, thi thử thường xuyên, chữa đề chi tiết và cam kết đầu ra."},{"question":"Có buổi học thử không?","answer":"Có. Học sinh được học thử 1 buổi để đánh giá trình độ và phù hợp phương pháp."},{"question":"Thanh toán học phí ra sao?","answer":"Chuyển khoản ngân hàng hoặc mã VietQR. Hỗ trợ chia học phí theo đợt cho khóa dài."}]'::jsonb,
  stats = '[{"value":"7+","label":"Năm kinh nghiệm"},{"value":"300+","label":"Học sinh đã luyện thi"},{"value":"92%","label":"Đỗ vào lớp 10 công lập"}]'::jsonb
WHERE instance_id = '0abe093c-4c66-4c99-abab-a756582dc60b';

-- ============================================================
-- Hero banner (demo) — PNG ở kiteclass-frontend/public/demo-banners/
-- ============================================================
UPDATE landing_pages SET hero_image_url='/demo-banners/co-khanh-phapluat.png' WHERE instance_id='126eaa8c-1f63-4c30-81b5-a5921b384b3b';
UPDATE landing_pages SET hero_image_url='/demo-banners/co-ha-toan.png'        WHERE instance_id='ad0fa96e-af24-49cb-b3e5-19d44f182d85';
UPDATE landing_pages SET hero_image_url='/demo-banners/thay-nhi-hoa.png'      WHERE instance_id='0abe093c-4c66-4c99-abab-a756582dc60b';

-- Template type: 3 GV độc lập → personal (7 section phù hợp cá nhân, không "đội ngũ GV"/gallery/tuyển sinh).
UPDATE landing_pages SET template_type='personal' WHERE instance_id IN (
  '126eaa8c-1f63-4c30-81b5-a5921b384b3b','ad0fa96e-af24-49cb-b3e5-19d44f182d85','0abe093c-4c66-4c99-abab-a756582dc60b');

# Văn nói thuyết trình bảo vệ — bám thesis final (ảnh thật)

> Xuất từ speaker notes của `KiteHub-baove-khoaluan-20slide.pptx` (27 slide, 20 ảnh thật từ thesis-v1.docx). Tổng nói ~18–19 phút. Demo chèn tại slide 20 theo `defense-demo-script.md`.


## Slide 1 — Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo

Chào hội đồng. Em là Nguyễn Văn Kiệt, lớp CNTT1-K63, dưới hướng dẫn của thầy Nguyễn Đức Dư. Em xin trình bày khóa luận: Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo — nền tảng KiteHub. (~30 giây)


## Slide 2 — Nội dung trình bày

Bài trình bày bám bốn chương: tổng quan, thiết kế kiến trúc, cài đặt và triển khai, đánh giá kết quả; khép lại bằng demo và kết luận. (~20 giây)


## Slide 3 — Bối cảnh và vấn đề (§1.1)

Bối cảnh: thị trường lớn, pháp luật hợp pháp hóa dạy thêm, đa số trung tâm nhỏ quản lý thủ công. Ba quan sát hội tụ: nhu cầu, mốc pháp lý cứng, AI vừa đủ chín. (~60 giây)


## Slide 4 — Khảo sát hệ thống tương tự (§1.3)

Khảo sát 5 hệ thống tham khảo (thêm MISA AMIS). Hầu hết đơn-tenant, không AI. KiteHub khai thác khoảng trống đa-tenant gốc + AI Branding phân khúc giá thấp. (~70 giây)


## Slide 5 — Mục tiêu và phạm vi đề tài (§1.7)

Bốn nhóm mục tiêu: chức năng, phi chức năng đo được, pháp lý, phương pháp luận. Phạm vi tập trung mức sẵn sàng cho tenant thực tế. (~60 giây)


## Slide 6 — Đóng góp 1 — Kỹ thuật AI Branding (§1.4)

Đóng góp thứ nhất: tự động hóa nhận diện thương hiệu. Dùng API thương mại thay vì tự host GPU. Pipeline bất đồng bộ không chặn giao diện, có quality gate và dự phòng provider. Bên phải là wizard thực tế. (~80 giây)


## Slide 7 — Tuân thủ pháp luật Việt Nam (§1.5)

Ba trụ cột pháp luật tích hợp từ thiết kế. PDPL mốc cứng 07/2026. Luật An ninh mạng có ngưỡng, chưa chạm, có lộ trình. Thông tư 78 dùng đối tác. (~60 giây)


## Slide 8 — Phương pháp luận hướng chất lượng (§1.6)

Bốn trụ cột có cơ sở lý thuyết. Trụ cột bốn: mỗi sai sót thành quy tắc có kiểm tra. Kim tự tháp kiểm thử minh họa phân bố ba tầng test. (~50 giây)


## Slide 9 — Kiến trúc tổng thể — C4 Level 1 (Hình 2.1)

Kiến trúc tổng thể chia hai mặt phẳng: KiteHub control-plane quản lý vòng đời tenant; KiteClass data-plane phục vụ giáo dục. Chia sẻ một PostgreSQL cô lập bằng RLS. (~70 giây)


## Slide 10 — Phân rã container — C4 Level 2 (Hình 2.2)

KiteHub 6 microservice vì vòng đời khác nhau; KiteClass modular monolith vì domain giáo dục gắn kết chặt. (~60 giây)


## Slide 11 — Đóng góp 2 — Cô lập đa-tenant bằng RLS

So sánh bốn mô hình. Chọn RLS vì chi phí thấp nhất và database engine ép buộc. Đóng góp thứ hai. (~70 giây)


## Slide 12 — Bảo mật nhiều lớp — Defense-in-depth (Hình 2.3)

Nhiều lớp độc lập. RLS là phòng tuyến cuối: quên kiểm tra tầng ứng dụng, database vẫn ép buộc. Phải thủng cả 5 lớp mới rò. (~60 giây)


## Slide 13 — Mô hình dữ liệu — ERD KiteClass (Hình 2.6b)

Mô hình dữ liệu domain giáo dục. ENROLLMENTS phân giải nhiều-nhiều học viên–lớp; điểm danh, điểm, thanh toán gắn quanh đăng ký. Mọi bảng mang tenant_id. (~50 giây)


## Slide 14 — Vòng đời tenant — máy trạng thái (Hình 2.8)

Vòng đời tenant: từ chờ duyệt, sang dùng thử khi cấp magic-link, đến hoạt động chính thức. Sự kiện branding.deploy phát song song dựng template. (~50 giây)


## Slide 15 — Triển khai thực tế — AWS Singapore (Hình 4.1a)

Triển khai thực tế AWS Singapore, VPC tách public/private subnet, RDS trong private subnet. Free Tier cho chi phí ~0. Chưa vượt ngưỡng pháp lý, có lộ trình chuyển vùng. (~60 giây)


## Slide 16 — CI/CD và giám sát vận hành (Hình 4.2a)

CI/CD chuẩn hiện đại: artifact bất biến, OIDC thay key tĩnh, xác nhận thủ công. Giám sát ba lớp; CloudTrail bật trước khi tạo tài nguyên. (~50 giây)


## Slide 17 — Sản phẩm thực tế — giao diện (Chương 3)

Ba giao diện thực tế: trang chủ thương hiệu riêng (minh chứng phân giải Tenant→Domain→Landing), dashboard, quản lý học viên. Sản phẩm chạy thật. (~60 giây)


## Slide 18 — Kết quả AI Branding — gói Miễn phí vs Trả phí

Minh chứng giá trị AI Branding: bên trái gói Miễn phí mẫu dựng sẵn; bên phải gói Trả phí bộ nhận diện sinh tự động qua AI cho môn Hóa, tông màu khác hẳn. Hai tenant thật, hai thương hiệu riêng. (~70 giây)


## Slide 19 — Kết quả đánh giá — các chỉ số chính (Chương 4)

Bốn chỉ số: hiệu năng 86, bảo mật 93, chất lượng 90/110, đều vượt ngưỡng đạt. Mỗi điểm số có audit report evidence block; trajectory cho thấy cải tiến đo được. (~80 giây)


## Slide 20 — Demo trực tiếp

Chuyển sang demo trực tiếp theo kịch bản 6 phase (defense-demo-script.md): khách tham quan, đăng ký onboarding, wizard tạo tenant, chứng minh cô lập bằng 2 tài khoản khác tenant, xem audit log. Dự phòng video. (~30 giây + demo)


**>>> CHÈN DEMO TRỰC TIẾP <<<** — 6 phase theo `defense-demo-script.md`: khách tham quan → đăng ký onboarding → wizard tạo tenant → cô lập đa-tenant (2 tài khoản khác tenant) → audit log → quay lại slide 21. Dự phòng `backup-demo.mp4`.


## Slide 21 — Hạn chế thừa nhận và hướng phát triển

Thừa nhận hạn chế kèm lộ trình tốt hơn che giấu. Mỗi hạn chế có hướng phát triển: nâng cấp hạ tầng, chuyển vùng, mở thanh toán, tích hợp Zalo và hóa đơn điện tử qua đối tác. (~70 giây)


## Slide 22 — Kết luận

Tóm ba đóng góp: AI Branding, đa-tenant RLS, phương pháp luận hướng chất lượng. Sản phẩm triển khai thực tế, đạt ngưỡng đánh giá. Cảm ơn GVHD và hội đồng. (~40 giây)


## Slide 23 — Phụ lục — slide dự phòng hỏi đáp

Slide phụ lục bật khi hội đồng hỏi sâu.


## Slide 24 — Phụ lục A1 — Định tuyến Tenant → Domain → Landing (Hình 2.4c)

Chuỗi định tuyến tenant theo Host: gateway ánh xạ tên miền thành định danh tenant rồi truyền ngữ cảnh xuống lớp dữ liệu cô lập RLS.


## Slide 25 — Phụ lục A2 — ERD KiteHub control-plane (Hình 2.6a)

Mô hình control-plane: INSTANCES quản lý vòng đời tenant, liên kết subscription và các bảng cấu hình.


## Slide 26 — Phụ lục A3 — PDPL 2023: điều luật → tính năng

Năm điều luật mapping 1-1 sang tính năng. Điều 11 audit log bất biến giải bằng bảng không cho sửa/xóa ở cấp database.


## Slide 27 — Phụ lục A4 — Phân tích chi phí

Chi phí hạ tầng gần như 0 nhờ Free Tier; chi phí chính AI Branding ~0,19 USD mỗi lần onboard tenant.

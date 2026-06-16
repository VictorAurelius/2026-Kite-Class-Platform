# Văn nói thuyết trình bảo vệ — bám thesis-v1.docx (bản đã lược) + template UTC

> Xuất từ speaker notes của `KiteHub-baove-khoaluan-20slide.pptx` (22 slide, nền template UTC, diagram nền trắng, nội dung bám `thesis-v1.docx`). Tổng nói ~18 phút. Demo chèn tại slide 20 theo `defense-demo-script.md`.

## Slide 1 — XÂY DỰNG HỆ THỐNG SAAS CUNG CẤP DỊCH VỤ ĐÀO TẠO

Chào hội đồng. Em là Nguyễn Văn Kiệt, lớp CNTT1-K63, dưới hướng dẫn của thầy Nguyễn Đức Dư. Em xin trình bày khóa luận: Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo — nền tảng KiteHub. (~30 giây)

## Slide 2 — Nội dung trình bày

Bài trình bày bám bốn chương: tổng quan, thiết kế kiến trúc, cài đặt và triển khai, đánh giá kết quả; khép lại bằng demo và kết luận. (~20 giây)

## Slide 3 — Bối cảnh và vấn đề (§1.1)

Bối cảnh: thị trường lớn, pháp luật hợp pháp hóa dạy thêm, đa số trung tâm nhỏ quản lý thủ công. Ba quan sát hội tụ: nhu cầu, mốc pháp lý cứng, AI vừa đủ chín. (~60 giây)

## Slide 4 — Khảo sát hệ thống tương tự (§1.2)

Khảo sát bốn hệ thống tham khảo: BeeClass (gamification nhẹ) + Mona eLMS, Easy Edu, DotB (quản lý trung tâm). Đều đơn-tenant, không AI. KiteHub khai thác khoảng trống đa-tenant gốc + AI Branding phân khúc giá thấp. (~70 giây)

## Slide 5 — Mục tiêu và phạm vi nghiên cứu (Mở đầu §2–§3)

Bốn mục tiêu (Mở đầu §2): SaaS đa-tenant, AI Branding, tuân thủ pháp luật, phương pháp luận chất lượng. Phạm vi (§3): AWS Singapore, hai giáo viên thật, ba nhóm người dùng; nhóm K-12 lùi sau do cần DPO/DPIA. (~60 giây)

## Slide 6 — Khác biệt — AI Branding tự động (§1.3 · §3.1.2)

Yếu tố khác biệt thứ hai (§1.3): tự động hóa nhận diện thương hiệu. Vận hành dùng OpenAI GPT-4 Vision + DALL-E 3, phát triển dùng Ollama tự host. Ưu tiên mẫu, chỉ gọi AI khi cần; xem trước bắt buộc đạt WCAG AA + qua bộ phân loại an toàn nội dung. Bên phải là wizard thực tế. (~80 giây)

## Slide 7 — Khác biệt — Tuân thủ pháp luật VN theo thiết kế (§1.3 · §2.1.2)

Ba trụ cột pháp luật tích hợp từ thiết kế. PDPL mốc cứng 07/2026, với consent_record, quy trình DSAR và audit log bất biến cho Điều 11. Luật An ninh mạng có ngưỡng chưa chạm, có lộ trình. Thông tư 78 dùng đối tác MISA. (~70 giây)

## Slide 8 — Phương pháp luận hướng chất lượng (Mở đầu §4 · §3.2)

Phương pháp luận kết hợp lý thuyết (so sánh hệ tham khảo) và thực nghiệm. Ba trụ cột có cơ sở: TDD (Beck), DDD (Evans), PDCA (Deming), theo chuẩn SQA IEEE 730. Kim tự tháp kiểm thử minh họa phân bố ba tầng test (~985 test). (~50 giây)

## Slide 9 — Kiến trúc tổng thể — C4 Level 1 (Hình 2.1)

Sơ đồ ngữ cảnh C4 Level 1 (Brown): Kite Platform tương tác 8 nhóm actor và 6 hệ thống bên ngoài. Mọi actor truy cập qua HTTPS (TLS 1.2+); hệ thống ngoài cô lập qua adapter pattern (NotificationChannel cho email, PaymentProcessor cho VietQR). Không actor nào chạm DB trực tiếp — đều qua biên trust gateway. (~70 giây)

## Slide 10 — Phân rã container — C4 Level 2 (Hình 2.2)

Sơ đồ container C4 Level 2: bốn cụm. Frontend gồm hai Next.js (kitehub cổng 3001 marketing/quản trị, kiteclass cổng 3000 giáo dục, ~85% mobile). Gateway (Spring Cloud Gateway cổng 9000) là điểm vào duy nhất. Cụm dịch vụ: 6 service KiteHub + kiteclass-core. Hạ tầng dùng chung 4 container kite- (Postgres/Redis/RabbitMQ/MinIO). Tổng 17 thành phần. (~60 giây)

## Slide 11 — Khác biệt — Cô lập đa-tenant bằng RLS (§2.2.3)

Đồ án đánh giá 6 pattern multi-tenant, chọn Shared Database + tenant_id + PostgreSQL RLS (mô hình Pool theo AWS SaaS Lens). Lý do: chi phí vận hành thấp nhất + database engine ép lọc, không phụ thuộc lập trình viên nhớ điều kiện. Bảng rút gọn 4 mô hình tiêu biểu. (~70 giây)

## Slide 12 — Bảo mật nhiều lớp — Defense-in-depth (Hình 2.3)

Nhiều lớp độc lập. RLS là phòng tuyến cuối: quên kiểm tra tầng ứng dụng, database vẫn ép buộc. Phải thủng cả 5 lớp mới rò dữ liệu chéo tenant. (~60 giây)

## Slide 13 — Mô hình dữ liệu — ERD KiteClass (Hình 2.6b)

Mô hình dữ liệu domain giáo dục. ENROLLMENTS phân giải nhiều-nhiều học viên–lớp; điểm danh, điểm, thanh toán gắn quanh đăng ký. Mọi bảng mang tenant_id phục vụ RLS. (~50 giây)

## Slide 14 — Vòng đời tenant — máy trạng thái (Hình 2.8)

Vòng đời tenant: từ chờ duyệt, sang dùng thử khi cấp magic-link, đến hoạt động chính thức. Sự kiện branding.deploy phát song song dựng template mặc định. (~50 giây)

## Slide 15 — Triển khai thực tế — AWS Singapore (Hình 4.1a)

Triển khai thực tế AWS Singapore (ap-southeast-1), VPC tách public/private subnet, RDS trong private subnet. Free Tier giữ tổng chi phí ~15–30 USD/tháng (EC2 vượt 750h tính ~7,38 USD; AI DALL-E 3 ~0,04 USD/ảnh). Chưa vượt ngưỡng pháp lý, có lộ trình chuyển vùng. (~60 giây)

## Slide 16 — CI/CD và giám sát vận hành (Hình 4.2a)

CI/CD chuẩn hiện đại: artifact bất biến, OIDC thay key tĩnh, xác nhận thủ công. Giám sát ba lớp; CloudTrail bật trước khi tạo tài nguyên để có audit baseline. (~50 giây)

## Slide 17 — Sản phẩm thực tế — giao diện (Chương 3)

Ba giao diện thực tế: trang chủ thương hiệu riêng (minh chứng phân giải Tenant→Domain→Landing), dashboard, quản lý học viên. Sản phẩm chạy thật, không phải mô hình. (~60 giây)

## Slide 18 — Kết quả AI Branding — gói Miễn phí vs Trả phí

Minh chứng giá trị AI Branding: bên trái gói Miễn phí mẫu dựng sẵn; bên phải gói Trả phí bộ nhận diện sinh tự động qua AI cho môn Hóa, tông màu khác hẳn. Hai tenant thật, hai thương hiệu riêng. (~70 giây)

## Slide 19 — Kết quả kiểm thử và đánh giá chất lượng (§3.2)

Tổng ~985 test (850 unit + 120 integration + 15-25 E2E), pass ≥99,5% trên main, coverage ≥75% line module nghiệp vụ. Integration dùng Testcontainers Postgres thật (không H2) cho RLS/GUC. Audit định kỳ 4 chiều theo IEEE 730, vòng cải tiến liên tục. (~80 giây)

## Slide 20 — Demo trực tiếp

Chuyển sang demo trực tiếp theo kịch bản 6 phase (defense-demo-script.md): khách tham quan, đăng ký onboarding, wizard tạo tenant, chứng minh cô lập bằng 2 tài khoản khác tenant, xem audit log. Dự phòng video. (~30 giây + demo)

## Slide 21 — Hạn chế thừa nhận và hướng phát triển

Thừa nhận hạn chế kèm lộ trình tốt hơn che giấu. Mỗi hạn chế có hướng phát triển: nâng cấp hạ tầng, chuyển vùng, mở thanh toán, tích hợp Zalo và hóa đơn điện tử qua đối tác. (~70 giây)

## Slide 22 — Kết luận

Tóm kết quả: nền tảng 7 microservice + 2 FE trên AWS Singapore, cô lập đa-tenant RLS, bốn yếu tố khác biệt, đáp ứng PDPL/An ninh mạng/TT78, kiểm chứng qua kiểm thử nhiều tầng, mã nguồn công khai. Cảm ơn GVHD và hội đồng, em sẵn sàng nhận câu hỏi. (~40 giây)

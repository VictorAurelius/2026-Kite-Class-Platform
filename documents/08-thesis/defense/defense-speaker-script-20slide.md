# Văn nói thuyết trình bảo vệ — 20 slide + demo

> Sinh ra từ speaker notes của `KiteHub-baove-khoaluan-20slide.pptx`. Tổng nói ~18–19 phút (chưa tính demo). Mỗi mục = 1 slide.

> Demo chèn tại slide 18, theo kịch bản `defense-demo-script.md` (6 phase ~10–15 phút).


## Slide 1 — Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo

Chào hội đồng. Em là Nguyễn Văn Kiệt, sinh viên lớp CNTT1-K63 dưới hướng dẫn của thầy Nguyễn Đức Dư. Hôm nay em xin trình bày khóa luận tốt nghiệp với đề tài Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo, gọi tắt là nền tảng KiteHub. (~30 giây)


## Slide 2 — Nội dung trình bày

Bài trình bày gồm 5 phần: bối cảnh và mục tiêu, khảo sát thị trường và AI, thiết kế kiến trúc, triển khai và kết quả, cuối cùng là demo trực tiếp và kết luận. (~20 giây)


## Slide 3 — Bối cảnh và vấn đề thực tế

Bối cảnh thúc đẩy đề tài: thị trường lớn, văn bản pháp luật hợp pháp hóa ngành dạy thêm, nhưng phần lớn trung tâm nhỏ vẫn quản lý thủ công vì phần mềm hiện có quá phức tạp hoặc quá đắt. Ba quan sát hội tụ: nhu cầu thị trường, mốc pháp lý, và công nghệ AI vừa đủ chín. (~60 giây)


## Slide 4 — Mục tiêu và phạm vi đề tài

Bốn nhóm mục tiêu: không chỉ làm sản phẩm chạy được mà còn phải đo lường được, tuân thủ pháp luật, và có phương pháp luận chứng minh được. Phạm vi tập trung mức sẵn sàng cho tenant thực tế dùng; các tính năng nâng cao thuộc hướng phát triển sau. (~60 giây)


## Slide 5 — Khảo sát hệ thống tương tự và yếu tố khác biệt

Khảo sát bốn sản phẩm tham khảo phổ biến tại Việt Nam. Hầu hết là đơn tenant, không có AI tích hợp. KiteHub khai thác khoảng trống đa-tenant gốc kết hợp AI Branding ở mức giá tier thấp. Chi tiết bốn phương pháp sinh ảnh để ở phụ lục. (~70 giây)


## Slide 6 — Đóng góp 1 — Kỹ thuật AI Branding tự động

Đóng góp thứ nhất: tự động hóa nhận diện thương hiệu. Quyết định không tự host AI là quyết định kiến trúc quan trọng, phù hợp quy mô và ngân sách. Pipeline gồm 6 bước, worker xử lý bất đồng bộ nên không chặn giao diện; có quality gate lọc nội dung và cơ chế dự phòng provider khi bị giới hạn. (~80 giây)


## Slide 7 — Tuân thủ pháp luật Việt Nam — 3 trụ cột

Ba trụ cột pháp luật được tích hợp ngay từ thiết kế. PDPL là mốc cứng tháng 7 năm 2026. Luật An ninh mạng có ngưỡng kích hoạt; hiện chưa chạm ngưỡng nhưng có lộ trình. Thông tư 78 về hóa đơn điện tử dùng đối tác chuyên trách. Mapping đầy đủ các điều luật PDPL ở phụ lục. (~60 giây)


## Slide 8 — Kiến trúc tổng thể — C4 Level 1

Kiến trúc tổng thể chia hai mặt phẳng: KiteHub là control-plane quản lý vòng đời tenant, KiteClass là data-plane phục vụ nghiệp vụ giáo dục. Cả hai dùng chung một PostgreSQL với RLS cô lập theo tenant. Sơ đồ trực quan có trong bản deck đầy đủ. (~70 giây)


## Slide 9 — Đóng góp 2 — Chiến lược cô lập đa-tenant

So sánh bốn mô hình cô lập. Chọn Row-Level Security vì chi phí thấp nhất và được chính database engine ép buộc — không phụ thuộc lập trình viên nhớ thêm điều kiện lọc. Đây là đóng góp thứ hai của đề tài. (~70 giây)


## Slide 10 — Cài đặt PostgreSQL Row-Level Security

RLS cài đặt hai lớp: lớp database tạo policy, lớp ứng dụng set biến phiên qua HikariCP. Mỗi truy vấn Postgres tự động đánh giá policy nên ngay cả khi quên lọc cũng không rò dữ liệu chéo tenant. (~70 giây)


## Slide 11 — Bảo mật nhiều lớp — Defense-in-depth

Nguyên tắc nhiều lớp độc lập. RLS là lớp cuối cùng: ngay cả khi lập trình viên quên kiểm tra ở tầng ứng dụng, Postgres vẫn ép buộc. Đây là khác biệt quan trọng so với cách chỉ lọc ở tầng ứng dụng. (~60 giây)


## Slide 12 — Công nghệ và phân chia dịch vụ

Kiến trúc lai: KiteHub tách 6 microservice vì vòng đời khác nhau (branding bất đồng bộ, email hàng đợi, subscription giao dịch). KiteClass là modular monolith vì domain giáo dục gắn kết chặt. Stack chọn theo bản LTS để giảm rủi ro nâng cấp. (~50 giây)


## Slide 13 — Đóng góp 3 — Trích cài đặt: JWT Auth Filter

Đoạn code đại diện một trong các trích dẫn trong báo cáo. Mẫu thiết kế: xác thực JWT ở gateway, không lặp ở mỗi dịch vụ; tenant context truyền qua header để dịch vụ set biến phiên trước truy vấn. (~70 giây)


## Slide 14 — Triển khai thực tế — AWS Singapore

Triển khai thực tế trên AWS Singapore. Lý do chọn: đăng ký ổn định, hệ sinh thái trưởng thành, Free Tier 12 tháng cho chi phí gần như 0. Hiện chưa vượt ngưỡng pháp lý, có lộ trình chuyển vùng rõ ràng khi cần. (~60 giây)


## Slide 15 — CI/CD và giám sát vận hành

CI/CD áp dụng chuẩn hiện đại: artifact bất biến, OIDC thay cho key tĩnh, xác nhận thủ công như một điểm dừng nhận thức. Giám sát ba lớp độc lập; CloudTrail bắt buộc bật trước khi tạo tài nguyên để có audit baseline. (~50 giây)


## Slide 16 — Phương pháp luận phát triển hướng chất lượng

Bốn trụ cột phương pháp luận, có cơ sở lý thuyết Deming, Beck, Poppendieck, IEEE 730. Đặc biệt trụ cột thứ tư: mỗi sai sót trở thành một quy tắc có cơ chế kiểm tra, không chỉ ghi chú lần sau cẩn thận hơn. (~50 giây)


## Slide 17 — Kết quả đánh giá — các chỉ số chính

Bốn chỉ số chính: hiệu năng 86, bảo mật 93, chất lượng 90 trên 110, đều vượt ngưỡng đạt. Quan trọng: mỗi điểm số có audit report với evidence block làm chứng cứ, và trajectory cho thấy cải tiến liên tục có thể đo được. (~80 giây)


## Slide 18 — Demo trực tiếp

Chuyển sang demo trực tiếp theo kịch bản 6 phase: khách tham quan trang công khai, đăng ký onboarding, wizard tạo tenant, chứng minh cô lập đa-tenant bằng hai tài khoản khác tenant, xem audit log. Nếu sự cố sẽ dùng video dự phòng. (~30 giây + demo)


**>>> CHÈN DEMO TRỰC TIẾP <<<** — chuyển sang trình duyệt, chạy 6 phase theo `defense-demo-script.md`:
1. Khách tham quan trang công khai → 2. Đăng ký onboarding → 3. Wizard tạo tenant → 4. Chứng minh cô lập đa-tenant (2 tài khoản khác tenant) → 5. Xem audit log → 6. Quay lại slide 19.
Dự phòng: video `backup-demo.mp4` nếu sự cố mạng/hạ tầng.


## Slide 19 — Hạn chế thừa nhận và hướng phát triển

Thừa nhận hạn chế trung thực kèm lộ trình là cách tiếp cận tốt hơn che giấu rồi bị hội đồng phát hiện. Mỗi hạn chế đều có hướng phát triển tương ứng: nâng cấp hạ tầng, chuyển vùng dữ liệu, mở thanh toán, tích hợp Zalo và hóa đơn điện tử qua đối tác. (~70 giây)


## Slide 20 — Kết luận

Tóm lại ba đóng góp: kỹ thuật AI Branding, kiến trúc đa-tenant RLS, và phương pháp luận hướng chất lượng. Sản phẩm đã triển khai thực tế và đạt các ngưỡng đánh giá. Cảm ơn GVHD và hội đồng, em sẵn sàng nhận câu hỏi. (~40 giây)


## Slide 21 — Phụ lục — Slide dự phòng cho phần hỏi đáp

Các slide phụ lục bật khi hội đồng hỏi sâu, theo 4 nhóm: Kiến trúc / Phi chức năng / Nghiệp vụ-Pháp lý / Quy trình.


## Slide 22 — Phụ lục A1 — So sánh 4 phương pháp sinh ảnh

Chọn Stable Diffusion XL vì cân bằng chi phí, chất lượng, độ trễ. DALL-E 3 đắt gấp khoảng 30 lần, không phù hợp tier thấp.


## Slide 23 — Phụ lục A2 — PDPL 2023: điều luật → tính năng

Năm điều luật cốt lõi mapping 1-1 sang tính năng. Điều 11 audit log bất biến là yêu cầu khó nhất, giải bằng bảng không cho sửa/xóa ở cấp database.


## Slide 24 — Phụ lục A3 — Luồng xác thực và truy vấn

Toàn bộ luồng không có điều kiện lọc tenant viết tay ở repository — database tự ép buộc.


## Slide 25 — Phụ lục A4 — Phân tích chi phí

Chi phí hạ tầng gần như 0 nhờ Free Tier; chi phí chính là AI Branding khoảng 0,19 USD mỗi lần onboard tenant. Số liệu chính xác tổng hợp khi có dữ liệu vận hành thực tế.


## Slide 26 — Phụ lục A5 — Phân khúc và persona mục tiêu

Ba persona chính P1-P2-P3 là trọng tâm hiện tại; phụ huynh và học viên hỗ trợ qua giao diện riêng, thuộc hướng phát triển sau.


## Slide 27 — Phụ lục A6 — Lộ trình mời tenant thực tế

Lộ trình mời tenant thực tế nhằm đạt mục tiêu: ít nhất 4 trung tâm ký xác nhận đã sử dụng thực tế — khác biệt giữa thesis demo trên máy và thesis có người dùng thật xác nhận.

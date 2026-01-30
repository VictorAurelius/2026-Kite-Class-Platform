ĐỀ CƯƠNG Ý TƯỞNG ĐỒ ÁN TỐT NGHIỆP

TÊN ĐỀ TÀI: XÂY DỰNG HỆ THỐNG QUẢN LÝ TRUNG TÂM GIÁO DỤC THEO KIẾN TRÚC MICROSERVICES - KITECLASS PLATFORM

SINH VIÊN THỰC HIỆN: NGUYỄN VĂN KIỆT


PHẦN 1: Ý TƯỞNG ĐỀ TÀI

1.1. Bối cảnh và lý do chọn đề tài

Trong bối cảnh chuyển đổi số giáo dục đang diễn ra mạnh mẽ tại Việt Nam, các trung tâm giáo dục, trung tâm ngoại ngữ và các tổ chức đào tạo nhỏ đang gặp nhiều khó khăn trong việc quản lý. Các giải pháp hiện tại thường gặp hạn chế về khả năng tùy biến theo đặc thù từng tổ chức, đặc biệt là với các giảng viên độc lập hoặc tổ chức giáo dục nhỏ và vừa. Ví dụ: các giáo viên các cấp THCS, THPT dạy thêm, các gia sư chứng chỉ IELTS, JLPT, AWS, các trung tâm giảng dạy du học, các trung tâm kỹ năng mềm, ...

Các vấn đề thực tế mà các trung tâm đang gặp phải bao gồm:
- Quản lý học viên phức tạp: Theo dõi điểm danh, điểm số, tiến độ học tập bằng sổ sách hoặc Excel
- Thu học phí thủ công: Ghi chép sổ sách, khó theo dõi công nợ, mất thời gian đối soát
- Thiếu kênh liên lạc với phụ huynh: Phụ huynh không nắm được tình hình học tập của con
- Marketing và xây dựng thương hiệu khó khăn: Chi phí thuê designer cao, không có nguồn lực

KiteClass Platform được phát triển nhằm giải quyết các vấn đề trên bằng cách cung cấp một nền tảng SaaS cho phép mỗi trung tâm nhanh chóng có hệ thống quản lý riêng với giao diện và thương hiệu cá nhân hóa, được hỗ trợ bởi trí tuệ nhân tạo.

Đề tài được phát triển dựa trên việc phân tích và học hỏi từ hệ thống BeeClass (beeclass.net) - một nền tảng quản lý trung tâm đang hoạt động thực tế tại Việt Nam, với các tính năng được học hỏi bao gồm: hệ thống quản lý phụ huynh và liên kết học viên, quy trình thanh toán học phí qua QR Code, hệ thống điểm thưởng và gamification, thông báo qua Zalo OA.

1.2. Mục tiêu đề tài

- Xây dựng nền tảng SaaS quản lý trung tâm giáo dục với khả năng multi-tenancy, cho phép mỗi trung tâm có hệ thống độc lập.

- Áp dụng kiến trúc Microservices tối ưu cho KiteClass instances với 3-5 services linh hoạt theo gói dịch vụ, giảm 40% chi phí RAM so với thiết kế ban đầu.

- Tích hợp AI Agent để tự động hóa quy trình tạo thương hiệu và giao diện cá nhân hóa cho mỗi instance với chi phí chỉ $0.19/instance.

- Xây dựng hệ thống KiteHub để quản lý toàn bộ vòng đời sản phẩm từ bán hàng đến vận hành.

- Phát triển Cổng phụ huynh để tăng cường liên lạc giữa gia đình và trung tâm, cho phép phụ huynh theo dõi điểm danh, điểm số và thanh toán học phí.

- Triển khai hệ thống Gamification với điểm thưởng, huy hiệu để tăng hứng thú học tập cho học viên.

1.3. Phạm vi nghiên cứu

Đối tượng sử dụng: Các trung tâm ngoại ngữ, trung tâm kỹ năng, trung tâm luyện thi, giảng viên độc lập.

Phạm vi chức năng: Quản lý lớp học, học viên, điểm danh, điểm số, học phí, phụ huynh, gamification, AI marketing và vận hành nền tảng.

Phạm vi kỹ thuật: Áp dụng kiến trúc Microservices (3-5 services) cho KiteClass instances, Modular Monolith cho KiteHub, triển khai trên Kubernetes.

Các actors trong hệ thống: Customer (khách hàng mua gói), Admin KiteHub (quản trị nền tảng), Center Owner (chủ trung tâm), Center Admin (quản trị viên), Teacher (giáo viên), Student (học viên), Parent (phụ huynh).


PHẦN 2: KIẾN TRÚC HỆ THỐNG VÀ LÝ DO LỰA CHỌN

2.1. Tổng quan kiến trúc

Hệ thống KiteClass Platform được thiết kế theo mô hình phân tán gồm 2 thành phần chính:

KiteHub Platform (Modular Monolith):
- Nhận vai trò trung tâm quản lý, điều phối toàn bộ hệ thống
- Được thiết kế theo kiến trúc Modular Monolith để tối ưu hiệu năng và giảm chi phí vận hành
- Bao gồm các module: Sale (bán hàng, thanh toán), Message (chat hỗ trợ, thông báo), Maintaining (quản lý và provision instance), AI Agent (tạo nội dung marketing)
- Frontend: Landing page, Admin dashboard, Customer portal

KiteClass Instances (Microservices - Tối ưu V3.1):
- Mỗi instance là một hệ thống quản lý trung tâm độc lập
- Áp dụng kiến trúc Microservices với 3-5 services linh hoạt thay vì 7 services cố định như thiết kế ban đầu
- Services bắt buộc: User+Gateway Service (xác thực, quản lý người dùng, API gateway), Core Service (lớp học, học viên, điểm danh, điểm số, học phí), Frontend (giao diện người dùng)
- Services tùy chọn theo gói: Engagement Service (gamification, forum, parent portal), Media Service (video streaming, live class)

[Xem sơ đồ kiến trúc chi tiết trong file 05-system-overview-v3.png]

2.2. Thay đổi kiến trúc từ V2 sang V3.1

So sánh kiến trúc V2 và V3.1:
- Số services: V2 có 7 services cố định, V3.1 có 3-5 services linh hoạt. Lý do: Giảm 40% RAM, dễ maintain, linh hoạt theo gói dịch vụ.
- Gateway Service: V2 có Gateway riêng, V3.1 merge vào User Service. Lý do: Giảm 1 container, giảm network latency, đơn giản hóa deployment.
- Parent Portal: V2 không có, V3.1 có. Lý do: Học từ BeeClass, nhu cầu thực tế cao tại Việt Nam.
- Billing với VietQR: V2 không có, V3.1 có. Lý do: Thanh toán QR Code phổ biến tại Việt Nam.
- Gamification: V2 không có, V3.1 có. Lý do: Tăng engagement cho học viên, học từ BeeClass.
- RAM tối thiểu/instance: V2 cần ~4GB, V3.1 chỉ cần ~2.5GB. Lý do: Tối ưu số services và resource allocation.

2.3. Lý do sử dụng Microservices cho KiteClass Instance

2.3.1. Khả năng scale độc lập theo nhu cầu

Mỗi service trong KiteClass instance có thể được scale riêng biệt tùy theo tải. Ví dụ:
- Core Service cần scale nhiều hơn trong giờ điểm danh peak (sáng, chiều)
- Media Service cần tài nguyên lớn hơn khi có live class
- Engagement Service có thể scale nhẹ hơn khi sử dụng ít

2.3.2. Linh hoạt trong lựa chọn công nghệ

Các services có thể sử dụng công nghệ phù hợp nhất:
- Backend services dùng Java Spring Boot để đảm bảo ổn định và type-safety
- Frontend dùng Next.js để tối ưu SEO và tốc độ tải trang
- Media Service có thể dùng Node.js để xử lý WebSocket hiệu quả (nếu cần)

2.3.3. Dễ dàng bảo trì và nâng cấp

- Mỗi service có thể được cập nhật, sửa lỗi độc lập mà không ảnh hưởng đến các service khác
- Giảm thiểu rủi ro khi triển khai thay đổi
- Dễ dàng test và debug từng thành phần riêng biệt

2.3.4. Tăng cường độ tin cậy (Reliability)

- Khi một service gặp sự cố, các service khác vẫn hoạt động bình thường
- Circuit breaker và retry mechanism giúp hệ thống chịu lỗi tốt
- Isolation giúp hạn chế tác động khi có lỗi xảy ra

2.3.5. Phù hợp với mô hình multi-tenancy và pricing linh hoạt

- Mỗi KiteClass instance chạy độc lập với database riêng (database-per-tenant)
- Có thể tùy chỉnh services theo gói dịch vụ: Basic chỉ có core features, Premium có đầy đủ Engagement và Media
- Isolation tốt giúp đảm bảo bảo mật và hiệu năng

2.4. Tại sao KiteHub nên là Modular Monolith?

KiteHub không nên dùng microservices vì:
- Các module trong KiteHub liên kết chặt chẽ (Sale → AI → Maintaining)
- Khối lượng request thấp hơn nhiều so với KiteClass instances
- Modular monolith giảm độ phức tạp trong giao tiếp giữa các module
- Chi phí vận hành thấp hơn (ít container, không cần service discovery)
- Dễ dàng quản lý transaction xuyên các module (ví dụ: Order → Payment → Provision)

Tuy nhiên, KiteHub vẫn được tổ chức thành các module rõ ràng để dễ bảo trì và có thể tách thành microservices sau này nếu cần thiết.

2.5. Tại sao không dùng Service Registry (Eureka/Consul)?

Sau khi phân tích chi tiết (xem service-registry-analysis.md), kết luận là KHÔNG KHUYẾN NGHỊ sử dụng Service Registry cho KiteClass vì:
- Chỉ có 3-5 services, quá ít để cần service registry
- Tăng 40% RAM overhead (~800MB cho Eureka cluster)
- Tăng độ phức tạp vận hành không cần thiết
- ROI (Return on Investment) = -95%, chi phí lớn hơn lợi ích
- Kubernetes Service Discovery đủ dùng cho quy mô này

Thay thế bằng: Kubernetes Service (internal DNS), Docker Compose networking (dev environment), hoặc hard-coded URLs với config management.


PHẦN 3: CÁC ACTORS VÀ USE CASES CHÍNH

3.1. Danh sách Actors

KiteHub Actors:
- Customer: Khách hàng tiềm năng, đăng ký mua gói dịch vụ
- Admin KiteHub: Nhân viên quản trị nền tảng, xử lý đơn hàng, hỗ trợ khách hàng

KiteClass Instance Actors:
- Center Owner: Chủ trung tâm, quản lý tổng thể, xem báo cáo doanh thu
- Center Admin: Quản trị viên, quản lý lớp học, học viên, học phí
- Teacher: Giáo viên, điểm danh, chấm điểm, giao bài tập
- Student: Học viên, xem lịch học, điểm số, làm bài tập
- Parent: Phụ huynh, theo dõi con, thanh toán học phí (NEW trong V3.1)

3.2. Use Cases chính theo Actor

Center Owner:
- Xem dashboard tổng quan (doanh thu, số học viên, công nợ)
- Quản lý nhân sự (Admin, Teacher)
- Cài đặt trung tâm (logo, thông tin, cấu hình)
- Xem báo cáo tài chính
- Sử dụng AI Marketing để tạo content quảng cáo

Center Admin:
- Quản lý khóa học (tạo, sửa, xóa khóa học)
- Quản lý lớp học (tạo lớp, xếp lịch, phân phòng)
- Quản lý học viên (đăng ký, chuyển lớp, bảo lưu)
- Quản lý học phí (tạo hóa đơn, thu tiền, theo dõi công nợ)
- Gửi thông báo (Zalo, Email, App)

Teacher:
- Xem lịch dạy (calendar view)
- Điểm danh học viên từng buổi
- Chấm điểm, nhập nhận xét
- Giao bài tập, upload tài liệu
- Thảo luận với học viên qua forum

Student:
- Xem lịch học, thời khóa biểu
- Xem điểm số, nhận xét từ giáo viên
- Làm và nộp bài tập
- Tham gia forum hỏi đáp
- Xem điểm thưởng, đổi quà (gamification)

Parent (NEW):
- Đăng ký/Liên kết với con qua QR Code + OTP Zalo
- Xem điểm danh của con theo từng buổi
- Xem điểm số, báo cáo học tập của con
- Thanh toán học phí qua QR Code VietQR
- Nhận thông báo từ trung tâm qua Zalo, App
- Nhắn tin trao đổi với giáo viên

[Xem sơ đồ Business Flow chi tiết trong file 06-business-flow-v3.png]


PHẦN 4: QUY TRÌNH MỞ NODE KITECLASS VỚI AI AGENT

[Xem sơ đồ flow chi tiết trong file 06-business-flow-v3.png]

4.1. Bước 1: Khách hàng đặt hàng và cấu hình

- Khách hàng truy cập KiteHub Platform (kiteclass.com)
- Xem thông tin sản phẩm, tính năng, bảng giá
- Chọn gói dịch vụ phù hợp (Basic, Standard, Premium)
- Cấu hình các thông số: subdomain (abc.kiteclass.com), số lượng người dùng, các service cần thiết
- Upload ảnh cá nhân/logo của tổ chức
- Thanh toán qua cổng thanh toán tích hợp (chuyển khoản, MoMo, ZaloPay)

4.2. Bước 2: AI Agent tự động tạo nội dung thương hiệu

Sau khi nhận được ảnh upload, AI Agent Module tự động xử lý:

a) Remove background (sử dụng Remove.bg API):
   - Loại bỏ nền ảnh để có ảnh trong suốt
   - Tạo các phiên bản ảnh với các kích thước khác nhau

b) Extract primary colors:
   - Phân tích ảnh để trích xuất bảng màu chủ đạo
   - Tạo color palette cho giao diện

c) Generate marketing copy (sử dụng OpenAI GPT-4):
   - Tạo các khẩu hiệu, slogan phù hợp
   - Sinh nội dung marketing đa dạng
   - Tạo mô tả cho các trang chính

d) Create visual assets (sử dụng Stable Diffusion XL):
   - Tạo logo với 3 phiên bản khác nhau
   - Tạo banner cho các vị trí trên website (5 kích thước)
   - Tạo thumbnail và avatar

Toàn bộ quy trình AI mong muốn dự kiến sẽ mất khoảng 30 giây và chi phí 0.19 USD/instance.

4.3. Bước 3: Preview và xác nhận

- Khách hàng xem trước các tài nguyên AI đã tạo
- Có thể yêu cầu tạo lại hoặc điều chỉnh nếu cần
- Xác nhận và bắt đầu quy trình provision

4.4. Bước 4: Tự động provision infrastructure

Maintaining Module nhận request và thực hiện:

a) Tạo database riêng (PostgreSQL): Khởi tạo schema database, Insert dữ liệu mặc định (roles, permissions, settings)

b) Deploy các microservices lên Kubernetes:
   - User+Gateway Service (xác thực, quản lý người dùng, API gateway)
   - Core Service (lớp học, học viên, điểm danh, học phí)
   - Engagement Service (nếu gói có - gamification, parent portal)
   - Frontend (giao diện người dùng với branding)

c) Deploy và build Frontend: Inject các thông tin branding từ AI-generated assets, Áp dụng color palette cho giao diện, Cấu hình routing và domain, Build và deploy Next.js application

Toàn bộ quy trình provision mong muốn dự kiến mất 3-5 phút và được tự động hóa hoàn toàn.

4.5. Bước 5: Bàn giao và kích hoạt

- Hệ thống gửi email thông báo hoàn thành
- Cung cấp URL truy cập: https://[subdomain].kiteclass.com
- Tài khoản admin mặc định cho khách hàng
- Tài liệu hướng dẫn sử dụng và quản trị

4.6. Lợi ích của quy trình tự động với AI Agent

- Tiết kiệm thời gian: Từ vài ngày xuống còn 3-5 phút
- Tiết kiệm chi phí: Không cần thuê designer (chỉ $0.19 cho AI)
- Nhất quán thương hiệu: AI đảm bảo các asset phù hợp với nhau
- Tùy biến cao: Mỗi instance có giao diện và thương hiệu riêng
- Chuyên nghiệp: Chất lượng tài nguyên AI tương đương designer


PHẦN 5: THIẾT KẾ DATABASE

5.1. Chiến lược Database

Hệ thống sử dụng chiến lược Database-per-tenant (Complete isolation):
- KiteHub Database: 1 database duy nhất cho platform trung tâm, bao gồm các schema: sales (customers, orders, payments), messages (chat, notifications), maintaining (instances, health checks), ai_agents (sessions, assets)
- KiteClass Instance Database: Mỗi trung tâm có 1 database riêng biệt hoàn toàn, đảm bảo isolation về bảo mật và hiệu năng

Lợi ích của Database-per-tenant:
- Complete data isolation: Không có rủi ro lộ dữ liệu giữa các tenant
- Easy backup/restore: Có thể backup/restore từng tenant độc lập
- Independent scaling: Mỗi database có thể scale riêng
- Compliance friendly: Dễ đáp ứng yêu cầu về data residency

5.2. Các module chính trong KiteClass Instance Database

User Module: users, roles, permissions, user_roles, user_sessions

Class Module: courses, classes, class_schedules, enrollments, rooms

Learning Module: attendance, grades, assignments, submissions

Billing Module: tuition_configs, invoices, invoice_items, payments, payment_reminders

Gamification Module: point_rules, student_points, badges, student_badges, rewards, reward_redemptions

Parent Module: parents, parent_children, parent_notifications

Forum Module: forum_topics, forum_posts, forum_comments

[Xem ERD chi tiết trong file 03-erd.png]


PHẦN 6: CÔNG NGHỆ SỬ DỤNG

6.1. Backend

- Ngôn ngữ: Java 21
- Framework: Spring Boot 3.2, Spring Security, Spring Data JPA
- API: RESTful API, WebSocket (STOMP/SockJS)
- Message Queue: RabbitMQ 3.12

6.2. Frontend

- Framework: Next.js 14 (App Router)
- Ngôn ngữ: TypeScript 5
- Styling: TailwindCSS, Shadcn/UI
- State Management: React Query, Zustand

6.3. Database và Cache

- Database: PostgreSQL 15
- Cache: Redis 7

6.4. Infrastructure

- Container: Docker
- Orchestration: Kubernetes
- CI/CD: GitHub Actions
- Web Server: Nginx

6.5. External Services

- AI Text Generation: OpenAI GPT-4
- AI Image Generation: Stability AI (SDXL)
- Background Removal: Remove.bg
- Notification: Zalo API
- Payment: VietQR


PHẦN 7: ĐIỂM MẠNH VÀ THỬ THÁCH CỦA ĐỀ TÀI

7.1. Giải quyết vấn đề thực tế

- Học hỏi từ BeeClass (hệ thống đang chạy thực tế tại Việt Nam)
- Đáp ứng nhu cầu thị trường: nhiều trung tâm, giảng viên muốn có nền tảng riêng
- Parent Portal: tính năng đặc thù cho thị trường Việt Nam, phụ huynh quan tâm việc học của con
- VietQR Payment: phương thức thanh toán phổ biến tại Việt Nam

7.2. Kiến trúc tối ưu và phù hợp

- Microservices cho KiteClass instances đảm bảo linh hoạt, scale tốt
- Modular Monolith cho KiteHub giảm chi phí và độ phức tạp
- Không dùng Service Registry sau khi phân tích ROI -95%
- Tiết kiệm 40% RAM so với thiết kế V2 (từ ~4GB xuống ~2.5GB/instance)

7.3. Tích hợp AI Agent sáng tạo

- Sử dụng AI để tự động hóa quy trình tạo nội dung, giao diện
- Kết hợp nhiều AI services: GPT-4, Stable Diffusion, Remove.bg
- Chi phí chỉ $0.19/instance, tiết kiệm 3-5 ngày công designer
- Tạo giá trị thực tế: tiết kiệm thời gian và chi phí cho khách hàng

7.4. Yêu cầu kiến thức kỹ thuật rộng

Để hoàn thành đề tài, sinh viên cần nắm vững nhiều lĩnh vực:
- Backend Development: Java Spring Boot, RESTful API
- Frontend Development: Next.js, React, TypeScript
- Database: PostgreSQL, Redis, database design và optimization
- DevOps: Docker, Kubernetes, CI/CD
- AI Integration: OpenAI API, Stable Diffusion, image processing
- Microservices patterns: API Gateway, Circuit breaker, Event-driven

7.5. Khối lượng code và công việc lớn

- Cần phát triển nhiều services độc lập (KiteHub + 3-5 services/instance)
- Mỗi service cần code đầy đủ các layer: Controller, Service, Repository
- Phải viết test cases cho tất cả các thành phần
- Cần setup CI/CD pipeline
- Khối lượng code ước tính: 20,000+ lines of code

7.6. Phức tạp trong triển khai và vận hành

- Phải hiểu sâu về Kubernetes để deploy và manage containers
- Cần thiết lập monitoring, logging cho toàn bộ hệ thống
- Quản lý nhiều databases và ensure data consistency
- Xử lý các vấn đề network, security trong môi trường distributed


KẾT LUẬN

Đề tài "Xây dựng hệ thống quản lý trung tâm giáo dục theo kiến trúc Microservices - KiteClass Platform" là một đồ án tốt nghiệp có tính thực tế cao, áp dụng nhiều công nghệ hiện đại và giải quyết vấn đề cụ thể của thị trường giáo dục Việt Nam.

Với việc kết hợp Microservices (3-5 services tối ưu) cho KiteClass instances và Modular Monolith cho KiteHub, đề tài thể hiện khả năng phân tích và lựa chọn kiến trúc phù hợp thay vì áp đặt kiến trúc một cách giáo điều. Việc phân tích ROI để loại bỏ Service Registry không cần thiết thể hiện tư duy tối ưu chi phí-lợi ích.

Đặc biệt, việc tích hợp AI Agent để tự động hóa quy trình tạo thương hiệu và giao diện là một điểm sáng, tạo giá trị thực tế và thể hiện khả năng ứng dụng AI vào giải quyết vấn đề thực tế. Các tính năng Parent Portal và VietQR Payment được học hỏi từ BeeClass thể hiện sự am hiểu thị trường Việt Nam.

Tuy nhiên, đề tài cũng đưa ra nhiều thử thách: yêu cầu kiến thức rộng về nhiều lĩnh vực, khối lượng code lớn, phức tạp trong triển khai. Nhưng chính những thử thách này sẽ giúp sinh viên phát triển toàn diện cả hard skills và soft skills, chuẩn bị tốt cho công việc sau khi tốt nghiệp.

Với lợi ích rõ ràng, kiến trúc hợp lý và khả năng ứng dụng thực tế, đề tài KiteClass Platform là một lựa chọn phù hợp cho đồ án tốt nghiệp chuyên ngành Công nghệ phần mềm.


PHỤ LỤC: DANH SÁCH SƠ ĐỒ VÀ TÀI LIỆU

Sơ đồ:
1. 01-architecture-simple.png - Sơ đồ kiến trúc đơn giản
2. 02-bfd-actors.png - Business Flow Diagram theo Actor
3. 03-erd.png - Entity Relationship Diagram
4. 04-architecture-full.png - Kiến trúc đầy đủ với Tech Stack
5. 05-system-overview-v3.png - System Overview V3.1
6. 06-business-flow-v3.png - Complete Business Flow

Tài liệu tham khảo:
1. system-architecture-v3-final.md - Báo cáo kiến trúc chi tiết
2. service-use-cases-v3.md - Use Cases theo Service
3. database-design.md - Thiết kế Database
4. service-registry-analysis.md - Phân tích Service Registry

---
Hệ thống KiteClass Platform
Phiên bản: 3.1 (Optimized)
Ngày cập nhật: 23/12/2025

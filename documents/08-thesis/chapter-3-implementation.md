---
title: Chương 3 — Triển khai (Kết quả triển khai sản phẩm + Kiểm thử)
audience: mixed
chapter: 3
status: draft
created: 2026-05-19
updated: 2026-05-20
---

# Chương 3 — Triển khai (Implementation)

## 3.1 Công nghệ sử dụng

Trước khi đi vào kết quả triển khai sản phẩm, mục này tổng hợp công nghệ, công cụ và ngôn ngữ lập trình được sử dụng theo từng giai đoạn phát triển nền tảng KiteHub Platform.

### 3.1.1 Ngôn ngữ lập trình

Nền tảng KiteHub sử dụng hai ngôn ngữ lập trình chính. Phía backend dùng Java 21 (LTS) — phiên bản hỗ trợ dài hạn được Oracle cam kết bảo trì đến năm 2031, kèm các tính năng hiện đại như virtual threads (Project Loom), pattern matching và records giúp viết code an toàn kiểu và biểu cảm. Phía frontend dùng TypeScript 5.7 — bản mở rộng kiểu tĩnh của JavaScript, hỗ trợ phát hiện lỗi sớm tại compile-time, refactoring an toàn và tích hợp IDE mạnh. Ngôn ngữ truy vấn cơ sở dữ liệu sử dụng SQL chuẩn PostgreSQL 16 dialect, kết hợp JPQL/Hibernate cho các truy vấn ORM phổ biến.

### 3.1.2 Framework phát triển

Phía backend, Spring Boot 3.5 đóng vai trò framework chính cung cấp auto-configuration, dependency injection và ecosystem mature cho microservices; Spring Security 6 đảm trách xác thực và phân quyền với hỗ trợ OAuth2/JWT; Spring Data JPA xử lý lớp truy cập dữ liệu; SpringDoc OpenAPI 2 tự động sinh tài liệu Swagger/OpenAPI từ annotations. Phía frontend, Next.js 15 cung cấp App Router, Server Components, SSR/SSG và image optimization; React 19 là thư viện UI nền tảng với hooks và concurrent features; Tailwind CSS 3.4 + Shadcn UI cho hệ thống styling utility-first; TanStack Query 5 + Zustand 5 quản lý state phía client; React Hook Form 7 + Zod 3 xử lý form và validation kiểu schema-driven.

### 3.1.3 Công cụ phát triển

Mỗi lập trình viên làm việc với IntelliJ IDEA Ultimate hoặc VS Code (extension Spring Boot + Java) cho backend; VS Code (extension TypeScript + Tailwind CSS + ESLint + Prettier) cho frontend. Quản lý phụ thuộc dùng Apache Maven 3.9 cho Java và pnpm 9 cho Node.js (lựa chọn pnpm thay npm/yarn nhờ disk space efficient và workspace mature). Phiên bản hóa source code qua Git + GitHub repository, với pre-commit hooks (Husky 9) chạy lint + format trước commit. Quy trình review code thực hiện qua GitHub Pull Request với required checks. Lombok 1.18 và MapStruct 1.6 hỗ trợ giảm boilerplate Java và mapping DTO compile-time.

### 3.1.4 Công cụ kiểm thử

Unit test phía backend sử dụng JUnit 5 (Jupiter) + AssertJ cho assertions biểu cảm + Mockito 5 cho mock dependencies. Integration test dùng Testcontainers 1.20 (PostgreSQL + Redis container ephemeral) đảm bảo môi trường test cô lập, không phụ thuộc dev DB. Spring Boot Test framework cung cấp `@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest` cho các test slice phù hợp. Phía frontend, Vitest 1 + React Testing Library cho unit test component; Playwright 1 cho end-to-end test browser-automation. Mã được kiểm tra chất lượng bằng SonarQube static analysis và OWASP Dependency-Check tự động trong CI pipeline.

### 3.1.5 Công cụ triển khai

Hạ tầng được mô tả bằng code (Infrastructure as Code) qua Terraform 1.x cho AWS resources (EC2, RDS, S3, SES, IAM, CloudWatch). Container hóa qua Docker 24 + Docker Compose 2 cho môi trường phát triển cục bộ; production triển khai container qua AWS Elastic Container Service (ECS) với task definitions JSON. Pipeline CI/CD chạy trên GitHub Actions với matrix builds (Java 21 + Node 22), tự động build + test + push container image lên AWS Elastic Container Registry (ECR). Phía vận hành, AWS Systems Manager (SSM) cung cấp shell access không cần SSH key; AWS CloudWatch tập hợp logs + metrics; AWS CloudTrail audit mọi thao tác API trên tài khoản. Phía CDN, Cloudflare đứng trước domain `kitehub.me` (DNS + WAF + DDoS protection layer). Migration cơ sở dữ liệu qua Flyway 10 (versioned schema changes, idempotent migrations).

### 3.1.6 Tổ chức source code

```
kitehub/
├── kitehub-gateway/           ~ 4.200 dòng       # API Gateway (Spring Cloud Gateway)
├── kitehub-platform/          ~ 5.800 dòng       # Tenant lifecycle service
├── kitehub-subscription/      ~ 7.100 dòng       # Subscription + billing service
├── kitehub-branding/          ~ 5.500 dòng       # AI Branding service
├── kitehub-email/             ~ 3.900 dòng       # Email notification service
├── kitehub-admin/             ~ 4.400 dòng       # Admin console service
├── kitehub-frontend/          ~ 12.800 dòng      # Next.js marketing + admin frontend

kiteclass/
├── kiteclass-core/            ~ 9.600 dòng       # Tenant application core
├── kiteclass-frontend/        ~ 8.500 dòng       # Next.js tenant frontend

infrastructure/
├── terraform-aws/             ~ 2.400 dòng       # AWS provisioning
├── helm/                      ~ 3.500 dòng       # Kubernetes manifests (defer giai đoạn paid)

(tooling + tài liệu nội bộ)    ~ 12.000 dòng
```

Tổng quy mô codebase ước tính khoảng 80.000 dòng (chưa tính tests và config), thể hiện tính chất production-grade của nền tảng. Chương này tập trung trình bày kết quả triển khai sản phẩm (giao diện người dùng) và kết quả kiểm thử + đánh giá chất lượng — code-level snippet analysis được lược bỏ khỏi flow chính theo convention báo cáo cử nhân CNTT.

---

## 3.2 Kết quả triển khai sản phẩm

Phần này trình bày kết quả triển khai 8 giao diện cốt lõi của KiteHub Platform giai đoạn beta, đại diện cho hành trình end-to-end của người dùng từ khám phá sản phẩm đến vận hành tenant. Mỗi giao diện được mô tả kèm hình minh họa, persona target và mục tiêu nghiệp vụ.

> Ghi chú về hình ảnh: trong phiên bản đồ án này, các hình minh họa giao diện đang ở dạng placeholder tham chiếu mockup HTML/JSX tại `documents/02-architecture/design-system/ui_kits/`. Trước cửa sổ bảo vệ, PNG snapshot độ phân giải 1440×900 (browser locale vi-VN) sẽ được capture và nhúng inline qua `add_image_inline()` helper.

### 3.2.1 Trang chủ marketing KiteHub

<!-- screenshot placeholder: capture kitehub-marketing-landing.png 1440×900 vi-VN — show hero section + value proposition + CTA "Yêu cầu truy cập Beta" -->

**Hình 3.1.** Trang chủ marketing KiteHub (`kitehub.me/`) — giao diện đầu tiên anonymous visitor tiếp xúc với nền tảng.

Mockup source: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/index.html`

Trang chủ marketing đóng vai trò "first impression" của KiteHub đối với anonymous prospect persona (Vy — học sinh THPT đang tìm hiểu nền tảng học tập số cho gia đình). Layout 3-fold: hero section với tagline "Nền tảng quản lý trung tâm dạy thêm" + value proposition 3 bullet (Multi-tenant isolation, AI Branding, Bộ tính năng quản lý lớp đầy đủ) + CTA chính "Yêu cầu truy cập Beta". Tone tiếng Việt formal-friendly, sample data VN (Trung tâm Anh ngữ Sky Education, Lớp 5A1) theo `vn-localization-audit-checklist.md` §3. Tiếp theo là 3 phần: "Vì sao chọn KiteHub" (so sánh với 3 đối tượng tham khảo — chi tiết tại Chương 1 §1.4), "Cho ai" (P1 Solo Teacher + P2 Center Owner profile), và footer minh bạch về giai đoạn beta + đường dẫn liên hệ qua email và Zalo.

### 3.2.2 Wizard đăng ký yêu cầu beta dành cho Chủ trung tâm (P2)

<!-- screenshot placeholder: capture p2-signup-wizard-step-1.png 1440×900 vi-VN — show 4-field form (tên, email, tên trung tâm, quy mô) + progress bar 1/3 + tone Vietnamese friendly -->

**Hình 3.2.** Wizard đăng ký yêu cầu beta giai đoạn 1 (`/auth/request-beta-access`) — form 4 trường thiết kế tối thiểu để giảm friction.

Mockup source: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/billing-default.html` (form layout pattern)

Wizard đăng ký bao gồm 4 trường: họ tên, email, tên trung tâm dự kiến, quy mô (combobox: dưới 50 học sinh / 50-150 / 150-500 / trên 500 học sinh). Form không yêu cầu mật khẩu tại bước này — admin nền tảng review request và gửi claim code 6 chữ số qua email sau khi duyệt. Field "tên trung tâm" đính kèm hint text "VD: Trung tâm Anh ngữ Sky Education". Form validation client-side bằng React Hook Form 7 + Zod 3 (định dạng email + tên không trống + quy mô bắt buộc); validation server-side bổ sung honeypot field chống bot + rate-limit 24h per email (chống abuse). Sau submit thành công, page hiển thị banner xác nhận "Yêu cầu đã được gửi — đội ngũ KiteHub sẽ phản hồi trong 1-2 ngày làm việc qua email <địa chỉ>".

### 3.2.3 Dashboard chính của Chủ trung tâm (P2)

<!-- screenshot placeholder: capture p2-owner-dashboard.png 1440×900 vi-VN — show 3 KPI cards (doanh thu tháng, số học sinh, số lớp) + 5-step onboarding checklist + sample data toggle -->

**Hình 3.3.** Dashboard chính của Chủ trung tâm sau lần đăng nhập đầu tiên — 3 KPI card + onboarding checklist 5 bước.

Mockup source: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/billing-default.html`

Sau lần đăng nhập đầu tiên, dashboard hiển thị 3 thành phần chính cho Chủ trung tâm: (1) KPI cards với 3 thẻ "Doanh thu tháng", "Số học sinh", "Số lớp" — mặc định hiển thị `0đ`, `0`, `0` cho tenant mới chưa nhập dữ liệu; (2) Onboarding checklist 5 bước với icon và link tới wizard tương ứng — "Tạo lớp đầu tiên", "Thêm học sinh", "Tạo lịch học", "Cấu hình thanh toán", "Mời giáo viên đồng nghiệp"; (3) Sample data toggle cho phép load dữ liệu mẫu (1 chủ trung tâm giả định + 4 học sinh + 1 lớp Anh ngữ 5A1) để user thử các chức năng trước khi nhập dữ liệu thật. Format VND `1.500.000đ` + date tiếng Việt `Thứ Hai, 20/05/2026` áp dụng đồng nhất theo `vn-localization-audit-checklist.md` §1.

### 3.2.4 Trang xác nhận provisioning tenant thành công

<!-- screenshot placeholder: capture tenant-provisioning-success.png 1440×900 vi-VN — show success message + tenant subdomain link + first-login CTA -->

**Hình 3.4.** Trang xác nhận provisioning tenant thành công sau khi user nhập claim code 6 chữ số và đặt mật khẩu.

Mockup source: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/branding-hub-default.html`

Sau khi user exchange claim code thành công, hệ thống tạo tenant mới với atomic transaction (INSERT tenant + INSERT user + UPDATE beta_request status — chi tiết tại Chương 4 §4.2.3). Trang xác nhận hiển thị: tiêu đề "Chào mừng đến với KiteHub!", subdomain tenant được cấp `https://sky-edu.kitehub.me`, danh sách 3 bước tiếp theo gợi ý ("Khám phá dashboard", "Cài đặt thông tin trung tâm", "Tạo lớp học đầu tiên"), và CTA "Đăng nhập ngay" tự động redirect tới `/dashboard` với JWT đã có sẵn (no re-login required). Banner phía dưới notify "Bạn đang trong giai đoạn beta — vui lòng phản hồi qua Zalo `zalo.me/kitehub` nếu gặp vấn đề".

### 3.2.5 Quản lý lớp học (Class Management)

<!-- screenshot placeholder: capture class-management-list.png 1440×900 vi-VN — show class list table + filter by status/teacher + bulk actions + create new class CTA -->

**Hình 3.5.** Giao diện quản lý lớp học — danh sách lớp với filter, bulk actions và CTA tạo lớp mới.

Mockup source: `documents/02-architecture/design-system/ui_kits/kiteclass-teacher/screens/multi-class-roster.html`

Giao diện quản lý lớp hiển thị danh sách tất cả các lớp của tenant với các cột: Mã lớp, Tên lớp (vd `Lớp Anh ngữ 5A1`), Giáo viên chủ nhiệm, Số học sinh, Lịch học, Trạng thái (Đang hoạt động / Tạm nghỉ / Đã kết thúc), Hành động (Xem chi tiết / Sửa / Lưu trữ). Bảng hỗ trợ filter combo (theo trạng thái + theo giáo viên + theo môn học) và search theo tên/mã lớp. Bulk actions cho phép chọn nhiều lớp để gửi thông báo Zalo group hoặc export Excel danh sách điểm danh. CTA "Tạo lớp mới" góc trên phải mở wizard 4 bước: thông tin cơ bản — lịch học (Mon-Sat per VN edu convention) — danh sách học sinh — cấu hình học phí.

### 3.2.6 Tạo và phát hành hóa đơn

<!-- screenshot placeholder: capture invoice-generation.png 1440×900 vi-VN — show invoice form with VND format + auto-calculated total + preview pane + send actions -->

**Hình 3.6.** Giao diện tạo hóa đơn — form với định dạng VND, preview bên phải và các action gửi qua email và Zalo.

Mockup source: `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/billing-payment.html`

Form tạo hóa đơn cho phép Chủ trung tâm hoặc Manager tạo hóa đơn cho học sinh (cá nhân) hoặc batch (nhiều học sinh trong cùng lớp). Layout 2 cột: cột trái — form nhập (Học sinh nhận hóa đơn, Lớp, Tháng/Kỳ học, Học phí gốc, Giảm giá, Phụ thu, Hạn thanh toán); cột phải — preview hóa đơn realtime với header "HÓA ĐƠN ĐIỆN TỬ" theo convention VN, mã số thuế tenant (nếu có), tổng tiền `Tổng cộng: 1.500.000đ` VND format. Preview bao gồm QR code VietQR để học sinh quét chuyển khoản trực tiếp (Vietcombank/Techcombank/MB merchant code). Sau khi tạo, hóa đơn được gửi qua 3 kênh: email PDF chính thức cho phụ huynh, link xem trực tuyến qua Zalo group cha mẹ, và lưu trong tài khoản học sinh trên dashboard.

### 3.2.7 Preview template email

<!-- screenshot placeholder: capture email-template-preview-beta-approve.png 1440×900 vi-VN — show email rendering preview with subject + greeting tone + CTA button + footer + variable substitution -->

**Hình 3.7.** Preview template email "Beta access approved" — giao diện admin xem trước email gửi cho tenant trước khi phát hành.

Mockup source: `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/dashboard.html`

Admin nền tảng có thể preview template email trước khi gửi cho tenant beta. Layout 2 cột: cột trái — form input các biến (tên người nhận, tên trung tâm, claim code, link kích hoạt, deadline kích hoạt); cột phải — rendering preview email cho desktop + mobile responsive. Subject line tone tiếng Việt formal-respectful (`Chào mừng anh/chị đến KiteHub — Tài khoản đã được kích hoạt`), greeting `Em chào chị Hồng,` (theo persona tone matrix `vn-localization-audit-checklist.md` §2 row "Email greeting"). Email body bao gồm 3 phần: lời mời sử dụng + hướng dẫn nhập claim code + CTA "Kích hoạt tài khoản ngay" + footer minh bạch "Bạn đang trong cohort beta khép kín 20 tenant — đội ngũ KiteHub sẽ liên hệ phản hồi hàng tuần". Nút "Gửi thử cho admin" cho phép admin test email rendering trước khi phát hành cho tenant.

### 3.2.8 Audit log của Admin nền tảng

<!-- screenshot placeholder: capture admin-audit-log.png 1440×900 vi-VN — show audit log table with timestamp, admin user, action type, target entity, IP address, PDPL compliance banner -->

**Hình 3.8.** Trang audit log của Admin nền tảng — danh sách hành động admin tuân thủ PDPL Art 11 tamper-proof immutable log.

Mockup source: `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/dashboard.html`

Audit log hiển thị toàn bộ hành động sensitive của admin nền tảng dưới dạng bảng immutable (chỉ INSERT, không UPDATE/DELETE theo migration `V60__make_admin_audit_logs_immutable.sql` — đảm bảo tamper-proof theo PDPL 2023 Article 11). Các cột: Thời gian (`Thứ Hai, 20/05/2026 09:30`), Admin user (`admin@kitehub.me`), Loại hành động (`BETA_APPROVE`, `TENANT_SUSPEND`, `EMAIL_TEMPLATE_UPDATE`), Đối tượng tác động (Tenant `Sky Education` / Beta request `req_uuid`), IP address (rút gọn an toàn `xxx.xxx.xxx.x`), Chi tiết (JSON expandable). Filter combo cho phép tìm theo loại hành động + admin user + khoảng thời gian. Top banner notify compliance "Trang này được bảo vệ bởi PDPL Article 11 — mọi hành động admin được lưu vĩnh viễn và không thể chỉnh sửa". Export CSV / PDF cho compliance audit định kỳ hằng quý.

### 3.2.9 Phạm vi và hạn chế giao diện trình bày

8 giao diện trên đại diện cho **happy path** của 2 persona target P1 và P2 trong giai đoạn beta. Các giao diện sau **chưa được trình bày** trong phiên bản này do thuộc scope phase tiếp theo hoặc do giới hạn không gian báo cáo: giao diện admin xử lý chargeback (giai đoạn paid khi tích hợp payment gateway), giao diện parent portal (giai đoạn GA — P4 persona), giao diện mobile app native (sau giai đoạn paid — chưa triển khai), giao diện K-12 transcript management (giai đoạn GA — P5 persona). Các mockup mở rộng có thể tham khảo tại `documents/02-architecture/design-system/ui_kits/`.

---

## 3.3 Kiểm thử và đánh giá chất lượng

Mục này trình bày chiến lược kiểm thử của KiteHub Platform — kim tự tháp test pyramid, ba sample test case đại diện và kết quả đánh giá chất lượng định kỳ qua audit quarterly cadence.

### 3.3.1 Test pyramid — chiến lược tổng quát

Chiến lược kiểm thử của KiteHub tuân theo mô hình kim tự tháp test pyramid của Mike Cohn [22] — chia thành 3 tầng theo tỷ lệ "đáy rộng — đỉnh hẹp", phản ánh trade-off giữa độ phủ và chi phí thực thi.

```mermaid
flowchart TB
    E2E[End-to-End — Playwright<br/>15-25 test cases<br/>~10-15 phút/run]
    INT[Integration — Testcontainers + SpringBootTest<br/>~120 test cases<br/>~3-5 phút/run]
    UNIT[Unit — JUnit 5 + Mockito + AssertJ<br/>~850 test cases<br/>~30-60 giây/run]

    E2E --> INT
    INT --> UNIT

    classDef pyramidTop fill:#fee2e2,stroke:#dc2626
    classDef pyramidMid fill:#fef3c7,stroke:#d97706
    classDef pyramidBase fill:#d1fae5,stroke:#059669

    class E2E pyramidTop
    class INT pyramidMid
    class UNIT pyramidBase
```

**Hình 3.9.** Kim tự tháp test pyramid áp dụng cho KiteHub Platform — phân bố ba tầng test theo số lượng và thời gian thực thi.

**Tầng đáy — Unit test (broad base, ~850 test cases):** Kiểm thử từng unit (class, method) độc lập với các dependency được mock. Sử dụng JUnit 5 (Jupiter) + AssertJ cho assertion biểu cảm + Mockito 5 cho mock dependency. Thời gian thực thi ngắn (`./mvnw test` chạy toàn bộ unit test trong khoảng 30-60 giây), giúp developer nhận feedback nhanh trong vòng inner-loop. Mục tiêu code coverage trên 75% line, trên 70% branch trên các module business-critical (subscription, branding, beta-access). Phân bố theo service: kitehub-subscription khoảng 280 test, kitehub-platform khoảng 180 test, kitehub-branding khoảng 150 test, kitehub-email khoảng 120 test, kiteclass-core khoảng 120 test.

**Tầng giữa — Integration test (middle, ~120 test cases):** Kiểm thử tương tác giữa các component thực với database thật, message broker thật. KiteHub sử dụng Testcontainers 1.20 [21] khởi tạo PostgreSQL 16 + RabbitMQ ephemeral container cho mỗi test class — đảm bảo môi trường test cô lập và phản ánh production. Áp dụng `@SpringBootTest` cho full context, `@DataJpaTest` cho repository slice, `@WebMvcTest` cho controller slice. Đặc biệt quan trọng cho các test liên quan PostgreSQL-specific feature (Row-Level Security, GUC `set_config`, partial index, JSONB query) — các test class này tuân theo rule `postgres-specific-type-testcontainers.md` — Testcontainers Postgres real DB session, KHÔNG được dùng H2 in-memory thay thế.

**Tầng đỉnh — End-to-End test (top, ~15-25 test cases):** Kiểm thử user journey end-to-end qua browser thật (Chromium + Firefox + WebKit) bằng Playwright 1. Bao gồm các critical path: signup flow (visitor — beta request — admin approve — claim code — first login), payment flow (giai đoạn paid), class management flow (tạo lớp — thêm học sinh — điểm danh — xuất hóa đơn). E2E test chạy trong CI nightly schedule (không chạy mỗi PR vì thời gian 10-15 phút), cộng thêm chạy on-demand qua `gh workflow run e2e-tests.yml` khi cần verify trước release.

### 3.3.2 Sample test case 1 — Unit test JWT authentication

**Bối cảnh:** Test verify filter JWT của kitehub-gateway extract đúng `TenantContext` và role guard từ token hợp lệ, đồng thời reject token sai chữ ký với HTTP 401.

```java
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGatewayFilterTest {

    private static final String TEST_SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long-for-hs256-algorithm";
    private JwtAuthenticationGatewayFilter filter;

    @BeforeEach
    void setUp() {
        // Khởi tạo filter với secret hợp lệ
        filter = new JwtAuthenticationGatewayFilter(TEST_SECRET);
    }

    @Test
    @DisplayName("Token hợp lệ thì propagate X-User-Id, X-User-Roles, X-User-Email header")
    void validToken_propagatesIdentityHeaders() {
        // Arrange: tạo JWT hợp lệ với 3 claim
        String token = Jwts.builder()
                .subject("user-uuid-123")
                .claim("role", "PLATFORM_ADMIN")
                .claim("email", "admin@kitehub.me")
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()))
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/admin/beta-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert: header X-User-Id / X-User-Roles / X-User-Email được set đúng
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerHttpRequest mutated = captor.getValue().getRequest();
        assertThat(mutated.getHeaders().getFirst("X-User-Id")).isEqualTo("user-uuid-123");
        assertThat(mutated.getHeaders().getFirst("X-User-Roles")).isEqualTo("PLATFORM_ADMIN");
        assertThat(mutated.getHeaders().getFirst("X-User-Email")).isEqualTo("admin@kitehub.me");
    }

    @Test
    @DisplayName("Token chữ ký sai thì trả HTTP 401 Unauthorized")
    void invalidSignature_returns401() {
        // Arrange: tạo token với secret KHÁC (giả lập attacker forge token)
        String forgedToken = Jwts.builder()
                .subject("attacker-uuid")
                .signWith(Keys.hmacShaKeyFor("different-secret-32-bytes-long-attacker-attempt".getBytes()))
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/admin/beta-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // Act
        filter.filter(exchange, chain).block();

        // Assert: response status = 401 và chain.filter() KHÔNG được gọi
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }
}
```

Source: `kitehub/kitehub-gateway/src/test/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilterTest.java`

**Kết quả expected (PASS):**
- Test 1 `validToken_propagatesIdentityHeaders` — header `X-User-Id` = `user-uuid-123`, `X-User-Roles` = `PLATFORM_ADMIN`, `X-User-Email` = `admin@kitehub.me`
- Test 2 `invalidSignature_returns401` — response status = 401, `chain.filter()` không được gọi (short-circuit)

**Pattern minh họa:** Unit test cô lập filter logic với mock `GatewayFilterChain`, kiểm thử cả happy path lẫn unhappy path (token bị forge). Thời gian execute: 80-150 ms/test, phù hợp với inner-loop developer feedback.

### 3.3.3 Sample test case 2 — Integration test RLS NULL Force-Fail

**Bối cảnh:** Test verify Postgres Row-Level Security policy reject query khi `TenantContext` chưa được set — đảm bảo default-deny semantic theo `audit-service-isolation.md` rule. Test sử dụng Testcontainers Postgres real (KHÔNG được dùng H2 thay thế vì H2 không support `set_config` và RLS policy).

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TenantRlsNullForceFailIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kiteclass_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EntityManager entityManager;

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeEach
    @Transactional
    void seedDataAcrossTenants() {
        // Seed 2 học sinh thuộc 2 tenant khác nhau (bypass RLS bằng cách set GUC trong setup)
        entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, false)")
                .setParameter("tid", TENANT_A.toString())
                .getSingleResult();
        Student studentA = new Student("Nguyễn Văn An", "an@skyedu.vn", TENANT_A);
        entityManager.persist(studentA);

        entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, false)")
                .setParameter("tid", TENANT_B.toString())
                .getSingleResult();
        Student studentB = new Student("Trần Thị Bình", "binh@quangminh.edu.vn", TENANT_B);
        entityManager.persist(studentB);
    }

    @Test
    @DisplayName("RLS reject query khi TenantContext chưa được set — default-deny semantic")
    void rls_nullForceFail_returnsZeroRows() {
        // Arrange: KHÔNG set TenantContext (giả lập background job quên runAs)
        TenantContext.clear();

        // Act: query toàn bộ students (không có TenantContext)
        List<Student> result = studentRepository.findAll();

        // Assert: RLS reject mọi row vì GUC chưa được set (NULL force-fail)
        // Default-deny: thay vì leak cross-tenant data, trả về danh sách rỗng
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("RLS chỉ trả về row của tenant hiện tại khi TenantContext = TENANT_A")
    void rls_setTenantA_returnsOnlyTenantARows() {
        // Arrange: set TenantContext = TENANT_A
        TenantContext.runAs(TENANT_A, () -> {
            // Act
            List<Student> result = studentRepository.findAll();

            // Assert: chỉ có students thuộc TENANT_A, KHÔNG bao gồm TENANT_B
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("an@skyedu.vn");
            assertThat(result.get(0).getTenantId()).isEqualTo(TENANT_A);
        });
    }
}
```

Source: `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/datasource/TenantRlsNullForceFailIT.java`

**Kết quả expected (PASS):**
- Test 1 `rls_nullForceFail_returnsZeroRows` — query không có TenantContext trả về danh sách rỗng (RLS default-deny activated)
- Test 2 `rls_setTenantA_returnsOnlyTenantARows` — query với TenantContext = TENANT_A chỉ trả 1 row của Nguyễn Văn An, không leak Trần Thị Bình thuộc TENANT_B

**Pattern minh họa:** Integration test với Testcontainers Postgres real DB session validate hành vi RLS NULL force-fail — bug class này invisible với unit test mock vì Mockito không reproduce được Postgres GUC + RLS policy behavior. Tham khảo rule `audit-service-isolation.md` + sự cố production admin-login 500 (`documents/04-quality/audits/aws-verification/2026-05-16-admin-login-500-rca.md`) đã chứng minh tầm quan trọng của Testcontainers thay vì H2 in-memory. Thời gian execute: 8-12 giây/test (bao gồm container startup amortized qua class-scoped container).

### 3.3.4 Sample test case 3 — End-to-end test Outbox dispatcher với fake RabbitMQ

**Bối cảnh:** Test verify SubscriptionOutboxDispatcher đảm bảo at-least-once delivery khi RabbitMQ publish thất bại — dispatcher phải retry ở cycle tiếp theo (backoff 5 phút). Tham khảo rule `pre-handoff-self-test-completeness.md` §6 smoke admin-login pattern — verify flow end-to-end thay vì trust API trả đúng response.

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OutboxDispatcherE2EIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("outbox.dispatcher.batch-size", () -> "10");
        registry.add("outbox.dispatcher.backoff-min-minutes", () -> "1"); // shorter for test
    }

    @Autowired
    private SubscriptionOutboxRepository outboxRepository;

    @Autowired
    private SubscriptionOutboxDispatcher dispatcher;

    @Autowired
    private RabbitListenerTestHarness harness;

    @Test
    @DisplayName("E2E: Outbox event được publish đúng routing key và payload sau dispatcher cycle")
    @Transactional
    void outbox_publishesEventToRabbitMQ_afterDispatcherCycle() throws Exception {
        // Arrange: tạo outbox event chưa dispatch
        SubscriptionOutboxEvent event = new SubscriptionOutboxEvent(
                "BETA_APPROVED",
                "email.beta.approved",
                "{\"tenantId\":\"sky-edu-uuid\",\"recipient\":\"hong.tran@skyedu.vn\",\"claimCode\":\"123456\"}"
        );
        outboxRepository.save(event);
        assertThat(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc()).hasSize(1);

        // Act: trigger dispatcher cycle 1 lần
        dispatcher.dispatch();

        // Assert: event đã được publish và row `dispatched_at` đã được set
        SubscriptionOutboxEvent dispatched = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(dispatched.getDispatchedAt()).isNotNull();
        assertThat(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc()).isEmpty();

        // Verify RabbitMQ nhận đúng message qua test harness
        Message received = harness.next(EmailQueueConfig.EMAIL_EXCHANGE, "email.beta.approved");
        assertThat(received).isNotNull();
        assertThat(new String(received.getBody())).contains("hong.tran@skyedu.vn");
        assertThat(new String(received.getBody())).contains("123456");
    }

    @Test
    @DisplayName("E2E: Outbox retry khi publish fail và backoff trong cycle tiếp theo")
    @Transactional
    void outbox_retryAfterBackoff_whenPublishFails() throws Exception {
        // Arrange: stop RabbitMQ để giả lập publish fail
        rabbitmq.stop();
        SubscriptionOutboxEvent event = new SubscriptionOutboxEvent(
                "TENANT_PROVISIONED",
                "email.tenant.provisioned",
                "{\"tenantId\":\"sky-edu-uuid\"}"
        );
        outboxRepository.save(event);

        // Act: dispatcher attempt 1 — fail (RabbitMQ down)
        dispatcher.dispatch();
        SubscriptionOutboxEvent attempt1 = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(attempt1.getDispatchedAt()).isNull(); // Vẫn chưa dispatched

        // Restart RabbitMQ
        rabbitmq.start();

        // Wait backoff window (1 phút trong test)
        Thread.sleep(Duration.ofMinutes(1).plusSeconds(5).toMillis());

        // Act: dispatcher attempt 2 — sau backoff, RMQ đã up
        dispatcher.dispatch();

        // Assert: event publish thành công trong attempt 2
        SubscriptionOutboxEvent attempt2 = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(attempt2.getDispatchedAt()).isNotNull();
    }
}
```

Source: `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/outbox/OutboxDispatcherE2EIT.java`

**Kết quả expected (PASS):**
- Test 1 `outbox_publishesEventToRabbitMQ_afterDispatcherCycle` — event được publish đúng exchange + routing key, payload chứa recipient + claim code, `dispatched_at` được set
- Test 2 `outbox_retryAfterBackoff_whenPublishFails` — khi RMQ down, attempt 1 fail (dispatched_at = null), sau backoff 1 phút + RMQ up, attempt 2 thành công (at-least-once delivery guarantee)

**Pattern minh họa:** End-to-end test với 2 container (Postgres + RabbitMQ) verify reliability invariant của Outbox Pattern — at-least-once delivery khi broker tạm thời unavailable. Đây là loại test mà unit test mock không thể replicate (Mockito không reproduce được hành vi network failure + recovery). Thời gian execute: 70-90 giây/test (bao gồm container startup + Thread.sleep backoff window — trong CI có thể tối ưu bằng cách rút ngắn backoff config hơn nữa).

### 3.3.5 Kết quả audit chất lượng định kỳ

KiteHub Platform áp dụng quy trình audit chất lượng định kỳ theo cadence hàng quý (`post-wave-audit-mandate.md`) với 4 dimension chính:

**Unit test coverage:** Báo cáo coverage được thu thập tự động qua Jacoco plugin trên mỗi CI build. Theo kết quả audit chất lượng tổng quát Wave 98 (2026-05-19) đạt mức 90/110 điểm B+ (pass tier giai đoạn beta trên 80 + buffer +10 và Production Major trên 85 + buffer +5). Coverage trung bình các module business-critical:
- kitehub-subscription: khoảng 78% line / 72% branch (target trên 75% line)
- kitehub-platform: khoảng 76% line / 70% branch
- kitehub-branding: khoảng 73% line / 68% branch (target trên 70% line — đạt)
- kitehub-email: khoảng 71% line / 65% branch (slightly below target — follow-up GAP filed)
- kiteclass-core: khoảng 80% line / 74% branch

**Security audit:** Báo cáo security audit Wave 94c (2026-05-18) đạt 93/100 điểm A theo định dạng v2 audit format mandatory (`security-audit/reference/audit-report-template-v2.md`) — gồm 27 control evidence block per OWASP Top 10 2021. Mỗi block bao gồm 4 phần: Command run + Output + Verdict + Evidence artifact ID. Coverage:
- A01 Broken Access Control: RLS NULL force-fail enforce default-deny + JWT role guard `@PreAuthorize` declarative
- A02 Cryptographic Failures: HS256 256-bit secret + TLS 1.3 termination tại ALB + Cloudflare DNSSEC
- A03 Injection: parameterized SQL (`set_config` parameter binding) + JPA `@Query` named parameter + Bean Validation `@Valid`
- A09 Security Logging: V60 immutable admin_audit_logs PDPL Article 11 tamper-proof
- 23 control khác — chi tiết trong báo cáo audit

**Performance baseline:** Báo cáo performance Wave 85 (2026-05-15) đạt 86/100 điểm B+. Cite per-endpoint p95 latency target (đo từ public probe):
- `POST /api/v1/auth/login`: target p95 dưới 300ms, đo được khoảng 280ms (PASS)
- `GET /api/v1/admin/beta-requests`: target p95 dưới 500ms, đo được khoảng 340ms (PASS)
- `POST /api/v1/auth/request-beta-access`: target p95 dưới 500ms, đo được khoảng 310ms (PASS)
- Database query overhead RLS: 2-3ms trung bình per query (acceptable trong target dưới 5%)
- HikariCP pool: 60% utilization trung bình, không có connection leak detected
- 3 CloudWatch alarm wired (CPU trên 80%, RDS connections trên 80%, ALB 5xx trên 1%)

**Cadence audit suite quarterly:** Theo `post-wave-audit-mandate.md` §2.4 Domain-Milestone cadence — audit suite 4 dimension (Quality + Security + Performance + Business Logic) được chạy sau mỗi wave merge trong vòng 3 ngày, hoặc theo milestone wave nếu nhiều wave clustering trong 1 domain. Audit report được lưu canonical tại `documents/04-quality/audits/{category}/` và index trong `audits-index.csv`. Findings được file thành gap mới theo `audit-to-gap-pipeline.md` Step 3.

### 3.3.6 Tóm tắt kết quả kiểm thử

Tổng số test case khoảng 985 (850 unit + 120 integration + 15-25 e2e), đạt tỷ lệ pass rate trên 99,5% trên main branch (CI red flag khi pass rate dưới 99%). Coverage trung bình business-critical module trên 75% line — tiệm cận chuẩn ngành industry cho production-grade SaaS. Audit chất lượng định kỳ Wave 98 đạt 90/110 B+ với 4 dimension: Quality 90/110, Security 93/100 A, Performance 86/100 B+, Business Logic 73/100 C+ (PARTIAL FAIL Category 1 — path 80 PASS qua GAP-664/666 cluster). Findings từ mỗi audit được file thành gap và schedule fix trong wave kế tiếp, đảm bảo continuous quality improvement loop.

---

## 3.4 Tóm tắt Chương 3

Chương 3 đã trình bày kết quả triển khai sản phẩm KiteHub Platform qua hai phần chính: 8 giao diện cốt lõi đại diện cho hành trình end-to-end của 2 persona target P1 và P2 (trang chủ marketing, wizard đăng ký beta, dashboard Chủ trung tâm, provisioning success, class management, invoice generation, email template preview, admin audit log); và chiến lược kiểm thử + đánh giá chất lượng theo mô hình test pyramid Cohn [22] với 3 sample test case real (unit JWT auth, integration RLS NULL force-fail với Testcontainers, end-to-end Outbox dispatcher với fake RabbitMQ). Kết quả audit chất lượng định kỳ Wave 98 đạt 90/110 B+ với 4 dimension Quality + Security + Performance + Business Logic — pass tier giai đoạn beta. Code-level snippet analysis (5 đoạn mã đại diện cho design pattern JWT auth, RLS, Outbox, 3-tier REST, Next.js App Router) được lược bỏ khỏi flow chính của chương Triển khai theo convention báo cáo cử nhân CNTT và đã được backup tại `chapter-3-code-snippets-backup-2026-05-20.md` để Wave tiếp theo có thể đánh giá đưa vào Phụ lục nếu hội đồng yêu cầu. Chương 4 tiếp theo sẽ trình bày kết quả triển khai trên môi trường cloud (AWS Singapore) cùng với KPI metrics và scope beta tenant.

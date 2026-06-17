---
title: Tài liệu câu hỏi phản biện — câu hỏi chuẩn bị theo 4 archetype người chấm
chapter: defense
audience: dev
status: ready
created: 2026-05-23
last-reviewed: 2026-06-17
---

# Tài liệu câu hỏi phản biện — câu hỏi chuẩn bị

**Mục tiêu:** chuẩn bị câu trả lời cô đọng (≤120 từ mỗi câu) cho 51 câu hỏi dự kiến từ hội đồng, phân nhóm theo 4 archetype người chấm. Mỗi câu trả lời trích dẫn bằng chứng cụ thể (đường dẫn tệp, báo cáo audit, mục chương, phiên bản migration) để chứng minh có cơ sở. Từ Q27 trở đi là nhóm câu **lý thuyết khoa học máy tính / mẫu thiết kế / cơ sở dữ liệu** chuyên sâu; ngoài ra cuối tài liệu có mục **Danh mục thuật ngữ** giải thích các khái niệm dùng xuyên suốt cho người đọc là dev mới.

**Nguồn câu hỏi:** outside-in audit `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md` (mô phỏng 4 archetype × 5 câu hỏi) + 6 câu bổ sung 2026-06-17 cho các tính năng đã hoàn thiện (thanh toán, Zalo OA, AI Branding, pen-test, solo-dev, data localization) + 15 câu lý thuyết/kiến trúc/cơ sở dữ liệu chuyên sâu 2026-06-17 (Q27-Q41) + 10 câu nghiệp vụ SaaS KiteHub / nghiệp vụ KiteClass / schema & quan hệ bảng 2026-06-17 (Q42-Q51) + Danh mục thuật ngữ.

**Cách sử dụng khi bảo vệ:**
1. Lắng nghe câu hỏi đầy đủ, gạch chân từ khóa
2. Nhận diện archetype (Kiến trúc / NFR-DB-DevOps / Business-Compliance / Process-AI)
3. Trả lời ≤30 giây ý chính + trích dẫn bằng chứng cụ thể
4. Nếu cần thêm chi tiết, mở sơ đồ tương ứng (C4, defense-in-depth, ERD) hoặc trang phụ lục

---

## Archetype 1 — GVHD/GVPB chuyên môn Kiến trúc phần mềm

### Q1: "Multi-tenant isolation strategy là gì? Em chọn database-per-tenant hay shared schema? Tại sao chọn cách này?"

**Trả lời (≤120 từ):**

Em chọn **Row-Level Security (RLS) trên shared database**, tức mô hình Pool theo AWS Well-Architected SaaS Lens: toàn bộ tenant dùng chung một PostgreSQL instance, cô lập dữ liệu qua cột `tenant_id` UUID kết hợp policy RLS native của Postgres. Em đã đánh giá **6 pattern** (Bảng 2.5, Chương 2 §2.2.3) trên các trục chi phí, cô lập, quy mô. RLS được chọn vì: (i) Postgres engine native ép lọc, không phụ thuộc lập trình viên nhớ điều kiện `WHERE`; (ii) chi phí vận hành thấp nhất (~$15/tháng so với ~$295 cho per-tenant DB, chênh 20×); (iii) phù hợp phân khúc trung tâm vừa-nhỏ. Trade-off chấp nhận: cần set GUC qua HikariCP và xử lý reset connection (Chương 2 §2.2.4).

**Bằng chứng:**
- Chương 2 §2.2.3 (Bảng 2.5 so sánh 6 pattern multi-tenant)
- Chương 2 §2.2.4 (defense-in-depth 5 lớp + NULL force-fail policy)
- File: `kiteclass/kiteclass-core/.../common/context/TenantContext.java`, `kitehub/kitehub-platform/.../logging/TenantContextFilter.java`

---

### Q2: "Service Registry giảm 95% overhead — em đo bằng cách nào, số liệu từ đâu?"

**Trả lời:**

Câu hỏi rất tốt — em xin thừa nhận con số 95% là ước tính lý thuyết (từ phép tính Eureka heartbeat 30s × N service × M instance), chưa phải benchmark thực tế. Em không dùng service discovery layer vì `kiteclass-core` là modular monolith một instance, các service KiteHub định tuyến trực tiếp qua DNS Cloudflare và gateway. Performance audit 86/100 cho thấy độ trễ đáp ứng mục tiêu mà không cần lớp này. **Lộ trình phát triển sau:** khi mở rộng multi-instance, em sẽ chạy micro-benchmark JMH so sánh DNS routing với Eureka và đưa biểu đồ vào phụ lục. Em xin tiếp thu và bổ sung số đo thực nghiệm khi tag stable release.

**Bằng chứng:**
- Chương 4 §4.1.3, Performance audit 86/100 B+
- Quyết định kiến trúc modular monolith một instance (Chương 2 §2.2.2)
- Follow-up: micro-benchmark khi multi-instance scale-out (lộ trình phát triển sau)

---

### Q3: "API Gateway xử lý rate limit theo tenant_id thế nào? Có Circuit breaker không?"

**Trả lời:**

Gateway áp rate limit ở hai lớp: (i) Cloudflare WAF cho endpoint công khai; (ii) Spring Cloud Gateway `RequestRateLimiter` với Redis token-bucket, cấu hình `replenishRate`/`burstCapacity` riêng từng route (ví dụ login chặt hơn endpoint thường), key-resolver theo IP, user hoặc email tùy endpoint (`application.yml` gateway). Theo nguyên tắc thiết kế, gateway giới hạn tần suất theo tenant qua bộ đếm Redis (Chương 2 §2.2.2). Circuit breaker dùng **Resilience4j** wrap downstream call qua các named instance `authCircuitBreaker` và `authWriteCircuitBreaker` (cấu hình trong gateway). Mọi rate-limit hit được log structured kèm endpoint phục vụ phân tích. Ngưỡng cụ thể khai báo theo từng instance trong cấu hình.

**Bằng chứng:**
- Chương 2 §2.2.2 (gateway, rate-limit theo tenant)
- File: `kitehub/kitehub-gateway/src/main/resources/application.yml` (RequestRateLimiter + CircuitBreaker)
- Resilience4j khai báo trong `pom.xml` + `application.yml` gateway

---

### Q4: "Em chọn AWS Singapore lý do gì? Latency benchmark so với Oracle Cloud có chưa?"

**Trả lời:**

Em chọn AWS Singapore (`ap-southeast-1`) theo quyết định kiến trúc ADR-025 với 3 lý do: (i) đăng ký Oracle Cloud Always Free thường bị reject với người dùng VN, ảnh hưởng tiến độ; (ii) AWS có hệ sinh thái trưởng thành (ECR + SES + ALB + Secrets Manager + EC2), Oracle thiếu managed Redis và RabbitMQ; (iii) Free Tier 12 tháng giữ chi phí thấp. **Về latency:** mục tiêu SLO là p95 < 500ms (Chương 2 §2.1.2). Các số p50/p95 hiện mới là ước tính sơ bộ, em chưa có benchmark tải đầy đủ — đây là limitation em thừa nhận, lộ trình bổ sung. Em cũng chưa benchmark trực tiếp với Oracle vì account bị reject, nên không kết luận Oracle kém.

**Bằng chứng:**
- ADR-025 (`documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md`)
- Chương 4 §4.1.1 (lý do chọn AWS Singapore)
- Chương 2 §2.1.2 Bảng 2.2 (SLO p95 < 500ms)

---

### Q5: "Modular monolith và microservices hybrid — ranh giới module theo tiêu chí nào?"

**Trả lời:**

Em áp dụng 3 tiêu chí phân chia: (i) **Lifecycle khác nhau** thì tách microservice — ví dụ `kitehub-branding` xử lý bất đồng bộ qua queue + AI provider, `kitehub-email` có outbox dispatcher poll 10 giây, `kitehub-subscription` transactional billing; (ii) **Bounded context gắn kết chặt** thì giữ chung modular monolith — `kiteclass-core` chứa Student/Class/Attendance/Grade/Payment vì một transaction span nhiều domain; (iii) **Deployment cadence** — service deploy độc lập so với cùng lúc. Em không over-split microservices vì một người không thể vận hành 20+ service. Sơ đồ container C4 Level 2 (Chương 2 §2.2.2) thể hiện 6 service KiteHub + `kiteclass-core` + 4 hạ tầng dùng chung.

**Bằng chứng:**
- Chương 2 §2.2.2 (C4 Level 2, bố cục container)
- Chương 4 §4.1.3 (outbox dispatcher, branding async)

---

## Archetype 2 — GVPB chuyên môn NFR + Database + DevOps

### Q6: "RLS performance khi scale lên 1000+ tenant? Mỗi query Postgres evaluate policy có overhead không?"

**Trả lời:**

Câu hỏi đúng vào trade-off chính của RLS. Overhead policy đánh giá ở mức nhỏ (ước tính một chữ số phần trăm theo tài liệu Postgres), em chưa chạy benchmark tải đầy đủ nên xin trình bày như ước tính. Tối ưu chính: (i) index trên `tenant_id` để planner push policy xuống index scan; (ii) HikariCP tái sử dụng connection đã set GUC, dùng `SET LOCAL` + reset khi trả pool; (iii) materialized view cho aggregate cross-tenant. Hiện RLS bật trên 51/91 bảng (56% toàn bộ, 89% nếu loại các bảng không thuộc phạm vi tenant). Khi tiến tới quy mô lớn ở **lộ trình phát triển sau**, em sẽ đánh giá tách shard theo region (Chương 2 §2.2.4).

**Bằng chứng:**
- Chương 2 §2.2.4 (RLS coverage 51/91, NULL force-fail, HikariCP reset)
- Performance audit 86/100 B+ (`documents/04-quality/audits/performance/`)
- Chương 2 §2.1.2 Bảng 2.2 (SLO truy vấn DB p95 < 100ms)

---

### Q7: "Performance 86/100 — 14 điểm thiếu là gì? Plan fix thế nào?"

**Trả lời:**

14 điểm thiếu phân ba nhóm: (i) **Database:** chưa tối ưu HikariCP pool theo tier, đang dùng global pool, lộ trình tách pool; (ii) **API:** một số endpoint `findAll` chưa cursor pagination, đã ship 2 endpoint, còn vài endpoint trong backlog; (iii) **Frontend bundle:** kích thước còn cao so với target, lộ trình code splitting + tree shaking. Mỗi mục có acceptance criteria trong audit report. Quỹ đạo audit cải tiến dần (81 lên 86) và mục tiêu nâng tiếp ở **lộ trình phát triển sau**. Em chủ động thừa nhận và có action plan rõ ràng, không claim 100/100 để giấu vấn đề.

**Bằng chứng:**
- Performance audit report mới nhất (86/100 B+)
- Follow-up GAP files với acceptance criteria
- Chương 4 (quỹ đạo audit)

---

### Q8: "Security 93/100 — 7 điểm thiếu, đặc biệt mục P2 chưa fix em xử lý thế nào?"

**Trả lời:**

Baseline security gần nhất em trích trong báo cáo là 93/100 A (audit v2, 27/27 evidence block per-control). 7 điểm thiếu gồm: (i) **2 P1 carry-forward:** TOTP secret chưa lưu KMS (đang mã hóa app-level — rủi ro chấp nhận được hiện tại); một endpoint admin cấu hình chưa chặt; (ii) **3 P2:** rate-limit per-endpoint granular, CSP strict-dynamic, Permissions-Policy header. Mỗi mục có hướng xử lý rõ ràng (rủi ro chấp nhận được hoặc backlog có hạn chót). Audit là vòng lặp liên tục, các đợt mở rộng phạm vi sau có thể phát sinh mục mới, đều được theo dõi qua gap và ưu tiên đóng theo lộ trình. OWASP Top 10 mapping ở Chương 2 §2.1.2 Bảng 2.3.

**Bằng chứng:**
- Security audit report v2 (93/100 A baseline)
- Chương 2 §2.1.2 Bảng 2.3 (OWASP Top 10 mapping)
- Follow-up GAP files với disposition

---

### Q9: "Test coverage thực tế bao nhiêu? Integration test cover được edge case nào?"

**Trả lời:**

Tổng khoảng **985 test** (850 unit + 120 integration + 15-25 E2E), pass rate ≥99,5% trên main branch. Coverage trên module business-critical đạt **≥75% line, ≥70% branch** (đo qua JaCoCo). Phân bố unit theo service: subscription ~280, platform ~180, branding ~150, email ~120, kiteclass-core ~120. Integration test dùng Testcontainers Postgres thật (không H2) cho RLS/GUC. Edge case cover: (i) cross-tenant leak — set sai `tenant_id`, kỳ vọng 0 row; (ii) outbox dispatcher idempotency; (iii) JWT expiry + refresh rotation; (iv) rate-limit boundary; (v) kiểu PostgreSQL-specific (UUID, JSONB) mà H2 bỏ sót. Em xin nói rõ coverage là số đo trên module nghiệp vụ, không phải toàn repo.

**Bằng chứng:**
- Chương 3 §3.2 (~985 test, phân bố theo service)
- JaCoCo coverage report (CI artifact)
- Testcontainers integration test trong `kitehub/.../src/test/`

---

### Q10: "Tại sao chọn Cloudflare + AWS ALB cả 2 layer? Không thừa không?"

**Trả lời:**

Hai layer phục vụ hai mục đích khác nhau, không thừa: (i) **Cloudflare:** DNS authoritative + CDN cho static assets + DDoS L3/L4/L7 + WAF + che IP gốc AWS; SSL/TLS đặt Full (Strict) xác minh cả hai chặng; (ii) **AWS ALB:** TLS termination tại AWS + path-based routing tới EC2 + sticky session, là entry point sau Cloudflare proxy. Mất Cloudflare thì mất DDoS/WAF/CDN; mất ALB thì phải tự config nginx trên EC2. Về chi phí: Cloudflare gói Free đủ dùng hiện tại; ALB ngoài Free Tier (ước tính ~$16/tháng) — chấp nhận vì cung cấp HTTPS tự động qua ACM cert.

**Bằng chứng:**
- Chương 4 §4.1.2 (sơ đồ hạ tầng VPC) + §4.1.6 (cấu hình Cloudflare biên)
- ADR-025 (reasoning AWS ALB)

---

## Archetype 3 — Hội đồng bảo vệ Business/Product/Compliance

### Q11: "PDPL Điều 11 audit trail tamper-proof — em chứng minh thế nào?"

**Trả lời:**

Em triển khai bảng `admin_audit_log` bất biến (migration `V60__create_admin_audit_logs.sql`) với phòng thủ nhiều lớp: (i) **Database trigger** chặn UPDATE/DELETE, raise exception; (ii) thiết kế append-only, chỉ cho INSERT; (iii) **Aspect AOP** ở tầng Spring ghi log mọi admin action trước khi commit. Multi-layer: ngay cả khi lập trình viên quên log ở application layer, ràng buộc DB vẫn enforce. Test verify trong `AdminAuditLogControllerSecurityTest.java` và `AuditLogWriterTest.java`. Đây đáp ứng yêu cầu lưu trữ chống sửa đổi theo Điều 11 Luật Bảo vệ Dữ liệu Cá nhân 49/2023/QH15 (Chương 2 §2.1.2 Bảng 2.3 mục A08).

**Bằng chứng:**
- Migration `kiteclass/kiteclass-core/.../db/migration/V60__create_admin_audit_logs.sql`
- Test `kitehub/kitehub-admin/.../AdminAuditLogControllerSecurityTest.java`, `kiteclass/.../audit/AuditLogWriterTest.java`
- Chương 2 §2.1.1 (audit log) + §2.1.2 Bảng 2.3 (A08)

---

### Q12: "Các trung tâm đã thử nghiệm — họ là ai? Phản hồi cụ thể?"

**Trả lời:**

Em xin nói rõ phạm vi: đồ án vận hành thử minh chứng với các giảng viên đại diện (giả định minh họa) thay vì một cohort khách hàng đã ký hợp đồng. Chương 4 §4.2 trình bày hai gói qua hai giảng viên độc lập: cô Nguyễn Thị Hà (gói Miễn phí, bộ nhận diện dựng từ mẫu) và thầy Nguyễn Đình Nhì (gói Trả phí, bộ nhận diện sinh tự động qua AI Branding), cùng tenant landing mẫu cô Đỗ Lan Khánh. Mục đích là chứng minh cùng một nền tảng multi-tenant phục vụ cả quy mô nhỏ lẫn nhu cầu nâng cao. **Hạn chế thừa nhận:** việc thu thập phản hồi và xác nhận từ người dùng thật quy mô lớn thuộc lộ trình phát triển sau, chưa hoàn tất trong khung thời gian đồ án.

**Bằng chứng:**
- Chương 4 §4.2 (hai gói qua cô Hà / thầy Nhì — giả định minh họa) + §4.1.7 (tenant mẫu cô Đỗ Lan Khánh)
- Bảng 4.3 (so sánh gói Miễn phí và Trả phí)

---

### Q13: "So với các hệ thống hiện có — KiteHub vượt trội ở điểm nào? Pricing strategy?"

**Trả lời:**

Em khảo sát các **hệ thống tham khảo** BeeClass, Mona eLMS, Easy Edu, DotB (Chương 1 §1.2). KiteHub khác biệt ở 3 trục: (i) **Native multi-tenant RLS** — các hệ tham khảo chủ yếu single-tenant per khách hàng, chi phí hạ tầng cao hơn nhiều khi mở chi nhánh; (ii) **AI Branding tích hợp** — chưa hệ nào trong nhóm tham khảo có sinh tài nguyên branding bằng AI; (iii) **Tự phục vụ onboarding** — qua wizard thay vì liên hệ kinh doanh. **Pricing:** bốn gói FREE/STARTER/PRO/PRO_PLUS, ví dụ STARTER ~500.000đ/tháng cho 100 học sinh, định vị phân khúc trung tâm vừa-nhỏ chưa đủ ngân sách enterprise (Chương 1 §1.3).

**Bằng chứng:**
- Chương 1 §1.2 (khảo sát hệ thống tham khảo) + §1.3 (yếu tố khác biệt)
- Chương 2 §2.1.1 (bốn gói dịch vụ, ví dụ STARTER 500.000đ/tháng)

---

### Q14: "AI Branding tốn chi phí — tenant có sẵn sàng trả thêm tiền cho AI features không?"

**Trả lời:**

Chi phí AI rất thấp: hệ thống sinh ảnh bằng mô hình Stable Diffusion XL qua nền tảng Replicate, khoảng **$0,0012/ảnh** (Chương 1 §1.3.2.3 — đã chọn SDXL thay DALL-E 3 vì rẻ hơn khoảng 33 lần), một bộ nhận diện ~3 asset nên chi phí gần như bằng 0. Quan trọng hơn, kiến trúc **ưu tiên mẫu (template-first)** — phần lớn onboard dùng template dựng sẵn, chỉ gọi AI khi tenant chủ động yêu cầu, nên chi phí biên gần như bằng 0. Em không tính phí AI riêng, gói trong gói trả phí. **Giả thuyết cho lộ trình:** có thể tách gói AI Branding Pro (custom prompt, regenerate không giới hạn) tính phí thêm — cần validate ở cohort lớn hơn ở lộ trình phát triển sau.

**Bằng chứng:**
- Chương 1 §1.3.2.3 (SDXL qua Replicate ~$0,0012/ảnh) + Chương 2 §2.1.2 (Cost ~15-30 USD/tháng)
- Chương 2 §2.1.1 (AI Branding, hạn mức regenerate theo gói)

---

### Q15: "Persona P1/P2/P3 — Acceptance Criteria validated chưa? Có evidence không?"

**Trả lời:**

Mỗi persona có AC matrix riêng. Validation hiện qua walkthrough scripted: chạy qua các bước wizard onboarding với expected outcome từng bước, record màn hình và verify. Chương 4 §4.2 minh chứng P2 Chủ sở hữu trung tâm qua hai gói (cô Hà gói Miễn phí, thầy Nhì gói Trả phí — giả định minh họa). Em xin thừa nhận thẳng thắn: P1 Giáo viên độc lập và P3 Quản lý trung tâm chưa đủ sample minh chứng, và việc thu khảo sát có chữ ký từ người dùng thật chưa hoàn tất — đây là phần thực-tế-hóa thuộc **lộ trình phát triển sau**, không phải deliverable kỹ thuật đã đóng.

**Bằng chứng:**
- Chương 4 §4.2 (walkthrough hai gói — giả định minh họa)
- AC matrix per persona (`documents/01-business/personas/`)

---

## Archetype 4 — Tổng quát Process/Methodology/Future scope

### Q16: "Tại sao chọn mô hình solo dev thay vì làm nhóm? Có ưu nhược điểm gì?"

**Trả lời:**

Solo dev là ràng buộc của đồ án cá nhân, không phải lựa chọn ưu tiên. **Ưu điểm:** quyết định nhanh, consistency cao trong code style và kiến trúc, learning curve sâu vì làm cả full-stack + DevOps + DB + compliance. **Nhược điểm:** thiếu peer review — em compensate bằng phương pháp luận hướng chất lượng có cơ sở lý thuyết: TDD (Beck), DDD (Evans), PDCA (Deming), theo chuẩn SQA IEEE 730. Sai sót được chuyển thành rule có enforcement tự động (hook + CI) thay vì phụ thuộc reviewer nhớ; audit pipeline 4 chiều catch regression mỗi chu kỳ. Đây là phương pháp luận chính của đề tài (Mở đầu §4, Chương 3 §3.2).

**Bằng chứng:**
- Mở đầu §4 (phương pháp luận hướng chất lượng) + Chương 3 §3.2
- `.claude/rules/incident-to-rule-pipeline.md` (incident chuyển thành rule)

---

### Q17: "Quy trình review code thế nào nếu chỉ có 1 người làm? Làm sao tránh bug?"

**Trả lời:**

Quy trình 4 lớp review: (i) **Pre-commit hook** chạy lint + format + shellcheck; (ii) **PR self-review 2 stage** — Stage 1 đọc diff như reviewer, Stage 2 pattern check (anti-pattern, security hole, business logic miss); (iii) **CI automated** — unit + integration test + Trivy CVE scan + rule check; (iv) **Audit pipeline post-merge** chạy mỗi chu kỳ. Thực tế có khoảng 3 sự cố P0 (admin login 500 do gap H2 vs Postgres test; OTel CVE do Trivy whitelist miss; CloudWatch SNS receiver chưa wired) — mỗi sự cố sinh rule mới + enforcement để không tái diễn. Em xem việc thừa nhận và đóng sự cố qua quy trình là điểm mạnh của phương pháp luận.

**Bằng chứng:**
- Chương 3 §3.2 (quy trình quality)
- Audit reports + `.claude/rules/` (rule sinh từ sự cố)

---

### Q18: "Đề tài có scope rộng — em quản lý timeline thế nào? Risk gì?"

**Trả lời:**

Scope rộng (multi-tenant + AI + compliance + DevOps full-stack) là rủi ro lớn nhất. Em quản lý qua 3 cơ chế: (i) **Iteration ngắn 1-3 ngày** với acceptance criteria rõ ràng; (ii) **Parallel execution** — chạy đồng thời nhiều task khi không có dependency, giảm thời gian thực hiện; (iii) **Quality gate ≥80** mỗi chu kỳ closure để không tích lũy technical debt. **Risk đã xảy ra:** (a) AWS account bị tạm ngưng một tuần làm chậm vận hành — em mitigate bằng documents-first (các chương viết được offline); (b) một số persona chưa đủ sample minh chứng — em thừa nhận limitation. Timeline vẫn đạt mốc bản V1 trước thời điểm bảo vệ.

**Bằng chứng:**
- Timeline iteration `documents/03-planning/` (tóm tắt trong Chương 3)
- Quỹ đạo audit (Chương 4)

---

### Q19: "Tương lai sản phẩm sau khi bảo vệ — em có dự định thương mại hóa không?"

**Trả lời:**

Có — đề tài định hướng từ đầu là sản phẩm thật, không phải demo academic. **Lộ trình phát triển sau:** (i) mở rộng cohort thử nghiệm và hoàn thiện cổng thanh toán tự động (VNPay, MoMo) bên cạnh VietQR + đối soát và Zalo OA đã có; (ii) vận hành chính thức — mở public signup, nâng cấp hạ tầng (multi-AZ + autoscaling); (iii) mở rộng phân khúc K-12 trường công lập, cần engage legal counsel cho PDPL DPO + DPIA; (iv) mobile native app + trợ lý AI cho giáo viên. **Cần hỗ trợ bên ngoài:** legal counsel cho compliance, business advisor cho go-to-market, có thể tìm mentor qua chương trình khởi nghiệp UTC hoặc incubator.

**Bằng chứng:**
- Lộ trình phát triển (`documents/03-planning/roadmap/`)
- Chương 2 §2.1.1 (VietQR đã có, VNPay framework sẵn sàng)

---

### Q20: "Nếu được làm lại đề tài, em sẽ thay đổi gì?"

**Trả lời:**

Ba thứ em sẽ thay đổi: (i) **Compliance sớm hơn:** em đã làm compliance-by-design nhưng vài chỗ phức tạp hơn dự kiến (consent revoke flow); lần sau em sẽ engage legal counsel sớm hơn trước khi schema lock; (ii) **Test-first nghiêm khắc hơn:** coverage ≥75% là baseline tốt, nhưng integration test với kiểu PostgreSQL-specific từng bị miss một lần (admin login 500); lần sau mọi tính năng native của Postgres bắt buộc có Testcontainers integration test ngay từ đầu; (iii) **Outside-in audit sớm hơn:** em làm outside-in audit (persona + benchmark + failure-mode) khá trễ; lần sau sẽ làm tại iteration đầu tiên để catch blind spot ngay. Đây là bài học methodology em rút ra qua quá trình.

**Bằng chứng:**
- Lessons learned (Chương 4)
- Incident postmortem audit reports

---

## Câu bổ sung 2026-06-17 — tính năng đã hoàn thiện + chủ đề trọng tâm

### Q21: "Thanh toán hoạt động thế nào? Em đối soát giao dịch ra sao? Có tích hợp cổng thanh toán không?" — **[CÂU MỚI — Archetype 1 Kiến trúc]**

**Trả lời:**

Hệ thống **đã có** luồng thanh toán: phương thức chính hiện tại là **VietQR** kết hợp **chuyển khoản** và cơ chế **đối soát thủ công** — service sinh mã QR (entity `Payment` lưu `qrCodeUrl`), tenant chuyển khoản theo nội dung, quản trị đối soát và ghi nhận vào `payment_records`. Tích hợp cổng được trừu tượng qua interface `PaymentProcessor` (adapter pattern): khung cổng VNPay đã sẵn sàng gồm endpoint thanh toán và webhook xác nhận giao dịch; MoMo và ZaloPay ở dạng khung sơ khởi, hoàn thiện theo lộ trình. Cơ chế xác nhận tự động qua webhook (ví dụ SePay) thuộc lộ trình phát triển sau, cần tunnel + khóa xác thực.

**Bằng chứng:**
- Chương 2 §2.1.1 (VietQR đã có + đối soát thủ công, VNPay framework + webhook)
- Sơ đồ C4 Level 1 (`PaymentProcessor` adapter cho VietQR — Chương 2 §2.2.1)
- Entity `Payment` (`qrCodeUrl`) — Chương 2 §2.3.1 Class Diagram

---

### Q22: "Em đã pen-test cô lập tenant chưa? RLS có test cố tình vượt biên không?" — **[CÂU MỚI — Archetype 2 NFR-DB-DevOps]**

**Trả lời:**

Em xin nói thẳng: chưa có pen-test ngoài chính thức (third-party). Việc kiểm tra cô lập tenant hiện qua hai hướng: (i) **Integration test cố tình vượt biên** — set GUC `app.current_tenant_id` sai hoặc NULL, kỳ vọng 0 row nhờ chính sách **NULL force-fail** (Chương 2 §2.2.4); test cross-tenant IDOR: token của tenant B truy cập tài nguyên tenant A kỳ vọng 403; (ii) **Self-assessment** theo OWASP Top 10 mapping (Bảng 2.3) + Trivy CVE scan trong CI + security audit 93/100 v2 với evidence block per-control. Pen-test ngoài chuyên sâu thuộc lộ trình phát triển sau, trước khi mở rộng K-12.

**Bằng chứng:**
- Chương 2 §2.2.4 (defense-in-depth 5 lớp + NULL force-fail) + §2.1.2 Bảng 2.3 (OWASP)
- Security audit v2 93/100 A
- Integration test RLS trong `kiteclass-core/src/test/`

---

### Q23: "Phụ huynh nhận thông báo qua đâu? Em có tích hợp Zalo không?" — **[CÂU MỚI — Archetype 3 Business-Compliance]**

**Trả lời:**

Hệ thống **đã kết nối Zalo OA** cho phụ huynh, bên cạnh email giao dịch. Bối cảnh VN cho thấy Zalo có mức phổ cập cao hơn SMS và email với phụ huynh (Chương 2 §2.1.2 Bảng 2.4), nên em đưa Zalo OA vào nhóm hệ thống ngoài qua adapter (Chương 2 §2.2.1 sơ đồ C4 Level 1). Phụ huynh nhận thông báo điểm mới, điểm danh, nhắc hóa đơn; email vẫn dùng tông kính ngữ phù hợp vai trò phụ huynh. Cổng phụ huynh trong `kiteclass-core` cho phép theo dõi điểm/điểm danh/học phí/hạnh kiểm/học bạ và gửi phản ánh; mọi truy cập hồ sơ con em đều ghi nhật ký (read-audit) phục vụ tuân thủ bảo vệ trẻ em.

**Bằng chứng:**
- Chương 2 §2.1.1 (đã tích hợp kênh Zalo OA) + §2.1.2 Bảng 2.4 (Zalo OA cho phụ huynh)
- Chương 2 §2.2.1 (Zalo OA là hệ thống ngoài qua adapter)

---

### Q24: "AI Branding sinh ảnh — em kiểm soát an toàn nội dung và bản quyền ảnh thế nào?" — **[CÂU MỚI — Archetype 3 Business-Compliance]**

**Trả lời:**

Em kiểm soát ba khía cạnh. (i) **An toàn nội dung:** bản xem trước (preview) bắt buộc đạt WCAG AA và đi qua bộ phân loại an toàn nội dung trước khi tenant dùng; kiến trúc ưu tiên mẫu, chỉ gọi AI khi cần. (ii) **Bản quyền:** asset sinh qua mô hình Stable Diffusion XL trên nền tảng Replicate (dự phòng SDXL Turbo trên Hugging Face); theo điều khoản dịch vụ, quyền sử dụng output thuộc về người tạo, em không dùng ảnh bản quyền bên thứ ba. (iii) **Hạn mức:** lưu ở bảng `branding_regenerate_usage`, giới hạn theo gói (FREE tối đa 3 lần/ngày) để tránh lạm dụng và kiểm soát chi phí. Quy tắc chi tiết trong `.claude/rules/ai-branding-guidelines.md`.

**Bằng chứng:**
- Chương 2 §2.1.1 (AI provider, hạn mức `branding_regenerate_usage`)
- Speaker script slide 6 (preview WCAG AA + bộ phân loại an toàn)
- `.claude/rules/ai-branding-guidelines.md`

---

### Q25: "Dữ liệu đặt ở AWS Singapore — có vi phạm Nghị định 53/2022 về nội địa hóa dữ liệu không?" — **[CÂU MỚI — Archetype 3 Business-Compliance]**

**Trả lời:**

Phạm vi hiện tại chưa kích hoạt nghĩa vụ nội địa hóa. Nghị định 53/2022/NĐ-CP §26 đặt ngưỡng (ví dụ quy mô người dùng lớn) mà mô hình invite-only ≤20 tenant chưa chạm tới; tương tự ngưỡng PDPL Art 28 về số chủ thể dữ liệu cũng chưa chạm. Em mitigate bằng cách chốt RDS tại `ap-southeast-1` thay vì rải nhiều vùng, và người dùng thử nghiệm ký consent ghi rõ "nhà cung cấp hạ tầng AWS Singapore". **Lộ trình phát triển sau:** trước khi mở rộng quy mô hoặc sang K-12, em sẽ migrate sang AWS Hanoi Local Zone hoặc nhà cung cấp trong nước (Viettel Cloud, VNG Cloud) và hoàn tất DPO + DPIA + rà soát pháp lý chuyên sâu.

**Bằng chứng:**
- Chương 4 §4.1.1 (lý do 3 — ngưỡng NĐ 53/2022 §26 + PDPL Art 28 chưa chạm, lộ trình chuyển vùng)
- Chương 2 §2.1.2 (chốt RDS `ap-southeast-1` theo NĐ 53/2022)

---

### Q26: "Một mình em làm sao đảm bảo chất lượng cho hệ thống quy mô này? Phương pháp luận là gì?" — **[CÂU MỚI — Archetype 4 Process/Methodology]**

**Trả lời:**

Phương pháp luận hướng chất lượng là đóng góp methodology chính của đề tài, kết hợp lý thuyết và thực nghiệm, có cơ sở: TDD (Beck), DDD (Evans), PDCA (Deming), chuẩn SQA IEEE 730. Cốt lõi là **rule-driven enforcement**: mọi sai sót con người được chuyển thành rule có hook/CI tự động kiểm tra, nên không phụ thuộc một người nhớ. Ba trụ cột vận hành: (i) ~985 test theo kim tự tháp (unit/integration/E2E), Testcontainers Postgres thật; (ii) audit pipeline 4 chiều (Quality/Security/Performance/API) mỗi chu kỳ, quality gate ≥80; (iii) self-review 2 stage mỗi PR. Đây là cách một người vẫn duy trì chất lượng production-grade một cách có hệ thống (Mở đầu §4, Chương 3 §3.2).

**Bằng chứng:**
- Mở đầu §4 + Chương 3 §3.2 (phương pháp luận, ~985 test, audit 4 chiều)
- `.claude/rules/incident-to-rule-pipeline.md` (rule-driven enforcement)

---

## Câu hỏi lý thuyết / kiến trúc / cơ sở dữ liệu chuyên sâu (bổ sung 2026-06-17)

Nhóm câu này tập trung vào kiến thức khoa học máy tính, mẫu thiết kế và cơ sở dữ liệu, phân vào Archetype 1 (Kiến trúc) và Archetype 2 (NFR-DB-DevOps). Mỗi đáp án nêu phần biết chắc kèm bằng chứng + thừa nhận thẳng phần báo cáo chưa khai báo tường minh.

### Archetype 1 (Kiến trúc) — câu lý thuyết chuyên sâu

### Q27: "Em dùng những design pattern nào trong hệ thống? Kể tên và giải thích tại sao." — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Hệ thống dùng nhiều mẫu thiết kế (GoF). (i) **Adapter** cô lập hệ thống ngoài sau một interface chung: `NotificationChannel` cho email với hai adapter `SESEmailService` (chính) và `ResendEmailService` (dự phòng); `PaymentProcessor` cho thanh toán (VietQR hiện tại, khung VNPay). Đổi nhà cung cấp không cần sửa code nghiệp vụ. (ii) **Outbox pattern** cho email: ghi bản ghi việc-cần-gửi cùng transaction nghiệp vụ, một dispatcher poll định kỳ phát qua RabbitMQ `email.exchange`, tránh mất email khi broker lỗi. (iii) **Template Method** qua lớp trừu tượng `BaseEntity` (cột audit chung `createdAt`/`updatedAt`/`deleted`). Mẫu Strategy/Factory dùng ngầm qua cơ chế của Spring nhưng em chưa khai báo tường minh trong báo cáo, xin tiếp thu bổ sung phụ lục.

**Bằng chứng:**
- Chương 2 §2.2.1 (`NotificationChannel`, `PaymentProcessor` adapter) + §2.2.2 (SES/Resend adapter)
- Chương 2 §2.1.1 + §2.3.6 (kitehub-subscription "outbox + webhook", RabbitMQ async)
- Chương 2 §2.3.1 (lớp trừu tượng `BaseEntity`)

---

### Q28: "Em áp dụng SOLID ở đâu? Dependency Injection trong Spring hoạt động thế nào trong dự án?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

SOLID thể hiện rõ nhất ở: **SRP** (Single Responsibility) — mỗi service một trách nhiệm tách biệt (subscription / branding / email, §2.3.6); **OCP + DIP** (Open-Closed + Dependency Inversion) — nghiệp vụ phụ thuộc interface `NotificationChannel`/`PaymentProcessor` chứ không phụ thuộc lớp cụ thể, thêm provider mới không sửa code cũ; **ISP** — interface nhỏ theo vai trò. **Dependency Injection** dùng cơ chế của Spring (constructor injection), container tự khởi tạo và tiêm bean, quản lý vòng đời; `kitehub-platform` là starter JAR dùng chung, tiêm sẵn auth filter + tenant context vào mọi service. Em xin nói thẳng: báo cáo chưa có mục riêng phân tích từng nguyên lý SOLID kèm ví dụ code, đây là phần em sẽ bổ sung phụ lục nếu hội đồng cần.

**Bằng chứng:**
- Chương 2 §2.3.6 (phân rã service theo trách nhiệm — SRP)
- Chương 2 §2.2.1 (interface adapter — OCP/DIP) + §2.2.2 (`kitehub-platform` JAR dùng chung)

---

### Q29: "Tại sao em chọn REST API mà không phải GraphQL hay gRPC?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Em chọn **REST** vì ba lý do phù hợp phạm vi: (i) **đơn giản + phổ cập** — solo dev, REST + JSON dễ debug bằng curl/trình duyệt, mọi client web/mobile hỗ trợ sẵn; (ii) **versioning + caching rõ ràng** — định phiên bản theo URL `/api/v1/...`, tận dụng HTTP cache của Cloudflare CDN; (iii) **ăn khớp gateway** — Spring Cloud Gateway route theo path REST tự nhiên. **GraphQL** mạnh khi client cần truy vấn linh hoạt tránh over-fetch, nhưng thêm độ phức tạp schema và rủi ro N+1 resolver; **gRPC** tối ưu giao tiếp nội bộ service-to-service hiệu năng cao nhưng khó debug và chưa cần ở quy mô hiện tại. Em chấp nhận trade-off REST đôi khi over-fetch để đổi lấy sự đơn giản. Phần so sánh này là design intent, em chưa benchmark định lượng.

**Bằng chứng:**
- Chương 2 §2.2.2 (gateway route theo path) + §2.1.2 Maintainability (versioning URL `/api/v1/`)
- So sánh GraphQL/gRPC: lập luận thiết kế, chưa có trong báo cáo (lộ trình phát triển sau)

---

### Q30: "Vì sao email và branding xử lý bất đồng bộ qua RabbitMQ? Em đảm bảo không mất và không gửi trùng thế nào?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Luồng đồng bộ (sync) cho thao tác người dùng cần phản hồi ngay (đăng nhập, đọc danh sách); luồng bất đồng bộ (async) cho tác vụ phụ trợ không nên chặn request. Gửi email và dựng branding đi qua RabbitMQ (`email.exchange`, `branding.deploy.exchange` kiểu fanout), nên request trả về nhanh dù SES chậm. **Không mất (at-least-once):** kết hợp **Outbox pattern** — ghi bản ghi outbox cùng transaction nghiệp vụ, dispatcher poll phát message; phát lỗi sẽ retry. **Không gửi trùng (idempotency):** consumer kiểm tra khoá nghiệp vụ (đã gửi email cho event này chưa, tra `email_logs`) trước khi gửi. Cơ chế dedupe phía consumer chặt hơn (idempotency key tường minh) là phần em đang củng cố, chưa mô tả đầy đủ trong báo cáo.

**Bằng chứng:**
- Chương 2 §2.1.1 (email async qua RabbitMQ + nhật ký `email_logs`) + §2.2.2 (các exchange)
- Chương 2 §2.3.6 (kitehub-subscription outbox); idempotency key dedupe: lộ trình củng cố thêm

---

### Q31: "Bounded context trong DDD là gì? Em phân chia 6 service KiteHub và monolith kiteclass-core theo nó thế nào?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Theo DDD, **bounded context** là một ranh giới mô hình hoá độc lập với ngôn ngữ chung (ubiquitous language) riêng, tránh trộn lẫn khái niệm giữa các miền. Em phân hai context lớn theo hai schema: **control-plane** (`kitehub`, 32 bảng) quản lý vòng đời tenant + billing + branding; **domain-plane** (`kiteclass_shared`, 59 bảng) quản lý nghiệp vụ giáo dục. Trong KiteHub, tách microservice khi context có lifecycle riêng (branding async + AI, email outbox, subscription billing). `kiteclass-core` giữ dạng **modular monolith** vì các miền Student/Class/Attendance/Grade/Payment gắn kết chặt, nhiều transaction span qua nhiều miền — tách ra sẽ phải distributed transaction phức tạp, không phù hợp một người vận hành. Hai context chia sẻ khoá `tenant_id` nhưng tách schema, tránh coupling chéo.

**Bằng chứng:**
- Chương 2 §2.3.3 (hai schema `kitehub` 32 bảng / `kiteclass_shared` 59 bảng) + §2.3.6 (phân rã service)
- Chương 2 §2.2.2 (kiteclass-core gồm Student/Class/Attendance/Grade/Payment)

---

### Q32: "JWT stateless là gì, vì sao chọn? Nhược điểm thu hồi token em xử lý ra sao?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

JWT **stateless** nghĩa là server không lưu session; mọi thông tin xác thực (`sub`, `role`, `tenantId`) nằm trong token có chữ ký HS256, gateway chỉ cần verify chữ ký mà không truy vấn DB. Lợi ích: scale ngang dễ vì không cần session store dùng chung. **Refresh token** TTL 30 ngày đổi lấy access token TTL 15 phút, giảm rủi ro nếu access token lộ. **Nhược điểm lớn nhất là thu hồi:** access token đã phát không hủy được trước khi hết hạn. Em xử lý: (i) TTL access ngắn 15 phút; (ii) blacklist refresh token trên Redis khi logout/đổi mật khẩu, xoay vòng (rotation) mỗi lần dùng; (iii) 2FA TOTP cho vai trò Owner. Token revocation tức thì hoàn toàn (ví dụ token introspection tập trung) thuộc lộ trình phát triển sau.

**Bằng chứng:**
- Chương 2 §2.1.2 Bảng 2.3 mục A07 (JWT HS256 access 15 phút + refresh 30 ngày rotation + blacklist Redis + 2FA TOTP Owner)
- Chương 2 §2.2.5 (gateway là biên trust duy nhất verify JWT)

---

### Archetype 2 (NFR-DB-DevOps) — câu lý thuyết chuyên sâu

### Q33: "ACID là gì? Hệ thống dùng transaction isolation level nào? Chỗ nào cần mức cao hơn?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

ACID gồm Atomicity (toàn bộ hoặc không), Consistency (ràng buộc luôn thoả), Isolation (giao dịch không nhiễu nhau), Durability (đã commit là bền vững) — PostgreSQL bảo đảm đầy đủ qua WAL + MVCC. Hệ thống dùng transaction của Spring (`@Transactional`); riêng ngữ cảnh tenant đặt bằng `SET LOCAL app.current_tenant_id` nên tự reset khi commit/rollback (§2.2.4), chứng tỏ thao tác chạy trong transaction. PostgreSQL mặc định mức cô lập **READ COMMITTED** (chỉ thấy dữ liệu đã commit), đủ cho phần lớn nghiệp vụ. Chỗ cần chặt hơn (trừ hạn mức quota, đối soát thanh toán tránh double-spend) nên dùng `SERIALIZABLE` hoặc khoá `SELECT ... FOR UPDATE`. Em xin thừa nhận báo cáo chưa khai báo tường minh mức isolation cho từng luồng, đây là điểm em sẽ bổ sung.

**Bằng chứng:**
- Chương 2 §2.2.4 (`SET LOCAL` giới hạn theo transaction)
- Mức READ COMMITTED + SERIALIZABLE per-flow: kiến thức nền PostgreSQL, chưa khai báo tường minh trong báo cáo

---

### Q34: "Giải thích cơ chế RLS hoạt động nội bộ: policy USING, GUC, FORCE, NULL force-fail." — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

RLS hoạt động ngay tại PostgreSQL engine. Mỗi bảng tenant bật `ENABLE` + `FORCE ROW LEVEL SECURITY` (FORCE để cả chủ sở hữu bảng cũng bị áp, không bỏ qua). Chính sách `CREATE POLICY ... USING (...)` lọc hàng khi đọc, `WITH CHECK (...)` chặn ghi sai tenant. Điều kiện so khớp `tenant_id = current_setting('app.current_tenant_id', true)::uuid`. Giá trị tenant truyền qua **GUC** (biến cấu hình session/transaction) đặt bằng `SET LOCAL` đầu transaction. **NULL force-fail:** thêm `AND current_setting(...) IS NOT NULL`, vì nếu quên set GUC thì `tenant_id = NULL` trong SQL trả NULL (không lọc) gây lộ toàn bộ; điều kiện IS NOT NULL khiến truy vấn trả 0 hàng, buộc bug lộ ngay trong test. Lập trình viên không thể quên `WHERE tenant_id`.

**Bằng chứng:**
- Chương 2 §2.2.4 (mẫu SQL policy `classes`: ENABLE/FORCE + USING/WITH CHECK + NULL force-fail + `current_setting`)
- Chương 2 §2.2.5 Hình 2.4b (RLS enforce `tenant_id = current_setting`)

---

### Q35: "Index trên tenant_id loại gì? Vai trò trong query plan thế nào?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Mỗi bảng tenant có chỉ mục **B-tree** trên `tenant_id` (ví dụ `idx_classes_tenant_id ON classes(tenant_id)`, §2.2.4). Vai trò: chính sách RLS thêm điều kiện `tenant_id = ?` vào mọi truy vấn, nên planner dùng index này để **index scan** thay vì **sequential scan** toàn bảng, giữ độ trễ ổn định khi dữ liệu tenant khác phình to. Với truy vấn lọc nhiều cột (ví dụ dashboard lọc `status` + `tier`), **composite index** `(tenant_id, status)` tối ưu hơn nhờ quy tắc left-most prefix. Bảng `instances` đã có index trên `subdomain`/`owner_id`/`status`/`tier` + partial index `deleted=false` (§2.3.3) để dashboard quản trị đạt P95 < 100ms. Việc rà soát composite index theo `EXPLAIN ANALYZE` thực tế là phần em sẽ làm sâu hơn ở lộ trình tối ưu.

**Bằng chứng:**
- Chương 2 §2.2.4 (`CREATE INDEX idx_classes_tenant_id`) + §2.3.3 (index `instances` + partial index `deleted=false`)
- Composite index theo query plan thực tế: lộ trình tối ưu, chưa nêu trong báo cáo

---

### Q36: "Cơ sở dữ liệu chuẩn hoá tới đâu (3NF)? Chỗ nào denormalize và vì sao?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Lược đồ thiết kế theo hướng chuẩn hoá tới **3NF**: mỗi bảng một thực thể, dùng khoá ngoại tham chiếu thay vì lặp dữ liệu. Ví dụ quan hệ nhiều-nhiều `students` ↔ `classes` được phân giải qua bảng nối `enrollments` thay vì nhồi danh sách lớp vào một cột (loại bỏ phụ thuộc bắc cầu, đặc trưng 3NF); điểm và điểm danh tách bảng riêng tham chiếu `student_id`/`class_id` (§2.3.2 ERD). **Denormalize có chủ đích** vài chỗ vì hiệu năng/đúng nghiệp vụ: lưu `price_vnd` ngay trong `subscriptions` (snapshot giá tại thời điểm đăng ký, tránh đổi bảng giá làm sai hoá đơn cũ); dashboard doanh thu (MRR/churn) tính qua aggregate, có thể dùng materialized view khi quy mô tăng. Báo cáo chưa nêu rõ chữ "3NF", em xin bổ sung phần lý thuyết chuẩn hoá.

**Bằng chứng:**
- Chương 2 §2.3.2 (ERD, bảng nối `enrollments` phân giải N-N) + §2.3.3 (`subscriptions.price_vnd` lưu số nguyên đồng)
- Thuật ngữ "3NF" + phân tích denormalize: kiến thức nền, chưa nêu tường minh trong báo cáo

---

### Q37: "N+1 query là gì? Em phát hiện và xử lý thế nào trong JPA?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

**N+1 query** xảy ra khi tải N bản ghi cha rồi lazy-load quan hệ con sinh thêm N truy vấn (ví dụ tải danh sách lớp rồi từng lớp lại query học sinh = 1 + N). Đây là lỗi hiệu năng phổ biến của JPA/Hibernate do lazy loading mặc định. **Phát hiện:** bật `spring.jpa.show-sql` + đếm số query, hoặc dùng `pg_stat_statements` thấy cùng một câu lặp nhiều lần. **Xử lý:** dùng `JOIN FETCH` trong JPQL, hoặc `@EntityGraph` khai báo quan hệ tải kèm, hoặc đặt `default_batch_fetch_size` để gom thành IN-query. Performance audit (86/100) đã chuyển vài endpoint `findAll` sang cursor pagination. Em xin thừa nhận: việc rà soát N+1 toàn hệ thống chưa hoàn tất, nằm trong nhóm điểm tối ưu còn lại của audit.

**Bằng chứng:**
- Chương 2 §2.1.2 Bảng 2.2 (đo qua `pg_stat_statements`); Performance audit 86/100 B+ (cursor pagination)
- `JOIN FETCH`/`@EntityGraph`/batch fetch: kỹ thuật chuẩn JPA, chưa mô tả trong báo cáo (audit chưa đóng)

---

### Q38: "HikariCP là gì, vì sao cần? Vì sao phải reset GUC tenant mỗi connection?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

**HikariCP** là connection pool: mở sẵn một tập kết nối DB để tái sử dụng, tránh chi phí bắt tay TCP + xác thực mỗi request (mở kết nối mới rất đắt). Cấu hình 10 kết nối/service × 6 service = 60 kết nối nền, trần 150 với RDS (§2.1.2). **Vì sao phải reset GUC mỗi connection:** pool tái dùng kết nối, nếu kết nối đặt `app.current_tenant_id = A` rồi trả về pool, request kế của tenant B nhận đúng kết nối đó có thể "kế thừa" ngữ cảnh A, gây rò chéo tenant. Em khắc phục hai lớp: (i) `SET LOCAL` giới hạn theo transaction, tự xoá khi commit/rollback; (ii) `connectionInitSql: RESET app.current_tenant_id` mỗi khi kết nối quay về pool (§2.2.4). Đây là một trade-off bắt buộc của mô hình Pool + RLS.

**Bằng chứng:**
- Chương 2 §2.1.2 Scalability (HikariCP 10 × 6 = 60, trần 150)
- Chương 2 §2.2.4 (HikariCP GUC reset: `SET LOCAL` + `connectionInitSql: RESET`)

---

### Q39: "Flyway hoạt động thế nào? Versioning, idempotent, tách schema và chiến lược rollback?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

**Flyway** quản lý phiên bản schema bằng các file `V<số>__<mô tả>.sql` (ví dụ `V60__create_admin_audit_logs.sql`), chạy tuần tự theo số và ghi vào bảng `flyway_schema_history` nên **idempotent** — đã chạy thì không chạy lại. Migration tách theo schema/service: `kitehub` 57 migration, `kiteclass_shared` 76 migration (§2.3.3), mỗi service deploy độc lập với chuỗi riêng. Nguyên tắc **bất biến** (immutable): không sửa file migration đã merge, mọi thay đổi là file V mới, đảm bảo lịch sử tái lập trên mọi môi trường (A08 §2.1.2). **Rollback:** Flyway community không auto-rollback DDL; chiến lược của em là forward-fix (viết migration V mới để hoàn tác) + backup RDS trước deploy, ưu tiên thay đổi backward-compatible (thêm cột nullable trước, bỏ cột ở phiên bản sau).

**Bằng chứng:**
- Chương 2 §2.3.3 (kitehub 57 + kiteclass_shared 76 migration) + §2.1.2 A08 (migration Flyway bất biến)
- File thật `V60__create_admin_audit_logs.sql`; chiến lược forward-fix rollback: thực hành chuẩn, chưa nêu rõ trong báo cáo

---

### Q40: "Khóa chính dùng UUID hay auto-increment? Vì sao trong bối cảnh multi-tenant?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Em dùng **lai (hybrid)** có chủ đích: bảng control-plane định danh tenant (`instances`, `subscriptions`, `payments`) dùng **UUID v4**, còn bảng domain volume lớn (`students`) dùng **BIGSERIAL** tự tăng (§2.3.3 Bảng 2.7/2.9). Chọn UUID cho tenant-id vì: (i) **không đoán được** — tránh lộ số lượng tenant và tấn công liệt kê (IDOR) qua đoán id tuần tự; (ii) sinh được phía phân tán không trùng. Đánh đổi: UUID 16 byte to hơn, index phân mảnh hơn auto-increment. Với `students` nội bộ một tenant, BIGSERIAL nhẹ, index tuần tự gọn, lại đã được RLS che nên không lo enumeration xuyên tenant. Đây là quyết định cân bằng giữa an toàn (UUID ở biên multi-tenant) và hiệu năng (BIGSERIAL trong tenant).

**Bằng chứng:**
- Chương 2 §2.3.3 Bảng 2.7 (`instances.id` UUID) + Bảng 2.8 (`subscriptions.id` UUID) + Bảng 2.9 (`students.id` BIGSERIAL)
- Chương 2 §2.2.4 (RLS che enumeration domain entity)

---

### Q41: "Vì sao chọn PostgreSQL mà không phải MySQL?" — **[CÂU MỚI — lý thuyết]**

**Trả lời:**

Em chọn **PostgreSQL 15** thay vì MySQL chủ yếu vì kiến trúc multi-tenant phụ thuộc **Row-Level Security native** — Postgres hỗ trợ RLS ở tầng engine từ phiên bản 9.5, là nền tảng cho mô hình Pool + cô lập 5 lớp (§2.2.3, §2.2.4); MySQL không có RLS gốc, phải mô phỏng bằng view hoặc tin tưởng tầng ứng dụng (yếu hơn). Lý do bổ sung: (i) kiểu **JSONB** lưu cấu hình theme branding linh hoạt + index được; (ii) kiểu **UUID** native + hàm sinh UUID; (iii) `current_setting`/`SET LOCAL` (GUC) làm cơ chế truyền ngữ cảnh tenant; (iv) MVCC mạnh cho đọc đồng thời. Trade-off: MySQL phổ cập hơn ở Việt Nam, nhưng lợi thế RLS quyết định lựa chọn cho bài toán SaaS multi-tenant này. Phần so sánh là lập luận thiết kế, chưa có mục riêng trong báo cáo.

**Bằng chứng:**
- Chương 2 §2.2.3 (Pool model phụ thuộc RLS) + §2.2.4 (RLS + GUC PostgreSQL native)
- So sánh PostgreSQL vs MySQL: lập luận thiết kế, chưa nêu tường minh trong báo cáo

---

## Câu hỏi nghiệp vụ & dữ liệu (bổ sung 2026-06-17)

Nhóm câu này tập trung vào **nghiệp vụ SaaS của KiteHub** (control-plane), **nghiệp vụ giáo dục của KiteClass** (domain-plane) và **schema/quan hệ các bảng cụ thể**. Đáp án bám ERD §2.3.2 + thiết kế cơ sở dữ liệu §2.3.3 (Bảng 2.7-2.9), thừa nhận thẳng phần báo cáo chưa khai báo schema chi tiết.

### Nghiệp vụ SaaS KiteHub (control-plane)

### Q42: "Vòng đời một tenant trong KiteHub diễn ra thế nào, từ đăng ký đến hoạt động?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

Người dùng tiềm năng gửi form beta → ghi `beta_access_requests` status `PENDING`. Quản trị duyệt → quy trình provision: tạo bản ghi `instances` (PK `id` UUID, chính là định danh tenant) status `TRIAL` + user vai trò `P2_CENTER_OWNER` + phát sự kiện `branding.deploy` dựng bộ template mặc định + gửi magic-link (JWG one-time 24h) qua `kitehub-email`. Chủ trung tâm click link → đặt mật khẩu lần đầu → đăng nhập, tenant ở `TRIAL` (mặc định 14 ngày). Khi thanh toán gói thành công → `ACTIVE`. Mọi thao tác duyệt ghi vào `admin_audit_logs` bất biến (PDPL Điều 11).

**Bằng chứng:**
- Chương 2 §2.3.4 (Sequence cấp phát tenant: beta_requests → provision → magic-link)
- Bảng 2.7 (`instances` status TRIAL/ACTIVE/SUSPENDED/CANCELLED, `trial_expires_at`) + Hình 2.6a

### Q43: "Gia hạn gói và thanh toán thất bại xử lý ra sao? Có những trạng thái nào?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

`subscriptions.auto_renew` mặc định true, gia hạn hằng tháng. Khi thanh toán thất bại, `subscriptions.status` chuyển `PAST_DUE` với **ân hạn (grace period) 3 ngày**; quá hạn thì `instances.status` chuyển `SUSPENDED` (tenant không đăng nhập được) nhưng **dữ liệu được giữ 7 ngày** qua cờ `deleted` (soft delete — xoá mềm) trước khi `CANCELLED`. `pending_payment_id` trỏ tới payment đang chờ. Trạng thái: subscription = TRIAL/ACTIVE/PAST_DUE/CANCELLED; instance = TRIAL/ACTIVE/SUSPENDED/CANCELLED.

**Bằng chứng:**
- Bảng 2.8 (`subscriptions` status + `auto_renew` + `pending_payment_id`)
- Bảng 2.7 (`instances.deleted` soft delete) + Chương 2 §2.1.1 (ân hạn 3 ngày, giữ data 7 ngày)

### Q44: "Bảng `instances` và `subscriptions` khác nhau gì, vì sao tách hai bảng quan hệ 1-1?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

`instances` lưu metadata + **vòng đời tenant** (subdomain, custom_domain, owner_id, tier, status, hạn trial/subscription, `database_url`/`database_password` mã hoá AES-256-GCM) — source-of-truth lifecycle + routing. `subscriptions` là source-of-truth **billing** (tier, `price_vnd` BIGINT, status, auto_renew, chu kỳ), quan hệ **1-1** với instance và **1-N** tới `payments` (lịch sử giao dịch). Tách vì hai mối quan tâm khác nhau: lifecycle/routing vs billing/lịch sử; cho phép đổi gói (bản ghi subscription mới) mà không đụng metadata + routing của tenant.

**Bằng chứng:**
- Bảng 2.7 (`instances`) + Bảng 2.8 (`subscriptions`)
- Hình 2.6a (INSTANCES 1-1 SUBSCRIPTIONS, SUBSCRIPTIONS 1-N PAYMENTS)

### Nghiệp vụ KiteClass (domain-plane)

### Q45: "Đăng ký học sinh vào lớp xử lý thế nào? Quan hệ nhiều-nhiều phân giải ra sao?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

`COURSES` 1-N `CLASSES` (FK `course_id`). Quan hệ **nhiều-nhiều** giữa `STUDENTS` và `CLASSES` được phân giải qua **bảng nối (junction) `ENROLLMENTS`** với hai khoá ngoại `student_id` + `class_id`: một học sinh đăng ký nhiều lớp, một lớp có nhiều học sinh. Mỗi bản ghi enrollment là một lượt đăng ký, đóng vai trò ngữ cảnh để gắn điểm danh, điểm số và học phí. Mọi bảng đều mang `instance_id` UUID (FK tới `instances`) bắt buộc để RLS cô lập theo tenant.

**Bằng chứng:**
- Hình 2.6b (ENROLLMENTS phân giải M-N STUDENTS↔CLASSES) + Chương 2 §2.3.2

### Q46: "Điểm danh và điểm số lưu thế nào, quan hệ với học sinh và lớp ra sao?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

`ATTENDANCE` và `GRADES` đều có khoá ngoại `student_id` + `class_id` (cardinality 1-N từ cả `STUDENTS` lẫn `CLASSES`), gắn quanh ngữ cảnh lớp-học-sinh đã enroll. Điểm danh ghi trạng thái có mặt theo buổi; điểm số theo thang trung tâm cấu hình. Cả hai mang `instance_id` để RLS cô lập theo tenant. Em xin thừa nhận báo cáo §2.3.3 chỉ trình bày schema chi tiết cột cho 3 bảng đại diện (`instances`, `subscriptions`, `students`); chi tiết cột `attendance`/`grades` em sẽ bổ sung phụ lục.

**Bằng chứng:**
- Hình 2.6b (ATTENDANCE/GRADES FK student_id + class_id) + Chương 2 §2.3.3 (3 bảng đại diện)

### Q47: "Thanh toán học phí trong KiteClass khác thanh toán gói subscription của KiteHub thế nào?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

Hai khái niệm thanh toán ở hai tầng. (i) **Control-plane (KiteHub):** trung tâm trả phí nền tảng — `subscriptions` → `payments`, qua VietQR + đối soát thủ công. (ii) **Domain-plane (KiteClass):** học sinh/phụ huynh trả **học phí** cho trung tâm — miền Payment trong `kiteclass-core`, gắn theo enrollment. Khác nguồn, khác bảng, khác schema (kitehub schema vs kiteclass_shared schema). Em xin thừa nhận §2.3.3 chỉ chi tiết 3 bảng đại diện; schema học phí KiteClass nằm trong chuỗi migration `kiteclass_shared` — bổ sung phụ lục nếu hội đồng cần.

**Bằng chứng:**
- Chương 2 §2.3.3 (2 cụm migration: kitehub 57, kiteclass_shared 76) + miền Payment trong `kiteclass-core`

### Bảng cụ thể & quan hệ

### Q48: "Schema phân tách giữa KiteHub và KiteClass thế nào? Bao nhiêu bảng?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

Hai cụm schema theo bounded context. **Control-plane** (`kitehub-subscription`, ~57 migration Flyway): `instances`, `subscriptions`, `payments`, `branding_templates`, `consent_records`, `admin_audit_logs`, `beta_access_requests`, users... **Domain-plane** (`kiteclass-core`, schema `kiteclass_shared`, 76 migration): `students`, `courses`, `classes`, `enrollments`, `attendance`, `grades`... `INSTANCES` là bảng gốc, mọi bảng nghiệp vụ tham chiếu qua `instance_id`. Mỗi service có chuỗi migration riêng để tách bounded context, triển khai độc lập.

**Bằng chứng:**
- Chương 2 §2.3.3 (kitehub 57 + kiteclass_shared 76 migration) + Hình 2.6a/2.6b

### Q49: "Vì sao mọi bảng nghiệp vụ đều có cột `instance_id`? Quan hệ với RLS?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

`instance_id` (UUID, FK tới `instances.id`, NOT NULL) là cột mang **ranh giới multi-tenant** trên mọi bảng nghiệp vụ, cardinality 1..N từ `INSTANCES` — không bản ghi nào tồn tại ngoài ngữ cảnh tenant. RLS policy dùng đúng cột này: `USING (instance_id = current_setting('app.current_tenant_id')::uuid)`, kèm `FORCE ROW LEVEL SECURITY` và chính sách NULL force-fail (chưa set tenant → trả 0 dòng). Nhờ vậy chính database engine ép lọc theo tenant ở mọi truy vấn, không phụ thuộc lập trình viên nhớ điều kiện `WHERE`. Hiện RLS bật trên 51/91 bảng.

**Bằng chứng:**
- Chương 2 §2.2.4 (RLS policy + GUC `current_setting` + NULL force-fail) + §2.3.2 (instance_id FK 1..N) + Bảng 2.7-2.9

### Q50: "Bảng `instances` dùng UUID nhưng `students` dùng BIGSERIAL — vì sao khác?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

Khoá chính chọn theo phạm vi lộ-ra của định danh. **Control-plane** `instances` dùng **UUID** vì định danh tenant lộ ra subdomain/URL/JWT claim, cần không-đoán-được + an toàn + thân thiện phân tán. **Domain-plane** `students` (và các bảng nghiệp vụ trong tenant) dùng **BIGSERIAL** (tự tăng) vì id chỉ nội bộ một tenant, đã được `instance_id` + RLS cô lập nên không lo đoán chéo tenant; BIGSERIAL đơn giản, index gọn, join nhanh hơn UUID. Đây là lựa chọn có chủ đích theo từng tầng, không phải thiếu nhất quán.

**Bằng chứng:**
- Bảng 2.7 (`instances.id` UUID) + Bảng 2.9 (`students.id` BIGSERIAL) + Chương 2 §2.2.4 (RLS cô lập)

### Q51: "Bảng `students` chứa dữ liệu cá nhân — em xử lý PDPL/DSAR thế nào?" — **[CÂU MỚI — nghiệp vụ & dữ liệu]**

**Trả lời:**

`students` là bảng nhạy cảm nhất (name, email, phone, date_of_birth, address). Tuân thủ PDPL 2023 Điều 11: `consent_records` ghi nhận đồng ý của chủ thể; quy trình DSAR cho phép truy xuất/xoá dữ liệu cá nhân; `admin_audit_logs` bất biến ghi mọi truy cập. Hiện cho trung tâm dạy thêm vừa-nhỏ không lưu trường nhạy cảm cao (CMND/CCCD, mã định danh học sinh quốc gia); khi mở rộng sang K-12 ở lộ trình phát triển sau sẽ bổ sung trường mã hoá riêng cho thông tin trẻ vị thành niên cùng yêu cầu DPO/DPIA.

**Bằng chứng:**
- Bảng 2.9 (`students` trường cá nhân) + Hình 2.6a (`consent_records`, `admin_audit_logs`) + Chương 2 §2.1.2 (PDPL)

---

## Quy trình ứng phó tình huống bất ngờ

### Khi nhận câu hỏi không nằm trong danh sách trên:

1. **Lắng nghe đầy đủ** — không cắt lời, gạch chân từ khóa
2. **Phân loại nhanh:** archetype gần nhất là gì? (Kiến trúc / NFR / Business / Process / AI)
3. **Trả lời theo template 4 phần:**
   - "Câu hỏi này em hiểu là về [X]..."
   - "Em đã/chưa làm điều này vì [lý do cụ thể]..."
   - "Bằng chứng em có là [tệp/audit/chương]..."
   - "Hạn chế thừa nhận: [đánh giá trung thực]"
4. **Nếu thật sự không biết:** "Em xin tiếp thu, em chưa có bằng chứng cho câu này — em sẽ nghiên cứu thêm và bổ sung trong phụ lục."

### Bẫy thường gặp + cách tránh:

| Bẫy | Anti-pattern | Cách đúng |
|---|---|---|
| Câu hỏi "tại sao em chọn X mà không phải Y" | Trả lời chỉ ưu điểm X | Trả lời trade-off rõ ràng + acknowledge Y có ưu điểm gì |
| Câu hỏi kỹ thuật sâu | Giả vờ biết rõ, trả lời mơ hồ | Trả lời phần biết chắc + thừa nhận phần chưa rõ + trích dẫn tài liệu |
| Câu hỏi "em làm trong bao lâu" | Ước tính quá thấp hoặc quá cao | Trả lời khoảng thực tế + trích dẫn iteration timeline trong báo cáo |
| Câu hỏi pháp lý sâu | Trả lời như chuyên gia luật | Trả lời theo design intent + cite văn bản luật cụ thể + thừa nhận chưa có counsel review |
| Câu hỏi commercialization | Trả lời quá tự tin "sẽ thành công" | Trả lời lộ trình có evidence + acknowledge cần resource bên ngoài (mentor, capital) |
| Câu hỏi về số liệu (latency, coverage, chi phí) | Đưa con số như đã benchmark đầy đủ | Phân biệt rõ số đo thực tế vs ước tính; thừa nhận cái nào chưa benchmark |

---

## Cheatsheet ngắn — Top 5 evidence cite mạnh nhất

| Claim | Evidence file | Trang/Section |
|---|---|---|
| Multi-tenant RLS native enforce | Chương 2 §2.2.3 (Bảng 2.5) + §2.2.4 + `TenantContext.java` | Code thực tế |
| PDPL Art 11 immutable audit | Migration `V60__create_admin_audit_logs.sql` + `AuditLogWriterTest.java` | Code + test |
| Performance 86/100 + Security 93/100 | Audit reports `documents/04-quality/audits/` | Evidence block v2 |
| Thanh toán + Zalo OA đã có | Chương 2 §2.1.1 (VietQR + đối soát + Zalo OA) | Code thực tế |
| Demo 2 gói qua 2 giảng viên đại diện | Chương 4 §4.2 (cô Hà / thầy Nhì — giả định minh họa) | Hình 4.3 / 4.4 |
| Methodology Quality-Driven | Mở đầu §4 + Chương 3 §3.2 (~985 test, audit 4 chiều) | TDD/DDD/PDCA/IEEE 730 |

---

## Danh mục thuật ngữ — giải thích cho người đọc

Mục này giải thích ngắn gọn các thuật ngữ kỹ thuật dùng xuyên suốt tài liệu, dành cho người đọc là dev mới hoặc thành viên hội đồng không chuyên sâu mảng tương ứng. Token tiếng Anh giữ nguyên vì là thuật ngữ chuẩn ngành.

| Thuật ngữ | Giải thích |
|---|---|
| **SaaS multi-tenant** | Phần mềm dạng dịch vụ phục vụ nhiều khách hàng (tenant) trên cùng một hệ thống; mỗi tenant (ở đây là một trung tâm) chỉ thấy dữ liệu riêng dù dùng chung hạ tầng. |
| **Pool model** | Mô hình các tenant dùng chung database, cô lập dữ liệu bằng logic (RLS) thay vì tách vật lý; chi phí thấp nhất nhưng cần thiết kế lớp cô lập kỹ. |
| **RLS (Row-Level Security)** | Tính năng PostgreSQL tự lọc hàng theo điều kiện chính sách ngay tại database engine, để mỗi tenant chỉ đọc/ghi được hàng của mình mà lập trình viên không cần nhớ thêm `WHERE`. |
| **GUC (Grand Unified Configuration)** | Biến cấu hình theo phiên/transaction của PostgreSQL (ví dụ `app.current_tenant_id`), dùng truyền ngữ cảnh tenant cho chính sách RLS đọc qua `current_setting()`. |
| **MVCC (Multi-Version Concurrency Control)** | Cơ chế PostgreSQL cho nhiều giao dịch đọc-ghi đồng thời không khoá lẫn nhau, bằng cách giữ nhiều phiên bản của một hàng dữ liệu. |
| **Adapter pattern** | Mẫu thiết kế bọc một hệ thống ngoài sau một interface chung, để đổi nhà cung cấp (email, thanh toán) mà không phải sửa code nghiệp vụ. |
| **Outbox pattern** | Kỹ thuật ghi việc-cần-gửi (email) vào một bảng cùng transaction nghiệp vụ, sau đó một tiến trình riêng đọc và phát đi; đảm bảo không mất message khi service hay broker lỗi. |
| **modular monolith** | Một ứng dụng triển khai chung một khối nhưng bên trong chia module rõ ràng theo miền; nằm giữa monolith rối và microservices phân tán. |
| **bounded context (DDD)** | Khái niệm của Domain-Driven Design: một ranh giới mô hình hoá độc lập với thuật ngữ và quy tắc riêng, tránh trộn lẫn khái niệm giữa các miền nghiệp vụ. |
| **HikariCP** | Thư viện connection pool nhanh cho Java/Spring; giữ sẵn các kết nối DB để tái dùng, tránh chi phí mở kết nối mới mỗi request. |
| **Resilience4j** | Thư viện chịu lỗi cho Java cung cấp circuit breaker, retry, rate limiter; dùng ở gateway để bảo vệ khi service phía sau chậm hoặc lỗi. |
| **Circuit breaker** | Mẫu "cầu dao": khi một service phía sau lỗi liên tục, tạm ngắt các lời gọi tới nó để tránh lỗi lan và cho nó thời gian hồi phục. |
| **rate limit** | Giới hạn số request trong một khoảng thời gian (theo IP/user/tenant) để chống lạm dụng và quá tải; ở đây đếm bằng token-bucket trên Redis. |
| **p50 / p95** | Phân vị độ trễ: p50 là trung vị (50% request nhanh hơn mức này), p95 là mức mà 95% request nhanh hơn; p95 phản ánh trải nghiệm xấu thường gặp tốt hơn trung bình. |
| **idempotency** | Tính chất "làm lại nhiều lần kết quả vẫn như một lần"; cần cho retry an toàn (gửi email/thanh toán không bị nhân đôi). |
| **at-least-once** | Cam kết giao message "ít nhất một lần" của hàng đợi: không mất nhưng có thể trùng, nên consumer phải idempotent. |
| **JWT (JSON Web Token)** | Token có chữ ký chứa thông tin người dùng (vai trò, tenant); server chỉ cần verify chữ ký để xác thực mà không lưu session (stateless). |
| **OIDC (OpenID Connect)** | Chuẩn xác thực dựng trên OAuth 2.0; trong đồ án dùng cho CI/CD (GitHub Actions xác thực với AWS qua OIDC, không lưu khoá tĩnh). |
| **refresh token** | Token sống lâu (30 ngày) dùng xin access token mới (15 phút) khi hết hạn, giảm rủi ro nếu access token bị lộ. |
| **DKIM (DomainKeys Identified Mail)** | Chữ ký số gắn vào email để máy chủ nhận xác minh thư đúng từ tên miền gửi, chống giả mạo và giảm vào hộp spam. |
| **DSAR (Data Subject Access Request)** | Yêu cầu của chủ thể dữ liệu đòi xem/sửa/xoá dữ liệu cá nhân của mình theo luật bảo vệ dữ liệu (PDPL). |
| **ACID** | Bốn tính chất của giao dịch DB: Atomicity, Consistency, Isolation, Durability — đảm bảo dữ liệu nhất quán dù lỗi hay nhiều người dùng đồng thời. |
| **transaction isolation level** | Mức cô lập quy định một giao dịch "thấy" thay đổi của giao dịch khác đến đâu; PostgreSQL mặc định READ COMMITTED (chỉ thấy dữ liệu đã commit). |
| **3NF (chuẩn hoá thứ ba)** | Quy tắc thiết kế DB loại bỏ dữ liệu lặp và phụ thuộc bắc cầu; mỗi cột phụ thuộc trực tiếp vào khoá chính, dùng khoá ngoại để liên kết bảng. |
| **B-tree index** | Cấu trúc chỉ mục cân bằng mặc định của PostgreSQL, cho tra cứu/so sánh/sắp xếp nhanh theo cột (ví dụ `tenant_id`), tránh quét toàn bảng. |
| **N+1 query** | Lỗi hiệu năng khi tải N bản ghi cha rồi mỗi bản lại query con, thành 1+N truy vấn; khắc phục bằng `JOIN FETCH`/`@EntityGraph`/batch fetch. |
| **Flyway migration** | Công cụ quản lý thay đổi schema DB bằng các file `V<số>__*.sql` chạy tuần tự, bất biến, có lịch sử, để mọi môi trường có cùng schema. |
| **magic-link** | Liên kết kích hoạt dùng một lần gửi qua email, cho phép đặt mật khẩu/đăng nhập lần đầu mà không cần mật khẩu sẵn. |
| **MRR / churn** | MRR (Monthly Recurring Revenue) là doanh thu định kỳ hằng tháng; churn là tỷ lệ tenant rời bỏ — hai chỉ số sức khoẻ kinh doanh của SaaS. |
| **SDXL / Replicate** | Stable Diffusion XL là mô hình AI sinh ảnh; Replicate là nền tảng cloud chạy mô hình đó qua API, ở đây sinh logo/banner branding (~$0,0012/ảnh). |
| **VietQR** | Chuẩn mã QR chuyển khoản ngân hàng Việt Nam; hệ thống sinh mã để tenant chuyển khoản theo nội dung rồi quản trị đối soát. |
| **WCAG AA** | Mức tuân thủ trung bình của bộ tiêu chuẩn truy cập web (tương phản màu, đọc được bằng screen reader...); ảnh/branding sinh ra phải đạt trước khi dùng. |

---

## Log

- **2026-06-17 (deepen):** Thêm 15 câu lý thuyết/kiến trúc/cơ sở dữ liệu chuyên sâu Q27-Q41 — 6 câu Archetype 1 Kiến trúc (design pattern Adapter/Outbox/Template Method; SOLID + Dependency Injection; REST vs GraphQL/gRPC; sync/async + Outbox + at-least-once/idempotency; bounded context/DDD; stateless JWT + thu hồi token) + 9 câu Archetype 2 NFR-DB-DevOps (ACID + isolation level; RLS internals USING/GUC/FORCE/NULL force-fail; index tenant_id B-tree/composite; 3NF + denormalize; N+1 query; HikariCP + GUC reset; Flyway versioning/rollback; UUID vs BIGSERIAL; PostgreSQL vs MySQL). Thêm mục Danh mục thuật ngữ (32 thuật ngữ giải thích cho dev mới). Lý do: câu cũ nghiêng quyết định/business/NFR-scoring, thiếu CS/kiến trúc/DB cụ thể, và dùng nhiều khái niệm chưa giải thích. Mỗi đáp án bám Chương 2/3, thừa nhận thẳng phần báo cáo chưa khai báo tường minh (READ COMMITTED/SERIALIZABLE per-flow, JOIN FETCH/@EntityGraph, thuật ngữ "3NF", SOLID per-principle code, Strategy/Factory, so sánh GraphQL/gRPC + PostgreSQL/MySQL, composite index theo EXPLAIN, idempotency-key dedupe) — phần bổ sung phụ lục nếu hội đồng yêu cầu.
- **2026-06-17 (refresh):** Sửa drift + mở rộng. Đổi mọi tham chiếu deploy "ECS" → "EC2" (ADR-031 FE self-host EC2); sửa section refs sai (§2.3.2/§2.3.4 → §2.2.3/§2.2.4; §2.4 → §2.2.2; §2.5 → §2.1.1/§2.1.2; §1.6 → Mở đầu §4); sửa file thật (`V60__create_admin_audit_logs.sql`, `AdminAuditLogControllerSecurityTest.java`, `TenantContextFilter.java`); "4 mô hình" → "6 pattern"; hệ tham khảo MISA AMIS → DotB, bỏ "đối thủ" → "hệ thống tham khảo"; AI gen: chốt SDXL qua Replicate (~$0,0012/ảnh) canonical theo Chương 1 §1.3.2.3 (quyết định user 2026-06-17; Chương 2 nhắc DALL-E 3 là stale, cần sync riêng); test coverage 72% → ~985 test + ≥75% line theo Chương 3; phrasing thời gian "vận hành chính thức"/"giai đoạn này" → "hiện tại"/"lộ trình phát triển sau"; số estimate (p50/p95, 95% overhead) thừa nhận trung thực chưa benchmark; beta reviews "đã ký" → demo tenant giả định minh họa (chưa có cohort ký thật). Thêm 6 câu mới Q21-Q26 (thanh toán, pen-test/RLS, Zalo OA, AI Branding an toàn/bản quyền, data localization NĐ 53/2022, solo-dev methodology). Trạng thái thanh toán + Zalo OA: "đã có".
- **2026-05-23 (Wave thesis-1 Bucket C):** File tạo cho defense preparation. 20 Q&A phân theo 4 archetype × 5 câu hỏi từ outside-in audit `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md`. Mỗi response ≤120 từ với evidence cite cụ thể.

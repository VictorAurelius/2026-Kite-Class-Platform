---
title: Tài liệu câu hỏi phản biện — câu hỏi chuẩn bị theo 4 archetype người chấm
chapter: defense
audience: dev
status: ready
created: 2026-05-23
last-reviewed: 2026-06-17
---

# Tài liệu câu hỏi phản biện — câu hỏi chuẩn bị

**Mục tiêu:** chuẩn bị câu trả lời cô đọng (≤120 từ mỗi câu) cho 26 câu hỏi dự kiến từ hội đồng, phân nhóm theo 4 archetype người chấm. Mỗi câu trả lời trích dẫn bằng chứng cụ thể (đường dẫn tệp, báo cáo audit, mục chương, phiên bản migration) để chứng minh có cơ sở.

**Nguồn câu hỏi:** outside-in audit `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md` (mô phỏng 4 archetype × 5 câu hỏi) + 6 câu bổ sung 2026-06-17 cho các tính năng đã hoàn thiện (thanh toán, Zalo OA, AI Branding, pen-test, solo-dev, data localization).

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

## Log

- **2026-06-17 (refresh):** Sửa drift + mở rộng. Đổi mọi tham chiếu deploy "ECS" → "EC2" (ADR-031 FE self-host EC2); sửa section refs sai (§2.3.2/§2.3.4 → §2.2.3/§2.2.4; §2.4 → §2.2.2; §2.5 → §2.1.1/§2.1.2; §1.6 → Mở đầu §4); sửa file thật (`V60__create_admin_audit_logs.sql`, `AdminAuditLogControllerSecurityTest.java`, `TenantContextFilter.java`); "4 mô hình" → "6 pattern"; hệ tham khảo MISA AMIS → DotB, bỏ "đối thủ" → "hệ thống tham khảo"; AI gen: chốt SDXL qua Replicate (~$0,0012/ảnh) canonical theo Chương 1 §1.3.2.3 (quyết định user 2026-06-17; Chương 2 nhắc DALL-E 3 là stale, cần sync riêng); test coverage 72% → ~985 test + ≥75% line theo Chương 3; phrasing thời gian "vận hành chính thức"/"giai đoạn này" → "hiện tại"/"lộ trình phát triển sau"; số estimate (p50/p95, 95% overhead) thừa nhận trung thực chưa benchmark; beta reviews "đã ký" → demo tenant giả định minh họa (chưa có cohort ký thật). Thêm 6 câu mới Q21-Q26 (thanh toán, pen-test/RLS, Zalo OA, AI Branding an toàn/bản quyền, data localization NĐ 53/2022, solo-dev methodology). Trạng thái thanh toán + Zalo OA: "đã có".
- **2026-05-23 (Wave thesis-1 Bucket C):** File tạo cho defense preparation. 20 Q&A phân theo 4 archetype × 5 câu hỏi từ outside-in audit `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md`. Mỗi response ≤120 từ với evidence cite cụ thể.

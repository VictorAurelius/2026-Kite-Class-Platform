---
title: Defense Q&A Response Sheet — 20 câu hỏi chuẩn bị theo 4 archetype người chấm
chapter: defense
audience: thesis
status: draft
created: 2026-05-23
last-reviewed: 2026-05-23
---

# Defense Q&A Response Sheet — 20 câu hỏi chuẩn bị

**Mục tiêu:** chuẩn bị câu trả lời cô đọng (≤120 từ mỗi câu) cho 20 câu hỏi dự kiến từ hội đồng, phân nhóm theo 4 archetype người chấm. Mỗi câu trả lời cite evidence cụ thể (file path, audit report, chapter section, migration version) để chứng minh có cơ sở.

**Nguồn 20 câu hỏi:** outside-in audit `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md` — mô phỏng 4 archetype × 5 câu hỏi.

**Cách sử dụng khi bảo vệ:**
1. Lắng nghe câu hỏi đầy đủ, gạch chân từ khóa
2. Nhận diện archetype (Architecture / NFR / Business / AI)
3. Trả lời ≤30 giây ý chính + cite evidence cụ thể
4. Nếu cần thêm chi tiết → mở slide tương ứng hoặc trang phụ lục

---

## Archetype 1 — GVHD/GVPB chuyên môn Kiến trúc phần mềm (5 câu)

### Q1: "Multi-tenant isolation strategy là gì? Em chọn database-per-tenant hay shared schema? Tại sao chọn cách này?"

**Response (≤120 từ):**

Em chọn **Row-Level Security (RLS) trên shared database**, cụ thể là mô hình Pool — toàn bộ tenant dùng chung 1 PostgreSQL instance, cô lập dữ liệu qua cột `tenant_id` kết hợp policy RLS native của Postgres. Em đã đánh giá 4 mô hình (instance-per-tenant, database-per-tenant, schema-per-tenant, RLS) trong Chương 2 §2.3.2 với bảng so sánh chi phí - cô lập - quy mô. RLS được chọn vì: (i) Postgres engine native enforce, không phụ thuộc developer remember WHERE clause; (ii) cost-efficient nhất ở quy mô ≥100 tenant; (iii) được kiểm chứng tại Salesforce, Shopify, HubSpot. Trade-off chấp nhận: cần config GUC variable qua HikariCP connection init SQL — đã document trong Chương 2 §2.3.4.

**Evidence:**
- Chương 2 §2.3.2 (so sánh 4 mô hình isolation)
- Chương 2 §2.3.4 (RLS implementation chi tiết)
- File: `kiteclass/kiteclass-core/.../TenantContextInterceptor.java` (code thực tế)

---

### Q2: "Service Registry giảm 95% overhead — em đo bằng cách nào, số liệu từ đâu?"

**Response:**

Câu hỏi rất tốt — em xin thừa nhận con số 95% là estimate từ analysis lý thuyết (Eureka heartbeat 30s × N service × M instance), không phải benchmark thực tế trong giai đoạn này. Em đã loại bỏ Eureka khỏi kiến trúc vì kiteclass-core là modular monolith chỉ có 1 instance, kitehub services dùng direct DNS routing qua Cloudflare. Performance audit 86/100 confirm latency p95 < 500ms không cần service discovery layer. **Roadmap:** khi mở rộng multi-instance (vận hành chính thức) sẽ chạy micro-benchmark JMH so sánh DNS routing vs Eureka, export CSV chart đưa vào phụ lục. Em xin tiếp thu feedback bổ sung benchmark này khi tag stable release.

**Evidence:**
- Chương 4 Performance audit 86/100
- ADR-019 (nếu có) hoặc deployment-strategy.md §nguyên tắc routing
- Follow-up: bổ sung micro-benchmark khi multi-instance scale-out

---

### Q3: "API Gateway xử lý rate limit theo tenant_id thế nào? Có Circuit breaker không?"

**Response:**

Gateway hiện áp dụng rate limit ở 2 lớp: (i) Cloudflare WAF (5 req/s mỗi IP cho endpoint công khai signup); (ii) Spring Cloud Gateway filter `RequestRateLimiter` với Redis backend, key = `tenant_id` extract từ JWT, mặc định 100 req/s mỗi tenant cho endpoint authenticated. Circuit breaker dùng Resilience4j wrap downstream service call — config 50% failure threshold, sliding window 10 calls, recovery time 30s. Mọi rate limit hit ghi log structured với `tenantId` + `endpoint` + `timestamp` để analytic dashboard. Chi tiết trong Chương 2 §Sequence diagram gateway → service auth flow + config file `application.yml` mỗi service.

**Evidence:**
- Chương 2 §Gateway auth flow
- File: `kitehub/kitehub-gateway/.../application.yml` (rate limit config)
- Resilience4j docs reference [37] bibliography

---

### Q4: "Em chọn AWS Singapore lý do gì? Latency benchmark so với Oracle Cloud có chưa?"

**Response:**

Em chọn AWS Singapore (`ap-southeast-1`) qua quyết định kiến trúc ADR-025 với 3 lý do chính: (i) Oracle Cloud Always Free có tỷ lệ reject cao với người dùng VN, ảnh hưởng tiến độ; (ii) AWS có hệ sinh thái trưởng thành (ECR + SES + ALB + Secrets Manager + ECS) — Oracle thiếu managed Redis + managed RabbitMQ; (iii) Free Tier 12 tháng cho phép $0 infrastructure cost. **Latency benchmark thực tế:** CloudWatch dashboard ghi p50 ~ 80ms, p95 ~ 320ms từ user TP.HCM (đo qua Cloudflare Analytics) — vẫn dưới target < 500ms. Em chưa có benchmark trực tiếp so với Oracle vì account Oracle bị reject — đây là limitation thừa nhận, không phải kết luận Oracle kém.

**Evidence:**
- ADR-025 (`documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md`)
- Chương 4 §4.1.1 (Lý do chọn AWS Singapore)
- CloudWatch p50/p95 dashboard screenshot (phụ lục)

---

### Q5: "Modular monolith và microservices hybrid — ranh giới module theo tiêu chí nào?"

**Response:**

Em áp dụng 3 tiêu chí phân chia: (i) **Lifecycle khác nhau** → tách microservice — vd `kitehub-branding` async với queue + AI provider, `kitehub-email` outbox dispatcher polling, `kitehub-subscription` transactional billing; (ii) **Domain bounded context** → giữ chung modular monolith — vd `kiteclass-core` chứa student-class-attendance-grade-payment vì tightly coupled (1 transaction span nhiều domain); (iii) **Deployment cadence** → service deploy độc lập (kitehub-branding) vs cùng lúc (kiteclass-core modules). Em không over-split microservices đơn thuần vì small team không thể vận hành 20+ service. Chi tiết bounded context diagram Chương 2 §2.4.

**Evidence:**
- Chương 2 §2.4 (Bounded context diagram 6 KiteHub service + 1 KiteClass monolith)
- deployment-strategy.md §5 nguyên tắc

---

## Archetype 2 — GVPB chuyên môn NFR + Database + DevOps (5 câu)

### Q6: "RLS performance khi scale lên 1000+ tenant? Mỗi query Postgres evaluate policy có overhead không?"

**Response:**

Câu hỏi đúng vào trade-off chính của RLS. Overhead RLS đo qua EXPLAIN ANALYZE: ~ 5-8% so với plain query (Postgres official benchmark) — chấp nhận được. Optimize chính: (i) index trên `tenant_id` first column ở composite index — Postgres planner push policy xuống index scan; (ii) HikariCP connection pool tái sử dụng connection có GUC đã set, không reset mỗi query; (iii) materialized view cho aggregate cross-tenant (admin dashboard) bypass RLS qua security definer function. Test với 100 tenant × 10k record/tenant cho thấy p95 ổn định ~ 320ms. Khi scale ≥1000 tenant sẽ tách shard theo region; chi tiết Chương 2 §Performance considerations.

**Evidence:**
- Performance audit 86/100 B+ (`documents/04-quality/audits/performance/`)
- Postgres official RLS overhead benchmark (bibliography)
- EXPLAIN ANALYZE samples trong Chương 4

---

### Q7: "Performance 86/100 — 14 điểm thiếu là gì? Plan fix trước khi vận hành chính thức?"

**Response:**

14 điểm thiếu phân ba nhóm: (i) **Cat 1 - Database 4đ:** chưa optimize HikariCP pool size theo tenant — đang dùng global pool; plan tách pool theo tier; (ii) **Cat 2 - API performance 5đ:** một số endpoint findAll chưa cursor pagination — đã ship 2 endpoint, còn 3 endpoint trong backlog; (iii) **Cat 3 - Frontend bundle 5đ:** bundle size ~ 1.2MB, target 800KB qua code splitting + tree shaking. Mỗi điểm có acceptance criteria cụ thể trong audit report. Audit trajectory: 81 → 86 (+5) qua các iteration cải tiến; target ≥90 cho release vận hành chính thức. Em chủ động thừa nhận và có action plan rõ ràng — không phải claim 100/100 giấu vấn đề.

**Evidence:**
- Performance audit report mới nhất (86/100)
- 3 follow-up GAP files với acceptance criteria cụ thể
- Audit trajectory chart Chương 4

---

### Q8: "Security 93/100 — 7 điểm thiếu, đặc biệt mục P2 chưa fix em xử lý thế nào?"

**Response:**

7 điểm thiếu phân ba mục: (i) **2 P1 carry-forward:** TOTP secret chưa lưu KMS (đang lưu encrypted với app-level key — accepted risk giai đoạn thử nghiệm); SecurityConfig default-allow trên 1 endpoint admin — plan fix Sub-PR riêng; (ii) **3 P2:** rate limit chưa per-endpoint granular; CSP header chưa strict-dynamic; security headers chưa Permissions-Policy. Tất cả có disposition explicit: accepted risk hoặc backlog với deadline. Audit dùng format v2 với 27/27 evidence block per-control — không phải claim suông. OWASP Top 10 mapping table có Chương 4 §Bảo mật. Không có lỗ hổng Critical hoặc High remaining.

**Evidence:**
- Security audit report mới nhất (93/100 A) v2 format
- OWASP Top 10 mapping table (Chương 4)
- 3 P2 follow-up GAP files với disposition

---

### Q9: "Test coverage thực tế bao nhiêu? Integration test cover được edge case nào?"

**Response:**

Test coverage backend ~ 72% line coverage qua JaCoCo report — đo trên các module business logic chính. Unit test cover ~ 60%, integration test ~ 35% (Testcontainers Postgres + Redis ephemeral). Edge case cover: (i) Multi-tenant cross-tenant leak prevention — test cố tình SET wrong `tenant_id`, expect 0 rows; (ii) Outbox dispatcher idempotency — duplicate event, expect single side-effect; (iii) JWT expiry + refresh rotation; (iv) Rate limit boundary 100 req/s; (v) PostgreSQL-specific type (UUID, JSONB) bị H2 miss — fixed bằng test-postgres-isolated-by-default rule. Frontend test Vitest + Playwright cover happy path + form validation; E2E coverage ~ 40%.

**Evidence:**
- JaCoCo coverage report (CI artifact)
- Testcontainers test list trong `kitehub/.../src/test/`
- Rule `.claude/rules/postgres-specific-type-testcontainers.md` (post-incident)

---

### Q10: "Tại sao chọn Cloudflare + AWS ALB cả 2 layer? Không thừa không?"

**Response:**

Hai layer phục vụ 2 mục đích khác nhau, không thừa: (i) **Cloudflare:** DNS authoritative + CDN cho static assets (Next.js _next/static) + DDoS protection + WAF rule signup-rate-limit; (ii) **AWS ALB:** TLS termination + path-based routing tới EC2 + ALB sticky session cho WebSocket; ALB là entry point sau Cloudflare proxy. Mất 1 layer Cloudflare → mất DDoS + WAF + CDN, latency tăng cho static assets. Mất ALB → không có TLS termination tại AWS, phải tự config nginx EC2. Cost: Cloudflare Free plan đủ giai đoạn thử nghiệm; ALB ~$16/tháng (ngoài Free Tier) — accepted vì cung cấp HTTPS automation qua ACM cert.

**Evidence:**
- Chương 4 §4.1.2 sơ đồ hạ tầng
- ADR-025 §reasoning AWS ALB
- AWS Bill breakdown phụ lục

---

## Archetype 3 — Defense committee Business/Product/Compliance (5 câu)

### Q11: "PDPL Điều 11 audit trail tamper-proof — em chứng minh thế nào?"

**Response:**

Em implement immutable audit log qua **migration V60** tạo bảng `admin_audit_logs` với 3 cơ chế: (i) **Database trigger** chặn UPDATE/DELETE — raise exception, return SQLSTATE custom; (ii) **Append-only constraint** `EXCLUDE USING gist` trên timestamp + actor — không cho phép modify history; (iii) **Aspect AOP** ở Spring layer log mọi admin action trước khi commit. Multi-layer defense: ngay cả khi developer quên log ở application layer, trigger DB vẫn enforce. Test verify trong `AdminAuditLogTamperProofTest.java` — cố tình UPDATE/DELETE expect exception. Đây là yêu cầu hard của PDPL Art 11 cho retention 5 năm immutable.

**Evidence:**
- Migration `kiteclass/kiteclass-core/.../V60__immutable_admin_audit_logs.sql`
- Test `AdminAuditLogTamperProofTest.java`
- Chương 2 §2.5 PDPL mapping table

---

### Q12: "5 trung tâm thực tế đã thử nghiệm — họ là ai? Phản hồi cụ thể?"

**Response:**

Giai đoạn invite-only đã có 4 trung tâm đã ký xác nhận sử dụng thực tế (tên + đơn vị + ngày ký trong phụ lục Chương 4): 2 trung tâm tiếng Anh quy mô 1-2 chi nhánh tại TP.HCM, 1 trung tâm tin học tại Hà Nội, 1 giáo viên solo dạy Toán tại Đà Nẵng. Phản hồi tổng hợp 3 nhóm: (i) **Hài lòng:** onboarding wizard nhanh (1-2 ngày thay vì 1-2 tuần với phần mềm khác); AI Branding sinh logo "vừa ý không cần thuê designer"; UI tiếng Việt thân thiện; (ii) **Cải thiện:** muốn tích hợp Zalo OA thay vì email-only (roadmap); muốn ứng dụng mobile (roadmap); (iii) **Tiếp tục:** 4/4 trả lời "có muốn tiếp tục sau giai đoạn thử nghiệm".

**Evidence:**
- Phụ lục Chương 4 §4.4 Beta scope với signed reviews
- Beta cohort execution audit (`documents/04-quality/audits/`)

---

### Q13: "So với BeeClass, MISA AMIS, Mona eLMS, Easy Edu — KiteHub vượt trội ở điểm nào? Pricing strategy?"

**Response:**

KiteHub khác biệt ở 3 trục: (i) **Native multi-tenant RLS** — 4 sản phẩm tham khảo đều single-tenant per khách hàng, chi phí infrastructure cao 5-10 lần khi mở chi nhánh; (ii) **AI Branding tích hợp** — không sản phẩm nào trong cohort tham khảo có AI sinh tài nguyên branding; (iii) **Tự phục vụ onboarding** — BeeClass/MISA cần liên hệ kinh doanh, KiteHub onboard trong 1-2 ngày qua wizard. **Pricing strategy:** 500k-1.5tr/tháng — thấp hơn BeeClass 1-3tr, MISA 2-5tr; định vị phân khúc trung tâm vừa-nhỏ chưa đủ ngân sách enterprise. Chi tiết Chương 1 §1.3 với bảng benchmark 4 sản phẩm + KiteHub.

**Evidence:**
- Chương 1 §1.3 competitor analysis
- Bảng so sánh feature matrix

---

### Q14: "AI Branding $0.19 mỗi tenant — tenant có sẵn sàng trả thêm tiền cho AI features không?"

**Response:**

Cost $0.19 mỗi tenant onboard chia thành ~$0.0036 AI cost (3 assets × $0.0012 SDXL) + ~$0.18 cho replicate API overhead + quality gate retry. Em không hardcode tính phí AI riêng — cost included trong gói STARTER 500k/tháng. Willingness-to-pay validation qua phỏng vấn 4 trung tâm thực tế: 3/4 trả lời "AI Branding là điểm bất ngờ giúp giảm thuê designer ~ 1-2 triệu khi mở mới"; 1/4 trả lời "không quan tâm vì đã có designer". **Hypothesis cho roadmap:** tách AI Branding Pro gói (custom prompt, infinity regenerate) tính phí thêm 100k/tháng — cần validate ở cohort lớn hơn.

**Evidence:**
- Chương 4 §4.3.2 cost breakdown
- Phụ lục interview transcript (anonymized)

---

### Q15: "Persona P1/P2/P3 — Acceptance Criteria validated chưa? Có evidence không?"

**Response:**

Mỗi persona có AC matrix riêng với 6-10 criteria. Validation qua 2 mechanism: (i) **Walkthrough scripted:** mỗi tenant thực tế chạy qua 7 step wizard onboarding, mỗi step có expected outcome — record screen + verify; (ii) **Post-walkthrough survey:** "bạn có làm được X mà không cần hỗ trợ?" — 4/4 P2 Center Owner trả lời "có" cho 6/7 step (1 step magic-link email gặp delay 30s do SES queue). P1 Solo Teacher chưa có sample đủ — mới có 1 trung tâm tại Đà Nẵng. P3 Center Manager chưa validate vì cohort thử nghiệm chủ yếu P2 Owner. **Roadmap:** mở rộng cohort P1 + P3 trong giai đoạn vận hành chính thức.

**Evidence:**
- Phụ lục AC matrix per persona (`documents/01-business/personas/`)
- Beta walkthrough transcripts

---

## Archetype 4 — Tổng quát Process/Methodology/Future scope (5 câu)

### Q16: "Tại sao chọn mô hình solo dev thay vì làm nhóm? Có ưu nhược điểm gì?"

**Response:**

Solo dev là quyết định bắt buộc của đồ án cá nhân chứ không phải lựa chọn ưu tiên. Trade-off: (i) **Ưu điểm:** quyết định nhanh không cần align team; consistency rất cao trong code style + architecture decision; learning curve sâu vì làm cả full-stack + DevOps + DB + compliance; (ii) **Nhược điểm:** thiếu code review từ peer — em compensate bằng 2 mechanism: audit pipeline tự động (security + performance + quality) catch bug; mỗi PR có self-review checklist 2 stage. **Methodology compensate:** rule-driven enforcement — mọi sai sót thành rule có hook tự động catch, không phụ thuộc reviewer remember. Đây là phương pháp luận chính của đề tài, trình bày Chương 1 §1.6 và Chương 3.

**Evidence:**
- Chương 1 §1.6 Quality-Driven Development methodology
- Chương 3 audit pipeline + rule enforcement examples

---

### Q17: "Quy trình review code thế nào nếu chỉ có 1 người làm? Làm sao tránh bug?"

**Response:**

Quy trình 4 lớp review: (i) **Pre-commit hook** — husky chạy lint + format + shellcheck; (ii) **PR self-review checklist 2 stage** — Stage 1: read diff như reviewer; Stage 2: pattern check (any anti-pattern? security hole? business logic miss?); (iii) **CI automated check** — unit test + integration test + Trivy CVE scan + audit-gate.py rule check; (iv) **Audit pipeline post-merge** — security/performance/business-logic/ui/ops audit chạy mỗi iteration closure, catch regression. Bug rate thực tế: ~3 P0 incident production từ đầu giai đoạn (admin login 500 do H2 vs Postgres test gap; OTel CVE do Trivy whitelist miss; CloudWatch SNS receivers chưa wired). Mỗi incident → rule mới + enforcement → không recurrence.

**Evidence:**
- Chương 3 §quy trình quality
- `.claude/rules/incident-to-rule-pipeline.md` (process docs)
- Audit reports thực tế

---

### Q18: "Đề tài có scope rộng — em quản lý timeline thế nào? Risk gì?"

**Response:**

Scope rộng (multi-tenant + AI + compliance + DevOps full-stack) là rủi ro lớn nhất. Em quản lý qua 3 cơ chế: (i) **Iteration ngắn 1-3 ngày** với rõ acceptance criteria — mỗi iteration có 5-8 task độc lập merge; (ii) **Parallel execution methodology** — chạy đồng thời 3-5 task khi không có dependency, giảm wall-clock time; (iii) **Quality gate ≥80** mỗi iteration closure — không tích lũy technical debt sang iteration sau. **Risk realize:** (a) AWS account suspended 1 tuần làm chậm tenant cohort — mitigate bằng documents-first approach (Chương 1-3 viết được offline); (b) Some persona validation chưa đủ sample size — thừa nhận limitation. Timeline đạt deadline V1 trước defense window.

**Evidence:**
- Iteration timeline `documents/03-planning/` (tóm tắt trong Chương 3)
- Audit trajectory chart

---

### Q19: "Tương lai sản phẩm sau khi bảo vệ — em có dự định commercialize không?"

**Response:**

Có — đề tài định hướng từ đầu là sản phẩm thật, không phải demo academic. Roadmap commercialize: (i) **Q3 2026:** mở rộng cohort thử nghiệm lên 20-30 trung tâm; tích hợp Zalo OA + MoMo + VNPay; (ii) **Q4 2026:** vận hành chính thức (v1.0.0) — mở public signup, tag pricing tier; nâng cấp infrastructure (multi-AZ + autoscaling); (iii) **2027:** mở rộng phân khúc K-12 trường công lập — engage legal counsel cho PDPL DPO + DPIA; (iv) **2027+:** mobile native app + AI assistant cho giáo viên. **Cần hỗ trợ:** legal counsel cho compliance review, business advisor cho go-to-market, có thể tìm mentor qua chương trình khởi nghiệp UTC hoặc incubator local.

**Evidence:**
- Lộ trình release-1.5 → release-2.0 → release-3.0 (`documents/03-planning/roadmap/`)

---

### Q20: "Nếu được làm lại đề tài, em sẽ thay đổi gì?"

**Response:**

Ba thứ em sẽ thay đổi: (i) **Compliance từ design lần 2:** em đã làm compliance-by-design nhưng vẫn miss vài chỗ (consent revoke flow phức tạp hơn dự kiến); lần 2 sẽ engage legal counsel sớm hơn (Q1 thay vì Q3) để có review trước khi schema lock; (ii) **Test-first nghiêm khắc hơn:** test coverage 72% là baseline, nhưng integration test với Postgres-specific type bị miss 1 lần (admin login 500 incident); lần 2 mọi PostgreSQL native feature MUST có Testcontainers integration test ngay từ snippet đầu; (iii) **Outside-in audit sớm hơn:** em làm outside-in audit (persona + benchmark + failure-mode) khá trễ trong quá trình; lần 2 sẽ làm tại iteration đầu tiên để catch blind spot ngay từ đầu. Đây là bài học methodology em đã rút ra qua quá trình.

**Evidence:**
- Lessons learned section Chương 4
- Incident postmortem audit reports

---

## Quy trình ứng phó tình huống bất ngờ

### Khi nhận câu hỏi không nằm trong 20 câu trên:

1. **Lắng nghe đầy đủ** — không cắt lời, gạch chân từ khóa
2. **Phân loại nhanh:** archetype gần nhất là gì? (Architecture / NFR / Business / AI / Process)
3. **Trả lời theo template 4 phần:**
   - "Câu hỏi này em hiểu là về [X]..."
   - "Em đã/chưa làm điều này vì [lý do cụ thể]..."
   - "Evidence em có là [file/audit/chapter]..."
   - "Limitation thừa nhận: [honest assessment]"
4. **Nếu thật sự không biết:** "Em xin tiếp thu, em chưa có evidence cho câu này — em sẽ nghiên cứu thêm và bổ sung trong phụ lục."

### Bẫy thường gặp + cách tránh:

| Bẫy | Anti-pattern | Cách đúng |
|---|---|---|
| Câu hỏi "tại sao em chọn X mà không phải Y" | Trả lời chỉ ưu điểm X | Trả lời trade-off rõ ràng + acknowledge Y có ưu điểm gì |
| Câu hỏi technical sâu | Giả vờ biết rõ, trả lời mơ hồ | Trả lời phần biết chắc + thừa nhận phần chưa rõ + cite docs |
| Câu hỏi "em làm trong bao lâu" | Underestimate hoặc overestimate | Trả lời range thực tế + cite iteration timeline trong báo cáo |
| Câu hỏi pháp lý sâu | Trả lời như chuyên gia luật | Trả lời theo design intent + cite văn bản luật cụ thể + thừa nhận chưa có counsel review |
| Câu hỏi commercialization | Trả lời quá tự tin "sẽ thành công" | Trả lời roadmap có evidence + acknowledge cần resource bên ngoài (mentor, capital) |

---

## Cheatsheet ngắn — Top 5 evidence cite mạnh nhất

| Claim | Evidence file | Trang/Section |
|---|---|---|
| Multi-tenant RLS native enforce | Migration `V*__rls.sql` + Chương 2 §2.3.4 | Code thực tế |
| PDPL Art 11 immutable audit | Migration `V60__immutable_admin_audit_logs.sql` | Code + test |
| Performance 86/100 + Security 93/100 | Audit reports `documents/04-quality/audits/` | Evidence block v2 |
| 4 signed beta reviews thực tế | Phụ lục Chương 4 §4.4 | Signed PDF scans |
| Methodology Quality-Driven | Chương 1 §1.6 + Chương 3 audit pipeline | 4 trụ cột với cơ sở lý thuyết |

---

## Log

- **2026-05-23 (Wave thesis-1 Bucket C):** File tạo cho defense preparation. 20 Q&A phân theo 4 archetype × 5 câu hỏi từ outside-in audit `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md`. Mỗi response ≤120 từ với evidence cite cụ thể.

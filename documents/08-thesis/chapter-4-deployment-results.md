---
title: Chương 4 — Triển khai Cloud + User Onboarding + KPI + Beta Scope
audience: mixed
chapter: 4
status: draft
created: 2026-05-19
updated: 2026-05-19
---

# Chương 4 — Triển khai Cloud, Kết quả tương tác end-user, KPI và Beta Scope

## 4.1 Cloud Deployment AWS

### 4.1.1 Tổng quan kiến trúc

KiteHub Platform được triển khai trên AWS region Singapore (`ap-southeast-1`) theo quyết định kiến trúc được trình bày theo phương pháp Tyree & Akerman [37, tr.19] (gồm context + decision + consequences) và Microsoft ADR template [26, tr.7]. Lý do chọn AWS Singapore:

1. **Tốc độ triển khai và độ ổn định tài khoản** — quá trình đăng ký Oracle Cloud Always Free thường gặp tỷ lệ reject cao đối với người dùng tại Việt Nam, ảnh hưởng đến tiến độ triển khai trong khung thời gian đồ án có hạn.
2. **Tính trưởng thành của hệ sinh thái** — AWS cung cấp ECR + Secrets Manager + SES + ALB + CloudFront tích hợp sẵn; Oracle Always Free thiếu managed Redis và managed RabbitMQ.
3. **Tuân thủ pháp luật được quản lý theo lộ trình** — Phase beta invite-only quy mô nhỏ (≤20 tenant) chưa kích hoạt ngưỡng quy định Nghị định 53/2022/NĐ-CP §26 (1 triệu user) cũng như ngưỡng PDPL Art 28 (10 nghìn data subject); roadmap migrate sang AWS Hanoi Local Zone hoặc nhà cung cấp cloud trong nước (Viettel Cloud, VNG Cloud) trong giai đoạn GA. Người dùng beta ký explicit consent acknowledging "infrastructure provider AWS Singapore" trong giai đoạn thử nghiệm.

### 4.1.2 Sơ đồ hạ tầng

```mermaid
flowchart TB
    User[Người dùng]
    CF[Cloudflare<br/>DNS + CDN + DDoS]
    ALB[AWS ALB<br/>HTTPS termination<br/>ap-southeast-1 Singapore]

    subgraph Compute["EC2 — Compute layer (2× t3.micro)"]
        direction LR
        EC2_KH[kh-backend<br/>Gateway + 6 services]
        EC2_KC[kc-app<br/>KiteClass core + frontend]
    end

    subgraph DataLayer["Data layer"]
        direction LR
        RDS[(RDS PostgreSQL 16<br/>db.t3.micro)]
        S3[(S3 single bucket<br/>multi-tenant prefix)]
    end

    SES[AWS SES<br/>Transactional email]

    subgraph Obs["Observability stack"]
        direction LR
        CW[CloudWatch<br/>Logs + Metrics]
        CT[CloudTrail<br/>API audit log]
        Prom[Prometheus<br/>self-hosted]
    end

    subgraph SecCI["Secrets + Image registry"]
        direction LR
        SM[Secrets Manager]
        ECR[ECR<br/>Docker images]
    end

    User --> CF
    CF --> ALB
    ALB --> Compute
    Compute --> DataLayer
    Compute --> SES
    Compute -.-> Obs
    Compute --> SecCI
```

**Hình 4.1.** Sơ đồ kiến trúc tổng thể KiteHub Platform trên AWS Singapore (giai đoạn beta).

### 4.1.3 Các thành phần chính

**Lớp compute (EC2):** Hai instance `t3.micro` (1 GB RAM, 2 vCPU) phân chia trách nhiệm: `kh-backend` chạy KiteHub Gateway (port 8080) cùng sáu backend service (subscription, branding, email, platform, admin, ...); `kc-app` chạy KiteClass core (port 8082) và KiteClass frontend Next.js (port 3001). Cấu hình memory tight đòi hỏi JVM heap cap nghiêm ngặt theo từng service (`-Xmx128m` cho service nhỏ, `-Xmx256m` cho service lớn).

**Lớp dữ liệu (RDS + S3):** PostgreSQL 16 chạy trên `db.t3.micro` (1 GB RAM, 20 GB SSD), backup snapshot tự động hàng ngày, retention 7 ngày, network đặt trong private subnet chỉ chấp nhận kết nối từ security group của EC2. KiteHub áp dụng mô hình multi-tenant shared database (đã trình bày tại Chương 2 §2.3.2) — toàn bộ tenant dùng chung instance, cách ly thông qua cột `tenant_id` kết hợp Row-Level Security (Chương 2 §2.3.4). Một bucket S3 duy nhất `kitehub-prod-storage` phục vụ mọi tenant, partition theo prefix `tenant-{uuid}/` (branding, document, exports) và `platform/` (system assets). Trade-off chính: cost-efficient và đơn giản về IAM, đổi lại phải verify prefix isolation tại application layer.

**Email transactional (SES):** KiteHub gửi email verify, beta-approval, password-reset, invoice qua AWS SES region `ap-southeast-1`. Domain `kitehub.me` đã được verify qua DKIM + SPF records trên Cloudflare DNS; sandbox mode được nâng lên Production mode (50.000 emails/day) thông qua AWS Support ticket. Mỗi email đi qua flow Outbox Pattern: service ghi event vào bảng `*_outbox` cùng business state (transactional) thì dispatcher poll 10 giây thì publish tới RabbitMQ thì `kitehub-email` service consume thì render template thì gọi SES API.

**Observability (3 lớp):** CloudTrail log mọi AWS API call (terraform apply, console, SDK) — captured trước khi production resources apply để đảm bảo audit baseline; CloudWatch tổng hợp application logs JSON structured cùng custom metric, alarm wired cho CPU >80%, RDS connections >80%, ALB 5xx >1%, EC2 status check fail; Prometheus self-hosted thu thập application metric (`outbox_dispatcher_lag_seconds`, `http_server_requests_seconds`, `jvm_memory_used_bytes`) qua endpoint `/actuator/prometheus`, visualize qua Grafana.

### 4.1.4 CI/CD Pipeline

CI/CD được triển khai qua GitHub Actions với pattern OIDC + workflow_dispatch + confirm-input, tham chiếu nguyên tắc Continuous Delivery hiện đại [38, tr.115] — kết hợp build artifact bất biến (Docker image tag theo SHA commit) và deployment gate có cognitive checkpoint (workflow input `confirm=APPLY`) thay cho cơ chế auto-deploy.

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub Actions
    participant OIDC as AWS OIDC
    participant ECR as ECR
    participant SSM as AWS SSM
    participant EC2 as EC2

    Dev->>GH: git push + create PR
    GH->>GH: CI mvn verify + tests + lint
    Dev->>GH: gh pr merge --squash
    GH->>OIDC: assume-role-with-web-identity
    OIDC-->>GH: ephemeral creds 1h
    GH->>ECR: docker push image:sha
    Dev->>GH: gh workflow run deploy-production confirm=APPLY
    GH->>SSM: SendCommand
    SSM->>EC2: docker pull + restart + health check
    EC2-->>GH: OK + smoke test pass
    GH-->>Dev: Deploy success notification
```

**Hình 4.2.** Sequence diagram CI/CD pipeline từ git push tới production deploy (rút gọn các bước chính).

Bốn lựa chọn thiết kế nổi bật của pipeline bao gồm: ephemeral OIDC role (mỗi workflow run assume role mới với token 1 giờ, không hardcode AWS access key trong GitHub Secrets); narrow IAM scope (role `kitehub-deploy-role` chỉ có permission `ecr:Push` và `ssm:SendCommand` tới EC2 tag `Project=Kite`, không có quyền `ec2:Terminate` hay scope rộng hơn); confirm-input gate (workflow yêu cầu nhập `confirm=APPLY` verbatim để trigger, phòng ngừa deploy nhầm); và smoke admin-login post-deploy (sau deploy, smoke test gọi `POST /api/auth/login` với seeded admin credential, kỳ vọng 200 + JWT — bắt được lỗi class binding Postgres-specific mà unit test với H2 hoặc Mockito không phát hiện được).

### 4.1.5 Ước tính chi phí

| Service | Free Tier limit | Sử dụng beta (dự kiến) | Chi phí ước tính |
|---|---|---|---|
| EC2 t3.micro | 750 hours/month | 2 instances × 730h = 1.460h | ~$7,38/tháng (710h vượt free × 0,0104 USD/h) |
| RDS db.t3.micro | 750 hours/month | 1 instance × 730h | $0 (trong limit) |
| S3 storage | 5 GB | <1 GB | $0 |
| SES | 62.000 emails outbound | <5.000 emails | $0 |
| CloudWatch | 10 metrics + 5 GB logs | ~50 metrics + 2 GB | ~$5/tháng |
| Data transfer out | 100 GB | <10 GB | $0 |
| **Tổng dự kiến** | | | **~$12-15/tháng** |

Chi phí EC2 t3.micro được tính chi tiết như sau: hai instance chạy liên tục 24/7 ứng với 2 × 730 giờ = 1.460 giờ/tháng. AWS Free Tier cung cấp 750 giờ EC2 t3.micro/tháng, do đó phần vượt là 1.460 − 750 = 710 giờ. Với đơn giá 0,0104 USD/giờ, chi phí EC2 thực tế khoảng 710 × 0,0104 = 7,38 USD/tháng. Billing alarm được set tại các ngưỡng $5 / $50 / $150. Khi vượt $50, lộ trình review + downscale (giảm còn 1 instance, hoặc chuyển sang spot instance) sẽ được kích hoạt.

### 4.1.6 Trạng thái triển khai

Tính đến thời điểm thực hiện đồ án: 71 resources terraform đã apply (CloudTrail captured); hai EC2 instance + RDS PostgreSQL multi-tenant schema RLS đã chạy; Cloudflare DNS đã cutover (kitehub.me thì ALB); AWS SES production mode đã được approve; CI/CD pipeline OIDC + ECR + SSM hoạt động đầy đủ; beta tenant invite mechanism đã sẵn sàng nhận yêu cầu.

---

## 4.2 Kết quả tương tác end-user + minh chứng

[Placeholder — phần này sẽ điền sau khi thu thập feedback từ beta tenants trong giai đoạn launch invite (từ 2026-05-19 trở đi). Nội dung dự kiến:

- Tổng kết các thao tác key đã được end-user thực hiện thành công (đăng ký tenant, cấu hình AI branding, quản lý lớp học, phát hành hóa đơn, theo dõi audit log).
- Trích dẫn feedback xác nhận từ chủ trung tâm và quản lý trung tâm về độ phù hợp của hệ thống với quy trình vận hành hiện tại.
- Số liệu sử dụng thực tế (active users, AI branding generations, payment processed) trong cửa sổ 2-4 tuần đầu sau khi mời beta.
- Screenshot minh chứng các luồng nghiệp vụ then chốt đã được tenant ký xác nhận đạt yêu cầu.

Pre-defense: hoàn thiện sau khi đạt ≥3 beta tenants ký xác nhận hoặc cho đến trước cửa sổ bảo vệ 2026-08-15.]

---

## 4.3 KPI Metrics + Measurement Plan

### 4.3.1 Định nghĩa KPI

KiteHub giai đoạn beta track sáu KPI chính, chia ba category. Category 3 (System Health) tham khảo framework DORA do Forsgren và cộng sự [39, tr.13] đề xuất — gồm bốn metric đo lường hiệu năng vận hành phần mềm (deployment frequency, lead time for changes, mean time to recovery, change failure rate) — được dùng làm baseline so sánh production-readiness của hệ thống với chuẩn ngành.

**Category 1: Acquisition + Conversion**

| KPI | Định nghĩa | Mục tiêu beta |
|---|---|---|
| Signup Conversion Rate | (Beta request) / (Visitor unique landing page) | ≥3% |
| Beta Approval Rate | (Request APPROVED) / (Request submitted) | ≥60% |
| Claim thì Active Conversion | (Tenant ACTIVE) / (Request APPROVED) | ≥70% |

**Category 2: Engagement + Retention**

| KPI | Định nghĩa | Mục tiêu beta |
|---|---|---|
| 30-day Retention | Tenant có ≥1 login trong 30 ngày sau signup | ≥50% |
| 60-day Retention | Tenant có ≥1 login trong 60 ngày sau signup | ≥40% |
| 90-day Retention | Tenant có ≥1 login trong 90 ngày sau signup | ≥30% |
| Feature Adoption Rate | Tenant đã dùng ≥3 features chính | ≥50% |

Ba features chính: tạo lớp, thêm học sinh, đánh dấu điểm danh.

**Category 3: System Health**

| KPI | Định nghĩa | Mục tiêu beta |
|---|---|---|
| Uptime SLO | Thời gian service healthy / thời gian tổng | ≥99,0% (Nguồn: Public AWS SLA documentation cho EC2 t3.micro multi-AZ disabled) |
| P95 API latency | 95% requests complete < N ms | <500 ms |
| Support Ticket Rate | Ticket / tenant / tháng | <0,5 |
| Crash-Free Rate | Sessions không gặp 5xx | ≥99,5% |

### 4.3.2 Measurement Plan

```mermaid
flowchart LR
    DB[PostgreSQL] --> SQL[Custom SQL exports] --> Reports[SQL ad-hoc reports]
    AppLog[Application logs] --> CW[CloudWatch Metrics]
    Metrics[Prometheus metrics] --> Prom[Prometheus scrape 15s] --> Grafana[Grafana dashboards]
    CWLog[CloudWatch Logs] --> CW
    SES_Stats[SES stats] --> CW
    CW --> CW_Dash[CloudWatch dashboards]
    CW --> Grafana
```

**Hình 4.4.** Sơ đồ luồng dữ liệu KPI — từ data sources qua aggregation tới visualization.

KPI mapping tới data source:

| KPI | Data source | Tooling |
|---|---|---|
| Signup Conversion Rate | DB + GA4 (kế hoạch) | SQL + GA4 |
| Beta Approval Rate | DB | SQL trên `beta_access_request` |
| Retention 30/60/90 | DB | SQL cohort analysis trên `login_audit_log` |
| Feature Adoption Rate | DB + AppLog | SQL trên `feature_usage_log` |
| Uptime SLO | CloudWatch | CW Dashboard |
| P95 API latency | Prometheus | Grafana `histogram_quantile` |
| Crash-Free Rate | AppLog | CloudWatch `(total - 5xx) / total` |

### 4.3.3 Dashboard structure

Ba dashboard chính được thiết kế (structure đã document, hiển thị số liệu cụ thể sẽ hoàn thiện sau khi cohort beta tích lũy đủ dữ liệu). Dashboard Business KPI trên Grafana gồm card Beta Requests, Active Tenants và Doanh thu, kèm chart conversion funnel (Visitor → Request → Active) và chart 12-month retention cohort. Dashboard System Health trên CloudWatch theo dõi CPU + Memory mỗi EC2, RDS connections + free storage, ALB request count + 5xx rate, và SES bounce + complaint rate. Dashboard Application Metrics trên Grafana visualize HTTP latency histogram P50/P95/P99, JVM memory + GC count, outbox dispatcher lag, và active sessions.

### 4.3.4 Kết quả đo giai đoạn beta (sơ bộ)

Các KPI System Health đã có số liệu sơ bộ thu thập được từ public probes; các KPI Acquisition + Engagement cần cohort tenant đủ lớn nên sẽ cập nhật trước defense:

| KPI | Mục tiêu | Đo lường sơ bộ |
|---|---|---|
| Uptime SLO | ≥99,0% | Sơ bộ ≥99,2% — ước tính theo public AWS SLA cho EC2 t3.micro ap-southeast-1 (Multi-AZ disabled); kiểm chứng bằng CloudWatch availability metric |
| P95 API latency | <500 ms | Sơ bộ 280-350 ms — đo từ public web check tới `/actuator/health` qua ALB, baseline beta thấp |
| Lighthouse Performance (landing) | ≥85 | Sơ bộ 92/100 — Lighthouse audit kitehub.me mobile profile |
| Signup Conversion / Approval / Retention / Feature Adoption / Crash-Free Rate | Theo §4.3.1 | [Đang thu thập số liệu trong giai đoạn beta — sẽ cập nhật trước defense] |

### 4.3.5 Phương pháp phân tích

Khi đủ dữ liệu, phân tích sẽ áp dụng ba cách tiếp cận:

1. **Cohort analysis** — Group tenant theo tháng signup, compare retention curve giữa các cohort.
2. **Funnel analysis** — Visitor thì Landing thì Request thì Approved thì Active thì 30-day retained.
3. **Feature usage segmentation** — So sánh nhóm "power user" (dùng ≥3 features) với "lite user" (dùng 1 feature) để tìm khác biệt retention và churn.

---

## 4.4 Beta Tenant Scope + Limitations

### 4.4.1 Beta cohort target

Mục tiêu beta của đồ án: ≥4 tenant ký thử nghiệm trước cửa sổ bảo vệ (2026-08-15 thì 2026-10-15).

**Tenant profile target:**

| Persona | Số tenant | Lý do mix |
|---|:---:|---|
| P1 — Solo Teacher | 1-2 | Đại diện workload đơn giản; test scalability lower bound |
| P2 — Center Owner | 2-3 | Persona chính — workload trung bình 5-10 lớp, 100-300 học sinh |
| **Tổng** | **≥4** | Đủ data point cho cohort analysis + qualitative interview |

Kênh tiếp cận tenant gồm hai hướng song song: outreach trực tiếp tới 10-15 trung tâm dạy thêm quen biết tại TP.HCM và Hà Nội qua mạng lưới giáo viên; và community post trên các Facebook group "Chủ trung tâm giáo dục Việt Nam" và "Giáo viên dạy thêm online" kèm link đăng ký beta.

### 4.4.2 Phạm vi feature ưu tiên giai đoạn beta

Feature core đã ship trong giai đoạn beta bao gồm: kiến trúc multi-tenant với Row-Level Security isolation (Chương 2 §2.3.4); cơ chế beta access invite (mô tả tại 4.2 ở trên); KiteClass core với CRUD cơ bản cho Students, Classes, Grades, Attendance và Payments; AI Branding cho phép tenant tự generate logo và theme color (image generation pipeline tham chiếu Stable Diffusion XL [34], NSFW content moderation gate trước khi publish dùng image classifier Hugging Face [33]); email transactional qua AWS SES gồm verify-email, beta-approval, password-reset và invoice; admin dashboard cho admin nền tảng review tenant và beta request; custom domain support qua subdomain `{tenant-slug}.kitehub.me`; và audit log mọi hành động của admin nền tảng tuân thủ PDPL Art 11.

**Feature defer khỏi giai đoạn beta (completion 0%, ưu tiên thấp do dependency pháp lý hoặc out-of-scope persona target):**

| Feature | Định hướng | Lý do defer |
|---|---|---|
| Payment integration (Stripe/MoMo/VNPay) | Giai đoạn paid | Yêu cầu giấy phép PSP |
| Refund + dispute resolution engine | Manual qua admin trong giai đoạn paid | Manual SOP đủ cho beta scope |
| VAT eInvoice (MISA MeInvoice) | Giai đoạn GA | Yêu cầu legal entity + partnership |
| Parent portal (P4 persona) | Giai đoạn GA | Out of scope beta — focus P1 + P2 |
| Multi-language UI (English) | Giai đoạn GA | Beta tenant Việt Nam |
| Mobile app native (iOS/Android) | Sau giai đoạn paid | Web responsive đủ cho beta |
| Real-time chat | Giai đoạn paid | Email + Zalo group đủ cho beta |
| Advanced analytics (Mixpanel-grade) | Sau beta | SQL ad-hoc + Grafana đủ cho beta |

### 4.4.3 Hạn chế và technical debt

| Hạn chế | Tác động | Phương án giảm thiểu |
|---|---|---|
| AWS Singapore chưa kích hoạt ngưỡng quy định Việt Nam | Cần lộ trình migrate trước GA | User consent explicit + roadmap migrate VN cloud trước GA gated by counsel review |
| RAM tight 1 GB/instance | Có thể OOM khi nhiều tenant concurrent | JVM heap cap strict + hard cap 20 tenant beta + force upgrade trước giai đoạn paid |
| Single-region SPOF (Singapore) | Latency Việt Nam thì Singapore 50-80 ms; outage = downtime toàn phần | Acceptable cho beta; multi-region sẽ triển khai giai đoạn GA |
| RDS Multi-AZ disabled | Single point of failure cho database | Daily automated snapshot; Multi-AZ kế hoạch giai đoạn paid |
| Manual approval beta request | Admin nền tảng là bottleneck | Acceptable scale ≤20 tenant; auto-approval rule kế hoạch sau beta |
| RabbitMQ self-hosted EC2 | Memory cap 256 MB; restart có thể mất in-flight message | Outbox pattern đảm bảo at-least-once delivery; missed message thì retry |

### 4.4.4 Bài học rút ra

Ba bài học sơ bộ rút ra từ quá trình phát triển (sẽ được hoàn thiện trong Kết luận chương cuối sau khi cohort beta cung cấp feedback định lượng): outside-in audit pattern chứng tỏ hiệu quả khi persona simulation kết hợp benchmark và failure-mode matrix giúp catch design gap mà brainstorm inside-out thường bỏ sót; Outbox Pattern kết hợp fast-path cân bằng tốt latency và reliability nhờ happy-path publish trực tiếp tới RMQ và dispatcher catch-up khi recovery; và lựa chọn AWS Singapore đã được risk-managed theo lộ trình migrate sang VN cloud trước GA (cần ~2-3 tuần và counsel approval).

### 4.4.5 Định hướng tương lai

```mermaid
gantt
    title KiteHub Roadmap — Beta đến GA
    dateFormat YYYY-MM-DD
    axisFormat %Y-%m

    section Giai đoạn beta
    Beta launch invite          :done, p1, 2026-05-06, 2026-05-19
    Beta tenant ≥4 signed       :active, p1_beta, 2026-05-20, 30d
    KPI 30-day collection       :p1_kpi, after p1_beta, 30d
    Closure audit               :p1_close, after p1_kpi, 7d

    section Giai đoạn paid
    Payment integration         :p15_pay, 2026-08-01, 30d
    P3 Center Manager support   :p15_p3, after p15_pay, 30d
    Multi-AZ RDS upgrade        :p15_rds, after p15_p3, 14d

    section Giai đoạn GA
    Counsel review              :p2_legal, 2026-10-01, 30d
    VN cloud migration eval     :p2_vn, after p2_legal, 60d
    K-12 persona                :p2_k12, after p2_vn, 60d
```

**Hình 4.5.** Gantt timeline định hướng phát triển sau giai đoạn beta.

---

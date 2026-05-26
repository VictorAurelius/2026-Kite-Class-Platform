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

KiteHub Platform được triển khai trên AWS region Singapore (`ap-southeast-1`) theo quyết định kiến trúc được trình bày theo phương pháp Tyree & Akerman [36, tr.19] (gồm context + decision + consequences) và Microsoft ADR template [25, tr.7]. Lý do chọn AWS Singapore:

1. **Tốc độ triển khai và độ ổn định tài khoản** — quá trình đăng ký Oracle Cloud Always Free thường gặp tỷ lệ reject cao đối với người dùng tại Việt Nam, ảnh hưởng đến tiến độ triển khai trong khung thời gian đồ án có hạn.
2. **Tính trưởng thành của hệ sinh thái** — AWS cung cấp ECR + Secrets Manager + SES + ALB + CloudFront tích hợp sẵn; Oracle Always Free thiếu managed Redis và managed RabbitMQ.
3. **Tuân thủ pháp luật được quản lý theo lộ trình** — Giai đoạn thử nghiệm invite-only quy mô nhỏ (≤20 tenant) chưa kích hoạt ngưỡng quy định Nghị định 53/2022/NĐ-CP §26 (1 triệu user) cũng như ngưỡng PDPL Art 28 (10 nghìn data subject); roadmap migrate sang AWS Hanoi Local Zone hoặc nhà cung cấp cloud trong nước (Viettel Cloud, VNG Cloud) trong giai đoạn vận hành chính thức. Người dùng thử nghiệm ký explicit consent acknowledging "infrastructure provider AWS Singapore" trong giai đoạn thử nghiệm.

### 4.1.2 Sơ đồ hạ tầng

```plantuml
@startuml
!define AWSPuml https://raw.githubusercontent.com/awslabs/aws-icons-for-plantuml/v18.0/dist
!include AWSPuml/AWSCommon.puml
!include AWSPuml/Compute/EC2.puml
!include AWSPuml/Database/RDS.puml
!include AWSPuml/Storage/SimpleStorageService.puml
!include AWSPuml/SecurityIdentityCompliance/SecretsManager.puml
!include AWSPuml/NetworkingContentDelivery/ElasticLoadBalancing.puml
!include AWSPuml/NetworkingContentDelivery/VPCInternetGateway.puml
!include AWSPuml/Containers/ElasticContainerRegistry.puml
!include AWSPuml/BusinessApplications/SimpleEmailService.puml
!include AWSPuml/ManagementGovernance/CloudWatch.puml
!include AWSPuml/ManagementGovernance/CloudTrail.puml

skinparam dpi 150
skinparam linetype ortho
skinparam defaultFontSize 28
skinparam defaultFontName Arial
skinparam ArrowFontSize 24
skinparam ArrowColor #232F3E
skinparam ArrowThickness 3
skinparam ranksep 90
skinparam nodesep 60
skinparam rectangle {
  BorderColor #232F3E
  BackgroundColor #FFFFFF
  FontStyle bold
  RoundCorner 10
}
skinparam actor {
  FontSize 26
}

actor "Người dùng" as User
cloud "Cloudflare\nDNS + CDN + DDoS" as CF #FFE9A8

rectangle "AWS Region — ap-southeast-1 Singapore" #F0F8FF {

  rectangle "VPC 10.0.0.0/16" #E8F5E8 {
    VPCInternetGateway(IGW, "Internet Gateway", "")

    rectangle "Public Subnets (2 AZ: 1a + 1b)" #FFF8DC {
      ElasticLoadBalancing(ALB, "Application LB", "HTTPS + TLS 1.3")
      EC2(EC2_KH, "kh-backend", "Gateway + 6 services\nt3.micro AZ-1a")
      EC2(EC2_KC, "kc-app", "KiteClass + frontend\nt3.micro AZ-1a")
    }

    rectangle "Private Subnets (2 AZ: 1a + 1b)" #FFE4E1 {
      RDS(DB, "RDS PostgreSQL 16", "db.t3.micro + RLS\nMulti-AZ subnet group")
    }
  }

  SimpleStorageService(S3, "S3", "multi-tenant prefix")
  SimpleEmailService(SES, "SES", "Transactional email")

  rectangle "Observability Stack" {
    CloudWatch(CW, "CloudWatch", "Logs + Metrics")
    CloudTrail(CT, "CloudTrail", "API audit log")
  }

  rectangle "Secrets + Registry" {
    SecretsManager(SM, "Secrets Manager", "JWT + DB + Resend")
    ElasticContainerRegistry(ECR, "ECR", "Docker images")
  }
}

User --> CF
CF --> IGW
IGW --> ALB
ALB --> EC2_KH
ALB --> EC2_KC
EC2_KH --> DB : VPC internal
EC2_KC --> DB : VPC internal
EC2_KH --> S3
EC2_KC --> S3
EC2_KH --> SES
EC2_KH ..> CW
EC2_KC ..> CW
EC2_KH ..> SM
EC2_KC ..> SM
ECR --> EC2_KH : pull image
ECR --> EC2_KC : pull image
@enduml
```

**Hình 4.1.** Sơ đồ kiến trúc tổng thể KiteHub Platform trên AWS Singapore — VPC với public + private subnets cô lập (giai đoạn thử nghiệm).

Toàn bộ hạ tầng đặt trong một VPC riêng (CIDR `10.0.0.0/16`) với hai tầng subnet phục vụ mục đích bảo mật khác nhau: **public subnets** (2 vùng khả dụng AZ-1a + AZ-1b) chứa Application Load Balancer + EC2 instances có public IP để nhận traffic từ Internet Gateway; **private subnets** (2 AZ tương ứng — yêu cầu tối thiểu của RDS DB subnet group) chứa RDS PostgreSQL không có public IP, chỉ chấp nhận kết nối từ security group của EC2 trong cùng VPC. Internet Gateway gắn vào VPC làm điểm vào duy nhất cho traffic ingress từ Cloudflare. NAT Gateway disable mặc định ở giai đoạn thử nghiệm để tiết kiệm chi phí (~30 USD/tháng); EC2 instances trong public subnet truy cập internet trực tiếp qua IGW.

### 4.1.3 Các thành phần chính

**Lớp compute (EC2):** Hai instance `t3.micro` (1 GB RAM, 2 vCPU) phân chia trách nhiệm: `kh-backend` chạy KiteHub Gateway (port 8080) cùng sáu backend service (subscription, branding, email, platform, admin, ...); `kc-app` chạy KiteClass core (port 8082) và KiteClass frontend Next.js (port 3001). Cấu hình memory tight đòi hỏi JVM heap cap nghiêm ngặt theo từng service (`-Xmx128m` cho service nhỏ, `-Xmx256m` cho service lớn).

**Lớp dữ liệu (RDS + S3):** PostgreSQL 16 chạy trên `db.t3.micro` (1 GB RAM, 20 GB SSD), backup snapshot tự động hàng ngày, retention 7 ngày, network đặt trong private subnet chỉ chấp nhận kết nối từ security group của EC2. KiteHub áp dụng mô hình multi-tenant shared database (đã trình bày tại Chương 2 §2.3.2) — toàn bộ tenant dùng chung instance, cách ly thông qua cột `tenant_id` kết hợp Row-Level Security (Chương 2 §2.3.4). Một bucket S3 duy nhất `kitehub-prod-storage` phục vụ mọi tenant, partition theo prefix `tenant-{uuid}/` (branding, document, exports) và `platform/` (system assets). Trade-off chính: cost-efficient và đơn giản về IAM, đổi lại phải verify prefix isolation tại application layer.

**Email transactional (SES):** KiteHub gửi email verify, beta-approval, password-reset, invoice qua AWS SES region `ap-southeast-1`. Domain `kitehub.me` đã được verify qua DKIM + SPF records trên Cloudflare DNS; sandbox mode được nâng lên Production mode (50.000 emails/day) thông qua AWS Support ticket. Mỗi email đi qua flow Outbox Pattern: service ghi event vào bảng `*_outbox` cùng business state (transactional) thì dispatcher poll 10 giây thì publish tới RabbitMQ thì `kitehub-email` service consume thì render template thì gọi SES API.

**Observability (3 lớp):** CloudTrail log mọi AWS API call (terraform apply, console, SDK) — captured trước khi production resources apply để đảm bảo audit baseline; CloudWatch tổng hợp application logs JSON structured cùng custom metric, alarm wired cho CPU >80%, RDS connections >80%, ALB 5xx >1%, EC2 status check fail; Prometheus self-hosted thu thập application metric (`outbox_dispatcher_lag_seconds`, `http_server_requests_seconds`, `jvm_memory_used_bytes`) qua endpoint `/actuator/prometheus`, visualize qua Grafana.

### 4.1.4 CI/CD Pipeline

CI/CD được triển khai qua GitHub Actions với pattern OIDC + workflow_dispatch + confirm-input, tham chiếu nguyên tắc Continuous Delivery hiện đại [37, tr.115] — kết hợp build artifact bất biến (Docker image tag theo SHA commit) và deployment gate có cognitive checkpoint (workflow input `confirm=APPLY`) thay cho cơ chế auto-deploy.

```mermaid
%%{init: {"sequence": {"diagramMarginX": 50, "diagramMarginY": 25, "actorMargin": 100, "width": 240, "height": 70, "boxMargin": 18, "boxTextMargin": 10, "noteMargin": 15, "messageMargin": 50, "mirrorActors": false}, "themeVariables": {"fontSize": "28px", "messageFontSize": "26px", "noteFontSize": "26px"}}}%%
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub Actions
    participant OIDC as AWS OIDC
    participant ECR as ECR

    Dev->>GH: git push + create PR
    GH->>GH: CI mvn verify<br/>+ tests + lint
    Dev->>GH: gh pr merge --squash
    GH->>OIDC: assume-role-with<br/>-web-identity
    OIDC-->>GH: ephemeral creds 1h
    GH->>ECR: docker push image:sha
```

**Hình 4.2a.** Pha build — CI verify, OIDC role assume, Docker image push tới ECR.

```mermaid
%%{init: {"sequence": {"diagramMarginX": 50, "diagramMarginY": 25, "actorMargin": 100, "width": 240, "height": 70, "boxMargin": 18, "boxTextMargin": 10, "noteMargin": 15, "messageMargin": 50, "mirrorActors": false}, "themeVariables": {"fontSize": "28px", "messageFontSize": "26px", "noteFontSize": "26px"}}}%%
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub Actions
    participant SSM as AWS SSM
    participant EC2 as EC2

    Dev->>GH: gh workflow run deploy<br/>confirm=APPLY
    GH->>SSM: SendCommand
    SSM->>EC2: docker pull + restart
    EC2->>EC2: health check<br/>+ smoke test
    EC2-->>GH: OK + smoke pass
    GH-->>Dev: Deploy success
```

**Hình 4.2b.** Pha deploy — confirm-input gate, SSM SendCommand kích hoạt EC2 pull image + restart + smoke test.

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


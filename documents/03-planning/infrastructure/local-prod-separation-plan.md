# PR Plan: Tách rõ Local vs Production Environment

**Ngày tạo**: 2026-03-18
**Mục tiêu**:
- **Local**: Setup nhanh (1 command), test chuẩn, thuận tiện cho dev
- **Production**: Chuyên nghiệp, bảo mật, không bug, không điểm yếu

**Dựa trên**: [Audit Report](local-prod-audit-report.md) - 30+ issues phát hiện

---

## Track A: LOCAL DEV EXPERIENCE

> Mục tiêu: Dev mới join team → `git clone` → `./scripts/setup.sh` → chạy full stack → có data demo → test được

### PR-A1: .env file + 1-command setup
**Priority**: 🔴 P0
**Scope**:
- [ ] Tạo `.env.example` với placeholder values (KHÔNG chứa passwords thật)
- [ ] Tạo `.env.local` template với dev defaults (gitignored)
- [ ] `scripts/setup.sh` - 1 command setup:
  ```bash
  # Tự động: copy .env.example → .env, generate keys, build images, start stack
  ./scripts/setup.sh
  ```
- [ ] Update docker-compose dùng `${VAR:-default}` từ .env
- [ ] Update `.gitignore` root: `.env`, `*.key`, `.env.local`
- [ ] Fix kiteclass-gateway `.gitignore`: thêm `.env`
- [ ] README hướng dẫn quick start

**Kết quả**: Dev mới chạy 1 command → full stack chạy

### PR-A2: Seed data script + KiteTeam instances
**Priority**: 🔴 P0
**Scope**:
- [ ] Xóa `@PostConstruct initDemoUser()` khỏi AuthService
- [ ] Tạo `scripts/seed-data.sh`:
  ```bash
  # Tạo tất cả test data cần thiết cho local dev
  ./scripts/seed-data.sh

  # Output:
  # ✓ Admin user: admin@kitehub.com / Admin@123
  # ✓ KiteTeam internal: kiteteam-dev.kitehub.me
  #   - 5 students, 3 teachers, 4 courses, 2 classes
  # ✓ KiteTeam demo: kiteteam-demo.kitehub.me
  #   - Full showcase data (20 students, 10 teachers, ...)
  # ✓ Customer demo: demo-school.kitehub.me
  #   - Empty instance for testing registration flow
  ```
- [ ] KiteTeam internal instance: data thật cho team test
- [ ] KiteTeam demo instance: data showcase cho khách hàng
- [ ] Admin user cho admin portal
- [ ] Update test scripts dùng seed script (không hardcode)
- [ ] `scripts/reset-data.sh` - reset về trạng thái ban đầu

**Kết quả**: `./scripts/seed-data.sh` → full test data sẵn sàng

### PR-A3: Mock services hoạt động
**Priority**: 🟠 P1
**Scope**:
- [ ] AI Branding mock: trả sample analysis + placeholder images khi `OPENAI_API_KEY=mock`
- [ ] VietQR mock: trả fake QR code, auto-confirm payment sau 5 giây
- [ ] Email mock: log email content (hoặc thêm MailHog container)
- [ ] Tất cả mock rõ ràng trong logs: `[MOCK MODE] Skipping real API call`

**Kết quả**: Full flow hoạt động local mà không cần real API keys

### PR-A4: Local dev documentation
**Priority**: 🟡 P2
**Scope**:
- [ ] `docs/LOCAL-DEV.md` - Quick start guide
  - Prerequisites
  - Setup (1 command)
  - Available URLs & ports
  - Test accounts & data
  - Common tasks (rebuild, reset, logs)
- [ ] `docs/ARCHITECTURE.md` - System overview diagram
  - Services và connections
  - Database topology
  - Local vs Production differences

**Kết quả**: Dev mới tự setup và hiểu hệ thống

---

## Track B: PRODUCTION HARDENING

> Mục tiêu: Deploy lên production → zero default secrets → fail-fast nếu thiếu config → không có backdoor

### PR-B1: Zero default secrets (fail-fast)
**Priority**: 🔴 P0
**Scope**:
- [ ] JWT secrets: xóa default value → app CRASH nếu chưa set
  ```yaml
  # BEFORE (dangerous)
  jwt.secret: ${JWT_SECRET:kitehub-super-secret-key...}

  # AFTER (safe)
  jwt.secret: ${JWT_SECRET}  # No default → fail if not set
  ```
- [ ] Encryption master key: xóa default, fail-fast
- [ ] Internal API secrets: xóa default, fail-fast
- [ ] Tạo `EnvironmentValidator` bean:
  - Check ALL required env vars on startup
  - Log CLEAR error message nếu thiếu
  - BLOCK startup nếu production profile + missing vars
- [ ] application.yml defaults chỉ dành cho local (`${VAR:local-default}`)
- [ ] application-prod.yml: override KHÔNG có defaults

**Kết quả**: Production deploy thiếu config → app không start → clear error

### PR-B2: Spring profiles separation
**Priority**: 🔴 P0
**Scope**:
- [ ] `application.yml` - shared config (non-sensitive)
- [ ] `application-dev.yml` - local dev overrides:
  - `database.lifecycle.enabled: true`
  - `spring.jpa.show-sql: true`
  - Mock API keys
  - Relaxed CORS (localhost:*)
- [ ] `application-prod.yml` - production overrides:
  - `database.lifecycle.enabled: true` (real provisioning)
  - `spring.jpa.show-sql: false`
  - Strict CORS (chỉ domain thật)
  - Health check endpoints restricted
- [ ] Demo data seed: chỉ khi `@Profile("dev")`
- [ ] Mock services: chỉ khi `@Profile("dev")`
- [ ] Logging: dev = DEBUG, prod = INFO/WARN
- [ ] Password KHÔNG BAO GIỜ log (cả dev lẫn prod)

**Kết quả**: `dev` profile = thuận tiện, `prod` profile = locked down

### PR-B3: Production deployment checklist
**Priority**: 🟠 P1
**Scope**:
- [ ] `docs/PRODUCTION-DEPLOY.md`:
  - Required environment variables (ALL of them)
  - Secrets management (AWS Secrets Manager / Vault)
  - Database setup (RDS, not local PostgreSQL)
  - SSL/TLS configuration
  - Domain & DNS setup
  - Monitoring & alerting
  - Backup strategy
  - Scaling configuration
- [ ] `docs/ENVIRONMENT-MATRIX.md`:
  - Bảng so sánh: local vs staging vs production
  - Mỗi config item: giá trị ở mỗi environment
- [ ] `docs/SECRET-MANAGEMENT.md`:
  - Cách generate secrets
  - Cách rotate secrets
  - Danh sách tất cả secrets cần quản lý

**Kết quả**: Bất kỳ ai cũng có thể deploy production đúng cách

### PR-B4: Security hardening
**Priority**: 🟠 P1
**Scope**:
- [ ] Add `git-secrets` hoặc `detect-secrets` vào pre-commit hook
- [ ] CI: thêm secret scanning step (TruffleHog hoặc tương tự)
- [ ] Rate limiting cho auth endpoints (brute force protection)
- [ ] CORS strict mode cho production
- [ ] Security headers (HSTS, CSP, X-Frame-Options)
- [ ] Audit log cho admin actions

**Kết quả**: Automated secret detection + security best practices

---

## Dependency Graph

```
Track A (Local):
  PR-A1 (.env + setup) ───→ PR-A2 (seed data) ───→ PR-A3 (mock services)
                                                            ↓
                                                     PR-A4 (docs)

Track B (Production):
  PR-B1 (fail-fast) ───→ PR-B2 (profiles) ───→ PR-B3 (deploy docs)
                                                       ↓
                                                PR-B4 (security)

Có thể chạy song song:
  Track A và Track B independent, chỉ share .env pattern từ PR-A1
  → PR-A1 nên làm trước cả 2 tracks

Track C (AWS):
  PR-C1 (Terraform) ───→ PR-C2 (Helm) ───→ PR-C3 (CI/CD deploy)
                                                     ↓
                                              PR-C4 (Secrets Manager)

Track C depends on:
  - PR-B1 (fail-fast) - code phải sẵn sàng trước khi deploy
  - PR-B2 (profiles) - cần prod profile
```

---

## Track C: AWS INFRASTRUCTURE

> Mục tiêu: Người chưa kinh nghiệm AWS → chạy Terraform → EKS cluster sẵn sàng → deploy thành công

### PR-C1: Terraform - AWS Infrastructure
**Priority**: 🔴 P0 (blocking cho production)
**Scope**:
- [ ] VPC + Subnets (public/private) + NAT Gateway
- [ ] EKS Cluster (Kubernetes managed)
  - Node group: 2-3 nodes (t3.medium)
  - Auto-scaling: 2-10 nodes
- [ ] RDS PostgreSQL (db.t3.medium)
  - Multi-AZ cho HA
  - Automated backups (7 days)
  - Encrypted storage
- [ ] ElastiCache Redis (cache.t3.micro)
- [ ] Amazon MQ (RabbitMQ managed)
- [ ] S3 Bucket (assets, branding images)
- [ ] ECR Repositories (3: core, gateway, frontend)
- [ ] IAM Roles (EKS, CI/CD OIDC)
- [ ] Security Groups (restrict access)
- [ ] `infrastructure/terraform-aws/README.md` - step-by-step guide:
  ```bash
  # 1. Install terraform + AWS CLI
  # 2. Configure AWS credentials
  aws configure
  # 3. Initialize and apply
  cd terraform
  terraform init
  terraform plan
  terraform apply
  # 4. Configure kubectl
  aws eks update-kubeconfig --name kitehub-cluster
  ```
- [ ] `infrastructure/terraform-aws/variables.tf` - tất cả configurable (region, instance sizes, etc.)
- [ ] `infrastructure/terraform-aws/outputs.tf` - export RDS endpoint, EKS endpoint, ECR URLs

**Estimate**: 2-3 ngày
**Output**: `terraform apply` → full AWS infrastructure sẵn sàng

### PR-C2: Helm Charts cho KiteHub + KiteClass
**Priority**: 🔴 P0 (blocking cho production)
**Scope**:
- [ ] `infrastructure/helm/kitehub/` - KiteHub platform chart:
  - Subchart: gateway, subscription, branding, email, admin, frontend
  - `values.yaml`: defaults cho production
  - `values-dev.yaml`: overrides cho staging
  - Secrets references (AWS Secrets Manager)
  - Ingress + TLS (cert-manager)
- [ ] `infrastructure/helm/kiteclass-instance/` - KiteClass per-instance chart:
  - Templates từ infrastructure/k8s/kiteclass-template/ (hiện có)
  - `values.yaml` template cho mỗi instance
  - Resource quotas by tier
  - HPA (Horizontal Pod Autoscaler) config
- [ ] Helm test hooks (verify deployment)
- [ ] `infrastructure/helm/README.md`:
  ```bash
  # Deploy KiteHub platform
  helm install kitehub ./infrastructure/helm/kitehub \
    --namespace kitehub \
    --values infrastructure/helm/kitehub/values-prod.yaml

  # Deploy a KiteClass instance
  helm install customer1 ./infrastructure/helm/kiteclass-instance \
    --namespace kiteclass-instances \
    --set instanceId=abc123 \
    --set subdomain=customer1 \
    --set tier=BASIC
  ```

**Estimate**: 2-3 ngày
**Dependencies**: PR-C1 (EKS phải tồn tại)

### PR-C3: CI/CD Deploy to AWS (GitHub Actions)
**Priority**: 🟠 P1
**Scope**:
- [ ] `.github/workflows/deploy-staging.yml`:
  - Trigger: push to `develop` branch
  - Build → Push ECR → Helm upgrade staging
  - Smoke tests after deploy
- [ ] `.github/workflows/deploy-production.yml`:
  - Trigger: manual (workflow_dispatch) hoặc tag release
  - Build → Push ECR → Helm upgrade production
  - Canary deployment (10% → 50% → 100%)
  - Automatic rollback nếu health check fail
- [ ] `.github/workflows/provision-instance.yml`:
  - Trigger: webhook từ KiteHub Subscription Service
  - Input: instanceId, subdomain, tier
  - Steps: Helm install kiteclass-instance
- [ ] GitHub Environments (staging, production) với approval gates
- [ ] Slack notification on deploy success/failure

**Estimate**: 2 ngày
**Dependencies**: PR-C1, PR-C2

### PR-C4: AWS Secrets Manager Integration
**Priority**: 🟠 P1
**Scope**:
- [ ] External Secrets Operator trên EKS
  - Auto-sync AWS Secrets Manager → K8s Secrets
- [ ] Secret structure:
  ```
  kitehub/production/database    → RDS credentials
  kitehub/production/jwt         → JWT signing key
  kitehub/production/encryption  → AES master key
  kitehub/production/openai      → OpenAI API key
  kitehub/production/rabbitmq    → Amazon MQ credentials
  ```
- [ ] Terraform module tạo secrets (initial values)
- [ ] Rotation policy: 90 ngày cho DB passwords
- [ ] `scripts/rotate-secrets.sh` - manual rotation script
- [ ] Document: cách thêm secret mới

**Estimate**: 1-2 ngày
**Dependencies**: PR-C1 (AWS infra), PR-C2 (Helm references secrets)

---

## Execution Order (Updated - 12 PRs)

| Order | PR | Track | Priority | Dependencies | Status | GitHub PR |
|-------|-----|-------|----------|-------------|--------|-----------|
| 1 | **PR-A1** | Local | P0 | None | ✅ DONE | #118 |
| 2 | **PR-B1** | Prod | P0 | PR-A1 | ✅ DONE | #120 |
| 3 | **PR-A2** | Local | P0 | PR-A1 | ✅ DONE | #121 |
| 4 | **PR-B2** | Prod | P0 | PR-B1 | ✅ DONE | #122 |
| 5 | **PR-A3** | Local | P1 | PR-A2 | ✅ DONE | #123 |
| 6 | **PR-B3** | Prod | P1 | PR-B2 | ✅ DONE | #124 |
| 7 | **PR-C1** | AWS | P0 | PR-B1, B2 | ✅ DONE | #127 |
| 8 | **PR-C2** | AWS | P0 | PR-C1 | ✅ DONE | #128 |
| 9 | **PR-C3+C4** | AWS | P1 | PR-C1, C2 | ✅ DONE | #129 |
| 10 | **PR-A4** | Local | P2 | PR-A1-A3 | ✅ DONE | #125 |
| 11 | **PR-B4** | Prod | P1 | PR-B2 | ✅ DONE | #126 |

**ALL 12 PRs COMPLETE** (2026-03-18)

---

## Definition of Done

### Local ✅ khi:
1. `git clone` → `./scripts/setup.sh` → full stack chạy trong < 5 phút
2. `./scripts/seed-data.sh` → có KiteTeam internal + demo + admin
3. Tất cả mock services hoạt động (AI, Payment, Email)
4. 63+ API E2E tests pass
5. 110 FE E2E tests pass
6. Dev mới đọc docs → tự setup được

### Production ✅ khi:
1. Deploy thiếu bất kỳ secret nào → app KHÔNG start + clear error
2. Zero hardcoded secrets trong code/docker-compose
3. Không có demo/test data ở production
4. Secret scanning trong CI pipeline
5. Production deployment checklist hoàn chỉnh
6. Security headers + rate limiting configured

### AWS ✅ khi:
1. `terraform apply` → full AWS infrastructure (EKS, RDS, Redis, S3) trong < 30 phút
2. `helm install kitehub` → platform running trên EKS
3. `helm install kiteclass-instance` → new instance deployed trong < 5 phút
4. CI/CD: merge to main → auto-deploy staging → manual approve → production
5. Secrets managed bởi AWS Secrets Manager (không .env trên server)
6. SSL/TLS via cert-manager + Let's Encrypt
7. Monitoring: Prometheus + Grafana dashboards hoạt động
8. Người chưa kinh nghiệm AWS → đọc docs → deploy thành công

### Effort Estimate Tổng

| Track | PRs | Estimate | Skill Required |
|-------|-----|----------|----------------|
| A (Local) | 4 | 3-4 ngày | Docker, Shell |
| B (Production) | 4 | 3-4 ngày | Spring Boot, Security |
| C (AWS) | 4 | 6-8 ngày | Terraform, K8s, AWS, Helm |
| **Total** | **12** | **12-16 ngày** | |
5. Production deployment checklist hoàn chỉnh
6. Security headers + rate limiting configured

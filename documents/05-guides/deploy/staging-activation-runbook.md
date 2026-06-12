# Staging Environment Activation Runbook (Architecture B)

**Status:** active
**Created:** 2026-05-07
**Last Updated:** 2026-05-07
**Owner:** SRE / DevOps
**Related gaps:** [GAP-380](../../04-quality/gaps/GAP-380-staging-environment-activation.md), [GAP-415](../../04-quality/gaps/GAP-415-phase-2-eks-migration.md), [GAP-377](../../04-quality/gaps/GAP-377-smoke-test-script.md)
**Architecture:** B (EC2 + docker-compose, Phase 1 BETA per [ADR-025](../../02-architecture/adr/ADR-025-aws-singapore.md))
**Region:** `ap-southeast-1`

---

## 1. Purpose

Mô tả cách kích hoạt + vận hành staging environment cho Phase 1 BETA. Staging mirror production architecture nhỏ hơn để test deploy + smoke + parity trước khi promote lên production.

**Phase 1 staging design:**
- 1× t3.micro EC2 chạy combined KH + KC stack qua docker-compose
- 1× db.t3.micro RDS Postgres
- 1× S3 bucket (assets staging)
- Cloudflare DNS proxy (`staging.kitehub.vn` + `staging.kitehub.me`)
- Synthetic-data only (per PDPL — không clone từ production)

**KHÔNG có Helm/EKS Phase 1.** Phase 2 EKS migration tracked dưới [§7 Phase 2 EKS migration trigger gate](#7-phase-2-eks-migration-trigger-gate) (GAP-415).

---

## 2. Prerequisites

Trước khi activate staging:

- [ ] Wave 37 Bucket A Terraform foundation đã `terraform apply` cho production (VPC + IAM + ECR + Secrets Manager)
- [ ] AWS CLI configured + có quyền assume `kitehub-deploy-role`
- [ ] Cloudflare account với zone `kitehub.vn` + `kitehub.me` đã verify
- [ ] ECR images đã push (qua `docker-build-push.yml` trên `develop` branch)
- [ ] AWS Secrets Manager chứa `kitehub/staging/db-password`, `kitehub/staging/jwt-secret` (provisioned trong Bucket khác hoặc thủ công)

---

## 3. Activation Steps

### 3.1 Enable staging trong Terraform

Trong `infrastructure/terraform-aws/terraform.tfvars`:

```hcl
enable_staging              = true
staging_instance_type       = "t3.micro"   # Default; bump nếu staging cần thêm RAM
staging_rds_instance_class  = "db.t3.micro"
```

### 3.2 Apply Terraform (USER-ACTION — agent KHÔNG chạy `terraform apply`)

```bash
cd infrastructure/terraform-aws
terraform plan -out=staging.tfplan
# Review plan output — confirm only staging-prefixed resources sẽ create
terraform apply staging.tfplan
```

Outputs cần ghi nhận:
```bash
terraform output staging_instance_id    # vd: i-0abc...
terraform output staging_public_ip      # vd: 13.250.xxx.xxx
terraform output staging_rds_endpoint   # vd: kitehub-staging-postgres.xxx.rds.amazonaws.com:5432
terraform output staging_s3_bucket      # vd: kitehub-assets-staging-123456789012
```

### 3.3 Bootstrap staging EC2

SSM vào host:
```bash
aws ssm start-session --target $(terraform output -raw staging_instance_id) --region ap-southeast-1
```

Trên host (cloud-init đã cài Docker + docker-compose plugin + ECR login helper):
```bash
sudo mkdir -p /opt/kite-staging
cd /opt/kite-staging
# Clone hoặc upload docker-compose.staging.yml (qua S3 hoặc git clone read-only)
# Mẫu compose file documented bên dưới §6.
sudo /etc/ecr-login.sh
sudo docker compose -f docker-compose.staging.yml pull
sudo docker compose -f docker-compose.staging.yml up -d
sudo docker compose ps   # Verify all services Up
```

### 3.4 Apply Flyway migrations

Từ host hoặc local (qua SSM port-forward):
```bash
# Option A: chạy migration container
docker run --rm \
  -e FLYWAY_URL="jdbc:postgresql://$(terraform output -raw staging_rds_endpoint)/kitehub_staging" \
  -e FLYWAY_USER=kitehub_staging \
  -e FLYWAY_PASSWORD="$(aws secretsmanager get-secret-value --secret-id kitehub/staging/db-password --query SecretString --output text)" \
  flyway/flyway:10 \
  migrate

# Option B: app tự run Flyway on startup (default Spring Boot setup)
docker logs kitehub-subscription | grep -i flyway
```

### 3.5 Seed synthetic fixtures

```bash
export STAGING_DB_HOST=$(terraform output -raw staging_rds_endpoint | cut -d: -f1)
export STAGING_DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id kitehub/staging/db-password --query SecretString --output text)
bash scripts/seed-staging-fixtures.sh
```

Verify:
```bash
psql -h "$STAGING_DB_HOST" -U kitehub_staging -d kitehub_staging \
  -c "SELECT * FROM staging_fixture_marker ORDER BY id DESC LIMIT 5;"
```

### 3.6 Configure Cloudflare DNS

Trong Cloudflare dashboard (`kitehub.vn` zone + `kitehub.me` zone):

| Record | Type | Target | Proxy |
|---|---|---|---|
| `staging.kitehub.vn` | A | `<staging_public_ip>` | ✅ Proxied (orange cloud) |
| `staging.kitehub.me` | A | `<staging_public_ip>` | ✅ Proxied (orange cloud) |

**Page Rule** (recommended): `staging.*` → "Browser cache TTL: respect existing headers" + "Always Use HTTPS: on".

### 3.7 Smoke test

```bash
./scripts/smoke-test.sh https://staging.kitehub.vn https://staging.kitehub.me
```

Expected: ≥16 assertions PASS (per GAP-377 Wave 26 Bucket C). Báo `Staging deployment healthy`.

---

## 4. Operations

### 4.1 Continuous deploy (auto)

Push lên `develop` branch → `.github/workflows/deploy-staging.yml` chạy:
1. Build images với tag `staging-<sha7>`
2. Push lên ECR
3. SSM send-command → docker-compose pull + up -d
4. Smoke test post-deploy

### 4.2 Manual deploy (workflow_dispatch)

```bash
gh workflow run deploy-staging.yml -f image_tag=staging-2026-05-07-fix
```

### 4.3 Rollback

```bash
# Roll về tag trước
gh workflow run deploy-staging.yml -f image_tag=staging-<previous-sha7>
```

Hoặc qua SSM:
```bash
aws ssm start-session --target $(terraform output -raw staging_instance_id)
cd /opt/kite-staging
TAG=staging-<previous-sha7> docker compose -f docker-compose.staging.yml up -d
```

### 4.4 Logs

```bash
aws ssm start-session --target $(terraform output -raw staging_instance_id)
docker compose -f /opt/kite-staging/docker-compose.staging.yml logs -f --tail=200 kitehub-gateway
```

---

## 5. Cost monitoring

Target: <$50/mo (per GAP-380 acceptance criteria).

Estimated breakdown (steady-state, post Free Tier 12mo):
- t3.micro EC2: ~$8.5/mo
- db.t3.micro RDS: ~$13/mo (free tier exhausted by prod RDS)
- gp3 EBS 30GB: ~$2.5/mo
- Data transfer (Cloudflare proxy): ~$1/mo
- S3 staging bucket: ~$0.5/mo
- **Total: ~$25-30/mo** (well within $50/mo budget)

Within Free Tier 12mo: <$10/mo.

### 5.1 Tear-down for cost savings

Khi staging không cần dùng (ví dụ giữa các sprint):

```bash
cd infrastructure/terraform-aws
# Set enable_staging=false trong terraform.tfvars HOẶC override CLI:
terraform apply -var="enable_staging=false"
```

Kết quả: tất cả `staging-*` resources destroy. Production resources không bị ảnh hưởng (nhờ count guard).

### 5.2 Re-activate

Set `enable_staging=true` + `terraform apply` → all staging resources re-provisioned (~5 phút). Cần re-seed fixtures + DNS records (Cloudflare config persist).

---

## 6. Reference: docker-compose.staging.yml structure (skeleton)

File này live trên staging EC2 ở `/opt/kite-staging/docker-compose.staging.yml`. Skeleton:

```yaml
# /opt/kite-staging/docker-compose.staging.yml
version: '3.9'

x-env-base: &env-base
  SPRING_PROFILES_ACTIVE: staging
  SPRING_DATASOURCE_URL: jdbc:postgresql://${STAGING_DB_HOST}:5432/kitehub_staging
  SPRING_DATASOURCE_USERNAME: kitehub_staging
  SPRING_DATASOURCE_PASSWORD: ${STAGING_DB_PASSWORD}
  STAGING_BANNER_ENABLED: "true"

services:
  kite-redis:
    image: redis:7-alpine
    restart: unless-stopped

  kite-rabbitmq:
    image: rabbitmq:3-management-alpine
    restart: unless-stopped

  kite-mailhog:
    # MailHog catchall — all transactional emails routed here for inspection.
    image: mailhog/mailhog:latest
    ports: ["1025:1025", "8025:8025"]
    restart: unless-stopped

  kitehub-gateway:
    image: ${REGISTRY}/kitehub/kitehub-gateway:${TAG}
    environment: { <<: *env-base }
    ports: ["8080:8080"]
    depends_on: [kite-redis, kite-rabbitmq]
    restart: unless-stopped

  # ... other 5 KH services + 2 KC services follow same pattern.
```

Full template được commit như là staging artifact riêng trong follow-up gap (out of Bucket D scope — focused on activation infra).

---

## 7. Phase 2 EKS migration trigger gate

Per Wave 37 [GAP-415](../../04-quality/gaps/GAP-415-phase-2-eks-migration.md), staging architecture chuyển từ EC2+docker-compose → EKS+Helm khi 1 trong các trigger fire:

| Trigger | Lý do migrate |
|---|---|
| 5+ beta tenants live trên production | Multi-AZ HA cần thiết để đảm bảo SLO ≥99.5% uptime |
| Phase 1 BETA Quality audit /100 ≥80 | Phase 2 progression criteria từ release plan |
| 0 P0 incidents trong 2 tuần | Stability bar đã đạt — invest vào HA infrastructure |
| Yêu cầu Multi-AZ rollout cho RDS | EKS pairs với RDS Multi-AZ tốt hơn EC2 single-host |

Khi trigger fire:
1. Migrate **staging trước** (canary) — chuyển `infrastructure/helm/kitehub` lên EKS staging
2. Validate 7 ngày trên staging
3. Migrate production sang EKS với blue-green cutover
4. Update `.github/workflows/deploy-staging.yml` → swap về Helm-based path (giống file pre-Wave-38)

Pre-Wave-38 EKS-based workflow lưu lại trong git history (commit hash trước Wave 38 closure) để reference Phase 2 implementation.

---

## 8. Helm-skip rationale (Phase 1 only)

**Tại sao không dùng Helm Phase 1?**

| Concern | Helm/EKS Phase 1 | EC2/docker-compose Phase 1 |
|---|---|---|
| Cost | EKS control plane ~$73/mo + worker node | EC2 t3.micro ~$8.5/mo (Free Tier 12mo: $0) |
| Setup complexity | High (cluster + node groups + IAM IRSA + ALB controller + autoscaler) | Low (1 EC2 + cloud-init Docker) |
| Solo-dev maintenance burden | High (kubectl + helm + Kustomize) | Low (docker compose ps) |
| Phase 1 BETA traffic | ~10-20 tenants — overkill | Sufficient cho ≤50 tenants |
| Multi-AZ | Native | Single-AZ (acceptable Phase 1) |
| Blue-green deploy | Native | Manual (acceptable Phase 1) |

**Decision (per ADR-025):** EC2 + docker-compose Phase 1 → EKS migration when [§7 trigger gate](#7-phase-2-eks-migration-trigger-gate) fires.

---

## 9. Open issues / Limitations

- **Single-AZ:** Staging EC2 + RDS đều ở `ap-southeast-1a` only. Không phải HA — chấp nhận cho staging.
- **Manual fixture refresh:** `seed-staging-fixtures.sh` idempotent (skip nếu marker tồn tại). Để re-seed, drop `staging_fixture_marker` row trước.
- **Observability minimal:** CloudWatch agent installed, nhưng không có Prometheus/Grafana. Phase 2 EKS sẽ ship full stack.
- **No staging E2E suite chạy auto:** Playwright E2E suite extension tracked trong follow-up (out of Bucket D scope). Smoke test (per GAP-377) là acceptance gate hiện tại.
- **DNS configuration manual:** Cloudflare DNS records phải set thủ công sau `terraform apply` (Cloudflare là DNS primary per ADR-018 — không quản lý qua Terraform).

---

## 10. Acceptance gates (per GAP-380)

Status flip → 🟢 DONE chỉ khi:
- [ ] Staging EC2 verified live (SSM session works)
- [ ] DNS staging.kitehub.vn + staging.kitehub.me resolve
- [ ] docker-compose stack healthy (`docker compose ps` all Up)
- [ ] Flyway migrations applied
- [ ] Smoke test passes
- [ ] Synthetic fixtures seeded
- [ ] Cost monitoring confirmed <$50/mo (CloudWatch Billing alarm)
- [ ] Tear-down command verified (`terraform apply -var=enable_staging=false`)

Tất cả các bước trên là **USER-ACTION** post-merge — agent ship code only (per GAP-381 Phase 2 BANNED). Wave 38 Bucket D ship Status = 🟡 PARTIAL.

---

## 11. Related runbooks

- [`backup-runbook.md`](./backup-runbook.md) — RDS backup procedures (apply to staging too)
- [`restore-procedure.md`](./restore-procedure.md) — Restore drill
- [`rollback-procedure.md`](./rollback-procedure.md) — Rollback (production-focused, adapt for staging)
- [`cicd-release-procedure.md`](./cicd-release-procedure.md) — Tag-based release CI (Wave 38 Bucket A — GAP-374)
- [`deploy-go-nogo-checklist.md`](./deploy-go-nogo-checklist.md) — Production deploy gate

---

## 12. Log

- **2026-05-07** (v1.0): Runbook created cho Wave 38 Bucket D (GAP-380 staging activation Architecture B revision). Architecture B locks EC2 + docker-compose Phase 1; Phase 2 EKS migration trigger gate documented per Wave 37 GAP-415.

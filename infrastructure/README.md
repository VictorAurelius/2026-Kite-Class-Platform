# Infrastructure

Deployment, orchestration, and provisioning configurations for the Kite Platform.

> **Phase 1 BETA reality (2026-05-11):** Production deploy = **AWS Singapore (`ap-southeast-1`) Free Tier với EC2 docker-compose (Architecture B)** per [ADR-025](../documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md). Kubernetes (`helm/`, `k8s/`) là **Phase 1.5+ migration target — dormant** ở phase hiện tại. Oracle Cloud (`terraform-oracle/`) đã **archived** 2026-05-07.

## Structure

```
infrastructure/
├── helm/                   # Helm charts — ⏳ DORMANT (Phase 1.5+ migration target)
│   ├── kitehub/            # KiteHub platform chart
│   └── kiteclass-instance/ # Per-tenant KiteClass chart
├── k8s/                    # Raw K8s manifests — ⏳ DORMANT (Phase 1.5+ migration target)
│   ├── kitehub/            # KiteHub deployments, services, secrets
│   └── kiteclass-template/ # KiteClass instance template
├── terraform-aws/          # ✅ ACTIVE — Phase 1 BETA AWS infra (VPC, EC2, RDS, ALB, S3, ECR, Secrets, IAM, CloudTrail)
└── logs/                   # CI/CD operation logs (mostly gitignored)
```

> `terraform-oracle/` — ❌ **ARCHIVED** to [`documents/07-archived/oracle-deploy-2026/`](../documents/07-archived/oracle-deploy-2026/) per [ADR-025](../documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md) (Oracle→AWS switch, 2026-05-07).

## Status Matrix

| Folder | Status | Phase | Notes |
|--------|--------|-------|-------|
| `terraform-aws/` | ✅ ACTIVE | Phase 1 BETA | EC2 docker-compose (Architecture B); single-region `ap-southeast-1`; Free Tier |
| `helm/` | ⏳ DORMANT | Phase 1.5+ | Pre-existing chart templates; planned full Kubernetes migration per GAP-415 |
| `k8s/` | ⏳ DORMANT | Phase 1.5+ | Raw manifest templates; co-migrate với Helm Phase 1.5+ |
| `terraform-oracle/` | ❌ ARCHIVED | — | Moved to `documents/07-archived/oracle-deploy-2026/` 2026-05-07 |
| `logs/` | — | — | Gitignored CI/CD operation logs |

## Usage — Phase 1 BETA (current)

### Terraform — AWS (✅ active deploy path)

```bash
cd infrastructure/terraform-aws
cp backend.config.example backend.config  # first time — fill in bucket name
terraform init -backend-config=backend.config
terraform plan
# Apply via GitHub Actions workflow_dispatch — see documents/05-guides/deploy/
```

> Production apply chỉ qua `.github/workflows/terraform-apply.yml` (workflow_dispatch + confirm input "APPLY") per [`release-deploy-standard.md`](../.claude/rules/release-deploy-standard.md) §9. Local `terraform apply` chỉ cho one-time bootstrap (chicken-and-egg OIDC role provisioning). Đọc:
> - [ADR-025 — AWS-only Phase 1 Free Tier](../documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md)
> - ADR-028 — ECS Fargate vs EKS decision (Wave 58 Bucket C — pending merge)
> - `documents/05-guides/deploy/` runbooks (terraform-apply, DNS, SES, secrets)

### Docker Compose (active runtime on EC2)

Compose files live in service directories, not here:
- **Full stack:** `kitehub/docker-compose.kitehub.yml` (canonical — deployed lên EC2 t4g.small)
- **KiteClass standalone:** `kiteclass/docker-compose.dev.yml`

Xem [`documents/02-architecture/docker-platform-architecture.md`](../documents/02-architecture/docker-platform-architecture.md) cho chi tiết.

## Usage — Phase 1.5+ (future, NOT active)

### Helm (Phase 1.5+ migration target)

⏳ **Dormant** — chart templates tồn tại trong `helm/` để chuẩn bị migration sang Kubernetes ở Phase 1.5 PAID. **KHÔNG** dùng cho Phase 1 BETA deploy. Migration plan tracked in GAP-415.

```bash
# NOT active Phase 1 BETA — Phase 1.5+ only
helm upgrade --install kitehub ./infrastructure/helm/kitehub \
  --namespace kite-platform \
  --set global.image.tag=latest
```

### Raw Kubernetes manifests

⏳ **Dormant** — co-migrate với Helm ở Phase 1.5+. Template files tồn tại cho future reference; **không apply** trong Phase 1 BETA.

## Architecture decisions

| ADR | Decision | Status |
|-----|----------|--------|
| [ADR-025](../documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md) | AWS-only Phase 1 BETA Free Tier; Oracle→AWS switch | ACCEPTED 2026-05-07 |
| ADR-028 | ECS Fargate vs EKS (Phase 1.5+ orchestration choice) | DRAFT (Wave 58 Bucket C) |
| [ADR-015](../documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md) | AWS Agent Plugins — DEFER Q3 2026 | ACCEPTED |

## Related rules

- [`release-deploy-standard.md`](../.claude/rules/release-deploy-standard.md) — per-bump-type checklist; §9 Claude agent role in deploy
- [`terraform-apply-retry-reconfirm.md`](../.claude/rules/terraform-apply-retry-reconfirm.md) — apply retry discipline
- [`terraform-partial-backend-public-repo.md`](../.claude/rules/terraform-partial-backend-public-repo.md) — partial backend config rule
- [`aws-sg-description-ascii.md`](../.claude/rules/aws-sg-description-ascii.md) — pre-apply ASCII guard
- [`aws-observability-first.md`](../.claude/rules/aws-observability-first.md) — CloudTrail baseline before infra apply
- [`agent-aws-access.md`](../.claude/rules/agent-aws-access.md) — agent AWS command tier allowlist

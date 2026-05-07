---
name: terraform-cloud-deploy
description: >
  Dùng khi user upload/paste file .tf, hỏi về "review terraform", "kiểm tra hạ tầng", "IaC review",
  "deploy lên AWS", "deploy lên Oracle Cloud", "OCI deployment", "chiến lược deploy cloud",
  "terraform plan", "terraform best practices", "module structure", "state management".
  Kích hoạt khi thấy từ khoá: EC2, EKS, VPC, RDS, OCI Compute, OCI VCN, Autonomous DB, OKE,
  infrastructure/terraform-aws, infrastructure/terraform-oracle.
---

# Terraform & Cloud Deploy Skill

Skill 2 mode. Đọc request → chọn mode:

| User muốn | Mode | Reference cần đọc |
|-----------|------|-------------------|
| Review .tf, tìm lỗi, best practices | **Terraform Review** | `reference/terraform-review.md` |
| Chiến lược deploy lên AWS | **AWS Deploy** | `reference/aws-deploy.md` |
| Chiến lược deploy lên Oracle Cloud | **OCI Deploy** | `reference/oracle-cloud-deploy.md` |
| Review + plan deploy | **Full** | Cả 3 files |

---

## Mode 1 — Terraform Review

1. Đọc `reference/terraform-review.md` → score rubric 5 chiều
2. Inventory tất cả resources
3. Top Issues — 3–5 vấn đề nghiêm trọng nhất với code fix
4. Output format: xem `reference/output-templates.md`

**Input:** file upload `.tf`, paste nội dung, hoặc mô tả cấu trúc.

---

## Mode 2 — Cloud Deploy Strategy

Thu thập thông tin (nếu thiếu) — tối đa 3 câu gộp 1 lần:

```
1. Workload: [web app / microservices / batch / ML]?
2. Cloud: [AWS / Oracle Cloud / cả hai]?
3. Có Terraform hiện tại không?
```

Output gồm: architecture diagram, module structure, cost estimate, deploy phases, pre-launch checklist.
Format: xem `reference/output-templates.md`.

---

## Gotchas — KiteClass-specific

- **Remote state bị comment** — `infrastructure/terraform-aws/main.tf` line 22-28: `backend "s3"` đang bị comment out → local state. PHẢI enable trước deploy prod
- **Oracle state = local** — `infrastructure/terraform-oracle/main.tf` không có backend block → state mất khi xóa VM
- **OCI target là ARM VMs (Always Free)** — không phải OKE; `VM.Standard.A1.Flex` 2 VMs cho backend + frontend/AI
- **Multi-tenant K8s namespaces** — mỗi tenant KiteClass → 1 namespace riêng, 1 Helm release riêng (`infrastructure/helm/kiteclass-instance/`)
- **KiteHub dùng AWS EKS** — `infrastructure/terraform-aws/eks.tf`; KiteClass dùng OCI Compute
- **tfvars không commit** — `terraform.tfvars.example` có sẵn; file thực phải trong `.gitignore`
- **Oracle provider auth** — local dev dùng `private_key_path`; production instance nên dùng `auth = "InstancePrincipal"`

---

## Skill Contents

- `reference/terraform-review.md` — Scoring rubric (100 pts), Common Issues, Security Checklist
- `reference/aws-deploy.md` — VPC/EKS/RDS modules, deploy phases, cost estimation, pre-launch checklist
- `reference/oracle-cloud-deploy.md` — VCN, ATP, OKE, IAM policies, OCI vs AWS mapping
- `reference/output-templates.md` — Report templates copy-paste

---

## KiteClass Infrastructure Map

```
infrastructure/
├── terraform-aws/      # KiteHub → AWS EKS (ap-southeast-1)
│   ├── main.tf         ⚠️ S3 backend commented out
│   ├── eks.tf          # EKS cluster
│   ├── rds.tf          # PostgreSQL RDS
│   └── elasticache.tf  # Redis ElastiCache
├── terraform-oracle/   # KiteClass → OCI ARM VMs (Always Free)
│   ├── main.tf         ⚠️ No remote backend
│   ├── compute.tf      # 2x VM.Standard.A1.Flex
│   └── network.tf      # VCN + subnets
├── helm/
│   ├── kitehub/        # KiteHub Helm chart
│   └── kiteclass-instance/  # Per-tenant chart
└── k8s/
    ├── kitehub/
    └── kiteclass-template/
```

---

---

## Phase 1 BETA AWS Gotchas (post Phase 2.3 apply, 2026-05-08)

Lessons khi apply 71 resources Architecture B trên AWS Singapore (account 906286017800). Detailed reference: [`reference/phase-1-aws-gotchas.md`](reference/phase-1-aws-gotchas.md).

Quick index:
- **G1** SG description ASCII-only — em-dash fail apply
- **G2** Public-repo partial backend config — `-backend-config=` + GitHub Variable
- **G3** CloudTrail BEFORE Phase 2.3 — audit baseline first
- **G4** Phased apply via `-target=` — split state/IAM/audit/infra
- **G5** `count` resource `[0]` indexing — required even for count=1
- **G6** GitHub Variables vs Secrets — non-secret config goes to Variables
- **G7** Resume after partial fail — fix file + re-run preserves state (BUT re-confirm with user, see `feedback_terraform_apply_retry_reconfirm.md`)
- **G8** OIDC role secret name disambiguation — least-privilege per workflow

Memory cross-refs (auto-loaded each session):
- `feedback_aws_sg_description_ascii_only.md`
- `feedback_terraform_partial_backend_public_repo.md`
- `feedback_aws_observability_first.md`
- `feedback_terraform_targeted_apply_phases.md`
- `feedback_terraform_apply_retry_reconfirm.md`

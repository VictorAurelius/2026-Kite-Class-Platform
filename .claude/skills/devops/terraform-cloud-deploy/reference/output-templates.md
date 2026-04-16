# Output Templates — Copy-paste

## Terraform Review Report

```
=== TERRAFORM REVIEW ===
Files phân tích: X files, Y resources
Score:
  Security:       ??/20
  Structure:      ??/20
  Best Practices: ??/20
  Cost Awareness: ??/20
  Reliability:    ??/20
  TOTAL:          ??/100

Critical Issues (fix ngay):
1. [Issue] — [File:Line] — [Fix]

Warnings:
...

Recommendations:
...
```

---

## Deploy Strategy Report

```
=== CLOUD DEPLOY STRATEGY ===
Target: AWS / OCI / Both
Architecture: [pattern name]
Estimated monthly cost: $X–$Y

Phase 1 (Tuần 1–2): Foundation
Phase 2 (Tuần 3–4): Application Layer
Phase 3 (Tuần 5–6): Production Hardening

Terraform Module Structure:
...

Pre-launch Checklist:
...
```

---

## KiteHub AWS Review Example

```
=== TERRAFORM REVIEW — KiteHub AWS ===
Files: infrastructure/terraform-aws/ (8 files)
Resources: VPC, EKS, RDS PostgreSQL, ElastiCache, S3, ECR, Secrets Manager

Score:
  Security:       16/20  (tags ok, nhưng thiếu MFA enforcement)
  Structure:      17/20  (tách files tốt, thiếu modules/ folder)
  Best Practices:  9/20  ⚠️ S3 backend COMMENTED OUT (local state = -6)
  Cost Awareness: 14/20  (sizing hợp lý dev/prod)
  Reliability:    15/20  (Multi-AZ ok, thiếu backup policy verify)
  TOTAL:          71/100

Critical Issues:
1. Local Terraform state — main.tf:22-28 — Uncomment backend "s3" block
   + Tạo S3 bucket + DynamoDB table trước (xem references/aws-deploy.md)
```

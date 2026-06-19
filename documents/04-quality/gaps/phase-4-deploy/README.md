# `phase-4-deploy/` — gaps requiring live AWS stack / vendor production (any non-DONE status)

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2 v2.0.0

## Contract

Files matching: `phase == phase-4-deploy` AND `status != DONE`. Phase 4 = deploy/go-live: work that CANNOT close without the AWS stack running (terraform/EC2/RDS/IAM/SES/CloudWatch/ECR/ALB/cert/DNS/restore-drill/"5 beta tenants live") OR a vendor production account (Resend/SePay/Zalo live). Split from phase-1-beta 2026-06-19 (campaign `phase-1-closeout`) so Phase 1 gate is not blocked by deploy/cost dependency. Resumes on AWS redeploy decision.

## Subdir

- `closed/` — DONE archive

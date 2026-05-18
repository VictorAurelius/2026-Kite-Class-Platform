---
title: AWS Account Re-Setup Estimate (GAP-612 recovery)
status: complete
created: 2026-05-18
phase: phase-1-beta
wave: post-95
gaps: [GAP-612]
related-rules: [release-deploy-standard.md, aws-observability-first.md, pre-mutation-state-check.md, agent-aws-access.md, concurrent-production-mutation-ops.md]
---

# AWS Account Re-Setup Estimate — Phase 1 BETA recovery

**Trigger:** GAP-612 AWS account suspension 2026-05-17. Estimate documented here so future sessions không phải re-investigate from scratch.

**Source of truth:** `infrastructure/terraform-aws/*.tf` (25 files) + `documents/05-guides/account-prep/` runbooks + recent AWS verification audits.

---

## 1. Resource inventory (terraform-managed)

**Total: ~170+ AWS resources**

| Category | Count | Notes |
|---|---|---|
| **Compute** | 4 EC2 | kh_backend t3.large + kc_app t3.medium + 2 kc_app FE |
| **Database** | 2 RDS db.t3.micro + 2 subnet groups | 1 free tier + 1 paid |
| **Network** | 1 VPC + 1 IGW + 4 subnets + 1 NAT (disabled default) + 6 SGs + 2 EIPs + 1 ALB + 2 listeners + 1 target group | |
| **Storage** | 3 S3 buckets + 1 ECR repo | + lifecycle/versioning/encryption/policy |
| **DNS** | 1 Route53 zone + 3 records | + Cloudflare external |
| **Secrets** | 5 secrets + 4 versions + 3 rotation Lambda hooks | + KMS keys |
| **IAM** | 14 roles + 15 policies + 8 attachments + 1 OIDC provider + 1 instance profile | OIDC = GitHub Actions deploy |
| **Observability** | 1 CloudTrail + 1 CloudWatch dashboard + 24 alarms + 4 metric filters + 3 log groups + 6 security alarms | |
| **Async** | 2 Lambda + 8 EventBridge scheduler + 4 SNS topics + 5 subscriptions + 1 event rule + 1 event target | |

## 2. Setup time estimate

| Phase | Tasks | Wall-clock |
|---|---|---|
| **0. AWS account creation** | Root email + payment + MFA + activation wait | **1-24h** (activation lag variable) |
| **1. Bootstrap chicken-and-egg** | OIDC provider + apply role + S3 backend + DynamoDB lock (one-time local terraform apply with admin key, immediately rotate per `release-deploy-standard.md` §9 carve-out) | **1h** |
| **2. CloudTrail FIRST** | Per `aws-observability-first.md` §3 targeted apply BEFORE full infra (audit baseline) | **15 min** |
| **3. Full terraform apply** | 170+ resources via `terraform-apply.yml` workflow_dispatch | **30-45 min** |
| **4. Application deploy** | ECR login + push 6 service images + SSM SendCommand deploy-prod.sh | **45-60 min** |
| **5. DNS swap** | Cloudflare DNS apex `kitehub.me` → new EIP/ALB | **30 min + TTL prop 2-5min** |
| **6. SES re-verify** | Domain verify TXT + DKIM CNAMEs (3) + SPF/DMARC + production approval request | **15 min + AWS approval 24-72h** |
| **7. ACM certificate** | Re-issue ACM cert (DNS validation via Route53/CF) + ALB attach | **15 min + DNS prop** |
| **8. Smoke tests** | Auth flow + admin login per `release-deploy-standard.md` §3.1 + email flow | **30 min** |
| **9. Secrets re-seed** | 5 secrets via terraform-managed (kitehub/production/* prefix) | **15 min** |
| **TOTAL active work** | | **~4-5h** |
| **TOTAL wall-clock with async** | | **~6-30h** (gated by AWS activation + SES approval) |

## 3. Monthly cost estimate (steady-state, no credits)

| Resource | Free Tier? | Paid (USD/mo) |
|---|---|---|
| EC2 t3.large (kh_backend) | ❌ (only t3.micro × 750h free) | **~$60** |
| EC2 t3.medium (kc_app BE) | ❌ | **~$30** |
| EC2 t3.medium (kc_app FE × 1-2) | ❌ | **~$30-60** |
| RDS db.t3.micro × 1 | ✅ 750h free 12mo | $0 |
| RDS db.t3.micro × 2nd | ❌ | **~$15** |
| ALB | ❌ (not in free tier) | **~$16** |
| NAT Gateway | (disabled default) | $0 |
| EBS gp3 (3-4 instances × 20-30GB) | partial 30GB free | **~$2-3** |
| Route53 zone | ❌ | **~$0.50** |
| Secrets Manager (5) | ❌ | **~$2** |
| CloudWatch logs/metrics | partial free | **~$3-5** |
| CloudTrail mgmt events (1st copy) | ✅ free | $0 |
| Data transfer (out) | partial 100GB free | **~$0-10** |
| SES (from EC2) | ✅ 62k email/mo free | $0 |
| Lambda (rotation hooks) | ✅ 1M req/mo free | $0 |
| S3 (3 buckets) | ✅ 5GB free | $0 |
| ECR | partial 500MB free | **~$1-2** |
| **TOTAL** | | **~$130-200/mo paid** |

**Mitigation:** New account = fresh 12-month Free Tier clock. Save ~$50/mo for first year vs exhausted old account.

**AWS Activate Founders/Startup credits** previously denied (per `documents/05-guides/deploy/aws-activate-confirmation/`). Re-apply with new account possible but not guaranteed.

## 4. Critical path sequence

```
Day 0:
  1. Create new AWS account → MFA root → enable Free Tier billing alerts (>$1 + $10 + $50)
  2. Apply for AWS Activate Founders (async 24-72h response)
  3. Submit SES production approval request (async 24-72h)

Day 1 (after account active):
  4. Bootstrap: create OIDC provider + apply role + S3 backend + DynamoDB lock
     (one-time local terraform apply with admin key, IMMEDIATELY rotate admin key)
  5. CloudTrail targeted apply (per aws-observability-first.md §3) BEFORE full infra
  6. Verify CloudTrail logging=true via aws cloudtrail get-trail-status
  7. Update workflow secrets (AWS_ACCOUNT_ID, OIDC role ARN, ECR registry URL)
  8. Find/replace old account ID `906286017800` → new account ID in:
     - infrastructure/terraform-aws/*.tf + README + terraform.tfvars
     - infrastructure/terraform-cloudflare/providers.tf + README
     - documents/05-guides/deploy/secrets-populate-phase-2-4.md
     - documents/05-guides/deploy/fe-self-host-runbook.md
     - documents/05-guides/deploy/phase-3-image-push.md
     - documents/05-guides/deploy/release-1-tier-3-cutover.md
     - documents/05-guides/deploy/rabbitmq-cred-sync-runbook.md
     - scripts/reboot-and-deploy-minimal.sh
     - scripts/bootstrap-and-verify.sh
     - scripts/populate-secrets.sh
     - scripts/deploy-via-local-ssm.sh
     (~28 files total per grep)

Day 1-2:
  9. Full terraform apply via workflow_dispatch (terraform-apply.yml)
     - Pre-mutation audit artifact per pre-mutation-state-check.md §3
     - confirm=APPLY explicit input
     - dry_run=true first → verify plan → dry_run=false
  10. terraform apply produces ~170 resources

Day 2:
  11. ECR push 6 service images (gateway, branding, email, platform, subscription, admin)
  12. SSM SendCommand deploy-prod.sh
  13. Smoke admin-login per release-deploy-standard.md §3.1
       (smoke test must hit real Postgres + production secrets, not H2/Mockito)

Day 2-3 (after SES approval + AWS Activate response):
  14. ACM cert re-issue + DNS validate + ALB attach
  15. Cloudflare DNS swap apex → new EIP
  16. Email flow smoke test (signup → SES verify email lands in inbox)
  17. UNBLOCK GAP-537c-followup-screenshot-capture (P2/P3 manual screenshots)
```

## 5. Files needing find/replace (28 files reference old account ID `906286017800`)

```
infrastructure/terraform-cloudflare/README.md
infrastructure/terraform-cloudflare/providers.tf
infrastructure/terraform-aws/README.md
infrastructure/terraform-aws/.terraform/terraform.tfstate
documents/05-guides/deploy/aws-activate-confirmation/2026-05-11-resubmission.md
documents/05-guides/deploy/aws-activate-confirmation/2026-05-09-submission.md
documents/05-guides/deploy/secrets-populate-phase-2-4.md
documents/05-guides/deploy/fe-self-host-runbook.md
documents/05-guides/deploy/phase-3-image-push.md
documents/05-guides/deploy/release-1-tier-3-cutover.md
documents/05-guides/deploy/rabbitmq-cred-sync-runbook.md
scripts/reboot-and-deploy-minimal.sh
scripts/bootstrap-and-verify.sh
scripts/populate-secrets.sh
scripts/deploy-via-local-ssm.sh
... (13 more — full grep available via `grep -rl 906286017800 infrastructure/ .github/ documents/05-guides/ scripts/`)
```

Per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync: account ID change = config-shaped value change → sweep mandatory same wave.

## 6. Risks / blockers

| # | Risk | Mitigation |
|---|---|---|
| 1 | **AWS Activate Founders credits denied** previously | Re-apply with new account + better plan; else pay full $130-200/mo |
| 2 | **SES production approval async** (sandbox limit 200/day) | Submit Day 0; can take 24-72h |
| 3 | **Account ID hardcoded** in ~28 files | Sweep same wave per §2.7 |
| 4 | **Cloudflare DNS swap** apex points to old EIP | Update A record + TTL prop |
| 5 | **Concurrent ops** per `concurrent-production-mutation-ops.md` | Serial bootstrap (CloudTrail → infra → app deploy) — no parallel mutation |
| 6 | **Pre-mutation state-check** per `pre-mutation-state-check.md` v1.2.0 | Audit artifact under `documents/04-quality/audits/aws-verification/YYYY-MM-DD-new-account-{bootstrap,infra-apply,dns-swap}.md` per each major mutation |
| 7 | **Past audits reference old account ID** | Grandfathered as historical; new audits use new account ID |
| 8 | **Secrets rotation Lambda permissions** | Already in terraform; provisioned on apply |
| 9 | **Cloudflare API token** survives vendor-scope | Rotate if leaked per session-rotation history |
| 10 | **Dependabot + CI workflows** reference OIDC role ARN | Update `aws-actions/configure-aws-credentials@v4` role-to-assume |
| 11 | **CloudFlare apex DNS TTL** affects swap window | Set TTL low (60-300s) before swap; restore after |
| 12 | **EBS snapshot/RDS final snapshot** old account | New account fresh; data loss for non-essential test data acceptable (Phase 1 BETA = invite-only, no production tenant data yet) |

## 7. Decision point: trigger now vs defer

| Option | Pros | Cons | Recommendation |
|---|---|---|---|
| **Restore old account** (appeal suspension) | No re-setup work | Outcome uncertain; 24-72h appeal cycle; root cause unclear | Try once parallel to new account prep |
| **Start new account immediately** | Reset Free Tier clock; clean slate | ~4-5h active work + 24-72h async | **Recommended if old account appeal stalls >48h** |
| **Defer beyond Phase 1 BETA gate** | Skip work | Blocks GAP-537c manual screenshots + Wave 91 Bucket F live verify + Wave 92 Bucket A/C verify cluster | Not viable per Phase 1 BETA gate dependencies |

## 8. Out-of-scope / future considerations

- **Multi-region failover** — single-region ap-southeast-1 by design (Phase 1 BETA). DR plan defer.
- **EKS migration** — Architecture B (EC2 + RDS) per ADR-025; EKS Phase 2 GAP-479.
- **Production data restore** — Phase 1 BETA = invite-only, no production tenant data; no restore needed.
- **Compliance attestation** — VN Cybersecurity Law / PDPL — same compliance regardless of account ID; ap-southeast-1 region satisfies data localization.

## 9. References

- Rule: `.claude/rules/release-deploy-standard.md` §3.4 + §9
- Rule: `.claude/rules/aws-observability-first.md` §3
- Rule: `.claude/rules/pre-mutation-state-check.md` §3
- Rule: `.claude/rules/agent-aws-access.md` §2
- Rule: `.claude/rules/concurrent-production-mutation-ops.md` §3
- Audit: `documents/04-quality/audits/aws-verification/2026-05-11-actual-cost-vs-estimate.md`
- Runbook: `documents/05-guides/account-prep/01-aws-account-creation.md`
- Runbook: `documents/05-guides/deploy/bootstrap-runbook.md`
- Runbook: `documents/05-guides/deploy/terraform-apply-bootstrap-runbook.md`

## 10. Log

- **2026-05-18:** Estimate documented to avoid re-investigation per user direction. Triggered by GAP-612 AWS account suspension 2026-05-17 (account 906286017800). User asked "estimate setup lại resource ở account AWS mới" — full inventory + setup time + cost + risk + critical path captured here. Next session can use as direct input without re-investigating terraform files + runbooks.

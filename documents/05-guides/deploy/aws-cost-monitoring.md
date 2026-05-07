# AWS Cost Monitoring — Budgets Alarms + ECR Lifecycle + Tag Policy

**Status:** ACCEPTED — Phase 1 BETA chốt 2026-05-07
**Last-Reviewed:** 2026-05-07
**Reviewer:** @nguyenvankiet (solo-dev, acting CTO + acting FinOps)
**Closes:** GAP-413 (AWS Budgets cost monitoring)
**Related:** GAP-411 (sizing matrix — $80 alarm threshold rationale), GAP-412 (Activate credit depletion alarm), GAP-414 (right-sizing review feeds), GAP-395 (Terraform provisions alarms)
**Region:** `ap-southeast-1`

---

## 1. Mục đích

Phase 1 BETA Architecture B target $72/mo (per `aws-architecture-sizing-matrix.md` §3). Cost overrun protection cần 3 layers:

1. **Hard cap monthly alarm** — actual + forecast > $80 → alert email
2. **Activate credit depletion alarm** — credit balance < 20% → alert email
3. **Per-service tag alarm** — per-service spike identification

Plus 2 supporting policies:
- **Tag policy** — Terraform `default_tags` apply tới TẤT CẢ AWS resources (Service / Environment / Phase)
- **ECR lifecycle policy** — auto-delete untagged + >7d old images (free 500MB tránh tràn)

AWS Budgets là **free service** (first 2 budgets free; alarms free unlimited). Email notifications free; Slack webhook qua SNS thêm $0.50/M messages → marginal.

---

## 2. 3 Required Budget Alarms

### 2.1 Alarm A — Monthly cost hard cap ($80)

**Purpose:** Detect cost overrun vs Architecture B target $72 + buffer 11%.

**Config:**
- Budget type: Cost Budget
- Period: Monthly
- Amount: $80
- Notification 1: Actual ≥ 80% ($64) → email warning
- Notification 2: Actual ≥ 100% ($80) → email critical
- Notification 3: Forecasted ≥ 100% ($80) → email proactive (forecast based on month-to-date trend)
- Recipient: vannkite@outlook.com

**Trigger response runbook:** see §6.

### 2.2 Alarm B — Activate credit depletion (<20%)

**Purpose:** Per GAP-412, AWS Activate $1k credit cover ~13.9 tháng. Khi balance <$200 → 2 tháng budget runway → cần plan transition to revenue-funded OR file follow-up Activate Investor track ($5k).

**Config:**
- Budget type: Cost Budget với Credit dimension
- Period: Quarterly (rolling)
- Amount: $1000 (initial credit)
- Notification: Credit Used ≥ 80% (~$800 used → ~$200 remaining = <20%) → email warning
- Notification: Credit Used ≥ 95% → email critical (transition imminent)
- Recipient: vannkite@outlook.com

**Trigger response runbook:** see §6.

### 2.3 Alarm C — Per-service tag spike

**Purpose:** Identify cost outlier service. Beta scope 8 services + infra → spike 1 service masks behind aggregate.

**Config:**
- Budget type: Cost Budget với Filter
- Filter dimension: Tag `Service` = each of (kitehub-branding, kitehub-subscription, kitehub-email, kitehub-admin, kitehub-gateway, kitehub-platform, kiteclass-core, infra-shared)
- Period: Monthly
- Amount: $15/service (rough split of $80 / 8 services + buffer; review monthly)
- Notification: Actual ≥ 100% → email
- Recipient: vannkite@outlook.com

**Note:** Per-service alarm requires §3 Tag policy active. If untagged resources accumulate, alarm misses spikes. Tag policy = prerequisite.

---

## 3. Tag Policy — Terraform `default_tags`

**Required tags on every AWS resource (Phase 1 BETA):**

| Tag key | Allowed values | Source |
|---|---|---|
| `Service` | `kitehub-branding`, `kitehub-subscription`, `kitehub-email`, `kitehub-admin`, `kitehub-gateway`, `kitehub-platform`, `kiteclass-core`, `infra-shared` | Per-module Terraform |
| `Environment` | `production`, `staging`, `dev` | Per-env workspace |
| `Phase` | `1-beta`, `1.5-paid`, `2-p3`, `3-k12` | Hardcode Phase 1 BETA = `1-beta` |
| `ManagedBy` | `terraform`, `manual` | Default `terraform` |
| `OwnerEmail` | `vannkite@outlook.com` | Solo-dev contact |

**Terraform pattern (per GAP-395 Bucket A):**

```hcl
provider "aws" {
  region = "ap-southeast-1"
  default_tags {
    tags = {
      Environment = var.environment
      Phase       = "1-beta"
      ManagedBy   = "terraform"
      OwnerEmail  = "vannkite@outlook.com"
    }
  }
}

# Per-resource Service tag override:
resource "aws_instance" "kitehub_main" {
  # ...
  tags = {
    Service = "kitehub-shared" # 6 KiteHub services co-located trên t3.medium
  }
}
```

**Untagged resource detection:** AWS Config rule `required-tags` (free tier 10 rules) → flag resources missing required tags. Optional Phase 2 enable.

---

## 4. ECR Lifecycle Policy

**Purpose:** ECR free tier 500MB Phase 1 BETA. 6 services × ~50MB image × 5 versions retained = ~1.5GB → vượt free → ~$0.10/GB/mo billable. Lifecycle policy ngăn drift.

**Policy (apply per-repo via Terraform):**

```json
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Delete untagged images after 7 days",
      "selection": {
        "tagStatus": "untagged",
        "countType": "sinceImagePushed",
        "countUnit": "days",
        "countNumber": 7
      },
      "action": { "type": "expire" }
    },
    {
      "rulePriority": 2,
      "description": "Keep 10 most recent tagged images per repo",
      "selection": {
        "tagStatus": "tagged",
        "tagPatternList": ["v*", "main-*", "rc-*"],
        "countType": "imageCountMoreThan",
        "countNumber": 10
      },
      "action": { "type": "expire" }
    }
  ]
}
```

**Coverage:** 8 ECR repos (1 per service) × policy → keep 10 recent tagged + delete untagged >7d. Estimated ECR usage post-policy: ~500MB stay in free tier.

**Apply via:** GAP-395 Terraform `infrastructure/terraform-aws/ecr.tf` resource `aws_ecr_lifecycle_policy`.

---

## 5. Notification Channels

### 5.1 Email primary (Phase 1 BETA)

- Recipient: vannkite@outlook.com
- Verification: AWS Budgets sends test email at provisioning; verify inbox + mark "not spam"
- Frequency cap: AWS Budgets dedupes same-condition within 24h

### 5.2 Slack webhook (Phase 1.5+ optional)

Defer Phase 1.5+. Provisioning steps cho future:
1. Create SNS topic `cost-alarms` ap-southeast-1
2. Subscribe Slack webhook endpoint qua SNS HTTP/HTTPS subscription
3. Update Budget notification target → SNS topic ARN
4. Cost: SNS $0.50/M messages → marginal cho ~10 alarms/month

---

## 6. Trigger Response Runbook

### 6.1 Alarm A trigger ($64 actual / $80 forecast / $80 actual)

**At 80% ($64):**
1. Open Cost Explorer: https://console.aws.amazon.com/cost-management/home#/cost-explorer
2. Filter: Last 14 days; Group by Service (then by Tag.Service)
3. Identify top 3 cost drivers vs prior period
4. Decision tree:
   - Spike trong EC2 → check CloudWatch CPU; nếu sustained → upsize logic OR investigate runaway loop
   - Spike trong Data Transfer → check VPC Flow Logs (nếu enabled); identify unusual egress; throttle
   - Spike trong CloudWatch Logs → identify chatty service (per Tag.Service); reduce log level production
5. File ad-hoc cost report `documents/04-quality/cost-reports/YYYY-MM-DD-spike.md` với root cause + action

**At 100% ($80):**
- Tất cả §6.1 80% steps + decision: scale-down OR enable Spot OR temporarily reduce non-critical workload (AI background workers, bulk email, batch reports)
- Update GAP-414 monthly review entry với incident link

**At forecast 100%:**
- Proactive — same as 80% trigger; pre-empt before actual hits

### 6.2 Alarm B trigger (Activate credit <20%)

**At 80% used (~$200 remaining):**
1. Calculate runway: $200 / $72 budget = ~2.7 tháng
2. Decision tree:
   - Phase 1 BETA still active → file Activate Investor application ($5k upgrade) — see GAP-412 §"Optional enhancement"
   - Phase 1.5 PAID early → revenue projection check; nếu ≥30 paying tenants @$5/mo = $150 → starts covering budget
   - Neither → reduce scope: hibernate non-critical services, downsize EC2, defer Phase 1.5 features
3. Update `documents/05-guides/deploy/aws-activate-credit-policy.md` Log

**At 95% used:**
- Transition complete OR runway < 1 month — emergency budget review
- Document quarterly retro

### 6.3 Alarm C trigger (per-service tag spike)

**At service > $15:**
1. Check service logs (per `logs-format-standard.md`) cho error spike, retry storm, runaway job
2. Cost Explorer drill-down: which AWS subsidiary cost (EC2 hours? Data transfer? CloudWatch?)
3. Identify if legitimate growth (more tenants → more usage) vs anomaly
4. Action: legitimate → adjust per-service threshold next month; anomaly → fix service + post-mortem

---

## 7. Provisioning via Terraform

GAP-395 Bucket A includes:
- `aws_budgets_budget.monthly_cap` (Alarm A)
- `aws_budgets_budget.activate_credit` (Alarm B)
- `aws_budgets_budget.per_service[*]` (Alarm C, for_each over service list)
- `aws_ecr_lifecycle_policy.cleanup` (per-repo)
- `provider "aws" { default_tags { ... } }` (Tag policy baseline)

Apply via `terraform apply` post-account-prep (per GAP-394).

---

## 8. Acceptance Criteria mapping

| GAP-413 AC | Status |
|---|---|
| 3 AWS Budgets alarms provisioned (Terraform via GAP-395) | ✅ §2 spec — Terraform implementation owned GAP-395 Bucket A |
| Email recipient: vannkite@outlook.com + on-call rotation | ✅ §5.1 (on-call rotation N/A solo-dev) |
| Tag policy enforced via Terraform `default_tags` | ✅ §3 |
| ECR lifecycle policy (delete untagged + >7d old) | ✅ §4 |
| Document `documents/05-guides/deploy/aws-cost-monitoring.md` (runbook on alarm) | ✅ this file (runbook §6) |

---

## 9. References

- GAP-411 — sizing matrix ($80 threshold = $72 + 11% buffer)
- GAP-412 — Activate credit policy
- GAP-414 — right-sizing monthly review (consumes alarm trigger data)
- GAP-395 — Terraform implementation (Bucket A Wave 37)
- ADR-025 — AWS Singapore decision
- `.claude/rules/logs-format-standard.md` — log format for service identification
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` — phase progression
- AWS Budgets docs: https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-managing-costs.html
- AWS Cost Explorer docs: https://docs.aws.amazon.com/cost-management/latest/userguide/ce-what-is.html

---

## 10. Log

- **2026-05-07** — Initial cost monitoring policy. 3 alarms + Tag policy + ECR lifecycle + runbook. Implementation deferred GAP-395 Terraform (Bucket A Wave 37). Closes GAP-413 acceptance criteria (policy doc); Terraform provisioning tracked separately Bucket A.

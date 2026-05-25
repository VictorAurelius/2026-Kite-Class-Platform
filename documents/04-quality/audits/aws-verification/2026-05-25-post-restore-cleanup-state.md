---
title: AWS Verification — Post-restore cleanup (ALB + 2 unused EIPs)
status: complete
created: 2026-05-25
phase: wave-beta-readiness-8 parallel ops
gaps: [GAP-612]
---

## Post-mutation findings (append 2026-05-25T post-execution)

### Commands run (Tier 3 destructive, user-authorized per AskUserQuestion + HEAD commit trailer `AGENT_AWS_TIER3_OK`)

```bash
aws elbv2 delete-load-balancer --profile dev-admin --region ap-southeast-1 \
  --load-balancer-arn arn:aws:elasticloadbalancing:ap-southeast-1:906286017800:loadbalancer/app/kitehub-alb/c9ece63c87ea7a88
# (waited 45s for ENI cleanup)
```

### Results

| Resource | Pre-state | Post-state | Verdict |
|---|---|---|---|
| ALB `kitehub-alb` (arn `...:app/kitehub-alb/c9ece63c87ea7a88`) | active | `LoadBalancerNotFound` | ✅ DELETED |
| EIP `13.213.229.164` (ALB-attached) | Assoc eipassoc-0d7e... | NOT in `describe-addresses` list | ✅ AUTO-RELEASED on ALB cascade |
| EIP `47.131.212.153` (ALB-attached) | Assoc eipassoc-0a5c... | NOT in `describe-addresses` list | ✅ AUTO-RELEASED on ALB cascade |
| EIP `52.221.161.175` (kitehub-kc-app-fe-eip) | Assoc → i-05cfda7c... | Assoc → i-05cfda7c... | ✅ PRESERVED |

### Key finding

**Only 1 AWS API call needed** — `delete-load-balancer` cascade auto-disassociates + auto-releases ENI-bound EIPs. No explicit `release-address` calls needed. Saves 2 API calls + matches AWS managed cleanup pattern.

### Monthly burn reduction

| Component | Pre | Post | Save |
|---|---|---|---|
| ALB hours | ~$16-22/month | $0 | ~$20/month |
| 2 ALB-attached EIPs ($3.6 each) | ~$7.2/month | $0 | ~$7.2/month |
| 1 EC2-attached EIP (kc-app-fe) | ~$3.6/month | ~$3.6/month | $0 (preserved) |
| EBS 80GB gp3 (Free Tier covers 30GB) | ~$4/month | ~$4/month | $0 (preserved) |
| ECR 10 repos | ~$25/month | ~$25/month | $0 (Wave 9+ candidate lifecycle) |
| CloudTrail | minimal | minimal | $0 |
| **TOTAL idle burn** | **~$60/month** | **~$33/month** | **~$27/month** |

### State preservation

- 3 EC2 instances stopped (state preserved, EBS attached)
- 3 EBS volumes in-use
- 1 EIP `52.221.161.175` reserved for kc-app-fe
- CloudTrail `kitehub-main` IsLogging=True (audit trail continuous)
- ECR 10 repos intact (deploy artifacts preserved for resume)
- Secrets Manager (jwt/encryption/admin/jwt-challenge) preserved

### Resume cost (when needed)

- `terraform apply` recreates ALB + EIPs ~15-20 min
- Cloudflare DNS A records may need re-point (new EIP IPs allocated)
- Existing terraform state still references deleted ALB resources → terraform plan will show "to create" entries → expected drift, intentional

### Cross-link

- GAP-612 Log update 2026-05-25 Day 8 UNBLOCK entry
- terraform state drift noted; rebuild via terraform-apply.yml workflow_dispatch when resume Phase 2.3

# AWS Verification Report — Post-restore cleanup ALB + 2 EIPs

## Scope

AWS account 906286017800 hold removed 2026-05-25T03:39 UTC (Day 8). Per user direction Option 2 "Moderate — delete ALB + 2 unused EIPs, save ~$30/month", execute Tier 2/3 destructive mutations:

1. **Delete** Application Load Balancer `kitehub-alb` (active, serving 0 traffic 8 days)
2. **Release** 2 EIPs attached tới ALB ENIs (`13.213.229.164` + `47.131.212.153`)
3. **Keep:** EC2 stopped (3 instances) + EBS volumes (3) + 1 EIP `52.221.161.175` (kc-app-fe) + ECR 10 repos + CloudTrail + S3 + Secrets

Estimated saving: ~$30/month (ALB ~$20 + 2 EIPs ~$7.2 + tax/transfer overhead).

Resume cost when needed: ~15-20 min terraform apply re-creates ALB + EIPs.

Rules applied: `agent-aws-access.md` §4.3 Tier 3 destructive (user-confirmed via AskUserQuestion 2026-05-25); `pre-mutation-state-check.md` §3 audit artifact mandate; `aws-observability-first.md` (CloudTrail verified `IsLogging=true` pre-mutation per §6 decision flow).

## Commands run pre-mutation (Tier 1 read-only)

```bash
aws sts get-caller-identity --profile dev-admin
aws ec2 describe-instances --query 'Reservations[].Instances[].{Id,State,Type,Name}'
aws ec2 describe-volumes --query 'Volumes[]'
aws ec2 describe-snapshots --owner-ids self
aws ec2 describe-addresses --query 'Addresses[]'
aws ec2 describe-nat-gateways
aws elbv2 describe-load-balancers
aws ecr describe-repositories
aws cloudtrail describe-trails + get-trail-status (IsLogging=True ✅)
aws ce get-cost-and-usage --time-period Start=2026-05-01,End=2026-05-26
```

## Findings (state inventory)

| Resource | Detail | Action |
|---|---|---|
| EC2 kh-backend (t3.large, i-05d7af46d01436b96) | stopped 2026-05-17 | KEEP |
| EC2 kc-app (t3.medium, i-01ad56b0067d0213b) | stopped 2026-05-17 | KEEP |
| EC2 kc-app-fe (t3.small, i-05cfda7c6c60b683f) | stopped 2026-05-17 | KEEP |
| EBS 3 volumes (30+30+20=80 GB gp3) | in-use, attached stopped EC2 | KEEP |
| Snapshots | **0 — none auto-created during suspension** | N/A |
| EIP 13.213.229.164 | Assoc eipassoc-0d7e... → ALB ENI eni-0856ced86b6d62ca3 | **DELETE** |
| EIP 47.131.212.153 | Assoc eipassoc-0a5c... → ALB ENI eni-052583b1940f56493 | **DELETE** |
| EIP 52.221.161.175 (kitehub-kc-app-fe-eip) | Assoc → i-05cfda7c6c60b683f (EC2 kc-app-fe) | KEEP |
| ALB kitehub-alb (app/kitehub-alb/c9ece63c87ea7a88) | active, application | **DELETE** |
| ECR 10 repos | active (deploy artifacts) | KEEP (consider lifecycle Wave 9+) |
| CloudTrail kitehub-main | multi-region, IsLogging=True | KEEP |
| RDS | NONE (chưa provision) | N/A |
| NAT Gateways | NONE | N/A |

### Billing pre-mutation (May 1-26 2026)

| Service | Cost (USD) |
|---|---|
| AWS Data Transfer | -24.9993 (CREDIT) |
| ECR | +24.9893 |
| S3 | +0.0058 |
| ELB | +0.0023 |
| EC2 Compute | +0.0016 |
| **Net** | **-0.0003** (credit covers ECR) |

Next month projection (no credit): ALB ~$20 + EIPs 3×$3.6=$10.8 + EBS ~$4 + ECR ~$25 = ~$60/month burn while idle.

Post-cleanup projection: EIP 1×$3.6 + EBS ~$4 + ECR ~$25 + CloudTrail ~$0 = ~$33/month burn while idle.

## Verdict

Mutations safe to execute. Rationale:
- ALB serving 0 traffic 8 days, no production users
- 2 EIPs ALB-attached → auto-released when ALB deleted
- IaC declarations in `infrastructure/terraform-aws/*.tf` preserve resource definitions → terraform apply later re-creates
- State drift acceptable per user direction (temporary stop to save cost; rebuild later)
- CloudTrail still logging → mutation API calls audited

## Order of operations

```bash
# 1. Delete ALB (auto-disassociates EIPs from ENIs)
aws elbv2 delete-load-balancer --profile dev-admin --region ap-southeast-1 \
  --load-balancer-arn $(aws elbv2 describe-load-balancers --profile dev-admin --region ap-southeast-1 \
    --names kitehub-alb --query 'LoadBalancers[0].LoadBalancerArn' --output text)

# Wait ENI cleanup ~30s

# 2. Release 2 EIPs (now disassociated)
aws ec2 release-address --profile dev-admin --region ap-southeast-1 \
  --allocation-id $(aws ec2 describe-addresses --profile dev-admin --region ap-southeast-1 \
    --filters "Name=public-ip,Values=13.213.229.164" --query 'Addresses[0].AllocationId' --output text)
aws ec2 release-address --profile dev-admin --region ap-southeast-1 \
  --allocation-id $(aws ec2 describe-addresses --profile dev-admin --region ap-southeast-1 \
    --filters "Name=public-ip,Values=47.131.212.153" --query 'Addresses[0].AllocationId' --output text)
```

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| Delete ALB kitehub-alb | Agent (user-confirmed Option 2) | Cascade releases 2 ENIs |
| Release EIP 13.213.229.164 | Agent | Post-ALB-delete |
| Release EIP 47.131.212.153 | Agent | Post-ALB-delete |
| Post-mutation verify | Agent | Re-run describe-load-balancers + describe-addresses |
| GAP-612 Log update | Agent | Append cleanup outcome + revised AC |

## Prior actions verified

- `2026-05-12-wave-64-pre-apply-plan-investigation.md` (Wave 64 cutover)
- `2026-05-08-wave-43-44-bootstrap-apply.md` (initial provision)
- GAP-612 Log entries from 2026-05-17 (suspension) → 2026-05-25 (hold removed)

## Recommendations post-mutation

1. **Terraform IaC preserve:** Do NOT delete `.tf` files for ALB + EIPs — keep IaC declarations for rebuild
2. **Resume cost when needed:** `terraform apply` recreates ALB + EIPs (~15-20 min); DNS Cloudflare A records may need re-point if EIP IPs change
3. **Next cleanup pass** (if extended idle): ECR lifecycle policy `retain only 5 newest images` (~$15-20/month additional save) — Wave 9+ candidate
4. **Re-enable docker-build-push.yml** AFTER local RST verify PASS (separate PR #1802)

## References

- GAP-612 (AWS account suspension recovery)
- `agent-aws-access.md` v1.0.3 §4.3 Tier 3 destructive ops
- `pre-mutation-state-check.md` v1.2.0 §3 audit artifact mandate
- `aws-observability-first.md` v1.0.0 (CloudTrail IsLogging verified)
- User authorization: Option 2 confirmed AskUserQuestion 2026-05-25 mid-session

## Credit-stacking strategy (note inline 2026-05-25)

Current state (account 906286017800, expiry 2027-05-07):
- $100 AWS Free Tier auto credit
- 4× $20 Explore AWS task credits (Budget/RDS/EC2/Lambda tutorials) = $80
- **Total $180 active, $0 used (suspension froze burn 8 days)**

Burn post-cleanup ~$33/month idle → $180 covers ~5.5 tháng runway.

### Cannot transfer credits between accounts

AWS Educate (GitHub Student Pack default benefit) là **sandbox account tách biệt** — credits bind vào account đó tại lúc redeem, KHÔNG transfer sang 906286017800. Anti-pattern: tạo account #2 cùng identity để stack credits → duplicate detection ban (per GAP-612 §"Decisions locked Item 1").

### Stacking paths cho 906286017800 (account-specific)

| Priority | Program | Credit | Process | Status |
|---|---|---|---|---|
| 🔴 P0 | **GitHub Student Pack promo code** (if format = redeemable code, NOT Educate sandbox link) | $100 | Login other-email GitHub → check Student Pack AWS link → if promo code dạng `XXXX-XXXX`, redeem vào Billing → "Redeem credit" của account 906286017800 | Pending user manual check format (5-10 min) |
| 🔴 P0 | **AWS Activate Founder resubmit** | $1,000 | apply via https://aws.amazon.com/activate/founders → input account 906286017800 + business details | DENIED 2026-05-10 per GAP-459; pending resubmit POST kitehub.me HTTPS 200 live (post-RST) |
| 🟠 P1 | **Y Combinator Startup School** | ~$1,000 | Free 3-4h course + certificate → AWS code redeemable bất kỳ account | Untouched |
| 🟡 P2 | **AWS Cloud Quest** | $25-50 | Game-based learning 2-4h | Untouched |

### Anti-pattern reminder

- ❌ Mua AWS promo codes gray market — vi phạm AWS Service Terms §3.2 + risk permanent ban
- ❌ Tạo AWS account #2 cùng identity → duplicate detection (per GAP-612 §Day 4 hypothesis: AWS Trust & Safety preemptive suspend new-account pattern)
- ❌ Stack 2 Activate apps cùng business identity — AWS detect, 1 app/business/year

### Stack projection (REVISED 2026-05-25 — chính chủ only)

User clarified other-email Student Pack = identity khác (người khác). **DECISION: skip path đó** — TOS "for educational use by THE STUDENT" violation + identity mismatch surface signal nếu AWS Trust & Safety re-review trong 30-ngày post-restore observation window. Cost-benefit $100 không justify re-suspend risk on freshly-restored account.

```
$180 (current chính chủ)
+ $0   (SKIP other-person Pack — risk amplifier)
+ $1000 (Activate Founder resubmit POST-RST, chính chủ Thuy Duong + business)
+ $1000 (YC Startup School free 3-4h course, redeem any account)
+ $50  (Cloud Quest game-based, 2-4h)
─────────
$2,230 chính chủ = ~67 tháng burn ở $33/month idle
   OR  ~37 tháng burn ở $60/month active (post resume Phase 2.3)
```

### Identity matching mandate

Per GAP-612 Day 4 hypothesis: AWS Trust & Safety preemptive suspend pattern triggered bởi "new account + spike + identity inconsistencies". Stack credit programs từ identities khác = thêm signal điểm trong observation window.

**Conservative rules cho 906286017800 ≤30 ngày post-restore (2026-05-25 → 2026-06-25):**

| Action | Allowed? |
|---|---|
| Redeem promo codes chính chủ (Activate Founder approved, Cloud Quest completed) | ✅ |
| Redeem AWS event giveaway codes (Summit, Workshop attendee) | ✅ |
| Redeem partner program codes chính chủ (YC, accelerator referral) | ✅ |
| Redeem other-person Student Pack code | ❌ identity mismatch risk |
| Redeem gray-market AWS promo codes | ❌ TOS §3.2 violation |
| Create AWS account #2 cùng identity (Thuy Duong) | ❌ duplicate detection (GAP-612 §Decisions locked Item 1) |
| Use Educate sandbox account của user khác để fund 906286017800 | ❌ KHÔNG technically possible (sandbox locked) + identity mismatch nếu attempted workaround |

### Post-observation window (>30 ngày, after 2026-06-25)

If account 906286017800 stable + no Trust & Safety contact + Phase 2.3 progressing healthy → observation window low concern. But chính chủ discipline vẫn maintain — đã build legitimate stack ($1.5k+), không cần gray paths.

## Y Combinator Startup School — step-by-step playbook (P1 path)

**Caveat:** Knowledge cutoff January 2026. YC Startup School + AWS partnership thay đổi theo cohort/period; user verify UI có thể đã đổi tại thời điểm execute.

### Step 1 — Sign up (5-10 phút)

URL: `https://www.startupschool.org/`

- Click **"Sign up"** / **"Join Startup School"**
- Use email chính chủ `mvann1207@gmail.com` (matching identity Thuy Duong với AWS account 906286017800) — bắt buộc cho credit redemption sau
- Verify email link

### Step 2 — Create startup profile (5-10 phút)

Required fields:

| Field | Value |
|---|---|
| Company name | "KiteHub" hoặc "KiteHub Education Platform" |
| One-liner | "Multi-tenant SaaS for Vietnamese tutoring centers + K-12 schools — manage students, courses, attendance, grades, payments" |
| Founders | Thuy Duong (solo founder) |
| Location | Hanoi, Vietnam (hoặc city của user) |
| Stage | "Idea" / "Building MVP" / "Pre-launch beta" — match Phase 1 BETA scope |
| Industry | "Education / EdTech / SaaS" |
| Website | `https://kitehub.me` (placeholder content OK, SSL phải work) |

### Step 3 — Enroll cohort path

2 paths:
- **Self-paced Library** — light, faster access to Deals tab (recommend cho credit-stacking goal)
- **Active Cohort** — 10-week curriculum + weekly check-ins + peer group (recommend nếu muốn full mentorship)

Pick **self-paced** cho minimal effort path ~$1k AWS reward.

### Step 4 — Find AWS deal trong Deals tab

- Login → **"Deals"** / **"Founder Resources"** / **"Perks"** tab (UI name thay đổi theo period)
- Search "AWS" → tìm card "AWS Activate" / "AWS Credits"
- Typical offer 2024-2026: $1,000 - $5,000 AWS Activate Provider Code

### Step 5 — Claim AWS Activate Provider Code

- Click AWS deal → reveal code (dạng `ABC123XYZ...` ~20 chars)
- Code này dùng để apply qua AWS Activate Provider portal (NOT direct Billing redeem)

### Step 6 — Apply qua AWS Activate Portfolio

URL: `https://aws.amazon.com/activate/portfolio-signup` HOẶC `https://activate.aws.com/portfolio` (path thay đổi)

Form fields:

| Field | Value |
|---|---|
| **Provider Code** | (từ Step 5 YC code) |
| **AWS Account ID** | **906286017800** — bắt buộc đúng để credits map account này |
| Business legal name | Thuy Duong / KiteHub |
| Address + phone | Match GAP-612 case 177903869600100 documents |
| Business website | `https://kitehub.me` |
| Use case + stage | Match Step 2 profile |

### Step 7 — Wait approval

- Typical: 3-14 ngày
- Email confirm tới root email AWS account (mvann1207@gmail.com)
- Credits hiển thị Billing → Credits page (cùng nơi $180 hiện tại)

### Pre-requisites ready check

| Item | Status |
|---|---|
| Email chính chủ matching identity Thuy Duong | ✅ mvann1207@gmail.com |
| AWS account ID 906286017800 | ✅ active post-restore 2026-05-25 |
| Business website HTTPS 200 (`kitehub.me`) | ⚠️ Pending POST-RST + Phase 2.3 resume; placeholder content OK nhưng SSL phải work |
| LinkedIn profile founder | 🟡 Recommend — boost approval rate |
| Brief pitch deck / one-pager | 🟡 Optional |

### Risks + mitigations

| Risk | Mitigation |
|---|---|
| Credits amount thay đổi $500-$5k range theo period | Verify Deals tab tại apply moment; nhận bao nhiêu là bấy nhiêu |
| YC đã thay AWS partnership với program khác | Plan B: AWS Activate Founder direct apply (`https://aws.amazon.com/activate/founders/`) chính chủ guaranteed $1k |
| `kitehub.me` chưa live full content khi apply | Placeholder content OK; AWS verify website CHÍNH CHỦ chứ không evaluate business fundamentals at this stage |
| Identity mismatch nếu YC profile khác AWS account holder | Cùng email + cùng business name (KiteHub) cho cả 2 |
| Trust & Safety re-review trong observation window | Apply CHÍNH CHỦ (Thuy Duong + business identity match) — không amplify mismatch signals |

### Alternative fallbacks nếu YC AWS deal không match

| Program | URL | Reward | Effort |
|---|---|---|---|
| **AWS Activate Founder direct** (recommended fallback) | `https://aws.amazon.com/activate/founders/` | $1,000 chính chủ | 30-60 min apply + 7-14 ngày — resubmit POST-RST per GAP-459 |
| **Microsoft for Startups Founders Hub** | `https://startups.microsoft.com/` | $1,000 Azure + small AWS via partner | Apply, 1-2 tuần |
| **Stripe Atlas** | `https://stripe.com/atlas` | $5,000 AWS via Atlas portal | $500 Atlas fee — overkill cho student stage |

### Recommend execution sequence

```
TUẦN NÀY (2026-05-25 → 2026-05-31):
  1. YC Startup School signup (5-10 min) — test water
  2. Apply qua Deals tab nếu AWS code có (max 2h total light path)
  3. Cloud Quest game 2-4h cho $25-50 small wins

POST-RST (sau khi kitehub.me HTTPS 200, ~tuần sau):
  4. AWS Activate Founder direct resubmit (chính chủ $1k guaranteed, GAP-459 unblock)

EXTRA (nếu thời gian + UTC student card available):
  5. AWS Educate $200 với UTC student verify (sandbox tách prod)
```

Cross-references:
- GAP-459 AWS Activate Founder resubmit blocked by kitehub.me reachability
- GAP-612 §Day 4 Trust & Safety hypothesis (identity matching mandate)
- `agent-action-bias.md` §1 Part A (do-it-yourself paths only for user manual application)

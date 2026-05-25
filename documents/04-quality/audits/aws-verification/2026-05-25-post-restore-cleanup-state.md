---
title: AWS Verification — Post-restore cleanup (ALB + 2 unused EIPs)
status: in-progress
created: 2026-05-25
phase: wave-beta-readiness-8 parallel ops
gaps: [GAP-612]
---

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

---
title: AWS Verification — Wave 82 Bucket B post-apply
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 82
gaps: [GAP-565, GAP-566, GAP-567]
---

# AWS Verification Report — Wave 82 Bucket B post-apply

## Scope

Post-mutation Tier 1 read-only verify per `agent-aws-access.md` §5 sau khi terraform apply success cho Wave 82 Bucket B EC2 t3.small + 4 P0 mitigation infra.

**Run sequence:**

| Run | Outcome | Notes |
|---|---|---|
| #25914266227 (plan dry-run) | DESTRUCTIVE — 3 EC2 replaced | AMI drift — pivot |
| #25914464130 (plan dry-run, AMI pinned) | CLEAN — 9 add / 0 destroy | PR #1400 |
| #25914617070 (apply attempt 1) | FAILED at iam:TagInstanceProfile denied | PR #1401 |
| #25915425769 (apply attempt 2) | FAILED — STS session cached old perms | retry needed |
| #25915573484 (apply attempt 3) | ✅ **SUCCESS** — 11:33 UTC | new STS session got new perms |

## Commands run (Tier 1 read-only)

```bash
aws ec2 describe-instances --filters "Name=tag:Name,Values=kitehub-kc-app-fe"
aws ec2 describe-security-groups --group-ids sg-06ecdb1c30c2fecd1
aws cloudwatch describe-alarms --alarm-name-prefix kitehub-kc-app-fe
aws iam get-instance-profile --instance-profile-name kitehub-production-kc-app-fe
```

## Results

### EC2 instance — GAP-566 base infra

| Field | Value |
|---|---|
| InstanceId | `i-05cfda7c6c60b683f` |
| State | `running` |
| InstanceType | `t3.small` |
| PublicIpAddress | `54.179.70.37` |
| PrivateIpAddress | `10.0.0.84` |
| PublicDnsName | `ec2-54-179-70-37.ap-southeast-1.compute.amazonaws.com` |
| AMI | `ami-04a8a2b994a2a7176` (pinned per PR #1400) |

### Security Group — GAP-565 F6 mitigation ✅

| Port | Source | Description |
|:---:|---|---|
| 80 | 0.0.0.0/0 (IpRanges) | HTTP for certbot HTTP-01 fallback challenge and redirect to HTTPS |
| 443 | 0.0.0.0/0 (IpRanges) | HTTPS public FE traffic to nginx reverse proxy |
| 4700 | SG self-ref (UserIdGroupPairs) | Internal nginx to Next.js standalone for kiteclass-frontend - self-reference only no public |
| 4701 | SG self-ref (UserIdGroupPairs) | Internal nginx to Next.js standalone for kitehub-frontend - self-reference only no public |

✅ All 4 rules có ASCII description per `aws-sg-description-ascii.md`. Port 4700/4701 (Next.js standalone) restricted to SG self-reference (NOT public 0.0.0.0/0).

### CloudWatch alarms — GAP-566 + GAP-567 mitigations ✅

| Alarm | State | Metric | Threshold |
|---|:---:|---|:---:|
| `kitehub-kc-app-fe-memory-high` | OK | mem_used_percent | 85% |
| `kitehub-kc-app-fe-cert-expiry` | INSUFFICIENT_DATA | CertDaysToExpire | <30d |

Memory alarm OK = instance healthy. Cert-expiry INSUFFICIENT_DATA expected (cert chưa acquired; sẽ resolve sau Bucket B server bootstrap step certbot DNS-01 setup).

### IAM least-privilege attachment ✅

- `kitehub-production-kc-app-fe` instance profile created 2026-05-15T11:33:35Z
- Role attached: `kitehub-production-kc-app-fe`
- Policy scope: SSM Parameter Store read (cloudflare-api-token) + CloudWatch PutMetricData (KiteHub/FE namespace)

## Findings

### ✅ PASS — All 9 NEW resources created successfully

`aws_instance.kc_app_fe` / `aws_security_group.kc_app_fe` / `aws_iam_role.kc_app_fe` / `aws_iam_instance_profile.kc_app_fe` / `aws_iam_role_policy.kc_app_fe_certbot_metrics` / 2× `aws_iam_role_policy_attachment` / 2× `aws_cloudwatch_metric_alarm`.

### ✅ PASS — GAP-565 / 566 / 567 P0 mitigations embedded

- F6 SG ASCII descriptions per-rule (verified above)
- F7 memory alarm @ 85% armed; swapfile creation via user_data (verify in next step server bootstrap)
- F10 cert expiry alarm armed (will exit INSUFFICIENT_DATA state once certbot publishes metric)

### ⚠️ Pending — server not yet bootstrapped

User_data ran on instance launch (per ec2-kc-app.tf heredoc): install nginx + Node 20 + PM2 + certbot scaffolding. But:
- SSM Parameter `/kitehub/production/cloudflare-api-token` not yet populated → certbot DNS-01 will fail
- FE artifacts (kitehub-frontend + kiteclass-frontend) not yet built/deployed → PM2 has no apps to start
- Cert acquisition pending Cloudflare token

→ **Bucket B server bootstrap step is next USER ACTION** per runbook §Bucket B server bootstrap.

## Prior actions verified

| Action | Where verified |
|---|---|
| BE deploy v0.9.0-beta-staging.16 | Run #25913661225 completed/success (10:43 UTC) + smoke 12/14 + CORS preflight 4/4 |
| AMI pin (PR #1400) | Plan run #25914464130 (CLEAN 9 add / 0 destroy) |
| IAM TagInstanceProfile (PR #1401) | Policy verified post-PR: `aws iam get-role-policy ... | grep TagInstanceProfile` returns match |
| Bucket B drafts (PR #1398) | All artifacts on main: ec2-kc-app.tf, nginx-fe.conf, pm2-ecosystem.config.js, certbot-dns-01-setup.sh, runbook |

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| SSM Parameter put cho Cloudflare API token | USER ACTION | `aws ssm put-parameter --name /kitehub/production/cloudflare-api-token --type SecureString --value <TOKEN>` |
| SSM SendCommand bootstrap server | USER ACTION | Run `certbot-dns-01-setup.sh` + verify nginx running |
| FE build + rsync + PM2 start | USER ACTION | per runbook §Bucket C |
| DNS cutover | USER ACTION | per runbook §Bucket D |
| Dev walk-through 126 rows | USER ACTION | per runbook §Bucket H |

## Recommendations

1. **USER**: Cloudflare API token sẵn sàng? Get from https://dash.cloudflare.com/profile/api-tokens (template "Edit zone DNS", Zone Resources: kitehub.me).

2. **USER**: SSM put-parameter command:
```bash
aws ssm put-parameter \
  --profile dev-admin --region ap-southeast-1 \
  --name /kitehub/production/cloudflare-api-token \
  --type SecureString \
  --value '<CLOUDFLARE_TOKEN_VALUE>' \
  --description 'Wave 82 GAP-567 — Certbot DNS-01 challenge token'
```

3. **USER**: SSM SendCommand bootstrap server (per runbook §Bucket B server bootstrap detailed commands).

4. **Coordinator (post-bootstrap)**: verify certbot acquired wildcard cert `*.kitehub.me` → CertDaysToExpire alarm transitions from INSUFFICIENT_DATA to OK.

5. **Wave 82 plan §8 Log update** post-Bucket-B-success: append entry confirming EC2 provisioning DONE 2026-05-15 11:33 UTC.

## References

- Pre-apply audit: `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-pre-apply.md`
- Plan run #25914464130 (clean): https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25914464130
- Apply success run #25915573484 (attempt 3): https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25915573484
- PRs: #1396 (Bucket F + A) + #1397 (OTel CVE) + #1398 (Bucket B drafts) + #1399 (GAP-570/571 docs) + #1400 (AMI pin) + #1401 (IAM TagInstanceProfile)
- ADR-031: FE self-host AWS EC2 architecture decision
- Runbook: `documents/05-guides/deploy/fe-self-host-runbook.md`
- Gaps: GAP-565 / GAP-566 / GAP-567 / GAP-568 (Wave 82 Bucket B prerequisites)
- Rules applied: `pre-mutation-state-check.md` §3 / `release-fix-retry-budget.md` §4 pivot (AMI replacement avoidance) / `terraform-apply-retry-reconfirm.md` §5 (STS session retry no-fix-needed) / `aws-observability-first.md` (CloudTrail baseline) / `concurrent-production-mutation-ops.md` §3.5 (no in-flight serialize)

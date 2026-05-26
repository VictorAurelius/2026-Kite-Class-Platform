---
title: AWS Verification — Wave aws-restore-1 Phase C2 pre-apply (enable_alb=false + SG migration)
status: complete
created: 2026-05-26
audience: dev
audit_type: aws-verification
scope: terraform-apply.yml dry_run=true expected diff per Phase C user-approved Path B architecture pivot
wave: aws-restore-1
gaps: [GAP-612, GAP-693]
---

# AWS Verification — Wave aws-restore-1 Phase C2 pre-apply

## Scope

Pre-apply audit for terraform-apply.yml run with new defaults (`var.enable_alb=false` per Phase C PR #1854 merged 2026-05-26). Per `pre-mutation-state-check.md` §3 mandate + `dev-authorized-terraform-trigger.md` §2.2 gate.

## Pre-apply state (Tier 1 read-only commands)

```bash
aws sts get-caller-identity --profile dev-admin
aws cloudtrail get-trail-status --name kitehub-main --profile dev-admin --query IsLogging
aws ec2 describe-instances --profile dev-admin --region ap-southeast-1
aws rds describe-db-instances --profile dev-admin --region ap-southeast-1 --db-instance-identifier kitehub-postgres
aws elbv2 describe-load-balancers --profile dev-admin --region ap-southeast-1
aws ec2 describe-security-groups --profile dev-admin --region ap-southeast-1 --group-ids sg-0b9d8e37f2bf977b7
```

## Findings

### Account + observability
- ✅ Account `906286017800` ACTIVE
- ✅ CloudTrail `kitehub-main` IsLogging=True (`aws-observability-first.md` baseline)

### Compute layer
- ✅ EC2 3/3 running post Phase A: kh_backend (i-05d7af46d01436b96, 10.0.0.129) + kc_app (i-01ad56b0067d0213b, 10.0.0.155) + kc_app_fe (i-05cfda7c6c60b683f, 10.0.0.84 + EIP 52.221.161.175)
- ✅ kh_backend Docker stack 7/7 HEALTHY (gateway/admin/branding/email/subscription/rabbitmq/redis)
- ✅ kc_app_fe nginx active + serving kitehub.me HTTPS 200

### Data layer
- ✅ RDS `kitehub-postgres` available (Phase B restore) — postgres 15.17 endpoint `kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com:5432`

### Ingress layer (pre-Phase C2 state)
- 🔴 ALB: 0 load balancers (deleted Wave br-8) — terraform state still references resources
- 🔴 ALB SG `sg-02dfda0973b34a130` orphan — exists in AWS but ALB removed
- 🟡 kh_backend SG `sg-0b9d8e37f2bf977b7` allows 80/8080/3000/8081-8087 from ALB SG (orphan reference) — `aws_security_group.ec2_app` resource
- ❌ kc_app_fe SG → kh_backend ingress NOT present (Path B requirement)
- ❌ `api.kitehub.me` DNS not present — HTTPS 530 (CF no-origin-reachable)
- ✅ apex `kitehub.me` HTTPS 200 via kc_app_fe EIP + nginx + PM2 Next.js (manual CF DNS pre-existing)

## Expected terraform plan diff (per Phase C PR #1854 merged)

| Resource | Action | Reason |
|---|---|---|
| `aws_lb.main[0]` | DESTROY | `enable_alb=false` → count=0 |
| `aws_lb_listener.http[0]` | DESTROY | ALB child resource |
| `aws_lb_listener.https[0]` | DESTROY | ALB child resource |
| `aws_lb_target_group.kh_backend[0]` | DESTROY | ALB child |
| `aws_lb_target_group_attachment.kh_backend[0]` | DESTROY | ALB child |
| `aws_security_group.alb[0]` | DESTROY | `enable_alb=false` → count=0 |
| `aws_security_group.ec2_app` | UPDATE in-place | Remove 10 ingress rules from ALB SG; ADD 3 ingress rules from kc_app_fe SG (ports 80/443/8080) |
| `aws_route53_record.apex_alb[0]` | (N/A — `manage_route53_zone=false`) | Skip |

**Expected summary:** `0 to add, 1 to change, ~6 to destroy`

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| `gh workflow run terraform-apply.yml --ref main -f confirm=APPLY -f dry_run=true` | Coordinator inline (Tier 2 user authorization received via "claude trigger Phase C2") | Plan-only |
| Concurrent op check | Per `concurrent-production-mutation-ops.md` §4 + `dev-authorized-terraform-trigger.md` §2.1 | `gh run list --status in_progress` |
| Review plan match expected diff above | Per `pre-mutation-state-check.md` §3.5 plan-vs-predicted reconciliation | Quote dry-run summary |
| `dry_run=false` apply trigger | Separate Tier 3 user authorization required (per `terraform-apply-retry-reconfirm.md`) | After plan review |

## Recommendations

1. Pre-apply concurrent op check — verify zero in-progress workflows
2. Trigger `dry_run=true` first (no targets — full plan to catch all impacts)
3. STOP at plan output — write reconciliation table + verify destroyed resources match Phase C scope only (no surprise touches to RDS, EC2, secrets, IAM)
4. If plan matches → ask user authorization for `dry_run=false` apply
5. Post-apply Tier 1 verify: `aws elbv2 describe-load-balancers` (expect empty) + `aws ec2 describe-security-groups --group-ids sg-0b9d8e37f2bf977b7` (verify new kc_app_fe SG ingress active)

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-26-aws-restore-1-production-stack-recovery.md`
- Phase C PR: #1854 (merged 2026-05-26)
- Phase B audit artifact: (RDS restore Phase B coordinator-inline; standalone audit artifact deferred to Wave closure §7 per agreed scope)
- Cross-reference: GAP-612 (PARTIAL 30% → projected DONE post-Wave aws-restore-1 closure) + GAP-693 (PARTIAL — SOP runbook deferred Wave aws-rebuild-sop-1)

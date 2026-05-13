---
title: AWS Verification — GAP-501 pre-apply (remove kc_app ALB TG + listener rule + attachment)
status: complete
created: 2026-05-13
phase: phase-1-beta
wave: 68
gaps: [GAP-501]
---

# AWS Verification Report — GAP-501 pre-apply (remove kc_app ALB TG)

## Scope

About to mutate: `aws_lb_target_group.kc_app`, `aws_lb_target_group_attachment.kc_app`, `aws_lb_listener_rule.kc_app_default` — **3 resources DESTROY**. Triggered after PR merge via user `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false` per `release-deploy-standard.md` §9.

Why: Post-Vercel pivot drift — FE moved off kc_app EC2 :3000 to Vercel CDN 2026-05-07; ALB priority-100 rule still routing `/`, `/auth/*`, `/dashboard/*` etc. to dead TG → HTTP 502 on production for those paths via `api.kitehub.me`.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Identity + region
aws sts get-caller-identity   # arn:aws:iam::906286017800:user/solo-dev-admin
export AWS_DEFAULT_REGION=ap-southeast-1

# Smoke probe — surface symptom
curl -sS -o /dev/null -w "%{http_code}\n" -L https://api.kitehub.me/actuator/health  # 200
curl -sS -o /dev/null -w "%{http_code}\n" -L https://api.kitehub.me/                  # 502 ← symptom
curl -sS -o /dev/null -w "%{http_code}\n" -L https://api.kitehub.me/auth/login        # 502
curl -sS -o /dev/null -w "%{http_code}\n" -L https://api.kitehub.me/dashboard         # 404

# ALB topology
aws elbv2 describe-target-groups \
  --query 'TargetGroups[?contains(TargetGroupName, `kitehub`)].{Name:TargetGroupName,ARN:TargetGroupArn,Port:Port}' \
  --output table
aws elbv2 describe-target-health --target-group-arn <kc_app_tg_arn> \
  --query 'TargetHealthDescriptions[].{Id:Target.Id,Port:Target.Port,State:TargetHealth.State,Reason:TargetHealth.Reason}'
aws elbv2 describe-listeners --load-balancer-arn <kitehub-alb-arn> \
  --query 'Listeners[].{Arn:ListenerArn,Port:Port}'
aws elbv2 describe-rules --listener-arn <https-listener-arn> \
  --query 'Rules[].{Priority:Priority,Action:Actions[0].Type,TargetGroup:Actions[0].TargetGroupArn,Conditions:Conditions}'

# Instance + SSM reachability (verify kc_app EC2 stays)
aws ec2 describe-instances --instance-ids i-01ad56b0067d0213b \
  --query 'Reservations[].Instances[].{State:State.Name,LaunchTime:LaunchTime,Type:InstanceType,Tags:Tags[?Key==`Name`].Value|[0]}'
aws ssm describe-instance-information --filters "Key=InstanceIds,Values=i-01ad56b0067d0213b" \
  --query 'InstanceInformationList[].{Id:InstanceId,PingStatus:PingStatus,LastPing:LastPingDateTime}'
```

## Findings

### Current ALB topology (live, pre-apply)

| Resource | Identifier | State |
|---|---|---|
| ALB | `kitehub-alb` | active |
| Listener HTTPS | port 443 | active, default → `kh_backend` TG |
| Listener HTTP | port 80 | redirect → 443 |
| Listener rule priority 100 | path-pattern `/ /_next/* /static/* /auth/* /dashboard/*` | forward → `kitehub-kc-app-tg` ← **TO REMOVE** |
| TG `kitehub-kh-backend-tg` | port 8080, instance i-05d7af… | **healthy** |
| TG `kitehub-kc-app-tg` | port 3000, instance i-01ad56… | 🔴 **unhealthy** (`Target.FailedHealthChecks`) ← **TO REMOVE** |
| EC2 `kitehub-kc-app` | i-01ad56b0067d0213b, t3.medium, running | KEEP (BE services) |
| EC2 `kitehub-kh-backend` | i-05d7af46d01436b96 | unaffected |

### Real changes (terraform destroy — must verify intent)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `aws_lb_listener_rule.kc_app_default` | destroy | Post-Vercel pivot drift; rule routes to dead TG | Removing rule = all paths fall through HTTPS listener default → `kh_backend` TG. Backend Spring Boot returns 404 for FE paths (acceptable: api.kitehub.me is API-only; FE on Vercel). **Net behavior change: 502 → 404 on `/`, `/auth/*`, `/dashboard/*`** (improvement) |
| 2 | `aws_lb_target_group_attachment.kc_app` | destroy | TG no longer attached to instance | EC2 i-01ad56 remains; only deregistered from this TG. No service interruption (TG already returns 502; not in use) |
| 3 | `aws_lb_target_group.kc_app` | destroy | TG itself removed | TG ARN no longer exists. Any code referencing `kitehub-kc-app-tg` ARN would fail — verified zero refs in terraform (only ec2.tf defined it) and zero refs in workflows (`grep -rn "kc-app-tg" .github/ infrastructure/`) |

### Phantom changes

None expected. No `lifecycle.ignore_changes` or hidden-attribute drift on these 3 resources.

### Verdict

Real changes are **intentional** + correct + reduce production noise (502 → 404). No data at risk (TG had no live traffic — was dead since Vercel pivot). EC2 itself preserved per GAP-447 BE workload requirement.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| Vercel pivot decision | 2026-05-07 | GAP-447 §"Root Cause" + GAP-411 sizing matrix note |
| kc_app EC2 right-sized to t3.medium | 2026-05-08 | GAP-447 PARTIAL 75% — kh_backend DONE, kc_app DONE per CSV |
| Workflows referencing kc_app TG | n/a | `grep -rn "kc-app-tg" .github/` → zero refs |
| Outputs / IAM referencing TG | n/a | `grep -rn "aws_lb_target_group.kc_app\|kc-app-tg" infrastructure/terraform-aws/` → 0 refs after this PR's edits |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Merge PR (terraform edit + gap file + this audit) | Agent → human review | docs-only path scope no — touches `.tf` → reviewer-checklist + CI required, not auto-merge |
| **Concurrent op check** | Agent | Active workflows touching same resources before re-trigger? Confirm zero (per `concurrent-production-mutation-ops.md` §4) at apply time |
| `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=true` | User | dry_run first — confirm plan = exactly 3 destroys, 0 changes, 0 adds |
| `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false` | User | only after dry_run plan matches expectation |
| Post-apply smoke verify | Agent | `curl https://api.kitehub.me/` should return 404 (not 502); TG should return `TargetGroupNotFound` |

## Recommendations

1. **Apply via dry_run=true first** — confirm plan output matches "3 to destroy, 0 to add, 0 to change" exactly. If anything else surfaces, abort.
2. **Post-apply verification commands:**
   ```bash
   curl -sS -o /dev/null -w "%{http_code}\n" -L https://api.kitehub.me/           # expect 404 (NOT 502)
   curl -sS -o /dev/null -w "%{http_code}\n" -L https://api.kitehub.me/auth/login  # expect 404
   curl -sS -o /dev/null -w "%{http_code}\n" -L https://api.kitehub.me/actuator/health  # expect 200 (unchanged)
   aws elbv2 describe-target-groups --names kitehub-kc-app-tg --region ap-southeast-1   # expect TargetGroupNotFound
   ```
3. **Flip GAP-501 to DONE** post-verification + sync `gap-status.csv` per `post-merge-sync-completeness.md` Rule 17.
4. **Watch-for:** any deploy-production.yml workflow that smoke-checked `https://api.kitehub.me/` previously would have passed because 502≠connection-error; ensure smoke step asserts 200 or 404 only (likely already does — `api.kitehub.me/actuator/health` is the existing health probe).

## References

- PR: <link added at PR creation>
- Branch: `fix/GAP-501-kc-app-tg-drift-post-vercel-pivot`
- Terraform diff: `infrastructure/terraform-aws/ec2.tf` (3 resources removed, 2 explanatory comments added)
- Related rules: `pre-mutation-state-check.md` v1.1.0, `release-deploy-standard.md` §9, `agent-aws-access.md` §2.1, `concurrent-production-mutation-ops.md` §4
- Related gaps: GAP-501 (this fix), GAP-447 (kc_app sizing context)

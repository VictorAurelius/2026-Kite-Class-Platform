---
title: AWS Verification — Wave 65 Bucket E pre-apply (GAP-483 EC2 user_data)
status: complete
created: 2026-05-12
phase: phase-1-beta
wave: 65
gaps: [GAP-483]
---

# AWS Verification Report — Wave 65 Bucket E pre-apply

## Scope

Pre-mutation state-check trước khi trigger `terraform-apply.yml dry_run=false` cho Bucket E (GAP-483):
- Bucket E ec2.tf đã merge main (commit `a221d776`, PR #1208)
- Change: `local.ec2_user_data` += `dnf install -y git` + `git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git /opt/kite-prod`
- `user_data_replace_on_change = false` — KHÔNG force replacement on user_data change alone

**Per `pre-mutation-state-check.md` v1.1.0 §3 — required before mutation.**

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
AWS_PROFILE=kite-readonly AWS_DEFAULT_REGION=ap-southeast-1 \
  aws ec2 describe-instances --filters Name=tag:Project,Values=Kite \
    --query 'Reservations[].Instances[].[InstanceId,State.Name,Tags[?Key==`Name`].Value|[0],LaunchTime]'

gh workflow run terraform-apply.yml -f version=main -f confirm=APPLY -f dry_run=true
gh run view <id> --log  # plan output review
```

## Findings — Bucket E ec2.tf scope

### Real changes expected (per `user_data_replace_on_change = false`)

`user_data` text change ALONE does NOT trigger EC2 replacement. Resources affected:
- `local.ec2_user_data` — string content drift detected by terraform
- `aws_instance.kh_backend.user_data` — **change in-place** (no replacement since `replace_on_change = false`)
- `aws_instance.kc_app.user_data` — **change in-place** (same)

⚠️ **In-place user_data update does NOT re-execute user_data on existing EC2.** New user_data only runs on NEW EC2 launches (AMI bump, instance class change, manual termination).

**Implication:** Bucket E's git install + repo clone will NOT take effect on existing EC2. Existing EC2 keeps the Wave 64 manual SSM bootstrap state. The bootstrap "automation" benefit kicks in only on NEXT EC2 replacement (future AMI bump etc.).

### Phantom changes (terraform state metadata only)

None expected.

### Verdict

✅ **Safe to apply** — non-disruptive:
- Existing EC2 keeps current state (no replacement, no downtime)
- New user_data takes effect prospectively on future EC2 replacement
- GAP-483 AC#1 (user_data updated) satisfied by terraform apply landing the change
- GAP-483 AC#2/#3 (verify on new EC2 boot) defer to next AMI bump / instance refresh

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| Wave 64 manual SSM bootstrap on existing EC2 | 2026-05-12 | `documents/04-quality/audits/aws-verification/2026-05-12-wave-64-pre-apply-plan-investigation.md` |
| Bucket E ec2.tf edit | 2026-05-12 | PR #1208 commit `a221d776` |
| EC2 healthy state | 2026-05-12 | `aws ec2 describe-instances --filters Name=tag:Project,Values=Kite` → `kh_backend=running`, `kc_app=running` |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Trigger terraform-apply.yml dry_run=false confirm=APPLY | Agent on behalf of user | Per user explicit auth "C, làm giúp tôi" 2026-05-12; first-attempt scope |
| Verify post-apply: terraform state shows new user_data hash | Agent verification | `terraform state show aws_instance.kh_backend` |
| Trigger deploy-production.yml v0.9.0-beta-staging.10 confirm=DEPLOY | Agent on behalf of user | Same explicit auth |
| Verify HTTPS api.kitehub.me/actuator/health = 200 | Agent verification | `curl -sI` |

## Recommendations

1. **Apply now** — non-disruptive in-place update; no rollback needed since `user_data_replace_on_change = false`
2. **Post-apply:** trigger deploy-production.yml to validate Bucket D OTel fix on existing EC2 (image-only update via SSM, doesn't depend on user_data)
3. **Watch-for items:**
   - Terraform plan output has NO unexpected `must be replaced` lines (only `~ user_data` in-place)
   - Plan output has NO IAM/secrets/RDS changes (out-of-scope for Bucket E)
4. If plan shows replacement (unexpected) → HOLD; review against this audit; re-confirm with user

## References

- Workflow run: https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25720960219 (dry_run plan)
- Gap: GAP-483
- PR: #1208 (Bucket E merge)
- Rules applied: `pre-mutation-state-check.md` v1.1.0 §1.5 + §3 + `release-deploy-standard.md` §9 + `terraform-apply-retry-reconfirm.md`

---

## Post-apply incident (2026-05-12 — concurrent ops conflict)

**Verified outcome:**
- Terraform apply completed 07:52:06 — `Apply complete! Resources: 0 added, 2 changed, 0 destroyed`
- 2 EC2 user_data updated in-place ✅
- New EC2 LaunchTimes: `kc_app` 07:51:39, `kh_backend` 07:51:49 — both `running`

**Unexpected: AWS in-place user_data update requires stop→ModifyInstanceAttribute→start** (per `EC2.ModifyInstanceAttribute` docs — user_data attribute can only be set on stopped instance). Terraform did this implicitly during apply window 07:51:08 → 07:52:06.

**Conflict with concurrent deploy-production.yml:**
- 07:50:41 deploy-production.yml queued (within 22s of terraform-apply trigger)
- 07:51:12 SSM SendCommand on `i-00505094277deda29` (kh_backend) — deploy-prod.sh START
- 07:51:13 ECR login OK
- 07:51:15 fetch-secrets.sh START (`aws secretsmanager get-secret-value`)
- 07:51:20 SSM Failed `Terminated / exit status 143` (SIGTERM) — terraform stop EC2 mid-fetch
- 07:51:49 EC2 back up (post-restart)
- Workflow poll loop continued showing `Status=InProgress` for 15 attempts (~2.5 min) before noticing actual failure → observability gap

**Root causes:**
1. **Concurrency conflict** — agent triggered terraform-apply + deploy-production within 22s on same EC2. SIGTERM from terraform stop killed running SSM command.
2. **Tooling visibility gap** — workflow polls SSM `Status` only; no CloudWatch streaming. Underlying command Failed at 7s but poll didn't surface for 2.5 min.

**Rules filed same PR per `incident-to-rule-pipeline.md` 5-stage:**
- ✅ `concurrent-production-mutation-ops.md` v1.0.0 — serialize mutation ops on shared production resource
- ✅ `release-fix-retry-budget.md` v1.1.0 — added §4 row "Tooling visibility gap" + §5 row "Tooling-fix-then-retry"
- ✅ GAP-491 filed — Path A CloudWatch streaming (BLOCKS next deploy retry)
- ✅ Memory `feedback_concurrent_mutation_ops_conflict.md` paired

**Next steps for deploy retry:**
1. GAP-491 Phase 1 (terraform CloudWatch log group + IAM) — apply
2. GAP-491 Phase 2 (workflow_dispatch deploy-production.yml update) — merge
3. Retry deploy with `RELEASE_RETRY_TOOLING_FIXED:` trailer citing GAP-491 fix PR
4. Verify CloudWatch streaming shows real-time stdout
5. Verify https://api.kitehub.me/actuator/health 200

**Cost of the miss (incident):**
- 1 wasted SSM command + ~2.5 min poll-loop confusion
- 1 cancel + re-trigger cycle (also cancelled before re-running per discipline)
- ~30 min rule-creation + audit-extension work this session

**Counterfactual cost-save with rules in place:**
- Serialize → no SIGTERM conflict
- CloudWatch streaming → instant failure detection
- Combined savings ~10-15 min wall-clock + future deploy attempts safe

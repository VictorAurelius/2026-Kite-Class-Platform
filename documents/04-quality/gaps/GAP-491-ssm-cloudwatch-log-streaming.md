# GAP-491: SSM command CloudWatch log streaming (deploy-production.yml visibility)

**Status:** 🟢 DONE 2026-05-12 — Phase 1+2+3 complete; live streaming verified on deploy run 25748003956
**Priority:** 🔴 P0 BLOCKING (next deploy retry blocked per `release-fix-retry-budget.md` v1.1.0 §4 row "Tooling visibility gap" — MUST land before next deploy attempt)
**Domain:** DevOps / Observability
**Found:** 2026-05-12 (Wave 65 deploy incident — SSM Status=InProgress for 15 poll attempts while command Failed at 7s)
**Affects:** Every deploy attempt via `deploy-production.yml` until fixed; future SSM-based ops will hit same visibility gap

## Problem

`.github/workflows/deploy-production.yml` uses `aws ssm send-command` WITHOUT `--cloud-watch-output-config`. SSM captures stdout/stderr only AT END of command execution. While running, poll loop sees `Status=InProgress` with no incremental output.

2026-05-12 incident — concrete cost:
- SSM command Failed at 7.88s with `Terminated / exit status 143`
- Workflow poll showed `Status=InProgress` for 15 attempts (2.5 min) before noticing
- No diagnostic available until command terminal state visible

Per `release-fix-retry-budget.md` v1.1.0 §4 — Tooling visibility gap = STOP retry, fix observability FIRST.

## Proposed Fix — Path A (CloudWatch streaming)

### Phase 1 — Terraform (CloudWatch log group + IAM)

1. New CloudWatch log group `/aws/ssm/kite-deploy`:
   ```hcl
   # infrastructure/terraform-aws/cloudwatch.tf (or extend existing)
   resource "aws_cloudwatch_log_group" "ssm_kite_deploy" {
     name              = "/aws/ssm/kite-deploy"
     retention_in_days = 7
     tags              = { Project = "Kite", Environment = "production" }
   }
   ```
2. IAM policy extension on EC2 instance profile (`kitehub-ec2-instance-profile` per existing convention):
   ```hcl
   # logs:CreateLogStream + logs:PutLogEvents on /aws/ssm/kite-deploy
   ```
3. Apply terraform → log group exists + IAM ready.

### Phase 2 — Workflow update (`deploy-production.yml`)

Modify `send-command` step:
```yaml
COMMAND_ID=$(aws ssm send-command \
  --region "${AWS_REGION}" \
  --instance-ids "${INSTANCE_ID}" \
  --document-name "AWS-RunShellScript" \
  --comment "Deploy ${KITE_VERSION}" \
  --timeout-seconds 600 \
  --cloud-watch-output-config CloudWatchLogGroupName=/aws/ssm/kite-deploy,CloudWatchOutputEnabled=true \
  --parameters commands="sudo KITE_VERSION=${KITE_VERSION} bash /opt/kite-prod/scripts/deploy-prod.sh" \
  --query "Command.CommandId" --output text)
```

Extend poll loop to ALSO tail CloudWatch logs:
```yaml
- name: Tail SSM CloudWatch logs in background
  run: |
    aws logs tail /aws/ssm/kite-deploy --since 0s --follow &
    echo $! > /tmp/tail.pid
- name: Poll SSM command status (up to 8 min)
  # ... existing poll loop
- name: Stop log tail
  if: always()
  run: kill $(cat /tmp/tail.pid) 2>/dev/null || true
```

Per AWS docs, SSM agent streams stdout/stderr to CloudWatch in 10s chunks during execution → poll loop sees real progress.

### Phase 3 — Verify

1. Run dry-deploy on existing staging.10 image (no-op deploy)
2. Observe CloudWatch log group receives stdout chunks every ~10s
3. Workflow log shows interleaved poll status + script output

## Acceptance Criteria

- [x] CloudWatch log group `/aws/ssm/kite-deploy` created via terraform — applied 2026-05-12 (PR #1216 + #1217)
- [x] EC2 instance profile has `logs:CreateLogStream` + `logs:PutLogEvents` — covered by existing `CloudWatchAgentServerPolicy` attachment (`iam.tf:34-37`)
- [x] `deploy-production.yml` send-command includes `--cloud-watch-output-config CloudWatchLogGroupName=/aws/ssm/kite-deploy,CloudWatchOutputEnabled=true` (Phase 2)
- [x] Poll loop interleaves with `filter-log-events --start-time` query every 10s + deploy OIDC role has `logs:FilterLogEvents` perm (Phase 2)
- [x] Verified on retry deploy run 25748003956: workflow log shows `│ <stdout>` lines interleaved with `Attempt N/48: Status=InProgress` — ECR login, secrets fetch, docker pull progress, container start all streamed live (Phase 3)
- [x] Bucket E (terraform user_data update) — apply 25747258524 succeeded standalone (no concurrent deploy); validates `concurrent-production-mutation-ops.md` serialization rule

## Effort estimate

~30-60 phút (Phase 1 + 2). Phase 3 verify = same retry deploy that's currently blocked.

## Related

- **Origin:** 2026-05-12 Wave 65 deploy incident — `documents/04-quality/audits/aws-verification/2026-05-12-wave-65-bucket-e-pre-apply.md` extension
- **Blocking:** Next deploy attempt (per `release-fix-retry-budget.md` v1.1.0 §4 row "Tooling visibility gap")
- **Sister rule (paired same PR):** `concurrent-production-mutation-ops.md` v1.0.0 — covers concurrency conflict that masked tooling gap
- **Reference:** `.claude/rules/release-fix-retry-budget.md` v1.1.0 §5 exception row "Tooling-fix-then-retry" with override trailer `RELEASE_RETRY_TOOLING_FIXED:`
- **Reference:** `release-deploy-standard.md` §9 (deploy execution = human-triggered workflow_dispatch)

## Log

- **2026-05-12 (Phase 3 — DONE):** Deploy retry run 25748003956 triggered with `RELEASE_RETRY_TOOLING_FIXED: GAP-491 PR-#1218` semantics. CloudWatch streaming **verified live** — workflow log shows `│ [2026-05-12T16:32:06Z] ==================== deploy-prod.sh START ====================` + ECR login + secrets fetch + 14 image-pull progress lines + container start status table all interleaved with `Attempt N/48: Status=InProgress` polls. Visibility gap CLOSED. Deploy itself failed for unrelated reasons (containers crash-restart loop on first deploy — Spring Boot startup config issue, NOT visibility); separate follow-up gap to be filed.
- **2026-05-12 (Phase 2):** `deploy-production.yml` adds `--cloud-watch-output-config` to send-command + poll loop interleaves `filter-log-events --start-time` query every 10s (timestamp-tracked). `iam.tf` extends `github_deploy_inline` policy with `logs:FilterLogEvents` + `logs:DescribeLogStreams` scoped to `/aws/ssm/kite-deploy`. terraform validate PASS + YAML parse PASS. Status remains 🟡 PARTIAL until IAM applied + retry deploy verifies live streaming (Phase 3).
- **2026-05-12 (Phase 1):** Terraform `aws_cloudwatch_log_group.ssm_kite_deploy` added to `cloudwatch.tf` (retention 7d). IAM coverage verified — `CloudWatchAgentServerPolicy` already attached to EC2 role (`iam.tf:34-37`) grants needed `logs:*` perms on all log groups. Status → 🟡 PARTIAL; pending human-triggered `terraform-apply.yml` + Phase 2 workflow update.
- **2026-05-12:** Filed after Wave 65 staging.10 deploy attempt failed silently (SSM Status=InProgress for 15 poll attempts while command actually Failed at 7s). User-flagged tooling gap + asked rule to prevent recurrence. P0 BLOCKING because next deploy retry MUST have observability shipped per `release-fix-retry-budget.md` v1.1.0 §4.

---
title: AWS Verification — Wave 70 Bucket D pre-apply (kh_backend t3.medium → t3.large)
status: complete
created: 2026-05-13
phase: wave-70-gap-502
wave: 70
gaps: [GAP-502, GAP-447]
---

# AWS Verification — Wave 70 Bucket D pre-apply

## Scope

Wave 70 Bucket D — `kh_backend_instance_type` variable change `t3.medium` → `t3.large` (PR #1261 merged 2026-05-13 08:46:25Z). About to trigger `terraform-apply.yml workflow_dispatch dry_run=false confirm=APPLY` để upsize EC2 `i-05d7af46d01436b96`. Per `pre-mutation-state-check.md` §3, this audit artifact must exist BEFORE mutation.

Rule references:
- `concurrent-production-mutation-ops.md` — no in-flight mutation on same EC2
- `agent-aws-access.md` §4.3 — human-triggered workflow_dispatch + confirm=APPLY (carve-out allowed)
- `terraform-apply-retry-reconfirm.md` — first apply approved by user this turn ("claude làm luôn 4 bước"); retry needs re-confirm
- `aws-observability-first.md` — CloudTrail IsLogging=true verified at session start
- `aws-sg-description-ascii.md` — N/A (no SG description change in this apply)

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
aws sts get-caller-identity --profile dev-admin
# → arn:aws:iam::906286017800:user/solo-dev-admin ✅

AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=kitehub-kh-backend" \
  --query 'Reservations[].Instances[].[InstanceId,InstanceType,State.Name,LaunchTime]'
# → i-05d7af46d01436b96 / t3.medium / running / 2026-05-13T01:00:41Z ✅

gh run list --workflow=terraform-apply.yml --limit 3
# → 3 completed/success — NO in-flight ✅

gh run list --workflow=deploy-production.yml --limit 3
# → completed — NO in-flight ✅
```

## Findings

### Real changes (per Wave 70 Bucket D scope)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `aws_instance.kh_backend` | **update** (`instance_type` t3.medium → t3.large) | GAP-502 OOM evidence (11 container die/1h on t3.medium); GAP-447 sizing assumption invalidated; user-directed upsize +$30/mo | EC2 stop-modify-start window ~3-5 min — production unavailable during window. Pre-launch Phase 1 BETA: no real users; acceptable. Container state lost on restart (acceptable — services auto-restart) |

### Phantom updates (lifecycle ignore_changes — non-functional)

Per GAP-450 v1.0.x investigation, plan likely also shows phantom updates on:
- `random_password.*` — `ignore_changes` lifecycle; no real rotation
- `aws_secretsmanager_secret_version.*` — metadata refresh

These are non-functional and expected.

### Verdict

Safe to apply. Pre-launch Phase 1 BETA state (no real users, currently thrashing per GAP-502); upsize is the documented escalation path in `kh_backend_instance_type` variable description; +$30/mo cost acceptable per user explicit approval; downsize evaluation tracked separately post-launch.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| Wave 67 production seed | 2026-05-11 | ROADMAP Wave 67 entry |
| Wave 68 verification + kc_app drift fix | 2026-05-12 | ROADMAP Wave 68 entry |
| Wave 69 audit-of-trust → GAP-502 filed | 2026-05-13 06:47Z | PR #1256 |
| Wave 70 plan merged | 2026-05-13 08:32Z | PR #1258 |
| Wave 70 Bucket D code merged | 2026-05-13 08:46Z | PR #1261 |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| `gh workflow run terraform-apply.yml -f dry_run=true -f confirm=APPLY` | Agent (this session) | Surface plan output for review BEFORE dry_run=false |
| `gh workflow run terraform-apply.yml -f dry_run=false -f confirm=APPLY` | Agent (this session) | Triggered after plan review |
| Post-apply EC2 verification | Agent (this session) | `aws ec2 describe-instances` confirms t3.large + running + new LaunchTime |
| **Concurrent op check** | Agent verification | ✅ No in-flight terraform/deploy workflows on same EC2 i-05d7af46d01436b96 |

## Recommendations

1. **Apply** — proceed with dry_run=true → review → dry_run=false sequence
2. Post-apply: verify EC2 reached `running` state + verify host memory delta (3.7 GiB → 7.4 GiB) before proceeding to Bucket A cred sync
3. Watch-for: Bucket A SSM commands must NOT trigger until EC2 fully back to `running` (per `concurrent-production-mutation-ops.md` §3.1)

## References

- PR #1261 (Bucket D code)
- Wave 70 plan: `documents/03-planning/waves/wave-2026-05-13-70-gap-502-production-thrashing-fix.md`
- GAP-502, GAP-447
- Rules applied: `pre-mutation-state-check.md`, `concurrent-production-mutation-ops.md`, `agent-aws-access.md` §4.3, `terraform-apply-retry-reconfirm.md`, `aws-observability-first.md`

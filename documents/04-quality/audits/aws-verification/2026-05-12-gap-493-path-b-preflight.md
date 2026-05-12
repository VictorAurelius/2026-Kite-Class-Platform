---
title: AWS Verification — GAP-493 Path B preflight + IAM rds:DescribeDBInstances
status: complete
created: 2026-05-12
phase: wave-66-bucket-a
wave: 66
gaps: [GAP-493, GAP-491]
---

# AWS Verification Report — GAP-493 Path B preflight job + IAM extension

## Scope

Wave 66 Bucket A ships GAP-493 Path B: a preflight job in `.github/workflows/deploy-production.yml` that verifies RDS `kitehub-postgres` is `available` BEFORE the deploy job runs SSM SendCommand on the EC2 host. The job uses `aws rds describe-db-instances`, which requires extending the `kitehub-github-deploy` OIDC role's inline policy (`github_deploy_inline`) with a new statement granting `rds:DescribeDBInstances` on `Resource: "*"` (RDS `Describe*` does not support tag-based Condition scoping; least-privilege is achieved by limiting the **action** to read-only `Describe`).

Mutation classification per `agent-aws-access.md`:
- **Workflow YAML edit:** repo-local file change, NOT an AWS mutation. Lands via PR + squash merge.
- **IAM policy edit:** `infrastructure/terraform-aws/iam.tf` change. Apply step is **user-triggered `workflow_dispatch terraform-apply.yml` post-merge with `confirm=APPLY` verbatim** per `release-deploy-standard.md` §9 carve-out (human-triggered workflow_dispatch ALLOWED). NOT agent-initiated `terraform apply` (Tier 3 BANNED).

Rules applied:
- `agent-aws-access.md` §2.1 — Tier 1 read-only verification commands listed below
- `release-deploy-standard.md` §9 — human-triggered workflow_dispatch carve-out for the terraform-apply.yml execution
- `concurrent-production-mutation-ops.md` §3.5 — IAM policy update + deploy using that role have ~10s eventual-consistency window; deploy-production.yml dry_run=true verification waits explicitly
- `pre-mutation-state-check.md` §1.5 — terraform-specific cross-reference matrix below
- `gap-done-discipline.md` §2 — Path A + B both DONE before flip; Log entry cites verification
- `post-merge-sync-completeness.md` §2 — 4 sync targets (gap file + CSV + ROADMAP + this audit)

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# 1. Verify deploy role identity + ARN
aws iam get-role --role-name kitehub-github-deploy --query 'Role.{Arn:Arn,Name:RoleName}'
# Purpose: confirm the role the workflow assumes via AWS_DEPLOY_ROLE_ARN

# 2. Inspect current inline policy on deploy role
aws iam get-role-policy --role-name kitehub-github-deploy --policy-name kitehub-github-deploy-inline \
  --query 'PolicyDocument.Statement[].Sid'
# Purpose: confirm RdsDescribeForPreflight Sid does NOT exist yet (will appear after terraform apply)

# 3. Confirm RDS instance identifier + status
aws rds describe-db-instances --db-instance-identifier kitehub-postgres \
  --query 'DBInstances[0].{Id:DBInstanceIdentifier,Status:DBInstanceStatus,Engine:Engine}'
# Purpose: verify the exact identifier used in the preflight job matches the live instance

# 4. (Post-apply) Re-run #2 to confirm new Sid present
aws iam get-role-policy --role-name kitehub-github-deploy --policy-name kitehub-github-deploy-inline \
  --query 'PolicyDocument.Statement[?Sid==`RdsDescribeForPreflight`]'

# 5. (Post-apply) Dry-run deploy workflow to verify preflight job parses + auth works
gh workflow run deploy-production.yml -f version=v0.9.0-beta-staging.10 -f confirm=DEPLOY
gh run watch <run-id>
# Purpose: confirm preflight job assumes role + calls rds:DescribeDBInstances cleanly
```

## Findings

### Real changes (must verify intent)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `aws_iam_role_policy.github_deploy_inline` | update (add `RdsDescribeForPreflight` statement) | GAP-493 Path B preflight needs `rds:DescribeDBInstances` to verify RDS available before deploy | LOW — additive read-only action; new Sid, no existing statement modified; existing perms unchanged |
| 2 | `.github/workflows/deploy-production.yml` | add `preflight` job + change `deploy.needs: validate` → `deploy.needs: preflight` + add `preflight` to `notify.needs` | Fail fast (<30s) if RDS stopped, instead of 8min container crash-restart loop (root cause from deploy run 25748003956) | LOW — preflight only adds a gate; if RDS available (normal state) flow is identical; if stopped, surfaces actionable error referencing `scripts/start-stack.sh` |

### Phantom updates (no real change)

None — both changes are real, intentional, additive.

### Verdict

Apply is safe. The IAM change is least-privilege (single read-only action, no lifecycle perms). The workflow change is additive (preflight gate inserted between validate and deploy; existing job semantics preserved). No production data at risk — no rotation, no destroy, no modification of running resources. Existing deploys triggered between PR merge and terraform-apply will use the OLD policy (still valid — pre-existing perms unchanged) but will not benefit from preflight until after `kitehub-github-deploy-inline` is updated. Recommended apply order below ensures the workflow + IAM land in lockstep.

### Cross-reference matrix (per `pre-mutation-state-check.md` §1.5)

| IAM Action | Resource pattern in policy | Actual resource (verified) | Workflow caller | Verdict |
|------------|---------------------------|---------------------------|-----------------|---------|
| `rds:DescribeDBInstances` | `*` (no tag scoping — RDS Describe does not support Condition on action) | RDS instance `kitehub-postgres` exists in account 906286017800 region ap-southeast-1 | `.github/workflows/deploy-production.yml` preflight job step `Verify RDS kitehub-postgres available` | ✅ match — single new action, single new caller |
| Existing `ec2:DescribeInstances` | `*` | EC2 tag Project=Kite (kitehub-kh-backend) | `deploy-production.yml` deploy.ec2_lookup step (unchanged) | ✅ unchanged |
| Existing `ssm:SendCommand` | `*` Condition `Project=Kite` | EC2 tag Project=Kite | `deploy-production.yml` deploy.ssm step (unchanged) | ✅ unchanged |
| Existing `secretsmanager:GetSecretValue` | `kitehub/production/*` + `kitehub/staging/*` | Secrets prefix `kitehub/production/*` | `deploy-prod.sh` on EC2 (unchanged) | ✅ unchanged |
| Existing `logs:FilterLogEvents` | `/aws/ssm/kite-deploy` | Log group exists post GAP-491 Phase 1 | `deploy-production.yml` deploy.poll step (unchanged) | ✅ unchanged |

All 5 mapping rows verified; only row 1 is new. No regressions to existing actions or callers.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

| Action | When | Where verified |
|--------|------|----------------|
| GAP-491 CloudWatch log streaming + IAM `logs:FilterLogEvents` added | 2026-05-12 | `gap-status.csv` row GAP-491 DONE 100%; deploy run 25748003956 surfaced live streaming |
| GAP-493 Path A: RDS started + Flyway V34 schema reset + 4/5 containers healthy + api.kitehub.me HTTP 200 | 2026-05-12 | `GAP-493-container-crash-restart-on-deploy.md` Log entry; ALB target health verified |
| Existing `kitehub-github-deploy` inline policy structure (ECR + SSM + EC2 describe + Secrets + Logs) | 2026-05-12 | `infrastructure/terraform-aws/iam.tf:286-374` (pre-this-PR baseline) |
| Identical pattern `rds:DescribeDBInstances` already present in `github_tier_3_cutover_inline` Sid `RdsLifecycle` (with full lifecycle perms) | 2026-05-12 | `iam.tf:570-580` — cutover role; we ADD only Describe to deploy role, NOT lifecycle |
| terraform-apply.yml workflow_dispatch + confirm input "APPLY" + narrow OIDC role pattern | Wave 44 GAP-449 | `release-deploy-standard.md` §9 matrix |
| GAP-492 `scripts/start-stack.sh` uses dynamic EC2 tag lookup (not stale hardcoded IDs) | 2026-05-12 | `gap-status.csv` GAP-492 DONE; preflight error message references this script |

No duplicate work — Path B is the unshipped half of GAP-493 (Path A landed earlier today; Path B explicitly deferred per gap file §"Path B").

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Merge this PR (deploy-production.yml + iam.tf + gap + CSV + ROADMAP + this audit) | User (coordinator) | Squash merge |
| Apply terraform change | User-triggered `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false` per `release-deploy-standard.md` §9 + `agent-aws-access.md` §4.3 carve-out | Agent-initiated `terraform apply` BANNED |
| Verify IAM update | `aws iam get-role-policy --role-name kitehub-github-deploy --policy-name kitehub-github-deploy-inline` shows `RdsDescribeForPreflight` Sid | Tier 1 read-only |
| Verify preflight job parses + role auth works | `gh workflow run deploy-production.yml -f version=<tag> -f confirm=DEPLOY` (post-IAM apply, after ~10s IAM propagation) | If RDS available → preflight passes → deploy proceeds; if RDS stopped → preflight fails fast with actionable error |
| **Concurrent op check** | Agent verification | Verified pre-write: no in-progress terraform-apply.yml or deploy-production.yml runs touching `kitehub-github-deploy` role or `kitehub-postgres` RDS. Apply ordering below avoids concurrent-mutation pitfalls per `concurrent-production-mutation-ops.md` §3.5 (IAM update + deploy using same role) |

## Recommendations

1. **Apply order (avoid IAM eventual-consistency race per `concurrent-production-mutation-ops.md` §3.5):**
   - Step 1: Merge PR — workflow + IAM diff lands on main together.
   - Step 2: Trigger `terraform-apply.yml` with `confirm=APPLY` + `dry_run=false`. Wait `completed/success`.
   - Step 3: Verify `aws iam get-role-policy` shows new `RdsDescribeForPreflight` Sid.
   - Step 4: Wait ≥10s (IAM eventual consistency window) per `concurrent-production-mutation-ops.md` §3.5 row.
   - Step 5: Trigger `deploy-production.yml` `workflow_dispatch` (dry-run or real); preflight job should succeed if RDS available, fail fast with actionable error if RDS stopped.

2. **Post-apply verification commands:**
   ```bash
   aws iam get-role-policy --role-name kitehub-github-deploy \
     --policy-name kitehub-github-deploy-inline \
     --query 'PolicyDocument.Statement[?Sid==`RdsDescribeForPreflight`]'
   # Expected: [{Sid: "RdsDescribeForPreflight", Effect: "Allow", Action: ["rds:DescribeDBInstances"], Resource: "*"}]

   aws rds describe-db-instances --db-instance-identifier kitehub-postgres \
     --query 'DBInstances[0].DBInstanceStatus'
   # Expected: "available" (or "stopped" — preflight should catch this)
   ```

3. **Watch-for items:**
   - If preflight job consistently fails with auth error post-apply → check IAM propagation; allow up to 60s before retry.
   - If preflight reports `ERROR` (catch-all) status → AWS API throttling or transient network; preflight error message preserves the literal status for triage.
   - If cost-saving scheduler stops RDS during off-hours and a deploy is attempted, preflight will block in <30s with explicit pointer to `scripts/start-stack.sh` — that is the intended UX.

## References

- Workflow: `.github/workflows/deploy-production.yml` (preflight job added between validate and deploy)
- Terraform: `infrastructure/terraform-aws/iam.tf` (new `RdsDescribeForPreflight` Sid in `github_deploy_inline` policy)
- Gap: `documents/04-quality/gaps/GAP-493-container-crash-restart-on-deploy.md` (Path B AC completed)
- Related gap: `documents/04-quality/gaps/GAP-491-ssm-cloudwatch-log-streaming.md` (visibility tooling that surfaced GAP-493)
- Wave plan: `documents/03-planning/waves/wave-2026-05-12-66-phase-1-beta-p0-cluster.md`
- Rules: `agent-aws-access.md` §2.1 + §4.3; `release-deploy-standard.md` §9; `concurrent-production-mutation-ops.md` §3.5; `pre-mutation-state-check.md` §1.5; `gap-done-discipline.md` §2; `post-merge-sync-completeness.md` §2

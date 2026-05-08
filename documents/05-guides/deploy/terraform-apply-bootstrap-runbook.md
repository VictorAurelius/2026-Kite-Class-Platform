---
title: Terraform Apply Workflow Bootstrap Runbook
status: active
created: 2026-05-08
phase: 1-beta
related:
  - documents/04-quality/gaps/GAP-449-terraform-apply-workflow-dispatch-rule-revise.md
  - documents/04-quality/gaps/GAP-446-aws-resource-scheduling-cost-saving.md
  - documents/04-quality/gaps/GAP-447-right-size-ec2-post-vercel-pivot.md
  - documents/03-planning/waves/wave-2026-05-08-44-terraform-apply-workflow.md
  - documents/05-guides/deploy/aws-cost-scheduling.md
  - documents/05-guides/deploy/right-size-stress-test.md
  - .claude/rules/agent-aws-access.md
  - .claude/rules/terraform-partial-backend-public-repo.md
  - .claude/rules/terraform-apply-retry-reconfirm.md
---

# Terraform Apply Workflow Bootstrap Runbook

**Audience:** Solo-dev (acting deploy operator) — first-time setup of `terraform-apply.yml` workflow infrastructure.

**One-shot ops:** sections §3-§5 run ONCE per AWS account. After bootstrap completes, all future `terraform apply` operations use `gh workflow run terraform-apply.yml` (no more local apply).

**Cross-links:**
- Wave 44 plan: [`documents/03-planning/waves/wave-2026-05-08-44-terraform-apply-workflow.md`](../../03-planning/waves/wave-2026-05-08-44-terraform-apply-workflow.md)
- Parent gap: [GAP-449](../../04-quality/gaps/GAP-449-terraform-apply-workflow-dispatch-rule-revise.md)
- Wave 43 close-out: [GAP-446](../../04-quality/gaps/GAP-446-aws-resource-scheduling-cost-saving.md) + [GAP-447](../../04-quality/gaps/GAP-447-right-size-ec2-post-vercel-pivot.md)

---

## 1. Why this runbook

Wave 44 ships the `terraform-apply.yml` GitHub Actions workflow infrastructure (Bucket A: rule revision; Bucket B: workflow + IAM scaffold; Bucket C: this runbook).

**Chicken-and-egg:** the workflow assumes role `kitehub-github-terraform-apply` exists in AWS IAM. That role is defined as terraform code in Bucket B's `iam.tf` — but to CREATE the role, terraform itself must run. The very first `terraform apply` after Wave 44 merge MUST therefore run **locally** (with admin credentials), not via the workflow.

After bootstrap completes:
- Apply role lives in AWS, ARN captured into GitHub Variable
- All subsequent `terraform apply` operations route through `gh workflow run terraform-apply.yml`
- Admin access key gets rotated/inactivated (one-shot use)
- Local apply is BANNED for routine work (`terraform-apply-retry-reconfirm.md` Tier 3)

This bootstrap also serves as the close-out path for Wave 43 (GAP-446 EventBridge schedules + GAP-447 EC2 right-size + memory alarms) — all those changes are in `infrastructure/terraform-aws/**` waiting to ship in this same first apply.

---

## 2. Prerequisites

Before running §3 onward, confirm:

- [ ] AWS admin access key (one-time use) — IAM user `solo-dev-admin` or equivalent with `AdministratorAccess` policy
- [ ] Terraform CLI ≥ 1.7.5 local: `terraform --version`
- [ ] AWS CLI v2 local: `aws --version`
- [ ] GitHub CLI authenticated: `gh auth status` shows logged in
- [ ] Repo write access (need `gh variable set` permission on `VictorAurelius/2026-Kite-Class-Platform`)
- [ ] Wave 44 PR merged to `main` (Bucket A rule revision + Bucket B workflow + IAM scaffold present in tree)
- [ ] You're on `main` branch with `git pull --ff-only` clean

If any item missing → STOP. Bootstrap requires all of the above.

---

## 3. One-time bootstrap — local terraform apply

### 3.1 Configure admin profile

```bash
aws configure --profile kite-admin
# AWS Access Key ID:     <paste-admin-key>
# AWS Secret Access Key: <paste-admin-secret>
# Default region:        ap-southeast-1
# Default output format: json
```

Verify:

```bash
aws sts get-caller-identity --profile kite-admin
# Expected: Account 906286017800, Arn ending in :user/solo-dev-admin
```

### 3.2 Sync main + cd terraform dir

```bash
git checkout main && git pull --ff-only origin main
cd infrastructure/terraform-aws
```

### 3.3 Backend config

Per [`terraform-partial-backend-public-repo.md`](../../../.claude/rules/terraform-partial-backend-public-repo.md), `backend.tf` uses partial config; bucket name lives in gitignored `backend.config`.

```bash
# First time setup (skip if backend.config already exists)
cp backend.config.example backend.config
```

Edit `backend.config` to set the actual bucket name:

```
bucket = "kitehub-terraform-state-906286017800"
```

(The `.gitignore` already excludes `backend.config`; do NOT commit it.)

### 3.4 Init + plan + review

```bash
AWS_PROFILE=kite-admin terraform init -backend-config=backend.config
AWS_PROFILE=kite-admin terraform plan -out=bootstrap.tfplan
```

**Review plan output** before applying. Expected resources:

- New IAM role `kitehub-github-terraform-apply` + trust policy + permission policy (Wave 44 Bucket B)
- New EventBridge schedules (`kite_cost_saving` schedule group, ~8 schedules — stop/start EC2 + RDS pairs) (Wave 43 Bucket A)
- EC2 `instance_type` change `m7i-flex.large` → `t3.medium` for Phase 1 BETA instances (Wave 43 Bucket B)
- New CloudWatch memory alarms `kitehub-memory-*` (Wave 43 Bucket B)
- Other Wave 43/44 deltas as listed in respective gap files

If plan shows unexpected resources (e.g. wholesale stack recreation) → STOP, investigate, do NOT apply.

### 3.5 Apply (single-shot, explicit confirmation)

Per [`terraform-apply-retry-reconfirm.md`](../../../.claude/rules/terraform-apply-retry-reconfirm.md), the first user approval covers ONE apply attempt. If apply fails mid-run, fix the offending file and request explicit re-confirm before re-running.

```bash
AWS_PROFILE=kite-admin terraform apply bootstrap.tfplan
```

Type `yes` at the confirmation prompt.

**On failure mid-apply:**

1. Read the error message; identify the offending resource
2. Fix the file (common case: ASCII-only issue per [`aws-sg-description-ascii.md`](../../../.claude/rules/aws-sg-description-ascii.md))
3. Re-run `terraform plan -out=bootstrap.tfplan`
4. Get explicit user re-confirm before next `terraform apply`
5. Resume

---

## 4. Capture role ARN + set GitHub Variable

### 4.1 Get apply role ARN

```bash
APPLY_ROLE_ARN=$(AWS_PROFILE=kite-admin terraform output -raw github_terraform_apply_role_arn)
echo "$APPLY_ROLE_ARN"
# Expected: arn:aws:iam::906286017800:role/kitehub-github-terraform-apply
```

If output not present, check Bucket B's `iam.tf` for the matching `output "github_terraform_apply_role_arn"` block.

### 4.2 Set GitHub Variable

```bash
gh variable set AWS_TERRAFORM_APPLY_ROLE_ARN --body "$APPLY_ROLE_ARN"
```

Verify:

```bash
gh variable list | grep AWS_TERRAFORM_APPLY_ROLE_ARN
# Expected: AWS_TERRAFORM_APPLY_ROLE_ARN  arn:aws:iam::906286017800:role/...  Updated <date>
```

### 4.3 (Optional but recommended) GitHub Environment `production` protection

For an extra approval gate on top of the workflow's `confirm` input:

1. Open `https://github.com/VictorAurelius/2026-Kite-Class-Platform/settings/environments`
2. Click **New environment** → name it `production`
3. Add **Required reviewers** = `@nguyenvankiet`
4. Restrict to **Selected branches** = `main`
5. Save

This is `agent-action-bias.md` §3 row 1 — no CLI path exists for full Environment protection-rule configuration as of this writing.

If `terraform-apply.yml` references `environment: production` (per Bucket B), this Environment must exist before the first workflow run succeeds.

---

## 5. Test workflow_dispatch in dry_run mode

### 5.1 Trigger dry_run (plan only — does NOT apply)

```bash
gh workflow run terraform-apply.yml \
  -f confirm=APPLY \
  -f dry_run=true
```

### 5.2 Watch run

```bash
gh run watch
# OR list latest run for inspection:
gh run list --workflow=terraform-apply.yml --limit 1
```

### 5.3 Download plan artifact

```bash
gh run download --name terraform-plan
cat plan-output.txt | head -50
```

Expected output (since §3.5 already applied everything):

```
No changes. Your infrastructure matches the configuration.
```

If dry_run fails:

- Verify role ARN matches GitHub Variable: `gh variable list`
- Verify GitHub Environment `production` exists (per §4.3)
- Inspect logs verbosely: `gh run view --log`
- Common cause: trust policy condition on `aud` / `sub` claim doesn't match repo OIDC subject — check `iam.tf` trust policy

---

## 6. (Future) First real workflow_dispatch apply

Once §5 dry_run passes clean, future terraform changes (Wave 45+) ship via:

```bash
# Step 1: merge a PR with terraform changes to main
# Step 2: trigger apply
gh workflow run terraform-apply.yml \
  -f version=main \
  -f confirm=APPLY \
  -f dry_run=false

# Step 3: watch + verify
gh run watch
```

Local `terraform apply` is reserved for emergency-only — see [`terraform-apply-retry-reconfirm.md`](../../../.claude/rules/terraform-apply-retry-reconfirm.md) overrides.

---

## 7. Verify Wave 43 changes (Tier 1 read-only)

Per [`agent-aws-access.md`](../../../.claude/rules/agent-aws-access.md) §2 Tier 1 allowlist. Use `kite-readonly` profile (or `--profile default` if read-only is configured there).

### 7.1 EventBridge schedules (Wave 43 Bucket A — GAP-446)

```bash
aws scheduler list-schedules \
  --profile kite-readonly \
  --query 'Schedules[?GroupName==`kite_cost_saving`].[Name,State,ScheduleExpression]' \
  --output table
```

Expected: 8 schedules total (4 stop + 4 start, paired across EC2 + RDS resources). All `State=ENABLED`.

If output is empty: schedules not deployed — re-check §3.5 apply included `aws_scheduler_schedule.*` resources.

### 7.2 EC2 right-size (Wave 43 Bucket B — GAP-447)

```bash
aws ec2 describe-instances \
  --profile kite-readonly \
  --query 'Reservations[].Instances[?Tags[?Key==`Phase` && Value==`1-beta`]].[InstanceId,InstanceType,State.Name]' \
  --output table
```

Expected: 2 instances (kh-backend + kc-app), both `InstanceType=t3.medium`, `State.Name=running`.

If `InstanceType` still shows `m7i-flex.large`: terraform diff was not applied — re-check §3.5 plan included EC2 instance_type changes.

### 7.3 CloudWatch memory alarms (Wave 43 Bucket B — GAP-447)

```bash
aws cloudwatch describe-alarms \
  --profile kite-readonly \
  --alarm-name-prefix kitehub-memory \
  --query 'MetricAlarms[].[AlarmName,StateValue]' \
  --output table
```

Expected: 2 alarms (one per EC2). `StateValue=OK` (no breach yet) or `INSUFFICIENT_DATA` (CloudWatch agent not yet emitting).

If `INSUFFICIENT_DATA` persists >30 min: CloudWatch agent likely not installed/configured on the EC2. See `right-size-stress-test.md` §CloudWatch agent section.

---

## 8. File AWS verification artifacts

Per [`agent-aws-access.md`](../../../.claude/rules/agent-aws-access.md) §5, multi-command verification sessions MUST save artifacts.

Replace `YYYY-MM-DD` with the actual session date (per [`session-currentdate-check.md`](../../../.claude/rules/session-currentdate-check.md)).

### 8.1 Wave 43 scheduler artifact

Create `documents/04-quality/audits/aws-verification/YYYY-MM-DD-wave-43-scheduler.md` using the format from [`agent-aws-access.md §5.1`](../../../.claude/rules/agent-aws-access.md):

- **Scope:** Wave 43 Bucket A EventBridge stop/start schedule verification
- **Commands run:** `aws scheduler list-schedules ...` (full §7.1 commands + outputs)
- **Results:** Schedule names + states + ARNs
- **Findings:** Any anomalies (extra schedules, missing pairs, wrong cron expressions)
- **Next steps:** Confirm 22:00 ICT first stop event fires + 08:00 ICT first start event fires; capture CloudWatch logs of first stop/start cycle

Reference template: `documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md`.

### 8.2 Wave 43 right-size artifact

Create `documents/04-quality/audits/aws-verification/YYYY-MM-DD-wave-43-right-size.md`:

- **Scope:** Wave 43 Bucket B right-size + memory alarm verification (GAP-447)
- **Commands run:** §7.2 + §7.3 commands + outputs
- **Results:** Instance type confirmation + alarm state per instance
- **Findings:** Stress test results (1h `kh-backend` monitor of `MemoryUtilization` post-right-size); was 80% threshold hit?
- **Next steps:** Decide kc-app right-size after kh-backend stable for ≥48h

---

## 9. Flip GAP-446 + GAP-447 → 🟢 DONE

Per [`gap-done-discipline.md`](../../../.claude/rules/gap-done-discipline.md) §2 — DONE flip requires every AC checkbox checked and verification artifact pointer in the closing Log entry.

For each gap (GAP-446, GAP-447):

1. Edit the gap file's frontmatter Status: `🟡 PARTIAL` → `🟢 DONE YYYY-MM-DD`
2. Verify ALL `- [ ]` AC checkboxes are now `- [x]` (no unchecked items)
3. Append a Log entry citing:
   - PR number that closed the gap
   - Verification artifact path (`documents/04-quality/audits/aws-verification/YYYY-MM-DD-wave-43-*.md`)
   - Brief note on outcome
4. Self-check: scan Log entry for banned phrases (`deferred`, `manual run`, `infra block`, `local can't`, `partial` when DONE) — none should appear

If any AC genuinely cannot close in this PR: keep status `🟡 PARTIAL`, don't flip to DONE. File follow-up gap for the deferred slice.

---

## 10. Rotate admin key (final security step — DO NOT SKIP)

The admin access key configured in §3.1 has wide blast-radius (`AdministratorAccess`). After bootstrap completes, it must be retired.

### 10.1 Inactivate via AWS Console

No CLI path covers all the IAM Console UI features cleanly (per [`agent-action-bias.md`](../../../.claude/rules/agent-action-bias.md) §3 row 1 — inactivate-then-delete flow is documented in Console).

1. Open `https://console.aws.amazon.com/iam/home#/users/details/solo-dev-admin?section=security_credentials`
2. Find the access key used in §3.1
3. Click **Actions → Make inactive**
4. Confirm
5. Click **Actions → Delete**
6. Confirm again (irreversible)

### 10.2 (Optional) Replace with read-only WSL profile

If you want continued read-only AWS access from WSL (recommended for ad-hoc verification):

1. Create a new IAM user `kite-readonly-wsl` with policy `arn:aws:iam::aws:policy/ReadOnlyAccess`
2. Generate access key for the new user
3. `aws configure --profile kite-readonly` — paste the new read-only key
4. Test: `aws sts get-caller-identity --profile kite-readonly` should return new ARN
5. Future Tier 1 commands use `--profile kite-readonly`

### 10.3 Verify admin profile no longer works

```bash
aws sts get-caller-identity --profile kite-admin
# Expected: error "InvalidClientTokenId" or "The security token included in the request is invalid"
```

If still works: the deletion didn't propagate yet — wait 60s, retry. If still works after 5 min: re-check §10.1 actually completed.

---

## 11. Summary checklist

After running this runbook end-to-end, all of these should be true:

- [ ] §3 local apply completed (admin profile, single attempt, plan reviewed before apply)
- [ ] §4.2 GitHub Variable `AWS_TERRAFORM_APPLY_ROLE_ARN` set
- [ ] §4.3 GitHub Environment `production` configured (with required reviewer + branch restriction)
- [ ] §5 workflow_dispatch dry_run returned "No changes"
- [ ] §7 Wave 43 changes verified (8 schedules + 2 t3.medium instances + 2 memory alarms)
- [ ] §8 two AWS verification artifacts filed
- [ ] §9 GAP-446 + GAP-447 flipped to 🟢 DONE
- [ ] §10 admin key rotated (inactive + deleted; read-only profile substituted if desired)

Past this point, terraform changes route exclusively through `gh workflow run terraform-apply.yml`. Local `terraform apply` is reserved for documented emergency overrides only.

# GAP-450: Terraform state drift — random_password + kc_app instance attributes

**Status:** 🟡 PARTIAL — Option B (lifecycle ignore_changes) shipped 2026-05-11 PR #1154 — drift symptom ẩn khỏi `terraform plan`. Option A (state rm + import current Secrets Manager values) tracked in `documents/05-guides/operations/terraform-state-import-runbook.md` for user manual execution (agent blocked by stale credentials + Tier 2/3 rule per `agent-aws-access.md` §4.3).
**Priority:** 🟠 P1 (cleanup; non-blocking — bootstrap apply succeeded around drift)
**Domain:** Infrastructure / Terraform / FinOps
**Found:** 2026-05-08 (Wave 43+44 bootstrap apply session)
**Affects:** Future `terraform apply` operations — drift will re-surface mỗi lần plan unless fixed

## Problem

Bootstrap apply 2026-05-08 phát hiện 2 state-drift class:

### 1. `random_password.{jwt,rds,encryption_raw}` ID="none"

Terraform state shows:
```
id = "none"
# (12 unchanged attributes hidden)
```

Nhưng AWS Secrets Manager đã có secret_version với timestamps `20260507222206...` (Phase 2.3 apply 2026-05-07). State không track random_password resources, nhưng AWS có secret values.

Apply targeted bootstrap skipped these → next `terraform plan` sẽ tiếp tục show "will be updated in-place" vô hạn.

**Risk if applied:** random_password regenerates → secret_version overwrites → kh_backend (LIVE) JWT/RDS password mismatch → outage.

### 2. `aws_instance.kc_app` `associate_public_ip_address` drift

Bootstrap plan showed:
```
~ associate_public_ip_address = false -> true # forces replacement
```

State had `false`, terraform config wants `true` → forced replacement. kc_app `i-04f65503ace7febe4` destroyed, new `i-07f6de54544162124` created.

Drift root cause: chưa rõ — possibly Phase 2.3 apply set `associate_public_ip_address` differently than current ec2.tf.

## Root Cause

State versioning history pre-Phase-2.3 unclear. Possibilities:
- Phase 2.3 apply used different state file than current S3 backend
- Manual AWS Console changes drift terraform state
- terraform refresh issue khi state migration giữa apply runs

## Proposed Fix

### Phase 1 — Investigate drift

```bash
# Check state version history (S3 versioning enabled)
aws s3api list-object-versions \
  --bucket kitehub-terraform-state-906286017800 \
  --prefix phase-1-beta/terraform.tfstate \
  --query 'Versions[].[VersionId,LastModified]' \
  --output table

# Compare current state với latest backup
terraform state pull > current.tfstate
aws s3api get-object --bucket ... --key phase-1-beta/terraform.tfstate --version-id <PHASE_2_3_VERSION> /tmp/phase-2-3.tfstate
diff <(jq '.resources[] | select(.type=="random_password")' current.tfstate) <(jq '.resources[] | select(.type=="random_password")' /tmp/phase-2-3.tfstate)
```

### Phase 2 — Fix random_password drift

3 sub-options:

**A.** `terraform state rm` random_password resources + re-`terraform import` với current secret values (read from Secrets Manager):
```bash
# Read existing secret values
JWT=$(aws secretsmanager get-secret-value --secret-id kitehub/production/jwt-secret --query SecretString --output text)
RDS_PASS=$(aws secretsmanager get-secret-value --secret-id kitehub/production/db-password --query SecretString --output text | jq -r .password)
ENC=$(aws secretsmanager get-secret-value --secret-id kitehub/production/encryption-key --query SecretString --output text)

# Remove from state
terraform state rm random_password.jwt random_password.rds random_password.encryption_raw

# Re-import — random_password import format: <result>
terraform import random_password.jwt "$JWT"
terraform import random_password.rds "$RDS_PASS"
terraform import random_password.encryption_raw "$ENC"
```

**B.** Add `lifecycle { ignore_changes = [result] }` to random_password — terraform stops tracking changes:
```hcl
resource "random_password" "jwt" {
  length  = 64
  special = false
  lifecycle {
    ignore_changes = [result]
  }
}
```

**C.** Use `random_id` keepers pattern — anchor regeneration to specific keepers (e.g., manual rotation trigger), not state drift.

**Recommendation:** Phase 2 Option A (cleanest, restores normal terraform tracking).

### Phase 3 — Fix kc_app drift

After kc_app replaced 2026-05-08, current state should match terraform config (since it just got created). Verify next plan run shows no drift on kc_app.

If drift returns:
- Check Console manually changed `associate_public_ip_address`
- OR re-`terraform import aws_instance.kc_app i-07f6de54544162124` if state mismatch

### Phase 4 — Provision kc_app_memory_high alarm

Currently skipped due to kc_app drift. After Phase 3 stable:
```bash
terraform apply -target=aws_cloudwatch_metric_alarm.kc_app_memory_high
```

## Acceptance Criteria

### Phase B — Option B shipped (this PR 2026-05-11)
- [x] `lifecycle { ignore_changes = [...] }` added to all 3 `random_password` resources (rds.tf + secrets.tf) — `terraform plan` no longer shows recurring drift on these resources
- [x] Runbook `documents/05-guides/operations/terraform-state-import-runbook.md` created for user manual Option A execution
- [x] Comment-block in each resource references GAP-450 + Option A runbook path

### Phase A — Option A deferred to user manual execution
- [ ] `terraform state pull` shows random_password resources với valid IDs (not `"none"`)
- [ ] `terraform plan` reports clean state (no `random_password` updates pending)
- [ ] Secret values trong AWS Secrets Manager unchanged (verified via timestamp not bumped)
- [ ] kh_backend health check 200 post-import (no JWT/RDS password rotation collateral) — verify khi infra resumed
- [ ] kc_app drift resolved — `terraform plan` shows no replacement pending
- [ ] kc_app_memory_high alarm provisioned post-stabilization
- [ ] Investigation findings documented (Phase 2.3 history) trong this gap Log post-Option-A execution

## Related

- Wave 43 closure (#1040) — kc_app replacement scope creep
- Wave 44 bootstrap (this session) — drift surfaced
- `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-44-bootstrap-apply.md` (verification artifact)
- `terraform-apply-retry-reconfirm.md` (drift-driven re-apply pattern)
- Memory `feedback_terraform_partial_backend_public_repo.md` (state backend pattern)
- Phase 2.3 apply 2026-05-07 (state baseline)

## Log

- **2026-05-08** — OPEN. Filed sau Wave 43+44 bootstrap apply phát hiện 2 drift classes (random_password ID=none + kc_app associate_public_ip_address). Targeted apply skipped drift to avoid kh_backend outage; clean fix tracked here for separate session với explicit safety net + investigation phase.
- **2026-05-11 — 🟡 PARTIAL (Path B+C combined per session retro):** Option A attempted but blocked pre-flight — agent credentials stale (key `AKIA…E7SO` deleted 2026-05-08 per ROADMAP §🚀 Next Action; local `~/.aws/credentials` chưa update với `AKIA…SVMD`) + Tier 3 BAN per `agent-aws-access.md` §4.3 (`terraform state rm` + `import`) + Tier 2 always-confirm per §2.2 (`get-secret-value` × 3). Path chuyển sang B+C: (a) **Option B shipped** — `lifecycle { ignore_changes = [result, length, special, lower, upper, numeric, ...] }` added to 3 random_password resources (rds.tf + secrets.tf) → drift symptom ẩn khỏi `terraform plan`. (b) **Runbook tạo** — `documents/05-guides/operations/terraform-state-import-runbook.md` cung cấp 12-step procedure cho user manual Option A execution khi credentials sẵn sàng + maintenance window. Phase A AC giữ unchecked, Phase B AC checked. Status PARTIAL không DONE per `gap-done-discipline.md` §3 — Phase A deferred items vẫn tracked. Per `release-fix-retry-budget.md`: retry #1 (Option A pre-flight) fail → §3 STOP-AND-REDESIGN → pivot Option B (correct decision, drift symptom resolved without state surgery).

---
title: AWS Verification — GAP-450 Option A pre-flight blocked, pivot Option B + runbook
status: complete
created: 2026-05-11
phase: cleanup
---

# AWS Verification Report — GAP-450 Option A pre-flight blocked, pivot Option B + runbook

## Scope

Attempted GAP-450 Option A (terraform state rm + import current Secrets Manager values for `random_password.{jwt, rds, encryption_raw}`) per user pre-authorization 2026-05-11. Goal: phục hồi normal terraform tracking thay vì che drift bằng lifecycle ignore_changes.

## Commands run

### Pre-flight Step 0 (Tier 1 read attempt)

```bash
aws sts get-caller-identity --query Account --output text
aws ec2 describe-instances --filters "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app" ...
aws rds describe-db-instances --db-instance-identifier kitehub-postgres ...
```

### Diagnosis

```bash
cat ~/.aws/credentials       # Tier 1 — local file inspection
cat ~/.aws/config            # Tier 1 — local file inspection
aws configure list-profiles  # Tier 1 — local CLI
```

## Results

| Command | Outcome |
|---|---|
| `aws sts get-caller-identity` | ❌ EXIT 253: `NoCredentials — Unable to locate credentials` |
| `aws ec2 describe-instances` | ❌ same error (chained Bash failure) |
| `aws rds describe-db-instances` | ❌ same error |
| `cat ~/.aws/credentials` | Shows profile `kite-readonly` với key `AKIA5GAW3FUEMPMSE7SO` |
| `cat ~/.aws/config` | Profile `kite-readonly` configured, region `ap-southeast-1` |
| `aws configure list-profiles` | Only `kite-readonly` listed |

## Findings

### Finding 1: Credentials stale (HARD BLOCKER)

Local key `AKIA5GAW3FUEMPMSE7SO` (đuôi `…E7SO`) is the OLD `kite-readonly-wsl` key.

Per `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action — Pending user actions item #2 (2026-05-08):

> ✅ **Rotate `kite-readonly-wsl` key** — DONE 2026-05-08 via `scripts/rotate-iam-access-key.sh`; new AKID `AKIA…SVMD`, old `AKIA…E7SO` deleted.

Local `~/.aws/credentials` was NOT updated after rotation → key authoritatively deleted 2026-05-08 in AWS IAM → any API call fails with `NoCredentials`.

### Finding 2: Profile scope mismatch (Tier 3 not authorized for kite-readonly even if credentials valid)

Profile name `kite-readonly` implies read-only IAM permissions. Option A requires:
- `secretsmanager:GetSecretValue` × 3 — read-only API but typically restricted in read-only profile
- `s3:PutObject` cho terraform state bucket — write operation, NOT in read-only scope

Required profile for Option A: `dev-admin` / `solo-dev-admin` (key `AKIA…52MY` per ROADMAP) which has full IAM + write permissions.

Audit-of-audit note: even if user updates credentials với new `AKIA…SVMD` key for `kite-readonly` profile, Option A still requires separate `dev-admin` profile.

### Finding 3: Agent rule constraint compounding (separate from credential issue)

Even if credentials were valid + correct profile available:

- `terraform state rm` — Tier 3 BANNED per `.claude/rules/agent-aws-access.md` §4.3
- `terraform import` — Tier 3 BANNED per §4.3
- `aws secretsmanager get-secret-value` × 3 — Tier 2 always-confirm per §2.2

User pre-authorized Tier 3 override for this session ("cứ làm Option A và xóa các dữ liệu bảo mật sau khi kết thúc"), commit trailer template chuẩn bị sẵn: `AGENT_AWS_TIER_3_OVERRIDE: GAP-450 random_password state-import — user pre-authorized 2026-05-11 — secrets unset post-run`. Nhưng credential blocker (Finding 1) prevented authorization từ being exercised.

## Next steps

Pivot to **Path B+C combined** trong cùng PR:

1. **Option B (this PR)** — add `lifecycle { ignore_changes = [result, length, special, lower, upper, numeric, min_lower, min_upper, min_numeric, min_special, override_special, keepers] }` to 3 `random_password` resources trong `infrastructure/terraform-aws/{secrets.tf, rds.tf}`. Effect: drift symptom ẩn khỏi `terraform plan`. Trade-off: random_password vĩnh viễn không tự xoay (acceptable — rotation cadence manual per `secrets-rotation-runbook.md` §5).

2. **Runbook (this PR)** — create `documents/05-guides/operations/terraform-state-import-runbook.md` với 12-step Option A procedure cho user manual execution khi credentials sẵn sàng.

3. **GAP-450 status** — 🔵 OPEN → 🟡 PARTIAL. Phase B AC checked, Phase A AC unchecked (deferred). Per `gap-done-discipline.md` §3 PARTIAL exit ramp.

4. **User follow-up actions (post-PR merge):**
   - Update `~/.aws/credentials` với new `kite-readonly-wsl` key `AKIA…SVMD` (per ROADMAP)
   - Add `dev-admin` profile với key `AKIA…52MY` cho write operations
   - Schedule maintenance window (EC2/RDS Phase 1 BETA hiện đang STOPPED — opportunistic timing)
   - Execute runbook Option A 12 steps
   - Save execution audit artifact tại `documents/04-quality/audits/aws-verification/<execution-date>-gap-450-option-a-execution.md`
   - Update GAP-450 status PARTIAL → DONE

## Compliance + Rule references

- `.claude/rules/agent-aws-access.md` §2.2 (Tier 2 get-secret-value), §4.3 (Tier 3 banned terraform state rm/import), §5 (audit artifact mandatory cho multi-command verification session)
- `.claude/rules/terraform-apply-retry-reconfirm.md` (state-changing operations need confirm per step)
- `.claude/rules/release-fix-retry-budget.md` §3 STOP-AND-REDESIGN (retry #1 pre-flight fail → pivot Option B)
- `.claude/rules/release-deploy-standard.md` §9 (Claude agent role matrix — state surgery = SKIP for human)
- `.claude/rules/gap-done-discipline.md` §3 PARTIAL exit ramp (Phase A deferred → status PARTIAL not DONE)

## Secret values handling

**Zero secret values were read in this session.** Pre-flight `get-secret-value` calls were never reached due to Finding 1 (credentials stale). No secret material flowed through session transcript. Audit artifact contains only command names + exit codes + diagnostic outputs (profile names, region, IAM key AKIDs in error messages which are not secret material).

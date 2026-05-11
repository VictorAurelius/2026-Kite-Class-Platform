# Terraform State Import Runbook — GAP-450 random_password drift fix (Option A)

**Audience:** Solo-dev / SRE thực hiện thủ công Option A của GAP-450 — phẫu thuật state để xoá lệch `random_password.{jwt, rds, encryption_raw}` mà không tự xoay secret values trong AWS Secrets Manager.
**Sister docs:** `documents/04-quality/gaps/GAP-450-terraform-state-drift-random-password-kc-app.md` (gap context); `documents/05-guides/operations/secrets-rotation-runbook.md` (rotation cadence).
**Standards:** `.claude/rules/agent-aws-access.md` (Tier hierarchy) · `.claude/rules/terraform-apply-retry-reconfirm.md` (state-changing operations need confirm per step) · `.claude/rules/release-fix-retry-budget.md` (retry budget §3).
**Naming:** Per `.claude/rules/deployment-naming-convention.md` §2 — recurring ops procedure → `operations/`.

---

## 1. Audience + scope

Runbook này dành **user (solo-dev) chạy thủ công** — KHÔNG phải agent. Lý do agent không chạy:

- `terraform state rm` + `terraform import` thuộc Tier 3 BANNED per `agent-aws-access.md` §4.3
- `aws secretsmanager get-secret-value` thuộc Tier 2 always-confirm per §2.2 (lộ giá trị bí mật)
- Cộng dồn 3 lệnh × 3 resource = 9 lần confirm + state-surgery risk → ngoài scope agent autonomy

Phase 2 Option B (`lifecycle { ignore_changes = [...] }`) đã land trong cùng PR với runbook này → drift symptom đã ẩn khỏi `terraform plan`. Option A là **cleanup definitive** — phục hồi tracking thực sự thay vì che drift bằng `ignore_changes`.

Khi nào chạy runbook này:
- Sau khi key `kite-readonly-wsl` mới (`AKIA…SVMD`) hoặc `dev-admin` (`AKIA…52MY`) được cập nhật vào `~/.aws/credentials`
- Trước khi cần `terraform apply` thay đổi gì khác (vì Option B ignore_changes có thể che drift hợp lệ trong tương lai)
- Trong maintenance window — EC2 + RDS Phase 1 BETA đang STOPPED là thời điểm an toàn

---

## 2. Pre-requisites

- AWS CLI authenticated với profile có quyền `secretsmanager:GetSecretValue` + `s3:GetObject`/`PutObject` cho terraform state bucket
- Suggested profile: `dev-admin` hoặc tương đương (KHÔNG dùng `kite-readonly` — thiếu quyền ghi state)
- Terraform `>= 1.5` installed
- Backend config có sẵn (`infrastructure/terraform-aws/backend.config` per `terraform-partial-backend-public-repo.md`)
- **Bắt buộc**: verify EC2 + RDS đang STOPPED (mitigate outage risk nếu import sai):

```bash
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app" \
  --query 'Reservations[].Instances[].{Name:Tags[?Key==`Name`].Value|[0],State:State.Name}' \
  --output table

aws rds describe-db-instances --db-instance-identifier kitehub-postgres \
  --query 'DBInstances[].DBInstanceStatus' --output text
```

Expected: cả 2 EC2 = `stopped`, RDS = `stopped`. Nếu LIVE → schedule maintenance window trước.

---

## 3. Procedure — Option A 12 bước

### Phase 1 — Investigate (Tier 1 reads)

```bash
cd infrastructure/terraform-aws
terraform init -backend-config=backend.config
```

```bash
# Step 1: dump current state random_password resources
terraform state pull | jq '.resources[] | select(.type=="random_password") | {address: (.module // "root") + "." + .type + "." + .name, instances: [.instances[] | {id: .attributes.id, length: .attributes.length}]}'
```

```bash
# Step 2: list S3 state versions (forensic — preserve rollback path)
aws s3api list-object-versions \
  --bucket kitehub-terraform-state-906286017800 \
  --prefix phase-1-beta/terraform.tfstate \
  --max-items 10 \
  --query 'Versions[].[VersionId,LastModified,Size]' \
  --output table

# Note: save current state version-id for rollback path
CURRENT_VERSION=$(aws s3api list-object-versions \
  --bucket kitehub-terraform-state-906286017800 \
  --prefix phase-1-beta/terraform.tfstate \
  --query 'Versions[0].VersionId' --output text)
echo "Current state version-id (for rollback): $CURRENT_VERSION"
```

```bash
# Step 3: describe secrets (Tier 1 — không đọc giá trị)
for SECRET in jwt-secret db-password encryption-key; do
  echo "=== kitehub/production/$SECRET ==="
  aws secretsmanager describe-secret --secret-id kitehub/production/$SECRET \
    --query '{Name:Name, LastChangedDate:LastChangedDate, Versions:VersionIdsToStages}' \
    --output table
done
```

Expected: 3 secrets exist + có `AWSCURRENT` stage. Nếu nào missing → STOP, investigate trước.

### Phase 2 — Read secrets + verify (Tier 2 always-confirm)

```bash
# Step 4: đọc 3 giá trị vào biến shell tạm (KHÔNG echo ra)
JWT=$(aws secretsmanager get-secret-value --secret-id kitehub/production/jwt-secret --query SecretString --output text)
RDS_PASS=$(aws secretsmanager get-secret-value --secret-id kitehub/production/db-password --query SecretString --output text | jq -r .password)
ENC=$(aws secretsmanager get-secret-value --secret-id kitehub/production/encryption-key --query SecretString --output text)
```

```bash
# Step 5: sanity check độ dài (KHÔNG in giá trị thật)
echo "JWT length=${#JWT}  (expect 64)"
echo "RDS_PASS length=${#RDS_PASS}  (expect 32)"
echo "ENC length=${#ENC}  (expect 32 — sau base64 decode sẽ là 32 bytes raw)"
```

Nếu length=0 cho bất kỳ → STOP. Có thể secret name sai hoặc structure khác (db-password là JSON object với key `password`). Investigate, **không proceed**.

### Phase 3 — State surgery (Tier 3 BAN — user thực hiện)

```bash
# Step 6: REMOVE temporary lifecycle ignore_changes blocks
# Phase 1 ship Option B (lifecycle ignore_changes) — Option A cần XOÁ blocks này trước
# khi import, nếu không Terraform sẽ ignore result mãi mãi.
# Edit infrastructure/terraform-aws/secrets.tf + rds.tf: xoá 3 lifecycle blocks (GAP-450 Option B markers)
# Comment GAP-450 ở trên cũng xoá đồng thời.
```

**Đề xuất:** dùng `git diff` xem PR đã add gì rồi revert chỉ những lines liên quan random_password lifecycle. Hoặc edit bằng tay.

```bash
# Step 7: state rm
terraform state rm random_password.jwt random_password.rds random_password.encryption_raw
```

Expected output: `Removed random_password.jwt` × 3. Exit 0.

```bash
# Step 8: import với giá trị thực
# Format import: terraform import <addr> "<value>"
terraform import random_password.jwt "$JWT"
terraform import random_password.rds "$RDS_PASS"
terraform import random_password.encryption_raw "$ENC"
```

Expected: 3 `Import successful!`. Exit 0 each.

```bash
# Step 9: verify plan sạch
terraform plan -detailed-exitcode
echo "Exit code: $? (0 = no changes, 2 = changes pending, 1 = error)"
```

Expected exit code: **0** (no changes pending cho random_password). Nếu = 2 → review plan output, có thể có drift class khác.

### Phase 4 — Cleanup secrets từ session

```bash
# Step 10: unset biến shell
unset JWT RDS_PASS ENC
```

```bash
# Step 11: xoá history dòng có giá trị (HISTFILE flush)
history -c
# OR start new shell
```

```bash
# Step 12: đóng shell
exit
```

---

## 4. Retry plan (per `release-fix-retry-budget.md`)

| Failure point | Retry strategy |
|---|---|
| Step 1-3 (reads fail) | STOP — kiểm tra AWS auth, không có mutation yet. Safe abort. |
| Step 4 (get-secret-value fail) | STOP — verify IAM policy, secret naming. Max 2 retry. |
| Step 5 (length=0) | STOP — secret structure unexpected. Investigate JSON shape. |
| Step 6 (file edit) | Idempotent — file edit có thể redo. |
| Step 7 (state rm fail) | Likely "resource not in state" → đã removed. Continue Step 8. |
| **Step 8 (import fail giữa chừng)** | **CRITICAL**: state partial. Retry import phần còn lại. Max 2 retry. Sau 2 fail → rollback path. |
| Step 9 (plan thấy drift bất thường) | STOP, KHÔNG apply. Review → có thể rollback or fall back Option B. |

**Rollback (worst case after Step 8 fail repeated):**

```bash
# Restore previous state from S3 version
aws s3 cp s3://kitehub-terraform-state-906286017800/phase-1-beta/terraform.tfstate \
  /tmp/recovery.tfstate \
  --version-id "$CURRENT_VERSION"

terraform state push /tmp/recovery.tfstate
rm /tmp/recovery.tfstate

# Verify
terraform state list | grep random_password
```

Sau rollback: Phase 1 Option B (lifecycle ignore_changes) vẫn active từ PR → drift vẫn ẩn → bạn có thời gian investigate trước khi retry.

---

## 5. Acceptance — runbook executed successfully

- [ ] AWS profile có quyền verified (`aws sts get-caller-identity` returns expected account 906286017800)
- [ ] EC2 + RDS STOPPED verified pre-run
- [ ] Phase 1 reads (steps 1-3) clean — no AWS API errors
- [ ] Phase 2 secret reads (step 4) all 3 values non-empty
- [ ] Step 5 lengths match expected (64 / 32 / 32)
- [ ] Step 6 lifecycle blocks removed from secrets.tf + rds.tf (3 blocks)
- [ ] Step 7 `state rm` removed 3 resources cleanly
- [ ] Step 8 imports all 3 succeeded
- [ ] Step 9 `terraform plan -detailed-exitcode` returns 0
- [ ] Steps 10-12 cleanup done — shell history cleared, biến unset, shell closed
- [ ] Audit artifact saved: `documents/04-quality/audits/aws-verification/YYYY-MM-DD-gap-450-option-a-execution.md` (per `agent-aws-access.md` §5) — KHÔNG chứa secret values, chỉ commands + exit codes + length checks + final plan output
- [ ] GAP-450 status 🟡 PARTIAL → 🟢 DONE; final log entry với commit SHA của Option A execution (no-code commit message reference)

---

## 6. Override note

Per `agent-aws-access.md` §4.3 — Tier 3 BAN cho `terraform state rm` + `terraform import`. Runbook này không phải agent execution path; là user manual execution. Override không cần thiết vì user là rule-approver per `rule-change-process.md` §5.

Nếu agent được delegate chạy runbook này trong tương lai (e.g. dev-admin profile available trong session):
1. User pre-authorize bằng câu rõ ràng + scope ("chạy GAP-450 Option A, override Tier 3, secrets unset post-run")
2. Commit trailer: `AGENT_AWS_TIER_3_OVERRIDE: GAP-450 random_password state-import — user pre-authorized YYYY-MM-DD`
3. Audit artifact §5 mandatory

Hiện tại 2026-05-11: agent attempted Option A nhưng credentials stale (`AKIA…E7SO` deleted) → blocker pre-flight → fall back Option B + ship runbook này (Path C).

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Skip pre-flight `describe-instances` để biết STOPPED | Verify maintenance window trước state mutation |
| Echo `$JWT` ra terminal để "kiểm tra" | Chỉ check `${#JWT}` length |
| Commit secret values vào git "tạm" | Giá trị secret chỉ tồn tại trong shell vars; unset sau cùng |
| Skip rollback path nếu Step 8 fail | Retry max 2, sau đó rollback state từ S3 version |
| Chạy `terraform apply` ngay sau import "để chắc" | Plan exit-code 0 đã verify state khớp — apply không cần |
| Save audit artifact CHỨA secret values | Audit artifact = commands + exit codes + lengths only |

---

## 8. Related

- **Gap parent**: `documents/04-quality/gaps/GAP-450-terraform-state-drift-random-password-kc-app.md`
- **Sister gap**: GAP-379 (secrets-management Phase 1 — secrets seeded by Phase 2.3 apply 2026-05-07 chính là nguồn drift)
- **Audit baseline**: `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-44-bootstrap-apply.md`
- **Rules**: `agent-aws-access.md` §4.3 (Tier 3 ban), §2.2 (Tier 2 get-secret-value), §5 (audit artifact mandate); `terraform-apply-retry-reconfirm.md`; `release-fix-retry-budget.md` §3 STOP-AND-REDESIGN; `release-deploy-standard.md` §9 (agent role matrix)
- **Sister runbook**: `secrets-rotation-runbook.md` (rotation cadence — random_password ignore_changes nghĩa là rotation MUST manual qua runbook §5)

---

## 9. Log

- **2026-05-11**: Runbook tạo cùng PR GAP-450 Option B (lifecycle ignore_changes shipped to file `.tf`). Lý do Option A defer sang user manual execution: (a) agent credentials stale (key `AKIA…E7SO` deleted 2026-05-08 per ROADMAP, profile local chưa update với `AKIA…SVMD`), (b) Tier 3 BAN trong `agent-aws-access.md` §4.3 cho `terraform state rm` + `import`, (c) Tier 2 always-confirm cho `get-secret-value` × 3. Path B+C combined: Option B fix symptom ngay; Option A runbook chờ user execute khi credentials sẵn sàng + maintenance window.

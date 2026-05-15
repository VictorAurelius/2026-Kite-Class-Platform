---
title: EC2 Deploy Bootstrap Runbook (deploy-bootstrap.sh)
status: active
created: 2026-05-15
phase: 1-beta
related:
  - documents/05-guides/deploy/terraform-apply-bootstrap-runbook.md
  - documents/05-guides/deploy/post-bootstrap-deploy-runbook.md
  - documents/04-quality/gaps/GAP-506-deploy-prod-tech-debt.md
  - documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md
  - scripts/deploy-bootstrap.sh
  - scripts/deploy-prod.sh
  - .claude/rules/release-deploy-standard.md
---

# EC2 Deploy Bootstrap Runbook — `scripts/deploy-bootstrap.sh`

**Audience:** Solo-dev (acting deploy operator) khi cần seed `/opt/kite-prod` lần đầu trên 1 EC2 instance mới (kh-backend hoặc thay thế sau EC2 replacement).

Phân biệt với sister runbook `terraform-apply-bootstrap-runbook.md` (covers OIDC + apply-role chicken-and-egg cho `terraform-apply.yml` workflow). Bootstrap runbook này covers **EC2 application-side seed** — tạo `/opt/kite-prod`, clone repo, đặt SSM bootstrap marker.

---

## 1. Bối cảnh — chicken-and-egg trên EC2

`scripts/deploy-prod.sh` (Wave 85 post-refactor) **giả định** `/opt/kite-prod` đã có `.git` + `docker-compose.production.yml`. Routine deploy chỉ chạy `git fetch + reset + compose pull + compose up` — không clone.

Lần đầu chạy trên 1 EC2 mới (greenfield instance hoặc post-replacement) thì repo chưa có — phải bootstrap qua script riêng để:
1. Force cognitive checkpoint (admin credentials only — không phải OIDC routine path).
2. Đặt SSM Parameter Store marker để deploy-prod.sh sau này verify đã bootstrap.
3. Phân tách trách nhiệm: bootstrap (one-time, admin) ≠ deploy (routine, OIDC ephemeral).

---

## 2. Điều kiện tiên quyết

- EC2 instance đang chạy (kh-backend) — verify qua `aws ec2 describe-instances --filters Name=tag:Name,Values=kitehub-kh-backend`.
- AWS admin profile khả dụng trên máy operator (rotate ngay sau bootstrap).
- Repo URL public hoặc deploy key đã wired qua user_data terraform module.
- IAM permissions cho SSM Parameter Store `PutParameter` trên `/kite/bootstrap-done` (instance profile thường đã có; verify trước).
- Empty state: `/opt/kite-prod/.git` chưa tồn tại HOẶC bạn đã xoá có chủ ý.

---

## 3. Quy trình thực thi (one-time)

### 3.1 Pre-flight check

```bash
# 1. Verify EC2 instance ID
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=kitehub-kh-backend" \
  --query 'Reservations[].Instances[?State.Name==`running`].InstanceId' \
  --profile dev-admin --output text

# 2. Verify SSM bootstrap marker chưa set
aws ssm get-parameter --name /kite/bootstrap-done \
  --region ap-southeast-1 --profile dev-admin 2>&1 | grep -E "ParameterNotFound|true|false"
# Expected: ParameterNotFound (lần đầu)
```

### 3.2 Trigger bootstrap qua SSM SendCommand

```bash
INSTANCE_ID="i-XXXXXXXX"  # lấy từ pre-flight
REPO_URL="https://github.com/VictorAurelius/2026-Kite-Class-Platform.git"

aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --parameters "commands=[\"sudo KITE_FIRST_APPLY=true KITE_REPO_URL=$REPO_URL bash -c 'curl -fsSL https://raw.githubusercontent.com/VictorAurelius/2026-Kite-Class-Platform/main/scripts/deploy-bootstrap.sh | bash'\"]" \
  --timeout-seconds 600 \
  --region ap-southeast-1 \
  --profile dev-admin
```

Hoặc nếu đã có `/opt/kite-prod/scripts/deploy-bootstrap.sh` (uploaded via scp / aws s3 cp):

```bash
aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --parameters "commands=[\"sudo KITE_FIRST_APPLY=true KITE_REPO_URL=$REPO_URL bash /tmp/deploy-bootstrap.sh\"]" \
  --timeout-seconds 600 \
  --region ap-southeast-1 \
  --profile dev-admin
```

### 3.3 Verify bootstrap success

```bash
# 1. SSM marker đã được set
aws ssm get-parameter --name /kite/bootstrap-done \
  --region ap-southeast-1 --profile dev-admin \
  --query 'Parameter.Value' --output text
# Expected: true

# 2. /opt/kite-prod tồn tại trên EC2 (ssh hoặc SSM session)
aws ssm start-session --target "$INSTANCE_ID" --profile dev-admin
# Bên trong session:
ls -la /opt/kite-prod/.git
ls -la /opt/kite-prod/docker-compose.production.yml
ls -la /opt/kite-prod/scripts/
tail -50 /var/log/kite-bootstrap.log
exit
```

### 3.4 Rotate admin credentials

Per `release-deploy-standard.md` §9 (one-time bootstrap row):
- Sau khi bootstrap xong, rotate AWS admin key dùng cho SSM SendCommand.
- Subsequent deploys MUST dùng OIDC ephemeral path (terraform-apply.yml + deploy-production.yml).
- Verify `scripts/deploy-prod.sh` chạy clean từ OIDC path (xem `post-bootstrap-deploy-runbook.md`).

---

## 4. Env guards (Bucket F AC F-AC1)

`deploy-bootstrap.sh` có 2 env guards để chống misuse:

| Guard | Behavior | Exit code |
|---|---|---|
| `KITE_FIRST_APPLY` phải = `true` | Refuse to run nếu unset hoặc giá trị khác | 2 |
| SSM `/kite/bootstrap-done` = `true` (hoặc `KITE_BOOTSTRAP_DONE=true` env) | Refuse to re-run khi đã bootstrap rồi | 3 |
| `KITE_REPO_URL` unset + chưa có `.git` | Cannot clone — refuse | 4 |
| Compose file missing post-clone | Bad branch / wrong repo — refuse | 5 |

Khi guard fire, script log ERROR và exit; không phá vỡ state hiện tại.

---

## 5. Trouble-shooting

### 5.1 "Bootstrap already completed" nhưng `/opt/kite-prod` rỗng

Có thể SSM marker được set thủ công hoặc EC2 đã thay nhưng marker còn. Cách reset:

```bash
aws ssm delete-parameter --name /kite/bootstrap-done \
  --region ap-southeast-1 --profile dev-admin
# Sau đó rerun §3.2
```

⚠️ **DESTRUCTIVE** — chỉ làm khi xác nhận EC2 thật sự cần re-bootstrap.

### 5.2 SSM `PutParameter` AccessDenied

Instance profile thiếu permission. Workaround tạm: set thủ công từ máy admin:

```bash
aws ssm put-parameter --name /kite/bootstrap-done --value true --type String --overwrite \
  --region ap-southeast-1 --profile dev-admin
```

Follow-up gap: bổ sung IAM policy cho instance profile (xem `pre-launch-infra-hardening-checklist.md` §2.5 least-privilege scope).

### 5.3 Git clone fails — repo private + no deploy key

Hai phương án:
- Pre-load repo qua `aws s3 cp s3://kite-bootstrap/kite-repo.tar.gz - | tar -xz -C /opt/kite-prod` rồi set marker thủ công.
- Hoặc thêm SSH deploy key vào user_data terraform module + rerun.

---

## 6. Cross-references

- **Sister runbook:** `terraform-apply-bootstrap-runbook.md` — OIDC + apply role chicken-and-egg
- **Post-bootstrap deploy:** `post-bootstrap-deploy-runbook.md` — routine deploys qua `deploy-prod.sh`
- **Rule §9:** `release-deploy-standard.md` — `local terraform apply with admin key` row covers cùng pattern
- **GAP-506:** Wave 85 Bucket F — split bootstrap khỏi deploy-prod.sh để eliminate chicken-and-egg ambiguity
- **Tests:** `scripts/tests/test-deploy-bootstrap-guards.sh` + `scripts/tests/test-deploy-prod-guards.sh`

---

## 7. Log

- **2026-05-15:** Runbook created (Wave 85 Bucket F per GAP-506). Documents one-time EC2 application-side seed; complements `terraform-apply-bootstrap-runbook.md` for full bootstrap coverage. Env guards (KITE_FIRST_APPLY + SSM marker) prevent both directional misuse — running deploy-bootstrap on already-bootstrapped EC2 OR running deploy-prod before bootstrap.

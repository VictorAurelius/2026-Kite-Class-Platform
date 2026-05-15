---
title: Wave 81 Bucket F — Sửa fail-fast guard secrets qua AWS Secrets Manager (P0)
status: active
created: 2026-05-15
phase: phase-1-beta
wave: 81
trigger: post-deploy-smoke-503
---

# Runbook sửa fail-fast guard secrets — Wave 81 Bucket F

## Bối cảnh

Smoke test Wave 81 Bucket F phát hiện `kitehub-admin` + `kitehub-subscription` restart loop. Code sweep `grep -rnE "isDevDefault|MUST be set"` cho thấy **3 fail-fast guards** trong `kitehub-subscription`:

| # | Service class | Property | Env var | Dev default length | Wave 81 status |
|---|---|---|---|---:|---|
| 1 | `ChallengeTokenService` | `jwt.challenge-secret` | `JWT_CHALLENGE_SECRET` | 40 | ✅ Fixed (PR #1388, attempt 1) |
| 2 | `TotpSecretCipher` | `kitehub.auth.totp.encryption-key` | `TOTP_ENCRYPTION_KEY` | 36 | ❌ Cần fix (attempt 2) |
| 3 | `InvitationTokenService` | `kitehub.staff.invitation.signing-secret` | `KITEHUB_STAFF_INVITATION_SIGNING_SECRET` | 36 | ❌ Cần fix (attempt 2) |

Per `release-fix-retry-budget.md` §4 pivot matrix row "Same finding class but in different sub-scope" → STOP patching individual sub-scope, fix root scope (tất cả 3 fail-fast guards + KITE_VERSION default bug) trong **1 PR duy nhất** (PR #1389).

Cũng phát hiện thêm 1 **infrastructure bug** trong `scripts/fetch-secrets.sh` line 108:

```bash
KITE_VERSION=${KITE_VERSION:-v0.9.0-beta-staging.8}
```

→ Khi user chạy `sudo bash fetch-secrets.sh` trực tiếp (không qua `deploy-prod.sh` set env trước), `/etc/kite/.env` bị ghi đè với `KITE_VERSION=v0.9.0-beta-staging.8` (stale + sai prefix `v` không có trong ECR tag format). Hậu quả: `docker compose up` fail với `manifest unknown` vì image tag không tồn tại.

## Chiến lược — kiến trúc sạch (không leak secret qua chat)

Khác với attempt 1 ban đầu (sed inject vào `/etc/kite/.env` → secret leak chat 2 lần):

1. **Tạo secrets trong AWS Secrets Manager** qua `--secret-string file:///dev/stdin` (stdin pipe — secret chưa bao giờ ở chat)
2. **Update `fetch-secrets.sh`** thêm dòng pull 2 secret mới → script tự append vào `/etc/kite/.env`
3. **Sửa luôn KITE_VERSION default bug** trong cùng PR
4. **Commit thay đổi script** thành PR (audit trail)
5. **Re-run fetch-secrets.sh** trên EC2 (qua SSM) → populates `/etc/kite/.env`
6. **Force-recreate 2 services** với `--env-file /etc/kite/.env` → containers reload env mới
7. **Verify** health endpoint + log không còn fail-fast

Giá trị secret chỉ tồn tại ở:
- AWS Secrets Manager (KMS encrypted at rest)
- `/etc/kite/.env` trên EC2 (chmod 0640 root:docker)
- KHÔNG bao giờ ở: chat output, session JSONL, git history, local file

## Tiền điều kiện

- ✅ AWS CLI profile `dev-admin` configured
- ✅ EC2 kh-backend ID: `i-05d7af46d01436b96`
- ✅ Per `agent-aws-access.md` §4.3 — `create-secret` Tier 3 BANNED for agent, USER ACTION required
- ✅ Per `pre-launch-secrets-hardening-checklist.md` §2.1 — no hardcoded secrets in source, stdin pipe pattern

## Sweep results (gaps phát hiện trong session này)

Đã chạy:

```bash
grep -rnE "isDevDefault|MUST be set|productionProfile" \
  kitehub/*/src/main/java --include="*.java" | grep -v "test\|Test"
```

→ 4 services có `productionProfile` boolean (`ChallengeTokenService`, `TotpSecretCipher`, `InvitationTokenService`, `JwtKeyService`), trong đó 3 services có fail-fast `MUST be set` guard.

`JwtKeyService` không bắn guard vì `JWT_SECRET` đã được provision (random_password Terraform).

`AuthService` có `@PostConstruct` cho `VERIFICATION_BASE_URL` nhưng đã được override qua compose env (Wave 71 fix).

**Kết luận:** Tổng 3 fail-fast guards mandatory production value; attempt 1 fix 1/3 (PR #1388); attempt 2 fix 2/3 còn lại (PR #1389).

## Các bước thực hiện

### Bước 1 — USER ACTION: Tạo 2 secrets qua stdin pipe

```bash
# Secret #2: TOTP encryption key
openssl rand -base64 48 | tr -d '\n' | \
  aws secretsmanager create-secret \
    --name kitehub/production/totp-encryption-key \
    --description "TOTP secret encryption key (TotpSecretCipher fail-fast guard) — Wave 81 fix" \
    --secret-string file:///dev/stdin \
    --tags Key=Project,Value=Kite Key=Wave,Value=81 Key=Environment,Value=production \
    --profile dev-admin --region ap-southeast-1 \
    --query '[Name, ARN]' --output text

# Secret #3: Staff invitation signing secret
openssl rand -base64 48 | tr -d '\n' | \
  aws secretsmanager create-secret \
    --name kitehub/production/staff-invitation-signing-secret \
    --description "Staff invitation token signing secret (InvitationTokenService fail-fast guard) — Wave 81 fix" \
    --secret-string file:///dev/stdin \
    --tags Key=Project,Value=Kite Key=Wave,Value=81 Key=Environment,Value=production \
    --profile dev-admin --region ap-southeast-1 \
    --query '[Name, ARN]' --output text
```

Verify metadata:

```bash
aws secretsmanager describe-secret --secret-id kitehub/production/totp-encryption-key \
  --profile dev-admin --region ap-southeast-1 --query '[Name, CreatedDate]' --output text
aws secretsmanager describe-secret --secret-id kitehub/production/staff-invitation-signing-secret \
  --profile dev-admin --region ap-southeast-1 --query '[Name, CreatedDate]' --output text
```

Báo Claude: `done 2 secrets`. Claude tiếp Bước 2.

### Bước 2 — CLAUDE ACTION: PR #1389

(Bước này Claude tự làm — KHÔNG cần user action.)

Claude sẽ:
- Update `scripts/fetch-secrets.sh`:
  - Add `TOTP_ENCRYPTION_KEY=$(fetch_secret totp-encryption-key)` sau `JWT_CHALLENGE_SECRET`
  - Add `KITEHUB_STAFF_INVITATION_SIGNING_SECRET=$(fetch_secret staff-invitation-signing-secret)` ngay sau
  - Add 2 lines tương ứng vào block `ENV_FILE` write
  - **Sửa luôn `KITE_VERSION` default bug** line 108: đổi từ `v0.9.0-beta-staging.8` → `0.9.0-beta-staging.14` (current deployed tag, no `v` prefix per ECR convention) hoặc remove default để fail-fast
- Update `documents/02-architecture/env-vars-registry.md`:
  - Row 16: `TOTP_ENCRYPTION_KEY`
  - Row 17: `KITEHUB_STAFF_INVITATION_SIGNING_SECRET`
- Commit + admin-merge (CI Free Tier throttle context giống PR #1388; trailer `ADMIN_MERGE_OVERRIDE`)

### Bước 3 — USER ACTION: Re-run fetch-secrets.sh sau khi PR #1389 merge

```bash
EC2_ID="i-05d7af46d01436b96"

CMD_ID=$(aws ssm send-command --instance-ids "$EC2_ID" \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["cd /opt/kite-prod && sudo git pull origin main 2>&1 | tail -3", "sudo bash /opt/kite-prod/scripts/fetch-secrets.sh 2>&1 | tail -15", "echo ---verify---", "sudo grep -cE \"^(JWT_CHALLENGE_SECRET|TOTP_ENCRYPTION_KEY|KITEHUB_STAFF_INVITATION_SIGNING_SECRET)=\" /etc/kite/.env"]' \
  --profile dev-admin --region ap-southeast-1 \
  --query 'Command.CommandId' --output text)
echo "Command ID: $CMD_ID"

sleep 12
aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$EC2_ID" \
  --profile dev-admin --region ap-southeast-1 \
  --query 'StandardOutputContent' --output text
```

**Output kỳ vọng:** count `3` (3 env vars: JWT_CHALLENGE_SECRET + TOTP_ENCRYPTION_KEY + KITEHUB_STAFF_INVITATION_SIGNING_SECRET).

### Bước 4 — USER ACTION: Force-recreate 2 services (dùng `--env-file` flag)

```bash
EC2_ID="i-05d7af46d01436b96"

aws ssm send-command --instance-ids "$EC2_ID" \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["cd /opt/kite-prod && sudo docker compose --env-file /etc/kite/.env -f docker-compose.production.yml up -d --force-recreate --no-deps kitehub-admin kitehub-subscription 2>&1 | tail -15"]' \
  --profile dev-admin --region ap-southeast-1 \
  --query 'Command.CommandId' --output text

sleep 90
echo "Báo Claude: done force-recreate"
```

**Lưu ý:** PHẢI dùng `--env-file /etc/kite/.env` flag — docker-compose CLI variable substitution `${KITE_VERSION}` đọc shell env, không tự đọc `env_file` directive trong compose YAML. SSM shell session không có pre-loaded env.

**Lưu ý 2:** `docker-compose restart` KHÔNG re-load `/etc/kite/.env` — env_file đọc tại container CREATE time. PHẢI dùng `up -d --force-recreate` để container reload env.

### Bước 5 — CLAUDE ACTION: Verify

Claude sẽ:
- Poll `docker ps` → "Up Xs (healthy)" cho cả 2 services
- Curl `http://localhost:8086/actuator/health` + `http://localhost:8082/actuator/health` → 200
- Grep recent logs → KHÔNG còn `IllegalStateException.*production`
- Đóng Bucket F nếu tất cả PASS

## Bugs đã catch trong session này (toàn bộ)

| # | Bug | Severity | Resolution |
|---|---|:---:|---|
| 1 | `JWT_CHALLENGE_SECRET` không có trong fetch-secrets.sh | P0 | PR #1388 (attempt 1) ✅ Merged |
| 2 | `TOTP_ENCRYPTION_KEY` không có trong fetch-secrets.sh | P0 | PR #1389 (attempt 2) — Bước 2 |
| 3 | `KITEHUB_STAFF_INVITATION_SIGNING_SECRET` không có trong fetch-secrets.sh | P0 | PR #1389 (attempt 2) — Bước 2 |
| 4 | `KITE_VERSION` default trong fetch-secrets.sh = `v0.9.0-beta-staging.8` (stale + sai prefix) | P1 | PR #1389 cùng PR — đổi sang `0.9.0-beta-staging.14` |
| 5 | `docker-compose restart` không re-load env_file | Behavior note | Document trong runbook Bước 4 |
| 6 | SSM shell session không pre-load env_file vào shell scope | Behavior note | Document `--env-file` flag mandatory |

## Acceptable-defaults còn lại (24 findings — KHÔNG block Bucket F)

Audit `bash scripts/audit-env-coverage.sh` ra 24 findings, all pre-existing acceptable scope:

- `RABBITMQ_HOST=localhost` (3 services) — đã override qua `SPRING_RABBITMQ_HOST=kite-rabbitmq` (audit không detect SPRING_ prefix variant)
- `DATABASE_URL=jdbc:postgresql://localhost...` — đã override qua `SPRING_DATASOURCE_URL`
- `DATABASE_MASTER_*` / `DATABASE_ADMIN_URL` — admin bootstrap path, không runtime
- `S3_ENDPOINT=http://localhost:9000` (3 services) — Phase 1 BETA không dùng S3
- `STORAGE_S3_ENDPOINT` (kiteclass-core) — KiteClass không deploy Phase 1 BETA
- `CORE_SERVICE_URL` (kiteclass-gateway 7 hits) — KiteClass không deploy
- `APP_BASE_URL=http://localhost:3000` (kiteclass-gateway) — KiteClass không deploy
- `OPENAI_API_KEY=sk-mock-key-for-local-testing` (kitehub-branding) — AI defer Phase 2 per ADR-026
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` (kitehub-branding) — branding dùng EC2 instance profile, không access keys
- `CDN_DOMAIN=localhost:9100` (kitehub-branding) — Phase 1 BETA serve via Vercel/EC2

**Verdict:** không có new P0 bug trong 24 findings. Tracked trong `env-vars-registry.md` accepted-defaults section.

## So sánh attempt 1 vs attempt 2

| Attempt 1 (PR #1388) | Attempt 2 (PR #1389) |
|---|---|
| 1 secret (JWT_CHALLENGE_SECRET) | 2 secrets (TOTP + STAFF_INVITATION) + KITE_VERSION default fix |
| 1 service fail-fast remaining (TotpSecretCipher) | Sweep all 3 fail-fast guards before fix |
| Discovered restart vs force-recreate behavior gap | Document --env-file mandatory in SSM session |
| Discovered KITE_VERSION stale default | Fix same PR |

Per `release-fix-retry-budget.md` §4 — retry #2 trigger redesign: thay vì patch từng secret, sweep root scope + fix tất cả + KITE_VERSION trong cùng PR ✅.

## Cross-link

- `scripts/fetch-secrets.sh` — target update Bước 2
- `documents/02-architecture/env-vars-registry.md` — track row 16-17
- Wave 79 Bucket C — `ChallengeTokenService` fail-fast guard (PR #1367)
- Wave 72b — `TotpSecretCipher` introduced (GAP-516)
- Wave 78 — `InvitationTokenService` introduced (GAP-548 staff invitation)
- `.claude/rules/pre-launch-secrets-hardening-checklist.md` §2.4 — KMS encryption at rest
- `.claude/rules/agent-aws-access.md` §4.3 — Tier 3 `create-secret` USER ACTION
- `.claude/rules/release-fix-retry-budget.md` §4 — pivot to redesign root scope at retry #2
- `.claude/rules/admin-merge-discipline.md` §3 — `bash -n` + `shellcheck` clean = allow `--admin` under CI throttle context
- GAP-525 — credential rotation pattern (precedent)

## Follow-up (Wave 82+)

- File memory `feedback_credential_leak_pattern.md` document pattern chat-leak lặp lại
- Wave 82+ — enforce workflow xử lý secret qua skill chuyên dụng, mandatory template `--secret-string file://`
- Wave 82+ — self-hosted GitHub runner (Task #64) → bypass Free Tier minutes throttle → không cần admin-merge cho future PRs
- Wave 82+ — extend `audit-env-coverage.sh` để detect `SPRING_*` prefix override variants (reduce false-positives 24 → realistic count)

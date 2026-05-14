---
title: Credential Rotation Runbook — Incident 2026-05-13 (3 credentials)
status: active
created: 2026-05-14
last-reviewed: 2026-05-14
scope: GAP-525 incident-specific procedure cho 3 credentials leak trong session 2026-05-13
owner: solo-dev (acting platform-admin) — user-action execution
parent-runbook: documents/05-guides/operations/credential-rotation-runbook.md
incident-artifact: documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md
related-gaps: [GAP-525]
related-wrapper: scripts/rotate-leaked-credentials.sh
---

# Credential Rotation Runbook — Incident 2026-05-13

Hướng dẫn step-by-step cụ thể cho việc rotate 3 credentials đã surface trong session Claude Code ngày `2026-05-13` (per [GAP-525](../../04-quality/gaps/GAP-525-rotate-credentials-leaked-session-2026-05-13.md)). Runbook này là **bản incident-specific** — bám sát parent runbook [`credential-rotation-runbook.md`](./credential-rotation-runbook.md) §2 procedures, đồng thời thêm:

- Thông tin cụ thể về 3 credentials (vendor portal URL, secret ID, consumers)
- Verification commands có sẵn copy-paste
- Audit log skeleton paths
- Closure checklist cho GAP-525 PARTIAL → DONE

## Bối cảnh

Trong session `2026-05-13` (Wave 71b → 71c-meta-Phase-2), 3 production credentials surfaced trong Anthropic API conversation context:

| # | Credential | Cách surface | Sensitivity |
|---|---|---|---|
| 1 | `admin@kitehub.me` production password | `aws secretsmanager get-secret-value` (Tier 2 user-confirmed) per user request "log cho tôi pass của admin" để test self-test flow | 🔴 critical |
| 2 | Cloudflare API token `cfut_B5d8tYY...` (read-only "Read all resources" scope) | User paste trực tiếp vào chat để enable DNS audit per `third-party-platform-automation-discovery.md` | 🟠 moderate |
| 3 | Resend API key `re_hoMkdPyz_NNZikknUkX7Ne3ovGJ7LuEkJ` | User paste trực tiếp vào chat (Option B "paste in chat" chọn thay vì Option A user-runs-locally) | 🟠 moderate |

Transcript local file: `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/7517e076-d175-4fb2-bbdf-23bb355763d9.jsonl`. Anthropic API traffic giữ values trong context window — KHÔNG thể recall retroactively. **Rotation là biện pháp mitigation duy nhất.**

Phòng vệ chiều sâu (defense-in-depth): rotate tất cả 3 dù chưa confirm external compromise.

## Pre-flight

Per parent runbook §3, xác nhận trước khi bắt đầu:

- [ ] Đã đọc parent runbook `credential-rotation-runbook.md` §2.1 + §2.3
- [ ] AWS CLI installed + profile `dev-admin` available (`aws sts get-caller-identity --profile dev-admin` returns identity)
- [ ] Có quyền vào AWS Secrets Manager (verify với `aws secretsmanager describe-secret --secret-id kitehub/production/admin-seed-password --profile dev-admin`)
- [ ] Có quyền vendor portal:
  - Cloudflare: https://dash.cloudflare.com/profile/api-tokens
  - Resend: https://resend.com/api-keys
- [ ] Browser đang đăng nhập 2 vendor portal trên
- [ ] Audit log skeleton đã được wrapper script tạo (chạy `bash scripts/rotate-leaked-credentials.sh --cred=all` để tạo)

## Thứ tự rotation đề xuất

Theo parent incident artifact §"Next steps":

1. **#1 Admin password trước** — sensitivity cao nhất, blast radius rộng nhất
2. **#3 Resend API key tiếp** — email path exercise frequent, smoke-test feedback nhanh
3. **#2 Cloudflare token cuối** — read-only blast radius thấp nhất

## Wrapper script

Sử dụng `scripts/rotate-leaked-credentials.sh` để xem hướng dẫn từng credential + tạo audit log skeleton:

```bash
# Bước 1: reachability check (default dry-run)
bash scripts/rotate-leaked-credentials.sh

# Bước 2: xem hướng dẫn từng credential khi sẵn sàng rotate
bash scripts/rotate-leaked-credentials.sh --cred=admin-password
bash scripts/rotate-leaked-credentials.sh --cred=resend-api-key
bash scripts/rotate-leaked-credentials.sh --cred=cloudflare-token

# HOẶC xem tất cả cùng lúc
bash scripts/rotate-leaked-credentials.sh --cred=all
```

Wrapper không thực hiện mutation — chỉ in instructions + tạo audit log skeleton. User chạy các lệnh AWS / vendor portal action.

---

## #1 Admin password

**Bám parent runbook §2.1.** Tóm tắt cụ thể cho incident này:

| Field | Value |
|---|---|
| Secret ID | `kitehub/production/admin-seed-password` |
| AWS profile | `dev-admin` (region `ap-southeast-1`) |
| Consumer | `scripts/seed-direct-sql.sh` + admin login UI tại `https://kitehub.me/login` |
| Downtime | zero (re-seed atomic) |
| Estimated time | ~10 phút |

### Steps

```bash
# 1. Snapshot version-id trước rotation
aws secretsmanager describe-secret \
  --secret-id kitehub/production/admin-seed-password \
  --profile dev-admin --region ap-southeast-1 \
  --query 'VersionIdsToStages'

# 2a. OPTION A (preferred) — Terraform re-roll
cd infrastructure/terraform-aws
terraform apply \
  -target=random_password.seed_admin_password \
  -replace=random_password.seed_admin_password
terraform apply -target=aws_secretsmanager_secret_version.admin_seed_password

# 2b. OPTION B — Manual put-secret-value (emergency)
NEW_PASSWORD=$(openssl rand -base64 24)
aws secretsmanager put-secret-value \
  --secret-id kitehub/production/admin-seed-password \
  --secret-string "$NEW_PASSWORD" \
  --profile dev-admin --region ap-southeast-1
unset NEW_PASSWORD

# 3. Re-seed admin account
bash scripts/seed-direct-sql.sh --target-env=production --user=admin@kitehub.me
```

### Verification

```bash
# Browser test
# - Mở https://kitehub.me/login
# - Login admin@kitehub.me + password mới → HTTP 200 + redirect /admin

# Service log check
docker compose logs kitehub-subscription --tail=50 | grep -iE "error|auth|seed"
# Kỳ vọng: zero auth errors post re-seed

# Secret version progression
aws secretsmanager describe-secret \
  --secret-id kitehub/production/admin-seed-password \
  --profile dev-admin --region ap-southeast-1 \
  --query 'VersionIdsToStages'
# Kỳ vọng: version mới với stage AWSCURRENT; version cũ với AWSPREVIOUS
```

### Recovery

Nếu rotation fail, revert AWSCURRENT về previous version:

```bash
aws secretsmanager update-secret-version-stage \
  --secret-id kitehub/production/admin-seed-password \
  --version-stage AWSCURRENT \
  --move-to-version-id <previous-version-id> \
  --remove-from-version-id <new-version-id> \
  --profile dev-admin --region ap-southeast-1
```

---

## #2 Cloudflare API token (read-only)

**Bám parent runbook §2.3 + §2.3.1.** Tóm tắt:

| Field | Value |
|---|---|
| Vendor portal | https://dash.cloudflare.com/profile/api-tokens |
| Scope cần preserve | "Read all resources" (Account/Settings Read + Zone Read + DNS Read) |
| Secret ID (optional) | `kitehub/production/cloudflare-api-token` (tạo mới nếu chưa có) |
| Consumer | Local DNS audit scripts, Cloudflare MCP server config (user-session), tương lai Workers deploy CI |
| Downtime | zero (read-only) |
| Estimated time | ~5 phút |

### Steps

1. **Tạo token mới** ở https://dash.cloudflare.com/profile/api-tokens
   - Template: "Read all resources"
   - TTL: 90 days (quarterly cadence)
   - Click "Create Token" → COPY (chỉ hiển thị 1 lần)

2. **Verify token mới** trước khi revoke cũ:

   ```bash
   curl -sH "Authorization: Bearer $NEW_TOKEN" \
     https://api.cloudflare.com/client/v4/user/tokens/verify | jq '.success'
   # Kỳ vọng: true
   ```

3. **Lưu vào Secrets Manager** (optional — nếu có service consumer):

   ```bash
   aws secretsmanager put-secret-value \
     --secret-id kitehub/production/cloudflare-api-token \
     --secret-string "$NEW_TOKEN" \
     --profile dev-admin --region ap-southeast-1
   unset NEW_TOKEN
   ```

   Nếu secret chưa tồn tại:

   ```bash
   aws secretsmanager create-secret \
     --name kitehub/production/cloudflare-api-token \
     --description "Cloudflare API token — Read all resources scope" \
     --secret-string "$NEW_TOKEN" \
     --profile dev-admin --region ap-southeast-1
   ```

4. **Update MCP/local config** — nếu Claude Code Cloudflare MCP server config dùng token cũ, cập nhật token mới.

5. **Revoke token CŨ** ở Cloudflare dashboard — tìm token prefix `cfut_B5d8tYY...` → Click "..." → "Delete".
   ⚠️ Irreversible — chỉ làm sau khi BƯỚC 2 verify token mới OK.

### Verification

```bash
# Token cũ bị reject
curl -sH "Authorization: Bearer $OLD_TOKEN" \
  https://api.cloudflare.com/client/v4/user/tokens/verify | jq '.success'
# Kỳ vọng: false (hoặc HTTP 401)

# Cloudflare audit log
# Browser: https://dash.cloudflare.com/?to=/account/audit-log
# Kỳ vọng: thấy event "API Token Deleted" với timestamp hôm nay
```

---

## #3 Resend API key (sending)

**Bám parent runbook §2.3 + §2.3.2.** Tóm tắt:

| Field | Value |
|---|---|
| Vendor portal | https://resend.com/api-keys |
| Scope cần preserve | "Sending access" — RESTRICT to `kitehub.me` domain |
| Secret ID | `kitehub/production/resend-api-key` |
| Consumer | `kitehub-email` service (`ResendClient` đọc tại startup); `EmailService` transactional path |
| Downtime | ~30s (kitehub-email container restart) |
| Estimated time | ~10 phút |

### Steps

1. **Tạo API key mới** ở https://resend.com/api-keys
   - Name: `kitehub-production-v2` (hoặc rename theo convention)
   - Permission: "Sending access" — restrict to `kitehub.me`
   - Click "Create" → COPY (chỉ hiển thị 1 lần)

2. **Verify key mới** với test email:

   ```bash
   curl -X POST https://api.resend.com/emails \
     -H "Authorization: Bearer $NEW_KEY" \
     -H "Content-Type: application/json" \
     -d '{"from":"noreply@kitehub.me","to":"dev@kitehub.me","subject":"rotation test","html":"ok"}'
   # Kỳ vọng: HTTP 200 + email_id trong response
   ```

   Verify dashboard: https://resend.com/emails → email vừa gửi status "delivered".

3. **Lưu vào AWS Secrets Manager**:

   ```bash
   aws secretsmanager put-secret-value \
     --secret-id kitehub/production/resend-api-key \
     --secret-string "$NEW_KEY" \
     --profile dev-admin --region ap-southeast-1
   unset NEW_KEY
   ```

4. **Redeploy kitehub-email** (pickup new key):

   ```bash
   # Option (a): via deploy workflow
   gh workflow run deploy-production.yml -f confirm=APPLY

   # Option (b): SSH/SSM direct (nếu access)
   docker compose restart kitehub-email
   docker compose logs kitehub-email --tail=20 | grep -iE "resend|email|init"
   # Kỳ vọng: zero "401" / "unauthorized" entries
   ```

5. **Smoke test internal endpoint** — trigger transactional email path (signup confirm, password reset, etc.) → verify delivered.

6. **Revoke key CŨ** ở Resend dashboard — tìm prefix `re_hoMkdPyz...` → "..." → "Revoke".
   ⚠️ Irreversible.

### Verification

```bash
# Key cũ bị reject
curl -sX POST https://api.resend.com/emails \
  -H "Authorization: Bearer $OLD_KEY" \
  -H "Content-Type: application/json" \
  -d '{"from":"noreply@kitehub.me","to":"dev@kitehub.me","subject":"old key test","html":"ok"}'
# Kỳ vọng: HTTP 401
```

---

## Closure checklist (sau khi 3 credentials đều verified)

Per [`gap-done-discipline.md`](../../../.claude/rules/gap-done-discipline.md) §2, GAP-525 flip → DONE chỉ khi mọi AC verified. Steps:

1. [ ] **3 audit skeleton files** (do wrapper script tạo) đã fill xong rotation-status table + `status: complete`:
   - `documents/04-quality/audits/credential-rotation/YYYY-MM-DD-credential-rotation-admin-password.md`
   - `documents/04-quality/audits/credential-rotation/YYYY-MM-DD-credential-rotation-cloudflare-token.md`
   - `documents/04-quality/audits/credential-rotation/YYYY-MM-DD-credential-rotation-resend-api-key.md`

2. [ ] **Parent incident artifact** rotation-status table 3 rows → `verified`:
   `documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md`

3. [ ] **GAP-525 file** Status flip:
   `documents/04-quality/gaps/GAP-525-rotate-credentials-leaked-session-2026-05-13.md`
   - Status: 🟡 PARTIAL → 🟢 DONE
   - AC checkboxes 1-4 → `[x]`

4. [ ] **gap-status.csv** row update — sửa GAP-525 row:
   - `status`: `PARTIAL` → `DONE`
   - `completion_pct`: `50` → `100`
   - `last_verified`: cập nhật về ngày completion
   - `notes`: cập nhật "Rotation complete"

5. [ ] **Optional cleanup** — delete hoặc redact local transcript:
   `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/7517e076-d175-4fb2-bbdf-23bb355763d9.jsonl`

6. [ ] **Memory entry** per [`incident-to-rule-pipeline.md`](../../../.claude/rules/incident-to-rule-pipeline.md) Stage 5:
   `~/.claude/projects/.../memory/feedback_credential_leak_session_2026_05_13.md`
   - Mô tả "Option B paste in chat" pattern (rule)
   - Cross-link parent runbook + dedicated runbook (this file)
   - Update `MEMORY.md` index

7. [ ] **Closure PR commit trailer** (PR có thể là docs-only edit của gap file + CSV):

   ```
   GAP-525_USER_ROTATED: admin-pwd YYYY-MM-DD / cloudflare YYYY-MM-DD / resend YYYY-MM-DD
   ```

   Format: 3 credential class + ISO date completion mỗi loại. Xác nhận user đã thực sự hoàn tất rotation outside Claude session.

## Related

- **Parent runbook** (general procedure): [`credential-rotation-runbook.md`](./credential-rotation-runbook.md)
- **Incident artifact** (Wave 72a): [`2026-05-14-wave-72a-3-credentials.md`](../../04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md)
- **Gap**: [GAP-525](../../04-quality/gaps/GAP-525-rotate-credentials-leaked-session-2026-05-13.md)
- **Wrapper**: [`scripts/rotate-leaked-credentials.sh`](../../../scripts/rotate-leaked-credentials.sh)
- **Sister runbooks**:
  - [`secrets-rotation-runbook.md`](./secrets-rotation-runbook.md) — quarterly cadence
  - [`secrets-seeding-runbook.md`](../deploy/secrets-seeding-runbook.md) — initial seeding
  - [`jwt-rotation-runbook.md`](./jwt-rotation-runbook.md) — JWT dual-key (per GAP-520)
  - [`incident-response-runbook.md`](./incident-response-runbook.md) — comms patterns
- **Rules**:
  - `.claude/rules/agent-aws-access.md` §2.2 (banned reveals), §4.3 (Tier 3 mutations user-execute)
  - `.claude/rules/agent-action-bias.md` §3 row 5 (destructive shared-state)
  - `.claude/rules/gap-done-discipline.md` §2 (DONE flip criteria)
  - `.claude/rules/incident-to-rule-pipeline.md` Stage 5 (memory + index)
  - `.claude/rules/pre-handoff-self-test-completeness.md` §2.4 (admin-flow verify)

## Log

- **2026-05-14** Wave 77 Bucket C — Runbook dedicated copy created. Bám sát parent `credential-rotation-runbook.md` §2.1 + §2.3 + §2.3.1 + §2.3.2 với incident-specific values (secret IDs, vendor portals, consumers, time estimates). Paired same-PR với `scripts/rotate-leaked-credentials.sh` automation wrapper + GAP-525 flip PARTIAL 50% → 85% + gap-status.csv update. User executes rotation OUTSIDE Claude per `agent-action-bias.md` §3 row 5; closure trailer `GAP-525_USER_ROTATED: admin-pwd YYYY-MM-DD / cloudflare YYYY-MM-DD / resend YYYY-MM-DD` flips gap → DONE.

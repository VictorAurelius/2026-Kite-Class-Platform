---
title: Wave 81 Bucket C — Credential Rotation Commands (copy-paste runbook)
status: active
created: 2026-05-15
phase: phase-1-beta
wave: 81
gaps: [GAP-525]
---

# Wave 81 Bucket C — Rotation Commands

**Mục đích:** Runbook copy-paste cho user rotate 2 credentials còn lại (cred #1 `seed-admin-password` đã tồn tại TF-managed, no action). Sau khi hoàn tất → GAP-525 flip DONE 100%.

**Threat model:** Real cred leak xác nhận trong session JSONL files (verified 2026-05-15 — `03M05pTouiE7GPJaAgTcOmaIW2W0dG9Ldm97ph5gOR` Cloudflare token + 42 password-paste matches). Local delete không đủ — rotate mandatory.

**Per `agent-aws-access.md` §4.3:** `put-secret-value` + vendor portal revoke = Tier 3 — USER thực hiện, Claude verify post-action via Tier 1 read-only.

---

## CRED #2 — cloudflare-api-token (~10 phút)

### Bước 1 — Snapshot trạng thái secret hiện tại (Tier 1, Claude verify post-rotate)

```bash
aws secretsmanager describe-secret \
  --secret-id kitehub/production/cloudflare-api-token \
  --profile dev-admin --region ap-southeast-1 \
  --query 'LastChangedDate'
```

### Bước 2 — Tạo token mới trên Cloudflare dashboard

1. Mở browser → https://dash.cloudflare.com/profile/api-tokens
2. Tìm token cũ (tên kiểu `kitehub-dns-edit`) → click **"Roll"** (xoay token, giữ tên + scope)
   - HOẶC: **"Create Token"** → template **"Edit zone DNS"** → zone `kitehub.me` → Continue → Create
3. **COPY** giá trị token (chỉ hiển thị 1 lần!)

> 💡 **Roll behavior:** click `Roll` = generate token mới + auto-revoke token cũ ngay lập tức (no grace period).
> Vì services Wave 81 chưa deployed (Bucket D pending) → SAFE bấm Roll hiện tại.
> Bước 5 (manual revoke) → SKIP vì Roll đã làm rồi.

### Bước 3 — Lưu vào AWS Secrets Manager

```bash
NEW_TOKEN="<dán-token-mới-vào-đây>"

aws secretsmanager put-secret-value \
  --secret-id kitehub/production/cloudflare-api-token \
  --secret-string "$NEW_TOKEN" \
  --profile dev-admin --region ap-southeast-1

unset NEW_TOKEN  # QUAN TRỌNG — xóa khỏi shell history
```

### Bước 4 — Kiểm tra token mới hoạt động (Tier 1 read-only)

```bash
# Re-export tạm để test (LƯU Ý: vẫn cần unset sau)
NEW_TOKEN="<dán-lại-token-mới>"

curl -s -X GET "https://api.cloudflare.com/client/v4/zones" \
  -H "Authorization: Bearer $NEW_TOKEN" \
  -H "Content-Type: application/json" | jq '.success'
# Kỳ vọng: true

unset NEW_TOKEN
```

### Bước 5 — Thu hồi token CŨ trên Cloudflare dashboard

> ⏩ **SKIP nếu Bước 2 đã dùng "Roll"** — Roll tự động revoke token cũ. Bỏ qua Bước 5, đi thẳng Bước 6.

Chỉ làm Bước 5 nếu Bước 2 dùng "Create Token" path (tạo token mới riêng biệt, không Roll):

1. Dashboard → API Tokens
2. Tìm token CŨ → click **"..."** → **"Delete"**
3. ⚠️ KHÔNG hoàn tác được — chỉ làm sau khi Bước 4 verify token mới OK

### Bước 6 — Báo Claude qua chat: "done cf"

Claude sẽ:
- Verify `LastChangedDate` = hôm nay qua Tier 1 `describe-secret`
- Cập nhật audit artifact `documents/04-quality/audits/credential-rotation/2026-05-15-credential-rotation-cloudflare-token.md` — 6 rows status: complete
- Emit hướng dẫn cred #3 resend-api-key

---

## CRED #3 — resend-api-key (~10 phút)

### Bước 1 — Snapshot trạng thái secret hiện tại

```bash
aws secretsmanager describe-secret \
  --secret-id kitehub/production/resend-api-key \
  --profile dev-admin --region ap-southeast-1 \
  --query 'LastChangedDate'
```

### Bước 2 — Tạo API key mới trên Resend dashboard

1. Mở browser → https://resend.com/api-keys
2. Click **"Create API Key"**
3. Name: `kitehub-production-v2` (hoặc tương tự để phân biệt với key cũ)
4. Permission: **Full access** (hoặc giới hạn `Sending access` nếu Resend hỗ trợ)
5. Domain: `kitehub.me` (nếu Resend yêu cầu)
6. Click **Create**
7. **COPY** key value (chỉ hiển thị 1 lần, prefix `re_`)

### Bước 3 — Lưu vào AWS Secrets Manager

```bash
NEW_KEY="<dán-key-mới-vào-đây>"

aws secretsmanager put-secret-value \
  --secret-id kitehub/production/resend-api-key \
  --secret-string "$NEW_KEY" \
  --profile dev-admin --region ap-southeast-1

unset NEW_KEY  # QUAN TRỌNG
```

### Bước 4 — Kiểm tra key mới hoạt động (Tier 1)

```bash
NEW_KEY="<dán-lại-key-mới>"

curl -s -X GET "https://api.resend.com/domains" \
  -H "Authorization: Bearer $NEW_KEY" \
  -H "Content-Type: application/json" | jq '.'
# Kỳ vọng: response list domains, không có "error"

unset NEW_KEY
```

### Bước 5 — Redeploy kitehub-email service (pickup key mới)

**Lưu ý:** Bước này phụ thuộc Bucket D deploy. Nếu chưa deploy services (ALB targets unhealthy), defer Bước 5 + 6 + 7 sau khi Bucket D ship. Trong trường hợp đó:

- Cred #3 rotation `put-secret-value` đã xong ở Bước 3 ✅
- Resend dashboard revoke key CŨ defer sau khi Bucket D verified kitehub-email service đọc key mới
- Báo Claude: "cred #3 putsecret done, defer revoke" để document trong audit

### Bước 6 — Smoke test transactional email (sau Bucket D deploy)

- Trigger 1 email test (vd: signup flow) → verify inbox arrive + Resend dashboard "delivered"

### Bước 7 — Revoke key CŨ trên Resend dashboard

1. Dashboard → API Keys → tìm key CŨ (prefix `re_hoMkdPyz...`)
2. Click **"..."** → **"Revoke"**
3. ⚠️ IRREVERSIBLE — chỉ làm sau khi smoke test Bước 6 OK

### Bước 8 — Báo Claude: "done resend"

Claude sẽ:
- Verify `LastChangedDate` = hôm nay
- Cập nhật audit `documents/04-quality/audits/credential-rotation/2026-05-15-credential-rotation-resend-api-key.md`
- Flip GAP-525 PARTIAL 85% → DONE 100% trong gap-status.csv
- Append commit trailer mẫu cho Wave 81 closure PR:
  ```
  GAP-525_USER_ROTATED: admin-pwd 2026-05-13 (TF-managed) / cloudflare 2026-05-15 / resend 2026-05-15
  ```

---

## Optional — Cleanup sau khi rotate xong

Sau khi BOTH cred #2 + #3 verified working + old keys revoked (defense in depth):

```bash
# Delete jsonl files chứa cred cũ (cred cũ đã chết sau revoke nên không còn risk; xóa = vệ sinh)
rm ~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/{0ba0bb7a,113a7ab9,5615b9b4,636cea3c,6ede7606}-*.jsonl

# Paranoid mode — zero-overwrite (tốn ~30 giây, ext4 không tự zero)
# Chỉ làm nếu vẫn lo backup recovery
# shred -uvz ~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/*.jsonl
```

⚠️ KHÔNG xóa session hiện tại đang chạy (file mới nhất, mtime hôm nay). Để Claude finish session bình thường.

---

## Recovery (nếu rotation fail)

AWS Secrets Manager tự giữ previous version 30 ngày. Revert về cũ:

```bash
# List versions
aws secretsmanager describe-secret \
  --secret-id kitehub/production/cloudflare-api-token \
  --profile dev-admin --region ap-southeast-1 \
  --query 'VersionIdsToStages'

# Promote previous version back to AWSCURRENT
aws secretsmanager update-secret-version-stage \
  --secret-id kitehub/production/cloudflare-api-token \
  --version-stage AWSCURRENT \
  --move-to-version-id <previous-version-id> \
  --remove-from-version-id <current-version-id> \
  --profile dev-admin --region ap-southeast-1
```

---

## References

- `documents/04-quality/audits/aws-verification/2026-05-15-wave-81-bucket-c-dry-run-analysis.md` — terraform dry-run findings
- `documents/04-quality/gaps/GAP-525-rotate-credentials-leaked-session-2026-05-13.md`
- `scripts/rotate-leaked-credentials.sh` — wrapper với 8-step Vietnamese guidance per cred
- `.claude/rules/agent-aws-access.md` §4.3 — Tier 3 ban (user-only)
- `documents/05-guides/operations/credential-rotation-2026-05-13.md` — incident artifact

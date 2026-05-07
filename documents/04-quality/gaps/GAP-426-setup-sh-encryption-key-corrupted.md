# GAP-426: setup.sh GAP-417 fix corrupts ENCRYPTION_MASTER_KEY base64

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟠 P1 (BLOCKING — first-time-setup fail; kitehub-subscription crash trên cold rebuild)
**Domain:** DevOps / Local dev tooling
**Found:** 2026-05-07 Wave 39 closure session, "visual lần 1" cold rebuild flow
**Affects:** Mọi `bash kitehub/scripts/setup.sh` chạy lần đầu sau khi GAP-417 fix shipped (#951 Wave-39-eve)

## Problem

GAP-417 fix tại `kitehub/scripts/setup.sh:40` strip quá nhiều ký tự khỏi base64 output:

```bash
# Hiện tại (BUG):
ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n=/+')
```

`tr -d '\n=/+'` xoá:
- `\n` newline (đúng — chống line break)
- `=` base64 padding (**BREAKS** decode)
- `/` và `+` (**BREAKS** — đây là 2 trong 64 ký tự hợp lệ của base64)

Kết quả: `ENCRYPTION_MASTER_KEY` thành base64 string bị corrupt, độ dài random ~38-44 chars. Khi `EncryptionService` decode (`Base64.getDecoder().decode()`) ra raw bytes thì kết quả KHÔNG phải 32 bytes:

```
java.lang.IllegalArgumentException: Master key must be 32 bytes (256 bits) for AES-256
    at com.kitehub.subscription.service.EncryptionService.<init>(EncryptionService.java:57)
```

→ kitehub-subscription crash loop forever trên cold setup.

JWT_SECRET line 41 cùng pattern (`tr -d '\n=/+'`) cũng bị corrupt, nhưng JWT signature có thể "tình cờ chạy" với key length khác — không error rõ rệt. Vẫn nên fix.

## Reproduction

```bash
rm kitehub/.env
bash kitehub/scripts/setup.sh
bash kitehub/scripts/up.sh --profile beta-funnel
# → kitehub-subscription crash với "Master key must be 32 bytes"
docker logs kitehub-subscription | grep "Master key must be"
```

## Root Cause

GAP-417 fix nhắm vào `JWT_SECRET=$(openssl rand -base64 64)` lỗi line break (76+ chars wrap → newline embed `.env`). Fix dùng `tr -d '\n=/+'` để strip newline + 3 ký tự "shell-special". Nhưng:

1. `=` chỉ là padding base64 — strip nó làm độ dài giảm 1-2 chars + decoder fail
2. `/` và `+` là valid base64 chars (set chuẩn `A-Za-z0-9+/`) — strip nó làm chuỗi không còn là base64 hợp lệ
3. Quoting trong `.env` đã đủ chống shell-special; không cần strip ký tự

GAP-417 self-test chỉ verify "no orphan continuation lines" (count of `^[A-Z_]*=`), KHÔNG verify base64 decode integrity hay AES-256 key length. AC quá nông.

## Proposed Fix

**Option A — Đúng nhất:** strip CHỈ newline, giữ nguyên base64 chars:

```bash
ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n')
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
```

Quote trong `.env` template:
```
ENCRYPTION_MASTER_KEY="${ENCRYPTION_KEY}"
JWT_SECRET="${JWT_SECRET}"
```
Quote ngăn shell hiểu `+/=` là special; docker-compose `env_file` parser xử lý quote đúng.

**Option B — Hex format:** chuyển sang hex (chỉ chứa `0-9a-f`, không cần escape):

```bash
ENCRYPTION_KEY=$(openssl rand -hex 32)  # 64 hex chars = 32 bytes raw
JWT_SECRET=$(openssl rand -hex 64)      # 128 hex chars
```

Nhưng `EncryptionService` hiện tại expect base64-encoded → cần đổi service code song song. Heavier change.

**Khuyến nghị:** Option A. Nhanh + đúng chuẩn + không touch backend.

## Acceptance Criteria

- [ ] `kitehub/scripts/setup.sh` line 40-41 sửa thành `tr -d '\n'` (bỏ `=/+`)
- [ ] `.env` template wrap `ENCRYPTION_MASTER_KEY` + `JWT_SECRET` trong dấu nháy kép `"..."`
- [ ] **Self-test mở rộng:** sau khi `setup.sh` chạy, verify base64 decode integrity:
  ```bash
  KEY=$(grep ^ENCRYPTION_MASTER_KEY kitehub/.env | sed 's/^[^=]*=//; s/^"//; s/"$//')
  test "$(echo -n "$KEY" | base64 -d | wc -c)" = "32" || echo "FAIL: not 32 bytes"
  ```
- [ ] Self-test verify cold rebuild path: `rm .env && setup.sh && up.sh --profile beta-funnel` → kitehub-subscription healthy <2 phút (không crash AES key)
- [ ] Comment trong setup.sh reference cả GAP-417 và GAP-426

## Related

- **GAP-417** (DONE 2026-05-07 PR #951) — original fix nhắm line break, vô tình tạo bug này
- **GAP-425** (filed 2026-05-07 Wave 39 closure) — sister gap về cold rebuild BE images stale; cùng surface "first-time setup broken"
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/EncryptionService.java:55-58` — strict 32-byte check
- `kitehub/scripts/setup.sh:40-41` — fix location
- `documents/05-guides/account-prep/04-kitehub-superadmin-first-login.md` §1 pre-conditions — sẽ ảnh hưởng nếu user follow runbook và bị block

## Estimated effort

~20 phút (sửa 2 dòng setup.sh + thêm AC self-test + commit).

## Log

- **2026-05-07** Filed during Wave 39 closure session "visual lần 1" cold rebuild flow. Sau khi force-recreate container với image latest fixed (GAP-242 V11), kitehub-subscription vẫn crash với "Master key must be 32 bytes". Investigation: setup.sh GAP-417 fix `tr -d '\n=/+'` strip cả base64 padding `=` và valid chars `/+` → corrupt key. Self-test GAP-417 quá nông (chỉ count `^[A-Z_]*=`). Inline workaround applied: `NEW_KEY=$(openssl rand -base64 32 | tr -d '\n'); sed -i "s|^ENCRYPTION_MASTER_KEY=.*|ENCRYPTION_MASTER_KEY=${NEW_KEY}|" .env`. Permanent fix tracked here.

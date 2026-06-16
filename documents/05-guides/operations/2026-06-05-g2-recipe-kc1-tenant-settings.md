---
title: G2 Human Test Recipe — KC-1 Tenant settings (branding + preferences)
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign G2 handoff cho luồng KC-1 (Owner settings)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-04-flow-kc1-tenant-provisioning-settings.md
---

# G2 Recipe — KC-1 Tenant settings (branding + preferences)

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn (Owner) tự test trên local stack: mở `/settings` của KiteClass → sửa branding → reload thấy giá trị mới → xác nhận tab "Tùy chọn" **KHÔNG** hiển thị cho Owner (đã fix GAP-979). Xác nhận trải nghiệm thật khớp G1 agent walk.

**Prereq state (verify lại 2026-06-08 — DB đã reseed, seed refs cập nhật):**
- Stack UP (`bash kitehub/scripts/up.sh --profile full`); kiteclass-core rebuild fresh Flyway **V94** (GAP-1066 V87 attendance-status normalize fix — trước đó core crash-loop).
- Tenant test: **sky-education** (instance `e8ff87e1-69fc-4842-a263-7385c68b4ffb`) — đã có branding persisted (org "Trung tâm Anh ngữ Sky Education").
- GAP-979 fix đã ship (ẩn tab Tùy chọn cho OWNER).

**Thời lượng:** ~10 phút.

## 2. Setup

- Mở **browser** + **DevTools** → tab **Network** (filter `settings`).
- (Tùy chọn) Terminal để verify DB:
  ```bash
  docker exec kite-postgres psql -U kitehub -d kiteclass_shared \
    -c "SELECT display_name FROM branding WHERE instance_id='e8ff87e1-69fc-4842-a263-7385c68b4ffb';"
  ```
- Verify stack sống:
  ```bash
  curl -s -o /dev/null -w "%{http_code}\n" -H "X-Instance-Subdomain: sky-education" \
    http://localhost:9000/api/v1/settings/branding   # expect 200
  ```

## 3. Các bước

### Bước 1 — Đăng nhập Owner

- **Hành động:** Mở **`http://localhost:3000/login`** → đăng nhập **`owner@skyedu.vn`** / **`SkyEdu@2026`** (Owner thật của tenant sky-education = `instances.owner_id` user `3c659096`; password reset 2026-06-08 cho G2). **LƯU Ý:** PHẢI dùng `owner@skyedu.vn`, KHÔNG phải `owner.sky@test.vn` — chỉ owner khớp `instances.owner_id` mới được nhúng `tenantId` claim vào JWT (Wave 104 GAP-704) → tenant resolve được; nếu sai owner thì mọi call KiteClass 400 (xem GAP-1068).
- **✅ Verify (2026-06-08):** login owner@skyedu.vn → JWT chứa `"tenantId":"e8ff87e1..."`; dashboard `/teachers` `/students` `/courses` → 200 không cần gắn header tay.
- **⚠️ Dashboard có 2 widget 404** (`/api/v1/classes` + `/api/v1/invoices` — BE chưa expose list endpoint root, GAP-1069) → cosmetic, KHÔNG chặn KC-1 settings.
- **⚠️ Nếu `ERR_EMPTY_RESPONSE` (bất kỳ route :3000):** port-forward host→container stale sau rebuild service (compose-up). Fix: `docker restart kiteclass-frontend`, chờ ~12s, F5 (GAP-1067). Verify: `curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/login` → 200.
- **✅ Kỳ vọng:** Login thành công → redirect vào dashboard KiteClass; Network tab `POST /api/auth/login` → HTTP 200 + JWT (role `OWNER`). Đã verify BE/gateway 2026-06-08: login 200 qua `:9000`.
- **⚠️ Sad path:** Sai password → báo lỗi rõ ("Email hoặc mật khẩu không đúng"), KHÔNG redirect. Nếu gặp trang "Dịch vụ tạm ngưng" → gateway `authCircuitBreaker` vừa mở (do request lỗi trước đó), chờ ~30s rồi thử lại.

### Bước 2 — Mở trang Cài đặt

- **Hành động:** Vào `http://localhost:3000/settings` (hoặc click menu "Cài đặt").
- **✅ Kỳ vọng:** Trang render với tabs **Branding** + **Theme preview**. Tab **"Tùy chọn"** **KHÔNG** xuất hiện (Owner — fix GAP-979).
- **⚠️ Sad path:** Nếu thấy tab "Tùy chọn" → fix GAP-979 chưa deploy đúng (báo lại).

### Bước 3 — Xem branding hiện tại

- **Hành động:** Tab **Branding** → xem các trường (tên hiển thị, tagline, 3 màu, contact).
- **✅ Kỳ vọng:** Hiển thị data thật: displayName "Trung tâm Anh ngữ Sky Education", primaryColor `#E8590C`...; Network `GET /api/v1/settings/branding` → 200 (`{"success":true,"data":{...}}`).

### Bước 4 — Sửa 1 trường + lưu

- **Hành động:** Đổi **Tên hiển thị** (vd thêm " ✦") → nhấn **Lưu**.
- **✅ Kỳ vọng:** Toast "Đã lưu"; Network `PUT /api/v1/settings/branding` → HTTP 200.
- **⚠️ Sad path:** Nhập màu sai format (vd "red") → 400 `VALIDATION_ERROR` + báo lỗi field màu.
- **🔍 Verify (tùy chọn):** chạy lại query DB ở §2 → `display_name` = giá trị mới.

### Bước 5 — Reload xác nhận persist

- **Hành động:** F5 reload `/settings`.
- **✅ Kỳ vọng:** Tên hiển thị mới vẫn còn (đã persist DB, không mất).

## 4. Sad path quick checks

- Sai mật khẩu login → lỗi rõ, không redirect.
- PUT branding thiếu màu / màu sai hex → 400 validation, không lưu.
- Mở `/settings` khi chưa login → redirect `/login`.

## 5. Báo kết quả

Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** → Claude flip campaign → ✅ G1+G2 chờ G3 (production parity).
- ⚠️ **MOSTLY PASS** (cosmetic) → catalog gap polish.
- 🔴 **BLOCKING** (vd tab Tùy chọn vẫn hiện / branding không lưu) → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm screenshot + Network/console error.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|---|---|
| `/settings` redirect `/login` dù đã login | JWT hết hạn → login lại |
| Branding GET 400 | Thiếu tenant context — verify đang trong tenant sky-education |
| Tab "Tùy chọn" vẫn hiện cho Owner | FE chưa rebuild fix GAP-979 → `bash kitehub/scripts/rebuild.sh ... kiteclass-frontend` |

**G3 production parity (Phase tiếp):** verify trên RDS thật (Flyway V86 migrate sạch) + FE serve qua subdomain wildcard + JWT tenantId resolve đúng tenant. Gated GAP-612 (AWS account suspended).


---

## 🔄 Re-walk update 2026-06-16 (agent headless browser-walk, nip.io)

- **Verdict:** ✅ PASS (14 bước, 0 product bug).\n- **Credential:** owner login đi qua KH SSO fallback `/api/auth/login` (tenant-auth `/api/v1/tenant-auth/login` trả 401 by-design vì owner tenant_id=NULL — KHÔNG phải lỗi).\n- **Seed drift:** displayName thực = "Trung tâm cô Đỗ Lan Khánh" (không phải "Sky Education").\n- **Access:** dùng nip.io `sky-education.127.0.0.1.nip.io:3000` (CẤM localhost:3000?tenant=).\n- Pre-walk predictions refuted: version-history 200 (không 403), logo MinIO OK, /classes /invoices 200.\n- Cosmetic: themeConfigJson embed tên cũ (GAP-1461, 0 user impact).

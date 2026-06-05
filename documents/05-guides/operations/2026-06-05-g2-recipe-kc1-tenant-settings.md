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

**Prereq state:**
- Stack UP (đã chạy `bash kitehub/scripts/up.sh --profile full`), image KiteClass đã rebuild fresh V86 (session 2026-06-05).
- Tenant test: **sky-education** (instance `0edaee10`, TRIAL) — đã có branding persisted.
- GAP-979 fix đã ship (ẩn tab Tùy chọn cho OWNER).

**Thời lượng:** ~10 phút.

## 2. Setup

- Mở **browser** + **DevTools** → tab **Network** (filter `settings`).
- (Tùy chọn) Terminal để verify DB:
  ```bash
  docker exec kite-postgres psql -U kitehub -d kiteclass_shared \
    -c "SELECT display_name FROM branding WHERE instance_id='0edaee10-2d13-44be-9151-12b78b7c5fd4';"
  ```
- Verify stack sống:
  ```bash
  curl -s -o /dev/null -w "%{http_code}\n" -H "X-Instance-Subdomain: sky-education" \
    http://localhost:9000/api/v1/settings/branding   # expect 200
  ```

## 3. Các bước

### Bước 1 — Đăng nhập Owner

- **Hành động:** Mở `http://localhost:3000` → đăng nhập `owner@skyedu.vn` / `SkyEdu@2026`.
- **✅ Kỳ vọng:** Login thành công → redirect vào dashboard KiteClass; Network tab `POST /api/auth/login` → HTTP 200 + JWT.
- **⚠️ Sad path:** Sai password → báo lỗi rõ ("Email hoặc mật khẩu không đúng"), KHÔNG redirect.

### Bước 2 — Mở trang Cài đặt

- **Hành động:** Vào `http://localhost:3000/settings` (hoặc click menu "Cài đặt").
- **✅ Kỳ vọng:** Trang render với tabs **Branding** + **Theme preview**. Tab **"Tùy chọn"** **KHÔNG** xuất hiện (Owner — fix GAP-979).
- **⚠️ Sad path:** Nếu thấy tab "Tùy chọn" → fix GAP-979 chưa deploy đúng (báo lại).

### Bước 3 — Xem branding hiện tại

- **Hành động:** Tab **Branding** → xem các trường (tên hiển thị, tagline, 3 màu, contact).
- **✅ Kỳ vọng:** Hiển thị data thật: displayName "Trung tâm Anh ngữ Sky Education 2026", primaryColor `#EA580C`...; Network `GET /api/v1/settings/branding` → 200.

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

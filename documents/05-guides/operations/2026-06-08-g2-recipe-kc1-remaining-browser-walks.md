---
title: G2 Recipe — KC-1 G2 browser-walk còn lại (session-isolation + logo + host→tenant landing)
audience: dev
date: 2026-06-08
flow: KC-1 G2 remaining
gaps: [GAP-1074, GAP-1072, GAP-1073, GAP-811]
stack_state_required: full (local Docker, all healthy)
supersedes: 2026-06-08-g2-recipe-kc1-session-isolation.md (mở rộng, credentials cập nhật sau re-seed)
---

# G2 Recipe — KC-1 browser-walk còn lại (2026-06-08)

> **Mục đích:** Hướng dẫn bạn (human) test trực tiếp trên **browser thật** 4 hạng mục còn lại của KC-1 G2 mà coordinator KHÔNG tự verify được (curl ≠ browser — đúng lỗ hổng `g1-browser-walk-before-flip`). Mỗi bước có **action → kỳ vọng → sad-path → cách verify**.

## 0. Tóm tắt — test gì

| # | Gap | Test gì | FE | Thời lượng |
|---|---|---|---|---|
| A | **GAP-1074** | Cross-tab session-isolation (login tab 1 → tab 2 không bắt login lại) | kiteclass `:3000` | ~5 phút |
| B | **GAP-1072 + GAP-1073** | Upload logo từ Settings → success + logo preview render | kiteclass `:3000` | ~5 phút |
| C | **GAP-811** | Host→tenant landing (subdomain → branding đúng tenant) | kitehub `:3001` | ~5 phút (cần sửa `/etc/hosts`) |

---

## 1. Setup (BẮT BUỘC trước khi walk)

### 1.1 Verify stack healthy

```bash
docker ps --format '{{.Names}}\t{{.Status}}' | grep kite
```

Kỳ vọng: **tất cả `healthy`** (postgres/redis/rabbitmq/minio/mailhog + gateway + subscription + branding + email + admin + kiteclass-core/frontend + kitehub-frontend). Nếu có service `Exited`/`unhealthy` → chạy `cd kitehub && bash scripts/up.sh --force-recreate` rồi đợi ~1 phút.

### 1.2 Credentials (đã verify HTTP 200 — 2026-06-08 sau re-seed)

| Tenant | Email | Password | Subdomain | Instance UUID |
|---|---|---|---|---|
| **A — Sky Education** | `owner@skyedu.vn` | `SkyEdu@2026` | `sky-education` | `0edaee10-2d13-44be-9151-12b78b7c5fd4` |
| **B — Sky Test** | `owner.test@test.vn` | `Test@1234` | `skytest` / `sky-test` | `aaaabbbb-0000-0000-0000-000000000001` |

> ⚠️ Credential `walk.owner+bucketb@skyedu.vn` trong handoff CŨ KHÔNG còn (re-seed sau rebuild). Dùng bảng trên.

### 1.3 URL

- **kiteclass-frontend (dashboard KC):** http://localhost:3000
- **kitehub-frontend (landing KH, cho mục C):** http://localhost:3001
- MailHog (xem email): http://localhost:8025

---

## 2. Mảng A — GAP-1074 Cross-tab session-isolation (kiteclass :3000)

**Cơ chế:** token lưu `localStorage['kc:<tenantId>:accessToken']` (cross-tab persist) + `sessionStorage['kc:currentTenant']` (per-tab bind) + `localStorage['kc:activeTenant']` (fresh-tab pointer).

### Bước A1 — Login tab 1 (tenant A)
- **Action:** Mở **Tab 1** → http://localhost:3000/login → đăng nhập `owner@skyedu.vn` / `SkyEdu@2026`.
- **Kỳ vọng:** Redirect `/dashboard` (hoặc `/`), hiện dashboard tenant A (tên "Trung tâm Anh ngữ Sky Education" ở header/sidebar).
- **Sad-path:** Nếu 400/trắng trang → SAI credential hoặc tenant chưa resolve → xem §5.

### Bước A2 — Cross-tab PERSIST (AC chính)
- **Action:** Giữ tab 1 đang login. Mở **Tab 2 MỚI** (cùng browser, Ctrl+T) → gõ http://localhost:3000/dashboard (hoặc `/`).
- **Kỳ vọng:** ✅ Tab 2 **KHÔNG bắt login lại** → vào thẳng dashboard tenant A, **đúng data tenant A** (cùng tên trung tâm, cùng lớp/học viên).
- **Verify:** DevTools (F12) → Application → Local Storage → `localhost:3000` → thấy key `kc:0edaee10...:accessToken`. Session Storage → `kc:currentTenant` = `0edaee10...`.
- **Sad-path:** Nếu tab 2 đá về `/login` → persist FAIL (token không cross-tab). Báo FAIL.

### Bước A3 — (Tùy chọn) Cross-tenant isolation
> Lưu ý: trên `localhost` đây là **dev-fallback path** (production tách bằng subdomain). Test phụ.
- **Action:** Trong tab 2, logout. Login `owner.test@test.vn` / `Test@1234` (tenant B). Quay lại **tab 1** (vẫn tenant A) → F5 reload.
- **Kỳ vọng:** Tab 1 vẫn là tenant A (logout/login tenant B ở tab 2 KHÔNG đá tab 1 sang B hoặc logout tab 1). Mỗi tab giữ namespace riêng.
- **Verify:** DevTools mỗi tab → `kc:currentTenant` khác nhau (tab1=A, tab2=B); local storage có CẢ 2 namespace `kc:<A>:*` + `kc:<B>:*` không clobber nhau.
- **Sad-path:** Tab 1 bị logout / đổi sang tenant B → isolation leak. Báo FAIL.

---

## 3. Mảng B — GAP-1072 + GAP-1073 Upload logo + render (kiteclass :3000)

### Bước B1 — Vào Settings branding
- **Action:** Login tenant A (`owner@skyedu.vn`) → vào http://localhost:3000/settings (hoặc menu Cài đặt → Thương hiệu/Branding).
- **Kỳ vọng:** Trang Settings render đầy đủ shell (header + sidebar + footer — KHÔNG vỡ layout per GAP-1071), có khu vực upload logo.

### Bước B2 — Upload logo (GAP-1073)
- **Action:** Chọn file logo (PNG/JPG bất kỳ, < 2MB) → upload.
- **Kỳ vọng:** ✅ Upload **success** (toast "đã lưu" / "thành công"), KHÔNG lỗi đỏ. (Trước fix: upload fail vì `Content-Type` đè multipart boundary.)
- **Verify:** DevTools → Network → request upload (`/branding/.../upload` hoặc `/assets`) → **Status 200/201**, Request Headers `Content-Type: multipart/form-data; boundary=...` (CÓ boundary).
- **Sad-path:** 400/415 "Unsupported Media Type" hoặc "Required part 'logo' not present" → boundary vẫn thiếu → báo FAIL (regression GAP-1073).

### Bước B3 — Logo render (GAP-1072)
- **Action:** Sau upload, F5 reload trang Settings (hoặc xem khu "Logo hiện tại").
- **Kỳ vọng:** ✅ Logo vừa upload **render được** (không vỡ ảnh, không "Không có tệp"). (GAP-1072: presigned URL regenerate-on-read → không hết hạn.)
- **Verify:** Right-click logo → Open image → ảnh load (presigned URL còn hạn). Network: GET logo URL → 200.
- **Sad-path:** Ảnh vỡ / 403 "Request has expired" (presigned hết hạn) → GAP-1072 regen chưa ăn → báo FAIL.

---

## 4. Mảng C — GAP-811 Host→tenant landing (kitehub :3001)

**Cơ chế:** middleware `kitehub-frontend/src/middleware.ts` đọc `Host` → extract subdomain slug (cần host ≥3 phần, vd `sky-education.kitehub.local`) → gọi BE `by-subdomain` → inject `x-tenant-id` → landing render branding đúng tenant. `localhost`/IP → pass-through fallback.

### Bước C1 — Sửa /etc/hosts (một lần)
- **Action (chạy lệnh — cần sudo):**
  ```bash
  echo "127.0.0.1 sky-education.kitehub.local" | sudo tee -a /etc/hosts
  ```
  > Mẹo: gõ `! echo "127.0.0.1 sky-education.kitehub.local" | sudo tee -a /etc/hosts` trong prompt session để chạy trực tiếp.
- **Kỳ vọng:** `/etc/hosts` có dòng map. Verify: `ping -c1 sky-education.kitehub.local` → `127.0.0.1`.

### Bước C2 — Browse subdomain → branding tenant A
- **Action:** Mở browser → http://sky-education.kitehub.local:3001/
- **Kỳ vọng:** ✅ Landing render **branding Sky Education** (tên "Trung tâm Anh ngữ Sky Education" ở nav/footer, KHÔNG phải fallback tenant `11111111-...`).
- **Verify:**
  - DevTools → Network → request landing → resolve đúng tenant.
  - Container log: `docker logs kitehub-frontend 2>&1 | grep -iE "x-tenant|resolved|sky"` → thấy slug `sky-education` → UUID `0edaee10...`.
  - BE endpoint (đã verify live): `curl -s http://localhost:9000/api/v1/public/tenants/by-subdomain/sky-education` → `{"id":"0edaee10...","name":"Trung tâm Anh ngữ Sky Education"}`.
- **Sad-path:** Landing hiện fallback branding generic / tenant `11111111` → middleware không resolve → báo FAIL.

### Bước C3 — Regression dev path
- **Action:** Mở http://localhost:3001/ (không subdomain).
- **Kỳ vọng:** ✅ Pass-through (landing default/fallback, KHÔNG crash). `?tenant=sky-education` override vẫn hoạt động.
- **Sad-path:** Crash / 500 → middleware fallback không graceful → báo FAIL.

---

## 5. Sad-path tổng hợp (gặp lỗi → check trước khi báo)

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử |
|---|---|---|
| Login 400 mọi call | Sai credential, HOẶC owner không phải `instances.owner_id` (per GAP-1068) | Dùng đúng bảng §1.2 (owner@skyedu.vn / owner.test@test.vn) |
| Tab 2 đá `/login` | localStorage cross-tab persist fail | DevTools → check key `kc:<tenant>:accessToken` tồn tại |
| Upload 415/400 "part not present" | multipart boundary thiếu | Network → check `Content-Type: multipart/form-data; boundary=` |
| Logo 403 "expired" | presigned URL hết hạn (GAP-1072) | F5 reload (regen-on-read); nếu vẫn 403 → FAIL |
| Subdomain → fallback branding | middleware không resolve / `/etc/hosts` thiếu | `ping sky-education.kitehub.local`; `docker logs kitehub-frontend` |
| Service unhealthy | infra/stack chưa up đủ | `cd kitehub && bash scripts/up.sh --force-recreate` |

---

## 6. Báo kết quả (4 outcomes)

Sau khi walk, báo coordinator theo 1 trong 4:

1. **✅ ALL PASS** — cả A/B/C pass → coordinator flip GAP-1074/1072/1073/811 → DONE (sau khi đối chiếu AC).
2. **⚠️ PARTIAL** — nêu rõ Mảng nào pass, Mảng nào fail (vd "A pass, B fail bước B2 upload 415"). Coordinator giữ gap PARTIAL + fix.
3. **❌ BLOCKED** — không walk được vì stack/setup (vd service unhealthy, /etc/hosts không sửa được). Nêu blocker.
4. **🔄 BUG MỚI** — phát hiện bug ngoài scope 4 gap → coordinator file gap mới (per `discovery-to-gap-inline-filing`).

Format gọn: `Mảng A: ✅ | Mảng B: ⚠️ (B2 fail HTTP 415) | Mảng C: ✅` + screenshot/Network nếu fail.

---

## 7. Troubleshooting + G3 preview

### Troubleshooting nhanh
- **Reset stack sạch:** `cd kitehub && bash scripts/down.sh && bash scripts/up.sh --force-recreate`
- **Xem log 1 service:** `docker logs <kitehub-subscription|kiteclass-core|kitehub-frontend> --tail 50`
- **Redis blacklist (GAP-1075 đã DONE, tham khảo):** `docker exec kite-redis redis-cli --scan --pattern 'refresh-blacklist:*'`
- **Gỡ /etc/hosts sau test:** `sudo sed -i '/sky-education.kitehub.local/d' /etc/hosts`

### G3 preview (production-parity — coordinator làm sau G2)
Sau khi G2 pass, G3 verify qua **gateway :9000 với JWT mint** (per memory `project_g3_walk_recipe`): cross-tenant IDOR (token tenant A → resource tenant B → 403), production env-var parity. Mảng C (host→tenant) trên production = subdomain thật `sky-education.kitehub.me` (không cần /etc/hosts).

---

**Liên kết:**
- Gaps: `documents/04-quality/gaps/phase-1-beta/GAP-1074*.md`, `GAP-1072*.md`, `GAP-1073*.md`, `GAP-811*.md`
- Design canonical: `documents/02-architecture/tenant-domain-landing-architecture.md`
- Rule: `g1-browser-walk-before-flip.md`, `g2-handoff-md-mandate.md`, `small-gap-inline-fix.md`

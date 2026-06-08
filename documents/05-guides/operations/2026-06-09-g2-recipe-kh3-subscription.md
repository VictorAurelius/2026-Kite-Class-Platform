---
title: G2 Human Test Recipe — KH-3 Subscription create + trial→paid migration
audience: dev
created: 2026-06-09
scope: Flow Verification Campaign G2 handoff for KH-3 (subscription create → upgrade → payment → PAID)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-04-flow-kh3-subscription-trial-paid.md
  - documents/03-planning/waves/wave-2026-06-07-p0-prov-1-kc1-kh3-closure.md
---

# G2 Recipe — KH-3 Subscription create + trial→paid

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Con người tự test luồng KH-3 trên local stack — Owner (tenant TRIAL) tạo subscription gói trả phí → sinh yêu cầu thanh toán (VietQR/SePay) → thanh toán confirm (SePay Test Mode HOẶC admin manual) → subscription flip `PAID`. Xác nhận trải nghiệm thật đúng, không chỉ tin agent walk.

**Prereq:**
- Stack UP đủ: `kite-gateway` (`:9000`) + `kite-postgres` + `kitehub-subscription` + `kitehub-frontend` (`:3001`) + `kite-mailhog` (`:8025`).
- Owner test tenant TRIAL: `g2test-an-8@example.com` (tenant `g2test-an-8`, tạo từ KH-1 chain). Password = mật khẩu bạn tự đặt khi register-via-invite ở KH-1 G2. Nếu quên → §Troubleshooting tạo Owner mới qua KH-1 HOẶC reset.
- G1 ✅ PASS: `POST /api/platform/subscriptions` LIVE 201 (GAP-942, Wave p0-prov-1). SePay webhook whitelisted Test Mode (GAP-975/976).

**FE port:** KH-3 chạy trên **kitehub-frontend `:3001`** (KHÔNG phải `:3000`). An toàn walk song song khi wave landing-100 rebuild `:3000`.

**Thời lượng:** ~12-15 phút.

---

## 2. Setup

1. Mở Chrome + DevTools → tab **Network** (filter `Fetch/XHR`) — để soi request tới gateway `:9000`.
2. Mở tab phụ **MailHog** `http://localhost:8025` — verify email notification (trial/payment confirm).
3. (Tùy chọn) Terminal DB query verify side-effect:
   ```bash
   docker exec kite-postgres psql -U kitehub -d kitehub -c \
     "SELECT id, instance_id, tier, status FROM subscriptions ORDER BY created_at DESC LIMIT 5;"
   ```
4. Verify state đầu: Owner tenant `g2test-an-8` đang `TRIAL`:
   ```bash
   docker exec kite-postgres psql -U kitehub -d kitehub -c \
     "SELECT subdomain, status FROM instances WHERE subdomain='g2test-an-8';"
   ```

---

## 3. Các bước test

### Bước 1 — Đăng nhập Owner
- **Hành động:** Mở `http://localhost:3001` → đăng nhập `g2test-an-8@example.com` + password. (Nếu Owner bật 2FA → nhập mã 2FA.)
- **✅ Kỳ vọng (PASS):** `POST /api/auth/login` → HTTP 200 + JWT trong response; redirect tới dashboard Owner (`/dashboard` hoặc `/billing`). DevTools Console clean.
- **⚠️ Sad path:** sai password → HTTP 401 + FE hiển thị error message rõ ràng (KHÔNG silent fail / KHÔNG redirect loop).

### Bước 2 — Xem trạng thái subscription (TRIAL)
- **Hành động:** Vào `http://localhost:3001/billing`.
- **✅ Kỳ vọng:** Page render trạng thái gói hiện tại = TRIAL (hoặc "Dùng thử" + đếm ngược 14 ngày). Network: `GET /api/platform/subscriptions/instance/{instanceId}/active` → 200. KHÔNG blank page / KHÔNG 404.
- **🔍 Verify:** đối chiếu tier hiển thị với DB query §2 bước 4.

### Bước 3 — Chọn gói trả phí + tạo subscription
- **Hành động:** Click "Nâng cấp" → vào `/billing/upgrade` → chọn tier `BASIC` (hoặc `PREMIUM`) → click nút tạo/xác nhận nâng cấp.
- **✅ Kỳ vọng:** `POST /api/platform/subscriptions` (UC-SUB-01) → HTTP **201** + body chứa subscription + pendingPaymentId. FE redirect `/billing/payment/{pendingPaymentId}` hiển thị **mã QR VietQR/SePay** + nội dung chuyển khoản.
- **⚠️ Sad path:** chọn lại tier hiện tại (không phải upgrade) → FE chặn / nút disable, KHÔNG tạo subscription rác.
- **🔍 Verify:** DB query §2 → row subscription mới status `PENDING`/`PENDING_PAYMENT` (chưa PAID).

### Bước 4 — Thanh toán (chọn 1 trong 2 đường)

**4a — SePay Test Mode (auto webhook):**
- **Hành động:** Thực hiện thanh toán test theo hướng dẫn SePay sandbox (hoặc trigger webhook test mode).
- **✅ Kỳ vọng:** `POST /api/platform/webhooks/payment` → 200; payment record `CONFIRMED`; subscription flip `PAID`.

**4b — Admin manual confirm (đường chắc chắn cho G2):**
- **Hành động:** Mở tab phụ đăng nhập **PlatformAdmin** → vào `http://localhost:3001/admin/payments` → tìm payment `PENDING` của `g2test-an-8` → click "Xác nhận thanh toán".
- **✅ Kỳ vọng:** `POST /api/platform/admin/payments/...` → 200; payment → `CONFIRMED`; subscription flip `PAID`.
- **⚠️ Sad path:** non-admin gọi admin confirm → 403.

### Bước 5 — Xác nhận PAID + email
- **Hành động:** Quay lại Owner `/billing` → refresh.
- **✅ Kỳ vọng:** Gói hiển thị `BASIC` (paid) + status active, hết banner trial. Network `GET .../active` → 200 tier=BASIC.
- **🔍 Verify:**
  - DB: subscription status = `PAID`, tier = `BASIC`.
  - MailHog `:8025`: có email xác nhận thanh toán / kích hoạt gói gửi tới `g2test-an-8@example.com`.

---

## 4. Sad path quick checks (gom)
- Đăng nhập sai password → 401 + message rõ.
- Tạo subscription khi đã PAID cùng tier → FE chặn (không tạo trùng).
- Non-admin truy cập `/admin/payments` → 403 / redirect.
- Reload trang payment `/billing/payment/{id}` sau khi đã confirm → hiển thị trạng thái đã thanh toán (không tạo payment mới).

---

## 5. Báo kết quả

**Khi G2 xong, báo lại 1 trong 4:**
- ✅ **FULL PASS** → Claude flip campaign KH-3 → ✅ G1+G2 (G3 production-parity check tiếp).
- ⚠️ **MOSTLY PASS + cosmetic** (vd email thiếu polish — GAP-974 đã biết) → catalog gap polish, vẫn coi luồng thông.
- 🔴 **BLOCKING ISSUE** (subscription không flip PAID / payment không confirm / 500) → catalog blocker + Claude fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm screenshot DevTools Network/Console + error.

---

## 6. Troubleshooting + G3 preview

**Troubleshooting:**
| Triệu chứng | Quick fix |
|---|---|
| `:3001` ERR_EMPTY_RESPONSE | `bash kitehub/scripts/status.sh` check `kitehub-frontend` Up; nếu stale → `bash kitehub/scripts/rebuild.sh kitehub-frontend` |
| Quên password Owner | Tạo Owner mới qua KH-1 chain (recipe `2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md`) HOẶC reset trong DB (dev) |
| `GET .../active` 404 | Owner chưa có instance/subscription — verify DB §2; có thể cần tạo subscription Bước 3 trước |
| Admin payments trống | Đảm bảo Bước 3 đã tạo payment PENDING; filter theo tenant `g2test-an-8` |
| SePay webhook 401 | Test Mode whitelist (GAP-975/976) — verify gateway whitelist `/api/platform/webhooks/payment` |

**G3 production-parity preview (Claude + Dev, sau G2):**
- SePay provider config reachable trên production (không chỉ Test Mode).
- Admin confirm qua real workflow + gateway JWT→header authority chain `:9000`.
- SES email delivery thật (không chỉ MailHog).
- DB state `PAID` persist trên Postgres+Flyway+RLS (không H2).

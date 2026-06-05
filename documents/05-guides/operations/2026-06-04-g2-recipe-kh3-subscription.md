---
title: Công thức G2 — KH-3 Đăng ký gói trả phí + chuyển khoản SePay (VietQR real-transfer)
audience: dev
created: 2026-06-04
scope: Bàn giao G2 thủ công cho luồng KH-3 (subscription create + trial→paid) thuộc Chiến dịch Xác minh Luồng
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-04-flow-kh3-subscription-trial-paid.md
  - documents/04-quality/audits/persona-review/2026-06-04-pre-walk-flow-kh3-subscription.md
  - documents/05-guides/operations/2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md
  - documents/01-business/kitehub/subscription-billing/api-contract.md
---

# Công thức G2 — KH-3 Đăng ký gói trả phí (trial → paid)

> **Trạng thái:** Cập nhật cho Wave flow-kh3-3 — thêm **biến thể SePay real-transfer** (đường KH-3 canonical mới, sau khi Wave flow-kh3-2 / PR #2165 tích hợp SePay webhook). Đường mock-mode admin-confirm cũ giữ làm phương án dự phòng (xem §"Phương án mock-mode").

## Mục tiêu G2

Bạn (người dùng) đóng vai **Owner Tuấn** (tenant `g2test-an-8` đã có sẵn từ chuỗi KH-1 + KH-2c) đi hết đường nâng cấp gói BASIC qua **chuyển khoản ngân hàng thật** (10.000đ tượng trưng chế độ Beta), từ chỗ thấy nút "Nâng cấp", chọn gói, quét QR VietQR bằng app ngân hàng, chuyển khoản thật, tới khi SePay webhook tự xác nhận → gói chuyển sang ACTIVE + nhận email kích hoạt. Mục đích là xác nhận G1 (do tôi tự walk) không bỏ sót lỗi hiển thị / sai cảm nhận người dùng / chỗ Owner sẽ bối rối.

So với phương án cũ (admin xác nhận thủ công qua curl), đường SePay loại bỏ bước admin confirm — webhook tự match theo `txnRef` (format `KH3SUB<8 hex>`) và flip Payment COMPLETED.

Thời lượng ước tính: ~20-25 phút (gồm cả setup SePay one-time lần đầu ~10 phút; lần sau ~15 phút).

## Điều kiện đầu vào

- Stack Docker local đang chạy (đã verify lúc bắt đầu phiên: `kite-gateway`, `kitehub-frontend`, `kitehub-subscription`, `kite-postgres`, `kitehub-admin`, `kitehub-email`, `kite-mailhog` — tất cả `healthy`).
- PR đã merge và stack đã rebuild:
  - PR #2165 — SePay integration (Bucket A `Payment.txnRef` + V64 migration; Bucket B `PaymentWebhookController` Apikey adapter; Bucket C activation email; Bucket D FE BetaModeBanner)
  - PR #2150 / #2149 — pre-walk fixes (trang nâng cấp FE, tạo subscription cho Owner FREE/TRIAL)
- Owner Tuấn đã có sẵn trên DB:
  - Email: `g2test-an-8@example.com`
  - Mật khẩu: `WalkKh3@2026` (tôi reset trong session này để G2 dùng được)
  - Tenant: `g2test-an-8`, đang TRIAL/FREE, còn 14 ngày
- **Một public tunnel** (ngrok hoặc cloudflared) để SePay dashboard gọi webhook về local stack — chi tiết §"Thiết lập SePay".
- **Một tài khoản ngân hàng cá nhân** có app banking để quét QR + chuyển khoản 10.000đ thật.
- Trình duyệt sạch (không session cũ), DevTools mở tab Network bật log.

## Thiết lập env trước khi bắt đầu

Các env var phải có khi rebuild stack (chế độ Beta + bật SePay):

| Service | Env var | Giá trị | Ý nghĩa |
|---|---|---|---|
| `kitehub-subscription` | `BETA_PAYMENT_OVERRIDE` | `true` | Bật override số tiền về 10.000đ (`kitehub.payment.beta-mode.enabled`) |
| `kitehub-subscription` | `BETA_PAYMENT_AMOUNT_VND` | `10000` | Số tiền tượng trưng (`kitehub.payment.beta-mode.override-amount-vnd`) |
| `kitehub-subscription` | `PAYMENT_MOCK_MODE` | `false` | TẮT mock-mode — dùng SePay thật |
| `kitehub-subscription` | `SEPAY_API_KEY` | `<key>` | API key cấu hình trong SePay dashboard (`kitehub.payment.sepay.api-key`) |
| `kitehub-frontend` | `NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE` | `true` | Bật banner Beta trên trang payment (GAP-977) |

Verify env sau khi rebuild:

```bash
docker exec kitehub-subscription printenv BETA_PAYMENT_OVERRIDE BETA_PAYMENT_AMOUNT_VND PAYMENT_MOCK_MODE SEPAY_API_KEY
# Mong đợi: true / 10000 / false / <key not empty>
docker exec kitehub-frontend printenv NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE
# Mong đợi: true
```

## Thiết lập trước khi bắt đầu (chung)

| Mục | Lệnh / hành động |
|---|---|
| Kiểm tra cổng gateway | `curl -sS http://localhost:9000/actuator/health` → phải trả `{"status":"UP",...}` (Gateway thật ở port **9000**, KHÔNG phải 8080 — đây là điểm khác KH-1 / KH-2c handoff cũ ghi 8080) |
| Mở hộp thư test | Trình duyệt → `http://localhost:8025/` (MailHog UI) — xóa hết thư cũ để dễ nhận biết thư mới |
| Xác nhận trạng thái Owner ban đầu | `docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT i.subdomain, i.tier, i.status, i.subscription_id FROM instances i WHERE i.subdomain = 'g2test-an-8';"` → kết quả mong đợi: `tier=FREE, status=TRIAL, subscription_id=null` |

---

## Thiết lập SePay (one-time, dev làm trước phiên)

Đây là bước one-time — chỉ làm lần đầu, lần sau tunnel + dashboard config còn nguyên thì bỏ qua.

### Bước S1 — Tạo tài khoản SePay Free

1. Truy cập `https://sepay.vn` → đăng ký tài khoản Free (gói Free đủ 50 giao dịch/tháng cho Phase 1 BETA).
2. Đăng nhập dashboard SePay.

> Bước đăng ký là OAuth/web form vendor-only (không có CLI/API path) — bạn thao tác trên web SePay.

### Bước S2 — Đăng ký tài khoản ngân hàng merchant

1. Trong SePay dashboard → mục "Tài khoản ngân hàng" → thêm tài khoản ngân hàng cá nhân của bạn (Vietcombank / MBBank / TCB — đều chấp nhận chuyển khoản tối thiểu 10.000đ).
2. Hoàn tất liên kết theo hướng dẫn SePay (thường cần thêm thông tin tài khoản + xác thực).

### Bước S3 — Mở public tunnel tới local stack

SePay gọi webhook từ internet → cần expose `localhost:9000` (qua gateway) ra public URL.

```bash
# Phương án ngrok
ngrok http 9000
# → copy Forwarding URL dạng https://<random>.ngrok-free.app

# HOẶC cloudflared
cloudflared tunnel --url http://localhost:9000
# → copy URL dạng https://<random>.trycloudflare.com
```

Giữ tunnel chạy suốt phiên test. Lấy public URL (gọi là `<TUNNEL>`).

### Bước S4 — Cấu hình webhook URL trong SePay dashboard

1. SePay dashboard → mục "Webhooks" / "Tích hợp" → thêm webhook mới.
2. **URL:** `<TUNNEL>/api/platform/webhooks/payment` (đi qua gateway port 9000 → route tới `kitehub-subscription`).
3. **Header xác thực:** SePay phải gửi header `Authorization: Apikey <key>` với `<key>` = giá trị `SEPAY_API_KEY` đã set ở env `kitehub-subscription`. Cấu hình API Key trong dashboard SePay match đúng env này.
4. Lưu cấu hình.

> ⚠️ **Dependency G3:** Webhook route `/api/platform/webhooks/payment` cần được gateway route đúng + nằm trong public whitelist (cho phép request không-JWT từ SePay qua, chỉ verify Apikey). Việc wire gateway route + whitelist này thuộc **PR wiring production G3** — nếu trên local stack hiện tại request webhook trả 401/404 ở gateway TRƯỚC khi tới `kitehub-subscription`, đây là gap đã biết của G3, báo lại tôi để bổ sung route + whitelist. Pre-cutover có thể test bằng cách gọi thẳng `kitehub-subscription` (bypass gateway) — xem §Khắc phục.

### Bước S5 — Smoke test webhook trước khi walk

Trước khi đi đường Owner thật, verify webhook endpoint sống bằng curl giả lập payload SePay (thay `<TUNNEL>` + `<key>`):

```bash
curl -sS -X POST "<TUNNEL>/api/platform/webhooks/payment" \
  -H "Authorization: Apikey <key>" \
  -H "Content-Type: application/json" \
  -d '{"id":99999001,"gateway":"Vietcombank","transactionDate":"2026-06-04 09:30:01","accountNumber":"1234567890","subAccount":null,"code":null,"content":"KH3SUBDEADBEEF smoke test","transferType":"in","description":"BankAPINotify KH3SUBDEADBEEF smoke","transferAmount":10000,"referenceCode":"FT00000000000001","accumulated":0}' \
  -w "\nHTTP=%{http_code}\n"
```

- HTTP 401 → Apikey sai (so lại `<key>` vs env `SEPAY_API_KEY`).
- HTTP 400 + "orphan / not found" → endpoint sống, chỉ là `txnRef` KH3SUBDEADBEEF không khớp payment nào (đúng kỳ vọng — chưa có payment thật). Webhook OK, sẵn sàng walk.
- HTTP 404 ở gateway → route/whitelist chưa wire (dependency G3, §Bước S4 note).

---

## Các bước test (đường SePay real-transfer)

### Bước 1 — Đăng nhập Owner

**Hành động:**
1. Mở `http://localhost:3000/login` (FE Next.js của KiteHub)
2. Nhập email `g2test-an-8@example.com` + mật khẩu `WalkKh3@2026` → submit

**✅ Kỳ vọng (PASS):**
- Redirect tới trang dashboard Owner (`/dashboard` hoặc `/`)
- Header hiển thị tên trung tâm "G2 Test Center" + badge "Gói FREE" (hoặc tương đương)
- DevTools Network thấy `POST /api/auth/login` trả HTTP 200 + payload chứa `accessToken`, `instances[0].tier="FREE"`, `instances[0].status="TRIAL"`

**⚠️ Sad path:**
- Sai mật khẩu → toast tiếng Việt "Email hoặc mật khẩu không đúng" (HTTP 400 hoặc 401 đều chấp nhận — GAP-917 chưa thống nhất spec)
- Gateway 503 cold-start (giống GAP-918 ở KH-1) → đợi 30 giây + thử lại

**🔍 Verify:** DevTools Application → Local Storage có key `accessToken` chứa JWT.

---

### Bước 2 — Vào trang nâng cấp + chọn gói BASIC

**Hành động:**
1. Trên dashboard tìm điều hướng "Gói & Thanh toán" / "Billing" → click → URL `/billing`
2. Click "Nâng cấp" → URL `/billing/upgrade`
3. Chọn **BASIC + MONTHLY** → click "Xác nhận nâng cấp"

**✅ Kỳ vọng (PASS):**
- DevTools Network thấy `POST /api/platform/subscriptions` trả HTTP 201 + payload chứa `id` (subscription UUID) + `pendingPaymentId` (UUID khác)
- Redirect tự động sang `/billing/payment/{pendingPaymentId}`

**⚠️ Sad path:**
- Network thấy `PATCH /api/platform/subscriptions/{...}/upgrade` thay vì POST → PR #2149 fix sai, báo lại
- Network thấy 2 lệnh POST cùng lúc (tạo Payment lần 2) → idempotency vỡ, báo lại
- Gateway 503 → đợi cold-start

**🔍 Verify:** Sau khi redirect, query DB
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT s.id, s.tier, s.status, s.pending_tier, s.pending_payment_id FROM subscriptions s JOIN instances i ON s.instance_id=i.id WHERE i.subdomain='g2test-an-8';"
```
→ phải có **đúng 1 dòng**: `tier=FREE`, `status=PENDING`, `pending_tier=BASIC`, `pending_payment_id` không null. Nếu thấy nhiều hơn 1 dòng → BR-SUB-17 idempotency vỡ.

---

### Bước 3 — Kiểm tra banner Beta

**Hành động:**
1. Sau redirect, trang `/billing/payment/{id}` mở
2. Quan sát đầu trang

**✅ Kỳ vọng (PASS):**
- Hiện banner màu hổ phách (amber) với nội dung kiểu: **"🧪 Bạn đang ở chế độ Beta — số tiền chuyển là 10.000đ tượng trưng..."** (component `BetaModeBanner`, GAP-977).
- Banner chỉ hiển thị khi `NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE=true`.

**⚠️ Sad path:**
- KHÔNG thấy banner → kiểm tra `docker exec kitehub-frontend printenv NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE` phải = `true`; nếu thiếu/`false` → FE chưa rebuild với env đúng. Báo lại.

**🔍 Verify:** Banner phải nói rõ "10.000đ tượng trưng" để Owner không nhầm là phải chuyển số tiền thật của gói BASIC.

---

### Bước 4 — Kiểm tra mã QR VietQR + memo txnRef

**Hành động:**
1. Trên trang payment, quan sát phần QR + thông tin chuyển khoản

**✅ Kỳ vọng (PASS):**
- Hiện hình ảnh QR VietQR (sinh động theo `txnRef`).
- Thông tin chuyển khoản hiển thị rõ:
  - Ngân hàng + số tài khoản + tên chủ tài khoản merchant (khớp tài khoản đăng ký SePay)
  - **Số tiền: 10.000đ** (đúng beta override, KHÔNG phải giá gốc BASIC)
  - **Nội dung chuyển khoản (memo)** chứa `txnRef` format `KH3SUB<8 hex>` (ví dụ `KH3SUB1A2B3C4D`)
- Trang đang ở trạng thái chờ (`usePayment` polling 5s).

**🔍 Verify `txn_ref` trong DB:**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT p.id, p.status, p.amount_vnd, p.txn_ref, p.transaction_id FROM payments p JOIN subscriptions s ON p.subscription_id=s.id JOIN instances i ON s.instance_id=i.id WHERE i.subdomain='g2test-an-8' ORDER BY p.created_at DESC LIMIT 1;"
```
→ phải thấy `status=PENDING`, `amount_vnd=10000`, `txn_ref` khớp memo trên QR (format `KH3SUB[A-F0-9]{8}`), `transaction_id` còn null.

**⚠️ Sad path:**
- QR trống / lỗi 500 → kiểm tra `txnRef` đã sinh ở BE chưa (query DB trên); nếu `txn_ref` null → Bucket A (GAP-975) chưa rebuild.
- Số tiền hiển thị KHÁC 10.000đ → beta override không bật, kiểm tra env `kitehub-subscription`.

---

### Bước 5 — Chuyển khoản ngân hàng thật

**Hành động:**
1. Mở app ngân hàng trên điện thoại → chức năng quét QR.
2. Quét QR trên trang `/billing/payment/{id}`.
3. App tự điền: **số tiền 10.000đ** + **nội dung = memo KH3SUB...** (VietQR nhúng sẵn).
4. Xác nhận nội dung khớp memo `txnRef` ở Bước 4 → chuyển khoản.

**✅ Kỳ vọng (PASS):**
- App ngân hàng auto-fill đúng 10.000đ + memo `KH3SUB<hex>` y hệt Bước 4.
- Giao dịch thành công phía app ngân hàng.

**⚠️ Sad path:**
- App không auto-fill memo → kiểm tra QR có encode đúng nội dung không (báo lại nếu memo trống — webhook sẽ không match được `txnRef`).
- App auto-fill số tiền khác 10.000đ → QR encode sai amount, báo lại.

**🔍 Verify:** Chụp màn hình app ngân hàng thấy 10.000đ + memo KH3SUB (để đối chiếu nếu webhook không fire).

---

### Bước 6 — Tự động xác nhận qua SePay webhook

> Đây là điểm khác cốt lõi so với mock-mode: **KHÔNG cần admin curl confirm**. SePay phát hiện giao dịch vào tài khoản merchant → gọi webhook → BE match theo `txnRef`.

**Hành động:**
1. Sau khi chuyển khoản (Bước 5), đợi vài giây tới ~1 phút để SePay nhận biến động số dư + gọi webhook.
2. Quan sát log `kitehub-subscription`:
   ```bash
   docker logs -f kitehub-subscription 2>&1 | grep -iE "webhook|sepay|txnRef|processSepay"
   ```

**✅ Kỳ vọng (PASS) — verify DB:**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT p.status, p.transaction_id, p.txn_ref FROM payments p JOIN subscriptions s ON p.subscription_id=s.id JOIN instances i ON s.instance_id=i.id WHERE i.subdomain='g2test-an-8' ORDER BY p.created_at DESC LIMIT 1;"
```
→ phải thấy `status=COMPLETED`, `transaction_id=<sepay id>` (số id SePay, không null nữa).

```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT s.tier, s.status, s.pending_tier, s.pending_payment_id, s.expires_at FROM subscriptions s JOIN instances i ON s.instance_id=i.id WHERE i.subdomain='g2test-an-8';"
```
→ phải thấy `tier=BASIC`, `status=ACTIVE`, `pending_tier=null`, `pending_payment_id=null`, `expires_at` ~1 tháng sau.

**✅ Kỳ vọng (PASS) — FE tự cập nhật:**
- Trang `/billing/payment/{id}` đang mở: `usePayment` polling 5s phát hiện Payment COMPLETED → hiện **success toast** + chuyển sang thông báo "Gói BASIC đã được kích hoạt".
- Vào lại `/billing` → "Gói hiện tại: BASIC" + ngày hết hạn ~1 tháng.

**⚠️ Sad path:**
- DB vẫn `PENDING` sau >2 phút → webhook chưa fire. Kiểm tra: (a) tunnel còn sống? (b) SePay dashboard có log gọi webhook không? (c) gateway route/whitelist (dependency G3, §Bước S4). Có thể gọi thủ công webhook bypass gateway để verify logic BE (§Khắc phục).
- DB `COMPLETED` nhưng FE vẫn "pending" → polling FE hỏng, báo lại để vá vòng sau.

---

### Bước 7 — Kiểm tra email kích hoạt

**Hành động:**
1. Mở MailHog UI `http://localhost:8025/`

**✅ Kỳ vọng (PASS):**
- Phải có **1 thư mới** tới `g2test-an-8@example.com`.
- Subject: **"[KiteHub] Gói BASIC đã kích hoạt"** (GAP-974, template `subscription-activated.html`).
- Body tiếng Việt, có chi tiết gói BASIC + ngày hết hạn.

**⚠️ Sad path:**
- MailHog không có thư → `applyPendingUpgrade` chưa emit `SUBSCRIPTION_ACTIVATED` outbox event (Bucket C GAP-974) HOẶC outbox dispatcher chưa chạy. Báo lại.

---

## Sad path quét nhanh (đường SePay)

Sau khi happy path PASS, thử nhanh các kịch bản này. Dùng curl gọi thẳng webhook (qua tunnel hoặc bypass gateway) để giả lập payload SePay.

### 7.1 — Idempotency: replay cùng payload (cùng `id`)

1. Lấy `transaction_id` (sepay id) của payment vừa COMPLETED từ DB Bước 6.
2. Gọi lại webhook với **cùng `id`** đó (cùng payload):
   ```bash
   curl -sS -X POST "<TUNNEL>/api/platform/webhooks/payment" \
     -H "Authorization: Apikey <key>" \
     -H "Content-Type: application/json" \
     -d '{"id":<SAME_SEPAY_ID>,"gateway":"Vietcombank","transactionDate":"2026-06-04 09:30:01","accountNumber":"1234567890","subAccount":null,"code":null,"content":"KH3SUB<hex> replay","transferType":"in","description":"BankAPINotify KH3SUB<hex> replay","transferAmount":10000,"referenceCode":"FT00000000000099","accumulated":0}' \
     -w "\nHTTP=%{http_code}\n"
   ```
3. ✅ Kỳ vọng: HTTP 200 + **early-return, KHÔNG double-process**. DB payment vẫn `COMPLETED` với cùng `transaction_id`; subscription KHÔNG bị áp pending upgrade lần 2 (UNIQUE constraint `payments.transaction_id` + idempotency check).

### 7.2 — Apikey sai → 401

```bash
curl -sS -X POST "<TUNNEL>/api/platform/webhooks/payment" \
  -H "Authorization: Apikey WRONG_KEY_123" \
  -H "Content-Type: application/json" \
  -d '{"id":99999002,"transferType":"in","description":"KH3SUBDEADBEEF","transferAmount":10000}' \
  -w "\nHTTP=%{http_code}\n"
```
→ ✅ Kỳ vọng: **HTTP 401** (missing/invalid Apikey header).

### 7.3 — Orphan txnRef → 400

```bash
curl -sS -X POST "<TUNNEL>/api/platform/webhooks/payment" \
  -H "Authorization: Apikey <key>" \
  -H "Content-Type: application/json" \
  -d '{"id":99999003,"transferType":"in","description":"BankAPINotify KH3SUB00000000 orphan","transferAmount":10000}' \
  -w "\nHTTP=%{http_code}\n"
```
→ ✅ Kỳ vọng: **HTTP 400** (txnRef `KH3SUB00000000` extract được nhưng `findPaymentByTxnRef` trả empty — orphan payment notify).

### 7.4 — Sai số tiền (amount mismatch)

1. Tạo subscription pending mới (lặp Bước 2) để có payment PENDING với `amount_vnd=10000`.
2. Gọi webhook với `transferAmount` KHÁC (ví dụ 5000):
   ```bash
   curl -sS -X POST "<TUNNEL>/api/platform/webhooks/payment" \
     -H "Authorization: Apikey <key>" \
     -H "Content-Type: application/json" \
     -d '{"id":99999004,"transferType":"in","description":"BankAPINotify KH3SUB<hex moi>","transferAmount":5000}' \
     -w "\nHTTP=%{http_code}\n"
   ```
3. ✅ Kỳ vọng: **HTTP 400 amount mismatch** — payment KHÔNG bị flip COMPLETED (DB vẫn `PENDING`). Log ghi nhận mismatch.

---

## Phương án mock-mode (không cần SePay) — DỰ PHÒNG

> Đây là đường cũ trước Wave flow-kh3-2. Dùng khi **chưa setup được SePay/tunnel** hoặc muốn test nhanh không qua ngân hàng thật. Yêu cầu set `PAYMENT_MOCK_MODE=true` cho `kitehub-subscription` (ngược với đường SePay).

Khi dùng mock-mode: làm Bước 1 → Bước 2 như trên (env `PAYMENT_MOCK_MODE=true`), sau đó thay Bước 4-6 bằng admin xác nhận thủ công qua curl.

### M1 — Trang QR mock-mode

Trang `/billing/payment/{id}` hiện QR placeholder + thông tin chuyển khoản mock. KHÔNG cần chuyển khoản thật. Coi như đã chuyển → sang M2.

Verify mock-mode bật: `docker exec kitehub-subscription printenv PAYMENT_MOCK_MODE` phải = `true`.

### M2 — Admin xác nhận thanh toán (giả lập)

> Admin endpoint dùng **JWT role `PLATFORM_ADMIN`** forward qua gateway (cơ chế `X-Admin-Key` cũ đã bị xóa per GAP-938). Cần JWT của user có role `PLATFORM_ADMIN`.

1. Lấy `pendingPaymentId` từ DB (lệnh ở Bước 2 §Verify).
2. Lấy JWT admin (đăng nhập user `PLATFORM_ADMIN` → copy `accessToken` từ Local Storage / response login).
3. Chạy lệnh confirm (thay `<PAYMENT_ID>` + `<ADMIN_JWT>`):
   ```bash
   curl -sS -X POST http://localhost:9000/api/platform/admin/payments/<PAYMENT_ID>/confirm \
     -H "Authorization: Bearer <ADMIN_JWT>" \
     -H "Content-Type: application/json" \
     -d '{"transactionId":"WALK-KH3-G2-MOCK-001"}' \
     -w "\nHTTP=%{http_code}\n"
   ```

**✅ Kỳ vọng (PASS):**
- HTTP 200, payload Payment với `status=COMPLETED` + `transactionId=WALK-KH3-G2-MOCK-001`.
- DB:
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c \
    "SELECT s.tier, s.status, s.pending_tier, s.pending_payment_id, s.expires_at FROM subscriptions s JOIN instances i ON s.instance_id=i.id WHERE i.subdomain='g2test-an-8';"
  ```
  → `tier=BASIC`, `status=ACTIVE`, `pending_tier=null`, `pending_payment_id=null`, `expires_at` ~1 tháng sau.

**⚠️ Sad path:**
- HTTP 404 → `AdminPaymentController` chưa lên (PR #2150 chưa merge / chưa rebuild).
- HTTP 401/403 → JWT thiếu hoặc không có role `PLATFORM_ADMIN`.

### M3 — Admin từ chối (sad path, tùy chọn)

```bash
curl -sS -X POST http://localhost:9000/api/platform/admin/payments/<PAYMENT_ID>/reject \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Không tìm thấy giao dịch khớp"}' \
  -w "\nHTTP=%{http_code}\n"
```
→ DB phải thấy `pending_tier=null` + `pending_payment_id=null` + `tier` vẫn FREE. Owner có thể submit lại từ Bước 2.

### M4 — Owner check + email (mock-mode)

Giống Bước 7 đường SePay: refresh `/billing/payment/{id}` → polling phát hiện COMPLETED → success; MailHog có email kích hoạt.

---

## Cách báo kết quả G2

Sau khi xong (hoặc dừng giữa chừng vì gặp blocker nặng), trả về **một trong 4 trạng thái** sau:

| Trạng thái | Khi nào dùng | Cần gửi kèm |
|---|---|---|
| ✅ **FULL PASS** | Cả 7 bước happy path (SePay) + ít nhất 7.1 idempotency sad path PASS, email tới MailHog | Xác nhận ngắn "Đi hết được", không cần evidence |
| ⚠️ **MOSTLY PASS có cosmetic** | Happy path xong nhưng có gap kiểu banner copy chưa chuẩn / email subject lạ / số tiền hiển thị nhỏ | Liệt kê 1 dòng cho mỗi cosmetic; tôi sẽ vá vòng sau |
| 🔴 **BLOCKING** | Có bước fail nặng (webhook không fire dù tunnel sống, BE 500, DB sai trạng thái, gateway route 404) | Chụp Network/log + DB query output + bước số mấy. Tôi gom catalog + vá batch |
| ❓ **UNCLEAR** | Không chắc PASS hay FAIL, lạ một chỗ nào | Ping với screenshot + giải thích ngắn |

Khi nhận báo cáo, tôi sẽ flip campaign §4 dòng KH-3: `🔄 walk-pass-pending-human` → `✅ G1+G2 chờ G3 production parity` (nếu PASS) hoặc thêm vòng vá (nếu BLOCKING).

---

## Khắc phục nhanh khi gặp sự cố

| Hiện tượng | Cách thử trước |
|---|---|
| Gateway timeout / 503 | `docker restart kite-gateway` + đợi 30-60 giây, retry. GAP-918 đã biết. |
| Login 401 | Mật khẩu Owner đã reset lại? Lấy lệnh reset từ tôi (BCrypt hash session-locked). |
| Banner Beta không hiện | `docker exec kitehub-frontend printenv NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE` phải = `true`; rebuild FE nếu thiếu. |
| Số tiền không phải 10.000đ | `docker exec kitehub-subscription printenv BETA_PAYMENT_OVERRIDE BETA_PAYMENT_AMOUNT_VND` phải = `true` / `10000`. |
| `txn_ref` null trên QR | Bucket A (GAP-975) + V64 migration chưa apply; rebuild `kitehub-subscription`. |
| Webhook 404 ở gateway | Route `/api/platform/webhooks/payment` + public whitelist chưa wire (dependency G3, §Bước S4). Bypass gateway: gọi thẳng `kitehub-subscription` container — `docker exec kitehub-subscription curl -sS -X POST http://localhost:8080/api/platform/webhooks/payment -H "Authorization: Apikey <key>" ...` để verify logic BE độc lập. |
| Webhook không fire dù chuyển khoản OK | (a) tunnel còn sống? (b) SePay dashboard có log gọi webhook + status? (c) memo có đúng `KH3SUB<hex>` không? Nếu memo trống → QR encode sai. |
| Webhook 401 | Apikey trong SePay dashboard ≠ env `SEPAY_API_KEY` của `kitehub-subscription`. Đồng bộ lại. |
| Email không tới MailHog | Bucket C (GAP-974) outbox event chưa emit hoặc dispatcher chưa chạy. Báo lại. |

---

## Lưu ý cho G3 (production parity, vòng sau)

G2 chạy trên stack local Docker + tunnel. G3 sẽ chạy trên môi trường production-equivalent (AWS EC2 stack đang stopped, cần khởi động lại). G3 cần verify thêm:

- **Gateway route + public whitelist** cho `/api/platform/webhooks/payment` (dependency đã note ở §Bước S4) — PR wiring production G3.
- SePay webhook gọi vào public domain thật (`kitehub.me`) thay vì tunnel local.
- Email SES gửi tới hộp thư thật (không MailHog) + DKIM signature pass.
- Số tiền beta override: xác nhận `BETA_PAYMENT_OVERRIDE` còn bật trên prod (Phase 1 BETA) — hết Beta thì tắt để dùng giá gốc.
- Idempotency dưới tải thật (SePay retry trên non-200) — verify UNIQUE constraint `payments.transaction_id` chịu được retry burst.

Hiện ngoài phạm vi G2; ghi nhận để chuẩn bị wave sau.

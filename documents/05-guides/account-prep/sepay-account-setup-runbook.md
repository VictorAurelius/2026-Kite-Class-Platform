# SePay Account Setup Runbook — Phase 1 BETA Payment Reconciliation

**Audience:** Solo dev đăng ký SePay merchant lần đầu cho Phase 1 BETA — đối soát chuyển khoản VietQR tự động.
**Standards:** `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2 · `deployment-naming-convention.md` §2 (`account-prep/` — one-time per merchant account).
**Cross-link upstream:** Domain `kitehub.me` đã verify + HTTPS active (production deploy) để webhook URL reachable.
**Cross-link downstream:** Unblocks `PaymentWebhookController` (`POST /api/platform/webhooks/payment`) + `PaymentService.processSepayWebhook` + GAP-975 (dynamic VietQR txnRef) + GAP-976 (SePay webhook auth + idempotency).
**Estimated time:** ~30-45 phút (đăng ký + liên kết bank + cấu hình webhook).
**Last-Updated:** 2026-06-08

---

## TL;DR

SePay (https://sepay.vn) đọc biến động số dư tài khoản ngân hàng của bạn rồi **POST webhook** về KiteHub mỗi khi có chuyển khoản đến. KiteHub khớp `txnRef` (`KH3SUB<8 hex>`) nhúng trong nội dung chuyển khoản → đánh dấu invoice PAID. Free tier 50 giao dịch/tháng đủ cho Phase 1 BETA.

3 việc cần làm:
1. Đăng ký SePay + liên kết tài khoản ngân hàng (real-user action — KYC).
2. Cấu hình webhook URL `https://kitehub.me/api/platform/webhooks/payment` + copy API key.
3. Set API key vào AWS Secrets Manager `kitehub/production/sepay-api-key` (sau khi AWS restore — GAP-612).

**Code đã sẵn sàng** (Wave flow-kh3 + p0-local): terraform secret + `fetch-secrets.sh` pull + `application.yml` wiring đều xong. Runbook này chỉ là phần đăng ký + cấu hình dashboard.

---

## 1. Trước khi bắt đầu — chuẩn bị

| Cần có | Ghi chú |
|---|---|
| Tài khoản ngân hàng VN | SePay hỗ trợ đa số bank VN (VCB / TCB / MB / ACB / BIDV / VPBank...). Dùng tài khoản nhận thanh toán của trung tâm. |
| Số điện thoại + email | Để đăng ký + xác thực SePay account. |
| Domain `kitehub.me` HTTPS live | Webhook URL phải reachable từ internet (SePay gọi POST). Production deploy phải xong + HTTPS active. |
| AWS Secrets Manager access | Để set API key (deferred tới khi AWS restore — GAP-612). |

⚠️ **AWS đang suspended (GAP-612)** → bước 4 (set secret) defer tới khi restore. Bước 1-3 (đăng ký + dashboard) làm trước được.

---

## 2. Đăng ký SePay merchant

### 2.1 Tạo tài khoản

1. Vào https://sepay.vn → **Đăng ký**.
2. Nhập email + số điện thoại + mật khẩu → xác thực OTP.
3. Đăng nhập dashboard.

### 2.2 Liên kết tài khoản ngân hàng

1. Dashboard → **Tài khoản ngân hàng** → **Thêm tài khoản**.
2. Chọn ngân hàng + nhập số tài khoản nhận thanh toán.
3. SePay hướng dẫn cấp quyền đọc biến động số dư (qua API ngân hàng hoặc kết nối Internet Banking — tùy bank). Làm theo hướng dẫn từng bank.
4. Chờ SePay xác nhận liên kết thành công (thường vài phút → vài giờ tùy bank).

### 2.3 Free tier

- Gói Free: 50 giao dịch/tháng — đủ cho Phase 1 BETA cohort nhỏ.
- Khi vượt → nâng gói trả phí (theo dõi `payment_records` count để biết khi nào cần).

---

## 3. Cấu hình webhook + API key

### 3.1 Tạo API key

1. Dashboard → **Cấu hình** → **API / Webhook** (hoặc **Tích hợp**).
2. Tạo **API Key** mới (đây là key dùng cho header `Authorization: Apikey <key>`).
3. **Copy + lưu tạm an toàn** (password manager) — sẽ dùng ở bước 4. KHÔNG commit vào repo.

### 3.2 Cấu hình webhook URL

1. Dashboard → **Webhooks** → **Thêm webhook**.
2. URL: `https://kitehub.me/api/platform/webhooks/payment`
3. Method: `POST`
4. Authentication: chọn **Apikey** (SePay gửi header `Authorization: Apikey <key>` — khớp với `kitehub.payment.sepay.api-key`).
5. Event: chọn **Giao dịch tiền vào** (incoming credit) — chỉ cần khi có tiền chuyển đến.
6. Lưu.

### 3.3 Định dạng nội dung chuyển khoản (txnRef)

KiteHub nhúng `txnRef` dạng `KH3SUB<8 hex>` (regex `KH3SUB[A-F0-9]{8}`) vào **nội dung chuyển khoản** của mã VietQR sinh cho mỗi invoice. SePay gửi nguyên `description` về webhook → `PaymentService` extract txnRef + khớp `Payment.findByTxnRef`.

> Không cần cấu hình gì thêm ở SePay cho txnRef — nó nằm trong description do KiteHub tự sinh khi tạo mã QR. Đảm bảo SePay forward đầy đủ field `description` trong payload webhook (mặc định có).

---

## 4. Set API key vào AWS Secrets Manager (defer — GAP-612)

Sau khi AWS account 906286017800 restore:

```bash
# 1. terraform apply tạo placeholder secret (đã declare ở infrastructure/terraform-aws/secrets.tf)
#    resource aws_secretsmanager_secret.sepay_api_key — name kitehub/production/sepay-api-key

# 2. Set giá trị thật qua AWS console:
#    Secrets Manager -> kitehub/production/sepay-api-key -> Retrieve secret value
#    -> Set new value -> plain string "<sepay-api-key-copy-từ-bước-3.1>"

# Hoặc qua CLI (khi có dev-admin creds):
aws secretsmanager put-secret-value \
  --secret-id kitehub/production/sepay-api-key \
  --secret-string '<sepay-api-key>' \
  --profile dev-admin --region ap-southeast-1
```

`fetch-secrets.sh` pull secret này lúc EC2 boot vào `/etc/kite/.env` dưới biến `SEPAY_API_KEY`. `lifecycle ignore_changes = [secret_string]` giữ giá trị thật qua các lần `terraform apply` sau.

---

## 5. Verify (sau khi live)

1. Tạo 1 invoice test → KiteHub sinh mã VietQR với `txnRef` `KH3SUB...` trong description.
2. Chuyển khoản số tiền nhỏ (vd 10.000đ) đúng nội dung từ tài khoản khác.
3. SePay phát hiện biến động → POST webhook về `/api/platform/webhooks/payment`.
4. Verify:
   - HTTP 200 từ webhook (SePay dashboard → webhook log).
   - DB: `payment_records` có row mới + invoice status → PAID.
   - Log kitehub-subscription: `Processing SePay webhook: id=..., amount=10000, description=...`.
5. Idempotency: SePay gửi lại cùng `id` → webhook trả 200 sớm, không double-process (GAP-976).

### Sad path
- Sai/thiếu API key → webhook trả `401`.
- txnRef không khớp invoice nào → `400` (orphan txnRef).
- `SEPAY_API_KEY` empty trong production → webhook reject toàn bộ (fail closed).

---

## 6. Beta-mode override (test rẻ)

`application.yml` có `beta-mode.enabled` + `override-amount-vnd` (default 10000) — khi bật, mọi invoice dùng số tiền override nhỏ để test thật không tốn nhiều. Tắt khi go-live thật.

---

## 7. Liên quan

- `PaymentWebhookController` + `PaymentService.processSepayWebhook` — `kitehub/kitehub-subscription`
- `infrastructure/terraform-aws/secrets.tf` — `aws_secretsmanager_secret.sepay_api_key`
- `scripts/fetch-secrets.sh` — pull `SEPAY_API_KEY` (line ~120)
- GAP-975 (dynamic VietQR txnRef + beta amount) · GAP-976 (webhook Apikey auth + idempotency)
- Sister vendor: `zalo-oa-setup-runbook.md` (notification) · `06-resend-account-setup.md` (email)
